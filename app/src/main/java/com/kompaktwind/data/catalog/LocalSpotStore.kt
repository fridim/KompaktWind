package com.kompaktwind.data.catalog

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.UUID
import kotlin.math.floor

sealed interface ImportOutcome {
    data class Success(val info: LocalImportInfo) : ImportOutcome
    data class Failure(val reason: ImportFailure) : ImportOutcome
}

enum class ImportFailure {
    INVALID_JSON,
    TOO_LARGE,
    TOO_MANY,
    NO_VALID_SPOTS,
    IO
}

data class LocalImportInfo(
    val id: String,
    val fileName: String,
    val spots: Int,
    val skipped: Int,
    val importedAt: Long
)

@Serializable
private data class ImportMetadata(
    val id: String,
    val fileName: String,
    val spots: Int,
    val skipped: Int,
    val importedAt: Long
)

@Serializable
private data class ImportedSpotJson(
    val id: String = "",
    val name: String = "",
    val shortName: String = "",
    val altNames: List<String> = emptyList(),
    val lat: Double? = null,
    val lon: Double? = null,
    val country: String? = null,
    val region: String? = null,
    val description: String = "",
    val windDirections: List<String> = emptyList(),
    val avoidWindDirections: List<String> = emptyList(),
    val activities: List<String> = emptyList(),
    val source: String = "",
    val sourceUrl: String = "",
    val sources: List<CatalogSource> = emptyList(),
    val updated: String = ""
)

class LocalSpotStore(
    private val dir: File,
    private val maxBytes: Int = 5 * 1024 * 1024,
    private val maxSpots: Int = 10_000,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val newId: () -> String = { UUID.randomUUID().toString().take(8) }
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun list(): List<LocalImportInfo> = withContext(Dispatchers.IO) {
        ensureDir()
        dir.listFiles { file -> file.name.endsWith(META_SUFFIX) }
            ?.mapNotNull(::readMeta)
            ?.sortedByDescending { it.importedAt }
            .orEmpty()
    }

    suspend fun spots(): List<CatalogSpot> = withContext(Dispatchers.IO) {
        list().flatMap { info ->
            val file = dataFile(info.id)
            if (!file.exists()) return@flatMap emptyList()
            when (val parsed = parse(file.readBytes(), info.id)) {
                is ParseResult.Success -> parsed.spots
                is ParseResult.Failure -> emptyList()
            }
        }
    }

    suspend fun import(fileName: String, input: InputStream): ImportOutcome = withContext(Dispatchers.IO) {
        try {
            ensureDir()
            val bytes = readCapped(input) ?: return@withContext ImportOutcome.Failure(ImportFailure.TOO_LARGE)
            val id = newId()
            when (val parsed = parse(bytes, id)) {
                is ParseResult.Failure -> ImportOutcome.Failure(parsed.reason)
                is ParseResult.Success -> {
                    val metadata = ImportMetadata(
                        id = id,
                        fileName = fileName,
                        spots = parsed.spots.size,
                        skipped = parsed.skipped,
                        importedAt = now()
                    )
                    dataFile(id).writeBytes(bytes)
                    metaFile(id).writeText(json.encodeToString(ImportMetadata.serializer(), metadata))
                    ImportOutcome.Success(metadata.toInfo())
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            ImportOutcome.Failure(ImportFailure.IO)
        }
    }

    suspend fun delete(id: String): Unit = withContext(Dispatchers.IO) {
        dataFile(id).delete()
        metaFile(id).delete()
        Unit
    }

    private fun parse(bytes: ByteArray, importId: String): ParseResult {
        if (bytes.size > maxBytes) return ParseResult.Failure(ImportFailure.TOO_LARGE)
        val root = try {
            json.parseToJsonElement(bytes.decodeToString())
        } catch (_: Exception) {
            return ParseResult.Failure(ImportFailure.INVALID_JSON)
        }
        val array = root as? JsonArray ?: return ParseResult.Failure(ImportFailure.INVALID_JSON)
        if (array.size > maxSpots) return ParseResult.Failure(ImportFailure.TOO_MANY)

        var skipped = 0
        val spots = ArrayList<CatalogSpot>(array.size)
        array.forEachIndexed { index, element ->
            val spot = try {
                json.decodeFromJsonElement(ImportSpotSerializer, element)
            } catch (_: Exception) {
                null
            }
            if (spot == null || spot.name.isBlank() || spot.lat == null || spot.lon == null ||
                spot.lat !in -90.0..90.0 || spot.lon !in -180.0..180.0
            ) {
                skipped++
            } else {
                spots += spot.toCatalogSpot(importId, index)
            }
        }
        return if (spots.isEmpty()) {
            ParseResult.Failure(ImportFailure.NO_VALID_SPOTS)
        } else {
            ParseResult.Success(spots, skipped)
        }
    }

    private fun ImportedSpotJson.toCatalogSpot(importId: String, index: Int): CatalogSpot {
        val latitude = lat ?: 0.0
        val longitude = lon ?: 0.0
        return CatalogSpot(
            id = "local-$importId-$index",
            name = name.trim(),
            shortName = shortName,
            altNames = altNames,
            lat = latitude,
            lon = longitude,
            country = country,
            region = region,
            description = description,
            windDirections = windDirections,
            avoidWindDirections = avoidWindDirections,
            activities = activities,
            source = source.ifBlank { LOCAL_SOURCE },
            sourceUrl = sourceUrl,
            sources = sources,
            tile = "${floor(latitude).toInt()}_${floor(longitude).toInt()}",
            updated = updated,
            local = true
        )
    }

    private fun readCapped(input: InputStream): ByteArray? = input.use {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = it.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) return null
            out.write(buffer, 0, read)
        }
        out.toByteArray()
    }

    private fun readMeta(file: File): LocalImportInfo? = try {
        json.decodeFromString(ImportMetadata.serializer(), file.readText()).toInfo()
    } catch (_: Exception) {
        null
    }

    private fun ImportMetadata.toInfo() = LocalImportInfo(id, fileName, spots, skipped, importedAt)

    private fun ensureDir() {
        if (!dir.exists()) dir.mkdirs()
    }

    private fun dataFile(id: String) = File(dir, "$id$DATA_SUFFIX")

    private fun metaFile(id: String) = File(dir, "$id$META_SUFFIX")

    private sealed interface ParseResult {
        data class Success(val spots: List<CatalogSpot>, val skipped: Int) : ParseResult
        data class Failure(val reason: ImportFailure) : ParseResult
    }

    private companion object {
        const val LOCAL_SOURCE = "local"
        const val DATA_SUFFIX = ".json"
        const val META_SUFFIX = ".meta.json"
        val ImportSpotSerializer = ImportedSpotJson.serializer()
    }
}

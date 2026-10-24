package com.kompaktwind.data.catalog

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream

class SpotCatalogRepository(
    private val context: Context,
    private val localStore: LocalSpotStore = LocalSpotStore(File(context.filesDir, "imported-spots"))
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Volatile
    private var memoryIndex: List<CatalogSpot>? = null

    @Volatile
    private var localCache: List<CatalogSpot>? = null

    suspend fun index(): Result<List<CatalogSpot>> = runCatching {
        memoryIndex?.let { return@runCatching it }
        val bundled = parse(assetText(INDEX_FILE))
        val seen = HashSet<String>(bundled.size + 16)
        val merged = ArrayList<CatalogSpot>(bundled.size + 16)
        bundled.forEach {
            seen += it.dedupeKey()
            merged += it
        }
        localSpots().forEach {
            if (seen.add(it.dedupeKey())) merged += it
        }
        memoryIndex = merged
        merged
    }

    suspend fun shard(tile: String): Result<List<CatalogSpot>> = runCatching {
        val bundled = runCatching { parse(assetText("spots/spots.$tile.json")) }
            .getOrDefault(emptyList())
        val seen = bundled.mapTo(HashSet(bundled.size)) { it.dedupeKey() }
        bundled + localSpots().filter { it.tile == tile && seen.add(it.dedupeKey()) }
    }

    suspend fun localSpots(): List<CatalogSpot> =
        localCache ?: localStore.spots().also { localCache = it }

    suspend fun imports(): List<LocalImportInfo> = localStore.list()

    suspend fun import(fileName: String, input: InputStream): ImportOutcome {
        val outcome = localStore.import(fileName, input)
        if (outcome is ImportOutcome.Success) invalidate()
        return outcome
    }

    suspend fun deleteImport(id: String) {
        localStore.delete(id)
        invalidate()
    }

    fun invalidate() {
        memoryIndex = null
        localCache = null
    }

    private suspend fun assetText(path: String): String = withContext(Dispatchers.IO) {
        context.assets.open(path).bufferedReader().use { it.readText() }
    }

    private fun parse(text: String): List<CatalogSpot> = json.decodeFromString(text)

    companion object {
        private const val INDEX_FILE = "spots/spots.index.json"
    }
}

package com.kompaktwind.ui.spots

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kompaktwind.R
import com.kompaktwind.data.catalog.CatalogSpot
import com.kompaktwind.data.catalog.displayName
import com.kompaktwind.data.catalog.CatalogState
import com.kompaktwind.ui.KompaktWindViewModel
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.progress_indicator.CircularProgressIndicatorMMD
import com.mudita.mmd.components.text.TextMMD

@Composable
fun SpotInfoScreen(viewModel: KompaktWindViewModel, catalogId: String) {
    val catalog by viewModel.catalog.collectAsState()

    LaunchedEffect(catalogId) {
        if (catalog !is CatalogState.Data) viewModel.loadCatalog()
    }

    when (val state = catalog) {
        is CatalogState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicatorMMD()
        }
        is CatalogState.Error -> Box(Modifier.fillMaxSize().padding(16.dp)) {
            TextMMD(text = stringResource(R.string.catalog_error), fontSize = 18.sp)
        }
        is CatalogState.Data -> {
            val spot = state.spots.firstOrNull { it.id == catalogId }
            if (spot == null) {
                Box(Modifier.fillMaxSize().padding(16.dp)) {
                    TextMMD(text = stringResource(R.string.spot_not_found), fontSize = 18.sp)
                }
            } else {
                SpotInfoContent(spot = spot)
            }
        }
    }
}

@Composable
private fun SpotInfoContent(spot: CatalogSpot) {
    val uriHandler = LocalUriHandler.current
    val activityLabels = mutableListOf<String>()
    for (activity in spot.activities) {
        activityLabels += when (activity) {
            "kitesurfing" -> stringResource(R.string.activity_kitesurfing)
            "windsurfing" -> stringResource(R.string.activity_windsurfing)
            "wing" -> stringResource(R.string.activity_wing)
            "surf" -> stringResource(R.string.activity_surf)
            "kite" -> stringResource(R.string.activity_kite)
            "parapente" -> stringResource(R.string.activity_parapente)
            else -> activity
        }
    }
    val place = listOfNotNull(spot.region, spot.country).joinToString(", ").ifBlank { "—" }
    val summary = if (activityLabels.isEmpty()) {
        stringResource(R.string.spot_summary_no_activities, place)
    } else {
        stringResource(R.string.spot_summary, place, activityLabels.joinToString(" · "))
    }
    LazyColumnMMD(modifier = Modifier.fillMaxSize()) {
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                TextMMD(
                    text = spot.displayName(),
                    fontSize = 24.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                if (spot.altNames.isNotEmpty()) {
                    TextMMD(
                        text = "${stringResource(R.string.alt_names)}: ${spot.altNames.joinToString(" · ")}",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                val place = listOfNotNull(spot.region, spot.country).joinToString(", ")
                if (place.isNotBlank()) {
                    TextMMD(text = place, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
                }
                TextMMD(
                    text = "${stringResource(R.string.coordinates)}: ${"%.4f, %.4f".format(spot.lat, spot.lon)}",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            HorizontalDividerMMD(thickness = 0.5.dp)
        }

        item {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoLine(R.string.recommended_winds, spot.windDirections)
                InfoLine(R.string.avoid_winds, spot.avoidWindDirections)
                InfoLine(R.string.activities, spot.activities)
                TextMMD(
                    text = summary,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (spot.description.isNotBlank()) {
                    TextMMD(
                        text = spot.description,
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            HorizontalDividerMMD(thickness = 0.5.dp)
        }

        item {
            Column(modifier = Modifier.padding(16.dp)) {
                TextMMD(
                    text = stringResource(R.string.sources),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                val links = spot.sources.ifEmpty {
                    listOf(com.kompaktwind.data.catalog.CatalogSource(spot.source, spot.sourceUrl))
                }
                links.filter { it.name.isNotBlank() }.forEach { source ->
                    val modifier = if (source.url.isNotBlank()) {
                        Modifier.padding(top = 4.dp).clickable { uriHandler.openUri(source.url) }
                    } else {
                        Modifier.padding(top = 4.dp)
                    }
                    TextMMD(
                        text = sourceName(source.name),
                        fontSize = 16.sp,
                        textDecoration = if (source.url.isNotBlank()) TextDecoration.Underline else TextDecoration.None,
                        modifier = modifier
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoLine(labelRes: Int, values: List<String>) {
    if (values.isEmpty()) return
    TextMMD(
        text = "${stringResource(labelRes)}: ${values.joinToString(" · ")}",
        fontSize = 18.sp,
        lineHeight = 22.sp,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun sourceName(source: String): String = when (source) {
    "openstreetmap.org" -> "OpenStreetMap"
    "local" -> stringResource(R.string.imported_source)
    else -> source
}

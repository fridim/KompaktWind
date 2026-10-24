package com.kompaktwind.ui.nearby

import android.Manifest
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kompaktwind.R
import com.kompaktwind.data.DeviceLocationProvider
import com.kompaktwind.data.catalog.CatalogSpot
import com.kompaktwind.data.catalog.CatalogState
import com.kompaktwind.data.catalog.hasEnabledSource
import com.kompaktwind.data.catalog.displayName
import com.kompaktwind.data.catalog.likelyCoastal
import com.kompaktwind.ui.KompaktWindViewModel
import com.kompaktwind.ui.common.UnitFormatters
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.progress_indicator.CircularProgressIndicatorMMD
import com.mudita.mmd.components.text.TextMMD
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun NearMeScreen(
    viewModel: KompaktWindViewModel,
    onOpenInfo: (String) -> Unit
) {
    val catalog by viewModel.catalog.collectAsState()
    val spots by viewModel.spots.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val context = LocalContext.current
    val provider = remember { DeviceLocationProvider(context) }
    var location by remember { mutableStateOf<Location?>(provider.lastKnown()) }
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        location = if (granted) provider.lastKnown() else null
    }

    LaunchedEffect(Unit) {
        if (catalog !is CatalogState.Data) viewModel.loadCatalog()
        if (!provider.hasPermission()) permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val fallback = spots.lastOrNull()
    val referenceLat = location?.latitude ?: fallback?.lat ?: 46.6
    val referenceLon = location?.longitude ?: fallback?.lon ?: 2.4
    val addedIds = remember(spots) { spots.mapNotNull { it.catalogId }.toSet() }

    Column(modifier = Modifier.fillMaxSize()) {
        TextMMD(
            text = stringResource(if (location != null) R.string.near_me_using_location else R.string.near_me_fallback),
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        if (permissionDenied) {
            TextMMD(
                text = stringResource(R.string.near_me_permission_denied),
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        when (val state = catalog) {
            is CatalogState.Loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicatorMMD()
            }
            is CatalogState.Error -> Box(Modifier.fillMaxWidth().weight(1f).padding(16.dp)) {
                TextMMD(text = stringResource(R.string.catalog_error), fontSize = 18.sp)
            }
            is CatalogState.Data -> {
                val nearest = remember(
                    state.spots,
                    settings.enabledSources,
                    referenceLat,
                    referenceLon
                ) {
                    state.spots
                        .filter { it.hasEnabledSource(settings.enabledSources) }
                        .map { it to haversineKm(referenceLat, referenceLon, it.lat, it.lon) }
                        .sortedBy { it.second }
                        .take(100)
                }
                LazyColumnMMD(modifier = Modifier.weight(1f)) {
                    items(nearest, key = { it.first.id }) { (spot, distance) ->
                        NearMeRow(
                            spot = spot,
                            distanceText = UnitFormatters.distanceLabel(distance, settings.distanceUnit),
                            added = spot.id in addedIds,
                            onOpenInfo = { onOpenInfo(spot.id) },
                            onAdd = {
                                viewModel.addSpot(
                                    name = spot.displayName(),
                                    lat = spot.lat,
                                    lon = spot.lon,
                                    isCoastal = spot.likelyCoastal(),
                                    shortName = spot.shortName.ifBlank { null },
                                    catalogId = spot.id
                                )
                            }
                        )
                        HorizontalDividerMMD(thickness = 0.5.dp)
                    }
                    item {
                        TextMMD(
                            text = stringResource(R.string.catalog_attribution),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NearMeRow(
    spot: CatalogSpot,
    distanceText: String,
    added: Boolean,
    onOpenInfo: () -> Unit,
    onAdd: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenInfo)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextMMD(
                text = spot.displayName(),
                fontSize = 18.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onOpenInfo) {
                Icon(Icons.Outlined.Lightbulb, contentDescription = stringResource(R.string.spot_info))
            }
            IconButton(onClick = onAdd, enabled = !added) {
                Icon(
                    if (added) Icons.Outlined.Check else Icons.Outlined.Add,
                    contentDescription = stringResource(if (added) R.string.added_to_favorites else R.string.add_to_favorites)
                )
            }
        }
        val place = listOfNotNull(spot.region, spot.country, distanceText).joinToString(" · ")
        TextMMD(text = place, fontSize = 14.sp)
        if (spot.windDirections.isNotEmpty()) {
            TextMMD(
                text = "${stringResource(R.string.recommended_winds)}: ${spot.windDirections.joinToString(" ")}",
                fontSize = 14.sp
            )
        }
    }
}

private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val radius = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
        sin(dLon / 2) * sin(dLon / 2)
    return radius * 2 * atan2(sqrt(a), sqrt(1 - a))
}

package com.kompaktwind.ui.addspot

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kompaktwind.R
import com.kompaktwind.data.CoordinatesParser
import com.kompaktwind.data.catalog.CatalogSpot
import com.kompaktwind.data.catalog.CatalogState
import com.kompaktwind.data.catalog.displayName
import com.kompaktwind.data.catalog.hasEnabledSource
import com.kompaktwind.data.catalog.likelyCoastal
import com.kompaktwind.ui.map.CoastlineMap
import com.kompaktwind.ui.map.MapCamera
import com.kompaktwind.ui.map.MapMarker
import com.kompaktwind.ui.AddSpotDraft
import com.kompaktwind.ui.KompaktWindViewModel
import java.util.Locale
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.checkbox.CheckboxMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AddSpotScreen(
    viewModel: KompaktWindViewModel,
    onSaved: () -> Unit,
    onOpenInfo: (String) -> Unit = {}
) {
    val tabIndex by viewModel.addSpotTabIndex.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        val tabLabels = listOf(
            stringResource(R.string.search),
            stringResource(R.string.addspot_map),
            stringResource(R.string.coordinates)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            tabLabels.forEachIndexed { i, label ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.setAddSpotTabIndex(i) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextMMD(
                        text = label,
                        fontSize = 18.sp,
                        fontWeight = if (tabIndex == i) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (tabIndex == i) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .fillMaxWidth(0.7f)
                                .height(2.dp)
                                .background(androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                        )
                    }
                }
            }
        }
        HorizontalDividerMMD(thickness = 0.5.dp)
        when (tabIndex) {
            0 -> SearchTab(viewModel = viewModel, onSaved = onSaved, onOpenInfo = onOpenInfo)
            1 -> MapPickerTab(viewModel = viewModel, onSaved = onSaved, onOpenInfo = onOpenInfo)
            else -> CoordinatesTab(viewModel = viewModel, onSaved = onSaved)
        }
    }
}

@Composable
private fun SearchTab(
    viewModel: KompaktWindViewModel,
    onSaved: () -> Unit,
    onOpenInfo: (String) -> Unit
) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val catalog by viewModel.catalog.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val listState = rememberLazyListState()
    val known = remember(query, catalog, settings.enabledSources) {
        (catalog as? CatalogState.Data)?.spots
            .orEmpty()
            .filter { spot ->
                query.isNotBlank() &&
                    spot.hasEnabledSource(settings.enabledSources) &&
                    (spot.displayName().contains(query, ignoreCase = true) ||
                        spot.altNames.any { it.contains(query, ignoreCase = true) })
            }
            .take(20)
    }

    LaunchedEffect(Unit) {
        if (catalog !is CatalogState.Data) viewModel.loadCatalog()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextFieldMMD(
            value = query,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            placeholder = { TextMMD(stringResource(R.string.place_name)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        LazyColumnMMD(state = listState) {
            if (known.isNotEmpty()) {
                item {
                    TextMMD(
                        text = stringResource(R.string.addspot_known),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(known, key = { "known-${it.id}" }) { spot ->
                    KnownSearchRow(
                        spot = spot,
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
                            onSaved()
                        }
                    )
                    HorizontalDividerMMD(thickness = 0.5.dp)
                }
            }
            items(results, key = { it.id }) { result ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    TextMMD(text = result.name, fontWeight = FontWeight.Bold)
                    val subtitle = listOfNotNull(result.admin1, result.country).joinToString(", ")
                    if (subtitle.isNotEmpty()) TextMMD(text = subtitle, fontSize = 16.sp)
                    Row {
                        OutlinedButtonMMD(onClick = {
                            viewModel.addSpot(name = result.name, lat = result.latitude, lon = result.longitude, isCoastal = false)
                            viewModel.clearSearch()
                            onSaved()
                        }) { TextMMD(stringResource(R.string.save_inland)) }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButtonMMD(onClick = {
                            viewModel.addSpot(name = result.name, lat = result.latitude, lon = result.longitude, isCoastal = true)
                            viewModel.clearSearch()
                            onSaved()
                        }) { TextMMD(stringResource(R.string.save_coastal)) }
                    }
                }
                HorizontalDividerMMD()
            }
        }
    }
}

@Composable
private fun KnownSearchRow(
    spot: CatalogSpot,
    onOpenInfo: () -> Unit,
    onAdd: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextMMD(
                text = spot.displayName(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onOpenInfo) {
                Icon(Icons.Outlined.Lightbulb, contentDescription = stringResource(R.string.spot_info))
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add_to_favorites))
            }
        }
        val details = listOfNotNull(
            listOfNotNull(spot.region, spot.country).joinToString(", ").ifBlank { null },
            spot.windDirections.takeIf { it.isNotEmpty() }?.let {
                "${stringResource(R.string.recommended_winds)}: ${it.joinToString(" ")}"
            }
        ).joinToString(" · ")
        if (details.isNotBlank()) TextMMD(text = details, fontSize = 16.sp)
    }
}

@Composable
private fun MapPickerTab(
    viewModel: KompaktWindViewModel,
    onSaved: () -> Unit,
    onOpenInfo: (String) -> Unit
) {
    val draft by viewModel.addSpotDraft.collectAsState()
    val camera = MapCamera(draft.lat, draft.lon, draft.zoom)
    val catalog by viewModel.catalog.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val knownSpots = (catalog as? CatalogState.Data)?.spots
        .orEmpty()
        .filter { it.hasEnabledSource(settings.enabledSources) }
    val markers = remember(knownSpots) { knownSpots.map { MapMarker(it.id, it.lat, it.lon) } }
    val selected = knownSpots.firstOrNull { it.id == draft.selectedCatalogId }

    LaunchedEffect(Unit) {
        if (catalog !is CatalogState.Data) viewModel.loadCatalog()
    }

    Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
        CoastlineMap(
            camera = camera,
            onCameraChange = { updated ->
                viewModel.updateAddSpotDraft {
                    it.copy(lat = updated.lat, lon = updated.lon, zoom = updated.zoom)
                }
            },
            markers = markers,
            selectedId = selected?.id,
            onMarkerClick = { marker ->
                val spot = knownSpots.firstOrNull { it.id == marker.id }
                if (spot != null) {
                    viewModel.updateAddSpotDraft {
                        it.copy(
                            selectedCatalogId = spot.id,
                            name = spot.displayName(),
                            shortName = spot.shortName
                        )
                    }
                }
            },
            onMapTap = {
                viewModel.updateAddSpotDraft { it.copy(selectedCatalogId = null) }
            },
            showCrosshair = selected == null
        )
        TextMMD(
            text = String.format(Locale.US, "%.4f, %.4f", camera.lat, camera.lon),
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
        )
        TextMMD(
            text = stringResource(R.string.coastline_attribution),
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
        )
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(6.dp)
        ) {
            MapZoomButton(text = "+", onClick = {
                viewModel.updateAddSpotDraft { it.copy(zoom = (it.zoom + 1.0).coerceAtMost(12.0)) }
            })
            Spacer(Modifier.height(8.dp))
            MapZoomButton(text = "\u2212", onClick = {
                viewModel.updateAddSpotDraft { it.copy(zoom = (it.zoom - 1.0).coerceAtLeast(1.0)) }
            })
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            selected?.let { spot ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    TextMMD(
                        text = "${spot.displayName()} · ${stringResource(R.string.addspot_known)}",
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onOpenInfo(spot.id) }) {
                        Icon(Icons.Outlined.Lightbulb, contentDescription = stringResource(R.string.spot_info))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextFieldMMD(
                    value = draft.name,
                    onValueChange = { value ->
                        viewModel.updateAddSpotDraft { it.copy(name = value, selectedCatalogId = null) }
                    },
                    placeholder = { TextMMD(stringResource(R.string.name), fontSize = 16.sp) },
                    modifier = Modifier.weight(1.2f)
                )
                Spacer(Modifier.width(6.dp))
                TextFieldMMD(
                    value = draft.shortName,
                    onValueChange = { value ->
                        viewModel.updateAddSpotDraft { it.copy(shortName = value, selectedCatalogId = null) }
                    },
                    placeholder = { TextMMD(stringResource(R.string.short_name_optional), fontSize = 16.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                CheckboxMMD(
                    checked = draft.coastal,
                    onCheckedChange = { value -> viewModel.updateAddSpotDraft { it.copy(coastal = value) } }
                )
                TextMMD(stringResource(R.string.coastal_spot), fontSize = 16.sp, modifier = Modifier.weight(1f))
                OutlinedButtonMMD(
                    enabled = selected != null || draft.name.isNotBlank(),
                    onClick = {
                        val known = selected
                        if (known != null) {
                            viewModel.addSpot(
                                name = known.displayName(),
                                lat = known.lat,
                                lon = known.lon,
                                isCoastal = known.likelyCoastal(),
                                shortName = known.shortName.ifBlank { null },
                                catalogId = known.id
                            )
                        } else {
                            viewModel.addSpot(
                                name = draft.name.trim(),
                                lat = camera.lat,
                                lon = camera.lon,
                                isCoastal = draft.coastal,
                                shortName = draft.shortName.trim().ifBlank { null }
                            )
                        }
                        onSaved()
                    }
                ) { TextMMD(stringResource(R.string.save_spot), fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            }
        }
    }
}

@Composable
private fun MapZoomButton(text: String, onClick: () -> Unit) {
    OutlinedButtonMMD(onClick = onClick, modifier = Modifier.size(48.dp)) {
        TextMMD(text = text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CoordinatesTab(viewModel: KompaktWindViewModel, onSaved: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var shortName by remember { mutableStateOf("") }
    var coordinates by remember { mutableStateOf("") }
    var coastal by remember { mutableStateOf(true) }
    val parsed = remember(coordinates) { CoordinatesParser.parse(coordinates) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextFieldMMD(value = name, onValueChange = { name = it }, placeholder = { TextMMD(stringResource(R.string.name)) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        TextFieldMMD(value = shortName, onValueChange = { shortName = it }, placeholder = { TextMMD(stringResource(R.string.short_name_optional)) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        TextFieldMMD(
            value = coordinates,
            onValueChange = { coordinates = it },
            placeholder = { TextMMD(stringResource(R.string.coordinates_hint)) },
            modifier = Modifier.fillMaxWidth()
        )
        if (coordinates.isNotBlank() && parsed == null) {
            TextMMD(
                text = stringResource(R.string.coordinates_invalid),
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row {
            CheckboxMMD(checked = coastal, onCheckedChange = { coastal = it })
            TextMMD(stringResource(R.string.coastal_spot))
        }
        Spacer(Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            OutlinedButtonMMD(
                enabled = name.isNotBlank() && parsed != null,
                onClick = {
                    if (parsed != null) {
                        viewModel.addSpot(
                            name = name.trim(),
                            lat = parsed.lat,
                            lon = parsed.lon,
                            isCoastal = coastal,
                            shortName = shortName.trim().ifBlank { null }
                        )
                        onSaved()
                    }
                }
            ) { TextMMD(stringResource(R.string.save_spot), fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        }
    }
}

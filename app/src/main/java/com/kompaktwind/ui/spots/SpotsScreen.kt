package com.kompaktwind.ui.spots

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kompaktwind.R
import com.kompaktwind.data.Spot
import com.kompaktwind.data.catalog.CatalogState
import com.kompaktwind.ui.KompaktWindViewModel
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SpotsScreen(
    viewModel: KompaktWindViewModel,
    onAddSpot: () -> Unit,
    onOpenSpot: (Spot) -> Unit,
    onOpenInfo: (String) -> Unit
) {
    val spots by viewModel.spots.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val catalog by viewModel.catalog.collectAsState()

    LaunchedEffect(spots) {
        if (spots.any { it.catalogId != null } && catalog !is CatalogState.Data) viewModel.loadCatalog()
    }
    val catalogById = (catalog as? CatalogState.Data)?.spots?.associateBy { it.id }.orEmpty()
    val preview by viewModel.forecastPreview.collectAsState()
    val showPicker by viewModel.showForecastPicker.collectAsState()
    var actionsFor by remember { mutableStateOf<String?>(null) }

    if (showPicker) {
        ForecastPreviewSheet(
            onDismiss = viewModel::dismissForecastPicker,
            onApply = viewModel::startForecastPreview
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (spots.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TextMMD(text = stringResource(R.string.spots_empty), fontSize = 20.sp, textAlign = TextAlign.Center)
                    ButtonMMD(onClick = onAddSpot) { TextMMD(stringResource(R.string.add_a_spot)) }
                }
            }
        } else {
            preview?.let { state ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextMMD(
                        text = stringResource(
                            R.string.forecast_preview_header,
                            forecastTimeLabel(state.targetEpochMs)
                        ),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = viewModel::clearForecastPreview) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.forecast_close)
                        )
                    }
                }
                HorizontalDividerMMD(thickness = 0.5.dp)
            }
            LazyColumnMMD(modifier = Modifier.weight(1f)) {
                itemsIndexed(spots, key = { _, spot -> spot.id }) { index, spot ->
                    SpotRow(
                        spot = spot,
                        catalogSpot = spot.catalogId?.let { catalogById[it] },
                        windUnit = settings.windUnit,
                        tempUnit = settings.tempUnit,
                        windColorMode = settings.windColorMode,
                        showForecast = preview != null,
                        forecastHour = preview?.values?.get(spot.id),
                        forecastLoading = preview?.loading == true,
                        showActions = actionsFor == spot.id,
                        canMoveUp = index > 0,
                        canMoveDown = index < spots.lastIndex,
                        onClick = {
                            actionsFor = null
                            onOpenSpot(spot)
                        },
                        onLongClick = {
                            actionsFor = if (actionsFor == spot.id) null else spot.id
                        },
                        onMoveUp = {
                            viewModel.moveSpotUp(spot.id)
                            actionsFor = null
                        },
                        onMoveDown = {
                            viewModel.moveSpotDown(spot.id)
                            actionsFor = null
                        },
                        onDelete = {
                            actionsFor = null
                            viewModel.deleteSpot(spot.id)
                        },
                        onInfo = spot.catalogId?.let { id -> { onOpenInfo(id) } }
                    )
                    HorizontalDividerMMD(thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun forecastTimeLabel(epochMs: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("EEE HH:mm", locale) }
    return formatter.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
}

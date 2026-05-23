package com.kompaktwind.ui.spots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kompaktwind.data.HourPoint
import com.kompaktwind.data.Spot
import com.kompaktwind.ui.KompaktWindViewModel
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD

@Composable
fun SpotsScreen(
    viewModel: KompaktWindViewModel,
    onAddSpot: () -> Unit,
    onOpenSpot: (Spot) -> Unit
) {
    val spots by viewModel.spots.collectAsState()
    val settings by viewModel.settingsState.collectAsState()

    val latestByMap: Map<String, HourPoint?> = remember(spots) { emptyMap() }

    if (spots.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextMMD(text = "No spots yet", fontSize = 20.sp, textAlign = TextAlign.Center)
                ButtonMMD(onClick = onAddSpot) { TextMMD("Add a spot") }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.CenterEnd) {
                ButtonMMD(onClick = onAddSpot) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add spot")
                }
            }
            LazyColumnMMD(modifier = Modifier.fillMaxSize()) {
                items(spots, key = { it.id }) { spot ->
                    SpotRow(
                        spot = spot,
                        latestHour = latestByMap[spot.id],
                        windUnit = settings.windUnit,
                        tempUnit = settings.tempUnit,
                        onClick = { onOpenSpot(spot) },
                        onLongClick = { viewModel.deleteSpot(spot.id) }
                    )
                    HorizontalDividerMMD()
                }
            }
        }
    }
}

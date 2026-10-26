package com.kompaktwind.ui.spots

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kompaktwind.R
import com.mudita.mmd.components.bottom_sheet.ModalBottomSheetMMD
import com.mudita.mmd.components.bottom_sheet.rememberModalBottomSheetMMDState
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastPreviewSheet(
    onDismiss: () -> Unit,
    onApply: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetMMDState(skipPartiallyExpanded = true)
    ModalBottomSheetMMD(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextMMD(
                text = stringResource(R.string.forecast_preview_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            val presets = listOf(
                stringResource(R.string.forecast_now) to 0L,
                stringResource(R.string.forecast_offset_hours, 3) to 180L,
                stringResource(R.string.forecast_offset_hours, 6) to 360L,
                stringResource(R.string.forecast_offset_hours, 12) to 720L,
                stringResource(R.string.forecast_tomorrow) to 1440L,
                stringResource(R.string.forecast_offset_days, 2) to 2880L
            )
            presets.chunked(2).forEach { presetRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetRow.forEach { (label, minutes) ->
                        OutlinedButtonMMD(
                            onClick = { onApply(minutes) },
                            modifier = Modifier.weight(1f)
                        ) {
                            TextMMD(
                                text = label,
                                fontSize = 24.sp,
                                lineHeight = 24.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

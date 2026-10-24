package com.kompaktwind.ui.forecast

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kompaktwind.R
import com.kompaktwind.ui.KompaktWindViewModel
import com.kompaktwind.ui.common.UnitFormatters
import com.mudita.mmd.components.text.TextMMD

@Composable
fun ForecastHelpScreen(viewModel: KompaktWindViewModel) {
    val settings by viewModel.settingsState.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextMMD(
            text = stringResource(R.string.help_title),
            fontSize = 24.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold
        )
        TextMMD(
            text = stringResource(
                R.string.help_text,
                UnitFormatters.windUnitLabel(settings.windUnit),
                UnitFormatters.tempUnitLabel(settings.tempUnit)
            ),
            fontSize = 18.sp,
            lineHeight = 24.sp,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

package com.kompaktwind.ui.forecast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.text.TextMMD
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DayHeader(epochMs: Long, timezone: String) {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(timezone, locale) {
        DateTimeFormatter.ofPattern("EEE d MMM", locale).withZone(ZoneId.of(timezone))
    }
    TextMMD(
        text = formatter.format(Instant.ofEpochMilli(epochMs)).uppercase(locale),
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

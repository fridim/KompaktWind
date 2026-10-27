package com.kompaktwind.ui.forecast

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kompaktwind.R
import com.kompaktwind.ui.KompaktWindViewModel
import com.kompaktwind.ui.common.UnitFormatters
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD

@Composable
fun ForecastHelpScreen(viewModel: KompaktWindViewModel) {
    val settings by viewModel.settingsState.collectAsState()
    val helpText = stringResource(
        R.string.help_text,
        UnitFormatters.windUnitLabel(settings.windUnit),
        UnitFormatters.tempUnitLabel(settings.tempUnit)
    )
    val tideText = stringResource(R.string.help_tide)
    val bodyStyle = LocalTextStyle.current.copy(fontSize = 18.sp, lineHeight = 24.sp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextMMD(
            text = stringResource(R.string.help_title),
            fontSize = 24.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold
        )
        PagedHelpText(
            text = "$helpText\n\n$tideText",
            style = bodyStyle,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 12.dp)
        )
    }
}

@Composable
private fun PagedHelpText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    val measurer = rememberTextMeasurer()
    val scrollbarReservePx = with(LocalDensity.current) { 40.dp.roundToPx() }
    BoxWithConstraints(modifier = modifier) {
        val widthPx = (constraints.maxWidth - scrollbarReservePx).coerceAtLeast(1)
        val heightPx = constraints.maxHeight
        val pages = remember(text, style, widthPx, heightPx, measurer) {
            paginateHelpText(text, style, widthPx, heightPx, measurer)
        }
        val listState = rememberLazyListState()
        LazyColumnMMD(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            scrollStep = 1
        ) {
            items(count = pages.size) { index ->
                TextMMD(
                    text = pages[index],
                    style = style,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun paginateHelpText(
    text: String,
    style: TextStyle,
    widthPx: Int,
    heightPx: Int,
    measurer: TextMeasurer
): List<String> {
    if (text.isBlank() || widthPx <= 0 || heightPx <= 0) return listOf(text)
    val layout = measurer.measure(
        text = AnnotatedString(text),
        style = style,
        constraints = Constraints(maxWidth = widthPx)
    )
    if (layout.lineCount == 0) return listOf(text)

    val pages = mutableListOf<String>()
    var startLine = 0
    while (startLine < layout.lineCount) {
        val pageTop = layout.getLineTop(startLine)
        var endLine = startLine
        while (endLine + 1 < layout.lineCount &&
            layout.getLineBottom(endLine + 1) - pageTop <= heightPx
        ) {
            endLine++
        }
        val startChar = layout.getLineStart(startLine)
        val endChar = layout.getLineEnd(endLine, visibleEnd = true)
        val page = text.substring(startChar, endChar).trim()
        if (page.isNotEmpty()) pages += page
        startLine = endLine + 1
    }
    return pages.ifEmpty { listOf(text) }
}

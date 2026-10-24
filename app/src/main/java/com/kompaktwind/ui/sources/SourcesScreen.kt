package com.kompaktwind.ui.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kompaktwind.R
import com.kompaktwind.data.DataSource
import com.kompaktwind.ui.KompaktWindViewModel
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD

@Composable
fun SourcesScreen(
    viewModel: KompaktWindViewModel,
    spotId: String,
    onBack: () -> Unit
) {
    val spots by viewModel.spots.collectAsState()
    val spot = spots.firstOrNull { it.id == spotId }

    if (spot == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            TextMMD(text = stringResource(R.string.spot_not_found), fontSize = 18.sp)
        }
        return
    }

    val current = DataSource.effectiveFor(spot.providerId, spot.lat, spot.lon)
    val sources = DataSource.availableAt(spot.lat, spot.lon)

    LazyColumnMMD(modifier = Modifier.fillMaxSize()) {
        items(items = sources, key = { it.providerId }) { source ->
            val selected = source == current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (!selected) viewModel.setSpotProvider(spot.id, source.providerId)
                        onBack()
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextMMD(
                        text = source.label,
                        fontSize = 21.sp,
                        lineHeight = 21.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (selected) {
                        TextMMD(
                            text = stringResource(R.string.source_current),
                            fontSize = 14.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.onBackground,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                val countryRes = countryRes(source.country)
                val country = if (countryRes != null) stringResource(countryRes) else source.country
                val horizonRes = horizonRes(source.horizon)
                val horizon = if (horizonRes != null) stringResource(horizonRes) else source.horizon
                TextMMD(
                    text = "${source.agency} · $country · ${source.resolution} · $horizon",
                    fontSize = 16.sp,
                    lineHeight = 16.sp
                )
            }
            HorizontalDividerMMD(thickness = 1.dp)
        }
    }
}

private fun countryRes(country: String): Int? = when (country) {
    "France" -> R.string.country_france
    "Netherlands" -> R.string.country_netherlands
    "Denmark" -> R.string.country_denmark
    "United Kingdom" -> R.string.country_uk
    "Germany" -> R.string.country_germany
    "Switzerland" -> R.string.country_switzerland
    "Europe" -> R.string.country_europe
    "USA" -> R.string.country_usa
    "Canada" -> R.string.country_canada
    "Global" -> R.string.country_global
    else -> null
}

private fun horizonRes(horizon: String): Int? = when (horizon) {
    "up to 7 d" -> R.string.horizon_up_to_7d
    "~2 d" -> R.string.horizon_2d
    "~2.5 d" -> R.string.horizon_2_5d
    "~2.3 d" -> R.string.horizon_2_3d
    "~1.2 d" -> R.string.horizon_1_2d
    "~4 d" -> R.string.horizon_4d
    "~5 d" -> R.string.horizon_5d
    else -> null
}

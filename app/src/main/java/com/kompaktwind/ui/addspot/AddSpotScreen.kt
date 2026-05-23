package com.kompaktwind.ui.addspot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kompaktwind.ui.KompaktWindViewModel
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.checkbox.CheckboxMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.tabs.PrimaryTabRowMMD
import com.mudita.mmd.components.tabs.TabMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AddSpotScreen(
    viewModel: KompaktWindViewModel,
    onSaved: () -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRowMMD(selectedTabIndex = tabIndex) {
            TabMMD(selected = tabIndex == 0, onClick = { tabIndex = 0 }) { TextMMD("Search") }
            TabMMD(selected = tabIndex == 1, onClick = { tabIndex = 1 }) { TextMMD("Coordinates") }
        }
        HorizontalDividerMMD()
        when (tabIndex) {
            0 -> SearchTab(viewModel = viewModel, onSaved = onSaved)
            else -> CoordinatesTab(viewModel = viewModel, onSaved = onSaved)
        }
    }
}

@Composable
private fun SearchTab(viewModel: KompaktWindViewModel, onSaved: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val results by viewModel.searchResults.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextFieldMMD(
            value = query,
            onValueChange = {
                query = it
                viewModel.onSearchQueryChanged(it)
            },
            placeholder = { TextMMD("Place name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        LazyColumnMMD {
            items(results, key = { it.id }) { result ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    TextMMD(text = result.name, fontWeight = FontWeight.Bold)
                    val subtitle = listOfNotNull(result.admin1, result.country).joinToString(", ")
                    if (subtitle.isNotEmpty()) TextMMD(text = subtitle, fontSize = 14.sp)
                    Row {
                        ButtonMMD(onClick = {
                            viewModel.addSpot(
                                name = result.name,
                                lat = result.latitude,
                                lon = result.longitude,
                                isCoastal = false
                            )
                            viewModel.clearSearch()
                            onSaved()
                        }) { TextMMD("Save (inland)") }
                        Spacer(Modifier.width(8.dp))
                        ButtonMMD(onClick = {
                            viewModel.addSpot(
                                name = result.name,
                                lat = result.latitude,
                                lon = result.longitude,
                                isCoastal = true
                            )
                            viewModel.clearSearch()
                            onSaved()
                        }) { TextMMD("Save (coastal)") }
                    }
                }
                HorizontalDividerMMD()
            }
        }
    }
}

@Composable
private fun CoordinatesTab(viewModel: KompaktWindViewModel, onSaved: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }
    var coastal by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextFieldMMD(value = name, onValueChange = { name = it }, placeholder = { TextMMD("Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        TextFieldMMD(value = lat, onValueChange = { lat = it }, placeholder = { TextMMD("Latitude (-90..90)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        TextFieldMMD(value = lon, onValueChange = { lon = it }, placeholder = { TextMMD("Longitude (-180..180)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row {
            CheckboxMMD(checked = coastal, onCheckedChange = { coastal = it })
            TextMMD("Coastal spot (show waves + water temp)")
        }
        Spacer(Modifier.height(16.dp))
        ButtonMMD(
            enabled = name.isNotBlank() && lat.toDoubleOrNull() != null && lon.toDoubleOrNull() != null,
            onClick = {
                viewModel.addSpot(name.trim(), lat.toDouble(), lon.toDouble(), coastal)
                onSaved()
            }
        ) { TextMMD("Save spot") }
    }
}

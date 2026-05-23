package com.kompaktwind.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kompaktwind.data.MarineDisplay
import com.kompaktwind.data.TempUnit
import com.kompaktwind.data.WindUnit
import com.kompaktwind.ui.KompaktWindViewModel
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.menus.DropdownMenuItemMMD
import com.mudita.mmd.components.menus.DropdownMenuMMD
import com.mudita.mmd.components.text.TextMMD

@Composable
fun SettingsScreen(viewModel: KompaktWindViewModel) {
    val s by viewModel.settingsState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {

        SettingRow("Wind units") {
            DropdownPicker(
                current = s.windUnit.name,
                options = WindUnit.entries.map { it.name },
                onSelect = { viewModel.setWindUnit(WindUnit.valueOf(it)) }
            )
        }
        HorizontalDividerMMD()

        SettingRow("Temperature units") {
            DropdownPicker(
                current = s.tempUnit.name,
                options = TempUnit.entries.map { it.name },
                onSelect = { viewModel.setTempUnit(TempUnit.valueOf(it)) }
            )
        }
        HorizontalDividerMMD()

        SettingRow("Wave/water shown") {
            DropdownPicker(
                current = s.marineDisplay.name,
                options = MarineDisplay.entries.map { it.name },
                onSelect = { viewModel.setMarineDisplay(MarineDisplay.valueOf(it)) }
            )
        }
        HorizontalDividerMMD()

        SettingRow("Cache TTL (minutes)") {
            DropdownPicker(
                current = s.cacheTtlMinutes.toString(),
                options = listOf("15", "30", "60", "120"),
                onSelect = { viewModel.setCacheTtl(it.toInt()) }
            )
        }
        HorizontalDividerMMD()

        SettingRow("Data provider") {
            TextMMD(text = s.providerId)
        }
        HorizontalDividerMMD()

        TextMMD(
            text = "Data: Open-Meteo (open-meteo.com)",
            fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 12.dp)
        )
    }
}

@Composable
private fun SettingRow(label: String, control: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        TextMMD(text = label, modifier = Modifier.weight(1f))
        control()
    }
}

@Composable
private fun DropdownPicker(current: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ButtonMMD(onClick = { expanded = true }) { TextMMD(current) }
        DropdownMenuMMD(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItemMMD(text = { TextMMD(opt) }, onClick = {
                    onSelect(opt); expanded = false
                })
            }
        }
    }
}

package com.kompaktwind.ui.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kompaktwind.R
import com.kompaktwind.data.DistanceUnit
import com.kompaktwind.data.LanguageManager
import com.kompaktwind.data.MarineDisplay
import com.kompaktwind.data.TempUnit
import com.kompaktwind.data.WindColorMode
import com.kompaktwind.data.WindUnit
import com.kompaktwind.data.catalog.ImportFailure
import com.kompaktwind.data.catalog.ImportOutcome
import com.kompaktwind.ui.KompaktWindViewModel
import com.kompaktwind.ui.common.DashedDivider
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.menus.DropdownMenuItemMMD
import com.mudita.mmd.components.menus.DropdownMenuMMD
import com.mudita.mmd.components.switcher.SwitchMMD
import com.mudita.mmd.components.text.TextMMD

@Composable
fun SettingsScreen(viewModel: KompaktWindViewModel) {
    val s by viewModel.settingsState.collectAsState()
    val languageTag by viewModel.languageTag.collectAsState()
    val localImports by viewModel.localImports.collectAsState()
    val importing by viewModel.importingSpots.collectAsState()
    val lastImport by viewModel.lastImport.collectAsState()
    val context = LocalContext.current
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importSpots(uri)
    }

    val systemLabel = stringResource(R.string.language_system)
    val languageLabels = LanguageManager.languages.map {
        if (it.tag == null) systemLabel else it.label
    }
    val currentLanguageIndex = LanguageManager.languages
        .indexOfFirst { it.tag == languageTag }
        .coerceAtLeast(0)

    val windLabels = listOf(
        stringResource(R.string.unit_ms),
        stringResource(R.string.unit_kmh),
        stringResource(R.string.unit_kn),
        stringResource(R.string.unit_mph)
    )
    val tempLabels = listOf(
        stringResource(R.string.temp_c),
        stringResource(R.string.temp_f)
    )
    val marineLabels = listOf(
        stringResource(R.string.marine_auto),
        stringResource(R.string.marine_always),
        stringResource(R.string.marine_never)
    )
    val distanceLabels = listOf(
        stringResource(R.string.unit_km),
        stringResource(R.string.unit_mi)
    )
    val windColorLabels = listOf(
        stringResource(R.string.wind_colors_mono),
        stringResource(R.string.wind_colors_color)
    )

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {

        SettingRow(stringResource(R.string.settings_language)) {
            DropdownPicker(
                current = languageLabels[currentLanguageIndex],
                options = languageLabels,
                onSelect = { selected ->
                    val tag = if (selected == systemLabel) {
                        null
                    } else {
                        LanguageManager.languages.firstOrNull { it.label == selected }?.tag
                    }
                    viewModel.setLanguage(tag)
                    (context as? Activity)?.recreate()
                }
            )
        }
        DashedDivider()

        SettingRow(stringResource(R.string.settings_wind_units)) {
            DropdownPicker(
                current = windLabels[WindUnit.entries.indexOf(s.windUnit)],
                options = windLabels,
                onSelect = { selected ->
                    val index = windLabels.indexOf(selected)
                    if (index >= 0) viewModel.setWindUnit(WindUnit.entries[index])
                }
            )
        }
        DashedDivider()

        SettingRow(stringResource(R.string.settings_temp_units)) {
            DropdownPicker(
                current = tempLabels[TempUnit.entries.indexOf(s.tempUnit)],
                options = tempLabels,
                onSelect = { selected ->
                    val index = tempLabels.indexOf(selected)
                    if (index >= 0) viewModel.setTempUnit(TempUnit.entries[index])
                }
            )
        }
        DashedDivider()

        SettingRow(stringResource(R.string.settings_distance_units)) {
            DropdownPicker(
                current = distanceLabels[DistanceUnit.entries.indexOf(s.distanceUnit)],
                options = distanceLabels,
                onSelect = { selected ->
                    val index = distanceLabels.indexOf(selected)
                    if (index >= 0) viewModel.setDistanceUnit(DistanceUnit.entries[index])
                }
            )
        }
        DashedDivider()

        SettingRow(stringResource(R.string.settings_wind_colors)) {
            DropdownPicker(
                current = windColorLabels[WindColorMode.entries.indexOf(s.windColorMode)],
                options = windColorLabels,
                onSelect = { selected ->
                    val index = windColorLabels.indexOf(selected)
                    if (index >= 0) viewModel.setWindColorMode(WindColorMode.entries[index])
                }
            )
        }
        DashedDivider()

        SettingRow(stringResource(R.string.settings_wave_water)) {
            DropdownPicker(
                current = marineLabels[MarineDisplay.entries.indexOf(s.marineDisplay)],
                options = marineLabels,
                onSelect = { selected ->
                    val index = marineLabels.indexOf(selected)
                    if (index >= 0) viewModel.setMarineDisplay(MarineDisplay.entries[index])
                }
            )
        }
        DashedDivider()

        SettingRow(stringResource(R.string.settings_dark_screen)) {
            SwitchMMD(
                checked = s.darkScreen,
                onCheckedChange = { viewModel.setDarkScreen(it) }
            )
        }
        DashedDivider()

        TextMMD(
            text = stringResource(R.string.settings_sources),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
        DashedDivider()

        SettingRow(stringResource(R.string.import_private_json)) {
            OutlinedButtonMMD(
                onClick = {
                    importLauncher.launch(
                        arrayOf("application/json", "text/plain", "application/octet-stream")
                    )
                },
                enabled = !importing
            ) {
                TextMMD(
                    text = stringResource(R.string.import_button),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        DashedDivider()

        TextMMD(
            text = stringResource(R.string.import_private_note),
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        if (importing) {
            TextMMD(
                text = stringResource(R.string.importing_spots),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        lastImport?.let { ImportStatus(it) }
        localImports.forEach { info ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextMMD(
                    text = stringResource(R.string.import_file_summary, info.fileName, info.spots),
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.deleteImport(info.id) }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.import_delete)
                    )
                }
            }
        }
        DashedDivider()

        SettingRow("OpenStreetMap") {
            SwitchMMD(
                checked = "openstreetmap.org" in s.enabledSources,
                onCheckedChange = { viewModel.setSourceEnabled("openstreetmap.org", it) }
            )
        }
        DashedDivider()

        SettingRow(stringResource(R.string.settings_cache_ttl)) {
            DropdownPicker(
                current = s.cacheTtlMinutes.toString(),
                options = listOf("15", "30", "60", "120"),
                onSelect = { viewModel.setCacheTtl(it.toInt()) }
            )
        }
        DashedDivider()

        TextMMD(
            text = stringResource(R.string.attribution_open_meteo),
            fontSize = 14.sp,
            modifier = Modifier.padding(vertical = 12.dp)
        )
    }
}

@Composable
private fun ImportStatus(outcome: ImportOutcome) {
    val text = when (outcome) {
        is ImportOutcome.Success -> if (outcome.info.skipped > 0) {
            stringResource(
                R.string.import_result_skipped,
                outcome.info.spots,
                outcome.info.fileName,
                outcome.info.skipped
            )
        } else {
            stringResource(R.string.import_result, outcome.info.spots, outcome.info.fileName)
        }
        is ImportOutcome.Failure -> stringResource(
            R.string.import_failed,
            stringResource(importFailureReason(outcome.reason))
        )
    }
    TextMMD(text = text, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
}

private fun importFailureReason(reason: ImportFailure): Int = when (reason) {
    ImportFailure.INVALID_JSON -> R.string.import_reason_invalid_json
    ImportFailure.TOO_LARGE -> R.string.import_reason_too_large
    ImportFailure.TOO_MANY -> R.string.import_reason_too_many
    ImportFailure.NO_VALID_SPOTS -> R.string.import_reason_no_valid
    ImportFailure.IO -> R.string.import_reason_io
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
        ButtonMMD(onClick = { expanded = true }) { TextMMD(current, fontWeight = FontWeight.Bold) }
        DropdownMenuMMD(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItemMMD(text = { TextMMD(opt) }, onClick = {
                    onSelect(opt); expanded = false
                })
            }
        }
    }
}

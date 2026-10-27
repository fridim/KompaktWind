package com.kompaktwind.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "kompaktwind_settings")

data class SettingsState(
    val windUnit: WindUnit = WindUnit.MS,
    val tempUnit: TempUnit = TempUnit.C,
    val marineDisplay: MarineDisplay = MarineDisplay.AUTO,
    val showTides: Boolean = false,
    val cacheTtlMinutes: Int = 30,
    val darkScreen: Boolean = false,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val windColorMode: WindColorMode = WindColorMode.MONO,
    val enabledSources: Set<String> = com.kompaktwind.data.catalog.ALL_SOURCES
)

class SettingsManager(
    context: Context,
    private val dataStore: DataStore<Preferences> = context.dataStore
) {

    private object Keys {
        val WIND_UNIT = stringPreferencesKey("wind_unit")
        val TEMP_UNIT = stringPreferencesKey("temp_unit")
        val MARINE = stringPreferencesKey("marine_display")
        val SHOW_TIDES = booleanPreferencesKey("show_tides")
        val TTL = intPreferencesKey("cache_ttl_minutes")
        val DARK_SCREEN = booleanPreferencesKey("dark_screen")
        val DISTANCE = stringPreferencesKey("distance_unit")
        val WIND_COLOR = stringPreferencesKey("wind_color_mode")
        val ENABLED_SOURCES = stringSetPreferencesKey("enabled_sources")
    }

    val flow: Flow<SettingsState> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { p -> p.toState() }

    suspend fun setWindUnit(u: WindUnit) = dataStore.edit { it[Keys.WIND_UNIT] = u.name }
    suspend fun setTempUnit(u: TempUnit) = dataStore.edit { it[Keys.TEMP_UNIT] = u.name }
    suspend fun setMarineDisplay(m: MarineDisplay) = dataStore.edit { it[Keys.MARINE] = m.name }
    suspend fun setShowTides(enabled: Boolean) = dataStore.edit { it[Keys.SHOW_TIDES] = enabled }
    suspend fun setCacheTtlMinutes(m: Int) = dataStore.edit { it[Keys.TTL] = m }
    suspend fun setDarkScreen(enabled: Boolean) = dataStore.edit { it[Keys.DARK_SCREEN] = enabled }
    suspend fun setDistanceUnit(unit: DistanceUnit) = dataStore.edit { it[Keys.DISTANCE] = unit.name }
    suspend fun setWindColorMode(mode: WindColorMode) = dataStore.edit { it[Keys.WIND_COLOR] = mode.name }
    suspend fun setSourceEnabled(source: String, enabled: Boolean) = dataStore.edit { prefs ->
        val allowed = com.kompaktwind.data.catalog.ALL_SOURCES
        val current = (prefs[Keys.ENABLED_SOURCES] ?: allowed).intersect(allowed)
        prefs[Keys.ENABLED_SOURCES] = if (enabled) current + source else current - source
    }

    private fun Preferences.toState(): SettingsState = SettingsState(
        windUnit = this[Keys.WIND_UNIT]?.let { runCatching { WindUnit.valueOf(it) }.getOrNull() } ?: WindUnit.MS,
        tempUnit = this[Keys.TEMP_UNIT]?.let { runCatching { TempUnit.valueOf(it) }.getOrNull() } ?: TempUnit.C,
        marineDisplay = this[Keys.MARINE]?.let { runCatching { MarineDisplay.valueOf(it) }.getOrNull() } ?: MarineDisplay.AUTO,
        showTides = this[Keys.SHOW_TIDES] ?: false,
        cacheTtlMinutes = this[Keys.TTL] ?: 30,
        darkScreen = this[Keys.DARK_SCREEN] ?: false,
        distanceUnit = this[Keys.DISTANCE]?.let { runCatching { DistanceUnit.valueOf(it) }.getOrNull() } ?: DistanceUnit.KM,
        windColorMode = this[Keys.WIND_COLOR]?.let { runCatching { WindColorMode.valueOf(it) }.getOrNull() } ?: WindColorMode.MONO,
        enabledSources = this[Keys.ENABLED_SOURCES]
            ?.intersect(com.kompaktwind.data.catalog.ALL_SOURCES)
            ?: com.kompaktwind.data.catalog.ALL_SOURCES
    )
}

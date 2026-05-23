package com.kompaktwind.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "kompaktwind_settings")

data class SettingsState(
    val windUnit: WindUnit = WindUnit.MS,
    val tempUnit: TempUnit = TempUnit.C,
    val marineDisplay: MarineDisplay = MarineDisplay.AUTO,
    val providerId: String = "open-meteo",
    val cacheTtlMinutes: Int = 30
)

class SettingsManager(private val context: Context) {

    private object Keys {
        val WIND_UNIT = stringPreferencesKey("wind_unit")
        val TEMP_UNIT = stringPreferencesKey("temp_unit")
        val MARINE = stringPreferencesKey("marine_display")
        val PROVIDER = stringPreferencesKey("provider_id")
        val TTL = intPreferencesKey("cache_ttl_minutes")
    }

    val flow: Flow<SettingsState> = context.dataStore.data.map { p -> p.toState() }

    suspend fun setWindUnit(u: WindUnit) = context.dataStore.edit { it[Keys.WIND_UNIT] = u.name }
    suspend fun setTempUnit(u: TempUnit) = context.dataStore.edit { it[Keys.TEMP_UNIT] = u.name }
    suspend fun setMarineDisplay(m: MarineDisplay) = context.dataStore.edit { it[Keys.MARINE] = m.name }
    suspend fun setProvider(id: String) = context.dataStore.edit { it[Keys.PROVIDER] = id }
    suspend fun setCacheTtlMinutes(m: Int) = context.dataStore.edit { it[Keys.TTL] = m }

    private fun Preferences.toState(): SettingsState = SettingsState(
        windUnit = this[Keys.WIND_UNIT]?.let { runCatching { WindUnit.valueOf(it) }.getOrNull() } ?: WindUnit.MS,
        tempUnit = this[Keys.TEMP_UNIT]?.let { runCatching { TempUnit.valueOf(it) }.getOrNull() } ?: TempUnit.C,
        marineDisplay = this[Keys.MARINE]?.let { runCatching { MarineDisplay.valueOf(it) }.getOrNull() } ?: MarineDisplay.AUTO,
        providerId = this[Keys.PROVIDER] ?: "open-meteo",
        cacheTtlMinutes = this[Keys.TTL] ?: 30
    )
}

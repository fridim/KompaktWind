package com.kompaktwind.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kompaktwind.data.ForecastRepository
import com.kompaktwind.data.ForecastUiState
import com.kompaktwind.data.MarineDisplay
import com.kompaktwind.data.SettingsManager
import com.kompaktwind.data.SettingsState
import com.kompaktwind.data.Spot
import com.kompaktwind.data.SpotDao
import com.kompaktwind.data.SpotEntity
import com.kompaktwind.data.TempUnit
import com.kompaktwind.data.WindUnit
import com.kompaktwind.data.provider.geocoding.GeocodingResultDto
import com.kompaktwind.data.provider.geocoding.GeocodingService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class KompaktWindViewModel(
    private val spotDao: SpotDao,
    private val forecastRepository: ForecastRepository,
    private val geocoding: GeocodingService,
    private val settings: SettingsManager
) : ViewModel() {

    val spots: StateFlow<List<Spot>> = spotDao.observeAll()
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val settingsState: StateFlow<SettingsState> = settings.flow
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState())

    private val _forecast = MutableStateFlow<ForecastUiState>(ForecastUiState.Loading)
    val forecast: StateFlow<ForecastUiState> = _forecast.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GeocodingResultDto>>(emptyList())
    val searchResults: StateFlow<List<GeocodingResultDto>> = _searchResults.asStateFlow()
    private var searchJob: Job? = null

    fun addSpot(name: String, lat: Double, lon: Double, isCoastal: Boolean) {
        viewModelScope.launch {
            spotDao.upsert(SpotEntity(UUID.randomUUID().toString(), name, lat, lon, isCoastal, System.currentTimeMillis()))
        }
    }

    fun renameSpot(id: String, newName: String) {
        viewModelScope.launch {
            val s = spotDao.get(id) ?: return@launch
            spotDao.upsert(s.copy(name = newName))
        }
    }

    fun deleteSpot(id: String) { viewModelScope.launch { spotDao.delete(id) } }

    fun loadForecast(spot: Spot, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _forecast.value = ForecastUiState.Loading
            _forecast.value = forecastRepository.getForecast(
                spot = spot,
                providerId = settingsState.value.providerId,
                forceRefresh = forceRefresh
            )
        }
    }

    fun onSearchQueryChanged(q: String) {
        searchJob?.cancel()
        if (q.isBlank()) { _searchResults.value = emptyList(); return }
        searchJob = viewModelScope.launch {
            delay(400)
            geocoding.search(q).onSuccess { _searchResults.value = it }
        }
    }

    fun clearSearch() { _searchResults.value = emptyList() }

    fun setWindUnit(u: WindUnit) = viewModelScope.launch { settings.setWindUnit(u) }
    fun setTempUnit(u: TempUnit) = viewModelScope.launch { settings.setTempUnit(u) }
    fun setMarineDisplay(m: MarineDisplay) = viewModelScope.launch { settings.setMarineDisplay(m) }
    fun setProvider(id: String) = viewModelScope.launch { settings.setProvider(id) }
    fun setCacheTtl(min: Int) = viewModelScope.launch { settings.setCacheTtlMinutes(min) }

    private fun SpotEntity.toDomain() = Spot(id, name, lat, lon, isCoastal, createdAt)
}

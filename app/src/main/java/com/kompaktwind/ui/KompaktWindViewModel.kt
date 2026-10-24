package com.kompaktwind.ui

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kompaktwind.data.DataSource
import com.kompaktwind.data.ForecastRepository
import com.kompaktwind.data.ForecastUiState
import com.kompaktwind.data.LanguageManager
import com.kompaktwind.data.MarineDisplay
import com.kompaktwind.data.SettingsManager
import com.kompaktwind.data.SettingsState
import com.kompaktwind.data.Spot
import com.kompaktwind.data.SpotDao
import com.kompaktwind.data.SpotEntity
import com.kompaktwind.data.TempUnit
import com.kompaktwind.data.WindColorMode
import com.kompaktwind.data.WindUnit
import com.kompaktwind.data.catalog.CatalogState
import com.kompaktwind.data.catalog.ImportFailure
import com.kompaktwind.data.catalog.ImportOutcome
import com.kompaktwind.data.catalog.LocalImportInfo
import com.kompaktwind.data.catalog.SpotCatalogRepository
import com.kompaktwind.ui.spots.ForecastPreviewState
import com.kompaktwind.ui.spots.nearestHour
import com.kompaktwind.ui.spots.previewTargetEpochMs
import com.kompaktwind.data.live.LiveStationRepository
import com.kompaktwind.data.live.MetarRepository
import com.kompaktwind.data.live.NearbyStationsState
import com.kompaktwind.data.live.StationDetailState
import com.kompaktwind.data.provider.geocoding.GeocodingResultDto
import com.kompaktwind.data.provider.geocoding.GeocodingService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.UUID

class KompaktWindViewModel(
    private val spotDao: SpotDao,
    private val forecastRepository: ForecastRepository,
    private val geocoding: GeocodingService,
    private val settings: SettingsManager,
    private val language: LanguageManager,
    private val liveStations: LiveStationRepository,
    private val metar: MetarRepository,
    private val catalogRepository: SpotCatalogRepository,
    private val contentResolver: ContentResolver
) : ViewModel() {

    val spots: StateFlow<List<Spot>> = spotDao.observeAll()
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val settingsState: StateFlow<SettingsState> = settings.flow
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState())

    private val _languageTag = MutableStateFlow(language.currentTag())
    val languageTag: StateFlow<String?> = _languageTag.asStateFlow()

    private val _addSpotTabIndex = MutableStateFlow(0)
    val addSpotTabIndex: StateFlow<Int> = _addSpotTabIndex.asStateFlow()

    private val _addSpotDraft = MutableStateFlow(AddSpotDraft())
    val addSpotDraft: StateFlow<AddSpotDraft> = _addSpotDraft.asStateFlow()

    private val _nearbyStations = MutableStateFlow<NearbyStationsState>(NearbyStationsState.Loading)
    val nearbyStations: StateFlow<NearbyStationsState> = _nearbyStations.asStateFlow()
    private var nearbyJob: Job? = null

    private val _stationDetail = MutableStateFlow<StationDetailState>(StationDetailState.Loading)
    val stationDetail: StateFlow<StationDetailState> = _stationDetail.asStateFlow()
    private var stationJob: Job? = null

    private val _catalog = MutableStateFlow<CatalogState>(CatalogState.Loading)
    val catalog: StateFlow<CatalogState> = _catalog.asStateFlow()

    private val _localImports = MutableStateFlow<List<LocalImportInfo>>(emptyList())
    val localImports: StateFlow<List<LocalImportInfo>> = _localImports.asStateFlow()
    private val _importingSpots = MutableStateFlow(false)
    val importingSpots: StateFlow<Boolean> = _importingSpots.asStateFlow()
    private val _lastImport = MutableStateFlow<ImportOutcome?>(null)
    val lastImport: StateFlow<ImportOutcome?> = _lastImport.asStateFlow()

    init {
        refreshImports()
    }
    private var catalogJob: Job? = null

    private val _forecast = MutableStateFlow<ForecastUiState>(ForecastUiState.Loading)
    val forecast: StateFlow<ForecastUiState> = _forecast.asStateFlow()
    private var forecastJob: Job? = null

    private val _forecastPreview = MutableStateFlow<ForecastPreviewState?>(null)
    val forecastPreview: StateFlow<ForecastPreviewState?> = _forecastPreview.asStateFlow()
    private val _showForecastPicker = MutableStateFlow(false)
    val showForecastPicker: StateFlow<Boolean> = _showForecastPicker.asStateFlow()
    private var forecastPreviewJob: Job? = null

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GeocodingResultDto>>(emptyList())
    val searchResults: StateFlow<List<GeocodingResultDto>> = _searchResults.asStateFlow()
    private var searchJob: Job? = null

    fun addSpot(
        name: String,
        lat: Double,
        lon: Double,
        isCoastal: Boolean,
        shortName: String? = null,
        catalogId: String? = null
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            spotDao.upsert(
                SpotEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    lat = lat,
                    lon = lon,
                    isCoastal = isCoastal,
                    createdAt = now,
                    catalogId = catalogId,
                    shortName = shortName,
                    sortOrder = now
                )
            )
        }
    }

    fun renameSpot(id: String, newName: String) {
        viewModelScope.launch {
            val s = spotDao.get(id) ?: return@launch
            spotDao.upsert(s.copy(name = newName))
        }
    }

    fun deleteSpot(id: String) { viewModelScope.launch { spotDao.delete(id) } }

    fun moveSpotUp(id: String) { viewModelScope.launch { spotDao.move(id, -1) } }

    fun moveSpotDown(id: String) { viewModelScope.launch { spotDao.move(id, 1) } }

    fun loadForecast(spot: Spot, forceRefresh: Boolean = false) {
        forecastJob?.cancel()
        forecastJob = viewModelScope.launch {
            _forecast.value = ForecastUiState.Loading
            val state = forecastRepository.getForecast(
                spot = spot,
                providerId = DataSource.effectiveFor(spot.providerId, spot.lat, spot.lon).providerId,
                forceRefresh = forceRefresh
            )
            if (!isActive) return@launch
            if (state is ForecastUiState.Data && state.forecast.spotId != spot.id) return@launch
            _forecast.value = state
        }
    }

    fun openForecastPicker() {
        _showForecastPicker.value = true
    }

    fun dismissForecastPicker() {
        _showForecastPicker.value = false
    }

    fun clearForecastPreview() {
        forecastPreviewJob?.cancel()
        forecastPreviewJob = null
        _forecastPreview.value = null
        _showForecastPicker.value = false
    }

    fun startForecastPreview(offsetMinutes: Long) {
        _showForecastPicker.value = false
        forecastPreviewJob?.cancel()
        val target = previewTargetEpochMs(System.currentTimeMillis(), offsetMinutes)
        _forecastPreview.value = ForecastPreviewState(targetEpochMs = target)
        forecastPreviewJob = viewModelScope.launch {
            try {
                val spotsToLoad = spots.value
                val semaphore = Semaphore(3)
                coroutineScope {
                    spotsToLoad.forEach { spot ->
                        launch {
                            semaphore.withPermit {
                                val hour = try {
                                    val providerId = DataSource
                                        .effectiveFor(spot.providerId, spot.lat, spot.lon)
                                        .providerId
                                    val state = forecastRepository.getForecast(
                                        spot = spot,
                                        providerId = providerId
                                    )
                                    (state as? ForecastUiState.Data)
                                        ?.let { nearestHour(it.forecast.hours, target) }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (_: Exception) {
                                    null
                                }
                                if (hour != null) {
                                    _forecastPreview.update { preview ->
                                        if (preview == null || preview.targetEpochMs != target) {
                                            preview
                                        } else {
                                            preview.copy(values = preview.values + (spot.id to hour))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } finally {
                _forecastPreview.update { preview ->
                    preview?.takeIf { it.targetEpochMs == target }?.copy(loading = false)
                }
            }
        }
    }

    fun onSearchQueryChanged(q: String) {
        _searchQuery.value = q
        searchJob?.cancel()
        if (q.isBlank()) { _searchResults.value = emptyList(); return }
        searchJob = viewModelScope.launch {
            delay(400)
            geocoding.search(q).onSuccess { _searchResults.value = it }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    fun setWindUnit(u: WindUnit) = viewModelScope.launch { settings.setWindUnit(u) }
    fun setTempUnit(u: TempUnit) = viewModelScope.launch { settings.setTempUnit(u) }
    fun setMarineDisplay(m: MarineDisplay) = viewModelScope.launch { settings.setMarineDisplay(m) }
    fun setSpotProvider(id: String, providerId: String) = viewModelScope.launch { spotDao.updateProvider(id, providerId) }
    fun setCacheTtl(min: Int) = viewModelScope.launch { settings.setCacheTtlMinutes(min) }
    fun setDarkScreen(enabled: Boolean) = viewModelScope.launch { settings.setDarkScreen(enabled) }
    fun setDistanceUnit(unit: com.kompaktwind.data.DistanceUnit) = viewModelScope.launch { settings.setDistanceUnit(unit) }
    fun setWindColorMode(mode: WindColorMode) = viewModelScope.launch { settings.setWindColorMode(mode) }
    fun setSourceEnabled(source: String, enabled: Boolean) = viewModelScope.launch { settings.setSourceEnabled(source, enabled) }
    fun beginAddSpot() {
        _addSpotTabIndex.value = 0
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }
    fun setAddSpotTabIndex(index: Int) { _addSpotTabIndex.value = index }
    fun updateAddSpotDraft(update: (AddSpotDraft) -> AddSpotDraft) {
        _addSpotDraft.value = update(_addSpotDraft.value)
    }
    fun setLanguage(tag: String?) { language.setTag(tag); _languageTag.value = tag }

    fun loadNearbyStations(spot: Spot, forceRefresh: Boolean = false) {
        nearbyJob?.cancel()
        nearbyJob = viewModelScope.launch {
            _nearbyStations.value = NearbyStationsState.Loading
            val stations = runCatching {
                coroutineScope {
                    val openWindMap = async {
                        liveStations.nearby(spot.lat, spot.lon, forceRefresh = forceRefresh)
                            .getOrDefault(emptyList())
                    }
                    val airports = async {
                        metar.nearby(spot.lat, spot.lon, forceRefresh = forceRefresh)
                            .getOrDefault(emptyList())
                    }
                    (openWindMap.await() + airports.await())
                        .distinctBy { it.id }
                        .sortedBy { it.distanceKm }
                        .take(20)
                }
            }.getOrDefault(emptyList())
            _nearbyStations.value = if (stations.isEmpty()) {
                NearbyStationsState.Error("Live data unavailable")
            } else {
                NearbyStationsState.Data(stations, System.currentTimeMillis())
            }
        }
    }

    fun refreshImports() {
        viewModelScope.launch {
            _localImports.value = catalogRepository.imports()
        }
    }

    fun importSpots(uri: Uri) {
        if (_importingSpots.value) return
        viewModelScope.launch {
            _importingSpots.value = true
            try {
                val outcome = withContext(Dispatchers.IO) {
                    val name = displayName(uri) ?: uri.lastPathSegment?.substringAfterLast('/')
                        ?: "spots.json"
                    val stream = runCatching { contentResolver.openInputStream(uri) }.getOrNull()
                    if (stream == null) {
                        ImportOutcome.Failure(ImportFailure.IO)
                    } else {
                        catalogRepository.import(name, stream)
                    }
                }
                _lastImport.value = outcome
                if (outcome is ImportOutcome.Success) {
                    refreshImports()
                    loadCatalog()
                }
            } finally {
                _importingSpots.value = false
            }
        }
    }

    fun deleteImport(id: String) {
        viewModelScope.launch {
            catalogRepository.deleteImport(id)
            _lastImport.value = null
            refreshImports()
            loadCatalog()
        }
    }

    fun clearImportResult() {
        _lastImport.value = null
    }

    private fun displayName(uri: Uri): String? = runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
    }.getOrNull()

    fun loadCatalog() {
        catalogJob?.cancel()
        catalogJob = viewModelScope.launch {
            _catalog.value = CatalogState.Loading
            _catalog.value = catalogRepository.index().fold(
                onSuccess = { spots ->
                    syncSavedSpotCoordinates(spots)
                    CatalogState.Data(spots)
                },
                onFailure = { CatalogState.Error(it.message ?: "Catalog unavailable") }
            )
        }
    }

    private suspend fun syncSavedSpotCoordinates(spots: List<com.kompaktwind.data.catalog.CatalogSpot>) {
        val byId = spots.associateBy { it.id }
        spotDao.all().forEach { saved ->
            val catalogSpot = saved.catalogId?.let { byId[it] } ?: return@forEach
            if (saved.lat != catalogSpot.lat || saved.lon != catalogSpot.lon) {
                spotDao.upsert(saved.copy(lat = catalogSpot.lat, lon = catalogSpot.lon))
            }
        }
    }

    fun loadStation(stationId: String, forceRefresh: Boolean = false) {
        stationJob?.cancel()
        stationJob = viewModelScope.launch {
            _stationDetail.value = StationDetailState.Loading
            _stationDetail.value = runCatching {
                coroutineScope {
                    val icao = stationId.removePrefix("metar:")
                    val piouId = stationId.removePrefix("pioupiou:").toIntOrNull()
                    val isMetar = stationId.startsWith("metar:")
                    val liveDeferred = async {
                        if (isMetar) metar.live(icao, forceRefresh).getOrThrow()
                        else liveStations.live(
                            piouId ?: error("Unknown station $stationId"),
                            forceRefresh
                        ).getOrThrow()
                    }
                    val historyDeferred = async {
                        if (isMetar) metar.history(icao, forceRefresh = forceRefresh).getOrDefault(emptyList())
                        else liveStations.history(
                            piouId ?: error("Unknown station $stationId"),
                            forceRefresh = forceRefresh
                        ).getOrDefault(emptyList())
                    }
                    StationDetailState.Data(liveDeferred.await(), historyDeferred.await())
                }
            }.getOrElse {
                StationDetailState.Error(it.message ?: "Live data unavailable")
            }
        }
    }

private fun SpotEntity.toDomain() = Spot(id, name, lat, lon, isCoastal, createdAt, providerId, catalogId, shortName)
}

data class AddSpotDraft(
    val lat: Double = 46.6,
    val lon: Double = 2.4,
    val zoom: Double = 6.5,
    val selectedCatalogId: String? = null,
    val name: String = "",
    val shortName: String = "",
    val coastal: Boolean = true
)

package com.kompaktwind.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kompaktwind.AppContainer

class KompaktWindViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return KompaktWindViewModel(
            spotDao = container.db.spotDao(),
            forecastRepository = container.forecastRepository,
            geocoding = container.geocoding,
            settings = container.settings
        ) as T
    }
}

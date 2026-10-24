package com.kompaktwind.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.kompaktwind.R

sealed class Screen(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector
) {
    data object Spots : Screen("spots", R.string.nav_spots, Icons.Outlined.Place)
    data object NearMe : Screen("nearby", R.string.nav_near_me, Icons.Outlined.MyLocation)
    data object Settings : Screen("settings", R.string.nav_settings, Icons.Outlined.Settings)
    data object AddSpot : Screen("addspot", R.string.screen_add_spot, Icons.Outlined.Place)
    data object Forecast : Screen("forecast/{spotId}", R.string.screen_forecast, Icons.Outlined.Place) {
        fun routeFor(spotId: String) = "forecast/$spotId"
    }
    data object ForecastHelp : Screen("help", R.string.help_title, Icons.Outlined.Info)
    data object Sources : Screen("sources/{spotId}", R.string.screen_forecast_source, Icons.Outlined.Layers) {
        fun routeFor(spotId: String) = "sources/$spotId"
    }
    data object LiveStations : Screen("live/{spotId}", R.string.screen_live_stations, Icons.Outlined.Radar) {
        fun routeFor(spotId: String) = "live/$spotId"
    }
    data object StationDetail : Screen("station/{stationId}", R.string.screen_station_detail, Icons.Outlined.Radar) {
        fun routeFor(stationId: String) = "station/$stationId"
    }
    data object SpotInfo : Screen("info/{catalogId}", R.string.screen_spot_info, Icons.Outlined.Info) {
        fun routeFor(catalogId: String) = "info/$catalogId"
    }
}

val bottomNavItems = listOf(Screen.Spots, Screen.NearMe, Screen.Settings)

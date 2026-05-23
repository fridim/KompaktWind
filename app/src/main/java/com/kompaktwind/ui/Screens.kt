package com.kompaktwind.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Spots : Screen("spots", "Spots", Icons.Outlined.Place)
    data object Settings : Screen("settings", "Settings", Icons.Outlined.Settings)
    data object AddSpot : Screen("addspot", "Add spot", Icons.Outlined.Place)
    data object Forecast : Screen("forecast/{spotId}", "Forecast", Icons.Outlined.Place) {
        fun routeFor(spotId: String) = "forecast/$spotId"
    }
}

val bottomNavItems = listOf(Screen.Spots, Screen.Settings)

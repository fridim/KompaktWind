package com.kompaktwind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kompaktwind.data.ForecastUiState
import com.kompaktwind.ui.KompaktWindViewModel
import com.kompaktwind.ui.KompaktWindViewModelFactory
import com.kompaktwind.ui.Screen
import com.kompaktwind.ui.addspot.AddSpotScreen
import com.kompaktwind.ui.bottomNavItems
import com.kompaktwind.ui.forecast.ForecastScreen
import com.kompaktwind.ui.settings.SettingsScreen
import com.kompaktwind.ui.spots.SpotsScreen
import com.mudita.mmd.ThemeMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.nav_bar.NavigationBarItemMMD
import com.mudita.mmd.components.nav_bar.NavigationBarMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as KompaktWindApplication).container
        setContent {
            ThemeMMD {
                val viewModel: KompaktWindViewModel = viewModel(factory = KompaktWindViewModelFactory(container))
                val navController = rememberNavController()
                val backEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backEntry?.destination
                val canNavigateBack = navController.previousBackStackEntry != null
                val isForecast = currentDestination?.route?.startsWith("forecast/") == true

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        Column {
                            TopAppBarMMD(
                                title = { TextMMD(text = titleFor(currentDestination?.route, viewModel)) },
                                navigationIcon = {
                                    if (canNavigateBack) {
                                        IconButton(onClick = { navController.navigateUp() }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                        }
                                    }
                                },
                                actions = {
                                    if (isForecast) {
                                        ForecastActions(viewModel)
                                    }
                                }
                            )
                            HorizontalDividerMMD()
                        }
                    },
                    bottomBar = {
                        val onTopLevel = currentDestination?.route in bottomNavItems.map { it.route }
                        if (onTopLevel) {
                            NavigationBarMMD {
                                bottomNavItems.forEach { screen ->
                                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                    NavigationBarItemMMD(
                                        icon = { Icon(rememberVectorPainter(screen.icon), contentDescription = screen.label) },
                                        label = { TextMMD(screen.label) },
                                        selected = selected,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id)
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Spots.route,
                        modifier = Modifier.padding(padding),
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None }
                    ) {
                        composable(Screen.Spots.route) {
                            SpotsScreen(
                                viewModel = viewModel,
                                onAddSpot = { navController.navigate(Screen.AddSpot.route) },
                                onOpenSpot = { spot -> navController.navigate(Screen.Forecast.routeFor(spot.id)) }
                            )
                        }
                        composable(Screen.AddSpot.route) {
                            AddSpotScreen(
                                viewModel = viewModel,
                                onSaved = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Forecast.route) { entry ->
                            val spotId = entry.arguments?.getString("spotId") ?: return@composable
                            ForecastScreen(viewModel = viewModel, spotId = spotId)
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ForecastActions(viewModel: KompaktWindViewModel) {
    val forecast by viewModel.forecast.collectAsState()
    val spots by viewModel.spots.collectAsState()

    val fetchedAt = (forecast as? ForecastUiState.Data)?.fetchedAt
    if (fetchedAt != null) {
        val ageMs = (System.currentTimeMillis() - fetchedAt).coerceAtLeast(0)
        val label = when {
            ageMs < TimeUnit.MINUTES.toMillis(1) -> "now"
            ageMs < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(ageMs)}m"
            else -> "${TimeUnit.MILLISECONDS.toHours(ageMs)}h"
        }
        TextMMD(text = label, fontSize = 14.sp)
    }

    IconButton(onClick = {
        val data = forecast as? ForecastUiState.Data ?: return@IconButton
        val spot = spots.firstOrNull { it.id == data.forecast.spotId } ?: return@IconButton
        viewModel.loadForecast(spot, forceRefresh = true)
    }) {
        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
    }
}

@Composable
private fun titleFor(route: String?, viewModel: KompaktWindViewModel): String = when {
    route == Screen.Spots.route -> "Spots"
    route == Screen.AddSpot.route -> "Add spot"
    route?.startsWith("forecast/") == true -> {
        val id = route.substringAfter("forecast/")
        viewModel.spots.value.firstOrNull { it.id == id }?.name ?: "Forecast"
    }
    route == Screen.Settings.route -> "Settings"
    else -> "KompaktWind"
}

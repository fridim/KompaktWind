package com.kompaktwind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
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
                val spots by viewModel.spots.collectAsState()
                val canNavigateBack = navController.previousBackStackEntry != null
                val isForecast = currentDestination?.route?.startsWith("forecast/") == true

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        Column {
                            val isSpots = currentDestination?.route == Screen.Spots.route
                            TopAppBarMMD(
                                title = { TextMMD(text = titleFor(currentDestination?.route, backEntry?.arguments?.getString("spotId"), spots)) },
                                navigationIcon = {
                                    if (canNavigateBack) {
                                        IconButton(onClick = { navController.navigateUp() }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                        }
                                    }
                                },
                                actions = {
                                    if (isSpots) {
                                        IconButton(onClick = { navController.navigate(Screen.AddSpot.route) }) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(Color.Black, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Outlined.Add, contentDescription = "Add spot", tint = Color.White, modifier = Modifier.size(24.dp))
                                            }
                                        }
                                    }
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
                            Column {
                                HorizontalDividerMMD()
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    bottomNavItems.forEach { screen ->
                                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    navController.navigate(screen.route) {
                                                        popUpTo(navController.graph.findStartDestination().id)
                                                        launchSingleTop = true
                                                    }
                                                }
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Icon(rememberVectorPainter(screen.icon), contentDescription = screen.label, modifier = Modifier.size(28.dp))
                                            TextMMD(screen.label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                            if (selected) {
                                                Box(modifier = Modifier.padding(top = 4.dp).fillMaxWidth(0.6f).height(3.dp).background(Color.Black))
                                            }
                                        }
                                    }
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
private fun titleFor(route: String?, spotId: String?, spots: List<com.kompaktwind.data.Spot>): String = when {
    route == Screen.Spots.route -> "Spots"
    route == Screen.AddSpot.route -> "Add spot"
    route == Screen.Forecast.route -> {
        spotId?.let { id -> spots.firstOrNull { it.id == id }?.name } ?: "Forecast"
    }
    route == Screen.Settings.route -> "Settings"
    else -> "KompaktWind"
}

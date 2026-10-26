package com.kompaktwind

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kompaktwind.R
import com.kompaktwind.data.DataSource
import com.kompaktwind.data.ForecastUiState
import com.kompaktwind.data.LanguageManager
import com.kompaktwind.data.Spot
import com.kompaktwind.data.live.NearbyStationsState
import com.kompaktwind.data.live.StationDetailState
import com.kompaktwind.ui.KompaktWindViewModel
import com.kompaktwind.ui.KompaktWindViewModelFactory
import com.kompaktwind.ui.Screen
import com.kompaktwind.ui.addspot.AddSpotScreen
import com.kompaktwind.ui.common.freshnessText
import com.kompaktwind.ui.bottomNavItems
import com.kompaktwind.ui.forecast.ForecastHelpScreen
import com.kompaktwind.ui.forecast.ForecastScreen
import com.kompaktwind.ui.live.LiveStationsScreen
import com.kompaktwind.ui.live.StationDetailScreen
import com.kompaktwind.ui.nearby.NearMeScreen
import com.kompaktwind.ui.settings.SettingsScreen
import com.kompaktwind.ui.sources.SourcesScreen
import com.kompaktwind.ui.spots.SpotsScreen
import com.kompaktwind.ui.spots.SpotInfoScreen
import com.kompaktwind.ui.theme.KompaktWindTheme
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD

private const val APP_FONT_SCALE = 1.1f

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager(newBase).wrap(newBase))
    }

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as KompaktWindApplication).container
        setContent {
            val viewModel: KompaktWindViewModel = viewModel(factory = KompaktWindViewModelFactory(container))
            val settings by viewModel.settingsState.collectAsState()
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, density.fontScale * APP_FONT_SCALE)
            ) {
                KompaktWindTheme(darkScreen = settings.darkScreen) {
                val view = LocalView.current
                SideEffect {
                    val activity = view.context as? Activity ?: return@SideEffect
                    WindowCompat.getInsetsController(activity.window, view).apply {
                        isAppearanceLightStatusBars = !settings.darkScreen
                        isAppearanceLightNavigationBars = !settings.darkScreen
                    }
                }
                val navController = rememberNavController()
                val backEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backEntry?.destination
                val spotId = backEntry?.arguments?.getString("spotId")
                val spots by viewModel.spots.collectAsState()
                val nearby by viewModel.nearbyStations.collectAsState()
                val stationDetail by viewModel.stationDetail.collectAsState()
                val stationId = backEntry?.arguments?.getString("stationId")
                val canNavigateBack = navController.previousBackStackEntry != null
                val isForecast = currentDestination?.route?.startsWith("forecast/") == true
                val currentSpot = spots.firstOrNull { it.id == spotId }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        Column {
                            if (isForecast) {
                                ForecastTopBar(
                                    title = titleFor(currentDestination?.route, spotId, spots),
                                    spot = currentSpot,
                                    canNavigateBack = canNavigateBack,
                                    onBack = { navController.navigateUp() },
                                    onOpenSources = {
                                        spotId?.let { navController.navigate(Screen.Sources.routeFor(it)) }
                                    },
                                    onOpenInfo = currentSpot?.catalogId?.let { catalogId ->
                                        { navController.navigate(Screen.SpotInfo.routeFor(catalogId)) }
                                    },
                                    viewModel = viewModel
                                )
                            } else {
                                val isSpots = currentDestination?.route == Screen.Spots.route
                                val isLive = currentDestination?.route == Screen.LiveStations.route
                                val isStationDetail = currentDestination?.route == Screen.StationDetail.route
                                TopAppBarMMD(
                                    title = { TextMMD(text = titleFor(currentDestination?.route, spotId, spots)) },
                                    navigationIcon = {
                                        if (canNavigateBack) {
                                            IconButton(onClick = { navController.navigateUp() }) {
                                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                                            }
                                        }
                                    },
                                    actions = {
                                        if (isSpots) {
                                            IconButton(onClick = { viewModel.openForecastPicker() }) {
                                                Icon(
                                                    Icons.Outlined.Schedule,
                                                    contentDescription = stringResource(R.string.forecast_preview)
                                                )
                                            }
                                            IconButton(onClick = {
                                                viewModel.beginAddSpot()
                                                navController.navigate(Screen.AddSpot.route)
                                            }) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .background(MaterialTheme.colorScheme.onBackground, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add_spot), tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(22.dp))
                                                }
                                            }
                                        }
                                        if (isLive && currentSpot != null) {
                                            LiveRefreshAction(
                                                fetchedAt = (nearby as? NearbyStationsState.Data)?.fetchedAt,
                                                onRefresh = {
                                                    viewModel.loadNearbyStations(currentSpot, forceRefresh = true)
                                                }
                                            )
                                        }
                                        if (isStationDetail) {
                                            LiveRefreshAction(
                                                fetchedAt = (stationDetail as? StationDetailState.Data)?.fetchedAt,
                                                onRefresh = {
                                                    stationId?.let {
                                                        viewModel.loadStation(it, forceRefresh = true)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                            HorizontalDividerMMD(thickness = 1.dp)
                        }
                    },
                    bottomBar = {
                        val onTopLevel = currentDestination?.route in bottomNavItems.map { it.route }
                        if (onTopLevel) {
                            Column {
                                HorizontalDividerMMD()
                                Row(
                                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 8.dp),
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
                                            Icon(rememberVectorPainter(screen.icon), contentDescription = stringResource(screen.labelRes), modifier = Modifier.size(28.dp))
                                            TextMMD(stringResource(screen.labelRes), fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                            if (selected) {
                                                Box(modifier = Modifier.padding(top = 4.dp).fillMaxWidth(0.6f).height(3.dp).background(MaterialTheme.colorScheme.onBackground))
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
                                onAddSpot = {
                                    viewModel.beginAddSpot()
                                    navController.navigate(Screen.AddSpot.route)
                                },
                                onOpenSpot = { spot -> navController.navigate(Screen.Forecast.routeFor(spot.id)) },
                                onOpenInfo = { catalogId -> navController.navigate(Screen.SpotInfo.routeFor(catalogId)) }
                            )
                        }
                        composable(Screen.NearMe.route) {
                            NearMeScreen(
                                viewModel = viewModel,
                                onOpenInfo = { catalogId -> navController.navigate(Screen.SpotInfo.routeFor(catalogId)) }
                            )
                        }
                        composable(Screen.AddSpot.route) {
                            AddSpotScreen(
                                viewModel = viewModel,
                                onSaved = { navController.popBackStack() },
                                onOpenInfo = { catalogId -> navController.navigate(Screen.SpotInfo.routeFor(catalogId)) }
                            )
                        }
                        composable(Screen.Forecast.route) { entry ->
                            val spotId = entry.arguments?.getString("spotId") ?: return@composable
                            ForecastScreen(
                                viewModel = viewModel,
                                spotId = spotId,
                                onOpenLive = { id -> navController.navigate(Screen.LiveStations.routeFor(id)) },
                                onOpenHelp = { navController.navigate(Screen.ForecastHelp.route) }
                            )
                        }
                        composable(Screen.LiveStations.route) { entry ->
                            LiveStationsScreen(
                                viewModel = viewModel,
                                spotId = entry.arguments?.getString("spotId") ?: return@composable,
                                onOpenStation = { id -> navController.navigate(Screen.StationDetail.routeFor(id)) }
                            )
                        }
                        composable(Screen.StationDetail.route) { entry ->
                            StationDetailScreen(
                                viewModel = viewModel,
                                stationId = entry.arguments?.getString("stationId") ?: return@composable
                            )
                        }
                        composable(Screen.ForecastHelp.route) {
                            ForecastHelpScreen(viewModel = viewModel)
                        }
                        composable(Screen.Sources.route) { entry ->
                            SourcesScreen(
                                viewModel = viewModel,
                                spotId = entry.arguments?.getString("spotId") ?: return@composable,
                                onBack = { navController.navigateUp() }
                            )
                        }
                        composable(Screen.SpotInfo.route) { entry ->
                            SpotInfoScreen(
                                viewModel = viewModel,
                                catalogId = entry.arguments?.getString("catalogId") ?: return@composable
                            )
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
}

@Composable
private fun LiveRefreshAction(fetchedAt: Long?, onRefresh: () -> Unit) {
    val age = fetchedAt?.let { freshnessText(it) }
    val description = age?.let { "${stringResource(R.string.refresh)}, $it" }
        ?: stringResource(R.string.refresh)
    Row(
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = description
            }
            .clickable(onClick = onRefresh)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            Icons.Outlined.Refresh,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        if (age != null) {
            TextMMD(text = age, fontSize = 16.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun ForecastTopBar(
    title: String,
    spot: Spot?,
    canNavigateBack: Boolean,
    onBack: () -> Unit,
    onOpenSources: () -> Unit,
    onOpenInfo: (() -> Unit)?,
    viewModel: KompaktWindViewModel
) {
    val forecast by viewModel.forecast.collectAsState()
    val fetchedAt = (forecast as? ForecastUiState.Data)?.fetchedAt
    val ageLabel = fetchedAt?.let { freshnessText(it) }
    val sourceLabel = spot
        ?.let { DataSource.effectiveFor(it.providerId, it.lat, it.lon).shortLabel }
        ?: stringResource(R.string.source)
    val refreshDescription = if (ageLabel != null) {
        "${stringResource(R.string.refresh)}, $ageLabel"
    } else {
        stringResource(R.string.refresh)
    }
    val sourceDescription = "${stringResource(R.string.choose_source)}, $sourceLabel"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .statusBarsPadding()
            .padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (canNavigateBack) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        SpotTitle(
            text = title,
            modifier = Modifier.weight(1f).padding(end = 4.dp)
        )
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onOpenInfo != null) {
                IconButton(onClick = onOpenInfo) {
                    Icon(
                        Icons.Outlined.Lightbulb,
                        contentDescription = stringResource(R.string.spot_info),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        contentDescription = refreshDescription
                    }
                    .clickable(enabled = spot != null) {
                        spot?.let { viewModel.loadForecast(it, forceRefresh = true) }
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                if (ageLabel != null) {
                    TextMMD(
                        text = ageLabel,
                        fontSize = 16.sp,
                        lineHeight = 16.sp
                    )
                }
            }
            Row(
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        contentDescription = sourceDescription
                    }
                    .clickable { onOpenSources() }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Outlined.Layers,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                TextMMD(
                    text = sourceLabel,
                    fontSize = 16.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun SpotTitle(text: String, modifier: Modifier = Modifier) {
    var width by remember { mutableIntStateOf(0) }
    val measurer = rememberTextMeasurer()
    val baseStyle = MaterialTheme.typography.titleMedium.copy(
        lineBreak = LineBreak.Heading,
        hyphens = Hyphens.None,
        fontWeight = FontWeight.Bold
    )
    val fontSize = remember(text, width, baseStyle) {
        if (width <= 0) {
            20.sp
        } else {
            var size = 20f
            while (size > 16f) {
                val layout = measurer.measure(
                    text = AnnotatedString(text),
                    style = baseStyle.copy(fontSize = size.sp),
                    constraints = Constraints(maxWidth = width),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                val lastLine = layout.lineCount - 1
                val lastLength = if (lastLine > 0) {
                    layout.getLineEnd(lastLine) - layout.getLineStart(lastLine)
                } else {
                    text.length
                }
                if (!layout.hasVisualOverflow && (lastLine == 0 || lastLength >= 3)) break
                size -= 1f
            }
            size.sp
        }
    }
    Box(modifier = modifier.fillMaxWidth().onSizeChanged { width = it.width }) {
        TextMMD(
            text = text,
            fontSize = fontSize,
            lineHeight = fontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            style = baseStyle,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun titleFor(route: String?, spotId: String?, spots: List<com.kompaktwind.data.Spot>): String {
    val spot = spotId?.let { id -> spots.firstOrNull { it.id == id } }
    val spotName = spot?.let {
        if (it.name.length > 18 && !it.shortName.isNullOrBlank()) it.shortName else it.name
    }
    return when {
        route == Screen.Spots.route -> stringResource(R.string.nav_spots)
        route == Screen.NearMe.route -> stringResource(R.string.nav_near_me)
        route == Screen.AddSpot.route -> stringResource(R.string.screen_add_spot)
        route == Screen.Forecast.route -> spotName ?: stringResource(R.string.screen_forecast)
        route == Screen.ForecastHelp.route -> stringResource(R.string.help_title)
        route == Screen.Sources.route -> spotName?.let {
            stringResource(R.string.screen_forecast_source_with_name, it)
        } ?: stringResource(R.string.screen_forecast_source)
        route == Screen.LiveStations.route -> stringResource(R.string.screen_live_stations)
        route == Screen.StationDetail.route -> stringResource(R.string.screen_station_detail)
        route == Screen.SpotInfo.route -> stringResource(R.string.screen_spot_info)
        route == Screen.Settings.route -> stringResource(R.string.nav_settings)
        else -> stringResource(R.string.app_name)
    }
}

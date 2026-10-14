package com.sergo379.gamedex.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sergo379.gamedex.BuildConfig
import com.sergo379.gamedex.AppGraph
import com.sergo379.gamedex.ui.screens.DetailScreen
import com.sergo379.gamedex.ui.screens.AnalyticsHub
import com.sergo379.gamedex.ui.screens.WeeklyAnalyticsScreen
import com.sergo379.gamedex.ui.screens.MonthlyAnalyticsScreen
import com.sergo379.gamedex.ui.screens.HomeScreen
import com.sergo379.gamedex.ui.screens.LibraryScreen
import com.sergo379.gamedex.ui.screens.SearchScreen
import com.sergo379.gamedex.ui.screens.SettingsScreen

private data class Destination(val route: String, val label: String, val icon: ImageVector)
private val destinations = listOf(
    Destination("home", "Главная", Icons.Rounded.Home),
    Destination("search", "Поиск", Icons.Rounded.Search),
    Destination("analytics", "Аналитика", Icons.Rounded.Analytics),
    Destination("library", "Библиотека", Icons.Rounded.VideoLibrary),
)

@Composable
fun GameDexApp(graph: AppGraph) {
    val navController = rememberNavController()
    val repository = graph.games
    val factory = remember(repository) { CatalogViewModelFactory(repository, graph.library, graph.settings) }
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(graph.settings, graph.api, BuildConfig.RAWG_API_KEY))
    val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()
    if (!settingsState.valid) {
        SettingsScreen(settingsViewModel, firstRun = true, onContinue = {})
        return
    }
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBottomBar = destinations.any { it.route == route }
    Scaffold(
        bottomBar = {
            if (showBottomBar) NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = route?.startsWith(destination.route) == true,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                HomeScreen(viewModel(factory = factory), { navController.navigate("detail/$it") }, { navController.navigate("search") }, { navController.navigate("settings") })
            }
            composable("search") {
                SearchScreen(viewModel(factory = factory), { navController.navigate("detail/$it") })
            }
            composable("analytics") { AnalyticsHub({ navController.navigate("analytics/weekly") }, { navController.navigate("analytics/monthly") }) }
            composable("analytics/weekly") {
                WeeklyAnalyticsScreen(viewModel(factory = AnalyticsViewModelFactory(7, graph.analytics)), navController::popBackStack)
            }
            composable("analytics/monthly") {
                MonthlyAnalyticsScreen(viewModel(factory = AnalyticsViewModelFactory(30, graph.analytics)), navController::popBackStack)
            }
            composable("library") { LibraryScreen(viewModel(factory = factory), { navController.navigate("detail/$it") }) }
            composable("settings") { SettingsScreen(settingsViewModel, firstRun = false, onContinue = navController::popBackStack) }
            composable("detail/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { backStack ->
                val id = requireNotNull(backStack.arguments).getInt("id")
                DetailScreen(
                    viewModel(key = "detail-$id", factory = CatalogViewModelFactory(repository, graph.library, graph.settings, id)),
                    navController::popBackStack,
                    onGameClick = { navController.navigate("detail/$it") },
                )
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, fontSize = 32.sp)
        Text(subtitle, modifier = Modifier.padding(top = 8.dp))
    }
}

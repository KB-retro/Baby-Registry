package com.kbretro.babyregistry.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.kbretro.babyregistry.data.repository.TrackerRepository
import com.kbretro.babyregistry.ui.history.HistoryScreen
import com.kbretro.babyregistry.ui.home.HomeScreen
import com.kbretro.babyregistry.ui.onboarding.OnboardingScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class RootState { LOADING, NEEDS_ONBOARDING, READY }

@HiltViewModel
class RootViewModel @Inject constructor(repo: TrackerRepository) : ViewModel() {
    val state: StateFlow<RootState> = repo.currentBaby
        .map { if (it == null) RootState.NEEDS_ONBOARDING else RootState.READY }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RootState.LOADING)
}

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Inicio", Icons.Rounded.Home),
    HISTORY("history", "Historial", Icons.Rounded.History),
}

@Composable
fun AppRoot(viewModel: RootViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (state) {
        RootState.LOADING -> Box(Modifier.fillMaxSize())
        RootState.NEEDS_ONBOARDING -> OnboardingScreen()
        RootState.READY -> MainScaffold()
    }
}

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = { navController.navigateToTab(tab.route) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.HOME.route,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable(Tab.HOME.route) {
                HomeScreen(onSeeAll = { navController.navigateToTab(Tab.HISTORY.route) })
            }
            composable(Tab.HISTORY.route) { HistoryScreen() }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

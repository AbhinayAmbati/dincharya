package com.dincharya.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dincharya.app.app.Graph
import com.dincharya.app.ui.screens.addtask.AddTaskScreen
import com.dincharya.app.ui.screens.focus.FocusScreen
import com.dincharya.app.ui.screens.insights.InsightsScreen
import com.dincharya.app.ui.screens.onboarding.OnboardingScreen
import com.dincharya.app.ui.screens.review.ReviewScreen
import com.dincharya.app.ui.screens.settings.SettingsScreen
import com.dincharya.app.ui.screens.today.TodayScreen
import com.dincharya.app.ui.theme.DincharyaTheme

/**
 * Root composable: theme + scaffold + navigation graph.
 *
 * Bottom bar and FAB are hidden on the "immersive" screens (onboarding, add
 * task, focus) so they never compete for the user's attention.
 */
@Composable
fun DincharyaApp(themeMode: String) {
    DincharyaTheme(themeMode = themeMode) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        // Screens that get the bottom navigation chrome.
        val chromeRoutes = setOf(
            Screen.Today.route,
            Screen.Insights.route,
            Screen.Review.route,
            Screen.Settings.route,
        )
        val showChrome = currentRoute in chromeRoutes

        Scaffold(
            bottomBar = {
                if (showChrome) DincharyaBottomBar(navController, currentRoute)
            },
            floatingActionButton = {
                // The add button only lives on Today — the one screen where
                // "capture a task" is the natural next action.
                if (currentRoute == Screen.Today.route) {
                    FloatingActionButton(onClick = { navController.navigate(Screen.AddTask.route) }) {
                        Text(text = "+", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            },
        ) { innerPadding ->
            DincharyaNavHost(
                navController = navController,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

/**
 * Editorial text-only bottom bar — no icons, serif labels. Selected item is
 * bold with a rule under it; unselected items are quiet grey.
 */
@Composable
private fun DincharyaBottomBar(navController: NavHostController, currentRoute: String?) {
    Surface(tonalElevation = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            bottomItems.forEach { (route, label) ->
                val selected = route == currentRoute
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            // Pop to the tab instead of stacking copies of it.
                            navController.navigate(route) {
                                popUpTo(Screen.Today.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = label,
                        style = if (selected) MaterialTheme.typography.titleMedium
                        else MaterialTheme.typography.bodyMedium,
                        color = if (selected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // Thin selected indicator — the monochrome "underline".
                    Surface(
                        color = if (selected) MaterialTheme.colorScheme.onSurface
                        else Color.Transparent,
                        modifier = Modifier.padding(top = 4.dp),
                    ) { Spacer(Modifier.size(16.dp, 2.dp)) }
                }
            }
        }
    }
}

/** Tabs in display order: (route, label). */
private val bottomItems = listOf(
    Screen.Today.route to "Today",
    Screen.Insights.route to "Insights",
    Screen.Review.route to "Review",
    Screen.Settings.route to "Settings",
)

/**
 * The navigation graph. Start destination depends on whether onboarding
 * has been completed (a one-time flag in SettingsStore).
 */
@Composable
fun DincharyaNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    val startDestination =
        if (Graph.settings.onboardingDone) Screen.Today.route else Screen.Onboarding.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Screen.Onboarding.route) { OnboardingScreen(navController) }
        composable(Screen.Today.route) { TodayScreen(navController) }
        composable(Screen.AddTask.route) { AddTaskScreen(navController) }
        composable(Screen.Insights.route) { InsightsScreen(navController) }
        composable(Screen.Focus.route) { FocusScreen(navController) }
        composable(Screen.Review.route) { ReviewScreen(navController) }
        composable(Screen.Settings.route) { SettingsScreen(navController) }
    }
}

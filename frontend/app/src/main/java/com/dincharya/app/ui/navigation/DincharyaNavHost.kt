package com.dincharya.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dincharya.app.R
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
 * Editorial bottom bar: text-only tabs, no icons.
 *
 * The selected tab sits in a soft pill (the quiet highlight of an active row
 * in a well-set document); everything else stays muted grey. A single
 * hairline rule separates the bar from the content above. Weight and
 * background — never colour alone — carry the selected state.
 */
@Composable
private fun DincharyaBottomBar(navController: NavHostController, currentRoute: String?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Hairline rule on top — the only line the bar needs.
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bottomItems.forEach { (route, labelRes) ->
                val selected = route == currentRoute
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.surfaceVariant
                            else Color.Transparent
                        )
                        .clickable {
                            // Pop to the tab instead of stacking copies of it.
                            navController.navigate(route) {
                                popUpTo(Screen.Today.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(labelRes),
                        style = if (selected) MaterialTheme.typography.titleMedium
                        else MaterialTheme.typography.bodyMedium,
                        color = if (selected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Tabs in display order: (route, label resource). */
private val bottomItems = listOf(
    Screen.Today.route to R.string.tab_today,
    Screen.Insights.route to R.string.tab_insights,
    Screen.Review.route to R.string.tab_review,
    Screen.Settings.route to R.string.tab_settings,
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

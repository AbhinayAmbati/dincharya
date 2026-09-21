package com.dincharya.app.ui.navigation

/**
 * Type-safe route definitions for the app's navigation graph.
 * A sealed class (not bare strings) so a typo fails at compile time.
 */
sealed class Screen(val route: String) {

    /** Intro carousel + chronotype quiz. */
    object Onboarding : Screen("onboarding")

    /** The day at a glance — momentum, sections, suggestions. */
    object Today : Screen("today")

    /** Create a new task. */
    object AddTask : Screen("add")

    /**
     * Edit an existing task (pending or completed). The Add Task screen is
     * reused with the form pre-filled; saving updates instead of inserting.
     */
    object EditTask : Screen("edit/{taskId}")

    /** Behavioural patterns — the honest mirror. */
    object Insights : Screen("insights")

    /** Deep-work timer for a single task. */
    object Focus : Screen("focus")

    /** 30-second evening review. */
    object Review : Screen("review")

    /** Preferences. */
    object Settings : Screen("settings")

    /** Privacy policy (opened from Settings). */
    object Privacy : Screen("privacy")

    /** Terms of use (opened from Settings). */
    object Terms : Screen("terms")

    /** Update checker — current vs. latest GitHub release (opened from Settings). */
    object Updates : Screen("updates")
}

/** Route for editing task [id] — see [Screen.EditTask]. */
fun editRoute(id: Long): String = "edit/$id"

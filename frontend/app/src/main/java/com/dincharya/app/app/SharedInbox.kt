package com.dincharya.app.app

/**
 * One-shot mailbox for content that arrives from OUTSIDE the app:
 *  - text shared from another app ("Share → Dincharya"),
 *  - the Add-task app shortcut.
 *
 * The inbox is consumed exactly once, by the first Add Task screen that
 * reads it — after that it is empty again. This deliberately avoids giving
 * the navigation layer any knowledge of intents.
 */
object SharedInbox {

    @Volatile
    var pendingTaskTitle: String? = null
        private set

    /** True when the launcher shortcut asked for the Add Task screen. */
    @Volatile
    var openAddTask: Boolean = false
        private set

    /** Store a shared title (drops blank shares). */
    fun offerTaskTitle(title: String?) {
        pendingTaskTitle = title?.trim()?.takeIf { it.isNotBlank() }
    }

    /** Consume and clear the pending title, if any. */
    fun consumePendingTitle(): String? {
        val value = pendingTaskTitle
        pendingTaskTitle = null
        return value
    }

    /** Record and clear the shortcut request in one go. */
    fun consumeOpenAddTask(): Boolean {
        val value = openAddTask
        openAddTask = false
        return value
    }

    /** Set by the launcher shortcut. */
    fun requestAddTask() {
        openAddTask = true
    }
}

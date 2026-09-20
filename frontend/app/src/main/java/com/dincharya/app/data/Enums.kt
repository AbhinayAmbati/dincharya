package com.dincharya.app.data

/**
 * Category of a task. Used by the learning layer to build per-category
 * behaviour profiles (e.g. "the user completes chores after lunch").
 *
 * Stored in the DB as its [name] so renaming a label later cannot corrupt
 * saved data.
 */
enum class TaskCategory(val label: String) {
    WORK("Work"),
    HEALTH("Health"),
    CHORES("Chores"),
    LEARNING("Learning"),
    PERSONAL("Personal");
}

/**
 * User-set priority (1 = lowest). Distinct from the *revealed* priority the
 * learning layer computes from behaviour — this is only what the user claims.
 */
enum class TaskPriority(val weight: Int, val label: String) {
    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3, "High");
}

/**
 * What actually happened to a task at a given moment. These labels are the
 * training data of the app: the adaptation engine learns to predict them.
 */
enum class EventOutcome {
    /** The task was created. */
    CREATED,

    /** The task was finished. */
    COMPLETED,

    /** The reminder was snoozed — pushed to a later time. */
    SNOOZED,

    /** The user explicitly dismissed/ignored the reminder. */
    IGNORED,

    /** The user moved the task to a different time. */
    RESCHEDULED,
}

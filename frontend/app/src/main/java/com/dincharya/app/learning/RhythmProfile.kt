package com.dincharya.app.learning

import com.dincharya.app.data.EventOutcome
import com.dincharya.app.data.TaskEntity
import com.dincharya.app.data.TaskEventEntity
import java.util.Calendar

/**
 * The personalised "reading" of the user, derived purely from their own
 * event log. Everything here exists to make the app speak about THIS user
 * ("you finish Learning at 9, Chores at 20") instead of generic advice.
 *
 * Pure Kotlin, no Android dependencies — unit-testable like the engine.
 */
object RhythmProfile {

    /** Completions needed before the profile dares to say anything personal. */
    const val MIN_PROFILE_SAMPLES = 15

    /** Days a habit is scored over. */
    private const val HABIT_WINDOW_DAYS = 30

    // ------------------------------------------------------------------
    // Per-category best hours
    // ------------------------------------------------------------------

    /**
     * For each category with enough history: the hour with the best
     * completion rate (at least [MIN_HOUR_SAMPLES] outcomes), ties broken
     * by volume, then earliest hour. A user can be a 9 AM learner and an
     * 8 PM chores person at the same time — the global "best hour" hides
     * that; this map shows it.
     */
    fun perCategoryBestHour(events: List<TaskEventEntity>): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        val categories = events.map { it.category }.distinct()
        for (category in categories) {
            val hour = bestHour(events.filter { it.category == category })
            if (hour != null) result[category] = hour
        }
        return result
    }

    /** Best hour in [events] by completion rate (min [MIN_HOUR_SAMPLES] samples). */
    fun bestHour(events: List<TaskEventEntity>): Int? {
        val completed = mutableMapOf<Int, Int>()
        val total = mutableMapOf<Int, Int>()
        for (event in events) {
            when (event.outcome) {
                EventOutcome.COMPLETED.name -> completed.merge(event.hourOfDay, 1, Int::plus)
                EventOutcome.SNOOZED.name, EventOutcome.IGNORED.name -> Unit
                else -> continue
            }
            total.merge(event.hourOfDay, 1, Int::plus)
        }
        return total.entries
            .filter { it.value >= MIN_HOUR_SAMPLES }
            .map { (hour, n) -> Triple(hour, (completed[hour] ?: 0).toFloat() / n, n) }
            .maxWithOrNull(
                compareBy<Triple<Int, Float, Int>> { it.second }
                    .thenBy { it.third }
                    .thenBy { -it.first }
            )
            ?.first
    }

    const val MIN_HOUR_SAMPLES = 3

    // ------------------------------------------------------------------
    // The self-image check
    // ------------------------------------------------------------------

    /**
     * The onboarding quiz asked the user what kind of day they have; the
     * event log knows what kind of day they ACTUALLY have. When the two
     * disagree loudly, say so — gently. Returns null when the history is
     * too thin to contradict anyone.
     */
    fun selfImage(chronotype: String, events: List<TaskEventEntity>): String? {
        val completions = events.filter { it.outcome == EventOutcome.COMPLETED.name }
        if (completions.size < MIN_PROFILE_SAMPLES) return null

        val morning = completions.count { it.hourOfDay in 5..11 }
        val evening = completions.count { it.hourOfDay in 17..23 }
        val share = { n: Int -> n.toFloat() / completions.size }

        return when {
            chronotype == "early" && share(evening) >= 0.55f ->
                "You called yourself a morning person, but " +
                    "${(share(evening) * 100).toInt()}% of your completions happen after 5 PM."
            chronotype == "late" && share(morning) >= 0.55f ->
                "You said you're a late riser, but you actually finish " +
                    "${(share(morning) * 100).toInt()}% of your tasks before noon."
            chronotype == "neutral" && share(morning) >= 0.7f ->
                "You picked 'somewhere in between'. Your record says mornings, clearly."
            chronotype == "neutral" && share(evening) >= 0.7f ->
                "You picked 'somewhere in between'. Your record says evenings, clearly."
            else -> null
        }
    }

    // ------------------------------------------------------------------
    // Weekly narrative
    // ------------------------------------------------------------------

    /**
     * 2–3 plain sentences that read like a person skimmed the week:
     * strongest day, golden window, and how this week compares to the last.
     */
    fun weeklyNarrative(events: List<TaskEventEntity>, thisWeek: Int): List<String> {
        val lines = mutableListOf<String>()
        val completions = events.filter { it.outcome == EventOutcome.COMPLETED.name }
        if (completions.size < MIN_PROFILE_SAMPLES) return lines

        // Strongest day of week.
        val dayNames = mapOf(
            Calendar.MONDAY to "Mondays", Calendar.TUESDAY to "Tuesdays",
            Calendar.WEDNESDAY to "Wednesdays", Calendar.THURSDAY to "Thursdays",
            Calendar.FRIDAY to "Fridays", Calendar.SATURDAY to "Saturdays",
            Calendar.SUNDAY to "Sundays",
        )
        val byDay = completions.groupBy {
            Calendar.getInstance().apply { timeInMillis = it.occurredAt }
                .get(Calendar.DAY_OF_WEEK)
        }
        if (byDay.size >= 3) {
            val (day, list) = byDay.maxByOrNull { it.value.size }!!.let { it.key to it.value }
            val pct = (list.size.toFloat() / completions.size * 100).toInt()
            lines.add("${dayNames[day]} are your strongest day: $pct% of everything you finish happens there.")
        }

        // Golden window: the two-hour span with the most completions.
        val byHour = completions.groupBy { it.hourOfDay / 2 } // 2-hour buckets
        val topBucket = byHour.maxByOrNull { it.value.size }
        if (topBucket != null && topBucket.value.size >= 3) {
            val from = topBucket.key * 2
            lines.add("Your golden window is around %02d:00 to %02d:00. That's when things get done.".format(from, from + 2))
        }

        // Momentum: this week vs the previous one.
        val now = System.currentTimeMillis()
        val week = 7L * 24 * 60 * 60 * 1000
        val lastWeek = completions.count { it.occurredAt in (now - 2 * week) until (now - week) }
        lines.add(
            when {
                thisWeek > lastWeek && lastWeek > 0 -> "You're up on last week ($thisWeek vs $lastWeek completions). Whatever changed, keep it."
                thisWeek < lastWeek && lastWeek > 0 -> "Slower than last week ($thisWeek vs $lastWeek). No verdicts, just an honest mirror."
                else -> "$thisWeek completed this week."
            }
        )
        return lines
    }

    // ------------------------------------------------------------------
    // Habit strength
    // ------------------------------------------------------------------

    /** One recurring habit and how automatic it has become (0..1). */
    data class HabitStat(
        val title: String,
        val repeatRule: String,
        val completions: Int,
        /** Rough expected completions in the window, from the repeat rule. */
        val expected: Int,
    ) {
        val strength: Float get() = if (expected == 0) 0f else (completions.toFloat() / expected).coerceIn(0f, 1f)
    }

    /**
     * Strength of every active recurring habit, scored over the last
     * [HABIT_WINDOW_DAYS] days. A habit is a chain of task rows sharing a
     * title, so completions are counted as finished rows with that title
     * (completedAt inside the window) while the rule comes from the pending
     * row. A daily habit done 17 of ~22 realistic days scores 0.77.
     */
    fun habitStrength(allTasks: List<TaskEntity>): List<HabitStat> {
        val windowStart = System.currentTimeMillis() - HABIT_WINDOW_DAYS.toLong() * 24 * 60 * 60 * 1000

        // Active habits: pending recurring rows, deduplicated by title.
        val active = allTasks
            .filter { !it.isCompleted && it.repeatRule != "NONE" }
            .distinctBy { it.title }
        if (active.isEmpty()) return emptyList()

        val doneByTitle = allTasks
            .filter { it.isCompleted && (it.completedAt ?: 0L) >= windowStart }
            .groupingBy { it.title }
            .eachCount()

        return active.map { task ->
            val expected = when (task.repeatRule) {
                "DAILY", "WEEKDAYS" -> HABIT_WINDOW_DAYS - 8 // ~22 realistic days
                "WEEKLY" -> 4
                else -> 0
            }
            HabitStat(
                title = task.title,
                repeatRule = task.repeatRule,
                completions = doneByTitle[task.title] ?: 0,
                expected = expected,
            )
        }.sortedByDescending { it.strength }
    }

    // ------------------------------------------------------------------
    // Estimate calibration
    // ------------------------------------------------------------------

    /**
     * Per category: the median ratio of actual focused minutes to the
     * estimated minutes (FOCUSED events carry "est/act" in their detail).
     * A Learning ratio of 2.3 means "Learning takes you 2.3× longer than
     * you think" — one of the most personal numbers an app can show.
     */
    fun estimateCalibration(events: List<TaskEventEntity>): Map<String, Float> {
        val ratios = mutableMapOf<String, MutableList<Float>>()
        for (event in events) {
            if (event.outcome != EventOutcome.FOCUSED.name) continue
            val parts = event.detail?.split("/") ?: continue
            if (parts.size != 2) continue
            val est = parts[0].toFloatOrNull() ?: continue
            val act = parts[1].toFloatOrNull() ?: continue
            if (est <= 0f || act < 1f) continue // ignore trivial sessions
            ratios.getOrPut(event.category) { mutableListOf() }.add(act / est)
        }
        return ratios.mapValues { (_, list) ->
            list.sorted()[list.size / 2] // median
        }.filterValues { it > 0.3f && it < 5f } // ignore absurd samples
    }
}

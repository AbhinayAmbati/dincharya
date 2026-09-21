package com.dincharya.app.learning

import com.dincharya.app.data.EventOutcome
import com.dincharya.app.data.TaskEventEntity
import java.util.Calendar

/**
 * Learning engine v2: a logistic regression trained ON-DEVICE, in pure Kotlin.
 *
 * No framework, no network, no model file — the user's own task_events rows
 * ARE the training set, and training is a few hundred passes of gradient
 * descent over a few hundred rows, which a phone finishes in milliseconds.
 *
 * The model answers one question: "given a slot (hour, day, category), how
 * likely is this user to actually finish a task there?" It powers the
 * rhythm profile, the suggested slots for anytime tasks and the order of
 * the morning brief. Because it is a linear model, its weights can be read
 * back in plain language — every suggestion keeps its human reason.
 *
 * Features (one-hot encoded, plus a bias term):
 *   - hour of day the task was scheduled for (24)
 *   - day of week (7)
 *   - task category (5, by name index in [com.dincharya.app.data.TaskCategory])
 * Labels: COMPLETED -> 1, SNOOZED / IGNORED -> 0. CREATED and RESCHEDULED
 * events carry no outcome signal and are skipped.
 */
class CompletionPredictor private constructor(
    private val weights: FloatArray,
    val trainedSamples: Int,
) {

    /** True when enough history exists to trust a prediction at all. */
    val isTrained: Boolean get() = trainedSamples >= MIN_SAMPLES

    /**
     * Probability (0..1) that a task scheduled at [hour] on [dayOfWeek]
     * in [category] gets completed. Day and category nudge the number;
     * the hour does most of the talking.
     */
    fun predict(hour: Int, dayOfWeek: Int, category: String): Float {
        val x = encode(hour, dayOfWeek, category)
        var z = 0f
        for (i in weights.indices) z += weights[i] * x[i]
        return sigmoid(z)
    }

    /**
     * The hour (0..23) with the highest predicted completion for [category]
     * on [dayOfWeek] — the "golden hour". Ties break earliest, so an
     * untrained model (all-equal probabilities) returns a sensible morning
     * hour rather than 23:00.
     */
    fun bestHourFor(category: String, dayOfWeek: Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)): Int {
        var bestHour = 9
        var bestProb = -1f
        for (hour in 0..23) {
            val p = predict(hour, dayOfWeek, category)
            if (p > bestProb) {
                bestProb = p
                bestHour = hour
            }
        }
        return bestHour
    }

    private fun encode(hour: Int, dayOfWeek: Int, category: String): FloatArray {
        val x = FloatArray(FEATURE_COUNT)
        x[0] = 1f // bias
        x[1 + hour.coerceIn(0, 23)] = 1f
        x[1 + 24 + dayOfWeek.coerceIn(0, 6)] = 1f
        x[1 + 24 + 7 + categoryIndex(category)] = 1f
        return x
    }

    private fun sigmoid(z: Float): Float = 1f / (1f + Math.exp(-z.toDouble()).toFloat())

    companion object {
        /** 1 bias + 24 hours + 7 days + 5 categories. */
        const val FEATURE_COUNT = 1 + 24 + 7 + 5
        const val CATEGORY_COUNT = 5

        /** Samples below this: the model reports untrained and stays quiet. */
        const val MIN_SAMPLES = 20

        private val CATEGORIES = listOf("WORK", "HEALTH", "CHORES", "LEARNING", "PERSONAL")

        fun categoryIndex(name: String): Int = CATEGORIES.indexOf(name).coerceAtLeast(0)

        /**
         * Train on the raw event log. Full-batch gradient descent with a
         * whisper of L2 regularisation so a thin hour bucket cannot shout.
         */
        fun train(events: List<TaskEventEntity>, epochs: Int = 250, learningRate: Float = 0.2f): CompletionPredictor {
            val samples = ArrayList<Pair<FloatArray, Float>>(events.size)
            for (event in events) {
                val label = when (event.outcome) {
                    EventOutcome.COMPLETED.name -> 1f
                    EventOutcome.SNOOZED.name, EventOutcome.IGNORED.name -> 0f
                    else -> continue
                }
                val cal = Calendar.getInstance().apply { timeInMillis = event.scheduledAt }
                val x = FloatArray(FEATURE_COUNT)
                x[0] = 1f
                x[1 + event.hourOfDay.coerceIn(0, 23)] = 1f
                // Calendar.DAY_OF_WEEK is 1..7 (SUNDAY..SATURDAY) — shift to 0..6.
                x[1 + 24 + (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7] = 1f
                x[1 + 24 + 7 + categoryIndex(event.category)] = 1f
                samples.add(x to label)
            }

            val weights = FloatArray(FEATURE_COUNT)
            if (samples.isEmpty()) return CompletionPredictor(weights, 0)

            val l2 = 1e-3f
            repeat(epochs) {
                val grad = FloatArray(FEATURE_COUNT)
                for ((x, y) in samples) {
                    var z = 0f
                    for (i in weights.indices) z += weights[i] * x[i]
                    val error = sigmoid(z) - y
                    for (i in weights.indices) grad[i] += error * x[i]
                }
                val scale = learningRate / samples.size
                for (i in weights.indices) {
                    weights[i] -= scale * grad[i] + l2 * weights[i]
                }
            }
            return CompletionPredictor(weights, samples.size)
        }

        private fun sigmoid(z: Float): Float = 1f / (1f + Math.exp(-z.toDouble()).toFloat())
    }
}

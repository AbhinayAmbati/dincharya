package com.dincharya.app.ui.screens.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dincharya.app.R
import com.dincharya.app.app.Graph
import com.dincharya.app.ui.navigation.Screen

/**
 * Onboarding: three intro pages, then a two-question chronotype quiz.
 *
 * The quiz result is stored as the timing prior ("early" / "neutral" /
 * "late") that the learning layer will refine with real behaviour. Short on
 * purpose — every extra question here loses users before they see the app.
 */
@Composable
fun OnboardingScreen(navController: NavController) {

    // 0..2 = intro pages, 3 = quiz page.
    var page by remember { mutableIntStateOf(0) }

    // Quiz answers: -1 = unanswered, 0/1/2 = option index.
    var answer1 by remember { mutableIntStateOf(-1) }
    var answer2 by remember { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        when (page) {
            0 -> IntroPage(
                title = stringResource(R.string.onboarding_p1_title),
                body = stringResource(R.string.onboarding_p1_body),
            )
            1 -> IntroPage(
                title = stringResource(R.string.onboarding_p2_title),
                body = stringResource(R.string.onboarding_p2_body),
            )
            2 -> IntroPage(
                title = stringResource(R.string.onboarding_p3_title),
                body = stringResource(R.string.onboarding_p3_body),
            )
            else -> {
                // ---- Chronotype quiz ----
                Text(stringResource(R.string.onboarding_quiz_title),
                    style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.onboarding_quiz_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
                QuizQuestion(
                    question = stringResource(R.string.onboarding_q1),
                    options = listOf(
                        stringResource(R.string.onboarding_q1_a),
                        stringResource(R.string.onboarding_q1_b),
                        stringResource(R.string.onboarding_q1_c),
                    ),
                    selected = answer1,
                ) { answer1 = it }
                Spacer(Modifier.height(24.dp))
                QuizQuestion(
                    question = stringResource(R.string.onboarding_q2),
                    options = listOf(
                        stringResource(R.string.onboarding_q2_a),
                        stringResource(R.string.onboarding_q2_b),
                        stringResource(R.string.onboarding_q2_c),
                    ),
                    selected = answer2,
                ) { answer2 = it }
            }
        }

        Spacer(Modifier.height(32.dp))

        // ---- Navigation buttons ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (page > 0) {
                TextButton(onClick = { page -= 1 }) {
                    Text(stringResource(R.string.onboarding_back))
                }
            } else {
                Spacer(Modifier.height(1.dp))
            }

            if (page < 3) {
                Button(onClick = { page += 1 }) {
                    Text(stringResource(R.string.onboarding_next))
                }
                TextButton(onClick = { finishOnboarding(answer1, answer2, navController) }) {
                    Text(stringResource(R.string.onboarding_skip))
                }
            } else {
                Button(
                    onClick = { finishOnboarding(answer1, answer2, navController) },
                    enabled = answer1 >= 0 && answer2 >= 0,
                ) {
                    Text(stringResource(R.string.onboarding_start))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Persist the chronotype prior, mark onboarding done and enter the app.
 *
 * Both answers are skipped entirely (both -1) when the user presses Skip —
 * in that case we keep the "neutral" default.
 */
private fun finishOnboarding(
    answer1: Int,
    answer2: Int,
    navController: NavController,
) {
    // Scoring: option 0 leans early, option 2 leans late, 1 is neutral.
    // A skipped quiz (either answer -1) keeps the "neutral" default.
    Graph.settings.chronotype = when {
        answer1 < 0 || answer2 < 0 -> "neutral"
        answer1 + answer2 <= 1 -> "early"   // two early-ish answers
        answer1 + answer2 >= 3 -> "late"    // two late-ish answers
        else -> "neutral"
    }
    Graph.settings.onboardingDone = true

    navController.navigate(Screen.Today.route) {
        popUpTo(Screen.Onboarding.route) { inclusive = true }
    }
}

/** One of the three intro slides: big title, calm paragraph. */
@Composable
private fun IntroPage(title: String, body: String) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * A quiz question: label + radio options. 48dp+ tap rows, no time pressure.
 */
@Composable
private fun QuizQuestion(
    question: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Column {
        Text(question, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        options.forEachIndexed { index, option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(index) },
            ) {
                RadioButton(selected = selected == index, onClick = { onSelect(index) })
                Text(option, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

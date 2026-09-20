package com.dincharya.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dincharya.app.R
import com.dincharya.app.app.Graph
import com.dincharya.app.ui.navigation.Screen

/**
 * Onboarding: three intro pages, then a two-question chronotype quiz.
 *
 * A fixed three-band structure so every page feels the same: a quiet top
 * row (brand + skip), the page content vertically centered (scrolls if it
 * ever outgrows the screen), and the controls pinned to the bottom. A row
 * of small dashes shows progress through the four pages. All text is
 * left-aligned.
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
            .padding(horizontal = 28.dp),
    ) {
        Spacer(Modifier.height(24.dp))

        // ---- Top band: quiet brand mark left, Skip right ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            if (page < 3) {
                TextButton(onClick = { finishOnboarding(answer1, answer2, navController) }) {
                    Text(stringResource(R.string.onboarding_skip))
                }
            }
        }

        // ---- Center band: progress + page content, centered, scrolls if tall ----
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 24.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(4) { index -> ProgressDash(active = index == page) }
                }

                Spacer(Modifier.height(28.dp))

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
                        Column {
                            Text(
                                stringResource(R.string.onboarding_quiz_title),
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.onboarding_quiz_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(28.dp))
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
                }
            }
        }

        // ---- Bottom band: Back (ghost, left) + Next/Start (primary, right) ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (page > 0) {
                TextButton(onClick = { page -= 1 }) {
                    Text(stringResource(R.string.onboarding_back))
                }
            }
            Spacer(Modifier.weight(1f))
            if (page < 3) {
                Button(
                    onClick = { page += 1 },
                    modifier = Modifier
                        .defaultMinSize(minWidth = 132.dp)
                        .height(48.dp),
                ) {
                    Text(stringResource(R.string.onboarding_next))
                }
            } else {
                Button(
                    onClick = { finishOnboarding(answer1, answer2, navController) },
                    enabled = answer1 >= 0 && answer2 >= 0,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 132.dp)
                        .height(48.dp),
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

/**
 * A thin progress dash: the filled one marks the current page. Length and
 * fill (never colour) carry the state, per the accessibility rules.
 */
@Composable
private fun ProgressDash(active: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 24.dp, height = 3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(
                if (active) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.outline
            ),
    )
}

/** One of the three intro slides: big title, calm muted paragraph. */
@Composable
private fun IntroPage(title: String, body: String) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A quiz question: label + full-width tappable option rows. The selected
 * row is highlighted with the same soft pill the bottom bar uses, so the
 * app's selection language stays consistent. 48dp+ tap targets.
 */
@Composable
private fun QuizQuestion(
    question: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Column {
        Text(question, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        options.forEachIndexed { index, option ->
            val isSelected = selected == index
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                        else Color.Transparent
                    )
                    .clickable { onSelect(index) }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { onSelect(index) },
                )
                Spacer(Modifier.size(8.dp))
                Text(option, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

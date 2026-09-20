package com.dincharya.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.dincharya.app.app.SettingsStore

/**
 * Monochrome Material 3 theme.
 *
 * Light: paper white background, ink-black primary.
 * Dark:  near-black background, white primary.
 *
 * Status colours map to the same ink/paper ramp on purpose — TaskRows and
 * charts communicate state through shape and weight (check glyph,
 * strikethrough, bar length), so colour carries no information that a
 * colour-blind user would miss.
 */
private val LightScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = Ink,
    onPrimaryContainer = Paper,
    secondary = SoftInk,
    onSecondary = Paper,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = OffPaper,
    onSurfaceVariant = Slate,
    outline = Rule,
    outlineVariant = Rule,
)

private val DarkScheme = darkColorScheme(
    primary = PaperInverse,
    onPrimary = DarkInk,
    primaryContainer = PaperInverse,
    onPrimaryContainer = DarkInk,
    secondary = DarkSlate,
    onSecondary = DarkInk,
    background = DarkInk,
    onBackground = PaperInverse,
    surface = DarkSurface,
    onSurface = PaperInverse,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkSlate,
    outline = DarkRule,
    outlineVariant = DarkRule,
)

/** Flat, slightly-rounded shapes — cards with 1px rules, not shadows. */
val DincharyaShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(10.dp),
)

/**
 * @param themeMode one of [SettingsStore.THEME_SYSTEM]/[THEME_LIGHT]/[THEME_DARK].
 */
@Composable
fun DincharyaTheme(
    themeMode: String = SettingsStore.THEME_SYSTEM,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        SettingsStore.THEME_DARK -> true
        SettingsStore.THEME_LIGHT -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (useDark) DarkScheme else LightScheme,
        typography = DincharyaTypography,
        shapes = DincharyaShapes,
        content = content,
    )
}

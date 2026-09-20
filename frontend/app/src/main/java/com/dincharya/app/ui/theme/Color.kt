package com.dincharya.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Dincharya palette: pure black & white.
 *
 * There is deliberately NO accent colour and NO semantic colour set —
 * status is communicated by icon shape, weight and strikethrough, never by
 * colour alone. This keeps the design accessible (colour-blind safe by
 * construction) and gives the app its editorial, ink-on-paper character.
 */

// --- Light theme ---
val Ink = Color(0xFF000000) // primary text, filled buttons
val SoftInk = Color(0xFF1A1A1A) // high-emphasis secondary surface
val Slate = Color(0xFF595959) // secondary text (AA on white)
val Faint = Color(0xFF8C8C8C) // tertiary text, disabled
val Rule = Color(0xFFE3E3E3) // hairline dividers, outlined fields
val Paper = Color(0xFFFFFFFF) // app background / cards
val OffPaper = Color(0xFFF7F7F5) // subtle section fills

// --- Dark theme (inverted same ramp) ---
val PaperInverse = Color(0xFFFFFFFF) // primary text in dark mode
val DarkInk = Color(0xFF0A0A0A) // app background
val DarkSurface = Color(0xFF161616) // cards
val DarkSlate = Color(0xFFA6A6A6) // secondary text
val DarkRule = Color(0xFF2B2B2B) // dividers

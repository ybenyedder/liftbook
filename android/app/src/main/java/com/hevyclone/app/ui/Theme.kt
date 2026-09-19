@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
package com.hevyclone.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import com.hevyclone.app.R

/** Inter variable font — SIL Open Font License (google/fonts). */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.inter_regular, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.inter_regular, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.inter_regular, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.inter_regular, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
)

/** Palette sampled from the user's own Hevy screenshots (dark, blue accent). */
object C {
    val Bg = Color(0xFF111113)
    val Card = Color(0xFF1C1C1E)
    val Card2 = Color(0xFF2A2A2D)
    val Line = Color(0xFF2C2C2F)
    val Line2 = Color(0xFF3A3A3E)
    val Text = Color(0xFFF4F8F8)
    val Mut = Color(0xFF8D9399)
    val Mut2 = Color(0xFF6B7076)
    val Accent = Color(0xFF028CFD)
    val AccPress = Color(0xFF0279DB)
    val AccText = Color(0xFFFFFFFF)
    val Red = Color(0xFFE5484D)
    val Gold = Color(0xFFF5C518)
}

private val DarkColors = darkColorScheme(
    primary = C.Accent,
    onPrimary = C.AccText,
    secondary = C.Card2,
    onSecondary = C.Text,
    background = C.Bg,
    onBackground = C.Text,
    surface = C.Card,
    onSurface = C.Text,
    surfaceVariant = C.Card2,
    onSurfaceVariant = C.Mut,
    outline = C.Line,
    outlineVariant = C.Line2,
    error = C.Red,
)

private val HevyTypography = Typography(
    displayLarge = defaultStyle(FontWeight.Bold),
    displayMedium = defaultStyle(FontWeight.Bold),
    displaySmall = defaultStyle(FontWeight.Bold),
    headlineLarge = defaultStyle(FontWeight.Bold),
    headlineMedium = defaultStyle(FontWeight.Bold),
    headlineSmall = defaultStyle(FontWeight.SemiBold),
    titleLarge = defaultStyle(FontWeight.SemiBold),
    titleMedium = defaultStyle(FontWeight.SemiBold),
    titleSmall = defaultStyle(FontWeight.Medium),
    bodyLarge = defaultStyle(FontWeight.Normal),
    bodyMedium = defaultStyle(FontWeight.Normal),
    bodySmall = defaultStyle(FontWeight.Normal),
    labelLarge = defaultStyle(FontWeight.Medium),
    labelMedium = defaultStyle(FontWeight.Medium),
    labelSmall = defaultStyle(FontWeight.Medium),
)

private fun defaultStyle(weight: FontWeight) = androidx.compose.ui.text.TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
)

@Composable
fun HevyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = HevyTypography,
        content = content,
    )
}

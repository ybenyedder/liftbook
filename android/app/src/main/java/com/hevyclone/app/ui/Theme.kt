package com.hevyclone.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.hevyclone.app.R

/** Poppins — SIL Open Font License (google/fonts). */
val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

/** Palette sampled from the official app screenshots. */
object C {
    // light
    val Bg = Color(0xFFFAFBFB)
    val Card = Color(0xFFFFFFFF)
    val Card2 = Color(0xFFF1F2F3)
    val Line = Color(0xFFECECED)
    val Line2 = Color(0xFFDDDDDF)
    val Text = Color(0xFF19191A)
    val Mut = Color(0xFF7F7F80)
    val Mut2 = Color(0xFFA0A7A6)
    val Accent = Color(0xFF20B49A)
    val AccPress = Color(0xFF1B9C86)
    val AccText = Color(0xFFFFFFFF)
    val Red = Color(0xFFE5484D)

    // dark (background from the dark promo material, same accent)
    val DBg = Color(0xFF191A1B)
    val DCard = Color(0xFF222324)
    val DCard2 = Color(0xFF2C2D2F)
    val DLine = Color(0xFF303134)
    val DLine2 = Color(0xFF3C3D40)
    val DText = Color(0xFFFAFAFA)
    val DMut = Color(0xFF909599)
    val DMut2 = Color(0xFF6B7074)
}

private val LightColors = lightColorScheme(
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

private val DarkColors = darkColorScheme(
    primary = C.Accent,
    onPrimary = C.AccText,
    secondary = C.DCard2,
    onSecondary = C.DText,
    background = C.DBg,
    onBackground = C.DText,
    surface = C.DCard,
    onSurface = C.DText,
    surfaceVariant = C.DCard2,
    onSurfaceVariant = C.DMut,
    outline = C.DLine,
    outlineVariant = C.DLine2,
    error = C.Red,
)

private fun hevyTypography() = Typography(
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
    fontFamily = Poppins,
    fontWeight = weight,
)

@Composable
fun HevyTheme(theme: String, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (theme == "dark") DarkColors else LightColors,
        typography = hevyTypography(),
        content = content,
    )
}

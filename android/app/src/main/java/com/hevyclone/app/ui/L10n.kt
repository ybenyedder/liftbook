package com.hevyclone.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hevyclone.app.R
import com.hevyclone.app.data.L10nData
import java.util.Locale

/** UI strings: EN default, FR/ES/DE provided; falls back to EN. */
object L10n {
    val lang: String get() = Locale.getDefault().language
    fun s(en: String, fr: String, es: String? = null, de: String? = null): String =
        when (lang) {
            "fr" -> fr
            "es" -> es ?: en
            "de" -> de ?: en
            else -> en
        }
}

fun exName(name: String): String = L10nData.name(name)
fun muscleName(m: String): String = L10nData.muscle(m)
fun equipName(e: String): String = L10nData.equip(e)

fun seriesLabel(n: Int, name: String): String = when (L10n.lang) {
    "fr" -> if (n > 1) "$n séries $name" else "$n série $name"
    "de" -> "$n Sätze $name"
    else -> "$n series $name"
}

fun seeMoreExercises(n: Int): String = when (L10n.lang) {
    "fr" -> if (n > 1) "Voir $n exercices en plus" else "Voir 1 exercice en plus"
    "es" -> "Ver $n ejercicios más"
    "de" -> "$n weitere Übungen anzeigen"
    else -> if (n > 1) "See $n more exercises" else "See 1 more exercise"
}

fun illRes(muscle: String): Int = when (muscle) {
    "Chest" -> R.drawable.ill_chest
    "Shoulders" -> R.drawable.ill_shoulders
    "Biceps" -> R.drawable.ill_biceps
    "Triceps" -> R.drawable.ill_triceps
    "Lats" -> R.drawable.ill_lats
    "Lower back" -> R.drawable.ill_lower_back
    "Traps" -> R.drawable.ill_traps
    "Quads" -> R.drawable.ill_quads
    "Hamstrings" -> R.drawable.ill_hamstrings
    "Glutes" -> R.drawable.ill_glutes
    "Abductors" -> R.drawable.ill_abductors
    "Adductors" -> R.drawable.ill_adductors
    "Calves" -> R.drawable.ill_calves
    "Abs" -> R.drawable.ill_abs
    "Forearms" -> R.drawable.ill_forearms
    else -> R.drawable.ill_chest
}

fun illAnimRes(muscle: String): Int = when (muscle) {
    "Chest" -> R.drawable.ill_chest_anim
    "Shoulders" -> R.drawable.ill_shoulders_anim
    "Biceps" -> R.drawable.ill_biceps_anim
    "Triceps" -> R.drawable.ill_triceps_anim
    "Lats" -> R.drawable.ill_lats_anim
    "Lower back" -> R.drawable.ill_lower_back_anim
    "Traps" -> R.drawable.ill_traps_anim
    "Quads" -> R.drawable.ill_quads_anim
    "Hamstrings" -> R.drawable.ill_hamstrings_anim
    "Glutes" -> R.drawable.ill_glutes_anim
    "Abductors" -> R.drawable.ill_abductors_anim
    "Adductors" -> R.drawable.ill_adductors_anim
    "Calves" -> R.drawable.ill_calves_anim
    "Abs" -> R.drawable.ill_abs_anim
    "Forearms" -> R.drawable.ill_forearms_anim
    else -> R.drawable.ill_chest_anim
}

@Composable
fun IllIcon(muscle: String, size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(illRes(muscle)),
        contentDescription = muscleName(muscle),
        modifier = modifier.size(size),
    )
}

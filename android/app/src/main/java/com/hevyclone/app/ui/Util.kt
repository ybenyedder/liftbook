package com.hevyclone.app.ui

import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Clickable with a spring press-scale + light haptic tick — the "animations when I
 * click on things" feel. Every interactive row/button/chip goes through here.
 */
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.955f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        )
}

/** Same press feel, but for already-clickable scopes that only want the scale + haptic. */
fun Modifier.pressScale(): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.955f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Light haptic tick on tap (checkboxes, toggles, set-complete). */
fun Modifier.hapticTap(onTap: () -> Unit): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onTap()
        },
    )
}

/**
 * Open the Basic-Fit app (where the gym-access QR lives). Falls back to the Play Store
 * when the app isn't installed. We cannot read another app's QR code — Android forbids
 * it and Basic-Fit has no public API — so the fastest path is a one-tap hop into their app.
 */
fun openBasicFit(ctx: android.content.Context) {
    val pkg = "com.basicfit.android"
    val launch = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
        addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        setPackage(pkg)
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val launched = runCatching {
        ctx.startActivity(launch)
        true
    }.getOrDefault(false)
    if (!launched) {
        // Not installed → open the Play Store listing.
        val store = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("market://details?id=$pkg"),
        ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
        runCatching { ctx.startActivity(store) }.onFailure {
            runCatching {
                ctx.startActivity(
                    android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://play.google.com/store/apps/details?id=$pkg"),
                    ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) },
                )
            }
        }
    }
}

fun toast(ctx: android.content.Context, msg: String) {
    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
}


/** Real Android share sheet with the workout summary. */
fun shareWorkout(ctx: android.content.Context, w: com.hevyclone.app.data.Workout) {
    val unit = com.hevyclone.app.data.Repo.settings.unit
    val uLabel = com.hevyclone.app.data.Calc.unitLabel(unit)
    val sb = StringBuilder()
    sb.appendLine(w.name)
    sb.appendLine(com.hevyclone.app.data.Calc.fmtDateFull(w.startedAt))
    sb.appendLine(
        L10n.s("Time", "Temps") + " " + com.hevyclone.app.data.Calc.fmtDur(w.endedAt - w.startedAt) +
        " · " + L10n.s("Volume", "Volume") + " " + com.hevyclone.app.data.Calc.fmtVol(com.hevyclone.app.data.Calc.vol(w), unit) + " $uLabel" +
        " · " + L10n.s("Records", "Records") + " " + w.prs.size
    )
    sb.appendLine()
    for (ex in w.exercises) {
        sb.appendLine(exName(ex.name) + " (" + ex.sets.size + " " + L10n.s("series", "séries") + ")")
        for (st in ex.sets) {
            val line = if (ex.muscle == "Cardio") com.hevyclone.app.data.Calc.fmtCardioSet(st.mins, st.km)
                else (if (st.kg != null) "${com.hevyclone.app.data.Calc.fmtKg(st.kg, unit)} $uLabel × " else "× ") + (st.reps ?: "—")
            sb.appendLine("  " + line)
        }
    }
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, sb.toString())
    }
    ctx.startActivity(android.content.Intent.createChooser(intent, L10n.s("Share workout", "Partager la séance")))
}

/** Export all workouts as CSV to cache dir, then share the file.
 *  Cardio sets export their Min/Km columns (round-trips through parseCsv). */
fun exportCsv(ctx: android.content.Context) {
    val unit = com.hevyclone.app.data.Repo.settings.unit
    val dir = java.io.File(ctx.cacheDir, "exports").apply { mkdirs() }
    val f = java.io.File(dir, "hevy-seances.csv")
    f.bufferedWriter().use { out ->
        out.write("Date;Heure;Exercice;Serie;${com.hevyclone.app.data.Calc.unitLabel(unit)};Reps;Min;Km\n")
        for (w in com.hevyclone.app.data.Repo.workouts.sortedBy { it.startedAt }) {
            // full "d MMM yyyy" date — parseCsv needs the year (fmtDateShort omits it and
            // re-imports used to collapse every workout around "now")
            val date = com.hevyclone.app.data.Calc.localDate(w.startedAt)
                .format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.FRANCE))
            val time = com.hevyclone.app.data.Calc.fmtTime(w.startedAt)
            for (ex in w.exercises) {
                ex.sets.forEachIndexed { i, st ->
                    out.write(
                        "$date;$time;${exName(ex.name).replace(';', ',')};${i + 1};" +
                            "${st.kg?.let { com.hevyclone.app.data.Calc.fmtKg(it, unit) } ?: ""};${st.reps ?: ""};" +
                            "${st.mins ?: ""};${st.km?.let { com.hevyclone.app.data.Calc.trimNum(it) } ?: ""}\n"
                    )
                }
            }
        }
    }
    val uri = androidx.core.content.FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(android.content.Intent.createChooser(intent, L10n.s("Export workouts", "Exporter les séances")))
}

/** Export the full database as a JSON backup file, shared as attachment. */
fun shareBackup(ctx: android.content.Context) {
    val dir = java.io.File(ctx.cacheDir, "exports").apply { mkdirs() }
    val f = java.io.File(dir, "hevy-sauvegarde.json")
    f.writeText(com.hevyclone.app.data.Repo.backupJson())
    val uri = androidx.core.content.FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(android.content.Intent.createChooser(intent, L10n.s("Backup", "Sauvegarde")))
}

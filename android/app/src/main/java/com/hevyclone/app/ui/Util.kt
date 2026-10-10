package com.hevyclone.app.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.composed
import androidx.compose.ui.Modifier

fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )
}

fun toast(ctx: android.content.Context, msg: String) {
    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
}


/** Real Android share sheet with the workout summary. */
fun shareWorkout(ctx: android.content.Context, w: com.hevyclone.app.data.Workout) {
    val unit = com.hevyclone.app.data.Repo.settings.unit
    val sb = StringBuilder()
    sb.appendLine(w.name)
    sb.appendLine(com.hevyclone.app.data.Calc.fmtDateFull(w.startedAt))
    sb.appendLine(
        "Temps " + com.hevyclone.app.data.Calc.fmtDur(w.endedAt - w.startedAt) +
        " · Volume " + com.hevyclone.app.data.Calc.fmtVol(com.hevyclone.app.data.Calc.vol(w), unit) + " kg" +
        " · Records " + w.prs.size
    )
    sb.appendLine()
    for (ex in w.exercises) {
        sb.appendLine(exName(ex.name) + " (" + ex.sets.size + " séries)")
        for (st in ex.sets) {
            val line = if (ex.muscle == "Cardio") com.hevyclone.app.data.Calc.fmtCardioSet(st.mins, st.km)
                else (if (st.kg != null) com.hevyclone.app.data.Calc.fmtKg(st.kg, unit) + " kg" else "") + " × " + (st.reps ?: "—")
            sb.appendLine("  " + line)
        }
    }
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, sb.toString())
    }
    ctx.startActivity(android.content.Intent.createChooser(intent, "Partager la séance"))
}

/** Export all workouts as CSV to cache dir, then share the file. */
fun exportCsv(ctx: android.content.Context) {
    val unit = com.hevyclone.app.data.Repo.settings.unit
    val dir = java.io.File(ctx.cacheDir, "exports").apply { mkdirs() }
    val f = java.io.File(dir, "hevy-seances.csv")
    f.bufferedWriter().use { out ->
        out.write("Date;Heure;Exercice;Serie;KG;Reps\n")
        for (w in com.hevyclone.app.data.Repo.workouts.sortedBy { it.startedAt }) {
            val date = com.hevyclone.app.data.Calc.fmtDateShort(w.startedAt)
            val time = com.hevyclone.app.data.Calc.fmtTime(w.startedAt)
            for (ex in w.exercises) {
                ex.sets.forEachIndexed { i, st ->
                    out.write("$date;$time;${exName(ex.name).replace(';', ',')};${i + 1};${st.kg?.let { com.hevyclone.app.data.Calc.fmtKg(it, unit) } ?: ""};${st.reps ?: ""}\n")
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
    ctx.startActivity(android.content.Intent.createChooser(intent, "Exporter les séances"))
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
    ctx.startActivity(android.content.Intent.createChooser(intent, "Sauvegarde"))
}

package com.hevyclone.app.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Pure workout math + formatting + demo seed. No Android dependencies (unit-testable). */
object Calc {
    const val LB = 2.2046226

    fun e1rm(kg: Double, reps: Int): Double = if (reps > 0) kg * (1 + reps / 30.0) else kg

    fun round125(x: Double): Double = Math.round(x / 1.25) * 1.25

    fun vol(w: Workout): Double = w.exercises.sumOf { ex ->
        ex.sets.filter { it.done && it.kg != null && it.reps != null }.sumOf { it.kg!! * it.reps!! }
    }

    fun reps(w: Workout): Int = w.exercises.sumOf { ex ->
        ex.sets.filter { it.done && it.reps != null }.sumOf { it.reps!! }
    }

    fun setsDone(w: Workout): Int = w.exercises.sumOf { ex ->
        ex.sets.count { it.done && (it.kg != null || it.reps != null) }
    }

    private fun r1(x: Double): Double = Math.round(x * 10) / 10.0
    private fun trim(x: Double): String =
        if (Math.round(x).toDouble() == x) Math.round(x).toString() else String.format(Locale.US, "%.1f", x)

    fun fmtKg(kg: Double?, unit: String): String {
        if (kg == null) return ""
        val v = if (unit == "lb") kg * LB else kg
        return trim(r1(v))
    }

    /** Display label for the weight unit, Hevy style: kgs / lbs. */
    fun unitLabel(unit: String): String = if (unit == "lb") "lbs" else "kg"

    fun toKg(text: String, unit: String): Double? {
        val v = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
        if (v <= 0) return null
        return if (unit == "lb") v / LB else v
    }

    fun fmtVol(kg: Double, unit: String): String =
        String.format(Locale.FRANCE, "%,d", Math.round(if (unit == "lb") kg * LB else kg))

    fun toDisplay(kg: Double, unit: String): Double = if (unit == "lb") kg * LB else kg

    fun fmtDur(ms: Long): String {
        val totalMin = ms / 60000
        val h = totalMin / 60
        val m = totalMin % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    fun fmtDateShort(ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM", Locale.FRANCE))

    fun fmtDateFull(ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRANCE))

    fun fmtTime(ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm", Locale.FRANCE))

    fun localDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()

    fun weekStart(ms: Long): Long {
        var d = localDate(ms)
        while (d.dayOfWeek != DayOfWeek.MONDAY) d = d.minusDays(1)
        return d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun dayKey(ms: Long): String = localDate(ms).toString()

    fun streak(workouts: List<Workout>, nowMs: Long): Int {
        val days = workouts.map { dayKey(it.startedAt) }.toSet()
        var d = LocalDate.now().let { it }
        var now = nowMs
        if (dayKey(now) !in days) now -= 86400000L
        var n = 0
        while (dayKey(now) in days) { n++; now -= 86400000L }
        return n
    }

    fun totalVol(workouts: List<Workout>): Double = workouts.sumOf { vol(it) }

    fun weekStats(workouts: List<Workout>, nowMs: Long): WeekStats {
        val ws = weekStart(nowMs)
        val inWeek = workouts.filter { it.startedAt >= ws }
        return WeekStats(
            count = inWeek.size,
            vol = inWeek.sumOf { vol(it) },
            reps = inWeek.sumOf { reps(it) },
            prs = inWeek.sumOf { it.prs.size },
        )
    }

    fun weekly(workouts: List<Workout>, n: Int, unit: String, nowMs: Long): List<Pair<String, Double>> {
        val ws = weekStart(nowMs)
        return (n - 1 downTo 0).map { i ->
            val start = ws - i * 7L * 86400000L
            val end = start + 7L * 86400000L
            val vol = workouts.filter { it.startedAt in start until end }.sumOf { vol(it) }
            fmtDateShort(start) to toDisplay(vol, unit)
        }
    }

    fun muscleDist(workouts: List<Workout>, n: Int): List<Pair<String, Double>> {
        val m = HashMap<String, Double>()
        for (w in workouts) for (ex in w.exercises) for (s in ex.sets) {
            if (!s.done || s.kg == null || s.reps == null) continue
            m[ex.muscle] = (m[ex.muscle] ?: 0.0) + s.kg!! * s.reps!!
        }
        return m.entries.sortedByDescending { it.value }.take(n).map { it.key to it.value }
    }

    fun recentPrs(workouts: List<Workout>, n: Int): List<Triple<PrRec, Long, String>> {
        val out = mutableListOf<Triple<PrRec, Long, String>>()
        for (w in workouts.sortedByDescending { it.startedAt })
            for (p in w.prs) out.add(Triple(p, w.startedAt, w.name))
        return out.take(n)
    }

    fun e1rmSeries(name: String, workouts: List<Workout>, maxPts: Int = 12): List<Pair<String, Double>> {
        val pts = mutableListOf<Pair<String, Double>>()
        for (w in workouts.sortedBy { it.startedAt }) {
            val ex = w.exercises.firstOrNull { it.name == name } ?: continue
            val best = ex.sets.filter { (it.kg ?: 0.0) > 0 && (it.reps ?: 0) > 0 }
                .maxOfOrNull { e1rm(it.kg!!, it.reps!!) }
            if (best != null) pts.add(fmtDateShort(w.startedAt) to best)
        }
        return pts.takeLast(maxPts)
    }

    fun prevFor(name: String, workouts: List<Workout>): List<String>? {
        for (w in workouts.sortedByDescending { it.startedAt }) {
            val ex = w.exercises.firstOrNull { e -> e.name == name && e.sets.any { it.kg != null || it.reps != null } }
            if (ex != null) {
                return ex.sets.map { s ->
                    if (s.kg == null && s.reps == null) "—"
                    else "${fmtKg(s.kg, "kg")} × ${s.reps ?: "—"}"
                }
            }
        }
        return null
    }

    /** Recompute PR cache, stamp per-set flags and per-workout PR lists (chronological). */
    fun rebuildPrs(workouts: List<Workout>): Map<String, PrBest> {
        val cache = HashMap<String, PrBest>()
        for (w in workouts.sortedBy { it.startedAt }) {
            val prs = mutableListOf<PrRec>()
            for (ex in w.exercises) {
                val prev = cache[ex.name]
                val pw = prev?.weight ?: 0.0
                val pe = prev?.e1rm ?: 0.0
                for (s in ex.sets) {
                    s.prW = false; s.prE = false
                    val kg = s.kg ?: continue
                    val reps = s.reps ?: continue
                    if (!s.done) continue
                    if (kg > 0 && kg > pw) s.prW = true
                    if (kg > 0 && e1rm(kg, reps) > pe) s.prE = true
                }
                val done = ex.sets.filter { it.kg != null && it.reps != null && it.done && it.kg!! > 0 }
                if (done.isNotEmpty()) {
                    val bw = done.maxOf { it.kg!! }
                    val be = done.maxOf { e1rm(it.kg!!, it.reps!!) }
                    if (bw > pw) prs.add(PrRec(ex.name, "Weight", bw))
                    if (be > pe) prs.add(PrRec(ex.name, "Est. 1RM", be))
                    val oldW = prev?.weight ?: 0.0
                    val oldE = prev?.e1rm ?: 0.0
                    cache[ex.name] = PrBest(
                        weight = if (bw > oldW) bw else oldW,
                        weightDate = if (bw > oldW) w.startedAt else (prev?.weightDate ?: w.startedAt),
                        e1rm = if (be > oldE) be else oldE,
                        e1rmDate = if (be > oldE) w.startedAt else (prev?.e1rmDate ?: w.startedAt),
                    )
                }
            }
            w.prs = prs
        }
        return cache
    }

    // ---------- deterministic demo seed ----------

    private class SeedRow(val name: String, val nSets: Int, val reps: Int, val base: Double, val inc: Double)

    fun seed(nowMs: Long): Pair<List<Workout>, List<Routine>> {
        var s = 987654321L
        fun rnd(): Double {
            s = (s * 1103515245L + 12345L) % 2147483648L
            return s / 2147483648.0
        }
        val T = mapOf(
            "push" to listOf(
                SeedRow("Barbell Bench Press", 4, 8, 70.0, 1.1),
                SeedRow("Incline Dumbbell Bench Press", 3, 10, 24.0, 0.35),
                SeedRow("Machine Chest Press", 3, 12, 55.0, 0.8),
                SeedRow("Lateral Raise", 3, 15, 9.0, 0.2),
                SeedRow("Tricep Pushdown", 3, 12, 27.0, 0.4),
                SeedRow("Overhead Cable Extension", 3, 11, 22.0, 0.35),
            ),
            "pull" to listOf(
                SeedRow("Lat Pulldown", 4, 10, 62.0, 0.8),
                SeedRow("Seated Cable Row", 3, 10, 57.0, 0.8),
                SeedRow("Dumbbell Row", 3, 10, 32.0, 0.4),
                SeedRow("Face Pull", 3, 15, 22.0, 0.25),
                SeedRow("Barbell Curl", 3, 10, 32.0, 0.3),
                SeedRow("Hammer Curl", 3, 12, 15.0, 0.25),
            ),
            "legs" to listOf(
                SeedRow("Barbell Squat", 4, 8, 97.0, 1.4),
                SeedRow("Romanian Deadlift", 3, 10, 92.0, 1.0),
                SeedRow("Leg Press", 3, 12, 185.0, 2.2),
                SeedRow("Lying Leg Curl", 3, 12, 46.0, 0.4),
                SeedRow("Standing Calf Raise", 4, 15, 85.0, 0.9),
                SeedRow("Cable Crunch", 3, 15, 31.0, 0.4),
            ),
        )
        val names = mapOf("push" to "Push Day", "pull" to "Pull Day", "legs" to "Leg Day")
        val cycle = listOf("push", "pull", "rest", "legs", "rest", "push", "pull", "rest", "legs", "rest")

        val workouts = mutableListOf<Workout>()
        var wid = 1L
        for (back in 83 downTo 0) {
            val kind: String = if (back < 5) listOf("push", "pull", "legs", "push", "pull")[4 - back]
            else cycle[(83 - back) % cycle.size]
            if (kind == "rest") continue
            if (back > 6 && rnd() < 0.10) continue
            val cal = java.util.Calendar.getInstance().apply {
                timeInMillis = nowMs
                add(java.util.Calendar.DAY_OF_YEAR, -back)
            }
            val start: Long = if (back == 0) nowMs - ((110 + rnd() * 30) * 60000L).toLong()
            else {
                cal.set(java.util.Calendar.HOUR_OF_DAY, 17 + (rnd() * 3).toInt())
                cal.set(java.util.Calendar.MINUTE, (rnd() * 60).toInt())
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            var end = start + ((55 + rnd() * 40) * 60000L).toLong()
            if (end > nowMs - 30000) end = nowMs - 30000
            if (end <= start) continue
            val weekIdx = (83 - back) / 7
            val exs = T[kind]!!.mapNotNull { row ->
                val kg = round125(row.base + row.inc * weekIdx)
                val sets = mutableListOf<SetEntry>()
                for (i in 0 until row.nSets) {
                    val rv = if (rnd() < 0.22) row.reps - 1 else if (rnd() > 0.9) row.reps + 1 else row.reps
                    sets.add(SetEntry(kg, maxOf(4, rv), done = true))
                }
                ExEntry(row.name, EX[row.name]?.muscle ?: "Quads", "", false, sets)
            }.toMutableList()
            workouts.add(Workout(wid++, names[kind]!!, start, end, exs))
        }

        fun routineRows(rows: List<SeedRow>) = rows.map { r ->
            ExEntry(r.name, EX[r.name]?.muscle ?: "", "", false,
                MutableList(r.nSets) { SetEntry(round125(r.base), r.reps, done = true) })
        }.toMutableList()
        var rid = 1L
        val routines = listOf(
            Routine(rid++, "Push Day", routineRows(T["push"]!!)),
            Routine(rid++, "Pull Day", routineRows(T["pull"]!!)),
            Routine(rid++, "Leg Day", routineRows(T["legs"]!!)),
            Routine(rid++, "Upper Body", listOf(
                ExEntry("Barbell Bench Press", "Chest", "", false, MutableList(4) { SetEntry(72.5, 8) }),
                ExEntry("Lat Pulldown", "Lats", "", false, MutableList(4) { SetEntry(62.0, 10) }),
                ExEntry("Seated Cable Row", "Lats", "", false, MutableList(3) { SetEntry(57.0, 10) }),
                ExEntry("Machine Shoulder Press", "Shoulders", "", false, MutableList(3) { SetEntry(40.0, 10) }),
                ExEntry("Barbell Curl", "Biceps", "", false, MutableList(3) { SetEntry(32.5, 10) }),
                ExEntry("Tricep Pushdown", "Triceps", "", false, MutableList(3) { SetEntry(27.5, 12) }),
            ).toMutableList()),
        )
        return workouts to routines
    }
}

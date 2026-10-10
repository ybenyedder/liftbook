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
        ex.sets.count { it.done && (it.kg != null || it.reps != null || it.mins != null || it.km != null) }
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

    /** Cardio exercises (muscle == "Cardio") log minutes/km instead of weight × reps. */
    fun isCardioName(name: String): Boolean = EX[name]?.muscle == "Cardio"

    /** "22min · 5.2km" for a cardio set; "—" when empty. */
    fun fmtCardioSet(mins: Int?, km: Double?): String {
        val parts = mutableListOf<String>()
        if (mins != null && mins > 0) parts.add(if (mins >= 60) "${mins / 60}h${if (mins % 60 > 0) "%02d".format(mins % 60) else ""}" else "${mins}min")
        if (km != null && km > 0) parts.add(trim(km) + "km")
        return if (parts.isEmpty()) "—" else parts.joinToString(" · ")
    }

    /** Plain number (1 decimal max) for input fields. */
    fun trimNum(x: Double): String = trim(x)

    /** Greedy plate breakdown per side for a target total (bar included). Returns plate→count. */
    fun platesForSide(targetKg: Double, barKg: Double, unit: String): List<Pair<Double, Int>> {
        val avail = if (unit == "lb") listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)
        else listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
        // barKg == 0 means "no bar" — everything goes on plates (was: default bar subtracted).
        var perSide = ((targetKg - barKg) / 2 * 100).toInt() / 100.0
        if (perSide < 0) perSide = 0.0
        val out = mutableListOf<Pair<Double, Int>>()
        for (p in avail) {
            val n = kotlin.math.floor(perSide / p + 1e-9).toInt()
            if (n > 0) { out.add(p to n); perSide -= n * p }
        }
        return out
    }

    fun toKg(text: String, unit: String): Double? {
        val v = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
        if (v <= 0) return null
        return if (unit == "lb") v / LB else v
    }

    fun fmtVol(kg: Double, unit: String): String =
        String.format(Locale.FRANCE, "%,d", Math.round(if (unit == "lb") kg * LB else kg))

    fun toDisplay(kg: Double, unit: String): Double = if (unit == "lb") kg * LB else kg

    fun fmtDur(ms: Long): String {
        val totalMin = (maxOf(0L, ms)) / 60000
        val h = totalMin / 60
        val m = totalMin % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    /** Live per-second clock label: 04:37, or 1:12:45 past the hour. Never negative. */
    fun fmtClock(ms: Long): String {
        val s = maxOf(0L, ms) / 1000
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
        else String.format(Locale.US, "%02d:%02d", m, sec)
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
        // Walk calendar days (not 24h steps) so a DST change can't skip a day.
        var d = localDate(nowMs)
        if (d.toString() !in days) d = d.minusDays(1)
        var n = 0
        while (d.toString() in days) { n++; d = d.minusDays(1) }
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

    // ---------- CSV import ----------
    // Format (export): Date;Heure;Exercice;Serie;KG;Reps[;Min;Km] — ';' or ',' accepted.
    fun parseCsv(content: String): List<Workout> {
        data class Key(val date: String, val time: String)
        data class Row(val si: Int, val kg: Double?, val reps: Int?, val mins: Int?, val km: Double?)

        val groups = LinkedHashMap<Key, LinkedHashMap<String, MutableList<Row>>>()
        content.lines().drop(1).filter { it.isNotBlank() }.forEach { line ->
            val sep = if (line.contains(';')) ';' else ','
            val parts = line.split(sep)
            if (parts.size < 6) return@forEach
            val date = parts[0].trim(); val time = parts[1].trim(); val exName = parts[2].trim()
            val si = parts[3].trim().toIntOrNull() ?: return@forEach
            val kg = parts[4].trim().replace(',', '.').toDoubleOrNull()
            val reps = parts[5].trim().toIntOrNull()
            val mins = parts.getOrNull(6)?.trim()?.takeIf { it.isNotEmpty() }?.replace(',', '.')?.toDoubleOrNull()?.toInt()
            val km = parts.getOrNull(7)?.trim()?.takeIf { it.isNotEmpty() }?.replace(',', '.')?.toDoubleOrNull()
            groups.getOrPut(Key(date, time)) { LinkedHashMap() }
                .getOrPut(exName) { mutableListOf() }
                .add(Row(si, kg, reps, mins, km))
        }
        val fmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRANCE)
        return groups.entries.sortedBy { it.key.date }.mapIndexed { i, (key, exMap) ->
            val ms = runCatching {
                java.time.LocalDate.parse(key.date, fmt).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }.getOrElse { System.currentTimeMillis() - (groups.size - i) * 36_00_000L }
            val exercises = exMap.entries.map { (frName, sets) ->
                val canonical = L10nData.NAME_FR.entries.firstOrNull { it.value == frName }?.key ?: frName
                val muscle = EX[canonical]?.muscle ?: ""
                ExEntry(canonical, muscle, "", false, null,
                    sets.sortedBy { it.si }.map { SetEntry(it.kg, it.reps, it.mins, it.km, done = true) }.toMutableList())
            }.toMutableList()
            Workout(10_000_000L + i, "Séance", ms, ms + 3_600_000, exercises)
        }
    }

    // ---------- Hevy / Strong CSV import ----------

    /** Hevy's own export date: "18 Sep 2026, 17:31" — month names follow the app language. */
    private val EXPORT_MONTH_LOCALES = listOf(Locale.US, Locale.FRENCH, Locale.GERMANY, Locale.ITALIAN, java.util.Locale.forLanguageTag("es"), java.util.Locale.forLanguageTag("pt"))
    private val EN_MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    private fun parseLocalizedDate(s: String): Long? {
        for (loc in EXPORT_MONTH_LOCALES) {
            for (pat in listOf("d MMM yyyy, HH:mm", "d MMMM yyyy, HH:mm", "d MMM yyyy HH:mm")) {
                runCatching {
                    return java.time.LocalDateTime.parse(s, java.time.format.DateTimeFormatter.ofPattern(pat, loc))
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
            }
        }
        // Japanese month form "5 9月 2026, 20:39"
        val jp = Regex("(\\d{1,2})\\s*月").replace(s) { m -> EN_MONTHS[m.groupValues[1].toInt().coerceIn(1, 12) - 1] }
        if (jp != s) runCatching {
            return java.time.LocalDateTime.parse(jp, java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.US))
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        return null
    }

    /** Parse a date as emitted by Hevy/Strong exports: ISO-8601 (with T, space, offset), epoch s/ms,
     *  Hevy's localized "d MMM yyyy, HH:mm", Strong's "Mon Jan 02 17:00:00 GMT+01:00 2023". */
    fun parseExportDate(raw: String): Long? {
        val s = raw.trim().trim('"')
        if (s.isEmpty()) return null
        // epoch seconds / millis
        s.toLongOrNull()?.let { n -> return if (n > 10_000_000_000L) n else n * 1000L }
        val iso = s.replace(' ', 'T')
        // trailing zone like +01:00 / GMT+01:00 / Z
        runCatching { return java.time.OffsetDateTime.parse(iso).toInstant().toEpochMilli() }
        runCatching { return java.time.OffsetDateTime.parse(if (iso.endsWith("Z")) iso else "${iso}Z").toInstant().toEpochMilli() }
        runCatching { return java.time.LocalDateTime.parse(iso).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }
        parseLocalizedDate(s)?.let { return it }
        // Strong-style: "Mon Jan 02 17:00:00 GMT+01:00 2023"
        runCatching {
            return java.time.ZonedDateTime.parse(
                s, java.time.format.DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US)
            ).toInstant().toEpochMilli()
        }
        return null
    }

    private data class HevyCols(
        val date: Int? = null, val endDate: Int? = null, val exercise: Int, val weight: Int? = null,
        val reps: Int? = null, val order: Int? = null, val seconds: Int? = null, val done: Int? = null,
        val name: Int? = null, val unit: Int? = null, val notes: Int? = null, val superset: Int? = null,
    )

    /** Full-file CSV reader (quotes with embedded commas/newlines honored); ';' or ',' accepted. */
    private fun readCsvTable(content: String): List<List<String>> {
        val rows = mutableListOf<MutableList<String>>()
        var cur = mutableListOf<String>(); val cell = StringBuilder(); var inQ = false; var started = false
        for (c in content) {
            when {
                c == '"' -> { inQ = !inQ; started = true }
                !inQ && (c == ',' || c == ';') -> { cur.add(cell.toString().trim()); cell.setLength(0); started = false }
                !inQ && c == '\n' -> { cur.add(cell.toString().trim()); cell.setLength(0); rows.add(cur); cur = mutableListOf(); started = false }
                !inQ && c == '\r' -> {}
                else -> { cell.append(c); started = true }
            }
        }
        if (cur.isNotEmpty()) { cur.add(cell.toString().trim()); rows.add(cur) }
        return rows.filter { r -> r.any { it.isNotEmpty() } }
    }

    /** CSV cell splitter handling quotes + , or ; separators. */
    private fun splitCsv(line: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder(); var inQ = false
        for (c in line) when {
            c == '"' -> inQ = !inQ
            (c == ',' || c == ';') && !inQ -> { out.add(cur.toString().trim()); cur.clear() }
            else -> cur.append(c)
        }
        out.add(cur.toString().trim())
        return out
    }

    private fun norm(s: String) = s.lowercase().replace("_", "").replace(" ", "").replace(".", "")

    /** Fuzzy column mapping — accepts Strong-style headers (Date, Workout Name, Exercise Name, Set Order,
     *  Weight, Weight Unit, Reps, Seconds…) as well as snake_case ones (exercise_name, weight_kg, set_index…). */
    private fun mapColumns(header: List<String>): HevyCols? {
        val cols = header.map { norm(it) }
        fun findFirst(vararg keys: String, not: List<String> = emptyList()): Int? =
            cols.indexOfFirst { c -> keys.any { k -> c.contains(k) } && not.none { c.contains(it) } }.takeIf { it >= 0 }
        val exercise = findFirst("exercisename", "exercise") ?: return null
        return HevyCols(
            exercise = exercise,
            date = findFirst("startdate", "start", "date", not = listOf("end", "duration")),
            endDate = findFirst("enddate", "endtime"),
            weight = findFirst("weightkg", "weightlbs", "weight", not = listOf("unit")),
            reps = findFirst("reps", "repetitions"),
            order = findFirst("setorder", "setindex", "setno"),
            seconds = findFirst("seconds", "durations", "durationsec"),
            done = findFirst("completed", "checked"),
            name = findFirst("workoutname", "title", "name", not = listOf("exercise")),
            unit = findFirst("weightunit", "unit"),
            notes = findFirst("exnotes", "exercisenotes", "notes", not = listOf("workout")),
            superset = findFirst("superset"),
        )
    }

    /** Map a Hevy/Strong exercise label to our canonical EN key (FR names remapped, unknowns kept). */
    fun canonicalExercise(label: String): String {
        val t = label.trim()
        if (EX.containsKey(t)) return t
        val clean = norm(t).replace("(", "").replace(")", "")
        val exact = L10nData.NAME_FR.entries.firstOrNull { norm(it.value).replace("(", "").replace(")", "") == clean }?.key
        if (exact != null) return exact
        // Hevy FR names may drop our parenthetical suffix ("Tirage Poitrine" vs "Tirage Poitrine (Machine)")
        return L10nData.NAME_FR.entries.firstOrNull { norm(it.value).replace("(", "").replace(")", "").startsWith(clean) && clean.length >= 5 }?.key ?: t
    }

    /**
     * Parse a Hevy (or Strong) export CSV. Rows are one per set; consecutive rows with the same
     * date form one workout, consecutive same-exercise rows form its exercise block.
     * Files without any date column are treated as routine/template definitions.
     */
    fun parseHevyCsv(content: String): Pair<List<Workout>, List<Routine>> {
        val table = readCsvTable(content)
        if (table.size < 2) return emptyList<Workout>() to emptyList<Routine>()
        val cols = mapColumns(table.first()) ?: return emptyList<Workout>() to emptyList<Routine>()
        data class Row(
            val date: Long?, val endDate: Long?, val ex: String, val kg: Double?, val reps: Int?,
            val order: Int, val done: Boolean, val workoutName: String?, val notes: String, val superset: Boolean,
        )
        val rows = mutableListOf<Row>()
        for (i in 1 until table.size) {
            val p = table[i]
            if (p.size <= cols.exercise) continue
            val exRaw = p[cols.exercise]
            if (exRaw.isEmpty()) continue
            val unitLb = cols.unit?.let { j -> p.getOrNull(j)?.contains("lb", true) == true } ?: false
            val kg = cols.weight?.let { j -> p.getOrNull(j)?.replace(',', '.')?.toDoubleOrNull() }
                ?.let { if (unitLb) it / LB else it }
            val dateStr = cols.date?.let { j -> p.getOrNull(j) } ?: ""
            val date = parseExportDate(dateStr)
            val doneRaw = cols.done?.let { j -> p.getOrNull(j)?.lowercase() } ?: "true"
            val sup = cols.superset?.let { j -> p.getOrNull(j)?.trim()?.isNotEmpty() == true && p.getOrNull(j) != "0" } ?: false
            rows.add(
                Row(
                    date = date,
                    endDate = cols.endDate?.let { j -> p.getOrNull(j)?.let { parseExportDate(it) } } ?: date,
                    ex = exRaw,
                    kg = kg,
                    reps = cols.reps?.let { j -> p.getOrNull(j)?.toDoubleOrNull()?.toInt() },
                    order = cols.order?.let { j -> p.getOrNull(j)?.toIntOrNull() } ?: rows.size,
                    done = doneRaw !in listOf("false", "0", "no", "warmup"),
                    workoutName = cols.name?.let { j -> p.getOrNull(j) }?.takeIf { it.isNotEmpty() },
                    notes = cols.notes?.let { j -> p.getOrNull(j) ?: "" } ?: "",
                    superset = sup,
                )
            )
        }
        if (rows.isEmpty()) return emptyList<Workout>() to emptyList<Routine>()

        // No date column anywhere → template/routine file
        if (rows.all { it.date == null }) {
            val exs = LinkedHashMap<String, MutableList<SetEntry>>()
            val sups = HashSet<String>()
            for (r in rows) {
                exs.getOrPut(r.ex) { mutableListOf() }.add(SetEntry(r.kg, r.reps, done = true))
                if (r.superset) sups.add(r.ex)
            }
            val routine = Routine(
                0, rows.firstOrNull()?.workoutName ?: "Routine Hevy",
                exs.map { (label, sets) ->
                    val canon = canonicalExercise(label)
                    ExEntry(canon, EX[canon]?.muscle ?: "", "", label in sups, null, sets)
                }.toMutableList(),
            )
            return emptyList<Workout>() to listOf(routine)
        }

        // Workouts: group by date key (fall back to row blocks with same consecutive name)
        val workouts = mutableListOf<Workout>()
        var i = 0
        var id = 10_000_000L
        while (i < rows.size) {
            val r = rows[i]
            val key = r.date
            if (key == null) { i++; continue }
            var j = i
            val exs = LinkedHashMap<String, MutableList<SetEntry>>()
            val names = HashMap<String, String>()
            val sups = HashSet<String>()
            while (j < rows.size && rows[j].date == key) {
                val row = rows[j]
                exs.getOrPut(row.ex) { mutableListOf() }.add(SetEntry(row.kg, row.reps, done = row.done))
                row.workoutName?.let { names[row.ex] = it }
                if (row.superset) sups.add(row.ex)
                j++
            }
            val end = rows.firstOrNull { it.date == key }?.endDate ?: key
            workouts.add(
                Workout(
                    id = id++,
                    name = names.values.firstOrNull() ?: "Séance Hevy",
                    startedAt = key,
                    endedAt = maxOf(end, key + 60_000),
                    exercises = exs.map { (label, sets) ->
                        val canon = canonicalExercise(label)
                        ExEntry(canon, EX[canon]?.muscle ?: "", "", label in sups, null, sets)
                    }.toMutableList(),
                ),
            )
            i = j
        }
        return workouts.sortedBy { it.startedAt } to emptyList<Routine>()
    }

    /**
     * Hevy never exports templates — rebuild routines from imported workout names:
     * each distinct workout name (case-insensitive, skipping existing routine names)
     * becomes a routine mirroring its most recent occurrence (exercises, sets, supersets).
     */
    fun routinesFromWorkouts(workouts: List<Workout>, existingRoutineNames: Set<String>): List<Routine> {
        val taken = existingRoutineNames.map { it.lowercase() }.toMutableSet()
        val out = mutableListOf<Routine>()
        var id = 10_000_000L
        for ((name, group) in workouts.groupBy { it.name.trim() }) {
            if (name.isEmpty() || name.lowercase() in taken) continue
            val latest = group.maxByOrNull { it.startedAt } ?: continue
            out.add(
                Routine(
                    id++,
                    name,
                    latest.exercises.map { ex ->
                        ExEntry(ex.name, ex.muscle, "", ex.superset, ex.restSec,
                            ex.sets.map { SetEntry(it.kg, it.reps, it.mins, it.km, it.done) }.toMutableList())
                    }.toMutableList(),
                )
            )
            taken.add(name.lowercase())
        }
        return out
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
                ExEntry(row.name, EX[row.name]?.muscle ?: "Quads", "", false, null, sets)
            }.toMutableList()
            workouts.add(Workout(wid++, names[kind]!!, start, end, exs))
        }

        fun routineRows(rows: List<SeedRow>) = rows.map { r ->
            ExEntry(r.name, EX[r.name]?.muscle ?: "", "", false, null,
                MutableList(r.nSets) { SetEntry(round125(r.base), r.reps, done = true) })
        }.toMutableList()
        var rid = 1L
        val routines = listOf(
            Routine(rid++, "Push Day", routineRows(T["push"]!!)),
            Routine(rid++, "Pull Day", routineRows(T["pull"]!!)),
            Routine(rid++, "Leg Day", routineRows(T["legs"]!!)),
            Routine(rid++, "Upper Body", listOf(
                ExEntry("Barbell Bench Press", "Chest", "", false, null, MutableList(4) { SetEntry(72.5, 8) }),
                ExEntry("Lat Pulldown", "Lats", "", false, null, MutableList(4) { SetEntry(62.0, 10) }),
                ExEntry("Seated Cable Row", "Lats", "", false, null, MutableList(3) { SetEntry(57.0, 10) }),
                ExEntry("Machine Shoulder Press", "Shoulders", "", false, null, MutableList(3) { SetEntry(40.0, 10) }),
                ExEntry("Barbell Curl", "Biceps", "", false, null, MutableList(3) { SetEntry(32.5, 10) }),
                ExEntry("Tricep Pushdown", "Triceps", "", false, null, MutableList(3) { SetEntry(27.5, 12) }),
            ).toMutableList()),
        )
        return workouts to routines
    }
}

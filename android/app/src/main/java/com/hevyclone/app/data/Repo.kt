package com.hevyclone.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.Json

/** SQLite persistence. Workouts/routines are stored as JSON blobs (single source of truth = in-memory lists). */
class Db(ctx: Context) : SQLiteOpenHelper(ctx, "hevy.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE workouts(id INTEGER PRIMARY KEY AUTOINCREMENT, startedAt INTEGER NOT NULL, endedAt INTEGER NOT NULL, name TEXT NOT NULL, json TEXT NOT NULL)")
        db.execSQL("CREATE TABLE routines(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, pos INTEGER NOT NULL DEFAULT 0, json TEXT NOT NULL)")
        db.execSQL("CREATE TABLE settings(k TEXT PRIMARY KEY, v TEXT NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldV: Int, newV: Int) {
        if (oldV < 2) {
            db.execSQL("ALTER TABLE routines ADD COLUMN pos INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE routines SET pos = id")
        }
    }
}

/** In-memory repository with write-through persistence; `rev` bumps trigger Compose recomposition. */
object Repo {
    private lateinit var db: Db
    private val json = Json { ignoreUnknownKeys = true }

    val workouts = mutableListOf<Workout>()     // sorted by startedAt ASC
    val routines = mutableListOf<Routine>()
    var settings = Settings()
    var draft: Draft? = null
    var prCache: Map<String, PrBest> = emptyMap()
    var rev by mutableStateOf(0)

    // ---- perf caches (invalidated on touch) ----
    private var descCache: List<Workout>? = null
    private var prevCache = HashMap<String, List<String>>()

    private fun touch() {
        descCache = null
        prevCache.clear()
        persistDraft()
        rev++
    }

    private var draftPrefs: android.content.SharedPreferences? = null

    fun persistDraftNow() { persistDraft() }

    private fun persistDraft() {
        val prefs = draftPrefs ?: return
        prefs.edit().apply {
            if (draft != null) putString("draft", json.encodeToString(Draft.serializer(), draft!!))
            else remove("draft")
        }.apply()
    }
    fun touchPublic() { touch() }

    fun init(ctx: Context) {
        if (this::db.isInitialized) return
        draftPrefs = ctx.getSharedPreferences("draft", Context.MODE_PRIVATE)
        draft = draftPrefs?.getString("draft", null)?.let {
            runCatching { json.decodeFromString<Draft>(it) }.getOrNull()
        }
        db = Db(ctx)
        val sRow = db.readableDatabase.rawQuery("SELECT v FROM settings WHERE k='settings'", null).use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
        settings = sRow?.let { runCatching { json.decodeFromString<Settings>(it) }.getOrNull() } ?: Settings(since = System.currentTimeMillis())
        db.readableDatabase.rawQuery("SELECT json FROM workouts ORDER BY startedAt ASC", null).use { c ->
            while (c.moveToNext()) runCatching { json.decodeFromString<Workout>(c.getString(0)) }.getOrNull()?.let { workouts.add(it) }
        }
        db.readableDatabase.rawQuery("SELECT json FROM routines ORDER BY pos ASC, id ASC", null).use { c ->
            while (c.moveToNext()) runCatching { json.decodeFromString<Routine>(c.getString(0)) }.getOrNull()?.let { routines.add(it) }
        }
        prCache = Calc.rebuildPrs(workouts)
        persistSettings()
        touch()
    }

    private fun persistWorkout(w: Workout) {
        val cv = ContentValues().apply {
            put("id", w.id); put("startedAt", w.startedAt); put("endedAt", w.endedAt)
            put("name", w.name); put("json", json.encodeToString(Workout.serializer(), w))
        }
        db.writableDatabase.insertWithOnConflict("workouts", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun persistRoutine(r: Routine) {
        val cv = ContentValues().apply {
            put("id", r.id); put("name", r.name); put("pos", r.pos)
            put("json", json.encodeToString(Routine.serializer(), r))
        }
        db.writableDatabase.insertWithOnConflict("routines", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun persistSettings() {
        val cv = ContentValues().apply { put("k", "settings"); put("v", json.encodeToString(Settings.serializer(), settings)) }
        db.writableDatabase.insertWithOnConflict("settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun nextWorkoutId(): Long = (workouts.maxOfOrNull { it.id } ?: 0L) + 1
    fun nextRoutineId(): Long = (routines.maxOfOrNull { it.id } ?: 0L) + 1
    fun nextPos(): Int = (routines.maxOfOrNull { it.pos } ?: 0) + 1

    fun workoutById(id: Long): Workout? = workouts.firstOrNull { it.id == id }
    fun routineById(id: Long): Routine? = routines.firstOrNull { it.id == id }
    fun workoutsDesc(): List<Workout> {
        descCache?.let { return it }
        val sorted = workouts.sortedByDescending { it.startedAt }
        descCache = sorted
        return sorted
    }

    // ---------- draft lifecycle ----------

    fun startWorkout(routineId: Long?) {
        val r = routineId?.let { routineById(it) }
        val exs = r?.exercises?.map { ex ->
            ExEntry(ex.name, ex.muscle, ex.notes, ex.superset, ex.restSec, ex.sets.map { SetEntry(it.kg, it.reps, done = false) }.toMutableList())
        }?.toMutableList() ?: mutableListOf()
        draft = Draft("workout", routineId, r?.name ?: "Séance", System.currentTimeMillis(), "", exs)
        touch()
    }

    fun startRoutine(routineId: Long?) {
        val r = routineId?.let { routineById(it) }
        val exs = r?.exercises?.map { ex ->
            ExEntry(ex.name, ex.muscle, ex.notes, ex.superset, ex.restSec, ex.sets.map { SetEntry(it.kg, it.reps, it.done) }.toMutableList())
        }?.toMutableList() ?: mutableListOf()
        draft = Draft("routine", routineId, r?.name ?: "Nouvelle Routine", null, "", exs)
        touch()
    }

    /** Start a new workout with the exercises of a past workout (null = most recent). */
    fun startRepeat(workoutId: Long? = null) {
        val last = workoutId?.let { workoutById(it) } ?: workouts.maxByOrNull { it.startedAt } ?: return
        draft = Draft(
            "workout", null, last.name, System.currentTimeMillis(), last.notes,
            last.exercises.map { ex ->
                ExEntry(ex.name, ex.muscle, ex.notes, ex.superset, ex.restSec, ex.sets.map { SetEntry(it.kg, it.reps, done = false) }.toMutableList())
            }.toMutableList(),
        )
        touch()
    }

    fun addExToDraft(name: String) {
        val d = draft ?: return
        d.exercises.add(ExEntry(name, EX[name]?.muscle ?: "", "", false, null, mutableListOf(SetEntry(null, null, done = false))))
        touch()
    }

    fun saveRoutine(name: String): Routine? {
        val d = draft ?: return null
        if (d.exercises.isEmpty()) return null
        val r: Routine = if (d.routineId != null) {
            val existing = routineById(d.routineId!!) ?: return null
            existing.name = name
            existing.exercises = d.exercises
            existing
        } else {
            Routine(nextRoutineId(), name, d.exercises, nextPos()).also { routines.add(it) }
        }
        persistRoutine(r)
        draft = null
        touch()
        Cloud.markDirty()
        return r
    }

    fun finishWorkout(name: String): Workout? {
        val d = draft ?: return null
        val now = System.currentTimeMillis()
        // records are evaluated against history strictly before this session
        val prs = mutableListOf<PrRec>()
        for (ex in d.exercises) {
            val done = ex.sets.filter { (it.kg ?: 0.0) > 0 && (it.reps ?: 0) > 0 && it.done }
            if (done.isEmpty()) continue
            val prev = prFor(ex.name)
            val pw = prev?.weight ?: 0.0
            val pe = prev?.e1rm ?: 0.0
            val bw = done.maxOf { it.kg!! }
            val be = done.maxOf { Calc.e1rm(it.kg!!, it.reps!!) }
            if (bw > pw) prs.add(PrRec(ex.name, "Weight", bw))
            if (be > pe) prs.add(PrRec(ex.name, "Est. 1RM", be))
        }
        val w = Workout(
            id = nextWorkoutId(),
            name = name.trim().ifEmpty { "Séance" },
            startedAt = d.startedAt ?: now - 3600000,
            endedAt = now,
            exercises = d.exercises,
            notes = d.notes,
            prs = prs.toMutableList(),
        )
        workouts.add(w)
        prCache = Calc.rebuildPrs(workouts)
        persistWorkout(w)
        draft = null
        touch()
        Cloud.markDirty()
        return w
    }

    fun discardDraft() { draft = null; touch() }

    fun restoreWorkout(w: Workout) {
        workouts.removeAll { it.id == w.id }
        workouts.add(w)
        workouts.sortBy { it.startedAt }
        persistWorkout(w)
        prCache = Calc.rebuildPrs(workouts)
        touch()
        Cloud.markDirty()
    }

    fun deleteWorkout(id: Long) {
        workoutById(id)?.let { Cloud.tombstoneWorkout(it.startedAt) }
        workouts.removeAll { it.id == id }
        db.writableDatabase.delete("workouts", "id=?", arrayOf(id.toString()))
        prCache = Calc.rebuildPrs(workouts)
        touch()
        Cloud.markDirty()
    }

    fun routineFromWorkout(workoutId: Long, name: String): Routine? {
        val w = workoutById(workoutId) ?: return null
        val r = Routine(nextRoutineId(), name.trim().ifEmpty { w.name }, w.exercises.map { ex ->
            ExEntry(ex.name, ex.muscle, ex.notes, ex.superset, ex.restSec, ex.sets.map { SetEntry(it.kg, it.reps, it.done) }.toMutableList())
        }.toMutableList(), nextPos())
        routines.add(r)
        persistRoutine(r)
        touch()
        Cloud.markDirty()
        return r
    }

    fun renameRoutine(id: Long, name: String) {
        val r = routineById(id) ?: return
        r.name = name.trim().ifEmpty { r.name }
        persistRoutine(r)
        touch()
        Cloud.markDirty()
    }

    fun duplicateRoutine(id: Long): Routine? {
        val r = routineById(id) ?: return null
        val copy = Routine(nextRoutineId(), r.name + " (2)", r.exercises.map { ex ->
            ExEntry(ex.name, ex.muscle, ex.notes, ex.superset, ex.restSec, ex.sets.map { SetEntry(it.kg, it.reps, it.done) }.toMutableList())
        }.toMutableList(), nextPos())
        routines.add(copy)
        persistRoutine(copy)
        touch()
        Cloud.markDirty()
        return copy
    }

    fun deleteRoutine(id: Long) {
        routineById(id)?.let { Cloud.tombstoneRoutine(it.name) }
        routines.removeAll { it.id == id }
        db.writableDatabase.delete("routines", "id=?", arrayOf(id.toString()))
        touch()
        Cloud.markDirty()
    }

    /** Drag & drop reorder (indices in routines list == manual pos order). */
    fun moveRoutine(from: Int, to: Int) {
        if (from == to || from !in routines.indices || to !in routines.indices) return
        val item = routines.removeAt(from)
        routines.add(to, item)
        for (i in routines.indices) routines[i].pos = i
        for (r in routines) persistRoutine(r)
        touch()
        Cloud.markDirty()
    }

    // ---------- settings ----------

    fun setUnit(u: String) { settings.unit = u; persistSettings(); touch(); Cloud.markDirty() }
    fun setRest(sec: Int) { settings.restSec = sec; persistSettings(); touch(); Cloud.markDirty() }
    fun setAccent(a: String) { settings.accent = a; persistSettings(); touch(); Cloud.markDirty() }
    fun setTheme(t: String) { settings.theme = t; persistSettings(); touch(); Cloud.markDirty() }
    fun setProfile(name: String, handle: String) {
        settings.profileName = name; settings.handle = handle
        persistSettings(); touch(); Cloud.markDirty()
    }

    fun setAvatar(url: String) {
        settings.avatarUrl = url
        persistSettings(); touch(); Cloud.markDirty()
    }

    fun backupJson(): String = json.encodeToString(
        BackupData.serializer(), BackupData(workouts.toList(), routines.toList())
    )

    fun restoreBackup(content: String): Boolean {
        val data = runCatching { json.decodeFromString<BackupData>(content) }.getOrNull() ?: return false
        db.writableDatabase.delete("workouts", null, null)
        db.writableDatabase.delete("routines", null, null)
        workouts.clear(); routines.clear()
        data.workouts.forEach { workouts.add(it); persistWorkout(it) }
        data.routines.forEachIndexed { i, r -> r.pos = i; routines.add(r); persistRoutine(r) }
        prCache = Calc.rebuildPrs(workouts)
        touch()
        Cloud.markDirty()
        return true
    }

    fun importCsv(content: String): Int {
        val imported = Calc.parseCsv(content)
        imported.forEach { w ->
            workouts.add(w)
            persistWorkout(w)
        }
        if (imported.isNotEmpty()) {
            prCache = Calc.rebuildPrs(workouts)
            touch()
            Cloud.markDirty()
        }
        return imported.size
    }

    /** Merge a Hevy/Strong export: workouts are added once per start date, routines appended.
     * Hevy never exports templates, so routines are also rebuilt from distinct workout names. */
    fun importHevy(newWorkouts: List<Workout>, newRoutines: List<Routine>): Int {
        val knownDates = workouts.map { it.startedAt }.toSet()
        var added = 0
        for (w in newWorkouts) {
            if (w.startedAt in knownDates) continue
            val fixed = w.copy(id = nextWorkoutId())
            workouts.add(fixed)
            persistWorkout(fixed)
            added++
        }
        val allRoutines = newRoutines +
            Calc.routinesFromWorkouts(newWorkouts, routines.map { it.name }.toSet())
        for (r in allRoutines) {
            val fixed = r.copy(id = nextRoutineId(), pos = nextPos())
            routines.add(fixed)
            persistRoutine(fixed)
        }
        if (added > 0 || allRoutines.isNotEmpty()) {
            workouts.sortBy { it.startedAt }
            prCache = Calc.rebuildPrs(workouts)
            touch()
            Cloud.markDirty()
        }
        return added
    }

    fun wipe(markDirty: Boolean = true) {
        workouts.clear(); routines.clear(); draft = null
        db.writableDatabase.delete("workouts", null, null)
        db.writableDatabase.delete("routines", null, null)
        prCache = emptyMap()
        touch()
        if (markDirty) Cloud.markDirty()
    }

    // ---------- cloud sync ----------

    /** Full snapshot of everything that syncs. */
    fun snapshot(): SyncPayload = SyncPayload(
        workouts = workouts.toList(),
        routines = routines.toList(),
        settings = settings,
        delW = Cloud.currentTombW(),
        delR = Cloud.currentTombR(),
    )

    /** Replace local state wholesale with a remote snapshot (clean pull). No dirty marking. */
    fun replaceAll(p: SyncPayload) {
        db.writableDatabase.delete("workouts", null, null)
        db.writableDatabase.delete("routines", null, null)
        workouts.clear(); routines.clear()
        p.workouts.forEach { workouts.add(it); persistWorkout(it) }
        p.routines.forEachIndexed { i, r -> r.pos = i; routines.add(r); persistRoutine(r) }
        p.settings?.let {
            settings = it
            persistSettings()
        }
        prCache = Calc.rebuildPrs(workouts)
        touch()
    }

    /** Union-merge a remote snapshot into local (conflict / first login). Local deletions win;
     *  remote deletions are applied; duplicates skipped by startedAt (workouts) and name (routines). */
    fun mergeRemote(p: SyncPayload): Boolean {
        val localDelW = Cloud.currentTombW().toSet()
        val localDelR = Cloud.currentTombR().toSet()
        val remoteDelW = p.delW.toSet()
        val remoteDelR = p.delR.toSet()

        val keepW = workouts.filter { it.startedAt !in remoteDelW }
        val keepR = routines.filter { it.name !in remoteDelR }
        val knownW = keepW.map { it.startedAt }.toSet()
        val knownR = keepR.map { it.name }.toSet()
        val addW = p.workouts.filter { it.startedAt !in localDelW && it.startedAt !in knownW }
        val addR = p.routines.filter { it.name !in localDelR && it.name !in knownR }

        if (keepW.size == workouts.size && keepR.size == routines.size && addW.isEmpty() && addR.isEmpty()) return false

        db.writableDatabase.delete("workouts", null, null)
        db.writableDatabase.delete("routines", null, null)
        workouts.clear(); routines.clear()
        keepW.forEach { workouts.add(it); persistWorkout(it) }
        addW.forEach { val f = it.copy(id = nextWorkoutId()); workouts.add(f); persistWorkout(f) }
        workouts.sortBy { it.startedAt }
        keepR.forEach { routines.add(it); persistRoutine(it) }
        addR.forEach { val f = it.copy(id = nextRoutineId(), pos = nextPos()); routines.add(f); persistRoutine(f) }
        prCache = Calc.rebuildPrs(workouts)
        touch()
        return true
    }

    // ---------- queries ----------

    fun prFor(name: String): PrBest? = prCache[name]

    fun e1rmSeries(name: String): List<Pair<String, Double>> = Calc.e1rmSeries(name, workouts)

    /** Previous performance of an exercise: per set index, "82.5 × 8" in display units. Cached. */
    fun prevFor(name: String): List<String>? {
        prevCache[name]?.let { return it.ifEmpty { null } }
        var result: List<String>? = null
        for (w in workoutsDesc()) {
            val ex = w.exercises.firstOrNull { e -> e.name == name && e.sets.any { it.kg != null || it.reps != null } }
            if (ex != null) {
                result = ex.sets.map { s ->
                    if (s.kg == null && s.reps == null) "—"
                    else "${Calc.fmtKg(s.kg, settings.unit)}${Calc.unitLabel(settings.unit)} × ${s.reps ?: "—"}"
                }
                break
            }
        }
        prevCache[name] = result ?: emptyList()
        return result
    }

    fun routineLastPerformed(r: Routine): Long? =
        workoutsDesc().firstOrNull { w -> w.name == r.name && w.exercises.any { e -> r.exercises.any { it.name == e.name } } }?.startedAt
    
    /** Per-set "previous" strings for an exercise, computed strictly before the given timestamp. */
    fun prevSetsBefore(beforeMs: Long, name: String): List<String>? {
        for (w in workouts.sortedByDescending { it.startedAt }) {
            if (w.startedAt >= beforeMs) continue
            val ex = w.exercises.firstOrNull { e -> e.name == name && e.sets.any { it.kg != null || it.reps != null } } ?: continue
            return ex.sets.map { s ->
                if (s.kg == null && s.reps == null) "—"
                else "${Calc.fmtKg(s.kg, settings.unit)}${Calc.unitLabel(settings.unit)} × ${s.reps ?: "—"}"
            }
        }
        return null
    }
}

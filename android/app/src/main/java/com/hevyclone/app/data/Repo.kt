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
class Db(ctx: Context) : SQLiteOpenHelper(ctx, "hevy.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE workouts(id INTEGER PRIMARY KEY AUTOINCREMENT, startedAt INTEGER NOT NULL, endedAt INTEGER NOT NULL, name TEXT NOT NULL, json TEXT NOT NULL)")
        db.execSQL("CREATE TABLE routines(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, json TEXT NOT NULL)")
        db.execSQL("CREATE TABLE settings(k TEXT PRIMARY KEY, v TEXT NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldV: Int, newV: Int) {}
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
        db.readableDatabase.rawQuery("SELECT json FROM routines ORDER BY id ASC", null).use { c ->
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
            put("id", r.id); put("name", r.name); put("json", json.encodeToString(Routine.serializer(), r))
        }
        db.writableDatabase.insertWithOnConflict("routines", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun persistSettings() {
        val cv = ContentValues().apply { put("k", "settings"); put("v", json.encodeToString(Settings.serializer(), settings)) }
        db.writableDatabase.insertWithOnConflict("settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun nextWorkoutId(): Long = (workouts.maxOfOrNull { it.id } ?: 0L) + 1
    fun nextRoutineId(): Long = (routines.maxOfOrNull { it.id } ?: 0L) + 1

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
            Routine(nextRoutineId(), name, d.exercises).also { routines.add(it) }
        }
        persistRoutine(r)
        draft = null
        touch()
        return r
    }

    fun finishWorkout(name: String): Workout? {
        val d = draft ?: return null
        val now = System.currentTimeMillis()
        val w = Workout(
            id = nextWorkoutId(),
            name = name.trim().ifEmpty { "Séance" },
            startedAt = d.startedAt ?: now - 3600000,
            endedAt = now,
            exercises = d.exercises,
            notes = d.notes,
        )
        workouts.add(w)
        prCache = Calc.rebuildPrs(workouts)
        persistWorkout(w)
        draft = null
        touch()
        return w
    }

    fun discardDraft() { draft = null; touch() }

    fun deleteWorkout(id: Long) {
        workouts.removeAll { it.id == id }
        db.writableDatabase.delete("workouts", "id=?", arrayOf(id.toString()))
        prCache = Calc.rebuildPrs(workouts)
        touch()
    }

    fun deleteRoutine(id: Long) {
        routines.removeAll { it.id == id }
        db.writableDatabase.delete("routines", "id=?", arrayOf(id.toString()))
        touch()
    }

    // ---------- settings ----------

    fun setUnit(u: String) { settings.unit = u; persistSettings(); touch() }
    fun setRest(sec: Int) { settings.restSec = sec; persistSettings(); touch() }
    fun setAccent(a: String) { settings.accent = a; persistSettings(); touch() }
    fun setTheme(t: String) { settings.theme = t; persistSettings(); touch() }

    fun importCsv(content: String): Int {
        val imported = Calc.parseCsv(content)
        imported.forEach { w ->
            workouts.add(w)
            persistWorkout(w)
        }
        if (imported.isNotEmpty()) {
            prCache = Calc.rebuildPrs(workouts)
            touch()
        }
        return imported.size
    }

    fun wipe() {
        workouts.clear(); routines.clear(); draft = null
        db.writableDatabase.delete("workouts", null, null)
        db.writableDatabase.delete("routines", null, null)
        prCache = emptyMap()
        touch()
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
                    else "${Calc.fmtKg(s.kg, settings.unit)} × ${s.reps ?: "—"}"
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
                else "${Calc.fmtKg(s.kg, settings.unit)} × ${s.reps ?: "—"}"
            }
        }
        return null
    }
}

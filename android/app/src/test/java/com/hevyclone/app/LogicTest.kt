package com.hevyclone.app

import com.hevyclone.app.data.Calc
import com.hevyclone.app.data.ExEntry
import com.hevyclone.app.data.SetEntry
import com.hevyclone.app.data.Workout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {

    private fun w(id: Long, startedAt: Long, vararg exs: Pair<String, List<Triple<Double?, Int?, Boolean>>>): Workout =
        Workout(
            id = id, name = "W$id", startedAt = startedAt, endedAt = startedAt + 3600000,
            exercises = exs.map { (name, sets) ->
                ExEntry(name, "Quads", "", sets.map { SetEntry(it.first, it.second, it.third) }.toMutableList())
            }.toMutableList(),
        )

    @Test
    fun `e1rm Epley formula`() {
        assertEquals(103.33, Calc.e1rm(100.0, 1), 0.01)
        assertEquals(120.0, Calc.e1rm(100.0, 6), 0.01)
        assertEquals(80.0, Calc.e1rm(80.0, 0), 0.01)
    }

    @Test
    fun `round125 snaps to plate increments`() {
        assertEquals(72.5, Calc.round125(72.3), 1e-9)
        assertEquals(71.25, Calc.round125(70.8), 1e-9)
        assertEquals(100.0, Calc.round125(100.4), 1e-9)
    }

    @Test
    fun `volume reps sets ignore skipped sets`() {
        val workout = w(1, 0, "Barbell Squat" to listOf(
            Triple(100.0, 8, true),
            Triple(100.0, 8, false),
            Triple(null, 12, true),
        ))
        assertEquals(800.0, Calc.vol(workout), 1e-9)
        assertEquals(20, Calc.reps(workout))
        assertEquals(2, Calc.setsDone(workout))
    }

    @Test
    fun `unit conversion kg lb roundtrip`() {
        val kg = 82.5
        val display = Calc.fmtKg(kg, "lb")
        val back = Calc.toKg(display, "lb")!!
        assertEquals(kg, back, 0.05)
        assertEquals("82.5", Calc.fmtKg(kg, "kg"))
    }

    @Test
    fun `rebuildPrs stamps flags and per-workout pr list chronologically`() {
        val workouts = mutableListOf(
            w(1, 1000L, "Deadlift" to listOf(Triple(140.0, 5, true))),
            w(2, 2000L, "Deadlift" to listOf(Triple(140.0, 5, true), Triple(145.0, 5, true))),
            w(3, 3000L, "Deadlift" to listOf(Triple(130.0, 5, true))),
        )
        val cache = Calc.rebuildPrs(workouts)
        // session 1: everything is a PR (heaviest weight + best est. 1RM)
        assertTrue(workouts[0].exercises[0].sets[0].prW)
        assertEquals(2, workouts[0].prs.size)
        // session 2: 145 is the new weight PR, 140×5 is not
        assertFalse(workouts[1].exercises[0].sets[0].prW)
        assertTrue(workouts[1].exercises[0].sets[1].prW)
        assertEquals(2, workouts[1].prs.size) // weight + est 1RM
        // session 3: regression → no PRs
        assertTrue(workouts[2].prs.isEmpty())
        assertEquals(145.0, cache["Deadlift"]!!.weight, 1e-9)
        assertEquals(2000L, cache["Deadlift"]!!.weightDate)
    }

    @Test
    fun `streak counts consecutive days up to today`() {
        val now = System.currentTimeMillis()
        val today = Calc.dayKey(now)
        fun at(daysAgo: Int): Long = java.time.LocalDate.now().minusDays(daysAgo.toLong())
            .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() + 7200000
        val workouts = listOf(w(1, at(0)), w(2, at(1)), w(3, at(2)))
        assertEquals(3, Calc.streak(workouts, now))
        // a workout 2 days ago only (today has nothing yet) still counts from yesterday
        assertEquals(0, Calc.streak(emptyList(), now))
    }

    @Test
    fun `seed produces realistic demo dataset`() {
        val (workouts, routines) = Calc.seed(System.currentTimeMillis())
        assertTrue("expected many seeded workouts", workouts.size in 30..60)
        assertEquals(4, routines.size)
        // chronological order + progressive weights
        assertTrue(workouts.zipWithNext().all { (a, b) -> a.startedAt < b.startedAt })
        val benches = workouts.mapNotNull { w -> w.exercises.firstOrNull { it.name == "Barbell Bench Press" } }
        assertTrue(benches.size >= 8)
        assertTrue(
            "weights should progress over time",
            benches.last().sets.first().kg!! > benches.first().sets.first().kg!!,
        )
        // PR flags + per-workout pr lists get filled
        Calc.rebuildPrs(workouts)
        assertTrue(workouts.any { it.prs.isNotEmpty() })
        // every seeded exercise exists in the DB
        workouts.forEach { wk -> wk.exercises.forEach { assertNotNull(com.hevyclone.app.data.EX[it.name]) } }
    }

    @Test
    fun `weekStats and weekly series agree`() {
        val now = System.currentTimeMillis()
        val (workouts, _) = Calc.seed(now)
        val stats = Calc.weekStats(workouts, now)
        val series = Calc.weekly(workouts, 12, "kg", now)
        assertEquals(stats.vol, series.last().second, 1e-6)
        assertTrue(series.all { it.second >= 0 })
    }
    @Test
    fun `fuzzy search matches french names accent-insensitive`() {
        val m = com.hevyclone.app.data.L10nData::matches
        assertTrue(m("developpe", "Barbell Bench Press"))
        assertTrue(m("DEVELOPPE COUCHE", "Barbell Bench Press"))
        assertTrue(m("dev", "Barbell Bench Press"))
        assertTrue(m("couché haltères", "Dumbbell Bench Press"))
        assertTrue(m("elevation", "Lateral Raise"))
        assertTrue(m("tirage", "Lat Pulldown"))
        assertTrue(m("bench press", "Barbell Bench Press"))
        assertTrue(m("pec", "Barbell Bench Press"))          // via muscle "Pectoraux"
        assertTrue(m("squat", "Barbell Squat"))
        assertTrue(m("", "Barbell Squat"))
        assertFalse(m("squat", "Barbell Bench Press"))
    }
}

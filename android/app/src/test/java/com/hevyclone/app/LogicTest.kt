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
                ExEntry(name, "Quads", "", false, null, sets.map { SetEntry(it.first, it.second, it.third) }.toMutableList())
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
        // typo tolerance — user example: "developer coucher"
        assertTrue(m("developer coucher", "Barbell Bench Press"))
        assertTrue(m("developer couché", "Barbell Bench Press"))
        assertTrue(m("sqaut", "Barbell Squat"))          // transposed letters
        assertTrue(m("tirrage", "Lat Pulldown"))
        assertTrue(m("elevaion", "Lateral Raise"))
    }
    @Test
    fun `fuzzy search matches hevy french vocabulary`() {
        val m = com.hevyclone.app.data.L10nData::matches
        // the user's exact example: « tirage poitrine » → Chest Supported Row (renamed "Tirage Poitrine")
        assertTrue(m("tirage poitrine", "Chest Supported Row"))
        assertTrue(m("poitrine", "Chest Supported Row"))
        assertTrue(m("pec deck", "Machine Fly (Pec Deck)"))
        // muscle synonyms (gym vocabulary)
        assertTrue(m("dos", "Pull Up"))
        assertTrue(m("dos", "Deadlift"))
        assertTrue(m("abdos", "Crunch"))
        assertTrue(m("ventre", "Plank"))
        assertTrue(m("jambes", "Barbell Squat"))
        assertTrue(m("mollets", "Standing Calf Raise"))
        assertTrue(m("bras", "Barbell Curl"))
        // equipment FR
        assertTrue(m("poulie", "Cable Curl"))
        assertTrue(m("halteres", "Dumbbell Bench Press"))
        assertTrue(m("poids du corps", "Push Up"))
        // gym slang aliases
        assertTrue(m("dc", "Barbell Bench Press"))
        assertTrue(m("sdt", "Deadlift"))
        assertTrue(m("barre au front", "Skullcrusher"))
        // newly added exercises are searchable via their FR names
        assertTrue(m("fentes arriere", "Reverse Lunge"))
        assertTrue(m("kettlebell", "Kettlebell Swing"))
        assertTrue(m("dragon flag", "Dragon Flag"))
        assertTrue(m("tractions prise large", "Wide Grip Pull Up"))
        // Hevy FR exact names: « Développé Militaire Haltères » family
        assertTrue(m("developpe militaire halteres", "Seated Dumbbell Shoulder Press"))
        assertTrue(m("developpe militaire", "Standing Dumbbell Shoulder Press"))
        assertTrue(m("militaire machine", "Machine Shoulder Press"))
        // gym vocabulary with noise words + stemming: « rowing poulie assis prise en v »
        assertTrue(m("rowing poulie assis prise en v", "V-Bar Cable Row"))
        assertTrue(m("rowing assis poulie", "Seated Cable Row"))
        assertTrue(m("curl machine assis", "Preacher Curl"))
        // no typo-fuzzy on muscle words: « goblet » must not match « mollets » noise
        assertTrue(m("goblet", "Goblet Squat"))
        assertFalse(m("goblet", "Standing Calf Raise"))
        assertFalse(m("squat", "Standing Calf Raise"))
        // DB sanity: unique canonical names, FR name + instructions for every exercise
        val names = com.hevyclone.app.data.EXERCISES.map { it.name }
        assertEquals(names.size, names.toSet().size)
        assertTrue(names.size >= 600)
        names.forEach { n ->
            assertNotNull("missing FR name: $n", com.hevyclone.app.data.L10nData.NAME_FR[n])
            assertTrue("no instructions: $n", com.hevyclone.app.data.L10nData.steps(n).isNotEmpty())
        }
    }
    @Test
    fun `hevy real export format with localized dates parses`() {
        // Exact columns of Hevy's own export; dates "d MMM yyyy, HH:mm" with French month names,
        // quoted commas, and a multi-line quoted note.
        val csv = """
            title,start_time,end_time,description,exercise_title,superset_id,exercise_notes,set_index,set_type,weight_kg,reps,distance_km,duration_seconds,rpe
            Push A,"18 sept. 2026, 17:30","18 sept. 2026, 18:15","",Développé Couché (Barre),,,"1",normal,80,8,,,,"note
            sur deux lignes"
            Push A,"18 sept. 2026, 17:30","18 sept. 2026, 18:15","",Élévation Latérale (Haltères),,,"2",normal,10,15,,,,
            Pull A,"20 Sep 2026, 18:00","20 Sep 2026, 19:00","",Tirage Poitrine (Machine),1,,1,normal,60,10,,,,
            Pull A,"20 Sep 2026, 18:00","20 Sep 2026, 19:00","",Tirage Poitrine (Machine),1,,2,normal,62,10,,,,
        """.trimIndent()
        val (ws, rs) = Calc.parseHevyCsv(csv)
        assertEquals(0, rs.size)
        assertEquals(2, ws.size)
        assertEquals("Push A", ws[0].name)
        assertEquals(2, ws[0].exercises.size)
        assertEquals("Barbell Bench Press", ws[0].exercises[0].name)
        assertEquals(80.0, ws[0].exercises[0].sets[0].kg!!, 1e-9)
        assertEquals(8, ws[0].exercises[0].sets[0].reps)
        // localized month date actually parsed
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = ws[0].startedAt }
        assertEquals(2026, cal.get(java.util.Calendar.YEAR))
        assertEquals(java.util.Calendar.SEPTEMBER, cal.get(java.util.Calendar.MONTH))
        // second workout grouped by its own date + superset preserved
        assertEquals("Pull A", ws[1].name)
        assertEquals("Chest Supported Row", ws[1].exercises[0].name)
        assertEquals(2, ws[1].exercises[0].sets.size)
        assertTrue(ws[1].exercises[0].superset)
    }
    @Test
    fun `hevy strong export csv import maps workouts exercises and units`() {
        // Strong-style header as produced by Hevy's export (FR exercise names, lbs on one workout)
        val csv = """
            Date,Workout Name,Exercise Name,Set Order,Weight,Weight Unit,Reps,Seconds,Notes
            2026-09-18T17:30:00+02:00,Push A,Développé Couché (Haltères),1,100,lb,8,0,
            2026-09-18T17:30:00+02:00,Push A,Développé Couché (Haltères),2,100,lb,8,0,
            2026-09-18T17:30:00+02:00,Push A,Élévation Latérale (Haltères),1,10,kg,15,0,
            2026-09-20T18:00:00+02:00,Jambes,Squat Barre,1,120,kg,5,0,
        """.trimIndent()
        val (ws, rs) = Calc.parseHevyCsv(csv)
        assertEquals(2, ws.size)
        assertEquals("Push A", ws[0].name)
        assertEquals(2, ws[0].exercises.size)
        // FR name remapped to canonical EN key, lbs → kg
        assertEquals("Dumbbell Bench Press", ws[0].exercises[0].name)
        assertEquals(2, ws[0].exercises[0].sets.size)
        assertEquals(45.36, ws[0].exercises[0].sets[0].kg!!, 0.01)
        assertEquals(8, ws[0].exercises[0].sets[0].reps)
        assertEquals("Barbell Squat", ws[1].exercises[0].name)
        // ISO date with offset parsed
        assertTrue(ws[0].startedAt < ws[1].startedAt)

        // snake_case variant + a template file (no dates → routine)
        val snake = """
            start_time,end_time,exercise_name,set_index,weight_kg,reps,checked,superset_group_id
            2026-09-19 10:00:00+02:00,2026-09-19 11:00:00+02:00,Tirage Poitrine,1,60,10,true,1
            2026-09-19 10:00:00+02:00,2026-09-19 11:00:00+02:00,"Tirage Horizontal (Poulie, V-Bar)",1,50,12,true,1
        """.trimIndent()
        val (ws2, _) = Calc.parseHevyCsv(snake)
        assertEquals(1, ws2.size)
        assertEquals(2, ws2[0].exercises.size)
        assertEquals("Chest Supported Row", ws2[0].exercises[0].name)
        assertTrue(ws2[0].exercises[0].superset)
        assertEquals("V-Bar Cable Row", ws2[0].exercises[1].name)

        val templates = """
            exercise_name,set_index,weight_kg,reps,superset_group_id
            Développé Militaire Haltères Assis,1,20,10,
            Tractions,1,0,8,1
        """.trimIndent()
        val (_, routines) = Calc.parseHevyCsv(templates)
        assertEquals(1, routines.size)
        assertEquals("Seated Dumbbell Shoulder Press", routines[0].exercises[0].name)
    }
    @Test
    fun `routines rebuilt from imported workout names use latest occurrence`() {
        fun w(id: Long, at: Long, name: String, vararg exs: String) = Workout(
            id, name, at, at + 3_600_000,
            exs.map { ExEntry(it, "Quads", "", false, null, mutableListOf(SetEntry(50.0, 10, true))) }.toMutableList(),
        )
        val imported = listOf(
            w(1, 1000L, "Push A", "Barbell Bench Press"),
            w(2, 2000L, "Pull A", "Lat Pulldown"),
            w(3, 9000L, "Push A", "Incline Dumbbell Bench Press", "Lateral Raise"),  // latest Push A
            w(4, 3000L, "push a", "Dumbbell Curl"),  // same name, case-insensitive
        )
        val routines = Calc.routinesFromWorkouts(imported, existingRoutineNames = setOf("Pull A"))
        assertEquals(1, routines.size)  // "Pull A" already exists, "push a" merged with "Push A"
        assertEquals("Push A", routines[0].name)
        assertEquals(listOf("Incline Dumbbell Bench Press", "Lateral Raise"), routines[0].exercises.map { it.name })
    }
    @Test
    fun `csv import parses export format with multiple exercises`() {
        val csv = """
            Date;Heure;Exercice;Serie;KG;Reps
            5 janv. 2026;17:30;Développé Couché (Barre);1;80;8
            5 janv. 2026;17:30;Développé Couché (Barre);2;80;8
            5 janv. 2026;17:30;Squat Barre;1;100;5
            7 janv. 2026;18:00;Tirage Vertical (Machine);1;60;10
        """.trimIndent()
        val ws = Calc.parseCsv(csv)
        assertEquals(2, ws.size)
        assertEquals(2, ws[0].exercises.size)
        // French names remapped to canonical EN keys
        assertEquals("Barbell Bench Press", ws[0].exercises[0].name)
        assertEquals("Barbell Squat", ws[0].exercises[1].name)
        assertEquals(2, ws[0].exercises[0].sets.size)
        assertEquals(80.0, ws[0].exercises[0].sets[0].kg!!, 1e-9)
        assertEquals(8, ws[0].exercises[0].sets[0].reps)
        assertEquals("Lat Pulldown", ws[1].exercises[0].name)
        // comma separator accepted too
        val csv2 = "Date,Heure,Exercice,Serie,KG,Reps\n5 janv. 2026;17:30;Squat Barre;1;90;6"
        val ws2 = Calc.parseCsv(csv2)
        assertEquals(1, ws2.size)
        assertEquals(90.0, ws2[0].exercises[0].sets[0].kg!!, 1e-9)
    }
    @Test
    fun `backup roundtrip preserves workouts and routines`() {
        val (ws, rs) = Calc.seed(1_700_000_000_000L)
        val backup = com.hevyclone.app.data.BackupData(
            ws.map { w -> w.copy(exercises = w.exercises) },
            rs,
        )
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val text = json.encodeToString(com.hevyclone.app.data.BackupData.serializer(), backup)
        val back = json.decodeFromString(com.hevyclone.app.data.BackupData.serializer(), text)
        assertEquals(backup.workouts.size, back.workouts.size)
        assertEquals(backup.routines.size, back.routines.size)
        assertEquals(backup.workouts.first().exercises.first().sets.size,
                     back.workouts.first().exercises.first().sets.size)
    }
}

package com.hevyclone.app.data

import kotlinx.serialization.Serializable

@Serializable
data class SetEntry(
    var kg: Double? = null,
    var reps: Int? = null,
    var done: Boolean = true,
    var prW: Boolean = false,
    var prE: Boolean = false,
)

@Serializable
data class ExEntry(
    var name: String,
    var muscle: String,
    var notes: String = "",
    var superset: Boolean = false,
    var restSec: Int? = null,
    var sets: MutableList<SetEntry> = mutableListOf(),
)

@Serializable
data class PrRec(val ex: String, val kind: String, val value: Double)

@Serializable
data class Workout(
    val id: Long,
    var name: String,
    val startedAt: Long,
    var endedAt: Long,
    val exercises: MutableList<ExEntry> = mutableListOf(),
    var prs: MutableList<PrRec> = mutableListOf(),
    var notes: String = "",
)

@Serializable
data class Routine(
    val id: Long,
    var name: String,
    var exercises: MutableList<ExEntry> = mutableListOf(),
)

@Serializable
data class Settings(
    var unit: String = "kg",
    var restSec: Int = 90,
    var theme: String = "dark",
    var accent: String = "blue",
    var profileName: String = "Athlète",
    var handle: String = "athlete",
    var since: Long = 0,
)

data class PrBest(
    val weight: Double,
    val weightDate: Long,
    val e1rm: Double,
    val e1rmDate: Long,
)

/** In-progress workout or routine being edited */
@Serializable
data class Draft(
    val mode: String,           // "workout" | "routine"
    val routineId: Long? = null,
    var name: String,
    var startedAt: Long? = null,
    var notes: String = "",
    val exercises: MutableList<ExEntry> = mutableListOf(),
)

data class WeekStats(val count: Int, val vol: Double, val reps: Int, val prs: Int)

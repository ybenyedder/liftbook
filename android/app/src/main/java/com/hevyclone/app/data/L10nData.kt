package com.hevyclone.app.data

import java.util.Locale

/** Content localization: FR translations of exercises, muscles, equipment, cues. Falls back to EN. */
object L10nData {
    val lang: String get() = Locale.getDefault().language

    val MUSCLE_FR = mapOf(
        "Chest" to "Pectoraux", "Shoulders" to "Épaules", "Biceps" to "Biceps", "Triceps" to "Triceps",
        "Lats" to "Dorsaux", "Lower back" to "Lombaires", "Traps" to "Trapèzes", "Quads" to "Quadriceps",
        "Hamstrings" to "Ischio-jambiers", "Glutes" to "Fessiers", "Abductors" to "Abducteurs",
        "Adductors" to "Adducteurs", "Calves" to "Mollets", "Abs" to "Abdominaux", "Forearms" to "Avant-bras",
    )
    val EQUIP_FR = mapOf(
        "Barbell" to "Barre", "Dumbbell" to "Haltères", "Machine" to "Machine",
        "Cable" to "Poulie", "Bodyweight" to "Poids du corps", "Other" to "Autre",
    )
    val NAME_FR = mapOf(
        "Barbell Bench Press" to "Développé Couché (Barre)",
        "Incline Barbell Bench Press" to "Développé Couché Incliné (Barre)",
        "Decline Bench Press" to "Développé Décliné (Barre)",
        "Smith Machine Bench Press" to "Développé Couché (Smith)",
        "Dumbbell Bench Press" to "Développé Couché (Haltères)",
        "Incline Dumbbell Bench Press" to "Développé Couché Incliné (Haltères)",
        "Dumbbell Fly" to "Écarté (Haltères)",
        "Incline Dumbbell Fly" to "Écarté Incliné (Haltères)",
        "Machine Chest Press" to "Développé Assis (Machine)",
        "Machine Fly (Pec Deck)" to "Butterfly (Pec Deck)",
        "Cable Crossover" to "Écarté Croisé à la Poulie",
        "Cable Fly" to "Écarté à la Poulie",
        "Push Up" to "Pompes",
        "Weighted Push Up" to "Pompes Lestées",
        "Overhead Press" to "Développé Militaire (Barre)",
        "Seated Dumbbell Shoulder Press" to "Développé Épaules Assis (Haltères)",
        "Arnold Press" to "Presse Arnold",
        "Machine Shoulder Press" to "Développé Épaules (Machine)",
        "Smith Machine Shoulder Press" to "Développé Épaules (Smith)",
        "Lateral Raise" to "Élévation Latérale (Haltères)",
        "Cable Lateral Raise" to "Élévation Latérale (Poulie)",
        "Front Raise" to "Élévation Frontale (Haltères)",
        "Rear Delt Fly" to "Oiseau (Haltères)",
        "Reverse Pec Deck" to "Butterfly Inversé (Machine)",
        "Face Pull" to "Face Pull (Poulie)",
        "Upright Row" to "Rowing Menton (Barre)",
        "Barbell Curl" to "Curl Barre",
        "EZ Bar Curl" to "Curl Barre EZ",
        "Drag Curl" to "Curl Traîné (Barre)",
        "Dumbbell Curl" to "Curl Haltères",
        "Hammer Curl" to "Curl Marteau (Haltères)",
        "Incline Dumbbell Curl" to "Curl Incliné (Haltères)",
        "Concentration Curl" to "Curl Concentré",
        "Preacher Curl" to "Curl Larry Scott (Machine)",
        "Cable Curl" to "Curl à la Poulie",
        "Bayesian Cable Curl" to "Curl Bayesian à la Poulie",
        "Chin Up" to "Tractions Supination",
        "Tricep Pushdown" to "Extension Poulie Haute",
        "Overhead Cable Extension" to "Extension Nuque à la Poulie",
        "Overhead Dumbbell Extension" to "Extension Nuque (Haltère)",
        "Skullcrusher" to "Extension Couchée (Barre)",
        "JM Press" to "JM Press",
        "Close Grip Bench Press" to "Développé Serré (Barre)",
        "Smith Machine Close Grip Bench Press" to "Développé Serré (Smith)",
        "Dips" to "Dips (Barres Parallèles)",
        "Weighted Dip" to "Dips Lestés",
        "Bench Dip" to "Dips entre Deux Bancs",
        "Kickback" to "Kickback (Haltère)",
        "Pull Up" to "Tractions Pronation",
        "Weighted Pull Up" to "Tractions Lestées",
        "Lat Pulldown" to "Tirage Vertical (Machine)",
        "Straight Arm Pulldown" to "Tirage Bras Tendus (Poulie)",
        "Bent Over Row" to "Rowing Barre",
        "Pendlay Row" to "Rowing Pendlay (Barre)",
        "Dumbbell Row" to "Rowing Haltère",
        "Chest Supported Row" to "Rowing sur Banc (Machine)",
        "Seated Cable Row" to "Tirage Horizontal (Poulie)",
        "T-Bar Row" to "Rowing T-Bar",
        "Machine Row" to "Rowing Machine",
        "Inverted Row" to "Rowing Inversé (Table)",
        "Deadlift" to "Soulevé de Terre",
        "Sumo Deadlift" to "Soulevé de Terre Sumo",
        "Trap Bar Deadlift" to "Soulevé de Terre (Trap Bar)",
        "Romanian Deadlift" to "Soulevé de Terre Jambes Tendues",
        "Stiff Leg Deadlift" to "Soulevé de Terre Jambes Raides",
        "Good Morning" to "Good Morning (Barre)",
        "Back Extension" to "Extension du Dos",
        "Barbell Shrug" to "Shrugs (Barre)",
        "Dumbbell Shrug" to "Shrugs (Haltères)",
        "Smith Machine Shrug" to "Shrugs (Smith)",
        "Rack Pull" to "Rack Pull (Barre)",
        "Barbell Squat" to "Squat Barre",
        "Front Squat" to "Squat Frontal (Barre)",
        "Smith Machine Squat" to "Squat (Smith)",
        "Hack Squat" to "Hack Squat (Machine)",
        "Belt Squat" to "Belt Squat (Machine)",
        "Leg Press" to "Presse à Cuisses",
        "Bulgarian Split Squat" to "Squat Bulgare (Haltères)",
        "Goblet Squat" to "Goblet Squat",
        "Walking Lunge" to "Fentes Marchées (Haltères)",
        "Step Up" to "Step Up (Banc)",
        "Leg Extension" to "Extension Jambes (Machine)",
        "Sissy Squat" to "Sissy Squat",
        "Lying Leg Curl" to "Leg Curl Allongé (Machine)",
        "Seated Leg Curl" to "Leg Curl Assis (Machine)",
        "Nordic Curl" to "Nordic Curl",
        "Glute Ham Raise" to "Glute Ham Raise",
        "Single Leg Romanian Deadlift" to "Soulevé de Terre Roumain sur Une Jambe",
        "Barbell Hip Thrust" to "Hip Thrust (Barre)",
        "Smith Machine Hip Thrust" to "Hip Thrust (Smith)",
        "Glute Bridge" to "Pont Fessier",
        "Cable Kickback" to "Kickback à la Poulie",
        "Cable Pull Through" to "Pull Through à la Poulie",
        "Abduction Machine" to "Abducteurs (Machine)",
        "Cable Hip Abduction" to "Abducteurs à la Poulie",
        "Adduction Machine" to "Adducteurs (Machine)",
        "Copenhagen Plank" to "Gainage Copenhague",
        "Sumo Squat" to "Squat Sumo (Barre)",
        "Standing Calf Raise" to "Extension Mollets Debout (Machine)",
        "Seated Calf Raise" to "Extension Mollets Assis (Machine)",
        "Leg Press Calf Raise" to "Extension Mollets à la Presse",
        "Donkey Calf Raise" to "Extension Mollets Donkey",
        "Single Leg Calf Raise" to "Extension Mollets sur Une Jambe",
        "Plank" to "Gainage (Planche)",
        "Side Plank" to "Gainage Latéral",
        "Crunch" to "Crunch",
        "Sit Up" to "Relevé de Buste",
        "Bicycle Crunch" to "Crunch Bicyclette",
        "Russian Twist" to "Russian Twist",
        "Dead Bug" to "Dead Bug",
        "Leg Raise" to "Relevé de Jambes",
        "Hanging Leg Raise" to "Relevé de Jambes Suspendu",
        "Hanging Knee Raise" to "Relevé de Genoux Suspendu",
        "Cable Crunch" to "Crunch à la Poulie",
        "Ab Wheel Rollout" to "Rouleau Abdominal (Ab Wheel)",
        "Machine Crunch" to "Crunch Machine",
        "Wrist Curl" to "Curl de Poignets (Haltères)",
        "Reverse Wrist Curl" to "Curl de Poignets Inversé (Barre)",
        "Reverse Curl" to "Curl Inversé (Barre)",
        "Farmer's Carry" to "Farmer's Walk (Haltères)",
    )
    val CUES_FR = mapOf(
        "Chest" to listOf(
            "Rentre les omoplates et garde-les collées au banc.",
            "Descends la charge sous contrôle jusqu'à sentir l'étirement des pectoraux.",
            "Pousse vers le haut sans ouvrir les coudes au-delà de 75° environ.",
        ),
        "Shoulders" to listOf(
            "Garde les côtes basses — ne creuse pas le dos pour pousser.",
            "Sur les élévations, guide avec les coudes et arrête-toi à hauteur d'épaules.",
            "Contrôle la descente : le deltoïde travaille le plus à la phase négative.",
        ),
        "Biceps" to listOf(
            "Garde les coudes collés au corps.",
            "Ne balance pas le buste ; descends en 2–3 secondes.",
            "Serre fort en haut et étends complètement le bras en bas.",
        ),
        "Triceps" to listOf(
            "Coudes fixes — seuls les avant-bras bougent.",
            "Atteins l'extension complète à chaque répétition.",
            "Ne laisse pas les coudes s'écarter quand la fatigue arrive.",
        ),
        "Lats" to listOf(
            "Démarque en ramenant les coudes vers les hanches, pas en tirant avec les mains.",
            "Poitrine haute, ne se penche pas trop en arrière.",
            "Étire complètement en haut, marque une pause en bas.",
        ),
        "Lower back" to listOf(
            "Gaine les abdos et garde le dos neutre jusqu'au verrouillage.",
            "Pousse le sol et termine en poussant les hanches vers l'avant.",
            "Reprends ta respiration et ta posture entre les répétitions lourdes.",
        ),
        "Traps" to listOf(
            "Monte les épaules tout droit — ne les roule pas.",
            "Marque une seconde complète en haut.",
            "Descends les épaules sur toute l'amplitude en bas.",
        ),
        "Quads" to listOf(
            "Garde les genoux alignés sur les orteils.",
            "Descends sous contrôle au moins à la parallèle.",
            "Pousse avec le milieu du pied, pas les orteils.",
        ),
        "Hamstrings" to listOf(
            "Charnière des hanches, garde juste une légère flexion des genoux.",
            "Cherche l'étirement profond en bas avant de remonter.",
            "Garde la charge proche des jambes pendant tout le mouvement.",
        ),
        "Glutes" to listOf(
            "Verrouille les hanches complètement et serre fort en haut.",
            "Menton rentré, côtes basses.",
            "Pousse avec les talons pour garder la tension sur les fessiers.",
        ),
        "Abductors" to listOf(
            "Bouge lentement et garde le buste droit.",
            "Marque une brève pause contre la résistance au point le plus large.",
        ),
        "Adductors" to listOf(
            "Bouge lentement et sous contrôle.",
            "Marque une brève pause contre la résistance au point le plus serré.",
        ),
        "Calves" to listOf(
            "Marque une seconde complète en haut de chaque répétition.",
            "Descends lentement sur toute l'amplitude pour un étirement profond.",
            "Garde les genoux tendus pour cibler les mollets.",
        ),
        "Abs" to listOf(
            "Expire et gaine-toi en remontant.",
            "Bouge avec contrôle — pas d'élan.",
            "Garde le bas du dos collé au sol quand indiqué.",
        ),
        "Forearms" to listOf(
            "Seuls les poignets bougent ; les coudes restent fixes.",
            "Tempo lent et répétitions hautes.",
        ),
    )

    fun name(en: String): String = if (lang == "fr") NAME_FR[en] ?: en else en
    fun muscle(m: String): String = if (lang == "fr") MUSCLE_FR[m] ?: m else m
    fun equip(e: String): String = if (lang == "fr") EQUIP_FR[e] ?: e else e
    fun cues(m: String): List<String> = if (lang == "fr") CUES_FR[m] ?: CUES[m] ?: emptyList() else CUES[m] ?: emptyList()
    
    // ---------- fuzzy search (accent-insensitive, multi-token, FR + EN + muscle) ----------

    fun normalize(s: String): String =
        java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("'", " ")

    /** Levenshtein edit distance (typo tolerance). */
    fun lev(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            val tmp = prev; prev = cur; cur = tmp
        }
        return prev[b.length]
    }

    /** A query token matches if it is a substring of any target word, or within edit distance of a whole word (typos). */
    private fun tokenMatches(token: String, words: List<String>): Boolean {
        if (words.any { it.contains(token) }) return true
        val maxDist = if (token.length >= 5) 2 else if (token.length == 4) 1 else 0
        if (maxDist == 0) return false
        return words.any { w -> kotlin.math.abs(w.length - token.length) <= maxDist && lev(token, w) <= maxDist }
    }

    /** True if every word of the query appears (or nearly, typos allowed) in the exercise name FR/EN, muscle or equipment. */
    fun matches(q: String, nameEn: String): Boolean {
        val nq = normalize(q).trim()
        if (nq.isEmpty()) return true
        val def = EX[nameEn]
        val target = normalize(nameEn) + " " +
            normalize(NAME_FR[nameEn] ?: nameEn) + " " +
            normalize(def?.muscle ?: "") + " " +
            normalize(MUSCLE_FR[def?.muscle] ?: "") + " " +
            normalize(def?.equip ?: "")
        val words = target.split(" ").filter { it.isNotBlank() }
        return nq.split(" ").all { it.isBlank() || tokenMatches(it.trim(), words) }
    }

    fun equipHint(e: String): String = when {
        lang != "fr" -> EQUIP_HINT[e] ?: ""
        else -> when (e) {
            "Barbell" -> "Avec une barre chargée de disques."
            "Dumbbell" -> "Avec des haltères — un dans chaque main sauf indication."
            "Machine" -> "Sur une machine à pile ou à charges guidées."
            "Cable" -> "À la station de poulies avec accessoires."
            "Bodyweight" -> "Sans matériel — le corps sert de résistance."
            else -> "Avec du matériel spécifique : banc, élastique ou roue."
        }
    }

}

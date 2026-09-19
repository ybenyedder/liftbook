package com.hevyclone.app.data

import java.util.Locale

/** Content localization: FR translations of exercises, muscles, equipment, cues, instructions. Falls back to EN. */
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

    /** Extra FR search words per muscle (gym vocabulary: « dos », « abdos », « jambes », « bras »…). */
    val MUSCLE_EXTRA = mapOf(
        "Chest" to "poitrine pec pectoraux bench",
        "Shoulders" to "epaules deltoides militaire",
        "Biceps" to "bras",
        "Triceps" to "bras",
        "Lats" to "dos dorsaux large",
        "Lower back" to "dos lombaires bas du dos chaine posterieure",
        "Traps" to "dos trapezes",
        "Quads" to "jambes cuisses quadris",
        "Hamstrings" to "jambes ischios ischio",
        "Glutes" to "jambes fessiers fessier",
        "Abductors" to "abducteurs moyen fessier",
        "Adductors" to "adducteurs",
        "Calves" to "jambes mollets mollet",
        "Abs" to "abdos abdo ventre abdominaux taille core",
        "Forearms" to "bras avant bras poignets avantbras",
    )

    /** Per-exercise FR aliases / gym slang, searched in addition to the translated name. */
    val ALIAS_FR: Map<String, String> = mapOf(
        "Barbell Bench Press" to "dc dev couche",
        "Overhead Press" to "dm developpe militaire",
        "Deadlift" to "sdt",
        "Sumo Deadlift" to "sdt sumo",
        "Romanian Deadlift" to "sdt roumain rdl",
        "Trap Bar Deadlift" to "hex deadlift",
        "Chest Supported Row" to "tirage poitrine",
        "Lat Pulldown" to "tirage dorsal",
        "Pull Up" to "traction tractions",
        "Skullcrusher" to "barre au front frontal",
        "Machine Fly (Pec Deck)" to "butterfly pec deck",
        "Farmer's Carry" to "marche du fermier farmers walk",
        "Back Extension" to "hyperextension banc romain",
        "Glute Ham Raise" to "ghd",
        "Ab Wheel Rollout" to "rouleau abwheel",
        "Kettlebell Swing" to "kb swing",
        "Barbell Hip Thrust" to "thrust fessiers",
        "Bulgarian Split Squat" to "bulgare",
        "Nordic Curl" to "nordic",
        "Dragon Flag" to "dragon",
        "Butterfly Sit Up" to "papillon",
        "Face Pull" to "facepull",
        "Machine Row" to "row assis",
        "Smith Machine Bench Press" to "dev couche smith",
        "Seated Cable Row" to "tirage horizontal poulie",
    )

    val NAME_FR = mapOf(
        // ---- Chest ----
        "Barbell Bench Press" to "Développé Couché (Barre)",
        "Incline Barbell Bench Press" to "Développé Couché Incliné (Barre)",
        "Decline Bench Press" to "Développé Décliné (Barre)",
        "Wide Grip Bench Press" to "Développé Couché Prise Large (Barre)",
        "Reverse Grip Bench Press" to "Développé Couché Prise Inversée (Barre)",
        "Guillotine Press" to "Presse Guillotine (Barre)",
        "Smith Machine Bench Press" to "Développé Couché (Smith)",
        "Incline Smith Machine Bench Press" to "Développé Couché Incliné (Smith)",
        "Dumbbell Bench Press" to "Développé Couché (Haltères)",
        "Incline Dumbbell Bench Press" to "Développé Couché Incliné (Haltères)",
        "Decline Dumbbell Press" to "Développé Décliné (Haltères)",
        "Single-Arm Dumbbell Press" to "Développé Unilatéral (Haltère)",
        "Dumbbell Floor Press" to "Développé au Sol (Haltères)",
        "Svend Press" to "Svend Press (Disques)",
        "Dumbbell Fly" to "Écarté (Haltères)",
        "Incline Dumbbell Fly" to "Écarté Incliné (Haltères)",
        "Machine Chest Press" to "Développé Assis (Machine)",
        "Machine Incline Press" to "Développé Incliné (Machine)",
        "Machine Fly (Pec Deck)" to "Butterfly (Pec Deck)",
        "Cable Crossover" to "Écarté Croisé à la Poulie",
        "Cable Fly" to "Écarté à la Poulie",
        "Cable Incline Fly" to "Écarté Incliné à la Poulie",
        "Low To High Cable Fly" to "Écarté Bas vers le Haut (Poulie)",
        "High To Low Cable Fly" to "Écarté Haut vers le Bas (Poulie)",
        "Single-Arm Cable Fly" to "Écarté Unilatéral (Poulie)",
        "Push Up" to "Pompes",
        "Weighted Push Up" to "Pompes Lestées",
        "Incline Push Up" to "Pompes Inclinées",
        "Decline Push Up" to "Pompes Déclinées",
        "Diamond Push Up" to "Pompes Diamant",
        "Plyometric Push Up" to "Pompes Plyométriques",
        "Chest Dip" to "Dips Pectoraux",
        // ---- Shoulders ----
        "Overhead Press" to "Développé Militaire (Barre)",
        "Seated Overhead Press" to "Développé Militaire Assis (Barre)",
        "Push Press" to "Push Press (Barre)",
        "Z Press" to "Z Press (Barre)",
        "Seated Dumbbell Shoulder Press" to "Développé Épaules Assis (Haltères)",
        "Single-Arm Dumbbell Shoulder Press" to "Développé Épaules Unilatéral (Haltère)",
        "Arnold Press" to "Presse Arnold",
        "Seated Arnold Press" to "Presse Arnold Assise",
        "Machine Shoulder Press" to "Développé Épaules (Machine)",
        "Smith Machine Shoulder Press" to "Développé Épaules (Smith)",
        "Landmine Press" to "Développé Landmine",
        "Lateral Raise" to "Élévation Latérale (Haltères)",
        "Single-Arm Lateral Raise" to "Élévation Latérale Unilatérale (Haltère)",
        "Cable Lateral Raise" to "Élévation Latérale (Poulie)",
        "Single-Arm Cable Lateral Raise" to "Élévation Latérale Unilatérale (Poulie)",
        "Leaning Cable Lateral Raise" to "Élévation Latérale en Appui (Poulie)",
        "Machine Lateral Raise" to "Élévation Latérale (Machine)",
        "Front Raise" to "Élévation Frontale (Haltères)",
        "Barbell Front Raise" to "Élévation Frontale (Barre)",
        "Cable Front Raise" to "Élévation Frontale (Poulie)",
        "Rear Delt Fly" to "Oiseau (Haltères)",
        "Incline Rear Delt Fly" to "Oiseau Incliné (Haltères)",
        "Cable Rear Delt Fly" to "Oiseau (Poulie)",
        "Dumbbell Rear Delt Row" to "Rowing Deltoïde Postérieur (Haltère)",
        "Reverse Pec Deck" to "Butterfly Inversé (Machine)",
        "Face Pull" to "Face Pull (Poulie)",
        "Upright Row" to "Rowing Menton (Barre)",
        "Wide Grip Upright Row" to "Rowing Menton Prise Large (Barre)",
        // ---- Biceps ----
        "Barbell Curl" to "Curl Barre",
        "EZ Bar Curl" to "Curl Barre EZ",
        "Drag Curl" to "Curl Traîné (Barre)",
        "Dumbbell Curl" to "Curl Haltères",
        "Seated Dumbbell Curl" to "Curl Assis (Haltères)",
        "Incline Dumbbell Curl" to "Curl Incliné (Haltères)",
        "Lying Dumbbell Curl" to "Curl Allongé (Haltères)",
        "Hammer Curl" to "Curl Marteau (Haltères)",
        "Incline Hammer Curl" to "Curl Marteau Incliné (Haltères)",
        "Cross Body Hammer Curl" to "Curl Marteau Croisé (Haltère)",
        "Pinwheel Curl" to "Curl Oblique (Haltère)",
        "Zottman Curl" to "Curl Zottman (Haltères)",
        "Spider Curl" to "Spider Curl (Haltères)",
        "Concentration Curl" to "Curl Concentré",
        "Preacher Curl" to "Curl Larry Scott (Machine)",
        "EZ Preacher Curl" to "Curl Larry Scott (Barre EZ)",
        "Cable Curl" to "Curl à la Poulie",
        "Single-Arm Cable Curl" to "Curl Unilatéral (Poulie)",
        "Rope Hammer Curl" to "Curl Marteau (Poulie, Corde)",
        "Bayesian Cable Curl" to "Curl Bayesian à la Poulie",
        "Chin Up" to "Tractions Supination",
        "Weighted Chin Up" to "Tractions Supination Lestées",
        // ---- Triceps ----
        "Tricep Pushdown" to "Extension Poulie Haute",
        "Rope Pushdown" to "Extension Poulie Haute (Corde)",
        "Straight Bar Pushdown" to "Extension Poulie Haute (Barre Droite)",
        "V-Bar Pushdown" to "Extension Poulie Haute (V-Bar)",
        "Single-Arm Pushdown" to "Extension Poulie Haute Unilatérale",
        "Overhead Cable Extension" to "Extension Nuque à la Poulie",
        "Overhead Rope Extension" to "Extension Nuque à la Poulie (Corde)",
        "Overhead Dumbbell Extension" to "Extension Nuque (Haltères)",
        "Single-Arm Overhead Dumbbell Extension" to "Extension Nuque Unilatérale (Haltère)",
        "Overhead Barbell Extension" to "Extension Nuque à la Barre",
        "Skullcrusher" to "Extension Couchée (Barre)",
        "Incline Skullcrusher" to "Extension Inclinée (Barre)",
        "Dumbbell Skullcrusher" to "Extension Couchée (Haltères)",
        "Tate Press" to "Tate Press (Haltères)",
        "JM Press" to "JM Press",
        "Close Grip Bench Press" to "Développé Serré (Barre)",
        "Smith Machine Close Grip Bench Press" to "Développé Serré (Smith)",
        "Dips" to "Dips (Barres Parallèles)",
        "Weighted Dip" to "Dips Lestés",
        "Bench Dip" to "Dips entre Deux Bancs",
        "Close Grip Push Up" to "Pompes Serrées",
        "Kickback" to "Kickback (Haltère)",
        "Cable Kickback Triceps" to "Kickback (Poulie)",
        // ---- Lats ----
        "Pull Up" to "Tractions Pronation",
        "Weighted Pull Up" to "Tractions Lestées",
        "Wide Grip Pull Up" to "Tractions Prise Large",
        "Close Grip Pull Up" to "Tractions Prise Serrée",
        "Neutral Grip Pull Up" to "Tractions Prise Neutre",
        "Assisted Pull Up" to "Tractions Assistées (Machine)",
        "Lat Pulldown" to "Tirage Vertical (Machine)",
        "Wide Grip Lat Pulldown" to "Tirage Vertical Prise Large",
        "Close Grip Lat Pulldown" to "Tirage Vertical Prise Serrée",
        "Reverse Grip Pulldown" to "Tirage Vertical Prise Supination",
        "Neutral Grip Lat Pulldown" to "Tirage Vertical Prise Neutre",
        "Single-Arm Lat Pulldown" to "Tirage Vertical Unilatéral",
        "Behind The Neck Pulldown" to "Tirage à la Nuque (Machine)",
        "Straight Arm Pulldown" to "Tirage Bras Tendus (Poulie)",
        "Bent Over Row" to "Rowing Barre",
        "Pendlay Row" to "Rowing Pendlay (Barre)",
        "Yates Row" to "Rowing Yates (Barre)",
        "Underhand Barbell Row" to "Rowing Supination (Barre)",
        "Seal Row" to "Rowing Seal (Barre)",
        "Meadow Row" to "Rowing Landmine",
        "Dumbbell Row" to "Rowing Haltère",
        "Incline Dumbbell Row" to "Rowing Incliné (Haltères)",
        "Renegade Row" to "Rowing Renegade (Haltères)",
        "Chest Supported Row" to "Tirage Poitrine (Machine)",
        "Seated Cable Row" to "Tirage Horizontal (Poulie)",
        "Single-Arm Cable Row" to "Tirage Horizontal Unilatéral (Poulie)",
        "Wide Grip Cable Row" to "Tirage Horizontal Prise Large (Poulie)",
        "V-Bar Cable Row" to "Tirage Horizontal (Poulie, V-Bar)",
        "T-Bar Row" to "Rowing T-Bar",
        "Machine Row" to "Rowing Assis (Machine)",
        "Inverted Row" to "Rowing Inversé (Table)",
        "Dumbbell Pullover" to "Pull-Over (Haltère)",
        "Cable Pullover" to "Pull-Over (Poulie)",
        "Machine Pullover" to "Pull-Over (Machine)",
        // ---- Lower back ----
        "Deadlift" to "Soulevé de Terre",
        "Sumo Deadlift" to "Soulevé de Terre Sumo",
        "Trap Bar Deadlift" to "Soulevé de Terre (Trap Bar)",
        "Deficit Deadlift" to "Soulevé de Terre sur Déficits",
        "Paused Deadlift" to "Soulevé de Terre avec Pause",
        "Snatch Grip Deadlift" to "Soulevé de Terre Prise Snatch",
        "Romanian Deadlift" to "Soulevé de Terre Roumain",
        "Dumbbell Romanian Deadlift" to "Soulevé de Terre Roumain (Haltères)",
        "Stiff Leg Deadlift" to "Soulevé de Terre Jambes Tendues",
        "Good Morning" to "Good Morning (Barre)",
        "Seated Good Morning" to "Good Morning Assis (Barre)",
        "Back Extension" to "Extension du Dos",
        "45 Degree Back Extension" to "Extension à 45° (Banc Romain)",
        "Weighted Back Extension" to "Extension du Dos Lestée",
        "Reverse Hyperextension" to "Reverse Hyperextension (Machine)",
        // ---- Traps ----
        "Barbell Shrug" to "Shrugs (Barre)",
        "Dumbbell Shrug" to "Shrugs (Haltères)",
        "Smith Machine Shrug" to "Shrugs (Smith)",
        "Cable Shrug" to "Shrugs (Poulie)",
        "Behind The Back Shrug" to "Shrugs dans le Dos (Barre)",
        "Overhead Barbell Shrug" to "Shrugs Bras Levés (Barre)",
        "Trap Bar Shrug" to "Shrugs (Trap Bar)",
        "Rack Pull" to "Rack Pull (Barre)",
        // ---- Quads ----
        "Barbell Squat" to "Squat Barre",
        "Pause Squat" to "Squat avec Pause (Barre)",
        "Box Squat" to "Box Squat (Barre)",
        "Front Squat" to "Squat Frontal (Barre)",
        "Zercher Squat" to "Zercher Squat (Barre)",
        "Overhead Squat" to "Squat Overhead (Barre)",
        "Smith Machine Squat" to "Squat (Smith)",
        "Hack Squat" to "Hack Squat (Machine)",
        "Reverse Hack Squat" to "Hack Squat Inversé (Machine)",
        "Belt Squat" to "Belt Squat (Machine)",
        "Dumbbell Squat" to "Squat (Haltères)",
        "Goblet Squat" to "Goblet Squat",
        "Landmine Squat" to "Squat Landmine",
        "Leg Press" to "Presse à Cuisses",
        "Single-Leg Leg Press" to "Presse à Cuisses Unilatérale",
        "Bulgarian Split Squat" to "Squat Bulgare (Haltères)",
        "Split Squat" to "Fentes Statiques (Poids du Corps)",
        "Walking Lunge" to "Fentes Marchées (Haltères)",
        "Reverse Lunge" to "Fentes Arrière (Haltères)",
        "Barbell Lunge" to "Fentes (Barre)",
        "Step Up" to "Step Up (Banc)",
        "Leg Extension" to "Extension Jambes (Machine)",
        "Sissy Squat" to "Sissy Squat",
        "Air Squat" to "Squat au Poids du Corps",
        "Jump Squat" to "Squat Sauté",
        "Pistol Squat" to "Squat au Pistolet",
        "Wall Sit" to "Chaise Murale",
        // ---- Hamstrings ----
        "Lying Leg Curl" to "Leg Curl Allongé (Machine)",
        "Single-Leg Lying Curl" to "Leg Curl Allongé Unilatéral",
        "Seated Leg Curl" to "Leg Curl Assis (Machine)",
        "Standing Leg Curl" to "Leg Curl Debout (Machine)",
        "Sliding Leg Curl" to "Leg Curl Glissant",
        "Stability Ball Leg Curl" to "Leg Curl sur Swiss Ball",
        "Nordic Curl" to "Nordic Curl",
        "Glute Ham Raise" to "Glute Ham Raise",
        "Kettlebell Swing" to "Swing Kettlebell",
        "Single Leg Romanian Deadlift" to "Soulevé de Terre Roumain sur Une Jambe",
        // ---- Glutes ----
        "Barbell Hip Thrust" to "Hip Thrust (Barre)",
        "Smith Machine Hip Thrust" to "Hip Thrust (Smith)",
        "Glute Bridge" to "Pont Fessier",
        "Barbell Glute Bridge" to "Pont Fessier (Barre)",
        "Single-Leg Glute Bridge" to "Pont Fessier sur Une Jambe",
        "Frog Pump" to "Pont Grenouille",
        "Cable Kickback" to "Kickback Fessiers (Poulie)",
        "Machine Glute Kickback" to "Kickback Fessiers (Machine)",
        "Cable Pull Through" to "Pull Through (Poulie)",
        "Curtsy Lunge" to "Fentes Révérence (Haltères)",
        // ---- Abductors / Adductors ----
        "Abduction Machine" to "Abducteurs (Machine)",
        "Cable Hip Abduction" to "Abducteurs (Poulie)",
        "Side Lying Hip Abduction" to "Abducteurs Couché de Côté",
        "Adduction Machine" to "Adducteurs (Machine)",
        "Standing Cable Adduction" to "Adducteurs (Poulie, Debout)",
        "Copenhagen Plank" to "Gainage Copenhague",
        "Side Lunge" to "Fente Latérale (Haltères)",
        "Sumo Squat" to "Squat Sumo (Barre)",
        // ---- Calves ----
        "Standing Calf Raise" to "Extension Mollets Debout (Machine)",
        "Smith Machine Calf Raise" to "Extension Mollets (Smith)",
        "Seated Calf Raise" to "Extension Mollets Assis (Machine)",
        "Leg Press Calf Raise" to "Extension Mollets à la Presse",
        "Donkey Calf Raise" to "Extension Mollets Donkey",
        "Single Leg Calf Raise" to "Extension Mollets sur Une Jambe",
        "Tibialis Raise" to "Extension Tibiale (Poids du Corps)",
        // ---- Abs ----
        "Plank" to "Gainage (Planche)",
        "Side Plank" to "Gainage Latéral",
        "Plank Shoulder Tap" to "Gainage avec Tape Épaule",
        "Hollow Hold" to "Gainage Creux (Hollow)",
        "Crunch" to "Crunch",
        "Weighted Crunch" to "Crunch Lesté (Disque)",
        "Machine Crunch" to "Crunch Machine",
        "Cable Crunch" to "Crunch à la Poulie",
        "Sit Up" to "Relevé de Buste",
        "Weighted Sit Up" to "Relevé de Buste Lesté",
        "Decline Sit Up" to "Relevé de Buste Décliné",
        "Butterfly Sit Up" to "Relevé de Buste Papillon",
        "Jackknife Sit Up" to "Relevé de Buste Cannonière",
        "Reverse Crunch" to "Crunch Inversé",
        "V-Up" to "V-Ups",
        "Bicycle Crunch" to "Crunch Bicyclette",
        "Russian Twist" to "Russian Twist",
        "Windshield Wipers" to "Essuie-Glace",
        "Dead Bug" to "Dead Bug",
        "Flutter Kicks" to "Battements de Jambes",
        "Scissor Kicks" to "Ciseaux (Jambes)",
        "Mountain Climbers" to "Grimpeur (Mountain Climbers)",
        "Leg Raise" to "Relevé de Jambes",
        "Hanging Leg Raise" to "Relevé de Jambes Suspendu",
        "Hanging Knee Raise" to "Relevé de Genoux Suspendu",
        "Dragon Flag" to "Dragon Flag",
        "Ab Wheel Rollout" to "Rouleau Abdominal (Ab Wheel)",
        "Dumbbell Side Bend" to "Inclinaison Latérale (Haltère)",
        // ---- Forearms ----
        "Wrist Curl" to "Curl de Poignets (Haltères)",
        "Reverse Wrist Curl" to "Curl de Poignets Inversé (Barre)",
        "Behind The Back Wrist Curl" to "Curl de Poignets dans le Dos (Barre)",
        "Reverse Curl" to "Curl Inversé (Barre)",
        "Farmer's Carry" to "Farmer's Walk (Haltères)",
        "Single-Arm Farmer's Carry" to "Farmer's Walk Unilatéral (Haltère)",
        "Dead Hang" to "Suspension Statique (Barre Fixe)",
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
            "Démarre en ramenant les coudes vers les hanches, pas en tirant avec les mains.",
            "Poitrine haute, ne te penche pas trop en arrière.",
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

    // ---------- instructions (FR) ----------

    val ARCH_STEPS_FR = mapOf(
        "bench" to listOf(
            "Allonge-toi sur le banc, yeux sous la barre, pieds bien ancrés au sol.",
            "Prends la barre un peu plus large que les épaules et décroche-la.",
            "Descends la barre au milieu des pectoraux, coudes à environ 75°.",
            "Pousse vers le haut et légèrement en arrière jusqu'à l'extension complète.",
        ),
        "incline_bench" to listOf(
            "Incline le banc à 30–45° et cale les omoplates en arrière.",
            "Décroche la charge au-dessus du haut des pectoraux.",
            "Descends vers la ligne des clavicules, coudes rentrés.",
            "Pousse en revenant à la position de départ.",
        ),
        "decline_bench" to listOf(
            "Cale tes jambes au bout du banc décliné et allonge-toi.",
            "Décroche la barre avec une prise largeur d'épaules.",
            "Descends la barre vers le bas des pectoraux, coudes rentrés.",
            "Pousse puissamment jusqu'à l'extension.",
        ),
        "fly" to listOf(
            "Mets-toi en place avec une légère flexion des coudes, gardée fixe toute la série.",
            "Ouvre les bras en grand arc jusqu'à sentir l'étirement des pectoraux.",
            "Referme les bras comme si tu enlaçais un tonneau.",
            "Marque une pause à la contraction maximale avant la répétition suivante.",
        ),
        "pushup" to listOf(
            "Place les mains un peu plus large que les épaules, corps aligné.",
            "Gaine les abdos et serre les fessiers.",
            "Descends jusqu'à ce que la poitrine frôle le sol, coudes à environ 45°.",
            "Pousse le sol loin de toi jusqu'à l'extension complète.",
        ),
        "ohp" to listOf(
            "Debout, pieds largeur d'épaules, barre posée sur les deltoïdes avant.",
            "Gaine le gainage et serre les fessiers pour bloquer le bassin.",
            "Pousse la barre verticalement en dégageant la tête en arrière.",
            "Verrouille au-dessus de la tête, barre au-dessus du milieu du pied, puis redescends sous contrôle.",
        ),
        "lateral_raise" to listOf(
            "Debout bien droit, poids le long du corps, coudes légèrement fléchis.",
            "Guide avec les coudes et monte les bras sur les côtés.",
            "Arrête-toi à hauteur d'épaules — pas plus haut.",
            "Descends lentement sur 2–3 secondes.",
        ),
        "front_raise" to listOf(
            "Debout, poids devant les cuisses.",
            "Monte les bras tendus devant toi jusqu'à hauteur d'épaules.",
            "Buste immobile — aucun balancement.",
            "Redescends sous contrôle à la position de départ.",
        ),
        "rear_delt" to listOf(
            "Penche-toi en avant, dos plat, ou assis face à la machine.",
            "Avec une légère flexion des coudes, ouvre les bras sur les côtés.",
            "Serre les omoplates au point le plus large.",
            "Reviens lentement en gardant la tension sur l'arrière d'épaules.",
        ),
        "shrug" to listOf(
            "Debout, bras tendus, charges en main.",
            "Monte les épaules tout droit vers les oreilles.",
            "Marque une seconde complète en haut.",
            "Redescends lentement sur toute l'amplitude.",
        ),
        "upright_row" to listOf(
            "Tiens la barre au niveau des hanches, prise largeur d'épaules.",
            "Tire la barre le long du corps, coudes directeurs.",
            "Arrête quand les coudes atteignent la hauteur des épaules.",
            "Redescends lentement à la position de départ.",
        ),
        "curl" to listOf(
            "Debout ou assis bien droit, coudes collés au corps.",
            "Curl la charge sans balancer le buste.",
            "Serre fort en haut de chaque répétition.",
            "Descends sur 2–3 secondes jusqu'à l'extension complète.",
        ),
        "triceps_pushdown" to listOf(
            "Face à la poulie, coudes collés au corps.",
            "Pousse la poignée vers le bas en extension des coudes uniquement.",
            "Atteins le verrouillage complet et contracte les triceps.",
            "Remonte lentement jusqu'à ce que les avant-bras touchent les biceps.",
        ),
        "triceps_overhead" to listOf(
            "Tiens la charge au-dessus de la tête à deux mains, coudes vers l'avant.",
            "Descends la charge derrière la tête en ne pliant que les coudes.",
            "Sens l'étirement profond des triceps en bas.",
            "Étends à nouveau jusqu'au verrouillage complet.",
        ),
        "skullcrusher" to listOf(
            "Allongé sur le banc, charge au-dessus de la poitrine.",
            "Plie les coudes pour descendre la charge vers le front.",
            "Garde les bras supérieurs verticaux et immobiles.",
            "Étends les coudes pour remonter.",
        ),
        "dip" to listOf(
            "Place-toi en appui sur les barres parallèles, bras verrouillés.",
            "Descends en pliant les coudes, buste légèrement incliné.",
            "Descends jusqu'à ce que les épaules arrivent à hauteur des coudes.",
            "Pousse jusqu'à l'extension complète.",
        ),
        "pulldown" to listOf(
            "Assis, cuisses bloquées sous les pads, poitrine haute.",
            "Prends la barre plus large que les épaules.",
            "Tire la barre vers le haut des pectoraux en guidant les coudes vers le bas.",
            "Laisse la barre remonter complètement pour un étirement total.",
        ),
        "pull_up" to listOf(
            "Suspension à la barre avec la prise choisie.",
            "Démarre le mouvement en ramenant les coudes vers les hanches.",
            "Tire jusqu'à ce que le menton passe la barre, poitrine vers la barre.",
            "Redescends en contrôle jusqu'à la suspension complète à chaque répétition.",
        ),
        "row_bent" to listOf(
            "Penché à environ 45°, dos plat, charge suspendue.",
            "Tire la charge vers le bas des pectoraux ou le nombril.",
            "Serre les omoplates en haut.",
            "Redescends sous contrôle sans bouger les hanches.",
        ),
        "row_supported" to listOf(
            "Installe-toi poitrine calée sur le dossier ou le banc.",
            "Tire les poignées vers le bas des pectoraux, coudes près du corps.",
            "Marque une pause et serre à la contraction maximale.",
            "Reviens lentement jusqu'à l'étirement complet.",
        ),
        "straight_arm" to listOf(
            "Debout face à la poulie, ou allongé avec l'haltère au-dessus de la poitrine.",
            "Garde les bras quasi tendus tout au long du mouvement.",
            "Tire ou descends la charge en arc en utilisant les dorsaux.",
            "Reviens lentement à la position étirée.",
        ),
        "deadlift" to listOf(
            "Place la barre au milieu du pied, hanches en arrière, dos plat.",
            "Prends une grande inspiration et gaine fort.",
            "Pousse le sol en faisant glisser la barre le long des tibias.",
            "Termine grand en poussant les hanches vers l'avant, puis inverse avec contrôle.",
        ),
        "rdl" to listOf(
            "Debout, charge tenue contre les cuisses.",
            "Pousse les hanches en arrière en laissant la charge descendre le long des jambes.",
            "Descends jusqu'à un étirement profond des ischio-jambiers, dos plat.",
            "Pousse les hanches vers l'avant pour te redresser.",
        ),
        "good_morning" to listOf(
            "Place la barre sur les trapèzes comme au squat.",
            "Genoux légèrement fléchis, charnière des hanches.",
            "Descends le buste jusqu'à presque la parallèle au sol.",
            "Pousse les hanches vers l'avant pour revenir droit.",
        ),
        "back_ext" to listOf(
            "Positionne-toi sur le banc, coussinet au pli de la hanche.",
            "Bras croisés sur la poitrine ou charge tenue.",
            "Descends en enroulant le haut du dos, puis relève le buste.",
            "Serre les fessiers et arrête-toi quand le corps forme une ligne droite.",
        ),
        "squat" to listOf(
            "Place la barre confortablement sur les trapèzes ou la charge à la poitrine.",
            "Décroche, recule de deux pas, pieds largeur d'épaules.",
            "Descends entre les hanches au moins à la parallèle, genoux dans l'axe des orteils.",
            "Pousse avec le milieu du pied pour te redresser complètement.",
        ),
        "lunge" to listOf(
            "Debout, haltères le long du corps.",
            "Fais un grand pas en avant ou en arrière.",
            "Descends jusqu'à ce que les deux genoux atteignent environ 90°.",
            "Pousse avec le pied avant pour revenir et alterne les jambes.",
        ),
        "leg_press" to listOf(
            "Assis, dos et hanches plaqués au dossier, pieds largeur d'épaules.",
            "Libère les sécurités et descends la charge jusqu'à ce que les genoux s'approchent de la poitrine.",
            "Garde le bas du dos collé au siège en bas.",
            "Pousse puissamment sans claquer les genoux en haut.",
        ),
        "leg_ext" to listOf(
            "Assis, coussinet posé sur le devant des tibias.",
            "Étends les genoux pour lever la charge.",
            "Contracte fort les quadriceps à l'extension complète.",
            "Redescends lentement en résistant.",
        ),
        "leg_curl" to listOf(
            "Place le coussinet au-dessus des talons ou au mollet.",
            "Curl les jambes en pliant les genoux.",
            "Contracte les ischio-jambiers pleinement en haut.",
            "Reviens lentement jusqu'à l'étirement complet.",
        ),
        "hip_thrust" to listOf(
            "Place le haut du dos sur un banc, barre ou charge sur les hanches.",
            "Pousse avec les talons pour monter les hanches.",
            "Termine torse parallèle au sol, menton rentré.",
            "Serre fort les fessiers au verrouillage, redescends sous contrôle.",
        ),
        "calf" to listOf(
            "Place l'avant des pieds sur la plateforme, talons dans le vide.",
            "Descends les talons pour un étirement profond.",
            "Monte sur la pointe des pieds le plus haut possible.",
            "Marque une seconde complète en haut.",
        ),
        "abduction" to listOf(
            "Assise ou allongée, coussinets à l'extérieur des genoux.",
            "Buste immobile, mouvement uniquement au niveau des hanches.",
            "Écarte les jambes contre la résistance.",
            "Reviens lentement en résistant sur tout le trajet.",
        ),
        "adduction" to listOf(
            "Assis ou debout, coussinet à l'intérieur du genou.",
            "Buste droit, mouvement contrôlé.",
            "Rapproche les jambes l'une de l'autre contre la résistance.",
            "Marque une pause, puis reviens lentement.",
        ),
        "crunch" to listOf(
            "Allonge-toi sur le dos, genoux pliés, pieds au sol.",
            "Expire et enroule les côtes vers le bassin.",
            "Décolle juste les omoplates du sol et serre.",
            "Redescends lentement sans perdre la tension.",
        ),
        "sit_up" to listOf(
            "Allonge-toi sur le dos, genoux pliés, pieds bloqués.",
            "Enroule le buste en commençant par la tête.",
            "Monte jusqu'à ce que le torse soit vertical.",
            "Redescends avec contrôle, vertèbre par vertèbre.",
        ),
        "leg_raise" to listOf(
            "Allongé au sol ou suspendu à la barre.",
            "Monte les jambes en basculant le bassin, sans balancer.",
            "Arrête quand les jambes sont verticales ou les pieds au-dessus de la barre.",
            "Descends lentement sans reposer entre les répétitions.",
        ),
        "plank" to listOf(
            "Appuie-toi sur les avant-bras et la pointe des pieds.",
            "Serre les fessiers et gaine le ventre.",
            "Garde une ligne droite de la tête aux talons.",
            "Respire régulièrement et tiens la durée demandée.",
        ),
        "twist" to listOf(
            "Assis, buste droit ou légèrement incliné en arrière.",
            "Fais pivoter le buste d'un côté à l'autre avec contrôle.",
            "La rotation vient des côtes, pas seulement des bras.",
            "Garde le gainage pendant tout le mouvement.",
        ),
        "rollout" to listOf(
            "À genoux, roue sous les épaules.",
            "Gaine le gainage et roule la roue vers l'avant.",
            "Étends aussi loin que possible sans creuser les hanches.",
            "Reviens en tirant avec les abdos, pas les bras.",
        ),
        "wrist" to listOf(
            "Avant-bras posés sur un banc, mains dans le vide.",
            "Curl les poignets de haut en bas sur toute l'amplitude.",
            "Bouge lentement avec des charges légères.",
            "Garde les avant-bras bloqués en place.",
        ),
        "carry" to listOf(
            "Décollé les charges du sol comme un soulevé de terre, debout bien droit.",
            "Gaine le ventre, tire les épaules en arrière.",
            "Marche à pas réguliers sur la distance demandée.",
            "Repose les charges dos plat.",
        ),
        "pullover" to listOf(
            "Allongé sur un banc ou debout face à la poulie.",
            "Tiens la charge au-dessus de la poitrine, coudes légèrement fléchis.",
            "Descends la charge en arc au-dessus de la tête ou tire-la vers le bas.",
            "Reviens le long du même arc en sentant l'étirement des dorsaux et pectoraux.",
        ),
        "swing" to listOf(
            "Debout au-dessus de la charge, dos plat, hanches en charnière.",
            "Balance la charge entre les jambes vers l'arrière.",
            "Projections des hanches vers l'avant pour monter la charge à hauteur de poitrine.",
            "Laisse-la redescendre et enchaîne immédiatement la répétition suivante.",
        ),
    )

    fun name(en: String): String = if (lang == "fr") NAME_FR[en] ?: en else en
    fun muscle(m: String): String = if (lang == "fr") MUSCLE_FR[m] ?: m else m
    fun equip(e: String): String = if (lang == "fr") EQUIP_FR[e] ?: e else e
    fun cues(m: String): List<String> = if (lang == "fr") CUES_FR[m] ?: CUES[m] ?: emptyList() else CUES[m] ?: emptyList()

    /** Step-by-step instructions for an exercise, localized. */
    fun steps(nameEn: String): List<String> {
        val arch = archetypeOf(nameEn)
        return if (lang == "fr") ARCH_STEPS_FR[arch] ?: ARCH_STEPS[arch] ?: emptyList()
        else ARCH_STEPS[arch] ?: emptyList()
    }

    // ---------- fuzzy search (accent-insensitive, multi-token, FR + EN + muscle + aliases) ----------

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
    private fun tokenMatches(token: String, words: List<String>, allowTypo: Boolean): Boolean {
        if (words.any { it.contains(token) }) return true
        if (!allowTypo) return false
        val maxDist = if (token.length >= 5) 2 else if (token.length == 4) 1 else 0
        if (maxDist == 0) return false
        return words.any { w -> kotlin.math.abs(w.length - token.length) <= maxDist && lev(token, w) <= maxDist }
    }

    /** Searchable text for an exercise, split in two groups: name words (typo-tolerant) vs attribute words (exact substring only). */
    private val nameWordsCache = HashMap<String, List<String>>()
    private val attrWordsCache = HashMap<String, List<String>>()

    /** True if every word of the query appears (or nearly, typos allowed on the name) in the exercise name FR/EN, muscle or equipment. */
    fun matches(q: String, nameEn: String): Boolean {
        val nq = normalize(q).trim()
        if (nq.isEmpty()) return true
        val def = EX[nameEn]
        val nameWords = nameWordsCache.getOrPut(nameEn) {
            (normalize(nameEn) + " " + normalize(NAME_FR[nameEn] ?: "") + " " + normalize(ALIAS_FR[nameEn] ?: ""))
                .split(" ").filter { it.isNotBlank() }
        }
        val attrWords = attrWordsCache.getOrPut(nameEn) {
            (normalize(def?.muscle ?: "") + " " + normalize(MUSCLE_FR[def?.muscle] ?: "") + " " +
                normalize(MUSCLE_EXTRA[def?.muscle] ?: "") + " " + normalize(def?.equip ?: "") + " " +
                normalize(EQUIP_FR[def?.equip] ?: ""))
                .split(" ").filter { it.isNotBlank() }
        }
        return nq.split(" ").all { it.isBlank() || tokenMatches(it.trim(), nameWords, allowTypo = true) || tokenMatches(it.trim(), attrWords, allowTypo = false) }
    }

    fun equipHint(e: String): String = when {
        lang != "fr" -> EQUIP_HINT[e] ?: ""
        else -> when (e) {
            "Barbell" -> "Avec une barre chargée de disques."
            "Dumbbell" -> "Avec des haltères — un dans chaque main sauf indication."
            "Machine" -> "Sur une machine à pile ou à charges guidées."
            "Cable" -> "À la station de poulies avec accessoires."
            "Bodyweight" -> "Sans matériel — le corps sert de résistance."
            else -> "Avec du matériel spécifique : banc, élastique, kettlebell ou roue."
        }
    }

}

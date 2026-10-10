package com.hevyclone.app.data

data class ExerciseDef(val name: String, val muscle: String, val equip: String)

private val EX_BASE: List<ExerciseDef> = listOf(
    // ---------------- Chest ----------------
    ExerciseDef("Barbell Bench Press", "Chest", "Barbell"),
    ExerciseDef("Incline Barbell Bench Press", "Chest", "Barbell"),
    ExerciseDef("Decline Bench Press", "Chest", "Barbell"),
    ExerciseDef("Wide Grip Bench Press", "Chest", "Barbell"),
    ExerciseDef("Reverse Grip Bench Press", "Chest", "Barbell"),
    ExerciseDef("Guillotine Press", "Chest", "Barbell"),
    ExerciseDef("Smith Machine Bench Press", "Chest", "Machine"),
    ExerciseDef("Incline Smith Machine Bench Press", "Chest", "Machine"),
    ExerciseDef("Dumbbell Bench Press", "Chest", "Dumbbell"),
    ExerciseDef("Incline Dumbbell Bench Press", "Chest", "Dumbbell"),
    ExerciseDef("Decline Dumbbell Press", "Chest", "Dumbbell"),
    ExerciseDef("Single-Arm Dumbbell Press", "Chest", "Dumbbell"),
    ExerciseDef("Dumbbell Floor Press", "Chest", "Dumbbell"),
    ExerciseDef("Svend Press", "Chest", "Other"),
    ExerciseDef("Dumbbell Fly", "Chest", "Dumbbell"),
    ExerciseDef("Incline Dumbbell Fly", "Chest", "Dumbbell"),
    ExerciseDef("Machine Chest Press", "Chest", "Machine"),
    ExerciseDef("Machine Incline Press", "Chest", "Machine"),
    ExerciseDef("Machine Fly (Pec Deck)", "Chest", "Machine"),
    ExerciseDef("Cable Crossover", "Chest", "Cable"),
    ExerciseDef("Cable Fly", "Chest", "Cable"),
    ExerciseDef("Cable Incline Fly", "Chest", "Cable"),
    ExerciseDef("Low To High Cable Fly", "Chest", "Cable"),
    ExerciseDef("High To Low Cable Fly", "Chest", "Cable"),
    ExerciseDef("Single-Arm Cable Fly", "Chest", "Cable"),
    ExerciseDef("Push Up", "Chest", "Bodyweight"),
    ExerciseDef("Weighted Push Up", "Chest", "Bodyweight"),
    ExerciseDef("Incline Push Up", "Chest", "Bodyweight"),
    ExerciseDef("Decline Push Up", "Chest", "Bodyweight"),
    ExerciseDef("Diamond Push Up", "Chest", "Bodyweight"),
    ExerciseDef("Plyometric Push Up", "Chest", "Bodyweight"),
    ExerciseDef("Chest Dip", "Chest", "Bodyweight"),
    // ---------------- Shoulders ----------------
    ExerciseDef("Overhead Press", "Shoulders", "Barbell"),
    ExerciseDef("Seated Overhead Press", "Shoulders", "Barbell"),
    ExerciseDef("Push Press", "Shoulders", "Barbell"),
    ExerciseDef("Z Press", "Shoulders", "Barbell"),
    ExerciseDef("Seated Dumbbell Shoulder Press", "Shoulders", "Dumbbell"),
    ExerciseDef("Single-Arm Dumbbell Shoulder Press", "Shoulders", "Dumbbell"),
    ExerciseDef("Arnold Press", "Shoulders", "Dumbbell"),
    ExerciseDef("Seated Arnold Press", "Shoulders", "Dumbbell"),
    ExerciseDef("Machine Shoulder Press", "Shoulders", "Machine"),
    ExerciseDef("Smith Machine Shoulder Press", "Shoulders", "Machine"),
    ExerciseDef("Landmine Press", "Shoulders", "Other"),
    ExerciseDef("Lateral Raise", "Shoulders", "Dumbbell"),
    ExerciseDef("Single-Arm Lateral Raise", "Shoulders", "Dumbbell"),
    ExerciseDef("Cable Lateral Raise", "Shoulders", "Cable"),
    ExerciseDef("Single-Arm Cable Lateral Raise", "Shoulders", "Cable"),
    ExerciseDef("Leaning Cable Lateral Raise", "Shoulders", "Cable"),
    ExerciseDef("Machine Lateral Raise", "Shoulders", "Machine"),
    ExerciseDef("Front Raise", "Shoulders", "Dumbbell"),
    ExerciseDef("Barbell Front Raise", "Shoulders", "Barbell"),
    ExerciseDef("Cable Front Raise", "Shoulders", "Cable"),
    ExerciseDef("Rear Delt Fly", "Shoulders", "Dumbbell"),
    ExerciseDef("Incline Rear Delt Fly", "Shoulders", "Dumbbell"),
    ExerciseDef("Cable Rear Delt Fly", "Shoulders", "Cable"),
    ExerciseDef("Dumbbell Rear Delt Row", "Shoulders", "Dumbbell"),
    ExerciseDef("Reverse Pec Deck", "Shoulders", "Machine"),
    ExerciseDef("Face Pull", "Shoulders", "Cable"),
    ExerciseDef("Upright Row", "Shoulders", "Barbell"),
    ExerciseDef("Wide Grip Upright Row", "Shoulders", "Barbell"),
    // ---------------- Biceps ----------------
    ExerciseDef("Barbell Curl", "Biceps", "Barbell"),
    ExerciseDef("EZ Bar Curl", "Biceps", "Barbell"),
    ExerciseDef("Drag Curl", "Biceps", "Barbell"),
    ExerciseDef("Dumbbell Curl", "Biceps", "Dumbbell"),
    ExerciseDef("Seated Dumbbell Curl", "Biceps", "Dumbbell"),
    ExerciseDef("Incline Dumbbell Curl", "Biceps", "Dumbbell"),
    ExerciseDef("Lying Dumbbell Curl", "Biceps", "Dumbbell"),
    ExerciseDef("Hammer Curl", "Biceps", "Dumbbell"),
    ExerciseDef("Incline Hammer Curl", "Biceps", "Dumbbell"),
    ExerciseDef("Cross Body Hammer Curl", "Biceps", "Dumbbell"),
    ExerciseDef("Pinwheel Curl", "Biceps", "Dumbbell"),
    ExerciseDef("Zottman Curl", "Biceps", "Dumbbell"),
    ExerciseDef("Spider Curl", "Biceps", "Dumbbell"),
    ExerciseDef("Concentration Curl", "Biceps", "Dumbbell"),
    ExerciseDef("Preacher Curl", "Biceps", "Machine"),
    ExerciseDef("EZ Preacher Curl", "Biceps", "Barbell"),
    ExerciseDef("Cable Curl", "Biceps", "Cable"),
    ExerciseDef("Single-Arm Cable Curl", "Biceps", "Cable"),
    ExerciseDef("Rope Hammer Curl", "Biceps", "Cable"),
    ExerciseDef("Bayesian Cable Curl", "Biceps", "Cable"),
    ExerciseDef("Chin Up", "Biceps", "Bodyweight"),
    ExerciseDef("Weighted Chin Up", "Biceps", "Bodyweight"),
    // ---------------- Triceps ----------------
    ExerciseDef("Tricep Pushdown", "Triceps", "Cable"),
    ExerciseDef("Rope Pushdown", "Triceps", "Cable"),
    ExerciseDef("Straight Bar Pushdown", "Triceps", "Cable"),
    ExerciseDef("V-Bar Pushdown", "Triceps", "Cable"),
    ExerciseDef("Single-Arm Pushdown", "Triceps", "Cable"),
    ExerciseDef("Overhead Cable Extension", "Triceps", "Cable"),
    ExerciseDef("Overhead Rope Extension", "Triceps", "Cable"),
    ExerciseDef("Overhead Dumbbell Extension", "Triceps", "Dumbbell"),
    ExerciseDef("Single-Arm Overhead Dumbbell Extension", "Triceps", "Dumbbell"),
    ExerciseDef("Overhead Barbell Extension", "Triceps", "Barbell"),
    ExerciseDef("Skullcrusher", "Triceps", "Barbell"),
    ExerciseDef("Incline Skullcrusher", "Triceps", "Barbell"),
    ExerciseDef("Dumbbell Skullcrusher", "Triceps", "Dumbbell"),
    ExerciseDef("Tate Press", "Triceps", "Dumbbell"),
    ExerciseDef("JM Press", "Triceps", "Barbell"),
    ExerciseDef("Close Grip Bench Press", "Triceps", "Barbell"),
    ExerciseDef("Smith Machine Close Grip Bench Press", "Triceps", "Machine"),
    ExerciseDef("Dips", "Triceps", "Bodyweight"),
    ExerciseDef("Weighted Dip", "Triceps", "Bodyweight"),
    ExerciseDef("Bench Dip", "Triceps", "Bodyweight"),
    ExerciseDef("Close Grip Push Up", "Triceps", "Bodyweight"),
    ExerciseDef("Kickback", "Triceps", "Dumbbell"),
    ExerciseDef("Cable Kickback Triceps", "Triceps", "Cable"),
    // ---------------- Lats ----------------
    ExerciseDef("Pull Up", "Lats", "Bodyweight"),
    ExerciseDef("Weighted Pull Up", "Lats", "Bodyweight"),
    ExerciseDef("Wide Grip Pull Up", "Lats", "Bodyweight"),
    ExerciseDef("Close Grip Pull Up", "Lats", "Bodyweight"),
    ExerciseDef("Neutral Grip Pull Up", "Lats", "Bodyweight"),
    ExerciseDef("Assisted Pull Up", "Lats", "Machine"),
    ExerciseDef("Lat Pulldown", "Lats", "Machine"),
    ExerciseDef("Wide Grip Lat Pulldown", "Lats", "Machine"),
    ExerciseDef("Close Grip Lat Pulldown", "Lats", "Machine"),
    ExerciseDef("Reverse Grip Pulldown", "Lats", "Machine"),
    ExerciseDef("Neutral Grip Lat Pulldown", "Lats", "Machine"),
    ExerciseDef("Single-Arm Lat Pulldown", "Lats", "Machine"),
    ExerciseDef("Behind The Neck Pulldown", "Lats", "Machine"),
    ExerciseDef("Straight Arm Pulldown", "Lats", "Cable"),
    ExerciseDef("Bent Over Row", "Lats", "Barbell"),
    ExerciseDef("Pendlay Row", "Lats", "Barbell"),
    ExerciseDef("Yates Row", "Lats", "Barbell"),
    ExerciseDef("Underhand Barbell Row", "Lats", "Barbell"),
    ExerciseDef("Seal Row", "Lats", "Barbell"),
    ExerciseDef("Meadow Row", "Lats", "Other"),
    ExerciseDef("Dumbbell Row", "Lats", "Dumbbell"),
    ExerciseDef("Incline Dumbbell Row", "Lats", "Dumbbell"),
    ExerciseDef("Renegade Row", "Lats", "Dumbbell"),
    ExerciseDef("Chest Supported Row", "Lats", "Machine"),
    ExerciseDef("Seated Cable Row", "Lats", "Cable"),
    ExerciseDef("Single-Arm Cable Row", "Lats", "Cable"),
    ExerciseDef("Wide Grip Cable Row", "Lats", "Cable"),
    ExerciseDef("V-Bar Cable Row", "Lats", "Cable"),
    ExerciseDef("T-Bar Row", "Lats", "Machine"),
    ExerciseDef("Machine Row", "Lats", "Machine"),
    ExerciseDef("Inverted Row", "Lats", "Bodyweight"),
    ExerciseDef("Dumbbell Pullover", "Lats", "Dumbbell"),
    ExerciseDef("Cable Pullover", "Lats", "Cable"),
    ExerciseDef("Machine Pullover", "Lats", "Machine"),
    // ---------------- Lower back ----------------
    ExerciseDef("Deadlift", "Lower back", "Barbell"),
    ExerciseDef("Sumo Deadlift", "Lower back", "Barbell"),
    ExerciseDef("Trap Bar Deadlift", "Lower back", "Barbell"),
    ExerciseDef("Deficit Deadlift", "Lower back", "Barbell"),
    ExerciseDef("Paused Deadlift", "Lower back", "Barbell"),
    ExerciseDef("Snatch Grip Deadlift", "Lower back", "Barbell"),
    ExerciseDef("Romanian Deadlift", "Lower back", "Barbell"),
    ExerciseDef("Dumbbell Romanian Deadlift", "Lower back", "Dumbbell"),
    ExerciseDef("Stiff Leg Deadlift", "Lower back", "Barbell"),
    ExerciseDef("Good Morning", "Lower back", "Barbell"),
    ExerciseDef("Seated Good Morning", "Lower back", "Barbell"),
    ExerciseDef("Back Extension", "Lower back", "Bodyweight"),
    ExerciseDef("45 Degree Back Extension", "Lower back", "Bodyweight"),
    ExerciseDef("Weighted Back Extension", "Lower back", "Other"),
    ExerciseDef("Reverse Hyperextension", "Lower back", "Machine"),
    // ---------------- Traps ----------------
    ExerciseDef("Barbell Shrug", "Traps", "Barbell"),
    ExerciseDef("Dumbbell Shrug", "Traps", "Dumbbell"),
    ExerciseDef("Smith Machine Shrug", "Traps", "Machine"),
    ExerciseDef("Cable Shrug", "Traps", "Cable"),
    ExerciseDef("Behind The Back Shrug", "Traps", "Barbell"),
    ExerciseDef("Overhead Barbell Shrug", "Traps", "Barbell"),
    ExerciseDef("Trap Bar Shrug", "Traps", "Barbell"),
    ExerciseDef("Rack Pull", "Traps", "Barbell"),
    // ---------------- Quads ----------------
    ExerciseDef("Barbell Squat", "Quads", "Barbell"),
    ExerciseDef("Pause Squat", "Quads", "Barbell"),
    ExerciseDef("Box Squat", "Quads", "Barbell"),
    ExerciseDef("Front Squat", "Quads", "Barbell"),
    ExerciseDef("Zercher Squat", "Quads", "Barbell"),
    ExerciseDef("Overhead Squat", "Quads", "Barbell"),
    ExerciseDef("Smith Machine Squat", "Quads", "Machine"),
    ExerciseDef("Hack Squat", "Quads", "Machine"),
    ExerciseDef("Reverse Hack Squat", "Quads", "Machine"),
    ExerciseDef("Belt Squat", "Quads", "Machine"),
    ExerciseDef("Dumbbell Squat", "Quads", "Dumbbell"),
    ExerciseDef("Goblet Squat", "Quads", "Dumbbell"),
    ExerciseDef("Landmine Squat", "Quads", "Other"),
    ExerciseDef("Leg Press", "Quads", "Machine"),
    ExerciseDef("Single-Leg Leg Press", "Quads", "Machine"),
    ExerciseDef("Bulgarian Split Squat", "Quads", "Dumbbell"),
    ExerciseDef("Split Squat", "Quads", "Bodyweight"),
    ExerciseDef("Walking Lunge", "Quads", "Dumbbell"),
    ExerciseDef("Reverse Lunge", "Quads", "Dumbbell"),
    ExerciseDef("Barbell Lunge", "Quads", "Barbell"),
    ExerciseDef("Step Up", "Quads", "Dumbbell"),
    ExerciseDef("Leg Extension", "Quads", "Machine"),
    ExerciseDef("Sissy Squat", "Quads", "Bodyweight"),
    ExerciseDef("Air Squat", "Quads", "Bodyweight"),
    ExerciseDef("Jump Squat", "Quads", "Bodyweight"),
    ExerciseDef("Pistol Squat", "Quads", "Bodyweight"),
    ExerciseDef("Wall Sit", "Quads", "Bodyweight"),
    // ---------------- Hamstrings ----------------
    ExerciseDef("Lying Leg Curl", "Hamstrings", "Machine"),
    ExerciseDef("Single-Leg Lying Curl", "Hamstrings", "Machine"),
    ExerciseDef("Seated Leg Curl", "Hamstrings", "Machine"),
    ExerciseDef("Standing Leg Curl", "Hamstrings", "Machine"),
    ExerciseDef("Sliding Leg Curl", "Hamstrings", "Bodyweight"),
    ExerciseDef("Stability Ball Leg Curl", "Hamstrings", "Other"),
    ExerciseDef("Nordic Curl", "Hamstrings", "Bodyweight"),
    ExerciseDef("Glute Ham Raise", "Hamstrings", "Bodyweight"),
    ExerciseDef("Kettlebell Swing", "Hamstrings", "Other"),
    ExerciseDef("Single Leg Romanian Deadlift", "Hamstrings", "Dumbbell"),
    // ---------------- Glutes ----------------
    ExerciseDef("Barbell Hip Thrust", "Glutes", "Barbell"),
    ExerciseDef("Smith Machine Hip Thrust", "Glutes", "Machine"),
    ExerciseDef("Glute Bridge", "Glutes", "Bodyweight"),
    ExerciseDef("Barbell Glute Bridge", "Glutes", "Barbell"),
    ExerciseDef("Single-Leg Glute Bridge", "Glutes", "Bodyweight"),
    ExerciseDef("Frog Pump", "Glutes", "Bodyweight"),
    ExerciseDef("Cable Kickback", "Glutes", "Cable"),
    ExerciseDef("Machine Glute Kickback", "Glutes", "Machine"),
    ExerciseDef("Cable Pull Through", "Glutes", "Cable"),
    ExerciseDef("Curtsy Lunge", "Glutes", "Dumbbell"),
    // ---------------- Abductors ----------------
    ExerciseDef("Abduction Machine", "Abductors", "Machine"),
    ExerciseDef("Cable Hip Abduction", "Abductors", "Cable"),
    ExerciseDef("Side Lying Hip Abduction", "Abductors", "Bodyweight"),
    // ---------------- Adductors ----------------
    ExerciseDef("Adduction Machine", "Adductors", "Machine"),
    ExerciseDef("Standing Cable Adduction", "Adductors", "Cable"),
    ExerciseDef("Copenhagen Plank", "Adductors", "Bodyweight"),
    ExerciseDef("Side Lunge", "Adductors", "Dumbbell"),
    ExerciseDef("Sumo Squat", "Adductors", "Barbell"),
    // ---------------- Calves ----------------
    ExerciseDef("Standing Calf Raise", "Calves", "Machine"),
    ExerciseDef("Smith Machine Calf Raise", "Calves", "Machine"),
    ExerciseDef("Seated Calf Raise", "Calves", "Machine"),
    ExerciseDef("Leg Press Calf Raise", "Calves", "Machine"),
    ExerciseDef("Donkey Calf Raise", "Calves", "Machine"),
    ExerciseDef("Single Leg Calf Raise", "Calves", "Bodyweight"),
    ExerciseDef("Tibialis Raise", "Calves", "Bodyweight"),
    // ---------------- Abs ----------------
    ExerciseDef("Plank", "Abs", "Bodyweight"),
    ExerciseDef("Side Plank", "Abs", "Bodyweight"),
    ExerciseDef("Plank Shoulder Tap", "Abs", "Bodyweight"),
    ExerciseDef("Hollow Hold", "Abs", "Bodyweight"),
    ExerciseDef("Crunch", "Abs", "Bodyweight"),
    ExerciseDef("Weighted Crunch", "Abs", "Other"),
    ExerciseDef("Machine Crunch", "Abs", "Machine"),
    ExerciseDef("Cable Crunch", "Abs", "Cable"),
    ExerciseDef("Sit Up", "Abs", "Bodyweight"),
    ExerciseDef("Weighted Sit Up", "Abs", "Other"),
    ExerciseDef("Decline Sit Up", "Abs", "Bodyweight"),
    ExerciseDef("Butterfly Sit Up", "Abs", "Bodyweight"),
    ExerciseDef("Jackknife Sit Up", "Abs", "Bodyweight"),
    ExerciseDef("Reverse Crunch", "Abs", "Bodyweight"),
    ExerciseDef("V-Up", "Abs", "Bodyweight"),
    ExerciseDef("Bicycle Crunch", "Abs", "Bodyweight"),
    ExerciseDef("Russian Twist", "Abs", "Bodyweight"),
    ExerciseDef("Windshield Wipers", "Abs", "Bodyweight"),
    ExerciseDef("Dead Bug", "Abs", "Bodyweight"),
    ExerciseDef("Flutter Kicks", "Abs", "Bodyweight"),
    ExerciseDef("Scissor Kicks", "Abs", "Bodyweight"),
    ExerciseDef("Mountain Climbers", "Abs", "Bodyweight"),
    ExerciseDef("Leg Raise", "Abs", "Bodyweight"),
    ExerciseDef("Hanging Leg Raise", "Abs", "Bodyweight"),
    ExerciseDef("Hanging Knee Raise", "Abs", "Bodyweight"),
    ExerciseDef("Dragon Flag", "Abs", "Bodyweight"),
    ExerciseDef("Ab Wheel Rollout", "Abs", "Other"),
    ExerciseDef("Dumbbell Side Bend", "Abs", "Dumbbell"),
    // ---------------- Forearms ----------------
    ExerciseDef("Wrist Curl", "Forearms", "Dumbbell"),
    ExerciseDef("Reverse Wrist Curl", "Forearms", "Barbell"),
    ExerciseDef("Behind The Back Wrist Curl", "Forearms", "Barbell"),
    ExerciseDef("Reverse Curl", "Forearms", "Barbell"),
    ExerciseDef("Farmer's Carry", "Forearms", "Dumbbell"),
    ExerciseDef("Single-Arm Farmer's Carry", "Forearms", "Dumbbell"),
    ExerciseDef("Dead Hang", "Forearms", "Bodyweight"),
)

private val EX_EXTRA: List<ExerciseDef> = listOf(
    // ---------------- Chest (extra) ----------------
    ExerciseDef("Smith Machine Decline Press", "Chest", "Machine"),
    ExerciseDef("Incline Close Grip Bench Press", "Chest", "Barbell"),
    ExerciseDef("Wide Grip Incline Bench Press", "Chest", "Barbell"),
    ExerciseDef("Barbell Floor Press", "Chest", "Barbell"),
    ExerciseDef("Close Grip Floor Press", "Chest", "Barbell"),
    ExerciseDef("Pause Bench Press", "Chest", "Barbell"),
    ExerciseDef("Tempo Bench Press", "Chest", "Barbell"),
    ExerciseDef("Spoto Press", "Chest", "Barbell"),
    ExerciseDef("Dumbbell Squeeze Press", "Chest", "Dumbbell"),
    ExerciseDef("Incline Dumbbell Squeeze Press", "Chest", "Dumbbell"),
    ExerciseDef("Neutral Grip Dumbbell Press", "Chest", "Dumbbell"),
    ExerciseDef("Alternating Dumbbell Bench Press", "Chest", "Dumbbell"),
    ExerciseDef("Floor Fly", "Chest", "Dumbbell"),
    ExerciseDef("Machine Decline Press", "Chest", "Machine"),
    ExerciseDef("Single-Arm Machine Press", "Chest", "Machine"),
    ExerciseDef("Jammer Press", "Chest", "Machine"),
    ExerciseDef("Vertical Chest Press", "Chest", "Machine"),
    ExerciseDef("Standing Cable Chest Press", "Chest", "Cable"),
    ExerciseDef("Single-Arm Cable Press", "Chest", "Cable"),
    ExerciseDef("Cable Iron Cross", "Chest", "Cable"),
    ExerciseDef("Incline Cable Crossover", "Chest", "Cable"),
    ExerciseDef("Decline Cable Fly", "Chest", "Cable"),
    ExerciseDef("Incline Machine Fly", "Chest", "Machine"),
    ExerciseDef("Weighted Chest Dip", "Chest", "Bodyweight"),
    ExerciseDef("Assisted Chest Dip", "Chest", "Machine"),
    ExerciseDef("Band Push Up", "Chest", "Other"),
    ExerciseDef("Band Chest Press", "Chest", "Other"),
    ExerciseDef("Band Chest Fly", "Chest", "Other"),
    ExerciseDef("TRX Push Up", "Chest", "Other"),
    ExerciseDef("Ring Push Up", "Chest", "Other"),
    ExerciseDef("Ring Fly", "Chest", "Other"),
    ExerciseDef("Hindu Push Up", "Chest", "Bodyweight"),
    ExerciseDef("Dive Bomber Push Up", "Chest", "Bodyweight"),
    ExerciseDef("Archer Push Up", "Chest", "Bodyweight"),
    ExerciseDef("Pseudo Planche Push Up", "Chest", "Bodyweight"),
    ExerciseDef("Deficit Push Up", "Chest", "Bodyweight"),
    ExerciseDef("Rotational Push Up", "Chest", "Bodyweight"),
    ExerciseDef("Tempo Push Up", "Chest", "Bodyweight"),
    ExerciseDef("Wide Push Up", "Chest", "Bodyweight"),
    ExerciseDef("Clapping Push Up", "Chest", "Bodyweight"),
    ExerciseDef("Wall Push Up", "Chest", "Bodyweight"),
    ExerciseDef("Knee Push Up", "Chest", "Bodyweight"),
    ExerciseDef("Single-Arm Push Up", "Chest", "Bodyweight"),
    ExerciseDef("Scap Push Up", "Chest", "Bodyweight"),
    ExerciseDef("Medicine Ball Chest Pass", "Chest", "Other"),
    // ---------------- Shoulders (extra) ----------------
    ExerciseDef("Standing Dumbbell Shoulder Press", "Shoulders", "Dumbbell"),
    ExerciseDef("Alternating Dumbbell Shoulder Press", "Shoulders", "Dumbbell"),
    ExerciseDef("Neutral Grip Dumbbell Shoulder Press", "Shoulders", "Dumbbell"),
    ExerciseDef("Standing Arnold Press", "Shoulders", "Dumbbell"),
    ExerciseDef("Bradford Press", "Shoulders", "Barbell"),
    ExerciseDef("Behind The Neck Press", "Shoulders", "Barbell"),
    ExerciseDef("Plate Overhead Press", "Shoulders", "Other"),
    ExerciseDef("Kettlebell Press", "Shoulders", "Other"),
    ExerciseDef("Single-Arm Landmine Press", "Shoulders", "Other"),
    ExerciseDef("Half Kneeling Landmine Press", "Shoulders", "Other"),
    ExerciseDef("Handstand Push Up", "Shoulders", "Bodyweight"),
    ExerciseDef("Wall Handstand Hold", "Shoulders", "Bodyweight"),
    ExerciseDef("Pike Push Up", "Shoulders", "Bodyweight"),
    ExerciseDef("Elevated Pike Push Up", "Shoulders", "Bodyweight"),
    ExerciseDef("Seated Lateral Raise", "Shoulders", "Dumbbell"),
    ExerciseDef("Lying Lateral Raise", "Shoulders", "Dumbbell"),
    ExerciseDef("Ring Lateral Raise", "Shoulders", "Other"),
    ExerciseDef("Plate Front Raise", "Shoulders", "Other"),
    ExerciseDef("Incline Front Raise", "Shoulders", "Dumbbell"),
    ExerciseDef("Single-Arm Front Raise", "Shoulders", "Dumbbell"),
    ExerciseDef("Single-Arm Cable Front Raise", "Shoulders", "Cable"),
    ExerciseDef("Cable Rear Delt Row", "Shoulders", "Cable"),
    ExerciseDef("Reverse Cable Crossover", "Shoulders", "Cable"),
    ExerciseDef("Incline Y Raise", "Shoulders", "Dumbbell"),
    ExerciseDef("W Raise", "Shoulders", "Dumbbell"),
    ExerciseDef("Cuban Press", "Shoulders", "Dumbbell"),
    ExerciseDef("Scaption", "Shoulders", "Dumbbell"),
    ExerciseDef("Band Lateral Raise", "Shoulders", "Other"),
    ExerciseDef("Band Front Raise", "Shoulders", "Other"),
    ExerciseDef("Band Pull Apart", "Shoulders", "Other"),
    ExerciseDef("Band Face Pull", "Shoulders", "Other"),
    ExerciseDef("Cable Upright Row", "Shoulders", "Cable"),
    ExerciseDef("Dumbbell Upright Row", "Shoulders", "Dumbbell"),
    ExerciseDef("Snatch Grip High Pull", "Shoulders", "Barbell"),
    ExerciseDef("Push Jerk", "Shoulders", "Barbell"),
    ExerciseDef("Split Jerk", "Shoulders", "Barbell"),
    ExerciseDef("Battle Ropes", "Shoulders", "Other"),
    // ---------------- Biceps (extra) ----------------
    ExerciseDef("Wide Grip Barbell Curl", "Biceps", "Barbell"),
    ExerciseDef("Close Grip EZ Bar Curl", "Biceps", "Barbell"),
    ExerciseDef("Supinating Dumbbell Curl", "Biceps", "Dumbbell"),
    ExerciseDef("Single-Arm Dumbbell Curl", "Biceps", "Dumbbell"),
    ExerciseDef("Seated Alternating Dumbbell Curl", "Biceps", "Dumbbell"),
    ExerciseDef("Deficit Dumbbell Curl", "Biceps", "Dumbbell"),
    ExerciseDef("Slow Eccentric Curl", "Biceps", "Dumbbell"),
    ExerciseDef("Slow Lowering Chin Up", "Biceps", "Bodyweight"),
    ExerciseDef("Close Grip Chin Up", "Biceps", "Bodyweight"),
    ExerciseDef("High Pulley Curl", "Biceps", "Cable"),
    ExerciseDef("Lying Cable Curl", "Biceps", "Cable"),
    ExerciseDef("Machine Biceps Curl", "Biceps", "Machine"),
    ExerciseDef("Preacher Curl Dumbbell", "Biceps", "Dumbbell"),
    ExerciseDef("Single-Arm Preacher Curl", "Biceps", "Dumbbell"),
    ExerciseDef("Wall Curl", "Biceps", "Barbell"),
    ExerciseDef("21s Curl", "Biceps", "Barbell"),
    ExerciseDef("Waiter Curl", "Biceps", "Other"),
    ExerciseDef("Kettlebell Curl", "Biceps", "Other"),
    ExerciseDef("Band Curl", "Biceps", "Other"),
    ExerciseDef("Band Hammer Curl", "Biceps", "Other"),
    ExerciseDef("TRX Curl", "Biceps", "Other"),
    ExerciseDef("Ring Curl", "Biceps", "Other"),
    // ---------------- Triceps (extra) ----------------
    ExerciseDef("Close Grip Decline Bench Press", "Triceps", "Barbell"),
    ExerciseDef("EZ Bar Skullcrusher", "Triceps", "Barbell"),
    ExerciseDef("Decline Skullcrusher", "Triceps", "Barbell"),
    ExerciseDef("Smith Machine Skullcrusher", "Triceps", "Machine"),
    ExerciseDef("Cable Skullcrusher", "Triceps", "Cable"),
    ExerciseDef("Overhead EZ Bar Extension", "Triceps", "Barbell"),
    ExerciseDef("V-Bar Overhead Extension", "Triceps", "Cable"),
    ExerciseDef("Single-Arm Overhead Cable Extension", "Triceps", "Cable"),
    ExerciseDef("Cable Cross Triceps Extension", "Triceps", "Cable"),
    ExerciseDef("Incline Cable Extension", "Triceps", "Cable"),
    ExerciseDef("Reverse Grip Pushdown", "Triceps", "Cable"),
    ExerciseDef("Assisted Dip", "Triceps", "Machine"),
    ExerciseDef("Ring Dip", "Triceps", "Other"),
    ExerciseDef("Machine Dip", "Triceps", "Machine"),
    ExerciseDef("Weighted Bench Dip", "Triceps", "Bodyweight"),
    ExerciseDef("Incline Dumbbell Kickback", "Triceps", "Dumbbell"),
    ExerciseDef("Lying Single-Arm Triceps Extension", "Triceps", "Dumbbell"),
    ExerciseDef("Elbows-In Dumbbell Floor Press", "Triceps", "Dumbbell"),
    ExerciseDef("Triceps Machine Extension", "Triceps", "Machine"),
    ExerciseDef("Band Pushdown", "Triceps", "Other"),
    ExerciseDef("Band Overhead Extension", "Triceps", "Other"),
    ExerciseDef("Band Kickback", "Triceps", "Other"),
    ExerciseDef("Band Close Grip Push Up", "Triceps", "Other"),
    ExerciseDef("Weighted Close Grip Push Up", "Triceps", "Bodyweight"),
    ExerciseDef("TRX Triceps Extension", "Triceps", "Other"),
    // ---------------- Lats (extra) ----------------
    ExerciseDef("Single-Arm Machine Row", "Lats", "Machine"),
    ExerciseDef("High Row Machine", "Lats", "Machine"),
    ExerciseDef("Low Row Machine", "Lats", "Machine"),
    ExerciseDef("Iso-Lateral Row", "Lats", "Machine"),
    ExerciseDef("Single-Arm T-Bar Row", "Lats", "Machine"),
    ExerciseDef("Smith Machine Row", "Lats", "Machine"),
    ExerciseDef("Smith Machine Bent Over Row", "Lats", "Machine"),
    ExerciseDef("Smith Machine Inverted Row", "Lats", "Machine"),
    ExerciseDef("Kroc Row", "Lats", "Dumbbell"),
    ExerciseDef("Three-Point Dumbbell Row", "Lats", "Dumbbell"),
    ExerciseDef("Gorilla Row", "Lats", "Dumbbell"),
    ExerciseDef("Landmine Single-Arm Row", "Lats", "Other"),
    ExerciseDef("Kettlebell Row", "Lats", "Other"),
    ExerciseDef("Barbell Pullover", "Lats", "Barbell"),
    ExerciseDef("Single-Arm Dumbbell Pullover", "Lats", "Dumbbell"),
    ExerciseDef("Wide Grip Barbell Row", "Lats", "Barbell"),
    ExerciseDef("Underhand Inverted Row", "Lats", "Bodyweight"),
    ExerciseDef("Wide Grip Inverted Row", "Lats", "Bodyweight"),
    ExerciseDef("Weighted Inverted Row", "Lats", "Bodyweight"),
    ExerciseDef("TRX Row", "Lats", "Other"),
    ExerciseDef("Ring Row", "Lats", "Other"),
    ExerciseDef("Band Bent Over Row", "Lats", "Other"),
    ExerciseDef("Band Straight Arm Pulldown", "Lats", "Other"),
    ExerciseDef("Band Lat Pulldown", "Lats", "Other"),
    ExerciseDef("Band Assisted Pull Up", "Lats", "Other"),
    ExerciseDef("Assisted Chin Up", "Lats", "Machine"),
    ExerciseDef("Assisted Neutral Grip Pull Up", "Lats", "Machine"),
    ExerciseDef("L-Pull Up", "Lats", "Bodyweight"),
    ExerciseDef("Commando Pull Up", "Lats", "Bodyweight"),
    ExerciseDef("Kipping Pull Up", "Lats", "Bodyweight"),
    ExerciseDef("Slow Lowering Pull Up", "Lats", "Bodyweight"),
    ExerciseDef("Muscle Up", "Lats", "Bodyweight"),
    ExerciseDef("Scap Pull Up", "Lats", "Bodyweight"),
    ExerciseDef("L-Hang", "Lats", "Bodyweight"),
    ExerciseDef("Lat Pulldown Rope", "Lats", "Machine"),
    ExerciseDef("Kneeling Single-Arm Lat Pulldown", "Lats", "Machine"),
    ExerciseDef("Standing Pulldown", "Lats", "Cable"),
    ExerciseDef("Rope Cable Row", "Lats", "Cable"),
    ExerciseDef("Underhand Cable Row", "Lats", "Cable"),
    ExerciseDef("Close Grip Cable Row", "Lats", "Cable"),
    ExerciseDef("Standing Single-Arm Cable Row", "Lats", "Cable"),
    // ---------------- Lower back (extra) ----------------
    ExerciseDef("Power Clean", "Lower back", "Barbell"),
    ExerciseDef("Hang Clean", "Lower back", "Barbell"),
    ExerciseDef("Squat Clean", "Lower back", "Barbell"),
    ExerciseDef("Clean Pull", "Lower back", "Barbell"),
    ExerciseDef("Clean and Jerk", "Lower back", "Barbell"),
    ExerciseDef("Clean and Press", "Lower back", "Barbell"),
    ExerciseDef("Power Snatch", "Lower back", "Barbell"),
    ExerciseDef("Hang Snatch", "Lower back", "Barbell"),
    ExerciseDef("Snatch Pull", "Lower back", "Barbell"),
    ExerciseDef("Snatch Balance", "Lower back", "Barbell"),
    ExerciseDef("Kettlebell Snatch", "Lower back", "Other"),
    ExerciseDef("Kettlebell Deadlift", "Lower back", "Other"),
    ExerciseDef("Dumbbell Deadlift", "Lower back", "Dumbbell"),
    ExerciseDef("Single-Leg Deadlift", "Lower back", "Barbell"),
    ExerciseDef("Dimel Deadlift", "Lower back", "Barbell"),
    ExerciseDef("Suitcase Deadlift", "Lower back", "Other"),
    ExerciseDef("Band Good Morning", "Lower back", "Other"),
    // ---------------- Traps (extra) ----------------
    ExerciseDef("Snatch Grip Shrug", "Traps", "Barbell"),
    ExerciseDef("Clean Grip Shrug", "Traps", "Barbell"),
    ExerciseDef("Jump Shrug", "Traps", "Barbell"),
    ExerciseDef("Machine Shrug", "Traps", "Machine"),
    ExerciseDef("Seated Dumbbell Shrug", "Traps", "Dumbbell"),
    ExerciseDef("Incline Dumbbell Shrug", "Traps", "Dumbbell"),
    ExerciseDef("Block Pull", "Traps", "Barbell"),
    ExerciseDef("Band Shrug", "Traps", "Other"),
    // ---------------- Quads (extra) ----------------
    ExerciseDef("Tempo Squat", "Quads", "Barbell"),
    ExerciseDef("Pin Squat", "Quads", "Barbell"),
    ExerciseDef("Anderson Squat", "Quads", "Barbell"),
    ExerciseDef("Low Bar Squat", "Quads", "Barbell"),
    ExerciseDef("Cyclist Squat", "Quads", "Barbell"),
    ExerciseDef("Squat to Bench", "Quads", "Barbell"),
    ExerciseDef("Close Stance Squat", "Quads", "Barbell"),
    ExerciseDef("Wide Stance Squat", "Quads", "Barbell"),
    ExerciseDef("Dumbbell Front Squat", "Quads", "Dumbbell"),
    ExerciseDef("Kettlebell Front Squat", "Quads", "Other"),
    ExerciseDef("Double Kettlebell Front Squat", "Quads", "Other"),
    ExerciseDef("Cable Squat", "Quads", "Cable"),
    ExerciseDef("Band Squat", "Quads", "Other"),
    ExerciseDef("Spanish Squat", "Quads", "Other"),
    ExerciseDef("Reverse Nordic", "Quads", "Bodyweight"),
    ExerciseDef("Cossack Squat", "Quads", "Bodyweight"),
    ExerciseDef("Skater Squat", "Quads", "Bodyweight"),
    ExerciseDef("Shrimp Squat", "Quads", "Bodyweight"),
    ExerciseDef("Assisted Pistol Squat", "Quads", "Bodyweight"),
    ExerciseDef("Squat Hold", "Quads", "Bodyweight"),
    ExerciseDef("Single-Leg Extension", "Quads", "Machine"),
    ExerciseDef("Narrow Stance Leg Press", "Quads", "Machine"),
    ExerciseDef("Wide Stance Leg Press", "Quads", "Machine"),
    ExerciseDef("High Box Step Up", "Quads", "Dumbbell"),
    ExerciseDef("Barbell Step Up", "Quads", "Barbell"),
    ExerciseDef("Forward Lunge", "Quads", "Dumbbell"),
    ExerciseDef("Overhead Lunge", "Quads", "Dumbbell"),
    ExerciseDef("Dumbbell Split Squat", "Quads", "Dumbbell"),
    ExerciseDef("Goblet Split Squat", "Quads", "Other"),
    ExerciseDef("Jumping Lunge", "Quads", "Bodyweight"),
    ExerciseDef("Step Down", "Quads", "Bodyweight"),
    ExerciseDef("Terminal Knee Extension", "Quads", "Other"),
    ExerciseDef("Box Jump", "Quads", "Bodyweight"),
    ExerciseDef("Broad Jump", "Quads", "Bodyweight"),
    ExerciseDef("Depth Jump", "Quads", "Bodyweight"),
    ExerciseDef("Frog Jump", "Quads", "Bodyweight"),
    ExerciseDef("Sled Push", "Quads", "Other"),
    ExerciseDef("Sled Drag", "Quads", "Other"),
    // ---------------- Hamstrings (extra) ----------------
    ExerciseDef("Kettlebell Romanian Deadlift", "Hamstrings", "Other"),
    ExerciseDef("GHD Hip Extension", "Hamstrings", "Other"),
    ExerciseDef("Assisted Nordic Curl", "Hamstrings", "Bodyweight"),
    ExerciseDef("Band Leg Curl", "Hamstrings", "Other"),
    ExerciseDef("Single-Arm Kettlebell Swing", "Hamstrings", "Other"),
    ExerciseDef("American Kettlebell Swing", "Hamstrings", "Other"),
    ExerciseDef("Sumo Stiff Leg Deadlift", "Hamstrings", "Barbell"),
    // ---------------- Glutes (extra) ----------------
    ExerciseDef("Single-Leg Hip Thrust", "Glutes", "Barbell"),
    ExerciseDef("Banded Hip Thrust", "Glutes", "Other"),
    ExerciseDef("Banded Glute Bridge", "Glutes", "Other"),
    ExerciseDef("Elevated Glute Bridge", "Glutes", "Bodyweight"),
    ExerciseDef("Glute Bridge March", "Glutes", "Bodyweight"),
    ExerciseDef("Single-Leg Cable Kickback", "Glutes", "Cable"),
    ExerciseDef("Band Lateral Walk", "Glutes", "Other"),
    ExerciseDef("Clamshell", "Glutes", "Other"),
    ExerciseDef("Fire Hydrant", "Glutes", "Bodyweight"),
    // ---------------- Abductors (extra) ----------------
    ExerciseDef("Band Hip Abduction", "Abductors", "Other"),
    ExerciseDef("Side Plank Leg Raise", "Abductors", "Bodyweight"),
    ExerciseDef("Monster Walk", "Abductors", "Other"),
    // ---------------- Calves (extra) ----------------
    ExerciseDef("Standing Barbell Calf Raise", "Calves", "Barbell"),
    ExerciseDef("Standing Dumbbell Calf Raise", "Calves", "Dumbbell"),
    ExerciseDef("Single-Leg Dumbbell Calf Raise", "Calves", "Dumbbell"),
    ExerciseDef("Seated Barbell Calf Raise", "Calves", "Barbell"),
    ExerciseDef("Hack Squat Calf Raise", "Calves", "Machine"),
    ExerciseDef("Band Calf Raise", "Calves", "Other"),
    ExerciseDef("Seated Tibialis Raise", "Calves", "Bodyweight"),
    ExerciseDef("Farmer's Walk on Toes", "Calves", "Dumbbell"),
    // ---------------- Abs (extra) ----------------
    ExerciseDef("Weighted Plank", "Abs", "Other"),
    ExerciseDef("RKC Plank", "Abs", "Bodyweight"),
    ExerciseDef("Extended Plank", "Abs", "Bodyweight"),
    ExerciseDef("Plank Hip Dip", "Abs", "Bodyweight"),
    ExerciseDef("Plank Knee To Elbow", "Abs", "Bodyweight"),
    ExerciseDef("Plank Pull Through", "Abs", "Bodyweight"),
    ExerciseDef("Body Saw", "Abs", "Bodyweight"),
    ExerciseDef("Weighted Side Plank", "Abs", "Other"),
    ExerciseDef("Side Plank Hip Raise", "Abs", "Bodyweight"),
    ExerciseDef("Side Plank Reach Through", "Abs", "Bodyweight"),
    ExerciseDef("Bird Dog", "Abs", "Bodyweight"),
    ExerciseDef("Bear Crawl", "Abs", "Bodyweight"),
    ExerciseDef("Crab Walk", "Abs", "Bodyweight"),
    ExerciseDef("Superman", "Abs", "Bodyweight"),
    ExerciseDef("Reverse Snow Angel", "Abs", "Bodyweight"),
    ExerciseDef("Arch Hold", "Abs", "Bodyweight"),
    ExerciseDef("Hollow Rock", "Abs", "Bodyweight"),
    ExerciseDef("V-Sit Hold", "Abs", "Bodyweight"),
    ExerciseDef("L-Sit", "Abs", "Bodyweight"),
    ExerciseDef("Cocoon", "Abs", "Bodyweight"),
    ExerciseDef("Cross Body Crunch", "Abs", "Bodyweight"),
    ExerciseDef("Oblique Crunch", "Abs", "Bodyweight"),
    ExerciseDef("Standing Oblique Crunch", "Abs", "Bodyweight"),
    ExerciseDef("Toe Touch Crunch", "Abs", "Bodyweight"),
    ExerciseDef("V-Up Twist", "Abs", "Bodyweight"),
    ExerciseDef("Stability Ball Crunch", "Abs", "Other"),
    ExerciseDef("Sit Up Twist", "Abs", "Bodyweight"),
    ExerciseDef("GHD Sit Up", "Abs", "Other"),
    ExerciseDef("Seated Knee Up", "Abs", "Bodyweight"),
    ExerciseDef("Side Jackknife", "Abs", "Bodyweight"),
    ExerciseDef("Captain's Chair Leg Raise", "Abs", "Other"),
    ExerciseDef("Captain's Chair Knee Raise", "Abs", "Other"),
    ExerciseDef("Toe To Bar", "Abs", "Bodyweight"),
    ExerciseDef("Standing Side Bend", "Abs", "Bodyweight"),
    ExerciseDef("Cable Side Bend", "Abs", "Cable"),
    ExerciseDef("Roman Chair Side Bend", "Abs", "Other"),
    ExerciseDef("Weighted Russian Twist", "Abs", "Other"),
    ExerciseDef("Decline Russian Twist", "Abs", "Bodyweight"),
    ExerciseDef("Seated Barbell Twist", "Abs", "Barbell"),
    ExerciseDef("Wood Chop", "Abs", "Cable"),
    ExerciseDef("Reverse Wood Chop", "Abs", "Cable"),
    ExerciseDef("Pallof Press", "Abs", "Cable"),
    ExerciseDef("Band Pallof Press", "Abs", "Other"),
    ExerciseDef("Abdominal Vacuum", "Abs", "Bodyweight"),
    ExerciseDef("Standing Ab Wheel Rollout", "Abs", "Other"),
    ExerciseDef("Swiss Ball Rollout", "Abs", "Other"),
    ExerciseDef("Swiss Ball Jackknife", "Abs", "Other"),
    ExerciseDef("Stability Ball Pass", "Abs", "Other"),
    ExerciseDef("TRX Crunch", "Abs", "Other"),
    ExerciseDef("TRX Fallout", "Abs", "Other"),
    ExerciseDef("Single Leg Lower", "Abs", "Bodyweight"),
    ExerciseDef("Hanging Oblique Knee Raise", "Abs", "Bodyweight"),
    // ---------------- Forearms (extra) ----------------
    ExerciseDef("Dumbbell Wrist Curl", "Forearms", "Dumbbell"),
    ExerciseDef("Standing Barbell Wrist Curl", "Forearms", "Barbell"),
    ExerciseDef("Cable Reverse Curl", "Forearms", "Cable"),
    ExerciseDef("Reverse Dumbbell Curl", "Forearms", "Dumbbell"),
    ExerciseDef("Plate Pinch", "Forearms", "Other"),
    ExerciseDef("Hex Dumbbell Hold", "Forearms", "Dumbbell"),
    ExerciseDef("Wrist Roller", "Forearms", "Other"),
    ExerciseDef("Towel Pull Up", "Forearms", "Bodyweight"),
    ExerciseDef("Rice Bucket", "Forearms", "Other"),
    ExerciseDef("Towel Wring", "Forearms", "Other"),
    ExerciseDef("Figure 8", "Forearms", "Other"),
    ExerciseDef("Waiter Carry", "Forearms", "Other"),
    ExerciseDef("Overhead Carry", "Forearms", "Dumbbell"),
    ExerciseDef("Racked Carry", "Forearms", "Other"),
    ExerciseDef("Suitcase Carry", "Forearms", "Dumbbell"),
    ExerciseDef("Turkish Get Up", "Abs", "Other"),
    ExerciseDef("Barbell Thruster", "Quads", "Barbell"),
    // ---------------- Cardio ----------------
    ExerciseDef("Burpees", "Cardio", "Bodyweight"),
    ExerciseDef("Jumping Jacks", "Cardio", "Bodyweight"),
    ExerciseDef("High Knees", "Cardio", "Bodyweight"),
    ExerciseDef("Butt Kicks", "Cardio", "Bodyweight"),
    ExerciseDef("Speed Skaters", "Cardio", "Bodyweight"),
    ExerciseDef("Lateral Bounds", "Cardio", "Bodyweight"),
    ExerciseDef("Shadow Boxing", "Cardio", "Bodyweight"),
    ExerciseDef("Jump Rope", "Cardio", "Other"),
    ExerciseDef("Rowing Machine", "Cardio", "Machine"),
    ExerciseDef("Assault Bike", "Cardio", "Machine"),
    ExerciseDef("Versa Climber", "Cardio", "Machine"),
    ExerciseDef("Ski Erg", "Cardio", "Machine"),
    ExerciseDef("Jacobs Ladder", "Cardio", "Machine"),
    ExerciseDef("Treadmill", "Cardio", "Machine"),
    ExerciseDef("Incline Treadmill Walk", "Cardio", "Machine"),
    ExerciseDef("Cycling", "Cardio", "Machine"),
    ExerciseDef("Elliptical", "Cardio", "Machine"),
    ExerciseDef("Stair Climber", "Cardio", "Machine"),
    ExerciseDef("Swimming", "Cardio", "Other"),
    ExerciseDef("Walking", "Cardio", "Bodyweight"),
    ExerciseDef("Running", "Cardio", "Bodyweight"),
)

val EXERCISES: MutableList<ExerciseDef> = (EX_BASE + EX_EXTRA).toMutableList()

val EX: MutableMap<String, ExerciseDef> = EXERCISES.associateBy { it.name }.toMutableMap()
val MUSCLES: List<String> = EXERCISES.map { it.muscle }.distinct().sorted()

val EQUIP_HINT: Map<String, String> = mapOf(
    "Barbell" to "Performed with a barbell loaded with plates.",
    "Dumbbell" to "Uses dumbbells — one in each hand unless noted.",
    "Machine" to "Performed on a pin- or plate-loaded machine.",
    "Cable" to "Performed at a cable station with attachments.",
    "Bodyweight" to "No equipment needed — your body provides the resistance.",
    "Other" to "Uses specialty equipment such as a bench, band, kettlebell or wheel.",
)

val CUES: Map<String, List<String>> = mapOf(
    "Chest" to listOf(
        "Pull the shoulder blades back and down and keep them pinned to the bench.",
        "Lower the weight under control until you feel a stretch across the chest.",
        "Press up and slightly back without letting the elbows flare past about 75°.",
    ),
    "Shoulders" to listOf(
        "Keep the ribcage down — do not arch the lower back to move the weight.",
        "On raises, lead with the elbows and stop at shoulder height.",
        "Control the negative; the delt works hardest on the way down.",
    ),
    "Biceps" to listOf(
        "Keep the elbows pinned to your sides.",
        "Curl without swinging the torso; take 2–3 seconds to lower.",
        "Squeeze hard at the top and fully straighten the arm at the bottom.",
    ),
    "Triceps" to listOf(
        "Keep the elbows fixed — only the forearms move.",
        "Reach full extension on every rep.",
        "Do not let the elbows flare outward as fatigue builds.",
    ),
    "Lats" to listOf(
        "Start each rep by driving the elbows down toward the hips, not by pulling with the hands.",
        "Keep the chest tall and avoid leaning back too far.",
        "Stretch fully at the top of each rep, pause a beat at the bottom.",
    ),
    "Lower back" to listOf(
        "Brace the core and keep the spine neutral from start to lockout.",
        "Push the floor away and finish the rep by driving the hips forward.",
        "Reset your setup and breath between heavy reps.",
    ),
    "Traps" to listOf(
        "Shrug straight up — do not roll the shoulders.",
        "Hold the top position for a full second.",
        "Let the shoulders travel through a complete range at the bottom.",
    ),
    "Quads" to listOf(
        "Keep the knees tracking over the toes.",
        "Squat with control to at least parallel depth.",
        "Drive through the mid-foot, not the toes.",
    ),
    "Hamstrings" to listOf(
        "Hinge at the hips and keep only a soft knee bend.",
        "Feel a deep stretch at the bottom before pulling back up.",
        "Keep the weight close to the legs throughout.",
    ),
    "Glutes" to listOf(
        "Drive the hips to full lockout and squeeze hard at the top.",
        "Keep the chin tucked and the ribs down.",
        "Push through the heels to keep tension on the glutes.",
    ),
    "Abductors" to listOf(
        "Move slowly and keep the torso upright.",
        "Pause briefly against the resistance at the widest point.",
    ),
    "Adductors" to listOf(
        "Move slowly and under control.",
        "Pause briefly against the resistance at the closest point.",
    ),
    "Calves" to listOf(
        "Pause one full second at the top of each rep.",
        "Lower slowly through a complete range for a deep stretch.",
        "Keep the knees straight to bias the gastrocnemius.",
    ),
    "Abs" to listOf(
        "Exhale and brace as you crunch or raise.",
        "Move with control — no swinging or momentum.",
        "Keep the lower back pressed into the floor where noted.",
    ),
    "Forearms" to listOf(
        "Move only at the wrists; the elbows stay still.",
        "Use a slow tempo and higher reps.",
    ),
)

// ------------------------------------------------------------------
// Step-by-step instructions (Hevy-like). Archetype-level steps keyed
// by movement pattern; every exercise maps to exactly one archetype.
// ------------------------------------------------------------------

private val ARCH_STEPS_BASE: Map<String, List<String>> = mapOf(
    "bench" to listOf(
        "Lie on the bench with eyes under the bar, feet planted firmly on the floor.",
        "Grip the bar slightly wider than shoulder width and unrack it.",
        "Lower the bar to the mid-chest with the elbows at about 75°.",
        "Press the bar up and slightly back until the arms are locked out.",
    ),
    "incline_bench" to listOf(
        "Set the bench to a 30–45° incline and sit back with shoulder blades pinned.",
        "Unrack the weight directly above the upper chest.",
        "Lower to the collarbone line, keeping the elbows tucked.",
        "Press up in a slight arc back to the start position.",
    ),
    "decline_bench" to listOf(
        "Secure your legs at the end of a decline bench and lie back.",
        "Unrack the bar with a shoulder-width grip.",
        "Lower the bar to the lower chest, elbows tucked.",
        "Press up powerfully to full extension.",
    ),
    "fly" to listOf(
        "Set up with a slight elbow bend and keep it fixed for the whole set.",
        "Open the arms in a wide arc until you feel a stretch across the chest.",
        "Squeeze the arms back together as if hugging a barrel.",
        "Pause briefly at peak contraction before the next rep.",
    ),
    "pushup" to listOf(
        "Place the hands slightly wider than the shoulders, body in a straight line.",
        "Brace the core and squeeze the glutes.",
        "Lower until the chest is close to the floor, elbows at about 45°.",
        "Push the floor away to full extension.",
    ),
    "ohp" to listOf(
        "Stand with feet shoulder width, bar resting on the front delts.",
        "Brace the core and squeeze the glutes to lock the ribcage.",
        "Press the bar straight up, moving the head back out of the way.",
        "Lock out overhead with the bar over the mid-foot, then lower under control.",
    ),
    "lateral_raise" to listOf(
        "Stand tall with the weights at your sides and a slight elbow bend.",
        "Lead with the elbows and raise the arms out to the sides.",
        "Stop at shoulder height — no higher.",
        "Lower slowly for a 2–3 second count.",
    ),
    "front_raise" to listOf(
        "Stand tall, weights in front of the thighs.",
        "Raise the arms straight ahead to shoulder height.",
        "Keep the torso still — no swinging.",
        "Lower under control to the start.",
    ),
    "rear_delt" to listOf(
        "Hinge forward with a flat back or sit facing the machine.",
        "With a slight elbow bend, open the arms out wide.",
        "Squeeze the shoulder blades together at the widest point.",
        "Return slowly, keeping tension on the rear delts.",
    ),
    "shrug" to listOf(
        "Stand tall holding the weight with arms fully extended.",
        "Shrug the shoulders straight up toward the ears.",
        "Hold the top position for one full second.",
        "Lower slowly through the complete range of motion.",
    ),
    "upright_row" to listOf(
        "Hold the bar at hip level with a shoulder-width grip.",
        "Pull the bar up along the body, elbows leading.",
        "Stop when the elbows reach shoulder height.",
        "Lower slowly back to the start.",
    ),
    "curl" to listOf(
        "Stand or sit tall with the elbows pinned to your sides.",
        "Curl the weight up without swinging the torso.",
        "Squeeze hard at the top of every rep.",
        "Lower for a 2–3 second count to full extension.",
    ),
    "triceps_pushdown" to listOf(
        "Face the cable station, elbows pinned to your sides.",
        "Push the handle down by extending the elbows only.",
        "Reach full lockout and squeeze the triceps.",
        "Return slowly until the forearms touch the biceps.",
    ),
    "triceps_overhead" to listOf(
        "Hold the weight overhead with both hands, elbows pointing forward.",
        "Lower the weight behind the head by bending only the elbows.",
        "Feel a deep stretch in the triceps at the bottom.",
        "Extend back up to full lockout.",
    ),
    "skullcrusher" to listOf(
        "Lie on a bench holding the weight above the chest.",
        "Bend the elbows to lower the weight toward the forehead.",
        "Keep the upper arms vertical and stationary.",
        "Extend the elbows to press back up.",
    ),
    "dip" to listOf(
        "Support yourself on parallel bars with arms locked.",
        "Lower the body by bending the elbows, leaning slightly forward.",
        "Descend until the shoulders reach elbow height.",
        "Press back up to full extension.",
    ),
    "pulldown" to listOf(
        "Sit with the thighs locked under the pads, chest tall.",
        "Grip the bar wider than the shoulders.",
        "Pull the bar to the upper chest by driving the elbows down.",
        "Let the bar travel all the way back up for a full stretch.",
    ),
    "pull_up" to listOf(
        "Hang from the bar with the chosen grip.",
        "Start the pull by driving the elbows down toward the hips.",
        "Pull until the chin clears the bar, chest to the bar.",
        "Lower all the way down to a dead hang each rep.",
    ),
    "row_bent" to listOf(
        "Hinge to about 45° with a flat back and the weight hanging.",
        "Pull the weight to the lower chest or navel.",
        "Squeeze the shoulder blades at the top.",
        "Lower under control without losing the hip position.",
    ),
    "row_supported" to listOf(
        "Set up with the chest supported on the pad or bench.",
        "Pull the handles toward the lower chest, elbows tight.",
        "Pause and squeeze at full contraction.",
        "Return slowly to a complete stretch.",
    ),
    "straight_arm" to listOf(
        "Stand facing the cable or hold the dumbbell above the chest.",
        "Keep the arms nearly straight throughout.",
        "Pull or lower the weight down in an arc using the lats.",
        "Return slowly to the stretched position.",
    ),
    "deadlift" to listOf(
        "Set up with the bar over mid-foot, hips back and back flat.",
        "Take a big breath and brace the core hard.",
        "Push the floor away, dragging the bar up the shins.",
        "Finish tall by driving the hips forward, then reverse with control.",
    ),
    "rdl" to listOf(
        "Stand tall with the weight held close to the thighs.",
        "Push the hips back, letting the weight slide down the legs.",
        "Descend until you feel a deep hamstring stretch, back flat.",
        "Drive the hips forward to stand back up.",
    ),
    "good_morning" to listOf(
        "Set the bar on the traps as for a squat.",
        "With a slight knee bend, hinge at the hips.",
        "Lower the torso until it is near parallel to the floor.",
        "Drive the hips forward to return upright.",
    ),
    "back_ext" to listOf(
        "Position yourself on the bench with the pad at the hip crease.",
        "Cross the arms over the chest or hold the weight.",
        "Hinge down with a rounded upper back, then raise the torso.",
        "Squeeze the glutes and stop when the body forms a straight line.",
    ),
    "squat" to listOf(
        "Set up with the bar comfortably on the traps or the weight at the chest.",
        "Unrack, take two steps back, feet shoulder width apart.",
        "Sit down between the hips to at least parallel, knees tracking the toes.",
        "Drive through the mid-foot to stand up tall.",
    ),
    "lunge" to listOf(
        "Stand tall holding the weights at the sides.",
        "Step forward or backward into a long split stance.",
        "Lower until both knees reach about 90°.",
        "Push through the front foot to return and alternate legs.",
    ),
    "leg_press" to listOf(
        "Sit with the back and hips flat against the pad, feet shoulder width.",
        "Release the safeties and lower the sled until the knees reach the chest.",
        "Keep the lower back glued to the seat at the bottom.",
        "Press up powerfully without snapping the knees straight.",
    ),
    "leg_ext" to listOf(
        "Sit with the pad resting on the front of the shins.",
        "Extend the knees to lift the weight.",
        "Squeeze the quads hard at full extension.",
        "Lower slowly, resisting on the way down.",
    ),
    "leg_curl" to listOf(
        "Position the pad above the heels or at the calf.",
        "Curl the legs by bending the knees.",
        "Contract the hamstrings fully at the top.",
        "Return slowly to a complete stretch.",
    ),
    "hip_thrust" to listOf(
        "Set the upper back on a bench, bar or weight over the hips.",
        "Push through the heels to lift the hips.",
        "Finish with the torso parallel to the floor, chin tucked.",
        "Squeeze the glutes hard at lockout, lower under control.",
    ),
    "calf" to listOf(
        "Place the balls of the feet on the platform, heels hanging.",
        "Lower the heels for a deep stretch.",
        "Press up onto the toes as high as possible.",
        "Pause one full second at the top.",
    ),
    "abduction" to listOf(
        "Sit or lie with the pads on the outside of the knees.",
        "Keep the torso still and move only at the hips.",
        "Push the legs apart against the resistance.",
        "Return slowly, resisting all the way in.",
    ),
    "adduction" to listOf(
        "Sit or stand with the pad on the inside of the knee.",
        "Keep the torso upright and move with control.",
        "Pull the legs together against the resistance.",
        "Pause briefly, then return slowly.",
    ),
    "crunch" to listOf(
        "Lie on your back with the knees bent and feet flat.",
        "Exhale and curl the ribs toward the hips.",
        "Lift the shoulder blades just off the floor and squeeze.",
        "Lower slowly without dropping the tension.",
    ),
    "sit_up" to listOf(
        "Lie on your back with the knees bent, feet anchored.",
        "Curl the torso up starting with the head.",
        "Come up until the torso is vertical.",
        "Lower with control, one vertebra at a time.",
    ),
    "leg_raise" to listOf(
        "Lie flat or hang from a bar.",
        "Raise the legs by curling the pelvis, not just swinging.",
        "Stop when the legs are vertical or the feet clear the bar.",
        "Lower slowly without touching down between reps.",
    ),
    "plank" to listOf(
        "Support the body on the forearms and toes.",
        "Squeeze the glutes and brace the core.",
        "Keep a straight line from head to heels.",
        "Breathe steadily and hold for the target time.",
    ),
    "twist" to listOf(
        "Sit with the torso upright or leaning back slightly.",
        "Rotate the torso from side to side with control.",
        "Move at the ribcage, not just the arms.",
        "Keep the core braced throughout.",
    ),
    "rollout" to listOf(
        "Kneel with the wheel under the shoulders.",
        "Brace the core and roll the wheel forward.",
        "Extend as far as you can keep the hips from sagging.",
        "Pull back with the abs, not the arms.",
    ),
    "wrist" to listOf(
        "Rest the forearms on a bench, hands hanging off the edge.",
        "Curl the wrists up and down through the full range.",
        "Move slowly with lighter weights.",
        "Keep the forearms pinned in place.",
    ),
    "carry" to listOf(
        "Deadlift the weights and stand tall.",
        "Brace the core, pull the shoulders back.",
        "Walk steady steps for the target distance.",
        "Set the weights down with a flat back.",
    ),
    "pullover" to listOf(
        "Lie on a bench or face the cable station.",
        "Hold the weight above the chest with a slight elbow bend.",
        "Lower the weight in an arc overhead or pull it down.",
        "Return along the same arc, feeling the lats and chest stretch.",
    ),
    "swing" to listOf(
        "Stand over the weight with a flat back, hips hinged.",
        "Hike the weight back between the legs.",
        "Snap the hips forward to swing the weight to chest height.",
        "Let it fall back down and immediately load the next rep.",
    ),

)

private val ARCH_STEPS_EXTRA: Map<String, List<String>> = mapOf(
    "olympic" to listOf(
        "Set up with the bar over mid-foot, hips back, back flat and lats braced.",
        "Explode by driving the legs and extending the hips violently.",
        "Pull yourself under the bar and catch it in the front rack or overhead.",
        "Stand up tall to finish the lift, then reset for the next rep.",
    ),
    "jerk" to listOf(
        "Set the bar on the front delts with the elbows high.",
        "Dip a few centimetres by bending the knees, torso upright.",
        "Drive up explosively, then split or reposition the feet to drop under the bar.",
        "Recover by bringing the feet together with the bar locked overhead.",
    ),
    "thruster" to listOf(
        "Hold the weight at the shoulders or in the front rack.",
        "Squat down until the hips pass the knees.",
        "Drive up through the legs and press the weight overhead in one motion.",
        "Lock out overhead with the bar over the mid-foot, then lower to the rack.",
    ),
    "plyo_jump" to listOf(
        "Stand facing the box or the landing area, feet shoulder width.",
        "Load the hips and swing the arms back.",
        "Jump explosively, driving through the whole foot.",
        "Land softly with bent knees, step down and reset.",
    ),
    "conditioning" to listOf(
        "Set your stance and brace the core.",
        "Perform the movement at a controlled, steady rhythm.",
        "Keep the effort constant for the target time or rounds.",
        "Breathe steadily and pace yourself to the end.",
    ),
    "crawl" to listOf(
        "Get into position on hands and feet, hips low.",
        "Keep the back flat and the core braced.",
        "Move the opposite hand and foot together.",
        "Keep the hips stable as you travel.",
    ),
    "superman_hold" to listOf(
        "Lie face down with the arms extended overhead.",
        "Squeeze the glutes and the lower back.",
        "Lift the arms, chest and legs off the floor.",
        "Hold for the target time, breathing steadily.",
    ),
    "getup" to listOf(
        "Lie down holding the weight locked overhead.",
        "Roll to the elbow, then up to the hand, keeping the arm vertical.",
        "Stand up while the weight stays locked out — move slowly.",
        "Reverse every step back down to the floor with control.",
    ),
    "pallof" to listOf(
        "Stand sideways to the cable with the handle at chest height.",
        "Hold the handle against the sternum and step away to create tension.",
        "Press the handle straight out without rotating the torso.",
        "Resist the pull, return the hands to the chest, repeat.",
    ),
    "bird_dog" to listOf(
        "Start on hands and knees with a flat back.",
        "Extend the opposite arm and leg simultaneously.",
        "Reach long and pause without tilting the hips.",
        "Return under control and switch sides.",
    ),
    "pull_apart" to listOf(
        "Hold the band at shoulder height, arms straight.",
        "Keep a slight tension at the start.",
        "Pull the band apart by squeezing the shoulder blades.",
        "Return slowly, keeping the arms at shoulder height.",
    ),
    "scaption" to listOf(
        "Stand tall with the weights at the sides, thumbs up.",
        "Raise the arms diagonally, about 30° from the body line.",
        "Stop when the hands reach head height, forming a Y.",
        "Lower slowly for a 2-3 second count.",
    ),
    "wood_chop" to listOf(
        "Stand sideways to the cable, hands above one shoulder.",
        "Brace the core and pull the handle across the body.",
        "Finish with the hands outside the opposite hip.",
        "Rotate back to the start with control.",
    ),
)

val ARCH_STEPS: Map<String, List<String>> = ARCH_STEPS_BASE + ARCH_STEPS_EXTRA

/** Map an exercise (canonical EN name) to its instruction archetype. */
fun archetypeOf(name: String): String {
    val n = name.lowercase()
    val muscle = EX[name]?.muscle ?: ""
    return when {
        // --- conditioning / cardio / locomotion (first: avoids row/squat/walking clashes) ---
        n.contains("walking lunge") -> "lunge"
        listOf(
            "burpee", "jumping jack", "high knee", "butt kick", "jump rope", "rowing machine",
            "treadmill", "cycling", "elliptical", "stair climber", "swimming", "ski erg",
            "jacobs ladder", "assault bike", "versa climber", "sled push", "sled drag",
            "battle rope", "shadow box", "speed skater", "lateral bound",
        ).any { n.contains(it) } -> "conditioning"
        n.contains("walking") || n.contains("running") -> "conditioning"
        // --- olympic & explosive lifts ---
        listOf(
            "power clean", "hang clean", "squat clean", "clean pull", "clean and",
            "power snatch", "hang snatch", "snatch pull", "snatch balance", "kettlebell snatch",
        ).any { n.contains(it) } -> "olympic"
        n.contains("jerk") -> "jerk"
        n.contains("thruster") -> "thruster"
        n.contains("box jump") || n.contains("broad jump") || n.contains("depth jump") ||
            n.contains("frog jump") -> "plyo_jump"
        // --- specialty patterns ---
        n.contains("get up") -> "getup"
        n.contains("pallof") -> "pallof"
        n.contains("bird dog") -> "bird_dog"
        n.contains("bear crawl") || n.contains("crab walk") -> "crawl"
        n.contains("superman") || n.contains("snow angel") -> "superman_hold"
        n.contains("pull apart") -> "pull_apart"
        n.contains("wood chop") -> "wood_chop"
        n.contains("scaption") || n.contains("y raise") || n.contains("w raise") -> "scaption"
        n.contains("handstand") -> "ohp"
        n.contains("hip extension") -> "back_ext"
        n == "reverse nordic" -> "squat"
        // --- chest ---
        n.contains("incline") && (n.contains("press") || n.contains("bench")) -> "incline_bench"
        n.contains("decline") && (n.contains("press") || n.contains("bench")) -> "decline_bench"
        (n.contains("bench press") && !n.contains("close grip")) || n == "machine chest press" ||
            n.contains("floor press") || n == "svend press" || n.contains("guillotine") -> "bench"
        n.contains("fly") || n.contains("crossover") || n.contains("pec deck") || n.contains("svend") -> "fly"
        n.contains("push up") || n.contains("pushup") -> "pushup"
        n.contains("dip") && muscle == "Chest" -> "dip"
        // --- shoulders ---
        n.contains("shrug") || n.contains("rack pull") || n.contains("block pull") -> "shrug"
        n.contains("lateral raise") -> "lateral_raise"
        n.contains("front raise") -> "front_raise"
        n.contains("rear delt") || n.contains("face pull") || n.contains("reverse pec deck") -> "rear_delt"
        n.contains("upright row") || n.contains("high pull") -> "upright_row"
        n.contains("press") && muscle == "Shoulders" -> "ohp"
        // --- arms ---
        n.contains("wrist") || n.contains("pinch") || n.contains("rice bucket") ||
            n.contains("towel wring") || n.contains("figure 8") || n.contains("hex dumbbell") -> "wrist"
        n.contains("curl") -> "curl"
        (n.contains("pushdown") || n.contains("kickback")) && muscle == "Triceps" -> "triceps_pushdown"
        n.contains("extension") && muscle == "Triceps" -> "triceps_overhead"
        n.contains("skullcrusher") || n.contains("tate") || n.contains("jm press") -> "skullcrusher"
        n.contains("close grip bench") -> "skullcrusher"
        n.contains("dip") -> "dip"
        // --- back ---
        n.contains("straight arm") -> "straight_arm"
        n.contains("pulldown") -> "pulldown"
        n.contains("pull up") || n.contains("pullup") || n.contains("chin up") ||
            n.contains("muscle up") || n.contains("scap pull") -> "pull_up"
        n.contains("pullover") -> "pullover"
        n.contains("inverted row") -> "pull_up"
        n.contains("cable row") || n.contains("supported row") || n.contains("machine row") ||
            n.contains("t-bar") || n.contains("incline dumbbell row") || n.contains("seal row") ||
            n.contains("meadow row") -> "row_supported"
        n.contains("row") -> "row_bent"
        // --- posterior chain ---
        n.contains("deadlift") && (n.contains("romanian") || n.contains("single leg")) -> "rdl"
        n.contains("deadlift") -> "deadlift"
        n.contains("good morning") -> "good_morning"
        (n.contains("extension") && muscle == "Lower back") || n.contains("hyperextension") -> "back_ext"
        n.contains("swing") -> "swing"
        n.contains("hip thrust") || n.contains("glute bridge") || n.contains("frog pump") ||
            n.contains("pull through") || n.contains("kickback") -> "hip_thrust"
        n.contains("leg curl") || n.contains("nordic") || n.contains("glute ham") -> "leg_curl"
        // --- legs ---
        n.contains("calf") || n.contains("tibialis") || n.contains("on toes") -> "calf"
        n.contains("squat") -> "squat"
        n.contains("lunge") || n.contains("split squat") || n.contains("step up") ||
            n.contains("curtsy") || n.contains("step down") -> "lunge"
        n.contains("leg press") -> "leg_press"
        n.contains("leg extension") || n.contains("knee extension") -> "leg_ext"
        n.contains("abduction") || n.contains("clamshell") || n.contains("fire hydrant") ||
            n.contains("lateral walk") || n.contains("monster walk") -> "abduction"
        n.contains("adduction") -> "adduction"
        // --- abs ---
        n.contains("plank") || n.contains("hollow") || n.contains("dead bug") || n.contains("wall sit") ||
            n.contains("mountain") || n.contains("body saw") || n.contains("arch hold") ||
            n.contains("v-sit") || n.contains("l-sit") || n.contains("l-hang") || n.contains("vacuum") -> "plank"
        n.contains("sit up") -> "sit_up"
        n.contains("crunch") || n.contains("cocoon") || n.contains("ball pass") -> "crunch"
        n.contains("twist") || n.contains("windshield") -> "twist"
        n.contains("leg raise") || n.contains("knee raise") || n.contains("v-up") ||
            n.contains("flutter") || n.contains("scissor") || n.contains("dragon flag") ||
            n.contains("toe to bar") || n.contains("captain") || n.contains("jackknife") ||
            n.contains("leg lower") -> "leg_raise"
        n.contains("rollout") || n.contains("fallout") -> "rollout"
        n.contains("side bend") -> "twist"
        // --- forearms ---
        n.contains("carry") || n.contains("dead hang") -> "carry"
        else -> when (muscle) {
            "Cardio" -> "conditioning"
            "Abs" -> "crunch"
            else -> "bench"
        }
    }
}

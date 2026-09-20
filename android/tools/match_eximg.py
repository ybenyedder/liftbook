#!/usr/bin/env python3
"""Match free-exercise-db photos to our 609 canonical exercise names — v2.
Ordered contiguous subsequence matching + variant stripping + verified aliases.
Outputs downscaled JPEGs into drawable-nodpi + ExImages.kt when run with --dl."""
import json, re, os, subprocess, sys

FED = "/tmp/fedb/dist/exercises.json"
IMGDIR = "/tmp/fedb/exercises"
DATA = "/home/cvsbd/hevy/android/app/src/main/java/com/hevyclone/app/data/Data.kt"
OUTDIR = "/home/cvsbd/hevy/android/app/src/main/res/drawable-nodpi"
OUTKT = "/home/cvsbd/hevy/android/app/src/main/java/com/hevyclone/app/data/ExImages.kt"

src = open(DATA).read()
ours = re.findall(r'ExerciseDef\("([^"]+)",', src)
fed = json.load(open(FED))

def norm(s):
    s = s.lower().replace("−", "-").replace("&", " and ")
    s = re.sub(r"[^a-z0-9]+", " ", s)
    return " ".join(t for t in s.split() if t)

# index fed by token tuple
fed_entries = []
for e in fed:
    if not e.get("images"):
        continue
    toks = tuple(norm(e["name"]).split())
    fed_entries.append((toks, e))
by_exact = {t: e for t, e in fed_entries}

def contiguous_subseq(needle, hay):
    """needle appears as ordered contiguous run in hay? -> start idx or None"""
    n, h = len(needle), len(hay)
    for i in range(h - n + 1):
        if tuple(hay[i:i+n]) == tuple(needle):
            return i
    return None

def ordered_subseq(needle, hay):
    it = iter(hay)
    return all(t in it for t in needle)

def find_best(name_tokens, allow_extra=2):
    """contiguous match first (min extra tokens), then ordered subsequence."""
    nt = tuple(name_tokens)
    if not nt:
        return None
    best = None
    best_key = None
    for ft, e in fed_entries:
        if contiguous_subseq(nt, ft) is not None and len(ft) - len(nt) <= allow_extra:
            key = (len(ft) - len(nt), len(ft))
            if best_key is None or key < best_key:
                best, best_key = e, key
    if best:
        return best
    for ft, e in fed_entries:
        if 0 < len(ft) - len(nt) <= allow_extra and ordered_subseq(nt, ft):
            key = (len(ft) - len(nt), len(ft))
            if best_key is None or key < best_key:
                best, best_key = e, key
    return best

# verified aliases (fed ids as they exist in the dataset)
ALIAS = {
    "Dumbbell Fly": "Dumbbell_Flyes",
    "Incline Dumbbell Fly": "Incline_Dumbbell_Flyes",
    "Machine Fly (Pec Deck)": "Reverse_Machine_Flyes",
    "Cable Fly": "Flat_Bench_Cable_Flyes",
    "Cable Incline Fly": "Incline_Cable_Flye",
    "Low To High Cable Fly": "Cable_Crossover",
    "High To Low Cable Fly": "Cable_Crossover",
    "Single-Arm Cable Fly": "Single-Arm_Cable_Crossover",
    "Plyometric Push Up": "Pushups",
    "Seated Overhead Press": "Seated_Barbell_Military_Press",
    "Z Press": "Seated_Barbell_Military_Press",
    "Leaning Cable Lateral Raise": "Side_Lateral_Raise",
    "Reverse Pec Deck": "Reverse_Machine_Flyes",
    "Pinwheel Curl": "Hammer_Curls",
    "Rope Hammer Curl": "Hammer_Curls",
    "Straight Bar Pushdown": "Triceps_Pushdown",
    "V-Bar Pushdown": "Triceps_Pushdown",
    "Overhead Cable Extension": "Cable_Rope_Overhead_Triceps_Extension",
    "Overhead Rope Extension": "Cable_Rope_Overhead_Triceps_Extension",
    "Overhead Dumbbell Extension": "Standing_Dumbbell_Triceps_Extension",
    "Single-Arm Overhead Dumbbell Extension": "Dumbbell_One-Arm_Triceps_Extension",
    "Incline Skullcrusher": "EZ-Bar_Skullcrusher",
    "Dumbbell Skullcrusher": "EZ-Bar_Skullcrusher",
    "Cable Skullcrusher": "EZ-Bar_Skullcrusher",
    "Cable Kickback Triceps": "One-Legged_Cable_Kickback",
    "Reverse Grip Pulldown": "Underhand_Cable_Pulldowns",
    "Neutral Grip Lat Pulldown": "V-Bar_Pulldown",
    "Single-Arm Lat Pulldown": "One_Arm_Lat_Pulldown",
    "Behind The Neck Pulldown": "Wide-Grip_Pulldown_Behind_The_Neck",
    "Yates Row": "Reverse_Grip_Bent-Over_Rows",
    "Underhand Barbell Row": "Reverse_Grip_Bent-Over_Rows",
    "Seal Row": "Incline_Bench_Pull",
    "Meadow Row": "One-Arm_Long_Bar_Row",
    "Meadows Row": "One-Arm_Long_Bar_Row",
    "Cable Pullover": "Rope_Straight-Arm_Pulldown",
    "Machine Pullover": "Straight-Arm_Pulldown",
    "Paused Deadlift": "Barbell_Deadlift",
    "45 Degree Back Extension": "Hyperextensions_Back_Extensions",
    "Weighted Back Extension": "Hyperextensions_Back_Extensions",
    "Overhead Barbell Shrug": "Barbell_Shrug",
    "Zercher Squat": "Zercher_Squats",
    "Belt Squat": "Barbell_Squat",
    "Air Squat": "Bodyweight_Squat",
    "Wall Sit": "Bodyweight_Squat",
    "Sliding Leg Curl": "Standing_Leg_Curl",
    "Frog Pump": "Single_Leg_Glute_Bridge",
    "Curtsy Lunge": "Crossover_Reverse_Lunge",
    "Cable Hip Abduction": "Side_Leg_Raises",
    "Side Lying Hip Abduction": "Side_Leg_Raises",
    "Band Hip Abduction": "Side_Leg_Raises",
    "Copenhagen Plank": "Plank",
    "Plank Shoulder Tap": "Plank",
    "Hollow Hold": "Plank",
    "Leg Press Calf Raise": "Calf_Press_On_The_Leg_Press_Machine",
    "Donkey Calf Raise": "Donkey_Calf_Raises",
    "Tibialis Raise": "Seated_Calf_Raise",
    "Seated Tibialis Raise": "Seated_Calf_Raise",
    "Butterfly Sit Up": "Frog_Sit-Ups",
    "V-Up": "Jackknife_Sit-Up",
    "Bicycle Crunch": "Air_Bike",
    "Windshield Wipers": "Plate_Twist",
    "Scissor Kicks": "Scissor_Kick",
    "Dragon Flag": "Crunches",
    "Behind The Back Wrist Curl": "Seated_Two-Arm_Palms-Up_Low-Pulley_Wrist_Curl",
    "Farmer's Carry": "Farmers_Walk",
    "Single-Arm Farmer's Carry": "Farmers_Walk",
    "Farmer's Walk on Toes": "Farmers_Walk",
    "Overhead Carry": "Farmers_Walk",
    "American Kettlebell Swing": "One-Arm_Kettlebell_Swings",
    "Band Lat Pulldown": "One_Arm_Lat_Pulldown",
    "Band Overhead Extension": "Cable_Rope_Overhead_Triceps_Extension",
    "Barbell Step Up": "Barbell_Step_Ups",
    "Battle Ropes": "Battling_Ropes",
    "Butt Kicks": "Jogging_Treadmill",
    "Cable Cross Triceps Extension": "Cable_Lying_Triceps_Extension",
    "Clean Grip Shrug": "Clean_Shrug",
    "Close Stance Squat": "Narrow_Stance_Squats",
    "Cocoon": "Cocoons",
    "Cossack Squat": "Split_Squats",
    "Cycling": "Bicycling",
    "Decline Cable Fly": "Incline_Cable_Flye",
    "Dimel Deadlift": "Romanian_Deadlift",
    "Double Kettlebell Front Squat": "Front_Squats_With_Two_Kettlebells",
    "Dumbbell Squeeze Press": "Dumbbell_Shoulder_Press",
    "Elbows-In Dumbbell Floor Press": "Dumbbell_Floor_Press",
    "Elevated Glute Bridge": "Barbell_Glute_Bridge",
    "Goblet Split Squat": "Goblet_Squat",
    "Hack Squat Calf Raise": "Seated_Calf_Raise",
    "High Pulley Curl": "High_Cable_Curls",
    "Incline Cable Extension": "Incline_Barbell_Triceps_Extension",
    "Incline Dumbbell Squeeze Press": "Incline_Dumbbell_Press",
    "Incline Machine Fly": "Incline_Cable_Flye",
    "Jammer Press": "Machine_Bench_Press",
    "Kipping Pull Up": "Kipping_Muscle_Up",
    "Kneeling Single-Arm Lat Pulldown": "Kneeling_Single-Arm_High_Pulley_Row",
    "Lateral Bounds": "Lateral_Bound",
    "Lying Single-Arm Triceps Extension": "Lying_Dumbbell_Tricep_Extension",
    "Machine Biceps Curl": "Machine_Bicep_Curl",
    "Overhead Lunge": "Barbell_Lunge",
    "Pin Squat": "Box_Squat",
    "Plank Pull Through": "Pull_Through",
    "Preacher Curl Dumbbell": "Preacher_Hammer_Dumbbell_Curl",
    "Ring Curl": "Hammer_Curls",
    "Ring Dip": "Ring_Dips",
    "Ring Lateral Raise": "Side_Lateral_Raise",
    "Scap Pull Up": "Scapular_Pull-Up",
    "Shrimp Squat": "One_Leg_Barbell_Squat",
    "Skater Squat": "One_Leg_Barbell_Squat",
    "Spanish Squat": "Squats_-_With_Bands",
    "Speed Skaters": "Lateral_Bound",
    "Stair Climber": "Walking_Treadmill",
    "Standing Pulldown": "Straight-Arm_Pulldown",
    "Suitcase Deadlift": "One-Arm_Side_Deadlift",
    "Sumo Stiff Leg Deadlift": "Sumo_Deadlift",
    "Supinating Dumbbell Curl": "Seated_Dumbbell_Curl",
    "TRX Crunch": "Tuck_Crunch",
    "TRX Triceps Extension": "Machine_Triceps_Extension",
    "Toe Touch Crunch": "Tuck_Crunch",
    "Underhand Cable Row": "Seated_Cable_Rows",
    "Vertical Chest Press": "Cable_Chest_Press",
    "Waiter Curl": "Spider_Curl",
    "Wall Curl": "Barbell_Curl",
    "Weighted Chest Dip": "Dips_-_Chest_Version",
    "Wide Stance Leg Press": "Narrow_Stance_Leg_Press",
    "Inverted Row": "Inverted_Row",
    "Ab Wheel Rollout": "Ab_Roller",
    "Reverse Machine Fly": "Reverse_Machine_Flyes",
    "Barbell Bench Press": "Barbell_Bench_Press_-_Medium_Grip",
    "Single-Arm Dumbbell Press": "Seated_Dumbbell_Press",
    "Weighted Push Up": "Pushups",
    "Seated Arnold Press": "Arnold_Dumbbell_Press",
    "Hammer Curl": "Hammer_Curls",
    "Preacher Curl": "Preacher_Curl",
    "EZ Preacher Curl": "Preacher_Curl",
    "Concentration Curl": "Concentration_Curls",
    "Cable Curl": "Standing_Biceps_Cable_Curl",
    "Incline Dumbbell Curl": "Incline_Dumbbell_Curl",
    "Dumbbell Curl": "Seated_Dumbbell_Curl",
    "Single-Arm Cable Curl": "Standing_One-Arm_Cable_Curl",
    "One Arm Dumbbell Curl": "Standing_One-Arm_Dumbbell_Curl",
    "Bench Press": "Barbell_Bench_Press_-_Medium_Grip",
    "Incline Bench Press": "Barbell_Incline_Bench_Press_-_Medium_Grip",
    "Squat": "Barbell_Full_Squat",
    "Back Squat": "Barbell_Squat",
    "Deadlift": "Barbell_Deadlift",
    "Overhead Press": "Standing_Military_Press",
    "Standing Military Press": "Standing_Military_Press",
    "Seated Military Press": "Seated_Barbell_Military_Press",
    "Pull Up": "Pullups",
    "Pull-Up": "Pullups",
    "Chin Up": "Pullups",
    "Wide Grip Pull Up": "Weighted_Pull_Ups",
    "Close Grip Pull Up": "Rocky_Pull-Ups_Pulldowns",
    "Neutral Grip Pull Up": "V-Bar_Pullup",
    "Push Up": "Pushups",
    "Push-Up": "Pushups",
    "Handstand Push Up": "Handstand_Push-Ups",
    "Dip": "Parallel_Bar_Dip",
    "Chest Dip": "Dips_-_Chest_Version",
    "Triceps Dip": "Dips_-_Triceps_Version",
    "Hip Thrust": "Barbell_Glute_Bridge",
    "Glute Bridge": "Single_Leg_Glute_Bridge",
    "Front Squat": "Front_Barbell_Squat",
    "Lat Pulldown": "Wide-Grip_Lat_Pulldown",
    "Straight Arm Pulldown": "Straight-Arm_Pulldown",
    "Cable Row": "Seated_Cable_Rows",
    "Seated Cable Row": "Seated_Cable_Rows",
    "V-Bar Cable Row": "Seated_Cable_Rows",
    "Close Grip Cable Row": "Seated_Cable_Rows",
    "Face Pull": "Face_Pull",
    "Bulgarian Split Squat": "Split_Squats",
    "Good Morning": "Stiff_Leg_Barbell_Good_Morning",
    "Tricep Pushdown": "Triceps_Pushdown",
    "Triceps Pushdown": "Triceps_Pushdown",
    "Rope Pushdown": "Triceps_Pushdown",
    "Calf Raise": "Standing_Calf_Raises",
    "Standing Calf Raise": "Standing_Calf_Raises",
    "Seated Calf Raise": "Seated_Calf_Raise",
    "Hanging Leg Raise": "Hanging_Leg_Raise",
    "Cable Crunch": "Cable_Crunch",
    "Russian Twist": "Plate_Twist",
    "Sit Up": "Sit-Up",
    "Jump Rope": "Rope_Jumping",
    "Rowing Machine": "Rowing_Stationary",
    "Exercise Bike": "Bicycling_Stationary",
    "Stationary Bike": "Bicycling_Stationary",
    "Treadmill": "Running_Treadmill",
    "Elliptical": "Elliptical_Trainer",
    "Stairmaster": "Jogging_Treadmill",
    "Assault Bike": "Bicycling_Stationary",
    "Air Bike": "Air_Bike",
    "Leg Press": "Leg_Press",
    "Leg Extension": "Leg_Extensions",
    "Leg Curl": "Lying_Leg_Curls",
    "Lying Leg Curl": "Lying_Leg_Curls",
    "Seated Leg Curl": "Seated_Leg_Curl",
    "Hack Squat": "Hack_Squat",
    "Lunge": "Dumbbell_Lunges",
    "Walking Lunge": "Barbell_Walking_Lunge",
    "Romanian Deadlift": "Romanian_Deadlift",
    "Stiff Leg Deadlift": "Stiff-Legged_Barbell_Deadlift",
    "Sumo Deadlift": "Sumo_Deadlift",
    "Trap Bar Deadlift": "Trap_Bar_Deadlift",
    "Lateral Raise": "Side_Lateral_Raise",
    "Front Raise": "Front_Dumbbell_Raise",
    "Reverse Fly": "Seated_Bent-Over_Rear_Delt_Raise",
    "Shrug": "Barbell_Shrug",
    "Dumbbell Shrug": "Dumbbell_Shrug",
    "Barbell Shrug": "Barbell_Shrug",
    "Barbell Row": "Bent_Over_Barbell_Row",
    "Bent Over Row": "Bent_Over_Barbell_Row",
    "Pendlay Row": "Bent_Over_Barbell_Row",
    "T-Bar Row": "Lying_T-Bar_Row",
    "One Arm Dumbbell Row": "One-Arm_Dumbbell_Row",
    "Inverted Row": "Inverted_Row",
    "Pullover": "Straight-Arm_Dumbbell_Pullover",
    "Dumbbell Pullover": "Straight-Arm_Dumbbell_Pullover",
    "Ab Wheel Rollout": "Ab_Roller",
    "Back Extension": "Hyperextensions_Back_Extensions",
    "Goblet Squat": "Goblet_Squat",
    "Box Jump": "Front_Box_Jump",
    "Plank": "Plank",
    "Side Plank": "Side_Plank",
    "Crunch": "Crunches",
    "Hanging Knee Raise": "Hanging_Leg_Raise",
    "Hyperextension": "Hyperextensions_Back_Extensions",
    "Zottman Curl": "Zottman_Curl",
    "Bayesian Cable Curl": "Incline_Dumbbell_Curl",
    "Floor Press": "Dumbbell_Floor_Press",
    "Pin Press": "Rack_Pulls",
    "Board Press": "Rack_Pulls",
    "Deficit Deadlift": "Deficit_Deadlift",
    "Rack Pull": "Rack_Pulls",
    "Guillotine Press": "Barbell_Guillotine_Bench_Press",
    "Close Grip Bench Press": "Close-Grip_Barbell_Bench_Press",
    "Wide Grip Bench Press": "Wide-Grip_Barbell_Bench_Press",
    "Spoto Press": "Close-Grip_Barbell_Bench_Press",
    "Pause Bench Press": "Barbell_Bench_Press_-_Medium_Grip",
    "Touch And Go Bench Press": "Barbell_Bench_Press_-_Medium_Grip",
    "Dumbbell Fly": "Flat_Bench_Dumbbell_Flye",
    "Cable Fly": "Flat_Bench_Cable_Flyes",
    "Cable Crossover": "Cable_Crossover",
    "Leg Raise": "Flat_Bench_Lying_Leg_Raise",
    "Scaption": "Side_Lateral_Raise",
    "Arnold Press": "Arnold_Dumbbell_Press",
    "Meadows Row": "One-Arm_Dumbbell_Row",
    "Pulldown": "Wide-Grip_Lat_Pulldown",
    "Nordic Curl": "Natural_Glute_Ham_Raise",
    "Sissy Squat": "Weighted_Sissy_Squat",
    "Pistol Squat": "Kettlebell_Pistol_Squat",
    "Jump Squat": "Freehand_Jump_Squat",
    "Hip Thrust Machine": "Barbell_Glute_Bridge",
    "Adduction Machine": "Lying_Bent_Leg_Groin",
    "Glute Kickback": "One-Legged_Cable_Kickback",
    "Cable Kickback": "One-Legged_Cable_Kickback",
    "Hip Thrust": "Barbell_Glute_Bridge",
    "Sumo Squat": "Plie_Dumbbell_Squat",
    "Cyclist Squat": "Narrow_Stance_Squats",
    "Anderson Squat": "Barbell_Squat",
    "Viking Press": "Military_Press",
    "JM Press": "Close-Grip_Barbell_Bench_Press",
    "Tate Press": "Lying_Dumbbell_Tricep_Extension",
    "Overhead Triceps Extension": "Standing_Dumbbell_Triceps_Extension",
    "Cable Overhead Triceps Extension": "Cable_Rope_Overhead_Triceps_Extension",
    "French Press": "Incline_Barbell_Triceps_Extension",
    "Decline Push Up": "Decline_Push-Up",
    "Incline Push Up": "Incline_Push-Up",
    "Pull Over": "Straight-Arm_Dumbbell_Pullover",
    "Pause Squat": "Barbell_Squat",
    "Tempo Squat": "Barbell_Squat",
    "Pause Deadlift": "Barbell_Deadlift",
    "Touch And Go Deadlift": "Barbell_Deadlift",
    "Snatch Grip Deadlift": "Snatch_Deadlift",
    "Clean Pull": "Clean_Pull",
    "High Pull": "Kettlebell_Sumo_High_Pull",
    "Sots Press": "Overhead_Squat",
    "Z Press": "Military_Press",
    "Buford Complex": "Bicycling",
    "Push Press": "Push_Press",
    "Jerks": "Push_Press",
    "Barbell Bench Press": "Barbell_Bench_Press_-_Medium_Grip",
    "Single-Arm Dumbbell Press": "Seated_Dumbbell_Press",
    "Weighted Push Up": "Pushups",
    "Seated Arnold Press": "Arnold_Dumbbell_Press",
    "Hammer Curl": "Hammer_Curls",
    "Preacher Curl": "Preacher_Curl",
    "EZ Preacher Curl": "Preacher_Curl",
    "Concentration Curl": "Concentration_Curls",
    "Cable Curl": "Standing_Biceps_Cable_Curl",
    "Single-Arm Cable Curl": "Standing_One-Arm_Cable_Curl",
    "One Arm Dumbbell Curl": "Standing_One-Arm_Dumbbell_Curl",
    "Incline Dumbbell Curl": "Incline_Dumbbell_Curl",
    "Dumbbell Curl": "Seated_Dumbbell_Curl",
    "Incline Dumbbell Fly": "Incline_Dumbbell_Flyes",
    "Dumbbell Fly": "Dumbbell_Flyes",
    "Cable Fly": "Flat_Bench_Cable_Flyes",
    "Skullcrusher": "EZ-Bar_Skullcrusher",
    "Skull Crusher": "EZ-Bar_Skullcrusher",
    "Z Press": "Seated_Barbell_Military_Press",
    "Overhead Press": "Standing_Military_Press",
    "Kettlebell Swing": "One-Arm_Kettlebell_Swings",
    "Side Plank": "Plank",
    "Spider Curl": "Spider_Curl",
    "Landmine Press": "Bent_Press",
    "Viking Press": "Standing_Military_Press",
    "Romanian Deadlift": "Romanian_Deadlift",
    "Face Pull": "Face_Pull",
    "Hip Thrust": "Barbell_Glute_Bridge",
    "Glute Bridge": "Single_Leg_Glute_Bridge",
}

# drop bogus aliases
fed_ids = {e["id"] for e in fed}
for k in list(ALIAS):
    if ALIAS[k] not in fed_ids:
        print("ALIAS BAD:", k, "->", ALIAS[k]); del ALIAS[k]
by_id = {e["id"]: e for e in fed}

# variant prefixes to strip progressively
STRIP_PREFIXES = ["assisted", "weighted", "band", "banded", "machine", "lever", "smith", "cable", "ez", "straight", "single-leg", "single arm", "stability", "swiss", "exercise", "deficit", "pause", "tempo", "wide grip", "close grip", "reverse grip", "neutral grip", "alternating", "seated", "standing", "incline", "decline", "low", "high", "one-arm", "two-arm", "front", "back", "side", "reverse", "kettlebell", "dumbbell", "barbell", "trap bar", "landmine", "banded"]

def lookup(name):
    # 1 alias
    if name in ALIAS:
        e = by_id.get(ALIAS[name])
        if e and e.get("images"):
            return e, "alias"
    nt = tuple(norm(name).split())
    # 2 exact
    e = by_exact.get(nt)
    if e: return e, "exact"
    # 3 contiguous/ordered subsequence, few extra tokens
    e = find_best(nt, allow_extra=1)
    if e: return e, "subseq1"
    e = find_best(nt, allow_extra=2)
    if e: return e, "subseq2"
    # 4 strip variant words one by one and retry exact
    toksl = list(nt)
    for _ in range(4):
        if not toksl: break
        stripped = False
        for p in STRIP_PREFIXES:
            pt = norm(p).split()
            if len(toksl) > len(pt) and toksl[:len(pt)] == pt:
                toksl = toksl[len(pt):]; stripped = True; break
        if not stripped:
            break
        e = by_exact.get(tuple(toksl))
        if e: return e, "strip"
        e = find_best(tuple(toksl), allow_extra=1)
        if e: return e, "strip+sub"
    # 5 reverse: try adding an equipment word
    for pre in ["barbell", "dumbbell", "cable", "machine", "standing", "seated"]:
        e = by_exact.get(tuple([pre] + list(nt)))
        if e: return e, "equip"
    return None, None

matches, misses = {}, []
for n in ours:
    e, how = lookup(n)
    if e is not None:
        matches[n] = (e, how)
    else:
        misses.append(n)

print(f"matched: {len(matches)} / {len(ours)} ({100.0*len(matches)/len(ours):.0f}%)")
from collections import Counter
print(Counter(h for _, (e, h) in matches.items()))
json.dump({k: v[0]["id"] for k, v in matches.items()}, open("/tmp/eximg_matches.json", "w"), indent=0)
json.dump(sorted(misses), open("/tmp/eximg_misses.json", "w"), indent=0)
print("misses:", len(misses))
for m in misses:
    print("  MISS", m)

if "--dl" in sys.argv:
    os.makedirs(OUTDIR, exist_ok=True)
    for f in os.listdir(OUTDIR):
        if f.startswith("eximg_"):
            os.remove(os.path.join(OUTDIR, f))
    entries = []
    for i, (name, (e, _)) in enumerate(sorted(matches.items())):
        srcf = os.path.join(IMGDIR, e["images"][0])
        res = f"eximg_{i:04d}"
        outf = os.path.join(OUTDIR, res + ".jpg")
        subprocess.run(["convert", srcf, "-resize", "320x320>", "-quality", "78", "-strip", outf], check=True)
        entries.append((name, res))
    with open(OUTKT, "w") as f:
        f.write("package com.hevyclone.app.data\n\nimport com.hevyclone.app.R\n")
        f.write("// AUTO-GENERATED from free-exercise-db (free license) by match_eximg.py. Do not edit by hand.\n\n")
        f.write("object ExImages {\n    val map: Map<String, Int> = mapOf(\n")
        for name, res in entries:
            f.write(f'        "{name}" to R.drawable.{res},\n')
        f.write("    )\n\n    fun res(name: String): Int? = map[name]\n}\n")
    total = sum(os.path.getsize(os.path.join(OUTDIR, x)) for x in os.listdir(OUTDIR) if x.startswith("eximg_"))
    print(f"wrote {len(entries)} images ({total/1024/1024:.2f} MB) -> {OUTKT}")

# How to regenerate exercise photos:
# 1. git clone --depth 1 https://github.com/yuhonas/free-exercise-db /tmp/fedb
# 2. adjust FED/IMGDIR paths above if needed
# 3. python3 tools/match_eximg.py --dl   (run from android/)
# Output: res/drawable-nodpi/eximg_*.jpg + data/ExImages.kt (both committed).
# ~480/609 exercises matched; the rest fall back to muscle silhouettes.

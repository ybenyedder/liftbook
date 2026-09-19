#!/usr/bin/env python3
"""Generate original anatomical pictogram VectorDrawables (static + animated).

Front and back body silhouettes built from scratch; each muscle group drawable
highlights its target muscles in accent blue. Output: res/drawable/ill_*.xml
"""
import os

OUT = "app/src/main/res/drawable"
ACCENT = "#028CFD"
BODY = "#DDE4EC"
DIM = "#C7CFD9"

# ---------------- FRONT VIEW ----------------
FRONT_BASE = {
    "head":  "M100,11 a13,13 0 1,0 0.01,0 Z",
    "neck":  "M93,37 h14 v9 h-14 Z",
    "pelvis":"M79,148 h42 l-7,22 h-28 Z",
    "hip_l": "M66,150 l14,-2 l-2,16 l-14,-2 Z",
    "hip_r": "M134,150 l-14,-2 l2,16 l14,-2 Z",
}
FRONT_MUSCLES = {
    "traps_f":   ["M79,46 q21,-7 42,0 l-8,9 q-13,-5 -26,0 Z"],
    "delts_l":   ["M62,50 a13,14 0 1,0 0.1,0 Z"],
    "delts_r":   ["M138,50 a13,14 0 1,0 0.1,0 Z"],
    "chest_l":   ["M67,64 Q85,57 97,61 L97,93 Q79,96 69,87 Q63,75 67,64 Z"],
    "chest_r":   ["M133,64 Q115,57 103,61 L103,93 Q121,96 131,87 Q137,75 133,64 Z"],
    "abs":       ["M83,96 h34 v44 q-17,9 -34,0 Z"],
    "obliq_l":   ["M69,90 Q77,95 81,96 L81,144 Q70,138 67,116 Z"],
    "obliq_r":   ["M131,90 Q123,95 119,96 L119,144 Q130,138 133,116 Z"],
    "biceps_l":  ["M52,64 Q42,80 45,108 L60,105 Q60,84 64,68 Z"],
    "biceps_r":  ["M148,64 Q158,80 155,108 L140,105 Q140,84 136,68 Z"],
    "forearm_l": ["M46,110 Q40,132 44,148 L56,146 Q54,128 59,110 Z"],
    "forearm_r": ["M154,110 Q160,132 156,148 L144,146 Q146,128 141,110 Z"],
    "quads_l":   ["M70,162 Q66,190 72,212 L92,210 Q94,186 92,162 Z"],
    "quads_r":   ["M130,162 Q134,190 128,212 L108,210 Q106,186 108,162 Z"],
    "calf_l":    ["M72,216 Q68,234 72,248 L88,248 Q90,232 90,216 Z"],
    "calf_r":    ["M128,216 Q132,234 128,248 L112,248 Q110,232 110,216 Z"],
}
FRONT_ARMS_ORDER = ["biceps_l", "forearm_l", "biceps_r", "forearm_r"]

# ---------------- BACK VIEW ----------------
BACK_BASE = {
    "head":  "M100,11 a13,13 0 1,0 0.01,0 Z",
    "neck":  "M93,37 h14 v9 h-14 Z",
    "hip_l": "M66,150 l14,-2 l-2,16 l-14,-2 Z",
    "hip_r": "M134,150 l-14,-2 l2,16 l14,-2 Z",
}
BACK_MUSCLES = {
    "traps":     ["M100,42 L131,64 Q112,80 100,78 Q88,80 69,64 Z"],
    "delts_l":   ["M62,50 a13,14 0 1,0 0.1,0 Z"],
    "delts_r":   ["M138,50 a13,14 0 1,0 0.1,0 Z"],
    "lats_l":    ["M70,66 Q94,72 96,102 Q94,124 78,114 Q64,92 70,66 Z"],
    "lats_r":    ["M130,66 Q106,72 104,102 Q106,124 122,114 Q136,92 130,66 Z"],
    "lower_back":["M84,112 h32 v28 q-16,8 -32,0 Z"],
    "glute_l":   ["M72,146 a17,16 0 1,0 0.1,0 Z"],
    "glute_r":   ["M128,146 a17,16 0 1,0 0.1,0 Z"],
    "hams_l":    ["M70,164 Q66,192 72,212 L92,210 Q94,186 92,164 Z"],
    "hams_r":    ["M130,164 Q134,192 128,212 L108,210 Q106,186 108,164 Z"],
    "calf_l":    ["M72,216 Q68,234 72,248 L88,248 Q90,232 90,216 Z"],
    "calf_r":    ["M128,216 Q132,234 128,248 L112,248 Q110,232 110,216 Z"],
    "forearm_l": ["M46,110 Q40,132 44,148 L56,146 Q54,128 59,110 Z"],
    "forearm_r": ["M154,110 Q160,132 156,148 L144,146 Q146,128 141,110 Z"],
}
BACK_ARMS_ORDER = ["forearm_l", "forearm_r"]

# muscle group (app) -> view + highlighted keys
GROUP_VIEW = {
    "Chest":        ("front", ["chest_l", "chest_r"]),
    "Shoulders":    ("front", ["delts_l", "delts_r"]),
    "Biceps":       ("front", ["biceps_l", "biceps_r"]),
    "Triceps":      ("back",  ["forearm_l", "forearm_r"]),  # placeholder: triceps shown via back arms
    "Lats":         ("back",  ["lats_l", "lats_r"]),
    "Lower back":   ("back",  ["lower_back"]),
    "Traps":        ("back",  ["traps"]),
    "Quads":        ("front", ["quads_l", "quads_r"]),
    "Hamstrings":   ("back",  ["hams_l", "hams_r"]),
    "Glutes":       ("back",  ["glute_l", "glute_r"]),
    "Abductors":    ("front", ["obliq_l", "obliq_r"]),
    "Adductors":    ("front", ["hip_l", "hip_r"]),
    "Calves":       ("front", ["calf_l", "calf_r"]),
    "Abs":          ("front", ["abs"]),
    "Forearms":     ("front", ["forearm_l", "forearm_r"]),
}

VIEW_PARTS = {
    "front": (FRONT_BASE, FRONT_MUSCLES),
    "back":  (BACK_BASE, BACK_MUSCLES),
}

def path_xml(name, fill):
    return f'    <path android:name="{name}" android:fillColor="{fill}" android:pathData="{name}" />'

def build_paths(view, targets):
    base, muscles = VIEW_PARTS[view]
    parts = []
    for name, d in base.items():
        parts.append((name, d, BODY))
    # draw non-target arms under, target arms over handled naturally by order
    for name, dlist in muscles.items():
        fill = ACCENT if name in targets else BODY
        for j, d in enumerate(dlist):
            parts.append((f"{name}_{j}", d, fill))
    return parts

def drawable(view, targets):
    parts = build_paths(view, targets)
    body = "\n".join(
        f'    <path android:name="{n}" android:fillColor="{c}" android:pathData="{d}" />'
        for (n, d, c) in parts
    )
    return (
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp" android:height="140dp"\n'
        '    android:viewportWidth="200" android:viewportHeight="260">\n'
        f'{body}\n</vector>\n'
    )

def animated(view, targets):
    parts = build_paths(view, targets)
    paths = []
    anims = []
    for (n, d, c) in parts:
        alpha = "1.0" if c == ACCENT else "1.0"
        paths.append(f'    <path android:name="{n}" android:fillColor="{c}" android:fillAlpha="{alpha}" android:pathData="{d}" />')
        if c == ACCENT:
            anims.append(
                f'        <objectAnimator android:propertyName="fillAlpha" android:duration="1100" '
                f'android:repeatCount="infinite" android:repeatMode="reverse" '
                f'android:valueFrom="1.0" android:valueTo="0.25" '
                f'android:valueType="floatType" android:startOffset="0">\n'
                f'            <propertyValuesHolder android:propertyName="fillAlpha"/>\n'
                f'        </objectAnimator>'
            )
    # one target object animator per path needs separate targets; merge: each target path gets its own target block
    targets_xml = ""
    for (n, d, c) in parts:
        if c == ACCENT:
            targets_xml += (
                f'    <target android:name="{n}">\n'
                f'        <objectAnimator android:propertyName="fillAlpha" android:duration="1100" '
                f'android:repeatCount="infinite" android:repeatMode="reverse" '
                f'android:valueFrom="1.0" android:valueTo="0.25" android:valueType="floatType" />\n'
                f'    </target>\n'
            )
    body = "\n".join(paths)
    return (
        '<animated-vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    xmlns:aapt="http://schemas.android.com/aapt">\n'
        '    <aapt:attr name="android:drawable">\n'
        '<vector android:width="108dp" android:height="140dp"\n'
        '    android:viewportWidth="200" android:viewportHeight="260">\n'
        f'{body}\n'
        '</vector>\n'
        '    </aapt:attr>\n'
        f'{targets_xml}'
        '</animated-vector>\n'
    )

def slug(m):
    return m.lower().replace(" ", "_")

os.makedirs(OUT, exist_ok=True)
count = 0
for group, (view, targets) in GROUP_VIEW.items():
    s = slug(group)
    with open(f"{OUT}/ill_{s}.xml", "w") as f:
        f.write(drawable(view, targets))
    with open(f"{OUT}/ill_{s}_anim.xml", "w") as f:
        f.write(animated(view, targets))
    count += 2
print(f"generated {count} drawables in {OUT}")

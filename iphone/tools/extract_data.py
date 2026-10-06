#!/usr/bin/env python3
"""Extract the exercise database from the Android Kotlin sources into exdata.json
for the iPhone (Expo) port. Single source of truth stays the Kotlin code."""
import json
import re
import sys
from pathlib import Path

ROOT = Path("/home/cvsbd/hevy/android/app/src/main/java/com/hevyclone/app/data")
DATA = (ROOT / "Data.kt").read_text(encoding="utf-8")
L10N = (ROOT / "L10nData.kt").read_text(encoding="utf-8")

STR = r'"((?:[^"\\]|\\.)*)"'

def kotlin_unescape(s: str) -> str:
    return (s.replace('\\"', '"').replace("\\n", "\n").replace("\\t", "\t")
             .replace("\\\\", "\\").replace("\\$", "$"))

def find_block(src: str, decl: str) -> str:
    """Return the balanced-parens block following `decl` (e.g. `val X = mapOf(`)."""
    m = re.search(re.escape(decl), src)
    if not m:
        sys.exit(f"decl not found: {decl}")
    i = src.index("(", m.end() - 1)
    depth = 0
    for j in range(i, len(src)):
        if src[j] == "(":
            depth += 1
        elif src[j] == ")":
            depth -= 1
            if depth == 0:
                return src[i + 1 : j]
    sys.exit(f"unbalanced block: {decl}")

def parse_strlist(block: str) -> list:
    return [kotlin_unescape(m) for m in re.findall(STR, block)]

def parse_map_str(block: str) -> dict:
    """`"k" to "v",` pairs."""
    out = {}
    for k, v in re.findall(STR + r"\s*to\s*" + STR, block):
        out[kotlin_unescape(k)] = kotlin_unescape(v)
    return out

def parse_map_strlist(block: str) -> dict:
    """`"k" to listOf("a", "b"),` entries."""
    out = {}
    for m in re.finditer(STR + r"\s*to\s*listOf\s*\(", block):
        key = kotlin_unescape(m.group(1))
        i = m.end() - 1
        depth = 0
        for j in range(i, len(block)):
            if block[j] == "(":
                depth += 1
            elif block[j] == ")":
                depth -= 1
                if depth == 0:
                    out[key] = [kotlin_unescape(x) for x in re.findall(STR, block[i + 1 : j])]
                    break
    return out

def parse_exercisedefs(block: str) -> list:
    out = []
    for name, muscle, equip in re.findall(r"ExerciseDef\(\s*" + STR + r"\s*,\s*" + STR + r"\s*,\s*" + STR, block):
        out.append({"name": kotlin_unescape(name), "muscle": kotlin_unescape(muscle), "equip": kotlin_unescape(equip)})
    return out

exercises = (
    parse_exercisedefs(find_block(DATA, "val EX_BASE: List<ExerciseDef> = listOf("))
    + parse_exercisedefs(find_block(DATA, "val EX_EXTRA: List<ExerciseDef> = listOf("))
)
muscle_fr = parse_map_str(find_block(L10N, "val MUSCLE_FR = mapOf("))
equip_fr = parse_map_str(find_block(L10N, "val EQUIP_FR = mapOf("))
muscle_extra = parse_map_str(find_block(L10N, "val MUSCLE_EXTRA = mapOf("))
alias_fr = parse_map_str(find_block(L10N, "val ALIAS_FR: Map<String, String> = mapOf("))
name_fr = {**parse_map_str(find_block(L10N, "val NAME_FR_BASE = mapOf(")),
           **parse_map_str(find_block(L10N, "val NAME_FR_EXTRA = mapOf("))}
cues = parse_map_strlist(find_block(DATA, "val CUES: Map<String, List<String>> = mapOf("))
cues_fr = parse_map_strlist(find_block(L10N, "val CUES_FR = mapOf("))
arch_steps = {**parse_map_strlist(find_block(DATA, "val ARCH_STEPS_BASE: Map<String, List<String>> = mapOf(")),
              **parse_map_strlist(find_block(DATA, "val ARCH_STEPS_EXTRA: Map<String, List<String>> = mapOf("))}
arch_steps_fr = {**parse_map_strlist(find_block(L10N, "val ARCH_STEPS_FR_BASE = mapOf(")),
                 **parse_map_strlist(find_block(L10N, "val ARCH_STEPS_FR_EXTRA = mapOf("))}
equip_hint = parse_map_str(find_block(DATA, "val EQUIP_HINT: Map<String, String> = mapOf("))

names = [e["name"] for e in exercises]
assert len(names) == len(set(names)), "duplicate exercise names"
missing_fr = [n for n in names if n not in name_fr]
if missing_fr:
    print(f"WARNING: {len(missing_fr)} exercises without FR name: {missing_fr[:5]}", file=sys.stderr)
# every muscle must have cues + instructions coverage
muscles = sorted({e["muscle"] for e in exercises})

out = {
    "exercises": exercises,
    "nameFr": name_fr,
    "muscleFr": muscle_fr,
    "equipFr": equip_fr,
    "muscleExtra": muscle_extra,
    "aliasFr": alias_fr,
    "cues": cues,
    "cuesFr": cues_fr,
    "archSteps": arch_steps,
    "archStepsFr": arch_steps_fr,
    "equipHint": equip_hint,
    "muscles": muscles,
}
dest = Path("/home/cvsbd/hevy/iphone/src/data/exdata.json")
dest.parent.mkdir(parents=True, exist_ok=True)
dest.write_text(json.dumps(out, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
print(f"{len(exercises)} exercises, {len(name_fr)} FR names, {len(arch_steps)}+{len(arch_steps_fr)} archetypes, "
      f"{len(alias_fr)} aliases, {len(muscles)} muscles -> {dest} ({dest.stat().st_size // 1024} KB)")
print("muscles:", muscles)

#!/usr/bin/env python3
"""Génère les modules de données du port web depuis les sources Android (source de vérité unique).
- web/src/data.js    : catalogue exercices + dictionnaires FR (depuis iphone/src/data/exdata.json, lui-même
                       extrait de Data.kt/L10nData.kt par iphone/tools/extract_data.py)
- web/src/figures.js : silhouettes muscles (paths SVG depuis les drawables Android ill_*.xml)
Usage : python3 web/extract_web.py  (à rejouer si Data.kt/L10nData.kt bougent)
"""
import json
import re
from pathlib import Path

ROOT = Path("/home/cvsbd/hevy")
EXDATA = ROOT / "iphone/src/data/exdata.json"
RES = ROOT / "android/app/src/main/res/drawable"
DEST = ROOT / "web/src"
DEST.mkdir(parents=True, exist_ok=True)

data = json.loads(EXDATA.read_text(encoding="utf-8"))

def js_str(s):
    return json.dumps(s, ensure_ascii=False)

def js_dict(d):
    return "{" + ",".join(f"{js_str(k)}:{js_str(v)}" for k, v in d.items()) + "}"

def js_dict_list(d):
    return "{" + ",".join(f"{js_str(k)}:[{','.join(js_str(i) for i in v)}]" for k, v in d.items()) + "}"

lines = [
    "// Généré depuis les sources Android (Data.kt / L10nData.kt) — ne pas éditer à la main (web/extract_web.py).",
    "export const EXERCISES = [",
    ",\n".join(
        "{name:%s,muscle:%s,equip:%s}" % (js_str(e["name"]), js_str(e["muscle"]), js_str(e["equip"]))
        for e in data["exercises"]
    ),
    "];",
    f"export const MUSCLES = [{','.join(js_str(m) for m in data['muscles'])}];",
    f"export const NAME_FR = {js_dict(data['nameFr'])};",
    f"export const MUSCLE_FR = {js_dict(data['muscleFr'])};",
    f"export const EQUIP_FR = {js_dict(data['equipFr'])};",
    f"export const MUSCLE_EXTRA = {js_dict(data['muscleExtra'])};",
    f"export const ALIAS_FR = {js_dict(data['aliasFr'])};",
    f"export const EQUIP_HINT = {js_dict(data['equipHint'])};",
    f"export const CUES = {js_dict_list(data['cues'])};",
    f"export const CUES_FR = {js_dict_list(data['cuesFr'])};",
    f"export const ARCH_STEPS = {js_dict_list(data['archSteps'])};",
    f"export const ARCH_STEPS_FR = {js_dict_list(data['archStepsFr'])};",
]
(DEST / "data.js").write_text("\n".join(lines) + "\n", encoding="utf-8")
print(f"data.js : {len(data['exercises'])} exercices, {(DEST / 'data.js').stat().st_size // 1024} Ko")

# ---- figures (silhouettes muscles, paths SVG bruts réutilisables dans un <svg>) ----
MUSCLES_FIG = ["Chest", "Shoulders", "Biceps", "Triceps", "Lats", "Lower back", "Traps",
               "Quads", "Hamstrings", "Glutes", "Abductors", "Adductors", "Calves", "Abs", "Forearms"]

def parse_svg(fname):
    xml = (RES / fname).read_text(encoding="utf-8")
    vw = re.search(r'viewportWidth="([\d.]+)"', xml).group(1)
    vh = re.search(r'viewportHeight="([\d.]+)"', xml).group(1)
    paths = [(m.group(1), m.group(2))
             for m in re.finditer(r'<path[^>]*android:fillColor="([^"]+)"[^>]*android:pathData="([^"]+)"', xml)]
    return vw, vh, paths

def fig_js(vw, vh, ps):
    body = ",".join("{fill:%s,d:%s}" % (js_str(f), js_str(d)) for f, d in ps)
    return "{vw:%s,vh:%s,paths:[%s]}" % (vw, vh, body)

flines = [
    "// Généré depuis les drawables Android ill_*.xml / d_ill_*.xml — ne pas éditer à la main.",
    "export const FIGURES = {",
]
for mus in MUSCLES_FIG:
    lvw, lvh, lp = parse_svg(f"ill_{mus.lower().replace(' ', '_')}.xml")
    dvw, dvh, dp = parse_svg(f"d_ill_{mus.lower().replace(' ', '_')}.xml")
    flines.append(f"{js_str(mus)}:{{light:{fig_js(lvw, lvh, lp)},dark:{fig_js(dvw, dvh, dp)}}},")
# Cardio réutilise la silhouette Abs (comme le port iOS)
lvw, lvh, lp = parse_svg("ill_abs.xml")
dvw, dvh, dp = parse_svg("d_ill_abs.xml")
flines.append(f'"Cardio":{{light:{fig_js(lvw, lvh, lp)},dark:{fig_js(dvw, dvh, dp)}}},')
flines.append("};")
(DEST / "figures.js").write_text("\n".join(flines) + "\n", encoding="utf-8")
print(f"figures.js : {len(MUSCLES_FIG) + 1} muscles ×2, {(DEST / 'figures.js').stat().st_size // 1024} Ko")

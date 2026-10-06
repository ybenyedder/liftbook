#!/usr/bin/env python3
"""Convert Android VectorDrawable muscle illustrations (ill_*.xml / d_ill_*.xml)
to a TS module of SVG path arrays — exact same artwork, no lossy re-drawing."""
import re
from pathlib import Path

RES = Path("/home/cvsbd/hevy/android/app/src/main/res/drawable")
MUSCLES = ["Chest", "Shoulders", "Biceps", "Triceps", "Lats", "Lower back", "Traps",
           "Quads", "Hamstrings", "Glutes", "Abductors", "Adductors", "Calves", "Abs", "Forearms"]

def slug(m):
    return m.lower().replace(" ", "_")

def parse(fname):
    xml = (RES / fname).read_text(encoding="utf-8")
    vw = re.search(r'viewportWidth="([\d.]+)"', xml).group(1)
    vh = re.search(r'viewportHeight="([\d.]+)"', xml).group(1)
    paths = []
    for m in re.finditer(r'<path[^>]*android:fillColor="([^"]+)"[^>]*android:pathData="([^"]+)"', xml):
        paths.append((m.group(1), m.group(2)))
    return {"vw": float(vw), "vh": float(vh), "paths": paths}

out = []
out.append("/* Muscle pictograms extracted 1:1 from the Android app's vector drawables. */")
out.append("export type Fig = { vw: number; vh: number; paths: [string, string][] };")
out.append("export type Figure = { light: Fig; dark: Fig };")
for mus in MUSCLES:
    light = parse(f"ill_{slug(mus)}.xml")
    dark = parse(f"d_ill_{slug(mus)}.xml")
    def emit(name, fig):
        body = ", ".join(f'["{f}", "{d}"]' for f, d in fig["paths"])
        return f'const {name}: Fig = {{ vw: {fig["vw"]}, vh: {fig["vh"]}, paths: [{body}] }};'
    k = slug(mus).replace("_", "")
    out.append(emit(f"{k}_l", light))
    out.append(emit(f"{k}_d", dark))
out.append("export const ILL: Record<string, Figure> = {")
for mus in MUSCLES:
    k = slug(mus).replace("_", "")
    out.append(f'  "{mus}": {{ light: {k}_l, dark: {k}_d }},')
out.append('  "Cardio": { light: abs_l, dark: abs_d },')
out.append("};")
dest = Path("/home/cvsbd/hevy/iphone/src/ui/figures_gen.ts")
dest.parent.mkdir(parents=True, exist_ok=True)
dest.write_text("\n".join(out) + "\n", encoding="utf-8")
total = sum(len(p) for m in MUSCLES for p in parse(f"ill_{slug(m)}.xml")["paths"])
print(f"OK — {len(MUSCLES)} muscles × (light+dark) → {dest} ({dest.stat().st_size // 1024} KB, {total} paths)")

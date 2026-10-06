#!/usr/bin/env python3
"""Generate the Swift data modules (ExData.swift + Figures.swift) for the native iOS port,
straight from the Android Kotlin sources — single source of truth stays the Kotlin code.
Reuses the parsing of tools/extract_data.py."""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
ROOT = Path("/home/cvsbd/hevy/android/app/src/main/java/com/hevyclone/app/data")
DEST = Path("/home/cvsbd/hevy/ios-native/LiftbookCore/Sources/LiftbookCore")
DEST.mkdir(parents=True, exist_ok=True)

# reuse the JSON extraction (already produced by extract_data.py into iphone/src/data)
data = json.loads(Path("/home/cvsbd/hevy/iphone/src/data/exdata.json").read_text(encoding="utf-8"))

def swift_str(s: str) -> str:
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'

def swift_list(items) -> str:
    return "[" + ", ".join(swift_str(i) for i in items) + "]"

out = []
out.append("// Generated from the Android app's Data.kt/L10nData.kt — do not edit by hand (tools/extract_swift.py).")
out.append("")
out.append("public struct ExerciseDef: Sendable {")
out.append("    public let name: String")
out.append("    public let muscle: String")
out.append("    public let equip: String")
out.append("    public init(_ name: String, _ muscle: String, _ equip: String) { self.name = name; self.muscle = muscle; self.equip = equip }")
out.append("}")
out.append("")
out.append("public enum ExData {")
exs = ",\n        ".join(f'ExerciseDef({swift_str(e["name"])}, {swift_str(e["muscle"])}, {swift_str(e["equip"])})' for e in data["exercises"])
out.append(f"    public static let all: [ExerciseDef] = [\n        {exs},\n    ]")
out.append(f"    public static let count = {len(data['exercises'])}")
out.append("    public static let byName: [String: ExerciseDef] = Dictionary(uniqueKeysWithValues: all.map { ($0.name, $0) })")
out.append(f"    public static let muscles: [String] = {swift_list(data['muscles'])}")

def dict_swift(d) -> str:
    return "[" + ", ".join(f"{swift_str(k)}: {swift_str(v)}" for k, v in d.items()) + "]"

out.append(f"    public static let nameFr: [String: String] = {dict_swift(data['nameFr'])}")
out.append(f"    public static let muscleFr: [String: String] = {dict_swift(data['muscleFr'])}")
out.append(f"    public static let equipFr: [String: String] = {dict_swift(data['equipFr'])}")
out.append(f"    public static let muscleExtra: [String: String] = {dict_swift(data['muscleExtra'])}")
out.append(f"    public static let aliasFr: [String: String] = {dict_swift(data['aliasFr'])}")
out.append(f"    public static let equipHintEn: [String: String] = {dict_swift(data['equipHint'])}")

def dict_list_swift(d) -> str:
    return "[" + ", ".join(f"{swift_str(k)}: {swift_list(v)}" for k, v in d.items()) + "]"

out.append(f"    public static let cuesEn: [String: [String]] = {dict_list_swift(data['cues'])}")
out.append(f"    public static let cuesFr: [String: [String]] = {dict_list_swift(data['cuesFr'])}")
out.append(f"    public static let archStepsEn: [String: [String]] = {dict_list_swift(data['archSteps'])}")
out.append(f"    public static let archStepsFr: [String: [String]] = {dict_list_swift(data['archStepsFr'])}")
out.append("}")
(DEST / "ExData.swift").write_text("\n".join(out) + "\n", encoding="utf-8")
print(f"ExData.swift: {len(data['exercises'])} exercices, {(DEST / 'ExData.swift').stat().st_size // 1024} KB")

# ---- figures: reuse the XML parsing from extract_figures.py ----
import re
RES = Path("/home/cvsbd/hevy/android/app/src/main/res/drawable")
MUSCLES = ["Chest", "Shoulders", "Biceps", "Triceps", "Lats", "Lower back", "Traps",
           "Quads", "Hamstrings", "Glutes", "Abductors", "Adductors", "Calves", "Abs", "Forearms"]

def slug(m):
    return m.lower().replace(" ", "_")

def parse(fname):
    xml = (RES / fname).read_text(encoding="utf-8")
    vw = float(re.search(r'viewportWidth="([\d.]+)"', xml).group(1))
    vh = float(re.search(r'viewportHeight="([\d.]+)"', xml).group(1))
    paths = [(m.group(1), m.group(2)) for m in re.finditer(r'<path[^>]*android:fillColor="([^"]+)"[^>]*android:pathData="([^"]+)"', xml)]
    return vw, vh, paths

lines = ["// Generated from the Android app's ill_*.xml / d_ill_*.xml drawables — do not edit by hand.",
         "",
         "public struct Figure: Sendable {",
         "    public let vw: Double",
         "    public let vh: Double",
         "    public let paths: [(fill: String, d: String)]",
         "    public init(vw: Double, vh: Double, paths: [(fill: String, d: String)]) { self.vw = vw; self.vh = vh; self.paths = paths }",
         "}",
         "",
         "public enum Figures {",
         "    public static let map: [String: (light: Figure, dark: Figure)] = ["]
for mus in MUSCLES:
    lvw, lvh, lp = parse(f"ill_{slug(mus)}.xml")
    dvw, dvh, dp = parse(f"d_ill_{slug(mus)}.xml")
    def emit(vw, vh, ps):
        body = ", ".join(f'(fill: {swift_str(f)}, d: {swift_str(d)})' for f, d in ps)
        return f"Figure(vw: {vw}, vh: {vh}, paths: [{body}])"
    lines.append(f'        {swift_str(mus)}: (light: {emit(lvw, lvh, lp)}, dark: {emit(dvw, dvh, dp)}),')
lvw, lvh, lp = parse("ill_abs.xml")
dvw, dvh, dp = parse("d_ill_abs.xml")
def emit(vw, vh, ps):
    body = ", ".join(f'(fill: {swift_str(f)}, d: {swift_str(d)})' for f, d in ps)
    return f"Figure(vw: {vw}, vh: {vh}, paths: [{body}])"
lines.append(f'        "Cardio": (light: {emit(lvw, lvh, lp)}, dark: {emit(dvw, dvh, dp)}),')
lines.append("    ]")
lines.append("}")
(DEST / "Figures.swift").write_text("\n".join(lines) + "\n", encoding="utf-8")
print(f"Figures.swift: {len(MUSCLES)} muscles ×2, {(DEST / 'Figures.swift').stat().st_size // 1024} KB")

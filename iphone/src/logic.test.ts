/** Logic tests (bun test) — port of the Android LogicTest.kt expectations. */
import { describe, expect, test } from "bun:test";
import {
  e1rm, parseCsv, parseHevyCsv, parseExportDate, rebuildPrs, routinesFromWorkouts, round125, seed, vol,
} from "./calc";
import { EXERCISES, EX, NAME_FR, matches, name, archetypeOf, steps } from "./l10ndata";
import { setLang } from "./l10n";

setLang("fr");

describe("exercise database", () => {
  test("≥ 600 unique exercises", () => {
    expect(EXERCISES.length).toBeGreaterThanOrEqual(600);
    expect(new Set(EXERCISES.map((e) => e.name)).size).toBe(EXERCISES.length);
  });
  test("every exercise has a FR name + non-empty instructions", () => {
    for (const e of EXERCISES) {
      expect(NAME_FR[e.name], `FR name for ${e.name}`).toBeTruthy();
      expect(steps(e.name).length, `steps for ${e.name}`).toBeGreaterThan(0);
    }
  });
});

describe("fuzzy FR search (parity with Android LogicTest)", () => {
  test('"tirage poitrine" → Chest Supported Row', () => {
    const hits = EXERCISES.filter((e) => matches("tirage poitrine", e.name)).map((e) => e.name);
    expect(hits).toContain("Chest Supported Row");
  });
  test('"dc" → Barbell Bench Press', () => {
    const hits = EXERCISES.filter((e) => matches("dc", e.name)).map((e) => e.name);
    expect(hits).toContain("Barbell Bench Press");
  });
  test('"sdt" → Deadlift', () => {
    const hits = EXERCISES.filter((e) => matches("sdt", e.name)).map((e) => e.name);
    expect(hits).toContain("Deadlift");
  });
  test('"rowing poulie assis prise en v" → V-Bar Cable Row', () => {
    const hits = EXERCISES.filter((e) => matches("rowing poulie assis prise en v", e.name)).map((e) => e.name);
    expect(hits).toContain("V-Bar Cable Row");
  });
  test("no typo tolerance on muscle/equipment (goblet ≠ mollets)", () => {
    const hits = EXERCISES.filter((e) => matches("goblet", e.name)).map((e) => e.name);
    expect(hits.some((n) => EX[n].muscle === "Calves")).toBe(false);
  });
});

describe("archetypes", () => {
  test("key exercises map to the right instruction archetype", () => {
    expect(archetypeOf("Barbell Bench Press")).toBe("bench");
    expect(archetypeOf("Incline Dumbbell Bench Press")).toBe("incline_bench");
    expect(archetypeOf("Barbell Curl")).toBe("curl");
    expect(archetypeOf("Deadlift")).toBe("deadlift");
    expect(archetypeOf("Romanian Deadlift")).toBe("rdl");
    expect(archetypeOf("Plank")).toBe("plank");
    expect(archetypeOf("Treadmill")).toBe("conditioning");
  });
});

describe("Hevy CSV import", () => {
  const hevyHeader =
    "title,start_time,end_time,description,exercise_title,superset_id,exercise_notes,set_index,set_type,weight_kg,reps,distance_km,duration_seconds,rpe";
  const row = (ex: string, i: number, kg: string, reps: string, date = "18 sept. 2026, 17:31") =>
    `"Push Day","${date}","18 sept. 2026, 18:20","","${ex}","","",${i},"regular",${kg},${reps},,`;

  test("real Hevy FR export format parses", () => {
    const csv = [hevyHeader, row("Développé Couché (Barre)", 1, "70", "8"), row("Développé Couché (Barre)", 2, "70", "8"), row("Tirage Vertical", 1, "60", "10")].join("\n");
    const [ws, rs] = parseHevyCsv(csv);
    expect(ws.length).toBe(1);
    expect(ws[0].name).toBe("Push Day");
    expect(ws[0].exercises.length).toBe(2);
    expect(ws[0].exercises[0].name).toBe("Barbell Bench Press");
    expect(ws[0].exercises[0].sets.length).toBe(2);
    expect(rs.length).toBe(0);
  });

  test("Strong-style headers parse too", () => {
    const csv = ["Date,Workout Name,Exercise Name,Set Order,Weight,Weight Unit,Reps", `"2026-09-18 17:31:00","Bench","Squat (Barre)",1,100,kg,5`].join("\n");
    const [ws] = parseHevyCsv(csv);
    expect(ws.length).toBe(1);
    expect(ws[0].exercises[0].name).toBe("Barbell Squat");
    expect(ws[0].exercises[0].sets[0].kg).toBe(100);
  });

  test("template file (no date) becomes a routine", () => {
    const csv = ["exercise_name,set_index,weight_kg,reps", "Développé Couché (Barre),1,70,8", "Tirage Vertical,1,60,10"].join("\n");
    const [ws, rs] = parseHevyCsv(csv);
    expect(ws.length).toBe(0);
    expect(rs.length).toBe(1);
    expect(rs[0].exercises.length).toBe(2);
  });

  test("localized dates: FR/EN/DE + Japanese month + ISO + epoch", () => {
    expect(parseExportDate("18 sept. 2026, 17:31")).not.toBeNull();
    expect(parseExportDate("18 Sep 2026, 17:31")).not.toBeNull();
    expect(parseExportDate("18 März 2026, 17:31")).not.toBeNull();
    expect(parseExportDate("5 9月 2026, 20:39")).not.toBeNull();
    expect(parseExportDate("2026-09-18T17:31:00+02:00")).not.toBeNull();
    expect(parseExportDate("1772000000")).not.toBeNull();
  });
});

describe("legacy CSV import", () => {
  test("Date;Heure;Exercice;Serie;KG;Reps groups by session", () => {
    const csv = ["Date;Heure;Exercice;Serie;KG;Reps", "5 sept. 2026;17:31;Développé Couché (Barre);1;70;8", "5 sept. 2026;17:31;Tirage Vertical;1;60;10"].join("\n");
    const ws = parseCsv(csv);
    expect(ws.length).toBe(1);
    expect(ws[0].exercises.length).toBe(2);
    expect(ws[0].exercises[0].name).toBe("Barbell Bench Press");
  });
});

describe("PR engine", () => {
  test("rebuildPrs stamps flags and per-workout records chronologically", () => {
    const [ws] = seed(Date.UTC(2026, 8, 1));
    const cache = rebuildPrs(ws);
    const totalPrs = ws.reduce((a, w) => a + w.prs.length, 0);
    expect(totalPrs).toBeGreaterThan(0);
    const bench = cache.get("Barbell Bench Press")!;
    expect(bench.weight).toBeGreaterThan(70);
    // every set carries the flags reset + best session stamped
    const last = ws[ws.length - 1];
    for (const ex of last.exercises) for (const s of ex.sets) expect(typeof s.prW).toBe("boolean");
  });
});

describe("routinesFromWorkouts", () => {
  test("distinct workout names become routines skipping existing", () => {
    const [ws] = seed(Date.UTC(2026, 8, 1));
    const routines = routinesFromWorkouts(ws, new Set(["Push Day"]));
    const names = routines.map((r) => r.name);
    expect(names).toContain("Pull Day");
    expect(names).toContain("Leg Day");
    expect(names).not.toContain("Push Day");
  });
});

describe("math + formatting", () => {
  test("e1rm + round125 + vol", () => {
    expect(e1rm(100, 0)).toBe(100);
    expect(e1rm(100, 30)).toBe(200);
    expect(round125(71.3)).toBe(71.25);
    const w = { exercises: [{ sets: [{ kg: 100, reps: 5, done: true, prW: false, prE: false }] }], prs: [] } as any;
    expect(vol(w)).toBe(500);
  });
  test("seed determinism", () => {
    const [a] = seed(1772000000000);
    const [b] = seed(1772000000000);
    expect(a.length).toBe(b.length);
    expect(JSON.stringify(a[0])).toBe(JSON.stringify(b[0]));
  });
});

describe("FR display names", () => {
  test("known canonical names localize", () => {
    expect(name("Barbell Bench Press")).toBe("Développé Couché (Barre)");
    expect(name("V-Bar Cable Row")).toBe("Tirage Horizontal (Poulie, V-Bar)");
    expect(name("Deadlift")).toBe("Soulevé de Terre");
  });
});

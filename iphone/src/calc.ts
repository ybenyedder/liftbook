/** Pure workout math + formatting + CSV import + demo seed — port of Calc.kt (no RN imports, testable). */
import { Draft, ExEntry, PrBest, PrRec, Routine, SetEntry, Workout, newEx, newSet } from "./models";
import { EX, NAME_FR } from "./l10ndata";

export const LB = 2.2046226;

export function e1rm(kg: number, reps: number): number {
  return reps > 0 ? kg * (1 + reps / 30.0) : kg;
}

export function round125(x: number): number {
  return Math.round(x / 1.25) * 1.25;
}

export function vol(w: Workout): number {
  return w.exercises.reduce(
    (acc, ex) => acc + ex.sets.reduce((a, s) => (s.done && s.kg != null && s.reps != null ? a + s.kg * s.reps : a), 0),
    0,
  );
}

export function reps(w: Workout): number {
  return w.exercises.reduce(
    (acc, ex) => acc + ex.sets.reduce((a, s) => (s.done && s.reps != null ? a + s.reps : a), 0),
    0,
  );
}

export function setsDone(w: Workout): number {
  return w.exercises.reduce((acc, ex) => acc + ex.sets.filter((s) => s.done && (s.kg != null || s.reps != null)).length, 0);
}

const r1 = (x: number) => Math.round(x * 10) / 10;
function trim(x: number): string {
  return Math.round(x) === x ? String(Math.round(x)) : r1(x).toFixed(1);
}

export function fmtKg(kg: number | null, unit: string): string {
  if (kg == null) return "";
  const v = unit === "lb" ? kg * LB : kg;
  return trim(r1(v));
}

/** Display label for the weight unit, Hevy style: kgs / lbs. */
export function unitLabel(unit: string): string {
  return unit === "lb" ? "lbs" : "kg";
}

export function toKg(text: string, unit: string): number | null {
  const v = parseFloat(text.trim().replace(",", "."));
  if (isNaN(v) || v <= 0) return null;
  return unit === "lb" ? v / LB : v;
}

export function fmtVol(kg: number, unit: string): string {
  const v = Math.round(unit === "lb" ? kg * LB : kg);
  return new Intl.NumberFormat("fr-FR").format(v);
}

export function toDisplay(kg: number, unit: string): number {
  return unit === "lb" ? kg * LB : kg;
}

export function fmtDur(ms: number): string {
  const totalMin = Math.floor(ms / 60000);
  const h = Math.floor(totalMin / 60);
  const m = totalMin % 60;
  return h > 0 ? `${h}h ${m}m` : `${m}m`;
}

/** Live per-second clock label: 04:37, or 1:12:45 past the hour. */
export function fmtClock(ms: number): string {
  const s = Math.floor(ms / 1000);
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = s % 60;
  const p = (n: number) => String(n).padStart(2, "0");
  return h > 0 ? `${h}:${p(m)}:${p(sec)}` : `${p(m)}:${p(sec)}`;
}

// ---- date helpers (local timezone, like ZoneId.systemDefault on device) ----

function fmtIntl(ms: number, locale: string, opts: Intl.DateTimeFormatOptions): string {
  return new Intl.DateTimeFormat(locale, opts).format(new Date(ms));
}

export function fmtDateShort(ms: number): string {
  return fmtIntl(ms, "fr-FR", { day: "numeric", month: "short" });
}

export function fmtDateShortEn(ms: number): string {
  return fmtIntl(ms, "en-US", { day: "numeric", month: "short" });
}

export function fmtDateFull(ms: number): string {
  return fmtIntl(ms, "fr-FR", { weekday: "long", day: "numeric", month: "long", year: "numeric" });
}

export function fmtTime(ms: number): string {
  return fmtIntl(ms, "fr-FR", { hour: "2-digit", minute: "2-digit" });
}

export function monthLabel(ms: number, locale = "fr-FR"): string {
  return fmtIntl(ms, locale, { month: "long", year: "numeric" });
}

export function monthShort(ms: number, locale = "fr-FR"): string {
  return fmtIntl(ms, locale, { month: "short" });
}

export function dayName(ms: number, locale: string): string {
  const s = fmtIntl(ms, locale, { weekday: "long" });
  return s.charAt(0).toUpperCase() + s.slice(1);
}

export function localDate(ms: number): { y: number; m: number; d: number; dow: number } {
  const dt = new Date(ms);
  // dow: 1=Monday..7=Sunday (java DayOfWeek)
  return { y: dt.getFullYear(), m: dt.getMonth() + 1, d: dt.getDate(), dow: dt.getDay() === 0 ? 7 : dt.getDay() };
}

export function dayKey(ms: number): string {
  const { y, m, d } = localDate(ms);
  const p = (n: number) => String(n).padStart(2, "0");
  return `${y}-${p(m)}-${p(d)}`;
}

export function weekStart(ms: number): number {
  const { y, m, d, dow } = localDate(ms);
  const monday = new Date(y, m - 1, d - (dow - 1), 0, 0, 0, 0);
  return monday.getTime();
}

export function streak(workouts: Workout[], nowMs: number): number {
  const days = new Set(workouts.map((w) => dayKey(w.startedAt)));
  let now = nowMs;
  if (!days.has(dayKey(now))) now -= 86400000;
  let n = 0;
  while (days.has(dayKey(now))) {
    n++;
    now -= 86400000;
  }
  return n;
}

export function totalVol(workouts: Workout[]): number {
  return workouts.reduce((a, w) => a + vol(w), 0);
}

export function weekStats(workouts: Workout[], nowMs: number) {
  const ws = weekStart(nowMs);
  const inWeek = workouts.filter((w) => w.startedAt >= ws);
  return {
    count: inWeek.length,
    vol: inWeek.reduce((a, w) => a + vol(w), 0),
    reps: inWeek.reduce((a, w) => a + reps(w), 0),
    prs: inWeek.reduce((a, w) => a + w.prs.length, 0),
  };
}

export function weekly(workouts: Workout[], n: number, unit: string, nowMs: number): [string, number][] {
  const ws = weekStart(nowMs);
  const out: [string, number][] = [];
  for (let i = n - 1; i >= 0; i--) {
    const start = ws - i * 7 * 86400000;
    const end = start + 7 * 86400000;
    const v = workouts.filter((w) => w.startedAt >= start && w.startedAt < end).reduce((a, w) => a + vol(w), 0);
    out.push([fmtDateShort(start), toDisplay(v, unit)]);
  }
  return out;
}

export function muscleDist(workouts: Workout[], n: number): [string, number][] {
  const m = new Map<string, number>();
  for (const w of workouts)
    for (const ex of w.exercises)
      for (const s of ex.sets) {
        if (!s.done || s.kg == null || s.reps == null) continue;
        m.set(ex.muscle, (m.get(ex.muscle) ?? 0) + s.kg * s.reps);
      }
  return [...m.entries()].sort((a, b) => b[1] - a[1]).slice(0, n);
}

export function recentPrs(workouts: Workout[], n: number): [PrRec, number, string][] {
  const out: [PrRec, number, string][] = [];
  for (const w of [...workouts].sort((a, b) => b.startedAt - a.startedAt))
    for (const p of w.prs) out.push([p, w.startedAt, w.name]);
  return out.slice(0, n);
}

export function e1rmSeries(name: string, workouts: Workout[], maxPts = 12): [string, number][] {
  const pts: [string, number][] = [];
  for (const w of [...workouts].sort((a, b) => a.startedAt - b.startedAt)) {
    const ex = w.exercises.find((e) => e.name === name);
    if (!ex) continue;
    const best = Math.max(...ex.sets.filter((s) => (s.kg ?? 0) > 0 && (s.reps ?? 0) > 0).map((s) => e1rm(s.kg!, s.reps!)), -Infinity);
    if (Number.isFinite(best)) pts.push([fmtDateShort(w.startedAt), best]);
  }
  return pts.slice(-maxPts);
}

/** Previous performance of an exercise: per set index, "82.5 × 8" (kg units). */
export function prevFor(name: string, workouts: Workout[]): string[] | null {
  for (const w of [...workouts].sort((a, b) => b.startedAt - a.startedAt)) {
    const ex = w.exercises.find((e) => e.name === name && e.sets.some((s) => s.kg != null || s.reps != null));
    if (ex) {
      return ex.sets.map((s) => (s.kg == null && s.reps == null ? "—" : `${fmtKg(s.kg, "kg")} × ${s.reps ?? "—"}`));
    }
  }
  return null;
}

/** Recompute PR cache, stamp per-set flags and per-workout PR lists (chronological). */
export function rebuildPrs(workouts: Workout[]): Map<string, PrBest> {
  const cache = new Map<string, PrBest>();
  for (const w of [...workouts].sort((a, b) => a.startedAt - b.startedAt)) {
    const prs: PrRec[] = [];
    for (const ex of w.exercises) {
      const prev = cache.get(ex.name);
      const pw = prev?.weight ?? 0;
      const pe = prev?.e1rm ?? 0;
      for (const s of ex.sets) {
        s.prW = false;
        s.prE = false;
        const kg = s.kg;
        const reps = s.reps;
        if (kg == null || reps == null) continue;
        if (!s.done) continue;
        if (kg > 0 && kg > pw) s.prW = true;
        if (kg > 0 && e1rm(kg, reps) > pe) s.prE = true;
      }
      const done = ex.sets.filter((s) => s.kg != null && s.reps != null && s.done && s.kg! > 0);
      if (done.length) {
        const bw = Math.max(...done.map((s) => s.kg!));
        const be = Math.max(...done.map((s) => e1rm(s.kg!, s.reps!)));
        if (bw > pw) prs.push({ ex: ex.name, kind: "Weight", value: bw });
        if (be > pe) prs.push({ ex: ex.name, kind: "Est. 1RM", value: be });
        const oldW = prev?.weight ?? 0;
        const oldE = prev?.e1rm ?? 0;
        cache.set(ex.name, {
          weight: Math.max(bw, oldW),
          weightDate: bw > oldW ? w.startedAt : prev?.weightDate ?? w.startedAt,
          e1rm: Math.max(be, oldE),
          e1rmDate: be > oldE ? w.startedAt : prev?.e1rmDate ?? w.startedAt,
        });
      }
    }
    w.prs = prs;
  }
  return cache;
}

// ---------------- legacy CSV import (Date;Heure;Exercice;Serie;KG;Reps) ----------------

export function parseCsv(content: string): Workout[] {
  interface Key {
    date: string;
    time: string;
  }
  const key = (d: string, t: string): string => `${d}\u0001${t}`;
  const groups = new Map<string, Map<string, [number, number | null, number | null][]>>();
  const keyOrder: string[] = [];
  content
    .split("\n")
    .slice(1)
    .filter((l) => l.trim() !== "")
    .forEach((line) => {
      const sep = line.includes(";") ? ";" : ",";
      const parts = line.split(sep);
      if (parts.length < 6) return;
      const date = parts[0].trim();
      const time = parts[1].trim();
      const exName = parts[2].trim();
      const si = parseInt(parts[3].trim(), 10);
      if (isNaN(si)) return;
      const kg = parts[4].trim().replace(",", ".");
      const kgV = kg === "" ? null : parseFloat(kg);
      const reps = parseInt(parts[5].trim(), 10);
      const k = key(date, time);
      if (!groups.has(k)) {
        groups.set(k, new Map());
        keyOrder.push(k);
      }
      const exMap = groups.get(k)!;
      if (!exMap.has(exName)) exMap.set(exName, []);
      exMap.get(exName)!.push([si, isNaN(kgV as number) ? null : (kgV as number), isNaN(reps) ? null : reps]);
    });
  keyOrder.sort((a, b) => a.split("\u0001")[0].localeCompare(b.split("\u0001")[0]));
  const workouts: Workout[] = [];
  keyOrder.forEach((k, i) => {
    const [date] = k.split("\u0001");
    const exMap = groups.get(k)!;
    const ms = parseFrDate(date) ?? Date.now() - (keyOrder.length - i) * 3600000;
    const exercises = [...exMap.entries()].map(([frName, sets]) => {
      const canonical = Object.entries(NAME_FR).find(([, v]) => v === frName)?.[0] ?? frName;
      const muscle = EX[canonical]?.muscle ?? "";
      return {
        name: canonical,
        muscle,
        notes: "",
        superset: false,
        restSec: null,
        sets: sets.sort((a, b) => a[0] - b[0]).map(([, kg, reps]) => newSet(kg, reps, true)),
      };
    });
    workouts.push({ id: 10000000 + i, name: "Séance", startedAt: ms, endedAt: ms + 3600000, exercises, prs: [], notes: "" });
  });
  return workouts;
}

/** "d MMM yyyy" with French month names ("5 sept. 2026"). */
export function parseFrDate(s: string): number | null {
  const m = s.trim().match(/^(\d{1,2})\s+(\S+)\s+(\d{4})$/);
  if (!m) return null;
  const month = matchMonth(m[2], MONTHS_FR);
  if (month == null) return null;
  return new Date(parseInt(m[3], 10), month, parseInt(m[1], 10)).getTime();
}

// ---------------- Hevy / Strong CSV import ----------------

const MONTHS_EN = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
const MONTHS_FR = ["janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc."];
const MONTHS_DE = ["Jan.", "Feb.", "März", "Apr.", "Mai", "Juni", "Juli", "Aug.", "Sept.", "Okt.", "Nov.", "Dez."];
const MONTHS_IT = ["gen.", "feb.", "mar.", "apr.", "mag.", "giu.", "lug.", "ago.", "set.", "ott.", "nov.", "dic."];
const MONTHS_ES = ["ene.", "feb.", "mar.", "abr.", "may.", "jun.", "jul.", "ago.", "sept.", "oct.", "nov.", "dic."];
const MONTHS_PT = ["jan.", "fev.", "mar.", "abr.", "mai.", "jun.", "jul.", "ago.", "set.", "out.", "nov.", "dez."];
const MONTH_TABLES: string[][] = [MONTHS_EN, MONTHS_FR, MONTHS_DE, MONTHS_IT, MONTHS_ES, MONTHS_PT];

function normMonthToken(t: string): string {
  return t.toLowerCase().replace(/[.\u0300-\u036f]/g, (c) => (c === "." ? "" : c)).replace(/\./g, "").normalize("NFD").replace(/[\u0300-\u036f]/g, "");
}

function matchMonth(token: string, table: string[]): number | null {
  const t = normMonthToken(token);
  if (!t) return null;
  const candidates = table.map((name, idx) => [normMonthToken(name), idx] as [string, number]).sort((a, b) => b[0].length - a[0].length);
  for (const [name, idx] of candidates) if (t.startsWith(name) || name.startsWith(t)) return idx;
  return null;
}

/** Hevy's own export date: "18 Sep 2026, 17:31" — month names follow the app language. */
function parseLocalizedDate(s: string): number | null {
  // Japanese month form "5 9月 2026, 20:39" → EN month names first
  const jp = s.replace(/(\d{1,2})\s*月/, (_m, d) => MONTHS_EN[Math.min(12, Math.max(1, parseInt(d, 10))) - 1]);
  const target = jp !== s ? jp : s;
  const m = target.trim().match(/^(\d{1,2})\s+([^\s\d]+)\s+(\d{4})[,\s]+(\d{1,2}):(\d{2})$/);
  if (!m) return null;
  for (const table of MONTH_TABLES) {
    const month = matchMonth(m[2], table);
    if (month != null) {
      return new Date(parseInt(m[3], 10), month, parseInt(m[1], 10), parseInt(m[4], 10), parseInt(m[5], 10)).getTime();
    }
  }
  return null;
}

/** Parse a date as emitted by Hevy/Strong exports: ISO-8601 (with T, space, offset), epoch s/ms,
 *  Hevy's localized "d MMM yyyy, HH:mm", Strong's "Mon Jan 02 17:00:00 GMT+01:00 2023". */
export function parseExportDate(raw: string): number | null {
  const s = raw.trim().replace(/^"|"$/g, "");
  if (!s) return null;
  if (/^\d+$/.test(s)) {
    const n = parseInt(s, 10);
    return n > 10000000000 ? n : n * 1000;
  }
  const iso = s.replace(" ", "T");
  // trailing zone like +01:00 / Z
  let t = Date.parse(iso);
  if (!isNaN(t)) return t;
  t = Date.parse(iso.endsWith("Z") ? iso : iso + "Z");
  if (!isNaN(t)) return t;
  // local datetime without zone
  const mLocal = iso.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?$/);
  if (mLocal) {
    return new Date(+mLocal[1], +mLocal[2] - 1, +mLocal[3], +mLocal[4], +mLocal[5], mLocal[6] ? +mLocal[6] : 0).getTime();
  }
  const loc = parseLocalizedDate(s);
  if (loc != null) return loc;
  // Strong-style: "Mon Jan 02 17:00:00 GMT+01:00 2023"
  const mStr = s.match(/^[A-Za-z]{3}\s+([A-Za-z]{3})\s+(\d{2})\s+(\d{2}):(\d{2}):(\d{2})\s+GMT([+-]\d{2}):?(\d{2})\s+(\d{4})$/);
  if (mStr) {
    const month = MONTHS_EN.findIndex((mm) => mm.toLowerCase() === mStr[1].toLowerCase());
    if (month >= 0) {
      // parse as UTC with the stated offset applied
      const utc = Date.UTC(+mStr[8], month, +mStr[2], +mStr[3], +mStr[4], +mStr[5]);
      const off = (parseInt(mStr[6], 10) * 60 + parseInt(mStr[7], 10)) * (mStr[6].startsWith("-") ? -1 : 1);
      return utc - off * 60000;
    }
  }
  return null;
}

/** Full-file CSV reader (quotes with embedded commas/newlines honored); ';' or ',' accepted. */
export function readCsvTable(content: string): string[][] {
  const rows: string[][] = [];
  let cur: string[] = [];
  let cell = "";
  let inQ = false;
  for (const c of content) {
    if (c === '"') {
      inQ = !inQ;
    } else if (!inQ && (c === "," || c === ";")) {
      cur.push(cell.trim());
      cell = "";
    } else if (!inQ && c === "\n") {
      cur.push(cell.trim());
      cell = "";
      rows.push(cur);
      cur = [];
    } else if (!inQ && c === "\r") {
      // skip
    } else {
      cell += c;
    }
  }
  if (cur.length) {
    cur.push(cell.trim());
    rows.push(cur);
  }
  return rows.filter((r) => r.some((x) => x !== ""));
}

const csvNorm = (s: string) => s.toLowerCase().replace(/_/g, "").replace(/ /g, "").replace(/\./g, "");

interface HevyCols {
  date: number | null;
  endDate: number | null;
  exercise: number;
  weight: number | null;
  reps: number | null;
  order: number | null;
  seconds: number | null;
  done: number | null;
  name: number | null;
  unit: number | null;
  notes: number | null;
  superset: number | null;
}

/** Fuzzy column mapping — Strong-style headers as well as snake_case ones. */
function mapColumns(header: string[]): HevyCols | null {
  const cols = header.map(csvNorm);
  const findFirst = (keys: string[], not: string[] = []): number | null => {
    const i = cols.findIndex((c) => keys.some((k) => c.includes(k)) && !not.some((n) => c.includes(n)));
    return i >= 0 ? i : null;
  };
  const exercise = findFirst(["exercisename", "exercise"]);
  if (exercise == null) return null;
  return {
    exercise,
    date: findFirst(["startdate", "start", "date"], ["end", "duration"]),
    endDate: findFirst(["enddate", "endtime"]),
    weight: findFirst(["weightkg", "weightlbs", "weight"], ["unit"]),
    reps: findFirst(["reps", "repetitions"]),
    order: findFirst(["setorder", "setindex", "setno"]),
    seconds: findFirst(["seconds", "durations", "durationsec"]),
    done: findFirst(["completed", "checked"]),
    name: findFirst(["workoutname", "title", "name"], ["exercise"]),
    unit: findFirst(["weightunit", "unit"]),
    notes: findFirst(["exnotes", "exercisenotes", "notes"], ["workout"]),
    superset: findFirst(["superset"]),
  };
}

/** Map a Hevy/Strong exercise label to our canonical EN key (FR names remapped, unknowns kept). */
export function canonicalExercise(label: string): string {
  const t = label.trim();
  if (EX[t]) return t;
  const clean = csvNorm(t).replace(/\(/g, "").replace(/\)/g, "");
  for (const [en, fr] of Object.entries(NAME_FR)) {
    if (csvNorm(fr).replace(/\(/g, "").replace(/\)/g, "") === clean) return en;
  }
  // Hevy FR names may drop our parenthetical suffix ("Tirage Poitrine" vs "Tirage Poitrine (Machine)")
  for (const [en, fr] of Object.entries(NAME_FR)) {
    if (clean.length >= 5 && csvNorm(fr).replace(/\(/g, "").replace(/\)/g, "").startsWith(clean)) return en;
  }
  return t;
}

interface HRow {
  date: number | null;
  endDate: number | null;
  ex: string;
  kg: number | null;
  reps: number | null;
  order: number;
  done: boolean;
  workoutName: string | null;
  notes: string;
  superset: boolean;
}

/**
 * Parse a Hevy (or Strong) export CSV. Rows are one per set; consecutive rows with the same
 * date form one workout. Files without any date column are treated as routine/template definitions.
 */
export function parseHevyCsv(content: string): [Workout[], Routine[]] {
  const table = readCsvTable(content);
  if (table.length < 2) return [[], []];
  const cols = mapColumns(table[0]);
  if (!cols) return [[], []];
  const rows: HRow[] = [];
  for (let i = 1; i < table.length; i++) {
    const p = table[i];
    if (p.length <= cols.exercise) continue;
    const exRaw = p[cols.exercise];
    if (!exRaw) continue;
    const unitLb = cols.unit != null ? (p[cols.unit] ?? "").toLowerCase().includes("lb") : false;
    let kg: number | null = null;
    if (cols.weight != null) {
      const raw = (p[cols.weight] ?? "").replace(",", ".");
      const v = parseFloat(raw);
      if (!isNaN(v) && raw !== "") kg = unitLb ? v / LB : v;
    }
    const dateStr = cols.date != null ? p[cols.date] ?? "" : "";
    const date = parseExportDate(dateStr);
    const doneRaw = cols.done != null ? (p[cols.done] ?? "").toLowerCase() : "true";
    const supCell = cols.superset != null ? (p[cols.superset] ?? "").trim() : "";
    const sup = supCell !== "" && supCell !== "0";
    const repsRaw = cols.reps != null ? parseFloat(p[cols.reps] ?? "") : NaN;
    rows.push({
      date,
      endDate: cols.endDate != null ? parseExportDate(p[cols.endDate] ?? "") ?? date : date,
      ex: exRaw,
      kg,
      reps: isNaN(repsRaw) ? null : Math.trunc(repsRaw),
      order: cols.order != null ? parseInt(p[cols.order] ?? "", 10) || rows.length : rows.length,
      done: !["false", "0", "no", "warmup"].includes(doneRaw),
      workoutName: cols.name != null ? (p[cols.name] || null) : null,
      notes: cols.notes != null ? p[cols.notes] ?? "" : "",
      superset: sup,
    });
  }
  if (!rows.length) return [[], []];

  // No date column anywhere → template/routine file
  if (rows.every((r) => r.date == null)) {
    const exs = new Map<string, SetEntry[]>();
    const sups = new Set<string>();
    for (const r of rows) {
      if (!exs.has(r.ex)) exs.set(r.ex, []);
      exs.get(r.ex)!.push(newSet(r.kg, r.reps, true));
      if (r.superset) sups.add(r.ex);
    }
    const routine: Routine = {
      id: 0,
      name: rows[0]?.workoutName ?? "Routine Hevy",
      pos: 0,
      exercises: [...exs.entries()].map(([label, sets]) => {
        const canon = canonicalExercise(label);
        return { name: canon, muscle: EX[canon]?.muscle ?? "", notes: "", superset: sups.has(label), restSec: null, sets };
      }),
    };
    return [[], [routine]];
  }

  // Workouts: group by consecutive rows sharing the same date
  const workouts: Workout[] = [];
  let i = 0;
  let id = 10000000;
  while (i < rows.length) {
    const key = rows[i].date;
    if (key == null) {
      i++;
      continue;
    }
    let j = i;
    const exs = new Map<string, SetEntry[]>();
    const names = new Map<string, string>();
    const sups = new Set<string>();
    while (j < rows.length && rows[j].date === key) {
      const row = rows[j];
      if (!exs.has(row.ex)) exs.set(row.ex, []);
      exs.get(row.ex)!.push(newSet(row.kg, row.reps, row.done));
      if (row.workoutName) names.set(row.ex, row.workoutName);
      if (row.superset) sups.add(row.ex);
      j++;
    }
    const end = rows.find((r) => r.date === key)?.endDate ?? key;
    workouts.push({
      id: id++,
      name: [...names.values()][0] ?? "Séance Hevy",
      startedAt: key,
      endedAt: Math.max(end, key + 60000),
      exercises: [...exs.entries()].map(([label, sets]) => {
        const canon = canonicalExercise(label);
        return { name: canon, muscle: EX[canon]?.muscle ?? "", notes: "", superset: sups.has(label), restSec: null, sets };
      }),
      prs: [],
      notes: "",
    });
    i = j;
  }
  workouts.sort((a, b) => a.startedAt - b.startedAt);
  return [workouts, []];
}

/**
 * Hevy never exports templates — rebuild routines from imported workout names:
 * each distinct workout name (case-insensitive, skipping existing routine names)
 * becomes a routine mirroring its most recent occurrence.
 */
export function routinesFromWorkouts(workouts: Workout[], existingRoutineNames: Set<string>): Routine[] {
  const taken = new Set([...existingRoutineNames].map((n) => n.toLowerCase()));
  const out: Routine[] = [];
  let id = 10000000;
  const groups = new Map<string, Workout[]>();
  for (const w of workouts) {
    const k = w.name.trim();
    if (!groups.has(k)) groups.set(k, []);
    groups.get(k)!.push(w);
  }
  for (const [name, group] of groups.entries()) {
    if (!name || taken.has(name.toLowerCase())) continue;
    const latest = group.reduce((a, b) => (b.startedAt > a.startedAt ? b : a), group[0]);
    out.push({
      id: id++,
      name,
      pos: 0,
      exercises: latest.exercises.map((ex) => ({
        name: ex.name, muscle: ex.muscle, notes: "", superset: ex.superset, restSec: ex.restSec,
        sets: ex.sets.map((s) => newSet(s.kg, s.reps, s.done)),
      })),
    });
    taken.add(name.toLowerCase());
  }
  return out;
}

// ---------------- deterministic demo seed ----------------

class SeedRow {
  constructor(public name: string, public nSets: number, public reps: number, public base: number, public inc: number) {}
}

export function seed(nowMs: number): [Workout[], Routine[]] {
  let s = 987654321;
  const rnd = () => {
    s = (((s * 1103515245 + 12345) % 2147483648) + 2147483648) % 2147483648;
    return s / 2147483648;
  };
  const T: Record<string, SeedRow[]> = {
    push: [
      new SeedRow("Barbell Bench Press", 4, 8, 70, 1.1),
      new SeedRow("Incline Dumbbell Bench Press", 3, 10, 24, 0.35),
      new SeedRow("Machine Chest Press", 3, 12, 55, 0.8),
      new SeedRow("Lateral Raise", 3, 15, 9, 0.2),
      new SeedRow("Tricep Pushdown", 3, 12, 27, 0.4),
      new SeedRow("Overhead Cable Extension", 3, 11, 22, 0.35),
    ],
    pull: [
      new SeedRow("Lat Pulldown", 4, 10, 62, 0.8),
      new SeedRow("Seated Cable Row", 3, 10, 57, 0.8),
      new SeedRow("Dumbbell Row", 3, 10, 32, 0.4),
      new SeedRow("Face Pull", 3, 15, 22, 0.25),
      new SeedRow("Barbell Curl", 3, 10, 32, 0.3),
      new SeedRow("Hammer Curl", 3, 12, 15, 0.25),
    ],
    legs: [
      new SeedRow("Barbell Squat", 4, 8, 97, 1.4),
      new SeedRow("Romanian Deadlift", 3, 10, 92, 1.0),
      new SeedRow("Leg Press", 3, 12, 185, 2.2),
      new SeedRow("Lying Leg Curl", 3, 12, 46, 0.4),
      new SeedRow("Standing Calf Raise", 4, 15, 85, 0.9),
      new SeedRow("Cable Crunch", 3, 15, 31, 0.4),
    ],
  };
  const names: Record<string, string> = { push: "Push Day", pull: "Pull Day", legs: "Leg Day" };
  const cycle = ["push", "pull", "rest", "legs", "rest", "push", "pull", "rest", "legs", "rest"];

  const workouts: Workout[] = [];
  let wid = 1;
  for (let back = 83; back >= 0; back--) {
    const kind = back < 5 ? ["push", "pull", "legs", "push", "pull"][4 - back] : cycle[(83 - back) % cycle.length];
    if (kind === "rest") continue;
    if (back > 6 && rnd() < 0.1) continue;
    let start: number;
    if (back === 0) {
      start = nowMs - Math.round((110 + rnd() * 30) * 60000);
    } else {
      const cal = new Date(nowMs);
      cal.setDate(cal.getDate() - back);
      cal.setHours(17 + Math.floor(rnd() * 3), Math.floor(rnd() * 60), 0, 0);
      start = cal.getTime();
    }
    let end = start + Math.round((55 + rnd() * 40) * 60000);
    if (end > nowMs - 30000) end = nowMs - 30000;
    if (end <= start) continue;
    const weekIdx = Math.floor((83 - back) / 7);
    const exs = T[kind].map((row) => {
      const kg = round125(row.base + row.inc * weekIdx);
      const sets: SetEntry[] = [];
      for (let i = 0; i < row.nSets; i++) {
        const rv = rnd() < 0.22 ? row.reps - 1 : rnd() > 0.9 ? row.reps + 1 : row.reps;
        sets.push(newSet(kg, Math.max(4, rv), true));
      }
      return { name: row.name, muscle: EX[row.name]?.muscle ?? "Quads", notes: "", superset: false, restSec: null, sets };
    });
    workouts.push({ id: wid++, name: names[kind], startedAt: start, endedAt: end, exercises: exs, prs: [], notes: "" });
  }

  const routineRows = (rows: SeedRow[]): ExEntry[] =>
    rows.map((r) => newEx(r.name, EX[r.name]?.muscle ?? "", Array.from({ length: r.nSets }, () => newSet(round125(r.base), r.reps, true))));
  let rid = 1;
  const routines: Routine[] = [
    { id: rid++, name: "Push Day", pos: 0, exercises: routineRows(T.push) },
    { id: rid++, name: "Pull Day", pos: 0, exercises: routineRows(T.pull) },
    { id: rid++, name: "Leg Day", pos: 0, exercises: routineRows(T.legs) },
    {
      id: rid++,
      name: "Upper Body",
      pos: 0,
      exercises: [
        newEx("Barbell Bench Press", "Chest", Array.from({ length: 4 }, () => newSet(72.5, 8))),
        newEx("Lat Pulldown", "Lats", Array.from({ length: 4 }, () => newSet(62.0, 10))),
        newEx("Seated Cable Row", "Lats", Array.from({ length: 3 }, () => newSet(57.0, 10))),
        newEx("Machine Shoulder Press", "Shoulders", Array.from({ length: 3 }, () => newSet(40.0, 10))),
        newEx("Barbell Curl", "Biceps", Array.from({ length: 3 }, () => newSet(32.5, 10))),
        newEx("Tricep Pushdown", "Triceps", Array.from({ length: 3 }, () => newSet(27.5, 12))),
      ],
    },
  ];
  return [workouts, routines];
}

/** Data model — 1:1 port of the Android app (Models.kt). */

export interface SetEntry {
  kg: number | null;
  reps: number | null;
  done: boolean;
  prW: boolean;
  prE: boolean;
}

export function newSet(kg: number | null = null, reps: number | null = null, done = true): SetEntry {
  return { kg, reps, done, prW: false, prE: false };
}

export interface ExEntry {
  name: string;
  muscle: string;
  notes: string;
  superset: boolean;
  restSec: number | null;
  sets: SetEntry[];
}

export function newEx(name: string, muscle: string, sets: SetEntry[] = [newSet(null, null, false)]): ExEntry {
  return { name, muscle, notes: "", superset: false, restSec: null, sets };
}

export interface PrRec {
  ex: string;
  kind: string; // "Weight" | "Est. 1RM"
  value: number;
}

export interface Workout {
  id: number;
  name: string;
  startedAt: number;
  endedAt: number;
  exercises: ExEntry[];
  prs: PrRec[];
  notes: string;
}

export interface Routine {
  id: number;
  name: string;
  exercises: ExEntry[];
  pos: number;
}

export interface Settings {
  unit: string; // "kg" | "lb"
  restSec: number;
  theme: string;
  accent: string;
  profileName: string;
  handle: string;
  since: number;
  avatarUrl: string;
}

export interface PrBest {
  weight: number;
  weightDate: number;
  e1rm: number;
  e1rmDate: number;
}

export interface Draft {
  mode: "workout" | "routine";
  routineId: number | null;
  name: string;
  startedAt: number | null;
  notes: string;
  exercises: ExEntry[];
}

export interface WeekStats {
  count: number;
  vol: number;
  reps: number;
  prs: number;
}

export interface BackupData {
  workouts: Workout[];
  routines: Routine[];
}

/** Full cloud snapshot pushed/pulled per account (deletions ride along as tombstones). */
export interface SyncPayload {
  workouts: Workout[];
  routines: Routine[];
  settings: Settings | null;
  delW: number[];
  delR: string[];
  v: number;
}

export function defaultSettings(): Settings {
  return { unit: "kg", restSec: 90, theme: "dark", accent: "blue", profileName: "Athlète", handle: "athlete", since: 0, avatarUrl: "" };
}

export function cloneSet(s: SetEntry): SetEntry {
  return { kg: s.kg, reps: s.reps, done: s.done, prW: s.prW, prE: s.prE };
}

export function cloneEx(e: ExEntry): ExEntry {
  return { name: e.name, muscle: e.muscle, notes: e.notes, superset: e.superset, restSec: e.restSec, sets: e.sets.map(cloneSet) };
}

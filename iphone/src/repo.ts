/** In-memory repository with write-through persistence — port of Repo.kt.
 *  Storage = AsyncStorage JSON blobs (single source of truth = the in-memory lists). */
import AsyncStorage from "@react-native-async-storage/async-storage";
import * as Calc from "./calc";
import { EX } from "./l10ndata";
import {
  BackupData, Draft, ExEntry, PrBest, Routine, SetEntry, Settings, SyncPayload, Workout,
  cloneEx, cloneSet, defaultSettings, newEx, newSet,
} from "./models";
import { Cloud } from "./cloud";
import { makeRev } from "./store";

class RepoImpl {
  workouts: Workout[] = []; // sorted by startedAt ASC
  routines: Routine[] = [];
  settings: Settings = defaultSettings();
  draft: Draft | null = null;
  prCache: Map<string, PrBest> = new Map();
  rev = makeRev();
  ready = false;

  private descCache: Workout[] | null = null;
  private prevCache = new Map<string, string[] | null>();
  private saveTimer: ReturnType<typeof setTimeout> | null = null;

  private touch() {
    this.descCache = null;
    this.prevCache.clear();
    void this.persistDraft();
    this.rev.bump();
  }

  touchPublic() {
    this.touch();
  }

  async init() {
    if (this.ready) return;
    this.ready = true;
    try {
      const [wRaw, rRaw, sRaw, dRaw] = await Promise.all([
        AsyncStorage.getItem("workouts"),
        AsyncStorage.getItem("routines"),
        AsyncStorage.getItem("settings"),
        AsyncStorage.getItem("draft"),
      ]);
      if (wRaw) this.workouts = JSON.parse(wRaw);
      if (rRaw) this.routines = JSON.parse(rRaw);
      if (sRaw) this.settings = { ...defaultSettings(), ...JSON.parse(sRaw) };
      else this.settings.since = Date.now();
      if (dRaw) {
        try {
          this.draft = JSON.parse(dRaw);
        } catch {}
      }
    } catch {}
    this.routines.sort((a, b) => (a.pos ?? 0) - (b.pos ?? 0) || a.id - b.id);
    this.prCache = Calc.rebuildPrs(this.workouts);
    this.rev.bump();
  }

  // ---- persistence (queued whole-blob writes, like the SQLite write-through) ----

  private queueSave() {
    if (this.saveTimer) return;
    this.saveTimer = setTimeout(() => {
      this.saveTimer = null;
      void this.flush();
    }, 150);
  }

  private async flush() {
    try {
      await Promise.all([
        AsyncStorage.setItem("workouts", JSON.stringify(this.workouts)),
        AsyncStorage.setItem("routines", JSON.stringify(this.routines)),
        AsyncStorage.setItem("settings", JSON.stringify(this.settings)),
      ]);
    } catch {}
  }

  async persistDraftNow() {
    await this.persistDraft();
  }

  private async persistDraft() {
    try {
      if (this.draft != null) await AsyncStorage.setItem("draft", JSON.stringify(this.draft));
      else await AsyncStorage.removeItem("draft");
    } catch {}
  }

  nextWorkoutId(): number {
    return (this.workouts.reduce((a, w) => Math.max(a, w.id), 0) || 0) + 1;
  }
  nextRoutineId(): number {
    return (this.routines.reduce((a, r) => Math.max(a, r.id), 0) || 0) + 1;
  }
  nextPos(): number {
    return (this.routines.reduce((a, r) => Math.max(a, r.pos ?? 0), 0) || 0) + 1;
  }

  workoutById(id: number): Workout | null {
    return this.workouts.find((w) => w.id === id) ?? null;
  }
  routineById(id: number): Routine | null {
    return this.routines.find((r) => r.id === id) ?? null;
  }
  workoutsDesc(): Workout[] {
    if (!this.descCache) this.descCache = [...this.workouts].sort((a, b) => b.startedAt - a.startedAt);
    return this.descCache;
  }

  // ---------- draft lifecycle ----------

  startWorkout(routineId: number | null) {
    const r = routineId != null ? this.routineById(routineId) : null;
    const exs: ExEntry[] = r
      ? r.exercises.map((ex) => ({
          name: ex.name, muscle: ex.muscle, notes: ex.notes, superset: ex.superset, restSec: ex.restSec,
          sets: ex.sets.map((s) => newSet(s.kg, s.reps, false)),
        }))
      : [];
    this.draft = { mode: "workout", routineId, name: r?.name ?? "Séance", startedAt: Date.now(), notes: "", exercises: exs };
    this.touch();
  }

  startRoutine(routineId: number | null) {
    const r = routineId != null ? this.routineById(routineId) : null;
    const exs: ExEntry[] = r ? r.exercises.map(cloneEx) : [];
    this.draft = { mode: "routine", routineId, name: r?.name ?? "Nouvelle Routine", startedAt: null, notes: "", exercises: exs };
    this.touch();
  }

  /** Start a new workout with the exercises of a past workout (null = most recent). */
  startRepeat(workoutId: number | null = null) {
    const last = workoutId != null ? this.workoutById(workoutId) : this.workouts.reduce<Workout | null>((a, w) => (!a || w.startedAt > a.startedAt ? w : a), null);
    if (!last) return;
    this.draft = {
      mode: "workout",
      routineId: null,
      name: last.name,
      startedAt: Date.now(),
      notes: last.notes,
      exercises: last.exercises.map((ex) => ({
        name: ex.name, muscle: ex.muscle, notes: ex.notes, superset: ex.superset, restSec: ex.restSec,
        sets: ex.sets.map((s) => newSet(s.kg, s.reps, false)),
      })),
    };
    this.touch();
  }

  addExToDraft(name: string) {
    const d = this.draft;
    if (!d) return;
    d.exercises.push(newEx(name, EX[name]?.muscle ?? "", [newSet(null, null, false)]));
    this.touch();
  }

  saveRoutine(name: string): Routine | null {
    const d = this.draft;
    if (!d || !d.exercises.length) return null;
    let r: Routine;
    if (d.routineId != null) {
      const existing = this.routineById(d.routineId);
      if (!existing) return null;
      existing.name = name;
      existing.exercises = d.exercises;
      r = existing;
    } else {
      r = { id: this.nextRoutineId(), name, exercises: d.exercises, pos: this.nextPos() };
      this.routines.push(r);
    }
    this.draft = null;
    this.queueSave();
    this.touch();
    Cloud.markDirty();
    return r;
  }

  finishWorkout(name: string): Workout | null {
    const d = this.draft;
    if (!d) return null;
    const now = Date.now();
    // records are evaluated against history strictly before this session
    const prs = [];
    for (const ex of d.exercises) {
      const done = ex.sets.filter((s) => (s.kg ?? 0) > 0 && (s.reps ?? 0) > 0 && s.done);
      if (!done.length) continue;
      const prev = this.prFor(ex.name);
      const pw = prev?.weight ?? 0;
      const pe = prev?.e1rm ?? 0;
      const bw = Math.max(...done.map((s) => s.kg!));
      const be = Math.max(...done.map((s) => Calc.e1rm(s.kg!, s.reps!)));
      if (bw > pw) prs.push({ ex: ex.name, kind: "Weight", value: bw });
      if (be > pe) prs.push({ ex: ex.name, kind: "Est. 1RM", value: be });
    }
    const w: Workout = {
      id: this.nextWorkoutId(),
      name: name.trim() || "Séance",
      startedAt: d.startedAt ?? now - 3600000,
      endedAt: now,
      exercises: d.exercises,
      prs,
      notes: d.notes,
    };
    this.workouts.push(w);
    this.prCache = Calc.rebuildPrs(this.workouts);
    this.draft = null;
    this.queueSave();
    this.touch();
    Cloud.markDirty();
    return w;
  }

  discardDraft() {
    this.draft = null;
    this.touch();
  }

  /** True when a workout started from a routine changed structurally vs that routine. */
  draftDiffersFromRoutine(): boolean {
    const d = this.draft;
    if (!d) return false;
    const r = d.routineId != null ? this.routineById(d.routineId) : null;
    if (!r) return false;
    if (d.exercises.length !== r.exercises.length) return true;
    for (let i = 0; i < d.exercises.length; i++) {
      const de = d.exercises[i];
      const re = r.exercises[i];
      if (de.name !== re.name || de.notes !== re.notes || de.superset !== re.superset || de.restSec !== re.restSec || de.sets.length !== re.sets.length)
        return true;
    }
    return false;
  }

  /** Copy the workout's structure back into the routine it was started from (sets kept as template, done=false). */
  updateRoutineFromDraft() {
    const d = this.draft;
    if (!d) return;
    const r = d.routineId != null ? this.routineById(d.routineId) : null;
    if (!r) return;
    r.exercises = d.exercises.map((ex) => ({
      name: ex.name, muscle: ex.muscle, notes: ex.notes, superset: ex.superset, restSec: ex.restSec,
      sets: ex.sets.map((s) => newSet(s.kg, s.reps, false)),
    }));
    this.queueSave();
    this.touch();
    Cloud.markDirty();
  }

  restoreWorkout(w: Workout) {
    this.workouts = this.workouts.filter((x) => x.id !== w.id);
    this.workouts.push(w);
    this.workouts.sort((a, b) => a.startedAt - b.startedAt);
    this.queueSave();
    this.prCache = Calc.rebuildPrs(this.workouts);
    this.touch();
    Cloud.markDirty();
  }

  deleteWorkout(id: number) {
    const w = this.workoutById(id);
    if (w) void Cloud.tombstoneWorkout(w.startedAt);
    this.workouts = this.workouts.filter((x) => x.id !== id);
    this.queueSave();
    this.prCache = Calc.rebuildPrs(this.workouts);
    this.touch();
    Cloud.markDirty();
  }

  routineFromWorkout(workoutId: number, name: string): Routine | null {
    const w = this.workoutById(workoutId);
    if (!w) return null;
    const r: Routine = {
      id: this.nextRoutineId(),
      name: name.trim() || w.name,
      exercises: w.exercises.map(cloneEx),
      pos: this.nextPos(),
    };
    this.routines.push(r);
    this.queueSave();
    this.touch();
    Cloud.markDirty();
    return r;
  }

  renameRoutine(id: number, name: string) {
    const r = this.routineById(id);
    if (!r) return;
    r.name = name.trim() || r.name;
    this.queueSave();
    this.touch();
    Cloud.markDirty();
  }

  duplicateRoutine(id: number): Routine | null {
    const r = this.routineById(id);
    if (!r) return null;
    const copy: Routine = {
      id: this.nextRoutineId(),
      name: r.name + " (2)",
      exercises: r.exercises.map(cloneEx),
      pos: this.nextPos(),
    };
    this.routines.push(copy);
    this.queueSave();
    this.touch();
    Cloud.markDirty();
    return copy;
  }

  deleteRoutine(id: number) {
    const r = this.routineById(id);
    if (r) void Cloud.tombstoneRoutine(r.name);
    this.routines = this.routines.filter((x) => x.id !== id);
    this.queueSave();
    this.touch();
    Cloud.markDirty();
  }

  /** Drag & drop reorder (indices in routines list == manual pos order). */
  moveRoutine(from: number, to: number) {
    if (from === to || from < 0 || from >= this.routines.length || to < 0 || to >= this.routines.length) return;
    const item = this.routines.splice(from, 1)[0];
    this.routines.splice(to, 0, item);
    this.routines.forEach((r, i) => (r.pos = i));
    this.queueSave();
    this.touch();
    Cloud.markDirty();
  }

  // ---------- settings ----------

  setUnit(u: string) {
    this.settings.unit = u;
    this.queueSave();
    this.touch();
    Cloud.markDirty();
  }
  setRest(sec: number) {
    this.settings.restSec = sec;
    this.queueSave();
    this.touch();
    Cloud.markDirty();
  }
  setAccent(a: string) {
    this.settings.accent = a;
    this.queueSave();
    this.touch();
    Cloud.markDirty();
  }
  setTheme(t: string) {
    this.settings.theme = t;
    this.queueSave();
    this.touch();
    Cloud.markDirty();
  }
  setProfile(name: string, handle: string) {
    this.settings.profileName = name;
    this.settings.handle = handle;
    this.queueSave();
    this.touch();
    Cloud.markDirty();
  }
  setAvatar(url: string) {
    this.settings.avatarUrl = url;
    this.queueSave();
    this.touch();
    Cloud.markDirty();
  }

  backupJson(): string {
    return JSON.stringify({ workouts: this.workouts, routines: this.routines } as BackupData);
  }

  restoreBackup(content: string): boolean {
    let data: BackupData;
    try {
      data = JSON.parse(content);
    } catch {
      return false;
    }
    if (!Array.isArray(data.workouts) || !Array.isArray(data.routines)) return false;
    this.workouts = data.workouts;
    this.routines = data.routines;
    this.routines.forEach((r, i) => (r.pos = i));
    this.prCache = Calc.rebuildPrs(this.workouts);
    this.queueSave();
    this.touch();
    Cloud.markDirty();
    return true;
  }

  importCsv(content: string): number {
    const imported = Calc.parseCsv(content);
    for (const w of imported) this.workouts.push(w);
    if (imported.length) {
      this.prCache = Calc.rebuildPrs(this.workouts);
      this.queueSave();
      this.touch();
      Cloud.markDirty();
    }
    return imported.length;
  }

  /** Merge a Hevy/Strong export: workouts added once per start date, routines appended. */
  importHevy(newWorkouts: Workout[], newRoutines: Routine[]): number {
    const knownDates = new Set(this.workouts.map((w) => w.startedAt));
    let added = 0;
    for (const w of newWorkouts) {
      if (knownDates.has(w.startedAt)) continue;
      const fixed = { ...w, id: this.nextWorkoutId() };
      this.workouts.push(fixed);
      added++;
    }
    const allRoutines = [...newRoutines, ...Calc.routinesFromWorkouts(newWorkouts, new Set(this.routines.map((r) => r.name)))];
    for (const r of allRoutines) {
      const fixed = { ...r, id: this.nextRoutineId(), pos: this.nextPos() };
      this.routines.push(fixed);
    }
    if (added > 0 || allRoutines.length) {
      this.workouts.sort((a, b) => a.startedAt - b.startedAt);
      this.prCache = Calc.rebuildPrs(this.workouts);
      this.queueSave();
      this.touch();
      Cloud.markDirty();
    }
    return added;
  }

  wipe(markDirty = true) {
    this.workouts = [];
    this.routines = [];
    this.draft = null;
    this.prCache = new Map();
    this.queueSave();
    this.touch();
    if (markDirty) Cloud.markDirty();
  }

  // ---------- cloud sync ----------

  snapshot(): SyncPayload {
    return {
      workouts: this.workouts,
      routines: this.routines,
      settings: this.settings,
      delW: Cloud.currentTombW(),
      delR: Cloud.currentTombR(),
      v: 1,
    };
  }

  /** Replace local state wholesale with a remote snapshot (clean pull). */
  replaceAll(p: SyncPayload) {
    this.workouts = [...p.workouts];
    this.routines = [...p.routines];
    this.routines.forEach((r, i) => (r.pos = i));
    if (p.settings) this.settings = { ...defaultSettings(), ...p.settings };
    this.prCache = Calc.rebuildPrs(this.workouts);
    this.queueSave();
    this.touch();
  }

  /** Union-merge a remote snapshot into local (conflict / first login). Local deletions win. */
  mergeRemote(p: SyncPayload): boolean {
    const localDelW = new Set(Cloud.currentTombW());
    const localDelR = new Set(Cloud.currentTombR());
    const remoteDelW = new Set(p.delW);
    const remoteDelR = new Set(p.delR);

    const keepW = this.workouts.filter((w) => !remoteDelW.has(w.startedAt));
    const keepR = this.routines.filter((r) => !remoteDelR.has(r.name));
    const knownW = new Set(keepW.map((w) => w.startedAt));
    const knownR = new Set(keepR.map((r) => r.name));
    const addW = p.workouts.filter((w) => !localDelW.has(w.startedAt) && !knownW.has(w.startedAt));
    const addR = p.routines.filter((r) => !localDelR.has(r.name) && !knownR.has(r.name));

    if (keepW.length === this.workouts.length && keepR.length === this.routines.length && !addW.length && !addR.length) return false;

    this.workouts = keepW;
    for (const w of addW) this.workouts.push({ ...w, id: this.nextWorkoutId() });
    this.workouts.sort((a, b) => a.startedAt - b.startedAt);
    this.routines = keepR;
    for (const r of addR) this.routines.push({ ...r, id: this.nextRoutineId(), pos: this.nextPos() });
    this.prCache = Calc.rebuildPrs(this.workouts);
    this.queueSave();
    this.touch();
    return true;
  }

  // ---------- queries ----------

  prFor(name: string): PrBest | null {
    return this.prCache.get(name) ?? null;
  }

  e1rmSeries(name: string): [string, number][] {
    return Calc.e1rmSeries(name, this.workouts);
  }

  /** Previous performance of an exercise: per set index, "82.5kg × 8" in display units. Cached. */
  prevFor(name: string): string[] | null {
    if (this.prevCache.has(name)) {
      const v = this.prevCache.get(name)!;
      return v && v.length ? v : null;
    }
    let result: string[] | null = null;
    for (const w of this.workoutsDesc()) {
      const ex = w.exercises.find((e) => e.name === name && e.sets.some((s) => s.kg != null || s.reps != null));
      if (ex) {
        result = ex.sets.map((s) =>
          s.kg == null && s.reps == null ? "—" : `${Calc.fmtKg(s.kg, this.settings.unit)}${Calc.unitLabel(this.settings.unit)} × ${s.reps ?? "—"}`,
        );
        break;
      }
    }
    this.prevCache.set(name, result ?? []);
    return result;
  }

  routineLastPerformed(r: Routine): number | null {
    const w = this.workoutsDesc().find((w) => w.name === r.name && w.exercises.some((e) => r.exercises.some((re) => re.name === e.name)));
    return w?.startedAt ?? null;
  }

  /** Per-set "previous" strings for an exercise, computed strictly before the given timestamp. */
  prevSetsBefore(beforeMs: number, name: string): string[] | null {
    for (const w of [...this.workouts].sort((a, b) => b.startedAt - a.startedAt)) {
      if (w.startedAt >= beforeMs) continue;
      const ex = w.exercises.find((e) => e.name === name && e.sets.some((s) => s.kg != null || s.reps != null));
      if (!ex) continue;
      return ex.sets.map((s) =>
        s.kg == null && s.reps == null ? "—" : `${Calc.fmtKg(s.kg, this.settings.unit)}${Calc.unitLabel(this.settings.unit)} × ${s.reps ?? "—"}`,
      );
    }
    return null;
  }
}

export const Repo = new RepoImpl();

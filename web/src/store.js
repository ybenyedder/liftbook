// Store central du port web — port de data/Repo.kt (persistance + mutations) et de
// l'orchestration sync de data/Cloud.kt (LWW, tombstones, photos), adapté au navigateur :
// localStorage pour les métadonnées JSON, IndexedDB pour les pixels (photos/avatar).
import * as Cloud from './cloud.js';
import * as Calc from './calc.js';
import { setCustomDefs, exName } from './search.js';
import { EXERCISES } from './data.js';

const LS = {
  workouts: 'lb.workouts', routines: 'lb.routines', settings: 'lb.settings',
  photos: 'lb.photos', customs: 'lb.customs', draft: 'lb.draft', meta: 'lb.meta',
  session: 'lb.session', skipped: 'lb.skipped', nav: 'lb.nav',
};

export const store = {
  workouts: [],       // triées startedAt ASC
  routines: [],
  photos: [],         // triées ts ASC
  customs: [],        // {name, muscle, equip}
  settings: { unit: 'kg', restSec: 90, theme: 'dark', accent: 'blue', profileName: 'Athlète', handle: 'athlete', since: null, avatarUrl: '' },
  draft: null,
  prCache: {},
  session: null,      // {email, userId, access, refresh, expiresAt}
  skipped: false,
  // ---- méta sync (Cloud.kt) ----
  dirtyAt: 0, pushedTs: 0, lastSeenRemoteTs: 0, lastAccount: '',
  delW: [], delR: [], delP: [], delC: [], pendingDelObjs: [],
  pendingUploadsBlobs: true, // les blobs vivent dans IndexedDB, pas ici
  syncing: false,
  syncStatus: '',
  deletedUndo: null, // séance supprimée → undo bar 5 s
};

/* ============================== événements ============================== */
const listeners = {};
export function on(topic, cb) { (listeners[topic] = listeners[topic] || []).push(cb); }
export function off(topic, cb) { const l = listeners[topic]; if (l) { const i = l.indexOf(cb); if (i >= 0) l.splice(i, 1); } }
export function emit(topic) { (listeners[topic] || []).forEach(cb => cb()); }
let prevDisplayCache = {};
function touch(topics = ['workouts']) {
  persistAll();
  prevDisplayCache = {};
  for (const t of new Set([...topics, 'change'])) emit(t);
}

/* ============================== persistance ============================== */
function lsGet(k, fallback) {
  try { const v = localStorage.getItem(LS[k]); return v == null ? fallback : JSON.parse(v); }
  catch { return fallback; }
}
function lsSet(k, v) { localStorage.setItem(LS[k], JSON.stringify(v)); }
function lsDel(k) { localStorage.removeItem(LS[k]); }

export function persistAll() {
  lsSet('workouts', store.workouts);
  lsSet('routines', store.routines);
  lsSet('photos', store.photos);
  lsSet('customs', store.customs);
  lsSet('settings', store.settings);
  if (store.draft) lsSet('draft', store.draft); else lsDel('draft');
  persistMeta();
}
function persistMeta() {
  lsSet('meta', {
    dirtyAt: store.dirtyAt, pushedTs: store.pushedTs, lastSeenRemoteTs: store.lastSeenRemoteTs,
    lastAccount: store.lastAccount, delW: store.delW, delR: store.delR, delP: store.delP,
    delC: store.delC, pendingDelObjs: store.pendingDelObjs,
  });
}
export const persistDraftNow = persistAll; // frappe dans le logger (port de persistDraftNow)

/* ============================== IndexedDB (pixels) ============================== */
let idb = null;
function db() {
  if (idb) return idb;
  idb = new Promise((res, rej) => {
    const r = indexedDB.open('liftbook', 1);
    r.onupgradeneeded = () => {
      r.result.createObjectStore('photos');   // id → Blob
      r.result.createObjectStore('misc');      // 'avatar' → Blob
    };
    r.onsuccess = () => res(r.result);
    r.onerror = () => rej(r.error);
  });
  return idb;
}
async function idbPut(storeName, key, blob) {
  const d = await db();
  return new Promise((res, rej) => {
    const tx = d.transaction(storeName, 'readwrite');
    tx.objectStore(storeName).put(blob, key);
    tx.oncomplete = res; tx.onerror = () => rej(tx.error);
  });
}
async function idbGet(storeName, key) {
  const d = await db();
  return new Promise((res) => {
    const tx = d.transaction(storeName, 'readonly');
    const rq = tx.objectStore(storeName).get(key);
    rq.onsuccess = () => res(rq.result ?? null);
    rq.onerror = () => res(null);
  });
}
async function idbDel(storeName, key) {
  const d = await db();
  return new Promise((res) => {
    const tx = d.transaction(storeName, 'readwrite');
    tx.objectStore(storeName).delete(key);
    tx.oncomplete = res; tx.onerror = res;
  });
}
const urlCache = new Map(); // id → object URL
/** Object URL du fichier local d'une photo (null si absent). */
export async function photoUrl(id) {
  if (urlCache.has(id)) return urlCache.get(id);
  const blob = await idbGet('photos', id);
  if (!blob) return null;
  const u = URL.createObjectURL(blob);
  urlCache.set(id, u);
  lruTrim();
  return u;
}
async function lruTrim() {
  // cache LRU 12 : les fichiers les plus vieux partent (métadonnées conservées)
  const ids = [...store.photos].sort((a, b) => b.ts - a.ts).map(p => p.id);
  for (const id of ids.slice(12)) {
    await idbDel('photos', id);
    const u = urlCache.get(id);
    if (u) { URL.revokeObjectURL(u); urlCache.delete(id); }
  }
}
export async function photoBlob(id) { return idbGet('photos', id); }
export async function savePhotoBytes(id, blob) {
  await idbPut('photos', id, blob);
  urlCache.delete(id) && URL.revokeObjectURL(urlCache.get(id));
  urlCache.delete(id);
  lruTrim();
}
export async function setAvatarBlob(blob) { await idbPut('misc', 'avatar', blob); emit('avatar'); }
export async function avatarBlob() { return idbGet('misc', 'avatar'); }

/* ============================== boot ============================== */
const builtinNames = new Set(EXERCISES.map(e => e.name));

export function init() {
  store.workouts = lsGet('workouts', []);
  store.routines = lsGet('routines', []);
  store.photos = lsGet('photos', []);
  store.customs = lsGet('customs', []);
  store.settings = Object.assign({}, store.settings, lsGet('settings', {}));
  if (!store.settings.since) store.settings.since = Date.now();
  store.draft = lsGet('draft', null);
  store.skipped = lsGet('skipped', false);
  store.session = lsGet('session', null);
  const meta = lsGet('meta', {});
  Object.assign(store, {
    dirtyAt: meta.dirtyAt || 0, pushedTs: meta.pushedTs || 0, lastSeenRemoteTs: meta.lastSeenRemoteTs || 0,
    lastAccount: meta.lastAccount || '', delW: meta.delW || [], delR: meta.delR || [],
    delP: meta.delP || [], delC: meta.delC || [], pendingDelObjs: meta.pendingDelObjs || [],
  });
  applyCustoms(store.customs);
  store.prCache = Calc.rebuildPrs(store.workouts);
  persistAll();
}

/** Les customs vivent dans le catalogue global : recherche, icône muscle, cardio, disques. */
function applyCustoms(list) {
  store.customs = [];
  const seen = new Set();
  for (const c of list) { if (!seen.has(c.name)) { seen.add(c.name); store.customs.push({ name: c.name, muscle: c.muscle, equip: c.equip || 'Other' }); } }
  lsSet('customs', store.customs);
  setCustomDefs(store.customs); // registre du module de recherche
  emit('customs');
}

/* ============================== ids / requêtes ============================== */
export const nextWorkoutId = () => (store.workouts.reduce((m, w) => Math.max(m, w.id), 0) || 0) + 1;
export const nextRoutineId = () => (store.routines.reduce((m, r) => Math.max(m, r.id), 0) || 0) + 1;
export const nextPos = () => (store.routines.reduce((m, r) => Math.max(m, r.pos), 0) || 0) + 1;
export const nextPhotoId = () => (store.photos.reduce((m, p) => Math.max(m, p.id), 0) || 0) + 1;
export const workoutById = id => store.workouts.find(w => w.id === id) || null;
export const routineById = id => store.routines.find(r => r.id === id) || null;
export const workoutsDesc = () => [...store.workouts].sort((a, b) => b.startedAt - a.startedAt);
export const photosDesc = () => [...store.photos].sort((a, b) => b.ts - a.ts);
export const photoById = id => store.photos.find(p => p.id === id) || null;
export const photoForWorkout = wId => [...store.photos].filter(p => p.wId === wId).pop() || null;

/* ============================== brouillon (draft lifecycle) ============================== */

const copyExForWorkout = ex => ({
  name: ex.name, muscle: ex.muscle, notes: ex.notes || '', superset: !!ex.superset,
  restSec: ex.restSec ?? null,
  sets: ex.sets.map(s => ({ kg: s.kg ?? null, reps: s.reps ?? null, mins: s.mins ?? null, km: s.km ?? null, done: false, prW: false, prE: false })),
});
const copyExFull = ex => ({
  name: ex.name, muscle: ex.muscle, notes: ex.notes || '', superset: !!ex.superset,
  restSec: ex.restSec ?? null,
  sets: ex.sets.map(s => ({ kg: s.kg ?? null, reps: s.reps ?? null, mins: s.mins ?? null, km: s.km ?? null, done: !!s.done, prW: false, prE: false })),
});

export function startWorkout(routineId) {
  const r = routineId != null ? routineById(routineId) : null;
  store.draft = {
    mode: 'workout', routineId: routineId ?? null, name: r ? r.name : 'Séance',
    startedAt: Date.now(), notes: '',
    exercises: r ? r.exercises.map(copyExForWorkout) : [],
  };
  touch(['draft', 'workouts']);
}
export function startRoutine(routineId) {
  const r = routineId != null ? routineById(routineId) : null;
  store.draft = {
    mode: 'routine', routineId: routineId ?? null, name: r ? r.name : 'Nouvelle Routine',
    startedAt: null, notes: '',
    exercises: r ? r.exercises.map(copyExFull) : [],
  };
  touch(['draft']);
}
export function startRepeat(workoutId = null) {
  const last = (workoutId != null ? workoutById(workoutId) : null) ||
    [...store.workouts].sort((a, b) => b.startedAt - a.startedAt)[0];
  if (!last) return;
  store.draft = {
    mode: 'workout', routineId: null, name: last.name, startedAt: Date.now(), notes: last.notes || '',
    exercises: last.exercises.map(copyExForWorkout),
  };
  touch(['draft', 'workouts']);
}
export function addExToDraft(name) {
  const d = store.draft; if (!d) return;
  const all = [...EXERCISES, ...store.customs.map(c => ({ name: c.name, muscle: c.muscle, equip: c.equip }))];
  const def = all.find(e => e.name === name);
  d.exercises.push({ name, muscle: def ? def.muscle : '', notes: '', superset: false, restSec: null, sets: [{ kg: null, reps: null, mins: null, km: null, done: false, prW: false, prE: false }] });
  touch(['draft']);
}
export function discardDraft() { store.draft = null; touch(['draft']); }

/** L'identité d'une routine en sync est son nom — jamais de doublon. */
export function uniqueRoutineName(base) {
  const b = (base || '').trim() || 'Routine';
  if (!store.routines.some(r => r.name === b)) return b;
  let i = 2;
  while (store.routines.some(r => r.name === `${b} (${i})`)) i++;
  return `${b} (${i})`;
}

export function saveRoutine(name) {
  const d = store.draft; if (!d || d.exercises.length === 0) return null;
  let r;
  if (d.routineId != null) {
    r = routineById(d.routineId);
    if (!r) return null;
    r.name = uniqueRoutineName(name);
    r.exercises = d.exercises;
  } else {
    r = { id: nextRoutineId(), name: uniqueRoutineName(name), exercises: d.exercises, pos: nextPos() };
    store.routines.push(r);
  }
  store.draft = null;
  touch(['draft', 'routines']);
  markDirty();
  return r;
}

export function finishWorkout(name) {
  const d = store.draft; if (!d) return null;
  const now = Date.now();
  const prs = [];
  for (const ex of d.exercises) {
    const done = ex.sets.filter(s => (s.kg || 0) > 0 && (s.reps || 0) > 0 && s.done);
    if (!done.length) continue;
    const prev = prFor(ex.name);
    const pw = prev ? prev.weight : 0, pe = prev ? prev.e1rm : 0;
    const bw = Math.max(...done.map(s => s.kg));
    const be = Math.max(...done.map(s => Calc.e1rm(s.kg, s.reps)));
    if (bw > pw) prs.push({ ex: ex.name, kind: 'Weight', value: bw });
    if (be > pe) prs.push({ ex: ex.name, kind: 'Est. 1RM', value: be });
  }
  const w = {
    id: nextWorkoutId(),
    name: (name || '').trim() || 'Séance',
    startedAt: d.startedAt ?? (now - 3600000),
    endedAt: now,
    exercises: d.exercises, notes: d.notes || '', prs,
  };
  store.workouts.push(w);
  store.prCache = Calc.rebuildPrs(store.workouts);
  store.draft = null;
  touch(['draft', 'workouts']);
  markDirty();
  return w;
}

/** Structurellement modifiée vs la routine d'origine (poids/réps ignorés volontairement). */
export function draftDiffersFromRoutine() {
  const d = store.draft; if (!d) return false;
  const r = d.routineId != null ? routineById(d.routineId) : null;
  if (!r) return false;
  if (d.exercises.length !== r.exercises.length) return true;
  return d.exercises.some((de, i) => {
    const re = r.exercises[i];
    return de.name !== re.name || (de.notes || '') !== (re.notes || '') || !!de.superset !== !!re.superset ||
      (de.restSec ?? null) !== (re.restSec ?? null) || de.sets.length !== re.sets.length;
  });
}

export function updateRoutineFromDraft() {
  const d = store.draft; if (!d) return;
  const r = d.routineId != null ? routineById(d.routineId) : null;
  if (!r) return;
  r.exercises = d.exercises.map(ex => ({
    name: ex.name, muscle: ex.muscle, notes: ex.notes || '', superset: !!ex.superset, restSec: ex.restSec ?? null,
    sets: ex.sets.map(s => ({ kg: s.kg ?? null, reps: s.reps ?? null, mins: s.mins ?? null, km: s.km ?? null, done: false, prW: false, prE: false })),
  }));
  touch(['routines']);
  markDirty();
}

export function restoreWorkout(w) {
  untombstoneWorkout(w.startedAt);
  store.workouts = store.workouts.filter(x => x.id !== w.id);
  store.workouts.push(w);
  store.workouts.sort((a, b) => a.startedAt - b.startedAt);
  store.prCache = Calc.rebuildPrs(store.workouts);
  touch(['workouts']);
  markDirty();
}

export function deleteWorkout(id) {
  const w = workoutById(id);
  if (w) tombstoneWorkout(w.startedAt);
  store.workouts = store.workouts.filter(x => x.id !== id);
  store.prCache = Calc.rebuildPrs(store.workouts);
  touch(['workouts']);
  markDirty();
}

export function routineFromWorkout(workoutId, name) {
  const w = workoutById(workoutId); if (!w) return null;
  const r = {
    id: nextRoutineId(), name: uniqueRoutineName((name || '') || w.name),
    exercises: w.exercises.map(copyExFull), pos: nextPos(),
  };
  store.routines.push(r);
  touch(['routines']);
  markDirty();
  return r;
}
export function renameRoutine(id, name) {
  const r = routineById(id); if (!r) return;
  const n = (name || '').trim();
  if (!n || n === r.name) return;
  r.name = uniqueRoutineName(n);
  touch(['routines']); markDirty();
}
export function duplicateRoutine(id) {
  const r = routineById(id); if (!r) return null;
  const copy = { id: nextRoutineId(), name: uniqueRoutineName(r.name), exercises: r.exercises.map(copyExFull), pos: nextPos() };
  store.routines.push(copy);
  touch(['routines']); markDirty();
  return copy;
}
export function deleteRoutine(id) {
  const r = routineById(id);
  if (r) tombstoneRoutine(r.name);
  store.routines = store.routines.filter(x => x.id !== id);
  touch(['routines']); markDirty();
}
export function moveRoutine(from, to) {
  const rs = store.routines;
  if (from === to || from < 0 || from >= rs.length || to < 0 || to >= rs.length) return;
  rs.splice(to, 0, rs.splice(from, 1)[0]);
  rs.forEach((r, i) => { r.pos = i; });
  touch(['routines']); markDirty();
}

/* ============================== exercices custom ============================== */

const normName = s => (s || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-z0-9]/g, '');

export function customNameTaken(raw) {
  const n = normName(raw);
  if (!n) return false;
  const all = [...EXERCISES, ...store.customs.map(c => ({ name: c.name }))];
  return all.some(e => normName(e.name) === n || normName(exName(e.name)) === n);
}

export function addCustom(nameRaw, muscle, equip) {
  const name = (nameRaw || '').trim();
  if (!name || customNameTaken(name)) return null;
  const cx = { name, muscle, equip: equip || 'Other' };
  store.customs.push(cx);
  applyCustoms(store.customs);
  touch(['customs']); markDirty();
  return cx;
}

export function deleteCustom(name) {
  if (!store.customs.some(c => c.name === name)) return;
  tombstoneCustom(name);
  applyCustoms(store.customs.filter(c => c.name !== name));
  touch(['customs']); markDirty();
}

/* ============================== photos de progression ============================== */

export async function addPhoto(blob, ts = Date.now(), wId = null) {
  const id = nextPhotoId();
  await idbPut('photos', id, blob);
  const p = { id, ts, wId: wId ?? null, note: '', kg: null, remote: '' };
  store.photos.push(p);
  store.photos.sort((a, b) => a.ts - b.ts);
  touch(['photos']); markDirty();
  return p;
}
export function updatePhoto(id, note = null, kg = null, kgSet = false) {
  const p = photoById(id); if (!p) return;
  if (note != null) p.note = (note || '').trim();
  if (kgSet) p.kg = kg;
  touch(['photos']); markDirty();
}
export function linkPhotoToWorkout(photoId, wId) {
  const p = photoById(photoId); if (!p) return;
  p.wId = wId;
  touch(['photos']); markDirty();
}
export function setPhotoRemote(id, remote) {
  const p = photoById(id); if (!p || p.remote === remote) return;
  p.remote = remote;
  persistAll(); markDirty();
}
export async function deletePhoto(id) {
  const p = photoById(id); if (!p) return;
  tombstonePhoto(id);
  if (p.remote) deletePhotoObject(p.remote);
  store.photos = store.photos.filter(x => x.id !== id);
  await idbDel('photos', id);
  const u = urlCache.get(id); if (u) { URL.revokeObjectURL(u); urlCache.delete(id); }
  touch(['photos']); markDirty();
}

/* ============================== réglages ============================== */
export function setUnit(u) { store.settings.unit = u; touch(['settings']); markDirty(); }
export function setRest(sec) { store.settings.restSec = sec; touch(['settings']); markDirty(); }
export function setAccent(a) { store.settings.accent = a; touch(['settings']); markDirty(); }
export function setProfile(name, handle) {
  store.settings.profileName = name; store.settings.handle = handle;
  touch(['settings']); markDirty();
}
export function setAvatar(url) { store.settings.avatarUrl = url; touch(['settings']); markDirty(); }

/* ============================== backup / import / wipe ============================== */

export function backupJson() {
  return JSON.stringify({ workouts: store.workouts, routines: store.routines, photos: store.photos });
}
export function restoreBackup(content) {
  let data; try { data = JSON.parse(content); } catch { return false; }
  if (!data || !Array.isArray(data.workouts) || !Array.isArray(data.routines)) return false;
  store.workouts = data.workouts.map(w => normalizeWorkout(w));
  store.routines = data.routines.map((r, i) => ({ ...normalizeRoutine(r), pos: i }));
  store.photos = (data.photos || []).map(p => ({ id: +p.id, ts: +p.ts, wId: p.wId ?? null, note: p.note || '', kg: p.kg ?? null, remote: p.remote || '' }));
  store.photos.sort((a, b) => a.ts - b.ts);
  store.prCache = Calc.rebuildPrs(store.workouts);
  touch(['workouts', 'routines', 'photos']); markDirty();
  return true;
}

export function importCsv(content) {
  const imported = Calc.parseCsv(content);
  const known = new Set(store.workouts.map(w => w.startedAt));
  let added = 0;
  for (const w of imported) {
    if (known.has(w.startedAt)) continue;
    const fixed = { ...w, id: nextWorkoutId() };
    store.workouts.push(fixed); added++;
  }
  if (added > 0) {
    store.workouts.sort((a, b) => a.startedAt - b.startedAt);
    store.prCache = Calc.rebuildPrs(store.workouts);
    touch(['workouts']); markDirty();
  }
  return added;
}

export function importHevy(newWorkouts, newRoutines) {
  const known = new Set(store.workouts.map(w => w.startedAt));
  let added = 0;
  for (const w of newWorkouts) {
    if (known.has(w.startedAt)) continue;
    store.workouts.push({ ...w, id: nextWorkoutId() }); added++;
  }
  const allRoutines = [...(newRoutines || []),
    ...Calc.routinesFromWorkouts(newWorkouts, new Set(store.routines.map(r => r.name)))];
  for (const r of allRoutines) {
    store.routines.push({ ...r, id: nextRoutineId(), pos: nextPos() });
  }
  if (added > 0 || allRoutines.length) {
    store.workouts.sort((a, b) => a.startedAt - b.startedAt);
    store.prCache = Calc.rebuildPrs(store.workouts);
    touch(['workouts', 'routines']); markDirty();
  }
  return added;
}

export function wipe(markDirtyFlag = true) {
  if (markDirtyFlag) {
    for (const p of store.photos) {
      tombstonePhoto(p.id);
      if (p.remote) deletePhotoObject(p.remote);
    }
    store.customs.forEach(c => tombstoneCustom(c.name));
  }
  store.workouts = []; store.routines = []; store.photos = []; store.customs = []; store.draft = null;
  store.prCache = {};
  applyCustoms([]);
  (async () => {
    const d = await db().catch(() => null);
    if (d) { try { d.transaction('photos', 'readwrite').objectStore('photos').clear(); } catch {} }
  })();
  touch(['workouts', 'routines', 'photos', 'customs', 'draft']);
  if (markDirtyFlag) markDirty();
}

/* ============================== tombstones + sync (Cloud.kt) ============================== */

function cap(list, item) { if (!list.includes(item)) { list.push(item); if (list.length > 400) list.shift(); } persistMeta(); }
export function tombstoneWorkout(startedAt) { cap(store.delW, startedAt); }
export function untombstoneWorkout(startedAt) { const i = store.delW.indexOf(startedAt); if (i >= 0) { store.delW.splice(i, 1); persistMeta(); } }
export function tombstoneRoutine(name) { cap(store.delR, name); }
export function tombstonePhoto(id) { cap(store.delP, id); }
export function tombstoneCustom(name) { if (name) cap(store.delC, name); }
export function deletePhotoObject(path) { if (path && !store.pendingDelObjs.includes(path)) { store.pendingDelObjs.push(path); persistMeta(); } }

let syncDebounce = null;
export function markDirty() {
  store.dirtyAt = Date.now();
  persistMeta();
  requestSync(3000);
}
export function requestSync(debounceMs = 3000) {
  if (!store.session) return;
  clearTimeout(syncDebounce);
  syncDebounce = setTimeout(() => syncNow(), debounceMs);
}

export function snapshot() {
  return {
    workouts: store.workouts, routines: store.routines, settings: store.settings,
    delW: [...store.delW], delR: [...store.delR], photos: store.photos, delP: [...store.delP],
    customs: store.customs, delC: [...store.delC], v: 4,
  };
}

const normW = w => normalizeWorkout(w);
function normalizeWorkout(w) {
  return {
    id: +w.id, name: String(w.name || 'Séance'), startedAt: +w.startedAt, endedAt: +w.endedAt,
    notes: w.notes || '',
    prs: (w.prs || []).map(p => ({ ex: p.ex, kind: p.kind, value: +p.value })),
    exercises: (w.exercises || []).map(e => ({
      name: e.name, muscle: e.muscle || '', notes: e.notes || '', superset: !!e.superset, restSec: e.restSec ?? null,
      sets: (e.sets || []).map(s => ({
        kg: s.kg ?? null, reps: s.reps ?? null, mins: s.mins ?? null, km: s.km ?? null,
        done: s.done !== false, prW: !!s.prW, prE: !!s.prE,
      })),
    })),
  };
}
function normalizeRoutine(r) {
  return {
    id: +r.id, name: String(r.name || 'Routine'), pos: +(r.pos || 0),
    exercises: (r.exercises || []).map(e => ({
      name: e.name, muscle: e.muscle || '', notes: e.notes || '', superset: !!e.superset, restSec: e.restSec ?? null,
      sets: (e.sets || []).map(s => ({
        kg: s.kg ?? null, reps: s.reps ?? null, mins: s.mins ?? null, km: s.km ?? null,
        done: s.done !== false, prW: false, prE: false,
      })),
    })),
  };
}
function normalizePayload(p) {
  if (!p || typeof p !== 'object') return null;
  return {
    workouts: (p.workouts || []).map(normW),
    routines: (p.routines || []).map(normalizeRoutine),
    settings: p.settings || null,
    delW: p.delW || [], delR: p.delR || [], delP: p.delP || [], delC: p.delC || [],
    photos: (p.photos || []).map(x => ({ id: +x.id, ts: +x.ts, wId: x.wId ?? null, note: x.note || '', kg: x.kg ?? null, remote: x.remote || '' })),
    customs: (p.customs || []).map(c => ({ name: c.name, muscle: c.muscle, equip: c.equip || 'Other' })),
    v: p.v || 0,
  };
}

export function replaceAll(rawP) {
  const p = normalizePayload(rawP); if (!p) return;
  store.workouts = p.workouts;
  store.routines = p.routines.map((r, i) => ({ ...r, pos: i }));
  store.photos = p.photos.sort((a, b) => a.ts - b.ts);
  applyCustoms(p.customs);
  if (p.settings) store.settings = Object.assign({}, store.settings, p.settings);
  store.prCache = Calc.rebuildPrs(store.workouts);
  touch(['workouts', 'routines', 'photos', 'customs', 'settings']);
}

/** Fusion-union : les suppressions gagnent, doublons par startedAt / nom / id. */
export function mergeRemote(rawP) {
  const p = normalizePayload(rawP); if (!p) return false;
  const ldW = new Set(store.delW), ldR = new Set(store.delR), ldP = new Set(store.delP), ldC = new Set(store.delC);
  const rdW = new Set(p.delW), rdR = new Set(p.delR), rdP = new Set(p.delP), rdC = new Set(p.delC);

  const keepW = store.workouts.filter(w => !rdW.has(w.startedAt));
  const keepR = store.routines.filter(r => !rdR.has(r.name));
  const keepP = store.photos.filter(ph => !rdP.has(ph.id));
  const keepC = store.customs.filter(c => !rdC.has(c.name));
  const kw = new Set(keepW.map(w => w.startedAt));
  const kr = new Set(keepR.map(r => r.name));
  const kp = new Set(keepP.map(ph => ph.id));
  const addW = p.workouts.filter(w => !ldW.has(w.startedAt) && !kw.has(w.startedAt));
  const addR = p.routines.filter(r => !ldR.has(r.name) && !kr.has(r.name));
  const addP = p.photos.filter(ph => !ldP.has(ph.id) && !kp.has(ph.id));
  const kc = new Set(keepC.map(c => c.name));
  const addC = p.customs.filter(c => !ldC.has(c.name) && !kc.has(c.name));

  if (keepW.length === store.workouts.length && keepR.length === store.routines.length &&
      keepP.length === store.photos.length && keepC.length === store.customs.length &&
      !addW.length && !addR.length && !addP.length && !addC.length) return false;

  store.workouts = [...keepW, ...addW.map(w => ({ ...w, id: nextWorkoutId() }))].sort((a, b) => a.startedAt - b.startedAt);
  store.routines = [...keepR, ...addR.map(r => ({ ...r, id: nextRoutineId(), pos: nextPos() }))];
  store.photos = [...keepP, ...addP].sort((a, b) => a.ts - b.ts);
  applyCustoms([...keepC, ...addC]);
  store.prCache = Calc.rebuildPrs(store.workouts);
  touch(['workouts', 'routines', 'photos', 'customs']);
  return true;
}

/* ============================== session / auth ============================== */

export function persistSkip() { lsSet('skipped', store.skipped); }
export function persistSessionNow() { store.session ? lsSet('session', store.session) : lsDel('session'); }

export async function applySession(session) {
  const previousAccount = store.lastAccount;
  const hadLocalData = store.workouts.length > 0 || store.routines.length > 0 || store.customs.length > 0;
  if (previousAccount && previousAccount !== session.userId) {
    // changement de compte : le local repart de zéro, le cloud garde sa copie
    wipe(false);
    store.delW = []; store.delR = []; store.delP = []; store.delC = []; store.pendingDelObjs = [];
    store.dirtyAt = 0; store.pushedTs = 0; store.lastSeenRemoteTs = 0;
  }
  store.session = session;
  store.lastAccount = session.userId;
  persistSessionNow(); persistMeta();
  if (hadLocalData && !store.dirtyAt) store.dirtyAt = Date.now(); // premier binding → push
  emit('session');
  requestSync(400);
}

export async function signOutNow() {
  const s = store.session;
  store.session = null;
  persistSessionNow();
  if (s) { try { await Cloud.logout(s); } catch {} }
  emit('session');
}

const stamp = s => `${s} · ${new Date().toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })}`;

export async function syncNow() {
  if (!store.session || store.syncing) return;
  store.syncing = true;
  try {
    let s = store.session;
    if (Cloud.isSessionExpired(s)) {
      const refreshed = await Cloud.refreshSession(s.refresh).catch(() => null);
      if (!refreshed) { await signOutNow(); return; }
      s = refreshed; store.session = refreshed; persistSessionNow();
    }
    // purge des suppressions distantes en attente (indépendante du dirty)
    for (const path of [...store.pendingDelObjs]) {
      const ok = await Cloud.deleteStorageObject(store.session, `progress/${path}`).catch(() => false);
      if (ok) store.pendingDelObjs = store.pendingDelObjs.filter(x => x !== path);
    }
    const { payload: rawRemote, clientTs } = await Cloud.pullSnapshot(store.session);
    const remote = normalizePayload(rawRemote);
    const dirty = store.dirtyAt > store.pushedTs;
    if (!dirty) {
      if (remote && clientTs > store.lastSeenRemoteTs) {
        const oldAvatar = store.settings.avatarUrl;
        replaceAll(remote);
        store.delW = remote.delW; store.delR = remote.delR; store.delP = remote.delP; store.delC = remote.delC;
        store.lastSeenRemoteTs = clientTs; store.pushedTs = clientTs; store.dirtyAt = clientTs;
        persistMeta();
        maybeFetchAvatar(oldAvatar);
        fetchMissingPhotos();
        store.syncStatus = stamp('Synchronisé');
      }
    } else {
      if (remote && clientTs > store.lastSeenRemoteTs) {
        mergeRemote(remote);
        for (const t of remote.delW) if (!store.delW.includes(t)) store.delW.push(t);
        for (const t of remote.delR) if (!store.delR.includes(t)) store.delR.push(t);
        for (const t of remote.delP) if (!store.delP.includes(t)) store.delP.push(t);
        for (const t of remote.delC) if (!store.delC.includes(t)) store.delC.push(t);
        store.lastSeenRemoteTs = clientTs;
      }
      // upload des pixels d'abord pour que le snapshot porte les chemins distants
      const changed = await uploadPendingPhotos();
      if (changed) persistAll();
      const ts = Math.max(Date.now(), store.lastSeenRemoteTs + 1);
      const stored = await Cloud.pushSnapshot(store.session, snapshot(), ts);
      store.pushedTs = ts; store.dirtyAt = ts; store.lastSeenRemoteTs = stored;
      persistMeta();
      fetchMissingPhotos();
      store.syncStatus = stamp('Synchronisé');
    }
  } catch (e) {
    store.syncStatus = stamp(`Sync échouée (${String(e && e.message || e).slice(0, 60)})`);
  } finally {
    store.syncing = false;
    emit('sync');
  }
}

/** Max 8 uploads par cycle (Cloud.kt). */
async function uploadPendingPhotos() {
  if (!store.session) return false;
  let changed = false;
  const pending = store.photos.filter(p => !p.remote).slice(0, 8);
  for (const p of pending) {
    const blob = await photoBlob(p.id);
    if (!blob) continue;
    const ok = await Cloud.uploadProgressPhoto(store.session, p.id, blob).catch(() => false);
    if (ok) { setPhotoRemote(p.id, `${store.session.userId}/ph_${p.id}.jpg`); changed = true; }
  }
  return changed;
}

async function fetchMissingPhotos() {
  const missing = [];
  for (const p of store.photos) {
    if (p.remote && !(await photoBlob(p.id))) missing.push(p);
  }
  for (const p of missing) {
    const buf = await Cloud.downloadProgressPhoto(store.session, p.remote).catch(() => null);
    if (buf) await savePhotoBytes(p.id, new Blob([buf], { type: 'image/jpeg' }));
  }
  if (missing.length) emit('photos');
}

async function maybeFetchAvatar(previousUrl) {
  const url = store.settings.avatarUrl;
  if (!url || url === previousUrl) return;
  const blob = await Cloud.downloadUrlBlob(url).catch(() => null);
  if (blob) { await setAvatarBlob(blob); }
}

/* ============================== requêtes d'affichage ============================== */
export const prFor = name => store.prCache[name] || null;
export const e1rmSeries = name => Calc.e1rmSeries(name, store.workouts);

let prevCache = {};
export function prevFor(name) {
  if (name in prevCache) { const v = prevCache[name]; return v && v.length ? v : null; }
  let result = null;
  for (const w of workoutsDesc()) {
    const ex = w.exercises.find(e => e.name === name && e.sets.some(s => s.kg != null || s.reps != null || s.mins != null || s.km != null));
    if (ex) {
      const cardio = Calc.isCardioName(name);
      result = ex.sets.map(s => {
        if (s.kg == null && s.reps == null && s.mins == null && s.km == null) return '—';
        if (cardio) return Calc.fmtCardioSet(s.mins, s.km);
        return `${Calc.fmtKg(s.kg, store.settings.unit)}${Calc.unitLabel(store.settings.unit)} × ${s.reps ?? '—'}`;
      });
      break;
    }
  }
  prevCache[name] = result;
  return result;
}
export function prevSetsBefore(beforeMs, name) {
  const sorted = [...store.workouts].sort((a, b) => b.startedAt - a.startedAt);
  for (const w of sorted) {
    if (w.startedAt >= beforeMs) continue;
    const ex = w.exercises.find(e => e.name === name && e.sets.some(s => s.kg != null || s.reps != null || s.mins != null || s.km != null));
    if (!ex) continue;
    const cardio = Calc.isCardioName(name);
    return ex.sets.map(s => {
      if (s.kg == null && s.reps == null && s.mins == null && s.km == null) return '—';
      if (cardio) return Calc.fmtCardioSet(s.mins, s.km);
      return `${Calc.fmtKg(s.kg, store.settings.unit)}${Calc.unitLabel(store.settings.unit)} × ${s.reps ?? '—'}`;
    });
  }
  return null;
}
export function routineLastPerformed(r) {
  return (workoutsDesc().find(w => w.name === r.name && w.exercises.some(e => r.exercises.some(re => re.name === e.name))) || {}).startedAt ?? null;
}

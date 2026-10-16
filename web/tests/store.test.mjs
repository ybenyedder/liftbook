// Tests des boucliers store : un localStorage corrompu ne doit jamais tuer le boot
// (audit P1-3 vecteur 5) et les valeurs numériques démesurées doivent être bornées.
// store.js ne touche localStorage que dans les fonctions appelées : on peut plugger
// un shim AVANT les appels, l'import hoisté ne pose pas problème.
var calls = {};
globalThis.localStorage = {
  getItem: (k) => (k in calls ? calls[k] : null),
  setItem: (k, v) => { calls[k] = String(v); },
  removeItem: (k) => { delete calls[k]; },
};
import * as storeMod from '../src/store.js';
import { store } from '../src/store.js';

function seed(obj) { calls = {}; for (const [k, v] of Object.entries(obj)) localStorage.setItem(k, JSON.stringify(v)); }

export async function testCorruptedWorkoutDroppedNotWhitePage() {
  // un élément non-objet fait lever le normalisateur → sané = jeté + signal 'repaired'
  let repaired = false;
  storeMod.on('repaired', () => { repaired = true; });
  seed({ 'lb.workouts': [null], 'lb.draft': null });
  storeMod.init();
  if (repaired !== true) throw new Error("le signal 'repaired' n'a pas été émis");
  if (store.workouts.length !== 0) throw new Error('la donnée corrompue n\'a pas été jetée');
  // le boot a toutefois produit un store cohérent
  if (!Array.isArray(store.routines) || !Array.isArray(store.photos)) throw new Error('store incohérent après réparation');
}

export async function testHugeWeightsClamped() {
  const w = { id: 1, name: 'x', startedAt: 1000, endedAt: 2000, uTs: 0, exercises: [{ name: 'Squat', muscle: 'Quads', sets: [{ kg: 1e308, reps: 5, mins: Infinity, km: 1e308, done: true }] }] };
  seed({ 'lb.workouts': [w] });
  storeMod.init();
  const s = store.workouts[0].exercises[0].sets[0];
  if (s.kg !== null || s.mins !== null || s.km !== null) throw new Error(`bornes non appliquées : kg=${s.kg} mins=${s.mins} km=${s.km}`);
  if (s.reps !== 5) throw new Error('valeur légitime altérée');
}

export async function testInitSucceedsWithEmptyStorage() {
  calls = {};
  storeMod.init();
  if (store.workouts.length !== 0) throw new Error('boot sur stockage vide devrait donner 0 séance');
}

export async function testDraftNormalizeKeepsShape() {
  const d = { mode: 'workout', routineId: null, name: 'Séance du jour', startedAt: 123456789, notes: '', exercises: [{ name: 'Squat', muscle: 'Quads', sets: [{ kg: 80, reps: 8, done: true }] }] };
  seed({ 'lb.draft': d });
  storeMod.init();
  if (!store.draft || store.draft.mode !== 'workout' || store.draft.startedAt !== 123456789) throw new Error('brouillon sain altéré');
  if (store.draft.exercises[0].sets[0].kg !== 80 || store.draft.exercises[0].sets[0].done !== true) throw new Error('série du brouillon altérée');
}

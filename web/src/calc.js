// Port JS fidèle de la logique pure de l'app Android (data/Calc.kt + exportCsv de ui/Util.kt).
// Aucune dépendance, aucune API DOM — module ES pur testable sous Node.
import { EXERCISES, NAME_FR } from './data.js';

const LB = 2.2046226; // Livre → kg (const LB de Calc.kt)

// ---------- catalogue (EX = EXERCISES.associateBy { name }) ----------
const EX = new Map(EXERCISES.map((e) => [e.name, e]));

// Défs d'exercices custom enregistrées au démarrage (priorité sur le catalogue, comme Repo côté Android).
let CUSTOM_DEFS = [];
export function setCustomDefs(defs) { CUSTOM_DEFS = Array.isArray(defs) ? defs : []; }

// ---------- noms de mois/jours France (Locale.FRANCE, CLDR) ----------
const MONTHS_SHORT_FR = ['janv.', 'févr.', 'mars', 'avr.', 'mai', 'juin', 'juil.', 'août', 'sept.', 'oct.', 'nov.', 'déc.'];
const MONTHS_FULL_FR = ['janvier', 'février', 'mars', 'avril', 'mai', 'juin', 'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre'];
const DAYS_FR = ['lundi', 'mardi', 'mercredi', 'jeudi', 'vendredi', 'samedi', 'dimanche']; // indexé lundi = 0

// Locales des exports Hevy (EXPORT_MONTH_LOCALES) : US, FR, DE, IT, ES, PT — mois courts + complets.
const EN_MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const EXPORT_MONTH_LOCALES = [
  { short: EN_MONTHS, full: ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'] },
  { short: MONTHS_SHORT_FR, full: MONTHS_FULL_FR },
  { short: ['Jan.', 'Feb.', 'März', 'Apr.', 'Mai', 'Juni', 'Juli', 'Aug.', 'Sep.', 'Okt.', 'Nov.', 'Dez.'], full: ['Januar', 'Februar', 'März', 'April', 'Mai', 'Juni', 'Juli', 'August', 'September', 'Oktober', 'November', 'Dezember'] },
  { short: ['gen.', 'feb.', 'mar.', 'apr.', 'mag.', 'giu.', 'lug.', 'ago.', 'set.', 'ott.', 'nov.', 'dic.'], full: ['gennaio', 'febbraio', 'marzo', 'aprile', 'maggio', 'giugno', 'luglio', 'agosto', 'settembre', 'ottobre', 'novembre', 'dicembre'] },
  { short: ['ene.', 'feb.', 'mar.', 'abr.', 'may.', 'jun.', 'jul.', 'ago.', 'sept.', 'oct.', 'nov.', 'dic.'], full: ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'] },
  { short: ['jan.', 'fev.', 'mar.', 'abr.', 'mai.', 'jun.', 'jul.', 'ago.', 'set.', 'out.', 'nov.', 'dez.'], full: ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'] },
];

// ---------- fabriques de modèles (équivalents JSON des data classes de Models.kt) ----------
function mkSet(kg = null, reps = null, o = {}) {
  return { kg, reps, mins: o.mins ?? null, km: o.km ?? null, done: o.done ?? true, prW: o.prW ?? false, prE: o.prE ?? false };
}
function mkEx(name, muscle, o = {}) {
  return { name, muscle, notes: o.notes ?? '', superset: o.superset ?? false, restSec: o.restSec ?? null, sets: o.sets ?? [] };
}
function mkWorkout(id, name, startedAt, endedAt, exercises = []) {
  return { id, name, startedAt, endedAt, exercises, prs: [], notes: '' };
}
function mkRoutine(id, name, exercises = [], pos = 0) {
  return { id, name, exercises, pos };
}

// ---------- petits utilitaires numériques (sémantique Kotlin) ----------
// Math.round de Java/Kotlin : half-up, identique à Math.round JS.
function r1(x) { return Math.round(x * 10) / 10.0; }
// Max borné — remplace tout Math.max(...spread) : un spread sur des milliers d'éléments
// fait exploser la pile d'appels (RangeError au boot sur un historique/import massif).
export function maxOf(arr) {
  let m = -Infinity;
  for (const v of arr) if (Number.isFinite(v) && v > m) m = v;
  return m;
}
// trim : entier sans décimale, sinon 1 décimale max (format US, %.1f half-up ≈ toFixed(1)).
function trim(x) { const r = Math.round(x); return r === x ? String(r) : x.toFixed(1); }
// toDoubleOrNull : parse flottant strict (comme Double.parseDouble, espaces trimmed).
function toDoubleOrNull(s) {
  if (typeof s !== 'string') return null;
  const t = s.trim();
  if (t === '') return null;
  if (!/^[+-]?(?:(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?|Infinity|NaN)$/.test(t)) return null;
  return Number(t);
}
// toIntOrNull : parse entier 32 bits strict (Kotlin Int).
function toIntOrNull(s) {
  if (typeof s !== 'string') return null;
  const t = s.trim();
  if (!/^[+-]?\d+$/.test(t)) return null;
  const v = Number(t);
  if (!Number.isSafeInteger(v) || v > 2147483647 || v < -2147483648) return null;
  return v;
}
// toLongOrNull : parse entier 64 bits (utilisé pour les epochs).
function toLongOrNull(s) {
  if (typeof s !== 'string') return null;
  const t = s.trim();
  if (!/^[+-]?\d+$/.test(t)) return null;
  const v = Number(t);
  if (!Number.isSafeInteger(v)) return null; // écart assumé : epochs > 2^53 non représentables en JS
  return v;
}
// Groupage de milliers %,d Locale.FRANCE : séparateur = espace insécable fine U+202F (CLDR).
function groupFr(v) {
  const neg = v < 0 ? '-' : '';
  const s = String(Math.abs(v));
  let out = '';
  for (let i = 0; i < s.length; i++) {
    if (i > 0 && (s.length - i) % 3 === 0) out += ' ';
    out += s[i];
  }
  return neg + out;
}
const pad2 = (n) => String(n).padStart(2, '0');

// ---------- calculs ----------

// Estimation 1RM (Epley) : kg × (1 + reps/30).
export function e1rm(kg, reps) { return reps > 0 ? kg * (1 + reps / 30.0) : kg; }

// Arrondit au multiple de 1,25 kg le plus proche (pas de disques).
export function round125(x) { return Math.round(x / 1.25) * 1.25; }

// Volume total : somme kg × reps des séries terminées avec charge et reps.
export function vol(w) {
  let s = 0;
  for (const ex of (w.exercises ?? []))
    for (const it of (ex.sets ?? []))
      if ((it.done ?? true) && it.kg != null && it.reps != null && Number.isFinite(it.kg) && Number.isFinite(it.reps)) s += it.kg * it.reps;
  return s;
}

// Total des répétitions des séries terminées.
export function reps(w) {
  let s = 0;
  for (const ex of (w.exercises ?? []))
    for (const it of (ex.sets ?? []))
      if ((it.done ?? true) && it.reps != null && Number.isFinite(it.reps)) s += it.reps;
  return s;
}

// Nombre de séries valides (force ou cardio).
export function setsDone(w) {
  let n = 0;
  for (const ex of (w.exercises ?? []))
    for (const it of (ex.sets ?? []))
      if ((it.done ?? true) && (it.kg != null || it.reps != null || it.mins != null || it.km != null)) n++;
  return n;
}

// ---------- formatage ----------

// Poids affiché : converti en lb si demandé, 1 décimale max, entier si rond.
export function fmtKg(kg, unit) {
  if (kg == null || !Number.isFinite(kg)) return '';
  const v = unit === 'lb' ? kg * LB : kg;
  return trim(r1(v));
}

// Libellé d'unité style Hevy : kgs/lbs → en fait kg/lbs.
export function unitLabel(unit) { return unit === 'lb' ? 'lbs' : 'kg'; }

// Cardio (muscle "Cardio") : loge minutes/km au lieu de charge × reps.
export function isCardioName(name) {
  for (const d of CUSTOM_DEFS) if (d.name === name) return d.muscle === 'Cardio';
  return EX.get(name)?.muscle === 'Cardio';
}

// « 22min · 5.2km » pour une série cardio ; « — » si vide.
export function fmtCardioSet(mins, km) {
  const parts = [];
  if (mins != null && mins > 0) parts.push(mins >= 60 ? `${Math.trunc(mins / 60)}h${mins % 60 > 0 ? pad2(mins % 60) : ''}` : `${mins}min`);
  if (km != null && km > 0) parts.push(trim(km) + 'km');
  return parts.length === 0 ? '—' : parts.join(' · ');
}

// Nombre brut (1 décimale max) pour les champs de saisie.
export function trimNum(x) { return trim(x); }

// Décomposition gloutonne des disques par côté pour une charge cible (barre incluse).
export function platesForSide(targetKg, barKg, unit) {
  const avail = unit === 'lb' ? [45.0, 35.0, 25.0, 10.0, 5.0, 2.5] : [25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25];
  // barKg == 0 = « sans barre » — tout va sur les disques.
  let perSide = Math.trunc(((targetKg - barKg) / 2) * 100) / 100;
  if (perSide < 0) perSide = 0.0;
  const out = [];
  let sum = 0;
  for (const p of avail) {
    const n = Math.trunc(perSide / p + 1e-9);
    if (n > 0) { out.push([p, n]); perSide -= n * p; sum += p * n; }
  }
  return { perSide: out, total: barKg + 2 * sum };
}

// Parse un texte de saisie en kg (virgule acceptée, lb converti), null si invalide/nul.
// Garde anti-NaN/Infinity : la comparaison v <= 0 laisse passer NaN (NaN <= 0 = false),
// et "Infinity"/"1e308" sont acceptés par toDoubleOrNull (parité Double.parseDouble Kotlin).
export function toKg(text, unit) {
  const v = toDoubleOrNull(String(text).replace(/,/g, '.'));
  if (v == null || !Number.isFinite(v) || v <= 0 || v > 1e5) return null;
  return unit === 'lb' ? v / LB : v;
}

// Volume formaté avec séparateur de milliers français (U+202F).
export function fmtVol(kg, unit) { return Number.isFinite(kg) ? groupFr(Math.round(unit === 'lb' ? kg * LB : kg)) : '0'; }

// Conversion brute kg → unité d'affichage.
export function toDisplay(kg, unit) { return unit === 'lb' ? kg * LB : kg; }

// Durée « 1h 05m » / « 5m », jamais négative.
export function fmtDur(ms) {
  const totalMin = Math.floor(Math.max(0, ms) / 60000);
  const h = Math.trunc(totalMin / 60);
  const m = totalMin % 60;
  return h > 0 ? `${h}h ${m}m` : `${m}m`;
}

// Horloge live « 04:37 » ou « 1:12:45 » au-delà d'une heure, jamais négative.
export function fmtClock(ms) {
  const s = Math.floor(Math.max(0, ms) / 1000);
  const h = Math.trunc(s / 3600);
  const m = Math.trunc((s % 3600) / 60);
  const sec = s % 60;
  return h > 0 ? `${h}:${pad2(m)}:${pad2(sec)}` : `${pad2(m)}:${pad2(sec)}`;
}

// ---------- dates (fuseau LOCAL, comme ZoneId.systemDefault) ----------

// LocalDate Kotlin → Date JS locale à minuit.
export function localDate(ms) { const d = new Date(ms); return new Date(d.getFullYear(), d.getMonth(), d.getDate()); }

// LocalDate.toString() → clé ISO « 2026-09-18 ».
export function dayKey(ms) { const d = localDate(ms); return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`; }

// « 18 sept. » (d MMM, Locale.FRANCE, sans année).
export function fmtDateShort(ms) { const d = localDate(ms); return `${d.getDate()} ${MONTHS_SHORT_FR[d.getMonth()]}`; }

// « vendredi 18 septembre 2026 » (EEEE d MMMM yyyy — pas de capitalisation en Kotlin).
export function fmtDateFull(ms) { const d = localDate(ms); return `${DAYS_FR[(d.getDay() + 6) % 7]} ${d.getDate()} ${MONTHS_FULL_FR[d.getMonth()]} ${d.getFullYear()}`; }

// « 17:30 » (HH:mm local).
export function fmtTime(ms) { const d = new Date(ms); return `${pad2(d.getHours())}:${pad2(d.getMinutes())}`; }

// Nom du jour complet capitalisé (Home.kt dayName, langue FR).
export function dayName(ms) { const s = DAYS_FR[(localDate(ms).getDay() + 6) % 7]; return s[0].toUpperCase() + s.slice(1); }

// Mois court FR (« sept. ») — ajout web pour axes de graphiques (pas de contrepartie Kotlin).
export function monthShort(ms) { return MONTHS_SHORT_FR[localDate(ms).getMonth()]; }

// Mois complet + année (« septembre 2026 ») — ajout web pour titres mensuels.
export function monthLabel(ms) { const d = localDate(ms); return `${MONTHS_FULL_FR[d.getMonth()]} ${d.getFullYear()}`; }

// Minuit (local) du lundi de la semaine de ms.
export function weekStart(ms) {
  let d = localDate(ms);
  while (d.getDay() !== 1) d = new Date(d.getFullYear(), d.getMonth(), d.getDate() - 1);
  return d.getTime();
}

// Série de jours consécutifs avec séance (tolère « pas encore aujourd'hui »).
export function streak(workouts, nowMs) {
  const days = new Set((workouts ?? []).map((w) => dayKey(w.startedAt ?? 0)));
  // On marche en jours calendaires (pas en pas de 24 h) pour survivre aux changements d'heure.
  let d = localDate(nowMs);
  if (!days.has(isoOf(d))) d = new Date(d.getFullYear(), d.getMonth(), d.getDate() - 1);
  let n = 0;
  while (days.has(isoOf(d))) { n++; d = new Date(d.getFullYear(), d.getMonth(), d.getDate() - 1); }
  return n;
}
function isoOf(d) { return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`; }

// Volume cumulé de toutes les séances.
export function totalVol(workouts) { return (workouts ?? []).reduce((a, w) => a + vol(w), 0); }

// Stats de la semaine en cours (depuis lundi minuit).
export function weekStats(workouts, nowMs) {
  const ws = weekStart(nowMs);
  const inWeek = (workouts ?? []).filter((w) => (w.startedAt ?? 0) >= ws);
  return {
    count: inWeek.length,
    vol: inWeek.reduce((a, w) => a + vol(w), 0),
    reps: inWeek.reduce((a, w) => a + reps(w), 0),
    prs: inWeek.reduce((a, w) => a + ((w.prs ?? []).length), 0),
  };
}

// Série des n derniers volumes hebdo : paires [label lundi, volume affiché].
export function weekly(workouts, n, unit, nowMs) {
  const ws = weekStart(nowMs);
  const out = [];
  for (let i = n - 1; i >= 0; i--) {
    const start = ws - i * 7 * 86400000;
    const end = start + 7 * 86400000;
    const v = (workouts ?? []).filter((w) => (w.startedAt ?? 0) >= start && (w.startedAt ?? 0) < end).reduce((a, w) => a + vol(w), 0);
    out.push([fmtDateShort(start), toDisplay(v, unit)]);
  }
  return out;
}

// Répartition du volume par muscle (top n, tri décroissant stable).
export function muscleDist(workouts, n) {
  const m = new Map();
  for (const w of (workouts ?? [])) for (const ex of (w.exercises ?? [])) for (const s of (ex.sets ?? [])) {
    if (!(s.done ?? true) || s.kg == null || s.reps == null) continue;
    m.set(ex.muscle, (m.get(ex.muscle) ?? 0) + s.kg * s.reps);
  }
  return [...m.entries()].sort((a, b) => b[1] - a[1]).slice(0, n);
}

// Derniers PRs : triplets [PrRec, startedAt, nom séance], séances récentes d'abord.
export function recentPrs(workouts, n) {
  const out = [];
  for (const w of [...(workouts ?? [])].sort((a, b) => (b.startedAt ?? 0) - (a.startedAt ?? 0)))
    for (const p of (w.prs ?? [])) out.push([p, w.startedAt, w.name]);
  return out.slice(0, n);
}

// Série temporelle du meilleur 1RM estimé d'un exercice (12 derniers points).
export function e1rmSeries(name, workouts, maxPts = 12) {
  const pts = [];
  for (const w of [...(workouts ?? [])].sort((a, b) => (a.startedAt ?? 0) - (b.startedAt ?? 0))) {
    const ex = (w.exercises ?? []).find((e) => e.name === name);
    if (!ex) continue;
    const cands = (ex.sets ?? []).filter((it) => (it.kg ?? 0) > 0 && (it.reps ?? 0) > 0 && Number.isFinite(it.kg) && Number.isFinite(it.reps));
    if (cands.length === 0) continue; // maxOfOrNull → null → pas de point
    pts.push([fmtDateShort(w.startedAt), maxOf(cands.map((it) => e1rm(it.kg, it.reps)))]);
  }
  return pts.slice(-maxPts);
}

// Dernière séance contenant cet exercice → libellés « 80 × 8 » par série (null si jamais fait).
export function prevFor(name, workouts) {
  for (const w of [...(workouts ?? [])].sort((a, b) => (b.startedAt ?? 0) - (a.startedAt ?? 0))) {
    const ex = (w.exercises ?? []).find((e) => e.name === name && (e.sets ?? []).some((s) => s.kg != null || s.reps != null));
    if (ex) {
      return (ex.sets ?? []).map((s) =>
        s.kg == null && s.reps == null ? '—' : `${fmtKg(s.kg ?? null, 'kg')} × ${s.reps ?? '—'}`);
    }
  }
  return null;
}

// Recalcule le cache de PRs, statue les drapeaux par série et les listes prs par séance (chronologique). MUTATE workouts.
export function rebuildPrs(workouts) {
  const cache = {};
  for (const w of [...(workouts ?? [])].sort((a, b) => (a.startedAt ?? 0) - (b.startedAt ?? 0))) {
    const prs = [];
    for (const ex of (w.exercises ?? [])) {
      const prev = cache[ex.name];
      const pw = prev ? prev.weight : 0.0;
      const pe = prev ? prev.e1rm : 0.0;
      for (const s of (ex.sets ?? [])) {
        s.prW = false; s.prE = false;
        const kg = s.kg ?? null; if (kg == null) continue;
        const r = s.reps ?? null; if (r == null) continue;
        if (!(s.done ?? true)) continue;
        if (kg > 0 && kg > pw) s.prW = true;
        if (kg > 0 && e1rm(kg, r) > pe) s.prE = true;
      }
      const done = (ex.sets ?? []).filter((s) => s.kg != null && s.reps != null && (s.done ?? true) && s.kg > 0 && Number.isFinite(s.kg) && Number.isFinite(s.reps));
      if (done.length > 0) {
        const bw = maxOf(done.map((s) => s.kg));
        const be = maxOf(done.map((s) => e1rm(s.kg, s.reps)));
        if (bw > pw) prs.push({ ex: ex.name, kind: 'Weight', value: bw });
        if (be > pe) prs.push({ ex: ex.name, kind: 'Est. 1RM', value: be });
        const oldW = prev ? prev.weight : 0.0;
        const oldE = prev ? prev.e1rm : 0.0;
        cache[ex.name] = {
          weight: bw > oldW ? bw : oldW,
          weightDate: bw > oldW ? w.startedAt : (prev ? prev.weightDate : w.startedAt),
          e1rm: be > oldE ? be : oldE,
          e1rmDate: be > oldE ? w.startedAt : (prev ? prev.e1rmDate : w.startedAt),
        };
      }
    }
    w.prs = prs;
  }
  return cache;
}

// ---------- import CSV « export maison » ----------
// Format (export) : Date;Heure;Exercice;Serie;KG;Reps[;Min;Km] — « ; » ou « , » acceptés.
export function parseCsv(content) {
  const groups = new Map(); // clé "date\u0001time" → Map(exName → lignes), ordre d'insertion (LinkedHashMap)
  const lines = String(content).split(/\r\n|\r|\n/); // équivalent String.lines()
  for (const line of lines.slice(1)) {
    if (line.trim() === '') continue; // isNotBlank
    const sep = line.includes(';') ? ';' : ',';
    const parts = line.split(sep);
    if (parts.length < 6) continue;
    const date = parts[0].trim(); const time = parts[1].trim(); const exName = parts[2].trim();
    const si = toIntOrNull(parts[3].trim()); if (si === null) continue;
    // bornes anti-valeurs absurdes (négatif/NaN/Infinity) : null plutôt que corrompre les stats
    const kgRaw = toDoubleOrNull(parts[4].trim().replace(/,/g, '.'));
    const kg = kgRaw != null && kgRaw > 0 && Number.isFinite(kgRaw) ? kgRaw : null;
    const repsRaw = toIntOrNull(parts[5].trim());
    const reps = repsRaw != null && repsRaw > 0 ? repsRaw : null;
    const minsCell = parts[6] != null ? parts[6].trim() : null;
    const mins = minsCell && minsCell !== '' ? toDoubleOrNull(minsCell.replace(/,/g, '.')) : null;
    const minsI = mins == null || mins <= 0 ? null : Math.trunc(mins); // .toInt() tronque
    const kmCell = parts[7] != null ? parts[7].trim() : null;
    const km = kmCell && kmCell !== '' ? (() => { const v = toDoubleOrNull(kmCell.replace(/,/g, '.')); return v != null && v > 0 ? v : null; })() : null;
    const key = date + '\u0001' + time;
    if (!groups.has(key)) groups.set(key, new Map());
    const exMap = groups.get(key);
    if (!exMap.has(exName)) exMap.set(exName, []);
    exMap.get(exName).push({ si, kg, reps, mins: minsI, km });
  }
  const fmtFr = (s) => { // « d MMM yyyy » Locale.FRANCE (mois courts FR stricts)
    const m = s.match(/^(\d{1,2}) (\S+) (\d{4})$/);
    if (!m) return null;
    const mo = MONTHS_SHORT_FR.findIndex((x) => x.toLowerCase() === m[2].toLowerCase());
    if (mo < 0) return null;
    const day = +m[1];
    const d = new Date(+m[3], mo, day);
    if (day < 1 || day > 31 || d.getDate() !== day || d.getMonth() !== mo) return null; // calendrier valide (resolver SMART)
    return d.getTime();
  };
  const sorted = [...groups.entries()]
    .map(([k, exMap]) => [k.split('\u0001'), exMap])
    .sort((a, b) => (a[0][0] < b[0][0] ? -1 : a[0][0] > b[0][0] ? 1 : 0)); // tri lexicographique de la chaîne date
  return sorted.map(([key, exMap], i) => {
    const ms = fmtFr(key[0]) ?? Date.now() - (groups.size - i) * 3600000; // repli : échelonné autour de maintenant
    const exercises = [...exMap.entries()].map(([frName, sets]) => {
      const canonical = Object.entries(NAME_FR).find(([, v]) => v === frName)?.[0] ?? frName; // nom FR → clé EN
      const muscle = EX.get(canonical)?.muscle ?? '';
      return mkEx(canonical, muscle, { sets: [...sets].sort((a, b) => a.si - b.si).map((r) => mkSet(r.kg, r.reps, { mins: r.mins, km: r.km, done: true })) });
    });
    return mkWorkout(10000000 + i, 'Séance', ms, ms + 3600000, exercises);
  });
}

// ---------- import Hevy / Strong ----------

// Date « d MMM yyyy, HH:mm » selon la langue de l'app Hevy (EN-FR-DE-IT-ES-PT + japonais « 5 9月 »).
function parseLocalizedDate(s) {
  const pats = [/^(\d{1,2}) (\S+) (\d{4}), (\d{2}):(\d{2})$/, /^(\d{1,2}) (\S+) (\d{4}), (\d{2}):(\d{2})$/, /^(\d{1,2}) (\S+) (\d{4}) (\d{2}):(\d{2})$/];
  const styles = ['short', 'full', 'short'];
  for (const loc of EXPORT_MONTH_LOCALES) {
    for (let k = 0; k < pats.length; k++) {
      const m = s.match(pats[k]);
      if (!m) continue;
      const mo = loc[styles[k]].findIndex((x) => x.toLowerCase() === m[2].toLowerCase());
      if (mo < 0) continue;
      const d = new Date(+m[3], mo, +m[1], +m[4], +m[5]);
      if (+m[1] < 1 || +m[1] > 31 || d.getDate() !== +m[1] || d.getMonth() !== mo || +m[4] > 23 || +m[5] > 59) continue;
      return d.getTime(); // interprétée dans le fuseau local
    }
  }
  // Forme japonaise « 5 9月 2026, 20:39 » → mois EN puis parse US.
  const jp = s.replace(/(\d{1,2})\s*月/g, (_, dd) => EN_MONTHS[Math.min(12, Math.max(1, +dd)) - 1]);
  if (jp !== s) {
    const m = jp.match(/^(\d{1,2}) (\S+) (\d{4}), (\d{2}):(\d{2})$/);
    if (m) {
      const mo = EN_MONTHS.findIndex((x) => x.toLowerCase() === m[2].toLowerCase());
      if (mo >= 0) {
        const d = new Date(+m[3], mo, +m[1], +m[4], +m[5]);
        if (d.getDate() === +m[1] && d.getMonth() === mo) return d.getTime();
      }
    }
  }
  return null;
}

// ISO-8601 avec décalage (Z, +01:00, +0100, +01) → epoch ms ; null si pas de décalage valide.
function parseOffsetIso(str) {
  const m = str.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2})(\.\d+)?)?(Z|[+-]\d{2}(?::?\d{2})?)$/);
  if (!m) return null;
  const y = +m[1], mo = +m[2] - 1, d = +m[3], h = +m[4], mi = +m[5], sec = m[6] ? +m[6] : 0;
  const ms = m[7] ? Math.trunc(parseFloat(m[7]) * 1000) : 0; // nanos → ms tronqués comme toEpochMilli
  if (mo < 0 || mo > 11 || h > 23 || mi > 59 || sec > 59) return null;
  const utc = Date.UTC(y, mo, 1, h, mi, sec, ms) + (d - 1) * 86400000; // jour ajouté hors DST (UTC)
  const chk = new Date(utc);
  if (chk.getUTCMonth() !== mo || chk.getUTCDate() !== d || d < 1 || d > 31) return null;
  if (m[8] === 'Z') return utc;
  const om = m[8].match(/^([+-])(\d{2})(?::?(\d{2}))?$/);
  let off = +om[2] * 60 + (om[3] ? +om[3] : 0);
  if (om[1] === '-') off = -off;
  return utc - off * 60000;
}

// ISO-8601 date-time SANS décalage → epoch ms en fuseau local (LocalDateTime.parse).
function parseLocalIso(str) {
  const m = str.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2})(\.\d+)?)?$/);
  if (!m) return null;
  const mo = +m[2] - 1, d = +m[3], h = +m[4], mi = +m[5], sec = m[6] ? +m[6] : 0;
  const ms = m[7] ? Math.trunc(parseFloat(m[7]) * 1000) : 0;
  if (mo < 0 || mo > 11 || d < 1 || d > 31 || h > 23 || mi > 59 || sec > 59) return null;
  const dt = new Date(+m[1], mo, d, h, mi, sec, ms);
  if (dt.getDate() !== d || dt.getMonth() !== mo) return null;
  return dt.getTime();
}

// Date à la Strong « Mon Jan 02 17:00:00 GMT+01:00 2023 » (zzz = Z/GMT±hh:mm/±hh:mm).
function parseStrongDate(s) {
  const m = s.match(/^([A-Za-z]{3}) ([A-Za-z]{3}) (\d{2}) (\d{2}):(\d{2}):(\d{2}) (Z|GMT[+-]\d{2}:\d{2}|[+-]\d{2}:?\d{2}) (\d{4})$/);
  if (!m) return null;
  const mo = EN_MONTHS.findIndex((x) => x.toLowerCase() === m[2].toLowerCase());
  if (mo < 0) return null;
  const dow = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'][new Date(Date.UTC(+m[8], mo, +m[3])).getUTCDay()];
  if (dow.toLowerCase() !== m[1].toLowerCase()) return null; // le resolver SMART vérifie la cohérence EEE
  const utc = Date.UTC(+m[8], mo, +m[3], +m[4], +m[5], +m[6]);
  if (m[7] === 'Z') return utc;
  const om = m[7].match(/^GMT?([+-])(\d{2}):(\d{2})$/);
  if (!om) return null; // zone nommée non supportée (Strong exporte toujours GMT±hh:mm)
  let off = +om[2] * 60 + +om[3];
  if (om[1] === '-') off = -off;
  return utc - off * 60000;
}

// Parse une date d'export Hevy/Strong : ISO-8601 (T, espace, décalage), epoch s/ms, « d MMM yyyy, HH:mm » localisé, Strong.
export function parseExportDate(raw) {
  const t = String(raw).trim().replace(/^"+|"+$/g, ''); // trim() puis trim('"') de Kotlin
  if (t === '') return null;
  // epoch secondes / millisecondes
  const n = toLongOrNull(t);
  if (n !== null) return n > 10000000000 ? n : n * 1000;
  const iso = t.replace(/ /g, 'T');
  // décalage final type +01:00 / GMT+01:00 / Z
  let r = parseOffsetIso(iso);
  if (r !== null) return r;
  r = parseOffsetIso(iso.endsWith('Z') ? iso : iso + 'Z');
  if (r !== null) return r;
  r = parseLocalIso(iso);
  if (r !== null) return r;
  r = parseLocalizedDate(t);
  if (r !== null) return r;
  r = parseStrongDate(t);
  if (r !== null) return r;
  return null;
}

// Lecteur CSV plein fichier (guillemets avec virgules/retours-ligne intégrés) ; « ; » ou « , » acceptés.
function readCsvTable(content) {
  const rows = [];
  let cur = []; let cell = ''; let inQ = false;
  for (const c of String(content)) {
    if (c === '"') inQ = !inQ;
    else if (!inQ && (c === ',' || c === ';')) { cur.push(cell.trim()); cell = ''; }
    else if (!inQ && c === '\n') { cur.push(cell.trim()); cell = ''; rows.push(cur); cur = []; }
    else if (!inQ && c === '\r') { /* ignoré */ }
    else cell += c;
  }
  if (cur.length > 0) { cur.push(cell.trim()); rows.push(cur); } // quirk Kotlin : ligne finale mono-cellule perdue
  return rows.filter((r) => r.some((x) => x !== ''));
}

function norm(s) { return s.toLowerCase().replace(/_/g, '').replace(/ /g, '').replace(/\./g, ''); }

// Mapping flou de colonnes — en-têtes Strong ET snake_case (exercise_name, weight_kg, set_index…).
function mapColumns(header) {
  const cols = header.map((h) => norm(h));
  const findFirst = (keys, not = []) => {
    const i = cols.findIndex((c) => keys.some((k) => c.includes(k)) && not.every((x) => !c.includes(x)));
    return i >= 0 ? i : null;
  };
  const exercise = findFirst(['exercisename', 'exercise']);
  if (exercise === null) return null;
  return {
    exercise,
    date: findFirst(['startdate', 'start', 'date'], ['end', 'duration']),
    endDate: findFirst(['enddate', 'endtime']),
    weight: findFirst(['weightkg', 'weightlbs', 'weight'], ['unit']),
    reps: findFirst(['reps', 'repetitions']),
    order: findFirst(['setorder', 'setindex', 'setno']),
    seconds: findFirst(['seconds', 'durations', 'durationsec']),
    done: findFirst(['completed', 'checked']),
    name: findFirst(['workoutname', 'title', 'name'], ['exercise']),
    unit: findFirst(['weightunit', 'unit']),
    notes: findFirst(['exnotes', 'exercisenotes', 'notes'], ['workout']),
    superset: findFirst(['superset']),
  };
}

// Fait correspondre un libellé d'exercice Hevy/Strong à notre clé EN canonique (noms FR remappés, inconnus gardés).
export function canonicalExercise(label) {
  const t = label.trim();
  if (EX.has(t)) return t;
  const clean = norm(t).replace(/\(/g, '').replace(/\)/g, '');
  // Hevy FR peut omettre le suffixe parenthèse (« Tirage Poitrine » vs « Tirage Poitrine (Machine) »)
  const cleanOf = (v) => norm(v).replace(/\(/g, '').replace(/\)/g, '');
  let hit = Object.entries(NAME_FR).find(([, v]) => cleanOf(v) === clean);
  if (hit) return hit[0];
  hit = Object.entries(NAME_FR).find(([, v]) => cleanOf(v).startsWith(clean) && clean.length >= 5);
  return hit ? hit[0] : t;
}

// Parse un CSV Hevy (ou Strong) : [workouts, routines]. Une ligne = une série ; dates identiques consécutives = une séance.
// Un fichier sans aucune colonne date est traité comme définition de routine/template.
export function parseHevyCsv(content) {
  const table = readCsvTable(content);
  if (table.length < 2) return [[], []];
  const cols = mapColumns(table[0]);
  if (cols === null) return [[], []];
  const rows = [];
  for (let i = 1; i < table.length; i++) {
    const p = table[i];
    if (p.length <= cols.exercise) continue;
    const exRaw = p[cols.exercise];
    if (exRaw === '') continue;
    const unitLb = cols.unit !== null && (p[cols.unit] ?? '').toLowerCase().includes('lb');
    const kgRaw = cols.weight !== null ? toDoubleOrNull((p[cols.weight] ?? '').replace(/,/g, '.')) : null;
    const kg = kgRaw !== null && kgRaw > 0 && Number.isFinite(kgRaw) ? (unitLb ? kgRaw / LB : kgRaw) : null;
    const date = parseExportDate(cols.date !== null ? (p[cols.date] ?? '') : '');
    const doneRaw = cols.done !== null ? (p[cols.done] ?? '').toLowerCase() : 'true';
    const supCell = cols.superset !== null ? p[cols.superset] : null;
    const sup = supCell != null && supCell != undefined && supCell.trim() !== '' && supCell !== '0';
    rows.push({
      date,
      endDate: (cols.endDate !== null && p[cols.endDate] != null ? parseExportDate(p[cols.endDate]) : null) ?? date,
      ex: exRaw,
      kg,
      reps: cols.reps !== null ? (() => { const v = toDoubleOrNull(p[cols.reps] ?? ''); return v == null || v <= 0 ? null : Math.trunc(v); })() : null,
      order: (cols.order !== null ? toIntOrNull(p[cols.order] ?? '') : null) ?? rows.length,
      done: !['false', '0', 'no', 'warmup'].includes(doneRaw),
      workoutName: (cols.name !== null ? (p[cols.name] || null) : null),
      notes: cols.notes !== null ? (p[cols.notes] ?? '') : '',
      superset: sup,
    });
  }
  if (rows.length === 0) return [[], []];

  // Aucune date nulle part → fichier template/routine
  if (rows.every((r) => r.date === null)) {
    const exs = new Map();
    const sups = new Set();
    for (const r of rows) {
      if (!exs.has(r.ex)) exs.set(r.ex, []);
      exs.get(r.ex).push(mkSet(r.kg, r.reps, { done: true }));
      if (r.superset) sups.add(r.ex);
    }
    const routine = mkRoutine(0, rows[0].workoutName ?? 'Routine Hevy',
      [...exs.entries()].map(([label, sets]) => {
        const canon = canonicalExercise(label);
        return mkEx(canon, EX.get(canon)?.muscle ?? '', { superset: sups.has(label), sets });
      }));
    return [[], [routine]];
  }

  // Séances : groupées par date (blocs consécutifs de même date)
  const workouts = [];
  let i = 0;
  let id = 10000000;
  while (i < rows.length) {
    const key = rows[i].date;
    if (key === null) { i++; continue; }
    let j = i;
    const exs = new Map();
    const names = new Map();
    const sups = new Set();
    while (j < rows.length && rows[j].date === key) {
      const row = rows[j];
      if (!exs.has(row.ex)) exs.set(row.ex, []);
      exs.get(row.ex).push(mkSet(row.kg, row.reps, { done: row.done }));
      if (row.workoutName != null) names.set(row.ex, row.workoutName);
      if (row.superset) sups.add(row.ex);
      j++;
    }
    const end = rows.find((r) => r.date === key)?.endDate ?? key;
    workouts.push(mkWorkout(
      id++,
      names.size > 0 ? [...names.values()][0] : 'Séance Hevy',
      key,
      Math.max(end, key + 60000),
      [...exs.entries()].map(([label, sets]) => {
        const canon = canonicalExercise(label);
        return mkEx(canon, EX.get(canon)?.muscle ?? '', { superset: sups.has(label), sets });
      }),
    ));
    i = j;
  }
  return [workouts.sort((a, b) => a.startedAt - b.startedAt), []];
}

// Hevy n'exporte jamais les templates : reconstruit une routine par nom de séance distinct
// (insensible à la casse, noms de routines existants skippés), mirant la dernière occurrence.
export function routinesFromWorkouts(workouts, existingRoutineNames) {
  const taken = new Set([...(existingRoutineNames ?? [])].map((n) => n.toLowerCase()));
  const out = [];
  let id = 10000000;
  const groups = new Map();
  for (const w of (workouts ?? [])) {
    const key = (w.name ?? '').trim();
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(w);
  }
  for (const [name, group] of groups) {
    if (name === '' || taken.has(name.toLowerCase())) continue;
    let latest = null;
    for (const w of group) if (latest === null || w.startedAt > latest.startedAt) latest = w; // maxByOrNull : premier max
    if (latest === null) continue;
    out.push(mkRoutine(id++, name, (latest.exercises ?? []).map((ex) => mkEx(ex.name, ex.muscle, {
      superset: ex.superset ?? false,
      restSec: ex.restSec ?? null,
      sets: (ex.sets ?? []).map((s) => mkSet(s.kg ?? null, s.reps ?? null, { mins: s.mins ?? null, km: s.km ?? null, done: s.done ?? true })),
    }))));
    taken.add(name.toLowerCase());
  }
  return out;
}

// ---------- seed de démo déterministe ----------

// LCG déterministe de Calc.kt : s = s*1103515245+12345 (mod 2^31) — Math.imul reproduit l'arithmétique exacte.
function mkRnd() {
  let s = 987654321;
  return () => {
    s = (Math.imul(s, 1103515245) + 12345) & 0x7fffffff; // (s*1103515245+12345) % 2^31, comme Long Kotlin
    return s / 2147483648.0;
  };
}

// Jeu de démo déterministe : 12 semaines de Push/Pull/Legs + 4 routines. Retourne [workouts, routines].
export function seed(nowMs) {
  const rnd = mkRnd();
  const T = {
    push: [
      { name: 'Barbell Bench Press', nSets: 4, reps: 8, base: 70.0, inc: 1.1 },
      { name: 'Incline Dumbbell Bench Press', nSets: 3, reps: 10, base: 24.0, inc: 0.35 },
      { name: 'Machine Chest Press', nSets: 3, reps: 12, base: 55.0, inc: 0.8 },
      { name: 'Lateral Raise', nSets: 3, reps: 15, base: 9.0, inc: 0.2 },
      { name: 'Tricep Pushdown', nSets: 3, reps: 12, base: 27.0, inc: 0.4 },
      { name: 'Overhead Cable Extension', nSets: 3, reps: 11, base: 22.0, inc: 0.35 },
    ],
    pull: [
      { name: 'Lat Pulldown', nSets: 4, reps: 10, base: 62.0, inc: 0.8 },
      { name: 'Seated Cable Row', nSets: 3, reps: 10, base: 57.0, inc: 0.8 },
      { name: 'Dumbbell Row', nSets: 3, reps: 10, base: 32.0, inc: 0.4 },
      { name: 'Face Pull', nSets: 3, reps: 15, base: 22.0, inc: 0.25 },
      { name: 'Barbell Curl', nSets: 3, reps: 10, base: 32.0, inc: 0.3 },
      { name: 'Hammer Curl', nSets: 3, reps: 12, base: 15.0, inc: 0.25 },
    ],
    legs: [
      { name: 'Barbell Squat', nSets: 4, reps: 8, base: 97.0, inc: 1.4 },
      { name: 'Romanian Deadlift', nSets: 3, reps: 10, base: 92.0, inc: 1.0 },
      { name: 'Leg Press', nSets: 3, reps: 12, base: 185.0, inc: 2.2 },
      { name: 'Lying Leg Curl', nSets: 3, reps: 12, base: 46.0, inc: 0.4 },
      { name: 'Standing Calf Raise', nSets: 4, reps: 15, base: 85.0, inc: 0.9 },
      { name: 'Cable Crunch', nSets: 3, reps: 15, base: 31.0, inc: 0.4 },
    ],
  };
  const names = { push: 'Push Day', pull: 'Pull Day', legs: 'Leg Day' };
  const cycle = ['push', 'pull', 'rest', 'legs', 'rest', 'push', 'pull', 'rest', 'legs', 'rest'];

  const workouts = [];
  let wid = 1;
  for (let back = 83; back >= 0; back--) {
    const kind = back < 5 ? ['push', 'pull', 'legs', 'push', 'pull'][4 - back] : cycle[(83 - back) % cycle.length];
    if (kind === 'rest') continue;
    if (back > 6 && rnd() < 0.10) continue; // séances manquées au fil des semaines
    let start;
    if (back === 0) {
      start = nowMs - Math.trunc((110 + rnd() * 30) * 60000); // séance du jour en cours
    } else {
      const cal = new Date(nowMs);
      cal.setDate(cal.getDate() - back); // Calendar.add(DAY_OF_YEAR, -back)
      cal.setHours(17 + Math.trunc(rnd() * 3), Math.trunc(rnd() * 60), 0, 0); // 17h-19h, minute aléatoire
      start = cal.getTime();
    }
    let end = start + Math.trunc((55 + rnd() * 40) * 60000);
    if (end > nowMs - 30000) end = nowMs - 30000;
    if (end <= start) continue;
    const weekIdx = Math.trunc((83 - back) / 7); // progression hebdo des charges
    const exs = T[kind].map((row) => {
      const kg = round125(row.base + row.inc * weekIdx);
      const sets = [];
      for (let i = 0; i < row.nSets; i++) {
        // ordre des rnd() identique à Kotlin : le 2e n'est tiré que si le 1er >= 0.22
        const rv = rnd() < 0.22 ? row.reps - 1 : (rnd() > 0.9 ? row.reps + 1 : row.reps);
        sets.push(mkSet(kg, Math.max(4, rv), { done: true }));
      }
      return mkEx(row.name, EX.get(row.name)?.muscle ?? 'Quads', { sets });
    });
    workouts.push(mkWorkout(wid++, names[kind], start, end, exs));
  }

  const routineRows = (rows) => rows.map((r) => mkEx(r.name, EX.get(r.name)?.muscle ?? '', {
    sets: Array.from({ length: r.nSets }, () => mkSet(round125(r.base), r.reps, { done: true })),
  }));
  let rid = 1;
  const routines = [
    mkRoutine(rid++, 'Push Day', routineRows(T.push)),
    mkRoutine(rid++, 'Pull Day', routineRows(T.pull)),
    mkRoutine(rid++, 'Leg Day', routineRows(T.legs)),
    mkRoutine(rid++, 'Upper Body', [
      mkEx('Barbell Bench Press', 'Chest', { sets: Array.from({ length: 4 }, () => mkSet(72.5, 8)) }),
      mkEx('Lat Pulldown', 'Lats', { sets: Array.from({ length: 4 }, () => mkSet(62.0, 10)) }),
      mkEx('Seated Cable Row', 'Lats', { sets: Array.from({ length: 3 }, () => mkSet(57.0, 10)) }),
      mkEx('Machine Shoulder Press', 'Shoulders', { sets: Array.from({ length: 3 }, () => mkSet(40.0, 10)) }),
      mkEx('Barbell Curl', 'Biceps', { sets: Array.from({ length: 3 }, () => mkSet(32.5, 10)) }),
      mkEx('Tricep Pushdown', 'Triceps', { sets: Array.from({ length: 3 }, () => mkSet(27.5, 12)) }),
    ]),
  ];
  return [workouts, routines];
}

// ---------- export CSV (port de exportCsv, ui/Util.kt — contenu texte, sans fichier) ----------

// Contenu texte du CSV d'export : une ligne par série, colonnes Min/Km pour le cardio, noms FR (« ; »→« , »).
export function exportCsvText(workouts, unit) {
  let out = `Date;Heure;Exercice;Serie;${unitLabel(unit)};Reps;Min;Km\n`;
  // cellule texte sûre : « ; »→« , » + neutralisation des formules tableur (préfixe ' devant = + - @)
  const csvCell = (n) => { const s = String(NAME_FR[n] ?? n ?? '').replace(/;/g, ','); return /^[=+\-@]/.test(s) ? `'${s}` : s; };
  const exName = csvCell;
  for (const w of [...(workouts ?? [])].sort((a, b) => (a.startedAt ?? 0) - (b.startedAt ?? 0))) {
    // date « d MMM yyyy » complète — parseCsv a besoin de l'année (fmtDateShort l'omettrait)
    const d = localDate(w.startedAt);
    const date = `${d.getDate()} ${MONTHS_SHORT_FR[d.getMonth()]} ${d.getFullYear()}`;
    const time = fmtTime(w.startedAt);
    for (const ex of (w.exercises ?? [])) {
      (ex.sets ?? []).forEach((st, i) => {
        out += `${date};${time};${exName(ex.name).replace(/;/g, ',')};${i + 1};` +
          `${st.kg != null ? fmtKg(st.kg, unit) : ''};${st.reps ?? ''};` +
          `${st.mins ?? ''};${st.km != null ? trimNum(st.km) : ''}\n`;
      });
    }
  }
  return out;
}

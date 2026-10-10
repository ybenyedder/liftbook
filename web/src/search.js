// Localisation FR + recherche floue — port JS fidèle de l'app Android.
// Sources : ui/L10n.kt, data/L10nData.kt (normalisation, Levenshtein, stems, NOISE_TOKENS,
// matches, equipHint FR), data/Data.kt (archetypeOf — l'ordre des conditions est critique).
// ES module pur : aucune dépendance, aucune API DOM (testable sous Node).

import {
  EXERCISES, NAME_FR, MUSCLE_FR, EQUIP_FR, MUSCLE_EXTRA, ALIAS_FR,
  EQUIP_HINT, CUES, CUES_FR, ARCH_STEPS, ARCH_STEPS_FR,
} from './data.js';

// ---------------------------------------------------------------------------
// L10n (port de ui/L10n.kt) — lang détecté via navigator, forcé dans {fr, es, de} sinon 'en' ;
// sans navigator (Node) → 'fr' par défaut. L10n.setLang(l) est le setter officiel.
// ---------------------------------------------------------------------------

function detectLang() {
  let raw = 'fr'; // sans navigator (Node) → 'fr' par défaut
  if (typeof navigator !== 'undefined' && navigator) {
    raw = String((navigator.language || 'en')).slice(0, 2).toLowerCase();
  }
  return (raw === 'fr' || raw === 'es' || raw === 'de') ? raw : 'en';
}

let _lang = detectLang();

export const L10n = {
  get lang() { return _lang; },
  set lang(v) { L10n.setLang(v); },
  setLang(l) {
    const s = String(l ?? '').slice(0, 2).toLowerCase();
    _lang = (s === 'fr' || s === 'es' || s === 'de') ? s : 'en';
  },
  /** EN par défaut, FR/ES/DE fournies ; retombée EN. */
  s(en, fr, es = null, de = null) {
    switch (_lang) {
      case 'fr': return fr;
      case 'es': return es ?? en;
      case 'de': return de ?? en;
      default: return en;
    }
  },
  /** « n série(s) nom » — logique pluriel exacte de L10n.kt. */
  seriesLabel(n, name) {
    switch (_lang) {
      case 'fr': return n > 1 ? `${n} séries ${name}` : `${n} série ${name}`;
      case 'de': return `${n} Sätze ${name}`;
      default: return `${n} series ${name}`;
    }
  },
  seeMoreExercises(n) {
    switch (_lang) {
      case 'fr': return n > 1 ? `Voir ${n} exercices en plus` : 'Voir 1 exercice en plus';
      case 'es': return `Ver ${n} ejercicios más`;
      case 'de': return `${n} weitere Übungen anzeigen`;
      default: return n > 1 ? `See ${n} more exercises` : 'See 1 more exercise';
    }
  },
  /** « il y a X min / X h / 1 jour / N jours » ; date future → nom du jour (import daté dans le futur). */
  relativeTime(ms, nowMs = Date.now()) {
    const diff = nowMs - ms;
    if (diff < 0) return dayName(ms);
    const hours = Math.floor(diff / 3600000);
    if (_lang !== 'fr') {
      if (hours < 1) return `${Math.max(1, Math.floor(diff / 60000))} min ago`;
      if (hours < 24) return `${hours} h ago`;
      const days = Math.floor(hours / 24);
      return days === 1 ? '1 day ago' : `${days} days ago`;
    }
    if (hours < 1) return `il y a ${Math.max(1, Math.floor(diff / 60000))} min`;
    if (hours < 24) return `il y a ${hours} h`;
    const days = Math.floor(hours / 24);
    return days === 1 ? 'il y a 1 jour' : `il y a ${days} jours`;
  },
};

function dayName(ms) {
  const locale = _lang === 'fr' ? 'fr-FR' : 'en-US';
  const s = new Date(ms).toLocaleDateString(locale, { weekday: 'long' });
  return s.charAt(0).toUpperCase() + s.slice(1);
}

// ---------------------------------------------------------------------------
// Résolution de noms (port de L10nData.name/muscle/equip + customs)
// ---------------------------------------------------------------------------

/** Registre des exercices custom : les customs participent à la recherche par nom/muscle/équipement. */
const builtinEx = new Map(EXERCISES.map((d) => [d.name, d]));
let customEx = new Map(); // name -> { name, muscle, equip }
let customNamesSeen = new Set();

function defOf(nameEn) {
  return builtinEx.get(nameEn) ?? customEx.get(nameEn) ?? null;
}

/** Remplace la liste des customs ({name, muscle, equip}). Un custom ne masque jamais le catalogue. */
export function setCustomDefs(defs) {
  for (const n of customNamesSeen) {
    nameWordsCache.delete(n);
    attrWordsCache.delete(n);
  }
  customEx = new Map();
  for (const d of Array.isArray(defs) ? defs : []) {
    if (!d || typeof d.name !== 'string' || d.name === '') continue;
    if (builtinEx.has(d.name)) continue; // sémantique Android registerCustomCatalog
    customEx.set(d.name, { name: d.name, muscle: String(d.muscle ?? ''), equip: String(d.equip ?? '') });
  }
  customNamesSeen = new Set(customEx.keys());
}

export function exName(nameEn) {
  return _lang === 'fr' ? (NAME_FR[nameEn] ?? nameEn) : nameEn; // un custom retombe sur son nom tel quel
}

export function muscleName(m) {
  return _lang === 'fr' ? (MUSCLE_FR[m] ?? m) : m;
}

export function equipName(e) {
  return _lang === 'fr' ? (EQUIP_FR[e] ?? e) : e;
}

export function cuesFor(muscle) {
  if (_lang === 'fr') return CUES_FR[muscle] ?? CUES[muscle] ?? [];
  return CUES[muscle] ?? [];
}

/** Instructions pas-à-pas : archétype du nom + ARCH_STEPS_FR si fr sinon ARCH_STEPS. */
export function stepsFor(nameEn) {
  const arch = archetypeOf(nameEn);
  if (_lang === 'fr') return ARCH_STEPS_FR[arch] ?? ARCH_STEPS[arch] ?? [];
  return ARCH_STEPS[arch] ?? [];
}

/** Port exact de L10nData.equipHint : EQUIP_HINT EN, variantes FR codées dans le when Kotlin. */
export function equipHintFor(e) {
  if (_lang !== 'fr') return EQUIP_HINT[e] ?? '';
  switch (e) {
    case 'Barbell': return 'Avec une barre chargée de disques.';
    case 'Dumbbell': return 'Avec des haltères — un dans chaque main sauf indication.';
    case 'Machine': return 'Sur une machine à pile ou à charges guidées.';
    case 'Cable': return 'À la station de poulies avec accessoires.';
    case 'Bodyweight': return 'Sans matériel — le corps sert de résistance.';
    default: return 'Avec du matériel spécifique : banc, élastique, kettlebell ou roue.';
  }
}

// ---------------------------------------------------------------------------
// archetypeOf — port EXACT de Data.kt (l'ordre des conditions est critique, ne pas réordonner)
// ---------------------------------------------------------------------------

export function archetypeOf(name) {
  const n = String(name).toLowerCase();
  const muscle = defOf(name)?.muscle ?? '';
  const has = (s) => n.includes(s);
  const any = (...ss) => ss.some(has);
  // --- conditioning / cardio / locomotion (en premier : évite les clashs row/squat/walking) ---
  if (has('walking lunge')) return 'lunge';
  if (any(
    'burpee', 'jumping jack', 'high knee', 'butt kick', 'jump rope', 'rowing machine',
    'treadmill', 'cycling', 'elliptical', 'stair climber', 'swimming', 'ski erg',
    'jacobs ladder', 'assault bike', 'versa climber', 'sled push', 'sled drag',
    'battle rope', 'shadow box', 'speed skater', 'lateral bound',
  )) return 'conditioning';
  if (has('walking') || has('running')) return 'conditioning';
  // --- olympique & lifts explosifs ---
  if (any(
    'power clean', 'hang clean', 'squat clean', 'clean pull', 'clean and',
    'power snatch', 'hang snatch', 'snatch pull', 'snatch balance', 'kettlebell snatch',
  )) return 'olympic';
  if (has('jerk')) return 'jerk';
  if (has('thruster')) return 'thruster';
  if (has('box jump') || has('broad jump') || has('depth jump') || has('frog jump')) return 'plyo_jump';
  // --- patterns spéciaux ---
  if (has('get up')) return 'getup';
  if (has('pallof')) return 'pallof';
  if (has('bird dog')) return 'bird_dog';
  if (has('bear crawl') || has('crab walk')) return 'crawl';
  if (has('superman') || has('snow angel')) return 'superman_hold';
  if (has('pull apart')) return 'pull_apart';
  if (has('wood chop')) return 'wood_chop';
  if (has('scaption') || has('y raise') || has('w raise')) return 'scaption';
  if (has('handstand')) return 'ohp';
  if (has('hip extension')) return 'back_ext';
  if (n === 'reverse nordic') return 'squat';
  // --- pecs ---
  if (has('incline') && (has('press') || has('bench'))) return 'incline_bench';
  if (has('decline') && (has('press') || has('bench'))) return 'decline_bench';
  if ((has('bench press') && !has('close grip')) || n === 'machine chest press' ||
    has('floor press') || n === 'svend press' || has('guillotine')) return 'bench';
  if (has('fly') || has('crossover') || has('pec deck') || has('svend')) return 'fly';
  if (has('push up') || has('pushup')) return 'pushup';
  if (has('dip') && muscle === 'Chest') return 'dip';
  // --- épaules ---
  if (has('shrug') || has('rack pull') || has('block pull')) return 'shrug';
  if (has('lateral raise')) return 'lateral_raise';
  if (has('front raise')) return 'front_raise';
  if (has('rear delt') || has('face pull') || has('reverse pec deck')) return 'rear_delt';
  if (has('upright row') || has('high pull')) return 'upright_row';
  if (has('press') && muscle === 'Shoulders') return 'ohp';
  // --- bras ---
  if (has('wrist') || has('pinch') || has('rice bucket') ||
    has('towel wring') || has('figure 8') || has('hex dumbbell')) return 'wrist';
  if (has('curl')) return 'curl';
  if ((has('pushdown') || has('kickback')) && muscle === 'Triceps') return 'triceps_pushdown';
  if (has('extension') && muscle === 'Triceps') return 'triceps_overhead';
  if (has('skullcrusher') || has('tate') || has('jm press')) return 'skullcrusher';
  if (has('close grip bench')) return 'skullcrusher';
  if (has('dip')) return 'dip';
  // --- dos ---
  if (has('straight arm')) return 'straight_arm';
  if (has('pulldown')) return 'pulldown';
  if (has('pull up') || has('pullup') || has('chin up') ||
    has('muscle up') || has('scap pull')) return 'pull_up';
  if (has('pullover')) return 'pullover';
  if (has('inverted row')) return 'pull_up';
  if (has('cable row') || has('supported row') || has('machine row') ||
    has('t-bar') || has('incline dumbbell row') || has('seal row') ||
    has('meadow row')) return 'row_supported';
  if (has('row')) return 'row_bent';
  // --- chaîne postérieure ---
  if (has('deadlift') && (has('romanian') || has('single leg'))) return 'rdl';
  if (has('deadlift')) return 'deadlift';
  if (has('good morning')) return 'good_morning';
  if ((has('extension') && muscle === 'Lower back') || has('hyperextension')) return 'back_ext';
  if (has('swing')) return 'swing';
  if (has('hip thrust') || has('glute bridge') || has('frog pump') ||
    has('pull through') || has('kickback')) return 'hip_thrust';
  if (has('leg curl') || has('nordic') || has('glute ham')) return 'leg_curl';
  // --- jambes ---
  if (has('calf') || has('tibialis') || has('on toes')) return 'calf';
  if (has('squat')) return 'squat';
  if (has('lunge') || has('split squat') || has('step up') ||
    has('curtsy') || has('step down')) return 'lunge';
  if (has('leg press')) return 'leg_press';
  if (has('leg extension') || has('knee extension')) return 'leg_ext';
  if (has('abduction') || has('clamshell') || has('fire hydrant') ||
    has('lateral walk') || has('monster walk')) return 'abduction';
  if (has('adduction')) return 'adduction';
  // --- abdos ---
  if (has('plank') || has('hollow') || has('dead bug') || has('wall sit') ||
    has('mountain') || has('body saw') || has('arch hold') ||
    has('v-sit') || has('l-sit') || has('l-hang') || has('vacuum')) return 'plank';
  if (has('sit up')) return 'sit_up';
  if (has('crunch') || has('cocoon') || has('ball pass')) return 'crunch';
  if (has('twist') || has('windshield')) return 'twist';
  if (has('leg raise') || has('knee raise') || has('v-up') ||
    has('flutter') || has('scissor') || has('dragon flag') ||
    has('toe to bar') || has('captain') || has('jackknife') ||
    has('leg lower')) return 'leg_raise';
  if (has('rollout') || has('fallout')) return 'rollout';
  if (has('side bend')) return 'twist';
  // --- avant-bras ---
  if (has('carry') || has('dead hang')) return 'carry';
  // --- repli ---
  if (muscle === 'Cardio') return 'conditioning';
  if (muscle === 'Abs') return 'crunch';
  return 'bench';
}

// ---------------------------------------------------------------------------
// Recherche floue (port exact de L10nData) — accents insensibles, multi-tokens,
// FR + EN + muscle + alias, tolérance typos sur le NOM uniquement.
// ---------------------------------------------------------------------------

/** Minuscules, accents NFD supprimés, apostrophes → espaces. */
export function normalize(s) {
  return String(s).toLowerCase().normalize('NFD').replace(/\p{Mn}+/gu, '').replace(/'/g, ' ');
}

/** Distance de Levenshtein (tolérance typos) — DP deux lignes. */
export function lev(a, b) {
  if (a === b) return 0;
  if (a.length === 0) return b.length;
  if (b.length === 0) return a.length;
  let prev = Array.from({ length: b.length + 1 }, (_, i) => i);
  let cur = new Array(b.length + 1);
  for (let i = 1; i <= a.length; i++) {
    cur[0] = i;
    for (let j = 1; j <= b.length; j++) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1;
      cur[j] = Math.min(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost);
    }
    const tmp = prev; prev = cur; cur = tmp;
  }
  return prev[b.length];
}

/** Tokens de requête sans signification — ignorés par la recherche (liste exacte de L10nData.kt). */
const NOISE_TOKENS = new Set([
  'en', 'de', 'du', 'la', 'le', 'les', 'un', 'une', 'des', 'a', 'au', 'aux',
  'et', 'ou', 'avec', 'sur', 'pour', 'dans', 'par', 'pose', 'prise', 'grip', 'the', 'and',
]);

/** Variantes de stemming léger : brut, -ing, -es, -s (rowing→row, curls→curl…). */
function stems(token) {
  const out = [token];
  if (token.length >= 5 && token.endsWith('ing')) out.push(token.slice(0, -3));
  if (token.length >= 4 && token.endsWith('es')) out.push(token.slice(0, -2));
  if (token.length >= 4 && token.endsWith('s')) out.push(token.slice(0, -1));
  return out;
}

/** Un token matche si lui (ou un stem) est sous-chaîne d'un mot cible, ou proche d'un mot entier (typos). */
function tokenMatches(token, words, allowTypo) {
  const variants = stems(token);
  for (const w of words) {
    for (const v of variants) {
      if (w.includes(v)) return true;
    }
  }
  if (!allowTypo) return false;
  const maxDist = token.length >= 5 ? 2 : token.length === 4 ? 1 : 0;
  if (maxDist === 0) return false;
  return words.some((w) => variants.some((v) =>
    Math.abs(w.length - v.length) <= maxDist && lev(v, w) <= maxDist));
}

/** Texte cherchable d'un exercice, deux groupes : mots du nom (typos OK) vs attributs (sous-chaîne exacte). */
const nameWordsCache = new Map();
const attrWordsCache = new Map();

/** Vrai si chaque mot de la requête apparaît (ou presque, typos sur le nom) dans le nom FR/EN, muscle ou équipement. */
export function matches(q, nameEn) {
  const nq = normalize(q).trim();
  if (nq === '') return true;
  const def = defOf(nameEn); // builtin d'abord, custom ensuite (jamais l'inverse)
  let nameWords = nameWordsCache.get(nameEn);
  if (nameWords === undefined) {
    nameWords = (normalize(nameEn) + ' ' + normalize(NAME_FR[nameEn] ?? '') + ' ' + normalize(ALIAS_FR[nameEn] ?? ''))
      .split(' ').filter((w) => w.trim() !== '');
    nameWordsCache.set(nameEn, nameWords);
  }
  let attrWords = attrWordsCache.get(nameEn);
  if (attrWords === undefined) {
    const muscle = def?.muscle ?? '';
    const equip = def?.equip ?? '';
    attrWords = (normalize(muscle) + ' ' + normalize(MUSCLE_FR[muscle] ?? '') + ' ' +
      normalize(MUSCLE_EXTRA[muscle] ?? '') + ' ' + normalize(equip) + ' ' + normalize(EQUIP_FR[equip] ?? ''))
      .split(' ').filter((w) => w.trim() !== '');
    attrWordsCache.set(nameEn, attrWords);
  }
  return nq.split(' ')
    .filter((t) => t.trim() !== '' && !NOISE_TOKENS.has(t.trim()))
    .every((t) => tokenMatches(t.trim(), nameWords, true) || tokenMatches(t.trim(), attrWords, false));
}

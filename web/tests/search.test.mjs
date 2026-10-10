// Tests du port web de la localisation + recherche floue FR.
// Assertions portées des tests Android LogicTest.kt (« fuzzy search matches french names
// accent-insensitive » et « fuzzy search matches hevy french vocabulary ») + API L10n.
// Harnais : node web/tests/run.mjs (fonctions exportées préfixées « test »).

import assert from 'node:assert/strict';
import {
  L10n, exName, muscleName, equipName, cuesFor, stepsFor, equipHintFor,
  archetypeOf, matches, setCustomDefs,
} from '../src/search.js';
import { EXERCISES, NAME_FR } from '../src/data.js';

/** Exécute fn avec une langue imposée, restaure l'état antérieur quoi qu'il arrive. */
function withLang(lang, fn) {
  const save = L10n.lang;
  L10n.setLang(lang);
  try {
    return fn();
  } finally {
    L10n.setLang(save);
  }
}

function capFr(s) {
  return s.charAt(0).toUpperCase() + s.slice(1);
}

// ---------------------------------------------------------------------------
// L10n : détection/forçage de langue + s() avec retombées
// ---------------------------------------------------------------------------

export function testLLangSetterAndStrings() {
  assert.ok(['fr', 'es', 'de', 'en'].includes(L10n.lang), `lang inattendu : ${L10n.lang}`);
  withLang('fr', () => {
    assert.equal(L10n.s('Add', 'Ajouter', 'Añadir', 'Hinzufügen'), 'Ajouter');
  });
  withLang('es', () => {
    assert.equal(L10n.s('Add', 'Ajouter', 'Añadir', 'Hinzufügen'), 'Añadir');
    assert.equal(L10n.s('Add', 'Ajouter'), 'Add'); // es absent → EN
  });
  withLang('de', () => {
    assert.equal(L10n.s('Add', 'Ajouter', 'Añadir', 'Hinzufügen'), 'Hinzufügen');
    assert.equal(L10n.s('Add', 'Ajouter'), 'Add'); // de absent → EN
  });
  withLang('en', () => {
    assert.equal(L10n.s('Add', 'Ajouter', 'Añadir', 'Hinzufügen'), 'Add');
  });
  // forçage dans {fr, es, de} sinon 'en' — et setLang slice(0,2)
  withLang('pt', () => assert.equal(L10n.lang, 'en'));
  withLang('fr-FR', () => assert.equal(L10n.lang, 'fr'));
  withLang('de-DE', () => assert.equal(L10n.lang, 'de'));
}

// ---------------------------------------------------------------------------
// seriesLabel / seeMoreExercises — logique pluriel exacte de L10n.kt
// ---------------------------------------------------------------------------

export function testSeriesLabelPlural() {
  withLang('fr', () => {
    assert.equal(L10n.seriesLabel(1, 'Squat'), '1 série Squat');
    assert.equal(L10n.seriesLabel(3, 'Squat'), '3 séries Squat');
  });
  withLang('de', () => assert.equal(L10n.seriesLabel(3, 'Squat'), '3 Sätze Squat'));
  withLang('en', () => assert.equal(L10n.seriesLabel(3, 'Squat'), '3 series Squat'));
  withLang('es', () => assert.equal(L10n.seriesLabel(3, 'Squat'), '3 series Squat')); // es retombe sur EN
}

export function testSeeMoreExercisesPlural() {
  withLang('fr', () => {
    assert.equal(L10n.seeMoreExercises(1), 'Voir 1 exercice en plus');
    assert.equal(L10n.seeMoreExercises(5), 'Voir 5 exercices en plus');
  });
  withLang('en', () => {
    assert.equal(L10n.seeMoreExercises(1), 'See 1 more exercise');
    assert.equal(L10n.seeMoreExercises(5), 'See 5 more exercises');
  });
  withLang('es', () => assert.equal(L10n.seeMoreExercises(5), 'Ver 5 ejercicios más'));
  withLang('de', () => assert.equal(L10n.seeMoreExercises(5), '5 weitere Übungen anzeigen'));
}

// ---------------------------------------------------------------------------
// relativeTime — min / h / jour / jours + date future → nom du jour
// ---------------------------------------------------------------------------

export function testRelativeTimeFr() {
  withLang('fr', () => {
    const now = 1_800_000_000_000;
    assert.equal(L10n.relativeTime(now - 5 * 60_000, now), 'il y a 5 min');
    assert.equal(L10n.relativeTime(now - 30_000, now), 'il y a 1 min'); // clamp min 1
    assert.equal(L10n.relativeTime(now, now), 'il y a 1 min');
    assert.equal(L10n.relativeTime(now - 3 * 3_600_000, now), 'il y a 3 h');
    assert.equal(L10n.relativeTime(now - 25 * 3_600_000, now), 'il y a 1 jour');
    assert.equal(L10n.relativeTime(now - 3 * 86_400_000, now), 'il y a 3 jours');
    // date future (import daté dans le futur) → nom du jour, pas « il y a … »
    const future = now + 36 * 3_600_000;
    const got = L10n.relativeTime(future, now);
    assert.equal(got, capFr(new Date(future).toLocaleDateString('fr-FR', { weekday: 'long' })));
    assert.ok(!got.includes('il y a'));
  });
}

export function testRelativeTimeEn() {
  withLang('en', () => {
    const now = 1_800_000_000_000;
    assert.equal(L10n.relativeTime(now - 5 * 60_000, now), '5 min ago');
    assert.equal(L10n.relativeTime(now - 3 * 3_600_000, now), '3 h ago');
    assert.equal(L10n.relativeTime(now - 25 * 3_600_000, now), '1 day ago');
    assert.equal(L10n.relativeTime(now - 3 * 86_400_000, now), '3 days ago');
    const future = now + 36 * 3_600_000;
    assert.equal(L10n.relativeTime(future, now), new Date(future).toLocaleDateString('en-US', { weekday: 'long' }));
  });
}

// ---------------------------------------------------------------------------
// Résolution de noms : exName / muscleName / equipName, cues, steps, equipHint
// ---------------------------------------------------------------------------

export function testLocalizedNamesFallback() {
  withLang('fr', () => {
    assert.equal(exName('Barbell Bench Press'), 'Développé Couché (Barre)');
    assert.equal(exName('Exercice Inconnu'), 'Exercice Inconnu'); // retombée EN
    assert.equal(muscleName('Lats'), 'Dorsaux');
    assert.equal(muscleName('Inconnu'), 'Inconnu');
    assert.equal(equipName('Cable'), 'Poulie');
    assert.equal(equipName('Inconnu'), 'Inconnu');
  });
  withLang('en', () => {
    assert.equal(exName('Barbell Bench Press'), 'Barbell Bench Press');
    assert.equal(muscleName('Lats'), 'Lats');
    assert.equal(equipName('Cable'), 'Cable');
  });
}

export function testCuesLocalized() {
  withLang('fr', () => {
    assert.equal(cuesFor('Chest').length, 3);
    assert.ok(cuesFor('Chest')[0].includes('omoplates'));
    assert.equal(cuesFor('Cardio').length, 0); // pas de cues Cardio → liste vide
  });
  withLang('en', () => {
    assert.ok(cuesFor('Chest')[0].includes('shoulder blades'));
  });
}

export function testStepsViaArchetype() {
  withLang('fr', () => {
    assert.equal(archetypeOf('Barbell Bench Press'), 'bench');
    assert.equal(stepsFor('Barbell Bench Press').length, 4);
    assert.ok(stepsFor('Barbell Bench Press')[0].includes('banc'));
    assert.equal(archetypeOf('Romanian Deadlift'), 'rdl');
    assert.equal(archetypeOf('Pull Up'), 'pull_up');
    assert.equal(archetypeOf('Seated Cable Row'), 'row_supported');
    assert.equal(archetypeOf('Bent Over Row'), 'row_bent');
    assert.equal(archetypeOf('Walking Lunge'), 'lunge'); // condition « walking lunge » AVANT conditioning
    assert.equal(archetypeOf('Treadmill'), 'conditioning');
    assert.equal(archetypeOf('Power Clean'), 'olympic');
    assert.equal(archetypeOf('Kettlebell Swing'), 'swing');
    // fidèle au Kotlin : la branche bras « curl » (Data.kt l.1148) précède la branche
    // jambes « leg curl/nordic » (l.1173) → « Nordic Curl » résout en "curl".
    assert.equal(archetypeOf('Nordic Curl'), 'curl');
    assert.equal(archetypeOf('Glute Ham Raise'), 'leg_curl'); // seul nom atteignant vraiment leg_curl
    assert.equal(archetypeOf('Reverse Nordic'), 'squat'); // égalité exacte, pas contains
    assert.equal(archetypeOf('Plank'), 'plank');
    assert.equal(archetypeOf('Leg Raise'), 'leg_raise');
    assert.equal(archetypeOf("Farmer's Carry"), 'carry');
    assert.equal(archetypeOf('Dips'), 'dip');
    assert.equal(archetypeOf('Chest Dip'), 'dip'); // dip + muscle Chest
    assert.equal(archetypeOf('Machine Fly (Pec Deck)'), 'fly');
    assert.equal(archetypeOf('Overhead Press'), 'ohp');
    assert.equal(archetypeOf('Deadlift'), 'deadlift');
  });
  withLang('en', () => {
    assert.ok(stepsFor('Barbell Bench Press').length > 0);
    assert.ok(stepsFor('Barbell Bench Press')[0].toLowerCase().includes('bench'));
  });
}

export function testEquipHintLocalized() {
  withLang('fr', () => {
    assert.equal(equipHintFor('Barbell'), 'Avec une barre chargée de disques.');
    assert.equal(equipHintFor('Dumbbell'), 'Avec des haltères — un dans chaque main sauf indication.');
    assert.equal(equipHintFor('Machine'), 'Sur une machine à pile ou à charges guidées.');
    assert.equal(equipHintFor('Cable'), 'À la station de poulies avec accessoires.');
    assert.equal(equipHintFor('Bodyweight'), 'Sans matériel — le corps sert de résistance.');
    assert.equal(equipHintFor('Other'), 'Avec du matériel spécifique : banc, élastique, kettlebell ou roue.');
    assert.equal(equipHintFor('Bande'), 'Avec du matériel spécifique : banc, élastique, kettlebell ou roue.'); // default
  });
  withLang('en', () => {
    assert.equal(equipHintFor('Barbell'), 'Performed with a barbell loaded with plates.');
    assert.equal(equipHintFor('Inconnu'), '');
  });
}

// ---------------------------------------------------------------------------
// Recherche floue — port exact des deux tests Android LogicTest.kt
// ---------------------------------------------------------------------------

export function testFuzzySearchFrenchNamesAccentInsensitive() {
  assert.ok(matches('developpe', 'Barbell Bench Press'));
  assert.ok(matches('DEVELOPPE COUCHE', 'Barbell Bench Press'));
  assert.ok(matches('dev', 'Barbell Bench Press'));
  assert.ok(matches('couché haltères', 'Dumbbell Bench Press'));
  assert.ok(matches('elevation', 'Lateral Raise'));
  assert.ok(matches('tirage', 'Lat Pulldown'));
  assert.ok(matches('bench press', 'Barbell Bench Press'));
  assert.ok(matches('pec', 'Barbell Bench Press')); // via muscle « Pectoraux »
  assert.ok(matches('squat', 'Barbell Squat'));
  assert.ok(matches('', 'Barbell Squat'));
  assert.equal(matches('squat', 'Barbell Bench Press'), false);
  // tolérance typos — exemple utilisateur : « developer coucher »
  assert.ok(matches('developer coucher', 'Barbell Bench Press'));
  assert.ok(matches('developer couché', 'Barbell Bench Press'));
  assert.ok(matches('sqaut', 'Barbell Squat')); // lettres inversées
  assert.ok(matches('tirrage', 'Lat Pulldown'));
  assert.ok(matches('elevaion', 'Lateral Raise'));
}

export function testFuzzySearchHevyFrenchVocabulary() {
  // l'exemple exact de l'utilisateur : « tirage poitrine » → Chest Supported Row (renommé « Tirage Poitrine »)
  assert.ok(matches('tirage poitrine', 'Chest Supported Row'));
  assert.ok(matches('poitrine', 'Chest Supported Row'));
  assert.ok(matches('pec deck', 'Machine Fly (Pec Deck)'));
  // synonymes musculaires (vocabulaire salle)
  assert.ok(matches('dos', 'Pull Up'));
  assert.ok(matches('dos', 'Deadlift'));
  assert.ok(matches('abdos', 'Crunch'));
  assert.ok(matches('ventre', 'Plank'));
  assert.ok(matches('jambes', 'Barbell Squat'));
  assert.ok(matches('mollets', 'Standing Calf Raise'));
  assert.ok(matches('bras', 'Barbell Curl'));
  // équipements FR
  assert.ok(matches('poulie', 'Cable Curl'));
  assert.ok(matches('halteres', 'Dumbbell Bench Press'));
  assert.ok(matches('poids du corps', 'Push Up'));
  // argot de salle (alias)
  assert.ok(matches('dc', 'Barbell Bench Press'));
  assert.ok(matches('sdt', 'Deadlift'));
  assert.ok(matches('barre au front', 'Skullcrusher'));
  // exercices récents cherchables par leur nom FR
  assert.ok(matches('fentes arriere', 'Reverse Lunge'));
  assert.ok(matches('kettlebell', 'Kettlebell Swing'));
  assert.ok(matches('dragon flag', 'Dragon Flag'));
  assert.ok(matches('tractions prise large', 'Wide Grip Pull Up'));
  // noms exacts Hevy FR : famille « Développé Militaire Haltères »
  assert.ok(matches('developpe militaire halteres', 'Seated Dumbbell Shoulder Press'));
  assert.ok(matches('developpe militaire', 'Standing Dumbbell Shoulder Press'));
  assert.ok(matches('militaire machine', 'Machine Shoulder Press'));
  // vocabulaire avec mots vides + stemming : « rowing poulie assis prise en v »
  assert.ok(matches('rowing poulie assis prise en v', 'V-Bar Cable Row'));
  assert.ok(matches('rowing assis poulie', 'Seated Cable Row'));
  assert.ok(matches('curl machine assis', 'Preacher Curl'));
  // mots vides seuls ignorés
  assert.ok(matches('le squat', 'Barbell Squat'));
  assert.ok(matches('avec barre', 'Barbell Bench Press'));
  // pas de typo-flou sur les mots muscle : « goblet » ne doit pas matcher « mollets »
  assert.ok(matches('goblet', 'Goblet Squat'));
  assert.equal(matches('goblet', 'Standing Calf Raise'), false);
  assert.equal(matches('squat', 'Standing Calf Raise'), false);
}

// ---------------------------------------------------------------------------
// Intégrité du catalogue (port de la sanity check Android)
// ---------------------------------------------------------------------------

export function testCatalogIntegrity() {
  const names = EXERCISES.map((d) => d.name);
  assert.equal(new Set(names).size, names.length, 'noms canoniques en doublon');
  assert.ok(names.length >= 600, `catalogue trop petit : ${names.length}`);
  withLang('fr', () => {
    for (const n of names) {
      assert.ok(NAME_FR[n], `nom FR manquant : ${n}`);
      assert.ok(stepsFor(n).length > 0, `pas d'instructions : ${n}`);
    }
  });
}

// ---------------------------------------------------------------------------
// Registre d'exercices custom — participent à la recherche, caches invalidés au remplacement
// ---------------------------------------------------------------------------

export function testCustomDefsParticipateInSearch() {
  try {
    setCustomDefs([{ name: 'Tirage Majorette', muscle: 'Lats', equip: 'Cable' }]);
    assert.ok(matches('majorette', 'Tirage Majorette')); // nom exact
    assert.ok(matches('majorete', 'Tirage Majorette')); // tolérance typo (distance 1, mot ≥ 5 lettres)
    assert.ok(matches('tirage majorette', 'Tirage Majorette'));
    assert.ok(matches('dos', 'Tirage Majorette')); // via muscle Lats (« dos »)
    assert.ok(matches('poulie', 'Tirage Majorette')); // via équipement Cable (« Poulie »)
    assert.equal(matches('squat', 'Tirage Majorette'), false);
    withLang('fr', () => assert.equal(exName('Tirage Majorette'), 'Tirage Majorette')); // nom tel quel
    assert.equal(archetypeOf('Tirage Majorette'), 'bench'); // repli générique (muscle Lats → bench)
    // un custom avec un nom parlant hérite des instructions via archetypeOf
    setCustomDefs([{ name: 'Mon Squat Perso', muscle: 'Quads', equip: 'Barbell' }]);
    assert.equal(archetypeOf('Mon Squat Perso'), 'squat');
    withLang('fr', () => assert.ok(stepsFor('Mon Squat Perso').length > 0));
    // le remplacement du registre met bien à jour les caches (muscle changé → attributs changés)
    setCustomDefs([{ name: 'Tirage Majorette', muscle: 'Chest', equip: 'Machine' }]);
    assert.ok(matches('poitrine', 'Tirage Majorette')); // MUSCLE_EXTRA Chest
    setCustomDefs([{ name: 'Tirage Majorette', muscle: 'Lats', equip: 'Cable' }]);
    assert.equal(matches('poitrine', 'Tirage Majorette'), false);
    // un custom ne masque jamais le catalogue builtin
    setCustomDefs([{ name: 'Barbell Squat', muscle: 'Chest', equip: 'Other' }]);
    assert.ok(matches('jambes', 'Barbell Squat')); // toujours Quads
    assert.equal(archetypeOf('Barbell Squat'), 'squat');
  } finally {
    setCustomDefs([]);
    assert.ok(matches('squat', 'Barbell Squat')); // catalogue intact après la purge
    assert.ok(matches('developpe', 'Barbell Bench Press'));
  }
}

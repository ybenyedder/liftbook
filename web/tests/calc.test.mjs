// Tests du port web de Calc.kt — port des assertions clés de LogicTest.kt.
import {
  e1rm, round125, vol, reps, setsDone, fmtKg, unitLabel, toKg, fmtVol, fmtDur, fmtClock,
  streak, platesForSide, isCardioName, fmtCardioSet, parseCsv, parseHevyCsv,
  routinesFromWorkouts, seed, exportCsvText, rebuildPrs,
} from '../src/calc.js';

const W = (id, daysAgo, exs, name = 'Séance') => ({
  id, name, startedAt: Date.now() - daysAgo * 86400000, endedAt: Date.now() - daysAgo * 86400000 + 3600000,
  notes: '', prs: [], exercises: exs,
});
const ex = (name, muscle, sets) => ({ name, muscle, notes: '', superset: false, restSec: null, sets });

export function testE1rmEpley() {
  const eps = 0.01;
  if (Math.abs(e1rm(80, 10) - 106.666) > eps) throw new Error(`e1rm(80,10)=${e1rm(80, 10)}`);
  if (Math.abs(e1rm(120, 2) - 128) > eps) throw new Error(`e1rm(120,2)=${e1rm(120, 2)}`);
  if (Math.abs(e1rm(80, 0) - 80) > eps) throw new Error('e1rm(80,0)');
  if (round125(81.3) !== 81.25 || round125(82.4) !== 82.5) throw new Error('round125');
}

export function testVolRepsSetsDoneIgnoreUnfinished() {
  const w = W(1, 1, [
    ex('Bench', 'Chest', [
      { kg: 80, reps: 8, done: true },
      { kg: 100, reps: 10, done: false },      // non cochée → ignorée
      { kg: null, reps: null, done: true },     // vide → ignorée
    ]),
  ]);
  if (vol(w) !== 640) throw new Error(`vol=${vol(w)}`);
  if (reps(w) !== 8) throw new Error(`reps=${reps(w)}`);
  if (setsDone(w) !== 1) throw new Error(`setsDone=${setsDone(w)}`);
}

export function testKgLbRoundtrip() {
  const kg = 100;
  const lb = kg * 2.2046226;
  if (Math.abs(toKg(String(lb.toFixed(2)), 'lb') - kg) > 0.01) throw new Error('lb→kg');
  if (toKg('82,5', 'kg') !== 82.5) throw new Error('virgule décimale');
  if (toKg('', 'kg') !== null || toKg('-5', 'kg') !== null) throw new Error('vide/négatif → null');
  if (fmtKg(82.5, 'kg') !== '82.5') throw new Error(`fmtKg=${fmtKg(82.5, 'kg')}`);
  if (unitLabel('lb') !== 'lbs' || unitLabel('kg') !== 'kg') throw new Error('unitLabel');
}

export function testFmtVolSeparator() {
  if (fmtVol(84325, 'kg') !== '84\u202f325') throw new Error(`fmtVol=${fmtVol(84325, 'kg').codePointAt(2).toString(16)}`);
}

export function testStreakCalendarDays() {
  const mk = (daysAgo) => W(1, daysAgo, []);
  // hier + aujourd'hui → 2
  if (streak([mk(0), mk(1)], Date.now()) !== 2) throw new Error('streak 2j');
  // hier seulement (rien aujourd'hui, tolérance Android) → 1
  if (streak([mk(1)], Date.now()) !== 1) throw new Error('streak start hier');
  // trou → 0/1
  if (streak([mk(0), mk(2)], Date.now()) !== 1) throw new Error('trou → redémarre à 1');
  if (streak([], Date.now()) !== 0) throw new Error('vide');
}

export function testFmtDurClockNeverNegative() {
  if (fmtDur(-500) !== '0m') throw new Error(`fmtDur(-500)=${fmtDur(-500)}`);
  if (fmtClock(-500) !== '00:00') throw new Error(`fmtClock(-500)=${fmtClock(-500)}`); // %02d:%02d Kotlin
  if (fmtDur(3900000) !== '1h 5m') throw new Error(fmtDur(3900000)); // pas de zéro de tête (Kotlin "${h}h ${m}m")
  if (fmtClock(3900000) !== '1:05:00') throw new Error(fmtClock(3900000));
}

export function testCardioFmtAndDetect() {
  if (fmtCardioSet(22, 5.2) !== '22min · 5.2km') throw new Error(fmtCardioSet(22, 5.2));
  if (fmtCardioSet(65, null) !== '1h05') throw new Error(fmtCardioSet(65, null));
  if (!isCardioName('Treadmill')) throw new Error('Treadmill devrait être cardio');
  if (isCardioName('Barbell Bench Press')) throw new Error('Bench pas cardio');
}

export function testPlatesNoBar() {
  // « Sans barre » (v1.50) : tout en disques, 70 kg → 35/side → 25+10
  const r = platesForSide(70, 0, 'kg');
  if (r.total !== 70) throw new Error(`total=${r.total}`);
  const flat = r.perSide.map(([w, c]) => `${c}x${w}`).join('+');
  if (flat !== '1x25+1x10') throw new Error(flat);
  const r2 = platesForSide(70, 20, 'kg');
  if (r2.total !== 70) throw new Error(`total2=${r2.total}`);
  if (r2.perSide.length !== 1 || r2.perSide[0][0] !== 25) throw new Error(JSON.stringify(r2.perSide));
  const r3 = platesForSide(135, 45, 'lb');
  if (r3.perSide[0][0] !== 45) throw new Error(JSON.stringify(r3.perSide));
}

export function testExportCsvRoundtrip() {
  const w = W(1, 3, [
    ex('Barbell Bench Press', 'Chest', [{ kg: 80, reps: 8, done: true }]),
    ex('Treadmill', 'Cardio', [{ mins: 22, km: 5.2, done: true }]),
  ]);
  const csv = exportCsvText([w], 'kg');
  const lines = csv.trim().split('\n');
  if (!lines[0].startsWith('Date;Heure;Exercice;Serie;kg;Reps;Min;Km')) throw new Error(lines[0]);
  if (lines.length !== 3) throw new Error(`${lines.length} lignes`);
  if (!/^\d+ [^;]+ \d{4};/.test(lines[1])) throw new Error('date sans année: ' + lines[1]); // l'année est obligatoire (fix v1.50)
  if (!lines[2].endsWith(';;22;5.2')) throw new Error('cardio: ' + lines[2]);
  const back = parseCsv(csv);
  if (back.length !== 1) throw new Error(`reimport ${back.length}`);
  const b = back[0];
  if (b.exercises[0].name !== 'Barbell Bench Press') throw new Error('canonical');
  if (b.exercises[1].sets[0].mins !== 22 || Math.abs(b.exercises[1].sets[0].km - 5.2) > 1e-9) throw new Error('cardio round-trip');
}

export function testParseHevyRealExport() {
  const csv = [
    'Date,Exercise Name,Set Order,Weight Kg,Reps,Distance Unit,Workout Name,Notes,Superset',
    '"18 sept. 2026, 17:30","Développé Couché (Barre)",1,80,8,,"Push",,',
    '"18 sept. 2026, 17:30","Développé Couché (Barre)",2,80,8,,"Push",,',
    '"18 sept. 2026, 17:30","Tirage Vertical (Poulie)",1,60,10,,"Push",,superset1',
  ].join('\n');
  const [ws] = parseHevyCsv(csv);
  if (ws.length !== 1) throw new Error(`${ws.length} séances`);
  if (ws[0].exercises.length !== 2) throw new Error(`${ws[0].exercises.length} exos`);
  if (ws[0].exercises[1].superset !== true) throw new Error('superset perdu');
  if (ws[0].exercises[0].name !== 'Barbell Bench Press') throw new Error(ws[0].exercises[0].name);
}

export function testTemplateCsvBecomesRoutine() {
  const csv = 'Exercise Name,Set Order,Weight Kg,Reps\n"Dumbbell Curl",1,12,12\n"Dumbbell Curl",2,12,10';
  const [ws, rs] = parseHevyCsv(csv);
  if (ws.length !== 0 || rs.length !== 1) throw new Error(`w=${ws.length} r=${rs.length}`);
  if (rs[0].exercises[0].sets[0].kg !== 12) throw new Error('sets template');
}

export function testRoutinesFromWorkouts() {
  const ws = [
    W(1, 10, [ex('Bench', 'Chest', [])], 'Push'),
    W(2, 5, [ex('Bench', 'Chest', [])], 'Push'),    // même nom exact → dernière occurrence
    W(3, 1, [ex('Squat', 'Quads', [])], 'Legs'),
  ];
  const rs = routinesFromWorkouts(ws, new Set(['Legs']));
  if (rs.length !== 1) throw new Error(`${rs.length} routines (Legs exclu, doublon fusionné)`);
  if (rs[0].name !== 'Push') throw new Error(rs[0].name);
  // un nom différent seulement par la casse = un groupe distinct (groupBy exact du Kotlin)
  const rs2 = routinesFromWorkouts([...ws, W(4, 2, [], 'push')], new Set());
  if (rs2.length !== 2) throw new Error(`${rs2.length} avec casse différente`);
}

export function testSeedDeterministic() {
  const [w1, r1] = seed(1234567890);
  const [w2, r2] = seed(1234567890);
  const v1 = w1.reduce((a, w) => a + vol(w), 0);
  const v2 = w2.reduce((a, w) => a + vol(w), 0);
  if (v1 !== v2 || w1.length !== w2.length || r1.length !== r2.length) throw new Error('non déterministe');
  if (w1.length < 30 || w1.length > 60) throw new Error(`seed ${w1.length} séances`);
  if (r1.length !== 4) throw new Error(`${r1.length} routines`);
}

export function testRebuildPrsChrono() {
  const [ws] = seed(1234567890);
  const cache = rebuildPrs(ws);
  const total = ws.reduce((a, w) => a + (w.prs || []).length, 0);
  if (total === 0) throw new Error('aucun PR');
  for (const [name, best] of Object.entries(cache)) {
    if (!(best.weight > 0) && !(best.e1rm > 0)) throw new Error(`cache vide pour ${name}`);
  }
}

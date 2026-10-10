// Logger — séance en cours / éditeur de routine — port de ui/Logger.kt (1 365 l.).
// Conventions : brouillon store.draft = source de vérité ; chaque mutation → draft puis
// S.persistDraftNow() + re-render LOCALE (jamais d'emit global : l'input actif n'est jamais réécrit).
// Fonctions portées : LoggerScreen, ExCard, SetRow, SetField, evaluatePr, PrBadge(Host),
// LiveStat, RestSheet, RestBar, fmtRestLabel, PickerContent, CreateExerciseDialog (Exercises.kt).
import { h, icon, toast, dialog, confirmDialog, promptDialog, listDialog, sheet, ctxMenu, emptyState, exCircle, bodyMap } from '../ui.js';
import { store } from '../store.js';
import * as S from '../store.js';
import * as nav from '../router.js';
import * as Calc from '../calc.js';
import { L10n, exName, muscleName, equipName, matches } from '../search.js';
import { EXERCISES, MUSCLES } from '../data.js';
import { rest, onRest, start as restStart, plus15, minus15, clear as restClear, remainingMs, isOver } from '../restimer.js';

nav.registerScreen('logger', { render });

/* ============================== CSS scoped ============================== */
// Injecté une fois. Préfixe .scr-logger pour l'écran ; les overlays (sheets ajoutés à #app,
// hors de l'élément écran) utilisent des classes dédiées lb-log-* propres à ce module.
function css() {
  if (!document.getElementById('css-logger')) {
    const st = h('style', { id: 'css-logger' });
    st.textContent = `
.scr-logger { overflow: hidden; position: relative; }
.scr-logger .log-scroll { flex: 1; overflow-y: auto; scrollbar-width: none; }
.scr-logger .log-scroll::-webkit-scrollbar { display: none; }
/* ---- barres supérieures ---- */
.scr-logger .wtop { display: flex; align-items: center; padding: 6px 12px; gap: 6px; }
.scr-logger .wmenubtn { display: flex; align-items: center; gap: 6px; padding: 6px 4px; border-radius: 10px; max-width: 55%; }
.scr-logger .wmenubtn .wt { font-size: 21px; font-weight: 800; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.scr-logger .finishbtn { background: var(--accent); color: #fff; border-radius: 12px; padding: 10px 18px; font-size: 15px; font-weight: 600; flex: 0 0 auto; }
.scr-logger .lnk { color: var(--accent); font-size: 15px; font-weight: 600; padding: 10px 6px; border-radius: 8px; flex: 0 0 auto; }
.scr-logger .rtitle { flex: 1; min-width: 0; text-align: center; font-size: 17px; font-weight: 800; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
/* ---- stats live (LiveStat : label gris au-dessus de la grande valeur) ---- */
.scr-logger .livestats { display: flex; align-items: center; gap: 8px; padding: 4px 20px 10px; }
.scr-logger .lstat { flex: 1; min-width: 0; padding: 4px 0; }
.scr-logger .lstat .l { color: var(--mut); font-size: 13px; font-weight: 500; }
.scr-logger .lstat .v { font-size: 21px; font-weight: 800; margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.scr-logger .lstat.acc .v { color: var(--accent); }
.scr-logger .bm { flex: 0 0 auto; }
/* ---- champ titre routine (BasicTextField sur filet) ---- */
.scr-logger .rname-wrap { padding: 4px 16px 2px; }
.scr-logger .rname { width: 100%; height: 38px; background: none; border: none; outline: none; color: var(--text); font-size: 24px; font-weight: 700; }
.scr-logger .rname::placeholder { color: var(--mut); font-size: 22px; font-weight: 700; }
/* ---- sections d'exercices (directement sur fond noir, pas de carte) ---- */
.scr-logger .exsection { padding: 8px 0; touch-action: pan-y; user-select: none; -webkit-user-select: none; }
.scr-logger .exsection.dragging { opacity: .92; background: var(--card2); border-radius: 12px; box-shadow: 0 10px 28px rgba(0,0,0,.55); position: relative; z-index: 5; }
.scr-logger .exsection-head { padding: 6px 12px; }
.scr-logger .ssbar { width: 3px; height: 34px; border-radius: 2px; background: var(--accent); flex: 0 0 auto; }
.scr-logger .exname { max-height: 46px; overflow: hidden; }
.scr-logger .exnotes { padding: 6px 16px; color: var(--mut); font-size: 15px; cursor: pointer; }
.scr-logger .exnotes-prev { display: flex; align-items: flex-start; gap: 6px; }
.scr-logger .exnotes-prev .msr { flex: 0 0 auto; margin-top: 2px; }
.scr-logger .exnotes-txt { font-size: 13.5px; line-height: 19px; display: -webkit-box; -webkit-line-clamp: 4; -webkit-box-orient: vertical; overflow: hidden; }
.scr-logger .exrest { display: flex; align-items: center; gap: 6px; margin: 2px 16px 0; padding: 4px; border-radius: 8px; color: var(--accent); font-size: 15px; font-weight: 500; }
.scr-logger .addset { display: flex; align-items: center; justify-content: center; gap: 8px; height: 42px; margin: 8px 12px 0; width: calc(100% - 24px); border-radius: 10px; background: var(--card2); font-size: 15px; font-weight: 500; }
.scr-logger .addex-wrap { padding: 8px 16px; }
.scr-logger .empty { white-space: pre-line; padding-top: 64px; }
/* ---- grilles de séries (SÉRIE | PRÉCÉDENT | KG | RÉPS | ✓) ---- */
.scr-logger .g5 { grid-template-columns: 38px 1.15fr 1fr 1fr 44px; }
.scr-logger .g4 { grid-template-columns: 38px 1fr 1fr 44px; }
.scr-logger .hcell { display: flex; align-items: center; justify-content: center; gap: 3px; text-align: center; min-width: 0; }
.scr-logger .prevtxt { color: var(--mut); font-size: 13.5px; text-align: center; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; padding: 0 2px; }
.scr-logger .prmedal { width: 38px; height: 38px; color: var(--gold); display: flex; align-items: center; justify-content: center; justify-self: center; }
.scr-logger .setcheck .msr { transition: transform .2s; }
.scr-logger .setcheck.pop .msr { animation: chkpop .28s cubic-bezier(.2,.8,.3,1.3); }
@keyframes chkpop { 0% { transform: scale(.6); opacity: .6; } 60% { transform: scale(1.12); } 100% { transform: scale(1); opacity: 1; } }
/* ---- pill PR (PrBadgeHost) ---- */
.scr-logger .prhost { position: absolute; top: 0; left: 0; right: 0; display: flex; justify-content: center; padding: 6px 20px; z-index: 40; pointer-events: none; }
.scr-logger .prpill { pointer-events: auto; display: flex; align-items: center; gap: 10px; width: 100%; background: var(--card2); border-radius: 999px; padding: 8px; animation: prpill-in .38s cubic-bezier(.2,.8,.3,1.15); }
@keyframes prpill-in { from { transform: translateY(-160%); opacity: 0; } }
.scr-logger .prpill-info { flex: 1; min-width: 0; }
.scr-logger .prpill-nm { font-size: 13.5px; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.scr-logger .prpill-k { color: var(--orange); font-size: 12.5px; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.scr-logger .prpill-x { flex: 0 0 auto; width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; }
/* ---- RestSheet (overlay hors écran) ---- */
.lb-log-rest .rest-big { color: var(--accent); font-size: 26px; font-weight: 800; margin: 4px 0 6px; }
.lb-log-rest .rest-list { height: 240px; overflow-y: auto; scroll-snap-type: y proximity; scrollbar-width: none; }
.lb-log-rest .rest-list::-webkit-scrollbar { display: none; }
.lb-log-rest .rest-row { width: 100%; padding: 9px 0; border-radius: 10px; text-align: center; font-size: 15px; color: var(--mut); scroll-snap-align: center; }
.lb-log-rest .rest-row.sel { color: var(--accent); font-weight: 800; font-size: 18px; background: var(--card2); }
.lb-log-rest .rest-actions { display: flex; align-items: center; padding: 10px 0 6px; }
/* ---- Picker (overlay hors écran) ---- */
.lb-log-pk .searchbar { margin: 4px 0 0; }
.lb-log-pk .pk-chips { display: flex; gap: 8px; overflow-x: auto; padding: 4px 0 10px; scrollbar-width: none; }
.lb-log-pk .pk-chips::-webkit-scrollbar { display: none; }
.lb-log-pk .pcreate { display: flex; align-items: center; gap: 12px; width: 100%; background: var(--card2); border-radius: 10px; padding: 10px 20px; color: var(--accent); font-size: 14px; font-weight: 600; margin: 4px 0; }
.lb-log-pk .prow { display: flex; align-items: center; gap: 14px; width: 100%; text-align: left; padding: 10px 4px; border-radius: 10px; }
.lb-log-pk .prow.sel { background: var(--card2); }
.lb-log-pk .pinfo { flex: 1; min-width: 0; }
.lb-log-pk .pnm { font-weight: 700; font-size: 15px; overflow: hidden; }
.lb-log-pk .psub { color: var(--mut); font-size: 12.5px; }
.lb-log-pk .padd { width: 30px; height: 30px; border-radius: 50%; background: var(--accent); color: #fff; display: flex; align-items: center; justify-content: center; flex: 0 0 auto; }
/* ---- dialogue « Nouvel exercice » (overlay hors écran) ---- */
.lb-log-cx .cx-label { font-size: 12px; color: var(--mut2); margin: 12px 2px 0; }
.lb-log-cx .cx-row { display: flex; align-items: center; background: var(--card2); border-radius: 10px; padding: 11px 12px; width: 100%; margin-top: 8px; }
.lb-log-cx .cx-row .lb { flex: 1; font-size: 13.5px; text-align: left; }
.lb-log-cx .cx-row .vl { color: var(--accent); font-size: 13.5px; font-weight: 600; }
`;
    document.head.append(st);
  }
}

/* ============================== état module ============================== */
let rootEl = null;        // élément écran courant
let scrollEl = null;      // zone scrollable des exercices
let statsRefs = null;     // noeuds des stats live
let dockEl = null, dockTime = null, dockProgress = null; // RestBar
const prBadge = { current: null }; // PrBadge (Logger.kt) — survit aux re-rendus locaux
let prHostEl = null;
let prTimer = null;
let liveTimers = [];      // setInterval de l'écran courant (tick stats / restbar)
let restWired = false;

/* ============================== helpers ============================== */

// LoggerScreen.hasData / anyDone
const hasData = () => !!store.draft && store.draft.exercises.some(ex =>
  ex.sets.some(s => s.kg != null || s.reps != null || s.mins != null || s.km != null));
const anyDone = () => !!store.draft && store.draft.exercises.some(ex =>
  ex.sets.some(s => s.done && (s.kg != null || s.reps != null || s.mins != null || s.km != null)));

/** « 2min 30s » — port de Logger.kt fmtRestLabel. */
function fmtRestLabel(sec) {
  return sec >= 60 ? `${Math.trunc(sec / 60)}min ${sec % 60}s` : `${sec}s`;
}

/** Définition catalogue (built-ins + customs) — équiv. data.EX côté Android. */
function exDef(name) {
  return [...EXERCISES, ...store.customs.map(c => ({ name: c.name, muscle: c.muscle, equip: c.equip }))]
    .find(e => e.name === name) || null;
}

// SetRow onChange : chiffres seuls, tronqués (Kotlin filter{}.take{}.to*OrNull)
function toIntFiltered(v, n) {
  const d = String(v || '').replace(/\D/g, '').slice(0, n);
  return d ? parseInt(d, 10) : null;
}
function toFloatFiltered(v, n) {
  const d = String(v || '').replace(/[^\d.]/g, '').slice(0, n);
  const f = parseFloat(d);
  return Number.isFinite(f) ? f : null;
}

const newSet = (kg, reps, mins, km, done = false) =>
  ({ kg: kg ?? null, reps: reps ?? null, mins: mins ?? null, km: km ?? null, done, prW: false, prE: false });

/** Chip locale — ui.chipEl a un bug de shorthand ({onclick} vs param onClick) : ReferenceError à l'appel. */
function chipEl(label, selected, onClick) {
  return h('button', { class: 'chip' + (selected ? ' sel' : ''), onclick: onClick }, label);
}

/* ============================== rendu ============================== */

function render(el) {
  css();
  el.classList.add('scr-logger');
  if (!store.draft) { nav.nav.toTab(0); return; } // LoggerScreen : draft null → HomeTab
  rootEl = el;
  wireRest();
  build();
}

function build() {
  for (const t of liveTimers) clearInterval(t);
  liveTimers = [];
  rootEl.addEventListener('screen-gone', () => {
    for (const t of liveTimers) clearInterval(t);
    liveTimers = [];
    clearTimeout(prTimer);
  }, { once: true });

  const draft = store.draft;
  const isWorkout = draft.mode === 'workout';

  rootEl.replaceChildren();

  if (isWorkout) {
    rootEl.append(workoutTopbar(), statsRow());
  } else {
    rootEl.append(routineTopbar(draft), routineTitleField(draft));
  }

  scrollEl = h('div', { class: 'log-scroll' });
  paintExercises();
  rootEl.append(scrollEl);

  // pill PR en overlay haut d'écran (PrBadgeHost)
  prHostEl = h('div', { class: 'prhost' });
  paintPrBadge();
  rootEl.append(prHostEl);

  buildDock();

  liveTimers.push(setInterval(paintStats, 1000)); // produceState 1 s (durée)
  liveTimers.push(setInterval(paintRest, 250));   // produceState 250 ms (RestBar)
  paintStats();
  paintRest();
}

/** Re-render locale : préserve la position de scroll (pas d'emit global). */
function rerender() {
  if (!rootEl || !rootEl.isConnected || !store.draft) return;
  const st = scrollEl ? scrollEl.scrollTop : 0;
  build();
  if (scrollEl) scrollEl.scrollTop = st;
}

/* ---------------- barre supérieure mode séance (∨ Entraînement | ⏱ | Terminer) ---------------- */

function workoutTopbar() {
  return h('div', { class: 'wtop' },
    h('button', {
      class: 'wmenubtn',
      onclick: e => ctxMenu(e.currentTarget, [
        { label: L10n.s('Workout notes', 'Notes de la séance'), icon: 'notes', onClick: openWorkoutNotes },
        {
          label: L10n.s('Discard workout', 'Supprimer la séance'), red: true,
          onClick: () => { if (hasData()) confirmDiscardWorkout(); else doDiscardWorkout(); },
        },
      ]),
    }, icon('keyboard_arrow_down', { size: 24 }),
      h('span', { class: 'wt' }, L10n.s('Training', 'Entraînement', 'Entrenamiento', 'Training'))),
    h('div', { style: { flex: 1 } }),
    icon('timer', { size: 20 }), // décoratif (Logger.kt)
    h('div', { style: { width: '12px' } }),
    h('button', {
      class: 'finishbtn',
      onclick: () => { if (anyDone()) nav.nav.push('summary'); else noSetsDialog(); },
    }, L10n.s('Finish', 'Terminer')),
  );
}

/** LiveStat ×3 + 2 BodyMap — durées depuis draft.startedAt, tick 1 s. */
function statsRow() {
  const dur = h('div');
  const vol = h('div');
  const sets = h('div');
  const mapF = h('div', { class: 'bm' });
  const mapB = h('div', { class: 'bm' });
  const liveStat = (v, label, acc) => h('div', { class: 'lstat' + (acc ? ' acc' : '') },
    h('div', { class: 'l' }, label), h('div', { class: 'v' }, v));
  statsRefs = { dur, vol, sets, mapF, mapB };
  paintBodyMaps();
  return h('div', { class: 'livestats' },
    liveStat(dur, L10n.s('Duration', 'Durée'), true),
    liveStat(vol, L10n.s('Volume', 'Volume'), false),
    liveStat(sets, L10n.s('Sets', 'Séries'), false),
    mapF, mapB);
}

function paintBodyMaps() {
  if (!statsRefs || !statsRefs.mapF.isConnected || !store.draft) return;
  const muscles = new Set(store.draft.exercises.map(e => e.muscle).filter(Boolean));
  statsRefs.mapF.replaceChildren(bodyMap(true, muscles));
  statsRefs.mapB.replaceChildren(bodyMap(false, muscles));
}

function paintStats() {
  if (!statsRefs || !statsRefs.dur.isConnected || !store.draft) return;
  const d = store.draft;
  const unit = store.settings.unit;
  let liveVol = 0, liveSets = 0;
  for (const ex of d.exercises) for (const s of ex.sets) {
    if (s.done && (s.kg != null || s.reps != null || s.mins != null || s.km != null)) {
      if (s.kg != null && s.reps != null) liveVol += s.kg * s.reps;
      liveSets++;
    }
  }
  const tick = Date.now();
  statsRefs.dur.textContent = Calc.fmtClock(Math.max(0, tick - (d.startedAt ?? tick)));
  statsRefs.vol.textContent = `${Calc.fmtVol(liveVol, unit)} ${Calc.unitLabel(unit)}`;
  statsRefs.sets.textContent = String(liveSets);
}

/* ---------------- barre supérieure mode routine (Annuler | titre | Enregistrer ⋮) ---------------- */

function routineTopbar(draft) {
  return h('div', { class: 'wtop' },
    h('button', {
      class: 'lnk',
      onclick: () => { if (hasData()) confirmDiscardRoutine(); else doDiscardRoutine(); },
    }, L10n.s('Cancel', 'Annuler')),
    h('div', { class: 'rtitle' },
      draft.routineId != null ? L10n.s('Edit Routine', 'Modifier la Routine') : L10n.s('Create Routine', 'Créer une Routine')),
    h('button', { class: 'finishbtn', onclick: trySaveRoutine }, L10n.s('Save', 'Enregistrer')),
    h('button', {
      class: 'iconbtn', style: { width: '28px', height: '28px', flex: '0 0 auto' },
      onclick: e => ctxMenu(e.currentTarget, [
        draft.routineId != null ? {
          label: L10n.s('Delete routine', 'Supprimer la routine'), red: true, onClick: confirmDeleteRoutine,
        } : null,
        {
          label: L10n.s('Discard changes', 'Ignorer les modifications'), red: true,
          onClick: () => { if (hasData()) confirmDiscardRoutine(); else doDiscardRoutine(); },
        },
      ]),
    }, icon('more_vert', { size: 17 })),
  );
}

/** Champ titre : 24/700 sur filet, persisté à la frappe (BasicTextField du Logger.kt). */
function routineTitleField(draft) {
  const input = h('input', { class: 'rname', placeholder: L10n.s('Routine title', 'Titre de la routine'), value: draft.name, autocomplete: 'off' });
  input.addEventListener('input', () => { draft.name = input.value; S.persistDraftNow(); });
  return h('div', { class: 'rname-wrap' }, input, h('div', { class: 'hairline' }));
}

function trySaveRoutine() {
  const d = store.draft; if (!d) return;
  const n = (d.name || '').trim();
  if (!n) { toast(L10n.s('Name your routine first', 'Donne un nom à ta routine')); return; }
  if (!d.exercises.length) { toast(L10n.s('Add at least one exercise', 'Ajoute au moins un exercice')); return; }
  S.saveRoutine(n);
  nav.nav.pop();
  toast(L10n.s('Routine saved', 'Routine enregistrée'));
}

/* ---------------- liste d'exercices ---------------- */

function paintExercises() {
  const d = store.draft;
  const isWorkout = d.mode === 'workout';
  scrollEl.replaceChildren();
  if (!d.exercises.length) {
    scrollEl.append(emptyState(isWorkout
      ? 'Aucun exercice.\nTouche « Ajouter un Exercice » pour commencer.'
      : 'Cette routine est vide.\nTouche « Ajouter un Exercice » pour la construire.'));
  }
  d.exercises.forEach((ex, i) => scrollEl.append(exCard(ex, i)));
  scrollEl.append(
    h('div', { class: 'addex-wrap' },
      h('button', { class: 'btn-blue-row', onclick: () => openPicker('add') },
        icon('add', { size: 20 }), L10n.s('Add Exercise', 'Ajouter un Exercice', 'Añadir Ejercicio', 'Übung hinzufügen'))),
    h('div', { style: { height: '140px' } })); // dockspace
}

/** ExCard — barre superset 3px, cercle 46, nom bleu 17/700, notes, ligne repos, entêtes, séries. */
function exCard(ex, ei) {
  const d = store.draft;
  const isWorkout = d.mode === 'workout';
  const isLast = ei === d.exercises.length - 1;
  const isCardio = ex.muscle === 'Cardio' || Calc.isCardioName(ex.name);
  const prev = isWorkout ? S.prevFor(ex.name) : null;
  const cols = isWorkout ? 'g5' : 'g4';

  // titre : cercle + nom bleu + ⋮ (toute la ligne → détail exercice)
  const head = h('div', { class: 'exsection-head', onclick: () => nav.nav.push('exerciseDetail', { name: ex.name }) },
    ex.superset ? h('div', { class: 'ssbar' }) : null,
    exCircle(ex.muscle || 'Chest', 46),
    h('div', { class: 'exname' }, exName(ex.name)),
    h('button', {
      class: 'iconbtn', style: { width: '30px', height: '30px', flex: '0 0 auto' },
      onclick: e => { e.stopPropagation(); exMenu(e.currentTarget, ex, isLast); },
    }, icon('more_vert', { size: 20, cls: 't-mut' })));

  // notes : placeholder ou aperçu 📝 4 lignes → dialogue
  const notes = h('div', {
    class: 'exnotes',
    onclick: () => promptDialog({
      title: exName(ex.name),
      value: ex.notes || '',
      placeholder: L10n.s('Exercise notes…', "Notes de l'exercice…"),
      multiline: true,
      confirmLabel: L10n.s('Save', 'Enregistrer'),
      onConfirm: v => { ex.notes = v; S.persistDraftNow(); rerender(); },
    }),
  }, ex.notes
    ? h('div', { class: 'exnotes-prev' }, icon('notes', { size: 14, cls: 't-mut' }), h('div', { class: 'exnotes-txt' }, ex.notes))
    : L10n.s('Add notes here…', 'Ajouter des notes ici…'));

  // ligne bleue « Repos: 1min 30s »
  const restLine = h('button', { class: 'exrest', onclick: () => openRestSheet(ex) },
    icon('timer', { size: 15 }),
    L10n.s('Rest: %1$s', 'Repos: %1$s').replace('%1$s', fmtRestLabel(ex.restSec ?? store.settings.restSec)));

  // entêtes de colonnes
  const unitHead = h('div', { class: 'hcell' },
    icon('fitness_center', { size: 12 }), Calc.unitLabel(store.settings.unit).toUpperCase());
  const headRow = h('div', { class: `setgrid-head ${cols}` },
    h('div', { class: 'hcell' }, L10n.s('SET', 'SÉRIE')),
    isWorkout ? h('div', { class: 'hcell' }, L10n.s('PREVIOUS', 'PRÉCÉDENT')) : null,
    isCardio ? h('div', { class: 'hcell' }, L10n.s('MIN', 'MIN')) : unitHead,
    isCardio ? h('div', { class: 'hcell' }, L10n.s('KM', 'KM')) : h('div', { class: 'hcell' }, L10n.s('REPS', 'RÉPS')),
    isWorkout ? h('div', { class: 'hcell' }, icon('check', { size: 15 })) : h('div'));

  const sec = h('div', { class: 'exsection' }, head, notes, restLine,
    h('div', { style: { height: '8px' } }), headRow, h('div', { style: { height: '4px' } }));
  ex.sets.forEach((s, si) => sec.append(setRow(s, si, ex, prev ? prev[si] : null, isCardio)));

  // « Ajouter une Série » — fond Card2, copie la dernière, décochée
  sec.append(h('button', {
    class: 'addset',
    onclick: () => {
      const last = ex.sets[ex.sets.length - 1] || null;
      ex.sets.push(newSet(last?.kg, last?.reps, last?.mins, last?.km, false));
      S.persistDraftNow();
      rerender();
    },
  }, icon('add', { size: 18 }), L10n.s('Add Set', 'Ajouter une Série')));

  wireDrag(sec);
  return sec;
}

/** Menu ⋮ d'un exercice (ExCard DropdownMenu). */
function exMenu(anchor, ex, isLast) {
  ctxMenu(anchor, [
    { label: L10n.s('Replace exercise', "Remplacer l'exercice"), icon: 'swap_horiz', onClick: () => openPicker('replace', ex) },
    {
      label: L10n.s('Duplicate exercise', "Dupliquer l'exercice"), icon: 'content_copy',
      onClick: () => {
        const list = store.draft?.exercises; if (!list) return;
        const idx = list.indexOf(ex); // par identité : ei peut être périmé
        if (idx >= 0) list.splice(idx + 1, 0, {
          name: ex.name, muscle: ex.muscle, notes: ex.notes, superset: ex.superset, restSec: ex.restSec,
          sets: ex.sets.map(s => newSet(s.kg, s.reps, s.mins, s.km, s.done)),
        });
        S.persistDraftNow();
        rerender();
      },
    },
    {
      label: L10n.s('Rest timer', 'Minuteur de repos') + (ex.restSec != null ? ` : ${ex.restSec}s` : ''),
      icon: 'timer', onClick: () => openRestSheet(ex),
    },
    !isLast ? {
      label: ex.superset ? L10n.s('Remove superset', 'Retirer le superset') : L10n.s('Superset with next', 'Superset avec le suivant'),
      icon: 'link',
      onClick: () => { ex.superset = !ex.superset; S.persistDraftNow(); rerender(); },
    } : null,
    {
      label: L10n.s('Delete exercise', "Supprimer l'exercice"), icon: 'delete', red: true,
      onClick: () => {
        const list = store.draft?.exercises; if (!list) return;
        const i = list.indexOf(ex);
        if (i >= 0) list.splice(i, 1);
        S.persistDraftNow();
        rerender();
      },
    },
  ]);
}

/* ---------------- drag & drop long-press 400 ms (pattern routineCard de training.js) ---------------- */

function wireDrag(sec) {
  let pressTimer = null, dragging = false, startY = 0;
  const LONGPRESS = 400;
  sec.addEventListener('pointerdown', e => {
    if (e.target.closest('button, input')) return;
    startY = e.clientY;
    pressTimer = setTimeout(() => {
      dragging = true;
      sec.classList.add('dragging');
      try { sec.setPointerCapture(e.pointerId); } catch {}
      if (navigator.vibrate) navigator.vibrate(10);
    }, LONGPRESS);
  });
  sec.addEventListener('pointermove', e => {
    if (!dragging) {
      if (Math.abs(e.clientY - startY) > 8) clearTimeout(pressTimer); // scroll simple : pas de drag
      return;
    }
    e.preventDefault();
    const dy = e.clientY - startY;
    sec.style.transform = `scale(1.03) translateY(${dy}px)`;
    const myMid = sec.getBoundingClientRect().top + sec.offsetHeight / 2;
    const sibs = [...sec.parentElement.querySelectorAll('.exsection')];
    const myIdx = sibs.indexOf(sec);
    for (const sib of sibs) {
      if (sib === sec) continue;
      const r2 = sib.getBoundingClientRect();
      const sibMid = r2.top + r2.height / 2;
      const sibIdx = sibs.indexOf(sib);
      const goingDown = sibIdx > myIdx && myMid > sibMid + 2;
      const goingUp = sibIdx < myIdx && myMid < sibMid - 2;
      if (goingDown || goingUp) {
        moveExercise(myIdx, sibIdx); // franchissement du milieu du voisin → réordonne + re-render
        return;
      }
    }
  });
  const endDrag = () => {
    clearTimeout(pressTimer);
    if (dragging) { dragging = false; sec.classList.remove('dragging'); sec.style.transform = ''; }
  };
  sec.addEventListener('pointerup', endDrag);
  sec.addEventListener('pointercancel', endDrag);
  sec.addEventListener('contextmenu', e => { if (dragging) e.preventDefault(); });
}

function moveExercise(from, to) {
  const list = store.draft?.exercises; if (!list) return;
  if (from === to || from < 0 || from >= list.length || to < 0 || to >= list.length) return;
  list.splice(to, 0, list.splice(from, 1)[0]);
  S.persistDraftNow();
  rerender();
}

/* ---------------- ligne de série (SetRow) ---------------- */

function setRow(s, si, ex, prevText, isCardio) {
  const isWorkout = store.draft.mode === 'workout';
  const isPr = isWorkout && s.done && (s.prW || s.prE);
  const cols = isWorkout ? 'g5' : 'g4';
  const row = h('div', { class: `setrow-grid ${cols}` + (isPr ? ' setrow-pr' : '') });

  // chip numéro 38px — ou médaillon 🏆 sur ligne PR ; ouvre copier/supprimer
  row.append(isPr
    ? h('button', { class: 'prmedal', onclick: e => setMenu(e.currentTarget, s, si, ex) }, icon('emoji_events', { size: 20, cls: 't-gold' }))
    : h('button', { class: 'setnum', onclick: e => setMenu(e.currentTarget, s, si, ex) }, String(si + 1)));

  if (isWorkout) row.append(h('div', { class: 'prevtxt' }, prevText ?? '—'));

  const unit = store.settings.unit;
  if (isCardio) {
    row.append(
      setField(String(s.mins ?? ''), 'numeric', v => { s.mins = toIntFiltered(v, 3); S.persistDraftNow(); }, () => s.mins != null ? String(s.mins) : ''),
      setField(s.km != null ? Calc.trimNum(s.km) : '', 'decimal', v => { s.km = toFloatFiltered(v, 6); S.persistDraftNow(); }, () => s.km != null ? Calc.trimNum(s.km) : ''));
  } else {
    row.append(
      setField(Calc.fmtKg(s.kg, unit), 'decimal', v => { s.kg = Calc.toKg(v, unit); S.persistDraftNow(); }, () => s.kg != null ? Calc.fmtKg(s.kg, unit) : ''),
      setField(s.reps != null ? String(s.reps) : '', 'numeric', v => { s.reps = toIntFiltered(v, 4); S.persistDraftNow(); }, () => s.reps != null ? String(s.reps) : ''));
  }

  if (isWorkout) {
    const chk = h('button', { class: 'setcheck' + (s.done ? ' on' : '') }, icon('check', { size: 22 }));
    chk.addEventListener('click', () => toggleDone(ex, s, chk));
    row.append(chk);
  } else {
    // éditeur de routine : poubelle directe
    row.append(h('button', {
      class: 'iconbtn', style: { width: '40px', height: '40px', justifySelf: 'center' },
      onclick: () => deleteSet(ex, s),
    }, icon('delete', { size: 18, cls: 't-mut' })));
  }
  return row;
}

/** SetField — le texte local prime pendant le focus ; la valeur externe s'applique au blur. */
function setField(init, inputMode, onInput, display) {
  const input = h('input', { class: 'setfield', inputMode, placeholder: '-', value: init, autocomplete: 'off' });
  input.addEventListener('input', () => onInput(input.value));
  input.addEventListener('blur', () => { input.value = display(); });
  return input;
}

/** Menu chip de série : copier après (décochée) / supprimer (dernière → supprime l'exo). */
function setMenu(anchor, s, si, ex) {
  ctxMenu(anchor, [
    {
      label: L10n.s('Copy set', 'Copier la série'), icon: 'content_copy',
      onClick: () => {
        const i = ex.sets.indexOf(s);
        if (i >= 0) ex.sets.splice(i + 1, 0, newSet(s.kg, s.reps, s.mins, s.km, false));
        S.persistDraftNow();
        rerender();
      },
    },
    { label: L10n.s('Delete set', 'Supprimer la série'), icon: 'delete', red: true, onClick: () => deleteSet(ex, s) },
  ]);
}

function deleteSet(ex, s) {
  const d = store.draft; if (!d) return;
  if (ex.sets.length === 1) {
    const i = d.exercises.indexOf(ex); // par identité (double-tap avant re-render)
    if (i >= 0) d.exercises.splice(i, 1);
  } else {
    const i = ex.sets.indexOf(s);
    if (i >= 0) ex.sets.splice(i, 1);
  }
  S.persistDraftNow();
  rerender();
}

/** Coche : toggle done + PR + minuteur de repos (SetRow check clickable). */
function toggleDone(ex, s, chk) {
  s.done = !s.done;
  S.persistDraftNow();
  if (s.done) {
    evaluatePr(ex, s);
    restStart(ex.restSec ?? store.settings.restSec, exName(ex.name), ex.muscle);
  }
  if (s.prW || s.prE) { rerender(); return; } // médaillon ↔ numéro : re-render local
  chk.classList.toggle('on', s.done);
  chk.classList.remove('pop');
  void chk.offsetWidth; // relance l'animation (haptique visuel)
  chk.classList.add('pop');
  paintStats();
  paintRest();
}

/** evaluatePr — records vs historique strictement antérieur, contrôles indépendants. */
function evaluatePr(ex, s) {
  const kg = s.kg;
  if (kg == null) return;
  const reps = s.reps;
  if (reps == null) return;
  if (kg <= 0 || reps <= 0) return;
  const pr = S.prFor(ex.name);
  const bestW = pr ? pr.weight : 0;
  const bestE = pr ? pr.e1rm : 0;
  const e = Calc.e1rm(kg, reps);
  const unit = store.settings.unit;
  if (kg > bestW && !s.prW) {
    s.prW = true;
    showPrBadge(ex.name, ex.muscle, L10n.s('Heaviest Weight', 'Plus Gros Poids'),
      `${Calc.fmtKg(kg, unit)} ${Calc.unitLabel(unit)}`);
  }
  if (e > bestE && !s.prE) {
    s.prE = true;
    showPrBadge(ex.name, ex.muscle, L10n.s('Best Est. 1RM', 'Meilleure Est. 1RM'),
      `${Calc.fmtKg(e, unit)} ${Calc.unitLabel(unit)}`);
  }
}

/* ---------------- pill PR (PrBadge / PrBadgeHost) ---------------- */

function showPrBadge(ex, muscle, kind, value) {
  prBadge.current = { ex, muscle, kind, value };
  clearTimeout(prTimer);
  prTimer = setTimeout(() => { prBadge.current = null; paintPrBadge(); }, 4200);
  paintPrBadge();
}

function paintPrBadge() {
  if (!prHostEl) return;
  const b = prBadge.current;
  prHostEl.replaceChildren();
  if (!b) return;
  prHostEl.append(h('div', { class: 'prpill' },
    exCircle(b.muscle || 'Chest', 40),
    h('div', { class: 'prpill-info' },
      h('div', { class: 'prpill-nm' }, exName(b.ex)),
      h('div', { class: 'prpill-k' }, `${b.kind} - ${b.value}`)),
    h('button', {
      class: 'prpill-x',
      onclick: () => { prBadge.current = null; clearTimeout(prTimer); paintPrBadge(); },
    }, icon('close', { size: 22, cls: 't-mut' }))));
}

/* ---------------- RestSheet (feuille « Minuteur de Repos ») ---------------- */

function openRestSheet(ex) {
  const initial = ex.restSec ?? store.settings.restSec;
  const values = [];
  for (let i = 3; i <= 120; i++) values.push(i * 5); // 15 s → 10 min, pas 5 (RestSheet)
  let sel = Math.min(600, Math.max(15, Math.trunc((initial + 2) / 5) * 5)); // snap grille 5 s
  sheet({
    title: L10n.s('Rest Timer', 'Minuteur de Repos'),
    okLabel: L10n.s('Done', 'Terminé'),
    onOk: () => { ex.restSec = sel; S.persistDraftNow(); rerender(); },
    render(body, close) {
      body.classList.add('lb-log-rest');
      const big = h('div', { class: 'rest-big' });
      const list = h('div', { class: 'rest-list' });
      const rows = [];
      values.forEach(v => {
        const r = h('button', { class: 'rest-row', onclick: () => { sel = v; paint(); } }, fmtRestLabel(v));
        rows.push([v, r]);
        list.append(r);
      });
      const paint = () => {
        big.textContent = L10n.s('Rest: %1$s', 'Repos: %1$s').replace('%1$s', fmtRestLabel(sel));
        for (const [v, r] of rows) r.classList.toggle('sel', v === sel);
      };
      paint();
      body.append(big, list,
        h('div', { class: 'rest-actions' },
          h('button', {
            class: 'btn-ghost t-mut',
            onclick: () => { ex.restSec = null; S.persistDraftNow(); rerender(); close(); },
          }, L10n.s('Use default', 'Par défaut'))));
      const selIndex = values.indexOf(sel);
      requestAnimationFrame(() => {
        const r = list.children[selIndex];
        if (r) list.scrollTop = Math.max(0, r.offsetTop - list.clientHeight / 2 + r.offsetHeight / 2);
      });
    },
  });
}

/* ---------------- RestBar (dock bas : −15 | mm:ss | +15 | Passer) ---------------- */

function buildDock() {
  dockProgress = h('div', { class: 'rest-progress' });
  dockTime = h('div', { class: 'rest-time' }, '');
  dockEl = h('div', { class: 'restbar' }, dockProgress,
    h('button', { class: 'restbtn', onclick: () => { minus15(); paintRest(); } }, '−15'),
    dockTime,
    h('button', { class: 'restbtn', onclick: () => { plus15(); paintRest(); } }, '+15'),
    h('button', { class: 'restbtn skip', onclick: () => { restClear(); paintRest(); } }, L10n.s('Skip', 'Passer')));
}

function paintRest() {
  if (!rootEl || !rootEl.isConnected || !dockEl) return;
  if (rest.endAt > 0) {
    if (!dockEl.isConnected) rootEl.append(dockEl);
    const rem = remainingMs();
    const over = isOver();
    const frac = rest.totalMs > 0 ? Math.min(1, Math.max(0, rem / rest.totalMs)) : 0;
    dockProgress.style.width = `${frac * 100}%`;
    const shown = Math.floor(rem / 1000);
    dockTime.textContent = `${String(Math.floor(shown / 60)).padStart(2, '0')}:${String(shown % 60).padStart(2, '0')}`;
    dockTime.classList.toggle('over', over);
  } else if (dockEl.isConnected) {
    dockEl.remove();
  }
}

function wireRest() {
  if (restWired) return;
  restWired = true;
  onRest(paintRest); // +15/−15/Passer/démarrage : repaint immédiat
}

/* ---------------- dialogues séance / routine ---------------- */

function openWorkoutNotes() {
  const d = store.draft; if (!d) return;
  promptDialog({
    title: L10n.s('Workout notes', 'Notes de la séance'),
    value: d.notes || '',
    placeholder: L10n.s('How did it feel?', "Comment ça s'est passé ?"),
    multiline: true,
    confirmLabel: L10n.s('Save', 'Enregistrer'),
    onConfirm: v => { d.notes = v; S.persistDraftNow(); },
  });
}

function doDiscardWorkout() {
  restClear();
  S.discardDraft();
  nav.nav.toTab(0);
  toast(L10n.s('Discarded', 'Supprimée'));
}

function confirmDiscardWorkout() {
  confirmDialog({
    title: L10n.s('Discard workout?', 'Supprimer la séance ?'),
    message: L10n.s('Your logged sets from this session will be lost.', 'Les séances enregistrées de cette session seront perdues.'),
    confirmLabel: L10n.s('Discard', 'Supprimer'),
    destructive: true,
    onConfirm: doDiscardWorkout,
  });
}

function doDiscardRoutine() {
  restClear();
  S.discardDraft();
  nav.nav.pop();
  toast(L10n.s('Discarded', 'Supprimée'));
}

function confirmDiscardRoutine() {
  confirmDialog({
    title: L10n.s('Discard changes?', 'Ignorer les modifications ?'),
    message: L10n.s('Changes to this routine will be lost.', 'Les modifications de cette routine seront perdues.'),
    confirmLabel: L10n.s('Discard', 'Supprimer'),
    destructive: true,
    onConfirm: doDiscardRoutine,
  });
}

function confirmDeleteRoutine() {
  confirmDialog({
    title: L10n.s('Delete routine?', 'Supprimer la routine ?'),
    message: L10n.s('This routine will be removed from your list.', 'Cette routine sera retirée de ta liste.'),
    confirmLabel: L10n.s('Delete', 'Supprimer'),
    destructive: true,
    onConfirm: () => {
      const d = store.draft;
      if (d && d.routineId != null) S.deleteRoutine(d.routineId);
      restClear();
      S.discardDraft();
      nav.nav.pop();
      toast(L10n.s('Routine deleted', 'Routine supprimée'));
    },
  });
}

function noSetsDialog() {
  dialog({
    title: L10n.s('No sets completed', 'Aucune série terminée'),
    message: L10n.s('There is nothing to save yet. Discard this workout?', "Il n'y a rien à enregistrer pour l'instant. Supprimer cette séance ?"),
    actions: [
      { label: L10n.s('Keep editing', 'Continuer'), class: 'acc' },
      { label: L10n.s('Discard', 'Supprimer'), class: 'red', onClick: doDiscardWorkout },
    ],
  });
}

/* ---------------- picker d'exercices (PickerContent) ---------------- */
// mode 'add' : multi-sélection, ajout à la validation OK (+ toast « <nom EN> ajouté »).
// mode 'replace' : remplace name+muscle de l'exercice cible (ExCard showReplace).

function openPicker(mode, targetEx = null) {
  let q = '';
  let mus = 'All';
  const picked = new Set();
  const sh = sheet({
    title: L10n.s('Select exercises', 'Choisir des exercices'),
    okLabel: L10n.s('OK', 'OK'),
    onOk: applyPicks,
    render(body) {
      body.classList.add('lb-log-pk');
      const input = h('input', { placeholder: L10n.s('Search exercises', 'Rechercher des exercices'), autocomplete: 'off' });
      input.addEventListener('input', () => { q = input.value; paintList(); });
      const chips = h('div', { class: 'pk-chips' });
      const list = h('div');
      body.append(h('div', { class: 'searchbar' }, icon('search', { size: 17 }), input), chips, list);

      const paintChips = () => {
        chips.replaceChildren(...['All', ...MUSCLES].map(m => chipEl(
          m === 'All' ? L10n.s('All', 'Tous') : muscleName(m),
          mus === m,
          () => { mus = m; paintChips(); paintList(); })));
      };
      paintChips();

      function paintList() {
        const all = [...EXERCISES, ...store.customs.map(c => ({ name: c.name, muscle: c.muscle, equip: c.equip }))];
        const filtered = all
          .filter(e => (mus === 'All' || e.muscle === mus) && matches(q, e.name))
          .sort((a, b) => exName(a.name).localeCompare(exName(b.name)));
        list.replaceChildren(
          h('button', {
            class: 'pcreate',
            onclick: () => createExerciseDialog(q, created => { applyCreated(created); }),
          }, icon('add', { size: 17 }), L10n.s('Create exercise', 'Créer un exercice')));
        if (!filtered.length) list.append(emptyState(L10n.s('No exercises found.', 'Aucun exercice trouvé.')));
        for (const e of filtered) {
          const isSel = picked.has(e.name);
          list.append(h('button', {
            class: 'prow' + (isSel ? ' sel' : ''),
            onclick: () => {
              if (picked.has(e.name)) picked.delete(e.name);
              else { if (mode === 'replace') picked.clear(); picked.add(e.name); }
              paintList();
            },
          },
          exCircle(e.muscle || 'Chest', 46),
          h('div', { class: 'pinfo' },
            h('div', { class: 'pnm' }, exName(e.name)),
            h('div', { class: 'psub' }, `${muscleName(e.muscle)} · ${equipName(e.equip)}`)),
          h('div', { class: 'padd' }, icon(isSel ? 'check' : 'add', { size: 17 }))));
        }
      }
      paintList();
    },
  });

  function replaceWith(newName) {
    const def = exDef(newName);
    targetEx.name = newName;
    if (def) targetEx.muscle = def.muscle; // EX[newName]?.muscle ?: muscle courante
    S.persistDraftNow();
  }

  function applyPicks() {
    const names = [...picked];
    if (!names.length) return;
    if (mode === 'replace' && targetEx) {
      replaceWith(names[0]);
      rerender();
      return;
    }
    for (const n of names) {
      S.addExToDraft(n);
      toast(`${n} ajouté`); // nom canonique EN (comportement Android)
    }
    rerender();
  }

  function applyCreated(created) {
    // Créer depuis le picker : ajoute l'exo et ferme la feuille (Logger.kt showCreate)
    if (mode === 'replace' && targetEx) replaceWith(created);
    else { S.addExToDraft(created); toast(`${created} ajouté`); }
    sh.close();
    rerender();
  }
}

/* ---------------- dialogue « Nouvel exercice » (CreateExerciseDialog, Exercises.kt) ---------------- */

function createExerciseDialog(initialName, onCreated) {
  const equips = ['Barbell', 'Dumbbell', 'Machine', 'Cable', 'Bodyweight', 'Other'];
  const st = { name: initialName || '', muscle: 'Chest', equip: 'Barbell' };
  open();

  function open() {
    const nameInput = h('input', { class: 'field', value: st.name, maxlength: 48, autocomplete: 'off' });
    nameInput.addEventListener('input', () => { st.name = nameInput.value.slice(0, 48); });
    const muscleVal = h('div', { class: 'vl' }, muscleName(st.muscle));
    const equipVal = h('div', { class: 'vl' }, equipName(st.equip));
    const pickRow = (label, val, onRow) => h('button', { class: 'cx-row', onclick: onRow },
      h('div', { class: 'lb' }, label), val, icon('expand_more', { size: 18, cls: 't-mut' }));
    dialog({
      title: L10n.s('New exercise', 'Nouvel exercice'),
      body: h('div', { class: 'lb-log-cx' },
        h('div', { class: 'cx-label' }, L10n.s('Name', 'Nom')),
        nameInput,
        h('div', { class: 'cx-label' }, L10n.s('Muscle group', 'Groupe musculaire')),
        pickRow(L10n.s('Muscle group', 'Groupe musculaire'), muscleVal, () => listDialog({
          title: L10n.s('Muscle group', 'Groupe musculaire'),
          items: MUSCLES,
          render: m => muscleName(m),
          onPick: m => { st.muscle = m; muscleVal.textContent = muscleName(m); },
        })),
        h('div', { class: 'cx-label' }, L10n.s('Equipment', 'Équipement')),
        pickRow(L10n.s('Equipment', 'Équipement'), equipVal, () => listDialog({
          title: L10n.s('Equipment', 'Équipement'),
          items: equips,
          render: e => equipName(e),
          onPick: e => { st.equip = e; equipVal.textContent = equipName(e); },
        }))),
      actions: [
        { label: L10n.s('Cancel', 'Annuler'), class: 'mut' },
        {
          label: L10n.s('Create', 'Créer'), class: 'acc',
          onClick: () => {
            if (!st.name.trim()) {
              toast(L10n.s('Give this exercise a name', "Donne un nom à cet exercice"));
              setTimeout(open, 60); // le dialogue s'est fermé : rouvert à l'identique (état conservé)
              return;
            }
            const cx = S.addCustom(st.name, st.muscle, st.equip);
            if (!cx) {
              toast(L10n.s('This exercise already exists', 'Cet exercice existe déjà'));
              setTimeout(open, 60);
              return;
            }
            toast(L10n.s('Exercise created', 'Exercice créé'));
            onCreated(cx.name);
          },
        },
      ],
    });
  }
}

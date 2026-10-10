// Écran Exercices (liste, recherche, création) + détail d'exercice (résumé/historique/
// instructions) + calculateur de disques — port de ui/Exercises.kt.
// Fonctions Kotlin portées : ExercisesScreen, ExerciseDetailScreen, ExerciseRow,
// CreateExerciseDialog, ListPickDialog, AnimatedDemo, PlateCalcSheet, PlateBarCanvas,
// plateColor. Les libellés/sizes/ordres reprennent le Kotlin à la lettre (FR via L10n.s).
import { h, icon, toast, dialog, listDialog, sheet, ctxMenu, confirmDialog, primaryButton, emptyState, lineChart, figureSvg, sub } from '../ui.js';
import { store } from '../store.js';
import * as S from '../store.js';
import * as nav from '../router.js';
import * as Calc from '../calc.js';
import { EXERCISES, MUSCLES } from '../data.js';
import { L10n, exName, muscleName, equipName, cuesFor, stepsFor, equipHintFor, matches } from '../search.js';

nav.registerScreen('exercises', { render: renderExercises });
nav.registerScreen('exerciseDetail', { render: renderExerciseDetail });

function css() {
  if (!document.getElementById('css-exercises')) {
    const st = h('style', { id: 'css-exercises' });
    st.textContent = `
.scr-ex .mkrow { display:flex; align-items:center; gap:8px; margin:4px 16px; padding:10px 12px;
  border-radius:10px; background:var(--card2); color:var(--accent); font-size:13.5px; font-weight:600; cursor:pointer; }
.scr-ex .exsearch { display:flex; align-items:center; gap:10px; margin:4px 16px 2px; height:52px;
  border-radius:12px; background:var(--card); padding:0 16px; }
.scr-ex .exsearch input { flex:1; min-width:0; background:none; border:none; outline:none; font-size:15px; color:var(--text); }
.scr-ex .exscroll { flex:1; min-height:0; overflow-y:auto; overflow-x:hidden; scrollbar-width:none; }
.scr-ex .exscroll::-webkit-scrollbar { display:none; }
.scr-ex .exhdr { color:var(--mut); font-size:12px; font-weight:700; padding:16px 16px 6px; }
.scr-ex .exrow { display:flex; align-items:center; padding:13px 16px; cursor:pointer; }
.scr-ex .exrow:active { background:var(--card2); }
.scr-ex .exr-txt { flex:1; min-width:0; }
.scr-ex .exr-name { font-size:14.5px; font-weight:600; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.scr-ex .exr-sub { color:var(--mut); font-size:12px; margin-top:1px; }
/* dialogue création (body rendu dans #app → wrapper .scr-ex pour le scope) */
.scr-ex .dlbl { font-size:12px; color:var(--mut); margin:0 4px 6px; }
.scr-ex .pkrow { display:flex; align-items:center; border-radius:10px; background:var(--card2);
  padding:11px 12px; cursor:pointer; margin-top:10px; }
.scr-ex .pkrow .pkl { flex:1; font-size:13.5px; }
.scr-ex .pkrow .pkv { color:var(--accent); font-size:13.5px; font-weight:600; }
.scr-ex .dlg-acts { display:flex; justify-content:flex-end; gap:4px; margin:16px -8px 0; }
.scr-ex .dlg-acts button { padding:10px 14px; font-size:14px; font-weight:600; border-radius:10px; }
.scr-ex .dlg-acts .acc { color:var(--accent); }
.scr-ex .dlg-acts .mut { color:var(--mut); }
/* ---- détail ---- */
.scr-exd .exscroll { flex:1; min-height:0; overflow-y:auto; overflow-x:hidden; scrollbar-width:none; }
.scr-exd .exscroll::-webkit-scrollbar { display:none; }
.scr-exd .calcrow { display:flex; align-items:center; gap:8px; margin:4px 16px; padding:11px 12px;
  border-radius:10px; background:var(--card2); color:var(--accent); font-size:13.5px; font-weight:600; cursor:pointer; }
.scr-exd .tabrow { position:relative; display:flex; margin:0 16px; }
.scr-exd .tabrow .tb { flex:1; text-align:center; padding:10px 0 12px; font-size:14px; font-weight:500;
  color:var(--mut); cursor:pointer; }
.scr-exd .tabrow .tb.sel { color:var(--text); font-weight:700; }
.scr-exd .tabline { position:absolute; left:0; bottom:0; width:33.3333%; height:2px; background:var(--accent);
  transition:transform .25s cubic-bezier(.4,0,.2,1); }
.scr-exd .pchips { display:flex; gap:8px; padding:12px 16px; }
.scr-exd .pchip { border:1px solid var(--line2); background:var(--card); border-radius:999px;
  padding:7px 14px; font-size:12.5px; font-weight:600; color:var(--mut); }
.scr-exd .pchip.sel { border-color:var(--accent); background:var(--card2); color:var(--accent); }
.scr-exd .rec3 { display:flex; gap:14px; }
.scr-exd .rec3 .c { flex:1; }
.scr-exd .rec3 .l { font-size:11.5px; color:var(--mut); }
.scr-exd .rec3 .v { font-size:14px; font-weight:700; margin-top:1px; }
.scr-exd .clbl { color:var(--mut); font-size:13px; font-weight:500; }
.scr-exd .cval { font-size:19px; font-weight:700; margin:6px 0 4px; }
.scr-exd .hdate { display:flex; align-items:baseline; padding:8px 16px 0; }
.scr-exd .hdate .d { flex:1; font-size:14px; font-weight:700; }
.scr-exd .hdate .v { color:var(--mut); font-size:13px; }
.scr-exd .stbl { display:grid; grid-template-columns:1fr 1fr 1fr; padding-bottom:14px; }
.scr-exd .stbl .th { color:var(--mut); font-size:11px; padding:0 16px 6px; }
.scr-exd .stbl .td { font-size:13px; font-weight:600; padding:9px 16px; display:flex; align-items:center; gap:6px; }
.scr-exd .stbl .td.mut { color:var(--mut); font-weight:400; }
.scr-exd .chip-pr { color:#fff; font-size:9px; font-weight:800; background:var(--accent);
  border-radius:5px; padding:3px 6px; white-space:nowrap; }
/* instructions */
.scr-exd .demo-box { position:relative; background:#000; border-radius:10px; height:200px;
  display:flex; align-items:center; justify-content:center; overflow:hidden; }
.scr-exd .demo-box svg { height:186px; width:auto; }
.scr-exd .demo-box .pulse { animation:ex-pulse 1.5s ease-in-out infinite; }
.scr-exd .demo-box .pulse.paused { animation-play-state:paused; }
@keyframes ex-pulse { 0%,100% { transform:scale(1); } 50% { transform:scale(1.06); } }
.scr-exd .demo-play { position:absolute; right:10px; bottom:10px; width:38px; height:38px; border-radius:50%;
  background:var(--card2); color:var(--text); display:flex; align-items:center; justify-content:center; }
.scr-exd .steprow { display:flex; gap:10px; padding:7px 0; }
.scr-exd .stepn { flex:0 0 auto; width:22px; height:22px; border-radius:50%; background:var(--accent);
  color:#fff; font-size:12px; font-weight:700; display:flex; align-items:center; justify-content:center; }
.scr-exd .stept { flex:1; font-size:14px; line-height:20px; }
.scr-exd .cuerow { display:flex; padding:6px 0; }
.scr-exd .cueb { color:var(--accent); font-weight:700; font-size:14px; width:14px; }
.scr-exd .cuet { flex:1; font-size:14px; line-height:20px; }
.scr-exd .addbar { padding:8px 16px calc(10px + env(safe-area-inset-bottom)); }
/* sheet calculateur de disques (body rendu dans #app → wrapper .scr-exd pour le scope) */
.scr-exd .platesheet .sub { color:var(--mut); font-size:13px; margin:2px 0 14px; }
.scr-exd .platesheet .flbl { font-size:12px; color:var(--mut); margin:0 4px 6px; }
.scr-exd .platesheet .barlbl { font-size:12px; font-weight:800; color:var(--mut); margin:14px 4px 6px; }
.scr-exd .barchips { display:flex; gap:8px; flex-wrap:wrap; }
.scr-exd .barchip { border-radius:999px; background:var(--card2); font-size:12.5px; font-weight:600; padding:7px 12px; }
.scr-exd .barchip.sel { background:var(--accent); color:#000; }
.scr-exd .platebar { width:100%; height:110px; display:block; margin-top:18px; }
.scr-exd .plnote { color:var(--mut); font-size:13.5px; margin-top:18px; }
.scr-exd .plsum { font-size:14px; font-weight:600; margin-top:12px; }
.scr-exd .plwarn { color:var(--mut); font-size:12px; margin-top:4px; }
`;
    document.head.append(st);
  }
}

/* ============================ liste des exercices (ExercisesScreen) ============================ */

function renderExercises(el) {
  css();
  el.classList.add('scr-ex');
  let q = '';
  let mus = 'All';

  // Catalogue complet = builtins + customs (équivalent Android EX + registerCustomCatalog).
  const catalog = () => [...EXERCISES, ...store.customs];

  el.append(
    h('div', { class: 'topbar' },
      h('button', { class: 'iconbtn', onclick: () => nav.nav.pop() }, icon('arrow_back')),
      h('div', { class: 'topbar-title' }, L10n.s('Exercises', 'Exercices'))),
  );

  const chipsRow = h('div', { class: 'chips-row', style: { padding: '6px 16px' } });
  const listBox = h('div', { class: 'exscroll' });
  el.append(
    h('div', {},
      h('div', {
        class: 'mkrow',
        onclick: () => createExerciseDialog(q, null, null),   // initialName = requête courante (Kotlin)
      }, icon('add', { size: 17 }), L10n.s('Create exercise', 'Créer un exercice')),
      searchBar(),
      chipsRow),
    listBox,
  );

  function searchBar() {
    const input = h('input', {
      placeholder: L10n.s('Search exercises', 'Rechercher des exercices'),
      value: q, autocapitalize: 'off', autocomplete: 'off', spellcheck: 'false',
    });
    input.addEventListener('input', () => { q = input.value; refreshList(); });
    return h('div', { class: 'exsearch' }, icon('search', { size: 17, cls: 't-mut' }), input);
  }

  function refreshChips() {
    // chips locales (h + .chip/.sel) — ⚠️ ui.chipEl est cassé : shorthand { onclick } sur un
    // paramètre nommé onClick → ReferenceError à l'appel. Signalé, non modifié (fichier partagé).
    chipsRow.replaceChildren(...['All', ...MUSCLES].map(m => h('button', {
      class: 'chip' + (mus === m ? ' sel' : ''),
      onclick: () => { mus = m; refreshChips(); refreshList(); },
    }, m === 'All' ? L10n.s('All', 'Tous') : muscleName(m))));
  }

  function refreshList() {
    // filtre + tri exName (compareBy du Kotlin, collation approx. insensible aux accents)
    const filtered = catalog()
      .filter(d => (mus === 'All' || d.muscle === mus) && matches(q, d.name))
      .sort((a, b) => exName(a.name).localeCompare(exName(b.name), 'fr', { sensitivity: 'base' }));
    listBox.replaceChildren();
    if (!filtered.length) {
      listBox.append(emptyState(L10n.s('No exercises found.', 'Aucun exercice trouvé.')));
      return;
    }
    if (!q.trim() && mus === 'All') {
      // groupé par muscle avec en-têtes majuscules (groupBy du Kotlin, ordre de 1re apparition)
      const groups = [];
      const byMuscle = new Map();
      for (const d of filtered) {
        let g = byMuscle.get(d.muscle);
        if (!g) { g = []; byMuscle.set(d.muscle, g); groups.push([d.muscle, g]); }
        g.push(d);
      }
      for (const [muscle, exs] of groups) {
        listBox.append(h('div', { class: 'exhdr' }, muscleName(muscle).toUpperCase()));
        for (const d of exs) listBox.append(exerciseRow(d));
      }
    } else {
      for (const d of filtered) listBox.append(exerciseRow(d));
    }
    listBox.append(h('div', { style: { height: '12px' } }));
  }

  refreshChips();
  refreshList();
  // un custom fraîchement créé apparaît sans retaper la recherche (Repo.rev côté Android)
  sub(el, 'customs', () => { refreshChips(); refreshList(); });
}

/** ExerciseRow : nom FR 14.5/600 + « Muscle · Équipement » 12 + chevron. */
function exerciseRow(e) {
  return h('div', {
    class: 'exrow',
    onclick: () => nav.nav.push('exerciseDetail', { name: e.name }),
  },
    h('div', { class: 'exr-txt' },
      h('div', { class: 'exr-name' }, exName(e.name)),
      h('div', { class: 'exr-sub' }, `${muscleName(e.muscle || '')} · ${equipName(e.equip || '')}`)),
    icon('chevron_right', { size: 16, cls: 't-mut' }));
}

/* ============================ création d'exercice (CreateExerciseDialog) ============================ */

/**
 * Dialogue de création : Nom (≤48) + Groupe musculaire + Équipement (ListPickDialog).
 * ui.dialog ferme AVANT l'action → actions codées dans le body pour garder le dialogue
 * ouvert quand la validation échoue (comportement AlertDialog Kotlin return@TextButton).
 */
function createExerciseDialog(initialName, onCreated, onClose) {
  let muscle = 'Chest';
  let equip = 'Barbell';
  const equips = ['Barbell', 'Dumbbell', 'Machine', 'Cable', 'Bodyweight', 'Other'];
  const nameInput = h('input', { class: 'field', value: initialName ?? '', maxlength: '48' });
  const muscleVal = h('div', { class: 'pkv' }, muscleName(muscle));
  const equipVal = h('div', { class: 'pkv' }, equipName(equip));

  const close = dialog({
    title: L10n.s('New exercise', 'Nouvel exercice'),
    onDismiss: onClose,
    body: h('div', { class: 'scr-ex' },
      h('div', { class: 'dlbl' }, L10n.s('Name', 'Nom')),
      nameInput,
      h('div', {
        class: 'pkrow',
        onclick: () => listDialog({
          title: L10n.s('Muscle group', 'Groupe musculaire'),
          items: MUSCLES.map(m => ({ value: m, label: muscleName(m) })),
          onPick: it => { muscle = it.value; muscleVal.textContent = muscleName(muscle); },
        }),
      }, h('div', { class: 'pkl' }, L10n.s('Muscle group', 'Groupe musculaire')), muscleVal),
      h('div', {
        class: 'pkrow',
        onclick: () => listDialog({
          title: L10n.s('Equipment', 'Équipement'),
          items: equips.map(e => ({ value: e, label: equipName(e) })),
          onPick: it => { equip = it.value; equipVal.textContent = equipName(equip); },
        }),
      }, h('div', { class: 'pkl' }, L10n.s('Equipment', 'Équipement')), equipVal),
      h('div', { class: 'dlg-acts' },
        h('button', { class: 'mut', onclick: () => { close(); onClose && onClose(); } }, L10n.s('Cancel', 'Annuler')),
        h('button', { class: 'acc', onclick: tryCreate }, L10n.s('Create', 'Créer')))),
  });

  function tryCreate() {
    const v = nameInput.value;
    if (!v.trim()) { toast(L10n.s('Give this exercise a name', 'Donne un nom à cet exercice')); return; }
    const cx = S.addCustom(v, muscle, equip);
    if (!cx) { toast(L10n.s('This exercise already exists', 'Cet exercice existe déjà')); return; }
    close();
    toast(L10n.s('Exercise created', 'Exercice créé'));
    onCreated && onCreated(cx.name);
  }
}

/* ============================ détail (ExerciseDetailScreen) ============================ */

function renderExerciseDetail(el, { name }) {
  css();
  el.classList.add('scr-exd');
  let tab = 0;      // 0=Résumé 1=Historique 2=Instructions
  let period = 3;   // 0=3m 1=6m 2=1a 3=Tout

  build();
  sub(el, 'workouts', build);
  sub(el, 'customs', build);
  sub(el, 'settings', build);
  sub(el, 'draft', build);

  function build() {
    const unit = store.settings.unit;
    const uLabel = Calc.unitLabel(unit);
    const def = EXERCISES.find(d => d.name === name) || store.customs.find(c => c.name === name) || null;
    const isCustom = store.customs.some(c => c.name === name);
    el.replaceChildren();

    el.append(h('div', { class: 'topbar' },
      h('button', { class: 'iconbtn', onclick: () => nav.nav.pop() }, icon('arrow_back')),
      h('div', {
        class: 'topbar-title t-center',
        style: { overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' },
      }, exName(name)),
      isCustom
        ? h('button', { class: 'iconbtn', onclick: e => menu(e.currentTarget) }, icon('more_horiz'))
        : h('div', { style: { width: '40px', flex: '0 0 auto' } })));

    if (def && def.equip === 'Barbell') {
      el.append(h('div', {
        class: 'calcrow',
        onclick: () => plateCalcSheet(name),
      }, icon('fitness_center', { size: 16 }), L10n.s('Plate calculator', 'Calculateur de disques')));
    }

    // onglets + soulignement glissant (animateDpAsState tween 250 FastOutSlowIn)
    const labels = [
      L10n.s('Summary', 'Résumé'),
      L10n.s('History', 'Historique'),
      L10n.s('Instructions', 'Instructions'),
    ];
    const underline = h('div', { class: 'tabline', style: { transform: `translateX(${tab * 100}%)` } });
    const tabrow = h('div', { class: 'tabrow' }, labels.map((label, i) => h('button', {
      class: 'tb' + (tab === i ? ' sel' : ''),
      onclick: () => { if (tab === i) return; tab = i; syncTabs(); paintTab(); },
    }, label)));
    tabrow.append(underline);
    el.append(tabrow);

    function syncTabs() {
      [...tabrow.querySelectorAll('.tb')].forEach((b, i) => b.classList.toggle('sel', i === tab));
      underline.style.transform = `translateX(${tab * 100}%)`;
    }

    const scroller = h('div', { class: 'exscroll' });
    el.append(scroller);
    paintTab();

    function paintTab() {
      scroller.replaceChildren(tab === 0 ? summaryTab() : tab === 1 ? historyTab() : instructionsTab());
      scroller.append(h('div', { style: { height: '20px' } }));
    }

    /* ---- Résumé : chips de période + carte Records + 2 graphiques ---- */
    function summaryTab() {
      const wrap = h('div');
      const cutoff = period === 0 ? Date.now() - 91 * 86400000
        : period === 1 ? Date.now() - 182 * 86400000
          : period === 2 ? Date.now() - 365 * 86400000 : 0;
      const sessions = store.workouts
        .filter(w => w.startedAt >= cutoff)
        .map(w => { const ex = w.exercises.find(e => e.name === name); return ex ? [w, ex] : null; })
        .filter(Boolean);
      // une seule série par séance (firstOrNull Kotlin)
      const seen = new Set();
      const uniq = sessions.filter(([w]) => !seen.has(w.id) && seen.add(w.id));

      const pchips = h('div', { class: 'pchips' }, [
        L10n.s('3m', '3m'), L10n.s('6m', '6m'), L10n.s('1y', '1a'), L10n.s('All', 'Tout'),
      ].map((label, i) => h('button', {
        class: 'pchip' + (period === i ? ' sel' : ''),
        onclick: () => { period = i; wrap.replaceWith(summaryTab()); },
      }, label)));
      wrap.append(pchips);

      const heaviest = uniq.map(([w, e]) => ({
        label: Calc.fmtDateShort(w.startedAt),
        value: e.sets.filter(s => s.done).reduce((m, s) => Math.max(m, s.kg ?? 0), 0),
      }));
      const volumes = uniq.map(([w, e]) => ({
        label: Calc.fmtDateShort(w.startedAt),
        value: e.sets.filter(s => s.done).reduce((a, s) => a + (s.kg ?? 0) * (s.reps ?? 0), 0),
      }));

      if (!heaviest.length) {
        wrap.append(h('div', { class: 'empty', style: { whiteSpace: 'pre-line' } },
          L10n.s('No data yet.\nLog this exercise to see charts.', 'Aucune donnée.\nEnregistre cet exercice pour voir les graphiques.')));
        return wrap;
      }

      // carte Records 🏆
      const pr = S.prFor(name);
      const best1rm = Math.max(Calc.maxOf(uniq.flatMap(([, e]) =>
        e.sets.filter(s => (s.kg ?? 0) > 0 && (s.reps ?? 0) > 0 && Number.isFinite(s.kg) && Number.isFinite(s.reps)).map(s => Calc.e1rm(s.kg, s.reps)))), 0);
      const bestSet = uniq.flatMap(([, e]) => e.sets.map(s => s.kg).filter(kg => kg != null && Number.isFinite(kg)));
      const bestTop = bestSet.length ? Calc.maxOf(bestSet) : 0;
      const rec = h('div', { class: 'card' },
        h('div', { style: { display: 'flex', alignItems: 'center', gap: '7px' } },
          icon('emoji_events', { size: 16, cls: 't-acc' }),
          h('div', { style: { fontSize: '14px', fontWeight: 700 } }, L10n.s('Records', 'Records'))),
        h('div', { style: { height: '8px' } }),
        h('div', { class: 'rec3' },
          h('div', { class: 'c' }, h('div', { class: 'l' }, L10n.s('Best est. 1RM', '1RM max est.')),
            h('div', { class: 'v' }, `${Calc.fmtKg(best1rm, unit)} ${uLabel}`)),
          h('div', { class: 'c' }, h('div', { class: 'l' }, L10n.s('Heaviest set', 'Série lourde')),
            h('div', { class: 'v' }, `${Calc.fmtKg(bestTop, unit)} ${uLabel}`)),
          h('div', { class: 'c' }, h('div', { class: 'l' }, L10n.s('Sessions', 'Séances')),
            h('div', { class: 'v' }, String(uniq.length)))),
        pr ? h('div', { class: 't-xs', style: { marginTop: '8px' } },
          L10n.s('PR set on ', 'Record établi le ') + Calc.fmtDateShort(pr.weightDate)) : null);
      if (uniq.length >= 2) {
        const lastTop = uniq[uniq.length - 1][1].sets.filter(s => s.done).reduce((m, s) => Math.max(m, s.kg ?? 0), 0);
        const delta = lastTop - bestTop;
        const pct = bestTop > 0 ? Math.trunc(delta / bestTop * 100) : 0;
        rec.append(h('div', {
          class: 't-xs', style: {
            marginTop: '8px', fontWeight: 600,
            color: delta < 0 ? 'var(--red)' : 'var(--accent)',
          },
        }, `${L10n.s('Last session vs best', 'Dernière séance vs record')} : ${delta >= 0 ? '+' : ''}${Calc.fmtKg(delta, unit)} ${uLabel} (${pct}%)`));
      }
      wrap.append(rec);

      // graphiques : poids le plus lourd + volume total
      wrap.append(h('div', { class: 'card' },
        h('div', { class: 'clbl' }, L10n.s('Heaviest weight', 'Poids le plus lourd')),
        h('div', { class: 'cval' }, `${Calc.fmtKg(Calc.maxOf(heaviest.map(p => p.value)), unit)} ${uLabel}`),
        lineChart(heaviest, v => Calc.fmtKg(v, unit))));
      wrap.append(h('div', { class: 'card' },
        h('div', { class: 'clbl' }, L10n.s('Total volume', 'Volume total')),
        h('div', { class: 'cval' }, `${Calc.fmtVol(volumes.reduce((a, p) => a + p.value, 0), unit)} ${uLabel}`),
        lineChart(volumes, v => Calc.fmtVol(v, unit))));
      return wrap;
    }

    /* ---- Historique : séances récentes d'abord, table zébrée, PRs marqués ---- */
    function historyTab() {
      const sessions = S.workoutsDesc()
        .map(w => { const ex = w.exercises.find(e => e.name === name); return ex ? [w, ex] : null; })
        .filter(Boolean);
      const wrap = h('div');
      if (!sessions.length) {
        wrap.append(emptyState(L10n.s('No sessions logged yet.', 'Aucune séance enregistrée.')));
        return wrap;
      }
      for (const [w, ex] of sessions) {
        const cardio = ex.muscle === 'Cardio' || Calc.isCardioName(ex.name);
        const doneVol = ex.sets.filter(s => s.done).reduce((a, s) => a + (s.kg ?? 0) * (s.reps ?? 0), 0);
        const maxOf = f => ex.sets.length ? ex.sets.reduce((m, s) => Math.max(m, f(s)), 0) : null;
        const tbl = h('div', { class: 'stbl' },
          h('div', { class: 'th' }, L10n.s('SET', 'SÉRIE')),
          cardio ? h('div', { class: 'th' }, L10n.s('MIN', 'MIN')) : h('div', { class: 'th' }, unit.toUpperCase()),
          cardio ? h('div', { class: 'th' }, L10n.s('KM', 'KM')) : h('div', { class: 'th' }, L10n.s('REPS', 'RÉPS')));
        ex.sets.forEach((st, si) => {
          const bg = si % 2 === 1 ? 'rgba(42,42,45,.35)' : 'transparent'; // surfaceVariant 35% (Kotlin)
          tbl.append(h('div', { class: 'td mut', style: { background: bg } }, String(si + 1)));
          if (cardio) {
            tbl.append(h('div', { class: 'td', style: { background: bg } }, st.mins != null ? String(st.mins) : '—'));
            tbl.append(h('div', { class: 'td', style: { background: bg } }, st.km != null ? Calc.trimNum(st.km) : '—'));
          } else {
            tbl.append(h('div', { class: 'td', style: { background: bg } }, st.kg != null ? Calc.fmtKg(st.kg, unit) : '—'));
            tbl.append(h('div', { class: 'td', style: { background: bg } }, String(st.reps ?? '—'),
              (st.prW || st.prE) ? h('span', { class: 'chip-pr' }, st.prW ? 'WEIGHT PR' : '1RM PR') : null));
          }
        });
        wrap.append(h('div', {},
          h('div', { class: 'hdate' },
            h('div', { class: 'd' }, Calc.fmtDateFull(w.startedAt)),
            h('div', { class: 'v' }, cardio
              ? Calc.fmtCardioSet(maxOf(s => s.mins ?? 0), maxOf(s => s.km ?? 0))
              : `${Calc.fmtVol(doneVol, unit)} ${uLabel}`)),
          tbl));
      }
      return wrap;
    }

    /* ---- Instructions : démo pulsante + cartes équipement/muscle/étapes/conseils ---- */
    function instructionsTab() {
      const muscle = (def && def.muscle) || 'Chest';
      const equip = def ? def.equip : '';
      const wrap = h('div');

      // AnimatedDemo : silhouette pulsante (CSS scale 1↔1.06 infinite) + play/pause
      let playing = true;
      const pulse = h('div', { class: 'pulse' }, figureSvg(muscle, 140, { variant: 'dark' }));
      const playBtn = h('button', {
        class: 'demo-play',
        onclick: () => {
          playing = !playing;
          pulse.classList.toggle('paused', !playing);
          playBtn.replaceChildren(icon(playing ? 'pause' : 'play_arrow', { size: 19 }));
        },
      }, icon('pause', { size: 19 }));
      wrap.append(h('div', { class: 'card' }, h('div', { class: 'demo-box' }, pulse, playBtn)));

      const hint = equipHintFor(equip);
      wrap.append(h('div', { class: 'card' },
        h('div', { style: { fontSize: '14px', fontWeight: 700 } }, equipName(equip)),
        hint ? h('div', { style: { color: 'var(--mut)', fontSize: '13px', lineHeight: '18px', marginTop: '6px' } }, hint) : null));

      wrap.append(h('div', { class: 'card' },
        h('div', { style: { color: 'var(--mut)', fontSize: '11px', fontWeight: 700 } }, L10n.s('PRIMARY MUSCLE', 'MUSCLE PRINCIPAL')),
        h('div', { style: { fontSize: '15px', fontWeight: 700, marginTop: '6px' } }, muscleName((def && def.muscle) || ''))));

      const steps = stepsFor(name);
      if (steps.length) {
        wrap.append(h('div', { class: 'card' },
          h('div', { style: { fontSize: '14px', fontWeight: 700, marginBottom: '4px' } }, L10n.s('How to perform', "Comment réaliser l'exercice")),
          steps.map((step, si) => h('div', { class: 'steprow' },
            h('div', { class: 'stepn' }, String(si + 1)),
            h('div', { class: 'stept' }, step)))));
      }

      const cues = cuesFor(muscle);
      if (cues.length) {
        wrap.append(h('div', { class: 'card' },
          h('div', { style: { color: 'var(--mut)', fontSize: '11px', fontWeight: 700, marginBottom: '4px' } }, L10n.s('TIPS', 'CONSEILS')),
          cues.map(cue => h('div', { class: 'cuerow' },
            h('div', { class: 'cueb' }, '•'),
            h('div', { class: 'cuet' }, cue)))));
      }
      return wrap;
    }

    /* ---- bas : ajout à la séance en cours ---- */
    if (store.draft && store.draft.mode === 'workout') {
      el.append(h('div', { class: 'addbar' },
        primaryButton(L10n.s('Add to Current Workout', 'Ajouter à la séance en cours'), {
          leading: icon('add', { size: 17 }),
          onClick: () => { S.addExToDraft(name); nav.nav.pop(); toast(`${exName(name)} ajouté`); },
        })));
    }

    /* ---- menu ⋮ (custom uniquement) : suppression ---- */
    function menu(anchor) {
      ctxMenu(anchor, [{
        label: L10n.s('Delete exercise', "Supprimer l'exercice"),
        icon: 'delete',
        red: true,
        onClick: () => confirmDialog({
          title: L10n.s('Delete exercise?', "Supprimer l'exercice ?"),
          message: L10n.s(
            'It will be removed from your lists on all your devices. Past workouts keep their history.',
            'Il sera retiré de tes listes sur tous tes appareils. Les séances passées conservent leur historique.'),
          confirmLabel: L10n.s('Delete', 'Supprimer'),
          destructive: true,
          onConfirm: () => { S.deleteCustom(name); nav.nav.pop(); },
        }),
      }]);
    }
  }
}

/* ============================ calculateur de disques (PlateCalcSheet) ============================ */

/** Couleurs gym par taille de disque — recopie exacte de plateColor (Exercises.kt l.629-635). */
function plateColor(kg) {
  if (kg === 45 || kg === 25) return '#D64545'; // rouge — 45/25 lb, 25 kg
  if (kg === 35 || kg === 15) return '#E8B931'; // jaune — 35 lb, 15 kg
  if (kg === 20) return '#3F7BD8';              // bleu — 20 kg
  if (kg === 10) return '#3FA35C';              // vert — 10 (kg/lb)
  return '#9AA3AB';                             // gris — le reste
}

/** toDoubleOrNull du Kotlin : virgule→point, invalide → NaN. */
function parseTarget(t) {
  const s = String(t).replace(/,/g, '.');
  if (!/^(\d+(\.\d*)?|\.\d+)$/.test(s)) return NaN;
  const v = parseFloat(s);
  return Number.isFinite(v) ? v : NaN;
}

/** PlateCalcSheet : poids ciblé + barre → décomposition par côté + dessin SVG de la barre chargée. */
function plateCalcSheet(nameEn, onClose) {
  const unit = store.settings.unit;
  const isLb = unit === 'lb';
  const pr = S.prFor(nameEn);
  let target = pr && pr.weight != null ? Calc.fmtKg(pr.weight, unit) : '';
  let barIdx = 0;
  const bars = isLb ? [45, 35, 25, 0] : [20, 15, 10, 0];
  const barLabel = b => b === 0 ? L10n.s('No bar', 'Sans barre') : Calc.trimNum(b) + Calc.unitLabel(unit);

  sheet({
    title: L10n.s('Plate calculator', 'Calculateur de disques'),
    onClose,
    render(bodyEl) {
      const rootEl = h('div', { class: 'scr-exd platesheet' });
      bodyEl.append(rootEl);
      const input = h('input', { class: 'field', value: target, inputmode: 'decimal', maxlength: '6' });
      input.addEventListener('input', () => {
        // filtre chiffres . , et coupe à 6 (onValueChange Kotlin)
        const v = [...input.value].filter(c => /\d/.test(c) || c === '.' || c === ',').slice(0, 6).join('');
        target = v;
        if (input.value !== v) input.value = v;
        paintResult();
      });
      const chipsRow = h('div', { class: 'barchips' });
      const result = h('div');
      rootEl.append(
        h('div', { class: 'sub' }, exName(nameEn)),
        h('div', { class: 'flbl' }, `${L10n.s('Target weight', 'Poids ciblé')} (${Calc.unitLabel(unit)})`),
        input,
        h('div', { class: 'barlbl' }, L10n.s('Bar', 'Barre')),
        chipsRow,
        result);
      paintChips();
      paintResult();

      function paintChips() {
        chipsRow.replaceChildren(...bars.map((b, i) => h('button', {
          class: 'barchip' + (i === barIdx ? ' sel' : ''),
          onclick: () => { barIdx = i; paintChips(); paintResult(); },
        }, barLabel(b))));
      }

      function paintResult() {
        const tv = parseTarget(target);
        if (!Number.isFinite(tv) || tv <= 0) {
          result.replaceChildren(h('div', { class: 'plnote' }, L10n.s('Enter a target weight.', 'Saisis un poids ciblé.')));
          return;
        }
        const { perSide, total } = Calc.platesForSide(tv, bars[barIdx], unit);
        result.replaceChildren(plateBarSvg(perSide));
        if (!perSide.length) {
          result.append(h('div', { class: 'plnote' }, L10n.s('Empty bar.', 'Barre à vide.')));
          return;
        }
        result.append(h('div', { class: 'plsum' },
          `${L10n.s('Per side', 'Par côté')} : ${perSide.map(([pl, n]) => `${n}×${Calc.trimNum(pl)}`).join(' · ')}`
          + `  —  ${L10n.s('Total', 'Total')} : ${Calc.trimNum(total)}${Calc.unitLabel(unit)}`));
        if (Math.abs(total - tv) > 0.001) {
          result.append(h('div', { class: 'plwarn' },
            L10n.s('Closest load achievable with these plates.', 'Charge la plus proche atteignable avec ces disques.')));
        }
      }
    },
  });
}

/** PlateBarCanvas : barre grise #6E767E + disques proportionnels des deux côtés (SVG). */
function plateBarSvg(plates) {
  const W = 340, H = 110;
  const maxPl = plates.length ? Math.max(...plates.map(p => p[0])) : 1;
  const ordered = [...plates].sort((a, b) => b[0] - a[0]); // sortedByDescending
  const ns = 'http://www.w3.org/2000/svg';
  const svg = document.createElementNS(ns, 'svg');
  svg.setAttribute('viewBox', `0 0 ${W} ${H}`);
  svg.setAttribute('class', 'platebar');
  const cy = H / 2, barH = 7, sleeve = W * 0.30, cx = W / 2;
  const rect = (x, y, w, hh, r, fill) => {
    const e = document.createElementNS(ns, 'rect');
    e.setAttribute('x', x); e.setAttribute('y', y);
    e.setAttribute('width', w); e.setAttribute('height', hh);
    e.setAttribute('rx', Math.min(r, w / 2, hh / 2));
    e.setAttribute('fill', fill);
    svg.append(e);
  };
  // barre
  rect(cx - sleeve - 14, cy - barH / 2, 2 * sleeve + 28, barH, barH / 2, '#6E767E');
  // disques par côté, du plus gros au plus petit, proportions du Canvas Kotlin
  for (const side of [-1, 1]) {
    let x = cx + side * (sleeve + 10);
    for (const [pl, n] of ordered) {
      const w = Math.min(6 + 3 * (pl / maxPl), 16);
      const hh = Math.min(26 + 62 * (pl / maxPl), H - 6);
      for (let k = 0; k < n; k++) {
        rect(x - w / 2, cy - hh / 2, w, hh, w / 2, plateColor(pl));
        x += side * (w + 3);
      }
    }
  }
  return svg;
}

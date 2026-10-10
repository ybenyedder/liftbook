// Onglet Entraînement (routines + calendrier) + détail de routine — port de ui/Routines.kt.
import { h, icon, toast, confirmDialog, promptDialog, ctxMenu, primaryButton, exCircle, lineChart, emptyState, workoutInProgressDialog, dialog } from '../ui.js';
import { store } from '../store.js';
import * as S from '../store.js';
import * as nav from '../router.js';
import * as Calc from '../calc.js';
import { L10n, exName } from '../search.js';
import { clear as restClear } from '../restimer.js';
import { shareText } from './home.js';

nav.registerScreen('trainingTab', { render });
nav.registerScreen('routineDetail', { render: renderRoutineDetail });

function css() {
  if (!document.getElementById('css-training')) {
    const st = h('style', { id: 'css-training' });
    st.textContent = `
.scr-train .viewsel { display:flex; align-items:center; gap:8px; margin:10px 8px 0; padding:4px 8px; border-radius:10px; cursor:pointer; width:fit-content; }
.scr-train .viewsel .chev { width:26px; height:26px; border-radius:50%; background:var(--card2); display:flex; align-items:center; justify-content:center; }
.scr-train .emptybtn { display:flex; align-items:center; justify-content:center; gap:10px; margin:6px 16px 0; height:50px; border-radius:12px; background:var(--card); border:1px solid var(--line2); font-size:16px; font-weight:600; }
.scr-train .rhead { display:flex; align-items:center; padding:18px 8px 0 16px; }
.scr-train .rhead .t { flex:1; font-size:20px; font-weight:800; }
.scr-train .twin { display:flex; gap:10px; padding:12px 16px 0; }
.scr-train .twin button { flex:1; }
.scr-train .collap { display:flex; align-items:center; gap:8px; padding:14px 16px; color:var(--mut); font-size:15px; font-weight:500; cursor:pointer; }
.scr-train .rcard { margin:6px 16px; background:var(--card); border-radius:16px; padding:16px; cursor:pointer; touch-action:pan-y; }
.scr-train .rcard.dragging { background:var(--card2); transform:scale(1.02); box-shadow:0 10px 28px rgba(0,0,0,.55); }
.scr-train .rcard .nm { font-size:19px; font-weight:800; flex:1; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.scr-train .rcard .exs { color:var(--mut); font-size:15px; line-height:21px; margin-top:6px; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden; }
.scr-train .rcard .go { margin-top:14px; height:46px; border-radius:12px; background:var(--accent); color:#fff; display:flex; align-items:center; justify-content:center; font-size:15px; font-weight:600; }
.scr-train .cal-wrap { padding:12px 16px; }
.scr-train .cal-title { text-align:center; font-size:18px; font-weight:700; flex:1; }
.scr-train .cal-grid { display:grid; grid-template-columns:repeat(7,1fr); gap:6px; padding:8px 4px; }
.scr-train .cal-cell { height:40px; border-radius:50%; display:flex; align-items:center; justify-content:center; font-size:13px; font-weight:600; position:relative; }
.scr-train .cal-cell.has { background:var(--accent); color:#fff; cursor:pointer; }
.scr-train .cal-cell.today:not(.has) { border:1.5px solid var(--accent); }
.scr-train .cal-sum { text-align:center; color:var(--mut); font-size:13.5px; padding:10px 0 4px; }
.scr-rdetail .mrow { display:flex; align-items:baseline; gap:8px; padding:18px 16px 4px; }
.scr-rdetail .total { font-size:19px; font-weight:800; }
.scr-rdetail .mchips { display:flex; gap:8px; padding:8px 16px; }
.scr-rdetail .mchip { border-radius:999px; background:var(--card2); padding:8px 16px; font-size:13.5px; font-weight:600; cursor:pointer; }
.scr-rdetail .mchip.sel { background:var(--accent); color:#fff; }
.scr-rdetail .exb { margin-bottom:18px; }
.scr-rdetail .exb .nm { color:var(--accent); font-size:18px; font-weight:600; flex:1; cursor:pointer; }
.scr-rdetail .restl { display:flex; align-items:center; gap:8px; color:var(--accent); font-size:14.5px; font-weight:500; padding:10px 16px; }
.scr-rdetail .tbl { display:grid; grid-template-columns:1fr 1fr 1fr; }
.scr-rdetail .tbl .th { color:var(--mut); font-size:12px; padding:0 16px; }
.scr-rdetail .tbl .td { font-size:15px; padding:10px 16px; }
.scr-rdetail .tbl .z:nth-child(even of .td) { }
`;
    document.head.append(st);
  }
}

/* ---------------- onglet Entraînement ---------------- */

function render(el) {
  css();
  el.classList.add('scr-train');
  let view = 0;        // 0 Routines, 1 Calendrier
  let expanded = true;

  const wrap = h('div', { class: 'screen' });
  el.append(wrap);

  function guard(block) {
    if (store.draft) {
      workoutInProgressDialog({
        onResume: () => nav.nav.push('logger'),
        onRestart: () => { restClear(); S.discardDraft(); block(); },
      });
    } else block();
  }

  function body() {
    wrap.replaceChildren(
      h('div', { class: 'viewsel', onclick: e => ctxMenu(e.currentTarget, [
        { label: L10n.s('Training', 'Entraînement', 'Entrenamiento', 'Training'), icon: view === 0 ? 'check' : null, onClick: () => { view = 0; body(); } },
        { label: L10n.s('Calendar', 'Calendrier', 'Calendario', 'Kalender'), icon: view === 1 ? 'check' : null, onClick: () => { view = 1; body(); } },
      ]) },
        h('div', { class: 't-title' }, L10n.s('Training', 'Entraînement', 'Entrenamiento', 'Training')),
        h('div', { class: 'chev' }, icon('expand_more', { size: 17 })),
      ),
    );
    if (view === 1) { wrap.append(calendarView()); return; }

    wrap.append(
      h('button', {
        class: 'emptybtn',
        onclick: () => guard(() => { S.startWorkout(null); nav.nav.push('logger'); }),
      }, icon('add', { size: 20 }), L10n.s('Start an Empty Workout', 'Démarrer un Entraînement Vide', 'Iniciar un Entrenamiento Vacío', 'Leeres Workout starten')),
      h('div', { class: 'rhead' },
        h('div', { class: 't' }, L10n.s('Routines', 'Routines', 'Rutinas', 'Routinen')),
        h('button', { class: 'iconbtn', onclick: () => guard(() => { S.startRoutine(null); nav.nav.push('logger'); }) }, icon('create_new_folder'))),
      h('div', { class: 'twin' },
        h('button', { class: 'btn-sec', onclick: () => guard(() => { S.startRoutine(null); nav.nav.push('logger'); }) }, icon('post_add', { size: 17 }), L10n.s('New routine', 'Nouv. Routine', 'Nueva rutina', 'Neue Routine')),
        h('button', { class: 'btn-sec', onclick: () => nav.nav.push('exercises') }, icon('search', { size: 17 }), L10n.s('Explore', 'Explorer', 'Explorar', 'Entdecken'))),
      h('div', { class: 'collap', onclick: () => { expanded = !expanded; body(); } },
        icon('expand_more', { size: 18, cls: 't-mut' }),
        L10n.s('My routines (%1$d)', 'Mes routines (%1$d)', 'Mis rutinas (%1$d)', 'Meine Routinen (%1$d)').replace('%1$d', String(store.routines.length))),
    );
    if (expanded) {
      if (!store.routines.length) {
        wrap.append(emptyState(L10n.s('No routines yet.\nTap “New routine” to create one.', 'Aucune routine.\nTouche « Nouvelle routine » pour en créer une.')));
      } else {
        const list = h('div');
        store.routines.forEach((r, i) => list.append(routineCard(r, i, guard, body)));
        wrap.append(list);
      }
    }
    wrap.append(h('div', { style: { height: '20px' } }));
  }

  body();
}

/** Carte routine + menu ⋮ + drag long-press (DragDropState). */
function routineCard(r, index, guard, rerender) {
  const card = h('div', { class: 'rcard' },
    h('div', { style: { display: 'flex', alignItems: 'center' } },
      h('div', { class: 'nm', onclick: e => { e.stopPropagation(); nav.nav.push('routineDetail', { id: r.id }); } }, exName(r.name)),
      h('button', {
        class: 'iconbtn', style: { width: '28px', height: '28px' },
        onclick: e => { e.stopPropagation(); cardMenu(e.currentTarget, r, rerender); },
      }, icon('more_horiz', { size: 20, cls: 't-mut' }))),
    h('div', { class: 'exs' }, r.exercises.length
      ? r.exercises.map(e => exName(e.name)).join(', ')
      : L10n.s("No exercises yet", "Aucun exercice pour l'instant")),
    h('div', {
      class: 'go',
      onclick: e => { e.stopPropagation(); guard(() => { S.startWorkout(r.id); nav.nav.push('logger'); }); },
    }, L10n.s('Start routine', 'Commencer la Routine', 'Comenzar la rutina', 'Routine starten')),
  );
  card.addEventListener('click', () => nav.nav.push('routineDetail', { id: r.id }));

  /* long-press drag → réordonnancement (swap au franchissement du milieu du voisin) */
  let pressTimer = null, dragging = false, startY = 0, curY = 0;
  const LONGPRESS = 400;
  card.addEventListener('pointerdown', e => {
    if (e.target.closest('button') || e.target.closest('.go')) return;
    startY = e.clientY; curY = e.clientY;
    pressTimer = setTimeout(() => {
      dragging = true;
      card.classList.add('dragging');
      card.setPointerCapture(e.pointerId);
      if (navigator.vibrate) navigator.vibrate(10);
    }, LONGPRESS);
  });
  card.addEventListener('pointermove', e => {
    if (!dragging) {
      if (Math.abs(e.clientY - startY) > 8) clearTimeout(pressTimer);
      return;
    }
    e.preventDefault();
    const dy = e.clientY - startY;
    card.style.transform = `scale(1.02) translateY(${dy}px)`;
    const myMid = card.getBoundingClientRect().top + card.offsetHeight / 2;
    const kids = [...card.parentElement.children];
    const myIdx = kids.indexOf(card);
    for (const sib of kids) {
      if (sib === card) continue;
      const r2 = sib.getBoundingClientRect();
      const sibMid = r2.top + r2.height / 2;
      const sibIdx = kids.indexOf(sib);
      const goingDown = sibIdx > myIdx && myMid > sibMid + 2;
      const goingUp = sibIdx < myIdx && myMid < sibMid - 2;
      if (goingDown || goingUp) {
        // moveRoutine fait splice(to, 0, splice(from,1)[0]) : la cible est l'index du voisin
        // APRÈS retrait de la carte (vers le bas ET vers le haut → sibIdx).
        S.moveRoutine(myIdx, sibIdx);
        dragging = false;
        card.classList.remove('dragging');
        card.style.transform = '';
        rerender();
        break;
      }
    }
  });
  const endDrag = () => {
    clearTimeout(pressTimer);
    if (dragging) { dragging = false; card.classList.remove('dragging'); card.style.transform = ''; }
  };
  card.addEventListener('pointerup', endDrag);
  card.addEventListener('pointercancel', endDrag);
  return card;
}

function cardMenu(anchor, r, rerender) {
  ctxMenu(anchor, [
    { label: L10n.s('View routine', 'Voir la routine'), icon: 'chevron_right', onClick: () => nav.nav.push('routineDetail', { id: r.id }) },
    { label: L10n.s('Rename routine', 'Renommer la routine'), icon: 'edit', onClick: () => promptDialog({
      title: L10n.s('Rename routine', 'Renommer la routine'), value: r.name, maxLength: 40,
      confirmLabel: L10n.s('Save', 'Enregistrer'),
      onConfirm: name => { S.renameRoutine(r.id, name); rerender(); },
    }) },
    { label: L10n.s('Duplicate routine', 'Dupliquer la routine'), icon: 'content_copy', onClick: () => { S.duplicateRoutine(r.id); rerender(); } },
    { label: L10n.s('Delete routine', 'Supprimer la routine'), icon: 'delete', red: true, onClick: () => confirmDialog({
      title: L10n.s('Delete routine?', 'Supprimer la routine ?'),
      message: L10n.s('This routine will be removed from your list.', 'Cette routine sera retirée de ta liste.'),
      confirmLabel: L10n.s('Delete', 'Supprimer'), destructive: true,
      onConfirm: () => { S.deleteRoutine(r.id); rerender(); },
    }) },
  ]);
}

/* ---------------- calendrier d'entraînement (TrainingCalendar.kt) ---------------- */

function calendarView() {
  const now = new Date();
  let vy = now.getFullYear(), vm = now.getMonth() + 1;
  const wrapEl = h('div', { class: 'cal-wrap' });
  paint();
  return wrapEl;

  function paint() {
    const byDay = {};
    for (const w of store.workouts) (byDay[Calc.dayKey(w.startedAt)] ||= []).push(w);
    const cap = s => s[0].toUpperCase() + s.slice(1);
    const monthDays = new Date(vy, vm, 0).getDate();
    const lead = (new Date(vy, vm - 1, 1).getDay() + 6) % 7;
    let vol = 0, count = 0;
    const grid = h('div', { class: 'cal-grid' });
    for (let i = 0; i < lead; i++) grid.append(h('div', { class: 'cal-cell' }));
    for (let d = 1; d <= monthDays; d++) {
      const key = `${String(vy).padStart(4, '0')}-${String(vm).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
      const ws = byDay[key] || [];
      const isToday = now.getFullYear() === vy && now.getMonth() + 1 === vm && now.getDate() === d;
      count += ws.length;
      vol += ws.reduce((a, w) => a + Calc.vol(w), 0);
      grid.append(h('div', {
        class: 'cal-cell' + (ws.length ? ' has' : '') + (isToday ? ' today' : ''),
        style: ws.length || isToday ? { fontWeight: 700 } : null,
        onclick: ws.length ? () => {
          const w = ws.reduce((a, b) => a.startedAt > b.startedAt ? a : b);
          nav.nav.push('workoutDetail', { id: w.id });
        } : null,
      }, String(d)));
    }
    wrapEl.replaceChildren(
      h('div', { style: { display: 'flex', alignItems: 'center' } },
        h('button', { class: 'iconbtn', onclick: () => { vm--; if (vm < 1) { vm = 12; vy--; } paint(); } }, icon('chevron_left')),
        h('div', { class: 'cal-title' }, cap(Calc.monthLabel(new Date(vy, vm - 1, 1).getTime()))),
        (() => {
          const future = vy > now.getFullYear() || (vy === now.getFullYear() && vm > now.getMonth() + 1);
          return h('button', { class: 'iconbtn', onclick: future ? null : () => { vm++; if (vm > 12) { vm = 1; vy++; } paint(); }, style: future ? { opacity: .3 } : null }, icon('chevron_right'));
        })(),
      ),
      h('div', { class: 'cal-grid', style: { paddingTop: 0 } }, ['L', 'M', 'M', 'J', 'V', 'S', 'D'].map(d => h('div', { class: 't-xs', style: { textAlign: 'center', fontWeight: 700 } }, d))),
      grid,
      h('div', { class: 'cal-sum' }, count
        ? `${count} ${L10n.s('workouts', 'séances')} · ${Calc.fmtVol(vol, store.settings.unit)} ${Calc.unitLabel(store.settings.unit)}`
        : L10n.s('No workouts this month.', 'Aucune séance ce mois-ci.')),
    );
  }
}

/* ---------------- détail de routine ---------------- */

function renderRoutineDetail(el, { id }) {
  css();
  el.classList.add('scr-rdetail');
  const r = S.routineById(id);
  if (!r) { nav.nav.pop(); return; }
  let metric = 0;
  const unit = store.settings.unit;

  function guard(block) {
    if (store.draft) {
      workoutInProgressDialog({
        onResume: () => nav.nav.push('logger'),
        onRestart: () => { restClear(); S.discardDraft(); block(); },
      });
    } else block();
  }

  el.append(h('div', { class: 'topbar' },
    h('button', { class: 'iconbtn', onclick: () => nav.nav.pop() }, icon('arrow_back')),
    h('div', { class: 'topbar-title t-center' }, L10n.s('Routine', 'Routine', 'Rutina', 'Routine')),
    h('button', { class: 'iconbtn', onclick: () => shareText(shareRoutineText(r)) }, icon('ios_share')),
    h('button', { class: 'iconbtn', onclick: e => menu(e.currentTarget) }, icon('more_horiz')),
  ));

  const list = h('div', { class: 'screen screen-pad', style: { paddingTop: '8px' } });
  list.append(
    h('div', { style: { fontSize: '26px', fontWeight: 800, padding: '8px 0 2px' } }, r.name),
    h('div', { class: 't-sm', style: { marginBottom: '12px' } },
      L10n.s('Created by %1$s', 'Créée par %1$s').replace('%1$s', '@' + store.settings.handle)),
    primaryButton(L10n.s('Start routine', 'Commencer la Routine', 'Comenzar la rutina', 'Routine starten'), {
      onClick: () => guard(() => { S.startWorkout(r.id); nav.nav.push('logger'); }),
    }),
  );

  // graphique des séances liées (≤ 90 j, ≤ 12 points, série ≤ 10)
  const sessions = S.workoutsDesc().filter(w =>
    w.startedAt >= Date.now() - 90 * 86400000 &&
    w.exercises.some(e => r.exercises.some(re => re.name === e.name)),
  ).sort((a, b) => a.startedAt - b.startedAt).slice(-12);

  if (sessions.length) {
    const chartBox = h('div');
    const totalEl = h('div', { class: 'mrow' });
    list.append(totalEl, chartBox, h('div', { class: 'mchips' }));
    const paintChart = () => {
      const series = sessions.map(w => ({
        label: Calc.fmtDateShort(w.startedAt),
        value: metric === 0 ? Calc.vol(w) : metric === 1 ? Calc.reps(w) : (w.endedAt - w.startedAt) / 60000,
      })).slice(-10);
      const volTargets = r.exercises.reduce((a, e) => a + e.sets.reduce((b, s) => b + (s.kg || 0) * (s.reps || 0), 0), 0);
      const total = metric === 0
        ? `${Calc.fmtVol(volTargets, unit)} ${Calc.unitLabel(unit)}`
        : metric === 1
          ? `${r.exercises.reduce((a, e) => a + e.sets.reduce((b, s) => b + (s.reps || 0), 0), 0)} ${L10n.s('reps', 'réps')}`
          : '—';
      totalEl.replaceChildren(
        h('div', { class: 'total' }, total),
        sessions.length ? h('div', { class: 't-acc', style: { fontSize: 14, fontWeight: 600 } }, Calc.fmtDateShort(sessions[sessions.length - 1].startedAt)) : null,
        h('div', { style: { flex: 1 } }),
        h('div', { class: 't-acc', style: { fontSize: 14, fontWeight: 600 } }, L10n.s('3 months', '3 derniers mois')),
      );
      chartBox.replaceChildren(lineChart(series, v => metric === 2 ? `${Math.round(v)}m` : Calc.fmtVol(v, unit)));
      const chips = list.querySelector('.mchips');
      chips.replaceChildren();
      [L10n.s('Volume', 'Volume'), L10n.s('Reps', 'Réps'), L10n.s('Duration', 'Durée')].forEach((label, i) => {
        chips.append(h('button', { class: 'mchip' + (metric === i ? ' sel' : ''), onclick: () => { metric = i; paintChart(); } }, label));
      });
    };
    paintChart();
  }

  list.append(h('div', { style: { display: 'flex', alignItems: 'center', padding: '10px 0' } },
    h('div', { style: { flex: 1, fontSize: 18, color: 'var(--mut)' } }, L10n.s('Exercises', 'Exercices', 'Ejercicios', 'Übungen')),
    h('button', { class: 'link', onclick: () => guard(() => { S.startRoutine(r.id); nav.nav.push('logger'); }) },
      L10n.s('Edit Routine', 'Modifier la Routine')),
  ));

  for (const ex of r.exercises) {
    const cardio = ex.muscle === 'Cardio';
    const tbl = h('div', { class: 'tbl' },
      h('div', { class: 'th' }, L10n.s('SET', 'SÉRIE')),
      h('div', { class: 'th' }, unit.toUpperCase()),
      h('div', { class: 'th' }, L10n.s('REPS', 'RÉPS')));
    ex.sets.forEach((s, si) => {
      const bg = si % 2 === 1 ? 'var(--card)' : 'transparent';
      tbl.append(
        h('div', { class: 'td', style: { background: bg } }, String(si + 1)),
        h('div', { class: 'td', style: { background: bg } }, s.kg != null ? Calc.fmtKg(s.kg, unit) : '—'),
        h('div', { class: 'td', style: { background: bg } }, String(s.reps ?? '—')));
    });
    const effective = ex.restSec ?? store.settings.restSec;
    const m = Math.floor(effective / 60), sec = effective % 60;
    list.append(h('div', { class: 'exb' },
      h('div', { style: { display: 'flex', alignItems: 'center', padding: '0 16px', gap: '14px' } },
        h('div', { style: { width: '42px', height: '42px', borderRadius: '50%', background: 'var(--card2)', display: 'flex', alignItems: 'center', justifyContent: 'center' } }, exCircle(ex.muscle || 'Chest', 34)),
        h('div', { class: 'nm', onclick: () => nav.nav.push('exerciseDetail', { name: ex.name }) }, exName(ex.name))),
      h('div', { class: 'restl' }, icon('timer', { size: 17 }),
        L10n.s('Rest Timer: %1$s', 'Minuteur de Repos: %1$s')
          .replace('%1$s', `${m > 0 ? m + 'min ' : ''}${sec}s`)),
      tbl,
    ));
  }

  el.append(list);

  function menu(anchor) {
    ctxMenu(anchor, [
      { label: L10n.s('Edit routine', 'Modifier la routine'), icon: 'edit', onClick: () => guard(() => { S.startRoutine(r.id); nav.nav.push('logger'); }) },
      { label: L10n.s('Rename routine', 'Renommer la routine'), icon: 'drive_file_rename_outline', onClick: () => promptDialog({
        title: L10n.s('Rename routine', 'Renommer la routine'), value: r.name, maxLength: 40,
        confirmLabel: L10n.s('Save', 'Enregistrer'),
        onConfirm: name => { S.renameRoutine(r.id, name); nav.nav.rerender(); },
      }) },
      { label: L10n.s('Duplicate routine', 'Dupliquer la routine'), icon: 'content_copy', onClick: () => { S.duplicateRoutine(r.id); toast(L10n.s('Routine duplicated', 'Routine dupliquée')); } },
      { label: L10n.s('Delete routine', 'Supprimer la routine'), icon: 'delete', red: true, onClick: () => confirmDialog({
        title: L10n.s('Delete routine?', 'Supprimer la routine ?'),
        message: L10n.s('This routine will be removed from your list.', 'Cette routine sera retirée de ta liste.'),
        confirmLabel: L10n.s('Delete', 'Supprimer'), destructive: true,
        onConfirm: () => { S.deleteRoutine(r.id); nav.nav.pop(); },
      }) },
    ]);
  }
}

function shareRoutineText(r) {
  const uLabel = Calc.unitLabel(store.settings.unit);
  const lines = [r.name, ''];
  for (const ex of r.exercises) {
    lines.push(`${exName(ex.name)} (${ex.sets.length} ${L10n.s('series', 'séries')})`);
    ex.sets.forEach((st, i) => {
      const kgLabel = st.kg != null ? `${Calc.fmtKg(st.kg, store.settings.unit)} ${uLabel}` : '';
      lines.push(`  ${i + 1}. ${kgLabel} × ${st.reps ?? '—'}`);
    });
  }
  return lines.join('\n');
}

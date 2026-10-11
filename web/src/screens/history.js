// Écran Historique + détail de séance — port de ui/History.kt.
import { h, icon, toast, confirmDialog, ctxMenu } from '../ui.js';
import { store, workoutsDesc, photoForWorkout, photoUrl } from '../store.js';
import * as S from '../store.js';
import * as nav from '../router.js';
import * as Calc from '../calc.js';
import { L10n, exName, matches } from '../search.js';
import { shareText } from './home.js';

nav.registerScreen('history', { render });
nav.registerScreen('workoutDetail', { render: renderDetail });

function css() {
  if (!document.getElementById('css-history')) {
    const st = h('style', { id: 'css-history' });
    st.textContent = `
.scr-history .filters { display:flex; gap:8px; padding:6px 16px; }
.scr-history .grp { display:flex; align-items:center; gap:2px; padding:14px 16px 8px; color:var(--mut); font-size:15px; font-weight:500; }
.scr-history .hrow { display:flex; align-items:center; gap:12px; margin:5px 16px; background:var(--card); border:1px solid var(--line); border-radius:14px; padding:11px; }
.scr-history .hdate { width:46px; border-radius:10px; background:var(--card2); text-align:center; padding:6px 0; }
.scr-history .hdate .dw { font-size:9.5px; font-weight:800; color:var(--mut); letter-spacing:.5px; }
.scr-history .hdate .dn { font-size:17px; font-weight:800; }
.scr-history .hname { font-size:16px; font-weight:700; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.scr-history .hwhen { font-size:13px; color:var(--mut); }
.scr-history .hvol { font-size:15px; font-weight:600; white-space:nowrap; }
.scr-history .cal { margin:4px 16px 12px; }
.scr-history .cal-head { display:flex; align-items:center; padding:0 6px; }
.scr-history .cal-title { flex:1; text-align:center; font-weight:800; font-size:14.5px; }
.scr-history .cal-dow { display:flex; padding:0 10px; color:var(--mut); font-size:9.5px; font-weight:800; }
.scr-history .cal-dow span { flex:1; text-align:center; }
.scr-history .cal-grid { display:grid; grid-template-columns:repeat(7,1fr); padding:0 10px; }
.scr-history .cal-cell { aspect-ratio:1; margin:1px; border-radius:10px; display:flex; align-items:center; justify-content:center; font-size:12.5px; font-weight:600; position:relative; }
.scr-history .cal-cell.has { background:var(--card2); cursor:pointer; }
.scr-history .cal-cell.today { border:1.5px solid var(--accent); }
.scr-history .cal-cell .dot { position:absolute; bottom:3px; left:50%; transform:translateX(-50%); width:5px; height:5px; border-radius:50%; background:var(--accent); }
.scr-wdetail .date-line { font-size:13px; color:var(--mut); padding:2px 16px; }
.scr-wdetail .stats4 { display:flex; gap:16px; padding:8px 16px; }
.scr-wdetail .stats4 .c { flex:1; }
.scr-wdetail .stats4 .c.wide { flex:1.4; }
.scr-wdetail .stats4 .l { font-size:12px; color:var(--mut); }
.scr-wdetail .stats4 .v { font-size:14px; font-weight:600; margin-top:1px; }
.scr-wdetail .pr-row { display:flex; justify-content:space-between; gap:8px; padding:3px 0; }
.scr-wdetail .pr-row .n { font-weight:700; font-size:13px; }
.scr-wdetail .pr-row .d { color:var(--mut); font-size:12.5px; white-space:nowrap; }
.scr-wdetail .excard .head { display:flex; align-items:center; }
.scr-wdetail .excard .head .nm { flex:1; }
.scr-wdetail .excard .head .nm .n1 { font-weight:700; font-size:16px; }
.scr-wdetail .excard .head .nm .n2 { color:var(--mut); font-size:13px; }
.scr-wdetail .tbl { display:grid; margin-top:6px; }
.scr-wdetail .tbl .th { color:var(--mut); font-size:11px; }
.scr-wdetail .tbl .td { font-size:13px; font-weight:600; padding:9px 0; }
.scr-wdetail .tbl .mut { color:var(--mut); font-weight:400; }
.scr-wdetail .tbl .zebra:nth-child(even) { background:rgba(42,42,45,.35); }
.scr-wdetail .chip-pr { color:#fff; font-size:9px; font-weight:800; background:var(--accent); border-radius:5px; padding:3px 6px; margin-left:6px; white-space:nowrap; }
.scr-wdetail .exnotes { display:flex; gap:6px; align-items:center; color:var(--mut); font-size:12.5px; margin-top:6px; }
.scr-wdetail .photo-lbl { font-size:12px; font-weight:800; color:var(--mut); }
`;
    document.head.append(st);
  }
}

function render(el) {
  css();
  el.classList.add('scr-history');
  let query = '', filter = 0, showCalendar = false;
  const nowD = new Date();
  let viewYear = nowD.getFullYear(), viewMonth = nowD.getMonth() + 1;
  const listWrap = h('div');

  const wrap = h('div', { class: 'screen' });
  el.append(wrap);

  function body() {
    wrap.replaceChildren(
      h('div', { class: 'hdr', style: { display: 'flex', alignItems: 'center', padding: '20px 8px 4px 16px' } },
        h('div', { class: 't-title', style: { flex: '1' } }, L10n.s('History', 'Historique')),
        h('button', { class: 'iconbtn', onclick: () => { showCalendar = !showCalendar; listBody(); } }, icon('calendar_month')),
      ),
      searchBar(),
      filters(),
      listWrap,
    );
    listBody();
  }

  function searchBar() {
    const input = h('input', {
      placeholder: L10n.s('Search workouts', 'Rechercher des séances'),
      value: query, autocapitalize: 'off', autocomplete: 'off',
      style: { flex: '1', background: 'none', border: 'none', outline: 'none', fontSize: '14px', color: 'var(--text)' },
    });
    input.addEventListener('input', () => { query = input.value; listBody(); });
    return h('div', { class: 'searchbar' }, icon('search', { size: 17, cls: 't-mut' }), input);
  }

  function filters() {
    const labels = [L10n.s('All', 'Toutes'), L10n.s('This week', 'Cette semaine'), L10n.s('This month', 'Ce mois'), L10n.s('This year', 'Cette année')];
    const row = h('div', { class: 'filters' });
    labels.forEach((label, i) => row.append(h('button', { class: 'chip' + (filter === i ? ' sel' : ''), onclick: () => { filter = i; listBody(); } }, label)));
    return row;
  }

  function listBody() {
    listWrap.replaceChildren();
    const byDay = {};
    for (const w of store.workouts) (byDay[Calc.dayKey(w.startedAt)] ||= []).push(w);

    if (showCalendar) listWrap.append(calendarCard(byDay));

    const cutoff = filter === 1 ? Calc.weekStart(Date.now())
      : filter === 2 ? Date.now() - 30 * 86400000
      : filter === 3 ? Date.now() - 365 * 86400000 : 0;
    const filtered = workoutsDesc().filter(w => w.startedAt >= cutoff && (!query ||
      matches(query, w.name) || w.exercises.some(e => matches(query, e.name))));

    const groups = buildGroups(filtered);
    if (!groups.length) {
      listWrap.append(h('div', { class: 'empty' }, 'Aucune séance enregistrée.'));
      return;
    }
    for (const [label, ws] of groups) {
      listWrap.append(h('div', { class: 'grp' }, label, icon('expand_more', { size: 18 })));
      for (const w of ws) listWrap.append(historyRow(w));
    }
    listWrap.append(h('div', { style: { height: '16px' } }));
  }

  function calendarCard(byDay) {
    const cap = s => s[0].toUpperCase() + s.slice(1);
    const title = cap(Calc.monthLabel(new Date(viewYear, viewMonth - 1, 1).getTime()));
    const grid = h('div', { class: 'cal-grid' });
    const first = new Date(viewYear, viewMonth - 1, 1);
    const lead = (first.getDay() + 6) % 7;
    const days = new Date(viewYear, viewMonth, 0).getDate();
    const today = new Date();
    for (let i = 0; i < lead; i++) grid.append(h('div', { class: 'cal-cell' }));
    for (let d = 1; d <= days; d++) {
      const key = `${String(viewYear).padStart(4, '0')}-${String(viewMonth).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
      const has = !!(byDay[key] && byDay[key].length);
      const isToday = today.getFullYear() === viewYear && today.getMonth() + 1 === viewMonth && today.getDate() === d;
      const cell = h('div', {
        class: 'cal-cell' + (has ? ' has' : '') + (isToday ? ' today' : ''),
        onclick: has ? () => {
          const w = byDay[key].reduce((a, b) => a.startedAt > b.startedAt ? a : b);
          nav.nav.push('workoutDetail', { id: w.id });
        } : null,
      }, String(d));
      if (has) cell.append(h('div', { class: 'dot' }));
      grid.append(cell);
    }
    return h('div', { class: 'card cal' },
      h('div', { class: 'cal-head' },
        h('button', { class: 'iconbtn', onclick: () => { viewMonth--; if (viewMonth < 1) { viewMonth = 12; viewYear--; } body(); } }, icon('chevron_left')),
        h('div', { class: 'cal-title' }, title),
        h('button', { class: 'iconbtn', onclick: () => { viewMonth++; if (viewMonth > 12) { viewMonth = 1; viewYear++; } body(); } }, icon('chevron_right')),
      ),
      h('div', { class: 'cal-dow' }, ['Lu', 'Ma', 'Me', 'Je', 'Ve', 'Sa', 'Di'].map(d => h('span', {}, d))),
      grid,
    );
  }

  body();
}

function buildGroups(desc) {
  const ws = Calc.weekStart(Date.now());
  const groups = [];
  for (const w of desc) {
    const label = w.startedAt >= ws ? L10n.s('This week', 'Cette semaine')
      : w.startedAt >= ws - 7 * 86400000 ? L10n.s('Last week', 'Semaine dernière')
      : Calc.monthLabel(w.startedAt);
    let g = groups.find(x => x[0] === label);
    if (!g) { g = [label, []]; groups.push(g); }
    g[1].push(w);
  }
  return groups;
}

function historyRow(w) {
  const d = Calc.localDate(w.startedAt);
  const dw = ['L', 'M', 'M', 'J', 'V', 'S', 'D'][(d.getDay() + 6) % 7];
  return h('button', { class: 'hrow', onclick: () => nav.nav.push('workoutDetail', { id: w.id }) },
    h('div', { class: 'hdate' }, h('div', { class: 'dw' }, dw), h('div', { class: 'dn' }, String(d.getDate()))),
    h('div', { style: { flex: '1', minWidth: '0', textAlign: 'left' } },
      h('div', { class: 'hname' }, w.name),
      h('div', { class: 'hwhen' }, `${Calc.fmtTime(w.startedAt)} — ${Calc.fmtTime(w.endedAt)}`)),
    h('div', { class: 'hvol' }, `${Calc.fmtVol(Calc.vol(w), store.settings.unit)} ${Calc.unitLabel(store.settings.unit)}`),
  );
}

/* ---------------- détail de séance ---------------- */

function renderDetail(el, { id }) {
  css();
  el.classList.add('scr-wdetail');
  const w = S.workoutById(id);
  if (!w) { nav.nav.pop(); return; }
  const unit = store.settings.unit, uLabel = Calc.unitLabel(unit);
  const linked = photoForWorkout(id);

  el.append(h('div', { class: 'topbar' },
    h('button', { class: 'iconbtn', onclick: () => nav.nav.pop() }, icon('arrow_back')),
    h('div', { class: 'topbar-title', style: { overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', fontWeight: '700' } }, w.name),
    h('button', { class: 'iconbtn', onclick: e => detailMenu(e.currentTarget, w) }, icon('more_horiz')),
  ));

  const list = h('div', { class: 'screen-pad', style: { paddingTop: '6px' } });
  list.append(h('div', { class: 'date-line' },
    `${Calc.fmtDateFull(w.startedAt)}, ${Calc.fmtTime(w.startedAt)} — ${Calc.fmtTime(w.endedAt)}`));

  if (linked) {
    const holder = h('div', { style: { margin: '8px 0' } },
      h('div', { class: 'photo-lbl', style: { marginBottom: '6px' } }, L10n.s('Progress photo', 'Photo de progression')),
      h('div', { style: { aspectRatio: '1', borderRadius: '12px', background: 'var(--card2)', overflow: 'hidden' } }));
    photoUrl(linked.id).then(u => {
      if (u) holder.lastChild.append(h('img', { src: u, alt: '', style: { width: '100%', height: '100%', objectFit: 'cover', display: 'block' } }));
    });
    holder.lastChild.addEventListener('click', () => nav.nav.push('photoViewer', { id: linked.id }));
    list.append(holder);
  }

  list.append(h('div', { class: 'stats4' },
    stat(L10n.s('Time', 'Temps'), Calc.fmtDur(w.endedAt - w.startedAt)),
    stat(L10n.s('Volume', 'Volume'), `${Calc.fmtVol(Calc.vol(w), unit)} kg`, true),
    stat(L10n.s('Records', 'Records'), `🏅 ${(w.prs || []).length}`),
    stat(L10n.s('Sets', 'Séries'), String(Calc.setsDone(w))),
  ));

  if ((w.prs || []).length) {
    list.append(h('div', { class: 'card', style: { display: 'flex', gap: '10px' } },
      icon('emoji_events', { size: 18, cls: 't-acc' }),
      h('div', { style: { flex: '1' } },
        w.prs.map(p => h('div', { class: 'pr-row' },
          h('div', { class: 'n' }, exName(p.ex)),
          h('div', { class: 'd' }, `${Calc.fmtKg(p.value, unit)} ${uLabel} · ${p.kind}`)))),
    ));
  }

  if (w.notes && w.notes.trim()) {
    list.append(h('div', { class: 'card', style: { display: 'flex', gap: '10px' } },
      icon('notes', { size: 16, cls: 't-mut' }), h('div', { style: { fontSize: '14px', lineHeight: '20px' } }, w.notes)));
  }

  for (const ex of w.exercises) {
    const prevSets = S.prevSetsBefore(w.startedAt, ex.name);
    const cardio = ex.muscle === 'Cardio';
    const cols = cardio ? '34px 1.1fr 1fr 1fr auto' : '34px 1.1fr 1fr 1fr auto';
    const tbl = h('div', { class: 'tbl', style: { gridTemplateColumns: cols } });
    tbl.append(h('div', { class: 'th' }, 'SÉRIE'), h('div', { class: 'th' }, L10n.s('PREVIOUS', 'PRÉCÉDENTE')));
    if (cardio) tbl.append(h('div', { class: 'th' }, 'MIN'), h('div', { class: 'th' }, 'KM'));
    else tbl.append(h('div', { class: 'th' }, 'KG'), h('div', { class: 'th' }, 'RÉPS'));
    tbl.append(h('div'));
    ex.sets.forEach((s, i) => {
      tbl.append(h('div', { class: 'td mut zebra' }, String(i + 1)));
      tbl.append(h('div', { class: 'td mut zebra' }, (prevSets && prevSets[i]) || '—'));
      if (cardio) {
        tbl.append(h('div', { class: 'td zebra' }, s.mins != null ? String(s.mins) : '—'));
        tbl.append(h('div', { class: 'td zebra' }, s.km != null ? Calc.trimNum(s.km) : '—'));
      } else {
        tbl.append(h('div', { class: 'td zebra' }, s.kg != null ? Calc.fmtKg(s.kg, unit) : '—'));
        tbl.append(h('div', { class: 'td zebra', style: { display: 'flex', alignItems: 'center' } }, String(s.reps ?? '—'),
          (s.prW || s.prE) ? h('span', { class: 'chip-pr' }, s.prW ? 'WEIGHT PR' : '1RM PR') : null));
      }
      if (!cardio && (s.prW || s.prE)) tbl.append(h('div'));
      else if (cardio) tbl.append(h('div'));
    });
    list.append(h('div', { class: 'card excard' },
      h('div', { class: 'head', onclick: () => nav.nav.push('exerciseDetail', { name: ex.name }), style: { cursor: 'pointer' } },
        h('div', { class: 'nm' },
          h('div', { class: 'n1' }, exName(ex.name)),
          h('div', { class: 'n2' }, `${ex.sets.length} ${ex.sets.length > 1 ? L10n.s('series', 'séries') : L10n.s('series', 'série')}`)),
        icon('chevron_right', { size: 20, cls: 't-mut' })),
      tbl,
      ex.notes ? h('div', { class: 'exnotes' }, icon('notes', { size: 14, cls: 't-mut' }), ex.notes) : null,
    ));
  }

  el.append(h('div', { class: 'screen' }, list));
}

function stat(l, v, wide = false) {
  return h('div', { class: 'c' + (wide ? ' wide' : '') }, h('div', { class: 'l' }, l), h('div', { class: 'v' }, v));
}

function detailMenu(anchor, w) {
  ctxMenu(anchor, [
    {
      label: L10n.s('Create routine from workout', 'Créer une routine depuis cette séance'), icon: 'create_new_folder',
      onClick: () => { S.routineFromWorkout(w.id, w.name); toast(L10n.s('Routine created', 'Routine créée')); },
    },
    {
      label: L10n.s('Repeat this workout', 'Refaire cette séance'), icon: 'refresh',
      onClick: () => {
        if (store.draft) { toast(L10n.s('Finish the current workout first', "Termine d'abord la séance en cours")); return; }
        S.startRepeat(w.id);
        nav.nav.toTab(1);
        nav.nav.push('logger');
      },
    },
    {
      label: L10n.s('Share workout', 'Partager la séance'), icon: 'ios_share',
      onClick: () => shareText(shareWorkoutTextOf(w)),
    },
    {
      label: L10n.s('Delete workout', 'Supprimer la séance'), icon: 'delete', red: true,
      onClick: () => confirmDialog({
        title: L10n.s('Delete workout?', 'Supprimer la séance ?'),
        message: L10n.s('This workout and its records will be permanently deleted.', 'Cette séance et ses records seront définitivement supprimés.'),
        confirmLabel: L10n.s('Delete', 'Supprimer'), destructive: true,
        onConfirm: () => { S.deleteWorkout(w.id); store.deletedUndo = w; nav.nav.pop(); },
      }),
    },
  ]);
}

export function shareWorkoutTextOf(w) {
  const unit = store.settings.unit, uLabel = Calc.unitLabel(unit);
  const lines = [w.name, Calc.fmtDateFull(w.startedAt),
    `${L10n.s('Time', 'Temps')} ${Calc.fmtDur(w.endedAt - w.startedAt)} · ${L10n.s('Volume', 'Volume')} ${Calc.fmtVol(Calc.vol(w), unit)} ${uLabel} · ${L10n.s('Records', 'Records')} ${(w.prs || []).length}`,
    ''];
  for (const ex of w.exercises) {
    lines.push(`${exName(ex.name)} (${ex.sets.length} ${L10n.s('series', 'séries')})`);
    for (const s of ex.sets) {
      const line = ex.muscle === 'Cardio' ? Calc.fmtCardioSet(s.mins, s.km)
        : (s.kg != null ? `${Calc.fmtKg(s.kg, unit)} ${uLabel} × ` : '× ') + (s.reps ?? '—');
      lines.push('  ' + line);
    }
  }
  return lines.join('\n');
}

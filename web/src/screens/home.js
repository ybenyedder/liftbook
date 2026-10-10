// Écran Accueil — port de ui/Home.kt (HomeScreen, FeedPost, PostUi).
import { h, icon, toast, avatarEl, sub } from '../ui.js';
import { store, workoutsDesc } from '../store.js';
import * as S from '../store.js';
import * as nav from '../router.js';
import * as Calc from '../calc.js';
import { L10n, exName } from '../search.js';

nav.registerScreen('homeTab', { render });

function render(el) {
  el.classList.add('scr-home');
  if (!document.getElementById('css-home')) {
    const st = h('style', { id: 'css-home' });
    st.textContent = `
.scr-home .hdr { display:flex; align-items:center; gap:4px; padding:14px 16px 10px; }
.scr-home .hdr .t-display { flex:1; }
.scr-home .post { background:var(--card); border:1px solid var(--line); border-radius:14px; margin:0 16px 10px; padding:14px 16px; }
.scr-home .post-head { display:flex; align-items:center; gap:10px; margin-bottom:10px; }
.scr-home .post-author { font-size:16px; font-weight:700; }
.scr-home .post-when { font-size:12px; color:var(--mut); }
.scr-home .post-title { font-size:14.5px; font-weight:600; margin-bottom:8px; }
.scr-home .post-stats { display:flex; gap:8px; margin-bottom:10px; }
.scr-home .pstat { flex:1; background:var(--card2); border-radius:10px; padding:8px 10px; }
.scr-home .pstat .v { font-size:15px; font-weight:700; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.scr-home .pstat .l { font-size:10px; font-weight:600; color:var(--mut); margin-top:1px; }
.scr-home .exline { display:flex; align-items:center; gap:10px; padding:4px 0; }
.scr-home .exn { min-width:26px; height:22px; border-radius:6px; background:var(--card2); color:var(--mut); font-size:11px; font-weight:700; display:flex; align-items:center; justify-content:center; }
.scr-home .exname-l { font-size:13.5px; color:var(--text); }
.scr-home .more-btn { font-size:13px; color:var(--accent); font-weight:600; padding:6px 0 2px; }
.scr-home .post-actions { display:flex; gap:2px; margin-top:8px; padding-top:8px; border-top:.5px solid var(--line); }
`;
    document.head.append(st);
  }

  el.append(h('div', { class: 'hdr' },
    h('div', { class: 't-display' }, L10n.s('Home', 'Accueil', 'Inicio', 'Startseite')),
    h('button', { class: 'iconbtn', onclick: () => nav.nav.push('exercises') }, icon('search')),
    h('button', { class: 'iconbtn', onclick: () => toast(L10n.s('No new notifications', 'Pas de nouvelles notifications')) }, icon('notifications')),
  ));

  const list = h('div', { class: 'screen-pad' });
  const posts = workoutsDesc().slice(0, 30);
  if (!posts.length) {
    list.append(h('div', { class: 'empty' },
      L10n.s('No workouts yet.\nGo to Training to start one!', 'Aucune séance pour le moment.\nVa dans Entraînement pour démarrer !')));
  } else {
    for (const w of posts) list.append(feedPost(w));
  }
  el.append(h('div', { class: 'screen' }, list));
}

function feedPost(w) {
  const prs = (w.prs || []).length;
  let expanded = false;
  const shown = () => expanded ? w.exercises : w.exercises.slice(0, 3);
  const hidden = () => Math.max(0, w.exercises.length - 3);

  const exWrap = h('div');
  const renderExs = () => {
    exWrap.replaceChildren();
    for (const ex of shown()) {
      exWrap.append(h('div', { class: 'exline' },
        h('div', { class: 'exn' }, String(ex.sets.length)),
        h('div', { class: 'exname-l' }, exName(ex.name))));
    }
    if (!expanded && hidden() > 0) {
      exWrap.append(h('button', { class: 'more-btn', onclick: e => { e.stopPropagation(); expanded = true; renderExs(); } },
        L10n.seeMoreExercises(hidden())));
    }
  };
  renderExs();

  const stats = h('div', { class: 'post-stats' },
    pstat(Calc.fmtDur(w.endedAt - w.startedAt), L10n.s('Time', 'Temps')),
    pstat(Calc.fmtVol(Calc.vol(w), store.settings.unit), L10n.s('Volume', 'Volume')),
    pstat(String(prs), '🏅 ' + L10n.s('Records', 'Records')));

  const card = h('div', { class: 'post', onclick: () => nav.nav.push('workoutDetail', { id: w.id }) },
    h('div', { class: 'post-head' },
      avatarEl(store.settings.profileName, 44),
      h('div', { style: { flex: '1' } },
        h('div', { class: 'post-author' }, store.settings.profileName),
        h('div', { class: 'post-when' }, L10n.relativeTime(w.startedAt)))),
    h('div', { class: 'post-title' }, Calc.dayName(w.startedAt)),
    stats,
    exWrap,
    h('div', { class: 'post-actions', onclick: e => e.stopPropagation() },
      h('button', { class: 'iconbtn', onclick: () => toast(L10n.s('Added to favorites', 'Ajouté aux favoris')) }, icon('thumb_up', { size: 20 })),
      h('button', { class: 'iconbtn', onclick: () => toast(L10n.s('Comments coming soon', 'Les commentaires arrivent bientôt')) }, icon('chat_bubble', { size: 20 })),
      h('button', { class: 'iconbtn', onclick: () => sharePost(w) }, icon('ios_share', { size: 20 })),
    ),
  );

  // avatar photo si dispo (object URL mis en cache côté store — plus de fuite par rendu)
  S.avatarObjectUrl().then(u => {
    if (u) {
      const av = card.querySelector('.avatar');
      if (av) av.replaceChildren(h('img', { src: u, alt: '', style: { width: '100%', height: '100%', objectFit: 'cover' } }));
    }
  });
  return card;
}

function pstat(v, l) {
  return h('div', { class: 'pstat' }, h('div', { class: 'v' }, v), h('div', { class: 'l' }, l));
}

async function sharePost(w) {
  const ok = await shareText(shareWorkoutText(w));
  toast(ok ? L10n.s('Shared!', 'Partagé !') : L10n.s('Copied!', 'Copié !'));
}

function shareWorkoutText(w) {
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
export async function shareText(text) {
  try {
    if (navigator.share) { await navigator.share({ text }); return true; }
    await navigator.clipboard.writeText(text);
    return false;
  } catch { return false; }
}

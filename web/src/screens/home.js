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
/* FeedPost (Home.kt) : pas de carte — colonne pleine largeur, padding vertical 10dp */
.scr-home .post { padding:10px 0; cursor:pointer; }
.scr-home .post + .post { border-top:8px solid var(--bg); }
.scr-home .post-head { display:flex; align-items:center; gap:12px; padding:0 16px; }
.scr-home .post-author { font-size:16px; font-weight:700; }
.scr-home .post-when { font-size:13px; color:var(--mut); margin-top:1px; }
.scr-home .post-title { font-size:21px; font-weight:800; padding:10px 16px 8px; }
/* FeedStat : label gris 13sp AU-DESSUS, valeur 15sp semibold — pas de fond */
.scr-home .post-stats { display:flex; gap:0; padding:0 16px; }
.scr-home .pstat { flex:1.2; min-width:0; }
.scr-home .pstat.last { flex:1; }
.scr-home .pstat .l { font-size:13px; color:var(--mut); }
.scr-home .pstat .v { font-size:15px; font-weight:600; margin-top:2px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.scr-home .pstat .medal { display:flex; align-items:center; gap:5px; margin-top:2px; font-size:15px; font-weight:600; }
.scr-home .post-div { border-top:.5px solid var(--line); margin:12px 16px; }
.scr-home .exline { display:flex; align-items:center; gap:12px; padding:7px 16px; }
.scr-home .exn { border-radius:8px; background:var(--card2); color:var(--text); font-size:13px; font-weight:700; padding:5px 9px; flex:0 0 auto; }
.scr-home .exname-l { font-size:16px; font-weight:500; color:var(--text); overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; }
.scr-home .more-btn { display:block; width:100%; font-size:14.5px; color:var(--mut); padding:8px 0; text-align:center; }
.scr-home .post-actions { display:flex; padding:6px 4px 0 12px; }
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
    pstat(L10n.s('Time', 'Temps'), Calc.fmtDur(w.endedAt - w.startedAt)),
    pstat(L10n.s('Volume', 'Volume'), `${Calc.fmtVol(Calc.vol(w), store.settings.unit)} ${Calc.unitLabel(store.settings.unit)}`),
    pstat(L10n.s('Records', 'Records'), null, prs));

  const card = h('div', { class: 'post', onclick: () => nav.nav.push('workoutDetail', { id: w.id }) },
    h('div', { class: 'post-head' },
      avatarEl(store.settings.profileName, 44),
      h('div', { style: { flex: '1', minWidth: '0' } },
        h('div', { class: 'post-author' }, store.settings.profileName),
        h('div', { class: 'post-when' }, L10n.relativeTime(w.startedAt))),
      h('button', { class: 'iconbtn', onclick: e => { e.stopPropagation(); nav.nav.push('workoutDetail', { id: w.id }); } }, icon('more_horiz', { size: 18 }))),
    h('div', { class: 'post-title' }, Calc.dayName(w.startedAt)),
    stats,
    h('div', { class: 'post-div' }),
    exWrap,
    h('div', { class: 'post-div' }),
    h('div', { class: 'post-actions', onclick: e => e.stopPropagation() },
      h('button', { class: 'iconbtn', onclick: () => toast(L10n.s('Added to favorites', 'Ajouté aux favoris')) }, icon('thumb_up')),
      h('button', { class: 'iconbtn', onclick: () => toast(L10n.s('Comments coming soon', 'Les commentaires arrivent bientôt')) }, icon('chat_bubble')),
      h('button', { class: 'iconbtn', onclick: () => sharePost(w) }, icon('ios_share')),
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

function pstat(l, v, medal = null) {
  // FeedStat (Home.kt) : label gris 13sp au-dessus ; valeur 15sp semibold ou « 🏅 N » pour Records.
  return h('div', { class: 'pstat' + (medal != null ? ' last' : '') },
    h('div', { class: 'l' }, l),
    medal != null
      ? h('div', { class: 'medal' }, '🏅', h('span', {}, String(medal)))
      : h('div', { class: 'v' }, v));
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

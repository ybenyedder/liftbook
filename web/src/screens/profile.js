// Écran Profil — port de ui/Profile.kt (ProfileScreen, ProfileStat, SectionTitle,
// HeatmapYear + drawHeatmap, histogramme 6 mois, carte photos, volume par muscle).
import { h, icon, sub, emptyState, avatarEl, exCircle } from '../ui.js';
import { store } from '../store.js';
import * as S from '../store.js';
import * as nav from '../router.js';
import * as Calc from '../calc.js';
import { L10n, muscleName } from '../search.js';

nav.registerScreen('profileTab', { render });

const NB2 = '\u00a0\u00a0·\u00a0\u00a0'; // «  ·  » du buildString Kotlin (espaces insécables)

function css() {
  if (document.getElementById('css-profile')) return;
  const st = h('style', { id: 'css-profile' });
  st.textContent = `
.scr-profile .pscroll { padding-bottom: calc(24px + env(safe-area-inset-bottom, 0px)); }
.scr-profile .phead { display:flex; align-items:flex-start; padding:10px 8px 0 16px; }
.scr-profile .pmain { flex:1; min-width:0; }
.scr-profile .pid { display:flex; align-items:center; }
.scr-profile .pid .who { margin-left:14px; min-width:0; }
.scr-profile .pname { font-size:23px; font-weight:700; }
.scr-profile .phandle { font-size:13px; color:var(--mut); }
.scr-profile .pstats { display:flex; gap:26px; margin-top:14px; }
.scr-profile .pstat .v { font-size:17px; font-weight:800; }
.scr-profile .pstat .l { font-size:11.5px; color:var(--mut); }
.scr-profile .psec-title { display:flex; align-items:center; padding:16px 16px 8px; }
.scr-profile .psec-title .tt { flex:1; font-size:16px; font-weight:700; }
.scr-profile .yearbtn { display:flex; align-items:center; border:none; background:none; cursor:pointer; border-radius:99px; padding:4px 8px; color:var(--accent); }
.scr-profile .yearbtn .y { font-size:14px; font-weight:600; }
.scr-profile .yearbtn .ar { font-size:12px; }
.scr-profile .hm { display:grid; grid-auto-flow:column; grid-template-rows:repeat(7,1fr); grid-auto-columns:1fr; gap:2px; height:96px; margin:0 16px; }
.scr-profile .hmc { border-radius:22%; }
.scr-profile .hmlabels { display:flex; justify-content:space-between; margin:4px 16px 0; font-size:10px; color:var(--mut); }
.scr-profile .stk { display:inline-flex; align-items:center; gap:6px; margin:12px 16px 0; background:var(--card2); border-radius:10px; padding:7px 10px; font-size:13px; font-weight:600; }
.scr-profile .stk .fl { font-size:14px; }
.scr-profile .ptot { font-size:13px; color:var(--mut); padding:6px 16px 0; }
.scr-profile .pmonth { font-size:13px; color:var(--mut); padding:10px 16px 0; }
.scr-profile .pweek { font-size:13px; line-height:19px; color:var(--mut); padding:10px 16px 0; white-space:pre-line; }
.scr-profile .mbars { display:flex; gap:10px; height:90px; margin:8px 16px; }
.scr-profile .mcol { flex:1; display:flex; flex-direction:column; align-items:center; }
.scr-profile .mbar-host { flex:1; width:100%; display:flex; flex-direction:column; justify-content:flex-end; }
.scr-profile .mbar { width:100%; border-radius:6px; background:var(--accent); }
.scr-profile .mlab { font-size:10px; color:var(--mut); margin-top:4px; }
.scr-profile .lcard { display:flex; align-items:center; padding:13px 14px; cursor:pointer; }
.scr-profile .lcard .msr { font-size:20px; color:var(--accent); }
.scr-profile .lcard .lt { flex:1; font-size:15px; font-weight:500; margin-left:12px; text-align:left; }
.scr-profile .lcard .cnt { font-size:13px; color:var(--mut); margin-right:4px; }
.scr-profile .lcard .chev { font-size:16px; color:var(--mut); }
.scr-profile .pthumbs { display:flex; gap:6px; margin-top:10px; }
.scr-profile .pthumbs .th { flex:1; aspect-ratio:1; }
.scr-profile .pm-title { font-size:16px; font-weight:700; padding:18px 16px 6px; }
.scr-profile .mrow { display:flex; align-items:center; padding:7px 16px; }
.scr-profile .mrow .mid { flex:1; min-width:0; margin-left:12px; }
.scr-profile .mrow .mnm { display:flex; }
.scr-profile .mrow .mnm .n { flex:1; font-size:13px; font-weight:500; }
.scr-profile .mrow .mnm .v { font-size:12px; color:var(--mut); }
.scr-profile .mrow .accbar-wrap { margin-top:5px; }
.scr-profile .pfoot { font-size:11.5px; color:var(--mut); text-align:center; padding:18px 16px 0; }
/* ---- grille multi-annuelle des jours d'entraînement (TrainingDaysGrid) ---- */
.scr-profile .tg { padding: 8px 0 4px; }
.scr-profile .tg-cap { color:var(--mut); font-size:13px; line-height:18px; padding:4px 16px 10px; }
.scr-profile .tg-year { padding: 6px 16px 12px; }
.scr-profile .tg-yhead { display:flex; align-items:baseline; margin-bottom:6px; }
.scr-profile .tg-y { font-size:28px; font-weight:800; letter-spacing:-.4px; flex:1; }
.scr-profile .tg-ycnt { font-size:12.5px; font-weight:600; color:var(--mut); }
.scr-profile .tg-months { display:grid; grid-template-columns:repeat(12,1fr); margin-bottom:4px; }
.scr-profile .tg-m { font-size:9.5px; font-weight:500; color:var(--mut); text-align:center; }
.scr-profile .tg-grid { display:grid; grid-auto-flow:column; grid-template-rows:repeat(7,1fr); gap:2px; height:88px; }
.scr-profile .tg-c { border-radius:22%; background:var(--card2); min-width:0; }
.scr-profile .tg-c.today { box-shadow:0 0 0 1.5px var(--accent); }
.scr-profile .tg-rest { font-size:26px; font-weight:800; text-align:center; padding:22px 16px; letter-spacing:-.3px; }
.scr-profile .tg-next { font-size:17px; font-weight:700; color:var(--accent); text-align:center; padding:6px 16px 28px; }
`;
  document.head.append(st);
}

function render(el) {
  css();
  el.classList.add('scr-profile');

  const wrap = h('div', { class: 'screen pscroll' });
  el.append(wrap);

  function build() {
    const unit = store.settings.unit, uLabel = Calc.unitLabel(unit);
    const workouts = store.workouts;
    wrap.replaceChildren();

    /* ---- en-tête : avatar + identité + stats + rouage ---- */
    const avatar = avatarEl(store.settings.profileName.trim().slice(0, 1), 52);
    S.avatarObjectUrl().then(u => {
      if (u) avatar.replaceChildren(h('img', { src: u, alt: '', style: { width: '100%', height: '100%', objectFit: 'cover' } }));
    });
    wrap.append(h('div', { class: 'phead' },
      h('div', { class: 'pmain' },
        h('div', { class: 'pid' },
          avatar,
          h('div', { class: 'who' },
            h('div', { class: 'pname' }, store.settings.profileName),
            h('div', { class: 'phandle' }, '@' + store.settings.handle))),
        h('div', { class: 'pstats' },
          pstat(String(workouts.length), L10n.s('Workouts', 'Entraînements', 'Entrenamientos', 'Trainings')),
          pstat('0', L10n.s('Followers', 'Abonnés', 'Seguidores', 'Follower')),
          pstat('0', L10n.s('Following', 'Abonnements', 'Seguidos', 'Folgt')))),
      h('button', { class: 'iconbtn', onclick: () => nav.nav.push('settings') }, icon('settings')),
    ));

    /* ---- grille multi-annuelle des jours d'entraînement (TrainingDaysGrid) ---- */
    const dayVolumes = {};
    for (const w of workouts) {
      const k = Calc.dayKey(w.startedAt);
      dayVolumes[k] = (dayVolumes[k] || 0) + Calc.vol(w);
    }
    wrap.append(trainingDaysGrid(dayVolumes));

    /* ---- pilule série (streak ≥ 2) ---- */
    const streak = Calc.streak(workouts, Date.now());
    if (streak >= 2) {
      wrap.append(h('div', { class: 'stk' },
        h('span', { class: 'fl' }, '🔥'),
        L10n.s('%1$d-day streak', 'Série de %1$d jours').replace('%1$d', String(streak))));
    }

    /* ---- totaux ---- */
    const totalVol = Calc.totalVol(workouts);
    const totalSets = workouts.reduce((a, w) => a + Calc.setsDone(w), 0);
    const totalReps = workouts.reduce((a, w) => a + Calc.reps(w), 0);
    wrap.append(h('div', { class: 'ptot' },
      `${Calc.fmtVol(totalVol, unit)} ${uLabel}${NB2}` +
      L10n.s('%1$d series', totalSets > 1 ? '%1$d séries' : '%1$d série').replace('%1$d', String(totalSets)) + NB2 +
      L10n.s('%1$d reps', '%1$d réps').replace('%1$d', String(totalReps))));

    /* ---- ce mois-ci ---- */
    const n = new Date();
    const monthStart = new Date(n.getFullYear(), n.getMonth(), 1).getTime();
    const month = workouts.filter(w => w.startedAt >= monthStart);
    if (month.length) {
      const mVol = month.reduce((a, w) => a + Calc.vol(w), 0);
      const mPrs = month.reduce((a, w) => a + (w.prs || []).length, 0);
      wrap.append(h('div', { class: 'pmonth' },
        L10n.s('This month', 'Ce mois-ci') + ` : ${month.length} ` + L10n.s('workouts', 'séances') +
        ` · ${Calc.fmtVol(mVol, unit)} ${uLabel} · ${mPrs} ` + L10n.s('PRs', 'records')));
    }

    /* ---- 7 derniers jours / 7 jours précédents (fenêtres glissantes du Kotlin) ---- */
    const now = Date.now();
    const last7 = workouts.filter(w => w.startedAt >= now - 7 * 86400000);
    const prev7 = workouts.filter(w => w.startedAt >= now - 14 * 86400000 && w.startedAt < now - 7 * 86400000);
    if (last7.length || prev7.length) {
      const v1 = last7.reduce((a, w) => a + Calc.vol(w), 0);
      const v2 = prev7.reduce((a, w) => a + Calc.vol(w), 0);
      const delta = v2 > 0 ? Math.trunc((v1 - v2) / v2 * 100) : null;
      const dPart = delta != null
        ? `\u00a0\u00a0(${delta >= 0 ? '+' : ''}${delta}% ` + L10n.s('vs previous week', 'vs semaine précédente') + ')'
        : '';
      wrap.append(h('div', { class: 'pweek' },
        L10n.s('Last 7 days', '7 derniers jours') + ` : ${Calc.fmtVol(v1, unit)} ${uLabel} · ${last7.length} ` +
        L10n.s('workouts', 'séances') + dPart + '\n' +
        L10n.s('Previous 7 days', '7 jours précédents') + ` : ${Calc.fmtVol(v2, unit)} ${uLabel} · ${prev7.length} ` +
        L10n.s('workouts', 'séances')));
    }

    /* ---- histogramme des 6 derniers mois ---- */
    const monthly = [];
    for (let back = 5; back >= 0; back--) {
      const m = new Date(n.getFullYear(), n.getMonth() - back, 1);
      const start = m.getTime();
      const end = new Date(n.getFullYear(), n.getMonth() - back + 1, 1).getTime();
      const label = Calc.monthShort(start).slice(0, 3); // label.take(3) du Kotlin
      monthly.push([label, workouts.filter(w => w.startedAt >= start && w.startedAt < end).reduce((a, w) => a + Calc.vol(w), 0)]);
    }
    const maxV = Math.max(1, ...monthly.map(x => x[1]));
    const bars = h('div', { class: 'mbars' });
    for (const [label, v] of monthly) {
      const bar = h('div', { class: 'mbar', style: { height: '0px' } });
      requestAnimationFrame(() => requestAnimationFrame(() => {
        bar.style.height = Math.max(0, Math.min(1, v / maxV)) * 76 + 'px';
      }));
      bars.append(h('div', { class: 'mcol' },
        h('div', { class: 'mbar-host' }, bar),
        h('div', { class: 'mlab' }, label)));
    }
    wrap.append(bars);

    /* ---- carte historique des séances ---- */
    wrap.append(h('div', { style: { height: '14px' } }));
    wrap.append(h('div', { class: 'card lcard', onclick: () => nav.nav.push('history') },
      icon('calendar_month'),
      h('div', { class: 'lt' }, L10n.s('Workout history', 'Historique des séances')),
      icon('chevron_right', { size: 16, cls: 'chev' })));

    /* ---- carte photos de progression (compteur + 5 vignettes récentes) ---- */
    const pc = h('div', { class: 'card', style: { padding: '13px 14px', cursor: 'pointer' }, onclick: () => nav.nav.push('progress') },
      h('div', { style: { display: 'flex', alignItems: 'center' } },
        icon('add_a_photo'),
        h('div', { class: 'lt' }, L10n.s('Progress photos', 'Photos de progression')),
        store.photos.length ? h('div', { class: 'cnt' }, String(store.photos.length)) : null,
        icon('chevron_right', { size: 16, cls: 'chev' })));
    const recent = S.photosDesc().slice(0, 5);
    if (recent.length) {
      const thumbs = h('div', { class: 'pthumbs' });
      for (const p of recent) {
        const th = h('div', { class: 'th' }, photoThumb(p));
        thumbs.append(th);
      }
      for (let i = 0; i < 5 - recent.length; i++) thumbs.append(h('div', { class: 'th' }));
      pc.append(thumbs);
    }
    wrap.append(h('div', { style: { height: '8px' } }), pc);

    /* ---- volume par groupe musculaire ---- */
    wrap.append(h('div', { class: 'pm-title' }, L10n.s('Volume by muscle group', 'Volume par groupe musculaire')));
    const dist = Calc.muscleDist(workouts, 15);
    if (!dist.length) {
      wrap.append(emptyState(L10n.s(
        'No data yet.\nYour stats will appear after your first workout.',
        'Aucune donnée.\nTes stats apparaîtront après ta première séance.')));
    } else {
      const maxVol = dist[0][1] || 1;
      for (const [muscle, v] of dist) {
        const bar = h('div', { class: 'accbar', style: { width: '0%' } });
        requestAnimationFrame(() => requestAnimationFrame(() => {
          bar.style.width = Math.max(0.02, Math.min(1, v / maxVol)) * 100 + '%';
        }));
        wrap.append(h('div', { class: 'mrow' },
          exCircle(muscle, 36),
          h('div', { class: 'mid' },
            h('div', { class: 'mnm' },
              h('div', { class: 'n' }, muscleName(muscle)),
              h('div', { class: 'v' }, `${Calc.fmtVol(v, unit)} ${uLabel}`)),
            h('div', { class: 'accbar-wrap' }, bar))));
      }
    }

    /* ---- pied de page ---- */
    wrap.append(h('div', { class: 'pfoot' },
      store.session
        ? L10n.s('Synced to your account', 'Synchronisé sur ton compte') + ' · ' + store.session.email
        : L10n.s('Local app · your data stays on this device.', 'Application locale · tes données restent sur cet appareil.')));
  }

  build();
  sub(el, 'workouts', build);
  sub(el, 'photos', build);
  sub(el, 'settings', build);
  sub(el, 'avatar', build);
  sub(el, 'session', build);
}

function pstat(v, l) {
  return h('div', { class: 'pstat' }, h('div', { class: 'v' }, v), h('div', { class: 'l' }, l));
}

/* Vignette carrée avec repli fond Card2 (PhotoImg du Kotlin). */
function photoThumb(p) {
  const el = h('div', { style: { width: '100%', height: '100%', background: 'var(--card2)', overflow: 'hidden', position: 'relative' } });
  S.photoUrl(p.id).then(url => {
    if (url) el.append(h('img', { src: url, alt: '', style: { width: '100%', height: '100%', objectFit: 'cover', display: 'block', position: 'relative', zIndex: '1' } }));
  });
  return el;
}

/**
 * Grille multi-annuelle des jours d'entraînement (parité TrainingDaysGrid.kt).
 * Un carré par jour, allumé les jours où tu t'es entraîné — en accent de l'app.
 * Années empilées + labels de mois + caption repos + closer.
 */
function trainingDaysGrid(dayVolumes) {
  const wrap = h('div', { class: 'tg' });
  const today = new Date();
  const trained = Object.keys(dayVolumes);
  const first = trained.length
    ? new Date(Math.min(...trained.map(k => new Date(k).getTime())))
    : today;
  const y0 = first.getFullYear();
  const y1 = today.getFullYear();

  wrap.append(h('div', { class: 'tg-cap' },
    L10n.s('One square per day, lit on the days you trained.',
      'Un carré par jour, allumé les jours où tu t\'es entraîné.')));

  for (let year = y0; year <= y1; year++) {
    wrap.append(yearGrid(year, dayVolumes, today));
  }

  // caption repos : le jour de semaine le moins entraîné
  if (trained.length >= 8) {
    const counts = [0, 0, 0, 0, 0, 0, 0]; // Mon=0 … Sun=6
    for (const k of trained) {
      const d = new Date(k);
      counts[(d.getDay() + 6) % 7]++;
    }
    let mi = 0;
    for (let i = 1; i < 7; i++) if (counts[i] < counts[mi]) mi = i;
    if (counts[mi] < trained.length / 20) {
      const name = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'][mi];
      const frName = ['Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi', 'Dimanche'][mi];
      wrap.append(h('div', { class: 'tg-rest' },
        L10n.s(`${name} is rest day.`, `${frName} c'est repos.`)));
    }
  }

  wrap.append(h('div', { class: 'tg-next' },
    L10n.s(`See you there in ${y1 + 1}`, `Rendez-vous en ${y1 + 1}`)));
  return wrap;
}

function yearGrid(year, dayVolumes, today) {
  const box = h('div', { class: 'tg-year' });
  const start = new Date(year, 0, 1), end = new Date(year, 11, 31);
  const lead = (start.getDay() + 6) % 7;
  const maxDay = Math.max(Calc.maxOf(Object.values(dayVolumes)), 1) || 1;
  let trainedCount = 0;
  const grid = h('div', { class: 'tg-grid' });
  for (let i = 0; i < lead; i++) grid.append(h('div', { class: 'tg-c' }));
  for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
    const dd = new Date(d.getFullYear(), d.getMonth(), d.getDate());
    const key = Calc.dayKey(dd.getTime());
    const v = dayVolumes[key] || 0;
    const future = dd > today;
    const isToday = dd.toDateString() === today.toDateString();
    if (v > 0) trainedCount++;
    const cell = h('div', { class: 'tg-c' + (isToday ? ' today' : '') });
    if (future) cell.style.opacity = '0.4';
    else if (v > 0) {
      cell.style.background = 'var(--accent)';
      cell.style.opacity = String(0.45 + 0.55 * Math.max(0, Math.min(1, v / maxDay)));
    }
    grid.append(cell);
  }
  const months = h('div', { class: 'tg-months' });
  for (let m = 0; m < 12; m++) {
    months.append(h('div', { class: 'tg-m' }, Calc.monthShort(new Date(year, m, 1).getTime()).slice(0, 3)));
  }
  box.append(
    h('div', { class: 'tg-yhead' },
      h('div', { class: 'tg-y' }, String(year)),
      trainedCount > 0 ? h('div', { class: 'tg-ycnt' },
        L10n.s(`${trainedCount} days`, `${trainedCount} jours`)) : null),
    months,
    grid,
  );
  return box;
}

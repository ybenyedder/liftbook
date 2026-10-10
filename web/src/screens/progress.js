// Écran Progression (photos) + visionneuse + comparaison — port de ui/Progress.kt
// (ProgressScreen, PhotoViewerScreen, EditPhotoDialog, ComparePhotosScreen, CompareChip).
import { h, icon, toast, sub, lineChart, imgEl, downscaleToJpeg, confirmDialog, dialog, emptyState } from '../ui.js';
import { store } from '../store.js';
import * as S from '../store.js';
import * as nav from '../router.js';
import * as Calc from '../calc.js';
import { L10n } from '../search.js';

nav.registerScreen('progress', { render });
nav.registerScreen('photoViewer', { render: renderViewer });
nav.registerScreen('comparePhotos', { render: renderCompare });

/* ---------------- utilitaires dates (SimpleDateFormat Locale.FRANCE du Kotlin) ---------------- */

// fmtDay : "d MMM yy" (ex. « 5 sept. 25 »)
function fmtDay(ts) {
  const d = Calc.localDate(ts);
  return `${d.getDate()} ${Calc.monthShort(ts)} ${String(d.getFullYear()).slice(-2)}`;
}

// fmtDayLong : "EEEE d MMMM yyyy" capitalisé (ex. « Lundi 5 septembre 2025 »)
function fmtDayLong(ts) {
  const d = Calc.localDate(ts);
  return `${Calc.dayName(ts)} ${d.getDate()} ${Calc.monthLabel(ts).split(' ')[0]} ${d.getFullYear()}`;
}

// Écart en JOURS CALENDAIRES (dates locales à minuit — fix v1.50 du Kotlin, ChronoUnit.DAYS)
function calendarDays(fromTs, toTs) {
  return Math.round((Calc.localDate(toTs) - Calc.localDate(fromTs)) / 86400000);
}

/* ---------------- CSS scoped ---------------- */

function css() {
  if (document.getElementById('css-prog')) return;
  const st = h('style', { id: 'css-prog' });  st.textContent = `
/* tuile photo commune : fond Card2 + repli icône appareil photo pendant le chargement */
.scr-prog .pimg, .scr-viewer .pimg, .scr-compare .pimg { position:relative; width:100%; height:100%; background:var(--card2); overflow:hidden; }
.scr-prog .pimg img, .scr-viewer .pimg img, .scr-compare .pimg img { position:relative; z-index:1; -webkit-user-drag:none; user-select:none; }
.scr-prog .pimg .ph-ic, .scr-viewer .pimg .ph-ic, .scr-compare .pimg .ph-ic { position:absolute; inset:0; display:flex; align-items:center; justify-content:center; color:var(--mut2); font-size:26px; }

/* ---- galerie (ProgressScreen) ---- */
.scr-prog .prog-empty { flex:1; display:flex; align-items:center; justify-content:center; padding:0 32px; }
.scr-prog .chart-lbl { font-size:13px; font-weight:800; color:var(--mut); padding:8px 16px; }
.scr-prog .grid-head { display:flex; align-items:center; padding:10px 16px; }
.scr-prog .grid-head .cnt { flex:1; font-size:15px; font-weight:800; }
.scr-prog .grid-head .cmp { font-size:13px; font-weight:600; color:var(--accent); background:none; border:none; padding:4px; cursor:pointer; }
.scr-prog .grid-head .hint { font-size:12px; color:var(--mut); }
.scr-prog .pgrid { display:grid; grid-template-columns:repeat(3,1fr); gap:6px; padding:0 12px; }
.scr-prog .pcell { position:relative; aspect-ratio:1; border-radius:10px; overflow:hidden; background:var(--card2); cursor:pointer; user-select:none; -webkit-touch-callout:none; touch-action:manipulation; }
.scr-prog .pcell.sel { box-shadow: inset 0 0 0 2.5px var(--accent); }
.scr-prog .pdate { position:absolute; left:5px; bottom:5px; background:rgba(0,0,0,.7); border-radius:5px; padding:2px 5px; color:#fff; font-size:9.5px; font-weight:600; z-index:2; }
.scr-prog .pchk { position:absolute; top:6px; right:6px; width:22px; height:22px; border-radius:99px; display:flex; align-items:center; justify-content:center; z-index:2; }
.scr-prog .pchk.on { background:var(--accent); color:#fff; font-size:13px; font-weight:700; }
.scr-prog .pchk.off { border:1.5px solid #fff; }
.scr-prog .prog-foot { height:24px; }

/* ---- visionneuse (PhotoViewerScreen) ---- */
.scr-viewer { overflow:hidden; background:#000; }
.scr-viewer .vbar { display:flex; align-items:center; gap:4px; padding:8px 8px 8px 4px; }
.scr-viewer .iconbtn { color:#fff; }
.scr-viewer .iconbtn:active { background:rgba(255,255,255,.12); }
.scr-viewer .vcount { flex:1; text-align:center; font-size:14px; font-weight:600; color:var(--mut); }
.scr-viewer .vpager { flex:1; min-height:0; display:flex; overflow-x:auto; overflow-y:hidden; scroll-snap-type:x mandatory; scrollbar-width:none; }
.scr-viewer .vpager::-webkit-scrollbar { display:none; }
.scr-viewer .vpage { flex:0 0 100%; min-width:100%; scroll-snap-align:center; }
.scr-viewer .vfoot { padding:14px 20px; }
.scr-viewer .vdate { color:#fff; font-size:15px; font-weight:700; }
.scr-viewer .vmeta { display:flex; align-items:center; margin-top:1px; }
.scr-viewer .vkg { color:var(--mut); font-size:13px; }
.scr-viewer .vwid { color:var(--mut2); font-size:12px; margin-left:12px; }
.scr-viewer .vnote { color:var(--mut); font-size:13px; line-height:18px; margin-top:6px; }
.scr-viewer .flabel { font-size:12px; color:var(--mut); margin-bottom:6px; }

/* ---- comparaison (ComparePhotosScreen) ---- */
.scr-compare { overflow:hidden; background:#000; }
.scr-compare .cbar { display:flex; align-items:center; gap:4px; padding:8px 8px 8px 4px; }
.scr-compare .iconbtn { color:#fff; }
.scr-compare .iconbtn:active { background:rgba(255,255,255,.12); }
.scr-compare .ctitle { flex:1; text-align:center; color:#fff; font-size:17px; font-weight:800; }
.scr-compare .chips { display:flex; justify-content:center; gap:8px; padding:8px 16px; }
.scr-compare .cchip { border:none; cursor:pointer; border-radius:99px; padding:7px 14px; font-size:12.5px; font-weight:600; background:var(--card2); color:#fff; }
.scr-compare .cchip.on { background:var(--accent); color:#000; }
.scr-compare .cstage { flex:1; min-height:0; display:flex; align-items:center; justify-content:center; padding:0 16px; }
.scr-compare .cslider { position:relative; width:100%; aspect-ratio:3/4; border-radius:16px; overflow:hidden; background:var(--card2); touch-action:none; cursor:ew-resize; }
.scr-compare .clayer { position:absolute; inset:0; }
.scr-compare .c-before { z-index:1; }
.scr-compare .cline { position:absolute; top:0; bottom:0; width:2px; background:#fff; z-index:2; }
.scr-compare .clineshadow { position:absolute; top:0; bottom:0; width:1px; background:rgba(0,0,0,.4); z-index:2; }
.scr-compare .chandle { position:absolute; top:50%; width:44px; height:44px; margin-left:-22px; margin-top:-22px; border-radius:99px; background:rgba(0,0,0,.8); color:#fff; font-size:20px; display:flex; align-items:center; justify-content:center; z-index:3; pointer-events:none; }
.scr-compare .clab { position:absolute; top:8px; color:#fff; font-size:11px; font-weight:700; background:rgba(0,0,0,.6); border-radius:6px; padding:3px 7px; z-index:3; pointer-events:none; }
.scr-compare .clab.b { left:8px; }
.scr-compare .clab.a { right:8px; }
.scr-compare .csbs { display:flex; gap:8px; width:100%; }
.scr-compare .csbs .col { flex:1; display:flex; flex-direction:column; align-items:center; }
.scr-compare .csbs .ph { width:100%; aspect-ratio:3/4; border-radius:14px; overflow:hidden; }
.scr-compare .csbs .who { color:var(--mut); font-size:11px; font-weight:800; margin-top:6px; }
.scr-compare .csbs .when { color:#fff; font-size:12px; font-weight:600; }
.scr-compare .cinfo { display:flex; justify-content:center; align-items:center; padding:16px; }
.scr-compare .cdays { color:var(--mut); font-size:13px; }
.scr-compare .cdelta { color:var(--accent); font-size:13px; font-weight:700; margin-left:14px; }
`;
  document.head.append(st);
}

/* ---------------- tuile photo (port de PhotoImg) ---------------- */

function photoImg(p, fit = 'cover') {
  const el = h('div', { class: 'pimg' }, icon('add_a_photo', { size: 26, cls: 'ph-ic' }));
  S.photoUrl(p.id).then(url => { if (url) el.append(imgEl(url, { fit })); });
  return el;
}

/* ================================ galerie ================================ */

function render(el) {
  css();
  el.classList.add('scr-prog');
  let selecting = false;
  const selected = []; // ids (max 2, ordre de sélection)

  const input = h('input', { type: 'file', accept: 'image/*', style: { display: 'none' } });
  input.addEventListener('change', async () => {
    const f = input.files && input.files[0];
    input.value = '';
    if (!f) return;
    try {
      const blob = await downscaleToJpeg(f, 1440, 0.86);
      await S.addPhoto(blob);
    } catch {
      toast(L10n.s('Could not read this image', 'Impossible de lire cette image'));
    }
  });

  el.append(
    h('div', { class: 'topbar' },
      h('button', { class: 'iconbtn', onclick: () => nav.nav.pop() }, icon('arrow_back')),
      h('div', { class: 'topbar-title', style: { fontSize: '18px', fontWeight: '800' } },
        L10n.s('Progress', 'Progression')),
      h('button', { class: 'iconbtn', style: { color: 'var(--accent)' }, onclick: () => input.click() },
        icon('add_a_photo')),
      input,
    ),
  );

  const wrap = h('div', { class: 'screen' });
  el.append(wrap);

  const cellEls = new Map(); // id -> élément .pcell
  const headEl = h('div');

  function tapCell(id) {
    if (selecting) {
      const i = selected.indexOf(id);
      if (i >= 0) selected.splice(i, 1);
      else if (selected.length < 2) selected.push(id);
      syncSel();
    } else {
      nav.nav.push('photoViewer', { id });
    }
  }

  function enterSelect(id) {
    if (!selecting) selecting = true;
    if (!selected.includes(id) && selected.length < 2) selected.push(id);
    syncSel();
  }

  function syncSel() {
    renderHead();
    for (const [id, c] of cellEls) {
      const sel = selected.includes(id);
      c.classList.toggle('sel', sel);
      let chk = c.querySelector('.pchk');
      if (selecting && !chk) {
        chk = h('div', { class: 'pchk' }, '✓');
        c.append(chk);
      }
      if (chk) chk.className = 'pchk ' + (sel ? 'on' : 'off');
      if (!selecting && chk) chk.remove();
    }
  }

  function renderHead() {
    const photos = S.photosDesc();
    headEl.replaceChildren(
      h('div', { class: 'grid-head' },
        h('div', { class: 'cnt' }, `${photos.length} ${L10n.s('photos', 'photos')}`),
        selecting
          ? h('button', {
            class: 'cmp', onclick: () => {
              if (selected.length === 2) {
                const byAge = [...selected].sort((x, y) => (S.photoById(x)?.ts || 0) - (S.photoById(y)?.ts || 0));
                nav.nav.push('comparePhotos', { aId: byAge[0], bId: byAge[1] });
              }
            },
          }, selected.length === 2 ? L10n.s('Compare →', 'Comparer →') : L10n.s('Select 2 photos', 'Choisis 2 photos'))
          : (photos.length >= 2 ? h('div', { class: 'hint' }, L10n.s('Long-press to compare', 'Appui long pour comparer')) : null),
      ),
    );
  }

  function build() {
    cellEls.clear();
    const photos = S.photosDesc();
    // purge la sélection des photos disparues
    for (let i = selected.length - 1; i >= 0; i--) if (!S.photoById(selected[i])) selected.splice(i, 1);
    wrap.replaceChildren();

    if (!photos.length) {
      wrap.append(h('div', { class: 'prog-empty' }, emptyState(L10n.s(
        'No progress photos yet.\nAdd one after a workout to track your physique.',
        'Aucune photo de progression.\nAjoute-en une après une séance pour suivre ton physique.'))));
      return;
    }

    const weights = photos.filter(p => p.kg != null).sort((a, b) => a.ts - b.ts);
    if (weights.length >= 2) {
      const unit = store.settings.unit;
      wrap.append(
        h('div', { class: 'chart-lbl' }, L10n.s('Body weight', 'Poids corporel')),
        h('div', { style: { padding: '0 8px' } },
          lineChart(weights.map(p => ({ label: fmtDay(p.ts), value: p.kg })),
            v => Calc.fmtKg(v, unit) + Calc.unitLabel(unit))),
      );
    }

    wrap.append(headEl);
    renderHead();

    const grid = h('div', { class: 'pgrid' });
    for (const p of photos) grid.append(photoCell(p));
    wrap.append(grid, h('div', { class: 'prog-foot' }));
    syncSel();
  }

  function photoCell(p) {
    const cell = h('div', { class: 'pcell' },
      photoImg(p),
      h('div', { class: 'pdate' }, fmtDay(p.ts)));
    cellEls.set(p.id, cell);
    // appui long ~500 ms → mode sélection (combinedClickable onLongClick)
    let timer = null, fired = false, sx = 0, sy = 0;
    const cancel = () => { if (timer) { clearTimeout(timer); timer = null; } };
    cell.addEventListener('pointerdown', e => {
      if (e.button !== 0) return;
      fired = false; sx = e.clientX; sy = e.clientY;
      timer = setTimeout(() => { timer = null; fired = true; enterSelect(p.id); }, 500);
    });
    cell.addEventListener('pointerup', cancel);
    cell.addEventListener('pointercancel', cancel);
    cell.addEventListener('pointerleave', cancel);
    cell.addEventListener('pointermove', e => { if (Math.hypot(e.clientX - sx, e.clientY - sy) > 12) cancel(); });
    cell.addEventListener('contextmenu', e => e.preventDefault());
    cell.addEventListener('click', () => { if (fired) { fired = false; return; } tapCell(p.id); });
    return cell;
  }

  build();
  sub(el, 'photos', build);
  sub(el, 'settings', build);
}

/* ================================ visionneuse ================================ */

function renderViewer(el, { id }) {
  css();
  el.classList.add('scr-viewer');
  let photos = S.photosDesc();
  const start = photos.findIndex(p => p.id === id);
  if (start < 0) { nav.nav.pop(); return; }

  const countEl = h('div', { class: 'vcount' }, start < photos.length ? `${start + 1}/${photos.length}` : '');
  const footEl = h('div', { class: 'vfoot' });

  const pager = h('div', { class: 'vpager' });
  for (const p of photos) pager.append(h('div', { class: 'vpage' }, photoImg(p, 'contain')));
  pager.scrollLeft = start * (pager.clientWidth || 0);
  requestAnimationFrame(() => { pager.scrollLeft = start * pager.clientWidth; });

  const pageOf = () => Math.max(0, Math.min(photos.length - 1, Math.round(pager.scrollLeft / (pager.clientWidth || 1))));
  const current = () => photos[pageOf()] || null;

  function syncTop() {
    countEl.textContent = photos.length > 1 ? `${Math.min(pageOf() + 1, photos.length)}/${photos.length}` : '';
  }

  function syncFoot() {
    const p = current();
    footEl.replaceChildren();
    if (!p) return;
    const unit = store.settings.unit;
    footEl.append(h('div', { class: 'vdate' }, fmtDayLong(p.ts)));
    if (p.kg != null || (p.wId != null && S.workoutById(p.wId))) {
      footEl.append(h('div', { class: 'vmeta' },
        p.kg != null ? h('div', { class: 'vkg' },
          L10n.s('Body weight', 'Poids corporel') + ' : ' + Calc.fmtKg(p.kg, unit) + Calc.unitLabel(unit)) : null,
        (p.wId != null && S.workoutById(p.wId)) ? h('div', { class: 'vwid' },
          '· ' + L10n.s('linked to a workout', 'liée à une séance')) : null,
      ));
    }
    if (p.note && p.note.length) footEl.append(h('div', { class: 'vnote' }, p.note));
  }

  let raf = false;
  pager.addEventListener('scroll', () => {
    if (raf) return;
    raf = true;
    requestAnimationFrame(() => { raf = false; syncTop(); syncFoot(); });
  });

  function editCurrent() {
    const p = current();
    if (!p) return;
    editPhotoDialog(p);
  }

  function deleteCurrent() {
    const p = current();
    if (!p) return;
    confirmDialog({
      title: L10n.s('Delete this photo?', 'Supprimer cette photo ?'),
      message: L10n.s('It will be removed from all your devices.', 'Elle sera supprimée de tous tes appareils.'),
      confirmLabel: L10n.s('Delete', 'Supprimer'),
      destructive: true,
      onConfirm: async () => {
        await S.deletePhoto(p.id);
        if (!store.photos.length) nav.nav.pop();
      },
    });
  }

  el.append(
    h('div', { class: 'vbar' },
      h('button', { class: 'iconbtn', onclick: () => nav.nav.pop() }, icon('arrow_back')),
      countEl,
      h('button', { class: 'iconbtn', onclick: editCurrent }, icon('edit')),
      h('button', { class: 'iconbtn', onclick: deleteCurrent }, icon('delete')),
    ),
    pager,
    footEl,
  );
  syncTop();
  syncFoot();

  sub(el, 'photos', () => {
    photos = S.photosDesc();
    if (!photos.length) { nav.nav.pop(); return; }
    // page ramenée dans les bornes si la photo disparaît (LaunchedEffect du Kotlin)
    const target = Math.min(pageOf(), photos.length - 1);
    pager.scrollTo({ left: target * pager.clientWidth });
    syncTop();
    syncFoot();
  });
  sub(el, 'settings', () => { syncFoot(); });
}

/* EditPhotoDialog : note + poids corporel saisi dans l'UNITÉ D'AFFICHAGE (fix v1.50). */
function editPhotoDialog(p) {
  const unit = store.settings.unit;
  const note = h('textarea', { class: 'field', style: { minHeight: '66px', resize: 'none' } });
  note.value = p.note || '';
  const kg = h('input', { class: 'field', inputmode: 'decimal' });
  kg.value = p.kg != null ? Calc.fmtKg(p.kg, unit) : ''; // pré-rempli dans l'unité d'affichage
  kg.addEventListener('input', () => {
    const f = kg.value.replace(/[^0-9.,]/g, '').replace(/,/g, '.');
    if (f !== kg.value) kg.value = f;
  });
  dialog({
    title: L10n.s('Edit photo', 'Modifier la photo'),
    body: h('div', { style: { margin: '4px 0 6px' } },
      h('div', { class: 'flabel' }, L10n.s('Note', 'Note')),
      note,
      h('div', { class: 'flabel', style: { marginTop: '10px' } }, L10n.s('Body weight (optional)', 'Poids corporel (optionnel)')),
      kg),
    actions: [
      { label: L10n.s('Cancel', 'Annuler'), class: 'mut' },
      {
        label: L10n.s('Save', 'Enregistrer'), class: 'acc', onClick: () => {
          // toDoubleOrNull strict du Kotlin (rejette « 1.2.3 »), puis conversion en kg
          const s = kg.value.trim();
          const num = /^\d+(\.\d+)?$/.test(s) ? parseFloat(s) : NaN;
          const parsed = isNaN(num) ? null : Calc.toKg(s, unit);
          S.updatePhoto(p.id, note.value, parsed, s !== '');
        },
      },
    ],
  });
}

/* ================================ comparaison ================================ */

function renderCompare(el, { aId, bId }) {
  css();
  el.classList.add('scr-compare');
  let a = S.photoById(aId), b = S.photoById(bId);
  if (!a || !b) { nav.nav.pop(); return; }
  const older = a.ts <= b.ts ? a : b, newer = a.ts <= b.ts ? b : a;
  let beforeId = older.id, afterId = newer.id; // AVANT = plus ancienne, APRÈS = plus récente
  const before = () => S.photoById(beforeId);
  const after = () => S.photoById(afterId);
  let mode = 0;       // 0 = curseur, 1 = côte à côte
  let fraction = 0.5; // position du curseur (3 %–97 %)

  const stage = h('div', { class: 'cstage' });
  const banner = h('div', { class: 'cinfo' });

  /* ---- vue curseur ---- */
  const slider = h('div', { class: 'cslider' });
  const afterLayer = h('div', { class: 'clayer' });
  const beforeLayer = h('div', { class: 'clayer c-before' }); // AVANT clippé à gauche du curseur
  const line = h('div', { class: 'cline' });
  const lineShadow = h('div', { class: 'clineshadow' });
  const handle = h('div', { class: 'chandle' }, '⇄');
  const labB = h('div', { class: 'clab b' });
  const labA = h('div', { class: 'clab a' });
  slider.append(afterLayer, beforeLayer, lineShadow, line, handle, labB, labA);

  function setLayer(holder, p) {
    holder.replaceChildren(photoImg(p));
  }

  function syncSlider() {
    const pct = fraction * 100;
    beforeLayer.style.clipPath = `inset(0 ${100 - pct}% 0 0)`;
    line.style.left = `calc(${pct}% - 1px)`;
    lineShadow.style.left = `calc(${pct}% + 1.5px)`;
    handle.style.left = pct + '%';
  }

  function syncLabels() {
    labB.textContent = L10n.s('BEFORE', 'AVANT') + ' · ' + fmtDay(before().ts);
    labA.textContent = L10n.s('AFTER', 'APRÈS') + ' · ' + fmtDay(after().ts);
  }

  // glissière : pointer events, position absolue (poignée verticale draggable)
  const dragFrom = e => {
    const r = slider.getBoundingClientRect();
    fraction = Math.max(0.03, Math.min(0.97, (e.clientX - r.left) / r.width));
    syncSlider();
  };
  // le drag natif d'une <img> volerait le pointer capture (pointercancel)
  slider.addEventListener('dragstart', e => e.preventDefault());
  slider.addEventListener('pointerdown', e => {
    slider.setPointerCapture(e.pointerId);
    dragFrom(e);
  });
  slider.addEventListener('pointermove', e => {
    if (slider.hasPointerCapture && slider.hasPointerCapture(e.pointerId)) dragFrom(e);
  });

  /* ---- vue côte à côte ---- */
  const sbBefore = h('div', { class: 'ph' });
  const sbAfter = h('div', { class: 'ph' });
  const sbWhoB = h('div', { class: 'who' }, L10n.s('BEFORE', 'AVANT'));
  const sbWhoA = h('div', { class: 'who' }, L10n.s('AFTER', 'APRÈS'));
  const sbWhenB = h('div', { class: 'when' });
  const sbWhenA = h('div', { class: 'when' });
  const sbs = h('div', { class: 'csbs' },
    h('div', { class: 'col' }, sbBefore, sbWhoB, sbWhenB),
    h('div', { class: 'col' }, sbAfter, sbWhoA, sbWhenA));

  function syncSbs() {
    sbWhenB.textContent = fmtDay(before().ts);
    sbWhenA.textContent = fmtDay(after().ts);
  }

  function renderStage() {
    setLayer(afterLayer, after());
    setLayer(beforeLayer, before());
    setLayer(sbBefore, before());
    setLayer(sbAfter, after());
    syncLabels();
    syncSbs();
    stage.replaceChildren(mode === 0 ? slider : sbs);
    if (mode === 0) syncSlider();
  }

  function renderBanner() {
    const unit = store.settings.unit;
    // recalculés à chaque rendu (recomposition Kotlin : le swap inverse le signe du delta
    // et l'écart devient négatif → « Le même jour »)
    const b = before(), a = after();
    const days = calendarDays(b.ts, a.ts);
    const kgDelta = (b.kg != null && a.kg != null) ? a.kg - b.kg : null;
    banner.replaceChildren(
      h('div', { class: 'cdays' },
        days > 0
          ? `${days} ${L10n.s('days apart', "jours d'écart")}`
          : L10n.s('Same day', 'Le même jour')));
    if (kgDelta != null) {
      banner.append(h('div', { class: 'cdelta' },
        (kgDelta >= 0 ? '+' : '−') + Calc.fmtKg(Math.abs(kgDelta), unit) + Calc.unitLabel(unit)));
    }
  }

  el.append(
    h('div', { class: 'cbar' },
      h('button', { class: 'iconbtn', onclick: () => nav.nav.pop() }, icon('arrow_back')),
      h('div', { class: 'ctitle' }, L10n.s('Comparison', 'Comparaison')),
      h('button', {
        class: 'iconbtn',
        onclick: () => { const t = beforeId; beforeId = afterId; afterId = t; fraction = 1 - fraction; renderStage(); renderBanner(); },
      }, icon('swap_horiz')),
    ),
    h('div', { class: 'chips' },
      h('button', { class: 'cchip' + (mode === 0 ? ' on' : ''), onclick: () => { mode = 0; syncChips(); renderStage(); } },
        L10n.s('Slider', 'Curseur')),
      h('button', { class: 'cchip' + (mode === 1 ? ' on' : ''), onclick: () => { mode = 1; syncChips(); renderStage(); } },
        L10n.s('Side by side', 'Côte à côte')),
    ),
    stage,
    banner,
  );

  const chipEls = el.querySelectorAll('.cchip');
  function syncChips() { chipEls.forEach((c, i) => c.classList.toggle('on', mode === i)); }

  renderStage();
  renderBanner();

  sub(el, 'photos', () => {
    // une photo supprimée par le sync fait quitter l'écran (rev du Kotlin)
    if (!S.photoById(beforeId) || !S.photoById(afterId)) { nav.nav.pop(); return; }
    renderStage(); // rafraîchit les objets (note/kg édités)
  });
}

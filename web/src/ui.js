// Composants partagés du port web — port direct de ui/Comps.kt + dialogues/sheets/menus.
import { FIGURES } from './figures.js';
import { on, off } from './store.js';
import { L10n } from './search.js';

/* icônes Material Symbols par codepoint PUA (la fonte sous-ensemblée n'a pas les ligatures) */
let MSR_MAP = null;
async function msrCodepoint(name) {
  if (!MSR_MAP) {
    try { MSR_MAP = await (await fetch('assets/material-symbols-map.json')).json(); }
    catch { MSR_MAP = {}; }
  }
  return MSR_MAP[name] ? String.fromCodePoint(parseInt(MSR_MAP[name], 16)) : name;
}

/** Abonnement store lié à la durée de vie de l'écran (nettoyé au changement d'écran). */
export function sub(el, topic, cb) {
  on(topic, cb);
  el.addEventListener('screen-gone', () => off(topic, cb), { once: true });
}

/* ---------- hyperscript ---------- */
export function h(tag, attrs = {}, ...kids) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs || {})) {
    if (v == null) continue;
    if (k === 'class') el.className = v;
    else if (k === 'style' && typeof v === 'object') Object.assign(el.style, v);
    else if (k.startsWith('on') && typeof v === 'function') el.addEventListener(k.slice(2), v);
    else if (k === 'dataset') Object.assign(el.dataset, v);
    else if (k === 'value') el.value = v;
    else if (k === 'checked') el.checked = !!v;
    // ⚠️ pas de prop `html`/innerHTML ici : tout contenu doit passer par des nœuds texte
    // (createTextNode ci-dessous) — c'est ce qui rend l'app XSS-safe par construction.
    else el.setAttribute(k, v);
  }
  for (const kid of kids.flat(9)) {
    if (kid == null || kid === false) continue;
    el.append(kid.nodeType ? kid : document.createTextNode(kid));
  }
  return el;
}
export const div = (cls, ...kids) => h('div', { class: cls }, ...kids);
export const span = (cls, ...kids) => h('span', { class: cls }, ...kids);
export const btn = (cls, onclick, ...kids) => h('button', { class: cls, onclick }, ...kids);
export function icon(name, { fill = false, size = null, cls = '' } = {}) {
  const s = h('span', { class: `msr${fill ? ' fill' : ''}${cls ? ' ' + cls : ''}` }, name);
  if (size) s.style.fontSize = size + 'px';
  msrCodepoint(name).then(cp => { if (cp !== name) s.textContent = cp; });
  return s;
}

/* ---------- toast (port de Util.kt toast) ---------- */
let toastTimer = null;
export function toast(msg) {
  let host = document.getElementById('toast-host');
  if (!host) { host = h('div', { class: 'toast-host', id: 'toast-host' }); document.getElementById('app').append(host); }
  host.replaceChildren(h('div', { class: 'toast' }, msg));
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => host.replaceChildren(), 2600);
}

/* ---------- dialogue central (AlertDialog) ---------- */
export function dialog({ title, message, actions = [], onDismiss = null, body = null }) {
  const overlay = h('div', { class: 'overlay' });
  const close = () => overlay.remove();
  const acts = actions.map(a => h('button', {
    class: a.class || 'acc',
    onclick: () => { close(); a.onClick && a.onClick(); },
  }, a.label));
  const d = h('div', { class: 'dialog' },
    title && h('div', { class: 'dialog-title' }, title),
    message && h('div', { class: 'dialog-msg' }, message),
    body,
    h('div', { class: 'dialog-actions' }, acts),
  );
  overlay.append(d);
  overlay.addEventListener('click', e => { if (e.target === overlay) { close(); onDismiss && onDismiss(); } });
  document.getElementById('app').append(overlay);
  return close;
}

export function confirmDialog({ title, message, confirmLabel = 'Confirmer', cancelLabel = 'Annuler', destructive = false, onConfirm, onCancel = null }) {
  return dialog({
    title, message,
    actions: [
      { label: cancelLabel, class: 'mut', onClick: onCancel },
      { label: confirmLabel, class: destructive ? 'red' : 'acc', onClick: onConfirm },
    ],
  });
}

/** Dialogue à champ texte (PromptAlert iOS / dialogs Android). */
export function promptDialog({ title, placeholder = '', value = '', maxLength = null, prefix = null, filter = null, confirmLabel = 'Enregistrer', cancelLabel = 'Annuler', onConfirm, multiline = false, numeric = null }) {
  const input = multiline
    ? h('textarea', { class: 'field', placeholder, style: { minHeight: '84px', resize: 'none' } })
    : h('input', { class: 'field', placeholder });
  input.value = value || '';
  if (maxLength) input.maxLength = maxLength;
  if (numeric === 'decimal') input.inputMode = 'decimal';
  if (numeric === 'number') input.inputMode = 'numeric';
  input.addEventListener('input', () => {
    if (!filter) return;
    const f = filter(input.value);
    if (f !== input.value) input.value = f;
  });
  const submit = () => {
    const v = input.value.trim();
    onConfirm && onConfirm(v);
  };
  if (!multiline) input.addEventListener('keydown', e => { if (e.key === 'Enter') submit(); });
  setTimeout(() => input.focus(), 60);
  return dialog({
    title,
    body: h('div', { style: { margin: '4px 0 6px' } }, prefix ? h('div', { class: 't-xs', style: { marginBottom: '6px' } }, prefix) : null, input),
    actions: [
      { label: cancelLabel, class: 'mut' },
      { label: confirmLabel, class: 'acc', onClick: submit },
    ],
  });
}

/** Dialogue-liste (choix muscle/équipement). */
export function listDialog({ title, items, onPick, render = null }) {
  const list = h('div', { style: { margin: '0 -6px 10px', maxHeight: '320px', overflowY: 'auto' } });
  for (const it of items) {
    list.append(h('button', {
      class: 'menu-item',
      onclick: () => { close(); onPick(it); },
    }, render ? render(it) : (it.label ?? it)));
  }
  const close = dialog({ title, body: list, actions: [{ label: 'Annuler', class: 'mut' }] });
  return close;
}

/** Séance déjà en cours → Reprendre / Supprimer et recommencer (Comps.kt WorkoutInProgressDialog). */
export function workoutInProgressDialog({ onResume, onRestart }) {
  return dialog({
    title: L10n.s('Workout in progress', 'Une séance est déjà en cours'),
    message: L10n.s('Resume it, or discard it and start a new one.', "Reprends-la, ou supprime-la pour en commencer une nouvelle."),
    actions: [
      { label: L10n.s('Discard & restart', 'Supprimer et recommencer'), class: 'red', onClick: onRestart },
      { label: L10n.s('Resume', 'Reprendre'), class: 'acc', onClick: onResume },
    ],
  });
}

/* ---------- bottom-sheet ---------- */
export function sheet({ title = '', okLabel = null, onOk = null, onClose = null, render }) {
  const overlay = h('div', { class: 'sheet-overlay' });
  const close = (viaOverlay = false) => { overlay.remove(); onClose && viaOverlay && onClose(); };
  const body = h('div', { class: 'sheet-body' });
  const head = h('div', { class: 'sheet-head' },
    h('div', { class: 't' }, title),
    okLabel
      ? h('button', { class: 'btn-acc-text', onclick: () => { close(); onOk && onOk(); } }, okLabel)
      : h('button', { onclick: () => close(true) }, icon('close', { size: 22, cls: 't-mut' })),
  );
  const sh = h('div', { class: 'sheet' }, head, body);
  overlay.append(sh);
  overlay.addEventListener('click', e => { if (e.target === overlay) close(true); });
  document.getElementById('app').append(overlay);
  render(body, close);
  return { close, body };
}

/* ---------- menu contextuel ⋮ ---------- */
export function ctxMenu(anchor, items) {
  document.querySelectorAll('.menu-wrap').forEach(m => m.remove());
  const wrap = h('div', { class: 'menu-wrap' });
  const menu = h('div', { class: 'menu' });
  for (const it of items) {
    if (!it) continue;
    menu.append(h('button', {
      class: 'menu-item' + (it.red ? ' red' : ''),
      onclick: () => { wrap.remove(); it.onClick && it.onClick(); },
    }, it.icon ? icon(it.icon, { size: 20 }) : null, it.label));
  }
  wrap.append(menu);
  document.body.append(wrap);
  const r = anchor.getBoundingClientRect();
  const appR = document.getElementById('app').getBoundingClientRect();
  wrap.style.top = '0px'; wrap.style.left = '0px';
  const mw = menu, mh = menu;
  requestAnimationFrame(() => {
    const bw = wrap.getBoundingClientRect();
    let x = r.right - bw.width; let y = r.bottom + 4;
    if (y + bw.height > window.innerHeight - 8) y = Math.max(8, r.top - bw.height - 4);
    if (x < appR.left + 8) x = appR.left + 8;
    wrap.style.left = x + 'px'; wrap.style.top = y + 'px';
  });
  setTimeout(() => {
    const off = e => {
      if (!wrap.contains(e.target)) { wrap.remove(); document.removeEventListener('pointerdown', off, true); }
    };
    document.addEventListener('pointerdown', off, true);
  });
  return () => wrap.remove();
}

/* ---------- composants Compose ---------- */
export function primaryButton(label, { leading = null, onClick = null, compact = false, small = false, disabled = false } = {}) {
  return h('button', {
    class: `btn-primary${compact ? ' compact' : ''}${small ? ' small' : ''}`,
    onclick: disabled ? null : onClick,
    disabled,
  }, leading, label);
}

export function ghostButton(label, { leading = null, onClick = null } = {}) {
  return h('button', { class: 'btn-sec', onclick: onClick }, leading, label);
}

export function chipEl(label, selected, onClick) {
  return h('button', { class: 'chip' + (selected ? ' sel' : ''), onclick: onClick }, label);
}

export function sectionLabel(text) {
  return h('div', { class: 'section-label' }, text.toUpperCase());
}

export function emptyState(text, slim = false) {
  return h('div', { class: 'empty', style: slim ? { padding: '10px 30px' } : null }, text);
}

/** Avatar : photo si dispo, sinon initiale sur cercle accent (Avatar/AvatarImg Comps.kt). */
export function avatarEl(letter, size, imgUrl = null) {
  if (imgUrl) {
    return h('div', { class: 'avatar', style: { width: size + 'px', height: size + 'px' } },
      h('img', { src: imgUrl, alt: '', style: { width: '100%', height: '100%', objectFit: 'cover' } }));
  }
  return h('div', {
    class: 'avatar',
    style: { width: size + 'px', height: size + 'px', fontSize: Math.round(size * 0.42) + 'px' },
  }, (letter || '?').slice(0, 1).toUpperCase());
}

/** Silhouette muscle en SVG inline (ill_*.xml Android → figures.js). */
export function figureSvg(muscle, size, { variant = 'dark' } = {}) {
  const fig = (FIGURES[muscle] || FIGURES['Chest'])[variant];
  const ns = 'http://www.w3.org/2000/svg';
  const svg = document.createElementNS(ns, 'svg');
  svg.setAttribute('viewBox', `0 0 ${fig.vw} ${fig.vh}`);
  svg.setAttribute('width', size); svg.setAttribute('height', Math.round(size * fig.vh / fig.vw));
  svg.style.display = 'block';
  for (const p of fig.paths) {
    const el = document.createElementNS(ns, 'path');
    el.setAttribute('d', p.d); el.setAttribute('fill', p.fill);
    svg.append(el);
  }
  return svg;
}

/** ExCircle : badge blanc + silhouette sombre, muscle coloré (L10n.kt ExCircle). */
export function exCircle(muscle, size) {
  const c = h('div', { class: 'excircle', style: { width: size + 'px', height: size + 'px' } });
  const fig = figureSvg(muscle, Math.round(size * 0.72), { variant: 'dark' });
  c.append(fig);
  return c;
}

/** BodyMap Hevy — pictogramme 34×56, muscles travaillés en accent (Comps.kt port exact). */
export function bodyMap(front, muscles, w = 34) {
  const H = Math.round(w * 56 / 34);
  const ns = 'http://www.w3.org/2000/svg';
  const svg = document.createElementNS(ns, 'svg');
  svg.setAttribute('viewBox', `0 0 ${w} ${H}`);
  svg.setAttribute('width', w); svg.setAttribute('height', H);
  svg.style.borderRadius = '9px';
  svg.style.display = 'block';
  const BODY = '#4A4F56';
  const ACC = 'var(--accent)';
  const P = d => { const p = document.createElementNS(ns, 'path'); p.setAttribute('d', d); p.setAttribute('fill', BODY); svg.append(p); return p; };
  const C = (cx, cy, r, fill = BODY) => { const c = document.createElementNS(ns, 'circle'); c.setAttribute('cx', cx); c.setAttribute('cy', cy); c.setAttribute('r', r); c.setAttribute('fill', fill); svg.append(c); return c; };
  const L = (x1, y1, x2, y2, t) => { const l = document.createElementNS(ns, 'line');
    for (const [k, v] of Object.entries({ x1: w * x1, y1: H * y1, x2: w * x2, y2: H * y2 })) l.setAttribute(k, v);
    l.setAttribute('stroke', BODY); l.setAttribute('stroke-width', w * t); l.setAttribute('stroke-linecap', 'round'); svg.append(l); };
  // tête + cou
  C(w * .5, H * .085, w * .115);
  L(.5, .14, .5, .185, .11);
  // torse en V
  P(`M ${w * .36} ${H * .19} L ${w * .64} ${H * .19} L ${w * .585} ${H * .475} L ${w * .415} ${H * .475} Z`);
  // bassin
  const rr = document.createElementNS(ns, 'rect');
  rr.setAttribute('x', w * .415); rr.setAttribute('y', H * .475);
  rr.setAttribute('width', w * .17); rr.setAttribute('height', H * .075);
  rr.setAttribute('rx', w * .09); rr.setAttribute('fill', BODY);
  svg.append(rr);
  // bras
  L(.345, .205, .245, .31, .105); L(.245, .31, .275, .435, .09);
  L(.655, .205, .755, .31, .105); L(.755, .31, .725, .435, .09);
  // jambes
  L(.455, .545, .43, .75, .125); L(.43, .75, .425, .925, .10);
  L(.545, .545, .57, .75, .125); L(.57, .75, .575, .925, .10);
  // muscles travaillés
  const SPOTS = {
    Chest: [[true, .37, .28], [true, .63, .28]], Shoulders: [[true, .30, .21], [true, .70, .21]],
    Biceps: [[true, .23, .32], [true, .77, .32]], Forearms: [[true, .17, .40], [true, .83, .40]],
    Abs: [[true, .5, .38]], Quads: [[true, .42, .60], [true, .58, .60]],
    Adductors: [[true, .46, .52], [true, .54, .52]],
    Calves: [[true, .41, .82], [true, .59, .82], [false, .41, .82], [false, .59, .82]],
    Traps: [[true, .5, .17], [false, .5, .17]], Lats: [[false, .37, .30], [false, .63, .30]],
    Triceps: [[false, .23, .32], [false, .77, .32]], 'Lower back': [[false, .5, .38]],
    Glutes: [[false, .43, .47], [false, .57, .47]], Hamstrings: [[false, .42, .60], [false, .58, .60]],
    Abductors: [[false, .36, .52], [false, .64, .52]],
  };
  for (const m of muscles) (SPOTS[m] || []).forEach(([f, x, y]) => { if (f === front) C(w * x, H * y, w * .085, ACC); });
  return svg;
}

/** LineChart maison — courbe lissée + dégradé accent + animation 650 ms + labels Y à droite (Comps.kt port exact). */
export function lineChart(points, fmtLabel = v => String(v)) {
  const el = h('div');
  points = (points || []).filter(p => p && Number.isFinite(p.value)); // NaN/Infinity → attribut SVG invalide, courbe morte
  // Comps.kt : le Canvas fait toujours 150dp — avec <2 points il reste un bloc vide de
  // la même hauteur (return@Canvas avant tout dessin), pas un div de hauteur 0.
  if (points.length < 2) { el.style.height = '150px'; return el; }
  const W = 340, Hh = 150, padL = 6, padR = 54, padT = 16, padB = 20;
  const iw = W - padL - padR, ih = Hh - padT - padB;
  let vals = points.map(p => p.value);
  let min = Math.min(...vals), max = Math.max(...vals);
  if (max - min < max * 0.06 + 1.0) { const mid = (max + min) / 2; min = mid * 0.97; max = mid * 1.03; }
  const pad = (max - min) * 0.1; min -= pad; max += pad;
  if (max - min < 1e-9) { min -= 1.0; max += 1.0; }
  const x = i => padL + (i / (points.length - 1)) * iw;
  const y = v => padT + ih - ((v - min) / (max - min)) * ih;
  const ns = 'http://www.w3.org/2000/svg';
  const svg = document.createElementNS(ns, 'svg');
  svg.setAttribute('viewBox', `0 0 ${W} ${Hh}`);
  svg.style.width = '100%'; svg.style.height = '150px'; svg.style.display = 'block';
  for (let g = 0; g <= 2; g++) {
    const gy = padT + ih * g / 2;
    const l = document.createElementNS(ns, 'line');
    l.setAttribute('x1', padL); l.setAttribute('y1', gy); l.setAttribute('x2', padL + iw); l.setAttribute('y2', gy);
    l.setAttribute('stroke', 'rgba(141,147,153,.15)');
    svg.append(l);
  }
  let d = `M ${x(0)} ${y(vals[0])}`;
  for (let i = 1; i < points.length; i++) {
    const x0 = x(i - 1), y0 = y(vals[i - 1]), x1 = x(i), y1 = y(vals[i]);
    d += ` Q ${x0} ${y0} ${(x0 + x1) / 2} ${(y0 + y1) / 2}`;
  }
  d += ` L ${x(points.length - 1)} ${y(vals[vals.length - 1])}`;
  const fill = d + ` L ${x(points.length - 1)} ${padT + ih} L ${x(0)} ${padT + ih} Z`;
  const gid = 'lg' + Math.random().toString(36).slice(2, 7);
  const defs = document.createElementNS(ns, 'defs');
  defs.innerHTML = `<linearGradient id="${gid}" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0" stop-color="var(--accent)" stop-opacity=".35"/>
    <stop offset="1" stop-color="var(--accent)" stop-opacity="0"/></linearGradient>`;
  svg.append(defs);
  const group = document.createElementNS(ns, 'g');
  const f = document.createElementNS(ns, 'path');
  f.setAttribute('d', fill); f.setAttribute('fill', `url(#${gid})`);
  const p = document.createElementNS(ns, 'path');
  p.setAttribute('d', d); p.setAttribute('fill', 'none'); p.setAttribute('stroke', 'var(--accent)');
  p.setAttribute('stroke-width', '4'); p.setAttribute('stroke-linecap', 'round');
  const lx = x(points.length - 1), ly = y(vals[vals.length - 1]);
  const c1 = document.createElementNS(ns, 'circle');
  c1.setAttribute('cx', lx); c1.setAttribute('cy', ly); c1.setAttribute('r', '6'); c1.setAttribute('fill', 'var(--accent)');
  const c2 = document.createElementNS(ns, 'circle');
  c2.setAttribute('cx', lx); c2.setAttribute('cy', ly); c2.setAttribute('r', '3.5'); c2.setAttribute('fill', '#000');
  group.append(f, p, c1, c2);
  const clip = document.createElementNS(ns, 'clipPath');
  const rect = document.createElementNS(ns, 'rect');
  rect.setAttribute('x', '0'); rect.setAttribute('y', '0'); rect.setAttribute('height', Hh); rect.setAttribute('width', '0');
  clip.append(rect); clip.id = 'cl' + gid; defs.append(clip);
  group.setAttribute('clip-path', `url(#${clip.id})`);
  svg.append(group);
  const T = 650;
  const t0 = performance.now();
  const tick = now => {
    const k = Math.min(1, (now - t0) / T);
    rect.setAttribute('width', String(W * (1 - Math.pow(1 - k, 3))));
    if (k < 1) requestAnimationFrame(tick);
  };
  requestAnimationFrame(tick);
  const lab = (v, yy) => {
    const t = document.createElementNS(ns, 'text');
    t.setAttribute('x', W - 4); t.setAttribute('y', yy); t.setAttribute('text-anchor', 'end');
    t.setAttribute('fill', 'rgba(141,147,153,.85)'); t.setAttribute('font-size', '9');
    t.setAttribute('font-family', 'Inter'); t.textContent = fmtLabel(v);
    svg.append(t);
  };
  lab(max - pad / 2, padT + 8);
  lab((min + max) / 2, padT + ih / 2 + 3);
  lab(min + pad / 2, padT + ih);
  el.append(svg);
  return el;
}

/* ---------- images ---------- */
/** Downscale + JPEG (port de l'import photo Android ≤1440 px q86 / avatar 640 q88).
 *  Garde taille : décoder une photo géante (>25 Mo) coûte des centaines de Mo de RAM. */
export function downscaleToJpeg(source, maxDim = 1440, quality = 0.86) {
  if (source && source.size > 25 * 1024 * 1024) {
    return Promise.reject(new Error('image trop volumineuse (25 Mo max)'));
  }
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(source);
    const img = new Image();
    img.onload = () => {
      URL.revokeObjectURL(url);
      const scale = Math.min(1, maxDim / Math.max(img.width, img.height));
      const w = Math.round(img.width * scale), hh = Math.round(img.height * scale);
      const cv = document.createElement('canvas');
      cv.width = w; cv.height = hh;
      cv.getContext('2d').drawImage(img, 0, 0, w, hh);
      cv.toBlob(b => b ? resolve(b) : reject(new Error('encode')), 'image/jpeg', quality);
    };
    img.onerror = () => { URL.revokeObjectURL(url); reject(new Error('image')); };
    img.src = url;
  });
}

export const imgEl = (blobUrl, { fit = 'cover', radius = '0px' } = {}) =>
  h('img', { src: blobUrl, alt: '', style: { width: '100%', height: '100%', objectFit: fit, borderRadius: radius, display: 'block' } });

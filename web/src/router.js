// Navigation à pile — port de Nav (App.kt) : stack d'écrans, 3 onglets, retour
// navigateur = pop, restauration après rechargement (équivalent process-death Android).
import { store } from './store.js';
import { emit } from './store.js';

export const screens = {}; // name -> { render(el, params) }
export function registerScreen(name, mod) { screens[name] = mod; }

export const nav = {
  tab: 0,          // 0 Accueil, 1 Entraînement, 2 Profil
  stack: [],       // [{s, params}]
  get atTab() { return this.stack.length === 0; },
  get current() { return this.stack[this.stack.length - 1] || null; },
  push(s, params = null, { history = true } = {}) {
    this.stack.push({ s, params });
    if (history) pushHistory();
    render();
  },
  pop({ fromPopstate = false } = {}) {
    if (this.stack.length) this.stack.pop();
    if (!fromPopstate) history.back(); // le popstate re-rendera
    render();
  },
  toTab(i) {
    this.tab = i; this.stack = [];
    pushHistory();
    render();
  },
};

/* ---- encodage de la pile (tags comme App.kt encode/decode) ---- */
function tagOf(e) {
  const p = e.params || {};
  switch (e.s) {
    case 'homeTab': return 'home'; case 'trainingTab': return 'train'; case 'profileTab': return 'profile';
    case 'settings': return 'settings'; case 'history': return 'history'; case 'exercises': return 'ex';
    case 'logger': return 'logger'; case 'summary': return 'summary'; case 'progress': return 'progress';
    case 'workoutDetail': return `wd:${p.id}`;
    case 'exerciseDetail': return `ed:${encodeURIComponent(p.name)}`;
    case 'routineDetail': return `rd:${p.id}`;
    case 'photoViewer': return `pv:${p.id}`;
    case 'comparePhotos': return `cp:${p.aId}:${p.bId}`;
    default: return null;
  }
}
function eventOf(tag) {
  const mk = (s, params) => ({ s, params });
  if (tag === 'home') return mk('homeTab');
  if (tag === 'train') return mk('trainingTab');
  if (tag === 'profile') return mk('profileTab');
  if (tag === 'settings') return mk('settings');
  if (tag === 'history') return mk('history');
  if (tag === 'exercises') return mk('exercises');
  if (tag === 'logger') return store.draft ? mk('logger') : null;          // sans brouillon → pop (App.kt)
  if (tag === 'summary') return (store.draft && store.draft.mode === 'workout') ? mk('summary') : null;
  if (tag === 'progress') return mk('progress');
  if (tag.startsWith('wd:')) { const id = +tag.slice(3); return Number.isFinite(id) ? mk('workoutDetail', { id }) : null; }
  if (tag.startsWith('ed:')) return mk('exerciseDetail', { name: decodeURIComponent(tag.slice(3)) });
  if (tag.startsWith('rd:')) { const id = +tag.slice(3); return Number.isFinite(id) ? mk('routineDetail', { id }) : null; }
  if (tag.startsWith('pv:')) { const id = +tag.slice(3); return Number.isFinite(id) ? mk('photoViewer', { id }) : null; }
  if (tag.startsWith('cp:')) {
    const parts = tag.slice(3).split(':').map(Number);
    return parts.length === 2 && parts.every(Number.isFinite) ? mk('comparePhotos', { aId: parts[0], bId: parts[1] }) : null;
  }
  return null;
}
export function saveNav() {
  const tags = [nav.tab === 1 ? 'train' : nav.tab === 2 ? 'profile' : 'home', ...nav.stack.map(tagOf)];
  if (tags.some(t => t == null)) return;
  localStorage.setItem('lb.nav', JSON.stringify(tags));
}
export function restoreNav() {
  try {
    const tags = JSON.parse(localStorage.getItem('lb.nav') || 'null');
    if (!Array.isArray(tags) || !tags.length) return;
    const first = tags.shift();
    nav.tab = first === 'train' ? 1 : first === 'profile' ? 2 : 0;
    nav.stack = tags.map(eventOf).filter(Boolean);
  } catch {}
}

/* ---- historique navigateur ---- */
function pushHistory() {
  saveNav();
  try { history.pushState({ nav: true }, ''); } catch {}
}
let popsHandled = 0;
export function wireHistory() {
  window.addEventListener('popstate', () => {
    if (nav.stack.length) {
      nav.stack.pop();
      render();
      saveNav();
    } else {
      // au niveau onglet : retour = sortir de l'app n'est pas bloquable, on reste
      render();
    }
  });
}

/* ---- rendu ---- */
let rootEl = null;
const navListeners = new Set();
export function onNav(cb) { navListeners.add(cb); }
export function setRoot(el) { rootEl = el; }
export function render() {
  if (!rootEl) return;
  const cur = nav.current;
  // détruire les abonnements de l'écran précédent
  rootEl.querySelectorAll('[data-live]').forEach(el => el.dispatchEvent(new CustomEvent('screen-gone')));
  rootEl.replaceChildren();
  let mod, params;
  if (cur) {
    mod = screens[cur.s]; params = cur.params || {};
    if (!mod) { nav.stack.pop(); return render(); }
  } else {
    mod = [screens.homeTab, screens.trainingTab, screens.profileTab][nav.tab] || screens.homeTab;
    params = {};
  }
  const el = document.createElement('div');
  el.className = 'screen crossfade';
  el.dataset.live = '1';
  rootEl.append(el);
  mod.render(el, params);
  emit('screen', cur ? cur.s : ['homeTab', 'trainingTab', 'profileTab'][nav.tab]);
  for (const cb of navListeners) cb();
  saveNav();
}
export function rerender() { render(); }

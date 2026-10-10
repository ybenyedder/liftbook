// Shell du port web — port de MainActivity + App.kt : porte auth, onglets, barre undo,
// hôte toast, sync au démarrage/visibilité, callback OAuth Google.
import './screens/home.js';
import './screens/training.js';
import './screens/logger.js';
import './screens/exercises.js';
import './screens/history.js';
import './screens/summary.js';
import './screens/progress.js';
import './screens/profile.js';
import './screens/settings.js';
import './screens/auth.js';

import { h, icon } from './ui.js';
import * as storeMod from './store.js';
import { store } from './store.js';
import * as Cloud from './cloud.js';
import { L10n } from './search.js';
import * as navMod from './router.js';
import { restore as restRestore } from './restimer.js';

const boot = async () => {
  storeMod.init();
  applyAccent();
  navMod.restoreNav();
  navMod.wireHistory();
  restRestore();

  // callback OAuth Google : /auth-callback(.html)?code=…
  const onCallback = /\/auth-callback(\.html)?$/.test(location.pathname);
  if (onCallback) {
    await handleGoogleCallback();
  }

  const app = document.getElementById('app');
  navMod.setRoot(app.querySelector('#screen-root'));
  storeMod.on('session', renderAll);
  renderAll();

  // sync : au boot (400 ms) et au retour de visibilité (onStart Android)
  if (store.session) storeMod.requestSync(400);
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible' && store.session) storeMod.requestSync(400);
  });
  window.addEventListener('online', () => store.session && storeMod.requestSync(400));

  if ('serviceWorker' in navigator && (location.protocol === 'https:' || location.hostname === 'localhost')) {
    navigator.serviceWorker.register('sw.js').catch(() => {});
  }
};

function applyAccent() { document.getElementById('app').dataset.accent = store.settings.accent || 'blue'; }

function renderAll() {
  const app = document.getElementById('app');
  applyAccent();
  app.replaceChildren();
  if (!store.session && !store.skipped) {
    navMod.screens.auth.render(app, {});
    return;
  }
  app.append(
    h('div', { id: 'screen-root', style: { flex: '1', display: 'flex', flexDirection: 'column', overflow: 'hidden', position: 'relative' } }),
    tabBar(),
  );
  navMod.setRoot(app.querySelector('#screen-root'));
  navMod.rerender();
  renderUndo();
  syncTabBar();
  document.getElementById('app').append(h('div', { class: 'toast-host', id: 'toast-host' }));
}

/* barre d'onglets visible uniquement au niveau des onglets (App.kt : stack.size == 1) */
function syncTabBar() {
  const bar = document.querySelector('.tabbar');
  if (bar) bar.style.display = navMod.nav.atTab ? 'flex' : 'none';
}
navMod.onNav && navMod.onNav(syncTabBar);

/* ---- barre d'onglets (App.kt : Accueil / Entraînement / Profil) ---- */
function tabBar() {
  const tabs = [
    { i: 0, label: L10n.s('Home', 'Accueil', 'Inicio', 'Startseite'), ic: 'home' },
    { i: 1, label: L10n.s('Training', 'Entraînement', 'Entrenamiento', 'Training'), ic: 'fitness_center' },
    { i: 2, label: L10n.s('Profile', 'Profil', 'Perfil', 'Profil'), ic: 'person' },
  ];
  const bar = h('div', { class: 'tabbar' });
  for (const t of tabs) {
    const sel = navMod.nav.tab === t.i && navMod.nav.atTab;
    bar.append(h('button', {
      class: 'tab' + (sel ? ' sel' : ''),
      onclick: () => navMod.nav.toTab(t.i),
    }, icon(t.ic, { size: 26 }), h('span', {}, t.label)));
  }
  return bar;
}

/* ---- barre undo globale (séance supprimée, 5 s) ---- */
let undoTimer = null;
function renderUndo() {
  const existing = document.getElementById('undobar');
  if (existing) existing.remove();
  clearTimeout(undoTimer);
  if (!store.deletedUndo) return;
  const bar = h('div', {
    class: 'undobar', id: 'undobar',
  },
    h('div', { style: { flex: '1', fontSize: '14px' } }, L10n.s('Workout deleted', 'Séance supprimée')),
    h('button', {
      class: 'undo-btn',
      onclick: () => { const w = store.deletedUndo; store.deletedUndo = null; renderUndo(); if (w) storeMod.restoreWorkout(w); },
    }, L10n.s('UNDO', 'ANNULER')),
  );
  document.getElementById('app').append(bar);
  undoTimer = setTimeout(() => { store.deletedUndo = null; bar.remove(); }, 5000);
}
storeMod.on('change', () => { if (store.deletedUndo) renderUndo(); });
storeMod.on('settings', applyAccent);

/* ---- OAuth Google (PKCE) ---- */
async function handleGoogleCallback() {
  const q = new URLSearchParams(location.search);
  const code = q.get('code') || new URLSearchParams(location.search).get('__gcode');
  const verifier = localStorage.getItem('lb.pkce_verifier');
  const ts = +(localStorage.getItem('lb.pkce_ts') || 0);
  localStorage.removeItem('lb.pkce_verifier'); localStorage.removeItem('lb.pkce_ts');
  if (!code || !verifier || Date.now() - ts > 600000) {
    history.replaceState({}, '', 'index.html');
    return;
  }
  try {
    const session = await Cloud.exchangePkce(code, verifier);
    await storeMod.applySession(session);
  } catch {
    import('./ui.js').then(ui => ui.toast('Connexion Google incomplète — réessaie.'));
  }
  history.replaceState({}, '', 'index.html');
}

export async function startGoogleAuth() {
  const verifier = Cloud.randomPkceVerifier();
  const challenge = await Cloud.pkceChallenge(verifier);
  localStorage.setItem('lb.pkce_verifier', verifier);
  localStorage.setItem('lb.pkce_ts', String(Date.now()));
  const redirectTo = new URL('auth-callback', new URL(location.href.replace(/[^/]*$/, ''))).href;
  location.href = Cloud.googleAuthorizeUrl(verifier, challenge, redirectTo);
}

boot();

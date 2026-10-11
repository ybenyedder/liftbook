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
  // callback OAuth Google : /auth-callback(.html)?code=… — AVANT tout accès au DOM de la
  // page principale : auth-callback.html n'a pas le shell de index.html (#screen-root).
  // (bug préexistant : applyAccent() sur #app absent jetait un TypeError qui empêchait
  // l'échange PKCE — le login Google était cassé côté client depuis la v1.52.)
  const onCallback = /\/auth-callback(\.html)?$/.test(location.pathname);
  if (onCallback) {
    await handleGoogleCallback();
  }
  applyAccent();
  navMod.restoreNav();
  navMod.wireHistory();
  restRestore();

  const app = document.getElementById('app');
  if (app && app.querySelector('#screen-root')) navMod.setRoot(app.querySelector('#screen-root'));
  storeMod.on('session', renderAll);
  storeMod.on('session', maybeAskLocalMerge);
  storeMod.on('storage-full', () => import('./ui.js').then(ui => ui.toast(L10n.s(
    'Local storage full — export a backup and free space',
    'Stockage local plein — exporte une sauvegarde et libère de l\'espace'))));
  renderAll();

  // multi-onglets : chaque onglet réécrivait TOUT le store → l'onglet périmé effaçait la
  // séance terminée dans l'autre. On recharge depuis le disque à chaque écriture externe
  // (débondé — un touch() écrit ~6 clés d'un coup), sauf brouillon actif → avertissement.
  // Session : on suit les refresh de l'autre onglet ; un CHANGEMENT de compte ou un logout
  // impose un reload complet (le wipe de changement de compte doit s'appliquer ici aussi —
  // sinon un draft de l'onglet 1 serait synchronisé dans le compte de l'onglet 2).
  let storageDebounce = null;
  window.addEventListener('storage', (e) => {
    if (!e.key || !e.key.startsWith('lb.') || e.key === 'lb.nav' || e.key === 'lb.rest' || e.key.startsWith('lb.pkce')) return;
    if (e.key === 'lb.session') {
      let sess = null;
      try { sess = e.newValue ? JSON.parse(e.newValue) : null; } catch { /* ignore */ }
      if (sess && store.lastAccount && sess.userId !== store.lastAccount) { location.replace('index.html'); location.reload(); return; }
      if (!sess && store.session) { store.session = null; storeMod.emit('session'); return; }
      store.session = sess; // même compte : adoption des jetons rafraîchis
      return;
    }
    if (store.draft && store.draft.mode === 'workout') {
      if (!multiTabWarned) {
        multiTabWarned = true;
        import('./ui.js').then(ui => ui.toast(L10n.s(
          'Another tab modified the data — finish your workout here, then reload',
          'Un autre onglet a modifié les données — termine ta séance ici puis recharge')));
      }
      return;
    }
    clearTimeout(storageDebounce);
    storageDebounce = setTimeout(() => { storeMod.reloadFromDisk(); renderAll(); }, 150);
  });

  // sync : au boot (400 ms) et au retour de visibilité (onStart Android)
  if (store.session && !store.pendingLocalMerge) storeMod.requestSync(400);
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible' && store.session && !store.pendingLocalMerge) storeMod.requestSync(400);
  });
  window.addEventListener('online', () => store.session && !store.pendingLocalMerge && storeMod.requestSync(400));

  if ('serviceWorker' in navigator && (location.protocol === 'https:' || location.hostname === 'localhost')) {
    navigator.serviceWorker.register('sw.js').catch(() => {});
  }
};
let multiTabWarned = false;

/** Premier login avec données locales : fusionner dans le compte, ou laisser le cloud écraser.
 *  ⚠️ onDismiss : fermer le dialogue sans choisir (clic overlay) = « garder le cloud » —
 *  sinon le flag restait armé et la première synchro écrasait le local sans consentement. */
function maybeAskLocalMerge() {
  if (!store.session || !store.pendingLocalMerge) return;
  import('./ui.js').then(ui => {
    const close = ui.dialog({
      title: L10n.s('Local data found', 'Données locales trouvées'),
      message: L10n.s(
        'This browser already has workouts. Merge them into your account, or replace them with your account data?',
        'Ce navigateur contient déjà des séances. Les fusionner dans ton compte, ou les remplacer par celles du compte ?'),
      onDismiss: () => storeMod.resolvePendingLocalMerge(false),
      actions: [
        { label: L10n.s('Use account data', 'Garder le cloud'), class: 'mut', onClick: () => storeMod.resolvePendingLocalMerge(false) },
        { label: L10n.s('Merge', 'Fusionner'), class: 'acc', onClick: () => storeMod.resolvePendingLocalMerge(true) },
      ],
    });
    return close;
  });
}

function applyAccent() { const app = document.getElementById('app'); if (app) app.dataset.accent = store.settings.accent || 'blue'; }

function renderAll() {
  const app = document.getElementById('app');
  if (!app) return;
  applyAccent();
  // les listeners sub() de l'écran courant sont liés au root : le prévenir avant de le détacher
  const oldRoot = app.querySelector('#screen-root');
  if (oldRoot) { try { oldRoot.dispatchEvent(new Event('screen-gone')); } catch {} }
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
    }, h('span', { class: 'ipill' }, icon(t.ic, { size: 26 })), h('span', {}, t.label)));
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
  // ⚠️ .html EXPLICITE : le retour Google atterrit sur cette URL telle quelle. GitHub Pages
  // redirige /auth-callback → .html tout seul, mais le serveur volthost non → « Error response » 404.
  const redirectTo = new URL('auth-callback.html', new URL(location.href.replace(/[^/]*$/, ''))).href;
  location.href = Cloud.googleAuthorizeUrl(verifier, challenge, redirectTo);
}

boot().catch((e) => {
  // Un crash au boot (données corrompues, etc.) ne doit plus laisser une page blanche à vie :
  // message + possibilité de repartir de zéro, les exports manuels restent possibles via les Réglages.
  console.error(e);
  const app = document.getElementById('app');
  if (!app) return;
  app.replaceChildren();
  const box = h('div', { style: { padding: '40px 24px', textAlign: 'center', color: 'var(--mut)', fontSize: '14px', lineHeight: '1.6' } },
    h('div', { style: { fontSize: '18px', fontWeight: '800', color: 'var(--text)', marginBottom: '10px' } }, 'Liftbook'),
    L10n.s('The app failed to start (corrupted local data?).', 'L\'app n\'a pas pu démarrer (données locales corrompues ?).'),
    h('div', { style: { height: '16px' } }),
    h('button', {
      class: 'btn-primary',
      onclick: () => {
        import('./ui.js').then(ui => ui.confirmDialog({
          title: L10n.s('Reset local data?', 'Réinitialiser les données locales ?'),
          message: L10n.s('All local workouts on this browser will be erased (cloud data is untouched).',
            'Toutes les séances locales de ce navigateur seront effacées (le cloud n\'est pas touché).'),
          destructive: true,
          confirmLabel: L10n.s('Reset', 'Réinitialiser'),
          onConfirm: () => { try { localStorage.clear(); } catch {} location.reload(); },
        }));
      },
    }, L10n.s('Reset local data', 'Réinitialiser les données locales')),
  );
  app.append(box);
});

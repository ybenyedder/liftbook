// Écran de connexion — port de ui/Auth.kt (email/mot de passe + Google PKCE + skip).
import { h, icon } from '../ui.js';
import { store, applySession, persistSkip, emit } from '../store.js';
import * as Cloud from '../cloud.js';
import { L10n } from '../search.js';
import * as nav from '../router.js';

nav.registerScreen('auth', { render });

function render(el) {
  el.classList.add('scr-auth');
  if (!document.getElementById('css-auth')) {
    const st = h('style', { id: 'css-auth' });
    st.textContent = `
.scr-auth { overflow-y:auto; padding:0 24px calc(30px + env(safe-area-inset-bottom)); }
.scr-auth .logo-row { display:flex; flex-direction:column; align-items:center; padding:64px 0 26px; }
.scr-auth .logo-box { width:58px; height:58px; border-radius:15px; background:var(--accent); color:#fff; display:flex; align-items:center; justify-content:center; }
.scr-auth .wordmark { font-size:24px; font-weight:800; margin-top:12px; }
.scr-auth .tagline { color:var(--mut); font-size:13px; margin-top:2px; }
.scr-auth .title { font-size:22px; font-weight:800; margin:26px 0 4px; }
.scr-auth .pitch { color:var(--mut); font-size:13.5px; line-height:1.45; margin-bottom:20px; }
.scr-auth .fld { position:relative; margin-bottom:12px; }
.scr-auth .fld input { width:100%; height:48px; background:var(--card); border:1px solid var(--line); border-radius:12px; padding:0 44px 0 14px; font-size:15px; color:var(--text); outline:none; }
.scr-auth .fld input:focus { border-color:var(--accent); }
.scr-auth .fld .eye { position:absolute; right:6px; top:4px; }
.scr-auth .err { color:var(--red); font-size:13px; margin:2px 0 10px; line-height:1.4; }
.scr-auth .switch { text-align:center; margin-top:18px; font-size:13.5px; }
.scr-auth .skip { text-align:center; margin-top:14px; color:var(--mut); font-size:13.5px; }
.scr-auth .google-g { width:20px; height:20px; }
.scr-auth .spin { width:18px; height:18px; border:2px solid rgba(255,255,255,.3); border-top-color:#fff; border-radius:50%; animation:spin .8s linear infinite; }
@keyframes spin { to { transform:rotate(360deg); } }
`;
    document.head.append(st);
  }

  let mode = 'signin'; // signin | signup
  let showPwd = false;
  let busy = false, googlePending = false;

  const body = h('div', { class: 'scr-auth' });
  el.append(body);

  function paint() {
    const email = body.querySelector('input[type=email], input[data-k=email]');
    const pwd = body.querySelector('input[data-k=pwd]');
    const ev = email ? email.value : '';
    const pv = pwd ? pwd.value : '';
    const kids = [
      h('div', { class: 'logo-row' },
        h('div', { class: 'logo-box' }, icon('fitness_center', { size: 30 })),
        h('div', { class: 'wordmark' }, 'Liftbook'),
        h('div', { class: 'tagline' }, L10n.s('Workout tracker', 'Carnet de musculation')),
      ),
      h('div', { class: 'title' }, mode === 'signin' ? L10n.s('Welcome back', 'Content de te revoir') : L10n.s('Create account', 'Créer un compte')),
      h('div', { class: 'pitch' }, L10n.s('Your workouts and routines, synced across all your devices.', 'Tes séances et tes routines, synchronisées sur tous tes appareils.')),
      (() => {
        const inp = h('input', { type: 'email', 'data-k': 'email', placeholder: L10n.s('Email', 'Email'), value: ev, autocapitalize: 'off', autocomplete: 'email', inputmode: 'email' });
        inp.addEventListener('keydown', e => { if (e.key === 'Enter') submit(); });
        return h('div', { class: 'fld' }, inp);
      })(),
      (() => {
        const inp = h('input', { type: showPwd ? 'text' : 'password', 'data-k': 'pwd', placeholder: L10n.s('Password', 'Mot de passe'), value: pv, autocomplete: mode === 'signin' ? 'current-password' : 'new-password' });
        inp.addEventListener('keydown', e => { if (e.key === 'Enter') submit(); });
        return h('div', { class: 'fld' }, inp,
          h('button', { class: 'iconbtn eye', onclick: () => { showPwd = !showPwd; paint(); } }, icon(showPwd ? 'visibility_off' : 'visibility', { size: 20, cls: 't-mut' })));
      })(),
      errMsg ? h('div', { class: 'err' }, errMsg) : null,
      h('button', {
        class: 'btn-primary', disabled: busy,
        onclick: submit,
      }, busy ? h('span', { class: 'spin' }) : (mode === 'signin' ? L10n.s('Sign in', 'Se connecter') : L10n.s('Create account', 'Créer un compte'))),
      h('div', { class: 'divider-or' }, L10n.s('or', 'ou')),
      h('button', { class: 'btn-white', onclick: startGoogle }, googleG(), L10n.s('Continue with Google', 'Continuer avec Google')),
      googlePending ? h('div', { class: 't-sm', style: { textAlign: 'center', marginTop: '10px' } }, L10n.s('Google sign-in in progress…', 'Connexion Google en cours…')) : null,
      h('div', { class: 'switch' },
        mode === 'signin'
          ? [L10n.s("No account yet? Sign up", "Pas encore de compte ? S'inscrire"), ' — ']
          : [L10n.s('Already have an account? Sign in', 'Déjà un compte ? Se connecter'), ' — '],
        h('button', { class: 'link', onclick: () => { mode = mode === 'signin' ? 'signup' : 'signin'; errMsg = ''; paint(); } },
          mode === 'signin' ? L10n.s('Sign up', "S'inscrire") : L10n.s('Sign in', 'Se connecter')),
      ),
      h('div', { class: 'skip' },
        h('button', { class: 'link', style: { color: 'var(--mut)' }, onclick: () => { store.skipped = true; persistSkip(); emit('session'); } },
          L10n.s('Continue without an account', 'Continuer sans compte'))),
    ];
    body.replaceChildren(...kids.filter(k => k != null && k !== false));
    const e2 = body.querySelector('input[data-k=email]');
    if (e2 && document.activeElement && document.activeElement.dataset && document.activeElement.dataset.k === 'email') e2.focus();
  }

  let errMsg = '';

  async function submit() {
    const email = (body.querySelector('input[data-k=email]').value || '').trim().toLowerCase();
    const pwd = body.querySelector('input[data-k=pwd]').value || '';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { errMsg = L10n.s('Enter a valid email address', 'Entre une adresse email valide'); paint(); return; }
    if (pwd.length < 6) { errMsg = L10n.s('Password must be at least 6 characters', 'Mot de passe de 6 caractères minimum'); paint(); return; }
    errMsg = ''; busy = true; paint();
    try {
      const session = mode === 'signin' ? await Cloud.signIn(email, pwd) : await Cloud.signUp(email, pwd);
      await applySession(session);
    } catch (e) {
      errMsg = e && e.message ? e.message : 'Erreur inattendue.';
      busy = false; paint();
    }
  }

  async function startGoogle() {
    try {
      const verifier = Cloud.randomPkceVerifier();
      const challenge = await Cloud.pkceChallenge(verifier);
      localStorage.setItem('lb.pkce_verifier', verifier);
      localStorage.setItem('lb.pkce_ts', String(Date.now()));
      sessionStorage.setItem('lb.google_pending', '1');
      googlePending = true; paint();
      const redirectTo = new URL('auth-callback', location.href.replace(/[^/]*$/, '')).href;
      location.href = Cloud.googleAuthorizeUrl(verifier, challenge, redirectTo);
    } catch {
      googlePending = false; paint();
    }
  }

  if (sessionStorage.getItem('lb.google_pending')) {
    googlePending = true;
    sessionStorage.removeItem('lb.google_pending');
  }
  paint();
}

/** G multicolore — 4 paths SVG officiels. */
function googleG() {
  const ns = 'http://www.w3.org/2000/svg';
  const svg = document.createElementNS(ns, 'svg');
  svg.setAttribute('viewBox', '0 0 48 48');
  svg.setAttribute('class', 'google-g');
  const paths = [
    ['#EA4335', 'M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z'],
    ['#4285F4', 'M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z'],
    ['#FBBC05', 'M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24s.92 7.54 2.56 10.78l7.97-6.19z'],
    ['#34A853', 'M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z'],
  ];
  for (const [fill, d] of paths) {
    const p = document.createElementNS(ns, 'path');
    p.setAttribute('fill', fill); p.setAttribute('d', d);
    svg.append(p);
  }
  return svg;
}

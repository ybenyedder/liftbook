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
/* Auth.kt : Column scrollable padding h24, spacer 46, logo Row (box 58 r16 + 14 + textes) */
.scr-auth { overflow-y:auto; padding:0 24px calc(24px + env(safe-area-inset-bottom)); }
.scr-auth .logo-row { display:flex; align-items:center; gap:14px; padding:46px 0 0; }
.scr-auth .logo-box { width:58px; height:58px; border-radius:16px; background:var(--accent); color:#fff; display:flex; align-items:center; justify-content:center; flex:0 0 auto; }
.scr-auth .wordmark { font-size:24px; font-weight:800; line-height:29px; }
.scr-auth .tagline { color:var(--mut); font-size:12.5px; font-weight:500; margin-top:1px; }
.scr-auth .title { font-size:26px; font-weight:800; margin:34px 0 0; }
.scr-auth .pitch { color:var(--mut); font-size:14px; line-height:20px; margin-top:8px; }
/* AuthField = OutlinedTextField M3 : label flottant sur la bordure, surface, radius 12 */
.scr-auth .fld { position:relative; margin-top:26px; }
.scr-auth .fld:first-of-type { margin-top:26px; }
.scr-auth .fld input { width:100%; height:56px; background:var(--card); border:1px solid var(--line2); border-radius:12px; padding:20px 44px 6px 14px; font-size:16px; color:var(--text); outline:none; }
.scr-auth .fld input:focus { border-color:var(--accent); }
.scr-auth .fld label { position:absolute; top:17px; left:14px; color:var(--mut); font-size:16px; pointer-events:none; transition:all .15s; }
.scr-auth .fld input:focus + label, .scr-auth .fld input:not(:placeholder-shown) + label { top:-8px; font-size:12px; background:var(--bg); padding:0 4px; }
.scr-auth .fld input:focus + label { color:var(--accent); }
.scr-auth .fld .eye { position:absolute; right:0px; top:4px; }
.scr-auth .err { color:var(--red); font-size:13.5px; margin:10px 0 0; line-height:1.4; }
.scr-auth .submit { margin-top:20px; height:52px; width:100%; border-radius:12px; background:var(--accent); color:#fff; font-size:16px; font-weight:700; display:flex; align-items:center; justify-content:center; }
.scr-auth .submit:disabled { background:var(--card); color:var(--mut); }
.scr-auth .switch { display:flex; justify-content:center; align-items:baseline; margin-top:22px; font-size:14px; color:var(--mut); }
.scr-auth .skip { display:block; width:100%; text-align:center; margin-top:40px; color:var(--mut); font-size:14px; font-weight:500; }
.scr-auth .google-g { width:22px; height:22px; }
.scr-auth .spin { width:24px; height:24px; border:2.5px solid rgba(255,255,255,.3); border-top-color:#fff; border-radius:50%; animation:spin .8s linear infinite; }
.scr-auth .submit:disabled .spin { border-color:rgba(255,255,255,.15); border-top-color:var(--mut); }
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
        h('div', {},
          h('div', { class: 'wordmark' }, 'Liftbook'),
          h('div', { class: 'tagline' }, L10n.s('Workout tracker', 'Carnet de musculation'))),
      ),
      h('div', { class: 'title' }, mode === 'signin' ? L10n.s('Welcome back', 'Content de te revoir') : L10n.s('Create account', 'Créer un compte')),
      h('div', { class: 'pitch' }, L10n.s('Your workouts and routines, synced on all your devices.', 'Tes séances et tes routines, synchronisées sur tous tes appareils.')),
      (() => {
        const inp = h('input', { type: 'email', 'data-k': 'email', placeholder: ' ', value: ev, autocapitalize: 'off', autocomplete: 'email', inputmode: 'email' });
        inp.addEventListener('keydown', e => { if (e.key === 'Enter') submit(); });
        inp.addEventListener('input', refreshSubmit);
        return h('div', { class: 'fld', style: { marginTop: '26px' } }, inp, h('label', {}, L10n.s('Email', 'Email')));
      })(),
      (() => {
        const inp = h('input', { type: showPwd ? 'text' : 'password', 'data-k': 'pwd', placeholder: ' ', value: pv, autocomplete: mode === 'signin' ? 'current-password' : 'new-password' });
        inp.addEventListener('keydown', e => { if (e.key === 'Enter') submit(); });
        inp.addEventListener('input', refreshSubmit);
        return h('div', { class: 'fld', style: { marginTop: '12px' } }, inp, h('label', {}, L10n.s('Password', 'Mot de passe')),
          h('button', { class: 'iconbtn eye', onclick: () => { showPwd = !showPwd; paint(); } }, icon(showPwd ? 'visibility_off' : 'visibility', { size: 24, cls: 't-mut' })));
      })(),
      errMsg ? h('div', { class: 'err' }, errMsg) : null,
      h('button', {
        class: 'submit', disabled: busy || !ev.trim() || !pv,
        onclick: submit,
      }, busy ? h('span', { class: 'spin' }) : (mode === 'signin' ? L10n.s('Sign in', 'Se connecter') : L10n.s('Create account', 'Créer un compte'))),
      h('div', { class: 'divider-or', style: { margin: '18px 0' } }, L10n.s('or', 'ou')),
      h('button', { class: 'btn-white', onclick: startGoogle, style: { height: '52px', fontSize: '15.5px', fontWeight: 600, color: '#1F1F1F' } }, googleG(), L10n.s('Continue with Google', 'Continuer avec Google')),
      googlePending ? h('div', { class: 't-sm', style: { textAlign: 'center', marginTop: '12px' } }, L10n.s('Google sign-in in progress…', 'Connexion Google en cours…')) : null,
      h('div', { class: 'switch' },
        mode === 'signin'
          ? L10n.s('No account yet? ', "Pas encore de compte ? ")
          : L10n.s('Already have an account? ', 'Déjà un compte ? '),
        h('button', { class: 'link', style: { fontSize: '14px', fontWeight: 600 }, onclick: () => { mode = mode === 'signin' ? 'signup' : 'signin'; errMsg = ''; paint(); } },
          mode === 'signin' ? L10n.s('Sign up', "S'inscrire") : L10n.s('Sign in', 'Se connecter')),
      ),
      h('button', { class: 'skip', onclick: () => { store.skipped = true; persistSkip(); emit('session'); } },
        L10n.s('Continue without an account', 'Continuer sans compte')),
    ];
    body.replaceChildren(...kids.filter(k => k != null && k !== false));
    const e2 = body.querySelector('input[data-k=email]');
    if (e2 && document.activeElement && document.activeElement.dataset && document.activeElement.dataset.k === 'email') e2.focus();
  }

  let errMsg = '';

  /** canSubmit Android : bouton grisé (surface) tant qu'un champ est vide. */
  function refreshSubmit() {
    const em = body.querySelector('input[data-k=email]');
    const pw = body.querySelector('input[data-k=pwd]');
    const btn = body.querySelector('.submit');
    if (btn) btn.disabled = busy || !(em && em.value.trim()) || !(pw && pw.value);
  }

  async function submit() {
    const email = (body.querySelector('input[data-k=email]').value || '').trim().toLowerCase();
    const pwd = body.querySelector('input[data-k=pwd]').value || '';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { errMsg = L10n.s('Enter a valid email address', 'Entre une adresse email valide'); paint(); return; }
    if (pwd.length < 8) { errMsg = L10n.s('Password must be at least 8 characters', 'Mot de passe de 8 caractères minimum'); paint(); return; }
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
      const redirectTo = new URL('auth-callback.html', location.href.replace(/[^/]*$/, '')).href;
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

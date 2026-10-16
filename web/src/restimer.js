// Minuteur de repos global — port de RestTimer (Logger.kt) + RestNotifService :
// persistance après rechargement, +15/−15/Passer, chime rest_done.wav, notification
// web « Repos terminé » (permission demandée au premier timer, comme Android).
const KEY = 'lb.rest';

export const rest = {
  endAt: 0, totalMs: 0, exName: '', exMuscle: '',
};

const listeners = new Set();
export function onRest(cb) { listeners.add(cb); }
function notify() { for (const cb of listeners) cb(); }

function persist() {
  try {
    localStorage.setItem(KEY, JSON.stringify({ endAt: rest.endAt, totalMs: rest.totalMs, exName: rest.exName, exMuscle: rest.exMuscle }));
  } catch { /* quota : le timer vit en mémoire */ }
}
export function restore() {
  try {
    const v = JSON.parse(localStorage.getItem(KEY) || 'null');
    if (v && v.endAt > Date.now()) { Object.assign(rest, v); startTick(); scheduleEnd(); }
    else if (v) localStorage.removeItem(KEY);
  } catch {}
}

let ticker = null;
function startTick() {
  clearInterval(ticker);
  ticker = setInterval(() => {
    if (rest.endAt > 0) notify();
    if (rest.endAt > 0 && Date.now() >= rest.endAt + 4000) clear(); // état « terminé » affiché 4 s (Android)
  }, 250);
}

let notifAsked = false;
async function askNotifPermission() {
  if (notifAsked || !('Notification' in window) || Notification.permission !== 'default') return;
  notifAsked = true;
  try { await Notification.requestPermission(); } catch {}
}

export function start(sec, exName = '', exMuscle = '') {
  rest.endAt = Date.now() + sec * 1000;
  rest.totalMs = sec * 1000;
  rest.exName = exName; rest.exMuscle = exMuscle;
  persist(); startTick();
  askNotifPermission();
  scheduleEnd();
  notify();
}

export function plus15() { rest.endAt += 15000; rest.totalMs = Math.max(rest.totalMs + 15000, 1); persist(); scheduleEnd(); notify(); }
/** −15 impossible sur un repos fini ; jamais moins de 0,5 s restant (v1.50). */
export function minus15() {
  if (rest.endAt <= 0) return;
  const remaining = rest.endAt - Date.now();
  if (remaining <= 0) return;
  if (remaining <= 500) return;
  rest.endAt = Math.max(Date.now() + 500, rest.endAt - 15000);
  persist(); scheduleEnd(); notify();
}
export function clear() {
  rest.endAt = 0; rest.totalMs = 0;
  localStorage.removeItem(KEY);
  clearInterval(ticker); ticker = null;
  notify();
}

export const remainingMs = () => Math.max(0, rest.endAt - Date.now());
export const isOver = () => rest.endAt > 0 && Date.now() >= rest.endAt;

/* ---------- fin de repos : chime + notification ---------- */
let endTimer = null;
function scheduleEnd() {
  clearTimeout(endTimer);
  endTimer = setTimeout(async () => {
    if (rest.endAt <= 0) return;
    chime();
    if ('Notification' in window && Notification.permission === 'granted') {
      try {
        new Notification('✅ Repos terminé — à toi !', {
          body: rest.exName ? `${rest.exName} · prêt pour la prochaine série` : 'Prêt pour la prochaine série',
          tag: 'liftbook-rest',
          requireInteraction: true,
          vibrate: [90, 120, 90, 120, 250],
        });
      } catch {}
    }
  }, Math.max(0, rest.endAt - Date.now()));
}

// Onglet en arrière-plan : les setTimeout sont throttlés → on rattrape au retour visible.
if (typeof document !== 'undefined') {
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState !== 'visible') return;
    if (rest.endAt > 0 && Date.now() >= rest.endAt && Date.now() < rest.endAt + 4000) {
      // le countdown a fini pendant qu'on était ailleurs — chime immédiate
      chime();
    }
  });
  window.addEventListener('focus', () => {
    if (rest.endAt > 0 && Date.now() >= rest.endAt && Date.now() < rest.endAt + 4000) chime();
  });
}

let audioCtx = null;
export function chime() {
  try {
    audioCtx = audioCtx || new (window.AudioContext || window.webkitAudioContext)();
    if (audioCtx.state === 'suspended') audioCtx.resume();
    fetch('assets/rest_done.wav')
      .then(r => r.arrayBuffer())
      .then(b => audioCtx.decodeAudioData(b))
      .then(buf => {
        // 2 cloches à plein volume — "plus fort et plus long" (gym-proof)
        const play = (when) => {
          const src = audioCtx.createBufferSource();
          const g = audioCtx.createGain();
          g.gain.value = 1.0;
          src.buffer = buf; src.connect(g); g.connect(audioCtx.destination);
          src.start(when);
        };
        const t0 = audioCtx.currentTime;
        play(t0);
        play(t0 + 0.35);
      })
      .catch(() => { fallbackBeep(); fallbackBeep(); });
  } catch { fallbackBeep(); }
}
function fallbackBeep() {
  try {
    audioCtx = audioCtx || new (window.AudioContext || window.webkitAudioContext)();
    const o = audioCtx.createOscillator(), g = audioCtx.createGain();
    o.connect(g); g.connect(audioCtx.destination);
    o.frequency.value = 880; g.gain.value = 0.22;
    o.start();
    o.stop(audioCtx.currentTime + 0.55);
  } catch {}
}

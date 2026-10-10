// Service worker minimal : cache-first sur le shell statique, réseau pour l'API.
// ⚠️ CACHE doit être bumpé À CHAQUE release : les clients servent sinon l'ancien code
// à vie (le no-store HTTP ne s'applique pas au Cache Storage).
const CACHE = 'liftbook-v1.53';
const SHELL = [
  'index.html', 'styles.css', 'manifest.webmanifest',
  'assets/material-symbols.css', 'assets/material-symbols.woff2',
  'assets/Inter-Regular.woff2', 'assets/Inter-Medium.woff2', 'assets/Inter-SemiBold.woff2',
  'assets/Inter-Bold.woff2', 'assets/Inter-ExtraBold.woff2', 'assets/rest_done.wav',
  'src/data.js', 'src/figures.js',
];

self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(SHELL)).then(() => self.skipWaiting()));
});
self.addEventListener('activate', e => {
  e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== CACHE).map(k => caches.delete(k)))).then(() => self.clients.claim()));
});
self.addEventListener('fetch', e => {
  const url = new URL(e.request.url);
  if (url.origin !== location.origin) return; // API Supabase : jamais de cache
  if (e.request.method !== 'GET') return;
  // jamais de cache pour une URL avec query (auth-callback.html?code=… ne doit pas finir
  // en clé du Cache Storage) ni pour la page de callback OAuth
  if (url.search !== '' || url.pathname.includes('auth-callback')) {
    e.respondWith(fetch(e.request).catch(() => caches.match('index.html')));
    return;
  }
  e.respondWith(
    caches.match(e.request).then(hit => hit ||
      fetch(e.request).then(res => {
        if (res.ok && (url.pathname.includes('/src/') || url.pathname.includes('/assets/') || url.pathname.endsWith('.html') || url.pathname.endsWith('.css'))) {
          const copy = res.clone();
          caches.open(CACHE).then(c => c.put(e.request, copy));
        }
        return res;
      }).catch(() => caches.match('index.html')),
    ),
  );
});

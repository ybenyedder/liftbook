// Service worker minimal : cache-first sur le shell statique, réseau pour l'API.
const CACHE = 'liftbook-v1';
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

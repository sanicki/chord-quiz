// Bump this whenever index.html (or any precached asset) changes, so the
// new version is fetched and old caches are cleaned up.
const CACHE_VERSION = 'guitar-quiz-v1';

const PRECACHE_URLS = [
    './',
    './index.html',
    './manifest.json',
    './icons/icon-192.png',
    './icons/icon-512.png',
    './icons/icon-maskable-512.png',
    './icons/apple-touch-icon.png'
];

self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(CACHE_VERSION)
            .then((cache) => cache.addAll(PRECACHE_URLS))
            .then(() => self.skipWaiting())
    );
});

self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys()
            .then((keys) => Promise.all(
                keys.filter((key) => key !== CACHE_VERSION)
                    .map((key) => caches.delete(key))
            ))
            .then(() => self.clients.claim())
    );
});

// Cache-first for everything: this app is a single self-contained file with
// no external resources, so once it's cached there's nothing else to fetch.
// Network is only used to populate/refresh the cache.
self.addEventListener('fetch', (event) => {
    if (event.request.method !== 'GET') return;

    event.respondWith(
        caches.match(event.request).then((cached) => {
            if (cached) return cached;

            return fetch(event.request)
                .then((response) => {
                    if (response && response.status === 200 && response.type === 'basic') {
                        const responseClone = response.clone();
                        caches.open(CACHE_VERSION).then((cache) => {
                            cache.put(event.request, responseClone);
                        });
                    }
                    return response;
                })
                .catch(() => {
                    // Offline and not cached: for navigations, fall back to
                    // the cached app shell rather than showing a browser
                    // error page.
                    if (event.request.mode === 'navigate') {
                        return caches.match('./index.html');
                    }
                    return undefined;
                });
        })
    );
});

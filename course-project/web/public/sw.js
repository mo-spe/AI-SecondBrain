const CACHE_NAME = 'feynman-shell-v2';
const SHELL_FILES = ['/', '/manifest.webmanifest'];

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(CACHE_NAME).then((cache) => cache.addAll(SHELL_FILES)));
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(caches.keys().then((keys) => Promise.all(keys
    .filter((key) => key.startsWith('feynman-shell-') && key !== CACHE_NAME)
    .map((key) => caches.delete(key)))).then(() => self.clients.claim()));
});

self.addEventListener('fetch', (event) => {
  if (event.request.method !== 'GET' || event.request.url.includes('/api/')) return;
  if (new URL(event.request.url).origin !== self.location.origin) return;
  event.respondWith((async () => {
    const cache = await caches.open(CACHE_NAME);
    const fetchAndCache = async () => {
      const response = await fetch(event.request);
      if (response.ok) await cache.put(event.request, response.clone());
      return response;
    };
    // 页面HTML优先检查服务器，避免旧壳永久引用旧chunk和全局地球脚本；离线时才回退缓存。
    if (event.request.mode === 'navigate') {
      try { return await fetchAndCache(); }
      catch { return await cache.match(event.request) || await cache.match('/') || Response.error(); }
    }
    try { return await cache.match(event.request) || await fetchAndCache(); }
    catch { return Response.error(); }
  })());
});


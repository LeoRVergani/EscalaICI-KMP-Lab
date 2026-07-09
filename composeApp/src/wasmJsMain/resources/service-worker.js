const CACHE_NAME = "escala-ici-kmp-lab-v2";
const APP_SHELL = [
  "./",
  "index.html",
  "manifest.json",
  "icons/icon.svg",
  "icons/icon-192.png",
  "icons/icon-512.png",
  "icons/icon-maskable-192.png",
  "icons/icon-maskable-512.png"
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(APP_SHELL))
  );
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key)))
    )
  );
  self.clients.claim();
});

self.addEventListener("fetch", (event) => {
  if (event.request.method !== "GET") return;

  event.respondWith(
    caches.match(event.request).then((cached) => {
      if (cached) return cached;

      return fetch(event.request)
        .then((response) => {
          const copy = response.clone();
          caches.open(CACHE_NAME).then((cache) => cache.put(event.request, copy));
          return response;
        })
        .catch(() => offlineFallbackFor(event.request));
    })
  );
});

// Sem rede e sem cache para o recurso pedido: para navegacao (troca de
// pagina/reload), cai no app shell (`index.html`) em vez de mostrar o erro
// generico do navegador, permitindo reabrir o app instalado offline.
function offlineFallbackFor(request) {
  if (request.mode === "navigate") {
    return caches.match("index.html");
  }
  return caches.match(request);
}

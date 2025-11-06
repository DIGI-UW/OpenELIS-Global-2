// Define a cache name for versioning your cache
const CACHE_NAME = "my-cache-v1";

// Cache assets during the install phase
self.addEventListener("install", (event) => {
  console.log("[Service Worker] Install");
  event.waitUntil(
    caches
      .open(CACHE_NAME)
      .then((cache) => {
        return cache.addAll(["/", "/index.html", "/styles.css"]);
      })
      .then(() => self.skipWaiting()), // Skip waiting to activate new service worker immediately
  );
});

// Clean up old caches during the activate phase
self.addEventListener("activate", (event) => {
  console.log("[Service Worker] Activate");
  event.waitUntil(
    caches
      .keys()
      .then((cacheNames) => {
        return Promise.all(
          cacheNames.map((cacheName) => {
            if (cacheName !== CACHE_NAME) {
              console.log("[Service Worker] Deleting old cache:", cacheName);
              return caches.delete(cacheName);
            }
          }),
        );
      })
      .then(() => self.clients.claim()), // Take control of all clients as soon as active
  );
});

// Listen for push events and display notifications
self.addEventListener("push", (event) => {
  console.log("[Service Worker] Push Received", event);
  if (event.data) {
    const data = event.data.json();
    const notificationOptions = {
      body: data.body || "Message Received from OpenELIS",
      tag: data.external_id || "default-tag",
      icon: "images/openelis_logo.png",
    };

    event.waitUntil(
      self.registration.showNotification(
        "OpenELIS Message Received",
        notificationOptions,
      ),
    );
  }
});

// Notification click event listener
// self.addEventListener("notificationclick", (event) => {
//   console.log('Notification clicked');
//   event.notification.close(); // Close the notification popout

//   event.waitUntil(
//     clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
//       // Check if any client (tab or window) is already open
//       for (let i = 0; i < clientList.length; i++) {
//         const client = clientList[i];
//         if (client.url === 'https://www.youtube.com/' && 'focus' in client) {
//           return client.focus();
//         }
//       }

//       // If no client is open, open a new window
//       if (clients.openWindow) {
//         return clients.openWindow('https://www.youtube.com/');
//       }
//     })
//   );
// });

// Handle messages from clients
self.addEventListener("message", (event) => {
  if (event.data && event.data.type === "SKIP_WAITING") {
    self.skipWaiting();
  }
});

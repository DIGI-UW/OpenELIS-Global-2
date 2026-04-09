// Function to register the service worker
export function registerServiceWorker() {
  if ("serviceWorker" in navigator) {
    window.addEventListener("load", () => {
      const swUrl = `./service-worker.js`;

      navigator.serviceWorker
        .register(swUrl)
        .then((registration) => {
          console.log(
            "Service Worker registered with scope:",
            registration.scope,
          );
        })
        .catch((error) => {
          console.error("Service Worker registration failed:", error);
        });
    });
  }
}

// Function to unregister the service worker
export function unregisterServiceWorker() {
  if ("serviceWorker" in navigator) {
    navigator.serviceWorker.ready.then((registration) => {
      registration.unregister().then((boolean) => {
        console.log("Service Worker unregistered:", boolean);
      });
    });
  }
}

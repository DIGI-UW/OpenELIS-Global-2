export function getServiceWorkerUrl(baseUrl = import.meta.env.BASE_URL) {
  const normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl : `${baseUrl}/`;
  return `${normalizedBaseUrl}service-worker.js`;
}

// Function to register the service worker
export function registerServiceWorker() {
  // Only register service worker in production
  if (import.meta.env.MODE !== "production") {
    console.log(
      "Service Worker registration skipped in development environment",
    );
    return;
  }

  if ("serviceWorker" in navigator) {
    window.addEventListener("load", () => {
      const swUrl = getServiceWorkerUrl();

      navigator.serviceWorker
        .register(swUrl)
        .then((registration) => {
          console.log(
            "Service Worker registered with scope:",
            registration?.scope,
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

import React from "react";

const RELOAD_MARK = "oe.lazyWithRetry.reloaded";

/**
 * Records that the page was reloaded to recover a chunk, and reports whether
 * this is the first such reload in the tab. Without storage it reports false,
 * so a browser that blocks sessionStorage never enters a reload loop.
 */
function markReload() {
  try {
    if (window.sessionStorage.getItem(RELOAD_MARK)) {
      return false;
    }
    window.sessionStorage.setItem(RELOAD_MARK, "1");
    return true;
  } catch {
    return false;
  }
}

function clearReloadMark() {
  try {
    window.sessionStorage.removeItem(RELOAD_MARK);
  } catch {
    // Storage unavailable: nothing was marked.
  }
}

/**
 * Wraps `React.lazy` with retry-on-failure semantics for the dynamic
 * `import()` factory. Handles transient chunk-fetch failures — e.g.
 * Chrome's `ERR_NETWORK_CHANGED` when the browser's network state
 * flickers during a chunk request, or any single failed resource fetch
 * that leaves the lazy component permanently broken until page reload.
 *
 * Without retry, a single chunk-fetch blip crashes the route and
 * surfaces as an E2E failure: the RouteErrorBoundary catches the
 * `TypeError: Failed to fetch dynamically imported module` and shows
 * its "module could not be loaded" fallback. The retry wrapper
 * gives the browser three chances with backoff before giving up.
 *
 * Backoff is intentionally short (0.5s/1s/1.5s): the real failures
 * are transient TCP / Docker-network conditions that resolve in
 * milliseconds. Longer waits would harm real error reporting when the
 * chunk is genuinely missing (e.g., deploy mismatch).
 *
 * A tab opened before a deploy still requests the previous build's
 * hashed chunks, which no longer exist, so retrying cannot succeed.
 * When every attempt fails, the page reloads once to fetch the current
 * index.html; a second failure in the same tab reaches the error
 * boundary. A successful load clears the guard for the next deploy.
 */
export default function lazyWithRetry(factory, retries = 3, backoffMs = 500) {
  // This helper is the one legitimate wrapper around React.lazy.
  // eslint-disable-next-line local/no-raw-react-lazy
  return React.lazy(async () => {
    let lastError;
    for (let attempt = 0; attempt < retries; attempt += 1) {
      try {
        const module = await factory();
        clearReloadMark();
        return module;
      } catch (err) {
        lastError = err;
        if (attempt < retries - 1) {
          await new Promise((resolve) =>
            setTimeout(resolve, backoffMs * (attempt + 1)),
          );
        }
      }
    }
    if (markReload()) {
      window.location.reload();
      return new Promise(() => {});
    }
    throw lastError;
  });
}

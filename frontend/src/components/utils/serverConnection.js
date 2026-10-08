/**
 * How the app shell rides out an unreachable server (OGC-1442).
 *
 * A server restart, a dropped site connection or a proxy hiccup must never end
 * the page. Work that needs the server (the session check, a route's code)
 * keeps trying with growing gaps (1, 2, 4, 8, 15 s, then every 30 s, each with
 * a little jitter so a lab full of tabs does not hit a restarting server in
 * lockstep) and no limit. While anything waits, the app shows one
 * non-blocking notice; "Try now", the browser coming back online and the tab
 * becoming visible all try again at once.
 */
export const SERVER_RETRY_DELAYS_MS = [1000, 2000, 4000, 8000, 15000, 30000];

export const SERVER_RETRY_JITTER = 0.1;

export const SERVER_REQUEST_TIMEOUT_MS = 20000;

/**
 * Milliseconds to wait after the given number of consecutive failures
 * (1 for the first failure).
 */
export function serverRetryDelay(failures, random = Math.random) {
  const step = Math.min(Math.max(failures, 1), SERVER_RETRY_DELAYS_MS.length);
  const base = SERVER_RETRY_DELAYS_MS[step - 1];
  return Math.round(base * (1 + SERVER_RETRY_JITTER * (2 * random() - 1)));
}

const waits = new Set();
const listeners = new Set();
let wakeListenersInstalled = false;

const currentWait = () => {
  if (waits.size === 0) {
    return null;
  }
  let retryAt = Infinity;
  waits.forEach((wait) => {
    retryAt = Math.min(retryAt, wait.retryAt);
  });
  return { retryAt };
};

const notify = () => {
  const snapshot = currentWait();
  listeners.forEach((listener) => listener(snapshot));
};

/** Ends every pending wait so each waiting piece of work tries again now. */
export function retryServerNow() {
  waits.forEach((wait) => wait.wake?.());
}

const installWakeListeners = () => {
  if (wakeListenersInstalled || typeof window === "undefined") {
    return;
  }
  wakeListenersInstalled = true;
  window.addEventListener("online", retryServerNow);
  document.addEventListener("visibilitychange", () => {
    if (document.visibilityState === "visible") {
      retryServerNow();
    }
  });
};

/**
 * Calls `listener` with `{ retryAt }` (the soonest next attempt, as epoch
 * milliseconds; at or before now while an attempt is running) whenever
 * something is waiting for the server, and with null once nothing is.
 * Returns the unsubscribe function.
 */
export function subscribeServerWait(listener) {
  listeners.add(listener);
  listener(currentWait());
  return () => {
    listeners.delete(listener);
  };
}

/**
 * One piece of work waiting for the server. `wait(ms)` resolves after `ms` or
 * as soon as something asks to try again now; between waits the work counts
 * as attempting. `done()` must be called once the work stops waiting, however
 * it ends.
 */
export function createServerRetrier() {
  const entry = { retryAt: Date.now(), wake: null };
  installWakeListeners();
  return {
    wait(ms) {
      return new Promise((resolve) => {
        const finish = () => {
          clearTimeout(timer);
          entry.wake = null;
          entry.retryAt = Date.now();
          notify();
          resolve();
        };
        const timer = setTimeout(finish, ms);
        entry.wake = finish;
        entry.retryAt = Date.now() + ms;
        waits.add(entry);
        notify();
      });
    },
    wake() {
      entry.wake?.();
    },
    done() {
      entry.wake = null;
      if (waits.delete(entry)) {
        notify();
      }
    },
  };
}

const fetchWithTimeout = async (url, options, timeoutMs) => {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(url, { ...options, signal: controller.signal });
  } finally {
    clearTimeout(timer);
  }
};

/**
 * Reads a session response as a final answer, or throws so the caller tries
 * again. Only the server itself can answer: a 200 carrying the session, or a
 * 401/403 saying the user is not signed in. A network error, a timeout, a 404
 * from a context still starting, a 5xx from the proxy or a body that is not
 * the session (a page served while Tomcat starts) all mean the server was not
 * reached.
 */
export async function readSessionResponse(response) {
  if (response.status === 401 || response.status === 403) {
    return { authenticated: false };
  }
  if (response.status !== 200) {
    throw new Error(`The session check answered HTTP ${response.status}`);
  }
  const details = await response.json();
  if (!details || typeof details.authenticated !== "boolean") {
    throw new Error("The session check answered without a session");
  }
  return details;
}

/** Fetches the session once; a request held open past the timeout fails. */
export async function requestSession(
  url,
  timeoutMs = SERVER_REQUEST_TIMEOUT_MS,
) {
  const response = await fetchWithTimeout(
    url,
    { credentials: "include" },
    timeoutMs,
  );
  return readSessionResponse(response);
}

/**
 * True when the server hands out the app's start page, so a missing route
 * chunk is a stale build rather than an outage.
 */
export async function appShellReachable(timeoutMs = SERVER_REQUEST_TIMEOUT_MS) {
  try {
    const response = await fetchWithTimeout(
      window.location.origin + "/",
      { cache: "no-store", credentials: "same-origin" },
      timeoutMs,
    );
    return response.ok;
  } catch {
    return false;
  }
}

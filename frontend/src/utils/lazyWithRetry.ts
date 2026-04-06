import { lazy, ComponentType } from "react";

type LazyFactory<T extends ComponentType<any>> = () => Promise<{
  default: T;
}>;

const MAX_RETRIES = 3;
const RETRY_DELAY_MS = 1500;

/**
 * Detects chunk-load / network errors that are recoverable by retrying.
 * Webpack names these `ChunkLoadError`; the regex covers other bundlers
 * and generic fetch failures.
 */
function isChunkLoadError(error: unknown): boolean {
  if (error instanceof Error) {
    if (error.name === "ChunkLoadError") return true;
    if (/loading.+chunk|failed.+fetch|network.+error/i.test(error.message))
      return true;
  }
  return false;
}

/**
 * Wraps React.lazy with retry logic for chunk load failures.
 * Handles the case where a deployment happens while a user has the app open,
 * causing stale chunk URLs to 404.
 *
 * Recovery strategy:
 * 1. Retry the import up to MAX_RETRIES times with a delay
 * 2. If retries exhaust, do a one-time hard reload (session-guarded)
 * 3. If already reloaded, throw to let the error boundary handle it
 *
 * Non-chunk errors (syntax, missing module) propagate immediately.
 */
export default function lazyWithRetry<T extends ComponentType<any>>(
  factory: LazyFactory<T>,
) {
  return lazy(() => retryImport(factory, MAX_RETRIES));
}

function retryImport<T extends ComponentType<any>>(
  factory: LazyFactory<T>,
  retriesLeft: number,
): Promise<{ default: T }> {
  return factory()
    .then((module) => {
      // Success — clear any reload flag for this factory
      try {
        const key = sessionStorageKey(factory);
        sessionStorage.removeItem(key);
      } catch {
        // sessionStorage may be unavailable (private browsing)
      }
      return module;
    })
    .catch((error: unknown) => {
      // Non-chunk errors propagate immediately — no retries
      if (!isChunkLoadError(error)) {
        throw error;
      }

      // Retry with delay if attempts remain
      if (retriesLeft > 0) {
        return new Promise<{ default: T }>((resolve) => {
          setTimeout(() => {
            resolve(retryImport(factory, retriesLeft - 1));
          }, RETRY_DELAY_MS);
        });
      }

      // Retries exhausted — try a one-time hard reload
      try {
        const key = sessionStorageKey(factory);
        if (!sessionStorage.getItem(key)) {
          sessionStorage.setItem(key, "true");
          window.location.reload();
          // Return a never-resolving promise — the page is reloading
          return new Promise<{ default: T }>(() => {});
        }
      } catch {
        // sessionStorage unavailable — fall through to throw
      }

      // Already reloaded once (or storage unavailable) — give up
      throw error;
    });
}

function sessionStorageKey<T extends ComponentType<any>>(
  factory: LazyFactory<T>,
): string {
  return `lazyRetry:${factory.toString().slice(0, 100)}`;
}

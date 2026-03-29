import { lazy, ComponentType } from "react";

type LazyFactory<T extends ComponentType<any>> = () => Promise<{
  default: T;
}>;

const MAX_RETRIES = 3;
const RETRY_DELAY_MS = 1500;

/**
 * Wraps React.lazy with retry logic for chunk load failures.
 * Handles the case where a deployment happens while a user has the app open,
 * causing stale chunk URLs to 404.
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
  return factory().catch((error: Error) => {
    if (retriesLeft <= 0) {
      throw error;
    }

    return new Promise<{ default: T }>((resolve) => {
      setTimeout(() => {
        resolve(retryImport(factory, retriesLeft - 1));
      }, RETRY_DELAY_MS);
    });
  });
}

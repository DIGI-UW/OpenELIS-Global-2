import { QueryClient } from "@tanstack/react-query";
import { fetchFromOpenElisServer } from "./Utils";

/**
 * Reads the same server endpoints through the shared request utility, retaining
 * the application's session credentials and locale headers.
 *
 * The legacy callback helper intentionally preserves its historical response
 * behavior. Query reads use the promise helper so HTTP failures reject and
 * reach the screen's retry state rather than being cached as response data.
 */
export const fetchFromServer = <T>(endPoint: string, signal?: AbortSignal) =>
  fetchFromOpenElisServer<T>(endPoint, signal);

/**
 * The endpoint is the key, so a screen invalidates exactly what it read and
 * anything sharing a prefix. Keys are arrays for that reason: ["users", url]
 * can be invalidated whole by ["users"].
 */
export const serverQuery = <T>(key: unknown[], endPoint: string) => ({
  queryKey: key,
  queryFn: ({ signal }: { signal?: AbortSignal }) =>
    fetchFromServer<T>(endPoint, signal),
});

/**
 * Refetching is explicit here: a write invalidates what it changed. Retries and
 * focus-refetching stay off so screens behave as they were written to, and a
 * failed read surfaces rather than being retried silently.
 */
export const createQueryClient = (): QueryClient =>
  new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        refetchOnWindowFocus: false,
        refetchOnReconnect: false,
        staleTime: 0,
      },
    },
  });

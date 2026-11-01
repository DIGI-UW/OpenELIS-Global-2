import { useCallback, useEffect, useState } from "react";
import { getAnalyzer } from "../../../services/analyzerService";
import type { Analyzer } from "../types";

export type ConnectionState =
  | "checking"
  | "unknown"
  | "unavailable"
  | "missing"
  | "attention"
  | "stopped"
  | "running";

export const expectsReception = (analyzer: Analyzer) =>
  analyzer.status === "ACTIVE" || analyzer.status === "VALIDATION";

const observedState = (analyzer: Analyzer | undefined): ConnectionState => {
  if (!analyzer) return "unknown";
  if (!analyzer.connected || !analyzer.connection) return "unavailable";
  if (!analyzer.connection.readiness.ready) return "attention";
  return analyzer.connection.actualRuntimeState === "ACTIVE"
    ? "running"
    : "stopped";
};

/** Read Bridge evidence separately: a saved connection ID is not a live route. */
export const useAnalyzerConnections = (analyzers: Analyzer[]) => {
  const [observations, setObservations] = useState<
    Record<string, ConnectionState>
  >({});

  const [refreshVersion, setRefreshVersion] = useState(0);
  const refreshConnections = useCallback((analyzerId?: string | null) => {
    setObservations((previous) =>
      Object.fromEntries(
        Object.entries(previous).filter(
          ([key]) => analyzerId && !key.startsWith(`${analyzerId}:`),
        ),
      ),
    );
    setRefreshVersion((version) => version + 1);
  }, []);

  const candidatesKey = JSON.stringify(
    analyzers
      .filter((analyzer) => analyzer.id && analyzer.bridgeConnectionId)
      .map((analyzer) => [analyzer.id!, analyzer.bridgeConnectionId!])
      .sort(([left], [right]) => left.localeCompare(right)),
  );

  useEffect(() => {
    const controller = new AbortController();
    let timer: ReturnType<typeof setTimeout>;
    let refreshing = false;
    let lastStarted = -Infinity;
    const candidates: Array<[string, string]> = JSON.parse(candidatesKey);
    const keys = new Set(
      candidates.map(([id, connectionId]) => `${id}:${connectionId}`),
    );
    setObservations((previous) =>
      Object.fromEntries(
        Object.entries(previous).filter(([key]) => keys.has(key)),
      ),
    );

    const refresh = async () => {
      if (refreshing || controller.signal.aborted || document.hidden) return;
      const remaining = 30_000 - (Date.now() - lastStarted);
      if (remaining > 0) {
        clearTimeout(timer);
        timer = setTimeout(refresh, remaining);
        return;
      }
      lastStarted = Date.now();
      refreshing = true;
      clearTimeout(timer);
      let next = 0;
      const worker = async () => {
        while (
          next < candidates.length &&
          !controller.signal.aborted &&
          !document.hidden
        ) {
          const [id, connectionId] = candidates[next++];
          await new Promise<void>((resolve) => {
            const request = new AbortController();
            let settled = false;
            const cancel = () => {
              settled = true;
              request.abort();
              clearTimeout(deadline);
              resolve();
            };
            const finish = (state: ConnectionState) => {
              if (settled) return;
              settled = true;
              clearTimeout(deadline);
              controller.signal.removeEventListener("abort", cancel);
              if (!controller.signal.aborted) {
                setObservations((previous) => ({
                  ...previous,
                  [`${id}:${connectionId}`]: state,
                }));
              }
              resolve();
            };
            const deadline = setTimeout(() => {
              request.abort();
              finish("unknown");
            }, 15_000);
            controller.signal.addEventListener("abort", cancel, { once: true });
            getAnalyzer(
              id,
              (detail) => finish(observedState(detail)),
              request.signal,
            );
          });
        }
      };
      // Limit concurrent Bridge lookups; a slow Bridge must not block the list.
      await Promise.all(Array.from({ length: 4 }, worker));
      refreshing = false;
      if (!controller.signal.aborted && !document.hidden)
        timer = setTimeout(refresh, 30_000);
    };
    void refresh();
    const visibilityChanged = () => {
      clearTimeout(timer);
      if (!document.hidden) void refresh();
    };
    window.addEventListener("focus", refresh);
    document.addEventListener("visibilitychange", visibilityChanged);
    return () => {
      controller.abort();
      clearTimeout(timer);
      window.removeEventListener("focus", refresh);
      document.removeEventListener("visibilitychange", visibilityChanged);
    };
  }, [candidatesKey, refreshVersion]);

  const connectionState = (analyzer: Analyzer): ConnectionState =>
    !analyzer.bridgeConnectionId
      ? "missing"
      : observations[`${analyzer.id}:${analyzer.bridgeConnectionId}`] ||
        "checking";

  return { connectionState, refreshConnections };
};

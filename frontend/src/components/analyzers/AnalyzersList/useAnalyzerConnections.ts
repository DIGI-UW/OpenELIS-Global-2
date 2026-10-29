import { useEffect, useState } from "react";
import { getAnalyzer } from "../../../services/analyzerService";
import type { Analyzer } from "../types";

export type ConnectionState =
  | "checking"
  | "unavailable"
  | "missing"
  | "attention"
  | "stopped"
  | "running";

export const expectsReception = (analyzer: Analyzer) =>
  analyzer.status === "ACTIVE" || analyzer.status === "VALIDATION";

const observedState = (analyzer: Analyzer | undefined): ConnectionState => {
  if (!analyzer?.connected || !analyzer.connection) return "unavailable";
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

  useEffect(() => {
    const controller = new AbortController();
    let timer: ReturnType<typeof setTimeout>;
    let refreshing = false;
    setObservations({});
    const candidates = analyzers.filter(
      (analyzer) => analyzer.id && analyzer.bridgeConnectionId,
    );

    const refresh = async () => {
      if (refreshing || controller.signal.aborted) return;
      refreshing = true;
      clearTimeout(timer);
      let next = 0;
      const worker = async () => {
        while (next < candidates.length && !controller.signal.aborted) {
          const analyzer = candidates[next++];
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
                  [analyzer.id!]: state,
                }));
              }
              resolve();
            };
            const deadline = setTimeout(() => {
              request.abort();
              finish("unavailable");
            }, 15_000);
            controller.signal.addEventListener("abort", cancel, { once: true });
            getAnalyzer(
              analyzer.id!,
              (detail) => finish(observedState(detail)),
              request.signal,
            );
          });
        }
      };
      // Limit concurrent Bridge lookups; a slow Bridge must not block the list.
      await Promise.all(Array.from({ length: 4 }, worker));
      refreshing = false;
      if (!controller.signal.aborted) timer = setTimeout(refresh, 30_000);
    };
    void refresh();
    window.addEventListener("focus", refresh);
    return () => {
      controller.abort();
      clearTimeout(timer);
      window.removeEventListener("focus", refresh);
    };
  }, [analyzers]);

  return (analyzer: Analyzer): ConnectionState =>
    !analyzer.bridgeConnectionId
      ? "missing"
      : observations[analyzer.id!] || "checking";
};

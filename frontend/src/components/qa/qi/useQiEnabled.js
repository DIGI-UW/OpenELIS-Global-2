import { useEffect, useState } from "react";
import { getFromOpenElisServer } from "../../utils/Utils";

/**
 * Resolves the enabled flag for one or more QI indicators from qi_config
 * (GET /rest/qi-config/resolve, gated qa.view.qi). Returns an `isEnabled(key)`
 * predicate. Fail-open: an indicator reads as enabled unless resolve explicitly
 * returns enabled === false (a failed/absent config fetch never hides a tile).
 *
 * Shared by the QI-indicator surfaces on the QA Overview so the disable cascade
 * (OGC-711) reaches them the same way it reaches the QI Dashboard.
 */
export default function useQiEnabled(indicators) {
  const [enabledMap, setEnabledMap] = useState({});

  useEffect(() => {
    let mounted = true;
    indicators.forEach((key) =>
      getFromOpenElisServer(
        `/rest/qi-config/resolve?indicator=${key}`,
        (res) => {
          if (mounted && res && typeof res.enabled === "boolean") {
            setEnabledMap((m) => ({ ...m, [key]: res.enabled }));
          }
        },
      ),
    );
    return () => {
      mounted = false;
    };
    // indicators is a stable literal at each call site; resolve once on mount.
  }, []);

  return (key) => enabledMap[key] !== false;
}

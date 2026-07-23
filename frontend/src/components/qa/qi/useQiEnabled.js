import { useCallback, useEffect, useRef, useState } from "react";
import { getFromOpenElisServer } from "../../utils/Utils";

/**
 * Resolves the enabled flag for one or more QI indicators from qi_config
 * (GET /rest/qi-config/resolve, gated qa.view.qi). Returns:
 *   - isEnabled(key): predicate, fail-open — an indicator reads as enabled
 *     unless resolve explicitly returns enabled === false (a failed/absent
 *     config fetch never hides a surface).
 *   - refetch(): re-resolve on demand (e.g. a dashboard Refresh button).
 *
 * Single source of truth for the OGC-711 disable cascade across the QI
 * Dashboard and the QA Overview, so the surfaces can't drift apart.
 */
export default function useQiEnabled(indicators) {
  const [enabledMap, setEnabledMap] = useState({});
  // Capture the indicator list once. Call sites pass a stable set (a module
  // const or a fixed literal), so refetch can read it from the ref without
  // making the mount effect depend on the array's identity (which would re-run
  // every render for an inline literal).
  const indicatorsRef = useRef(indicators);
  const mountedRef = useRef(true);

  const refetch = useCallback(() => {
    indicatorsRef.current.forEach((key) =>
      getFromOpenElisServer(
        `/rest/qi-config/resolve?indicator=${key}`,
        (res) => {
          if (mountedRef.current && res && typeof res.enabled === "boolean") {
            setEnabledMap((m) => ({ ...m, [key]: res.enabled }));
          }
        },
      ),
    );
  }, []);

  useEffect(() => {
    mountedRef.current = true;
    refetch();
    return () => {
      mountedRef.current = false;
    };
  }, [refetch]);

  const isEnabled = (key) => enabledMap[key] !== false;
  return { isEnabled, refetch };
}

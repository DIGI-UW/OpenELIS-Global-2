import { useServerData } from "../../utils/useServerData";

/**
 * Resolves one QI indicator's qi_config (GET /rest/qi-config/resolve, gated
 * qa.view.qi) and returns:
 *   - enabled: fail-open — an indicator reads as enabled unless resolve
 *     explicitly returns enabled === false, so a failed or absent config fetch
 *     never hides a surface.
 *   - config: the resolved {enabled, target, action, direction}, or undefined
 *     while loading / on fetch failure (consumers fall back to their unthemed
 *     rendering, mirroring the fail-open contract).
 *
 * Single source of truth for the OGC-711 disable cascade and the OGC-710
 * threshold consumption across the QI Dashboard, the detail pages and the QA
 * Overview, so the surfaces can't drift apart. Reads share the server-data
 * cache, so a route guard and the page it wraps resolve the same indicator
 * from one entry.
 */

export const qiResolveUrl = (indicator) =>
  `/rest/qi-config/resolve?indicator=${indicator}`;

export default function useQiConfig(indicator) {
  const query = useServerData(qiResolveUrl(indicator));
  const config =
    typeof query.data?.enabled === "boolean" ? query.data : undefined;
  return {
    enabled: config?.enabled !== false,
    config,
    loading: query.isLoading,
  };
}

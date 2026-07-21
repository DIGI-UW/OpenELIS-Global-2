import React, { useContext } from "react";
import { Redirect } from "react-router-dom";
import { ConfigurationContext } from "../../layout/Layout";
import UnifiedResults from "./UnifiedResults";

/**
 * OGC-1020 (R1) — route consolidation behind the `results.entry.unifiedRoute`
 * site flag.
 *
 * Flag ON: the legacy result-entry routes redirect to the canonical /Results
 * worklist. Flag OFF (default): legacy routes behave exactly as today and
 * /Results redirects back to the legacy page, so the new surface is
 * unreachable until a site opts in.
 */

export function useUnifiedResultsEnabled(): boolean {
  const { configurationProperties } = useContext(ConfigurationContext) as {
    configurationProperties?: Record<string, string>;
  };
  return configurationProperties?.RESULTS_ENTRY_UNIFIED_ROUTE === "true";
}

/** The /Results route: the unified worklist, or legacy when the flag is off. */
export const UnifiedResultsRoute: React.FC = () => {
  return useUnifiedResultsEnabled() ? (
    <UnifiedResults />
  ) : (
    <Redirect to="/result?type=unit&doRange=false" />
  );
};

/**
 * Wraps a legacy results route: redirects to /Results when the flag is on,
 * renders the legacy page otherwise.
 */
export const LegacyResultsGate: React.FC<{ children: React.ReactElement }> = ({
  children,
}) => {
  return useUnifiedResultsEnabled() ? <Redirect to="/Results" /> : children;
};

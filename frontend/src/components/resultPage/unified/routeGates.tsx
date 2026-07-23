import React, { useContext } from "react";
import { Redirect } from "react-router-dom";
import { Loading } from "@carbon/react";
import { ConfigurationContext } from "../../layout/Layout";
import UnifiedResults from "./UnifiedResults";

/**
 * OGC-1020 (R1) — route consolidation behind the `resultsEntryUnifiedRoute`
 * site flag.
 *
 * Flag ON: the legacy result-entry routes redirect to the canonical /Results
 * worklist. Flag OFF (default): legacy routes behave exactly as today and
 * /Results redirects back to the legacy page, so the new surface is
 * unreachable until a site opts in.
 *
 * IMPORTANT: on a fresh page load the configuration context is briefly an
 * empty object. Redirecting on that transient state bounced users between the
 * two route families and dropped their URL params (blank page on refresh /
 * deep link) — so both gates HOLD with a spinner until the flag's real value
 * is known.
 */

type FlagState = "on" | "off" | "loading";

function useUnifiedResultsFlag(): FlagState {
  const { configurationProperties } = useContext(ConfigurationContext) as {
    configurationProperties?: Record<string, string | null>;
  };
  // The context fills in two waves (open properties pre-auth, then the
  // authenticated set that carries this flag) — only the KEY's presence
  // proves the flag's real value is known. A present-but-null value means
  // the site_information row is missing: treat as off (legacy).
  if (
    !configurationProperties ||
    !("RESULTS_ENTRY_UNIFIED_ROUTE" in configurationProperties)
  ) {
    return "loading";
  }
  return configurationProperties.RESULTS_ENTRY_UNIFIED_ROUTE === "true"
    ? "on"
    : "off";
}

/** The /Results route: the unified worklist, or legacy when the flag is off. */
export const UnifiedResultsRoute: React.FC = () => {
  const flag = useUnifiedResultsFlag();
  if (flag === "loading") {
    return <Loading description="" withOverlay={false} />;
  }
  return flag === "on" ? (
    <UnifiedResults />
  ) : (
    <Redirect to="/result?type=unit&doRange=false" />
  );
};

/**
 * Wraps a legacy results route: redirects to /Results when the flag is on,
 * renders the legacy page otherwise. An accessionNumber on the legacy URL
 * (e.g. the in-progress dashboard's /result?type=order&accessionNumber=X
 * links) is carried through so the unified page loads that order directly.
 */
export const LegacyResultsGate: React.FC<{ children: React.ReactElement }> = ({
  children,
}) => {
  const flag = useUnifiedResultsFlag();
  if (flag === "loading") {
    return <Loading description="" withOverlay={false} />;
  }
  if (flag === "off") {
    return children;
  }
  const accession = new URLSearchParams(window.location.search).get(
    "accessionNumber",
  );
  return (
    <Redirect
      to={
        accession
          ? `/Results?accessionNumber=${encodeURIComponent(accession)}`
          : "/Results"
      }
    />
  );
};

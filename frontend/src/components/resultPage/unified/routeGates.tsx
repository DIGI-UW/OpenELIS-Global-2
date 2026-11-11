import React from "react";
import { Redirect, useHistory, useLocation } from "react-router-dom";
import UnifiedResults from "./UnifiedResults";
import RunView from "./runs/RunView";
import { RunLocationState, hadWorklistFilters } from "./runs/runFormat";

/**
 * /Results is the one results-entry page. The legacy result-entry routes
 * (/result, /LogbookResults, /PatientResults, /AccessionResults,
 * /StatusResults, /RangeResults) are kept only as addresses: bookmarks and
 * links into them land on /Results, carrying an accessionNumber or patientId
 * so the page loads that order or patient directly.
 */
export const UnifiedResultsRoute: React.FC = () => {
  const location = useLocation<RunLocationState | undefined>();
  const history = useHistory<RunLocationState>();
  const runParam = new URLSearchParams(location.search).get("run");
  const runId = runParam && /^\d+$/.test(runParam) ? Number(runParam) : null;

  // OGC-1200 FR-E1b: a run replaces the worklist's filters, and leaving it
  // brings back the worklist exactly as it was.
  const openRun = (id: number) => {
    const returnTo = runId
      ? location.state?.returnTo
      : window.location.search || "";
    history.push({
      pathname: "/Results",
      search: `?run=${id}`,
      state: {
        returnTo,
        filtersReplaced: runId
          ? location.state?.filtersReplaced
          : hadWorklistFilters(window.location.search),
      },
    });
  };

  if (runId) {
    return (
      <RunView
        key={runId}
        runId={runId}
        filtersReplaced={Boolean(location.state?.filtersReplaced)}
        onOpenRun={openRun}
        onBack={() => history.push(`/Results${location.state?.returnTo || ""}`)}
      />
    );
  }
  return <UnifiedResults onOpenRun={openRun} />;
};

export const legacyResultsRedirectTarget = (search: string): string => {
  const legacyParams = new URLSearchParams(search);
  const forwarded = new URLSearchParams();
  for (const key of ["accessionNumber", "patientId"]) {
    const value = legacyParams.get(key);
    if (value) {
      forwarded.set(key, value);
    }
  }
  const query = forwarded.toString();
  return query ? `/Results?${query}` : "/Results";
};

export const LegacyResultsRedirect: React.FC = () => (
  <Redirect to={legacyResultsRedirectTarget(window.location.search)} />
);

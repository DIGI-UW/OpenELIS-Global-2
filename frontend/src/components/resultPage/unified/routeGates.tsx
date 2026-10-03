import React from "react";
import { Redirect } from "react-router-dom";
import UnifiedResults from "./UnifiedResults";

/**
 * /Results is the one results-entry page. The legacy result-entry routes
 * (/result, /LogbookResults, /PatientResults, /AccessionResults,
 * /StatusResults, /RangeResults) are kept only as addresses: bookmarks and
 * links into them land on /Results, carrying an accessionNumber or patientId
 * so the page loads that order or patient directly.
 */
export const UnifiedResultsRoute: React.FC = () => <UnifiedResults />;

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

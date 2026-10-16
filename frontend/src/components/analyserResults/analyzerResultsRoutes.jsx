import React from "react";
import { Redirect, useLocation } from "react-router-dom";

/**
 * Where an analyzer's review worklist and the import issues live. Both sit
 * under /analyzers so the side nav keeps "Analyzers" selected while the
 * reviewer works there.
 */
export const analyzerResultsPath = (analyzerId) =>
  analyzerId
    ? `/analyzers/${encodeURIComponent(analyzerId)}/results`
    : "/analyzers";

export const importIssuesPath = "/analyzers/import-issues";

/**
 * The route these pages used to have, /AnalyzerResults?id=… and
 * /AnalyzerResults?view=import-issues, kept for bookmarks and links from
 * older builds. It sends the browser to the page's current address.
 */
export const LegacyAnalyzerResultsRedirect = () => {
  const { search } = useLocation();
  const query = new URLSearchParams(search);
  if (query.get("view") === "import-issues") {
    return <Redirect to={importIssuesPath} />;
  }
  return <Redirect to={analyzerResultsPath(query.get("id"))} />;
};

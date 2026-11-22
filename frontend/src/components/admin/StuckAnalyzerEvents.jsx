import React from "react";
import PageBreadCrumb from "../common/PageBreadCrumb";
import ImportIssuesPanel from "../analyserResults/ImportIssuesPanel";

/**
 * The analyzer's stuck import events, inside the admin shell. The same panel
 * is reachable from the Analyzers area at /AnalyzerResults?view=import-issues;
 * this route exists so that opening it from the admin menu keeps the admin
 * navigation in place instead of dropping the reader back to the main one.
 */
export default function StuckAnalyzerEvents({ basePath = "/MasterListsPage" }) {
  return (
    <>
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/" },
          { label: "breadcrums.admin.managment", link: basePath },
          {
            label: "analyzer.importIssues.events.title",
            link: `${basePath}/stuckAnalyzerEvents`,
          },
        ]}
      />
      <ImportIssuesPanel />
    </>
  );
}

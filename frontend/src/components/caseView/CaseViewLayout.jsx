import React from "react";
import { Grid, Column } from "@carbon/react";
import "./caseView.scss";

const TOTAL_SPAN = 16;
const RAIL_SPAN = 3;
const SUMMARY_SPAN = 4;

/**
 * The Workbench grid the anatomic-pathology case views share: an optional
 * progress rail on the left, the work sections in the centre, a sticky case
 * summary on the right, and the action bar in its own full-width row below.
 *
 * A view with fewer than five gated sections has no rail, and an early view
 * may have no summary yet either. Reserving a blank column for either one
 * would leave a gap only a sighted user notices and nobody can explain, so
 * the centre column's span is computed from which of its neighbours are
 * actually present rather than the screen choosing between a fixed set of
 * three-column, two-column and one-column layouts by hand.
 *
 * The grid is drawn on a surface of its own. The application's content area
 * is a light grey inside a Carbon white theme zone, where a field and a tile
 * are the same light grey, so a form laid straight onto the page has inputs
 * nobody can see until one is focused; every other form screen in this
 * application sits on a white body card for the same reason.
 */
const CaseViewLayout = ({
  rail = null,
  summary = null,
  actionBar = null,
  children,
}) => {
  const railSpan = rail ? RAIL_SPAN : 0;
  const summarySpan = summary ? SUMMARY_SPAN : 0;
  const centreSpan = TOTAL_SPAN - railSpan - summarySpan;

  return (
    <Grid fullWidth className="case-view__layout">
      {rail && (
        <Column lg={railSpan} md={8} sm={4}>
          {rail}
        </Column>
      )}
      <Column lg={centreSpan} md={8} sm={4}>
        {children}
      </Column>
      {summary && (
        <Column lg={summarySpan} md={8} sm={4}>
          {summary}
        </Column>
      )}
      {actionBar && (
        <Column lg={16} md={8} sm={4}>
          {actionBar}
        </Column>
      )}
    </Grid>
  );
};

export default CaseViewLayout;

import React from "react";
import { Grid, Column } from "@carbon/react";
import { FormattedMessage } from "react-intl";

export default function QCDashboardPlaceholder() {
  return (
    <div data-testid="qc-dashboard-placeholder">
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <h2>
            <FormattedMessage id="analyzer.qc.placeholder.title" />
          </h2>
          <p>
            <FormattedMessage id="analyzer.qc.placeholder.message" />
          </p>
        </Column>
      </Grid>
    </div>
  );
}

import React from "react";
import { Grid, Column } from "@carbon/react";
import { FormattedMessage } from "react-intl";

export default function QCAlertsPlaceholder() {
  return (
    <div data-testid="qc-alerts-placeholder">
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <h2>
            <FormattedMessage id="analyzer.qc.alerts.title" />
          </h2>
          <p>
            <FormattedMessage id="analyzer.qc.alerts.message" />
          </p>
        </Column>
      </Grid>
    </div>
  );
}

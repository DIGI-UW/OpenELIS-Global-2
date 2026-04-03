import React from "react";
import { Grid, Column } from "@carbon/react";
import { FormattedMessage } from "react-intl";

export default function CorrectiveActionsPlaceholder() {
  return (
    <div data-testid="qc-corrective-actions-placeholder">
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <h2>
            <FormattedMessage id="analyzer.qc.corrective.title" />
          </h2>
          <p>
            <FormattedMessage id="analyzer.qc.corrective.message" />
          </p>
        </Column>
      </Grid>
    </div>
  );
}

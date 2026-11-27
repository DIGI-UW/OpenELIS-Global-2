import React from "react";
import { Button, InlineNotification } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory } from "react-router-dom";

/**
 * Shown after an order is saved for a patient whose sex or birth date is not
 * recorded: the tests whose reference range depends on the missing value, so
 * their results will not be flagged and will wait for review. It never blocks
 * the order; "Report NCE" opens the non-conformity form on this order.
 */
const RangeNotAppliedWarning = ({ tests, labNumber }) => {
  const intl = useIntl();
  const history = useHistory();
  if (!Array.isArray(tests) || tests.length === 0) {
    return null;
  }
  const subtitle = intl.formatMessage(
    { id: "order.rangeNotApplied.warning.subtitle" },
    { tests: tests.join(", ") },
  );
  const reportNce = () => {
    const params = new URLSearchParams();
    if (labNumber) {
      params.set("labNumber", labNumber);
    }
    params.set("description", subtitle);
    history.push(`/ReportNonConformingEvent?${params.toString()}`);
  };
  return (
    <div data-testid="range-not-applied-warning">
      <InlineNotification
        kind="warning"
        lowContrast
        hideCloseButton
        title={intl.formatMessage({
          id: "order.rangeNotApplied.warning.title",
        })}
        subtitle={subtitle}
      />
      <Button
        kind="tertiary"
        size="sm"
        onClick={reportNce}
        data-testid="range-not-applied-report-nce"
      >
        <FormattedMessage id="order.rangeNotApplied.warning.reportNce" />
      </Button>
    </div>
  );
};

export default RangeNotAppliedWarning;

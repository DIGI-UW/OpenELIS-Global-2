import React from "react";
import { Button, InlineNotification } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

/**
 * Shown when a save is refused because someone else changed the record after
 * this page loaded it (409 conflict "stale", OGC-1463). Nothing was written;
 * Refresh loads the latest version so the change can be made again.
 */
const StaleSaveNotice = ({ onRefresh, message, testId = "stale-save" }) => {
  const intl = useIntl();
  return (
    <div data-testid={testId}>
      <InlineNotification
        kind="error"
        lowContrast
        hideCloseButton
        title={intl.formatMessage({ id: "error.title" })}
        subtitle={
          message || intl.formatMessage({ id: "error.staleSave.section" })
        }
      />
      <Button
        kind="secondary"
        size="sm"
        onClick={onRefresh || (() => window.location.reload())}
      >
        <FormattedMessage id="label.results.refresh" />
      </Button>
    </div>
  );
};

/** True for a 409 the server answered because the save was stale. */
export const isStaleConflict = (status, body) =>
  status === 409 &&
  !!body &&
  (body.conflict === "stale" || body.field === "stale");

export default StaleSaveNotice;

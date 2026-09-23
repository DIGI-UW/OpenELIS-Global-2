import React from "react";
import { InlineNotification } from "@carbon/react";
import { useIntl } from "react-intl";

/**
 * The body of a bench stage the case moves through but for which OpenELIS
 * records no work yet.
 *
 * The section is still numbered, still in the rail and can still be the case's
 * current stage (S-3.3), so the one thing it owes the bench is to say plainly
 * that there is nothing to fill in here, rather than looking like a section
 * that failed to load.
 */
const StagePlaceholderSection = () => {
  const intl = useIntl();

  return (
    <InlineNotification
      kind="info"
      lowContrast
      hideCloseButton
      title={intl.formatMessage({ id: "pathology.empty.stageNotRecorded" })}
    />
  );
};

export default StagePlaceholderSection;

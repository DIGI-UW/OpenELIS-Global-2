import React from "react";
import { useIntl } from "react-intl";
import { InlineNotification } from "@carbon/react";
import {
  describeUnmetRequirements,
  unmetRequirements,
} from "./saveRequirements";

/**
 * Says what the order still needs, beside the Save buttons that stay disabled
 * until it is there, so a user never has to press Save to find out.
 */
const SaveRequirementsNotice = ({ requirements }) => {
  const intl = useIntl();
  if (unmetRequirements(requirements).length === 0) {
    return null;
  }
  return (
    <InlineNotification
      kind="info"
      lowContrast
      hideCloseButton
      data-testid="save-requirements-notice"
      title={intl.formatMessage({ id: "order.save.requirements.title" })}
      subtitle={describeUnmetRequirements(intl, requirements)}
    />
  );
};

export default SaveRequirementsNotice;

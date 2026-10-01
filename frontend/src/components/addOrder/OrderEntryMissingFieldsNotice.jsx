import React from "react";
import { useIntl } from "react-intl";
import { InlineNotification } from "@carbon/react";
import { missingFieldRequirements } from "./orderEntryMissingFields";

/**
 * Says which fields keep Submit disabled on the Add Order page, including
 * fields on an earlier step such as the patient's sex or date of birth.
 */
const OrderEntryMissingFieldsNotice = ({ errors }) => {
  const intl = useIntl();
  const requirements = missingFieldRequirements(errors);
  if (requirements.length === 0) {
    return null;
  }
  const labels = requirements.map((requirement) =>
    requirement.labelId
      ? intl.formatMessage({ id: requirement.labelId })
      : requirement.text,
  );
  const fields =
    labels.length === 1
      ? labels[0]
      : `${labels.slice(0, -1).join(", ")} ${intl.formatMessage({
          id: "order.save.incomplete.and",
        })} ${labels[labels.length - 1]}`;
  return (
    <InlineNotification
      kind="info"
      lowContrast
      hideCloseButton
      data-testid="order-entry-missing-fields"
      title={intl.formatMessage({ id: "order.save.requirements.title" })}
      subtitle={intl.formatMessage(
        { id: "order.save.incomplete.fields" },
        { fields },
      )}
    />
  );
};

export default OrderEntryMissingFieldsNotice;

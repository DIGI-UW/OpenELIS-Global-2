import React from "react";
import { InlineNotification } from "@carbon/react";
import { useIntl } from "react-intl";
import { useOrderContext, SaveStatus } from "./OrderContext";

/**
 * Server-side rejections travel as message bundle keys, because the validator
 * passes the key as its own default message. Render the translated text when
 * the bundle knows the key, and the server's own wording when it does not.
 * A rejection reported as `field: key` is translated on its key half so the
 * field the server names is still visible.
 */
export const localizeServerMessage = (intl, value) => {
  if (!value) {
    return value;
  }
  if (intl.messages[value]) {
    return intl.formatMessage({ id: value });
  }
  const separator = value.indexOf(": ");
  if (separator > 0) {
    const code = value.slice(separator + 2);
    if (intl.messages[code]) {
      return `${value.slice(0, separator)}: ${intl.formatMessage({ id: code })}`;
    }
  }
  return value;
};

const ORDER_LEVEL_FIELD = "sampleOrderItems";

/**
 * A rejection against the order as a whole names no field the user can find on
 * the screen, so it reads as the message alone.
 */
const withoutOrderLevelField = (text) =>
  typeof text === "string" && text.startsWith(`${ORDER_LEVEL_FIELD}: `)
    ? text.slice(ORDER_LEVEL_FIELD.length + 2)
    : text;

/**
 * What a blocked save asks the user to correct. Fields the screen already marks
 * inline are left out so nothing is reported twice, and the server's summary is
 * left out when it only repeats one of the field errors.
 */
const SaveFailureNotice = ({ inlineFields = [] }) => {
  const intl = useIntl();
  const { saveStatus, error, fieldErrors } = useOrderContext();
  if (saveStatus !== SaveStatus.ERROR) {
    return null;
  }
  const entries = Object.entries(fieldErrors || {});
  const remaining = entries.filter(([field]) => !inlineFields.includes(field));
  const summaryRepeatsAField = entries.some(
    ([field, message]) => error === `${field}: ${message}`,
  );
  const summary = summaryRepeatsAField
    ? undefined
    : withoutOrderLevelField(localizeServerMessage(intl, error));
  return (
    <InlineNotification
      kind="error"
      lowContrast
      hideCloseButton
      className="order-save-failure"
      title={intl.formatMessage({
        id: "order.save.blocked",
        defaultMessage: "The order was not saved",
      })}
      subtitle={summary}
    >
      {remaining.length > 0 && (
        <ul className="order-save-failure-fields">
          {remaining.map(([field, message]) => (
            <li key={field}>
              {field === ORDER_LEVEL_FIELD
                ? localizeServerMessage(intl, message)
                : `${field}: ${localizeServerMessage(intl, message)}`}
            </li>
          ))}
        </ul>
      )}
    </InlineNotification>
  );
};

export default SaveFailureNotice;

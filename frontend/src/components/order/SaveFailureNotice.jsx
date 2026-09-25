import React from "react";
import { InlineNotification } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
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

/**
 * What a blocked save asks the user to correct. Fields the screen already marks
 * inline are left out so nothing is reported twice.
 */
const SaveFailureNotice = ({ inlineFields = [] }) => {
  const intl = useIntl();
  const { saveStatus, error, fieldErrors } = useOrderContext();
  if (saveStatus !== SaveStatus.ERROR) {
    return null;
  }
  const remaining = Object.entries(fieldErrors || {}).filter(
    ([field]) => !inlineFields.includes(field),
  );
  return (
    <InlineNotification
      kind="error"
      lowContrast
      hideCloseButton
      className="order-save-failure"
      title={
        <FormattedMessage
          id="order.save.blocked"
          defaultMessage="The order was not saved"
        />
      }
      subtitle={
        <span>
          {localizeServerMessage(intl, error)}
          {remaining.length > 0 && (
            <ul className="order-save-failure-fields">
              {remaining.map(([field, message]) => (
                <li key={field}>
                  {field}: {localizeServerMessage(intl, message)}
                </li>
              ))}
            </ul>
          )}
        </span>
      }
    />
  );
};

export default SaveFailureNotice;

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
 * The name the screen gives a field the server rejected, by the field's path
 * on the order form. A field not listed keeps its path.
 */
const FIELD_LABELS = {
  "sampleOrderItems.requestorFirstName": "order.field.requestorFirstName",
  "sampleOrderItems.requestorLastName": "order.field.requestorLastName",
  "sampleOrderItems.providerFirstName": "order.field.providerFirstName",
  "sampleOrderItems.providerLastName": "order.field.providerLastName",
};

export const fieldLabel = (intl, field) =>
  FIELD_LABELS[field] ? intl.formatMessage({ id: FIELD_LABELS[field] }) : field;

/** A `field: message` summary, with the field named as the screen names it. */
const withFieldLabel = (intl, text) => {
  if (typeof text !== "string") {
    return text;
  }
  const field = Object.keys(FIELD_LABELS).find((path) =>
    text.startsWith(`${path}: `),
  );
  return field
    ? `${fieldLabel(intl, field)}: ${text.slice(field.length + 2)}`
    : text;
};

/**
 * A rejection against the order as a whole names no field the user can find on
 * the screen, so it reads as the message alone.
 */
const withoutOrderLevelField = (text) =>
  typeof text === "string" && text.startsWith(`${ORDER_LEVEL_FIELD}: `)
    ? text.slice(ORDER_LEVEL_FIELD.length + 2)
    : text;

/**
 * The toast for a failed save. A save the server refused reads as the server's
 * reason, the same one the inline notice shows, instead of claiming a server
 * error; only a failure that carries no reason falls back to the generic text.
 */
export const saveFailureMessage = (intl, error) =>
  withoutOrderLevelField(
    withFieldLabel(intl, localizeServerMessage(intl, error?.message)),
  ) || intl.formatMessage({ id: "server.error.msg" });

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
    : withoutOrderLevelField(
        withFieldLabel(intl, localizeServerMessage(intl, error)),
      );
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
                : `${fieldLabel(intl, field)}: ${localizeServerMessage(intl, message)}`}
            </li>
          ))}
        </ul>
      )}
    </InlineNotification>
  );
};

export default SaveFailureNotice;

/**
 * Pure helper functions for label preset management (OGC-285 M3).
 */

/**
 * Reduces content field entries to the shape LabelPresetForm.FieldEntry
 * declares. The list endpoint returns each field with its id, sourceType and
 * lastupdated; echoing those back is a 400 (OGC-1227 defect 1).
 *
 * @param {Array} fields - fields as returned by GET /api/labelPresets
 * @returns {Array<{fieldKey: string, isRequired: boolean, displayOrder: number}>}
 */
export function toFieldEntries(fields) {
  if (!Array.isArray(fields)) return [];
  return fields.map(({ fieldKey, isRequired, displayOrder }) => ({
    fieldKey,
    isRequired: Boolean(isRequired),
    displayOrder,
  }));
}

/**
 * Turns a rejected save response into one readable sentence. Bean-validation
 * messages arrive as "{error.some.key}" and server errors as a messageKey; both
 * are translated when the key exists in the message bundle, otherwise the raw
 * text is shown so the cause is never hidden behind a bare "Save failed".
 *
 * @param {object} intl - react-intl instance
 * @param {number} status - HTTP status of the response
 * @param {string} rawBody - response body text
 * @returns {string}
 */
export function describeSaveFailure(intl, status, rawBody) {
  let message = rawBody || "";
  try {
    const body = JSON.parse(rawBody);
    const keyTranslated =
      typeof body.messageKey === "string" &&
      hasTranslation(intl, body.messageKey);
    const candidates = [
      body.messageKey,
      body.message,
      keyTranslated ? null : body.error,
      ...(Array.isArray(body.globalErrors) ? body.globalErrors : []),
      ...(Array.isArray(body.fieldErrors)
        ? body.fieldErrors.map((fe) => fe.defaultMessage)
        : []),
    ].filter((c) => typeof c === "string" && c.trim().length > 0);
    if (candidates.length > 0) {
      message = candidates
        .map((c) => translateServerMessage(intl, c))
        .join("; ");
    }
  } catch (e) {
    // not JSON: show the text as received
  }
  return intl.formatMessage(
    { id: "admin.labelPresets.saveFailed.detail" },
    { status, message: message.trim() },
  );
}

/**
 * Resolves "{error.key}" or "error.key" to its translation when the bundle has
 * it; returns the input unchanged otherwise.
 */
export function translateServerMessage(intl, raw) {
  if (hasTranslation(intl, raw)) {
    return intl.formatMessage({ id: messageId(raw) });
  }
  return raw;
}

function hasTranslation(intl, raw) {
  return Boolean(intl.messages && intl.messages[messageId(raw)]);
}

function messageId(raw) {
  return raw.replace(/^\{|\}$/g, "");
}

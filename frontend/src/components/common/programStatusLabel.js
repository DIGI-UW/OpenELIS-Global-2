function toLowerCamel(statusId) {
  const [first, ...rest] = String(statusId).toLowerCase().split("_");
  return (
    first +
    rest.map((word) => word.charAt(0).toUpperCase() + word.slice(1)).join("")
  );
}

/**
 * Localized label for a program case status id (e.g. READY_PATHOLOGIST under
 * "immunohistochemistry.status."). A status the catalogue has no key for falls
 * back to the served text, then to the raw id.
 */
export function programStatusLabel(intl, keyPrefix, statusId, fallback) {
  const id = keyPrefix + toLowerCamel(statusId);
  return intl.messages?.[id]
    ? intl.formatMessage({ id })
    : (fallback ?? statusId);
}

/**
 * A result whose reference range could not be applied because the patient's
 * sex or birth date was not recorded carries the message key of that reason.
 */
export function rangeNotAppliedKey(row) {
  const key = row && row.rangeNotAppliedReason;
  return typeof key === "string" && key.trim().length > 0 ? key : null;
}

/** The range cell text: the reason when no range was applied, else the range. */
export function displayRange(intl, row) {
  const key = rangeNotAppliedKey(row);
  if (key) {
    return intl.formatMessage({ id: key });
  }
  return (row && row.normalRange) || "";
}

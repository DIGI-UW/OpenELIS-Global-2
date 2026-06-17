/**
 * OGC-949 M7 — reference-range age helpers.
 *
 * The backend stores and reports ages in DAYS (the legacy result_limits unit).
 * The UI lets the admin enter an age as a value + unit and renders day counts
 * back in the most human-readable unit, so a neonatal gap reads "2 days" rather
 * than "0.005 years".
 */
export const DAYS_PER = { days: 1, months: 30.4375, years: 365 };

/** Convert a user-entered value+unit to days; null if the value is not numeric. */
export const toDays = (value, unit) => {
  if (value === "" || value === null || value === undefined) {
    return null;
  }
  const n = parseFloat(value);
  if (Number.isNaN(n)) {
    return null;
  }
  return n * (DAYS_PER[unit] || 1);
};

/** Format a day count in the most readable unit (<60d → days, <2y → months, else years). */
export const formatAgeDays = (days, intl) => {
  if (days === null || days === undefined) {
    return "";
  }
  const unitLabel = (u) =>
    intl.formatMessage({ id: `label.testCatalog.ranges.${u}` });
  if (days < 60) {
    return `${Math.round(days)} ${unitLabel("days")}`;
  }
  if (days < 730) {
    return `${Math.round(days / DAYS_PER.months)} ${unitLabel("months")}`;
  }
  const years = Math.round((days / DAYS_PER.years) * 10) / 10;
  return `${years} ${unitLabel("years")}`;
};

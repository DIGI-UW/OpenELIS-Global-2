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

/**
 * Format a day count in the most readable unit (<60d → days, <2y → months, else
 * years).
 *
 * OGC-1153: an open-ended coverage bound is not a number. The backend reports a
 * trailing coverage gap as `new AgeInterval(coveredTo, POSITIVE_INFINITY)` and
 * Jackson serialises a non-finite double as the quoted string "Infinity"
 * (QUOTE_NON_NUMERIC_NUMBERS), so both the string and a real ±Infinity have to be
 * recognised here — otherwise the arithmetic below renders "Infinity years" into
 * the coverage-gap dialog. Non-finite bounds get a directional open-ended label
 * so `{from} – {to}` still reads correctly; unparseable input renders as nothing.
 */
export const formatAgeDays = (days, intl) => {
  if (days === null || days === undefined) {
    return "";
  }
  const unitLabel = (u) =>
    intl.formatMessage({ id: `label.testCatalog.ranges.${u}` });
  const n = Number(days);
  if (!Number.isFinite(n)) {
    if (n === Number.POSITIVE_INFINITY) {
      return intl.formatMessage({
        id: "label.testCatalog.ranges.noUpperLimit",
      });
    }
    if (n === Number.NEGATIVE_INFINITY) {
      return intl.formatMessage({
        id: "label.testCatalog.ranges.noLowerLimit",
      });
    }
    return "";
  }
  if (n < 60) {
    return `${Math.round(n)} ${unitLabel("days")}`;
  }
  if (n < 730) {
    return `${Math.round(n / DAYS_PER.months)} ${unitLabel("months")}`;
  }
  const years = Math.round((n / DAYS_PER.years) * 10) / 10;
  return `${years} ${unitLabel("years")}`;
};

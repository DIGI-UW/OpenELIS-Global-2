/**
 * Shared date/duration helpers for the Cold Storage Monitoring components.
 *
 * The backend now serializes every timestamp consumed here (Alert
 * startTime/endTime/acknowledgedAt/resolvedAt/lastDuplicateTime, freezer
 * reading recordedAt, corrective-action createdAt/updatedAt/completedAt/
 * retractedAt) as unambiguous ISO-8601 strings. There is no longer any need
 * to guess whether a numeric value is epoch-seconds or epoch-milliseconds -
 * `new Date(value)` parses ISO strings correctly and unambiguously in every
 * browser. Do not reintroduce a threshold/heuristic here.
 */

/**
 * Safely parse a date value (ISO-8601 string, Date instance, or timestamp)
 * into a Date object.
 * @param {string|number|Date|null|undefined} value
 * @returns {Date|null} a valid Date, or null if value is missing/invalid.
 */
export const toDate = (value) => {
  if (value === null || value === undefined || value === "") {
    return null;
  }
  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? null : value;
  }
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
};

/**
 * Format a date value as a locale-aware date+time string, or a placeholder
 * when the value is missing/invalid.
 * @param {string|number|Date|null|undefined} value
 * @param {string} placeholder
 * @returns {string}
 */
export const formatDateTime = (value, placeholder = "—") => {
  const date = toDate(value);
  return date ? date.toLocaleString() : placeholder;
};

/**
 * Format a duration given in seconds as a human-readable "N minutes" string.
 * @param {number|null|undefined} seconds
 * @param {string} placeholder
 * @returns {string}
 */
export const formatDuration = (seconds, placeholder = "—") => {
  if (seconds === null || seconds === undefined || Number.isNaN(seconds)) {
    return placeholder;
  }
  const minutes = Math.max(1, Math.round(seconds / 60));
  return `${minutes} minutes`;
};

/**
 * Compute the number of whole seconds elapsed between a start value and now
 * (or a provided reference time).
 * @param {string|number|Date|null|undefined} startValue
 * @param {Date} [now]
 * @returns {number|null}
 */
export const secondsSince = (startValue, now = new Date()) => {
  const start = toDate(startValue);
  if (!start) {
    return null;
  }
  return Math.floor((now.getTime() - start.getTime()) / 1000);
};

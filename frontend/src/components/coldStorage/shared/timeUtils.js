// The Cold Storage REST API serializes every timestamp as an unambiguous
// ISO-8601 string (see shared/dateUtils.js) - `new Date(value)` parses those
// correctly on its own. The numeric-seconds branch below is defensive only
// (kept in case a caller ever passes a raw epoch value); do not assume the
// backend sends epoch numbers.
const SECONDS_TO_MILLIS_THRESHOLD = 1e12;

export const toDate = (value) => {
  if (value == null || value === "") {
    return null;
  }
  const millis =
    typeof value === "number" && value < SECONDS_TO_MILLIS_THRESHOLD
      ? value * 1000
      : value;
  const date = new Date(millis);
  return Number.isNaN(date.getTime()) ? null : date;
};

export const formatDuration = (seconds) => {
  const total = Number(seconds);
  if (seconds == null || Number.isNaN(total)) {
    return "—";
  }
  let remaining = Math.max(0, Math.floor(total));
  const days = Math.floor(remaining / 86400);
  remaining %= 86400;
  const hours = Math.floor(remaining / 3600);
  remaining %= 3600;
  const minutes = Math.floor(remaining / 60);
  const secs = remaining % 60;

  if (days > 0) {
    return hours > 0 ? `${days}d ${hours}h` : `${days}d`;
  }
  if (hours > 0) {
    return minutes > 0 ? `${hours}h ${minutes}m` : `${hours}h`;
  }
  if (minutes > 0) {
    return `${minutes}m`;
  }
  return `${secs}s`;
};

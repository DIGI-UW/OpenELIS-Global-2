// The Cold Storage REST API serializes timestamps with Jackson's
// WRITE_DATES_AS_TIMESTAMPS enabled, so date fields arrive as a Unix epoch in
// SECONDS. JavaScript's Date constructor expects MILLISECONDS, so a raw value
// must be scaled by 1000 first; toDate() normalizes seconds, millisecond
// epochs, and ISO strings to a single Date.
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

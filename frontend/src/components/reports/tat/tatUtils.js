/**
 * Format TAT hours as a human-readable string (e.g., "2h 30m").
 */
export function formatTat(hours) {
  if (hours == null) return "—";
  const totalMinutes = Math.round(hours * 60);
  const h = Math.floor(totalMinutes / 60);
  const m = totalMinutes % 60;
  if (h === 0 && m === 0) return "0h 0m";
  if (h === 0) return `${m}m`;
  if (m === 0) return `${h}h`;
  return `${h}h ${m}m`;
}

/** Trend bucket sizes, offered by the TAT trend and by every QI rate report. */
export const INTERVALS = [
  { id: "DAILY", labelKey: "reports.tat.daily" },
  { id: "WEEKLY", labelKey: "reports.tat.weekly" },
  { id: "MONTHLY", labelKey: "reports.tat.monthly" },
];

/**
 * How a tile shows a window's movement against the prior window: an arrow, the
 * size of the move, and whether that direction is good news. A difference
 * smaller than `flat` is no movement at all.
 */
function windowDelta(diff, flat, format, higherBetter) {
  const isFlat = Math.abs(diff) < flat;
  return {
    tone: isFlat ? "flat" : (higherBetter ? diff > 0 : diff < 0) ? "good" : "bad",
    arrow: isFlat ? "—" : diff < 0 ? "↓" : "↑",
    text: isFlat ? "" : format(Math.abs(diff)),
  };
}

/**
 * Delta of a window's mean TAT vs the equal-length prior window. Under a
 * minute of difference reads as flat. Null when either window has no runs.
 * Shared by the QI Dashboard TAT tile and the QA Overview QI rollup.
 */
export function tatDelta(current, prior) {
  if (!(current?.totalCount > 0) || !(prior?.totalCount > 0)) return null;
  return windowDelta(current.mean - prior.mean, 1 / 60, formatTat, false);
}

/**
 * Delta of a percentage metric vs the prior window, to 2dp. `higherBetter`
 * says which direction is good news — compliance rises, rejections fall.
 */
export function pctDelta(current, prior, higherBetter) {
  if (current == null || prior == null) return null;
  return windowDelta(
    current - prior,
    0.005,
    (d) => `${d.toFixed(2)}%`,
    higherBetter,
  );
}

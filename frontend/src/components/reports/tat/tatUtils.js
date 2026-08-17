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

/**
 * Delta of a window's mean TAT vs the equal-length prior window. Under a
 * minute of difference reads as flat. Null when either window has no runs.
 * Shared by the QI Dashboard TAT tile and the QA Overview QI rollup.
 */
export function tatDelta(current, prior) {
  if (!(current?.totalCount > 0) || !(prior?.totalCount > 0)) return null;
  const diff = current.mean - prior.mean;
  const flat = Math.abs(diff) < 1 / 60;
  return {
    tone: flat ? "flat" : diff < 0 ? "good" : "bad",
    arrow: flat ? "—" : diff < 0 ? "↓" : "↑",
    text: flat ? "" : formatTat(Math.abs(diff)),
  };
}

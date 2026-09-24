import { addDays, startOfWeek } from "date-fns";
import { toLocalIsoDate } from "../../utils/Utils";

/**
 * Date helpers shared by every QA surface. Each page used to carry its own
 * copy of "thirty days ago", its own day-in-milliseconds constant and its own
 * duration formatter; they live here so the windows the pages report on are
 * the same windows.
 */

export const DAY_MS = 24 * 60 * 60 * 1000;

/** Local yyyy-MM-dd `days` from today; negative counts backwards. */
export const isoDaysFromToday = (days) =>
  toLocalIsoDate(addDays(new Date(), days));

/** The {fromDate, toDate} window a QA filter opens on: the last `days` days. */
export const lastDays = (days) => ({
  fromDate: isoDaysFromToday(-days),
  toDate: toLocalIsoDate(new Date()),
});

/** Local Monday of the week containing `now`, at midnight. */
export const weekStart = (now = new Date()) =>
  startOfWeek(now, { weekStartsOn: 1 });

/**
 * A duration in minutes as "45m" / "3h 20m" / "2d 4h", or "—" when absent.
 * Negative durations keep their sign: a critical result called back before it
 * was released is ahead of its deadline, not missing one.
 */
export function formatMinutes(minutes) {
  if (minutes == null) {
    return "—";
  }
  const sign = minutes < 0 ? "−" : "";
  const abs = Math.abs(minutes);
  if (abs < 60) {
    return `${sign}${abs}m`;
  }
  const hours = Math.floor(abs / 60);
  if (hours < 24) {
    return `${sign}${hours}h ${abs % 60}m`;
  }
  return `${sign}${Math.floor(hours / 24)}d ${hours % 24}h`;
}

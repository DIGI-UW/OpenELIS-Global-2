import {
  differenceInCalendarDays,
  format,
  isValid,
  parse,
  parseISO,
} from "date-fns";

export const configuredDatePattern = (dateLocale) =>
  dateLocale === "fr-FR" ? "dd/MM/yyyy" : "MM/dd/yyyy";

/**
 * Today in the browser's own time zone as `yyyy-MM-dd`.
 *
 * `toISOString()` reports UTC, which puts a lab west of Greenwich a day behind
 * and a lab east of it a day ahead for part of every day. Collection and
 * receipt dates are wall-clock events at the site, so they are stamped from
 * local time.
 */
export const todayLocalIso = (now = new Date()) => format(now, "yyyy-MM-dd");

/** The current wall-clock time at the site as `HH:mm`. */
export const currentLocalTime = (now = new Date()) => format(now, "HH:mm");

export const formatIsoDateForBackend = (isoDate, dateLocale) => {
  if (!isoDate) {
    return "";
  }
  if (isoDate.includes("/")) {
    return isoDate;
  }
  const parsed = parseISO(isoDate.slice(0, 10));
  return isValid(parsed)
    ? format(parsed, configuredDatePattern(dateLocale))
    : isoDate;
};

export const formatPickerDateForIso = (pickerDate, dateLocale) => {
  if (!pickerDate) {
    return "";
  }
  const pattern = configuredDatePattern(dateLocale);
  const parsed = parse(pickerDate, pattern, new Date());
  return isValid(parsed) && format(parsed, pattern) === pickerDate
    ? format(parsed, "yyyy-MM-dd")
    : "";
};

export const normalizeDateForState = (dateValue, dateLocale) => {
  if (!dateValue) {
    return "";
  }
  const isoDate = dateValue.slice(0, 10);
  const parsedIso = parseISO(isoDate);
  if (isValid(parsedIso) && format(parsedIso, "yyyy-MM-dd") === isoDate) {
    return isoDate;
  }
  return formatPickerDateForIso(dateValue, dateLocale);
};

export const isCollectionDateBeforeAdmissionDate = (
  collectionDate,
  admissionDate,
) =>
  Boolean(collectionDate && admissionDate) &&
  collectionDate.slice(0, 10) < admissionDate.slice(0, 10);

export const daysBetweenIsoDates = (startDate, endDate) => {
  if (!startDate || !endDate) {
    return null;
  }
  const start = parseISO(startDate.slice(0, 10));
  const end = parseISO(endDate.slice(0, 10));
  return isValid(start) && isValid(end)
    ? differenceInCalendarDays(end, start)
    : null;
};

/**
 * The shortest holding time, in minutes, among a sample's ordered tests.
 *
 * Holding time is a test attribute (`Test.timeHolding`), never user input, and
 * a sample carrying several tests must satisfy the tightest of them.
 */
export const shortestHoldingMinutes = (tests = []) => {
  const limits = tests
    .map((test) => parseInt(test?.timeHolding, 10))
    .filter((minutes) => Number.isFinite(minutes) && minutes > 0);
  return limits.length > 0 ? Math.min(...limits) : null;
};

/**
 * When a sample must be tested by, measured from lab receipt.
 *
 * The existing over-hold flag on Results measures from collection; receipt is
 * the correct anchor for the laboratory's own clock, and is what the order
 * flow can show at intake. Falls back to collection when receipt is not yet
 * recorded. Returns null when nothing can be derived.
 */
export const holdingDeadline = (sample = {}, tests = []) => {
  const minutes = shortestHoldingMinutes(tests);
  if (!minutes) {
    return null;
  }
  const anchorDate = sample.receivedDate || sample.collectionDate;
  if (!anchorDate) {
    return null;
  }
  const anchorTime = sample.receivedDate
    ? sample.receivedTime || "00:00"
    : sample.collectionTime || "00:00";
  const parsed = parseISO(`${anchorDate.slice(0, 10)}T${anchorTime}`);
  if (!isValid(parsed)) {
    return null;
  }
  return new Date(parsed.getTime() + minutes * 60 * 1000);
};

/** Renders a holding time in minutes as a short "36 h 30 m" style label. */
export const formatHoldingMinutes = (minutes) => {
  if (!minutes) {
    return "";
  }
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  if (hours === 0) return `${rest} min`;
  if (rest === 0) return `${hours} h`;
  return `${hours} h ${rest} min`;
};

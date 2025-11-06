import dayjs from "dayjs";
import utc from "dayjs/plugin/utc";
import isToday from "dayjs/plugin/isToday";
import { ParsedTimeType } from "../../filter/filter-types";

dayjs.extend(utc);
dayjs.extend(isToday);

export type FormatDateMode = "standard" | "wide";

export type FormatDateOptions = {
  /**
   * - `standard`: "03 Feb 2022"
   * - `wide`:     "03 — Feb — 2022"
   */
  mode: FormatDateMode;
  /**
   * Whether the time should be included in the output always (`true`),
   * never (`false`), or only when the input date is today (`for today`).
   */
  time: true | false | "for today";
  /** Whether to include the day number */
  day: boolean;
  /** Whether to include the year */
  year: boolean;
};

const defaultOptions: FormatDateOptions = {
  mode: "standard",
  time: "for today",
  day: true,
  year: true,
};

export function formatDate(date: Date, options?: Partial<FormatDateOptions>) {
  const { mode, time, day, year }: FormatDateOptions = {
    ...defaultOptions,
    ...options,
  };
  const formatterOptions: Intl.DateTimeFormatOptions = {
    year: year ? "numeric" : undefined,
    month: "short",
    day: day ? "2-digit" : undefined,
  };
  let locale = getLocale();
  let localeString: string;
  const isToday = dayjs(date).isToday();
  if (isToday) {
    // This produces the word "Today" in the language of `locale`
    const rtf = new Intl.RelativeTimeFormat(locale, { numeric: "auto" });
    localeString = rtf.format(0, "day");
    localeString =
      localeString[0].toLocaleUpperCase(locale) + localeString.slice(1);
  } else {
    if (locale == "en") {
      // This locale override is here rather than in `getLocale`
      // because Americans should see AM/PM for times.
      locale = "en-GB";
    }
    localeString = date.toLocaleDateString(locale, formatterOptions);
    if (locale == "en-GB" && mode == "standard" && year && day) {
      // Custom formatting for English. Use hyphens instead of spaces.
      localeString = localeString.replace(/ /g, "-");
    }
    if (mode == "wide") {
      localeString = localeString.replace(/ /g, " — "); // space-emdash-space
      if (/ru.*/.test(locale)) {
        // Remove the extra em-dash that gets added between the year and the suffix 'r.'
        const len = localeString.length;
        localeString =
          localeString.slice(0, len - 5) +
          localeString.slice(len - 5).replace(" — ", " ");
      }
    }
  }
  if (time === true || (isToday && time === "for today")) {
    localeString += `, ${formatTime(date)}`;
  }
  return localeString;
}

/**
 * Formats the input as a time, according to the current locale.
 * 12-hour or 24-hour clock depends on locale.
 */
export function formatTime(date: Date) {
  return date.toLocaleTimeString(getLocale(), {
    hour: "2-digit",
    minute: "2-digit",
  });
}

/**
 * Formats the input into a string showing the date and time, according
 * to the current locale. The `mode` parameter is as described for
 * `formatDate`.
 *
 * This is created by concatenating the results of `formatDate`
 * and `formatTime` with a comma and space. This agrees with the
 * output of `Date.prototype.toLocaleString` for *most* locales.
 */
export function formatDatetime(
  date: Date,
  options?: Partial<Omit<FormatDateOptions, "time">>,
) {
  return formatDate(date, { ...options, time: true });
}

function getLocale() {
  return "en";
}

/**
 * Utility function to parse an arbitrary string into a date.
 * Uses `dayjs(dateString)`.
 */
export function parseDate(dateString: string) {
  return dayjs(dateString).toDate();
}

export const parseTime: (sortedTimes: Array<string>) => ParsedTimeType = (
  sortedTimes,
) => {
  const yearColumns: Array<{ year: string; size: number }> = [],
    dayColumns: Array<{ year: string; day: string; size: number }> = [],
    timeColumns: string[] = [];

  sortedTimes.forEach((datetime) => {
    const parsedDate = parseDate(datetime);
    const year = parsedDate.getFullYear().toString();
    const date = formatDate(parsedDate, { mode: "wide", year: false });
    const time = formatTime(parsedDate);

    const yearColumn = yearColumns.find(
      ({ year: innerYear }) => year === innerYear,
    );
    if (yearColumn) yearColumn.size++;
    else yearColumns.push({ year, size: 1 });

    const dayColumn = dayColumns.find(
      ({ year: innerYear, day: innerDay }) =>
        date === innerDay && year === innerYear,
    );
    if (dayColumn) dayColumn.size++;
    else dayColumns.push({ day: date, year, size: 1 });

    timeColumns.push(time);
  });

  return { yearColumns, dayColumns, timeColumns, sortedTimes };
};

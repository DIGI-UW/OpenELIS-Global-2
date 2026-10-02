import { getFromOpenElisServer } from "./Utils";

/**
 * The lab's clock. Calendar values (today, this year, a range ending today, a
 * latest allowed date) follow the lab server's time zone, not the browser's:
 * a browser in another zone would otherwise pre-fill, accept or filter by a
 * day that is not the lab's.
 *
 * The zone is read once, after login. Until then, or if the server names no
 * zone this browser knows, the browser's own clock is used.
 */
interface ServerTime {
  date?: string;
  time?: string;
  timezone?: string;
}

let labFields: Intl.DateTimeFormat | null = null;
let skewMs = 0;

const MINUTE_MS = 60 * 1000;
const CLOCK_TIMEOUT_MS = 5000;

const zoneFields = (timeZone: string): Intl.DateTimeFormat | null => {
  try {
    return new Intl.DateTimeFormat("en-US", {
      timeZone,
      hourCycle: "h23",
      year: "numeric",
      month: "numeric",
      day: "numeric",
      hour: "numeric",
      minute: "numeric",
      second: "numeric",
    });
  } catch {
    return null;
  }
};

/** The lab's wall clock at an instant, in a Date's local fields. */
const wallClockAt = (instant: number): Date => {
  const part = Object.fromEntries(
    (labFields as Intl.DateTimeFormat)
      .formatToParts(instant)
      .map(({ type, value }) => [type, Number(value)]),
  );
  return new Date(
    part.year,
    part.month - 1,
    part.day,
    part.hour,
    part.minute,
    part.second,
  );
};

/**
 * The server also reports its own date and minute, which corrects a browser
 * clock that is simply wrong. Differences under a minute are below what the
 * server reports and are ignored.
 */
const serverSkew = (response: ServerTime | undefined): number => {
  const date = /^(\d{4})-(\d{2})-(\d{2})$/.exec(response?.date || "");
  const time = /^(\d{2}):(\d{2})$/.exec(response?.time || "");
  if (!labFields || !date || !time) {
    return 0;
  }
  const serverWall = new Date(
    Number(date[1]),
    Number(date[2]) - 1,
    Number(date[3]),
    Number(time[1]),
    Number(time[2]),
  ).getTime();
  const browserWall = wallClockAt(Date.now()).getTime();
  const skew = serverWall - browserWall;
  return Math.abs(skew) < MINUTE_MS ? 0 : skew;
};

export const loadLabClock = (): Promise<void> =>
  new Promise((resolve) => {
    const controller = new AbortController();
    let settled = false;
    const finish = (response?: ServerTime) => {
      if (settled) return;
      settled = true;
      clearTimeout(timeout);
      labFields = response?.timezone ? zoneFields(response.timezone) : null;
      skewMs = serverSkew(response);
      resolve();
    };
    // Authentication can proceed with the browser fallback if this optional
    // request stalls. Ignore a late response rather than changing mounted forms.
    const timeout = setTimeout(() => {
      controller.abort();
      finish();
    }, CLOCK_TIMEOUT_MS);
    getFromOpenElisServer<ServerTime>(
      "/rest/server-time",
      finish,
      controller.signal,
    );
  });

export const resetLabClock = (): void => {
  labFields = null;
  skewMs = 0;
};

/**
 * Now on the lab's wall clock: a Date whose local fields (getDate, getHours,
 * and so on) read the lab's date and time. It is for calendar values only; an
 * instant to store or send is `new Date()`, or `labTimeToInstant()` of a lab
 * time.
 */
export const labNow = (): Date =>
  labFields ? wallClockAt(Date.now() + skewMs) : new Date(Date.now());

const wallClockTime = (value: Date | number): number =>
  value instanceof Date ? value.getTime() : Number(value);

/**
 * The instant a lab wall-clock time names (the reverse of `labNow()`), such as
 * the start of the lab's day as a bound sent to the server. The offset is
 * refined at the target, so a daylight-saving change in between is honoured.
 */
export const labTimeToInstant = (wallClock: Date | number): Date => {
  const wall = wallClockTime(wallClock);
  if (!labFields) {
    return new Date(wall);
  }
  let instant = wall - (wallClockAt(wall).getTime() - wall);
  instant = wall - (wallClockAt(instant).getTime() - instant);
  return new Date(instant);
};

/** Calendar-day distance to a stored ISO date (including dates at UTC midnight).
 * UTC is only an arithmetic frame here: no browser offset or daylight-saving
 * transition may shorten a calendar day.
 */
export const daysFromLabToday = (value: string): number => {
  const stored = new Date(value);
  const today = labNow();
  return (
    (Date.UTC(
      stored.getUTCFullYear(),
      stored.getUTCMonth(),
      stored.getUTCDate(),
    ) -
      Date.UTC(today.getFullYear(), today.getMonth(), today.getDate())) /
    (24 * 60 * 60 * 1000)
  );
};

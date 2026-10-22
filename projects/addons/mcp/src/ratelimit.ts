// Per-token limits: a token bucket refilled continuously (ratePerMinute) and a
// counter per calendar day in the laboratory's time zone (ratePerDay).
import { dayIn } from "./config.js";

export interface Limits {
  ratePerMinute: number;
  ratePerDay: number;
  timeZone: string;
}

export type Verdict =
  { ok: true } | { ok: false; retryAfterSeconds: number; limit: "minute" | "day" };

interface State {
  tokens: number;
  at: number;
  day: string;
  used: number;
}

export class RateLimiter {
  private readonly state = new Map<string, State>();

  constructor(private readonly now: () => Date = () => new Date()) {}

  /** Takes one call from `key`'s allowance, or says when to come back. */
  take(key: string, limits: Limits): Verdict {
    const now = this.now();
    const t = now.getTime();
    const day = dayIn(limits.timeZone, now);
    const cap = limits.ratePerMinute;
    let s = this.state.get(key);
    if (!s) {
      s = { tokens: cap, at: t, day, used: 0 };
      this.state.set(key, s);
    }
    s.tokens = Math.min(cap, s.tokens + ((t - s.at) / 60000) * cap);
    s.at = t;
    if (s.day !== day) {
      s.day = day;
      s.used = 0;
    }
    if (s.used >= limits.ratePerDay) {
      return { ok: false, limit: "day", retryAfterSeconds: secondsToNextDay(now, limits.timeZone) };
    }
    if (s.tokens < 1) {
      return {
        ok: false,
        limit: "minute",
        retryAfterSeconds: Math.max(1, Math.ceil(((1 - s.tokens) * 60) / cap)),
      };
    }
    s.tokens -= 1;
    s.used += 1;
    return { ok: true };
  }

  usedToday(key: string, timeZone: string): number {
    const s = this.state.get(key);
    return s && s.day === dayIn(timeZone, this.now()) ? s.used : 0;
  }
}

/** Seconds until the date in `timeZone` changes (searched by the minute, then second). */
export function secondsToNextDay(now: Date, timeZone: string): number {
  const today = dayIn(timeZone, now);
  let lo = 0;
  let hi = 26 * 3600;
  while (hi - lo > 1) {
    const mid = Math.floor((lo + hi) / 2);
    if (dayIn(timeZone, new Date(now.getTime() + mid * 1000)) === today) lo = mid;
    else hi = mid;
  }
  return hi;
}

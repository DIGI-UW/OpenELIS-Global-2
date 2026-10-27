import { describe, expect, it } from "vitest";
import { RateLimiter, secondsToNextDay } from "../src/ratelimit.js";

describe("rate limiter", () => {
  it("allows ratePerMinute at once, then refills continuously", () => {
    let now = new Date("2026-10-22T08:00:00Z");
    const r = new RateLimiter(() => now);
    const l = { ratePerMinute: 3, ratePerDay: 1000, timeZone: "UTC" };
    expect([1, 2, 3].map(() => r.take("a", l).ok)).toEqual([true, true, true]);
    const v = r.take("a", l);
    expect(v).toMatchObject({ ok: false, limit: "minute" });
    expect(v.ok === false && v.retryAfterSeconds).toBe(20);
    expect(r.take("b", l).ok).toBe(true); // per token
    now = new Date(now.getTime() + 20_000);
    expect(r.take("a", l).ok).toBe(true);
    expect(r.take("a", l).ok).toBe(false);
  });

  it("caps calls per day in the laboratory's time zone", () => {
    // 16:59:30 UTC = 23:59:30 in Ho Chi Minh City (UTC+7)
    let now = new Date("2026-10-22T16:59:30Z");
    const r = new RateLimiter(() => now);
    const l = { ratePerMinute: 100, ratePerDay: 2, timeZone: "Asia/Ho_Chi_Minh" };
    expect(r.take("a", l).ok).toBe(true);
    expect(r.take("a", l).ok).toBe(true);
    const v = r.take("a", l);
    expect(v).toMatchObject({ ok: false, limit: "day", retryAfterSeconds: 30 });
    expect(r.usedToday("a", l.timeZone)).toBe(2);
    now = new Date("2026-10-22T17:00:00Z"); // midnight there
    expect(r.take("a", l).ok).toBe(true);
    expect(r.usedToday("a", l.timeZone)).toBe(1);
  });

  it("finds the next local midnight", () => {
    expect(secondsToNextDay(new Date("2026-10-22T23:00:00Z"), "UTC")).toBe(3600);
    expect(secondsToNextDay(new Date("2026-10-22T16:00:00Z"), "Asia/Ho_Chi_Minh")).toBe(3600);
  });
});

import { toIsoDate } from "./dates";

describe("toIsoDate", () => {
  // Both ends of the day, because toISOString() moves the date in one
  // direction east of Greenwich and in the other west of it. A single sample
  // would pass in half the world with the bug still in place.
  it("keeps the calendar day the picker handed back, whatever the hour", () => {
    expect(toIsoDate(new Date(2026, 9, 15))).toBe("2026-10-15");
    expect(toIsoDate(new Date(2026, 9, 15, 23, 30))).toBe("2026-10-15");
  });

  it("pads single-digit months and days", () => {
    expect(toIsoDate(new Date(2026, 0, 5))).toBe("2026-01-05");
  });

  it("reads no date as no date", () => {
    expect(toIsoDate(null)).toBeNull();
  });
});

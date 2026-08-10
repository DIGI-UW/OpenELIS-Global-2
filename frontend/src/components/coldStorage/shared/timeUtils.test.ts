import { toDate, formatDuration } from "./timeUtils";

const EPOCH_SECONDS = 1781792388;

describe("toDate", () => {
  it("scales epoch seconds to milliseconds (not Jan 1970)", () => {
    const date = toDate(EPOCH_SECONDS);
    expect(date).toBeInstanceOf(Date);
    // Without the ×1000 fix this would be 1970.
    expect(date.getUTCFullYear()).toBe(2026);
    expect(date.getTime()).toBe(EPOCH_SECONDS * 1000);
  });

  it("leaves genuine millisecond timestamps untouched", () => {
    const millis = 1781792388000;
    expect(toDate(millis).getTime()).toBe(millis);
  });

  it("parses ISO strings", () => {
    expect(toDate("2026-06-18T14:19:48Z").getUTCFullYear()).toBe(2026);
  });

  it("returns null for empty or unparseable input", () => {
    expect(toDate(null)).toBeNull();
    expect(toDate("")).toBeNull();
    expect(toDate("not-a-date")).toBeNull();
  });
});

describe("formatDuration", () => {
  it("renders days and hours", () => {
    expect(formatDuration(13 * 86400 + 6 * 3600)).toBe("13d 6h");
  });

  it("drops the hours segment when zero", () => {
    expect(formatDuration(2 * 86400)).toBe("2d");
  });

  it("renders hours and minutes", () => {
    expect(formatDuration(5 * 3600 + 20 * 60)).toBe("5h 20m");
  });

  it("renders minutes only", () => {
    expect(formatDuration(45 * 60)).toBe("45m");
  });

  it("renders seconds for very short durations", () => {
    expect(formatDuration(30)).toBe("30s");
  });

  it("clamps negative durations to zero", () => {
    expect(formatDuration(-5)).toBe("0s");
  });

  it("returns a dash for null", () => {
    expect(formatDuration(null)).toBe("—");
  });
});

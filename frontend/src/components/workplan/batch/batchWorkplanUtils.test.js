import { isCurrentMinute, toLocalDateTime } from "./batchWorkplanUtils";

describe("bench QC time", () => {
  const now = new Date(2026, 10, 25, 7, 26, 42);

  it("starts a typed time at its minute", () => {
    expect(toLocalDateTime("11/25/2026", "07:20", "en-US")).toBe(
      "2026-11-25T07:20:00",
    );
    expect(toLocalDateTime("25/11/2026", "07:26", "fr-FR")).toBe(
      "2026-11-25T07:26:00",
    );
  });

  it("is null while the date or the time is incomplete", () => {
    expect(toLocalDateTime("", "07:26", "en-US")).toBeNull();
    expect(toLocalDateTime("11/25/2026", "7:2", "en-US")).toBeNull();
  });

  it("tells the lab's current minute from any other", () => {
    expect(isCurrentMinute("11/25/2026", "07:26", "en-US", now)).toBe(true);
    expect(isCurrentMinute("11/25/2026", "07:25", "en-US", now)).toBe(false);
    expect(isCurrentMinute("11/24/2026", "07:26", "en-US", now)).toBe(false);
  });
});

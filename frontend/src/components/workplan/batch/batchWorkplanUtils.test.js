import { toLocalDateTime } from "./batchWorkplanUtils";

describe("toLocalDateTime", () => {
  const now = new Date(2026, 10, 25, 7, 26, 42);

  it("keeps the seconds of a time in the current minute, so a per-run control follows the release made earlier in that minute", () => {
    expect(toLocalDateTime("11/25/2026", "07:26", "en-US", now)).toBe(
      "2026-11-25T07:26:42",
    );
  });

  it("starts a time typed for another minute at that minute", () => {
    expect(toLocalDateTime("11/25/2026", "07:20", "en-US", now)).toBe(
      "2026-11-25T07:20:00",
    );
    expect(toLocalDateTime("25/11/2026", "07:26", "fr-FR")).toBe(
      "2026-11-25T07:26:00",
    );
  });

  it("is null while the date or the time is incomplete", () => {
    expect(toLocalDateTime("", "07:26", "en-US", now)).toBeNull();
    expect(toLocalDateTime("11/25/2026", "7:2", "en-US", now)).toBeNull();
  });
});

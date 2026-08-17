import { formatDateOnly } from "../Utils";

describe("formatDateOnly", () => {
  test("keeps the entered calendar date for an end-of-day deadline", () => {
    // stored as 23:59:59 on the 30th; reading local components would roll this
    // to 01/10 for any browser east of the server
    expect(formatDateOnly("2026-09-30T23:59:59Z")).toBe("30/09/2026");
  });

  test("renders a plain date string as dd/mm/yyyy", () => {
    expect(formatDateOnly("2026-09-30")).toBe("30/09/2026");
  });

  test("returns an empty string for absent or unparseable values", () => {
    expect(formatDateOnly(null)).toBe("");
    expect(formatDateOnly("")).toBe("");
    expect(formatDateOnly("not a date")).toBe("");
  });
});

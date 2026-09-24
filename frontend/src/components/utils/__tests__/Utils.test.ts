import { convertAlphaNumLabNumForDisplay, formatDateOnly } from "../Utils";

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

describe("convertAlphaNumLabNumForDisplay", () => {
  test("keeps every part of a lab number carrying more than one dash", () => {
    // An EQA blind code is IH-<cycle>-<sample>. Keeping only the first part
    // after the split rendered every row on the workplan as "IH-2".
    expect(convertAlphaNumLabNumForDisplay("IH-2-04")).toBe("IH-2-04");
    expect(convertAlphaNumLabNumForDisplay("IH-12-07-1")).toBe("IH-12-07-1");
  });

  test("still formats a legacy dashed lab number the same way", () => {
    expect(convertAlphaNumLabNumForDisplay("20260900123-1")).toBe(
      "20-260-900-123-1",
    );
    expect(convertAlphaNumLabNumForDisplay("20260900123")).toBe(
      "20-260-900-123",
    );
  });

  test("passes absent and opaque values straight through", () => {
    expect(convertAlphaNumLabNumForDisplay(null)).toBeNull();
    expect(convertAlphaNumLabNumForDisplay("DEV01263000000000001")).toBe(
      "DEV01263000000000001",
    );
  });
});

import { describe, expect, it } from "vitest";
import { resultFlagFor } from "./resultFlagFor";

// Normal 5–100, Critical 2–150, Valid 0–1000, with the server's flag for the
// stored value alongside.
const row = (overrides: Record<string, unknown> = {}) => ({
  resultType: "N",
  resultLimitId: "7",
  lowerNormalRange: 5,
  upperNormalRange: 100,
  lowerCritical: 2,
  higherCritical: 150,
  lowerAbnormalRange: 0,
  upperAbnormalRange: 1000,
  resultFlag: "NORMAL",
  ...overrides,
});

describe("resultFlagFor", () => {
  it("judges the value the row holds against its bounds", () => {
    expect(resultFlagFor(row({ resultValue: "50" }))).toBe("NORMAL");
    expect(resultFlagFor(row({ resultValue: "120" }))).toBe("ABNORMAL");
    expect(resultFlagFor(row({ resultValue: "200" }))).toBe("CRITICAL");
    expect(resultFlagFor(row({ resultValue: "2000" }))).toBe("INVALID");
  });

  it("a value typed over a stored one is judged afresh, not by the stored flag", () => {
    expect(
      resultFlagFor(row({ resultValue: "200", resultFlag: "NORMAL" })),
    ).toBe("CRITICAL");
    expect(
      resultFlagFor(row({ resultValue: "50", resultFlag: "CRITICAL" })),
    ).toBe("NORMAL");
  });

  it("reads written scientific notation and an analyzer's < or > prefix", () => {
    expect(resultFlagFor(row({ resultValue: "1.2×10²" }))).toBe("ABNORMAL");
    expect(resultFlagFor(row({ resultValue: "2e2" }))).toBe("CRITICAL");
    expect(resultFlagFor(row({ resultValue: "<50" }))).toBe("NORMAL");
  });

  it("carries no flag for a blank or unparseable value", () => {
    expect(resultFlagFor(row({ resultValue: "" }))).toBeUndefined();
    expect(resultFlagFor(row({ resultValue: "   " }))).toBeUndefined();
    expect(resultFlagFor(row({ resultValue: "abc" }))).toBeUndefined();
  });

  it("keeps the server's flag for a row that is not numeric", () => {
    expect(
      resultFlagFor(
        row({ resultType: "D", resultValue: "1578", resultFlag: "ABNORMAL" }),
      ),
    ).toBe("ABNORMAL");
  });

  it("keeps the server's verdict when no result limit applies to the row", () => {
    expect(
      resultFlagFor(
        row({ resultLimitId: null, resultValue: "9999", resultFlag: null }),
      ),
    ).toBeUndefined();
  });
});

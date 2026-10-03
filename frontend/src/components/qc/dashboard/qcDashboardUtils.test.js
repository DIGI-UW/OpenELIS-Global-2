import { describe, expect, it } from "vitest";

import { getZScoreBadgeType } from "./qcDashboardUtils";

describe("getZScoreBadgeType", () => {
  it.each([
    [0, "green"],
    [-1.99, "green"],
    [2, "warm-gray"],
    [-2.99, "warm-gray"],
    [3, "red"],
    [-4, "red"],
    [null, "gray"],
  ])("maps z-score %s to %s", (zScore, expectedType) => {
    expect(getZScoreBadgeType(zScore)).toBe(expectedType);
  });
});

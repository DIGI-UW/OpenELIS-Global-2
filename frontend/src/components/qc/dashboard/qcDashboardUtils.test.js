import { describe, expect, it } from "vitest";

import {
  getComplianceTagType,
  getSeverityTagType,
  getZScoreBadgeType,
} from "./qcDashboardUtils";

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

describe("getComplianceTagType", () => {
  it.each([
    ["GREEN", "green"],
    ["YELLOW", "warm-gray"],
    ["RED", "red"],
    ["NOT_CONFIGURED", "gray"],
    ["green", "green"],
    ["UNKNOWN", "gray"],
    [null, "gray"],
    [undefined, "gray"],
  ])("maps compliance color %s to %s", (complianceColor, expectedType) => {
    expect(getComplianceTagType(complianceColor)).toBe(expectedType);
  });
});

describe("getSeverityTagType", () => {
  it.each([
    ["REJECTION", "red"],
    ["WARNING", "warm-gray"],
    ["UNKNOWN", "warm-gray"],
    [null, "warm-gray"],
    [undefined, "warm-gray"],
  ])("maps severity %s to %s", (severity, expectedType) => {
    expect(getSeverityTagType(severity)).toBe(expectedType);
  });
});

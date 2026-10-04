/**
 * Unit tests for qcDashboardUtils.js
 *
 * First test coverage for the QC dashboard utilities.
 * Covers the bug fixed in issue #4152: getZScoreBadgeType was mapping
 * Westgard SD-index z-scores backwards (bigger = green when it should be red).
 */

import {
  getZScoreBadgeType,
  getComplianceTagType,
  getSeverityTagType,
} from "./qcDashboardUtils";

// ---------------------------------------------------------------------------
// getZScoreBadgeType
// ---------------------------------------------------------------------------
describe("getZScoreBadgeType", () => {
  // --- null / missing data ---
  it("returns 'gray' for null", () => {
    expect(getZScoreBadgeType(null)).toBe("gray");
  });

  it("returns 'gray' for undefined", () => {
    expect(getZScoreBadgeType(undefined)).toBe("gray");
  });

  it("returns 'gray' for a non-numeric string", () => {
    expect(getZScoreBadgeType("abc")).toBe("gray");
  });

  // --- in-control: |z| < 2 → green ---
  it("returns 'green' for z = -0.8  (in-control)", () => {
    expect(getZScoreBadgeType(-0.8)).toBe("green");
  });

  it("returns 'green' for z =  0.1  (in-control)", () => {
    expect(getZScoreBadgeType(0.1)).toBe("green");
  });

  it("returns 'green' for z =  0.5  (in-control)", () => {
    expect(getZScoreBadgeType(0.5)).toBe("green");
  });

  it("returns 'green' for z =  1.0  (in-control)", () => {
    expect(getZScoreBadgeType(1.0)).toBe("green");
  });

  it("returns 'green' for z =  1.99 (just below 1_2s boundary)", () => {
    expect(getZScoreBadgeType(1.99)).toBe("green");
  });

  // --- 1_2s warning: 2 <= |z| < 3 → warm-gray ---
  it("returns 'warm-gray' for z =  2.0  (exactly at 1_2s boundary)", () => {
    expect(getZScoreBadgeType(2.0)).toBe("warm-gray");
  });

  it("returns 'warm-gray' for z = -2.5  (1_2s warning zone)", () => {
    expect(getZScoreBadgeType(-2.5)).toBe("warm-gray");
  });

  it("returns 'warm-gray' for z =  2.99 (just below 1_3s boundary)", () => {
    expect(getZScoreBadgeType(2.99)).toBe("warm-gray");
  });

  // --- 1_3s rejection: |z| >= 3 → red ---
  it("returns 'red' for z =  3.0  (exactly at 1_3s boundary)", () => {
    expect(getZScoreBadgeType(3.0)).toBe("red");
  });

  it("returns 'red' for z =  3.5  (rejection)", () => {
    expect(getZScoreBadgeType(3.5)).toBe("red");
  });

  it("returns 'red' for z =  3.8  (rejection)", () => {
    expect(getZScoreBadgeType(3.8)).toBe("red");
  });

  it("returns 'red' for z =  4.2  (worst fixture value - was wrongly green before fix)", () => {
    expect(getZScoreBadgeType(4.2)).toBe("red");
  });

  it("returns 'red' for negative rejection z = -3.5", () => {
    expect(getZScoreBadgeType(-3.5)).toBe("red");
  });

  // --- string numbers ---
  it("handles z-score passed as a string", () => {
    expect(getZScoreBadgeType("3.2")).toBe("red");
    expect(getZScoreBadgeType("1.5")).toBe("green");
    expect(getZScoreBadgeType("2.5")).toBe("warm-gray");
  });
});

// ---------------------------------------------------------------------------
// getComplianceTagType
// ---------------------------------------------------------------------------
describe("getComplianceTagType", () => {
  it("maps GREEN  -> 'green'", () => {
    expect(getComplianceTagType("GREEN")).toBe("green");
  });

  it("maps YELLOW -> 'warm-gray'", () => {
    expect(getComplianceTagType("YELLOW")).toBe("warm-gray");
  });

  it("maps RED    -> 'red'", () => {
    expect(getComplianceTagType("RED")).toBe("red");
  });

  it("maps lowercase 'green' -> 'green' (case-insensitive)", () => {
    expect(getComplianceTagType("green")).toBe("green");
  });

  it("returns 'gray' for null", () => {
    expect(getComplianceTagType(null)).toBe("gray");
  });

  it("returns 'gray' for an unknown value", () => {
    expect(getComplianceTagType("BLUE")).toBe("gray");
  });
});

// ---------------------------------------------------------------------------
// getSeverityTagType
// ---------------------------------------------------------------------------
describe("getSeverityTagType", () => {
  it("maps REJECTION -> 'red'", () => {
    expect(getSeverityTagType("REJECTION")).toBe("red");
  });

  it("maps WARNING -> 'warm-gray'", () => {
    expect(getSeverityTagType("WARNING")).toBe("warm-gray");
  });

  it("maps any other string -> 'warm-gray' (safe default)", () => {
    expect(getSeverityTagType("UNKNOWN")).toBe("warm-gray");
  });
});

import { computeReportedValue, dilutionApplies } from "./dilution";

/** OGC-1021 (R2) — FR-D5: reported result = measured value × dilution factor. */
describe("dilution (FR-D5)", () => {
  it("applies to numeric results only", () => {
    expect(dilutionApplies("N")).toBe(true);
    expect(dilutionApplies("D")).toBe(false);
    expect(dilutionApplies("M")).toBe(false);
    expect(dilutionApplies(undefined)).toBe(false);
  });

  it("multiplies measured value by the factor", () => {
    expect(computeReportedValue("50", "10")).toBe("500");
  });

  it("keeps the measured value's decimal places", () => {
    expect(computeReportedValue("2.50", "4")).toBe("10.00");
    expect(computeReportedValue("1.5", "3")).toBe("4.5");
  });

  /**
   * OGC-1408 — a product finer than the test reports to would be refused by
   * the precision gate, leaving Save disabled on a value the panel itself
   * produced; it is rounded to the test's precision instead.
   */
  it("rounds to the test's precision when the measured value is finer than it reports to", () => {
    expect(computeReportedValue("3.5", "2", 0)).toBe("7");
    expect(computeReportedValue("1.25", "3", 1)).toBe("3.8");
    expect(computeReportedValue("1.5", "3", 2)).toBe("4.5");
    expect(computeReportedValue("50", "10", 0)).toBe("500");
    expect(computeReportedValue("2.50", "4", undefined)).toBe("10.00");
  });

  it("returns null for unusable input so the caller leaves the value alone", () => {
    expect(computeReportedValue("", "10")).toBeNull();
    expect(computeReportedValue("50", "")).toBeNull();
    expect(computeReportedValue("abc", "10")).toBeNull();
    expect(computeReportedValue("50", "0")).toBeNull();
    expect(computeReportedValue("50", "-2")).toBeNull();
  });
});

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

  it("returns null for unusable input so the caller leaves the value alone", () => {
    expect(computeReportedValue("", "10")).toBeNull();
    expect(computeReportedValue("50", "")).toBeNull();
    expect(computeReportedValue("abc", "10")).toBeNull();
    expect(computeReportedValue("50", "0")).toBeNull();
    expect(computeReportedValue("50", "-2")).toBeNull();
  });
});

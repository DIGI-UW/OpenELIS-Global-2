import { computeDilutedValue, dilutionApplies } from "./dilution";

/** OGC-1021 (R2) — FR-D5: reported result = measured value × dilution factor. */
describe("dilution (FR-D5)", () => {
  it("applies to numeric results only", () => {
    expect(dilutionApplies("N")).toBe(true);
    expect(dilutionApplies("D")).toBe(false);
    expect(dilutionApplies("M")).toBe(false);
    expect(dilutionApplies(undefined)).toBe(false);
  });

  it("multiplies measured value by the factor", () => {
    expect(computeDilutedValue("50", "10")).toBe("500");
  });

  it("keeps the measured value's decimal places", () => {
    expect(computeDilutedValue("2.50", "4")).toBe("10.00");
    expect(computeDilutedValue("1.5", "3")).toBe("4.5");
  });

  it("preserves the product independently of report formatting", () => {
    expect(computeDilutedValue("1.25", "3")).toBe("3.75");
    expect(computeDilutedValue("2", "1.25")).toBe("2.50");
    expect(computeDilutedValue("0.1", "0.2")).toBe("0.02");
    expect(computeDilutedValue("1.25e-2", "3")).toBe("0.0375");
    expect(computeDilutedValue("-1.25", "3")).toBe("-3.75");
  });

  it("returns null for unusable input so the caller leaves the value alone", () => {
    expect(computeDilutedValue("", "10")).toBeNull();
    expect(computeDilutedValue("50", "")).toBeNull();
    expect(computeDilutedValue("abc", "10")).toBeNull();
    expect(computeDilutedValue("50", "0")).toBeNull();
    expect(computeDilutedValue("50", "-2")).toBeNull();
  });
});

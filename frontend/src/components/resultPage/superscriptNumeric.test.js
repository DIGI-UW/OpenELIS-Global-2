import { convertSuperscriptToScientific } from "./superscriptNumeric";

describe("convertSuperscriptToScientific", () => {
  it("rewrites '3²' as scientific notation that isNaN accepts", () => {
    const converted = convertSuperscriptToScientific("3²");
    expect(converted).toBe("3e2");
    expect(isNaN(converted)).toBe(false);
    expect(Number(converted)).toBe(300);
  });

  it("handles a decimal mantissa", () => {
    expect(convertSuperscriptToScientific("3.5³")).toBe("3.5e3");
    expect(Number(convertSuperscriptToScientific("3.5³"))).toBe(3500);
  });

  it("handles multi-digit and negative exponents", () => {
    expect(convertSuperscriptToScientific("3¹⁰")).toBe("3e10");
    expect(convertSuperscriptToScientific("3⁻²")).toBe("3e-2");
    expect(Number(convertSuperscriptToScientific("3⁻²"))).toBeCloseTo(0.03);
  });

  it("leaves plain numeric strings and ASCII scientific notation unchanged", () => {
    expect(convertSuperscriptToScientific("3")).toBe("3");
    expect(convertSuperscriptToScientific("3.14")).toBe("3.14");
    expect(convertSuperscriptToScientific("3e2")).toBe("3e2");
    expect(convertSuperscriptToScientific("-45.67")).toBe("-45.67");
  });

  it("keeps non-numeric strings non-numeric", () => {
    // A superscript with no numeric prefix is not a real exponent; leave it.
    expect(convertSuperscriptToScientific("²")).toBe("²");
    expect(convertSuperscriptToScientific("abc")).toBe("abc");
    expect(convertSuperscriptToScientific("")).toBe("");
  });

  it("returns non-string inputs unchanged", () => {
    expect(convertSuperscriptToScientific(null)).toBe(null);
    expect(convertSuperscriptToScientific(undefined)).toBe(undefined);
    expect(convertSuperscriptToScientific(42)).toBe(42);
  });
});

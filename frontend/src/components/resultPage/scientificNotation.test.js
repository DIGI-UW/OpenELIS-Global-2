import {
  decimalPlaces,
  exceedsDecimalPlaces,
  isExponentNotation,
  normalizeScientificNotation,
  roundToDecimalPlaces,
  splitExponentNotation,
} from "./scientificNotation";

describe("normalizeScientificNotation", () => {
  it("rewrites every written form as canonical e-notation", () => {
    expect(normalizeScientificNotation("1.5e5")).toBe("1.5e5");
    expect(normalizeScientificNotation("1.5E+05")).toBe("1.5e5");
    expect(normalizeScientificNotation("1.5 x 10^5")).toBe("1.5e5");
    expect(normalizeScientificNotation("1.5×10⁵")).toBe("1.5e5");
    expect(normalizeScientificNotation(" 1.5*10^5 ")).toBe("1.5e5");
    expect(normalizeScientificNotation("2×10⁻³")).toBe("2e-3");
    expect(normalizeScientificNotation("10⁻³")).toBe("1e-3");
    expect(normalizeScientificNotation("-10^3")).toBe("-1e3");
    expect(normalizeScientificNotation("-2.5E-07")).toBe("-2.5e-7");
  });

  it("produces values Number() accepts", () => {
    expect(Number(normalizeScientificNotation("1.5×10⁵"))).toBe(150000);
    expect(Number(normalizeScientificNotation("2×10⁻³"))).toBeCloseTo(0.002);
    expect(isNaN(normalizeScientificNotation("1.5 x 10^5"))).toBe(false);
  });

  it("leaves plain decimals and text unchanged", () => {
    expect(normalizeScientificNotation("3")).toBe("3");
    expect(normalizeScientificNotation("3.14")).toBe("3.14");
    expect(normalizeScientificNotation("-45.67")).toBe("-45.67");
    expect(normalizeScientificNotation("abc")).toBe("abc");
    expect(normalizeScientificNotation("")).toBe("");
  });

  it("does not read a bare superscript as a power of ten", () => {
    expect(normalizeScientificNotation("3²")).toBe("3²");
    expect(isNaN(normalizeScientificNotation("3²"))).toBe(true);
    expect(normalizeScientificNotation("3.5⁻³")).toBe("3.5⁻³");
    expect(normalizeScientificNotation("²")).toBe("²");
  });

  it("returns non-string inputs unchanged", () => {
    expect(normalizeScientificNotation(null)).toBe(null);
    expect(normalizeScientificNotation(undefined)).toBe(undefined);
    expect(normalizeScientificNotation(42)).toBe(42);
  });
});

describe("splitExponentNotation and isExponentNotation", () => {
  it("splits canonical e-notation into mantissa and exponent", () => {
    expect(splitExponentNotation("1.5e5")).toEqual({
      mantissa: "1.5",
      exponent: "5",
    });
    expect(splitExponentNotation("-2E-3")).toEqual({
      mantissa: "-2",
      exponent: "-3",
    });
    expect(splitExponentNotation("150000")).toBeNull();
    expect(splitExponentNotation("1.5×10⁵")).toBeNull();
    expect(isExponentNotation("1.5e5")).toBe(true);
    expect(isExponentNotation("12.5")).toBe(false);
  });
});

describe("decimalPlaces", () => {
  it("counts the places of a plain decimal", () => {
    expect(decimalPlaces("23")).toBe(0);
    expect(decimalPlaces("23.7")).toBe(1);
    expect(decimalPlaces("5.1234")).toBe(4);
  });

  it("counts the mantissa's places for e-notation", () => {
    expect(decimalPlaces("1.5e5")).toBe(1);
    expect(decimalPlaces("3e2")).toBe(0);
    expect(decimalPlaces("1.25e-7")).toBe(2);
  });
});

describe("exceedsDecimalPlaces", () => {
  it("compares a plain decimal's places with the test's", () => {
    expect(exceedsDecimalPlaces("23.7", 0)).toBe(true);
    expect(exceedsDecimalPlaces("23", 0)).toBe(false);
    expect(exceedsDecimalPlaces("5.1234", 2)).toBe(true);
    expect(exceedsDecimalPlaces("5.12", 2)).toBe(false);
  });

  it("compares the mantissa's places when the test reports decimals", () => {
    expect(exceedsDecimalPlaces("1.5e1", 1)).toBe(false);
    expect(exceedsDecimalPlaces("1.55e1", 1)).toBe(true);
    expect(exceedsDecimalPlaces("1.234e5", 2)).toBe(true);
    expect(exceedsDecimalPlaces("1.5e-7", 2)).toBe(false);
  });

  it("does not constrain a mantissa for a whole-number test", () => {
    expect(exceedsDecimalPlaces("1.5e5", 0)).toBe(false);
    expect(exceedsDecimalPlaces("1.234e5", 0)).toBe(false);
  });

  it("says nothing when the test declares no precision", () => {
    expect(exceedsDecimalPlaces("23.7", undefined)).toBe(false);
    expect(exceedsDecimalPlaces("23.7", -1)).toBe(false);
    expect(exceedsDecimalPlaces("23.7", NaN)).toBe(false);
  });
});

describe("roundToDecimalPlaces", () => {
  it("rounds the mantissa and keeps the power of ten", () => {
    expect(roundToDecimalPlaces("1.567e5", 2)).toBe("1.57e5");
    expect(roundToDecimalPlaces("1.5e-7", 2)).toBe("1.50e-7");
    expect(roundToDecimalPlaces("2.25e3", 0)).toBe("2e3");
  });

  it("rounds a plain decimal as before", () => {
    expect(roundToDecimalPlaces("3.456", 1)).toBe("3.5");
    expect(roundToDecimalPlaces("12.5", 0)).toBe("13");
  });
});

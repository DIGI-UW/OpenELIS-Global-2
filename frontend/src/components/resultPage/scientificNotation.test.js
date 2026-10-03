import {
  decimalPlaces,
  exceedsDecimalPlaces,
  isScientificNotation,
  normalizeScientificNotation,
  roundMantissa,
  splitMantissa,
} from "./scientificNotation";

describe("normalizeScientificNotation", () => {
  it("reads every written form as the same number", () => {
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

describe("isScientificNotation", () => {
  it("recognises every written form and nothing else", () => {
    expect(isScientificNotation("1.5e5")).toBe(true);
    expect(isScientificNotation("1.5E+05")).toBe(true);
    expect(isScientificNotation("1.5 x 10^5")).toBe(true);
    expect(isScientificNotation("1.5×10⁵")).toBe(true);
    expect(isScientificNotation("10⁻³")).toBe(true);
    expect(isScientificNotation("<1.5e-3")).toBe(true);
    expect(isScientificNotation("150000")).toBe(false);
    expect(isScientificNotation("3²")).toBe(false);
    expect(isScientificNotation(null)).toBe(false);
  });
});

describe("splitMantissa", () => {
  it("separates the mantissa from the notation around it", () => {
    expect(splitMantissa("1.5×10⁵")).toEqual({
      prefix: "",
      mantissa: "1.5",
      rest: "×10⁵",
    });
    expect(splitMantissa("<2.5e-3")).toEqual({
      prefix: "<",
      mantissa: "2.5",
      rest: "e-3",
    });
    expect(splitMantissa("12.5")).toEqual({
      prefix: "",
      mantissa: "12.5",
      rest: "",
    });
  });

  it("reports no mantissa for a bare power of ten", () => {
    expect(splitMantissa("10⁻³")).toBeNull();
    expect(splitMantissa("10^-3")).toBeNull();
    expect(splitMantissa("abc")).toBeNull();
  });
});

describe("decimalPlaces", () => {
  it("counts the places of a plain decimal", () => {
    expect(decimalPlaces("23")).toBe(0);
    expect(decimalPlaces("23.7")).toBe(1);
    expect(decimalPlaces("5.1234")).toBe(4);
  });

  it("counts the mantissa's places whatever the notation", () => {
    expect(decimalPlaces("1.5e5")).toBe(1);
    expect(decimalPlaces("1.5×10⁵")).toBe(1);
    expect(decimalPlaces("3.567 x 10^3")).toBe(3);
    expect(decimalPlaces("3e2")).toBe(0);
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
    expect(exceedsDecimalPlaces("1.5×10¹", 1)).toBe(false);
    expect(exceedsDecimalPlaces("1.55×10¹", 1)).toBe(true);
    expect(exceedsDecimalPlaces("3.567 x 10^3", 2)).toBe(true);
    expect(exceedsDecimalPlaces("1.5e-7", 2)).toBe(false);
  });

  it("does not constrain a mantissa for a whole-number test", () => {
    expect(exceedsDecimalPlaces("1.5e5", 0)).toBe(false);
    expect(exceedsDecimalPlaces("1.234×10⁵", 0)).toBe(false);
  });

  it("says nothing when the test declares no precision", () => {
    expect(exceedsDecimalPlaces("23.7", undefined)).toBe(false);
    expect(exceedsDecimalPlaces("23.7", -1)).toBe(false);
    expect(exceedsDecimalPlaces("23.7", NaN)).toBe(false);
  });
});

describe("roundMantissa", () => {
  it("rounds in place, leaving the notation exactly as written", () => {
    expect(roundMantissa("3.567 x 10^3", 2)).toBe("3.57 x 10^3");
    expect(roundMantissa("1.567×10⁵", 2)).toBe("1.57×10⁵");
    expect(roundMantissa("1.567E+05", 2)).toBe("1.57E+05");
    expect(roundMantissa("<1.567e-7", 2)).toBe("<1.57e-7");
  });

  it("rounds a plain decimal as before", () => {
    expect(roundMantissa("3.456", 1)).toBe("3.5");
    expect(roundMantissa("12.5", 0)).toBe("13");
  });

  it("leaves a bare power of ten alone", () => {
    expect(roundMantissa("10⁻³", 2)).toBe("10⁻³");
  });
});

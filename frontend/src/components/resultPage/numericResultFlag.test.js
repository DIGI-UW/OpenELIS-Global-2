/**
 * OGC-1121 — the legacy Results Entry screen must tell a critical value from a
 * merely abnormal one, with the server's precedence and without ever letting an
 * unauthored critical bound fire.
 */
import {
  ABNORMAL_BACKGROUND,
  CRITICAL_BACKGROUND,
  CRITICAL_BORDER,
  INVALID_BACKGROUND,
  classifyNumericResult,
  numericResultStyle,
} from "./numericResultFlag";

// Normal 5–100, Critical 2–150, Valid 0–1000 (the ticket's QA_AUTO_0706 setup).
const row = (overrides = {}) => ({
  lowerNormalRange: 5,
  upperNormalRange: 100,
  lowerCritical: 2,
  higherCritical: 150,
  lowerAbnormalRange: 0,
  upperAbnormalRange: 1000,
  ...overrides,
});

describe("classifyNumericResult", () => {
  it("120 is abnormal, 200 is critical, 50 is normal", () => {
    expect(classifyNumericResult("120", row())).toMatchObject({
      outsideNormal: true,
      isCritical: false,
      flag: "ABNORMAL",
    });
    expect(classifyNumericResult("200", row())).toMatchObject({
      outsideNormal: true,
      isCritical: true,
      flag: "CRITICAL",
    });
    expect(classifyNumericResult("50", row())).toMatchObject({
      outsideNormal: false,
      isCritical: false,
      flag: "NORMAL",
    });
  });

  it("a value below the low critical bound is critical too", () => {
    expect(classifyNumericResult("1", row()).flag).toBe("CRITICAL");
  });

  it("invalid beats critical, as on the server", () => {
    expect(classifyNumericResult("2000", row())).toMatchObject({
      isInvalid: true,
      outsideValid: true,
      isCritical: false,
      flag: "INVALID",
    });
  });

  it("an unauthored critical bound (null) never fires", () => {
    const noCritical = row({ lowerCritical: null, higherCritical: null });
    expect(classifyNumericResult("200", noCritical).flag).toBe("ABNORMAL");
    expect(classifyNumericResult("1", noCritical).flag).toBe("ABNORMAL");
  });

  it("one authored bound fires alone", () => {
    const highOnly = row({ lowerCritical: null });
    expect(classifyNumericResult("200", highOnly).flag).toBe("CRITICAL");
    expect(classifyNumericResult("1", highOnly).flag).toBe("ABNORMAL");
  });

  it("blank or non-numeric input carries no flag", () => {
    expect(classifyNumericResult("", row()).flag).toBeUndefined();
    expect(classifyNumericResult("abc", row()).flag).toBeUndefined();
  });
});

describe("numericResultStyle", () => {
  it("gives critical its own colour, distinct from abnormal yellow", () => {
    const critical = numericResultStyle(classifyNumericResult("200", row()));
    const abnormal = numericResultStyle(classifyNumericResult("120", row()));
    expect(critical.background).toBe(CRITICAL_BACKGROUND);
    expect(critical.borderColor).toBe(CRITICAL_BORDER);
    expect(abnormal.background).toBe(ABNORMAL_BACKGROUND);
    expect(abnormal.borderColor).toBe("");
    expect(critical.background).not.toBe(abnormal.background);
  });

  it("keeps the invalid colouring", () => {
    const invalid = numericResultStyle(classifyNumericResult("2000", row()));
    expect(invalid.background).toBe(INVALID_BACKGROUND);
    expect(invalid.borderColor).toBe("red");
  });
});

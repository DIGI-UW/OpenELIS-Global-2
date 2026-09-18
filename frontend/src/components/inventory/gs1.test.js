import { parseGs1, gtinMatches } from "./gs1";

const GS = "";

/**
 * The parser is the one piece of this feature that can be wrong silently: a
 * misread expiry date is a number that looks plausible and is not. These cases
 * are written against the published GS1 element-string rules rather than
 * against whatever one supplier happens to print.
 */
describe("parseGs1", () => {
  it("reads a GTIN, expiry and lot from one element string", () => {
    const result = parseGs1("010890123456789017271231101A2B3C");

    expect(result.recognised).toBe(true);
    expect(result.fields.gtin).toBe("08901234567890");
    expect(result.fields.expirationDate).toBe("2027-12-31");
    expect(result.fields.lotNumber).toBe("1A2B3C");
  });

  /**
   * Batch is variable length, so where it ends depends on a separator. Printed
   * last it runs to the end of the string; printed first it must stop at one.
   */
  it("ends a variable-length lot at the group separator, not at the string end", () => {
    const result = parseGs1(`10LOT-99${GS}0108901234567890`);

    expect(result.fields.lotNumber).toBe("LOT-99");
    expect(result.fields.gtin).toBe("08901234567890");
  });

  it("reads a lot that runs to the end when nothing follows it", () => {
    const result = parseGs1("010890123456789010LOT-99");

    expect(result.fields.lotNumber).toBe("LOT-99");
  });

  it("does not need a separator after a fixed-length field", () => {
    const result = parseGs1("17271231" + "0108901234567890");

    expect(result.fields.expirationDate).toBe("2027-12-31");
    expect(result.fields.gtin).toBe("08901234567890");
  });

  /**
   * A GS1 day of 00 means the end of that month, which reagent labels use.
   * Written with the GTIN in front, because an expiry alone is not a scan a
   * label ever carries — see the EAN-8 case below for why that matters.
   */
  it("reads a day of 00 as the last day of the month", () => {
    const at = (date) =>
      parseGs1(`010890123456789017${date}`).fields.expirationDate;

    expect(at("270200")).toBe("2027-02-28");
    expect(at("240200")).toBe("2024-02-29");
    expect(at("271100")).toBe("2027-11-30");
  });

  /**
   * Eight digits are ambiguous: an EAN-8 product code, or the identifier 17
   * followed by a six-digit date. A product code wins, because an expiry with
   * nothing to attach it to identifies nothing and no label prints one alone.
   */
  it("reads a bare eight-digit code as EAN-8, not as a lone expiry", () => {
    const result = parseGs1("17270200");

    expect(result.fields.gtin).toBe("17270200");
    expect(result.fields.expirationDate).toBeUndefined();
  });

  it("treats a plain product barcode as a GTIN rather than a failure", () => {
    expect(parseGs1("08901234567890").fields.gtin).toBe("08901234567890");
    expect(parseGs1("012345678905").fields.gtin).toBe("012345678905");
    expect(parseGs1("5901234123457").fields.gtin).toBe("5901234123457");
  });

  it("keeps the raw scan whatever happens to it", () => {
    expect(parseGs1("  0108901234567890  ").raw).toBe("0108901234567890");
    expect(parseGs1("not a barcode at all").raw).toBe("not a barcode at all");
  });

  /**
   * Guessing where an unknown identifier's value ends would corrupt every field
   * after it. Stopping and reporting the remainder is the honest answer.
   */
  it("stops at an identifier it does not know rather than guessing", () => {
    const result = parseGs1("010890123456789091SOMETHINGELSE");

    expect(result.fields.gtin).toBe("08901234567890");
    expect(result.unparsed).toContain("91SOMETHINGELSE");
    expect(result.fields.lotNumber).toBeUndefined();
  });

  it("reports an impossible date as unparsed instead of inventing one", () => {
    const result = parseGs1("011234567890123417271331");

    expect(result.fields.expirationDate).toBeUndefined();
    expect(result.unparsed.join("")).toContain("271331");
  });

  it("degrades to nothing recognised rather than throwing", () => {
    const result = parseGs1("QR-CODE-FROM-A-DIFFERENT-SYSTEM");

    expect(result.recognised).toBe(false);
    expect(result.raw).toBe("QR-CODE-FROM-A-DIFFERENT-SYSTEM");
  });

  it("handles an empty scan", () => {
    expect(parseGs1("").recognised).toBe(false);
    expect(parseGs1(null).raw).toBe("");
  });

  it("tolerates a leading separator some scanners emit", () => {
    expect(parseGs1(`${GS}0108901234567890`).fields.gtin).toBe(
      "08901234567890",
    );
  });

  it("reads a serial without mistaking it for a lot", () => {
    const result = parseGs1(`21SER-1${GS}10LOT-1`);

    expect(result.fields.serial).toBe("SER-1");
    expect(result.fields.lotNumber).toBe("LOT-1");
  });
});

/**
 * The same product is a 12-digit UPC on the box and a 14-digit GTIN in the
 * barcode. Comparing them as typed would fail to find stock already defined.
 */
describe("gtinMatches", () => {
  it("matches the same product written to different lengths", () => {
    expect(gtinMatches("00012345678905", "012345678905")).toBe(true);
    expect(gtinMatches("012345678905", "12345678905")).toBe(true);
  });

  it("does not match different products", () => {
    expect(gtinMatches("08901234567890", "08901234567891")).toBe(false);
  });

  it("does not match when either side is missing", () => {
    expect(gtinMatches(null, "012345678905")).toBe(false);
    expect(gtinMatches("012345678905", "")).toBe(false);
  });
});

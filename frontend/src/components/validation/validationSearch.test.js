import {
  isEmptySearch,
  isSingleLabNumber,
  parseLabNumberSearch,
  releaseScope,
  searchFromLocation,
  searchQueryString,
  validationEndpoint,
} from "./validationSearch";

/**
 * OGC-1418 — the one Validation search: what the search box names, how an old
 * address maps onto it, and what the page asks the server for.
 */
describe("parseLabNumberSearch", () => {
  it("reads one lab number as a range of one", () => {
    expect(parseLabNumberSearch(" DEV0126000770 ")).toEqual({
      labNumberFrom: "DEV0126000770",
      labNumberTo: "DEV0126000770",
    });
  });

  it("reads A..B, A - B and A to B as a range", () => {
    for (const text of ["ACC1..ACC9", "ACC1 - ACC9", "ACC1 to ACC9"]) {
      expect(parseLabNumberSearch(text)).toEqual({
        labNumberFrom: "ACC1",
        labNumberTo: "ACC9",
      });
    }
  });

  it("reads A.. as every lab number from A on", () => {
    expect(parseLabNumberSearch("ACC1..")).toEqual({
      labNumberFrom: "ACC1",
      labNumberTo: "",
    });
  });

  it("drops a sample item suffix, as the old lab number search did", () => {
    expect(parseLabNumberSearch("ACC1-2")).toEqual({
      labNumberFrom: "ACC1",
      labNumberTo: "ACC1",
    });
  });

  it("names nothing for an empty box", () => {
    expect(parseLabNumberSearch("  ")).toEqual({
      labNumberFrom: "",
      labNumberTo: "",
    });
  });
});

describe("searchFromLocation", () => {
  it("opens a bare /validation with an empty search", () => {
    expect(isEmptySearch(searchFromLocation("/validation", ""))).toBe(true);
  });

  it("maps every old address onto the equivalent filter", () => {
    expect(
      searchFromLocation("/validation", "?type=routine&testSectionId=7"),
    ).toMatchObject({ testSectionId: "7" });
    expect(
      searchFromLocation("/validation", "?type=order&accessionNumber=ACC1"),
    ).toMatchObject({ labNumber: "ACC1" });
    expect(
      searchFromLocation("/validation", "?type=range&accessionNumber=ACC1"),
    ).toMatchObject({ labNumber: "ACC1.." });
    expect(
      searchFromLocation("/validation", "?type=testDate&date=01/09/2026"),
    ).toMatchObject({ fromDate: "01/09/2026", toDate: "01/09/2026" });
    expect(
      searchFromLocation("/ResultValidation", "?testSectionId=7"),
    ).toMatchObject({ testSectionId: "7" });
    expect(
      searchFromLocation("/AccessionValidation", "?accessionNumber=ACC1"),
    ).toMatchObject({ labNumber: "ACC1" });
    expect(
      searchFromLocation("/AccessionValidationRange", "?accessionNumber=ACC1"),
    ).toMatchObject({ labNumber: "ACC1.." });
    expect(
      searchFromLocation("/ResultValidationByTestDate", "?date=01/09/2026"),
    ).toMatchObject({ fromDate: "01/09/2026", toDate: "01/09/2026" });
  });

  it("reads its own address back unchanged", () => {
    const search = {
      labNumber: "ACC1..ACC9",
      testSectionId: "7",
      fromDate: "01/09/2026",
      toDate: "30/09/2026",
      patientId: "42",
    };
    expect(
      searchFromLocation("/validation", searchQueryString(search)),
    ).toEqual(search);
  });
});

describe("validationEndpoint and releaseScope", () => {
  it("sends every criterion, combined", () => {
    const url = validationEndpoint({
      labNumber: "ACC1..ACC9",
      testSectionId: "7",
      fromDate: "01/09/2026",
      toDate: "",
      patientId: "42",
    });
    const params = new URLSearchParams(url.split("?")[1]);
    expect(url.startsWith("/rest/AccessionValidation?")).toBe(true);
    expect(Object.fromEntries(params)).toEqual({
      labNumberFrom: "ACC1",
      labNumberTo: "ACC9",
      testSectionId: "7",
      fromDate: "01/09/2026",
      toDate: "",
      patientId: "42",
    });
  });

  it("knows one lab number from a range", () => {
    expect(isSingleLabNumber({ labNumber: "ACC1" })).toBe(true);
    expect(isSingleLabNumber({ labNumber: "ACC1..ACC2" })).toBe(false);
    expect(releaseScope("?labNumber=ACC1&testSectionId=7")).toMatchObject({
      labNumberFrom: "ACC1",
      labNumberTo: "ACC1",
      testSectionId: "7",
    });
  });
});

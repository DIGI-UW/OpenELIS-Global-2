import {
  EMPTY_SAMPLE_SEARCH_FILTERS,
  buildFulfillmentSearchQuery,
  buildSampleSearchQuery,
  formatCollectionDate,
  hasActiveSearchFilters,
  sortSearchResults,
} from "./biorepoSampleSearchHelpers";

describe("biorepoSampleSearchHelpers", () => {
  test("hasActiveSearchFilters is false when all filters empty", () => {
    expect(hasActiveSearchFilters(EMPTY_SAMPLE_SEARCH_FILTERS)).toBe(false);
  });

  test("hasActiveSearchFilters is true when any filter has value", () => {
    expect(
      hasActiveSearchFilters({ ...EMPTY_SAMPLE_SEARCH_FILTERS, sampleType: "Plasma" }),
    ).toBe(true);
    expect(
      hasActiveSearchFilters({ ...EMPTY_SAMPLE_SEARCH_FILTERS, originLab: "CTD" }),
    ).toBe(true);
  });

  test("buildSampleSearchQuery combines filters and status", () => {
    const query = buildSampleSearchQuery(
      {
        ...EMPTY_SAMPLE_SEARCH_FILTERS,
        sampleType: "Plasma",
        originLab: "CTD",
      },
      { status: "STORED", limit: 25 },
    );

    expect(query).toContain("status=STORED");
    expect(query).toContain("limit=25");
    expect(query).toContain("sampleType=Plasma");
    expect(query).toContain("originLab=CTD");
  });

  test("buildFulfillmentSearchQuery omits sampleType when identity is set", () => {
    const query = buildFulfillmentSearchQuery(
      {
        ...EMPTY_SAMPLE_SEARCH_FILTERS,
        accessionNumber: "DEV012600000000000050",
        sampleType: "blood",
      },
      { status: "STORED" },
    );

    expect(query).toContain("identity=DEV012600000000000050");
    expect(query).not.toContain("sampleType=");
  });

  test("buildSampleSearchQuery adds browse flag", () => {
    const query = buildSampleSearchQuery(EMPTY_SAMPLE_SEARCH_FILTERS, {
      browse: true,
      status: "STORED",
    });
    expect(query).toContain("browse=true");
    expect(query).toContain("status=STORED");
  });

  test("sortSearchResults ranks exact barcode matches first", () => {
    const results = sortSearchResults(
      [
        { barcode: "BIO-PLASMA-2", collectionDate: "2026-01-01" },
        { barcode: "BIO-001", collectionDate: "2026-02-01" },
        { barcode: "BIO-001-ALT", collectionDate: "2026-03-01" },
      ],
      { ...EMPTY_SAMPLE_SEARCH_FILTERS, barcode: "BIO-001" },
    );

    expect(results[0].barcode).toBe("BIO-001");
  });

  test("formatCollectionDate returns dash for empty values", () => {
    expect(formatCollectionDate(null)).toBe("-");
    expect(formatCollectionDate("2026-05-01")).not.toBe("-");
  });
});

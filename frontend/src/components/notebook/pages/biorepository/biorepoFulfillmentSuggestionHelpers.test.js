import {
  buildBulkSuggestionRequest,
  buildSuggestionSummaryFromResults,
  formatTopCandidateIdentity,
  getSuggestionTagProps,
  isValidSuggestionApiMap,
  lookupSuggestion,
  mapApiSuggestionEntry,
  mapApiSuggestionResponse,
  mergeSuggestionMaps,
  normalizeRetrievalItemId,
  hasUsableTopSuggestion,
  SUGGESTION_STATUS,
  resolveAttachAvailableQuantity,
  resolveDefaultAttachQuantity,
  validateAttachQuantity,
} from "./biorepoFulfillmentSuggestionHelpers";

describe("biorepoFulfillmentSuggestionHelpers", () => {
  it("buildBulkSuggestionRequest packages item ids", () => {
    expect(buildBulkSuggestionRequest([1, 2])).toEqual({
      itemIds: [1, 2],
      identityLookups: [],
    });
  });

  it("normalizeRetrievalItemId and lookupSuggestion resolve string and numeric ids", () => {
    const map = mapApiSuggestionResponse({
      7: {
        retrievalItemId: 7,
        suggestionStatus: "EXACT_MATCH",
        topCandidate: { id: 99, accessionNumber: "ACC-7" },
        candidates: [{ id: 99 }],
      },
    });
    expect(lookupSuggestion(map, 7)?.status).toBe(SUGGESTION_STATUS.EXACT_MATCH);
    expect(lookupSuggestion(map, "7")?.topSuggestion.accessionNumber).toBe("ACC-7");
    expect(normalizeRetrievalItemId(7)).toBe("7");
  });

  it("mergeSuggestionMaps prefers primary entries", () => {
    const merged = mergeSuggestionMaps(
      { 1: { status: SUGGESTION_STATUS.EXACT_MATCH } },
      { 1: { status: SUGGESTION_STATUS.NO_CANDIDATE }, 2: { status: SUGGESTION_STATUS.REVIEW_SUGGESTIONS } },
    );
    expect(merged["1"].status).toBe(SUGGESTION_STATUS.EXACT_MATCH);
    expect(merged["2"].status).toBe(SUGGESTION_STATUS.REVIEW_SUGGESTIONS);
  });

  it("isValidSuggestionApiMap rejects errors and arrays", () => {
    expect(isValidSuggestionApiMap({ 1: {} })).toBe(true);
    expect(isValidSuggestionApiMap({ error: "fail" })).toBe(false);
    expect(isValidSuggestionApiMap({ status: 404 })).toBe(false);
    expect(isValidSuggestionApiMap({ statusCode: 500 })).toBe(false);
    expect(isValidSuggestionApiMap([])).toBe(false);
  });

  it("buildSuggestionSummaryFromResults prefers exact matches", () => {
    const summary = buildSuggestionSummaryFromResults([
      { id: 1, exactIdentityMatch: true, accessionNumber: "A1" },
      { id: 2, exactIdentityMatch: false },
    ]);
    expect(summary.status).toBe(SUGGESTION_STATUS.EXACT_MATCH);
    expect(summary.topSuggestion.id).toBe(1);
  });

  it("mapApiSuggestionEntry maps review suggestions", () => {
    const mapped = mapApiSuggestionEntry({
      suggestionStatus: "REVIEW_SUGGESTIONS",
      topCandidate: { id: 5, accessionNumber: "X" },
      candidates: [{ id: 5 }],
      fallbackUsed: true,
      noExactMatch: true,
    });
    expect(mapped.status).toBe(SUGGESTION_STATUS.REVIEW_SUGGESTIONS);
    expect(mapped.fallbackUsed).toBe(true);
  });

  it("formatTopCandidateIdentity combines accession and barcode", () => {
    expect(
      formatTopCandidateIdentity({ accessionNumber: "ACC-1", barcode: "BIO-1" }),
    ).toBe("ACC-1 · BIO-1");
  });

  it("getSuggestionTagProps returns green for exact match", () => {
    expect(getSuggestionTagProps(SUGGESTION_STATUS.EXACT_MATCH)).toEqual({ type: "green" });
  });

  it("hasUsableTopSuggestion accepts accession without id", () => {
    expect(
      hasUsableTopSuggestion({
        topSuggestion: { accessionNumber: "ACC-ONLY" },
      }),
    ).toBe(true);
  });

  it("validateAttachQuantity rejects over available", () => {
    const result = validateAttachQuantity(5, 3, "4");
    expect(result.valid).toBe(false);
    expect(result.errorKey).toContain("exceedsAvailable");
  });

  it("validateAttachQuantity accepts default requested quantity", () => {
    const result = validateAttachQuantity(2, 10, "");
    expect(result.valid).toBe(true);
    expect(result.quantity).toBe(2);
  });

  it("validateAttachQuantity caps implicit line default to stored available", () => {
    const bioSample = { remainingQuantity: 1, hierarchicalPath: "Room > Freezer" };
    const result = validateAttachQuantity(10, null, "10", { bioSample });
    expect(result.valid).toBe(true);
    expect(result.quantity).toBe(1);
  });

  it("resolveDefaultAttachQuantity prefers min of requested and available", () => {
    expect(
      resolveDefaultAttachQuantity(
        { quantityRequested: 10 },
        { remainingQuantity: 1, hierarchicalPath: "A" },
      ),
    ).toBe(1);
  });

  it("resolveAttachAvailableQuantity uses quantity hint when no ledger", () => {
    expect(resolveAttachAvailableQuantity({}, 5)).toBe(5);
  });
});

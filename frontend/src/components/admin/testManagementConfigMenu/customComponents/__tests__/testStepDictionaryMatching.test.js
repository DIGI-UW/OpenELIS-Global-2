import {
  normalizeDictionaryValue,
  isQualifiableMarker,
  hydrateDictionaryFromInitial,
  resolveDictionaryItemId,
} from "../testStepDictionaryMatching";

/**
 * OGC-525 — Test Modify wizard must round-trip the FULL dictionary set on
 * edit. Previously, hydrate-on-edit used `value.trim().split(" ")[0]` to
 * match initialData.dictionary entries against the master dictionaryList.
 * Single-token values like "Invalid" survived; multi-token values like
 * "DENGUE VIRUS TYPE2 DETECTED" silently dropped because `firstToken ===
 * "DENGUE"` never matched the full dictionary entry. Saving back persisted
 * only the survivors → silent data loss (Highest priority).
 *
 * These pure helpers extract the matching logic out of the component so the
 * fix is testable without rendering. Tests are written failing-first against
 * the broken first-token behavior, then green when the helpers compare on
 * the full normalized value.
 */

describe("normalizeDictionaryValue", () => {
  test("trims surrounding whitespace", () => {
    expect(normalizeDictionaryValue("  Invalid  ")).toBe("Invalid");
  });

  test("preserves multi-word content", () => {
    expect(normalizeDictionaryValue("DENGUE VIRUS TYPE2 DETECTED")).toBe(
      "DENGUE VIRUS TYPE2 DETECTED",
    );
  });

  test("strips a trailing 'qualifiable' sentinel (case-insensitive)", () => {
    expect(normalizeDictionaryValue("Invalid qualifiable")).toBe("Invalid");
    expect(normalizeDictionaryValue("Invalid QUALIFIABLE")).toBe("Invalid");
    expect(
      normalizeDictionaryValue("DENGUE VIRUS TYPE2 DETECTED qualifiable"),
    ).toBe("DENGUE VIRUS TYPE2 DETECTED");
  });

  test("returns empty string for nullish input", () => {
    expect(normalizeDictionaryValue(null)).toBe("");
    expect(normalizeDictionaryValue(undefined)).toBe("");
    expect(normalizeDictionaryValue("")).toBe("");
  });
});

describe("isQualifiableMarker", () => {
  test("detects the 'qualifiable' marker case-insensitively", () => {
    expect(isQualifiableMarker("Invalid qualifiable")).toBe(true);
    expect(isQualifiableMarker("Invalid QUALIFIABLE")).toBe(true);
    expect(isQualifiableMarker("Invalid")).toBe(false);
  });
});

describe("hydrateDictionaryFromInitial — OGC-525 regression lock", () => {
  const denguePcrSerum = [
    { id: "1001", value: "DENGUE VIRUS NOT DETECTED" },
    { id: "1002", value: "DENGUE VIRUS TYPE1 DETECTED" },
    { id: "1003", value: "DENGUE VIRUS TYPE2 DETECTED" },
    { id: "1004", value: "DENGUE VIRUS TYPE3 DETECTED" },
    { id: "1005", value: "Invalid" },
  ];

  test("preserves all 5 values for DENGUE PCR (Serum), including multi-word entries", () => {
    const initial = [
      { value: "DENGUE VIRUS NOT DETECTED" },
      { value: "DENGUE VIRUS TYPE1 DETECTED" },
      { value: "DENGUE VIRUS TYPE2 DETECTED" },
      { value: "DENGUE VIRUS TYPE3 DETECTED" },
      { value: "Invalid" },
    ];

    const result = hydrateDictionaryFromInitial(initial, denguePcrSerum);

    expect(result).toHaveLength(5);
    expect(result.map((r) => r.value)).toEqual([
      "DENGUE VIRUS NOT DETECTED",
      "DENGUE VIRUS TYPE1 DETECTED",
      "DENGUE VIRUS TYPE2 DETECTED",
      "DENGUE VIRUS TYPE3 DETECTED",
      "Invalid",
    ]);
    expect(result.every((r) => r.qualified === "N")).toBe(true);
  });

  test("marks qualified='Y' when the inline 'qualifiable' marker is present", () => {
    const initial = [
      { value: "DENGUE VIRUS TYPE2 DETECTED qualifiable" },
      { value: "Invalid" },
    ];
    const result = hydrateDictionaryFromInitial(initial, denguePcrSerum);
    expect(result).toHaveLength(2);
    expect(
      result.find((r) => r.value === "DENGUE VIRUS TYPE2 DETECTED").qualified,
    ).toBe("Y");
    expect(result.find((r) => r.value === "Invalid").qualified).toBe("N");
  });

  test("accepts plain-string entries (legacy payload shape)", () => {
    const initial = ["DENGUE VIRUS TYPE2 DETECTED", "Invalid"];
    const result = hydrateDictionaryFromInitial(initial, denguePcrSerum);
    expect(result.map((r) => r.value)).toEqual([
      "DENGUE VIRUS TYPE2 DETECTED",
      "Invalid",
    ]);
  });

  test("returns empty array for empty/missing input", () => {
    expect(hydrateDictionaryFromInitial(undefined, denguePcrSerum)).toEqual([]);
    expect(hydrateDictionaryFromInitial(null, denguePcrSerum)).toEqual([]);
    expect(hydrateDictionaryFromInitial([], denguePcrSerum)).toEqual([]);
  });

  test("drops entries that have no match in the master dictionary", () => {
    const initial = [
      { value: "DENGUE VIRUS TYPE2 DETECTED" },
      { value: "UNKNOWN PHANTOM VALUE" },
    ];
    const result = hydrateDictionaryFromInitial(initial, denguePcrSerum);
    expect(result).toHaveLength(1);
    expect(result[0].value).toBe("DENGUE VIRUS TYPE2 DETECTED");
  });
});

describe("resolveDictionaryItemId — for dictionaryReference + defaultTestResult", () => {
  const dictionaryList = [
    { id: "1003", value: "DENGUE VIRUS TYPE2 DETECTED" },
    { id: "1005", value: "Invalid" },
  ];

  test("resolves a multi-word value to its id", () => {
    expect(
      resolveDictionaryItemId("DENGUE VIRUS TYPE2 DETECTED", dictionaryList),
    ).toBe("1003");
  });

  test("strips a 'qualifiable' marker before matching", () => {
    expect(
      resolveDictionaryItemId(
        "DENGUE VIRUS TYPE2 DETECTED qualifiable",
        dictionaryList,
      ),
    ).toBe("1003");
  });

  test("returns null when no master entry matches", () => {
    expect(resolveDictionaryItemId("UNKNOWN", dictionaryList)).toBeNull();
    expect(resolveDictionaryItemId(null, dictionaryList)).toBeNull();
  });
});

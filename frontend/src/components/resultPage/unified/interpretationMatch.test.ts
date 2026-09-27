import {
  bucketMatches,
  bucketTagType,
  matchingBucket,
} from "./interpretationMatch";

/** OGC-1026 (R7, FR-G1) — bucket valueMatch grammar. */
describe("interpretationMatch (FR-G1)", () => {
  it("matches inclusive numeric ranges", () => {
    expect(bucketMatches("70-99", "70")).toBe(true);
    expect(bucketMatches("70-99", "99")).toBe(true);
    expect(bucketMatches("70-99", "99.5")).toBe(false);
    expect(bucketMatches("70 - 99", "85")).toBe(true);
  });

  it("matches one-sided bounds", () => {
    expect(bucketMatches(">=126", "126")).toBe(true);
    expect(bucketMatches(">=126", "125.9")).toBe(false);
    expect(bucketMatches("<70", "69.9")).toBe(true);
    expect(bucketMatches("<70", "70")).toBe(false);
    expect(bucketMatches("<=69", "69")).toBe(true);
    expect(bucketMatches(">400", "401")).toBe(true);
  });

  it("matches dictionary values exactly, case-insensitive", () => {
    expect(bucketMatches("Positive", "positive")).toBe(true);
    expect(bucketMatches("Positive", "Negative")).toBe(false);
  });

  it("unparseable expressions and blank values match nothing", () => {
    expect(bucketMatches("70..99", "80")).toBe(false);
    expect(bucketMatches(">=126", "abc")).toBe(false);
    expect(bucketMatches(">=126", "")).toBe(false);
    expect(bucketMatches(undefined, "80")).toBe(false);
  });

  it("matchingBucket honors display order", () => {
    const buckets = [
      { valueMatch: ">=126", text: "DM", displayOrder: 2 },
      { valueMatch: ">=100", text: "IFG", displayOrder: 1 },
    ];
    expect(matchingBucket(buckets, "142")?.text).toBe("IFG");
  });

  it("tag type prefers configured color, falls back on severity", () => {
    expect(bucketTagType({ color: "red" })).toBe("red");
    expect(bucketTagType({ severity: "CRITICAL" })).toBe("red");
    expect(bucketTagType({ severity: "NORMAL" })).toBe("green");
    expect(bucketTagType({})).toBe("cool-gray");
  });
});

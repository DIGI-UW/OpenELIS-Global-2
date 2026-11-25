import { describe, expect, it } from "vitest";
import { foldForSearch, matchesSearch } from "./searchText";

describe("searchText", () => {
  it("matches a typed fragment anywhere in the text, ignoring case", () => {
    expect(matchesSearch("Whole Blood", "BLOO")).toBe(true);
    expect(matchesSearch("Whole Blood", "serum")).toBe(false);
  });

  it("ignores accents on either side, so French labels match plain typing", () => {
    expect(matchesSearch("Hématologie", "hemato")).toBe(true);
    expect(matchesSearch("Hematologie", "hémato")).toBe(true);
    expect(matchesSearch("Sérologie VIH", "SEROLOGIE")).toBe(true);
  });

  it("matches everything for an empty or blank query", () => {
    expect(matchesSearch("Serum", "")).toBe(true);
    expect(matchesSearch("Serum", "   ")).toBe(true);
    expect(matchesSearch("Serum", undefined)).toBe(true);
  });

  it("reads a missing label as empty text", () => {
    expect(foldForSearch(null)).toBe("");
    expect(matchesSearch(undefined, "a")).toBe(false);
  });
});

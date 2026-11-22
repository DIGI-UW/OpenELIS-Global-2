import { analyzerPageTitle } from "./Index";

describe("analyzerPageTitle", () => {
  it("shows the analyzer's name once it is resolved", () => {
    expect(analyzerPageTitle("Analyzer", "Leonardo")).toBe(
      "Analyzer: Leonardo",
    );
  });

  it("never falls back to the id while the name is unresolved", () => {
    // the URL id must not leak into the title on first paint
    expect(analyzerPageTitle("Analyzer", "")).toBe("Analyzer");
  });

  it("degrades to the bare label for an id that matches no analyzer", () => {
    expect(analyzerPageTitle("Analyzer", undefined)).toBe("Analyzer");
    expect(analyzerPageTitle("Analyzer", null)).toBe("Analyzer");
  });
});

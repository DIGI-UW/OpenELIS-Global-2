import { buildAnalyzerResultsRedirectUrl } from "./AnalyserResults";

describe("buildAnalyzerResultsRedirectUrl", () => {
  it("preserves id-based routes after save", () => {
    expect(buildAnalyzerResultsRedirectUrl("id", "22")).toBe(
      "/AnalyzerResults?id=22",
    );
  });

  it("preserves legacy type-based routes after save", () => {
    expect(
      buildAnalyzerResultsRedirectUrl("type", "Demo: GeneXpert ASTM"),
    ).toBe("/AnalyzerResults?type=Demo: GeneXpert ASTM");
  });

  it("falls back to the analyzer landing page when no query is available", () => {
    expect(buildAnalyzerResultsRedirectUrl("id", "")).toBe("/AnalyzerResults");
  });
});

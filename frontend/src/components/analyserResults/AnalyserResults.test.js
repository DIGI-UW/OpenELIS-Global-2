import { getAnalyzerResultsView } from "./Index";

describe("getAnalyzerResultsView", () => {
  it("selects the canonical import-issues view from the query string", () => {
    expect(getAnalyzerResultsView("?view=import-issues")).toBe("import-issues");
  });
});

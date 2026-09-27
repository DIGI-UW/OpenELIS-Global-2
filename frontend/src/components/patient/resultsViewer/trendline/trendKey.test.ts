import { trendHash, parseTrendHash, trendQuery } from "./trendKey";

/**
 * The graph is reached through the URL hash, so the hash has to carry enough
 * to name one series. Before the timeline was rewritten it carried a test id
 * alone, which no longer identifies a single line.
 */
describe("trend key", () => {
  it("round-trips the series a link points at", () => {
    const key = {
      testId: "6",
      sampleTypeId: "26",
      componentId: "c-sys",
    };
    expect(parseTrendHash(trendHash(key))).toEqual(key);
  });

  it("keeps an unscoped test out of the hash rather than sending blanks", () => {
    expect(trendHash({ testId: "6" })).toBe("#trendline/testId=6");
    expect(parseTrendHash("#trendline/testId=6")).toEqual({
      testId: "6",
      sampleTypeId: undefined,
      componentId: undefined,
    });
  });

  it("still reads the bare test id links used before the key existed", () => {
    expect(parseTrendHash("#trendline/6")).toEqual({ testId: "6" });
  });

  it("names no series when the hash is not a trend hash", () => {
    expect(parseTrendHash("")).toBeNull();
    expect(parseTrendHash("#groupedtimeline")).toBeNull();
    expect(parseTrendHash("#trendline/")).toBeNull();
  });

  it("asks the server for exactly the series being graphed", () => {
    expect(
      trendQuery("7", {
        testId: "6",
        sampleTypeId: "26",
        componentId: "c-sys",
      }),
    ).toBe(
      "/rest/test-result-tree?patientId=7&testId=6&sampleTypeId=26&componentId=c-sys",
    );
    // A single-component test on one specimen has nothing to narrow by.
    expect(trendQuery("7", { testId: "6" })).toBe(
      "/rest/test-result-tree?patientId=7&testId=6",
    );
  });
});

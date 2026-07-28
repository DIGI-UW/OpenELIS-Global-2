import {
  buildAnalyzerListUrl,
  buildAnalyzerSetupUrl,
  buildAnalyzerQcRuleUrl,
  buildAnalyzerControlLotUrl,
  buildProfileCatalogUrl,
  parseAnalyzerListQuery,
  parseProfileCatalogQuery,
  resolveAnalyzerReturnTo,
} from "./analyzerRoutes";

describe("analyzer route contract", () => {
  it("serializes list state in canonical order and omits empty values", () => {
    expect(
      buildAnalyzerListUrl({
        analyzerType: "GeneXpert",
        testUnit: "",
        status: "SETUP",
        search: "  xpert  ",
      }),
    ).toBe("/analyzers?search=xpert&status=SETUP&analyzerType=GeneXpert");
  });

  it("parses list state without admitting setup parameters", () => {
    expect(
      parseAnalyzerListQuery(
        "?add=1&search=xpert&status=ACTIVE&testUnit=7&profile=astm%2Fgx",
      ),
    ).toEqual({
      search: "xpert",
      status: "ACTIVE",
      testUnit: "7",
      analyzerType: "",
    });
  });

  it("round-trips profile catalog state in canonical order", () => {
    expect(
      buildProfileCatalogUrl({
        readiness: "READY",
        search: "  xpert  ",
        protocol: "ASTM",
      }),
    ).toBe("/analyzers/types?search=xpert&protocol=ASTM&readiness=READY");

    expect(
      parseProfileCatalogQuery(
        "?readiness=READY&ignored=1&protocol=ASTM&search=xpert",
      ),
    ).toEqual({
      search: "xpert",
      protocol: "ASTM",
      readiness: "READY",
    });
  });

  it("builds each canonical setup step with stable query ordering", () => {
    const context = {
      analyzerId: "42",
      profileId: "astm/genexpert-astm",
      returnTo: "/analyzers/types?protocol=ASTM",
    };

    expect(buildAnalyzerSetupUrl("instrument", context)).toBe(
      "/analyzers?add=1&step=instrument&profile=astm%2Fgenexpert-astm&returnTo=%2Fanalyzers%2Ftypes%3Fprotocol%3DASTM",
    );
    expect(buildAnalyzerSetupUrl("verify", context)).toContain(
      "/analyzers/42/mappings?setup=1&step=verify",
    );
    expect(buildAnalyzerSetupUrl("connect", context)).toContain(
      "/analyzers/42/edit?setup=1&step=connect",
    );
    expect(buildAnalyzerSetupUrl("review", context)).toContain(
      "/analyzers/42/review?setup=1&step=review",
    );
  });

  it("builds QC detours that return to the verified setup URL", () => {
    const verifyUrl =
      "/analyzers/42/mappings?setup=1&step=verify&profile=astm%2Fgx";
    expect(buildAnalyzerQcRuleUrl("42", verifyUrl)).toBe(
      "/analyzers/42/qc-rules?returnTo=%2Fanalyzers%2F42%2Fmappings%3Fsetup%3D1%26step%3Dverify%26profile%3Dastm%252Fgx",
    );
    expect(buildAnalyzerControlLotUrl("42", verifyUrl)).toBe(
      "/analyzers/qc/control-lots/new?analyzerId=42&returnTo=%2Fanalyzers%2F42%2Fmappings%3Fsetup%3D1%26step%3Dverify%26profile%3Dastm%252Fgx",
    );
  });

  it.each([
    ["https://example.org/analyzers", "/analyzers"],
    ["//example.org/analyzers", "/analyzers"],
    ["javascript:alert(1)", "/analyzers"],
    ["/patients", "/analyzers"],
    ["not-a-path", "/analyzers"],
    [null, "/analyzers"],
  ])("rejects unsafe return target %s", (candidate, expected) => {
    expect(resolveAnalyzerReturnTo(candidate)).toBe(expected);
  });

  it("accepts analyzer application paths and preserves their query", () => {
    expect(
      resolveAnalyzerReturnTo("/analyzers/42/mappings?setup=1&step=verify"),
    ).toBe("/analyzers/42/mappings?setup=1&step=verify");
  });
});

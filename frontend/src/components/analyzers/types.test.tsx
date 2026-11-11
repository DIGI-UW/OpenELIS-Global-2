import { describe, expect, expectTypeOf, test } from "vitest";

import type {
  Analyzer,
  AnalyzerApiError,
  AnalyzerApiResponse,
  AnalyzerStatus,
} from "./types";

describe("analyzer TypeScript contracts", () => {
  test("restricts analyzer status and lifecycleStage to known lifecycle values", () => {
    const analyzer: Analyzer = {
      status: "ACTIVE",
      lifecycleStage: "ERROR_PENDING",
    };

    expect(analyzer.status).toBe("ACTIVE");
    expect(analyzer.lifecycleStage).toBe("ERROR_PENDING");
    expectTypeOf<Analyzer["status"]>().toEqualTypeOf<
      AnalyzerStatus | undefined
    >();
    expectTypeOf<Analyzer["lifecycleStage"]>().toEqualTypeOf<
      AnalyzerStatus | undefined
    >();
  });

  test("keeps OpenELIS analyzer identity separate from its Bridge connection", () => {
    const analyzer: Analyzer = {
      id: "42",
      name: "GeneXpert",
      analyzerType: "ASTM",
      type: "ASTM",
      testUnitIds: ["1001", 1002],
      active: true,
      protocol: "FILE",
      profileId: "genexpert-astm",
      profileRevision: 1,
      bridgeConnectionId: "bridge-42",
      connected: true,
    };

    expect(analyzer.testUnitIds).toEqual(["1001", 1002]);
    expectTypeOf<Analyzer["id"]>().toEqualTypeOf<string | undefined>();
    expectTypeOf<Analyzer["bridgeConnectionId"]>().toEqualTypeOf<
      string | null | undefined
    >();
    expectTypeOf<Analyzer["testUnitIds"]>().toEqualTypeOf<
      Array<string | number> | undefined
    >();
  });

  test("types structured analyzer API errors with field errors and message args", () => {
    const error: AnalyzerApiError = {
      status: 400,
      messageKey: "analyzer.error.validation",
      messageArgs: { analyzerName: "GeneXpert" },
      fieldErrors: [
        {
          field: "connectionValues",
          defaultMessage: "Connection values are invalid",
        },
      ],
    };

    expect(error.fieldErrors?.[0]?.field).toBe("connectionValues");
    expectTypeOf<AnalyzerApiError["messageArgs"]>().toEqualTypeOf<
      Record<string, unknown> | undefined
    >();
    expectTypeOf<AnalyzerApiError["fieldErrors"]>().toEqualTypeOf<
      Array<{ field?: string; defaultMessage?: string }> | undefined
    >();
  });

  test("keeps analyzer API responses open for endpoint-specific payload fields", () => {
    const response: AnalyzerApiResponse = {
      success: true,
      message: "Connection successful",
      connectionLatencyMs: 125,
      analyzer: { id: "42", status: "VALIDATION" },
    };

    expect(response.connectionLatencyMs).toBe(125);
    expectTypeOf<AnalyzerApiResponse>().toMatchTypeOf<AnalyzerApiError>();
    expectTypeOf<AnalyzerApiResponse>().toMatchTypeOf<
      Record<string, unknown>
    >();
  });
});

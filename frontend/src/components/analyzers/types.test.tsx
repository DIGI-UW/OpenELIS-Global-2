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

    // @ts-expect-error AnalyzerStatus must reject unsupported lifecycle values.
    const unsupportedStatus: AnalyzerStatus = "CONNECTED";
    expect(unsupportedStatus).toBe("CONNECTED");
  });

  test("allows analyzer identifiers and transport values returned by the API", () => {
    const analyzer: Analyzer = {
      id: "42",
      name: "GeneXpert",
      analyzerType: "ASTM",
      type: "ASTM",
      ipAddress: "192.0.2.10",
      port: "8080",
      importDirectory: "/var/openelis/analyzers/incoming",
      testUnitIds: ["1001", 1002],
      active: true,
      pluginLoaded: false,
      protocol: "FILE",
    };

    expect(analyzer.testUnitIds).toEqual(["1001", 1002]);
    expectTypeOf<Analyzer["id"]>().toEqualTypeOf<string | undefined>();
    expectTypeOf<Analyzer["port"]>().toEqualTypeOf<
      string | number | undefined
    >();
    expectTypeOf<Analyzer["testUnitIds"]>().toEqualTypeOf<
      Array<string | number> | undefined
    >();

    // @ts-expect-error testUnitIds only accepts string or number identifiers.
    const analyzerWithInvalidTestUnits: Analyzer = { testUnitIds: [true] };
    expect(analyzerWithInvalidTestUnits.testUnitIds).toEqual([true]);
  });

  test("types structured analyzer API errors with field errors and message args", () => {
    const error: AnalyzerApiError = {
      status: 400,
      messageKey: "analyzer.error.validation",
      messageArgs: { analyzerName: "GeneXpert" },
      fieldErrors: [
        { field: "port", defaultMessage: "Port must be a valid number" },
      ],
    };

    expect(error.fieldErrors?.[0]?.field).toBe("port");
    expectTypeOf<AnalyzerApiError["messageArgs"]>().toEqualTypeOf<
      Record<string, unknown> | undefined
    >();
    expectTypeOf<AnalyzerApiError["fieldErrors"]>().toEqualTypeOf<
      Array<{ field?: string; defaultMessage?: string }> | undefined
    >();

    // @ts-expect-error messageArgs must be an object keyed by argument name.
    const invalidError: AnalyzerApiError = { messageArgs: "GeneXpert" };
    expect(invalidError.messageArgs).toBe("GeneXpert");
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

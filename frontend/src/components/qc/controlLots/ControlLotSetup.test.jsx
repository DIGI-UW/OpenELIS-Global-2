import { describe, expect, test } from "vitest";
import { buildControlLotPayload } from "./ControlLotSetup";

describe("buildControlLotPayload", () => {
  test("testBuildPayload_PreservesAnalyzerAndTestIdsAsStrings", () => {
    const payload = buildControlLotPayload(
      {
        lotNumber: "QC-001",
        controlMaterial: "Acme control",
        controlLevel: "LOW",
        expirationDate: "12/31/2026",
        analyzerId: "AN-STR-1",
        testId: "TEST-STR-9",
        isActive: true,
      },
      {
        calculationMethod: "MANUFACTURER_FIXED",
        initialRunsRequired: 20,
        mean: 12.3,
        standardDeviation: 0.4,
      },
      { isEditMode: false },
    );

    expect(payload.instrumentId).toBe("AN-STR-1");
    expect(payload.testId).toBe("TEST-STR-9");
  });
});

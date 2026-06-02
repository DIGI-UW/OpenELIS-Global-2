import {
  getAllowedCorrectionActions,
  hasInvalidMarkMissingLocationFields,
  isMissingDiscrepancyType,
} from "./qcCorrectionGuardrails";

const CORRECTION_ACTIONS = [
  { id: "UPDATE_LOCATION", label: "Update correct location" },
  { id: "REASSIGN_POSITION", label: "Reassign position" },
  { id: "MARK_MISSING", label: "Mark sample as Missing" },
  { id: "QUARANTINE_SAMPLE", label: "Quarantine sample pending review" },
  { id: "REQUEST_SUPERVISOR_REVIEW", label: "Request supervisor review" },
];

describe("qcCorrectionGuardrails", () => {
  test("isMissingDiscrepancyType only accepts missing discrepancy types", () => {
    expect(isMissingDiscrepancyType("SAMPLE_MISSING")).toBe(true);
    expect(isMissingDiscrepancyType("missing_sample")).toBe(true);
    expect(isMissingDiscrepancyType("MISPLACED_SAMPLE_FOUND")).toBe(false);
  });

  test("getAllowedCorrectionActions excludes MARK_MISSING for non-missing discrepancy", () => {
    const options = getAllowedCorrectionActions(
      CORRECTION_ACTIONS,
      "MISPLACED_SAMPLE_FOUND",
    );

    expect(options.map((action) => action.id)).toEqual([
      "UPDATE_LOCATION",
      "REASSIGN_POSITION",
      "QUARANTINE_SAMPLE",
      "REQUEST_SUPERVISOR_REVIEW",
    ]);
  });

  test("getAllowedCorrectionActions includes quarantine and supervisor review actions", () => {
    const allowed = getAllowedCorrectionActions(
      CORRECTION_ACTIONS,
      "LABELING_ERROR",
    );
    const ids = allowed.map((action) => action.id);
    expect(ids).toContain("QUARANTINE_SAMPLE");
    expect(ids).toContain("REQUEST_SUPERVISOR_REVIEW");
  });

  test("getAllowedCorrectionActions includes MARK_MISSING for missing discrepancy", () => {
    const options = getAllowedCorrectionActions(
      CORRECTION_ACTIONS,
      "SAMPLE_MISSING",
    );

    expect(options.map((action) => action.id)).toEqual(
      expect.arrayContaining([
        "UPDATE_LOCATION",
        "REASSIGN_POSITION",
        "MARK_MISSING",
        "QUARANTINE_SAMPLE",
        "REQUEST_SUPERVISOR_REVIEW",
      ]),
    );
  });

  test("hasInvalidMarkMissingLocationFields guards stale location/position fields", () => {
    expect(
      hasInvalidMarkMissingLocationFields({
        correctionActionType: "MARK_MISSING",
        correctionBoxId: "123",
        correctionPositionCoordinate: "",
      }),
    ).toBe(true);

    expect(
      hasInvalidMarkMissingLocationFields({
        correctionActionType: "MARK_MISSING",
        correctionBoxId: "",
        correctionPositionCoordinate: "A1",
      }),
    ).toBe(true);

    expect(
      hasInvalidMarkMissingLocationFields({
        correctionActionType: "MARK_MISSING",
        correctionBoxId: "",
        correctionPositionCoordinate: "",
      }),
    ).toBe(false);
  });
});

import {
  createEmptyRequestRow,
  validateRequestReferenceRow,
  validateRequestReferenceRows,
  buildReferenceItemsPayload,
  formatRequestedReferenceSummary,
} from "./biorepoRequestReferenceHelpers";

describe("biorepoRequestReferenceHelpers", () => {
  test("createEmptyRequestRow returns editable defaults", () => {
    const row = createEmptyRequestRow();
    expect(row.requestedSampleType).toBe("");
    expect(row.quantityRequested).toBe("");
    expect(row.id).toBeTruthy();
  });

  test("validateRequestReferenceRow requires sample type and quantity", () => {
    expect(validateRequestReferenceRow(createEmptyRequestRow(), 0)).toEqual([
      "Row 1: sample type is required",
      "Row 1: quantity requested must be greater than zero",
    ]);
  });

  test("validateRequestReferenceRows accepts type and quantity without accession", () => {
    const rows = [
      {
        ...createEmptyRequestRow(),
        requestedSampleType: "Plasma",
        quantityRequested: 2,
      },
    ];
    expect(validateRequestReferenceRows(rows)).toEqual([]);
  });

  test("buildReferenceItemsPayload omits bioSampleId", () => {
    const payload = buildReferenceItemsPayload([
      {
        ...createEmptyRequestRow(),
        requestedSampleType: "Serum",
        requestedOriginLab: "CTD",
        quantityRequested: 3,
        unitOfMeasure: "mL",
        remark: "urgent",
      },
    ]);

    expect(payload[0]).toEqual({
      requestedAccessionNumber: null,
      requestedBarcode: null,
      requestedSampleType: "Serum",
      requestedOriginLab: "CTD",
      requestedProjectId: null,
      quantityRequested: 3,
      unitOfMeasure: "mL",
      remark: "urgent",
    });
    expect(payload[0].bioSampleId).toBeUndefined();
  });

  test("formatRequestedReferenceSummary joins known fields", () => {
    expect(
      formatRequestedReferenceSummary({
        requestedSampleType: "Plasma",
        requestedAccessionNumber: "ACC-1",
        requestedOriginLab: "CTD",
      }),
    ).toContain("Plasma");
  });
});

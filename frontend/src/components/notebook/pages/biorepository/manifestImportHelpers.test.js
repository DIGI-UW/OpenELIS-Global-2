import {
  convertLegacyWorksheetRows,
  isSupportedDateValue,
  mergeMappedRowValues,
  normalizeDateValue,
} from "./manifestImportHelpers";

describe("manifestImportHelpers", () => {
  test("normalizeDateValue converts European dot dates", () => {
    expect(normalizeDateValue("11.02.2026")).toBe("2026-02-11");
    expect(normalizeDateValue("11.02.2026 14:30")).toBe("2026-02-11 14:30:00");
  });

  test("isSupportedDateValue accepts normalized European dates", () => {
    expect(isSupportedDateValue("2026-02-11", false)).toBe(true);
    expect(isSupportedDateValue("11.02.2026", false)).toBe(true);
  });

  test("mergeMappedRowValues keeps Request_Date as receiptDate when Transfer_Date present", () => {
    const headers = [
      "Request_Date",
      "Transfering_Unit",
      "Project_Name",
      "sample type",
      "Sample_ID",
      "Transfer_Date",
    ];
    const values = [
      "02/09/2026",
      "Bacteriology",
      "HIEPV",
      "DNA",
      "H-0001",
      "11.02.2026",
    ];

    const row = mergeMappedRowValues(headers, values);
    expect(row.receiptDate).toBe("2026-02-09");
    expect(row.collectionDate).toBe("2026-02-11");
    expect(row.barcode).toBe("H-0001");
  });

  test("convertLegacyWorksheetRows produces zero invalid receipt dates for AHRI template row", () => {
    const rows = [
      [
        "Request_Date",
        "Transfering_Unit",
        "Project_Name",
        "sample type",
        "Sample_ID",
        "Transfer_Date",
      ],
      [
        "02/09/2026",
        "Bacteriology",
        "HIEPV",
        "DNA",
        "H-0001",
        "11.02.2026",
      ],
    ];

    const converted = convertLegacyWorksheetRows(rows, "HIEPVBacteriology");
    expect(converted).toHaveLength(1);
    expect(converted[0][5]).toBe("2026-02-09");
    expect(converted[0][9]).toBe("2026-02-11");
    expect(isSupportedDateValue(converted[0][5], true)).toBe(true);
  });
});

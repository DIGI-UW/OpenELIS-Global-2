import {
  buildLifecycleSummaryRows,
  formatStructuredTransferNotes,
  mergeTransferSummary,
  parseStructuredTransferNotes,
  shouldShowLocationTransition,
  shouldShowWorkflowTransition,
} from "./biorepositoryLifecycleHelpers";

describe("biorepositoryLifecycleHelpers", () => {
  test("parseStructuredTransferNotes reads project and reason", () => {
    const parsed = parseStructuredTransferNotes(
      "Project: COVID Study\nReason: Long-term storage",
    );
    expect(parsed.projectName).toBe("COVID Study");
    expect(parsed.transferReason).toBe("Long-term storage");
  });

  test("parseStructuredTransferNotes falls back to full notes", () => {
    const parsed = parseStructuredTransferNotes("Legacy transfer note");
    expect(parsed.projectName).toBeNull();
    expect(parsed.transferReason).toBe("Legacy transfer note");
  });

  test("mergeTransferSummary prefers API values with context fallback", () => {
    const merged = mergeTransferSummary(
      { sampleType: "Serum", quantity: 5 },
      {
        sourceLab: "MEDICAL_LAB",
        requestNotes: "Project: TB Study\nReason: Archive completed samples",
        externalId: "S-001",
      },
    );

    expect(merged.sourceLab).toBe("MEDICAL_LAB");
    expect(merged.sampleType).toBe("Serum");
    expect(merged.projectName).toBe("TB Study");
    expect(merged.transferReason).toBe("Archive completed samples");
    expect(merged.sampleExternalId).toBe("S-001");
  });

  test("buildLifecycleSummaryRows includes key operational fields", () => {
    const rows = buildLifecycleSummaryRows({
      sourceLab: "CTD",
      sampleExternalId: "S-100",
      sampleType: "Plasma",
      quantity: 2,
      unitOfMeasure: "mL",
      sampleCondition: "Good",
      preservationMedium: "RNAlater",
      sourceNotebookId: 29,
      sourceNotebookEntryId: 29,
      sourceStorageLocation: "CTD Freezer > Shelf 1 > A1",
      transferReason: "Archive",
      projectName: "Study A",
    });

    expect(
      rows.some(
        (row) => row.label === "Source department" && row.value === "CTD",
      ),
    ).toBe(true);
    expect(
      rows.some((row) => row.label === "Sample ID" && row.value === "S-100"),
    ).toBe(true);
    expect(
      rows.some((row) => row.label === "Volume" && row.value === "2 mL"),
    ).toBe(true);
    expect(
      rows.some(
        (row) => row.label === "Sample condition" && row.value === "Good",
      ),
    ).toBe(true);
    expect(
      rows.some((row) => row.label === "Source notebook" && row.value === 29),
    ).toBe(true);
    expect(
      rows.some(
        (row) =>
          row.label === "Source storage" &&
          row.value === "CTD Freezer > Shelf 1 > A1",
      ),
    ).toBe(true);
    expect(
      rows.some(
        (row) => row.label === "Transfer reason" && row.value === "Archive",
      ),
    ).toBe(true);
  });

  test("transition visibility helpers hide empty rows", () => {
    expect(shouldShowWorkflowTransition({})).toBe(false);
    expect(shouldShowWorkflowTransition({ fromWorkflowStatus: "STORED" })).toBe(
      true,
    );
    expect(shouldShowLocationTransition({})).toBe(false);
    expect(
      shouldShowLocationTransition({ toLocationDisplay: "Biorepository" }),
    ).toBe(true);
  });

  test("formatStructuredTransferNotes matches backend format", () => {
    expect(formatStructuredTransferNotes("Study A", "Archive")).toBe(
      "Project: Study A\nReason: Archive",
    );
  });
});

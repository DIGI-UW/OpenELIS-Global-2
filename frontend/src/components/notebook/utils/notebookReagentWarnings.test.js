import {
  buildReagentOptionLabel,
  normalizeInventoryReagentOption,
} from "./notebookReagentWarnings";

describe("notebookReagentWarnings", () => {
  const intl = {
    formatMessage: ({ defaultMessage }) => defaultMessage,
  };

  test("normalizeInventoryReagentOption maps selection warnings from API", () => {
    const normalized = normalizeInventoryReagentOption({
      id: "9",
      name: "Buffer",
      selectionWarnings: ["QC_FAILED", "ZERO_QUANTITY"],
    });

    expect(normalized.selectionWarnings).toEqual(["QC_FAILED", "ZERO_QUANTITY"]);
  });

  test("buildReagentOptionLabel appends warning summary for dropdown display", () => {
    const label = buildReagentOptionLabel(
      {
        id: "9",
        name: "Buffer",
        selectionWarnings: ["QC_FAILED"],
      },
      intl,
    );

    expect(label).toContain("Buffer");
    expect(label).toContain("QC failed");
  });
});

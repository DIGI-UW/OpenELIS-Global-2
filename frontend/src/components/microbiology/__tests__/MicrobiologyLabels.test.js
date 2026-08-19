import { describe, expect, it } from "vitest";
import { formatMicrobiologyEnum } from "../MicrobiologyLabels";

describe("formatMicrobiologyEnum", () => {
  it("preserves microbiology acronyms in status labels", () => {
    expect(formatMicrobiologyEnum("QC_FAILED")).toBe("QC Failed");
    expect(formatMicrobiologyEnum("AST_REVIEWED")).toBe("AST Reviewed");
  });
});

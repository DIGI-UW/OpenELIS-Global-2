import { describe, expect, it } from "vitest";
import { createIntl, createIntlCache } from "react-intl";
import { formatMicrobiologyEnum } from "../MicrobiologyLabels";
import messages from "../../../languages/en.json";

const intl = createIntl({ locale: "en", messages }, createIntlCache());

describe("formatMicrobiologyEnum", () => {
  it("preserves microbiology acronyms in status labels", () => {
    expect(formatMicrobiologyEnum("QC_FAILED", intl)).toBe("QC Failed");
    expect(formatMicrobiologyEnum("AST_REVIEWED", intl)).toBe("AST Reviewed");
  });
  it("calls a partial report Partial, never Preliminary or Interim", () => {
    for (const value of [
      "PRELIM_RELEASED",
      "PRELIMINARY_RELEASED",
      "PRELIMINARY_REPORT_RELEASED",
    ]) {
      expect(formatMicrobiologyEnum(value, intl)).toMatch(/^Partial/);
    }
  });
});

import { describe, expect, it, vi } from "vitest";
import {
  MICROBIOLOGY_CASE_READY_MARK,
  markMicrobiologyReady,
} from "../MicrobiologyPerformance";

describe("MicrobiologyPerformance", () => {
  it("replaces a prior ready mark before recording the current render", () => {
    const performanceApi = {
      clearMarks: vi.fn(),
      mark: vi.fn(),
    };

    expect(
      markMicrobiologyReady(MICROBIOLOGY_CASE_READY_MARK, performanceApi),
    ).toBe(true);
    expect(performanceApi.clearMarks).toHaveBeenCalledWith(
      MICROBIOLOGY_CASE_READY_MARK,
    );
    expect(performanceApi.mark).toHaveBeenCalledWith(
      MICROBIOLOGY_CASE_READY_MARK,
    );
  });

  it("is safe when the Performance API is unavailable", () => {
    expect(markMicrobiologyReady(MICROBIOLOGY_CASE_READY_MARK, null)).toBe(
      false,
    );
  });
});

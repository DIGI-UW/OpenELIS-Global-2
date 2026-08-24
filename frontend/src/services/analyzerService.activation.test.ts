import { beforeEach, describe, expect, it, vi } from "vitest";

import { activateAnalyzer } from "./analyzerService";

describe("analyzer activation client", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
  });

  it("preserves analyzer status when activation returns an HTTP blocker", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue({
      ok: false,
      status: 422,
      statusText: "Unprocessable Entity",
      json: async () => ({
        analyzerId: "42",
        status: "SETUP",
        ready: false,
        activated: false,
        blockers: [{ code: "analyzer.activation.blocker.mappings" }],
      }),
    } as Response);

    const result = await new Promise((resolve) =>
      activateAnalyzer("42", resolve),
    );

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining("/rest/analyzer/analyzers/42/activate"),
      expect.objectContaining({ method: "POST" }),
    );
    expect(result).toEqual(
      expect.objectContaining({
        analyzerId: "42",
        status: "SETUP",
        statusCode: 422,
        ready: false,
        activated: false,
        blockers: [{ code: "analyzer.activation.blocker.mappings" }],
      }),
    );
  });
});

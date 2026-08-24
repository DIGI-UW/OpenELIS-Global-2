import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  activateAnalyzer,
  deactivateAnalyzer,
  reactivateAnalyzer,
} from "./analyzerService";

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

  it("posts deactivation through the lifecycle endpoint", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue({
      ok: true,
      status: 200,
      statusText: "OK",
      json: async () => ({
        analyzerId: "42",
        status: "INACTIVE",
        deactivated: true,
        failure: null,
      }),
    } as Response);

    const result = await new Promise((resolve) =>
      deactivateAnalyzer("42", resolve),
    );

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining("/rest/analyzer/analyzers/42/deactivate"),
      expect.objectContaining({ method: "POST" }),
    );
    expect(result).toEqual(
      expect.objectContaining({
        analyzerId: "42",
        status: "INACTIVE",
        deactivated: true,
      }),
    );
  });

  it("preserves analyzer status and exact blockers when reactivation is rejected", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue({
      ok: false,
      status: 422,
      statusText: "Unprocessable Entity",
      json: async () => ({
        analyzerId: "42",
        status: "INACTIVE",
        ready: false,
        activated: false,
        blockers: [{ code: "analyzer.activation.blocker.mappings" }],
      }),
    } as Response);

    const result = await new Promise((resolve) =>
      reactivateAnalyzer("42", resolve),
    );

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining("/rest/analyzer/analyzers/42/reactivate"),
      expect.objectContaining({ method: "POST" }),
    );
    expect(result).toEqual(
      expect.objectContaining({
        analyzerId: "42",
        status: "INACTIVE",
        statusCode: 422,
        ready: false,
        activated: false,
        blockers: [{ code: "analyzer.activation.blocker.mappings" }],
      }),
    );
  });
});

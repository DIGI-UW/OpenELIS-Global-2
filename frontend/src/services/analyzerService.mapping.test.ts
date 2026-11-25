import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  applyAnalyzerMapping,
  confirmAnalyzerMapping,
  getAnalyzerMapping,
  getAnalyzerTypeDefaults,
  saveAnalyzerMapping,
} from "./analyzerService";

const FINGERPRINT = `sha256:${"3".repeat(64)}`;

describe("analyzer mapping client", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
    localStorage.setItem("CSRF", "csrf-token");
  });

  it("puts the exact reviewed mapping reference on the analyzer", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue({
      ok: true,
      json: async () => ({ id: "42", status: "SETUP" }),
    } as Response);
    const selection = {
      mappingId: "12",
      revision: 2,
      mappingFingerprint: FINGERPRINT,
    };

    const result = await new Promise((resolve) =>
      applyAnalyzerMapping("42", selection, resolve),
    );

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining("/rest/analyzer/analyzers/42/mapping/apply"),
      expect.objectContaining({
        method: "PUT",
        body: JSON.stringify(selection),
        headers: expect.objectContaining({ "X-CSRF-Token": "csrf-token" }),
      }),
    );
    expect(result).toEqual({ id: "42", status: "SETUP" });
  });

  it("edits the analyzer's own mapping, not the type's", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue({
      ok: true,
      json: async () => ({ analyzerId: "42" }),
    } as Response);
    const update = {
      baseMappingFingerprint: FINGERPRINT,
      tests: [],
      results: [],
    };

    await new Promise((resolve) => saveAnalyzerMapping("42", update, resolve));
    await new Promise((resolve) =>
      confirmAnalyzerMapping(
        "42",
        {
          baseMappingFingerprint: FINGERPRINT,
          recognitionFingerprint: FINGERPRINT,
          confirmedRows: [],
          excludedRows: [],
        },
        resolve,
      ),
    );

    expect(fetch).toHaveBeenNthCalledWith(
      1,
      expect.stringContaining("/rest/analyzer/analyzers/42/mapping"),
      expect.objectContaining({ method: "PUT", body: JSON.stringify(update) }),
    );
    expect(fetch).toHaveBeenNthCalledWith(
      2,
      expect.stringContaining("/rest/analyzer/analyzers/42/mapping/confirm"),
      expect.objectContaining({ method: "POST" }),
    );
  });

  it("reads a type's defaults and an analyzer's mapping from different endpoints", () => {
    const get = vi.spyOn(globalThis, "fetch").mockResolvedValue({
      ok: true,
      json: async () => ({}),
    } as Response);

    getAnalyzerMapping("42", () => {});
    getAnalyzerTypeDefaults("site.mock", 3, () => {});

    const urls = get.mock.calls.map(([url]) => String(url));
    expect(urls[0]).toContain("/rest/analyzer/analyzers/42/mapping");
    expect(urls[1]).toContain(
      "/rest/analyzer-types/site.mock/mapping?revision=3",
    );
  });
});

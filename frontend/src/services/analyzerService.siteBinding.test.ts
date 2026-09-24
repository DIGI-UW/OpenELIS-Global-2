import { beforeEach, describe, expect, it, vi } from "vitest";

import { selectAnalyzerSiteBinding } from "./analyzerService";

describe("analyzer site-binding client", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
    localStorage.setItem("CSRF", "csrf-token");
  });

  it("puts the exact reviewed binding reference on the analyzer", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue({
      ok: true,
      json: async () => ({ id: "42", status: "SETUP" }),
    } as Response);
    const selection = {
      siteBindingId: "12",
      revision: 2,
      bindingFingerprint: `sha256:${"3".repeat(64)}`,
    };

    const result = await new Promise((resolve) =>
      selectAnalyzerSiteBinding("42", selection, resolve),
    );

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining("/rest/analyzer/analyzers/42/site-binding"),
      expect.objectContaining({
        method: "PUT",
        body: JSON.stringify(selection),
        headers: expect.objectContaining({ "X-CSRF-Token": "csrf-token" }),
      }),
    );
    expect(result).toEqual({ id: "42", status: "SETUP" });
  });
});

import { beforeEach, expect, it, vi } from "vitest";
import { updateAnalyzerTypeDraft } from "./analyzerService";

beforeEach(() => {
  vi.restoreAllMocks();
  localStorage.clear();
  localStorage.setItem("CSRF", "test-csrf");
});

it("sends the complete profile through the authenticated OE2 draft endpoint", async () => {
  const profile = {
    profileMeta: { id: "site.test" },
    configDefaults: { extractionOverrides: { arbitrary: "retained" } },
  };
  vi.spyOn(globalThis, "fetch").mockResolvedValue({
    ok: true,
    json: async () => ({ draftId: "draft/one", profile, validationIssues: [] }),
  } as Response);
  const result = await new Promise((resolve) =>
    updateAnalyzerTypeDraft("draft/one", profile, resolve),
  );
  expect(fetch).toHaveBeenCalledWith(
    expect.stringContaining("/rest/analyzer-types/drafts/draft%2Fone"),
    {
      credentials: "include",
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": "test-csrf",
      },
      body: JSON.stringify({ profile }),
    },
  );
  expect(result).toEqual({
    draftId: "draft/one",
    profile,
    validationIssues: [],
  });
});

it("preserves an unsuccessful HTTP status even if the server sends no JSON error", async () => {
  vi.spyOn(globalThis, "fetch").mockResolvedValue({
    ok: false,
    status: 503,
    statusText: "Unavailable",
    json: async () => {
      throw new Error("not JSON");
    },
  } as unknown as Response);
  const result = await new Promise((resolve) =>
    updateAnalyzerTypeDraft("draft-1", {}, resolve),
  );
  expect(result).toEqual({
    status: 503,
    statusCode: 503,
    statusText: "Unavailable",
  });
});

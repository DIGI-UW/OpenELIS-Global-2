import type { Page } from "@playwright/test";
import {
  getCsrfToken,
  seedMicrobiologyWhonetExport,
  seedMicrobiologyWhonetExportFilters,
} from "../../playwright/helpers/seed-microbiology-data";

describe("microbiology Playwright scenario authentication", () => {
  it("fails clearly when the authenticated storage state has no CSRF token", async () => {
    const page = {
      context: () => ({
        storageState: async () => ({ cookies: [], origins: [] }),
      }),
    } as unknown as Page;

    await expect(getCsrfToken(page)).rejects.toThrow(
      "Authenticated Playwright storage state is missing the CSRF token",
    );
  });
});

describe("WHONET scenario isolation", () => {
  it.each([
    ["export", seedMicrobiologyWhonetExport],
    ["filtered export", seedMicrobiologyWhonetExportFilters],
  ])("does not reuse another %s setup's case key", async (_, seed) => {
    const scenarioKeys: string[] = [];
    const page = {
      context: () => ({
        storageState: async () => ({
          cookies: [],
          origins: [{ localStorage: [{ name: "CSRF", value: "test-csrf" }] }],
        }),
      }),
      request: {
        post: async (
          _url: string,
          options: { data: { scenarioKey: string } },
        ) => {
          scenarioKeys.push(options.data.scenarioKey);
          return { ok: () => true, json: async () => ({}) };
        },
      },
    } as unknown as Page;

    // Stop after provisioning so this test observes fixture identity without
    // simulating the separate result-entry and release workflows.
    await expect(seed(page)).rejects.toThrow(
      "M4 scenario is missing organismId",
    );
    await expect(seed(page)).rejects.toThrow(
      "M4 scenario is missing organismId",
    );

    expect(scenarioKeys).toHaveLength(2);
    expect(scenarioKeys[0]).not.toBe(scenarioKeys[1]);
  });
});

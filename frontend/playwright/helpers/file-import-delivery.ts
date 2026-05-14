import { Page } from "@playwright/test";

/**
 * Harness helper for FILE analyzer fixture delivery.
 *
 * Two modes:
 *   1. Watched-dir drop (default): mock copies fixture into the analyzer's
 *      watched folder. Works when the file carries per-row test codes.
 *   2. Bridge upload (when opts.uploadTestCode is set): mock multipart-
 *      POSTs the fixture to bridge /admin/upload with the admin-declared
 *      test code, matching the real lab-tech workflow.
 *
 * Lives in helpers/ (not a spec) so the analyzer-id lookup via
 * page.request can't trip the demo-ui-only guard.
 */

export type MockFileResult = {
  readonly sampleId: string;
  readonly result: string;
  readonly testCode?: string;
};

export type MockFileResponse = {
  readonly status: string;
  readonly written_path: string | null;
  readonly metadata: {
    readonly analyzerName: string;
    readonly format: string;
    readonly fixture: string;
    readonly results: MockFileResult[];
  };
};

export interface DropFixtureOptions {
  readonly mockTemplate: string;
  /** Bridge-registered analyzer name to look up when using upload path. */
  readonly analyzerName: string;
  /** Legacy watched-dir sub-path (used when uploadTestCode isn't set). */
  readonly importDirSafeName: string;
  /** When set, route through bridge /admin/upload with this testCode. */
  readonly uploadTestCode?: string;
  /** Mock server URL (defaults to env MOCK_SIMULATOR_URL or localhost:8085). */
  readonly mockApiUrl?: string;
}

export async function dropFixtureViaMock(
  page: Page,
  opts: DropFixtureOptions,
): Promise<MockFileResponse> {
  const mockUrl =
    opts.mockApiUrl ||
    process.env.MOCK_SIMULATOR_URL ||
    "http://localhost:8085";
  const body: Record<string, unknown> = {};

  if (opts.uploadTestCode) {
    const baseUrl = (process.env.BASE_URL || "https://localhost").replace(
      /\/$/,
      "",
    );
    const resp = await page.request.get(
      `${baseUrl}/api/OpenELIS-Global/rest/analyzer/analyzers`,
    );
    if (!resp.ok()) {
      throw new Error(
        `Analyzer list fetch failed: ${resp.status()} — cannot resolve id for ${opts.analyzerName}`,
      );
    }
    const json = (await resp.json()) as
      | Array<{ id: string; name: string }>
      | { analyzers?: Array<{ id: string; name: string }> };
    await resp.dispose();
    const list = Array.isArray(json) ? json : (json.analyzers ?? []);
    const match = list.find((a) => a.name === opts.analyzerName);
    if (!match) {
      throw new Error(
        `No analyzer named ${JSON.stringify(opts.analyzerName)} found. ` +
          `Available: ${list
            .map((a) => a.name)
            .slice(0, 10)
            .join(", ")}`,
      );
    }
    body.bridge_upload = {
      analyzer_id: match.id,
      test_code: opts.uploadTestCode,
    };
  } else {
    body.target_dir = `/data/analyzer-imports/${opts.importDirSafeName}/incoming`;
  }

  const response = await fetch(
    `${mockUrl}/simulate/file/${opts.mockTemplate}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    },
  );

  if (!response.ok) {
    const text = await response.text();
    throw new Error(
      `Mock server POST /simulate/file/${opts.mockTemplate} failed: ${response.status} ${text}`,
    );
  }

  const data = (await response.json()) as MockFileResponse;

  if (!data.metadata?.results?.length) {
    throw new Error(
      `Mock server returned no results for ${opts.mockTemplate}: ${JSON.stringify(data)}`,
    );
  }

  return data;
}

/**
 * Harness helper for FILE analyzer fixture delivery.
 *
 * The mock copies the analyzer's real fixture into its Bridge-watched folder.
 *
 * This is foundational transport support. UI-only demo stories cannot import
 * it because it triggers mock delivery directly.
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
  /** Sub-path of the analyzer's Bridge-watched import directory. */
  readonly importDirSafeName: string;
  /** Mock server URL (defaults to env MOCK_SIMULATOR_URL or localhost:8085). */
  readonly mockApiUrl?: string;
}

export async function dropFixtureViaMock(
  opts: DropFixtureOptions,
): Promise<MockFileResponse> {
  const mockUrl =
    opts.mockApiUrl ||
    process.env.MOCK_SIMULATOR_URL ||
    "http://localhost:8085";
  const body: Record<string, unknown> = {
    target_dir: `/data/analyzer-imports/${opts.importDirSafeName}/incoming`,
  };

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

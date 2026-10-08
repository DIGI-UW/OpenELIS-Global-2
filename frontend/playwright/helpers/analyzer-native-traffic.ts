import { expect, type APIRequestContext, type Page } from "@playwright/test";
import { csrfToken } from "./api-session";

const API = "/api/OpenELIS-Global/rest";

const mockUrl =
  process.env.MOCK_SIMULATOR_URL ||
  process.env.MOCK_URL ||
  "http://localhost:8085";

/** A message Cepheid documents for one assay outcome, as the mock replays it. */
export type GeneXpertFixture = { assay: string; outcome: string };

/**
 * Replay a Cepheid-documented message from the provisioned GeneXpert mock to
 * this connection's Bridge listener, for this accession. `instrumentCodes`
 * renames profile codes the way the instrument would send them, and `patient`
 * fills the patient record Cepheid's examples leave empty.
 */
export async function sendGeneXpertFixture(
  page: Page,
  analyzerId: string,
  accession: string,
  fixture: GeneXpertFixture,
  senderId: string,
  instrumentCodes: Record<string, string> = {},
  patient?: { id: string; name: string },
): Promise<string> {
  // The paired Bridge answers only OpenELIS, so its connection is read and
  // probed through OpenELIS.
  const request = page.request;
  const analyzerUrl = `${API}/analyzer/analyzers/${encodeURIComponent(analyzerId)}`;
  const analyzerResponse = await request.get(analyzerUrl);
  expect(
    analyzerResponse.ok(),
    `Analyzer ${analyzerId}: ${analyzerResponse.status()}`,
  ).toBeTruthy();
  const { connection } = (await analyzerResponse.json()) as {
    connection?: {
      fields: Array<{ key: string; currentValue?: string | number }>;
      actualRuntimeState: string;
    };
  };
  expect(connection, "OpenELIS reads the Bridge connection").toBeDefined();
  expect(connection?.actualRuntimeState).toBe("ACTIVE");
  expect(
    connection?.fields.find((field) => field.key === "senderId")?.currentValue,
    "The UI-configured sender must match the instrument message",
  ).toBe(senderId);
  const probeResponse = await request.post(`${analyzerUrl}/test-connection`, {
    headers: { "X-CSRF-Token": await csrfToken(page) },
  });
  expect(
    probeResponse.ok(),
    `Bridge listener probe: ${probeResponse.status()}`,
  ).toBeTruthy();
  const probe = (await probeResponse.json()) as {
    checks: Array<{ key: string; status: string; details?: { port?: number } }>;
  };
  const listener = probe.checks.find((check) => check.key === "listener");
  expect(listener?.status, "Shared listener ready").toBe("PASSED");
  const port = listener?.details?.port;
  expect(port, "Effective shared listener port from Bridge").toEqual(
    expect.any(Number),
  );
  const networksResponse = await request.get(`${mockUrl}/analyzers`);
  expect(networksResponse.ok()).toBeTruthy();
  const networks = (await networksResponse.json()) as {
    analyzers: Array<{ name: string; subnet: string; template: string }>;
  };
  const mockNetworks = networks.analyzers.filter(
    (analyzer) => analyzer.name === "genexpert",
  );
  expect(mockNetworks, "One provisioned GeneXpert mock network").toHaveLength(
    1,
  );
  expect(mockNetworks[0].template).toBe("genexpert_astm");
  expect(mockNetworks[0].subnet).toMatch(/^10\.\d+\.\d+\.0\/24$/);
  // The mock's isolated /24 attaches Bridge at .2 (its network manager contract).
  const bridgeIp = mockNetworks[0].subnet.replace(/\.0\/24$/, ".2");
  const destination = `tcp://${bridgeIp}:${port}`;
  const response = await request.post(
    `${mockUrl}/simulate/fixture/genexpert/${fixture.assay}/${fixture.outcome}`,
    {
      data: {
        destination,
        sample_id: accession,
        sender_id: senderId,
        instrument_codes: instrumentCodes,
        ...(patient ? { patient } : {}),
      },
    },
  );
  expect(
    response.ok(),
    `GeneXpert mock: ${response.status()} ${await response.text()}`,
  ).toBeTruthy();
  const result = (await response.json()) as {
    pushed: number;
    results: Array<{
      sample_id: string;
      pushed: boolean;
      error: string | null;
    }>;
  };
  expect(result.pushed, JSON.stringify(result)).toBe(1);
  expect(result.results).toHaveLength(1);
  expect(result.results[0]).toMatchObject({
    sample_id: accession,
    pushed: true,
    error: null,
  });
  return destination;
}

/**
 * The mock writes an instrument's results workbook into a folder a connection
 * watches, with these sample IDs on its result rows, in the workbook's order.
 */
export async function writeResultsFile(
  request: APIRequestContext,
  template: "hain_fluorocycler" | "quantstudio7",
  targetDirectory: string,
  sampleIds: string[],
): Promise<Array<{ sampleId: string; result: string }>> {
  const response = await request.post(`${mockUrl}/simulate/file/${template}`, {
    data: { target_dir: targetDirectory, sample_ids: sampleIds },
  });
  expect(
    response.ok(),
    `${template} mock: ${response.status()} ${await response.text()}`,
  ).toBeTruthy();
  const result = (await response.json()) as {
    written_path: string;
    metadata: { results: Array<{ sampleId: string; result: string }> };
  };
  expect(result.written_path).toContain(targetDirectory);
  expect(result.metadata.results.length).toBeGreaterThan(0);
  return result.metadata.results;
}

export const writeFluoroCyclerFile = (
  request: APIRequestContext,
  targetDirectory: string,
  sampleIds: string[],
) => writeResultsFile(request, "hain_fluorocycler", targetDirectory, sampleIds);

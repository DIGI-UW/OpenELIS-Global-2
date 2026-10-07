import { expect, type APIRequestContext } from "@playwright/test";

const mockUrl =
  process.env.MOCK_SIMULATOR_URL ||
  process.env.MOCK_URL ||
  "http://localhost:8085";

export async function sendGeneXpertAstm(
  request: APIRequestContext,
  connectionId: string,
  accession: string,
  testCode: string,
  value: string,
  senderId: string,
  additionalResults: Array<{ testCode: string; value: string }> = [],
): Promise<string> {
  const bridgeUrl =
    process.env.ANALYZER_BRIDGE_URL ||
    process.env.BRIDGE_ADMIN_URL ||
    "https://localhost:8442";
  const bridgeUser =
    process.env.ANALYZER_BRIDGE_USERNAME || process.env.TEST_USER || "admin";
  const bridgePassword =
    process.env.ANALYZER_BRIDGE_PASSWORD ||
    process.env.TEST_PASS ||
    "adminADMIN!";
  const connectionResponse = await request.get(
    `${bridgeUrl}/api/connections/${encodeURIComponent(connectionId)}`,
    {
      headers: {
        Authorization: `Basic ${Buffer.from(`${bridgeUser}:${bridgePassword}`).toString("base64")}`,
      },
    },
  );
  expect(
    connectionResponse.ok(),
    `Bridge connection: ${connectionResponse.status()}`,
  ).toBeTruthy();
  const connection = (await connectionResponse.json()) as {
    fields: Array<{ key: string; currentValue?: string | number }>;
    configRevision: number;
    actualRuntimeState: string;
  };
  expect(connection.actualRuntimeState).toBe("ACTIVE");
  expect(
    connection.fields.find((field) => field.key === "senderId")?.currentValue,
    "The UI-configured sender must match the instrument message",
  ).toBe(senderId);
  const probeResponse = await request.post(
    `${bridgeUrl}/api/connections/${encodeURIComponent(connectionId)}/probe`,
    {
      headers: {
        Authorization: `Basic ${Buffer.from(`${bridgeUser}:${bridgePassword}`).toString("base64")}`,
      },
      data: {
        schemaVersion: "1.0",
        requestId: `native-${accession}`,
        connectionId,
        expectedConfigRevision: connection.configRevision,
      },
    },
  );
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
  const response = await request.post(`${mockUrl}/simulate/astm/genexpert`, {
    data: {
      destination,
      sample_id: accession,
      sender_id: senderId,
      results: [
        { test_code: testCode, value },
        ...additionalResults.map((result) => ({
          test_code: result.testCode,
          value: result.value,
        })),
      ],
    },
  });
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

/** A message Cepheid documents for one assay outcome, as the mock replays it. */
export type GeneXpertFixture = { assay: string; outcome: string };

/**
 * Replay a Cepheid-documented message from the provisioned GeneXpert mock to
 * this connection's Bridge listener, for this accession. `instrumentCodes`
 * renames profile codes the way the instrument would send them.
 */
export async function sendGeneXpertFixture(
  request: APIRequestContext,
  connectionId: string,
  accession: string,
  fixture: GeneXpertFixture,
  senderId: string,
  instrumentCodes: Record<string, string> = {},
): Promise<string> {
  const bridgeUrl =
    process.env.ANALYZER_BRIDGE_URL ||
    process.env.BRIDGE_ADMIN_URL ||
    "https://localhost:8442";
  const bridgeUser =
    process.env.ANALYZER_BRIDGE_USERNAME || process.env.TEST_USER || "admin";
  const bridgePassword =
    process.env.ANALYZER_BRIDGE_PASSWORD ||
    process.env.TEST_PASS ||
    "adminADMIN!";
  const connectionResponse = await request.get(
    `${bridgeUrl}/api/connections/${encodeURIComponent(connectionId)}`,
    {
      headers: {
        Authorization: `Basic ${Buffer.from(`${bridgeUser}:${bridgePassword}`).toString("base64")}`,
      },
    },
  );
  expect(
    connectionResponse.ok(),
    `Bridge connection: ${connectionResponse.status()}`,
  ).toBeTruthy();
  const connection = (await connectionResponse.json()) as {
    fields: Array<{ key: string; currentValue?: string | number }>;
    configRevision: number;
    actualRuntimeState: string;
  };
  expect(connection.actualRuntimeState).toBe("ACTIVE");
  expect(
    connection.fields.find((field) => field.key === "senderId")?.currentValue,
    "The UI-configured sender must match the instrument message",
  ).toBe(senderId);
  const probeResponse = await request.post(
    `${bridgeUrl}/api/connections/${encodeURIComponent(connectionId)}/probe`,
    {
      headers: {
        Authorization: `Basic ${Buffer.from(`${bridgeUser}:${bridgePassword}`).toString("base64")}`,
      },
      data: {
        schemaVersion: "1.0",
        requestId: `native-${accession}`,
        connectionId,
        expectedConfigRevision: connection.configRevision,
      },
    },
  );
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

export async function writeFluoroCyclerFile(
  request: APIRequestContext,
  targetDirectory: string,
  sampleIds: string[],
): Promise<Array<{ sampleId: string; result: string }>> {
  const response = await request.post(
    `${mockUrl}/simulate/file/hain_fluorocycler`,
    {
      data: { target_dir: targetDirectory, sample_ids: sampleIds },
    },
  );
  expect(
    response.ok(),
    `FluoroCycler mock: ${response.status()} ${await response.text()}`,
  ).toBeTruthy();
  const result = (await response.json()) as {
    written_path: string;
    metadata: { results: Array<{ sampleId: string; result: string }> };
  };
  expect(result.written_path).toContain(targetDirectory);
  expect(result.metadata.results.length).toBeGreaterThan(0);
  return result.metadata.results;
}

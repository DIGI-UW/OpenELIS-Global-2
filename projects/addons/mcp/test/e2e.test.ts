// End to end, in process: the add-on against a fake OpenELIS, driven by the
// official MCP client over Streamable HTTP — once on the current revision
// (2026-07-28, stateless) and once as a 2025-11-25 client (initialize).
import { readFileSync } from "node:fs";
import { join } from "node:path";
import { Client, StreamableHTTPClientTransport } from "@modelcontextprotocol/client";
import { afterAll, beforeAll, describe, expect, it } from "vitest";
import { ADMIN_TOKEN } from "./fake-openelis.js";
import { type Harness, startHarness } from "./harness.js";

let h: Harness;

beforeAll(async () => {
  h = await startHarness();
});
afterAll(async () => {
  await h.close();
});

async function createToken(body: Record<string, unknown>): Promise<string> {
  const r = await fetch(`${h.base}/api/agents`, {
    method: "POST",
    headers: { Authorization: `Bearer ${ADMIN_TOKEN}`, "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  expect(r.status).toBe(201);
  return ((await r.json()) as { token: string }).token;
}

async function connect(token: string, era: "modern" | "legacy") {
  const client = new Client(
    { name: `e2e-${era}`, version: "1.0" },
    era === "modern" ? { versionNegotiation: { mode: { pin: "2026-07-28" } } } : {},
  );
  const transport = new StreamableHTTPClientTransport(new URL(`${h.base}/addons/mcp/mcp`), {
    authProvider: { token: async () => token },
  });
  await client.connect(transport);
  return client;
}

function auditLines() {
  const day = new Date().toISOString().slice(0, 10);
  return readFileSync(join(h.dataDir, `audit-${day}.jsonl`), "utf8")
    .trim()
    .split("\n")
    .map((l) => JSON.parse(l) as Record<string, unknown>);
}

describe.each(["modern", "legacy"] as const)("MCP client (%s)", (era) => {
  it("lists only the tools the token may use and calls them", async () => {
    const token = await createToken({ name: `Claude Code — ${era}`, identifiers: false });
    const client = await connect(token, era);
    if (era === "modern") expect(client.getNegotiatedProtocolVersion()).toBe("2026-07-28");

    const { tools } = await client.listTools();
    const names = tools.map((t) => t.name).sort();
    expect(names).toEqual([
      "get_order",
      "get_worklist",
      "lab_statistics",
      "list_orders",
      "list_tests",
    ]);
    const getOrder = tools.find((t) => t.name === "get_order");
    expect(getOrder?.annotations).toMatchObject({ readOnlyHint: true, openWorldHint: false });
    expect(getOrder?.outputSchema).toBeTruthy();

    const r = await client.callTool({
      name: "get_order",
      arguments: { labNumber: "0221026-0004" },
    });
    expect(r.isError).toBeFalsy();
    const sc = r.structuredContent as Record<string, unknown>;
    expect(sc.accessionNumber).toBe("0221026-0004");
    expect((sc.results as unknown[]).length).toBe(2);
    expect(sc.patient).toBeUndefined(); // stripped: the token has no identifiers
    expect(JSON.stringify(r.content)).not.toContain("Lan");

    const list = await client.callTool({
      name: "list_orders",
      arguments: { from: "2026-10-01", to: "2026-10-22" },
    });
    expect(list.isError).toBeFalsy();
    expect(JSON.stringify(list)).not.toContain("079180000001");

    const tat = await client.callTool({
      name: "lab_statistics",
      arguments: { report: "tat", from: "2026-10-01", to: "2026-10-22", groupBy: "section" },
    });
    expect(tat.isError).toBeFalsy();

    // hidden tool: refused as a tool error
    const denied = await client.callTool({ name: "find_patients", arguments: { query: "Lan" } });
    expect(denied.isError).toBe(true);
    expect(JSON.stringify(denied.content)).toContain("forbidden");

    // invalid input: refused before OpenELIS
    const before = h.fake.requests.length;
    const bad = await client.callTool({
      name: "list_orders",
      arguments: { from: "2026-08-01", to: "2026-10-22" },
    });
    expect(bad.isError).toBe(true);
    expect(h.fake.requests.length).toBe(before);

    const missing = await client.callTool({ name: "get_order", arguments: { labNumber: "nope" } });
    expect(missing.isError).toBe(true);
    expect(JSON.stringify(missing.content)).toContain("not_found");

    await client.close();

    const mine = auditLines().filter((l) => l.agentName === `Claude Code — ${era}`);
    expect(mine.map((l) => [l.tool, l.outcome])).toEqual([
      ["get_order", "ok"],
      ["list_orders", "ok"],
      ["lab_statistics", "ok"],
      ["find_patients", "denied"],
      ["list_orders", "invalid"],
      ["get_order", "not_found"],
    ]);
    expect(mine[0]?.client).toBe(`e2e-${era}/1.0`);
    expect(mine[0]?.proto).toBe(era === "modern" ? "2026-07-28" : "2025-11-25");
    expect(mine[0]?.core).toEqual([
      { endpoint: "/orders/{accession}", status: 200 },
      { endpoint: "/orders/{accession}/results", status: 200 },
    ]);

    // who and why reach OpenELIS; the agent's token never does
    const coreCall = h.fake.requests.find((r) => r.path === "/orders/0221026-0004");
    expect(coreCall?.headers["x-addon-actor"]).toMatch(/^agent:[0-9a-f]{8} Claude Code/);
    expect(coreCall?.headers["x-addon-purpose"]).toBe("get_order");
    // this token has no identifiers: OpenELIS is asked for the tool's scopes only
    expect(coreCall?.headers["x-addon-scopes"]).toBe("orders.search,orders.read,results.read");
    expect(h.fake.requests.every((r) => !String(r.headers.authorization).includes("oemcp_"))).toBe(
      true,
    );
  });

  it("gives names and identifiers to a token that has them", async () => {
    const token = await createToken({ name: `Dr Lan ${era}`, identifiers: true });
    const client = await connect(token, era);
    const names = (await client.listTools()).tools.map((t) => t.name);
    expect(names).toContain("find_patients");
    expect(names).toContain("get_patient_results");
    const found = await client.callTool({ name: "find_patients", arguments: { query: "Lan" } });
    expect((found.structuredContent as { patients: unknown[] }).patients.length).toBe(1);
    const res = await client.callTool({
      name: "get_patient_results",
      arguments: { patientId: "1001" },
    });
    expect(res.isError).toBeFalsy();
    const sc = res.structuredContent as {
      patient: { fullName: string };
      orders: { results: unknown[] }[];
    };
    expect(sc.patient.fullName).toBe("Nguyễn Thị Lan");
    expect(sc.orders[0]?.results.length).toBe(2);
    const order = await client.callTool({
      name: "get_order",
      arguments: { labNumber: "0221026-0004" },
    });
    expect((order.structuredContent as { patient?: unknown }).patient).toBeTruthy();
    await client.close();
  });

  it("refuses a revoked token with 401", async () => {
    const token = await createToken({ name: `revoked ${era}`, identifiers: false });
    const id = token.split("_")[1];
    const r = await fetch(`${h.base}/api/agents/${id}/revoke`, {
      method: "POST",
      headers: { Authorization: `Bearer ${ADMIN_TOKEN}` },
    });
    expect(r.status).toBe(200);
    await expect(connect(token, era)).rejects.toThrow();
  });
});

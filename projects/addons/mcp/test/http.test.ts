import { readFileSync } from "node:fs";
import { join } from "node:path";
import { Client, StreamableHTTPClientTransport } from "@modelcontextprotocol/client";
import { afterEach, describe, expect, it } from "vitest";
import { ADMIN_TOKEN, VALIDATION_TOKEN } from "./fake-openelis.js";
import { ALL_SCOPES, type Harness, PUBLIC_ORIGIN, startHarness } from "./harness.js";

let h: Harness | null = null;
afterEach(async () => {
  await h?.close();
  h = null;
});

const PRM = `${PUBLIC_ORIGIN}/.well-known/oauth-protected-resource/addons/mcp/mcp`;
const LIST = { jsonrpc: "2.0", id: 1, method: "tools/list", params: {} };

async function token(
  hh: Harness,
  body: Record<string, unknown> = { name: "t", identifiers: false },
) {
  const r = await fetch(`${hh.base}/api/agents`, {
    method: "POST",
    headers: { Authorization: `Bearer ${ADMIN_TOKEN}`, "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  return ((await r.json()) as { token: string }).token;
}

function post(hh: Harness, headers: Record<string, string>, body: unknown = LIST) {
  return fetch(`${hh.base}/mcp`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json, text/event-stream",
      ...headers,
    },
    body: JSON.stringify(body),
  });
}

async function client(hh: Harness, tok: string) {
  const c = new Client(
    { name: "t", version: "1" },
    { versionNegotiation: { mode: { pin: "2026-07-28" } } },
  );
  await c.connect(
    new StreamableHTTPClientTransport(new URL(`${hh.base}/mcp`), {
      authProvider: { token: async () => tok },
    }),
  );
  return c;
}

const audit = (hh: Harness) =>
  readFileSync(join(hh.dataDir, `audit-${new Date().toISOString().slice(0, 10)}.jsonl`), "utf8")
    .trim()
    .split("\n")
    .map((l) => JSON.parse(l) as Record<string, unknown>);

describe("MCP endpoint", () => {
  it("401 with a resource_metadata challenge, and an unauthorized audit line", async () => {
    h = await startHarness();
    const none = await post(h, { "Mcp-Name": "get_order" });
    expect(none.status).toBe(401);
    expect(none.headers.get("www-authenticate")).toBe(`Bearer resource_metadata="${PRM}"`);
    const bad = await post(h, {
      Authorization: "Bearer oemcp_00000000_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
    });
    expect(bad.status).toBe(401);
    expect(bad.headers.get("www-authenticate")).toContain(`resource_metadata="${PRM}"`);
    expect(bad.headers.get("www-authenticate")).toContain('error="invalid_token"');
    const lines = audit(h);
    expect(lines[0]).toMatchObject({
      agent: null,
      agentName: null,
      tool: "get_order",
      outcome: "unauthorized",
    });
    expect(Object.keys(lines[0]!)).toEqual([
      "at",
      "agent",
      "agentName",
      "client",
      "proto",
      "ip",
      "tool",
      "args",
      "outcome",
      "rows",
      "core",
      "ms",
    ]);
  });

  it("refuses a foreign Origin with 403, accepts the lab's own and none", async () => {
    h = await startHarness();
    const tok = await token(h);
    const auth = { Authorization: `Bearer ${tok}` };
    expect((await post(h, { ...auth, Origin: "https://evil.example" })).status).toBe(403);
    expect((await post(h, { ...auth, Origin: "https://lims.example.org:8443" })).status).toBe(403);
    // without auth too: Origin is checked first
    expect((await post(h, { Origin: "https://evil.example" })).status).toBe(403);
    expect((await post(h, { ...auth, Origin: PUBLIC_ORIGIN })).status).not.toBe(403);
    expect((await post(h, auth)).status).not.toBe(403);
  });

  it("uses allowedOrigins from the configuration when set", async () => {
    h = await startHarness(ALL_SCOPES, {
      allowedOrigins: "https://a.example, https://b.example:8443",
    });
    const auth = { Authorization: `Bearer ${await token(h)}` };
    expect((await post(h, { ...auth, Origin: "https://b.example:8443" })).status).not.toBe(403);
    expect((await post(h, { ...auth, Origin: PUBLIC_ORIGIN })).status).toBe(403);
  });

  it("GET and DELETE are 405 (stateless)", async () => {
    h = await startHarness();
    expect((await fetch(`${h.base}/mcp`)).status).toBe(405);
    expect((await fetch(`${h.base}/addons/mcp/mcp`, { method: "DELETE" })).status).toBe(405);
  });

  it("serves the protected-resource metadata and health", async () => {
    h = await startHarness();
    for (const p of [
      "/.well-known/oauth-protected-resource/addons/mcp/mcp",
      "/.well-known/oauth-protected-resource",
    ]) {
      const r = await fetch(h.base + p);
      expect(await r.json()).toEqual({
        resource: `${PUBLIC_ORIGIN}/addons/mcp/mcp`,
        bearer_methods_supported: ["header"],
        resource_name: "OpenELIS MCP server",
      });
    }
    expect(await (await fetch(`${h.base}/health`)).json()).toMatchObject({
      status: "ok",
      openelis: "ok",
    });
  });

  it("lists only tools whose scopes OpenELIS granted, and honours the token allowlist", async () => {
    h = await startHarness(["stats.read", "catalog.read"]);
    const c = await client(h, await token(h, { name: "x", identifiers: true }));
    expect((await c.listTools()).tools.map((t) => t.name).sort()).toEqual([
      "lab_statistics",
      "list_tests",
    ]);
    await c.close();
    const c2 = await client(
      h,
      await token(h, { name: "y", identifiers: false, tools: ["list_tests"] }),
    );
    expect((await c2.listTools()).tools.map((t) => t.name)).toEqual(["list_tests"]);
    const r = await c2.callTool({
      name: "lab_statistics",
      arguments: { report: "orders", from: "2026-10-01", to: "2026-10-02", groupBy: "day" },
    });
    expect(r.isError).toBe(true);
    expect(JSON.stringify(r.content)).toContain("forbidden");
    await c2.close();
  });

  it("explains a scope OpenELIS withdrew", async () => {
    h = await startHarness();
    const c = await client(h, await token(h));
    await c.listTools(); // scopes cached (60 s)
    h.fake.scopes = h.fake.scopes.filter((s) => s !== "results.read");
    const r = await c.callTool({ name: "get_order", arguments: { labNumber: "0221026-0004" } });
    expect(r.isError).toBe(true);
    expect(JSON.stringify(r.content)).toContain(
      "OpenELIS has not granted scope results.read to this add-on",
    );
    expect(audit(h).at(-1)).toMatchObject({ tool: "get_order", outcome: "denied" });
    await c.close();
  });

  it("rate limits per token, per minute", async () => {
    h = await startHarness(ALL_SCOPES, { ratePerMinute: 2 });
    const c = await client(h, await token(h));
    const call = () => c.callTool({ name: "list_tests", arguments: {} });
    expect((await call()).isError).toBeFalsy();
    expect((await call()).isError).toBeFalsy();
    const r = await call();
    expect(r.isError).toBe(true);
    expect(JSON.stringify(r.content)).toMatch(/rate_limited.*retry after \d+ seconds/);
    expect(audit(h).at(-1)).toMatchObject({ outcome: "rate_limited", core: [] });
    // another token has its own allowance
    const c2 = await client(h, await token(h));
    expect((await c2.callTool({ name: "list_tests", arguments: {} })).isError).toBeFalsy();
    await c.close();
    await c2.close();
  });
});

describe("admin API", () => {
  const api = (hh: Harness, path: string, tok?: string, init: RequestInit = {}) =>
    fetch(`${hh.base}/api${path}`, {
      ...init,
      headers: {
        ...(tok ? { Authorization: `Bearer ${tok}` } : {}),
        "Content-Type": "application/json",
        ...(init.headers ?? {}),
      },
    });

  it("requires an OpenELIS UI token issued for the settings page slot", async () => {
    h = await startHarness();
    expect((await api(h, "/agents")).status).toBe(401);
    expect((await api(h, "/agents", "oeat.unknown.sig")).status).toBe(401);
    expect((await api(h, "/agents", VALIDATION_TOKEN)).status).toBe(403);
    expect((await api(h, "/agents", "oemcp_00000000_x")).status).toBe(401);
    const ok = await api(h, "/agents", ADMIN_TOKEN);
    expect(ok.status).toBe(200);
    expect(ok.headers.get("access-control-allow-origin")).toBe("*");
    // the UI token was checked with OpenELIS, not the service key
    const who = h.fake.requests.filter(
      (r) => r.path === "/whoami" && r.headers.authorization === `Bearer ${ADMIN_TOKEN}`,
    );
    expect(who.length).toBe(1);
    await api(h, "/agents", ADMIN_TOKEN);
    expect(
      h.fake.requests.filter(
        (r) => r.path === "/whoami" && r.headers.authorization === `Bearer ${ADMIN_TOKEN}`,
      ).length,
    ).toBe(1); // cached
  });

  it("answers CORS preflight", async () => {
    h = await startHarness();
    const r = await fetch(`${h.base}/api/agents`, {
      method: "OPTIONS",
      headers: { Origin: "null" },
    });
    expect(r.status).toBe(204);
    expect(r.headers.get("access-control-allow-headers")).toMatch(/Authorization/);
    expect(r.headers.get("access-control-allow-credentials")).toBeNull();
  });

  it("creates (token shown once), lists without secrets, revokes, and shows the audit and status", async () => {
    h = await startHarness();
    const created = await api(h, "/agents", ADMIN_TOKEN, {
      method: "POST",
      body: JSON.stringify({
        name: "Claude Desktop — lab",
        identifiers: false,
        expiresInDays: 30,
        tools: ["get_order"],
      }),
    });
    expect(created.status).toBe(201);
    const { agent, token: tok } = (await created.json()) as {
      agent: Record<string, unknown>;
      token: string;
    };
    expect(agent).toMatchObject({
      createdBy: "42",
      identifiers: false,
      tools: ["get_order"],
      status: "active",
    });
    expect(agent.expiresAt).toBeTruthy();
    const listed = await (await api(h, "/agents", ADMIN_TOKEN)).text();
    expect(listed).not.toContain(tok);
    expect(listed).not.toContain("hash");

    expect(
      (await api(h, "/agents", ADMIN_TOKEN, { method: "POST", body: JSON.stringify({ name: "" }) }))
        .status,
    ).toBe(400);
    expect(
      (
        await api(h, "/agents", ADMIN_TOKEN, {
          method: "POST",
          body: JSON.stringify({ name: "x", tools: ["rm_rf"] }),
        })
      ).status,
    ).toBe(400);
    expect(
      (
        await api(h, "/agents", ADMIN_TOKEN, {
          method: "POST",
          body: JSON.stringify({ name: "x", identifiers: "yes" }),
        })
      ).status,
    ).toBe(400);

    const c = await client(h, tok);
    await c.callTool({ name: "get_order", arguments: { labNumber: "0221026-0004" } });
    await c.close();
    const lines = (await (
      await api(h, `/audit?agent=${String(agent.id)}`, ADMIN_TOKEN)
    ).json()) as { lines: Record<string, unknown>[] };
    expect(lines.lines.map((l) => l.outcome)).toEqual(["ok"]);

    const status = (await (await api(h, "/status", ADMIN_TOKEN)).json()) as Record<string, unknown>;
    expect(status).toMatchObject({ endpoint: `${PUBLIC_ORIGIN}/addons/mcp/mcp`, openelis: "ok" });
    expect(status.grantedScopes).toEqual([...ALL_SCOPES].sort());

    expect(
      (await api(h, `/agents/${String(agent.id)}/revoke`, ADMIN_TOKEN, { method: "POST" })).status,
    ).toBe(200);
    expect((await post(h, { Authorization: `Bearer ${tok}` })).status).toBe(401);
    expect((await api(h, "/agents/00000000/revoke", ADMIN_TOKEN, { method: "POST" })).status).toBe(
      404,
    );
  });
});

describe("behind the generic add-on API route", () => {
  it("serves MCP at /api/mcp and the metadata under /api/", async () => {
    const h = await startHarness();
    try {
      const meta = await fetch(`${h.base}/api/.well-known/oauth-protected-resource`);
      expect(meta.status).toBe(200);
      expect(
        ((await meta.json()) as { bearer_methods_supported: string[] }).bearer_methods_supported,
      ).toEqual(["header"]);
      const r = await fetch(`${h.base}/api/mcp`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json, text/event-stream",
        },
        body: JSON.stringify({ jsonrpc: "2.0", id: 1, method: "tools/list" }),
      });
      expect(r.status).toBe(401);
      expect(r.headers.get("www-authenticate")).toContain("resource_metadata=");
    } finally {
      await h.close();
    }
  });
});

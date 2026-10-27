import { mkdtempSync, readdirSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { AuditLog } from "../src/audit.js";

const line = (at: string, agent: string, tool = "get_order") => ({
  at,
  agent,
  agentName: "Claude Code — Dr Lan",
  client: "claude-code/2.1",
  proto: "2026-07-28",
  ip: "100.64.0.9",
  tool,
  args: { labNumber: "0221026-0004" },
  outcome: "ok" as const,
  rows: 12,
  core: [{ endpoint: "/orders/{accession}/results", status: 200 }],
  ms: 84,
});

describe("audit log", () => {
  it("writes one line per call in a file per day, newest first on read, filtered by agent", () => {
    const dir = mkdtempSync(join(tmpdir(), "mcp-audit-"));
    const a = new AuditLog(dir, () => new Date("2026-10-22T12:00:00Z"));
    a.write(line("2026-10-21T08:00:00.000Z", "a7f30000"));
    a.write(line("2026-10-22T08:15:02.114Z", "a7f30000", "list_orders"));
    a.write(line("2026-10-22T09:00:00.000Z", "b0000000"));
    expect(readdirSync(dir).sort()).toEqual(["audit-2026-10-21.jsonl", "audit-2026-10-22.jsonl"]);
    const all = a.read({});
    expect(all.map((l) => l.at)).toEqual([
      "2026-10-22T09:00:00.000Z",
      "2026-10-22T08:15:02.114Z",
      "2026-10-21T08:00:00.000Z",
    ]);
    expect(a.read({ agent: "a7f30000" }).map((l) => l.tool)).toEqual(["list_orders", "get_order"]);
    expect(a.read({ from: "2026-10-22", to: "2026-10-22" }).length).toBe(2);
    expect(a.read({ limit: 1 }).length).toBe(1);
    expect(Object.keys(all[0]!)).toEqual([
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
    rmSync(dir, { recursive: true, force: true });
  });

  it("purges files older than the retention", () => {
    const dir = mkdtempSync(join(tmpdir(), "mcp-audit-"));
    for (const d of ["2026-01-01", "2026-04-24", "2026-04-25", "2026-10-22"])
      writeFileSync(join(dir, `audit-${d}.jsonl`), "");
    writeFileSync(join(dir, "agents.json"), "{}");
    const a = new AuditLog(dir, () => new Date("2026-10-22T12:00:00Z"));
    expect(a.purge(180)).toEqual(expect.arrayContaining(["2026-01-01", "2026-04-24"]));
    expect(readdirSync(dir).sort()).toEqual([
      "agents.json",
      "audit-2026-04-25.jsonl",
      "audit-2026-10-22.jsonl",
    ]);
    rmSync(dir, { recursive: true, force: true });
  });
});

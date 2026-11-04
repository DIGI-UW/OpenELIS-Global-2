import { mkdtempSync, readFileSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { afterEach, describe, expect, it } from "vitest";
import { AgentStore, sha256 } from "../src/agents.js";

const dirs: string[] = [];
const tmp = () => {
  const d = mkdtempSync(join(tmpdir(), "mcp-agents-"));
  dirs.push(d);
  return d;
};
afterEach(() => dirs.splice(0).forEach((d) => rmSync(d, { recursive: true, force: true })));

describe("agent tokens", () => {
  it("creates oemcp_<id>_<secret> and stores only its SHA-256", () => {
    const dir = tmp();
    const s = new AgentStore(dir);
    const { agent, token } = s.create({
      name: "Claude Code — Dr Lan",
      identifiers: false,
      createdBy: "42",
    });
    expect(token).toMatch(/^oemcp_[0-9a-f]{8}_[A-Za-z0-9_-]{43}$/);
    expect(token.split("_")[1]).toBe(agent.id);
    const file = readFileSync(join(dir, "agents.json"), "utf8");
    expect(file).not.toContain(token);
    expect(file).not.toContain(token.slice(15));
    expect(file).toContain(sha256(token));
    expect(JSON.stringify(s.list())).not.toContain(sha256(token));
    expect(agent).toMatchObject({
      name: "Claude Code — Dr Lan",
      identifiers: false,
      createdBy: "42",
      status: "active",
    });
  });

  it("verifies the exact token only", () => {
    const s = new AgentStore(tmp());
    const { agent, token } = s.create({
      name: "a",
      identifiers: true,
      tools: ["get_order"],
      createdBy: "1",
    });
    expect(s.verify(token)?.id).toBe(agent.id);
    expect(s.verify(token)?.tools).toEqual(["get_order"]);
    const last = token.slice(-1) === "A" ? "B" : "A";
    expect(s.verify(token.slice(0, -1) + last)).toBeNull();
    expect(s.verify(`oemcp_ffffffff_${token.slice(15)}`)).toBeNull();
    expect(s.verify("garbage")).toBeNull();
    expect(s.verify(undefined)).toBeNull();
  });

  it("survives a restart, and records lastUsedAt", () => {
    const dir = tmp();
    const s = new AgentStore(dir);
    const { token } = s.create({ name: "a", identifiers: false, createdBy: "1" });
    s.verify(token);
    s.flush();
    const again = new AgentStore(dir);
    expect(again.verify(token)?.name).toBe("a");
    expect(again.list()[0]?.lastUsedAt).toBeTruthy();
  });

  it("refuses revoked and expired tokens", () => {
    let now = new Date("2026-10-22T00:00:00Z");
    const s = new AgentStore(tmp(), () => now);
    const a = s.create({ name: "a", identifiers: false, createdBy: "1" });
    const b = s.create({ name: "b", identifiers: false, expiresInDays: 7, createdBy: "1" });
    expect(s.revoke(a.agent.id)?.status).toBe("revoked");
    expect(s.verify(a.token)).toBeNull();
    expect(s.verify(b.token)).not.toBeNull();
    now = new Date("2026-10-29T00:00:01Z");
    expect(s.verify(b.token)).toBeNull();
    expect(s.get(b.agent.id)?.status).toBe("expired");
    expect(s.revoke("00000000")).toBeNull();
  });
});

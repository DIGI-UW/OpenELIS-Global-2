// Per-agent bearer tokens: `oemcp_<id>_<secret>`, id 8 hex, secret 32 random
// bytes (base64url). Only the SHA-256 of the whole token is stored, in
// /data/agents.json (written atomically). A token is shown once, at creation.
import { createHash, randomBytes, timingSafeEqual } from "node:crypto";
import { mkdirSync, readFileSync, renameSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";

export interface Agent {
  id: string;
  name: string;
  createdAt: string;
  /** OpenELIS user id of the administrator who created it. */
  createdBy: string;
  /** May see patient names and identifiers (needs patient.identity on the add-on too). */
  identifiers: boolean;
  /** Optional allowlist of tool names; absent = every tool the add-on may offer. */
  tools?: string[];
  expiresAt?: string;
  revokedAt?: string;
  lastUsedAt?: string;
}

interface StoredAgent extends Agent {
  hash: string;
}

export type PublicAgent = Agent & { status: "active" | "revoked" | "expired" };

const TOKEN = /^oemcp_([0-9a-f]{8})_([A-Za-z0-9_-]{43})$/;
const DUMMY_HASH = sha256("oemcp_00000000_dummy");

export function sha256(s: string): string {
  return createHash("sha256").update(s, "utf8").digest("hex");
}

export interface NewAgent {
  name: string;
  identifiers: boolean;
  tools?: string[];
  expiresInDays?: number;
  createdBy: string;
}

export class AgentStore {
  private readonly file: string;
  private agents: StoredAgent[] = [];
  private dirty = false;
  private flushTimer: NodeJS.Timeout | null = null;

  constructor(
    dataDir: string,
    private readonly now: () => Date = () => new Date(),
  ) {
    this.file = join(dataDir, "agents.json");
    try {
      const parsed = JSON.parse(readFileSync(this.file, "utf8")) as { agents?: StoredAgent[] };
      this.agents = Array.isArray(parsed.agents) ? parsed.agents : [];
    } catch (e) {
      if ((e as NodeJS.ErrnoException).code !== "ENOENT") throw e;
    }
  }

  create(n: NewAgent): { agent: PublicAgent; token: string } {
    let id: string;
    do id = randomBytes(4).toString("hex");
    while (this.agents.some((a) => a.id === id));
    const token = `oemcp_${id}_${randomBytes(32).toString("base64url")}`;
    const now = this.now();
    const a: StoredAgent = {
      id,
      name: n.name,
      createdAt: now.toISOString(),
      createdBy: n.createdBy,
      identifiers: n.identifiers,
      hash: sha256(token),
    };
    if (n.tools) a.tools = [...n.tools];
    if (n.expiresInDays)
      a.expiresAt = new Date(now.getTime() + n.expiresInDays * 86400000).toISOString();
    this.agents.push(a);
    this.save();
    return { agent: this.toPublic(a), token };
  }

  /** The agent of a valid, active token, or null. Constant time in the secret. */
  verify(token: string | undefined): Agent | null {
    const m = token ? TOKEN.exec(token) : null;
    const found = m ? this.agents.find((a) => a.id === m[1]) : undefined;
    const expected = Buffer.from(found ? found.hash : DUMMY_HASH, "hex");
    const actual = Buffer.from(sha256(token ?? ""), "hex");
    const same = timingSafeEqual(expected, actual);
    if (!found || !same || !m) return null;
    if (this.statusOf(found) !== "active") return null;
    found.lastUsedAt = this.now().toISOString();
    this.saveSoon();
    const { hash: _hash, ...agent } = found;
    return agent;
  }

  revoke(id: string): PublicAgent | null {
    const a = this.agents.find((x) => x.id === id);
    if (!a) return null;
    if (!a.revokedAt) {
      a.revokedAt = this.now().toISOString();
      this.save();
    }
    return this.toPublic(a);
  }

  list(): PublicAgent[] {
    return this.agents.map((a) => this.toPublic(a));
  }

  get(id: string): PublicAgent | null {
    const a = this.agents.find((x) => x.id === id);
    return a ? this.toPublic(a) : null;
  }

  private statusOf(a: Agent): PublicAgent["status"] {
    if (a.revokedAt) return "revoked";
    if (a.expiresAt && Date.parse(a.expiresAt) <= this.now().getTime()) return "expired";
    return "active";
  }

  private toPublic(a: StoredAgent): PublicAgent {
    const { hash: _hash, ...rest } = a;
    return { ...rest, status: this.statusOf(a) };
  }

  /** Writes now (create, revoke). */
  save() {
    if (this.flushTimer) {
      clearTimeout(this.flushTimer);
      this.flushTimer = null;
    }
    this.dirty = false;
    mkdirSync(dirname(this.file), { recursive: true });
    const tmp = `${this.file}.${process.pid}.tmp`;
    writeFileSync(tmp, JSON.stringify({ agents: this.agents }, null, 2) + "\n", { mode: 0o600 });
    renameSync(tmp, this.file);
  }

  /** lastUsedAt changes on every call: written at most every 10 s. */
  private saveSoon() {
    this.dirty = true;
    if (this.flushTimer) return;
    this.flushTimer = setTimeout(() => {
      this.flushTimer = null;
      if (this.dirty) this.save();
    }, 10000);
    this.flushTimer.unref();
  }

  flush() {
    if (this.dirty) this.save();
  }
}

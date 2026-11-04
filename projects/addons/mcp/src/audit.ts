// The add-on's own record of every MCP tool call, including the ones that never
// reached OpenELIS (denied, invalid, rate limited) and failed authentications.
// One JSON object per line in /data/audit-YYYY-MM-DD.jsonl (UTC date of `at`).
import { appendFileSync, mkdirSync, readdirSync, readFileSync, unlinkSync } from "node:fs";
import { join } from "node:path";
import type { CoreCall } from "./openelis.js";

export type Outcome =
  "ok" | "denied" | "invalid" | "rate_limited" | "not_found" | "error" | "unauthorized";

export interface AuditLine {
  at: string;
  agent: string | null;
  agentName: string | null;
  client: string | null;
  proto: string | null;
  ip: string | null;
  tool: string | null;
  args: unknown;
  outcome: Outcome;
  rows: number;
  core: CoreCall[];
  ms: number;
}

const FILE = /^audit-(\d{4}-\d{2}-\d{2})\.jsonl$/;
const DATE = /^\d{4}-\d{2}-\d{2}$/;

export class AuditLog {
  constructor(
    private readonly dir: string,
    private readonly now: () => Date = () => new Date(),
  ) {
    mkdirSync(dir, { recursive: true });
  }

  write(line: AuditLine) {
    const ordered: AuditLine = {
      at: line.at,
      agent: line.agent,
      agentName: line.agentName,
      client: line.client,
      proto: line.proto,
      ip: line.ip,
      tool: line.tool,
      args: line.args,
      outcome: line.outcome,
      rows: line.rows,
      core: line.core,
      ms: line.ms,
    };
    try {
      appendFileSync(
        join(this.dir, `audit-${line.at.slice(0, 10)}.jsonl`),
        JSON.stringify(ordered) + "\n",
        {
          mode: 0o600,
        },
      );
    } catch (e) {
      console.error("audit: cannot write", (e as Error).message);
    }
  }

  /** Newest first, between two UTC dates inclusive (default: the last 7 days). */
  read(q: { from?: string; to?: string; agent?: string; limit?: number }): AuditLine[] {
    const today = this.now().toISOString().slice(0, 10);
    const to = q.to && DATE.test(q.to) ? q.to : today;
    const from = q.from && DATE.test(q.from) ? q.from : addDays(to, -6);
    const limit = Math.min(500, Math.max(1, q.limit ?? 200));
    const days = this.files()
      .filter((d) => d >= from && d <= to)
      .sort()
      .reverse();
    const out: AuditLine[] = [];
    for (const d of days) {
      let text: string;
      try {
        text = readFileSync(join(this.dir, `audit-${d}.jsonl`), "utf8");
      } catch {
        continue;
      }
      const lines = text.split("\n");
      for (let i = lines.length - 1; i >= 0 && out.length < limit; i--) {
        const raw = lines[i];
        if (!raw) continue;
        try {
          const l = JSON.parse(raw) as AuditLine;
          if (!q.agent || l.agent === q.agent) out.push(l);
        } catch {
          /* a torn last line after a crash */
        }
      }
      if (out.length >= limit) break;
    }
    return out;
  }

  /** Deletes the files of days older than `retentionDays`. */
  purge(retentionDays: number): string[] {
    const oldest = addDays(this.now().toISOString().slice(0, 10), -retentionDays);
    const removed: string[] = [];
    for (const d of this.files()) {
      if (d < oldest) {
        try {
          unlinkSync(join(this.dir, `audit-${d}.jsonl`));
          removed.push(d);
        } catch {
          /* already gone */
        }
      }
    }
    return removed;
  }

  private files(): string[] {
    try {
      return readdirSync(this.dir)
        .map((f) => FILE.exec(f)?.[1])
        .filter((d): d is string => !!d);
    } catch {
      return [];
    }
  }
}

export function addDays(day: string, n: number): string {
  const t = Date.parse(`${day}T00:00:00Z`) + n * 86400000;
  return new Date(t).toISOString().slice(0, 10);
}

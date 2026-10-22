// The OpenELIS add-on API (`/rest/addon-api/v1`), called from inside the compose
// network with the add-on's service key — or, for the admin page only, with the
// OpenELIS UI token of the administrator's frame (to learn who is calling).
// The agent's own MCP token is never sent here.
import http from "node:http";
import https from "node:https";

export interface CoreCall {
  endpoint: string;
  status: number;
}

export class CoreError extends Error {
  constructor(
    readonly status: number,
    readonly body: Record<string, unknown>,
    message?: string,
  ) {
    super(message ?? `OpenELIS answered ${status}`);
  }
  get code(): string | undefined {
    return typeof this.body.code === "string" ? this.body.code : undefined;
  }
  get scope(): string | undefined {
    return typeof this.body.scope === "string" ? this.body.scope : undefined;
  }
}

export interface CallOptions {
  /** Who the call is for: "agent:<id> <name>" (sanitized here). */
  actor?: string;
  /** Why: the tool name. */
  purpose?: string;
  /**
   * Narrow the service key to these scopes for this call (`X-Addon-Scopes`):
   * OpenELIS then answers as if the add-on held only them.
   */
  scopes?: string[];
  /** Another bearer credential than the service key (the admin's UI token). */
  token?: string;
  /** Every call is appended here (route template + status), for the audit line. */
  calls?: CoreCall[];
}

export type Query = Record<string, string | number | boolean | undefined>;

export interface OpenElisOptions {
  apiUrl: string;
  apiKey: string;
  tlsInsecure?: boolean;
  timeoutMs?: number;
}

const MAX_BODY = 8 * 1024 * 1024;

export class OpenElis {
  private readonly base: URL;
  private readonly agent: http.Agent;

  constructor(private readonly o: OpenElisOptions) {
    this.base = new URL(o.apiUrl.replace(/\/+$/, "") + "/");
    this.agent =
      this.base.protocol === "https:"
        ? new https.Agent({ keepAlive: true, rejectUnauthorized: !o.tlsInsecure })
        : new http.Agent({ keepAlive: true });
  }

  /**
   * GET `path` (relative, already encoded) under the add-on API. `template` is
   * the route as logged (e.g. `/orders/{accession}`), never the subject itself.
   */
  async get<T>(
    path: string,
    template: string,
    query: Query = {},
    opts: CallOptions = {},
  ): Promise<T> {
    const url = new URL(path.replace(/^\/+/, ""), this.base);
    for (const [k, v] of Object.entries(query)) {
      if (v !== undefined && v !== "") url.searchParams.set(k, String(v));
    }
    const headers: Record<string, string> = {
      Accept: "application/json",
      Authorization: `Bearer ${opts.token ?? this.o.apiKey}`,
    };
    if (opts.actor) headers["X-Addon-Actor"] = sanitizeActor(opts.actor);
    if (opts.purpose) headers["X-Addon-Purpose"] = sanitizePurpose(opts.purpose);
    if (opts.scopes) headers["X-Addon-Scopes"] = opts.scopes.join(",");

    let status = 0;
    try {
      const res = await this.request(url, headers);
      status = res.status;
      let body: unknown = {};
      if (res.text) {
        try {
          body = JSON.parse(res.text);
        } catch {
          body = {};
        }
      }
      if (status < 200 || status >= 300) {
        throw new CoreError(
          status,
          body && typeof body === "object" ? (body as Record<string, unknown>) : {},
        );
      }
      return body as T;
    } finally {
      opts.calls?.push({ endpoint: template, status });
    }
  }

  private request(
    url: URL,
    headers: Record<string, string>,
  ): Promise<{ status: number; text: string }> {
    const lib = url.protocol === "https:" ? https : http;
    const timeoutMs = this.o.timeoutMs ?? 15000;
    return new Promise((resolve, reject) => {
      const req = lib.request(url, { method: "GET", headers, agent: this.agent }, (res) => {
        const chunks: Buffer[] = [];
        let size = 0;
        res.on("data", (c: Buffer) => {
          size += c.length;
          if (size > MAX_BODY) {
            req.destroy(new Error("OpenELIS answer too large"));
            return;
          }
          chunks.push(c);
        });
        res.on("end", () =>
          resolve({ status: res.statusCode ?? 0, text: Buffer.concat(chunks).toString("utf8") }),
        );
        res.on("error", reject);
      });
      req.setTimeout(timeoutMs, () => req.destroy(new Error("OpenELIS did not answer in time")));
      req.on("error", reject);
      req.end();
    });
  }

  close() {
    this.agent.destroy();
  }
}

/** Printable ASCII only, Vietnamese and other accents folded, at most 100 chars. */
export function sanitizeActor(s: string): string {
  return s
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .normalize("NFKD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/[^\x20-\x7e]/g, "")
    .replace(/\s+/g, " ")
    .trim()
    .slice(0, 100);
}

export function sanitizePurpose(s: string): string {
  return s.replace(/[^A-Za-z0-9._:-]/g, "").slice(0, 64);
}

// The add-on service: the MCP endpoint, the admin API used by the settings page
// inside OpenELIS, the protected-resource metadata and a health check. Plain
// node:http; behind nginx at /addons/mcp/ (prefix stripped), so it sees /mcp,
// /api/…, /health.
import { createHash } from "node:crypto";
import http from "node:http";
import { toNodeHandler } from "@modelcontextprotocol/node";
import {
  createMcpHandler,
  isLegacyRequest,
  McpServer,
  WebStandardStreamableHTTPServerTransport,
} from "@modelcontextprotocol/server";
import type {
  AuthInfo,
  CallToolResult,
  McpHandlerRequestOptions,
  McpRequestContext,
} from "@modelcontextprotocol/server";
import * as z from "zod";
import { type Agent, AgentStore } from "./agents.js";
import { AuditLog, type AuditLine, type Outcome } from "./audit.js";
import { type Env, mcpUrl, resourceMetadataUrl, type Settings, settingsFrom } from "./config.js";
import { type CoreCall, CoreError, OpenElis } from "./openelis.js";
import { RateLimiter } from "./ratelimit.js";
import { stripPatients, TOOL_NAMES, TOOLS, type ToolDef, toolVisible } from "./tools.js";

export const VERSION = "1.0.0";
export const SETTINGS_SLOT = "addon-settings-page-slot";
const MCP_BODY_LIMIT = 1024 * 1024;
const ADMIN_BODY_LIMIT = 16 * 1024;
const CACHE_MS = 60_000;
const ADMIN_CACHE_MS = 30_000;
const PROTOCOL_VERSION_META_KEY = "io.modelcontextprotocol/protocolVersion";
const CLIENT_INFO_META_KEY = "io.modelcontextprotocol/clientInfo";

const INSTRUCTIONS =
  "Read-only access to one laboratory's OpenELIS. Lab numbers are accession numbers. " +
  "Dates are YYYY-MM-DD in the laboratory's time zone; date ranges are at most 31 days, both included. " +
  "Every call is recorded with this agent's name.";

export interface AppOptions {
  env: Env;
  core?: OpenElis;
  now?: () => Date;
}

interface WhoAmI {
  addon?: string;
  kind?: string;
  scopes?: string[];
  userId?: string | number;
  slot?: string;
}

/** What one MCP request carries from the HTTP layer into the per-request server. */
interface McpState {
  agent: Agent;
  granted: Set<string>;
  settings: Settings;
  method: string;
  tool: string | null;
  outcome?: Outcome;
  rows: number;
  core: CoreCall[];
}

export class App {
  readonly env: Env;
  readonly core: OpenElis;
  readonly agents: AgentStore;
  readonly audit: AuditLog;
  readonly limiter: RateLimiter;
  private readonly now: () => Date;
  private readonly mcp: ReturnType<typeof toNodeHandler>;
  private readonly mcpHandler: ReturnType<typeof createMcpHandler>;
  private scopes: { value: string[]; at: number } | null = null;
  private config: { value: Record<string, unknown>; at: number } | null = null;
  private readonly admins = new Map<string, { until: number; userId: string | null }>();
  private readonly clients = new Map<string, string>();
  private purgeTimer: NodeJS.Timeout | null = null;

  constructor(o: AppOptions) {
    this.env = o.env;
    this.now = o.now ?? (() => new Date());
    this.core =
      o.core ??
      new OpenElis({
        apiUrl: o.env.openelisApiUrl,
        apiKey: o.env.openelisApiKey,
        tlsInsecure: o.env.openelisTlsInsecure,
      });
    this.agents = new AgentStore(o.env.dataDir, this.now);
    this.audit = new AuditLog(o.env.dataDir, this.now);
    this.limiter = new RateLimiter(this.now);
    // 2026-07-28 traffic: the SDK's stateless per-request handler. 2025-era
    // traffic (initialize, then calls without the per-request envelope) is
    // routed by the SDK's own classifier to a stateless legacy leg that answers
    // JSON rather than SSE (the SDK's built-in fallback always streams).
    this.mcpHandler = createMcpHandler((ctx) => this.buildServer(ctx), {
      legacy: "reject",
      maxRequestBodySize: MCP_BODY_LIMIT,
      onerror: (e) => console.error("mcp:", e.message),
    });
    const modern = this.mcpHandler;
    const legacy = (request: Request, options?: McpHandlerRequestOptions) =>
      this.serveLegacy(request, options);
    const router = {
      fetch: async (request: Request, options?: McpHandlerRequestOptions) =>
        (await isLegacyRequest(request, options?.parsedBody, {
          maxRequestBodySize: MCP_BODY_LIMIT,
        }))
          ? legacy(request, options)
          : modern.fetch(request, options),
    };
    this.mcp = toNodeHandler(router, {
      maxRequestBodySize: MCP_BODY_LIMIT,
      onerror: (e) => console.error("mcp adapter:", e.message),
    });
  }

  // ---------------------------------------------------------------- core state

  /** Scopes OpenELIS currently grants this add-on (whoami with the service key, 60 s). */
  async grantedScopes(): Promise<Set<string>> {
    const t = this.now().getTime();
    if (this.scopes && t - this.scopes.at < CACHE_MS) return new Set(this.scopes.value);
    try {
      const w = await this.core.get<WhoAmI>("whoami", "/whoami");
      this.scopes = { value: Array.isArray(w.scopes) ? w.scopes.map(String) : [], at: t };
    } catch (e) {
      console.error("whoami:", (e as Error).message);
      if (!this.scopes) return new Set();
      // OpenELIS unreachable or the add-on turned off: a refused key means no scopes at all
      if (e instanceof CoreError && (e.status === 401 || e.status === 403))
        this.scopes = { value: [], at: t };
    }
    return new Set(this.scopes?.value ?? []);
  }

  /** The add-on configuration from the Extensions page (60 s; defaults when unreachable). */
  async settings(): Promise<Settings> {
    const t = this.now().getTime();
    if (!this.config || t - this.config.at >= CACHE_MS) {
      try {
        const c = await this.core.get<Record<string, unknown>>("config", "/config");
        this.config = { value: c && typeof c === "object" ? c : {}, at: t };
      } catch (e) {
        console.error("config:", (e as Error).message);
        if (!this.config) return settingsFrom({}, this.env);
      }
    }
    return settingsFrom(this.config?.value ?? {}, this.env);
  }

  // ---------------------------------------------------------------- HTTP

  /** RFC 9728 Protected Resource Metadata. No authorization server yet: tokens are issued on the settings page. */
  private metadata(res: http.ServerResponse) {
    return json(res, 200, {
      resource: mcpUrl(this.env),
      bearer_methods_supported: ["header"],
      resource_name: "OpenELIS MCP server",
    });
  }

  readonly handle = async (req: http.IncomingMessage, res: http.ServerResponse) => {
    try {
      const url = new URL(req.url ?? "/", "http://addon");
      let path = url.pathname;
      if (path === "/addons/mcp" || path.startsWith("/addons/mcp/"))
        path = path.slice("/addons/mcp".length) || "/";
      // behind the generic per-add-on route (/addons/<id>/api/…) the endpoint is /api/mcp
      if (path === "/mcp" || path === "/mcp/" || path === "/api/mcp")
        return await this.handleMcp(req, res);
      if (req.method === "GET" && path === "/api/.well-known/oauth-protected-resource")
        return this.metadata(res);
      if (path.startsWith("/api/"))
        return await this.handleAdmin(req, res, path.slice(4), url.searchParams);
      if (req.method === "GET" && path === "/health") return await this.health(res);
      if (
        req.method === "GET" &&
        (path === "/.well-known/oauth-protected-resource" ||
          path === "/.well-known/oauth-protected-resource/addons/mcp/mcp")
      ) {
        return this.metadata(res);
      }
      json(res, 404, { error: "not_found" });
    } catch (e) {
      console.error("request:", (e as Error).stack ?? e);
      if (!res.headersSent) json(res, 500, { error: "internal" });
      else res.end();
    }
  };

  private async health(res: http.ServerResponse) {
    const scopes = await this.grantedScopes();
    json(res, 200, {
      status: "ok",
      version: VERSION,
      openelis: this.scopes ? "ok" : "unreachable",
      scopes: scopes.size,
    });
  }

  // ---------------------------------------------------------------- MCP

  private async handleMcp(req: http.IncomingMessage, res: http.ServerResponse) {
    const started = Date.now();
    if (req.method !== "POST") {
      res.writeHead(405, { Allow: "POST", "Content-Type": "application/json" });
      res.end(JSON.stringify(rpcError(-32000, "Method not allowed.")));
      return;
    }
    const settings = await this.settings();
    const origin = req.headers.origin;
    if (origin !== undefined && !settings.allowedOrigins.includes(origin)) {
      json(res, 403, rpcError(-32000, "Forbidden: origin not allowed"));
      return;
    }

    const bearer = /^Bearer (\S+)$/i.exec(req.headers.authorization ?? "")?.[1];
    const agent = this.agents.verify(bearer);
    if (!agent) {
      const tool = req.headers["mcp-name"];
      this.audit.write(
        this.line(req, {
          agent: null,
          tool: typeof tool === "string" ? tool.slice(0, 64) : null,
          outcome: "unauthorized",
          started,
        }),
      );
      const challenge = `Bearer resource_metadata="${resourceMetadataUrl(this.env)}"${bearer ? ', error="invalid_token"' : ""}`;
      res.writeHead(401, { "WWW-Authenticate": challenge, "Content-Type": "application/json" });
      res.end(
        JSON.stringify({
          error: bearer ? "invalid_token" : "unauthorized",
          error_description: "A valid agent token is required (Authorization: Bearer oemcp_…).",
        }),
      );
      return;
    }

    let body: unknown;
    try {
      body = JSON.parse((await readBody(req, MCP_BODY_LIMIT)).toString("utf8"));
    } catch (e) {
      if ((e as Error).message === "too_large")
        json(res, 413, rpcError(-32600, "Request body too large"));
      else json(res, 400, rpcError(-32700, "Parse error"));
      return;
    }
    if (Array.isArray(body)) {
      json(res, 400, rpcError(-32600, "Batches are not supported"));
      return;
    }
    const msg = (body && typeof body === "object" ? body : {}) as Record<string, unknown>;
    const params = (msg.params && typeof msg.params === "object" ? msg.params : {}) as Record<
      string,
      unknown
    >;
    const method = typeof msg.method === "string" ? msg.method : "";
    const meta = (params._meta && typeof params._meta === "object" ? params._meta : {}) as Record<
      string,
      unknown
    >;

    if (method === "initialize") {
      const ci = clientName(params.clientInfo);
      if (ci) this.rememberClient(agent.id, ci);
    }
    const client = clientName(meta[CLIENT_INFO_META_KEY]) ?? this.clients.get(agent.id) ?? null;
    const header = req.headers["mcp-protocol-version"];
    const proto =
      (typeof meta[PROTOCOL_VERSION_META_KEY] === "string"
        ? (meta[PROTOCOL_VERSION_META_KEY] as string)
        : null) ??
      (typeof header === "string" ? header : null) ??
      (method === "initialize" && typeof params.protocolVersion === "string"
        ? params.protocolVersion
        : null);

    const isCall = method === "tools/call";
    const state: McpState = {
      agent,
      granted: await this.grantedScopes(),
      settings,
      method,
      tool: isCall && typeof params.name === "string" ? params.name.slice(0, 64) : null,
      rows: 0,
      core: [],
    };
    const authed = req as http.IncomingMessage & { auth?: AuthInfo };
    authed.auth = { token: "agent", clientId: agent.id, scopes: [], extra: { state } };
    await this.mcp(authed, res, body);

    if (isCall) {
      this.audit.write({
        ...this.line(req, {
          agent,
          tool: state.tool,
          outcome: state.outcome ?? "invalid",
          started,
        }),
        client,
        proto,
        args: boundedArgs(params.arguments),
        rows: state.rows,
        core: state.core,
      });
    }
  }

  private rememberClient(id: string, name: string) {
    this.clients.delete(id);
    this.clients.set(id, name);
    if (this.clients.size > 1000) this.clients.delete(this.clients.keys().next().value as string);
  }

  private line(
    req: http.IncomingMessage,
    o: { agent: Agent | null; tool: string | null; outcome: Outcome; started: number },
  ): AuditLine {
    return {
      at: this.now().toISOString(),
      agent: o.agent?.id ?? null,
      agentName: o.agent?.name ?? null,
      client: null,
      proto:
        typeof req.headers["mcp-protocol-version"] === "string"
          ? req.headers["mcp-protocol-version"]
          : null,
      ip: clientIp(req),
      tool: o.tool,
      args: null,
      outcome: o.outcome,
      rows: 0,
      core: [],
      ms: Date.now() - o.started,
    };
  }

  /** A 2025-era request: a fresh server and transport, JSON answer, nothing kept. */
  private async serveLegacy(
    request: Request,
    options?: McpHandlerRequestOptions,
  ): Promise<Response> {
    const server = this.buildServer({
      era: "legacy",
      authInfo: options?.authInfo,
      requestInfo: request,
    });
    const transport = new WebStandardStreamableHTTPServerTransport({
      sessionIdGenerator: undefined,
      enableJsonResponse: true,
      maxRequestBodySize: MCP_BODY_LIMIT,
    });
    await server.connect(transport);
    try {
      return await transport.handleRequest(request, {
        authInfo: options?.authInfo,
        parsedBody: options?.parsedBody,
      });
    } finally {
      void transport.close().catch(() => undefined);
      void server.close().catch(() => undefined);
    }
  }

  /** One McpServer per HTTP request (stateless), offering what this agent may use. */
  private buildServer(ctx: McpRequestContext): McpServer {
    const server = new McpServer(
      { name: "openelis-mcp", title: "OpenELIS", version: VERSION },
      { capabilities: { tools: { listChanged: false } }, instructions: INSTRUCTIONS },
    );
    const state = ctx.authInfo?.extra?.state as McpState | undefined;
    if (!state) return server;
    for (const def of TOOLS) {
      if (toolVisible(def, state.granted, state.agent)) this.registerTool(server, def, state);
      else if (state.method === "tools/call" && state.tool === def.name) {
        // hidden from tools/list, but a direct call gets a clear refusal (and is audited)
        server.registerTool(def.name, { inputSchema: z.looseObject({}) }, async () => {
          state.outcome = "denied";
          return toolError(`forbidden: this agent may not use ${def.name}`);
        });
      }
    }
    return server;
  }

  private registerTool(server: McpServer, def: ToolDef, state: McpState) {
    server.registerTool(
      def.name,
      {
        title: def.title,
        description: def.description,
        inputSchema: def.input,
        outputSchema: def.output,
        annotations: {
          title: def.title,
          readOnlyHint: true,
          destructiveHint: false,
          idempotentHint: true,
          openWorldHint: false,
        },
      },
      async (args: Record<string, unknown>): Promise<CallToolResult> => {
        const v = this.limiter.take(state.agent.id, state.settings);
        if (!v.ok) {
          state.outcome = "rate_limited";
          return toolError(
            `rate_limited: this agent's ${v.limit === "day" ? "daily" : "per-minute"} limit is reached; ` +
              `retry after ${v.retryAfterSeconds} seconds`,
          );
        }
        const actor = `agent:${state.agent.id} ${state.agent.name}`;
        // least privilege per call: only the tool's scopes, plus identities when this
        // agent may see them; OpenELIS enforces it, the strip below is a second guard
        const scopes = [...def.scopes, ...(state.agent.identifiers ? ["patient.identity"] : [])];
        const get = <T>(
          path: string,
          template: string,
          query?: Record<string, string | number | undefined>,
        ) =>
          this.core.get<T>(path, template, query, {
            actor,
            purpose: def.name,
            scopes: [...new Set(scopes)],
            calls: state.core,
          });
        try {
          const r = await def.run(get, args);
          const data = state.agent.identifiers ? r.data : stripPatients(r.data);
          if (!def.output.safeParse(data).success) {
            state.outcome = "error";
            return toolError("error: OpenELIS answered in an unexpected shape");
          }
          state.outcome = "ok";
          state.rows = r.rows;
          return {
            content: [{ type: "text", text: `${r.summary}\n\n${JSON.stringify(data)}` }],
            structuredContent: data,
          };
        } catch (e) {
          const [outcome, message] = coreFailure(e);
          state.outcome = outcome;
          return toolError(message);
        }
      },
    );
  }

  // ---------------------------------------------------------------- admin API

  private async handleAdmin(
    req: http.IncomingMessage,
    res: http.ServerResponse,
    path: string,
    q: URLSearchParams,
  ) {
    res.setHeader("Access-Control-Allow-Origin", "*");
    res.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
    res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    res.setHeader("Access-Control-Max-Age", "600");
    res.setHeader("Cache-Control", "no-store");
    res.setHeader("Vary", "Authorization");
    if (req.method === "OPTIONS") {
      res.writeHead(204);
      res.end();
      return;
    }
    const admin = await this.admin(req.headers.authorization);
    if (admin === "unauthorized") return json(res, 401, { error: "token" });
    if (admin === "forbidden") return json(res, 403, { error: "forbidden" });
    if (admin === "unavailable") return json(res, 503, { error: "openelis" });

    if (req.method === "GET" && path === "/agents")
      return json(res, 200, { agents: this.agents.list() });
    if (req.method === "POST" && path === "/agents") {
      let b: Record<string, unknown>;
      try {
        b = JSON.parse((await readBody(req, ADMIN_BODY_LIMIT)).toString("utf8")) as Record<
          string,
          unknown
        >;
      } catch {
        return json(res, 400, { error: "body" });
      }
      const parsed = NEW_AGENT.safeParse(b);
      if (!parsed.success)
        return json(res, 400, {
          error: "invalid",
          issues: parsed.error.issues.map((i) => i.message),
        });
      const created = this.agents.create({
        name: parsed.data.name,
        identifiers: parsed.data.identifiers,
        tools: parsed.data.tools,
        expiresInDays: parsed.data.expiresInDays,
        createdBy: admin.userId ?? "",
      });
      return json(res, 201, { agent: created.agent, token: created.token });
    }
    const revoke = /^\/agents\/([0-9a-f]{8})\/revoke$/.exec(path);
    if (req.method === "POST" && revoke) {
      const a = this.agents.revoke(revoke[1] as string);
      return a ? json(res, 200, { agent: a }) : json(res, 404, { error: "not_found" });
    }
    if (req.method === "GET" && path === "/audit") {
      const limit = Number(q.get("limit") ?? 200);
      return json(res, 200, {
        lines: this.audit.read({
          from: q.get("from") ?? undefined,
          to: q.get("to") ?? undefined,
          agent: q.get("agent") ?? undefined,
          limit: Number.isFinite(limit) ? limit : 200,
        }),
      });
    }
    if (req.method === "GET" && path === "/status") {
      const granted = await this.grantedScopes();
      const s = await this.settings();
      return json(res, 200, {
        version: VERSION,
        endpoint: mcpUrl(this.env),
        resourceMetadata: resourceMetadataUrl(this.env),
        openelis: this.scopes ? "ok" : "unreachable",
        grantedScopes: [...granted].sort(),
        settings: s,
        tools: TOOLS.map((t) => ({
          name: t.name,
          title: t.title,
          scopes: t.scopes,
          identifiers: t.identifiers,
          granted: t.scopes.every((x) => granted.has(x)),
        })),
      });
    }
    json(res, 404, { error: "not_found" });
  }

  /**
   * The caller must be an OpenELIS administrator on this add-on's settings
   * page: OpenELIS confirms the UI token (whoami with that token) and its slot.
   */
  private async admin(
    authorization: string | undefined,
  ): Promise<{ userId: string | null } | "unauthorized" | "forbidden" | "unavailable"> {
    const token = /^Bearer (oeat\.\S+)$/.exec(authorization ?? "")?.[1];
    if (!token) return "unauthorized";
    const key = createHash("sha256").update(token).digest("hex");
    const t = this.now().getTime();
    const hit = this.admins.get(key);
    if (hit && hit.until > t) return { userId: hit.userId };
    let w: WhoAmI;
    try {
      w = await this.core.get<WhoAmI>("whoami", "/whoami", {}, { token });
    } catch (e) {
      if (e instanceof CoreError && (e.status === 401 || e.status === 403)) return "unauthorized";
      return "unavailable";
    }
    if (w.kind !== "user" || w.slot !== SETTINGS_SLOT) return "forbidden";
    const userId = w.userId === undefined || w.userId === null ? null : String(w.userId);
    for (const [k, v] of this.admins) if (v.until <= t) this.admins.delete(k);
    this.admins.set(key, { until: t + ADMIN_CACHE_MS, userId });
    return { userId };
  }

  // ---------------------------------------------------------------- lifecycle

  async purgeNow() {
    const s = await this.settings();
    return this.audit.purge(s.auditRetentionDays);
  }

  listen(port: number, host = "0.0.0.0"): Promise<http.Server> {
    const server = http.createServer(this.handle);
    server.requestTimeout = 60_000;
    server.headersTimeout = 20_000;
    void this.purgeNow().catch(() => undefined);
    this.purgeTimer = setInterval(
      () => void this.purgeNow().catch(() => undefined),
      24 * 3600 * 1000,
    );
    this.purgeTimer.unref();
    return new Promise((resolve) => server.listen(port, host, () => resolve(server)));
  }

  async close() {
    if (this.purgeTimer) clearInterval(this.purgeTimer);
    this.agents.flush();
    await this.mcpHandler.close();
    this.core.close();
  }
}

const NEW_AGENT = z.object({
  name: z.string().trim().min(1, "name is required").max(100, "name: at most 100 characters"),
  identifiers: z.boolean().default(false),
  tools: z
    .array(z.enum(TOOL_NAMES as [string, ...string[]]))
    .min(1, "tools: choose at least one, or leave it out for all")
    .optional(),
  expiresInDays: z.number().int().min(1).max(3650).optional(),
});

function coreFailure(e: unknown): [Outcome, string] {
  if (e instanceof CoreError) {
    if (e.status === 403 && e.code === "scope") {
      return [
        "denied",
        `forbidden: OpenELIS has not granted scope ${e.scope ?? "?"} to this add-on`,
      ];
    }
    if (e.status === 403)
      return ["denied", "forbidden: OpenELIS does not let this add-on read that"];
    if (e.status === 404) {
      const what = e.code === "patient" ? "patient" : e.code === "order" ? "order" : "record";
      return ["not_found", `not_found: no such ${what}`];
    }
    if (e.status === 400) {
      const p = typeof e.body.param === "string" ? ` (${e.body.param})` : "";
      return ["invalid", `invalid: OpenELIS refused the request${p}: ${e.code ?? "param"}`];
    }
    if (e.status === 401)
      return ["error", "error: OpenELIS refused this add-on's key (turned off or key rotated)"];
    return ["error", `error: OpenELIS answered ${e.status}`];
  }
  return ["error", "error: OpenELIS could not be reached"];
}

function toolError(text: string): CallToolResult {
  return { content: [{ type: "text", text }], isError: true };
}

function rpcError(code: number, message: string) {
  return { jsonrpc: "2.0", error: { code, message }, id: null };
}

function json(res: http.ServerResponse, status: number, body: unknown) {
  res.writeHead(status, { "Content-Type": "application/json; charset=utf-8" });
  res.end(JSON.stringify(body));
}

function clientName(v: unknown): string | null {
  if (!v || typeof v !== "object") return null;
  const c = v as Record<string, unknown>;
  if (typeof c.name !== "string") return null;
  const s = typeof c.version === "string" ? `${c.name}/${c.version}` : c.name;
  return s.replace(/[^\x20-\x7e]/g, "").slice(0, 100);
}

export function clientIp(req: http.IncomingMessage): string | null {
  const xff = req.headers["x-forwarded-for"];
  const first = (Array.isArray(xff) ? xff[0] : xff)?.split(",")[0]?.trim();
  return (first || req.socket.remoteAddress || null)?.slice(0, 64) ?? null;
}

function boundedArgs(a: unknown): unknown {
  if (a === undefined) return null;
  const s = JSON.stringify(a);
  return s.length <= 2000 ? a : { truncated: s.slice(0, 2000) };
}

function readBody(req: http.IncomingMessage, limit: number): Promise<Buffer> {
  return new Promise((resolve, reject) => {
    const chunks: Buffer[] = [];
    let size = 0;
    req.on("data", (c: Buffer) => {
      size += c.length;
      if (size > limit) {
        reject(new Error("too_large"));
        req.resume();
        return;
      }
      chunks.push(c);
    });
    req.on("end", () => resolve(Buffer.concat(chunks)));
    req.on("error", reject);
  });
}

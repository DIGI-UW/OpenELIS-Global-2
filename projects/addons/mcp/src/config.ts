// Environment (set by the operator in compose) and the add-on's configuration
// (set by an administrator on the Extensions page, read from OpenELIS).

export interface Env {
  port: number;
  dataDir: string;
  /** The laboratory's public origin, e.g. https://lims.example.org (no path). */
  publicOrigin: string;
  openelisApiUrl: string;
  openelisApiKey: string;
  openelisTlsInsecure: boolean;
  /** Path of the MCP endpoint at the public origin (MCP_PUBLIC_PATH). */
  mcpPath: string;
  /** Path of its RFC 9728 metadata at the public origin (MCP_METADATA_PATH). */
  metadataPath: string;
}

export function readEnv(e: NodeJS.ProcessEnv = process.env): Env {
  const publicOrigin = normalizeOrigin(e.PUBLIC_ORIGIN ?? "");
  if (!publicOrigin)
    throw new Error("PUBLIC_ORIGIN must be the laboratory's origin, e.g. https://lims.example.org");
  const openelisApiUrl = (e.OPENELIS_API_URL ?? "").replace(/\/+$/, "");
  if (!/^https?:\/\//.test(openelisApiUrl)) throw new Error("OPENELIS_API_URL is not set");
  const openelisApiKey = e.OPENELIS_API_KEY ?? "";
  if (!openelisApiKey) throw new Error("OPENELIS_API_KEY is not set");
  return {
    port: Number(e.PORT ?? 8080) || 8080,
    dataDir: e.MCP_DATA_DIR ?? "/data",
    publicOrigin,
    openelisApiUrl,
    openelisApiKey,
    openelisTlsInsecure: e.OPENELIS_TLS_INSECURE === "1" || e.OPENELIS_TLS_INSECURE === "true",
    mcpPath: cleanPath(e.MCP_PUBLIC_PATH, "/addons/mcp/mcp"),
    metadataPath: cleanPath(
      e.MCP_METADATA_PATH,
      defaultMetadataPath(cleanPath(e.MCP_PUBLIC_PATH, "/addons/mcp/mcp")),
    ),
  };
}

/** `https://Host:443/x` -> `https://host`; anything that is not an http(s) origin -> "". */
export function normalizeOrigin(value: string): string {
  try {
    const u = new URL(value.trim());
    if (u.protocol !== "https:" && u.protocol !== "http:") return "";
    return u.origin;
  } catch {
    return "";
  }
}

/**
 * Behind the generic per-add-on route the endpoint is `…/api/mcp` and the add-on
 * serves its metadata at `…/api/.well-known/oauth-protected-resource`; otherwise
 * the RFC 9728 path-suffixed form.
 */
function defaultMetadataPath(mcpPath: string): string {
  return mcpPath.endsWith("/api/mcp")
    ? mcpPath.slice(0, -"mcp".length) + ".well-known/oauth-protected-resource"
    : "/.well-known/oauth-protected-resource" + mcpPath;
}

function cleanPath(value: string | undefined, fallback: string): string {
  const v = (value ?? "").trim();
  return /^\/[A-Za-z0-9._~\/-]*$/.test(v) ? v.replace(/\/+$/, "") || "/" : fallback;
}

/**
 * The MCP endpoint as clients reach it through the reverse proxy. By default
 * `<origin>/addons/mcp/mcp` (a dedicated nginx location); a site that only has
 * the generic per-add-on API route sets MCP_PUBLIC_PATH=/addons/mcp/api/mcp.
 */
export const mcpUrl = (env: Pick<Env, "publicOrigin"> & Partial<Pick<Env, "mcpPath">>) =>
  `${env.publicOrigin}${env.mcpPath ?? "/addons/mcp/mcp"}`;

/** RFC 9728 metadata URL for that resource (path-suffixed form by default). */
export const resourceMetadataUrl = (
  env: Pick<Env, "publicOrigin"> & Partial<Pick<Env, "metadataPath">>,
) =>
  `${env.publicOrigin}${env.metadataPath ?? "/.well-known/oauth-protected-resource/addons/mcp/mcp"}`;

export interface Settings {
  ratePerMinute: number;
  ratePerDay: number;
  auditRetentionDays: number;
  timeZone: string;
  /** Exact origins allowed to send `Origin` to /mcp (normalized). */
  allowedOrigins: string[];
}

export function settingsFrom(
  c: Record<string, unknown> | null | undefined,
  env: Pick<Env, "publicOrigin">,
): Settings {
  const origins = str(c, "allowedOrigins", "")
    .split(",")
    .map((s) => normalizeOrigin(s))
    .filter(Boolean);
  return {
    ratePerMinute: num(c, "ratePerMinute", 30, 1, 600),
    ratePerDay: num(c, "ratePerDay", 1000, 1, 100000),
    auditRetentionDays: num(c, "auditRetentionDays", 180, 30, 3650),
    timeZone: zone(str(c, "timeZone", "UTC")),
    allowedOrigins: origins.length ? origins : [env.publicOrigin],
  };
}

function str(c: Record<string, unknown> | null | undefined, k: string, fallback: string): string {
  const v = c?.[k];
  return v === undefined || v === null || String(v).trim() === "" ? fallback : String(v).trim();
}

function num(
  c: Record<string, unknown> | null | undefined,
  k: string,
  fallback: number,
  min: number,
  max: number,
) {
  const n = Number(c?.[k]);
  if (c?.[k] === undefined || c?.[k] === null || c?.[k] === "" || !Number.isFinite(n))
    return fallback;
  return Math.min(max, Math.max(min, Math.floor(n)));
}

function zone(z: string): string {
  try {
    new Intl.DateTimeFormat("en-CA", { timeZone: z });
    return z;
  } catch {
    return "UTC";
  }
}

/** YYYY-MM-DD of an instant in a time zone. */
export function dayIn(timeZone: string, at: Date = new Date()): string {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(at);
}

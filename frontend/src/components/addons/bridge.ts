/**
 * The postMessage protocol between OpenELIS and an add-on frame. The frame is
 * sandboxed without allow-same-origin, so its origin is "null": the host checks
 * the sender by window (event.source), never by origin, and posts with "*" to
 * that one frame's contentWindow only.
 *
 *   frame -> host  {type: "ready"} | {type: "resize", height} | {type: "token-refresh"}
 *   host -> frame  {type: "context", slot, extension, context, token, expiresIn,
 *                   scopes, locale, apiBase}
 *
 * The context is what the server returned with the token: already filtered by
 * the add-on's scopes. Nothing else (session, user, patient details) is sent.
 */
export type FrameMessage =
  | { type: "ready" }
  | { type: "token-refresh" }
  | { type: "resize"; height: number };

export const MIN_HEIGHT = 80;
export const MAX_HEIGHT = 900;

export function clampHeight(height: number) {
  return Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, Math.round(height)));
}

export function parseFrameMessage(data: unknown): FrameMessage | null {
  if (!data || typeof data !== "object") return null;
  const d = data as Record<string, unknown>;
  if (d.type === "ready" || d.type === "token-refresh") {
    return { type: d.type };
  }
  if (d.type === "resize") {
    const h = Number(d.height);
    return Number.isFinite(h)
      ? { type: "resize", height: clampHeight(h) }
      : null;
  }
  return null;
}

export interface HostContext {
  slot: string;
  extension: string;
  context: Record<string, unknown>;
  token: string;
  expiresIn: number;
  scopes: string[];
  locale: string;
  apiBase: string;
}

export const contextMessage = (c: HostContext) => ({ type: "context", ...c });

/** Stable key for a state object: a change means a new token. */
export const stateKey = (state?: Record<string, unknown>) =>
  JSON.stringify(state || {});

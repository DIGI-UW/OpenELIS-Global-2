import type { ConfigField } from "./addonApi";

/**
 * Starting values of the configuration form: saved value, else the manifest
 * default. Secret fields start empty: the server never sends them back.
 */
export function initialConfig(
  schema: ConfigField[],
  saved: Record<string, unknown> | undefined,
): Record<string, unknown> {
  const out: Record<string, unknown> = {};
  for (const f of schema) {
    const value = saved?.[f.key];
    if (f.type === "secret") {
      out[f.key] = "";
    } else if (value !== undefined && value !== null) {
      out[f.key] = value;
    } else if (f.defaultValue !== null && f.defaultValue !== undefined) {
      out[f.key] = f.defaultValue;
    } else {
      out[f.key] =
        f.type === "boolean" ? false : f.type === "number" ? (f.min ?? 0) : "";
    }
  }
  return out;
}

/**
 * What to send when saving: every plain field; a secret only when the user typed
 * a new value (absent = keep the stored one) or cleared it (sent as "").
 */
export function configToSave(
  schema: ConfigField[],
  values: Record<string, unknown>,
  cleared: ReadonlySet<string> = new Set(),
): Record<string, unknown> {
  const out: Record<string, unknown> = {};
  for (const f of schema) {
    if (f.type !== "secret") {
      out[f.key] = values[f.key];
    } else if (cleared.has(f.key)) {
      out[f.key] = "";
    } else if (typeof values[f.key] === "string" && values[f.key] !== "") {
      out[f.key] = values[f.key];
    }
  }
  return out;
}

import type { ConfigField } from "./addonApi";

/** Starting values of the configuration form: saved value, else the manifest default. */
export function initialConfig(
  schema: ConfigField[],
  saved: Record<string, unknown> | undefined,
): Record<string, unknown> {
  const out: Record<string, unknown> = {};
  for (const f of schema) {
    const value = saved?.[f.key];
    if (value !== undefined && value !== null) {
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

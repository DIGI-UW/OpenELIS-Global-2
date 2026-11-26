import { readFileSync } from "fs";

/**
 * The application's own English, so an expected sentence is the one the
 * screen is built from rather than a copy of it that drifts.
 */
const MESSAGES = JSON.parse(
  readFileSync(new URL("../../src/languages/en.json", import.meta.url), "utf8"),
) as Record<string, string>;

/** One message with its plain `{name}` placeholders filled in. */
export function text(
  key: string,
  values: Record<string, string | number> = {},
) {
  const message = MESSAGES[key];
  if (message === undefined) {
    throw new Error(`en.json has no message ${key}`);
  }
  return Object.entries(values).reduce(
    (filled, [name, value]) => filled.split(`{${name}}`).join(String(value)),
    message,
  );
}

/**
 * One branch of a plural message for a count, with `#` filled in. Only the
 * `one` and `other` branches are read, which is all English has.
 */
export function plural(key: string, count: number) {
  const branch = count === 1 ? "one" : "other";
  const found = new RegExp(`\\b${branch} \\{([^}]*)\\}`).exec(text(key));
  if (!found) {
    throw new Error(`en.json message ${key} has no ${branch} branch`);
  }
  return found[1].split("#").join(String(count));
}

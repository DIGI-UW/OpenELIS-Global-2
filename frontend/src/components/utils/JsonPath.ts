import { JSONPath } from "jsonpath-plus";

/**
 * Get a value from an object using a JSONPath/dot-bracket path.
 * Returns the first matched value directly (unwrapped).
 *
 * @param {object} obj - The object to query
 * @param {string} path - A dot-bracket path (e.g. "testResult[0].refer")
 * @returns {*} The matched value, or undefined if not found
 */
export function jpGet<T = unknown>(obj: object, path: string): T | undefined {
  return JSONPath({ path, json: obj, wrap: false });
}

/**
 * Set a value on an object at the location specified by a JSONPath/dot-bracket path.
 * Mutates the object in place.
 *
 * jsonpath-plus does not have a built-in setter, so this resolves the parent
 * via resultType:"all" and assigns through it.
 *
 * @param {object} obj - The object to mutate
 * @param {string} path - A dot-bracket path (e.g. "resultList[0].sentDate_")
 * @param {*} val - The value to set
 */
export function jpSet(obj: object, path: string, val: unknown): void {
  const results = JSONPath({ path, json: obj, resultType: "all" });
  if (results.length > 0) {
    const result = results[0];
    if (result.parent && result.parentProperty !== null) {
      (result.parent as Record<PropertyKey, unknown>)[result.parentProperty] =
        val;
    }
    return;
  }
  const lastDot = path.lastIndexOf(".");
  if (lastDot === -1) return;
  const parent = JSONPath({
    path: path.substring(0, lastDot),
    json: obj,
    wrap: false,
  });
  if (parent && typeof parent === "object") {
    (parent as Record<string, unknown>)[path.substring(lastDot + 1)] = val;
  }
}

import { JSONPath } from "jsonpath-plus";

/**
 * Get a value from an object using a JSONPath/dot-bracket path.
 * Returns the first matched value directly (unwrapped).
 *
 * @param {object} obj - The object to query
 * @param {string} path - A dot-bracket path (e.g. "testResult[0].refer")
 * @returns {*} The matched value, or undefined if not found
 */
export function jpGet(obj, path) {
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
export function jpSet(obj, path, val) {
  const results = JSONPath({ path, json: obj, resultType: "all" });
  if (results.length > 0) {
    results[0].parent[results[0].parentProperty] = val;
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
    parent[path.substring(lastDot + 1)] = val;
  }
}

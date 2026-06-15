/**
 * Pure helper functions for label preset management (OGC-285 M3, task T076).
 */

/**
 * Normalizes a preset name: trims leading/trailing whitespace and converts to
 * lower-case. Mirrors the server-side normalizeName() in LabelPresetServiceImpl.
 *
 * @param {string} input - raw name input
 * @returns {string} normalized name
 */
export function normalizeName(input) {
  if (!input) return "";
  return input.trim().toLowerCase();
}

/**
 * Reorders field entries by their displayOrder property (ascending).
 * Returns a new array — does not mutate the input.
 *
 * @param {Array<{displayOrder: number}>} fields - array of field entries
 * @returns {Array} sorted copy
 */
export function reorderFields(fields) {
  if (!Array.isArray(fields)) return [];
  return [...fields].sort(
    (a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0),
  );
}

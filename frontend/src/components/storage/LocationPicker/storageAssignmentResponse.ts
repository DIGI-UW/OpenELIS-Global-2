/**
 * Interprets the JSON body of a storage assign/move response.
 *
 * Precedence:
 *   1. `success === true`  → definitive success (authoritative)
 *   2. `success === false` → definitive failure (authoritative)
 *   3. legacy heuristic: no error/message field AND at least one known
 *      identifier is present (assignmentId / movementId / hierarchicalPath /
 *      newHierarchicalPath).
 *
 * Only the boolean literal `true` short-circuits positive; truthy non-booleans
 * (e.g. "true", 1) fall through to the heuristic to avoid accidental
 * positives from string/numeric backend drift.
 *
 * HTTP-level gating (`response.ok`) is the caller's responsibility.
 */
interface StorageAssignmentResponse {
  success?: boolean;
  error?: unknown;
  message?: unknown;
  assignmentId?: unknown;
  movementId?: unknown;
  hierarchicalPath?: unknown;
  newHierarchicalPath?: unknown;
}

export function isStorageAssignmentSuccess(
  body: StorageAssignmentResponse | null | undefined,
) {
  if (!body) return false;

  if (body.success === true) return true;
  if (body.success === false) return false;

  if (body.error || body.message) return false;

  return Boolean(
    body.assignmentId ||
    body.movementId ||
    body.hierarchicalPath ||
    body.newHierarchicalPath,
  );
}

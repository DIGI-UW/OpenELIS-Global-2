/**
 * The eleven bench stages a pathology case moves through, in the order the
 * bench actually works them. Kept as a pure module (no React, no I/O) so the
 * dashboard, the case view and any later pathology screen read the same
 * order and the same "in progress" grouping the backend dashboard tiles use,
 * rather than each screen re-deriving its own list and drifting apart.
 */

export const PATHOLOGY_STAGES = Object.freeze([
  "ACCESSIONED",
  "GROSSING",
  "DECALCIFICATION",
  "PROCESSING",
  "EMBEDDING",
  "MICROTOMY",
  "STAINING",
  "COVERSLIPPING",
  "READY_PATHOLOGIST",
  "UNDER_REVIEW",
  "COMPLETED",
]);

function toLowerCamel(stageId) {
  const [first, ...rest] = stageId.toLowerCase().split("_");
  return (
    first +
    rest.map((word) => word.charAt(0).toUpperCase() + word.slice(1)).join("")
  );
}

// i18n-keys: pathology.stage.*
export function stageDisplayKey(stageId) {
  return "pathology.stage." + toLowerCamel(stageId);
}

/**
 * A server newer than this build may send a stage id this module does not
 * know about yet; in that case there is no key to translate, so the caller's
 * fallback (or the raw id) is returned unchanged instead of a broken lookup.
 */
export function stageLabel(intl, stageId, fallback) {
  if (!PATHOLOGY_STAGES.includes(stageId)) {
    return fallback ?? stageId;
  }
  return intl.formatMessage({
    id: stageDisplayKey(stageId),
    defaultMessage: fallback ?? stageId,
  });
}

// Matches the backend dashboard tile grouping: everything except the two
// stages that already have their own tile (awaiting review and complete).
// Filtering the served list directly, rather than a second copy of the stage
// names kept here, is what stops a stage the server has learned about from
// silently dropping out of the grouping while the backend tile still counts
// it.
export function inProgressStageIds(servedIds) {
  return servedIds.filter(
    (stage) => stage !== "READY_PATHOLOGIST" && stage !== "COMPLETED",
  );
}

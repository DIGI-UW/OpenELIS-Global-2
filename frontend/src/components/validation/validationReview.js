/**
 * OGC-1028 (Validation v4 slice V2) — pure helpers behind the per-row review
 * panel: result flag styling, multi-component grouping (FR-C4), the dual-axis
 * note (FR-F1) and the per-row action payloads/errors (FR-D1, FR-D4).
 * Kept free of React so every rule is unit-testable.
 */
import { triageRows } from "./validationTriage";

export const NOTE_INTERNAL = "I";
export const NOTE_EXTERNAL = "E";
export const NOTE_CONTEXT_VALIDATION = "VALIDATION";
export const NOTE_CONTEXT_MODIFICATION = "MODIFICATION";

/** result types the panel can edit inline; multi-selects stay on Results Entry */
const EDITABLE_RESULT_TYPES = ["N", "A", "R", "D"];

const KNOWN_ERRORS = [
  "notAwaitingValidation",
  "qcAcknowledgmentRequired",
  "modificationReasonRequired",
  "modifyRoleRequired",
  "resultRequired",
  "invalidResult",
];

/**
 * The flag the summary styles the value with. Critical wins (authored critical
 * bound crossed, computed server-side); abnormal/normal only when the row has a
 * reference range — an unranged row shows no flag rather than a false "Normal".
 */
export function flagFor(row, signals) {
  if (!row) {
    return undefined;
  }
  const derived = signals || triageRows([row])[0].signals;
  if (row.critical) {
    return "CRITICAL";
  }
  if (derived.abnormal) {
    return "ABNORMAL";
  }
  if (derived.inRange) {
    return "NORMAL";
  }
  return undefined;
}

const componentOrder = (row) =>
  row.componentDisplayOrder === null || row.componentDisplayOrder === undefined
    ? -1
    : Number(row.componentDisplayOrder);

/**
 * FR-C4 — every queue row of the same analysis (the queue serves one row per
 * result component), primary first then by display_order.
 */
export function componentRowsFor(rows, analysisId) {
  if (!analysisId) {
    return [];
  }
  return (rows || [])
    .filter((row) => String(row.analysisId) === String(analysisId))
    .slice()
    .sort((a, b) => componentOrder(a) - componentOrder(b));
}

/** dictionary rows show the coded label; everything else shows the raw value */
export function displayResult(row) {
  if (!row) {
    return "";
  }
  if (row.resultType === "D") {
    const match = (row.dictionaryResults || []).find(
      (entry) => String(entry.id) === String(row.result),
    );
    return match ? match.value : (row.result ?? "");
  }
  return row.result ?? "";
}

export function unitsOnly(units) {
  return units ? units.split(" (")[0].trim() : "";
}

export function isEditableHere(resultType) {
  return EDITABLE_RESULT_TYPES.includes(resultType);
}

/**
 * A note typed in the queue's Notes column and one typed in the panel are both
 * kept: releasing from the panel must never drop what the validator wrote in
 * the row. Identical text is not repeated.
 */
export function mergeNotes(rowNote, panelNote) {
  const parts = [rowNote, panelNote]
    .map((text) => (typeof text === "string" ? text.trim() : ""))
    .filter(Boolean);
  return parts
    .filter((part, index) => parts.indexOf(part) === index)
    .join("\n");
}

/**
 * The body of a per-row action: the row as served, plus the dual-axis note and
 * (for a modification) the new value. The server re-derives the identifiers.
 * Without an explicit visibility the server applies its legacy rule (external
 * when the row is accepted), which is what the patient report prints.
 */
export function actionPayload(
  row,
  { note, noteVisibility, noteContext, result },
) {
  const payload = {
    ...row,
    note: mergeNotes(row && row.note, note),
    noteVisibility: noteVisibility || "",
    noteContext,
  };
  if (result !== undefined) {
    payload.result = result;
  }
  return payload;
}

/** i18n key for a failed per-row action; unknown codes fall back to generic */
export function errorMessageKey(response) {
  const code = response && response.error;
  const known = KNOWN_ERRORS.includes(code) ? code : "generic";
  return `label.validation.review.error.${known}`;
}

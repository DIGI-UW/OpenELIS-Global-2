/**
 * OGC-1020 (R1) — per-row edit-state machine for the unified Results worklist.
 *
 * FR-A2/FR-A3: `EMPTY → DIRTY → SAVED(read-only) → (Edit) → EDITING → SAVED`.
 * A saved result renders read-only until Edit; an un-resulted row is editable
 * on open and shows Save once a value is entered.
 */

export type RowEditState = "EMPTY" | "DIRTY" | "SAVED" | "EDITING";

export type RowEditEvent =
  | { type: "VALUE_CHANGED" }
  | { type: "EDIT_CLICKED" }
  | { type: "SAVE_SUCCEEDED" }
  | { type: "SAVE_REJECTED_STALE" };

/** Initial state from the loaded row: saved value present ⇒ SAVED else EMPTY. */
export function initialRowState(hasSavedValue: boolean): RowEditState {
  return hasSavedValue ? "SAVED" : "EMPTY";
}

/** Fields are writable only in EMPTY/DIRTY (un-resulted) or EDITING. */
export function isRowEditable(state: RowEditState): boolean {
  return state === "EMPTY" || state === "DIRTY" || state === "EDITING";
}

/** Save is offered once there is something to save (FR-A3). */
export function showSave(state: RowEditState): boolean {
  return state === "DIRTY" || state === "EDITING";
}

/** Edit is offered only on a saved, read-only row (FR-A2). */
export function showEdit(state: RowEditState): boolean {
  return state === "SAVED";
}

export function nextRowState(
  state: RowEditState,
  event: RowEditEvent,
): RowEditState {
  switch (event.type) {
    case "VALUE_CHANGED":
      return state === "EMPTY" ? "DIRTY" : state;
    case "EDIT_CLICKED":
      return state === "SAVED" ? "EDITING" : state;
    case "SAVE_SUCCEEDED":
      return "SAVED";
    case "SAVE_REJECTED_STALE":
      // The stale editor loses (FR-O2): the row stays in its editing state so
      // the user can refresh; nothing is silently merged.
      return state;
    default:
      return state;
  }
}

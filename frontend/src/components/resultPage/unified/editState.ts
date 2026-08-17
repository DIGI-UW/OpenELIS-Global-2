/**
 * OGC-1020 (R1) — per-row edit-state machine for the unified Results worklist.
 *
 * FR-A2/FR-A3: `EMPTY → DIRTY → SAVED(read-only) → (Edit) → EDITING → SAVED`.
 * A saved result renders read-only until Edit; an un-resulted row is editable
 * on open and shows Save once a value is entered.
 *
 * Opening Edit is not itself a change, so `EDITING` waits for one before it
 * offers Save (OGC-1179). Offering Save on an untouched row invited a save that
 * wrote the reported value over the stored one and, where electronic signatures
 * are on, asked for a legally binding signature under 21 CFR Part 11 for a
 * revision the signer had not made.
 */

export type RowEditState =
  | "EMPTY"
  | "DIRTY"
  | "SAVED"
  | "EDITING"
  | "EDITING_DIRTY";

export type RowEditEvent =
  | { type: "VALUE_CHANGED" }
  | { type: "EDIT_CLICKED" }
  | { type: "SAVE_SUCCEEDED" }
  | { type: "SAVE_REJECTED_STALE" };

/** Initial state from the loaded row: saved value present ⇒ SAVED else EMPTY. */
export function initialRowState(hasSavedValue: boolean): RowEditState {
  return hasSavedValue ? "SAVED" : "EMPTY";
}

/** Fields are writable only in EMPTY/DIRTY (un-resulted) or while editing. */
export function isRowEditable(state: RowEditState): boolean {
  return (
    state === "EMPTY" ||
    state === "DIRTY" ||
    state === "EDITING" ||
    state === "EDITING_DIRTY"
  );
}

/** Save is offered once there is something to save (FR-A3). */
export function showSave(state: RowEditState): boolean {
  return state === "DIRTY" || state === "EDITING_DIRTY";
}

/** Edit is offered only on a saved, read-only row (FR-A2). */
export function showEdit(state: RowEditState): boolean {
  return state === "SAVED";
}

/**
 * Whether this row is revising a value that was already saved, as against
 * entering one for the first time.
 *
 * <p>The note context has always drawn this distinction — `Modification` rather
 * than `Entry` — and the electronic signature has to draw the same one, from
 * the same place, so a correction is not recorded as authorship.
 */
export function isModifyingSavedResult(state: RowEditState): boolean {
  return state === "EDITING" || state === "EDITING_DIRTY";
}

export function nextRowState(
  state: RowEditState,
  event: RowEditEvent,
): RowEditState {
  switch (event.type) {
    case "VALUE_CHANGED":
      if (state === "EMPTY") {
        return "DIRTY";
      }
      // A row opened for editing becomes savable at the first actual change,
      // and not before.
      return state === "EDITING" ? "EDITING_DIRTY" : state;
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

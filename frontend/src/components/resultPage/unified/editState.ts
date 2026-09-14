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
  | "EDITING_DIRTY"
  // A saved result with a referral waiting to be saved alongside it. Savable,
  // but the result itself stays locked and the save is not a revision of it.
  | "DISPOSITION_PENDING";

export type RowEditEvent =
  | { type: "VALUE_CHANGED" }
  // Something recorded about the analysis that is not its result: referring the
  // test out. Kept separate from a value change because a saved result must
  // stay read-only until Edit, and because signing this is not signing a
  // revision.
  | { type: "DISPOSITION_CHANGED" }
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
  return (
    state === "DIRTY" ||
    state === "EDITING_DIRTY" ||
    state === "DISPOSITION_PENDING"
  );
}

/**
 * Edit is offered on a saved, read-only row, including one with a referral
 * waiting: referring a test out does not stop the bench correcting its result.
 */
export function showEdit(state: RowEditState): boolean {
  return state === "SAVED" || state === "DISPOSITION_PENDING";
}

/**
 * Whether this save writes a result value at all.
 *
 * <p>A referral saved against an already-saved result does not: the value stays
 * exactly as stored. That matters twice over, because the value the row carries
 * is the one the test *reports* (95.00 for a stored 95 on a test reporting to
 * one place), so a save that means to leave the result alone has to write back
 * what was stored and not what was displayed, and the precision guard that
 * blocks a too-fine value has nothing to block.
 */
export function writesResultValue(state: RowEditState): boolean {
  return state !== "DISPOSITION_PENDING";
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
    case "DISPOSITION_CHANGED":
      // On a row still being entered, a referral is part of that entry. On one
      // already saved it stands on its own, so the result stays locked and the
      // save is not recorded as a revision of it.
      if (state === "SAVED") {
        return "DISPOSITION_PENDING";
      }
      if (state === "EMPTY") {
        return "DIRTY";
      }
      return state === "EDITING" ? "EDITING_DIRTY" : state;
    case "EDIT_CLICKED":
      if (state === "DISPOSITION_PENDING") {
        // The referral is already pending, so the row is savable either way;
        // unlocking the result makes this a revision as well.
        return "EDITING_DIRTY";
      }
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

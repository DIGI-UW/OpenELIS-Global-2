import { describe, expect, it } from "vitest";
import {
  initialRowState,
  isModifyingSavedResult,
  isRowEditable,
  nextRowState,
  showEdit,
  showSave,
  writesResultValue,
} from "./editState";

/**
 * OGC-1020 (R1) — FR-A2/FR-A3 edit-state machine:
 * EMPTY → DIRTY → SAVED(read-only) → (Edit) → EDITING → SAVED.
 */
describe("editState machine", () => {
  it("starts SAVED for a row with a stored value, EMPTY otherwise", () => {
    expect(initialRowState(true)).toBe("SAVED");
    expect(initialRowState(false)).toBe("EMPTY");
  });

  it("un-resulted row is editable on open and shows Save once a value is entered (FR-A3)", () => {
    const empty = initialRowState(false);
    expect(isRowEditable(empty)).toBe(true);
    expect(showSave(empty)).toBe(false);

    const dirty = nextRowState(empty, { type: "VALUE_CHANGED" });
    expect(dirty).toBe("DIRTY");
    expect(showSave(dirty)).toBe(true);
    expect(showEdit(dirty)).toBe(false);
  });

  it("saved row is read-only until Edit unlocks it (FR-A2)", () => {
    const saved = initialRowState(true);
    expect(isRowEditable(saved)).toBe(false);
    expect(showEdit(saved)).toBe(true);
    expect(showSave(saved)).toBe(false);

    const editing = nextRowState(saved, { type: "EDIT_CLICKED" });
    expect(editing).toBe("EDITING");
    expect(isRowEditable(editing)).toBe(true);
    // Opening Edit is not a change, so there is nothing to save yet.
    expect(showSave(editing)).toBe(false);
  });

  /**
   * OGC-1179 #1/#2 — Save was offered the moment Edit opened, so pressing it
   * without touching anything wrote the reported value over the stored one
   * (a numeric result on a test reporting to no decimal places was stored as
   * 23.7 and reported as "23") and, with electronic signatures on, demanded a
   * legally binding Part 11 signature for a revision the signer had not made.
   */
  it("an untouched row opened for Edit offers no Save (OGC-1179)", () => {
    const editing = nextRowState(initialRowState(true), {
      type: "EDIT_CLICKED",
    });

    expect(showSave(editing)).toBe(false);

    const changed = nextRowState(editing, { type: "VALUE_CHANGED" });
    expect(changed).toBe("EDITING_DIRTY");
    expect(showSave(changed)).toBe(true);
    expect(isRowEditable(changed)).toBe(true);
    expect(showEdit(changed)).toBe(false);
  });

  it("a further change to an already-dirty editor stays savable", () => {
    expect(nextRowState("EDITING_DIRTY", { type: "VALUE_CHANGED" })).toBe(
      "EDITING_DIRTY",
    );
    expect(nextRowState("DIRTY", { type: "VALUE_CHANGED" })).toBe("DIRTY");
  });

  /**
   * The signature's meaning and the note's context are the same question —
   * is this a first entry or a revision — and must be answered the same way.
   */
  it("knows a revision from a first entry", () => {
    expect(isModifyingSavedResult("EDITING")).toBe(true);
    expect(isModifyingSavedResult("EDITING_DIRTY")).toBe(true);
    expect(isModifyingSavedResult("EMPTY")).toBe(false);
    expect(isModifyingSavedResult("DIRTY")).toBe(false);
    expect(isModifyingSavedResult("SAVED")).toBe(false);
  });

  /**
   * Referring a test out is not a revision of its result. A confirmation
   * referral is raised precisely when an in-house result already exists, and
   * that row is SAVED, where a value change is deliberately ignored — so Save
   * never appeared and the referral could not be raised at all. Clicking Edit
   * first worked, but recorded the save as a result modification and, under
   * electronic signatures, asked for a binding signature on a revision nobody
   * had made.
   */
  describe("a referral on a row whose result is already saved", () => {
    const pending = nextRowState(initialRowState(true), {
      type: "DISPOSITION_CHANGED",
    });

    it("offers Save without unlocking the result", () => {
      expect(pending).toBe("DISPOSITION_PENDING");
      expect(showSave(pending)).toBe(true);
      expect(isRowEditable(pending)).toBe(false);
    });

    it("is not a revision of the result", () => {
      expect(isModifyingSavedResult(pending)).toBe(false);
    });

    it("still lets the result be edited as well", () => {
      expect(showEdit(pending)).toBe(true);
      expect(nextRowState(pending, { type: "EDIT_CLICKED" })).toBe(
        "EDITING_DIRTY",
      );
    });

    it("writes back the stored value, not the reported one", () => {
      expect(writesResultValue(pending)).toBe(false);
      expect(writesResultValue("EDITING_DIRTY")).toBe(true);
      expect(writesResultValue("DIRTY")).toBe(true);
    });

    it("relocks on save", () => {
      expect(nextRowState(pending, { type: "SAVE_SUCCEEDED" })).toBe("SAVED");
    });

    it("leaves the untouched-row rule alone", () => {
      // A value change on a saved row is still ignored: only Edit unlocks it.
      expect(nextRowState("SAVED", { type: "VALUE_CHANGED" })).toBe("SAVED");
      // And a disposition on a row being entered for the first time is simply
      // part of that entry.
      expect(nextRowState("EMPTY", { type: "DISPOSITION_CHANGED" })).toBe(
        "DIRTY",
      );
      expect(nextRowState("EDITING", { type: "DISPOSITION_CHANGED" })).toBe(
        "EDITING_DIRTY",
      );
    });
  });

  it("save relocks the row read-only", () => {
    expect(nextRowState("DIRTY", { type: "SAVE_SUCCEEDED" })).toBe("SAVED");
    expect(nextRowState("EDITING", { type: "SAVE_SUCCEEDED" })).toBe("SAVED");
    expect(isRowEditable("SAVED")).toBe(false);
  });

  it("stale rejection keeps the editor's state — nothing silently merged (FR-O2)", () => {
    expect(nextRowState("EDITING", { type: "SAVE_REJECTED_STALE" })).toBe(
      "EDITING",
    );
    expect(nextRowState("DIRTY", { type: "SAVE_REJECTED_STALE" })).toBe(
      "DIRTY",
    );
  });

  it("Edit only applies to a SAVED row", () => {
    expect(nextRowState("EMPTY", { type: "EDIT_CLICKED" })).toBe("EMPTY");
    expect(nextRowState("DIRTY", { type: "EDIT_CLICKED" })).toBe("DIRTY");
  });
});

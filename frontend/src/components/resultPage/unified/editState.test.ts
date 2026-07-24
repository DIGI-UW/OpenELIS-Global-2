import { describe, expect, it } from "vitest";
import {
  initialRowState,
  isRowEditable,
  nextRowState,
  showEdit,
  showSave,
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
    expect(showSave(editing)).toBe(true);
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

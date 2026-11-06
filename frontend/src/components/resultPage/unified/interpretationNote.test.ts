import { mergeInterpretationNote } from "./interpretationNote";

/**
 * #4899 — the interpretation saved on a row must stay visible after the
 * save, which means it has to land in that row's analysisNotes before the
 * draft is deleted. The merge replaces, never duplicates.
 */
describe("mergeInterpretationNote", () => {
  it("appends one Interpretation note when the row has none", () => {
    const merged = mergeInterpretationNote(
      [],
      "Suggestive of iron deficiency",
      "comp-A",
    );
    expect(merged).toEqual([
      {
        subject: "Interpretation",
        text: "Suggestive of iron deficiency",
        testResultComponentId: "comp-A",
      },
    ]);
  });

  it("appends an analysis-level note when the row has no component", () => {
    const merged = mergeInterpretationNote(
      undefined,
      "Within normal limits",
      undefined,
    );
    expect(merged).toEqual([
      { subject: "Interpretation", text: "Within normal limits" },
    ]);
  });

  it("replaces the Interpretation note for the same component, not a duplicate", () => {
    const existing = [
      { subject: "Note", text: "keep me" },
      {
        subject: "Interpretation",
        text: "old text",
        testResultComponentId: "comp-A",
      },
    ];
    const merged = mergeInterpretationNote(existing, "new text", "comp-A");
    expect(merged).toHaveLength(2);
    expect(merged[1]).toEqual({
      subject: "Interpretation",
      text: "new text",
      testResultComponentId: "comp-A",
    });
    expect(merged[0]).toEqual({ subject: "Note", text: "keep me" });
  });

  it("replaces the visible Interpretation note, the one the field shows", () => {
    const existing = [
      {
        subject: "Interpretation",
        text: "stale",
        testResultComponentId: "comp-A",
      },
      {
        subject: "Interpretation",
        text: "other component",
        testResultComponentId: "comp-B",
      },
    ];
    const merged = mergeInterpretationNote(existing, "fresh", "comp-A");
    expect(merged).toHaveLength(2);
    expect(merged[0].text).toBe("fresh");
    expect(merged[1]).toEqual({
      subject: "Interpretation",
      text: "other component",
      testResultComponentId: "comp-B",
    });
  });

  it("leaves another component's Interpretation note alone", () => {
    const other = {
      subject: "Interpretation",
      text: "comp-B text",
      testResultComponentId: "comp-B",
    };
    const merged = mergeInterpretationNote([other], "comp-A text", "comp-A");
    expect(merged).toHaveLength(2);
    expect(merged[0]).toEqual(other);
    expect(merged[1]).toEqual({
      subject: "Interpretation",
      text: "comp-A text",
      testResultComponentId: "comp-A",
    });
  });

  it("leaves the notes unchanged for a blank interpretation", () => {
    const existing = [
      {
        subject: "Interpretation",
        text: "already saved",
        testResultComponentId: "comp-A",
      },
    ];
    expect(mergeInterpretationNote(existing, "   ", "comp-A")).toEqual(
      existing,
    );
    expect(mergeInterpretationNote(existing, undefined, "comp-A")).toEqual(
      existing,
    );
  });
});

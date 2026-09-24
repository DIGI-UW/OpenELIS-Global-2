import { noteVisibleOnRow } from "./noteScope";

/**
 * OGC-811 — notes are component-specific on component rows, with graceful
 * legacy fallback: analysis-level notes (no component id) show everywhere.
 */
describe("noteVisibleOnRow", () => {
  const legacy = { text: "analysis-wide note" };
  const compA = { text: "A only", testResultComponentId: "comp-A" };
  const compB = { text: "B only", testResultComponentId: "comp-B" };

  it("component rows see their own notes plus analysis-level ones", () => {
    expect(noteVisibleOnRow(compA, "comp-A")).toBe(true);
    expect(noteVisibleOnRow(legacy, "comp-A")).toBe(true);
  });

  it("component rows never see another component's notes (A must not show B's)", () => {
    expect(noteVisibleOnRow(compB, "comp-A")).toBe(false);
    expect(noteVisibleOnRow(compA, "comp-B")).toBe(false);
  });

  it("rows without a component (single-component tests, legacy) see everything", () => {
    expect(noteVisibleOnRow(legacy, undefined)).toBe(true);
    expect(noteVisibleOnRow(compA, undefined)).toBe(true);
  });

  it("interpretation notes follow the same scoping", () => {
    const interpA = {
      text: "Suggestive of iron deficiency",
      subject: "Interpretation",
      testResultComponentId: "comp-A",
    };
    expect(noteVisibleOnRow(interpA, "comp-A")).toBe(true);
    expect(noteVisibleOnRow(interpA, "comp-B")).toBe(false);
  });
});

/**
 * OGC-1028 (Validation v4 slice V2) — the pure review-panel rules: flag
 * styling, multi-component ordering, dictionary display, the action payload
 * carrying the dual-axis note, and error-code mapping.
 */
import {
  NOTE_CONTEXT_MODIFICATION,
  NOTE_CONTEXT_VALIDATION,
  NOTE_EXTERNAL,
  NOTE_INTERNAL,
  actionPayload,
  componentRowsFor,
  displayResult,
  errorMessageKey,
  flagFor,
  isEditableHere,
  mergeNotes,
  unitsOnly,
} from "./validationReview";

const row = (overrides = {}) => ({
  id: 0,
  analysisId: "100",
  normalRange: "10 - 20",
  normal: true,
  result: "15",
  resultType: "N",
  critical: false,
  ...overrides,
});

describe("flagFor", () => {
  it("critical wins over the range verdict", () => {
    expect(flagFor(row({ critical: true, normal: false }))).toBe("CRITICAL");
  });

  it("abnormal / normal follow the range verdict when a range is known", () => {
    expect(flagFor(row({ normal: false }))).toBe("ABNORMAL");
    expect(flagFor(row({ normal: true }))).toBe("NORMAL");
  });

  it("no range means no flag — never a false Normal", () => {
    expect(flagFor(row({ normalRange: "", normal: true }))).toBeUndefined();
    expect(flagFor(row({ normalRange: null, normal: false }))).toBeUndefined();
    expect(flagFor(undefined)).toBeUndefined();
  });
});

describe("componentRowsFor", () => {
  it("returns the analysis's rows, primary first then by display order", () => {
    const rows = [
      row({ id: 3, analysisId: "200" }),
      row({ id: 1, componentLabel: "Ct E", componentDisplayOrder: 2 }),
      row({ id: 0 }),
      row({ id: 2, componentLabel: "Ct N2", componentDisplayOrder: 1 }),
    ];
    expect(componentRowsFor(rows, "100").map((r) => r.id)).toEqual([0, 2, 1]);
  });

  it("is empty without an analysis id or rows", () => {
    expect(componentRowsFor([row()], undefined)).toEqual([]);
    expect(componentRowsFor(undefined, "100")).toEqual([]);
  });
});

describe("displayResult / unitsOnly / isEditableHere", () => {
  it("shows the coded label for dictionary results and the raw value otherwise", () => {
    expect(
      displayResult(
        row({
          resultType: "D",
          result: "7",
          dictionaryResults: [{ id: 7, value: "Positive" }],
        }),
      ),
    ).toBe("Positive");
    expect(displayResult(row({ resultType: "D", result: "9" }))).toBe("9");
    expect(displayResult(row({ result: "15" }))).toBe("15");
    expect(displayResult(row({ result: null }))).toBe("");
  });

  it("strips the parenthesised suffix from units", () => {
    expect(unitsOnly("mg/dL (mass)")).toBe("mg/dL");
    expect(unitsOnly(undefined)).toBe("");
  });

  it("only single-value result types are edited inline", () => {
    expect(isEditableHere("N")).toBe(true);
    expect(isEditableHere("D")).toBe(true);
    expect(isEditableHere("M")).toBe(false);
    expect(isEditableHere("C")).toBe(false);
  });
});

describe("mergeNotes", () => {
  it("keeps the queue-column note and the panel note, in that order", () => {
    expect(mergeNotes("From the row", "From the panel")).toBe(
      "From the row\nFrom the panel",
    );
  });

  it("uses whichever one exists and never repeats identical text", () => {
    expect(mergeNotes("Only row", "")).toBe("Only row");
    expect(mergeNotes(undefined, "  Only panel ")).toBe("Only panel");
    expect(mergeNotes("Same", "Same")).toBe("Same");
    expect(mergeNotes(null, undefined)).toBe("");
  });
});

describe("actionPayload", () => {
  it("carries the row plus the dual-axis note; no explicit visibility leaves the server's legacy rule", () => {
    const payload = actionPayload(row(), {
      note: "ok",
      noteContext: NOTE_CONTEXT_VALIDATION,
    });
    expect(payload.analysisId).toBe("100");
    expect(payload.note).toBe("ok");
    expect(payload.noteVisibility).toBe("");
    expect(payload.noteContext).toBe("VALIDATION");
    expect(payload).not.toHaveProperty("result", undefined);
    expect(payload.result).toBe("15");
  });

  it("a note typed in the row's Notes column survives a release from the panel", () => {
    const payload = actionPayload(row({ note: "Typed in the queue" }), {
      note: "",
      noteVisibility: NOTE_EXTERNAL,
      noteContext: NOTE_CONTEXT_VALIDATION,
    });
    expect(payload.note).toBe("Typed in the queue");
    const both = actionPayload(row({ note: "Typed in the queue" }), {
      note: "And in the panel",
      noteVisibility: NOTE_EXTERNAL,
      noteContext: NOTE_CONTEXT_VALIDATION,
    });
    expect(both.note).toBe("Typed in the queue\nAnd in the panel");
  });

  it("a modification replaces the value and keeps the chosen visibility", () => {
    const payload = actionPayload(row(), {
      note: "typo",
      noteVisibility: NOTE_EXTERNAL,
      noteContext: NOTE_CONTEXT_MODIFICATION,
      result: "16",
    });
    expect(payload.result).toBe("16");
    expect(payload.noteVisibility).toBe("E");
    expect(payload.noteContext).toBe("MODIFICATION");
  });
});

describe("errorMessageKey", () => {
  it("maps known server codes and falls back to generic", () => {
    expect(errorMessageKey({ error: "notAwaitingValidation" })).toBe(
      "label.validation.review.error.notAwaitingValidation",
    );
    expect(errorMessageKey({ error: "qcAcknowledgmentRequired" })).toBe(
      "label.validation.review.error.qcAcknowledgmentRequired",
    );
    expect(errorMessageKey({ error: "somethingElse" })).toBe(
      "label.validation.review.error.generic",
    );
    expect(errorMessageKey(undefined)).toBe(
      "label.validation.review.error.generic",
    );
  });
});

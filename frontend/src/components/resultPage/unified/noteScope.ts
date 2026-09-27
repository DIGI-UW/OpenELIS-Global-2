/**
 * OGC-811 — component scoping for analysis notes.
 *
 * A note row carries an optional test_result_component_id: null/absent means
 * analysis-level (every legacy note, and notes authored outside a component
 * context); set means the note was authored from that component's row.
 */
export interface AnalysisNote {
  text?: string;
  noteType?: string;
  subject?: string;
  author?: string;
  date?: string;
  /** null/absent = analysis-level (legacy); set = scoped to one component. */
  testResultComponentId?: string;
}

/**
 * Component scoping with graceful legacy fallback: a note with no component id
 * is analysis-level and shows on every row; a scoped note shows only on its
 * component's row. Rows without a component id (single-component tests) show
 * everything, preserving the historic analysis-level behavior.
 */
export const noteVisibleOnRow = (
  note: AnalysisNote,
  rowComponentId?: string,
): boolean =>
  !note.testResultComponentId ||
  !rowComponentId ||
  note.testResultComponentId === rowComponentId;

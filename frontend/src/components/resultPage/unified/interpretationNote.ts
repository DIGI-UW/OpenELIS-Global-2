import { AnalysisNote, noteVisibleOnRow } from "./noteScope";

/**
 * #4899 — merge a just-saved interpretation into the worklist row's
 * analysisNotes, so the Interpretation field keeps showing it after save.
 *
 * The save response carries no notes (see SaveResponse in
 * UnifiedResults.tsx), and the save callback deletes the interpretation
 * draft, so without this merge the field falls back to blank until the
 * next reload re-reads analysisNotes from the server.
 *
 * Replace, never append a duplicate: if the row already has an
 * Interpretation note visible on it, the saved text replaces that note's
 * text (its scoping is preserved); otherwise one note is appended,
 * scoped to this row's component when the row has one.
 */
export const mergeInterpretationNote = (
  notes: AnalysisNote[] | undefined,
  interpretation: string | undefined,
  componentId?: string,
): AnalysisNote[] => {
  const text = interpretation?.trim();
  if (!text) {
    return notes ?? [];
  }
  const current = notes ?? [];
  let replaceIndex = -1;
  for (let i = current.length - 1; i >= 0; i -= 1) {
    const note = current[i];
    if (
      note.subject === "Interpretation" &&
      noteVisibleOnRow(note, componentId)
    ) {
      replaceIndex = i;
      break;
    }
  }
  if (replaceIndex === -1) {
    const appended: AnalysisNote = {
      subject: "Interpretation",
      text,
      ...(componentId ? { testResultComponentId: componentId } : {}),
    };
    return [...current, appended];
  }
  return current.map((note, index) =>
    index === replaceIndex ? { ...note, text } : note,
  );
};

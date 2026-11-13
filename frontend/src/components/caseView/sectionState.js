/**
 * Pure derivation of one case-view section's state.
 *
 * A section's state is always computed fresh from the case status, the stage
 * order, the caller's role and the section's own prerequisites — never
 * stored and never cached across a render. The status enum is the one fact
 * that says where a case stands; a second, independently stored flag (for
 * example a "ready for review" bit sitting beside the status) would be a
 * second representation of that same fact, and two stored representations of
 * one fact cannot be kept in step with each other. Deriving state from the
 * status alone removes the possibility of that drift, because there is
 * nowhere for a second copy to live.
 *
 * No React, no I/O: everything here is a plain function of its arguments, so
 * every rule is testable without mounting anything.
 */

export const SECTION_STATE = Object.freeze({
  OPEN: "OPEN",
  COMPLETE: "COMPLETE",
  DISABLED: "DISABLED",
  READ_ONLY: "READ_ONLY",
});

// There is deliberately no HIDDEN state. A section is never removed from the
// DOM on the basis of status or role, so the model has no vocabulary for
// hiding one — only for describing why it cannot be worked on yet.

const DEFAULT_AWAITING_STAGE_KEY = "caseView.locked.awaitingStage";
const DEFAULT_PREREQUISITE_KEY = "caseView.locked.prerequisite";

/**
 * Decide what one section should show, given the case's current status and
 * the section's own gating stage.
 *
 * The rules below are applied strictly in order: each one either returns a
 * result or falls through to the next. Reordering them changes behaviour, so
 * the order is part of the contract, not an implementation detail.
 */
export function deriveSectionState({
  status,
  stageOrder = [],
  sectionStage = null,
  stageLabel = null,
  roleCanEdit = true,
  prerequisitesMet = true,
  readOnly = false,
  lockedHintKey,
} = {}) {
  // A section with no gating stage is not part of the bench sequence at all
  // (a display-only summary, for example), so status and stage order never
  // apply to it. It is only ever open or read-only.
  if (sectionStage == null) {
    return withNoHint(
      readOnly || !roleCanEdit ? SECTION_STATE.READ_ONLY : SECTION_STATE.OPEN,
    );
  }

  const caseIndex = stageOrder.indexOf(status);
  const sectionIndex = stageOrder.indexOf(sectionStage);

  // Fail-safe: a status or a section stage this build cannot place in the
  // order must never be read as "ahead" or "behind", because either reading
  // could unlock bench work the running code does not actually understand
  // yet. The only safe answer to "where does this fall in a sequence I
  // cannot find it in" is locked. This is what protects an older browser
  // talking to a server that has learned a status the browser has not.
  if (sectionIndex === -1 || caseIndex === -1) {
    return disabledWithHint(
      lockedHintKey ?? DEFAULT_AWAITING_STAGE_KEY,
      // sectionStage is a SCREAMING_SNAKE status constant, not something a
      // bench user should ever read. stageLabel is the caller's already
      // localized display text for that stage; falling back to the raw
      // constant here is a last resort for a caller that forgot to supply
      // one, not the intended display.
      { stage: stageLabel ?? sectionStage },
    );
  }

  // The case has not reached this section's stage yet.
  if (sectionIndex > caseIndex) {
    return disabledWithHint(lockedHintKey ?? DEFAULT_AWAITING_STAGE_KEY, {
      stage: stageLabel ?? sectionStage,
    });
  }

  // A prerequisite (something other than stage order) is not satisfied,
  // for example a required upstream field is still empty.
  if (!prerequisitesMet) {
    return disabledWithHint(lockedHintKey ?? DEFAULT_PREREQUISITE_KEY);
  }

  if (readOnly || !roleCanEdit) {
    return withNoHint(SECTION_STATE.READ_ONLY);
  }

  // The case has moved past this section's stage. Completeness is a
  // presentation detail, not a lock: the section stays interactive so its
  // content can still be revised.
  if (sectionIndex < caseIndex) {
    return withNoHint(SECTION_STATE.COMPLETE);
  }

  return withNoHint(SECTION_STATE.OPEN);
}

function withNoHint(state) {
  return { state, lockedHintKey: undefined, lockedHintValues: undefined };
}

function disabledWithHint(lockedHintKey, lockedHintValues) {
  return { state: SECTION_STATE.DISABLED, lockedHintKey, lockedHintValues };
}

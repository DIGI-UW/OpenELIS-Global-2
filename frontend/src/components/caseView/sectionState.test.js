import { SECTION_STATE, deriveSectionState } from "./sectionState";

const STAGE_ORDER = [
  "GROSSING",
  "PROCESSING",
  "EMBEDDING",
  "STAINING",
  "COMPLETED",
];

describe("deriveSectionState", () => {
  it("is open when the case sits exactly at the section's own stage", () => {
    const result = deriveSectionState({
      status: "EMBEDDING",
      stageOrder: STAGE_ORDER,
      sectionStage: "EMBEDDING",
    });

    expect(result.state).toBe(SECTION_STATE.OPEN);
  });

  it("is complete, not disabled, once the case has moved past the section's stage", () => {
    const result = deriveSectionState({
      status: "STAINING",
      stageOrder: STAGE_ORDER,
      sectionStage: "GROSSING",
    });

    expect(result.state).toBe(SECTION_STATE.COMPLETE);
  });

  it("is read-only when the case is open to the section but the caller cannot edit", () => {
    const result = deriveSectionState({
      status: "EMBEDDING",
      stageOrder: STAGE_ORDER,
      sectionStage: "EMBEDDING",
      roleCanEdit: false,
    });

    expect(result.state).toBe(SECTION_STATE.READ_ONLY);
  });

  it("is read-only when the screen itself is opened read-only, regardless of role", () => {
    const result = deriveSectionState({
      status: "EMBEDDING",
      stageOrder: STAGE_ORDER,
      sectionStage: "EMBEDDING",
      roleCanEdit: true,
      readOnly: true,
    });

    expect(result.state).toBe(SECTION_STATE.READ_ONLY);
  });

  it("locks a section the case has not reached yet, and names the stage that unlocks it", () => {
    const result = deriveSectionState({
      status: "GROSSING",
      stageOrder: STAGE_ORDER,
      sectionStage: "STAINING",
    });

    expect(result.state).toBe(SECTION_STATE.DISABLED);
    expect(result.lockedHintKey).toBe("caseView.locked.awaitingStage");
    expect(result.lockedHintValues).toEqual({ stage: "STAINING" });
  });

  it("names the unlock stage using the caller's localized label, not the raw status constant", () => {
    const result = deriveSectionState({
      status: "GROSSING",
      stageOrder: STAGE_ORDER,
      sectionStage: "READY_PATHOLOGIST",
      stageLabel: "Ready for Pathologist",
    });

    expect(result.state).toBe(SECTION_STATE.DISABLED);
    expect(result.lockedHintValues).toEqual({
      stage: "Ready for Pathologist",
    });
  });

  // Regression test: before stageLabel existed, the hint values fell back to
  // the raw SCREAMING_SNAKE status constant, so a bench user would read
  // something like "Available once the case reaches READY_PATHOLOGIST".
  it("never leaves an underscore in the unlock stage when the caller supplies a label", () => {
    const result = deriveSectionState({
      status: "GROSSING",
      stageOrder: STAGE_ORDER,
      sectionStage: "READY_PATHOLOGIST",
      stageLabel: "Ready for Pathologist",
    });

    expect(result.lockedHintValues.stage).not.toMatch(/_/);
  });

  it("lets the caller's own locked-hint key win over the awaiting-stage default", () => {
    const result = deriveSectionState({
      status: "GROSSING",
      stageOrder: STAGE_ORDER,
      sectionStage: "STAINING",
      lockedHintKey: "pathology.locked.awaitingGrossing",
    });

    expect(result.lockedHintKey).toBe("pathology.locked.awaitingGrossing");
  });

  it("falls back to the prerequisite key when a prerequisite is unmet and the caller gave no key of its own", () => {
    const result = deriveSectionState({
      status: "STAINING",
      stageOrder: STAGE_ORDER,
      sectionStage: "STAINING",
      prerequisitesMet: false,
    });

    expect(result.state).toBe(SECTION_STATE.DISABLED);
    expect(result.lockedHintKey).toBe("caseView.locked.prerequisite");
  });

  it("lets the caller's own locked-hint key win over the prerequisite default too", () => {
    const result = deriveSectionState({
      status: "STAINING",
      stageOrder: STAGE_ORDER,
      sectionStage: "STAINING",
      prerequisitesMet: false,
      lockedHintKey: "pathology.locked.awaitingBlock",
    });

    expect(result.lockedHintKey).toBe("pathology.locked.awaitingBlock");
  });

  // A status (or a section stage) this build cannot find in its own stage
  // order must never be read as "ahead of" or "behind" the case, because
  // either reading could unlock bench work the running code does not
  // actually understand. An unrecognised status typically means a server
  // has learned a stage an older browser has not, so the only safe answer
  // is locked, exactly like a section that is genuinely ahead of the case.
  it("locks every stage-gated section when the case status is not in the stage order at all", () => {
    const result = deriveSectionState({
      status: "SOME_FUTURE_STATUS_THIS_BUILD_DOES_NOT_KNOW",
      stageOrder: STAGE_ORDER,
      sectionStage: "GROSSING",
    });

    expect(result.state).toBe(SECTION_STATE.DISABLED);
    expect(result.lockedHintKey).toBe("caseView.locked.awaitingStage");
  });

  it("locks a section whose own gating stage is not in the stage order", () => {
    const result = deriveSectionState({
      status: "GROSSING",
      stageOrder: STAGE_ORDER,
      sectionStage: "SOME_STAGE_NOT_IN_THE_ORDER",
    });

    expect(result.state).toBe(SECTION_STATE.DISABLED);
  });

  it("is open when the section is not gated on any stage", () => {
    const result = deriveSectionState({
      status: "GROSSING",
      stageOrder: STAGE_ORDER,
      sectionStage: null,
    });

    expect(result.state).toBe(SECTION_STATE.OPEN);
  });

  it("is read-only, not open, for a stage-less section when the caller cannot edit", () => {
    const result = deriveSectionState({
      status: "GROSSING",
      stageOrder: STAGE_ORDER,
      sectionStage: null,
      roleCanEdit: false,
    });

    expect(result.state).toBe(SECTION_STATE.READ_ONLY);
  });

  it("carries no locked hint for a stage-less section, even if the caller passed one", () => {
    const result = deriveSectionState({
      status: "GROSSING",
      stageOrder: STAGE_ORDER,
      sectionStage: null,
      lockedHintKey: "caseView.locked.awaitingStage",
    });

    expect(result.state).toBe(SECTION_STATE.OPEN);
    expect(result.lockedHintKey).toBeUndefined();
    expect(result.lockedHintValues).toBeUndefined();
  });

  // Inversion test: a user who is not permitted to edit still sees the same
  // section a permitted user sees, just not editably. Flipping only
  // roleCanEdit on an otherwise-open case must never fall through to
  // DISABLED — a locked section means "not yet", a read-only one means
  // "not you", and the two must not collapse into each other.
  it("turns an open section read-only, and specifically not disabled, when only roleCanEdit is flipped off", () => {
    const openInputs = {
      status: "EMBEDDING",
      stageOrder: STAGE_ORDER,
      sectionStage: "EMBEDDING",
    };

    const openResult = deriveSectionState(openInputs);
    expect(openResult.state).toBe(SECTION_STATE.OPEN);

    const readOnlyResult = deriveSectionState({
      ...openInputs,
      roleCanEdit: false,
    });

    expect(readOnlyResult.state).toBe(SECTION_STATE.READ_ONLY);
    expect(readOnlyResult.state).not.toBe(SECTION_STATE.DISABLED);
  });

  it("has exactly the four states, with no hidden state among them", () => {
    expect(Object.keys(SECTION_STATE).sort()).toEqual(
      ["COMPLETE", "DISABLED", "OPEN", "READ_ONLY"].sort(),
    );
    expect(Object.values(SECTION_STATE)).not.toContain("HIDDEN");
  });

  it("leaves the locked-hint fields undefined whenever the state is not disabled", () => {
    const open = deriveSectionState({
      status: "EMBEDDING",
      stageOrder: STAGE_ORDER,
      sectionStage: "EMBEDDING",
    });
    const complete = deriveSectionState({
      status: "STAINING",
      stageOrder: STAGE_ORDER,
      sectionStage: "GROSSING",
    });
    const readOnly = deriveSectionState({
      status: "EMBEDDING",
      stageOrder: STAGE_ORDER,
      sectionStage: "EMBEDDING",
      roleCanEdit: false,
    });

    for (const result of [open, complete, readOnly]) {
      expect(result.lockedHintKey).toBeUndefined();
      expect(result.lockedHintValues).toBeUndefined();
    }
  });
});

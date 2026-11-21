import { createIntl } from "react-intl";
import messages from "../../languages/en.json";
import { PATHOLOGY_STAGES, stageDisplayKey } from "./pathologyStages";
import { SECTION_STATE } from "../caseView/sectionState";
import {
  coverslipSummary,
  embeddingSummary,
  stainingSummary,
} from "./pathologyRows";
import {
  PATHOLOGY_SECTIONS,
  grossingComplete,
  stageEnablementProperty,
  isStageEnabled,
  stageBadgeKind,
  sectionBadge,
  openRequestNames,
  openRequests,
  openRequestsLabel,
  requestActionLock,
  requestDraftBlockedReason,
  requestDraftBody,
  requestStatusBody,
  REVIEW_SECTION_ID,
  reviewTakesRequests,
  deriveCaseSections,
  railCurrentIndex,
  deriveRailItems,
  outstandingLabel,
  embeddingStatement,
  stageApplies,
  decalcificationFlagFrozen,
  advanceLabelKey,
  addSlideLock,
  advanceBlockedReason,
  returnBlockedReason,
  rowActionLock,
  scanVerificationRequired,
  switchedOn,
  verifyLabelLock,
  sectionIdOfStage,
  sectionHoldsRecords,
  batchHold,
  redoNotice,
  slideWorkLock,
  slideWorkReading,
  slideWorkStatement,
  slideWorkSummaries,
  COVERSLIPPING_SECTION_ID,
  STAINING_SECTION_ID,
  grossExamPlace,
  slidesReceived,
  neverCheckedSlides,
} from "./pathologySections";

const intl = createIntl({ locale: "en", messages });

const byId = (sections, id) => sections.find((section) => section.id === id);

describe("PATHOLOGY_SECTIONS", () => {
  it("numbers the eleven sections in bench order with unique ids, each built only from real bench stages", () => {
    expect(PATHOLOGY_SECTIONS).toHaveLength(11);
    expect(PATHOLOGY_SECTIONS.map((section) => section.number)).toEqual([
      1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
    ]);

    const ids = PATHOLOGY_SECTIONS.map((section) => section.id);
    expect(new Set(ids).size).toBe(ids.length);

    PATHOLOGY_SECTIONS.forEach((section) => {
      section.stages.forEach((stage) => {
        expect(PATHOLOGY_STAGES).toContain(stage);
      });
    });
  });
});

describe("isStageEnabled", () => {
  it("disables an optional stage whose configuration property is the string false", () => {
    expect(
      isStageEnabled("COVERSLIPPING", {
        PATHOLOGY_STAGE_COVERSLIPPING_ENABLED: "false",
      }),
    ).toBe(false);
  });

  it("enables an optional stage whose configuration property is the string true", () => {
    expect(
      isStageEnabled("COVERSLIPPING", {
        PATHOLOGY_STAGE_COVERSLIPPING_ENABLED: "true",
      }),
    ).toBe(true);
  });

  // The server trims the stored value and ignores its case when it picks the
  // next and previous stage, so the sections read it the same way.
  it.each([
    ["false", false],
    ["False", false],
    ["FALSE", false],
    [" FALSE ", false],
    ["\tfalse\n", false],
    ["true", true],
    [" True ", true],
    ["", true],
    ["no", true],
    ["falsey", true],
  ])("reads a switch stored as %j as enabled: %s", (value, enabled) => {
    expect(
      isStageEnabled("COVERSLIPPING", {
        PATHOLOGY_STAGE_COVERSLIPPING_ENABLED: value,
      }),
    ).toBe(enabled);
  });

  it("enables an optional stage the served configuration says nothing about", () => {
    expect(isStageEnabled("COVERSLIPPING", {})).toBe(true);
  });

  it("enables an optional stage when no configuration has been served to the browser at all", () => {
    expect(isStageEnabled("COVERSLIPPING", undefined)).toBe(true);
  });

  // The server serves a switch only for the seven stages a laboratory may
  // stop tracking, so a stage with nothing served for it is enabled by the
  // same rule as any other, not by a second list kept here.
  it("enables a stage the served configuration carries no switch for", () => {
    expect(
      isStageEnabled("GROSSING", {
        PATHOLOGY_STAGE_COVERSLIPPING_ENABLED: "false",
      }),
    ).toBe(true);
  });
});

describe("switchedOn", () => {
  // One reading for every served switch: only an explicit false turns it off.
  it.each([
    [undefined, true],
    [null, true],
    ["", true],
    ["true", true],
    ["no", true],
    ["false", false],
    [" FALSE ", false],
    ["False", false],
  ])("reads %j as on: %s", (value, expected) => {
    expect(switchedOn(value)).toBe(expected);
  });
});

describe("scanVerificationRequired", () => {
  // Scanning is the default, as the server's; only an explicit false, read
  // as the stage switches are read, turns it into the typed confirmation.
  it.each([
    [undefined, true],
    [{}, true],
    [{ PATHOLOGY_SCAN_VERIFICATION_REQUIRED: "true" }, true],
    [{ PATHOLOGY_SCAN_VERIFICATION_REQUIRED: "" }, true],
    [{ PATHOLOGY_SCAN_VERIFICATION_REQUIRED: "no" }, true],
    [{ PATHOLOGY_SCAN_VERIFICATION_REQUIRED: "false" }, false],
    [{ PATHOLOGY_SCAN_VERIFICATION_REQUIRED: " FALSE " }, false],
    [{ PATHOLOGY_SCAN_VERIFICATION_REQUIRED: "False" }, false],
  ])("reads %j as scanning: %s", (configurationProperties, expected) => {
    expect(scanVerificationRequired(configurationProperties)).toBe(expected);
  });
});

describe("stageEnablementProperty", () => {
  it("names the exact configuration key a stage is switched off with", () => {
    expect(stageEnablementProperty("COVERSLIPPING")).toBe(
      "PATHOLOGY_STAGE_COVERSLIPPING_ENABLED",
    );
  });
});

describe("deriveCaseSections at GROSSING for a technician", () => {
  // Flagged for decalcification, so every section ahead is locked by the
  // stage order alone.
  const sections = deriveCaseSections({
    intl,
    status: "GROSSING",
    isPathologist: false,
    decalcificationRequired: true,
  });

  it("shows Case Information read-only and opens the grossing and reports sections", () => {
    expect(byId(sections, "pathology-section-case-info").state).toBe(
      SECTION_STATE.READ_ONLY,
    );
    expect(byId(sections, "pathology-section-grossing").state).toBe(
      SECTION_STATE.OPEN,
    );
    expect(byId(sections, "pathology-section-reports").state).toBe(
      SECTION_STATE.OPEN,
    );
  });

  it("locks every section ahead of grossing, naming the unlocking stage in the bench's own language", () => {
    const lockedLeafKeys = {
      "pathology-section-decalcification": "pathology.stage.decalcification",
      "pathology-section-processing": "pathology.stage.processing",
      "pathology-section-embedding": "pathology.stage.embedding",
      "pathology-section-microtomy": "pathology.stage.microtomy",
      "pathology-section-staining": "pathology.stage.staining",
      "pathology-section-coverslipping": "pathology.stage.coverslipping",
    };

    Object.entries(lockedLeafKeys).forEach(([id, key]) => {
      const section = byId(sections, id);
      expect(section.state).toBe(SECTION_STATE.DISABLED);
      expect(section.lockedHintKey).toBe("caseView.locked.awaitingStage");
      expect(section.lockedHintValues.stage).toBe(messages[key]);
      // Never the raw SCREAMING_SNAKE constant a bench user cannot read.
      expect(section.lockedHintValues.stage).not.toMatch(/_/);
    });

    ["pathology-section-review", "pathology-section-findings"].forEach((id) => {
      expect(byId(sections, id).state).toBe(SECTION_STATE.DISABLED);
    });
  });
});

describe("deriveCaseSections and pathologist-only sections", () => {
  const REVIEW = "pathology-section-review";
  const FINDINGS = "pathology-section-findings";
  const stateOf = (status, isPathologist, caseFields = {}) => {
    const sections = deriveCaseSections({
      intl,
      status,
      isPathologist,
      ...caseFields,
    });
    return {
      review: byId(sections, REVIEW),
      findings: byId(sections, FINDINGS),
    };
  };
  const awaiting = (stageId) =>
    intl.formatMessage({ id: stageDisplayKey(stageId) });

  it("the rail stays on Under Pathologist Review while the case is read", () => {
    const sections = deriveCaseSections({
      intl,
      status: "UNDER_REVIEW",
      isPathologist: true,
      readingStage: "UNDER_REVIEW",
      findingsOpen: true,
    });

    expect(sections[railCurrentIndex(sections, "UNDER_REVIEW")].id).toBe(
      REVIEW,
    );
    expect(sectionIdOfStage("UNDER_REVIEW")).toBe(REVIEW);
  });

  it("a case on the bench names the stage that opens Findings: the reading stage the case sends (S1, S2)", () => {
    // S1: Under Pathologist Review on.
    const on = stateOf("STAINING", true, { readingStage: "UNDER_REVIEW" });
    expect(on.findings.state).toBe(SECTION_STATE.DISABLED);
    expect(on.findings.lockedHintKey).toBe("caseView.locked.awaitingStage");
    expect(on.findings.lockedHintValues.stage).toBe(awaiting("UNDER_REVIEW"));
    // The review keeps its own gate.
    expect(on.review.lockedHintValues.stage).toBe(
      awaiting("READY_PATHOLOGIST"),
    );

    // S2: switched off, and a case that sends no reading stage at all.
    [{ readingStage: "READY_PATHOLOGIST" }, {}].forEach((caseFields) => {
      const off = stateOf("STAINING", true, caseFields);
      expect(off.findings.state).toBe(SECTION_STATE.DISABLED);
      expect(off.findings.lockedHintValues.stage).toBe(
        awaiting("READY_PATHOLOGIST"),
      );
    });
  });

  it('in the queue Findings stays locked: a pathologist reads "Press Begin review to write the findings.", anyone else the stage hint; Review is open for a pathologist and read-only for anyone else (S3, S4)', () => {
    const queue = { readingStage: "UNDER_REVIEW", findingsOpen: false };

    const pathologist = stateOf("READY_PATHOLOGIST", true, queue);
    expect(pathologist.findings.state).toBe(SECTION_STATE.DISABLED);
    expect(pathologist.findings.lockedHintKey).toBe(
      "pathology.locked.beginReviewFirst",
    );
    expect(pathologist.review.state).toBe(SECTION_STATE.OPEN);

    const technician = stateOf("READY_PATHOLOGIST", false, queue);
    expect(technician.findings.state).toBe(SECTION_STATE.DISABLED);
    expect(technician.findings.lockedHintKey).toBe(
      "caseView.locked.awaitingStage",
    );
    expect(technician.findings.lockedHintValues.stage).toBe(
      awaiting("UNDER_REVIEW"),
    );
    expect(technician.review.state).toBe(SECTION_STATE.READ_ONLY);
  });

  it("where review is switched off, Findings opens in the queue (S5)", () => {
    const { review, findings } = stateOf("READY_PATHOLOGIST", true, {
      readingStage: "READY_PATHOLOGIST",
      findingsOpen: true,
    });

    expect(review.state).toBe(SECTION_STATE.OPEN);
    expect(findings.state).toBe(SECTION_STATE.OPEN);
  });

  it("leaves the same two sections read-only, not disabled, for anyone who is not the pathologist", () => {
    const sections = deriveCaseSections({
      intl,
      status: "READY_PATHOLOGIST",
      isPathologist: false,
      readingStage: "READY_PATHOLOGIST",
      findingsOpen: true,
    });

    expect(byId(sections, REVIEW).state).toBe(SECTION_STATE.READ_ONLY);
    expect(byId(sections, FINDINGS).state).toBe(SECTION_STATE.READ_ONLY);
  });

  it("under review both are open for a pathologist and read-only for anyone else (S6, S7)", () => {
    ["UNDER_REVIEW", "READY_PATHOLOGIST"].forEach((readingStage) => {
      const reading = { readingStage, findingsOpen: true };
      const pathologist = stateOf("UNDER_REVIEW", true, reading);
      expect(pathologist.review.state).toBe(SECTION_STATE.OPEN);
      expect(pathologist.findings.state).toBe(SECTION_STATE.OPEN);

      const technician = stateOf("UNDER_REVIEW", false, reading);
      expect(technician.review.state).toBe(SECTION_STATE.READ_ONLY);
      expect(technician.findings.state).toBe(SECTION_STATE.READ_ONLY);
    });
    expect(
      byId(
        deriveCaseSections({
          intl,
          status: "UNDER_REVIEW",
          isPathologist: true,
          readingStage: "UNDER_REVIEW",
          findingsOpen: true,
        }),
        "pathology-section-grossing",
      ).state,
    ).toBe(SECTION_STATE.COMPLETE);
  });

  it("once signed out both are read-only for everyone (S8)", () => {
    [true, false].forEach((isPathologist) => {
      const { review, findings } = stateOf("COMPLETED", isPathologist, {
        readingStage: "UNDER_REVIEW",
        findingsOpen: false,
      });
      expect(review.state).toBe(SECTION_STATE.READ_ONLY);
      expect(findings.state).toBe(SECTION_STATE.READ_ONLY);
    });
  });

  it("a completed case that was never released keeps both editable for a pathologist (S9)", () => {
    const legacy = { readingStage: "UNDER_REVIEW", findingsOpen: true };

    const pathologist = stateOf("COMPLETED", true, legacy);
    expect(pathologist.review.state).toBe(SECTION_STATE.COMPLETE);
    expect(pathologist.findings.state).toBe(SECTION_STATE.COMPLETE);

    const technician = stateOf("COMPLETED", false, legacy);
    expect(technician.review.state).toBe(SECTION_STATE.READ_ONLY);
    expect(technician.findings.state).toBe(SECTION_STATE.READ_ONLY);
  });
});

describe("Begin review and the reading's helpers", () => {
  it("the move to Under Pathologist Review is labelled Begin review; `grossExamPlace`, `slidesReceived` and `neverCheckedSlides` answer by the rows", () => {
    expect(advanceLabelKey("UNDER_REVIEW")).toBe(
      "pathology.action.beginReview",
    );

    ["ACCESSIONED", "GROSSING", "COVERSLIPPING"].forEach((status) =>
      expect(grossExamPlace(status)).toBe("grossing"),
    );
    ["READY_PATHOLOGIST", "UNDER_REVIEW", "COMPLETED"].forEach((status) =>
      expect(grossExamPlace(status)).toBe("findings"),
    );

    const caseInfo = {
      blocks: [
        { id: 1, designation: "A1", active: true },
        { id: 2, designation: "A2", active: true },
      ],
      slides: [
        {
          id: 5,
          designation: "1",
          blockId: 1,
          active: true,
          stainId: "31",
          stainStatus: "STAINED",
          coverslipQc: "PASS",
        },
        // Retired: left out.
        { id: 6, designation: "2", blockId: 1, active: false, stainId: "31" },
        // In use, never checked, nothing recorded.
        { id: 7, designation: "1", blockId: 2, active: true, stainId: "32" },
        // Cut before slides named their block: no label to check.
        { id: 8, designation: "9", blockId: null, active: true },
      ],
      stainNames: { 31: "H&E", 32: "PAS" },
      labelVerifications: { 5: { verifiedAt: "x", verifiedBy: "y" } },
    };

    expect(slidesReceived(caseInfo, intl)).toEqual([
      {
        id: 5,
        name: "A1.1",
        stain: "H&E",
        blockId: 1,
        stained: true,
        coverslipQc: "PASS",
      },
      {
        id: 7,
        name: "A2.1",
        stain: "PAS",
        blockId: 2,
        stained: false,
        coverslipQc: null,
      },
      {
        id: 8,
        name: "9",
        stain: messages["common.notRecorded"],
        blockId: null,
        stained: false,
        coverslipQc: null,
      },
    ]);
    expect(neverCheckedSlides(caseInfo)).toEqual(["A2.1"]);
    expect(
      neverCheckedSlides({
        ...caseInfo,
        labelVerifications: { 5: {}, 7: {} },
      }),
    ).toEqual([]);
    expect(slidesReceived({}, intl)).toEqual([]);
  });
});

describe("a stage a laboratory has switched off", () => {
  it("locks the section by configuration while the case is only at an earlier stage, without touching an unrelated section", () => {
    const configurationProperties = {
      PATHOLOGY_STAGE_COVERSLIPPING_ENABLED: "false",
    };

    const sections = deriveCaseSections({
      intl,
      status: "STAINING",
      isPathologist: false,
      configurationProperties,
    });

    const coverslipping = byId(sections, "pathology-section-coverslipping");
    expect(coverslipping.state).toBe(SECTION_STATE.DISABLED);
    expect(coverslipping.lockedHintKey).toBe("pathology.locked.stageDisabled");

    const railItems = deriveRailItems(sections, {
      intl,
      status: "STAINING",
      reportCount: 0,
      openRequestNames: [],
    });
    const coverslippingItem = railItems.find(
      (item) => item.id === "pathology-section-coverslipping",
    );
    expect(coverslippingItem.notApplicable).toBe(true);
    expect(coverslippingItem.complete).toBe(false);

    expect(byId(sections, "pathology-section-staining").state).toBe(
      SECTION_STATE.OPEN,
    );
  });

  it("still opens for the case actually standing at that stage, even though the laboratory does not track it", () => {
    const configurationProperties = {
      PATHOLOGY_STAGE_COVERSLIPPING_ENABLED: "false",
    };

    const sections = deriveCaseSections({
      intl,
      status: "COVERSLIPPING",
      isPathologist: false,
      configurationProperties,
    });

    const coverslipping = byId(sections, "pathology-section-coverslipping");
    expect(coverslipping.state).toBe(SECTION_STATE.OPEN);

    const railItems = deriveRailItems(sections, {
      intl,
      status: "COVERSLIPPING",
      reportCount: 0,
      openRequestNames: [],
    });
    const coverslippingItem = railItems.find(
      (item) => item.id === "pathology-section-coverslipping",
    );
    expect(coverslippingItem.notApplicable).toBe(false);

    expect(railCurrentIndex(sections, "COVERSLIPPING")).toBe(
      sections.indexOf(coverslipping),
    );
  });
});

describe("deriveRailItems", () => {
  // Embedded too, so the embedding step's own rule is met and every step the
  // case has passed reads complete.
  const grossed = {
    grossExam: "Firm tan nodule, 20mm",
    blocks: [{ id: "1", cassetteState: "BLOCK" }],
  };

  it("marks each section complete once the case has moved past every stage it spans, and leaves the current and later ones not complete", () => {
    const sections = deriveCaseSections({
      intl,
      status: "MICROTOMY",
      isPathologist: true,
      decalcificationRequired: true,
    });

    const items = deriveRailItems(sections, {
      intl,
      status: "MICROTOMY",
      reportCount: 0,
      openRequestNames: [],
      caseInfo: grossed,
    });

    expect(items.slice(0, 5).every((item) => item.complete)).toBe(true);
    expect(items.slice(5).every((item) => !item.complete)).toBe(true);

    const itemsWithAReport = deriveRailItems(sections, {
      intl,
      status: "MICROTOMY",
      reportCount: 2,
      openRequestNames: [],
    });
    const reportsItem = itemsWithAReport.find(
      (item) => item.id === "pathology-section-reports",
    );
    expect(reportsItem.complete).toBe(true);
  });

  // The step a reader looks at first must not claim work the section's own
  // rule says was never recorded. Grossing is the one stage with such a rule
  // (FR-3.4), so it is the one step that can disagree with the case's
  // position on the bench.
  it("leaves grossing incomplete on the rail when the case went past it with nothing recorded", () => {
    const sections = deriveCaseSections({
      intl,
      status: "MICROTOMY",
      isPathologist: true,
    });

    const grossingItem = (caseInfo) =>
      deriveRailItems(sections, {
        intl,
        status: "MICROTOMY",
        reportCount: 0,
        openRequestNames: [],
        caseInfo,
      }).find((item) => item.id === "pathology-section-grossing");

    expect(grossingItem({}).complete).toBe(false);
    expect(
      grossingItem({ grossExam: "  ", blocks: [{ id: "1" }] }).complete,
    ).toBe(false);
    expect(grossingItem({ grossExam: "Described", blocks: [] }).complete).toBe(
      false,
    );
    expect(grossingItem(grossed).complete).toBe(true);
  });

  // Microtomy records slides, so its step, like grossing's, follows the rows
  // rather than the case's position: a case past microtomy with no slide is
  // not ticked, and the summary beside it saying "Slides 0" agrees.
  it("leaves microtomy incomplete on the rail when the case went past it with no slide", () => {
    const sections = deriveCaseSections({
      intl,
      status: "READY_PATHOLOGIST",
      isPathologist: true,
    });
    const microtomyItem = (caseInfo) =>
      deriveRailItems(sections, {
        intl,
        status: "READY_PATHOLOGIST",
        reportCount: 0,
        openRequestNames: [],
        caseInfo,
      }).find((item) => item.id === "pathology-section-microtomy");

    expect(microtomyItem({ slides: [] }).complete).toBe(false);
    expect(microtomyItem({ slides: [{ id: "1" }] }).complete).toBe(true);
  });

  it("names the open requests on the review item's pending label, and leaves it undefined when there are none", () => {
    const sections = deriveCaseSections({
      intl,
      status: "READY_PATHOLOGIST",
      isPathologist: true,
    });

    const withRequests = deriveRailItems(sections, {
      intl,
      status: "READY_PATHOLOGIST",
      reportCount: 0,
      openRequestNames: ["Recut", "Extra levels"],
    });
    const reviewItem = withRequests.find(
      (item) => item.id === "pathology-section-review",
    );
    expect(reviewItem.pendingLabel).toBe(
      messages["pathology.badge.openRequests"].replace(
        "{names}",
        "Recut, Extra levels",
      ),
    );

    const withoutRequests = deriveRailItems(sections, {
      intl,
      status: "READY_PATHOLOGIST",
      reportCount: 0,
      openRequestNames: [],
    });
    const reviewItemWithNoPending = withoutRequests.find(
      (item) => item.id === "pathology-section-review",
    );
    expect(reviewItemWithNoPending.pendingLabel).toBeUndefined();
  });

  // Carbon puts a step's secondary label inside the step's own button, where
  // a third name wrapped onto a third line and overran the step below it.
  // The names still lead: a step that said only "3" would name nothing the
  // pathologist could act on.
  it("names two outstanding requests and counts whatever is left over", () => {
    const sections = deriveCaseSections({
      intl,
      status: "READY_PATHOLOGIST",
      isPathologist: true,
    });

    const reviewItem = (names) =>
      deriveRailItems(sections, {
        intl,
        status: "READY_PATHOLOGIST",
        reportCount: 0,
        openRequestNames: names,
      }).find((item) => item.id === "pathology-section-review");

    expect(
      reviewItem(["Recut", "Extra levels", "Special stain"]).pendingLabel,
    ).toBe(
      messages["pathology.badge.openRequestsMore"]
        .replace("{names}", "Recut, Extra levels")
        .replace("{count}", "1"),
    );

    // Two is the whole list, so nothing is counted and nothing is left out.
    expect(reviewItem(["Recut", "Extra levels"]).pendingLabel).toBe(
      messages["pathology.badge.openRequests"].replace(
        "{names}",
        "Recut, Extra levels",
      ),
    );

    // However many are capped out of the visible label, the hover title names
    // every one of them.
    expect(
      reviewItem(["Recut", "Extra levels", "Special stain"]).pendingTitle,
    ).toBe(
      messages["pathology.badge.openRequests"].replace(
        "{names}",
        "Recut, Extra levels, Special stain",
      ),
    );
  });
});

describe("openRequestNames", () => {
  it("keeps the open requests in their original order and drops the closed ones", () => {
    const requests = [
      { id: 1, instruction: "Recut", status: "OPENED" },
      { id: 2, instruction: "Extra levels", status: "OPENED" },
      { id: 3, instruction: "Special stain", status: "COMPLETED" },
      { id: 4, instruction: "Deeper section", status: "CANCELLED" },
    ];

    expect(openRequestNames(requests)).toEqual(["Recut", "Extra levels"]);
    expect(openRequests(requests).map((request) => request.id)).toEqual([1, 2]);
  });

  // The case read names a request by its instruction; the old dictionary
  // row's `value` is no longer served, so reading it would name nothing.
  it("names a request by its instruction, not by the old dictionary value", () => {
    expect(
      openRequestNames([
        { id: 1, value: "Old", instruction: "Recut", status: "OPENED" },
      ]),
    ).toEqual(["Recut"]);
  });

  // The server counts a request as open only when it says OPENED, and the
  // dashboard tile is counted with that same predicate. Reading a row with no
  // status as open here would make this screen disagree with the tile.
  it("counts a request with no status at all as closed, not as open", () => {
    expect(
      openRequestNames([
        { id: 1, instruction: "Unstamped", status: null },
        { id: 2, instruction: "Also unstamped" },
        { id: 3, instruction: "Recut", status: "OPENED" },
      ]),
    ).toEqual(["Recut"]);
  });

  it("returns nothing for a case with no requests array at all", () => {
    expect(openRequestNames(undefined)).toEqual([]);
    expect(openRequests(undefined)).toEqual([]);
  });
});

describe("openRequestsLabel, for instructions written as sentences", () => {
  const long =
    "Cut three deeper levels through block A2 and stain each with H&E";

  // An instruction is free text up to 255 characters, and the rail step has
  // two lines; the step names it short and the title names it whole.
  it("cuts a long instruction to forty characters and an ellipsis in the visible label, and keeps it whole in the title", () => {
    const label = openRequestsLabel(intl, [long, "Recut"]);

    expect(label.visible).toBe(
      intl.formatMessage(
        { id: "pathology.badge.openRequests" },
        { names: long.slice(0, 40) + "…, Recut" },
      ),
    );
    expect(label.full).toBe(
      intl.formatMessage(
        { id: "pathology.badge.openRequests" },
        { names: long + ", Recut" },
      ),
    );
  });

  it("leaves an instruction of forty characters or fewer as it is", () => {
    const forty = "x".repeat(40);

    expect(openRequestsLabel(intl, [forty]).visible).toBe(
      intl.formatMessage(
        { id: "pathology.badge.openRequests" },
        { names: forty },
      ),
    );
  });
});

describe("sectionBadge", () => {
  const sectionsAt = (status, isPathologist = true) =>
    deriveCaseSections({ intl, status, isPathologist });

  const described = {
    grossExam: "Firm tan nodule, 20mm",
    blocks: [{ id: "1" }],
  };

  it("marks grossing complete once the specimen is described and cut", () => {
    const grossing = byId(
      sectionsAt("MICROTOMY"),
      "pathology-section-grossing",
    );

    expect(sectionBadge(grossing, { caseInfo: described })).toEqual({
      kind: "complete",
      textKey: "common.complete",
    });
  });

  // The rule is FR-3.4's, not the case's position on the bench. A case at the
  // pathologist has gone past grossing whatever was written down there, and a
  // header reading "Complete" over an empty description is the screen
  // asserting work nobody recorded.
  it.each([
    ["nothing recorded at all", {}],
    [
      "a description but no block",
      { grossExam: "Firm tan nodule", blocks: [] },
    ],
  ])("badges grossing with nothing when it has %s", (_, caseInfo) => {
    const grossing = byId(
      sectionsAt("READY_PATHOLOGIST"),
      "pathology-section-grossing",
    );

    expect(sectionBadge(grossing, { caseInfo })).toBeNull();
  });

  it("counts the rows a section holds when it has rows but no rule to call them complete", () => {
    const sections = sectionsAt("READY_PATHOLOGIST");
    const grossing = byId(sections, "pathology-section-grossing");
    const microtomy = byId(sections, "pathology-section-microtomy");

    // A whitespace-only description does not make grossing complete; the
    // blocks are still counted.
    expect(
      sectionBadge(grossing, {
        caseInfo: { grossExam: "   ", blocks: [{ id: "1" }, { id: "2" }] },
      }),
    ).toEqual({
      kind: "inProgress",
      textKey: "pathology.badge.cassetteCount",
      values: { count: 2 },
    });
    expect(
      sectionBadge(microtomy, {
        caseInfo: { slides: [{ id: "1" }, { id: "2" }, { id: "3" }] },
      }),
    ).toEqual({
      kind: "inProgress",
      textKey: "pathology.badge.slideCount",
      values: { count: 3 },
    });
    expect(sectionBadge(microtomy, { caseInfo: { slides: [] } })).toBeNull();
  });

  // These stages between grossing and the reading are ones OpenELIS records
  // no work at yet, so none of them has a completion rule to satisfy.
  it.each([
    "pathology-section-decalcification",
    "pathology-section-processing",
    "pathology-section-staining",
  ])("badges %s with nothing however far the case has gone", (id) => {
    const section = byId(sectionsAt("READY_PATHOLOGIST"), id);

    expect(sectionBadge(section, { caseInfo: described })).toBeNull();
  });

  it("badges the review section with how many requests are outstanding", () => {
    const review = byId(
      sectionsAt("READY_PATHOLOGIST"),
      "pathology-section-review",
    );

    expect(sectionBadge(review, { openRequestCount: 2 })).toEqual({
      kind: "pending",
      textKey: "pathology.badge.openRequestCount",
      values: { count: 2 },
    });
  });

  // Inversion of the badge above: the count is the only thing that puts it
  // there, so a review with every request closed says nothing rather than
  // announcing that none are open.
  it("badges a review with no outstanding request with nothing", () => {
    const review = byId(
      sectionsAt("READY_PATHOLOGIST"),
      "pathology-section-review",
    );

    expect(sectionBadge(review, { openRequestCount: 0 })).toBeNull();
  });

  // A signed-out case with a recut still outstanding is not finished, so the
  // header has to say the outstanding thing rather than the reassuring one.
  // The case was never released, the one signed-out case whose review the
  // pathologist still holds (S9).
  it("says a signed-out review is still outstanding when a request is open on it", () => {
    const review = byId(
      deriveCaseSections({
        intl,
        status: "COMPLETED",
        isPathologist: true,
        requestCount: 1,
        findingsOpen: true,
      }),
      "pathology-section-review",
    );

    expect(review.state).toBe(SECTION_STATE.COMPLETE);
    expect(sectionBadge(review, { openRequestCount: 1 }).kind).toBe("pending");
  });

  // The bench works the requests from a read-only review, so the count has to
  // be on the header it looks at; before, the read-only return came first and
  // the header said only Read only.
  it.each([
    ["at a bench stage", "MICROTOMY", true],
    ["for someone other than the pathologist", "READY_PATHOLOGIST", false],
  ])(
    "counts the open requests on a review in bench mode %s",
    (_label, status, isPathologist) => {
      const review = byId(
        deriveCaseSections({ intl, status, isPathologist, requestCount: 2 }),
        "pathology-section-review",
      );

      expect(review.benchRequests).toBe(true);
      expect(sectionBadge(review, { openRequestCount: 2 })).toEqual({
        kind: "pending",
        textKey: "pathology.badge.openRequestCount",
        values: { count: 2 },
      });
    },
  );

  // With nothing open, a read-only review leaves the slot to the shell's own
  // Read only marker.
  it("leaves the read-only marker in place on a review with no open request", () => {
    const review = byId(
      sectionsAt("READY_PATHOLOGIST", false),
      "pathology-section-review",
    );

    expect(review.state).toBe(SECTION_STATE.READ_ONLY);
    expect(sectionBadge(review, { openRequestCount: 0 })).toBeNull();
  });

  // The rule moved ahead of the read-only return for the review only: any
  // other read-only section still leaves the slot to the shell.
  it("leaves the read-only marker on a read-only findings section however many requests are open", () => {
    const findings = byId(
      sectionsAt("READY_PATHOLOGIST", false),
      "pathology-section-findings",
    );

    expect(findings.state).toBe(SECTION_STATE.READ_ONLY);
    expect(sectionBadge(findings, { openRequestCount: 2 })).toBeNull();
  });

  it("badges a section that is neither complete nor waiting on a request with nothing", () => {
    const grossing = byId(sectionsAt("GROSSING"), "pathology-section-grossing");

    expect(sectionBadge(grossing, { openRequestCount: 2 })).toBeNull();
  });
});

describe("which sections start expanded", () => {
  const openByDefault = (status, id) =>
    byId(deriveCaseSections({ intl, status, isPathologist: true }), id)
      .openByDefault;

  // The gross description and the microscopic description have to be legible
  // together for a sign-out, which is the reason this screen is an accordion
  // and not a set of tabs. Grossing derives as complete by then, so without
  // this it would collapse exactly as the reading sections opened.
  it("keeps grossing expanded once the case is with the pathologist", () => {
    expect(
      openByDefault("READY_PATHOLOGIST", "pathology-section-grossing"),
    ).toBe(true);
    expect(openByDefault("UNDER_REVIEW", "pathology-section-grossing")).toBe(
      true,
    );
  });

  it("leaves grossing to decide for itself while the case is still on the bench", () => {
    expect(
      openByDefault("GROSSING", "pathology-section-grossing"),
    ).toBeUndefined();
    expect(
      openByDefault("MICROTOMY", "pathology-section-grossing"),
    ).toBeUndefined();
  });

  it("starts case information collapsed whatever stage the case stands at", () => {
    PATHOLOGY_STAGES.forEach((status) => {
      expect(openByDefault(status, "pathology-section-case-info")).toBe(false);
    });
  });
});

describe("stageBadgeKind", () => {
  it("maps every known stage to the shared badge vocabulary", () => {
    expect(stageBadgeKind("ACCESSIONED")).toBe("none");

    [
      "GROSSING",
      "DECALCIFICATION",
      "PROCESSING",
      "EMBEDDING",
      "MICROTOMY",
      "STAINING",
      "COVERSLIPPING",
      "UNDER_REVIEW",
    ].forEach((stage) => {
      expect(stageBadgeKind(stage)).toBe("inProgress");
    });

    expect(stageBadgeKind("READY_PATHOLOGIST")).toBe("pending");
    expect(stageBadgeKind("COMPLETED")).toBe("complete");
  });

  it("falls back to none for a stage it does not recognize", () => {
    expect(stageBadgeKind("FROZEN_SECTION")).toBe("none");
  });
});

describe("deriveCaseSections across every stage this build knows about", () => {
  it("always returns eleven sections, each with a real state, whatever the case status", () => {
    PATHOLOGY_STAGES.forEach((status) => {
      const sections = deriveCaseSections({
        intl,
        status,
        isPathologist: true,
      });

      expect(sections).toHaveLength(11);
      sections.forEach((section) => {
        expect(section.state).toBeDefined();
        expect(Object.values(SECTION_STATE)).toContain(section.state);
      });
    });
  });
});

describe("a status this build does not recognize", () => {
  // A browser older than the server it talks to must never read an unknown
  // status as ahead of or behind any stage, so every gated section stays
  // locked and the rail marks nothing complete or current.
  it("locks every stage-gated section and leaves the rail with nothing complete or current", () => {
    const status = "SOME_FUTURE_STAGE_THIS_BUILD_DOES_NOT_KNOW";
    const sections = deriveCaseSections({
      intl,
      status,
      isPathologist: true,
      decalcificationRequired: true,
    });

    sections
      .filter((section) => section.gate !== null)
      .forEach((section) => {
        expect(section.state).toBe(SECTION_STATE.DISABLED);
        expect(section.lockedHintKey).toBe("caseView.locked.awaitingStage");
      });

    const items = deriveRailItems(sections, {
      intl,
      status,
      reportCount: 0,
      openRequestNames: [],
    });
    expect(items.some((item) => item.complete)).toBe(false);
    expect(railCurrentIndex(sections, status)).toBe(-1);
  });
});

describe("grossingComplete over saved cassettes in use", () => {
  it("does not call a case cut whose only cassette has been deactivated", () => {
    expect(
      grossingComplete({
        grossExam: "Firm tan nodule",
        blocks: [{ id: "1", active: false }],
      }),
    ).toBe(false);
    expect(
      grossingComplete({
        grossExam: "Firm tan nodule",
        blocks: [
          { id: "1", active: false },
          { id: "2", active: true },
        ],
      }),
    ).toBe(true);
  });

  it("does not call a case cut on a cassette added but not yet saved", () => {
    expect(
      grossingComplete({
        grossExam: "Firm tan nodule",
        blocks: [{ location: "" }],
      }),
    ).toBe(false);
  });
});

describe("counting badges over rows in use", () => {
  const sections = deriveCaseSections({
    intl,
    status: "READY_PATHOLOGIST",
    isPathologist: true,
  });

  it("counts only the cassettes and slides still in use", () => {
    expect(
      sectionBadge(byId(sections, "pathology-section-grossing"), {
        caseInfo: {
          blocks: [{ id: "1" }, { id: "2", active: false }, { location: "" }],
        },
      }),
    ).toEqual({
      kind: "inProgress",
      textKey: "pathology.badge.cassetteCount",
      values: { count: 1 },
    });
    expect(
      sectionBadge(byId(sections, "pathology-section-microtomy"), {
        caseInfo: { slides: [{ id: "1", active: false }] },
      }),
    ).toBeNull();
  });

  it("ticks microtomy on the rail for a saved slide in use, and for no other slide", () => {
    const microtomy = (slides) =>
      deriveRailItems(sections, {
        intl,
        status: "READY_PATHOLOGIST",
        reportCount: 0,
        openRequestNames: [],
        caseInfo: { slides },
      }).find((item) => item.id === "pathology-section-microtomy");

    expect(microtomy([{ id: "1", active: false }]).complete).toBe(false);
    expect(microtomy([{ blockId: "1", location: "" }]).complete).toBe(false);
    expect(microtomy([{ id: "1", active: true }]).complete).toBe(true);
  });
});

describe("decalcification and the grossing flag", () => {
  const DECALCIFICATION = "pathology-section-decalcification";

  const derive = (status, decalcificationRequired, configurationProperties) =>
    deriveCaseSections({
      intl,
      status,
      isPathologist: true,
      configurationProperties,
      decalcificationRequired,
    });

  const railItem = (sections, status) =>
    deriveRailItems(sections, {
      intl,
      status,
      reportCount: 0,
      openRequestNames: [],
    }).find((item) => item.id === DECALCIFICATION);

  it("locks decalcification as not required on a specimen grossing did not flag, and marks its rail step not applicable", () => {
    const sections = derive("GROSSING", false);
    const decalcification = byId(sections, DECALCIFICATION);

    expect(decalcification.state).toBe(SECTION_STATE.DISABLED);
    expect(decalcification.lockedHintKey).toBe(
      "pathology.locked.decalcNotRequired",
    );
    expect(railItem(sections, "GROSSING").notApplicable).toBe(true);
  });

  it("never ticks the step of a decalcification the specimen did not need, however far the case has gone", () => {
    const sections = derive("MICROTOMY", false);
    const item = railItem(sections, "MICROTOMY");

    expect(item.notApplicable).toBe(true);
    expect(item.complete).toBe(false);
  });

  it("locks a flagged specimen's decalcification by the stage order instead, and ticks it once the case is past", () => {
    const atGrossing = derive("GROSSING", true);
    const decalcification = byId(atGrossing, DECALCIFICATION);

    expect(decalcification.state).toBe(SECTION_STATE.DISABLED);
    expect(decalcification.lockedHintKey).toBe("caseView.locked.awaitingStage");
    expect(decalcification.lockedHintValues.stage).toBe(
      messages["pathology.stage.decalcification"],
    );
    expect(railItem(atGrossing, "GROSSING").notApplicable).toBe(false);

    expect(railItem(derive("MICROTOMY", true), "MICROTOMY").complete).toBe(
      true,
    );
  });

  it("opens decalcification for a case standing at it even when grossing did not flag the specimen", () => {
    const sections = derive("DECALCIFICATION", false);
    const decalcification = byId(sections, DECALCIFICATION);

    expect(decalcification.state).toBe(SECTION_STATE.OPEN);
    expect(railItem(sections, "DECALCIFICATION").notApplicable).toBe(false);
  });

  it("says a stage the laboratory does not track is untracked before saying the specimen does not need it", () => {
    const sections = derive("GROSSING", false, {
      PATHOLOGY_STAGE_DECALCIFICATION_ENABLED: "false",
    });

    expect(byId(sections, DECALCIFICATION).lockedHintKey).toBe(
      "pathology.locked.stageDisabled",
    );
    expect(railItem(sections, "GROSSING").notApplicable).toBe(true);
  });
});

describe("stageApplies", () => {
  it("applies decalcification only to a specimen grossing flagged as needing it", () => {
    expect(
      stageApplies("DECALCIFICATION", { decalcificationRequired: false }),
    ).toBe(false);
    expect(
      stageApplies("DECALCIFICATION", { decalcificationRequired: true }),
    ).toBe(true);
  });

  it("turns down a stage the laboratory does not track, however the specimen is flagged", () => {
    const configurationProperties = {
      PATHOLOGY_STAGE_DECALCIFICATION_ENABLED: "false",
      PATHOLOGY_STAGE_COVERSLIPPING_ENABLED: "false",
    };

    expect(
      stageApplies("DECALCIFICATION", {
        configurationProperties,
        decalcificationRequired: true,
      }),
    ).toBe(false);
    expect(
      stageApplies("COVERSLIPPING", {
        configurationProperties,
        decalcificationRequired: true,
      }),
    ).toBe(false);
  });

  it("lets the flag decide no stage but decalcification", () => {
    [
      "ACCESSIONED",
      "GROSSING",
      "PROCESSING",
      "EMBEDDING",
      "MICROTOMY",
      "STAINING",
      "COVERSLIPPING",
      "READY_PATHOLOGIST",
      "UNDER_REVIEW",
      "COMPLETED",
    ].forEach((stageId) => {
      expect(stageApplies(stageId, { decalcificationRequired: false })).toBe(
        true,
      );
      expect(stageApplies(stageId, { decalcificationRequired: true })).toBe(
        true,
      );
    });
  });
});

describe("decalcificationFlagFrozen", () => {
  it("leaves the flag editable up to and at decalcification", () => {
    ["ACCESSIONED", "GROSSING", "DECALCIFICATION"].forEach((stored) => {
      expect(decalcificationFlagFrozen(stored)).toBe(false);
    });
  });

  it("freezes the flag at every stage after decalcification", () => {
    [
      "PROCESSING",
      "EMBEDDING",
      "MICROTOMY",
      "STAINING",
      "COVERSLIPPING",
      "READY_PATHOLOGIST",
      "UNDER_REVIEW",
      "COMPLETED",
    ].forEach((stored) => {
      expect(decalcificationFlagFrozen(stored)).toBe(true);
    });
  });

  // The server judges by the stored stage too, so a stage it has and this
  // build lacks is left for the server to decide rather than guessed at.
  it("freezes nothing for a case not yet loaded or at a stage this build does not know", () => {
    expect(decalcificationFlagFrozen(null)).toBe(false);
    expect(decalcificationFlagFrozen("SOME_LATER_STAGE")).toBe(false);
  });
});

describe("embedding, read from the cassettes", () => {
  const sectionsAt = (status) =>
    deriveCaseSections({
      intl,
      status,
      isPathologist: true,
      decalcificationRequired: true,
    });
  const embeddingSection = (status) =>
    byId(sectionsAt(status), "pathology-section-embedding");
  const railStep = (status, blocks, configurationProperties) =>
    deriveRailItems(
      deriveCaseSections({
        intl,
        status,
        isPathologist: true,
        decalcificationRequired: true,
        configurationProperties,
      }),
      {
        intl,
        status,
        reportCount: 0,
        openRequestNames: [],
        caseInfo: { blocks },
      },
    ).find((item) => item.id === "pathology-section-embedding");

  const cassette = (id, designation, overrides = {}) => ({
    id,
    designation,
    cassetteState: "CASSETTE",
    active: true,
    ...overrides,
  });
  const block = (id, designation) =>
    cassette(id, designation, { cassetteState: "BLOCK" });

  const oneOfThree = [block(1, "A1"), cassette(2, "A2"), cassette(3, "A3")];
  const allThree = [block(1, "A1"), block(2, "A2"), block(3, "A3")];

  it("badges the section with nothing while no cassette has been cut", () => {
    expect(
      sectionBadge(embeddingSection("EMBEDDING"), { caseInfo: { blocks: [] } }),
    ).toBeNull();
  });

  it("badges the section with how many cassettes are embedded while any is outstanding", () => {
    expect(
      sectionBadge(embeddingSection("EMBEDDING"), {
        caseInfo: { blocks: oneOfThree },
      }),
    ).toEqual({
      kind: "partial",
      textKey: "pathology.badge.embeddedOf",
      values: { embedded: 1, total: 3 },
    });
  });

  it("badges the section Complete once every cassette in use is a block", () => {
    expect(
      sectionBadge(embeddingSection("EMBEDDING"), {
        caseInfo: {
          blocks: [...allThree, cassette(4, "A4", { active: false })],
        },
      }),
    ).toEqual({ kind: "complete", textKey: "common.complete" });
  });

  // The badge reports the record, not the bench position: a case moved on
  // with a cassette still unembedded keeps saying so, and one embedded early
  // already reads Complete.
  it("follows the rows and not the stage the case stands at", () => {
    expect(
      sectionBadge(embeddingSection("STAINING"), {
        caseInfo: { blocks: oneOfThree },
      }).kind,
    ).toBe("partial");
    expect(
      sectionBadge(embeddingSection("EMBEDDING"), {
        caseInfo: { blocks: allThree },
      }).kind,
    ).toBe("complete");
  });

  // Before the case reaches embedding nothing waits at that bench, so the
  // rail names nothing there yet; from the stage on it names every cassette.
  it("names the outstanding cassettes on the rail once the case has reached embedding, and not before", () => {
    expect(railStep("GROSSING", oneOfThree).pendingLabel).toBeUndefined();

    const atEmbedding = railStep("EMBEDDING", oneOfThree);
    const sentence = intl.formatMessage(
      { id: "pathology.badge.outstanding" },
      { designations: "A2, A3" },
    );
    expect(atEmbedding.pendingLabel).toBe(sentence);
    expect(atEmbedding.pendingTitle).toBe(sentence);

    expect(railStep("STAINING", oneOfThree).pendingLabel).toBe(sentence);
  });

  it("names nothing on the rail when the laboratory does not track embedding", () => {
    const step = railStep("STAINING", oneOfThree, {
      PATHOLOGY_STAGE_EMBEDDING_ENABLED: "false",
    });

    expect(step.notApplicable).toBe(true);
    expect(step.pendingLabel).toBeUndefined();
  });

  it("completes the rail step only once the case has passed embedding with every cassette a block", () => {
    expect(railStep("STAINING", allThree).complete).toBe(true);
    expect(railStep("STAINING", oneOfThree).complete).toBe(false);
    expect(railStep("EMBEDDING", allThree).complete).toBe(false);
    expect(railStep("STAINING", []).complete).toBe(false);
  });

  it("names two outstanding cassettes in one sentence", () => {
    const sentence = messages["pathology.badge.outstanding"].replace(
      "{designations}",
      "A2, A3",
    );
    expect(outstandingLabel(intl, ["A2", "A3"])).toEqual({
      visible: sentence,
      full: sentence,
    });
  });

  it("names two of three outstanding cassettes and counts the third, with all three in full", () => {
    expect(outstandingLabel(intl, ["A2", "A3", "A4"])).toEqual({
      visible: intl.formatMessage(
        { id: "pathology.badge.outstandingMore" },
        { designations: "A2, A3", count: 1 },
      ),
      full: intl.formatMessage(
        { id: "pathology.badge.outstanding" },
        { designations: "A2, A3, A4" },
      ),
    });
  });
});

// The summary's line follows the rail: not applicable where the stage is not
// tracked, and the outstanding cassettes named only once the case is there.
describe("embeddingStatement", () => {
  const cassette = (id, designation, cassetteState = "CASSETTE") => ({
    id,
    designation,
    cassetteState,
    active: true,
  });
  const oneOfThree = embeddingSummary([
    cassette(1, "A1", "BLOCK"),
    cassette(2, "A2"),
    cassette(3, "A3"),
  ]);

  it("reads not applicable where the laboratory does not track embedding", () => {
    expect(
      embeddingStatement(intl, {
        summary: oneOfThree,
        status: "EMBEDDING",
        configurationProperties: { PATHOLOGY_STAGE_EMBEDDING_ENABLED: "false" },
      }),
    ).toEqual({ value: messages["caseView.badge.notApplicable"] });
  });

  it("reads nothing before a cassette has been saved", () => {
    expect(
      embeddingStatement(intl, {
        summary: embeddingSummary([]),
        status: "EMBEDDING",
      }),
    ).toEqual({ value: null });
  });

  it("counts the embedded cassettes and names the outstanding ones once the case is at embedding", () => {
    expect(
      embeddingStatement(intl, { summary: oneOfThree, status: "EMBEDDING" }),
    ).toEqual({
      value: intl.formatMessage(
        { id: "pathology.badge.embeddedOf" },
        { embedded: 1, total: 3 },
      ),
      title: intl.formatMessage(
        { id: "pathology.badge.outstanding" },
        { designations: "A2, A3" },
      ),
    });
  });

  it("counts the embedded cassettes but names none before the case reaches embedding", () => {
    expect(
      embeddingStatement(intl, { summary: oneOfThree, status: "GROSSING" }),
    ).toEqual({
      value: intl.formatMessage(
        { id: "pathology.badge.embeddedOf" },
        { embedded: 1, total: 3 },
      ),
      title: undefined,
    });
  });
});

describe("advanceLabelKey", () => {
  it.each([
    ["GROSSING", "pathology.action.beginGrossing"],
    ["DECALCIFICATION", "pathology.action.sendForDecalc"],
    ["PROCESSING", "pathology.action.sendForProcessing"],
    ["EMBEDDING", "pathology.action.sendForEmbedding"],
    ["MICROTOMY", "pathology.action.sendToMicrotomy"],
    ["STAINING", "pathology.action.sendForStaining"],
    ["COVERSLIPPING", "pathology.action.sendForCoverslip"],
    ["READY_PATHOLOGIST", "pathology.action.sendToPathologist"],
  ])("labels a move to %s with a key the bundle holds", (stageId, key) => {
    expect(advanceLabelKey(stageId)).toBe(key);
    expect(messages[key]).toBeTruthy();
  });

  it("labels nothing an advance never reaches", () => {
    ["ACCESSIONED", "COMPLETED", "SOMETHING_NEW"].forEach((stageId) =>
      expect(advanceLabelKey(stageId)).toBeNull(),
    );
  });
});

describe("advanceBlockedReason", () => {
  const grossed = {
    grossExam: "Skin ellipse",
    blocks: [{ id: 1, designation: "A1", cassetteState: "CASSETTE" }],
  };

  it("says to wait while a save or a move is out, before unsaved work or any stage rule", () => {
    expect(
      advanceBlockedReason(
        intl,
        {},
        { storedStatus: "EMBEDDING", saving: true, dirty: true },
      ),
    ).toEqual({ key: "pathology.locked.requestInFlight" });
    expect(
      advanceBlockedReason(intl, grossed, {
        storedStatus: "GROSSING",
        moving: true,
      }),
    ).toEqual({ key: "pathology.locked.requestInFlight" });
  });

  it("says to save first while the form holds unsaved work, before any stage rule", () => {
    expect(
      advanceBlockedReason(intl, {}, { storedStatus: "GROSSING", dirty: true }),
    ).toEqual({ key: "pathology.locked.saveBeforeTransition" });
  });

  it("holds grossing until the specimen is described and cut", () => {
    expect(
      advanceBlockedReason(
        intl,
        { ...grossed, grossExam: "  " },
        { storedStatus: "GROSSING" },
      ),
    ).toEqual({ key: "pathology.locked.grossingIncomplete" });
    expect(
      advanceBlockedReason(
        intl,
        { ...grossed, blocks: [] },
        { storedStatus: "GROSSING" },
      ),
    ).toEqual({ key: "pathology.locked.grossingIncomplete" });
    expect(
      advanceBlockedReason(intl, grossed, { storedStatus: "GROSSING" }),
    ).toBe(null);
  });

  it("holds embedding until every cassette in use is a block, naming the outstanding ones", () => {
    const blocks = [
      { id: 1, designation: "A1", cassetteState: "BLOCK" },
      { id: 2, designation: "A2", cassetteState: "CASSETTE" },
      { id: 3, designation: "A3", cassetteState: "CASSETTE" },
      { id: 4, designation: "A4", cassetteState: "CASSETTE", active: false },
    ];

    expect(
      advanceBlockedReason(intl, { blocks }, { storedStatus: "EMBEDDING" }),
    ).toEqual({
      key: "pathology.locked.embeddingIncomplete",
      values: {
        outstanding: intl.formatMessage(
          { id: "pathology.badge.outstanding" },
          { designations: "A2, A3" },
        ),
      },
    });
    expect(
      advanceBlockedReason(
        intl,
        {
          blocks: blocks.map((block) => ({ ...block, cassetteState: "BLOCK" })),
        },
        { storedStatus: "EMBEDDING" },
      ),
    ).toBe(null);
  });

  // The rail's cap: a case with many cassettes must not stretch the bar.
  it("names two outstanding cassettes and counts the rest, as the rail does", () => {
    const blocks = ["A1", "A2", "A3", "A4"].map((designation, index) => ({
      id: index + 1,
      designation,
      cassetteState: "CASSETTE",
    }));

    expect(
      advanceBlockedReason(intl, { blocks }, { storedStatus: "EMBEDDING" })
        .values,
    ).toEqual({
      outstanding: intl.formatMessage(
        { id: "pathology.badge.outstandingMore" },
        { designations: "A1, A2", count: 2 },
      ),
    });
  });

  it("holds embedding on a case with no cassette at all, naming the way out", () => {
    expect(
      advanceBlockedReason(intl, { blocks: [] }, { storedStatus: "EMBEDDING" }),
    ).toEqual({ key: "pathology.locked.embeddingNoCassettes" });
  });

  it("holds microtomy until at least one slide in use is saved", () => {
    expect(
      advanceBlockedReason(intl, { slides: [] }, { storedStatus: "MICROTOMY" }),
    ).toEqual({ key: "pathology.locked.microtomyIncomplete" });
    expect(
      advanceBlockedReason(intl, {}, { storedStatus: "MICROTOMY" }),
    ).toEqual({ key: "pathology.locked.microtomyIncomplete" });
  });

  it("counts neither a retired slide nor one not yet saved towards microtomy", () => {
    expect(
      advanceBlockedReason(
        intl,
        { slides: [{ id: 5, blockId: 1, active: false }] },
        { storedStatus: "MICROTOMY" },
      ),
    ).toEqual({ key: "pathology.locked.microtomyIncomplete" });
    expect(
      advanceBlockedReason(
        intl,
        { slides: [{ blockId: 1, level: "" }] },
        { storedStatus: "MICROTOMY" },
      ),
    ).toEqual({ key: "pathology.locked.microtomyIncomplete" });
  });

  it("lets a case with one saved slide in use leave microtomy, one slide for the whole case", () => {
    expect(
      advanceBlockedReason(
        intl,
        {
          blocks: [
            { id: 1, cassetteState: "BLOCK" },
            { id: 2, cassetteState: "BLOCK" },
          ],
          slides: [{ id: 5, blockId: 1, active: true }],
          labelVerifications: { 5: { verifiedAt: "x", verifiedBy: "y" } },
        },
        { storedStatus: "MICROTOMY" },
      ),
    ).toBe(null);
  });

  it("says to save first at microtomy too, before the slide rule", () => {
    expect(
      advanceBlockedReason(
        intl,
        { slides: [] },
        { storedStatus: "MICROTOMY", dirty: true },
      ),
    ).toEqual({ key: "pathology.locked.saveBeforeTransition" });
  });

  describe("at microtomy, the slide labels (FR-7.5)", () => {
    const blocks = [
      { id: 1, designation: "A1", cassetteState: "BLOCK" },
      { id: 2, designation: "A2", cassetteState: "BLOCK" },
    ];
    const slides = [
      { id: 5, designation: "1", blockId: 1, active: true },
      { id: 6, designation: "2", blockId: 1, active: true },
      { id: 7, designation: "1", blockId: 2, active: true },
    ];
    const verified = (...ids) =>
      Object.fromEntries(
        ids.map((id) => [id, { verifiedAt: "x", verifiedBy: "y" }]),
      );
    const unverifiedReason = (outstanding) => ({
      key: "pathology.locked.slidesUnverified",
      values: { outstanding },
    });

    // In flight, unsaved work, no slide, then the labels: each row of the
    // order on a case that would also fail every later rule.
    it.each([
      [
        "a request in flight first",
        { slides },
        { saving: true, dirty: true },
        { key: "pathology.locked.requestInFlight" },
      ],
      [
        "unsaved work next",
        { slides },
        { dirty: true },
        { key: "pathology.locked.saveBeforeTransition" },
      ],
      [
        "no slide in use before any label",
        { slides: [{ id: 5, blockId: 1, active: false }] },
        {},
        { key: "pathology.locked.microtomyIncomplete" },
      ],
      [
        "every unchecked slide, named under its block, in list order",
        { slides, labelVerifications: verified(6) },
        {},
        unverifiedReason("A1.1, A2.1 outstanding"),
      ],
      [
        "nothing once every label is checked",
        { slides, labelVerifications: verified(5, 6, 7) },
        {},
        null,
      ],
    ])("says %s", (_, caseInfo, flags, expected) => {
      expect(
        advanceBlockedReason(
          intl,
          { blocks, ...caseInfo },
          { storedStatus: "MICROTOMY", ...flags },
        ),
      ).toEqual(expected);
    });

    // A case of many blocks would otherwise put every slide in one title.
    it("names the first two unchecked slides and counts the rest, the way the rail names cassettes", () => {
      const many = [1, 2, 3, 4].map((n) => ({
        id: 10 + n,
        designation: String(n),
        blockId: 1,
        active: true,
      }));
      expect(
        advanceBlockedReason(
          intl,
          { blocks, slides: many },
          { storedStatus: "MICROTOMY" },
        ),
      ).toEqual(
        unverifiedReason(
          intl.formatMessage(
            { id: "pathology.badge.outstandingMore" },
            { designations: "A1.1, A1.2", count: 2 },
          ),
        ),
      );
    });

    it("leaves out a retired slide, an unsaved one and one with no block on record", () => {
      expect(
        advanceBlockedReason(
          intl,
          {
            blocks,
            slides: [
              { id: 5, designation: "1", blockId: 1, active: true },
              { id: 6, designation: "2", blockId: 1, active: false },
              { designation: "", blockId: 1, level: "" },
              { id: 8, designation: "7", blockId: null, active: true },
            ],
            labelVerifications: verified(5),
          },
          { storedStatus: "MICROTOMY" },
        ),
      ).toBe(null);
    });

    it("holds no other stage for an unchecked label", () => {
      // Stained, so staining's own rule has nothing to say about them.
      const stained = slides.map((slide) => ({
        ...slide,
        stainStatus: "STAINED",
      }));
      expect(
        advanceBlockedReason(
          intl,
          { blocks, slides: stained },
          { storedStatus: "STAINING" },
        ),
      ).toBe(null);
    });
  });

  it("judges each rule on the stored stage only", () => {
    expect(
      advanceBlockedReason(
        intl,
        { blocks: [] },
        { storedStatus: "PROCESSING" },
      ),
    ).toBe(null);
    expect(
      advanceBlockedReason(
        intl,
        { grossExam: "", slides: [] },
        { storedStatus: "PROCESSING" },
      ),
    ).toBe(null);
  });
});

describe("returnBlockedReason", () => {
  it("says a signed-out case cannot be changed, before anything else", () => {
    expect(
      returnBlockedReason({
        storedStatus: "COMPLETED",
        previousStage: null,
        dirty: true,
      }),
    ).toEqual({ key: "pathology.locked.caseCompleted" });
  });

  it("says there is nothing to return to before the first stage", () => {
    expect(
      returnBlockedReason({ storedStatus: "ACCESSIONED", previousStage: null }),
    ).toEqual({ key: "pathology.locked.nothingToReturnTo" });
  });

  it("says to wait while a save or a move is out, and to save first while the form holds unsaved work", () => {
    const clean = { storedStatus: "PROCESSING", previousStage: "GROSSING" };
    expect(
      returnBlockedReason({ ...clean, saving: true, dirty: true }),
    ).toEqual({ key: "pathology.locked.requestInFlight" });
    expect(returnBlockedReason({ ...clean, moving: true })).toEqual({
      key: "pathology.locked.requestInFlight",
    });
    expect(returnBlockedReason({ ...clean, dirty: true })).toEqual({
      key: "pathology.locked.saveBeforeTransition",
    });
  });

  it("lets a clean case with an earlier stage be returned", () => {
    expect(
      returnBlockedReason({
        storedStatus: "PROCESSING",
        previousStage: "GROSSING",
      }),
    ).toBe(null);
  });
});

describe("rowActionLock", () => {
  const saveFirstKey = "pathology.locked.saveBeforeEmbed";

  it.each([
    ["a clean case standing still", {}, false, null],
    [
      "a signed-out case, before anything else",
      { status: "COMPLETED", dirty: true, moving: true },
      true,
      "pathology.locked.caseCompleted",
    ],
    [
      "a save in flight",
      { saving: true, dirty: true },
      true,
      "pathology.locked.requestInFlight",
    ],
    [
      "a move in flight",
      { moving: true },
      true,
      "pathology.locked.requestInFlight",
    ],
    [
      "an embed in flight",
      { embedding: true },
      true,
      "pathology.locked.requestInFlight",
    ],
    ["unsaved work", { dirty: true }, true, saveFirstKey],
  ])("locks a row action on %s", (_, state, disabled, reasonKey) => {
    expect(
      rowActionLock({ status: "EMBEDDING", ...state, saveFirstKey }),
    ).toEqual({ disabled, reasonKey });
  });
});

describe("verifyLabelLock", () => {
  const slide = { id: 6, designation: "2", blockId: 1, active: true };
  const stamp = { verifiedAt: "x", verifiedBy: "y" };
  const offeredFree = { offered: true, disabled: false, reasonKey: null };
  const notOffered = { offered: false, disabled: true, reasonKey: null };
  const inFlight = {
    offered: true,
    disabled: true,
    reasonKey: "pathology.locked.requestInFlight",
  };

  it.each([
    ["an unchecked saved slide on a clean case", {}, offeredFree],
    [
      "unsaved work, which a check does not touch",
      { dirty: true },
      offeredFree,
    ],
    ["a save in flight", { saving: true }, inFlight],
    ["a move in flight", { moving: true }, inFlight],
    ["the row's own check in flight", { verifying: true }, inFlight],
    [
      "a slide already verified",
      { labelVerifications: { 6: stamp } },
      notOffered,
    ],
    [
      "a signed-out case, before any request",
      { status: "COMPLETED", saving: true },
      notOffered,
    ],
    ["a section shown only for reading", { readOnly: true }, notOffered],
    [
      "a stage the laboratory stopped tracking",
      { untracked: true },
      notOffered,
    ],
    ["a retired slide", { slide: { ...slide, active: false } }, notOffered],
    [
      "an unsaved slide",
      { slide: { designation: "", blockId: 1 } },
      notOffered,
    ],
    [
      "a slide with no block on record",
      { slide: { ...slide, blockId: null } },
      notOffered,
    ],
  ])("on %s", (_, state, expected) => {
    expect(
      verifyLabelLock({
        slide,
        labelVerifications: {},
        status: "MICROTOMY",
        ...state,
      }),
    ).toEqual(expected);
  });
});

describe("addSlideLock", () => {
  const block = { id: 1, designation: "A1", cassetteState: "BLOCK" };
  const cassette = { id: 2, designation: "A2", cassetteState: "CASSETTE" };
  const embeddingOff = { PATHOLOGY_STAGE_EMBEDDING_ENABLED: "false" };

  it("lets a slide be added under a block", () => {
    expect(addSlideLock({ status: "MICROTOMY", block })).toEqual({
      disabled: false,
      reasonKey: null,
    });
  });

  it("holds a cassette back until it is embedded, where the laboratory tracks embedding", () => {
    expect(addSlideLock({ status: "MICROTOMY", block: cassette })).toEqual({
      disabled: true,
      reasonKey: "pathology.locked.embedBeforeSlides",
    });
    // A row with no state is a cassette, as the server reads it.
    expect(
      addSlideLock({ status: "MICROTOMY", block: { id: 3 } }).reasonKey,
    ).toBe("pathology.locked.embedBeforeSlides");
  });

  it("offers every row in use once the laboratory has stopped tracking embedding", () => {
    expect(
      addSlideLock({
        status: "MICROTOMY",
        block: cassette,
        configurationProperties: embeddingOff,
      }),
    ).toEqual({ disabled: false, reasonKey: null });
  });

  it("holds every row back on a signed-out case, before the embedding rule", () => {
    [block, cassette].forEach((row) =>
      expect(addSlideLock({ status: "COMPLETED", block: row })).toEqual({
        disabled: true,
        reasonKey: "pathology.locked.caseCompleted",
      }),
    );
  });
});

// 12.5 item 1: a stage switched off used to hide the slides already cut there
// behind a locked section, whose body is never shown.
describe("a stage the laboratory stopped tracking, with rows on record", () => {
  const MICROTOMY = "pathology-section-microtomy";
  const microtomyOff = { PATHOLOGY_STAGE_MICROTOMY_ENABLED: "false" };
  const derive = (status, configurationProperties, hasRows) =>
    deriveCaseSections({
      intl,
      status,
      isPathologist: false,
      configurationProperties,
      decalcificationRequired: true,
      sectionHasRows: (section) => hasRows && section.id === MICROTOMY,
    });

  it("shows the section read-only and still not applicable, with a badge saying it is not tracked", () => {
    const sections = derive("STAINING", microtomyOff, true);
    const microtomy = byId(sections, MICROTOMY);

    expect(microtomy.state).toBe(SECTION_STATE.READ_ONLY);
    expect(microtomy.notApplicable).toBe(true);
    expect(microtomy.readOnlyBecauseUntracked).toBe(true);
    expect(microtomy.lockedHintKey).toBeUndefined();
    expect(
      sectionBadge(microtomy, { caseInfo: { slides: [{ id: "1" }] } }),
    ).toEqual({ kind: "none", textKey: "pathology.locked.stageDisabled" });

    const railItem = deriveRailItems(sections, {
      intl,
      status: "STAINING",
      reportCount: 0,
      openRequestNames: [],
      caseInfo: { slides: [{ id: "1" }] },
    }).find((item) => item.id === MICROTOMY);
    expect(railItem.notApplicable).toBe(true);
    expect(railItem.complete).toBe(false);
  });

  it("keeps the section locked as before when it holds no rows", () => {
    const microtomy = byId(derive("STAINING", microtomyOff, false), MICROTOMY);

    expect(microtomy.state).toBe(SECTION_STATE.DISABLED);
    expect(microtomy.lockedHintKey).toBe("pathology.locked.stageDisabled");
    expect(microtomy.notApplicable).toBe(true);
    expect(microtomy.readOnlyBecauseUntracked).toBeUndefined();
  });

  it("changes nothing for a section whose stage the laboratory tracks", () => {
    const microtomy = byId(derive("STAINING", {}, true), MICROTOMY);

    expect(microtomy.state).toBe(SECTION_STATE.COMPLETE);
    expect(microtomy.notApplicable).toBe(false);
    expect(microtomy.readOnlyBecauseUntracked).toBeUndefined();
  });

  it("asks no caller for rows it does not know about, and locks every other switched-off stage as before", () => {
    const sections = deriveCaseSections({
      intl,
      status: "STAINING",
      isPathologist: false,
      configurationProperties: microtomyOff,
      decalcificationRequired: true,
    });
    expect(byId(sections, MICROTOMY).state).toBe(SECTION_STATE.DISABLED);

    const coverslipping = byId(
      derive(
        "STAINING",
        { PATHOLOGY_STAGE_COVERSLIPPING_ENABLED: "false" },
        true,
      ),
      "pathology-section-coverslipping",
    );
    expect(coverslipping.state).toBe(SECTION_STATE.DISABLED);
  });
});

describe("sectionIdOfStage", () => {
  it("finds the section each stage is worked in, and none for a stage this build does not know", () => {
    expect(sectionIdOfStage("PROCESSING")).toBe("pathology-section-processing");
    expect(sectionIdOfStage("UNDER_REVIEW")).toBe("pathology-section-review");
    expect(sectionIdOfStage("COMPLETED")).toBe("pathology-section-reports");
    expect(sectionIdOfStage("SOMETHING_NEW")).toBeNull();
  });
});

// FR-8: staining and coverslipping, read from the slides. Three slides on
// two blocks, so every rule below names them block first ("A1.1").
const slideWorkBlocks = [
  { id: 1, designation: "A1", cassetteState: "BLOCK", active: true },
  { id: 2, designation: "A2", cassetteState: "BLOCK", active: true },
];
const workSlide = (id, designation, blockId, overrides = {}) => ({
  id,
  designation,
  blockId,
  stainId: "31",
  active: true,
  ...overrides,
});

describe("advanceBlockedReason at staining and coverslipping (FR-8)", () => {
  const atStaining = (slides, options = {}) =>
    advanceBlockedReason(
      intl,
      { blocks: slideWorkBlocks, slides },
      { storedStatus: "STAINING", ...options },
    );
  const atCoverslipping = (slides, options = {}) =>
    advanceBlockedReason(
      intl,
      { blocks: slideWorkBlocks, slides },
      { storedStatus: "COVERSLIPPING", ...options },
    );

  it("holds staining back until every slide in use is stained, naming the ones that are not", () => {
    const blocked = atStaining([
      workSlide(5, "1", 1, { stainStatus: "STAINED" }),
      workSlide(6, "2", 1),
      workSlide(7, "1", 2, { stainStatus: "UNSATISFACTORY" }),
    ]);

    expect(blocked.key).toBe("pathology.locked.stainingIncomplete");
    expect(intl.formatMessage({ id: blocked.key }, blocked.values)).toContain(
      "A1.2, A2.1",
    );
  });

  it("names the slides that hold the primary in the order the case lists them", () => {
    const blocked = atCoverslipping([
      workSlide(7, "1", 2, { stainStatus: "STAINED" }),
      workSlide(5, "1", 1, { stainStatus: "STAINED" }),
    ]);

    expect(intl.formatMessage({ id: blocked.key }, blocked.values)).toContain(
      "A2.1, A1.1",
    );
  });

  it("lets a case with every slide in use stained leave staining", () => {
    expect(
      atStaining([
        workSlide(5, "1", 1, { stainStatus: "STAINED" }),
        workSlide(6, "2", 1, { active: false }),
      ]),
    ).toBe(null);
  });

  it("holds either stage with no slide in use as microtomy holds it, so retiring every slide does not open the gate", () => {
    const retired = [workSlide(5, "1", 1, { active: false })];

    expect(atStaining(retired)).toEqual({
      key: "pathology.locked.microtomyIncomplete",
    });
    expect(atCoverslipping([])).toEqual({
      key: "pathology.locked.microtomyIncomplete",
    });
  });

  it("holds coverslipping back until every slide passed QC, naming a failed and an unchecked one", () => {
    const blocked = atCoverslipping([
      workSlide(5, "1", 1, { stainStatus: "STAINED", coverslipQc: "PASS" }),
      workSlide(6, "2", 1, { stainStatus: "STAINED", coverslipQc: "FAIL" }),
      workSlide(7, "1", 2, { stainStatus: "STAINED" }),
    ]);

    expect(blocked.key).toBe("pathology.locked.coverslipQcIncomplete");
    expect(intl.formatMessage({ id: blocked.key }, blocked.values)).toContain(
      "A1.2, A2.1",
    );
  });

  it("asks a passed slide to read Stained too, unless the laboratory does not track staining", () => {
    const passedUnstained = [workSlide(5, "1", 1, { coverslipQc: "PASS" })];

    expect(atCoverslipping(passedUnstained)?.key).toBe(
      "pathology.locked.coverslipQcIncomplete",
    );
    expect(
      atCoverslipping(passedUnstained, {
        configurationProperties: {
          PATHOLOGY_STAGE_STAINING_ENABLED: "false",
        },
      }),
    ).toBe(null);
  });

  it("says a request in flight and then unsaved work before the slides", () => {
    const pending = [workSlide(5, "1", 1)];

    expect(atStaining(pending, { moving: true }).key).toBe(
      "pathology.locked.requestInFlight",
    );
    expect(atStaining(pending, { dirty: true }).key).toBe(
      "pathology.locked.saveBeforeTransition",
    );
  });

  it("holds no other stage for an unstained or unchecked slide", () => {
    expect(
      advanceBlockedReason(
        intl,
        { blocks: slideWorkBlocks, slides: [workSlide(5, "1", 1)] },
        { storedStatus: "PROCESSING" },
      ),
    ).toBe(null);
  });
});

describe("slideWorkLock", () => {
  const stained = workSlide(5, "1", 1, { stainStatus: "STAINED" });
  const pending = workSlide(6, "2", 1);
  const unstainedNoStain = workSlide(7, "3", 1, { stainId: null });

  it.each([
    [{ status: "COMPLETED" }, "pathology.locked.caseCompleted"],
    [{ status: "READY_PATHOLOGIST" }, "pathology.locked.slideWorkAfterBench"],
    [{ status: "UNDER_REVIEW" }, "pathology.locked.slideWorkAfterBench"],
    [{ status: "STAINING", saving: true }, "pathology.locked.requestInFlight"],
    [{ status: "STAINING", moving: true }, "pathology.locked.requestInFlight"],
    [{ status: "STAINING", dirty: true }, "pathology.locked.saveBeforeMarking"],
  ])("holds the whole section with %o and says why", (state, reasonKey) => {
    const lock = { disabled: true, reasonKey, own: false };
    expect(
      slideWorkLock({ ...state, action: "stained", slide: pending }),
    ).toEqual(lock);
    expect(slideWorkLock(state)).toEqual(lock);
  });

  it("takes marks at staining and coverslipping, the stages the bench still owes them", () => {
    ["STAINING", "COVERSLIPPING"].forEach((status) => {
      expect(
        slideWorkLock({ status, action: "unsatisfactory", slide: pending }),
      ).toEqual({ disabled: false, reasonKey: null, own: false });
    });
  });

  it("says past the bench before unsaved work, since saving does not change it", () => {
    expect(
      slideWorkLock({ status: "READY_PATHOLOGIST", dirty: true }).reasonKey,
    ).toBe("pathology.locked.slideWorkAfterBench");
  });

  it("holds Mark stained on a slide with no stain saved as the row's own reason, and nothing else on it", () => {
    expect(
      slideWorkLock({
        status: "STAINING",
        action: "stained",
        slide: unstainedNoStain,
      }),
    ).toEqual({
      disabled: true,
      reasonKey: "pathology.locked.stainMissing",
      own: true,
    });
    expect(
      slideWorkLock({
        status: "STAINING",
        action: "unsatisfactory",
        slide: unstainedNoStain,
      }).disabled,
    ).toBe(false);
  });

  it("holds Pass and Fail on an unstained slide where staining is tracked, and not where it is not", () => {
    ["pass", "fail"].forEach((action) => {
      expect(
        slideWorkLock({ status: "COVERSLIPPING", action, slide: pending }),
      ).toEqual({
        disabled: true,
        reasonKey: "pathology.locked.qcBeforeStain",
        own: true,
      });
      expect(
        slideWorkLock({
          status: "COVERSLIPPING",
          action,
          slide: pending,
          stainingTracked: false,
        }).disabled,
      ).toBe(false);
      expect(
        slideWorkLock({ status: "COVERSLIPPING", action, slide: stained })
          .disabled,
      ).toBe(false);
    });
  });

  it("says the section's reason before the slide's own", () => {
    expect(
      slideWorkLock({
        status: "STAINING",
        dirty: true,
        action: "stained",
        slide: unstainedNoStain,
      }).reasonKey,
    ).toBe("pathology.locked.saveBeforeMarking");
  });
});

describe("batchHold", () => {
  const stained = workSlide(5, "1", 1, { stainStatus: "STAINED" });
  const pending = workSlide(6, "2", 1);
  const unstainedNoStain = workSlide(7, "1", 2, { stainId: null });

  it("names the selected slides a batch cannot be applied to, by the row's own rule", () => {
    expect(
      batchHold(
        slideWorkBlocks,
        [stained, pending, unstainedNoStain],
        "stained",
      ),
    ).toEqual({
      key: "pathology.locked.stainMissingFor",
      values: { designations: "A2.1" },
    });
    expect(batchHold(slideWorkBlocks, [stained, pending], "pass")).toEqual({
      key: "pathology.locked.qcBeforeStainFor",
      values: { designations: "A1.2" },
    });
  });

  it("holds nothing when every selected slide can take the batch", () => {
    expect(batchHold(slideWorkBlocks, [stained, pending], "stained")).toBe(
      null,
    );
    expect(batchHold(slideWorkBlocks, [stained, pending], "pass", false)).toBe(
      null,
    );
  });
});

describe("redoNotice", () => {
  const slides = [
    workSlide(5, "1", 1, { stainStatus: "UNSATISFACTORY" }),
    workSlide(6, "2", 1, { stainStatus: "STAINED", coverslipQc: "FAIL" }),
    workSlide(7, "1", 2, { stainStatus: "UNSATISFACTORY", active: false }),
  ];

  it("names the slides its own stage's rule finds broken, and says they hold the case where the gate of the case's stage waits for them", () => {
    expect(redoNotice("STAINING", slideWorkBlocks, slides, "STAINING")).toEqual(
      {
        titleKey: "pathology.banner.unsatisfactorySlidesHere",
        values: { designations: "A1.1" },
        recipeKey: "pathology.banner.restainOrRecut",
      },
    );
    expect(
      redoNotice("COVERSLIPPING", slideWorkBlocks, slides, "COVERSLIPPING"),
    ).toEqual({
      titleKey: "pathology.banner.failedQcSlidesHere",
      values: { designations: "A1.2" },
      recipeKey: "pathology.banner.recoverslipOrRecut",
    });
  });

  it("says an unsatisfactory slide holds a case at coverslipping too, where the coverslip gate asks for a stained slide", () => {
    expect(
      redoNotice("STAINING", slideWorkBlocks, slides, "COVERSLIPPING").titleKey,
    ).toBe("pathology.banner.unsatisfactorySlidesHere");
    // With staining untracked the coverslip gate asks only for a pass, so a
    // passed slide holds nothing there, whatever its stain status.
    const passed = [
      workSlide(5, "1", 1, {
        stainStatus: "UNSATISFACTORY",
        coverslipQc: "PASS",
      }),
    ];
    expect(
      redoNotice("STAINING", slideWorkBlocks, passed, "COVERSLIPPING", false)
        .titleKey,
    ).toBe("pathology.banner.unsatisfactorySlides");
    expect(
      redoNotice("STAINING", slideWorkBlocks, passed, "COVERSLIPPING").titleKey,
    ).toBe("pathology.banner.unsatisfactorySlidesHere");
  });

  it("does not say a failed coverslip holds a case returned to staining, whose gate asks only for a stain", () => {
    expect(
      redoNotice("COVERSLIPPING", slideWorkBlocks, slides, "STAINING").titleKey,
    ).toBe("pathology.banner.failedQcSlides");
  });

  it("names the broken slides in the order the case lists them", () => {
    const listed = [
      workSlide(7, "1", 2, { stainStatus: "UNSATISFACTORY" }),
      workSlide(5, "1", 1, { stainStatus: "UNSATISFACTORY" }),
    ];
    expect(
      redoNotice("STAINING", slideWorkBlocks, listed, "STAINING").values,
    ).toEqual({ designations: "A2.1, A1.1" });
  });

  it("gives no way back once the case has left the bench, and no notice with nothing broken", () => {
    expect(
      redoNotice("STAINING", slideWorkBlocks, slides, "READY_PATHOLOGIST"),
    ).toEqual({
      titleKey: "pathology.banner.unsatisfactorySlides",
      values: { designations: "A1.1" },
      recipeKey: null,
    });
    expect(
      redoNotice("COVERSLIPPING", slideWorkBlocks, [slides[0]], "STAINING"),
    ).toBe(null);
  });
});

describe("the staining and coverslipping badges, rail steps and summary lines", () => {
  const slides = [
    workSlide(5, "1", 1, { stainStatus: "STAINED", coverslipQc: "PASS" }),
    workSlide(6, "2", 1, { stainStatus: "STAINED" }),
    workSlide(7, "1", 2),
  ];
  const caseAt = (status, rows = slides) => ({
    status,
    grossExam: "Described",
    blocks: slideWorkBlocks,
    slides: rows,
  });
  const nothingRecorded = [workSlide(5, "1", 1), workSlide(6, "2", 1)];
  const sectionsAt = (status, configurationProperties) =>
    deriveCaseSections({
      intl,
      status,
      isPathologist: false,
      decalcificationRequired: true,
      configurationProperties,
    });
  const railAt = (status, rows) =>
    deriveRailItems(sectionsAt(status), {
      intl,
      status,
      reportCount: 0,
      openRequestNames: [],
      caseInfo: caseAt(status, rows),
    });
  const item = (items, id) => items.find((entry) => entry.id === id);

  it("badges each section with how many slides are done, and Complete once all are", () => {
    const sections = sectionsAt("STAINING");

    expect(
      sectionBadge(byId(sections, STAINING_SECTION_ID), {
        caseInfo: caseAt("STAINING"),
      }),
    ).toEqual({
      kind: "partial",
      textKey: "pathology.badge.stainedOf",
      values: { stained: 2, total: 3 },
    });
    expect(
      sectionBadge(byId(sections, COVERSLIPPING_SECTION_ID), {
        caseInfo: caseAt("STAINING"),
      }),
    ).toEqual({
      kind: "partial",
      textKey: "pathology.badge.qcPassedOf",
      values: { passed: 1, total: 3 },
    });
    expect(
      sectionBadge(byId(sections, STAINING_SECTION_ID), {
        caseInfo: caseAt("STAINING", [slides[0]]),
      }),
    ).toEqual({ kind: "complete", textKey: "common.complete" });
    expect(
      sectionBadge(byId(sections, STAINING_SECTION_ID), {
        caseInfo: caseAt("STAINING", []),
      }),
    ).toBeNull();
  });

  it("shows no count on a case that left the bench with nothing recorded, and reads it Not recorded in the summary", () => {
    ["READY_PATHOLOGIST", "COMPLETED"].forEach((status) => {
      const sections = sectionsAt(status);
      expect(
        sectionBadge(byId(sections, STAINING_SECTION_ID), {
          caseInfo: caseAt(status, nothingRecorded),
        }),
      ).toBeNull();
      expect(
        slideWorkStatement(intl, {
          stage: "COVERSLIPPING",
          summary: coverslipSummary(slideWorkBlocks, nothingRecorded),
          status,
        }).value,
      ).toBe(messages["common.notRecorded"]);
    });
  });

  it("keeps counting at the bench even with nothing recorded, since the bench still owes the work", () => {
    expect(
      slideWorkReading(
        stainingSummary(slideWorkBlocks, nothingRecorded),
        "COVERSLIPPING",
      ),
    ).toBe("count");
    expect(
      slideWorkReading(
        stainingSummary(slideWorkBlocks, nothingRecorded),
        "READY_PATHOLOGIST",
      ),
    ).toBe("notRecorded");
    expect(
      slideWorkReading(stainingSummary(slideWorkBlocks, []), "STAINING"),
    ).toBe("none");
  });

  it("names the outstanding slides on the rail once the case stands at the stage, and leaves the step not complete", () => {
    const items = railAt("STAINING", slides);
    const staining = item(items, STAINING_SECTION_ID);

    expect(staining.complete).toBe(false);
    expect(staining.pendingTitle).toBe(
      intl.formatMessage(
        { id: "pathology.badge.outstanding" },
        { designations: "A2.1" },
      ),
    );
    expect(item(items, COVERSLIPPING_SECTION_ID).pendingLabel).toBeUndefined();
  });

  it("keeps the staining step open and named at coverslipping while a slide is still unstained", () => {
    const staining = item(railAt("COVERSLIPPING", slides), STAINING_SECTION_ID);

    expect(staining.complete).toBe(false);
    expect(staining.pendingTitle).toContain("A2.1");
  });

  it("ticks both steps by passage once the case has left the bench, recorded or not, and names nothing", () => {
    [slides, nothingRecorded].forEach((rows) => {
      const items = railAt("READY_PATHOLOGIST", rows);
      [STAINING_SECTION_ID, COVERSLIPPING_SECTION_ID].forEach((id) => {
        expect(item(items, id).complete).toBe(true);
        expect(item(items, id).pendingLabel).toBeUndefined();
      });
    });
  });

  it("reads the summary line as a count, names the outstanding on hover at the stage, and N/A with the stage switched off", () => {
    const staining = slideWorkStatement(intl, {
      stage: "STAINING",
      summary: stainingSummary(slideWorkBlocks, slides),
      status: "STAINING",
    });
    expect(staining.value).toBe(
      intl.formatMessage(
        { id: "pathology.badge.stainedOf" },
        { stained: 2, total: 3 },
      ),
    );
    expect(staining.title).toContain("A2.1");

    const earlier = slideWorkStatement(intl, {
      stage: "COVERSLIPPING",
      summary: coverslipSummary(slideWorkBlocks, slides, {
        stainingTracked: true,
      }),
      status: "STAINING",
    });
    expect(earlier.title).toBeUndefined();

    expect(
      slideWorkStatement(intl, {
        stage: "COVERSLIPPING",
        summary: coverslipSummary(slideWorkBlocks, slides),
        status: "STAINING",
        configurationProperties: {
          PATHOLOGY_STAGE_COVERSLIPPING_ENABLED: "false",
        },
      }).value,
    ).toBe(messages["caseView.badge.notApplicable"]);
    expect(
      slideWorkStatement(intl, {
        stage: "STAINING",
        summary: stainingSummary([], []),
        status: "STAINING",
      }).value,
    ).toBeNull();
  });

  it("reads whether staining is tracked once, for both summaries", () => {
    const off = { PATHOLOGY_STAGE_STAINING_ENABLED: "false" };
    const passedUnstained = {
      blocks: slideWorkBlocks,
      slides: [workSlide(5, "1", 1, { coverslipQc: "PASS" })],
    };

    expect(slideWorkSummaries(passedUnstained, off).stainingTracked).toBe(
      false,
    );
    expect(
      slideWorkSummaries(passedUnstained, off).COVERSLIPPING.complete,
    ).toBe(true);
    expect(slideWorkSummaries(passedUnstained, {}).COVERSLIPPING.complete).toBe(
      false,
    );
  });
});

describe("sectionHoldsRecords, for a stage switched off with work on record", () => {
  it("counts any slide for microtomy, a stain status for staining and a QC result for coverslipping", () => {
    const plain = { slides: [{ id: 5, active: false }] };
    const stained = { slides: [{ id: 5, stainStatus: "UNSATISFACTORY" }] };
    const checked = { slides: [{ id: 5, coverslipQc: "FAIL" }] };

    expect(sectionHoldsRecords("pathology-section-microtomy", plain)).toBe(
      true,
    );
    expect(sectionHoldsRecords(STAINING_SECTION_ID, plain)).toBe(false);
    expect(sectionHoldsRecords(STAINING_SECTION_ID, stained)).toBe(true);
    expect(sectionHoldsRecords(COVERSLIPPING_SECTION_ID, stained)).toBe(false);
    expect(sectionHoldsRecords(COVERSLIPPING_SECTION_ID, checked)).toBe(true);
    expect(sectionHoldsRecords("pathology-section-grossing", checked)).toBe(
      false,
    );
    expect(sectionHoldsRecords(STAINING_SECTION_ID, undefined)).toBe(false);
  });

  it("shows staining read-only and not tracked when the stage is off and a slide carries a status, and locked otherwise", () => {
    const off = { PATHOLOGY_STAGE_STAINING_ENABLED: "false" };
    const derive = (caseInfo) =>
      byId(
        deriveCaseSections({
          intl,
          status: "COVERSLIPPING",
          isPathologist: false,
          configurationProperties: off,
          decalcificationRequired: true,
          sectionHasRows: (section) =>
            sectionHoldsRecords(section.id, caseInfo),
        }),
        STAINING_SECTION_ID,
      );

    const withRecords = derive({ slides: [{ id: 5, stainStatus: "STAINED" }] });
    expect(withRecords.state).toBe(SECTION_STATE.READ_ONLY);
    expect(withRecords.readOnlyBecauseUntracked).toBe(true);

    const without = derive({ slides: [{ id: 5 }] });
    expect(without.state).toBe(SECTION_STATE.DISABLED);
    expect(without.lockedHintKey).toBe("pathology.locked.stageDisabled");
  });
});

// Review section state (FR-10.12): one row of the table per test. The bench
// works the requests from a read-only review, so "bench mode" is read-only
// fields with the table's status actions live, and only the pathologist's
// open review offers a new request.
describe("the review section's state, by stage, requests on the case and viewer", () => {
  const BENCH_STAGES = PATHOLOGY_STAGES.slice(
    0,
    PATHOLOGY_STAGES.indexOf("READY_PATHOLOGIST"),
  );
  const review = (status, requestCount, isPathologist) =>
    byId(
      deriveCaseSections({ intl, status, isPathologist, requestCount }),
      REVIEW_SECTION_ID,
    );

  it.each(
    BENCH_STAGES.flatMap((stage) => [
      [stage, true],
      [stage, false],
    ]),
  )(
    "stays locked at %s with no request on the case (pathologist: %s)",
    (status, isPathologist) => {
      const section = review(status, 0, isPathologist);

      expect(section.state).toBe(SECTION_STATE.DISABLED);
      expect(section.benchRequests).toBeFalsy();
    },
  );

  it.each(
    BENCH_STAGES.flatMap((stage) => [
      [stage, true],
      [stage, false],
    ]),
  )(
    "opens read-only in bench mode at %s once the case holds a request (pathologist: %s)",
    (status, isPathologist) => {
      const section = review(status, 1, isPathologist);

      expect(section.state).toBe(SECTION_STATE.READ_ONLY);
      expect(section.benchRequests).toBe(true);
      expect(section.lockedHintKey).toBeUndefined();
    },
  );

  it.each([
    ["READY_PATHOLOGIST", 0],
    ["READY_PATHOLOGIST", 2],
    ["UNDER_REVIEW", 0],
    ["UNDER_REVIEW", 2],
  ])(
    "is open for the pathologist at %s with %s requests on the case",
    (status, count) => {
      const section = review(status, count, true);

      expect(section.state).toBe(SECTION_STATE.OPEN);
      expect(section.benchRequests).toBeFalsy();
    },
  );

  it.each(["READY_PATHOLOGIST", "UNDER_REVIEW"])(
    "is in bench mode for anyone else at %s once the case holds a request",
    (status) => {
      const section = review(status, 2, false);

      expect(section.state).toBe(SECTION_STATE.READ_ONLY);
      expect(section.benchRequests).toBe(true);
    },
  );

  it.each(["READY_PATHOLOGIST", "UNDER_REVIEW"])(
    "is read-only with no actions for anyone else at %s with no request on the case",
    (status) => {
      const section = review(status, 0, false);

      expect(section.state).toBe(SECTION_STATE.READ_ONLY);
      expect(section.benchRequests).toBeFalsy();
    },
  );

  // The bench-mode rule does not fire on a signed-out case: a released case's
  // review is read-only for everyone (S8), and the case view offers no
  // request action there (requestActionLock and the signed-out case view
  // test).
  it.each([
    [0, true, SECTION_STATE.READ_ONLY],
    [2, true, SECTION_STATE.READ_ONLY],
    [0, false, SECTION_STATE.READ_ONLY],
    [2, false, SECTION_STATE.READ_ONLY],
  ])(
    "is read-only once signed out, with %s requests (pathologist: %s), and never in bench mode",
    (count, isPathologist, state) => {
      const section = review("COMPLETED", count, isPathologist);

      expect(section.state).toBe(state);
      expect(section.benchRequests).toBeFalsy();
    },
  );

  // FR-10.12: only the pathologist's open review offers New request, and
  // Microtomy's Request slide and Request stain follow the same rule.
  it.each([
    ["READY_PATHOLOGIST", 0, true, true],
    ["UNDER_REVIEW", 2, true, true],
    ["READY_PATHOLOGIST", 2, false, false],
    ["READY_PATHOLOGIST", 0, false, false],
    ["MICROTOMY", 2, true, false],
    ["MICROTOMY", 0, true, false],
    ["COMPLETED", 2, true, false],
    ["COMPLETED", 0, false, false],
  ])(
    "takes a new request at %s with %s requests (pathologist: %s): %s",
    (status, count, isPathologist, takes) => {
      expect(reviewTakesRequests(review(status, count, isPathologist))).toBe(
        takes,
      );
    },
  );

  it("takes no request on any section but the review", () => {
    const findings = byId(
      deriveCaseSections({
        intl,
        status: "READY_PATHOLOGIST",
        isPathologist: true,
        readingStage: "READY_PATHOLOGIST",
        findingsOpen: true,
      }),
      "pathology-section-findings",
    );

    expect(findings.state).toBe(SECTION_STATE.OPEN);
    expect(reviewTakesRequests(findings)).toBe(false);
  });

  // Fail-safe: a status this build cannot place is never read as a bench
  // stage, so a request on the case unlocks nothing there.
  it("stays locked at a status this build does not know, whatever the case holds", () => {
    expect(review("SOMETHING_NEW", 2, true).state).toBe(SECTION_STATE.DISABLED);
  });

  // The rule is the review's own: findings keeps the stage gate.
  it("leaves the findings section locked at a bench stage however many requests the case holds", () => {
    const findings = byId(
      deriveCaseSections({
        intl,
        status: "MICROTOMY",
        isPathologist: true,
        requestCount: 2,
      }),
      "pathology-section-findings",
    );

    expect(findings.state).toBe(SECTION_STATE.DISABLED);
  });
});

describe("requestActionLock", () => {
  it("holds every request action on a signed-out case first", () => {
    expect(
      requestActionLock({ status: "COMPLETED", dirty: true, moving: true }),
    ).toEqual({
      disabled: true,
      reasonKey: "pathology.locked.caseCompleted",
    });
  });

  it.each([
    ["a save", { saving: true }],
    ["a move, a mark or another request", { moving: true }],
  ])("holds them while %s is unanswered", (_label, flags) => {
    expect(
      requestActionLock({ status: "MICROTOMY", dirty: true, ...flags }),
    ).toEqual({
      disabled: true,
      reasonKey: "pathology.locked.requestInFlight",
    });
  });

  // The case is read again after every request action, which would replace
  // what was typed.
  it("holds them over unsaved work, saying to save or discard first", () => {
    expect(requestActionLock({ status: "MICROTOMY", dirty: true })).toEqual({
      disabled: true,
      reasonKey: "pathology.locked.saveBeforeRequest",
    });
  });

  it("lets them go on a clean, settled case", () => {
    expect(requestActionLock({ status: "READY_PATHOLOGIST" })).toEqual({
      disabled: false,
      reasonKey: null,
    });
  });
});

describe("requestDraftBlockedReason, the first unmet rule of a new request", () => {
  const draft = (overrides = {}) => ({
    kind: "SLIDE",
    targetId: "1",
    stainId: "",
    priority: "NORMAL",
    instruction: "Two deeper levels",
    ...overrides,
  });

  it("asks for an instruction first, before any target, as the server does", () => {
    expect(
      requestDraftBlockedReason(draft({ instruction: "   ", targetId: "" })),
    ).toEqual({ key: "pathology.locked.requestInstructionRequired" });
  });

  it.each(["SLIDE", "STAIN"])("asks for the target of a %s request", (kind) => {
    expect(requestDraftBlockedReason(draft({ kind, targetId: "" }))).toEqual({
      key: "pathology.locked.requestTargetRequired",
    });
  });

  it("asks for the stain of a stain request once its slide is picked", () => {
    expect(
      requestDraftBlockedReason(draft({ kind: "STAIN", stainId: "" })),
    ).toEqual({ key: "pathology.locked.requestStainRequired" });
  });

  it("asks a block request for neither a target nor a stain", () => {
    expect(
      requestDraftBlockedReason(draft({ kind: "BLOCK", targetId: "" })),
    ).toBeNull();
  });

  it("lets a complete slide or stain request go", () => {
    expect(requestDraftBlockedReason(draft())).toBeNull();
    expect(
      requestDraftBlockedReason(draft({ kind: "STAIN", stainId: "31" })),
    ).toBeNull();
  });
});

describe("requestDraftBody, what raising a request posts", () => {
  it("posts a slide request's block as its target and no stain", () => {
    expect(
      requestDraftBody({
        kind: "SLIDE",
        targetId: "4",
        stainId: "31",
        priority: "URGENT",
        instruction: "  Two deeper levels ",
      }),
    ).toEqual({
      kind: "SLIDE",
      targetBlockId: 4,
      targetSlideId: null,
      stainId: null,
      priority: "URGENT",
      instruction: "Two deeper levels",
    });
  });

  it("posts a stain request's slide as its target with the stain", () => {
    expect(
      requestDraftBody({
        kind: "STAIN",
        targetId: "7",
        stainId: "31",
        priority: "STAT",
        instruction: "PAS",
      }),
    ).toEqual({
      kind: "STAIN",
      targetBlockId: null,
      targetSlideId: 7,
      stainId: "31",
      priority: "STAT",
      instruction: "PAS",
    });
  });

  // A target picked for a slide request and left behind when the kind was
  // switched must not travel with the block request.
  it("posts no target and no stain for a block request, whatever was picked before the kind changed", () => {
    expect(
      requestDraftBody({
        kind: "BLOCK",
        targetId: "4",
        stainId: "31",
        priority: "NORMAL",
        instruction: "Another block",
      }),
    ).toEqual({
      kind: "BLOCK",
      targetBlockId: null,
      targetSlideId: null,
      stainId: null,
      priority: "NORMAL",
      instruction: "Another block",
    });
  });
});

describe("requestStatusBody, what a status change posts", () => {
  const request = (overrides = {}) => ({
    id: 5,
    kind: "SLIDE",
    status: "OPENED",
    ...overrides,
  });

  // `from` is the status the page showed, so a request changed in another
  // tab is refused rather than changed twice.
  it("names the status the page shows as the one it changes from", () => {
    expect(
      requestStatusBody(request({ status: "CANCELLED" }), "OPENED"),
    ).toEqual({
      from: "CANCELLED",
      status: "OPENED",
      cancelReason: null,
      resultBlockId: null,
      resultSlideId: null,
    });
  });

  it("posts the fulfilling slide of a slide or stain request, and the block of a block request", () => {
    expect(
      requestStatusBody(request(), "COMPLETED", { resultId: "9" }),
    ).toMatchObject({ resultBlockId: null, resultSlideId: 9 });
    expect(
      requestStatusBody(request({ kind: "STAIN" }), "COMPLETED", {
        resultId: "9",
      }),
    ).toMatchObject({ resultBlockId: null, resultSlideId: 9 });
    expect(
      requestStatusBody(request({ kind: "BLOCK" }), "COMPLETED", {
        resultId: "3",
      }),
    ).toMatchObject({ resultBlockId: 3, resultSlideId: null });
  });

  it("posts no result for a request on file with no kind", () => {
    expect(
      requestStatusBody(request({ kind: null }), "COMPLETED", {
        resultId: "9",
      }),
    ).toMatchObject({ resultBlockId: null, resultSlideId: null });
  });

  it("posts the cancellation's reason trimmed, and no reason with any other status", () => {
    expect(
      requestStatusBody(request(), "CANCELLED", {
        cancelReason: "  Tissue exhausted ",
      }),
    ).toMatchObject({ status: "CANCELLED", cancelReason: "Tissue exhausted" });
    expect(
      requestStatusBody(request(), "COMPLETED", { cancelReason: "x" }),
    ).toMatchObject({ cancelReason: null });
  });
});

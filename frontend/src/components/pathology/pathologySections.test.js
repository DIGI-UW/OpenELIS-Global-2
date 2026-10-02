import { createIntl } from "react-intl";
import messages from "../../languages/en.json";
import { PATHOLOGY_STAGES } from "./pathologyStages";
import { SECTION_STATE } from "../caseView/sectionState";
import {
  PATHOLOGY_SECTIONS,
  stageEnablementProperty,
  isStageEnabled,
  stageBadgeKind,
  sectionBadge,
  openRequestNames,
  deriveCaseSections,
  railCurrentIndex,
  deriveRailItems,
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

describe("stageEnablementProperty", () => {
  it("names the exact configuration key a stage is switched off with", () => {
    expect(stageEnablementProperty("COVERSLIPPING")).toBe(
      "PATHOLOGY_STAGE_COVERSLIPPING_ENABLED",
    );
  });
});

describe("deriveCaseSections at GROSSING for a technician", () => {
  const sections = deriveCaseSections({
    intl,
    status: "GROSSING",
    isPathologist: false,
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
  it("opens review and findings for a pathologist once the case reaches ready for pathologist", () => {
    const sections = deriveCaseSections({
      intl,
      status: "READY_PATHOLOGIST",
      isPathologist: true,
    });

    expect(byId(sections, "pathology-section-review").state).toBe(
      SECTION_STATE.OPEN,
    );
    expect(byId(sections, "pathology-section-findings").state).toBe(
      SECTION_STATE.OPEN,
    );
  });

  it("leaves the same two sections read-only, not disabled, for anyone who is not the pathologist", () => {
    const sections = deriveCaseSections({
      intl,
      status: "READY_PATHOLOGIST",
      isPathologist: false,
    });

    expect(byId(sections, "pathology-section-review").state).toBe(
      SECTION_STATE.READ_ONLY,
    );
    expect(byId(sections, "pathology-section-findings").state).toBe(
      SECTION_STATE.READ_ONLY,
    );
  });

  it("keeps review and findings open, not complete, while a case past ready for pathologist is still under review", () => {
    const sections = deriveCaseSections({
      intl,
      status: "UNDER_REVIEW",
      isPathologist: true,
    });

    expect(byId(sections, "pathology-section-review").state).toBe(
      SECTION_STATE.OPEN,
    );
    expect(byId(sections, "pathology-section-findings").state).toBe(
      SECTION_STATE.OPEN,
    );
    expect(byId(sections, "pathology-section-grossing").state).toBe(
      SECTION_STATE.COMPLETE,
    );
  });

  it("marks review, findings and grossing complete for a pathologist once the case is complete", () => {
    const sections = deriveCaseSections({
      intl,
      status: "COMPLETED",
      isPathologist: true,
    });

    expect(byId(sections, "pathology-section-review").state).toBe(
      SECTION_STATE.COMPLETE,
    );
    expect(byId(sections, "pathology-section-findings").state).toBe(
      SECTION_STATE.COMPLETE,
    );
    expect(byId(sections, "pathology-section-grossing").state).toBe(
      SECTION_STATE.COMPLETE,
    );
  });

  it("leaves review and findings read-only, not complete, for a technician once the case is complete", () => {
    const sections = deriveCaseSections({
      intl,
      status: "COMPLETED",
      isPathologist: false,
    });

    expect(byId(sections, "pathology-section-review").state).toBe(
      SECTION_STATE.READ_ONLY,
    );
    expect(byId(sections, "pathology-section-findings").state).toBe(
      SECTION_STATE.READ_ONLY,
    );
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
    expect(coverslipping.stageDisabled).toBe(true);

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
    expect(coverslipping.stageDisabled).toBe(false);

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
  const grossed = { grossExam: "Firm tan nodule, 20mm", blocks: [{ id: "1" }] };

  it("marks each section complete once the case has moved past every stage it spans, and leaves the current and later ones not complete", () => {
    const sections = deriveCaseSections({
      intl,
      status: "MICROTOMY",
      isPathologist: true,
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
      { id: 1, value: "Recut", status: "OPENED" },
      { id: 2, value: "Extra levels", status: "OPENED" },
      { id: 3, value: "Special stain", status: "COMPLETED" },
      { id: 4, value: "Deeper section", status: "CANCELLED" },
    ];

    expect(openRequestNames(requests)).toEqual(["Recut", "Extra levels"]);
  });

  // The server counts a request as open only when it says OPENED, and the
  // dashboard tile is counted with that same predicate. Reading a row with no
  // status as open here would make this screen disagree with the tile.
  it("counts a request with no status at all as closed, not as open", () => {
    expect(
      openRequestNames([
        { id: 1, value: "Unstamped", status: null },
        { id: 2, value: "Also unstamped" },
        { id: 3, value: "Recut", status: "OPENED" },
      ]),
    ).toEqual(["Recut"]);
  });

  it("returns nothing for a case with no requests array at all", () => {
    expect(openRequestNames(undefined)).toEqual([]);
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
        caseInfo: { grossExam: "   ", blocks: [{}, {}] },
      }),
    ).toEqual({
      kind: "inProgress",
      textKey: "pathology.badge.blockCount",
      values: { count: 2 },
    });
    expect(
      sectionBadge(microtomy, { caseInfo: { slides: [{}, {}, {}] } }),
    ).toEqual({
      kind: "inProgress",
      textKey: "pathology.badge.slideCount",
      values: { count: 3 },
    });
    expect(sectionBadge(microtomy, { caseInfo: { slides: [] } })).toBeNull();
  });

  // Every stage between grossing and the reading is one OpenELIS records no
  // work at yet, so none of them has a completion rule to satisfy.
  it.each([
    "pathology-section-decalcification",
    "pathology-section-processing",
    "pathology-section-embedding",
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
  it("says a completed review is still outstanding when a request is open on it", () => {
    const review = byId(sectionsAt("COMPLETED"), "pathology-section-review");

    expect(review.state).toBe(SECTION_STATE.COMPLETE);
    expect(sectionBadge(review, { openRequestCount: 1 }).kind).toBe("pending");
  });

  // The header has one badge slot, and for someone who cannot act on the
  // case, being unable to act on it is the more useful of the two facts. The
  // requests are still named on the rail and counted in the summary.
  it("leaves the read-only marker in place on a section the viewer cannot edit, however many requests are open", () => {
    const review = byId(
      sectionsAt("READY_PATHOLOGIST", false),
      "pathology-section-review",
    );

    expect(review.state).toBe(SECTION_STATE.READ_ONLY);
    expect(sectionBadge(review, { openRequestCount: 2 })).toBeNull();
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

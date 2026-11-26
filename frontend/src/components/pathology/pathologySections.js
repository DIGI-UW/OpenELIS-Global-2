/**
 * The single description of the case view's eleven sections and how each one
 * maps onto the bench stages in pathologyStages.js. The accordion, the
 * progress rail and the summary panel all read this one list instead of each
 * keeping its own copy of the section order, so a section can never appear in
 * one place and be missing, reordered or differently gated in another.
 *
 * No React, no I/O: everything here is a plain function of its arguments.
 * The functions that produce a localized string take an intl object and
 * require one; the rest need nothing, so every rule is testable without
 * mounting anything.
 */

import { deriveSectionState, SECTION_STATE } from "../caseView/sectionState";
import {
  PATHOLOGY_STAGES,
  stageDisplayKey,
  stageLabel,
} from "./pathologyStages";
import {
  benchStillOwes,
  countedRows,
  coverslipSummary,
  embeddingSummary,
  hasVerifiableLabel,
  isDeactivatedRow,
  isEmbeddedRow,
  isLabelVerified,
  isStainedRow,
  slideOnBlockName,
  slideRowsToRedo,
  stainingSummary,
  unverifiedSlides,
} from "./pathologyRows";

// The embedding section's id, which the case view also needs to reach the
// section's header.
export const EMBEDDING_SECTION_ID = "pathology-section-embedding";

export const PROCESSING_SECTION_ID = "pathology-section-processing";

export const MICROTOMY_SECTION_ID = "pathology-section-microtomy";

export const STAINING_SECTION_ID = "pathology-section-staining";

export const COVERSLIPPING_SECTION_ID = "pathology-section-coverslipping";

// The review section, which holds the case's bench requests.
export const REVIEW_SECTION_ID = "pathology-section-review";

// The findings section, where the reading is written.
export const FINDINGS_SECTION_ID = "pathology-section-findings";

// A section's own header, where focus lands once nothing left to do in the
// section can take it.
export const sectionHeader = (sectionId) =>
  "#" + sectionId + " button.cds--accordion__heading";

// The single-stage sections are titled by the stage's own key, so the rail,
// the header and the locked hint can never call one stage two things.
// i18n-keys: pathology.section.*
export const PATHOLOGY_SECTIONS = Object.freeze([
  Object.freeze({
    id: "pathology-section-case-info",
    number: 1,
    titleKey: "pathology.section.caseInfo",
    stages: Object.freeze(["ACCESSIONED"]),
    gate: null,
    displayOnly: true,
    placeholder: false,
    pathologistOnly: false,
  }),
  Object.freeze({
    id: "pathology-section-grossing",
    number: 2,
    titleKey: stageDisplayKey("GROSSING"),
    stages: Object.freeze(["GROSSING"]),
    gate: "GROSSING",
    displayOnly: false,
    placeholder: false,
    pathologistOnly: false,
  }),
  Object.freeze({
    id: "pathology-section-decalcification",
    number: 3,
    titleKey: stageDisplayKey("DECALCIFICATION"),
    stages: Object.freeze(["DECALCIFICATION"]),
    gate: "DECALCIFICATION",
    displayOnly: false,
    placeholder: true,
    pathologistOnly: false,
  }),
  Object.freeze({
    id: PROCESSING_SECTION_ID,
    number: 4,
    titleKey: stageDisplayKey("PROCESSING"),
    stages: Object.freeze(["PROCESSING"]),
    gate: "PROCESSING",
    displayOnly: false,
    placeholder: false,
    pathologistOnly: false,
  }),
  Object.freeze({
    id: EMBEDDING_SECTION_ID,
    number: 5,
    titleKey: stageDisplayKey("EMBEDDING"),
    stages: Object.freeze(["EMBEDDING"]),
    gate: "EMBEDDING",
    displayOnly: false,
    placeholder: false,
    pathologistOnly: false,
  }),
  Object.freeze({
    id: MICROTOMY_SECTION_ID,
    number: 6,
    titleKey: stageDisplayKey("MICROTOMY"),
    stages: Object.freeze(["MICROTOMY"]),
    gate: "MICROTOMY",
    displayOnly: false,
    placeholder: false,
    pathologistOnly: false,
  }),
  Object.freeze({
    id: STAINING_SECTION_ID,
    number: 7,
    titleKey: stageDisplayKey("STAINING"),
    stages: Object.freeze(["STAINING"]),
    gate: "STAINING",
    displayOnly: false,
    placeholder: false,
    pathologistOnly: false,
  }),
  Object.freeze({
    id: COVERSLIPPING_SECTION_ID,
    number: 8,
    titleKey: stageDisplayKey("COVERSLIPPING"),
    stages: Object.freeze(["COVERSLIPPING"]),
    gate: "COVERSLIPPING",
    displayOnly: false,
    placeholder: false,
    pathologistOnly: false,
  }),
  Object.freeze({
    id: REVIEW_SECTION_ID,
    number: 9,
    titleKey: "pathology.section.review",
    stages: Object.freeze(["READY_PATHOLOGIST", "UNDER_REVIEW"]),
    gate: "READY_PATHOLOGIST",
    displayOnly: false,
    placeholder: false,
    pathologistOnly: true,
  }),
  Object.freeze({
    id: FINDINGS_SECTION_ID,
    number: 10,
    titleKey: "pathology.section.findings",
    stages: Object.freeze(["READY_PATHOLOGIST", "UNDER_REVIEW"]),
    gate: "READY_PATHOLOGIST",
    displayOnly: false,
    placeholder: false,
    pathologistOnly: true,
  }),
  Object.freeze({
    id: "pathology-section-reports",
    number: 11,
    titleKey: "common.reports",
    stages: Object.freeze(["COMPLETED"]),
    gate: null,
    displayOnly: false,
    placeholder: false,
    pathologistOnly: false,
  }),
]);

/**
 * The configuration key a laboratory flips to stop tracking a stage.
 */
export function stageEnablementProperty(stageId) {
  return "PATHOLOGY_STAGE_" + stageId + "_ENABLED";
}

/**
 * Whether a laboratory tracks the given stage. A stage defaults to enabled,
 * matching the server's own default, so a browser that has not yet fetched
 * configuration never locks a section out by accident; it is disabled only
 * when the server has explicitly said so.
 *
 * The switch is read the way the server reads it when it picks the next and
 * previous stage: trimmed and ignoring case, so a value stored as " FALSE "
 * cannot make the server skip a stage whose section this screen still tracks.
 */
export function isStageEnabled(stageId, configurationProperties) {
  // The server never serves a switch for a stage every deployment tracks, so
  // an absent property is what "cannot be disabled" looks like from here.
  return switchedOn(
    configurationProperties?.[stageEnablementProperty(stageId)],
  );
}

/**
 * Whether a served on/off setting is on: anything but an explicit "false",
 * trimmed and ignoring case, as the server reads its switches, so an absent
 * or unreadable value leaves the default on.
 */
export function switchedOn(value) {
  return (
    String(value ?? "")
      .trim()
      .toLowerCase() !== "false"
  );
}

/**
 * Whether the bench scans a slide's label against its block, or, where the
 * laboratory has no scanner, types both as printed (FR-7.5). Scanning is the
 * default; only the setting switched off turns it into the typed confirmation.
 */
export function scanVerificationRequired(configurationProperties) {
  return switchedOn(
    configurationProperties?.PATHOLOGY_SCAN_VERIFICATION_REQUIRED,
  );
}

/**
 * Whether a stage applies to this case: the laboratory tracks it, and for
 * decalcification grossing also flagged the specimen as needing it (FR-4.1).
 * The stage control and the sections both ask this one question, so what the
 * bench can move a case to and what the case view calls not applicable
 * cannot disagree.
 */
export function stageApplies(
  stageId,
  { configurationProperties, decalcificationRequired } = {},
) {
  if (!isStageEnabled(stageId, configurationProperties)) {
    return false;
  }
  return stageId !== "DECALCIFICATION" || Boolean(decalcificationRequired);
}

// i18n-keys: pathology.action.*
const ADVANCE_LABEL_KEYS = Object.freeze({
  GROSSING: "pathology.action.beginGrossing",
  DECALCIFICATION: "pathology.action.sendForDecalc",
  PROCESSING: "pathology.action.sendForProcessing",
  EMBEDDING: "pathology.action.sendForEmbedding",
  MICROTOMY: "pathology.action.sendToMicrotomy",
  STAINING: "pathology.action.sendForStaining",
  COVERSLIPPING: "pathology.action.sendForCoverslip",
  READY_PATHOLOGIST: "pathology.action.sendToPathologist",
  UNDER_REVIEW: "pathology.action.beginReview",
});

/**
 * What the action bar's primary says for a move to the given stage, or null
 * for a stage no advance reaches.
 */
export function advanceLabelKey(stageId) {
  return ADVANCE_LABEL_KEYS[stageId] ?? null;
}

/**
 * Why the case cannot be sent on yet, as the hint the disabled primary
 * carries, or null when it can. A request still in flight is said first,
 * since its answer may change the stage; then unsaved work, since saving it
 * may be what completes the stage; then the stage's own rule, judged on the
 * stored stage as the server judges it: grossing done under FR-3.4, every
 * cassette embedded, at least one slide cut at microtomy and every slide's
 * label checked against its block (FR-7.5), every slide stained at staining
 * and every slide's coverslip passed at coverslipping (FR-8). The outstanding
 * cassettes and slides are named the way the rail names cassettes, the first
 * two and a count of the rest.
 */
export function advanceBlockedReason(
  intl,
  caseInfo,
  { storedStatus, dirty, saving, moving, configurationProperties } = {},
) {
  if (saving || moving) {
    return { key: "pathology.locked.requestInFlight" };
  }
  if (dirty) {
    return { key: "pathology.locked.saveBeforeTransition" };
  }
  if (storedStatus === "GROSSING" && !grossingComplete(caseInfo)) {
    return { key: "pathology.locked.grossingIncomplete" };
  }
  if (
    storedStatus === "MICROTOMY" &&
    countedRows(caseInfo?.slides).length === 0
  ) {
    return { key: "pathology.locked.microtomyIncomplete" };
  }
  if (storedStatus === "MICROTOMY") {
    const unverified = unverifiedSlides(
      caseInfo?.blocks,
      caseInfo?.slides,
      caseInfo?.labelVerifications,
    );
    if (unverified.length > 0) {
      return {
        key: "pathology.locked.slidesUnverified",
        values: {
          outstanding: outstandingLabel(
            intl,
            unverified.map((entry) => entry.name),
          ).visible,
        },
      };
    }
  }
  if (storedStatus === "EMBEDDING") {
    const summary = embeddingSummary(caseInfo?.blocks);
    if (summary.total === 0) {
      return { key: "pathology.locked.embeddingNoCassettes" };
    }
    if (!summary.complete) {
      return {
        key: "pathology.locked.embeddingIncomplete",
        values: {
          outstanding: outstandingLabel(intl, summary.outstanding).visible,
        },
      };
    }
  }
  if (storedStatus === "STAINING" || storedStatus === "COVERSLIPPING") {
    return outstandingSlidesReason(
      intl,
      slideWorkSummaries(caseInfo, configurationProperties)[storedStatus],
      SLIDE_WORK_GATE_KEYS[storedStatus],
    );
  }
  return null;
}

// i18n-keys: pathology.locked.*
const SLIDE_WORK_GATE_KEYS = Object.freeze({
  STAINING: "pathology.locked.stainingIncomplete",
  COVERSLIPPING: "pathology.locked.coverslipQcIncomplete",
});

// A case with no slide in use has nothing to stain or check, so it is held
// as microtomy holds it: retiring every slide does not open the gate.
function outstandingSlidesReason(intl, summary, key) {
  if (summary.total === 0) {
    return { key: "pathology.locked.microtomyIncomplete" };
  }
  if (summary.outstanding.length === 0) {
    return null;
  }
  return {
    key,
    values: {
      outstanding: outstandingLabel(intl, summary.names).visible,
    },
  };
}

/**
 * Why the case cannot be returned, as the hint the disabled control carries,
 * or null when it can. The lasting reasons come first, since neither saving
 * nor waiting changes them.
 */
export function returnBlockedReason({
  storedStatus,
  previousStage,
  dirty,
  saving,
  moving,
} = {}) {
  if (storedStatus === "COMPLETED") {
    return { key: "pathology.locked.caseCompleted" };
  }
  if (!previousStage) {
    return { key: "pathology.locked.nothingToReturnTo" };
  }
  if (saving || moving) {
    return { key: "pathology.locked.requestInFlight" };
  }
  if (dirty) {
    return { key: "pathology.locked.saveBeforeTransition" };
  }
  return null;
}

/**
 * Whether a row action (Embed, Deactivate) can be pressed, and if not, the
 * hint that says why. A signed-out case holds it until it is reopened;
 * a save, a move or an embed still unanswered holds it until the answer is
 * read back; unsaved work holds it until it is saved or discarded, in the
 * words of `saveFirstKey`, because the action re-reads the case.
 */
export function rowActionLock({
  status,
  dirty = false,
  saving = false,
  moving = false,
  embedding = false,
  saveFirstKey,
} = {}) {
  if (status === "COMPLETED") {
    return { disabled: true, reasonKey: "pathology.locked.caseCompleted" };
  }
  if (saving || moving || embedding) {
    return { disabled: true, reasonKey: "pathology.locked.requestInFlight" };
  }
  if (dirty) {
    return { disabled: true, reasonKey: saveFirstKey };
  }
  return { disabled: false, reasonKey: null };
}

/**
 * Whether a request action (Raise request, Complete, Cancel request, Reopen)
 * can be sent, and if not, the hint that says why. Each is held as Embed is:
 * on a signed-out case, while a save, a move, a mark or another request is
 * unanswered (`moving` covers the last three), and over unsaved work, since
 * the case is read again after it.
 */
export function requestActionLock({
  status,
  dirty = false,
  saving = false,
  moving = false,
} = {}) {
  return rowActionLock({
    status,
    dirty,
    saving,
    moving,
    saveFirstKey: "pathology.locked.saveBeforeRequest",
  });
}

// What a request names as its target, by its kind: a slide request names the
// block to cut it from, a stain request the slide to stain.
const REQUEST_TARGET_FIELD = Object.freeze({
  SLIDE: "targetBlockId",
  STAIN: "targetSlideId",
});

// What a completed request names as having fulfilled it, by its kind.
const REQUEST_RESULT_FIELD = Object.freeze({
  BLOCK: "resultBlockId",
  SLIDE: "resultSlideId",
  STAIN: "resultSlideId",
});

const blank = (value) =>
  value === null || value === undefined || String(value).trim() === "";

const idOrNull = (value) => (blank(value) ? null : Number(value));

/**
 * The first rule a new request does not meet yet, in the order the server
 * checks them (FR-10.3 to FR-10.7): an instruction, then the target of a
 * slide or stain request, then the stain of a stain request. Null once it
 * can be raised. `draft` is the new-request row's own state.
 */
export function requestDraftBlockedReason(draft) {
  if (blank(draft.instruction)) {
    return { key: "pathology.locked.requestInstructionRequired" };
  }
  if (REQUEST_TARGET_FIELD[draft.kind] && blank(draft.targetId)) {
    return { key: "pathology.locked.requestTargetRequired" };
  }
  if (draft.kind === "STAIN" && blank(draft.stainId)) {
    return { key: "pathology.locked.requestStainRequired" };
  }
  return null;
}

/**
 * What raising a request posts. Only the target field of the request's own
 * kind is sent, and a stain only with a stain request, so a pick left behind
 * when the kind was switched never travels with it.
 */
export function requestDraftBody(draft) {
  const targetField = REQUEST_TARGET_FIELD[draft.kind];
  const target = (field) =>
    targetField === field ? idOrNull(draft.targetId) : null;
  return {
    kind: draft.kind,
    targetBlockId: target("targetBlockId"),
    targetSlideId: target("targetSlideId"),
    stainId:
      draft.kind === "STAIN" && !blank(draft.stainId)
        ? String(draft.stainId)
        : null,
    priority: draft.priority,
    instruction: (draft.instruction ?? "").trim(),
  };
}

/**
 * What a status change posts: the status the page shows as `from`, so a
 * request changed elsewhere meanwhile is refused rather than changed twice;
 * a reason only with a cancellation; and what fulfilled a completed request
 * only in the field of its own kind. A request on file has no kind and names
 * nothing as its result.
 */
export function requestStatusBody(
  request,
  status,
  { cancelReason, resultId } = {},
) {
  const resultField =
    status === "COMPLETED" ? REQUEST_RESULT_FIELD[request.kind] : undefined;
  const result = (field) => (resultField === field ? idOrNull(resultId) : null);
  return {
    from: request.status,
    status,
    cancelReason:
      status === "CANCELLED" && !blank(cancelReason)
        ? cancelReason.trim()
        : null,
    resultBlockId: result("resultBlockId"),
    resultSlideId: result("resultSlideId"),
  };
}

/**
 * Whether a slide action at staining or coverslipping (FR-8) can be pressed,
 * and if not, the hint that says why. A mark is taken only while the bench
 * still owes it, so a case with the pathologist holds it until it is
 * returned, and a signed-out case until it is reopened. Then it is held as
 * Embed is: while a save, a move or a mark is unanswered (`moving` covers
 * both), and while the form holds unsaved work, since the mark records the
 * stored stain and the case is read again after it. These reasons hold the
 * whole section. Past them, the row's own reason, marked `own`: a slide is
 * marked stained only with a stain saved on it, and its coverslip checked
 * only once it is stained where the laboratory tracks staining. `action` is
 * "stained", "unsatisfactory", "pass" or "fail"; with no slide only the
 * section's reasons apply, as for the batch button.
 */
export function slideWorkLock({
  status,
  dirty = false,
  saving = false,
  moving = false,
  action,
  slide,
  stainingTracked = true,
} = {}) {
  if (status !== "COMPLETED" && !benchStillOwes(status)) {
    return {
      disabled: true,
      reasonKey: "pathology.locked.slideWorkAfterBench",
      own: false,
    };
  }
  const shared = rowActionLock({
    status,
    dirty,
    saving,
    moving,
    saveFirstKey: "pathology.locked.saveBeforeMarking",
  });
  if (shared.disabled || !slide) {
    return { ...shared, own: false };
  }
  const rowReason = slideActionReason(slide, action, stainingTracked);
  return rowReason
    ? { disabled: true, reasonKey: rowReason, own: true }
    : { disabled: false, reasonKey: null, own: false };
}

// The one rule a row's own action is held by, which a batch reads too.
function slideActionReason(slide, action, stainingTracked) {
  const noStain =
    slide.stainId === null ||
    slide.stainId === undefined ||
    slide.stainId === "";
  if (action === "stained" && noStain) {
    return "pathology.locked.stainMissing";
  }
  if (
    (action === "pass" || action === "fail") &&
    stainingTracked &&
    !isStainedRow(slide)
  ) {
    return "pathology.locked.qcBeforeStain";
  }
  return null;
}

// i18n-keys: pathology.locked.*
const BATCH_HOLD_KEYS = Object.freeze({
  "pathology.locked.stainMissing": "pathology.locked.stainMissingFor",
  "pathology.locked.qcBeforeStain": "pathology.locked.qcBeforeStainFor",
});

/**
 * Why a batch cannot be applied to the selected slides, naming the ones a
 * row's own rule holds, or null when none is. The confirmation says it before
 * anything is posted.
 */
export function batchHold(blocks, slides, action, stainingTracked = true) {
  const held = (slides ?? []).filter((slide) =>
    Boolean(slideActionReason(slide, action, stainingTracked)),
  );
  if (held.length === 0) {
    return null;
  }
  return {
    key: BATCH_HOLD_KEYS[slideActionReason(held[0], action, stainingTracked)],
    values: {
      designations: held
        .map((slide) => slideOnBlockName(slide, blocks))
        .join(", "),
    },
  };
}

/**
 * The case's entries into one stage, oldest first as the case lists them.
 */
export function stageEntries(caseInfo, stage) {
  return (caseInfo?.stageEvents ?? []).filter((event) => event.stage === stage);
}

/**
 * The notice a section shows for the slides its own rule finds broken: those
 * marked unsatisfactory at staining, those whose coverslip failed at
 * coverslipping. It names them in list order and gives the way back only
 * while the bench still owes the work. It says they hold the case here only
 * when they do: when every one of them is still owed by the gate of the stage
 * the case stands at now. An unsatisfactory slide therefore holds a case at
 * coverslipping too, where the laboratory tracks staining, since the
 * coverslip gate asks for a stained slide; a failed coverslip does not hold a
 * case returned to staining.
 */
export function redoNotice(
  stage,
  blocks,
  slides,
  status,
  stainingTracked = true,
) {
  const broken = slideRowsToRedo(slides, stage);
  if (broken.length === 0) {
    return null;
  }
  const owed = new Set(
    (
      gateSummary(blocks, slides, status, stainingTracked)?.outstanding ?? []
    ).map((entry) => String(entry.slide.id)),
  );
  const holdsHere = broken.every((slide) => owed.has(String(slide.id)));
  const keys = REDO_NOTICE_KEYS[stage];
  return {
    titleKey: holdsHere ? keys.here : keys.plain,
    values: {
      designations: broken
        .map((slide) => slideOnBlockName(slide, blocks))
        .join(", "),
    },
    recipeKey: benchStillOwes(status) ? keys.recipe : null,
  };
}

// The summary the gate of the stage the case stands at is judged by, the one
// the advance is held by, or none where that stage asks nothing of slides.
function gateSummary(blocks, slides, status, stainingTracked) {
  if (status === "STAINING") {
    return stainingSummary(blocks, slides);
  }
  if (status === "COVERSLIPPING") {
    return coverslipSummary(blocks, slides, { stainingTracked });
  }
  return null;
}

// i18n-keys: pathology.banner.*
const REDO_NOTICE_KEYS = Object.freeze({
  STAINING: {
    here: "pathology.banner.unsatisfactorySlidesHere",
    plain: "pathology.banner.unsatisfactorySlides",
    recipe: "pathology.banner.restainOrRecut",
  },
  COVERSLIPPING: {
    here: "pathology.banner.failedQcSlidesHere",
    plain: "pathology.banner.failedQcSlides",
    recipe: "pathology.banner.recoverslipOrRecut",
  },
});

/**
 * Staining and coverslipping for the case, each counted from its slides
 * once, with whether the laboratory tracks staining read in one place: the
 * coverslip rule asks a slide to read Stained only where it does.
 */
export function slideWorkSummaries(caseInfo, configurationProperties) {
  const stainingTracked = isStageEnabled("STAINING", configurationProperties);
  return {
    stainingTracked,
    STAINING: gateSummary(
      caseInfo?.blocks,
      caseInfo?.slides,
      "STAINING",
      stainingTracked,
    ),
    COVERSLIPPING: gateSummary(
      caseInfo?.blocks,
      caseInfo?.slides,
      "COVERSLIPPING",
      stainingTracked,
    ),
  };
}

/**
 * What a stage's slide work reads as on the header badge, the summary and
 * the rail: "none" with no slide in use, "notRecorded" once the case has
 * left the bench with no slide carrying a value (every case worked before
 * statuses were kept), and otherwise "count".
 */
export function slideWorkReading(summary, status) {
  if (summary.total === 0) {
    return "none";
  }
  if (!benchStillOwes(status) && summary.recorded === 0) {
    return "notRecorded";
  }
  return "count";
}

/**
 * Whether a slide can be added under the given block, and if not, the hint
 * that says why. A signed-out case takes no new slide until it is reopened;
 * where the laboratory tracks embedding a slide is cut from a block, so a
 * cassette not yet embedded offers none (with the stage switched off no row
 * ever becomes a block, so every row in use qualifies). Unsaved work and a
 * request in flight do not hold it: adding a slide re-reads nothing.
 */
export function addSlideLock({ status, block, configurationProperties } = {}) {
  const signedOut = rowActionLock({ status });
  if (signedOut.disabled) {
    return signedOut;
  }
  if (
    !isEmbeddedRow(block) &&
    isStageEnabled("EMBEDDING", configurationProperties)
  ) {
    return { disabled: true, reasonKey: "pathology.locked.embedBeforeSlides" };
  }
  return { disabled: false, reasonKey: null };
}

/**
 * Whether a slide's label check is offered on its row, and if so whether it
 * can be pressed, with the hint that says why not (FR-7.3 to FR-7.5). It is
 * offered on a saved slide in use that names its block and is not yet
 * verified, until the case is signed out, and only where the section can be
 * worked in. Like Embed and Deactivate it is held while a save, a move or
 * the row's own check is unanswered, since a check landing during the save
 * races the same slide; unsaved work does not hold it, because a check
 * changes nothing the save posts.
 */
export function verifyLabelLock({
  slide,
  labelVerifications,
  status,
  readOnly = false,
  untracked = false,
  saving = false,
  moving = false,
  verifying = false,
} = {}) {
  const offered =
    hasVerifiableLabel(slide) &&
    !isDeactivatedRow(slide) &&
    !isLabelVerified(slide, labelVerifications) &&
    !rowActionLock({ status }).disabled &&
    !readOnly &&
    !untracked;
  if (!offered) {
    return { offered: false, disabled: true, reasonKey: null };
  }
  const lock = rowActionLock({ status, saving, moving });
  if (lock.disabled) {
    return { offered, ...lock };
  }
  if (verifying) {
    return {
      offered,
      disabled: true,
      reasonKey: "pathology.locked.requestInFlight",
    };
  }
  return { offered, disabled: false, reasonKey: null };
}

/**
 * The id of the section a case standing at the given stage is worked in, or
 * null for a stage this build does not know.
 */
export function sectionIdOfStage(stageId) {
  return (
    PATHOLOGY_SECTIONS.find((section) => section.stages.includes(stageId))
      ?.id ?? null
  );
}

/**
 * Whether the decalcification flag is past changing, judged by the stage the
 * case is stored at, as the server judges it, never by a stage picked but not
 * yet saved. Once the case has gone past decalcification the flag records
 * what was done to the specimen, and unticking it would make the case say the
 * stage never applied. PATHOLOGY_STAGES is in bench order, the order the
 * server's own stage list is declared in (FR-2.6); a stage this build does
 * not know freezes nothing, and the server still has the last word.
 */
export function decalcificationFlagFrozen(storedStatus) {
  return (
    PATHOLOGY_STAGES.indexOf(storedStatus) >
    PATHOLOGY_STAGES.indexOf("DECALCIFICATION")
  );
}

/**
 * The shared badge vocabulary (see caseView/StatusBadge.jsx) that best
 * describes a case sitting at the given stage.
 */
export function stageBadgeKind(stageId) {
  switch (stageId) {
    case "ACCESSIONED":
      return "none";
    case "GROSSING":
    case "DECALCIFICATION":
    case "PROCESSING":
    case "EMBEDDING":
    case "MICROTOMY":
    case "STAINING":
    case "COVERSLIPPING":
    case "UNDER_REVIEW":
      return "inProgress";
    case "READY_PATHOLOGIST":
      return "pending";
    case "COMPLETED":
      return "complete";
    default:
      return "none";
  }
}

/**
 * FR-3.4: grossing is done when the specimen has been described and cut, so
 * both the macroscopic description and at least one saved cassette in use
 * are required. A description of only whitespace is not a description.
 */
export function grossingComplete(caseInfo) {
  const described = (caseInfo?.grossExam ?? "").trim() !== "";
  const cut = countedRows(caseInfo?.blocks).length > 0;

  return described && cut;
}

/**
 * The badge a section header carries, derived from the rows the case holds
 * and never from where the case stands: grossing is Complete under FR-3.4,
 * embedding is Complete once every saved cassette is a block and otherwise
 * says how many are, staining and coverslipping likewise for the slides
 * stained and passed, the other sections that record rows name how many, and
 * an outstanding request outranks all of it. A read-only section leaves the
 * slot to the shell's own marker, except a stage the laboratory does not
 * track, which says so.
 */
export function sectionBadge(
  section,
  { openRequestCount, caseInfo, configurationProperties } = {},
) {
  if (section.readOnlyBecauseUntracked) {
    return { kind: "none", textKey: "pathology.locked.stageDisabled" };
  }
  // The bench works the requests from a read-only review, so the count is
  // said there too; any other read-only section leaves the slot to the
  // shell's own marker.
  if (section.id === REVIEW_SECTION_ID && openRequestCount > 0) {
    return {
      kind: "pending",
      textKey: "pathology.badge.openRequestCount",
      values: { count: openRequestCount },
    };
  }

  if (section.state === SECTION_STATE.READ_ONLY) {
    return null;
  }

  if (section.id === "pathology-section-grossing") {
    if (grossingComplete(caseInfo)) {
      return { kind: "complete", textKey: "common.complete" };
    }
    return countBadge("pathology.badge.cassetteCount", caseInfo?.blocks);
  }

  if (section.id === EMBEDDING_SECTION_ID) {
    return embeddingBadge(embeddingSummary(caseInfo?.blocks));
  }

  if (section.id === MICROTOMY_SECTION_ID) {
    return countBadge("pathology.badge.slideCount", caseInfo?.slides);
  }

  const stage = SLIDE_WORK_STAGE_OF[section.id];
  if (stage) {
    const summary = slideWorkSummaries(caseInfo, configurationProperties)[
      stage
    ];
    return slideWorkBadge(stage, summary, caseInfo?.status);
  }

  return null;
}

// The sections that count slide work, by the stage they count.
const SLIDE_WORK_STAGE_OF = Object.freeze({
  [STAINING_SECTION_ID]: "STAINING",
  [COVERSLIPPING_SECTION_ID]: "COVERSLIPPING",
});

// A case that left the bench with nothing recorded shows no count at all: a
// count of none would claim the slides were never stained.
function slideWorkBadge(stage, summary, status) {
  if (slideWorkReading(summary, status) !== "count") {
    return null;
  }
  if (summary.complete) {
    return { kind: "complete", textKey: "common.complete" };
  }
  return {
    kind: "partial",
    ...slideWorkCount(stage, summary),
  };
}

// The "{n} of {total}" sentence for a stage, as a key and its values.
function slideWorkCount(stage, summary) {
  const { key, placeholder } = SLIDE_WORK_BADGES[stage];
  return {
    textKey: key,
    values: { [placeholder]: summary.done, total: summary.total },
  };
}

function embeddingBadge({ embedded, total, complete }) {
  if (total === 0) {
    return null;
  }
  if (complete) {
    return { kind: "complete", textKey: "common.complete" };
  }
  return {
    kind: "partial",
    textKey: "pathology.badge.embeddedOf",
    values: { embedded, total },
  };
}

/**
 * The sentence that names what is still outstanding, the cassettes waiting
 * for embedding or the slides whose label is unchecked, so the rail, the
 * section, the summary and the disabled primary agree: `visible` names the
 * first few and counts the rest, `full` names them all.
 */
export function outstandingLabel(intl, outstanding) {
  return namedListLabel(intl, outstanding, {
    allKey: "pathology.badge.outstanding",
    moreKey: "pathology.badge.outstandingMore",
    placeholder: "designations",
  });
}

/**
 * The summary's line about embedding, under the rail's rule: not applicable
 * where the laboratory does not track the stage, nothing before a cassette is
 * saved, otherwise how many are embedded, with the outstanding ones named in
 * the hover title once the case has reached the stage.
 */
export function embeddingStatement(
  intl,
  { summary, status, configurationProperties } = {},
) {
  if (!isStageEnabled("EMBEDDING", configurationProperties)) {
    return {
      value: intl.formatMessage({ id: "caseView.badge.notApplicable" }),
    };
  }
  if (summary.total === 0) {
    return { value: null };
  }
  return {
    value: intl.formatMessage(
      { id: "pathology.badge.embeddedOf" },
      { embedded: summary.embedded, total: summary.total },
    ),
    title: embeddingAwaitsBench(summary, status, false)
      ? outstandingLabel(intl, summary.outstanding).full
      : undefined,
  };
}

/**
 * The summary's line about staining or coverslipping (FR-8), under the
 * rail's rule: not applicable where the laboratory does not track the stage,
 * nothing before a slide is saved, Not recorded on a case that left the
 * bench with nothing recorded, otherwise how many are done, with the
 * outstanding slides named in the hover title while they hold the case.
 * `stage` is "STAINING" or "COVERSLIPPING".
 */
export function slideWorkStatement(
  intl,
  { stage, summary, status, configurationProperties } = {},
) {
  if (!isStageEnabled(stage, configurationProperties)) {
    return {
      value: intl.formatMessage({ id: "caseView.badge.notApplicable" }),
    };
  }
  const reading = slideWorkReading(summary, status);
  if (reading === "none") {
    return { value: null };
  }
  if (reading === "notRecorded") {
    return { value: intl.formatMessage({ id: "common.notRecorded" }) };
  }
  const { textKey, values } = slideWorkCount(stage, summary);
  return {
    value: intl.formatMessage({ id: textKey }, values),
    title: slideWorkAwaitsBench(summary, status, stage, false)
      ? outstandingLabel(intl, summary.names).full
      : undefined,
  };
}

// i18n-keys: pathology.badge.*
const SLIDE_WORK_BADGES = Object.freeze({
  STAINING: { key: "pathology.badge.stainedOf", placeholder: "stained" },
  COVERSLIPPING: { key: "pathology.badge.qcPassedOf", placeholder: "passed" },
});

// i18n-keys: pathology.badge.*
function countBadge(textKey, rows) {
  const count = countedRows(rows).length;
  return count > 0 ? { kind: "inProgress", textKey, values: { count } } : null;
}

/**
 * The requests still open on a case, in the order the case holds them. Open
 * means the status the server calls open, and nothing else:
 * the dashboard tile is counted with exactly that status, so reading a
 * status-less row as open here would make this screen count requests the rest
 * of the application does not.
 * Nothing on the screen can produce such a row any more either, since a
 * request is stamped open the moment it is raised.
 */
export function openRequests(requests) {
  return (requests ?? []).filter((request) => request.status === "OPENED");
}

/**
 * The instructions of the requests still open on a case, in the order the
 * case holds them.
 */
export function openRequestNames(requests) {
  return openRequests(requests).map((request) => request.instruction);
}

// How many characters of an instruction a rail step shows; the rest is cut
// to an ellipsis there and read in full in the step's title.
const REQUEST_NAME_LIMIT = 40;

const shortened = (name) =>
  name.length > REQUEST_NAME_LIMIT
    ? name.slice(0, REQUEST_NAME_LIMIT) + "\u2026"
    : name;

/**
 * How many names a rail step shows before counting the rest; the two-line
 * clamp in caseView.scss is the actual bound on the step.
 */
const NAMED_LIST_LIMIT = 2;

/**
 * A sentence naming a list of things: `visible` names the first few and
 * counts the rest, `full` names them all, for the hover title. `placeholder`
 * is the argument the two messages name the list by, and `shorten` cuts a
 * name in the visible label only.
 */
export function namedListLabel(
  intl,
  names,
  {
    allKey,
    moreKey,
    limit = NAMED_LIST_LIMIT,
    placeholder = "names",
    shorten = (name) => name,
  },
) {
  const named = names.slice(0, limit).map(shorten);
  const remaining = names.length - named.length;
  const full = intl.formatMessage(
    { id: allKey },
    { [placeholder]: names.join(", ") },
  );

  if (remaining <= 0) {
    return {
      visible: intl.formatMessage(
        { id: allKey },
        { [placeholder]: named.join(", ") },
      ),
      full,
    };
  }

  return {
    visible: intl.formatMessage(
      { id: moreKey },
      { [placeholder]: named.join(", "), count: remaining },
    ),
    full,
  };
}

/**
 * The rail's sentence about the requests still open. An instruction is free
 * text, so the visible label cuts a long one short and the title keeps it
 * whole.
 */
export function openRequestsLabel(intl, names) {
  return namedListLabel(intl, names, {
    allKey: "pathology.badge.openRequests",
    moreKey: "pathology.badge.openRequestsMore",
    shorten: shortened,
  });
}

/**
 * Whether a section starts expanded, or undefined to leave that to the
 * section itself, which opens when its own state says it holds the work in
 * hand.
 *
 * The case view is an accordion rather than a set of tabs because the
 * pathologist has to read the macroscopic description and their own
 * microscopic description together to sign a case out, and tabs would put one
 * of the two behind a click. Grossing derives as complete by the time a case
 * reaches the pathologist, though, so under the ordinary rule it would
 * collapse at exactly the moment the reading sections open, which is the
 * arrangement the accordion was chosen to avoid.
 */
function sectionOpensByDefault(section, status) {
  if (section.displayOnly) {
    return false;
  }

  if (
    section.id === "pathology-section-grossing" &&
    (status === "READY_PATHOLOGIST" || status === "UNDER_REVIEW")
  ) {
    return true;
  }

  return undefined;
}

/**
 * Why a section does not apply to this case, or null when it does: the hint
 * that says so, and whether the reason is that the laboratory does not track
 * the stage at all. That reason is said first, since it holds for every case;
 * past that, the only way stageApplies turns a stage down is FR-3.2's flag on
 * decalcification.
 * A case standing at the stage is never told it does not apply, so a case
 * moved there by hand can still say where it is.
 */
function notApplicableReason(
  section,
  { isCurrent, configurationProperties, decalcificationRequired },
) {
  if (section.gate === null || isCurrent) {
    return null;
  }
  if (!isStageEnabled(section.gate, configurationProperties)) {
    return { key: "pathology.locked.stageDisabled", untracked: true };
  }
  if (
    !stageApplies(section.gate, {
      configurationProperties,
      decalcificationRequired,
    })
  ) {
    return { key: "pathology.locked.decalcNotRequired", untracked: false };
  }
  return null;
}

/**
 * The review section's own state where the stage gate alone would be wrong,
 * or null to leave it to the gate. The review holds the case's bench
 * requests, and the bench works them: once the case holds a request, a
 * bench stage shows the review read-only in bench mode (its fields
 * read-only, the requests table's status actions live) instead of locked, and
 * so does a case with the pathologist for anyone but the pathologist. A
 * signed-out case is left to the gate as it always was (complete for the
 * pathologist, read-only for anyone else); the case view offers no request
 * action on it. A status this build cannot place is left to the gate too,
 * which locks it.
 */
function reviewSectionState(status, { isPathologist, requestCount }) {
  const benchMode = {
    state: SECTION_STATE.READ_ONLY,
    lockedHintKey: undefined,
    lockedHintValues: undefined,
    benchRequests: true,
  };
  const holdsRequests = requestCount > 0;
  if (benchStillOwes(status)) {
    return holdsRequests ? benchMode : null;
  }
  if (status === "READY_PATHOLOGIST" || status === "UNDER_REVIEW") {
    return holdsRequests && !isPathologist ? benchMode : null;
  }
  return null;
}

/**
 * Whether the review takes a new request (FR-10.12): only while it is
 * open, which is the pathologist's while the case is with them. In bench
 * mode the review's requests are worked but none is raised, and a signed-out
 * case's review, complete for the pathologist, takes no request action.
 */
export function reviewTakesRequests(section) {
  return (
    section?.id === REVIEW_SECTION_ID && section.state === SECTION_STATE.OPEN
  );
}

/**
 * The findings section's state (design table S1 to S9). It gates on the
 * stage the server names as the one the case is read at, Under Pathologist
 * Review where the laboratory tracks it, else Ready for Pathologist; a case
 * that names none is read at Ready for Pathologist. A pathologist with the
 * case in the queue is told to press Begin review instead of being told to
 * wait. At or past the gate it is the pathologist's only while the server
 * says the reading is open (`findingsOpen`), which on a completed case means
 * one never released; it is read-only for everyone else and once signed out.
 * A status this build cannot place is locked by the gate.
 */
function findingsSectionState(
  intl,
  status,
  { isPathologist, readingStage, findingsOpen },
) {
  const gate = readingStage || "READY_PATHOLOGIST";
  const derived = deriveSectionState({
    status,
    stageOrder: PATHOLOGY_STAGES,
    sectionStage: gate,
    stageLabel: stageLabel(intl, gate),
  });
  if (derived.state === SECTION_STATE.DISABLED) {
    return isPathologist &&
      status === "READY_PATHOLOGIST" &&
      gate === "UNDER_REVIEW"
      ? {
          state: SECTION_STATE.DISABLED,
          lockedHintKey: "pathology.locked.beginReviewFirst",
          lockedHintValues: undefined,
        }
      : derived;
  }
  let state = SECTION_STATE.READ_ONLY;
  if (isPathologist && findingsOpen) {
    state =
      status === "COMPLETED" ? SECTION_STATE.COMPLETE : SECTION_STATE.OPEN;
  }
  return { state, lockedHintKey: undefined, lockedHintValues: undefined };
}

/**
 * A signed-out case's review is complete, and still editable, for the
 * pathologist only while the server says the reading is open, which is a
 * case never released (S9); a released one is read-only for everyone (S8).
 */
function signedOutReviewState(section, state, findingsOpen) {
  if (
    section.id === REVIEW_SECTION_ID &&
    state === SECTION_STATE.COMPLETE &&
    !findingsOpen
  ) {
    return SECTION_STATE.READ_ONLY;
  }
  return state;
}

/**
 * One case view section, derived fresh from the case's status, the caller's
 * role, the laboratory's stage configuration, the grossing bench's
 * decalcification flag, whether the section holds rows of its own and, for
 * the review, how many requests the case holds.
 * Nothing here is stored: the same inputs always produce the same eleven
 * results.
 *
 * A stage the laboratory does not track locks its section, except one that
 * already holds rows: those are on record, and a locked section cannot show
 * its body, so it is read-only instead and still not applicable.
 */
function deriveOneSection(
  section,
  {
    intl,
    status,
    isPathologist,
    configurationProperties,
    decalcificationRequired,
    sectionHasRows,
    requestCount,
    readingStage,
    findingsOpen,
  },
) {
  const isCurrent = section.stages.includes(status);
  const reason = notApplicableReason(section, {
    isCurrent,
    configurationProperties,
    decalcificationRequired,
  });

  if (reason?.untracked && sectionHasRows(section)) {
    return {
      ...section,
      state: SECTION_STATE.READ_ONLY,
      lockedHintKey: undefined,
      lockedHintValues: undefined,
      isCurrent,
      notApplicable: true,
      readOnlyBecauseUntracked: true,
    };
  }

  if (reason) {
    return {
      ...section,
      state: SECTION_STATE.DISABLED,
      lockedHintKey: reason.key,
      lockedHintValues: undefined,
      isCurrent,
      notApplicable: true,
    };
  }

  const review =
    section.id === REVIEW_SECTION_ID &&
    reviewSectionState(status, { isPathologist, requestCount });
  if (review) {
    return { ...section, ...review, isCurrent, notApplicable: false };
  }

  if (section.id === FINDINGS_SECTION_ID) {
    return {
      ...section,
      ...findingsSectionState(intl, status, {
        isPathologist,
        readingStage,
        findingsOpen,
      }),
      isCurrent,
      notApplicable: false,
    };
  }

  if (section.gate === null) {
    const derived = section.displayOnly
      ? deriveSectionState({ sectionStage: null, readOnly: true })
      : deriveSectionState({ sectionStage: null });
    return { ...section, ...derived, isCurrent, notApplicable: false };
  }

  const derived = deriveSectionState({
    status,
    stageOrder: PATHOLOGY_STAGES,
    sectionStage: section.gate,
    stageLabel: stageLabel(intl, section.gate),
    roleCanEdit: section.pathologistOnly ? Boolean(isPathologist) : true,
  });

  // A case at UNDER_REVIEW sits past the READY_PATHOLOGIST gate that the
  // review and findings sections open at, so the ordinary stage-order rule
  // reads them as already complete. The case is still being reviewed, so the
  // pathologist needs them open for work, not merely revisable.
  const state =
    derived.state === SECTION_STATE.COMPLETE && isCurrent
      ? SECTION_STATE.OPEN
      : signedOutReviewState(section, derived.state, findingsOpen);

  return {
    ...section,
    ...derived,
    state,
    isCurrent,
    notApplicable: false,
  };
}

/**
 * All eleven sections, each carrying its derived state alongside the static
 * description in PATHOLOGY_SECTIONS. Every status, known or not, yields
 * exactly eleven results: nothing is ever hidden, only locked, read-only or
 * marked not applicable. `sectionHasRows` says whether a section holds rows
 * on record; a section it is not asked about holds none. `requestCount` is
 * how many requests the case holds, whatever their status, which the review
 * section reads. `readingStage` and `findingsOpen` are the case read's own:
 * the stage the case is read at and whether the reading can be written now.
 */
export function deriveCaseSections({
  intl,
  status,
  isPathologist,
  configurationProperties,
  decalcificationRequired,
  sectionHasRows = () => false,
  requestCount = 0,
  readingStage,
  findingsOpen = false,
} = {}) {
  return PATHOLOGY_SECTIONS.map((section) => ({
    ...deriveOneSection(section, {
      intl,
      status,
      isPathologist,
      configurationProperties,
      decalcificationRequired,
      sectionHasRows,
      requestCount,
      readingStage,
      findingsOpen: Boolean(findingsOpen),
    }),
    openByDefault: sectionOpensByDefault(section, status),
  }));
}

/**
 * The index of the section the case is currently sitting at, for the
 * progress rail's current-step marker, or -1 when the status is not one this
 * build recognizes.
 */
export function railCurrentIndex(sections, status) {
  return sections.findIndex((section) => section.stages.includes(status));
}

// A cassette is named as outstanding, on the rail and in the summary, only
// once the case has reached embedding: before that nothing awaits the bench
// there, and the section itself is still locked.
function embeddingAwaitsBench(embedding, status, notApplicable) {
  return (
    embedding.outstanding.length > 0 &&
    !notApplicable &&
    PATHOLOGY_STAGES.indexOf(status) >= PATHOLOGY_STAGES.indexOf("EMBEDDING")
  );
}

// Slides are named as outstanding at staining or coverslipping once the case
// has reached the stage and while the bench still owes them.
function slideWorkAwaitsBench(summary, status, stage, notApplicable) {
  return (
    summary.outstanding.length > 0 &&
    !notApplicable &&
    PATHOLOGY_STAGES.indexOf(status) >= PATHOLOGY_STAGES.indexOf(stage) &&
    benchStillOwes(status)
  );
}

/**
 * Whether a section holds rows on record of its own, which a stage the
 * laboratory stopped tracking still shows read-only: microtomy any slide,
 * staining a slide with a stain status, coverslipping a slide with a QC
 * result. Retired slides count: they are on record too.
 */
export function sectionHoldsRecords(sectionId, caseInfo) {
  const slides = caseInfo?.slides ?? [];
  switch (sectionId) {
    case MICROTOMY_SECTION_ID:
      return slides.length > 0;
    case STAINING_SECTION_ID:
      return slides.some((slide) => Boolean(slide.stainStatus));
    case COVERSLIPPING_SECTION_ID:
      return slides.some((slide) => Boolean(slide.coverslipQc));
    default:
      return false;
  }
}

/**
 * The progress rail's item for each derived section. A step is complete on
 * the record where a rule exists (grossing under FR-3.4, microtomy holding a
 * slide, every slide stained or passed, a report existing) and on the case having passed the stage where
 * none does. A step the laboratory does not track, or one this specimen does
 * not need, is marked not applicable and never complete. The review step
 * names the outstanding requests, the embedding step the outstanding
 * cassettes, and staining and coverslipping the outstanding slides.
 */
export function deriveRailItems(
  sections,
  {
    intl,
    status,
    reportCount,
    openRequestNames: names,
    caseInfo,
    configurationProperties,
  } = {},
) {
  const caseIndex = PATHOLOGY_STAGES.indexOf(status);
  const embedding = embeddingSummary(caseInfo?.blocks);
  const summaries = slideWorkSummaries(caseInfo, configurationProperties);

  return sections.map((section) => {
    const isReportsSection = section.id === "pathology-section-reports";
    const lastStage = section.stages[section.stages.length - 1];
    const lastStageIndex = PATHOLOGY_STAGES.indexOf(lastStage);
    const notApplicable = section.notApplicable;
    const passed = !notApplicable && caseIndex > lastStageIndex;

    let complete = passed;
    if (isReportsSection) {
      complete = reportCount > 0;
    } else if (section.id === "pathology-section-grossing") {
      complete = passed && grossingComplete(caseInfo);
    } else if (section.id === EMBEDDING_SECTION_ID) {
      complete = passed && embedding.complete;
    } else if (section.id === MICROTOMY_SECTION_ID) {
      complete = passed && countedRows(caseInfo?.slides).length > 0;
    } else if (SLIDE_WORK_STAGE_OF[section.id]) {
      // Once the case has left the bench a passed step is complete by
      // passage: the gates held it until the work was done, or it was done
      // before statuses were kept.
      const summary = summaries[SLIDE_WORK_STAGE_OF[section.id]];
      complete = passed && (summary.complete || !benchStillOwes(status));
    }

    let pending;
    if (section.id === REVIEW_SECTION_ID && names?.length > 0) {
      pending = openRequestsLabel(intl, names);
    } else if (
      section.id === EMBEDDING_SECTION_ID &&
      embeddingAwaitsBench(embedding, status, notApplicable)
    ) {
      pending = outstandingLabel(intl, embedding.outstanding);
    } else if (
      SLIDE_WORK_STAGE_OF[section.id] &&
      slideWorkAwaitsBench(
        summaries[SLIDE_WORK_STAGE_OF[section.id]],
        status,
        SLIDE_WORK_STAGE_OF[section.id],
        notApplicable,
      )
    ) {
      pending = outstandingLabel(
        intl,
        summaries[SLIDE_WORK_STAGE_OF[section.id]].names,
      );
    }

    return {
      id: section.id,
      labelKey: section.titleKey,
      complete,
      notApplicable,
      pendingLabel: pending?.visible,
      pendingTitle: pending?.full,
    };
  });
}

/**
 * Where the macroscopic description is written (AC-23): Grossing's field on
 * the bench, Findings' from Ready for Pathologist on. A status this build
 * cannot place is read as the bench's.
 */
export function grossExamPlace(status) {
  return PATHOLOGY_STAGES.indexOf(status) >=
    PATHOLOGY_STAGES.indexOf("READY_PATHOLOGIST")
    ? "findings"
    : "grossing";
}

/**
 * The slides in use on the case, one row each in list order, for the review's
 * Slides received: the name it goes by under its block, its stain, whether
 * its label was checked against its block, whether it is stained and its
 * coverslip QC result, null when none was recorded. A slide added since the
 * last save is not received yet.
 */
export function slidesReceived(caseInfo, intl) {
  return countedRows(caseInfo?.slides).map((slide) => ({
    id: slide.id,
    name: slideOnBlockName(slide, caseInfo.blocks),
    stain:
      caseInfo.stainNames?.[slide.stainId] ||
      intl.formatMessage({ id: "common.notRecorded" }),
    blockId: slide.blockId ?? null,
    stained: isStainedRow(slide),
    coverslipQc: slide.coverslipQc ?? null,
  }));
}

/**
 * The names of the slides in use that name their block and whose label was
 * never checked against it: a legacy case that passed Microtomy before the
 * check existed. Saying so is a note, never a gate.
 */
export function neverCheckedSlides(caseInfo) {
  return unverifiedSlides(
    caseInfo?.blocks,
    caseInfo?.slides,
    caseInfo?.labelVerifications,
  ).map(({ name }) => name);
}

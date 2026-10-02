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
    id: "pathology-section-processing",
    number: 4,
    titleKey: stageDisplayKey("PROCESSING"),
    stages: Object.freeze(["PROCESSING"]),
    gate: "PROCESSING",
    displayOnly: false,
    placeholder: true,
    pathologistOnly: false,
  }),
  Object.freeze({
    id: "pathology-section-embedding",
    number: 5,
    titleKey: stageDisplayKey("EMBEDDING"),
    stages: Object.freeze(["EMBEDDING"]),
    gate: "EMBEDDING",
    displayOnly: false,
    placeholder: true,
    pathologistOnly: false,
  }),
  Object.freeze({
    id: "pathology-section-microtomy",
    number: 6,
    titleKey: stageDisplayKey("MICROTOMY"),
    stages: Object.freeze(["MICROTOMY"]),
    gate: "MICROTOMY",
    displayOnly: false,
    placeholder: false,
    pathologistOnly: false,
  }),
  Object.freeze({
    id: "pathology-section-staining",
    number: 7,
    titleKey: stageDisplayKey("STAINING"),
    stages: Object.freeze(["STAINING"]),
    gate: "STAINING",
    displayOnly: false,
    placeholder: true,
    pathologistOnly: false,
  }),
  Object.freeze({
    id: "pathology-section-coverslipping",
    number: 8,
    titleKey: stageDisplayKey("COVERSLIPPING"),
    stages: Object.freeze(["COVERSLIPPING"]),
    gate: "COVERSLIPPING",
    displayOnly: false,
    placeholder: true,
    pathologistOnly: false,
  }),
  Object.freeze({
    id: "pathology-section-review",
    number: 9,
    titleKey: "pathology.section.review",
    stages: Object.freeze(["READY_PATHOLOGIST", "UNDER_REVIEW"]),
    gate: "READY_PATHOLOGIST",
    displayOnly: false,
    placeholder: false,
    pathologistOnly: true,
  }),
  Object.freeze({
    id: "pathology-section-findings",
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
 */
export function isStageEnabled(stageId, configurationProperties) {
  // The server never serves a switch for a stage every deployment tracks, so
  // an absent property is what "cannot be disabled" looks like from here.
  return (
    configurationProperties?.[stageEnablementProperty(stageId)] !== "false"
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
 * both the macroscopic description and at least one block are required. A
 * description of only whitespace is not a description.
 */
export function grossingComplete(caseInfo) {
  const described = (caseInfo?.grossExam ?? "").trim() !== "";
  const cut = (caseInfo?.blocks ?? []).length > 0;

  return described && cut;
}

/**
 * The badge a section header carries, derived from the rows the case holds
 * and never from where the case stands: grossing is Complete under FR-3.4,
 * otherwise the sections that record rows name how many, and an outstanding
 * request outranks all of it. A read-only section leaves the slot to the
 * shell's own marker.
 */
export function sectionBadge(section, { openRequestCount, caseInfo } = {}) {
  if (section.state === SECTION_STATE.READ_ONLY) {
    return null;
  }

  if (section.id === "pathology-section-review" && openRequestCount > 0) {
    return {
      kind: "pending",
      textKey: "pathology.badge.openRequestCount",
      values: { count: openRequestCount },
    };
  }

  if (section.id === "pathology-section-grossing") {
    if (grossingComplete(caseInfo)) {
      return { kind: "complete", textKey: "common.complete" };
    }
    return countBadge("pathology.badge.blockCount", caseInfo?.blocks);
  }

  if (section.id === "pathology-section-microtomy") {
    return countBadge("pathology.badge.slideCount", caseInfo?.slides);
  }

  return null;
}

// i18n-keys: pathology.badge.*
function countBadge(textKey, rows) {
  const count = (rows ?? []).length;
  return count > 0 ? { kind: "inProgress", textKey, values: { count } } : null;
}

/**
 * The names of the requests still open on a case, in the order the case
 * holds them. Open means the status the server calls open, and nothing else:
 * the dashboard tile is counted with exactly that status, so reading a
 * status-less row as open here would make this screen count requests the rest
 * of the application does not.
 * Nothing on the screen can produce such a row any more either, since a
 * request is stamped open the moment it is raised.
 */
export function openRequestNames(requests) {
  if (!requests) {
    return [];
  }
  return requests
    .filter((request) => request.status === "OPENED")
    .map((request) => request.value);
}

/**
 * How many outstanding requests a rail step names before counting the rest;
 * the two-line clamp in caseView.scss is the actual bound on the step.
 */
const NAMED_REQUEST_LIMIT = 2;

/**
 * The rail's sentence about the requests still open: `visible` names the
 * first few and counts the rest, `full` names them all, for the hover title.
 */
export function openRequestsLabel(intl, names) {
  const named = names.slice(0, NAMED_REQUEST_LIMIT);
  const remaining = names.length - named.length;
  const full = intl.formatMessage(
    { id: "pathology.badge.openRequests" },
    { names: names.join(", ") },
  );

  if (remaining <= 0) {
    return { visible: full, full };
  }

  return {
    visible: intl.formatMessage(
      { id: "pathology.badge.openRequestsMore" },
      { names: named.join(", "), count: remaining },
    ),
    full,
  };
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
 * One case view section, derived fresh from the case's status, the caller's
 * role and the laboratory's stage configuration. Nothing here is stored: the
 * same status and configuration always produce the same eleven results.
 */
function deriveOneSection(
  section,
  { intl, status, isPathologist, configurationProperties },
) {
  const isCurrent = section.stages.includes(status);
  const stageDisabled =
    section.gate !== null &&
    !isStageEnabled(section.gate, configurationProperties) &&
    !isCurrent;

  if (stageDisabled) {
    return {
      ...section,
      state: SECTION_STATE.DISABLED,
      lockedHintKey: "pathology.locked.stageDisabled",
      lockedHintValues: undefined,
      isCurrent,
      stageDisabled,
    };
  }

  if (section.gate === null) {
    const derived = section.displayOnly
      ? deriveSectionState({ sectionStage: null, readOnly: true })
      : deriveSectionState({ sectionStage: null });
    return {
      ...section,
      ...derived,
      isCurrent,
      stageDisabled,
    };
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
      : derived.state;

  return {
    ...section,
    ...derived,
    state,
    isCurrent,
    stageDisabled,
  };
}

/**
 * All eleven sections, each carrying its derived state alongside the static
 * description in PATHOLOGY_SECTIONS. Every status, known or not, yields
 * exactly eleven results: nothing is ever hidden, only locked, read-only or
 * marked not applicable.
 */
export function deriveCaseSections({
  intl,
  status,
  isPathologist,
  configurationProperties,
} = {}) {
  return PATHOLOGY_SECTIONS.map((section) => ({
    ...deriveOneSection(section, {
      intl,
      status,
      isPathologist,
      configurationProperties,
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

/**
 * The progress rail's item for each derived section. A step is complete on
 * the record where a rule exists (grossing under FR-3.4, microtomy holding a
 * slide, a report existing) and on the case having passed the stage where
 * none does. Only the review step names outstanding requests.
 */
export function deriveRailItems(
  sections,
  { intl, status, reportCount, openRequestNames: names, caseInfo } = {},
) {
  const caseIndex = PATHOLOGY_STAGES.indexOf(status);

  return sections.map((section) => {
    const isReportsSection = section.id === "pathology-section-reports";
    const lastStage = section.stages[section.stages.length - 1];
    const lastStageIndex = PATHOLOGY_STAGES.indexOf(lastStage);
    const passed = !section.stageDisabled && caseIndex > lastStageIndex;

    let complete = passed;
    if (isReportsSection) {
      complete = reportCount > 0;
    } else if (section.id === "pathology-section-grossing") {
      complete = passed && grossingComplete(caseInfo);
    } else if (section.id === "pathology-section-microtomy") {
      complete = passed && (caseInfo?.slides ?? []).length > 0;
    }

    const pending =
      section.id === "pathology-section-review" && names && names.length > 0
        ? openRequestsLabel(intl, names)
        : undefined;

    return {
      id: section.id,
      labelKey: section.titleKey,
      complete,
      notApplicable: section.stageDisabled,
      pendingLabel: pending?.visible,
      pendingTitle: pending?.full,
    };
  });
}

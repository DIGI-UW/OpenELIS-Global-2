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
import { countedRows, embeddingSummary } from "./pathologyRows";

// The embedding section's id, which the case view also needs to reach the
// section's header.
export const EMBEDDING_SECTION_ID = "pathology-section-embedding";

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
 * says how many are, the other sections that record rows name how many, and
 * an outstanding request outranks all of it. A read-only section leaves the
 * slot to the shell's own marker.
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
    return countBadge("pathology.badge.cassetteCount", caseInfo?.blocks);
  }

  if (section.id === EMBEDDING_SECTION_ID) {
    return embeddingBadge(embeddingSummary(caseInfo?.blocks));
  }

  if (section.id === "pathology-section-microtomy") {
    return countBadge("pathology.badge.slideCount", caseInfo?.slides);
  }

  return null;
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
 * The sentence that names the cassettes still waiting for embedding, so the
 * rail, the section and the summary agree: `visible` names the first few and
 * counts the rest, `full` names them all.
 */
export function embeddingOutstandingLabel(intl, outstanding) {
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
      ? embeddingOutstandingLabel(intl, summary.outstanding).full
      : undefined,
  };
}

// i18n-keys: pathology.badge.*
function countBadge(textKey, rows) {
  const count = countedRows(rows).length;
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
 * How many names a rail step shows before counting the rest; the two-line
 * clamp in caseView.scss is the actual bound on the step.
 */
const NAMED_LIST_LIMIT = 2;

/**
 * A sentence naming a list of things: `visible` names the first few and
 * counts the rest, `full` names them all, for the hover title. `placeholder`
 * is the argument the two messages name the list by.
 */
export function namedListLabel(
  intl,
  names,
  { allKey, moreKey, limit = NAMED_LIST_LIMIT, placeholder = "names" },
) {
  const named = names.slice(0, limit);
  const remaining = names.length - named.length;
  const full = intl.formatMessage(
    { id: allKey },
    { [placeholder]: names.join(", ") },
  );

  if (remaining <= 0) {
    return { visible: full, full };
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
 * The rail's sentence about the requests still open.
 */
export function openRequestsLabel(intl, names) {
  return namedListLabel(intl, names, {
    allKey: "pathology.badge.openRequests",
    moreKey: "pathology.badge.openRequestsMore",
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
 * The hint that says why a section does not apply to this case, or null when
 * it does. A stage the laboratory does not track is said first, since it
 * holds for every case; past that, the only way stageApplies turns a stage
 * down is FR-3.2's flag on decalcification.
 * A case standing at the stage is never told it does not apply, so a case
 * moved there by hand can still say where it is.
 */
function notApplicableHintKey(
  section,
  { isCurrent, configurationProperties, decalcificationRequired },
) {
  if (section.gate === null || isCurrent) {
    return null;
  }
  if (!isStageEnabled(section.gate, configurationProperties)) {
    return "pathology.locked.stageDisabled";
  }
  if (
    !stageApplies(section.gate, {
      configurationProperties,
      decalcificationRequired,
    })
  ) {
    return "pathology.locked.decalcNotRequired";
  }
  return null;
}

/**
 * One case view section, derived fresh from the case's status, the caller's
 * role, the laboratory's stage configuration and the grossing bench's
 * decalcification flag. Nothing here is stored: the same inputs always
 * produce the same eleven results.
 */
function deriveOneSection(
  section,
  {
    intl,
    status,
    isPathologist,
    configurationProperties,
    decalcificationRequired,
  },
) {
  const isCurrent = section.stages.includes(status);
  const hintKey = notApplicableHintKey(section, {
    isCurrent,
    configurationProperties,
    decalcificationRequired,
  });

  if (hintKey) {
    return {
      ...section,
      state: SECTION_STATE.DISABLED,
      lockedHintKey: hintKey,
      lockedHintValues: undefined,
      isCurrent,
      notApplicable: true,
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
      : derived.state;

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
 * marked not applicable.
 */
export function deriveCaseSections({
  intl,
  status,
  isPathologist,
  configurationProperties,
  decalcificationRequired,
} = {}) {
  return PATHOLOGY_SECTIONS.map((section) => ({
    ...deriveOneSection(section, {
      intl,
      status,
      isPathologist,
      configurationProperties,
      decalcificationRequired,
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

/**
 * The progress rail's item for each derived section. A step is complete on
 * the record where a rule exists (grossing under FR-3.4, microtomy holding a
 * slide, a report existing) and on the case having passed the stage where
 * none does. A step the laboratory does not track, or one this specimen does
 * not need, is marked not applicable and never complete. The review step
 * names the outstanding requests and the embedding step the outstanding
 * cassettes.
 */
export function deriveRailItems(
  sections,
  { intl, status, reportCount, openRequestNames: names, caseInfo } = {},
) {
  const caseIndex = PATHOLOGY_STAGES.indexOf(status);
  const embedding = embeddingSummary(caseInfo?.blocks);

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
    } else if (section.id === "pathology-section-microtomy") {
      complete = passed && countedRows(caseInfo?.slides).length > 0;
    }

    let pending;
    if (section.id === "pathology-section-review" && names?.length > 0) {
      pending = openRequestsLabel(intl, names);
    } else if (
      section.id === EMBEDDING_SECTION_ID &&
      embeddingAwaitsBench(embedding, status, notApplicable)
    ) {
      pending = embeddingOutstandingLabel(intl, embedding.outstanding);
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

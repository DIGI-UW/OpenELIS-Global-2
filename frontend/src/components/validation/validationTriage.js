/**
 * OGC-1027 (Validation v4 slice V1) — pure triage rules for the validation
 * queue: which "Check before release" chips a row carries, which lane it sits
 * in, and how the filter chips narrow the queue.
 *
 * No React, no I/O: the backend row is the only input, so the rules are
 * unit-tested directly and the component just renders the outcome.
 *
 * The lane is the server's verdict (OGC-1226 FR-5, FR-6): the backend evaluates
 * the one clearance rule on the rows it serves and marks each row `clear`; this
 * module reads that mark and never derives a lane of its own. Chips are still
 * derived here from the row's signals, and a blank chip cell is never read as
 * "QC passed" (FR-A2).
 */

export const QC_PASS = "PASS";
export const QC_FAIL = "FAIL";

export const LANE_CLEAR = "clear";
export const LANE_NEEDS_REVIEW = "needsReview";

/** Filter chip keys, in display order. */
export const FILTERS = [
  "all",
  "needsReview",
  "nce",
  "qcFail",
  "modified",
  "ackPending",
  "critical",
  "abnormal",
];

/** "Check before release" chips, in display order. */
export const SIGNAL_KEYS = [
  "critical",
  "nce",
  "qcFail",
  "modified",
  "ackPending",
  "nonconforming",
];

const isTrue = (value) => value === true;

export function deriveSignals(row) {
  const source = row || {};
  const rangeKnown =
    typeof source.normalRange === "string" &&
    source.normalRange.trim().length > 0;
  const qcStatus =
    source.qcStatus === QC_PASS
      ? QC_PASS
      : source.qcStatus === QC_FAIL
        ? QC_FAIL
        : null;
  return {
    nce: isTrue(source.nceOpen),
    qcFail: qcStatus === QC_FAIL,
    modified: isTrue(source.modified),
    ackPending: isTrue(source.ackPending),
    nonconforming: isTrue(source.nonconforming),
    critical: isTrue(source.critical),
    rangeKnown,
    inRange: rangeKnown && source.normal === true,
    abnormal: rangeKnown && source.normal === false,
  };
}

export function activeSignalChips(signals) {
  return SIGNAL_KEYS.filter((key) => signals[key] === true);
}

/** The lane the server put the row in; anything but an explicit clear is Needs-review. */
export function laneOf(row) {
  return row && row.clear === true ? LANE_CLEAR : LANE_NEEDS_REVIEW;
}

export function matchesFilter(signals, lane, filter) {
  switch (filter) {
    case "needsReview":
      return lane === LANE_NEEDS_REVIEW;
    case "nce":
      return signals.nce;
    case "qcFail":
      return signals.qcFail;
    case "modified":
      return signals.modified;
    case "ackPending":
      return signals.ackPending;
    case "critical":
      return signals.critical;
    case "abnormal":
      return signals.abnormal;
    case "all":
    default:
      return true;
  }
}

/** Annotates every row of the whole queue with its signals, lane and chips. */
export function triageRows(rows) {
  return (rows || []).map((row) => {
    const signals = deriveSignals(row);
    const lane = laneOf(row);
    return { row, signals, lane, chips: activeSignalChips(signals) };
  });
}

/** The signals carried most often across the given rows, most frequent first. */
function dominantSignals(items) {
  const counts = new Map();
  for (const item of items) {
    const carried = [...item.chips];
    if (item.signals.abnormal && !item.signals.critical) {
      carried.push("abnormal");
    }
    for (const key of carried) {
      counts.set(key, (counts.get(key) || 0) + 1);
    }
  }
  return [...counts.entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, 3)
    .map(([key]) => key);
}

/**
 * OGC-1226 (FR-13 to FR-15) — why "Release all clear" cannot be used right now,
 * as every reason that applies with its count, in display order. Empty when the
 * button is usable. A row whose test has no reference value in the catalogue is
 * reported as such rather than as a risk, because the fix for it lives in the
 * Test Catalogue, not in the queue.
 */
export function bulkUnavailableReasons(triaged, { bulkAllowed = true } = {}) {
  const reasons = [];
  if (!bulkAllowed) {
    reasons.push({ key: "bulkDisabled" });
  }
  const items = triaged || [];
  if (items.length === 0) {
    reasons.push({ key: "queueEmpty" });
    return reasons;
  }
  if (items.some((item) => item.lane === LANE_CLEAR)) {
    return reasons;
  }
  const withReference = items.filter((item) => item.signals.rangeKnown);
  const noReference = items.filter((item) => !item.signals.rangeKnown);
  if (withReference.length > 0) {
    reasons.push({
      key: "signals",
      count: withReference.length,
      dominant: dominantSignals(withReference),
    });
  }
  if (noReference.length > 0) {
    reasons.push({ key: "noReference", count: noReference.length });
  }
  return reasons;
}

/** Live counts per filter, always over the whole queue (FR-A3). */
export function countByFilter(triaged) {
  const counts = Object.fromEntries(FILTERS.map((filter) => [filter, 0]));
  for (const item of triaged) {
    for (const filter of FILTERS) {
      if (matchesFilter(item.signals, item.lane, filter)) {
        counts[filter] += 1;
      }
    }
  }
  return counts;
}

export function filterTriaged(triaged, filter) {
  return triaged.filter((item) =>
    matchesFilter(item.signals, item.lane, filter),
  );
}

/** OGC-1029 (FR-B2) — the rows "Release all clear" may touch: the Clear lane only. */
export function clearRows(triaged) {
  return (triaged || [])
    .filter((item) => item.lane === LANE_CLEAR)
    .map((item) => item.row);
}

/**
 * OGC-1029 — the bulk request: the page's own search key (so the server reloads
 * the same queue and re-derives the lane itself) plus the candidate rows with
 * the validator's note. `params` is the page's query string, e.g.
 * "?type=order&accessionNumber=…"; only an accession ("order") search is unranged.
 */
export function bulkReleaseRequest(results, params, rows) {
  const search = new URLSearchParams((params || "").replace(/^\?/, ""));
  const type = search.get("type") || "";
  return {
    accessionNumber:
      (results && results.accessionNumber) ||
      search.get("accessionNumber") ||
      "",
    testSectionId:
      (results && results.testSectionId) || search.get("testSectionId") || "",
    testDate: (results && results.testDate) || search.get("date") || "",
    doRange: type !== "order",
    rows: (rows || []).map((row) => ({
      analysisId: row.analysisId,
      accessionNumber: row.accessionNumber,
      note: row.note || "",
      noteVisibility: row.noteVisibility || "",
      noteContext: row.noteContext || "VALIDATION",
    })),
  };
}

/** i18n key describing why a bulk release did nothing (or failed). */
export function bulkOutcomeKey(response) {
  const code = response && response.error;
  if (code === "bulkReleaseDisabled" || code === "qcAcknowledgmentRequired") {
    return `label.validation.bulk.error.${code}`;
  }
  if (
    !code &&
    response &&
    Array.isArray(response.released) &&
    response.released.length === 0
  ) {
    const skipped = Array.isArray(response.skipped) ? response.skipped : [];
    if (skipped.length > 0 && skipped.every((s) => s && s.reason === "stale")) {
      return "label.validation.bulk.error.stale";
    }
    return "label.validation.bulk.error.nothingReleased";
  }
  return "label.validation.bulk.error.generic";
}

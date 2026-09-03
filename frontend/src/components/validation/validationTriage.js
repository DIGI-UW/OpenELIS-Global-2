/**
 * OGC-1027 (Validation v4 slice V1) — pure triage rules for the validation
 * queue: which "Check before release" chips a row carries, which lane it sits
 * in, and how the filter chips narrow the queue.
 *
 * No React, no I/O: the backend row is the only input, so the rules are
 * unit-tested directly and the component just renders the outcome.
 *
 * Fail-safe (FRS FR-B1): a row is Clear only when every clearance input is
 * affirmatively known and clean. Anything missing or indeterminate — no
 * reference range, QC not evaluated — keeps the row in Needs-review. A blank
 * chip cell is therefore never read as "QC passed" (FR-A2).
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
    qcKnownPass: qcStatus === QC_PASS,
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

export function computeLane(signals) {
  const clear =
    signals.inRange &&
    signals.qcKnownPass &&
    !signals.nce &&
    !signals.modified &&
    !signals.critical &&
    !signals.nonconforming &&
    !signals.ackPending;
  return clear ? LANE_CLEAR : LANE_NEEDS_REVIEW;
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
    const lane = computeLane(signals);
    return { row, signals, lane, chips: activeSignalChips(signals) };
  });
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

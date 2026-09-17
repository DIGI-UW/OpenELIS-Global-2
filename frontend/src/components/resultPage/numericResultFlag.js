/**
 * OGC-1121 — range classification of a numeric result on the legacy Results
 * Entry screen, with the precedence the server uses for `resultFlag`
 * (ValidationSignals): INVALID beats CRITICAL beats ABNORMAL beats NORMAL.
 *
 * Normal and valid bounds arrive as numbers with 0 standing for "unauthored"
 * (the legacy beans collapse an open bound to 0, so a pair that is equal is
 * ignored). Critical bounds arrive as numbers or null; null never fires.
 */

export const CRITICAL_BACKGROUND = "#ffd7d9";
export const CRITICAL_BORDER = "#da1e28";
export const INVALID_BACKGROUND = "#ffa0a0";
export const ABNORMAL_BACKGROUND = "#ffffa0";

const isBound = (bound) =>
  bound !== null &&
  bound !== undefined &&
  bound !== "" &&
  !Number.isNaN(Number(bound));

const outsidePair = (numeric, low, high) => {
  const lower = Number(low);
  const upper = Number(high);
  return lower !== upper && (numeric < lower || numeric > upper);
};

export function classifyNumericResult(value, row) {
  const outcome = {
    isInvalid: false,
    outsideValid: false,
    isCritical: false,
    outsideNormal: false,
    flag: undefined,
  };
  if (!row || value === null || value === undefined || value === "") {
    return outcome;
  }
  const numeric = Number(value);
  if (Number.isNaN(numeric)) {
    return outcome;
  }
  if (outsidePair(numeric, row.lowerAbnormalRange, row.upperAbnormalRange)) {
    return { ...outcome, isInvalid: true, outsideValid: true, flag: "INVALID" };
  }
  const criticalLow =
    isBound(row.lowerCritical) && numeric < Number(row.lowerCritical);
  const criticalHigh =
    isBound(row.higherCritical) && numeric > Number(row.higherCritical);
  if (criticalLow || criticalHigh) {
    return {
      ...outcome,
      isCritical: true,
      outsideNormal: true,
      flag: "CRITICAL",
    };
  }
  if (outsidePair(numeric, row.lowerNormalRange, row.upperNormalRange)) {
    return { ...outcome, outsideNormal: true, flag: "ABNORMAL" };
  }
  return { ...outcome, flag: "NORMAL" };
}

/** The input styling for a classified value: critical is never just yellow. */
export function numericResultStyle(outcome) {
  return {
    borderColor: outcome.isCritical
      ? CRITICAL_BORDER
      : outcome.isInvalid
        ? "red"
        : "",
    background: outcome.outsideValid
      ? INVALID_BACKGROUND
      : outcome.isCritical
        ? CRITICAL_BACKGROUND
        : outcome.outsideNormal
          ? ABNORMAL_BACKGROUND
          : "var(--cds-field)",
  };
}

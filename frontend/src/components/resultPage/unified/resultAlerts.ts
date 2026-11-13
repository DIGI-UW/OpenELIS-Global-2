import { normalizeScientificNotation } from "../scientificNotation";
import type { ResultAlert } from "../ResultAlertModal";
import { FlaggableRow, resultFlagFor } from "./resultFlagFor";

/** The fields a worklist row carries for deciding what its value owes. */
export interface AlertableRow extends FlaggableRow {
  analysisId: string;
  testResultComponentId?: string;
  resultId?: string | null;
  rawResultValue?: string;
  testName?: string;
  accessionNumber?: string;
}

const numberOf = (value?: string | null): number | null => {
  const text = String(value ?? "")
    .trim()
    .replace(/[<>]/g, "");
  if (!text) {
    return null;
  }
  const numeric = Number(normalizeScientificNotation(text));
  return Number.isNaN(numeric) ? null : numeric;
};

const bound = (value: unknown): number | null =>
  value === null || value === undefined || value === "" || isNaN(Number(value))
    ? null
    : Number(value);

/** Beyond an authored critical bound, whatever the valid range says. */
const beyondCriticalBound = (row: AlertableRow, numeric: number): boolean => {
  const low = bound(row.lowerCritical);
  const high = bound(row.higherCritical);
  return (low !== null && numeric < low) || (high !== null && numeric > high);
};

/**
 * OGC-1417 — what the value a row holds owes before it is saved: a critical
 * value is acknowledged, even when it also lies outside the valid range, and a
 * value outside the valid range is confirmed when Result Configuration asks
 * for it (`alertInvalidResults`). The value already stored owes nothing, it
 * was acknowledged when it was saved; nor does a save that does not write the
 * value. The server applies the same rule and refuses a save that skips it.
 */
export function owedResultAlerts(
  row: AlertableRow,
  options: { alertInvalidResults: boolean; writesValue: boolean },
): ResultAlert[] {
  if (!options.writesValue || row.resultType !== "N" || !row.resultLimitId) {
    return [];
  }
  const value = String(row.resultValue ?? "").trim();
  const numeric = numberOf(value);
  if (!value || numeric === null) {
    return [];
  }
  const stored = numberOf(row.rawResultValue);
  if (row.resultId && stored !== null && stored === numeric) {
    return [];
  }
  const flag = resultFlagFor(row);
  const kinds: ResultAlert["kind"][] = [];
  if (flag === "INVALID" && options.alertInvalidResults) {
    kinds.push("INVALID");
  }
  if (
    flag === "CRITICAL" ||
    (flag === "INVALID" && beyondCriticalBound(row, numeric))
  ) {
    kinds.push("CRITICAL");
  }
  return kinds.map((kind) => ({
    kind,
    value,
    testName: row.testName,
    accessionNumber: row.accessionNumber,
    lowValid: row.lowerAbnormalRange ?? null,
    highValid: row.upperAbnormalRange ?? null,
    analysisId: row.analysisId,
    componentId: row.testResultComponentId ?? null,
  }));
}

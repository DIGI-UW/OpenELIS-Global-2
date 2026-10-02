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

/**
 * OGC-1417 — what the value a row holds owes before it is saved: a critical
 * value is acknowledged, and a value outside the valid range is confirmed when
 * Result Configuration asks for it (`alertInvalidResults`). The value already
 * stored owes nothing, it was acknowledged when it was saved; nor does a save
 * that does not write the value. The server applies the same rule and refuses
 * a save that skips it.
 */
export function owedResultAlert(
  row: AlertableRow,
  options: { alertInvalidResults: boolean; writesValue: boolean },
): ResultAlert | null {
  if (!options.writesValue || row.resultType !== "N") {
    return null;
  }
  const value = String(row.resultValue ?? "").trim();
  if (!value) {
    return null;
  }
  const flag = resultFlagFor(row);
  const kind =
    flag === "CRITICAL"
      ? "CRITICAL"
      : flag === "INVALID" && options.alertInvalidResults
        ? "INVALID"
        : null;
  if (!kind) {
    return null;
  }
  const stored = numberOf(row.rawResultValue);
  if (row.resultId && stored !== null && stored === numberOf(value)) {
    return null;
  }
  return {
    kind,
    value,
    testName: row.testName,
    accessionNumber: row.accessionNumber,
    lowValid: row.lowerAbnormalRange ?? null,
    highValid: row.upperAbnormalRange ?? null,
    analysisId: row.analysisId,
    componentId: row.testResultComponentId ?? null,
  };
}

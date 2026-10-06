/**
 * OGC-1021 (R2) — dilution factor (FR-D5, decision D14).
 *
 * Quantitative results only: reported result = measured value × dilution
 * factor. The reported value is what is stored as the result; the factor and
 * measured value are preserved server-side as an internal provenance note.
 */

/** A dilution applies only to numeric result types (FR-D5). */
export function dilutionApplies(resultType?: string): boolean {
  return resultType === "N";
}

/**
 * The reported value for a measured value and dilution factor, preserving the
 * measured value's decimal places up to the precision the test reports to.
 * A product finer than that (3.5 × 2 on a test reporting whole numbers) is
 * rounded to it, since the precision gate would otherwise refuse the save
 * and leave the technician at a dead end (OGC-1408). Returns null when either
 * input is not a usable positive number — the caller then leaves the result
 * value alone.
 */
export function computeReportedValue(
  measuredValue: string,
  dilutionFactor: string,
  significantDigits?: number,
): string | null {
  const measured = Number(measuredValue);
  const factor = Number(dilutionFactor);
  if (
    measuredValue.trim() === "" ||
    dilutionFactor.trim() === "" ||
    !Number.isFinite(measured) ||
    !Number.isFinite(factor) ||
    factor <= 0
  ) {
    return null;
  }
  const reported = measured * factor;
  let decimals = (measuredValue.split(".")[1] || "").length;
  if (
    typeof significantDigits === "number" &&
    significantDigits >= 0 &&
    decimals > significantDigits
  ) {
    decimals = significantDigits;
  }
  return decimals > 0
    ? reported.toFixed(decimals)
    : String(Math.round(reported));
}

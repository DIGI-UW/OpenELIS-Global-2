import { normalizeScientificNotation } from "../scientificNotation";

/** A dilution applies only to numeric result types. */
export function dilutionApplies(resultType?: string): boolean {
  return resultType === "N";
}

/** Parse a finite decimal into an integer coefficient and decimal scale. */
function decimal(value: string): { coefficient: bigint; scale: number } | null {
  const text = normalizeScientificNotation(value.trim());
  const match = /^([+-]?)(\d*)(?:\.(\d*))?(?:e([+-]?\d+))?$/i.exec(text);
  if (!match || !(match[2] || match[3]) || !Number.isFinite(Number(text))) {
    return null;
  }
  const fraction = match[3] || "";
  const scale = fraction.length - Number(match[4] || 0);
  // Bound work independently of untrusted exponent text.
  if (Math.abs(scale) > 1000 || text.length > 1000) return null;
  return {
    coefficient: BigInt(`${match[1]}${match[2] || "0"}${fraction}`),
    scale,
  };
}

/**
 * The clinical result of the dilution. Reporting decimal places never round
 * the product that will be saved. Decimal arithmetic avoids binary-float
 * artifacts such as 0.1 × 0.2 becoming 0.020000000000000004.
 */
export function computeDilutedValue(
  measuredValue: string,
  dilutionFactor: string,
): string | null {
  const measured = decimal(measuredValue);
  const factor = decimal(dilutionFactor);
  if (!measured || !factor || factor.coefficient <= BigInt(0)) return null;
  const product = measured.coefficient * factor.coefficient;
  const scale = measured.scale + factor.scale;
  const negative = product < BigInt(0);
  const digits = (negative ? -product : product).toString();
  let value: string;
  if (scale <= 0) {
    value = digits + "0".repeat(-scale);
  } else {
    const padded = digits.padStart(scale + 1, "0");
    value = padded.slice(0, -scale) + "." + padded.slice(-scale);
  }
  return (negative ? "-" : "") + value;
}

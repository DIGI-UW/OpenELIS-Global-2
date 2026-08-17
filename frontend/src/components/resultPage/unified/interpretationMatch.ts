/**
 * OGC-1026 (R7, FR-G1) — matches an entered result value against an
 * interpretation bucket's valueMatch expression (configured per component in
 * the Test Catalog Editor). Tolerant grammar:
 *
 *   "70-99"    numeric range, inclusive
 *   ">=126"    / ">126" / "<=69" / "<70"  one-sided bounds
 *   "Positive" exact string match, case-insensitive (dictionary results)
 *
 * Unparseable expressions match nothing — a rule is a non-binding suggestion,
 * never a gate.
 */
export interface InterpretationBucket {
  id?: string;
  valueMatch?: string;
  text?: string;
  severity?: string;
  color?: string;
  displayOrder?: number;
}

export const bucketMatches = (
  valueMatch: string | undefined,
  value: string | undefined,
): boolean => {
  if (!valueMatch || value === undefined || value === null || value === "") {
    return false;
  }
  const expression = valueMatch.trim();
  const numeric = Number(value);
  const hasNumeric = Number.isFinite(numeric);

  const oneSided = expression.match(/^(>=|<=|>|<)\s*(-?\d+(?:\.\d+)?)$/);
  if (oneSided) {
    if (!hasNumeric) {
      return false;
    }
    const bound = Number(oneSided[2]);
    switch (oneSided[1]) {
      case ">=":
        return numeric >= bound;
      case "<=":
        return numeric <= bound;
      case ">":
        return numeric > bound;
      default:
        return numeric < bound;
    }
  }

  const range = expression.match(
    /^(-?\d+(?:\.\d+)?)\s*[-–]\s*(-?\d+(?:\.\d+)?)$/,
  );
  if (range) {
    if (!hasNumeric) {
      return false;
    }
    return numeric >= Number(range[1]) && numeric <= Number(range[2]);
  }

  return expression.toLowerCase() === String(value).trim().toLowerCase();
};

/** the first bucket (by display order) whose expression matches the value */
export const matchingBucket = (
  buckets: InterpretationBucket[],
  value: string | undefined,
): InterpretationBucket | undefined =>
  [...buckets]
    .sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0))
    .find((bucket) => bucketMatches(bucket.valueMatch, value));

/** severity → Carbon tag type; the configured color wins when recognizable */
export const bucketTagType = (bucket: InterpretationBucket): string => {
  const color = (bucket.color || "").toLowerCase();
  if (
    [
      "red",
      "green",
      "blue",
      "teal",
      "purple",
      "magenta",
      "cyan",
      "gray",
    ].includes(color)
  ) {
    return color;
  }
  const severity = (bucket.severity || "").toLowerCase();
  if (severity.includes("crit") || severity.includes("high")) return "red";
  if (severity.includes("abnorm") || severity.includes("warn"))
    return "magenta";
  if (severity.includes("normal") || severity.includes("low")) return "green";
  return "cool-gray";
};

/**
 * Scientific notation for numeric results, shared by the legacy and the
 * unified Results Entry screens.
 *
 * A value written as 1.5e5, 1.5E+05, 1.5 x 10^5, 1.5×10⁵ or 10⁻³ is read as
 * scientific notation. The value is stored and displayed in the notation the
 * technologist wrote; `normalizeScientificNotation` exists only to obtain a
 * number from it, for validating and for flagging against the ranges.
 * Superscript digits count only as the power of ten in that form: a bare "3²"
 * is ambiguous (nine, or three hundred?) and is left alone so that it fails
 * numeric validation. Plain decimals pass through unchanged.
 */
const MANTISSA = "[+-]?(?:\\d+\\.?\\d*|\\.\\d+)";
const EXPONENT_NOTATION = new RegExp(`^(${MANTISSA})[eE]([+-]?\\d+)$`);
const TIMES_TEN_NOTATION = new RegExp(
  `^(?:(${MANTISSA})\\s*[xX×*]\\s*)?([+-]?)10\\s*(?:\\^\\s*([+-]?\\d+)|([⁺⁻]?[⁰¹²³⁴⁵⁶⁷⁸⁹]+))$`,
);
/** A written value split into its comparator, its mantissa and the rest. */
const MANTISSA_SPLIT = new RegExp(`^([<>]?\\s*)(${MANTISSA})(.*)$`);
/** A power of ten carrying no mantissa of its own, such as 10^-3 or 10⁻³. */
const BARE_TEN = /^[+-]?10\s*(?:\^\s*[+-]?\d+|[⁺⁻]?[⁰¹²³⁴⁵⁶⁷⁸⁹]+)$/;
const SUPERSCRIPT_DIGITS = "⁰¹²³⁴⁵⁶⁷⁸⁹";

function superscriptToAscii(superscripts) {
  let ascii = "";
  for (const ch of superscripts) {
    const digit = SUPERSCRIPT_DIGITS.indexOf(ch);
    if (digit >= 0) {
      ascii += digit;
    } else if (ch === "⁺") {
      ascii += "+";
    } else if (ch === "⁻") {
      ascii += "-";
    }
  }
  return ascii;
}

function canonical(mantissa, power, original) {
  const exponent = parseInt(power, 10);
  if (!Number.isFinite(exponent)) {
    return original;
  }
  return mantissa.replace(/^\+/, "") + "e" + exponent;
}

/** The value without a leading `<` or `>` and surrounding space. */
function withoutComparator(value) {
  const trimmed = value.trim();
  return /^[<>]/.test(trimmed) ? trimmed.slice(1).trim() : trimmed;
}

/**
 * The number the written value denotes, as a string Number() accepts. Used for
 * validation and range comparison only, never for what the screen shows.
 */
export function normalizeScientificNotation(value) {
  if (typeof value !== "string" || value.trim() === "") {
    return value;
  }
  const candidate = value.trim();
  const exponent = EXPONENT_NOTATION.exec(candidate);
  if (exponent) {
    return canonical(exponent[1], exponent[2], value);
  }
  const timesTen = TIMES_TEN_NOTATION.exec(candidate);
  if (timesTen) {
    const mantissa =
      timesTen[1] !== undefined ? timesTen[1] : timesTen[2] + "1";
    const power =
      timesTen[3] !== undefined ? timesTen[3] : superscriptToAscii(timesTen[4]);
    return canonical(mantissa, power, value);
  }
  return value;
}

/** Whether the value is written in scientific notation, in any accepted form. */
export function isScientificNotation(value) {
  if (typeof value !== "string") {
    return false;
  }
  const number = withoutComparator(value);
  return EXPONENT_NOTATION.test(number) || TIMES_TEN_NOTATION.test(number);
}

/** The value split into its comparator, its mantissa and the rest, or null. */
export function splitMantissa(value) {
  if (typeof value !== "string") {
    return null;
  }
  const match = MANTISSA_SPLIT.exec(value.trim());
  if (!match || BARE_TEN.test(withoutComparator(value))) {
    return null;
  }
  return { prefix: match[1], mantissa: match[2], rest: match[3] };
}

/**
 * The decimal places the technologist entered. For a value in scientific
 * notation these are the mantissa's places, so 1.5e5 and 1.5×10⁵ each carry
 * one, not three.
 */
export function decimalPlaces(value) {
  const parts = splitMantissa(value);
  if (!parts) {
    return 0;
  }
  const dot = parts.mantissa.indexOf(".");
  return dot === -1 ? 0 : parts.mantissa.length - dot - 1;
}

/**
 * Whether the value carries more decimal places than the test reports to. A
 * test reporting whole numbers does not constrain a mantissa: rounding 1.5e5
 * to 2e5 would change the result, where 150000 is stored exactly.
 */
export function exceedsDecimalPlaces(value, places) {
  if (places === undefined || places === null || isNaN(places) || places < 0) {
    return false;
  }
  if (isScientificNotation(value)) {
    return places > 0 && decimalPlaces(value) > places;
  }
  return decimalPlaces(value) > places;
}

/**
 * Rounds the mantissa to the places the test reports to, leaving the notation
 * exactly as written: 3.567 x 10^3 reported to two places becomes
 * 3.57 x 10^3.
 */
export function roundMantissa(value, places) {
  const parts = splitMantissa(value);
  if (!parts) {
    return value;
  }
  return parts.prefix + Number(parts.mantissa).toFixed(places) + parts.rest;
}

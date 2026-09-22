/**
 * Scientific notation for numeric results, shared by the legacy and the
 * unified Results Entry screens.
 *
 * A value written as 1.5e5, 1.5E+05, 1.5 x 10^5, 1.5×10⁵ or 10⁻³ is read as
 * scientific notation and normalized to canonical e-notation (1.5e5, 1e-3),
 * which Number() and the server both parse and which the server stores.
 * Superscript digits count only as the power of ten in that form: a bare "3²"
 * is ambiguous (nine, or three hundred?) and is left alone so that it fails
 * numeric validation. Plain decimals pass through unchanged.
 */
const MANTISSA = "[+-]?(?:\\d+\\.?\\d*|\\.\\d+)";
const EXPONENT_NOTATION = new RegExp(`^(${MANTISSA})[eE]([+-]?\\d+)$`);
const TIMES_TEN_NOTATION = new RegExp(
  `^(?:(${MANTISSA})\\s*[xX×*]\\s*)?([+-]?)10\\s*(?:\\^\\s*([+-]?\\d+)|([⁺⁻]?[⁰¹²³⁴⁵⁶⁷⁸⁹]+))$`,
);
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

/** The mantissa and exponent of a value in e-notation, or null. */
export function splitExponentNotation(value) {
  if (typeof value !== "string") {
    return null;
  }
  const match = EXPONENT_NOTATION.exec(value.trim());
  return match ? { mantissa: match[1], exponent: match[2] } : null;
}

export function isExponentNotation(value) {
  return splitExponentNotation(value) !== null;
}

/**
 * The decimal places of a plain decimal, or of the mantissa when the value is
 * in e-notation: "1.5e5" carries one place, not three.
 */
export function decimalPlaces(value) {
  if (typeof value !== "string") {
    return 0;
  }
  const parts = splitExponentNotation(value);
  const number = parts ? parts.mantissa : value.trim();
  const dot = number.indexOf(".");
  return dot === -1 ? 0 : number.length - dot - 1;
}

/**
 * Whether the value carries more decimal places than the test reports to. In
 * scientific notation the places are the mantissa's, and a test reporting
 * whole numbers (0 places) does not constrain the mantissa: rounding 1.5e5 to
 * 2e5 would change the result, where 150000 is stored exactly.
 */
export function exceedsDecimalPlaces(value, places) {
  if (places === undefined || places === null || isNaN(places) || places < 0) {
    return false;
  }
  if (isExponentNotation(value)) {
    return places > 0 && decimalPlaces(value) > places;
  }
  return decimalPlaces(value) > places;
}

/** Rounds to the configured decimal places, keeping the power of ten. */
export function roundToDecimalPlaces(value, places) {
  const parts = splitExponentNotation(value);
  if (parts) {
    return Number(parts.mantissa).toFixed(places) + "e" + parts.exponent;
  }
  return parseFloat(value).toFixed(places);
}

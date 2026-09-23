/**
 * Reading a GS1-128 barcode off a delivery.
 *
 * A GS1 element string is application identifiers and their values run
 * together: `01` then 14 digits of GTIN, `17` then 6 digits of expiry, `10`
 * then a batch number of variable length. Fixed-length identifiers need no
 * terminator; variable-length ones run to a group separator or to the end of
 * the string, which is why the order the fields were printed in changes where
 * they end.
 *
 * Scope, deliberately narrow: this reads the four identifiers a lab delivery
 * actually carries and copies everything else through untouched. A parser that
 * claims to understand the whole GS1 specification and quietly gets an
 * unfamiliar identifier wrong is worse than one that says it did not recognise
 * something.
 *
 * NOTE FOR DEPLOYMENT: the identifier set below is the published GS1 standard,
 * not a guess, but which identifiers a given supplier prints and whether a
 * scanner emits the group separator at all are site facts. If labels here do
 * not parse, the raw scan is kept on the line for exactly that diagnosis —
 * check what the scanner emits before changing this table.
 */

/** ASCII 29, the group separator a scanner substitutes for GS1's FNC1. */
const GROUP_SEPARATOR = "";

/**
 * The identifiers this reads. `length` is the fixed value length in
 * characters; a null length means the value runs to a separator or the end.
 */
const IDENTIFIERS = {
  "01": { key: "gtin", length: 14 },
  10: { key: "lotNumber", length: null },
  17: { key: "expirationDate", length: 6 },
  21: { key: "serial", length: null },
};

/** A GS1 date is YYMMDD, and a day of 00 means "end of that month". */
const parseGs1Date = (value) => {
  if (!/^\d{6}$/.test(value)) return null;
  const year = 2000 + Number(value.slice(0, 2));
  const month = Number(value.slice(2, 4));
  const day = Number(value.slice(4, 6));
  if (month < 1 || month > 12) return null;
  // Day 00 is legal and means the last day of the month — a real distinction on
  // reagent labels, where an expiry is often quoted to the month.
  const resolvedDay = day === 0 ? new Date(year, month, 0).getDate() : day;
  if (resolvedDay < 1 || resolvedDay > 31) return null;
  const iso = `${year}-${String(month).padStart(2, "0")}-${String(
    resolvedDay,
  ).padStart(2, "0")}`;
  return iso;
};

/**
 * Reads a scanned string.
 *
 * Always returns an object carrying the raw scan, so a label that does not
 * parse can still be acted on by hand and reported accurately. `recognised`
 * says whether any identifier was understood — a plain product barcode with no
 * identifiers at all is not a failure, it is a UPC, and is returned as one.
 */
export const parseGs1 = (scanned) => {
  const raw = (scanned || "").trim();
  const result = { raw, recognised: false, fields: {}, unparsed: [] };
  if (!raw) return result;

  // A bare numeric code with no identifier structure is an ordinary product
  // barcode. EAN-8 is 8 digits, UPC-A is 12, EAN-13 is 13, GTIN-14 is 14.
  //
  // Eight digits are ambiguous — they are also identifier 17 plus a six-digit
  // date. A product code wins: an expiry with nothing to attach it to
  // identifies nothing, and no label prints one on its own.
  if (/^\d{8}$|^\d{12,14}$/.test(raw)) {
    result.fields.gtin = raw;
    result.recognised = true;
    return result;
  }

  let rest = raw;
  // Some scanners prefix the whole string with a separator; it carries no value.
  while (rest.startsWith(GROUP_SEPARATOR)) rest = rest.slice(1);

  while (rest.length >= 2) {
    const ai = rest.slice(0, 2);
    const definition = IDENTIFIERS[ai];
    if (!definition) {
      // An identifier this does not know. Stop rather than guess where its
      // value ends: misreading a length would corrupt every field after it.
      result.unparsed.push(rest);
      break;
    }

    rest = rest.slice(2);
    let value;
    if (definition.length != null) {
      value = rest.slice(0, definition.length);
      rest = rest.slice(definition.length);
    } else {
      const end = rest.indexOf(GROUP_SEPARATOR);
      value = end === -1 ? rest : rest.slice(0, end);
      rest = end === -1 ? "" : rest.slice(end + 1);
    }

    if (!value) {
      result.unparsed.push(ai);
      break;
    }

    if (definition.key === "expirationDate") {
      const iso = parseGs1Date(value);
      if (iso) {
        result.fields.expirationDate = iso;
        result.recognised = true;
      } else {
        result.unparsed.push(`${ai}${value}`);
      }
    } else {
      result.fields[definition.key] = value;
      result.recognised = true;
    }

    while (rest.startsWith(GROUP_SEPARATOR)) rest = rest.slice(1);
  }

  return result;
};

/**
 * A GTIN and a UPC can be the same product written to different lengths: a
 * 12-digit UPC-A is a GTIN-14 with two leading zeros. Comparing them as typed
 * would fail to match stock the lab has already defined.
 */
export const gtinMatches = (a, b) => {
  if (!a || !b) return false;
  const strip = (value) => String(value).trim().replace(/^0+/, "");
  return strip(a) === strip(b);
};

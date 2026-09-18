/** ASCII 29, the group separator a scanner substitutes for GS1's FNC1. */
const GROUP_SEPARATOR = "";

/** A null length means the value runs to a group separator or the end. */
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
  const resolvedDay = day === 0 ? new Date(year, month, 0).getDate() : day;
  if (resolvedDay < 1 || resolvedDay > 31) return null;
  const iso = `${year}-${String(month).padStart(2, "0")}-${String(
    resolvedDay,
  ).padStart(2, "0")}`;
  return iso;
};

export const parseGs1 = (scanned) => {
  const raw = (scanned || "").trim();
  const result = { raw, recognised: false, fields: {}, unparsed: [] };
  if (!raw) return result;

  // A bare EAN-8, UPC-A, EAN-13 or GTIN-14. Eight digits could also be AI 17
  // and a date, but no label prints a lone expiry, so it is read as EAN-8.
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
      // Without a known length, nothing after this identifier can be read.
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

/** A UPC or EAN left-padded with zeros is the same product's GTIN-14. */
export const gtinMatches = (a, b) => {
  if (!a || !b) return false;
  const strip = (value) => String(value).trim().replace(/^0+/, "");
  return strip(a) === strip(b);
};

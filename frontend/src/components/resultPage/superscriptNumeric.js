/**
 * Converts Unicode superscript digits that follow a numeric character into
 * scientific 'e' notation so downstream numeric parsers (isNaN, Double.parseDouble)
 * accept them. Example: "3²" -> "3e2", "3.5³" -> "3.5e3", "3⁻²" -> "3e-2".
 *
 * Non-superscript strings pass through unchanged, so existing inputs like
 * "3", "3.14", or "3e2" behave exactly as before.
 */

const SUPERSCRIPT_TO_ASCII = {
  "⁰": "0",
  "¹": "1",
  "²": "2",
  "³": "3",
  "⁴": "4",
  "⁵": "5",
  "⁶": "6",
  "⁷": "7",
  "⁸": "8",
  "⁹": "9",
  "⁺": "+",
  "⁻": "-",
};

const SUPERSCRIPT_RUN = /([0-9.])([⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻]+)/g;

export function convertSuperscriptToScientific(value) {
  if (typeof value !== "string" || value.length === 0) {
    return value;
  }
  return value.replace(SUPERSCRIPT_RUN, (_match, mantissaChar, supers) => {
    let ascii = "";
    for (const ch of supers) {
      ascii += SUPERSCRIPT_TO_ASCII[ch] || ch;
    }
    return mantissaChar + "e" + ascii;
  });
}

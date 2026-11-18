/**
 * Checks a price typed in rupees, with the same rules as the server
 * (org.openelisglobal.payment.Money.parseRupeesToPaise), so a bad value is
 * caught before saving. Returns a React Intl message id, or null when the value
 * is fine. Blank is fine: it means "no price".
 */

/** ₹10,00,000, the same limit as Money.MAX_PRICE_PAISE. */
export const MAX_PRICE_RUPEES = 1000000;

const PLAIN_AMOUNT = /^\d+(\.\d*)?$/;

export const priceErrorId = (value) => {
  const trimmed = value == null ? "" : String(value).trim();
  if (trimmed === "") {
    return null;
  }
  if (!PLAIN_AMOUNT.test(trimmed)) {
    return "error.price.format";
  }
  const [whole, decimals = ""] = trimmed.split(".");
  if (decimals.replace(/0+$/, "").length > 2) {
    return "error.price.decimals";
  }
  // Check the digit count first, so a huge input is refused before it is
  // turned into a Number (which would lose precision), then compare in paise.
  const wholeDigits = whole.replace(/^0+(?=\d)/, "");
  const maxDigits = String(MAX_PRICE_RUPEES);
  if (wholeDigits.length > maxDigits.length) {
    return "error.price.max";
  }
  const paise =
    Number(wholeDigits) * 100 + Number((decimals + "00").slice(0, 2));
  if (paise > MAX_PRICE_RUPEES * 100) {
    return "error.price.max";
  }
  return null;
};

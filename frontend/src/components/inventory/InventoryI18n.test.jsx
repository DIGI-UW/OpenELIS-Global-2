import fs from "fs";
import path from "path";
import { describe, it, expect } from "vitest";
import messages from "../../languages/en.json";

/**
 * The module's i18n contract, checked against the source rather than against a
 * list someone maintains by hand.
 *
 * react-intl renders a missing id as the raw dotted key, and nothing fails: the
 * Update QC Status modal shipped six of them — its notes label, its select
 * label and its whole failure warning — and every test passed throughout,
 * because no test rendered that modal. This is the bug class OGC-1052 recorded,
 * and it can only be caught by reading the keys the code actually asks for.
 */
const DIR = path.join(__dirname);

const sourceFiles = fs
  .readdirSync(DIR)
  .filter((f) => /\.(jsx|js)$/.test(f) && !f.includes(".test."))
  .map((f) => ({ name: f, text: fs.readFileSync(path.join(DIR, f), "utf8") }));

// Only literal ids can be read statically. Keys assembled at runtime (the
// lotStatus/qcStatus prefixes, and the backend's own error codes) are checked
// separately below.
const LITERAL_ID =
  /(?:FormattedMessage[^>]*?\bid=|formatMessage\(\s*\{\s*id:\s*)["']([a-z][\w.]*)["']/gi;

const referencedKeys = () => {
  const found = new Map();
  for (const { name, text } of sourceFiles) {
    for (const [, key] of text.matchAll(LITERAL_ID)) {
      if (!found.has(key)) found.set(key, name);
    }
  }
  return found;
};

describe("inventory i18n", () => {
  it("asks for no message id that en.json does not define", () => {
    const missing = [...referencedKeys().entries()]
      .filter(([key]) => !(key in messages))
      .map(([key, file]) => `${key}  (${file})`);

    expect(missing).toEqual([]);
  });

  it("reads a meaningful number of keys, so a broken matcher cannot pass vacuously", () => {
    expect(referencedKeys().size).toBeGreaterThan(200);
  });

  it("defines every status value the board renders by prefix", () => {
    // InventoryItemsBoard builds these ids by concatenation, so the scan above
    // cannot see them and a missing one shows as a raw key on a live row.
    const lotStatuses = [
      "ACTIVE",
      "IN_USE",
      "CONSUMED",
      "EXPIRED",
      "DISPOSED",
      "QUARANTINED",
    ];
    const qcStatuses = ["PENDING", "PASSED", "FAILED", "QUARANTINED"];

    const absent = [
      ...lotStatuses.map((s) => `lot.status.${s}`),
      ...qcStatuses.map((s) => `lot.qcStatus.${s}`),
    ].filter((key) => !(key in messages));

    expect(absent).toEqual([]);
  });

  it("defines every error code the inventory backend can send to a form", () => {
    // InventoryItemForm translates err.errorCode straight from the response
    // body, so a code with no key renders as the raw code to the user.
    const backendCodes = [
      "reports.error.unknownReportType",
      "reports.error.dateRangeRequired",
      "reports.error.unknownExportFormat",
      "reports.error.invalidDate",
      "inventory.item.error.duplicateCode",
      "inventory.tags.error.blank",
    ];

    expect(backendCodes.filter((c) => !(c in messages))).toEqual([]);
  });

  it("leaves no user-facing English hardcoded in a validation message", () => {
    // setError with a string literal puts untranslated English in the banner.
    const offenders = [];
    for (const { name, text } of sourceFiles) {
      for (const line of text.split("\n")) {
        const m = line.match(/setError\(\s*["'](.+?)["']\s*\)/);
        if (m) offenders.push(`${name}: ${m[1]}`);
      }
    }

    expect(offenders).toEqual([]);
  });
});

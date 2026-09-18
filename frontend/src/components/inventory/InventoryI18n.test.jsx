import fs from "fs";
import path from "path";
import { describe, it, expect } from "vitest";
import messages from "../../languages/en.json";

const DIR = path.join(__dirname);

const sourceFiles = fs
  .readdirSync(DIR)
  .filter((f) => /\.(jsx|js)$/.test(f) && !f.includes(".test."))
  .map((f) => ({ name: f, text: fs.readFileSync(path.join(DIR, f), "utf8") }));

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

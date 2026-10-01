import fs from "node:fs";
import path from "node:path";

// OGC-1378: a date picker returns local midnight, and toISOString() converts it
// to UTC, so east of UTC the calendar date taken from it is the day before the
// one picked. Calendar dates are formatted with toLocalIsoDate() instead.
const PATTERN =
  /toISOString\(\)\s*\.\s*(slice\(\s*0\s*,\s*10\s*\)|substring\(\s*0\s*,\s*10\s*\)|split\(\s*["']T["']\s*\)\s*\[\s*0\s*\])/;

// These build the Date from a UTC value and read it back in UTC (a typed
// yyyy-MM-dd is a real calendar day, or a UTC-built range), so no zone shift is
// involved. Each is pinned to its exact line, so a new use in the same file is
// still caught.
const UTC_ROUND_TRIPS = {
  "components/microbiology/MicrobiologyRoutes.js":
    "new Date(`${value}T00:00:00Z`).toISOString().slice(0, 10) === value;",
  "components/microbiology/WhonetRoutes.js":
    "new Date(`${value}T00:00:00Z`).toISOString().slice(0, 10) === value;",
  "components/reports/CustomDataExport/CustomDataExport.jsx":
    "new Date(day).toISOString().slice(0, 10) === value",
  "components/reports/vectorSurveillance/ManualEntryHelper.jsx":
    "const fmt = (d) => d.toISOString().slice(0, 10);",
};

const SRC = path.resolve(__dirname, "../..");

const sourceFiles = (dir) =>
  fs.readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      return entry.name === "__tests__" ? [] : sourceFiles(full);
    }
    return /\.(js|jsx|ts|tsx)$/.test(entry.name) &&
      !/\.(test|spec)\.[jt]sx?$/.test(entry.name)
      ? [full]
      : [];
  });

describe("calendar dates are never taken from toISOString() (OGC-1378)", () => {
  test("no source file formats a calendar date in UTC", () => {
    const offenders = sourceFiles(SRC).flatMap((file) => {
      const relative = path.relative(SRC, file).split(path.sep).join("/");
      return fs
        .readFileSync(file, "utf8")
        .split("\n")
        .map((line, index) => ({ line, index }))
        .filter(
          ({ line }) =>
            PATTERN.test(line) && UTC_ROUND_TRIPS[relative] !== line.trim(),
        )
        .map(({ index }) => `${relative}:${index + 1} use toLocalIsoDate()`);
    });

    expect(offenders).toEqual([]);
  });

  test("the scan recognises every shape of the pattern", () => {
    [
      "d.toISOString().slice(0, 10)",
      'd.toISOString().split("T")[0]',
      "d.toISOString().split('T')[0]",
      "d.toISOString().substring(0,10)",
    ].forEach((line) => expect(PATTERN.test(line)).toBe(true));
    expect(PATTERN.test("d.toISOString()")).toBe(false);
  });

  test("every allowed line is still in its file, so the list cannot go stale", () => {
    Object.entries(UTC_ROUND_TRIPS).forEach(([file, line]) =>
      expect(
        fs
          .readFileSync(path.join(SRC, file), "utf8")
          .split("\n")
          .map((text) => text.trim()),
      ).toContain(line),
    );
  });
});

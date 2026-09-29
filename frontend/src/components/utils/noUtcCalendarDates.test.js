import fs from "node:fs";
import path from "node:path";

// OGC-1378: a date picker returns local midnight, and toISOString() converts it
// to UTC, so east of UTC the calendar date taken from it is the day before the
// one picked. Calendar dates are formatted with toLocalIsoDate() instead.
const PATTERN =
  /toISOString\(\)\s*\.\s*(slice\(\s*0\s*,\s*10\s*\)|substring\(\s*0\s*,\s*10\s*\)|split\(\s*["']T["']\s*\)\s*\[\s*0\s*\])/;

// These build the Date from a UTC string and read it back in UTC to check that
// a typed yyyy-MM-dd is a real calendar day, so no zone shift is involved.
const UTC_ROUND_TRIPS = [
  "components/microbiology/MicrobiologyRoutes.js",
  "components/microbiology/WhonetRoutes.js",
  "components/reports/CustomDataExport/CustomDataExport.jsx",
  "components/reports/vectorSurveillance/ManualEntryHelper.jsx",
];

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
    const offenders = sourceFiles(SRC)
      .filter(
        (file) =>
          !UTC_ROUND_TRIPS.includes(
            path.relative(SRC, file).split(path.sep).join("/"),
          ),
      )
      .flatMap((file) =>
        fs
          .readFileSync(file, "utf8")
          .split("\n")
          .map((line, index) => ({ line, index }))
          .filter(({ line }) => PATTERN.test(line))
          .map(
            ({ index }) =>
              `${path.relative(SRC, file)}:${index + 1} use toLocalIsoDate()`,
          ),
      );

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

  test("the allowed files still exist, so the list cannot go stale", () => {
    UTC_ROUND_TRIPS.forEach((file) =>
      expect(fs.existsSync(path.join(SRC, file))).toBe(true),
    );
  });
});

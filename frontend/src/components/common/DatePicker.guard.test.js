import fs from "fs";
import path from "path";

const SRC = path.resolve(__dirname, "../..");
const ALLOWED = new Set([
  path.join("components", "common", "DatePicker.jsx"),
  path.join("components", "common", "CustomDatePicker.jsx"),
]);

const sources = (dir) =>
  fs.readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      return entry.name === "node_modules" || entry.name === "__tests__"
        ? []
        : sources(full);
    }
    return /\.(jsx?|tsx?)$/.test(entry.name) && !/\.test\./.test(entry.name)
      ? [full]
      : [];
  });

const CARBON_IMPORT = /import\s*\{([^}]*)\}\s*from\s*["']@carbon\/react["']/g;

/**
 * OGC-1635: a Carbon DatePicker used directly drops typed dates; screens use
 * the DatePicker in components/common, which commits them.
 */
describe("date pickers", () => {
  it("are imported from components/common, not from Carbon", () => {
    const offenders = sources(SRC)
      .filter((file) => !ALLOWED.has(path.relative(SRC, file)))
      .filter((file) =>
        [...fs.readFileSync(file, "utf8").matchAll(CARBON_IMPORT)].some(
          ([, names]) =>
            names
              .split(",")
              .map((name) => name.trim().split(/\s+as\s+/)[0])
              .includes("DatePicker"),
        ),
      )
      .map((file) => path.relative(SRC, file));
    expect(offenders).toEqual([]);
  });
});

import fs from "node:fs";
import path from "node:path";

// The lab's today, this year or a range ending now follows the lab server's
// time zone; a browser in another zone would otherwise pre-fill, accept or
// filter by a day that is not the lab's. Calendar values read the clock through
// labNow() / labToday() (labClock.ts).
const PATTERN = /new Date\(\s*\)/;

// Real instants: elapsed-time windows, timestamps sent or stored as instants,
// and times shown against other instants in the browser's zone. Each is pinned
// to its exact line, so a new use in the same file is still caught.
const INSTANTS = {
  "components/coldStorage/CorrectiveActions.jsx": [
    "let end = new Date();",
    "const parseDate = (dateValue) => toDate(dateValue) ?? new Date();",
  ],
  "components/coldStorage/DeviceHistoryExpansion.jsx": [
    "const end = new Date();",
    "`temperature-history-${device?.id || device?.freezerId}-${new Date().toISOString()}.csv`,",
  ],
  "components/coldStorage/FreezerMonitoringDashboard.jsx": [
    "setLastUpdated(new Date().toISOString());",
  ],
  "components/coldStorage/HistoricalTrends.jsx": ["const end = new Date();"],
  "components/admin/DataExportStatus/DataExportStatus.jsx": [
    "setLastRefreshed(new Date());",
  ],
  "components/qc/dashboard/QCDashboard.jsx": ["setLastUpdated(new Date());"],
  "components/qc/dashboard/qcDashboardUtils.js": ["const now = new Date();"],
  "components/qc/controlLots/ControlLotSetup.jsx": [
    "activationDate: new Date().toISOString(),",
  ],
  "components/qa/overview/RecentActivity.jsx": [
    "return date.toDateString() === new Date().toDateString()",
  ],
  "components/patient/resultsViewer/trendline/trendline.component.tsx": [
    "return [new Date(), new Date()];",
    "return [new Date(), new Date(Date.parse(obs[obs.length - 1].obsDatetime))];",
  ],
};

const SRC = path.resolve(__dirname, "../..");

const sourceFiles = (dir) =>
  fs.readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      return ["__tests__", "__mocks__"].includes(entry.name)
        ? []
        : sourceFiles(full);
    }
    return /\.(js|jsx|ts|tsx)$/.test(entry.name) &&
      !/\.(test|spec)\.[jt]sx?$/.test(entry.name)
      ? [full]
      : [];
  });

describe("calendar values read the lab's clock, not the browser's", () => {
  test("no source file takes a calendar value from new Date()", () => {
    const offenders = sourceFiles(SRC).flatMap((file) => {
      const relative = path.relative(SRC, file).split(path.sep).join("/");
      if (relative === "components/utils/labClock.ts") {
        return [];
      }
      const allowed = INSTANTS[relative] || [];
      return fs
        .readFileSync(file, "utf8")
        .split("\n")
        .map((line, index) => ({ line, index }))
        .filter(
          ({ line }) => PATTERN.test(line) && !allowed.includes(line.trim()),
        )
        .map(({ index }) => `${relative}:${index + 1} use labNow()`);
    });

    expect(offenders).toEqual([]);
  });

  test("every allowed line is still in its file, so the list cannot go stale", () => {
    Object.entries(INSTANTS).forEach(([file, lines]) => {
      const text = fs
        .readFileSync(path.join(SRC, file), "utf8")
        .split("\n")
        .map((line) => line.trim());
      lines.forEach((line) => expect(text).toContain(line));
    });
  });
});

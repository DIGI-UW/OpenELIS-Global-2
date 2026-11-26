import { afterEach, describe, expect, test, vi } from "vitest";
import { createIntl } from "react-intl";
import messages from "../../../../languages/en.json";
import { getFromOpenElisServer } from "../../../utils/Utils";
import { loadLabClock, resetLabClock } from "../../../utils/labClock";
import {
  describeControl,
  formatRunTime,
  missingQcItems,
  missingQcTag,
  testVerdictText,
} from "./runFormat";

vi.mock("../../../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  getFromOpenElisServer: vi.fn(),
}));

const intl = createIntl({ locale: "en", messages });

// 20:00 UTC is 10:00 the next day in Kiritimati (UTC+14) and 09:00 the same
// day in Pago Pago (UTC-11), whatever zone the test runner is in.
const RECORDED = "2031-03-05T20:00:00Z";

const labIn = async (timezone: string) => {
  vi.mocked(getFromOpenElisServer).mockImplementation((url, callback) =>
    callback({ timezone }),
  );
  await loadLabClock();
};

describe("run times", () => {
  afterEach(() => {
    resetLabClock();
    vi.mocked(getFromOpenElisServer).mockReset();
  });

  test("read the lab's clock in the site format with a 24-hour time", async () => {
    await labIn("Pacific/Kiritimati");
    expect(formatRunTime(intl, RECORDED, "MM/dd/yyyy")).toBe(
      "03/06/2031 10:00",
    );
    expect(formatRunTime(intl, RECORDED, "dd/MM/yyyy")).toBe(
      "06/03/2031 10:00",
    );

    await labIn("Pacific/Pago_Pago");
    expect(formatRunTime(intl, RECORDED, "MM/dd/yyyy")).toBe(
      "03/05/2031 09:00",
    );
  });

  test("a control reads the same in every place it is shown", async () => {
    await labIn("Pacific/Kiritimati");
    expect(
      describeControl(
        intl,
        {
          level: "NORMAL",
          lotNumber: "LOT-N",
          observed: "120",
          expected: "100 ± 5",
          verdictSource: "COMPUTED",
          operator: "Open ELIS",
          recordedAt: RECORDED,
        },
        "MM/dd/yyyy",
      ),
    ).toBe(
      "NORMAL · LOT-N · 120 against 100 ± 5 · Judged against the QC target · Open ELIS · 03/06/2031 10:00",
    );
    expect(
      describeControl(
        intl,
        { lotNumber: "KIT-7", observed: "INVALID", verdictSource: "DECLARED" },
        "MM/dd/yyyy",
      ),
    ).toBe("KIT-7 · Invalid · Declared by the technician");
  });
});

describe("what a test lacks (OGC-1200 FR-H1)", () => {
  test("names each kind it lacks, and the control when none is named", () => {
    expect(missingQcTag(intl, ["BLANK"])).toBe("No blank");
    expect(missingQcTag(intl, ["DUPLICATE", "CONTROL"])).toBe(
      "No control or duplicate",
    );
    expect(missingQcTag(intl, null)).toBe("No control");
    expect(missingQcItems(intl, ["CONTROL", "BLANK", "DUPLICATE"])).toBe(
      "control, blank, and duplicate",
    );
    expect(testVerdictText(intl, "NO_CONTROL", ["BLANK"])).toBe("No blank");
    expect(testVerdictText(intl, "PASS_WITH_WARNING", ["BLANK"])).toBe(
      "Pass with warning",
    );
  });
});

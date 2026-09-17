/**
 * rangeUtils — OGC-949 M7 / OGC-1153.
 *
 * formatAgeDays renders the age bounds shown in the coverage-gap panel and in the
 * red activation-acknowledgment modal, so an open-ended bound must never leak the
 * raw value into that text (OGC-1153: "18 years – Infinity years").
 */
import { createIntl } from "react-intl";
import { formatAgeDays, toDays, DAYS_PER } from "./rangeUtils";
import messages from "../../../../languages/en.json";

const intl = createIntl({ locale: "en", messages });

const gapRange = (from, to) =>
  intl.formatMessage(
    { id: "label.testCatalog.ranges.gapRange" },
    { from: formatAgeDays(from, intl), to: formatAgeDays(to, intl) },
  );

describe("formatAgeDays", () => {
  it('renders the open-ended label for the string "Infinity" Jackson sends', () => {
    expect(formatAgeDays("Infinity", intl)).toBe("no upper limit");
    expect(formatAgeDays("Infinity", intl)).not.toMatch(/Infinity/);
  });

  it("renders the open-ended label for a numeric Infinity", () => {
    expect(formatAgeDays(Infinity, intl)).toBe("no upper limit");
    expect(formatAgeDays(Number.POSITIVE_INFINITY, intl)).toBe(
      "no upper limit",
    );
  });

  it("renders a lower-bound label for -Infinity so the range still reads correctly", () => {
    expect(formatAgeDays(-Infinity, intl)).toBe("no lower limit");
  });

  it("reads correctly inside the coverage-gap sentence", () => {
    expect(gapRange(18 * DAYS_PER.years, "Infinity")).toBe(
      "18 years – no upper limit",
    );
  });

  it("renders nothing for absent or unparseable input", () => {
    expect(formatAgeDays(null, intl)).toBe("");
    expect(formatAgeDays(undefined, intl)).toBe("");
    expect(formatAgeDays("not-a-number", intl)).toBe("");
  });

  it("keeps the finite day / month / year formatting", () => {
    expect(formatAgeDays(30, intl)).toBe("30 days");
    expect(formatAgeDays(59, intl)).toBe("59 days");
    expect(formatAgeDays(90, intl)).toBe("3 months");
    expect(formatAgeDays(729, intl)).toBe("24 months");
    expect(formatAgeDays(730, intl)).toBe("2 years");
    expect(formatAgeDays(18 * DAYS_PER.years, intl)).toBe("18 years");
    expect(formatAgeDays("6570", intl)).toBe("18 years");
  });
});

describe("toDays", () => {
  it("converts value + unit to days", () => {
    expect(toDays("2", "days")).toBe(2);
    expect(toDays("1", "years")).toBe(365);
    expect(toDays("", "days")).toBeNull();
    expect(toDays("abc", "days")).toBeNull();
  });
});

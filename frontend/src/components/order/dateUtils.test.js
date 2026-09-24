import { describe, expect, it } from "vitest";
import {
  currentLocalTime,
  formatHoldingMinutes,
  holdingDeadline,
  shortestHoldingMinutes,
  daysBetweenIsoDates,
  formatIsoDateForBackend,
  isCollectionDateBeforeAdmissionDate,
  normalizeDateForState,
  todayLocalIso,
} from "./dateUtils";

describe("order date utilities", () => {
  it("serializes ISO dates using the configured deployment locale", () => {
    expect(formatIsoDateForBackend("2026-08-13", "en-US")).toBe("08/13/2026");
    expect(formatIsoDateForBackend("2026-08-13", "fr-FR")).toBe("13/08/2026");
  });

  it("normalizes configured API dates into canonical ISO state", () => {
    expect(normalizeDateForState("13/08/2026", "fr-FR")).toBe("2026-08-13");
    expect(normalizeDateForState("08/13/2026", "en-US")).toBe("2026-08-13");
    expect(normalizeDateForState("2026-08-13", "fr-FR")).toBe("2026-08-13");
  });

  it("detects a collection date before admission without rejecting empty dates", () => {
    expect(
      isCollectionDateBeforeAdmissionDate("2026-08-02", "2026-08-03"),
    ).toBe(true);
    expect(
      isCollectionDateBeforeAdmissionDate("2026-08-03", "2026-08-03"),
    ).toBe(false);
    expect(isCollectionDateBeforeAdmissionDate("", "2026-08-03")).toBe(false);
  });

  it("computes calendar days between admission and collection", () => {
    expect(daysBetweenIsoDates("2026-08-03", "2026-08-07")).toBe(4);
    expect(daysBetweenIsoDates("", "2026-08-07")).toBeNull();
  });

  // OGC-1201 O: vector orders were stamped from toISOString(), i.e. UTC, so a
  // late-evening collection west of Greenwich was stored as the next day.
  it("stamps the site's own calendar day, not the UTC one", () => {
    const lateEveningLocal = new Date(2026, 7, 13, 23, 30);

    expect(todayLocalIso(lateEveningLocal)).toBe("2026-08-13");
    expect(currentLocalTime(lateEveningLocal)).toBe("23:30");
  });

  it("stamps an early-morning time with a padded hour", () => {
    expect(currentLocalTime(new Date(2026, 7, 13, 6, 5))).toBe("06:05");
  });
});

// OGC-1201 AP: holding time was already on the per-test payload but never
// shown at order time, and the only over-hold flag anywhere measures from
// collection rather than from lab receipt.
describe("test holding limits", () => {
  it("takes the tightest limit among a sample's tests", () => {
    expect(
      shortestHoldingMinutes([
        { timeHolding: "1440" },
        { timeHolding: "120" },
        { timeHolding: "" },
      ]),
    ).toBe(120);
    expect(shortestHoldingMinutes([])).toBeNull();
    expect(shortestHoldingMinutes([{ timeHolding: "0" }])).toBeNull();
    expect(shortestHoldingMinutes([{ timeHolding: "abc" }])).toBeNull();
  });

  it("anchors the deadline on lab receipt, not collection", () => {
    const sample = {
      collectionDate: "2026-08-13",
      collectionTime: "08:00",
      receivedDate: "2026-08-14",
      receivedTime: "09:30",
    };
    const deadline = holdingDeadline(sample, [{ timeHolding: "120" }]);
    expect(deadline.getFullYear()).toBe(2026);
    expect(deadline.getMonth()).toBe(7);
    expect(deadline.getDate()).toBe(14);
    expect(deadline.getHours()).toBe(11);
    expect(deadline.getMinutes()).toBe(30);
  });

  it("falls back to collection when receipt is not recorded yet", () => {
    const deadline = holdingDeadline(
      { collectionDate: "2026-08-13", collectionTime: "08:00" },
      [{ timeHolding: "60" }],
    );
    expect(deadline.getDate()).toBe(13);
    expect(deadline.getHours()).toBe(9);
  });

  it("derives nothing without a limit or an anchor", () => {
    expect(holdingDeadline({ receivedDate: "2026-08-14" }, [])).toBeNull();
    expect(holdingDeadline({}, [{ timeHolding: "60" }])).toBeNull();
  });

  it("reads a limit back in hours and minutes", () => {
    expect(formatHoldingMinutes(30)).toBe("30 min");
    expect(formatHoldingMinutes(120)).toBe("2 h");
    expect(formatHoldingMinutes(150)).toBe("2 h 30 min");
    expect(formatHoldingMinutes(null)).toBe("");
  });
});

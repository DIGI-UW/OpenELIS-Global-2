import { describe, expect, it } from "vitest";
import {
  daysBetweenIsoDates,
  formatIsoDateForBackend,
  isCollectionDateBeforeAdmissionDate,
  normalizeDateForState,
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
});

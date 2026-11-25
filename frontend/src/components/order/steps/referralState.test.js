import { describe, expect, test } from "vitest";
import { hasReferral, isFullyReferred } from "./referralState";

const tube = (overrides = {}) => ({
  sampleTypeId: "2",
  sampleItemId: "501",
  tests: [{ id: "7" }],
  ...overrides,
});

describe("referralState", () => {
  test("a staged referral and an open saved referral both count", () => {
    expect(hasReferral(tube({ referralItems: [{ pendingSave: true }] }))).toBe(
      true,
    );
    expect(
      hasReferral(tube({ referralItems: [{ referralStatus: "DRAFT" }] })),
    ).toBe(true);
    expect(
      hasReferral(tube({ referralItems: [{ referralStatus: "CANCELLED" }] })),
    ).toBe(false);
    expect(hasReferral(tube())).toBe(false);
  });

  test("an order is fully referred only when every tube with a test is referred", () => {
    const referred = tube({ referralItems: [{ referralStatus: "DRAFT" }] });
    const inHouse = tube({ sampleItemId: "502" });
    const noTests = tube({ sampleItemId: "503", tests: [] });
    const qc = tube({ sampleItemId: "504", qcMetadata: { qcType: "BLANK" } });

    expect(isFullyReferred([referred, noTests, qc])).toBe(true);
    expect(isFullyReferred([referred, inHouse])).toBe(false);
    expect(isFullyReferred([noTests])).toBe(false);
    expect(isFullyReferred([])).toBe(false);
  });
});

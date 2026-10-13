import { describe, expect, it } from "vitest";
import { samplesFromPendingRequests } from "./pendingRequestSamples";

/**
 * Prepare Samples rebuilds its samples from the order's pending requests after
 * the page opens. Input typed into a card before that load finished was lost
 * (found walking OGC-1400: a quantity typed right after Save and next).
 */
const request = {
  sampleTypeId: "2",
  sampleTypeName: "Serum",
  tests: [{ id: "1" }],
};

describe("samplesFromPendingRequests", () => {
  it("keeps a quantity and unit typed before the requests arrived", () => {
    const atLoad = [{ sampleTypeId: "2", quantity: "", quantityUnit: "" }];
    const current = [{ sampleTypeId: "2", quantity: "3", quantityUnit: "49" }];

    const [sample] = samplesFromPendingRequests([request], atLoad, current);

    expect(sample.quantity).toBe("3");
    expect(sample.quantityUnit).toBe("49");
    expect(sample.tests).toEqual([{ id: "1" }]);
  });

  it("carries the Step 2 fields the order held and leaves the stored entry date out", () => {
    const atLoad = [
      {
        sampleTypeId: "2",
        collectorId: "7",
        receivedDate: "2026-10-11",
        collectionDate: "2026-10-01",
      },
    ];

    const [sample] = samplesFromPendingRequests([request], atLoad, atLoad);

    expect(sample.collectorId).toBe("7");
    expect(sample.receivedDate).toBe("2026-10-11");
    expect(sample.collectionDate).toBeUndefined();
  });

  it("takes the request as it is when the sample type changed", () => {
    const atLoad = [{ sampleTypeId: "5", quantity: "1" }];
    const current = [{ sampleTypeId: "5", quantity: "9" }];

    const [sample] = samplesFromPendingRequests([request], atLoad, current);

    expect(sample).toEqual(request);
  });
});

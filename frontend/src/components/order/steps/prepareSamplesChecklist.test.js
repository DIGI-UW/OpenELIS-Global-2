/**
 * OGC-1419 — what Prepare Samples still needs before Save and next. The
 * collector is optional: many tubes arrive with no name on them, so a sample
 * with a type and a collection date and time can move on without one.
 */
import { prepareSamplesToContinue } from "./prepareSamplesChecklist";

const intl = {
  formatMessage: ({ id }, values) =>
    values ? `${id}:${JSON.stringify(values)}` : id,
};

const serum = (fields = {}) => ({
  sampleTypeId: "2",
  sampleTypeName: "Serum",
  collectionDate: "2026-10-02",
  collectionTime: "09:30",
  collectorId: "",
  labPerformedSampling: false,
  ...fields,
});

const itemsFor = (samples, options = {}) =>
  prepareSamplesToContinue({
    samples,
    labNumber: "DEV0126",
    admissionDate: "",
    consentSatisfied: true,
    intl,
    ...options,
  });

describe("prepareSamplesToContinue (OGC-1419)", () => {
  it("lets a sample with a type, a collection date and time and no collector continue", () => {
    expect(itemsFor([serum()])).toEqual([]);
  });

  it("never lists the collector, whatever else is missing", () => {
    const items = itemsFor([serum({ collectionTime: "" }), serum()], {
      consentSatisfied: false,
    });

    expect(items.map((item) => item.id)).toEqual([
      "collectionTime-0",
      "order.continue.item.consent",
    ]);
    expect(items.some((item) => item.id.startsWith("collector"))).toBe(false);
  });

  it("still asks for a sample type, and for the collection date and time", () => {
    expect(itemsFor([{ sampleTypeId: "" }]).map((item) => item.id)).toEqual([
      "order.continue.item.sampleType",
    ]);
    expect(
      itemsFor([serum({ collectionDate: "" })]).map((item) => item.label),
    ).toEqual([
      'order.continue.item.collectionTime:{"sample":"DEV0126-1 Serum"}',
    ]);
  });

  it("skips rejected samples and flags a collection before admission", () => {
    expect(
      itemsFor([serum({ collectionTime: "", sampleRejected: true })]),
    ).toEqual([]);
    expect(
      itemsFor([serum({ collectionDate: "2026-10-01" })], {
        admissionDate: "2026-10-02",
      }).map((item) => item.id),
    ).toEqual(["collectionConflict-0"]);
  });
});

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
  tests: [{ id: "7", name: "Glucose" }],
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

describe("prepareSamplesToContinue measured temperature (OGC-1424)", () => {
  it("lists a measured temperature that cannot be stored and points at its field", () => {
    const items = itemsFor([serum({ arrivalTemperature: "999" })]);

    expect(items).toEqual([
      expect.objectContaining({
        id: "arrivalTemperature-0",
        targetId: "arrivalTemperature-0",
      }),
    ]);
  });

  it("lets a blank or plausible temperature continue", () => {
    expect(
      itemsFor([
        serum({ arrivalTemperature: "" }),
        serum({ arrivalTemperature: "4,5" }),
      ]),
    ).toEqual([]);
  });
});

describe("prepareSamplesToContinue — a sample with no tests (OGC-1443)", () => {
  it("names the sample that has no tests, since the server refuses to save it", () => {
    expect(itemsFor([serum(), serum({ tests: [] })])).toEqual([
      {
        id: "noTests-1",
        label:
          'order.continue.item.noTestsOnSample:{"sample":"DEV0126-2 Serum"}',
        targetId: "sampleType-1",
      },
    ]);
  });

  it("accepts a sample carrying a panel instead of single tests", () => {
    expect(itemsFor([serum({ tests: [], panels: [{ id: "3" }] })])).toEqual([]);
  });

  it("leaves out a rejected sample", () => {
    expect(
      itemsFor([serum(), serum({ tests: [], sampleRejected: true })]),
    ).toEqual([]);
  });
});

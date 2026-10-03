import { describe, expect, test } from "vitest";
import { applySampleTypeUpdate, newSampleKey } from "./sampleTypeUpdate";

const sample = (overrides) => ({
  index: 1,
  sampleRejected: false,
  rejectionReason: "",
  requestReferralEnabled: false,
  referralItems: [],
  sampleTypeId: "",
  sampleXML: null,
  panels: [],
  tests: [],
  ...overrides,
});

const histology = { id: "322", name: "Histopathology examination" };

describe("applySampleTypeUpdate", () => {
  test("an unticked last test leaves the sample with no tests", () => {
    const samples = [sample({ sampleTypeId: "37", tests: [histology] })];
    const next = applySampleTypeUpdate(samples, {
      selectedTests: [],
      sampleObjectIndex: 0,
    });
    expect(next[0].tests).toEqual([]);
  });

  test("a removed last panel leaves the sample with no panels", () => {
    const samples = [sample({ panels: [{ id: "5", name: "Serologie VIH" }] })];
    expect(
      applySampleTypeUpdate(samples, {
        selectedPanels: [],
        sampleObjectIndex: 0,
      })[0].panels,
    ).toEqual([]);
  });

  test("choosing Select sample type clears the stored sample type", () => {
    const samples = [sample({ sampleTypeId: "106" })];
    expect(
      applySampleTypeUpdate(samples, {
        sampleTypeId: "",
        sampleObjectIndex: 0,
      })[0].sampleTypeId,
    ).toBe("");
  });

  test("switching the referral off and emptying its rows is kept", () => {
    const samples = [
      sample({
        requestReferralEnabled: true,
        referralItems: [{ testId: "322", institute: "7" }],
      }),
    ];
    let next = applySampleTypeUpdate(samples, {
      requestReferralEnabled: false,
      sampleObjectIndex: 0,
    });
    next = applySampleTypeUpdate(next, {
      referralItems: [],
      sampleObjectIndex: 0,
    });
    expect(next[0].requestReferralEnabled).toBe(false);
    expect(next[0].referralItems).toEqual([]);
  });

  test("never modifies the samples it is given", () => {
    const first = sample({ sampleTypeId: "2", tests: [histology] });
    const samples = [first, sample()];
    const next = applySampleTypeUpdate(samples, {
      selectedTests: [],
      sampleObjectIndex: 0,
    });
    expect(first.tests).toEqual([histology]);
    expect(samples[0]).toBe(first);
    expect(next[1]).toBe(samples[1]);
  });

  test("only the reported sample changes", () => {
    const samples = [
      sample({ sampleTypeId: "2" }),
      sample({ sampleTypeId: "4" }),
    ];
    const next = applySampleTypeUpdate(samples, {
      selectedTests: [histology],
      sampleObjectIndex: 1,
    });
    expect(next[0].tests).toEqual([]);
    expect(next[1].tests).toEqual([histology]);
  });

  test("undefined and null leave the field as it was", () => {
    const samples = [
      sample({ sampleTypeId: "2", sampleXML: { quantity: "1" } }),
    ];
    expect(
      applySampleTypeUpdate(samples, { sampleXML: null, sampleObjectIndex: 0 }),
    ).toBe(samples);
    expect(
      applySampleTypeUpdate(samples, {
        rejectionReason: undefined,
        sampleObjectIndex: 0,
      }),
    ).toBe(samples);
  });

  test("an index with no sample is ignored", () => {
    const samples = [sample()];
    expect(
      applySampleTypeUpdate(samples, {
        selectedTests: [],
        sampleObjectIndex: 3,
      }),
    ).toBe(samples);
  });
});

describe("newSampleKey", () => {
  test("never repeats", () => {
    expect(newSampleKey()).not.toBe(newSampleKey());
  });
});

import { describe, expect, it } from "vitest";
import {
  convertRequestsToSamples,
  mergeCollectedAndPendingSamples,
  toRequestedSampleTypes,
} from "./sampleTypeRequestApi";

describe("convertRequestsToSamples", () => {
  it("keeps the requested quantity when restoring a pending request", () => {
    const samples = convertRequestsToSamples([
      {
        id: "11",
        typeOfSampleId: "5",
        status: "REQUESTED",
        requestedQuantity: 2.5,
      },
      {
        id: "12",
        typeOfSampleId: "6",
        status: "REQUESTED",
        requestedQuantity: null,
      },
    ]);

    expect(samples[0].quantity).toBe("2.5");
    expect(samples[1].quantity).toBe("");
  });

  it("preserves workflow and Method metadata when restoring selected tests", () => {
    const selectedTest = {
      id: "42",
      name: "Blood culture",
      cultureWorkflowType: "BACTERIOLOGY",
      methods: [
        {
          methodId: "7",
          methodName: "Blood Culture Standard",
          isDefault: true,
        },
      ],
    };

    const samples = convertRequestsToSamples([
      {
        id: "11",
        typeOfSampleId: "5",
        typeOfSampleName: "Blood",
        requestedTests: "42",
        requestedTestNames: "Blood culture",
        requestedTestDetails: [selectedTest],
        status: "REQUESTED",
      },
    ]);

    expect(samples[0].tests).toEqual([selectedTest]);
  });

  it("retains compatibility with pending requests that only contain IDs and names", () => {
    const samples = convertRequestsToSamples([
      {
        id: "11",
        typeOfSampleId: "5",
        requestedTests: "42",
        requestedTestNames: "Blood culture",
      },
    ]);

    expect(samples[0].tests).toEqual([{ id: "42", name: "Blood culture" }]);
  });
});

describe("requested specimens sent with the order", () => {
  it("carries the entered type, quantity, tests and panels in entry order", () => {
    const requested = toRequestedSampleTypes([
      {
        sampleTypeId: "5",
        quantity: "2.5",
        quantityUnit: "9",
        tests: [{ id: "42" }, { id: "43" }],
        panels: [{ id: "7" }],
      },
      { sampleTypeId: "6" },
    ]);

    expect(requested).toEqual([
      {
        typeOfSampleId: "5",
        requestedQuantity: 2.5,
        unitOfMeasureId: "9",
        requestedTests: "42,43",
        requestedPanels: "7",
      },
      {
        typeOfSampleId: "6",
        requestedQuantity: null,
        unitOfMeasureId: null,
        requestedTests: "",
        requestedPanels: "",
      },
    ]);
  });

  it("ignores rows where no sample type was chosen", () => {
    expect(toRequestedSampleTypes([{ quantity: "3" }, {}])).toEqual([]);
    expect(toRequestedSampleTypes()).toEqual([]);
  });
});

describe("mergeCollectedAndPendingSamples", () => {
  const collected = { sampleItemId: "900", sampleTypeId: "5" };
  const pendingRequest = {
    id: "11",
    typeOfSampleId: "6",
    status: "REQUESTED",
  };

  it("keeps pending requests alongside already collected specimens", () => {
    const merged = mergeCollectedAndPendingSamples(
      [collected],
      [pendingRequest],
      null,
    );

    expect(merged).toHaveLength(2);
    expect(merged[0].sampleItemId).toBe("900");
    expect(merged[1].sampleTypeRequestId).toBe("11");
  });

  it("drops requests that were already fulfilled or cancelled", () => {
    const merged = mergeCollectedAndPendingSamples(
      [collected],
      [
        { id: "12", typeOfSampleId: "6", status: "COLLECTED" },
        { id: "13", typeOfSampleId: "7", status: "CANCELLED" },
      ],
      null,
    );

    expect(merged).toHaveLength(1);
    expect(merged[0].sampleItemId).toBe("900");
  });

  it("falls back when neither source has rows", () => {
    const fallback = [{ index: 0 }];
    expect(mergeCollectedAndPendingSamples([], [], fallback)).toBe(fallback);
  });
});

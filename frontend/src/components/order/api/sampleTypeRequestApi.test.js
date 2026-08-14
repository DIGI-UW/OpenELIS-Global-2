import { describe, expect, it } from "vitest";
import { convertRequestsToSamples } from "./sampleTypeRequestApi";

describe("convertRequestsToSamples", () => {
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

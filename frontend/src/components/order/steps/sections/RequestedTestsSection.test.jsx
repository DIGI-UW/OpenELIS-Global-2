import { getAssignableSamplesOfType } from "./RequestedTestsSection";

describe("requested test specimen assignment", () => {
  it("excludes rejected specimens when a replacement has the same type", () => {
    const samples = [
      { sampleItemId: "rejected", sampleTypeId: "5", sampleRejected: true },
      { sampleItemId: "replacement", sampleTypeId: "5", sampleRejected: false },
    ];

    expect(getAssignableSamplesOfType(samples, "5")).toEqual([
      expect.objectContaining({ sampleItemId: "replacement", index: 1 }),
    ]);
  });
});

import { describe, expect, test } from "vitest";
import { samplesMissingTests, samplesWithTests } from "./orderSamples";

const blank = { sampleTypeId: "", tests: [] };
const withTest = (sampleTypeId, id) => ({
  sampleTypeId,
  tests: [{ id, name: "Test " + id }],
});

describe("samplesWithTests", () => {
  test("keeps an added sample when the first sample is blank", () => {
    const added = withTest("37", "322");
    expect(samplesWithTests([blank, added])).toEqual([added]);
  });

  test("keeps every sample with tests in step order", () => {
    const serum = withTest("2", "4");
    const plasma = withTest("3", "3");
    expect(samplesWithTests([serum, blank, plasma])).toEqual([serum, plasma]);
  });

  test("is empty when no sample has a test", () => {
    expect(samplesWithTests([blank, { sampleTypeId: "2", tests: [] }])).toEqual(
      [],
    );
    expect(samplesWithTests(undefined)).toEqual([]);
  });
});

describe("samplesMissingTests", () => {
  test("names a sample that has a type but no test", () => {
    expect(
      samplesMissingTests([
        withTest("2", "4"),
        { sampleTypeId: "37", tests: [] },
      ]),
    ).toEqual([2]);
  });

  test("ignores a blank sample and complete samples", () => {
    expect(samplesMissingTests([blank, withTest("37", "322")])).toEqual([]);
    expect(samplesMissingTests(undefined)).toEqual([]);
  });
});

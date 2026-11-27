import { describe, expect, test } from "vitest";
import { createIntl } from "react-intl";
import messages from "../../languages/en.json";
import {
  missingTestsTitle,
  samplesMissingTests,
  samplesWithTests,
} from "./orderSamples";

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

describe("missingTestsTitle", () => {
  const intl = createIntl({ locale: "en", messages });

  test("names the sample's type as the Result Reporting headings do", () => {
    const samples = [withTest("2", "4"), { sampleTypeId: "37", tests: [] }];

    expect(
      missingTestsTitle(intl, samples, 2, { 37: "Respiratory Swab" }),
    ).toBe(
      "Sample 2: Respiratory Swab has no test. Add a test or remove the sample before saving.",
    );
  });

  test("falls back to the sample number while the type names are unknown", () => {
    const samples = [{ sampleTypeId: "37", tests: [] }];

    expect(missingTestsTitle(intl, samples, 1, {})).toBe(
      "Sample 1 has a sample type but no test. Add a test or remove the sample before saving.",
    );
  });
});

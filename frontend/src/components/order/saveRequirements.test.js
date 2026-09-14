import { describe, expect, it } from "vitest";
import { createIntl } from "react-intl";
import messages from "../../languages/en.json";
import {
  describeUnmetRequirements,
  unmetRequirements,
} from "./saveRequirements";

const intl = createIntl({ locale: "en", messages });

const requirements = (labNumber, patient, sampleType) => [
  { met: labNumber, labelId: "order.save.requirement.labNumber" },
  { met: patient, labelId: "order.save.requirement.patient" },
  { met: sampleType, labelId: "order.save.requirement.sampleType" },
];

describe("order save requirements", () => {
  // OGC-1201 AK: the gate checks the lab number, but the message told the user
  // to add a patient and a sample type — the two things they already had.
  it("names only the lab number when that is all that is missing", () => {
    expect(
      describeUnmetRequirements(intl, requirements(false, true, true)),
    ).toBe("Add a lab number before saving.");
  });

  it("joins several missing requirements into one sentence", () => {
    expect(
      describeUnmetRequirements(intl, requirements(false, true, false)),
    ).toBe("Add a lab number and at least one sample type before saving.");
    expect(
      describeUnmetRequirements(intl, requirements(false, false, false)),
    ).toBe(
      "Add a lab number, a patient and at least one sample type before saving.",
    );
  });

  it("says nothing when the gate is satisfied", () => {
    expect(
      describeUnmetRequirements(intl, requirements(true, true, true)),
    ).toBe("");
    expect(unmetRequirements(requirements(true, true, true))).toEqual([]);
  });
});

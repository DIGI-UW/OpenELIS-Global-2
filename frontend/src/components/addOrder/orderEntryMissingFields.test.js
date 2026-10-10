import { describe, expect, test } from "vitest";
import { missingFieldRequirements } from "./orderEntryMissingFields";

const validationError = (...inner) => ({ inner });

describe("missingFieldRequirements", () => {
  test("names the patient sex and birth date that keep Submit disabled", () => {
    expect(
      missingFieldRequirements(
        validationError(
          { path: "patientProperties.birthDateForDisplay", message: "x" },
          { path: "patientProperties.gender", message: "y" },
        ),
      ).map((requirement) => requirement.labelId),
    ).toEqual([
      "order.save.requirement.patientBirthDate",
      "order.save.requirement.patientSex",
    ]);
  });

  test("lists a field once and keeps unknown messages as they are", () => {
    const requirements = missingFieldRequirements(
      validationError(
        { path: "sampleOrderItems.providerFirstName", message: "a" },
        { path: "sampleOrderItems.providerLastName", message: "b" },
        { path: "somethingElse", message: "Something else is wrong" },
      ),
    );
    expect(requirements).toEqual([
      { met: false, labelId: "order.save.requirement.provider" },
      { met: false, labelId: null, text: "Something else is wrong" },
    ]);
  });

  test("is empty when the form is valid", () => {
    expect(missingFieldRequirements([])).toEqual([]);
    expect(missingFieldRequirements(undefined)).toEqual([]);
  });
});

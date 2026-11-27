import { describe, expect, test } from "vitest";
import { createPatientValidationSchema } from "./CreatePatientValidationShema";

const patient = (overrides = {}) => ({
  nationalId: "NAT-1",
  gender: "F",
  birthDateForDisplay: "01/01/1990",
  email: "",
  ...overrides,
});

const errorsFor = async (config, values) => {
  try {
    await createPatientValidationSchema(config).validate(values, {
      abortEarly: false,
    });
    return [];
  } catch (error) {
    return error.inner.map((inner) => inner.path);
  }
};

describe("createPatientValidationSchema sex and age settings", () => {
  test("sex and birth date stay required by default", async () => {
    const errors = await errorsFor(
      {},
      patient({ gender: "", birthDateForDisplay: "" }),
    );
    expect(errors).toContain("gender");
    expect(errors).toContain("birthDateForDisplay");
  });

  test("sex can be left blank when PATIENT_SEX_REQUIRED is false", async () => {
    const errors = await errorsFor(
      { PATIENT_SEX_REQUIRED: "false" },
      patient({ gender: "" }),
    );
    expect(errors).not.toContain("gender");
  });

  test("birth date can be left blank when PATIENT_AGE_REQUIRED is false", async () => {
    const errors = await errorsFor(
      { PATIENT_AGE_REQUIRED: "false" },
      patient({ birthDateForDisplay: "" }),
    );
    expect(errors).not.toContain("birthDateForDisplay");
  });

  test("a birth date that is entered must still be a valid date", async () => {
    const errors = await errorsFor(
      { PATIENT_AGE_REQUIRED: "false" },
      patient({ birthDateForDisplay: "1990-01-01" }),
    );
    expect(errors).toContain("birthDateForDisplay");
  });

  test("a patient arriving with no sex or birth date at all passes when both are optional", async () => {
    const errors = await errorsFor(
      { PATIENT_SEX_REQUIRED: "false", PATIENT_AGE_REQUIRED: "false" },
      patient({ gender: null, birthDateForDisplay: null }),
    );
    expect(errors).toEqual([]);
  });

  test("a null sex is still refused when sex is required", async () => {
    const errors = await errorsFor({}, patient({ gender: null }));
    expect(errors).toContain("gender");
  });
});

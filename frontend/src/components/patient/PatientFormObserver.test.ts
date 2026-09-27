import {
  derivePatientUpdateStatus,
  mergePatientIntoOrderFormValues,
} from "./PatientFormObserver";

describe("derivePatientUpdateStatus", () => {
  const selectedPatient = {
    patientPK: "9001000",
    firstName: "John",
    lastName: "TEST-Smith",
    nationalId: "E2E-PAT-001",
    subjectNumber: "SUBJECT-1",
    primaryPhone: "555-0101",
    email: "john.test@openelis.org",
    birthDateForDisplay: "01/15/1990",
    patientContact: {
      id: "contact-1",
      person: {
        firstName: "Jane",
        lastName: "Contact",
        primaryPhone: "555-0102",
        email: "contact@openelis.org",
      },
    },
    addressHierarchy: {
      addressHierarchy_0: "region-1",
      addressHierarchy_1: "district-1",
    },
  };

  it("keeps existing patients at no action until edited", () => {
    const formValues = {
      ...selectedPatient,
      patientUpdateStatus: "NO_ACTION",
      addressHierarchy_0: "region-1",
      addressHierarchy_1: "district-1",
      photo: "avatar-bytes",
    };

    expect(
      derivePatientUpdateStatus(formValues, selectedPatient, "NO_ACTION"),
    ).toBe("NO_ACTION");
  });

  it("switches existing patients to update after a real edit", () => {
    const editedValues = {
      ...selectedPatient,
      primaryPhone: "555-9999",
    };

    expect(
      derivePatientUpdateStatus(editedValues, selectedPatient, "NO_ACTION"),
    ).toBe("UPDATE");
  });

  it("preserves add semantics for new patients", () => {
    expect(derivePatientUpdateStatus({}, {}, "ADD")).toBe("ADD");
  });
});

describe("mergePatientIntoOrderFormValues", () => {
  it("reuses the previous order form object when patient state is unchanged", () => {
    const orderFormValues = {
      sampleOrderItems: { labNo: "ABC-123" },
      patientUpdateStatus: "NO_ACTION",
      patientProperties: {
        patientPK: "9001000",
        firstName: "John",
        lastName: "TEST-Smith",
        patientUpdateStatus: "NO_ACTION",
      },
    };

    const result = mergePatientIntoOrderFormValues(
      orderFormValues,
      {
        patientPK: "9001000",
        firstName: "John",
        lastName: "TEST-Smith",
      },
      "NO_ACTION",
    );

    expect(result).toBe(orderFormValues);
  });

  it("updates patientProperties while preserving unrelated order state", () => {
    const orderFormValues = {
      sampleOrderItems: { labNo: "ABC-123" },
      patientUpdateStatus: "NO_ACTION",
      patientProperties: {
        patientPK: "9001000",
        firstName: "John",
        lastName: "TEST-Smith",
        patientUpdateStatus: "NO_ACTION",
      },
    };

    const result = mergePatientIntoOrderFormValues(
      orderFormValues,
      {
        patientPK: "9001000",
        firstName: "John",
        lastName: "TEST-Smith",
        primaryPhone: "555-9999",
      },
      "UPDATE",
    );

    expect(result).not.toBe(orderFormValues);
    expect(result.sampleOrderItems).toEqual(orderFormValues.sampleOrderItems);
    expect(result.patientUpdateStatus).toBe("UPDATE");
    expect(result.patientProperties.primaryPhone).toBe("555-9999");
    expect(result.patientProperties.patientUpdateStatus).toBe("UPDATE");
  });
});

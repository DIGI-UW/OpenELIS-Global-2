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

describe("the patient the order already holds", () => {
  const orderWithSavedPatient = {
    sampleOrderItems: { labNo: "DEV01260000000000552" },
    patientUpdateStatus: "NO_ACTION",
    patientProperties: {
      patientPK: "115",
      firstName: "Nia",
      lastName: "Qadup",
      nationalId: "QA1407N1",
      patientUpdateStatus: "NO_ACTION",
    },
  };

  it("is kept when a form without a patient id asks to add the patient", () => {
    const result = mergePatientIntoOrderFormValues(
      orderWithSavedPatient,
      {
        firstName: "Nia",
        lastName: "Qadup",
        nationalId: "QA1407N1",
        primaryPhone: "0788123456",
      },
      "ADD",
    );

    expect(result.patientProperties.patientPK).toBe("115");
    expect(result.patientProperties.patientUpdateStatus).toBe("UPDATE");
    expect(result.patientUpdateStatus).toBe("UPDATE");
    expect(result.patientProperties.primaryPhone).toBe("0788123456");
  });

  it("is kept with the status the form derived when it is not an add", () => {
    const result = mergePatientIntoOrderFormValues(
      orderWithSavedPatient,
      { firstName: "Nia", lastName: "Qadup", nationalId: "QA1407N1" },
      "NO_ACTION",
    );

    expect(result.patientProperties.patientPK).toBe("115");
    expect(result.patientProperties.patientUpdateStatus).toBe("NO_ACTION");
  });

  it("gives way to a form for another patient", () => {
    const result = mergePatientIntoOrderFormValues(
      orderWithSavedPatient,
      { patientPK: "7", firstName: "Mary", lastName: "Kila" },
      "NO_ACTION",
    );

    expect(result.patientProperties.patientPK).toBe("7");
    expect(result.patientProperties.firstName).toBe("Mary");
  });

  it("does not exist for an order whose patient was cleared, so a new one is added", () => {
    const result = mergePatientIntoOrderFormValues(
      {
        ...orderWithSavedPatient,
        patientUpdateStatus: "",
        patientProperties: { patientPK: "", firstName: "", lastName: "" },
      },
      { firstName: "Mary", lastName: "Kila", nationalId: "NID-2" },
      "ADD",
    );

    expect(result.patientProperties.patientPK).toBeUndefined();
    expect(result.patientProperties.patientUpdateStatus).toBe("ADD");
    expect(result.patientUpdateStatus).toBe("ADD");
  });
});

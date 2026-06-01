import {
  buildRequestorSessionDefaults,
  deriveDestinationType,
  mergePrefillFields,
  validateBrf02RequestForm,
} from "./biorepoImportFormHelpers";

describe("biorepoImportFormHelpers", () => {
  const session = {
    firstName: "Jane",
    lastName: "Doe",
    loginName: "jdoe",
    loginLabUnit: "Immunology",
  };

  test("buildRequestorSessionDefaults uses provided lab unit over session", () => {
    expect(buildRequestorSessionDefaults(session, "CTD")).toEqual({
      requestorName: "Jane Doe",
      requesterLabUnit: "CTD",
      requesterContactInfo: "Jane Doe (jdoe) · CTD",
    });
  });

  test("mergePrefillFields does not overwrite non-biorepository lab unit with biorepository", () => {
    expect(
      mergePrefillFields(
        { requesterLabUnit: "CTD" },
        { requesterLabUnit: "Biorepository Laboratory" },
      ),
    ).toEqual({ requesterLabUnit: "CTD" });
  });

  test("mergePrefillFields only fills empty fields", () => {
    expect(
      mergePrefillFields(
        { requestorName: "Custom Name", requesterLabUnit: "" },
        { requestorName: "Jane Doe", requesterLabUnit: "Immunology" },
      ),
    ).toEqual({
      requestorName: "Custom Name",
      requesterLabUnit: "Immunology",
    });
  });

  test("deriveDestinationType maps destroy/return choices", () => {
    expect(deriveDestinationType(true)).toBe("INTERNAL_LAB");
    expect(deriveDestinationType(false)).toBe("ANALYSIS_RETURN");
    expect(deriveDestinationType(null)).toBe("ANALYSIS_RETURN");
  });

  test("validateBrf02RequestForm requires destroy/return choice", () => {
    const errors = validateBrf02RequestForm(
      {
        requestorName: "Jane Doe",
        requesterLabUnit: "CTD",
        principalInvestigator: "Dr. Smith",
        projectTitle: "Study A",
        requesterContactInfo: "Jane Doe (jdoe)",
        intendedUseDescription: "Analysis",
        samplesWillBeDestroyed: null,
      },
      [{ id: 1, requestedSampleType: "Plasma", quantityRequested: 1 }],
    );

    expect(errors).toContain(
      "Please indicate whether samples will be destroyed after use",
    );
  });

  test("validateBrf02RequestForm requires return date when samples return", () => {
    const errors = validateBrf02RequestForm(
      {
        requestorName: "Jane Doe",
        requesterLabUnit: "CTD",
        principalInvestigator: "Dr. Smith",
        projectTitle: "Study A",
        requesterContactInfo: "Jane Doe (jdoe)",
        intendedUseDescription: "Analysis",
        samplesWillBeDestroyed: false,
        estimatedReturnDate: null,
      },
      [{ id: 1, requestedSampleType: "Plasma", quantityRequested: 1 }],
    );

    expect(errors).toContain(
      "Estimated return date is required when samples will be returned",
    );
  });
});

import {
  buildBiorepositoryTransferPayload,
  validateBiorepositoryTransferRequest,
  validateSampleItemForBiorepositoryTransfer,
} from "./biorepositoryTransferValidation";

describe("biorepositoryTransferValidation", () => {
  const validSample = {
    sampleItemId: 10,
    externalId: "S-10",
    sampleType: "Serum",
    collectionDate: "2026-01-01",
    quantity: 5,
    sampleCondition: "Good",
    preservationMedium: "RNAlater",
  };

  test("validateSampleItemForBiorepositoryTransfer passes valid sample", () => {
    expect(validateSampleItemForBiorepositoryTransfer(validSample)).toEqual([]);
  });

  test("validateSampleItemForBiorepositoryTransfer parses quantity with unit suffix", () => {
    expect(
      validateSampleItemForBiorepositoryTransfer({
        ...validSample,
        quantity: "5.0 mL",
      }),
    ).toEqual([]);
  });

  test("validateSampleItemForBiorepositoryTransfer reports missing fields", () => {
    const errors = validateSampleItemForBiorepositoryTransfer({
      sampleItemId: 11,
    });
    expect(errors.length).toBeGreaterThan(0);
    expect(errors.join(" ")).toMatch(/identifier/);
    expect(errors.join(" ")).toMatch(/sample type/);
    expect(errors.join(" ")).toMatch(/collection date/);
    expect(errors.join(" ")).toMatch(/volume/);
    expect(errors.join(" ")).toMatch(/condition/);
    expect(errors.join(" ")).toMatch(/preservative/);
  });

  test("validateBiorepositoryTransferRequest requires project and reason", () => {
    const errors = validateBiorepositoryTransferRequest({
      projectName: "",
      transferReason: "",
      samples: [validSample],
    });
    expect(errors).toContain("Project name is required");
    expect(errors).toContain("Transfer reason is required");
  });

  test("buildBiorepositoryTransferPayload trims request fields and includes metadata", () => {
    expect(
      buildBiorepositoryTransferPayload({
        sourceLab: "CTD",
        sampleItemIds: [1, 2],
        projectName: " Study ",
        transferReason: " Archive ",
        itemMetadata: {
          1: {
            collectionDate: "2026-01-01",
            quantity: "5.0 mL",
            unitOfMeasure: " mL ",
            sampleCondition: " Good ",
            preservationMedium: " RNAlater ",
          },
          2: {
            collectionDate: "2026-01-02",
            quantity: 2,
            sampleCondition: "Frozen",
            preservationMedium: "DMSO",
          },
        },
      }),
    ).toEqual({
      sourceLab: "CTD",
      sampleItemIds: [1, 2],
      projectName: "Study",
      requestNotes: "Archive",
      itemMetadata: [
        {
          sampleItemId: 1,
          sourceNotebookId: null,
          sourceNotebookEntryId: null,
          collectionDate: "2026-01-01",
          quantity: 5,
          unitOfMeasure: "mL",
          sampleCondition: "Good",
          preservationMedium: "RNAlater",
        },
        {
          sampleItemId: 2,
          sourceNotebookId: null,
          sourceNotebookEntryId: null,
          collectionDate: "2026-01-02",
          quantity: 2,
          unitOfMeasure: "",
          sampleCondition: "Frozen",
          preservationMedium: "DMSO",
        },
      ],
    });
  });
});

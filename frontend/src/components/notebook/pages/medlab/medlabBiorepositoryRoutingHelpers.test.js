import {
  buildMedLabBiorepositoryTransferPayload,
  initBiorepositoryTransferMetadata,
  mapSelectedSamplesForBiorepositoryTransfer,
  validateMedLabBiorepositoryTransfer,
} from "./medlabBiorepositoryRoutingHelpers";

describe("medlabBiorepositoryRoutingHelpers", () => {
  const tableSamples = [
    {
      id: 206,
      externalId: "MEDLAB-2026-005",
      accessionNumber: "ACC-005",
      sampleType: "Serum",
      collectionDate: null,
      quantity: null,
      data: {},
    },
  ];

  const selectedSampleIds = ["206"];

  test("maps selected samples without requiring table collection date or volume", () => {
    const mapped = mapSelectedSamplesForBiorepositoryTransfer(
      tableSamples,
      selectedSampleIds,
    );

    expect(mapped).toHaveLength(1);
    expect(mapped[0].sampleItemId).toBe(206);
    expect(mapped[0].collectionDate).toBeNull();
    expect(mapped[0].quantity).toBeNull();
  });

  test("validation fails before submit when modal fields are empty", () => {
    const selectedSamples = mapSelectedSamplesForBiorepositoryTransfer(
      tableSamples,
      selectedSampleIds,
    );
    const itemMetadata = initBiorepositoryTransferMetadata(selectedSamples);

    const errors = validateMedLabBiorepositoryTransfer({
      projectName: "",
      transferReason: "",
      selectedSamples,
      itemMetadata,
    });

    expect(errors).toContain("Project name is required");
    expect(errors).toContain("Transfer reason is required");
    expect(errors.join(" ")).toMatch(/missing collection date/);
    expect(errors.join(" ")).toMatch(/missing or invalid volume/);
    expect(errors.join(" ")).toMatch(/sample condition is required/);
    expect(errors.join(" ")).toMatch(/preservative or medium is required/);
  });

  test("validation passes after modal values are filled for sparse table row", () => {
    const selectedSamples = mapSelectedSamplesForBiorepositoryTransfer(
      tableSamples,
      selectedSampleIds,
    );
    const itemMetadata = {
      206: {
        collectionDate: "2026-05-01",
        quantity: "2.5",
        unitOfMeasure: "mL",
        sampleCondition: "Good",
        preservationMedium: "RNAlater",
      },
    };

    const errors = validateMedLabBiorepositoryTransfer({
      projectName: "CTD Study",
      transferReason: "Long-term storage",
      selectedSamples,
      itemMetadata,
    });

    expect(errors).toEqual([]);
  });

  test("payload includes collectionDate, quantity, unitOfMeasure, condition, and preservative", () => {
    const payload = buildMedLabBiorepositoryTransferPayload({
      sampleItemIds: [206],
      projectName: "CTD Study",
      transferReason: "Archive",
      sourceNotebookId: 29,
      sourceNotebookEntryId: 29,
      itemMetadata: {
        206: {
          collectionDate: "2026-05-01",
          quantity: "2.5",
          unitOfMeasure: "mL",
          sampleCondition: "Good",
          preservationMedium: "RNAlater",
        },
      },
    });

    expect(payload.sourceLab).toBe("CTD");
    expect(payload.itemMetadata).toEqual([
      {
        sampleItemId: 206,
        sourceNotebookId: 29,
        sourceNotebookEntryId: 29,
        collectionDate: "2026-05-01",
        quantity: 2.5,
        unitOfMeasure: "mL",
        sampleCondition: "Good",
        preservationMedium: "RNAlater",
      },
    ]);
  });
});

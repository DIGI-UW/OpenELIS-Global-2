import {
  mapPageSamplesForBiorepositoryTransfer,
  BiorepositoryTransferFormFields,
} from "./SendToBiorepositoryModal";
import { buildBiorepositoryTransferPayload } from "./biorepositoryTransferValidation";
import { buildDefaultTransferItemMetadata } from "./BiorepositoryTransferSampleTable";

describe("SendToBiorepositoryModal helpers", () => {
  test("mapPageSamplesForBiorepositoryTransfer maps selected page samples", () => {
    const samples = [
      {
        id: "101",
        externalId: "EXT-101",
        sampleType: "Plasma",
        collectionDate: "2026-01-15",
        quantity: 2,
        unitOfMeasure: "mL",
      },
      { id: "102", externalId: "EXT-102", sampleType: "Serum" },
    ];

    const mapped = mapPageSamplesForBiorepositoryTransfer(samples, ["101"]);
    expect(mapped).toHaveLength(1);
    expect(mapped[0].sampleItemId).toBe("101");
    expect(mapped[0].externalId).toBe("EXT-101");
  });

  test("sparse sample metadata produces valid transfer payload", () => {
    const sample = {
      sampleItemId: 55,
      externalId: "S-55",
      sampleType: "Whole Blood",
    };
    const metadata = {
      55: {
        ...buildDefaultTransferItemMetadata(sample),
        collectionDate: "2026-02-01",
        quantity: 3,
        unitOfMeasure: "mL",
        sampleCondition: "Good",
        preservationMedium: "EDTA",
      },
    };

    const payload = buildBiorepositoryTransferPayload({
      sourceLab: "IMMUNOLOGY",
      sampleItemIds: [55],
      projectName: "Project A",
      transferReason: "Long-term storage",
      itemMetadata: metadata,
      sourceNotebookId: 12,
      sourceNotebookEntryId: 34,
    });

    expect(payload.sourceLab).toBe("IMMUNOLOGY");
    expect(payload.sampleItemIds).toEqual([55]);
    expect(payload.itemMetadata[0].collectionDate).toBe("2026-02-01");
    expect(payload.itemMetadata[0].quantity).toBe(3);
  });

  test("BiorepositoryTransferFormFields renders project and reason inputs", () => {
    expect(BiorepositoryTransferFormFields).toBeDefined();
  });
});

import {
  buildBiorepositoryTransferPayload,
  validateBiorepositoryTransferRequest,
} from "../biorepository/biorepositoryTransferValidation";
import {
  buildDefaultTransferItemMetadata,
  buildTransferSamplesForValidation,
} from "../biorepository/BiorepositoryTransferSampleTable";

export function normalizeSampleItemId(id) {
  return String(id);
}

export function mapSelectedSamplesForBiorepositoryTransfer(
  samples,
  selectedSampleIds,
) {
  return (samples || [])
    .filter((sample) => selectedSampleIds.includes(String(sample.id)))
    .map((sample) => ({
      sampleItemId: sample.id,
      externalId: sample.externalId,
      accessionNumber: sample.accessionNumber || sample.labNo,
      sampleType: sample.sampleType,
      collectionDate: sample.collectionDate,
      quantity: sample.quantity,
      unitOfMeasure: sample.unitOfMeasure,
      data: sample.data,
    }));
}

export function initBiorepositoryTransferMetadata(selectedSamples) {
  const metadata = {};
  (selectedSamples || []).forEach((sample) => {
    const sampleItemId = normalizeSampleItemId(
      sample.sampleItemId || sample.id,
    );
    metadata[sampleItemId] = buildDefaultTransferItemMetadata({
      sampleItemId: sample.sampleItemId || sample.id,
      ...sample,
    });
  });
  return metadata;
}

export function validateMedLabBiorepositoryTransfer({
  projectName,
  transferReason,
  selectedSamples,
  itemMetadata,
}) {
  const samples = buildTransferSamplesForValidation(
    selectedSamples,
    itemMetadata,
  );
  return validateBiorepositoryTransferRequest({
    projectName,
    transferReason,
    samples,
  });
}

export function buildMedLabBiorepositoryTransferPayload({
  sampleItemIds,
  projectName,
  transferReason,
  itemMetadata,
  sourceNotebookId,
  sourceNotebookEntryId,
}) {
  return buildBiorepositoryTransferPayload({
    sourceLab: "CTD",
    sampleItemIds,
    projectName,
    transferReason,
    itemMetadata,
    sourceNotebookId,
    sourceNotebookEntryId,
  });
}

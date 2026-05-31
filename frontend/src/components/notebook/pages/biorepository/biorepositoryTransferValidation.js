import { parseQuantityValue } from "./biorepositoryQuantityHelpers";

export function validateSampleItemForBiorepositoryTransfer(
  sample,
  sampleItemId,
) {
  const errors = [];
  const id = sampleItemId || sample?.sampleItemId || sample?.id || "unknown";

  const externalId = sample?.externalId || sample?.sampleExternalId;
  const accessionNumber = sample?.accessionNumber;
  if (!externalId && !accessionNumber) {
    errors.push(
      `Sample ${id}: missing sample identifier (external ID or accession number)`,
    );
  }

  const sampleType =
    sample?.sampleType ||
    sample?.typeOfSample ||
    sample?.typeOfSampleDescription ||
    sample?.data?.sampleType;
  if (!sampleType) {
    errors.push(`Sample ${id}: missing sample type`);
  }

  const collectionDate = sample?.collectionDate || sample?.data?.collectionDate;
  if (!collectionDate) {
    errors.push(`Sample ${id}: missing collection date`);
  }

  const quantity = parseQuantityValue(
    sample?.quantity ??
      sample?.volume ??
      sample?.data?.sampleVolume ??
      sample?.data?.volume,
  );
  if (quantity === null || quantity <= 0) {
    errors.push(`Sample ${id}: missing or invalid volume (quantity)`);
  }

  const sampleCondition = sample?.sampleCondition;
  if (!sampleCondition || !String(sampleCondition).trim()) {
    errors.push(`Sample ${id}: sample condition is required`);
  }

  const preservationMedium = sample?.preservationMedium;
  if (!preservationMedium || !String(preservationMedium).trim()) {
    errors.push(`Sample ${id}: preservative or medium is required`);
  }

  return errors;
}

export function validateBiorepositoryTransferRequest({
  projectName,
  transferReason,
  samples,
}) {
  const errors = [];

  if (!projectName || !projectName.trim()) {
    errors.push("Project name is required");
  }
  if (!transferReason || !transferReason.trim()) {
    errors.push("Transfer reason is required");
  }

  (samples || []).forEach((sample) => {
    errors.push(...validateSampleItemForBiorepositoryTransfer(sample));
  });

  return errors;
}

export function buildBiorepositoryTransferPayload({
  sourceLab,
  sampleItemIds,
  projectName,
  transferReason,
  itemMetadata,
}) {
  const metadataList = (sampleItemIds || []).map((sampleItemId) => {
    const metadata = itemMetadata?.[sampleItemId] || {};
    return {
      sampleItemId,
      collectionDate: (metadata.collectionDate || "").trim(),
      quantity: parseQuantityValue(metadata.quantity),
      unitOfMeasure: (metadata.unitOfMeasure || "").trim(),
      sampleCondition: (metadata.sampleCondition || "").trim(),
      preservationMedium: (metadata.preservationMedium || "").trim(),
    };
  });

  return {
    sourceLab,
    sampleItemIds,
    projectName: projectName.trim(),
    requestNotes: transferReason.trim(),
    itemMetadata: metadataList,
  };
}

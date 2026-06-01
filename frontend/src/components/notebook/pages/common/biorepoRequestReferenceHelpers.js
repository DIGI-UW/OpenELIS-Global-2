import { parseQuantityValue } from "../biorepository/biorepositoryQuantityHelpers";

let nextRowId = 1;

export const createEmptyRequestRow = () => ({
  id: `ref-${nextRowId++}`,
  requestedAccessionNumber: "",
  requestedBarcode: "",
  requestedSampleType: "",
  requestedOriginLab: "",
  requestedProjectId: "",
  quantityRequested: "",
  unitOfMeasure: "",
  remark: "",
});

export const validateRequestReferenceRow = (row, index = 0) => {
  const errors = [];
  const label = `Row ${index + 1}`;

  if (!String(row?.requestedSampleType || "").trim()) {
    errors.push(`${label}: sample type is required`);
  }

  const quantity = parseQuantityValue(row?.quantityRequested);
  if (quantity === null || quantity <= 0) {
    errors.push(`${label}: quantity requested must be greater than zero`);
  }

  return errors;
};

export const validateRequestReferenceRows = (rows) => {
  if (!rows || rows.length === 0) {
    return ["Please add at least one requested sample"];
  }
  return rows.flatMap((row, index) => validateRequestReferenceRow(row, index));
};

export const buildReferenceItemsPayload = (rows) =>
  (rows || []).map((row) => ({
    requestedAccessionNumber: String(row.requestedAccessionNumber || "").trim() || null,
    requestedBarcode: String(row.requestedBarcode || "").trim() || null,
    requestedSampleType: String(row.requestedSampleType || "").trim() || null,
    requestedOriginLab: String(row.requestedOriginLab || "").trim() || null,
    requestedProjectId: String(row.requestedProjectId || "").trim() || null,
    quantityRequested: parseQuantityValue(row.quantityRequested),
    unitOfMeasure: String(row.unitOfMeasure || "").trim() || null,
    remark: String(row.remark || "").trim() || null,
  }));

export const formatRequestedReferenceSummary = (item) => {
  const parts = [
    item?.requestedSampleType,
    item?.requestedAccessionNumber,
    item?.requestedBarcode,
    item?.requestedOriginLab,
    item?.requestedProjectId,
  ].filter(Boolean);
  return parts.length > 0 ? parts.join(" · ") : "-";
};

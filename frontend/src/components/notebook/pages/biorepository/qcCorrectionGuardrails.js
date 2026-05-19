const MISSING_DISCREPANCY_TYPES = new Set(["SAMPLE_MISSING", "MISSING_SAMPLE"]);

export const isMissingDiscrepancyType = (discrepancyType) =>
  MISSING_DISCREPANCY_TYPES.has((discrepancyType || "").trim().toUpperCase());

export const getAllowedCorrectionActions = (allActions, discrepancyType) => {
  if (!Array.isArray(allActions)) {
    return [];
  }
  if (isMissingDiscrepancyType(discrepancyType)) {
    return allActions;
  }
  return allActions.filter((action) => action?.id !== "MARK_MISSING");
};

export const hasInvalidMarkMissingLocationFields = (values) => {
  if (!values || values.correctionActionType !== "MARK_MISSING") {
    return false;
  }
  return Boolean(
    values.correctionBoxId ||
    (values.correctionPositionCoordinate || "").trim(),
  );
};

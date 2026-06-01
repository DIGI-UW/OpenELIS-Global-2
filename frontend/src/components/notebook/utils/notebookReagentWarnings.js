const WARNING_LABEL_KEYS = {
  QC_PENDING: "notebook.reagentUsage.warning.qcPending",
  QC_FAILED: "notebook.reagentUsage.warning.qcFailed",
  QC_QUARANTINED: "notebook.reagentUsage.warning.qcQuarantined",
  ZERO_QUANTITY: "notebook.reagentUsage.warning.zeroQuantity",
  INACTIVE_LOT: "notebook.reagentUsage.warning.inactiveLot",
  NO_LOTS: "notebook.reagentUsage.warning.noLots",
};

const WARNING_LABEL_DEFAULTS = {
  QC_PENDING:
    "QC pending: usable for preparation, but review before release.",
  QC_FAILED: "QC failed: verify lot status before use.",
  QC_QUARANTINED: "QC quarantined: do not use until cleared.",
  ZERO_QUANTITY: "No stock on hand for the displayed lot(s).",
  INACTIVE_LOT: "Lot is not active or in use; verify before processing.",
  NO_LOTS: "No lots recorded for this reagent in inventory.",
};

const DROPDOWN_WARNING_SUFFIX = {
  QC_PENDING: "QC pending",
  QC_FAILED: "QC failed",
  QC_QUARANTINED: "QC quarantined",
  ZERO_QUANTITY: "no stock",
  INACTIVE_LOT: "inactive lot",
  NO_LOTS: "no lots",
};

export const normalizeInventoryReagentOption = (item = {}) => {
  const warnings = Array.isArray(item.selectionWarnings)
    ? item.selectionWarnings
    : item.qcStatus === "PENDING"
      ? ["QC_PENDING"]
      : [];

  return {
    id: item.id,
    itemId: item.itemId ?? item.id,
    label: item.name || item.description || String(item.id),
    name: item.name,
    lotNumber: item.lotNumber,
    units: item.units,
    currentQuantity: item.currentQuantity,
    qcStatus: item.qcStatus,
    lotStatus: item.lotStatus,
    totalLots: item.totalLots,
    selectionWarnings: warnings,
  };
};

export const formatReagentWarningLabel = (intl, warningCode) => {
  const id = WARNING_LABEL_KEYS[warningCode];
  if (!id) {
    return warningCode;
  }
  return intl.formatMessage({
    id,
    defaultMessage: WARNING_LABEL_DEFAULTS[warningCode] || warningCode,
  });
};

export const buildReagentOptionLabel = (item, intl) => {
  const baseName = item.name || item.description || item.label || String(item.id);
  const warnings = item.selectionWarnings || [];
  if (warnings.length === 0) {
    return baseName;
  }

  const suffix = warnings
    .map((code) => DROPDOWN_WARNING_SUFFIX[code] || formatReagentWarningLabel(intl, code))
    .join(", ");

  return `${baseName} (${suffix})`;
};

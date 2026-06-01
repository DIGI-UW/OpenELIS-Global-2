/**
 * Normalize transfer source-lab labels for display and filtering.
 * CTD workflow transfers were incorrectly stored as MEDICAL_LAB at creation.
 */
export function formatTransferSourceLab(sourceLab) {
  if (!sourceLab) {
    return sourceLab;
  }
  return sourceLab === "MEDICAL_LAB" ? "CTD" : sourceLab;
}

/**
 * OGC-811 — order attachments are the single attachments store for order
 * entry and both Results pages. An attachment may be scoped to the analysis
 * (and, for multi-component tests, the result component) it documents; the
 * scope is persisted by the backend and returned by the list endpoint.
 */
export interface ScopedAttachment {
  id: number;
  fileName?: string;
  fileType?: string;
  fileSizeBytes?: number;
  uploadedAt?: string;
  /** "" for order-level attachments; set when scoped to an analysis */
  analysisId?: string;
  /** "" unless scoped to a result component (multi-component tests) */
  testResultComponentId?: string;
}

/**
 * Which attachments belong on a given result row: order-level ones (no
 * analysis) always; analysis-scoped ones only on that analysis's rows; and
 * component-scoped ones only on the matching component row.
 */
export const attachmentVisibleOnRow = (
  attachment: ScopedAttachment,
  analysisId?: string,
  componentId?: string,
): boolean => {
  if (!attachment.analysisId) {
    return true;
  }
  if (attachment.analysisId !== String(analysisId ?? "")) {
    return false;
  }
  return (
    !attachment.testResultComponentId ||
    attachment.testResultComponentId === String(componentId ?? "")
  );
};

/** upload URL carrying the row's scope as query params */
export const scopedAttachmentUploadUrl = (
  accessionNumber: string,
  analysisId?: string,
  componentId?: string,
): string => {
  const scope = new URLSearchParams();
  if (analysisId) {
    scope.set("analysisId", String(analysisId));
  }
  if (componentId) {
    scope.set("testResultComponentId", String(componentId));
  }
  const query = scope.toString();
  return `/rest/order/${accessionNumber}/attachments${query ? `?${query}` : ""}`;
};

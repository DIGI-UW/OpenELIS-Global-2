/**
 * OGC-1023 (R4, FR-E3) — NCE result dispositions. Refer-out is deliberately
 * NOT here: it is a separate row action (FR-F1, D3/D16).
 *
 *   CANCEL — voids the analysis via the shipped sample-management endpoint.
 *   REJECT — arms the legacy rejection mechanics (reason + cleared value) on
 *            the row; the tech's e-signature Save applies it (FR-A4 — nothing
 *            bypasses the signature).
 *   RETEST — orders the same test again on the sample item via the shipped
 *            add-tests endpoint; the new analysis appears as pending.
 */
export type NceDisposition = "NONE" | "CANCEL" | "REJECT" | "RETEST";

export interface DispositionRow {
  analysisId?: string;
  sampleItemId?: string;
  testId?: string;
}

export interface DispositionRequest {
  url: string;
  body: Record<string, unknown>;
}

/**
 * The REST call sequence a disposition maps to, executed in order; empty when
 * it rides the row's Save instead. RETEST is two steps: the shipped add-tests
 * endpoint skips tests that already have an active analysis on the item, so
 * the non-conforming analysis is cancelled first and the test re-ordered — the
 * repeat then appears as a fresh pending analysis.
 */
export const dispositionRequests = (
  disposition: NceDisposition,
  row: DispositionRow,
): DispositionRequest[] => {
  const cancel: DispositionRequest | null =
    row.analysisId && row.sampleItemId
      ? {
          url: "/rest/sample-management/cancel-test",
          body: {
            analysisId: String(row.analysisId),
            sampleItemId: String(row.sampleItemId),
          },
        }
      : null;
  const addSameTest: DispositionRequest | null =
    row.sampleItemId && row.testId
      ? {
          url: "/rest/sample-management/add-tests",
          body: {
            sampleItemIds: [String(row.sampleItemId)],
            testIds: [String(row.testId)],
          },
        }
      : null;
  switch (disposition) {
    case "CANCEL":
      return cancel ? [cancel] : [];
    case "RETEST":
      return cancel && addSameTest ? [cancel, addSameTest] : [];
    default:
      return [];
  }
};

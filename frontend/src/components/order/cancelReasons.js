/** The dictionary category that lists why an order or a test is cancelled. */
export const CANCEL_REASON_CATEGORY = "Order cancel reasons";

/** The entry value that opens the free-text reason. */
export const OTHER_REASON = "__other__";

/** The reason to send: the chosen entry, or the free text under Other. */
export const cancelReasonText = (choice, other) =>
  choice === OTHER_REASON ? (other || "").trim() : choice;

/**
 * The samples with the tests the order last held put back on each saved
 * sample, so keeping a test after a refused removal leaves the form as saved.
 */
export const restoreRemovedTests = (samples, savedSamples) =>
  (samples || []).map((sample) => {
    if (!sample?.sampleItemId) {
      return sample;
    }
    const saved = (savedSamples || []).find(
      (candidate) =>
        String(candidate?.sampleItemId) === String(sample.sampleItemId),
    );
    const current = sample.tests || [];
    const missing = (saved?.tests || []).filter(
      (test) => !current.some((kept) => String(kept.id) === String(test.id)),
    );
    return missing.length
      ? { ...sample, tests: [...current, ...missing] }
      : sample;
  });

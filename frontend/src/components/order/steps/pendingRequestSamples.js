/**
 * The samples Prepare Samples shows once the order's pending sample requests
 * have loaded. The requests are the source of truth for each sample's identity,
 * type and tests; the Step 2 fields the order already held carry over
 * (collector, conditions, received date and time), and the collection date and
 * time do not (the backend stores the order entry date there, so they are left
 * for the card to fill with today).
 *
 * The requests arrive after the page opens, and the user may already have typed
 * into a card by then. A field that differs from what the card showed when the
 * load started is the user's edit, and it wins: the load used to rebuild every
 * sample from the snapshot taken at mount and dropped a quantity or unit typed
 * in the meantime.
 */
export const samplesFromPendingRequests = (requestSamples, atLoad, current) =>
  requestSamples.map((requestSample, index) => {
    const before = atLoad[index];
    const merged =
      before && before.sampleTypeId === requestSample.sampleTypeId
        ? {
            ...requestSample,
            collectorId: before.collectorId || requestSample.collectorId,
            collectionConditions:
              before.collectionConditions || requestSample.collectionConditions,
            receivedDate: before.receivedDate || requestSample.receivedDate,
            receivedTime: before.receivedTime || requestSample.receivedTime,
          }
        : requestSample;
    const now = current?.[index];
    if (!before || !now || now.sampleTypeId !== requestSample.sampleTypeId) {
      return merged;
    }
    const edited = Object.fromEntries(
      Object.entries(now).filter(([field, value]) => value !== before[field]),
    );
    return { ...merged, ...edited };
  });

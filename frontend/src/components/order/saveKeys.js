/**
 * Keys that let the server recognise a retried save. The order key becomes the
 * new order's FHIR UUID and each sample row's client key becomes its sample
 * item's, so a save repeated after its reply was lost updates what the first
 * attempt created instead of failing or creating it twice.
 */
export const newClientKey = () => {
  if (typeof crypto !== "undefined" && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    return (c === "x" ? r : (r & 0x3) | 0x8).toString(16);
  });
};

/**
 * Gives every unsaved sample row that has a sample type a client key it keeps
 * across retries. Rows that already have a sample item id, or already carry a
 * key, are returned unchanged. The array is only copied when a key was added.
 */
export const withClientKeys = (samples = []) => {
  let changed = false;
  const seen = new Set();
  const keyed = samples.map((sample) => {
    if (!sample || !sample.sampleTypeId || sample.sampleItemId) {
      return sample;
    }
    if (sample.clientKey && !seen.has(sample.clientKey)) {
      seen.add(sample.clientKey);
      return sample;
    }
    changed = true;
    const clientKey = newClientKey();
    seen.add(clientKey);
    return { ...sample, clientKey };
  });
  return { samples: changed ? keyed : samples, changed };
};

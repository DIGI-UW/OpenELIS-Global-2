const SAMPLE_FIELD_BY_UPDATE_KEY = {
  sampleTypeId: "sampleTypeId",
  sampleRejected: "sampleRejected",
  rejectionReason: "rejectionReason",
  selectedTests: "tests",
  selectedPanels: "panels",
  sampleXML: "sampleXML",
  requestReferralEnabled: "requestReferralEnabled",
  referralItems: "referralItems",
};

let lastSampleKey = 0;

/**
 * A React key for a sample row that survives removing an earlier sample, so a
 * sample's local state (ticked tests, search) never slides onto its neighbour.
 */
export function newSampleKey() {
  lastSampleKey += 1;
  return "sample-" + lastSampleKey;
}

/**
 * Applies what SampleType reported for one sample (sampleObjectIndex) and
 * returns new samples. Empty values are applied too: unticking the last test,
 * removing the last panel or clearing the sample type empties the order, and
 * false switches a flag off. Only undefined or null leaves a field as it is.
 * The input samples and their objects are never modified.
 */
export function applySampleTypeUpdate(samples, update) {
  const index = update?.sampleObjectIndex;
  if (!samples || !samples[index]) {
    return samples;
  }
  const changes = {};
  Object.entries(SAMPLE_FIELD_BY_UPDATE_KEY).forEach(([updateKey, field]) => {
    const value = update[updateKey];
    if (value !== undefined && value !== null) {
      changes[field] = value;
    }
  });
  if (Object.keys(changes).length === 0) {
    return samples;
  }
  return samples.map((sample, i) =>
    i === index ? { ...sample, ...changes } : sample,
  );
}

/**
 * Whether a sample is ticked Reject without a rejection reason. The order
 * cannot be submitted then: the server refuses it (OGC-1488).
 */
export function rejectedWithoutReason(samples) {
  return (samples || []).some((sample) => {
    const rejected = sample?.sampleXML?.rejected ?? sample?.sampleRejected;
    const reason =
      sample?.sampleXML?.rejectionReason || sample?.rejectionReason || "";
    return Boolean(rejected) && !String(reason).trim();
  });
}

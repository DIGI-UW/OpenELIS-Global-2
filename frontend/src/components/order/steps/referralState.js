/**
 * What the referrals staged or saved on the order's samples add up to.
 *
 * A referral counts while it is pending the step's save or while its status
 * is anything but cancelled or rejected: a referral the reference laboratory
 * has completed still means the test was not done here.
 */

const OPEN_REFERRAL_STATUSES_EXCLUDED = ["CANCELLED", "REJECTED"];

export const physicalSamples = (samples) =>
  (samples || []).filter(
    (sample) =>
      sample &&
      sample.sampleTypeId &&
      !sample.qcMetadata?.qcType &&
      !sample.sampleRejected,
  );

export const hasReferral = (sample) => {
  const referral = sample?.referralItems?.[0];
  if (!referral) {
    return false;
  }
  if (referral.pendingSave) {
    return true;
  }
  return !OPEN_REFERRAL_STATUSES_EXCLUDED.includes(referral.referralStatus);
};

/** Every tube that carries a test is referred out: nothing is left in-house. */
export const isFullyReferred = (samples) => {
  const withTests = physicalSamples(samples).filter(
    (sample) => (sample.tests || []).length > 0,
  );
  return withTests.length > 0 && withTests.every(hasReferral);
};

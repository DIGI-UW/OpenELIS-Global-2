/**
 * What a lane still needs before its order can be saved.
 *
 * Each lane gates saving on its own set of conditions, so a fixed sentence
 * cannot describe them: it told a user with a patient and a sample type but no
 * lab number to add the two things they already had. Callers pass the gate
 * itself — the same conditions the Save button reads — and get back only what
 * is actually missing.
 */
export const unmetRequirements = (requirements) =>
  requirements.filter((requirement) => !requirement.met);

/**
 * The server refuses an environmental or vector order that names neither a
 * requesting organisation nor a requestor contact; this is the same check, so
 * Save is not offered for an order that would be refused.
 */
export const hasRequesterOrRequestor = (sampleOrderItems = {}) =>
  [
    "referringSiteId",
    "referringSiteName",
    "newRequesterName",
    "requestorPersonId",
    "requestorFirstName",
    "requestorLastName",
  ].some((field) => String(sampleOrderItems?.[field] ?? "").trim() !== "");

/**
 * Renders the unmet requirements as one sentence, e.g.
 * "Add a lab number and at least one sample type before saving."
 */
export const describeUnmetRequirements = (intl, requirements) => {
  const missing = unmetRequirements(requirements).map((requirement) =>
    intl.formatMessage({ id: requirement.labelId }),
  );
  if (missing.length === 0) {
    return "";
  }
  const fields =
    missing.length === 1
      ? missing[0]
      : `${missing.slice(0, -1).join(", ")} ${intl.formatMessage({
          id: "order.save.incomplete.and",
        })} ${missing[missing.length - 1]}`;
  return intl.formatMessage({ id: "order.save.incomplete.fields" }, { fields });
};

/**
 * The save gate of an environmental or vector order: a lab number, the lane's
 * site, a requesting organisation or requestor, a sample type, and a test or
 * panel on every sample that has a type. The vector lane lacked the last one,
 * so an order saved with a sample and no test (OGC-1060).
 */
export const fieldOrderRequirements = ({
  labNumber,
  hasSite,
  siteLabelId,
  sampleOrderItems,
  samples = [],
}) => {
  const typed = samples.filter((sample) => sample.sampleTypeId);
  const everySampleHasATest = typed.every(
    (sample) => (sample.tests?.length || 0) + (sample.panels?.length || 0) > 0,
  );
  return [
    { met: Boolean(labNumber), labelId: "order.save.requirement.labNumber" },
    { met: Boolean(hasSite), labelId: siteLabelId },
    {
      met: hasRequesterOrRequestor(sampleOrderItems),
      labelId: "order.save.requirement.requesterOrRequestor",
    },
    { met: typed.length > 0, labelId: "order.save.requirement.sampleType" },
    {
      met: typed.length > 0 && everySampleHasATest,
      labelId: "order.save.requirement.testsPerSample",
    },
  ];
};

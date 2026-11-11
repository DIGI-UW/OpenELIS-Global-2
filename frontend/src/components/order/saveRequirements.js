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

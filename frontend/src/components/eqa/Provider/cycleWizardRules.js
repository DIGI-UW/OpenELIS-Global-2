// What the provider cycle wizard refuses to write, kept out of the component so
// the rules can be tested without rendering five steps (OGC-613, FR-V2.5-02).

export const DISTRIBUTION_METHODS = ["FHIR", "CSV", "MIXED"];

/**
 * How many aliquots this cycle needs: one per sample per participant. The prep
 * gate enforces this against what was actually produced — here it only
 * seeds the field, so the operator starts from the right number.
 */
export const aliquotsNeeded = (samples, participantCount) =>
  (samples || []).length * (Number(participantCount) || 0);

/**
 * Everything missing, as message ids the wizard renders. Order matters: the
 * first blocker names the earliest step still incomplete, so "Create" can say
 * what to go back to.
 */
export const wizardBlockers = ({
  cycle = {},
  samples = [],
  participants = [],
  distributionMethod = "",
}) => {
  const blockers = [];
  if (!cycle.schemeId) {
    blockers.push("eqa.provider.wizard.blocker.scheme");
  }
  if (!cycle.plannedEndDate) {
    blockers.push("eqa.provider.wizard.blocker.deadline");
  }
  if (samples.length === 0) {
    blockers.push("eqa.provider.wizard.blocker.samples");
  }
  if (samples.some((sample) => !sample.testId || !sample.targetValue)) {
    blockers.push("eqa.provider.wizard.blocker.targets");
  }
  if (participants.length === 0) {
    blockers.push("eqa.provider.wizard.blocker.participants");
  }
  if (!DISTRIBUTION_METHODS.includes(distributionMethod)) {
    blockers.push("eqa.provider.wizard.blocker.method");
  }
  return blockers;
};

/**
 * What POST /rest/eqa/cycles is sent. The cycle number is deliberately absent:
 * the service takes the scheme's next one, so two providers filling the wizard at
 * once cannot both claim the same number.
 */
export const cyclePayload = ({ cycle = {}, distributionMethod }) => ({
  schemeId: cycle.schemeId,
  cycleName: cycle.cycleName,
  plannedStartDate: cycle.plannedStartDate || null,
  plannedEndDate: cycle.plannedEndDate,
  distributionMethod,
});

/**
 * What POST /rest/eqa/panels is sent, once the cycle exists. Empty range bounds
 * go as null rather than "": the column is numeric, and an empty string is a 422
 * the operator can do nothing about.
 */
export const panelPayload = (
  { cycle = {}, samples = [], prep = {} },
  cycleResponse,
  panelName,
) => ({
  schemeId: cycle.schemeId,
  cycleId: cycleResponse.id,
  panelName,
  panelType: "PROVIDER",
  sourceType: prep.sourceType,
  lotNumber: prep.lotNumber,
  storageTemp: prep.storageTemp,
  expirationDate: prep.expirationDate || null,
  aliquotsProduced: prep.aliquotsProduced || 0,
  homogeneityQcPassed: prep.homogeneityQcPassed,
  homogeneityQcNotes: prep.homogeneityQcNotes,
  samples: samples.map((sample) => ({
    testId: sample.testId,
    targetValue: sample.targetValue,
    targetUnit: sample.targetUnit,
    acceptanceRangeLow: sample.rangeLow || null,
    acceptanceRangeHigh: sample.rangeHigh || null,
  })),
});

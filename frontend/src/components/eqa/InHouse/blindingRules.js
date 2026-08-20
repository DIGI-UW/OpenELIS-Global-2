// The prep gate, in the same words the server uses (EQABlindingServiceImpl
// #requirePrepEvidence). One rule, one vocabulary: the wizard must not let
// through what seal-and-distribute would refuse, and must not refuse what it
// would accept.
//
// Callers pass the EXPANDED rows (see expandForMode), never the materials the
// user typed: one blinded order is created per row, so one aliquot per row is
// the whole rule. Under an identical set the expansion is materials x analysts,
// which is exactly the figure the FRS writes the gate as — same arithmetic,
// reached without a second formula to keep in step.
export const prepBlockers = (samples, prep) => {
  const blockers = [];
  const produced = Number(prep.aliquotsProduced || 0);
  if (samples.length === 0) {
    blockers.push("eqa.inhouse.gate.noSamples");
  } else if (produced < samples.length) {
    blockers.push("eqa.inhouse.gate.aliquots");
  }
  if (!prep.homogeneityQcPassed && !(prep.homogeneityQcNotes || "").trim()) {
    blockers.push("eqa.inhouse.gate.homogeneity");
  }
  if (
    samples.some((sample) => !sample.testId || !`${sample.targetValue}`.trim())
  ) {
    blockers.push("eqa.inhouse.gate.targets");
  }
  return blockers;
};

// FR-V2.4-03's assignment modes. The rows the wizard collects in step 2 are
// materials; what gets sealed is one aliquot per row of the expansion below,
// because distribution creates exactly one blinded order per panel sample.
export const ASSIGNMENT_MODES = ["ROUND_ROBIN", "IDENTICAL", "MANUAL"];

// Round-robin: one aliquot per material, analysts dealt in roster order.
// Manual: same shape, but the analyst on each row is whatever the user set.
// Identical set: every analyst runs every material, so each material becomes one
// aliquot per analyst — each with its own blind code, which is what keeps two
// analysts testing the same material from comparing notes.
//
// The identical expansion is why the FRS writes the gate as
// aliquots >= samples x analysts: in that mode the expansion IS that product,
// so the one rule "an aliquot per blinded row" covers both readings.
export const expandForMode = (samples, roster, mode) => {
  if (mode === "IDENTICAL") {
    return samples.flatMap((sample) =>
      roster.map((analyst) => ({
        ...sample,
        key: `${sample.key}-${analyst.systemUserId}`,
        materialKey: sample.key,
        analystId: analyst.systemUserId,
      })),
    );
  }
  if (mode === "MANUAL") {
    return samples.map((sample) => ({ ...sample, materialKey: sample.key }));
  }
  return samples.map((sample, index) => ({
    ...sample,
    materialKey: sample.key,
    analystId:
      roster.length === 0 ? null : roster[index % roster.length].systemUserId,
  }));
};

/** Kept for the round-robin display path; expandForMode is the general form. */
export const roundRobin = (samples, roster) =>
  expandForMode(samples, roster, "ROUND_ROBIN");

// Identical set has nothing to expand over without a roster, and silently
// falling back to one aliquot per material would seal a panel nobody asked for.
export const modeBlockers = (roster, mode) =>
  mode === "IDENTICAL" && roster.length === 0
    ? ["eqa.inhouse.gate.identicalNeedsAnalysts"]
    : [];

// The seal, as a fact rather than an inference from the status: sealed while the
// panel holds its targets back, unsealed with the date they were revealed.
//
// Deliberately no cipher name — the mockup's "AES-256" is a claim we cannot make
// per install: EncryptionConverter delegates to Jasypt's TextEncryptor, whose
// algorithm comes from deployment configuration.
export const sealState = (panel) => {
  if (["SEALED", "DISTRIBUTED"].includes(panel.status)) {
    return { key: "eqa.inhouse.seal.sealed", sealed: true };
  }
  if (["UNBLINDED", "SCORED", "CLOSED"].includes(panel.status)) {
    return {
      key: "eqa.inhouse.seal.unsealed",
      sealed: false,
      // The instant is what the audit cares about; the calendar day is what fits
      // the column.
      date: (panel.unblindedAt || "").slice(0, 10),
    };
  }
  return { key: null, sealed: false };
};

const withinDays = (isoDate, days, today) => {
  if (!isoDate) {
    return false;
  }
  const horizon = new Date(today);
  horizon.setDate(horizon.getDate() + days);
  return new Date(isoDate) <= horizon;
};

// The four tiles the mockup puts above the list. Counted from the rows already
// fetched — no second endpoint for arithmetic the client can do.
export const panelKpis = (panels, today = new Date()) => ({
  awaitingDistribution: panels.filter((p) => p.status === "SEALED").length,
  inTesting: panels.filter((p) => p.status === "DISTRIBUTED").length,
  unblindingSoon: panels.filter(
    (p) =>
      ["SEALED", "DISTRIBUTED"].includes(p.status) &&
      withinDays(p.unblindDate, 7, today),
  ).length,
  closed: panels.filter((p) => ["SCORED", "CLOSED"].includes(p.status)).length,
});

// LESOTHO: which ordered tests bring up which program's questionnaire.
//
// A test matches by LOINC code or by its name, never by database id: ids differ
// on every install, so an id written here only works on the machine it was
// copied from. Names are compared the way testForms.ts compares them (upper
// case, no spaces or punctuation), so "HIV-1 Viral Load" and "HIV 1 viral load"
// are the same test.
//
// The list is in priority order. When an order has tests for several programs,
// the first program in the list wins.

/** Upper case without spaces or punctuation: "HIV-1 Viral Load" -> "HIV1VIRALLOAD". */
const squash = (value) =>
  String(value || "")
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, "");

// Viral load tests that are not HIV: hepatitis, and infant HIV tests (those
// belong to the DNA-PCR form).
const NOT_HIV_VIRAL_LOAD = ["HEPATITIS", "HBV", "HCV", "INFANT"];

export const TRIGGERED_PROGRAMS = [
  {
    // Program code (Admin > Programs > Programme Code).
    code: "HIV_VL",
    // LOINC: HIV viral load.
    loinc: ["10351-5", "20447-9"],
    // Name, for tests that have no LOINC set (for example "Viral Load" and the
    // French "Mesure de la charge virale").
    matchesName: (name) =>
      (name.includes("VIRALLOAD") || name.includes("CHARGEVIRALE")) &&
      !NOT_HIV_VIRAL_LOAD.some((word) => name.includes(word)),
  },
  // Add the next program here, for example:
  // { code: "TB_EXAM", loinc: [...], matchesName: (name) => ... },
];

const testMatches = (test, trigger) => {
  if (!test || typeof test !== "object") {
    return false;
  }
  const loinc = String(test.loinc || "").trim();
  if (loinc && trigger.loinc.includes(loinc)) {
    return true;
  }
  return trigger.matchesName(squash(test.name));
};

/** Codes of the programs the given tests trigger, in priority order. */
export const triggeredProgramCodes = (tests = []) =>
  TRIGGERED_PROGRAMS.filter((trigger) =>
    tests.some((test) => testMatches(test, trigger)),
  ).map((trigger) => trigger.code);

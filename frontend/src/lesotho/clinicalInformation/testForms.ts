// LESOTHO: which MOH request form's fields Add Order shows, worked out from the
// tests on the order. There are no form checkboxes: ordering a test is what
// brings up its form's section under Clinical Information.
//
// Linked so far:
// - HIV DNA PCR -> the DNA-PCR (infant HIV test) form.
// - Crossmatch -> the blood transfusion request. A cross-match is what the lab
//   does when blood is requested for a patient; blood group, Rhesus or an
//   antibody screen on their own (for example at ANC) do not bring it up.
// To link another form later, add a rule here (for example viral load tests
// -> "VIRAL_LOAD").
import type { RequestForm } from "./ClinicalInformation";

interface OrderedTest {
  name?: string;
}

/** Upper case without spaces or punctuation: "HIV DNA-PCR" -> "HIVDNAPCR". */
const squash = (name?: string) =>
  (name || "").toUpperCase().replace(/[^A-Z0-9]/g, "");

const RULES: { form: RequestForm; matches: (name: string) => boolean }[] = [
  { form: "DNA_PCR", matches: (name) => name.includes("DNAPCR") },
  {
    form: "BLOOD_TRANSFUSION",
    matches: (name) => name.includes("CROSSMATCH"),
  },
];

/** The request forms that apply to an order with these tests, in form order. */
export const formsForTests = (tests: OrderedTest[] = []): RequestForm[] => {
  const names = tests.map((test) => squash(test.name));
  return RULES.filter((rule) => names.some(rule.matches)).map(
    (rule) => rule.form,
  );
};

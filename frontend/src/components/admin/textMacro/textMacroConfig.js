export const TEXT_MACRO_CONTEXTS = Object.freeze([
  "MICROBIOLOGY_CULTURE_ACTIVITY",
  "MICROBIOLOGY_CLINICAL_HISTORY",
  "MICROBIOLOGY_ANTIBIOTIC_EXPOSURE",
]);

export const createEmptyTextMacro = () => ({
  code: "",
  expansionText: "",
  contexts: [TEXT_MACRO_CONTEXTS[0]],
  active: true,
});

export const TEXT_MACRO_CONTEXTS = Object.freeze([
  "MICROBIOLOGY_CLINICAL_HISTORY",
  "MICROBIOLOGY_CASE_NOTE",
  "MICROBIOLOGY_CULTURE_READING",
  "MICROBIOLOGY_OVERRIDE_REASON",
  "MICROBIOLOGY_AMENDMENT_REASON",
]);

export const TEXT_MACRO_CATEGORIES = Object.freeze([
  "CLINICAL",
  "GRAM_STAIN",
  "COLONY",
  "CULTURE",
  "ORGANISMS",
  "AST",
  "REPORTING",
  "TIMELINE",
]);

export const createEmptyTextMacro = () => ({
  code: "",
  expansionText: "",
  category: "",
  contexts: [],
  active: true,
});

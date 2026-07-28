import type { IntlShape } from "react-intl";

const ANALYZER_TYPE_MESSAGE_IDS = {
  HEMATOLOGY: "analyzer.type.hematology",
  CHEMISTRY: "analyzer.type.chemistry",
  IMMUNOLOGY: "analyzer.type.immunology",
  MICROBIOLOGY: "analyzer.type.microbiology",
  MOLECULAR: "analyzer.type.molecular",
  COAGULATION: "analyzer.type.coagulation",
  OTHER: "analyzer.form.type.other",
} as const;

export const buildAnalyzerTypeOptions = (intl: IntlShape) =>
  Object.entries(ANALYZER_TYPE_MESSAGE_IDS).map(([id, messageId]) => ({
    id,
    text: intl.formatMessage({ id: messageId }),
  }));

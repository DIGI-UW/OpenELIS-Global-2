export interface ProgramFormValues {
  program: {
    programName: string;
    code: string;
    id: string;
    questionnaireUUID: string;
  };
  additionalOrderEntryQuestions: string;
  testSectionId: string;
}

export default {
  program: {
    programName: "",
    code: "",
    id: "",
    questionnaireUUID: "",
  },
  additionalOrderEntryQuestions: '{"resourceType":"Questionnaire"}',
  testSectionId: "",
} as ProgramFormValues;

const RuleBuilderFormValues = {
  id: null,
  ruleName: "",
  overall: "",
  toggled: true,
  active: true,
  analyteId: null,
  conditions: [
    {
      id: null,
      sampleId: "",
      testName: "",
      testId: "",
      relation: "",
      value: "0",
      value2: "0",
      testAnalyteId: null,
    },
  ],
  actions: [
    {
      id: null,
      sampleId: "",
      reflexTestName: "",
      reflexTestId: "",
      internalNote: "",
      externalNote: "",
      addNotification: "Y",
      testReflexId: null,
    },
  ],
};

export default RuleBuilderFormValues;

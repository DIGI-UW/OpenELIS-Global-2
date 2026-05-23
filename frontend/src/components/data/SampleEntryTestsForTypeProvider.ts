interface SampleTypeTestsStructure {
  sampleTypeId: string;
  panels: Array<{
    name: string;
    testIds: string;
    panelId: string;
    panelOrder: number;
  }>;
  tests: Array<{
    id: string;
    name: string;
    userBenchChoice: boolean;
  }>;
}

export const sampleTypeTestsStructure: SampleTypeTestsStructure = {
  sampleTypeId: "",
  panels: [
    {
      name: "",
      testIds: "",
      panelId: "1",
      panelOrder: 0,
    },
  ],
  tests: [
    {
      id: "",
      name: "",
      userBenchChoice: false,
    },
  ],
};

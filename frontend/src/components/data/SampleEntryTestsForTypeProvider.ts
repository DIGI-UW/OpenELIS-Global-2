export interface SampleTypePanel {
  name: string;
  testIds: string;
  panelId: string;
  panelOrder: number;
}

export interface SampleTypeTest {
  id: string;
  name: string;
  userBenchChoice: boolean;
}

export interface SampleTypeTestsStructure {
  sampleTypeId: string;
  panels: SampleTypePanel[];
  tests: SampleTypeTest[];
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

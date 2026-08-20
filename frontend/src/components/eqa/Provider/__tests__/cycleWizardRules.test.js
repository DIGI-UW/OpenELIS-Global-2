import {
  aliquotsNeeded,
  cyclePayload,
  panelPayload,
  wizardBlockers,
} from "../cycleWizardRules";

const complete = {
  cycle: { schemeId: "3", plannedEndDate: "2026-10-15" },
  samples: [{ testId: "70", targetValue: "4.52" }],
  participants: [{ id: 1 }, { id: 2 }],
  distributionMethod: "FHIR",
};

describe("cycleWizardRules", () => {
  test("a complete wizard has nothing left to block it", () => {
    expect(wizardBlockers(complete)).toEqual([]);
  });

  test("each missing answer names its own step, earliest first", () => {
    expect(
      wizardBlockers({
        cycle: {},
        samples: [],
        participants: [],
        distributionMethod: "",
      }),
    ).toEqual([
      "eqa.provider.wizard.blocker.scheme",
      "eqa.provider.wizard.blocker.deadline",
      "eqa.provider.wizard.blocker.samples",
      "eqa.provider.wizard.blocker.participants",
      "eqa.provider.wizard.blocker.method",
    ]);
  });

  test("a sample without a target is as blocking as no sample at all", () => {
    expect(
      wizardBlockers({ ...complete, samples: [{ testId: "70" }] }),
    ).toEqual(["eqa.provider.wizard.blocker.targets"]);
    expect(
      wizardBlockers({ ...complete, samples: [{ targetValue: "4.52" }] }),
    ).toEqual(["eqa.provider.wizard.blocker.targets"]);
  });

  test("a scheme with no active participants cannot start a cycle", () => {
    expect(wizardBlockers({ ...complete, participants: [] })).toEqual([
      "eqa.provider.wizard.blocker.participants",
    ]);
  });

  test("an unknown distribution method is refused, not passed through", () => {
    expect(
      wizardBlockers({ ...complete, distributionMethod: "PIGEON" }),
    ).toEqual(["eqa.provider.wizard.blocker.method"]);
  });

  test("aliquots needed is one per sample per participant", () => {
    expect(aliquotsNeeded([{}, {}, {}], 4)).toBe(12);
    expect(aliquotsNeeded([{}, {}], 0)).toBe(0);
    expect(aliquotsNeeded([], 5)).toBe(0);
  });

  test("the cycle payload leaves the number to the server", () => {
    expect(
      cyclePayload({
        cycle: {
          schemeId: "3",
          cycleName: "2026 Round 1",
          plannedStartDate: "",
          plannedEndDate: "2026-10-15",
        },
        distributionMethod: "MIXED",
      }),
    ).toEqual({
      schemeId: "3",
      cycleName: "2026 Round 1",
      plannedStartDate: null,
      plannedEndDate: "2026-10-15",
      distributionMethod: "MIXED",
    });
  });

  test("the panel payload sends blank range bounds as null, not as empty strings", () => {
    const payload = panelPayload(
      {
        cycle: { schemeId: "3", cycleName: "" },
        samples: [
          {
            testId: "70",
            targetValue: "4.52",
            targetUnit: "log",
            rangeLow: "4.1",
            rangeHigh: "",
          },
        ],
        prep: {
          sourceType: "VENDOR_SOURCED",
          lotNumber: "LOT-9",
          storageTemp: "FROZEN_MINUS_20C",
          expirationDate: "",
          aliquotsProduced: 8,
          homogeneityQcPassed: true,
          homogeneityQcNotes: "CV 3%",
        },
      },
      { id: 42, cycleNumber: 1 },
      "National HIV VL PT Cycle 1",
    );

    expect(payload).toEqual({
      schemeId: "3",
      cycleId: 42,
      panelName: "National HIV VL PT Cycle 1",
      panelType: "PROVIDER",
      sourceType: "VENDOR_SOURCED",
      lotNumber: "LOT-9",
      storageTemp: "FROZEN_MINUS_20C",
      expirationDate: null,
      aliquotsProduced: 8,
      homogeneityQcPassed: true,
      homogeneityQcNotes: "CV 3%",
      samples: [
        {
          testId: "70",
          targetValue: "4.52",
          targetUnit: "log",
          acceptanceRangeLow: "4.1",
          acceptanceRangeHigh: null,
        },
      ],
    });
  });
});

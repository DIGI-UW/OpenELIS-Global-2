import {
  expandForMode,
  modeBlockers,
  panelKpis,
  prepBlockers,
  roundRobin,
  sealState,
} from "../blindingRules";

const sample = (key, overrides = {}) => ({
  key,
  testId: "58",
  targetValue: "4.52",
  ...overrides,
});

const prep = (overrides = {}) => ({
  aliquotsProduced: 2,
  homogeneityQcPassed: true,
  homogeneityQcNotes: "",
  ...overrides,
});

describe("in-house prep gate (AC-V2.4-12)", () => {
  test("clear when every sample is covered by an aliquot and QC passed", () => {
    expect(prepBlockers([sample("S1"), sample("S2")], prep())).toEqual([]);
  });

  test("blocks when aliquots produced fall short of the sample count", () => {
    expect(
      prepBlockers([sample("S1"), sample("S2")], prep({ aliquotsProduced: 1 })),
    ).toContain("eqa.inhouse.gate.aliquots");
  });

  test("failed homogeneity QC needs a written justification", () => {
    expect(
      prepBlockers(
        [sample("S1")],
        prep({ aliquotsProduced: 1, homogeneityQcPassed: false }),
      ),
    ).toContain("eqa.inhouse.gate.homogeneity");

    expect(
      prepBlockers(
        [sample("S1")],
        prep({
          aliquotsProduced: 1,
          homogeneityQcPassed: false,
          homogeneityQcNotes: "Second run pending, supervisor approved",
        }),
      ),
    ).toEqual([]);
  });

  test("a sample without a target value cannot pass", () => {
    expect(
      prepBlockers([sample("S1", { targetValue: "  " })], prep()),
    ).toContain("eqa.inhouse.gate.targets");
  });
});

describe("round-robin assignment (FR-V2.4-03)", () => {
  test("cycles the roster across samples", () => {
    const roster = [{ systemUserId: 7 }, { systemUserId: 9 }];
    const assigned = roundRobin(
      [sample("S1"), sample("S2"), sample("S3")],
      roster,
    );
    expect(assigned.map((row) => row.analystId)).toEqual([7, 9, 7]);
  });

  test("an empty roster leaves rows unassigned rather than throwing", () => {
    expect(roundRobin([sample("S1")], [])[0].analystId).toBeNull();
  });
});

describe("assignment modes (FR-V2.4-03)", () => {
  const roster = [{ systemUserId: 7 }, { systemUserId: 9 }];
  const materials = [sample("S01"), sample("S02")];

  test("identical set gives every analyst every sample, each its own aliquot", () => {
    const rows = expandForMode(materials, roster, "IDENTICAL");
    // 2 materials x 2 analysts — this IS the FRS's samples x analysts figure.
    expect(rows).toHaveLength(4);
    expect(rows.map((r) => r.analystId)).toEqual([7, 9, 7, 9]);
    expect(rows.map((r) => r.materialKey)).toEqual([
      "S01",
      "S01",
      "S02",
      "S02",
    ]);
    // Distinct rows, so distinct blind codes at seal — two analysts on the same
    // material must not share a tube label.
    expect(new Set(rows.map((r) => r.key)).size).toBe(4);
  });

  test("round-robin and manual keep one aliquot per sample", () => {
    expect(expandForMode(materials, roster, "ROUND_ROBIN")).toHaveLength(2);
    expect(expandForMode(materials, roster, "MANUAL")).toHaveLength(2);
  });

  test("manual mode leaves the analyst on the row alone", () => {
    const preset = [{ ...sample("S01"), analystId: 9 }];
    expect(expandForMode(preset, roster, "MANUAL")[0].analystId).toBe(9);
  });

  test("identical set with no roster is blocked rather than silently halved", () => {
    expect(expandForMode(materials, [], "IDENTICAL")).toHaveLength(0);
    expect(modeBlockers([], "IDENTICAL")).toContain(
      "eqa.inhouse.gate.identicalNeedsAnalysts",
    );
    expect(modeBlockers([], "ROUND_ROBIN")).toEqual([]);
  });

  test("the prep gate counts aliquots, so identical set needs samples x analysts", () => {
    const rows = expandForMode(materials, roster, "IDENTICAL");
    expect(prepBlockers(rows, prep({ aliquotsProduced: 3 }))).toContain(
      "eqa.inhouse.gate.aliquots",
    );
    expect(prepBlockers(rows, prep({ aliquotsProduced: 4 }))).toEqual([]);
  });
});

describe("seal state (FR-V2.1-16)", () => {
  test("a panel holding its targets back reads as sealed", () => {
    expect(sealState({ status: "SEALED" }).sealed).toBe(true);
    expect(sealState({ status: "DISTRIBUTED" }).sealed).toBe(true);
  });

  test("a revealed panel reads as unsealed, dated by the unblind", () => {
    const state = sealState({
      status: "SCORED",
      unblindedAt: "2026-08-19 21:03:12.147",
    });
    expect(state.sealed).toBe(false);
    expect(state.date).toBe("2026-08-19");
  });

  test("a panel that never sealed claims nothing either way", () => {
    expect(sealState({ status: "PREPARING" }).key).toBeNull();
  });
});

describe("landing KPI tiles", () => {
  const today = new Date("2026-08-20T00:00:00Z");
  const panels = [
    { status: "SEALED", unblindDate: "2026-08-24" },
    { status: "SEALED", unblindDate: "2026-10-01" },
    { status: "DISTRIBUTED", unblindDate: "2026-08-21" },
    { status: "SCORED", unblindDate: "2026-07-01" },
    { status: "CLOSED", unblindDate: "2026-06-01" },
    { status: "PREPARING", unblindDate: null },
  ];

  test("counts each lifecycle bucket", () => {
    const kpis = panelKpis(panels, today);
    expect(kpis.awaitingDistribution).toBe(2);
    expect(kpis.inTesting).toBe(1);
    expect(kpis.closed).toBe(2);
  });

  test("the 7-day horizon counts only panels still sealed", () => {
    // The far-off SEALED panel and the already-scored ones are out; a
    // null unblind date must not count as imminent.
    expect(panelKpis(panels, today).unblindingSoon).toBe(2);
  });
});

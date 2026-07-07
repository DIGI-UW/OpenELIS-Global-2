import { generateVectorSurveillancePDF } from "./vectorPdfGenerator";

// Uses the REAL jsPDF + jspdf-autotable (no mocks) so the test exercises the
// actual export pipeline — it throws (and fails) if doc.autoTable is not wired
// (the jspdf-autotable v5 regression). Only the browser download is neutralized.

const indices = {
  freshness: "2026-06-10T08:30:00Z",
  collectionDensity: [
    { periodLabel: "2026-W23", siteName: "Kupang", poolCount: 12, specimenCount: 480 },
  ],
  speciesDistribution: [
    { genus: "Aedes", species: "aegypti", specimenCount: 300, pct: 62.5 },
  ],
  mirBySpecies: [
    {
      speciesLabel: "Aedes aegypti",
      pathogen: "Dengue",
      mirClassic: 4.17,
      infectionRateObserved: 3.9,
      positiveResolutionPct: 80,
      positivePools: 2,
      totalSpecimens: 480,
    },
  ],
  sporozoiteRatePct: 1.5,
  pathogenPositivity: [
    { pathogen: "Dengue", poolsPositive: 2, poolsTested: 12, positivityPct: 16.7 },
  ],
  qcPassRate: { passRatePct: 95.7, analysesPassed: 67, analysesTotal: 70 },
};

const scope = { dateFrom: "01/06/2026", dateTo: "07/06/2026", siteName: "Kupang" };

describe("generateVectorSurveillancePDF", () => {
  beforeAll(() => {
    // jsPDF.save() triggers a browser download; jsdom has no URL.createObjectURL.
    global.URL.createObjectURL = vi.fn(() => "blob:test");
    global.URL.revokeObjectURL = vi.fn();
  });

  it("runs the real jsPDF + autoTable pipeline without throwing and renders all six sections", () => {
    const fmt = vi.fn(({ id }) => id);

    expect(() =>
      generateVectorSurveillancePDF(indices, scope, fmt),
    ).not.toThrow();

    // Every section header + the sporozoite KPI was requested from i18n — proves
    // all six panels ran (and, with not.toThrow, that each autoTable call worked).
    const requested = fmt.mock.calls.map((c) => c[0].id);
    for (const id of [
      "vectorReport.density.title",
      "vectorReport.species.title",
      "vectorReport.mir.title",
      "vectorReport.sporozoite.title",
      "vectorReport.positivity.title",
      "vectorReport.qc.title",
    ]) {
      expect(requested).toContain(id);
    }
  });
});

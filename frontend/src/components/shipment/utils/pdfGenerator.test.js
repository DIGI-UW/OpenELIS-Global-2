import { generateManifestPDF, generateLabelPDF } from "./pdfGenerator";

// Uses the REAL jsPDF + jspdf-autotable (no mocks) so the test exercises the
// actual export pipeline — generateManifestPDF calls doc.autoTable, which is only
// wired by applyPlugin(jsPDF). Without that call the manifest export throws
// "doc.autoTable is not a function" (the jspdf-autotable v5 regression), so this
// is the inversion guard for the shipment-side fix. bwip-js is stubbed to throw so
// the barcode path is skipped (jsdom has no canvas) without touching autoTable.
vi.mock("bwip-js", () => ({
  default: {
    toCanvas: () => {
      throw new Error("no canvas in jsdom");
    },
  },
}));

const manifestData = {
  boxId: "BOX-001",
  serviceLocation: "Central Lab",
  destinationFacility: "National Reference Lab",
  state: "PACKED",
  temperature: "FROZEN",
  createdDate: "2026-06-10T08:30:00Z",
  createdBy: "tech1",
  notes: "Keep frozen.",
  samples: [
    {
      accessionNumber: "S-1001",
      typeOfSample: "Serum",
      referralTests: "Dengue",
      collectionDate: "2026-06-09T00:00:00Z",
    },
    {
      accessionNumber: "S-1002",
      typeOfSample: "Plasma",
      referralTests: "Malaria",
      collectionDate: "2026-06-09T00:00:00Z",
    },
  ],
};

const labelData = {
  boxId: "BOX-001",
  destinationFacility: "National Reference Lab",
  temperature: "FROZEN",
  sampleCount: 2,
  sampleTypeCounts: { Serum: 1, Plasma: 1 },
  createdDate: "2026-06-10T08:30:00Z",
};

describe("shipment pdfGenerator", () => {
  beforeAll(() => {
    // jsPDF.save() triggers a browser download; jsdom has no URL.createObjectURL.
    global.URL.createObjectURL = vi.fn(() => "blob:test");
    global.URL.revokeObjectURL = vi.fn();
  });

  it("generateManifestPDF runs the real jsPDF + autoTable pipeline without throwing", async () => {
    const fmt = vi.fn(({ id }) => id);

    // Rejects with "doc.autoTable is not a function" if applyPlugin(jsPDF) is missing.
    await expect(
      generateManifestPDF(manifestData, fmt),
    ).resolves.toBeUndefined();

    // The autoTable-backed sections (box info + samples table) and the barcode
    // section all requested their labels — proof each block executed.
    const requested = fmt.mock.calls.map((c) => c[0].id);
    for (const id of [
      "shipment.manifest.title",
      "shipment.box.id",
      "sample.label.accessionNumber",
      "shipment.manifest.specimenBarcodes",
      "shipment.manifest.totalSamples",
    ]) {
      expect(requested).toContain(id);
    }
  });

  it("generateLabelPDF renders the box label without throwing", () => {
    const fmt = vi.fn(({ id }) => id);
    expect(() => generateLabelPDF(labelData, fmt)).not.toThrow();
  });
});

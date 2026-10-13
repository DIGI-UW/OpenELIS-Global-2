import { beforeEach, describe, expect, it, vi } from "vitest";
import { createIntl } from "react-intl";
import { QueryClient } from "@tanstack/react-query";
import { unzipSync } from "fflate";
import html2canvas from "html2canvas";
import { jsPDF } from "jspdf";
import messages from "../../../languages/en.json";
import { SERVER_DATA_KEY } from "../../utils/useServerData";
import { kpisUrl } from "./managementApi";
import {
  EXPORT_SECTIONS,
  buildPdf,
  csvDownload,
  loadTables,
  toCsv,
} from "./managementExport";

vi.mock("html2canvas", () => ({ default: vi.fn() }));

const PIXEL =
  "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA=";

const intl = createIntl({ locale: "en", messages });
const mapper = (id) => EXPORT_SECTIONS.find((s) => s.id === id);
const kpis = mapper("kpis");
const kpiData = {
  samples: { value: 65, prior: 60 },
  medianTat: { value: 20, prior: 22.5 },
  onTime: { value: 60.66, prior: null },
  rejection: { value: 3.28, prior: 3.39 },
  qcPass: { value: 93.33, prior: null },
};

const meta = {
  title: "Management Overview",
  labName: "CPHL UAT Lab",
  cover: [["Time scope", "Last 30 days"]],
  chartUnavailable: "[Chart image unavailable]",
  pageLabel: (page, pages) => `Page ${page} of ${pages}`,
};

const pageText = (doc) => doc.internal.pages.flat().join("\n");

const bytes = (blob) =>
  new Promise((resolve) => {
    const reader = new FileReader();
    reader.onload = () => resolve(Buffer.from(reader.result));
    reader.readAsArrayBuffer(blob);
  });

const kpiTable = () => ({
  section: kpis,
  title: "Key indicators",
  ...kpis.table(kpiData, intl),
});

describe("management export", () => {
  beforeEach(() => {
    document.body.innerHTML = '<div data-testid="mgmt-kpis"></div>';
    html2canvas.mockReset();
  });

  it("flattens the KPI strip to one row per indicator", () => {
    const { head, body } = kpis.table(kpiData, intl);
    expect(head).toEqual(["Indicator", "Value", "Prior period"]);
    expect(body).toHaveLength(5);
    expect(body[0]).toEqual(["Total samples", "65", "60"]);
    expect(body[2]).toEqual(["On-time completion", "60.66", "—"]);
  });

  it("flattens workload to one row per section", () => {
    const { head, body } = mapper("workload").table(
      {
        days: ["2026-10-11", "2026-10-12"],
        sections: [
          {
            id: "36",
            name: "Hematology",
            counts: [1, 2],
            samples: 31,
            medianTat: 20,
            p90Tat: 50,
            onTimePct: 61.29,
          },
        ],
      },
      intl,
    );
    expect(head).toEqual([
      "Section",
      "Samples",
      "Median TAT (h)",
      "P90 TAT (h)",
      "On-time %",
    ]);
    expect(body[0]).toEqual(["Hematology", "31", "20.00", "50.00", "61.29"]);
  });

  it("flattens equipment to one row per analyzer", () => {
    const fourHoursAgo = new Date(Date.now() - 4 * 3600000).toISOString();
    const { head, body } = mapper("equipment").table(
      [
        {
          id: "7",
          name: "Sysmex XN-550",
          samples: 12,
          section: "Hematology",
          lastActivityAt: fourHoursAgo,
        },
      ],
      intl,
    );
    expect(head).toEqual([
      "Analyzer",
      "Status",
      "Samples",
      "Section",
      "Last activity",
    ]);
    expect(body[0]).toEqual([
      "Sysmex XN-550",
      "Quiet",
      "12",
      "Hematology",
      fourHoursAgo,
    ]);
  });

  it("flattens the antibiogram to one row per organism and antibiotic", () => {
    const table = mapper("antibiogram").table(
      {
        organisms: [{ id: "o1", name: "E. coli", isolates: 42 }],
        antibiotics: [
          { id: "a1", name: "Ampicillin" },
          { id: "a2", name: "Meropenem" },
        ],
        cells: [
          { organismId: "o1", antibioticId: "a1", n: 40, pctR: 62.5 },
          { organismId: "o1", antibioticId: "a2", n: 6, pctR: null },
        ],
      },
      intl,
    );
    expect(table.head).toEqual([
      "Organism",
      "Antibiotic",
      "Tested",
      "% resistant",
    ]);
    expect(table.body[0]).toEqual(["E. coli", "Ampicillin", "40", "62.5"]);
    expect(toCsv(table).split("\r\n")[2]).toBe('"E. coli","Meropenem","6",""');
  });

  it("flattens the antibiogram fallback to organism counts", () => {
    const { head, body } = mapper("antibiogram").table(
      {
        organisms: [{ id: "o1", name: "E. coli", isolates: 3 }],
        antibiotics: [],
        cells: [],
      },
      intl,
    );
    expect(head).toEqual(["Organism", "Isolates"]);
    expect(body[0]).toEqual(["E. coli", "3"]);
  });

  it("flattens stock to one row per item across the three lists", () => {
    const { head, body } = mapper("stock").table(
      {
        state: "OK",
        criticalReagents: [
          {
            name: "Xpert MTB/RIF cartridge",
            lot: "L-01",
            expiry: "2026-12-31",
            onHand: 40,
            threshold: 50,
            status: "REORDER_SOON",
          },
        ],
        expiring: [
          { name: "CBC reagent", lot: "L-02", daysLeft: 12, remaining: 3 },
          {
            name: "Saline",
            lot: "L-03",
            daysLeft: -2,
            expired: true,
            remaining: 1,
          },
        ],
        reorder: [
          {
            name: "Gloves",
            onHand: 2,
            threshold: 10,
            kind: "BELOW",
            orderBy: null,
            orderedOn: null,
          },
        ],
      },
      intl,
    );
    expect(head).toEqual(["List", "Item", "Lot", "Quantity", "Detail"]);
    expect(body).toEqual([
      [
        "Critical reagents — on hand",
        "Xpert MTB/RIF cartridge",
        "L-01",
        "40",
        "2026-12-31",
      ],
      ["Expiring in 30 days", "CBC reagent", "L-02", "3", "Expires in 12 days"],
      ["Expiring in 30 days", "Saline", "L-03", "1", "Expired"],
      ["Reorder alerts", "Gloves", "—", "2", "Below reorder point"],
    ]);
  });

  it("flattens surveillance to one row per program metric", () => {
    const table = mapper("surveillance").table(
      {
        programs: [
          {
            code: "TB",
            name: "label.mgmt.surveillance.tb",
            testCount: 2,
            metrics: [
              {
                key: "MTB_DETECTION",
                label: "label.mgmt.surveillance.metric.mtbDetected",
                value: 30,
                numerator: 3,
                denominator: 10,
                delta: -2.5,
              },
              {
                key: "RIF_RESISTANCE",
                label: "RIF resistance",
                value: null,
                numerator: 0,
                denominator: 2,
                delta: null,
              },
            ],
            missingCodes: [],
          },
          {
            code: "LOCAL",
            name: "Cholera watch",
            testCount: 1,
            metrics: [],
            missingCodes: [],
          },
        ],
      },
      intl,
    );
    expect(table.head).toEqual([
      "Program",
      "Metric",
      "Value",
      "Positive",
      "Tested",
      "Change vs. prior (points)",
    ]);
    expect(table.body).toEqual([
      ["Tuberculosis", "MTB detected", "30.0", "3", "10", "-2.5"],
      ["Tuberculosis", "RIF resistance", "—", "0", "2", "—"],
    ]);
    expect(toCsv(table).split("\r\n")[2]).toBe(
      '"Tuberculosis","RIF resistance","","0","2",""',
    );
  });

  it("flattens quality to its components and the composite", () => {
    const { head, body } = mapper("quality").table(
      {
        composite: 95.4,
        prior: 100,
        components: [
          { key: "qcPass", value: 93.33, weight: 0.3 },
          { key: "eqa", value: null, weight: 0.25 },
        ],
      },
      intl,
    );
    expect(head).toEqual(["Components", "Value", "Weight"]);
    expect(body).toEqual([
      ["QC pass rate", "93.33", "0.30"],
      ["EQA performance", "—", "0.25"],
      ["Composite quality", "95.4", ""],
    ]);
  });

  it("quotes CSV cells that hold commas, quotes or line breaks", () => {
    expect(
      toCsv({ head: ["Item", "Detail"], body: [['Tips, 200 "µL"', "a\nb"]] }),
    ).toBe('\uFEFF"Item","Detail"\r\n"Tips, 200 ""µL""","a\nb"');
  });

  it("leaves a missing value as an empty CSV cell", () => {
    expect(
      toCsv({ head: ["KPI", "Prior"], body: [["QC pass rate", "—"]] }),
    ).toBe('\uFEFF"KPI","Prior"\r\n"QC pass rate",""');
  });

  it("keeps a name that starts like a formula as text but leaves numbers alone", () => {
    expect(
      toCsv({
        head: ["Name"],
        body: [["=HYPERLINK(1)", "@SUM(A1)", "+cmd", "-", "-2.5", "+1"]],
      }).split("\r\n")[1],
    ).toBe(`"'=HYPERLINK(1)","'@SUM(A1)","'+cmd","'-","-2.5","+1"`);
  });

  const quality = mapper("quality");
  const qualityTable = {
    section: quality,
    ...quality.table({ composite: 90, components: [] }, intl),
  };

  it("downloads one section as a CSV file", async () => {
    const { filename, blob } = csvDownload("stem", [kpiTable()]);

    expect(filename).toBe("stem-kpis.csv");
    expect((await bytes(blob)).toString()).toBe(toCsv(kpiTable()));
  });

  it("downloads several sections as one zip of CSV files", async () => {
    const { filename, blob } = csvDownload("stem", [kpiTable(), qualityTable]);
    const files = unzipSync(await bytes(blob));

    expect(filename).toBe("stem.zip");
    expect(Object.keys(files)).toEqual(["stem-kpis.csv", "stem-quality.csv"]);
    expect(Buffer.from(files["stem-quality.csv"]).toString()).toBe(
      toCsv(qualityTable),
    );
  });

  it("builds tables from the data the page already shows", async () => {
    const queryClient = new QueryClient();
    queryClient.setQueryData([SERVER_DATA_KEY, kpisUrl("30d")], kpiData);
    const fetch = vi.spyOn(global, "fetch");

    const [table] = await loadTables([kpis], "30d", intl, queryClient);

    expect(fetch).not.toHaveBeenCalled();
    expect(table.title).toBe("Key indicators");
    expect(table.body[0]).toEqual(["Total samples", "65", "60"]);
    fetch.mockRestore();
  });

  it("puts the captured picture, the table and page numbers in the PDF", async () => {
    html2canvas.mockResolvedValue({
      toDataURL: () => PIXEL,
      width: 100,
      height: 40,
    });

    const doc = await buildPdf([kpiTable()], meta);
    const pdf = pageText(doc);

    expect(html2canvas).toHaveBeenCalledTimes(1);
    expect(pdf).toContain("Key indicators");
    expect(pdf).toContain("On-time completion");
    expect(pdf).toContain("Page 1 of 1");
    expect(pdf).not.toContain("[Chart image unavailable]");
  });

  it("keeps a tall section's proportions when it caps the picture height", async () => {
    html2canvas.mockResolvedValue({
      toDataURL: () => PIXEL,
      width: 100,
      height: 200,
    });
    const addImage = vi.spyOn(jsPDF.API, "addImage");

    await buildPdf([kpiTable()], meta);

    expect(addImage).toHaveBeenCalledWith(
      PIXEL,
      "JPEG",
      14,
      expect.any(Number),
      74.25,
      148.5,
    );
    addImage.mockRestore();
  });

  it("drops the font name the canvas rejects from the captured section", async () => {
    html2canvas.mockResolvedValue({
      toDataURL: () => PIXEL,
      width: 100,
      height: 40,
    });
    document.body.innerHTML =
      '<style>.plex { font-family: "IBM Plex Sans", system-ui, ".SFNSText-Regular", sans-serif; }</style>' +
      '<div data-testid="mgmt-kpis" class="plex"><p class="plex">3,610</p></div>';

    await buildPdf([kpiTable()], meta);
    const [section, { onclone }] = html2canvas.mock.calls[0];
    onclone(document, section);

    expect(section.querySelector("p").style.fontFamily).toBe(
      '"IBM Plex Sans", system-ui, sans-serif',
    );
  });

  it("falls back to a placeholder when the picture cannot be taken", async () => {
    html2canvas.mockRejectedValue(new Error("tainted canvas"));

    const pdf = pageText(await buildPdf([kpiTable()], meta));

    expect(pdf).toContain("[Chart image unavailable]");
    expect(pdf).toContain("Total samples");
  });
});

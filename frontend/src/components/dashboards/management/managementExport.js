import { jsPDF } from "jspdf";
import autoTable from "jspdf-autotable";
import html2canvas from "html2canvas";
import { fetchFromOpenElisServer } from "../../utils/Utils";
import {
  antibiogramUrl,
  equipmentUrl,
  kpisUrl,
  qualityUrl,
  stockUrl,
  surveillanceUrl,
  workloadUrl,
} from "./managementApi";

const MARGIN = 14;
const PAGE_W = 210;
const PAGE_H = 297;
const CONTENT_W = PAGE_W - MARGIN * 2;
const FOOTER_Y = PAGE_H - 8;

const value = (v, digits = 2) =>
  v == null ? "—" : typeof v === "number" ? v.toFixed(digits) : String(v);

const t = (intl, id, values) => intl.formatMessage({ id }, values);

/**
 * One entry per exportable section: where its numbers come from, the DOM
 * wrapper whose picture goes in the PDF, and how its JSON flattens to a table.
 */
export const EXPORT_SECTIONS = [
  {
    id: "kpis",
    titleKey: "label.mgmt.export.section.kpis",
    testId: "mgmt-kpis",
    url: kpisUrl,
    table: (data, intl) => ({
      head: [
        t(intl, "label.mgmt.export.col.indicator"),
        t(intl, "label.mgmt.export.col.value"),
        t(intl, "label.mgmt.export.col.prior"),
      ],
      body: [
        ["label.mgmt.kpi.totalSamples", data.samples, 0],
        ["label.mgmt.kpi.medianTat", data.medianTat, 2],
        ["label.mgmt.kpi.onTime", data.onTime, 2],
        ["label.mgmt.kpi.rejectionRate", data.rejection, 2],
        ["label.mgmt.kpi.qcPassRate", data.qcPass, 2],
      ].map(([key, kpi, digits]) => [
        t(intl, key),
        value(kpi?.value, digits),
        value(kpi?.prior, digits),
      ]),
    }),
  },
  {
    id: "workload",
    titleKey: "label.mgmt.workload.title",
    testId: "mgmt-workload",
    url: workloadUrl,
    table: (data, intl) => ({
      head: [
        t(intl, "label.mgmt.workload.tatTable.section"),
        t(intl, "label.mgmt.workload.tatTable.samples"),
        t(intl, "label.mgmt.export.col.medianTatHours"),
        t(intl, "label.mgmt.export.col.p90TatHours"),
        t(intl, "label.mgmt.workload.tatTable.onTime"),
      ],
      body: (data.sections || []).map((s) => [
        s.name,
        String(s.samples),
        value(s.medianTat),
        value(s.p90Tat),
        value(s.onTimePct),
      ]),
    }),
  },
  {
    id: "equipment",
    titleKey: "label.mgmt.equipment.title",
    testId: "mgmt-equipment",
    url: equipmentUrl,
    table: (data, intl) => ({
      head: [
        t(intl, "label.mgmt.export.col.analyzer"),
        t(intl, "label.mgmt.export.col.status"),
        t(intl, "label.mgmt.workload.tatTable.samples"),
        t(intl, "label.mgmt.workload.tatTable.section"),
        t(intl, "label.mgmt.export.col.lastActivity"),
      ],
      body: (data || []).map((a) => [
        a.name,
        t(
          intl,
          {
            ACTIVE: "label.mgmt.equipment.status.active",
            QUIET: "label.mgmt.equipment.status.quiet",
            NO_ACTIVITY: "label.mgmt.equipment.status.noActivity",
          }[a.status] || "label.mgmt.equipment.status.noActivity",
        ),
        String(a.samples),
        a.section || "—",
        a.lastActivityAt || "—",
      ]),
    }),
  },
  {
    id: "antibiogram",
    titleKey: "label.mgmt.antibiogram.title",
    testId: "mgmt-antibiogram",
    url: antibiogramUrl,
    table: (data, intl) => {
      const organisms = Object.fromEntries(
        (data.organisms || []).map((o) => [o.id, o]),
      );
      if (data.fallback) {
        return {
          head: [
            t(intl, "label.mgmt.antibiogram.organismHeader"),
            t(intl, "label.mgmt.export.col.isolates"),
          ],
          body: (data.organisms || []).map((o) => [o.name, String(o.isolates)]),
        };
      }
      const antibiotics = Object.fromEntries(
        (data.antibiotics || []).map((a) => [a.id, a.name]),
      );
      return {
        head: [
          t(intl, "label.mgmt.antibiogram.organismHeader"),
          t(intl, "label.mgmt.export.col.antibiotic"),
          t(intl, "label.mgmt.export.col.tested"),
          t(intl, "label.mgmt.export.col.percentResistant"),
        ],
        body: (data.cells || []).map((c) => [
          organisms[c.organismId]?.name ?? c.organismId,
          antibiotics[c.antibioticId] ?? c.antibioticId,
          String(c.n),
          value(c.pctR, 1),
        ]),
      };
    },
  },
  {
    id: "quality",
    titleKey: "label.mgmt.quality.title",
    testId: "mgmt-quality",
    url: qualityUrl,
    table: (data, intl) => ({
      head: [
        t(intl, "label.mgmt.quality.components"),
        t(intl, "label.mgmt.export.col.value"),
        t(intl, "label.mgmt.export.col.weight"),
      ],
      body: [
        ...(data.components || []).map((c) => [
          t(
            intl,
            {
              qcPass: "label.mgmt.quality.qcPass",
              eqa: "label.mgmt.quality.eqaPerf",
              nceInverse: "label.mgmt.quality.nceInverse",
              amendmentInverse: "label.mgmt.quality.amendmentInverse",
            }[c.key],
          ),
          value(c.value),
          value(c.weight),
        ]),
        [t(intl, "label.mgmt.quality.composite"), value(data.composite, 1), ""],
      ],
    }),
  },
  {
    id: "surveillance",
    titleKey: "label.mgmt.surveillance.title",
    testId: "mgmt-surveillance",
    url: surveillanceUrl,
    table: (data, intl) => ({
      head: [
        t(intl, "label.mgmt.export.col.program"),
        t(intl, "label.mgmt.export.col.metric"),
        t(intl, "label.mgmt.export.col.value"),
        t(intl, "label.mgmt.export.col.positive"),
        t(intl, "label.mgmt.export.col.tested"),
        t(intl, "label.mgmt.export.col.deltaPoints"),
      ],
      body: (data.programs || []).flatMap((program) =>
        program.metrics.map((metric) => [
          intl.messages[program.name] ? t(intl, program.name) : program.name,
          metric.label,
          value(metric.value, 1),
          String(metric.numerator),
          String(metric.denominator),
          value(metric.delta, 1),
        ]),
      ),
    }),
  },
  {
    id: "stock",
    titleKey: "label.mgmt.stock.title",
    testId: "mgmt-stock",
    url: () => stockUrl(),
    table: (data, intl) => ({
      head: [
        t(intl, "label.mgmt.export.col.list"),
        t(intl, "label.mgmt.export.col.item"),
        t(intl, "label.mgmt.export.col.lot"),
        t(intl, "label.mgmt.export.col.quantity"),
        t(intl, "label.mgmt.export.col.detail"),
      ],
      body: [
        ...(data.criticalReagents || []).map((r) => [
          t(intl, "label.mgmt.stock.criticalReagents"),
          r.name,
          r.lot || "—",
          value(r.onHand, 0),
          r.expiry || "—",
        ]),
        ...(data.expiring || []).map((l) => [
          t(intl, "label.mgmt.stock.expiringIn30"),
          l.name,
          l.lot || "—",
          value(l.remaining, 0),
          t(intl, "label.mgmt.stock.expiresIn", { days: l.daysLeft }),
        ]),
        ...(data.reorder || []).map((r) => [
          t(intl, "label.mgmt.stock.reorderAlerts"),
          r.name,
          "—",
          value(r.onHand, 0),
          t(
            intl,
            r.kind === "BELOW"
              ? "label.mgmt.stock.belowReorder"
              : "label.mgmt.stock.approachingReorder",
          ),
        ]),
      ],
    }),
  },
];

export const sectionsOnPage = () =>
  EXPORT_SECTIONS.filter((s) =>
    document.querySelector(`[data-testid="${s.testId}"]`),
  );

/** Each section's table for the scope, read fresh from the server. */
export const loadTables = (sections, scope, intl) =>
  Promise.all(
    sections.map((section) =>
      fetchFromOpenElisServer(section.url(scope)).then((data) => ({
        section,
        title: t(intl, section.titleKey, { 0: "" }).trim(),
        ...section.table(data, intl),
      })),
    ),
  );

const csvCell = (cell) => {
  const text = cell == null || cell === "—" ? "" : String(cell);
  return /[",\r\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
};

// The byte order mark is what makes Excel read the file as UTF-8.
export const toCsv = ({ head, body }) =>
  "\uFEFF" +
  [head, ...body].map((row) => row.map(csvCell).join(",")).join("\r\n");

export const saveFile = (filename, content, type) => {
  const url = URL.createObjectURL(new Blob([content], { type }));
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
};

const capture = (element) =>
  element
    ? html2canvas(element, {
        scale: 1,
        backgroundColor: "#ffffff",
        useCORS: true,
        logging: false,
      })
        .then((canvas) => ({
          data: canvas.toDataURL("image/jpeg", 0.85),
          ratio: canvas.height / canvas.width,
        }))
        .catch(() => null)
    : Promise.resolve(null);

export const buildPdf = (tables, meta) =>
  Promise.all(
    tables.map((table) =>
      capture(
        document.querySelector(`[data-testid="${table.section.testId}"]`),
      ),
    ),
  ).then((images) => {
    const doc = new jsPDF({
      orientation: "portrait",
      unit: "mm",
      format: "a4",
      compress: true,
    });
    doc.setFontSize(18);
    doc.setFont("helvetica", "bold");
    doc.text(meta.title, MARGIN, 24);
    doc.setFont("helvetica", "normal");
    doc.setFontSize(10);
    meta.cover.forEach(([label, text], i) => {
      doc.text(`${label}: ${text}`, MARGIN, 34 + i * 6);
    });
    let y = 34 + meta.cover.length * 6 + 6;

    tables.forEach((table, i) => {
      const image = images[i];
      const imageH = image ? Math.min(CONTENT_W * image.ratio, PAGE_H / 2) : 12;
      if (y + 10 + imageH > PAGE_H - 20) {
        doc.addPage();
        y = 20;
      }
      doc.setFontSize(13);
      doc.setFont("helvetica", "bold");
      doc.text(table.title, MARGIN, y);
      doc.setFont("helvetica", "normal");
      y += 4;
      if (image) {
        doc.addImage(image.data, "JPEG", MARGIN, y, CONTENT_W, imageH);
      } else {
        doc.setFontSize(9);
        doc.setTextColor(150);
        doc.text(meta.chartUnavailable, PAGE_W / 2, y + imageH / 2, {
          align: "center",
        });
        doc.setTextColor(0);
      }
      autoTable(doc, {
        startY: y + imageH + 4,
        head: [table.head],
        body: table.body,
        styles: { fontSize: 8, cellPadding: 1.5 },
        headStyles: { fillColor: [0, 98, 196], textColor: 255 },
        margin: { left: MARGIN, right: MARGIN, bottom: 16 },
      });
      y = doc.lastAutoTable.finalY + 10;
    });

    const pages = doc.getNumberOfPages();
    for (let page = 1; page <= pages; page++) {
      doc.setPage(page);
      doc.setFontSize(8);
      doc.setTextColor(120);
      doc.text(meta.labName, MARGIN, FOOTER_Y);
      doc.text(meta.pageLabel(page, pages), PAGE_W - MARGIN, FOOTER_Y, {
        align: "right",
      });
      doc.setTextColor(0);
    }
    return doc;
  });

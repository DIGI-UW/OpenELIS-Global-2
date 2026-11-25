import { jsPDF } from "jspdf";
import autoTable from "jspdf-autotable";
import html2canvas from "html2canvas";
import { strToU8, zipSync } from "fflate";
import { csvCell } from "../../utils/Utils";
import { serverQuery } from "../../utils/queryClient";
import { SERVER_DATA_KEY } from "../../utils/useServerData";
import { statusOf } from "./EquipmentSection";
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

const keyOrText = (intl, text) => (intl.messages[text] ? t(intl, text) : text);

export const EXPORT_SECTIONS = [
  {
    id: "kpis",
    titleKey: "label.mgmt.export.section.kpis",
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
          }[statusOf(a.lastActivityAt)],
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
    url: antibiogramUrl,
    table: (data, intl) => {
      const organisms = Object.fromEntries(
        (data.organisms || []).map((o) => [o.id, o]),
      );
      if (!data.cells || data.cells.length === 0) {
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
          keyOrText(intl, program.name),
          keyOrText(intl, metric.label),
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
    url: stockUrl,
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
          t(
            intl,
            l.expired
              ? "label.mgmt.stock.expired"
              : "label.mgmt.stock.expiresIn",
            { days: l.daysLeft },
          ),
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
    document.querySelector(`[data-testid="mgmt-${s.id}"]`),
  );

export const loadTables = (sections, scope, intl, queryClient) =>
  Promise.all(
    sections.map((section) => {
      const url = section.url(scope);
      return queryClient
        .ensureQueryData(serverQuery([SERVER_DATA_KEY, url], url))
        .then((data) => ({
          section,
          title: t(intl, section.titleKey),
          ...section.table(data, intl),
        }));
    }),
  );

const FORMULA_START = /^[=+\-@\t\r]/;

const exportCell = (cell) => {
  const text = cell == null || cell === "—" ? "" : String(cell);
  return csvCell(
    FORMULA_START.test(text) && !Number.isFinite(Number(text))
      ? `'${text}`
      : text,
  );
};

// The byte order mark is what makes Excel read the file as UTF-8.
export const toCsv = ({ head, body }) =>
  "\uFEFF" +
  [head, ...body].map((row) => row.map(exportCell).join(",")).join("\r\n");

export const csvDownload = (stem, tables) => {
  const name = (table) => `${stem}-${table.section.id}.csv`;
  if (tables.length === 1) {
    return {
      filename: name(tables[0]),
      blob: new Blob([toCsv(tables[0])], { type: "text/csv;charset=utf-8" }),
    };
  }
  const files = Object.fromEntries(
    tables.map((table) => [name(table), strToU8(toCsv(table))]),
  );
  return {
    filename: `${stem}.zip`,
    blob: new Blob([zipSync(files)], { type: "application/zip" }),
  };
};

const capture = (element, title) =>
  element
    ? html2canvas(element, {
        scale: 1,
        backgroundColor: "#ffffff",
        useCORS: true,
        logging: false,
        onclone: (clone, section) => {
          // html2canvas leaves Carbon's ".SFNSText-Regular" unquoted, and the canvas rejects the whole font.
          [section, ...section.querySelectorAll("*")].forEach((el) => {
            el.style.fontFamily = clone.defaultView
              .getComputedStyle(el)
              .fontFamily.replace(/,\s*"\.[^"]*"/g, "");
          });
          // The PDF prints the section title as text above the picture.
          section.querySelectorAll("h1, h2, h3, h4, h5, h6").forEach((h) => {
            if (h.textContent.trim() === title) h.style.display = "none";
          });
        },
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
        document.querySelector(`[data-testid="mgmt-${table.section.id}"]`),
        table.title,
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
        doc.addImage(
          image.data,
          "JPEG",
          MARGIN,
          y,
          imageH / image.ratio,
          imageH,
        );
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

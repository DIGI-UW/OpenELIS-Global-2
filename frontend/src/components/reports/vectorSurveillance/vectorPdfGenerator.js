import jsPDF from "jspdf";
import "jspdf-autotable";

/**
 * Client-side PDF export of the Vector Surveillance dashboard (US3 / FR-006).
 * Mirrors shipment/utils/pdfGenerator.js (jsPDF + jspdf-autotable). The PDF
 * reflects the on-screen figures and the active filter scope. All labels are
 * passed in via React-Intl formatMessage (CR-002 / FR-014) — no hardcoded
 * display strings here.
 *
 * @param {object} indices  The SurveillanceIndicesDTO payload currently shown.
 * @param {object} scope     { dateFrom, dateTo, siteName } describing the filter.
 * @param {(descriptor: {id: string}) => string} formatMessage
 */
export const generateVectorSurveillancePDF = (indices, scope, formatMessage) => {
  const t = (id) => formatMessage({ id });
  const doc = new jsPDF();

  // Title
  doc.setFontSize(16);
  doc.setFont(undefined, "bold");
  doc.text(t("vectorReport.title"), 14, 18);

  // Scope line (date range + site + freshness)
  doc.setFontSize(10);
  doc.setFont(undefined, "normal");
  let yPos = 26;
  const scopeRows = [
    [t("vectorReport.filter.dateRange"), `${scope.dateFrom} — ${scope.dateTo}`],
    [
      t("vectorReport.filter.site"),
      scope.siteName || t("vectorReport.filter.allSites"),
    ],
    [
      t("vectorReport.freshness"),
      indices.freshness
        ? new Date(indices.freshness).toLocaleString()
        : t("vectorReport.freshness.unknown"),
    ],
  ];
  doc.autoTable({
    startY: yPos,
    head: [],
    body: scopeRows,
    theme: "plain",
    styles: { fontSize: 9 },
    columnStyles: { 0: { fontStyle: "bold", cellWidth: 50 } },
    margin: { left: 14 },
  });
  yPos = doc.lastAutoTable.finalY + 6;

  const sectionHeader = (titleId) => {
    if (yPos > 260) {
      doc.addPage();
      yPos = 18;
    }
    doc.setFontSize(12);
    doc.setFont(undefined, "bold");
    doc.text(t(titleId), 14, yPos);
    yPos += 2;
  };

  // Collection density
  sectionHeader("vectorReport.density.title");
  doc.autoTable({
    startY: yPos,
    head: [
      [
        t("vectorReport.density.period"),
        t("vectorReport.density.site"),
        t("vectorReport.density.pools"),
        t("vectorReport.density.specimens"),
      ],
    ],
    body: (indices.collectionDensity || []).map((r) => [
      r.periodLabel,
      r.siteName || "-",
      r.poolCount,
      r.specimenCount,
    ]),
    theme: "striped",
    styles: { fontSize: 9 },
    margin: { left: 14, right: 14 },
  });
  yPos = doc.lastAutoTable.finalY + 6;

  // Species distribution
  sectionHeader("vectorReport.species.title");
  doc.autoTable({
    startY: yPos,
    head: [
      [
        t("vectorReport.species.genus"),
        t("vectorReport.species.species"),
        t("vectorReport.species.specimens"),
        t("vectorReport.species.pct"),
      ],
    ],
    body: (indices.speciesDistribution || []).map((r) => [
      r.genus,
      r.species,
      r.specimenCount,
      `${Number(r.pct).toFixed(1)}%`,
    ]),
    theme: "striped",
    styles: { fontSize: 9 },
    margin: { left: 14, right: 14 },
  });
  yPos = doc.lastAutoTable.finalY + 6;

  // MIR by species
  sectionHeader("vectorReport.mir.title");
  doc.autoTable({
    startY: yPos,
    head: [
      [
        t("vectorReport.mir.species"),
        t("vectorReport.mir.pathogen"),
        t("vectorReport.mir.classic"),
        t("vectorReport.mir.observed"),
        t("vectorReport.mir.resolution"),
        t("vectorReport.mir.positivePools"),
        t("vectorReport.mir.totalSpecimens"),
      ],
    ],
    body: (indices.mirBySpecies || []).map((r) => [
      r.speciesLabel,
      r.pathogen,
      Number(r.mirClassic).toFixed(2),
      Number(r.infectionRateObserved).toFixed(2),
      `${Number(r.positiveResolutionPct).toFixed(1)}%`,
      r.positivePools,
      r.totalSpecimens,
    ]),
    theme: "striped",
    styles: { fontSize: 9 },
    margin: { left: 14, right: 14 },
  });
  yPos = doc.lastAutoTable.finalY + 6;

  // Sporozoite rate (Anopheles CSP-ELISA) — top-level figure
  if (indices.sporozoiteRatePct != null) {
    doc.setFontSize(10);
    doc.text(
      `${t("vectorReport.sporozoite.title")}: ${Number(
        indices.sporozoiteRatePct,
      ).toFixed(2)}%`,
      14,
      yPos,
    );
    yPos += 6;
  }

  // Pathogen positivity
  sectionHeader("vectorReport.positivity.title");
  doc.autoTable({
    startY: yPos,
    head: [
      [
        t("vectorReport.positivity.pathogen"),
        t("vectorReport.positivity.poolsPositive"),
        t("vectorReport.positivity.poolsTested"),
        t("vectorReport.positivity.pct"),
      ],
    ],
    body: (indices.pathogenPositivity || []).map((r) => [
      r.pathogen,
      r.poolsPositive,
      r.poolsTested,
      `${Number(r.positivityPct).toFixed(1)}%`,
    ]),
    theme: "striped",
    styles: { fontSize: 9 },
    margin: { left: 14, right: 14 },
  });
  yPos = doc.lastAutoTable.finalY + 6;

  // QC pass-rate
  const qc = indices.qcPassRate || {};
  sectionHeader("vectorReport.qc.title");
  doc.autoTable({
    startY: yPos,
    head: [],
    body: [
      [
        t("vectorReport.qc.passRate"),
        `${Number(qc.passRatePct || 0).toFixed(1)}%`,
      ],
      [t("vectorReport.qc.passed"), `${qc.analysesPassed || 0}`],
      [t("vectorReport.qc.total"), `${qc.analysesTotal || 0}`],
    ],
    theme: "plain",
    styles: { fontSize: 9 },
    columnStyles: { 0: { fontStyle: "bold", cellWidth: 60 } },
    margin: { left: 14 },
  });

  // Generated timestamp footer
  doc.setFontSize(8);
  doc.setFont(undefined, "normal");
  doc.text(
    `${t("vectorReport.generated")} ${new Date().toLocaleString()}`,
    14,
    doc.lastAutoTable.finalY + 10,
  );

  doc.save("vector-surveillance.pdf");
};

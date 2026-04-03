import jsPDF from "jspdf";
import "jspdf-autotable";
import bwipjs from "bwip-js";

/**
 * Generate a Code128 barcode as a base64 PNG data URL.
 * @param {string} text - Text to encode
 * @param {Object} [opts] - Optional overrides for bwip-js
 * @returns {Promise<string>} base64 data URL
 */
const generateBarcodeDataUrl = async (text, opts = {}) => {
  const canvas = document.createElement("canvas");
  bwipjs.toCanvas(canvas, {
    bcid: "code128",
    text: text,
    scale: 3,
    height: 8,
    includetext: true,
    textxalign: "center",
    textsize: 8,
    ...opts,
  });
  return canvas.toDataURL("image/png");
};

/**
 * Generate shipping manifest PDF with barcoded specimen IDs and service location
 * @param {Object} manifestData - Manifest data from backend
 * @param {Function} formatMessage - React Intl formatMessage function
 */
export const generateManifestPDF = async (manifestData, formatMessage) => {
  const doc = new jsPDF();

  // Title
  doc.setFontSize(18);
  doc.setFont(undefined, "bold");
  doc.text(
    formatMessage({ id: "shipment.manifest.title" }) || "Shipping Manifest",
    105,
    20,
    {
      align: "center",
    },
  );

  // Box Information Section
  doc.setFontSize(12);
  doc.setFont(undefined, "normal");
  let yPos = 40;

  const boxInfo = [
    [
      formatMessage({ id: "shipment.box.id" }) || "Box ID:",
      manifestData.boxId || "-",
    ],
    [
      formatMessage({ id: "shipment.manifest.serviceLocation" }) ||
        "Service Location:",
      manifestData.serviceLocation || "-",
    ],
    [
      formatMessage({ id: "shipment.box.destination" }) || "Destination:",
      manifestData.destinationFacility || "-",
    ],
    [
      formatMessage({ id: "shipment.box.state" }) || "State:",
      manifestData.state || "-",
    ],
    [
      formatMessage({ id: "shipment.box.temperature" }) || "Temperature:",
      manifestData.temperature || "AMBIENT",
    ],
    [
      formatMessage({ id: "shipment.box.created" }) || "Created:",
      manifestData.createdDate
        ? new Date(manifestData.createdDate).toLocaleString()
        : "-",
    ],
    [
      formatMessage({ id: "shipment.box.createdBy" }) || "Created By:",
      manifestData.createdBy || "-",
    ],
  ];

  doc.autoTable({
    startY: yPos,
    head: [],
    body: boxInfo,
    theme: "plain",
    columnStyles: {
      0: { fontStyle: "bold", cellWidth: 60 },
      1: { cellWidth: 120 },
    },
    margin: { left: 20 },
  });

  // Box ID barcode
  try {
    const boxBarcode = await generateBarcodeDataUrl(
      manifestData.boxId || "UNKNOWN",
      { height: 10 },
    );
    yPos = doc.lastAutoTable.finalY + 5;
    doc.addImage(boxBarcode, "PNG", 20, yPos, 60, 12);
    yPos += 17;
  } catch {
    yPos = doc.lastAutoTable.finalY + 10;
  }

  // Pre-generate barcodes for all samples
  const sampleBarcodes = {};
  for (const sample of manifestData.samples) {
    if (sample.accessionNumber) {
      try {
        sampleBarcodes[sample.accessionNumber] = await generateBarcodeDataUrl(
          sample.accessionNumber,
        );
      } catch {
        // skip barcode if generation fails
      }
    }
  }

  // Samples Table header
  doc.setFontSize(14);
  doc.setFont(undefined, "bold");
  doc.text(
    formatMessage({ id: "shipment.label.samples" }) || "Samples",
    20,
    yPos,
  );

  const samplesTableData = manifestData.samples.map((sample, index) => [
    (index + 1).toString(),
    sample.accessionNumber || "-",
    sample.typeOfSample || "-",
    sample.referralTests || "-",
    sample.collectionDate
      ? new Date(sample.collectionDate).toLocaleDateString()
      : "-",
  ]);

  doc.autoTable({
    startY: yPos + 5,
    head: [
      [
        formatMessage({ id: "shipment.manifest.number" }) || "#",
        formatMessage({ id: "sample.label.accessionNumber" }) ||
          "Accession Number",
        formatMessage({ id: "sample.label.typeOfSample" }) || "Type",
        formatMessage({ id: "shipment.label.tests" }) || "Tests",
        formatMessage({ id: "sample.label.collectionDate" }) ||
          "Collection Date",
      ],
    ],
    body: samplesTableData,
    theme: "striped",
    headStyles: {
      fillColor: [200, 200, 200],
      textColor: [0, 0, 0],
      fontStyle: "bold",
    },
    margin: { left: 20, right: 20 },
  });

  // Barcoded specimen IDs section
  yPos = doc.lastAutoTable.finalY + 10;
  doc.setFontSize(12);
  doc.setFont(undefined, "bold");
  doc.text(
    formatMessage({ id: "shipment.manifest.specimenBarcodes" }) ||
      "Specimen Barcodes",
    20,
    yPos,
  );
  yPos += 5;

  const barcodeH = 10;
  const barcodeW = 50;
  const colGap = 5;
  const cols = 3;
  const colWidth = barcodeW + colGap;
  let col = 0;

  for (const sample of manifestData.samples) {
    const barcodeImg = sampleBarcodes[sample.accessionNumber];
    if (!barcodeImg) continue;

    // Check if we need a new page
    if (yPos + barcodeH + 5 > 280) {
      doc.addPage();
      yPos = 20;
      col = 0;
    }

    const x = 20 + col * colWidth;
    doc.addImage(barcodeImg, "PNG", x, yPos, barcodeW, barcodeH);

    col++;
    if (col >= cols) {
      col = 0;
      yPos += barcodeH + 5;
    }
  }

  // Move past the last row of barcodes
  if (col > 0) {
    yPos += barcodeH + 5;
  }

  // Footer
  yPos += 10;
  // Check if we need a new page for footer
  if (yPos > 270) {
    doc.addPage();
    yPos = 20;
  }

  doc.setFontSize(12);
  doc.setFont(undefined, "bold");
  doc.text(
    `${formatMessage({ id: "shipment.manifest.totalSamples" }) || "Total Samples:"} ${manifestData.samples.length}`,
    20,
    yPos,
  );

  // Notes if present
  if (manifestData.notes?.trim()) {
    yPos += 10;
    doc.setFont(undefined, "bold");
    doc.text(formatMessage({ id: "shipment.box.notes" }) || "Notes:", 20, yPos);
    yPos += 7;
    doc.setFont(undefined, "normal");
    const splitNotes = doc.splitTextToSize(manifestData.notes, 170);
    doc.text(splitNotes, 20, yPos);
    yPos += splitNotes.length * 7;
  }

  // Generated timestamp
  yPos += 10;
  doc.setFontSize(8);
  doc.setFont(undefined, "normal");
  doc.text(
    `${formatMessage({ id: "shipment.manifest.generated" }) || "Generated:"} ${new Date().toLocaleString()}`,
    20,
    yPos,
  );

  // Save PDF
  doc.save(`manifest-${manifestData.boxId}.pdf`);
};

/**
 * Generate shipping box label PDF with improved layout
 * @param {Object} boxData - Box data
 * @param {Object} boxData.boxId - Box identifier
 * @param {Object} boxData.destinationFacility - Destination name
 * @param {Object} boxData.temperature - Temperature requirement
 * @param {number} boxData.sampleCount - Total number of samples
 * @param {Object} boxData.sampleTypeCounts - Map of type name to count, e.g. { "Serum": 3, "Plasma": 2 }
 * @param {Function} formatMessage - React Intl formatMessage function
 */
export const generateLabelPDF = (boxData, formatMessage) => {
  const pageW = 150;
  const pageH = 100;
  const doc = new jsPDF({
    orientation: "landscape",
    unit: "mm",
    format: [pageH, pageW],
  });

  const margin = 8;
  const innerW = pageW - margin * 2;

  // --- Header (black text, no background — printer-friendly) ---
  doc.setTextColor(0, 0, 0);
  doc.setFontSize(11);
  doc.setFont(undefined, "bold");
  doc.text(
    (
      formatMessage({ id: "shipment.label.shippingLabel" }) || "SHIPPING LABEL"
    ).toUpperCase(),
    pageW / 2,
    9,
    { align: "center" },
  );

  // Box ID — large, centered, black
  doc.setFontSize(26);
  doc.text(boxData.boxId || "", pageW / 2, 22, { align: "center" });

  // Header separator line
  doc.setDrawColor(0, 0, 0);
  doc.setLineWidth(0.8);
  doc.line(margin, 27, pageW - margin, 27);

  // --- Two-column layout below header ---
  let yPos = 33;
  const col1X = margin;
  const col2X = pageW / 2 + 4;
  const colW = innerW / 2 - 4;

  // Left column: Destination
  doc.setFontSize(8);
  doc.setFont(undefined, "bold");
  doc.setTextColor(100, 100, 100);
  doc.text(
    (
      formatMessage({ id: "shipment.box.destination" }) || "Destination"
    ).toUpperCase(),
    col1X,
    yPos,
  );
  doc.setTextColor(0, 0, 0);
  doc.setFontSize(11);
  doc.setFont(undefined, "normal");
  const destLines = doc.splitTextToSize(
    boxData.destinationFacility || "-",
    colW,
  );
  doc.text(destLines, col1X, yPos + 5);

  // Right column: Temperature
  doc.setFontSize(8);
  doc.setFont(undefined, "bold");
  doc.setTextColor(100, 100, 100);
  doc.text(
    (
      formatMessage({ id: "shipment.box.temperature" }) || "Temperature"
    ).toUpperCase(),
    col2X,
    yPos,
  );
  doc.setTextColor(0, 0, 0);
  doc.setFontSize(11);
  doc.setFont(undefined, "normal");
  doc.text(boxData.temperature || "AMBIENT", col2X, yPos + 5);

  // --- Separator line ---
  yPos = yPos + 5 + destLines.length * 5 + 4;
  doc.setDrawColor(200, 200, 200);
  doc.setLineWidth(0.3);
  doc.line(margin, yPos, pageW - margin, yPos);
  yPos += 5;

  // --- Sample Types section with counts ---
  const typeCounts = boxData.sampleTypeCounts || {};
  const typeEntries = Object.entries(typeCounts);

  doc.setFontSize(8);
  doc.setFont(undefined, "bold");
  doc.setTextColor(100, 100, 100);
  doc.text(
    (
      formatMessage({ id: "sample.label.types" }) || "Sample Types"
    ).toUpperCase(),
    col1X,
    yPos,
  );
  yPos += 5;

  doc.setTextColor(0, 0, 0);
  doc.setFontSize(10);

  if (typeEntries.length > 0) {
    // Render each type with its count as a row
    typeEntries.forEach(([type, count]) => {
      doc.setFont(undefined, "normal");
      doc.text(type, col1X + 2, yPos);
      doc.setFont(undefined, "bold");
      doc.text(`x${count}`, col1X + 60, yPos);
      yPos += 5;
    });
  } else {
    doc.setFont(undefined, "normal");
    doc.text("-", col1X + 2, yPos);
    yPos += 5;
  }

  // --- Total samples — prominent ---
  yPos += 2;
  doc.setDrawColor(200, 200, 200);
  doc.line(margin, yPos, pageW - margin, yPos);
  yPos += 5;

  // Total badge — bordered rectangle, printer-friendly
  const totalLabel = `${formatMessage({ id: "shipment.manifest.totalSamples" }) || "Total Samples:"} ${boxData.sampleCount || 0}`;
  doc.setFontSize(12);
  doc.setFont(undefined, "bold");
  doc.setTextColor(0, 0, 0);
  const totalW = doc.getTextWidth(totalLabel) + 12;
  doc.setDrawColor(0, 0, 0);
  doc.setLineWidth(0.5);
  doc.roundedRect(col1X, yPos - 4, totalW, 8, 2, 2, "S");
  doc.text(totalLabel, col1X + 6, yPos + 1.5);

  // --- Footer: date ---
  doc.setFontSize(7);
  doc.setFont(undefined, "normal");
  doc.setTextColor(130, 130, 130);
  doc.text(
    `${formatMessage({ id: "shipment.box.created" }) || "Created:"} ${boxData.createdDate ? new Date(boxData.createdDate).toLocaleDateString() : "-"}`,
    margin,
    pageH - 4,
  );
  doc.text(new Date().toLocaleDateString(), pageW - margin, pageH - 4, {
    align: "right",
  });

  // Save PDF
  const boxIdStr = typeof boxData.boxId === "string" ? boxData.boxId : "box";
  doc.save(`label-${boxIdStr}.pdf`);
};

import React, { useState, useCallback, useRef, useEffect } from "react";
import {
  Button,
  Dropdown,
  Checkbox,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectAll,
  TableSelectRow,
  InlineNotification,
  Grid,
  Column,
  Tag,
} from "@carbon/react";
import { Printer, Download, Renew } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";
import { getFromOpenElisServer } from "../../../utils/Utils";

/**
 * BarcodeGenerator - Component for generating and printing sample barcodes
 * Sub-stage 1d of the Biorepository Intake workflow
 *
 * Features:
 * - Generate DataMatrix 2D barcodes
 * - Multiple label sizes
 * - Batch printing
 * - Preview before printing
 *
 * @param {Object} props
 * @param {Array} props.samples - Array of samples to generate barcodes for
 * @param {number} props.shipmentId - Optional shipment ID to load samples from
 * @param {Function} props.onComplete - Callback when barcode generation is complete
 */
function BarcodeGenerator({ samples: propSamples, shipmentId, onComplete }) {
  const intl = useIntl();
  const printRef = useRef(null);

  const [samples, setSamples] = useState(propSamples || []);
  const [selectedRows, setSelectedRows] = useState([]);
  const [labelSize, setLabelSize] = useState("standard");
  const [includeExternalId, setIncludeExternalId] = useState(true);
  const [includeDate, setIncludeDate] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [generatedBarcodes, setGeneratedBarcodes] = useState({});

  const labelSizes = [
    {
      id: "small",
      text: intl.formatMessage({
        id: "biorepository.barcode.size.small",
        defaultMessage: 'Small (1" x 0.5")',
      }),
      width: 72,
      height: 36,
    },
    {
      id: "standard",
      text: intl.formatMessage({
        id: "biorepository.barcode.size.standard",
        defaultMessage: 'Standard (2" x 1")',
      }),
      width: 144,
      height: 72,
    },
    {
      id: "large",
      text: intl.formatMessage({
        id: "biorepository.barcode.size.large",
        defaultMessage: 'Large (3" x 1.5")',
      }),
      width: 216,
      height: 108,
    },
  ];

  // Load samples from shipment if shipmentId provided
  useEffect(() => {
    if (shipmentId && (!propSamples || propSamples.length === 0)) {
      setLoading(true);
      getFromOpenElisServer(
        `/rest/biorepository/shipment/${shipmentId}/samples`,
        (data) => {
          if (data && Array.isArray(data)) {
            setSamples(data);
          }
          setLoading(false);
        },
      );
    }
  }, [shipmentId, propSamples]);

  // Generate a simple DataMatrix-like pattern using canvas
  const generateBarcodeImage = useCallback((barcode, size) => {
    const canvas = document.createElement("canvas");
    const ctx = canvas.getContext("2d");
    const moduleSize = 4;
    const modules = 16; // 16x16 grid for DataMatrix

    canvas.width = modules * moduleSize;
    canvas.height = modules * moduleSize;

    // Create a pattern based on the barcode string
    ctx.fillStyle = "#ffffff";
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    ctx.fillStyle = "#000000";

    // Simple hash-based pattern generation (for demo - use a proper DataMatrix library in production)
    let hash = 0;
    for (let i = 0; i < barcode.length; i++) {
      hash = (hash << 5) - hash + barcode.charCodeAt(i);
      hash = hash & hash;
    }

    // Draw finder pattern (L-shaped border)
    // Left border
    for (let i = 0; i < modules; i++) {
      ctx.fillRect(0, i * moduleSize, moduleSize, moduleSize);
    }
    // Bottom border
    for (let i = 0; i < modules; i++) {
      ctx.fillRect(
        i * moduleSize,
        (modules - 1) * moduleSize,
        moduleSize,
        moduleSize,
      );
    }
    // Top alternating
    for (let i = 0; i < modules; i += 2) {
      ctx.fillRect(i * moduleSize, 0, moduleSize, moduleSize);
    }
    // Right alternating
    for (let i = 0; i < modules; i += 2) {
      ctx.fillRect(
        (modules - 1) * moduleSize,
        i * moduleSize,
        moduleSize,
        moduleSize,
      );
    }

    // Fill data pattern
    for (let y = 1; y < modules - 1; y++) {
      for (let x = 1; x < modules - 1; x++) {
        const index = y * modules + x;
        const bit = (hash >> index % 32) & 1;
        if (bit || (x + y) % 3 === 0) {
          ctx.fillRect(x * moduleSize, y * moduleSize, moduleSize, moduleSize);
        }
      }
    }

    return canvas.toDataURL("image/png");
  }, []);

  // Generate barcodes for selected samples
  const handleGenerateBarcodes = useCallback(() => {
    const samplesToGenerate =
      selectedRows.length > 0
        ? samples.filter((s) => selectedRows.includes(String(s.id)))
        : samples;

    const selectedSize =
      labelSizes.find((s) => s.id === labelSize) || labelSizes[1];
    const barcodes = {};

    samplesToGenerate.forEach((sample) => {
      barcodes[sample.id] = generateBarcodeImage(sample.barcode, selectedSize);
    });

    setGeneratedBarcodes(barcodes);
  }, [samples, selectedRows, labelSize, labelSizes, generateBarcodeImage]);

  // Print labels
  const handlePrint = useCallback(() => {
    const printWindow = window.open("", "_blank");
    if (!printWindow) {
      setError(
        intl.formatMessage({
          id: "biorepository.barcode.error.popupBlocked",
          defaultMessage: "Please allow popups to print labels",
        }),
      );
      return;
    }

    const selectedSize =
      labelSizes.find((s) => s.id === labelSize) || labelSizes[1];
    const samplesToprint =
      selectedRows.length > 0
        ? samples.filter((s) => selectedRows.includes(String(s.id)))
        : samples;

    let labelsHtml = "";
    samplesToprint.forEach((sample) => {
      const barcodeImage = generatedBarcodes[sample.id];
      if (barcodeImage) {
        labelsHtml += `
          <div class="label" style="
            width: ${selectedSize.width}px;
            height: ${selectedSize.height}px;
            border: 1px solid #ccc;
            padding: 4px;
            margin: 4px;
            display: inline-block;
            text-align: center;
            font-family: Arial, sans-serif;
            font-size: 8px;
            page-break-inside: avoid;
          ">
            <img src="${barcodeImage}" style="width: ${selectedSize.width * 0.5}px; height: ${selectedSize.height * 0.5}px;" />
            <div style="font-weight: bold; margin-top: 2px;">${sample.barcode}</div>
            ${includeExternalId && sample.externalId ? `<div>${sample.externalId}</div>` : ""}
            ${includeDate && sample.receiptDate ? `<div>${new Date(sample.receiptDate).toLocaleDateString()}</div>` : ""}
          </div>
        `;
      }
    });

    printWindow.document.write(`
      <!DOCTYPE html>
      <html>
        <head>
          <title>Sample Labels</title>
          <style>
            @media print {
              body { margin: 0; }
              .label { page-break-inside: avoid; }
            }
          </style>
        </head>
        <body>
          ${labelsHtml}
          <script>
            window.onload = function() {
              window.print();
              window.close();
            };
          </script>
        </body>
      </html>
    `);
    printWindow.document.close();
  }, [
    samples,
    selectedRows,
    labelSize,
    labelSizes,
    generatedBarcodes,
    includeExternalId,
    includeDate,
    intl,
  ]);

  const tableHeaders = [
    { key: "barcode", header: "Barcode" },
    { key: "externalId", header: "External ID" },
    { key: "status", header: "Status" },
    { key: "preview", header: "Preview" },
  ];

  const tableRows = samples.map((sample) => ({
    id: String(sample.id),
    barcode: sample.barcode,
    externalId: sample.externalId || "-",
    status: sample.documentationStatus || sample.status,
    preview: generatedBarcodes[sample.id] ? "generated" : "pending",
  }));

  return (
    <div className="barcode-generator">
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "biorepository.barcode.error.title",
            defaultMessage: "Error",
          })}
          subtitle={error}
          lowContrast
          onClose={() => setError(null)}
        />
      )}

      <Grid>
        <Column lg={8} md={4} sm={4}>
          <Dropdown
            id="labelSize"
            titleText={intl.formatMessage({
              id: "biorepository.barcode.field.labelSize",
              defaultMessage: "Label Size",
            })}
            items={labelSizes}
            itemToString={(item) => (item ? item.text : "")}
            selectedItem={labelSizes.find((s) => s.id === labelSize)}
            onChange={({ selectedItem }) =>
              setLabelSize(selectedItem?.id || "standard")
            }
          />
        </Column>

        <Column lg={8} md={4} sm={4}>
          <div style={{ marginTop: "1.5rem" }}>
            <Checkbox
              id="includeExternalId"
              labelText={intl.formatMessage({
                id: "biorepository.barcode.option.includeExternalId",
                defaultMessage: "Include External ID on label",
              })}
              checked={includeExternalId}
              onChange={(e, { checked }) => setIncludeExternalId(checked)}
            />
            <Checkbox
              id="includeDate"
              labelText={intl.formatMessage({
                id: "biorepository.barcode.option.includeDate",
                defaultMessage: "Include receipt date on label",
              })}
              checked={includeDate}
              onChange={(e, { checked }) => setIncludeDate(checked)}
            />
          </div>
        </Column>

        <Column lg={16} md={8} sm={4}>
          <div
            style={{
              marginTop: "1rem",
              marginBottom: "1rem",
              display: "flex",
              gap: "1rem",
            }}
          >
            <Button
              onClick={handleGenerateBarcodes}
              renderIcon={Renew}
              disabled={samples.length === 0}
            >
              <FormattedMessage
                id="biorepository.barcode.button.generate"
                defaultMessage="Generate Barcodes"
              />
            </Button>
            <Button
              onClick={handlePrint}
              renderIcon={Printer}
              kind="secondary"
              disabled={Object.keys(generatedBarcodes).length === 0}
            >
              <FormattedMessage
                id="biorepository.barcode.button.print"
                defaultMessage="Print Labels"
              />
            </Button>
          </div>
        </Column>

        <Column lg={16} md={8} sm={4}>
          {samples.length === 0 ? (
            <InlineNotification
              kind="info"
              title={intl.formatMessage({
                id: "biorepository.barcode.noSamples.title",
                defaultMessage: "No Samples",
              })}
              subtitle={intl.formatMessage({
                id: "biorepository.barcode.noSamples.message",
                defaultMessage: "Register samples first to generate barcodes.",
              })}
              lowContrast
              hideCloseButton
            />
          ) : (
            <DataTable
              rows={tableRows}
              headers={tableHeaders}
              size="sm"
              isSortable
            >
              {({
                rows,
                headers,
                getTableProps,
                getHeaderProps,
                getRowProps,
                getSelectionProps,
                selectedRows: dtSelectedRows,
              }) => (
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      <TableSelectAll {...getSelectionProps()} />
                      {headers.map((header) => (
                        <TableHeader
                          key={header.key}
                          {...getHeaderProps({ header })}
                        >
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {rows.map((row) => (
                      <TableRow key={row.id} {...getRowProps({ row })}>
                        <TableSelectRow
                          {...getSelectionProps({ row })}
                          onChange={() => {
                            const newSelected = selectedRows.includes(row.id)
                              ? selectedRows.filter((id) => id !== row.id)
                              : [...selectedRows, row.id];
                            setSelectedRows(newSelected);
                          }}
                          checked={selectedRows.includes(row.id)}
                        />
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>
                            {cell.info.header === "preview" ? (
                              generatedBarcodes[row.id] ? (
                                <img
                                  src={generatedBarcodes[row.id]}
                                  alt="Barcode"
                                  style={{ width: "48px", height: "48px" }}
                                />
                              ) : (
                                <Tag size="sm">
                                  <FormattedMessage
                                    id="biorepository.barcode.status.pending"
                                    defaultMessage="Pending"
                                  />
                                </Tag>
                              )
                            ) : cell.info.header === "status" ? (
                              <Tag
                                size="sm"
                                type={
                                  cell.value === "VERIFIED"
                                    ? "green"
                                    : cell.value === "QUARANTINE"
                                      ? "red"
                                      : "gray"
                                }
                              >
                                {cell.value}
                              </Tag>
                            ) : (
                              cell.value
                            )}
                          </TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </DataTable>
          )}
        </Column>

        {onComplete && (
          <Column lg={16} md={8} sm={4}>
            <div style={{ marginTop: "1rem" }}>
              <Button onClick={onComplete}>
                <FormattedMessage
                  id="biorepository.barcode.button.complete"
                  defaultMessage="Complete Intake"
                />
              </Button>
            </div>
          </Column>
        )}
      </Grid>

      {/* Hidden print area */}
      <div ref={printRef} style={{ display: "none" }} />
    </div>
  );
}

BarcodeGenerator.propTypes = {
  samples: PropTypes.array,
  shipmentId: PropTypes.number,
  onComplete: PropTypes.func,
};

export default BarcodeGenerator;

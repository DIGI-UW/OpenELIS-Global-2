import React, { useState, useCallback, useRef, useEffect } from "react";
import {
  Modal,
  FileUploaderDropContainer,
  FileUploaderItem,
  Select,
  SelectItem,
  InlineNotification,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Loading,
  Tag,
  TextInput,
  Grid,
  Column,
} from "@carbon/react";
import { Checkmark } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../../config.json";
import "../workflow/NotebookWorkflow.css";

/**
 * MedLabManifestImportModal - Modal for importing samples from CSV manifest files.
 * Uses the 13-field MedLab manifest specification per spec FR-010 to FR-014.
 *
 * Required fields (9): sampleId, sampleType, containerType, quantity,
 *   unitOfMeasure, collectionSource, collector, collectionDate, collectionTime
 * Optional fields (4): customLabel, orderId, patientId, notes
 *
 * @param {Object} props
 * @param {boolean} props.open - Whether the modal is open
 * @param {function} props.onClose - Callback when modal is closed
 * @param {number} props.entryId - The notebook entry ID to import samples to
 * @param {Object} props.pageData - The notebook page data (for page-sample linking)
 * @param {function} props.onImportSuccess - Callback when import is successful
 */
function MedLabManifestImportModal({
  open,
  onClose,
  entryId,
  pageData,
  onImportSuccess,
}) {
  const intl = useIntl();
  const componentMounted = useRef(true);

  // Track component mount state
  useEffect(() => {
    componentMounted.current = true;
    return () => {
      componentMounted.current = false;
    };
  }, []);

  // State for file upload
  const [file, setFile] = useState(null);
  const [fileError, setFileError] = useState(null);

  // State for column mapping - 13 fields per MedLab spec
  const [csvHeaders, setCsvHeaders] = useState([]);
  const [columnMapping, setColumnMapping] = useState({
    // Required fields (9)
    sampleIdColumn: "",
    sampleTypeColumn: "",
    containerTypeColumn: "",
    quantityColumn: "",
    unitOfMeasureColumn: "",
    collectionSourceColumn: "",
    collectorColumn: "",
    collectionDateColumn: "",
    collectionTimeColumn: "",
    // Optional fields (4)
    customLabelColumn: "",
    orderIdColumn: "",
    patientIdColumn: "",
    notesColumn: "",
    // Format settings
    dateFormat: "yyyy-MM-dd",
    timeFormat: "HH:mm",
  });

  // State for preview
  const [previewData, setPreviewData] = useState(null);
  const [previewErrors, setPreviewErrors] = useState([]);
  const [isPreviewLoading, setIsPreviewLoading] = useState(false);
  const [isImporting, setIsImporting] = useState(false);

  // Store all valid rows for import (from preview response)
  const [validRowsForImport, setValidRowsForImport] = useState([]);

  // Steps: 1 = Upload, 2 = Map Columns, 3 = Preview, 4 = Success
  const [step, setStep] = useState(1);

  const handleFileAdded = useCallback(
    (event, { addedFiles }) => {
      const addedFile = addedFiles[0];
      if (!addedFile) return;

      // Validate file type
      if (!addedFile.name.endsWith(".csv")) {
        setFileError(
          intl.formatMessage({
            id: "notebook.manifest.error.invalidFileType",
            defaultMessage: "Please upload a CSV file",
          }),
        );
        return;
      }

      setFile(addedFile);
      setFileError(null);

      // Parse headers from file
      const reader = new FileReader();
      reader.onload = (e) => {
        if (!componentMounted.current) return;
        const text = e.target.result;
        const firstLine = text.split("\n")[0];
        const headers = firstLine
          .split(",")
          .map((h) => h.trim().replace(/"/g, ""));
        setCsvHeaders(headers);

        // Auto-map columns based on header names
        const autoMapping = autoMapColumns(headers);
        setColumnMapping((prev) => ({ ...prev, ...autoMapping }));

        setStep(2);
      };
      reader.readAsText(addedFile);
    },
    [intl],
  );

  /**
   * Auto-map CSV columns to form fields based on header names.
   * Supports various naming conventions (snake_case, camelCase, spaces).
   */
  const autoMapColumns = (headers) => {
    const mapping = {};
    const headerLower = headers.map((h) =>
      h.toLowerCase().replace(/[_\s-]/g, ""),
    );

    // Define field mappings with possible CSV header variations
    const fieldMappings = {
      sampleIdColumn: ["sampleid", "sample_id", "id", "specimenid"],
      sampleTypeColumn: [
        "sampletype",
        "sample_type",
        "type",
        "specimentype",
        "sampletypeid",
      ],
      containerTypeColumn: [
        "containertype",
        "container_type",
        "container",
        "tubetype",
      ],
      quantityColumn: ["quantity", "qty", "amount", "volume"],
      unitOfMeasureColumn: ["unitofmeasure", "unit_of_measure", "unit", "uom"],
      collectionSourceColumn: [
        "collectionsource",
        "collection_source",
        "source",
        "location",
      ],
      collectorColumn: [
        "collector",
        "collectedby",
        "collected_by",
        "technician",
      ],
      collectionDateColumn: [
        "collectiondate",
        "collection_date",
        "date",
        "collectdate",
      ],
      collectionTimeColumn: [
        "collectiontime",
        "collection_time",
        "time",
        "collecttime",
      ],
      customLabelColumn: ["customlabel", "custom_label", "label", "barcode"],
      orderIdColumn: ["orderid", "order_id", "order", "laborder"],
      patientIdColumn: ["patientid", "patient_id", "patient", "subjectid"],
      notesColumn: ["notes", "note", "comments", "remarks"],
    };

    // Match headers to fields
    for (const [field, variations] of Object.entries(fieldMappings)) {
      for (let i = 0; i < headerLower.length; i++) {
        if (variations.includes(headerLower[i])) {
          mapping[field] = headers[i]; // Use original header name
          break;
        }
      }
    }

    return mapping;
  };

  const handleRemoveFile = () => {
    setFile(null);
    setCsvHeaders([]);
    setColumnMapping({
      sampleIdColumn: "",
      sampleTypeColumn: "",
      containerTypeColumn: "",
      quantityColumn: "",
      unitOfMeasureColumn: "",
      collectionSourceColumn: "",
      collectorColumn: "",
      collectionDateColumn: "",
      collectionTimeColumn: "",
      customLabelColumn: "",
      orderIdColumn: "",
      patientIdColumn: "",
      notesColumn: "",
      dateFormat: "yyyy-MM-dd",
      timeFormat: "HH:mm",
    });
    setPreviewData(null);
    setPreviewErrors([]);
    setStep(1);
  };

  const handleMappingChange = (field, value) => {
    setColumnMapping((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handlePreview = async () => {
    if (!file) {
      console.error("MedLabManifestImportModal: No file selected");
      return;
    }

    if (!entryId) {
      console.error("MedLabManifestImportModal: No entryId provided");
      setPreviewErrors([
        {
          rowNumber: 0,
          column: "system",
          message: "Entry ID is not available. Please try again.",
        },
      ]);
      return;
    }

    setIsPreviewLoading(true);
    setPreviewErrors([]);

    const formData = new FormData();
    formData.append("file", file);
    // Add each column mapping as individual form params
    Object.entries(columnMapping).forEach(([key, value]) => {
      if (value) {
        formData.append(key, value);
      }
    });

    const endpoint = `${config.serverBaseUrl}/rest/medlab/samples/preview-manifest`;

    try {
      console.log(
        "MedLabManifestImportModal: Sending preview request to " + endpoint,
      );
      const response = await fetch(endpoint, {
        method: "POST",
        body: formData,
        credentials: "include",
        headers: {
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
      });

      console.log(
        "MedLabManifestImportModal: Response status:",
        response.status,
      );
      const data = await response.json();
      console.log("MedLabManifestImportModal: Response data:", data);

      if (response.ok) {
        setPreviewData(data);
        setPreviewErrors(data.errors || []);
        // Store all valid rows for import (not just the preview subset)
        setValidRowsForImport(data.allValidRows || []);
        setStep(3);
      } else {
        setPreviewErrors([
          {
            rowNumber: 0,
            column: "file",
            message: data.error || data.message || "Failed to preview manifest",
          },
        ]);
      }
    } catch (error) {
      console.error("MedLabManifestImportModal: Preview error:", error);
      setPreviewErrors([
        { rowNumber: 0, column: "file", message: error.message },
      ]);
    } finally {
      setIsPreviewLoading(false);
    }
  };

  const handleImport = async () => {
    if (!entryId || validRowsForImport.length === 0) {
      console.error("MedLabManifestImportModal: No valid rows to import");
      setPreviewErrors([
        {
          rowNumber: 0,
          column: "import",
          message: "No valid rows available for import",
        },
      ]);
      return;
    }

    setIsImporting(true);

    const endpoint = `${config.serverBaseUrl}/rest/medlab/samples/import-validated`;

    // Prepare JSON payload with validated rows
    const importPayload = {
      entryId: entryId,
      pageId: pageData?.id || null,
      validRows: validRowsForImport,
      // selectedTests: [], // TODO: Add test selection if needed
    };

    // Log payload being sent
    console.log("=== MedLab Manifest Import (Validated Rows) ===");
    console.log("Endpoint:", endpoint);
    console.log("Entry ID:", entryId);
    console.log("Valid rows count:", validRowsForImport.length);
    console.log("First 3 rows:", validRowsForImport.slice(0, 3));
    console.log("Full payload:", JSON.stringify(importPayload, null, 2));
    console.log("================================================");

    try {
      const response = await fetch(endpoint, {
        method: "POST",
        body: JSON.stringify(importPayload),
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
      });

      const data = await response.json();

      console.log("=== MedLab Manifest Import Response ===");
      console.log("Status:", response.status, response.statusText);
      console.log("Response data:", JSON.stringify(data, null, 2));
      console.log("========================================");

      if (response.ok && data.success) {
        setPreviewData(data); // Store result for success display
        setStep(4); // Go to success step
        if (onImportSuccess) {
          onImportSuccess(data);
        }
      } else {
        console.error("Import failed:", data);
        setPreviewErrors(
          data.errors || [
            { rowNumber: 0, column: "import", message: data.error },
          ],
        );
      }
    } catch (error) {
      console.error("Import exception:", error);
      setPreviewErrors([
        { rowNumber: 0, column: "import", message: error.message },
      ]);
    } finally {
      setIsImporting(false);
    }
  };

  const handleClose = () => {
    handleRemoveFile();
    onClose();
  };

  // Check if all required columns are mapped
  const requiredColumnsMapped =
    columnMapping.sampleIdColumn &&
    columnMapping.sampleTypeColumn &&
    columnMapping.containerTypeColumn &&
    columnMapping.quantityColumn &&
    columnMapping.unitOfMeasureColumn &&
    columnMapping.collectionSourceColumn &&
    columnMapping.collectorColumn &&
    columnMapping.collectionDateColumn &&
    columnMapping.collectionTimeColumn;

  const renderColumnMappingSelect = (field, labelId, required = false) => (
    <div className={`mapping-field ${required ? "required" : ""}`}>
      <Select
        id={`mapping-${field}`}
        labelText={
          <>
            <FormattedMessage
              id={labelId}
              defaultMessage={field.replace("Column", "")}
            />
            {required && <span className="required-marker">*</span>}
          </>
        }
        value={columnMapping[field]}
        onChange={(e) => handleMappingChange(field, e.target.value)}
        size="sm"
      >
        <SelectItem
          value=""
          text={intl.formatMessage({ id: "label.select" })}
        />
        {csvHeaders.map((header) => (
          <SelectItem key={header} value={header} text={header} />
        ))}
      </Select>
    </div>
  );

  return (
    <Modal
      open={open}
      onRequestClose={handleClose}
      modalHeading={intl.formatMessage({
        id: "medlab.manifest.title",
        defaultMessage: "Import Sample Manifest",
      })}
      primaryButtonText={
        step === 1
          ? intl.formatMessage({ id: "label.button.next" })
          : step === 2
            ? intl.formatMessage({ id: "medlab.manifest.preview" })
            : step === 3
              ? intl.formatMessage({ id: "medlab.manifest.import" })
              : intl.formatMessage({ id: "label.button.close" })
      }
      secondaryButtonText={
        step > 1 && step < 4
          ? intl.formatMessage({ id: "label.button.back" })
          : null
      }
      onRequestSubmit={() => {
        if (step === 1 && file) setStep(2);
        else if (step === 2) handlePreview();
        else if (step === 3) handleImport();
        else handleClose();
      }}
      onSecondarySubmit={() => setStep(step - 1)}
      primaryButtonDisabled={
        (step === 1 && !file) ||
        (step === 2 && !requiredColumnsMapped) ||
        (step === 3 && (!previewData || previewData.validRows === 0)) ||
        isPreviewLoading ||
        isImporting
      }
      size="lg"
    >
      <div className="manifest-import-modal">
        {/* Step 1: File Upload */}
        {step === 1 && (
          <div className="upload-step">
            <p className="step-description">
              <FormattedMessage
                id="medlab.manifest.description"
                defaultMessage="Import samples from a CSV manifest file. Map the columns from your file to the required fields."
              />
            </p>

            {!file ? (
              <FileUploaderDropContainer
                accept={[".csv"]}
                labelText={intl.formatMessage({
                  id: "notebook.manifest.dropzone",
                  defaultMessage:
                    "Drag and drop a CSV file here or click to upload",
                })}
                onAddFiles={handleFileAdded}
              />
            ) : (
              <FileUploaderItem
                name={file.name}
                status="edit"
                onDelete={handleRemoveFile}
              />
            )}

            {fileError && (
              <InlineNotification
                kind="error"
                title={fileError}
                hideCloseButton
                lowContrast
              />
            )}
          </div>
        )}

        {/* Step 2: Column Mapping - 13 fields */}
        {step === 2 && (
          <div className="mapping-step">
            <p className="step-description">
              <FormattedMessage
                id="medlab.manifest.columnMapping"
                defaultMessage="Map the columns from your CSV file to the required fields. Required fields are marked with *."
              />
            </p>

            <h5>
              <FormattedMessage
                id="medlab.manifest.requiredFields"
                defaultMessage="Required Fields"
              />
            </h5>
            <Grid narrow>
              <Column lg={4} md={4} sm={4}>
                {renderColumnMappingSelect(
                  "sampleIdColumn",
                  "medlab.manifest.field.sampleId",
                  true,
                )}
              </Column>
              <Column lg={4} md={4} sm={4}>
                {renderColumnMappingSelect(
                  "sampleTypeColumn",
                  "medlab.manifest.field.sampleType",
                  true,
                )}
              </Column>
              <Column lg={4} md={4} sm={4}>
                {renderColumnMappingSelect(
                  "containerTypeColumn",
                  "medlab.manifest.field.containerType",
                  true,
                )}
              </Column>
              <Column lg={4} md={4} sm={4}>
                {renderColumnMappingSelect(
                  "quantityColumn",
                  "medlab.manifest.field.quantity",
                  true,
                )}
              </Column>
              <Column lg={4} md={4} sm={4}>
                {renderColumnMappingSelect(
                  "unitOfMeasureColumn",
                  "medlab.manifest.field.unitOfMeasure",
                  true,
                )}
              </Column>
              <Column lg={4} md={4} sm={4}>
                {renderColumnMappingSelect(
                  "collectionSourceColumn",
                  "medlab.manifest.field.collectionSource",
                  true,
                )}
              </Column>
              <Column lg={4} md={4} sm={4}>
                {renderColumnMappingSelect(
                  "collectorColumn",
                  "medlab.manifest.field.collector",
                  true,
                )}
              </Column>
              <Column lg={4} md={4} sm={4}>
                {renderColumnMappingSelect(
                  "collectionDateColumn",
                  "medlab.manifest.field.collectionDate",
                  true,
                )}
              </Column>
              <Column lg={4} md={4} sm={4}>
                {renderColumnMappingSelect(
                  "collectionTimeColumn",
                  "medlab.manifest.field.collectionTime",
                  true,
                )}
              </Column>
            </Grid>

            <h5 style={{ marginTop: "1rem" }}>
              <FormattedMessage
                id="medlab.manifest.optionalFields"
                defaultMessage="Optional Fields"
              />
            </h5>
            <Grid narrow>
              <Column lg={4} md={4} sm={4}>
                {renderColumnMappingSelect(
                  "customLabelColumn",
                  "medlab.manifest.field.customLabel",
                  false,
                )}
              </Column>
              <Column lg={4} md={4} sm={4}>
                {renderColumnMappingSelect(
                  "orderIdColumn",
                  "medlab.manifest.field.orderId",
                  false,
                )}
              </Column>
              <Column lg={4} md={4} sm={4}>
                {renderColumnMappingSelect(
                  "patientIdColumn",
                  "medlab.manifest.field.patientId",
                  false,
                )}
              </Column>
              <Column lg={4} md={4} sm={4}>
                {renderColumnMappingSelect(
                  "notesColumn",
                  "medlab.manifest.field.notes",
                  false,
                )}
              </Column>
            </Grid>

            <h5 style={{ marginTop: "1rem" }}>
              <FormattedMessage
                id="medlab.manifest.formatSettings"
                defaultMessage="Format Settings"
              />
            </h5>
            <Grid narrow>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="dateFormat"
                  labelText={intl.formatMessage({
                    id: "medlab.manifest.field.dateFormat",
                  })}
                  value={columnMapping.dateFormat}
                  onChange={(e) =>
                    handleMappingChange("dateFormat", e.target.value)
                  }
                  size="sm"
                  placeholder="yyyy-MM-dd"
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="timeFormat"
                  labelText={intl.formatMessage({
                    id: "medlab.manifest.field.timeFormat",
                  })}
                  value={columnMapping.timeFormat}
                  onChange={(e) =>
                    handleMappingChange("timeFormat", e.target.value)
                  }
                  size="sm"
                  placeholder="HH:mm"
                />
              </Column>
            </Grid>

            {isPreviewLoading && (
              <Loading
                withOverlay={false}
                description={intl.formatMessage({
                  id: "notebook.manifest.loading",
                  defaultMessage: "Loading...",
                })}
              />
            )}

            {/* Show errors that occur during preview fetch */}
            {previewErrors.length > 0 && (
              <InlineNotification
                kind="error"
                title={intl.formatMessage({
                  id: "medlab.manifest.errors",
                  defaultMessage: "Errors",
                })}
                subtitle={
                  <ul className="error-list">
                    {previewErrors.map((error, idx) => (
                      <li key={idx}>
                        {error.rowNumber > 0 ? `Row ${error.rowNumber}: ` : ""}
                        {error.message}
                      </li>
                    ))}
                  </ul>
                }
                hideCloseButton
                lowContrast
              />
            )}
          </div>
        )}

        {/* Step 3: Preview */}
        {step === 3 && (
          <div className="preview-step">
            {previewData && (
              <>
                {/* Summary */}
                <div
                  className="preview-summary"
                  style={{ marginBottom: "1rem" }}
                >
                  <Tag type="blue">
                    <FormattedMessage
                      id="medlab.manifest.totalRows"
                      defaultMessage="Total Rows"
                    />
                    : {previewData.totalRows}
                  </Tag>
                  <Tag type="green">
                    <FormattedMessage
                      id="medlab.manifest.validRows"
                      defaultMessage="Valid Rows"
                    />
                    : {previewData.validRows}
                  </Tag>
                  {previewData.invalidRows > 0 && (
                    <Tag type="red">
                      <FormattedMessage
                        id="medlab.manifest.invalidRows"
                        defaultMessage="Invalid Rows"
                      />
                      : {previewData.invalidRows}
                    </Tag>
                  )}
                </div>

                {/* Invalid Rows Section */}
                {previewData.invalidPreviewRows &&
                  previewData.invalidPreviewRows.length > 0 && (
                    <div
                      className="invalid-rows-section"
                      style={{ marginBottom: "1.5rem" }}
                    >
                      <h5 style={{ color: "#da1e28", marginBottom: "0.5rem" }}>
                        <FormattedMessage
                          id="medlab.manifest.invalidRowsTitle"
                          defaultMessage="Invalid Rows (will not be imported)"
                        />
                      </h5>
                      <InlineNotification
                        kind="error"
                        title={intl.formatMessage({
                          id: "medlab.manifest.invalidRowsInfo",
                          defaultMessage:
                            "The following rows have errors and will be skipped",
                        })}
                        subtitle={intl.formatMessage({
                          id: "medlab.manifest.fixErrors",
                          defaultMessage:
                            "Fix the errors in your CSV and re-upload, or proceed to import only valid rows.",
                        })}
                        hideCloseButton
                        lowContrast
                        style={{ marginBottom: "0.5rem" }}
                      />
                      <DataTable
                        rows={previewData.invalidPreviewRows.map(
                          (row, idx) => ({
                            id: `invalid-${idx}`,
                            ...row,
                            errorMessages: row.errors
                              ? row.errors.join("; ")
                              : "",
                          }),
                        )}
                        headers={[
                          {
                            key: "rowNumber",
                            header: intl.formatMessage({
                              id: "medlab.manifest.field.rowNumber",
                              defaultMessage: "Row",
                            }),
                          },
                          {
                            key: "sampleId",
                            header: intl.formatMessage({
                              id: "medlab.manifest.field.sampleId",
                            }),
                          },
                          {
                            key: "sampleType",
                            header: intl.formatMessage({
                              id: "medlab.manifest.field.sampleType",
                            }),
                          },
                          {
                            key: "errorMessages",
                            header: intl.formatMessage({
                              id: "medlab.manifest.field.errors",
                              defaultMessage: "Errors",
                            }),
                          },
                        ]}
                        size="sm"
                      >
                        {({
                          rows,
                          headers,
                          getTableProps,
                          getHeaderProps,
                          getRowProps,
                        }) => (
                          <Table {...getTableProps()}>
                            <TableHead>
                              <TableRow>
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
                                <TableRow
                                  key={row.id}
                                  {...getRowProps({ row })}
                                  style={{ backgroundColor: "#fff1f1" }}
                                >
                                  {row.cells.map((cell) => (
                                    <TableCell
                                      key={cell.id}
                                      style={
                                        cell.info.header === "errorMessages"
                                          ? {
                                              color: "#da1e28",
                                              fontSize: "0.85rem",
                                            }
                                          : {}
                                      }
                                    >
                                      {cell.value || "-"}
                                    </TableCell>
                                  ))}
                                </TableRow>
                              ))}
                            </TableBody>
                          </Table>
                        )}
                      </DataTable>
                    </div>
                  )}

                {/* Valid Rows Section */}
                {previewData.previewRows &&
                  previewData.previewRows.length > 0 && (
                    <div className="valid-rows-section">
                      <h5 style={{ color: "#198038", marginBottom: "0.5rem" }}>
                        <FormattedMessage
                          id="medlab.manifest.validRowsTitle"
                          defaultMessage="Valid Rows (will be imported)"
                        />
                      </h5>
                      <DataTable
                        rows={previewData.previewRows.map((row, idx) => ({
                          id: `valid-${idx}`,
                          ...row,
                        }))}
                        headers={[
                          {
                            key: "sampleId",
                            header: intl.formatMessage({
                              id: "medlab.manifest.field.sampleId",
                            }),
                          },
                          {
                            key: "sampleType",
                            header: intl.formatMessage({
                              id: "medlab.manifest.field.sampleType",
                            }),
                          },
                          {
                            key: "containerType",
                            header: intl.formatMessage({
                              id: "medlab.manifest.field.containerType",
                            }),
                          },
                          {
                            key: "quantity",
                            header: intl.formatMessage({
                              id: "medlab.manifest.field.quantity",
                            }),
                          },
                          {
                            key: "patientId",
                            header: intl.formatMessage({
                              id: "medlab.manifest.field.patientId",
                            }),
                          },
                          {
                            key: "orderId",
                            header: intl.formatMessage({
                              id: "medlab.manifest.field.orderId",
                            }),
                          },
                        ]}
                        size="sm"
                      >
                        {({
                          rows,
                          headers,
                          getTableProps,
                          getHeaderProps,
                          getRowProps,
                        }) => (
                          <Table {...getTableProps()}>
                            <TableHead>
                              <TableRow>
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
                                <TableRow
                                  key={row.id}
                                  {...getRowProps({ row })}
                                >
                                  {row.cells.map((cell) => (
                                    <TableCell key={cell.id}>
                                      {cell.value || "-"}
                                    </TableCell>
                                  ))}
                                </TableRow>
                              ))}
                            </TableBody>
                          </Table>
                        )}
                      </DataTable>
                    </div>
                  )}

                {/* No valid rows message */}
                {previewData.validRows === 0 && (
                  <InlineNotification
                    kind="warning"
                    title={intl.formatMessage({
                      id: "medlab.manifest.noValidRows",
                      defaultMessage: "No valid rows to import",
                    })}
                    subtitle={intl.formatMessage({
                      id: "medlab.manifest.fixAllErrors",
                      defaultMessage:
                        "Please fix all errors in your CSV file and try again.",
                    })}
                    hideCloseButton
                    lowContrast
                  />
                )}
              </>
            )}

            {isImporting && (
              <Loading
                withOverlay={false}
                description={intl.formatMessage({
                  id: "notebook.manifest.importing",
                  defaultMessage: "Creating samples...",
                })}
              />
            )}
          </div>
        )}

        {/* Step 4: Success */}
        {step === 4 && (
          <div className="success-step">
            <Checkmark size={48} className="success-icon" />
            <h3>
              <FormattedMessage
                id="notebook.manifest.success.title"
                defaultMessage="Import Successful"
              />
            </h3>
            <p>
              <FormattedMessage
                id="medlab.manifest.success"
                defaultMessage="{count} sample(s) imported successfully"
                values={{ count: previewData?.samplesCreated || 0 }}
              />
            </p>
            {previewData?.analysesCreated > 0 && (
              <p>
                <FormattedMessage
                  id="medlab.manifest.analysesCreated"
                  defaultMessage="{count} analysis record(s) created"
                  values={{ count: previewData.analysesCreated }}
                />
              </p>
            )}
            {previewData?.skippedRows > 0 && (
              <p style={{ color: "#da1e28" }}>
                <FormattedMessage
                  id="medlab.manifest.skippedRows"
                  defaultMessage="{count} row(s) were skipped due to validation errors"
                  values={{ count: previewData.skippedRows }}
                />
              </p>
            )}
          </div>
        )}
      </div>
    </Modal>
  );
}

export default MedLabManifestImportModal;

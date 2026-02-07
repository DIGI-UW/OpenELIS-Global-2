import React, { useState, useCallback } from "react";
import {
  Modal,
  FileUploader,
  Button,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  InlineNotification,
  Loading,
  Tag,
  Link,
} from "@carbon/react";
import {
  DocumentImport,
  Checkmark,
  Warning,
  Download,
} from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";
import { postToOpenElisServerJsonResponse } from "../../../utils/Utils";

/**
 * ManifestUploadModal - CSV manifest upload modal for bulk sample import
 * Part of Sub-stage 1b of the Biorepository Intake workflow
 *
 * Expected CSV format:
 * externalId,sampleType,projectId,collectionDate,biosafetyLevel,originLab,consentId,ethicsApprovalRef,mtaReference
 *
 * @param {Object} props
 * @param {boolean} props.open - Whether the modal is open
 * @param {Function} props.onClose - Callback to close the modal
 * @param {number} props.shipmentId - The shipment ID to associate samples with
 * @param {Function} props.onImportComplete - Callback when import is complete
 */
function ManifestUploadModal({ open, onClose, shipmentId, onImportComplete }) {
  const intl = useIntl();

  const [file, setFile] = useState(null);
  const [parsedData, setParsedData] = useState([]);
  const [validationErrors, setValidationErrors] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [importStatus, setImportStatus] = useState(null); // null, 'parsed', 'validating', 'preview', 'importing', 'complete'
  const [backendValidationDone, setBackendValidationDone] = useState(false);

  // ============================================================
  // FIELD DEFINITIONS - Must match backend SampleRegistrationDTO
  // ============================================================
  // Backend DTO: src/main/java/org/openelisglobal/biorepository/controller/rest/dto/SampleRegistrationDTO.java

  // Required fields per spec FR-MAN-003
  const requiredFields = [
    "externalId",
    "sampleType",
    "biosafetyLevel",
    "collectionDate",
    "originLab",
  ];

  // Conditional fields (required based on sample type/source)
  const conditionalFields = [
    { name: "consentId", description: "Required for human samples" },
    { name: "ethicsApprovalRef", description: "Required for human samples" },
    { name: "mtaReference", description: "Required for external samples" },
  ];

  // Optional fields
  const optionalFields = [
    "projectId",
    "principalInvestigator",
    "preservationMedium",
    "arrivalCondition",
    "notes",
  ];

  // All fields supported by the backend DTO (used for validation)
  // This MUST match SampleRegistrationDTO.java fields exactly
  const supportedBackendFields = new Set([
    // Required
    "externalId",
    "sampleTypeId", // Maps from CSV "sampleType"
    "biosafetyLevel",
    "collectionDate",
    "originLab",
    // Conditional
    "consentId",
    "ethicsApprovalRef",
    "mtaReference",
    // Optional
    "projectId",
    "principalInvestigator",
    "preservationMedium",
    "arrivalCondition",
    "specialHandling", // Maps from CSV "notes"
    // System fields (set by backend, not from CSV)
    "barcode",
    "receiptDate",
    "requiredTempMin",
    "requiredTempMax",
    "shipmentId",
  ]);

  // CSV column to backend field mapping
  const csvToBackendFieldMap = {
    externalId: "externalId",
    sampleType: "sampleTypeId",
    projectId: "projectId",
    collectionDate: "collectionDate",
    biosafetyLevel: "biosafetyLevel",
    originLab: "originLab",
    principalInvestigator: "principalInvestigator",
    consentId: "consentId",
    ethicsApprovalRef: "ethicsApprovalRef",
    mtaReference: "mtaReference",
    preservationMedium: "preservationMedium",
    arrivalCondition: "arrivalCondition",
    notes: "specialHandling",
  };

  // Supported sample types per spec (Sample Type Classifications)
  const supportedSampleTypes = [
    {
      category: "Blood-derived",
      types: ["Serum", "Plasma", "Whole Blood", "Buffy Coat"],
    },
    { category: "Nucleic Acids", types: ["DNA", "RNA", "cDNA"] },
    { category: "Tissue", types: ["Biopsy", "FFPE Block", "Fresh Frozen"] },
    { category: "Cellular", types: ["Cell Line", "PBMCs", "Primary Cells"] },
    {
      category: "Microbiological",
      types: ["Bacterial Isolate", "Viral Culture", "Fungal Isolate"],
    },
    {
      category: "Other",
      types: [
        "Urine",
        "CSF",
        "Saliva",
        "Stool",
        "Respiratory Swab",
        "Fluid",
        "Environmental",
      ],
    },
  ];

  // All expected CSV headers (for template generation)
  const expectedHeaders = Object.keys(csvToBackendFieldMap);

  /**
   * Generate and download CSV template per spec FR-MAN-002
   */
  const downloadTemplate = useCallback(() => {
    const headers = expectedHeaders.join(",");
    // Example row matching the header order
    const exampleRow =
      "SAMPLE-001,Serum,PROJ-123,2026-01-09,BSL_2,AHRI Lab,Dr. Smith,CONSENT-001,ETH-2025-001,MTA-001,EDTA,Good,Sample notes";
    const csvContent = `${headers}\n${exampleRow}`;

    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const link = document.createElement("a");
    const url = URL.createObjectURL(blob);
    link.setAttribute("href", url);
    link.setAttribute("download", "biorepository_manifest_template.csv");
    link.style.visibility = "hidden";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }, [expectedHeaders]);

  const parseCSV = useCallback(
    (csvText) => {
      const lines = csvText.split(/\r?\n/).filter((line) => line.trim());
      if (lines.length < 2) {
        throw new Error(
          intl.formatMessage({
            id: "biorepository.manifest.error.emptyFile",
            defaultMessage:
              "CSV must contain at least a header row and one data row",
          }),
        );
      }

      const headers = lines[0].split(",").map((h) => h.trim());

      // Validate required headers per spec FR-MAN-003
      const missingHeaders = requiredFields.filter(
        (field) => !headers.includes(field),
      );
      if (missingHeaders.length > 0) {
        throw new Error(
          intl.formatMessage(
            {
              id: "biorepository.manifest.error.missingHeaders",
              defaultMessage:
                "Missing required columns: {fields}. Please download the template.",
            },
            { fields: missingHeaders.join(", ") },
          ),
        );
      }

      // Validate that all CSV columns are supported (prevents backend DTO mismatch)
      const supportedCsvColumns = new Set(Object.keys(csvToBackendFieldMap));
      const unknownColumns = headers.filter(
        (header) => !supportedCsvColumns.has(header),
      );
      if (unknownColumns.length > 0) {
        throw new Error(
          intl.formatMessage(
            {
              id: "biorepository.manifest.error.unknownColumns",
              defaultMessage:
                "Unknown columns in CSV: {columns}. These fields are not supported. Please use the template.",
            },
            { columns: unknownColumns.join(", ") },
          ),
        );
      }

      const data = [];
      const errors = [];

      for (let i = 1; i < lines.length; i++) {
        const values = lines[i].split(",").map((v) => v.trim());
        const row = {};

        headers.forEach((header, index) => {
          row[header] = values[index] || "";
        });

        // Validate required fields
        requiredFields.forEach((field) => {
          if (!row[field]) {
            errors.push({
              row: i + 1,
              field,
              message: intl.formatMessage(
                {
                  id: "biorepository.manifest.error.required",
                  defaultMessage: "{field} is required",
                },
                { field },
              ),
            });
          }
        });

        // Validate biosafety level per spec FR-MAN-003
        if (
          row.biosafetyLevel &&
          !["BSL_1", "BSL_2", "BSL_3", "BSL_4"].includes(row.biosafetyLevel)
        ) {
          errors.push({
            row: i + 1,
            field: "biosafetyLevel",
            message: intl.formatMessage(
              {
                id: "biorepository.manifest.error.invalidBSL",
                defaultMessage:
                  "Invalid biosafety level: {value}. Must be BSL_1, BSL_2, BSL_3, or BSL_4",
              },
              { value: row.biosafetyLevel },
            ),
          });
        }

        // Validate date format (supporting multiple formats per spec FR-MAN-004)
        if (row.collectionDate) {
          const dateRegex =
            /^\d{4}-\d{2}-\d{2}$|^\d{2}\/\d{2}\/\d{4}$|^\d{2}-\d{2}-\d{4}$/;
          if (!dateRegex.test(row.collectionDate)) {
            errors.push({
              row: i + 1,
              field: "collectionDate",
              message: intl.formatMessage({
                id: "biorepository.manifest.error.invalidDate",
                defaultMessage:
                  "Invalid date format. Use yyyy-MM-dd, dd/MM/yyyy, or dd-MM-yyyy",
              }),
            });
          }
        }

        row._rowNumber = i + 1;
        row._valid = errors.filter((e) => e.row === i + 1).length === 0;
        data.push(row);
      }

      return { data, errors };
    },
    [intl, requiredFields],
  );

  const handleFileChange = useCallback(
    (event) => {
      const uploadedFile = event.target.files?.[0];
      if (!uploadedFile) return;

      if (!uploadedFile.name.endsWith(".csv")) {
        setError(
          intl.formatMessage({
            id: "biorepository.manifest.error.invalidFormat",
            defaultMessage: "Please upload a CSV file",
          }),
        );
        return;
      }

      setFile(uploadedFile);
      setError(null);

      const reader = new FileReader();
      reader.onload = (e) => {
        try {
          const { data, errors } = parseCSV(e.target.result);
          setParsedData(data);
          setValidationErrors(errors);
          setBackendValidationDone(false);
          // Set to 'parsed' - user must click Preview & Validate to proceed
          setImportStatus("parsed");
        } catch (err) {
          setError(err.message);
          setParsedData([]);
          setValidationErrors([]);
        }
      };
      reader.onerror = () => {
        setError(
          intl.formatMessage({
            id: "biorepository.manifest.error.readFile",
            defaultMessage: "Failed to read file",
          }),
        );
      };
      reader.readAsText(uploadedFile);
    },
    [intl, parseCSV],
  );

  /**
   * Transform parsed CSV data to backend DTO format
   */
  const transformToBackendFormat = useCallback((data) => {
    return data.map((row) => {
      const sample = {};
      // Map each CSV column to its backend field name
      Object.entries(csvToBackendFieldMap).forEach(
        ([csvField, backendField]) => {
          const value = row[csvField];
          // Only include non-empty values
          if (value !== undefined && value !== "") {
            sample[backendField] = value;
          }
        },
      );
      // Default biosafety level if not provided
      if (!sample.biosafetyLevel) {
        sample.biosafetyLevel = "BSL_1";
      }
      return sample;
    });
  }, []);

  /**
   * Validate manifest data against backend before import.
   * This catches issues like invalid sample types or projects that don't exist in the database.
   */
  const validateWithBackend = useCallback(
    async (samples) => {
      const response = await postToOpenElisServerJsonResponse(
        "/rest/biorepository/sample/validate-manifest-import",
        JSON.stringify({
          samples,
          shipmentId,
        }),
      );
      return response;
    },
    [shipmentId],
  );

  /**
   * Handle Preview & Validate button click.
   * Sends parsed data to backend for validation and shows results.
   */
  const handlePreviewValidation = useCallback(async () => {
    // Don't proceed if there are frontend validation errors
    if (validationErrors.length > 0) {
      setError(
        intl.formatMessage({
          id: "biorepository.manifest.error.fixErrorsFirst",
          defaultMessage: "Please fix the validation errors before previewing.",
        }),
      );
      return;
    }

    setLoading(true);
    setError(null);
    setImportStatus("validating");

    try {
      const samples = transformToBackendFormat(parsedData);
      const validationResult = await validateWithBackend(samples);

      // Process backend validation results
      const backendErrors = [];
      const updatedData = parsedData.map((row, index) => {
        const backendRow = validationResult.rows?.[index];
        if (backendRow && !backendRow.valid && backendRow.errors) {
          backendRow.errors.forEach((errMsg) => {
            backendErrors.push({
              row: row._rowNumber,
              field: "backend",
              message: errMsg,
            });
          });
        }
        return {
          ...row,
          _valid: backendRow?.valid ?? row._valid,
          _backendErrors: backendRow?.errors || [],
        };
      });

      setParsedData(updatedData);
      setValidationErrors((prev) => [...prev, ...backendErrors]);
      setBackendValidationDone(true);
      setImportStatus("preview");

      if (!validationResult.valid) {
        setError(
          intl.formatMessage(
            {
              id: "biorepository.manifest.error.backendValidationPreview",
              defaultMessage:
                "{count} sample(s) have validation errors. Please review below.",
            },
            { count: validationResult.invalidCount },
          ),
        );
      }
    } catch (err) {
      setError(
        intl.formatMessage({
          id: "biorepository.manifest.error.validationFailed",
          defaultMessage:
            "Failed to validate samples with server. Please try again.",
        }),
      );
      setImportStatus("parsed");
    } finally {
      setLoading(false);
    }
  }, [
    parsedData,
    validationErrors,
    intl,
    transformToBackendFormat,
    validateWithBackend,
  ]);

  // Count of valid samples for display and logic
  const validSampleCount = parsedData.filter((row) => row._valid).length;

  const handleImport = useCallback(async () => {
    if (!backendValidationDone) {
      setError(
        intl.formatMessage({
          id: "biorepository.manifest.error.notValidated",
          defaultMessage:
            "Please validate the samples first by clicking Preview & Validate",
        }),
      );
      return;
    }

    // Filter to only valid samples
    const validSamples = parsedData.filter((row) => row._valid);

    if (validSamples.length === 0) {
      setError(
        intl.formatMessage({
          id: "biorepository.manifest.error.noValidSamples",
          defaultMessage:
            "No valid samples to import. Please fix errors first.",
        }),
      );
      return;
    }

    setLoading(true);
    setError(null);
    setImportStatus("importing");

    try {
      // Only send valid samples to backend
      const samples = transformToBackendFormat(validSamples);

      const response = await postToOpenElisServerJsonResponse(
        "/rest/biorepository/sample/register-bulk",
        JSON.stringify({
          samples,
          shipmentId,
        }),
      );

      if (response.error) {
        setError(response.error);
        setImportStatus("preview");
      } else {
        setImportStatus("complete");
        if (onImportComplete) {
          onImportComplete(response.samples || []);
        }
        // Close modal after successful import
        setTimeout(() => {
          handleClose();
        }, 2000);
      }
    } catch (err) {
      setError(
        intl.formatMessage({
          id: "biorepository.manifest.error.import",
          defaultMessage: "Failed to import samples. Please try again.",
        }),
      );
      setImportStatus("preview");
    } finally {
      setLoading(false);
    }
  }, [
    parsedData,
    backendValidationDone,
    shipmentId,
    onImportComplete,
    intl,
    transformToBackendFormat,
  ]);

  const handleClear = useCallback(() => {
    setFile(null);
    setParsedData([]);
    setValidationErrors([]);
    setImportStatus(null);
    setError(null);
    setBackendValidationDone(false);
  }, []);

  const handleClose = useCallback(() => {
    handleClear();
    onClose();
  }, [handleClear, onClose]);

  const tableHeaders = [
    { key: "row", header: "#" },
    {
      key: "externalId",
      header: intl.formatMessage({
        id: "biorepository.manifest.column.externalId",
        defaultMessage: "External ID",
      }),
    },
    {
      key: "sampleType",
      header: intl.formatMessage({
        id: "biorepository.manifest.column.sampleType",
        defaultMessage: "Sample Type",
      }),
    },
    {
      key: "originLab",
      header: intl.formatMessage({
        id: "biorepository.manifest.column.originLab",
        defaultMessage: "Origin Lab",
      }),
    },
    {
      key: "biosafetyLevel",
      header: intl.formatMessage({
        id: "biorepository.manifest.column.bsl",
        defaultMessage: "BSL",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "biorepository.manifest.column.status",
        defaultMessage: "Status",
      }),
    },
  ];

  const tableRows = parsedData.map((row) => ({
    id: String(row._rowNumber),
    row: row._rowNumber,
    externalId: row.externalId,
    sampleType: row.sampleType || "-",
    originLab: row.originLab || "-",
    biosafetyLevel: row.biosafetyLevel || "BSL_1",
    status: row._valid ? "valid" : "error",
  }));

  // Show errors for specific rows
  const getRowErrors = (rowNumber) => {
    return validationErrors.filter((e) => e.row === rowNumber);
  };

  return (
    <Modal
      open={open}
      onRequestClose={handleClose}
      modalHeading={intl.formatMessage({
        id: "biorepository.manifest.modal.title",
        defaultMessage: "Import Sample Manifest",
      })}
      modalLabel={intl.formatMessage({
        id: "biorepository.manifest.modal.label",
        defaultMessage: "Bulk Sample Registration",
      })}
      primaryButtonText={
        importStatus === "preview"
          ? intl.formatMessage(
              {
                id: "biorepository.manifest.button.import",
                defaultMessage: "Import {count} Valid Samples",
              },
              { count: validSampleCount },
            )
          : undefined
      }
      secondaryButtonText={
        importStatus === "preview"
          ? intl.formatMessage({
              id: "biorepository.manifest.button.clear",
              defaultMessage: "Upload Different File",
            })
          : importStatus === "parsed" || importStatus === "validating"
            ? undefined // Buttons are inline for parsed/validating states
            : intl.formatMessage({
                id: "biorepository.button.close",
                defaultMessage: "Close",
              })
      }
      onRequestSubmit={importStatus === "preview" ? handleImport : undefined}
      onSecondarySubmit={
        importStatus === "preview"
          ? handleClear
          : importStatus === "parsed" || importStatus === "validating"
            ? undefined
            : handleClose
      }
      primaryButtonDisabled={
        loading || validSampleCount === 0 || importStatus !== "preview"
      }
      size="lg"
      preventCloseOnClickOutside={loading}
    >
      {loading && <Loading description="Importing samples..." />}

      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "biorepository.manifest.error.title",
            defaultMessage: "Error",
          })}
          subtitle={error}
          lowContrast
          onClose={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {importStatus === "complete" && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({
            id: "biorepository.manifest.success.title",
            defaultMessage: "Import Complete",
          })}
          subtitle={intl.formatMessage(
            {
              id: "biorepository.manifest.success.message",
              defaultMessage:
                "{importedCount} samples imported successfully.{skippedMessage}",
            },
            {
              importedCount: validSampleCount,
              skippedMessage:
                parsedData.length > validSampleCount
                  ? ` ${parsedData.length - validSampleCount} invalid sample(s) were skipped.`
                  : "",
            },
          )}
          lowContrast
          hideCloseButton
          style={{ marginBottom: "1rem" }}
        />
      )}

      {importStatus === null && (
        <div style={{ marginBottom: "1.5rem" }}>
          <p style={{ marginBottom: "1rem" }}>
            <FormattedMessage
              id="biorepository.manifest.instructions"
              defaultMessage="Upload a CSV file containing sample data. Download the template below for the correct format."
            />
          </p>
          <div style={{ marginBottom: "1rem" }}>
            <Button
              kind="tertiary"
              size="sm"
              renderIcon={Download}
              onClick={downloadTemplate}
            >
              <FormattedMessage
                id="biorepository.manifest.button.downloadTemplate"
                defaultMessage="Download CSV Template"
              />
            </Button>
          </div>
          <div
            style={{
              padding: "1rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
              marginBottom: "1rem",
            }}
          >
            <h6 style={{ marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="biorepository.manifest.requiredFields.title"
                defaultMessage="Required Fields:"
              />
            </h6>
            <ul style={{ margin: 0, paddingLeft: "1.5rem" }}>
              {requiredFields.map((field) => (
                <li key={field}>{field}</li>
              ))}
            </ul>

            <h6 style={{ marginBottom: "0.5rem", marginTop: "1rem" }}>
              <FormattedMessage
                id="biorepository.manifest.conditionalFields.title"
                defaultMessage="Conditional Fields:"
              />
            </h6>
            <ul style={{ margin: 0, paddingLeft: "1.5rem" }}>
              {conditionalFields.map((field) => (
                <li key={field.name}>
                  {field.name}{" "}
                  <span style={{ color: "#525252", fontSize: "0.75rem" }}>
                    ({field.description})
                  </span>
                </li>
              ))}
            </ul>

            <h6 style={{ marginBottom: "0.5rem", marginTop: "1rem" }}>
              <FormattedMessage
                id="biorepository.manifest.optionalFields.title"
                defaultMessage="Optional Fields:"
              />
            </h6>
            <ul style={{ margin: 0, paddingLeft: "1.5rem" }}>
              {optionalFields.map((field) => (
                <li key={field}>{field}</li>
              ))}
            </ul>
          </div>

          <div
            style={{
              padding: "1rem",
              backgroundColor: "#e0f0e0",
              borderRadius: "4px",
              marginBottom: "1rem",
            }}
          >
            <h6 style={{ marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="biorepository.manifest.sampleTypes.title"
                defaultMessage="Supported Sample Types:"
              />
            </h6>
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))",
                gap: "0.5rem",
              }}
            >
              {supportedSampleTypes.map((category) => (
                <div key={category.category}>
                  <strong style={{ fontSize: "0.75rem", color: "#161616" }}>
                    {category.category}:
                  </strong>
                  <br />
                  <span style={{ fontSize: "0.75rem", color: "#525252" }}>
                    {category.types.join(", ")}
                  </span>
                </div>
              ))}
            </div>
          </div>
          <FileUploader
            labelTitle={intl.formatMessage({
              id: "biorepository.manifest.upload.title",
              defaultMessage: "Select manifest file",
            })}
            labelDescription={intl.formatMessage({
              id: "biorepository.manifest.upload.description",
              defaultMessage: "Upload CSV file with sample data",
            })}
            buttonLabel={intl.formatMessage({
              id: "biorepository.manifest.upload.button",
              defaultMessage: "Select CSV file",
            })}
            iconDescription={intl.formatMessage({
              id: "biorepository.manifest.upload.iconDescription",
              defaultMessage: "Delete file",
            })}
            accept={[".csv"]}
            multiple={false}
            onChange={handleFileChange}
            filenameStatus="edit"
          />
        </div>
      )}

      {(importStatus === "parsed" || importStatus === "validating") &&
        parsedData.length > 0 && (
          <div>
            <div style={{ marginBottom: "1rem" }}>
              <h5 style={{ display: "inline-block", marginRight: "0.5rem" }}>
                <FormattedMessage
                  id="biorepository.manifest.parsed.title"
                  defaultMessage="Parsed: {count} samples"
                  values={{ count: parsedData.length }}
                />
              </h5>
              {validationErrors.length > 0 && (
                <Tag type="red">
                  <Warning size={16} style={{ marginRight: "0.25rem" }} />
                  {validationErrors.length}{" "}
                  <FormattedMessage
                    id="biorepository.manifest.parsed.formatErrors"
                    defaultMessage="format error(s)"
                  />
                </Tag>
              )}
              {validationErrors.length === 0 && (
                <Tag type="blue">
                  <FormattedMessage
                    id="biorepository.manifest.parsed.readyToValidate"
                    defaultMessage="Ready for validation"
                  />
                </Tag>
              )}
            </div>

            {validationErrors.length > 0 && (
              <InlineNotification
                kind="warning"
                title={intl.formatMessage({
                  id: "biorepository.manifest.formatErrors.title",
                  defaultMessage: "Format Errors",
                })}
                subtitle={intl.formatMessage({
                  id: "biorepository.manifest.formatErrors.subtitle",
                  defaultMessage:
                    "Please fix these errors before validation. You may need to upload a corrected CSV.",
                })}
                lowContrast
                hideCloseButton
                style={{ marginBottom: "1rem" }}
              />
            )}

            {importStatus === "validating" && (
              <InlineNotification
                kind="info"
                title={intl.formatMessage({
                  id: "biorepository.manifest.validating.title",
                  defaultMessage: "Validating",
                })}
                subtitle={intl.formatMessage({
                  id: "biorepository.manifest.validating.subtitle",
                  defaultMessage:
                    "Checking samples against database for valid sample types, projects, etc...",
                })}
                lowContrast
                hideCloseButton
                style={{ marginBottom: "1rem" }}
              />
            )}

            <div
              style={{
                maxHeight: "300px",
                overflowY: "auto",
                marginBottom: "1rem",
              }}
            >
              <DataTable rows={tableRows} headers={tableHeaders} size="sm">
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <Table {...getTableProps()} size="sm">
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
                      {rows.map((row) => {
                        const rowNumber = parseInt(row.id);
                        const rowErrors = getRowErrors(rowNumber);
                        return (
                          <React.Fragment key={row.id}>
                            <TableRow {...getRowProps({ row })}>
                              {row.cells.map((cell) => (
                                <TableCell key={cell.id}>
                                  {cell.info.header === "status" ? (
                                    cell.value === "valid" ? (
                                      <Tag type="gray" size="sm">
                                        <FormattedMessage
                                          id="biorepository.manifest.status.pending"
                                          defaultMessage="Pending"
                                        />
                                      </Tag>
                                    ) : (
                                      <Tag type="red" size="sm">
                                        <FormattedMessage
                                          id="biorepository.manifest.status.error"
                                          defaultMessage="Error"
                                        />
                                      </Tag>
                                    )
                                  ) : (
                                    cell.value
                                  )}
                                </TableCell>
                              ))}
                            </TableRow>
                            {rowErrors.length > 0 && (
                              <TableRow>
                                <TableCell colSpan={headers.length}>
                                  <div
                                    style={{
                                      backgroundColor: "#fff1f1",
                                      padding: "0.5rem",
                                      fontSize: "0.875rem",
                                    }}
                                  >
                                    {rowErrors.map((err, idx) => (
                                      <div key={idx}>
                                        • {err.field}: {err.message}
                                      </div>
                                    ))}
                                  </div>
                                </TableCell>
                              </TableRow>
                            )}
                          </React.Fragment>
                        );
                      })}
                    </TableBody>
                  </Table>
                )}
              </DataTable>
            </div>

            <div
              style={{
                display: "flex",
                gap: "1rem",
                justifyContent: "flex-end",
              }}
            >
              <Button
                kind="secondary"
                size="md"
                onClick={handleClear}
                disabled={loading}
              >
                <FormattedMessage
                  id="biorepository.manifest.button.uploadDifferent"
                  defaultMessage="Upload Different File"
                />
              </Button>
              <Button
                kind="primary"
                size="md"
                onClick={handlePreviewValidation}
                disabled={loading || validationErrors.length > 0}
                renderIcon={
                  importStatus === "validating" ? undefined : Checkmark
                }
              >
                {importStatus === "validating" ? (
                  <FormattedMessage
                    id="biorepository.manifest.button.validating"
                    defaultMessage="Validating..."
                  />
                ) : (
                  <FormattedMessage
                    id="biorepository.manifest.button.previewValidate"
                    defaultMessage="Preview & Validate"
                  />
                )}
              </Button>
            </div>
          </div>
        )}

      {importStatus === "preview" && parsedData.length > 0 && (
        <div>
          <div style={{ marginBottom: "1rem" }}>
            <h5 style={{ display: "inline-block", marginRight: "0.5rem" }}>
              <FormattedMessage
                id="biorepository.manifest.preview.title"
                defaultMessage="Preview: {count} samples"
                values={{ count: parsedData.length }}
              />
            </h5>
            {validationErrors.length > 0 && (
              <Tag type="red">
                <Warning size={16} style={{ marginRight: "0.25rem" }} />
                {validationErrors.length} error(s)
              </Tag>
            )}
            {validationErrors.length === 0 && (
              <Tag type="green">
                <Checkmark size={16} style={{ marginRight: "0.25rem" }} />
                <FormattedMessage
                  id="biorepository.manifest.preview.valid"
                  defaultMessage="All valid"
                />
              </Tag>
            )}
          </div>

          {validationErrors.length > 0 && (
            <InlineNotification
              kind="warning"
              title={intl.formatMessage({
                id: "biorepository.manifest.validation.title",
                defaultMessage: "Validation Errors",
              })}
              subtitle={intl.formatMessage({
                id: "biorepository.manifest.validation.subtitle",
                defaultMessage:
                  "Please fix the errors below before importing. Rows with errors are highlighted.",
              })}
              lowContrast
              hideCloseButton
              style={{ marginBottom: "1rem" }}
            />
          )}

          <div style={{ maxHeight: "400px", overflowY: "auto" }}>
            <DataTable rows={tableRows} headers={tableHeaders} size="sm">
              {({
                rows,
                headers,
                getTableProps,
                getHeaderProps,
                getRowProps,
              }) => (
                <Table {...getTableProps()} size="sm">
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
                    {rows.map((row) => {
                      const rowNumber = parseInt(row.id);
                      const rowErrors = getRowErrors(rowNumber);
                      return (
                        <React.Fragment key={row.id}>
                          <TableRow {...getRowProps({ row })}>
                            {row.cells.map((cell) => (
                              <TableCell key={cell.id}>
                                {cell.info.header === "status" ? (
                                  cell.value === "valid" ? (
                                    <Tag type="green" size="sm">
                                      <FormattedMessage
                                        id="biorepository.manifest.status.valid"
                                        defaultMessage="Valid"
                                      />
                                    </Tag>
                                  ) : (
                                    <Tag type="red" size="sm">
                                      <FormattedMessage
                                        id="biorepository.manifest.status.error"
                                        defaultMessage="Error"
                                      />
                                    </Tag>
                                  )
                                ) : (
                                  cell.value
                                )}
                              </TableCell>
                            ))}
                          </TableRow>
                          {rowErrors.length > 0 && (
                            <TableRow>
                              <TableCell colSpan={headers.length}>
                                <div
                                  style={{
                                    backgroundColor: "#fff1f1",
                                    padding: "0.5rem",
                                    fontSize: "0.875rem",
                                  }}
                                >
                                  {rowErrors.map((err, idx) => (
                                    <div key={idx}>
                                      • {err.field}: {err.message}
                                    </div>
                                  ))}
                                </div>
                              </TableCell>
                            </TableRow>
                          )}
                        </React.Fragment>
                      );
                    })}
                  </TableBody>
                </Table>
              )}
            </DataTable>
          </div>
        </div>
      )}
    </Modal>
  );
}

ManifestUploadModal.propTypes = {
  open: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  shipmentId: PropTypes.number,
  onImportComplete: PropTypes.func,
};

export default ManifestUploadModal;

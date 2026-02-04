import React, { useState, useCallback, useEffect } from "react";
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
  Button,
} from "@carbon/react";
import { Checkmark, Download } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../../config.json";
import "../workflow/NotebookWorkflow.css";

/**
 * PathologyManifestImportModal - Modal for importing pathology samples from CSV manifest files.
 * Supports two manifest types:
 *
 * 1. CLINICAL MANIFEST - For clinical diagnostic samples (currently disabled)
 *
 * 2. RESEARCH MANIFEST - For research samples:
 *    Required: firstName, studyId, piName, participantAnimalId, specimenType,
 *              specimenSite (Patient Site / Anatomical Source), collectionDateTime,
 *              collectionMethod, collector, processingCondition, laboratoryMaterial,
 *              ethicalApprovalRef, receivedDateTime, receivedBy
 *    Optional: surname, sourceFacility
 *
 * Sample Attributes (ALL MANDATORY for all samples):
 *    - specimenType - Specimen Type (Tissue, Cytology, Body Fluid, Blood, Bone Marrow)
 *    - specimenSite - Patient Site / Anatomical Source
 *    - collectionDateTime - Collection Date & Time
 *    - collectionMethod - How sample was collected (Biopsy, FNAC, etc.)
 *    - collector - Personnel who collected the sample
 *    - processingCondition - Sample condition (Fresh, Fixed, Frozen, FFPE, etc.)
 *    - laboratoryMaterial - Material type (Blocks, Slides, Smears, etc.)
 */

// Clinical manifest column definitions
const CLINICAL_COLUMNS = {
  required: [
    { key: "firstNameColumn", label: "First Name", csvHeader: "firstName" },
    { key: "patientIdColumn", label: "Patient ID", csvHeader: "patientId" },
    {
      key: "requestingClinicianColumn",
      label: "Requesting Clinician",
      csvHeader: "requestingClinician",
    },
    {
      key: "specimenTypeColumn",
      label: "Specimen Type",
      csvHeader: "specimenType",
    },
    {
      key: "collectionDateTimeColumn",
      label: "Collection Date & Time",
      csvHeader: "collectionDateTime",
    },
    {
      key: "clinicalDetailsColumn",
      label: "Clinical Details/Indication",
      csvHeader: "clinicalDetails",
    },
    {
      key: "receivedDateTimeColumn",
      label: "Received Date & Time",
      csvHeader: "receivedDateTime",
    },
    { key: "receivedByColumn", label: "Received By", csvHeader: "receivedBy" },
  ],
  optional: [
    { key: "surnameColumn", label: "Surname/Last Name", csvHeader: "surname" },
    {
      key: "specimenSiteColumn",
      label: "Specimen Site",
      csvHeader: "specimenSite",
    },
    {
      key: "sourceFacilityColumn",
      label: "Source Facility",
      csvHeader: "sourceFacility",
    },
  ],
};

// Research manifest column definitions
const RESEARCH_COLUMNS = {
  required: [
    { key: "firstNameColumn", label: "First Name", csvHeader: "firstName" },
    { key: "studyIdColumn", label: "Study ID", csvHeader: "studyId" },
    {
      key: "piNameColumn",
      label: "Principal Investigator (PI)",
      csvHeader: "piName",
    },
    {
      key: "participantAnimalIdColumn",
      label: "Participant/Animal ID",
      csvHeader: "participantAnimalId",
    },
    {
      key: "specimenTypeColumn",
      label: "Specimen Type",
      csvHeader: "specimenType",
    },
    {
      key: "specimenSiteColumn",
      label: "Patient Site / Anatomical Source",
      csvHeader: "specimenSite",
    },
    {
      key: "collectionDateTimeColumn",
      label: "Collection Date & Time",
      csvHeader: "collectionDateTime",
    },
    {
      key: "collectionMethodColumn",
      label: "Collection Method",
      csvHeader: "collectionMethod",
    },
    {
      key: "collectorColumn",
      label: "Collector / Personnel",
      csvHeader: "collector",
    },
    {
      key: "processingConditionColumn",
      label: "Processing Condition",
      csvHeader: "processingCondition",
    },
    {
      key: "laboratoryMaterialColumn",
      label: "Laboratory Material",
      csvHeader: "laboratoryMaterial",
    },
    {
      key: "ethicalApprovalRefColumn",
      label: "Ethical Approval Reference",
      csvHeader: "ethicalApprovalRef",
    },
    {
      key: "receivedDateTimeColumn",
      label: "Received Date & Time",
      csvHeader: "receivedDateTime",
    },
    { key: "receivedByColumn", label: "Received By", csvHeader: "receivedBy" },
  ],
  optional: [
    { key: "surnameColumn", label: "Surname/Last Name", csvHeader: "surname" },
    {
      key: "sourceFacilityColumn",
      label: "Source Facility",
      csvHeader: "sourceFacility",
    },
  ],
};

function PathologyManifestImportModal({
  open,
  onClose,
  entryId,
  manifestType, // "clinical" or "research" - passed from parent
  onImportSuccess,
}) {
  const intl = useIntl();

  // State for file upload
  const [file, setFile] = useState(null);
  const [fileError, setFileError] = useState(null);

  // State for column mapping
  const [csvHeaders, setCsvHeaders] = useState([]);
  const [columnMapping, setColumnMapping] = useState({});

  // State for preview
  const [previewData, setPreviewData] = useState(null);
  const [previewErrors, setPreviewErrors] = useState([]);
  const [isPreviewLoading, setIsPreviewLoading] = useState(false);
  const [isImporting, setIsImporting] = useState(false);

  // Steps: 1 = Upload, 2 = Map Columns, 3 = Preview, 4 = Success
  const [step, setStep] = useState(1);

  // Get current column definitions based on manifest type
  const getColumnDefs = useCallback(() => {
    return manifestType === "clinical" ? CLINICAL_COLUMNS : RESEARCH_COLUMNS;
  }, [manifestType]);

  // Initialize column mapping when manifest type changes or modal opens
  useEffect(() => {
    if (open && manifestType) {
      const defs = getColumnDefs();
      const initialMapping = {};
      [...defs.required, ...defs.optional].forEach((col) => {
        initialMapping[col.key] = "";
      });
      initialMapping.dateFormat = "yyyy-MM-dd HH:mm";
      // Note: sampleCategory is determined by the endpoint (clinical vs research),
      // not sent in the form data
      setColumnMapping(initialMapping);
    }
  }, [open, manifestType, getColumnDefs]);

  const handleFileAdded = useCallback(
    (event, { addedFiles }) => {
      const addedFile = addedFiles[0];
      if (!addedFile) return;

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
        const text = e.target.result;
        const firstLine = text.split("\n")[0];
        const headers = firstLine
          .split(",")
          .map((h) => h.trim().replace(/"/g, ""));
        setCsvHeaders(headers);

        // Auto-map columns based on header names
        const defs = getColumnDefs();
        const autoMapping = { ...columnMapping };

        headers.forEach((header) => {
          const lowerHeader = header.toLowerCase().replace(/[\s_-]/g, "");

          [...defs.required, ...defs.optional].forEach((col) => {
            const expectedHeader = col.csvHeader.toLowerCase();
            if (lowerHeader === expectedHeader) {
              autoMapping[col.key] = header;
            }
          });
        });

        setColumnMapping(autoMapping);
        setStep(2); // Go to column mapping step
      };
      reader.readAsText(addedFile);
    },
    [intl, getColumnDefs, columnMapping],
  );

  const handleRemoveFile = () => {
    setFile(null);
    setCsvHeaders([]);
    setPreviewData(null);
    setPreviewErrors([]);
  };

  const handleMappingChange = (field, value) => {
    setColumnMapping((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleDownloadTemplate = () => {
    window.open(
      `${config.serverBaseUrl}/rest/notebook/pathology/manifest-template/${manifestType}`,
      "_blank",
    );
  };

  const handlePreview = async () => {
    if (!file || !entryId) return;

    setIsPreviewLoading(true);
    setPreviewErrors([]);

    const formData = new FormData();
    formData.append("file", file);
    formData.append(
      "mapping",
      new Blob([JSON.stringify(columnMapping)], { type: "application/json" }),
    );

    const endpoint = `${config.serverBaseUrl}/rest/notebook/pathology/entry/${entryId}/samples/preview-manifest/${manifestType}`;

    try {
      const response = await fetch(endpoint, {
        method: "POST",
        body: formData,
        credentials: "include",
        headers: {
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
      });

      const data = await response.json();

      if (response.ok) {
        setPreviewData(data);
        setPreviewErrors(data.errors || []);
        setStep(3); // Go to preview step
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
      setPreviewErrors([
        { rowNumber: 0, column: "file", message: error.message },
      ]);
    } finally {
      setIsPreviewLoading(false);
    }
  };

  const handleImport = async () => {
    if (!file || !entryId) return;

    setIsImporting(true);

    const formData = new FormData();
    formData.append("file", file);
    formData.append(
      "mapping",
      new Blob([JSON.stringify(columnMapping)], { type: "application/json" }),
    );

    const endpoint = `${config.serverBaseUrl}/rest/notebook/pathology/entry/${entryId}/samples/create-from-manifest/${manifestType}`;

    try {
      const response = await fetch(endpoint, {
        method: "POST",
        body: formData,
        credentials: "include",
        headers: {
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
      });

      const data = await response.json();

      if (response.ok && data.success) {
        setStep(4); // Go to success step
        if (onImportSuccess) {
          onImportSuccess(data);
        }
      } else {
        setPreviewErrors(
          data.errors || [
            { rowNumber: 0, column: "import", message: data.error },
          ],
        );
      }
    } catch (error) {
      setPreviewErrors([
        { rowNumber: 0, column: "import", message: error.message },
      ]);
    } finally {
      setIsImporting(false);
    }
  };

  const handleClose = () => {
    setFile(null);
    setCsvHeaders([]);
    setColumnMapping({});
    setPreviewData(null);
    setPreviewErrors([]);
    setStep(1);
    onClose();
  };

  // Check if required columns are mapped
  const requiredColumnsMapped = useCallback(() => {
    if (!manifestType) return false;
    const defs = getColumnDefs();
    return defs.required.every((col) => columnMapping[col.key]);
  }, [manifestType, getColumnDefs, columnMapping]);

  // Count auto-mapped columns
  const autoMappedCount = Object.entries(columnMapping).filter(
    ([key, value]) => value && key !== "dateFormat" && key !== "sampleCategory",
  ).length;

  const renderColumnMappingSelect = (col, required = false) => {
    const isMapped = columnMapping[col.key] && columnMapping[col.key] !== "";
    return (
      <div key={col.key} className="mapping-field-container">
        <Select
          id={`mapping-${col.key}`}
          labelText={
            col.label + (required ? " *" : "") + (isMapped ? " ✓" : "")
          }
          value={columnMapping[col.key] || ""}
          onChange={(e) => handleMappingChange(col.key, e.target.value)}
          size="sm"
        >
          <SelectItem value="" text="Select column..." />
          {csvHeaders.map((header) => (
            <SelectItem key={header} value={header} text={header} />
          ))}
        </Select>
      </div>
    );
  };

  // Get preview table headers based on manifest type
  const getPreviewHeaders = () => {
    if (manifestType === "clinical") {
      return [
        { key: "firstName", header: "First Name" },
        { key: "patientId", header: "Patient ID" },
        { key: "specimenType", header: "Specimen Type" },
        { key: "requestingClinician", header: "Clinician" },
        { key: "receivedBy", header: "Received By" },
      ];
    } else {
      return [
        { key: "firstName", header: "First Name" },
        { key: "studyId", header: "Study ID" },
        { key: "specimenType", header: "Specimen Type" },
        { key: "participantAnimalId", header: "Participant/Animal ID" },
        { key: "receivedBy", header: "Received By" },
      ];
    }
  };

  // Get modal heading based on manifest type
  const getModalHeading = () => {
    const typeLabel = manifestType === "clinical" ? "Clinical" : "Research";
    return intl.formatMessage(
      {
        id: "pathology.manifest.title.typed",
        defaultMessage: "Import {type} Samples from Manifest",
      },
      { type: typeLabel },
    );
  };

  return (
    <Modal
      open={open}
      onRequestClose={handleClose}
      modalHeading={getModalHeading()}
      primaryButtonText={
        step === 1
          ? intl.formatMessage({
              id: "label.button.next",
              defaultMessage: "Next",
            })
          : step === 2
            ? intl.formatMessage({
                id: "notebook.manifest.preview",
                defaultMessage: "Preview",
              })
            : step === 3
              ? intl.formatMessage({
                  id: "notebook.manifest.import",
                  defaultMessage: "Import",
                })
              : intl.formatMessage({
                  id: "label.button.close",
                  defaultMessage: "Close",
                })
      }
      secondaryButtonText={
        step > 1 && step < 4
          ? intl.formatMessage({
              id: "label.button.back",
              defaultMessage: "Back",
            })
          : null
      }
      onRequestSubmit={() => {
        if (step === 1 && file) setStep(2);
        else if (step === 2) handlePreview();
        else if (step === 3) handleImport();
        else handleClose();
      }}
      onSecondarySubmit={() => {
        if (step === 2) {
          handleRemoveFile();
          setStep(1);
        } else {
          setStep(step - 1);
        }
      }}
      primaryButtonDisabled={
        (step === 1 && !file) ||
        (step === 2 && !requiredColumnsMapped()) ||
        (step === 3 && previewErrors.length > 0) ||
        isPreviewLoading ||
        isImporting
      }
      size="lg"
    >
      <div className="manifest-import-modal pathology-manifest">
        {/* Step 1: File Upload */}
        {step === 1 && (
          <div className="upload-step">
            <p className="step-description">
              <FormattedMessage
                id="pathology.manifest.uploadDescription"
                defaultMessage="Upload a CSV file containing {type} sample information."
                values={{
                  type:
                    manifestType === "clinical"
                      ? "clinical diagnostic"
                      : "research",
                }}
              />
            </p>

            <Button
              kind="ghost"
              size="sm"
              renderIcon={Download}
              onClick={handleDownloadTemplate}
              style={{ marginBottom: "1rem" }}
            >
              <FormattedMessage
                id="pathology.manifest.downloadTemplate"
                defaultMessage="Download {type} CSV Template"
                values={{
                  type: manifestType === "clinical" ? "Clinical" : "Research",
                }}
              />
            </Button>

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

            <div
              style={{
                marginTop: "1rem",
                padding: "1rem",
                backgroundColor: "#f4f4f4",
                borderRadius: "4px",
              }}
            >
              <p style={{ marginBottom: "0.5rem" }}>
                <strong>Expected CSV columns:</strong>
              </p>
              <code
                style={{
                  fontSize: "0.75rem",
                  display: "block",
                  backgroundColor: "#e0e0e0",
                  padding: "0.5rem",
                  overflowX: "auto",
                }}
              >
                {[...getColumnDefs().required, ...getColumnDefs().optional]
                  .map((col) => col.csvHeader)
                  .join(", ")}
              </code>
            </div>
          </div>
        )}

        {/* Step 2: Column Mapping */}
        {step === 2 && (
          <div className="mapping-step">
            <p className="step-description">
              <FormattedMessage
                id="pathology.manifest.mappingDescription"
                defaultMessage="Map columns from your CSV to the required fields. Fields marked with * are required."
              />
            </p>

            {/* Auto-mapping summary */}
            <div
              style={{
                marginBottom: "1rem",
                padding: "0.75rem",
                backgroundColor: autoMappedCount > 0 ? "#defbe6" : "#fff1f1",
                borderRadius: "4px",
                border:
                  autoMappedCount > 0
                    ? "1px solid #24a148"
                    : "1px solid #da1e28",
              }}
            >
              {autoMappedCount > 0 ? (
                <span style={{ color: "#24a148", fontWeight: 500 }}>
                  Auto-mapped {autoMappedCount} column(s). Fields marked with ✓
                  are mapped.
                </span>
              ) : (
                <span style={{ color: "#da1e28", fontWeight: 500 }}>
                  No columns were auto-mapped. Please manually select CSV
                  columns.
                </span>
              )}
            </div>

            {/* Detected columns */}
            <div
              style={{
                marginBottom: "1rem",
                padding: "0.5rem",
                backgroundColor: "#e8e8e8",
                borderRadius: "4px",
              }}
            >
              <strong style={{ fontSize: "0.85rem" }}>
                Detected CSV columns:
              </strong>{" "}
              <span style={{ fontSize: "0.8rem" }}>
                {csvHeaders.join(", ")}
              </span>
            </div>

            {/* Required Fields */}
            <div
              style={{
                padding: "1rem",
                marginBottom: "1rem",
                border: "2px solid #0f62fe",
                borderRadius: "4px",
                backgroundColor: "#edf5ff",
              }}
            >
              <h5 style={{ marginBottom: "1rem", color: "#0f62fe" }}>
                Required Fields *
              </h5>
              <div className="mapping-grid">
                {getColumnDefs().required.map((col) =>
                  renderColumnMappingSelect(col, true),
                )}
              </div>
            </div>

            {/* Optional Fields */}
            <div
              style={{
                padding: "1rem",
                border: "1px solid #e0e0e0",
                borderRadius: "4px",
              }}
            >
              <h5 style={{ marginBottom: "1rem", color: "#525252" }}>
                Optional Fields
              </h5>
              <div className="mapping-grid">
                {getColumnDefs().optional.map((col) =>
                  renderColumnMappingSelect(col, false),
                )}
              </div>
            </div>

            {isPreviewLoading && (
              <Loading withOverlay={false} description="Loading preview..." />
            )}

            {previewErrors.length > 0 && (
              <InlineNotification
                kind="error"
                title="Validation Error"
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
            {previewErrors.length > 0 && (
              <InlineNotification
                kind="error"
                title="Validation Errors"
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

            {previewData && previewErrors.length === 0 && (
              <>
                <div className="preview-summary">
                  <Tag type="blue">{previewData.totalRows} rows</Tag>
                  <Tag type="green">
                    {previewData.totalSamples} samples to create
                  </Tag>
                  <Tag type="purple">
                    {manifestType === "clinical" ? "Clinical" : "Research"}{" "}
                    samples
                  </Tag>
                </div>

                <DataTable
                  rows={previewData.rows.map((row, idx) => ({
                    id: String(idx),
                    ...row,
                  }))}
                  headers={getPreviewHeaders()}
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
                          <TableRow key={row.id} {...getRowProps({ row })}>
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
              </>
            )}

            {isImporting && (
              <Loading withOverlay={false} description="Creating samples..." />
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
                id="pathology.manifest.success.message"
                defaultMessage="{type} samples have been created and linked to the notebook entry."
                values={{
                  type: manifestType === "clinical" ? "Clinical" : "Research",
                }}
              />
            </p>
          </div>
        )}
      </div>
    </Modal>
  );
}

export default PathologyManifestImportModal;

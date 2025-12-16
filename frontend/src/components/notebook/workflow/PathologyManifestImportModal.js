import React, { useState, useCallback } from "react";
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
  Accordion,
  AccordionItem,
} from "@carbon/react";
import { Checkmark } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../../config.json";
import "../workflow/NotebookWorkflow.css";

/**
 * PathologyManifestImportModal - Modal for importing pathology samples from CSV manifest files.
 * Uses pathology-specific field mappings matching the PathologySampleCreationPage form.
 *
 * @param {Object} props
 * @param {boolean} props.open - Whether the modal is open
 * @param {function} props.onClose - Callback when modal is closed
 * @param {number} props.entryId - The notebook entry ID to import samples to
 * @param {function} props.onImportSuccess - Callback when import is successful
 */
function PathologyManifestImportModal({
  open,
  onClose,
  entryId,
  onImportSuccess,
}) {
  const intl = useIntl();

  // State for file upload
  const [file, setFile] = useState(null);
  const [fileError, setFileError] = useState(null);

  // State for column mapping - pathology-specific fields
  const [csvHeaders, setCsvHeaders] = useState([]);
  const [columnMapping, setColumnMapping] = useState({
    // Required fields
    groupIdColumn: "",
    sampleTypeColumn: "",
    numOfSamplesColumn: "",
    collectionDateColumn: "",
    // Sample Identity
    sampleCategoryColumn: "",
    // Sample Source
    sourceFacilityColumn: "",
    specimenSiteColumn: "",
    // Clinical metadata
    patientIdColumn: "",
    requestingClinicianColumn: "",
    clinicalDetailsColumn: "",
    // Research metadata
    studyIdColumn: "",
    piNameColumn: "",
    participantAnimalIdColumn: "",
    ethicalApprovalRefColumn: "",
    // Notes
    notesColumn: "",
    dateFormat: "yyyy-MM-dd",
  });

  // State for preview
  const [previewData, setPreviewData] = useState(null);
  const [previewErrors, setPreviewErrors] = useState([]);
  const [isPreviewLoading, setIsPreviewLoading] = useState(false);
  const [isImporting, setIsImporting] = useState(false);

  // Steps: 1 = Upload, 2 = Map Columns, 3 = Preview, 4 = Import
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
        const text = e.target.result;
        const firstLine = text.split("\n")[0];
        const headers = firstLine
          .split(",")
          .map((h) => h.trim().replace(/"/g, ""));
        setCsvHeaders(headers);

        // Auto-map columns based on header names
        const autoMapping = { ...columnMapping };
        headers.forEach((header) => {
          const lowerHeader = header.toLowerCase();
          if (
            lowerHeader.includes("groupid") ||
            lowerHeader.includes("group_id")
          ) {
            autoMapping.groupIdColumn = header;
          } else if (
            lowerHeader.includes("sampletype") ||
            lowerHeader.includes("sample_type") ||
            lowerHeader === "sampletype"
          ) {
            autoMapping.sampleTypeColumn = header;
          } else if (
            lowerHeader.includes("numofsamples") ||
            lowerHeader.includes("num_of_samples") ||
            lowerHeader.includes("quantity")
          ) {
            autoMapping.numOfSamplesColumn = header;
          } else if (
            lowerHeader.includes("collectiondate") ||
            lowerHeader.includes("collection_date")
          ) {
            autoMapping.collectionDateColumn = header;
          } else if (
            lowerHeader.includes("samplecategory") ||
            lowerHeader.includes("sample_category") ||
            lowerHeader.includes("category")
          ) {
            autoMapping.sampleCategoryColumn = header;
          } else if (
            lowerHeader.includes("sourcefacility") ||
            lowerHeader.includes("source_facility") ||
            lowerHeader.includes("facility")
          ) {
            autoMapping.sourceFacilityColumn = header;
          } else if (
            lowerHeader.includes("specimensite") ||
            lowerHeader.includes("specimen_site") ||
            lowerHeader.includes("site")
          ) {
            autoMapping.specimenSiteColumn = header;
          } else if (
            lowerHeader.includes("patientid") ||
            lowerHeader.includes("patient_id")
          ) {
            autoMapping.patientIdColumn = header;
          } else if (
            lowerHeader.includes("requestingclinician") ||
            lowerHeader.includes("requesting_clinician") ||
            lowerHeader.includes("clinician")
          ) {
            autoMapping.requestingClinicianColumn = header;
          } else if (
            lowerHeader.includes("clinicaldetails") ||
            lowerHeader.includes("clinical_details")
          ) {
            autoMapping.clinicalDetailsColumn = header;
          } else if (
            lowerHeader.includes("studyid") ||
            lowerHeader.includes("study_id")
          ) {
            autoMapping.studyIdColumn = header;
          } else if (
            lowerHeader.includes("piname") ||
            lowerHeader.includes("pi_name") ||
            lowerHeader === "pi"
          ) {
            autoMapping.piNameColumn = header;
          } else if (
            lowerHeader.includes("participantanimalid") ||
            lowerHeader.includes("participant_animal_id") ||
            lowerHeader.includes("participant") ||
            lowerHeader.includes("animal_id")
          ) {
            autoMapping.participantAnimalIdColumn = header;
          } else if (
            lowerHeader.includes("ethicalapprovalref") ||
            lowerHeader.includes("ethical_approval") ||
            lowerHeader.includes("irb") ||
            lowerHeader.includes("iacuc")
          ) {
            autoMapping.ethicalApprovalRefColumn = header;
          } else if (
            lowerHeader.includes("notes") ||
            lowerHeader.includes("comments")
          ) {
            autoMapping.notesColumn = header;
          }
        });

        setColumnMapping(autoMapping);
        setStep(2);
      };
      reader.readAsText(addedFile);
    },
    [intl, columnMapping],
  );

  const handleRemoveFile = () => {
    setFile(null);
    setCsvHeaders([]);
    setColumnMapping({
      groupIdColumn: "",
      sampleTypeColumn: "",
      numOfSamplesColumn: "",
      collectionDateColumn: "",
      sampleCategoryColumn: "",
      sourceFacilityColumn: "",
      specimenSiteColumn: "",
      patientIdColumn: "",
      requestingClinicianColumn: "",
      clinicalDetailsColumn: "",
      studyIdColumn: "",
      piNameColumn: "",
      participantAnimalIdColumn: "",
      ethicalApprovalRefColumn: "",
      notesColumn: "",
      dateFormat: "yyyy-MM-dd",
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
      console.error("PathologyManifestImportModal: No file selected");
      return;
    }

    if (!entryId) {
      console.error("PathologyManifestImportModal: No entryId provided");
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
    formData.append(
      "mapping",
      new Blob([JSON.stringify(columnMapping)], { type: "application/json" }),
    );

    const endpoint = `${config.serverBaseUrl}/rest/notebook/pathology/entry/${entryId}/samples/preview-manifest`;

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
      console.error("PathologyManifestImportModal: Preview error:", error);
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

    const endpoint = `${config.serverBaseUrl}/rest/notebook/pathology/entry/${entryId}/samples/create-from-manifest`;

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
        setStep(4);
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
    handleRemoveFile();
    onClose();
  };

  const requiredColumnsMapped =
    columnMapping.groupIdColumn &&
    columnMapping.sampleTypeColumn &&
    columnMapping.numOfSamplesColumn;

  const renderColumnMappingSelect = (field, label, required = false) => (
    <div className="mapping-field-container">
      <Select
        id={`mapping-${field}`}
        labelText={label + (required ? " *" : "")}
        value={columnMapping[field]}
        onChange={(e) => handleMappingChange(field, e.target.value)}
        size="sm"
      >
        <SelectItem
          value=""
          text={intl.formatMessage({
            id: "label.select",
            defaultMessage: "Select column...",
          })}
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
        id: "pathology.manifest.title",
        defaultMessage: "Import Pathology Samples from Manifest",
      })}
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
      onSecondarySubmit={() => setStep(step - 1)}
      primaryButtonDisabled={
        (step === 1 && !file) ||
        (step === 2 && !requiredColumnsMapped) ||
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
                defaultMessage="Upload a CSV file containing pathology sample information. The file should include columns for Group ID, Sample Type, Number of Samples, and optionally Sample Category, Source Facility, Patient ID (for clinical samples), or Study ID (for research samples)."
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

            <div
              className="template-info"
              style={{
                marginTop: "1rem",
                padding: "1rem",
                backgroundColor: "#f4f4f4",
                borderRadius: "4px",
              }}
            >
              <p style={{ marginBottom: "0.5rem" }}>
                <strong>Required CSV columns (exact names):</strong>
              </p>
              <ul style={{ fontSize: "0.85rem", marginBottom: "0.5rem" }}>
                <li>
                  <strong>GroupID</strong> - Batch identifier (e.g.,
                  PATH-2025-001)
                </li>
                <li>
                  <strong>SampleType</strong> - Must match system sample type
                  (e.g., FFPE tissue block)
                </li>
                <li>
                  <strong>NumOfSamples</strong> - Number only: 1, 2, 3, etc.
                  (NOT text!)
                </li>
              </ul>
              <p style={{ marginBottom: "0.5rem" }}>
                <strong>Optional columns:</strong>
              </p>
              <code
                style={{
                  fontSize: "0.75rem",
                  display: "block",
                  backgroundColor: "#e0e0e0",
                  padding: "0.5rem",
                }}
              >
                CollectionDate, SampleCategory, SourceFacility, SpecimenSite,
                PatientID, RequestingClinician, ClinicalDetails, StudyID,
                PIName, ParticipantAnimalID, EthicalApprovalRef, Notes
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
                defaultMessage="Map columns from your CSV to pathology fields. Auto-mapped columns are pre-selected."
              />
            </p>

            {/* Show detected CSV columns */}
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

            {/* Required Fields - Always visible, highlighted */}
            <div
              style={{
                padding: "1rem",
                marginBottom: "1rem",
                border: "2px solid #0f62fe",
                borderRadius: "4px",
                backgroundColor: "#edf5ff",
              }}
            >
              <h5 style={{ marginBottom: "0.5rem", color: "#0f62fe" }}>
                Required Fields *
              </h5>
              <p
                style={{
                  fontSize: "0.75rem",
                  color: "#525252",
                  marginBottom: "1rem",
                }}
              >
                IMPORTANT: NumOfSamples column must contain numbers only (1, 2,
                3...)
              </p>
              <div className="mapping-grid">
                {renderColumnMappingSelect(
                  "groupIdColumn",
                  "Group ID (batch identifier) *",
                  true,
                )}
                {renderColumnMappingSelect(
                  "sampleTypeColumn",
                  "Sample Type *",
                  true,
                )}
                {renderColumnMappingSelect(
                  "numOfSamplesColumn",
                  "Number of Samples (numbers only!) *",
                  true,
                )}
                {renderColumnMappingSelect(
                  "collectionDateColumn",
                  "Collection Date",
                )}
              </div>
            </div>

            <Accordion>
              {/* Sample Identity */}
              <AccordionItem
                title={intl.formatMessage({
                  id: "pathology.manifest.section.identity",
                  defaultMessage: "Sample Identity",
                })}
              >
                <div className="mapping-grid">
                  {renderColumnMappingSelect(
                    "sampleCategoryColumn",
                    intl.formatMessage({
                      id: "pathology.manifest.column.sampleCategory",
                      defaultMessage: "Sample Category",
                    }),
                  )}
                  {renderColumnMappingSelect(
                    "sourceFacilityColumn",
                    intl.formatMessage({
                      id: "pathology.manifest.column.sourceFacility",
                      defaultMessage: "Source Facility",
                    }),
                  )}
                  {renderColumnMappingSelect(
                    "specimenSiteColumn",
                    intl.formatMessage({
                      id: "pathology.manifest.column.specimenSite",
                      defaultMessage: "Specimen Site",
                    }),
                  )}
                </div>
              </AccordionItem>

              {/* Clinical Metadata */}
              <AccordionItem
                title={intl.formatMessage({
                  id: "pathology.manifest.section.clinical",
                  defaultMessage: "Clinical Metadata",
                })}
              >
                <div className="mapping-grid">
                  {renderColumnMappingSelect(
                    "patientIdColumn",
                    intl.formatMessage({
                      id: "pathology.manifest.column.patientId",
                      defaultMessage: "Patient ID",
                    }),
                  )}
                  {renderColumnMappingSelect(
                    "requestingClinicianColumn",
                    intl.formatMessage({
                      id: "pathology.manifest.column.requestingClinician",
                      defaultMessage: "Requesting Clinician",
                    }),
                  )}
                  {renderColumnMappingSelect(
                    "clinicalDetailsColumn",
                    intl.formatMessage({
                      id: "pathology.manifest.column.clinicalDetails",
                      defaultMessage: "Clinical Details",
                    }),
                  )}
                </div>
              </AccordionItem>

              {/* Research Metadata */}
              <AccordionItem
                title={intl.formatMessage({
                  id: "pathology.manifest.section.research",
                  defaultMessage: "Research Metadata",
                })}
              >
                <div className="mapping-grid">
                  {renderColumnMappingSelect(
                    "studyIdColumn",
                    intl.formatMessage({
                      id: "pathology.manifest.column.studyId",
                      defaultMessage: "Study ID",
                    }),
                  )}
                  {renderColumnMappingSelect(
                    "piNameColumn",
                    intl.formatMessage({
                      id: "pathology.manifest.column.piName",
                      defaultMessage: "PI Name",
                    }),
                  )}
                  {renderColumnMappingSelect(
                    "participantAnimalIdColumn",
                    intl.formatMessage({
                      id: "pathology.manifest.column.participantAnimalId",
                      defaultMessage: "Participant/Animal ID",
                    }),
                  )}
                  {renderColumnMappingSelect(
                    "ethicalApprovalRefColumn",
                    intl.formatMessage({
                      id: "pathology.manifest.column.ethicalApprovalRef",
                      defaultMessage: "Ethical Approval Reference",
                    }),
                  )}
                </div>
              </AccordionItem>

              {/* Notes */}
              <AccordionItem
                title={intl.formatMessage({
                  id: "pathology.manifest.section.notes",
                  defaultMessage: "Notes",
                })}
              >
                <div className="mapping-grid">
                  {renderColumnMappingSelect(
                    "notesColumn",
                    intl.formatMessage({
                      id: "pathology.manifest.column.notes",
                      defaultMessage: "Notes",
                    }),
                  )}
                </div>
              </AccordionItem>
            </Accordion>

            {isPreviewLoading && (
              <Loading
                withOverlay={false}
                description={intl.formatMessage({
                  id: "notebook.manifest.loading",
                  defaultMessage: "Loading preview...",
                })}
              />
            )}

            {previewErrors.length > 0 && (
              <InlineNotification
                kind="error"
                title={intl.formatMessage({
                  id: "notebook.manifest.validationErrors",
                  defaultMessage: "Error",
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
            {previewErrors.length > 0 && (
              <InlineNotification
                kind="error"
                title={intl.formatMessage({
                  id: "notebook.manifest.validationErrors",
                  defaultMessage: "Validation Errors",
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

            {previewData && previewErrors.length === 0 && (
              <>
                <div className="preview-summary">
                  <Tag type="blue">
                    <FormattedMessage
                      id="notebook.manifest.preview.rows"
                      defaultMessage="{count} rows"
                      values={{ count: previewData.totalRows }}
                    />
                  </Tag>
                  <Tag type="green">
                    <FormattedMessage
                      id="notebook.manifest.preview.samples"
                      defaultMessage="{count} samples to create"
                      values={{ count: previewData.totalSamples }}
                    />
                  </Tag>
                </div>

                <DataTable
                  rows={previewData.rows.map((row, idx) => ({
                    id: String(idx),
                    ...row,
                  }))}
                  headers={[
                    {
                      key: "groupId",
                      header: intl.formatMessage({
                        id: "pathology.manifest.column.groupId",
                        defaultMessage: "Group ID",
                      }),
                    },
                    {
                      key: "sampleType",
                      header: intl.formatMessage({
                        id: "pathology.manifest.column.sampleType",
                        defaultMessage: "Sample Type",
                      }),
                    },
                    {
                      key: "numOfSamples",
                      header: intl.formatMessage({
                        id: "pathology.manifest.column.numOfSamples",
                        defaultMessage: "# Samples",
                      }),
                    },
                    {
                      key: "sampleCategory",
                      header: intl.formatMessage({
                        id: "pathology.manifest.column.sampleCategory",
                        defaultMessage: "Category",
                      }),
                    },
                    {
                      key: "sourceFacility",
                      header: intl.formatMessage({
                        id: "pathology.manifest.column.sourceFacility",
                        defaultMessage: "Facility",
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
                id="pathology.manifest.success.message"
                defaultMessage="Pathology samples have been created and linked to the notebook entry."
              />
            </p>
          </div>
        )}
      </div>
    </Modal>
  );
}

export default PathologyManifestImportModal;

import React, { useState, useRef, useContext } from "react";
import {
  Modal,
  Button,
  FileUploader,
  Grid,
  Column,
  ProgressBar,
  InlineNotification,
  Select,
  SelectItem,
  TextInput,
  Checkbox,
  Loading,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../../layout/Layout";
import config from "../../../config.json";
import "./BioanalyticalManifestImportModal.css";

/**
 * BioanalyticalManifestImportModal - CSV manifest import interface for bioanalytical samples.
 *
 * Features:
 * - File upload (CSV format)
 * - Column mapping configuration
 * - Preview before import
 * - Error display and handling
 * - Progress tracking
 * - Pagination for large result sets
 *
 * Required CSV columns:
 * - Sample ID, Sample Type, Source Origin, Requested Tests, Date/Time of Receipt, Receiving Personnel
 *
 * Optional CSV columns:
 * - Project/Study Association, Storage Condition, Sample Volume, Transport Temperature, Verification Status, Notes
 *
 * @param {Object} props
 * @param {boolean} props.isOpen - Modal open state
 * @param {function} props.onClose - Callback to close modal
 * @param {number} props.entryId - Notebook entry ID for import
 * @param {function} props.onSuccess - Callback after successful import
 */
function BioanalyticalManifestImportModal({
  isOpen,
  onClose,
  entryId,
  onSuccess,
}) {
  const intl = useIntl();
  const { setNotificationVisible } = useContext(NotificationContext);

  // File upload state
  const [files, setFiles] = useState([]);
  const fileInputRef = useRef(null);

  // Column mapping state
  const [columnMapping, setColumnMapping] = useState({
    uniqueSampleIdColumn: "Sample ID",
    sampleTypeColumn: "Sample Type",
    sourceOriginColumn: "Source Origin",
    requestedTestsColumn: "Requested Tests",
    dateTimeOfReceiptColumn: "Date/Time of Receipt",
    receivingPersonnelColumn: "Receiving Personnel",
    projectStudyAssociationColumn: "Project/Study",
    storageConditionPriorColumn: "Storage Condition",
    sampleVolumeColumn: "Sample Volume",
    transportTemperatureColumn: "Transport Temperature",
    manifestVerificationStatusColumn: "Verification Status",
    notesColumn: "Notes",
  });

  // Import state
  const [step, setStep] = useState("upload"); // upload, mapping, preview, importing, results
  const [isLoadingPreview, setIsLoadingPreview] = useState(false);
  const [isImporting, setIsImporting] = useState(false);
  const [importProgress, setImportProgress] = useState(0);

  // Preview and results state
  const [previewData, setPreviewData] = useState(null);
  const [importResults, setImportResults] = useState(null);
  const [errors, setErrors] = useState([]);
  const [errorMessage, setErrorMessage] = useState("");

  // Pagination state
  const [previewPage, setPreviewPage] = useState(0);
  const previewPageSize = 10;

  const handleFileChange = (event) => {
    const uploadedFiles = event.target.files;
    if (uploadedFiles && uploadedFiles.length > 0) {
      setFiles(Array.from(uploadedFiles));
      setErrors([]);
      setErrorMessage("");
    }
  };

  const handleColumnMappingChange = (field, value) => {
    setColumnMapping((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handlePreview = async () => {
    if (!files || files.length === 0) {
      setErrorMessage("Please select a CSV file to preview");
      return;
    }

    if (!entryId) {
      setErrorMessage("Entry ID is required");
      return;
    }

    setIsLoadingPreview(true);
    setErrors([]);
    setErrorMessage("");

    try {
      const formData = new FormData();
      formData.append("file", files[0]);
      formData.append("mapping", JSON.stringify(columnMapping));

      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/bioanalytical/entry/${entryId}/samples/preview-manifest`,
        {
          method: "POST",
          credentials: "include",
          headers: {
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          body: formData,
        },
      );

      const data = await response.json();

      if (!response.ok) {
        setErrorMessage(data.error || "Failed to preview manifest");
        if (data.errors) {
          setErrors(data.errors);
        }
        return;
      }

      setPreviewData(data);
      setPreviewPage(0);
      setStep("preview");
    } catch (error) {
      console.error("Preview error:", error);
      setErrorMessage(`Preview failed: ${error.message}`);
    } finally {
      setIsLoadingPreview(false);
    }
  };

  const handleImport = async () => {
    if (!files || files.length === 0) {
      setErrorMessage("Please select a CSV file to import");
      return;
    }

    if (!entryId) {
      setErrorMessage("Entry ID is required");
      return;
    }

    setIsImporting(true);
    setImportProgress(0);
    setErrors([]);
    setErrorMessage("");

    try {
      // Simulate progress updates
      const progressInterval = setInterval(() => {
        setImportProgress((prev) => {
          if (prev >= 90) {
            clearInterval(progressInterval);
            return 90;
          }
          return prev + Math.random() * 20;
        });
      }, 300);

      const formData = new FormData();
      formData.append("file", files[0]);
      formData.append("mapping", JSON.stringify(columnMapping));

      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/bioanalytical/entry/${entryId}/samples/import-manifest`,
        {
          method: "POST",
          credentials: "include",
          headers: {
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          body: formData,
        },
      );

      clearInterval(progressInterval);
      setImportProgress(100);

      const data = await response.json();

      if (!response.ok) {
        setErrorMessage(data.error || "Failed to import manifest");
        if (data.errors) {
          setErrors(data.errors);
        }
        return;
      }

      setImportResults(data);
      setStep("results");

      // Notify parent component of success
      if (onSuccess) {
        setTimeout(() => {
          onSuccess(data);
        }, 1000);
      }
    } catch (error) {
      console.error("Import error:", error);
      setErrorMessage(`Import failed: ${error.message}`);
    } finally {
      setIsImporting(false);
    }
  };

  const handleReset = () => {
    setFiles([]);
    setStep("upload");
    setPreviewData(null);
    setImportResults(null);
    setErrors([]);
    setErrorMessage("");
    setImportProgress(0);
  };

  const handleClose = () => {
    handleReset();
    onClose();
  };

  const getPaginatedRows = () => {
    if (!previewData || !previewData.rows) {
      return [];
    }
    const startIdx = previewPage * previewPageSize;
    return previewData.rows.slice(startIdx, startIdx + previewPageSize);
  };

  const getTotalPages = () => {
    if (!previewData || !previewData.rows) {
      return 0;
    }
    return Math.ceil(previewData.rows.length / previewPageSize);
  };

  return (
    <Modal
      isOpen={isOpen}
      onRequestClose={handleClose}
      modalHeading={
        <FormattedMessage
          id="notebook.bioanalytical.manifest.import.title"
          defaultMessage="Import Sample Manifest"
        />
      }
      primaryButtonText={
        step === "upload" || step === "mapping"
          ? intl.formatMessage({
              id: "notebook.bioanalytical.manifest.preview",
              defaultMessage: "Preview",
            })
          : step === "preview"
            ? intl.formatMessage({
                id: "notebook.bioanalytical.manifest.import",
                defaultMessage: "Import",
              })
            : intl.formatMessage({
                id: "notebook.bioanalytical.manifest.close",
                defaultMessage: "Close",
              })
      }
      secondaryButtonText={intl.formatMessage({
        id: "notebook.bioanalytical.manifest.cancel",
        defaultMessage: "Cancel",
      })}
      onRequestSubmit={
        step === "upload" || step === "mapping"
          ? handlePreview
          : step === "preview"
            ? handleImport
            : handleClose
      }
      size="lg"
    >
      <div className="manifest-import-container">
        {/* Step 1: File Upload */}
        {(step === "upload" || step === "mapping") && (
          <div className="step-content">
            <h3>
              <FormattedMessage
                id="notebook.bioanalytical.manifest.step1.title"
                defaultMessage="Step 1: Select CSV File"
              />
            </h3>

            <FileUploader
              accept={[".csv"]}
              buttonKind="primary"
              buttonLabel={intl.formatMessage({
                id: "notebook.bioanalytical.manifest.selectFile",
                defaultMessage: "Select File",
              })}
              filenameStatus="edit"
              iconDescription={intl.formatMessage({
                id: "notebook.bioanalytical.manifest.deleteFile",
                defaultMessage: "Delete File",
              })}
              labelDescription={intl.formatMessage({
                id: "notebook.bioanalytical.manifest.fileDescription",
                defaultMessage: "CSV file (comma-separated values)",
              })}
              labelTitle={intl.formatMessage({
                id: "notebook.bioanalytical.manifest.uploadLabel",
                defaultMessage: "Upload CSV manifest file",
              })}
              multiple={false}
              onChange={handleFileChange}
              ref={fileInputRef}
            />

            {files && files.length > 0 && (
              <div style={{ marginTop: "1rem" }}>
                <InlineNotification
                  kind="success"
                  title={intl.formatMessage({
                    id: "notebook.bioanalytical.manifest.fileSelected",
                    defaultMessage: "File Selected",
                  })}
                  subtitle={files[0].name}
                  lowContrast
                />
              </div>
            )}

            {/* Column Mapping */}
            {files && files.length > 0 && step === "mapping" && (
              <div style={{ marginTop: "2rem" }}>
                <h3>
                  <FormattedMessage
                    id="notebook.bioanalytical.manifest.step2.title"
                    defaultMessage="Step 2: Configure Column Mapping"
                  />
                </h3>

                <p>
                  <FormattedMessage
                    id="notebook.bioanalytical.manifest.columnMappingHelp"
                    defaultMessage="Specify which CSV columns correspond to sample data fields. Required fields are marked with *."
                  />
                </p>

                <Grid>
                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      labelText={
                        <span>
                          <FormattedMessage
                            id="notebook.bioanalytical.manifest.column.sampleId"
                            defaultMessage="Sample ID *"
                          />
                        </span>
                      }
                      value={columnMapping.uniqueSampleIdColumn}
                      onChange={(e) =>
                        handleColumnMappingChange(
                          "uniqueSampleIdColumn",
                          e.target.value,
                        )
                      }
                      disabled
                      helperText={intl.formatMessage({
                        id: "notebook.bioanalytical.manifest.required",
                        defaultMessage: "Required field",
                      })}
                    />
                  </Column>

                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      labelText={
                        <span>
                          <FormattedMessage
                            id="notebook.bioanalytical.manifest.column.sampleType"
                            defaultMessage="Sample Type *"
                          />
                        </span>
                      }
                      value={columnMapping.sampleTypeColumn}
                      onChange={(e) =>
                        handleColumnMappingChange(
                          "sampleTypeColumn",
                          e.target.value,
                        )
                      }
                      disabled
                      helperText={intl.formatMessage({
                        id: "notebook.bioanalytical.manifest.required",
                        defaultMessage: "Required field",
                      })}
                    />
                  </Column>

                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      labelText={
                        <span>
                          <FormattedMessage
                            id="notebook.bioanalytical.manifest.column.sourceOrigin"
                            defaultMessage="Source Origin *"
                          />
                        </span>
                      }
                      value={columnMapping.sourceOriginColumn}
                      onChange={(e) =>
                        handleColumnMappingChange(
                          "sourceOriginColumn",
                          e.target.value,
                        )
                      }
                      disabled
                      helperText={intl.formatMessage({
                        id: "notebook.bioanalytical.manifest.required",
                        defaultMessage: "Required field",
                      })}
                    />
                  </Column>

                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      labelText={
                        <span>
                          <FormattedMessage
                            id="notebook.bioanalytical.manifest.column.requestedTests"
                            defaultMessage="Requested Tests *"
                          />
                        </span>
                      }
                      value={columnMapping.requestedTestsColumn}
                      onChange={(e) =>
                        handleColumnMappingChange(
                          "requestedTestsColumn",
                          e.target.value,
                        )
                      }
                      disabled
                      helperText={intl.formatMessage({
                        id: "notebook.bioanalytical.manifest.required",
                        defaultMessage: "Required field",
                      })}
                    />
                  </Column>

                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      labelText={
                        <span>
                          <FormattedMessage
                            id="notebook.bioanalytical.manifest.column.dateTime"
                            defaultMessage="Date/Time of Receipt *"
                          />
                        </span>
                      }
                      value={columnMapping.dateTimeOfReceiptColumn}
                      onChange={(e) =>
                        handleColumnMappingChange(
                          "dateTimeOfReceiptColumn",
                          e.target.value,
                        )
                      }
                      disabled
                      helperText={intl.formatMessage({
                        id: "notebook.bioanalytical.manifest.required",
                        defaultMessage: "Required field",
                      })}
                    />
                  </Column>

                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      labelText={
                        <span>
                          <FormattedMessage
                            id="notebook.bioanalytical.manifest.column.personnel"
                            defaultMessage="Receiving Personnel *"
                          />
                        </span>
                      }
                      value={columnMapping.receivingPersonnelColumn}
                      onChange={(e) =>
                        handleColumnMappingChange(
                          "receivingPersonnelColumn",
                          e.target.value,
                        )
                      }
                      disabled
                      helperText={intl.formatMessage({
                        id: "notebook.bioanalytical.manifest.required",
                        defaultMessage: "Required field",
                      })}
                    />
                  </Column>
                </Grid>

                <div style={{ marginTop: "1.5rem" }}>
                  <p style={{ fontWeight: "bold", marginBottom: "1rem" }}>
                    <FormattedMessage
                      id="notebook.bioanalytical.manifest.optionalFields"
                      defaultMessage="Optional Fields:"
                    />
                  </p>

                  <Grid>
                    <Column lg={8} md={4} sm={4}>
                      <TextInput
                        labelText={intl.formatMessage({
                          id: "notebook.bioanalytical.manifest.column.project",
                          defaultMessage: "Project/Study Association",
                        })}
                        value={columnMapping.projectStudyAssociationColumn}
                        onChange={(e) =>
                          handleColumnMappingChange(
                            "projectStudyAssociationColumn",
                            e.target.value,
                          )
                        }
                      />
                    </Column>

                    <Column lg={8} md={4} sm={4}>
                      <TextInput
                        labelText={intl.formatMessage({
                          id: "notebook.bioanalytical.manifest.column.storage",
                          defaultMessage: "Storage Condition",
                        })}
                        value={columnMapping.storageConditionPriorColumn}
                        onChange={(e) =>
                          handleColumnMappingChange(
                            "storageConditionPriorColumn",
                            e.target.value,
                          )
                        }
                      />
                    </Column>

                    <Column lg={8} md={4} sm={4}>
                      <TextInput
                        labelText={intl.formatMessage({
                          id: "notebook.bioanalytical.manifest.column.volume",
                          defaultMessage: "Sample Volume",
                        })}
                        value={columnMapping.sampleVolumeColumn}
                        onChange={(e) =>
                          handleColumnMappingChange(
                            "sampleVolumeColumn",
                            e.target.value,
                          )
                        }
                      />
                    </Column>
                  </Grid>
                </div>
              </div>
            )}

            {errorMessage && (
              <div style={{ marginTop: "1rem" }}>
                <InlineNotification
                  kind="error"
                  title={intl.formatMessage({
                    id: "notebook.bioanalytical.manifest.error",
                    defaultMessage: "Error",
                  })}
                  subtitle={errorMessage}
                  lowContrast
                />
              </div>
            )}
          </div>
        )}

        {/* Step 2: Preview */}
        {step === "preview" && (
          <div className="step-content">
            <h3>
              <FormattedMessage
                id="notebook.bioanalytical.manifest.step3.title"
                defaultMessage="Step 3: Preview Data"
              />
            </h3>

            {previewData && (
              <div>
                <div style={{ marginBottom: "1rem" }}>
                  <InlineNotification
                    kind="info"
                    title={intl.formatMessage({
                      id: "notebook.bioanalytical.manifest.previewSummary",
                      defaultMessage: "Preview Summary",
                    })}
                    subtitle={intl.formatMessage(
                      {
                        id: "notebook.bioanalytical.manifest.previewStats",
                        defaultMessage:
                          "Total rows: {totalRows}, Valid rows: {validRows}, Errors: {errorCount}",
                      },
                      {
                        totalRows: previewData.totalRows,
                        validRows: previewData.validRows,
                        errorCount: previewData.errors?.length || 0,
                      },
                    )}
                    lowContrast
                  />
                </div>

                {previewData.errors && previewData.errors.length > 0 && (
                  <div style={{ marginBottom: "1.5rem" }}>
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.manifest.validationErrors"
                        defaultMessage="Validation Errors"
                      />
                    </h4>
                    <Table>
                      <TableHead>
                        <TableRow>
                          <TableHeader>
                            <FormattedMessage
                              id="notebook.bioanalytical.manifest.row"
                              defaultMessage="Row"
                            />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage
                              id="notebook.bioanalytical.manifest.column"
                              defaultMessage="Column"
                            />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage
                              id="notebook.bioanalytical.manifest.message"
                              defaultMessage="Message"
                            />
                          </TableHeader>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {previewData.errors.slice(0, 5).map((error, idx) => (
                          <TableRow key={idx}>
                            <TableCell>{error.rowNumber}</TableCell>
                            <TableCell>{error.column}</TableCell>
                            <TableCell>{error.message}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                    {previewData.errors.length > 5 && (
                      <p style={{ fontSize: "0.875rem", marginTop: "0.5rem" }}>
                        {intl.formatMessage(
                          {
                            id: "notebook.bioanalytical.manifest.moreErrors",
                            defaultMessage: "...and {count} more errors",
                          },
                          { count: previewData.errors.length - 5 },
                        )}
                      </p>
                    )}
                  </div>
                )}

                {previewData.rows && previewData.rows.length > 0 && (
                  <div>
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.manifest.samplePreview"
                        defaultMessage="Sample Data Preview"
                      />
                    </h4>
                    <Table>
                      <TableHead>
                        <TableRow>
                          <TableHeader>
                            <FormattedMessage
                              id="notebook.bioanalytical.manifest.sampleId"
                              defaultMessage="Sample ID"
                            />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage
                              id="notebook.bioanalytical.manifest.type"
                              defaultMessage="Type"
                            />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage
                              id="notebook.bioanalytical.manifest.origin"
                              defaultMessage="Origin"
                            />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage
                              id="notebook.bioanalytical.manifest.tests"
                              defaultMessage="Tests"
                            />
                          </TableHeader>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {getPaginatedRows().map((row, idx) => (
                          <TableRow key={idx}>
                            <TableCell>{row.uniqueSampleId}</TableCell>
                            <TableCell>{row.sampleType}</TableCell>
                            <TableCell>{row.sourceOrigin}</TableCell>
                            <TableCell>{row.requestedTests}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>

                    {getTotalPages() > 1 && (
                      <div style={{ marginTop: "1rem", textAlign: "center" }}>
                        <Button
                          kind="ghost"
                          size="sm"
                          onClick={() =>
                            setPreviewPage(Math.max(0, previewPage - 1))
                          }
                          disabled={previewPage === 0}
                        >
                          <FormattedMessage
                            id="notebook.bioanalytical.manifest.previous"
                            defaultMessage="Previous"
                          />
                        </Button>
                        <span style={{ margin: "0 1rem" }}>
                          {previewPage + 1} / {getTotalPages()}
                        </span>
                        <Button
                          kind="ghost"
                          size="sm"
                          onClick={() =>
                            setPreviewPage(
                              Math.min(getTotalPages() - 1, previewPage + 1),
                            )
                          }
                          disabled={previewPage >= getTotalPages() - 1}
                        >
                          <FormattedMessage
                            id="notebook.bioanalytical.manifest.next"
                            defaultMessage="Next"
                          />
                        </Button>
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}

            {errorMessage && (
              <div style={{ marginTop: "1rem" }}>
                <InlineNotification
                  kind="error"
                  title={intl.formatMessage({
                    id: "notebook.bioanalytical.manifest.error",
                    defaultMessage: "Error",
                  })}
                  subtitle={errorMessage}
                  lowContrast
                />
              </div>
            )}
          </div>
        )}

        {/* Step 3: Importing */}
        {step === "importing" && (
          <div className="step-content" style={{ textAlign: "center" }}>
            <Loading description="Importing samples..." />
            <div style={{ marginTop: "2rem" }}>
              <ProgressBar
                label={intl.formatMessage({
                  id: "notebook.bioanalytical.manifest.importProgress",
                  defaultMessage: "Import Progress",
                })}
                value={importProgress}
                max={100}
              />
            </div>
          </div>
        )}

        {/* Step 4: Results */}
        {step === "results" && (
          <div className="step-content">
            <h3>
              <FormattedMessage
                id="notebook.bioanalytical.manifest.step4.title"
                defaultMessage="Import Complete"
              />
            </h3>

            {importResults && (
              <div>
                {importResults.success && (
                  <div style={{ marginBottom: "1rem" }}>
                    <InlineNotification
                      kind="success"
                      title={intl.formatMessage({
                        id: "notebook.bioanalytical.manifest.importSuccess",
                        defaultMessage: "Import Successful",
                      })}
                      subtitle={intl.formatMessage(
                        {
                          id: "notebook.bioanalytical.manifest.samplesCreated",
                          defaultMessage:
                            "{count} samples created successfully",
                        },
                        { count: importResults.totalCreated },
                      )}
                      lowContrast
                    />
                  </div>
                )}

                {!importResults.success && (
                  <div style={{ marginBottom: "1rem" }}>
                    <InlineNotification
                      kind="error"
                      title={intl.formatMessage({
                        id: "notebook.bioanalytical.manifest.importFailed",
                        defaultMessage: "Import Failed",
                      })}
                      subtitle={intl.formatMessage(
                        {
                          id: "notebook.bioanalytical.manifest.samplesFailedCount",
                          defaultMessage: "{count} samples failed to create",
                        },
                        {
                          count:
                            importResults.totalRequested -
                            importResults.totalCreated,
                        },
                      )}
                      lowContrast
                    />
                  </div>
                )}

                {importResults.createdSampleIds &&
                  importResults.createdSampleIds.length > 0 && (
                    <div style={{ marginBottom: "1rem" }}>
                      <h4>
                        <FormattedMessage
                          id="notebook.bioanalytical.manifest.createdSamples"
                          defaultMessage="Created Sample IDs"
                        />
                      </h4>
                      <div
                        style={{
                          maxHeight: "200px",
                          overflow: "auto",
                          padding: "0.5rem",
                          backgroundColor: "#f4f4f4",
                          borderRadius: "4px",
                        }}
                      >
                        {importResults.createdSampleIds.map((id, idx) => (
                          <div key={idx} style={{ fontSize: "0.875rem" }}>
                            {id}
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                {importResults.errors && importResults.errors.length > 0 && (
                  <div>
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.manifest.importErrors"
                        defaultMessage="Import Errors"
                      />
                    </h4>
                    <Table>
                      <TableHead>
                        <TableRow>
                          <TableHeader>
                            <FormattedMessage
                              id="notebook.bioanalytical.manifest.row"
                              defaultMessage="Row"
                            />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage
                              id="notebook.bioanalytical.manifest.column"
                              defaultMessage="Column"
                            />
                          </TableHeader>
                          <TableHeader>
                            <FormattedMessage
                              id="notebook.bioanalytical.manifest.message"
                              defaultMessage="Message"
                            />
                          </TableHeader>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {importResults.errors.slice(0, 10).map((error, idx) => (
                          <TableRow key={idx}>
                            <TableCell>{error.rowNumber}</TableCell>
                            <TableCell>{error.column}</TableCell>
                            <TableCell>{error.message}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                    {importResults.errors.length > 10 && (
                      <p style={{ fontSize: "0.875rem", marginTop: "0.5rem" }}>
                        {intl.formatMessage(
                          {
                            id: "notebook.bioanalytical.manifest.moreErrors",
                            defaultMessage: "...and {count} more errors",
                          },
                          { count: importResults.errors.length - 10 },
                        )}
                      </p>
                    )}
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </div>
    </Modal>
  );
}

export default BioanalyticalManifestImportModal;

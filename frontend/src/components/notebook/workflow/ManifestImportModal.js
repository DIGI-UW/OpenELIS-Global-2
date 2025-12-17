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
} from "@carbon/react";
import { Upload, Warning, Checkmark } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { postToOpenElisServerFormData } from "../../utils/Utils";
import config from "../../../config.json";
import "../workflow/NotebookWorkflow.css";

/**
 * ManifestImportModal - Modal for importing samples from CSV manifest files.
 *
 * @param {Object} props
 * @param {boolean} props.open - Whether the modal is open
 * @param {function} props.onClose - Callback when modal is closed
 * @param {number} props.entryId - The notebook entry ID to import samples to
 * @param {function} props.onImportSuccess - Callback when import is successful
 */
function ManifestImportModal({ open, onClose, entryId, onImportSuccess }) {
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

  // State for column mapping
  const [csvHeaders, setCsvHeaders] = useState([]);
  const [columnMapping, setColumnMapping] = useState({
    groupIdColumn: "",
    sampleTypeColumn: "",
    collectionDateColumn: "",
    volumeColumn: "",
    numOfSamplesColumn: "",
    notesColumn: "",
    dateFormat: "yyyy-MM-dd",
  });

  // State for preview
  const [previewData, setPreviewData] = useState(null);
  const [previewErrors, setPreviewErrors] = useState([]);
  const [isPreviewLoading, setIsPreviewLoading] = useState(false);
  const [isImporting, setIsImporting] = useState(false);

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
        setStep(2);
      };
      reader.readAsText(addedFile);
    },
    [intl],
  );

  const handleRemoveFile = () => {
    setFile(null);
    setCsvHeaders([]);
    setColumnMapping({
      groupIdColumn: "",
      sampleTypeColumn: "",
      collectionDateColumn: "",
      volumeColumn: "",
      numOfSamplesColumn: "",
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
      console.error("ManifestImportModal: No file selected");
      return;
    }

    if (!entryId) {
      console.error("ManifestImportModal: No entryId provided");
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

    const endpoint = `${config.serverBaseUrl}/rest/notebook/entry/${entryId}/samples/preview-manifest`;

    try {
      console.log(
        "ManifestImportModal: Sending preview request to " + endpoint,
      );
      const response = await fetch(endpoint, {
        method: "POST",
        body: formData,
        credentials: "include",
        headers: {
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
      });

      console.log("ManifestImportModal: Response status:", response.status);
      const data = await response.json();
      console.log("ManifestImportModal: Response data:", data);

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
      console.error("ManifestImportModal: Preview error:", error);
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
      new Blob([JSON.stringify(columnMapping)], {
        type: "application/json",
      }),
    );

    const endpoint = `${config.serverBaseUrl}/rest/notebook/entry/${entryId}/samples/create-from-manifest`;

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
    handleRemoveFile();
    onClose();
  };

  const requiredColumnsMapped =
    columnMapping.groupIdColumn &&
    columnMapping.sampleTypeColumn &&
    columnMapping.numOfSamplesColumn;

  const renderColumnMappingSelect = (field, labelId, required = false) => (
    <Select
      id={`mapping-${field}`}
      labelText={
        <FormattedMessage
          id={labelId}
          defaultMessage={field.replace("Column", "")}
        />
      }
      value={columnMapping[field]}
      onChange={(e) => handleMappingChange(field, e.target.value)}
    >
      <SelectItem value="" text={intl.formatMessage({ id: "label.select" })} />
      {csvHeaders.map((header) => (
        <SelectItem key={header} value={header} text={header} />
      ))}
    </Select>
  );

  return (
    <Modal
      open={open}
      onRequestClose={handleClose}
      modalHeading={intl.formatMessage({
        id: "notebook.manifest.title",
        defaultMessage: "Import Samples from Manifest",
      })}
      primaryButtonText={
        step === 1
          ? intl.formatMessage({ id: "label.button.next" })
          : step === 2
            ? intl.formatMessage({ id: "notebook.manifest.preview" })
            : step === 3
              ? intl.formatMessage({ id: "notebook.manifest.import" })
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
        (step === 3 && previewErrors.length > 0) ||
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
                id="notebook.manifest.uploadDescription"
                defaultMessage="Upload a CSV file containing sample information. The file should include columns for group ID, sample type, and number of samples."
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

        {/* Step 2: Column Mapping */}
        {step === 2 && (
          <div className="mapping-step">
            <p className="step-description">
              <FormattedMessage
                id="notebook.manifest.mappingDescription"
                defaultMessage="Map the columns from your CSV file to the required fields. Required fields are marked with *."
              />
            </p>

            <div className="mapping-grid">
              <div className="mapping-field required">
                {renderColumnMappingSelect(
                  "groupIdColumn",
                  "notebook.manifest.column.groupId",
                  true,
                )}
                <span className="required-marker">*</span>
              </div>

              <div className="mapping-field required">
                {renderColumnMappingSelect(
                  "sampleTypeColumn",
                  "notebook.manifest.column.sampleType",
                  true,
                )}
                <span className="required-marker">*</span>
              </div>

              <div className="mapping-field required">
                {renderColumnMappingSelect(
                  "numOfSamplesColumn",
                  "notebook.manifest.column.numOfSamples",
                  true,
                )}
                <span className="required-marker">*</span>
              </div>

              <div className="mapping-field">
                {renderColumnMappingSelect(
                  "collectionDateColumn",
                  "notebook.manifest.column.collectionDate",
                )}
              </div>

              <div className="mapping-field">
                {renderColumnMappingSelect(
                  "volumeColumn",
                  "notebook.manifest.column.volume",
                )}
              </div>

              <div className="mapping-field">
                {renderColumnMappingSelect(
                  "notesColumn",
                  "notebook.manifest.column.notes",
                )}
              </div>
            </div>

            {isPreviewLoading && (
              <Loading
                withOverlay={false}
                description={intl.formatMessage({
                  id: "notebook.manifest.loading",
                })}
              />
            )}

            {/* Show errors that occur during preview fetch */}
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
                        id: "notebook.manifest.column.groupId",
                      }),
                    },
                    {
                      key: "sampleType",
                      header: intl.formatMessage({
                        id: "notebook.manifest.column.sampleType",
                      }),
                    },
                    {
                      key: "numOfSamples",
                      header: intl.formatMessage({
                        id: "notebook.manifest.column.numOfSamples",
                      }),
                    },
                    {
                      key: "collectionDate",
                      header: intl.formatMessage({
                        id: "notebook.manifest.column.collectionDate",
                      }),
                    },
                    {
                      key: "volume",
                      header: intl.formatMessage({
                        id: "notebook.manifest.column.volume",
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
                              <TableCell key={cell.id}>{cell.value}</TableCell>
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
                id="notebook.manifest.success.message"
                defaultMessage="Samples have been created and linked to the notebook entry."
              />
            </p>
          </div>
        )}
      </div>
    </Modal>
  );
}

export default ManifestImportModal;

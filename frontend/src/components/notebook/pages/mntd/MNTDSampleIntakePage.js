import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Loading,
  Modal,
  FileUploaderDropContainer,
  FileUploaderItem,
  Select,
  SelectItem,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tag,
  TextInput,
  DatePicker,
  DatePickerInput,
  TimePicker,
} from "@carbon/react";
import { Upload, Checkmark, Warning, Add, Printer } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerFormDataJson,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import config from "../../../../config.json";
import "../../workflow/NotebookWorkflow.css";

/**
 * MNTDSampleIntakePage - Page 1 of the MNTD workflow.
 * Handles sample intake with CSV import. MNTD-specific data points are hardcoded:
 * - Sample ID / Registration name
 * - Sample Source (Field, External organization, etc.)
 * - Project Name
 * - Sample Type (fetched from type_of_sample table)
 * - Collection Site/Location
 * - Collection Date & Time
 * - Collected By (person/organization)
 * - Number of Samples
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function MNTDSampleIntakePage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // State for sample types from type_of_sample table
  const [sampleTypes, setSampleTypes] = useState([]);

  // Modal state for import
  const [importModalOpen, setImportModalOpen] = useState(false);
  const [file, setFile] = useState(null);
  const [fileError, setFileError] = useState(null);
  const [csvHeaders, setCsvHeaders] = useState([]);
  const [columnMapping, setColumnMapping] = useState({
    sampleIdColumn: "",
    sampleSourceColumn: "",
    projectNameColumn: "",
    sampleTypeColumn: "",
    collectionSiteColumn: "",
    collectionDateTimeColumn: "",
    collectedByColumn: "",
    numOfSamplesColumn: "",
    dateFormat: "yyyy-MM-dd",
  });
  const [previewData, setPreviewData] = useState(null);
  const [previewErrors, setPreviewErrors] = useState([]);
  const [isPreviewLoading, setIsPreviewLoading] = useState(false);
  const [isImporting, setIsImporting] = useState(false);
  const [importStep, setImportStep] = useState(1);
  const [importResult, setImportResult] = useState(null);

  // Load sample types from type_of_sample table
  useEffect(() => {
    getFromOpenElisServer("/rest/user-sample-types", (res) => {
      setSampleTypes(res || []);
    });
  }, []);

  // Load samples for this page
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id) {
      setLoading(false);
      return;
    }

    if (String(pageData.id).startsWith("default-")) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            const transformedSamples = response.map((sample) => ({
              id: String(sample.id || sample.sampleItemId),
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || "PENDING",
              patientName: sample.patientName,
              volume: sample.volume,
              // MNTD specific fields from questionnaire responses
              sampleSource: sample.data?.sampleSource,
              projectName: sample.data?.projectName,
              collectionSite: sample.data?.collectionSite,
              collectedBy: sample.data?.collectedBy,
            }));
            setSamples(transformedSamples);
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  // Handle file upload
  const handleFileAdded = useCallback(
    (event, { addedFiles }) => {
      const addedFile = addedFiles[0];
      if (!addedFile) return;

      if (!addedFile.name.endsWith(".csv")) {
        setFileError(
          intl.formatMessage({
            id: "notebook.mntd.error.invalidFileType",
            defaultMessage: "Please upload a CSV file",
          }),
        );
        return;
      }

      setFile(addedFile);
      setFileError(null);

      const reader = new FileReader();
      reader.onload = (e) => {
        const text = e.target.result;
        const firstLine = text.split("\n")[0];
        const headers = firstLine
          .split(",")
          .map((h) => h.trim().replace(/"/g, ""));
        setCsvHeaders(headers);
        setImportStep(2);
      };
      reader.readAsText(addedFile);
    },
    [intl],
  );

  const handleRemoveFile = () => {
    setFile(null);
    setCsvHeaders([]);
    setColumnMapping({
      sampleIdColumn: "",
      sampleSourceColumn: "",
      projectNameColumn: "",
      sampleTypeColumn: "",
      collectionSiteColumn: "",
      collectionDateTimeColumn: "",
      collectedByColumn: "",
      numOfSamplesColumn: "",
      dateFormat: "yyyy-MM-dd",
    });
    setPreviewData(null);
    setPreviewErrors([]);
    setImportStep(1);
    setImportResult(null);
  };

  const handleMappingChange = (field, value) => {
    setColumnMapping((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  // Preview import
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

    postToOpenElisServerFormDataJson(
      `/rest/notebook/mntd/entry/${entryId}/samples/preview-manifest`,
      formData,
      (response) => {
        setIsPreviewLoading(false);
        if (response && response.rows) {
          setPreviewData(response);
          if (response.errors && response.errors.length > 0) {
            setPreviewErrors(response.errors);
          }
          setImportStep(3);
        } else if (response && response.error) {
          setPreviewErrors([
            { rowNumber: 0, column: "system", message: response.error },
          ]);
        }
      },
    );
  };

  // Execute import
  const handleImport = async () => {
    if (!file || !entryId) return;

    setIsImporting(true);

    const formData = new FormData();
    formData.append("file", file);
    formData.append(
      "mapping",
      new Blob([JSON.stringify(columnMapping)], { type: "application/json" }),
    );

    postToOpenElisServerFormDataJson(
      `/rest/notebook/mntd/entry/${entryId}/samples/create-from-manifest`,
      formData,
      (response) => {
        setIsImporting(false);
        if (response && response.success) {
          setImportResult(response);
          setImportStep(4);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setPreviewErrors([
            {
              rowNumber: 0,
              column: "system",
              message: response?.error || "Import failed",
            },
          ]);
        }
      },
    );
  };

  // Close modal and reset
  const handleCloseModal = () => {
    setImportModalOpen(false);
    handleRemoveFile();
  };

  // Handle bulk mark as registered
  const handleBulkMarkRegistered = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    const hasRealPageId =
      pageData?.id && !String(pageData.id).startsWith("default-");
    if (!hasRealPageId) {
      setError("Cannot update samples: Page not properly initialized.");
      return;
    }

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
      }),
      (status) => {
        if (status === 200) {
          loadPageSamples();
          setSelectedSampleIds([]);
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError("Failed to update sample status.");
        }
      },
    );
  }, [selectedSampleIds, pageData?.id, loadPageSamples, onProgressUpdate]);

  // Print barcode
  const handlePrintBarcode = (accessionNumber) => {
    const barcodesPdf =
      config.serverBaseUrl +
      `/LabelMakerServlet?labNo=${encodeURIComponent(accessionNumber)}&type=order&quantity=1`;
    window.open(barcodesPdf);
  };

  // Calculate stats
  const registeredCount = samples.filter(
    (s) => s.status === "COMPLETED",
  ).length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;

  // Get column mapping fields for MNTD
  const mappingFields = [
    {
      key: "sampleIdColumn",
      label: "Sample ID / Registration name",
      required: true,
    },
    { key: "sampleSourceColumn", label: "Sample Source", required: true },
    { key: "projectNameColumn", label: "Project Name", required: true },
    { key: "sampleTypeColumn", label: "Sample Type", required: true },
    {
      key: "collectionSiteColumn",
      label: "Collection Site/Location",
      required: true,
    },
    {
      key: "collectionDateTimeColumn",
      label: "Collection Date & Time",
      required: true,
    },
    { key: "collectedByColumn", label: "Collected By", required: true },
    { key: "numOfSamplesColumn", label: "Number of Samples", required: true },
  ];

  return (
    <div className="mntd-sample-intake-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.mntd.sampleIntake.title"
            defaultMessage="Sample Intake / Sample Creation"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.mntd.sampleIntake.description"
            defaultMessage="Capture all incoming samples as they arrive at reception. Import samples via CSV with additional data captured through questionnaire fields. System generates unique IDs and barcodes."
          />
        </p>
      </div>

      {/* Progress Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.mntd.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.mntd.registered"
                  defaultMessage="Registered"
                />
              </span>
              <span className="progress-value">{registeredCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.mntd.awaitingReception"
                  defaultMessage="Awaiting Reception"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Upload}
          onClick={() => setImportModalOpen(true)}
        >
          <FormattedMessage
            id="notebook.page.mntd.importManifest"
            defaultMessage="Import from Manifest"
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={Checkmark}
            onClick={handleBulkMarkRegistered}
          >
            <FormattedMessage
              id="notebook.page.mntd.markRegistered"
              defaultMessage="Mark as Registered ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        )}
      </div>

      {/* Error Display */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton
          lowContrast
        />
      )}

      {/* Sample Grid */}
      <div className="sample-grid-container">
        <SampleGrid
          samples={samples}
          selectedIds={selectedSampleIds}
          onSelectionChange={setSelectedSampleIds}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
          columns={[
            { key: "externalId", header: "Sample ID" },
            { key: "sampleType", header: "Sample Type" },
            { key: "sampleSource", header: "Source" },
            { key: "projectName", header: "Project" },
            { key: "collectionSite", header: "Collection Site" },
            { key: "collectionDate", header: "Collection Date" },
            { key: "collectedBy", header: "Collected By" },
            { key: "status", header: "Status" },
          ]}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.mntd.sampleIntake.empty"
              defaultMessage="No samples have been added yet. Use 'Import from Manifest' to create samples from a CSV file."
            />
          </p>
        </div>
      )}

      {/* Import Modal */}
      <Modal
        open={importModalOpen}
        onRequestClose={handleCloseModal}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.import.title",
          defaultMessage: "Import MNTD Samples from Manifest",
        })}
        primaryButtonText={
          importStep === 2
            ? intl.formatMessage({
                id: "label.preview",
                defaultMessage: "Preview",
              })
            : importStep === 3
              ? intl.formatMessage({
                  id: "label.import",
                  defaultMessage: "Import",
                })
              : intl.formatMessage({
                  id: "label.close",
                  defaultMessage: "Close",
                })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={() => {
          if (importStep === 2) handlePreview();
          else if (importStep === 3) handleImport();
          else handleCloseModal();
        }}
        onSecondarySubmit={handleCloseModal}
        size="lg"
        primaryButtonDisabled={
          (importStep === 2 && !columnMapping.sampleIdColumn) ||
          (importStep === 3 && previewErrors.length > 0) ||
          isPreviewLoading ||
          isImporting
        }
      >
        {/* Step 1: Upload File */}
        {importStep === 1 && (
          <div className="import-step">
            <p className="step-description">
              <FormattedMessage
                id="notebook.mntd.import.step1"
                defaultMessage="Upload a CSV file containing sample data. The file should have columns for Sample ID, Sample Source, Project Name, Sample Type, Collection Site, Collection Date/Time, Collected By, and Number of Samples."
              />
            </p>
            <FileUploaderDropContainer
              accept={[".csv"]}
              labelText={intl.formatMessage({
                id: "notebook.mntd.import.dropzone",
                defaultMessage:
                  "Drag and drop a CSV file here or click to upload",
              })}
              onAddFiles={handleFileAdded}
            />
            {fileError && (
              <InlineNotification
                kind="error"
                title={fileError}
                lowContrast
                hideCloseButton
              />
            )}
          </div>
        )}

        {/* Step 2: Map Columns */}
        {importStep === 2 && (
          <div className="import-step">
            <p className="step-description">
              <FormattedMessage
                id="notebook.mntd.import.step2"
                defaultMessage="Map the CSV columns to the required fields. Sample types will be validated against the system's sample type list."
              />
            </p>
            {file && (
              <FileUploaderItem
                name={file.name}
                status="edit"
                onDelete={handleRemoveFile}
              />
            )}
            <Grid fullWidth>
              {mappingFields.map((field) => (
                <Column lg={8} md={4} sm={4} key={field.key}>
                  <Select
                    id={field.key}
                    labelText={`${field.label}${field.required ? " *" : ""}`}
                    value={columnMapping[field.key]}
                    onChange={(e) =>
                      handleMappingChange(field.key, e.target.value)
                    }
                  >
                    <SelectItem value="" text="Select column..." />
                    {csvHeaders.map((header) => (
                      <SelectItem key={header} value={header} text={header} />
                    ))}
                  </Select>
                </Column>
              ))}
              <Column lg={8} md={4} sm={4}>
                <Select
                  id="dateFormat"
                  labelText="Date Format"
                  value={columnMapping.dateFormat}
                  onChange={(e) =>
                    handleMappingChange("dateFormat", e.target.value)
                  }
                >
                  <SelectItem value="yyyy-MM-dd" text="yyyy-MM-dd" />
                  <SelectItem value="dd/MM/yyyy" text="dd/MM/yyyy" />
                  <SelectItem value="MM/dd/yyyy" text="MM/dd/yyyy" />
                  <SelectItem
                    value="yyyy-MM-dd HH:mm"
                    text="yyyy-MM-dd HH:mm"
                  />
                </Select>
              </Column>
            </Grid>
            <div style={{ marginTop: "1rem" }}>
              <p>
                <strong>Available Sample Types:</strong>{" "}
                {sampleTypes.map((st) => st.value).join(", ")}
              </p>
            </div>
          </div>
        )}

        {/* Step 3: Preview */}
        {importStep === 3 && (
          <div className="import-step">
            {isPreviewLoading ? (
              <Loading description="Loading preview..." />
            ) : (
              <>
                {previewData && (
                  <div>
                    {/* Summary Statistics */}
                    <div
                      style={{
                        display: "flex",
                        gap: "1rem",
                        marginBottom: "1rem",
                        flexWrap: "wrap",
                      }}
                    >
                      <Tag type="blue">Total Rows: {previewData.totalRows}</Tag>
                      <Tag type="green">Valid: {previewData.validRows}</Tag>
                      {previewData.totalRows - previewData.validRows > 0 && (
                        <Tag type="red">
                          Invalid:{" "}
                          {previewData.totalRows - previewData.validRows}
                        </Tag>
                      )}
                      <Tag type="purple">
                        Samples to Create: {previewData.totalSamplesToCreate}
                      </Tag>
                    </div>

                    {/* Validation Errors Section */}
                    {previewErrors.length > 0 && (
                      <div style={{ marginBottom: "1.5rem" }}>
                        <InlineNotification
                          kind="error"
                          title={intl.formatMessage({
                            id: "notebook.mntd.import.validationErrors",
                            defaultMessage: "Validation errors found",
                          })}
                          subtitle={intl.formatMessage(
                            {
                              id: "notebook.mntd.import.fixErrors",
                              defaultMessage:
                                "{count} row(s) have invalid sample types. Please fix the CSV or add missing sample types to the system.",
                            },
                            { count: previewErrors.length },
                          )}
                          lowContrast
                          hideCloseButton
                        />
                        <div
                          style={{
                            marginTop: "0.5rem",
                            maxHeight: "150px",
                            overflowY: "auto",
                            border: "1px solid #da1e28",
                            borderRadius: "4px",
                            padding: "0.5rem",
                            backgroundColor: "#fff1f1",
                          }}
                        >
                          <table
                            style={{ width: "100%", fontSize: "0.875rem" }}
                          >
                            <thead>
                              <tr
                                style={{
                                  borderBottom: "1px solid #da1e28",
                                  textAlign: "left",
                                }}
                              >
                                <th style={{ padding: "0.25rem" }}>Row</th>
                                <th style={{ padding: "0.25rem" }}>Column</th>
                                <th style={{ padding: "0.25rem" }}>Error</th>
                              </tr>
                            </thead>
                            <tbody>
                              {previewErrors.map((error, idx) => (
                                <tr
                                  key={idx}
                                  style={{ borderBottom: "1px solid #ffcdd2" }}
                                >
                                  <td style={{ padding: "0.25rem" }}>
                                    {error.rowNumber}
                                  </td>
                                  <td style={{ padding: "0.25rem" }}>
                                    {error.column}
                                  </td>
                                  <td style={{ padding: "0.25rem" }}>
                                    {error.message}
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      </div>
                    )}

                    {/* Valid Rows Preview */}
                    {previewData.rows && previewData.rows.length > 0 && (
                      <div>
                        <h5 style={{ marginBottom: "0.5rem" }}>
                          <FormattedMessage
                            id="notebook.mntd.import.previewRows"
                            defaultMessage="Preview (first 10 rows)"
                          />
                        </h5>
                        <DataTable
                          rows={previewData.rows
                            .slice(0, 10)
                            .map((row, idx) => {
                              // Check if this row has an error
                              const hasError = previewErrors.some(
                                (e) => e.rowNumber === row.rowNumber,
                              );
                              return {
                                id: String(idx),
                                rowNumber: row.rowNumber,
                                sampleId: row.sampleId,
                                sampleType: row.sampleType,
                                sampleSource: row.sampleSource,
                                projectName: row.projectName,
                                numOfSamples: row.numOfSamples,
                                status: hasError ? "Invalid" : "Valid",
                              };
                            })}
                          headers={[
                            { key: "rowNumber", header: "Row #" },
                            { key: "sampleId", header: "Sample ID" },
                            { key: "sampleType", header: "Sample Type" },
                            { key: "sampleSource", header: "Source" },
                            { key: "projectName", header: "Project" },
                            { key: "numOfSamples", header: "# Samples" },
                            { key: "status", header: "Status" },
                          ]}
                        >
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
                                  const isInvalid =
                                    row.cells.find(
                                      (c) => c.info.header === "status",
                                    )?.value === "Invalid";
                                  return (
                                    <TableRow
                                      key={row.id}
                                      {...getRowProps({ row })}
                                      style={
                                        isInvalid
                                          ? { backgroundColor: "#fff1f1" }
                                          : {}
                                      }
                                    >
                                      {row.cells.map((cell) => (
                                        <TableCell key={cell.id}>
                                          {cell.info.header === "status" ? (
                                            <Tag
                                              type={
                                                cell.value === "Valid"
                                                  ? "green"
                                                  : "red"
                                              }
                                              size="sm"
                                            >
                                              {cell.value}
                                            </Tag>
                                          ) : (
                                            cell.value
                                          )}
                                        </TableCell>
                                      ))}
                                    </TableRow>
                                  );
                                })}
                              </TableBody>
                            </Table>
                          )}
                        </DataTable>
                      </div>
                    )}
                  </div>
                )}
              </>
            )}
          </div>
        )}

        {/* Step 4: Import Result */}
        {importStep === 4 && importResult && (
          <div className="import-step">
            <InlineNotification
              kind="success"
              title={intl.formatMessage({
                id: "notebook.mntd.import.success",
                defaultMessage: "Import successful",
              })}
              subtitle={`Created ${importResult.totalCreated} samples`}
              lowContrast
              hideCloseButton
            />
            {importResult.createdAccessionNumbers && (
              <div style={{ marginTop: "1rem" }}>
                <p>
                  <strong>Created Accession Numbers:</strong>
                </p>
                <p>{importResult.createdAccessionNumbers.join(", ")}</p>
                <Button
                  kind="tertiary"
                  size="sm"
                  renderIcon={Printer}
                  onClick={() => {
                    importResult.createdAccessionNumbers.forEach(
                      (accNo, idx) => {
                        setTimeout(() => handlePrintBarcode(accNo), idx * 500);
                      },
                    );
                  }}
                  style={{ marginTop: "1rem" }}
                >
                  <FormattedMessage
                    id="notebook.mntd.printAllBarcodes"
                    defaultMessage="Print All Barcodes ({count})"
                    values={{
                      count: importResult.createdAccessionNumbers.length,
                    }}
                  />
                </Button>
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}

export default MNTDSampleIntakePage;

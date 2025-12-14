import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  TextInput,
  TextArea,
  InlineNotification,
  Loading,
  Modal,
  Tag,
  FileUploader,
  Dropdown,
  ProgressBar,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
} from "@carbon/react";
import {
  Upload,
  CheckmarkFilled,
  WarningAlt,
  Checkmark,
  Edit,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerFormDataJson,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import SampleGrid from "../workflow/SampleGrid";

/**
 * AnalysisPage - Page 6: Main Analysis Execution
 *
 * Allows technicians to:
 * - Import analyzer results (CSV/Excel) from ELISA or Flow Cytometry
 * - Map columns to sample identifiers (well coordinate or external ID)
 * - Record assay run metadata (run ID, operator, machine params, reagent lots)
 * - View imported results in the sample grid
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - Page configuration data
 * @param {Object} props.progress - Page progress info
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function AnalysisPage({ entryId, pageData, progress, onProgressUpdate }) {
  const componentMounted = useRef(true);
  const intl = useIntl();

  // State
  const [loading, setLoading] = useState(true);
  const [samples, setSamples] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Import state
  const [showImportModal, setShowImportModal] = useState(false);
  const [importStep, setImportStep] = useState(1); // 1=upload, 2=map, 3=preview, 4=metadata
  const [uploadedFile, setUploadedFile] = useState(null);
  const [parseResult, setParseResult] = useState(null);
  const [columnMapping, setColumnMapping] = useState({});
  const [previewData, setPreviewData] = useState(null);
  const [importing, setImporting] = useState(false);
  const [importProgress, setImportProgress] = useState(0);

  // Assay metadata
  const [assayMetadata, setAssayMetadata] = useState({
    assayRunId: "",
    operatorId: "",
    machineParameters: "",
    reagentLots: "",
  });

  // Import summary
  const [importSummary, setImportSummary] = useState(null);

  // Completing state
  const [completing, setCompleting] = useState(false);

  // Manual entry modal state
  const [showManualEntryModal, setShowManualEntryModal] = useState(false);
  const [manualEntryData, setManualEntryData] = useState({
    result: "",
    assayRunId: "",
    notes: "",
  });
  const [savingManualEntry, setSavingManualEntry] = useState(false);

  // Load samples for this page
  const loadSamples = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
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
              // Preserve full data object for status checks
              data: sample.data,
              // Analysis data from sample's page data
              analyzerResult: sample.data?.analyzerResult || null,
              assayRunId: sample.data?.analyzerResult?.assayRunId || "",
              resultValue: sample.data?.analyzerResult?.result || "",
              importDate: sample.data?.analyzerResult?.importDate || "",
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

  // Load import summary
  const loadImportSummary = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      return;
    }

    getFromOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/analyzer-import/summary`,
      (response) => {
        if (componentMounted.current && response && !response.error) {
          setImportSummary(response);
        }
      },
    );
  }, [pageData?.id]);

  // Reset state and load data when page changes
  useEffect(() => {
    componentMounted.current = true;
    setSelectedIds([]);
    setStatusFilter("ALL");
    setError(null);
    setSuccess(null);
    loadSamples();
    loadImportSummary();

    return () => {
      componentMounted.current = false;
    };
  }, [pageData?.id, loadSamples, loadImportSummary]);

  // Handle file upload - Carbon FileUploader passes event with target.files
  const handleFileUpload = (event) => {
    console.log("handleFileUpload called, event:", event);
    const file = event.target?.files?.[0];
    console.log("File extracted:", file);
    if (!file) {
      console.log("No file found in event");
      return;
    }

    setUploadedFile(file);
    setError(null);

    // Parse the file
    const formData = new FormData();
    formData.append("file", file);

    console.log("Sending parse request for file:", file.name);
    postToOpenElisServerFormDataJson(
      `/rest/notebook/bulk/page/${pageData.id}/analyzer-import/parse`,
      formData,
      (response) => {
        console.log("Parse response:", response);
        if (response && response.success) {
          setParseResult(response);
          setImportStep(2); // Move to column mapping
        } else {
          setError(response?.error || "Failed to parse file");
        }
      },
    );
  };

  // Handle column mapping change
  const handleMappingChange = (targetField, sourceColumn) => {
    setColumnMapping((prev) => ({
      ...prev,
      [targetField]: sourceColumn,
    }));
  };

  // Generate preview
  const handleGeneratePreview = () => {
    console.log("handleGeneratePreview called");
    console.log("uploadedFile:", uploadedFile);
    console.log("columnMapping:", columnMapping);

    if (!uploadedFile) {
      console.log("No uploaded file available!");
      setError("Please upload a file first");
      return;
    }

    setError(null);
    const formData = new FormData();
    formData.append("file", uploadedFile);
    Object.entries(columnMapping).forEach(([key, value]) => {
      console.log(`Adding mapping: ${key} = ${value}`);
      formData.append(key, value);
    });

    console.log("Sending preview request...");
    postToOpenElisServerFormDataJson(
      `/rest/notebook/bulk/page/${pageData.id}/analyzer-import/preview`,
      formData,
      (response) => {
        console.log("Preview response:", response);
        if (response && response.success) {
          setPreviewData(response);
          setImportStep(3); // Move to preview
        } else {
          setError(response?.error || "Failed to generate preview");
        }
      },
    );
  };

  // Execute import
  const handleExecuteImport = () => {
    if (!uploadedFile) return;

    setImporting(true);
    setImportProgress(0);
    setError(null);

    const formData = new FormData();
    formData.append("file", uploadedFile);
    formData.append("assayRunId", assayMetadata.assayRunId);
    formData.append("operatorId", assayMetadata.operatorId);
    Object.entries(columnMapping).forEach(([key, value]) => {
      formData.append(key, value);
    });

    // Parse machine parameters and reagent lots
    if (assayMetadata.machineParameters) {
      try {
        const params = JSON.parse(assayMetadata.machineParameters);
        Object.entries(params).forEach(([key, value]) => {
          formData.append(`machineParameters[${key}]`, value);
        });
      } catch (e) {
        // Treat as single key-value
        formData.append(
          "machineParameters[raw]",
          assayMetadata.machineParameters,
        );
      }
    }
    if (assayMetadata.reagentLots) {
      assayMetadata.reagentLots.split(",").forEach((lot, idx) => {
        formData.append(`reagentLots[${idx}]`, lot.trim());
      });
    }

    postToOpenElisServerFormDataJson(
      `/rest/notebook/bulk/page/${pageData.id}/analyzer-import`,
      formData,
      (response) => {
        console.log("Import response:", response);
        setImporting(false);
        if (response && response.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.analysis.importSuccess",
                defaultMessage:
                  "Successfully imported {count} of {total} results",
              },
              {
                count: response.successfulRows,
                total: response.totalRows,
              },
            ),
          );
          setShowImportModal(false);
          resetImportState();
          loadSamples();
          loadImportSummary();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Import failed");
        }
      },
    );
  };

  // Mark selected samples as complete (or all with results if none selected)
  const handleMarkComplete = (sampleIdsToComplete = null) => {
    let idsToComplete = sampleIdsToComplete;

    // If no specific IDs provided, use selected samples or all with results
    if (!idsToComplete) {
      if (selectedIds.length > 0) {
        // Use selected samples
        idsToComplete = selectedIds.map((id) => parseInt(id, 10));
      } else {
        // Fallback to all samples with results
        const samplesWithResults = samples.filter(
          (s) => s.status === "IN_PROGRESS" && s.data?.analyzerResult,
        );
        if (samplesWithResults.length === 0) {
          setError(
            intl.formatMessage({
              id: "notebook.analysis.noSamplesToComplete",
              defaultMessage: "No samples to mark complete",
            }),
          );
          return;
        }
        idsToComplete = samplesWithResults.map((s) => parseInt(s.id, 10));
      }
    }

    if (idsToComplete.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.analysis.noSamplesToComplete",
          defaultMessage: "No samples to mark complete",
        }),
      );
      return;
    }

    setCompleting(true);
    setError(null);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: idsToComplete,
        status: "COMPLETED",
      }),
      (response) => {
        setCompleting(false);
        if (response && response.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.analysis.completeSuccess",
                defaultMessage:
                  "Successfully marked {count} samples as complete",
              },
              { count: response.updatedCount },
            ),
          );
          setSelectedIds([]);
          loadSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Failed to mark samples complete");
        }
      },
    );
  };

  // Handle status change for individual samples (from SampleGrid overflow menu)
  const handleStatusChange = (sampleId, newStatus) => {
    if (newStatus === "COMPLETED") {
      handleMarkComplete([parseInt(sampleId, 10)]);
    }
  };

  // Handle manual result entry
  const handleOpenManualEntry = () => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.analysis.selectSamplesFirst",
          defaultMessage: "Please select at least one sample to add results to",
        }),
      );
      return;
    }
    setManualEntryData({
      result: "",
      assayRunId: "",
      notes: "",
    });
    setShowManualEntryModal(true);
  };

  // Save manual results
  const handleSaveManualResults = () => {
    if (!manualEntryData.result.trim()) {
      setError(
        intl.formatMessage({
          id: "notebook.analysis.resultRequired",
          defaultMessage: "Result value is required",
        }),
      );
      return;
    }

    setSavingManualEntry(true);
    setError(null);

    const request = {
      sampleIds: selectedIds.map((id) => parseInt(id, 10)),
      result: manualEntryData.result.trim(),
      assayRunId: manualEntryData.assayRunId.trim() || null,
      notes: manualEntryData.notes.trim() || null,
      entryType: "MANUAL",
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/manual-results`,
      JSON.stringify(request),
      (response) => {
        setSavingManualEntry(false);
        if (response && response.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.analysis.manualEntrySaved",
                defaultMessage:
                  "Successfully added results to {count} sample(s)",
              },
              { count: response.updatedCount || selectedIds.length },
            ),
          );
          setShowManualEntryModal(false);
          setSelectedIds([]);
          loadSamples();
          loadImportSummary();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Failed to save manual results");
        }
      },
    );
  };

  // Reset import modal state
  const resetImportState = () => {
    setImportStep(1);
    setUploadedFile(null);
    setParseResult(null);
    setColumnMapping({});
    setPreviewData(null);
    setAssayMetadata({
      assayRunId: "",
      operatorId: "",
      machineParameters: "",
      reagentLots: "",
    });
  };

  // Render result info column
  const renderResultInfo = (sample) => {
    if (sample.analyzerResult) {
      return (
        <div style={{ fontSize: "12px" }}>
          <Tag type="green" size="sm">
            {sample.resultValue || "Imported"}
          </Tag>
          {sample.assayRunId && (
            <span style={{ marginLeft: "4px", color: "#525252" }}>
              Run: {sample.assayRunId}
            </span>
          )}
        </div>
      );
    }
    return (
      <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
        <FormattedMessage
          id="notebook.analysis.noResult"
          defaultMessage="No result imported"
        />
      </span>
    );
  };

  // Render import step content
  const renderImportStep = () => {
    switch (importStep) {
      case 1:
        return (
          <div>
            <p style={{ marginBottom: "1rem", color: "#525252" }}>
              <FormattedMessage
                id="notebook.analysis.uploadInstructions"
                defaultMessage="Upload a CSV or Excel file containing analyzer results. The first row should contain column headers."
              />
            </p>
            <FileUploader
              accept={[".csv", ".xlsx", ".xls"]}
              buttonLabel={intl.formatMessage({
                id: "notebook.analysis.selectFile",
                defaultMessage: "Select file",
              })}
              filenameStatus="edit"
              iconDescription="Clear file"
              labelDescription="Max file size is 10MB. Supported formats: CSV, XLSX, XLS"
              labelTitle={intl.formatMessage({
                id: "notebook.analysis.uploadFile",
                defaultMessage: "Upload analyzer results",
              })}
              onChange={handleFileUpload}
            />
            {parseResult && (
              <div style={{ marginTop: "1rem" }}>
                <Tag type="blue">
                  {parseResult.totalRows}{" "}
                  <FormattedMessage
                    id="notebook.analysis.rowsFound"
                    defaultMessage="rows found"
                  />
                </Tag>
              </div>
            )}
          </div>
        );

      case 2:
        return (
          <div>
            <p style={{ marginBottom: "1rem", color: "#525252" }}>
              <FormattedMessage
                id="notebook.analysis.mappingInstructions"
                defaultMessage="Map columns from your file to sample identifiers. Well coordinate is the primary matching method."
              />
            </p>
            <div
              style={{ display: "flex", flexDirection: "column", gap: "1rem" }}
            >
              <Dropdown
                id="wellCoordinate-mapping"
                titleText={intl.formatMessage({
                  id: "notebook.analysis.wellCoordinateColumn",
                  defaultMessage: "Well Coordinate Column",
                })}
                label="Select column"
                items={parseResult?.headers || []}
                onChange={({ selectedItem }) =>
                  handleMappingChange("wellCoordinate", selectedItem)
                }
              />
              <Dropdown
                id="externalId-mapping"
                titleText={intl.formatMessage({
                  id: "notebook.analysis.externalIdColumn",
                  defaultMessage: "External ID Column (fallback)",
                })}
                label="Select column"
                items={parseResult?.headers || []}
                onChange={({ selectedItem }) =>
                  handleMappingChange("externalId", selectedItem)
                }
              />
              <Dropdown
                id="result-mapping"
                titleText={intl.formatMessage({
                  id: "notebook.analysis.resultColumn",
                  defaultMessage: "Result Value Column",
                })}
                label="Select column"
                items={parseResult?.headers || []}
                onChange={({ selectedItem }) =>
                  handleMappingChange("result", selectedItem)
                }
              />
            </div>
            <Button
              kind="primary"
              onClick={handleGeneratePreview}
              style={{ marginTop: "1rem" }}
              disabled={
                !columnMapping.wellCoordinate && !columnMapping.externalId
              }
            >
              <FormattedMessage
                id="notebook.analysis.generatePreview"
                defaultMessage="Generate Preview"
              />
            </Button>
          </div>
        );

      case 3:
        return (
          <div>
            <p style={{ marginBottom: "1rem", color: "#525252" }}>
              <FormattedMessage
                id="notebook.analysis.previewInstructions"
                defaultMessage="Review the matching results below. Matched samples will be updated with analyzer results."
              />
            </p>
            {previewData && (
              <div style={{ marginBottom: "1rem" }}>
                <Tag type="green">
                  {previewData.matchedCount}{" "}
                  <FormattedMessage
                    id="notebook.analysis.matched"
                    defaultMessage="matched"
                  />
                </Tag>
                <Tag type="red" style={{ marginLeft: "0.5rem" }}>
                  {previewData.unmatchedCount}{" "}
                  <FormattedMessage
                    id="notebook.analysis.unmatched"
                    defaultMessage="unmatched"
                  />
                </Tag>
              </div>
            )}
            {previewData?.warnings?.length > 0 && (
              <InlineNotification
                kind="warning"
                title="Warnings"
                subtitle={previewData.warnings.join("; ")}
                style={{ marginBottom: "1rem" }}
              />
            )}
            <DataTable
              rows={
                previewData?.rows?.slice(0, 10).map((row, idx) => ({
                  id: String(idx),
                  rowNumber: row.rowNumber,
                  matchStatus: row.matchStatus,
                  matchType: row.matchType || "-",
                  sampleId: row.matchedSampleId || "-",
                })) || []
              }
              headers={[
                { key: "rowNumber", header: "Row" },
                { key: "matchStatus", header: "Status" },
                { key: "matchType", header: "Match Type" },
                { key: "sampleId", header: "Sample ID" },
              ]}
            >
              {({
                rows,
                headers,
                getTableProps,
                getHeaderProps,
                getRowProps,
              }) => (
                <TableContainer>
                  <Table {...getTableProps()} size="sm">
                    <TableHead>
                      <TableRow>
                        {headers.map((header) => (
                          <TableHeader
                            {...getHeaderProps({ header })}
                            key={header.key}
                          >
                            {header.header}
                          </TableHeader>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((row) => (
                        <TableRow {...getRowProps({ row })} key={row.id}>
                          {row.cells.map((cell) => (
                            <TableCell key={cell.id}>
                              {cell.info.header === "matchStatus" ? (
                                <Tag
                                  type={
                                    cell.value === "MATCHED"
                                      ? "green"
                                      : cell.value === "DUPLICATE_MATCH"
                                        ? "blue"
                                        : cell.value === "DUPLICATE"
                                          ? "orange"
                                          : "red"
                                  }
                                  size="sm"
                                >
                                  {cell.value === "DUPLICATE_MATCH"
                                    ? "REPLICATE"
                                    : cell.value}
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
                </TableContainer>
              )}
            </DataTable>
            <Button
              kind="primary"
              onClick={() => setImportStep(4)}
              style={{ marginTop: "1rem" }}
              disabled={!previewData || previewData.matchedCount === 0}
            >
              <FormattedMessage
                id="notebook.analysis.continue"
                defaultMessage="Continue to Metadata"
              />
            </Button>
          </div>
        );

      case 4:
        return (
          <div>
            <p style={{ marginBottom: "1rem", color: "#525252" }}>
              <FormattedMessage
                id="notebook.analysis.metadataInstructions"
                defaultMessage="Enter assay run metadata. This information will be stored with each imported result."
              />
            </p>
            <TextInput
              id="assayRunId"
              labelText={intl.formatMessage({
                id: "notebook.analysis.assayRunId",
                defaultMessage: "Assay Run ID",
              })}
              value={assayMetadata.assayRunId}
              onChange={(e) =>
                setAssayMetadata({
                  ...assayMetadata,
                  assayRunId: e.target.value,
                })
              }
              style={{ marginBottom: "1rem" }}
            />
            <TextInput
              id="operatorId"
              labelText={intl.formatMessage({
                id: "notebook.analysis.operatorId",
                defaultMessage: "Operator ID / Name",
              })}
              value={assayMetadata.operatorId}
              onChange={(e) =>
                setAssayMetadata({
                  ...assayMetadata,
                  operatorId: e.target.value,
                })
              }
              style={{ marginBottom: "1rem" }}
            />
            <TextArea
              id="machineParameters"
              labelText={intl.formatMessage({
                id: "notebook.analysis.machineParameters",
                defaultMessage: "Machine Parameters (JSON or text)",
              })}
              value={assayMetadata.machineParameters}
              onChange={(e) =>
                setAssayMetadata({
                  ...assayMetadata,
                  machineParameters: e.target.value,
                })
              }
              rows={3}
              placeholder='{"wavelength": "450nm", "temperature": "25C"}'
              style={{ marginBottom: "1rem" }}
            />
            <TextInput
              id="reagentLots"
              labelText={intl.formatMessage({
                id: "notebook.analysis.reagentLots",
                defaultMessage: "Reagent Lot Numbers (comma-separated)",
              })}
              value={assayMetadata.reagentLots}
              onChange={(e) =>
                setAssayMetadata({
                  ...assayMetadata,
                  reagentLots: e.target.value,
                })
              }
              placeholder="LOT123, LOT456, LOT789"
              style={{ marginBottom: "1rem" }}
            />
            {importing && (
              <ProgressBar
                label="Importing results..."
                value={importProgress}
                max={100}
                style={{ marginTop: "1rem" }}
              />
            )}
          </div>
        );

      default:
        return null;
    }
  };

  // Check if using default pages (no real page data)
  const isDefaultPage =
    !pageData?.id || String(pageData.id).startsWith("default-");

  // Debug logging
  console.log("AnalysisPage render:", {
    pageData,
    pageDataId: pageData?.id,
    isDefaultPage,
    loading,
    entryId,
    samplesCount: samples.length,
  });

  if (loading && !isDefaultPage) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <Loading withOverlay={false} description="Loading samples..." />
      </div>
    );
  }

  // Render placeholder for default pages
  if (isDefaultPage) {
    return (
      <div className="analysis-page">
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <div
              className="page-instructions"
              style={{ marginBottom: "1.5rem" }}
            >
              <p style={{ color: "#525252" }}>
                <FormattedMessage
                  id="notebook.analysis.instructions"
                  defaultMessage="Import analyzer results from ELISA or Flow Cytometry. Results are matched to samples by well coordinate or external ID."
                />
              </p>
            </div>
            <InlineNotification
              kind="info"
              title={intl.formatMessage({
                id: "notebook.analysis.noPageData",
                defaultMessage: "Page Not Configured",
              })}
              subtitle={intl.formatMessage({
                id: "notebook.analysis.noPageDataDesc",
                defaultMessage:
                  "This notebook does not have configured pages yet. Create a notebook entry to enable analysis import functionality.",
              })}
              hideCloseButton
              style={{ marginBottom: "1rem" }}
            />
            <div
              style={{
                padding: "2rem",
                backgroundColor: "#f4f4f4",
                borderRadius: "4px",
                textAlign: "center",
              }}
            >
              <Upload
                size={48}
                style={{ color: "#8d8d8d", marginBottom: "1rem" }}
              />
              <p style={{ color: "#525252", marginBottom: "0.5rem" }}>
                <FormattedMessage
                  id="notebook.analysis.importDisabled"
                  defaultMessage="Analyzer result import will be available once a notebook entry is created."
                />
              </p>
              <p style={{ color: "#8d8d8d", fontSize: "0.875rem" }}>
                <FormattedMessage
                  id="notebook.analysis.supportedFormats"
                  defaultMessage="Supported formats: CSV, XLSX, XLS"
                />
              </p>
            </div>
          </Column>
        </Grid>
      </div>
    );
  }

  return (
    <div className="analysis-page">
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({ id: "error" })}
          subtitle={error}
          onCloseButtonClick={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({ id: "success" })}
          subtitle={success}
          onCloseButtonClick={() => setSuccess(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <div className="page-instructions" style={{ marginBottom: "1.5rem" }}>
            <p style={{ color: "#525252" }}>
              <FormattedMessage
                id="notebook.analysis.instructions"
                defaultMessage="Import analyzer results from ELISA or Flow Cytometry. Results are matched to samples by well coordinate or external ID."
              />
            </p>
          </div>
        </Column>

        {/* Import Summary */}
        {importSummary && importSummary.importCount > 0 && (
          <Column lg={16} md={8} sm={4}>
            <div
              style={{
                padding: "1rem",
                backgroundColor: "#e0f0e0",
                borderRadius: "4px",
                marginBottom: "1rem",
              }}
            >
              <div
                style={{ display: "flex", gap: "1rem", alignItems: "center" }}
              >
                <CheckmarkFilled size={20} style={{ color: "#24a148" }} />
                <span>
                  <FormattedMessage
                    id="notebook.analysis.importSummary"
                    defaultMessage="{count} imports completed, {successRate}% success rate"
                    values={{
                      count: importSummary.importCount,
                      successRate:
                        importSummary.overallSuccessRate?.toFixed(1) || 0,
                    }}
                  />
                </span>
              </div>
            </div>
          </Column>
        )}

        {/* Actions */}
        <Column lg={16} md={8} sm={4}>
          <div
            className="bulk-actions"
            style={{
              display: "flex",
              gap: "1rem",
              marginBottom: "1rem",
              padding: "1rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
            }}
          >
            <Button
              kind="primary"
              size="md"
              renderIcon={Upload}
              onClick={() => setShowImportModal(true)}
            >
              <FormattedMessage
                id="notebook.analysis.importResults"
                defaultMessage="Import Analyzer Results"
              />
            </Button>
            <Button
              kind="tertiary"
              size="md"
              renderIcon={Edit}
              onClick={handleOpenManualEntry}
              disabled={selectedIds.length === 0}
            >
              <FormattedMessage
                id="notebook.analysis.manualEntry"
                defaultMessage="Add Results Manually"
              />
              {selectedIds.length > 0 && ` (${selectedIds.length})`}
            </Button>
            <Button
              kind="secondary"
              size="md"
              renderIcon={Checkmark}
              onClick={() => handleMarkComplete()}
              disabled={
                completing ||
                (selectedIds.length === 0 &&
                  samples.filter(
                    (s) => s.status === "IN_PROGRESS" && s.data?.analyzerResult,
                  ).length === 0)
              }
            >
              {completing ? (
                <FormattedMessage
                  id="notebook.analysis.completing"
                  defaultMessage="Completing..."
                />
              ) : selectedIds.length > 0 ? (
                <FormattedMessage
                  id="notebook.analysis.markSelectedComplete"
                  defaultMessage="Mark Selected Complete ({count})"
                  values={{ count: selectedIds.length }}
                />
              ) : (
                <FormattedMessage
                  id="notebook.analysis.markAllComplete"
                  defaultMessage="Mark All with Results Complete ({count})"
                  values={{
                    count: samples.filter(
                      (s) =>
                        s.status === "IN_PROGRESS" && s.data?.analyzerResult,
                    ).length,
                  }}
                />
              )}
            </Button>
          </div>
        </Column>

        {/* Selection info */}
        {selectedIds.length > 0 && (
          <Column lg={16} md={8} sm={4}>
            <div
              style={{
                padding: "0.5rem 1rem",
                backgroundColor: "#e0e0e0",
                borderRadius: "4px",
                marginBottom: "1rem",
              }}
            >
              <FormattedMessage
                id="notebook.analysis.selectedCount"
                defaultMessage="{count} samples selected - use 'Mark Selected Complete' to mark them as complete"
                values={{ count: selectedIds.length }}
              />
            </div>
          </Column>
        )}

        {/* Sample Grid */}
        <Column lg={16} md={8} sm={4}>
          <SampleGrid
            gridId="analysis"
            samples={samples}
            selectedIds={selectedIds}
            onSelectionChange={setSelectedIds}
            onStatusChange={handleStatusChange}
            statusFilter={statusFilter}
            onStatusFilterChange={setStatusFilter}
            showSelection={true}
            loading={loading}
            additionalColumns={[
              {
                key: "resultInfo",
                header: intl.formatMessage({
                  id: "notebook.analysis.resultInfo",
                  defaultMessage: "Result Info",
                }),
                render: renderResultInfo,
              },
            ]}
          />
        </Column>
      </Grid>

      {/* Import Modal */}
      <Modal
        open={showImportModal}
        onRequestClose={() => {
          setShowImportModal(false);
          resetImportState();
        }}
        onRequestSubmit={handleExecuteImport}
        modalHeading={intl.formatMessage({
          id: "notebook.analysis.importTitle",
          defaultMessage: "Import Analyzer Results",
        })}
        primaryButtonText={
          importStep === 4
            ? intl.formatMessage({
                id: "notebook.analysis.import",
                defaultMessage: "Import",
              })
            : undefined
        }
        primaryButtonDisabled={importStep !== 4 || importing}
        secondaryButtonText={intl.formatMessage({
          id: "notebook.analysis.cancel",
          defaultMessage: "Cancel",
        })}
        size="lg"
      >
        <div style={{ minHeight: "400px" }}>
          {/* Step indicator */}
          <div
            style={{
              display: "flex",
              marginBottom: "1.5rem",
              borderBottom: "1px solid #e0e0e0",
              paddingBottom: "1rem",
            }}
          >
            {[1, 2, 3, 4].map((step) => (
              <div
                key={step}
                style={{
                  flex: 1,
                  textAlign: "center",
                  color: importStep >= step ? "#0f62fe" : "#8d8d8d",
                  fontWeight: importStep === step ? "bold" : "normal",
                }}
              >
                {step === 1 && "Upload"}
                {step === 2 && "Map Columns"}
                {step === 3 && "Preview"}
                {step === 4 && "Metadata"}
              </div>
            ))}
          </div>

          {renderImportStep()}
        </div>
      </Modal>

      {/* Manual Entry Modal */}
      <Modal
        open={showManualEntryModal}
        onRequestClose={() => setShowManualEntryModal(false)}
        onRequestSubmit={handleSaveManualResults}
        modalHeading={intl.formatMessage({
          id: "notebook.analysis.manualEntryTitle",
          defaultMessage: "Add Results Manually",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.analysis.saveResults",
          defaultMessage: "Save Results",
        })}
        primaryButtonDisabled={
          savingManualEntry || !manualEntryData.result.trim()
        }
        secondaryButtonText={intl.formatMessage({
          id: "notebook.analysis.cancel",
          defaultMessage: "Cancel",
        })}
        size="md"
      >
        <div style={{ minHeight: "250px" }}>
          <p style={{ marginBottom: "1rem", color: "#525252" }}>
            <FormattedMessage
              id="notebook.analysis.manualEntryDescription"
              defaultMessage="Enter result value for {count} selected sample(s). The same result will be applied to all selected samples."
              values={{ count: selectedIds.length }}
            />
          </p>

          {/* Show selected samples */}
          <div
            style={{
              marginBottom: "1rem",
              padding: "0.5rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
              maxHeight: "100px",
              overflowY: "auto",
            }}
          >
            <strong>
              <FormattedMessage
                id="notebook.analysis.selectedSamples"
                defaultMessage="Selected samples:"
              />
            </strong>
            <div style={{ marginTop: "0.25rem", fontSize: "0.875rem" }}>
              {selectedIds
                .map((id) => {
                  const sample = samples.find((s) => s.id === id);
                  return sample?.externalId || sample?.accessionNumber || id;
                })
                .join(", ")}
            </div>
          </div>

          <TextInput
            id="manual-result"
            labelText={intl.formatMessage({
              id: "notebook.analysis.resultValue",
              defaultMessage: "Result Value",
            })}
            value={manualEntryData.result}
            onChange={(e) =>
              setManualEntryData({
                ...manualEntryData,
                result: e.target.value,
              })
            }
            placeholder={intl.formatMessage({
              id: "notebook.analysis.resultPlaceholder",
              defaultMessage: "e.g., Positive, Negative, 1.5, 0.8 OD",
            })}
            style={{ marginBottom: "1rem" }}
            required
          />

          <TextInput
            id="manual-assay-run-id"
            labelText={intl.formatMessage({
              id: "notebook.analysis.assayRunId",
              defaultMessage: "Assay Run ID (Optional)",
            })}
            value={manualEntryData.assayRunId}
            onChange={(e) =>
              setManualEntryData({
                ...manualEntryData,
                assayRunId: e.target.value,
              })
            }
            placeholder={intl.formatMessage({
              id: "notebook.analysis.assayRunIdPlaceholder",
              defaultMessage: "e.g., RUN-2024-001",
            })}
            style={{ marginBottom: "1rem" }}
          />

          <TextArea
            id="manual-notes"
            labelText={intl.formatMessage({
              id: "notebook.analysis.notes",
              defaultMessage: "Notes (Optional)",
            })}
            value={manualEntryData.notes}
            onChange={(e) =>
              setManualEntryData({
                ...manualEntryData,
                notes: e.target.value,
              })
            }
            placeholder={intl.formatMessage({
              id: "notebook.analysis.notesPlaceholder",
              defaultMessage: "Any additional notes about this result...",
            })}
            rows={3}
          />

          {savingManualEntry && (
            <div
              style={{
                marginTop: "1rem",
                display: "flex",
                alignItems: "center",
                gap: "0.5rem",
              }}
            >
              <Loading withOverlay={false} small />
              <span>
                <FormattedMessage
                  id="notebook.analysis.saving"
                  defaultMessage="Saving results..."
                />
              </span>
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
}

export default AnalysisPage;

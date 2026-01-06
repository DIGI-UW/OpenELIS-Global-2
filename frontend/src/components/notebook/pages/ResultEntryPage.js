import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useContext,
} from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Loading,
  Modal,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectAll,
  TableSelectRow,
  Tag,
  TextInput,
  TextArea,
  Dropdown,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  FileUploader,
  ProgressBar,
} from "@carbon/react";
import {
  Save,
  Edit,
  Upload,
  Checkmark,
  Warning,
  WarningAlt,
  CheckmarkFilled,
  CloseFilled,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import "../workflow/NotebookWorkflow.css";

/**
 * ResultEntryPage - Enhanced Result Entry for MedLab workflow.
 *
 * Features:
 * - Machine-generated results auto-upload (CSV/Excel)
 * - Manual result entry (MANUAL, MICROSCOPY, RDT)
 * - Units and reference ranges display
 * - Flags (CRITICAL_LOW, LOW, NORMAL, HIGH, CRITICAL_HIGH)
 * - Validation prevention before release
 * - Timestamp and user stamp
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function ResultEntryPage({ entryId, pageData, progress, onProgressUpdate }) {
  const intl = useIntl();
  const componentMounted = useRef(false);
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Tab state
  const [activeTab, setActiveTab] = useState(0);

  // Selection state
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);

  // Result entry modal state
  const [resultModalOpen, setResultModalOpen] = useState(false);
  const [selectedSample, setSelectedSample] = useState(null);
  const [selectedTest, setSelectedTest] = useState(null);
  const [resultValue, setResultValue] = useState("");
  const [entryType, setEntryType] = useState("MANUAL");
  const [notes, setNotes] = useState("");
  const [saving, setSaving] = useState(false);

  // Import modal state
  const [importModalOpen, setImportModalOpen] = useState(false);
  const [importStep, setImportStep] = useState(1);
  const [uploadedFile, setUploadedFile] = useState(null);
  const [parseResult, setParseResult] = useState(null);
  const [columnMapping, setColumnMapping] = useState({});
  const [importMetadata, setImportMetadata] = useState({
    analyzerName: "",
    runId: "",
  });
  const [importing, setImporting] = useState(false);
  const [importProgress, setImportProgress] = useState(0);

  // Mark complete state
  const [completing, setCompleting] = useState(false);

  // Entry type options
  const entryTypeOptions = [
    {
      id: "MANUAL",
      label: intl.formatMessage({
        id: "medlab.result.entryType.manual",
        defaultMessage: "Manual Entry",
      }),
    },
    {
      id: "MICROSCOPY",
      label: intl.formatMessage({
        id: "medlab.result.entryType.microscopy",
        defaultMessage: "Microscopy",
      }),
    },
    {
      id: "RDT",
      label: intl.formatMessage({
        id: "medlab.result.entryType.rdt",
        defaultMessage: "Rapid Diagnostic Test (RDT)",
      }),
    },
    {
      id: "AUTO_IMPORT",
      label: intl.formatMessage({
        id: "medlab.result.entryType.autoImport",
        defaultMessage: "Auto Import",
      }),
    },
  ];

  // Load samples for result entry
  const loadSamplesForResultEntry = useCallback(() => {
    if (!entryId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    const url = `/rest/medlab/entry/${entryId}/samples-for-results`;

    getFromOpenElisServer(url, (response) => {
      if (componentMounted.current) {
        if (response && Array.isArray(response)) {
          setSamples(response);
        } else {
          setSamples([]);
        }
        setLoading(false);
      }
    });
  }, [entryId]);

  // Load data on mount
  useEffect(() => {
    componentMounted.current = true;
    loadSamplesForResultEntry();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id, loadSamplesForResultEntry]);

  // Open result entry modal
  const handleOpenResultModal = useCallback((sample, test) => {
    setSelectedSample(sample);
    setSelectedTest(test);
    setResultValue(test.resultValue || "");
    setEntryType(test.entryType || "MANUAL");
    setNotes("");
    setResultModalOpen(true);
  }, []);

  // Submit result
  const handleSubmitResult = useCallback(() => {
    if (!selectedSample || !selectedTest) return;

    setSaving(true);
    setError(null);

    const resultData = {
      labNo: selectedSample.labNo,
      testId: selectedTest.testId,
      resultValue: resultValue,
      unit: selectedTest.unit,
      entryType: entryType,
      notes: notes,
      notebookPageId: pageData?.id,
    };

    postToOpenElisServerJsonResponse(
      "/rest/medlab/result-entry",
      JSON.stringify(resultData),
      (response) => {
        setSaving(false);
        if (response && response.success) {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "medlab.result.save.success",
              defaultMessage: "Result saved successfully",
            }),
            kind: NotificationKinds.success,
          });
          setNotificationVisible(true);
          setResultModalOpen(false);
          loadSamplesForResultEntry();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Error saving result");
        }
      },
    );
  }, [
    selectedSample,
    selectedTest,
    resultValue,
    entryType,
    notes,
    pageData,
    intl,
    addNotification,
    setNotificationVisible,
    loadSamplesForResultEntry,
    onProgressUpdate,
  ]);

  // Handle file upload for import
  const handleFileUpload = (event) => {
    const file = event.target?.files?.[0];
    if (!file) return;

    setUploadedFile(file);
    setError(null);

    // Parse file to get headers
    const reader = new FileReader();
    reader.onload = (e) => {
      const text = e.target.result;
      const lines = text.split("\n");
      if (lines.length > 0) {
        const headers = lines[0]
          .split(",")
          .map((h) => h.trim().replace(/"/g, ""));
        setParseResult({
          headers,
          totalRows: lines.length - 1,
          previewRows: lines
            .slice(1, 6)
            .map((line) =>
              line.split(",").map((c) => c.trim().replace(/"/g, "")),
            ),
        });
        setImportStep(2);
      }
    };
    reader.readAsText(file);
  };

  // Handle column mapping
  const handleMappingChange = (field, column) => {
    setColumnMapping((prev) => ({
      ...prev,
      [field]: column,
    }));
  };

  // Execute import
  const handleExecuteImport = useCallback(() => {
    if (!uploadedFile || !parseResult) return;

    setImporting(true);
    setImportProgress(0);
    setError(null);

    // Parse the file and create results
    const reader = new FileReader();
    reader.onload = (e) => {
      const text = e.target.result;
      const lines = text.split("\n");
      const headers = lines[0]
        .split(",")
        .map((h) => h.trim().replace(/"/g, ""));

      const labNoIdx = headers.indexOf(columnMapping.labNo);
      const testIdIdx = headers.indexOf(columnMapping.testId);
      const resultIdx = headers.indexOf(columnMapping.result);
      const unitIdx = columnMapping.unit
        ? headers.indexOf(columnMapping.unit)
        : -1;

      const results = [];
      for (let i = 1; i < lines.length; i++) {
        const cells = lines[i]
          .split(",")
          .map((c) => c.trim().replace(/"/g, ""));
        if (cells.length > 0 && cells[labNoIdx]) {
          results.push({
            labNo: cells[labNoIdx],
            testId: cells[testIdIdx] || null,
            resultValue: cells[resultIdx] || "",
            unit: unitIdx >= 0 ? cells[unitIdx] : null,
          });
        }
        setImportProgress(Math.round((i / lines.length) * 50));
      }

      // Send bulk import request
      const importData = {
        results,
        entryType: "AUTO_IMPORT",
        analyzerName: importMetadata.analyzerName,
        runId: importMetadata.runId,
        notebookPageId: pageData?.id,
      };

      postToOpenElisServerJsonResponse(
        "/rest/medlab/bulk-result-entry",
        JSON.stringify(importData),
        (response) => {
          setImporting(false);
          setImportProgress(100);

          if (response && response.success !== false) {
            setSuccess(
              intl.formatMessage(
                {
                  id: "medlab.result.import.success",
                  defaultMessage:
                    "Successfully imported {count} of {total} results",
                },
                {
                  count: response.successCount || 0,
                  total: response.totalCount || results.length,
                },
              ),
            );
            setImportModalOpen(false);
            resetImportState();
            loadSamplesForResultEntry();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError(response?.error || "Import failed");
          }
        },
      );
    };
    reader.readAsText(uploadedFile);
  }, [
    uploadedFile,
    parseResult,
    columnMapping,
    importMetadata,
    pageData,
    intl,
    loadSamplesForResultEntry,
    onProgressUpdate,
  ]);

  // Reset import state
  const resetImportState = () => {
    setImportStep(1);
    setUploadedFile(null);
    setParseResult(null);
    setColumnMapping({});
    setImportMetadata({ analyzerName: "", runId: "" });
    setImportProgress(0);
  };

  // Mark selected samples as complete
  const handleMarkComplete = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      // Mark all completed samples
      const completedSamples = samples.filter(
        (s) => s.resultEntryStatus === "COMPLETED" || s.pendingTests === 0,
      );
      if (completedSamples.length === 0) {
        setError(
          intl.formatMessage({
            id: "medlab.result.noSamplesToComplete",
            defaultMessage:
              "No samples with all results entered to mark complete",
          }),
        );
        return;
      }
    }

    setCompleting(true);
    setError(null);

    const sampleIds =
      selectedSampleIds.length > 0
        ? selectedSampleIds.map((id) => parseInt(id, 10))
        : samples
            .filter(
              (s) =>
                s.resultEntryStatus === "COMPLETED" || s.pendingTests === 0,
            )
            .map((s) => parseInt(s.id, 10));

    postToOpenElisServerJsonResponse(
      "/rest/medlab/result-entry/mark-complete",
      JSON.stringify({
        sampleIds,
        notebookPageId: pageData?.id,
      }),
      (response) => {
        setCompleting(false);
        if (response && response.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "medlab.result.markComplete.success",
                defaultMessage: "Marked {count} samples as complete",
              },
              { count: response.updatedCount },
            ),
          );
          setSelectedSampleIds([]);
          loadSamplesForResultEntry();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Error marking samples complete");
        }
      },
    );
  }, [
    selectedSampleIds,
    samples,
    pageData,
    intl,
    loadSamplesForResultEntry,
    onProgressUpdate,
  ]);

  // Calculate stats
  const totalSamples = samples.length;
  const completedSamples = samples.filter(
    (s) => s.resultEntryStatus === "COMPLETED",
  ).length;
  const inProgressSamples = samples.filter(
    (s) => s.resultEntryStatus === "IN_PROGRESS",
  ).length;
  const pendingSamples = samples.filter(
    (s) => s.resultEntryStatus === "PENDING",
  ).length;

  // Calculate total tests
  const totalTests = samples.reduce((sum, s) => sum + (s.totalTests || 0), 0);
  const enteredTests = samples.reduce(
    (sum, s) => sum + (s.enteredTests || 0),
    0,
  );
  const pendingTests = samples.reduce(
    (sum, s) => sum + (s.pendingTests || 0),
    0,
  );

  // Count flagged results
  const flaggedResults = samples.reduce((count, s) => {
    if (!s.tests) return count;
    return count + s.tests.filter((t) => t.flag && t.flag !== "NORMAL").length;
  }, 0);

  // Get flag tag type
  const getFlagTagType = (flag) => {
    switch (flag) {
      case "CRITICAL_LOW":
      case "CRITICAL_HIGH":
        return "red";
      case "LOW":
      case "HIGH":
        return "orange";
      case "NORMAL":
        return "green";
      default:
        return "gray";
    }
  };

  // Get flag display
  const getFlagDisplay = (flag) => {
    switch (flag) {
      case "CRITICAL_LOW":
        return "↓↓ Critical Low";
      case "CRITICAL_HIGH":
        return "↑↑ Critical High";
      case "LOW":
        return "↓ Low";
      case "HIGH":
        return "↑ High";
      case "NORMAL":
        return "Normal";
      default:
        return "-";
    }
  };

  // Status tag color
  const getStatusTagType = (status) => {
    switch (status) {
      case "COMPLETED":
        return "green";
      case "IN_PROGRESS":
        return "blue";
      case "PENDING":
        return "gray";
      default:
        return "gray";
    }
  };

  // Render import step content
  const renderImportStep = () => {
    switch (importStep) {
      case 1:
        return (
          <div>
            <p style={{ marginBottom: "1rem", color: "#525252" }}>
              <FormattedMessage
                id="medlab.result.import.uploadInstructions"
                defaultMessage="Upload a CSV file containing test results. The first row should contain column headers."
              />
            </p>
            <FileUploader
              accept={[".csv"]}
              buttonLabel={intl.formatMessage({
                id: "medlab.result.import.selectFile",
                defaultMessage: "Select file",
              })}
              filenameStatus="edit"
              iconDescription="Clear file"
              labelDescription="Supported format: CSV"
              labelTitle={intl.formatMessage({
                id: "medlab.result.import.uploadFile",
                defaultMessage: "Upload results file",
              })}
              onChange={handleFileUpload}
            />
          </div>
        );

      case 2:
        return (
          <div>
            <p style={{ marginBottom: "1rem", color: "#525252" }}>
              <FormattedMessage
                id="medlab.result.import.mappingInstructions"
                defaultMessage="Map columns from your file to result fields."
              />
            </p>
            <div
              style={{ display: "flex", flexDirection: "column", gap: "1rem" }}
            >
              <Dropdown
                id="labNo-mapping"
                titleText={intl.formatMessage({
                  id: "medlab.result.import.labNoColumn",
                  defaultMessage: "Lab Number Column *",
                })}
                label="Select column"
                items={parseResult?.headers || []}
                onChange={({ selectedItem }) =>
                  handleMappingChange("labNo", selectedItem)
                }
              />
              <Dropdown
                id="testId-mapping"
                titleText={intl.formatMessage({
                  id: "medlab.result.import.testIdColumn",
                  defaultMessage: "Test ID Column (optional)",
                })}
                label="Select column"
                items={parseResult?.headers || []}
                onChange={({ selectedItem }) =>
                  handleMappingChange("testId", selectedItem)
                }
              />
              <Dropdown
                id="result-mapping"
                titleText={intl.formatMessage({
                  id: "medlab.result.import.resultColumn",
                  defaultMessage: "Result Value Column *",
                })}
                label="Select column"
                items={parseResult?.headers || []}
                onChange={({ selectedItem }) =>
                  handleMappingChange("result", selectedItem)
                }
              />
              <Dropdown
                id="unit-mapping"
                titleText={intl.formatMessage({
                  id: "medlab.result.import.unitColumn",
                  defaultMessage: "Unit Column (optional)",
                })}
                label="Select column"
                items={parseResult?.headers || []}
                onChange={({ selectedItem }) =>
                  handleMappingChange("unit", selectedItem)
                }
              />
            </div>
            <Button
              kind="primary"
              onClick={() => setImportStep(3)}
              style={{ marginTop: "1rem" }}
              disabled={!columnMapping.labNo || !columnMapping.result}
            >
              <FormattedMessage
                id="medlab.result.import.continue"
                defaultMessage="Continue"
              />
            </Button>
          </div>
        );

      case 3:
        return (
          <div>
            <p style={{ marginBottom: "1rem", color: "#525252" }}>
              <FormattedMessage
                id="medlab.result.import.metadataInstructions"
                defaultMessage="Enter analyzer information for traceability."
              />
            </p>
            <TextInput
              id="analyzerName"
              labelText={intl.formatMessage({
                id: "medlab.result.import.analyzerName",
                defaultMessage: "Analyzer Name",
              })}
              value={importMetadata.analyzerName}
              onChange={(e) =>
                setImportMetadata((prev) => ({
                  ...prev,
                  analyzerName: e.target.value,
                }))
              }
              placeholder="e.g., Cobas C311"
              style={{ marginBottom: "1rem" }}
            />
            <TextInput
              id="runId"
              labelText={intl.formatMessage({
                id: "medlab.result.import.runId",
                defaultMessage: "Run ID / Batch ID",
              })}
              value={importMetadata.runId}
              onChange={(e) =>
                setImportMetadata((prev) => ({
                  ...prev,
                  runId: e.target.value,
                }))
              }
              placeholder="e.g., RUN-2024-001"
              style={{ marginBottom: "1rem" }}
            />
            {parseResult && (
              <div style={{ marginTop: "1rem" }}>
                <Tag type="blue">
                  {parseResult.totalRows}{" "}
                  <FormattedMessage
                    id="medlab.result.import.rowsFound"
                    defaultMessage="rows found"
                  />
                </Tag>
              </div>
            )}
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

  return (
    <div className="result-entry-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="medlab.page.resultEntry.title"
            defaultMessage="Result Entry"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="medlab.page.resultEntry.description"
            defaultMessage="Enter test results for samples that passed quality check. Machine-generated results can be imported from CSV."
          />
        </p>
      </div>

      {/* Error/Success Messages */}
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({ id: "error", defaultMessage: "Error" })}
          subtitle={error}
          onCloseButtonClick={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}
      {success && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({
            id: "success",
            defaultMessage: "Success",
          })}
          subtitle={success}
          onCloseButtonClick={() => setSuccess(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Progress Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.resultEntry.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{totalSamples}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.resultEntry.testsEntered"
                  defaultMessage="Tests Entered"
                />
              </span>
              <span className="progress-value">
                {enteredTests}/{totalTests}
              </span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.resultEntry.testsPending"
                  defaultMessage="Tests Pending"
                />
              </span>
              <span className="progress-value">{pendingTests}</span>
            </Tile>
            <Tile
              className={`progress-tile ${flaggedResults > 0 ? "rejected" : ""}`}
            >
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.resultEntry.flagged"
                  defaultMessage="Flagged Results"
                />
              </span>
              <span className="progress-value">{flaggedResults}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar" style={{ marginBottom: "1rem" }}>
        <Button
          kind="primary"
          size="md"
          renderIcon={Upload}
          onClick={() => setImportModalOpen(true)}
        >
          <FormattedMessage
            id="medlab.result.importResults"
            defaultMessage="Import Results"
          />
        </Button>
        <Button
          kind="secondary"
          size="md"
          renderIcon={Checkmark}
          onClick={handleMarkComplete}
          disabled={
            completing ||
            (completedSamples === 0 && selectedSampleIds.length === 0)
          }
        >
          {completing ? (
            <FormattedMessage
              id="medlab.result.completing"
              defaultMessage="Completing..."
            />
          ) : selectedSampleIds.length > 0 ? (
            <FormattedMessage
              id="medlab.result.markSelectedComplete"
              defaultMessage="Mark Selected Complete ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          ) : (
            <FormattedMessage
              id="medlab.result.markAllComplete"
              defaultMessage="Mark All Complete ({count})"
              values={{ count: completedSamples }}
            />
          )}
        </Button>
      </div>

      {/* Loading State */}
      {loading && <Loading />}

      {/* Tabs for Pending/Completed */}
      {!loading && samples.length > 0 && (
        <Tabs
          selectedIndex={activeTab}
          onChange={({ selectedIndex }) => setActiveTab(selectedIndex)}
        >
          <TabList aria-label="Result entry tabs">
            <Tab>
              <FormattedMessage
                id="medlab.result.tab.pending"
                defaultMessage="Pending Entry ({count})"
                values={{ count: pendingSamples + inProgressSamples }}
              />
            </Tab>
            <Tab>
              <FormattedMessage
                id="medlab.result.tab.completed"
                defaultMessage="Completed ({count})"
                values={{ count: completedSamples }}
              />
            </Tab>
          </TabList>

          <TabPanels>
            {/* Pending Tab */}
            <TabPanel>
              {samples
                .filter((s) => s.resultEntryStatus !== "COMPLETED")
                .map((sample) => (
                  <div
                    key={sample.id}
                    className="sample-card"
                    style={{ marginBottom: "1rem" }}
                  >
                    <Tile style={{ padding: "1rem" }}>
                      <Grid>
                        <Column lg={3} md={2} sm={2}>
                          <strong>
                            <FormattedMessage id="sample.label.labnumber" />:
                          </strong>{" "}
                          {sample.labNo}
                        </Column>
                        <Column lg={3} md={2} sm={2}>
                          <strong>
                            <FormattedMessage id="patient.label" />:
                          </strong>{" "}
                          {sample.patientName}
                        </Column>
                        <Column lg={3} md={2} sm={2}>
                          <strong>
                            <FormattedMessage
                              id="sample.sampleType"
                              defaultMessage="Sample Type"
                            />
                            :
                          </strong>{" "}
                          {sample.sampleType}
                        </Column>
                        <Column lg={3} md={2} sm={2}>
                          <Tag
                            type={getStatusTagType(sample.resultEntryStatus)}
                          >
                            {sample.enteredTests}/{sample.totalTests}{" "}
                            <FormattedMessage
                              id="medlab.result.entered"
                              defaultMessage="entered"
                            />
                          </Tag>
                        </Column>
                      </Grid>

                      {/* Tests Table */}
                      <div style={{ marginTop: "1rem" }}>
                        <DataTable
                          rows={(sample.tests || []).map((t, idx) => ({
                            ...t,
                            id: `${sample.id}-${t.testId}-${idx}`,
                          }))}
                          headers={[
                            {
                              key: "testName",
                              header: intl.formatMessage({
                                id: "test.testName",
                                defaultMessage: "Test",
                              }),
                            },
                            {
                              key: "resultValue",
                              header: intl.formatMessage({
                                id: "medlab.result.value",
                                defaultMessage: "Result",
                              }),
                            },
                            {
                              key: "unit",
                              header: intl.formatMessage({
                                id: "medlab.result.unit",
                                defaultMessage: "Unit",
                              }),
                            },
                            {
                              key: "referenceRange",
                              header: intl.formatMessage({
                                id: "medlab.result.referenceRange",
                                defaultMessage: "Reference Range",
                              }),
                            },
                            {
                              key: "flag",
                              header: intl.formatMessage({
                                id: "medlab.result.flag",
                                defaultMessage: "Flag",
                              }),
                            },
                            {
                              key: "resultStatus",
                              header: intl.formatMessage({
                                id: "medlab.result.status",
                                defaultMessage: "Status",
                              }),
                            },
                            {
                              key: "actions",
                              header: intl.formatMessage({
                                id: "label.button.actions",
                              }),
                            },
                          ]}
                          size="sm"
                        >
                          {({
                            rows,
                            headers,
                            getHeaderProps,
                            getTableProps,
                          }) => (
                            <TableContainer>
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
                                    const test =
                                      sample.tests.find(
                                        (t) =>
                                          `${sample.id}-${t.testId}` ===
                                          row.id.substring(
                                            0,
                                            row.id.lastIndexOf("-"),
                                          ),
                                      ) || sample.tests[rows.indexOf(row)];
                                    return (
                                      <TableRow key={row.id}>
                                        {row.cells.map((cell) => (
                                          <TableCell key={cell.id}>
                                            {cell.info.header === "actions" ? (
                                              <Button
                                                kind={
                                                  test?.resultStatus ===
                                                  "ENTERED"
                                                    ? "ghost"
                                                    : "primary"
                                                }
                                                size="sm"
                                                renderIcon={
                                                  test?.resultStatus ===
                                                  "ENTERED"
                                                    ? Edit
                                                    : Save
                                                }
                                                onClick={() =>
                                                  handleOpenResultModal(
                                                    sample,
                                                    test,
                                                  )
                                                }
                                              >
                                                {test?.resultStatus ===
                                                "ENTERED" ? (
                                                  <FormattedMessage
                                                    id="medlab.result.edit"
                                                    defaultMessage="Edit"
                                                  />
                                                ) : (
                                                  <FormattedMessage
                                                    id="medlab.result.enter"
                                                    defaultMessage="Enter"
                                                  />
                                                )}
                                              </Button>
                                            ) : cell.info.header ===
                                              "resultStatus" ? (
                                              <Tag
                                                type={
                                                  cell.value === "ENTERED"
                                                    ? "green"
                                                    : "gray"
                                                }
                                                size="sm"
                                              >
                                                {cell.value}
                                              </Tag>
                                            ) : cell.info.header === "flag" ? (
                                              cell.value ? (
                                                <Tag
                                                  type={getFlagTagType(
                                                    cell.value,
                                                  )}
                                                  size="sm"
                                                >
                                                  {getFlagDisplay(cell.value)}
                                                </Tag>
                                              ) : (
                                                "-"
                                              )
                                            ) : cell.info.header ===
                                              "resultValue" ? (
                                              cell.value || "-"
                                            ) : cell.info.header === "unit" ? (
                                              cell.value || "-"
                                            ) : cell.info.header ===
                                              "referenceRange" ? (
                                              cell.value || "-"
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
                            </TableContainer>
                          )}
                        </DataTable>
                      </div>
                    </Tile>
                  </div>
                ))}
              {samples.filter((s) => s.resultEntryStatus !== "COMPLETED")
                .length === 0 && (
                <div className="empty-state">
                  <CheckmarkFilled
                    size={48}
                    style={{ color: "#24a148", marginBottom: "1rem" }}
                  />
                  <p>
                    <FormattedMessage
                      id="medlab.result.allEntered"
                      defaultMessage="All results have been entered!"
                    />
                  </p>
                </div>
              )}
            </TabPanel>

            {/* Completed Tab */}
            <TabPanel>
              {samples
                .filter((s) => s.resultEntryStatus === "COMPLETED")
                .map((sample) => (
                  <div
                    key={sample.id}
                    className="sample-card"
                    style={{ marginBottom: "1rem" }}
                  >
                    <Tile style={{ padding: "1rem" }}>
                      <Grid>
                        <Column lg={3} md={2} sm={2}>
                          <strong>
                            <FormattedMessage id="sample.label.labnumber" />:
                          </strong>{" "}
                          {sample.labNo}
                        </Column>
                        <Column lg={3} md={2} sm={2}>
                          <strong>
                            <FormattedMessage id="patient.label" />:
                          </strong>{" "}
                          {sample.patientName}
                        </Column>
                        <Column lg={3} md={2} sm={2}>
                          <strong>
                            <FormattedMessage
                              id="sample.sampleType"
                              defaultMessage="Sample Type"
                            />
                            :
                          </strong>{" "}
                          {sample.sampleType}
                        </Column>
                        <Column lg={3} md={2} sm={2}>
                          <Tag type="green">
                            <CheckmarkFilled
                              size={16}
                              style={{ marginRight: "4px" }}
                            />
                            {sample.totalTests}{" "}
                            <FormattedMessage
                              id="medlab.result.testsComplete"
                              defaultMessage="tests complete"
                            />
                          </Tag>
                        </Column>
                      </Grid>

                      {/* Tests summary */}
                      <div style={{ marginTop: "1rem" }}>
                        <DataTable
                          rows={(sample.tests || []).map((t, idx) => ({
                            ...t,
                            id: `${sample.id}-${t.testId}-${idx}`,
                          }))}
                          headers={[
                            {
                              key: "testName",
                              header: intl.formatMessage({
                                id: "test.testName",
                                defaultMessage: "Test",
                              }),
                            },
                            {
                              key: "resultValue",
                              header: intl.formatMessage({
                                id: "medlab.result.value",
                                defaultMessage: "Result",
                              }),
                            },
                            {
                              key: "unit",
                              header: intl.formatMessage({
                                id: "medlab.result.unit",
                                defaultMessage: "Unit",
                              }),
                            },
                            {
                              key: "referenceRange",
                              header: intl.formatMessage({
                                id: "medlab.result.referenceRange",
                                defaultMessage: "Reference Range",
                              }),
                            },
                            {
                              key: "flag",
                              header: intl.formatMessage({
                                id: "medlab.result.flag",
                                defaultMessage: "Flag",
                              }),
                            },
                          ]}
                          size="sm"
                        >
                          {({
                            rows,
                            headers,
                            getHeaderProps,
                            getTableProps,
                          }) => (
                            <TableContainer>
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
                                  {rows.map((row) => (
                                    <TableRow key={row.id}>
                                      {row.cells.map((cell) => (
                                        <TableCell key={cell.id}>
                                          {cell.info.header === "flag" ? (
                                            cell.value ? (
                                              <Tag
                                                type={getFlagTagType(
                                                  cell.value,
                                                )}
                                                size="sm"
                                              >
                                                {getFlagDisplay(cell.value)}
                                              </Tag>
                                            ) : (
                                              "-"
                                            )
                                          ) : (
                                            cell.value || "-"
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
                      </div>
                    </Tile>
                  </div>
                ))}
              {samples.filter((s) => s.resultEntryStatus === "COMPLETED")
                .length === 0 && (
                <div className="empty-state">
                  <p>
                    <FormattedMessage
                      id="medlab.result.noCompleted"
                      defaultMessage="No completed samples yet."
                    />
                  </p>
                </div>
              )}
            </TabPanel>
          </TabPanels>
        </Tabs>
      )}

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <WarningAlt
            size={48}
            style={{ color: "#f1c21b", marginBottom: "1rem" }}
          />
          <p>
            <FormattedMessage
              id="medlab.page.resultEntry.empty"
              defaultMessage="No samples available for result entry. Complete the Testing & Analyzer page first."
            />
          </p>
        </div>
      )}

      {/* Result Entry Modal */}
      <Modal
        open={resultModalOpen}
        onRequestClose={() => setResultModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "medlab.page.resultEntry.enterResult",
          defaultMessage: "Enter Test Result",
        })}
        primaryButtonText={intl.formatMessage({
          id: "label.button.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
        onRequestSubmit={handleSubmitResult}
        primaryButtonDisabled={saving || !resultValue.trim()}
        size="md"
      >
        <Grid>
          {selectedSample && selectedTest && (
            <>
              <Column lg={16} md={8} sm={4}>
                <Tile
                  className="order-info-tile"
                  style={{ marginBottom: "1rem" }}
                >
                  <strong>
                    <FormattedMessage id="sample.label.labnumber" />:
                  </strong>{" "}
                  {selectedSample.labNo}
                  <br />
                  <strong>
                    <FormattedMessage id="patient.label" />:
                  </strong>{" "}
                  {selectedSample.patientName}
                  <br />
                  <strong>
                    <FormattedMessage
                      id="test.testName"
                      defaultMessage="Test"
                    />
                    :
                  </strong>{" "}
                  {selectedTest.testName}
                  {selectedTest.referenceRange && (
                    <>
                      <br />
                      <strong>
                        <FormattedMessage
                          id="medlab.result.referenceRange"
                          defaultMessage="Reference Range"
                        />
                        :
                      </strong>{" "}
                      {selectedTest.referenceRange} {selectedTest.unit}
                    </>
                  )}
                </Tile>
              </Column>
              <Column lg={16} md={8} sm={4}>
                <TextInput
                  id="result-value"
                  labelText={intl.formatMessage({
                    id: "medlab.result.value",
                    defaultMessage: "Result Value *",
                  })}
                  value={resultValue}
                  onChange={(e) => setResultValue(e.target.value)}
                  placeholder={intl.formatMessage({
                    id: "medlab.result.value.placeholder",
                    defaultMessage: "Enter result value",
                  })}
                  helperText={
                    selectedTest.unit ? `Unit: ${selectedTest.unit}` : ""
                  }
                  style={{ marginBottom: "1rem" }}
                />
              </Column>
              <Column lg={16} md={8} sm={4}>
                <Dropdown
                  id="entry-type"
                  titleText={intl.formatMessage({
                    id: "medlab.result.entryType",
                    defaultMessage: "Entry Type",
                  })}
                  label="Select entry type"
                  items={entryTypeOptions}
                  itemToString={(item) => (item ? item.label : "")}
                  selectedItem={entryTypeOptions.find(
                    (t) => t.id === entryType,
                  )}
                  onChange={({ selectedItem }) =>
                    setEntryType(selectedItem?.id || "MANUAL")
                  }
                  style={{ marginBottom: "1rem" }}
                />
              </Column>
              <Column lg={16} md={8} sm={4}>
                <TextArea
                  id="result-notes"
                  labelText={intl.formatMessage({
                    id: "medlab.result.notes",
                    defaultMessage: "Notes (optional)",
                  })}
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  rows={3}
                  placeholder={intl.formatMessage({
                    id: "medlab.result.notes.placeholder",
                    defaultMessage: "Any additional notes about this result...",
                  })}
                />
              </Column>
            </>
          )}
        </Grid>
        {saving && (
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
                id="medlab.result.saving"
                defaultMessage="Saving result..."
              />
            </span>
          </div>
        )}
      </Modal>

      {/* Import Modal */}
      <Modal
        open={importModalOpen}
        onRequestClose={() => {
          setImportModalOpen(false);
          resetImportState();
        }}
        onRequestSubmit={handleExecuteImport}
        modalHeading={intl.formatMessage({
          id: "medlab.result.import.title",
          defaultMessage: "Import Analyzer Results",
        })}
        primaryButtonText={
          importStep === 3
            ? intl.formatMessage({
                id: "medlab.result.import.import",
                defaultMessage: "Import",
              })
            : undefined
        }
        primaryButtonDisabled={importStep !== 3 || importing}
        secondaryButtonText={intl.formatMessage({
          id: "medlab.result.import.cancel",
          defaultMessage: "Cancel",
        })}
        size="lg"
      >
        <div style={{ minHeight: "300px" }}>
          {/* Step indicator */}
          <div
            style={{
              display: "flex",
              marginBottom: "1.5rem",
              borderBottom: "1px solid #e0e0e0",
              paddingBottom: "1rem",
            }}
          >
            {[1, 2, 3].map((step) => (
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
                {step === 3 && "Metadata"}
              </div>
            ))}
          </div>

          {renderImportStep()}
        </div>
      </Modal>
    </div>
  );
}

export default ResultEntryPage;

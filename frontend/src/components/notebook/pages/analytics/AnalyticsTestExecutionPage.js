import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Loading,
  Modal,
  TextInput,
  TextArea,
  NumberInput,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  TimePicker,
  Accordion,
  AccordionItem,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectRow,
  TableSelectAll,
  TableContainer,
  Tag,
  Toggle,
} from "@carbon/react";
import { Checkmark, Edit, DocumentAttachment } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import "../../workflow/NotebookWorkflow.css";

/**
 * AnalyticsTestExecutionPage - Page 3 of the Analytics Laboratory workflow.
 * Purpose: Perform analytical testing while preserving data integrity.
 *
 * Data Points:
 * - Test start date & time, Instrument used, Method reference/SOP, Analyst
 * - Chromatographic/Spectroscopic: Run ID, System suitability, Calibration, Raw data ref
 * - Dissolution: Apparatus type, RPM, Medium, Time points
 * - Physical Tests: Individual values, Average, Acceptance decision
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID
 */
function AnalyticsTestExecutionPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookId,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Modal state for test execution
  const [executeModalOpen, setExecuteModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [testType, setTestType] = useState("chromatographic");

  // Form state for test execution
  const [formData, setFormData] = useState({
    // Common fields
    testStartDate: "",
    testStartTime: "",
    instrumentUsed: "",
    methodReference: "",
    analystPerforming: "",
    // Chromatographic/Spectroscopic fields
    runId: "",
    systemSuitabilityStatus: "",
    calibrationStatus: "",
    rawDataFileRef: "",
    // Dissolution fields
    apparatusType: "",
    rpm: "",
    medium: "",
    timePoints: "",
    // Physical test fields
    individualValues: "",
    average: "",
    acceptanceDecision: "",
  });

  // Options for dropdowns
  const systemSuitabilityOptions = [
    { value: "PASS", label: "Pass" },
    { value: "FAIL", label: "Fail" },
  ];

  const calibrationStatusOptions = [
    { value: "VALID", label: "Valid" },
    { value: "EXPIRED", label: "Expired" },
    { value: "NOT_REQUIRED", label: "Not Required" },
  ];

  const apparatusTypeOptions = [
    { value: "USP_I", label: "USP I (Basket)" },
    { value: "USP_II", label: "USP II (Paddle)" },
  ];

  const acceptanceDecisionOptions = [
    { value: "PASS", label: "Pass" },
    { value: "FAIL", label: "Fail" },
  ];

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
              status: sample.pageStatus || "PENDING",
              // Test execution fields
              testStartDateTime: sample.data?.testStartDateTime,
              instrumentUsed: sample.data?.instrumentUsed,
              methodReference: sample.data?.methodReference,
              analystPerforming: sample.data?.analystPerforming,
              analysisCompleted: sample.data?.analysisCompleted,
              // Methodology from assignment
              analyticalMethodology: sample.data?.analyticalMethodology || [],
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

  // Handle form input changes
  const handleFormChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  // Reset form
  const resetForm = () => {
    setFormData({
      testStartDate: "",
      testStartTime: "",
      instrumentUsed: "",
      methodReference: "",
      analystPerforming: "",
      runId: "",
      systemSuitabilityStatus: "",
      calibrationStatus: "",
      rawDataFileRef: "",
      apparatusType: "",
      rpm: "",
      medium: "",
      timePoints: "",
      individualValues: "",
      average: "",
      acceptanceDecision: "",
    });
    setTestType("chromatographic");
  };

  // Submit test execution data
  const handleSubmitExecution = async () => {
    if (!entryId || !pageData?.id) {
      setError("Cannot save: Page not properly initialized.");
      return;
    }

    if (selectedSampleIds.length === 0) {
      setError("Please select at least one sample.");
      return;
    }

    // Validate required fields
    if (
      !formData.testStartDate ||
      !formData.instrumentUsed ||
      !formData.methodReference ||
      !formData.analystPerforming
    ) {
      setError("Please fill in all required fields.");
      return;
    }

    setIsSubmitting(true);
    setError(null);

    const testStartDateTime = formData.testStartTime
      ? `${formData.testStartDate} ${formData.testStartTime}`
      : formData.testStartDate;

    const executionData = {
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      testStartDateTime: testStartDateTime,
      instrumentUsed: formData.instrumentUsed,
      methodReference: formData.methodReference,
      analystPerforming: formData.analystPerforming,
      testType: testType,
      // Chromatographic data
      runId: formData.runId,
      systemSuitabilityStatus: formData.systemSuitabilityStatus,
      calibrationStatus: formData.calibrationStatus,
      rawDataFileRef: formData.rawDataFileRef,
      // Dissolution data
      apparatusType: formData.apparatusType,
      rpm: formData.rpm,
      medium: formData.medium,
      timePoints: formData.timePoints,
      // Physical test data
      individualValues: formData.individualValues,
      average: formData.average,
      acceptanceDecision: formData.acceptanceDecision,
      status: "ANALYSIS_COMPLETED",
    };

    postToOpenElisServer(
      `/rest/notebook/analytics/entry/${entryId}/page/${pageData.id}/samples/execute`,
      JSON.stringify(executionData),
      (status, response) => {
        setIsSubmitting(false);
        if (status === 200 || status === 201) {
          setExecuteModalOpen(false);
          resetForm();
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            response?.error ||
              "Failed to save test execution data. Please try again.",
          );
        }
      },
    );
  };

  // Handle bulk mark as completed
  const handleBulkMarkCompleted = useCallback(() => {
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

  // Calculate stats
  const completedCount = samples.filter(
    (s) => s.status === "COMPLETED" || s.status === "ANALYSIS_COMPLETED",
  ).length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;
  const inProgressCount = samples.filter(
    (s) => s.status === "IN_PROGRESS",
  ).length;

  // Table headers
  const tableHeaders = [
    { key: "externalId", header: "Sample Identifier" },
    { key: "sampleType", header: "Sample Type" },
    { key: "instrumentUsed", header: "Instrument" },
    { key: "methodReference", header: "Method/SOP" },
    { key: "analystPerforming", header: "Analyst" },
    { key: "testStartDateTime", header: "Test Started" },
    { key: "status", header: "Status" },
  ];

  return (
    <div className="analytics-test-execution-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.analytics.testExecution.title"
            defaultMessage="Analysis / Test Execution"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.analytics.testExecution.description"
            defaultMessage="Perform analytical testing while preserving data integrity. Record test details, instrument data, and results for each sample."
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
                  id="notebook.page.analytics.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.analytics.analysisCompleted"
                  defaultMessage="Analysis Completed"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.analytics.pendingAnalysis"
                  defaultMessage="Pending Analysis"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        {selectedSampleIds.length > 0 && (
          <>
            <Button
              kind="primary"
              size="sm"
              renderIcon={Edit}
              onClick={() => setExecuteModalOpen(true)}
            >
              <FormattedMessage
                id="notebook.page.analytics.recordExecution"
                defaultMessage="Record Test Execution ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>

            <Button
              kind="secondary"
              size="sm"
              renderIcon={Checkmark}
              onClick={handleBulkMarkCompleted}
            >
              <FormattedMessage
                id="notebook.page.analytics.markAnalysisComplete"
                defaultMessage="Mark Analysis Complete ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
          </>
        )}
      </div>

      {/* Error Display */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onCloseButtonClick={() => setError(null)}
          lowContrast
        />
      )}

      {/* Samples Table */}
      <div className="sample-grid-container">
        {loading ? (
          <Loading description="Loading samples..." withOverlay={false} />
        ) : samples.length === 0 ? (
          <div className="empty-state">
            <p>
              <FormattedMessage
                id="notebook.page.analytics.testExecution.empty"
                defaultMessage="No samples available for analysis. Samples must be assigned in the Test Assignment page first."
              />
            </p>
          </div>
        ) : (
          <DataTable rows={samples} headers={tableHeaders} isSortable>
            {({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
              getSelectionProps,
            }) => (
              <TableContainer>
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      <TableSelectAll
                        {...getSelectionProps()}
                        onSelect={() => {
                          if (selectedSampleIds.length === samples.length) {
                            setSelectedSampleIds([]);
                          } else {
                            setSelectedSampleIds(samples.map((s) => s.id));
                          }
                        }}
                        checked={selectedSampleIds.length === samples.length}
                        indeterminate={
                          selectedSampleIds.length > 0 &&
                          selectedSampleIds.length < samples.length
                        }
                      />
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
                          checked={selectedSampleIds.includes(row.id)}
                          onSelect={() => {
                            if (selectedSampleIds.includes(row.id)) {
                              setSelectedSampleIds(
                                selectedSampleIds.filter((id) => id !== row.id),
                              );
                            } else {
                              setSelectedSampleIds([
                                ...selectedSampleIds,
                                row.id,
                              ]);
                            }
                          }}
                        />
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>
                            {cell.info.header === "status" ? (
                              <Tag
                                type={
                                  cell.value === "COMPLETED" ||
                                  cell.value === "ANALYSIS_COMPLETED"
                                    ? "green"
                                    : cell.value === "PENDING"
                                      ? "gray"
                                      : cell.value === "IN_PROGRESS"
                                        ? "blue"
                                        : "purple"
                                }
                                size="sm"
                              >
                                {cell.value === "ANALYSIS_COMPLETED"
                                  ? "Pending Review"
                                  : cell.value}
                              </Tag>
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
        )}
      </div>

      {/* Test Execution Modal */}
      <Modal
        open={executeModalOpen}
        onRequestClose={() => {
          setExecuteModalOpen(false);
          resetForm();
          setError(null);
        }}
        modalHeading={intl.formatMessage({
          id: "notebook.analytics.testExecution.title",
          defaultMessage: `Record Test Execution for ${selectedSampleIds.length} Sample(s)`,
        })}
        primaryButtonText={intl.formatMessage({
          id: "label.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleSubmitExecution}
        onSecondarySubmit={() => {
          setExecuteModalOpen(false);
          resetForm();
          setError(null);
        }}
        size="lg"
        primaryButtonDisabled={isSubmitting}
      >
        <Grid fullWidth>
          {/* Common Test Execution Fields */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem", marginTop: "1rem" }}>
              <FormattedMessage
                id="notebook.analytics.section.testExecution"
                defaultMessage="Test Execution Details"
              />
            </h5>
          </Column>

          <Column lg={4} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) => {
                if (dates && dates[0]) {
                  const date = dates[0];
                  const formatted = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
                  handleFormChange("testStartDate", formatted);
                }
              }}
            >
              <DatePickerInput
                id="testStartDate"
                labelText={intl.formatMessage({
                  id: "notebook.analytics.field.testStartDate",
                  defaultMessage: "Test start date *",
                })}
                placeholder="yyyy-mm-dd"
              />
            </DatePicker>
          </Column>

          <Column lg={4} md={4} sm={4}>
            <TimePicker
              id="testStartTime"
              labelText={intl.formatMessage({
                id: "notebook.analytics.field.testStartTime",
                defaultMessage: "Test start time",
              })}
              value={formData.testStartTime}
              onChange={(e) =>
                handleFormChange("testStartTime", e.target.value)
              }
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="instrumentUsed"
              labelText={intl.formatMessage({
                id: "notebook.analytics.field.instrumentUsed",
                defaultMessage: "Instrument used *",
              })}
              placeholder="Enter instrument name/ID"
              value={formData.instrumentUsed}
              onChange={(e) =>
                handleFormChange("instrumentUsed", e.target.value)
              }
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="methodReference"
              labelText={intl.formatMessage({
                id: "notebook.analytics.field.methodReference",
                defaultMessage: "Method reference / SOP *",
              })}
              placeholder="Enter method reference or SOP number"
              value={formData.methodReference}
              onChange={(e) =>
                handleFormChange("methodReference", e.target.value)
              }
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="analystPerforming"
              labelText={intl.formatMessage({
                id: "notebook.analytics.field.analystPerforming",
                defaultMessage: "Analyst performing test *",
              })}
              placeholder="Enter analyst name"
              value={formData.analystPerforming}
              onChange={(e) =>
                handleFormChange("analystPerforming", e.target.value)
              }
            />
          </Column>

          {/* Test Type Selection */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem", marginTop: "1.5rem" }}>
              <FormattedMessage
                id="notebook.analytics.section.instrumentData"
                defaultMessage="Instrument-Specific Data"
              />
            </h5>
            <Select
              id="testType"
              labelText="Test Type"
              value={testType}
              onChange={(e) => setTestType(e.target.value)}
            >
              <SelectItem
                value="chromatographic"
                text="Chromatographic / Spectroscopic (HPLC, UV-Vis, LC-MS/MS)"
              />
              <SelectItem value="dissolution" text="Dissolution" />
              <SelectItem
                value="physical"
                text="Physical Tests (Hardness, Friability, Disintegration)"
              />
            </Select>
          </Column>

          {/* Chromatographic/Spectroscopic Fields */}
          {testType === "chromatographic" && (
            <>
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="runId"
                  labelText="Run ID"
                  placeholder="Enter run ID"
                  value={formData.runId}
                  onChange={(e) => handleFormChange("runId", e.target.value)}
                />
              </Column>

              <Column lg={8} md={4} sm={4}>
                <Select
                  id="systemSuitabilityStatus"
                  labelText="System suitability status"
                  value={formData.systemSuitabilityStatus}
                  onChange={(e) =>
                    handleFormChange("systemSuitabilityStatus", e.target.value)
                  }
                >
                  <SelectItem value="" text="Select status..." />
                  {systemSuitabilityOptions.map((option) => (
                    <SelectItem
                      key={option.value}
                      value={option.value}
                      text={option.label}
                    />
                  ))}
                </Select>
              </Column>

              <Column lg={8} md={4} sm={4}>
                <Select
                  id="calibrationStatus"
                  labelText="Calibration status"
                  value={formData.calibrationStatus}
                  onChange={(e) =>
                    handleFormChange("calibrationStatus", e.target.value)
                  }
                >
                  <SelectItem value="" text="Select status..." />
                  {calibrationStatusOptions.map((option) => (
                    <SelectItem
                      key={option.value}
                      value={option.value}
                      text={option.label}
                    />
                  ))}
                </Select>
              </Column>

              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="rawDataFileRef"
                  labelText="Raw data file reference"
                  placeholder="Enter file reference/path"
                  value={formData.rawDataFileRef}
                  onChange={(e) =>
                    handleFormChange("rawDataFileRef", e.target.value)
                  }
                />
              </Column>
            </>
          )}

          {/* Dissolution Fields */}
          {testType === "dissolution" && (
            <>
              <Column lg={8} md={4} sm={4}>
                <Select
                  id="apparatusType"
                  labelText="Apparatus type"
                  value={formData.apparatusType}
                  onChange={(e) =>
                    handleFormChange("apparatusType", e.target.value)
                  }
                >
                  <SelectItem value="" text="Select apparatus..." />
                  {apparatusTypeOptions.map((option) => (
                    <SelectItem
                      key={option.value}
                      value={option.value}
                      text={option.label}
                    />
                  ))}
                </Select>
              </Column>

              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="rpm"
                  labelText="RPM"
                  placeholder="e.g., 50, 100"
                  value={formData.rpm}
                  onChange={(e) => handleFormChange("rpm", e.target.value)}
                />
              </Column>

              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="medium"
                  labelText="Medium"
                  placeholder="e.g., 0.1N HCl, pH 6.8 buffer"
                  value={formData.medium}
                  onChange={(e) => handleFormChange("medium", e.target.value)}
                />
              </Column>

              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="timePoints"
                  labelText="Time points"
                  placeholder="e.g., 5, 10, 15, 30, 45, 60 min"
                  value={formData.timePoints}
                  onChange={(e) =>
                    handleFormChange("timePoints", e.target.value)
                  }
                />
              </Column>
            </>
          )}

          {/* Physical Test Fields */}
          {testType === "physical" && (
            <>
              <Column lg={16} md={8} sm={4}>
                <TextArea
                  id="individualValues"
                  labelText="Individual values"
                  placeholder="Enter individual test values (comma-separated)"
                  value={formData.individualValues}
                  onChange={(e) =>
                    handleFormChange("individualValues", e.target.value)
                  }
                  rows={3}
                />
              </Column>

              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="average"
                  labelText="Average"
                  placeholder="Enter calculated average"
                  value={formData.average}
                  onChange={(e) => handleFormChange("average", e.target.value)}
                />
              </Column>

              <Column lg={8} md={4} sm={4}>
                <Select
                  id="acceptanceDecision"
                  labelText="Acceptance decision"
                  value={formData.acceptanceDecision}
                  onChange={(e) =>
                    handleFormChange("acceptanceDecision", e.target.value)
                  }
                >
                  <SelectItem value="" text="Select decision..." />
                  {acceptanceDecisionOptions.map((option) => (
                    <SelectItem
                      key={option.value}
                      value={option.value}
                      text={option.label}
                    />
                  ))}
                </Select>
              </Column>
            </>
          )}
        </Grid>
      </Modal>
    </div>
  );
}

export default AnalyticsTestExecutionPage;

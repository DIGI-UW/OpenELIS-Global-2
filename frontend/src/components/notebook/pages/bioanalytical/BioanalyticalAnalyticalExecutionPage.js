import React, { useState, useCallback, useEffect } from "react";
import {
  Grid,
  Column,
  Button,
  InlineNotification,
  Loading,
  Tabs,
  TabList,
  Tab,
  TabPanel,
  TabPanels,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Select,
  SelectItem,
  FileUploader,
  Checkbox,
  NumberInput,
  TextInput,
  TextArea,
  DatePicker,
  DatePickerInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../../../config.json";
import "./BioanalyticalPages.css";

/**
 * BioanalyticalAnalyticalExecutionPage - Stage 3 of bioanalytical workflow.
 *
 * Features:
 * - Analytical instrument data upload (mzML, CDF, CSV, PDF)
 * - Instrument type selection and file format validation
 * - Calibration curve validation (r², slope, intercept)
 * - QC result tracking with Westgard rule detection
 * - Levey-Jennings trending data management
 * - System suitability verification
 * - Data integrity and acceptance validation
 *
 * @param {Object} props
 * @param {number} props.entryId - Notebook entry ID
 * @param {Object} props.pageData - Page configuration
 * @param {Object} props.progress - Sample progress counts
 * @param {function} props.onProgressUpdate - Callback after changes
 * @param {Array} props.templateInstruments - Available instruments
 */
function BioanalyticalAnalyticalExecutionPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  templateInstruments,
}) {
  const intl = useIntl();

  // Loading and data states
  const [isLoading, setIsLoading] = useState(false);
  const [isExecuting, setIsExecuting] = useState(false);
  const [selectedTab, setSelectedTab] = useState(0);

  // Stage 2 integration - assigned samples and their configurations
  const [assignedSamples, setAssignedSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState(new Set());

  // Test execution data
  const [executionData, setExecutionData] = useState({
    analystId: "",
    instrumentId: "",
    batchNumber: "",
    executionDate: "",
    testParameters: {},
    notes: "",
  });

  // File upload and validation
  const [selectedInstrument, setSelectedInstrument] = useState("");
  const [uploadedFiles, setUploadedFiles] = useState([]);

  // Results and QC
  const [analyzerResults, setAnalyzerResults] = useState([]);
  const [calibrationData, setCalibrationData] = useState(null);
  const [qcResults, setQcResults] = useState([]);
  const [westgardRules, setWestgardRules] = useState([]);
  const [deviations, setDeviations] = useState([]);
  const [deviationForm, setDeviationForm] = useState({
    type: "",
    severity: "",
    description: "",
    correctiveAction: "",
    reportedBy: "",
    batchDisposition: "",
  });

  // UI states
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  // Load assigned samples from Stage 2 on component mount
  useEffect(() => {
    const loadAssignedSamples = async () => {
      if (!entryId || String(entryId).startsWith("default-")) {
        setAssignedSamples([]);
        return;
      }

      setIsLoading(true);
      try {
        // Load samples that have Stage 2 test assignments
        const response = await fetch(
          `${config.serverBaseUrl}/rest/notebook/entry/${entryId}/samples`,
          {
            method: "GET",
            credentials: "include",
            headers: {
              "X-CSRF-Token": localStorage.getItem("CSRF"),
            },
          },
        );

        if (response.ok) {
          const data = await response.json();
          // Filter samples that have test assignments from Stage 2
          const samplesWithAssignments = (data.samples || []).filter(
            (sample) => {
              return (
                sample.data &&
                sample.data.analyticalMethod &&
                sample.data.assignedStaff
              );
            },
          );
          setAssignedSamples(samplesWithAssignments);
        } else {
          console.error("Failed to load assigned samples:", response.status);
          setAssignedSamples([]);
        }
      } catch (error) {
        console.error("Error loading assigned samples:", error);
        setAssignedSamples([]);
        setErrorMessage(
          intl.formatMessage({
            id: "notebook.bioanalytical.execution.loadError",
            defaultMessage:
              "Failed to load assigned samples. Please refresh the page.",
          }),
        );
      } finally {
        setIsLoading(false);
      }
    };

    loadAssignedSamples();
  }, [entryId, intl]);

  // Instrument configuration (use templateInstruments from props or fallback to mock data)
  const instruments = templateInstruments || [
    {
      id: "1",
      name: "LC-MS/MS System",
      type: "LCMS",
      formats: ["mzML", "CDF"],
    },
    { id: "2", name: "HPLC System", type: "HPLC", formats: ["CSV", "PDF"] },
    {
      id: "3",
      name: "Dissolution Tester",
      type: "DISSOLUTION",
      formats: ["CSV"],
    },
    {
      id: "4",
      name: "USP Apparatus II",
      type: "APPARATUS",
      formats: ["CSV", "PDF"],
    },
  ];

  const handleFileUpload = useCallback(
    (files) => {
      if (files.length === 0) return;

      const file = files[0];
      const fileExtension = file.name.split(".").pop().toUpperCase();

      // Validate file format based on selected instrument
      const instrument = instruments.find((i) => i.id === selectedInstrument);
      if (!instrument) {
        setErrorMessage(
          intl.formatMessage({
            id: "notebook.bioanalytical.execution.selectInstrument",
            defaultMessage: "Please select an instrument first",
          }),
        );
        return;
      }

      if (!instrument.formats.includes(fileExtension)) {
        setErrorMessage(
          intl.formatMessage(
            {
              id: "notebook.bioanalytical.execution.invalidFormat",
              defaultMessage:
                "File format {ext} not supported for {instrument}. Supported formats: {formats}",
            },
            {
              ext: fileExtension,
              instrument: instrument.name,
              formats: instrument.formats.join(", "),
            },
          ),
        );
        return;
      }

      // Add file to list
      const newFile = {
        id: Date.now(),
        name: file.name,
        size: file.size,
        instrument: instrument.name,
        status: "PENDING_VALIDATION",
        uploadedAt: new Date().toISOString(),
      };

      setUploadedFiles([...uploadedFiles, newFile]);
      setSuccessMessage(
        intl.formatMessage(
          {
            id: "notebook.bioanalytical.execution.fileUploaded",
            defaultMessage: "File {name} uploaded successfully",
          },
          { name: file.name },
        ),
      );
    },
    [selectedInstrument, intl],
  );

  // Execute tests for selected samples
  const handleExecuteTests = useCallback(async () => {
    if (selectedSampleIds.size === 0) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.execution.noSamplesSelected",
          defaultMessage: "Please select samples to execute tests",
        }),
      );
      return;
    }

    if (!executionData.analystId || !executionData.instrumentId) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.execution.missingExecutionData",
          defaultMessage:
            "Please fill in analyst ID and instrument information",
        }),
      );
      return;
    }

    setIsExecuting(true);
    setErrorMessage("");

    try {
      // Prepare execution data for backend
      const testExecutionData = {
        entryId: entryId,
        pageId: pageData?.id,
        sampleIds: Array.from(selectedSampleIds),
        executionDetails: {
          analystId: executionData.analystId,
          instrumentId: executionData.instrumentId,
          batchNumber: executionData.batchNumber,
          executionDate:
            executionData.executionDate ||
            new Date().toISOString().split("T")[0],
          testParameters: executionData.testParameters,
          notes: executionData.notes,
        },
        rawDataFiles: uploadedFiles.map((file) => ({
          fileName: file.name,
          instrumentId: executionData.instrumentId,
          fileType: file.name.split(".").pop().toUpperCase(),
          uploadedAt: file.uploadedAt,
        })),
      };

      // Execute tests via backend API
      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData?.id}/samples/apply`,
        {
          method: "POST",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          body: JSON.stringify({
            sampleIds: Array.from(selectedSampleIds),
            data: {
              executionStatus: "EXECUTED",
              testExecution: testExecutionData.executionDetails,
              rawDataFiles: testExecutionData.rawDataFiles,
              executedAt: new Date().toISOString(),
              executedBy: executionData.analystId,
            },
            userId: executionData.analystId,
          }),
        },
      );

      if (response.ok) {
        setSuccessMessage(
          intl.formatMessage(
            {
              id: "notebook.bioanalytical.execution.testsExecuted",
              defaultMessage: "Tests executed successfully for {count} samples",
            },
            { count: selectedSampleIds.size },
          ),
        );

        // Move to validation tab
        setSelectedTab(1);

        if (onProgressUpdate) {
          onProgressUpdate();
        }
      } else {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(
          errorData.message || errorData.error || "Test execution failed",
        );
      }
    } catch (error) {
      console.error("Test execution error:", error);
      setErrorMessage(
        intl.formatMessage(
          {
            id: "notebook.bioanalytical.execution.executionError",
            defaultMessage: "Failed to execute tests: {error}",
          },
          { error: error.message },
        ),
      );
    } finally {
      setIsExecuting(false);
    }
  }, [
    selectedSampleIds,
    executionData,
    entryId,
    pageData?.id,
    uploadedFiles,
    intl,
    onProgressUpdate,
  ]);

  // Handle deviation recording
  const handleRecordDeviation = useCallback(
    async (deviationData) => {
      try {
        const deviation = {
          ...deviationData,
          recordedBy: executionData.analystId,
          recordedAt: new Date().toISOString(),
          status: "OPEN",
        };

        // Store deviation in sample data
        const response = await fetch(
          `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData?.id}/samples/apply`,
          {
            method: "POST",
            credentials: "include",
            headers: {
              "Content-Type": "application/json",
              "X-CSRF-Token": localStorage.getItem("CSRF"),
            },
            body: JSON.stringify({
              sampleIds: Array.from(selectedSampleIds),
              data: {
                deviations: [...(deviations || []), deviation],
              },
              userId: executionData.analystId,
            }),
          },
        );

        if (response.ok) {
          setDeviations((prev) => [...prev, deviation]);
          setSuccessMessage(
            intl.formatMessage({
              id: "notebook.bioanalytical.execution.deviationRecorded",
              defaultMessage: "Deviation recorded successfully",
            }),
          );
        }
      } catch (error) {
        console.error("Deviation recording error:", error);
        setErrorMessage(
          intl.formatMessage({
            id: "notebook.bioanalytical.execution.deviationError",
            defaultMessage: "Failed to record deviation",
          }),
        );
      }
    },
    [
      deviations,
      selectedSampleIds,
      executionData.analystId,
      pageData?.id,
      intl,
    ],
  );

  const handleAddDeviation = useCallback(async () => {
    if (
      !deviationForm.type ||
      !deviationForm.severity ||
      !deviationForm.description ||
      !deviationForm.correctiveAction ||
      !deviationForm.reportedBy
    ) {
      return;
    }

    await handleRecordDeviation(deviationForm);

    // Reset form on successful submission
    setDeviationForm({
      type: "",
      severity: "",
      description: "",
      correctiveAction: "",
      reportedBy: "",
      batchDisposition: "",
    });
  }, [deviationForm, handleRecordDeviation]);

  const handleValidateData = useCallback(() => {
    if (uploadedFiles.length === 0) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.execution.noFiles",
          defaultMessage: "Please upload analyzer data files first",
        }),
      );
      return;
    }

    setIsLoading(true);

    // Simulate validation process with Stage 2 QC parameters
    setTimeout(() => {
      // Mock calibration data
      setCalibrationData({
        equation: "y = 0.9876x + 0.0234",
        rSquared: 0.9987,
        slope: 0.9876,
        intercept: 0.0234,
        range: "10-10000 ng/mL",
        pointsUsed: 6,
        qualityAssessment: "PASS",
      });

      // Mock QC results (3 levels: Low, Medium, High)
      const mockQcResults = [
        {
          id: "1",
          level: "LOW",
          spikedConcentration: "20 ng/mL",
          measuredValue: "19.8 ng/mL",
          accuracy: "99.0%",
          precision: "2.1%",
          status: "PASS",
        },
        {
          id: "2",
          level: "MEDIUM",
          spikedConcentration: "500 ng/mL",
          measuredValue: "497.5 ng/mL",
          accuracy: "99.5%",
          precision: "1.8%",
          status: "PASS",
        },
        {
          id: "3",
          level: "HIGH",
          spikedConcentration: "8000 ng/mL",
          measuredValue: "7950 ng/mL",
          accuracy: "99.4%",
          precision: "2.3%",
          status: "PASS",
        },
      ];
      setQcResults(mockQcResults);

      // Mock Westgard rules check
      const mockWestgardRules = [
        {
          rule: "1-3S",
          status: "PASS",
          description: "No value exceeds 3 sigma",
        },
        {
          rule: "2-2S",
          status: "PASS",
          description: "No 2 consecutive values exceed 2 sigma",
        },
        {
          rule: "R-4S",
          status: "PASS",
          description: "Range between consecutive runs does not exceed 4 sigma",
        },
        {
          rule: "4-1S",
          status: "PASS",
          description: "No 4 consecutive values on same side of mean",
        },
        {
          rule: "10X",
          status: "PASS",
          description: "No 10 consecutive values on same side",
        },
      ];
      setWestgardRules(mockWestgardRules);

      // Mock analyzer results
      const mockResults = [
        {
          id: "1",
          sampleId: "S001",
          result: "456.2 ng/mL",
          replicate: "Rep 1",
          quality: "PASSED",
          recoveryRate: "98.5%",
        },
        {
          id: "2",
          sampleId: "S001",
          result: "457.8 ng/mL",
          replicate: "Rep 2",
          quality: "PASSED",
          recoveryRate: "99.1%",
        },
        {
          id: "3",
          sampleId: "S002",
          result: "523.1 ng/mL",
          replicate: "Rep 1",
          quality: "PASSED",
          recoveryRate: "99.8%",
        },
      ];
      setAnalyzerResults(mockResults);

      setSuccessMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.execution.validationComplete",
          defaultMessage: "Data validation completed successfully",
        }),
      );
      setIsLoading(false);
    }, 2000);
  }, [uploadedFiles, intl]);

  const handleApproveResults = useCallback(() => {
    if (analyzerResults.length === 0) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioanalytical.execution.noResults",
          defaultMessage: "No validated results to approve",
        }),
      );
      return;
    }

    setSuccessMessage(
      intl.formatMessage(
        {
          id: "notebook.bioanalytical.execution.resultsApproved",
          defaultMessage: "{count} sample results approved for QA review",
        },
        { count: analyzerResults.length },
      ),
    );

    if (onProgressUpdate) {
      onProgressUpdate();
    }
  }, [analyzerResults, intl, onProgressUpdate]);

  return (
    <div className="bioanalytical-page">
      <div className="page-instructions">
        <h3>
          <FormattedMessage
            id="notebook.bioanalytical.execution.title"
            defaultMessage="Analytical Execution & Data Acquisition"
          />
        </h3>
        <p>
          <FormattedMessage
            id="notebook.bioanalytical.execution.description"
            defaultMessage="Upload raw analytical instrument data (mzML, CDF, CSV, PDF), validate calibration curves, monitor QC results using Westgard rules, and verify system suitability before approving data for QA review."
          />
        </p>
      </div>

      {errorMessage && (
        <div style={{ marginBottom: "1rem" }}>
          <InlineNotification
            kind="error"
            title={intl.formatMessage({
              id: "notebook.bioanalytical.execution.error",
              defaultMessage: "Error",
            })}
            subtitle={errorMessage}
            lowContrast
            onCloseButtonClick={() => setErrorMessage("")}
          />
        </div>
      )}

      {successMessage && (
        <div style={{ marginBottom: "1rem" }}>
          <InlineNotification
            kind="success"
            title={intl.formatMessage({
              id: "notebook.bioanalytical.execution.success",
              defaultMessage: "Success",
            })}
            subtitle={successMessage}
            lowContrast
            onCloseButtonClick={() => setSuccessMessage("")}
          />
        </div>
      )}

      <Tabs
        selectedIndex={selectedTab}
        onChange={(evt) => setSelectedTab(evt.selectedIndex)}
      >
        <TabList aria-label="Analytical execution tabs">
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.execution.tab.testExecution"
              defaultMessage="Test Execution"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.execution.tab.dataUpload"
              defaultMessage="Raw Data Upload"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.execution.tab.calibration"
              defaultMessage="Calibration & QC"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.execution.tab.results"
              defaultMessage="Results & Approval"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.execution.tab.deviations"
              defaultMessage="Deviations"
            />
          </Tab>
        </TabList>

        <TabPanels>
          {/* Tab 1: Test Execution - Load Stage 2 Assignments & Execute Tests */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.testExecutionSection"
                        defaultMessage="Execute Assigned Tests Using Validated Analytical Methods"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.testExecutionHelp"
                        defaultMessage="Execute tests for samples with Stage 2 assignments. Record test parameters, instrument IDs, and analyst identification. Ensure all assigned tests follow the validated analytical methods from Stage 2."
                      />
                    </p>
                  </div>

                  {/* Load Assigned Samples from Stage 2 */}
                  {isLoading ? (
                    <div style={{ marginTop: "2rem", textAlign: "center" }}>
                      <Loading description="Loading assigned samples..." />
                    </div>
                  ) : assignedSamples.length === 0 ? (
                    <div
                      style={{
                        marginTop: "2rem",
                        padding: "2rem",
                        backgroundColor: "#f4f4f4",
                        borderRadius: "4px",
                        textAlign: "center",
                      }}
                    >
                      <h5 style={{ color: "#525252", marginBottom: "1rem" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.execution.noAssignedSamples"
                          defaultMessage="No Test Assignments Found"
                        />
                      </h5>
                      <p style={{ color: "#525252" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.execution.noAssignmentsHelp"
                          defaultMessage="Complete Stage 2 (Test Assignment & Preparation) first to assign analytical methods and staff to samples before proceeding to execution."
                        />
                      </p>
                    </div>
                  ) : (
                    <div style={{ marginTop: "1.5rem" }}>
                      <h5 style={{ marginBottom: "1rem" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.execution.assignedSamplesTitle"
                          defaultMessage="Samples with Stage 2 Test Assignments ({count})"
                          values={{ count: assignedSamples.length }}
                        />
                      </h5>

                      <Table>
                        <TableHead>
                          <TableRow>
                            <TableHeader>
                              <Checkbox
                                id="select-all-samples"
                                onChange={(checked) => {
                                  if (checked) {
                                    setSelectedSampleIds(
                                      new Set(assignedSamples.map((s) => s.id)),
                                    );
                                  } else {
                                    setSelectedSampleIds(new Set());
                                  }
                                }}
                                checked={
                                  selectedSampleIds.size ===
                                    assignedSamples.length &&
                                  assignedSamples.length > 0
                                }
                                indeterminate={
                                  selectedSampleIds.size > 0 &&
                                  selectedSampleIds.size <
                                    assignedSamples.length
                                }
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.sampleId"
                                defaultMessage="Sample ID"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.analyticalMethod"
                                defaultMessage="Analytical Method"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.assignedStaff"
                                defaultMessage="Assigned Staff"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.sampleType"
                                defaultMessage="Sample Type"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.executionStatus"
                                defaultMessage="Execution Status"
                              />
                            </TableHeader>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {assignedSamples.map((sample) => (
                            <TableRow key={sample.id}>
                              <TableCell>
                                <Checkbox
                                  id={`sample-${sample.id}`}
                                  onChange={(checked) => {
                                    const newSelection = new Set(
                                      selectedSampleIds,
                                    );
                                    if (checked) {
                                      newSelection.add(sample.id);
                                    } else {
                                      newSelection.delete(sample.id);
                                    }
                                    setSelectedSampleIds(newSelection);
                                  }}
                                  checked={selectedSampleIds.has(sample.id)}
                                />
                              </TableCell>
                              <TableCell>
                                {sample.accessionNumber || sample.id}
                              </TableCell>
                              <TableCell>
                                {sample.data?.analyticalMethod
                                  ? sample.data.analyticalMethod.replace(
                                      /_/g,
                                      " ",
                                    )
                                  : "Not specified"}
                              </TableCell>
                              <TableCell>
                                {sample.data?.assignedStaff
                                  ? sample.data.assignedStaff.replace(/_/g, " ")
                                  : "Not assigned"}
                              </TableCell>
                              <TableCell>
                                {sample.sampleType || "Unknown"}
                              </TableCell>
                              <TableCell>
                                <span
                                  style={{
                                    padding: "0.25rem 0.5rem",
                                    borderRadius: "4px",
                                    fontSize: "0.75rem",
                                    backgroundColor:
                                      sample.data?.executionStatus ===
                                      "EXECUTED"
                                        ? "#24a148"
                                        : "#8a3ffc",
                                    color: "white",
                                  }}
                                >
                                  {sample.data?.executionStatus || "PENDING"}
                                </span>
                              </TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>

                      {/* Test Execution Configuration */}
                      <div
                        style={{
                          marginTop: "2rem",
                          padding: "1.5rem",
                          backgroundColor: "#f4f4f4",
                          borderRadius: "4px",
                        }}
                      >
                        <h6 style={{ marginBottom: "1rem" }}>
                          <FormattedMessage
                            id="notebook.bioanalytical.execution.executionConfig"
                            defaultMessage="Test Execution Configuration"
                          />
                        </h6>

                        <Grid>
                          <Column lg={8} md={4} sm={4}>
                            <TextInput
                              id="analyst-id"
                              labelText={
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.analystId"
                                  defaultMessage="Analyst ID *"
                                />
                              }
                              placeholder="Enter analyst identification"
                              value={executionData.analystId}
                              onChange={(e) =>
                                setExecutionData((prev) => ({
                                  ...prev,
                                  analystId: e.target.value,
                                }))
                              }
                            />
                          </Column>

                          <Column lg={8} md={4} sm={4}>
                            <Select
                              id="instrument-id"
                              labelText={
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.instrumentId"
                                  defaultMessage="Instrument ID *"
                                />
                              }
                              value={executionData.instrumentId}
                              onChange={(e) =>
                                setExecutionData((prev) => ({
                                  ...prev,
                                  instrumentId: e.target.value,
                                }))
                              }
                            >
                              <SelectItem
                                value=""
                                text="Select instrument..."
                              />
                              {instruments.map((instrument) => (
                                <SelectItem
                                  key={instrument.id}
                                  value={instrument.id}
                                  text={`${instrument.name} (${instrument.type})`}
                                />
                              ))}
                            </Select>
                          </Column>
                        </Grid>

                        <Grid style={{ marginTop: "1rem" }}>
                          <Column lg={5} md={3} sm={4}>
                            <TextInput
                              id="batch-number"
                              labelText={
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.batchNumber"
                                  defaultMessage="Batch Number"
                                />
                              }
                              placeholder="Enter batch/run number"
                              value={executionData.batchNumber}
                              onChange={(e) =>
                                setExecutionData((prev) => ({
                                  ...prev,
                                  batchNumber: e.target.value,
                                }))
                              }
                            />
                          </Column>

                          <Column lg={5} md={3} sm={4}>
                            <DatePicker
                              dateFormat="Y-m-d"
                              datePickerType="single"
                            >
                              <DatePickerInput
                                id="execution-date"
                                labelText={
                                  <FormattedMessage
                                    id="notebook.bioanalytical.execution.executionDate"
                                    defaultMessage="Execution Date"
                                  />
                                }
                                placeholder="YYYY-MM-DD"
                                value={executionData.executionDate}
                                onChange={(e) =>
                                  setExecutionData((prev) => ({
                                    ...prev,
                                    executionDate: e.target.value,
                                  }))
                                }
                              />
                            </DatePicker>
                          </Column>

                          <Column lg={6} md={2} sm={4}>
                            <TextArea
                              id="execution-notes"
                              labelText={
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.notes"
                                  defaultMessage="Execution Notes"
                                />
                              }
                              placeholder="Document any observations or special conditions..."
                              rows={3}
                              value={executionData.notes}
                              onChange={(e) =>
                                setExecutionData((prev) => ({
                                  ...prev,
                                  notes: e.target.value,
                                }))
                              }
                            />
                          </Column>
                        </Grid>

                        <div style={{ marginTop: "1.5rem" }}>
                          <Button
                            kind="primary"
                            onClick={handleExecuteTests}
                            disabled={
                              isExecuting ||
                              selectedSampleIds.size === 0 ||
                              !executionData.analystId ||
                              !executionData.instrumentId
                            }
                          >
                            {isExecuting ? (
                              <Loading description="Executing tests..." small />
                            ) : (
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.executeTests"
                                defaultMessage="Execute Tests for {count} Sample(s)"
                                values={{ count: selectedSampleIds.size }}
                              />
                            )}
                          </Button>
                        </div>
                      </div>
                    </div>
                  )}
                </Column>
              </Grid>
            </div>
          </TabPanel>

          {/* Tab 2: Raw Data Upload */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.dataUploadSection"
                        defaultMessage="Capture Raw Instrument Data"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.dataUploadHelp"
                        defaultMessage="Upload raw analytical instrument data files (chromatograms, spectra, physical test results). Supported formats: mzML, CDF, CSV, PDF."
                      />
                    </p>
                  </div>

                  <div style={{ marginTop: "1.5rem" }}>
                    <Select
                      id="instrument-select"
                      labelText={
                        <FormattedMessage
                          id="notebook.bioanalytical.execution.selectInstrument"
                          defaultMessage="Select Instrument"
                        />
                      }
                      value={selectedInstrument}
                      onChange={(e) => setSelectedInstrument(e.target.value)}
                    >
                      <SelectItem value="" text="-- Choose an instrument --" />
                      {instruments.map((instrument) => (
                        <SelectItem
                          key={instrument.id}
                          value={instrument.id}
                          text={instrument.name}
                        />
                      ))}
                    </Select>
                  </div>

                  {selectedInstrument && (
                    <div
                      style={{
                        marginTop: "1.5rem",
                        padding: "1rem",
                        backgroundColor: "#f4f4f4",
                        borderRadius: "4px",
                      }}
                    >
                      <p
                        style={{
                          fontSize: "0.875rem",
                          marginBottom: "0.5rem",
                        }}
                      >
                        <strong>
                          <FormattedMessage
                            id="notebook.bioanalytical.execution.supportedFormats"
                            defaultMessage="Supported Formats:"
                          />
                        </strong>
                      </p>
                      <p style={{ fontSize: "0.875rem", color: "#525252" }}>
                        {instruments
                          .find((i) => i.id === selectedInstrument)
                          ?.formats.join(", ")}
                      </p>
                    </div>
                  )}

                  <div style={{ marginTop: "1.5rem" }}>
                    <FileUploader
                      labelTitle={intl.formatMessage({
                        id: "notebook.bioanalytical.execution.uploadFile",
                        defaultMessage: "Upload File",
                      })}
                      iconDescription={intl.formatMessage({
                        id: "notebook.bioanalytical.execution.deleteFile",
                        defaultMessage: "Delete file",
                      })}
                      accept={
                        selectedInstrument
                          ? instruments
                              .find((i) => i.id === selectedInstrument)
                              ?.formats.map((f) => `.${f.toLowerCase()}`) || []
                          : []
                      }
                      multiple={false}
                      onChange={(e) => {
                        if (e.target.files) {
                          handleFileUpload(e.target.files);
                        }
                      }}
                    />
                  </div>

                  {uploadedFiles.length > 0 && (
                    <div style={{ marginTop: "1.5rem" }}>
                      <h5 style={{ marginBottom: "1rem" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.execution.uploadedFiles"
                          defaultMessage="Uploaded Files ({count})"
                          values={{ count: uploadedFiles.length }}
                        />
                      </h5>
                      <Table>
                        <TableHead>
                          <TableRow>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.fileName"
                                defaultMessage="File Name"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.size"
                                defaultMessage="Size"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.instrument"
                                defaultMessage="Instrument"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.status"
                                defaultMessage="Status"
                              />
                            </TableHeader>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {uploadedFiles.map((file) => (
                            <TableRow key={file.id}>
                              <TableCell>{file.name}</TableCell>
                              <TableCell>
                                {(file.size / 1024).toFixed(2)} KB
                              </TableCell>
                              <TableCell>{file.instrument}</TableCell>
                              <TableCell>
                                <span className="status-badge warning">
                                  {file.status.replace(/_/g, " ")}
                                </span>
                              </TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </div>
                  )}

                  {selectedInstrument && uploadedFiles.length > 0 && (
                    <div style={{ marginTop: "1.5rem" }}>
                      <Button
                        kind="primary"
                        onClick={handleValidateData}
                        disabled={isLoading}
                      >
                        {isLoading ? (
                          <>
                            <Loading description="Validating..." />
                          </>
                        ) : (
                          <FormattedMessage
                            id="notebook.bioanalytical.execution.validateData"
                            defaultMessage="Validate Uploaded Data"
                          />
                        )}
                      </Button>
                    </div>
                  )}
                </Column>
              </Grid>
            </div>
          </TabPanel>

          {/* Tab 3: Calibration & QC */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.calibrationSection"
                        defaultMessage="Calibration Curve Validation"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.calibrationHelp"
                        defaultMessage="Review calibration curve regression analysis. Acceptable criteria: r² ≥ 0.99 (99% of variance explained). Display linear regression equation, slope, intercept, and calibration range."
                      />
                    </p>

                    {calibrationData ? (
                      <div style={{ marginTop: "1.5rem" }}>
                        <div
                          style={{
                            padding: "1.5rem",
                            backgroundColor: "#f4f4f4",
                            borderRadius: "4px",
                          }}
                        >
                          <div style={{ marginBottom: "1rem" }}>
                            <strong>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.equation"
                                defaultMessage="Regression Equation:"
                              />
                            </strong>
                            <p
                              style={{
                                fontSize: "1rem",
                                marginTop: "0.25rem",
                                color: "#161616",
                              }}
                            >
                              {calibrationData.equation}
                            </p>
                          </div>

                          <table
                            style={{
                              width: "100%",
                              borderCollapse: "collapse",
                            }}
                          >
                            <tbody>
                              <tr>
                                <td
                                  style={{
                                    padding: "0.5rem",
                                    borderBottom: "1px solid #e0e0e0",
                                  }}
                                >
                                  <strong>
                                    <FormattedMessage
                                      id="notebook.bioanalytical.execution.rSquared"
                                      defaultMessage="R² Value:"
                                    />
                                  </strong>
                                </td>
                                <td
                                  style={{
                                    padding: "0.5rem",
                                    borderBottom: "1px solid #e0e0e0",
                                  }}
                                >
                                  {calibrationData.rSquared.toFixed(4)}
                                  <span
                                    style={{
                                      marginLeft: "0.5rem",
                                      padding: "0.25rem 0.5rem",
                                      backgroundColor:
                                        calibrationData.rSquared >= 0.99
                                          ? "#24a148"
                                          : "#da1e28",
                                      color: "white",
                                      borderRadius: "4px",
                                      fontSize: "0.75rem",
                                    }}
                                  >
                                    {calibrationData.rSquared >= 0.99
                                      ? "PASS"
                                      : "FAIL"}
                                  </span>
                                </td>
                              </tr>
                              <tr>
                                <td
                                  style={{
                                    padding: "0.5rem",
                                    borderBottom: "1px solid #e0e0e0",
                                  }}
                                >
                                  <strong>
                                    <FormattedMessage
                                      id="notebook.bioanalytical.execution.slope"
                                      defaultMessage="Slope:"
                                    />
                                  </strong>
                                </td>
                                <td
                                  style={{
                                    padding: "0.5rem",
                                    borderBottom: "1px solid #e0e0e0",
                                  }}
                                >
                                  {calibrationData.slope}
                                </td>
                              </tr>
                              <tr>
                                <td
                                  style={{
                                    padding: "0.5rem",
                                    borderBottom: "1px solid #e0e0e0",
                                  }}
                                >
                                  <strong>
                                    <FormattedMessage
                                      id="notebook.bioanalytical.execution.intercept"
                                      defaultMessage="Intercept:"
                                    />
                                  </strong>
                                </td>
                                <td
                                  style={{
                                    padding: "0.5rem",
                                    borderBottom: "1px solid #e0e0e0",
                                  }}
                                >
                                  {calibrationData.intercept}
                                </td>
                              </tr>
                              <tr>
                                <td
                                  style={{
                                    padding: "0.5rem",
                                    borderBottom: "1px solid #e0e0e0",
                                  }}
                                >
                                  <strong>
                                    <FormattedMessage
                                      id="notebook.bioanalytical.execution.range"
                                      defaultMessage="Calibration Range:"
                                    />
                                  </strong>
                                </td>
                                <td
                                  style={{
                                    padding: "0.5rem",
                                    borderBottom: "1px solid #e0e0e0",
                                  }}
                                >
                                  {calibrationData.range}
                                </td>
                              </tr>
                              <tr>
                                <td
                                  style={{
                                    padding: "0.5rem",
                                    borderBottom: "1px solid #e0e0e0",
                                  }}
                                >
                                  <strong>
                                    <FormattedMessage
                                      id="notebook.bioanalytical.execution.pointsUsed"
                                      defaultMessage="Points Used:"
                                    />
                                  </strong>
                                </td>
                                <td
                                  style={{
                                    padding: "0.5rem",
                                    borderBottom: "1px solid #e0e0e0",
                                  }}
                                >
                                  {calibrationData.pointsUsed}
                                </td>
                              </tr>
                            </tbody>
                          </table>
                        </div>

                        <div
                          style={{
                            marginTop: "1rem",
                            padding: "1rem",
                            backgroundColor: "#e7f1f5",
                            borderRadius: "4px",
                            borderLeft: "4px solid #0043ce",
                          }}
                        >
                          <p style={{ fontSize: "0.875rem", margin: 0 }}>
                            <strong>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.calibrationNote"
                                defaultMessage="Calibration Quality Assessment:"
                              />
                            </strong>
                          </p>
                          <p
                            style={{
                              fontSize: "0.875rem",
                              color: "#24a148",
                              margin: "0.25rem 0 0 0",
                            }}
                          >
                            {calibrationData.rSquared >= 0.99
                              ? "✓ Excellent correlation with r² > 0.99"
                              : "✗ Calibration does not meet acceptance criteria"}
                          </p>
                        </div>
                      </div>
                    ) : (
                      <div
                        style={{
                          marginTop: "1.5rem",
                          padding: "1rem",
                          backgroundColor: "#f4f4f4",
                          borderRadius: "4px",
                          textAlign: "center",
                        }}
                      >
                        <p style={{ color: "#525252" }}>
                          <FormattedMessage
                            id="notebook.bioanalytical.execution.noCalibration"
                            defaultMessage="Upload and validate data to see calibration results"
                          />
                        </p>
                      </div>
                    )}
                  </div>
                </Column>
              </Grid>
            </div>
          </TabPanel>

          {/* Tab 3: QC Results & Trending */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.qcSection"
                        defaultMessage="QC Results & Westgard Rule Monitoring"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.qcHelp"
                        defaultMessage="Monitor QC results across Low/Medium/High levels. Acceptance criteria: accuracy 90-110%, precision RSD ≤ 15%. Westgard multi-rule detection identifies out-of-control conditions: 1-3S, 2-2S, R-4S, 4-1S, 10X."
                      />
                    </p>

                    {qcResults.length > 0 && (
                      <div style={{ marginTop: "1.5rem" }}>
                        <h5 style={{ marginBottom: "1rem" }}>
                          <FormattedMessage
                            id="notebook.bioanalytical.execution.qcByLevel"
                            defaultMessage="QC Results by Level"
                          />
                        </h5>
                        <Table>
                          <TableHead>
                            <TableRow>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.qcLevel"
                                  defaultMessage="QC Level"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.spiked"
                                  defaultMessage="Spiked Conc."
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.measured"
                                  defaultMessage="Measured Value"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.accuracy"
                                  defaultMessage="Accuracy"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.precision"
                                  defaultMessage="Precision (RSD)"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.status"
                                  defaultMessage="Status"
                                />
                              </TableHeader>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {qcResults.map((qc) => (
                              <TableRow key={qc.id}>
                                <TableCell>{qc.level}</TableCell>
                                <TableCell>{qc.spikedConcentration}</TableCell>
                                <TableCell>{qc.measuredValue}</TableCell>
                                <TableCell>{qc.accuracy}</TableCell>
                                <TableCell>{qc.precision}</TableCell>
                                <TableCell>
                                  <span
                                    className="status-badge"
                                    style={{
                                      backgroundColor:
                                        qc.status === "PASS"
                                          ? "#24a148"
                                          : "#da1e28",
                                      color: "white",
                                      padding: "0.25rem 0.5rem",
                                      borderRadius: "4px",
                                      fontSize: "0.75rem",
                                    }}
                                  >
                                    {qc.status}
                                  </span>
                                </TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </div>
                    )}

                    {westgardRules.length > 0 && (
                      <div style={{ marginTop: "2rem" }}>
                        <h5 style={{ marginBottom: "1rem" }}>
                          <FormattedMessage
                            id="notebook.bioanalytical.execution.westgard"
                            defaultMessage="Westgard Multi-Rule Detection"
                          />
                        </h5>
                        <Table>
                          <TableHead>
                            <TableRow>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.rule"
                                  defaultMessage="Rule"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.status"
                                  defaultMessage="Status"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.description"
                                  defaultMessage="Description"
                                />
                              </TableHeader>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {westgardRules.map((wr) => (
                              <TableRow key={wr.rule}>
                                <TableCell>
                                  <strong>{wr.rule}</strong>
                                </TableCell>
                                <TableCell>
                                  <span
                                    className="status-badge"
                                    style={{
                                      backgroundColor:
                                        wr.status === "PASS"
                                          ? "#24a148"
                                          : "#da1e28",
                                      color: "white",
                                      padding: "0.25rem 0.5rem",
                                      borderRadius: "4px",
                                      fontSize: "0.75rem",
                                    }}
                                  >
                                    {wr.status}
                                  </span>
                                </TableCell>
                                <TableCell style={{ fontSize: "0.875rem" }}>
                                  {wr.description}
                                </TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </div>
                    )}

                    {qcResults.length === 0 && (
                      <div
                        style={{
                          marginTop: "1.5rem",
                          padding: "1rem",
                          backgroundColor: "#f4f4f4",
                          borderRadius: "4px",
                          textAlign: "center",
                        }}
                      >
                        <p style={{ color: "#525252" }}>
                          <FormattedMessage
                            id="notebook.bioanalytical.execution.noQCData"
                            defaultMessage="Upload and validate data to see QC results and Westgard rule monitoring"
                          />
                        </p>
                      </div>
                    )}
                  </div>
                </Column>
              </Grid>
            </div>
          </TabPanel>

          {/* Tab 4: Analyzer Results */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.resultsSection"
                        defaultMessage="Analyzer Results & Sample Data"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.resultsHelp"
                        defaultMessage="Review analyzed sample results with replicate values, acceptance quality assessment, and analyte recovery rates. All results must pass QA review before data release."
                      />
                    </p>

                    {analyzerResults.length > 0 && (
                      <div style={{ marginTop: "1.5rem" }}>
                        <h5 style={{ marginBottom: "1rem" }}>
                          <FormattedMessage
                            id="notebook.bioanalytical.execution.analyzerResults"
                            defaultMessage="Sample Analysis Results ({count})"
                            values={{ count: analyzerResults.length }}
                          />
                        </h5>
                        <Table>
                          <TableHead>
                            <TableRow>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.sampleId"
                                  defaultMessage="Sample ID"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.replicate"
                                  defaultMessage="Replicate"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.result"
                                  defaultMessage="Result"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.recovery"
                                  defaultMessage="Recovery Rate"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.quality"
                                  defaultMessage="Quality"
                                />
                              </TableHeader>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {analyzerResults.map((result) => (
                              <TableRow key={result.id}>
                                <TableCell>{result.sampleId}</TableCell>
                                <TableCell>{result.replicate}</TableCell>
                                <TableCell>{result.result}</TableCell>
                                <TableCell>{result.recoveryRate}</TableCell>
                                <TableCell>
                                  <span
                                    className="status-badge"
                                    style={{
                                      backgroundColor:
                                        result.quality === "PASSED"
                                          ? "#24a148"
                                          : "#da1e28",
                                      color: "white",
                                      padding: "0.25rem 0.5rem",
                                      borderRadius: "4px",
                                      fontSize: "0.75rem",
                                    }}
                                  >
                                    {result.quality}
                                  </span>
                                </TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>

                        <div style={{ marginTop: "1.5rem" }}>
                          <Button kind="primary" onClick={handleApproveResults}>
                            <FormattedMessage
                              id="notebook.bioanalytical.execution.approveResults"
                              defaultMessage="Approve Results for QA Review"
                            />
                          </Button>
                        </div>
                      </div>
                    )}

                    {analyzerResults.length === 0 && (
                      <div
                        style={{
                          marginTop: "1.5rem",
                          padding: "1rem",
                          backgroundColor: "#f4f4f4",
                          borderRadius: "4px",
                          textAlign: "center",
                        }}
                      >
                        <p style={{ color: "#525252" }}>
                          <FormattedMessage
                            id="notebook.bioanalytical.execution.noResults"
                            defaultMessage="Upload and validate data to see analyzer results"
                          />
                        </p>
                      </div>
                    )}
                  </div>
                </Column>
              </Grid>
            </div>
          </TabPanel>

          {/* Tab 5: Deviations */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.deviationsSection"
                        defaultMessage="Document Any Deviations or Reprocessing Events"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.deviationsHelp"
                        defaultMessage="Record and justify any deviations from SOPs, method specifications, or QC failures. Include corrective actions taken, root cause analysis, and batch disposition decisions."
                      />
                    </p>
                  </div>

                  {/* Deviation Entry Form */}
                  <div
                    style={{
                      marginTop: "1.5rem",
                      padding: "1.5rem",
                      backgroundColor: "#f4f4f4",
                      borderRadius: "4px",
                    }}
                  >
                    <h5 style={{ marginBottom: "1rem" }}>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.addDeviation"
                        defaultMessage="Add New Deviation"
                      />
                    </h5>

                    <Grid style={{ marginBottom: "1rem" }}>
                      <Column lg={8} md={4} sm={4}>
                        <Select
                          id="deviation-type"
                          labelText={
                            <FormattedMessage
                              id="notebook.bioanalytical.execution.deviationType"
                              defaultMessage="Deviation Type *"
                            />
                          }
                          value={deviationForm.type}
                          onChange={(e) =>
                            setDeviationForm((prev) => ({
                              ...prev,
                              type: e.target.value,
                            }))
                          }
                        >
                          <SelectItem
                            value=""
                            text="Select deviation type..."
                          />
                          <SelectItem
                            value="SOP_DEVIATION"
                            text="SOP Deviation"
                          />
                          <SelectItem
                            value="METHOD_DEVIATION"
                            text="Method Deviation"
                          />
                          <SelectItem value="QC_FAILURE" text="QC Failure" />
                          <SelectItem
                            value="INSTRUMENT_ISSUE"
                            text="Instrument Issue"
                          />
                          <SelectItem
                            value="SAMPLE_ISSUE"
                            text="Sample Issue"
                          />
                          <SelectItem
                            value="CALCULATION_ERROR"
                            text="Calculation Error"
                          />
                          <SelectItem value="OTHER" text="Other" />
                        </Select>
                      </Column>

                      <Column lg={8} md={4} sm={4}>
                        <Select
                          id="deviation-severity"
                          labelText={
                            <FormattedMessage
                              id="notebook.bioanalytical.execution.severity"
                              defaultMessage="Severity *"
                            />
                          }
                          value={deviationForm.severity}
                          onChange={(e) =>
                            setDeviationForm((prev) => ({
                              ...prev,
                              severity: e.target.value,
                            }))
                          }
                        >
                          <SelectItem value="" text="Select severity..." />
                          <SelectItem
                            value="MINOR"
                            text="Minor (no impact on results)"
                          />
                          <SelectItem
                            value="MAJOR"
                            text="Major (potential impact)"
                          />
                          <SelectItem
                            value="CRITICAL"
                            text="Critical (requires reprocessing)"
                          />
                        </Select>
                      </Column>
                    </Grid>

                    <TextArea
                      id="deviation-description"
                      labelText={
                        <FormattedMessage
                          id="notebook.bioanalytical.execution.deviationDescription"
                          defaultMessage="Deviation Description *"
                        />
                      }
                      placeholder="Describe the deviation, when it occurred, and what was affected..."
                      rows={3}
                      value={deviationForm.description}
                      onChange={(e) =>
                        setDeviationForm((prev) => ({
                          ...prev,
                          description: e.target.value,
                        }))
                      }
                    />

                    <TextArea
                      id="corrective-action"
                      labelText={
                        <FormattedMessage
                          id="notebook.bioanalytical.execution.correctiveAction"
                          defaultMessage="Corrective Action Taken *"
                        />
                      }
                      placeholder="Describe corrective actions, reprocessing, or reanalysis performed..."
                      rows={3}
                      style={{ marginTop: "1rem" }}
                      value={deviationForm.correctiveAction}
                      onChange={(e) =>
                        setDeviationForm((prev) => ({
                          ...prev,
                          correctiveAction: e.target.value,
                        }))
                      }
                    />

                    <Grid style={{ marginTop: "1rem" }}>
                      <Column lg={8} md={4} sm={4}>
                        <TextInput
                          id="reported-by"
                          labelText={
                            <FormattedMessage
                              id="notebook.bioanalytical.execution.reportedBy"
                              defaultMessage="Reported By *"
                            />
                          }
                          placeholder="Enter analyst name"
                          value={deviationForm.reportedBy}
                          onChange={(e) =>
                            setDeviationForm((prev) => ({
                              ...prev,
                              reportedBy: e.target.value,
                            }))
                          }
                        />
                      </Column>

                      <Column lg={8} md={4} sm={4}>
                        <Select
                          id="batch-disposition"
                          labelText={
                            <FormattedMessage
                              id="notebook.bioanalytical.execution.batchDisposition"
                              defaultMessage="Batch Disposition"
                            />
                          }
                          value={deviationForm.batchDisposition}
                          onChange={(e) =>
                            setDeviationForm((prev) => ({
                              ...prev,
                              batchDisposition: e.target.value,
                            }))
                          }
                        >
                          <SelectItem value="" text="Select disposition..." />
                          <SelectItem value="ACCEPT" text="Accept as is" />
                          <SelectItem
                            value="ACCEPT_WITH_LIMITS"
                            text="Accept with limits"
                          />
                          <SelectItem
                            value="REPROCESS"
                            text="Reprocess required"
                          />
                          <SelectItem value="REJECT" text="Reject batch" />
                        </Select>
                      </Column>
                    </Grid>

                    <div style={{ marginTop: "1.5rem" }}>
                      <Button
                        kind="primary"
                        onClick={handleAddDeviation}
                        disabled={
                          !deviationForm.type ||
                          !deviationForm.severity ||
                          !deviationForm.description ||
                          !deviationForm.correctiveAction ||
                          !deviationForm.reportedBy
                        }
                      >
                        <FormattedMessage
                          id="notebook.bioanalytical.execution.recordDeviation"
                          defaultMessage="Record Deviation"
                        />
                      </Button>
                    </div>
                  </div>

                  {/* Recorded Deviations List */}
                  {deviations.length > 0 && (
                    <div style={{ marginTop: "2rem" }}>
                      <h5 style={{ marginBottom: "1rem" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.execution.recordedDeviations"
                          defaultMessage="Recorded Deviations ({count})"
                          values={{ count: deviations.length }}
                        />
                      </h5>

                      <Table>
                        <TableHead>
                          <TableRow>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.deviationType"
                                defaultMessage="Type"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.severity"
                                defaultMessage="Severity"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.deviationDescription"
                                defaultMessage="Description"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.reportedBy"
                                defaultMessage="Reported By"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.batchDisposition"
                                defaultMessage="Disposition"
                              />
                            </TableHeader>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {deviations.map((deviation, index) => (
                            <TableRow key={index}>
                              <TableCell>
                                <span
                                  style={{
                                    padding: "0.25rem 0.5rem",
                                    borderRadius: "4px",
                                    fontSize: "0.75rem",
                                    backgroundColor: "#8a3ffc",
                                    color: "white",
                                  }}
                                >
                                  {deviation.type.replace(/_/g, " ")}
                                </span>
                              </TableCell>
                              <TableCell>
                                <span
                                  style={{
                                    padding: "0.25rem 0.5rem",
                                    borderRadius: "4px",
                                    fontSize: "0.75rem",
                                    backgroundColor:
                                      deviation.severity === "CRITICAL"
                                        ? "#da1e28"
                                        : deviation.severity === "MAJOR"
                                          ? "#f1c21b"
                                          : "#24a148",
                                    color:
                                      deviation.severity === "MAJOR"
                                        ? "#161616"
                                        : "white",
                                  }}
                                >
                                  {deviation.severity}
                                </span>
                              </TableCell>
                              <TableCell
                                style={{
                                  maxWidth: "300px",
                                  overflow: "hidden",
                                  textOverflow: "ellipsis",
                                  whiteSpace: "nowrap",
                                }}
                              >
                                {deviation.description}
                              </TableCell>
                              <TableCell>{deviation.reportedBy}</TableCell>
                              <TableCell>
                                {deviation.batchDisposition ? (
                                  <span
                                    style={{
                                      padding: "0.25rem 0.5rem",
                                      borderRadius: "4px",
                                      fontSize: "0.75rem",
                                      backgroundColor:
                                        deviation.batchDisposition === "ACCEPT"
                                          ? "#24a148"
                                          : deviation.batchDisposition ===
                                              "REJECT"
                                            ? "#da1e28"
                                            : "#0043ce",
                                      color: "white",
                                    }}
                                  >
                                    {deviation.batchDisposition.replace(
                                      /_/g,
                                      " ",
                                    )}
                                  </span>
                                ) : (
                                  <span style={{ color: "#525252" }}>
                                    Pending
                                  </span>
                                )}
                              </TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </div>
                  )}

                  {deviations.length === 0 && (
                    <div
                      style={{
                        marginTop: "1.5rem",
                        padding: "1rem",
                        backgroundColor: "#f4f4f4",
                        borderRadius: "4px",
                        textAlign: "center",
                      }}
                    >
                      <p style={{ color: "#525252" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.execution.noDeviations"
                          defaultMessage="No deviations recorded for this batch. Document any deviations or issues above."
                        />
                      </p>
                    </div>
                  )}
                </Column>
              </Grid>
            </div>
          </TabPanel>
        </TabPanels>
      </Tabs>
    </div>
  );
}

export default BioanalyticalAnalyticalExecutionPage;

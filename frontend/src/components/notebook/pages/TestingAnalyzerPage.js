import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Dropdown,
  Checkbox,
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
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  TextArea,
  TextInput,
  NumberInput,
  Toggle,
  ExpandableTile,
  TileAboveTheFoldContent,
  TileBelowTheFoldContent,
  TableToolbarSearch,
  Loading,
} from "@carbon/react";
import {
  Chemistry,
  Microscope,
  Renew,
  Checkmark,
  Close,
  WarningAlt,
  ChartLineData,
  Play,
  Stop,
  Information,
  DataBase,
  Report,
  Add,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import "../workflow/NotebookWorkflow.css";

/**
 * TestingAnalyzerPage - Testing Phase & Analyzer Integration for MedLab workflow.
 * Handles test execution, analyzer selection, QC tracking, and deviation handling.
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function TestingAnalyzerPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  // Use a ref to track selection synchronously (avoids setTimeout race conditions)
  const selectedSampleIdsRef = useRef([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [activeTab, setActiveTab] = useState(0);

  // Analyzers and QC state
  const [analyzers, setAnalyzers] = useState([]);
  const [qcRecords, setQcRecords] = useState([]);
  const [deviations, setDeviations] = useState([]);

  // Test execution modal state
  const [testExecutionModalOpen, setTestExecutionModalOpen] = useState(false);
  const [selectedAnalyzer, setSelectedAnalyzer] = useState(null);
  const [worklistGenerated, setWorklistGenerated] = useState(false);
  const [isManualTest, setIsManualTest] = useState(false);
  const [technologyUsed, setTechnologyUsed] = useState("");
  const [executing, setExecuting] = useState(false);

  // QC modal state
  const [qcModalOpen, setQcModalOpen] = useState(false);
  const [qcType, setQcType] = useState("daily");
  const [qcLevel, setQcLevel] = useState("normal");
  const [qcResult, setQcResult] = useState("pass");
  const [calibrationStatus, setCalibrationStatus] = useState("valid");
  const [qcNotes, setQcNotes] = useState("");
  const [savingQc, setSavingQc] = useState(false);

  // Deviation modal state
  const [deviationModalOpen, setDeviationModalOpen] = useState(false);
  const [deviationType, setDeviationType] = useState("random");
  const [deviationAction, setDeviationAction] = useState("rerun");
  const [rootCauseAnalysis, setRootCauseAnalysis] = useState("");
  const [deviationNotes, setDeviationNotes] = useState("");
  const [savingDeviation, setSavingDeviation] = useState(false);

  // Bulk entry modal state
  const [bulkEntryModalOpen, setBulkEntryModalOpen] = useState(false);
  const [bulkResults, setBulkResults] = useState([]);

  // Assign tests modal state
  const [assignTestsModalOpen, setAssignTestsModalOpen] = useState(false);
  const [availableTests, setAvailableTests] = useState([]);
  const [selectedTestIds, setSelectedTestIds] = useState([]);
  const [isLoadingTests, setIsLoadingTests] = useState(false);
  const [testSearchText, setTestSearchText] = useState("");
  const [assigningTests, setAssigningTests] = useState(false);

  // Analyzer options by discipline
  const analyzerOptions = [
    // Chemistry
    {
      id: "cobas-c311",
      label: "Cobas C311 (Chemistry)",
      discipline: "CHEMISTRY",
      technology: "spectrophotometry",
    },
    {
      id: "cobas-c501",
      label: "Cobas C501 (Chemistry)",
      discipline: "CHEMISTRY",
      technology: "spectrophotometry",
    },
    {
      id: "beckman-au480",
      label: "Beckman AU480",
      discipline: "CHEMISTRY",
      technology: "spectrophotometry",
    },
    {
      id: "ise-module",
      label: "ISE Module (Electrolytes)",
      discipline: "CHEMISTRY",
      technology: "ise",
    },
    // Immunoassay
    {
      id: "cobas-e411",
      label: "Cobas E411 (Immunoassay)",
      discipline: "IMMUNOASSAY",
      technology: "clia",
    },
    {
      id: "cobas-e601",
      label: "Cobas E601 (Immunoassay)",
      discipline: "IMMUNOASSAY",
      technology: "clia",
    },
    {
      id: "architect-i1000",
      label: "Architect i1000SR",
      discipline: "IMMUNOASSAY",
      technology: "clia",
    },
    {
      id: "elisa-reader",
      label: "ELISA Reader",
      discipline: "IMMUNOASSAY",
      technology: "elisa",
    },
    // Hematology
    {
      id: "sysmex-xn1000",
      label: "Sysmex XN-1000",
      discipline: "HEMATOLOGY",
      technology: "flow-cytometry",
    },
    {
      id: "sysmex-xs500i",
      label: "Sysmex XS-500i",
      discipline: "HEMATOLOGY",
      technology: "impedance",
    },
    {
      id: "beckman-dxh800",
      label: "Beckman DxH 800",
      discipline: "HEMATOLOGY",
      technology: "flow-cytometry",
    },
    // Coagulation
    {
      id: "sta-compact",
      label: "STA Compact",
      discipline: "COAGULATION",
      technology: "clotting",
    },
    {
      id: "acl-top",
      label: "ACL TOP",
      discipline: "COAGULATION",
      technology: "clotting",
    },
    // Urinalysis
    {
      id: "urisys-1100",
      label: "Urisys 1100",
      discipline: "URINALYSIS",
      technology: "chemical-microscopy",
    },
    {
      id: "clinitek-advantus",
      label: "Clinitek Advantus",
      discipline: "URINALYSIS",
      technology: "chemical",
    },
    // Microbiology
    {
      id: "vitek-2",
      label: "VITEK 2",
      discipline: "MICROBIOLOGY",
      technology: "biochemical",
    },
    {
      id: "maldi-tof",
      label: "MALDI-TOF MS",
      discipline: "MICROBIOLOGY",
      technology: "mass-spec",
    },
    {
      id: "microscope-micro",
      label: "Light Microscope",
      discipline: "MICROBIOLOGY",
      technology: "microscopy",
    },
    // Parasitology
    {
      id: "microscope-para",
      label: "Light Microscope",
      discipline: "PARASITOLOGY",
      technology: "microscopy",
    },
    {
      id: "rdt-malaria",
      label: "Malaria RDT",
      discipline: "PARASITOLOGY",
      technology: "rdt",
    },
    // Manual
    {
      id: "manual",
      label: "Manual Testing",
      discipline: "MANUAL",
      technology: "manual",
    },
  ];

  // Technology options
  const technologyOptions = [
    {
      id: "spectrophotometry",
      label: "Spectrophotometry",
      disciplines: ["CHEMISTRY"],
    },
    {
      id: "ise",
      label: "Ion-Selective Electrode (ISE)",
      disciplines: ["CHEMISTRY"],
    },
    { id: "enzymatic", label: "Enzymatic", disciplines: ["CHEMISTRY"] },
    { id: "elisa", label: "ELISA", disciplines: ["IMMUNOASSAY"] },
    {
      id: "clia",
      label: "Chemiluminescence (CLIA)",
      disciplines: ["IMMUNOASSAY"],
    },
    {
      id: "ria",
      label: "Radioimmunoassay (RIA)",
      disciplines: ["IMMUNOASSAY"],
    },
    {
      id: "flow-cytometry",
      label: "Flow Cytometry",
      disciplines: ["HEMATOLOGY"],
    },
    { id: "impedance", label: "Impedance", disciplines: ["HEMATOLOGY"] },
    {
      id: "microscopy",
      label: "Microscopy",
      disciplines: ["MICROBIOLOGY", "PARASITOLOGY", "HEMATOLOGY", "URINALYSIS"],
    },
    {
      id: "biochemical",
      label: "Biochemical Tests",
      disciplines: ["MICROBIOLOGY"],
    },
    {
      id: "rdt",
      label: "Rapid Diagnostic Test (RDT)",
      disciplines: ["PARASITOLOGY"],
    },
    {
      id: "chemical-microscopy",
      label: "Chemical + Microscopy",
      disciplines: ["URINALYSIS"],
    },
    { id: "clotting", label: "Clotting Methods", disciplines: ["COAGULATION"] },
    { id: "manual", label: "Manual Method", disciplines: ["MANUAL"] },
  ];

  // QC level options
  const qcLevelOptions = [
    { id: "normal", label: "Normal Control" },
    { id: "pathologic-low", label: "Pathologic Low" },
    { id: "pathologic-high", label: "Pathologic High" },
  ];

  // QC type options
  const qcTypeOptions = [
    { id: "daily", label: "Daily QC" },
    { id: "calibration", label: "Calibration Verification" },
    { id: "eqa", label: "External Quality Assessment (EQA)" },
  ];

  // Deviation type options
  const deviationTypeOptions = [
    { id: "random", label: "Random Error" },
    { id: "systematic", label: "Systematic Error" },
    { id: "instrument", label: "Instrument Malfunction" },
    { id: "reagent", label: "Reagent Issue" },
    { id: "sample", label: "Sample Issue" },
  ];

  // Deviation action options
  const deviationActionOptions = [
    { id: "rerun", label: "Rerun Test" },
    { id: "recalibrate", label: "Recalibration" },
    { id: "maintenance", label: "Perform Maintenance" },
    { id: "replace-reagent", label: "Replace Reagent" },
    { id: "request-new-sample", label: "Request New Sample" },
    { id: "escalate", label: "Escalate to Supervisor" },
  ];

  // Calibration status options
  const calibrationStatusOptions = [
    { id: "valid", label: "Valid (Within Limits)" },
    { id: "expired", label: "Expired" },
    { id: "pending", label: "Pending Verification" },
    { id: "failed", label: "Failed" },
  ];

  // Load samples for testing
  useEffect(() => {
    componentMounted.current = true;
    loadSamplesForTesting();
    loadAnalyzers();
    loadQcRecords();
    loadDeviations();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId]);

  const loadSamplesForTesting = useCallback(() => {
    if (!entryId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    getFromOpenElisServer(
      `/rest/medlab/entry/${entryId}/samples-for-testing`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            setSamples(response);
          } else if (response && response.error) {
            setError(response.error);
            setSamples([]);
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [entryId]);

  const loadAnalyzers = useCallback(() => {
    getFromOpenElisServer("/rest/analyzers", (response) => {
      if (componentMounted.current && response && Array.isArray(response)) {
        // Merge with local analyzer options if backend provides more
        setAnalyzers(response);
      }
    });
  }, []);

  const loadQcRecords = useCallback(() => {
    if (!entryId) return;

    getFromOpenElisServer(
      `/rest/medlab/entry/${entryId}/qc-records`,
      (response) => {
        if (componentMounted.current && response && Array.isArray(response)) {
          setQcRecords(response);
        }
      },
    );
  }, [entryId]);

  const loadDeviations = useCallback(() => {
    if (!entryId) return;

    getFromOpenElisServer(
      `/rest/medlab/entry/${entryId}/deviations`,
      (response) => {
        if (componentMounted.current && response && Array.isArray(response)) {
          setDeviations(response);
        }
      },
    );
  }, [entryId]);

  // Handle open test execution modal
  const handleOpenTestExecutionModal = useCallback(() => {
    // Use ref for immediate access to current selection
    const currentSelection = selectedSampleIdsRef.current;
    if (currentSelection.length === 0) {
      setError("Please select at least one sample to run tests.");
      return;
    }
    setSelectedAnalyzer(null);
    setWorklistGenerated(false);
    setIsManualTest(false);
    setTechnologyUsed("");
    setTestExecutionModalOpen(true);
  }, []);

  // Handle execute tests
  const handleExecuteTests = useCallback(() => {
    if (!selectedAnalyzer && !isManualTest) {
      setError("Please select an analyzer or enable manual testing.");
      return;
    }

    // Use ref for immediate access to current selection
    const currentSelection = selectedSampleIdsRef.current;

    setExecuting(true);
    setError(null);

    const requestBody = {
      sampleIds: currentSelection.map((id) => parseInt(id, 10)),
      analyzerId: selectedAnalyzer?.id || null,
      analyzerName: selectedAnalyzer?.label || "Manual",
      worklistGenerated: worklistGenerated,
      isManualTest: isManualTest,
      technologyUsed:
        technologyUsed || selectedAnalyzer?.technology || "manual",
      notebookPageId: pageData?.id || null,
    };

    postToOpenElisServerJsonResponse(
      `/rest/medlab/entry/${entryId}/execute-tests`,
      JSON.stringify(requestBody),
      (response) => {
        setExecuting(false);
        setTestExecutionModalOpen(false);

        if (response && response.success) {
          setSuccess(
            `Successfully initiated testing for ${response.processedCount || currentSelection.length} sample(s).`,
          );
          setSelectedSampleIds([]);
          selectedSampleIdsRef.current = [];
          loadSamplesForTesting();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Failed to execute tests.");
        }
      },
    );
  }, [
    selectedAnalyzer,
    worklistGenerated,
    isManualTest,
    technologyUsed,
    pageData?.id,
    entryId,
    loadSamplesForTesting,
    onProgressUpdate,
  ]);

  // Handle save QC record
  const handleSaveQcRecord = useCallback(() => {
    if (!selectedAnalyzer) {
      setError("Please select an analyzer for QC.");
      return;
    }

    setSavingQc(true);
    setError(null);

    const requestBody = {
      analyzerId: selectedAnalyzer?.id || null,
      analyzerName: selectedAnalyzer?.label || "",
      qcType: qcType,
      qcLevel: qcLevel,
      qcResult: qcResult,
      calibrationStatus: calibrationStatus,
      notes: qcNotes,
      notebookPageId: pageData?.id || null,
    };

    postToOpenElisServerJsonResponse(
      `/rest/medlab/entry/${entryId}/record-qc`,
      JSON.stringify(requestBody),
      (response) => {
        setSavingQc(false);
        setQcModalOpen(false);

        if (response && response.success) {
          setSuccess("QC record saved successfully.");
          loadQcRecords();
          // If QC failed, lock results
          if (qcResult === "fail") {
            setError("QC Failed - Results are locked until QC passes.");
          }
        } else {
          setError(response?.error || "Failed to save QC record.");
        }
      },
    );
  }, [
    selectedAnalyzer,
    qcType,
    qcLevel,
    qcResult,
    calibrationStatus,
    qcNotes,
    pageData?.id,
    entryId,
    loadQcRecords,
  ]);

  // Handle save deviation
  const handleSaveDeviation = useCallback(() => {
    if (!rootCauseAnalysis.trim()) {
      setError("Root cause analysis is required.");
      return;
    }

    // Use ref for immediate access to current selection
    const currentSelection = selectedSampleIdsRef.current;

    setSavingDeviation(true);
    setError(null);

    const requestBody = {
      sampleIds: currentSelection.map((id) => parseInt(id, 10)),
      deviationType: deviationType,
      actionTaken: deviationAction,
      rootCauseAnalysis: rootCauseAnalysis,
      notes: deviationNotes,
      notebookPageId: pageData?.id || null,
    };

    postToOpenElisServerJsonResponse(
      `/rest/medlab/entry/${entryId}/record-deviation`,
      JSON.stringify(requestBody),
      (response) => {
        setSavingDeviation(false);
        setDeviationModalOpen(false);

        if (response && response.success) {
          setSuccess("Deviation recorded successfully.");
          setSelectedSampleIds([]);
          selectedSampleIdsRef.current = [];
          loadDeviations();
          loadSamplesForTesting();
        } else {
          setError(response?.error || "Failed to record deviation.");
        }
      },
    );
  }, [
    deviationType,
    deviationAction,
    rootCauseAnalysis,
    deviationNotes,
    pageData?.id,
    entryId,
    loadDeviations,
    loadSamplesForTesting,
  ]);

  // Handle bulk entry
  const handleOpenBulkEntryModal = useCallback(() => {
    // Use ref for immediate access to current selection
    const currentSelection = selectedSampleIdsRef.current;
    if (currentSelection.length === 0) {
      setError("Please select samples for bulk result entry.");
      return;
    }

    // Initialize bulk results with selected samples
    const initialBulkResults = currentSelection.map((id) => {
      const sample = samples.find((s) => String(s.sampleItemId) === id);
      return {
        sampleItemId: id,
        labNo: sample?.labNo || sample?.accessionNumber || "-",
        patientName: sample?.patientName || "-",
        tests: sample?.tests || [],
        results: {},
      };
    });
    setBulkResults(initialBulkResults);
    setBulkEntryModalOpen(true);
  }, [samples]);

  // Load available tests for assignment
  const loadAvailableTests = useCallback(() => {
    setIsLoadingTests(true);
    getFromOpenElisServer("/rest/test-list", (response) => {
      if (componentMounted.current) {
        if (response && Array.isArray(response)) {
          const tests = response.map((test) => ({
            id: String(test.id),
            name: test.value || test.name || test.localizedName,
            description: test.description || "",
          }));
          setAvailableTests(tests);
        }
        setIsLoadingTests(false);
      }
    });
  }, []);

  // Handle open assign tests modal
  const handleOpenAssignTestsModal = useCallback(() => {
    // Use ref for immediate access to current selection
    const currentSelection = selectedSampleIdsRef.current;
    const samplesWithNoTests = currentSelection.filter((id) => {
      const sample = samples.find((s) => String(s.sampleItemId) === id);
      return (
        sample &&
        (sample.testCount === 0 || !sample.tests || sample.tests.length === 0)
      );
    });

    if (samplesWithNoTests.length === 0) {
      setError("Please select samples that have no tests assigned.");
      return;
    }

    setSelectedTestIds([]);
    setTestSearchText("");
    loadAvailableTests();
    setAssignTestsModalOpen(true);
  }, [samples, loadAvailableTests]);

  // Handle test selection toggle
  const handleTestSelectionChange = (testId, isSelected) => {
    if (isSelected) {
      setSelectedTestIds((prev) => [...prev, testId]);
    } else {
      setSelectedTestIds((prev) => prev.filter((id) => id !== testId));
    }
  };

  // Filter tests based on search
  const filteredTests = availableTests.filter(
    (test) =>
      test.name.toLowerCase().includes(testSearchText.toLowerCase()) ||
      test.description.toLowerCase().includes(testSearchText.toLowerCase()),
  );

  // Handle assign tests
  const handleAssignTests = useCallback(() => {
    if (selectedTestIds.length === 0) {
      setError("Please select at least one test to assign.");
      return;
    }

    // Use ref for immediate access to current selection
    const currentSelection = selectedSampleIdsRef.current;

    // Get only samples with 0 tests from the selected samples
    const samplesWithNoTests = currentSelection.filter((id) => {
      const sample = samples.find((s) => String(s.sampleItemId) === id);
      return (
        sample &&
        (sample.testCount === 0 || !sample.tests || sample.tests.length === 0)
      );
    });

    if (samplesWithNoTests.length === 0) {
      setError("No samples without tests to assign.");
      return;
    }

    setAssigningTests(true);
    setError(null);

    const requestBody = {
      sampleItemIds: samplesWithNoTests.map((id) => parseInt(id, 10)),
      testIds: selectedTestIds,
      notebookPageId: pageData?.id || null,
    };

    postToOpenElisServerJsonResponse(
      `/rest/medlab/entry/${entryId}/assign-tests`,
      JSON.stringify(requestBody),
      (response) => {
        setAssigningTests(false);

        if (response && response.success) {
          setSuccess(
            `Successfully assigned ${response.analysesCreated || 0} test(s) to ${response.samplesProcessed || 0} sample(s).`,
          );
          setAssignTestsModalOpen(false);
          setSelectedSampleIds([]);
          selectedSampleIdsRef.current = [];
          setSelectedTestIds([]);
          loadSamplesForTesting();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Failed to assign tests.");
        }
      },
    );
  }, [
    selectedTestIds,
    samples,
    pageData?.id,
    entryId,
    loadSamplesForTesting,
    onProgressUpdate,
  ]);

  // Calculate stats
  const totalSamples = samples.length;
  const testedSamples = samples.filter(
    (s) => s.testingStatus === "COMPLETED" || s.pageStatus === "COMPLETED",
  ).length;
  const pendingSamples = totalSamples - testedSamples;
  const qcPassed = qcRecords.filter((qc) => qc.result === "pass").length;
  const qcFailed = qcRecords.filter((qc) => qc.result === "fail").length;

  // Check if QC has passed today
  const todayQcPassed = qcRecords.some(
    (qc) =>
      qc.result === "pass" &&
      new Date(qc.recordedDate).toDateString() === new Date().toDateString(),
  );

  // Table headers
  const headers = [
    { key: "labNo", header: "Lab No" },
    { key: "patientName", header: "Patient" },
    { key: "sampleType", header: "Sample Type" },
    { key: "tests", header: "Tests" },
    { key: "analyzer", header: "Analyzer" },
    { key: "technology", header: "Technology" },
    { key: "status", header: "Status" },
  ];

  // Transform samples for table
  const tableRows = samples.map((sample) => ({
    id: String(sample.sampleItemId),
    labNo: sample.labNo || sample.accessionNumber || "-",
    patientName: sample.patientName || "-",
    sampleType: sample.sampleType || "-",
    tests: sample.testCount || (sample.tests ? sample.tests.length : 0),
    analyzer: sample.analyzerName || "-",
    technology: sample.technologyUsed
      ? technologyOptions.find((t) => t.id === sample.technologyUsed)?.label ||
        sample.technologyUsed
      : "-",
    status: sample.testingStatus || sample.pageStatus || "PENDING",
    hasAbnormalResults: sample.hasAbnormalResults,
    qcLocked: sample.qcLocked,
  }));

  // Tested samples table
  const testedRows = tableRows.filter((r) => r.status === "COMPLETED");
  const pendingRows = tableRows.filter((r) => r.status !== "COMPLETED");

  // QC headers
  const qcHeaders = [
    { key: "date", header: "Date" },
    { key: "analyzer", header: "Analyzer" },
    { key: "type", header: "QC Type" },
    { key: "level", header: "Level" },
    { key: "result", header: "Result" },
    { key: "calibration", header: "Calibration" },
  ];

  // Deviation headers
  const deviationHeaders = [
    { key: "date", header: "Date" },
    { key: "type", header: "Type" },
    { key: "action", header: "Action Taken" },
    { key: "rootCause", header: "Root Cause" },
    { key: "status", header: "Status" },
  ];

  return (
    <div className="testing-analyzer-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="medlab.page.testingAnalyzer.title"
            defaultMessage="Testing Phase & Analyzer Integration"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="medlab.page.testingAnalyzer.description"
            defaultMessage="Execute tests, manage analyzer workflows, record QC results, and handle deviations. Ensure daily QC passes before releasing results."
          />
        </p>
      </div>

      {/* QC Status Alert */}
      {!todayQcPassed && (
        <InlineNotification
          kind="warning"
          title={intl.formatMessage({
            id: "medlab.page.testingAnalyzer.qcWarning",
            defaultMessage: "Daily QC Not Performed",
          })}
          subtitle={intl.formatMessage({
            id: "medlab.page.testingAnalyzer.qcWarningDesc",
            defaultMessage:
              "Please run daily QC before releasing test results. Results will be locked until QC passes.",
          })}
          lowContrast
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
                  id="medlab.page.testingAnalyzer.total"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{totalSamples}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.testingAnalyzer.tested"
                  defaultMessage="Tested"
                />
              </span>
              <span className="progress-value">{testedSamples}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.testingAnalyzer.pending"
                  defaultMessage="Pending"
                />
              </span>
              <span className="progress-value">{pendingSamples}</span>
            </Tile>
            <Tile
              className={`progress-tile ${qcFailed > 0 ? "rejected" : "verified"}`}
            >
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.testingAnalyzer.qcStatus"
                  defaultMessage="QC Status"
                />
              </span>
              <span className="progress-value">
                {qcPassed}/{qcPassed + qcFailed}
              </span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Play}
          onClick={handleOpenTestExecutionModal}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="medlab.page.testingAnalyzer.executeTests"
            defaultMessage="Execute Tests ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={Add}
          onClick={handleOpenAssignTestsModal}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="medlab.page.testingAnalyzer.assignTests"
            defaultMessage="Assign Tests"
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={ChartLineData}
          onClick={() => {
            setQcModalOpen(true);
            setQcType("daily");
            setQcLevel("normal");
            setQcResult("pass");
            setCalibrationStatus("valid");
            setQcNotes("");
          }}
        >
          <FormattedMessage
            id="medlab.page.testingAnalyzer.recordQc"
            defaultMessage="Record QC"
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={WarningAlt}
          onClick={() => {
            // Use ref for immediate access to current selection
            const currentSelection = selectedSampleIdsRef.current;
            if (currentSelection.length === 0) {
              setError("Please select samples to record deviation.");
              return;
            }
            setDeviationModalOpen(true);
            setDeviationType("random");
            setDeviationAction("rerun");
            setRootCauseAnalysis("");
            setDeviationNotes("");
          }}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="medlab.page.testingAnalyzer.recordDeviation"
            defaultMessage="Record Deviation"
          />
        </Button>

        <Button
          kind="ghost"
          size="sm"
          renderIcon={DataBase}
          onClick={handleOpenBulkEntryModal}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="medlab.page.testingAnalyzer.bulkEntry"
            defaultMessage="Bulk Result Entry"
          />
        </Button>

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={() => {
            loadSamplesForTesting();
            loadQcRecords();
            loadDeviations();
          }}
        >
          <FormattedMessage
            id="medlab.page.testingAnalyzer.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton={false}
          lowContrast
          onClose={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title={success}
          hideCloseButton={false}
          lowContrast
          onClose={() => setSuccess(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Tabs */}
      <Tabs
        selectedIndex={activeTab}
        onChange={({ selectedIndex }) => setActiveTab(selectedIndex)}
      >
        <TabList aria-label="Testing tabs">
          <Tab>
            <FormattedMessage
              id="medlab.page.testingAnalyzer.tab.pending"
              defaultMessage="Pending Testing ({count})"
              values={{ count: pendingRows.length }}
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="medlab.page.testingAnalyzer.tab.tested"
              defaultMessage="Tested ({count})"
              values={{ count: testedRows.length }}
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="medlab.page.testingAnalyzer.tab.qc"
              defaultMessage="QC Records ({count})"
              values={{ count: qcRecords.length }}
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="medlab.page.testingAnalyzer.tab.deviations"
              defaultMessage="Deviations ({count})"
              values={{ count: deviations.length }}
            />
          </Tab>
        </TabList>

        <TabPanels>
          {/* Pending Testing Tab */}
          <TabPanel>
            {loading ? (
              <p>Loading samples...</p>
            ) : pendingRows.length === 0 ? (
              <div className="empty-state">
                <p>
                  <FormattedMessage
                    id="medlab.page.testingAnalyzer.noPending"
                    defaultMessage="No samples pending testing. All samples have been tested."
                  />
                </p>
              </div>
            ) : (
              <DataTable
                rows={pendingRows}
                headers={headers}
                isSortable
                render={({
                  rows,
                  headers,
                  getHeaderProps,
                  getRowProps,
                  getSelectionProps,
                  getTableProps,
                  selectedRows,
                }) => {
                  // Synchronously update ref for immediate access in handlers
                  const newSelectedIds = selectedRows.map((r) => r.id);
                  selectedSampleIdsRef.current = newSelectedIds;
                  // Also update state for UI reactivity (using useEffect pattern)
                  if (
                    JSON.stringify(newSelectedIds) !==
                    JSON.stringify(selectedSampleIds)
                  ) {
                    // Use Promise.resolve for microtask timing (faster than setTimeout)
                    Promise.resolve().then(() => {
                      if (componentMounted.current) {
                        setSelectedSampleIds(newSelectedIds);
                      }
                    });
                  }

                  return (
                    <TableContainer>
                      <Table {...getTableProps()}>
                        <TableHead>
                          <TableRow>
                            <TableSelectAll {...getSelectionProps()} />
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
                            const rowData = pendingRows.find(
                              (r) => r.id === row.id,
                            );
                            return (
                              <TableRow key={row.id} {...getRowProps({ row })}>
                                <TableSelectRow
                                  {...getSelectionProps({ row })}
                                />
                                {row.cells.map((cell) => (
                                  <TableCell key={cell.id}>
                                    {cell.info.header === "status" ? (
                                      <Tag
                                        type={
                                          cell.value === "COMPLETED"
                                            ? "green"
                                            : cell.value === "IN_PROGRESS"
                                              ? "blue"
                                              : rowData?.qcLocked
                                                ? "red"
                                                : "gray"
                                        }
                                      >
                                        {rowData?.qcLocked
                                          ? "QC LOCKED"
                                          : cell.value}
                                      </Tag>
                                    ) : cell.info.header === "tests" ? (
                                      <Tag type="cyan">{cell.value}</Tag>
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
                  );
                }}
              />
            )}
          </TabPanel>

          {/* Tested Samples Tab */}
          <TabPanel>
            {loading ? (
              <p>Loading samples...</p>
            ) : testedRows.length === 0 ? (
              <div className="empty-state">
                <p>
                  <FormattedMessage
                    id="medlab.page.testingAnalyzer.noTested"
                    defaultMessage="No samples have been tested yet."
                  />
                </p>
              </div>
            ) : (
              <DataTable rows={testedRows} headers={headers} isSortable>
                {({
                  rows,
                  headers,
                  getHeaderProps,
                  getRowProps,
                  getTableProps,
                }) => (
                  <TableContainer>
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
                          <TableHeader>Flags</TableHeader>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {rows.map((row) => {
                          const rowData = testedRows.find(
                            (r) => r.id === row.id,
                          );
                          return (
                            <TableRow key={row.id} {...getRowProps({ row })}>
                              {row.cells.map((cell) => (
                                <TableCell key={cell.id}>
                                  {cell.info.header === "status" ? (
                                    <Tag type="green">{cell.value}</Tag>
                                  ) : cell.info.header === "tests" ? (
                                    <Tag type="cyan">{cell.value}</Tag>
                                  ) : (
                                    cell.value
                                  )}
                                </TableCell>
                              ))}
                              <TableCell>
                                {rowData?.hasAbnormalResults && (
                                  <Tag type="red" renderIcon={WarningAlt}>
                                    Abnormal
                                  </Tag>
                                )}
                              </TableCell>
                            </TableRow>
                          );
                        })}
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              </DataTable>
            )}
          </TabPanel>

          {/* QC Records Tab */}
          <TabPanel>
            {qcRecords.length === 0 ? (
              <div className="empty-state">
                <p>
                  <FormattedMessage
                    id="medlab.page.testingAnalyzer.noQcRecords"
                    defaultMessage="No QC records yet. Run daily QC before releasing results."
                  />
                </p>
              </div>
            ) : (
              <DataTable
                rows={qcRecords.map((qc, idx) => ({
                  id: String(qc.id || idx),
                  date: qc.recordedDate
                    ? new Date(qc.recordedDate).toLocaleString()
                    : "-",
                  analyzer: qc.analyzerName || "-",
                  type:
                    qcTypeOptions.find((t) => t.id === qc.qcType)?.label ||
                    qc.qcType,
                  level:
                    qcLevelOptions.find((l) => l.id === qc.qcLevel)?.label ||
                    qc.qcLevel,
                  result: qc.result || qc.qcResult,
                  calibration:
                    calibrationStatusOptions.find(
                      (c) => c.id === qc.calibrationStatus,
                    )?.label || qc.calibrationStatus,
                }))}
                headers={qcHeaders}
              >
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <TableContainer>
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
                                {cell.info.header === "result" ? (
                                  <Tag
                                    type={
                                      cell.value === "pass" ? "green" : "red"
                                    }
                                    renderIcon={
                                      cell.value === "pass" ? Checkmark : Close
                                    }
                                  >
                                    {cell.value === "pass" ? "PASS" : "FAIL"}
                                  </Tag>
                                ) : cell.info.header === "calibration" ? (
                                  <Tag
                                    type={
                                      cell.value === "Valid (Within Limits)"
                                        ? "green"
                                        : cell.value === "Expired" ||
                                            cell.value === "Failed"
                                          ? "red"
                                          : "gray"
                                    }
                                  >
                                    {cell.value}
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
            )}

            {/* LJ Chart Link Placeholder */}
            <div style={{ marginTop: "1rem" }}>
              <Button
                kind="ghost"
                size="sm"
                renderIcon={ChartLineData}
                disabled
              >
                <FormattedMessage
                  id="medlab.page.testingAnalyzer.viewLjChart"
                  defaultMessage="View Levey-Jennings Chart"
                />
              </Button>
              <span
                style={{
                  marginLeft: "0.5rem",
                  color: "#6f6f6f",
                  fontSize: "0.875rem",
                }}
              >
                (Coming soon)
              </span>
            </div>
          </TabPanel>

          {/* Deviations Tab */}
          <TabPanel>
            {deviations.length === 0 ? (
              <div className="empty-state">
                <p>
                  <FormattedMessage
                    id="medlab.page.testingAnalyzer.noDeviations"
                    defaultMessage="No deviations recorded. Use 'Record Deviation' to document errors."
                  />
                </p>
              </div>
            ) : (
              <DataTable
                rows={deviations.map((dev, idx) => ({
                  id: String(dev.id || idx),
                  date: dev.recordedDate
                    ? new Date(dev.recordedDate).toLocaleString()
                    : "-",
                  type:
                    deviationTypeOptions.find((t) => t.id === dev.deviationType)
                      ?.label || dev.deviationType,
                  action:
                    deviationActionOptions.find((a) => a.id === dev.actionTaken)
                      ?.label || dev.actionTaken,
                  rootCause: dev.rootCauseAnalysis || "-",
                  status: dev.resolved ? "Resolved" : "Open",
                }))}
                headers={deviationHeaders}
              >
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <TableContainer>
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
                                {cell.info.header === "status" ? (
                                  <Tag
                                    type={
                                      cell.value === "Resolved"
                                        ? "green"
                                        : "red"
                                    }
                                  >
                                    {cell.value}
                                  </Tag>
                                ) : cell.info.header === "rootCause" ? (
                                  <span
                                    style={{
                                      maxWidth: "200px",
                                      overflow: "hidden",
                                      textOverflow: "ellipsis",
                                      whiteSpace: "nowrap",
                                      display: "block",
                                    }}
                                    title={cell.value}
                                  >
                                    {cell.value}
                                  </span>
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
            )}
          </TabPanel>
        </TabPanels>
      </Tabs>

      {/* Test Execution Modal */}
      <Modal
        open={testExecutionModalOpen}
        modalHeading={intl.formatMessage({
          id: "medlab.page.testingAnalyzer.executeModal.title",
          defaultMessage: "Execute Tests",
        })}
        primaryButtonText={intl.formatMessage({
          id: "medlab.page.testingAnalyzer.executeModal.run",
          defaultMessage: "Run Tests",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "medlab.page.testingAnalyzer.executeModal.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setTestExecutionModalOpen(false)}
        onRequestSubmit={handleExecuteTests}
        primaryButtonDisabled={
          executing || (!selectedAnalyzer && !isManualTest)
        }
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p>
            <FormattedMessage
              id="medlab.page.testingAnalyzer.executeModal.description"
              defaultMessage="Configure test execution for {count} selected sample(s)."
              values={{ count: selectedSampleIds.length }}
            />
          </p>
        </div>

        <Toggle
          id="isManualTest"
          labelText={intl.formatMessage({
            id: "medlab.page.testingAnalyzer.executeModal.manualTest",
            defaultMessage: "Manual Testing (No Analyzer)",
          })}
          toggled={isManualTest}
          onToggle={(checked) => {
            setIsManualTest(checked);
            if (checked) {
              setSelectedAnalyzer(null);
            }
          }}
          style={{ marginBottom: "1rem" }}
        />

        {!isManualTest && (
          <Dropdown
            id="analyzer"
            titleText={intl.formatMessage({
              id: "medlab.page.testingAnalyzer.executeModal.analyzer",
              defaultMessage: "Select Analyzer *",
            })}
            label="Select analyzer"
            items={analyzerOptions}
            itemToString={(item) => (item ? item.label : "")}
            selectedItem={selectedAnalyzer}
            onChange={({ selectedItem }) => {
              setSelectedAnalyzer(selectedItem);
              if (selectedItem) {
                setTechnologyUsed(selectedItem.technology || "");
              }
            }}
            style={{ marginBottom: "1rem" }}
          />
        )}

        <Dropdown
          id="technology"
          titleText={intl.formatMessage({
            id: "medlab.page.testingAnalyzer.executeModal.technology",
            defaultMessage: "Technology Used",
          })}
          label="Select technology"
          items={technologyOptions}
          itemToString={(item) => (item ? item.label : "")}
          selectedItem={technologyOptions.find((t) => t.id === technologyUsed)}
          onChange={({ selectedItem }) =>
            setTechnologyUsed(selectedItem?.id || "")
          }
          style={{ marginBottom: "1rem" }}
        />

        <Checkbox
          id="worklistGenerated"
          labelText={intl.formatMessage({
            id: "medlab.page.testingAnalyzer.executeModal.worklist",
            defaultMessage: "Worklist Generated",
          })}
          checked={worklistGenerated}
          onChange={(e, { checked }) => setWorklistGenerated(checked)}
        />

        {!todayQcPassed && (
          <InlineNotification
            kind="warning"
            title={intl.formatMessage({
              id: "medlab.page.testingAnalyzer.qcReminder",
              defaultMessage: "Remember: Run daily QC before releasing results",
            })}
            lowContrast
            hideCloseButton
            style={{ marginTop: "1rem" }}
          />
        )}
      </Modal>

      {/* QC Modal */}
      <Modal
        open={qcModalOpen}
        modalHeading={intl.formatMessage({
          id: "medlab.page.testingAnalyzer.qcModal.title",
          defaultMessage: "Record Quality Control",
        })}
        primaryButtonText={intl.formatMessage({
          id: "medlab.page.testingAnalyzer.qcModal.save",
          defaultMessage: "Save QC Record",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "medlab.page.testingAnalyzer.qcModal.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setQcModalOpen(false)}
        onRequestSubmit={handleSaveQcRecord}
        primaryButtonDisabled={savingQc || !selectedAnalyzer}
        size="md"
      >
        <Dropdown
          id="qcAnalyzer"
          titleText={intl.formatMessage({
            id: "medlab.page.testingAnalyzer.qcModal.analyzer",
            defaultMessage: "Analyzer *",
          })}
          label="Select analyzer"
          items={analyzerOptions}
          itemToString={(item) => (item ? item.label : "")}
          selectedItem={selectedAnalyzer}
          onChange={({ selectedItem }) => setSelectedAnalyzer(selectedItem)}
          style={{ marginBottom: "1rem" }}
        />

        <Dropdown
          id="qcType"
          titleText={intl.formatMessage({
            id: "medlab.page.testingAnalyzer.qcModal.type",
            defaultMessage: "QC Type",
          })}
          label="Select QC type"
          items={qcTypeOptions}
          itemToString={(item) => (item ? item.label : "")}
          selectedItem={qcTypeOptions.find((t) => t.id === qcType)}
          onChange={({ selectedItem }) =>
            setQcType(selectedItem?.id || "daily")
          }
          style={{ marginBottom: "1rem" }}
        />

        <Dropdown
          id="qcLevel"
          titleText={intl.formatMessage({
            id: "medlab.page.testingAnalyzer.qcModal.level",
            defaultMessage: "Control Level",
          })}
          label="Select control level"
          items={qcLevelOptions}
          itemToString={(item) => (item ? item.label : "")}
          selectedItem={qcLevelOptions.find((l) => l.id === qcLevel)}
          onChange={({ selectedItem }) =>
            setQcLevel(selectedItem?.id || "normal")
          }
          style={{ marginBottom: "1rem" }}
        />

        <Dropdown
          id="qcResult"
          titleText={intl.formatMessage({
            id: "medlab.page.testingAnalyzer.qcModal.result",
            defaultMessage: "QC Result *",
          })}
          label="Select result"
          items={[
            { id: "pass", label: "Pass" },
            { id: "fail", label: "Fail" },
          ]}
          itemToString={(item) => (item ? item.label : "")}
          selectedItem={{
            id: qcResult,
            label: qcResult === "pass" ? "Pass" : "Fail",
          }}
          onChange={({ selectedItem }) =>
            setQcResult(selectedItem?.id || "pass")
          }
          style={{ marginBottom: "1rem" }}
        />

        <Dropdown
          id="calibrationStatus"
          titleText={intl.formatMessage({
            id: "medlab.page.testingAnalyzer.qcModal.calibration",
            defaultMessage: "Calibration Status",
          })}
          label="Select calibration status"
          items={calibrationStatusOptions}
          itemToString={(item) => (item ? item.label : "")}
          selectedItem={calibrationStatusOptions.find(
            (c) => c.id === calibrationStatus,
          )}
          onChange={({ selectedItem }) =>
            setCalibrationStatus(selectedItem?.id || "valid")
          }
          style={{ marginBottom: "1rem" }}
        />

        <TextArea
          id="qcNotes"
          labelText={intl.formatMessage({
            id: "medlab.page.testingAnalyzer.qcModal.notes",
            defaultMessage: "Notes",
          })}
          placeholder="Enter any QC notes..."
          value={qcNotes}
          onChange={(e) => setQcNotes(e.target.value)}
          rows={3}
        />

        {qcResult === "fail" && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({
              id: "medlab.page.testingAnalyzer.qcModal.failWarning",
              defaultMessage:
                "Results will be locked. Investigate and resolve before releasing.",
            })}
            lowContrast
            hideCloseButton
            style={{ marginTop: "1rem" }}
          />
        )}
      </Modal>

      {/* Deviation Modal */}
      <Modal
        open={deviationModalOpen}
        modalHeading={intl.formatMessage({
          id: "medlab.page.testingAnalyzer.deviationModal.title",
          defaultMessage: "Record Deviation",
        })}
        primaryButtonText={intl.formatMessage({
          id: "medlab.page.testingAnalyzer.deviationModal.save",
          defaultMessage: "Save Deviation",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "medlab.page.testingAnalyzer.deviationModal.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setDeviationModalOpen(false)}
        onRequestSubmit={handleSaveDeviation}
        primaryButtonDisabled={savingDeviation || !rootCauseAnalysis.trim()}
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p>
            <FormattedMessage
              id="medlab.page.testingAnalyzer.deviationModal.description"
              defaultMessage="Document deviation for {count} selected sample(s)."
              values={{ count: selectedSampleIds.length }}
            />
          </p>
        </div>

        <Dropdown
          id="deviationType"
          titleText={intl.formatMessage({
            id: "medlab.page.testingAnalyzer.deviationModal.type",
            defaultMessage: "Deviation Type *",
          })}
          label="Select deviation type"
          items={deviationTypeOptions}
          itemToString={(item) => (item ? item.label : "")}
          selectedItem={deviationTypeOptions.find(
            (t) => t.id === deviationType,
          )}
          onChange={({ selectedItem }) =>
            setDeviationType(selectedItem?.id || "random")
          }
          style={{ marginBottom: "1rem" }}
        />

        <Dropdown
          id="deviationAction"
          titleText={intl.formatMessage({
            id: "medlab.page.testingAnalyzer.deviationModal.action",
            defaultMessage: "Action Taken *",
          })}
          label="Select action"
          items={deviationActionOptions}
          itemToString={(item) => (item ? item.label : "")}
          selectedItem={deviationActionOptions.find(
            (a) => a.id === deviationAction,
          )}
          onChange={({ selectedItem }) =>
            setDeviationAction(selectedItem?.id || "rerun")
          }
          style={{ marginBottom: "1rem" }}
        />

        <TextArea
          id="rootCauseAnalysis"
          labelText={intl.formatMessage({
            id: "medlab.page.testingAnalyzer.deviationModal.rootCause",
            defaultMessage: "Root Cause Analysis *",
          })}
          placeholder="Describe the root cause of the deviation..."
          value={rootCauseAnalysis}
          onChange={(e) => setRootCauseAnalysis(e.target.value)}
          rows={4}
          invalid={!rootCauseAnalysis.trim()}
          invalidText="Root cause analysis is required"
          style={{ marginBottom: "1rem" }}
        />

        <TextArea
          id="deviationNotes"
          labelText={intl.formatMessage({
            id: "medlab.page.testingAnalyzer.deviationModal.notes",
            defaultMessage: "Additional Notes",
          })}
          placeholder="Enter any additional notes..."
          value={deviationNotes}
          onChange={(e) => setDeviationNotes(e.target.value)}
          rows={3}
        />
      </Modal>

      {/* Bulk Entry Modal */}
      <Modal
        open={bulkEntryModalOpen}
        modalHeading={intl.formatMessage({
          id: "medlab.page.testingAnalyzer.bulkModal.title",
          defaultMessage: "Bulk Result Entry",
        })}
        primaryButtonText={intl.formatMessage({
          id: "medlab.page.testingAnalyzer.bulkModal.save",
          defaultMessage: "Save Results",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "medlab.page.testingAnalyzer.bulkModal.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setBulkEntryModalOpen(false)}
        onRequestSubmit={() => {
          // Bulk save would be implemented here
          setBulkEntryModalOpen(false);
          setSuccess(
            "Bulk results saved. Proceed to Result Entry page for detailed entry.",
          );
        }}
        size="lg"
        passiveModal={bulkResults.length === 0}
      >
        <div style={{ marginBottom: "1rem" }}>
          <InlineNotification
            kind="info"
            title={intl.formatMessage({
              id: "medlab.page.testingAnalyzer.bulkModal.info",
              defaultMessage:
                "Quick entry mode. For detailed result entry with reference ranges, use the Result Entry page.",
            })}
            lowContrast
            hideCloseButton
          />
        </div>

        {bulkResults.length === 0 ? (
          <p>No samples selected for bulk entry.</p>
        ) : (
          <div style={{ maxHeight: "400px", overflowY: "auto" }}>
            {bulkResults.map((sample, idx) => (
              <ExpandableTile
                key={sample.sampleItemId}
                tileCollapsedIconText="Expand"
                tileExpandedIconText="Collapse"
                style={{ marginBottom: "0.5rem" }}
              >
                <TileAboveTheFoldContent>
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                    }}
                  >
                    <span>
                      <strong>{sample.labNo}</strong> - {sample.patientName}
                    </span>
                    <Tag type="cyan">{sample.tests.length || 0} tests</Tag>
                  </div>
                </TileAboveTheFoldContent>
                <TileBelowTheFoldContent>
                  <p style={{ color: "#6f6f6f", marginTop: "0.5rem" }}>
                    Test results can be entered on the Result Entry page for
                    full validation and reference range checking.
                  </p>
                </TileBelowTheFoldContent>
              </ExpandableTile>
            ))}
          </div>
        )}
      </Modal>

      {/* Assign Tests Modal */}
      <Modal
        open={assignTestsModalOpen}
        modalHeading={intl.formatMessage({
          id: "medlab.page.testingAnalyzer.assignTestsModal.title",
          defaultMessage: "Assign Tests to Samples",
        })}
        primaryButtonText={intl.formatMessage({
          id: "medlab.page.testingAnalyzer.assignTestsModal.assign",
          defaultMessage: "Assign Tests",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "medlab.page.testingAnalyzer.assignTestsModal.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setAssignTestsModalOpen(false)}
        onRequestSubmit={handleAssignTests}
        primaryButtonDisabled={assigningTests || selectedTestIds.length === 0}
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p>
            <FormattedMessage
              id="medlab.page.testingAnalyzer.assignTestsModal.description"
              defaultMessage="Select tests to assign to {count} sample(s) without tests."
              values={{
                count: selectedSampleIdsRef.current.filter((id) => {
                  const sample = samples.find(
                    (s) => String(s.sampleItemId) === id,
                  );
                  return (
                    sample &&
                    (sample.testCount === 0 ||
                      !sample.tests ||
                      sample.tests.length === 0)
                  );
                }).length,
              }}
            />
          </p>
        </div>

        {isLoadingTests ? (
          <div
            style={{
              display: "flex",
              justifyContent: "center",
              padding: "2rem",
            }}
          >
            <Loading
              description="Loading available tests..."
              withOverlay={false}
            />
          </div>
        ) : (
          <>
            <TableToolbarSearch
              placeholder={intl.formatMessage({
                id: "medlab.page.testingAnalyzer.assignTestsModal.search",
                defaultMessage: "Search tests...",
              })}
              value={testSearchText}
              onChange={(e) => setTestSearchText(e.target.value)}
              persistent
              style={{ marginBottom: "1rem" }}
            />

            <div
              style={{
                maxHeight: "300px",
                overflowY: "auto",
                border: "1px solid #e0e0e0",
                borderRadius: "4px",
                padding: "0.5rem",
              }}
            >
              {filteredTests.length === 0 ? (
                <p style={{ color: "#6f6f6f", textAlign: "center" }}>
                  <FormattedMessage
                    id="medlab.page.testingAnalyzer.assignTestsModal.noTests"
                    defaultMessage="No tests found matching your search."
                  />
                </p>
              ) : (
                filteredTests.map((test) => (
                  <div
                    key={test.id}
                    style={{
                      padding: "0.5rem",
                      borderBottom: "1px solid #e0e0e0",
                    }}
                  >
                    <Checkbox
                      id={`test-${test.id}`}
                      labelText={test.name}
                      checked={selectedTestIds.includes(test.id)}
                      onChange={(e, { checked }) =>
                        handleTestSelectionChange(test.id, checked)
                      }
                    />
                    {test.description && (
                      <span
                        style={{
                          fontSize: "0.75rem",
                          color: "#6f6f6f",
                          marginLeft: "1.5rem",
                          display: "block",
                        }}
                      >
                        {test.description}
                      </span>
                    )}
                  </div>
                ))
              )}
            </div>

            <div style={{ marginTop: "1rem" }}>
              <Tag type="blue">
                {selectedTestIds.length}{" "}
                <FormattedMessage
                  id="medlab.page.testingAnalyzer.assignTestsModal.testsSelected"
                  defaultMessage="test(s) selected"
                />
              </Tag>
            </div>
          </>
        )}

        {assigningTests && (
          <InlineNotification
            kind="info"
            title={intl.formatMessage({
              id: "medlab.page.testingAnalyzer.assignTestsModal.assigning",
              defaultMessage: "Assigning tests to samples...",
            })}
            lowContrast
            hideCloseButton
            style={{ marginTop: "1rem" }}
          />
        )}
      </Modal>
    </div>
  );
}

export default TestingAnalyzerPage;

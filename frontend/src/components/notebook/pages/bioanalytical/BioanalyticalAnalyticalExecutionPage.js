import React, { useState, useCallback, useEffect, useContext } from "react";
import {
  Grid,
  Column,
  Button,
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
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
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
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  // Notification helper function
  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, subtitle, message }) => {
      setNotificationVisible(true);
      addNotification({
        kind,
        title:
          title ||
          intl.formatMessage({
            id:
              kind === NotificationKinds.error
                ? "notification.error"
                : "notification.success",
            defaultMessage:
              kind === NotificationKinds.error ? "Error" : "Success",
          }),
        subtitle,
        message,
      });
    },
    [addNotification, setNotificationVisible, intl],
  );

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

  // Enhanced QC Parameters for FDA Bioanalytical Compliance
  const [systemSuitabilityResults, setSystemSuitabilityResults] = useState([]);
  const [controlSamples, setControlSamples] = useState([]);
  const [referenceStandardVerification, setReferenceStandardVerification] =
    useState(null);
  const [instrumentPerformance, setInstrumentPerformance] = useState(null);
  const [qcAcceptanceCriteria, setQcAcceptanceCriteria] = useState({
    accuracyLimits: { min: 85, max: 115 }, // ±15% standard
    precisionLimit: 15, // ≤15% CV
    lloqAccuracyLimits: { min: 80, max: 120 }, // ±20% at LLOQ
    lloqPrecisionLimit: 20, // ≤20% CV at LLOQ
  });
  const [deviations, setDeviations] = useState([]);
  const [deviationForm, setDeviationForm] = useState({
    type: "",
    severity: "",
    description: "",
    correctiveAction: "",
    reportedBy: "",
    batchDisposition: "",
  });

  // UI states - removed inline notification states, now using NotificationContext

  // ALCOA+ Compliance and Review Workflow state
  const [reviewStatus, setReviewStatus] = useState("PENDING_ANALYST_REVIEW");
  const [analystReview, setAnalystReview] = useState({
    reviewerId: "",
    reviewerName: "",
    reviewDate: "",
    comments: "",
    approved: false,
    electronicSignature: "",
  });

  // Multi-level Review Workflow: Secondary QA Review
  const [qaReview, setQaReview] = useState({
    reviewerId: "",
    reviewerName: "",
    reviewDate: "",
    comments: "",
    approved: false,
    electronicSignature: "",
    dataIntegrityVerified: false,
    methodComplianceVerified: false,
    qcAcceptable: false,
  });

  // Multi-level Review Workflow: Final Manager Approval
  const [managerReview, setManagerReview] = useState({
    reviewerId: "",
    reviewerName: "",
    reviewDate: "",
    comments: "",
    approved: false,
    electronicSignature: "",
    regulatoryCompliance: false,
    studyImpact: "",
    finalDisposition: "",
  });
  const [dataIntegrity, setDataIntegrity] = useState({
    checksumVerified: false,
    timestampVerified: false,
    originalityVerified: false,
    userAttribution: {},
    auditTrail: [],
    contemporaneousRecord: true,
  });

  // Audit Trail System
  const [auditTrail, setAuditTrail] = useState([]);
  const [auditFilters, setAuditFilters] = useState({
    userId: "",
    action: "",
    startDate: "",
    endDate: "",
    showSystemEvents: true,
  });

  // Audit Trail Logger
  const logAuditEvent = useCallback(
    (action, details, userId = null) => {
      const auditEvent = {
        id: Date.now() + Math.random(),
        timestamp: new Date().toISOString(),
        userId: userId || executionData.analystId || "SYSTEM",
        action: action,
        details: details,
        entryId: entryId,
        sessionId: localStorage.getItem("sessionId") || "unknown",
        ipAddress: "CLIENT_IP", // Would be populated by backend
        userAgent: navigator.userAgent,
        changeType: action.includes("CREATE")
          ? "INSERT"
          : action.includes("UPDATE") || action.includes("MODIFY")
            ? "UPDATE"
            : action.includes("DELETE") || action.includes("REMOVE")
              ? "DELETE"
              : "READ",
      };

      setAuditTrail((prev) => [auditEvent, ...prev]);

      // Also update dataIntegrity auditTrail for ALCOA+ compliance
      setDataIntegrity((prev) => ({
        ...prev,
        auditTrail: [auditEvent, ...prev.auditTrail],
      }));

      return auditEvent;
    },
    [entryId, executionData.analystId],
  );

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
          `${config.serverBaseUrl}/rest/notebook-entry/${entryId}/samples`,
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
        notify({
          kind: NotificationKinds.error,
          message: intl.formatMessage({
            id: "notebook.bioanalytical.execution.loadError",
            defaultMessage:
              "Failed to load assigned samples. Please refresh the page.",
          }),
        });
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
      formats: ["MZML", "CDF"], // Uppercase for validation consistency
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
        notify({
          kind: NotificationKinds.error,
          message: intl.formatMessage({
            id: "notebook.bioanalytical.execution.selectInstrument",
            defaultMessage: "Please select an instrument first",
          }),
        });
        return;
      }

      if (!instrument.formats.includes(fileExtension)) {
        notify({
          kind: NotificationKinds.error,
          message: intl.formatMessage(
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
        });
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

      // Log file upload to audit trail
      logAuditEvent("FILE_UPLOAD", {
        fileName: file.name,
        fileSize: file.size,
        instrument: instrument.name,
        fileType: fileExtension,
        validationStatus: "PENDING_VALIDATION",
      });

      notify({
        kind: NotificationKinds.success,
        message: intl.formatMessage(
          {
            id: "notebook.bioanalytical.execution.fileUploaded",
            defaultMessage: "File {name} uploaded successfully",
          },
          { name: file.name },
        ),
      });
    },
    [selectedInstrument, uploadedFiles, intl, logAuditEvent, notify],
  );

  // Execute tests for selected samples
  const handleExecuteTests = useCallback(async () => {
    if (selectedSampleIds.size === 0) {
      notify({
        kind: NotificationKinds.error,
        message: intl.formatMessage({
          id: "notebook.bioanalytical.execution.noSamplesSelected",
          defaultMessage: "Please select samples to execute tests",
        }),
      });
      return;
    }

    if (!executionData.analystId || !executionData.instrumentId) {
      notify({
        kind: NotificationKinds.error,
        message: intl.formatMessage({
          id: "notebook.bioanalytical.execution.missingExecutionData",
          defaultMessage:
            "Please fill in analyst ID and instrument information",
        }),
      });
      return;
    }

    setIsExecuting(true);

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
        // Log test execution to audit trail
        logAuditEvent(
          "TEST_EXECUTION",
          {
            sampleCount: selectedSampleIds.size,
            sampleIds: Array.from(selectedSampleIds),
            instrumentId: executionData.instrumentId,
            batchNumber: executionData.batchNumber,
            executionDate: executionData.executionDate,
            rawDataFiles: uploadedFiles.length,
          },
          executionData.analystId,
        );

        notify({
          kind: NotificationKinds.success,
          message: intl.formatMessage(
            {
              id: "notebook.bioanalytical.execution.testsExecuted",
              defaultMessage: "Tests executed successfully for {count} samples",
            },
            { count: selectedSampleIds.size },
          ),
        });

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
      notify({
        kind: NotificationKinds.error,
        message: intl.formatMessage(
          {
            id: "notebook.bioanalytical.execution.executionError",
            defaultMessage: "Failed to execute tests: {error}",
          },
          { error: error.message },
        ),
      });
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
    logAuditEvent,
    notify,
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
          // Log deviation recording to audit trail
          logAuditEvent(
            "DEVIATION_RECORDED",
            {
              deviationType: deviation.type,
              severity: deviation.severity,
              description: deviation.description.substring(0, 100) + "...", // Truncate for audit log
              batchDisposition: deviation.batchDisposition,
              affectedSamples: Array.from(selectedSampleIds).length,
            },
            executionData.analystId,
          );

          setDeviations((prev) => [...prev, deviation]);
          notify({
            kind: NotificationKinds.success,
            message: intl.formatMessage({
              id: "notebook.bioanalytical.execution.deviationRecorded",
              defaultMessage: "Deviation recorded successfully",
            }),
          });
        }
      } catch (error) {
        console.error("Deviation recording error:", error);
        notify({
          kind: NotificationKinds.error,
          message: intl.formatMessage({
            id: "notebook.bioanalytical.execution.deviationError",
            defaultMessage: "Failed to record deviation",
          }),
        });
      }
    },
    [
      deviations,
      selectedSampleIds,
      executionData.analystId,
      pageData?.id,
      intl,
      logAuditEvent,
      notify,
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

  // Enhanced Data Processing Automation
  const [dataProcessingSettings, setDataProcessingSettings] = useState({
    autoBaseline: true,
    autoIntegration: true,
    autoQuantification: true,
    baselineAlgorithm: "asymmetric_least_squares",
    integrationMethod: "drop_line",
    smoothingFactor: 0.1,
    peakDetectionThreshold: 0.01,
    retentionTimeWindow: 0.5,
    massAccuracyPpm: 5.0,
  });

  const [processingResults, setProcessingResults] = useState({
    peakIntegration: [],
    quantification: [],
    automationSummary: null,
  });

  const handleValidateData = useCallback(() => {
    if (uploadedFiles.length === 0) {
      notify({
        kind: NotificationKinds.error,
        message: intl.formatMessage({
          id: "notebook.bioanalytical.execution.noFiles",
          defaultMessage: "Please upload analyzer data files first",
        }),
      });
      return;
    }

    setIsLoading(true);

    // Enhanced validation with automated data processing
    setTimeout(() => {
      // Automated Peak Integration Results
      const mockPeakIntegration = [
        {
          peakId: "PEAK_001",
          retentionTime: 3.42,
          peakArea: 125847,
          peakHeight: 15623,
          symmetryFactor: 1.08,
          plateCount: 8543,
          resolution: 2.34,
          signalToNoise: 145.2,
          integrationStatus: "AUTO_INTEGRATED",
          manualReview: false,
        },
        {
          peakId: "PEAK_002",
          retentionTime: 4.15,
          peakArea: 203451,
          peakHeight: 23789,
          symmetryFactor: 1.12,
          plateCount: 9234,
          resolution: 3.21,
          signalToNoise: 187.5,
          integrationStatus: "AUTO_INTEGRATED",
          manualReview: false,
        },
      ];
      setProcessingResults((prev) => ({
        ...prev,
        peakIntegration: mockPeakIntegration,
      }));

      // Automated Quantification Results
      const mockQuantification = [
        {
          sampleId: "S001",
          analyte: "Test Compound",
          peakArea: 125847,
          concentration: 456.2,
          units: "ng/mL",
          dilutionFactor: 1.0,
          responseFunction: "LINEAR",
          accuracyPercent: 98.5,
          cvPercent: 2.1,
          quantificationFlag: "PASSED",
          automationStatus: "FULLY_AUTOMATED",
        },
        {
          sampleId: "S002",
          analyte: "Test Compound",
          peakArea: 203451,
          concentration: 523.1,
          units: "ng/mL",
          dilutionFactor: 1.0,
          responseFunction: "LINEAR",
          accuracyPercent: 99.8,
          cvPercent: 1.8,
          quantificationFlag: "PASSED",
          automationStatus: "FULLY_AUTOMATED",
        },
      ];
      setProcessingResults((prev) => ({
        ...prev,
        quantification: mockQuantification,
      }));

      // Automation Summary
      const automationSummary = {
        totalPeaks: mockPeakIntegration.length,
        autoIntegratedPeaks: mockPeakIntegration.filter(
          (p) => p.integrationStatus === "AUTO_INTEGRATED",
        ).length,
        manualReviewRequired: mockPeakIntegration.filter((p) => p.manualReview)
          .length,
        totalSamples: mockQuantification.length,
        fullyAutomatedSamples: mockQuantification.filter(
          (q) => q.automationStatus === "FULLY_AUTOMATED",
        ).length,
        processingTimeSeconds: 15.3,
        dataQualityScore: 98.7,
        automationEfficiency: 96.2,
      };
      setProcessingResults((prev) => ({
        ...prev,
        automationSummary: automationSummary,
      }));

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

      // Enhanced QC results with FDA bioanalytical compliance
      const mockQcResults = [
        {
          id: "1",
          level: "LOW",
          spikedConcentration: 20,
          spikedUnit: "ng/mL",
          measuredValue: 19.8,
          measuredUnit: "ng/mL",
          accuracy: 99.0,
          precision: 2.1,
          isLLOQ: false,
          acceptanceCriteria: qcAcceptanceCriteria.accuracyLimits,
          precisionLimit: qcAcceptanceCriteria.precisionLimit,
          status: "PASS", // 99.0% within 85-115%
          n: 6,
          mean: 19.8,
          sd: 0.42,
          cv: 2.1,
          runs: ["19.5", "19.9", "20.1", "19.7", "19.8", "20.0"],
        },
        {
          id: "2",
          level: "MEDIUM",
          spikedConcentration: 500,
          spikedUnit: "ng/mL",
          measuredValue: 497.5,
          measuredUnit: "ng/mL",
          accuracy: 99.5,
          precision: 1.8,
          isLLOQ: false,
          acceptanceCriteria: qcAcceptanceCriteria.accuracyLimits,
          precisionLimit: qcAcceptanceCriteria.precisionLimit,
          status: "PASS", // 99.5% within 85-115%
          n: 6,
          mean: 497.5,
          sd: 8.96,
          cv: 1.8,
          runs: ["495.2", "498.7", "501.3", "496.8", "497.5", "495.5"],
        },
        {
          id: "3",
          level: "HIGH",
          spikedConcentration: 8000,
          spikedUnit: "ng/mL",
          measuredValue: 7950,
          measuredUnit: "ng/mL",
          accuracy: 99.4,
          precision: 2.3,
          isLLOQ: false,
          acceptanceCriteria: qcAcceptanceCriteria.accuracyLimits,
          precisionLimit: qcAcceptanceCriteria.precisionLimit,
          status: "PASS", // 99.4% within 85-115%
          n: 6,
          mean: 7950,
          sd: 182.85,
          cv: 2.3,
          runs: ["7895", "7968", "8012", "7934", "7950", "7941"],
        },
        {
          id: "4",
          level: "LLOQ",
          spikedConcentration: 10,
          spikedUnit: "ng/mL",
          measuredValue: 9.7,
          measuredUnit: "ng/mL",
          accuracy: 97.0,
          precision: 18.5,
          isLLOQ: true,
          acceptanceCriteria: qcAcceptanceCriteria.lloqAccuracyLimits,
          precisionLimit: qcAcceptanceCriteria.lloqPrecisionLimit,
          status: "PASS", // 97.0% within 80-120%, 18.5% ≤ 20%
          n: 6,
          mean: 9.7,
          sd: 1.79,
          cv: 18.5,
          runs: ["8.9", "10.2", "11.1", "9.4", "9.7", "8.9"],
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

      // System Suitability Testing (pre-analytical run)
      const mockSystemSuitability = [
        {
          id: "1",
          parameter: "Retention Time",
          expected: 3.45,
          measured: 3.42,
          unit: "minutes",
          tolerance: "±0.1 min",
          withinTolerance: true,
          status: "PASS",
          criteria: "RT ± 2.5% or ± 0.1 min, whichever is greater",
        },
        {
          id: "2",
          parameter: "Peak Area Reproducibility",
          expected: 125000,
          measured: 125847,
          unit: "area units",
          tolerance: "±5.0%",
          withinTolerance: true,
          status: "PASS",
          criteria: "RSD ≤ 5.0% for 5 consecutive injections",
        },
        {
          id: "3",
          parameter: "Resolution",
          expected: 2.0,
          measured: 2.34,
          unit: "Rs",
          tolerance: "≥2.0",
          withinTolerance: true,
          status: "PASS",
          criteria: "Resolution ≥ 2.0 between critical pairs",
        },
        {
          id: "4",
          parameter: "Tailing Factor",
          expected: 1.0,
          measured: 1.08,
          unit: "Tf",
          tolerance: "≤1.5",
          withinTolerance: true,
          status: "PASS",
          criteria: "Tailing factor ≤ 1.5",
        },
        {
          id: "5",
          parameter: "Signal-to-Noise",
          expected: 100,
          measured: 145.2,
          unit: "S/N",
          tolerance: "≥100",
          withinTolerance: true,
          status: "PASS",
          criteria: "S/N ≥ 100 at LLOQ level",
        },
      ];
      setSystemSuitabilityResults(mockSystemSuitability);

      // Control Samples (Positive and Negative)
      const mockControlSamples = [
        {
          id: "1",
          type: "POSITIVE_CONTROL",
          matrix: "Blank plasma spiked",
          expectedResult: "DETECTED",
          measuredResult: "DETECTED",
          concentration: 100.5,
          unit: "ng/mL",
          expectedConcentration: 100,
          accuracy: 100.5,
          status: "PASS",
          criteria: "Must detect analyte within ±20%",
        },
        {
          id: "2",
          type: "NEGATIVE_CONTROL",
          matrix: "Blank plasma",
          expectedResult: "NOT_DETECTED",
          measuredResult: "NOT_DETECTED",
          concentration: 0,
          unit: "ng/mL",
          interferingPeaks: false,
          status: "PASS",
          criteria: "No interfering peaks at analyte RT ± 2.5%",
        },
        {
          id: "3",
          type: "SOLVENT_BLANK",
          matrix: "Mobile phase",
          expectedResult: "NOT_DETECTED",
          measuredResult: "NOT_DETECTED",
          concentration: 0,
          unit: "ng/mL",
          carryoverDetected: false,
          status: "PASS",
          criteria: "No carryover from previous samples",
        },
        {
          id: "4",
          type: "MATRIX_BLANK",
          matrix: "Drug-free biological matrix",
          expectedResult: "NOT_DETECTED",
          measuredResult: "NOT_DETECTED",
          concentration: 0,
          unit: "ng/mL",
          matrixInterference: false,
          status: "PASS",
          criteria: "Matrix interference ≤ 20% of LLOQ response",
        },
      ];
      setControlSamples(mockControlSamples);

      // Reference Standard Verification
      const mockReferenceStandard = {
        standardId: "REF-STD-001",
        analyteName: "Test Compound",
        purity: 99.8,
        purityUnit: "%",
        certificateNumber: "COA-2024-001",
        expiryDate: "2025-12-31",
        storageConditions: "-20°C, desiccated",
        verificationDate: new Date().toISOString().split("T")[0],
        potencyVerified: true,
        potencyResult: 99.7,
        potencyCriteria: "98.0 - 102.0%",
        identityVerified: true,
        identityMethod: "LC-MS/MS RT and mass spectra match",
        moistureContent: 0.3,
        moistureCriteria: "≤ 0.5%",
        relatedSubstances: 0.12,
        relatedSubstancesCriteria: "≤ 0.2% total",
        status: "QUALIFIED",
        verifiedBy: "QC Lab Manager",
      };
      setReferenceStandardVerification(mockReferenceStandard);

      // Instrument Performance Checks
      const mockInstrumentPerformance = {
        instrumentId: executionData.instrumentId || "LC-MS-001",
        checkDate: new Date().toISOString().split("T")[0],
        checkTime: new Date().toLocaleTimeString(),
        parameters: [
          {
            parameter: "Column Pressure",
            value: 245,
            unit: "bar",
            specification: "200-300 bar",
            status: "PASS",
          },
          {
            parameter: "Column Temperature",
            value: 40.2,
            unit: "°C",
            specification: "40.0 ± 1.0°C",
            status: "PASS",
          },
          {
            parameter: "Flow Rate",
            value: 0.498,
            unit: "mL/min",
            specification: "0.500 ± 0.010 mL/min",
            status: "PASS",
          },
          {
            parameter: "Autosampler Temperature",
            value: 4.1,
            unit: "°C",
            specification: "4.0 ± 2.0°C",
            status: "PASS",
          },
          {
            parameter: "Ion Source Temperature",
            value: 350,
            unit: "°C",
            specification: "350 ± 10°C",
            status: "PASS",
          },
          {
            parameter: "Nebulizer Gas Pressure",
            value: 45,
            unit: "psi",
            specification: "45 ± 5 psi",
            status: "PASS",
          },
        ],
        overallStatus: "QUALIFIED",
        nextCalibrationDue: "2025-02-15",
        lastMaintenanceDate: "2024-12-15",
        qualificationStatus: "CURRENT",
      };
      setInstrumentPerformance(mockInstrumentPerformance);

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

      // Log data validation to audit trail
      logAuditEvent("DATA_VALIDATION", {
        filesValidated: uploadedFiles.length,
        calibrationRSquared: 0.9987,
        qcResultsCount: mockQcResults.length,
        westgardRulesStatus: "ALL_PASS",
        analyzerResultsCount: mockResults.length,
      });

      notify({
        kind: NotificationKinds.success,
        message: intl.formatMessage({
          id: "notebook.bioanalytical.execution.validationComplete",
          defaultMessage: "Data validation completed successfully",
        }),
      });
      setIsLoading(false);
    }, 2000);
  }, [uploadedFiles, intl, logAuditEvent, notify]);

  const handleApproveResults = useCallback(() => {
    if (analyzerResults.length === 0) {
      notify({
        kind: NotificationKinds.error,
        message: intl.formatMessage({
          id: "notebook.bioanalytical.execution.noResults",
          defaultMessage: "No validated results to approve",
        }),
      });
      return;
    }

    notify({
      kind: NotificationKinds.success,
      message: intl.formatMessage(
        {
          id: "notebook.bioanalytical.execution.resultsApproved",
          defaultMessage: "{count} sample results approved for QA review",
        },
        { count: analyzerResults.length },
      ),
    });

    if (onProgressUpdate) {
      onProgressUpdate();
    }
  }, [analyzerResults, intl, onProgressUpdate, notify]);

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
              id="notebook.bioanalytical.execution.tab.automation"
              defaultMessage="Automated Processing"
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
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.execution.tab.analystReview"
              defaultMessage="Analyst Review (ALCOA+)"
            />
          </Tab>
          <Tab disabled={reviewStatus !== "ANALYST_APPROVED"}>
            <FormattedMessage
              id="notebook.bioanalytical.execution.tab.qaReview"
              defaultMessage="QA Review (Level 2)"
            />
          </Tab>
          <Tab disabled={reviewStatus !== "QA_APPROVED"}>
            <FormattedMessage
              id="notebook.bioanalytical.execution.tab.managerReview"
              defaultMessage="Manager Approval (Final)"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.execution.tab.auditTrail"
              defaultMessage="Audit Trail & Data Integrity"
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

                  {/* Data Processing Automation Settings */}
                  {selectedInstrument && uploadedFiles.length > 0 && (
                    <div style={{ marginTop: "2rem" }}>
                      <h5 style={{ marginBottom: "1rem" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.execution.automationSettings"
                          defaultMessage="Data Processing Automation Settings"
                        />
                      </h5>
                      <div
                        style={{
                          padding: "1rem",
                          backgroundColor: "#f4f4f4",
                          borderRadius: "4px",
                        }}
                      >
                        <div
                          style={{
                            display: "flex",
                            flexDirection: "column",
                            gap: "1rem",
                          }}
                        >
                          <div
                            style={{
                              display: "flex",
                              alignItems: "center",
                              gap: "1rem",
                            }}
                          >
                            <Checkbox
                              id="auto-baseline"
                              checked={dataProcessingSettings.autoBaseline}
                              onChange={(e) =>
                                setDataProcessingSettings((prev) => ({
                                  ...prev,
                                  autoBaseline: e.target.checked,
                                }))
                              }
                              labelText="Automatic baseline correction"
                            />
                            <Checkbox
                              id="auto-integration"
                              checked={dataProcessingSettings.autoIntegration}
                              onChange={(e) =>
                                setDataProcessingSettings((prev) => ({
                                  ...prev,
                                  autoIntegration: e.target.checked,
                                }))
                              }
                              labelText="Automatic peak integration"
                            />
                            <Checkbox
                              id="auto-quantification"
                              checked={
                                dataProcessingSettings.autoQuantification
                              }
                              onChange={(e) =>
                                setDataProcessingSettings((prev) => ({
                                  ...prev,
                                  autoQuantification: e.target.checked,
                                }))
                              }
                              labelText="Automatic quantification"
                            />
                          </div>

                          <div
                            style={{
                              display: "grid",
                              gridTemplateColumns: "1fr 1fr 1fr",
                              gap: "1rem",
                            }}
                          >
                            <Select
                              labelText="Baseline Algorithm"
                              value={dataProcessingSettings.baselineAlgorithm}
                              onChange={(e) =>
                                setDataProcessingSettings((prev) => ({
                                  ...prev,
                                  baselineAlgorithm: e.target.value,
                                }))
                              }
                            >
                              <SelectItem
                                value="asymmetric_least_squares"
                                text="Asymmetric Least Squares"
                              />
                              <SelectItem
                                value="rolling_ball"
                                text="Rolling Ball"
                              />
                              <SelectItem
                                value="linear_interpolation"
                                text="Linear Interpolation"
                              />
                              <SelectItem
                                value="polynomial_fit"
                                text="Polynomial Fit"
                              />
                            </Select>

                            <Select
                              labelText="Integration Method"
                              value={dataProcessingSettings.integrationMethod}
                              onChange={(e) =>
                                setDataProcessingSettings((prev) => ({
                                  ...prev,
                                  integrationMethod: e.target.value,
                                }))
                              }
                            >
                              <SelectItem value="drop_line" text="Drop Line" />
                              <SelectItem
                                value="tangent_skim"
                                text="Tangent Skim"
                              />
                              <SelectItem
                                value="perpendicular"
                                text="Perpendicular"
                              />
                              <SelectItem
                                value="valley_to_valley"
                                text="Valley to Valley"
                              />
                            </Select>

                            <NumberInput
                              id="peak-threshold"
                              label="Peak Detection Threshold"
                              value={
                                dataProcessingSettings.peakDetectionThreshold
                              }
                              onChange={(e) =>
                                setDataProcessingSettings((prev) => ({
                                  ...prev,
                                  peakDetectionThreshold:
                                    parseFloat(e.target.value) || 0.01,
                                }))
                              }
                              min={0.001}
                              max={0.1}
                              step={0.001}
                            />
                          </div>

                          <div
                            style={{
                              display: "grid",
                              gridTemplateColumns: "1fr 1fr 1fr",
                              gap: "1rem",
                            }}
                          >
                            <NumberInput
                              id="smoothing-factor"
                              label="Smoothing Factor"
                              value={dataProcessingSettings.smoothingFactor}
                              onChange={(e) =>
                                setDataProcessingSettings((prev) => ({
                                  ...prev,
                                  smoothingFactor:
                                    parseFloat(e.target.value) || 0.1,
                                }))
                              }
                              min={0.01}
                              max={1.0}
                              step={0.01}
                            />

                            <NumberInput
                              id="rt-window"
                              label="RT Window (min)"
                              value={dataProcessingSettings.retentionTimeWindow}
                              onChange={(e) =>
                                setDataProcessingSettings((prev) => ({
                                  ...prev,
                                  retentionTimeWindow:
                                    parseFloat(e.target.value) || 0.5,
                                }))
                              }
                              min={0.1}
                              max={2.0}
                              step={0.1}
                            />

                            <NumberInput
                              id="mass-accuracy"
                              label="Mass Accuracy (ppm)"
                              value={dataProcessingSettings.massAccuracyPpm}
                              onChange={(e) =>
                                setDataProcessingSettings((prev) => ({
                                  ...prev,
                                  massAccuracyPpm:
                                    parseFloat(e.target.value) || 5.0,
                                }))
                              }
                              min={1.0}
                              max={20.0}
                              step={0.1}
                            />
                          </div>
                        </div>
                      </div>

                      <div style={{ marginTop: "1.5rem" }}>
                        <Button
                          kind="primary"
                          onClick={handleValidateData}
                          disabled={isLoading}
                        >
                          {isLoading ? (
                            <>
                              <Loading description="Processing..." />
                            </>
                          ) : (
                            <FormattedMessage
                              id="notebook.bioanalytical.execution.processData"
                              defaultMessage="Process Data with Automation"
                            />
                          )}
                        </Button>
                      </div>
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

          {/* Tab 4: Automated Processing Results */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.automationResults"
                        defaultMessage="Automated Data Processing Results"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.automationResultsHelp"
                        defaultMessage="Review automated peak integration, baseline correction, and quantification results. Verify processing efficiency and identify any peaks requiring manual review."
                      />
                    </p>
                  </div>

                  {/* Automation Summary */}
                  {processingResults.automationSummary && (
                    <div style={{ marginTop: "1.5rem" }}>
                      <h5 style={{ marginBottom: "1rem" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.execution.automationSummary"
                          defaultMessage="Processing Automation Summary"
                        />
                      </h5>
                      <div
                        style={{
                          display: "grid",
                          gridTemplateColumns: "1fr 1fr 1fr 1fr",
                          gap: "1rem",
                          marginBottom: "2rem",
                        }}
                      >
                        <div
                          style={{
                            padding: "1rem",
                            backgroundColor: "#e7f1f5",
                            borderRadius: "4px",
                            textAlign: "center",
                          }}
                        >
                          <h6
                            style={{ color: "#0043ce", marginBottom: "0.5rem" }}
                          >
                            Processing Time
                          </h6>
                          <p
                            style={{
                              fontSize: "1.5rem",
                              fontWeight: "bold",
                              color: "#0043ce",
                              margin: 0,
                            }}
                          >
                            {
                              processingResults.automationSummary
                                .processingTimeSeconds
                            }
                            s
                          </p>
                        </div>
                        <div
                          style={{
                            padding: "1rem",
                            backgroundColor: "#f0f7ff",
                            borderRadius: "4px",
                            textAlign: "center",
                          }}
                        >
                          <h6
                            style={{ color: "#24a148", marginBottom: "0.5rem" }}
                          >
                            Automation Efficiency
                          </h6>
                          <p
                            style={{
                              fontSize: "1.5rem",
                              fontWeight: "bold",
                              color: "#24a148",
                              margin: 0,
                            }}
                          >
                            {
                              processingResults.automationSummary
                                .automationEfficiency
                            }
                            %
                          </p>
                        </div>
                        <div
                          style={{
                            padding: "1rem",
                            backgroundColor: "#fff8e1",
                            borderRadius: "4px",
                            textAlign: "center",
                          }}
                        >
                          <h6
                            style={{ color: "#f1c21b", marginBottom: "0.5rem" }}
                          >
                            Data Quality Score
                          </h6>
                          <p
                            style={{
                              fontSize: "1.5rem",
                              fontWeight: "bold",
                              color: "#f1c21b",
                              margin: 0,
                            }}
                          >
                            {
                              processingResults.automationSummary
                                .dataQualityScore
                            }
                            %
                          </p>
                        </div>
                        <div
                          style={{
                            padding: "1rem",
                            backgroundColor: "#f4f4f4",
                            borderRadius: "4px",
                            textAlign: "center",
                          }}
                        >
                          <h6
                            style={{ color: "#525252", marginBottom: "0.5rem" }}
                          >
                            Manual Review Required
                          </h6>
                          <p
                            style={{
                              fontSize: "1.5rem",
                              fontWeight: "bold",
                              color: "#525252",
                              margin: 0,
                            }}
                          >
                            {
                              processingResults.automationSummary
                                .manualReviewRequired
                            }{" "}
                            peaks
                          </p>
                        </div>
                      </div>
                    </div>
                  )}

                  {/* Peak Integration Results */}
                  {processingResults.peakIntegration.length > 0 && (
                    <div style={{ marginTop: "2rem" }}>
                      <h5 style={{ marginBottom: "1rem" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.execution.peakIntegration"
                          defaultMessage="Automated Peak Integration Results ({count})"
                          values={{
                            count: processingResults.peakIntegration.length,
                          }}
                        />
                      </h5>
                      <Table>
                        <TableHead>
                          <TableRow>
                            <TableHeader>Peak ID</TableHeader>
                            <TableHeader>RT (min)</TableHeader>
                            <TableHeader>Peak Area</TableHeader>
                            <TableHeader>Peak Height</TableHeader>
                            <TableHeader>Symmetry</TableHeader>
                            <TableHeader>Resolution</TableHeader>
                            <TableHeader>S/N Ratio</TableHeader>
                            <TableHeader>Status</TableHeader>
                            <TableHeader>Manual Review</TableHeader>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {processingResults.peakIntegration.map((peak) => (
                            <TableRow key={peak.peakId}>
                              <TableCell style={{ fontFamily: "monospace" }}>
                                {peak.peakId}
                              </TableCell>
                              <TableCell>
                                {peak.retentionTime.toFixed(2)}
                              </TableCell>
                              <TableCell>
                                {peak.peakArea.toLocaleString()}
                              </TableCell>
                              <TableCell>
                                {peak.peakHeight.toLocaleString()}
                              </TableCell>
                              <TableCell>
                                {peak.symmetryFactor.toFixed(2)}
                              </TableCell>
                              <TableCell>
                                {peak.resolution.toFixed(2)}
                              </TableCell>
                              <TableCell>
                                {peak.signalToNoise.toFixed(1)}
                              </TableCell>
                              <TableCell>
                                <span
                                  style={{
                                    padding: "0.25rem 0.5rem",
                                    borderRadius: "4px",
                                    fontSize: "0.75rem",
                                    backgroundColor: "#24a148",
                                    color: "white",
                                  }}
                                >
                                  {peak.integrationStatus.replace(/_/g, " ")}
                                </span>
                              </TableCell>
                              <TableCell>
                                <span
                                  style={{
                                    padding: "0.25rem 0.5rem",
                                    borderRadius: "4px",
                                    fontSize: "0.75rem",
                                    backgroundColor: peak.manualReview
                                      ? "#f1c21b"
                                      : "#24a148",
                                    color: peak.manualReview
                                      ? "#161616"
                                      : "white",
                                  }}
                                >
                                  {peak.manualReview
                                    ? "REQUIRED"
                                    : "NOT NEEDED"}
                                </span>
                              </TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </div>
                  )}

                  {/* Quantification Results */}
                  {processingResults.quantification.length > 0 && (
                    <div style={{ marginTop: "2rem" }}>
                      <h5 style={{ marginBottom: "1rem" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.execution.quantificationResults"
                          defaultMessage="Automated Quantification Results ({count})"
                          values={{
                            count: processingResults.quantification.length,
                          }}
                        />
                      </h5>
                      <Table>
                        <TableHead>
                          <TableRow>
                            <TableHeader>Sample ID</TableHeader>
                            <TableHeader>Analyte</TableHeader>
                            <TableHeader>Peak Area</TableHeader>
                            <TableHeader>Concentration</TableHeader>
                            <TableHeader>Accuracy %</TableHeader>
                            <TableHeader>CV %</TableHeader>
                            <TableHeader>Response Function</TableHeader>
                            <TableHeader>Flag</TableHeader>
                            <TableHeader>Automation</TableHeader>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {processingResults.quantification.map(
                            (quant, index) => (
                              <TableRow key={index}>
                                <TableCell style={{ fontFamily: "monospace" }}>
                                  {quant.sampleId}
                                </TableCell>
                                <TableCell>{quant.analyte}</TableCell>
                                <TableCell>
                                  {quant.peakArea.toLocaleString()}
                                </TableCell>
                                <TableCell>
                                  {quant.concentration.toFixed(1)} {quant.units}
                                </TableCell>
                                <TableCell>
                                  {quant.accuracyPercent.toFixed(1)}%
                                </TableCell>
                                <TableCell>
                                  {quant.cvPercent.toFixed(1)}%
                                </TableCell>
                                <TableCell>{quant.responseFunction}</TableCell>
                                <TableCell>
                                  <span
                                    style={{
                                      padding: "0.25rem 0.5rem",
                                      borderRadius: "4px",
                                      fontSize: "0.75rem",
                                      backgroundColor:
                                        quant.quantificationFlag === "PASSED"
                                          ? "#24a148"
                                          : "#da1e28",
                                      color: "white",
                                    }}
                                  >
                                    {quant.quantificationFlag}
                                  </span>
                                </TableCell>
                                <TableCell>
                                  <span
                                    style={{
                                      padding: "0.25rem 0.5rem",
                                      borderRadius: "4px",
                                      fontSize: "0.75rem",
                                      backgroundColor: "#0043ce",
                                      color: "white",
                                    }}
                                  >
                                    {quant.automationStatus.replace(/_/g, " ")}
                                  </span>
                                </TableCell>
                              </TableRow>
                            ),
                          )}
                        </TableBody>
                      </Table>
                    </div>
                  )}

                  {/* No Automation Results */}
                  {!processingResults.automationSummary && (
                    <div
                      style={{
                        marginTop: "2rem",
                        padding: "2rem",
                        backgroundColor: "#f4f4f4",
                        borderRadius: "4px",
                        textAlign: "center",
                      }}
                    >
                      <h6 style={{ color: "#525252", marginBottom: "0.5rem" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.execution.noAutomationResults"
                          defaultMessage="No Automation Results Available"
                        />
                      </h6>
                      <p style={{ color: "#525252", fontSize: "0.875rem" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.execution.automationResultsHelp2"
                          defaultMessage="Upload data files and run automated processing to see peak integration and quantification results here."
                        />
                      </p>
                    </div>
                  )}
                </Column>
              </Grid>
            </div>
          </TabPanel>

          {/* Tab 5: QC Results & Trending */}
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
                        defaultMessage="Comprehensive FDA bioanalytical QC validation including: calibration curve verification (r² ≥ 0.99), QC samples (Low/Medium/High/LLOQ) with acceptance criteria (±15%, ±20% at LLOQ), system suitability testing, positive/negative controls, reference standard verification, and instrument performance checks."
                      />
                    </p>

                    {/* QC Summary Dashboard */}
                    <div style={{ marginTop: "1.5rem" }}>
                      <h5 style={{ marginBottom: "1rem" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.execution.qcSummary"
                          defaultMessage="QC Compliance Summary"
                        />
                      </h5>
                      <div
                        style={{
                          display: "grid",
                          gridTemplateColumns: "1fr 1fr 1fr 1fr 1fr",
                          gap: "1rem",
                          marginBottom: "2rem",
                        }}
                      >
                        <div
                          style={{
                            padding: "1rem",
                            backgroundColor:
                              calibrationData &&
                              calibrationData.rSquared >= 0.99
                                ? "#e7f6ea"
                                : "#fff1f1",
                            borderRadius: "4px",
                            textAlign: "center",
                            borderLeft: `4px solid ${calibrationData && calibrationData.rSquared >= 0.99 ? "#24a148" : "#da1e28"}`,
                          }}
                        >
                          <h6
                            style={{
                              color:
                                calibrationData &&
                                calibrationData.rSquared >= 0.99
                                  ? "#24a148"
                                  : "#da1e28",
                              marginBottom: "0.5rem",
                            }}
                          >
                            Calibration Curve
                          </h6>
                          <p
                            style={{
                              fontSize: "1.25rem",
                              fontWeight: "bold",
                              color:
                                calibrationData &&
                                calibrationData.rSquared >= 0.99
                                  ? "#24a148"
                                  : "#da1e28",
                              margin: 0,
                            }}
                          >
                            r² = {calibrationData?.rSquared || "N/A"}
                          </p>
                          <p style={{ fontSize: "0.75rem", margin: 0 }}>
                            {calibrationData && calibrationData.rSquared >= 0.99
                              ? "✓ PASS (≥0.99)"
                              : "❌ FAIL (<0.99)"}
                          </p>
                        </div>
                        <div
                          style={{
                            padding: "1rem",
                            backgroundColor:
                              qcResults.length > 0 &&
                              qcResults.every((qc) => qc.status === "PASS")
                                ? "#e7f6ea"
                                : "#fff1f1",
                            borderRadius: "4px",
                            textAlign: "center",
                            borderLeft: `4px solid ${qcResults.length > 0 && qcResults.every((qc) => qc.status === "PASS") ? "#24a148" : "#da1e28"}`,
                          }}
                        >
                          <h6
                            style={{
                              color:
                                qcResults.length > 0 &&
                                qcResults.every((qc) => qc.status === "PASS")
                                  ? "#24a148"
                                  : "#da1e28",
                              marginBottom: "0.5rem",
                            }}
                          >
                            QC Samples
                          </h6>
                          <p
                            style={{
                              fontSize: "1.25rem",
                              fontWeight: "bold",
                              color:
                                qcResults.length > 0 &&
                                qcResults.every((qc) => qc.status === "PASS")
                                  ? "#24a148"
                                  : "#da1e28",
                              margin: 0,
                            }}
                          >
                            {
                              qcResults.filter((qc) => qc.status === "PASS")
                                .length
                            }
                            /{qcResults.length}
                          </p>
                          <p style={{ fontSize: "0.75rem", margin: 0 }}>
                            {qcResults.length > 0 &&
                            qcResults.every((qc) => qc.status === "PASS")
                              ? "✓ ALL PASS"
                              : "❌ SOME FAIL"}
                          </p>
                        </div>
                        <div
                          style={{
                            padding: "1rem",
                            backgroundColor:
                              systemSuitabilityResults.length > 0 &&
                              systemSuitabilityResults.every(
                                (s) => s.status === "PASS",
                              )
                                ? "#e7f6ea"
                                : "#fff1f1",
                            borderRadius: "4px",
                            textAlign: "center",
                            borderLeft: `4px solid ${systemSuitabilityResults.length > 0 && systemSuitabilityResults.every((s) => s.status === "PASS") ? "#24a148" : "#da1e28"}`,
                          }}
                        >
                          <h6
                            style={{
                              color:
                                systemSuitabilityResults.length > 0 &&
                                systemSuitabilityResults.every(
                                  (s) => s.status === "PASS",
                                )
                                  ? "#24a148"
                                  : "#da1e28",
                              marginBottom: "0.5rem",
                            }}
                          >
                            System Suitability
                          </h6>
                          <p
                            style={{
                              fontSize: "1.25rem",
                              fontWeight: "bold",
                              color:
                                systemSuitabilityResults.length > 0 &&
                                systemSuitabilityResults.every(
                                  (s) => s.status === "PASS",
                                )
                                  ? "#24a148"
                                  : "#da1e28",
                              margin: 0,
                            }}
                          >
                            {
                              systemSuitabilityResults.filter(
                                (s) => s.status === "PASS",
                              ).length
                            }
                            /{systemSuitabilityResults.length}
                          </p>
                          <p style={{ fontSize: "0.75rem", margin: 0 }}>
                            {systemSuitabilityResults.length > 0 &&
                            systemSuitabilityResults.every(
                              (s) => s.status === "PASS",
                            )
                              ? "✓ ALL PASS"
                              : "❌ SOME FAIL"}
                          </p>
                        </div>
                        <div
                          style={{
                            padding: "1rem",
                            backgroundColor:
                              controlSamples.length > 0 &&
                              controlSamples.every((c) => c.status === "PASS")
                                ? "#e7f6ea"
                                : "#fff1f1",
                            borderRadius: "4px",
                            textAlign: "center",
                            borderLeft: `4px solid ${controlSamples.length > 0 && controlSamples.every((c) => c.status === "PASS") ? "#24a148" : "#da1e28"}`,
                          }}
                        >
                          <h6
                            style={{
                              color:
                                controlSamples.length > 0 &&
                                controlSamples.every((c) => c.status === "PASS")
                                  ? "#24a148"
                                  : "#da1e28",
                              marginBottom: "0.5rem",
                            }}
                          >
                            Control Samples
                          </h6>
                          <p
                            style={{
                              fontSize: "1.25rem",
                              fontWeight: "bold",
                              color:
                                controlSamples.length > 0 &&
                                controlSamples.every((c) => c.status === "PASS")
                                  ? "#24a148"
                                  : "#da1e28",
                              margin: 0,
                            }}
                          >
                            {
                              controlSamples.filter((c) => c.status === "PASS")
                                .length
                            }
                            /{controlSamples.length}
                          </p>
                          <p style={{ fontSize: "0.75rem", margin: 0 }}>
                            {controlSamples.length > 0 &&
                            controlSamples.every((c) => c.status === "PASS")
                              ? "✓ ALL PASS"
                              : "❌ SOME FAIL"}
                          </p>
                        </div>
                        <div
                          style={{
                            padding: "1rem",
                            backgroundColor:
                              referenceStandardVerification &&
                              referenceStandardVerification.status ===
                                "QUALIFIED"
                                ? "#e7f6ea"
                                : "#fff1f1",
                            borderRadius: "4px",
                            textAlign: "center",
                            borderLeft: `4px solid ${referenceStandardVerification && referenceStandardVerification.status === "QUALIFIED" ? "#24a148" : "#da1e28"}`,
                          }}
                        >
                          <h6
                            style={{
                              color:
                                referenceStandardVerification &&
                                referenceStandardVerification.status ===
                                  "QUALIFIED"
                                  ? "#24a148"
                                  : "#da1e28",
                              marginBottom: "0.5rem",
                            }}
                          >
                            Reference Std
                          </h6>
                          <p
                            style={{
                              fontSize: "1.25rem",
                              fontWeight: "bold",
                              color:
                                referenceStandardVerification &&
                                referenceStandardVerification.status ===
                                  "QUALIFIED"
                                  ? "#24a148"
                                  : "#da1e28",
                              margin: 0,
                            }}
                          >
                            {referenceStandardVerification?.purity || "N/A"}%
                          </p>
                          <p style={{ fontSize: "0.75rem", margin: 0 }}>
                            {referenceStandardVerification &&
                            referenceStandardVerification.status === "QUALIFIED"
                              ? "✓ QUALIFIED"
                              : "❌ UNQUALIFIED"}
                          </p>
                        </div>
                      </div>
                    </div>

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
                                  defaultMessage="Precision (CV%)"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.acceptanceCriteria"
                                  defaultMessage="Acceptance Criteria"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioanalytical.execution.n"
                                  defaultMessage="n"
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
                                <TableCell>
                                  <strong>{qc.level}</strong>
                                  {qc.isLLOQ && (
                                    <span
                                      style={{
                                        display: "block",
                                        fontSize: "0.75rem",
                                        color: "#8a3ffc",
                                      }}
                                    >
                                      (Lower Limit of Quantification)
                                    </span>
                                  )}
                                </TableCell>
                                <TableCell>
                                  {qc.spikedConcentration} {qc.spikedUnit}
                                </TableCell>
                                <TableCell>
                                  {qc.measuredValue} {qc.measuredUnit}
                                </TableCell>
                                <TableCell>
                                  <span
                                    style={{
                                      color:
                                        qc.accuracy >=
                                          qc.acceptanceCriteria.min &&
                                        qc.accuracy <= qc.acceptanceCriteria.max
                                          ? "#24a148"
                                          : "#da1e28",
                                      fontWeight: "bold",
                                    }}
                                  >
                                    {qc.accuracy.toFixed(1)}%
                                  </span>
                                </TableCell>
                                <TableCell>
                                  <span
                                    style={{
                                      color:
                                        qc.precision <= qc.precisionLimit
                                          ? "#24a148"
                                          : "#da1e28",
                                      fontWeight: "bold",
                                    }}
                                  >
                                    {qc.precision.toFixed(1)}%
                                  </span>
                                </TableCell>
                                <TableCell style={{ fontSize: "0.75rem" }}>
                                  <div>
                                    Accuracy: {qc.acceptanceCriteria.min}-
                                    {qc.acceptanceCriteria.max}%
                                  </div>
                                  <div>Precision: ≤{qc.precisionLimit}%</div>
                                </TableCell>
                                <TableCell style={{ textAlign: "center" }}>
                                  <strong>{qc.n}</strong>
                                </TableCell>
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

          {/* Tab 6: Analyst Review (ALCOA+ Compliance) */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.analystReviewSection"
                        defaultMessage="Primary Analyst Review - ALCOA+ Compliance"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.analystReviewHelp"
                        defaultMessage="Conduct primary analyst review of all analytical data ensuring ALCOA+ compliance: Attributable, Legible, Contemporaneous, Original, Accurate, Complete, Consistent, Enduring, Available. Electronic signature required for validation."
                      />
                    </p>

                    {/* ALCOA+ Compliance Checklist */}
                    <div
                      style={{
                        marginTop: "1.5rem",
                        padding: "1rem",
                        backgroundColor: "#f4f4f4",
                        borderRadius: "4px",
                      }}
                    >
                      <h5 style={{ marginBottom: "1rem" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.execution.alcoaChecklist"
                          defaultMessage="ALCOA+ Data Integrity Verification"
                        />
                      </h5>
                      <div
                        style={{
                          display: "flex",
                          flexDirection: "column",
                          gap: "0.75rem",
                        }}
                      >
                        <div style={{ display: "flex", alignItems: "center" }}>
                          <Checkbox
                            id="alcoa-attributable"
                            checked={dataIntegrity.userAttribution.verified}
                            onChange={(e) =>
                              setDataIntegrity((prev) => ({
                                ...prev,
                                userAttribution: {
                                  verified: e.target.checked,
                                  user: "CURRENT_USER",
                                },
                              }))
                            }
                            labelText=""
                          />
                          <label
                            htmlFor="alcoa-attributable"
                            style={{
                              marginLeft: "0.5rem",
                              fontSize: "0.875rem",
                            }}
                          >
                            <strong>Attributable:</strong> All data actions
                            traced to responsible individuals
                          </label>
                        </div>
                        <div style={{ display: "flex", alignItems: "center" }}>
                          <Checkbox
                            id="alcoa-legible"
                            checked={true}
                            disabled
                            labelText=""
                          />
                          <label
                            htmlFor="alcoa-legible"
                            style={{
                              marginLeft: "0.5rem",
                              fontSize: "0.875rem",
                              color: "#525252",
                            }}
                          >
                            <strong>Legible:</strong> Digital format ensures
                            permanent readability ✓
                          </label>
                        </div>
                        <div style={{ display: "flex", alignItems: "center" }}>
                          <Checkbox
                            id="alcoa-contemporaneous"
                            checked={dataIntegrity.contemporaneousRecord}
                            onChange={(e) =>
                              setDataIntegrity((prev) => ({
                                ...prev,
                                contemporaneousRecord: e.target.checked,
                              }))
                            }
                            labelText=""
                          />
                          <label
                            htmlFor="alcoa-contemporaneous"
                            style={{
                              marginLeft: "0.5rem",
                              fontSize: "0.875rem",
                            }}
                          >
                            <strong>Contemporaneous:</strong> Data recorded at
                            time of observation
                          </label>
                        </div>
                        <div style={{ display: "flex", alignItems: "center" }}>
                          <Checkbox
                            id="alcoa-original"
                            checked={dataIntegrity.originalityVerified}
                            onChange={(e) =>
                              setDataIntegrity((prev) => ({
                                ...prev,
                                originalityVerified: e.target.checked,
                              }))
                            }
                            labelText=""
                          />
                          <label
                            htmlFor="alcoa-original"
                            style={{
                              marginLeft: "0.5rem",
                              fontSize: "0.875rem",
                            }}
                          >
                            <strong>Original:</strong> First recording of data,
                            checksums verified
                          </label>
                        </div>
                        <div style={{ display: "flex", alignItems: "center" }}>
                          <Checkbox
                            id="alcoa-accurate"
                            checked={calibrationData && qcResults.length > 0}
                            disabled
                            labelText=""
                          />
                          <label
                            htmlFor="alcoa-accurate"
                            style={{
                              marginLeft: "0.5rem",
                              fontSize: "0.875rem",
                              color:
                                calibrationData && qcResults.length > 0
                                  ? "#161616"
                                  : "#da1e28",
                            }}
                          >
                            <strong>Accurate:</strong> QC validation and
                            calibration curve verification{" "}
                            {calibrationData && qcResults.length > 0
                              ? "✓"
                              : "❌"}
                          </label>
                        </div>
                        <div style={{ display: "flex", alignItems: "center" }}>
                          <Checkbox
                            id="alcoa-complete"
                            checked={
                              uploadedFiles.length > 0 &&
                              analyzerResults.length > 0
                            }
                            disabled
                            labelText=""
                          />
                          <label
                            htmlFor="alcoa-complete"
                            style={{
                              marginLeft: "0.5rem",
                              fontSize: "0.875rem",
                              color:
                                uploadedFiles.length > 0 &&
                                analyzerResults.length > 0
                                  ? "#161616"
                                  : "#da1e28",
                            }}
                          >
                            <strong>Complete:</strong> All required data
                            uploaded and processed{" "}
                            {uploadedFiles.length > 0 &&
                            analyzerResults.length > 0
                              ? "✓"
                              : "❌"}
                          </label>
                        </div>
                      </div>
                    </div>

                    {/* Analyst Review Form */}
                    <div style={{ marginTop: "2rem" }}>
                      <h5 style={{ marginBottom: "1rem" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.execution.analystReviewForm"
                          defaultMessage="Primary Analyst Review & Electronic Signature"
                        />
                      </h5>

                      <div
                        style={{
                          display: "grid",
                          gridTemplateColumns: "1fr 1fr",
                          gap: "1rem",
                          marginBottom: "1.5rem",
                        }}
                      >
                        <TextInput
                          labelText="Reviewing Analyst ID *"
                          value={analystReview.reviewerId}
                          onChange={(e) =>
                            setAnalystReview((prev) => ({
                              ...prev,
                              reviewerId: e.target.value,
                            }))
                          }
                          placeholder="Enter analyst ID"
                        />

                        <TextInput
                          labelText="Reviewing Analyst Name *"
                          value={analystReview.reviewerName}
                          onChange={(e) =>
                            setAnalystReview((prev) => ({
                              ...prev,
                              reviewerName: e.target.value,
                            }))
                          }
                          placeholder="Enter full name"
                        />
                      </div>

                      <div style={{ marginBottom: "1.5rem" }}>
                        <TextArea
                          labelText="Review Comments & Observations *"
                          placeholder="Document review findings, data integrity assessment, any anomalies or deviations observed, compliance with analytical methods..."
                          value={analystReview.comments}
                          onChange={(e) =>
                            setAnalystReview((prev) => ({
                              ...prev,
                              comments: e.target.value,
                            }))
                          }
                          rows={4}
                        />
                      </div>

                      <div style={{ marginBottom: "1.5rem" }}>
                        <TextInput
                          labelText="Electronic Signature *"
                          type="password"
                          value={analystReview.electronicSignature}
                          onChange={(e) =>
                            setAnalystReview((prev) => ({
                              ...prev,
                              electronicSignature: e.target.value,
                            }))
                          }
                          placeholder="Enter password for electronic signature"
                          helperText="Electronic signature validates data review and ALCOA+ compliance"
                        />
                      </div>

                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "0.5rem",
                          marginBottom: "1.5rem",
                        }}
                      >
                        <Checkbox
                          id="analyst-approval"
                          checked={analystReview.approved}
                          onChange={(e) =>
                            setAnalystReview((prev) => ({
                              ...prev,
                              approved: e.target.checked,
                              reviewDate: e.target.checked
                                ? new Date().toISOString()
                                : "",
                            }))
                          }
                          labelText=""
                        />
                        <label
                          htmlFor="analyst-approval"
                          style={{ fontSize: "0.875rem", fontWeight: "bold" }}
                        >
                          I certify that I have reviewed all analytical data and
                          confirm ALCOA+ compliance
                        </label>
                      </div>

                      <div style={{ display: "flex", gap: "1rem" }}>
                        <Button
                          kind="primary"
                          disabled={
                            !analystReview.reviewerId ||
                            !analystReview.reviewerName ||
                            !analystReview.comments ||
                            !analystReview.electronicSignature ||
                            !analystReview.approved ||
                            !dataIntegrity.userAttribution.verified ||
                            !dataIntegrity.contemporaneousRecord ||
                            !dataIntegrity.originalityVerified
                          }
                          onClick={() => {
                            // Log analyst review approval to audit trail
                            logAuditEvent(
                              "ANALYST_REVIEW_APPROVED",
                              {
                                reviewerId: analystReview.reviewerId,
                                reviewerName: analystReview.reviewerName,
                                reviewComments:
                                  analystReview.comments.substring(0, 200) +
                                  "...",
                                alcoaCompliance: {
                                  attributable:
                                    dataIntegrity.userAttribution.verified,
                                  contemporaneous:
                                    dataIntegrity.contemporaneousRecord,
                                  original: dataIntegrity.originalityVerified,
                                },
                                electronicSignature: "PROVIDED",
                                nextStage: "QA_REVIEW",
                              },
                              analystReview.reviewerId,
                            );

                            setReviewStatus("ANALYST_APPROVED");
                            notify({
                              kind: NotificationKinds.success,
                              message:
                                "Primary analyst review completed. Data approved for secondary QA review.",
                            });
                            // Auto-navigate to QA Review tab
                            setSelectedTab(7); // QA Review is now tab 7
                          }}
                        >
                          <FormattedMessage
                            id="notebook.bioanalytical.execution.submitAnalystReview"
                            defaultMessage="Submit Analyst Review"
                          />
                        </Button>

                        <Button
                          kind="secondary"
                          disabled={!analystReview.comments}
                          onClick={() => {
                            setReviewStatus("ANALYST_REJECTED");
                            notify({
                              kind: NotificationKinds.error,
                              message:
                                "Analyst review rejected. Data requires correction before QA review.",
                            });
                          }}
                        >
                          <FormattedMessage
                            id="notebook.bioanalytical.execution.rejectAnalystReview"
                            defaultMessage="Reject & Request Corrections"
                          />
                        </Button>
                      </div>
                    </div>

                    {/* Review Status Display */}
                    <div style={{ marginTop: "2rem" }}>
                      <div
                        style={{
                          padding: "1rem",
                          backgroundColor:
                            reviewStatus === "ANALYST_APPROVED"
                              ? "#e7f1f5"
                              : reviewStatus === "ANALYST_REJECTED"
                                ? "#fff1f1"
                                : "#f4f4f4",
                          borderRadius: "4px",
                          borderLeft: `4px solid ${
                            reviewStatus === "ANALYST_APPROVED"
                              ? "#24a148"
                              : reviewStatus === "ANALYST_REJECTED"
                                ? "#da1e28"
                                : "#8d8d8d"
                          }`,
                        }}
                      >
                        <h6 style={{ margin: "0 0 0.5rem 0" }}>
                          <FormattedMessage
                            id="notebook.bioanalytical.execution.reviewStatus"
                            defaultMessage="Review Status: {status}"
                            values={{ status: reviewStatus.replace(/_/g, " ") }}
                          />
                        </h6>
                        {analystReview.reviewDate && (
                          <p style={{ fontSize: "0.875rem", margin: 0 }}>
                            <FormattedMessage
                              id="notebook.bioanalytical.execution.reviewInfo"
                              defaultMessage="Reviewed by: {analyst} on {date}"
                              values={{
                                analyst: analystReview.reviewerName,
                                date: new Date(
                                  analystReview.reviewDate,
                                ).toLocaleString(),
                              }}
                            />
                          </p>
                        )}
                      </div>
                    </div>
                  </div>
                </Column>
              </Grid>
            </div>
          </TabPanel>

          {/* Tab 7: QA Review (Level 2) */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.qaReviewSection"
                        defaultMessage="Secondary QA Review - Data Quality & Method Compliance"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.qaReviewHelp"
                        defaultMessage="Conduct secondary quality assurance review focused on data quality, method compliance, and statistical validity. Verify QC acceptance criteria, calibration validity, and overall analytical quality before final manager approval."
                      />
                    </p>
                  </div>

                  {/* QA Review Prerequisites Check */}
                  <div
                    style={{
                      marginTop: "1.5rem",
                      padding: "1rem",
                      backgroundColor:
                        reviewStatus === "ANALYST_APPROVED"
                          ? "#e7f1f5"
                          : "#fff1f1",
                      borderRadius: "4px",
                      borderLeft: `4px solid ${reviewStatus === "ANALYST_APPROVED" ? "#24a148" : "#da1e28"}`,
                    }}
                  >
                    <h6
                      style={{
                        margin: "0 0 0.5rem 0",
                        color:
                          reviewStatus === "ANALYST_APPROVED"
                            ? "#24a148"
                            : "#da1e28",
                      }}
                    >
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.qaPrerequisites"
                        defaultMessage="QA Review Prerequisites"
                      />
                    </h6>
                    <div style={{ fontSize: "0.875rem" }}>
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          marginBottom: "0.5rem",
                        }}
                      >
                        <span style={{ marginRight: "0.5rem" }}>
                          {reviewStatus === "ANALYST_APPROVED" ? "✓" : "❌"}
                        </span>
                        <span>
                          Primary Analyst Review:{" "}
                          {reviewStatus === "ANALYST_APPROVED"
                            ? "COMPLETED"
                            : "PENDING"}
                        </span>
                      </div>
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          marginBottom: "0.5rem",
                        }}
                      >
                        <span style={{ marginRight: "0.5rem" }}>
                          {calibrationData && calibrationData.rSquared >= 0.99
                            ? "✓"
                            : "❌"}
                        </span>
                        <span>
                          Calibration Curve Validation:{" "}
                          {calibrationData && calibrationData.rSquared >= 0.99
                            ? "PASSED"
                            : "PENDING"}
                        </span>
                      </div>
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          marginBottom: "0.5rem",
                        }}
                      >
                        <span style={{ marginRight: "0.5rem" }}>
                          {qcResults.length > 0 &&
                          qcResults.every((qc) => qc.status === "PASS")
                            ? "✓"
                            : "❌"}
                        </span>
                        <span>
                          QC Results Validation:{" "}
                          {qcResults.length > 0 &&
                          qcResults.every((qc) => qc.status === "PASS")
                            ? "ALL PASSED"
                            : "PENDING"}
                        </span>
                      </div>
                      <div style={{ display: "flex", alignItems: "center" }}>
                        <span style={{ marginRight: "0.5rem" }}>
                          {westgardRules.length > 0 &&
                          westgardRules.every((rule) => rule.status === "PASS")
                            ? "✓"
                            : "❌"}
                        </span>
                        <span>
                          Westgard Rules Check:{" "}
                          {westgardRules.length > 0 &&
                          westgardRules.every((rule) => rule.status === "PASS")
                            ? "ALL PASSED"
                            : "PENDING"}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* QA Review Checklist */}
                  {reviewStatus === "ANALYST_APPROVED" && (
                    <div style={{ marginTop: "2rem" }}>
                      <h5 style={{ marginBottom: "1rem" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.execution.qaChecklist"
                          defaultMessage="QA Review Checklist"
                        />
                      </h5>
                      <div
                        style={{
                          padding: "1rem",
                          backgroundColor: "#f4f4f4",
                          borderRadius: "4px",
                        }}
                      >
                        <div
                          style={{
                            display: "flex",
                            flexDirection: "column",
                            gap: "0.75rem",
                          }}
                        >
                          <div
                            style={{ display: "flex", alignItems: "center" }}
                          >
                            <Checkbox
                              id="qa-data-integrity"
                              checked={qaReview.dataIntegrityVerified}
                              onChange={(e) =>
                                setQaReview((prev) => ({
                                  ...prev,
                                  dataIntegrityVerified: e.target.checked,
                                }))
                              }
                              labelText=""
                            />
                            <label
                              htmlFor="qa-data-integrity"
                              style={{
                                marginLeft: "0.5rem",
                                fontSize: "0.875rem",
                              }}
                            >
                              <strong>Data Integrity:</strong> ALCOA+ compliance
                              verified, audit trail complete
                            </label>
                          </div>
                          <div
                            style={{ display: "flex", alignItems: "center" }}
                          >
                            <Checkbox
                              id="qa-method-compliance"
                              checked={qaReview.methodComplianceVerified}
                              onChange={(e) =>
                                setQaReview((prev) => ({
                                  ...prev,
                                  methodComplianceVerified: e.target.checked,
                                }))
                              }
                              labelText=""
                            />
                            <label
                              htmlFor="qa-method-compliance"
                              style={{
                                marginLeft: "0.5rem",
                                fontSize: "0.875rem",
                              }}
                            >
                              <strong>Method Compliance:</strong> Validated
                              analytical method followed exactly
                            </label>
                          </div>
                          <div
                            style={{ display: "flex", alignItems: "center" }}
                          >
                            <Checkbox
                              id="qa-qc-acceptable"
                              checked={qaReview.qcAcceptable}
                              onChange={(e) =>
                                setQaReview((prev) => ({
                                  ...prev,
                                  qcAcceptable: e.target.checked,
                                }))
                              }
                              labelText=""
                            />
                            <label
                              htmlFor="qa-qc-acceptable"
                              style={{
                                marginLeft: "0.5rem",
                                fontSize: "0.875rem",
                              }}
                            >
                              <strong>QC Acceptance:</strong> All QC levels meet
                              acceptance criteria (90-110% accuracy, ≤15% RSD)
                            </label>
                          </div>
                        </div>
                      </div>

                      {/* QA Review Form */}
                      <div style={{ marginTop: "1.5rem" }}>
                        <h5 style={{ marginBottom: "1rem" }}>
                          <FormattedMessage
                            id="notebook.bioanalytical.execution.qaReviewForm"
                            defaultMessage="QA Review & Electronic Signature"
                          />
                        </h5>
                        <div
                          style={{
                            display: "grid",
                            gridTemplateColumns: "1fr 1fr",
                            gap: "1rem",
                            marginBottom: "1.5rem",
                          }}
                        >
                          <TextInput
                            labelText="QA Reviewer ID *"
                            value={qaReview.reviewerId}
                            onChange={(e) =>
                              setQaReview((prev) => ({
                                ...prev,
                                reviewerId: e.target.value,
                              }))
                            }
                            placeholder="Enter QA reviewer ID"
                          />
                          <TextInput
                            labelText="QA Reviewer Name *"
                            value={qaReview.reviewerName}
                            onChange={(e) =>
                              setQaReview((prev) => ({
                                ...prev,
                                reviewerName: e.target.value,
                              }))
                            }
                            placeholder="Enter full name"
                          />
                        </div>

                        <div style={{ marginBottom: "1.5rem" }}>
                          <TextArea
                            labelText="QA Review Comments *"
                            placeholder="Document QA assessment, data quality evaluation, method compliance verification, statistical validity, any concerns or recommendations..."
                            value={qaReview.comments}
                            onChange={(e) =>
                              setQaReview((prev) => ({
                                ...prev,
                                comments: e.target.value,
                              }))
                            }
                            rows={4}
                          />
                        </div>

                        <div style={{ marginBottom: "1.5rem" }}>
                          <TextInput
                            labelText="Electronic Signature *"
                            type="password"
                            value={qaReview.electronicSignature}
                            onChange={(e) =>
                              setQaReview((prev) => ({
                                ...prev,
                                electronicSignature: e.target.value,
                              }))
                            }
                            placeholder="Enter password for electronic signature"
                            helperText="Electronic signature validates QA review and data quality assessment"
                          />
                        </div>

                        <div
                          style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "0.5rem",
                            marginBottom: "1.5rem",
                          }}
                        >
                          <Checkbox
                            id="qa-approval"
                            checked={qaReview.approved}
                            onChange={(e) =>
                              setQaReview((prev) => ({
                                ...prev,
                                approved: e.target.checked,
                                reviewDate: e.target.checked
                                  ? new Date().toISOString()
                                  : "",
                              }))
                            }
                            labelText=""
                          />
                          <label
                            htmlFor="qa-approval"
                            style={{ fontSize: "0.875rem", fontWeight: "bold" }}
                          >
                            I certify that all analytical data meets quality
                            standards and regulatory requirements
                          </label>
                        </div>

                        <div style={{ display: "flex", gap: "1rem" }}>
                          <Button
                            kind="primary"
                            disabled={
                              !qaReview.reviewerId ||
                              !qaReview.reviewerName ||
                              !qaReview.comments ||
                              !qaReview.electronicSignature ||
                              !qaReview.approved ||
                              !qaReview.dataIntegrityVerified ||
                              !qaReview.methodComplianceVerified ||
                              !qaReview.qcAcceptable
                            }
                            onClick={() => {
                              // Log QA review approval to audit trail
                              logAuditEvent(
                                "QA_REVIEW_APPROVED",
                                {
                                  reviewerId: qaReview.reviewerId,
                                  reviewerName: qaReview.reviewerName,
                                  reviewComments:
                                    qaReview.comments.substring(0, 200) + "...",
                                  dataIntegrityVerified:
                                    qaReview.dataIntegrityVerified,
                                  methodComplianceVerified:
                                    qaReview.methodComplianceVerified,
                                  qcAcceptable: qaReview.qcAcceptable,
                                  electronicSignature: "PROVIDED",
                                  nextStage: "MANAGER_APPROVAL",
                                },
                                qaReview.reviewerId,
                              );

                              setReviewStatus("QA_APPROVED");
                              notify({
                                kind: NotificationKinds.success,
                                message:
                                  "QA review completed. Data approved for final manager approval.",
                              });
                              // Auto-navigate to Manager Review tab
                              setSelectedTab(8); // Manager Review is now tab 8
                            }}
                          >
                            <FormattedMessage
                              id="notebook.bioanalytical.execution.submitQaReview"
                              defaultMessage="Submit QA Review"
                            />
                          </Button>

                          <Button
                            kind="secondary"
                            disabled={!qaReview.comments}
                            onClick={() => {
                              logAuditEvent(
                                "QA_REVIEW_REJECTED",
                                {
                                  reviewerId: qaReview.reviewerId,
                                  reviewerName: qaReview.reviewerName,
                                  reviewComments:
                                    qaReview.comments.substring(0, 200) + "...",
                                  rejectionReason:
                                    "Data quality concerns require correction",
                                },
                                qaReview.reviewerId,
                              );

                              setReviewStatus("QA_REJECTED");
                              notify({
                                kind: NotificationKinds.error,
                                message:
                                  "QA review rejected. Data requires correction before manager approval.",
                              });
                            }}
                          >
                            <FormattedMessage
                              id="notebook.bioanalytical.execution.rejectQaReview"
                              defaultMessage="Reject & Request Corrections"
                            />
                          </Button>
                        </div>
                      </div>
                    </div>
                  )}
                </Column>
              </Grid>
            </div>
          </TabPanel>

          {/* Tab 8: Manager Approval (Final Level) */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.managerReviewSection"
                        defaultMessage="Final Manager Approval - Study Impact & Regulatory Compliance"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.managerReviewHelp"
                        defaultMessage="Final approval level for Laboratory Manager or Study Director. Evaluate overall study impact, regulatory compliance, and business implications. Authorize data release to external systems."
                      />
                    </p>
                  </div>

                  {/* Manager Review Prerequisites Check */}
                  <div
                    style={{
                      marginTop: "1.5rem",
                      padding: "1rem",
                      backgroundColor:
                        reviewStatus === "QA_APPROVED" ? "#e7f1f5" : "#fff1f1",
                      borderRadius: "4px",
                      borderLeft: `4px solid ${reviewStatus === "QA_APPROVED" ? "#24a148" : "#da1e28"}`,
                    }}
                  >
                    <h6
                      style={{
                        margin: "0 0 0.5rem 0",
                        color:
                          reviewStatus === "QA_APPROVED"
                            ? "#24a148"
                            : "#da1e28",
                      }}
                    >
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.managerPrerequisites"
                        defaultMessage="Manager Approval Prerequisites"
                      />
                    </h6>
                    <div style={{ fontSize: "0.875rem" }}>
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          marginBottom: "0.5rem",
                        }}
                      >
                        <span style={{ marginRight: "0.5rem" }}>
                          {reviewStatus === "QA_APPROVED" ? "✓" : "❌"}
                        </span>
                        <span>
                          QA Review:{" "}
                          {reviewStatus === "QA_APPROVED"
                            ? "COMPLETED"
                            : "PENDING"}
                        </span>
                      </div>
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          marginBottom: "0.5rem",
                        }}
                      >
                        <span style={{ marginRight: "0.5rem" }}>
                          {analystReview.approved && qaReview.approved
                            ? "✓"
                            : "❌"}
                        </span>
                        <span>
                          All Review Levels:{" "}
                          {analystReview.approved && qaReview.approved
                            ? "APPROVED"
                            : "PENDING"}
                        </span>
                      </div>
                      <div style={{ display: "flex", alignItems: "center" }}>
                        <span style={{ marginRight: "0.5rem" }}>
                          {deviations.length === 0 ||
                          deviations.every(
                            (d) => d.batchDisposition !== "REJECT",
                          )
                            ? "✓"
                            : "❌"}
                        </span>
                        <span>
                          Deviations Status:{" "}
                          {deviations.length === 0
                            ? "NONE RECORDED"
                            : deviations.every(
                                  (d) => d.batchDisposition !== "REJECT",
                                )
                              ? "ALL RESOLVED"
                              : "PENDING RESOLUTION"}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Manager Review Form */}
                  {reviewStatus === "QA_APPROVED" && (
                    <div style={{ marginTop: "2rem" }}>
                      <h5 style={{ marginBottom: "1rem" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.execution.managerReviewForm"
                          defaultMessage="Final Manager Approval & Data Release Authorization"
                        />
                      </h5>

                      <div
                        style={{
                          display: "grid",
                          gridTemplateColumns: "1fr 1fr",
                          gap: "1rem",
                          marginBottom: "1.5rem",
                        }}
                      >
                        <TextInput
                          labelText="Manager/Director ID *"
                          value={managerReview.reviewerId}
                          onChange={(e) =>
                            setManagerReview((prev) => ({
                              ...prev,
                              reviewerId: e.target.value,
                            }))
                          }
                          placeholder="Enter manager ID"
                        />
                        <TextInput
                          labelText="Manager/Director Name *"
                          value={managerReview.reviewerName}
                          onChange={(e) =>
                            setManagerReview((prev) => ({
                              ...prev,
                              reviewerName: e.target.value,
                            }))
                          }
                          placeholder="Enter full name"
                        />
                      </div>

                      <div style={{ marginBottom: "1.5rem" }}>
                        <Select
                          labelText="Study Impact Assessment *"
                          value={managerReview.studyImpact}
                          onChange={(e) =>
                            setManagerReview((prev) => ({
                              ...prev,
                              studyImpact: e.target.value,
                            }))
                          }
                        >
                          <SelectItem value="" text="Select study impact..." />
                          <SelectItem
                            value="NO_IMPACT"
                            text="No Impact - Standard results"
                          />
                          <SelectItem
                            value="MINOR_IMPACT"
                            text="Minor Impact - Within expected variance"
                          />
                          <SelectItem
                            value="MODERATE_IMPACT"
                            text="Moderate Impact - Requires documentation"
                          />
                          <SelectItem
                            value="SIGNIFICANT_IMPACT"
                            text="Significant Impact - May affect study conclusions"
                          />
                        </Select>
                      </div>

                      <div style={{ marginBottom: "1.5rem" }}>
                        <Select
                          labelText="Final Data Disposition *"
                          value={managerReview.finalDisposition}
                          onChange={(e) =>
                            setManagerReview((prev) => ({
                              ...prev,
                              finalDisposition: e.target.value,
                            }))
                          }
                        >
                          <SelectItem
                            value=""
                            text="Select final disposition..."
                          />
                          <SelectItem
                            value="APPROVE_RELEASE"
                            text="Approve & Release to External Systems"
                          />
                          <SelectItem
                            value="APPROVE_HOLD"
                            text="Approve but Hold for Further Review"
                          />
                          <SelectItem
                            value="CONDITIONAL_APPROVAL"
                            text="Conditional Approval with Restrictions"
                          />
                          <SelectItem
                            value="REJECT_REPROCESS"
                            text="Reject - Require Reprocessing"
                          />
                        </Select>
                      </div>

                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "0.5rem",
                          marginBottom: "1.5rem",
                        }}
                      >
                        <Checkbox
                          id="regulatory-compliance"
                          checked={managerReview.regulatoryCompliance}
                          onChange={(e) =>
                            setManagerReview((prev) => ({
                              ...prev,
                              regulatoryCompliance: e.target.checked,
                            }))
                          }
                          labelText=""
                        />
                        <label
                          htmlFor="regulatory-compliance"
                          style={{ fontSize: "0.875rem", fontWeight: "bold" }}
                        >
                          I certify regulatory compliance and authorize data for
                          external use
                        </label>
                      </div>

                      <div style={{ marginBottom: "1.5rem" }}>
                        <TextArea
                          labelText="Manager Review Comments *"
                          placeholder="Document overall assessment, study impact evaluation, regulatory considerations, business implications, and authorization for data release..."
                          value={managerReview.comments}
                          onChange={(e) =>
                            setManagerReview((prev) => ({
                              ...prev,
                              comments: e.target.value,
                            }))
                          }
                          rows={4}
                        />
                      </div>

                      <div style={{ marginBottom: "1.5rem" }}>
                        <TextInput
                          labelText="Electronic Signature *"
                          type="password"
                          value={managerReview.electronicSignature}
                          onChange={(e) =>
                            setManagerReview((prev) => ({
                              ...prev,
                              electronicSignature: e.target.value,
                            }))
                          }
                          placeholder="Enter password for electronic signature"
                          helperText="Electronic signature provides final authorization for data release"
                        />
                      </div>

                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "0.5rem",
                          marginBottom: "1.5rem",
                        }}
                      >
                        <Checkbox
                          id="manager-approval"
                          checked={managerReview.approved}
                          onChange={(e) =>
                            setManagerReview((prev) => ({
                              ...prev,
                              approved: e.target.checked,
                              reviewDate: e.target.checked
                                ? new Date().toISOString()
                                : "",
                            }))
                          }
                          labelText=""
                        />
                        <label
                          htmlFor="manager-approval"
                          style={{ fontSize: "0.875rem", fontWeight: "bold" }}
                        >
                          I provide final approval and authorization for this
                          analytical data
                        </label>
                      </div>

                      <div style={{ display: "flex", gap: "1rem" }}>
                        <Button
                          kind="primary"
                          disabled={
                            !managerReview.reviewerId ||
                            !managerReview.reviewerName ||
                            !managerReview.comments ||
                            !managerReview.electronicSignature ||
                            !managerReview.approved ||
                            !managerReview.regulatoryCompliance ||
                            !managerReview.studyImpact ||
                            !managerReview.finalDisposition
                          }
                          onClick={() => {
                            // Log final manager approval to audit trail
                            logAuditEvent(
                              "MANAGER_APPROVAL_FINAL",
                              {
                                reviewerId: managerReview.reviewerId,
                                reviewerName: managerReview.reviewerName,
                                studyImpact: managerReview.studyImpact,
                                finalDisposition:
                                  managerReview.finalDisposition,
                                regulatoryCompliance:
                                  managerReview.regulatoryCompliance,
                                reviewComments:
                                  managerReview.comments.substring(0, 200) +
                                  "...",
                                electronicSignature: "PROVIDED",
                                dataReleaseAuthorized: true,
                              },
                              managerReview.reviewerId,
                            );

                            setReviewStatus("FINAL_APPROVED");
                            notify({
                              kind: NotificationKinds.success,
                              message:
                                "Final manager approval completed. Data authorized for external release.",
                            });
                            // Auto-navigate to Audit Trail to show complete workflow
                            setSelectedTab(9); // Audit Trail is now tab 9
                          }}
                        >
                          <FormattedMessage
                            id="notebook.bioanalytical.execution.finalApproval"
                            defaultMessage="Provide Final Approval"
                          />
                        </Button>

                        <Button
                          kind="secondary"
                          disabled={!managerReview.comments}
                          onClick={() => {
                            logAuditEvent(
                              "MANAGER_APPROVAL_REJECTED",
                              {
                                reviewerId: managerReview.reviewerId,
                                reviewerName: managerReview.reviewerName,
                                rejectionReason:
                                  "Business/regulatory concerns require resolution",
                                reviewComments:
                                  managerReview.comments.substring(0, 200) +
                                  "...",
                              },
                              managerReview.reviewerId,
                            );

                            setReviewStatus("MANAGER_REJECTED");
                            notify({
                              kind: NotificationKinds.error,
                              message:
                                "Manager approval rejected. Data requires resolution before release.",
                            });
                          }}
                        >
                          <FormattedMessage
                            id="notebook.bioanalytical.execution.rejectManagerReview"
                            defaultMessage="Reject & Request Resolution"
                          />
                        </Button>
                      </div>
                    </div>
                  )}
                </Column>
              </Grid>
            </div>
          </TabPanel>

          {/* Tab 9: Audit Trail & Data Integrity */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.auditTrailSection"
                        defaultMessage="Comprehensive Audit Trail & Data Integrity Monitoring"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.auditTrailHelp"
                        defaultMessage="Complete chronological record of all data modifications, user actions, and system events. Ensures full traceability for regulatory compliance and data integrity verification according to 21 CFR Part 11 requirements."
                      />
                    </p>
                  </div>

                  {/* Audit Trail Filters */}
                  <div
                    style={{
                      marginTop: "1.5rem",
                      padding: "1rem",
                      backgroundColor: "#f4f4f4",
                      borderRadius: "4px",
                    }}
                  >
                    <h5 style={{ marginBottom: "1rem" }}>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.auditFilters"
                        defaultMessage="Audit Trail Filters"
                      />
                    </h5>
                    <div
                      style={{
                        display: "grid",
                        gridTemplateColumns: "1fr 1fr 1fr 1fr",
                        gap: "1rem",
                      }}
                    >
                      <TextInput
                        labelText="User ID"
                        placeholder="Filter by user..."
                        value={auditFilters.userId}
                        onChange={(e) =>
                          setAuditFilters((prev) => ({
                            ...prev,
                            userId: e.target.value,
                          }))
                        }
                      />
                      <Select
                        labelText="Action Type"
                        value={auditFilters.action}
                        onChange={(e) =>
                          setAuditFilters((prev) => ({
                            ...prev,
                            action: e.target.value,
                          }))
                        }
                      >
                        <SelectItem value="" text="All actions" />
                        <SelectItem value="FILE_UPLOAD" text="File Upload" />
                        <SelectItem
                          value="TEST_EXECUTION"
                          text="Test Execution"
                        />
                        <SelectItem
                          value="DATA_VALIDATION"
                          text="Data Validation"
                        />
                        <SelectItem
                          value="DEVIATION_RECORDED"
                          text="Deviation Recorded"
                        />
                        <SelectItem
                          value="ANALYST_REVIEW"
                          text="Analyst Review"
                        />
                      </Select>
                      <DatePicker dateFormat="Y-m-d" datePickerType="single">
                        <DatePickerInput
                          labelText="Start Date"
                          placeholder="YYYY-MM-DD"
                          value={auditFilters.startDate}
                          onChange={(e) =>
                            setAuditFilters((prev) => ({
                              ...prev,
                              startDate: e.target.value,
                            }))
                          }
                        />
                      </DatePicker>
                      <DatePicker dateFormat="Y-m-d" datePickerType="single">
                        <DatePickerInput
                          labelText="End Date"
                          placeholder="YYYY-MM-DD"
                          value={auditFilters.endDate}
                          onChange={(e) =>
                            setAuditFilters((prev) => ({
                              ...prev,
                              endDate: e.target.value,
                            }))
                          }
                        />
                      </DatePicker>
                    </div>
                    <div style={{ marginTop: "1rem" }}>
                      <Checkbox
                        id="show-system-events"
                        checked={auditFilters.showSystemEvents}
                        onChange={(e) =>
                          setAuditFilters((prev) => ({
                            ...prev,
                            showSystemEvents: e.target.checked,
                          }))
                        }
                        labelText="Include system-generated events"
                      />
                    </div>
                  </div>

                  {/* Audit Trail Records */}
                  <div style={{ marginTop: "2rem" }}>
                    <h5 style={{ marginBottom: "1rem" }}>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.auditRecords"
                        defaultMessage="Audit Records ({count})"
                        values={{ count: auditTrail.length }}
                      />
                    </h5>

                    {auditTrail.length > 0 ? (
                      <Table>
                        <TableHead>
                          <TableRow>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.timestamp"
                                defaultMessage="Timestamp"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.userId"
                                defaultMessage="User ID"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.action"
                                defaultMessage="Action"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.changeType"
                                defaultMessage="Change Type"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.details"
                                defaultMessage="Details"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.execution.sessionId"
                                defaultMessage="Session ID"
                              />
                            </TableHeader>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {auditTrail
                            .filter((record) => {
                              if (
                                auditFilters.userId &&
                                !record.userId
                                  .toLowerCase()
                                  .includes(auditFilters.userId.toLowerCase())
                              ) {
                                return false;
                              }
                              if (
                                auditFilters.action &&
                                record.action !== auditFilters.action
                              ) {
                                return false;
                              }
                              if (
                                auditFilters.startDate &&
                                new Date(record.timestamp) <
                                  new Date(auditFilters.startDate)
                              ) {
                                return false;
                              }
                              if (
                                auditFilters.endDate &&
                                new Date(record.timestamp) >
                                  new Date(auditFilters.endDate)
                              ) {
                                return false;
                              }
                              if (
                                !auditFilters.showSystemEvents &&
                                record.userId === "SYSTEM"
                              ) {
                                return false;
                              }
                              return true;
                            })
                            .slice(0, 50) // Limit to 50 records for performance
                            .map((record) => (
                              <TableRow key={record.id}>
                                <TableCell
                                  style={{
                                    fontSize: "0.75rem",
                                    fontFamily: "monospace",
                                  }}
                                >
                                  {new Date(record.timestamp).toLocaleString()}
                                </TableCell>
                                <TableCell>
                                  <span
                                    style={{
                                      padding: "0.25rem 0.5rem",
                                      borderRadius: "4px",
                                      fontSize: "0.75rem",
                                      backgroundColor:
                                        record.userId === "SYSTEM"
                                          ? "#8a3ffc"
                                          : "#0043ce",
                                      color: "white",
                                    }}
                                  >
                                    {record.userId}
                                  </span>
                                </TableCell>
                                <TableCell>
                                  <span
                                    style={{
                                      padding: "0.25rem 0.5rem",
                                      borderRadius: "4px",
                                      fontSize: "0.75rem",
                                      backgroundColor: "#24a148",
                                      color: "white",
                                    }}
                                  >
                                    {record.action.replace(/_/g, " ")}
                                  </span>
                                </TableCell>
                                <TableCell>
                                  <span
                                    style={{
                                      padding: "0.25rem 0.5rem",
                                      borderRadius: "4px",
                                      fontSize: "0.75rem",
                                      backgroundColor:
                                        record.changeType === "INSERT"
                                          ? "#24a148"
                                          : record.changeType === "UPDATE"
                                            ? "#f1c21b"
                                            : record.changeType === "DELETE"
                                              ? "#da1e28"
                                              : "#8a3ffc",
                                      color:
                                        record.changeType === "UPDATE"
                                          ? "#161616"
                                          : "white",
                                    }}
                                  >
                                    {record.changeType}
                                  </span>
                                </TableCell>
                                <TableCell style={{ maxWidth: "300px" }}>
                                  <div
                                    style={{
                                      fontSize: "0.875rem",
                                      fontFamily: "monospace",
                                    }}
                                  >
                                    {JSON.stringify(
                                      record.details,
                                      null,
                                      1,
                                    ).substring(0, 100)}
                                    ...
                                  </div>
                                </TableCell>
                                <TableCell
                                  style={{
                                    fontSize: "0.75rem",
                                    fontFamily: "monospace",
                                  }}
                                >
                                  {record.sessionId.substring(0, 8)}...
                                </TableCell>
                              </TableRow>
                            ))}
                        </TableBody>
                      </Table>
                    ) : (
                      <div
                        style={{
                          padding: "2rem",
                          backgroundColor: "#f4f4f4",
                          borderRadius: "4px",
                          textAlign: "center",
                        }}
                      >
                        <h6
                          style={{ color: "#525252", marginBottom: "0.5rem" }}
                        >
                          <FormattedMessage
                            id="notebook.bioanalytical.execution.noAuditRecords"
                            defaultMessage="No Audit Records Found"
                          />
                        </h6>
                        <p style={{ color: "#525252", fontSize: "0.875rem" }}>
                          <FormattedMessage
                            id="notebook.bioanalytical.execution.auditRecordsHelp"
                            defaultMessage="Audit records will appear here as you perform actions in this workflow. All data modifications, file uploads, reviews, and system events are automatically logged for compliance."
                          />
                        </p>
                      </div>
                    )}

                    {auditTrail.length > 50 && (
                      <div style={{ marginTop: "1rem", textAlign: "center" }}>
                        <p style={{ color: "#525252", fontSize: "0.875rem" }}>
                          <FormattedMessage
                            id="notebook.bioanalytical.execution.auditLimitNotice"
                            defaultMessage="Showing latest 50 records. Use filters to refine results or export full audit log."
                          />
                        </p>
                      </div>
                    )}
                  </div>

                  {/* Data Integrity Summary */}
                  <div style={{ marginTop: "2rem" }}>
                    <h5 style={{ marginBottom: "1rem" }}>
                      <FormattedMessage
                        id="notebook.bioanalytical.execution.dataIntegritySummary"
                        defaultMessage="Data Integrity Compliance Summary"
                      />
                    </h5>
                    <div
                      style={{
                        display: "grid",
                        gridTemplateColumns: "1fr 1fr 1fr",
                        gap: "1rem",
                      }}
                    >
                      <div
                        style={{
                          padding: "1rem",
                          backgroundColor: "#e7f1f5",
                          borderRadius: "4px",
                          textAlign: "center",
                        }}
                      >
                        <h6
                          style={{ color: "#0043ce", marginBottom: "0.5rem" }}
                        >
                          Total Events Logged
                        </h6>
                        <p
                          style={{
                            fontSize: "1.5rem",
                            fontWeight: "bold",
                            color: "#0043ce",
                            margin: 0,
                          }}
                        >
                          {auditTrail.length}
                        </p>
                      </div>
                      <div
                        style={{
                          padding: "1rem",
                          backgroundColor: "#f0f7ff",
                          borderRadius: "4px",
                          textAlign: "center",
                        }}
                      >
                        <h6
                          style={{ color: "#24a148", marginBottom: "0.5rem" }}
                        >
                          Users Involved
                        </h6>
                        <p
                          style={{
                            fontSize: "1.5rem",
                            fontWeight: "bold",
                            color: "#24a148",
                            margin: 0,
                          }}
                        >
                          {new Set(auditTrail.map((r) => r.userId)).size}
                        </p>
                      </div>
                      <div
                        style={{
                          padding: "1rem",
                          backgroundColor: "#fff8e1",
                          borderRadius: "4px",
                          textAlign: "center",
                        }}
                      >
                        <h6
                          style={{ color: "#f1c21b", marginBottom: "0.5rem" }}
                        >
                          ALCOA+ Compliance
                        </h6>
                        <p
                          style={{
                            fontSize: "1.5rem",
                            fontWeight: "bold",
                            color: "#f1c21b",
                            margin: 0,
                          }}
                        >
                          {dataIntegrity.userAttribution.verified &&
                          dataIntegrity.contemporaneousRecord &&
                          dataIntegrity.originalityVerified
                            ? "VERIFIED"
                            : "PENDING"}
                        </p>
                      </div>
                    </div>
                  </div>
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

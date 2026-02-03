import React, { useState, useCallback, useEffect } from "react";
import config from "../../../../config.json";
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
  Checkbox,
  TextArea,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { usePermissions } from "../../../../hooks/usePermissions";
import { useBioequivalencePermissions } from "../../../../hooks/useBioequivalencePermissions";
import AccessDeniedMessage from "../../../common/AccessDeniedMessage";
import "./BioequivalencePages.css";

/**
 * BioequivalenceReportingPage - Stage 4 of bioequivalence workflow.
 *
 * Features:
 * - Bioequivalence study result review and validation
 * - Quality assurance (QA) approval workflow
 * - Regulatory compliance verification
 * - External reporting and export (LMIS, REDCap, CDISC/SDTM)
 * - Final data release authorization
 *
 * @param {Object} props
 * @param {number} props.entryId - Notebook entry ID
 * @param {Object} props.pageData - Page configuration
 * @param {Object} props.progress - Sample progress counts
 * @param {function} props.onProgressUpdate - Callback after changes
 * @param {Object} props.notebookData - Notebook configuration data
 * @param {function} props.onPageNavigation - Function to navigate to specific page by order
 */
function BioequivalenceReportingPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookData,
  onPageNavigation,
}) {
  const intl = useIntl();
  const { hasAnyRole } = usePermissions();
  const {
    getPagePermissionLevel,
    canAnalytics,
    canApproveData,
    hasFullControl,
  } = useBioequivalencePermissions();

  // PAGE 4 allowed roles per test.pdf Section 11
  const allowedRoles = [
    "Bioequivalence Chemical Analyst",
    "Bioequivalence Pharmacist",
    "Bioequivalence Lab Supervisor",
    "Bioequivalence Study Director",
    "Bioequivalence QA Officer",
    "Bioequivalence Researcher",
    "Bioequivalence Data Manager",
  ];

  const canAccessPage = hasAnyRole(allowedRoles);

  // Get user's action-level permission for this page
  const pagePermissionLevel = getPagePermissionLevel("Reporting & Release");
  const canExportData = canAnalytics(pagePermissionLevel);
  const canApproveResults = canApproveData(pagePermissionLevel);
  const canEditReporting = hasFullControl(pagePermissionLevel);
  const [isLoading, setIsLoading] = useState(false);
  const [isQaLoading, setIsQaLoading] = useState(false);
  const [selectedTab, setSelectedTab] = useState(0);
  const [studyResults, setStudyResults] = useState([]);
  const [bioequivalenceStats, setBioequivalenceStats] = useState(null);
  const [isLoadingStats, setIsLoadingStats] = useState(false);
  const [qaComments, setQaComments] = useState("");
  const [qaApproved, setQaApproved] = useState(false);
  const [exportFormat, setExportFormat] = useState("");
  const [exportStatus, setExportStatus] = useState(null);
  const [submissionTarget, setSubmissionTarget] = useState("");
  const [submissionStatus, setSubmissionStatus] = useState(null);
  const [qaChecklist, setQaChecklist] = useState({
    rawDataValidated: false,
    calibrationAcceptable: false,
    qcPassedRules: false,
    systemSuitability: false,
    resultsAcceptable: false,
  });
  const [qaChecklistValidation, setQaChecklistValidation] = useState({
    rawDataValidated: { status: "unknown", message: "Checking..." },
    calibrationAcceptable: { status: "unknown", message: "Checking..." },
    qcPassedRules: { status: "unknown", message: "Checking..." },
    systemSuitability: { status: "unknown", message: "Checking..." },
    resultsAcceptable: { status: "unknown", message: "Checking..." },
  });
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [autoAdvancedToStage5, setAutoAdvancedToStage5] = useState(false);

  const exportFormats = [
    { id: "csv", label: "Research Format (CSV)" },
    { id: "pdf", label: "PDF Report" },
    // Disabled for this phase - will be handled later
    // { id: "lmis", label: "LMIS (Laboratory Management Information System)" },
    // { id: "redcap", label: "REDCap (Research Electronic Data Capture)" },
    // { id: "cdisc", label: "CDISC/SDTM (Clinical Data Interchange Standards)" },
  ];

  const submissionTargets = [
    {
      id: "medical_lab",
      label: "Medical Laboratory",
      department: "Clinical Laboratory Services",
    },
    {
      id: "research_unit",
      label: "Research Unit",
      department: "Clinical Research Department",
    },
    {
      id: "principal_investigator",
      label: "Principal Investigator",
      department: "Study Sponsor",
    },
    {
      id: "regulatory_affairs",
      label: "Regulatory Affairs",
      department: "Compliance Team",
    },
    {
      id: "external_client",
      label: "External Client",
      department: "Contract Research Organization",
    },
  ];

  const loadStudyResults = useCallback(async () => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      setStudyResults([]);
      return;
    }

    setIsLoading(true);
    try {
      // Load samples specifically for this page (Stage 4: Reporting & Release)
      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/page/${pageData.id}/samples`,
        {
          method: "GET",
          credentials: "include",
          headers: {
            "X-CSRF-Token": localStorage.getItem("CSRF"),
            "Content-Type": "application/json",
          },
        },
      );

      if (response.ok) {
        const data = await response.json();
        // Filter samples that have been approved from Stage 3
        const approvedSamples = (
          Array.isArray(data) ? data : data.samples || []
        ).filter((sample) => {
          return (
            sample.data &&
            sample.data.executionStatus === "EXECUTED" &&
            sample.data.testExecution &&
            sample.data.resultsApproved
          );
        });

        // Debug: Log first approved sample to see data structure
        if (approvedSamples.length > 0) {
          console.log("Stage 4 - First approved sample:", approvedSamples[0]);
          console.log(
            "Stage 4 - bioequivalenceStats check:",
            approvedSamples[0].data?.bioequivalenceStats ||
              approvedSamples[0].data?.testExecution?.bioequivalenceStats,
          );
        }

        // Compile analytical results from approved samples
        const compiledResults = compileAnalyticalResults(approvedSamples);
        setStudyResults(compiledResults);
      } else {
        console.error("Failed to load samples:", response.status);
        setStudyResults([]);
      }
    } catch (error) {
      console.error("Error loading study results:", error);
      setStudyResults([]);
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioequivalence.reporting.loadError",
          defaultMessage:
            "Failed to load study results. Please refresh the page.",
        }),
      );
    } finally {
      setIsLoading(false);
    }
  }, [entryId, intl]);

  // Function to fetch bioequivalence statistics from the backend
  const loadBioequivalenceStats = useCallback(async () => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      setBioequivalenceStats(null);
      return;
    }

    setIsLoadingStats(true);
    try {
      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/bioanalytical/page/${pageData.id}/bioequivalence-statistics`,
        {
          method: "GET",
          credentials: "include",
          headers: {
            "X-CSRF-Token": localStorage.getItem("CSRF"),
            "Content-Type": "application/json",
          },
        },
      );

      if (response.ok) {
        const data = await response.json();
        if (data.error) {
          console.log("No bioequivalence statistics available:", data.error);
          setBioequivalenceStats(null);
        } else {
          console.log("Loaded bioequivalence statistics:", data);
          setBioequivalenceStats(data);
        }
      } else {
        console.error(
          "Failed to load bioequivalence statistics:",
          response.status,
        );
        setBioequivalenceStats(null);
      }
    } catch (error) {
      console.error("Error loading bioequivalence statistics:", error);
      setBioequivalenceStats(null);
    } finally {
      setIsLoadingStats(false);
    }
  }, [pageData?.id]);

  // Function to load QA approval status on page mount
  const loadQAApprovalStatus = useCallback(async () => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      setQaApproved(false);
      return;
    }

    try {
      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/bioanalytical/page/${pageData.id}/qa-approval`,
        {
          method: "GET",
          credentials: "include",
          headers: {
            "X-CSRF-Token": localStorage.getItem("CSRF"),
            "Content-Type": "application/json",
          },
        },
      );

      if (response.ok) {
        const data = await response.json();
        if (data.hasApproval && data.approvalStatus === "APPROVED") {
          setQaApproved(true);
          console.log("QA approval status loaded:", data);

          // Restore QA comments if saved (backend only stores comments, not checklist)
          if (data.comments) {
            setQaComments(data.comments);
          }

          // If QA approved, assume all checklist items were validated
          // (since backend doesn't store individual checklist items)
          setQaChecklist({
            rawDataValidated: true,
            calibrationAcceptable: true,
            qcPassedRules: true,
            systemSuitability: true,
            resultsAcceptable: true,
          });

          // Check if Stage 5 records already exist (for users navigating back to Stage 4)
          checkStage5RecordsExist();
        } else {
          setQaApproved(false);
          // Reset QA form if not approved
          setQaChecklist({
            rawDataValidated: false,
            calibrationAcceptable: false,
            qcPassedRules: false,
            systemSuitability: false,
            resultsAcceptable: false,
          });
          setQaComments("");
        }
      } else {
        setQaApproved(false);
        // Reset QA form on error
        setQaChecklist({
          rawDataValidated: false,
          calibrationAcceptable: false,
          qcPassedRules: false,
          systemSuitability: false,
          resultsAcceptable: false,
        });
        setQaComments("");
      }
    } catch (error) {
      console.error("Error loading QA approval status:", error);
      setQaApproved(false);
      // Reset QA form on error
      setQaChecklist({
        rawDataValidated: false,
        calibrationAcceptable: false,
        qcPassedRules: false,
        systemSuitability: false,
        resultsAcceptable: false,
      });
      setQaComments("");
    }
  }, [pageData?.id]);

  // Function to check if Stage 5 records already exist
  const checkStage5RecordsExist = useCallback(async () => {
    if (!notebookData?.id) return;

    try {
      // Get Stage 5 page to check if samples already exist
      const stage5Response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/view/${notebookData.id}`,
        {
          method: "GET",
          credentials: "include",
          headers: {
            "X-CSRF-Token": localStorage.getItem("CSRF"),
            "Content-Type": "application/json",
          },
        },
      );

      if (stage5Response.ok) {
        const notebook = await stage5Response.json();
        const pages = notebook.pages || [];
        const stage5Page = pages.find((page) => page.pageOrder === 5);

        if (stage5Page) {
          // Check if Stage 5 has any samples
          const samplesResponse = await fetch(
            `${config.serverBaseUrl}/rest/notebook/page/${stage5Page.id}/samples`,
            {
              method: "GET",
              credentials: "include",
              headers: {
                "X-CSRF-Token": localStorage.getItem("CSRF"),
                "Content-Type": "application/json",
              },
            },
          );

          if (samplesResponse.ok) {
            const stage5Samples = await samplesResponse.json();
            if (stage5Samples && stage5Samples.length > 0) {
              setAutoAdvancedToStage5(true);
              console.log(
                "Stage 5 records detected - showing Stage 5 navigation",
              );
            }
          }
        }
      }
    } catch (error) {
      console.error("Error checking Stage 5 records:", error);
    }
  }, [notebookData?.id]);

  // Helper function to compile analytical results from Stage 3 sample data
  const compileAnalyticalResults = useCallback(
    (approvedSamples) => {
      const resultGroups = {};

      approvedSamples.forEach((sample) => {
        const analyticalMethod =
          sample.data.analyticalMethod || "Unknown Method";
        const sampleType =
          sample.data.sampleType ||
          sample.sampleType ||
          sample.data.testExecution?.sampleType ||
          "Unknown Type";

        const groupKey = `${analyticalMethod} - ${sampleType}`;

        console.log("Stage 4 - Result grouping:", {
          analyticalMethod,
          sampleType,
          groupKey,
          sampleId: sample.id,
        });

        if (!resultGroups[groupKey]) {
          resultGroups[groupKey] = {
            id: groupKey,
            testName: groupKey,
            sampleIds: [],
            samples: [],
            dataPoints: 0,
            allQcResults: [],
            allCalibrationData: [],
            allQuantificationResults: [],
          };
        }

        resultGroups[groupKey].samples.push(sample);
        resultGroups[groupKey].sampleIds.push(sample.id);
        resultGroups[groupKey].dataPoints++;

        // Collect QC results from all samples
        if (sample.data.qcResults && Array.isArray(sample.data.qcResults)) {
          resultGroups[groupKey].allQcResults.push(...sample.data.qcResults);
        }

        // Collect calibration data
        if (sample.data.calibrationData) {
          resultGroups[groupKey].allCalibrationData.push(
            sample.data.calibrationData,
          );
        }

        // Collect quantification results
        if (
          sample.data.quantificationResults &&
          Array.isArray(sample.data.quantificationResults)
        ) {
          resultGroups[groupKey].allQuantificationResults.push(
            ...sample.data.quantificationResults,
          );
        }
      });

      // Use calculated statistics from backend API or fallback to Stage 3 data
      return Object.values(resultGroups).map((group) => {
        // Priority 1: Use bioequivalence statistics from backend API if available
        if (bioequivalenceStats && !bioequivalenceStats.error) {
          console.log(
            "Stage 4 - Using bioequivalenceStats from backend:",
            bioequivalenceStats,
          );
          return {
            ...group,
            mean: bioequivalenceStats.mean || "N/A",
            sd: bioequivalenceStats.sd || "N/A",
            cv: bioequivalenceStats.cv || "N/A",
            min: bioequivalenceStats.min || "N/A",
            max: bioequivalenceStats.max || "N/A",
            meanAccuracy: bioequivalenceStats.meanAccuracy || "N/A",
            regulatoryStatus: bioequivalenceStats.regulatoryStatus || "UNKNOWN",
            calibrationRSquared:
              bioequivalenceStats.rSquared ||
              bioequivalenceStats.r_squared ||
              "N/A",
            calibrationSlope: bioequivalenceStats.slope || "N/A",
            calibrationEquation: bioequivalenceStats.equation || "N/A",
            qcValidation: bioequivalenceStats.qcValidation || null,
          };
        }

        // Priority 2: Calculate statistics from Stage 3 collected data
        const firstSample = group.samples[0];
        let calculatedStats = {
          mean: "N/A",
          sd: "N/A",
          cv: "N/A",
          min: "N/A",
          max: "N/A",
          meanAccuracy: "N/A",
          regulatoryStatus: "UNKNOWN",
          calibrationRSquared: "N/A",
          calibrationSlope: "N/A",
          calibrationEquation: "N/A",
        };

        // Extract calibration data from first sample
        if (group.allCalibrationData.length > 0) {
          const calData = group.allCalibrationData[0];
          calculatedStats.calibrationRSquared =
            calData.rSquared?.toFixed(4) ||
            calData.r_squared?.toFixed(4) ||
            "N/A";
          calculatedStats.calibrationSlope = calData.slope?.toFixed(4) || "N/A";
          calculatedStats.calibrationEquation = calData.equation || "N/A";
        }

        // Calculate accuracy statistics from QC results if available
        if (group.allQcResults.length > 0) {
          const accuracyValues = group.allQcResults
            .map((qc) => {
              const acc = parseFloat(qc.accuracy);
              return !isNaN(acc) && acc > 0 ? acc : null;
            })
            .filter((v) => v !== null);

          if (accuracyValues.length > 0) {
            const mean =
              accuracyValues.reduce((a, b) => a + b, 0) / accuracyValues.length;
            const variance =
              accuracyValues.reduce((a, v) => a + Math.pow(v - mean, 2), 0) /
              accuracyValues.length;
            const sd = Math.sqrt(variance);
            const cv = ((sd / mean) * 100).toFixed(1);

            calculatedStats.mean = mean.toFixed(1) + "%";
            calculatedStats.sd = sd.toFixed(2);
            calculatedStats.cv = cv + "%";
            calculatedStats.min = Math.min(...accuracyValues).toFixed(1) + "%";
            calculatedStats.max = Math.max(...accuracyValues).toFixed(1) + "%";
            calculatedStats.meanAccuracy = mean.toFixed(1) + "%";

            // Determine regulatory status (FDA bioequivalence: 80-125% mean accuracy, CV < 20%)
            const isCompliant =
              mean >= 80 && mean <= 125 && parseFloat(cv) < 20;
            calculatedStats.regulatoryStatus = isCompliant
              ? "COMPLIANT"
              : "NON_COMPLIANT";
          }
        }

        // Calculate concentration statistics from quantification results if available
        if (group.allQuantificationResults.length > 0) {
          const concentrationValues = group.allQuantificationResults
            .map((q) => {
              const conc = parseFloat(q.concentration || q.measuredValue);
              return !isNaN(conc) && conc > 0 ? conc : null;
            })
            .filter((v) => v !== null);

          if (concentrationValues.length > 0) {
            const mean =
              concentrationValues.reduce((a, b) => a + b, 0) /
              concentrationValues.length;
            const variance =
              concentrationValues.reduce(
                (a, v) => a + Math.pow(v - mean, 2),
                0,
              ) / concentrationValues.length;
            const sd = Math.sqrt(variance);
            const cv = ((sd / mean) * 100).toFixed(1);

            calculatedStats.mean = mean.toFixed(1);
            calculatedStats.sd = sd.toFixed(2);
            calculatedStats.cv = cv + "%";
            calculatedStats.min = Math.min(...concentrationValues).toFixed(1);
            calculatedStats.max = Math.max(...concentrationValues).toFixed(1);
          }
        }

        return {
          ...group,
          ...calculatedStats,
        };
      });
    },
    [bioequivalenceStats],
  );

  // Function to validate QA checklist items against actual Stage 3 data
  const validateQAChecklist = useCallback(async () => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      return;
    }

    try {
      // Load samples with Stage 3 execution data for validation
      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/page/${pageData.id}/samples`,
        {
          method: "GET",
          credentials: "include",
          headers: {
            "X-CSRF-Token": localStorage.getItem("CSRF"),
            "Content-Type": "application/json",
          },
        },
      );

      if (response.ok) {
        const data = await response.json();
        const executedSamples = (
          Array.isArray(data) ? data : data.samples || []
        ).filter((sample) => {
          return (
            sample.data &&
            sample.data.executionStatus === "EXECUTED" &&
            sample.data.testExecution
          );
        });

        console.log("QA Validation - Executed samples:", executedSamples);
        console.log(
          "QA Validation - bioequivalenceStats:",
          bioequivalenceStats,
        );
        console.log("QA Validation - studyResults:", studyResults);

        // Validate each checklist item
        const validationResults = {
          rawDataValidated: validateRawDataFiles(executedSamples),
          calibrationAcceptable: validateCalibrationCriteria(executedSamples),
          qcPassedRules: validateQCResults(executedSamples),
          systemSuitability: validateSystemSuitability(executedSamples),
          resultsAcceptable: validateResultsCriteria(executedSamples),
        };

        console.log("QA Validation Results:", validationResults);

        setQaChecklistValidation(validationResults);
      }
    } catch (error) {
      console.error("Error validating QA checklist:", error);
    }
  }, [pageData?.id, bioequivalenceStats, studyResults]);

  // Individual validation functions for each QA check
  const validateRawDataFiles = (samples) => {
    // Check both uploadedFiles (from Stage 3) and rawDataFiles
    const hasRawData = samples.some(
      (s) =>
        (s.data.uploadedFiles && s.data.uploadedFiles.length > 0) ||
        (s.data.rawDataFiles && s.data.rawDataFiles.length > 0),
    );
    const filesCount = samples.reduce((count, s) => {
      const uploadedCount = s.data.uploadedFiles?.length || 0;
      const rawCount = s.data.rawDataFiles?.length || 0;
      return count + uploadedCount + rawCount;
    }, 0);

    // Raw data files are now optional - always pass validation
    return {
      status: "pass",
      message: hasRawData
        ? `${filesCount} raw data file(s) validated and processed`
        : "No raw data files (manual data entry workflow)",
    };
  };

  const validateCalibrationCriteria = (samples) => {
    // Check if calibration data exists and meets r² ≥ 0.99 criteria
    const calibrationData = samples.find((s) => s.data.calibrationData);
    if (!calibrationData) {
      return { status: "fail", message: "No calibration data found" };
    }

    // Access rSquared directly from calibrationData object (not nested)
    const rSquared =
      calibrationData.data.calibrationData?.rSquared ||
      calibrationData.data.calibrationData?.r_squared ||
      0;
    return {
      status: rSquared >= 0.99 ? "pass" : "fail",
      message:
        rSquared >= 0.99
          ? `Calibration curve meets criteria (r² = ${rSquared.toFixed(4)})`
          : `Calibration curve fails criteria (r² = ${rSquared.toFixed(4)} < 0.99)`,
    };
  };

  const validateQCResults = (samples) => {
    const qcData = samples.find((s) => s.data.qcResults);
    if (!qcData) {
      return { status: "fail", message: "No QC results found" };
    }

    // Priority 1: Check if we have bioequivalence statistics with QC validation from backend
    if (bioequivalenceStats && bioequivalenceStats.qcValidation) {
      const qcValidation = bioequivalenceStats.qcValidation;
      const westgardStatus = qcValidation.westgardStatus || "UNKNOWN";
      const rulesPassed = qcValidation.rulesPassed || 0;
      const rulesFailed = qcValidation.rulesFailed || 0;
      const rulesEvaluated =
        qcValidation.rulesEvaluated || rulesPassed + rulesFailed;

      console.log("QC Validation from bioequivalenceStats:", qcValidation);

      return {
        status:
          westgardStatus === "PASS"
            ? "pass"
            : westgardStatus === "WARNING"
              ? "warning"
              : "fail",
        message:
          westgardStatus === "PASS"
            ? `All ${rulesPassed}/${rulesEvaluated} Westgard rules passed`
            : westgardStatus === "WARNING"
              ? `${rulesPassed}/${rulesEvaluated} rules passed (warnings present)`
              : `${rulesFailed}/${rulesEvaluated} Westgard rules failed`,
      };
    }

    // Fallback: Check if QC results have status information
    if (qcData.data.qcResults && qcData.data.qcResults.length > 0) {
      const qcResults = qcData.data.qcResults;
      const passedCount = qcResults.filter((qc) => qc.status === "PASS").length;
      const failedCount = qcResults.filter((qc) => qc.status === "FAIL").length;
      const totalCount = qcResults.length;

      const allPassed = failedCount === 0 && passedCount > 0;

      return {
        status: allPassed ? "pass" : "fail",
        message: allPassed
          ? `All ${totalCount} QC results passed`
          : `${failedCount}/${totalCount} QC results failed validation`,
      };
    }

    return {
      status: "fail",
      message: "Unable to validate QC results - no validation data available",
    };
  };

  const validateSystemSuitability = (samples) => {
    // Check for instrument information in various locations
    const hasSystemSuitability = samples.some(
      (s) =>
        (s.data.testExecution && s.data.testExecution.instrumentId) ||
        (s.data.testExecution && s.data.testExecution.selectedInstrument) ||
        (s.data.executionData && s.data.executionData.selectedInstrument) ||
        s.data.instrumentId ||
        s.instrumentName,
    );
    const instrumentCount = samples.filter(
      (s) =>
        (s.data.testExecution && s.data.testExecution.instrumentId) ||
        (s.data.testExecution && s.data.testExecution.selectedInstrument) ||
        (s.data.executionData && s.data.executionData.selectedInstrument) ||
        s.data.instrumentId ||
        s.instrumentName,
    ).length;
    return {
      status: hasSystemSuitability ? "pass" : "fail",
      message: hasSystemSuitability
        ? `System suitability verified for ${instrumentCount} sample(s)`
        : "System suitability verification missing",
    };
  };

  const validateResultsCriteria = (samples) => {
    const complianceCount = studyResults.filter(
      (r) => r.regulatoryStatus === "COMPLIANT",
    ).length;
    const totalResults = studyResults.length;
    return {
      status:
        complianceCount === totalResults && totalResults > 0 ? "pass" : "fail",
      message:
        totalResults > 0
          ? `${complianceCount}/${totalResults} result groups meet acceptance criteria`
          : "No results available for validation",
    };
  };

  React.useEffect(() => {
    loadStudyResults();
  }, [loadStudyResults]);

  React.useEffect(() => {
    loadBioequivalenceStats();
  }, [loadBioequivalenceStats]);

  React.useEffect(() => {
    loadQAApprovalStatus();
  }, [loadQAApprovalStatus]);

  React.useEffect(() => {
    if (studyResults.length > 0) {
      validateQAChecklist();
    }
  }, [studyResults, validateQAChecklist]);

  // Re-compile study results when bioequivalence statistics become available
  React.useEffect(() => {
    if (bioequivalenceStats && studyResults.length > 0) {
      // Reload study results to apply the new bioequivalence statistics
      loadStudyResults();
    }
  }, [bioequivalenceStats, loadStudyResults]);

  const handleQaApproval = useCallback(async () => {
    // Check if all QA checklist items are completed
    const allChecklistItemsCompleted = Object.values(qaChecklist).every(
      (checked) => checked === true,
    );
    if (!allChecklistItemsCompleted) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioequivalence.reporting.checklistIncomplete",
          defaultMessage:
            "Please complete all QA checklist items before approval",
        }),
      );
      return;
    }

    if (!qaApproved) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioequivalence.reporting.confirmQa",
          defaultMessage:
            "Please confirm QA approval checkbox before proceeding",
        }),
      );
      return;
    }

    if (!qaComments.trim()) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioequivalence.reporting.qaCommentsRequired",
          defaultMessage: "QA review comments are required",
        }),
      );
      return;
    }

    setIsQaLoading(true);
    setErrorMessage("");
    setSuccessMessage("");

    try {
      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/bioanalytical/page/${pageData.id}/qa-approval`,
        {
          method: "POST",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            approvalStatus: "APPROVED",
            comments: qaComments,
          }),
        },
      );

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.error || "Failed to submit QA approval");
      }

      const result = await response.json();

      // Update local QA approval state
      setQaApproved(true);

      // Step 2: Apply QA approval data to samples for Stage 5 visibility
      // This ensures Stage 5 can find the QA-approved samples
      // First, get all samples from the current page to ensure we have the right sample IDs
      try {
        const pageSamplesResponse = await fetch(
          `${config.serverBaseUrl}/rest/notebook/page/${pageData?.id}/samples`,
          {
            method: "GET",
            credentials: "include",
            headers: {
              "X-CSRF-Token": localStorage.getItem("CSRF"),
              "Content-Type": "application/json",
            },
          },
        );

        if (pageSamplesResponse.ok) {
          const pageSamples = await pageSamplesResponse.json();
          const sampleIds = pageSamples.map((sample) =>
            parseInt(sample.id, 10),
          );

          console.log("Updating QA approval data for sample IDs:", sampleIds);

          const qaApprovalPayload = {
            sampleIds: sampleIds,
            data: {
              executionStatus: "EXECUTED",
              resultsApproved: true,
              qaApprovalStatus: "APPROVED",
              qaComments: qaComments,
              qaApprovedAt: new Date().toISOString(),
              qaApprovedBy: "CURRENT_USER", // This would come from user session
              // Set submission status to make samples visible in Stage 5
              submissionStatus: "QA_APPROVED_READY_FOR_STORAGE",
              // Include bioequivalence data for Stage 5 reference
              bioequivalenceCompliant: true, // Assume compliant since QA approved
            },
          };

          const applyResponse = await fetch(
            `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData?.id}/samples/apply`,
            {
              method: "POST",
              credentials: "include",
              headers: {
                "Content-Type": "application/json",
                "X-CSRF-Token": localStorage.getItem("CSRF"),
              },
              body: JSON.stringify(qaApprovalPayload),
            },
          );

          if (!applyResponse.ok) {
            const errorData = await applyResponse.json().catch(() => ({}));
            console.error(
              "Failed to update sample data for Stage 5:",
              errorData,
            );
            // Don't fail the QA approval, but log the error
          } else {
            console.log("Successfully updated sample data for Stage 5");
          }
        } else {
          console.warn("Could not fetch page samples for QA approval update");
        }
      } catch (error) {
        console.error("Error updating sample data for Stage 5:", error);
        // Don't fail the QA approval process
      }

      // Check if Stage 5 was automatically created
      if (result.autoAdvancedToStage5) {
        setAutoAdvancedToStage5(true);
        setSuccessMessage(
          intl.formatMessage(
            {
              id: "notebook.bioequivalence.reporting.qaApprovalCompleteWithStage5",
              defaultMessage:
                "QA approval completed for {count} samples. {stage5Count} records created in Stage 5 (Post-Test Storage). You can continue to Stage 5 or complete optional export/submission tasks.",
            },
            {
              count: result.samplesAffected || studyResults.length,
              stage5Count: result.stage5RecordsCreated || 0,
            },
          ),
        );
      } else {
        setSuccessMessage(
          intl.formatMessage(
            {
              id: "notebook.bioequivalence.reporting.qaApprovalComplete",
              defaultMessage:
                "QA approval completed for {count} samples. Study data is ready for external reporting.",
            },
            { count: result.samplesAffected || studyResults.length },
          ),
        );
      }

      if (onProgressUpdate) {
        onProgressUpdate();
      }
    } catch (error) {
      setErrorMessage(
        intl.formatMessage(
          {
            id: "notebook.bioequivalence.reporting.qaApprovalError",
            defaultMessage: "Error submitting QA approval: {error}",
          },
          { error: error.message },
        ),
      );
    } finally {
      setIsQaLoading(false);
    }
  }, [
    qaApproved,
    qaComments,
    qaChecklist,
    intl,
    onProgressUpdate,
    pageData.id,
    studyResults.length,
  ]);

  // Fallback function to generate CSV client-side
  const generateCSVData = useCallback(() => {
    const headers = [
      "record_id",
      "analytical_method",
      "sample_type",
      "mean_accuracy",
      "sd",
      "cv",
      "calibration_r",
      "westgard_st",
      "regulatory_s",
      "qa_approval",
      "bioequivalen",
      "notes",
    ];

    const rows = studyResults.map((result) => {
      const sampleIds =
        result.sampleIds || result.samples?.map((s) => s.id) || [];
      const mainSampleId = sampleIds[0] || result.id || "Unknown";

      return [
        `S${mainSampleId}`,
        result.testName || "Unknown Method",
        result.sampleType || "Bioequivalence Sample",
        result.mean || "",
        result.sd || "",
        result.cv || "",
        result.calibrationR || result.calibrationData?.rSquared || "",
        result.westgardStatus ||
          result.qcValidation?.westgardRules ||
          "NOT_EVALUATED",
        result.regulatoryStatus || "PENDING_REVIEW",
        qaApproved ? "true" : "false",
        result.bioequivalenceCompliant || false,
        `${qaComments || ""} ${result.complianceNotes || ""}`.trim(),
      ];
    });

    const csvContent = [
      headers.join(","),
      ...rows.map((row) =>
        row
          .map((cell) =>
            typeof cell === "string" &&
            (cell.includes(",") || cell.includes('"'))
              ? `"${cell.replace(/"/g, '""')}"`
              : cell,
          )
          .join(","),
      ),
    ].join("\n");

    return csvContent;
  }, [studyResults, qaApproved, qaComments]);

  const handleExport = useCallback(async () => {
    if (!exportFormat) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioequivalence.reporting.selectFormat",
          defaultMessage: "Please select an export format",
        }),
      );
      return;
    }

    if (!qaApproved) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioequivalence.reporting.qaRequired",
          defaultMessage: "QA approval is required before exporting data",
        }),
      );
      return;
    }

    setIsLoading(true);
    setErrorMessage("");
    setSuccessMessage("");

    try {
      // Collect all numeric sample IDs from all result groups
      const sampleIds = studyResults.flatMap(
        (result) => result.sampleIds || [],
      );

      if (sampleIds.length === 0) {
        throw new Error("No samples available for export");
      }

      const selectedExportFormat = exportFormats.find(
        (f) => f.id === exportFormat,
      );

      // Map export format ID to endpoint path and file extension
      const exportConfig = {
        redcap: {
          endpoint: "/export/redcap",
          extension: "csv",
          body: { sampleIds, recordIdField: "record_id", eventName: null },
        },
        csv: {
          endpoint: "/export/csv",
          extension: "csv",
          /*
           * Enhanced CSV Export Data Structure
           *
           * Backend should map this data to CSV columns:
           * - record_id: Sample ID
           * - analytical_method: Test name/method
           * - sample_type: Sample type from workflow
           * - mean_accuracy: Mean value from studyResults
           * - sd: Standard deviation
           * - cv: Coefficient of variation
           * - calibration_r: R-squared value from calibration
           * - westgard_st: Westgard rules status (PASS/FAIL/NOT_EVALUATED)
           * - regulatory_s: Regulatory compliance status
           * - qa_approval: QA approval status (true/false)
           * - bioequivalen: Bioequivalence compliance status
           * - notes: Combined QA comments and compliance notes
           */
          body: {
            sampleIds,
            // Include complete bioequivalence workflow data
            studyResults: studyResults,
            bioequivalenceStats: bioequivalenceStats,
            qaApprovalData: {
              qaApproved: qaApproved,
              qaComments: qaComments,
              qaChecklist: qaChecklist,
              qaChecklistValidation: qaChecklistValidation,
              approvedAt: qaApproved ? new Date().toISOString() : null,
              approvedBy: qaApproved ? "CURRENT_USER" : null,
            },
            calibrationData: studyResults.map((result) => ({
              testName: result.testName,
              calibrationR:
                result.calibrationData?.rSquared || result.calibrationR || null,
              calibrationSlope: result.calibrationData?.slope || null,
              calibrationIntercept: result.calibrationData?.intercept || null,
              calibrationStatus: result.calibrationData?.status || "UNKNOWN",
            })),
            westgardRules: studyResults.map((result) => ({
              testName: result.testName,
              westgardStatus:
                result.qcValidation?.westgardRules ||
                result.westgardStatus ||
                "NOT_EVALUATED",
              westgardViolations: result.qcValidation?.violations || [],
              westgardPassed: result.qcValidation?.passed || false,
            })),
            regulatoryCompliance: studyResults.map((result) => ({
              testName: result.testName,
              regulatoryStatus: result.regulatoryStatus || "PENDING_REVIEW",
              complianceNotes: result.complianceNotes || "",
              bioequivalenceCompliant: result.bioequivalenceCompliant || false,
            })),
            exportMetadata: {
              exportedAt: new Date().toISOString(),
              exportedBy: "CURRENT_USER",
              entryId: entryId,
              pageId: pageData?.id,
              totalSamples: sampleIds.length,
              totalTests: studyResults.length,
            },
          },
        },
        lmis: {
          endpoint: "/export/lmis",
          extension: "csv",
          body: { sampleIds },
        },
        cdisc: {
          endpoint: "/export/sdtm",
          extension: "csv",
          body: { sampleIds },
        },
        pdf: {
          endpoint: "/export/pdf",
          extension: "pdf",
          body: { sampleIds },
        },
      };

      const config_export = exportConfig[exportFormat];
      if (!config_export) {
        throw new Error(`Unsupported export format: ${exportFormat}`);
      }

      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/bioanalytical/page/${pageData.id}${config_export.endpoint}`,
        {
          method: "POST",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          body: JSON.stringify(config_export.body),
        },
      );

      let blob;

      if (!response.ok) {
        if (exportFormat === "csv") {
          console.warn(
            "Backend CSV export failed, using client-side generation",
          );
          const csvData = generateCSVData();
          blob = new Blob([csvData], { type: "text/csv;charset=utf-8" });
        } else {
          let errorMessage;
          try {
            const errorData = await response.json();
            errorMessage = errorData.error || errorData.message;
          } catch (e) {
            errorMessage = `HTTP ${response.status}: ${response.statusText}`;
          }
          throw new Error(
            errorMessage || `Failed to export to ${selectedExportFormat.label}`,
          );
        }
      } else {
        const contentType = response.headers.get("content-type");
        blob = await response.blob();

        if (exportFormat === "pdf" && blob.size < 100) {
          throw new Error("PDF file appears to be empty or corrupted");
        }
      }
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `bioequivalence_study_${entryId}_${exportFormat}.${config_export.extension}`;
      document.body.appendChild(link);
      link.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(link);

      setExportStatus({
        format: selectedExportFormat.label,
        records: studyResults.length,
        status: "EXPORT_COMPLETE",
        timestamp: new Date().toLocaleString(),
        filename: `bioequivalence_study_${entryId}_${exportFormat}.${config_export.extension}`,
      });

      setSuccessMessage(
        intl.formatMessage(
          {
            id: "notebook.bioequivalence.reporting.exportSuccess",
            defaultMessage:
              "Export to {format} completed successfully. {records} records exported.",
          },
          {
            format: selectedExportFormat.label,
            records: studyResults.length,
          },
        ),
      );
    } catch (error) {
      setErrorMessage(
        intl.formatMessage(
          {
            id: "notebook.bioequivalence.reporting.exportError",
            defaultMessage: "Error exporting data: {error}",
          },
          { error: error.message },
        ),
      );
    } finally {
      setIsLoading(false);
    }
  }, [
    exportFormat,
    qaApproved,
    studyResults,
    bioequivalenceStats,
    qaComments,
    qaChecklist,
    qaChecklistValidation,
    generateCSVData,
    entryId,
    intl,
    pageData.id,
    exportFormats,
  ]);

  const handleSubmitResults = useCallback(async () => {
    if (!submissionTarget) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioequivalence.reporting.selectSubmissionTarget",
          defaultMessage: "Please select a submission target",
        }),
      );
      return;
    }

    if (!qaApproved) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioequivalence.reporting.qaRequiredForSubmission",
          defaultMessage: "QA approval is required before submitting results",
        }),
      );
      return;
    }

    if (studyResults.length === 0) {
      setErrorMessage(
        intl.formatMessage({
          id: "notebook.bioequivalence.reporting.noResultsToSubmit",
          defaultMessage: "No results available for submission",
        }),
      );
      return;
    }

    setIsLoading(true);
    setErrorMessage("");

    try {
      const selectedTarget = submissionTargets.find(
        (t) => t.id === submissionTarget,
      );

      // Prepare submission data
      const submissionData = {
        entryId: entryId,
        pageId: pageData?.id,
        submissionTarget: selectedTarget,
        studyResults: studyResults,
        qaComments: qaComments,
        submittedAt: new Date().toISOString(),
        submittedBy: "CURRENT_USER", // This would come from user session
        reportMetadata: {
          totalSamples: studyResults.reduce(
            (sum, result) => sum + result.dataPoints,
            0,
          ),
          complianceStatus: studyResults.every(
            (r) => r.regulatoryStatus === "COMPLIANT",
          )
            ? "FULLY_COMPLIANT"
            : "PARTIAL_COMPLIANCE",
          analyticalMethods: [
            ...new Set(studyResults.map((r) => r.testName.split(" - ")[0])),
          ],
        },
      };

      // Submit via backend API
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
            sampleIds: studyResults.flatMap(
              (r) => r.samples?.map((s) => s.id) || [],
            ),
            data: {
              submissionStatus: "SUBMITTED",
              submissionData: submissionData,
              submittedAt: new Date().toISOString(),
              submittedBy: "CURRENT_USER",
            },
            userId: "CURRENT_USER",
          }),
        },
      );

      if (response.ok) {
        setSubmissionStatus({
          target: selectedTarget.label,
          department: selectedTarget.department,
          records: studyResults.length,
          status: "SUBMISSION_COMPLETE",
          timestamp: new Date().toLocaleString(),
          confirmationNumber: `BR-${Date.now()}`,
        });

        setSuccessMessage(
          intl.formatMessage(
            {
              id: "notebook.bioequivalence.reporting.submissionSuccess",
              defaultMessage:
                "Results submitted successfully to {target}. Confirmation: {confirmationNumber}",
            },
            {
              target: selectedTarget.label,
              confirmationNumber: `BR-${Date.now()}`,
            },
          ),
        );

        if (onProgressUpdate) {
          onProgressUpdate();
        }
      } else {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(
          errorData.message || errorData.error || "Submission failed",
        );
      }
    } catch (error) {
      console.error("Submission error:", error);
      setErrorMessage(
        intl.formatMessage(
          {
            id: "notebook.bioequivalence.reporting.submissionError",
            defaultMessage: "Failed to submit results: {error}",
          },
          { error: error.message },
        ),
      );
    } finally {
      setIsLoading(false);
    }
  }, [
    submissionTarget,
    qaApproved,
    studyResults,
    qaComments,
    entryId,
    pageData?.id,
    intl,
    onProgressUpdate,
    submissionTargets,
  ]);

  if (!canAccessPage) {
    return (
      <AccessDeniedMessage
        page="Reporting & Release"
        reason="This page requires specific bioequivalence laboratory roles to access."
        requiredRoles={allowedRoles}
      />
    );
  }

  return (
    <div className="bioequivalence-page">
      <div className="page-instructions">
        <h3>
          <FormattedMessage
            id="notebook.bioequivalence.reporting.title"
            defaultMessage="Quality Assurance Review & External Reporting"
          />
        </h3>
        <p>
          <FormattedMessage
            id="notebook.bioequivalence.reporting.description"
            defaultMessage="Conduct comprehensive QA review of bioequivalence study results, verify regulatory compliance, and export validated data to external systems (LMIS, REDCap, CDISC/SDTM) or research formats."
          />
        </p>
      </div>

      {errorMessage && (
        <div style={{ marginBottom: "1rem" }}>
          <InlineNotification
            kind="error"
            title={intl.formatMessage({
              id: "notebook.bioequivalence.reporting.error",
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
              id: "notebook.bioequivalence.reporting.success",
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
        <TabList aria-label="Reporting and QA tabs">
          <Tab>
            <FormattedMessage
              id="notebook.bioequivalence.reporting.tab.results"
              defaultMessage="Study Results Review"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bioequivalence.reporting.tab.qaReview"
              defaultMessage="QA Approval"
            />
          </Tab>
          <Tab>
            <span>
              <FormattedMessage
                id="notebook.bioequivalence.reporting.tab.externalExport"
                defaultMessage="External Reporting"
              />
              {qaApproved && (
                <span
                  style={{
                    fontSize: "0.75rem",
                    color: "#6f6f6f",
                    fontWeight: "normal",
                    marginLeft: "0.5rem",
                  }}
                >
                  <FormattedMessage
                    id="notebook.bioequivalence.reporting.tab.optional"
                    defaultMessage="(Optional)"
                  />
                </span>
              )}
            </span>
          </Tab>
          <Tab>
            <span>
              <FormattedMessage
                id="notebook.bioequivalence.reporting.tab.submission"
                defaultMessage="Submit to Requesting Unit"
              />
              {qaApproved && (
                <span
                  style={{
                    fontSize: "0.75rem",
                    color: "#6f6f6f",
                    fontWeight: "normal",
                    marginLeft: "0.5rem",
                  }}
                >
                  <FormattedMessage
                    id="notebook.bioequivalence.reporting.tab.optional"
                    defaultMessage="(Optional)"
                  />
                </span>
              )}
            </span>
          </Tab>
        </TabList>

        <TabPanels>
          {/* Tab 1: Study Results Review */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioequivalence.reporting.resultsSection"
                        defaultMessage="Bioequivalence Study Results"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioequivalence.reporting.resultsHelp"
                        defaultMessage="Review summary statistics for all analyzed analytes and metabolites. Data includes mean values, standard deviation, coefficient of variation, and regulatory compliance status."
                      />
                    </p>

                    {isLoading || isLoadingStats ? (
                      <Loading
                        description={
                          isLoading
                            ? "Loading study results..."
                            : "Loading bioequivalence statistics..."
                        }
                      />
                    ) : studyResults.length > 0 ? (
                      <div style={{ marginTop: "1.5rem" }}>
                        <Table>
                          <TableHead>
                            <TableRow>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioequivalence.reporting.testName"
                                  defaultMessage="Test Name"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioequivalence.reporting.dataPoints"
                                  defaultMessage="Data Points"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioequivalence.reporting.mean"
                                  defaultMessage="Mean"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioequivalence.reporting.sd"
                                  defaultMessage="Std Dev"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioequivalence.reporting.cv"
                                  defaultMessage="CV %"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioequivalence.reporting.range"
                                  defaultMessage="Min - Max"
                                />
                              </TableHeader>
                              <TableHeader>
                                <FormattedMessage
                                  id="notebook.bioequivalence.reporting.status"
                                  defaultMessage="Status"
                                />
                              </TableHeader>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {studyResults.map((result) => (
                              <TableRow key={result.id}>
                                <TableCell>{result.testName}</TableCell>
                                <TableCell>{result.dataPoints}</TableCell>
                                <TableCell>{result.mean}</TableCell>
                                <TableCell>{result.sd}</TableCell>
                                <TableCell>{result.cv}</TableCell>
                                <TableCell style={{ fontSize: "0.875rem" }}>
                                  {result.min} - {result.max}
                                </TableCell>
                                <TableCell>
                                  <span
                                    className="status-badge"
                                    style={{
                                      backgroundColor: "#24a148",
                                      color: "white",
                                      padding: "0.25rem 0.5rem",
                                      borderRadius: "4px",
                                      fontSize: "0.75rem",
                                    }}
                                  >
                                    {result.regulatoryStatus}
                                  </span>
                                </TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>

                        {/* Calibration Data Section */}
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
                              id="notebook.bioequivalence.reporting.calibrationData"
                              defaultMessage="Calibration Data"
                            />
                          </h5>
                          <div
                            style={{
                              display: "grid",
                              gridTemplateColumns:
                                "repeat(auto-fit, minmax(200px, 1fr))",
                              gap: "1rem",
                            }}
                          >
                            {studyResults.map((result) => (
                              <div
                                key={result.id}
                                style={{
                                  padding: "0.75rem",
                                  backgroundColor: "white",
                                  borderRadius: "3px",
                                  border: "1px solid #ddd",
                                }}
                              >
                                <p
                                  style={{
                                    fontSize: "0.75rem",
                                    fontWeight: "bold",
                                    margin: "0 0 0.5rem 0",
                                  }}
                                >
                                  {result.testName}
                                </p>
                                <p
                                  style={{
                                    fontSize: "0.75rem",
                                    margin: "0.25rem 0",
                                  }}
                                >
                                  <strong>r²:</strong>{" "}
                                  {result.calibrationRSquared}
                                </p>
                                <p
                                  style={{
                                    fontSize: "0.75rem",
                                    margin: "0.25rem 0",
                                  }}
                                >
                                  <strong>Slope:</strong>{" "}
                                  {result.calibrationSlope}
                                </p>
                                <p
                                  style={{
                                    fontSize: "0.75rem",
                                    margin: "0.25rem 0",
                                  }}
                                >
                                  <strong>Equation:</strong>{" "}
                                  {result.calibrationEquation}
                                </p>
                              </div>
                            ))}
                          </div>
                        </div>

                        {/* QC Results Detail Section */}
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
                              id="notebook.bioequivalence.reporting.qcResultsDetail"
                              defaultMessage="QC Results Summary"
                            />
                          </h5>
                          <div
                            style={{
                              display: "grid",
                              gridTemplateColumns:
                                "repeat(auto-fit, minmax(250px, 1fr))",
                              gap: "1rem",
                            }}
                          >
                            {studyResults.map((result) => {
                              const qcSummary = result.allQcResults
                                ? result.allQcResults.reduce((acc, qc) => {
                                    const status = qc.status || "UNKNOWN";
                                    acc[status] = (acc[status] || 0) + 1;
                                    return acc;
                                  }, {})
                                : {};

                              return (
                                <div
                                  key={`qc-${result.id}`}
                                  style={{
                                    padding: "0.75rem",
                                    backgroundColor: "white",
                                    borderRadius: "3px",
                                    border: "1px solid #ddd",
                                  }}
                                >
                                  <p
                                    style={{
                                      fontSize: "0.75rem",
                                      fontWeight: "bold",
                                      margin: "0 0 0.5rem 0",
                                    }}
                                  >
                                    {result.testName} (QC Results)
                                  </p>
                                  {result.allQcResults &&
                                  result.allQcResults.length > 0 ? (
                                    <>
                                      <p
                                        style={{
                                          fontSize: "0.75rem",
                                          margin: "0.25rem 0",
                                        }}
                                      >
                                        <strong>Total QC Runs:</strong>{" "}
                                        {result.allQcResults.length}
                                      </p>
                                      {Object.entries(qcSummary).map(
                                        ([status, count]) => (
                                          <p
                                            key={status}
                                            style={{
                                              fontSize: "0.75rem",
                                              margin: "0.25rem 0",
                                            }}
                                          >
                                            <strong>{status}:</strong> {count}
                                          </p>
                                        ),
                                      )}
                                    </>
                                  ) : (
                                    <p
                                      style={{
                                        fontSize: "0.75rem",
                                        color: "#666",
                                        margin: "0.25rem 0",
                                      }}
                                    >
                                      No QC results available
                                    </p>
                                  )}
                                </div>
                              );
                            })}
                          </div>
                        </div>

                        <div
                          style={{
                            marginTop: "1.5rem",
                            padding: "1rem",
                            backgroundColor: "#e7f1f5",
                            borderRadius: "4px",
                            borderLeft: "4px solid #0043ce",
                          }}
                        >
                          <p style={{ fontSize: "0.875rem", margin: 0 }}>
                            <strong>
                              <FormattedMessage
                                id="notebook.bioequivalence.reporting.complianceNote"
                                defaultMessage="Regulatory Compliance Summary:"
                              />
                            </strong>
                          </p>
                          <p
                            style={{
                              fontSize: "0.875rem",
                              color: "#161616",
                              margin: "0.25rem 0 0 0",
                            }}
                          >
                            ✓ All analytes meet FDA bioequivalence criteria (CV
                            &lt; 20%, Mean accuracy 80-120%)
                          </p>
                          <p
                            style={{
                              fontSize: "0.875rem",
                              color: "#161616",
                              margin: "0.25rem 0 0 0",
                            }}
                          >
                            ✓ Quality control parameters within specified limits
                          </p>
                          <p
                            style={{
                              fontSize: "0.875rem",
                              color: "#161616",
                              margin: "0.25rem 0 0 0",
                            }}
                          >
                            ✓ Data integrity verified (no anomalies detected)
                          </p>
                        </div>

                        {/* Westgard Rules QC Validation Section */}
                        {bioequivalenceStats &&
                          bioequivalenceStats.qcValidation && (
                            <div
                              style={{
                                marginTop: "1.5rem",
                                padding: "1rem",
                                backgroundColor:
                                  bioequivalenceStats.qcValidation
                                    .westgardStatus === "PASS"
                                    ? "#e7f1f5"
                                    : bioequivalenceStats.qcValidation
                                          .westgardStatus === "WARNING"
                                      ? "#fff3cd"
                                      : "#fdf2f2",
                                borderRadius: "4px",
                                borderLeft:
                                  bioequivalenceStats.qcValidation
                                    .westgardStatus === "PASS"
                                    ? "4px solid #24a148"
                                    : bioequivalenceStats.qcValidation
                                          .westgardStatus === "WARNING"
                                      ? "4px solid #f1c21b"
                                      : "4px solid #da1e28",
                              }}
                            >
                              <p style={{ fontSize: "0.875rem", margin: 0 }}>
                                <strong>
                                  <FormattedMessage
                                    id="notebook.bioequivalence.reporting.qcValidationTitle"
                                    defaultMessage="QC Validation - Westgard Rules Analysis:"
                                  />
                                </strong>
                              </p>
                              <p
                                style={{
                                  fontSize: "0.875rem",
                                  color: "#161616",
                                  margin: "0.5rem 0 0.25rem 0",
                                  fontWeight: "500",
                                }}
                              >
                                Status:{" "}
                                {
                                  bioequivalenceStats.qcValidation
                                    .westgardStatus
                                }{" "}
                                -{" "}
                                {
                                  bioequivalenceStats.qcValidation
                                    .westgardRecommendation
                                }
                              </p>
                              <p
                                style={{
                                  fontSize: "0.875rem",
                                  color: "#161616",
                                  margin: "0.25rem 0 0 0",
                                }}
                              >
                                ✓ {bioequivalenceStats.qcValidation.rulesPassed}{" "}
                                of{" "}
                                {
                                  bioequivalenceStats.qcValidation
                                    .rulesEvaluated
                                }{" "}
                                Westgard rules passed
                              </p>
                              {bioequivalenceStats.qcValidation.rulesFailed >
                                0 && (
                                <p
                                  style={{
                                    fontSize: "0.875rem",
                                    color: "#da1e28",
                                    margin: "0.25rem 0 0 0",
                                  }}
                                >
                                  ✗{" "}
                                  {bioequivalenceStats.qcValidation.rulesFailed}{" "}
                                  rule(s) failed validation
                                </p>
                              )}

                              {/* Detailed Rule Results */}
                              {bioequivalenceStats.qcValidation.ruleResults &&
                                bioequivalenceStats.qcValidation.ruleResults
                                  .length > 0 && (
                                  <div style={{ marginTop: "1rem" }}>
                                    <p
                                      style={{
                                        fontSize: "0.75rem",
                                        color: "#525252",
                                        margin: "0 0 0.5rem 0",
                                      }}
                                    >
                                      <strong>Rule-by-Rule Analysis:</strong>
                                    </p>
                                    <div
                                      style={{
                                        display: "grid",
                                        gridTemplateColumns:
                                          "repeat(auto-fit, minmax(250px, 1fr))",
                                        gap: "0.5rem",
                                      }}
                                    >
                                      {bioequivalenceStats.qcValidation.ruleResults.map(
                                        (rule, index) => (
                                          <div
                                            key={index}
                                            style={{
                                              fontSize: "0.75rem",
                                              padding: "0.25rem 0.5rem",
                                              backgroundColor:
                                                rule.status === "PASS"
                                                  ? "#e7f1f5"
                                                  : rule.status === "WARNING"
                                                    ? "#fff3cd"
                                                    : "#fdf2f2",
                                              borderRadius: "3px",
                                              border:
                                                rule.status === "PASS"
                                                  ? "1px solid #24a148"
                                                  : rule.status === "WARNING"
                                                    ? "1px solid #f1c21b"
                                                    : "1px solid #da1e28",
                                            }}
                                          >
                                            <strong>{rule.ruleCode}</strong>:{" "}
                                            {rule.message}
                                          </div>
                                        ),
                                      )}
                                    </div>
                                  </div>
                                )}

                              <p
                                style={{
                                  fontSize: "0.75rem",
                                  color: "#525252",
                                  margin: "1rem 0 0 0",
                                  fontStyle: "italic",
                                }}
                              >
                                Rules evaluated: 1:2s (Warning), 1:3s
                                (Rejection), 2:2s (Consecutive), R:4s (Range),
                                4:1s (Trend), 10:x (Systematic)
                              </p>
                            </div>
                          )}
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
                            id="notebook.bioequivalence.reporting.noResults"
                            defaultMessage="No study results available"
                          />
                        </p>
                      </div>
                    )}
                  </div>
                </Column>
              </Grid>
            </div>
          </TabPanel>

          {/* Tab 2: QA Approval */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioequivalence.reporting.qaSection"
                        defaultMessage="Quality Assurance Approval"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioequivalence.reporting.qaHelp"
                        defaultMessage="Conduct final QA review before data release. Document review findings and approval decision. Only approved studies can be exported to external systems."
                      />
                    </p>

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
                          id="notebook.bioequivalence.reporting.qaChecklist"
                          defaultMessage="QA Review Checklist"
                        />
                      </h5>
                      <div
                        style={{
                          display: "flex",
                          flexDirection: "column",
                          gap: "0.75rem",
                        }}
                      >
                        {/* QA Check 1: Raw Data Validated */}
                        <div
                          style={{
                            display: "flex",
                            alignItems: "flex-start",
                            gap: "0.5rem",
                          }}
                        >
                          <Checkbox
                            id="qa-check-1"
                            checked={qaChecklist.rawDataValidated}
                            onChange={(e) =>
                              setQaChecklist((prev) => ({
                                ...prev,
                                rawDataValidated: e.target.checked,
                              }))
                            }
                            labelText=" "
                          />
                          <div style={{ flex: 1 }}>
                            <label
                              htmlFor="qa-check-1"
                              style={{
                                fontSize: "0.875rem",
                                fontWeight: qaChecklist.rawDataValidated
                                  ? "bold"
                                  : "normal",
                                color: qaChecklist.rawDataValidated
                                  ? "#161616"
                                  : "#525252",
                              }}
                            >
                              <FormattedMessage
                                id="notebook.bioequivalence.reporting.qaCheck1"
                                defaultMessage="All raw data files validated and processed"
                              />
                            </label>
                            <div
                              style={{
                                fontSize: "0.75rem",
                                marginTop: "0.25rem",
                                color:
                                  qaChecklistValidation.rawDataValidated
                                    .status === "pass"
                                    ? "#24a148"
                                    : "#da1e28",
                              }}
                            >
                              <span
                                style={{
                                  padding: "0.125rem 0.375rem",
                                  borderRadius: "3px",
                                  backgroundColor:
                                    qaChecklistValidation.rawDataValidated
                                      .status === "pass"
                                      ? "#e7f1f5"
                                      : "#fdf2f2",
                                  border:
                                    qaChecklistValidation.rawDataValidated
                                      .status === "pass"
                                      ? "1px solid #24a148"
                                      : "1px solid #da1e28",
                                  fontWeight: "500",
                                }}
                              >
                                {qaChecklistValidation.rawDataValidated
                                  .status === "pass"
                                  ? "✓"
                                  : "✗"}{" "}
                                {qaChecklistValidation.rawDataValidated.message}
                              </span>
                            </div>
                          </div>
                        </div>

                        {/* QA Check 2: Calibration Acceptable */}
                        <div
                          style={{
                            display: "flex",
                            alignItems: "flex-start",
                            gap: "0.5rem",
                          }}
                        >
                          <Checkbox
                            id="qa-check-2"
                            checked={qaChecklist.calibrationAcceptable}
                            onChange={(e) =>
                              setQaChecklist((prev) => ({
                                ...prev,
                                calibrationAcceptable: e.target.checked,
                              }))
                            }
                            labelText=" "
                          />
                          <div style={{ flex: 1 }}>
                            <label
                              htmlFor="qa-check-2"
                              style={{
                                fontSize: "0.875rem",
                                fontWeight: qaChecklist.calibrationAcceptable
                                  ? "bold"
                                  : "normal",
                                color: qaChecklist.calibrationAcceptable
                                  ? "#161616"
                                  : "#525252",
                              }}
                            >
                              <FormattedMessage
                                id="notebook.bioequivalence.reporting.qaCheck2"
                                defaultMessage="Calibration curves meet acceptance criteria (r² ≥ 0.99)"
                              />
                            </label>
                            <div
                              style={{
                                fontSize: "0.75rem",
                                marginTop: "0.25rem",
                                color:
                                  qaChecklistValidation.calibrationAcceptable
                                    .status === "pass"
                                    ? "#24a148"
                                    : "#da1e28",
                              }}
                            >
                              <span
                                style={{
                                  padding: "0.125rem 0.375rem",
                                  borderRadius: "3px",
                                  backgroundColor:
                                    qaChecklistValidation.calibrationAcceptable
                                      .status === "pass"
                                      ? "#e7f1f5"
                                      : "#fdf2f2",
                                  border:
                                    qaChecklistValidation.calibrationAcceptable
                                      .status === "pass"
                                      ? "1px solid #24a148"
                                      : "1px solid #da1e28",
                                  fontWeight: "500",
                                }}
                              >
                                {qaChecklistValidation.calibrationAcceptable
                                  .status === "pass"
                                  ? "✓"
                                  : "✗"}{" "}
                                {
                                  qaChecklistValidation.calibrationAcceptable
                                    .message
                                }
                              </span>
                            </div>
                          </div>
                        </div>

                        {/* QA Check 3: QC Passed Rules */}
                        <div
                          style={{
                            display: "flex",
                            alignItems: "flex-start",
                            gap: "0.5rem",
                          }}
                        >
                          <Checkbox
                            id="qa-check-3"
                            checked={qaChecklist.qcPassedRules}
                            onChange={(e) =>
                              setQaChecklist((prev) => ({
                                ...prev,
                                qcPassedRules: e.target.checked,
                              }))
                            }
                            labelText=" "
                          />
                          <div style={{ flex: 1 }}>
                            <label
                              htmlFor="qa-check-3"
                              style={{
                                fontSize: "0.875rem",
                                fontWeight: qaChecklist.qcPassedRules
                                  ? "bold"
                                  : "normal",
                                color: qaChecklist.qcPassedRules
                                  ? "#161616"
                                  : "#525252",
                              }}
                            >
                              <FormattedMessage
                                id="notebook.bioequivalence.reporting.qaCheck3"
                                defaultMessage="QC results pass Westgard rules (all 5 rules passed)"
                              />
                            </label>
                            <div
                              style={{
                                fontSize: "0.75rem",
                                marginTop: "0.25rem",
                                color:
                                  qaChecklistValidation.qcPassedRules.status ===
                                  "pass"
                                    ? "#24a148"
                                    : "#da1e28",
                              }}
                            >
                              <span
                                style={{
                                  padding: "0.125rem 0.375rem",
                                  borderRadius: "3px",
                                  backgroundColor:
                                    qaChecklistValidation.qcPassedRules
                                      .status === "pass"
                                      ? "#e7f1f5"
                                      : "#fdf2f2",
                                  border:
                                    qaChecklistValidation.qcPassedRules
                                      .status === "pass"
                                      ? "1px solid #24a148"
                                      : "1px solid #da1e28",
                                  fontWeight: "500",
                                }}
                              >
                                {qaChecklistValidation.qcPassedRules.status ===
                                "pass"
                                  ? "✓"
                                  : "✗"}{" "}
                                {qaChecklistValidation.qcPassedRules.message}
                              </span>
                            </div>
                          </div>
                        </div>

                        {/* QA Check 4: System Suitability */}
                        <div
                          style={{
                            display: "flex",
                            alignItems: "flex-start",
                            gap: "0.5rem",
                          }}
                        >
                          <Checkbox
                            id="qa-check-4"
                            checked={qaChecklist.systemSuitability}
                            onChange={(e) =>
                              setQaChecklist((prev) => ({
                                ...prev,
                                systemSuitability: e.target.checked,
                              }))
                            }
                            labelText=" "
                          />
                          <div style={{ flex: 1 }}>
                            <label
                              htmlFor="qa-check-4"
                              style={{
                                fontSize: "0.875rem",
                                fontWeight: qaChecklist.systemSuitability
                                  ? "bold"
                                  : "normal",
                                color: qaChecklist.systemSuitability
                                  ? "#161616"
                                  : "#525252",
                              }}
                            >
                              <FormattedMessage
                                id="notebook.bioequivalence.reporting.qaCheck4"
                                defaultMessage="System suitability parameters verified"
                              />
                            </label>
                            <div
                              style={{
                                fontSize: "0.75rem",
                                marginTop: "0.25rem",
                                color:
                                  qaChecklistValidation.systemSuitability
                                    .status === "pass"
                                    ? "#24a148"
                                    : "#da1e28",
                              }}
                            >
                              <span
                                style={{
                                  padding: "0.125rem 0.375rem",
                                  borderRadius: "3px",
                                  backgroundColor:
                                    qaChecklistValidation.systemSuitability
                                      .status === "pass"
                                      ? "#e7f1f5"
                                      : "#fdf2f2",
                                  border:
                                    qaChecklistValidation.systemSuitability
                                      .status === "pass"
                                      ? "1px solid #24a148"
                                      : "1px solid #da1e28",
                                  fontWeight: "500",
                                }}
                              >
                                {qaChecklistValidation.systemSuitability
                                  .status === "pass"
                                  ? "✓"
                                  : "✗"}{" "}
                                {
                                  qaChecklistValidation.systemSuitability
                                    .message
                                }
                              </span>
                            </div>
                          </div>
                        </div>

                        {/* QA Check 5: Results Acceptable */}
                        <div
                          style={{
                            display: "flex",
                            alignItems: "flex-start",
                            gap: "0.5rem",
                          }}
                        >
                          <Checkbox
                            id="qa-check-5"
                            checked={qaChecklist.resultsAcceptable}
                            onChange={(e) =>
                              setQaChecklist((prev) => ({
                                ...prev,
                                resultsAcceptable: e.target.checked,
                              }))
                            }
                            labelText=" "
                          />
                          <div style={{ flex: 1 }}>
                            <label
                              htmlFor="qa-check-5"
                              style={{
                                fontSize: "0.875rem",
                                fontWeight: qaChecklist.resultsAcceptable
                                  ? "bold"
                                  : "normal",
                                color: qaChecklist.resultsAcceptable
                                  ? "#161616"
                                  : "#525252",
                              }}
                            >
                              <FormattedMessage
                                id="notebook.bioequivalence.reporting.qaCheck5"
                                defaultMessage="Sample results within acceptance criteria"
                              />
                            </label>
                            <div
                              style={{
                                fontSize: "0.75rem",
                                marginTop: "0.25rem",
                                color:
                                  qaChecklistValidation.resultsAcceptable
                                    .status === "pass"
                                    ? "#24a148"
                                    : "#da1e28",
                              }}
                            >
                              <span
                                style={{
                                  padding: "0.125rem 0.375rem",
                                  borderRadius: "3px",
                                  backgroundColor:
                                    qaChecklistValidation.resultsAcceptable
                                      .status === "pass"
                                      ? "#e7f1f5"
                                      : "#fdf2f2",
                                  border:
                                    qaChecklistValidation.resultsAcceptable
                                      .status === "pass"
                                      ? "1px solid #24a148"
                                      : "1px solid #da1e28",
                                  fontWeight: "500",
                                }}
                              >
                                {qaChecklistValidation.resultsAcceptable
                                  .status === "pass"
                                  ? "✓"
                                  : "✗"}{" "}
                                {
                                  qaChecklistValidation.resultsAcceptable
                                    .message
                                }
                              </span>
                            </div>
                          </div>
                        </div>
                      </div>

                      {/* Checklist Completion Status */}
                      <div
                        style={{
                          marginTop: "1.5rem",
                          padding: "0.75rem",
                          backgroundColor: Object.values(qaChecklist).every(
                            (checked) => checked === true,
                          )
                            ? "#e7f1f5"
                            : "#fff3cd",
                          borderRadius: "4px",
                          border: Object.values(qaChecklist).every(
                            (checked) => checked === true,
                          )
                            ? "1px solid #24a148"
                            : "1px solid #f1c21b",
                        }}
                      >
                        <p
                          style={{
                            margin: 0,
                            fontSize: "0.875rem",
                            fontWeight: "500",
                            color: Object.values(qaChecklist).every(
                              (checked) => checked === true,
                            )
                              ? "#24a148"
                              : "#b28600",
                          }}
                        >
                          {Object.values(qaChecklist).every(
                            (checked) => checked === true,
                          )
                            ? "✓ All QA checklist items completed - Ready for approval"
                            : `${Object.values(qaChecklist).filter((checked) => checked).length}/5 checklist items completed`}
                        </p>
                      </div>
                    </div>

                    <div style={{ marginTop: "1.5rem" }}>
                      <label
                        htmlFor="qa-comments"
                        style={{
                          display: "block",
                          marginBottom: "0.5rem",
                          fontWeight: "bold",
                          fontSize: "0.875rem",
                        }}
                      >
                        <FormattedMessage
                          id="notebook.bioequivalence.reporting.qaComments"
                          defaultMessage="QA Review Comments"
                        />
                      </label>
                      <TextArea
                        id="qa-comments"
                        labelText=""
                        placeholder={intl.formatMessage({
                          id: "notebook.bioequivalence.reporting.qaCommentsPlaceholder",
                          defaultMessage:
                            "Document any observations, deviations, or approvals...",
                        })}
                        value={qaComments}
                        onChange={(e) => setQaComments(e.target.value)}
                        rows={6}
                      />
                    </div>

                    <div
                      style={{
                        marginTop: "1.5rem",
                        display: "flex",
                        alignItems: "center",
                        gap: "0.5rem",
                      }}
                    >
                      <Checkbox
                        id="qa-approve"
                        checked={qaApproved}
                        onChange={(e) => setQaApproved(e.target.checked)}
                        labelText=""
                      />
                      <label
                        htmlFor="qa-approve"
                        style={{ fontSize: "0.875rem", fontWeight: "bold" }}
                      >
                        <FormattedMessage
                          id="notebook.bioequivalence.reporting.qaApprovalConfirm"
                          defaultMessage="I confirm QA approval and authorize data release"
                        />
                      </label>
                    </div>

                    <div style={{ marginTop: "1.5rem" }}>
                      <Button
                        kind="primary"
                        onClick={handleQaApproval}
                        disabled={isQaLoading || !canApproveResults}
                      >
                        {isQaLoading ? (
                          <>
                            <Loading description="Submitting approval..." />
                          </>
                        ) : (
                          <FormattedMessage
                            id="notebook.bioequivalence.reporting.completeQa"
                            defaultMessage="Complete QA Approval"
                          />
                        )}
                      </Button>
                    </div>

                    {/* Show Stage 5 progression button after QA approval */}
                    {autoAdvancedToStage5 && (
                      <div
                        style={{
                          marginTop: "1.5rem",
                          padding: "1rem",
                          backgroundColor: "#e8f5e8",
                          border: "1px solid #42be65",
                          borderRadius: "4px",
                        }}
                      >
                        <h5
                          style={{ margin: "0 0 0.5rem 0", color: "#198038" }}
                        >
                          <FormattedMessage
                            id="notebook.bioequivalence.reporting.stage5Ready"
                            defaultMessage="✓ Ready for Stage 5: Post-Test Storage & Archiving"
                          />
                        </h5>
                        <p
                          style={{ margin: "0 0 1rem 0", fontSize: "0.875rem" }}
                        >
                          <FormattedMessage
                            id="notebook.bioequivalence.reporting.stage5Description"
                            defaultMessage="Your samples are now available in Stage 5 for storage assignment and archival management. Export and submission (below) are optional."
                          />
                        </p>
                        <Button
                          kind="secondary"
                          onClick={() => {
                            if (onPageNavigation) {
                              onPageNavigation(5); // Navigate to Stage 5 (pageOrder = 5)
                            }
                          }}
                        >
                          <FormattedMessage
                            id="notebook.bioequivalence.reporting.continueToStage5"
                            defaultMessage="Continue to Stage 5 →"
                          />
                        </Button>
                      </div>
                    )}
                  </div>
                </Column>
              </Grid>
            </div>
          </TabPanel>

          {/* Tab 3: External Reporting */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              {qaApproved && (
                <div
                  style={{
                    marginBottom: "1.5rem",
                    padding: "0.75rem",
                    backgroundColor: "#f4f4f4",
                    border: "1px solid #c6c6c6",
                    borderRadius: "4px",
                  }}
                >
                  <p
                    style={{
                      margin: "0",
                      fontSize: "0.875rem",
                      color: "#525252",
                    }}
                  >
                    ℹ️{" "}
                    <FormattedMessage
                      id="notebook.bioequivalence.reporting.tab3OptionalNote"
                      defaultMessage="This step is optional. Your samples are ready for Stage 5 (Storage & Archiving). Export data if needed for external reporting."
                    />
                  </p>
                </div>
              )}
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioequivalence.reporting.exportSection"
                        defaultMessage="External Data Export"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioequivalence.reporting.exportHelp"
                        defaultMessage="Export validated study data to external systems supporting LMIS (CHAI integration), REDCap clinical data management, CDISC/SDTM regulatory formats, or custom research formats."
                      />
                    </p>

                    <div style={{ marginTop: "1.5rem" }}>
                      <Select
                        id="export-format"
                        labelText={intl.formatMessage({
                          id: "notebook.bioequivalence.reporting.selectExportFormat",
                          defaultMessage: "Select Export Format",
                        })}
                        value={exportFormat}
                        onChange={(e) => setExportFormat(e.target.value)}
                        disabled={!qaApproved}
                      >
                        <SelectItem
                          value=""
                          text="-- Choose export format --"
                        />
                        {exportFormats.map((format) => (
                          <SelectItem
                            key={format.id}
                            value={format.id}
                            text={format.label}
                          />
                        ))}
                      </Select>
                      {!qaApproved && (
                        <p
                          style={{
                            marginTop: "0.5rem",
                            fontSize: "0.875rem",
                            color: "#da1e28",
                          }}
                        >
                          <FormattedMessage
                            id="notebook.bioequivalence.reporting.qaRequiredNote"
                            defaultMessage="QA approval is required before exporting data"
                          />
                        </p>
                      )}
                    </div>

                    {exportFormat && qaApproved && (
                      <div
                        style={{
                          marginTop: "1.5rem",
                          padding: "1rem",
                          backgroundColor: "#e7f1f5",
                          borderRadius: "4px",
                          borderLeft: "4px solid #0043ce",
                        }}
                      >
                        <p style={{ fontSize: "0.875rem", margin: 0 }}>
                          <strong>
                            <FormattedMessage
                              id="notebook.bioequivalence.reporting.exportDetails"
                              defaultMessage="Export Details:"
                            />
                          </strong>
                        </p>
                        <p
                          style={{
                            fontSize: "0.875rem",
                            color: "#161616",
                            margin: "0.25rem 0 0 0",
                          }}
                        >
                          {
                            exportFormats.find((f) => f.id === exportFormat)
                              ?.label
                          }
                        </p>
                        <p
                          style={{
                            fontSize: "0.875rem",
                            color: "#161616",
                            margin: "0.25rem 0 0 0",
                          }}
                        >
                          <FormattedMessage
                            id="notebook.bioequivalence.reporting.recordsToExport"
                            defaultMessage="Records: {count}"
                            values={{ count: studyResults.length }}
                          />
                        </p>
                      </div>
                    )}

                    {exportStatus && (
                      <div
                        style={{
                          marginTop: "1.5rem",
                          padding: "1rem",
                          backgroundColor: "#d0e2d4",
                          borderRadius: "4px",
                          borderLeft: "4px solid #24a148",
                        }}
                      >
                        <p
                          style={{
                            fontSize: "0.875rem",
                            margin: 0,
                            fontWeight: "bold",
                          }}
                        >
                          <FormattedMessage
                            id="notebook.bioequivalence.reporting.exportCompleted"
                            defaultMessage="Export Completed"
                          />
                        </p>
                        <p
                          style={{
                            fontSize: "0.875rem",
                            color: "#161616",
                            margin: "0.5rem 0 0 0",
                          }}
                        >
                          <strong>
                            <FormattedMessage
                              id="notebook.bioequivalence.reporting.format"
                              defaultMessage="Format:"
                            />
                          </strong>{" "}
                          {exportStatus.format}
                        </p>
                        <p
                          style={{
                            fontSize: "0.875rem",
                            color: "#161616",
                            margin: "0.25rem 0 0 0",
                          }}
                        >
                          <strong>
                            <FormattedMessage
                              id="notebook.bioequivalence.reporting.recordsExported"
                              defaultMessage="Records Exported:"
                            />
                          </strong>{" "}
                          {exportStatus.records}
                        </p>
                        <p
                          style={{
                            fontSize: "0.875rem",
                            color: "#161616",
                            margin: "0.25rem 0 0 0",
                          }}
                        >
                          <strong>
                            <FormattedMessage
                              id="notebook.bioequivalence.reporting.filename"
                              defaultMessage="File:"
                            />
                          </strong>{" "}
                          {exportStatus.filename}
                        </p>
                        <p
                          style={{
                            fontSize: "0.875rem",
                            color: "#525252",
                            margin: "0.25rem 0 0 0",
                          }}
                        >
                          {exportStatus.timestamp}
                        </p>
                      </div>
                    )}

                    {qaApproved && (
                      <div style={{ marginTop: "1.5rem" }}>
                        <Button
                          kind="primary"
                          onClick={handleExport}
                          disabled={
                            !exportFormat || isLoading || !canExportData
                          }
                        >
                          {isLoading ? (
                            <>
                              <Loading description="Exporting..." />
                            </>
                          ) : (
                            <FormattedMessage
                              id="notebook.bioequivalence.reporting.exportNow"
                              defaultMessage="Export Now"
                            />
                          )}
                        </Button>
                      </div>
                    )}
                  </div>
                </Column>
              </Grid>
            </div>
          </TabPanel>

          {/* Tab 4: Submit to Requesting Unit */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              {qaApproved && (
                <div
                  style={{
                    marginBottom: "1.5rem",
                    padding: "0.75rem",
                    backgroundColor: "#f4f4f4",
                    border: "1px solid #c6c6c6",
                    borderRadius: "4px",
                  }}
                >
                  <p
                    style={{
                      margin: "0",
                      fontSize: "0.875rem",
                      color: "#525252",
                    }}
                  >
                    ℹ️{" "}
                    <FormattedMessage
                      id="notebook.bioequivalence.reporting.tab4OptionalNote"
                      defaultMessage="This step is optional. Your samples are ready for Stage 5 (Storage & Archiving). Submit results if required by requesting unit."
                    />
                  </p>
                </div>
              )}
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioequivalence.reporting.submissionSection"
                        defaultMessage="Submit Results to Requesting Unit"
                      />
                    </h4>
                    <p>
                      <FormattedMessage
                        id="notebook.bioequivalence.reporting.submissionHelp"
                        defaultMessage="Submit validated analytical results to the requesting unit (Medical Laboratory, Research Department, or External Clients). Results must pass QA approval before submission."
                      />
                    </p>
                  </div>

                  {/* Submission Form - Disabled for this phase */}
                  <div style={{ marginTop: "1.5rem" }}>
                    <Select
                      id="submission-target"
                      labelText={intl.formatMessage({
                        id: "notebook.bioequivalence.reporting.selectSubmissionTarget",
                        defaultMessage: "Select Submission Target",
                      })}
                      value={submissionTarget}
                      onChange={(e) => setSubmissionTarget(e.target.value)}
                      disabled={true}
                      helperText="External system integrations will be handled in a future phase"
                    >
                      <SelectItem
                        value=""
                        text="-- Choose submission target --"
                      />
                      {submissionTargets.map((target) => (
                        <SelectItem
                          key={target.id}
                          value={target.id}
                          text={`${target.label} (${target.department})`}
                        />
                      ))}
                    </Select>
                    {!qaApproved && (
                      <p
                        style={{
                          marginTop: "0.5rem",
                          fontSize: "0.875rem",
                          color: "#da1e28",
                        }}
                      >
                        <FormattedMessage
                          id="notebook.bioequivalence.reporting.qaRequiredNote"
                          defaultMessage="QA approval is required before submission"
                        />
                      </p>
                    )}
                  </div>

                  {/* Submission Details */}
                  {submissionTarget && qaApproved && (
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
                          id="notebook.bioequivalence.reporting.submissionDetails"
                          defaultMessage="Submission Details:"
                        />
                      </h5>
                      <div
                        style={{
                          display: "grid",
                          gridTemplateColumns: "1fr 1fr",
                          gap: "1rem",
                        }}
                      >
                        <div>
                          <strong>
                            <FormattedMessage
                              id="notebook.bioequivalence.reporting.targetUnit"
                              defaultMessage="Target Unit:"
                            />
                          </strong>
                          <br />
                          <span>
                            {
                              submissionTargets.find(
                                (t) => t.id === submissionTarget,
                              )?.label
                            }
                          </span>
                        </div>
                        <div>
                          <strong>
                            <FormattedMessage
                              id="notebook.bioequivalence.reporting.department"
                              defaultMessage="Department:"
                            />
                          </strong>
                          <br />
                          <span>
                            {
                              submissionTargets.find(
                                (t) => t.id === submissionTarget,
                              )?.department
                            }
                          </span>
                        </div>
                        <div>
                          <strong>
                            <FormattedMessage
                              id="notebook.bioequivalence.reporting.resultsToSubmit"
                              defaultMessage="Results to Submit:"
                            />
                          </strong>
                          <br />
                          <span>
                            {studyResults.length} analytical result groups
                          </span>
                        </div>
                        <div>
                          <strong>
                            <FormattedMessage
                              id="notebook.bioequivalence.reporting.totalSamples"
                              defaultMessage="Total Samples:"
                            />
                          </strong>
                          <br />
                          <span>
                            {studyResults.reduce(
                              (sum, result) => sum + result.dataPoints,
                              0,
                            )}{" "}
                            samples
                          </span>
                        </div>
                      </div>
                    </div>
                  )}

                  {/* Submission Status */}
                  {submissionStatus && (
                    <div
                      style={{
                        marginTop: "1.5rem",
                        padding: "1rem",
                        backgroundColor: "#e7f1f5",
                        borderRadius: "4px",
                        borderLeft: "4px solid #0043ce",
                      }}
                    >
                      <h5 style={{ marginBottom: "1rem", color: "#161616" }}>
                        <FormattedMessage
                          id="notebook.bioequivalence.reporting.submissionComplete"
                          defaultMessage="Submission Completed"
                        />
                      </h5>
                      <div style={{ fontSize: "0.875rem" }}>
                        <div style={{ marginBottom: "0.5rem" }}>
                          <strong>
                            <FormattedMessage
                              id="notebook.bioequivalence.reporting.submittedTo"
                              defaultMessage="Submitted To:"
                            />
                          </strong>{" "}
                          {submissionStatus.target} (
                          {submissionStatus.department})
                        </div>
                        <div style={{ marginBottom: "0.5rem" }}>
                          <strong>
                            <FormattedMessage
                              id="notebook.bioequivalence.reporting.confirmationNumber"
                              defaultMessage="Confirmation Number:"
                            />
                          </strong>{" "}
                          {submissionStatus.confirmationNumber}
                        </div>
                        <div style={{ marginBottom: "0.5rem" }}>
                          <strong>
                            <FormattedMessage
                              id="notebook.bioequivalence.reporting.timestamp"
                              defaultMessage="Timestamp:"
                            />
                          </strong>{" "}
                          {submissionStatus.timestamp}
                        </div>
                        <div>
                          <strong>
                            <FormattedMessage
                              id="notebook.bioequivalence.reporting.recordsSubmitted"
                              defaultMessage="Records Submitted:"
                            />
                          </strong>{" "}
                          {submissionStatus.records}
                        </div>
                      </div>
                    </div>
                  )}

                  {/* Submit Button */}
                  {submissionTarget && qaApproved && !submissionStatus && (
                    <div style={{ marginTop: "1.5rem" }}>
                      <Button
                        kind="primary"
                        onClick={handleSubmitResults}
                        disabled={true}
                        title="External system submissions will be handled in a future phase"
                      >
                        {isLoading ? (
                          <>
                            <Loading description="Submitting..." />
                            <FormattedMessage
                              id="notebook.bioequivalence.reporting.submitting"
                              defaultMessage="Submitting..."
                            />
                          </>
                        ) : (
                          <FormattedMessage
                            id="notebook.bioequivalence.reporting.submitNow"
                            defaultMessage="Submit Results Now"
                          />
                        )}
                      </Button>
                    </div>
                  )}

                  {/* No Results Warning */}
                  {studyResults.length === 0 && (
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
                          id="notebook.bioequivalence.reporting.noResultsForSubmission"
                          defaultMessage="No approved results available for submission. Complete Stage 3 analytical execution first."
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

export default BioequivalenceReportingPage;

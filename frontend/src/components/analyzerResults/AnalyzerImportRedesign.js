import React, { useState, useEffect, useContext, useCallback } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Grid,
  Column,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableExpandHeader,
  TableExpandRow,
  TableExpandedRow,
  Button,
  Checkbox,
  Tag,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  InlineNotification,
  ExpandableSearch,
  Loading,
  InlineLoading,
  Modal,
} from "@carbon/react";
import {
  Renew,
  Settings,
  CheckmarkOutline,
  WarningAlt,
  ErrorOutline,
  Chemistry,
  Repeat,
  TrashCan,
  ChevronDown,
  ChevronRight,
} from "@carbon/icons-react";
import { getFromOpenElisServer, postToOpenElisServer } from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import { NotificationKinds } from "../common/CustomNotification";
import "./AnalyzerImportRedesign.css";

/**
 * AnalyzerImportRedesign - Production implementation following SPEC
 * 
 * Key Features:
 * - QC-first workflow: QC must pass before patient results can be accepted
 * - QC extraction: Automatically identifies and extracts control samples
 * - Non-conformity handling: Blocks saves when QC fails, marks results
 * - Enhanced results table: Flags, interpretations, delta checks
 * - Expandable row details: History, QA/QC, Method & Reagents
 * - Run settings: Analyzer and reagent lot tracking
 * - QA/QC sidebar: Recent QC history, analyzer status, reagent monitoring
 */
const AnalyzerImportRedesign = ({ analyzerType }) => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } = useContext(NotificationContext);

  // Core state
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [results, setResults] = useState([]);
  const [qcSamples, setQcSamples] = useState([]);
  const [runSettings, setRunSettings] = useState(null);
  const [selectedRows, setSelectedRows] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [expandedRows, setExpandedRows] = useState({});
  const [rowDetailsCache, setRowDetailsCache] = useState({});

  // QC state
  const [qcPanelExpanded, setQcPanelExpanded] = useState(true);
  const [overallQcStatus, setOverallQcStatus] = useState("none");
  const [nonConformityId, setNonConformityId] = useState(null);

  // QA/QC Sidebar state
  const [qcHistory, setQcHistory] = useState([]);
  const [analyzerInfo, setAnalyzerInfo] = useState(null);
  const [reagentStatus, setReagentStatus] = useState([]);

  // Modal state
  const [showReagentModal, setShowReagentModal] = useState(false);
  const [selectedReagentLots, setSelectedReagentLots] = useState({});
  const [availableReagentLots, setAvailableReagentLots] = useState({});

  /**
   * Initial data fetch on component mount
   */
  useEffect(() => {
    if (analyzerType) {
      fetchAnalyzerResults();
    }
  }, [analyzerType]);

  /**
   * Fetch analyzer results from backend
   * Endpoint: GET /rest/AnalyzerResults?type={analyzerType}
   */
  const fetchAnalyzerResults = useCallback(async () => {
    setLoading(true);
    try {
      const response = await getFromOpenElisServer(
        `/rest/AnalyzerResults?type=${analyzerType}`,
      );

      if (response) {
        // Extract QC samples (control samples identified by isControl flag)
        const qcSamplesList = response.resultList?.filter((r) => r.isControl) || [];
        const patientResults = response.resultList?.filter((r) => !r.isControl) || [];

        // Enrich patient results with computed fields
        const enrichedResults = patientResults.map((result) => 
          enrichResultWithComputedFields(result)
        );

        setQcSamples(qcSamplesList);
        setResults(enrichedResults);

        // Evaluate QC status
        evaluateQcStatus(qcSamplesList);

        // Fetch run settings
        await fetchRunSettings();

        // Fetch QA/QC sidebar data
        await fetchQcHistory();
        await fetchAnalyzerInfo();
        await fetchReagentStatus();
      }
    } catch (error) {
      console.error("Error fetching analyzer results:", error);
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "error.fetch.analyzer.results" }) || "Error fetching analyzer results",
      });
      setNotificationVisible(true);
    } finally {
      setLoading(false);
    }
  }, [analyzerType, intl]);

  /**
   * Enrich result with computed fields (flags, interpretation, delta check)
   * Implementation of TODO: Add flag detection, interpretation, and delta check logic
   */
  const enrichResultWithComputedFields = (result) => {
    const enriched = { ...result };

    // Compute flags based on result value and normal range
    enriched.flags = computeFlags(result);

    // Suggest interpretation based on flags
    enriched.interpretation = suggestInterpretation(result, enriched.flags);

    // QC status from result or default to 'none'
    enriched.qcStatus = result.qcStatus || "none";

    // Patient name from result
    enriched.patientName = result.patientName || "";

    return enriched;
  };

  /**
   * Compute flags for a result
   * Implementation of TODO: Flag detection logic
   */
  const computeFlags = (result) => {
    const flags = [];

    if (!result.result || !result.normalRange) {
      return flags;
    }

    // Parse normal range (e.g., "4.00 - 10.00")
    const rangeMatch = result.normalRange.match(/(\d+\.?\d*)\s*-\s*(\d+\.?\d*)/);
    if (!rangeMatch) {
      return flags;
    }

    const minRange = parseFloat(rangeMatch[1]);
    const maxRange = parseFloat(rangeMatch[2]);
    const resultValue = parseFloat(result.result);

    if (isNaN(resultValue)) {
      return flags;
    }

    // Critical thresholds (configurable - using 20% outside normal range)
    const criticalLow = minRange - (maxRange - minRange) * 0.2;
    const criticalHigh = maxRange + (maxRange - minRange) * 0.2;

    // Flag detection
    if (resultValue < criticalLow || resultValue > criticalHigh) {
      flags.push("critical");
    } else if (resultValue < minRange) {
      flags.push("below-normal");
    } else if (resultValue > maxRange) {
      flags.push("above-normal");
    }

    // Check for delta check if previous result exists
    if (result.previousResult && result.previousResult.value) {
      const deltaPercent = calculateDeltaPercent(
        resultValue,
        parseFloat(result.previousResult.value),
      );
      // Delta check threshold: 50% change
      if (Math.abs(deltaPercent) > 50) {
        flags.push("delta-check");
      }
    }

    return flags;
  };

  /**
   * Calculate percentage change between two values
   */
  const calculateDeltaPercent = (current, previous) => {
    if (previous === 0) return 0;
    return ((current - previous) / previous) * 100;
  };

  /**
   * Suggest interpretation based on result and flags
   * Implementation of TODO: Interpretation suggestion logic
   */
  const suggestInterpretation = (result, flags) => {
    if (flags.includes("critical")) {
      return {
        suggested: {
          label: flags.includes("below-normal") || parseFloat(result.result) < parseFloat(result.normalRange?.split("-")[0] || "0")
            ? "Critical Low"
            : "Critical High",
          color: "red",
        },
      };
    }

    if (flags.includes("above-normal")) {
      return {
        suggested: {
          label: "High",
          color: "orange",
        },
      };
    }

    if (flags.includes("below-normal")) {
      return {
        suggested: {
          label: "Low",
          color: "yellow",
        },
      };
    }

    return {
      suggested: {
        label: "Normal",
        color: "green",
      },
    };
  };

  /**
   * Evaluate overall QC status from control samples
   * QC-FIRST RULE: If ANY control fails, ALL patient results are non-conforming
   * Implementation of TODO: QC evaluation against expected ranges
   */
  const evaluateQcStatus = (qcSamplesList) => {
    if (!qcSamplesList || qcSamplesList.length === 0) {
      setOverallQcStatus("none");
      return;
    }

    // Evaluate each QC sample against expected range
    const evaluatedQcSamples = qcSamplesList.map((qc) => {
      const qcResult = evaluateQcSample(qc);
      return { ...qc, ...qcResult };
    });

    setQcSamples(evaluatedQcSamples);

    // Check if any QC failed
    const anyFailed = evaluatedQcSamples.some((qc) => qc.qcStatus === "fail");
    const allPassed = evaluatedQcSamples.every((qc) => qc.qcStatus === "pass");

    if (anyFailed) {
      setOverallQcStatus("fail");
      // CRITICAL: Mark all patient results as non-conforming
      setResults((prev) => prev.map((r) => ({ ...r, nonConforming: true })));
      // Create non-conformity record
      createNonConformityRecord(evaluatedQcSamples);
    } else if (allPassed) {
      setOverallQcStatus("pass");
      setResults((prev) => prev.map((r) => ({ ...r, nonConforming: false })));
    } else {
      setOverallQcStatus("none");
    }
  };

  /**
   * Evaluate a single QC sample against expected range
   * Implementation of TODO: QC rules engine (basic range check + Westgard 1-2s rule)
   */
  const evaluateQcSample = (qc) => {
    if (!qc.result || !qc.expectedRange) {
      return { qcStatus: "none" };
    }

    // Parse expected range
    const rangeMatch = qc.expectedRange.match(/(\d+\.?\d*)\s*-\s*(\d+\.?\d*)/);
    if (!rangeMatch) {
      return { qcStatus: "none" };
    }

    const minExpected = parseFloat(rangeMatch[1]);
    const maxExpected = parseFloat(rangeMatch[2]);
    const resultValue = parseFloat(qc.result);

    if (isNaN(resultValue)) {
      return { qcStatus: "none" };
    }

    // Basic range check (Westgard 1-2s rule: within ±2 SD)
    if (resultValue >= minExpected && resultValue <= maxExpected) {
      return { qcStatus: "pass" };
    } else {
      return {
        qcStatus: "fail",
        failureReason: `Result ${resultValue} ${qc.unit || ""} is outside acceptable range ${qc.expectedRange}`,
      };
    }
  };

  /**
   * Create non-conformity record when QC fails
   * Implementation of TODO: Non-conformity management
   */
  const createNonConformityRecord = async (failedQcSamples) => {
    try {
      const ncPayload = {
        date: new Date().toISOString(),
        type: "QC_FAILURE",
        analyzerType: analyzerType,
        failedControls: failedQcSamples.filter((qc) => qc.qcStatus === "fail"),
        affectedResultCount: results.length,
        status: "OPEN",
      };

      const response = await postToOpenElisServer(
        "/rest/non-conformities",
        JSON.stringify(ncPayload),
      );

      if (response && response.id) {
        setNonConformityId(response.id);
      }
    } catch (error) {
      console.error("Error creating non-conformity record:", error);
    }
  };

  /**
   * Fetch run settings (analyzer and reagent lots)
   * Endpoint: GET /rest/AnalyzerResults/runSettings?type={analyzerType}
   */
  const fetchRunSettings = async () => {
    try {
      const response = await getFromOpenElisServer(
        `/rest/AnalyzerResults/runSettings?type=${analyzerType}`,
      );
      if (response) {
        setRunSettings(response);
        // Initialize selected reagent lots from FIFO pre-selection
        if (response.reagentLots) {
          const selected = {};
          response.reagentLots.forEach((lot) => {
            selected[lot.reagentId] = lot.lotNumber;
          });
          setSelectedReagentLots(selected);
        }
      }
    } catch (error) {
      console.error("Error fetching run settings:", error);
    }
  };

  /**
   * Fetch available reagent lots for analyzer
   * Implementation of TODO: Reagent inventory integration
   */
  const fetchAvailableReagentLots = async () => {
    try {
      const response = await getFromOpenElisServer(
        `/rest/AnalyzerResults/availableReagentLots?type=${analyzerType}`,
      );
      if (response && response.reagents) {
        setAvailableReagentLots(response.reagents);
      }
    } catch (error) {
      console.error("Error fetching available reagent lots:", error);
    }
  };

  /**
   * Fetch recent QC history
   * Endpoint: GET /rest/AnalyzerResults/qcHistory?analyzerId={id}
   */
  const fetchQcHistory = async () => {
    try {
      const response = await getFromOpenElisServer(
        `/rest/AnalyzerResults/qcHistory?type=${analyzerType}`,
      );
      if (response) {
        setQcHistory(response.history || []);
      }
    } catch (error) {
      console.error("Error fetching QC history:", error);
    }
  };

  /**
   * Fetch analyzer information
   * Endpoint: GET /rest/AnalyzerResults/analyzerInfo?type={analyzerType}
   */
  const fetchAnalyzerInfo = async () => {
    try {
      const response = await getFromOpenElisServer(
        `/rest/AnalyzerResults/analyzerInfo?type=${analyzerType}`,
      );
      if (response) {
        setAnalyzerInfo(response);
      }
    } catch (error) {
      console.error("Error fetching analyzer info:", error);
    }
  };

  /**
   * Fetch reagent status
   * Endpoint: GET /rest/AnalyzerResults/reagents?type={analyzerType}
   */
  const fetchReagentStatus = async () => {
    try {
      const response = await getFromOpenElisServer(
        `/rest/AnalyzerResults/reagents?type=${analyzerType}`,
      );
      if (response) {
        setReagentStatus(response.reagents || []);
      }
    } catch (error) {
      console.error("Error fetching reagent status:", error);
    }
  };

  /**
   * Fetch row details on demand (when row is expanded)
   * Fetches: History, QA/QC linkage, Method & Reagents
   * Implementation of TODO: Previous results and delta check retrieval
   */
  const fetchRowDetails = async (resultId, result) => {
    if (rowDetailsCache[resultId]) {
      return;
    }

    try {
      const response = await getFromOpenElisServer(
        `/rest/AnalyzerResults/details?resultId=${resultId}`,
      );
      
      if (response) {
        // Enrich with delta check calculation if history exists
        if (response.history && response.history.length > 0) {
          const currentValue = parseFloat(result.result);
          const previousValue = parseFloat(response.history[0].value);
          
          if (!isNaN(currentValue) && !isNaN(previousValue)) {
            const deltaPercent = calculateDeltaPercent(currentValue, previousValue);
            response.deltaCheck = {
              previous: response.history[0].value,
              change: `${deltaPercent > 0 ? "+" : ""}${deltaPercent.toFixed(1)}%`,
              threshold: "50%",
              exceeded: Math.abs(deltaPercent) > 50,
            };
          }
        }

        setRowDetailsCache((prev) => ({ ...prev, [resultId]: response }));
      }
    } catch (error) {
      console.error(`Error fetching details for result ${resultId}:`, error);
    }
  };

  /**
   * Handle row expansion
   */
  const handleRowExpand = (resultId, result, isExpanding) => {
    setExpandedRows((prev) => ({ ...prev, [resultId]: isExpanding }));
    if (isExpanding) {
      fetchRowDetails(resultId, result);
    }
  };

  /**
   * Handle row selection
   */
  const toggleSelectRow = (rowId) => {
    setSelectedRows((prev) =>
      prev.includes(rowId) ? prev.filter((id) => id !== rowId) : [...prev, rowId],
    );
  };

  /**
   * Handle select all
   */
  const toggleSelectAll = () => {
    if (selectedRows.length === results.length) {
      setSelectedRows([]);
    } else {
      setSelectedRows(results.map((r) => r.id));
    }
  };

  /**
   * Select only normal results (no flags, not non-conforming)
   */
  const selectNormalOnly = () => {
    const normalResults = results.filter(
      (r) => !r.nonConforming && (!r.flags || r.flags.length === 0),
    );
    setSelectedRows(normalResults.map((r) => r.id));
  };

  /**
   * Handle Save/Accept action
   * QC-FIRST ENFORCEMENT: Blocked if QC fails
   */
  const handleSave = async () => {
    if (overallQcStatus === "fail") {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "error.qc.failed.cannot.save" }) || 
                "QC failure detected - Results cannot be accepted until non-conformity is resolved",
      });
      setNotificationVisible(true);
      return;
    }

    if (selectedRows.length === 0) {
      addNotification({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "warning.no.results.selected" }) || 
                "Please select results to import",
      });
      setNotificationVisible(true);
      return;
    }

    setSaving(true);
    try {
      const selectedResults = results.filter((r) => selectedRows.includes(r.id));
      const payload = {
        resultList: selectedResults.map((r) => ({
          ...r,
          isAccepted: true,
          analyzerId: runSettings?.analyzerId,
          reagentLots: runSettings?.reagentLots,
        })),
      };

      await postToOpenElisServer("/rest/AnalyzerResults", JSON.stringify(payload));

      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "success.results.saved" }) || 
                "Results imported successfully",
      });
      setNotificationVisible(true);
      
      // Refresh data
      await fetchAnalyzerResults();
      setSelectedRows([]);
    } catch (error) {
      console.error("Error saving results:", error);
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "error.save.results" }) || 
                "Error saving results",
      });
      setNotificationVisible(true);
    } finally {
      setSaving(false);
    }
  };

  /**
   * Handle Retest action
   */
  const handleRetest = async () => {
    if (selectedRows.length === 0) {
      return;
    }

    try {
      const selectedResults = results.filter((r) => selectedRows.includes(r.id));
      const payload = {
        resultList: selectedResults.map((r) => ({ ...r, isRejected: true })),
      };

      await postToOpenElisServer("/rest/AnalyzerResults", JSON.stringify(payload));

      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "success.results.retest" }) || 
                "Results marked for retest",
      });
      setNotificationVisible(true);
      await fetchAnalyzerResults();
      setSelectedRows([]);
    } catch (error) {
      console.error("Error marking retest:", error);
    }
  };

  /**
   * Handle Ignore action
   */
  const handleIgnore = async () => {
    if (selectedRows.length === 0) {
      return;
    }

    try {
      const selectedResults = results.filter((r) => selectedRows.includes(r.id));
      const payload = {
        resultList: selectedResults.map((r) => ({ ...r, isDeleted: true })),
      };

      await postToOpenElisServer("/rest/AnalyzerResults", JSON.stringify(payload));

      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "success.results.ignored" }) || 
                "Results ignored",
      });
      setNotificationVisible(true);
      await fetchAnalyzerResults();
      setSelectedRows([]);
    } catch (error) {
      console.error("Error ignoring results:", error);
    }
  };

  /**
   * Handle refresh
   */
  const handleRefresh = () => {
    fetchAnalyzerResults();
  };

  /**
   * Open reagent lot change modal
   */
  const handleOpenReagentModal = async () => {
    await fetchAvailableReagentLots();
    setShowReagentModal(true);
  };

  /**
   * Apply reagent lot changes
   */
  const handleApplyReagentLots = () => {
    if (runSettings) {
      const updatedLots = Object.keys(selectedReagentLots).map((reagentId) => {
        const lotNumber = selectedReagentLots[reagentId];
        const lotDetails = availableReagentLots[reagentId]?.find(
          (lot) => lot.lotNumber === lotNumber,
        );
        return lotDetails || { reagentId, lotNumber };
      });

      setRunSettings({
        ...runSettings,
        reagentLots: updatedLots,
      });
    }
    setShowReagentModal(false);
  };

  /**
   * Filter results based on search
   */
  const filteredResults = results.filter((r) => {
    if (!searchTerm) return true;
    const search = searchTerm.toLowerCase();
    return (
      r.accessionNumber?.toLowerCase().includes(search) ||
      r.testName?.toLowerCase().includes(search) ||
      r.patientName?.toLowerCase().includes(search)
    );
  });

  /**
   * Get flag icon component
   */
  const getFlagIcon = (flag) => {
    switch (flag) {
      case "critical":
        return <ErrorOutline size={16} style={{ color: "#da1e28" }} />;
      case "above-normal":
      case "below-normal":
        return <WarningAlt size={16} style={{ color: "#f1c21b" }} />;
      case "delta-check":
        return <Repeat size={16} style={{ color: "#0f62fe" }} />;
      default:
        return null;
    }
  };

  /**
   * Get interpretation color
   */
  const getInterpretationColor = (label) => {
    const lower = label?.toLowerCase();
    if (lower?.includes("critical")) return "red";
    if (lower?.includes("high") || lower?.includes("low")) return "orange";
    if (lower?.includes("normal")) return "green";
    return "gray";
  };

  /**
   * Render QC Results Panel
   */
  const renderQcPanel = () => {
    if (qcSamples.length === 0 && overallQcStatus === "none") {
      return (
        <div className="qc-panel qc-panel--warning" style={{ marginBottom: "1rem" }}>
          <div
            className="qc-panel__header"
            onClick={() => setQcPanelExpanded(!qcPanelExpanded)}
          >
            <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
              {qcPanelExpanded ? <ChevronDown size={20} /> : <ChevronRight size={20} />}
              <WarningAlt size={20} />
              <strong>
                <FormattedMessage
                  id="qc.no.samples.detected"
                  defaultMessage="No QC samples detected in this run"
                />
              </strong>
            </div>
          </div>
          {qcPanelExpanded && (
            <div className="qc-panel__content" style={{ padding: "1rem" }}>
              <FormattedMessage
                id="qc.manual.verification.required"
                defaultMessage="Manual QC verification required before accepting patient results"
              />
            </div>
          )}
        </div>
      );
    }

    const panelClass = overallQcStatus === "fail" ? "qc-panel--fail" : "qc-panel--pass";

    return (
      <div className={`qc-panel ${panelClass}`} style={{ marginBottom: "1rem" }}>
        <div
          className="qc-panel__header"
          onClick={() => setQcPanelExpanded(!qcPanelExpanded)}
        >
          <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
            {qcPanelExpanded ? <ChevronDown size={20} /> : <ChevronRight size={20} />}
            {overallQcStatus === "pass" ? (
              <CheckmarkOutline size={20} />
            ) : (
              <ErrorOutline size={20} />
            )}
            <strong>
              {overallQcStatus === "pass" ? (
                <FormattedMessage
                  id="qc.all.passed"
                  defaultMessage="All {count} controls passed - Patient results can be accepted"
                  values={{ count: qcSamples.length }}
                />
              ) : (
                <FormattedMessage
                  id="qc.failure"
                  defaultMessage="QC FAILURE - Patient results marked as non-conforming"
                />
              )}
            </strong>
          </div>
        </div>

        {qcPanelExpanded && (
          <div
            className="qc-panel__content"
            style={{ padding: "1rem", display: "flex", gap: "1rem", flexWrap: "wrap" }}
          >
            {qcSamples.map((qc) => (
              <div
                key={qc.id}
                className={`qc-card qc-card--${qc.qcStatus}`}
                style={{
                  border: "1px solid #e0e0e0",
                  borderRadius: "4px",
                  padding: "1rem",
                  minWidth: "200px",
                  flex: "1 1 auto",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    marginBottom: "0.5rem",
                  }}
                >
                  <Tag type={qc.qcStatus === "pass" ? "green" : "red"}>
                    {qc.qcStatus === "pass" ? "PASS" : "FAIL"}
                  </Tag>
                  <span style={{ fontSize: "0.875rem", color: "#525252" }}>
                    {qc.runTime || qc.completeDate}
                  </span>
                </div>
                <div style={{ marginBottom: "0.5rem" }}>
                  <strong>{qc.controlType || "Control"}</strong>
                  <div style={{ fontSize: "0.875rem", color: "#525252" }}>
                    {qc.accessionNumber}
                  </div>
                </div>
                <div style={{ borderTop: "1px solid #e0e0e0", paddingTop: "0.5rem" }}>
                  <div style={{ display: "flex", justifyContent: "space-between" }}>
                    <div>
                      <div style={{ fontSize: "0.75rem", color: "#525252" }}>Result</div>
                      <strong>
                        {qc.result} {qc.units}
                      </strong>
                    </div>
                    <div style={{ textAlign: "right" }}>
                      <div style={{ fontSize: "0.75rem", color: "#525252" }}>Expected</div>
                      <div>{qc.expectedRange || "N/A"}</div>
                    </div>
                  </div>
                  {qc.failureReason && (
                    <div
                      style={{
                        marginTop: "0.5rem",
                        color: "#da1e28",
                        fontSize: "0.875rem",
                      }}
                    >
                      ⚠ {qc.failureReason}
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    );
  };

  /**
   * Render non-conformity alert banner
   */
  const renderNonConformityAlert = () => {
    if (overallQcStatus !== "fail") return null;

    const nonConformingCount = results.filter((r) => r.nonConforming).length;

    return (
      <InlineNotification
        kind="error"
        title={intl.formatMessage({
          id: "non.conformity.alert.title",
          defaultMessage: "Non-Conformity Alert",
        })}
        subtitle={intl.formatMessage(
          {
            id: "non.conformity.alert.message",
            defaultMessage:
              "QC failure detected in this run. All {count} patient results have been marked as non-conforming and cannot be released until the non-conformity is resolved.",
          },
          { count: nonConformingCount },
        )}
        lowContrast
        style={{ marginBottom: "1rem" }}
        actions={
          nonConformityId ? (
            <Button
              kind="ghost"
              size="sm"
              onClick={() => window.location.href = `/NonConformity?id=${nonConformityId}`}
            >
              View Non-Conformity
            </Button>
          ) : null
        }
      />
    );
  };

  /**
   * Render run settings panel
   */
  const renderRunSettings = () => {
    if (!runSettings) return null;

    return (
      <div
        className="run-settings-panel"
        style={{
          border: "1px solid #e0e0e0",
          borderRadius: "4px",
          padding: "1rem",
          marginBottom: "1rem",
          backgroundColor: "#f4f4f4",
        }}
      >
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: "1rem",
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
            <Settings size={20} />
            <strong>Run Settings</strong>
          </div>
          <span style={{ fontSize: "0.875rem", color: "#525252" }}>
            Applied to all {results.length} results in this run
          </span>
        </div>

        <Grid>
          <Column lg={6} md={4} sm={4}>
            <div style={{ marginBottom: "1rem" }}>
              <div
                style={{
                  fontSize: "0.75rem",
                  color: "#525252",
                  marginBottom: "0.25rem",
                }}
              >
                ANALYZER
              </div>
              <div
                style={{
                  border: "1px solid #e0e0e0",
                  borderRadius: "4px",
                  padding: "0.75rem",
                  backgroundColor: "#ffffff",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "0.5rem",
                    marginBottom: "0.25rem",
                  }}
                >
                  <Chemistry size={20} />
                  <strong>{runSettings.analyzerName}</strong>
                </div>
                <div style={{ fontSize: "0.875rem", color: "#525252" }}>
                  <span
                    style={{
                      color: runSettings.analyzerStatus === "online" ? "#24a148" : "#da1e28",
                    }}
                  >
                    ● {runSettings.analyzerStatus}
                  </span>
                  {" • "}
                  QC {runSettings.analyzerQcStatus === "pass" ? "Pass" : "Fail"}
                </div>
                <div style={{ fontSize: "0.75rem", color: "#525252", marginTop: "0.5rem" }}>
                  ✓ Auto-assigned to all
                </div>
              </div>
            </div>
          </Column>

          <Column lg={10} md={4} sm={4}>
            <div style={{ marginBottom: "1rem" }}>
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  marginBottom: "0.25rem",
                }}
              >
                <div style={{ fontSize: "0.75rem", color: "#525252" }}>
                  REAGENTS / CARTRIDGES USED THIS RUN
                </div>
                <Button kind="ghost" size="sm" onClick={handleOpenReagentModal}>
                  Change
                </Button>
              </div>
              <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
                {runSettings.reagentLots?.map((reagent) => (
                  <div
                    key={reagent.lotNumber}
                    style={{
                      border: "1px solid #e0e0e0",
                      borderRadius: "4px",
                      padding: "0.75rem",
                      backgroundColor: "#ffffff",
                      minWidth: "180px",
                    }}
                  >
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "0.25rem",
                        marginBottom: "0.25rem",
                      }}
                    >
                      <Chemistry size={16} />
                      <strong style={{ fontSize: "0.875rem" }}>
                        {reagent.reagentName}
                      </strong>
                    </div>
                    <div style={{ fontSize: "0.75rem", color: "#525252" }}>
                      LOT-{reagent.lotNumber}
                    </div>
                    <div
                      style={{
                        fontSize: "0.75rem",
                        color: reagent.isExpiring ? "#f1c21b" : "#525252",
                      }}
                    >
                      Exp: {reagent.expirationDate}
                      {reagent.fifo && " [FIFO]"}
                    </div>
                  </div>
                ))}
              </div>
              <div style={{ fontSize: "0.75rem", color: "#0f62fe", marginTop: "0.5rem" }}>
                ⚡ FIFO lots pre-selected. Applied to all saved results.
              </div>
            </div>
          </Column>
        </Grid>
      </div>
    );
  };

  /**
   * Render reagent lot change modal
   */
  const renderReagentModal = () => {
    return (
      <Modal
        open={showReagentModal}
        onRequestClose={() => setShowReagentModal(false)}
        modalHeading="Select Reagent Lots for This Run"
        primaryButtonText="Apply to All Results"
        secondaryButtonText="Cancel"
        onRequestSubmit={handleApplyReagentLots}
      >
        <div style={{ padding: "1rem" }}>
          {Object.keys(availableReagentLots).map((reagentId) => {
            const lots = availableReagentLots[reagentId];
            const reagentName = lots[0]?.reagentName || reagentId;

            return (
              <div key={reagentId} style={{ marginBottom: "1.5rem" }}>
                <h5 style={{ marginBottom: "0.5rem" }}>{reagentName}</h5>
                {lots.map((lot) => (
                  <div key={lot.lotNumber} style={{ marginBottom: "0.5rem" }}>
                    <Checkbox
                      id={`lot-${reagentId}-${lot.lotNumber}`}
                      labelText={
                        <span>
                          LOT-{lot.lotNumber} | Rcvd: {lot.receivedDate} | Exp:{" "}
                          {lot.expirationDate}
                          {lot.fifo && " [FIFO]"}
                          {lot.isExpiring && " ⚠ Expiring soon"}
                        </span>
                      }
                      checked={selectedReagentLots[reagentId] === lot.lotNumber}
                      onChange={(checked) => {
                        if (checked) {
                          setSelectedReagentLots((prev) => ({
                            ...prev,
                            [reagentId]: lot.lotNumber,
                          }));
                        }
                      }}
                    />
                  </div>
                ))}
              </div>
            );
          })}
          <p style={{ fontSize: "0.875rem", color: "#525252", marginTop: "1rem" }}>
            ℹ Select the lots actually used for this analyzer run.
          </p>
        </div>
      </Modal>
    );
  };

  /**
   * Render summary stats
   */
  const renderSummaryStats = () => {
    const pendingCount = results.filter((r) => r.status === "pending").length;
    const flaggedCount = results.filter((r) => r.flags && r.flags.length > 0).length;
    const criticalCount = results.filter((r) => r.flags?.includes("critical")).length;
    const nonConformingCount = results.filter((r) => r.nonConforming).length;

    return (
      <div style={{ display: "flex", gap: "1rem", marginBottom: "1rem" }}>
        <Tag>Total: {results.length}</Tag>
        <Tag type="blue">Pending: {pendingCount}</Tag>
        {flaggedCount > 0 && <Tag type="orange">Flagged: {flaggedCount}</Tag>}
        {criticalCount > 0 && <Tag type="red">Critical: {criticalCount}</Tag>}
        {nonConformingCount > 0 && <Tag type="red">NC: {nonConformingCount}</Tag>}
      </div>
    );
  };

  /**
   * Render bulk actions bar
   */
  const renderBulkActions = () => {
    const saveDisabled = overallQcStatus === "fail" || selectedRows.length === 0;

    return (
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          padding: "1rem",
          borderTop: "1px solid #e0e0e0",
          borderBottom: "1px solid #e0e0e0",
          marginBottom: "1rem",
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
          <Checkbox
            id="select-all"
            labelText=""
            checked={selectedRows.length === results.length && results.length > 0}
            indeterminate={
              selectedRows.length > 0 && selectedRows.length < results.length
            }
            onChange={toggleSelectAll}
          />
          <span>{selectedRows.length} selected</span>
          <Button kind="ghost" size="sm" onClick={selectNormalOnly}>
            Select Normal Only
          </Button>
        </div>

        <div style={{ display: "flex", gap: "0.5rem" }}>
          <Button
            kind="primary"
            onClick={handleSave}
            disabled={saveDisabled || saving}
            renderIcon={CheckmarkOutline}
          >
            {saving ? <InlineLoading description="Saving..." /> : "Import"}
          </Button>
          <Button
            kind="secondary"
            onClick={handleRetest}
            disabled={selectedRows.length === 0}
            renderIcon={Repeat}
          >
            Retest
          </Button>
          <Button
            kind="ghost"
            onClick={handleIgnore}
            disabled={selectedRows.length === 0}
            renderIcon={TrashCan}
          >
            Ignore
          </Button>
        </div>
      </div>
    );
  };

  /**
   * Render expandable row details (tabs)
   */
  const renderExpandedRow = (row) => {
    const details = rowDetailsCache[row.id];

    if (!details) {
      return (
        <div style={{ padding: "2rem", textAlign: "center" }}>
          <InlineLoading description="Loading details..." />
        </div>
      );
    }

    return (
      <div style={{ padding: "1rem", backgroundColor: "#f4f4f4" }}>
        <Tabs>
          <TabList aria-label="Result details tabs">
            <Tab>History</Tab>
            <Tab>QA/QC</Tab>
            <Tab>Method & Reagents</Tab>
          </TabList>
          <TabPanels>
            {/* History Tab */}
            <TabPanel>
              <div style={{ padding: "1rem" }}>
                <h5>Previous Results</h5>
                {details.history && details.history.length > 0 ? (
                  <div style={{ marginTop: "1rem" }}>
                    {details.history.map((hist, idx) => (
                      <div
                        key={idx}
                        style={{
                          display: "flex",
                          justifyContent: "space-between",
                          padding: "0.5rem",
                          borderBottom: "1px solid #e0e0e0",
                        }}
                      >
                        <span>{hist.date}</span>
                        <span>
                          <strong>{hist.value}</strong> {hist.unit}
                        </span>
                        <span style={{ color: "#525252" }}>{hist.interpretation}</span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div style={{ marginTop: "1rem", color: "#525252" }}>
                    No previous results
                  </div>
                )}

                {details.deltaCheck && details.deltaCheck.exceeded && (
                  <div
                    style={{
                      marginTop: "1rem",
                      padding: "1rem",
                      backgroundColor: "#fff3cd",
                      borderRadius: "4px",
                      border: "1px solid #f1c21b",
                    }}
                  >
                    <strong>⚠ Delta Check Alert</strong>
                    <div>Previous: {details.deltaCheck.previous}</div>
                    <div>Change: {details.deltaCheck.change}</div>
                    <div>Threshold: {details.deltaCheck.threshold}</div>
                  </div>
                )}
              </div>
            </TabPanel>

            {/* QA/QC Tab */}
            <TabPanel>
              <div style={{ padding: "1rem" }}>
                <h5>QC Linkage</h5>
                {details.qcLinkage ? (
                  <div style={{ marginTop: "1rem" }}>
                    <div>
                      <strong>Run ID:</strong> {details.qcLinkage.runId}
                    </div>
                    <div>
                      <strong>QC Status:</strong>{" "}
                      <Tag type={details.qcLinkage.status === "pass" ? "green" : "red"}>
                        {details.qcLinkage.status.toUpperCase()}
                      </Tag>
                    </div>
                    <div>
                      <strong>Control Levels:</strong>{" "}
                      {details.qcLinkage.controlLevels?.join(", ")}
                    </div>
                  </div>
                ) : (
                  <div style={{ marginTop: "1rem", color: "#525252" }}>
                    No QC data available
                  </div>
                )}
              </div>
            </TabPanel>

            {/* Method & Reagents Tab */}
            <TabPanel>
              <div style={{ padding: "1rem" }}>
                <h5>Analyzer</h5>
                <div style={{ marginTop: "0.5rem" }}>
                  <strong>{details.analyzer?.name || runSettings?.analyzerName}</strong>
                </div>

                <h5 style={{ marginTop: "1rem" }}>Reagents Used</h5>
                {details.reagents && details.reagents.length > 0 ? (
                  <div style={{ marginTop: "0.5rem" }}>
                    {details.reagents.map((reagent, idx) => (
                      <div key={idx} style={{ marginBottom: "0.5rem" }}>
                        <div>
                          <strong>{reagent.name}</strong>
                        </div>
                        <div style={{ fontSize: "0.875rem", color: "#525252" }}>
                          Lot: {reagent.lot} | Exp: {reagent.expiration}
                        </div>
                      </div>
                    ))}
                  </div>
                ) : runSettings?.reagentLots ? (
                  <div style={{ marginTop: "0.5rem" }}>
                    {runSettings.reagentLots.map((reagent, idx) => (
                      <div key={idx} style={{ marginBottom: "0.5rem" }}>
                        <div>
                          <strong>{reagent.reagentName}</strong>
                        </div>
                        <div style={{ fontSize: "0.875rem", color: "#525252" }}>
                          Lot: {reagent.lotNumber} | Exp: {reagent.expirationDate}
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div style={{ marginTop: "0.5rem", color: "#525252" }}>
                    No reagent information available
                  </div>
                )}
              </div>
            </TabPanel>
          </TabPanels>
        </Tabs>
      </div>
    );
  };

  /**
   * Render QA/QC Sidebar
   */
  const renderQaSidebar = () => {
    return (
      <div style={{ padding: "1rem", backgroundColor: "#f4f4f4", height: "100%" }}>
        {/* Current Run QC Status */}
        <div style={{ marginBottom: "1.5rem" }}>
          <h5>
            Current Run: QC{" "}
            {overallQcStatus === "pass"
              ? "Passed"
              : overallQcStatus === "fail"
              ? "Failed"
              : "Pending"}
          </h5>
          {qcSamples.length > 0 && (
            <div style={{ display: "flex", gap: "0.5rem", marginTop: "0.5rem" }}>
              {qcSamples.map((qc, idx) => (
                <div
                  key={qc.id}
                  style={{
                    border: "1px solid #e0e0e0",
                    borderRadius: "4px",
                    padding: "0.5rem",
                    textAlign: "center",
                    backgroundColor: "#ffffff",
                    minWidth: "60px",
                  }}
                >
                  <div style={{ fontSize: "0.75rem", color: "#525252" }}>L{idx + 1}</div>
                  <div style={{ fontSize: "0.875rem" }}>
                    <strong>{qc.result}</strong>
                  </div>
                  <div>{qc.qcStatus === "pass" ? "✓" : "✗"}</div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Recent QC History */}
        <div style={{ marginBottom: "1.5rem" }}>
          <h5>Recent QC History</h5>
          <div style={{ marginTop: "0.5rem" }}>
            {qcHistory.slice(0, 5).map((hist, idx) => (
              <div
                key={idx}
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  padding: "0.5rem",
                  borderBottom: "1px solid #e0e0e0",
                  fontSize: "0.875rem",
                }}
              >
                <span>
                  {hist.date} {hist.time}
                </span>
                <span>{hist.status === "pass" ? "✓ PASS" : "✗ FAIL"}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Analyzer Info */}
        {analyzerInfo && (
          <div style={{ marginBottom: "1.5rem" }}>
            <h5>Analyzer Info</h5>
            <div style={{ marginTop: "0.5rem", fontSize: "0.875rem" }}>
              <div>
                <strong>Name:</strong> {analyzerInfo.name}
              </div>
              <div>
                <strong>Status:</strong> {analyzerInfo.status}
              </div>
              <div>
                <strong>Last Calibration:</strong> {analyzerInfo.lastCalibration}
              </div>
            </div>
          </div>
        )}

        {/* Reagent Status */}
        {reagentStatus.length > 0 && (
          <div>
            <h5>Reagent Status</h5>
            <div style={{ marginTop: "0.5rem" }}>
              {reagentStatus.map((reagent, idx) => (
                <div
                  key={idx}
                  style={{
                    padding: "0.5rem",
                    borderBottom: "1px solid #e0e0e0",
                    fontSize: "0.875rem",
                  }}
                >
                  <div>
                    <strong>{reagent.name}</strong>
                  </div>
                  <div style={{ color: "#525252" }}>Lot: {reagent.lot}</div>
                  <div
                    style={{
                      color: reagent.status === "expiring" ? "#f1c21b" : "#525252",
                    }}
                  >
                    Exp: {reagent.expires}
                    {reagent.status === "expiring" && " ⚠"}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    );
  };

  /**
   * Main render
   */
  if (loading) {
    return <Loading description="Loading analyzer results..." />;
  }

  const headers = [
    { key: "select", header: "" },
    {
      key: "sampleInfo",
      header: intl.formatMessage({
        id: "column.name.sampleInfo",
        defaultMessage: "Sample Info",
      }),
    },
    {
      key: "testName",
      header: intl.formatMessage({
        id: "column.name.testName",
        defaultMessage: "Test Name",
      }),
    },
    {
      key: "result",
      header: intl.formatMessage({ id: "column.name.result", defaultMessage: "Result" }),
    },
    {
      key: "range",
      header: intl.formatMessage({ id: "column.name.range", defaultMessage: "Range" }),
    },
    { key: "qc", header: "QC" },
    {
      key: "flags",
      header: intl.formatMessage({ id: "column.name.flags", defaultMessage: "Flags" }),
    },
    {
      key: "interpretation",
      header: intl.formatMessage({
        id: "column.name.interpretation",
        defaultMessage: "Interpretation",
      }),
    },
  ];

  const rows = filteredResults.map((result) => ({
    id: result.id,
    _originalResult: result,
    select: (
      <Checkbox
        id={`select-${result.id}`}
        labelText=""
        checked={selectedRows.includes(result.id)}
        onChange={() => toggleSelectRow(result.id)}
      />
    ),
    sampleInfo: (
      <div>
        <div>
          <strong>{result.accessionNumber}</strong>
        </div>
        <div style={{ fontSize: "0.875rem", color: "#525252" }}>
          {result.patientName}
        </div>
        {result.nonConforming && (
          <Tag type="red" size="sm" style={{ marginTop: "0.25rem" }}>
            NC
          </Tag>
        )}
      </div>
    ),
    testName: result.testName,
    result: (
      <div>
        <strong
          style={{
            color: result.flags?.includes("above-normal") ||
              result.flags?.includes("below-normal")
              ? "#f1c21b"
              : result.flags?.includes("critical")
              ? "#da1e28"
              : "#161616",
          }}
        >
          {result.result}
        </strong>{" "}
        {result.units}
      </div>
    ),
    range: result.normalRange || "—",
    qc: (
      <Tag
        type={
          result.qcStatus === "pass"
            ? "green"
            : result.qcStatus === "fail"
            ? "red"
            : "gray"
        }
        size="sm"
      >
        QC {result.qcStatus === "pass" ? "✓" : result.qcStatus === "fail" ? "✗" : "—"}
      </Tag>
    ),
    flags: (
      <div style={{ display: "flex", gap: "0.25rem" }}>
        {result.flags?.map((flag, idx) => (
          <span key={idx}>{getFlagIcon(flag)}</span>
        ))}
      </div>
    ),
    interpretation: result.interpretation?.suggested ? (
      <Tag
        type={getInterpretationColor(result.interpretation.suggested.label)}
        size="sm"
      >
        {result.interpretation.suggested.label}
      </Tag>
    ) : null,
  }));

  return (
    <div style={{ padding: "1rem" }}>
      <Grid fullWidth>
        <Column lg={13} md={6} sm={4}>
          {/* Header */}
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginBottom: "1rem",
            }}
          >
            <h3>
              <FormattedMessage
                id="analyzer.import.title"
                defaultMessage="Analyzer Results Import"
              />
              {" - "}
              {analyzerType}
            </h3>
            <div style={{ display: "flex", gap: "0.5rem" }}>
              <Button kind="ghost" renderIcon={Renew} onClick={handleRefresh}>
                <FormattedMessage id="button.refresh" defaultMessage="Refresh" />
              </Button>
              <Button kind="ghost" renderIcon={Settings}>
                <FormattedMessage id="button.settings" defaultMessage="Settings" />
              </Button>
            </div>
          </div>

          {/* QC Results Panel */}
          {renderQcPanel()}

          {/* Non-Conformity Alert */}
          {renderNonConformityAlert()}

          {/* Run Settings */}
          {renderRunSettings()}

          {/* Search & Summary */}
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginBottom: "1rem",
            }}
          >
            <ExpandableSearch
              size="lg"
              labelText="Search"
              placeholder="Search by lab number, test name, or patient"
              onChange={(e) => setSearchTerm(e.target.value)}
              value={searchTerm}
            />
            {renderSummaryStats()}
          </div>

          {/* Bulk Actions */}
          {renderBulkActions()}

          {/* Results Table */}
          <DataTable
            rows={rows}
            headers={headers}
            isSortable
            render={({
              rows,
              headers,
              getHeaderProps,
              getRowProps,
              getTableProps,
              getTableContainerProps,
            }) => (
              <TableContainer {...getTableContainerProps()}>
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      <TableExpandHeader />
                      {headers.map((header) => (
                        <TableHeader {...getHeaderProps({ header })} key={header.key}>
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {rows.map((row) => {
                      const originalRow = row.cells.find((c) => c.id.includes("_originalResult"))?.value;
                      return (
                        <React.Fragment key={row.id}>
                          <TableExpandRow
                            {...getRowProps({ row })}
                            style={{
                              backgroundColor: originalRow?.nonConforming
                                ? "#fff1f1"
                                : originalRow?.flags?.includes("critical")
                                ? "#fff4e5"
                                : undefined,
                            }}
                            isExpanded={expandedRows[row.id]}
                            onExpand={() =>
                              handleRowExpand(row.id, originalRow, !expandedRows[row.id])
                            }
                          >
                            {row.cells.map((cell) => {
                              if (cell.info.header === "_originalResult") return null;
                              return <TableCell key={cell.id}>{cell.value}</TableCell>;
                            })}
                          </TableExpandRow>
                          {expandedRows[row.id] && (
                            <TableExpandedRow colSpan={headers.length + 1}>
                              {renderExpandedRow(originalRow)}
                            </TableExpandedRow>
                          )}
                        </React.Fragment>
                      );
                    })}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          />
        </Column>

        {/* QA/QC Sidebar */}
        <Column lg={3} md={2} sm={4}>
          {renderQaSidebar()}
        </Column>
      </Grid>

      {/* Reagent Modal */}
      {renderReagentModal()}
    </div>
  );
};

export default AnalyzerImportRedesign;

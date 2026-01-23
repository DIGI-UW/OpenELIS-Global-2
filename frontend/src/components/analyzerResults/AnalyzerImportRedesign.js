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

const AnalyzerImportRedesign = ({ analyzerType }) => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [results, setResults] = useState([]);
  const [qcSamples, setQcSamples] = useState([]);
  const [runSettings, setRunSettings] = useState(null);
  const [selectedRows, setSelectedRows] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [expandedRows, setExpandedRows] = useState({});
  const [rowDetailsCache, setRowDetailsCache] = useState({});

  const [qcPanelExpanded, setQcPanelExpanded] = useState(true);
  const [overallQcStatus, setOverallQcStatus] = useState("none");
  const [nonConformityId, setNonConformityId] = useState(null);

  const [qcHistory, setQcHistory] = useState([]);
  const [analyzerInfo, setAnalyzerInfo] = useState(null);
  const [reagentStatus, setReagentStatus] = useState([]);

  const [showReagentModal, setShowReagentModal] = useState(false);
  const [showSettingsModal, setShowSettingsModal] = useState(false);
  const [selectedReagentLots, setSelectedReagentLots] = useState({});
  const [availableReagentLots, setAvailableReagentLots] = useState({});
  const [fifoWarnings, setFifoWarnings] = useState({});

  const formatResult = (value, significantDigits) => {
    const numValue = parseFloat(value);
    if (isNaN(numValue)) {
      return value;
    }
    let digits = parseInt(significantDigits);
    if (isNaN(digits) || digits < 0) {
      digits = 2;
    } else if (digits > 3) {
      digits = 3;
    }
    return numValue.toFixed(digits);
  };

  useEffect(() => {
    if (analyzerType) {
      fetchAnalyzerResults();
    }
  }, [analyzerType]);

  const fetchAnalyzerResults = useCallback(() => {
    setLoading(true);
    getFromOpenElisServer(
      `/rest/AnalyzerResults?type=${analyzerType}`,
      (response) => {
        if (response) {
          // Extract QC samples (control samples identified by isControl flag)
          const qcSamplesList =
            response.resultList?.filter((r) => r.isControl) || [];
          const patientResults =
            response.resultList?.filter((r) => !r.isControl) || [];

          // Enrich patient results with computed fields
          const enrichedResults = patientResults.map((result) =>
            enrichResultWithComputedFields(result),
          );

          setQcSamples(qcSamplesList);
          setResults(enrichedResults);

          // Evaluate QC status
          evaluateQcStatus(qcSamplesList);

          // Fetch run settings and QA/QC sidebar data
          fetchRunSettings();
          fetchQcHistory();
          fetchAnalyzerInfo();
          fetchReagentStatus();
        }
        setLoading(false);
      },
    );
  }, [analyzerType]);

  const enrichResultWithComputedFields = (result) => {
    const enriched = { ...result };

    enriched.flags = computeFlags(result);
    enriched.interpretation = suggestInterpretation(result, enriched.flags);
    enriched.qcStatus = result.qcStatus || "none";
    enriched.patientName = result.patientName || "";

    return enriched;
  };

  const computeFlags = (result) => {
    const flags = [];

    if (!result.result || !result.normalRange) {
      return flags;
    }

    const rangeMatch = result.normalRange.match(
      /(\d+\.?\d*)\s*-\s*(\d+\.?\d*)/,
    );
    if (!rangeMatch) {
      return flags;
    }

    const minRange = parseFloat(rangeMatch[1]);
    const maxRange = parseFloat(rangeMatch[2]);
    const resultValue = parseFloat(result.result);

    if (isNaN(resultValue)) {
      return flags;
    }

    const criticalLow = minRange - (maxRange - minRange) * 0.2;
    const criticalHigh = maxRange + (maxRange - minRange) * 0.2;

    if (resultValue < criticalLow || resultValue > criticalHigh) {
      flags.push("critical");
    } else if (resultValue < minRange) {
      flags.push("below-normal");
    } else if (resultValue > maxRange) {
      flags.push("above-normal");
    }

    if (result.previousResult && result.previousResult.value) {
      const deltaPercent = calculateDeltaPercent(
        resultValue,
        parseFloat(result.previousResult.value),
      );
      if (Math.abs(deltaPercent) > 50) {
        flags.push("delta-check");
      }
    }

    return flags;
  };

  const calculateDeltaPercent = (current, previous) => {
    if (previous === 0) return 0;
    return ((current - previous) / previous) * 100;
  };

  const suggestInterpretation = (result, flags) => {
    if (flags.includes("critical")) {
      return {
        suggested: {
          label:
            flags.includes("below-normal") ||
            parseFloat(result.result) <
              parseFloat(result.normalRange?.split("-")[0] || "0")
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

  const evaluateQcStatus = (qcSamplesList) => {
    if (!qcSamplesList || qcSamplesList.length === 0) {
      setOverallQcStatus("none");
      return;
    }

    const evaluatedQcSamples = qcSamplesList.map((qc) => {
      const qcResult = evaluateQcSample(qc);
      return { ...qc, ...qcResult };
    });

    setQcSamples(evaluatedQcSamples);

    const anyFailed = evaluatedQcSamples.some((qc) => qc.qcStatus === "fail");
    const allPassed = evaluatedQcSamples.every((qc) => qc.qcStatus === "pass");

    if (anyFailed) {
      setOverallQcStatus("fail");
      setResults((prev) => prev.map((r) => ({ ...r, nonConforming: true })));
      createNonConformityRecord(evaluatedQcSamples);
    } else if (allPassed) {
      setOverallQcStatus("pass");
      setResults((prev) => prev.map((r) => ({ ...r, nonConforming: false })));
    } else {
      setOverallQcStatus("none");
    }
  };

  const evaluateQcSample = (qc) => {
    if (!qc.result || !qc.expectedRange) {
      return { qcStatus: "none" };
    }

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

    if (resultValue >= minExpected && resultValue <= maxExpected) {
      return { qcStatus: "pass" };
    } else {
      return {
        qcStatus: "fail",
        failureReason: `Result ${resultValue} ${qc.unit || ""} is outside acceptable range ${qc.expectedRange}`,
      };
    }
  };

  const createNonConformityRecord = (failedQcSamples) => {
    const ncPayload = {
      date: new Date().toISOString(),
      type: "QC_FAILURE",
      analyzerType: analyzerType,
      failedControls: failedQcSamples.filter((qc) => qc.qcStatus === "fail"),
      affectedResultCount: results.length,
      status: "OPEN",
    };

    postToOpenElisServer(
      "/rest/non-conformities",
      JSON.stringify(ncPayload),
      (status) => {
        console.log("Non-conformity record created, status:", status);
      },
    );
  };

  const fetchRunSettings = () => {
    getFromOpenElisServer(
      `/rest/AnalyzerResults/runSettings?type=${analyzerType}`,
      (response) => {
        if (response) {
          setRunSettings(response);
          if (response.reagentLots) {
            const selected = {};
            response.reagentLots.forEach((lot) => {
              selected[lot.reagentId] = lot.lotNumber;
            });
            setSelectedReagentLots(selected);
          }
        }
      },
    );
  };

  const fetchAvailableReagentLots = () => {
    getFromOpenElisServer(
      `/rest/AnalyzerResults/availableReagentLots?type=${analyzerType}`,
      (response) => {
        if (response && response.reagents) {
          setAvailableReagentLots(response.reagents);
        }
      },
    );
  };

  const fetchQcHistory = () => {
    getFromOpenElisServer(
      `/rest/AnalyzerResults/qcHistory?type=${analyzerType}`,
      (response) => {
        if (response) {
          setQcHistory(response.history || []);
        }
      },
    );
  };

  const fetchAnalyzerInfo = () => {
    getFromOpenElisServer(
      `/rest/AnalyzerResults/analyzerInfo?type=${analyzerType}`,
      (response) => {
        if (response) {
          setAnalyzerInfo(response);
        }
      },
    );
  };

  const fetchReagentStatus = () => {
    getFromOpenElisServer(
      `/rest/AnalyzerResults/reagents?type=${analyzerType}`,
      (response) => {
        if (response) {
          setReagentStatus(response.reagents || []);
        }
      },
    );
  };

  const fetchRowDetails = (resultId, result) => {
    if (rowDetailsCache[resultId]) {
      return;
    }

    getFromOpenElisServer(
      `/rest/AnalyzerResults/details?resultId=${resultId}`,
      (response) => {
        if (response) {
          if (response.history && response.history.length > 0) {
            const currentValue = parseFloat(result.result);
            const previousValue = parseFloat(response.history[0].value);

            if (!isNaN(currentValue) && !isNaN(previousValue)) {
              const deltaPercent = calculateDeltaPercent(
                currentValue,
                previousValue,
              );
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
      },
    );
  };

  const handleRowExpand = (resultId, result, isExpanding) => {
    setExpandedRows((prev) => ({ ...prev, [resultId]: isExpanding }));
    if (isExpanding) {
      fetchRowDetails(resultId, result);
    }
  };

  const toggleSelectRow = (rowId) => {
    setSelectedRows((prev) =>
      prev.includes(rowId)
        ? prev.filter((id) => id !== rowId)
        : [...prev, rowId],
    );
  };

  const toggleSelectAll = () => {
    if (selectedRows.length === results.length) {
      setSelectedRows([]);
    } else {
      setSelectedRows(results.map((r) => r.id));
    }
  };

  const selectNormalOnly = () => {
    const normalResults = results.filter(
      (r) => !r.nonConforming && (!r.flags || r.flags.length === 0),
    );
    setSelectedRows(normalResults.map((r) => r.id));
  };

  const handleSave = () => {
    if (overallQcStatus === "fail") {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message:
          intl.formatMessage({ id: "error.qc.failed.cannot.save" }) ||
          "QC failure detected - Results cannot be accepted until non-conformity is resolved",
      });
      setNotificationVisible(true);
      return;
    }

    if (selectedRows.length === 0) {
      addNotification({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "notification.title" }),
        message:
          intl.formatMessage({ id: "warning.no.results.selected" }) ||
          "Please select results to import",
      });
      setNotificationVisible(true);
      return;
    }

    setSaving(true);
    const selectedResults = results.filter((r) => selectedRows.includes(r.id));
    const payload = {
      resultList: selectedResults.map((r) => ({
        ...r,
        isAccepted: true,
        analyzerId: runSettings?.analyzerId,
        reagentLots: runSettings?.reagentLots,
      })),
    };

    postToOpenElisServer(
      "/rest/AnalyzerResults",
      JSON.stringify(payload),
      (status) => {
        if (status === 200) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message:
              intl.formatMessage({ id: "success.results.saved" }) ||
              "Results imported successfully",
          });
          setNotificationVisible(true);

          // Refresh data
          fetchAnalyzerResults();
          setSelectedRows([]);
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message:
              intl.formatMessage({ id: "error.save.results" }) ||
              "Error saving results",
          });
          setNotificationVisible(true);
        }
        setSaving(false);
      },
    );
  };

  const handleRetest = () => {
    if (selectedRows.length === 0) {
      return;
    }

    const selectedResults = results.filter((r) => selectedRows.includes(r.id));
    const payload = {
      resultList: selectedResults.map((r) => ({ ...r, isRejected: true })),
    };

    postToOpenElisServer(
      "/rest/AnalyzerResults",
      JSON.stringify(payload),
      (status) => {
        if (status === 200) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message:
              intl.formatMessage({ id: "success.results.retest" }) ||
              "Results marked for retest",
          });
          setNotificationVisible(true);
          fetchAnalyzerResults();
          setSelectedRows([]);
        }
      },
    );
  };

  const handleIgnore = () => {
    if (selectedRows.length === 0) {
      return;
    }

    const selectedResults = results.filter((r) => selectedRows.includes(r.id));
    const payload = {
      resultList: selectedResults.map((r) => ({ ...r, isDeleted: true })),
    };

    postToOpenElisServer(
      "/rest/AnalyzerResults",
      JSON.stringify(payload),
      (status) => {
        if (status === 200) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message:
              intl.formatMessage({ id: "success.results.ignored" }) ||
              "Results ignored",
          });
          setNotificationVisible(true);
          fetchAnalyzerResults();
          setSelectedRows([]);
        }
      },
    );
  };

  const handleRefresh = () => {
    fetchAnalyzerResults();
  };

  const handleOpenReagentModal = () => {
    fetchAvailableReagentLots();
    setShowReagentModal(true);
  };

  const isFifoLot = (reagentId, lotNumber) => {
    const lots = availableReagentLots[reagentId];
    if (!lots || lots.length === 0) return true;
    // First lot in sorted list is FIFO
    return lots[0].lotNumber === lotNumber;
  };

  const handleReagentLotChange = (reagentId, lotNumber, checked) => {
    if (checked) {
      setSelectedReagentLots((prev) => ({ ...prev, [reagentId]: lotNumber }));

      // Check if non-FIFO lot selected
      if (!isFifoLot(reagentId, lotNumber)) {
        setFifoWarnings((prev) => ({
          ...prev,
          [reagentId]: `⚠️ Not FIFO - Older lot available (${availableReagentLots[reagentId][0].lotNumber})`,
        }));
      } else {
        setFifoWarnings((prev) => {
          const updated = { ...prev };
          delete updated[reagentId];
          return updated;
        });
      }
    }
  };

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

  const filteredResults = results.filter((r) => {
    if (!searchTerm) return true;
    const search = searchTerm.toLowerCase();
    return (
      r.accessionNumber?.toLowerCase().includes(search) ||
      r.testName?.toLowerCase().includes(search) ||
      r.patientName?.toLowerCase().includes(search)
    );
  });

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

  const getInterpretationColor = (label) => {
    const lower = label?.toLowerCase();
    if (lower?.includes("critical")) return "red";
    if (lower?.includes("high") || lower?.includes("low")) return "orange";
    if (lower?.includes("normal")) return "green";
    return "gray";
  };

  const renderQcPanel = () => {
    if (qcSamples.length === 0 && overallQcStatus === "none") {
      return (
        <div className="modern-card" style={{ marginBottom: "1.5rem" }}>
          <div
            className="qc-summary-tile warning"
            onClick={() => setQcPanelExpanded(!qcPanelExpanded)}
            style={{ cursor: "pointer" }}
          >
            <div className="flex-row-center">
              <WarningAlt size={20} fill="#f1c21b" />
              <strong>No QC Samples Detected</strong>
            </div>
            {qcPanelExpanded ? (
              <ChevronDown size={20} />
            ) : (
              <ChevronRight size={20} />
            )}
          </div>
          {qcPanelExpanded && (
            <div style={{ padding: "1rem" }}>
              Manual QC verification required before accepting patient results.
            </div>
          )}
        </div>
      );
    }

    const statusClass = overallQcStatus === "fail" ? "fail" : "pass";
    const icon =
      overallQcStatus === "fail" ? (
        <ErrorOutline size={24} fill="#da1e28" />
      ) : (
        <CheckmarkOutline size={24} fill="#525252" />
      );

    return (
      <div className="modern-card" style={{ marginBottom: "1.5rem" }}>
        <div
          className={`qc-summary-tile ${statusClass}`}
          onClick={() => setQcPanelExpanded(!qcPanelExpanded)}
          style={{ cursor: "pointer" }}
        >
          <div className="flex-row-center">
            {icon}
            <div>
              <h5 style={{ margin: 0, fontSize: "1rem" }}>
                {overallQcStatus === "pass"
                  ? "Quality Control Passed"
                  : "Quality Control FAIL"}
              </h5>
              <span className="text-label" style={{ fontSize: "0.8125rem" }}>
                {qcSamples.length} Controls Evaluated
              </span>
            </div>
          </div>
          {qcPanelExpanded ? (
            <ChevronDown size={20} />
          ) : (
            <ChevronRight size={20} />
          )}
        </div>

        {qcPanelExpanded && (
          <div
            style={{
              padding: "1rem",
              background: "#f4f4f4",
              overflowX: "auto",
              overflowY: "hidden",
            }}
          >
            <div
              style={{
                display: "flex",
                gap: "1rem",
                minWidth: "min-content",
              }}
            >
              {qcSamples.map((qc, idx) => (
                <div
                  key={qc.id}
                  className={`qc-micro-card ${qc.qcStatus}`}
                  style={{
                    minWidth: "110px",
                    maxWidth: "130px",
                    flex: "0 0 auto",
                  }}
                >
                  <div style={{ marginBottom: "0.5rem" }}>
                    <div
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                        marginBottom: "0.25rem",
                      }}
                    >
                      <span
                        className="text-label"
                        style={{ fontSize: "0.75rem", fontWeight: "600" }}
                      >
                        {qc.controlType || `L${idx + 1}`}
                      </span>
                      <span
                        style={{
                          fontSize: "0.65rem",
                          fontWeight: "600",
                          color: qc.qcStatus === "pass" ? "#525252" : "#da1e28",
                          textTransform: "uppercase",
                        }}
                      >
                        {qc.qcStatus === "pass" ? "PASS" : "FAIL"}
                      </span>
                    </div>
                  </div>
                  <div
                    className="text-value-lg"
                    style={{ fontSize: "1rem", marginBottom: "0.25rem" }}
                  >
                    {formatResult(qc.result, qc.significantDigits)}
                  </div>
                  <div className="text-label" style={{ fontSize: "0.75rem" }}>
                    {qc.units}
                  </div>

                  {qc.failureReason && (
                    <div
                      style={{
                        color: "#da1e28",
                        fontSize: "0.65rem",
                        marginTop: "0.25rem",
                      }}
                    >
                      {qc.failureReason}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    );
  };

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
              onClick={() =>
                (window.location.href = `/NonConformity?id=${nonConformityId}`)
              }
            >
              View Non-Conformity
            </Button>
          ) : null
        }
      />
    );
  };

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
                      color:
                        runSettings.analyzerStatus === "online"
                          ? "#24a148"
                          : "#da1e28",
                    }}
                  >
                    ● {runSettings.analyzerStatus}
                  </span>
                  {" • "}
                  QC {runSettings.analyzerQcStatus === "pass" ? "Pass" : "Fail"}
                </div>
                <div
                  style={{
                    fontSize: "0.75rem",
                    color: "#525252",
                    marginTop: "0.5rem",
                  }}
                >
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
              <div
                style={{
                  fontSize: "0.75rem",
                  color: "#0f62fe",
                  marginTop: "0.5rem",
                }}
              >
                ⚡ FIFO lots pre-selected. Applied to all saved results.
              </div>
            </div>
          </Column>
        </Grid>
      </div>
    );
  };

  const renderReagentModal = () => {
    return (
      <Modal
        open={showReagentModal}
        onRequestClose={() => {
          setShowReagentModal(false);
          setFifoWarnings({});
        }}
        modalHeading="Select Reagent Lots (FIFO Recommended)"
        primaryButtonText="Apply to All Results"
        secondaryButtonText="Cancel"
        onRequestSubmit={handleApplyReagentLots}
        size="md"
      >
        <div style={{ padding: "1rem" }}>
          <InlineNotification
            kind="info"
            title="FIFO Protocol"
            subtitle="First-In-First-Out: Select the oldest lot (received date) to maintain inventory rotation."
            lowContrast
            hideCloseButton
            style={{ marginBottom: "1rem" }}
          />

          {Object.keys(availableReagentLots).map((reagentId) => {
            const lots = availableReagentLots[reagentId];
            const fifoLot = lots[0];

            return (
              <div
                key={reagentId}
                style={{
                  marginBottom: "1.5rem",
                  padding: "1rem",
                  border: "1px solid #e0e0e0",
                  borderRadius: "4px",
                }}
              >
                <h6 style={{ marginBottom: "0.75rem", fontWeight: "600" }}>
                  {fifoLot?.reagentName || reagentId}
                </h6>

                {lots.map((lot, index) => {
                  const isFifo = index === 0;
                  const isExpiring = lot.isExpiring;
                  const isSelected =
                    selectedReagentLots[reagentId] === lot.lotNumber;

                  return (
                    <div
                      key={lot.lotNumber}
                      style={{
                        padding: "0.75rem",
                        marginBottom: "0.5rem",
                        backgroundColor: isFifo ? "#e5f6ff" : "#ffffff",
                        border: isFifo
                          ? "2px solid #0f62fe"
                          : "1px solid #e0e0e0",
                        borderRadius: "4px",
                        position: "relative",
                      }}
                    >
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "0.5rem",
                        }}
                      >
                        <Checkbox
                          id={`lot-${reagentId}-${lot.lotNumber}`}
                          labelText={
                            <div>
                              <div style={{ fontWeight: "500" }}>
                                LOT: {lot.lotNumber}
                                {isFifo && (
                                  <Tag
                                    type="blue"
                                    size="sm"
                                    style={{ marginLeft: "0.5rem" }}
                                  >
                                    ⚡ FIFO
                                  </Tag>
                                )}
                                {isExpiring && (
                                  <Tag
                                    type="red"
                                    size="sm"
                                    style={{ marginLeft: "0.5rem" }}
                                  >
                                    Expiring Soon
                                  </Tag>
                                )}
                              </div>
                              <div
                                style={{
                                  fontSize: "0.75rem",
                                  color: "#525252",
                                  marginTop: "0.25rem",
                                }}
                              >
                                Received: {lot.receivedDate} | Expires:{" "}
                                {lot.expirationDate}
                              </div>
                            </div>
                          }
                          checked={isSelected}
                          onChange={(checked) =>
                            handleReagentLotChange(
                              reagentId,
                              lot.lotNumber,
                              checked,
                            )
                          }
                        />
                      </div>
                    </div>
                  );
                })}

                {fifoWarnings[reagentId] && (
                  <InlineNotification
                    kind="warning"
                    title="Non-FIFO Selection"
                    subtitle={fifoWarnings[reagentId]}
                    lowContrast
                    hideCloseButton
                    style={{ marginTop: "0.5rem" }}
                  />
                )}
              </div>
            );
          })}

          {Object.keys(availableReagentLots).length === 0 && (
            <div
              style={{ textAlign: "center", padding: "2rem", color: "#525252" }}
            >
              No reagent lots available. Please configure inventory.
            </div>
          )}
        </div>
      </Modal>
    );
  };

  const renderSummaryStats = () => {
    const pendingCount = results.filter((r) => r.status === "pending").length;
    const flaggedCount = results.filter(
      (r) => r.flags && r.flags.length > 0,
    ).length;
    const criticalCount = results.filter((r) =>
      r.flags?.includes("critical"),
    ).length;
    const nonConformingCount = results.filter((r) => r.nonConforming).length;

    return (
      <>
        <Tag>Total: {results.length}</Tag>
        <Tag type="blue">Pending: {pendingCount}</Tag>
        {flaggedCount > 0 && <Tag type="orange">Flagged: {flaggedCount}</Tag>}
        {criticalCount > 0 && <Tag type="red">Critical: {criticalCount}</Tag>}
        {nonConformingCount > 0 && (
          <Tag type="red">NC: {nonConformingCount}</Tag>
        )}
      </>
    );
  };

  const renderBulkActions = () => {
    const saveDisabled =
      overallQcStatus === "fail" || selectedRows.length === 0;

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
            checked={
              selectedRows.length === results.length && results.length > 0
            }
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

  const renderExpandedRow = (row) => {
    if (!row) {
      return (
        <div style={{ padding: "2rem", textAlign: "center", color: "#da1e28" }}>
          Unable to load row data
        </div>
      );
    }

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
                        <span style={{ color: "#525252" }}>
                          {hist.interpretation}
                        </span>
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
                      <Tag
                        type={
                          details.qcLinkage.status === "pass" ? "green" : "red"
                        }
                      >
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

            <TabPanel>
              <div style={{ padding: "1rem" }}>
                <h5>Analyzer</h5>
                <div style={{ marginTop: "0.5rem" }}>
                  <strong>
                    {details.analyzer?.name || runSettings?.analyzerName}
                  </strong>
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
                          Lot: {reagent.lotNumber} | Exp:{" "}
                          {reagent.expirationDate}
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

  const renderQaSidebar = () => {
    return (
      <div
        style={{
          padding: "1rem",
          backgroundColor: "#f4f4f4",
          height: "100%",
          maxHeight: "calc(100vh - 120px)",
          overflowY: "auto",
          position: "sticky",
          top: "80px",
        }}
      >
        {/* Current Run QC Status */}
        <div style={{ marginBottom: "1.5rem" }}>
          <h5
            style={{
              marginBottom: "0.75rem",
              fontSize: "0.875rem",
              fontWeight: "600",
            }}
          >
            Current Run Summary
          </h5>
          <div
            style={{
              border: "1px solid #e0e0e0",
              borderRadius: "4px",
              padding: "0.75rem",
              backgroundColor: "#ffffff",
            }}
          >
            <div style={{ marginBottom: "0.5rem" }}>
              <strong style={{ fontSize: "0.875rem" }}>QC Status:</strong>{" "}
              <span
                style={{
                  color:
                    overallQcStatus === "pass"
                      ? "#24a148"
                      : overallQcStatus === "fail"
                        ? "#da1e28"
                        : "#525252",
                  fontWeight: "600",
                }}
              >
                {overallQcStatus === "pass"
                  ? "Passed"
                  : overallQcStatus === "fail"
                    ? "Failed"
                    : "Pending"}
              </span>
            </div>
            {qcSamples.length > 0 && (
              <>
                <div
                  style={{
                    fontSize: "0.75rem",
                    color: "#525252",
                    marginBottom: "0.25rem",
                  }}
                >
                  Passed:{" "}
                  {qcSamples.filter((qc) => qc.qcStatus === "pass").length} /{" "}
                  {qcSamples.length}
                </div>
                <div style={{ fontSize: "0.75rem", color: "#da1e28" }}>
                  Failed:{" "}
                  {qcSamples.filter((qc) => qc.qcStatus === "fail").length}
                </div>
              </>
            )}

            {/* Analyzer Info */}
            {analyzerInfo && (
              <div
                style={{
                  marginTop: "0.75rem",
                  paddingTop: "0.75rem",
                  borderTop: "1px solid #e0e0e0",
                }}
              >
                <div
                  style={{
                    fontSize: "0.75rem",
                    color: "#525252",
                    marginBottom: "0.25rem",
                  }}
                >
                  <strong>Analyzer:</strong> {analyzerInfo.name}
                </div>
                <div
                  style={{
                    fontSize: "0.75rem",
                    color: "#525252",
                    marginBottom: "0.25rem",
                  }}
                >
                  <strong>Status:</strong> {analyzerInfo.status}
                </div>
                <div style={{ fontSize: "0.75rem", color: "#525252" }}>
                  <strong>Last Calibration:</strong>{" "}
                  {analyzerInfo.lastCalibration}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Recent QC History */}
        <div style={{ marginBottom: "1.5rem" }}>
          <h5
            style={{
              marginBottom: "0.75rem",
              fontSize: "0.875rem",
              fontWeight: "600",
            }}
          >
            Recent QC History
          </h5>
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
                <span>{hist.status === "pass" ? "PASS" : "FAIL"}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Reagent Status */}
        {reagentStatus.length > 0 && (
          <div>
            <h5
              style={{
                marginBottom: "0.75rem",
                fontSize: "0.875rem",
                fontWeight: "600",
              }}
            >
              Reagent Status
            </h5>
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
                      color:
                        reagent.status === "expiring" ? "#f1c21b" : "#525252",
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
      header: intl.formatMessage({
        id: "column.name.result",
        defaultMessage: "Result",
      }),
    },
    {
      key: "range",
      header: intl.formatMessage({
        id: "column.name.range",
        defaultMessage: "Range",
      }),
    },
    { key: "qc", header: "QC" },
    {
      key: "flags",
      header: intl.formatMessage({
        id: "column.name.flags",
        defaultMessage: "Flags",
      }),
    },
    {
      key: "interpretation",
      header: intl.formatMessage({
        id: "column.name.interpretation",
        defaultMessage: "Interpretation",
      }),
    },
    { key: "_originalResult", header: "" }, // Hidden column for row expansion
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
            color:
              result.flags?.includes("above-normal") ||
              result.flags?.includes("below-normal")
                ? "#f1c21b"
                : result.flags?.includes("critical")
                  ? "#da1e28"
                  : "#161616",
          }}
        >
          {formatResult(result.result, result.significantDigits)}
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
        QC{" "}
        {result.qcStatus === "pass"
          ? "PASS"
          : result.qcStatus === "fail"
            ? "FAIL"
            : "—"}
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
    <div style={{ padding: "1rem", maxWidth: "100%", overflowX: "hidden" }}>
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
                <FormattedMessage
                  id="button.refresh"
                  defaultMessage="Refresh"
                />
              </Button>
              <Button
                kind="ghost"
                renderIcon={Settings}
                onClick={() => {
                  fetchAvailableReagentLots();
                  setShowReagentModal(true);
                }}
              >
                <FormattedMessage
                  id="button.settings"
                  defaultMessage="Settings"
                />
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
              alignItems: "flex-start",
              marginBottom: "1rem",
              gap: "1rem",
              flexWrap: "wrap",
            }}
          >
            <ExpandableSearch
              size="lg"
              labelText="Search"
              placeholder="Search by lab number, test name, or patient"
              onChange={(e) => setSearchTerm(e.target.value)}
              value={searchTerm}
              style={{ flexGrow: 1, minWidth: "250px" }}
            />
            <div style={{ display: "flex", flexWrap: "wrap", gap: "0.5rem" }}>
              {renderSummaryStats()}
            </div>
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
                    {rows.map((row) => {
                      const originalRow = row.cells.find((c) =>
                        c.id.includes("_originalResult"),
                      )?.value;
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
                              handleRowExpand(
                                row.id,
                                originalRow,
                                !expandedRows[row.id],
                              )
                            }
                          >
                            {row.cells
                              .filter(
                                (cell) =>
                                  cell.info.header !== "_originalResult",
                              )
                              .map((cell) => (
                                <TableCell key={cell.id}>
                                  {cell.value}
                                </TableCell>
                              ))}
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

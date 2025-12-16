import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Tag,
  Loading,
  Dropdown,
  TextInput,
  TextArea,
  ProgressBar,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectRow,
  TableSelectAll,
  Modal,
  DatePicker,
  DatePickerInput,
} from "@carbon/react";
import {
  Download,
  DocumentExport,
  Email,
  FlagFilled,
  Checkmark,
  Warning,
  Help,
  Analytics,
  DataBase,
  User,
  Calendar,
  ArrowRight,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import FlagSampleModal from "../../workflow/FlagSampleModal";
import config from "../../../../config.json";
import "../../workflow/NotebookWorkflow.css";

/**
 * MNTDDataAnalysisPage - Page 9: Data Analysis & Export
 *
 * Purpose: Analyze validated data and prepare final outputs for the MNTD workflow.
 *
 * Who uses it:
 * - Data manager
 * - PI (Principal Investigator)
 *
 * Features:
 * - Analysis Details entry (software used, analyst name, analysis date, notes)
 * - View samples with validation status (Valid/Invalid/Inconclusive/Pending)
 * - Flag individual or bulk samples with validation status
 * - Export results to Excel/CSV formats (raw or processed data)
 * - Record result delivery to recipients
 * - Lock raw data (audit trail)
 * - View delivery history
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - Page configuration data
 * @param {Object} props.progress - Page progress info
 * @param {function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID
 */
function MNTDDataAnalysisPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookId: propNotebookId,
}) {
  const intl = useIntl();
  const componentMounted = useRef(true);

  // State
  const [loading, setLoading] = useState(true);
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Validation summary
  const [validationSummary, setValidationSummary] = useState({
    total: 0,
    valid: 0,
    invalid: 0,
    inconclusive: 0,
    pending: 0,
  });

  // Flag modal state
  const [flagModalOpen, setFlagModalOpen] = useState(false);

  // Export state
  const [exporting, setExporting] = useState(false);
  const [exportFormat, setExportFormat] = useState("excel");

  // Delivery state
  const [deliveryHistory, setDeliveryHistory] = useState([]);
  const [recipientName, setRecipientName] = useState("");
  const [recipientEmail, setRecipientEmail] = useState("");
  const [delivering, setDelivering] = useState(false);

  // Analysis details modal state
  const [analysisModalOpen, setAnalysisModalOpen] = useState(false);
  const [analysisDetails, setAnalysisDetails] = useState({
    softwareUsed: "",
    analystName: "",
    analysisDate: new Date().toISOString().slice(0, 10),
    notes: "",
  });
  const [savedAnalysisDetails, setSavedAnalysisDetails] = useState(null);
  const [savingAnalysis, setSavingAnalysis] = useState(false);

  // Send to reporting state
  const [sendingToReporting, setSendingToReporting] = useState(false);

  // Notebook ID (from parent context or fetched)
  const [notebookId, setNotebookId] = useState(propNotebookId);

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Load notebook ID from entry if not provided
  useEffect(() => {
    if (!propNotebookId && entryId) {
      getFromOpenElisServer(`/rest/notebook-entry/${entryId}`, (response) => {
        if (componentMounted.current && response?.notebook?.id) {
          setNotebookId(response.notebook.id);
        }
      });
    }
  }, [entryId, propNotebookId]);

  // Load samples with validation status
  const loadSamples = useCallback(() => {
    if (!hasRealPageId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    const url = `/rest/notebook/bulk/page/${pageData.id}/samples-with-validation`;
    getFromOpenElisServer(url, (response) => {
      if (componentMounted.current) {
        if (response && Array.isArray(response)) {
          setSamples(response);
        } else if (response && !response.error) {
          setSamples([]);
        }
        setLoading(false);
      }
    });
  }, [pageData?.id, hasRealPageId]);

  // Load validation summary
  const loadValidationSummary = useCallback(() => {
    if (!hasRealPageId) return;

    getFromOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/validation-summary`,
      (response) => {
        if (componentMounted.current && response && !response.error) {
          setValidationSummary(response);
        }
      },
    );
  }, [pageData?.id, hasRealPageId]);

  // Load delivery history
  const loadDeliveryHistory = useCallback(() => {
    if (!notebookId) return;

    getFromOpenElisServer(
      `/rest/notebook/bulk/notebook/${notebookId}/delivery-history`,
      (response) => {
        if (componentMounted.current && Array.isArray(response)) {
          setDeliveryHistory(response);
        }
      },
    );
  }, [notebookId]);

  // Load saved analysis details
  const loadAnalysisDetails = useCallback(() => {
    if (!hasRealPageId) return;

    getFromOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/progress`,
      (response) => {
        if (componentMounted.current && response) {
          // Check if analysis details were saved in page data
          if (response.analysisDetails) {
            setSavedAnalysisDetails(response.analysisDetails);
            setAnalysisDetails(response.analysisDetails);
          }
        }
      },
    );
  }, [pageData?.id, hasRealPageId]);

  // Load data on mount
  useEffect(() => {
    componentMounted.current = true;
    loadSamples();
    loadValidationSummary();
    loadAnalysisDetails();

    return () => {
      componentMounted.current = false;
    };
  }, [loadSamples, loadValidationSummary, loadAnalysisDetails]);

  // Load delivery history when notebookId is available
  useEffect(() => {
    if (notebookId) {
      loadDeliveryHistory();
    }
  }, [notebookId, loadDeliveryHistory]);

  // Handle flag success
  const handleFlagSuccess = (result) => {
    setSuccess(
      intl.formatMessage(
        {
          id: "notebook.mntd.analysis.flagSuccess",
          defaultMessage: "Successfully flagged {count} sample(s) as {status}",
        },
        { count: result.flaggedCount, status: result.status },
      ),
    );
    setSelectedSampleIds([]);
    loadSamples();
    loadValidationSummary();
  };

  // Handle save analysis details
  const handleSaveAnalysisDetails = async () => {
    if (!hasRealPageId) return;

    setSavingAnalysis(true);
    setError(null);

    try {
      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          credentials: "include",
          body: JSON.stringify({
            sampleIds: samples.map((s) => s.sampleItemId || s.id),
            data: {
              analysisDetails: analysisDetails,
            },
          }),
        },
      );

      const data = await response.json();

      if (response.ok && data.success) {
        setSavedAnalysisDetails(analysisDetails);
        setAnalysisModalOpen(false);
        setSuccess(
          intl.formatMessage({
            id: "notebook.mntd.analysis.detailsSaved",
            defaultMessage: "Analysis details saved successfully",
          }),
        );
        if (onProgressUpdate) onProgressUpdate();
      } else {
        setError(
          data.error ||
            intl.formatMessage({
              id: "notebook.mntd.analysis.error.saveFailed",
              defaultMessage: "Failed to save analysis details",
            }),
        );
      }
    } catch (err) {
      console.error("Save analysis details error:", err);
      setError(
        intl.formatMessage({
          id: "notebook.mntd.analysis.error.network",
          defaultMessage: "Network error",
        }),
      );
    } finally {
      setSavingAnalysis(false);
    }
  };

  // Handle export
  const handleExport = async (format, dataType = "processed") => {
    if (!notebookId) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.analysis.error.noNotebook",
          defaultMessage: "Notebook not found",
        }),
      );
      return;
    }

    setExporting(true);
    setError(null);

    try {
      const endpoint = `${config.serverBaseUrl}/rest/notebook/bulk/notebook/${notebookId}/export/${format}?dataType=${dataType}`;
      const response = await fetch(endpoint, {
        method: "GET",
        credentials: "include",
        headers: {
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
      });

      const contentType = response.headers.get("content-type") || "";
      const isExcelFile =
        contentType.includes(
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        ) || contentType.includes("application/vnd.ms-excel");
      const isCsvFile =
        contentType.includes("text/csv") ||
        contentType.includes("application/csv");

      if (
        response.ok &&
        ((format === "excel" && isExcelFile) ||
          (format === "csv" && isCsvFile) ||
          contentType.includes("application/octet-stream"))
      ) {
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = `mntd_${dataType}_results_${notebookId}.${format === "excel" ? "xlsx" : "csv"}`;
        document.body.appendChild(a);
        a.click();
        setTimeout(() => {
          window.URL.revokeObjectURL(url);
          document.body.removeChild(a);
        }, 100);

        setSuccess(
          intl.formatMessage({
            id: "notebook.mntd.analysis.exportSuccess",
            defaultMessage: "Results exported successfully",
          }),
        );
      } else {
        let errorMessage;
        try {
          if (contentType.includes("application/json")) {
            const data = await response.json();
            errorMessage = data.error;
          } else {
            const text = await response.text();
            if (text.includes("<html") || text.includes("<!DOCTYPE")) {
              errorMessage = `Authentication required (status ${response.status})`;
            } else {
              errorMessage =
                text.substring(0, 100) ||
                `Export failed with status ${response.status}`;
            }
          }
        } catch {
          errorMessage = `Export failed with status ${response.status}`;
        }
        setError(
          errorMessage ||
            intl.formatMessage({
              id: "notebook.mntd.analysis.error.exportFailed",
              defaultMessage: "Export failed",
            }),
        );
      }
    } catch (err) {
      console.error("Export error:", err);
      setError(
        intl.formatMessage({
          id: "notebook.mntd.analysis.error.network",
          defaultMessage: "Network error during export",
        }),
      );
    } finally {
      setExporting(false);
    }
  };

  // Handle delivery
  const handleRecordDelivery = async () => {
    if (!notebookId || !recipientName.trim()) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.analysis.error.recipientRequired",
          defaultMessage: "Recipient name is required",
        }),
      );
      return;
    }

    setDelivering(true);
    setError(null);

    try {
      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/bulk/notebook/${notebookId}/deliver`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          credentials: "include",
          body: JSON.stringify({
            recipientName: recipientName.trim(),
            recipientEmail: recipientEmail.trim() || null,
          }),
        },
      );

      const data = await response.json();

      if (response.ok && data.success) {
        setSuccess(
          intl.formatMessage({
            id: "notebook.mntd.analysis.deliverySuccess",
            defaultMessage: "Delivery recorded successfully",
          }),
        );
        setRecipientName("");
        setRecipientEmail("");
        loadDeliveryHistory();
      } else {
        setError(
          data.error ||
            intl.formatMessage({
              id: "notebook.mntd.analysis.error.deliveryFailed",
              defaultMessage: "Failed to record delivery",
            }),
        );
      }
    } catch (err) {
      console.error("Delivery error:", err);
      setError(
        intl.formatMessage({
          id: "notebook.mntd.analysis.error.network",
          defaultMessage: "Network error",
        }),
      );
    } finally {
      setDelivering(false);
    }
  };

  // Handle sending flagged samples to Reporting page (marks as COMPLETED to trigger T150)
  // Any sample with a flag (VALID, INVALID, or INCONCLUSIVE) can be sent
  const handleSendToReporting = useCallback(() => {
    if (!hasRealPageId) return;

    // If samples are selected, only send selected flagged samples
    // Otherwise, send all flagged samples not yet completed
    let samplesToSend;
    if (selectedSampleIds.length > 0) {
      // Filter selected samples that have ANY flag and are not COMPLETED
      samplesToSend = samples.filter(
        (s) =>
          selectedSampleIds.includes(String(s.id)) &&
          s.validationStatus &&
          s.validationStatus !== "PENDING" &&
          s.pageStatus !== "COMPLETED",
      );
    } else {
      // Send all flagged samples not yet completed
      samplesToSend = samples.filter(
        (s) =>
          s.validationStatus &&
          s.validationStatus !== "PENDING" &&
          s.pageStatus !== "COMPLETED",
      );
    }

    if (samplesToSend.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.analysis.error.noFlaggedSamples",
          defaultMessage:
            "No flagged samples to send. Flag samples first (VALID, INVALID, or INCONCLUSIVE).",
        }),
      );
      return;
    }

    setSendingToReporting(true);
    setError(null);

    const sampleIds = samplesToSend.map((s) =>
      parseInt(s.sampleItemId || s.id, 10),
    );

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: sampleIds,
        status: "COMPLETED",
      }),
      (response) => {
        if (componentMounted.current) {
          setSendingToReporting(false);
          if (response === 200 || (response && !response.error)) {
            setSuccess(
              intl.formatMessage(
                {
                  id: "notebook.mntd.analysis.sentToReporting",
                  defaultMessage: "{count} sample(s) sent to Reporting page.",
                },
                { count: samplesToSend.length },
              ),
            );
            setSelectedSampleIds([]);
            loadSamples();
            loadValidationSummary();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError(
              response?.error ||
                intl.formatMessage({
                  id: "notebook.mntd.analysis.error.sendFailed",
                  defaultMessage: "Failed to send samples to Reporting.",
                }),
            );
          }
        }
      },
    );
  }, [
    hasRealPageId,
    samples,
    selectedSampleIds,
    pageData?.id,
    loadSamples,
    loadValidationSummary,
    onProgressUpdate,
    intl,
  ]);

  // Count flagged samples available to send to reporting
  // If samples are selected, count selected flagged samples not yet COMPLETED
  // Otherwise, count all flagged samples not yet COMPLETED
  const flaggedNotCompletedCount =
    selectedSampleIds.length > 0
      ? samples.filter(
          (s) =>
            selectedSampleIds.includes(String(s.id)) &&
            s.validationStatus &&
            s.validationStatus !== "PENDING" &&
            s.pageStatus !== "COMPLETED",
        ).length
      : samples.filter(
          (s) =>
            s.validationStatus &&
            s.validationStatus !== "PENDING" &&
            s.pageStatus !== "COMPLETED",
        ).length;

  // Filter samples by validation status
  const filteredSamples = samples.filter((sample) => {
    if (statusFilter === "ALL") return true;
    return sample.validationStatus === statusFilter;
  });

  // Status filter options
  const statusFilterOptions = [
    {
      id: "ALL",
      label: intl.formatMessage({
        id: "notebook.mntd.analysis.filter.all",
        defaultMessage: "All",
      }),
    },
    {
      id: "VALID",
      label: intl.formatMessage({
        id: "notebook.mntd.analysis.filter.valid",
        defaultMessage: "Valid",
      }),
    },
    {
      id: "INVALID",
      label: intl.formatMessage({
        id: "notebook.mntd.analysis.filter.invalid",
        defaultMessage: "Invalid",
      }),
    },
    {
      id: "INCONCLUSIVE",
      label: intl.formatMessage({
        id: "notebook.mntd.analysis.filter.inconclusive",
        defaultMessage: "Inconclusive",
      }),
    },
    {
      id: "PENDING",
      label: intl.formatMessage({
        id: "notebook.mntd.analysis.filter.pending",
        defaultMessage: "Pending",
      }),
    },
  ];

  // Software options
  const softwareOptions = [
    { id: "excel", label: "Microsoft Excel" },
    { id: "r", label: "R / RStudio" },
    { id: "python", label: "Python (pandas/numpy)" },
    { id: "spss", label: "IBM SPSS" },
    { id: "stata", label: "Stata" },
    { id: "sas", label: "SAS" },
    { id: "prism", label: "GraphPad Prism" },
    { id: "other", label: "Other" },
  ];

  // Get validation status tag
  const getValidationTag = (sample) => {
    const color = sample?.validationColor || "gray";
    const displayName = sample?.validationDisplayName || "Pending";

    return (
      <Tag type={color === "gray" ? "cool-gray" : color}>{displayName}</Tag>
    );
  };

  // Get result summary from sample data
  const getResultSummary = (sample) => {
    if (!sample?.data) return "-";

    const data = sample.data;

    if (data.result) {
      const result = String(data.result);
      return result.length > 100 ? result.substring(0, 97) + "..." : result;
    }

    if (data.analyzerResult) {
      const result = String(data.analyzerResult);
      return result.length > 100 ? result.substring(0, 97) + "..." : result;
    }

    if (data.resultValue) {
      const result = String(data.resultValue);
      return result.length > 100 ? result.substring(0, 97) + "..." : result;
    }

    return "-";
  };

  // Calculate progress percentage
  const progressPercentage =
    validationSummary.total > 0
      ? Math.round(
          ((validationSummary.valid +
            validationSummary.invalid +
            validationSummary.inconclusive) /
            validationSummary.total) *
            100,
        )
      : 0;

  if (loading) {
    return (
      <div className="mntd-data-analysis-page">
        <Loading withOverlay={false} />
      </div>
    );
  }

  return (
    <div className="mntd-data-analysis-page">
      {/* Header */}
      <div className="page-section-header">
        <h4>
          <Analytics size={20} style={{ marginRight: "0.5rem" }} />
          <FormattedMessage
            id="notebook.mntd.analysis.title"
            defaultMessage="Data Analysis & Export"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.mntd.analysis.description"
            defaultMessage="Analyze validated data, record analysis details, and prepare final outputs for reporting."
          />
        </p>
      </div>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "label.error",
            defaultMessage: "Error",
          })}
          subtitle={error}
          onCloseButtonClick={() => setError(null)}
          lowContrast
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({
            id: "label.success",
            defaultMessage: "Success",
          })}
          subtitle={success}
          onCloseButtonClick={() => setSuccess(null)}
          lowContrast
        />
      )}

      {/* Analysis Details Section */}
      <div className="analysis-details-section">
        <Grid narrow>
          <Column lg={16} md={8} sm={4}>
            <Tile className="analysis-details-tile">
              <div className="tile-header">
                <h5>
                  <DataBase size={16} style={{ marginRight: "0.5rem" }} />
                  <FormattedMessage
                    id="notebook.mntd.analysis.analysisDetails"
                    defaultMessage="Analysis Details"
                  />
                </h5>
                <Button
                  kind="ghost"
                  size="sm"
                  onClick={() => setAnalysisModalOpen(true)}
                >
                  <FormattedMessage
                    id="notebook.mntd.analysis.editDetails"
                    defaultMessage="Edit Details"
                  />
                </Button>
              </div>

              {savedAnalysisDetails ? (
                <Grid narrow>
                  <Column lg={4} md={2} sm={2}>
                    <div className="detail-item">
                      <span className="detail-label">
                        <FormattedMessage
                          id="notebook.mntd.analysis.softwareUsed"
                          defaultMessage="Software Used"
                        />
                      </span>
                      <span className="detail-value">
                        {savedAnalysisDetails.softwareUsed || "-"}
                      </span>
                    </div>
                  </Column>
                  <Column lg={4} md={2} sm={2}>
                    <div className="detail-item">
                      <span className="detail-label">
                        <User size={14} style={{ marginRight: "0.25rem" }} />
                        <FormattedMessage
                          id="notebook.mntd.analysis.analystName"
                          defaultMessage="Analyst Name"
                        />
                      </span>
                      <span className="detail-value">
                        {savedAnalysisDetails.analystName || "-"}
                      </span>
                    </div>
                  </Column>
                  <Column lg={4} md={2} sm={2}>
                    <div className="detail-item">
                      <span className="detail-label">
                        <Calendar
                          size={14}
                          style={{ marginRight: "0.25rem" }}
                        />
                        <FormattedMessage
                          id="notebook.mntd.analysis.analysisDate"
                          defaultMessage="Analysis Date"
                        />
                      </span>
                      <span className="detail-value">
                        {savedAnalysisDetails.analysisDate || "-"}
                      </span>
                    </div>
                  </Column>
                  <Column lg={4} md={2} sm={2}>
                    <div className="detail-item">
                      <span className="detail-label">
                        <FormattedMessage
                          id="notebook.mntd.analysis.notes"
                          defaultMessage="Notes"
                        />
                      </span>
                      <span className="detail-value">
                        {savedAnalysisDetails.notes || "-"}
                      </span>
                    </div>
                  </Column>
                </Grid>
              ) : (
                <div className="no-details">
                  <FormattedMessage
                    id="notebook.mntd.analysis.noDetailsRecorded"
                    defaultMessage="No analysis details recorded yet. Click 'Edit Details' to add information."
                  />
                </div>
              )}
            </Tile>
          </Column>
        </Grid>
      </div>

      {/* Validation Summary Dashboard */}
      <div className="validation-dashboard">
        <Grid narrow>
          <Column lg={12} md={8} sm={4}>
            <div className="progress-section">
              <div className="progress-tiles">
                <Tile className="progress-tile">
                  <span className="progress-label">
                    <FormattedMessage
                      id="notebook.mntd.analysis.total"
                      defaultMessage="Total"
                    />
                  </span>
                  <span className="progress-value">
                    {validationSummary.total}
                  </span>
                </Tile>
                <Tile className="progress-tile valid">
                  <span className="progress-label">
                    <Checkmark size={16} />
                    <FormattedMessage
                      id="notebook.mntd.analysis.valid"
                      defaultMessage="Valid"
                    />
                  </span>
                  <span className="progress-value valid-value">
                    {validationSummary.valid}
                  </span>
                </Tile>
                <Tile className="progress-tile invalid">
                  <span className="progress-label">
                    <Warning size={16} />
                    <FormattedMessage
                      id="notebook.mntd.analysis.invalid"
                      defaultMessage="Invalid"
                    />
                  </span>
                  <span className="progress-value invalid-value">
                    {validationSummary.invalid}
                  </span>
                </Tile>
                <Tile className="progress-tile inconclusive">
                  <span className="progress-label">
                    <Help size={16} />
                    <FormattedMessage
                      id="notebook.mntd.analysis.inconclusive"
                      defaultMessage="Inconclusive"
                    />
                  </span>
                  <span className="progress-value inconclusive-value">
                    {validationSummary.inconclusive}
                  </span>
                </Tile>
                <Tile className="progress-tile pending">
                  <span className="progress-label">
                    <FormattedMessage
                      id="notebook.mntd.analysis.pending"
                      defaultMessage="Pending"
                    />
                  </span>
                  <span className="progress-value pending-value">
                    {validationSummary.pending}
                  </span>
                </Tile>
              </div>
              <ProgressBar
                label={intl.formatMessage(
                  {
                    id: "notebook.mntd.analysis.progress",
                    defaultMessage: "Validation Progress: {percent}%",
                  },
                  { percent: progressPercentage },
                )}
                value={progressPercentage}
                max={100}
              />
            </div>
          </Column>
        </Grid>
      </div>

      {/* Actions Bar */}
      <div className="page-actions-bar">
        <Dropdown
          id="status-filter"
          titleText=""
          label={intl.formatMessage({
            id: "notebook.mntd.analysis.filter.label",
            defaultMessage: "Filter by status",
          })}
          items={statusFilterOptions}
          itemToString={(item) => (item ? item.label : "")}
          selectedItem={statusFilterOptions.find((o) => o.id === statusFilter)}
          onChange={({ selectedItem }) =>
            setStatusFilter(selectedItem?.id || "ALL")
          }
          size="sm"
        />

        <Button
          kind="primary"
          size="sm"
          renderIcon={FlagFilled}
          onClick={() => setFlagModalOpen(true)}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="notebook.mntd.analysis.flagSelected"
            defaultMessage="Flag Selected ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={DocumentExport}
          onClick={() => handleExport("excel", "processed")}
          disabled={exporting || !notebookId}
        >
          <FormattedMessage
            id="notebook.mntd.analysis.exportProcessed"
            defaultMessage="Export Processed (Excel)"
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Download}
          onClick={() => handleExport("csv", "raw")}
          disabled={exporting || !notebookId}
        >
          <FormattedMessage
            id="notebook.mntd.analysis.exportRaw"
            defaultMessage="Export Raw (CSV)"
          />
        </Button>

        <Button
          kind="primary"
          size="sm"
          renderIcon={ArrowRight}
          onClick={handleSendToReporting}
          disabled={sendingToReporting || flaggedNotCompletedCount === 0}
        >
          {sendingToReporting ? (
            <FormattedMessage
              id="notebook.mntd.analysis.sending"
              defaultMessage="Sending..."
            />
          ) : (
            <FormattedMessage
              id="notebook.mntd.analysis.sendToReporting"
              defaultMessage="Send to Reporting ({count})"
              values={{ count: flaggedNotCompletedCount }}
            />
          )}
        </Button>
      </div>

      {/* Sample Table with Validation Status */}
      <div className="sample-grid-container">
        {filteredSamples.length > 0 ? (
          <DataTable
            rows={filteredSamples.map((sample) => ({
              id: String(sample.id),
              externalId: sample.externalId || "-",
              sampleType: sample.sampleType || "-",
              result: getResultSummary(sample),
              pageStatus: sample.pageStatus || "-",
              validationStatus: sample.validationStatus,
              validationDisplayName: sample.validationDisplayName,
              validationColor: sample.validationColor,
              validationReason: sample.validationReason || "-",
            }))}
            headers={[
              {
                key: "id",
                header: intl.formatMessage({
                  id: "notebook.mntd.analysis.column.sampleId",
                  defaultMessage: "Sample ID",
                }),
              },
              {
                key: "externalId",
                header: intl.formatMessage({
                  id: "notebook.mntd.analysis.column.externalId",
                  defaultMessage: "External ID",
                }),
              },
              {
                key: "sampleType",
                header: intl.formatMessage({
                  id: "notebook.mntd.analysis.column.sampleType",
                  defaultMessage: "Sample Type",
                }),
              },
              {
                key: "result",
                header: intl.formatMessage({
                  id: "notebook.mntd.analysis.column.result",
                  defaultMessage: "Result",
                }),
              },
              {
                key: "pageStatus",
                header: intl.formatMessage({
                  id: "notebook.mntd.analysis.column.status",
                  defaultMessage: "Status",
                }),
              },
              {
                key: "validationStatus",
                header: intl.formatMessage({
                  id: "notebook.mntd.analysis.column.validation",
                  defaultMessage: "Validation",
                }),
              },
              {
                key: "validationReason",
                header: intl.formatMessage({
                  id: "notebook.mntd.analysis.column.reason",
                  defaultMessage: "Reason",
                }),
              },
            ]}
            size="sm"
          >
            {({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
            }) => {
              const allRowIds = rows.map((row) => row.id);
              const allSelected =
                allRowIds.length > 0 &&
                allRowIds.every((id) => selectedSampleIds.includes(id));
              const someSelected =
                !allSelected &&
                allRowIds.some((id) => selectedSampleIds.includes(id));

              const handleSelectAll = () => {
                if (allSelected) {
                  setSelectedSampleIds((prev) =>
                    prev.filter((id) => !allRowIds.includes(id)),
                  );
                } else {
                  setSelectedSampleIds((prev) => {
                    const newSet = new Set(prev);
                    allRowIds.forEach((id) => newSet.add(id));
                    return Array.from(newSet);
                  });
                }
              };

              const handleRowSelect = (rowId) => {
                setSelectedSampleIds((prev) =>
                  prev.includes(rowId)
                    ? prev.filter((id) => id !== rowId)
                    : [...prev, rowId],
                );
              };

              return (
                <TableContainer>
                  <Table {...getTableProps()}>
                    <TableHead>
                      <TableRow>
                        <TableSelectAll
                          id="analysis-select-all"
                          name="analysis-select-all"
                          checked={allSelected}
                          indeterminate={someSelected}
                          onSelect={handleSelectAll}
                          ariaLabel={intl.formatMessage({
                            id: "notebook.mntd.analysis.selectAll",
                            defaultMessage: "Select all samples",
                          })}
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
                      {rows.map((row) => {
                        const sample = filteredSamples.find(
                          (s) => String(s.id) === row.id,
                        );
                        const isSelected = selectedSampleIds.includes(row.id);
                        return (
                          <TableRow key={row.id} {...getRowProps({ row })}>
                            <TableSelectRow
                              id={`analysis-select-row-${row.id}`}
                              name={`analysis-select-row-${row.id}`}
                              checked={isSelected}
                              onSelect={() => handleRowSelect(row.id)}
                              ariaLabel={intl.formatMessage(
                                {
                                  id: "notebook.mntd.analysis.selectRow",
                                  defaultMessage: "Select sample {id}",
                                },
                                { id: row.id },
                              )}
                            />
                            {row.cells.map((cell) => (
                              <TableCell key={cell.id}>
                                {cell.info.header === "validationStatus"
                                  ? getValidationTag(sample)
                                  : cell.value}
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
          </DataTable>
        ) : (
          <div className="empty-state">
            <FormattedMessage
              id="notebook.mntd.analysis.noSamples"
              defaultMessage="No samples found for this page."
            />
          </div>
        )}
      </div>

      {/* Delivery Section */}
      <div className="delivery-section">
        <h5>
          <Email size={16} style={{ marginRight: "0.5rem" }} />
          <FormattedMessage
            id="notebook.mntd.analysis.delivery.title"
            defaultMessage="Record Delivery"
          />
        </h5>
        <Grid narrow>
          <Column lg={4} md={4} sm={4}>
            <TextInput
              id="recipient-name"
              labelText={intl.formatMessage({
                id: "notebook.mntd.analysis.delivery.recipientName",
                defaultMessage: "Recipient Name",
              })}
              placeholder={intl.formatMessage({
                id: "notebook.mntd.analysis.delivery.recipientNamePlaceholder",
                defaultMessage: "e.g., Data Management Team, PI Name",
              })}
              value={recipientName}
              onChange={(e) => setRecipientName(e.target.value)}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <TextInput
              id="recipient-email"
              labelText={intl.formatMessage({
                id: "notebook.mntd.analysis.delivery.recipientEmail",
                defaultMessage: "Recipient Email (optional)",
              })}
              placeholder="email@example.com"
              value={recipientEmail}
              onChange={(e) => setRecipientEmail(e.target.value)}
              type="email"
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <Button
              kind="primary"
              size="sm"
              renderIcon={Email}
              onClick={handleRecordDelivery}
              disabled={delivering || !recipientName.trim()}
              style={{ marginTop: "1.5rem" }}
            >
              <FormattedMessage
                id="notebook.mntd.analysis.delivery.record"
                defaultMessage="Record Delivery"
              />
            </Button>
          </Column>
        </Grid>

        {/* Delivery History */}
        {deliveryHistory.length > 0 && (
          <div className="delivery-history">
            <h6>
              <FormattedMessage
                id="notebook.mntd.analysis.delivery.history"
                defaultMessage="Delivery History"
              />
            </h6>
            <DataTable
              rows={deliveryHistory.map((record, idx) => ({
                id: String(record.id || idx),
                recipientName: record.recipientName,
                recipientEmail: record.recipientEmail || "-",
                fileName: record.fileName || "-",
                deliveredAt: record.deliveredAt
                  ? new Date(record.deliveredAt).toLocaleString()
                  : "-",
                deliveredBy: record.deliveredBy || "-",
              }))}
              headers={[
                { key: "recipientName", header: "Recipient" },
                { key: "recipientEmail", header: "Email" },
                { key: "fileName", header: "File" },
                { key: "deliveredAt", header: "Delivered At" },
                { key: "deliveredBy", header: "Delivered By" },
              ]}
              size="sm"
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
                            <TableCell key={cell.id}>{cell.value}</TableCell>
                          ))}
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </DataTable>
          </div>
        )}
      </div>

      {/* Flag Sample Modal */}
      <FlagSampleModal
        open={flagModalOpen}
        onClose={() => setFlagModalOpen(false)}
        pageId={pageData?.id}
        selectedSamples={selectedSampleIds}
        onFlagSuccess={handleFlagSuccess}
      />

      {/* Analysis Details Modal */}
      <Modal
        open={analysisModalOpen}
        onRequestClose={() => setAnalysisModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.analysis.modal.title",
          defaultMessage: "Edit Analysis Details",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.mntd.analysis.modal.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "notebook.mntd.analysis.modal.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleSaveAnalysisDetails}
        primaryButtonDisabled={savingAnalysis}
      >
        <Grid narrow>
          <Column lg={8} md={4} sm={4}>
            <Dropdown
              id="software-used"
              titleText={intl.formatMessage({
                id: "notebook.mntd.analysis.softwareUsed",
                defaultMessage: "Software Used",
              })}
              label="Select software..."
              items={softwareOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={softwareOptions.find(
                (s) => s.id === analysisDetails.softwareUsed,
              )}
              onChange={({ selectedItem }) =>
                setAnalysisDetails((prev) => ({
                  ...prev,
                  softwareUsed: selectedItem?.label || "",
                }))
              }
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="analyst-name"
              labelText={intl.formatMessage({
                id: "notebook.mntd.analysis.analystName",
                defaultMessage: "Analyst Name",
              })}
              placeholder="Enter analyst name"
              value={analysisDetails.analystName}
              onChange={(e) =>
                setAnalysisDetails((prev) => ({
                  ...prev,
                  analystName: e.target.value,
                }))
              }
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div className="cds--form-item">
              <label className="cds--label">
                <FormattedMessage
                  id="notebook.mntd.analysis.analysisDate"
                  defaultMessage="Analysis Date"
                />
              </label>
              <input
                type="date"
                className="cds--text-input"
                value={analysisDetails.analysisDate}
                onChange={(e) =>
                  setAnalysisDetails((prev) => ({
                    ...prev,
                    analysisDate: e.target.value,
                  }))
                }
              />
            </div>
          </Column>
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="analysis-notes"
              labelText={intl.formatMessage({
                id: "notebook.mntd.analysis.notes",
                defaultMessage: "Notes",
              })}
              placeholder="Enter any analysis notes or observations..."
              value={analysisDetails.notes}
              onChange={(e) =>
                setAnalysisDetails((prev) => ({
                  ...prev,
                  notes: e.target.value,
                }))
              }
              rows={4}
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default MNTDDataAnalysisPage;

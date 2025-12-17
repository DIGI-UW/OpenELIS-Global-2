import { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Select,
  SelectItem,
  TextInput,
  TextArea,
  DatePicker,
  DatePickerInput,
  Modal,
  Tag,
  Loading,
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
  Dropdown,
  Toggle,
} from "@carbon/react";
import {
  Report,
  Download,
  Renew,
  CheckmarkFilled,
  Warning,
  Certificate,
  Email,
  FlagFilled,
  Help,
  Checkmark,
  Calendar,
  Microscope,
  DocumentExport,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import config from "../../../../config.json";
import "../../workflow/NotebookWorkflow.css";

/**
 * PathologyReportingPage - Page 6: Reporting & Performance Monitoring
 *
 * Pathology-specific reporting including:
 * - Result Validation (Reviewed/Verified/Signed-off)
 * - Case sign-off by pathologist
 * - Pathology Report generation
 * - Performance metrics (TAT, specimen rejection, assay success)
 * - Export and delivery functionality
 * - QC meeting documentation
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {number} props.notebookId - The notebook ID
 * @param {Object} props.pageData - Page configuration data
 * @param {Object} props.progress - Page progress info
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function PathologyReportingPage({
  entryId,
  notebookId,
  pageData,
  onProgressUpdate,
}) {
  const componentMounted = useRef(true);
  const intl = useIntl();

  // State
  const [loading, setLoading] = useState(true);
  const [samples, setSamples] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Validation summary state
  const [validationSummary, setValidationSummary] = useState({
    total: 0,
    reviewed: 0,
    verified: 0,
    signedOff: 0,
    pending: 0,
    requiresAttention: 0,
  });

  // Validation/Review modal state
  const [showValidationModal, setShowValidationModal] = useState(false);
  const [validationData, setValidationData] = useState({
    validationStatus: "",
    reviewer: "",
    reviewDate: new Date().toISOString().split("T")[0],
    pathologistSignOff: "",
    requiresAddendum: false,
    addendumReason: "",
    clinicalCorrelation: "",
    comments: "",
  });

  // Pathology Report generation modal state
  const [showReportModal, setShowReportModal] = useState(false);
  const [reportData, setReportData] = useState({
    reportType: "",
    reportPeriod: "",
    diagnosisSummary: "",
    clinicalFindings: "",
    signedBy: "",
    signedDate: new Date().toISOString().split("T")[0],
  });

  // Delivery state
  const [showDeliveryModal, setShowDeliveryModal] = useState(false);
  const [deliveryHistory, setDeliveryHistory] = useState([]);
  const [deliveryData, setDeliveryData] = useState({
    recipientName: "",
    recipientEmail: "",
    deliveryType: "internal",
    regulatoryBody: "",
    notes: "",
  });
  const [delivering, setDelivering] = useState(false);

  // QC Meeting modal
  const [qcMeetingModalOpen, setQcMeetingModalOpen] = useState(false);
  const [qcMeetingData, setQcMeetingData] = useState({
    meetingDate: "",
    attendees: "",
    issuesDiscussed: "",
    actionsRequired: "",
  });
  const [submitting, setSubmitting] = useState(false);

  // Export state
  const [exporting, setExporting] = useState(false);

  // Performance metrics
  const [metrics, setMetrics] = useState({
    totalSamples: 0,
    specimenRejectionRate: 0,
    assaySuccessRate: 0,
    averageTAT: 0,
    equipmentDowntimeHours: 0,
    pendingReview: 0,
    reviewed: 0,
    signedOff: 0,
    reportsGenerated: 0,
  });

  // Validation status options (pathology-specific)
  const validationStatuses = [
    { value: "PENDING", label: "Pending Review" },
    { value: "REVIEWED", label: "Reviewed - Awaiting Verification" },
    { value: "VERIFIED", label: "Verified - Awaiting Sign-off" },
    { value: "SIGNED_OFF", label: "Signed Off by Pathologist" },
    { value: "REQUIRES_ADDENDUM", label: "Requires Addendum" },
    { value: "REQUIRES_RECUT", label: "Requires Re-cut/Re-stain" },
    { value: "SECOND_OPINION", label: "Referred for Second Opinion" },
    { value: "FINALIZED", label: "Finalized & Released" },
    { value: "AMENDED", label: "Amended Report" },
  ];

  // Report type options (pathology-specific)
  const reportTypes = [
    { value: "histopathology", label: "Histopathology Report" },
    { value: "cytopathology", label: "Cytopathology Report" },
    { value: "surgical", label: "Surgical Pathology Report" },
    { value: "autopsy", label: "Autopsy Report" },
    { value: "ihc", label: "IHC/Special Stain Report" },
    { value: "molecular", label: "Molecular Pathology Report" },
    { value: "frozen_section", label: "Frozen Section Report" },
    { value: "summary", label: "Summary Report" },
    { value: "monthly_volume", label: "Monthly Specimen Volume" },
    { value: "tat", label: "Turnaround Time (TAT)" },
    { value: "rejection_rates", label: "Rejection Rates" },
  ];

  // Status filter options
  const statusFilterOptions = [
    { id: "ALL", label: "All Samples" },
    { id: "PENDING", label: "Pending Review" },
    { id: "REVIEWED", label: "Reviewed" },
    { id: "VERIFIED", label: "Verified" },
    { id: "SIGNED_OFF", label: "Signed Off" },
    { id: "REQUIRES_ADDENDUM", label: "Requires Addendum" },
    { id: "FINALIZED", label: "Finalized" },
  ];

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Load samples for this specific page
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
              // Pathology-specific data
              specimenType: sample.data?.specimenType,
              specimenSite: sample.data?.specimenSite,
              clinicalHistory: sample.data?.clinicalHistory,
              patientId: sample.data?.patientId,
              requestingClinician: sample.data?.requestingClinician,
              // Testing data from previous pages
              testName: sample.data?.testName,
              result: sample.data?.result,
              diagnosis: sample.data?.diagnosis,
              microscopicFindings: sample.data?.microscopicFindings,
              // Validation/Reporting data
              validationStatus: sample.data?.validationStatus || "PENDING",
              reviewer: sample.data?.reviewer || "",
              reviewDate: sample.data?.reviewDate || "",
              pathologistSignOff: sample.data?.pathologistSignOff || "",
              requiresAddendum: sample.data?.requiresAddendum || false,
              reportGenerated: sample.data?.reportGenerated || false,
              comments: sample.data?.comments || "",
            }));
            setSamples(transformedSamples);
            calculateMetrics(transformedSamples);
            calculateValidationSummary(transformedSamples);
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  // Load metrics from backend
  const loadMetrics = useCallback(() => {
    const nbId = notebookId || entryId;
    if (!nbId) return;

    getFromOpenElisServer(
      `/rest/notebook/pathology/metrics?entryId=${nbId}`,
      (response) => {
        if (componentMounted.current && response) {
          setMetrics((prev) => ({
            ...prev,
            specimenRejectionRate: response.specimenRejectionRate || 0,
            assaySuccessRate: response.assaySuccessRate || 0,
            averageTAT: response.averageTAT || 0,
            equipmentDowntimeHours: response.equipmentDowntimeHours || 0,
          }));
        }
      },
    );
  }, [notebookId, entryId]);

  // Calculate performance metrics from samples
  const calculateMetrics = (sampleData) => {
    const total = sampleData.length;
    if (total === 0) {
      setMetrics((prev) => ({
        ...prev,
        totalSamples: 0,
        pendingReview: 0,
        reviewed: 0,
        signedOff: 0,
        reportsGenerated: 0,
      }));
      return;
    }

    const pending = sampleData.filter(
      (s) => !s.validationStatus || s.validationStatus === "PENDING",
    ).length;
    const reviewed = sampleData.filter(
      (s) =>
        s.validationStatus === "REVIEWED" || s.validationStatus === "VERIFIED",
    ).length;
    const signedOff = sampleData.filter(
      (s) =>
        s.validationStatus === "SIGNED_OFF" ||
        s.validationStatus === "FINALIZED",
    ).length;
    const reportsCount = sampleData.filter((s) => s.reportGenerated).length;

    setMetrics((prev) => ({
      ...prev,
      totalSamples: total,
      pendingReview: pending,
      reviewed: reviewed,
      signedOff: signedOff,
      reportsGenerated: reportsCount,
    }));
  };

  // Calculate validation summary
  const calculateValidationSummary = (sampleData) => {
    const total = sampleData.length;
    const reviewed = sampleData.filter(
      (s) => s.validationStatus === "REVIEWED",
    ).length;
    const verified = sampleData.filter(
      (s) => s.validationStatus === "VERIFIED",
    ).length;
    const signedOff = sampleData.filter(
      (s) =>
        s.validationStatus === "SIGNED_OFF" ||
        s.validationStatus === "FINALIZED",
    ).length;
    const pending = sampleData.filter(
      (s) => !s.validationStatus || s.validationStatus === "PENDING",
    ).length;
    const requiresAttention = sampleData.filter(
      (s) =>
        s.validationStatus === "REQUIRES_ADDENDUM" ||
        s.validationStatus === "REQUIRES_RECUT" ||
        s.validationStatus === "SECOND_OPINION",
    ).length;

    setValidationSummary({
      total,
      reviewed,
      verified,
      signedOff,
      pending,
      requiresAttention,
    });
  };

  // Load delivery history
  const loadDeliveryHistory = useCallback(() => {
    const nbId = notebookId || entryId;
    if (!nbId) return;

    getFromOpenElisServer(
      `/rest/notebook/bulk/notebook/${nbId}/delivery-history`,
      (response) => {
        if (componentMounted.current && Array.isArray(response)) {
          setDeliveryHistory(response);
        }
      },
    );
  }, [notebookId, entryId]);

  useEffect(() => {
    componentMounted.current = true;
    setSelectedIds([]);
    setStatusFilter("ALL");
    setError(null);
    setSuccess(null);
    loadSamples();
    loadMetrics();
    loadDeliveryHistory();

    return () => {
      componentMounted.current = false;
    };
  }, [pageData?.id, loadSamples, loadMetrics, loadDeliveryHistory]);

  // Handle apply validation data
  const handleApplyValidation = () => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.reporting.noSamplesSelected",
          defaultMessage: "Please select samples to apply validation data",
        }),
      );
      return;
    }

    if (!validationData.validationStatus) {
      setError(
        intl.formatMessage({
          id: "pathology.reporting.validationRequired",
          defaultMessage: "Validation status is required",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setShowValidationModal(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: numericIds,
        data: {
          validationStatus: validationData.validationStatus,
          reviewer: validationData.reviewer,
          reviewDate: validationData.reviewDate,
          pathologistSignOff: validationData.pathologistSignOff,
          requiresAddendum: validationData.requiresAddendum,
          addendumReason: validationData.addendumReason,
          clinicalCorrelation: validationData.clinicalCorrelation,
          comments: validationData.comments,
        },
      }),
      (status, response) => {
        if (componentMounted.current) {
          if (status === 200) {
            // Update page status based on validation
            const newStatus =
              validationData.validationStatus === "FINALIZED" ||
              validationData.validationStatus === "SIGNED_OFF"
                ? "COMPLETED"
                : "IN_PROGRESS";

            postToOpenElisServer(
              `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
              JSON.stringify({
                sampleIds: numericIds,
                status: newStatus,
              }),
              () => {
                setSuccess(
                  intl.formatMessage(
                    {
                      id: "pathology.reporting.validationApplied",
                      defaultMessage:
                        "Validation data applied to {count} samples",
                    },
                    { count: selectedIds.length },
                  ),
                );
                setShowValidationModal(false);
                setSelectedIds([]);
                resetValidationForm();
                loadSamples();
                if (onProgressUpdate) {
                  onProgressUpdate();
                }
              },
            );
          } else {
            const errorData =
              typeof response === "string" ? JSON.parse(response) : response;
            setError(errorData?.error || "Failed to apply validation data");
          }
        }
      },
    );
  };

  const resetValidationForm = () => {
    setValidationData({
      validationStatus: "",
      reviewer: "",
      reviewDate: new Date().toISOString().split("T")[0],
      pathologistSignOff: "",
      requiresAddendum: false,
      addendumReason: "",
      clinicalCorrelation: "",
      comments: "",
    });
  };

  // Handle Pathology Report generation
  const handleGenerateReport = async () => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.reporting.noSamplesSelected",
          defaultMessage: "Please select samples to generate report",
        }),
      );
      return;
    }

    if (!reportData.reportType || !reportData.signedBy) {
      setError(
        intl.formatMessage({
          id: "pathology.reporting.reportFieldsRequired",
          defaultMessage: "Report type and signed by are required",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setShowReportModal(false);
      return;
    }

    setSubmitting(true);
    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    try {
      // Generate the pathology report
      const pdfResponse = await fetch(
        `${config.serverBaseUrl}/rest/notebook/pathology/report/generate`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          credentials: "include",
          body: JSON.stringify({
            entryId: entryId,
            pageId: pageData.id,
            sampleIds: numericIds,
            reportType: reportData.reportType,
            reportPeriod: reportData.reportPeriod,
            diagnosisSummary: reportData.diagnosisSummary,
            clinicalFindings: reportData.clinicalFindings,
            signedBy: reportData.signedBy,
            signedDate: reportData.signedDate,
          }),
        },
      );

      const contentType = pdfResponse.headers.get("content-type") || "";

      if (pdfResponse.ok) {
        // Mark samples as report generated
        postToOpenElisServer(
          `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
          JSON.stringify({
            sampleIds: numericIds,
            data: {
              reportGenerated: true,
              reportType: reportData.reportType,
              reportSignedBy: reportData.signedBy,
              reportSignedDate: reportData.signedDate,
            },
          }),
          () => {
            if (componentMounted.current) {
              setSuccess(
                intl.formatMessage(
                  {
                    id: "pathology.reporting.reportGenerated",
                    defaultMessage:
                      "Pathology report generated for {count} samples",
                  },
                  { count: selectedIds.length },
                ),
              );
              setShowReportModal(false);
              setSelectedIds([]);
              setReportData({
                reportType: "",
                reportPeriod: "",
                diagnosisSummary: "",
                clinicalFindings: "",
                signedBy: "",
                signedDate: new Date().toISOString().split("T")[0],
              });
              loadSamples();
              if (onProgressUpdate) {
                onProgressUpdate();
              }
            }
          },
        );
      } else {
        let errorMessage = "Failed to generate report";
        try {
          if (contentType.includes("application/json")) {
            const errorData = await pdfResponse.json();
            errorMessage = errorData.error || errorMessage;
          }
        } catch {
          // ignore parse error
        }
        setError(errorMessage);
      }
    } catch (err) {
      console.error("Report generation error:", err);
      setError("Network error during report generation");
    } finally {
      setSubmitting(false);
    }
  };

  // Handle export (Excel/CSV) - similar to pharma workflow
  const handleExport = async (format) => {
    const nbId = notebookId || entryId;
    if (!nbId) {
      setError(
        intl.formatMessage({
          id: "pathology.reporting.error.noNotebook",
          defaultMessage: "Notebook not found",
        }),
      );
      return;
    }

    setExporting(true);
    setError(null);

    try {
      const endpoint = `${config.serverBaseUrl}/rest/notebook/bulk/notebook/${nbId}/export/${format}`;
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
        a.download = `pathology_results_${nbId}.${format === "excel" ? "xlsx" : "csv"}`;
        document.body.appendChild(a);
        a.click();
        setTimeout(() => {
          window.URL.revokeObjectURL(url);
          document.body.removeChild(a);
        }, 100);

        setSuccess(
          intl.formatMessage({
            id: "pathology.reporting.exportSuccess",
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
        setError(errorMessage || "Export failed");
      }
    } catch (err) {
      console.error("Export error:", err);
      setError("Network error during export");
    } finally {
      setExporting(false);
    }
  };

  // Handle delivery
  const handleRecordDelivery = async () => {
    const nbId = notebookId || entryId;
    if (!nbId || !deliveryData.recipientName.trim()) {
      setError(
        intl.formatMessage({
          id: "pathology.reporting.error.recipientRequired",
          defaultMessage: "Recipient name is required",
        }),
      );
      return;
    }

    setDelivering(true);
    setError(null);

    try {
      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/bulk/notebook/${nbId}/deliver`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          credentials: "include",
          body: JSON.stringify({
            recipientName: deliveryData.recipientName.trim(),
            recipientEmail: deliveryData.recipientEmail.trim() || null,
            deliveryType: deliveryData.deliveryType || "internal",
            regulatoryBody: deliveryData.regulatoryBody || null,
            notes: deliveryData.notes || null,
          }),
        },
      );

      const data = await response.json();

      if (response.ok && data.success) {
        setSuccess(
          intl.formatMessage({
            id: "pathology.reporting.deliverySuccess",
            defaultMessage: "Delivery recorded successfully",
          }),
        );
        setShowDeliveryModal(false);
        setDeliveryData({
          recipientName: "",
          recipientEmail: "",
          deliveryType: "internal",
          regulatoryBody: "",
          notes: "",
        });
        loadDeliveryHistory();
      } else {
        setError(data.error || "Failed to record delivery");
      }
    } catch (err) {
      console.error("Delivery error:", err);
      setError("Network error");
    } finally {
      setDelivering(false);
    }
  };

  // Handle QC meeting save
  const handleSaveQcMeeting = () => {
    if (!qcMeetingData.meetingDate) {
      setError("Please select meeting date");
      return;
    }
    setSubmitting(true);
    postToOpenElisServer(
      `/rest/notebook/pathology/qc-meeting`,
      JSON.stringify({ ...qcMeetingData, entryId, pageId: pageData?.id }),
      (status) => {
        setSubmitting(false);
        if (status === 200) {
          setQcMeetingModalOpen(false);
          setSuccess(
            intl.formatMessage({
              id: "pathology.reporting.qcMeetingSaved",
              defaultMessage: "QC meeting documented successfully",
            }),
          );
          setQcMeetingData({
            meetingDate: "",
            attendees: "",
            issuesDiscussed: "",
            actionsRequired: "",
          });
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError("Failed to save QC meeting. Please try again.");
        }
      },
    );
  };

  // Handle mark samples complete
  const handleMarkComplete = () => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.reporting.noSamplesSelected",
          defaultMessage: "Please select samples to mark as complete",
        }),
      );
      return;
    }

    if (!hasRealPageId) return;

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({ sampleIds: numericIds, status: "COMPLETED" }),
      (response) => {
        if (componentMounted.current) {
          if (response && response.success) {
            setSuccess(
              intl.formatMessage(
                {
                  id: "pathology.reporting.markedComplete",
                  defaultMessage: "Marked {count} samples as complete",
                },
                { count: selectedIds.length },
              ),
            );
            setSelectedIds([]);
            loadSamples();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError(response?.error || "Failed to update status");
          }
        }
      },
    );
  };

  // Filter samples
  const filteredSamples = samples.filter((sample) => {
    if (statusFilter === "ALL") return true;
    return sample.validationStatus === statusFilter;
  });

  // Get validation status tag
  const getValidationTag = (sample) => {
    const statusConfig = {
      REVIEWED: { type: "blue", icon: Checkmark },
      VERIFIED: { type: "cyan", icon: Checkmark },
      SIGNED_OFF: { type: "green", icon: CheckmarkFilled },
      FINALIZED: { type: "green", icon: CheckmarkFilled },
      REQUIRES_ADDENDUM: { type: "magenta", icon: Warning },
      REQUIRES_RECUT: { type: "purple", icon: Warning },
      SECOND_OPINION: { type: "teal", icon: Help },
      AMENDED: { type: "cyan", icon: Report },
      PENDING: { type: "gray", icon: null },
    };

    const cfg = statusConfig[sample.validationStatus] || statusConfig.PENDING;
    const Icon = cfg.icon;

    return (
      <Tag type={cfg.type} size="sm" renderIcon={Icon}>
        {sample.validationStatus || "Pending"}
      </Tag>
    );
  };

  // Get result summary tags
  const renderResultSummary = (sample) => {
    const tags = [];
    if (sample.diagnosis) {
      tags.push(
        <Tag key="diag" type="blue" size="sm" renderIcon={Microscope}>
          Diagnosis
        </Tag>,
      );
    }
    if (sample.requiresAddendum) {
      tags.push(
        <Tag key="addendum" type="magenta" size="sm">
          Addendum
        </Tag>,
      );
    }
    if (sample.reportGenerated) {
      tags.push(
        <Tag key="report" type="cyan" size="sm" renderIcon={Report}>
          Report
        </Tag>,
      );
    }
    if (sample.pathologistSignOff) {
      tags.push(
        <Tag key="signoff" type="green" size="sm" renderIcon={Certificate}>
          Signed
        </Tag>,
      );
    }
    return tags.length > 0 ? (
      <div style={{ display: "flex", gap: "4px", flexWrap: "wrap" }}>
        {tags}
      </div>
    ) : (
      <span style={{ color: "#8d8d8d" }}>-</span>
    );
  };

  // Calculate progress percentage
  const progressPercentage =
    validationSummary.total > 0
      ? Math.round(
          ((validationSummary.signedOff + validationSummary.verified) /
            validationSummary.total) *
            100,
        )
      : 0;

  const handleInputChange = (e, setState) => {
    const { name, value } = e.target;
    setState((prev) => ({ ...prev, [name]: value }));
  };

  const handleDateChange = (dates, fieldName, setState) => {
    if (dates?.[0]) {
      setState((prev) => ({
        ...prev,
        [fieldName]: dates[0].toISOString().split("T")[0],
      }));
    }
  };

  if (loading) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <Loading withOverlay={false} description="Loading samples..." />
      </div>
    );
  }

  return (
    <div className="pathology-reporting-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="pathology.page.reporting.title"
            defaultMessage="Reporting &amp; Performance Monitoring"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="pathology.page.reporting.description"
            defaultMessage="Review and validate pathology results, generate reports, track laboratory performance metrics, and document QC meetings."
          />
        </p>
      </div>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onCloseButtonClick={() => setError(null)}
          style={{ marginBottom: "1rem" }}
          lowContrast
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title={success}
          onCloseButtonClick={() => setSuccess(null)}
          style={{ marginBottom: "1rem" }}
          lowContrast
        />
      )}

      {/* Validation Summary Dashboard */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.reporting.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{validationSummary.total}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.reporting.pendingReview"
                  defaultMessage="Pending Review"
                />
              </span>
              <span className="progress-value">
                {validationSummary.pending}
              </span>
            </Tile>
            <Tile className="progress-tile" style={{ borderColor: "#0072c3" }}>
              <span className="progress-label" style={{ color: "#0072c3" }}>
                <Checkmark size={16} />
                <FormattedMessage
                  id="pathology.reporting.reviewed"
                  defaultMessage="Reviewed"
                />
              </span>
              <span className="progress-value" style={{ color: "#0072c3" }}>
                {validationSummary.reviewed}
              </span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <CheckmarkFilled size={16} />
                <FormattedMessage
                  id="pathology.reporting.signedOff"
                  defaultMessage="Signed Off"
                />
              </span>
              <span className="progress-value">
                {validationSummary.signedOff}
              </span>
            </Tile>
            {validationSummary.requiresAttention > 0 && (
              <Tile
                className="progress-tile"
                style={{ borderColor: "#da1e28" }}
              >
                <span className="progress-label" style={{ color: "#da1e28" }}>
                  <Warning size={16} />
                  <FormattedMessage
                    id="pathology.reporting.requiresAttention"
                    defaultMessage="Requires Attention"
                  />
                </span>
                <span className="progress-value" style={{ color: "#da1e28" }}>
                  {validationSummary.requiresAttention}
                </span>
              </Tile>
            )}
          </div>
          <ProgressBar
            label={intl.formatMessage(
              {
                id: "pathology.reporting.progress",
                defaultMessage: "Sign-off Progress: {percent}%",
              },
              { percent: progressPercentage },
            )}
            value={progressPercentage}
            max={100}
            style={{ marginTop: "0.5rem" }}
          />
        </Column>
      </Grid>

      {/* Performance Metrics Row */}
      <Grid fullWidth style={{ marginTop: "1rem" }}>
        <Column lg={4} md={2} sm={2}>
          <Tile className="metric-tile">
            <span className="metric-label">Specimen Rejection Rate</span>
            <span className="metric-value">
              {metrics.specimenRejectionRate}%
            </span>
            <ProgressBar
              value={100 - metrics.specimenRejectionRate}
              max={100}
              size="small"
              status={metrics.specimenRejectionRate <= 5 ? "active" : "error"}
            />
          </Tile>
        </Column>
        <Column lg={4} md={2} sm={2}>
          <Tile className="metric-tile">
            <span className="metric-label">Assay Success Rate</span>
            <span className="metric-value">{metrics.assaySuccessRate}%</span>
            <ProgressBar
              value={metrics.assaySuccessRate}
              max={100}
              size="small"
            />
          </Tile>
        </Column>
        <Column lg={4} md={2} sm={2}>
          <Tile className="metric-tile">
            <span className="metric-label">Average TAT (hours)</span>
            <span
              className="metric-value"
              style={{ color: metrics.averageTAT > 48 ? "#da1e28" : "#198038" }}
            >
              {metrics.averageTAT}
            </span>
          </Tile>
        </Column>
        <Column lg={4} md={2} sm={2}>
          <Tile className="metric-tile">
            <span className="metric-label">Reports Generated</span>
            <span className="metric-value">{metrics.reportsGenerated}</span>
          </Tile>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar" style={{ marginTop: "1rem" }}>
        <Dropdown
          id="status-filter"
          titleText=""
          label="Filter by status"
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
          onClick={() => setShowValidationModal(true)}
          disabled={selectedIds.length === 0}
        >
          <FormattedMessage
            id="pathology.reporting.applyValidation"
            defaultMessage="Apply Review ({count})"
            values={{ count: selectedIds.length }}
          />
        </Button>

        {/* Generate Report button commented out for now
        <Button
          kind="secondary"
          size="sm"
          renderIcon={Report}
          onClick={() => setShowReportModal(true)}
          disabled={selectedIds.length === 0}
        >
          <FormattedMessage
            id="pathology.reporting.generateReport"
            defaultMessage="Generate Report"
          />
        </Button>
        */}

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={CheckmarkFilled}
          onClick={handleMarkComplete}
          disabled={selectedIds.length === 0}
        >
          <FormattedMessage
            id="pathology.reporting.markComplete"
            defaultMessage="Mark Complete"
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={DocumentExport}
          onClick={() => handleExport("excel")}
          disabled={exporting || samples.length === 0}
        >
          <FormattedMessage
            id="pathology.reporting.exportExcel"
            defaultMessage="Export Excel"
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Download}
          onClick={() => handleExport("csv")}
          disabled={exporting || samples.length === 0}
        >
          <FormattedMessage
            id="pathology.reporting.exportCsv"
            defaultMessage="Export CSV"
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Email}
          onClick={() => setShowDeliveryModal(true)}
          disabled={samples.length === 0}
        >
          <FormattedMessage
            id="pathology.reporting.recordDelivery"
            defaultMessage="Record Delivery"
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Calendar}
          onClick={() => setQcMeetingModalOpen(true)}
        >
          <FormattedMessage
            id="pathology.reporting.documentQcMeeting"
            defaultMessage="QC Meeting"
          />
        </Button>

        <Button kind="ghost" size="sm" renderIcon={Renew} onClick={loadSamples}>
          <FormattedMessage
            id="pathology.reporting.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Sample Table */}
      <div className="sample-grid-container" style={{ marginTop: "1rem" }}>
        {filteredSamples.length > 0 ? (
          <DataTable
            rows={filteredSamples.map((sample) => ({
              id: sample.id,
              accessionNumber:
                sample.accessionNumber || sample.externalId || "-",
              specimenType: sample.specimenType || sample.sampleType || "-",
              specimenSite: sample.specimenSite || "-",
              testName: sample.testName || "-",
              status: sample.status,
              validationStatus: sample.validationStatus,
              reviewer: sample.reviewer || "-",
            }))}
            headers={[
              { key: "accessionNumber", header: "Accession #" },
              { key: "specimenType", header: "Specimen Type" },
              { key: "specimenSite", header: "Site" },
              { key: "testName", header: "Test" },
              { key: "status", header: "Page Status" },
              { key: "validationStatus", header: "Validation" },
              { key: "reviewer", header: "Reviewer" },
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
                allRowIds.every((id) => selectedIds.includes(id));
              const someSelected =
                !allSelected &&
                allRowIds.some((id) => selectedIds.includes(id));

              const handleSelectAll = () => {
                if (allSelected) {
                  setSelectedIds((prev) =>
                    prev.filter((id) => !allRowIds.includes(id)),
                  );
                } else {
                  setSelectedIds((prev) => {
                    const newSet = new Set(prev);
                    allRowIds.forEach((id) => newSet.add(id));
                    return Array.from(newSet);
                  });
                }
              };

              const handleRowSelect = (rowId) => {
                setSelectedIds((prev) =>
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
                          id="pathology-reporting-select-all"
                          name="pathology-reporting-select-all"
                          checked={allSelected}
                          indeterminate={someSelected}
                          onSelect={handleSelectAll}
                          ariaLabel="Select all samples"
                        />
                        {headers.map((header) => (
                          <TableHeader
                            key={header.key}
                            {...getHeaderProps({ header })}
                          >
                            {header.header}
                          </TableHeader>
                        ))}
                        <TableHeader>Summary</TableHeader>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((row) => {
                        const sample = filteredSamples.find(
                          (s) => s.id === row.id,
                        );
                        const isSelected = selectedIds.includes(row.id);
                        return (
                          <TableRow key={row.id} {...getRowProps({ row })}>
                            <TableSelectRow
                              id={`pathology-reporting-select-row-${row.id}`}
                              name={`pathology-reporting-select-row-${row.id}`}
                              checked={isSelected}
                              onSelect={() => handleRowSelect(row.id)}
                              ariaLabel={`Select sample ${row.id}`}
                            />
                            {row.cells.map((cell) => (
                              <TableCell key={cell.id}>
                                {cell.info.header === "validationStatus"
                                  ? getValidationTag(sample)
                                  : cell.value}
                              </TableCell>
                            ))}
                            <TableCell>{renderResultSummary(sample)}</TableCell>
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
              id="pathology.page.reporting.empty"
              defaultMessage="No samples available for reporting. Complete Testing & Storage first."
            />
          </div>
        )}
      </div>

      {/* Delivery History */}
      {deliveryHistory.length > 0 && (
        <div className="delivery-section" style={{ marginTop: "2rem" }}>
          <h5>
            <FormattedMessage
              id="pathology.reporting.deliveryHistory"
              defaultMessage="Delivery History"
            />
          </h5>
          <DataTable
            rows={deliveryHistory.map((record, idx) => ({
              id: String(record.id || idx),
              recipientName: record.recipientName,
              recipientEmail: record.recipientEmail || "-",
              deliveryType: record.deliveryType || "Internal",
              deliveredAt: record.deliveredAt
                ? new Date(record.deliveredAt).toLocaleString()
                : "-",
              deliveredBy: record.deliveredBy || "-",
            }))}
            headers={[
              { key: "recipientName", header: "Recipient" },
              { key: "recipientEmail", header: "Email" },
              { key: "deliveryType", header: "Type" },
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

      {/* Validation Modal */}
      <Modal
        open={showValidationModal}
        onRequestClose={() => setShowValidationModal(false)}
        onRequestSubmit={handleApplyValidation}
        modalHeading={intl.formatMessage({
          id: "pathology.reporting.validationModal.title",
          defaultMessage: "Apply Review & Validation Data",
        })}
        primaryButtonText={intl.formatMessage({
          id: "pathology.reporting.apply",
          defaultMessage: "Apply",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        size="md"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <p style={{ color: "#525252" }}>
            <FormattedMessage
              id="pathology.reporting.applyToSelected"
              defaultMessage="Applying validation data to {count} selected samples."
              values={{ count: selectedIds.length }}
            />
          </p>

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="validation-status"
                labelText={intl.formatMessage({
                  id: "pathology.reporting.validationStatus",
                  defaultMessage: "Validation Status *",
                })}
                value={validationData.validationStatus}
                onChange={(e) =>
                  setValidationData({
                    ...validationData,
                    validationStatus: e.target.value,
                    requiresAddendum: e.target.value === "REQUIRES_ADDENDUM",
                  })
                }
              >
                <SelectItem value="" text="Select status..." />
                {validationStatuses.map((status) => (
                  <SelectItem
                    key={status.value}
                    value={status.value}
                    text={status.label}
                  />
                ))}
              </Select>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="reviewer"
                labelText={intl.formatMessage({
                  id: "pathology.reporting.reviewer",
                  defaultMessage: "Reviewer / Technician",
                })}
                value={validationData.reviewer}
                onChange={(e) =>
                  setValidationData({
                    ...validationData,
                    reviewer: e.target.value,
                  })
                }
              />
            </Column>
          </Grid>

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <DatePicker
                datePickerType="single"
                onChange={([date]) =>
                  setValidationData({
                    ...validationData,
                    reviewDate: date?.toISOString().split("T")[0] || "",
                  })
                }
              >
                <DatePickerInput
                  id="review-date"
                  labelText={intl.formatMessage({
                    id: "pathology.reporting.reviewDate",
                    defaultMessage: "Review Date",
                  })}
                  placeholder="mm/dd/yyyy"
                />
              </DatePicker>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="pathologist-signoff"
                labelText={intl.formatMessage({
                  id: "pathology.reporting.pathologistSignOff",
                  defaultMessage: "Pathologist Sign-off",
                })}
                value={validationData.pathologistSignOff}
                onChange={(e) =>
                  setValidationData({
                    ...validationData,
                    pathologistSignOff: e.target.value,
                  })
                }
                placeholder="Pathologist name for final sign-off"
              />
            </Column>
          </Grid>

          <Grid fullWidth narrow>
            <Column lg={16} md={8} sm={4}>
              <Toggle
                id="requires-addendum"
                labelText={intl.formatMessage({
                  id: "pathology.reporting.requiresAddendum",
                  defaultMessage: "Requires Addendum",
                })}
                toggled={validationData.requiresAddendum}
                onToggle={(checked) =>
                  setValidationData({
                    ...validationData,
                    requiresAddendum: checked,
                  })
                }
                labelA="No"
                labelB="Yes"
              />
            </Column>
          </Grid>

          {validationData.requiresAddendum && (
            <TextInput
              id="addendum-reason"
              labelText={intl.formatMessage({
                id: "pathology.reporting.addendumReason",
                defaultMessage: "Addendum Reason",
              })}
              value={validationData.addendumReason}
              onChange={(e) =>
                setValidationData({
                  ...validationData,
                  addendumReason: e.target.value,
                })
              }
              placeholder="Reason for addendum..."
            />
          )}

          <TextArea
            id="clinical-correlation"
            labelText={intl.formatMessage({
              id: "pathology.reporting.clinicalCorrelation",
              defaultMessage: "Clinical Correlation",
            })}
            value={validationData.clinicalCorrelation}
            onChange={(e) =>
              setValidationData({
                ...validationData,
                clinicalCorrelation: e.target.value,
              })
            }
            placeholder="Note any clinical correlation..."
            rows={2}
          />

          <TextArea
            id="comments"
            labelText={intl.formatMessage({
              id: "pathology.reporting.comments",
              defaultMessage: "Comments / Notes",
            })}
            value={validationData.comments}
            onChange={(e) =>
              setValidationData({
                ...validationData,
                comments: e.target.value,
              })
            }
            placeholder="Enter review comments, observations, etc."
            rows={3}
          />
        </div>
      </Modal>

      {/* Pathology Report Generation Modal */}
      <Modal
        open={showReportModal}
        onRequestClose={() => setShowReportModal(false)}
        onRequestSubmit={handleGenerateReport}
        modalHeading={intl.formatMessage({
          id: "pathology.reporting.reportModal.title",
          defaultMessage: "Generate Pathology Report",
        })}
        primaryButtonText={
          submitting
            ? intl.formatMessage({
                id: "label.generating",
                defaultMessage: "Generating...",
              })
            : intl.formatMessage({
                id: "pathology.reporting.generateReport",
                defaultMessage: "Generate Report",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={submitting}
        size="md"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <p style={{ color: "#525252" }}>
            <FormattedMessage
              id="pathology.reporting.reportDescription"
              defaultMessage="Generate pathology report for {count} selected samples."
              values={{ count: selectedIds.length }}
            />
          </p>

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="report-type"
                labelText={intl.formatMessage({
                  id: "pathology.reporting.reportType",
                  defaultMessage: "Report Type *",
                })}
                value={reportData.reportType}
                onChange={(e) =>
                  setReportData({ ...reportData, reportType: e.target.value })
                }
              >
                <SelectItem value="" text="Select report type..." />
                {reportTypes.map((type) => (
                  <SelectItem
                    key={type.value}
                    value={type.value}
                    text={type.label}
                  />
                ))}
              </Select>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="report-period"
                labelText={intl.formatMessage({
                  id: "pathology.reporting.reportPeriod",
                  defaultMessage: "Report Period",
                })}
                value={reportData.reportPeriod}
                onChange={(e) =>
                  setReportData({ ...reportData, reportPeriod: e.target.value })
                }
                placeholder="e.g., January 2025"
              />
            </Column>
          </Grid>

          <TextArea
            id="diagnosis-summary"
            labelText={intl.formatMessage({
              id: "pathology.reporting.diagnosisSummary",
              defaultMessage: "Diagnosis Summary",
            })}
            value={reportData.diagnosisSummary}
            onChange={(e) =>
              setReportData({ ...reportData, diagnosisSummary: e.target.value })
            }
            placeholder="Enter diagnosis summary..."
            rows={3}
          />

          <TextArea
            id="clinical-findings"
            labelText={intl.formatMessage({
              id: "pathology.reporting.clinicalFindings",
              defaultMessage: "Clinical Findings",
            })}
            value={reportData.clinicalFindings}
            onChange={(e) =>
              setReportData({
                ...reportData,
                clinicalFindings: e.target.value,
              })
            }
            placeholder="Enter clinical findings..."
            rows={3}
          />

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="signed-by"
                labelText={intl.formatMessage({
                  id: "pathology.reporting.signedBy",
                  defaultMessage: "Signed By (Pathologist) *",
                })}
                value={reportData.signedBy}
                onChange={(e) =>
                  setReportData({ ...reportData, signedBy: e.target.value })
                }
                required
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <DatePicker
                datePickerType="single"
                onChange={([date]) =>
                  setReportData({
                    ...reportData,
                    signedDate: date?.toISOString().split("T")[0] || "",
                  })
                }
              >
                <DatePickerInput
                  id="signed-date"
                  labelText={intl.formatMessage({
                    id: "pathology.reporting.signedDate",
                    defaultMessage: "Signed Date",
                  })}
                  placeholder="mm/dd/yyyy"
                />
              </DatePicker>
            </Column>
          </Grid>
        </div>
      </Modal>

      {/* Delivery Modal */}
      <Modal
        open={showDeliveryModal}
        onRequestClose={() => setShowDeliveryModal(false)}
        onRequestSubmit={handleRecordDelivery}
        modalHeading={intl.formatMessage({
          id: "pathology.reporting.deliveryModal.title",
          defaultMessage: "Record Report Delivery",
        })}
        primaryButtonText={
          delivering
            ? intl.formatMessage({
                id: "label.recording",
                defaultMessage: "Recording...",
              })
            : intl.formatMessage({
                id: "pathology.reporting.recordDelivery",
                defaultMessage: "Record Delivery",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={delivering}
        size="md"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="delivery-recipient"
                labelText={intl.formatMessage({
                  id: "pathology.reporting.recipientName",
                  defaultMessage: "Recipient Name *",
                })}
                value={deliveryData.recipientName}
                onChange={(e) =>
                  setDeliveryData({
                    ...deliveryData,
                    recipientName: e.target.value,
                  })
                }
                placeholder="e.g., Dr. Smith, Records Dept"
                required
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="delivery-email"
                labelText={intl.formatMessage({
                  id: "pathology.reporting.recipientEmail",
                  defaultMessage: "Recipient Email",
                })}
                value={deliveryData.recipientEmail}
                onChange={(e) =>
                  setDeliveryData({
                    ...deliveryData,
                    recipientEmail: e.target.value,
                  })
                }
                type="email"
                placeholder="email@example.com"
              />
            </Column>
          </Grid>

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="delivery-type"
                labelText={intl.formatMessage({
                  id: "pathology.reporting.deliveryType",
                  defaultMessage: "Delivery Type",
                })}
                value={deliveryData.deliveryType}
                onChange={(e) =>
                  setDeliveryData({
                    ...deliveryData,
                    deliveryType: e.target.value,
                  })
                }
              >
                <SelectItem value="internal" text="Internal (Department)" />
                <SelectItem value="clinician" text="Requesting Clinician" />
                <SelectItem value="external" text="External (Referral)" />
                <SelectItem value="patient" text="Patient Records" />
              </Select>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="regulatoryBody"
                labelText={intl.formatMessage({
                  id: "pathology.reporting.regulatoryBody",
                  defaultMessage: "Regulatory Body / Clinician",
                })}
                value={deliveryData.regulatoryBody}
                onChange={(e) =>
                  setDeliveryData({
                    ...deliveryData,
                    regulatoryBody: e.target.value,
                  })
                }
                placeholder="e.g., FDA, EMA, Requesting Clinician"
              />
            </Column>
          </Grid>

          <TextArea
            id="delivery-notes"
            labelText={intl.formatMessage({
              id: "pathology.reporting.deliveryNotes",
              defaultMessage: "Notes",
            })}
            value={deliveryData.notes}
            onChange={(e) =>
              setDeliveryData({ ...deliveryData, notes: e.target.value })
            }
            placeholder="Additional notes about the delivery..."
            rows={3}
          />
        </div>
      </Modal>

      {/* QC Meeting Modal */}
      <Modal
        open={qcMeetingModalOpen}
        modalHeading={intl.formatMessage({
          id: "pathology.reporting.qcMeetingModal.title",
          defaultMessage: "Document QC Meeting",
        })}
        primaryButtonText={intl.formatMessage({
          id: "common.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setQcMeetingModalOpen(false)}
        onRequestSubmit={handleSaveQcMeeting}
        primaryButtonDisabled={submitting}
        size="lg"
      >
        <Grid fullWidth>
          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) =>
                handleDateChange(dates, "meetingDate", setQcMeetingData)
              }
            >
              <DatePickerInput
                id="meetingDate"
                labelText={intl.formatMessage({
                  id: "pathology.reporting.meetingDate",
                  defaultMessage: "Meeting Date *",
                })}
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="attendees"
              name="attendees"
              labelText={intl.formatMessage({
                id: "pathology.reporting.attendees",
                defaultMessage: "Attendees",
              })}
              value={qcMeetingData.attendees}
              onChange={(e) => handleInputChange(e, setQcMeetingData)}
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="issuesDiscussed"
              name="issuesDiscussed"
              labelText={intl.formatMessage({
                id: "pathology.reporting.issuesDiscussed",
                defaultMessage: "Issues Discussed",
              })}
              value={qcMeetingData.issuesDiscussed}
              onChange={(e) => handleInputChange(e, setQcMeetingData)}
              rows={4}
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="actionsRequired"
              name="actionsRequired"
              labelText={intl.formatMessage({
                id: "pathology.reporting.actionsRequired",
                defaultMessage: "Actions Required",
              })}
              value={qcMeetingData.actionsRequired}
              onChange={(e) => handleInputChange(e, setQcMeetingData)}
              rows={4}
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default PathologyReportingPage;

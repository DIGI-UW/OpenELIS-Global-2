import React, { useState, useEffect, useRef, useCallback } from "react";
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
  Checkbox,
  Toggle,
} from "@carbon/react";
import {
  Report,
  Download,
  Renew,
  CheckmarkFilled,
  ChartColumn,
  Warning,
  Certificate,
  DocumentExport,
  Email,
  FlagFilled,
  Help,
  Checkmark,
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
 * PharmaceuticalReportingPage - Page 6: Reporting & Performance Monitoring
 *
 * Pharmaceutical-specific reporting including:
 * - QC/Testing Result Validation (Pass/Fail/OOS)
 * - Out-of-Specification (OOS) Investigation tracking
 * - Certificate of Analysis (COA) generation
 * - Stability study reporting
 * - Performance metrics (TAT, pass rates, deviations)
 * - Regulatory submission tracking
 * - Export and delivery functionality
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {number} props.notebookId - The notebook ID
 * @param {Object} props.pageData - Page configuration data
 * @param {Object} props.progress - Page progress info
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function PharmaceuticalReportingPage({
  entryId,
  notebookId,
  pageData,
  progress,
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
    valid: 0,
    invalid: 0,
    oos: 0,
    pending: 0,
    reviewed: 0,
    approved: 0,
  });

  // Validation/Flagging modal state
  const [showValidationModal, setShowValidationModal] = useState(false);
  const [validationData, setValidationData] = useState({
    validationStatus: "",
    reviewer: "",
    reviewDate: new Date().toISOString().split("T")[0],
    oosInvestigation: false,
    oosRootCause: "",
    correctiveAction: "",
    comments: "",
  });

  // COA generation modal state
  const [showCOAModal, setShowCOAModal] = useState(false);
  const [coaData, setCoaData] = useState({
    productName: "",
    batchNumber: "",
    manufacturingDate: "",
    expiryDate: "",
    specifications: "",
    authorizedBy: "",
    authorizedDate: new Date().toISOString().split("T")[0],
  });

  // Export state
  const [exporting, setExporting] = useState(false);

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

  // Performance metrics
  const [metrics, setMetrics] = useState({
    totalSamples: 0,
    qcPassRate: 0,
    qcFailRate: 0,
    oosRate: 0,
    assaySuccessRate: 0,
    averageTAT: 0,
    deviationCount: 0,
    pendingReview: 0,
    reviewed: 0,
    coaGenerated: 0,
  });

  // Validation status options (pharmaceutical-specific)
  const validationStatuses = [
    { value: "VALID", label: "Valid - Meets Specifications" },
    { value: "INVALID", label: "Invalid - Out of Specification (OOS)" },
    { value: "OOS_INVESTIGATION", label: "OOS - Under Investigation" },
    { value: "OOS_RESOLVED", label: "OOS - Resolved (Root Cause Identified)" },
    { value: "RETEST_REQUIRED", label: "Retest Required" },
    {
      value: "INCONCLUSIVE",
      label: "Inconclusive - Additional Testing Needed",
    },
    { value: "PENDING", label: "Pending Review" },
    { value: "APPROVED", label: "Approved for Release" },
    { value: "REJECTED", label: "Rejected - Do Not Release" },
  ];

  // Report type options
  const reportTypes = [
    { value: "intake_log", label: "Sample Intake Log" },
    { value: "qc_summary", label: "QC Summary Report" },
    { value: "assay_summary", label: "Assay/Test Summary Report" },
    { value: "oos_report", label: "OOS Investigation Report" },
    { value: "stability_report", label: "Stability Study Report" },
    { value: "coa", label: "Certificate of Analysis (COA)" },
    { value: "deviation_report", label: "Deviation Report" },
    { value: "tat_report", label: "Turnaround Time (TAT) Report" },
    { value: "regulatory_submission", label: "Regulatory Submission Package" },
  ];

  // Status filter options
  const statusFilterOptions = [
    { id: "ALL", label: "All Samples" },
    { id: "VALID", label: "Valid" },
    { id: "INVALID", label: "Invalid/OOS" },
    { id: "OOS_INVESTIGATION", label: "Under OOS Investigation" },
    { id: "PENDING", label: "Pending Review" },
    { id: "APPROVED", label: "Approved" },
    { id: "REJECTED", label: "Rejected" },
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
              // Pharmaceutical data
              sampleCategory: sample.data?.sampleCategory,
              chemicalName: sample.data?.chemicalName,
              lotNumber: sample.data?.lotNumber,
              grade: sample.data?.grade,
              // QC and testing data from previous pages
              qcResult: sample.data?.qcResult,
              testType: sample.data?.testType,
              assayResults: sample.data?.results || sample.data?.assayResults,
              hasDeviation: sample.data?.hasDeviation || false,
              // Validation/Reporting data
              validationStatus: sample.data?.validationStatus || "PENDING",
              reviewer: sample.data?.reviewer || "",
              reviewDate: sample.data?.reviewDate || "",
              oosInvestigation: sample.data?.oosInvestigation || false,
              oosRootCause: sample.data?.oosRootCause || "",
              correctiveAction: sample.data?.correctiveAction || "",
              coaGenerated: sample.data?.coaGenerated || false,
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

  // Calculate performance metrics from samples
  const calculateMetrics = (sampleData) => {
    const total = sampleData.length;
    if (total === 0) {
      setMetrics({
        totalSamples: 0,
        qcPassRate: 0,
        qcFailRate: 0,
        oosRate: 0,
        assaySuccessRate: 0,
        averageTAT: 0,
        deviationCount: 0,
        pendingReview: 0,
        reviewed: 0,
        coaGenerated: 0,
      });
      return;
    }

    const qcPassed = sampleData.filter((s) => s.qcResult === "Pass").length;
    const qcFailed = sampleData.filter((s) => s.qcResult === "Fail").length;
    const oosCount = sampleData.filter(
      (s) =>
        s.validationStatus === "INVALID" ||
        s.validationStatus === "OOS_INVESTIGATION",
    ).length;
    const withResults = sampleData.filter(
      (s) => s.assayResults && s.assayResults !== "",
    ).length;
    const deviations = sampleData.filter((s) => s.hasDeviation).length;
    const pending = sampleData.filter(
      (s) => !s.validationStatus || s.validationStatus === "PENDING",
    ).length;
    const reviewed = sampleData.filter(
      (s) =>
        s.validationStatus === "VALID" ||
        s.validationStatus === "APPROVED" ||
        s.validationStatus === "REJECTED",
    ).length;
    const coaCount = sampleData.filter((s) => s.coaGenerated).length;

    setMetrics({
      totalSamples: total,
      qcPassRate: total > 0 ? Math.round((qcPassed / total) * 100) : 0,
      qcFailRate: total > 0 ? Math.round((qcFailed / total) * 100) : 0,
      oosRate: total > 0 ? Math.round((oosCount / total) * 100) : 0,
      assaySuccessRate: total > 0 ? Math.round((withResults / total) * 100) : 0,
      averageTAT: 0, // Would need timestamps to calculate
      deviationCount: deviations,
      pendingReview: pending,
      reviewed: reviewed,
      coaGenerated: coaCount,
    });
  };

  // Calculate validation summary
  const calculateValidationSummary = (sampleData) => {
    const total = sampleData.length;
    const valid = sampleData.filter(
      (s) => s.validationStatus === "VALID",
    ).length;
    const invalid = sampleData.filter(
      (s) =>
        s.validationStatus === "INVALID" || s.validationStatus === "REJECTED",
    ).length;
    const oos = sampleData.filter(
      (s) =>
        s.validationStatus === "OOS_INVESTIGATION" ||
        s.validationStatus === "OOS_RESOLVED",
    ).length;
    const pending = sampleData.filter(
      (s) => !s.validationStatus || s.validationStatus === "PENDING",
    ).length;
    const reviewed = sampleData.filter(
      (s) =>
        s.validationStatus &&
        s.validationStatus !== "PENDING" &&
        s.validationStatus !== "OOS_INVESTIGATION",
    ).length;
    const approved = sampleData.filter(
      (s) => s.validationStatus === "APPROVED",
    ).length;

    setValidationSummary({
      total,
      valid,
      invalid,
      oos,
      pending,
      reviewed,
      approved,
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
    loadDeliveryHistory();

    return () => {
      componentMounted.current = false;
    };
  }, [pageData?.id, loadSamples, loadDeliveryHistory]);

  // Handle apply validation data
  const handleApplyValidation = () => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.reporting.noSamplesSelected",
          defaultMessage: "Please select samples to apply validation data",
        }),
      );
      return;
    }

    if (!validationData.validationStatus) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.reporting.validationRequired",
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
          oosInvestigation: validationData.oosInvestigation,
          oosRootCause: validationData.oosRootCause,
          correctiveAction: validationData.correctiveAction,
          comments: validationData.comments,
        },
      }),
      (status, response) => {
        if (componentMounted.current) {
          if (status === 200) {
            // Update page status based on validation
            const newStatus =
              validationData.validationStatus === "APPROVED"
                ? "COMPLETED"
                : validationData.validationStatus === "REJECTED"
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
                      id: "notebook.pharma.reporting.validationApplied",
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
      oosInvestigation: false,
      oosRootCause: "",
      correctiveAction: "",
      comments: "",
    });
  };

  // Handle COA generation - generates and downloads PDF
  const handleGenerateCOA = async () => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.reporting.noSamplesSelected",
          defaultMessage: "Please select samples to generate COA",
        }),
      );
      return;
    }

    if (!coaData.productName || !coaData.batchNumber || !coaData.authorizedBy) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.reporting.coaFieldsRequired",
          defaultMessage:
            "Product name, batch number, and authorizer are required for COA",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setShowCOAModal(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    try {
      // First, generate and download the COA PDF
      const pdfResponse = await fetch(
        `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData.id}/coa/generate`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          credentials: "include",
          body: JSON.stringify({
            sampleIds: numericIds,
            productName: coaData.productName,
            batchNumber: coaData.batchNumber,
            manufacturingDate: coaData.manufacturingDate,
            expiryDate: coaData.expiryDate,
            specifications: coaData.specifications,
            authorizedBy: coaData.authorizedBy,
            authorizedDate: coaData.authorizedDate,
          }),
        },
      );

      const contentType = pdfResponse.headers.get("content-type") || "";

      if (pdfResponse.ok && contentType.includes("application/pdf")) {
        // Download the PDF
        const blob = await pdfResponse.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = `COA_${coaData.batchNumber.replace(/[^a-zA-Z0-9]/g, "_")}.pdf`;
        document.body.appendChild(a);
        a.click();
        setTimeout(() => {
          window.URL.revokeObjectURL(url);
          document.body.removeChild(a);
        }, 100);

        // Now mark samples as COA generated in the database
        postToOpenElisServer(
          `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
          JSON.stringify({
            sampleIds: numericIds,
            data: {
              coaGenerated: true,
              coaProductName: coaData.productName,
              coaBatchNumber: coaData.batchNumber,
              coaManufacturingDate: coaData.manufacturingDate,
              coaExpiryDate: coaData.expiryDate,
              coaSpecifications: coaData.specifications,
              coaAuthorizedBy: coaData.authorizedBy,
              coaAuthorizedDate: coaData.authorizedDate,
            },
          }),
          () => {
            if (componentMounted.current) {
              setSuccess(
                intl.formatMessage(
                  {
                    id: "notebook.pharma.reporting.coaGenerated",
                    defaultMessage:
                      "Certificate of Analysis generated and downloaded for {count} samples",
                  },
                  { count: selectedIds.length },
                ),
              );
              setShowCOAModal(false);
              setSelectedIds([]);
              // Reset COA form
              setCoaData({
                productName: "",
                batchNumber: "",
                manufacturingDate: "",
                expiryDate: "",
                specifications: "",
                authorizedBy: "",
                authorizedDate: new Date().toISOString().split("T")[0],
              });
              loadSamples();
              if (onProgressUpdate) {
                onProgressUpdate();
              }
            }
          },
        );
      } else {
        // Handle error response
        let errorMessage = "Failed to generate COA";
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
      console.error("COA generation error:", err);
      setError("Network error during COA generation");
    }
  };

  // Handle export
  const handleExport = async (format) => {
    const nbId = notebookId || entryId;
    if (!nbId) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.reporting.error.noNotebook",
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
        a.download = `pharma_results_${nbId}.${format === "excel" ? "xlsx" : "csv"}`;
        document.body.appendChild(a);
        a.click();
        setTimeout(() => {
          window.URL.revokeObjectURL(url);
          document.body.removeChild(a);
        }, 100);

        setSuccess(
          intl.formatMessage({
            id: "notebook.pharma.reporting.exportSuccess",
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
          id: "notebook.pharma.reporting.error.recipientRequired",
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
            deliveryType: deliveryData.deliveryType,
            regulatoryBody: deliveryData.regulatoryBody || null,
            notes: deliveryData.notes || null,
          }),
        },
      );

      const data = await response.json();

      if (response.ok && data.success) {
        setSuccess(
          intl.formatMessage({
            id: "notebook.pharma.reporting.deliverySuccess",
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

  // Handle mark samples complete
  const handleMarkComplete = () => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.reporting.noSamplesSelected",
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
                  id: "notebook.pharma.reporting.markedComplete",
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
    if (statusFilter === "INVALID") {
      return (
        sample.validationStatus === "INVALID" ||
        sample.validationStatus === "REJECTED"
      );
    }
    return sample.validationStatus === statusFilter;
  });

  // Get validation status tag
  const getValidationTag = (sample) => {
    const statusConfig = {
      VALID: { type: "green", icon: Checkmark },
      APPROVED: { type: "green", icon: CheckmarkFilled },
      INVALID: { type: "red", icon: Warning },
      REJECTED: { type: "red", icon: Warning },
      OOS_INVESTIGATION: { type: "magenta", icon: Warning },
      OOS_RESOLVED: { type: "teal", icon: Checkmark },
      RETEST_REQUIRED: { type: "purple", icon: Help },
      INCONCLUSIVE: { type: "purple", icon: Help },
      PENDING: { type: "gray", icon: null },
    };

    const config =
      statusConfig[sample.validationStatus] || statusConfig.PENDING;
    const Icon = config.icon;

    return (
      <Tag type={config.type} size="sm" renderIcon={Icon}>
        {sample.validationStatus || "Pending"}
      </Tag>
    );
  };

  // Get result summary
  const renderResultSummary = (sample) => {
    const tags = [];
    if (sample.qcResult) {
      tags.push(
        <Tag
          key="qc"
          type={sample.qcResult === "Pass" ? "green" : "red"}
          size="sm"
        >
          QC: {sample.qcResult}
        </Tag>,
      );
    }
    if (sample.hasDeviation) {
      tags.push(
        <Tag key="dev" type="magenta" size="sm">
          Deviation
        </Tag>,
      );
    }
    if (sample.oosInvestigation) {
      tags.push(
        <Tag key="oos" type="red" size="sm">
          OOS
        </Tag>,
      );
    }
    if (sample.coaGenerated) {
      tags.push(
        <Tag key="coa" type="cyan" size="sm" renderIcon={Certificate}>
          COA
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
      ? Math.round((validationSummary.reviewed / validationSummary.total) * 100)
      : 0;

  if (loading) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <Loading withOverlay={false} description="Loading samples..." />
      </div>
    );
  }

  return (
    <div className="pharma-reporting-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.pharma.reporting.title"
            defaultMessage="Reporting &amp; Performance Monitoring"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.pharma.reporting.description"
            defaultMessage="Review and validate test results, track OOS investigations, generate Certificates of Analysis (COA), and monitor laboratory performance metrics."
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
                  id="notebook.pharma.reporting.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{validationSummary.total}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <Checkmark size={16} />
                <FormattedMessage
                  id="notebook.pharma.reporting.valid"
                  defaultMessage="Valid"
                />
              </span>
              <span className="progress-value">{validationSummary.valid}</span>
            </Tile>
            <Tile className="progress-tile" style={{ borderColor: "#da1e28" }}>
              <span className="progress-label" style={{ color: "#da1e28" }}>
                <Warning size={16} />
                <FormattedMessage
                  id="notebook.pharma.reporting.invalid"
                  defaultMessage="Invalid/OOS"
                />
              </span>
              <span className="progress-value" style={{ color: "#da1e28" }}>
                {validationSummary.invalid + validationSummary.oos}
              </span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.pharma.reporting.pendingReview"
                  defaultMessage="Pending Review"
                />
              </span>
              <span className="progress-value">
                {validationSummary.pending}
              </span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <CheckmarkFilled size={16} />
                <FormattedMessage
                  id="notebook.pharma.reporting.approved"
                  defaultMessage="Approved"
                />
              </span>
              <span className="progress-value">
                {validationSummary.approved}
              </span>
            </Tile>
            {metrics.deviationCount > 0 && (
              <Tile
                className="progress-tile"
                style={{ borderColor: "#a56eff" }}
              >
                <span className="progress-label" style={{ color: "#a56eff" }}>
                  <FormattedMessage
                    id="notebook.pharma.reporting.deviations"
                    defaultMessage="Deviations"
                  />
                </span>
                <span className="progress-value" style={{ color: "#a56eff" }}>
                  {metrics.deviationCount}
                </span>
              </Tile>
            )}
          </div>
          <ProgressBar
            label={intl.formatMessage(
              {
                id: "notebook.pharma.reporting.progress",
                defaultMessage: "Review Progress: {percent}%",
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
            <span className="metric-label">QC Pass Rate</span>
            <span className="metric-value">{metrics.qcPassRate}%</span>
            <ProgressBar
              value={metrics.qcPassRate}
              max={100}
              size="small"
              status={metrics.qcPassRate >= 90 ? "active" : "error"}
            />
          </Tile>
        </Column>
        <Column lg={4} md={2} sm={2}>
          <Tile className="metric-tile">
            <span className="metric-label">Assay Success</span>
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
            <span className="metric-label">OOS Rate</span>
            <span
              className="metric-value"
              style={{ color: metrics.oosRate > 5 ? "#da1e28" : "#198038" }}
            >
              {metrics.oosRate}%
            </span>
            <ProgressBar
              value={metrics.oosRate}
              max={100}
              size="small"
              status={metrics.oosRate <= 5 ? "active" : "error"}
            />
          </Tile>
        </Column>
        <Column lg={4} md={2} sm={2}>
          <Tile className="metric-tile">
            <span className="metric-label">COA Generated</span>
            <span className="metric-value">{metrics.coaGenerated}</span>
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
            id="notebook.pharma.reporting.applyValidation"
            defaultMessage="Apply Validation ({count})"
            values={{ count: selectedIds.length }}
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={Certificate}
          onClick={() => setShowCOAModal(true)}
          disabled={selectedIds.length === 0}
        >
          <FormattedMessage
            id="notebook.pharma.reporting.generateCOA"
            defaultMessage="Generate COA"
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={CheckmarkFilled}
          onClick={handleMarkComplete}
          disabled={selectedIds.length === 0}
        >
          <FormattedMessage
            id="notebook.pharma.reporting.markComplete"
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
            id="notebook.pharma.reporting.exportExcel"
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
            id="notebook.pharma.reporting.exportCsv"
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
            id="notebook.pharma.reporting.recordDelivery"
            defaultMessage="Record Delivery"
          />
        </Button>

        <Button kind="ghost" size="sm" renderIcon={Renew} onClick={loadSamples}>
          <FormattedMessage
            id="notebook.pharma.reporting.refresh"
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
              externalId: sample.externalId || "-",
              chemicalName: sample.chemicalName || "-",
              lotNumber: sample.lotNumber || "-",
              sampleType: sample.sampleType || "-",
              status: sample.status,
              validationStatus: sample.validationStatus,
              reviewer: sample.reviewer || "-",
            }))}
            headers={[
              { key: "externalId", header: "Sample ID" },
              { key: "chemicalName", header: "Chemical/Product" },
              { key: "lotNumber", header: "Lot/Batch" },
              { key: "sampleType", header: "Sample Type" },
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
                          id="pharma-reporting-select-all"
                          name="pharma-reporting-select-all"
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
                              id={`pharma-reporting-select-row-${row.id}`}
                              name={`pharma-reporting-select-row-${row.id}`}
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
              id="notebook.page.pharma.reporting.empty"
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
              id="notebook.pharma.reporting.deliveryHistory"
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
          id: "notebook.pharma.reporting.validationModal.title",
          defaultMessage: "Apply Validation & Review Data",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.pharma.reporting.apply",
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
              id="notebook.pharma.reporting.applyToSelected"
              defaultMessage="Applying validation data to {count} selected samples."
              values={{ count: selectedIds.length }}
            />
          </p>

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="validation-status"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.reporting.validationStatus",
                  defaultMessage: "Validation Status *",
                })}
                value={validationData.validationStatus}
                onChange={(e) =>
                  setValidationData({
                    ...validationData,
                    validationStatus: e.target.value,
                    oosInvestigation:
                      e.target.value === "OOS_INVESTIGATION" ||
                      e.target.value === "INVALID",
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
                  id: "notebook.pharma.reporting.reviewer",
                  defaultMessage: "Reviewer / QA Supervisor",
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
                    id: "notebook.pharma.reporting.reviewDate",
                    defaultMessage: "Review Date",
                  })}
                  placeholder="mm/dd/yyyy"
                />
              </DatePicker>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <Toggle
                id="oos-investigation"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.reporting.oosInvestigation",
                  defaultMessage: "OOS Investigation Required",
                })}
                toggled={validationData.oosInvestigation}
                onToggle={(checked) =>
                  setValidationData({
                    ...validationData,
                    oosInvestigation: checked,
                  })
                }
                labelA="No"
                labelB="Yes"
              />
            </Column>
          </Grid>

          {validationData.oosInvestigation && (
            <Grid fullWidth narrow>
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="oos-root-cause"
                  labelText={intl.formatMessage({
                    id: "notebook.pharma.reporting.oosRootCause",
                    defaultMessage: "OOS Root Cause",
                  })}
                  value={validationData.oosRootCause}
                  onChange={(e) =>
                    setValidationData({
                      ...validationData,
                      oosRootCause: e.target.value,
                    })
                  }
                  placeholder="Identify root cause..."
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="corrective-action"
                  labelText={intl.formatMessage({
                    id: "notebook.pharma.reporting.correctiveAction",
                    defaultMessage: "Corrective Action (CAPA)",
                  })}
                  value={validationData.correctiveAction}
                  onChange={(e) =>
                    setValidationData({
                      ...validationData,
                      correctiveAction: e.target.value,
                    })
                  }
                  placeholder="Describe corrective action..."
                />
              </Column>
            </Grid>
          )}

          <TextArea
            id="comments"
            labelText={intl.formatMessage({
              id: "notebook.pharma.reporting.comments",
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

      {/* COA Generation Modal */}
      <Modal
        open={showCOAModal}
        onRequestClose={() => setShowCOAModal(false)}
        onRequestSubmit={handleGenerateCOA}
        modalHeading={intl.formatMessage({
          id: "notebook.pharma.reporting.coaModal.title",
          defaultMessage: "Generate Certificate of Analysis (COA)",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.pharma.reporting.generateCOA",
          defaultMessage: "Generate COA",
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
              id="notebook.pharma.reporting.coaDescription"
              defaultMessage="Generate Certificate of Analysis for {count} selected samples."
              values={{ count: selectedIds.length }}
            />
          </p>

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="coa-product-name"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.reporting.productName",
                  defaultMessage: "Product Name *",
                })}
                value={coaData.productName}
                onChange={(e) =>
                  setCoaData({ ...coaData, productName: e.target.value })
                }
                required
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="coa-batch-number"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.reporting.batchNumber",
                  defaultMessage: "Batch/Lot Number *",
                })}
                value={coaData.batchNumber}
                onChange={(e) =>
                  setCoaData({ ...coaData, batchNumber: e.target.value })
                }
                required
              />
            </Column>
          </Grid>

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <DatePicker
                datePickerType="single"
                onChange={([date]) =>
                  setCoaData({
                    ...coaData,
                    manufacturingDate: date?.toISOString().split("T")[0] || "",
                  })
                }
              >
                <DatePickerInput
                  id="coa-mfg-date"
                  labelText={intl.formatMessage({
                    id: "notebook.pharma.reporting.manufacturingDate",
                    defaultMessage: "Manufacturing Date",
                  })}
                  placeholder="mm/dd/yyyy"
                />
              </DatePicker>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <DatePicker
                datePickerType="single"
                onChange={([date]) =>
                  setCoaData({
                    ...coaData,
                    expiryDate: date?.toISOString().split("T")[0] || "",
                  })
                }
              >
                <DatePickerInput
                  id="coa-expiry-date"
                  labelText={intl.formatMessage({
                    id: "notebook.pharma.reporting.expiryDate",
                    defaultMessage: "Expiry/Retest Date",
                  })}
                  placeholder="mm/dd/yyyy"
                />
              </DatePicker>
            </Column>
          </Grid>

          <TextArea
            id="coa-specifications"
            labelText={intl.formatMessage({
              id: "notebook.pharma.reporting.specifications",
              defaultMessage: "Specifications",
            })}
            value={coaData.specifications}
            onChange={(e) =>
              setCoaData({ ...coaData, specifications: e.target.value })
            }
            placeholder="Enter product specifications (appearance, assay, impurities, etc.)"
            rows={4}
          />

          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="coa-authorized-by"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.reporting.authorizedBy",
                  defaultMessage: "Authorized By *",
                })}
                value={coaData.authorizedBy}
                onChange={(e) =>
                  setCoaData({ ...coaData, authorizedBy: e.target.value })
                }
                required
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <DatePicker
                datePickerType="single"
                onChange={([date]) =>
                  setCoaData({
                    ...coaData,
                    authorizedDate: date?.toISOString().split("T")[0] || "",
                  })
                }
              >
                <DatePickerInput
                  id="coa-authorized-date"
                  labelText={intl.formatMessage({
                    id: "notebook.pharma.reporting.authorizedDate",
                    defaultMessage: "Authorization Date",
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
          id: "notebook.pharma.reporting.deliveryModal.title",
          defaultMessage: "Record Results Delivery",
        })}
        primaryButtonText={
          delivering
            ? intl.formatMessage({
                id: "label.recording",
                defaultMessage: "Recording...",
              })
            : intl.formatMessage({
                id: "notebook.pharma.reporting.recordDelivery",
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
                  id: "notebook.pharma.reporting.recipientName",
                  defaultMessage: "Recipient Name *",
                })}
                value={deliveryData.recipientName}
                onChange={(e) =>
                  setDeliveryData({
                    ...deliveryData,
                    recipientName: e.target.value,
                  })
                }
                placeholder="e.g., QA Department, Client Name"
                required
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="delivery-email"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.reporting.recipientEmail",
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
                  id: "notebook.pharma.reporting.deliveryType",
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
                <SelectItem value="internal" text="Internal (QA/Production)" />
                <SelectItem
                  value="external"
                  text="External (Client/Customer)"
                />
                <SelectItem value="regulatory" text="Regulatory Submission" />
              </Select>
            </Column>
            {deliveryData.deliveryType === "regulatory" && (
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="regulatory-body"
                  labelText={intl.formatMessage({
                    id: "notebook.pharma.reporting.regulatoryBody",
                    defaultMessage: "Regulatory Body",
                  })}
                  value={deliveryData.regulatoryBody}
                  onChange={(e) =>
                    setDeliveryData({
                      ...deliveryData,
                      regulatoryBody: e.target.value,
                    })
                  }
                  placeholder="e.g., FDA, EMA, NAFDAC"
                />
              </Column>
            )}
          </Grid>

          <TextArea
            id="delivery-notes"
            labelText={intl.formatMessage({
              id: "notebook.pharma.reporting.deliveryNotes",
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
    </div>
  );
}

export default PharmaceuticalReportingPage;

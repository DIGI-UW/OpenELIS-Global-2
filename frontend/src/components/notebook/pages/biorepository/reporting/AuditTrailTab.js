import React, { useState, useEffect, useRef } from "react";
import {
  Grid,
  Column,
  Loading,
  InlineNotification,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Pagination,
  Search,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  Button,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { Download, Reset } from "@carbon/icons-react";
import PropTypes from "prop-types";
import config from "../../../../../config.json";
import PermissionGate from "../../../../security/PermissionGate";
import { Permissions } from "../../../../../constants/roles";

/**
 * AuditTrailTab - Immutable chain of custody log viewer
 *
 * Displays searchable, filterable audit trail of all custody changes.
 * Features:
 * - Search by sample barcode/accession number
 * - Filter by custody action type
 * - Filter by date range
 * - Server-side pagination
 * - Expandable rows for full details
 */
function AuditTrailTab({ entryId, notebookId, pageData }) {
  const intl = useIntl();
  const [isLoading, setIsLoading] = useState(true);
  const [auditLogs, setAuditLogs] = useState([]);
  const [error, setError] = useState(null);
  const [actions, setActions] = useState([]);
  const [qcAuditData, setQCAuditData] = useState(null);
  const [qcAuditError, setQCAuditError] = useState(null);
  const [expandedQcBatches, setExpandedQcBatches] = useState({});

  // Filter states
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedAction, setSelectedAction] = useState("ALL");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  // Pagination states
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(50);
  const [totalCount, setTotalCount] = useState(0);

  // Helper function to format Date to YYYY-MM-DD
  const formatDateToISO = (date) => {
    if (!date) return "";
    const d = new Date(date);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  };

  // Fetch custody action types
  useEffect(() => {
    fetch(`${config.serverBaseUrl}/rest/biorepository/custody/actions`, {
      credentials: "include",
    })
      .then((r) => r.json())
      .then((data) => setActions(data))
      .catch((err) => console.error("Failed to load action types:", err));
  }, []);

  // Fetch QC-focused reporting/audit snapshots from dashboard endpoints.
  useEffect(() => {
    Promise.all([
      fetch(`${config.serverBaseUrl}/rest/biorepository/dashboard/qc-metrics`, {
        credentials: "include",
      }).then((r) => r.json()),
      fetch(
        `${config.serverBaseUrl}/rest/biorepository/dashboard/qc-history?limit=100`,
        {
          credentials: "include",
        },
      ).then((r) => r.json()),
      fetch(
        `${config.serverBaseUrl}/rest/biorepository/dashboard/qc-discrepancies`,
        {
          credentials: "include",
        },
      ).then((r) => r.json()),
    ])
      .then(([qcMetrics, qcHistory, qcDiscrepancies]) => {
        setQCAuditData({
          qcMetrics: qcMetrics || {},
          qcHistory: qcHistory || { items: [] },
          qcDiscrepancies: qcDiscrepancies || {},
        });
        setQCAuditError(null);
      })
      .catch((err) => {
        setQCAuditError(err.message || "Failed to load QC audit snapshot");
      });
  }, []);

  // Fetch audit logs
  const fetchAuditLogs = (page, filters = {}) => {
    setIsLoading(true);
    setError(null);

    const params = new URLSearchParams();
    params.append("page", page - 1); // Convert to 0-indexed
    params.append("pageSize", pageSize);

    if (filters.searchQuery)
      params.append("sampleExternalId", filters.searchQuery);
    if (filters.action && filters.action !== "ALL")
      params.append("action", filters.action);
    // Always format dates to ISO string
    if (filters.startDate) {
      const formattedStart =
        typeof filters.startDate === "string"
          ? filters.startDate
          : formatDateToISO(filters.startDate);
      if (formattedStart) params.append("startDate", formattedStart);
    }
    if (filters.endDate) {
      const formattedEnd =
        typeof filters.endDate === "string"
          ? filters.endDate
          : formatDateToISO(filters.endDate);
      if (formattedEnd) params.append("endDate", formattedEnd);
    }

    fetch(
      `${config.serverBaseUrl}/rest/biorepository/lifecycle/search?${params}`,
      {
        credentials: "include",
      },
    )
      .then((r) => {
        if (!r.ok) throw new Error("Failed to load audit trail");
        return r.json();
      })
      .then((response) => {
        setAuditLogs(response.data || []);
        setTotalCount(response.totalCount || 0);
        setIsLoading(false);
      })
      .catch((err) => {
        setError(err.message);
        setIsLoading(false);
      });
  };

  // Store current filters in a ref to access in useEffect
  const currentFiltersRef = useRef({
    searchQuery: "",
    selectedAction: "ALL",
    startDate: "",
    endDate: "",
  });

  useEffect(() => {
    fetchAuditLogs(currentPage, currentFiltersRef.current);
  }, [currentPage, pageSize]);

  const handleApplyFilters = () => {
    // Update the filters ref
    currentFiltersRef.current = {
      searchQuery,
      action: selectedAction,
      startDate,
      endDate,
    };
    // Reset to page 1 and fetch
    setCurrentPage(1);
    fetchAuditLogs(1, currentFiltersRef.current);
  };

  const handleResetFilters = () => {
    setSearchQuery("");
    setSelectedAction("ALL");
    setStartDate("");
    setEndDate("");
    // Update the filters ref
    currentFiltersRef.current = {
      searchQuery: "",
      action: "ALL",
      startDate: "",
      endDate: "",
    };
    // Reset to page 1 and fetch
    setCurrentPage(1);
    fetchAuditLogs(1, currentFiltersRef.current);
  };

  if (isLoading && auditLogs.length === 0) {
    return (
      <div style={{ padding: "2rem" }}>
        <Loading
          description={intl.formatMessage({
            id: "biorepository.reporting.audit.loading",
            defaultMessage: "Loading audit trail...",
          })}
        />
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ padding: "2rem" }}>
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "biorepository.reporting.audit.error",
            defaultMessage: "Error Loading Audit Trail",
          })}
          subtitle={error}
          lowContrast
        />
      </div>
    );
  }

  // Map action to color
  const getActionTag = (action) => {
    const tagMap = {
      CHECKOUT_REQUESTED: { type: "blue", label: "Checkout Requested" },
      CHECKOUT_APPROVED: { type: "green", label: "Checkout Approved" },
      CHECKOUT_RETRIEVED: { type: "cyan", label: "Checkout Retrieved" },
      CHECKOUT_RELEASED: { type: "teal", label: "Checkout Released" },
      TRANSFER_INITIATED: { type: "magenta", label: "Transfer Initiated" },
      TRANSFER_RECEIVED: { type: "cyan", label: "Transfer Received" },
      STORAGE_ASSIGNED: { type: "green", label: "Storage Assigned" },
      STORAGE_MOVED: { type: "teal", label: "Storage Moved" },
      RETURN_RECEIVED: { type: "green", label: "Return Received" },
      RETURN_INSPECTED: { type: "teal", label: "Return Inspected" },
      RETURN_STORED: { type: "purple", label: "Return Stored" },
      DISPOSED: { type: "red", label: "Disposed" },
    };

    const tag = tagMap[action] || { type: "gray", label: action };
    return <Tag type={tag.type}>{tag.label}</Tag>;
  };

  // DataTable rows
  const rows = auditLogs.map((log) => ({
    id: `${log.sampleItemId || "sample"}-${log.sourceRecordType || "event"}-${
      log.sourceRecordId ||
      log.actionTimestamp ||
      log.eventTimestamp ||
      "unknown"
    }`,
    timestamp: new Date(
      log.actionTimestamp || log.eventTimestamp,
    ).toLocaleString(),
    sampleId: log.sampleExternalId || log.accessionNumber || "N/A",
    action: log.custodyAction,
    custodian: log.actorDisplayName || log.actorUserId || "System",
    fromLocation: log.fromLocationDisplay || log.fromLocation || "-",
    toLocation: log.toLocationDisplay || log.toLocation || "-",
    temperature: log.temperature ? `${log.temperature}°C` : "-",
    notes: log.notes || "-",
  }));

  const headers = [
    { key: "timestamp", header: "Timestamp" },
    { key: "sampleId", header: "Sample Barcode" },
    { key: "action", header: "Custody Action" },
    { key: "custodian", header: "Custodian" },
    { key: "fromLocation", header: "From Location" },
    { key: "toLocation", header: "To Location" },
  ];

  const formatPercent = (value) => {
    const numeric = Number(value);
    if (Number.isNaN(numeric)) {
      return "N/A";
    }
    return `${numeric.toFixed(1)}%`;
  };

  const downloadBlob = (blob, filename) => {
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = filename;
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    window.URL.revokeObjectURL(url);
  };

  const exportQcBatch = (qcBatchId, format = "pdf") => {
    if (!qcBatchId || qcBatchId === "UNBATCHED") {
      return;
    }
    fetch(
      `${config.serverBaseUrl}/rest/biorepository/qc/export/${format}?qcBatchId=${encodeURIComponent(qcBatchId)}`,
      {
        credentials: "include",
      },
    )
      .then((response) => {
        if (!response.ok) {
          throw new Error(`Failed to export QC batch ${qcBatchId}`);
        }
        return response.blob();
      })
      .then((blob) => {
        const stamp = new Date().toISOString().slice(0, 10);
        downloadBlob(blob, `qc_batch_${qcBatchId}_${stamp}.${format}`);
      })
      .catch((err) => setQCAuditError(err.message));
  };

  const qcMetrics = qcAuditData?.qcMetrics || {};
  const escalationSignals = qcMetrics.escalationSignals || {};
  const qcHistoryItems = Array.isArray(qcAuditData?.qcHistory?.items)
    ? qcAuditData.qcHistory.items
    : [];
  const qcDiscrepancyEntries = Object.entries(
    qcAuditData?.qcDiscrepancies || {},
  );

  const deriveQcStatus = (item) => {
    if (item.qcStatus) return item.qcStatus;
    if (item.qcResult === "VERIFIED") return "VALID";
    if (item.qcResult === "DISCREPANCY_FOUND") {
      return item.discrepancyType === "SAMPLE_MISSING"
        ? "MISSING"
        : "QC_FAILED";
    }
    return "UNKNOWN";
  };

  const deriveLifecycleOutcome = (item) => {
    if (item.lifecycleOutcome) return item.lifecycleOutcome;
    const status = deriveQcStatus(item);
    if (item.qcResult === "VERIFIED" || status === "VALID") {
      return "PASSED";
    }
    if (status === "MISSING") {
      return "FAILED_MARKED_MISSING";
    }
    if (item.qcResult === "DISCREPANCY_FOUND" && item.correctiveAction) {
      return "FAILED_CORRECTED";
    }
    if (item.qcResult === "DISCREPANCY_FOUND") {
      return "FAILED_PENDING_CORRECTION";
    }
    return "UNKNOWN";
  };

  const formatLifecycleOutcome = (value) => {
    const labels = {
      PASSED: "Passed",
      FAILED_PENDING_CORRECTION: "Failed (pending correction)",
      FAILED_CORRECTED: "Failed (correction logged)",
      FAILED_MARKED_MISSING: "Failed (marked missing)",
      UNKNOWN: "Unknown",
    };
    return labels[value] || value || "Unknown";
  };

  const extractAuditField = (item, primaryKey, fallbackKey) => {
    if (item?.auditTrail?.[primaryKey]) return item.auditTrail[primaryKey];
    if (item?.auditTrail?.[fallbackKey]) return item.auditTrail[fallbackKey];
    return "-";
  };

  const lifecycleOutcomes = qcHistoryItems.map(deriveLifecycleOutcome);
  const correctedOutcomeCount = lifecycleOutcomes.filter(
    (outcome) =>
      outcome === "FAILED_CORRECTED" || outcome === "FAILED_MARKED_MISSING",
  ).length;
  const pendingCorrectionCount = lifecycleOutcomes.filter(
    (outcome) => outcome === "FAILED_PENDING_CORRECTION",
  ).length;

  const qcHistoryRows = qcHistoryItems.map((item, idx) => ({
    id: `qc-audit-${idx}`,
    qcBatchId: item.qcBatchId || "UNBATCHED",
    inspectionDateRaw: item.inspectionDate || null,
    inspectionDate: item.inspectionDate
      ? new Date(item.inspectionDate).toLocaleString()
      : "-",
    result: item.qcResult || "-",
    lifecycleOutcome: formatLifecycleOutcome(deriveLifecycleOutcome(item)),
    status: deriveQcStatus(item),
    technician: item.inspectorName || item.technicianId || "-",
    sampleFlag: item.sampleFlag || "-",
    qcFailed: item.qcFailed ? "Yes" : "No",
    freezer: item.freezer || "Unknown",
    rack: item.rack || "Unknown",
    discrepancyType: item.discrepancyType || "-",
    failureComment: item.failureComment || item.remarks || "-",
    correctiveAction: item.correctiveAction || "-",
    correctionFrom: extractAuditField(item, "fromCoordinates", "oldCoordinate"),
    correctionTo: extractAuditField(item, "toCoordinates", "newCoordinate"),
    correctedBy:
      item.auditTrail?.correctedBy ||
      item.auditTrail?.user ||
      item.inspectorName ||
      item.technicianId ||
      "-",
    correctedAt:
      item.auditTrail?.correctedAt || item.auditTrail?.timestamp
        ? new Date(
            item.auditTrail.correctedAt || item.auditTrail.timestamp,
          ).toLocaleString()
        : "-",
    correctionReason: item.auditTrail?.reason || "-",
  }));

  const qcHistoryBatchRows = (() => {
    const grouped = new Map();
    qcHistoryRows.forEach((row) => {
      const key = row.qcBatchId || "UNBATCHED";
      if (!grouped.has(key)) {
        grouped.set(key, {
          id: `qc-batch-${key}`,
          qcBatchId: key,
          latestInspectionDate: row.inspectionDate || "-",
          latestInspectionDateRaw: row.inspectionDateRaw,
          sampleCount: 0,
          passCount: 0,
          failCount: 0,
          pendingCorrectionCount: 0,
          details: [],
        });
      }
      const batch = grouped.get(key);
      batch.details.push(row);
      batch.sampleCount += 1;
      if (row.result === "VERIFIED" || row.status === "VALID") {
        batch.passCount += 1;
      } else {
        batch.failCount += 1;
      }
      if (row.lifecycleOutcome === "Failed (pending correction)") {
        batch.pendingCorrectionCount += 1;
      }
      if (
        row.inspectionDateRaw &&
        (!batch.latestInspectionDateRaw ||
          new Date(row.inspectionDateRaw) >
            new Date(batch.latestInspectionDateRaw))
      ) {
        batch.latestInspectionDate = row.inspectionDate;
        batch.latestInspectionDateRaw = row.inspectionDateRaw;
      }
    });
    return Array.from(grouped.values()).sort(
      (a, b) =>
        new Date(b.latestInspectionDateRaw || 0).getTime() -
        new Date(a.latestInspectionDateRaw || 0).getTime(),
    );
  })();

  const qcCorrectiveActionsLogged = qcHistoryRows.filter(
    (row) => row.correctiveAction && row.correctiveAction !== "-",
  ).length;

  const qcSummaryRows = [
    {
      id: "qc-summary-pass-rate",
      metric: "Pass Rate",
      value: formatPercent(qcMetrics.passRate ?? qcMetrics.complianceRate),
    },
    {
      id: "qc-summary-fail-count",
      metric: "Fail Count",
      value: qcMetrics.failCount ?? qcMetrics.failedInspections ?? 0,
    },
    {
      id: "qc-summary-fail-trend",
      metric: "Fail Trend Basis",
      value: qcMetrics.failTrendBasis
        ? `${qcMetrics.failTrendBasis.granularity || "day"}, ${qcMetrics.failTrendBasis.windowDays || 30} days, source: ${qcMetrics.failTrendBasis.source || "N/A"}`
        : "N/A",
    },
    {
      id: "qc-summary-corrective-actions",
      metric: "Corrective Actions Logged",
      value: qcCorrectiveActionsLogged,
    },
    {
      id: "qc-summary-corrected-outcomes",
      metric: "Failed Outcomes Corrected",
      value: correctedOutcomeCount,
    },
    {
      id: "qc-summary-pending-corrections",
      metric: "Pending Corrections",
      value: pendingCorrectionCount,
    },
    {
      id: "qc-summary-missing",
      metric: "Missing Samples (QC)",
      value: escalationSignals.criticalMissingSamples || 0,
    },
    {
      id: "qc-summary-triggers",
      metric: "Escalation Rules Triggered",
      value: Array.isArray(escalationSignals.triggeredRules)
        ? escalationSignals.triggeredRules.join(", ") || "None"
        : "None",
    },
  ];

  const qcSummaryHeaders = [
    { key: "metric", header: "QC Audit Metric" },
    { key: "value", header: "Value" },
  ];

  const qcHistoryHeaders = [
    { key: "inspectionDate", header: "Inspection Date" },
    { key: "qcBatchId", header: "QC Batch ID" },
    { key: "result", header: "Result" },
    { key: "lifecycleOutcome", header: "Lifecycle Outcome" },
    { key: "status", header: "QC Status" },
    { key: "technician", header: "Technician" },
    { key: "sampleFlag", header: "Sample Flag" },
    { key: "qcFailed", header: "QC Failed" },
    { key: "freezer", header: "Freezer" },
    { key: "rack", header: "Rack" },
    { key: "discrepancyType", header: "Discrepancy Type" },
    { key: "failureComment", header: "Failure Comment" },
    { key: "correctiveAction", header: "Corrective Action" },
    { key: "correctionFrom", header: "Correction From" },
    { key: "correctionTo", header: "Correction To" },
    { key: "correctedBy", header: "Corrected By" },
    { key: "correctedAt", header: "Corrected At" },
    { key: "correctionReason", header: "Correction Reason" },
  ];

  const qcBatchHeaders = [
    { key: "qcBatchId", header: "QC Batch ID" },
    { key: "latestInspectionDate", header: "Latest Inspection" },
    { key: "sampleCount", header: "Samples" },
    { key: "passCount", header: "Pass" },
    { key: "failCount", header: "Fail" },
    { key: "pendingCorrectionCount", header: "Pending Correction" },
    { key: "actions", header: "Actions" },
  ];

  const qcDiscrepancyRows = qcDiscrepancyEntries.map(([type, count], idx) => ({
    id: `qc-discrepancy-${idx}`,
    type,
    count,
  }));

  const qcDiscrepancyHeaders = [
    { key: "type", header: "Discrepancy Type" },
    { key: "count", header: "Count" },
  ];

  return (
    <div className="audit-trail-tab" style={{ padding: "1.5rem" }}>
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <h4 style={{ marginBottom: "1rem" }}>
            <FormattedMessage
              id="biorepository.reporting.audit.title"
              defaultMessage="Chain of Custody Audit Trail"
            />
          </h4>
          <p
            style={{
              fontSize: "0.875rem",
              color: "#525252",
              marginBottom: "1.5rem",
            }}
          >
            <FormattedMessage
              id="biorepository.reporting.audit.description"
              defaultMessage="Complete immutable audit trail of all sample custody changes. Search by barcode, filter by action type or date range."
            />
          </p>
        </Column>

        <Column lg={16} md={8} sm={4} style={{ marginBottom: "1.5rem" }}>
          <h5 style={{ marginBottom: "0.75rem" }}>
            QC Outcome Audit Visibility (Batch Grouped)
          </h5>
          <p
            style={{
              fontSize: "0.875rem",
              color: "#525252",
              marginBottom: "0.75rem",
            }}
          >
            Summary metrics above provide quick indicators; the history table
            below provides record-level lifecycle details (failed, corrected, or
            marked missing) from existing dashboard data sources.
          </p>

          {qcAuditError ? (
            <InlineNotification
              kind="warning"
              title="QC audit snapshot unavailable"
              subtitle={qcAuditError}
              lowContrast
            />
          ) : (
            <div>
              <DataTable rows={qcSummaryRows} headers={qcSummaryHeaders}>
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <TableContainer
                    title="QC Reporting Snapshot"
                    description={`History source: ${qcAuditData?.qcHistory?.source || "N/A"} | Records: ${qcAuditData?.qcHistory?.count || 0}`}
                  >
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
                              <TableCell key={cell.id}>{cell.value}</TableCell>
                            ))}
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              </DataTable>

              <div style={{ marginTop: "1rem" }}>
                <DataTable rows={qcHistoryBatchRows} headers={qcBatchHeaders}>
                  {({
                    rows,
                    headers,
                    getTableProps,
                    getHeaderProps,
                    getRowProps,
                  }) => (
                    <TableContainer
                      title="QC History by Batch ID"
                      description="NEW: one row per QC batch ID. Use Show details to expand records, then Print PDF/CSV for reporting."
                    >
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
                          {rows.map((row) => {
                            const batchId =
                              row.cells.find(
                                (cell) => cell.info.header === "qcBatchId",
                              )?.value || "UNBATCHED";
                            const detailRows =
                              qcHistoryBatchRows.find(
                                (batch) => batch.qcBatchId === batchId,
                              )?.details || [];
                            const isExpanded = Boolean(
                              expandedQcBatches[batchId],
                            );

                            return (
                              <React.Fragment key={row.id}>
                                <TableRow {...getRowProps({ row })}>
                                  {row.cells.map((cell) => {
                                    if (cell.info.header === "actions") {
                                      return (
                                        <TableCell key={cell.id}>
                                          <div
                                            style={{
                                              display: "flex",
                                              gap: "0.5rem",
                                              flexWrap: "wrap",
                                            }}
                                          >
                                            <Button
                                              kind="ghost"
                                              size="sm"
                                              onClick={() =>
                                                setExpandedQcBatches(
                                                  (prev) => ({
                                                    ...prev,
                                                    [batchId]: !prev[batchId],
                                                  }),
                                                )
                                              }
                                            >
                                              {isExpanded
                                                ? "Hide details"
                                                : "Show details"}
                                            </Button>
                                            {batchId !== "UNBATCHED" && (
                                              <>
                                                <Button
                                                  kind="ghost"
                                                  size="sm"
                                                  renderIcon={Download}
                                                  onClick={() =>
                                                    exportQcBatch(
                                                      batchId,
                                                      "pdf",
                                                    )
                                                  }
                                                >
                                                  Print PDF
                                                </Button>
                                                <Button
                                                  kind="ghost"
                                                  size="sm"
                                                  onClick={() =>
                                                    exportQcBatch(
                                                      batchId,
                                                      "csv",
                                                    )
                                                  }
                                                >
                                                  CSV
                                                </Button>
                                              </>
                                            )}
                                          </div>
                                        </TableCell>
                                      );
                                    }
                                    return (
                                      <TableCell key={cell.id}>
                                        {cell.value}
                                      </TableCell>
                                    );
                                  })}
                                </TableRow>
                                {isExpanded && (
                                  <TableRow>
                                    <TableCell
                                      colSpan={qcBatchHeaders.length}
                                      style={{ backgroundColor: "#f4f4f4" }}
                                    >
                                      <DataTable
                                        rows={detailRows}
                                        headers={qcHistoryHeaders}
                                      >
                                        {({
                                          rows: nestedRows,
                                          headers: nestedHeaders,
                                          getTableProps: getNestedTableProps,
                                          getHeaderProps: getNestedHeaderProps,
                                          getRowProps: getNestedRowProps,
                                        }) => (
                                          <TableContainer title="">
                                            <Table {...getNestedTableProps()}>
                                              <TableHead>
                                                <TableRow>
                                                  {nestedHeaders.map(
                                                    (header) => (
                                                      <TableHeader
                                                        key={header.key}
                                                        {...getNestedHeaderProps(
                                                          {
                                                            header,
                                                          },
                                                        )}
                                                      >
                                                        {header.header}
                                                      </TableHeader>
                                                    ),
                                                  )}
                                                </TableRow>
                                              </TableHead>
                                              <TableBody>
                                                {nestedRows.map((nestedRow) => (
                                                  <TableRow
                                                    key={nestedRow.id}
                                                    {...getNestedRowProps({
                                                      row: nestedRow,
                                                    })}
                                                  >
                                                    {nestedRow.cells.map(
                                                      (cell) => (
                                                        <TableCell
                                                          key={cell.id}
                                                        >
                                                          {cell.value}
                                                        </TableCell>
                                                      ),
                                                    )}
                                                  </TableRow>
                                                ))}
                                              </TableBody>
                                            </Table>
                                          </TableContainer>
                                        )}
                                      </DataTable>
                                    </TableCell>
                                  </TableRow>
                                )}
                              </React.Fragment>
                            );
                          })}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  )}
                </DataTable>
              </div>

              <div style={{ marginTop: "1rem" }}>
                <DataTable
                  rows={qcDiscrepancyRows}
                  headers={qcDiscrepancyHeaders}
                >
                  {({
                    rows,
                    headers,
                    getTableProps,
                    getHeaderProps,
                    getRowProps,
                  }) => (
                    <TableContainer
                      title="QC Discrepancy Type Breakdown"
                      description="Supports quick review of most common discrepancy classes"
                    >
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
                                  {cell.value}
                                </TableCell>
                              ))}
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  )}
                </DataTable>
              </div>
            </div>
          )}
        </Column>

        {/* Filters */}
        <Column lg={16} md={8} sm={4} style={{ marginBottom: "1rem" }}>
          <div
            style={{
              display: "flex",
              gap: "1rem",
              alignItems: "flex-end",
              flexWrap: "wrap",
            }}
          >
            <Search
              id="audit-search"
              labelText={intl.formatMessage({
                id: "biorepository.reporting.audit.search",
                defaultMessage: "Search by sample barcode or accession number",
              })}
              placeholder={intl.formatMessage({
                id: "biorepository.reporting.audit.searchPlaceholder",
                defaultMessage: "Enter barcode...",
              })}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              size="md"
              style={{ minWidth: "250px" }}
            />

            <Select
              id="action-filter"
              labelText={intl.formatMessage({
                id: "biorepository.reporting.audit.actionFilter",
                defaultMessage: "Custody Action",
              })}
              value={selectedAction}
              onChange={(e) => setSelectedAction(e.target.value)}
              size="md"
              style={{ minWidth: "200px" }}
            >
              <SelectItem value="ALL" text="All Actions" />
              {actions.map((action) => (
                <SelectItem
                  key={action.value}
                  value={action.value}
                  text={action.label}
                />
              ))}
            </Select>

            <DatePicker
              datePickerType="single"
              value={startDate}
              onChange={(dates) => setStartDate(dates[0] || "")}
            >
              <DatePickerInput
                id="start-date"
                placeholder="mm/dd/yyyy"
                labelText={intl.formatMessage({
                  id: "biorepository.reporting.audit.startDate",
                  defaultMessage: "Start Date",
                })}
                size="md"
              />
            </DatePicker>

            <DatePicker
              datePickerType="single"
              value={endDate}
              onChange={(dates) => setEndDate(dates[0] || "")}
            >
              <DatePickerInput
                id="end-date"
                placeholder="mm/dd/yyyy"
                labelText={intl.formatMessage({
                  id: "biorepository.reporting.audit.endDate",
                  defaultMessage: "End Date",
                })}
                size="md"
              />
            </DatePicker>

            <Button kind="primary" onClick={handleApplyFilters} size="md">
              <FormattedMessage
                id="biorepository.reporting.audit.apply"
                defaultMessage="Apply Filters"
              />
            </Button>

            <Button
              kind="secondary"
              onClick={handleResetFilters}
              renderIcon={Reset}
              size="md"
            >
              <FormattedMessage
                id="biorepository.reporting.audit.reset"
                defaultMessage="Reset"
              />
            </Button>
          </div>
        </Column>

        {/* Results Summary */}
        <Column lg={16} md={8} sm={4} style={{ marginBottom: "1rem" }}>
          <p style={{ fontSize: "0.875rem", color: "#525252" }}>
            <FormattedMessage
              id="biorepository.reporting.audit.resultsCount"
              defaultMessage="Showing {start}-{end} of {total} custody records"
              values={{
                start:
                  auditLogs.length > 0 ? (currentPage - 1) * pageSize + 1 : 0,
                end: Math.min(currentPage * pageSize, totalCount),
                total: totalCount,
              }}
            />
          </p>
        </Column>

        {/* Audit Trail Table */}
        <Column lg={16} md={8} sm={4}>
          <DataTable rows={rows} headers={headers}>
            {({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
            }) => (
              <TableContainer title="">
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
                            {cell.info.header === "action"
                              ? getActionTag(cell.value)
                              : cell.value}
                          </TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>

          {/* Pagination */}
          <Pagination
            backwardText={intl.formatMessage({
              id: "biorepository.reporting.audit.previousPage",
              defaultMessage: "Previous page",
            })}
            forwardText={intl.formatMessage({
              id: "biorepository.reporting.audit.nextPage",
              defaultMessage: "Next page",
            })}
            itemsPerPageText={intl.formatMessage({
              id: "biorepository.reporting.audit.itemsPerPage",
              defaultMessage: "Items per page:",
            })}
            page={currentPage}
            pageSize={pageSize}
            pageSizes={[25, 50, 100]}
            totalItems={totalCount}
            onChange={({ page, pageSize: newPageSize }) => {
              if (newPageSize !== pageSize) {
                setPageSize(newPageSize);
                setCurrentPage(1);
              } else {
                setCurrentPage(page);
              }
            }}
          />
        </Column>
      </Grid>
    </div>
  );
}

AuditTrailTab.propTypes = {
  entryId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  notebookId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  pageData: PropTypes.object,
};

export default AuditTrailTab;

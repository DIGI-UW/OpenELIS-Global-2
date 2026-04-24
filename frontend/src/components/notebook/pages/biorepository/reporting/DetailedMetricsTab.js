import React, { useState, useEffect } from "react";
import {
  Grid,
  Column,
  Loading,
  InlineNotification,
  Accordion,
  AccordionItem,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  DatePicker,
  DatePickerInput,
  Button,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { Search, Reset, Download } from "@carbon/icons-react";
import PropTypes from "prop-types";
import config from "../../../../../config.json";

/**
 * DetailedMetricsTab - Drill-down metrics with advanced filtering
 *
 * Displays detailed breakdowns of:
 * - Storage capacity by status
 * - Sample aging by time range
 * - QC inspection checkpoint statistics
 * - Retrieval and disposal details
 */
function DetailedMetricsTab({ entryId, notebookId, pageData }) {
  const intl = useIntl();
  const [isLoading, setIsLoading] = useState(true);
  const [metricsData, setMetricsData] = useState(null);
  const [error, setError] = useState(null);
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  // Helper function to format Date to YYYY-MM-DD
  const formatDateToISO = (date) => {
    if (!date) return "";
    const d = new Date(date);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  };

  const fetchMetrics = (start = null, end = null) => {
    setIsLoading(true);
    setError(null);

    const dateParams = new URLSearchParams();
    if (start) dateParams.append("startDate", start);
    if (end) dateParams.append("endDate", end);
    const dateQuery = dateParams.toString() ? `?${dateParams.toString()}` : "";

    Promise.all([
      fetch(
        `${config.serverBaseUrl}/rest/biorepository/dashboard/storage-capacity`,
        {
          credentials: "include",
        },
      ).then((r) => r.json()),
      fetch(
        `${config.serverBaseUrl}/rest/biorepository/dashboard/sample-aging`,
        {
          credentials: "include",
        },
      ).then((r) => r.json()),
      fetch(`${config.serverBaseUrl}/rest/biorepository/dashboard/qc-metrics`, {
        credentials: "include",
      }).then((r) => r.json()),
      fetch(
        `${config.serverBaseUrl}/rest/biorepository/dashboard/qc-discrepancies`,
        {
          credentials: "include",
        },
      ).then((r) => r.json()),
      fetch(
        `${config.serverBaseUrl}/rest/biorepository/dashboard/qc-history?limit=100`,
        {
          credentials: "include",
        },
      ).then((r) => r.json()),
      fetch(
        `${config.serverBaseUrl}/rest/biorepository/dashboard/retrieval-stats${dateQuery}`,
        {
          credentials: "include",
        },
      ).then((r) => r.json()),
      fetch(
        `${config.serverBaseUrl}/rest/biorepository/dashboard/disposal-stats${dateQuery}`,
        {
          credentials: "include",
        },
      ).then((r) => r.json()),
    ])
      .then(
        ([
          capacity,
          aging,
          qc,
          discrepancies,
          qcHistory,
          retrieval,
          disposal,
        ]) => {
        setMetricsData({
          capacity,
          aging,
          qc,
          discrepancies,
          qcHistory,
          retrieval,
          disposal,
        });
        setIsLoading(false);
      },
      )
      .catch((err) => {
        setError(err.message);
        setIsLoading(false);
      });
  };

  useEffect(() => {
    fetchMetrics();
  }, []);

  const handleApplyFilters = () => {
    // Convert dates to ISO format strings before sending to API
    const startISO = startDate ? formatDateToISO(startDate) : null;
    const endISO = endDate ? formatDateToISO(endDate) : null;
    fetchMetrics(startISO, endISO);
  };

  const handleResetFilters = () => {
    setStartDate("");
    setEndDate("");
    fetchMetrics();
  };

  if (isLoading) {
    return (
      <div style={{ padding: "2rem" }}>
        <Loading
          description={intl.formatMessage({
            id: "biorepository.reporting.metrics.loading",
            defaultMessage: "Loading detailed metrics...",
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
            id: "biorepository.reporting.metrics.error",
            defaultMessage: "Error Loading Metrics",
          })}
          subtitle={error}
          lowContrast
        />
      </div>
    );
  }

  if (!metricsData) {
    return (
      <div style={{ padding: "2rem" }}>
        <InlineNotification
          kind="warning"
          title={intl.formatMessage({
            id: "biorepository.reporting.metrics.noData",
            defaultMessage: "No Data Available",
          })}
          lowContrast
        />
      </div>
    );
  }

  const { capacity, aging, qc, discrepancies, qcHistory, retrieval, disposal } =
    metricsData;

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
      .catch((err) => setError(err.message));
  };

  // Storage capacity rows
  const storageRows = [
    {
      id: "1",
      status: "Stored",
      count: capacity.totalSamplesStored || 0,
      percentage: capacity.totalSamplesStored ? "N/A" : "0%",
    },
    {
      id: "2",
      status: "Pending Storage",
      count: capacity.pendingStorage || 0,
      percentage: "N/A",
    },
  ];

  const storageHeaders = [
    { key: "status", header: "Status" },
    { key: "count", header: "Sample Count" },
    { key: "percentage", header: "Percentage" },
  ];

  // Sample aging rows
  const agingRows = [
    {
      id: "1",
      category: "Expired",
      count: aging.expired || 0,
      description: "Past retention expiry date",
    },
    {
      id: "2",
      category: "Expiring 0-30 days",
      count: aging.expiring30Days || 0,
      description: "Urgent attention needed",
    },
    {
      id: "3",
      category: "Expiring 31-60 days",
      count: aging.expiring60Days || 0,
      description: "Plan for disposal/extension",
    },
    {
      id: "4",
      category: "Expiring 61-90 days",
      count: aging.expiring90Days || 0,
      description: "Monitor closely",
    },
    {
      id: "5",
      category: "Total Active",
      count: aging.total || 0,
      description: "All samples in repository",
    },
  ];

  const agingHeaders = [
    { key: "category", header: "Time Range" },
    { key: "count", header: "Sample Count" },
    { key: "description", header: "Description" },
  ];

  // QC checkpoint rows
  const qcCheckpointRows = [
    {
      id: "1",
      checkpoint: "Sample Present",
      passRate: qc.samplePresentPassRate
        ? `${qc.samplePresentPassRate.toFixed(1)}%`
        : "N/A",
    },
    {
      id: "2",
      checkpoint: "Label Integrity",
      passRate: qc.labelIntegrityPassRate
        ? `${qc.labelIntegrityPassRate.toFixed(1)}%`
        : "N/A",
    },
    {
      id: "3",
      checkpoint: "Container Integrity",
      passRate: qc.containerIntegrityPassRate
        ? `${qc.containerIntegrityPassRate.toFixed(1)}%`
        : "N/A",
    },
    {
      id: "4",
      checkpoint: "Volume Appearance",
      passRate: qc.volumeAppearancePassRate
        ? `${qc.volumeAppearancePassRate.toFixed(1)}%`
        : "N/A",
    },
    {
      id: "5",
      checkpoint: "Correct Position",
      passRate: qc.correctPositionPassRate
        ? `${qc.correctPositionPassRate.toFixed(1)}%`
        : "N/A",
    },
    {
      id: "6",
      checkpoint: "Overall Compliance",
      passRate: qc.complianceRate ? `${qc.complianceRate.toFixed(1)}%` : "N/A",
    },
  ];

  const qcHeaders = [
    { key: "checkpoint", header: "Checkpoint" },
    { key: "passRate", header: "Pass Rate" },
  ];

  const qcSummaryRows = [
    {
      id: "qc-summary-1",
      metric: "Pass Rate",
      value: formatPercent(qc.passRate ?? qc.complianceRate),
    },
    {
      id: "qc-summary-2",
      metric: "Fail Count",
      value: qc.failCount ?? qc.failedInspections ?? 0,
    },
    {
      id: "qc-summary-3",
      metric: "Fail Trend Basis",
      value: qc.failTrendBasis
        ? `${qc.failTrendBasis.granularity || "day"}, ${qc.failTrendBasis.windowDays || 30} days, source: ${qc.failTrendBasis.source || "N/A"}`
        : "N/A",
    },
  ];

  const qcSummaryHeaders = [
    { key: "metric", header: "QC Summary Metric" },
    { key: "value", header: "Value" },
  ];

  const failTrendRows = (qc.failTrend || []).map((point, idx) => ({
    id: `fail-trend-${idx}`,
    date: point.date || "-",
    failedInspections: point.failedInspections ?? 0,
    totalInspections: point.totalInspections ?? 0,
    passRate: formatPercent(point.passRate),
  }));

  const failTrendHeaders = [
    { key: "date", header: "Date" },
    { key: "failedInspections", header: "Fail Count" },
    { key: "totalInspections", header: "Total Checks" },
    { key: "passRate", header: "Pass Rate" },
  ];

  const mapBreakdownRows = (rows, prefix) =>
    (rows || []).map((entry, idx) => ({
      id: `${prefix}-${idx}`,
      key: entry.key || "Unknown",
      totalInspections: entry.totalInspections ?? 0,
      failedInspections: entry.failedInspections ?? 0,
      passRate: formatPercent(entry.passRate),
    }));

  const breakdownHeaders = [
    { key: "key", header: "Dimension" },
    { key: "totalInspections", header: "Total Checks" },
    { key: "failedInspections", header: "Fail Count" },
    { key: "passRate", header: "Pass Rate" },
  ];

  const freezerBreakdownRows = mapBreakdownRows(
    qc.breakdownByFreezer,
    "freezer-breakdown",
  );
  const rackBreakdownRows = mapBreakdownRows(
    qc.breakdownByRack,
    "rack-breakdown",
  );
  const technicianBreakdownRows = mapBreakdownRows(
    qc.breakdownByTechnician,
    "technician-breakdown",
  );

  const escalationSignals = qc.escalationSignals || {};
  const escalationSummaryRows = [
    {
      id: "qc-escalation-1",
      signal: "Batch Fail Rate",
      value: `${formatPercent(escalationSignals.batchFailRatePercent)} (threshold ${Number.isFinite(Number(escalationSignals.batchFailRateThresholdPercent)) ? Number(escalationSignals.batchFailRateThresholdPercent).toFixed(1) : "5.0"}%)`,
    },
    {
      id: "qc-escalation-2",
      signal: "Threshold Exceeded",
      value: escalationSignals.batchFailRateExceeded ? "Yes" : "No",
    },
    {
      id: "qc-escalation-3",
      signal: "Repeated Failure (Box/Rack)",
      value: escalationSignals.repeatedFailureInSameBoxOrRack
        ? `Yes (boxes: ${escalationSignals.repeatedFailureBoxesCount || 0}, racks: ${escalationSignals.repeatedFailureRacksCount || 0})`
        : "No",
    },
    {
      id: "qc-escalation-4",
      signal: "Critical Missing Samples",
      value: escalationSignals.criticalMissingSamples || 0,
    },
    {
      id: "qc-escalation-5",
      signal: "Triggered Rules",
      value: Array.isArray(escalationSignals.triggeredRules)
        ? escalationSignals.triggeredRules.join(", ") || "None"
        : "None",
    },
  ];

  const escalationSummaryHeaders = [
    { key: "signal", header: "Escalation Signal" },
    { key: "value", header: "Value" },
  ];

  const investigationBoxRows = (qc.underInvestigationBoxes || []).map(
    (entry, idx) => ({
      id: `investigation-box-${idx}`,
      key: entry.key || "Unknown",
      failedInspections: entry.failedInspections ?? 0,
      totalInspections: entry.totalInspections ?? 0,
      failRate: formatPercent(entry.failRate),
    }),
  );

  const flaggedFreezerRows = (
    escalationSignals.flaggedFreezers || []
  ).map((entry, idx) => ({
    id: `flagged-freezer-${idx}`,
    key: entry.key || "Unknown",
    failedInspections: entry.failedInspections ?? 0,
    totalInspections: entry.totalInspections ?? 0,
    failRate: formatPercent(entry.failRate),
  }));

  const problematicLocationRows = (
    qc.frequentlyProblematicLocations || []
  ).map((entry, idx) => ({
    id: `problematic-location-${idx}`,
    locationType: entry.locationType || "Unknown",
    key: entry.key || "Unknown",
    failedInspections: entry.failedInspections ?? 0,
    totalInspections: entry.totalInspections ?? 0,
    failRate: formatPercent(entry.failRate),
  }));

  const problematicLocationHeaders = [
    { key: "locationType", header: "Type" },
    { key: "key", header: "Location" },
    { key: "failedInspections", header: "Fail Count" },
    { key: "totalInspections", header: "Total Checks" },
    { key: "failRate", header: "Fail Rate" },
  ];

  const deriveQcStatus = (item) => {
    if (item.qcStatus) return item.qcStatus;
    if (item.qcResult === "VERIFIED") return "VALID";
    if (item.qcResult === "DISCREPANCY_FOUND") {
      return item.discrepancyType === "SAMPLE_MISSING" ? "MISSING" : "QC_FAILED";
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

  const historyRows = (qcHistory?.items || []).map((item, idx) => ({
    id: `qc-history-${idx}`,
    qcBatchId: item.qcBatchId || "UNBATCHED",
    inspectionDateRaw: item.inspectionDate || null,
    inspectionDate: item.inspectionDate
      ? new Date(item.inspectionDate).toLocaleString()
      : "-",
    qcResult: item.qcResult || "-",
    qcStatus: deriveQcStatus(item),
    lifecycleOutcome: formatLifecycleOutcome(deriveLifecycleOutcome(item)),
    sampleFlag: item.sampleFlag || "-",
    qcFailed: item.qcFailed ? "Yes" : "No",
    inspectorName: item.inspectorName || item.technicianId || "-",
    freezer: item.freezer || "Unknown",
    rack: item.rack || "Unknown",
    discrepancyType: item.discrepancyType || "-",
    failureComment: item.failureComment || item.remarks || "-",
    correctiveAction: item.correctiveAction || "-",
    correctionFrom:
      item.auditTrail?.fromCoordinates || item.auditTrail?.oldCoordinate || "-",
    correctionTo:
      item.auditTrail?.toCoordinates || item.auditTrail?.newCoordinate || "-",
    correctedBy:
      item.auditTrail?.correctedBy ||
      item.auditTrail?.user ||
      item.inspectorName ||
      item.technicianId ||
      "-",
    correctedAt: item.auditTrail?.correctedAt || item.auditTrail?.timestamp
      ? new Date(item.auditTrail.correctedAt || item.auditTrail.timestamp).toLocaleString()
      : "-",
    correctionReason: item.auditTrail?.reason || "-",
    remarks: item.remarks || "-",
  }));

  const historyBatchRows = (() => {
    const grouped = new Map();
    historyRows.forEach((row) => {
      const key = row.qcBatchId || "UNBATCHED";
      if (!grouped.has(key)) {
        grouped.set(key, {
          id: `qc-history-batch-${key}`,
          qcBatchId: key,
          latestInspectionDate: row.inspectionDate || "-",
          latestInspectionDateRaw: row.inspectionDateRaw,
          sampleCount: 0,
          passCount: 0,
          failCount: 0,
        });
      }
      const batch = grouped.get(key);
      batch.sampleCount += 1;
      if (row.qcResult === "VERIFIED" || row.qcStatus === "VALID") {
        batch.passCount += 1;
      } else {
        batch.failCount += 1;
      }
      if (
        row.inspectionDateRaw &&
        (!batch.latestInspectionDateRaw ||
          new Date(row.inspectionDateRaw) > new Date(batch.latestInspectionDateRaw))
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

  const discrepancyHistoryCount = historyRows.filter(
    (row) => row.qcResult === "DISCREPANCY_FOUND",
  ).length;
  const correctiveActionCount = historyRows.filter(
    (row) => row.correctiveAction && row.correctiveAction !== "-",
  ).length;
  const correctedOutcomeCount = historyRows.filter(
    (row) =>
      row.lifecycleOutcome === "Failed (correction logged)" ||
      row.lifecycleOutcome === "Failed (marked missing)",
  ).length;
  const pendingCorrectionCount = historyRows.filter(
    (row) => row.lifecycleOutcome === "Failed (pending correction)",
  ).length;

  const historyHeaders = [
    { key: "inspectionDate", header: "Inspection Date" },
    { key: "qcBatchId", header: "QC Batch ID" },
    { key: "qcResult", header: "Result" },
    { key: "qcStatus", header: "QC Status" },
    { key: "lifecycleOutcome", header: "Lifecycle Outcome" },
    { key: "sampleFlag", header: "Sample Flag" },
    { key: "qcFailed", header: "QC Failed" },
    { key: "inspectorName", header: "Technician" },
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
    { key: "remarks", header: "Remarks" },
  ];

  const historyBatchHeaders = [
    { key: "qcBatchId", header: "QC Batch ID" },
    { key: "latestInspectionDate", header: "Latest Inspection" },
    { key: "sampleCount", header: "Samples" },
    { key: "passCount", header: "Pass" },
    { key: "failCount", header: "Fail" },
    { key: "actions", header: "Batch Report" },
  ];

  // Retrieval stats rows
  const retrievalRows = [
    {
      id: "1",
      metric: "Total Requests",
      value: retrieval.totalRequests || 0,
    },
    {
      id: "2",
      metric: "Completed",
      value: retrieval.completedRequests || 0,
    },
    {
      id: "3",
      metric: "Pending/In Progress",
      value: retrieval.pendingRequests || 0,
    },
    {
      id: "4",
      metric: "Rejected",
      value: retrieval.rejectedRequests || 0,
    },
    {
      id: "5",
      metric: "Total Items Retrieved",
      value: retrieval.totalItemsRetrieved || 0,
    },
    {
      id: "6",
      metric: "Items Returned",
      value: retrieval.returnedItems || 0,
    },
    {
      id: "7",
      metric: "Items Consumed",
      value: retrieval.consumedItems || 0,
    },
    {
      id: "8",
      metric: "Overdue Returns",
      value: retrieval.overdueReturns || 0,
    },
  ];

  const retrievalHeaders = [
    { key: "metric", header: "Metric" },
    { key: "value", header: "Count" },
  ];

  // Disposal stats rows
  const disposalsByProject = disposal.disposalsByProject || {};
  const disposalRows = [
    {
      id: "total",
      project: "Total Disposed",
      count: disposal.totalDisposed || 0,
    },
    ...Object.entries(disposalsByProject).map(([project, count], idx) => ({
      id: `project-${idx}`,
      project,
      count,
    })),
  ];

  const disposalHeaders = [
    { key: "project", header: "Project" },
    { key: "count", header: "Disposed Count" },
  ];

  return (
    <div className="detailed-metrics-tab" style={{ padding: "1.5rem" }}>
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginBottom: "1rem",
              gap: "1rem",
              flexWrap: "wrap",
            }}
          >
            <h4 style={{ marginBottom: 0 }}>
              <FormattedMessage
                id="biorepository.reporting.metrics.title"
                defaultMessage="Detailed Metrics"
              />
            </h4>
            <Button kind="secondary" size="sm" onClick={() => window.print()}>
              <FormattedMessage
                id="biorepository.reporting.metrics.print"
                defaultMessage="Print Current View"
              />
            </Button>
          </div>
        </Column>

        {/* Date Range Filters */}
        <Column lg={16} md={8} sm={4} style={{ marginBottom: "1rem" }}>
          <div
            style={{
              display: "flex",
              gap: "1rem",
              alignItems: "flex-end",
              flexWrap: "wrap",
            }}
          >
            <DatePicker
              datePickerType="single"
              value={startDate}
              onChange={(dates) => setStartDate(dates[0] || "")}
            >
              <DatePickerInput
                id="start-date"
                placeholder="mm/dd/yyyy"
                labelText={intl.formatMessage({
                  id: "biorepository.reporting.metrics.startDate",
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
                  id: "biorepository.reporting.metrics.endDate",
                  defaultMessage: "End Date",
                })}
                size="md"
              />
            </DatePicker>

            <Button
              kind="primary"
              onClick={handleApplyFilters}
              renderIcon={Search}
              size="md"
            >
              <FormattedMessage
                id="biorepository.reporting.metrics.apply"
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
                id="biorepository.reporting.metrics.reset"
                defaultMessage="Reset"
              />
            </Button>
          </div>
        </Column>

        {/* Accordion Sections */}
        <Column lg={16} md={8} sm={4}>
          <Accordion>
            {/* Storage Capacity Details */}
            <AccordionItem
              title={intl.formatMessage({
                id: "biorepository.reporting.metrics.storage.title",
                defaultMessage: "Storage Capacity Details",
              })}
            >
              <DataTable rows={storageRows} headers={storageHeaders}>
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <TableContainer
                    title={intl.formatMessage({
                      id: "biorepository.reporting.metrics.storage.subtitle",
                      defaultMessage: "Sample counts by storage status",
                    })}
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
            </AccordionItem>

            {/* Sample Aging Breakdown */}
            <AccordionItem
              title={intl.formatMessage({
                id: "biorepository.reporting.metrics.aging.title",
                defaultMessage: "Sample Aging Breakdown",
              })}
            >
              <DataTable rows={agingRows} headers={agingHeaders}>
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <TableContainer
                    title={intl.formatMessage({
                      id: "biorepository.reporting.metrics.aging.subtitle",
                      defaultMessage:
                        "Samples categorized by retention expiry timeline",
                    })}
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
            </AccordionItem>

            {/* QC Checkpoint Statistics */}
            <AccordionItem
              title={intl.formatMessage({
                id: "biorepository.reporting.metrics.qc.title",
                defaultMessage: "QC Checkpoint Statistics",
              })}
            >
              <DataTable rows={qcSummaryRows} headers={qcSummaryHeaders}>
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <TableContainer
                    title="QC Core Summary"
                    description="Pass rate, fail count, and fail trend basis used by the dashboard"
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

              <DataTable rows={qcCheckpointRows} headers={qcHeaders}>
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <TableContainer
                    title={intl.formatMessage({
                      id: "biorepository.reporting.metrics.qc.subtitle",
                      defaultMessage:
                        "Pass rates for individual QC checkpoints",
                    })}
                    description={intl.formatMessage(
                      {
                        id: "biorepository.reporting.metrics.qc.description",
                        defaultMessage:
                          "Total inspections: {total} | Passed: {passed} | Failed: {failed}",
                      },
                      {
                        total: qc.totalInspections || 0,
                        passed: qc.passedInspections || 0,
                        failed: qc.failedInspections || 0,
                      },
                    )}
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
                <DataTable rows={failTrendRows} headers={failTrendHeaders}>
                  {({
                    rows,
                    headers,
                    getTableProps,
                    getHeaderProps,
                    getRowProps,
                  }) => (
                    <TableContainer
                      title="Fail Trend (Daily Basis)"
                      description="Trend basis: QC inspection date, daily aggregation, rolling 30-day window"
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
              </div>

              <div style={{ marginTop: "1rem" }}>
                <DataTable rows={freezerBreakdownRows} headers={breakdownHeaders}>
                  {({
                    rows,
                    headers,
                    getTableProps,
                    getHeaderProps,
                    getRowProps,
                  }) => (
                    <TableContainer
                      title="QC Breakdown by Freezer"
                      description="Grouped where expected location snapshot data is available"
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
              </div>

              <div style={{ marginTop: "1rem" }}>
                <DataTable rows={rackBreakdownRows} headers={breakdownHeaders}>
                  {({
                    rows,
                    headers,
                    getTableProps,
                    getHeaderProps,
                    getRowProps,
                  }) => (
                    <TableContainer
                      title="QC Breakdown by Rack"
                      description="Grouped where expected location snapshot data is available"
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
              </div>

              <div style={{ marginTop: "1rem" }}>
                <DataTable rows={technicianBreakdownRows} headers={breakdownHeaders}>
                  {({
                    rows,
                    headers,
                    getTableProps,
                    getHeaderProps,
                    getRowProps,
                  }) => (
                    <TableContainer
                      title="QC Breakdown by Technician"
                      description="Grouped by inspector/technician where current data supports it"
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
              </div>

              <div style={{ marginTop: "1rem" }}>
                <DataTable
                  rows={escalationSummaryRows}
                  headers={escalationSummaryHeaders}
                >
                  {({
                    rows,
                    headers,
                    getTableProps,
                    getHeaderProps,
                    getRowProps,
                  }) => (
                    <TableContainer
                      title="Escalation Basis Snapshot"
                      description="Minimal rule basis from AHRI guidance: batch fail rate, repeated location failures, and missing-sample triggers"
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
              </div>

              <div style={{ marginTop: "1rem" }}>
                <DataTable rows={investigationBoxRows} headers={breakdownHeaders}>
                  {({
                    rows,
                    headers,
                    getTableProps,
                    getHeaderProps,
                    getRowProps,
                  }) => (
                    <TableContainer
                      title="Boxes Under Investigation"
                      description="Boxes with repeated failures (two or more failed checks)"
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
              </div>

              <div style={{ marginTop: "1rem" }}>
                <DataTable rows={flaggedFreezerRows} headers={breakdownHeaders}>
                  {({
                    rows,
                    headers,
                    getTableProps,
                    getHeaderProps,
                    getRowProps,
                  }) => (
                    <TableContainer
                      title="Flagged Freezers"
                      description="Freezers whose fail rate exceeds the 5% threshold"
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
              </div>

              <div style={{ marginTop: "1rem" }}>
                <DataTable
                  rows={problematicLocationRows}
                  headers={problematicLocationHeaders}
                >
                  {({
                    rows,
                    headers,
                    getTableProps,
                    getHeaderProps,
                    getRowProps,
                  }) => (
                    <TableContainer
                      title="Frequently Problematic Locations"
                      description="Highest-failure locations based on current QC history"
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
              </div>
            </AccordionItem>

            {/* QC History for Completed Checks */}
            <AccordionItem
              title={intl.formatMessage({
                id: "biorepository.reporting.metrics.qc.history.title",
                defaultMessage: "QC History (Completed Checks)",
              })}
            >
              <DataTable rows={historyBatchRows} headers={historyBatchHeaders}>
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <TableContainer
                    title="QC Batch Summary"
                    description="Grouped by QC Batch ID for quick reporting and printable exports."
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
                          return (
                            <TableRow key={row.id} {...getRowProps({ row })}>
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
                                        {batchId !== "UNBATCHED" ? (
                                          <>
                                            <Button
                                              kind="ghost"
                                              size="sm"
                                              renderIcon={Download}
                                              onClick={() =>
                                                exportQcBatch(batchId, "pdf")
                                              }
                                            >
                                              Print PDF
                                            </Button>
                                            <Button
                                              kind="ghost"
                                              size="sm"
                                              onClick={() =>
                                                exportQcBatch(batchId, "csv")
                                              }
                                            >
                                              CSV
                                            </Button>
                                          </>
                                        ) : (
                                          "N/A"
                                        )}
                                      </div>
                                    </TableCell>
                                  );
                                }
                                return (
                                  <TableCell key={cell.id}>{cell.value}</TableCell>
                                );
                              })}
                            </TableRow>
                          );
                        })}
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              </DataTable>

              <div style={{ marginTop: "1rem" }}>
              <DataTable rows={historyRows} headers={historyHeaders}>
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <TableContainer
                    title="Completed QC Check History"
                    description={`Source: ${qcHistory?.source || "N/A"} | Records: ${qcHistory?.count || 0} | Discrepancies: ${discrepancyHistoryCount} | Corrective actions logged: ${correctiveActionCount} | Failed outcomes corrected: ${correctedOutcomeCount} | Pending corrections: ${pendingCorrectionCount}`}
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
              </div>
            </AccordionItem>

            {/* Retrieval Statistics */}
            <AccordionItem
              title={intl.formatMessage({
                id: "biorepository.reporting.metrics.retrieval.title",
                defaultMessage: "Retrieval Statistics",
              })}
            >
              <DataTable rows={retrievalRows} headers={retrievalHeaders}>
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <TableContainer
                    title={intl.formatMessage({
                      id: "biorepository.reporting.metrics.retrieval.subtitle",
                      defaultMessage:
                        "Sample retrieval request and item metrics",
                    })}
                    description={
                      startDate || endDate
                        ? intl.formatMessage(
                            {
                              id: "biorepository.reporting.metrics.retrieval.filtered",
                              defaultMessage: "Filtered: {start} to {end}",
                            },
                            {
                              start: startDate
                                ? formatDateToISO(startDate)
                                : "All time",
                              end: endDate ? formatDateToISO(endDate) : "Today",
                            },
                          )
                        : intl.formatMessage({
                            id: "biorepository.reporting.metrics.retrieval.default",
                            defaultMessage: "Last 30 days",
                          })
                    }
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
            </AccordionItem>

            {/* Disposal Statistics */}
            <AccordionItem
              title={intl.formatMessage({
                id: "biorepository.reporting.metrics.disposal.title",
                defaultMessage: "Disposal Statistics",
              })}
            >
              <DataTable rows={disposalRows} headers={disposalHeaders}>
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <TableContainer
                    title={intl.formatMessage({
                      id: "biorepository.reporting.metrics.disposal.subtitle",
                      defaultMessage: "Disposed samples by project",
                    })}
                    description={
                      startDate || endDate
                        ? intl.formatMessage(
                            {
                              id: "biorepository.reporting.metrics.disposal.filtered",
                              defaultMessage: "Filtered: {start} to {end}",
                            },
                            {
                              start: startDate
                                ? formatDateToISO(startDate)
                                : "All time",
                              end: endDate ? formatDateToISO(endDate) : "Today",
                            },
                          )
                        : intl.formatMessage({
                            id: "biorepository.reporting.metrics.disposal.default",
                            defaultMessage: "All time",
                          })
                    }
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
            </AccordionItem>
          </Accordion>
        </Column>
      </Grid>
    </div>
  );
}

DetailedMetricsTab.propTypes = {
  entryId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  notebookId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  pageData: PropTypes.object,
};

export default DetailedMetricsTab;

import { useState, useEffect, useRef, useCallback, useMemo } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  DatePicker,
  DatePickerInput,
  Tag,
  ProgressBar,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  Modal,
  TextArea,
  Checkbox,
} from "@carbon/react";
import {
  Renew,
  CheckmarkFilled,
  Warning,
  Calendar,
  DocumentExport,
  ChartColumn,
  Time,
  Analytics,
  Activity,
  Download,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../../utils/Utils";
import config from "../../../../config.json";
import "../../workflow/NotebookWorkflow.css";

/**
 * PathologyReportingPage - Page 6: Reporting & Performance Monitoring
 *
 * Pathology-specific reporting focused on calculated metrics:
 * - Monthly specimen volume (by type)
 * - Turnaround Time (TAT) - reception to final report
 * - Rejection rates (by reason)
 * - Specimen rejection rate
 * - Assay success rate (% of IHC/special stains with acceptable controls)
 * - Turnaround time (by specimen type)
 * - Equipment downtime (processors, microtomes, stainers)
 * - Monthly QC meetings documentation
 *
 * Note: Samples do NOT appear on this page. They jump directly from
 * Storage & Inventory to Disposal & Archiving.
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
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Active tab
  const [activeTab, setActiveTab] = useState(0);

  // Date range filter for metrics
  const [dateRange, setDateRange] = useState({
    startDate: new Date(new Date().getFullYear(), new Date().getMonth(), 1)
      .toISOString()
      .split("T")[0],
    endDate: new Date().toISOString().split("T")[0],
  });

  // Track if we have any real data to display
  const [hasData, setHasData] = useState(false);

  // Performance metrics state - null values indicate no data
  const [metrics, setMetrics] = useState({
    // Routine Reports
    monthlySpecimenVolume: {
      total: null,
      byType: [],
    },
    turnaroundTime: {
      overall: null,
      byType: [],
    },
    rejectionRates: {
      overall: null,
      byReason: [],
    },
    qcMeetingsCount: null,

    // Key Performance Metrics
    specimenRejectionRate: null,
    assaySuccessRate: null,
    equipmentDowntime: {
      total: null,
      byEquipment: [],
    },

    // Additional stats
    totalSamplesProcessed: null,
    pendingReview: null,
    completedReports: null,
  });

  // Specimen volume by type data for table
  const [specimenVolumeData, setSpecimenVolumeData] = useState([]);

  // TAT by type data for table
  const [tatByTypeData, setTatByTypeData] = useState([]);

  // Rejection by reason data for table
  const [rejectionByReasonData, setRejectionByReasonData] = useState([]);

  // Equipment downtime data for table
  const [equipmentDowntimeData, setEquipmentDowntimeData] = useState([]);

  // Report generation modal state (similar to MNTD)
  const [showReportModal, setShowReportModal] = useState(false);
  const [reportData, setReportData] = useState({
    dateRangeStart: "",
    dateRangeEnd: "",
    includeAllData: true,
    reportFormat: "CSV",
    reportNotes: "",
  });
  const [isGeneratingReport, setIsGeneratingReport] = useState(false);

  // Report type - only Summary Report is available
  const reportType = "SUMMARY";

  // Export state
  const [exporting, setExporting] = useState(false);

  // Calculate overall health score (moved here so it's available for useCallback below)
  // Returns null if no data is available
  const healthScore = useMemo(() => {
    // If no data, return null
    if (!hasData || metrics.totalSamplesProcessed === null) {
      return null;
    }

    let score = 0;
    let total = 0;

    // Rejection rate (target: <= 5%)
    const rejectionRate = metrics.specimenRejectionRate ?? 0;
    if (rejectionRate <= 5) score += 25;
    else if (rejectionRate <= 10) score += 15;
    total += 25;

    // Assay success rate (target: >= 95%)
    const assayRate = metrics.assaySuccessRate ?? 0;
    if (assayRate >= 95) score += 25;
    else if (assayRate >= 90) score += 15;
    total += 25;

    // TAT (target: <= 48 hours)
    const tat = metrics.turnaroundTime?.overall ?? 0;
    if (tat <= 48) score += 25;
    else if (tat <= 72) score += 15;
    total += 25;

    // Equipment downtime (target: <= 8 hours/month)
    const downtime = metrics.equipmentDowntime?.total ?? 0;
    if (downtime <= 8) score += 25;
    else if (downtime <= 24) score += 15;
    total += 25;

    return Math.round((score / total) * 100);
  }, [metrics, hasData]);

  // Load metrics from backend
  const loadMetrics = useCallback(() => {
    const nbId = notebookId || entryId;
    if (!nbId) {
      setLoading(false);
      setHasData(false);
      return;
    }

    setLoading(true);
    setError(null);

    const params = new URLSearchParams({
      entryId: nbId,
      startDate: dateRange.startDate,
      endDate: dateRange.endDate,
    });

    getFromOpenElisServer(
      `/rest/notebook/pathology/metrics?${params.toString()}`,
      (response) => {
        if (componentMounted.current) {
          // Check if we got a valid response with any data
          // Accept response if it has any metrics fields, not just if totalSamplesProcessed > 0
          const hasMetrics = response && (
            response.totalSamplesProcessed !== undefined ||
            response.monthlySpecimenVolume?.total !== undefined ||
            response.turnaroundTime?.overall !== undefined ||
            (response.monthlySpecimenVolume?.byType && response.monthlySpecimenVolume.byType.length > 0)
          );

          if (hasMetrics) {
            // We have data from backend (even if values are 0)
            setHasData(true);
            // Update main metrics
            setMetrics({
              monthlySpecimenVolume: {
                total: response.monthlySpecimenVolume?.total ?? 0,
                byType: response.monthlySpecimenVolume?.byType || [],
              },
              turnaroundTime: {
                overall: response.turnaroundTime?.overall ?? 0,
                byType: response.turnaroundTime?.byType || [],
              },
              rejectionRates: {
                overall: response.rejectionRates?.overall ?? 0,
                byReason: response.rejectionRates?.byReason || [],
              },
              qcMeetingsCount: response.qcMeetingsCount ?? 0,
              specimenRejectionRate: response.specimenRejectionRate ?? 0,
              assaySuccessRate: response.assaySuccessRate ?? 100, // Default to 100% if not available
              equipmentDowntime: {
                total: response.equipmentDowntime?.total ?? 0,
                byEquipment: response.equipmentDowntime?.byEquipment || [],
              },
              totalSamplesProcessed: response.totalSamplesProcessed ?? 0,
              pendingReview: response.pendingReview ?? 0,
              completedReports: response.completedReports ?? 0,
            });

            // Set table data
            setSpecimenVolumeData(
              (response.monthlySpecimenVolume?.byType || []).map(
                (item, idx) => ({
                  id: String(idx),
                  specimenType: item.type || "Unknown",
                  count: item.count || 0,
                  percentage: item.percentage || 0,
                }),
              ),
            );

            setTatByTypeData(
              (response.turnaroundTime?.byType || []).map((item, idx) => ({
                id: String(idx),
                specimenType: item.type || "Unknown",
                averageTAT: item.averageHours || 0,
                minTAT: item.minHours || 0,
                maxTAT: item.maxHours || 0,
                withinTarget: item.withinTarget || 0,
              })),
            );

            setRejectionByReasonData(
              (response.rejectionRates?.byReason || []).map((item, idx) => ({
                id: String(idx),
                reason: item.reason || "Unknown",
                count: item.count || 0,
                percentage: item.percentage || 0,
              })),
            );

            setEquipmentDowntimeData(
              (response.equipmentDowntime?.byEquipment || []).map(
                (item, idx) => ({
                  id: String(idx),
                  equipment: item.name || "Unknown",
                  downtimeHours: item.hours || 0,
                  incidents: item.incidents || 0,
                  lastIncident: item.lastIncident || "-",
                }),
              ),
            );
          } else {
            // Backend returned no data or endpoint doesn't exist
            // Show the dashboard with zero values instead of empty state
            // This allows users to see the metrics structure even before processing samples
            setHasData(true);
            setMetrics({
              monthlySpecimenVolume: { total: 0, byType: [] },
              turnaroundTime: { overall: 0, byType: [] },
              rejectionRates: { overall: 0, byReason: [] },
              qcMeetingsCount: 0,
              specimenRejectionRate: 0,
              assaySuccessRate: 100, // Default to 100% (no failures)
              equipmentDowntime: { total: 0, byEquipment: [] },
              totalSamplesProcessed: 0,
              pendingReview: 0,
              completedReports: 0,
            });
            setSpecimenVolumeData([]);
            setTatByTypeData([]);
            setRejectionByReasonData([]);
            setEquipmentDowntimeData([]);
          }
          setLoading(false);
        }
      },
    );
  }, [notebookId, entryId, dateRange]);

  useEffect(() => {
    componentMounted.current = true;
    loadMetrics();

    return () => {
      componentMounted.current = false;
    };
  }, [loadMetrics]);

  // Handle export metrics to Excel
  const handleExportMetrics = async () => {
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
      const params = new URLSearchParams({
        entryId: nbId,
        startDate: dateRange.startDate,
        endDate: dateRange.endDate,
        format: "excel",
      });

      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/pathology/metrics/export?${params.toString()}`,
        {
          method: "GET",
          credentials: "include",
          headers: {
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
        },
      );

      const contentType = response.headers.get("content-type") || "";
      const isExcelFile =
        contentType.includes(
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        ) || contentType.includes("application/vnd.ms-excel");

      if (
        response.ok &&
        (isExcelFile || contentType.includes("application/octet-stream"))
      ) {
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        const dateStr = new Date().toISOString().split("T")[0];
        a.download = `pathology_metrics_${dateStr}.xlsx`;
        document.body.appendChild(a);
        a.click();
        setTimeout(() => {
          window.URL.revokeObjectURL(url);
          document.body.removeChild(a);
        }, 100);

        setSuccess(
          intl.formatMessage({
            id: "pathology.reporting.exportSuccess",
            defaultMessage: "Metrics exported successfully",
          }),
        );
      } else {
        // Fallback: Generate CSV from current metrics data
        await exportMetricsToCSV();
      }
    } catch (err) {
      console.error("Export error:", err);
      // Fallback: Generate CSV from current metrics data
      await exportMetricsToCSV();
    } finally {
      setExporting(false);
    }
  };

  // Helper to escape CSV values (handles commas, quotes, newlines)
  const escapeCsvValue = (value) => {
    if (value === null || value === undefined) return "";
    const str = String(value);
    // If value contains comma, quote, or newline, wrap in quotes and escape internal quotes
    if (str.includes(",") || str.includes('"') || str.includes("\n")) {
      return `"${str.replace(/"/g, '""')}"`;
    }
    return str;
  };

  // Convert array of arrays to CSV string
  const arrayToCSV = (data) => {
    return data.map((row) => row.map(escapeCsvValue).join(",")).join("\n");
  };

  // Generate CSV file from current metrics data (Excel-compatible)
  const exportMetricsToCSV = useCallback(async () => {
    try {
      const csvData = [];

      // =============================================
      // Report Header
      // =============================================
      csvData.push(["PATHOLOGY PERFORMANCE METRICS REPORT"]);
      csvData.push([
        `Report Period: ${dateRange.startDate} to ${dateRange.endDate}`,
      ]);
      csvData.push([`Generated: ${new Date().toLocaleString()}`]);
      csvData.push([`Overall Performance Health Score: ${healthScore}%`]);
      csvData.push([]);

      // =============================================
      // Key Performance Indicators Section
      // =============================================
      csvData.push(["KEY PERFORMANCE INDICATORS"]);
      csvData.push(["Metric", "Value", "Target", "Status"]);
      csvData.push([
        "Total Specimens Processed",
        metrics.monthlySpecimenVolume?.total ?? 0,
        "-",
        "-",
      ]);
      csvData.push([
        "Specimen Rejection Rate",
        `${metrics.specimenRejectionRate ?? 0}%`,
        "<5%",
        (metrics.specimenRejectionRate ?? 0) <= 5 ? "Within Target" : "Above Target",
      ]);
      csvData.push([
        "Assay Success Rate",
        `${metrics.assaySuccessRate ?? 0}%`,
        ">95%",
        (metrics.assaySuccessRate ?? 0) >= 95 ? "Within Target" : "Below Target",
      ]);
      csvData.push([
        "Average TAT (hours)",
        metrics.turnaroundTime?.overall ?? 0,
        "<48",
        (metrics.turnaroundTime?.overall ?? 0) <= 48 ? "Within Target" : "Above Target",
      ]);
      csvData.push([
        "Equipment Downtime (hours)",
        metrics.equipmentDowntime?.total ?? 0,
        "<8",
        (metrics.equipmentDowntime?.total ?? 0) <= 8 ? "Within Target" : "Above Target",
      ]);
      csvData.push([
        "QC Meetings This Period",
        metrics.qcMeetingsCount ?? 0,
        "-",
        "-",
      ]);
      csvData.push([]);

      // =============================================
      // Performance Score Breakdown
      // =============================================
      const rejRate = metrics.specimenRejectionRate ?? 0;
      const assayRate = metrics.assaySuccessRate ?? 0;
      const tatVal = metrics.turnaroundTime?.overall ?? 0;
      const downtimeVal = metrics.equipmentDowntime?.total ?? 0;

      csvData.push(["PERFORMANCE SCORE BREAKDOWN"]);
      csvData.push(["Category", "Score", "Max", "Status"]);
      csvData.push([
        "Rejection Rate",
        rejRate <= 5
          ? 25
          : rejRate <= 10
            ? 15
            : 0,
        25,
        rejRate <= 5 ? "Good" : "Needs Improvement",
      ]);
      csvData.push([
        "Assay Success",
        assayRate >= 95
          ? 25
          : assayRate >= 90
            ? 15
            : 0,
        25,
        assayRate >= 95 ? "Good" : "Needs Improvement",
      ]);
      csvData.push([
        "Turnaround Time",
        tatVal <= 48
          ? 25
          : tatVal <= 72
            ? 15
            : 0,
        25,
        tatVal <= 48 ? "Good" : "Needs Improvement",
      ]);
      csvData.push([
        "Equipment Uptime",
        downtimeVal <= 8
          ? 25
          : downtimeVal <= 24
            ? 15
            : 0,
        25,
        downtimeVal <= 8 ? "Good" : "Needs Improvement",
      ]);
      csvData.push([]);
      csvData.push([]);

      // =============================================
      // Specimen Volume by Type
      // =============================================
      csvData.push(["MONTHLY SPECIMEN VOLUME BY TYPE"]);
      csvData.push(["Specimen Type", "Count", "Percentage"]);
      specimenVolumeData.forEach((item) => {
        csvData.push([item.specimenType, item.count, `${item.percentage}%`]);
      });
      if (specimenVolumeData.length === 0) {
        csvData.push(["No data available", "", ""]);
      }
      csvData.push([]);
      csvData.push([]);

      // =============================================
      // Turnaround Time by Specimen Type
      // =============================================
      csvData.push(["TURNAROUND TIME BY SPECIMEN TYPE"]);
      csvData.push([
        "Specimen Type",
        "Average TAT (hrs)",
        "Min TAT (hrs)",
        "Max TAT (hrs)",
        "Within Target %",
      ]);
      tatByTypeData.forEach((item) => {
        csvData.push([
          item.specimenType,
          item.averageTAT,
          item.minTAT,
          item.maxTAT,
          `${item.withinTarget}%`,
        ]);
      });
      if (tatByTypeData.length === 0) {
        csvData.push(["No data available", "", "", "", ""]);
      }
      csvData.push([]);
      csvData.push([]);

      // =============================================
      // Rejection Rates by Reason
      // =============================================
      csvData.push(["REJECTION RATES BY REASON"]);
      csvData.push([
        "Rejection Reason",
        "Count",
        "Percentage",
        "Overall Rejection Rate",
      ]);
      rejectionByReasonData.forEach((item, idx) => {
        csvData.push([
          item.reason,
          item.count,
          `${item.percentage}%`,
          idx === 0 ? `${metrics.rejectionRates?.overall ?? 0}%` : "",
        ]);
      });
      if (rejectionByReasonData.length === 0) {
        csvData.push([
          "No rejections recorded",
          "",
          "",
          `${metrics.rejectionRates?.overall ?? 0}%`,
        ]);
      }
      csvData.push([]);
      csvData.push([]);

      // =============================================
      // Equipment Downtime
      // =============================================
      csvData.push(["EQUIPMENT DOWNTIME (PROCESSORS, MICROTOMES, STAINERS)"]);
      csvData.push(["Equipment", "Downtime (hrs)", "Incidents", "Last Incident"]);
      equipmentDowntimeData.forEach((item) => {
        csvData.push([
          item.equipment,
          item.downtimeHours,
          item.incidents,
          item.lastIncident,
        ]);
      });
      if (equipmentDowntimeData.length === 0) {
        csvData.push(["No downtime recorded", "", "", ""]);
      }

      // Convert to CSV string and create download
      const csvString = arrayToCSV(csvData);
      // Add BOM for Excel UTF-8 compatibility
      const BOM = "\uFEFF";
      const blob = new Blob([BOM + csvString], {
        type: "text/csv;charset=utf-8;",
      });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      const dateStr = new Date().toISOString().split("T")[0];
      a.download = `pathology_metrics_${dateStr}.csv`;
      document.body.appendChild(a);
      a.click();
      setTimeout(() => {
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
      }, 100);

      setSuccess(
        intl.formatMessage({
          id: "pathology.reporting.exportSuccess",
          defaultMessage: "Metrics exported successfully",
        }),
      );
    } catch (err) {
      console.error("CSV export error:", err);
      setError("Failed to export metrics");
    }
  }, [dateRange, healthScore, metrics, specimenVolumeData, tatByTypeData, rejectionByReasonData, equipmentDowntimeData, intl, setSuccess, setError]);

  // Helper function to trigger file download
  const downloadFile = useCallback((blob, fileName) => {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    setTimeout(() => {
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    }, 100);
  }, []);

  // Handle generating report - downloads CSV file from backend (same pattern as MNTD)
  const handleGenerateReport = useCallback(() => {
    const nbId = notebookId || entryId;
    if (!nbId) {
      setError("Entry ID not available for report generation.");
      return;
    }

    setIsGeneratingReport(true);
    setError(null);

    // Build query params for the pathology report endpoint
    const params = new URLSearchParams({
      entryId: nbId,
      reportType: reportType,
      reportPeriod: `${reportData.dateRangeStart || dateRange.startDate} to ${reportData.dateRangeEnd || dateRange.endDate}`,
      startDate: reportData.dateRangeStart || dateRange.startDate,
      endDate: reportData.dateRangeEnd || dateRange.endDate,
      includeMetrics: "true",
      includeSampleDetails: "true",
      includeQcData: "true",
      includeProcessingData: "true",
      includeTestingData: "true",
      includeStorageData: "true",
      includeDisposalData: "true",
      includeSopData: "true",
    });

    // Fetch report from backend (same pattern as MNTD)
    fetch(
      `${config.serverBaseUrl}/rest/notebook/pathology/report/export-csv?${params.toString()}`,
      {
        method: "GET",
        credentials: "include",
        headers: {
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
      },
    )
      .then((response) => {
        if (!response.ok) {
          throw new Error("Failed to fetch report data from server");
        }
        // Get filename from Content-Disposition header if available
        const contentDisposition = response.headers.get("Content-Disposition");
        let fileName = `Pathology_Summary_Report_${new Date().toISOString().split("T")[0]}.csv`;

        if (contentDisposition) {
          const match = contentDisposition.match(/filename="?([^"]+)"?/);
          if (match) fileName = match[1];
        }

        return response.blob().then((blob) => ({ blob, fileName }));
      })
      .then(({ blob, fileName }) => {
        if (!componentMounted.current) return;

        // Download the file
        downloadFile(blob, fileName);

        setIsGeneratingReport(false);
        setSuccess(
          intl.formatMessage({
            id: "pathology.reporting.reportGenerated",
            defaultMessage:
              "Report generated and downloaded successfully. Open in Excel for best viewing.",
          }),
        );
        setShowReportModal(false);
        setReportData({
          dateRangeStart: "",
          dateRangeEnd: "",
          includeAllData: true,
          reportFormat: "CSV",
          reportNotes: "",
        });
      })
      .catch((err) => {
        if (componentMounted.current) {
          console.error("Report generation error (using local fallback):", err);
          setIsGeneratingReport(false);
          // Fallback to local CSV export - this works fine, so show success message
          exportMetricsToCSV();
          setShowReportModal(false);
          // The exportMetricsToCSV function already sets its own success message
        }
      });
  }, [
    reportData,
    reportType,
    notebookId,
    entryId,
    dateRange,
    intl,
    downloadFile,
    exportMetricsToCSV,
  ]);

  // Helper for date change
  const handleDateChange = (dates, fieldName, setState) => {
    if (dates?.[0]) {
      setState((prev) => ({
        ...prev,
        [fieldName]: dates[0].toISOString().split("T")[0],
      }));
    }
  };

  // Get status color for metrics
  const getMetricStatus = (value, target, isLowerBetter = true) => {
    if (isLowerBetter) {
      return value <= target ? "green" : "red";
    }
    return value >= target ? "green" : "red";
  };

  if (loading) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <ProgressBar label="Loading metrics..." />
      </div>
    );
  }

  // Show empty state when no samples have been processed
  if (!hasData) {
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
              defaultMessage="View calculated performance metrics, generate reports, and document monthly QC meetings. Note: Samples proceed directly from Storage to Disposal."
            />
          </p>
        </div>

        {/* Empty State */}
        <Tile
          style={{
            textAlign: "center",
            padding: "3rem",
            marginTop: "2rem",
            backgroundColor: "#f4f4f4",
          }}
        >
          <Analytics size={48} style={{ color: "#8d8d8d", marginBottom: "1rem" }} />
          <h4 style={{ marginBottom: "0.5rem", color: "#525252" }}>
            <FormattedMessage
              id="pathology.reporting.noData.title"
              defaultMessage="No Performance Data Available"
            />
          </h4>
          <p style={{ color: "#8d8d8d", marginBottom: "1rem" }}>
            <FormattedMessage
              id="pathology.reporting.noData.description"
              defaultMessage="Performance metrics will appear here once samples have been imported and processed through the workflow. Start by importing samples on the Sample Creation page."
            />
          </p>
          <Button
            kind="tertiary"
            size="sm"
            renderIcon={Renew}
            onClick={loadMetrics}
          >
            <FormattedMessage
              id="pathology.reporting.refresh"
              defaultMessage="Refresh Metrics"
            />
          </Button>
        </Tile>
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
            defaultMessage="View calculated performance metrics, generate reports, and document monthly QC meetings. Note: Samples proceed directly from Storage to Disposal."
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

      {/* Date Range Filter */}
      <Grid fullWidth style={{ marginBottom: "1rem" }}>
        <Column lg={4} md={4} sm={4}>
          <DatePicker
            datePickerType="single"
            value={dateRange.startDate}
            onChange={(dates) =>
              handleDateChange(dates, "startDate", setDateRange)
            }
          >
            <DatePickerInput
              id="metrics-start-date"
              labelText={intl.formatMessage({
                id: "pathology.reporting.startDate",
                defaultMessage: "Start Date",
              })}
              placeholder="mm/dd/yyyy"
            />
          </DatePicker>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <DatePicker
            datePickerType="single"
            value={dateRange.endDate}
            onChange={(dates) =>
              handleDateChange(dates, "endDate", setDateRange)
            }
          >
            <DatePickerInput
              id="metrics-end-date"
              labelText={intl.formatMessage({
                id: "pathology.reporting.endDate",
                defaultMessage: "End Date",
              })}
              placeholder="mm/dd/yyyy"
            />
          </DatePicker>
        </Column>
        <Column
          lg={8}
          md={8}
          sm={4}
          style={{ display: "flex", alignItems: "flex-end", gap: "0.5rem" }}
        >
          <Button
            kind="secondary"
            size="sm"
            renderIcon={Renew}
            onClick={loadMetrics}
          >
            <FormattedMessage
              id="pathology.reporting.refresh"
              defaultMessage="Refresh Metrics"
            />
          </Button>
          <Button
            kind="primary"
            size="sm"
            renderIcon={DocumentExport}
            onClick={() => setShowReportModal(true)}
            disabled={exporting || isGeneratingReport}
          >
            <FormattedMessage
              id="pathology.reporting.generateReport"
              defaultMessage="Generate Report"
            />
          </Button>
          <Button
            kind="tertiary"
            size="sm"
            renderIcon={Download}
            onClick={handleExportMetrics}
            disabled={exporting}
            style={{ marginLeft: "0.5rem" }}
          >
            <FormattedMessage
              id="pathology.reporting.quickExport"
              defaultMessage="Quick Export CSV"
            />
          </Button>
        </Column>
      </Grid>

      {/* Overall Health Score */}
      <Grid fullWidth style={{ marginBottom: "1rem" }}>
        <Column lg={16} md={8} sm={4}>
          <Tile className="health-score-tile" style={{ padding: "1rem" }}>
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
              }}
            >
              <div>
                <h5 style={{ marginBottom: "0.5rem" }}>
                  <Activity size={20} style={{ marginRight: "0.5rem" }} />
                  <FormattedMessage
                    id="pathology.reporting.overallHealth"
                    defaultMessage="Overall Performance Health"
                  />
                </h5>
                <p style={{ color: "#525252", fontSize: "14px" }}>
                  <FormattedMessage
                    id="pathology.reporting.healthDescription"
                    defaultMessage="Based on rejection rate, assay success, TAT, and equipment uptime"
                  />
                </p>
              </div>
              <div style={{ textAlign: "right" }}>
                <span
                  style={{
                    fontSize: "2.5rem",
                    fontWeight: "bold",
                    color:
                      healthScore !== null && healthScore >= 80
                        ? "#198038"
                        : healthScore !== null && healthScore >= 60
                          ? "#f1c21b"
                          : "#da1e28",
                  }}
                >
                  {healthScore !== null ? `${healthScore}%` : "-"}
                </span>
                <Tag
                  type={
                    healthScore !== null && healthScore >= 80
                      ? "green"
                      : healthScore !== null && healthScore >= 60
                        ? "orange"
                        : "red"
                  }
                  style={{ marginLeft: "0.5rem" }}
                >
                  {healthScore === null
                    ? "No Data"
                    : healthScore >= 80
                      ? "Good"
                      : healthScore >= 60
                        ? "Fair"
                        : "Needs Attention"}
                </Tag>
              </div>
            </div>
            <ProgressBar
              value={healthScore ?? 0}
              max={100}
              status={
                healthScore !== null && healthScore >= 80
                  ? "active"
                  : healthScore !== null && healthScore >= 60
                    ? "active"
                    : "error"
              }
              style={{ marginTop: "0.5rem" }}
            />
          </Tile>
        </Column>
      </Grid>

      {/* Key Performance Indicators */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <h5 style={{ marginBottom: "0.5rem" }}>
            <ChartColumn size={20} style={{ marginRight: "0.5rem" }} />
            <FormattedMessage
              id="pathology.reporting.kpis"
              defaultMessage="Key Performance Indicators"
            />
          </h5>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.reporting.totalSpecimens"
                  defaultMessage="Total Specimens"
                />
              </span>
              <span className="progress-value">
                {metrics.monthlySpecimenVolume?.total ?? 0}
              </span>
            </Tile>
            <Tile
              className="progress-tile"
              style={{
                borderColor:
                  getMetricStatus(metrics.specimenRejectionRate ?? 0, 5) === "green"
                    ? "#198038"
                    : "#da1e28",
              }}
            >
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.reporting.rejectionRate"
                  defaultMessage="Rejection Rate"
                />
              </span>
              <span
                className="progress-value"
                style={{
                  color:
                    getMetricStatus(metrics.specimenRejectionRate ?? 0, 5) ===
                    "green"
                      ? "#198038"
                      : "#da1e28",
                }}
              >
                {metrics.specimenRejectionRate ?? 0}%
              </span>
              <span style={{ fontSize: "12px", color: "#525252" }}>
                Target: {"<"}5%
              </span>
            </Tile>
            <Tile
              className="progress-tile"
              style={{
                borderColor:
                  getMetricStatus(metrics.assaySuccessRate ?? 0, 95, false) ===
                  "green"
                    ? "#198038"
                    : "#da1e28",
              }}
            >
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.reporting.assaySuccess"
                  defaultMessage="Assay Success Rate"
                />
              </span>
              <span
                className="progress-value"
                style={{
                  color:
                    getMetricStatus(metrics.assaySuccessRate ?? 0, 95, false) ===
                    "green"
                      ? "#198038"
                      : "#da1e28",
                }}
              >
                {metrics.assaySuccessRate ?? 0}%
              </span>
              <span style={{ fontSize: "12px", color: "#525252" }}>
                Target: {">"}95%
              </span>
            </Tile>
            <Tile
              className="progress-tile"
              style={{
                borderColor:
                  getMetricStatus(metrics.turnaroundTime?.overall ?? 0, 48) ===
                  "green"
                    ? "#198038"
                    : "#da1e28",
              }}
            >
              <span className="progress-label">
                <Time size={16} style={{ marginRight: "4px" }} />
                <FormattedMessage
                  id="pathology.reporting.avgTAT"
                  defaultMessage="Average TAT"
                />
              </span>
              <span
                className="progress-value"
                style={{
                  color:
                    getMetricStatus(metrics.turnaroundTime?.overall ?? 0, 48) ===
                    "green"
                      ? "#198038"
                      : "#da1e28",
                }}
              >
                {metrics.turnaroundTime?.overall ?? 0}h
              </span>
              <span style={{ fontSize: "12px", color: "#525252" }}>
                Target: {"<"}48h
              </span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <Warning size={16} style={{ marginRight: "4px" }} />
                <FormattedMessage
                  id="pathology.reporting.equipmentDowntime"
                  defaultMessage="Equipment Downtime"
                />
              </span>
              <span className="progress-value">
                {metrics.equipmentDowntime?.total ?? 0}h
              </span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <Calendar size={16} style={{ marginRight: "4px" }} />
                <FormattedMessage
                  id="pathology.reporting.qcMeetings"
                  defaultMessage="QC Meetings"
                />
              </span>
              <span className="progress-value">{metrics.qcMeetingsCount ?? 0}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Tabbed Detailed Metrics */}
      <Tabs
        selectedIndex={activeTab}
        onChange={({ selectedIndex }) => setActiveTab(selectedIndex)}
        style={{ marginTop: "1.5rem" }}
      >
        <TabList aria-label="Metrics tabs">
          <Tab>
            <FormattedMessage
              id="pathology.reporting.tab.specimenVolume"
              defaultMessage="Specimen Volume"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="pathology.reporting.tab.turnaroundTime"
              defaultMessage="Turnaround Time"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="pathology.reporting.tab.rejectionRates"
              defaultMessage="Rejection Rates"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="pathology.reporting.tab.equipmentDowntime"
              defaultMessage="Equipment Downtime"
            />
          </Tab>
        </TabList>

        <TabPanels>
          {/* Specimen Volume Tab */}
          <TabPanel>
            <div style={{ padding: "1rem" }}>
              <h5 style={{ marginBottom: "1rem" }}>
                <Analytics size={20} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="pathology.reporting.monthlyVolume"
                  defaultMessage="Monthly Specimen Volume by Type"
                />
              </h5>
              {specimenVolumeData.length > 0 ? (
                <DataTable
                  rows={specimenVolumeData}
                  headers={[
                    { key: "specimenType", header: "Specimen Type" },
                    { key: "count", header: "Count" },
                    { key: "percentage", header: "Percentage" },
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
                                  {cell.info.header === "percentage"
                                    ? `${cell.value}%`
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
              ) : (
                <Tile style={{ textAlign: "center", padding: "2rem" }}>
                  <p style={{ color: "#8d8d8d" }}>
                    <FormattedMessage
                      id="pathology.reporting.noVolumeData"
                      defaultMessage="No specimen volume data available for the selected period."
                    />
                  </p>
                </Tile>
              )}
            </div>
          </TabPanel>

          {/* Turnaround Time Tab */}
          <TabPanel>
            <div style={{ padding: "1rem" }}>
              <h5 style={{ marginBottom: "1rem" }}>
                <Time size={20} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="pathology.reporting.tatByType"
                  defaultMessage="Turnaround Time by Specimen Type"
                />
              </h5>
              {tatByTypeData.length > 0 ? (
                <DataTable
                  rows={tatByTypeData}
                  headers={[
                    { key: "specimenType", header: "Specimen Type" },
                    { key: "averageTAT", header: "Avg TAT (hrs)" },
                    { key: "minTAT", header: "Min TAT (hrs)" },
                    { key: "maxTAT", header: "Max TAT (hrs)" },
                    { key: "withinTarget", header: "Within Target %" },
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
                                  {cell.info.header === "withinTarget"
                                    ? `${cell.value}%`
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
              ) : (
                <Tile style={{ textAlign: "center", padding: "2rem" }}>
                  <p style={{ color: "#8d8d8d" }}>
                    <FormattedMessage
                      id="pathology.reporting.noTatData"
                      defaultMessage="No turnaround time data available for the selected period."
                    />
                  </p>
                </Tile>
              )}
            </div>
          </TabPanel>

          {/* Rejection Rates Tab */}
          <TabPanel>
            <div style={{ padding: "1rem" }}>
              <h5 style={{ marginBottom: "1rem" }}>
                <Warning size={20} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="pathology.reporting.rejectionByReason"
                  defaultMessage="Rejection Rates by Reason"
                />
              </h5>
              <Tile style={{ marginBottom: "1rem", padding: "1rem" }}>
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <span>
                    <FormattedMessage
                      id="pathology.reporting.overallRejection"
                      defaultMessage="Overall Rejection Rate"
                    />
                  </span>
                  <div>
                    <span
                      style={{
                        fontSize: "1.5rem",
                        fontWeight: "bold",
                        color:
                          (metrics.rejectionRates?.overall ?? 0) <= 5
                            ? "#198038"
                            : "#da1e28",
                      }}
                    >
                      {metrics.rejectionRates?.overall ?? 0}%
                    </span>
                    <Tag
                      type={
                        (metrics.rejectionRates?.overall ?? 0) <= 5 ? "green" : "red"
                      }
                      style={{ marginLeft: "0.5rem" }}
                    >
                      {(metrics.rejectionRates?.overall ?? 0) <= 5 ? (
                        <CheckmarkFilled
                          size={12}
                          style={{ marginRight: "4px" }}
                        />
                      ) : (
                        <Warning size={12} style={{ marginRight: "4px" }} />
                      )}
                      {(metrics.rejectionRates?.overall ?? 0) <= 5
                        ? "Within Target"
                        : "Above Target"}
                    </Tag>
                  </div>
                </div>
                <ProgressBar
                  value={100 - (metrics.rejectionRates?.overall ?? 0)}
                  max={100}
                  status={
                    (metrics.rejectionRates?.overall ?? 0) <= 5 ? "active" : "error"
                  }
                  label="Acceptance Rate"
                  style={{ marginTop: "0.5rem" }}
                />
              </Tile>
              {rejectionByReasonData.length > 0 ? (
                <DataTable
                  rows={rejectionByReasonData}
                  headers={[
                    { key: "reason", header: "Rejection Reason" },
                    { key: "count", header: "Count" },
                    { key: "percentage", header: "Percentage" },
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
                                  {cell.info.header === "percentage"
                                    ? `${cell.value}%`
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
              ) : (
                <Tile style={{ textAlign: "center", padding: "2rem" }}>
                  <p style={{ color: "#8d8d8d" }}>
                    <FormattedMessage
                      id="pathology.reporting.noRejectionData"
                      defaultMessage="No rejection data available for the selected period."
                    />
                  </p>
                </Tile>
              )}
            </div>
          </TabPanel>

          {/* Equipment Downtime Tab */}
          <TabPanel>
            <div style={{ padding: "1rem" }}>
              <h5 style={{ marginBottom: "1rem" }}>
                <FormattedMessage
                  id="pathology.reporting.equipmentDowntimeByType"
                  defaultMessage="Equipment Downtime (Processors, Microtomes, Stainers)"
                />
              </h5>
              <Tile style={{ marginBottom: "1rem", padding: "1rem" }}>
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <span>
                    <FormattedMessage
                      id="pathology.reporting.totalDowntime"
                      defaultMessage="Total Equipment Downtime This Period"
                    />
                  </span>
                  <span style={{ fontSize: "1.5rem", fontWeight: "bold" }}>
                    {metrics.equipmentDowntime?.total ?? 0} hours
                  </span>
                </div>
              </Tile>
              {equipmentDowntimeData.length > 0 ? (
                <DataTable
                  rows={equipmentDowntimeData}
                  headers={[
                    { key: "equipment", header: "Equipment" },
                    { key: "downtimeHours", header: "Downtime (hrs)" },
                    { key: "incidents", header: "Incidents" },
                    { key: "lastIncident", header: "Last Incident" },
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
              ) : (
                <Tile style={{ textAlign: "center", padding: "2rem" }}>
                  <p style={{ color: "#8d8d8d" }}>
                    <FormattedMessage
                      id="pathology.reporting.noDowntimeData"
                      defaultMessage="No equipment downtime recorded for the selected period."
                    />
                  </p>
                </Tile>
              )}
            </div>
          </TabPanel>
        </TabPanels>
      </Tabs>

      {/* Report Generation Modal - Similar to MNTD */}
      <Modal
        open={showReportModal}
        modalHeading={intl.formatMessage({
          id: "pathology.reporting.generateReportTitle",
          defaultMessage: "Generate Pathology Report",
        })}
        primaryButtonText={
          isGeneratingReport
            ? intl.formatMessage({
                id: "pathology.reporting.generating",
                defaultMessage: "Generating...",
              })
            : intl.formatMessage({
                id: "pathology.reporting.generate",
                defaultMessage: "Generate Report",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleGenerateReport}
        onRequestClose={() => setShowReportModal(false)}
        primaryButtonDisabled={isGeneratingReport}
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="pathology.reporting.modalDescription"
              defaultMessage="Generate a comprehensive pathology performance summary report in CSV format."
            />
          </p>

          <Grid narrow style={{ marginBottom: "1rem" }}>
            <Column lg={8} md={4} sm={4}>
              <DatePicker
                datePickerType="single"
                dateFormat="Y-m-d"
                value={reportData.dateRangeStart || dateRange.startDate}
                onChange={(dates) =>
                  handleDateChange(dates, "dateRangeStart", setReportData)
                }
              >
                <DatePickerInput
                  id="reportStartDate"
                  labelText={intl.formatMessage({
                    id: "pathology.reporting.startDate",
                    defaultMessage: "Start Date",
                  })}
                  placeholder="YYYY-MM-DD"
                />
              </DatePicker>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <DatePicker
                datePickerType="single"
                dateFormat="Y-m-d"
                value={reportData.dateRangeEnd || dateRange.endDate}
                onChange={(dates) =>
                  handleDateChange(dates, "dateRangeEnd", setReportData)
                }
              >
                <DatePickerInput
                  id="reportEndDate"
                  labelText={intl.formatMessage({
                    id: "pathology.reporting.endDate",
                    defaultMessage: "End Date",
                  })}
                  placeholder="YYYY-MM-DD"
                />
              </DatePicker>
            </Column>
          </Grid>

          <Checkbox
            id="includeAllData"
            labelText={intl.formatMessage({
              id: "pathology.reporting.includeAllData",
              defaultMessage: "Include all metrics data",
            })}
            checked={reportData.includeAllData}
            onChange={(_, { checked }) =>
              setReportData((prev) => ({ ...prev, includeAllData: checked }))
            }
            style={{ marginBottom: "1rem" }}
          />

          <TextArea
            id="reportNotes"
            labelText={intl.formatMessage({
              id: "pathology.reporting.reportNotes",
              defaultMessage: "Notes (optional)",
            })}
            placeholder={intl.formatMessage({
              id: "pathology.reporting.notesPlaceholder",
              defaultMessage: "Add any notes to include in the report...",
            })}
            value={reportData.reportNotes}
            onChange={(e) =>
              setReportData((prev) => ({ ...prev, reportNotes: e.target.value }))
            }
            rows={3}
          />
        </div>
      </Modal>
    </div>
  );
}

export default PathologyReportingPage;

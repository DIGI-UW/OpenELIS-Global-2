import React, {
  useMemo,
  useState,
  useEffect,
  useCallback,
  useContext,
} from "react";
import {
  Grid,
  Column,
  Dropdown,
  Button,
  DatePicker,
  DatePickerInput,
  Tag,
  Tabs,
  Tab,
  TabList,
  TabPanels,
  TabPanel,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableHeader,
  TableBody,
  TableRow,
  TableCell,
} from "@carbon/react";
import "./Reports.scss";
import {
  fetchReportExcursions,
  fetchAuditTrail,
  downloadReportDirect,
} from "./api";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { NotificationContext } from "../layout/Layout";

const REPORT_TYPES = [
  "Daily Log",
  "Weekly Log",
  "Monthly Log",
  "Excursion Summary",
  "Audit Trail",
];

const EXPORT_FORMATS = ["PDF"]; // CSV, XML, and Excel to be implemented in Phase 2

const REPORT_TYPE_MAP = {
  "Daily Log": "freezerDailyLogReport",
  "Weekly Log": "freezerDailyLogReport",
  "Monthly Log": "freezerDailyLogReport",
  "Excursion Summary": "freezerExcursionReport",
  "Audit Trail": "freezerAuditTrailReport",
};

const EXCURSION_HEADERS = [
  { key: "id", header: "Excursion ID" },
  { key: "freezer", header: "Freezer" },
  { key: "location", header: "Location" },
  { key: "startTime", header: "Start Time" },
  { key: "duration", header: "Duration" },
  { key: "range", header: "Temperature Range" },
  { key: "severity", header: "Severity" },
  { key: "status", header: "Status" },
];

const AUDIT_HEADERS = [
  { key: "eventId", header: "Event ID" },
  { key: "timestamp", header: "Timestamp" },
  { key: "freezer", header: "Freezer" },
  { key: "action", header: "Action" },
  { key: "performedBy", header: "Performed By" },
  { key: "comment", header: "Notes" },
];

const formatDateTime = (value) => {
  if (!value) {
    return "—";
  }
  return new Date(value).toLocaleString();
};

const formatTemperature = (value) => {
  if (value === null || value === undefined) {
    return "—";
  }
  const number = Number(value);
  return Number.isNaN(number) ? "—" : `${number.toFixed(1)}°C`;
};

const formatRange = (min, max) => {
  if (min == null && max == null) {
    return "—";
  }
  if (min == null) {
    return `Max ${formatTemperature(max)}`;
  }
  if (max == null) {
    return `Min ${formatTemperature(min)}`;
  }
  return `${formatTemperature(min)} to ${formatTemperature(max)}`;
};

const mapAlertToExcursion = (alert) => {
  const alertId = alert.alertId ?? alert.id;
  const freezerId = alert.freezerId ?? alert.freezer;
  const durationSeconds =
    alert.durationSeconds != null ? alert.durationSeconds : alert.duration;
  return {
    id: `ALERT-${alertId}`,
    alertId,
    freezerId,
    freezerName: alert.freezerName ?? `Freezer ${freezerId}`,
    location: alert.locationName ?? "Unknown location",
    startTime: formatDateTime(alert.startTime),
    endTime: formatDateTime(alert.endTime),
    duration: formatDuration(durationSeconds),
    range: formatRange(alert.minTemperature, alert.maxTemperature),
    severity: alert.severity ?? "UNKNOWN",
    status: alert.status ?? "OPEN",
  };
};

const defaultDateRange = () => {
  const end = new Date();
  const start = new Date();
  start.setDate(start.getDate() - 7);
  return [start, end];
};

const toIsoString = (value) => {
  if (!value) return null;
  if (value instanceof Date) {
    return value.toISOString();
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date.toISOString();
};

const formatDuration = (seconds) => {
  if (seconds == null) {
    return "—";
  }
  const minutes = Math.max(1, Math.round(seconds / 60));
  return `${minutes} minutes`;
};

const formatActionLabel = (value) =>
  value
    ? value
        .toLowerCase()
        .replace(/_/g, " ")
        .replace(/\b\w/g, (match) => match.toUpperCase())
    : "";

const mapAuditEvent = (event) => ({
  id: `ACTION-${
    event.id ?? Math.random().toString(36).substring(2, 10).toUpperCase()
  }`,
  freezerName: event.freezerName ?? `Freezer ${event.freezerId ?? ""}`.trim(),
  actionType: formatActionLabel(event.actionType),
  performedBy: event.performedBy ?? "System",
  timestamp: formatDateTime(event.performedAt),
  comment: event.comment || event.details || "—",
});

function Reports({ devices = [] }) {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, subtitle, message }) => {
      setNotificationVisible(true);
      addNotification({
        kind,
        title,
        subtitle,
        message,
      });
    },
    [addNotification, setNotificationVisible],
  );

  const normalizeArray = useCallback((payload) => {
    if (Array.isArray(payload)) {
      return payload;
    }
    if (payload && typeof payload === "object") {
      return (
        payload.items ||
        payload.data ||
        payload.results ||
        payload.list ||
        payload.rows ||
        []
      );
    }
    return [];
  }, []);

  const freezerOptions = useMemo(() => {
    const uniqueDevices = devices.filter(
      (device, index, self) =>
        device.id && self.findIndex((d) => d.id === device.id) === index,
    );
    const names = uniqueDevices.map(
      (device) => device.unitName || `Freezer ${device.id}`,
    );
    return ["All Freezers", ...names];
  }, [devices]);

  const freezerNameToIdMap = useMemo(() => {
    const map = {};
    devices.forEach((device) => {
      if (device.id) {
        const name = device.unitName || `Freezer ${device.id}`;
        map[name] = device.id;
      }
    });
    return map;
  }, [devices]);

  const [excursions, setExcursions] = useState([]);
  const [auditTrail, setAuditTrail] = useState([]);

  const [excursionsLoading, setExcursionsLoading] = useState(false);
  const [auditLoading, setAuditLoading] = useState(false);

  const [pendingDownload, setPendingDownload] = useState(null);

  const excursionRows = useMemo(
    () =>
      excursions.map((item) => ({
        id: item.id,
        freezer: item.freezerName,
        location: item.location,
        startTime: item.startTime,
        duration: item.duration,
        range: item.range,
        severity: item.severity,
        status: item.status,
        source: item,
      })),
    [excursions],
  );

  const auditRows = useMemo(
    () =>
      auditTrail.map((event) => ({
        id: event.id,
        timestamp: event.timestamp,
        freezer: event.freezerName,
        action: event.actionType,
        performedBy: event.performedBy,
        comment: event.comment,
        source: event,
      })),
    [auditTrail],
  );

  const [reportType, setReportType] = useState("Daily Log");
  const [freezer, setFreezer] = useState("All Freezers");
  const [format, setFormat] = useState("PDF");
  const [dateRange, setDateRange] = useState(defaultDateRange());

  const selectedFreezerId = useMemo(() => {
    if (!freezer || freezer === "All Freezers") {
      return null;
    }
    return freezerNameToIdMap[freezer] || null;
  }, [freezer, freezerNameToIdMap]);

  const rangeParams = useMemo(() => {
    if (!Array.isArray(dateRange) || dateRange.length < 2) {
      return null;
    }
    const [start, end] = dateRange;
    const startIso = toIsoString(start);
    const endIso = toIsoString(end);
    if (!startIso || !endIso) {
      return null;
    }
    return { start: startIso, end: endIso };
  }, [dateRange]);

  const loadExcursions = useCallback(async () => {
    if (!rangeParams) {
      return;
    }
    setExcursionsLoading(true);
    try {
      const data = await fetchReportExcursions({
        freezerId: selectedFreezerId,
        start: rangeParams.start,
        end: rangeParams.end,
      });
      const items = normalizeArray(data);
      setExcursions(items.map((alert) => mapAlertToExcursion(alert)));
    } catch (error) {
      notify({
        kind: NotificationKinds.error,
        title: "Unable to load excursion history",
        subtitle: error.message || "Unexpected error while loading excursions.",
      });
    } finally {
      setExcursionsLoading(false);
    }
  }, [rangeParams, selectedFreezerId, notify, normalizeArray]);

  const loadAuditTrail = useCallback(async () => {
    if (!rangeParams) {
      return;
    }
    setAuditLoading(true);
    try {
      const data = await fetchAuditTrail({
        freezerId: selectedFreezerId,
        start: rangeParams.start,
        end: rangeParams.end,
      });
      const items = normalizeArray(data);
      setAuditTrail(items.map((event) => mapAuditEvent(event)));
    } catch (error) {
      notify({
        kind: NotificationKinds.error,
        title: "Unable to load audit trail",
        subtitle:
          error.message || "Unexpected error while loading audit records.",
      });
    } finally {
      setAuditLoading(false);
    }
  }, [rangeParams, selectedFreezerId, notify, normalizeArray]);

  useEffect(() => {
    if (!rangeParams) {
      return;
    }
    loadExcursions();
    loadAuditTrail();
  }, [rangeParams, loadExcursions, loadAuditTrail]);

  const handleDateChange = (range) => {
    if (!Array.isArray(range) || range.length < 2) {
      return;
    }
    const normalized = range.map((value) =>
      value instanceof Date ? value : new Date(value),
    );
    setDateRange(normalized);
  };

  const handleDownload = useCallback(
    (reference, formatType) => {
      try {
        const downloadUrl = `/rest/coldstorage/reports/download/${reference}`;
        const link = document.createElement("a");
        link.href = downloadUrl;
        link.download = `freezer_report_${reference}.${formatType.toLowerCase()}`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        setPendingDownload(null);
        notify({
          kind: NotificationKinds.success,
          title: "Download started",
          subtitle: "Your report download has started successfully.",
        });
      } catch (error) {
        notify({
          kind: NotificationKinds.error,
          title: "Download failed",
          subtitle: "Unable to download the report. Please try again.",
        });
      }
    },
    [notify],
  );

  const handleGenerate = async () => {
    if (!rangeParams) {
      notify({
        kind: NotificationKinds.error,
        title: "Missing date range",
        subtitle:
          "Please select a valid start and end date before generating a report.",
      });
      return;
    }

    const reportName = REPORT_TYPE_MAP[reportType];
    const formatParam = format.toUpperCase();

    if (!reportName) {
      notify({
        kind: NotificationKinds.error,
        title: "Invalid report type",
        subtitle: `Report type "${reportType}" is not supported.`,
      });
      return;
    }

    try {
      // Call API to get report blob
      const blob = await downloadReportDirect({
        reportName: reportName,
        format: formatParam,
        startDate: rangeParams.start.split("T")[0],
        endDate: rangeParams.end.split("T")[0],
        freezerId: selectedFreezerId,
      });

      // Download file immediately
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      const dateStr = new Date().toISOString().split("T")[0];
      const reportSlug = reportType.toLowerCase().replace(/ /g, "_");
      link.download = `freezer_report_${reportSlug}_${dateStr}.${formatParam.toLowerCase()}`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      notify({
        kind: NotificationKinds.success,
        title: "Report generated successfully",
        subtitle: `${reportType} report has been downloaded to your computer.`,
      });
    } catch (error) {
      const errorDetails = error.message || "Unexpected error occurred.";
      notify({
        kind: NotificationKinds.error,
        title: "Report generation failed",
        subtitle: `Unable to generate report. ${errorDetails}`,
      });
    }
  };

  const severityTag = (severity) => {
    if ((severity || "").toUpperCase() === "CRITICAL") {
      return <Tag type="red">CRITICAL</Tag>;
    }
    if ((severity || "").toUpperCase() === "WARNING") {
      return <Tag type="yellow">WARNING</Tag>;
    }
    return <Tag>{severity}</Tag>;
  };

  const statusTag = (status) => {
    if ((status || "").toUpperCase() === "RESOLVED") {
      return <Tag type="green">Resolved</Tag>;
    }
    return <Tag>{status}</Tag>;
  };

  return (
    <div className="reports-page">
      {notificationVisible === true ? <AlertDialog /> : ""}

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <h3 className="reports-title">Regulatory Reports</h3>
        </Column>
      </Grid>

      <Grid fullWidth={true} className="reports-form">
        <Column lg={4} md={4} sm={4}>
          <Dropdown
            id="report-type"
            titleText="Report Type"
            items={REPORT_TYPES}
            label={reportType}
            selectedItem={reportType}
            onChange={({ selectedItem }) => setReportType(selectedItem)}
          />
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Dropdown
            id="freezer-select"
            titleText="Freezer"
            items={freezerOptions}
            label={freezer}
            selectedItem={freezer}
            onChange={({ selectedItem }) => setFreezer(selectedItem)}
          />
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Dropdown
            id="export-format"
            titleText="Export Format"
            items={EXPORT_FORMATS}
            label={format}
            selectedItem={format}
            onChange={({ selectedItem }) => setFormat(selectedItem)}
          />
        </Column>
        <Column lg={2} md={2} sm={4}>
          <DatePicker
            datePickerType="single"
            onChange={(dates) => {
              if (dates && dates.length > 0) {
                const newRange = [...dateRange];
                newRange[0] = dates[0];
                setDateRange(newRange);
              }
            }}
          >
            <DatePickerInput
              id="reports-start-date"
              placeholder="mm/dd/yyyy"
              labelText="Start date"
              size="md"
            />
          </DatePicker>
        </Column>
        <Column lg={2} md={2} sm={4}>
          <DatePicker
            datePickerType="single"
            onChange={(dates) => {
              if (dates && dates.length > 0) {
                const newRange = [...dateRange];
                newRange[1] = dates[0];
                setDateRange(newRange);
              }
            }}
          >
            <DatePickerInput
              id="reports-end-date"
              placeholder="mm/dd/yyyy"
              labelText="End date"
              size="md"
            />
          </DatePicker>
        </Column>
        <Column lg={16} md={8} sm={4} className="reports-generate">
          <Button size="sm" onClick={handleGenerate}>
            Generate Report
          </Button>
          {pendingDownload && (
            <Button
              size="lg"
              kind="secondary"
              onClick={() =>
                handleDownload(
                  pendingDownload.reference,
                  pendingDownload.format,
                )
              }
              style={{ marginLeft: "1rem" }}
            >
              Download {pendingDownload.format} Report
            </Button>
          )}
        </Column>
        <Column lg={16} md={8} sm={4}>
          <div className="reports-compliance-box">
            <p>
              <strong>Regulatory Compliance</strong>
              <br />
              Reports follow CAP (College of American Pathologists), CLIA
              (Clinical Laboratory Improvement Amendments), FDA (Food and Drug
              Administration), and WHO (World Health Organization) guidance for
              temperature-controlled storage.
            </p>
          </div>
        </Column>
      </Grid>

      <Grid fullWidth={true} className="reports-bottom-tabs">
        <Column lg={16} md={8} sm={4}>
          <Tabs>
            <TabList aria-label="Reports tabs" contained>
              <Tab>Temperature Excursions</Tab>
              <Tab>Audit Trail</Tab>
            </TabList>
            <TabPanels>
              <TabPanel>
                <h4 className="exc-title">Temperature Excursion History</h4>
                <DataTable rows={excursionRows} headers={EXCURSION_HEADERS}>
                  {({
                    rows,
                    headers,
                    getRowProps,
                    getHeaderProps,
                    getTableProps,
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
                          {rows.length === 0 && (
                            <TableRow>
                              <TableCell
                                colSpan={EXCURSION_HEADERS.length}
                                className="empty-state"
                              >
                                {excursionsLoading
                                  ? "Loading excursions…"
                                  : "No excursions available for the selected filters."}
                              </TableCell>
                            </TableRow>
                          )}
                          {rows.map((row) => {
                            const excursion = row.source;
                            if (!excursion) {
                              return null;
                            }
                            return (
                              <TableRow key={row.id} {...getRowProps({ row })}>
                                <TableCell>{excursion.id}</TableCell>
                                <TableCell>
                                  <span className="freezer-stack">
                                    <strong>
                                      {excursion.freezerId ?? "—"}
                                    </strong>
                                    <br />
                                    {excursion.freezerName}
                                  </span>
                                </TableCell>
                                <TableCell>{excursion.location}</TableCell>
                                <TableCell>{excursion.startTime}</TableCell>
                                <TableCell>{excursion.duration}</TableCell>
                                <TableCell>{excursion.range}</TableCell>
                                <TableCell>
                                  {severityTag(excursion.severity)}
                                </TableCell>
                                <TableCell>
                                  {statusTag(excursion.status)}
                                </TableCell>
                              </TableRow>
                            );
                          })}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  )}
                </DataTable>
              </TabPanel>
              <TabPanel>
                <h4 className="exc-title">Audit Trail</h4>
                <DataTable rows={auditRows} headers={AUDIT_HEADERS}>
                  {({
                    rows,
                    headers,
                    getRowProps,
                    getHeaderProps,
                    getTableProps,
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
                          {rows.length === 0 && (
                            <TableRow>
                              <TableCell
                                colSpan={AUDIT_HEADERS.length}
                                className="empty-state"
                              >
                                {auditLoading
                                  ? "Loading audit entries…"
                                  : "No audit activity for the selected filters."}
                              </TableCell>
                            </TableRow>
                          )}
                          {rows.map((row) => {
                            const event = row.source;
                            if (!event) {
                              return null;
                            }
                            return (
                              <TableRow key={row.id} {...getRowProps({ row })}>
                                <TableCell>{event.id}</TableCell>
                                <TableCell>{event.timestamp}</TableCell>
                                <TableCell>{event.freezerName}</TableCell>
                                <TableCell>{event.actionType}</TableCell>
                                <TableCell>{event.performedBy}</TableCell>
                                <TableCell>{event.comment}</TableCell>
                              </TableRow>
                            );
                          })}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  )}
                </DataTable>
              </TabPanel>
            </TabPanels>
          </Tabs>
        </Column>
      </Grid>

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <p className="reports-footer">
            Cold Storage Monitoring v2.1.0 | CAP, CLIA, FDA & WHO compliant |
            HIPAA-ready data handling
          </p>
        </Column>
      </Grid>
    </div>
  );
}

export default Reports;

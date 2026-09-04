import React, { useState, useEffect, useMemo, useCallback } from "react";
import {
  Grid,
  Column,
  Tag,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Dropdown,
  Pagination,
  Tabs,
  TabList,
  TabPanels,
  Tab,
  TabPanel,
  Loading,
  Button,
  Search,
} from "@carbon/react";
import {
  Document,
  Warning,
  Calendar,
  Time,
  Download,
} from "@carbon/icons-react";
import { LineChart } from "@carbon/charts-react";
import "@carbon/charts/styles.css";
import "./DeviceHistoryExpansion.scss";
import {
  fetchCorrectiveActions,
  fetchFilteredAlerts,
  fetchHistoricalReadings,
} from "./api";
import { toDate } from "./shared/dateUtils";
import { FormattedMessage, useIntl } from "react-intl";

const EVENT_TYPE_OPTIONS = [
  { id: "all", label: "All Events" },
  { id: "alert", label: "Alerts Only" },
  { id: "corrective-action", label: "Corrective Actions Only" },
];

const TIME_RANGE_OPTIONS = [
  { id: "24h", label: "Last 24 Hours" },
  { id: "7d", label: "Last 7 Days" },
  { id: "30d", label: "Last 30 Days" },
  { id: "all", label: "All Time" },
];

const METRIC_OPTIONS = [
  {
    id: "temperature",
    label: "Temperature",
    field: "temperatureCelsius",
    unit: "°C",
  },
  { id: "humidity", label: "Humidity", field: "humidityPercentage", unit: "%" },
  {
    id: "temperature2",
    label: "Temperature (Probe 2)",
    field: "temperatureCelsius2",
    unit: "°C",
  },
];

const RANGE_TO_DURATION = {
  "24h": 24 * 60 * 60 * 1000,
  "7d": 7 * 24 * 60 * 60 * 1000,
  "30d": 30 * 24 * 60 * 60 * 1000,
  all: 90 * 24 * 60 * 60 * 1000,
};

function DeviceHistoryExpansion({ device }) {
  const intl = useIntl();
  const [activeTab, setActiveTab] = useState(0);
  const [loading, setLoading] = useState(true);
  const [correctiveActions, setCorrectiveActions] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [eventFilter, setEventFilter] = useState("all");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);

  const [timeRange, setTimeRange] = useState("24h");
  const [selectedMetric, setSelectedMetric] = useState("temperature");
  const [chartData, setChartData] = useState([]);
  const [trendsLoading, setTrendsLoading] = useState(false);
  const [trendsError, setTrendsError] = useState(null);

  // fetchCorrectiveActions/fetchFilteredAlerts (via getFromOpenElisServerV2)
  // don't expose an AbortController signal, so this guard can't cancel the
  // in-flight network call itself - it drops a stale response instead,
  // which is what actually prevents a fast device-row expand/collapse from
  // showing another device's history. The controller is created by the
  // caller (the effect below) and passed in, matching the existing house
  // pattern for load-on-mount effects in this codebase.
  const loadDeviceHistory = useCallback(
    (controller) => {
      if (!device) return;

      const deviceId = device.id || device.freezerId;
      if (!deviceId) return;

      setLoading(true);
      (async () => {
        try {
          const freezerId =
            typeof deviceId === "string" ? parseInt(deviceId, 10) : deviceId;
          const [actionsData, alertsData] = await Promise.all([
            fetchCorrectiveActions({ freezerId: freezerId }),
            fetchFilteredAlerts({
              entityType: "Freezer",
              entityId: freezerId,
              page: 0,
              size: 100,
            }),
          ]);
          if (controller.signal.aborted) {
            return;
          }

          const normalizeArray = (payload) => {
            if (Array.isArray(payload)) return payload;
            if (payload && typeof payload === "object") {
              return (
                payload.items ||
                payload.data ||
                payload.results ||
                payload.content ||
                payload.list ||
                payload.rows ||
                []
              );
            }
            return [];
          };

          const actions = normalizeArray(actionsData);
          const alertsResult = normalizeArray(alertsData);
          const alertsList = Array.isArray(alertsResult)
            ? alertsResult
            : alertsResult?.content || [];

          setCorrectiveActions(actions || []);
          setAlerts(alertsList || []);
        } catch (error) {
          if (controller.signal.aborted) {
            return;
          }
          console.error("Error loading device history:", error);
        } finally {
          if (!controller.signal.aborted) {
            setLoading(false);
          }
        }
      })();
    },
    [device],
  );

  useEffect(() => {
    if (!device) {
      return undefined;
    }
    const controller = new AbortController();
    loadDeviceHistory(controller);
    return () => controller.abort();
  }, [device, loadDeviceHistory]);

  const loadTemperatureReadings = useCallback(
    (controller) => {
      if (!device) return;

      const deviceId = device.id || device.freezerId;
      if (!deviceId) return;

      setTrendsLoading(true);
      setTrendsError(null);

      (async () => {
        try {
          const duration =
            RANGE_TO_DURATION[timeRange] || RANGE_TO_DURATION["24h"];
          const end = new Date();
          const start = new Date(end.getTime() - duration);

          const freezerId =
            typeof deviceId === "string" ? parseInt(deviceId, 10) : deviceId;
          const readings = await fetchHistoricalReadings(
            freezerId,
            start.toISOString(),
            end.toISOString(),
          );
          if (controller.signal.aborted) {
            return;
          }

          const metric = METRIC_OPTIONS.find((m) => m.id === selectedMetric);
          const normalizedReadings = (readings || [])
            .filter((reading) => reading[metric.field] != null)
            .map((reading) => ({
              group: metric.label,
              date: toDate(reading.recordedAt),
              value: reading[metric.field],
            }))
            .filter((reading) => reading.date !== null)
            .sort((a, b) => a.date - b.date);

          setChartData(normalizedReadings);
        } catch (error) {
          if (controller.signal.aborted) {
            return;
          }
          console.error("Error loading readings:", error);
          setTrendsError(error.message || "Unable to load reading data.");
          setChartData([]);
        } finally {
          if (!controller.signal.aborted) {
            setTrendsLoading(false);
          }
        }
      })();
    },
    [device, timeRange, selectedMetric],
  );

  useEffect(() => {
    if (activeTab !== 1 || !device) {
      return undefined;
    }
    const controller = new AbortController();
    loadTemperatureReadings(controller);
    return () => controller.abort();
  }, [activeTab, device, loadTemperatureReadings]);

  const formatDate = (dateValue) => {
    const date = toDate(dateValue);
    if (!date) return "—";
    return date.toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  };

  const formatTime = (dateValue) => {
    const date = toDate(dateValue);
    if (!date) return "—";
    return date.toLocaleTimeString("en-US", {
      hour: "2-digit",
      minute: "2-digit",
      hour12: true,
    });
  };

  const formatTemperature = (value) => {
    if (value == null) return "—";
    return `${value.toFixed(1)}°C`;
  };

  const allEvents = useMemo(() => {
    const events = [];

    alerts.forEach((alert) => {
      events.push({
        id: `ALT-${String(alert.id).padStart(3, "0")}`,
        type: "alert",
        eventId: `ALT-${String(alert.id).padStart(3, "0")}`,
        summary:
          alert.currentTemperature != null
            ? `Temperature ${alert.currentTemperature > alert.maxTemperature ? "exceeded" : "dropped below"} ${alert.severity === "CRITICAL" ? "critical" : "warning"} threshold (${formatTemperature(alert.maxTemperature || alert.minTemperature)})`
            : alert.severity === "CRITICAL"
              ? "Critical temperature excursion detected"
              : "Warning threshold exceeded",
        severity: alert.severity,
        date: alert.startTime,
        time: alert.startTime,
        acknowledgedBy: alert.actions?.[0]?.performedBy || "—",
        rawData: alert,
      });
    });

    correctiveActions.forEach((action) => {
      events.push({
        id: `CA-${String(action.id).padStart(3, "0")}`,
        type: "corrective-action",
        eventId: `CA-${String(action.id).padStart(3, "0")}`,
        summary: action.description || "No description provided",
        severity: null,
        status: action.status || "PENDING",
        isEdited: action.isEdited || false,
        date: action.createdAt,
        time: action.createdAt,
        acknowledgedBy: action.createdByName || "—",
        rawData: action,
      });
    });

    return events.sort((a, b) => {
      const dateA = toDate(a.date) ?? new Date(0);
      const dateB = toDate(b.date) ?? new Date(0);
      return dateB - dateA;
    });
  }, [alerts, correctiveActions]);

  const filteredEvents = useMemo(() => {
    let filtered = allEvents;

    if (eventFilter === "alert") {
      filtered = filtered.filter((e) => e.type === "alert");
    } else if (eventFilter === "corrective-action") {
      filtered = filtered.filter((e) => e.type === "corrective-action");
    }

    if (searchTerm) {
      const lc = searchTerm.toLowerCase();
      filtered = filtered.filter(
        (e) =>
          e.eventId.toLowerCase().includes(lc) ||
          e.summary.toLowerCase().includes(lc) ||
          e.acknowledgedBy.toLowerCase().includes(lc),
      );
    }

    return filtered;
  }, [allEvents, eventFilter, searchTerm]);

  const paginatedEvents = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    const end = start + pageSize;
    return filteredEvents.slice(start, end);
  }, [filteredEvents, currentPage, pageSize]);

  const totalEvents = allEvents.length;
  const totalAlerts = alerts.length;
  const totalCorrectiveActions = correctiveActions.length;

  const temperatureStats = useMemo(() => {
    if (!chartData.length) {
      return { avg: "—", min: "—", max: "—", count: 0 };
    }
    const values = chartData.map((d) => d.value);
    const sum = values.reduce((acc, v) => acc + v, 0);
    const avg = sum / values.length;
    const min = Math.min(...values);
    const max = Math.max(...values);
    return {
      avg: avg.toFixed(1),
      min: min.toFixed(1),
      max: max.toFixed(1),
      count: values.length,
    };
  }, [chartData]);

  const selectedMetricOption = useMemo(
    () => METRIC_OPTIONS.find((m) => m.id === selectedMetric),
    [selectedMetric],
  );

  const formattedChartData = useMemo(() => {
    return chartData.map((point) => ({
      group: point.group,
      key: point.date.toLocaleString([], {
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      }),
      value: point.value,
    }));
  }, [chartData]);

  const deviceThresholds = useMemo(() => {
    const minTemp = device?.minTemperature || device?.thresholdMin || -20;
    const maxTemp = device?.maxTemperature || device?.thresholdMax || -18;
    return { minTemp, maxTemp };
  }, [device]);

  const chartOptions = useMemo(() => {
    const { minTemp, maxTemp } = deviceThresholds;
    const metric = METRIC_OPTIONS.find((m) => m.id === selectedMetric);

    return {
      title: "",
      axes: {
        bottom: {
          title: "",
          mapsTo: "key",
          scaleType: "labels",
        },
        left: {
          title: `${metric.label} (${metric.unit})`,
          mapsTo: "value",
          scaleType: "linear",
        },
      },
      legend: {
        enabled: false,
      },
      height: "400px",
      tooltip: {
        showTotal: false,
      },
      // Threshold lines are temperature-specific (device min/max in °C) - only
      // meaningful when that's the metric being charted.
      ...(selectedMetric === "temperature" && {
        thresholds: [
          {
            value: maxTemp,
            label: "Warning",
            fillColor: "#FF832B",
          },
          {
            value: minTemp,
            label: "Alert",
            fillColor: "#DA1E28",
          },
        ],
      }),
    };
  }, [deviceThresholds, selectedMetric]);

  const handleExportCsv = () => {
    if (!chartData.length) return;

    const metric = METRIC_OPTIONS.find((m) => m.id === selectedMetric);
    const csvHeader = `Timestamp,${metric.label} (${metric.unit})\n`;
    const csvRows = chartData
      .map((point) => `${point.date.toISOString()},${point.value}`)
      .join("\n");
    const csvContent = csvHeader + csvRows;

    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const link = document.createElement("a");
    const url = URL.createObjectURL(blob);
    link.setAttribute("href", url);
    link.setAttribute(
      "download",
      `temperature-history-${device?.id || device?.freezerId}-${new Date().toISOString()}.csv`,
    );
    link.style.visibility = "hidden";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const severityTag = (severity) => {
    if (!severity) return null;
    switch (severity) {
      case "WARNING":
        return <Tag type="yellow">WARNING</Tag>;
      case "CRITICAL":
        return <Tag type="red">CRITICAL</Tag>;
      default:
        return <Tag>{severity}</Tag>;
    }
  };

  const statusTag = (status, isEdited = false) => {
    const tag = (() => {
      switch (status) {
        case "PENDING":
          return <Tag type="red">Pending</Tag>;
        case "IN_PROGRESS":
          return <Tag type="blue">In Progress</Tag>;
        case "COMPLETED":
          return <Tag type="green">Completed</Tag>;
        case "CANCELLED":
          return <Tag type="gray">Cancelled</Tag>;
        case "RETRACTED":
          return <Tag type="magenta">Retracted</Tag>;
        default:
          return <Tag>{status}</Tag>;
      }
    })();

    return (
      <div className="oe-deviceHistory-inlineTagRow">
        {tag}
        {isEdited && (
          <Tag type="purple" size="sm">
            Edited
          </Tag>
        )}
      </div>
    );
  };

  const eventColumns = [
    {
      key: "eventType",
      header: intl.formatMessage({
        id: "coldStorage.deviceHistory.column.eventType",
        defaultMessage: "Event Type",
      }),
    },
    {
      key: "eventId",
      header: intl.formatMessage({
        id: "coldStorage.deviceHistory.column.eventId",
        defaultMessage: "Event ID",
      }),
    },
    {
      key: "summary",
      header: intl.formatMessage({
        id: "coldStorage.deviceHistory.column.summaryTitle",
        defaultMessage: "Summary / Title",
      }),
    },
    {
      key: "severity",
      header: intl.formatMessage({
        id: "coldStorage.dashboard.column.severity",
        defaultMessage: "Severity",
      }),
    },
    {
      key: "date",
      header: intl.formatMessage({
        id: "coldStorage.deviceHistory.column.date",
        defaultMessage: "Date",
      }),
    },
    {
      key: "time",
      header: intl.formatMessage({
        id: "coldStorage.deviceHistory.column.time",
        defaultMessage: "Time",
      }),
    },
    {
      key: "performedBy",
      header: intl.formatMessage({
        id: "coldStorage.deviceHistory.column.acknowledgedPerformedBy",
        defaultMessage: "Acknowledged / Performed By",
      }),
    },
  ];

  const eventRows = paginatedEvents.map((event) => {
    const isAlert = event.type === "alert";

    return {
      id: event.id,
      eventType: (
        <div className="oe-deviceHistory-inlineTagRow">
          {isAlert ? <Warning size={16} /> : <Document size={16} />}
          <Tag type={isAlert ? "red" : "blue"} size="sm">
            {isAlert ? "Alert" : "Corrective Action"}
          </Tag>
        </div>
      ),
      eventId: event.eventId,
      summary: event.summary,
      severity: isAlert
        ? severityTag(event.severity)
        : statusTag(event.status, event.isEdited),
      date: formatDate(event.date),
      time: formatTime(event.time),
      performedBy: event.acknowledgedBy,
    };
  });

  const deviceDisplayName =
    device?.unitName ||
    device?.name ||
    device?.freezerName ||
    `Freezer ${device?.id || device?.freezerId}`;
  const deviceId = device?.id || device?.freezerId;

  return (
    <div className="oe-deviceHistory">
      <h3 className="oe-deviceHistory-title">
        <FormattedMessage
          id="coldStorage.deviceHistory.title"
          defaultMessage="Device History - {name} ({id})"
          values={{ name: deviceDisplayName, id: deviceId }}
        />
      </h3>

      {/* Summary Cards */}
      <Grid condensed className="oe-deviceHistory-summaryGrid">
        <Column
          lg={4}
          md={4}
          sm={4}
          className="oe-deviceHistory-summaryColumn--left"
        >
          <div className="oe-deviceHistory-summaryCard oe-deviceHistory-summaryCard--events">
            <div className="oe-deviceHistory-summaryLabel">
              <FormattedMessage
                id="coldStorage.deviceHistory.totalEvents"
                defaultMessage="Total Events"
              />
            </div>
            <div className="oe-deviceHistory-summaryValue">{totalEvents}</div>
          </div>
        </Column>
        <Column
          lg={4}
          md={4}
          sm={4}
          className="oe-deviceHistory-summaryColumn--middle"
        >
          <div className="oe-deviceHistory-summaryCard oe-deviceHistory-summaryCard--alerts">
            <div className="oe-deviceHistory-summaryLabel">
              <FormattedMessage
                id="coldStorage.deviceHistory.totalAlerts"
                defaultMessage="Total Alerts"
              />
            </div>
            <div className="oe-deviceHistory-summaryValue">{totalAlerts}</div>
          </div>
        </Column>
        <Column
          lg={4}
          md={4}
          sm={4}
          className="oe-deviceHistory-summaryColumn--right"
        >
          <div className="oe-deviceHistory-summaryCard oe-deviceHistory-summaryCard--actions">
            <div className="oe-deviceHistory-summaryLabel">
              <FormattedMessage
                id="coldStorage.deviceHistory.correctiveActions"
                defaultMessage="Corrective Actions"
              />
            </div>
            <div className="oe-deviceHistory-summaryValue">
              {totalCorrectiveActions}
            </div>
          </div>
        </Column>
      </Grid>

      {/* Tabs */}
      <Tabs
        selectedIndex={activeTab}
        onChange={({ selectedIndex }) => setActiveTab(selectedIndex)}
      >
        <TabList aria-label="Device history sections" contained>
          <Tab renderIcon={Calendar}>
            <FormattedMessage
              id="coldStorage.deviceHistory.tab.eventHistory"
              defaultMessage="Event History"
            />
          </Tab>
          <Tab renderIcon={Time}>
            <FormattedMessage
              id="coldStorage.deviceHistory.tab.temperatureTrends"
              defaultMessage="Temperature Trends"
            />
          </Tab>
        </TabList>
        <TabPanels>
          {/* Event History Tab */}
          <TabPanel>
            {loading ? (
              <Loading
                description={intl.formatMessage({
                  id: "coldStorage.deviceHistory.loadingEvents",
                  defaultMessage: "Loading event history...",
                })}
              />
            ) : (
              <>
                {/* Search and Filters Section */}
                <Grid fullWidth className="oe-deviceHistory-filterGrid">
                  {/* Search - full width */}
                  <Column lg={16} md={8} sm={4}>
                    <Search
                      labelText={intl.formatMessage({
                        id: "coldStorage.deviceHistory.searchEvents",
                        defaultMessage: "Search events",
                      })}
                      placeholder={intl.formatMessage({
                        id: "coldStorage.deviceHistory.searchPlaceholder",
                        defaultMessage:
                          "Search by event ID, summary, or user...",
                      })}
                      value={searchTerm}
                      onChange={(e) => {
                        setSearchTerm(e.target.value);
                        setCurrentPage(1);
                      }}
                      size="lg"
                    />
                  </Column>
                  {/* Filter Dropdown */}
                  <Column
                    lg={6}
                    md={4}
                    sm={4}
                    className="oe-deviceHistory-filterColumn"
                  >
                    <Dropdown
                      id="event-filter"
                      titleText="Event Type"
                      label={
                        EVENT_TYPE_OPTIONS.find((opt) => opt.id === eventFilter)
                          ?.label || "All Events"
                      }
                      items={EVENT_TYPE_OPTIONS}
                      itemToString={(item) => (item ? item.label : "")}
                      selectedItem={EVENT_TYPE_OPTIONS.find(
                        (opt) => opt.id === eventFilter,
                      )}
                      onChange={({ selectedItem }) => {
                        setEventFilter(selectedItem?.id || "all");
                        setCurrentPage(1);
                      }}
                      size="md"
                    />
                  </Column>
                </Grid>

                <DataTable rows={eventRows} headers={eventColumns}>
                  {({
                    rows,
                    headers,
                    getHeaderProps,
                    getRowProps,
                    getTableProps,
                    getTableContainerProps,
                  }) => (
                    <TableContainer
                      title={intl.formatMessage({
                        id: "coldStorage.deviceHistory.tab.eventHistory",
                        defaultMessage: "Event History",
                      })}
                      {...getTableContainerProps()}
                    >
                      <Table {...getTableProps()} size="md">
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
                              <TableCell colSpan={eventColumns.length}>
                                <FormattedMessage
                                  id="coldStorage.deviceHistory.noEventsFound"
                                  defaultMessage="No events found."
                                />
                              </TableCell>
                            </TableRow>
                          )}
                          {rows.map((row) => (
                            <TableRow key={row.id} {...getRowProps({ row })}>
                              {row.cells.map((cell) => (
                                <TableCell
                                  key={cell.id}
                                  className="oe-deviceHistory-tableCell"
                                >
                                  {cell.value}
                                </TableCell>
                              ))}
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                      <Pagination
                        backwardText={intl.formatMessage({
                          id: "pagination.previousPage",
                          defaultMessage: "Previous page",
                        })}
                        forwardText={intl.formatMessage({
                          id: "pagination.nextPage",
                          defaultMessage: "Next page",
                        })}
                        itemsPerPageText={intl.formatMessage({
                          id: "pagination.itemsPerPage",
                          defaultMessage: "Items per page:",
                        })}
                        page={currentPage}
                        pageSize={pageSize}
                        pageSizes={[5, 10, 20, 50]}
                        totalItems={filteredEvents.length}
                        onChange={({ page, pageSize }) => {
                          setCurrentPage(page);
                          setPageSize(pageSize);
                        }}
                      />
                    </TableContainer>
                  )}
                </DataTable>
              </>
            )}
          </TabPanel>

          {/* Temperature Trends Tab */}
          <TabPanel>
            <div>
              {/* Time Range and Export Controls */}
              <Grid className="oe-deviceHistory-controlsGrid">
                <Column lg={5} md={4} sm={4}>
                  <div className="oe-deviceHistory-timeRangeControl">
                    <span className="oe-deviceHistory-timeRangeLabel">
                      <FormattedMessage
                        id="coldStorage.deviceHistory.timeRange"
                        defaultMessage="Time Range:"
                      />
                    </span>
                    <Dropdown
                      id="time-range-dropdown"
                      titleText=""
                      label={
                        TIME_RANGE_OPTIONS.find((opt) => opt.id === timeRange)
                          ?.label || "Last 24 Hours"
                      }
                      items={TIME_RANGE_OPTIONS}
                      itemToString={(item) => (item ? item.label : "")}
                      selectedItem={TIME_RANGE_OPTIONS.find(
                        (opt) => opt.id === timeRange,
                      )}
                      onChange={({ selectedItem }) =>
                        setTimeRange(selectedItem?.id || "24h")
                      }
                      size="md"
                    />
                  </div>
                </Column>
                <Column lg={5} md={4} sm={4}>
                  <div className="oe-deviceHistory-timeRangeControl">
                    <span className="oe-deviceHistory-timeRangeLabel">
                      <FormattedMessage
                        id="coldStorage.deviceHistory.metric"
                        defaultMessage="Metric:"
                      />
                    </span>
                    <Dropdown
                      id="metric-dropdown"
                      titleText=""
                      label={
                        METRIC_OPTIONS.find((opt) => opt.id === selectedMetric)
                          ?.label || "Temperature"
                      }
                      items={METRIC_OPTIONS}
                      itemToString={(item) => (item ? item.label : "")}
                      selectedItem={METRIC_OPTIONS.find(
                        (opt) => opt.id === selectedMetric,
                      )}
                      onChange={({ selectedItem }) =>
                        setSelectedMetric(selectedItem?.id || "temperature")
                      }
                      size="md"
                    />
                  </div>
                </Column>
                <Column
                  lg={6}
                  md={4}
                  sm={4}
                  className="oe-deviceHistory-exportColumn"
                >
                  <Button
                    kind="tertiary"
                    size="md"
                    renderIcon={Download}
                    onClick={handleExportCsv}
                    disabled={!chartData.length}
                  >
                    <FormattedMessage
                      id="coldStorage.deviceHistory.exportCsv"
                      defaultMessage="Export CSV"
                    />
                  </Button>
                </Column>
              </Grid>

              {/* Metrics Cards */}
              <Grid className="oe-deviceHistory-metricsGrid">
                <Column lg={4} md={4} sm={4}>
                  <div className="oe-deviceHistory-metricCard">
                    <div className="oe-deviceHistory-metricLabel">
                      <FormattedMessage
                        id="coldStorage.deviceHistory.averageMetric"
                        defaultMessage="Average {metric}"
                        values={{ metric: selectedMetricOption.label }}
                      />
                    </div>
                    <div className="oe-deviceHistory-metricValue">
                      {temperatureStats.avg === "—"
                        ? "—"
                        : `${temperatureStats.avg}${selectedMetricOption.unit}`}
                    </div>
                  </div>
                </Column>
                <Column lg={4} md={4} sm={4}>
                  <div className="oe-deviceHistory-metricCard">
                    <div className="oe-deviceHistory-metricLabel">
                      <FormattedMessage
                        id="coldStorage.deviceHistory.minMetric"
                        defaultMessage="Min {metric}"
                        values={{ metric: selectedMetricOption.label }}
                      />
                    </div>
                    <div className="oe-deviceHistory-metricValue oe-deviceHistory-metricValue--min">
                      {temperatureStats.min === "—"
                        ? "—"
                        : `${temperatureStats.min}${selectedMetricOption.unit}`}
                    </div>
                  </div>
                </Column>
                <Column lg={4} md={4} sm={4}>
                  <div className="oe-deviceHistory-metricCard">
                    <div className="oe-deviceHistory-metricLabel">
                      <FormattedMessage
                        id="coldStorage.deviceHistory.maxMetric"
                        defaultMessage="Max {metric}"
                        values={{ metric: selectedMetricOption.label }}
                      />
                    </div>
                    <div className="oe-deviceHistory-metricValue oe-deviceHistory-metricValue--max">
                      {temperatureStats.max === "—"
                        ? "—"
                        : `${temperatureStats.max}${selectedMetricOption.unit}`}
                    </div>
                  </div>
                </Column>
                <Column lg={4} md={4} sm={4}>
                  <div className="oe-deviceHistory-metricCard">
                    <div className="oe-deviceHistory-metricLabel">
                      <FormattedMessage
                        id="coldStorage.deviceHistory.dataPoints"
                        defaultMessage="Data Points"
                      />
                    </div>
                    <div className="oe-deviceHistory-metricValue">
                      {temperatureStats.count.toLocaleString()}
                    </div>
                  </div>
                </Column>
              </Grid>

              {/* Temperature Chart */}
              <div className="oe-deviceHistory-chartCard">
                {trendsLoading ? (
                  <Loading
                    description={intl.formatMessage({
                      id: "coldStorage.deviceHistory.loadingTemperature",
                      defaultMessage: "Loading temperature data...",
                    })}
                  />
                ) : trendsError ? (
                  <div className="oe-deviceHistory-chartMessage oe-deviceHistory-chartMessage--error">
                    {trendsError}
                  </div>
                ) : formattedChartData.length === 0 ? (
                  <div className="oe-deviceHistory-chartMessage">
                    <FormattedMessage
                      id="coldStorage.deviceHistory.noTemperatureData"
                      defaultMessage="No temperature data available for the selected time range."
                    />
                  </div>
                ) : (
                  <LineChart data={formattedChartData} options={chartOptions} />
                )}
              </div>
            </div>
          </TabPanel>
        </TabPanels>
      </Tabs>
    </div>
  );
}

export default DeviceHistoryExpansion;

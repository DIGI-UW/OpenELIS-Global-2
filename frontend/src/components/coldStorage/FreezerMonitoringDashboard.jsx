import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  useContext,
} from "react";
import {
  Grid,
  Column,
  InlineNotification,
  Tabs,
  TabList,
  TabPanels,
  Tab,
  TabPanel,
  Tag,
  Button,
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
  Search,
  Dropdown,
  Section,
  Heading,
  Form,
  Tile,
  Pagination,
} from "@carbon/react";
import { View, TrashCan } from "@carbon/icons-react";
import "./FreezerMonitoringDashboard.scss";
import CorrectiveActions from "./CorrectiveActions";
import HistoricalTrends from "./HistoricalTrends";
import Reports from "./Reports";
import Settings from "./Settings";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { injectIntl, FormattedMessage } from "react-intl";
import {
  fetchFreezerStatus,
  fetchOpenAlerts,
  acknowledgeAlert,
  resolveAlert,
  deleteAlert,
} from "./api";
import AlertDetailModal from "./AlertDetailModal";
import DeviceHistoryExpansion from "./DeviceHistoryExpansion";
import { toDate, formatDuration } from "./shared/timeUtils";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { NotificationContext } from "../layout/Layout";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { hasRole, Roles } from "../utils/Utils";

// Dashboard auto-refresh interval. The backend default Modbus poll cycle is
// 5 minutes; refreshing every 60s is meaningfully fresher than "never" while
// staying well under the poll cadence so we don't hammer the backend.
const REFRESH_INTERVAL_MS = 60 * 1000;

// Fallback staleness threshold used when no poll-interval configuration is
// available to this dashboard (no config endpoint is currently fetched
// here). 15 minutes is a conservative multiple of the 5 minute default poll
// cycle; devices configured with a longer poll interval should have their
// own threshold surfaced via Settings > Temperature Thresholds in future.
const DEFAULT_STALE_THRESHOLD_MS = 15 * 60 * 1000;

const getColumns = (intl) => [
  {
    key: "id",
    header: intl.formatMessage({
      id: "coldStorage.dashboard.column.unitId",
      defaultMessage: "Unit ID",
    }),
  },
  {
    key: "status",
    header: intl.formatMessage({
      id: "coldStorage.status",
      defaultMessage: "Status",
    }),
  },
  {
    key: "unitName",
    header: intl.formatMessage({
      id: "coldStorage.dashboard.column.unitName",
      defaultMessage: "Unit Name",
    }),
  },
  {
    key: "deviceType",
    header: intl.formatMessage({
      id: "coldStorage.device.type",
      defaultMessage: "Device Type",
    }),
  },
  {
    key: "location",
    header: intl.formatMessage({
      id: "coldStorage.dashboard.column.location",
      defaultMessage: "Location",
    }),
  },
  {
    key: "currentTemp",
    header: intl.formatMessage({
      id: "coldStorage.dashboard.column.currentTemp",
      defaultMessage: "Current Temp",
    }),
  },
  {
    key: "targetTemp",
    header: intl.formatMessage({
      id: "coldStorage.dashboard.column.targetTemp",
      defaultMessage: "Target Temp",
    }),
  },
  {
    key: "currentHumidity",
    header: intl.formatMessage({
      id: "coldStorage.dashboard.column.humidity",
      defaultMessage: "Humidity",
    }),
  },
  {
    key: "currentTemp2",
    header: intl.formatMessage({
      id: "coldStorage.dashboard.column.currentTemp2",
      defaultMessage: "Probe 2 Temp",
    }),
  },
  {
    key: "protocol",
    header: intl.formatMessage({
      id: "coldStorage.device.protocol",
      defaultMessage: "Protocol",
    }),
  },
  {
    key: "lastReading",
    header: intl.formatMessage({
      id: "coldStorage.dashboard.column.lastReading",
      defaultMessage: "Last Reading",
    }),
  },
];

function statusTag(status) {
  switch (status) {
    case "NORMAL":
      return <Tag type="green">Normal</Tag>;
    case "WARNING":
      return (
        <Tag type="warm-gray" className="oe-coldStorage-tag--warning">
          Warning
        </Tag>
      );
    case "CRITICAL":
      return <Tag type="red">Critical</Tag>;
    case null:
    case undefined:
      return (
        <Tag type="cool-gray">
          <FormattedMessage
            id="coldStorage.status.noData"
            defaultMessage="No data"
          />
        </Tag>
      );
    default:
      return <Tag>{status}</Tag>;
  }
}

// A device can simultaneously have a last-known status of Normal/Warning/
// Critical AND be stale/offline (dead-man's-switch) - these are independent
// facts and both must be visible, so staleness gets its own tag rendered
// alongside statusTag() rather than replacing it.
function stalenessTag(lastReading, thresholdMs = DEFAULT_STALE_THRESHOLD_MS) {
  const readingDate = toDate(lastReading);
  if (!readingDate) {
    return (
      <Tag type="cool-gray">
        <FormattedMessage
          id="coldStorage.status.unknown"
          defaultMessage="Unknown"
        />
      </Tag>
    );
  }
  const ageMs = Date.now() - readingDate.getTime();
  if (ageMs > thresholdMs) {
    return (
      <Tag type="gray">
        <FormattedMessage
          id="coldStorage.status.offline"
          defaultMessage="Offline"
        />
      </Tag>
    );
  }
  return null;
}

function temperatureColor(value, target) {
  if (value == null || target == null) {
    return "oe-coldStorage-temp--ok";
  }
  if (value > target) {
    return "oe-coldStorage-temp--high";
  }
  return "oe-coldStorage-temp--ok";
}

const breadcrumbs = [
  { label: "home.label", link: "/", defaultMessage: "Home" },
  {
    label: "coldstorage.label.dashboard",
    link: "/FreezerMonitoring",
    defaultMessage: "Cold Storage Monitoring",
  },
];

const STATUS_OPTIONS = ["All Status", "NORMAL", "WARNING", "CRITICAL"];
const DEFAULT_DEVICE_TYPE = "Cold Storage Unit";

const toNumber = (value) => {
  if (value === null || value === undefined || value === "") {
    return null;
  }
  const parsed = Number(value);
  return Number.isNaN(parsed) ? null : parsed;
};

const formatDateTime = (value) => {
  const date = toDate(value);
  return date ? date.toLocaleString() : "—";
};

const normalizeUnit = (unit) => ({
  id: unit.freezerId?.toString() ?? unit.freezerName ?? "UNKNOWN",
  // status is now returned as null (not fabricated as "NORMAL") for a
  // device that has never recorded a reading - keep that null distinct so
  // it renders as its own "No data" tag instead of a false-green Normal.
  status: unit.status ?? null,
  unitName: unit.freezerName ?? unit.freezerId ?? "Unnamed Freezer",
  deviceType: unit.deviceType ?? DEFAULT_DEVICE_TYPE,
  location: unit.locationName ?? "Unknown location",
  currentTemp: toNumber(unit.temperatureCelsius),
  targetTemp: toNumber(
    unit.targetTemperatureCelsius ?? unit.temperatureCelsius,
  ),
  currentHumidity: toNumber(unit.humidityPercentage),
  currentTemp2: toNumber(unit.temperatureCelsius2),
  protocol: unit.protocol ?? "Unknown",
  lastReading: unit.recordedAt,
});

const normalizeAlert = (alert) => {
  let contextData = {};
  try {
    contextData = alert.contextData ? JSON.parse(alert.contextData) : {};
  } catch (e) {
    console.warn("Failed to parse alert contextData:", alert.contextData);
  }

  const currentTemp = toNumber(contextData.temperature);

  let durationSeconds = null;
  const startTime = toDate(alert.startTime);
  if (startTime) {
    durationSeconds = Math.floor((Date.now() - startTime.getTime()) / 1000);
  }

  return {
    id: alert.id,
    severity: alert.severity ?? "WARNING",
    status: alert.status ?? "OPEN",
    unitName: alert.freezer?.name ?? `Freezer ${alert.alertEntityId}`,
    location: alert.freezer?.code ?? "Unknown location",
    currentTemp,
    durationSeconds,
    startedAt: alert.startTime,
  };
};

const formatTemperatureDisplay = (value) =>
  value == null ? "—" : `${value.toFixed(1)}°C`;

const formatHumidityDisplay = (value) =>
  value == null ? "—" : `${value.toFixed(1)}%`;

function FreezerMonitoringDashboard({ intl }) {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const currentUserId = userSessionDetails?.userId;
  // Alert deletion is ADMIN-only server-side (Roles.GLOBAL_ADMIN maps to the
  // backend's ADMIN role) - hide the control for non-admins rather than
  // showing it and relying solely on a 403 toast.
  const isAdminUser = hasRole(userSessionDetails, Roles.GLOBAL_ADMIN);
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
  const [statusFilter, setStatusFilter] = useState("All Status");
  const [deviceFilter, setDeviceFilter] = useState("All Device Types");
  const [searchTerm, setSearchTerm] = useState("");
  const [isMobile, setIsMobile] = useState(window.innerWidth < 720);
  const [storageUnits, setStorageUnits] = useState([]);
  const [activeAlerts, setActiveAlerts] = useState([]);
  const [dashboardLoading, setDashboardLoading] = useState(false);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [actionInFlight, setActionInFlight] = useState(null);
  const [selectedAlertId, setSelectedAlertId] = useState(null);
  const [showAlertDetail, setShowAlertDetail] = useState(false);
  const [selectedTabIndex, setSelectedTabIndex] = useState(0);
  const [preselectedFreezerId, setPreselectedFreezerId] = useState(null);
  const [expandedRowIds, setExpandedRowIds] = useState({});
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);
  const [alertsCurrentPage, setAlertsCurrentPage] = useState(1);
  const [alertsPageSize, setAlertsPageSize] = useState(5);
  // Guards against overlapping refresh requests (e.g. the 60s poll firing
  // again before a slow previous request has resolved).
  const isFetchingRef = useRef(false);

  const handleRowExpand = useCallback((rowId) => {
    const rowIdStr = String(rowId || "");
    setExpandedRowIds((prevExpanded) => ({
      ...prevExpanded,
      [rowIdStr]: !prevExpanded[rowIdStr],
    }));
  }, []);

  const columns = useMemo(() => getColumns(intl), [intl]);

  const deviceOptions = useMemo(() => {
    const unique = Array.from(
      new Set(
        storageUnits.map((unit) => unit.deviceType || DEFAULT_DEVICE_TYPE),
      ),
    );
    return ["All Device Types", ...unique];
  }, [storageUnits]);

  const filteredUnits = useMemo(() => {
    return storageUnits.filter((unit) => {
      if (statusFilter !== "All Status" && unit.status !== statusFilter) {
        return false;
      }
      if (
        deviceFilter !== "All Device Types" &&
        unit.deviceType !== deviceFilter
      ) {
        return false;
      }
      if (!searchTerm) return true;
      const lc = searchTerm.toLowerCase();
      return (
        unit.id.toLowerCase().includes(lc) ||
        unit.unitName.toLowerCase().includes(lc)
      );
    });
  }, [statusFilter, deviceFilter, searchTerm, storageUnits]);

  const paginatedUnits = useMemo(() => {
    const startIndex = (currentPage - 1) * pageSize;
    const endIndex = startIndex + pageSize;
    return filteredUnits.slice(startIndex, endIndex);
  }, [filteredUnits, currentPage, pageSize]);

  const paginatedAlerts = useMemo(() => {
    const startIndex = (alertsCurrentPage - 1) * alertsPageSize;
    const endIndex = startIndex + alertsPageSize;
    return activeAlerts.slice(startIndex, endIndex);
  }, [activeAlerts, alertsCurrentPage, alertsPageSize]);

  const totalUnits = storageUnits.length;
  const normalUnits = storageUnits.filter((u) => u.status === "NORMAL").length;
  const warningUnits = storageUnits.filter(
    (u) => u.status === "WARNING",
  ).length;
  const criticalUnits = storageUnits.filter(
    (u) => u.status === "CRITICAL",
  ).length;

  const loadDashboardData = useCallback(async () => {
    if (isFetchingRef.current) {
      // A previous refresh (manual or polled) is still in flight - skip
      // this call rather than firing an overlapping duplicate request.
      return;
    }
    isFetchingRef.current = true;
    setDashboardLoading(true);
    try {
      const [statusPayload, alertsPayload] = await Promise.all([
        fetchFreezerStatus(),
        fetchOpenAlerts(),
      ]);

      const unitsArray = Array.isArray(statusPayload)
        ? statusPayload
        : statusPayload?.items ||
          statusPayload?.data ||
          statusPayload?.results ||
          [];

      const alertsArray = Array.isArray(alertsPayload)
        ? alertsPayload
        : alertsPayload?.content ||
          alertsPayload?.alerts ||
          alertsPayload?.items ||
          [];

      setStorageUnits(unitsArray.map(normalizeUnit));
      setActiveAlerts(alertsArray.map(normalizeAlert));
      setLastUpdated(new Date().toISOString());
    } catch (error) {
      const isForbidden = error?.status === 403;
      notify({
        kind: NotificationKinds.error,
        title: isForbidden
          ? "Access denied"
          : "Unable to update cold storage data",
        subtitle: isForbidden
          ? "You do not have permission to view cold storage monitoring data."
          : error.message || "Unable to load cold storage monitoring data.",
      });
    } finally {
      setDashboardLoading(false);
      isFetchingRef.current = false;
    }
  }, [notify]);

  useEffect(() => {
    loadDashboardData();
    // Live refresh so a technician who leaves the tab open sees current
    // data instead of a permanent page-load snapshot. loadDashboardData
    // guards against overlapping requests via isFetchingRef.
    const intervalId = setInterval(loadDashboardData, REFRESH_INTERVAL_MS);
    return () => clearInterval(intervalId);
  }, [loadDashboardData]);

  // Reset to first page when filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [statusFilter, deviceFilter, searchTerm]);

  const handleAlertAction = useCallback(
    async (alertId, action) => {
      if (!currentUserId) {
        notify({
          kind: NotificationKinds.error,
          title: "Unable to identify current user",
          subtitle: "Please sign in again before actioning alerts.",
        });
        return;
      }
      setActionInFlight(alertId);
      try {
        if (action === "acknowledge") {
          await acknowledgeAlert(
            alertId,
            currentUserId,
            "Acknowledged via Cold Storage dashboard",
          );
        } else if (action === "resolve") {
          await resolveAlert(
            alertId,
            currentUserId,
            "Resolved via Cold Storage dashboard",
          );
        } else if (action === "delete") {
          await deleteAlert(alertId);
        }
        await loadDashboardData();
        notify({
          kind: NotificationKinds.success,
          title: "Success",
          subtitle:
            action === "acknowledge"
              ? "Alert acknowledged successfully"
              : action === "resolve"
                ? "Alert resolved successfully"
                : "Alert deleted successfully",
        });
      } catch (error) {
        const isForbidden = error?.status === 403;
        notify({
          kind: NotificationKinds.error,
          title: isForbidden ? "Access denied" : "Error",
          subtitle: isForbidden
            ? `You do not have permission to ${action} this alert.`
            : error.message || `Unable to ${action} alert ${alertId}`,
        });
      } finally {
        setActionInFlight(null);
      }
    },
    [loadDashboardData, notify, currentUserId],
  );

  const handleAcknowledgeAlert = useCallback(
    (alertId) => handleAlertAction(alertId, "acknowledge"),
    [handleAlertAction],
  );

  const handleResolveAlert = useCallback(
    (alertId) => handleAlertAction(alertId, "resolve"),
    [handleAlertAction],
  );

  const handleDeleteAlert = useCallback(
    (alertId) => handleAlertAction(alertId, "delete"),
    [handleAlertAction],
  );

  const lastUpdateLabel = formatDateTime(lastUpdated);

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth < 720);
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  const handleAlertRowClick = useCallback((alertId) => {
    setSelectedAlertId(alertId);
    setShowAlertDetail(true);
  }, []);

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          {notificationVisible === true ? <AlertDialog /> : ""}
          <Section>
            <Section>
              <Heading>
                {intl.formatMessage({
                  id: "coldstorage.label.dashboard",
                  defaultMessage: "Cold Storage Monitoring",
                })}
              </Heading>
            </Section>
            <p className="oe-coldStorage-pageSubtitle">
              {intl.formatMessage({
                id: "coldStorage.dashboard.subtitle",
                defaultMessage: "Real-time temperature monitoring & compliance",
              })}
            </p>
          </Section>
          <Section>
            <div className="oe-coldStorage-statusRow">
              <InlineNotification
                title={intl.formatMessage(
                  {
                    id: "coldStorage.dashboard.systemStatus",
                    defaultMessage: "System Status: {status}",
                  },
                  {
                    status: dashboardLoading
                      ? intl.formatMessage({
                          id: "coldStorage.dashboard.refreshing",
                          defaultMessage: "Refreshing",
                        })
                      : intl.formatMessage({
                          id: "coldStorage.dashboard.online",
                          defaultMessage: "Online",
                        }),
                  },
                )}
                subtitle={intl.formatMessage(
                  {
                    id: "coldStorage.dashboard.lastUpdate",
                    defaultMessage: "Last update: {time}",
                  },
                  { time: lastUpdateLabel },
                )}
                kind={dashboardLoading ? "info" : "success"}
                lowContrast
                hideCloseButton
                className="oe-coldStorage-systemStatus"
              />
              <Button
                kind="ghost"
                size="sm"
                disabled={dashboardLoading}
                onClick={loadDashboardData}
              >
                {dashboardLoading
                  ? intl.formatMessage({
                      id: "coldStorage.dashboard.refreshingEllipsis",
                      defaultMessage: "Refreshing...",
                    })
                  : intl.formatMessage({
                      id: "coldStorage.dashboard.refresh",
                      defaultMessage: "Refresh",
                    })}
              </Button>
            </div>
          </Section>
        </Column>
      </Grid>
      <div className="orderLegendBody">
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Tabs
                selectedIndex={selectedTabIndex}
                onChange={({ selectedIndex }) => {
                  setSelectedTabIndex(selectedIndex);
                  // Clear preselected freezer when switching away from Historical Trends tab
                  if (selectedIndex !== 2) {
                    setPreselectedFreezerId(null);
                  }
                }}
              >
                <TabList aria-label="Cold storage sections" contained>
                  <Tab>
                    <FormattedMessage
                      id="coldStorage.dashboard.tab.dashboard"
                      defaultMessage="Dashboard"
                    />
                  </Tab>
                  <Tab>
                    <FormattedMessage
                      id="coldStorage.dashboard.tab.correctiveActions"
                      defaultMessage="Corrective Actions"
                    />
                  </Tab>
                  <Tab>
                    <FormattedMessage
                      id="coldStorage.dashboard.tab.historicalTrends"
                      defaultMessage="Historical Trends"
                    />
                  </Tab>
                  <Tab>
                    <FormattedMessage
                      id="coldStorage.dashboard.tab.reports"
                      defaultMessage="Reports"
                    />
                  </Tab>
                  <Tab>
                    <FormattedMessage
                      id="coldStorage.dashboard.tab.settings"
                      defaultMessage="Settings"
                    />
                  </Tab>
                </TabList>
                <TabPanels>
                  <TabPanel>
                    <Grid fullWidth className="oe-coldStorage-grid">
                      {criticalUnits > 0 && (
                        <Column lg={16} md={8} sm={4}>
                          <InlineNotification
                            kind="error"
                            title={intl.formatMessage({
                              id: "coldStorage.dashboard.criticalAlertTitle",
                              defaultMessage: "CRITICAL ALERT",
                            })}
                            subtitle={intl.formatMessage(
                              {
                                id: "coldStorage.dashboard.criticalAlertSubtitle",
                                defaultMessage:
                                  "{count} storage unit(s) experiencing critical temperature excursions",
                              },
                              { count: criticalUnits },
                            )}
                            hideCloseButton
                            lowContrast={false}
                            size="sm"
                          />
                        </Column>
                      )}

                      <Column lg={16} md={8} sm={4}>
                        <Grid condensed className="oe-coldStorage-kpis">
                          <Column lg={4} md={4} sm={4}>
                            <div className="oe-coldStorage-kpiCard">
                              <p className="oe-coldStorage-kpiLabel">
                                <FormattedMessage
                                  id="coldStorage.dashboard.kpi.totalUnits"
                                  defaultMessage="Total Storage Units"
                                />
                              </p>
                              <p className="oe-coldStorage-kpiValue">
                                {totalUnits}
                              </p>
                            </div>
                          </Column>
                          <Column lg={4} md={4} sm={4}>
                            <div className="oe-coldStorage-kpiCard">
                              <p className="oe-coldStorage-kpiLabel">
                                <FormattedMessage
                                  id="coldStorage.dashboard.kpi.normal"
                                  defaultMessage="Normal Status"
                                />
                              </p>
                              <p className="oe-coldStorage-kpiValue">
                                {normalUnits}
                              </p>
                            </div>
                          </Column>
                          <Column lg={4} md={4} sm={4}>
                            <div className="oe-coldStorage-kpiCard">
                              <p className="oe-coldStorage-kpiLabel">
                                <FormattedMessage
                                  id="coldStorage.dashboard.kpi.warnings"
                                  defaultMessage="Warnings"
                                />
                              </p>
                              <p className="oe-coldStorage-kpiValue">
                                {warningUnits}
                              </p>
                            </div>
                          </Column>
                          <Column lg={4} md={4} sm={4}>
                            <div className="oe-coldStorage-kpiCard">
                              <p className="oe-coldStorage-kpiLabel">
                                <FormattedMessage
                                  id="coldStorage.dashboard.kpi.critical"
                                  defaultMessage="Critical Alerts"
                                />
                              </p>
                              <p className="oe-coldStorage-kpiValue">
                                {criticalUnits}
                              </p>
                            </div>
                          </Column>
                        </Grid>
                      </Column>

                      <Column lg={16} md={8} sm={4}>
                        <Form
                          onSubmit={(event) => event.preventDefault()}
                          className={`oe-coldStorage-filterForm${
                            isMobile ? " oe-coldStorage-filterForm--mobile" : ""
                          }`}
                        >
                          <Search
                            size="lg"
                            labelText={intl.formatMessage({
                              id: "coldStorage.dashboard.searchLabel",
                              defaultMessage: "Search by Unit ID or Name",
                            })}
                            placeholder={intl.formatMessage({
                              id: "coldStorage.dashboard.searchLabel",
                              defaultMessage: "Search by Unit ID or Name",
                            })}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            value={searchTerm}
                            className={`oe-coldStorage-filterSearch${
                              isMobile
                                ? " oe-coldStorage-filterSearch--mobile"
                                : ""
                            }`}
                          />
                          <div
                            className={`oe-coldStorage-filterControls${
                              isMobile
                                ? " oe-coldStorage-filterControls--mobile"
                                : ""
                            }`}
                          >
                            <Dropdown
                              id="status-filter"
                              label="All Status"
                              titleText={intl.formatMessage({
                                id: "coldStorage.dashboard.statusFilter",
                                defaultMessage: "Status",
                              })}
                              items={STATUS_OPTIONS}
                              selectedItem={statusFilter}
                              onChange={({ selectedItem }) =>
                                setStatusFilter(selectedItem)
                              }
                            />
                            <Dropdown
                              id="device-filter"
                              label="All Device Types"
                              titleText={intl.formatMessage({
                                id: "coldStorage.dashboard.deviceTypeFilter",
                                defaultMessage: "Device Type",
                              })}
                              items={deviceOptions}
                              selectedItem={deviceFilter}
                              onChange={({ selectedItem }) =>
                                setDeviceFilter(selectedItem)
                              }
                            />
                          </div>
                        </Form>

                        <DataTable
                          rows={paginatedUnits.map((row) => ({
                            id: row.id,
                            ...row,
                            isExpanded: !!expandedRowIds[String(row.id || "")],
                          }))}
                          headers={columns}
                          size="lg"
                          expandableRows
                        >
                          {({
                            rows,
                            headers,
                            getHeaderProps,
                            getTableProps,
                            getRowProps,
                          }) => (
                            <TableContainer
                              title={intl.formatMessage({
                                id: "coldStorage.dashboard.storageUnitsTitle",
                                defaultMessage: "Storage Units",
                              })}
                            >
                              <Table {...getTableProps()}>
                                <TableHead>
                                  <TableRow>
                                    <TableExpandHeader aria-label="expand row" />
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
                                        colSpan={columns.length + 2}
                                        className="empty-state"
                                      >
                                        {dashboardLoading
                                          ? intl.formatMessage({
                                              id: "coldStorage.dashboard.loadingUnits",
                                              defaultMessage:
                                                "Loading storage units…",
                                            })
                                          : intl.formatMessage({
                                              id: "coldStorage.dashboard.noUnitsFound",
                                              defaultMessage:
                                                "No storage units found.",
                                            })}
                                      </TableCell>
                                    </TableRow>
                                  )}
                                  {rows.map((row) => {
                                    const unit =
                                      storageUnits.find(
                                        (u) => u.id === row.id,
                                      ) || row;
                                    return (
                                      <React.Fragment key={row.id}>
                                        <TableExpandRow
                                          isExpanded={row.isExpanded}
                                          ariaLabel={
                                            row.isExpanded
                                              ? "Collapse row"
                                              : "Expand row"
                                          }
                                          {...getRowProps({
                                            row,
                                            onClick: () => {
                                              handleRowExpand(row.id);
                                            },
                                          })}
                                        >
                                          {row.cells.map((cell) => {
                                            if (cell.info.header === "status") {
                                              return (
                                                <TableCell key={cell.id}>
                                                  <div
                                                    style={{
                                                      display: "flex",
                                                      gap: "0.35rem",
                                                      flexWrap: "wrap",
                                                    }}
                                                  >
                                                    {statusTag(cell.value)}
                                                    {stalenessTag(
                                                      unit.lastReading,
                                                    )}
                                                  </div>
                                                </TableCell>
                                              );
                                            }
                                            if (
                                              cell.info.header === "currentTemp"
                                            ) {
                                              return (
                                                <TableCell key={cell.id}>
                                                  <span
                                                    className={temperatureColor(
                                                      unit.currentTemp,
                                                      unit.targetTemp,
                                                    )}
                                                  >
                                                    {formatTemperatureDisplay(
                                                      unit.currentTemp,
                                                    )}
                                                  </span>
                                                </TableCell>
                                              );
                                            }
                                            if (
                                              cell.info.header === "targetTemp"
                                            ) {
                                              return (
                                                <TableCell key={cell.id}>
                                                  {formatTemperatureDisplay(
                                                    unit.targetTemp,
                                                  )}
                                                </TableCell>
                                              );
                                            }
                                            if (
                                              cell.info.header ===
                                              "currentHumidity"
                                            ) {
                                              return (
                                                <TableCell key={cell.id}>
                                                  {formatHumidityDisplay(
                                                    unit.currentHumidity,
                                                  )}
                                                </TableCell>
                                              );
                                            }
                                            if (
                                              cell.info.header ===
                                              "currentTemp2"
                                            ) {
                                              return (
                                                <TableCell key={cell.id}>
                                                  {formatTemperatureDisplay(
                                                    unit.currentTemp2,
                                                  )}
                                                </TableCell>
                                              );
                                            }
                                            if (
                                              cell.info.header === "lastReading"
                                            ) {
                                              return (
                                                <TableCell key={cell.id}>
                                                  {formatDateTime(
                                                    unit.lastReading,
                                                  )}
                                                </TableCell>
                                              );
                                            }
                                            return (
                                              <TableCell key={cell.id}>
                                                {cell.value}
                                              </TableCell>
                                            );
                                          })}
                                        </TableExpandRow>
                                        {row.isExpanded && (
                                          <TableExpandedRow
                                            colSpan={headers.length + 1}
                                          >
                                            <DeviceHistoryExpansion
                                              key={unit.id || unit.freezerId}
                                              device={unit}
                                            />
                                          </TableExpandedRow>
                                        )}
                                      </React.Fragment>
                                    );
                                  })}
                                </TableBody>
                              </Table>
                            </TableContainer>
                          )}
                        </DataTable>

                        {filteredUnits.length > 0 && (
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
                            pageSizes={[5, 10, 20, 30, 50]}
                            totalItems={filteredUnits.length}
                            onChange={({ page, pageSize: newPageSize }) => {
                              setCurrentPage(page);
                              setPageSize(newPageSize);
                            }}
                          />
                        )}
                      </Column>

                      <Column lg={16} md={8} sm={4}>
                        <Section className="oe-coldStorage-activeAlertsSection">
                          <Heading className="oe-coldStorage-activeAlertsHeading">
                            <FormattedMessage
                              id="coldStorage.dashboard.activeAlerts"
                              defaultMessage="Active Alerts ({count})"
                              values={{ count: activeAlerts.length }}
                            />
                          </Heading>

                          {activeAlerts.length > 0 ? (
                            <>
                              <DataTable
                                rows={paginatedAlerts.map((alert) => ({
                                  id: alert.id.toString(),
                                  severity: (
                                    <Tag
                                      type={
                                        alert.severity === "CRITICAL"
                                          ? "red"
                                          : "warm-gray"
                                      }
                                    >
                                      {alert.severity}
                                    </Tag>
                                  ),
                                  device: alert.unitName,
                                  location: alert.location,
                                  temperature: formatTemperatureDisplay(
                                    alert.currentTemp,
                                  ),
                                  duration: formatDuration(
                                    alert.durationSeconds,
                                  ),
                                  startedAt: formatDateTime(alert.startedAt),
                                  status: alert.status,
                                  _alert: alert,
                                }))}
                                headers={[
                                  {
                                    key: "severity",
                                    header: intl.formatMessage({
                                      id: "coldStorage.dashboard.column.severity",
                                      defaultMessage: "Severity",
                                    }),
                                  },
                                  {
                                    key: "device",
                                    header: intl.formatMessage({
                                      id: "coldStorage.dashboard.column.device",
                                      defaultMessage: "Device",
                                    }),
                                  },
                                  {
                                    key: "location",
                                    header: intl.formatMessage({
                                      id: "coldStorage.dashboard.column.location",
                                      defaultMessage: "Location",
                                    }),
                                  },
                                  {
                                    key: "temperature",
                                    header: intl.formatMessage({
                                      id: "coldStorage.dashboard.column.temperature",
                                      defaultMessage: "Temperature",
                                    }),
                                  },
                                  {
                                    key: "duration",
                                    header: intl.formatMessage({
                                      id: "coldStorage.dashboard.column.duration",
                                      defaultMessage: "Duration",
                                    }),
                                  },
                                  {
                                    key: "startedAt",
                                    header: intl.formatMessage({
                                      id: "coldStorage.dashboard.column.started",
                                      defaultMessage: "Started",
                                    }),
                                  },
                                ]}
                                size="sm"
                              >
                                {({
                                  rows,
                                  headers,
                                  getHeaderProps,
                                  getRowProps,
                                  getTableProps,
                                  getTableContainerProps,
                                }) => (
                                  <TableContainer
                                    {...getTableContainerProps()}
                                    className="oe-coldStorage-activeAlertsTable"
                                  >
                                    <Table
                                      {...getTableProps()}
                                      size="sm"
                                      useZebraStyles
                                    >
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
                                          <TableHeader>
                                            {intl.formatMessage({
                                              id: "coldStorage.actions",
                                              defaultMessage: "Actions",
                                            })}
                                          </TableHeader>
                                        </TableRow>
                                      </TableHead>
                                      <TableBody>
                                        {rows.map((row) => {
                                          const alert = activeAlerts.find(
                                            (a) => a.id.toString() === row.id,
                                          );
                                          return (
                                            <TableRow
                                              key={row.id}
                                              {...getRowProps({ row })}
                                              className="oe-coldStorage-clickableRow"
                                              onClick={() =>
                                                handleAlertRowClick(alert.id)
                                              }
                                            >
                                              {row.cells.map((cell) => (
                                                <TableCell key={cell.id}>
                                                  {cell.value}
                                                </TableCell>
                                              ))}
                                              <TableCell>
                                                <div className="oe-coldStorage-rowActions">
                                                  <Button
                                                    kind="ghost"
                                                    size="sm"
                                                    renderIcon={View}
                                                    iconDescription={intl.formatMessage(
                                                      {
                                                        id: "coldStorage.dashboard.viewAlertDetails",
                                                        defaultMessage:
                                                          "View alert details",
                                                      },
                                                    )}
                                                    hasIconOnly
                                                    onClick={(e) => {
                                                      e.stopPropagation();
                                                      handleAlertRowClick(
                                                        alert.id,
                                                      );
                                                    }}
                                                  />
                                                  {alert.status === "OPEN" && (
                                                    <Button
                                                      kind="ghost"
                                                      size="sm"
                                                      disabled={
                                                        actionInFlight ===
                                                          alert.id ||
                                                        !currentUserId
                                                      }
                                                      onClick={(e) => {
                                                        e.stopPropagation();
                                                        handleAcknowledgeAlert(
                                                          alert.id,
                                                        );
                                                      }}
                                                    >
                                                      {intl.formatMessage({
                                                        id: "coldStorage.dashboard.acknowledge",
                                                        defaultMessage:
                                                          "Acknowledge",
                                                      })}
                                                    </Button>
                                                  )}
                                                  {isAdminUser && (
                                                    <Button
                                                      kind="danger--ghost"
                                                      size="sm"
                                                      renderIcon={TrashCan}
                                                      iconDescription={intl.formatMessage(
                                                        {
                                                          id: "coldStorage.dashboard.deleteAlert",
                                                          defaultMessage:
                                                            "Delete alert",
                                                        },
                                                      )}
                                                      hasIconOnly
                                                      disabled={
                                                        actionInFlight ===
                                                        alert.id
                                                      }
                                                      onClick={(e) => {
                                                        e.stopPropagation();
                                                        handleDeleteAlert(
                                                          alert.id,
                                                        );
                                                      }}
                                                    />
                                                  )}
                                                </div>
                                              </TableCell>
                                            </TableRow>
                                          );
                                        })}
                                      </TableBody>
                                    </Table>
                                  </TableContainer>
                                )}
                              </DataTable>

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
                                page={alertsCurrentPage}
                                pageSize={alertsPageSize}
                                pageSizes={[5, 10, 20, 30, 50]}
                                totalItems={activeAlerts.length}
                                onChange={({ page, pageSize: newPageSize }) => {
                                  setAlertsCurrentPage(page);
                                  setAlertsPageSize(newPageSize);
                                }}
                              />
                            </>
                          ) : (
                            <Tile className="oe-coldStorage-emptyAlertsTile">
                              <p>
                                {intl.formatMessage({
                                  id: "coldStorage.dashboard.noActiveAlerts",
                                  defaultMessage: "No active alerts",
                                })}
                              </p>
                            </Tile>
                          )}
                        </Section>
                      </Column>
                    </Grid>
                    <Grid fullWidth>
                      <Column lg={16} md={8} sm={4}>
                        <p className="hist-footer">
                          {intl.formatMessage({
                            id: "coldStorage.footer",
                            defaultMessage:
                              "Cold Storage Monitoring v2.1.0 | Compliant with CAP, CLIA, FDA, and WHO guidelines | HIPAA Compliant Data Handling",
                          })}
                        </p>
                      </Column>
                    </Grid>
                  </TabPanel>

                  <TabPanel>
                    <CorrectiveActions />
                  </TabPanel>
                  <TabPanel>
                    <HistoricalTrends
                      devices={storageUnits}
                      initialSelectedFreezerId={preselectedFreezerId}
                      onFreezerSelected={(freezerId) =>
                        setPreselectedFreezerId(freezerId)
                      }
                    />
                  </TabPanel>
                  <TabPanel>
                    <Reports devices={storageUnits} />
                  </TabPanel>
                  <TabPanel>
                    <Settings />
                  </TabPanel>
                </TabPanels>
              </Tabs>
            </Section>
          </Column>
        </Grid>
      </div>

      <AlertDetailModal
        alertId={selectedAlertId}
        open={showAlertDetail}
        onClose={() => {
          setShowAlertDetail(false);
          setSelectedAlertId(null);
        }}
      />
    </>
  );
}

export default injectIntl(FreezerMonitoringDashboard);

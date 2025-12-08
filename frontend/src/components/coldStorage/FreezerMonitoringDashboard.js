import React, {
  useCallback,
  useEffect,
  useMemo,
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
  Loading,
  Pagination,
} from "@carbon/react";
import { CheckmarkFilled, View } from "@carbon/icons-react";
import "./FreezerMonitoringDashboard.scss";
import CorrectiveActions from "./CorrectiveActions";
import HistoricalTrends from "./HistoricalTrends";
import Reports from "./Reports";
import Settings from "./Settings";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { injectIntl } from "react-intl";
import {
  fetchFreezerStatus,
  fetchOpenAlerts,
  acknowledgeAlert,
  resolveAlert,
  fetchFilteredAlerts,
} from "./api";
import AlertDetailModal from "./AlertDetailModal";
import DeviceHistoryExpansion from "./DeviceHistoryExpansion";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { NotificationContext } from "../layout/Layout";

const COLUMNS = [
  { key: "id", header: "Unit ID" },
  { key: "status", header: "Status" },
  { key: "unitName", header: "Unit Name" },
  { key: "deviceType", header: "Device Type" },
  { key: "location", header: "Location" },
  { key: "currentTemp", header: "Current Temp" },
  { key: "targetTemp", header: "Target Temp" },
  { key: "protocol", header: "Protocol" },
  { key: "lastReading", header: "Last Reading" },
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
    default:
      return <Tag>{status}</Tag>;
  }
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
  if (!value) {
    return "—";
  }
  try {
    // If value is a number and appears to be in seconds (Unix timestamp < year 2100 in milliseconds)
    // convert it to milliseconds
    const timestamp =
      typeof value === "number" && value < 4102444800000 ? value * 1000 : value;
    return new Date(timestamp).toLocaleString();
  } catch (error) {
    return value;
  }
};

const normalizeUnit = (unit) => ({
  id: unit.freezerId?.toString() ?? unit.freezerName ?? "UNKNOWN",
  status: unit.status ?? "NORMAL",
  unitName: unit.freezerName ?? unit.freezerId ?? "Unnamed Freezer",
  deviceType: unit.deviceType ?? DEFAULT_DEVICE_TYPE,
  location: unit.locationName ?? "Unknown location",
  currentTemp: toNumber(unit.temperatureCelsius),
  targetTemp: toNumber(
    unit.targetTemperatureCelsius ?? unit.temperatureCelsius,
  ),
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
  let durationMinutes = null;
  if (alert.startTime) {
    try {
      const startTime = new Date(alert.startTime);
      const now = new Date();
      durationSeconds = Math.floor((now - startTime) / 1000);
      durationMinutes = Math.max(1, Math.round(durationSeconds / 60));
    } catch (e) {
      console.warn("Failed to calculate alert duration:", alert.startTime);
    }
  }

  return {
    id: alert.id,
    severity: alert.severity ?? "WARNING",
    status: alert.status ?? "OPEN",
    unitName: alert.freezer?.name ?? `Freezer ${alert.alertEntityId}`,
    location: alert.freezer?.code ?? "Unknown location",
    currentTemp,
    durationSeconds,
    durationMinutes,
    startedAt: alert.startTime,
  };
};

const formatTemperatureDisplay = (value) =>
  value == null ? "—" : `${value.toFixed(1)}°C`;

function FreezerMonitoringDashboard({ intl }) {
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

  const handleRowExpand = useCallback((rowId) => {
    const rowIdStr = String(rowId || "");
    setExpandedRowIds((prevExpanded) => ({
      ...prevExpanded,
      [rowIdStr]: !prevExpanded[rowIdStr],
    }));
  }, []);

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
      notify({
        kind: NotificationKinds.error,
        title: "Unable to update cold storage data",
        subtitle:
          error.message || "Unable to load cold storage monitoring data.",
      });
    } finally {
      setDashboardLoading(false);
    }
  }, [notify]);

  useEffect(() => {
    loadDashboardData();
  }, [loadDashboardData]);

  // Reset to first page when filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [statusFilter, deviceFilter, searchTerm]);

  const handleAlertAction = useCallback(
    async (alertId, action) => {
      setActionInFlight(alertId);
      try {
        if (action === "acknowledge") {
          await acknowledgeAlert(
            alertId,
            1,
            "Acknowledged via Cold Storage dashboard",
          );
        } else {
          await resolveAlert(alertId, 1, "Resolved via Cold Storage dashboard");
        }
        await loadDashboardData();
        notify({
          kind: NotificationKinds.success,
          title: "Success",
          subtitle: `Alert ${action === "acknowledge" ? "acknowledged" : "resolved"} successfully`,
        });
      } catch (error) {
        notify({
          kind: NotificationKinds.error,
          title: "Error",
          subtitle: error.message || `Unable to ${action} alert ${alertId}`,
        });
      } finally {
        setActionInFlight(null);
      }
    },
    [loadDashboardData, notify],
  );

  const handleAcknowledgeAlert = useCallback(
    (alertId) => handleAlertAction(alertId, "acknowledge"),
    [handleAlertAction],
  );

  const handleResolveAlert = useCallback(
    (alertId) => handleAlertAction(alertId, "resolve"),
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
              Real-time temperature monitoring & compliance
            </p>
          </Section>
          <Section>
            <div className="oe-coldStorage-statusRow">
              <InlineNotification
                title={`System Status: ${
                  dashboardLoading ? "Refreshing" : "Online"
                }`}
                subtitle={`Last update: ${lastUpdateLabel}`}
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
                {dashboardLoading ? "Refreshing..." : "Refresh"}
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
                  <Tab>Dashboard</Tab>
                  <Tab>Corrective Actions</Tab>
                  <Tab>Historical Trends</Tab>
                  <Tab>Reports</Tab>
                  <Tab>Settings</Tab>
                </TabList>
                <TabPanels>
                  <TabPanel>
                    <Grid fullWidth className="oe-coldStorage-grid">
                      {criticalUnits > 0 && (
                        <Column lg={16} md={8} sm={4}>
                          <InlineNotification
                            kind="error"
                            title="CRITICAL ALERT"
                            subtitle={`${criticalUnits} storage unit(s) experiencing critical temperature excursions`}
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
                                Total Storage Units
                              </p>
                              <p className="oe-coldStorage-kpiValue">
                                {totalUnits}
                              </p>
                            </div>
                          </Column>
                          <Column lg={4} md={4} sm={4}>
                            <div className="oe-coldStorage-kpiCard">
                              <p className="oe-coldStorage-kpiLabel">
                                Normal Status
                              </p>
                              <p className="oe-coldStorage-kpiValue">
                                {normalUnits}
                              </p>
                            </div>
                          </Column>
                          <Column lg={4} md={4} sm={4}>
                            <div className="oe-coldStorage-kpiCard">
                              <p className="oe-coldStorage-kpiLabel">
                                Warnings
                              </p>
                              <p className="oe-coldStorage-kpiValue">
                                {warningUnits}
                              </p>
                            </div>
                          </Column>
                          <Column lg={4} md={4} sm={4}>
                            <div className="oe-coldStorage-kpiCard">
                              <p className="oe-coldStorage-kpiLabel">
                                Critical Alerts
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
                          style={{
                            display: "flex",
                            flexDirection: isMobile ? "column" : "row",
                            gap: isMobile ? "1rem" : "1.5rem",
                            justifyContent: isMobile
                              ? "stretch"
                              : "space-between",
                            alignItems: isMobile ? "stretch" : "center",
                            flexWrap: "wrap",
                            marginBottom: "1rem",
                          }}
                        >
                          <Search
                            size="lg"
                            labelText="Search by Unit ID or Name"
                            placeholder="Search by Unit ID or Name"
                            onChange={(e) => setSearchTerm(e.target.value)}
                            value={searchTerm}
                            style={{
                              flex: isMobile ? "1 1 100%" : "1 1 40%",
                              minWidth: isMobile ? "100%" : "15rem",
                            }}
                          />
                          <div
                            style={{
                              display: "flex",
                              flexDirection: isMobile ? "column" : "row",
                              gap: isMobile ? "0.75rem" : "0.5rem",
                              width: isMobile ? "100%" : "auto",
                              alignItems: "stretch",
                              justifyContent: isMobile ? "stretch" : "center",
                            }}
                          >
                            <Dropdown
                              id="status-filter"
                              label="All Status"
                              titleText="Status"
                              items={STATUS_OPTIONS}
                              selectedItem={statusFilter}
                              onChange={({ selectedItem }) =>
                                setStatusFilter(selectedItem)
                              }
                            />
                            <Dropdown
                              id="device-filter"
                              label="All Device Types"
                              titleText="Device Type"
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
                          headers={COLUMNS}
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
                            <TableContainer title="Storage Units">
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
                                        colSpan={COLUMNS.length + 2}
                                        className="empty-state"
                                      >
                                        {dashboardLoading
                                          ? "Loading storage units…"
                                          : "No storage units found."}
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
                                                  {statusTag(cell.value)}
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
                            backwardText="Previous page"
                            forwardText="Next page"
                            itemsPerPageText="Items per page:"
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
                        <Section style={{ marginTop: "2rem" }}>
                          <Heading style={{ marginBottom: "1rem" }}>
                            Active Alerts ({activeAlerts.length})
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
                                  duration: alert.durationMinutes
                                    ? `${alert.durationMinutes} min`
                                    : "—",
                                  startedAt: formatDateTime(alert.startedAt),
                                  status: alert.status,
                                  _alert: alert,
                                }))}
                                headers={[
                                  { key: "severity", header: "Severity" },
                                  { key: "device", header: "Device" },
                                  { key: "location", header: "Location" },
                                  { key: "temperature", header: "Temperature" },
                                  { key: "duration", header: "Duration" },
                                  { key: "startedAt", header: "Started" },
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
                                    style={{ maxHeight: "400px" }}
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
                                          <TableHeader>Actions</TableHeader>
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
                                              style={{ cursor: "pointer" }}
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
                                                <div
                                                  style={{
                                                    display: "flex",
                                                    gap: "0.5rem",
                                                    alignItems: "center",
                                                  }}
                                                >
                                                  <Button
                                                    kind="ghost"
                                                    size="sm"
                                                    renderIcon={View}
                                                    iconDescription="View alert details"
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
                                                        alert.id
                                                      }
                                                      onClick={(e) => {
                                                        e.stopPropagation();
                                                        handleAcknowledgeAlert(
                                                          alert.id,
                                                        );
                                                      }}
                                                    >
                                                      Acknowledge
                                                    </Button>
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
                                backwardText="Previous page"
                                forwardText="Next page"
                                itemsPerPageText="Items per page:"
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
                            <Tile
                              style={{ padding: "1rem", textAlign: "center" }}
                            >
                              <p style={{ margin: 0 }}>No active alerts</p>
                            </Tile>
                          )}
                        </Section>
                      </Column>
                    </Grid>
                    <Grid fullWidth>
                      <Column lg={16} md={8} sm={4}>
                        <p className="hist-footer">
                          Cold Storage Monitoring v2.1.0 | Compliant with CAP,
                          CLIA, FDA, and WHO guidelines | HIPAA Compliant Data
                          Handling
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

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
} from "@carbon/react";
import { CheckmarkFilled } from "@carbon/icons-react";
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
  const currentTemp =
    toNumber(alert.currentTemperature) ??
    toNumber(alert.maxTemperature) ??
    toNumber(alert.minTemperature);
  return {
    id: alert.id,
    severity: alert.severity ?? "WARNING",
    status: alert.status ?? "OPEN",
    unitName: alert.freezerName ?? `Freezer ${alert.freezerId}`,
    location: alert.locationName ?? "Unknown location",
    currentTemp,
    durationSeconds: alert.durationSeconds ?? null,
    durationMinutes:
      alert.durationSeconds != null
        ? Math.max(1, Math.round(alert.durationSeconds / 60))
        : null,
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

  const handleAlertAction = useCallback(
    async (alertId, action) => {
      setActionInFlight(alertId);
      try {
        if (action === "acknowledge") {
          await acknowledgeAlert(
            alertId,
            "Acknowledged via Cold Storage dashboard",
          );
        } else {
          await resolveAlert(alertId, "Resolved via Cold Storage dashboard");
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
                          rows={filteredUnits.map((row) => ({
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
                      </Column>

                      <Column lg={16} md={8} sm={4}>
                        <Section style={{ marginTop: "2rem" }}>
                          <Heading style={{ marginBottom: "1rem" }}>
                            Active Alerts
                          </Heading>

                          {activeAlerts.length > 0 ? (
                            <Grid
                              condensed
                              className="oe-coldStorage-alerts-grid"
                            >
                              {activeAlerts.map((alert) => (
                                <Column lg={8} md={8} sm={4} key={alert.id}>
                                  <Tile
                                    className="oe-coldStorage-alertCard"
                                    style={{
                                      padding: "1.5rem",
                                      marginBottom: "1rem",
                                      cursor: "pointer",
                                      borderLeft:
                                        alert.severity === "CRITICAL"
                                          ? "4px solid #da1e28"
                                          : "4px solid #f1c21b",
                                    }}
                                    onClick={() =>
                                      handleAlertRowClick(alert.id)
                                    }
                                  >
                                    <div
                                      style={{
                                        display: "flex",
                                        justifyContent: "space-between",
                                        marginBottom: "1rem",
                                      }}
                                    >
                                      <Tag
                                        type={
                                          alert.severity === "CRITICAL"
                                            ? "red"
                                            : "warm-gray"
                                        }
                                      >
                                        {alert.severity}
                                      </Tag>
                                      <span
                                        style={{
                                          fontSize: "0.875rem",
                                          color: "#525252",
                                        }}
                                      >
                                        {formatDateTime(alert.startedAt)}
                                      </span>
                                    </div>

                                    <p
                                      style={{
                                        fontSize: "1rem",
                                        fontWeight: "600",
                                        marginBottom: "0.5rem",
                                      }}
                                    >
                                      {alert.unitName}
                                    </p>

                                    <p
                                      style={{
                                        fontSize: "0.875rem",
                                        color: "#525252",
                                        marginBottom: "0.5rem",
                                      }}
                                    >
                                      {alert.location}
                                    </p>

                                    <div
                                      style={{
                                        display: "flex",
                                        justifyContent: "space-between",
                                        alignItems: "center",
                                        marginTop: "1rem",
                                      }}
                                    >
                                      <div>
                                        <span
                                          style={{
                                            fontSize: "1.25rem",
                                            fontWeight: "600",
                                            color:
                                              alert.severity === "CRITICAL"
                                                ? "#da1e28"
                                                : "#f1c21b",
                                          }}
                                        >
                                          {formatTemperatureDisplay(
                                            alert.currentTemp,
                                          )}
                                        </span>
                                        {alert.durationMinutes && (
                                          <span
                                            style={{
                                              fontSize: "0.875rem",
                                              color: "#525252",
                                              marginLeft: "1rem",
                                            }}
                                          >
                                            Duration: {alert.durationMinutes}{" "}
                                            min
                                          </span>
                                        )}
                                      </div>

                                      {alert.status === "OPEN" && (
                                        <Button
                                          kind="ghost"
                                          size="sm"
                                          disabled={actionInFlight === alert.id}
                                          onClick={(e) => {
                                            e.stopPropagation();
                                            handleAcknowledgeAlert(alert.id);
                                          }}
                                        >
                                          Acknowledge
                                        </Button>
                                      )}
                                    </div>
                                  </Tile>
                                </Column>
                              ))}
                            </Grid>
                          ) : (
                            <Tile
                              style={{ padding: "2rem", textAlign: "center" }}
                            >
                              <p>No active alerts</p>
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

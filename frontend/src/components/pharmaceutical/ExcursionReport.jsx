import React, { useState, useEffect, useCallback } from "react";
import {
  Grid,
  Column,
  Tile,
  Loading,
  InlineNotification,
  DatePicker,
  DatePickerInput,
  Button,
  Select,
  SelectItem,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
  Tag,
  Accordion,
  AccordionItem,
  Modal,
} from "@carbon/react";
import {
  Download,
  View,
  WarningAlt,
  Checkmark,
  Time,
  Filter,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";
import { DonutChart, SimpleBarChart } from "@carbon/charts-react";
import "@carbon/charts/styles.css";

const ExcursionReport = () => {
  const intl = useIntl();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [excursionHistory, setExcursionHistory] = useState([]);
  const [excursionMetrics, setExcursionMetrics] = useState(null);
  const [affectedSamples, setAffectedSamples] = useState([]);
  const [selectedExcursion, setSelectedExcursion] = useState(null);
  const [isViewModalOpen, setIsViewModalOpen] = useState(false);
  const [dateRange, setDateRange] = useState({
    startDate: new Date(Date.now() - 90 * 24 * 60 * 60 * 1000),
    endDate: new Date(),
  });
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [alertTypeFilter, setAlertTypeFilter] = useState("ALL");

  const fetchExcursionHistory = useCallback(() => {
    setLoading(true);
    const startTs = dateRange.startDate.getTime();
    const endTs = dateRange.endDate.getTime();

    getFromOpenElisServer(
      `/rest/pharmaceutical/reports/excursion-history?startDate=${startTs}&endDate=${endTs}`,
      (response) => {
        if (response) {
          setExcursionHistory(response);
        } else {
          setError("Failed to load excursion history");
        }
        setLoading(false);
      },
    );
  }, [dateRange]);

  const fetchExcursionMetrics = useCallback(() => {
    const startTs = dateRange.startDate.getTime();
    const endTs = dateRange.endDate.getTime();

    getFromOpenElisServer(
      `/rest/pharmaceutical/reports/excursions?startDate=${startTs}&endDate=${endTs}`,
      (response) => {
        if (response) {
          setExcursionMetrics(response);
        }
      },
    );
  }, [dateRange]);

  const fetchAffectedSamples = useCallback((excursionId) => {
    getFromOpenElisServer(
      `/rest/pharmaceutical/excursions/${excursionId}/affected-samples`,
      (response) => {
        if (response) {
          setAffectedSamples(response);
        }
      },
    );
  }, []);

  useEffect(() => {
    fetchExcursionHistory();
    fetchExcursionMetrics();
  }, [fetchExcursionHistory, fetchExcursionMetrics]);

  const handleDateChange = (dates) => {
    if (dates && dates.length === 2) {
      setDateRange({
        startDate: dates[0],
        endDate: dates[1],
      });
    }
  };

  const handleExportCSV = () => {
    const startTs = dateRange.startDate.getTime();
    const endTs = dateRange.endDate.getTime();
    window.open(
      `/rest/pharmaceutical/reports/export/excursions/csv?startDate=${startTs}&endDate=${endTs}`,
      "_blank",
    );
  };

  const handleExportPDF = () => {
    const startTs = dateRange.startDate.getTime();
    const endTs = dateRange.endDate.getTime();
    window.open(
      `/rest/pharmaceutical/reports/export/excursions/pdf?startDate=${startTs}&endDate=${endTs}`,
      "_blank",
    );
  };

  const openViewModal = (excursion) => {
    setSelectedExcursion(excursion);
    fetchAffectedSamples(excursion.id);
    setIsViewModalOpen(true);
  };

  const getStatusTag = (status) => {
    const statusColors = {
      ACTIVE: "red",
      ACKNOWLEDGED: "orange",
      RESOLVED: "green",
      ESCALATED: "purple",
    };
    return <Tag type={statusColors[status] || "gray"}>{status}</Tag>;
  };

  const getAlertTypeTag = (alertType) => {
    const alertColors = {
      TEMPERATURE_HIGH: "red",
      TEMPERATURE_LOW: "blue",
      HUMIDITY_HIGH: "cyan",
      HUMIDITY_LOW: "orange",
      POWER_FAILURE: "purple",
      DOOR_OPEN: "magenta",
    };
    return <Tag type={alertColors[alertType] || "gray"}>{alertType}</Tag>;
  };

  const formatDuration = (minutes) => {
    if (!minutes) return "--";
    if (minutes < 60) return `${minutes}m`;
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
  };

  const filteredHistory = excursionHistory.filter((exc) => {
    if (statusFilter !== "ALL" && exc.status !== statusFilter) return false;
    if (alertTypeFilter !== "ALL" && exc.alertType !== alertTypeFilter)
      return false;
    return true;
  });

  const headers = [
    {
      key: "id",
      header: intl.formatMessage({ id: "pharmaceutical.excursion.id" }),
    },
    {
      key: "deviceId",
      header: intl.formatMessage({ id: "pharmaceutical.excursion.device" }),
    },
    {
      key: "deviceLocation",
      header: intl.formatMessage({ id: "pharmaceutical.excursion.location" }),
    },
    {
      key: "alertType",
      header: intl.formatMessage({ id: "pharmaceutical.excursion.alertType" }),
    },
    {
      key: "recordedValue",
      header: intl.formatMessage({ id: "pharmaceutical.excursion.recorded" }),
    },
    {
      key: "thresholdValue",
      header: intl.formatMessage({ id: "pharmaceutical.excursion.threshold" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "pharmaceutical.excursion.status" }),
    },
    {
      key: "duration",
      header: intl.formatMessage({ id: "pharmaceutical.report.duration" }),
    },
    {
      key: "detectedAt",
      header: intl.formatMessage({ id: "pharmaceutical.report.detectedAt" }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "label.actions" }),
    },
  ];

  const rows = filteredHistory.map((exc) => ({
    id: String(exc.id),
    deviceId: exc.deviceId,
    deviceLocation: exc.deviceLocation || "--",
    alertType: exc.alertType,
    recordedValue: exc.recordedValue,
    thresholdValue: exc.thresholdValue,
    status: exc.status,
    duration: exc.durationMinutes,
    detectedAt: exc.detectedAt
      ? new Date(exc.detectedAt).toLocaleString()
      : "--",
    actions: exc,
  }));

  const statusDistribution = excursionMetrics?.byStatus
    ? Object.entries(excursionMetrics.byStatus).map(([key, value]) => ({
        group: key,
        value: value,
      }))
    : [];

  const alertTypeDistribution = excursionMetrics?.byAlertType
    ? Object.entries(excursionMetrics.byAlertType).map(([key, value]) => ({
        group: key,
        value: value,
      }))
    : [];

  const MetricTile = ({ title, value, icon: Icon, color, subtitle }) => (
    <Tile className="metric-tile">
      <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
        {Icon && <Icon size={24} style={{ color }} />}
        <div>
          <p style={{ fontSize: "0.875rem", color: "#525252" }}>{title}</p>
          <p style={{ fontSize: "2rem", fontWeight: "600", color }}>
            {value !== undefined ? value : "--"}
          </p>
          {subtitle && (
            <p style={{ fontSize: "0.75rem", color: "#8d8d8d" }}>{subtitle}</p>
          )}
        </div>
      </div>
    </Tile>
  );

  if (loading) {
    return <Loading />;
  }

  return (
    <Grid>
      <Column lg={16} md={8} sm={4}>
        {error && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({ id: "error.title" })}
            subtitle={error}
            onCloseButtonClick={() => setError(null)}
          />
        )}

        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: "1rem",
          }}
        >
          <h3>
            <FormattedMessage id="pharmaceutical.report.excursion.title" />
          </h3>
          <div style={{ display: "flex", gap: "0.5rem" }}>
            <Button
              kind="tertiary"
              size="sm"
              renderIcon={Download}
              onClick={handleExportCSV}
            >
              <FormattedMessage id="label.exportCSV" />
            </Button>
            <Button
              kind="tertiary"
              size="sm"
              renderIcon={Download}
              onClick={handleExportPDF}
            >
              <FormattedMessage id="label.exportPDF" />
            </Button>
          </div>
        </div>

        {/* Date Range and Filters */}
        <Tile style={{ marginBottom: "1rem" }}>
          <Grid>
            <Column lg={6} md={4} sm={4}>
              <DatePicker
                datePickerType="range"
                onChange={handleDateChange}
                value={[dateRange.startDate, dateRange.endDate]}
              >
                <DatePickerInput
                  id="start-date"
                  placeholder="mm/dd/yyyy"
                  labelText={intl.formatMessage({ id: "label.startDate" })}
                />
                <DatePickerInput
                  id="end-date"
                  placeholder="mm/dd/yyyy"
                  labelText={intl.formatMessage({ id: "label.endDate" })}
                />
              </DatePicker>
            </Column>
            <Column lg={3} md={2} sm={2}>
              <Select
                id="status-filter"
                labelText={intl.formatMessage({
                  id: "pharmaceutical.excursion.status",
                })}
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <SelectItem
                  value="ALL"
                  text={intl.formatMessage({ id: "label.all" })}
                />
                <SelectItem value="ACTIVE" text="Active" />
                <SelectItem value="ACKNOWLEDGED" text="Acknowledged" />
                <SelectItem value="RESOLVED" text="Resolved" />
                <SelectItem value="ESCALATED" text="Escalated" />
              </Select>
            </Column>
            <Column lg={3} md={2} sm={2}>
              <Select
                id="alert-type-filter"
                labelText={intl.formatMessage({
                  id: "pharmaceutical.excursion.alertType",
                })}
                value={alertTypeFilter}
                onChange={(e) => setAlertTypeFilter(e.target.value)}
              >
                <SelectItem
                  value="ALL"
                  text={intl.formatMessage({ id: "label.all" })}
                />
                <SelectItem value="TEMPERATURE_HIGH" text="Temperature High" />
                <SelectItem value="TEMPERATURE_LOW" text="Temperature Low" />
                <SelectItem value="HUMIDITY_HIGH" text="Humidity High" />
                <SelectItem value="HUMIDITY_LOW" text="Humidity Low" />
                <SelectItem value="POWER_FAILURE" text="Power Failure" />
                <SelectItem value="DOOR_OPEN" text="Door Open" />
              </Select>
            </Column>
            <Column
              lg={4}
              md={2}
              sm={2}
              style={{ display: "flex", alignItems: "flex-end" }}
            >
              <Button
                kind="primary"
                size="md"
                renderIcon={Filter}
                onClick={() => {
                  fetchExcursionHistory();
                  fetchExcursionMetrics();
                }}
              >
                <FormattedMessage id="label.applyFilters" />
              </Button>
            </Column>
          </Grid>
        </Tile>

        {/* Summary Metrics */}
        <h4 style={{ marginBottom: "1rem" }}>
          <FormattedMessage id="pharmaceutical.report.summary" />
        </h4>
        <Grid style={{ marginBottom: "1.5rem" }}>
          <Column lg={4} md={4} sm={4}>
            <MetricTile
              title={intl.formatMessage({
                id: "pharmaceutical.report.totalExcursions",
              })}
              value={excursionMetrics?.totalCount || 0}
              icon={WarningAlt}
              color="#da1e28"
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <MetricTile
              title={intl.formatMessage({
                id: "pharmaceutical.report.resolved",
              })}
              value={excursionMetrics?.resolvedCount || 0}
              icon={Checkmark}
              color="#24a148"
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <MetricTile
              title={intl.formatMessage({
                id: "pharmaceutical.report.avgResolutionTime",
              })}
              value={formatDuration(excursionMetrics?.avgResolutionMinutes)}
              icon={Time}
              color="#0043ce"
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <MetricTile
              title={intl.formatMessage({
                id: "pharmaceutical.report.samplesImpacted",
              })}
              value={excursionMetrics?.samplesImpacted || 0}
              color="#8a3ffc"
            />
          </Column>
        </Grid>

        {/* Charts */}
        <Grid style={{ marginBottom: "1.5rem" }}>
          <Column lg={8} md={4} sm={4}>
            <Tile>
              <h5>
                <FormattedMessage id="pharmaceutical.dashboard.excursionsByStatus" />
              </h5>
              {statusDistribution.length > 0 ? (
                <DonutChart
                  data={statusDistribution}
                  options={{
                    title: "",
                    resizable: true,
                    height: "250px",
                    donut: {
                      center: {
                        label: intl.formatMessage({
                          id: "pharmaceutical.dashboard.excursions",
                        }),
                      },
                    },
                    legend: {
                      alignment: "center",
                    },
                    color: {
                      scale: {
                        ACTIVE: "#da1e28",
                        ACKNOWLEDGED: "#f1c21b",
                        RESOLVED: "#24a148",
                        ESCALATED: "#8a3ffc",
                      },
                    },
                  }}
                />
              ) : (
                <p>
                  <FormattedMessage id="pharmaceutical.dashboard.noData" />
                </p>
              )}
            </Tile>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Tile>
              <h5>
                <FormattedMessage id="pharmaceutical.report.byAlertType" />
              </h5>
              {alertTypeDistribution.length > 0 ? (
                <SimpleBarChart
                  data={alertTypeDistribution}
                  options={{
                    title: "",
                    axes: {
                      left: {
                        mapsTo: "value",
                      },
                      bottom: {
                        mapsTo: "group",
                        scaleType: "labels",
                      },
                    },
                    height: "250px",
                    color: {
                      scale: {
                        TEMPERATURE_HIGH: "#da1e28",
                        TEMPERATURE_LOW: "#0043ce",
                        HUMIDITY_HIGH: "#1192e8",
                        HUMIDITY_LOW: "#f1c21b",
                        POWER_FAILURE: "#8a3ffc",
                        DOOR_OPEN: "#ee5396",
                      },
                    },
                  }}
                />
              ) : (
                <p>
                  <FormattedMessage id="pharmaceutical.dashboard.noData" />
                </p>
              )}
            </Tile>
          </Column>
        </Grid>

        {/* Excursion History Table */}
        <h4 style={{ marginBottom: "1rem" }}>
          <FormattedMessage id="pharmaceutical.report.history" />
        </h4>
        <DataTable rows={rows} headers={headers}>
          {({
            rows,
            headers,
            getTableProps,
            getHeaderProps,
            getRowProps,
            onInputChange,
          }) => (
            <>
              <TableToolbar>
                <TableToolbarContent>
                  <TableToolbarSearch onChange={onInputChange} />
                </TableToolbarContent>
              </TableToolbar>
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
                          {cell.info.header === "status" ? (
                            getStatusTag(cell.value)
                          ) : cell.info.header === "alertType" ? (
                            getAlertTypeTag(cell.value)
                          ) : cell.info.header === "duration" ? (
                            formatDuration(cell.value)
                          ) : cell.info.header === "actions" ? (
                            <Button
                              kind="ghost"
                              size="sm"
                              renderIcon={View}
                              iconDescription="View Details"
                              hasIconOnly
                              onClick={() => openViewModal(cell.value)}
                            />
                          ) : (
                            cell.value
                          )}
                        </TableCell>
                      ))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </>
          )}
        </DataTable>

        {/* View Excursion Details Modal */}
        <Modal
          open={isViewModalOpen}
          onRequestClose={() => {
            setIsViewModalOpen(false);
            setAffectedSamples([]);
          }}
          modalHeading={intl.formatMessage({
            id: "pharmaceutical.excursion.details.title",
          })}
          passiveModal
          size="lg"
        >
          {selectedExcursion && (
            <div>
              <Grid>
                <Column lg={8} md={4} sm={4}>
                  <h5 style={{ marginBottom: "1rem" }}>
                    <FormattedMessage id="pharmaceutical.report.excursionInfo" />
                  </h5>
                  <p>
                    <strong>
                      <FormattedMessage id="pharmaceutical.excursion.id" />:
                    </strong>{" "}
                    {selectedExcursion.id}
                  </p>
                  <p>
                    <strong>
                      <FormattedMessage id="pharmaceutical.excursion.device" />:
                    </strong>{" "}
                    {selectedExcursion.deviceId}
                  </p>
                  <p>
                    <strong>
                      <FormattedMessage id="pharmaceutical.excursion.location" />
                      :
                    </strong>{" "}
                    {selectedExcursion.deviceLocation || "--"}
                  </p>
                  <p>
                    <strong>
                      <FormattedMessage id="pharmaceutical.excursion.alertType" />
                      :
                    </strong>{" "}
                    {getAlertTypeTag(selectedExcursion.alertType)}
                  </p>
                  <p>
                    <strong>
                      <FormattedMessage id="pharmaceutical.excursion.recorded" />
                      :
                    </strong>{" "}
                    {selectedExcursion.recordedValue}
                  </p>
                  <p>
                    <strong>
                      <FormattedMessage id="pharmaceutical.excursion.threshold" />
                      :
                    </strong>{" "}
                    {selectedExcursion.thresholdValue}
                  </p>
                  <p>
                    <strong>
                      <FormattedMessage id="pharmaceutical.excursion.status" />:
                    </strong>{" "}
                    {getStatusTag(selectedExcursion.status)}
                  </p>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <h5 style={{ marginBottom: "1rem" }}>
                    <FormattedMessage id="pharmaceutical.report.timeline" />
                  </h5>
                  <p>
                    <strong>
                      <FormattedMessage id="pharmaceutical.report.detectedAt" />
                      :
                    </strong>{" "}
                    {selectedExcursion.detectedAt
                      ? new Date(selectedExcursion.detectedAt).toLocaleString()
                      : "--"}
                  </p>
                  <p>
                    <strong>
                      <FormattedMessage id="pharmaceutical.report.acknowledgedAt" />
                      :
                    </strong>{" "}
                    {selectedExcursion.acknowledgedAt
                      ? new Date(
                          selectedExcursion.acknowledgedAt,
                        ).toLocaleString()
                      : "--"}
                  </p>
                  <p>
                    <strong>
                      <FormattedMessage id="pharmaceutical.report.resolvedAt" />
                      :
                    </strong>{" "}
                    {selectedExcursion.resolvedAt
                      ? new Date(selectedExcursion.resolvedAt).toLocaleString()
                      : "--"}
                  </p>
                  <p>
                    <strong>
                      <FormattedMessage id="pharmaceutical.report.duration" />:
                    </strong>{" "}
                    {formatDuration(selectedExcursion.durationMinutes)}
                  </p>
                </Column>
              </Grid>

              <Accordion style={{ marginTop: "1rem" }}>
                <AccordionItem
                  title={intl.formatMessage({
                    id: "pharmaceutical.report.resolutionDetails",
                  })}
                >
                  <p>
                    <strong>
                      <FormattedMessage id="pharmaceutical.report.acknowledgedBy" />
                      :
                    </strong>{" "}
                    {selectedExcursion.acknowledgedBy || "--"}
                  </p>
                  <p>
                    <strong>
                      <FormattedMessage id="pharmaceutical.report.acknowledgmentNotes" />
                      :
                    </strong>{" "}
                    {selectedExcursion.acknowledgmentNotes || "--"}
                  </p>
                  <p>
                    <strong>
                      <FormattedMessage id="pharmaceutical.report.resolvedBy" />
                      :
                    </strong>{" "}
                    {selectedExcursion.resolvedBy || "--"}
                  </p>
                  <p>
                    <strong>
                      <FormattedMessage id="pharmaceutical.excursion.resolve.notes" />
                      :
                    </strong>{" "}
                    {selectedExcursion.resolutionNotes || "--"}
                  </p>
                  {selectedExcursion.escalationReason && (
                    <p>
                      <strong>
                        <FormattedMessage id="pharmaceutical.report.escalationReason" />
                        :
                      </strong>{" "}
                      {selectedExcursion.escalationReason}
                    </p>
                  )}
                </AccordionItem>

                <AccordionItem
                  title={`${intl.formatMessage({ id: "pharmaceutical.report.affectedSamples" })} (${affectedSamples.length})`}
                >
                  {affectedSamples.length > 0 ? (
                    <div style={{ maxHeight: "200px", overflowY: "auto" }}>
                      {affectedSamples.map((sample, idx) => (
                        <Tile
                          key={idx}
                          style={{
                            marginBottom: "0.5rem",
                            padding: "0.5rem",
                          }}
                        >
                          <strong>{sample.sampleName}</strong>
                          <br />
                          <small>
                            ID: {sample.sampleId} | Lot:{" "}
                            {sample.lotNumber || "--"} | Status: {sample.status}
                          </small>
                        </Tile>
                      ))}
                    </div>
                  ) : (
                    <p>
                      <FormattedMessage id="pharmaceutical.report.noAffectedSamples" />
                    </p>
                  )}
                </AccordionItem>
              </Accordion>
            </div>
          )}
        </Modal>
      </Column>
    </Grid>
  );
};

export default ExcursionReport;

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
  Tag,
} from "@carbon/react";
import {
  Dashboard,
  WarningAlt,
  Checkmark,
  Time,
  Download,
  ChartPie,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";
import {
  PieChart,
  DonutChart,
  SimpleBarChart,
  StackedBarChart,
} from "@carbon/charts-react";
import "@carbon/charts/styles.css";

const PharmaceuticalDashboard = () => {
  const intl = useIntl();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [dashboardData, setDashboardData] = useState(null);
  const [dateRange, setDateRange] = useState({
    startDate: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000),
    endDate: new Date(),
  });

  const fetchDashboardData = useCallback(() => {
    setLoading(true);
    getFromOpenElisServer(
      "/rest/pharmaceutical/reports/dashboard",
      (response) => {
        if (response) {
          setDashboardData(response);
        } else {
          setError("Failed to load dashboard data");
        }
        setLoading(false);
      },
    );
  }, []);

  useEffect(() => {
    fetchDashboardData();
  }, [fetchDashboardData]);

  const handleExportCSV = (reportType) => {
    const startDate = dateRange.startDate.getTime();
    const endDate = dateRange.endDate.getTime();
    window.open(
      `/rest/pharmaceutical/reports/export/${reportType}/csv?startDate=${startDate}&endDate=${endDate}`,
      "_blank",
    );
  };

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

  const AlertTile = ({ title, count, type, items }) => (
    <Tile
      className={`alert-tile alert-tile--${type}`}
      style={{
        borderLeft: `4px solid ${
          type === "critical"
            ? "#da1e28"
            : type === "warning"
              ? "#f1c21b"
              : "#0043ce"
        }`,
      }}
    >
      <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
        <WarningAlt
          size={20}
          style={{
            color:
              type === "critical"
                ? "#da1e28"
                : type === "warning"
                  ? "#f1c21b"
                  : "#0043ce",
          }}
        />
        <strong>{title}</strong>
      </div>
      <p style={{ fontSize: "1.5rem", fontWeight: "600" }}>{count}</p>
      {items && items.length > 0 && (
        <ul style={{ margin: 0, paddingLeft: "1rem" }}>
          {items.slice(0, 3).map((item, idx) => (
            <li key={idx} style={{ fontSize: "0.75rem" }}>
              {item}
            </li>
          ))}
        </ul>
      )}
    </Tile>
  );

  if (loading) {
    return <Loading />;
  }

  const sampleStatusData = dashboardData?.intake?.byLabType
    ? Object.entries(dashboardData.intake.byLabType).map(([key, value]) => ({
        group: key,
        value: value,
      }))
    : [];

  const qcPassFailData = dashboardData?.qc
    ? [
        { group: "Pass", value: dashboardData.qc.passCount || 0 },
        { group: "Fail", value: dashboardData.qc.failCount || 0 },
      ]
    : [];

  const assayTypeData = dashboardData?.assays?.byType
    ? Object.entries(dashboardData.assays.byType).map(([key, value]) => ({
        group: key,
        value: value,
      }))
    : [];

  const excursionStatusData = dashboardData?.excursions?.byStatus
    ? Object.entries(dashboardData.excursions.byStatus).map(([key, value]) => ({
        group: key,
        value: value,
      }))
    : [];

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
            <FormattedMessage id="pharmaceutical.dashboard.title" />
          </h3>
          <Button kind="ghost" onClick={fetchDashboardData}>
            <FormattedMessage id="label.refresh" />
          </Button>
        </div>

        {/* Alert Section */}
        <Grid style={{ marginBottom: "1.5rem" }}>
          <Column lg={4} md={4} sm={4}>
            <AlertTile
              title={intl.formatMessage({
                id: "pharmaceutical.dashboard.activeExcursions",
              })}
              count={dashboardData?.excursions?.currentlyActive || 0}
              type={
                dashboardData?.excursions?.currentlyActive > 0
                  ? "critical"
                  : "info"
              }
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <AlertTile
              title={intl.formatMessage({
                id: "pharmaceutical.dashboard.pendingReview",
              })}
              count={dashboardData?.pendingReviewCount || 0}
              type={dashboardData?.pendingReviewCount > 5 ? "warning" : "info"}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <AlertTile
              title={intl.formatMessage({
                id: "pharmaceutical.dashboard.pendingDisposal",
              })}
              count={dashboardData?.pendingDisposalApprovalCount || 0}
              type="info"
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <AlertTile
              title={intl.formatMessage({
                id: "pharmaceutical.dashboard.expiringSoon",
              })}
              count={dashboardData?.storage?.expiringSoon30Days || 0}
              type={
                dashboardData?.storage?.expiringSoon30Days > 0
                  ? "warning"
                  : "info"
              }
            />
          </Column>
        </Grid>

        {/* Key Metrics Section */}
        <h4 style={{ marginBottom: "1rem" }}>
          <FormattedMessage id="pharmaceutical.dashboard.keyMetrics" />
        </h4>
        <Grid style={{ marginBottom: "1.5rem" }}>
          <Column lg={4} md={4} sm={4}>
            <MetricTile
              title={intl.formatMessage({
                id: "pharmaceutical.dashboard.totalIntake",
              })}
              value={dashboardData?.intake?.totalIntake || 0}
              icon={Dashboard}
              color="#0043ce"
              subtitle={intl.formatMessage({
                id: "pharmaceutical.dashboard.last30days",
              })}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <MetricTile
              title={intl.formatMessage({
                id: "pharmaceutical.dashboard.qcPassRate",
              })}
              value={`${dashboardData?.qc?.passRate || 0}%`}
              icon={Checkmark}
              color="#24a148"
              subtitle={intl.formatMessage({
                id: "pharmaceutical.dashboard.last30days",
              })}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <MetricTile
              title={intl.formatMessage({
                id: "pharmaceutical.dashboard.oosRate",
              })}
              value={`${dashboardData?.oos?.oosRate || 0}%`}
              icon={WarningAlt}
              color={dashboardData?.oos?.oosRate > 5 ? "#da1e28" : "#f1c21b"}
              subtitle={intl.formatMessage({
                id: "pharmaceutical.dashboard.last30days",
              })}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <MetricTile
              title={intl.formatMessage({
                id: "pharmaceutical.dashboard.avgTAT",
              })}
              value={`${dashboardData?.tat?.averageTATHours || 0}h`}
              icon={Time}
              color="#0043ce"
              subtitle={
                intl.formatMessage({
                  id: "pharmaceutical.dashboard.slaCompliance",
                }) + `: ${dashboardData?.tat?.slaComplianceRate || 0}%`
              }
            />
          </Column>
        </Grid>

        {/* Storage Metrics */}
        <h4 style={{ marginBottom: "1rem" }}>
          <FormattedMessage id="pharmaceutical.dashboard.storageMetrics" />
        </h4>
        <Grid style={{ marginBottom: "1.5rem" }}>
          <Column lg={4} md={4} sm={4}>
            <MetricTile
              title={intl.formatMessage({
                id: "pharmaceutical.dashboard.samplesInStorage",
              })}
              value={dashboardData?.storage?.totalInStorage || 0}
              color="#0043ce"
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <MetricTile
              title={intl.formatMessage({
                id: "pharmaceutical.dashboard.availableAliquots",
              })}
              value={dashboardData?.storage?.availableAliquots || 0}
              color="#0043ce"
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <MetricTile
              title={intl.formatMessage({
                id: "pharmaceutical.dashboard.freezeThawExceeded",
              })}
              value={dashboardData?.storage?.freezeThawExceeded || 0}
              color={
                dashboardData?.storage?.freezeThawExceeded > 0
                  ? "#da1e28"
                  : "#24a148"
              }
            />
          </Column>
        </Grid>

        {/* Charts Section */}
        <h4 style={{ marginBottom: "1rem" }}>
          <FormattedMessage id="pharmaceutical.dashboard.charts" />
        </h4>
        <Grid>
          <Column lg={8} md={4} sm={4}>
            <Tile>
              <h5>
                <FormattedMessage id="pharmaceutical.dashboard.samplesByLabType" />
              </h5>
              {sampleStatusData.length > 0 ? (
                <DonutChart
                  data={sampleStatusData}
                  options={{
                    title: "",
                    resizable: true,
                    height: "300px",
                    donut: {
                      center: {
                        label: intl.formatMessage({
                          id: "pharmaceutical.dashboard.samples",
                        }),
                      },
                    },
                    legend: {
                      alignment: "center",
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
                <FormattedMessage id="pharmaceutical.dashboard.qcResults" />
              </h5>
              {qcPassFailData.some((d) => d.value > 0) ? (
                <PieChart
                  data={qcPassFailData}
                  options={{
                    title: "",
                    resizable: true,
                    height: "300px",
                    pie: {
                      alignment: "center",
                    },
                    legend: {
                      alignment: "center",
                    },
                    color: {
                      scale: {
                        Pass: "#24a148",
                        Fail: "#da1e28",
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

        <Grid style={{ marginTop: "1rem" }}>
          <Column lg={8} md={4} sm={4}>
            <Tile>
              <h5>
                <FormattedMessage id="pharmaceutical.dashboard.assaysByType" />
              </h5>
              {assayTypeData.length > 0 ? (
                <SimpleBarChart
                  data={assayTypeData}
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
                    height: "300px",
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
                <FormattedMessage id="pharmaceutical.dashboard.excursionsByStatus" />
              </h5>
              {excursionStatusData.length > 0 ? (
                <DonutChart
                  data={excursionStatusData}
                  options={{
                    title: "",
                    resizable: true,
                    height: "300px",
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
        </Grid>

        {/* Export Section */}
        <h4 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
          <FormattedMessage id="pharmaceutical.dashboard.exportReports" />
        </h4>
        <Grid>
          <Column lg={8} md={4} sm={4}>
            <Tile>
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                }}
              >
                <div>
                  <h5>
                    <FormattedMessage id="pharmaceutical.dashboard.excursionReport" />
                  </h5>
                  <p style={{ fontSize: "0.875rem", color: "#525252" }}>
                    <FormattedMessage id="pharmaceutical.dashboard.excursionReport.description" />
                  </p>
                </div>
                <Button
                  kind="tertiary"
                  size="sm"
                  renderIcon={Download}
                  onClick={() => handleExportCSV("excursions")}
                >
                  <FormattedMessage id="label.exportCSV" />
                </Button>
              </div>
            </Tile>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Tile>
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                }}
              >
                <div>
                  <h5>
                    <FormattedMessage id="pharmaceutical.dashboard.disposalReport" />
                  </h5>
                  <p style={{ fontSize: "0.875rem", color: "#525252" }}>
                    <FormattedMessage id="pharmaceutical.dashboard.disposalReport.description" />
                  </p>
                </div>
                <Button
                  kind="tertiary"
                  size="sm"
                  renderIcon={Download}
                  onClick={() => handleExportCSV("disposal")}
                >
                  <FormattedMessage id="label.exportCSV" />
                </Button>
              </div>
            </Tile>
          </Column>
        </Grid>
      </Column>
    </Grid>
  );
};

export default PharmaceuticalDashboard;

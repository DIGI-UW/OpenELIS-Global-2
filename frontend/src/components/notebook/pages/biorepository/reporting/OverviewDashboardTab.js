import React, { useState, useEffect } from "react";
import {
  Grid,
  Column,
  Loading,
  InlineNotification,
  Tile,
  Button,
  InlineLoading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { Download } from "@carbon/icons-react";
import PropTypes from "prop-types";
import config from "../../../../../config.json";

const parseApiErrorMessage = async (response, fallback) => {
  try {
    const data = await response.json();
    if (data?.error) {
      return data.error;
    }
  } catch {
    // Response body was not JSON
  }
  return fallback;
};

const fetchDashboardJson = async (url, fallbackMessage, signal) => {
  const response = await fetch(url, { credentials: "include", signal });
  if (!response.ok) {
    throw new Error(await parseApiErrorMessage(response, fallbackMessage));
  }
  return response.json();
};

/**
 * OverviewDashboardTab - High-level KPI metrics for biorepository dashboard
 *
 * Displays:
 * - 4 KPI cards: Total Samples Stored, Freezer Utilization, QC Compliance, Samples Expiring Soon
 * - Key metrics aggregated from multiple dashboard endpoints
 *
 * Pattern: Follows FreezerMonitoringDashboard structure with Promise.all for parallel fetching
 */
function OverviewDashboardTab({ entryId, notebookId, pageData }) {
  const intl = useIntl();
  const [isLoading, setIsLoading] = useState(true);
  const [dashboardData, setDashboardData] = useState(null);
  const [error, setError] = useState(null);
  const [csvLoading, setCsvLoading] = useState(false);
  const [csvError, setCsvError] = useState(null);
  const [notificationCopyState, setNotificationCopyState] = useState(null);

  useEffect(() => {
    const controller = new AbortController();

    setIsLoading(true);
    setError(null);

    // Fetch all dashboard metrics in parallel
    Promise.all([
      fetchDashboardJson(
        `${config.serverBaseUrl}/rest/biorepository/dashboard/storage-capacity`,
        "Failed to load storage capacity",
        controller.signal,
      ),
      fetchDashboardJson(
        `${config.serverBaseUrl}/rest/biorepository/dashboard/sample-aging`,
        "Failed to load sample aging",
        controller.signal,
      ),
      fetchDashboardJson(
        `${config.serverBaseUrl}/rest/biorepository/dashboard/qc-metrics`,
        "Failed to load QC metrics",
        controller.signal,
      ),
      fetchDashboardJson(
        `${config.serverBaseUrl}/rest/biorepository/dashboard/retrieval-stats`,
        "Failed to load retrieval stats",
        controller.signal,
      ),
    ])
      .then(([capacity, aging, qc, retrieval]) => {
        setDashboardData({ capacity, aging, qc, retrieval });
        setIsLoading(false);
      })
      .catch((err) => {
        if (err.name !== "AbortError") {
          setError(err.message);
          setIsLoading(false);
        }
      });

    return () => controller.abort();
  }, []);

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

  const handleExportCsv = () => {
    setCsvLoading(true);
    setCsvError(null);
    const stamp = new Date().toISOString().split("T")[0];
    fetch(`${config.serverBaseUrl}/rest/biorepository/dashboard/export/csv`, {
      credentials: "include",
      method: "GET",
    })
      .then(async (response) => {
        if (!response.ok) {
          const fallback = `Export failed: ${response.status} ${response.statusText}`;
          throw new Error(await parseApiErrorMessage(response, fallback));
        }
        return response.blob();
      })
      .then((blob) => {
        downloadBlob(blob, `biorepository_dashboard_${stamp}.csv`);
        setCsvLoading(false);
      })
      .catch((err) => {
        setCsvError(err.message);
        setCsvLoading(false);
      });
  };

  const handleCopyNotification = async () => {
    const message =
      dashboardData?.qc?.escalationSignals?.supervisorNotificationMessage;
    if (!message) {
      setNotificationCopyState("error");
      return;
    }
    try {
      await navigator.clipboard.writeText(message);
      setNotificationCopyState("success");
    } catch (e) {
      setNotificationCopyState("error");
    }
  };

  if (isLoading) {
    return (
      <div style={{ padding: "2rem" }}>
        <Loading
          description={intl.formatMessage({
            id: "biorepository.reporting.loading",
            defaultMessage: "Loading dashboard metrics...",
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
            id: "biorepository.reporting.error",
            defaultMessage: "Error Loading Dashboard",
          })}
          subtitle={error}
          lowContrast
        />
      </div>
    );
  }

  if (!dashboardData) {
    return (
      <div style={{ padding: "2rem" }}>
        <InlineNotification
          kind="warning"
          title={intl.formatMessage({
            id: "biorepository.reporting.noData",
            defaultMessage: "No Data Available",
          })}
          subtitle={intl.formatMessage({
            id: "biorepository.reporting.noDataDescription",
            defaultMessage: "Dashboard data could not be loaded.",
          })}
          lowContrast
        />
      </div>
    );
  }

  const { capacity, aging, qc, retrieval } = dashboardData;
  const escalationSignals = qc?.escalationSignals || {};
  const triggeredRules = Array.isArray(escalationSignals.triggeredRules)
    ? escalationSignals.triggeredRules
    : [];
  const recommendedActions = Array.isArray(escalationSignals.recommendedActions)
    ? escalationSignals.recommendedActions
    : [];

  return (
    <div className="overview-dashboard-tab" style={{ padding: "1.5rem" }}>
      <Grid fullWidth>
        {/* Page Header */}
        <Column lg={16} md={8} sm={4}>
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginBottom: "1.5rem",
              gap: "1rem",
              flexWrap: "wrap",
            }}
          >
            <h4 style={{ marginBottom: 0 }}>
              <FormattedMessage
                id="biorepository.reporting.overview.title"
                defaultMessage="Biorepository Overview Dashboard"
              />
            </h4>
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: "0.75rem",
                flexWrap: "wrap",
              }}
            >
              {csvLoading ? (
                <InlineLoading
                  description={intl.formatMessage({
                    id: "biorepository.reporting.overview.csv.loading",
                    defaultMessage: "Preparing CSV export...",
                  })}
                />
              ) : (
                <Button
                  kind="primary"
                  size="sm"
                  renderIcon={Download}
                  onClick={handleExportCsv}
                >
                  <FormattedMessage
                    id="biorepository.reporting.overview.csv.export"
                    defaultMessage="Export CSV"
                  />
                </Button>
              )}
              <Button kind="secondary" size="sm" onClick={() => window.print()}>
                <FormattedMessage
                  id="biorepository.reporting.overview.print"
                  defaultMessage="Print Dashboard"
                />
              </Button>
            </div>
          </div>
          {csvError && (
            <InlineNotification
              kind="error"
              title={intl.formatMessage({
                id: "biorepository.reporting.overview.csv.error.title",
                defaultMessage: "CSV Export Failed",
              })}
              subtitle={csvError}
              lowContrast
              onClose={() => setCsvError(null)}
              style={{ marginBottom: "1rem" }}
            />
          )}
          {notificationCopyState === "success" && (
            <InlineNotification
              kind="success"
              title={intl.formatMessage({
                id: "biorepository.reporting.overview.notification.copy.success.title",
                defaultMessage: "Supervisor Alert Copied",
              })}
              subtitle={intl.formatMessage({
                id: "biorepository.reporting.overview.notification.copy.success.subtitle",
                defaultMessage:
                  "Escalation notification text copied. Paste it to your supervisor channel.",
              })}
              lowContrast
              onClose={() => setNotificationCopyState(null)}
              style={{ marginBottom: "1rem" }}
            />
          )}
          {notificationCopyState === "error" && (
            <InlineNotification
              kind="warning"
              title={intl.formatMessage({
                id: "biorepository.reporting.overview.notification.copy.error.title",
                defaultMessage: "Could Not Copy Alert",
              })}
              subtitle={intl.formatMessage({
                id: "biorepository.reporting.overview.notification.copy.error.subtitle",
                defaultMessage:
                  "Copy the escalation text manually from the Escalation Summary section.",
              })}
              lowContrast
              onClose={() => setNotificationCopyState(null)}
              style={{ marginBottom: "1rem" }}
            />
          )}
        </Column>

        {/* KPI Cards Row */}
        <Column lg={4} md={4} sm={4}>
          <Tile
            style={{
              padding: "1.5rem",
              backgroundColor: "#e7f1f5",
              borderLeft: "4px solid #0043ce",
              marginBottom: "1rem",
            }}
          >
            <p
              style={{
                fontSize: "0.875rem",
                color: "#525252",
                margin: 0,
                textTransform: "uppercase",
              }}
            >
              <FormattedMessage
                id="biorepository.reporting.kpi.samplesStored"
                defaultMessage="Total Samples Stored"
              />
            </p>
            <h2
              style={{
                fontSize: "2.5rem",
                fontWeight: "bold",
                color: "#161616",
                margin: "0.5rem 0 0 0",
              }}
            >
              {capacity?.totalSamplesStored || 0}
            </h2>
            <p
              style={{
                fontSize: "0.75rem",
                color: "#525252",
                marginTop: "0.5rem",
              }}
            >
              <FormattedMessage
                id="biorepository.reporting.kpi.pendingStorage"
                defaultMessage="{count} pending storage assignment"
                values={{ count: capacity?.pendingStorage || 0 }}
              />
            </p>
          </Tile>
        </Column>

        <Column lg={4} md={4} sm={4}>
          <Tile
            style={{
              padding: "1.5rem",
              backgroundColor: "#e5f6e3",
              borderLeft: "4px solid #24a148",
              marginBottom: "1rem",
            }}
          >
            <p
              style={{
                fontSize: "0.875rem",
                color: "#525252",
                margin: 0,
                textTransform: "uppercase",
              }}
            >
              <FormattedMessage
                id="biorepository.reporting.kpi.qcCompliance"
                defaultMessage="QC Compliance Rate"
              />
            </p>
            <h2
              style={{
                fontSize: "2.5rem",
                fontWeight: "bold",
                color: "#161616",
                margin: "0.5rem 0 0 0",
              }}
            >
              {Number.isFinite(Number(qc?.passRate ?? qc?.complianceRate))
                ? `${Number(qc?.passRate ?? qc?.complianceRate).toFixed(1)}%`
                : "N/A"}
            </h2>
            <p
              style={{
                fontSize: "0.75rem",
                color: "#525252",
                marginTop: "0.5rem",
              }}
            >
              <FormattedMessage
                id="biorepository.reporting.kpi.qcInspections"
                defaultMessage="{passed} of {total} inspections passed | {failed} failed"
                values={{
                  passed: qc?.passedInspections || 0,
                  total: qc?.totalInspections || 0,
                  failed: (qc?.failCount ?? qc?.failedInspections) || 0,
                }}
              />
            </p>
            <p
              style={{
                fontSize: "0.75rem",
                color: triggeredRules.length ? "#da1e28" : "#525252",
                marginTop: "0.25rem",
              }}
            >
              {triggeredRules.length
                ? `Escalation: ${triggeredRules.join(", ")}`
                : "Escalation: none triggered"}
            </p>
          </Tile>
        </Column>

        <Column lg={4} md={4} sm={4}>
          <Tile
            style={{
              padding: "1.5rem",
              backgroundColor: "#fff3e0",
              borderLeft: "4px solid #ff832b",
              marginBottom: "1rem",
            }}
          >
            <p
              style={{
                fontSize: "0.875rem",
                color: "#525252",
                margin: 0,
                textTransform: "uppercase",
              }}
            >
              <FormattedMessage
                id="biorepository.reporting.kpi.expiringSoon"
                defaultMessage="Expiring Soon"
              />
            </p>
            <h2
              style={{
                fontSize: "2.5rem",
                fontWeight: "bold",
                color: "#161616",
                margin: "0.5rem 0 0 0",
              }}
            >
              {aging?.expiring30Days || 0}
            </h2>
            <p
              style={{
                fontSize: "0.75rem",
                color: "#525252",
                marginTop: "0.5rem",
              }}
            >
              <FormattedMessage
                id="biorepository.reporting.kpi.expiring30Days"
                defaultMessage="Expiring within 30 days"
              />
            </p>
          </Tile>
        </Column>

        <Column lg={4} md={4} sm={4}>
          <Tile
            style={{
              padding: "1.5rem",
              backgroundColor: "#fce4ec",
              borderLeft: "4px solid #da1e28",
              marginBottom: "1rem",
            }}
          >
            <p
              style={{
                fontSize: "0.875rem",
                color: "#525252",
                margin: 0,
                textTransform: "uppercase",
              }}
            >
              <FormattedMessage
                id="biorepository.reporting.kpi.expired"
                defaultMessage="Expired Samples"
              />
            </p>
            <h2
              style={{
                fontSize: "2.5rem",
                fontWeight: "bold",
                color: "#161616",
                margin: "0.5rem 0 0 0",
              }}
            >
              {aging?.expired || 0}
            </h2>
            <p
              style={{
                fontSize: "0.75rem",
                color: "#525252",
                marginTop: "0.5rem",
              }}
            >
              <FormattedMessage
                id="biorepository.reporting.kpi.expiredDescription"
                defaultMessage="Past retention expiry date"
              />
            </p>
          </Tile>
        </Column>

        {/* Summary Section */}
        <Column lg={16} md={8} sm={4} style={{ marginTop: "2rem" }}>
          <Tile style={{ padding: "1.5rem" }}>
            <h5 style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="biorepository.reporting.overview.summary"
                defaultMessage="System Summary"
              />
            </h5>
            <Grid fullWidth>
              <Column lg={8} md={4} sm={4}>
                <p style={{ fontSize: "0.875rem", marginBottom: "0.5rem" }}>
                  <strong>
                    <FormattedMessage
                      id="biorepository.reporting.summary.agingBreakdown"
                      defaultMessage="Sample Aging Breakdown:"
                    />
                  </strong>
                </p>
                <ul style={{ paddingLeft: "1.5rem" }}>
                  <li>
                    <FormattedMessage
                      id="biorepository.reporting.summary.expiring60"
                      defaultMessage="Expiring 31-60 days: {count}"
                      values={{ count: aging?.expiring60Days || 0 }}
                    />
                  </li>
                  <li>
                    <FormattedMessage
                      id="biorepository.reporting.summary.expiring90"
                      defaultMessage="Expiring 61-90 days: {count}"
                      values={{ count: aging?.expiring90Days || 0 }}
                    />
                  </li>
                  <li>
                    <FormattedMessage
                      id="biorepository.reporting.summary.totalActive"
                      defaultMessage="Total active samples: {count}"
                      values={{ count: aging?.total || 0 }}
                    />
                  </li>
                </ul>
                <p style={{ fontSize: "0.875rem", marginBottom: "0.5rem" }}>
                  {`Storage utilization (weighted): ${
                    Number.isFinite(Number(capacity?.averageUtilization))
                      ? Number(capacity.averageUtilization).toFixed(1)
                      : "0.0"
                  }% across ${capacity?.totalDevices || 0} active devices`}
                </p>
              </Column>
              <Column lg={8} md={4} sm={4}>
                <p style={{ fontSize: "0.875rem", marginBottom: "0.5rem" }}>
                  <strong>
                    <FormattedMessage
                      id="biorepository.reporting.summary.retrievalStats"
                      defaultMessage="Retrieval Statistics (Last 30 Days):"
                    />
                  </strong>
                </p>
                <ul style={{ paddingLeft: "1.5rem" }}>
                  <li>
                    <FormattedMessage
                      id="biorepository.reporting.summary.totalRequests"
                      defaultMessage="Total requests: {count}"
                      values={{ count: retrieval?.totalRequests || 0 }}
                    />
                  </li>
                  <li>
                    <FormattedMessage
                      id="biorepository.reporting.summary.completedRequests"
                      defaultMessage="Completed: {count}"
                      values={{ count: retrieval?.completedRequests || 0 }}
                    />
                  </li>
                  <li>
                    <FormattedMessage
                      id="biorepository.reporting.summary.pendingRequests"
                      defaultMessage="Pending: {count}"
                      values={{ count: retrieval?.pendingRequests || 0 }}
                    />
                  </li>
                  <li>
                    <FormattedMessage
                      id="biorepository.reporting.summary.overdueReturns"
                      defaultMessage="Overdue returns: {count}"
                      values={{ count: retrieval?.overdueReturns || 0 }}
                    />
                  </li>
                  <li>
                    {`QC fail rate: ${Number.isFinite(Number(escalationSignals.batchFailRatePercent)) ? Number(escalationSignals.batchFailRatePercent).toFixed(1) : "N/A"}% (threshold ${Number.isFinite(Number(escalationSignals.batchFailRateThresholdPercent)) ? Number(escalationSignals.batchFailRateThresholdPercent).toFixed(1) : "5.0"}%)`}
                  </li>
                  <li>
                    {`Repeated failure flags (box/rack): ${
                      escalationSignals.repeatedFailureInSameBoxOrRack
                        ? "Yes"
                        : "No"
                    }`}
                  </li>
                  <li>
                    {`Flagged freezers: ${
                      Array.isArray(escalationSignals.flaggedFreezers)
                        ? escalationSignals.flaggedFreezers.length
                        : 0
                    }`}
                  </li>
                  <li>
                    {`Supervisor notification required: ${
                      escalationSignals.supervisorNotificationRequired
                        ? "Yes"
                        : "No"
                    }`}
                  </li>
                  {recommendedActions.length > 0 && (
                    <li>{`Recommended actions: ${recommendedActions.join("; ")}`}</li>
                  )}
                </ul>
                {escalationSignals.supervisorNotificationRequired &&
                  escalationSignals.supervisorNotificationMessage && (
                    <div style={{ marginTop: "0.75rem" }}>
                      <p style={{ marginBottom: "0.5rem", fontWeight: 600 }}>
                        <FormattedMessage
                          id="biorepository.reporting.overview.notification.preview.title"
                          defaultMessage="Supervisor alert preview"
                        />
                      </p>
                      <p style={{ marginBottom: "0.75rem" }}>
                        {escalationSignals.supervisorNotificationMessage}
                      </p>
                      <Button
                        kind="tertiary"
                        size="sm"
                        onClick={handleCopyNotification}
                      >
                        <FormattedMessage
                          id="biorepository.reporting.overview.notification.copy"
                          defaultMessage="Copy Supervisor Alert"
                        />
                      </Button>
                    </div>
                  )}
              </Column>
            </Grid>
          </Tile>
        </Column>
      </Grid>
    </div>
  );
}

OverviewDashboardTab.propTypes = {
  entryId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  notebookId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  pageData: PropTypes.object,
};

export default OverviewDashboardTab;

import React, { useState, useEffect } from "react";
import { Grid, Column, Loading, InlineNotification, Tile } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";
import config from "../../../../../config.json";

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

  useEffect(() => {
    const controller = new AbortController();

    setIsLoading(true);
    setError(null);

    // Fetch all dashboard metrics in parallel
    Promise.all([
      fetch(
        `${config.serverBaseUrl}/rest/biorepository/dashboard/storage-capacity`,
        {
          credentials: "include",
          signal: controller.signal,
        },
      ).then((r) => {
        if (!r.ok) throw new Error("Failed to load storage capacity");
        return r.json();
      }),
      fetch(
        `${config.serverBaseUrl}/rest/biorepository/dashboard/sample-aging`,
        {
          credentials: "include",
          signal: controller.signal,
        },
      ).then((r) => {
        if (!r.ok) throw new Error("Failed to load sample aging");
        return r.json();
      }),
      fetch(`${config.serverBaseUrl}/rest/biorepository/dashboard/qc-metrics`, {
        credentials: "include",
        signal: controller.signal,
      }).then((r) => {
        if (!r.ok) throw new Error("Failed to load QC metrics");
        return r.json();
      }),
      fetch(
        `${config.serverBaseUrl}/rest/biorepository/dashboard/retrieval-stats`,
        {
          credentials: "include",
          signal: controller.signal,
        },
      ).then((r) => {
        if (!r.ok) throw new Error("Failed to load retrieval stats");
        return r.json();
      }),
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

  return (
    <div className="overview-dashboard-tab" style={{ padding: "1.5rem" }}>
      <Grid fullWidth>
        {/* Page Header */}
        <Column lg={16} md={8} sm={4}>
          <h4 style={{ marginBottom: "1.5rem" }}>
            <FormattedMessage
              id="biorepository.reporting.overview.title"
              defaultMessage="Biorepository Overview Dashboard"
            />
          </h4>
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
              {qc?.complianceRate ? `${qc.complianceRate.toFixed(1)}%` : "N/A"}
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
                defaultMessage="{passed} of {total} inspections passed"
                values={{
                  passed: qc?.passedInspections || 0,
                  total: qc?.totalInspections || 0,
                }}
              />
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
                </ul>
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

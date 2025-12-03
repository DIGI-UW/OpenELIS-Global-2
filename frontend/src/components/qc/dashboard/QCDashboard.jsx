/**
 * QCDashboard Component
 *
 * Main dashboard showing compliance status for all analyzers
 * Task Reference: T123
 * Specification: FR-046 to FR-051, User Story 1
 *
 * Features:
 * - Compliance status tiles for each analyzer (green/yellow/red)
 * - Alert feed in top-right corner
 * - Auto-refresh at configurable intervals (default: 5 minutes)
 * - Last update timestamp display
 */

import React, { useState, useEffect, useCallback } from "react";
import {
  Grid,
  Column,
  Loading,
  InlineNotification,
  Button,
} from "@carbon/react";
import { Renew } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import { getFromOpenElisServer } from "../../utils/Utils";
import ComplianceStatusTile from "./ComplianceStatusTile";
import AlertFeed from "../alerts/AlertFeed";
import PageTitle from "../../common/PageTitle/PageTitle";
import "./QCDashboard.css";

const QCDashboard = () => {
  const intl = useIntl();
  const history = useHistory();

  // State
  const [analyzers, setAnalyzers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [refreshInterval] = useState(5 * 60 * 1000); // 5 minutes (FR-050)

  // Load dashboard data
  const loadDashboardData = useCallback(() => {
    setLoading(true);
    setError(null);

    getFromOpenElisServer("/rest/qc/dashboard", (response) => {
      if (response && response.data) {
        setAnalyzers(response.data.analyzers || []);
        setLastUpdated(new Date());
      } else if (Array.isArray(response)) {
        setAnalyzers(response);
        setLastUpdated(new Date());
      } else {
        setError(
          intl.formatMessage({ id: "qc.dashboard.error.loadFailed" }),
        );
      }
      setLoading(false);
    });
  }, [intl]);

  // Initial load and auto-refresh
  useEffect(() => {
    loadDashboardData();

    // Set up auto-refresh (FR-050)
    const intervalId = setInterval(loadDashboardData, refreshInterval);

    return () => clearInterval(intervalId);
  }, [loadDashboardData, refreshInterval]);

  // Handle analyzer click - navigate to control chart detail
  const handleAnalyzerClick = (analyzerId) => {
    history.push(`/analyzers/qc/charts/${analyzerId}`);
  };

  // Handle manual refresh
  const handleRefresh = () => {
    loadDashboardData();
  };

  // Format last updated timestamp
  const formatLastUpdated = () => {
    if (!lastUpdated) return "";
    return intl.formatDate(lastUpdated, {
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    });
  };

  if (loading && analyzers.length === 0) {
    return (
      <div className="qc-dashboard-loading" data-testid="qc-dashboard-loading">
        <Loading
          description={intl.formatMessage({ id: "qc.dashboard.loading" })}
          withOverlay={false}
        />
      </div>
    );
  }

  return (
    <div className="qc-dashboard" data-testid="qc-dashboard">
      {/* Header */}
      <div className="qc-dashboard-header" data-testid="qc-dashboard-header">
        <div className="qc-dashboard-header-title">
          <PageTitle
            breadcrumbs={[
              {
                label: intl.formatMessage({ id: "analyzer.page.hierarchy.root" }),
                link: "/analyzers",
              },
              {
                label: intl.formatMessage({ id: "qc.dashboard.title" }),
              },
            ]}
            subtitle={intl.formatMessage({ id: "qc.dashboard.subtitle" })}
          />
        </div>
        <div className="qc-dashboard-header-actions">
          <span className="qc-dashboard-last-updated" data-testid="qc-dashboard-last-updated">
            {intl.formatMessage({ id: "qc.dashboard.lastUpdated" })}: {formatLastUpdated()}
          </span>
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Renew}
            iconDescription={intl.formatMessage({ id: "qc.dashboard.refresh" })}
            onClick={handleRefresh}
            data-testid="qc-dashboard-refresh-button"
          >
            {intl.formatMessage({ id: "qc.dashboard.refresh" })}
          </Button>
        </div>
      </div>

      {/* Error notification */}
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({ id: "qc.dashboard.error.title" })}
          subtitle={error}
          onClose={() => setError(null)}
          data-testid="qc-dashboard-error"
        />
      )}

      {/* Main content area with Alert Feed */}
      <Grid className="qc-dashboard-content">
        {/* Analyzer compliance tiles */}
        <Column lg={12} md={6} sm={4}>
          <div className="qc-dashboard-analyzers" data-testid="qc-dashboard-analyzers">
            <h3 className="qc-dashboard-section-title">
              {intl.formatMessage({ id: "qc.dashboard.analyzers.title" })}
            </h3>
            {analyzers.length === 0 ? (
              <div className="qc-dashboard-empty" data-testid="qc-dashboard-empty">
                {intl.formatMessage({ id: "qc.dashboard.noAnalyzers" })}
              </div>
            ) : (
              <Grid className="qc-dashboard-tiles">
                {analyzers.map((analyzer) => (
                  <Column key={analyzer.id} lg={4} md={4} sm={4}>
                    <ComplianceStatusTile
                      analyzerId={analyzer.id}
                      analyzerName={analyzer.name}
                      status={analyzer.complianceStatus}
                      triggeredRules={analyzer.triggeredRules || []}
                      lastResultTime={analyzer.lastResultTime}
                      unresolvedViolationCount={analyzer.unresolvedViolationCount || 0}
                      onClick={() => handleAnalyzerClick(analyzer.id)}
                    />
                  </Column>
                ))}
              </Grid>
            )}
          </div>
        </Column>

        {/* Alert Feed sidebar */}
        <Column lg={4} md={2} sm={4}>
          <div className="qc-dashboard-alerts" data-testid="qc-dashboard-alerts">
            <h3 className="qc-dashboard-section-title">
              {intl.formatMessage({ id: "qc.dashboard.alerts.title" })}
            </h3>
            <AlertFeed maxItems={10} />
          </div>
        </Column>
      </Grid>
    </div>
  );
};

export default QCDashboard;

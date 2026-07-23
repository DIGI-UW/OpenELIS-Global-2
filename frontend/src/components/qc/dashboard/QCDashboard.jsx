/**
 * QCDashboard Component
 *
 * Main dashboard with summary tiles and tabbed layout:
 * - Summary tiles (In Control, Warning, Out of Control, Pass Rate)
 * - Instruments tab (DataTable with search + pagination)
 * - Alerts tab (active violations + acknowledged table)
 *
 * Auto-refreshes every 5 minutes.
 */

import React, { useState, useEffect, useCallback, useRef } from "react";
import {
  Loading,
  InlineNotification,
  Button,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
} from "@carbon/react";
import { Renew } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import QCSummaryTiles from "./QCSummaryTiles";
import InstrumentsTab from "./InstrumentsTab";
import AlertsTab from "./AlertsTab";
import PageTitle from "../../common/PageTitle/PageTitle";
import "./QCDashboard.css";

const REFRESH_INTERVAL = 5 * 60 * 1000; // 5 minutes

const QCDashboard = () => {
  const intl = useIntl();
  const intlRef = useRef(intl);
  intlRef.current = intl;

  const [summary, setSummary] = useState(null);
  const [instruments, setInstruments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);

  const loadDashboardData = useCallback(() => {
    setLoading(true);
    setError(null);

    let completed = 0;
    const total = 2;
    let hasError = false;

    const checkDone = () => {
      completed++;
      if (completed >= total) {
        setLoading(false);
        if (!hasError) {
          setLastUpdated(new Date());
        }
      }
    };

    // Load summary data
    getFromOpenElisServer("/rest/qc/dashboard/summary", (response) => {
      if (response && (response.data || response.totalInstruments != null)) {
        setSummary(response.data || response);
      } else if (response && typeof response === "object") {
        setSummary(response);
      } else {
        hasError = true;
        setError(
          intlRef.current.formatMessage({
            id: "qc.dashboard.error.loadFailed",
          }),
        );
      }
      checkDone();
    });

    // Load instruments data
    getFromOpenElisServer("/rest/qc/dashboard/instruments", (response) => {
      if (response && response.data) {
        setInstruments(response.data || []);
      } else if (Array.isArray(response)) {
        setInstruments(response);
      } else {
        hasError = true;
        setError(
          intlRef.current.formatMessage({
            id: "qc.dashboard.error.loadFailed",
          }),
        );
      }
      checkDone();
    });
  }, []);

  useEffect(() => {
    loadDashboardData();
    const intervalId = setInterval(loadDashboardData, REFRESH_INTERVAL);
    return () => clearInterval(intervalId);
  }, [loadDashboardData]);

  const handleRefresh = () => {
    loadDashboardData();
  };

  const formatLastUpdated = () => {
    if (!lastUpdated) return "";
    return intl.formatDate(lastUpdated, {
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    });
  };

  if (loading && !summary && instruments.length === 0) {
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
                label: intl.formatMessage({
                  id: "analyzer.page.hierarchy.root",
                }),
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
          <span
            className="qc-dashboard-last-updated"
            data-testid="qc-dashboard-last-updated"
          >
            {intl.formatMessage({ id: "qc.dashboard.lastUpdated" })}:{" "}
            {formatLastUpdated()}
          </span>
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Renew}
            iconDescription={intl.formatMessage({
              id: "qc.dashboard.refresh",
            })}
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

      {/* Summary Tiles */}
      <QCSummaryTiles summary={summary || {}} loading={loading} />

      {/* Tabbed Content */}
      <Tabs>
        <TabList contained aria-label="QC Dashboard tabs">
          <Tab data-testid="qc-tab-instruments">
            {intl.formatMessage({ id: "qc.dashboard.tab.instruments" })}
          </Tab>
          <Tab data-testid="qc-tab-alerts">
            {intl.formatMessage({ id: "qc.dashboard.tab.alerts" })}
          </Tab>
        </TabList>
        <TabPanels>
          <TabPanel>
            <InstrumentsTab instruments={instruments} loading={loading} />
          </TabPanel>
          <TabPanel>
            <AlertsTab />
          </TabPanel>
        </TabPanels>
      </Tabs>
    </div>
  );
};

export default QCDashboard;

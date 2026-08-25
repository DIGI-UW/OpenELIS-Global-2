/**
 * InstrumentDetailPage Component
 *
 * Page showing detailed information about a laboratory instrument's QC status.
 * Navigated to from the QC Dashboard InstrumentsTab.
 *
 * Route: /analyzers/qc/instruments/:instrumentId
 */

import React, { useState, useEffect } from "react";
import {
  Button,
  Tag,
  Tile,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  Loading,
  InlineNotification,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { useHistory, useLocation, useParams } from "react-router-dom";
import {
  getComplianceTagType,
  getComplianceLabelKey,
  getZScoreBadgeType,
  formatTimestamp,
} from "./qcDashboardUtils";
import ActivityTimelineTab from "./ActivityTimelineTab";
import ControlChartTab from "./ControlChartTab";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { getFromOpenElisServer } from "../../utils/Utils";
import "./InstrumentDetailModal.css";

const InstrumentDetailPage = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const { instrumentId } = useParams();
  const [instrumentResponse, setInstrumentResponse] = useState({
    instrumentId: null,
    instrument: null,
  });
  const loading = instrumentResponse.instrumentId !== instrumentId;
  const instrument = loading ? null : instrumentResponse.instrument;
  const activeSubTab =
    new URLSearchParams(location.search).get("view") === "chart" ? 1 : 0;

  const selectSubTab = ({ selectedIndex }) => {
    const params = new URLSearchParams(location.search);
    params.set("view", selectedIndex === 1 ? "chart" : "activity");
    history.replace({
      pathname: location.pathname,
      search: params.toString(),
      hash: location.hash,
    });
  };

  const requestedReturnPath = new URLSearchParams(location.search).get(
    "returnTo",
  );
  const analyzerReturnPath = (() => {
    if (!requestedReturnPath) {
      return "/analyzers";
    }

    try {
      const candidate = new URL(requestedReturnPath, window.location.origin);
      if (
        candidate.origin === window.location.origin &&
        candidate.pathname === "/analyzers"
      ) {
        return `${candidate.pathname}${candidate.search}${candidate.hash}`;
      }
    } catch {
      // Ignore malformed return paths and fall back to the analyzer dashboard.
    }

    return "/analyzers";
  })();

  useEffect(() => {
    if (!instrumentId) {
      return undefined;
    }

    const controller = new AbortController();
    getFromOpenElisServer(
      `/rest/qc/dashboard/instruments/${instrumentId}`,
      (response) => {
        setInstrumentResponse({
          instrumentId,
          instrument: response?.data || response || null,
        });
      },
      controller.signal,
    );

    return () => controller.abort();
  }, [instrumentId]);

  if (loading) {
    return <Loading withOverlay={false} />;
  }

  if (!instrument) {
    return (
      <div>
        <PageBreadCrumb
          breadcrumbs={[
            { label: "home.label", link: "/" },
            {
              label: "analyzer.page.hierarchy.root",
              link: analyzerReturnPath,
            },
            { label: "qc.dashboard.title", link: "/analyzers/qc/db" },
            {
              label: "qc.instrument.notFound",
              isCurrentPage: true,
            },
          ]}
        />
        <div className="instrument-detail-content">
          <h1>{intl.formatMessage({ id: "qc.instrument.notFound" })}</h1>
        </div>
      </div>
    );
  }

  const analyteDetails = instrument.analyteDetails || [];
  const instrumentName =
    instrument.instrumentName || instrument.name || instrumentId;
  const operationalQcNotConfigured =
    instrument.complianceColor?.toUpperCase() === "NOT_CONFIGURED";

  return (
    <div
      data-testid="instrument-detail-page"
      className="instrument-detail-page"
    >
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/" },
          {
            label: "analyzer.page.hierarchy.root",
            link: analyzerReturnPath,
          },
          {
            label: "qc.dashboard.title",
            link: "/analyzers/qc/db",
          },
          { label: instrumentName, isCurrentPage: true },
        ]}
      />

      <div className="instrument-detail-content">
        <h1>{instrumentName}</h1>

        {/* Status header */}
        <div
          className="instrument-detail-status-header"
          data-testid="instrument-detail-modal-header"
        >
          <div>
            <span className="instrument-detail-subtitle">
              {instrument.instrumentType && (
                <span className="instrument-detail-type">
                  {instrument.instrumentType}
                </span>
              )}
              {instrument.instrumentType && instrument.instrumentLocation && (
                <span className="instrument-detail-subtitle__separator">
                  &middot;
                </span>
              )}
              {instrument.instrumentLocation && (
                <span>{instrument.instrumentLocation}</span>
              )}
            </span>
          </div>
          <Tag
            type={getComplianceTagType(instrument.complianceColor)}
            data-testid="compliance-tag"
          >
            {intl.formatMessage({
              id: getComplianceLabelKey(instrument.complianceColor),
            })}
          </Tag>
        </div>

        {operationalQcNotConfigured && (
          <InlineNotification
            kind="info"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "qc.instrumentDetail.notConfigured.title",
            })}
            subtitle={intl.formatMessage({
              id: "qc.instrumentDetail.notConfigured.description",
            })}
          />
        )}

        {/* Analyte cards */}
        {analyteDetails.length > 0 && (
          <div
            className="analyte-detail-grid"
            data-testid="analyte-detail-grid"
          >
            {analyteDetails.map((analyte) => (
              <Tile
                key={analyte.testId}
                className="analyte-detail-card"
                data-testid={`analyte-card-${analyte.testId}`}
              >
                <div className="analyte-name">{analyte.testName}</div>
                <div className="analyte-zscore">
                  <Tag
                    type={getZScoreBadgeType(analyte.latestZScore)}
                    size="sm"
                  >
                    z = {analyte.latestZScore?.toFixed(2) ?? "—"}
                  </Tag>
                </div>
                <div className="analyte-last-run">
                  {formatTimestamp(analyte.lastRunTime, intl)}
                </div>
              </Tile>
            ))}
          </div>
        )}

        {/* Tabs */}
        <Tabs selectedIndex={activeSubTab} onChange={selectSubTab}>
          <TabList aria-label="Instrument detail tabs">
            <Tab data-testid="tab-activity-timeline">
              {intl.formatMessage({
                id: "qc.instrumentDetail.tab.activityTimeline",
              })}
            </Tab>
            <Tab data-testid="tab-control-chart">
              {intl.formatMessage({
                id: "qc.instrumentDetail.tab.controlChart",
              })}
            </Tab>
          </TabList>
          <TabPanels>
            <TabPanel>
              <ActivityTimelineTab instrument={instrument} open={true} />
            </TabPanel>
            <TabPanel>
              <ControlChartTab
                instrument={instrument}
                active={activeSubTab === 1}
              />
            </TabPanel>
          </TabPanels>
        </Tabs>
      </div>

      <Button
        kind="secondary"
        onClick={() => history.push("/analyzers/qc/db")}
        style={{ marginTop: "1rem" }}
      >
        {intl.formatMessage({ id: "qc.dashboard.title" })}
      </Button>
    </div>
  );
};

export default InstrumentDetailPage;

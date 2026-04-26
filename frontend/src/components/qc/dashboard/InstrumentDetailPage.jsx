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
} from "@carbon/react";
import { useIntl } from "react-intl";
import { useHistory, useParams } from "react-router-dom";
import {
  getComplianceTagType,
  getComplianceLabelKey,
  getZScoreBadgeType,
  formatTimestamp,
} from "./qcDashboardUtils";
import ActivityTimelineTab from "./ActivityTimelineTab";
import ControlChartTab from "./ControlChartTab";
import PageTitle from "../../common/PageTitle/PageTitle";
import { getFromOpenElisServer } from "../../utils/Utils";
import "./InstrumentDetailModal.css";

const InstrumentDetailPage = () => {
  const intl = useIntl();
  const history = useHistory();
  const { instrumentId } = useParams();
  const [instrument, setInstrument] = useState(null);
  const [loading, setLoading] = useState(true);
  const [activeSubTab, setActiveSubTab] = useState(0);

  useEffect(() => {
    if (instrumentId) {
      setLoading(true);
      getFromOpenElisServer(
        `/rest/qc/dashboard/instruments/${instrumentId}`,
        (response) => {
          setInstrument(response?.data || response);
          setLoading(false);
        },
      );
    }
  }, [instrumentId]);

  if (loading) {
    return <Loading withOverlay={false} />;
  }

  if (!instrument) {
    return (
      <div>
        <PageTitle
          breadcrumbs={[
            {
              label: intl.formatMessage({ id: "analyzer.page.hierarchy.root" }),
              link: "/analyzers",
            },
            {
              label: intl.formatMessage({ id: "qc.dashboard.title" }),
              link: "/analyzers/qc/db",
            },
            { label: "..." },
          ]}
        />
        <p>{intl.formatMessage({ id: "qc.instrument.notFound" })}</p>
      </div>
    );
  }

  const analyteDetails = instrument.analyteDetails || [];

  return (
    <div
      data-testid="instrument-detail-page"
      className="instrument-detail-page"
    >
      <PageTitle
        breadcrumbs={[
          {
            label: intl.formatMessage({ id: "analyzer.page.hierarchy.root" }),
            link: "/analyzers",
          },
          {
            label: intl.formatMessage({ id: "qc.dashboard.title" }),
            link: "/analyzers/qc/db",
          },
          { label: instrument.instrumentName || instrumentId },
        ]}
      />

      <div className="instrument-detail-content">
        {/* Status header */}
        <div
          className="instrument-detail-status-header"
          data-testid="instrument-detail-modal-header"
        >
          <div>
            <span className="instrument-detail-subtitle">
              <span>{instrument.instrumentId}</span>
              <span className="instrument-detail-type">
                {instrument.instrumentType}
              </span>
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
        <Tabs
          selectedIndex={activeSubTab}
          onChange={({ selectedIndex }) => setActiveSubTab(selectedIndex)}
        >
          <TabList aria-label="Instrument detail tabs">
            <Tab data-testid="tab-activity-timeline">
              {intl.formatMessage({ id: "qc.instrument.tab.timeline" })}
            </Tab>
            <Tab data-testid="tab-control-chart">
              {intl.formatMessage({ id: "qc.instrument.tab.chart" })}
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

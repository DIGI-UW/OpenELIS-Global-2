/**
 * InstrumentDetailModal Component
 *
 * Modal showing detailed information about a laboratory instrument's QC status.
 * Launched from the "View" button in InstrumentsTab.
 *
 * Features:
 * - Instrument metadata header with compliance status
 * - Analytes Monitored cards with sigma values
 * - Activity Timeline tab (delegated to ActivityTimelineTab)
 * - Control Chart tab (delegated to ControlChartTab)
 */

import React, { useState } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  Tag,
  Tile,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
} from "@carbon/react";
import { useIntl } from "react-intl";
import PropTypes from "prop-types";
import {
  getComplianceTagType,
  getComplianceLabelKey,
  getZScoreBadgeType,
  formatTimestamp,
} from "./qcDashboardUtils";
import ActivityTimelineTab from "./ActivityTimelineTab";
import ControlChartTab from "./ControlChartTab";
import "./InstrumentDetailModal.css";

const InstrumentDetailModal = ({ instrument, open, onClose }) => {
  const intl = useIntl();
  const [activeSubTab, setActiveSubTab] = useState(0);

  if (!instrument) return null;

  const analyteDetails = instrument.analyteDetails || [];

  return (
    <ComposedModal
      open={open}
      onClose={onClose}
      size="lg"
      data-testid="instrument-detail-modal"
    >
      <ModalHeader
        title={instrument.instrumentName}
        label={
          <span className="instrument-detail-subtitle">
            <span>{instrument.instrumentId}</span>
            <span className="instrument-detail-subtitle__separator">
              &middot;
            </span>
            <span>{instrument.instrumentType}</span>
            <span className="instrument-detail-subtitle__separator">
              &middot;
            </span>
            <span>{instrument.instrumentLocation}</span>
            <span className="instrument-detail-subtitle__separator">
              &middot;
            </span>
            <Tag
              type={getComplianceTagType(instrument.complianceColor)}
              size="sm"
            >
              {intl.formatMessage({
                id: getComplianceLabelKey(instrument.complianceColor),
              })}
            </Tag>
          </span>
        }
        data-testid="instrument-detail-modal-header"
      />
      <ModalBody data-testid="instrument-detail-modal-body">
        {/* Analytes Monitored Section */}
        <div className="instrument-detail-section">
          <h4>
            {intl.formatMessage({
              id: "qc.instrumentDetail.analytesMonitored",
            })}
          </h4>
          <div className="instrument-detail-analytes">
            {analyteDetails.map((analyte) => (
              <Tile
                key={analyte.testId}
                className="instrument-detail-analyte-card"
              >
                <div className="instrument-detail-analyte-card__name">
                  {analyte.testName}
                </div>
                <div className="instrument-detail-analyte-card__meta">
                  {analyte.latestZScore != null && (
                    <Tag
                      type={getZScoreBadgeType(analyte.latestZScore)}
                      size="sm"
                    >
                      {Math.abs(parseFloat(analyte.latestZScore)).toFixed(1)}
                      &sigma;
                    </Tag>
                  )}
                </div>
                <div className="instrument-detail-analyte-card__detail">
                  {analyte.lastRunTime && (
                    <span className="instrument-detail-analyte-card__time">
                      {formatTimestamp(analyte.lastRunTime)}
                    </span>
                  )}
                </div>
              </Tile>
            ))}
          </div>
        </div>

        {/* Sub-tabs: Activity Timeline + Control Chart */}
        <Tabs
          selectedIndex={activeSubTab}
          onChange={({ selectedIndex }) => setActiveSubTab(selectedIndex)}
        >
          <TabList aria-label="Instrument detail tabs">
            <Tab>
              {intl.formatMessage({
                id: "qc.instrumentDetail.tab.activityTimeline",
              })}
            </Tab>
            <Tab>
              {intl.formatMessage({
                id: "qc.instrumentDetail.tab.controlChart",
              })}
            </Tab>
          </TabList>
          <TabPanels>
            <TabPanel>
              <ActivityTimelineTab instrument={instrument} open={open} />
            </TabPanel>
            <TabPanel>
              <ControlChartTab
                instrument={instrument}
                active={open && activeSubTab === 1}
              />
            </TabPanel>
          </TabPanels>
        </Tabs>
      </ModalBody>
      <ModalFooter data-testid="instrument-detail-modal-footer">
        <Button kind="secondary" onClick={onClose}>
          {intl.formatMessage({ id: "button.close" })}
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

InstrumentDetailModal.propTypes = {
  instrument: PropTypes.shape({
    instrumentId: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    instrumentName: PropTypes.string,
    instrumentType: PropTypes.string,
    instrumentLocation: PropTypes.string,
    complianceColor: PropTypes.string,
    analyteDetails: PropTypes.arrayOf(
      PropTypes.shape({
        testId: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
        testName: PropTypes.string,
        latestZScore: PropTypes.number,
        lastRunTime: PropTypes.string,
      }),
    ),
    triggeredRuleDetails: PropTypes.array,
    unresolvedRejections: PropTypes.number,
    unresolvedWarnings: PropTypes.number,
    activeControlLots: PropTypes.number,
  }),
  open: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
};

export default InstrumentDetailModal;

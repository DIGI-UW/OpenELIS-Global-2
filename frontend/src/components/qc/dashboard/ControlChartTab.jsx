/**
 * ControlChartTab Component
 *
 * Extracted from InstrumentDetailModal.jsx. Handles the Control Chart tab
 * content including analyte selection, chart data fetching, chart rendering,
 * statistics cards, and active rules display.
 *
 * Props:
 * - instrument: the instrument object with analyteDetails, instrumentId, etc.
 * - active: whether this tab is currently visible (replaces activeSubTab === 1)
 */

import React, { useState, useEffect, useMemo, useCallback } from "react";
import { Dropdown, Loading, Tag, Tile, Grid, Column } from "@carbon/react";
import { useIntl } from "react-intl";
import PropTypes from "prop-types";
import { getFromOpenElisServer } from "../../utils/Utils";
import LeveyJenningsChart from "../charts/LeveyJenningsChart";

const ControlChartTab = ({ instrument, active }) => {
  const intl = useIntl();

  // Control Chart state
  const [selectedAnalyteIndex, setSelectedAnalyteIndex] = useState(0);
  const [chartData, setChartData] = useState([]);
  const [chartStatistics, setChartStatistics] = useState(null);
  const [chartLoading, setChartLoading] = useState(false);

  // Active QC Rules state
  const [activeRules, setActiveRules] = useState([]);

  // Transform backend dataPoints to LeveyJenningsChart format
  const transformDataPoints = (dataPoints) => {
    return (dataPoints || []).map((pt) => ({
      id: pt.resultId,
      runDateTime: pt.timestamp,
      resultValue: pt.value,
      value: pt.value,
      zScore: pt.zscore ?? pt.zScore,
      violated: pt.hasViolation,
      violations: (pt.violatedRules || []).map((rule) => ({
        code: rule,
      })),
    }));
  };

  // Load chart data for a specific control lot (two parallel calls)
  const loadChartForControlLot = useCallback((controlLotId) => {
    setChartLoading(true);
    setChartData([]);
    setChartStatistics(null);

    let completedCalls = 0;
    const checkDone = () => {
      completedCalls++;
      if (completedCalls >= 2) {
        setChartLoading(false);
      }
    };

    // Fetch data points
    getFromOpenElisServer(`/rest/qc/charts/${controlLotId}`, (response) => {
      const dataPoints =
        response?.dataPoints || response?.data?.dataPoints || [];
      setChartData(transformDataPoints(dataPoints));
      checkDone();
    });

    // Fetch statistics (mean, SD for reference lines)
    getFromOpenElisServer(
      `/rest/qc/charts/${controlLotId}/statistics`,
      (response) => {
        if (response && response.mean != null) {
          setChartStatistics(response);
        } else {
          setChartStatistics(null);
        }
        checkDone();
      },
    );
  }, []);

  // When chart tab activates or analyte changes, fetch control lots then chart data
  useEffect(() => {
    if (!instrument || !active) return;

    const analyte = instrument.analyteDetails?.[selectedAnalyteIndex];
    if (!analyte) return;

    setChartLoading(true);

    getFromOpenElisServer(
      `/rest/qc/controlLots?testId=${analyte.testId}&instrumentId=${instrument.instrumentId}`,
      (response) => {
        const lots = Array.isArray(response) ? response : response?.data || [];
        if (lots.length > 0) {
          loadChartForControlLot(lots[0].id);
        } else {
          setChartData([]);
          setChartStatistics(null);
          setChartLoading(false);
        }
      },
    );

    // Fetch active QC rules for this analyte+instrument
    getFromOpenElisServer(
      `/rest/qc/ruleConfig/enabled?testId=${analyte.testId}&instrumentId=${instrument.instrumentId}`,
      (response) => {
        const rules = Array.isArray(response) ? response : response?.data || [];
        setActiveRules(rules.filter((r) => r.enabled));
      },
    );
  }, [instrument, active, selectedAnalyteIndex, loadChartForControlLot]);

  // Build analyte dropdown options
  const analyteOptions = useMemo(() => {
    return (instrument?.analyteDetails || []).map((analyte, idx) => ({
      id: String(analyte.testId),
      index: idx,
      label: analyte.testName,
      testId: analyte.testId,
    }));
  }, [instrument]);

  if (!instrument) return null;

  return (
    <>
      {analyteOptions.length > 1 && (
        <div className="instrument-detail-chart__controls">
          <Dropdown
            id="instrument-detail-analyte-selector"
            titleText={intl.formatMessage({
              id: "qc.instrumentDetail.chart.selectAnalyte",
            })}
            items={analyteOptions}
            selectedItem={analyteOptions[selectedAnalyteIndex]}
            itemToString={(item) => item?.label || ""}
            onChange={({ selectedItem }) =>
              setSelectedAnalyteIndex(selectedItem?.index ?? 0)
            }
            data-testid="instrument-detail-analyte-dropdown"
          />
        </div>
      )}
      {analyteOptions.length === 1 && (
        <div className="instrument-detail-chart__controls">
          <p className="instrument-detail-chart__analyte-label">
            {analyteOptions[0]?.label}
          </p>
        </div>
      )}
      <div className="instrument-detail-chart">
        {chartLoading ? (
          <div className="instrument-detail-chart__loading">
            <Loading
              withOverlay={false}
              small
              description={intl.formatMessage({
                id: "qc.instrumentDetail.chart.loading",
              })}
            />
          </div>
        ) : chartData.length === 0 ? (
          <div className="instrument-detail-chart__empty">
            {intl.formatMessage({
              id: "qc.instrumentDetail.chart.noData",
            })}
          </div>
        ) : (
          <LeveyJenningsChart
            data={chartData}
            statistics={chartStatistics}
            height="350px"
            showLegend={true}
          />
        )}
      </div>

      {/* Statistics Cards */}
      {chartStatistics && !chartLoading && (
        <Grid className="instrument-detail-chart__stats-section">
          <Column lg={4} md={2} sm={2}>
            <Tile className="instrument-detail-chart__stat-card">
              <span className="instrument-detail-chart__stat-label">
                {intl.formatMessage({
                  id: "qc.instrumentDetail.chart.stats.mean",
                })}
              </span>
              <span className="instrument-detail-chart__stat-value">
                {chartStatistics.mean?.toFixed(2) ?? "-"}
              </span>
            </Tile>
          </Column>
          <Column lg={4} md={2} sm={2}>
            <Tile className="instrument-detail-chart__stat-card">
              <span className="instrument-detail-chart__stat-label">
                {intl.formatMessage({
                  id: "qc.instrumentDetail.chart.stats.sd",
                })}
              </span>
              <span className="instrument-detail-chart__stat-value">
                {chartStatistics.standardDeviation?.toFixed(2) ?? "-"}
              </span>
            </Tile>
          </Column>
          <Column lg={4} md={2} sm={2}>
            <Tile className="instrument-detail-chart__stat-card">
              <span className="instrument-detail-chart__stat-label">
                {intl.formatMessage({
                  id: "qc.instrumentDetail.chart.stats.cv",
                })}
              </span>
              <span className="instrument-detail-chart__stat-value">
                {chartStatistics.mean && chartStatistics.standardDeviation
                  ? (
                      (chartStatistics.standardDeviation /
                        Math.abs(chartStatistics.mean)) *
                      100
                    ).toFixed(1) + "%"
                  : "-"}
              </span>
            </Tile>
          </Column>
          <Column lg={4} md={2} sm={2}>
            <Tile className="instrument-detail-chart__stat-card">
              <span className="instrument-detail-chart__stat-label">
                {intl.formatMessage({
                  id: "qc.instrumentDetail.chart.stats.n",
                })}
              </span>
              <span className="instrument-detail-chart__stat-value">
                {chartStatistics.resultCount ?? chartData.length}
              </span>
            </Tile>
          </Column>
        </Grid>
      )}

      {/* Active QC Rules */}
      {!chartLoading && (
        <div className="instrument-detail-chart__rules-section">
          <h5>
            {intl.formatMessage({
              id: "qc.instrumentDetail.chart.activeRules",
            })}
          </h5>
          {activeRules.length > 0 ? (
            <div className="instrument-detail-chart__rules-tags">
              {activeRules.map((rule) => (
                <Tag
                  key={rule.id || rule.ruleCode}
                  type={rule.severity === "REJECTION" ? "red" : "teal"}
                  size="sm"
                >
                  {rule.ruleCode}
                </Tag>
              ))}
            </div>
          ) : (
            <p className="instrument-detail-chart__no-rules">
              {intl.formatMessage({
                id: "qc.instrumentDetail.chart.noRules",
              })}
            </p>
          )}
        </div>
      )}
    </>
  );
};

ControlChartTab.propTypes = {
  instrument: PropTypes.shape({
    instrumentId: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    instrumentName: PropTypes.string,
    analyteDetails: PropTypes.arrayOf(
      PropTypes.shape({
        testId: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
        testName: PropTypes.string,
      }),
    ),
  }),
  active: PropTypes.bool.isRequired,
};

export default ControlChartTab;

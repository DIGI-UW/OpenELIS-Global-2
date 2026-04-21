/**
 * ControlChartDetail Component
 *
 * Detailed view of Levey-Jennings chart with filtering and export
 * Task Reference: T133
 * Specification: FR-056, FR-057, FR-058, FR-059, User Story 2
 *
 * Features:
 * - Date range filtering
 * - Control level filtering (Low/Normal/High)
 * - Zoom and pan functionality
 * - Export to PNG/PDF
 */

import React, { useState, useEffect, useCallback } from "react";
import {
  Grid,
  Column,
  DatePicker,
  DatePickerInput,
  Dropdown,
  Button,
  Loading,
  InlineNotification,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
} from "@carbon/react";
import { Download, ZoomIn, ZoomOut, FitToScreen } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { useParams, useHistory } from "react-router-dom";
import { getFromOpenElisServer } from "../../utils/Utils";
import LeveyJenningsChart from "./LeveyJenningsChart";
import PageTitle from "../../common/PageTitle/PageTitle";
import "./ControlChartDetail.css";

const ControlChartDetail = () => {
  const intl = useIntl();
  const history = useHistory();
  const { analyzerId } = useParams();

  // State
  const [chartData, setChartData] = useState([]);
  const [statistics, setStatistics] = useState(null);
  const [analyzerInfo, setAnalyzerInfo] = useState(null);
  const [controlLots, setControlLots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filters
  const [dateRange, setDateRange] = useState([null, null]);
  const [selectedControlLevel, setSelectedControlLevel] = useState("ALL");
  const [selectedControlLot, setSelectedControlLot] = useState(null);

  // Zoom state
  const [zoomLevel, setZoomLevel] = useState(1);

  // Control level options
  const controlLevelOptions = [
    {
      id: "ALL",
      label: intl.formatMessage({ id: "qc.chart.filter.allLevels" }),
    },
    {
      id: "LOW",
      label: intl.formatMessage({ id: "qc.chart.filter.levelLow" }),
    },
    {
      id: "NORMAL",
      label: intl.formatMessage({ id: "qc.chart.filter.levelNormal" }),
    },
    {
      id: "HIGH",
      label: intl.formatMessage({ id: "qc.chart.filter.levelHigh" }),
    },
  ];

  // Transform backend dataPoints to LeveyJenningsChart format
  const transformDataPoints = (dataPoints) => {
    return (dataPoints || []).map((pt) => ({
      id: pt.resultId,
      runDateTime: pt.timestamp,
      resultValue: pt.value,
      value: pt.value,
      zScore: pt.zscore ?? pt.zScore,
      controlLevel: pt.controlLevel,
      violated: pt.hasViolation,
      violations: (pt.violatedRules || []).map((rule) => ({
        code: rule,
      })),
    }));
  };

  // Load control lots and analyzer info on mount
  useEffect(() => {
    getFromOpenElisServer(
      `/rest/qc/controlLots?instrumentId=${analyzerId}`,
      (response) => {
        const lots = Array.isArray(response) ? response : response?.data || [];
        setControlLots(lots);
        if (lots.length > 0 && !selectedControlLot) {
          setSelectedControlLot(lots[0].id);
        }
      },
    );

    getFromOpenElisServer(
      `/rest/analyzer/analyzers/${analyzerId}`,
      (response) => {
        if (response) {
          setAnalyzerInfo(response?.data || response);
        }
      },
    );
  }, [analyzerId]); // eslint-disable-line react-hooks/exhaustive-deps

  // Load chart data for the selected control lot
  const loadChartData = useCallback(
    (controlLotId) => {
      if (!controlLotId) {
        setChartData([]);
        setStatistics(null);
        setLoading(false);
        return;
      }

      setLoading(true);
      setError(null);

      // Build date range query params (only params the backend accepts)
      const params = new URLSearchParams();
      if (dateRange[0]) {
        params.append(
          "startDate",
          dateRange[0] instanceof Date
            ? dateRange[0].toISOString().split("T")[0]
            : dateRange[0],
        );
      }
      if (dateRange[1]) {
        params.append(
          "endDate",
          dateRange[1] instanceof Date
            ? dateRange[1].toISOString().split("T")[0]
            : dateRange[1],
        );
      }

      const qs = params.toString() ? `?${params.toString()}` : "";
      let completedCalls = 0;
      const checkDone = () => {
        completedCalls++;
        if (completedCalls >= 2) {
          setLoading(false);
        }
      };

      // Fetch data points
      getFromOpenElisServer(
        `/rest/qc/charts/${controlLotId}${qs}`,
        (response) => {
          const dataPoints =
            response?.dataPoints || response?.data?.dataPoints || [];
          setChartData(transformDataPoints(dataPoints));
          checkDone();
        },
      );

      // Fetch statistics (mean, SD for reference lines)
      getFromOpenElisServer(
        `/rest/qc/charts/${controlLotId}/statistics`,
        (response) => {
          if (response && response.mean != null) {
            setStatistics(response);
          } else {
            setStatistics(null);
          }
          checkDone();
        },
      );
    },
    [dateRange],
  );

  // Reload chart when selected lot or date range changes
  useEffect(() => {
    if (selectedControlLot) {
      loadChartData(selectedControlLot);
    }
  }, [selectedControlLot, loadChartData]);

  // Handle date range change
  const handleDateRangeChange = (dates) => {
    setDateRange(dates);
  };

  // Handle control level change
  const handleControlLevelChange = ({ selectedItem }) => {
    setSelectedControlLevel(selectedItem.id);
  };

  // Handle control lot change
  const handleControlLotChange = ({ selectedItem }) => {
    setSelectedControlLot(selectedItem?.id || null);
  };

  // Handle export (FR-059)
  const handleExport = (format) => {
    // Export chart as image
    const chartElement = document.querySelector(".lj-chart-container svg");
    if (!chartElement) return;

    if (format === "png") {
      // Convert SVG to PNG
      const svgData = new XMLSerializer().serializeToString(chartElement);
      const canvas = document.createElement("canvas");
      const ctx = canvas.getContext("2d");
      const img = new Image();

      img.onload = () => {
        canvas.width = img.width;
        canvas.height = img.height;
        ctx.drawImage(img, 0, 0);
        const pngUrl = canvas.toDataURL("image/png");
        const link = document.createElement("a");
        link.download = `qc-chart-${analyzerId}-${new Date().toISOString().split("T")[0]}.png`;
        link.href = pngUrl;
        link.click();
      };

      img.src =
        "data:image/svg+xml;base64," +
        btoa(unescape(encodeURIComponent(svgData)));
    }
  };

  // Handle zoom
  const handleZoomIn = () => {
    setZoomLevel((prev) => Math.min(prev + 0.25, 3));
  };

  const handleZoomOut = () => {
    setZoomLevel((prev) => Math.max(prev - 0.25, 0.5));
  };

  const handleZoomReset = () => {
    setZoomLevel(1);
  };

  // Navigate back to dashboard
  const handleBackToDashboard = () => {
    history.push("/analyzers/qc/db");
  };

  if (loading && chartData.length === 0) {
    return (
      <div
        className="control-chart-detail-loading"
        data-testid="control-chart-detail-loading"
      >
        <Loading
          description={intl.formatMessage({ id: "qc.chart.loading" })}
          withOverlay={false}
        />
      </div>
    );
  }

  return (
    <div className="control-chart-detail" data-testid="control-chart-detail">
      {/* Header */}
      <div
        className="control-chart-detail-header"
        data-testid="control-chart-detail-header"
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
            {
              label:
                analyzerInfo?.name ||
                intl.formatMessage({ id: "qc.chart.title" }),
            },
          ]}
          subtitle={intl.formatMessage({ id: "qc.chart.subtitle" })}
        />
      </div>

      {/* Error notification */}
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({ id: "qc.chart.error.title" })}
          subtitle={error}
          onClose={() => setError(null)}
          data-testid="control-chart-detail-error"
        />
      )}

      {/* Filters */}
      <Grid
        className="control-chart-detail-filters"
        data-testid="control-chart-detail-filters"
      >
        <Column lg={6} md={4} sm={4}>
          <DatePicker
            datePickerType="range"
            dateFormat="Y-m-d"
            onChange={handleDateRangeChange}
            value={dateRange}
          >
            <DatePickerInput
              id="date-picker-start"
              placeholder="yyyy-mm-dd"
              labelText={intl.formatMessage({
                id: "qc.chart.filter.startDate",
              })}
              data-testid="chart-filter-start-date"
            />
            <DatePickerInput
              id="date-picker-end"
              placeholder="yyyy-mm-dd"
              labelText={intl.formatMessage({ id: "qc.chart.filter.endDate" })}
              data-testid="chart-filter-end-date"
            />
          </DatePicker>
        </Column>
        <Column lg={3} md={2} sm={4}>
          <Dropdown
            id="control-level-dropdown"
            titleText={intl.formatMessage({
              id: "qc.chart.filter.controlLevel",
            })}
            label={intl.formatMessage({ id: "qc.chart.filter.selectLevel" })}
            items={controlLevelOptions}
            itemToString={(item) => item?.label || ""}
            selectedItem={controlLevelOptions.find(
              (o) => o.id === selectedControlLevel,
            )}
            onChange={handleControlLevelChange}
            data-testid="chart-filter-control-level"
          />
        </Column>
        <Column lg={3} md={2} sm={4}>
          <Dropdown
            id="control-lot-dropdown"
            titleText={intl.formatMessage({ id: "qc.chart.filter.controlLot" })}
            label={intl.formatMessage({ id: "qc.chart.filter.selectLot" })}
            items={controlLots.map((lot) => ({
              id: lot.id,
              label: lot.lotNumber || lot.id,
            }))}
            itemToString={(item) => item?.label || ""}
            selectedItem={
              selectedControlLot
                ? {
                    id: selectedControlLot,
                    label:
                      controlLots.find((l) => l.id === selectedControlLot)
                        ?.lotNumber || selectedControlLot,
                  }
                : {
                    id: null,
                    label: intl.formatMessage({
                      id: "qc.chart.filter.allLots",
                    }),
                  }
            }
            onChange={handleControlLotChange}
            data-testid="chart-filter-control-lot"
          />
        </Column>
        <Column lg={4} md={4} sm={4}>
          <div className="control-chart-detail-actions">
            <Button
              kind="ghost"
              size="sm"
              renderIcon={ZoomIn}
              iconDescription={intl.formatMessage({
                id: "qc.chart.action.zoomIn",
              })}
              onClick={handleZoomIn}
              data-testid="chart-zoom-in"
            />
            <Button
              kind="ghost"
              size="sm"
              renderIcon={ZoomOut}
              iconDescription={intl.formatMessage({
                id: "qc.chart.action.zoomOut",
              })}
              onClick={handleZoomOut}
              data-testid="chart-zoom-out"
            />
            <Button
              kind="ghost"
              size="sm"
              renderIcon={FitToScreen}
              iconDescription={intl.formatMessage({
                id: "qc.chart.action.zoomReset",
              })}
              onClick={handleZoomReset}
              data-testid="chart-zoom-reset"
            />
            <Button
              kind="primary"
              size="sm"
              renderIcon={Download}
              onClick={() => handleExport("png")}
              data-testid="chart-export-button"
            >
              {intl.formatMessage({ id: "qc.chart.action.export" })}
            </Button>
          </div>
        </Column>
      </Grid>

      {/* Chart with control levels as tabs (FR-058) */}
      {selectedControlLevel === "ALL" && controlLots.length > 0 ? (
        <Tabs>
          <TabList
            aria-label={intl.formatMessage({ id: "qc.chart.tabs.label" })}
          >
            {controlLevelOptions
              .filter((level) => level.id !== "ALL")
              .map((level) => (
                <Tab key={level.id} data-testid={`chart-tab-${level.id}`}>
                  {level.label}
                </Tab>
              ))}
          </TabList>
          <TabPanels>
            {controlLevelOptions
              .filter((level) => level.id !== "ALL")
              .map((level) => {
                const levelData = chartData.filter(
                  (d) => d.controlLevel?.toUpperCase() === level.id,
                );
                const levelStats = statistics?.[level.id] || statistics;
                return (
                  <TabPanel key={level.id}>
                    <div
                      className="control-chart-detail-chart"
                      style={{
                        transform: `scale(${zoomLevel})`,
                        transformOrigin: "top left",
                      }}
                      data-testid={`chart-panel-${level.id}`}
                    >
                      <LeveyJenningsChart
                        data={levelData}
                        statistics={levelStats}
                        height={`${400 * zoomLevel}px`}
                      />
                    </div>
                  </TabPanel>
                );
              })}
          </TabPanels>
        </Tabs>
      ) : (
        <div
          className="control-chart-detail-chart"
          style={{
            transform: `scale(${zoomLevel})`,
            transformOrigin: "top left",
          }}
          data-testid="chart-panel-single"
        >
          <LeveyJenningsChart
            data={chartData}
            statistics={statistics}
            height={`${400 * zoomLevel}px`}
          />
        </div>
      )}

      {/* Statistics summary */}
      {statistics && (
        <Grid
          className="control-chart-detail-stats"
          data-testid="control-chart-detail-stats"
        >
          <Column lg={4} md={2} sm={4}>
            <div className="stat-item">
              <span className="stat-label">
                {intl.formatMessage({ id: "qc.chart.stats.mean" })}
              </span>
              <span className="stat-value">
                {statistics.mean?.toFixed(2) || "-"}
              </span>
            </div>
          </Column>
          <Column lg={4} md={2} sm={4}>
            <div className="stat-item">
              <span className="stat-label">
                {intl.formatMessage({ id: "qc.chart.stats.sd" })}
              </span>
              <span className="stat-value">
                {statistics.standardDeviation?.toFixed(2) || "-"}
              </span>
            </div>
          </Column>
          <Column lg={4} md={2} sm={4}>
            <div className="stat-item">
              <span className="stat-label">
                {intl.formatMessage({ id: "qc.chart.stats.cv" })}
              </span>
              <span className="stat-value">
                {statistics.mean && statistics.standardDeviation
                  ? (
                      (statistics.standardDeviation / statistics.mean) *
                      100
                    ).toFixed(2) + "%"
                  : "-"}
              </span>
            </div>
          </Column>
          <Column lg={4} md={2} sm={4}>
            <div className="stat-item">
              <span className="stat-label">
                {intl.formatMessage({ id: "qc.chart.stats.n" })}
              </span>
              <span className="stat-value">
                {statistics.resultCount || statistics.n || chartData.length}
              </span>
            </div>
          </Column>
        </Grid>
      )}
    </div>
  );
};

export default ControlChartDetail;

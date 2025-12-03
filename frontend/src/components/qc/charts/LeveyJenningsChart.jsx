/**
 * LeveyJenningsChart Component
 *
 * Displays interactive Levey-Jennings control chart with Westgard rule overlays
 * Task Reference: T132
 * Specification: FR-052 to FR-059, User Story 2
 *
 * Features:
 * - QC results plotted chronologically
 * - Reference lines for mean, ±1SD, ±2SD, ±3SD
 * - Violation points highlighted with distinct colors and increased size
 * - Tooltips showing result value, z-score, date/time, and rule violations
 */

import React, { useMemo } from "react";
import { LineChart } from "@carbon/charts-react";
import { useIntl } from "react-intl";
import PropTypes from "prop-types";
import "@carbon/charts/styles.css";
import "./LeveyJenningsChart.css";

const LeveyJenningsChart = ({
  data,
  statistics,
  height,
  showLegend,
}) => {
  const intl = useIntl();

  // Default statistics if not provided
  const mean = statistics?.mean || 0;
  const sd = statistics?.standardDeviation || 1;

  // Transform data for Carbon Charts
  const chartData = useMemo(() => {
    if (!data || data.length === 0) return [];

    return data.map((point) => ({
      group: point.violated
        ? intl.formatMessage({ id: "qc.chart.legend.violation" })
        : intl.formatMessage({ id: "qc.chart.legend.normal" }),
      date: new Date(point.runDateTime || point.date),
      value: point.resultValue || point.value,
      // Store additional data for tooltip
      zScore: point.zScore,
      violations: point.violations || [],
      resultId: point.id,
    }));
  }, [data, intl]);

  // Reference lines for SD levels
  const referenceLines = useMemo(() => {
    return [
      { value: mean, label: intl.formatMessage({ id: "qc.chart.mean" }), type: "mean" },
      { value: mean + sd, label: "+1SD", type: "sd1" },
      { value: mean - sd, label: "-1SD", type: "sd1" },
      { value: mean + 2 * sd, label: "+2SD", type: "sd2" },
      { value: mean - 2 * sd, label: "-2SD", type: "sd2" },
      { value: mean + 3 * sd, label: "+3SD", type: "sd3" },
      { value: mean - 3 * sd, label: "-3SD", type: "sd3" },
    ];
  }, [mean, sd, intl]);

  // Chart options
  const options = useMemo(() => ({
    title: "",
    height: height || "400px",
    axes: {
      bottom: {
        title: intl.formatMessage({ id: "qc.chart.axis.date" }),
        mapsTo: "date",
        scaleType: "time",
      },
      left: {
        title: intl.formatMessage({ id: "qc.chart.axis.value" }),
        mapsTo: "value",
        scaleType: "linear",
        includeZero: false,
      },
    },
    points: {
      radius: 5,
      filled: true,
    },
    color: {
      scale: {
        [intl.formatMessage({ id: "qc.chart.legend.normal" })]: "#0f62fe", // Carbon blue
        [intl.formatMessage({ id: "qc.chart.legend.violation" })]: "#da1e28", // Carbon red
      },
    },
    legend: {
      enabled: showLegend,
      position: "bottom",
    },
    tooltip: {
      enabled: true,
      showTotal: false,
      customHTML: (datapoint) => {
        if (!datapoint || !datapoint[0]) return "";

        const point = datapoint[0];
        const originalData = data.find(d =>
          new Date(d.runDateTime || d.date).getTime() === point.date?.getTime()
        );

        const dateStr = point.date
          ? intl.formatDate(point.date, {
              year: "numeric",
              month: "2-digit",
              day: "2-digit",
              hour: "2-digit",
              minute: "2-digit",
            })
          : "-";

        const zScore = originalData?.zScore !== undefined
          ? originalData.zScore.toFixed(2)
          : "-";

        const violations = originalData?.violations || [];
        const violationsHtml = violations.length > 0
          ? `<div class="lj-tooltip-violations">
               <strong>${intl.formatMessage({ id: "qc.chart.tooltip.violations" })}:</strong>
               ${violations.map(v => `<span class="lj-tooltip-violation-tag">${v.code || v}</span>`).join(" ")}
             </div>`
          : "";

        return `
          <div class="lj-tooltip">
            <div class="lj-tooltip-row">
              <strong>${intl.formatMessage({ id: "qc.chart.tooltip.value" })}:</strong>
              <span>${point.value?.toFixed(2) || "-"}</span>
            </div>
            <div class="lj-tooltip-row">
              <strong>${intl.formatMessage({ id: "qc.chart.tooltip.zScore" })}:</strong>
              <span>${zScore}</span>
            </div>
            <div class="lj-tooltip-row">
              <strong>${intl.formatMessage({ id: "qc.chart.tooltip.date" })}:</strong>
              <span>${dateStr}</span>
            </div>
            ${violationsHtml}
          </div>
        `;
      },
    },
    // Reference lines for SD levels (FR-053)
    grid: {
      y: {
        enabled: true,
      },
    },
    // Threshold lines (displayed as horizontal grid lines)
    thresholds: referenceLines.map((line) => ({
      value: line.value,
      label: line.label,
      fillColor: line.type === "mean"
        ? "#161616" // Black for mean
        : line.type === "sd1"
          ? "#6fdc8c" // Green for ±1SD
          : line.type === "sd2"
            ? "#f1c21b" // Yellow for ±2SD
            : "#da1e28", // Red for ±3SD
    })),
    curve: "curveMonotoneX",
    data: {
      loading: false,
    },
  }), [height, intl, showLegend, referenceLines, data]);

  // Empty state
  if (!data || data.length === 0) {
    return (
      <div className="lj-chart-empty" data-testid="lj-chart-empty">
        {intl.formatMessage({ id: "qc.chart.noData" })}
      </div>
    );
  }

  return (
    <div className="lj-chart-container" data-testid="lj-chart">
      <LineChart
        data={chartData}
        options={options}
      />

      {/* Legend for reference lines */}
      <div className="lj-chart-legend" data-testid="lj-chart-legend">
        <div className="lj-chart-legend-item">
          <span className="lj-chart-legend-line lj-chart-legend-line--mean" />
          <span>{intl.formatMessage({ id: "qc.chart.mean" })}: {mean.toFixed(2)}</span>
        </div>
        <div className="lj-chart-legend-item">
          <span className="lj-chart-legend-line lj-chart-legend-line--sd1" />
          <span>±1SD: {sd.toFixed(2)}</span>
        </div>
        <div className="lj-chart-legend-item">
          <span className="lj-chart-legend-line lj-chart-legend-line--sd2" />
          <span>±2SD: {(2 * sd).toFixed(2)}</span>
        </div>
        <div className="lj-chart-legend-item">
          <span className="lj-chart-legend-line lj-chart-legend-line--sd3" />
          <span>±3SD: {(3 * sd).toFixed(2)}</span>
        </div>
      </div>
    </div>
  );
};

LeveyJenningsChart.propTypes = {
  data: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string,
      runDateTime: PropTypes.string,
      date: PropTypes.string,
      resultValue: PropTypes.number,
      value: PropTypes.number,
      zScore: PropTypes.number,
      violated: PropTypes.bool,
      violations: PropTypes.arrayOf(
        PropTypes.oneOfType([
          PropTypes.string,
          PropTypes.shape({
            code: PropTypes.string,
            severity: PropTypes.string,
          }),
        ])
      ),
    })
  ),
  statistics: PropTypes.shape({
    mean: PropTypes.number,
    standardDeviation: PropTypes.number,
  }),
  height: PropTypes.string,
  showLegend: PropTypes.bool,
};

LeveyJenningsChart.defaultProps = {
  data: [],
  statistics: { mean: 0, standardDeviation: 1 },
  height: "400px",
  showLegend: true,
};

export default LeveyJenningsChart;

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

const LeveyJenningsChart = ({ data, statistics, height, showLegend }) => {
  const intl = useIntl();

  // Transform data for Carbon Charts — plot z-scores on Y-axis.
  // Levey-Jennings convention (Shewhart / CLSI C24): one evenly-spaced slot per
  // run, ordered chronologically — NOT a real-time axis. Westgard rules read
  // consecutive runs, so bunching same-day runs by wall-clock time is wrong and
  // occludes points recorded close together.
  // All points go into the Normal group for a continuous line; violations are
  // duplicated into the Violation group so red dots render on top.
  const chartData = useMemo(() => {
    if (!data || data.length === 0) return [];

    const normalLabel = intl.formatMessage({ id: "qc.chart.legend.normal" });
    const violationLabel = intl.formatMessage({
      id: "qc.chart.legend.violation",
    });

    // Carbon's "labels" scale builds its domain from unique mapsTo values in
    // data order, so points must be pre-sorted by time.
    const sorted = data
      .filter((point) => point.zScore != null)
      .map((point) => ({
        point,
        date: new Date(point.runDateTime || point.date),
      }))
      .sort((a, b) => a.date - b.date);

    const points = [];

    sorted.forEach(({ point, date }, index) => {
      // Slot key = 1-based run number. It MUST be a unique per-run value:
      // Carbon's "labels" scale de-dupes its domain, so a timestamp string
      // would collapse runs recorded in the same second (batch imports / seeded
      // data) onto one slot — the exact clustering this change fixes. The run
      // number also becomes the axis label (Carbon renders the key verbatim for
      // a labels scale — ticks.formatter is ignored), matching the printed
      // Inspector report; the full date/time stays in the tooltip.
      const key = String(index + 1);

      const mapped = {
        key,
        date,
        value: point.zScore,
        rawValue: point.resultValue || point.value,
        zScore: point.zScore,
        violations: point.violations || [],
        resultId: point.id,
      };

      // Every point in Normal group for continuous line
      points.push({ ...mapped, group: normalLabel });

      // Violation points also in Violation group for red dot overlay. Shared
      // key => same slot, so the red dot lands on top of the blue point.
      if (point.violated) {
        points.push({ ...mapped, group: violationLabel });
      }
    });

    return points;
  }, [data, intl]);

  // Dynamic Y-axis domain: minimum [-4, 4], expands for outliers
  const yDomain = useMemo(() => {
    if (!data || data.length === 0) return [-4, 4];
    const zScores = data.filter((p) => p.zScore != null).map((p) => p.zScore);
    if (zScores.length === 0) return [-4, 4];
    const minZ = Math.min(...zScores);
    const maxZ = Math.max(...zScores);
    return [
      Math.min(-4, Math.floor(minZ - 0.5)),
      Math.max(4, Math.ceil(maxZ + 0.5)),
    ];
  }, [data]);

  // Reference lines at fixed z-score positions (placed inside axes.left.thresholds)
  const referenceLines = useMemo(() => {
    return [
      {
        value: 0,
        label: intl.formatMessage({ id: "qc.chart.mean" }),
        fillColor: "#161616",
      },
      { value: 1, label: "+1\u03C3", fillColor: "#a8a8a8" },
      { value: -1, label: "-1\u03C3", fillColor: "#a8a8a8" },
      { value: 2, label: "+2\u03C3", fillColor: "#f1c21b" },
      { value: -2, label: "-2\u03C3", fillColor: "#f1c21b" },
      { value: 3, label: "+3\u03C3", fillColor: "#da1e28" },
      { value: -3, label: "-3\u03C3", fillColor: "#da1e28" },
    ];
  }, [intl]);

  // Chart options
  const options = useMemo(
    () => ({
      title: "",
      height: height || "400px",
      axes: {
        bottom: {
          title: intl.formatMessage({ id: "qc.chart.axis.run" }),
          mapsTo: "key",
          scaleType: "labels",
        },
        left: {
          title: intl.formatMessage({ id: "qc.chart.axis.value" }),
          mapsTo: "value",
          scaleType: "linear",
          includeZero: true,
          domain: yDomain,
          thresholds: referenceLines,
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

          const escapeHtml = (str) => {
            const div = document.createElement("div");
            div.textContent = String(str);
            return div.innerHTML;
          };

          const point = datapoint[0];
          const originalData = data.find(
            (d) =>
              new Date(d.runDateTime || d.date).getTime() ===
              point.date?.getTime(),
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

          const rawValue = originalData?.resultValue || originalData?.value;
          const zScoreStr =
            point.value != null ? point.value.toFixed(2) + "\u03C3" : "-";

          const violations = originalData?.violations || [];
          const violationsHtml =
            violations.length > 0
              ? `<div class="lj-tooltip-violations">
               <strong>${intl.formatMessage({ id: "qc.chart.tooltip.violations" })}:</strong>
               ${violations.map((v) => `<span class="lj-tooltip-violation-tag">${escapeHtml(v.code || v)}</span>`).join(" ")}
             </div>`
              : "";

          return `
          <div class="lj-tooltip">
            <div class="lj-tooltip-row">
              <strong>${intl.formatMessage({ id: "qc.chart.tooltip.value" })}:</strong>
              <span>${rawValue != null ? escapeHtml(Number(rawValue).toFixed(2)) : "-"}</span>
            </div>
            <div class="lj-tooltip-row">
              <strong>${intl.formatMessage({ id: "qc.chart.tooltip.zScore" })}:</strong>
              <span>${escapeHtml(zScoreStr)}</span>
            </div>
            <div class="lj-tooltip-row">
              <strong>${intl.formatMessage({ id: "qc.chart.tooltip.date" })}:</strong>
              <span>${escapeHtml(dateStr)}</span>
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
      // L-J charts use straight segments between runs; a smoothed curve would
      // imply interpolated values that never existed.
      curve: "curveLinear",
      data: {
        loading: false,
      },
    }),
    [height, intl, showLegend, referenceLines, data, yDomain],
  );

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
      <LineChart data={chartData} options={options} />

      {/* Legend for reference lines */}
      <div className="lj-chart-legend" data-testid="lj-chart-legend">
        <div className="lj-chart-legend-item">
          <span className="lj-chart-legend-line lj-chart-legend-line--mean" />
          <span>{intl.formatMessage({ id: "qc.chart.mean" })}</span>
        </div>
        <div className="lj-chart-legend-item">
          <span className="lj-chart-legend-line lj-chart-legend-line--sd1" />
          <span>&plusmn;1&sigma;</span>
        </div>
        <div className="lj-chart-legend-item">
          <span className="lj-chart-legend-line lj-chart-legend-line--sd2" />
          <span>&plusmn;2&sigma;</span>
        </div>
        <div className="lj-chart-legend-item">
          <span className="lj-chart-legend-line lj-chart-legend-line--sd3" />
          <span>&plusmn;3&sigma;</span>
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
        ]),
      ),
    }),
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

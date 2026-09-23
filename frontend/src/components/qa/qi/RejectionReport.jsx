import React, { useState } from "react";
import { DonutChart, LineChart, SimpleBarChart } from "@carbon/charts-react";
import "@carbon/charts/styles.css";
import { FormattedMessage, useIntl } from "react-intl";
import { Link } from "react-router-dom";
import { toLocalIsoDateTime } from "../../utils/Utils";
import { useServerData } from "../../utils/useServerData";
import QASimpleTable from "../common/QASimpleTable";
import { chartThresholds, rateTone } from "./qiThresholds";
import QIReportPage, {
  QIRateHeader,
  QITrendInterval,
  rateChartOptions,
} from "./QIReportPage";

/**
 * Rejection Rate detail page (OGC-697 tile, full visuals OGC-710) at
 * /qa/qi/rejection: rate header colored against qi_config thresholds, rate
 * trend with target/action lines, reason Pareto (rejection reasons are
 * dictionary-driven, unlike amendments), per-test breakdown, and the
 * rejection list.
 */

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sideNav.label.qa", link: "/qa/overview" },
  { label: "sideNav.label.qa.qi.dashboard", link: "/qa/qi/dashboard" },
  { label: "qa.qi.dashboard.tile.rejection.label", link: "" },
];

const HEADERS = [
  { key: "rejectedAt", labelKey: "qa.qi.rejection.column.rejectedAt" },
  { key: "labNumber", labelKey: "common.labNumber" },
  { key: "testName", labelKey: "common.test" },
  { key: "location", labelKey: "common.location" },
  { key: "reason", labelKey: "storage.audit.reason" },
  { key: "rejectedBy", labelKey: "qa.qi.rejection.column.rejectedBy" },
  { key: "nce", labelKey: "label.validation.filter.nce" },
];

const REASON_HEADERS = [
  { key: "reason", labelKey: "storage.audit.reason" },
  { key: "count", labelKey: "reports.tat.column.count" },
  { key: "percent", labelKey: "qa.qi.rejection.reasons.column.percent" },
  {
    key: "cumulative",
    labelKey: "qa.qi.rejection.reasons.column.cumulative",
  },
];

const BREAKDOWN_HEADERS = [
  { key: "testName", labelKey: "common.test" },
  {
    key: "rejectedCount",
    labelKey: "sampleAcceptance.qa.status.rejected",
  },
  { key: "totalCount", labelKey: "reports.tat.column.started" },
  { key: "ratePercent", labelKey: "qa.qi.amendment.breakdown.column.rate" },
];

// The design's one line of guidance: how few reasons cover >=80% of
// rejections. Only meaningful once there is more than one reason and the
// cumulative percentages are present.
function paretoInsight(reasons) {
  if (reasons.length < 2) {
    return null;
  }
  const index = reasons.findIndex((r) => (r.cumulativePercent ?? 0) >= 80);
  const top = index === -1 ? reasons.length - 1 : index;
  if (reasons[top].cumulativePercent == null) {
    return null;
  }
  return {
    count: top + 1,
    total: reasons.length,
    percent: reasons[top].cumulativePercent.toFixed(0),
    reason: reasons[0].reason,
  };
}

const toRows = (detail) =>
  (detail.items || []).map((item, index) => ({
    id: `${item.analysisId}-${index}`,
    rejectedAt: toLocalIsoDateTime(item.rejectedAt),
    labNumber: item.labNumber || "—",
    testName: item.testName || "—",
    location: item.location || "—",
    reason: item.reason || "—",
    rejectedBy: item.rejectedBy || "—",
    nce: item.nceNumber ? (
      <Link
        to={`/ViewNonConformingEvent?nceNumber=${encodeURIComponent(item.nceNumber)}`}
      >
        {item.nceNumber}
      </Link>
    ) : (
      "—"
    ),
  }));

/** Trend, reason Pareto, ordering-location heatmap and per-test breakdown. */
const RejectionSections = ({ range, config }) => {
  const intl = useIntl();
  const [interval, setInterval] = useState("DAILY");
  const trendQuery = useServerData(
    `/rest/reports/rejection/trend?fromDate=${range.fromDate}` +
      `&toDate=${range.toDate}&interval=${interval}`,
  );
  const breakdownQuery = useServerData(
    `/rest/reports/rejection/breakdown?fromDate=${range.fromDate}` +
      `&toDate=${range.toDate}`,
  );
  const heatmapQuery = useServerData(
    `/rest/reports/rejection/heatmap?fromDate=${range.fromDate}` +
      `&toDate=${range.toDate}`,
  );

  // Window totals derive from the trend buckets — same SQL predicates as the
  // summary endpoint, just grouped; no separate summary fetch needed.
  const points = trendQuery.data?.points || [];
  const totalRejected = points.reduce((sum, p) => sum + p.rejectedCount, 0);
  const totalStarted = points.reduce((sum, p) => sum + p.totalCount, 0);
  const windowRate =
    totalStarted > 0
      ? Math.round((totalRejected * 10000) / totalStarted) / 100 // 2dp, like the backend
      : null;

  const timeAxis = interval === "DAILY";
  const chartData = points
    .filter((p) => p.ratePercent != null)
    .map((p) => ({
      period: timeAxis ? new Date(`${p.period}T00:00:00`) : p.period,
      value: p.ratePercent,
      group: intl.formatMessage({ id: "qa.qi.rejection.rate.label" }),
    }));

  const chartOptions = rateChartOptions(
    chartThresholds(
      config,
      intl.formatMessage({ id: "qa.qiConfig.field.target" }),
      intl.formatMessage({ id: "common.action" }),
    ),
    timeAxis,
  );

  const breakdown = breakdownQuery.data;
  const reasons = breakdown?.reasons || [];
  const donutData = reasons.map((row) => ({
    group: row.reason,
    value: row.count,
  }));
  const donutOptions = {
    title: "",
    height: "260px",
    donut: {
      center: {
        label: intl.formatMessage({
          id: "qa.qi.rejection.reasons.donut.label",
        }),
      },
      alignment: "center",
    },
    legend: { alignment: "center" },
    toolbar: { enabled: false },
  };

  const insight = paretoInsight(reasons);

  // By-test bars: top 7 + an aggregated tail, per the design; the table below
  // keeps the full list with started counts and rates.
  const tests = breakdown?.tests || [];
  const barData = tests.slice(0, 7).map((row) => ({
    group: row.testName,
    value: row.rejectedCount,
  }));
  if (tests.length > 7) {
    barData.push({
      group: intl.formatMessage(
        { id: "qa.qi.rejection.breakdown.other" },
        { count: tests.length - 7 },
      ),
      value: tests.slice(7).reduce((sum, row) => sum + row.rejectedCount, 0),
    });
  }
  const barOptions = {
    title: "",
    height: `${Math.max(barData.length * 40 + 60, 140)}px`,
    axes: {
      left: { mapsTo: "group", scaleType: "labels" },
      bottom: { mapsTo: "value", includeZero: true },
    },
    legend: { enabled: false },
    toolbar: { enabled: false },
  };

  const reasonRows = reasons.map((row, index) => ({
    id: `${row.reason}-${index}`,
    reason: row.reason,
    count: row.count,
    percent:
      row.percentOfRejections != null
        ? `${row.percentOfRejections.toFixed(2)}%`
        : "—",
    cumulative:
      row.cumulativePercent != null
        ? `${row.cumulativePercent.toFixed(2)}%`
        : "—",
  }));

  const breakdownRows = tests.map((row, index) => ({
    id: `${row.testName}-${index}`,
    testName: row.testName,
    rejectedCount: row.rejectedCount,
    totalCount: row.totalCount,
    ratePercent:
      row.ratePercent != null ? `${row.ratePercent.toFixed(2)}%` : "—",
  }));

  // Pivot the flat heatmap cells into sections × locations; null location /
  // section are the "not captured" buckets, kept last.
  const unknownLocation = intl.formatMessage({
    id: "qa.qi.rejection.heatmap.unknown",
  });
  const unknownSection = intl.formatMessage({
    id: "qa.qi.rejection.heatmap.unknownSection",
  });
  const heatCells = (heatmapQuery.data?.cells || []).map((cell) => ({
    ...cell,
    location: cell.location || unknownLocation,
    section: cell.section || unknownSection,
  }));
  const heatLocations = [...new Set(heatCells.map((c) => c.location))].sort(
    (a, b) =>
      (a === unknownLocation) - (b === unknownLocation) || a.localeCompare(b),
  );
  const heatSections = [...new Set(heatCells.map((c) => c.section))].sort(
    (a, b) =>
      (a === unknownSection) - (b === unknownSection) || a.localeCompare(b),
  );
  const heatCellMap = new Map(
    heatCells.map((c) => [`${c.location}|${c.section}`, c]),
  );

  return (
    <>
      {trendQuery.isError ? (
        <p className="qi-tile__message">
          <FormattedMessage id="qa.qi.dashboard.tile.rejection.error" />
        </p>
      ) : (
        trendQuery.data && (
          <>
            <QIRateHeader
              label={<FormattedMessage id="qa.qi.rejection.rate.label" />}
              value={windowRate}
              tone={rateTone(windowRate, config)}
              secondary={
                <FormattedMessage
                  id="qa.qi.dashboard.tile.rejection.secondary"
                  values={{ rejected: totalRejected, total: totalStarted }}
                />
              }
            />

            <h4 className="amendment-section__title">
              <FormattedMessage id="reports.tat.trend" />
            </h4>
            <QITrendInterval
              id="rejection-trend-interval"
              titleKey="reports.tat.aggregation"
              interval={interval}
              onChange={setInterval}
            />
            {chartData.length === 0 ? (
              <p className="qi-tile__message">
                <FormattedMessage id="qa.qi.amendment.trend.empty" />
              </p>
            ) : (
              <LineChart data={chartData} options={chartOptions} />
            )}
          </>
        )
      )}

      {reasonRows.length > 0 && (
        <>
          <h4 className="amendment-section__title">
            <FormattedMessage id="qa.qi.rejection.reasons.title" />
          </h4>
          {insight && (
            <p className="qi-tile__secondary">
              <FormattedMessage
                id="qa.qi.rejection.reasons.insight"
                values={insight}
              />
            </p>
          )}
          <DonutChart data={donutData} options={donutOptions} />
          <QASimpleTable rows={reasonRows} headers={REASON_HEADERS} />
        </>
      )}

      {heatCells.length > 0 && (
        <>
          <h4 className="amendment-section__title">
            <FormattedMessage id="qa.qi.rejection.heatmap.title" />
          </h4>
          <div className="qi-heatmap">
            <table>
              <thead>
                <tr>
                  <th>
                    <FormattedMessage id="qa.qi.rejection.heatmap.section" />
                  </th>
                  {heatLocations.map((location) => (
                    <th key={location}>{location}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {heatSections.map((section) => (
                  <tr key={section}>
                    <th scope="row">{section}</th>
                    {heatLocations.map((location) => {
                      const cell = heatCellMap.get(`${location}|${section}`);
                      const tone =
                        cell?.ratePercent != null
                          ? rateTone(cell.ratePercent, config)
                          : null;
                      return (
                        <td
                          key={location}
                          className={
                            tone && tone !== "gray"
                              ? `qi-heatmap__cell--${tone}`
                              : undefined
                          }
                          title={
                            cell
                              ? `${cell.rejectedCount} / ${cell.totalCount}`
                              : undefined
                          }
                        >
                          {cell?.ratePercent != null
                            ? `${cell.ratePercent.toFixed(1)}%`
                            : "—"}
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {config?.enabled &&
            config.target != null &&
            config.action != null && (
              <p className="qi-tile__secondary">
                <FormattedMessage
                  id="qa.qi.rejection.heatmap.legend"
                  values={{ target: config.target, action: config.action }}
                />
              </p>
            )}
        </>
      )}

      {breakdownRows.length > 0 && (
        <>
          <h4 className="amendment-section__title">
            <FormattedMessage id="qa.qi.amendment.breakdown.title" />
          </h4>
          <SimpleBarChart data={barData} options={barOptions} />
          <QASimpleTable rows={breakdownRows} headers={BREAKDOWN_HEADERS} />
        </>
      )}
    </>
  );
};

const RejectionReport = () => (
  <QIReportPage
    indicator="REJECTION"
    breadcrumbs={breadcrumbs}
    titleKey="qa.qi.rejection.title"
    subtitleKey="qa.qi.rejection.subtitle"
    errorKey="qa.qi.dashboard.tile.rejection.error"
    idPrefix="rejection"
    fromLabelKey="common.from"
    toLabelKey="to.title"
    detailUrl={(range, page, pageSize) =>
      `/rest/reports/rejection/detail?fromDate=${range.fromDate}` +
      `&toDate=${range.toDate}&page=${page}&pageSize=${pageSize}`
    }
    headers={HEADERS}
    toRows={toRows}
    listTitleKey="qa.qi.rejection.list.title"
    emptyTitleKey="qa.empty.rejection.title"
    emptySubheadKey="qa.empty.rejection.subhead"
  >
    {({ range, config }) => <RejectionSections range={range} config={config} />}
  </QIReportPage>
);

export default RejectionReport;

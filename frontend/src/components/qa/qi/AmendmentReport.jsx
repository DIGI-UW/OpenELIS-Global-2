import React, { useState } from "react";
import { LineChart } from "@carbon/charts-react";
import "@carbon/charts/styles.css";
import { FormattedMessage, useIntl } from "react-intl";
import { toLocalIsoDateTime } from "../../utils/Utils";
import { useServerData } from "../../utils/useServerData";
import QASimpleTable from "../common/QASimpleTable";
import { formatMinutes } from "../common/qaDates";
import { chartThresholds, rateTone } from "./qiThresholds";
import QIReportPage, {
  QIRateHeader,
  QITrendInterval,
  rateChartOptions,
} from "./QIReportPage";

/**
 * Amendment Rate detail page (OGC-698, full visuals OGC-710) at
 * /qa/qi/amendment: rate header colored against qi_config thresholds, rate
 * trend with target/action lines, per-test breakdown, and the amendment list
 * with prior/current values from the result audit history. No Pareto — no
 * amendment-reason data exists until OGC-713.
 */

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sideNav.label.qa", link: "/qa/overview" },
  { label: "sideNav.label.qa.qi.dashboard", link: "/qa/qi/dashboard" },
  { label: "qa.qi.dashboard.tile.amendment.label", link: "" },
];

const HEADERS = [
  { key: "amendedAt", labelKey: "qa.qi.amendment.column.amendedAt" },
  { key: "labNumber", labelKey: "common.labNumber" },
  { key: "testName", labelKey: "common.test" },
  { key: "priorValue", labelKey: "qa.qi.amendment.column.priorValue" },
  { key: "currentValue", labelKey: "qa.qi.amendment.column.currentValue" },
  { key: "amendedBy", labelKey: "qa.qi.amendment.column.amendedBy" },
  { key: "releasedAt", labelKey: "qa.qi.amendment.column.releasedAt" },
  { key: "timeToAmend", labelKey: "qa.qi.amendment.column.timeToAmend" },
];

const BREAKDOWN_HEADERS = [
  { key: "testName", labelKey: "common.test" },
  { key: "amendedCount", labelKey: "microbiology.enum.AMENDED" },
  {
    key: "releasedCount",
    labelKey: "microbiology.enum.RELEASED",
  },
  { key: "ratePercent", labelKey: "qa.qi.amendment.breakdown.column.rate" },
];

const toRows = (detail) =>
  (detail.items || []).map((item, index) => ({
    id: `${item.analysisId}-${index}`,
    amendedAt: toLocalIsoDateTime(item.amendedAt),
    labNumber: item.labNumber || "—",
    testName: item.testName || "—",
    priorValue: item.priorValue ?? "—",
    currentValue: item.currentValue ?? "—",
    amendedBy: item.amendedBy || "—",
    releasedAt: toLocalIsoDateTime(item.releasedAt),
    timeToAmend: formatMinutes(item.minutesToAmend),
  }));

/** Trend chart + window rate + per-test breakdown for the selected range. */
const AmendmentSections = ({ range, config }) => {
  const intl = useIntl();
  const [interval, setInterval] = useState("DAILY");
  const trendQuery = useServerData(
    `/rest/reports/amendment/trend?fromDate=${range.fromDate}` +
      `&toDate=${range.toDate}&interval=${interval}`,
  );
  const breakdownQuery = useServerData(
    `/rest/reports/amendment/breakdown?fromDate=${range.fromDate}` +
      `&toDate=${range.toDate}`,
  );

  // Window totals derive from the trend buckets — same SQL predicates as the
  // summary endpoint, just grouped; no separate summary fetch needed.
  const points = trendQuery.data?.points || [];
  const totalAmended = points.reduce((sum, p) => sum + p.amendedCount, 0);
  const totalReleased = points.reduce((sum, p) => sum + p.releasedCount, 0);
  const windowRate =
    totalReleased > 0
      ? Math.round((totalAmended * 10000) / totalReleased) / 100 // 2dp, like the backend
      : null;

  const chartData = points
    .filter((p) => p.ratePercent != null)
    .map((p) => ({
      period: p.period,
      value: p.ratePercent,
      group: intl.formatMessage({ id: "qa.qi.amendment.rate.label" }),
    }));

  const chartOptions = rateChartOptions(
    chartThresholds(
      config,
      intl.formatMessage({ id: "qa.qiConfig.field.target" }),
      intl.formatMessage({ id: "common.action" }),
    ),
  );

  const breakdownRows = (breakdownQuery.data?.rows || []).map((row, index) => ({
    id: `${row.testName}-${index}`,
    testName: row.testName,
    amendedCount: row.amendedCount,
    releasedCount: row.releasedCount,
    ratePercent:
      row.ratePercent != null ? `${row.ratePercent.toFixed(2)}%` : "—",
  }));

  return (
    <>
      {trendQuery.isError ? (
        <p className="qi-tile__message">
          <FormattedMessage id="qa.qi.dashboard.tile.amendment.error" />
        </p>
      ) : (
        trendQuery.data && (
          <>
            <QIRateHeader
              label={<FormattedMessage id="qa.qi.amendment.rate.label" />}
              value={windowRate}
              tone={rateTone(windowRate, config)}
              secondary={
                <FormattedMessage
                  id="qa.qi.dashboard.tile.amendment.secondary"
                  values={{ amended: totalAmended, released: totalReleased }}
                />
              }
            />

            <h4 className="amendment-section__title">
              <FormattedMessage id="reports.tat.trend" />
            </h4>
            <QITrendInterval
              id="amendment-trend-interval"
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

      {breakdownRows.length > 0 && (
        <>
          <h4 className="amendment-section__title">
            <FormattedMessage id="qa.qi.amendment.breakdown.title" />
          </h4>
          <QASimpleTable rows={breakdownRows} headers={BREAKDOWN_HEADERS} />
        </>
      )}
    </>
  );
};

const AmendmentReport = () => (
  <QIReportPage
    indicator="AMENDMENT"
    breadcrumbs={breadcrumbs}
    titleKey="qa.qi.amendment.title"
    subtitleKey="qa.qi.amendment.subtitle"
    errorKey="qa.qi.dashboard.tile.amendment.error"
    idPrefix="amendment"
    fromLabelKey="common.from"
    toLabelKey="to.title"
    detailUrl={(range, page, pageSize) =>
      `/rest/reports/amendment/detail?fromDate=${range.fromDate}` +
      `&toDate=${range.toDate}&page=${page}&pageSize=${pageSize}`
    }
    headers={HEADERS}
    toRows={toRows}
    listTitleKey="qa.qi.amendment.list.title"
    emptyTitleKey="qa.empty.amendment.title"
    emptySubheadKey="qa.empty.amendment.subhead"
  >
    {({ range, config }) => <AmendmentSections range={range} config={config} />}
  </QIReportPage>
);

export default AmendmentReport;

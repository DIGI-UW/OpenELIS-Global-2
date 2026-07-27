import React, { useCallback, useEffect, useState } from "react";
import {
  DataTable,
  DataTableSkeleton,
  DatePicker,
  DatePickerInput,
  Dropdown,
  Pagination,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
} from "@carbon/react";
import { DonutChart, LineChart, SimpleBarChart } from "@carbon/charts-react";
import "@carbon/charts/styles.css";
import { FormattedMessage, useIntl } from "react-intl";
import { Link } from "react-router-dom";
import {
  getFromOpenElisServer,
  toLocalIsoDate,
  toLocalIsoDateTime,
} from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import QAEmptyState from "../common/QAEmptyState";
import { chartThresholds, rateTone } from "./qiThresholds";
import "./QIDashboard.css";

/**
 * Rejection Rate detail page (OGC-697 tile, full visuals OGC-710) at
 * /qa/qi/rejection: rate header colored against qi_config thresholds, rate
 * trend with target/action lines, reason Pareto (rejection reasons are
 * dictionary-driven, unlike amendments), per-test breakdown, and the
 * rejection list. Mirrors AmendmentReport so the two detail pages stay
 * reviewable side by side.
 */

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sideNav.label.qa", link: "/qa/overview" },
  { label: "sideNav.label.qa.qi.dashboard", link: "/qa/qi/dashboard" },
  { label: "qa.qi.dashboard.tile.rejection.label", link: "" },
];

const HEADERS = [
  { key: "rejectedAt", labelKey: "qa.qi.rejection.column.rejectedAt" },
  { key: "labNumber", labelKey: "qa.qi.rejection.column.labNumber" },
  { key: "testName", labelKey: "qa.qi.rejection.column.test" },
  { key: "location", labelKey: "qa.qi.rejection.column.location" },
  { key: "reason", labelKey: "qa.qi.rejection.column.reason" },
  { key: "rejectedBy", labelKey: "qa.qi.rejection.column.rejectedBy" },
  { key: "nce", labelKey: "qa.qi.rejection.column.nce" },
];

const REASON_HEADERS = [
  { key: "reason", labelKey: "qa.qi.rejection.reasons.column.reason" },
  { key: "count", labelKey: "qa.qi.rejection.reasons.column.count" },
  { key: "percent", labelKey: "qa.qi.rejection.reasons.column.percent" },
  {
    key: "cumulative",
    labelKey: "qa.qi.rejection.reasons.column.cumulative",
  },
];

const BREAKDOWN_HEADERS = [
  { key: "testName", labelKey: "qa.qi.rejection.breakdown.column.test" },
  {
    key: "rejectedCount",
    labelKey: "qa.qi.rejection.breakdown.column.rejected",
  },
  { key: "totalCount", labelKey: "qa.qi.rejection.breakdown.column.total" },
  { key: "ratePercent", labelKey: "qa.qi.rejection.breakdown.column.rate" },
];

const INTERVALS = [
  { id: "DAILY", labelKey: "reports.tat.daily" },
  { id: "WEEKLY", labelKey: "reports.tat.weekly" },
  { id: "MONTHLY", labelKey: "reports.tat.monthly" },
];

function defaultRange() {
  const to = new Date();
  const from = new Date();
  from.setDate(from.getDate() - 30);
  return { fromDate: toLocalIsoDate(from), toDate: toLocalIsoDate(to) };
}

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

const RejectionReport = () => {
  const intl = useIntl();
  const [range, setRange] = useState(defaultRange);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(25);
  // undefined = loading, null = fetch yielded no data
  const [detail, setDetail] = useState();
  const [trend, setTrend] = useState();
  const [breakdown, setBreakdown] = useState();
  const [heatmap, setHeatmap] = useState();
  const [interval, setInterval] = useState("DAILY");
  // fail-open like QIDashboard/QIEnabledRoute: no config -> plain gray tag
  const [config, setConfig] = useState(null);

  const fetchDetail = useCallback(() => {
    setDetail(undefined);
    getFromOpenElisServer(
      `/rest/reports/rejection/detail?fromDate=${range.fromDate}` +
        `&toDate=${range.toDate}&page=${page}&pageSize=${pageSize}`,
      (res) => setDetail(res ?? null),
    );
  }, [range, page, pageSize]);

  useEffect(() => {
    fetchDetail();
  }, [fetchDetail]);

  useEffect(() => {
    setTrend(undefined);
    getFromOpenElisServer(
      `/rest/reports/rejection/trend?fromDate=${range.fromDate}` +
        `&toDate=${range.toDate}&interval=${interval}`,
      (res) => setTrend(res ?? null),
    );
  }, [range, interval]);

  useEffect(() => {
    setBreakdown(undefined);
    getFromOpenElisServer(
      `/rest/reports/rejection/breakdown?fromDate=${range.fromDate}` +
        `&toDate=${range.toDate}`,
      (res) => setBreakdown(res ?? null),
    );
  }, [range]);

  useEffect(() => {
    setHeatmap(undefined);
    getFromOpenElisServer(
      `/rest/reports/rejection/heatmap?fromDate=${range.fromDate}` +
        `&toDate=${range.toDate}`,
      (res) => setHeatmap(res ?? null),
    );
  }, [range]);

  useEffect(() => {
    getFromOpenElisServer(
      "/rest/qi-config/resolve?indicator=REJECTION",
      (res) => setConfig(res ?? null),
    );
  }, []);

  const handleDates = (dates) => {
    if (dates.length === 2) {
      setPage(0);
      setRange({
        fromDate: toLocalIsoDate(dates[0]),
        toDate: toLocalIsoDate(dates[1]),
      });
    }
  };

  // Window totals derive from the trend buckets — same SQL predicates as the
  // summary endpoint, just grouped; no separate summary fetch needed.
  const points = trend?.points || [];
  const totalRejected = points.reduce((sum, p) => sum + p.rejectedCount, 0);
  const totalStarted = points.reduce((sum, p) => sum + p.totalCount, 0);
  const windowRate =
    totalStarted > 0
      ? Math.round((totalRejected * 10000) / totalStarted) / 100 // 2dp, like the backend
      : null;

  const tone = rateTone(windowRate, config);

  // Daily periods are real dates — a time axis spaces them honestly (a
  // 4-day gap looks like a gap, not one tick). Weekly/monthly keys
  // ("2026-W30", "2026-07") aren't parseable dates, so those stay labels.
  const timeAxis = interval === "DAILY";
  const chartData = points
    .filter((p) => p.ratePercent != null)
    .map((p) => ({
      period: timeAxis ? new Date(`${p.period}T00:00:00`) : p.period,
      value: p.ratePercent,
      group: intl.formatMessage({ id: "qa.qi.rejection.trend.series" }),
    }));

  const thresholds = chartThresholds(
    config,
    intl.formatMessage({ id: "qa.qi.rejection.threshold.target" }),
    intl.formatMessage({ id: "qa.qi.rejection.threshold.action" }),
  );

  const chartOptions = {
    title: "",
    height: "320px",
    axes: {
      bottom: { mapsTo: "period", scaleType: timeAxis ? "time" : "labels" },
      left: {
        title: "%",
        mapsTo: "value",
        scaleType: "linear",
        includeZero: true,
        thresholds,
      },
    },
    curve: "curveMonotoneX",
    points: { radius: 3, filled: true },
    legend: { enabled: false },
  };

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

  const reasonRows = (breakdown?.reasons || []).map((row, index) => ({
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

  const breakdownRows = (breakdown?.tests || []).map((row, index) => ({
    id: `${row.testName}-${index}`,
    testName: row.testName,
    rejectedCount: row.rejectedCount,
    totalCount: row.totalCount,
    ratePercent:
      row.ratePercent != null ? `${row.ratePercent.toFixed(2)}%` : "—",
  }));

  const rows = (detail?.items || []).map((item, index) => ({
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

  // Pivot the flat heatmap cells into sections × locations; null location /
  // section are the "not captured" buckets, kept last.
  const unknownLocation = intl.formatMessage({
    id: "qa.qi.rejection.heatmap.unknown",
  });
  const unknownSection = intl.formatMessage({
    id: "qa.qi.rejection.heatmap.unknownSection",
  });
  const heatCells = (heatmap?.cells || []).map((cell) => ({
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

  const renderTable = (tableRows, headers) => (
    <DataTable
      rows={tableRows}
      headers={headers.map((h) => ({
        key: h.key,
        header: intl.formatMessage({ id: h.labelKey }),
      }))}
    >
      {({
        rows: bodyRows,
        headers: tableHeaders,
        getHeaderProps,
        getRowProps,
      }) => (
        <TableContainer>
          <Table size="sm">
            <TableHead>
              <TableRow>
                {tableHeaders.map((header) => (
                  <TableHeader {...getHeaderProps({ header })} key={header.key}>
                    {header.header}
                  </TableHeader>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {bodyRows.map((row) => (
                <TableRow {...getRowProps({ row })} key={row.id}>
                  {row.cells.map((cell) => (
                    <TableCell key={cell.id}>{cell.value}</TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </DataTable>
  );

  return (
    <div className="pageContent qi-dashboard">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <h2>
        <FormattedMessage id="qa.qi.rejection.title" />
      </h2>
      <p className="qi-dashboard__subtitle">
        <FormattedMessage id="qa.qi.rejection.subtitle" />
      </p>
      <DatePicker
        datePickerType="range"
        dateFormat="Y-m-d"
        value={[range.fromDate, range.toDate]}
        onChange={handleDates}
      >
        <DatePickerInput
          id="rejection-from"
          labelText={intl.formatMessage({
            id: "qa.qi.rejection.filter.from",
          })}
          placeholder="yyyy-mm-dd"
        />
        <DatePickerInput
          id="rejection-to"
          labelText={intl.formatMessage({ id: "qa.qi.rejection.filter.to" })}
          placeholder="yyyy-mm-dd"
        />
      </DatePicker>

      {trend === null ? (
        <p className="qi-tile__message">
          <FormattedMessage id="qa.qi.rejection.error" />
        </p>
      ) : (
        trend !== undefined && (
          <>
            <div className="amendment-rate-header">
              <span className="qi-tile__title">
                <FormattedMessage id="qa.qi.rejection.rate.label" />
              </span>
              <Tag
                type={tone === "amber" ? "gray" : tone}
                className={
                  tone === "amber"
                    ? "amendment-rate-tag qi-rate-tag--amber"
                    : "amendment-rate-tag"
                }
              >
                {windowRate != null ? `${windowRate.toFixed(2)}%` : "—"}
              </Tag>
              <span className="qi-tile__secondary">
                <FormattedMessage
                  id="qa.qi.dashboard.tile.rejection.secondary"
                  values={{ rejected: totalRejected, total: totalStarted }}
                />
              </span>
            </div>

            <h4 className="amendment-section__title">
              <FormattedMessage id="qa.qi.rejection.trend.title" />
            </h4>
            <Dropdown
              id="rejection-trend-interval"
              size="sm"
              className="amendment-trend-interval"
              titleText={intl.formatMessage({
                id: "qa.qi.rejection.trend.interval",
              })}
              label=""
              items={INTERVALS}
              itemToString={(item) =>
                item ? intl.formatMessage({ id: item.labelKey }) : ""
              }
              selectedItem={INTERVALS.find((i) => i.id === interval)}
              onChange={({ selectedItem }) => setInterval(selectedItem.id)}
            />
            {chartData.length === 0 ? (
              <p className="qi-tile__message">
                <FormattedMessage id="qa.qi.rejection.trend.empty" />
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
          {renderTable(reasonRows, REASON_HEADERS)}
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
            <FormattedMessage id="qa.qi.rejection.breakdown.title" />
          </h4>
          <SimpleBarChart data={barData} options={barOptions} />
          {renderTable(breakdownRows, BREAKDOWN_HEADERS)}
        </>
      )}

      {detail === undefined ? (
        <DataTableSkeleton columnCount={HEADERS.length} rowCount={5} />
      ) : detail === null ? (
        <p className="qi-tile__message">
          <FormattedMessage id="qa.qi.rejection.error" />
        </p>
      ) : rows.length === 0 ? (
        <QAEmptyState
          titleKey="qa.empty.rejection.title"
          subheadKey="qa.empty.rejection.subhead"
        />
      ) : (
        <>
          <h4 className="amendment-section__title">
            <FormattedMessage id="qa.qi.rejection.list.title" />
          </h4>
          {renderTable(rows, HEADERS)}
          <Pagination
            page={page + 1}
            pageSize={pageSize}
            pageSizes={[25, 50, 100]}
            totalItems={detail.totalCount}
            onChange={({ page: newPage, pageSize: newPageSize }) => {
              setPage(newPage - 1);
              setPageSize(newPageSize);
            }}
          />
        </>
      )}
    </div>
  );
};

export default RejectionReport;

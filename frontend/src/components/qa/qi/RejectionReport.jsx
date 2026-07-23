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
import { LineChart } from "@carbon/charts-react";
import "@carbon/charts/styles.css";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import QAEmptyState from "../common/QAEmptyState";
import { rateTone } from "./qiThresholds";
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
  { label: "sideNav.label.qa", link: "" },
  { label: "sideNav.label.qa.qi.dashboard", link: "/qa/qi/dashboard" },
  { label: "qa.qi.dashboard.tile.rejection.label", link: "" },
];

const HEADERS = [
  { key: "rejectedAt", labelKey: "qa.qi.rejection.column.rejectedAt" },
  { key: "labNumber", labelKey: "qa.qi.rejection.column.labNumber" },
  { key: "testName", labelKey: "qa.qi.rejection.column.test" },
  { key: "reason", labelKey: "qa.qi.rejection.column.reason" },
  { key: "rejectedBy", labelKey: "qa.qi.rejection.column.rejectedBy" },
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

function formatDate(d) {
  return d.toISOString().split("T")[0];
}

function defaultRange() {
  const to = new Date();
  const from = new Date();
  from.setDate(from.getDate() - 30);
  return { fromDate: formatDate(from), toDate: formatDate(to) };
}

function formatTimestamp(value) {
  return value ? new Date(value).toLocaleString() : "—";
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
    getFromOpenElisServer(
      "/rest/qi-config/resolve?indicator=REJECTION",
      (res) => setConfig(res ?? null),
    );
  }, []);

  const handleDates = (dates) => {
    if (dates.length === 2) {
      setPage(0);
      setRange({
        fromDate: formatDate(dates[0]),
        toDate: formatDate(dates[1]),
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

  const chartData = points
    .filter((p) => p.ratePercent != null)
    .map((p) => ({
      period: p.period,
      value: p.ratePercent,
      group: intl.formatMessage({ id: "qa.qi.rejection.trend.series" }),
    }));

  const thresholds =
    config?.enabled && config.target != null && config.action != null
      ? [
          {
            value: config.target,
            label: intl.formatMessage({
              id: "qa.qi.rejection.threshold.target",
            }),
            fillColor: "#198038",
          },
          {
            value: config.action,
            label: intl.formatMessage({
              id: "qa.qi.rejection.threshold.action",
            }),
            fillColor: "#da1e28",
          },
        ]
      : undefined;

  const chartOptions = {
    title: "",
    height: "320px",
    axes: {
      bottom: { mapsTo: "period", scaleType: "labels" },
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
    rejectedAt: formatTimestamp(item.rejectedAt),
    labNumber: item.labNumber || "—",
    testName: item.testName || "—",
    reason: item.reason || "—",
    rejectedBy: item.rejectedBy || "—",
  }));

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
    <div className="adminPageContent qi-dashboard">
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
          {renderTable(reasonRows, REASON_HEADERS)}
        </>
      )}

      {breakdownRows.length > 0 && (
        <>
          <h4 className="amendment-section__title">
            <FormattedMessage id="qa.qi.rejection.breakdown.title" />
          </h4>
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

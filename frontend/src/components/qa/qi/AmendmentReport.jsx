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
  { key: "labNumber", labelKey: "qa.qi.amendment.column.labNumber" },
  { key: "testName", labelKey: "qa.qi.amendment.column.test" },
  { key: "priorValue", labelKey: "qa.qi.amendment.column.priorValue" },
  { key: "currentValue", labelKey: "qa.qi.amendment.column.currentValue" },
  { key: "amendedBy", labelKey: "qa.qi.amendment.column.amendedBy" },
  { key: "releasedAt", labelKey: "qa.qi.amendment.column.releasedAt" },
  { key: "timeToAmend", labelKey: "qa.qi.amendment.column.timeToAmend" },
];

const BREAKDOWN_HEADERS = [
  { key: "testName", labelKey: "qa.qi.amendment.breakdown.column.test" },
  { key: "amendedCount", labelKey: "qa.qi.amendment.breakdown.column.amended" },
  {
    key: "releasedCount",
    labelKey: "qa.qi.amendment.breakdown.column.released",
  },
  { key: "ratePercent", labelKey: "qa.qi.amendment.breakdown.column.rate" },
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

function formatMinutes(minutes) {
  if (minutes == null) {
    return "—";
  }
  if (minutes < 60) {
    return `${minutes}m`;
  }
  const hours = Math.floor(minutes / 60);
  if (hours < 24) {
    return `${hours}h ${minutes % 60}m`;
  }
  return `${Math.floor(hours / 24)}d ${hours % 24}h`;
}

const AmendmentReport = () => {
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
      `/rest/reports/amendment/detail?fromDate=${range.fromDate}` +
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
      `/rest/reports/amendment/trend?fromDate=${range.fromDate}` +
        `&toDate=${range.toDate}&interval=${interval}`,
      (res) => setTrend(res ?? null),
    );
  }, [range, interval]);

  useEffect(() => {
    setBreakdown(undefined);
    getFromOpenElisServer(
      `/rest/reports/amendment/breakdown?fromDate=${range.fromDate}` +
        `&toDate=${range.toDate}`,
      (res) => setBreakdown(res ?? null),
    );
  }, [range]);

  useEffect(() => {
    getFromOpenElisServer(
      "/rest/qi-config/resolve?indicator=AMENDMENT",
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
  const totalAmended = points.reduce((sum, p) => sum + p.amendedCount, 0);
  const totalReleased = points.reduce((sum, p) => sum + p.releasedCount, 0);
  const windowRate =
    totalReleased > 0
      ? Math.round((totalAmended * 10000) / totalReleased) / 100 // 2dp, like the backend
      : null;

  const tone = rateTone(windowRate, config);

  const chartData = points
    .filter((p) => p.ratePercent != null)
    .map((p) => ({
      period: p.period,
      value: p.ratePercent,
      group: intl.formatMessage({ id: "qa.qi.amendment.trend.series" }),
    }));

  const thresholds = chartThresholds(
    config,
    intl.formatMessage({ id: "qa.qi.amendment.threshold.target" }),
    intl.formatMessage({ id: "qa.qi.amendment.threshold.action" }),
  );

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

  const breakdownRows = (breakdown?.rows || []).map((row, index) => ({
    id: `${row.testName}-${index}`,
    testName: row.testName,
    amendedCount: row.amendedCount,
    releasedCount: row.releasedCount,
    ratePercent:
      row.ratePercent != null ? `${row.ratePercent.toFixed(2)}%` : "—",
  }));

  const rows = (detail?.items || []).map((item, index) => ({
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

  return (
    <div className="pageContent qi-dashboard">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <h2>
        <FormattedMessage id="qa.qi.amendment.title" />
      </h2>
      <p className="qi-dashboard__subtitle">
        <FormattedMessage id="qa.qi.amendment.subtitle" />
      </p>
      <DatePicker
        datePickerType="range"
        dateFormat="Y-m-d"
        value={[range.fromDate, range.toDate]}
        onChange={handleDates}
      >
        <DatePickerInput
          id="amendment-from"
          labelText={intl.formatMessage({
            id: "qa.qi.amendment.filter.from",
          })}
          placeholder="yyyy-mm-dd"
        />
        <DatePickerInput
          id="amendment-to"
          labelText={intl.formatMessage({ id: "qa.qi.amendment.filter.to" })}
          placeholder="yyyy-mm-dd"
        />
      </DatePicker>

      {trend === null ? (
        <p className="qi-tile__message">
          <FormattedMessage id="qa.qi.amendment.error" />
        </p>
      ) : (
        trend !== undefined && (
          <>
            <div className="amendment-rate-header">
              <span className="qi-tile__title">
                <FormattedMessage id="qa.qi.amendment.rate.label" />
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
                  id="qa.qi.dashboard.tile.amendment.secondary"
                  values={{ amended: totalAmended, released: totalReleased }}
                />
              </span>
            </div>

            <h4 className="amendment-section__title">
              <FormattedMessage id="qa.qi.amendment.trend.title" />
            </h4>
            <Dropdown
              id="amendment-trend-interval"
              size="sm"
              className="amendment-trend-interval"
              titleText={intl.formatMessage({
                id: "qa.qi.amendment.trend.interval",
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
          <DataTable
            rows={breakdownRows}
            headers={BREAKDOWN_HEADERS.map((h) => ({
              key: h.key,
              header: intl.formatMessage({ id: h.labelKey }),
            }))}
          >
            {({ rows: tableRows, headers, getHeaderProps, getRowProps }) => (
              <TableContainer>
                <Table size="sm">
                  <TableHead>
                    <TableRow>
                      {headers.map((header) => (
                        <TableHeader
                          {...getHeaderProps({ header })}
                          key={header.key}
                        >
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {tableRows.map((row) => (
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
        </>
      )}

      {detail === undefined ? (
        <DataTableSkeleton columnCount={HEADERS.length} rowCount={5} />
      ) : detail === null ? (
        <p className="qi-tile__message">
          <FormattedMessage id="qa.qi.amendment.error" />
        </p>
      ) : rows.length === 0 ? (
        <QAEmptyState
          titleKey="qa.empty.amendment.title"
          subheadKey="qa.empty.amendment.subhead"
        />
      ) : (
        <>
          <h4 className="amendment-section__title">
            <FormattedMessage id="qa.qi.amendment.list.title" />
          </h4>
          <DataTable
            rows={rows}
            headers={HEADERS.map((h) => ({
              key: h.key,
              header: intl.formatMessage({ id: h.labelKey }),
            }))}
          >
            {({ rows: tableRows, headers, getHeaderProps, getRowProps }) => (
              <TableContainer>
                <Table size="sm">
                  <TableHead>
                    <TableRow>
                      {headers.map((header) => (
                        <TableHeader
                          {...getHeaderProps({ header })}
                          key={header.key}
                        >
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {tableRows.map((row) => (
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

export default AmendmentReport;

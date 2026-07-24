import React, { useCallback, useEffect, useState } from "react";
import {
  DataTable,
  DataTableSkeleton,
  DatePicker,
  DatePickerInput,
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
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import QAEmptyState from "../common/QAEmptyState";
import "./QIDashboard.css";

/**
 * Critical Callback Compliance detail page (OGC-715) at /qa/qi/callback:
 * compliance header colored against qi_config thresholds, the design's
 * time-to-acknowledge histogram + failures-by-reason table (qa-final-preview
 * §callback), and the released critical results list with each result's
 * latest callback attempt. Never-logged criticals sort first — they are the
 * actionable gap list.
 */

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sideNav.label.qa", link: "" },
  { label: "sideNav.label.qa.qi.dashboard", link: "/qa/qi/dashboard" },
  { label: "qa.qi.dashboard.tile.callback.label", link: "" },
];

const HEADERS = [
  { key: "releasedAt", labelKey: "qa.qi.callback.column.releasedAt" },
  { key: "labNumber", labelKey: "qa.qi.callback.column.labNumber" },
  { key: "testName", labelKey: "qa.qi.callback.column.test" },
  { key: "resultValue", labelKey: "qa.qi.callback.column.resultValue" },
  { key: "criticalRange", labelKey: "qa.qi.callback.column.criticalRange" },
  { key: "status", labelKey: "qa.qi.callback.column.status" },
  { key: "timeToCallback", labelKey: "qa.qi.callback.column.timeToCallback" },
  { key: "recipientName", labelKey: "qa.qi.callback.column.recipient" },
  { key: "loggedBy", labelKey: "qa.qi.callback.column.loggedBy" },
];

// CONFIRMED green, REACHED_NO_READBACK amber (gray Tag + class — Carbon has
// no amber), UNABLE_TO_REACH red, never-logged gray.
const STATUS_TAG_TYPE = {
  CONFIRMED: "green",
  REACHED_NO_READBACK: "gray",
  UNABLE_TO_REACH: "red",
};

// Histogram buckets (backend ackDistribution keys); red = non-compliant.
const DISTRIBUTION_BUCKETS = [
  { key: "0-5" },
  { key: "5-15" },
  { key: "15-30" },
  { key: "30-60" },
  { key: "over60", red: true },
  { key: "noAck", red: true },
];

const FAILURE_REASONS = [
  "overTarget",
  "unableToReach",
  "noReadback",
  "noCallback",
];

/** Tag tone against qi_config thresholds (CALLBACK is HIGHER_BETTER). */
function rateTone(rate, config) {
  if (
    rate == null ||
    !config?.enabled ||
    config.target == null ||
    config.action == null
  ) {
    return "gray";
  }
  if (rate >= config.target) {
    return "green";
  }
  return rate <= config.action ? "red" : "amber";
}

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

function formatMinutes(minutes) {
  if (minutes == null) {
    return "—";
  }
  // negative = called before release (compliant)
  const sign = minutes < 0 ? "−" : "";
  const abs = Math.abs(minutes);
  if (abs < 60) {
    return `${sign}${abs}m`;
  }
  const hours = Math.floor(abs / 60);
  if (hours < 24) {
    return `${sign}${hours}h ${abs % 60}m`;
  }
  return `${sign}${Math.floor(hours / 24)}d ${hours % 24}h`;
}

const CallbackReport = () => {
  const intl = useIntl();
  const [range, setRange] = useState(defaultRange);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(25);
  // undefined = loading, null = fetch yielded no data
  const [detail, setDetail] = useState();
  const [summary, setSummary] = useState();
  // fail-open like QIDashboard/QIEnabledRoute: no config -> plain gray tag
  const [config, setConfig] = useState(null);

  const fetchDetail = useCallback(() => {
    setDetail(undefined);
    getFromOpenElisServer(
      `/rest/critical-callback/detail?fromDate=${range.fromDate}` +
        `&toDate=${range.toDate}&page=${page}&pageSize=${pageSize}`,
      (res) => setDetail(res ?? null),
    );
  }, [range, page, pageSize]);

  useEffect(() => {
    fetchDetail();
  }, [fetchDetail]);

  useEffect(() => {
    setSummary(undefined);
    getFromOpenElisServer(
      `/rest/critical-callback/summary?fromDate=${range.fromDate}` +
        `&toDate=${range.toDate}`,
      (res) => setSummary(res ?? null),
    );
  }, [range]);

  useEffect(() => {
    getFromOpenElisServer("/rest/qi-config/resolve?indicator=CALLBACK", (res) =>
      setConfig(res ?? null),
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

  const tone = rateTone(summary?.compliancePercent, config);

  const distribution = detail?.ackDistribution;
  const maxBucket = distribution
    ? Math.max(1, ...DISTRIBUTION_BUCKETS.map((b) => distribution[b.key] || 0))
    : 1;
  const failureTotal = detail?.failureCounts
    ? FAILURE_REASONS.reduce(
        (sum, reason) => sum + (detail.failureCounts[reason] || 0),
        0,
      )
    : 0;

  const rows = (detail?.items || []).map((item, index) => ({
    id: `${item.analysisId}-${index}`,
    releasedAt: formatTimestamp(item.releasedAt),
    labNumber: item.labNumber || "—",
    testName: item.testName || "—",
    resultValue: item.resultValue ?? "—",
    criticalRange: item.criticalRange ?? "—",
    status: (
      <Tag
        type={STATUS_TAG_TYPE[item.status] || "gray"}
        size="sm"
        className={
          item.status === "REACHED_NO_READBACK" ? "qi-rate-tag--amber" : ""
        }
      >
        {intl.formatMessage({
          id: item.status
            ? `qa.qi.callback.status.${item.status}`
            : "qa.qi.callback.status.notLogged",
        })}
      </Tag>
    ),
    timeToCallback: formatMinutes(item.minutesToCallback),
    recipientName: item.recipientName || "—",
    loggedBy: item.loggedBy || "—",
  }));

  return (
    <div className="adminPageContent qi-dashboard">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <h2>
        <FormattedMessage id="qa.qi.callback.title" />
      </h2>
      <p className="qi-dashboard__subtitle">
        <FormattedMessage id="qa.qi.callback.subtitle" />
      </p>
      <DatePicker
        datePickerType="range"
        dateFormat="Y-m-d"
        value={[range.fromDate, range.toDate]}
        onChange={handleDates}
      >
        <DatePickerInput
          id="callback-from"
          labelText={intl.formatMessage({
            id: "qa.qi.amendment.filter.from",
          })}
          placeholder="yyyy-mm-dd"
        />
        <DatePickerInput
          id="callback-to"
          labelText={intl.formatMessage({ id: "qa.qi.amendment.filter.to" })}
          placeholder="yyyy-mm-dd"
        />
      </DatePicker>

      {summary && (
        <div className="amendment-rate-header">
          <span className="qi-tile__title">
            <FormattedMessage id="qa.qi.callback.rate.label" />
          </span>
          <Tag
            type={tone === "amber" ? "gray" : tone}
            className={
              tone === "amber"
                ? "amendment-rate-tag qi-rate-tag--amber"
                : "amendment-rate-tag"
            }
            data-testid="callback-rate-tag"
          >
            {summary.compliancePercent != null
              ? `${summary.compliancePercent.toFixed(2)}%`
              : "—"}
          </Tag>
          <span className="qi-tile__secondary">
            <FormattedMessage
              id="qa.qi.dashboard.tile.callback.secondary"
              values={{
                confirmed: summary.confirmedCount,
                critical: summary.criticalCount,
              }}
            />
          </span>
        </div>
      )}

      {detail === undefined ? (
        <DataTableSkeleton columnCount={HEADERS.length} rowCount={5} />
      ) : detail === null ? (
        <p className="qi-tile__message">
          <FormattedMessage id="qa.qi.callback.error" />
        </p>
      ) : rows.length === 0 ? (
        <QAEmptyState
          titleKey="qa.empty.callback.title"
          subheadKey="qa.empty.callback.subhead"
        />
      ) : (
        <>
          {distribution && (
            <>
              <h4 className="amendment-section__title">
                <FormattedMessage id="qa.qi.callback.distribution.title" />
              </h4>
              <div className="qi-barlist" data-testid="callback-distribution">
                {DISTRIBUTION_BUCKETS.map((bucket) => (
                  <div className="qi-barlist__row" key={bucket.key}>
                    <span className="qi-barlist__label">
                      {intl.formatMessage({
                        id: `qa.qi.callback.distribution.${bucket.key}`,
                      })}
                    </span>
                    <span className="qi-barlist__track">
                      <span
                        className={`qi-barlist__fill${
                          bucket.red ? " qi-barlist__fill--red" : ""
                        }`}
                        style={{
                          width: `${((distribution[bucket.key] || 0) / maxBucket) * 100}%`,
                        }}
                      />
                    </span>
                    <span className="qi-barlist__count">
                      {distribution[bucket.key] || 0}
                    </span>
                  </div>
                ))}
              </div>
            </>
          )}
          {failureTotal > 0 && (
            <>
              <h4 className="amendment-section__title">
                <FormattedMessage
                  id="qa.qi.callback.failures.title"
                  values={{ count: failureTotal }}
                />
              </h4>
              <TableContainer>
                <Table size="sm" data-testid="callback-failures">
                  <TableHead>
                    <TableRow>
                      <TableHeader>
                        {intl.formatMessage({
                          id: "qa.qi.callback.failures.column.reason",
                        })}
                      </TableHeader>
                      <TableHeader>
                        {intl.formatMessage({
                          id: "qa.qi.callback.failures.column.count",
                        })}
                      </TableHeader>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {FAILURE_REASONS.filter(
                      (reason) => (detail.failureCounts[reason] || 0) > 0,
                    ).map((reason) => (
                      <TableRow key={reason}>
                        <TableCell>
                          {intl.formatMessage({
                            id: `qa.qi.callback.failures.${reason}`,
                          })}
                        </TableCell>
                        <TableCell>{detail.failureCounts[reason]}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </>
          )}
          <h4 className="amendment-section__title">
            <FormattedMessage id="qa.qi.callback.list.title" />
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

export default CallbackReport;

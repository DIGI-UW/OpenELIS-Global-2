import React, { useState } from "react";
import {
  DataTableSkeleton,
  DatePicker,
  DatePickerInput,
  Dropdown,
  Pagination,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { toLocalIsoDate } from "../../utils/Utils";
import { INTERVALS } from "../../reports/tat/tatUtils";
import { useServerData } from "../../utils/useServerData";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import QASimpleTable from "../common/QASimpleTable";
import QAEmptyState from "../common/QAEmptyState";
import { lastDays } from "../common/qaDates";
import useQiConfig from "./useQiConfig";
import "../common/QAStyles.css";

/**
 * The shell every QI detail page is: breadcrumbs, a 30-day range picker, the
 * report's own sections, then the paged list with its loading, error and empty
 * states. The amendment, rejection and callback pages differ only in which
 * endpoint they page through, which columns they show and which charts sit
 * above the list, so those are all they carry.
 */

/** Range + paging for a report, with paging reset whenever the range moves. */
export function useDateRange(days = 30) {
  const [range, setRange] = useState(() => lastDays(days));
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(25);

  const handleDates = (dates) => {
    if (dates.length === 2) {
      setPage(0);
      setRange({
        fromDate: toLocalIsoDate(dates[0]),
        toDate: toLocalIsoDate(dates[1]),
      });
    }
  };

  return { range, page, pageSize, setPage, setPageSize, handleDates };
}

/**
 * The headline rate for the window, tagged against its qi_config band.
 * Carbon has no amber Tag, so the between-thresholds band is a gray tag
 * restyled by qi-rate-tag--amber.
 */
export const QIRateHeader = ({ label, value, tone, secondary, testId }) => (
  <div className="amendment-rate-header">
    <span className="qi-tile__title">{label}</span>
    <Tag
      type={tone === "amber" ? "gray" : tone}
      className={
        tone === "amber"
          ? "amendment-rate-tag qi-rate-tag--amber"
          : "amendment-rate-tag"
      }
      data-testid={testId}
    >
      {value != null ? `${value.toFixed(2)}%` : "—"}
    </Tag>
    <span className="qi-tile__secondary">{secondary}</span>
  </div>
);

/** Bucket-size picker above a trend chart. */
export const QITrendInterval = ({ id, titleKey, interval, onChange }) => {
  const intl = useIntl();
  return (
    <Dropdown
      id={id}
      size="sm"
      className="amendment-trend-interval"
      titleText={intl.formatMessage({ id: titleKey })}
      label=""
      items={INTERVALS}
      itemToString={(item) =>
        item ? intl.formatMessage({ id: item.labelKey }) : ""
      }
      selectedItem={INTERVALS.find((i) => i.id === interval)}
      onChange={({ selectedItem }) => onChange(selectedItem.id)}
    />
  );
};

/**
 * Carbon-charts options for a percentage trend line with the config's
 * target/action lines drawn on it. Daily periods are real dates, so a time
 * axis spaces them honestly (a 4-day gap looks like a gap, not one tick);
 * weekly and monthly keys ("2026-W30", "2026-07") are not parseable dates and
 * stay labels.
 */
export const rateChartOptions = (thresholds, timeAxis = false) => ({
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
});

const QIReportPage = ({
  indicator,
  breadcrumbs,
  titleKey,
  subtitleKey,
  errorKey,
  idPrefix,
  fromLabelKey,
  toLabelKey,
  className = "pageContent qi-dashboard",
  detailUrl,
  headers,
  toRows,
  listTitleKey,
  emptyTitleKey,
  emptySubheadKey,
  children,
}) => {
  const intl = useIntl();
  const { range, page, pageSize, setPage, setPageSize, handleDates } =
    useDateRange();
  // fail-open like QIDashboard/QIEnabledRoute: no config -> plain gray tag
  const { config } = useQiConfig(indicator);
  const detailQuery = useServerData(detailUrl(range, page, pageSize));
  const detail = detailQuery.data;
  const rows = detail ? toRows(detail) : [];

  return (
    <div className={className}>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <h2>
        <FormattedMessage id={titleKey} />
      </h2>
      <p className="qi-dashboard__subtitle">
        <FormattedMessage id={subtitleKey} />
      </p>
      <DatePicker
        datePickerType="range"
        dateFormat="Y-m-d"
        value={[range.fromDate, range.toDate]}
        onChange={handleDates}
      >
        <DatePickerInput
          id={`${idPrefix}-from`}
          labelText={intl.formatMessage({ id: fromLabelKey })}
          placeholder="yyyy-mm-dd"
        />
        <DatePickerInput
          id={`${idPrefix}-to`}
          labelText={intl.formatMessage({ id: toLabelKey })}
          placeholder="yyyy-mm-dd"
        />
      </DatePicker>

      {children({ range, config, detail })}

      {/* Skeleton on every read, not just the first: a new range or page is a
          different list, and showing the old rows under the new dates would
          read as the answer. */}
      {detailQuery.isFetching ? (
        <DataTableSkeleton columnCount={headers.length} rowCount={5} />
      ) : !detail ? (
        <p className="qi-tile__message">
          <FormattedMessage id={errorKey} />
        </p>
      ) : rows.length === 0 ? (
        <QAEmptyState titleKey={emptyTitleKey} subheadKey={emptySubheadKey} />
      ) : (
        <>
          <h4 className="amendment-section__title">
            <FormattedMessage id={listTitleKey} />
          </h4>
          <QASimpleTable rows={rows} headers={headers} />
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

export default QIReportPage;

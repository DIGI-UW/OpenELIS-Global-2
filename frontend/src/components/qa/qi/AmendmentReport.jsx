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
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import "./QIDashboard.css";

/**
 * Amendment Rate detail page (OGC-698) at /qa/qi/amendment: results corrected
 * after their patient report went out, with prior/current values from the
 * result audit history. No Pareto — no amendment-reason data exists (v8).
 */

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sideNav.label.qa", link: "" },
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

  const handleDates = (dates) => {
    if (dates.length === 2) {
      setPage(0);
      setRange({
        fromDate: formatDate(dates[0]),
        toDate: formatDate(dates[1]),
      });
    }
  };

  const rows = (detail?.items || []).map((item, index) => ({
    id: `${item.analysisId}-${index}`,
    amendedAt: formatTimestamp(item.amendedAt),
    labNumber: item.labNumber || "—",
    testName: item.testName || "—",
    priorValue: item.priorValue ?? "—",
    currentValue: item.currentValue ?? "—",
    amendedBy: item.amendedBy || "—",
    releasedAt: formatTimestamp(item.releasedAt),
    timeToAmend: formatMinutes(item.minutesToAmend),
  }));

  return (
    <div className="adminPageContent qi-dashboard">
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
      {detail === undefined ? (
        <DataTableSkeleton columnCount={HEADERS.length} rowCount={5} />
      ) : detail === null ? (
        <p className="qi-tile__message">
          <FormattedMessage id="qa.qi.amendment.error" />
        </p>
      ) : rows.length === 0 ? (
        <p className="qi-tile__message">
          <FormattedMessage id="qa.qi.amendment.empty" />
        </p>
      ) : (
        <>
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

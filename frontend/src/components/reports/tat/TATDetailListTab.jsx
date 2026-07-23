import React, { useState, useEffect, useCallback } from "react";
import {
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  Pagination,
  DataTableSkeleton,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import { formatTat } from "./tatUtils";

function formatTimestamp(ts) {
  if (!ts) return "—";
  try {
    return new Date(ts).toLocaleString();
  } catch {
    return ts;
  }
}

function TATDetailListTab({ filters, buildQueryString }) {
  const intl = useIntl();

  const ALL_HEADERS = [
    { key: "labNumber", header: intl.formatMessage({ id: "reports.tat.column.labNumber" }), alwaysVisible: true },
    { key: "testName", header: intl.formatMessage({ id: "reports.tat.column.test" }), alwaysVisible: true },
    { key: "labUnit", header: intl.formatMessage({ id: "reports.tat.column.labUnit" }), alwaysVisible: true },
    { key: "priority", header: intl.formatMessage({ id: "reports.tat.column.priority" }), alwaysVisible: true },
    { key: "orderCreated", header: intl.formatMessage({ id: "reports.tat.column.ordered" }), alwaysVisible: true },
    { key: "collected", header: intl.formatMessage({ id: "reports.tat.column.collected" }), alwaysVisible: true },
    { key: "received", header: intl.formatMessage({ id: "reports.tat.column.received" }), alwaysVisible: true },
    { key: "testingStarted", header: intl.formatMessage({ id: "reports.tat.column.started" }), alwaysVisible: false },
    { key: "resultEntered", header: intl.formatMessage({ id: "reports.tat.column.resulted" }), alwaysVisible: false },
    { key: "validated", header: intl.formatMessage({ id: "reports.tat.column.validated" }), alwaysVisible: true },
    { key: "selectedSegmentTat", header: intl.formatMessage({ id: "reports.tat.column.selectedTat" }), alwaysVisible: true },
    { key: "overallTat", header: intl.formatMessage({ id: "reports.tat.column.overallTat" }), alwaysVisible: true },
  ];
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(25);
  const [sortField, setSortField] = useState("selectedSegmentTat");
  const [sortOrder, setSortOrder] = useState("desc");

  const fetchData = useCallback(() => {
    if (!filters) return;
    setLoading(true);
    const qs = buildQueryString(
      filters,
      `&page=${page}&pageSize=${pageSize}&sortField=${sortField}&sortOrder=${sortOrder}`,
    );
    getFromOpenElisServer(`/rest/reports/tat/detail?${qs}`, (res) => {
      setData(res || null);
      setLoading(false);
    });
  }, [filters, buildQueryString, page, pageSize, sortField, sortOrder]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // Reset pagination when filters change
  useEffect(() => {
    setPage(0);
  }, [filters]);

  if (!filters) {
    return (
      <div style={{ padding: "2rem", textAlign: "center", color: "var(--cds-text-helper)" }}>
        <FormattedMessage id="reports.tat.noResults" />
      </div>
    );
  }

  if (loading) {
    return <DataTableSkeleton headers={ALL_HEADERS.filter((h) => h.alwaysVisible)} rowCount={10} />;
  }

  if (!data || !data.results || data.results.length === 0) {
    return (
      <div style={{ padding: "2rem", textAlign: "center", color: "var(--cds-text-helper)" }}>
        <FormattedMessage id="reports.tat.noResults" />
      </div>
    );
  }

  const visibleHeaders = ALL_HEADERS.filter((h) => h.alwaysVisible);

  const rows = data.results.map((r, i) => ({
    id: String(i),
    labNumber: r.labNumber,
    testName: r.testName,
    labUnit: r.labUnit,
    priority: r.priority,
    orderCreated: formatTimestamp(r.orderCreated),
    collected: formatTimestamp(r.collected),
    received: formatTimestamp(r.received),
    testingStarted: formatTimestamp(r.testingStarted),
    resultEntered: formatTimestamp(r.resultEntered),
    validated: formatTimestamp(r.validated),
    selectedSegmentTat: formatTat(r.selectedSegmentTat),
    overallTat: formatTat(r.overallTat),
    rawPriority: r.priority,
  }));

  return (
    <div>
      <DataTable rows={rows} headers={visibleHeaders}>
        {({ rows: tableRows, headers: tableHeaders, getTableProps }) => (
          <TableContainer>
            <Table {...getTableProps()}>
              <TableHead>
                <TableRow>
                  {tableHeaders.map((header) => (
                    <TableHeader
                      key={header.key}
                      isSortable
                      onClick={() => {
                        if (sortField === header.key) {
                          setSortOrder(sortOrder === "asc" ? "desc" : "asc");
                        } else {
                          setSortField(header.key);
                          setSortOrder("desc");
                        }
                      }}
                    >
                      {header.header}
                    </TableHeader>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {tableRows.map((row) => {
                  const original = data.results[parseInt(row.id)];
                  const isStat = original?.priority === "STAT";
                  return (
                    <TableRow
                      key={row.id}
                      style={
                        isStat
                          ? { borderLeft: "3px solid #da1e28" }
                          : { borderLeft: "3px solid transparent" }
                      }
                    >
                      {row.cells.map((cell) => (
                        <TableCell key={cell.id}>
                          {cell.info.header === "labNumber" ? (
                            <a
                              href={`/SamplePatientEntry?accessionNumber=${cell.value}`}
                              target="_blank"
                              rel="noopener noreferrer"
                            >
                              {cell.value}
                            </a>
                          ) : (
                            cell.value
                          )}
                        </TableCell>
                      ))}
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </DataTable>
      <Pagination
        totalItems={data.totalCount}
        pageSize={pageSize}
        page={page + 1}
        onChange={({ page: newPage, pageSize: newSize }) => {
          setPage(newPage - 1);
          setPageSize(newSize);
        }}
        pageSizes={[25, 50, 100]}
      />
    </div>
  );
}

export default TATDetailListTab;

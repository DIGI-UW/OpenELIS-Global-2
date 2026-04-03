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

function formatTat(hours) {
  if (hours == null) return "—";
  const h = Math.floor(hours);
  const m = Math.round((hours - h) * 60);
  if (h === 0 && m === 0) return "0h 0m";
  if (h === 0) return `${m}m`;
  if (m === 0) return `${h}h`;
  return `${h}h ${m}m`;
}

function formatTimestamp(ts) {
  if (!ts) return "—";
  try {
    return new Date(ts).toLocaleString();
  } catch {
    return ts;
  }
}

const ALL_HEADERS = [
  { key: "labNumber", header: "Lab Number", alwaysVisible: true },
  { key: "testName", header: "Test", alwaysVisible: true },
  { key: "labUnit", header: "Lab Unit", alwaysVisible: true },
  { key: "priority", header: "Priority", alwaysVisible: true },
  { key: "orderCreated", header: "Ordered", alwaysVisible: true },
  { key: "collected", header: "Collected", alwaysVisible: true },
  { key: "received", header: "Received", alwaysVisible: true },
  { key: "testingStarted", header: "Started", alwaysVisible: false },
  { key: "resultEntered", header: "Resulted", alwaysVisible: false },
  { key: "validated", header: "Validated", alwaysVisible: true },
  { key: "selectedSegmentTat", header: "Selected TAT", alwaysVisible: true },
  { key: "overallTat", header: "Overall TAT", alwaysVisible: true },
];

function TATDetailListTab({ filters, buildQueryString }) {
  const intl = useIntl();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(25);
  const [sortField, setSortField] = useState("selectedTat");
  const [sortOrder, setSortOrder] = useState("desc");

  const fetchData = useCallback(() => {
    if (!filters) return;
    setLoading(true);
    const qs = buildQueryString(
      filters,
      `&page=${page}&pageSize=${pageSize}&sortField=${sortField}&sortOrder=${sortOrder}`,
    );
    getFromOpenElisServer(`/rest/reports/tat/detail?${qs}`, (res) => {
      setData(res);
      setLoading(false);
    });
  }, [filters, buildQueryString, page, pageSize, sortField, sortOrder]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  if (!filters) {
    return (
      <div style={{ padding: "2rem", textAlign: "center", color: "#6f6f6f" }}>
        <FormattedMessage id="reports.tat.noResults" />
      </div>
    );
  }

  if (loading) {
    return <DataTableSkeleton headers={ALL_HEADERS.filter((h) => h.alwaysVisible)} rowCount={10} />;
  }

  if (!data || !data.results || data.results.length === 0) {
    return (
      <div style={{ padding: "2rem", textAlign: "center", color: "#6f6f6f" }}>
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
                          ? { borderLeft: "3px solid #DA1E28" }
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

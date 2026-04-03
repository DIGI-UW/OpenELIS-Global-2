import React from "react";
import {
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
} from "@carbon/react";
import { FormattedMessage } from "react-intl";

function formatTat(hours) {
  if (hours == null) return "—";
  const h = Math.floor(hours);
  const m = Math.round((hours - h) * 60);
  if (h === 0 && m === 0) return "0h 0m";
  if (h === 0) return `${m}m`;
  if (m === 0) return `${h}h`;
  return `${h}h ${m}m`;
}

const headers = [
  { key: "dimensionValue", header: "Name" },
  { key: "count", header: "Count" },
  { key: "mean", header: "Mean" },
  { key: "median", header: "Median" },
  { key: "percentile90", header: "90th %ile" },
  { key: "max", header: "Max" },
];

function TATBreakdownTable({ breakdown, onDrillDown }) {
  const rows = breakdown.map((row, i) => ({
    id: String(i),
    dimensionValue: row.dimensionValue,
    count: row.count,
    mean: formatTat(row.mean),
    median: formatTat(row.median),
    percentile90: formatTat(row.percentile90),
    max: formatTat(row.max),
    rawMax: row.max,
  }));

  return (
    <div>
      <DataTable rows={rows} headers={headers}>
        {({ rows: tableRows, headers: tableHeaders, getTableProps }) => (
          <TableContainer>
            <Table {...getTableProps()}>
              <TableHead>
                <TableRow>
                  {tableHeaders.map((header) => (
                    <TableHeader key={header.key}>{header.header}</TableHeader>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {tableRows.map((row) => {
                  const original = breakdown[parseInt(row.id)];
                  return (
                    <TableRow
                      key={row.id}
                      onClick={() => onDrillDown && onDrillDown(original.dimensionValue)}
                      style={{ cursor: onDrillDown ? "pointer" : "default" }}
                    >
                      {row.cells.map((cell) => (
                        <TableCell
                          key={cell.id}
                          style={
                            cell.info.header === "max" && original.max > 24
                              ? { color: "#DA1E28", fontWeight: 600 }
                              : undefined
                          }
                        >
                          {cell.value}
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
      <p style={{ fontSize: "12px", color: "#6f6f6f", marginTop: "0.5rem" }}>
        <FormattedMessage id="reports.tat.clickRowHint" />
      </p>
    </div>
  );
}

export default TATBreakdownTable;

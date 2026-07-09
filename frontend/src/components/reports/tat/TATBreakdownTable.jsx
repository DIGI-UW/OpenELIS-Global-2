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
import { FormattedMessage, useIntl } from "react-intl";
import { formatTat } from "./tatUtils";

function TATBreakdownTable({ breakdown, onDrillDown }) {
  const intl = useIntl();

  const headers = [
    { key: "dimensionValue", header: intl.formatMessage({ id: "reports.tat.column.name" }) },
    { key: "count", header: intl.formatMessage({ id: "reports.tat.column.count" }) },
    { key: "mean", header: intl.formatMessage({ id: "reports.tat.column.mean" }) },
    { key: "median", header: intl.formatMessage({ id: "reports.tat.column.median" }) },
    { key: "percentile90", header: intl.formatMessage({ id: "reports.tat.column.p90" }) },
    { key: "max", header: intl.formatMessage({ id: "reports.tat.column.max" }) },
  ];
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
                              ? { color: "var(--cds-support-error)", fontWeight: 600 }
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
      <p style={{ fontSize: "12px", color: "var(--cds-text-helper)", marginTop: "0.5rem" }}>
        <FormattedMessage id="reports.tat.clickRowHint" />
      </p>
    </div>
  );
}

export default TATBreakdownTable;

import React from "react";
import {
  DataTable,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
} from "@carbon/react";

const AnalyzerConfigTable = ({
  headers,
  rows,
  tableLabel,
  testId,
  getRowTestId,
  renderCell = (cell) => cell.value,
}) => (
  <DataTable rows={rows} headers={headers}>
    {({
      rows: carbonRows,
      headers: carbonHeaders,
      getHeaderProps,
      getRowProps,
      getTableProps,
    }) => (
      <TableContainer>
        <Table
          {...getTableProps()}
          aria-label={tableLabel}
          data-testid={testId}
        >
          <TableHead>
            <TableRow>
              {carbonHeaders.map((header) => (
                <TableHeader key={header.key} {...getHeaderProps({ header })}>
                  {header.header}
                </TableHeader>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {carbonRows.map((row) => (
              <TableRow
                key={row.id}
                {...getRowProps({ row })}
                data-testid={getRowTestId?.(row)}
              >
                {row.cells.map((cell) => (
                  <TableCell key={cell.id}>{renderCell(cell, row)}</TableCell>
                ))}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    )}
  </DataTable>
);

export default AnalyzerConfigTable;

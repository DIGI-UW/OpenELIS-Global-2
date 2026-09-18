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
import { useIntl } from "react-intl";

/**
 * The plain Carbon DataTable every QA list renders: headers are i18n keys,
 * rows carry already-formatted cell values. Shared so the QI and QMS pages
 * do not each keep their own copy of the same table markup.
 */
const QASimpleTable = ({ rows, headers, size = "sm" }) => {
  const intl = useIntl();
  return (
    <DataTable
      rows={rows}
      headers={headers.map((h) => ({
        key: h.key,
        header: intl.formatMessage({ id: h.labelKey }),
      }))}
    >
      {({
        rows: tableRows,
        headers: tableHeaders,
        getHeaderProps,
        getRowProps,
      }) => (
        <TableContainer>
          <Table size={size}>
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
  );
};

export default QASimpleTable;

import React, { useEffect, useState } from "react";
import {
  DataTable,
  DataTableSkeleton,
  Dropdown,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  Tile,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";

/**
 * OGC-1147 — bench QC (manual quantitative and RDT controls), grouped by lab unit
 * and test.
 *
 * A separate tab rather than a source filter on the Instruments tab: that tab's rows are
 * analyzers, and a bench control has none, so filtering it by source could only ever
 * return an empty list. For RDT this is the only QC surface it appears on at all —
 * an Invalid control line is deliberately kept out of the statistical violation
 * record, so it never reaches the Alerts tab.
 */
const SOURCES = [
  { id: "ALL", labelId: "qc.bench.source.all" },
  { id: "MANUAL", labelId: "qc.bench.source.manual" },
  { id: "RDT", labelId: "qc.bench.source.rdt" },
];

const BenchQcTab = () => {
  const intl = useIntl();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [source, setSource] = useState(SOURCES[0]);

  useEffect(() => {
    setLoading(true);
    const query = source && source.id !== "ALL" ? `?source=${source.id}` : "";
    getFromOpenElisServer(`/rest/qc/dashboard/bench${query}`, (data) => {
      setRows(Array.isArray(data) ? data : []);
      setLoading(false);
    });
  }, [source]);

  const headers = [
    {
      key: "testSectionName",
      header: intl.formatMessage({ id: "qc.bench.column.labUnit" }),
    },
    {
      key: "testName",
      header: intl.formatMessage({ id: "qc.bench.column.test" }),
    },
    {
      key: "source",
      header: intl.formatMessage({ id: "qc.bench.column.source" }),
    },
    {
      key: "totalRuns",
      header: intl.formatMessage({ id: "qc.bench.column.runs" }),
    },
    {
      key: "failedRuns",
      header: intl.formatMessage({ id: "qc.bench.column.failed" }),
    },
    {
      key: "lastRun",
      header: intl.formatMessage({ id: "qc.bench.column.lastRun" }),
    },
  ];

  const tableRows = rows.map((row, index) => ({
    id: `${row.testSectionId}-${row.testId}-${row.source}-${index}`,
    testSectionName: row.testSectionName || "-",
    testName: row.testName || "-",
    source: row.source,
    totalRuns: row.totalRuns,
    failedRuns: row.failedRuns,
    lastRun: row.lastRun ? row.lastRun.replace("T", " ") : "-",
  }));

  if (loading) {
    return <DataTableSkeleton columnCount={headers.length} rowCount={5} />;
  }

  return (
    <>
      <div style={{ maxWidth: "16rem", marginBottom: "1rem" }}>
        <Dropdown
          id="bench-qc-source"
          titleText={intl.formatMessage({ id: "qc.bench.source.label" })}
          label={intl.formatMessage({ id: "qc.bench.source.all" })}
          items={SOURCES}
          selectedItem={source}
          itemToString={(item) =>
            item ? intl.formatMessage({ id: item.labelId }) : ""
          }
          onChange={({ selectedItem }) => setSource(selectedItem)}
        />
      </div>

      {tableRows.length === 0 ? (
        <Tile>
          <FormattedMessage id="qc.bench.empty" />
        </Tile>
      ) : (
        <DataTable rows={tableRows} headers={headers}>
          {({ rows: dataRows, headers: dataHeaders, getTableProps }) => (
            <TableContainer>
              <Table {...getTableProps()}>
                <TableHead>
                  <TableRow>
                    {dataHeaders.map((header) => (
                      <TableHeader key={header.key}>
                        {header.header}
                      </TableHeader>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {dataRows.map((row) => (
                    <TableRow key={row.id}>
                      {row.cells.map((cell) => (
                        <TableCell key={cell.id}>
                          {/* Failures carry a tag as well as a number: a count alone
                              does not read as "act on this" at a glance. */}
                          {cell.info.header === "failedRuns" &&
                          cell.value > 0 ? (
                            <Tag type="red" size="sm">
                              {cell.value}
                            </Tag>
                          ) : (
                            cell.value
                          )}
                        </TableCell>
                      ))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </DataTable>
      )}
    </>
  );
};

export default BenchQcTab;

import React, { useEffect, useState } from "react";
import { DataTableSkeleton, Dropdown, Tag } from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import { formatTimestamp } from "./qcDashboardUtils";
import QAEmptyState from "../../qa/common/QAEmptyState";
import QASimpleTable from "../../qa/common/QASimpleTable";

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

const HEADERS = [
  { key: "testSectionName", labelKey: "label.results.labUnit" },
  { key: "testName", labelKey: "common.test" },
  { key: "source", labelKey: "label.testCatalog.terminology.col.source" },
  { key: "totalRuns", labelKey: "qc.bench.column.runs" },
  { key: "failedRuns", labelKey: "common.failed" },
  { key: "lastRun", labelKey: "qc.bench.column.lastRun" },
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

  const tableRows = rows.map((row, index) => ({
    id: `${row.testSectionId}-${row.testId}-${row.source}-${index}`,
    testSectionName: row.testSectionName || "-",
    testName: row.testName || "-",
    source: row.source,
    totalRuns: row.totalRuns,
    // Failures carry a tag as well as a number: a count alone does not read as
    // "act on this" at a glance.
    failedRuns:
      row.failedRuns > 0 ? (
        <Tag type="red" size="sm">
          {row.failedRuns}
        </Tag>
      ) : (
        row.failedRuns
      ),
    lastRun: formatTimestamp(row.lastRun),
  }));

  if (loading) {
    return <DataTableSkeleton columnCount={HEADERS.length} rowCount={5} />;
  }

  return (
    <>
      <div style={{ maxWidth: "16rem", marginBottom: "1rem" }}>
        <Dropdown
          id="bench-qc-source"
          titleText={intl.formatMessage({
            id: "label.testCatalog.terminology.col.source",
          })}
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
        <QAEmptyState titleKey="qc.bench.empty" size="inline" />
      ) : (
        <QASimpleTable rows={tableRows} headers={HEADERS} />
      )}
    </>
  );
};

export default BenchQcTab;

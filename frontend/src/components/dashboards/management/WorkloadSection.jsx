import React from "react";
import {
  DataTable,
  DataTableSkeleton,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tile,
} from "@carbon/react";
import { StackedAreaChart } from "@carbon/charts-react";
import "@carbon/charts/styles.css";
import { FormattedMessage, useIntl } from "react-intl";
import QAEmptyState from "../../qa/common/QAEmptyState";
import { formatTat } from "../../reports/tat/tatUtils";
import ServerDataState from "../../utils/ServerDataState";
import { useServerData } from "../../utils/useServerData";
import { workloadUrl } from "./managementApi";
import "./WorkloadSection.css";

const COLUMNS = [
  { key: "name", labelKey: "label.mgmt.workload.tatTable.section" },
  { key: "samples", labelKey: "label.mgmt.workload.tatTable.samples" },
  { key: "medianTat", labelKey: "label.mgmt.workload.tatTable.median" },
  { key: "p90Tat", labelKey: "label.mgmt.workload.tatTable.p90" },
  { key: "onTimePct", labelKey: "label.mgmt.workload.tatTable.onTime" },
];

// A section with no validated analyses has no TAT; keep it last either way.
const sortRow = (a, b, { sortDirection, sortStates, locale, compare }) => {
  if (a == null || b == null) return (a == null) - (b == null);
  return sortDirection === sortStates.DESC
    ? compare(b, a, locale)
    : compare(a, b, locale);
};

const chartOptions = (samplesTitle) => ({
  height: "320px",
  curve: "curveMonotoneX",
  axes: {
    bottom: { mapsTo: "date", scaleType: "time" },
    left: {
      mapsTo: "value",
      title: samplesTitle,
      scaleType: "linear",
      includeZero: true,
      stacked: true,
    },
  },
  points: { enabled: false },
  legend: { enabled: true, position: "bottom" },
  toolbar: { enabled: false },
});

const WorkloadSection = ({ scope, scopeLabel }) => {
  const intl = useIntl();
  const query = useServerData(workloadUrl(scope));
  const data = query.data;

  const formatCell = (key, value) => {
    if (value == null) return "—";
    if (key === "name") return value;
    if (key === "medianTat" || key === "p90Tat") return formatTat(value);
    return intl.formatNumber(value, { maximumFractionDigits: 1 });
  };

  const headers = COLUMNS.map(({ key, labelKey }) => ({
    key,
    header: intl.formatMessage({ id: labelKey }),
  }));

  const body = () => {
    if (!data) {
      return query.isError ? (
        <ServerDataState query={query} />
      ) : (
        <DataTableSkeleton columnCount={COLUMNS.length} rowCount={4} />
      );
    }
    if (data.sections.length === 0) {
      return (
        <QAEmptyState titleKey="label.mgmt.workload.empty" size="inline" />
      );
    }
    const chartData = data.sections.flatMap((section) =>
      section.counts.map((value, i) => ({
        group: section.name,
        date: new Date(`${data.days[i]}T00:00:00`),
        value,
      })),
    );
    return (
      <div className="mgmt-workload__cards">
        <Tile className="mgmt-workload__card">
          <h4>
            <FormattedMessage
              id="label.mgmt.workload.byCard"
              values={{ 0: scopeLabel }}
            />
          </h4>
          <StackedAreaChart
            data={chartData}
            options={chartOptions(headers[1].header)}
          />
        </Tile>
        <Tile className="mgmt-workload__card">
          <h4>
            <FormattedMessage
              id="label.mgmt.workload.tatTable"
              values={{ 0: scopeLabel }}
            />
          </h4>
          <DataTable
            rows={data.sections}
            headers={headers}
            isSortable
            sortRow={sortRow}
            size="sm"
          >
            {({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
            }) => (
              <TableContainer>
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      {headers.map((header) => (
                        <TableHeader
                          key={header.key}
                          {...getHeaderProps({ header })}
                        >
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {rows.map((row) => (
                      <TableRow key={row.id} {...getRowProps({ row })}>
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>
                            {formatCell(cell.info.header, cell.value)}
                          </TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
        </Tile>
      </div>
    );
  };

  return (
    <section className="mgmt-workload" data-testid="mgmt-workload">
      <h3 className="mgmt-workload__title">
        <FormattedMessage id="label.mgmt.workload.title" />
      </h3>
      {body()}
    </section>
  );
};

export default WorkloadSection;

import React, { useContext } from "react";
import { useIntl } from "react-intl";
import {
  DataTable,
  Link,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
} from "@carbon/react";
import { EmptyState } from "../commons";
import { useHistory } from "react-router-dom";
import FilterContext from "../filter/filter-context";
import { trendHash } from "../trendline/trendKey";

// Map an observation interpretation to a Carbon Tag color so abnormal /
// high / low / critical results stand out without leaning on custom CSS.
function interpretationToTagType(interp?: string): string {
  const i = (interp || "").toUpperCase();
  if (i.includes("CRITICAL")) return "red";
  if (i.includes("HIGH")) return "red";
  if (i.includes("LOW")) return "purple";
  if (i === "NORMAL") return "green";
  if (i.includes("ABNORMAL")) return "magenta";
  return "gray";
}

/** A result a line can be drawn through — i.e. one that parses as a number. */
function isPlottable(obs?: { value?: string }): boolean {
  return !Number.isNaN(parseFloat(obs?.value ?? ""));
}

function formatDateHeader(iso: string): string {
  const d = new Date(iso);
  if (isNaN(d.getTime())) return iso;
  return d.toLocaleString(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  });
}

function isNarrativeResult(value: string): boolean {
  return value.length > 40 || value.includes("\n");
}

export const GroupedTimeline = () => {
  const { activeTests, timelineData, checkboxes, someChecked } =
    useContext(FilterContext);
  const intl = useIntl();
  const history = useHistory();

  if (!activeTests || !timelineData || !timelineData.loaded) return null;

  const {
    data: {
      parsedTime: { sortedTimes = [] } = { sortedTimes: [] },
      rowData = [],
    },
  } = timelineData;

  const visibleRows: any[] = !someChecked
    ? rowData
    : (rowData || []).filter((row: any) => checkboxes[row.flatName]);

  if (!visibleRows.length) {
    return (
      <EmptyState
        displayText={intl.formatMessage({ id: "label.test.resultsData" })}
        headerTitle={intl.formatMessage({ id: "label.test.results" })}
      />
    );
  }

  // Static "Test" column + one column per sorted date (desc). The matrix
  // shape is positional: row.entries[i] aligns with sortedTimes[i].
  const headers = [
    { key: "test", header: intl.formatMessage({ id: "label.results.test" }) },
    ...sortedTimes.map((time: string, i: number) => ({
      key: `d${i}`,
      header: formatDateHeader(time),
    })),
  ];

  const rows = visibleRows.map((row: any, ri: number) => {
    // Always show units when present, even when no reference range exists —
    // units are independent context that should not be suppressed by a
    // missing range (e.g., qualitative results with a unit but no range).
    const rangeAndUnits = [row.range, row.units].filter(Boolean).join(" ");
    const base: any = {
      id: row.flatName ?? `row-${ri}`,
      // A test can run on several sample types and hold several components, so
      // the name alone does not say which result this is: the specimen and the
      // component travel with it.
      test: {
        name: row.testName || row.display,
        context: [row.sampleType, row.component].filter(Boolean).join(" · "),
        range: rangeAndUnits,
        // Only a numeric series can be plotted; a dictionary or free-text
        // result has nothing to draw a line through.
        trend:
          row.conceptUuid && (row.obs || []).some((o: any) => isPlottable(o))
            ? {
                testId: row.conceptUuid,
                sampleTypeId: row.sampleTypeId,
                componentId: row.componentId,
              }
            : null,
      },
    };
    (row.entries || []).forEach((entry: any, i: number) => {
      base[`d${i}`] = entry
        ? { value: String(entry.value), interpretation: entry.interpretation }
        : null;
    });
    return base;
  });

  return (
    <DataTable rows={rows} headers={headers}>
      {({ rows, headers, getHeaderProps, getRowProps, getTableProps }) => (
        <TableContainer>
          <Table {...getTableProps()}>
            <TableHead>
              <TableRow>
                {headers.map((h: any) => (
                  <TableHeader key={h.key} {...getHeaderProps({ header: h })}>
                    {h.header}
                  </TableHeader>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((row: any) => (
                <TableRow key={row.id} {...getRowProps({ row })}>
                  {row.cells.map((cell: any) => {
                    if (cell.info.header === "test") {
                      return (
                        <TableCell key={cell.id}>
                          <div className="timelineTestCell">
                            <div className="timelineTestName">
                              {cell.value.trend ? (
                                <Link
                                  href={trendHash(cell.value.trend)}
                                  data-testid={`trend-link-${row.id}`}
                                  onClick={(e: React.MouseEvent) => {
                                    e.preventDefault();
                                    history.push({
                                      hash: trendHash(cell.value.trend),
                                    });
                                  }}
                                >
                                  {cell.value.name}
                                </Link>
                              ) : (
                                cell.value.name
                              )}
                            </div>
                            {cell.value.context && (
                              <div className="timelineTestMeta">
                                {cell.value.context}
                              </div>
                            )}
                            {cell.value.range && (
                              <div className="timelineTestMeta">
                                {cell.value.range}
                              </div>
                            )}
                          </div>
                        </TableCell>
                      );
                    }
                    const v = cell.value;
                    if (!v) return <TableCell key={cell.id}>—</TableCell>;
                    return (
                      <TableCell key={cell.id}>
                        {isNarrativeResult(v.value) ? (
                          <span>{v.value}</span>
                        ) : (
                          <Tag
                            type={interpretationToTagType(v.interpretation)}
                            size="sm"
                          >
                            {v.value}
                          </Tag>
                        )}
                      </TableCell>
                    );
                  })}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </DataTable>
  );
};

export default GroupedTimeline;

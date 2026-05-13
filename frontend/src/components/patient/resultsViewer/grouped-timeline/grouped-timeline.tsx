import React, { useContext } from "react";
import { useTranslation } from "react-i18next";
import {
  DataTable,
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
import FilterContext from "../filter/filter-context";

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

function formatDateHeader(iso: string): string {
  const d = new Date(iso);
  if (isNaN(d.getTime())) return iso;
  return d.toLocaleString(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  });
}

export const GroupedTimeline = () => {
  const { activeTests, timelineData, checkboxes, someChecked } =
    useContext(FilterContext);
  const { t } = useTranslation();

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
        displayText={t("data", "data")}
        headerTitle={t("dataTimelineText", "Data Timeline")}
      />
    );
  }

  // Static "Test" column + one column per sorted date (desc). The matrix
  // shape is positional: row.entries[i] aligns with sortedTimes[i].
  const headers = [
    { key: "test", header: t("Test", "Test") },
    ...sortedTimes.map((time: string, i: number) => ({
      key: `d${i}`,
      header: formatDateHeader(time),
    })),
  ];

  const rows = visibleRows.map((row: any, ri: number) => {
    const rangeSuffix = row.range
      ? ` (${row.range}${row.units ? " " + row.units : ""})`
      : "";
    const base: any = {
      id: row.flatName ?? `row-${ri}`,
      test: `${row.display}${rangeSuffix}`,
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
        <TableContainer title={t("patientResults", "Patient Results")}>
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
                      return <TableCell key={cell.id}>{cell.value}</TableCell>;
                    }
                    const v = cell.value;
                    if (!v) return <TableCell key={cell.id}>—</TableCell>;
                    return (
                      <TableCell key={cell.id}>
                        <Tag
                          type={interpretationToTagType(v.interpretation)}
                          size="sm"
                        >
                          {v.value}
                        </Tag>
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

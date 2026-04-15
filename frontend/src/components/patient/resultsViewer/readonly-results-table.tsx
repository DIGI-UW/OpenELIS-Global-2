import React from "react";
import {
  DataTableSkeleton,
  Pagination,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
} from "@carbon/react";
import { formatDatetime } from "./commons";
import type {
  OBSERVATION_INTERPRETATION,
  PatientData,
  ObsRecord,
} from "./commons";
import { useIntl } from "react-intl";

export interface ReadonlyResultRow {
  id: string;
  testName: string;
  result: string;
  referenceRange: string;
  collectedAt: string;
  resultedAt: string;
  interpretation: OBSERVATION_INTERPRETATION;
  sortTimestamp: number;
  resultedAtRaw?: string;
}

interface ReadonlyResultsTableProps {
  loading: boolean;
  rows: ReadonlyResultRow[];
}

const DEFAULT_PAGE_SIZE = 20;

const isPresent = (value: unknown) =>
  value !== undefined && value !== null && value !== "";

const formatObservationDate = (dateString?: string) => {
  if (!dateString) {
    return "--";
  }

  const date = new Date(dateString);

  if (Number.isNaN(date.getTime())) {
    return "--";
  }

  return formatDatetime(date, { mode: "standard" });
};

const getInterpretation = (
  observation: ObsRecord,
): OBSERVATION_INTERPRETATION => {
  if (!observation.meta?.assessValue || !isPresent(observation.value)) {
    return "--";
  }

  return observation.meta.assessValue(String(observation.value));
};

const toResultValue = (observation: ObsRecord) =>
  isPresent(observation.value) ? String(observation.value) : "--";

const toSortTimestamp = (observation: ObsRecord) => {
  const preferredTimestamp =
    observation.issued || observation.effectiveDateTime;
  const parsedTime = Date.parse(preferredTimestamp || "");

  return Number.isNaN(parsedTime) ? 0 : parsedTime;
};

const toStableId = (observation: ObsRecord, fallbackLabel: string) =>
  String(
    observation.id ||
      observation.uuid ||
      `${fallbackLabel}-${observation.effectiveDateTime || "unknown"}-${toResultValue(
        observation,
      )}`,
  );

const buildRowFromObservation = (
  observation: ObsRecord,
  testName: string,
): ReadonlyResultRow => ({
  id: toStableId(observation, testName),
  testName,
  result: toResultValue(observation),
  referenceRange: observation.meta?.range || "--",
  collectedAt: formatObservationDate(observation.effectiveDateTime),
  resultedAt: formatObservationDate(observation.issued),
  resultedAtRaw: observation.issued,
  interpretation: getInterpretation(observation),
  sortTimestamp: toSortTimestamp(observation),
});

const shouldReplaceRow = (
  existingRow: ReadonlyResultRow,
  candidateRow: ReadonlyResultRow,
) => {
  if (
    existingRow.referenceRange === "--" &&
    candidateRow.referenceRange !== "--"
  ) {
    return true;
  }

  if (!existingRow.resultedAtRaw && candidateRow.resultedAtRaw) {
    return true;
  }

  return false;
};

export const flattenPatientResults = (
  sortedObs: PatientData,
): ReadonlyResultRow[] => {
  const flattenedRows = new Map<string, ReadonlyResultRow>();

  Object.entries(sortedObs).forEach(([panelName, { entries, type }]) => {
    if (type === "Test") {
      entries.forEach((entry) => {
        const row = buildRowFromObservation(entry, entry.name || panelName);
        const existingRow = flattenedRows.get(row.id);

        if (!existingRow || shouldReplaceRow(existingRow, row)) {
          flattenedRows.set(row.id, row);
        }
      });
      return;
    }

    entries.forEach((entry) => {
      (entry.members || []).filter(Boolean).forEach((member) => {
        const row = buildRowFromObservation(member, member.name || panelName);
        const existingRow = flattenedRows.get(row.id);

        if (!existingRow || shouldReplaceRow(existingRow, row)) {
          flattenedRows.set(row.id, row);
        }
      });
    });
  });

  return Array.from(flattenedRows.values()).sort((left, right) => {
    return right.sortTimestamp - left.sortTimestamp;
  });
};

const getInterpretationClassName = (
  interpretation: OBSERVATION_INTERPRETATION,
) => {
  if (
    !interpretation ||
    interpretation === "NORMAL" ||
    interpretation === "--"
  ) {
    return "resultsTableValue";
  }

  return `resultsTableValue resultsTableValue--${interpretation
    .toLowerCase()
    .replace(/_/g, "-")}`;
};

const ReadonlyResultsTable: React.FC<ReadonlyResultsTableProps> = ({
  loading,
  rows,
}) => {
  const intl = useIntl();
  const [page, setPage] = React.useState(1);
  const [pageSize, setPageSize] = React.useState(DEFAULT_PAGE_SIZE);

  React.useEffect(() => {
    const lastPage = Math.max(1, Math.ceil(rows.length / pageSize));

    if (page > lastPage) {
      setPage(1);
    }
  }, [page, pageSize, rows.length]);

  const paginatedRows = React.useMemo(() => {
    const startIndex = (page - 1) * pageSize;

    return rows.slice(startIndex, startIndex + pageSize);
  }, [page, pageSize, rows]);

  const headers = React.useMemo(
    () => [
      {
        key: "testName",
        header: intl.formatMessage({ id: "column.name.testName" }),
      },
      {
        key: "result",
        header: intl.formatMessage({ id: "column.name.result" }),
      },
      {
        key: "referenceRange",
        header: intl.formatMessage({ id: "column.name.normalRange" }),
      },
      {
        key: "collectedAt",
        header: intl.formatMessage({
          id: "column.name.collected",
          defaultMessage: "Collected",
        }),
      },
      {
        key: "resultedAt",
        header: intl.formatMessage({
          id: "column.name.resulted",
          defaultMessage: "Resulted",
        }),
      },
    ],
    [intl],
  );

  if (loading) {
    return <DataTableSkeleton columnCount={headers.length} rowCount={5} />;
  }

  return (
    <div className="resultsTableSection">
      <TableContainer className="resultsTableContainer">
        <Table
          size="lg"
          aria-label={intl.formatMessage({ id: "label.test.results" })}
        >
          <TableHead>
            <TableRow>
              {headers.map((header) => (
                <TableHeader key={header.key}>{header.header}</TableHeader>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {paginatedRows.map((row) => (
              <TableRow key={row.id}>
                <TableCell className="resultsTableCell resultsTableCell--testName">
                  {row.testName}
                </TableCell>
                <TableCell className="resultsTableCell">
                  <span
                    className={getInterpretationClassName(row.interpretation)}
                  >
                    {row.result}
                  </span>
                </TableCell>
                <TableCell className="resultsTableCell">
                  {row.referenceRange}
                </TableCell>
                <TableCell className="resultsTableCell resultsTableCell--date">
                  {row.collectedAt}
                </TableCell>
                <TableCell className="resultsTableCell resultsTableCell--date">
                  {row.resultedAt}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      <Pagination
        className="resultsTablePagination"
        onChange={({ page, pageSize }) => {
          setPage(page);
          setPageSize(pageSize);
        }}
        page={page}
        pageSize={pageSize}
        pageSizes={[10, 20, 50, 100]}
        totalItems={rows.length}
        forwardText={intl.formatMessage({ id: "pagination.forward" })}
        backwardText={intl.formatMessage({ id: "pagination.backward" })}
        itemRangeText={(min, max, total) =>
          intl.formatMessage(
            { id: "pagination.item-range" },
            { min, max, total },
          )
        }
        itemsPerPageText={intl.formatMessage({
          id: "pagination.items-per-page",
        })}
        itemText={(min, max) =>
          intl.formatMessage({ id: "pagination.item" }, { min, max })
        }
        pageNumberText={intl.formatMessage({
          id: "pagination.page-number",
        })}
        pageRangeText={(_current, total) =>
          intl.formatMessage({ id: "pagination.page-range" }, { total })
        }
        pageText={(page, pagesUnknown) =>
          intl.formatMessage(
            { id: "pagination.page" },
            { page: pagesUnknown ? "" : page },
          )
        }
      />
    </div>
  );
};

export default ReadonlyResultsTable;

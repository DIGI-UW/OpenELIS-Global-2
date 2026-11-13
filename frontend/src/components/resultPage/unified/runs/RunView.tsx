import React, { useCallback, useEffect, useState } from "react";
import {
  Button,
  Checkbox,
  Column,
  Grid,
  Heading,
  InlineLoading,
  InlineNotification,
  Link,
  Pagination,
  Section,
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
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import { FlagChip } from "../flags";
import PageBreadCrumb from "../../../common/PageBreadCrumb";
import RunsChip from "./RunsChip";
import UnifiedResults from "../UnifiedResults";
import { formatRunTime, runSourceKey, runStateKey } from "./runFormat";
import {
  EMPTY_SELECTION,
  RUN_CHIPS,
  RunChip,
  RunChipCounts,
  RunSelection,
  chipCount,
  isEmptySelection,
  isSelected,
  selectAllInFilter,
  toggleRow,
} from "./runSelection";

export interface RunHeader {
  id: number;
  label: string;
  source: string;
  state: string;
  analyzerId?: string | null;
  analyzerName?: string | null;
  method?: string | null;
  labUnits: string[];
  operatorName?: string | null;
  openedAt?: string | null;
  lastActivityAt?: string | null;
  completedAt?: string | null;
  dispositioned: number;
  total: number;
  rawAvailable: boolean;
  chipCounts?: RunChipCounts;
}

interface RunRow {
  id: string;
  accessionNumber?: string | null;
  testName?: string | null;
  part?: string | null;
  value?: string | null;
  units?: string | null;
  control: boolean;
  duplicate: boolean;
  importIssueReason?: string | null;
  instrumentFlags?: string | null;
  flag?: string | null;
  category?: string | null;
  testId?: string | null;
  componentId?: string | null;
  stagedResult?: string | null;
}

interface RunRowsPage {
  rows: RunRow[];
  page: number;
  pageSize: number;
  totalRows: number;
}

interface SelectionSummary {
  selected: number;
  critical: number;
  abnormal: number;
  normal: number;
  skipped: {
    id: string;
    accessionNumber?: string | null;
    testName?: string | null;
    reason: string;
  }[];
}

const isSelectionSummary = (response: unknown): response is SelectionSummary =>
  Boolean(response) &&
  typeof (response as SelectionSummary).selected === "number" &&
  Array.isArray((response as SelectionSummary).skipped);

interface RunViewProps {
  runId: number;
  filtersReplaced?: boolean;
  onBack: () => void;
  onOpenRun: (runId: number) => void;
}

const PAGE_SIZES = [25, 50, 100];

/** The message as indented JSON, whether it arrives parsed or as JSON text. */
export const prettyJson = (message: unknown): string => {
  if (typeof message === "string") {
    try {
      return JSON.stringify(JSON.parse(message), null, 2);
    } catch {
      return message;
    }
  }
  return JSON.stringify(message, null, 2);
};

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sidenav.label.results", link: "/Results" },
];

/**
 * OGC-1200 FR-E1, E3, E5, E8: a run opened on the Results worklist. Its header
 * names where the run came from and how far its review has gone, and its
 * values page server-side. FR-E1a, E11, E11a, E12: the worklist chips filter
 * the run with counts over the whole run, and a selection follows the run
 * across pages and chips. FR-E10, E10a, E13: a row expands into the Results
 * Entry row with the analyzer's value prefilled and editable, and accepting it
 * is a Results Entry save. Bulk actions on a selection are still taken on the
 * analyzer import page, linked from the header.
 */
const RunView: React.FC<RunViewProps> = ({
  runId,
  filtersReplaced = false,
  onBack,
  onOpenRun,
}) => {
  const intl = useIntl();
  const [header, setHeader] = useState<RunHeader | null>(null);
  const [headerState, setHeaderState] = useState<
    "loading" | "ready" | "missing"
  >("loading");
  const [rows, setRows] = useState<RunRowsPage | null>(null);
  const [rowsLoading, setRowsLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(50);
  const [raw, setRaw] = useState<string | null>(null);
  const [rawState, setRawState] = useState<
    "hidden" | "loading" | "shown" | "failed"
  >("hidden");
  const [showFiltersNotice, setShowFiltersNotice] = useState(filtersReplaced);
  const [chip, setChip] = useState<RunChip>("ALL");
  const [selection, setSelection] = useState<RunSelection>(EMPTY_SELECTION);
  const [summaryFailed, setSummaryFailed] = useState(false);
  const [summaryAttempt, setSummaryAttempt] = useState(0);
  const [summary, setSummary] = useState<SelectionSummary | null>(null);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [reload, setReload] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setHeaderState("loading");
    setRaw(null);
    setRawState("hidden");
    setPage(1);
    setChip("ALL");
    setSelection(EMPTY_SELECTION);
    getFromOpenElisServer(
      `/rest/result-runs/${runId}`,
      (response?: RunHeader) => {
        setHeader(response ?? null);
        setHeaderState(response ? "ready" : "missing");
      },
      controller.signal,
    );
    return () => controller.abort();
  }, [runId]);

  useEffect(() => {
    if (!reload) {
      return;
    }
    const controller = new AbortController();
    getFromOpenElisServer(
      `/rest/result-runs/${runId}`,
      (response?: RunHeader) => {
        if (response) {
          setHeader(response);
        }
      },
      controller.signal,
    );
    return () => controller.abort();
  }, [runId, reload]);

  const accepted = useCallback(() => {
    setExpandedId(null);
    setSelection(EMPTY_SELECTION);
    setReload((n) => n + 1);
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    setRowsLoading(true);
    getFromOpenElisServer(
      `/rest/result-runs/${runId}/rows?chip=${chip}&page=${page}&pageSize=${pageSize}`,
      (response?: RunRowsPage) => {
        setRows(response ?? null);
        setRowsLoading(false);
      },
      controller.signal,
    );
    return () => controller.abort();
  }, [runId, chip, page, pageSize, reload]);

  useEffect(() => {
    setSummaryFailed(false);
    if (isEmptySelection(selection)) {
      setSummary(null);
      return;
    }
    setSummary(null);
    let current = true;
    postToOpenElisServerJsonResponse(
      `/rest/result-runs/${runId}/selection`,
      JSON.stringify(selection),
      (response?: SelectionSummary) => {
        if (!current) {
          return;
        }
        if (isSelectionSummary(response)) {
          setSummary(response);
        } else {
          setSummaryFailed(true);
        }
      },
    );
    return () => {
      current = false;
    };
  }, [runId, selection, summaryAttempt]);

  const chooseChip = (next: RunChip) => {
    setChip(next);
    setPage(1);
  };

  const pageRows = rows?.rows ?? [];
  const pageAllSelected =
    pageRows.length > 0 && pageRows.every((row) => isSelected(selection, row));
  const togglePage = () =>
    setSelection((current) =>
      pageRows
        .filter((row) => isSelected(current, row) === pageAllSelected)
        .reduce(toggleRow, current),
    );

  const toggleRaw = useCallback(() => {
    if (rawState === "shown" || rawState === "failed") {
      setRawState("hidden");
      return;
    }
    if (raw !== null) {
      setRawState("shown");
      return;
    }
    setRawState("loading");
    getFromOpenElisServer(
      `/rest/result-runs/${runId}/raw`,
      (bundle?: unknown) => {
        if (bundle) {
          setRaw(prettyJson(bundle));
          setRawState("shown");
        } else {
          setRawState("failed");
        }
      },
    );
  }, [raw, rawState, runId]);

  const statusTag = (row: RunRow) => {
    if (row.control) {
      return (
        <Tag size="sm" type="purple">
          <FormattedMessage id="run.qc.kind.control" />
        </Tag>
      );
    }
    if (row.duplicate) {
      return (
        <Tag size="sm" type="magenta">
          <FormattedMessage id="run.row.duplicate" />
        </Tag>
      );
    }
    if (row.importIssueReason) {
      return (
        <Tag size="sm" type="red">
          <FormattedMessage id="run.row.needsAttention" />
        </Tag>
      );
    }
    return (
      <Tag size="sm" type="green">
        <FormattedMessage id="run.row.ready" />
      </Tag>
    );
  };

  /**
   * FR-E10: what a run adds above the Results Entry row, then the row itself.
   * A control, a duplicate, a value the import could not place, or a value in a
   * held run is not accepted here, and the panel says why.
   */
  const runRowEntry = (row: RunRow) => {
    const notAccepted = row.control
      ? "run.entry.control"
      : row.duplicate
        ? "run.entry.duplicate"
        : row.importIssueReason
          ? "run.entry.importIssue"
          : header?.state === "HELD"
            ? "run.entry.held"
            : header?.state !== "OPEN"
              ? "run.entry.closed"
              : null;
    return (
      <div className="runRowEntry" data-testid={`run-row-entry-${row.id}`}>
        <p className="runRowProvenance" data-testid="run-row-provenance">
          <FormattedMessage
            id="run.entry.provenance"
            values={{
              run: header?.label ?? "",
              analyzer: header?.analyzerName || "—",
              test: `${row.testName || "—"}${row.part ? ` — ${row.part}` : ""}`,
              value: `${row.value || "—"}${row.units ? ` ${row.units}` : ""}`,
            }}
          />{" "}
          <FormattedMessage id="run.entry.noLot" />
        </p>
        {notAccepted || !row.accessionNumber || !row.value ? (
          <InlineNotification
            kind="info"
            lowContrast
            hideCloseButton
            title={intl.formatMessage(
              { id: notAccepted ?? "run.entry.noPendingRow" },
              { reason: row.importIssueReason ?? "" },
            )}
          />
        ) : (
          <UnifiedResults
            embedded
            accessionNumber={row.accessionNumber}
            runEntry={{
              runId,
              runRowId: row.id,
              testId: row.testId,
              componentId: row.componentId,
              value: row.stagedResult || row.value,
              reported: row.value,
              units: row.units,
              analyzerId: header?.analyzerId,
              onAccepted: accepted,
            }}
          />
        )}
      </div>
    );
  };

  const setting = (labelId: string, value?: React.ReactNode) => (
    <div className="runSetting">
      <div className="cds--label">
        <FormattedMessage id={labelId} />
      </div>
      <div data-testid={`run-setting-${labelId}`}>{value || "—"}</div>
    </div>
  );

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth className="unifiedResultsPage runView">
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage id="sidenav.label.results" />
            </Heading>
            {header && (
              <p className="runViewSubtitle" data-testid="run-subtitle">
                {header.label}
              </p>
            )}
          </Section>
        </Column>
        <Column lg={16} md={8} sm={4} className="runViewActions">
          <Button kind="tertiary" size="sm" onClick={onBack}>
            <FormattedMessage id="run.action.backToAll" />
          </Button>
          <RunsChip activeRunId={runId} onOpenRun={onOpenRun} />
        </Column>
        {showFiltersNotice && (
          <Column lg={16} md={8} sm={4}>
            <InlineNotification
              kind="info"
              lowContrast
              title={intl.formatMessage({ id: "run.filters.replacedTitle" })}
              subtitle={intl.formatMessage({
                id: "run.filters.replacedBody",
              })}
              onClose={() => {
                setShowFiltersNotice(false);
                return true;
              }}
            />
          </Column>
        )}
        {headerState === "loading" && (
          <Column lg={16} md={8} sm={4}>
            <InlineLoading />
          </Column>
        )}
        {headerState === "missing" && (
          <Column lg={16} md={8} sm={4}>
            <InlineNotification
              kind="error"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({ id: "run.notFound" })}
            />
          </Column>
        )}
        {header && (
          <Column lg={16} md={8} sm={4}>
            <Tile className="runHeaderTile" data-testid="run-settings">
              <h4>
                <FormattedMessage id="run.header.settings" />
              </h4>
              <div className="runSettings">
                {setting(
                  "run.label.source",
                  intl.formatMessage({ id: runSourceKey(header.source) }),
                )}
                {setting("label.results.analyzer", header.analyzerName)}
                {setting("label.results.method", header.method)}
                {setting("label.results.labUnit", header.labUnits.join(", "))}
                {setting("run.label.operator", header.operatorName)}
                {setting(
                  "run.label.opened",
                  header.openedAt && formatRunTime(intl, header.openedAt),
                )}
                {setting(
                  "run.label.lastActivity",
                  header.lastActivityAt &&
                    formatRunTime(intl, header.lastActivityAt),
                )}
                {setting(
                  "run.label.state",
                  <Tag
                    size="sm"
                    type={header.state === "HELD" ? "red" : "cyan"}
                  >
                    {intl.formatMessage({ id: runStateKey(header.state) })}
                  </Tag>,
                )}
              </div>
              <p className="runProgress" data-testid="run-progress">
                <FormattedMessage
                  id="run.progress"
                  values={{ done: header.dispositioned, total: header.total }}
                />
              </p>
              <div className="runHeaderActions">
                {header.rawAvailable && (
                  <Button kind="ghost" size="sm" onClick={toggleRaw}>
                    <FormattedMessage
                      id={
                        rawState === "shown"
                          ? "run.action.hideRaw"
                          : "run.action.viewRaw"
                      }
                    />
                  </Button>
                )}
                {header.analyzerId && header.state !== "COMPLETE" && (
                  <Link
                    href={`/AnalyzerResults?id=${encodeURIComponent(header.analyzerId)}`}
                  >
                    <FormattedMessage id="run.action.acceptOnImport" />
                  </Link>
                )}
              </div>
              {rawState === "loading" && <InlineLoading />}
              {rawState === "failed" && (
                <p className="runsChipNote">
                  <FormattedMessage id="run.raw.unavailable" />
                </p>
              )}
              {rawState === "shown" && raw && (
                <pre className="runRaw" data-testid="run-raw">
                  {raw}
                </pre>
              )}
            </Tile>
          </Column>
        )}
        {header && (
          <Column lg={16} md={8} sm={4}>
            <div className="runChips" data-testid="run-chips">
              {RUN_CHIPS.map((key) => (
                <Button
                  key={key}
                  size="sm"
                  kind={chip === key ? "primary" : "tertiary"}
                  aria-pressed={chip === key}
                  data-testid={`run-chip-${key}`}
                  onClick={() => chooseChip(key)}
                >
                  {intl.formatMessage({ id: `run.chip.${key}` })} (
                  {chipCount(header.chipCounts, key)})
                </Button>
              ))}
            </div>
            {!isEmptySelection(selection) && (
              <Tile className="runSelectionBar" data-testid="run-selection">
                <div className="runSelectionSummary">
                  {summaryFailed ? (
                    <div className="runSelectionFailed">
                      <InlineNotification
                        kind="error"
                        lowContrast
                        hideCloseButton
                        data-testid="run-selection-failed"
                        title={intl.formatMessage({
                          id: "run.bulk.summaryFailed",
                        })}
                      />
                      <Button
                        kind="ghost"
                        size="sm"
                        onClick={() => setSummaryAttempt((n) => n + 1)}
                      >
                        <FormattedMessage id="run.bulk.summaryRetry" />
                      </Button>
                    </div>
                  ) : summary ? (
                    <FormattedMessage
                      id="run.bulk.summary"
                      values={{
                        selected: summary.selected,
                        critical: summary.critical,
                        abnormal: summary.abnormal,
                        normal: summary.normal,
                      }}
                    />
                  ) : (
                    <InlineLoading />
                  )}
                </div>
                {summary && summary.skipped.length > 0 && (
                  <div data-testid="run-selection-skipped">
                    <p className="cds--label">
                      <FormattedMessage
                        id="run.bulk.skipped"
                        values={{ count: summary.skipped.length }}
                      />
                    </p>
                    <ul>
                      {summary.skipped.map((skippedRow) => (
                        <li key={skippedRow.id}>
                          {skippedRow.accessionNumber || "—"},{" "}
                          {skippedRow.testName || "—"}:{" "}
                          {intl.formatMessage({
                            id: `run.bulk.skip.${skippedRow.reason}`,
                          })}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
                <p className="runsChipNote">
                  <FormattedMessage id="run.bulk.acceptNext" />
                </p>
                <div className="runSelectionActions">
                  <Button
                    kind="ghost"
                    size="sm"
                    data-testid="run-select-all"
                    onClick={() => setSelection(selectAllInFilter(chip))}
                  >
                    <FormattedMessage
                      id="run.bulk.selectAllInFilter"
                      values={{ count: chipCount(header.chipCounts, chip) }}
                    />
                  </Button>
                  <Button
                    kind="ghost"
                    size="sm"
                    data-testid="run-clear-selection"
                    onClick={() => setSelection(EMPTY_SELECTION)}
                  >
                    <FormattedMessage id="run.bulk.clearSelection" />
                  </Button>
                </div>
              </Tile>
            )}
            <TableContainer
              title={intl.formatMessage(
                { id: "run.rowCount" },
                { count: rows?.totalRows ?? 0 },
              )}
            >
              <Table size="lg" aria-label={header.label} data-testid="run-rows">
                <TableHead>
                  <TableRow>
                    <TableHeader className="unifiedExpandHeader" />
                    <TableHeader>
                      <Checkbox
                        id={`run-select-page-${runId}`}
                        labelText={intl.formatMessage({
                          id: "run.bulk.selectPage",
                        })}
                        hideLabel
                        checked={pageAllSelected}
                        onChange={togglePage}
                      />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="run.row.labNumber" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="label.results.test" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="run.row.value" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="run.row.units" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="label.results.flag" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="run.row.status" />
                    </TableHeader>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {rows?.rows.map((row) => (
                    <React.Fragment key={row.id}>
                      <TableRow data-testid={`run-row-${row.id}`}>
                        <TableCell className="unifiedExpandCell">
                          <Button
                            kind="ghost"
                            size="sm"
                            className="unifiedExpandButton"
                            aria-expanded={expandedId === row.id}
                            aria-label={intl.formatMessage({
                              id:
                                expandedId === row.id
                                  ? "label.results.collapseRow"
                                  : "label.results.expandRow",
                            })}
                            data-testid={`run-row-expand-${row.id}`}
                            onClick={() =>
                              setExpandedId(
                                expandedId === row.id ? null : row.id,
                              )
                            }
                          >
                            {expandedId === row.id ? "▼" : "▶"}
                          </Button>
                        </TableCell>
                        <TableCell>
                          <Checkbox
                            id={`run-select-${row.id}`}
                            labelText={intl.formatMessage(
                              { id: "run.bulk.selectRow" },
                              { labNumber: row.accessionNumber || row.id },
                            )}
                            hideLabel
                            checked={isSelected(selection, row)}
                            onChange={() =>
                              setSelection((current) => toggleRow(current, row))
                            }
                          />
                        </TableCell>
                        <TableCell>{row.accessionNumber || "—"}</TableCell>
                        <TableCell>
                          {row.testName || "—"}
                          {row.part ? ` — ${row.part}` : ""}
                        </TableCell>
                        <TableCell>{row.value || "—"}</TableCell>
                        <TableCell>{row.units || "—"}</TableCell>
                        <TableCell>
                          <FlagChip flag={row.flag} />
                          {row.instrumentFlags ? ` ${row.instrumentFlags}` : ""}
                          {!row.flag && !row.instrumentFlags ? "—" : ""}
                        </TableCell>
                        <TableCell>{statusTag(row)}</TableCell>
                      </TableRow>
                      {expandedId === row.id && (
                        <TableRow className="unifiedExpandedRow">
                          <TableCell colSpan={8}>{runRowEntry(row)}</TableCell>
                        </TableRow>
                      )}
                    </React.Fragment>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
            {rowsLoading && <InlineLoading />}
            {!rowsLoading && rows && rows.totalRows === 0 && (
              <p className="runsChipNote">
                <FormattedMessage id="run.row.empty" />
              </p>
            )}
            {!rowsLoading && !rows && (
              <InlineNotification
                kind="error"
                lowContrast
                hideCloseButton
                title={intl.formatMessage({ id: "run.loadFailed" })}
              />
            )}
            {rows && rows.totalRows > 0 && (
              <Pagination
                page={page}
                pageSize={pageSize}
                pageSizes={PAGE_SIZES}
                totalItems={rows.totalRows}
                onChange={({
                  page: nextPage,
                  pageSize: nextSize,
                }: {
                  page: number;
                  pageSize: number;
                }) => {
                  if (nextSize !== pageSize) {
                    setPageSize(nextSize);
                    setPage(1);
                    return;
                  }
                  setPage(nextPage);
                }}
              />
            )}
          </Column>
        )}
      </Grid>
    </>
  );
};

export default RunView;

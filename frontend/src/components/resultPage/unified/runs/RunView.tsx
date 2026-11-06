import React, { useCallback, useEffect, useState } from "react";
import {
  Button,
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
import { getFromOpenElisServer } from "../../../utils/Utils";
import PageBreadCrumb from "../../../common/PageBreadCrumb";
import RunsChip from "./RunsChip";
import { formatRunTime, runSourceKey, runStateKey } from "./runFormat";

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
}

interface RunRowsPage {
  rows: RunRow[];
  page: number;
  pageSize: number;
  totalRows: number;
}

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
 * values page server-side. Accepting them arrives with the next slice; until
 * then they are accepted on the analyzer import page, linked from the header.
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

  useEffect(() => {
    const controller = new AbortController();
    setHeaderState("loading");
    setRaw(null);
    setRawState("hidden");
    setPage(1);
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
    const controller = new AbortController();
    setRowsLoading(true);
    getFromOpenElisServer(
      `/rest/result-runs/${runId}/rows?page=${page}&pageSize=${pageSize}`,
      (response?: RunRowsPage) => {
        setRows(response ?? null);
        setRowsLoading(false);
      },
      controller.signal,
    );
    return () => controller.abort();
  }, [runId, page, pageSize]);

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
            <TableContainer
              title={intl.formatMessage(
                { id: "run.rowCount" },
                { count: rows?.totalRows ?? 0 },
              )}
            >
              <Table size="lg" aria-label={header.label} data-testid="run-rows">
                <TableHead>
                  <TableRow>
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
                    <TableRow key={row.id} data-testid={`run-row-${row.id}`}>
                      <TableCell>{row.accessionNumber || "—"}</TableCell>
                      <TableCell>
                        {row.testName || "—"}
                        {row.part ? ` — ${row.part}` : ""}
                      </TableCell>
                      <TableCell>{row.value || "—"}</TableCell>
                      <TableCell>{row.units || "—"}</TableCell>
                      <TableCell>{row.instrumentFlags || "—"}</TableCell>
                      <TableCell>{statusTag(row)}</TableCell>
                    </TableRow>
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

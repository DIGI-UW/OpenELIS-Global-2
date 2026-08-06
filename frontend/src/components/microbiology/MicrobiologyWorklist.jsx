import React, { useEffect, useMemo, useState } from "react";
import { FilterRemove } from "@carbon/icons-react";
import {
  Button,
  ClickableTile,
  ContentSwitcher,
  DataTable,
  InlineNotification,
  Loading,
  OverflowMenu,
  OverflowMenuItem,
  Pagination,
  Select,
  SelectItem,
  Stack,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
  Tag,
  Tile,
} from "@carbon/react";
import { Link, useHistory, useLocation } from "react-router-dom";
import { useIntl } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";
import {
  getMicrobiologyCaseUrl,
  getMicrobiologyWorklistUrl,
  MICROBIOLOGY_WORKLIST_PAGE_SIZES,
  parseMicrobiologyWorklistSearch,
} from "./MicrobiologyRoutes";
import {
  markMicrobiologyReady,
  MICROBIOLOGY_WORKLIST_READY_MARK,
} from "./MicrobiologyPerformance";
import MicrobiologyService from "./MicrobiologyService";
import "./MicrobiologyWorklist.css";

const WORKFLOW_OPTIONS = ["BACTERIOLOGY", "MYCOBACTERIOLOGY_TB", "MYCOLOGY"];
const STAGE_OPTIONS = [
  "RECEIVED",
  "SETUP_RECORDED",
  "INCUBATING",
  "GROWTH_DETECTED",
  "NO_GROWTH_READY",
  "IDENTIFICATION",
  "AST_READY",
  "AST_IN_PROGRESS",
  "REVIEW_READY",
  "PRELIM_RELEASED",
];
const URGENCY_OPTIONS = ["HIGH", "ROUTINE"];
const DUE_OPTIONS = [
  "NEEDS_WORKFLOW",
  "SETUP",
  "ISOLATE_ID",
  "AST_ENTRY",
  "AST_REVIEW",
  "CASE_REVIEW",
];
const EMPTY_SUMMARY = {
  totalPending: 0,
  incubating: 0,
  positiveSignals: 0,
  growthDetected: 0,
  identification: 0,
  needsAstReview: 0,
  readyForCaseReview: 0,
  openCriticalFollowUps: 0,
  astInQueue: 0,
  astPendingSetup: 0,
  astInProgress: 0,
  astAwaitingResults: 0,
  astResultsIn: 0,
};

const asCount = (value) => {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : 0;
};

const summaryFromRows = (rows) =>
  rows.reduce(
    (summary, row) => ({
      ...summary,
      incubating: summary.incubating + (row.stage === "INCUBATING" ? 1 : 0),
      positiveSignals:
        summary.positiveSignals + (row.stage === "POSITIVE_SIGNAL" ? 1 : 0),
      growthDetected:
        summary.growthDetected + (row.stage === "GROWTH_DETECTED" ? 1 : 0),
      identification:
        summary.identification + (row.stage === "IDENTIFICATION" ? 1 : 0),
      needsAstReview: summary.needsAstReview + (row.needsAstReview ? 1 : 0),
      readyForCaseReview:
        summary.readyForCaseReview + (row.dueAction === "CASE_REVIEW" ? 1 : 0),
      openCriticalFollowUps:
        summary.openCriticalFollowUps +
        (row.hasOpenCriticalCommunication ? 1 : 0),
      astInQueue: summary.astInQueue + (row.grain === "ast" ? 1 : 0),
      astPendingSetup:
        summary.astPendingSetup + (row.astStatus === "PENDING_SETUP" ? 1 : 0),
      astInProgress:
        summary.astInProgress +
        (["IN_PROGRESS", "AWAITING_RESULTS", "REVIEWED"].includes(row.astStatus)
          ? 1
          : 0),
      astAwaitingResults:
        summary.astAwaitingResults +
        (row.astStatus === "AWAITING_RESULTS" ? 1 : 0),
      astResultsIn:
        summary.astResultsIn +
        (["RESULTS_IN", "QC_FAILED"].includes(row.astStatus) ? 1 : 0),
    }),
    { ...EMPTY_SUMMARY, totalPending: rows.length },
  );

const normalizeSummary = (summary, rows) => {
  const fallback = summaryFromRows(rows);
  return Object.keys(EMPTY_SUMMARY).reduce(
    (normalized, key) => ({
      ...normalized,
      [key]: asCount(summary?.[key] ?? fallback[key]),
    }),
    {},
  );
};

const normalizePageResponse = (response, filters) => {
  const rows = Array.isArray(response)
    ? response
    : Array.isArray(response?.rows)
      ? response.rows
      : [];
  return {
    rows,
    summary: normalizeSummary(response?.summary, rows),
    total: Array.isArray(response)
      ? response.length
      : Number.isInteger(response?.total)
        ? response.total
        : rows.length,
    page: Array.isArray(response)
      ? filters.page
      : response?.page || filters.page,
    pageSize: Array.isArray(response)
      ? filters.pageSize
      : response?.pageSize || filters.pageSize,
  };
};

const tagTypeForUrgency = (urgency) => (urgency === "HIGH" ? "red" : "gray");
const tagTypeForDueAction = (dueAction) =>
  dueAction === "AST_REVIEW" ? "purple" : "blue";
const tagTypeForStage = (stage) => {
  if (stage === "GROWTH_DETECTED") {
    return "red";
  }
  if (stage === "AST_IN_PROGRESS" || stage === "AST_READY") {
    return "purple";
  }
  if (stage === "REVIEW_READY" || stage === "NO_GROWTH_READY") {
    return "green";
  }
  return "cyan";
};
const DUE_ACTION_DETAIL_IDS = {
  AST_ENTRY: "microbiology.worklist.dueDetail.ast_entry",
  AST_REVIEW: "microbiology.worklist.dueDetail.ast_review",
  CASE_REVIEW: "microbiology.worklist.dueDetail.case_review",
  ISOLATE_ID: "microbiology.worklist.dueDetail.isolate_id",
  SETUP: "microbiology.worklist.dueDetail.setup",
  SUBCULTURE_GRAM_STAIN:
    "microbiology.worklist.dueDetail.subculture_gram_stain",
};

const MicrobiologyWorklist = ({ service = MicrobiologyService }) => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const filters = parseMicrobiologyWorklistSearch(location.search);
  const isAstGrain = filters.grain === "ast";
  const [worklistState, setWorklistState] = useState({
    current: {
      rows: [],
      summary: EMPTY_SUMMARY,
      total: 0,
      page: filters.page,
      pageSize: filters.pageSize,
    },
    previousRows: [],
  });
  const [requestState, setRequestState] = useState({
    search: null,
    status: "loading",
  });
  const [hasLoaded, setHasLoaded] = useState(false);
  const { current: worklist, previousRows } = worklistState;
  const responseMatchesLocation = requestState.search === location.search;
  const loading = !responseMatchesLocation || requestState.status === "loading";
  const hasLoadError =
    responseMatchesLocation && requestState.status === "error";

  useEffect(() => {
    let active = true;
    service
      .getWorklistRows(filters)
      .then((response) => {
        if (!active) {
          return;
        }
        const nextWorklist = normalizePageResponse(response, filters);
        setWorklistState(({ current }) => ({
          current: nextWorklist,
          previousRows: current.rows,
        }));
        setRequestState({ search: location.search, status: "success" });
        setHasLoaded(true);
      })
      .catch(() => {
        if (!active) {
          return;
        }
        setRequestState({ search: location.search, status: "error" });
      });
    return () => {
      active = false;
    };
  }, [location.search, service]);

  useEffect(() => {
    if (!loading && !hasLoadError) {
      markMicrobiologyReady(MICROBIOLOGY_WORKLIST_READY_MARK);
    }
  }, [hasLoadError, loading, location.search, worklist]);

  const updateFilters = (changes, resetPage = true) => {
    const nextState = {
      ...filters,
      ...changes,
      page: resetPage ? 1 : changes.page || filters.page,
    };
    history.push(getMicrobiologyWorklistUrl(nextState));
  };

  const hasFilters = Boolean(
    filters.status ||
    filters.workflow ||
    filters.stage ||
    filters.urgency ||
    filters.due ||
    filters.q ||
    filters.sort !== "priority" ||
    filters.page !== 1 ||
    filters.pageSize !== 20,
  );

  const headers = useMemo(() => {
    if (isAstGrain) {
      return [
        {
          key: "labNumber",
          header: intl.formatMessage({
            id: "microbiology.worklist.column.labNumber",
          }),
        },
        {
          key: "isolate",
          header: intl.formatMessage({
            id: "microbiology.worklist.column.isolate",
          }),
        },
        {
          key: "patient",
          header: intl.formatMessage({
            id: "microbiology.worklist.column.patient",
          }),
        },
        {
          key: "organism",
          header: intl.formatMessage({
            id: "microbiology.worklist.column.organism",
          }),
        },
        {
          key: "panel",
          header: intl.formatMessage({
            id: "microbiology.worklist.column.panel",
          }),
        },
        {
          key: "astStatus",
          header: intl.formatMessage({
            id: "microbiology.worklist.column.status",
          }),
        },
        {
          key: "started",
          header: intl.formatMessage({
            id: "microbiology.worklist.column.started",
          }),
        },
        {
          key: "priority",
          header: intl.formatMessage({
            id: "microbiology.worklist.column.priority",
          }),
        },
        {
          key: "action",
          header: intl.formatMessage({
            id: "microbiology.worklist.column.action",
          }),
        },
      ];
    }
    return [
      {
        key: "labNumber",
        header: intl.formatMessage({
          id: "microbiology.worklist.column.labNumber",
        }),
      },
      {
        key: "patient",
        header: intl.formatMessage({
          id: "microbiology.worklist.column.patient",
        }),
      },
      {
        key: "specimen",
        header: intl.formatMessage({
          id: "microbiology.worklist.column.specimen",
        }),
      },
      {
        key: "stage",
        header: intl.formatMessage({
          id: "microbiology.worklist.column.stage",
        }),
      },
      {
        key: "due",
        header: intl.formatMessage({ id: "microbiology.worklist.column.due" }),
      },
      {
        key: "priority",
        header: intl.formatMessage({
          id: "microbiology.worklist.column.priority",
        }),
      },
      {
        key: "lastActivity",
        header: intl.formatMessage({
          id: "microbiology.worklist.column.lastActivity",
        }),
      },
      {
        key: "action",
        header: intl.formatMessage({
          id: "microbiology.worklist.column.action",
        }),
      },
    ];
  }, [intl, isAstGrain]);

  const rowsById = useMemo(
    () =>
      Object.fromEntries(
        [...previousRows, ...worklist.rows].map((row) => [
          row.rowId || row.caseId,
          row,
        ]),
      ),
    [previousRows, worklist.rows],
  );
  const tableRows = useMemo(
    () =>
      worklist.rows.map((row) =>
        isAstGrain
          ? {
              id: row.rowId || row.astRunId || `setup:${row.isolateId}`,
              labNumber: row.accessionNumber || row.sampleItemId,
              isolate: row.isolateLabel,
              patient:
                row.patientDisplay ||
                intl.formatMessage({ id: "microbiology.worklist.notSet" }),
              organism:
                row.organismDisplay ||
                intl.formatMessage({
                  id: "microbiology.worklist.ast.pendingIdentification",
                }),
              panel:
                row.panelName ||
                row.panelId ||
                intl.formatMessage({ id: "microbiology.worklist.notSet" }),
              astStatus: formatMicrobiologyEnum(row.astStatus),
              started: row.astStartedAt
                ? intl.formatDate(row.astStartedAt, {
                    month: "short",
                    day: "2-digit",
                    hour: "2-digit",
                    minute: "2-digit",
                  })
                : intl.formatMessage({
                    id: "microbiology.worklist.notStarted",
                  }),
              priority: formatMicrobiologyEnum(row.priority || row.urgency),
              action: row.caseId,
            }
          : {
              id: row.rowId || row.caseId,
              labNumber: row.accessionNumber || row.sampleItemId,
              patient:
                row.patientDisplay ||
                intl.formatMessage({ id: "microbiology.worklist.notSet" }),
              specimen:
                row.specimenDisplay ||
                intl.formatMessage({ id: "microbiology.worklist.notSet" }),
              stage: formatMicrobiologyEnum(row.stage),
              due: formatMicrobiologyEnum(row.dueAction),
              priority: formatMicrobiologyEnum(row.priority || row.urgency),
              lastActivity:
                row.lastActivityBy ||
                intl.formatMessage({ id: "microbiology.worklist.notSet" }),
              action: row.caseId,
            },
      ),
    [intl, isAstGrain, worklist.rows],
  );
  const cultureSummaryTiles = [
    {
      id: "total",
      value: worklist.summary.totalPending,
      labelId: "microbiology.worklist.summary.totalPending",
      changes: { status: "", stage: "", due: "" },
      selected: !filters.status,
    },
    {
      id: "incubating",
      value: worklist.summary.incubating,
      labelId: "microbiology.worklist.summary.incubating",
      changes: { status: "incubating", stage: "", due: "" },
      selected: filters.status === "incubating",
    },
    {
      id: "positive",
      value: worklist.summary.positiveSignals,
      labelId: "microbiology.worklist.summary.positive",
      changes: { status: "positive", stage: "", due: "" },
      selected: filters.status === "positive",
    },
    {
      id: "growth",
      value: worklist.summary.growthDetected,
      labelId: "microbiology.worklist.summary.growthDetected",
      changes: { status: "growth", stage: "", due: "" },
      selected: filters.status === "growth",
    },
    {
      id: "case-review",
      value: worklist.summary.readyForCaseReview,
      labelId: "microbiology.worklist.summary.caseReview",
      changes: { status: "ready", stage: "", due: "" },
      selected: filters.status === "ready",
    },
  ];
  const astSummaryTiles = [
    {
      id: "in-queue",
      value: worklist.summary.astInQueue,
      labelId: "microbiology.worklist.summary.astInQueue",
      changes: { status: "" },
      selected: !filters.status,
    },
    {
      id: "pending-setup",
      value: worklist.summary.astPendingSetup,
      labelId: "microbiology.worklist.summary.astPendingSetup",
      changes: { status: "pending-setup" },
      selected: filters.status === "pending-setup",
    },
    {
      id: "in-progress",
      value: worklist.summary.astInProgress,
      labelId: "microbiology.worklist.summary.astInProgress",
      changes: { status: "in-progress" },
      selected: filters.status === "in-progress",
    },
    {
      id: "results-in",
      value: worklist.summary.astResultsIn,
      labelId: "microbiology.worklist.summary.astResultsIn",
      changes: { status: "results-in" },
      selected: filters.status === "results-in",
    },
  ];
  const summaryTiles = isAstGrain ? astSummaryTiles : cultureSummaryTiles;

  return (
    <main className="microbiology-worklist" data-testid="microbiology-worklist">
      <Stack gap={5}>
        <PageBreadCrumb
          breadcrumbs={[
            { label: "home.label", link: "/" },
            {
              label: "microbiology.navigation.worklist",
              link: getMicrobiologyWorklistUrl(filters),
              isCurrentPage: true,
            },
          ]}
        />
        <header className="microbiology-worklist__header">
          <div>
            <p className="microbiology-worklist__eyebrow">
              {intl.formatMessage({ id: "microbiology.worklist.sharedQueue" })}
            </p>
            <h1>{intl.formatMessage({ id: "microbiology.worklist.title" })}</h1>
            <p>
              {intl.formatMessage({ id: "microbiology.worklist.subtitle" })}
            </p>
          </div>
          <Tag type="blue">
            {intl.formatMessage({ id: "microbiology.worklist.queueStatus" })}
          </Tag>
        </header>
        <div className="microbiology-worklist__grain-control">
          <ContentSwitcher
            selectedIndex={isAstGrain ? 1 : 0}
            size="sm"
            onChange={({ name }) =>
              updateFilters({
                grain: name,
                status: "",
                stage: "",
                due: "",
                q: "",
              })
            }
          >
            <Switch
              name="cultures"
              text={intl.formatMessage({
                id: "microbiology.worklist.grain.cultures",
              })}
            />
            <Switch
              name="ast"
              text={intl.formatMessage({
                id: "microbiology.worklist.grain.ast",
              })}
            />
          </ContentSwitcher>
        </div>
        {hasLoadError && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({
              id: "microbiology.worklist.loadError.title",
            })}
            subtitle={intl.formatMessage({
              id: "microbiology.worklist.loadError.subtitle",
            })}
            hideCloseButton
          />
        )}
        <section
          className="microbiology-worklist__summary"
          aria-labelledby="microbiology-worklist-summary-heading"
        >
          <div className="microbiology-worklist__section-heading">
            <div>
              <h2 id="microbiology-worklist-summary-heading">
                {intl.formatMessage({
                  id: "microbiology.worklist.summary.title",
                })}
              </h2>
              <p>
                {intl.formatMessage({
                  id: "microbiology.worklist.summary.hint",
                })}
              </p>
            </div>
          </div>
          <div className="microbiology-worklist__summary-grid">
            {summaryTiles.map((tile) => (
              <ClickableTile
                key={tile.id}
                className="microbiology-worklist__summary-card"
                clicked={tile.selected}
                data-testid={`microbiology-worklist-summary-${tile.id}`}
                aria-label={intl.formatMessage({ id: tile.labelId })}
                onClick={() => updateFilters(tile.changes)}
              >
                <span className="microbiology-worklist__summary-value">
                  {tile.value}
                </span>
                <span className="microbiology-worklist__summary-label">
                  {intl.formatMessage({ id: tile.labelId })}
                </span>
              </ClickableTile>
            ))}
            {isAstGrain ? (
              <Tile
                className="microbiology-worklist__summary-card microbiology-worklist__summary-card--disabled"
                title={intl.formatMessage({
                  id: "microbiology.worklist.phase1b",
                })}
              >
                <span className="microbiology-worklist__summary-value">-</span>
                <span className="microbiology-worklist__summary-label">
                  {intl.formatMessage({
                    id: "microbiology.worklist.summary.expertFlags",
                  })}
                </span>
              </Tile>
            ) : (
              <Tile
                className="microbiology-worklist__summary-card microbiology-worklist__summary-card--static"
                data-testid="microbiology-worklist-summary-critical"
              >
                <span className="microbiology-worklist__summary-value">
                  {worklist.summary.openCriticalFollowUps}
                </span>
                <span className="microbiology-worklist__summary-label">
                  {intl.formatMessage({
                    id: "microbiology.worklist.summary.critical",
                  })}
                </span>
              </Tile>
            )}
          </div>
        </section>
        {isAstGrain && (
          <InlineNotification
            kind="info"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "microbiology.worklist.ast.automaticResults",
            })}
            subtitle={intl.formatMessage({
              id: "microbiology.worklist.ast.automaticResultsDetail",
            })}
          />
        )}
        <section
          className="microbiology-worklist__filters"
          aria-labelledby="microbiology-worklist-filter-heading"
        >
          <div className="microbiology-worklist__filter-header">
            <h2 id="microbiology-worklist-filter-heading">
              {intl.formatMessage({ id: "microbiology.worklist.filters" })}
            </h2>
            <Button
              kind="ghost"
              size="sm"
              renderIcon={FilterRemove}
              disabled={!hasFilters}
              onClick={() =>
                history.push(
                  getMicrobiologyWorklistUrl({ grain: filters.grain }),
                )
              }
            >
              {intl.formatMessage({ id: "microbiology.worklist.clearFilters" })}
            </Button>
          </div>
          <div className="microbiology-worklist__filter-grid">
            <Select
              id="microbiology-worklist-workflow-filter"
              labelText={intl.formatMessage({
                id: "microbiology.worklist.filter.workflow",
              })}
              value={filters.workflow}
              onChange={(event) =>
                updateFilters({ workflow: event.target.value })
              }
            >
              <SelectItem
                value=""
                text={intl.formatMessage({
                  id: "microbiology.worklist.filter.allWorkflows",
                })}
              />
              {WORKFLOW_OPTIONS.map((workflow) => (
                <SelectItem
                  key={workflow}
                  value={workflow}
                  text={formatMicrobiologyEnum(workflow)}
                />
              ))}
            </Select>
            {!isAstGrain && (
              <Select
                id="microbiology-worklist-stage-filter"
                labelText={intl.formatMessage({
                  id: "microbiology.worklist.filter.stage",
                })}
                value={filters.stage}
                onChange={(event) =>
                  updateFilters({ stage: event.target.value, due: "" })
                }
              >
                <SelectItem
                  value=""
                  text={intl.formatMessage({
                    id: "microbiology.worklist.filter.allStages",
                  })}
                />
                {STAGE_OPTIONS.map((stage) => (
                  <SelectItem
                    key={stage}
                    value={stage}
                    text={formatMicrobiologyEnum(stage)}
                  />
                ))}
              </Select>
            )}
            {!isAstGrain && (
              <Select
                id="microbiology-worklist-due-filter"
                labelText={intl.formatMessage({
                  id: "microbiology.worklist.filter.dueAction",
                })}
                value={filters.due}
                onChange={(event) =>
                  updateFilters({ due: event.target.value, stage: "" })
                }
              >
                <SelectItem
                  value=""
                  text={intl.formatMessage({
                    id: "microbiology.worklist.filter.allActions",
                  })}
                />
                {DUE_OPTIONS.map((dueAction) => (
                  <SelectItem
                    key={dueAction}
                    value={dueAction}
                    text={formatMicrobiologyEnum(dueAction)}
                  />
                ))}
              </Select>
            )}
            <Select
              id="microbiology-worklist-urgency-filter"
              labelText={intl.formatMessage({
                id: "microbiology.worklist.filter.urgency",
              })}
              value={filters.urgency}
              onChange={(event) =>
                updateFilters({ urgency: event.target.value })
              }
            >
              <SelectItem
                value=""
                text={intl.formatMessage({
                  id: "microbiology.worklist.filter.allUrgencies",
                })}
              />
              {URGENCY_OPTIONS.map((urgency) => (
                <SelectItem
                  key={urgency}
                  value={urgency}
                  text={formatMicrobiologyEnum(urgency)}
                />
              ))}
            </Select>
            <Select
              id="microbiology-worklist-sort"
              labelText={intl.formatMessage({
                id: "microbiology.worklist.sort",
              })}
              value={filters.sort}
              onChange={(event) => updateFilters({ sort: event.target.value })}
            >
              <SelectItem
                value="priority"
                text={intl.formatMessage({
                  id: "microbiology.worklist.sort.priority",
                })}
              />
              <SelectItem
                value="newest"
                text={intl.formatMessage({
                  id: "microbiology.worklist.sort.newest",
                })}
              />
              <SelectItem
                value="workflow"
                text={intl.formatMessage({
                  id: "microbiology.worklist.sort.workflow",
                })}
              />
            </Select>
          </div>
        </section>
        {loading && !hasLoaded ? (
          <Loading withOverlay={false} />
        ) : (
          <section
            className="microbiology-worklist__table-section"
            aria-labelledby="microbiology-worklist-table-heading"
            aria-busy={loading}
          >
            <div className="microbiology-worklist__section-heading">
              <div>
                <h2 id="microbiology-worklist-table-heading">
                  {intl.formatMessage({
                    id: isAstGrain
                      ? "microbiology.worklist.table.astTitle"
                      : "microbiology.worklist.table.title",
                  })}
                </h2>
                <p>
                  {intl.formatMessage({
                    id: "microbiology.worklist.table.hint",
                  })}
                </p>
              </div>
            </div>
            <DataTable rows={tableRows} headers={headers}>
              {({
                rows,
                headers: tableHeaders,
                getHeaderProps,
                getRowProps,
                getTableProps,
                getToolbarProps,
              }) => (
                <TableContainer>
                  <TableToolbar {...getToolbarProps()}>
                    <TableToolbarContent>
                      <TableToolbarSearch
                        persistent
                        value={filters.q}
                        placeholder={intl.formatMessage({
                          id: isAstGrain
                            ? "microbiology.worklist.search.ast"
                            : "microbiology.worklist.search.cultures",
                        })}
                        onChange={(event) =>
                          updateFilters({ q: event.target.value })
                        }
                      />
                    </TableToolbarContent>
                  </TableToolbar>
                  <div
                    className="microbiology-worklist__table-scroll"
                    tabIndex={0}
                    aria-label={intl.formatMessage({
                      id: isAstGrain
                        ? "microbiology.worklist.table.astTitle"
                        : "microbiology.worklist.table.title",
                    })}
                  >
                    <div className="microbiology-worklist__table-content">
                      <Table {...getTableProps()}>
                        <TableHead>
                          <TableRow>
                            {tableHeaders.map((header) => (
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
                          {rows.length === 0 ? (
                            <TableRow>
                              <TableCell colSpan={tableHeaders.length}>
                                {intl.formatMessage({
                                  id: isAstGrain
                                    ? "microbiology.worklist.empty.ast"
                                    : "microbiology.worklist.empty.cultures",
                                })}
                              </TableCell>
                            </TableRow>
                          ) : (
                            rows.map((tableRow) => {
                              const row = rowsById[tableRow.id];
                              const caseUrl = getMicrobiologyCaseUrl(
                                row.caseId,
                                isAstGrain
                                  ? {
                                      ...filters,
                                      section: "ast",
                                      astIsolateId: row.isolateId,
                                      astRunId: row.astRunId,
                                    }
                                  : filters,
                              );
                              return (
                                <TableRow
                                  key={tableRow.id}
                                  {...getRowProps({ row: tableRow })}
                                  data-testid={`microbiology-worklist-row-${tableRow.id}`}
                                >
                                  {tableRow.cells.map((cell) => {
                                    if (cell.info.header === "labNumber") {
                                      return (
                                        <TableCell key={cell.id}>
                                          <div className="microbiology-worklist__lab-context">
                                            <Link
                                              className="microbiology-worklist__case-link"
                                              to={caseUrl}
                                            >
                                              {cell.value}
                                            </Link>
                                            <Tag type="outline">
                                              {formatMicrobiologyEnum(
                                                row.workflowType,
                                              )}
                                            </Tag>
                                            {row.siblingWorkflows?.length >
                                              0 && (
                                              <span data-testid="microbiology-worklist-siblings">
                                                {intl.formatMessage(
                                                  {
                                                    id: "microbiology.worklist.linkedWorkflows",
                                                  },
                                                  {
                                                    count:
                                                      row.siblingWorkflows
                                                        .length + 1,
                                                  },
                                                )}
                                              </span>
                                            )}
                                          </div>
                                        </TableCell>
                                      );
                                    }
                                    if (cell.info.header === "stage") {
                                      return (
                                        <TableCell key={cell.id}>
                                          <div className="microbiology-worklist__stage-status">
                                            <Tag
                                              type={tagTypeForStage(row.stage)}
                                            >
                                              {cell.value}
                                            </Tag>
                                            {row.needsAstReview && (
                                              <span>
                                                {intl.formatMessage({
                                                  id: "microbiology.worklist.ast.resultsInBadge",
                                                })}
                                              </span>
                                            )}
                                            {row.hasOpenCriticalCommunication && (
                                              <span>
                                                {intl.formatMessage({
                                                  id: "microbiology.worklist.critical",
                                                })}
                                              </span>
                                            )}
                                          </div>
                                        </TableCell>
                                      );
                                    }
                                    if (cell.info.header === "astStatus") {
                                      return (
                                        <TableCell key={cell.id}>
                                          <div className="microbiology-worklist__ast-status">
                                            <Tag
                                              type={tagTypeForStage(
                                                row.astStatus,
                                              )}
                                            >
                                              {cell.value}
                                            </Tag>
                                            {row.analyzerResultsAvailable && (
                                              <span>
                                                {intl.formatMessage({
                                                  id: "microbiology.worklist.ast.resultsInBadge",
                                                })}
                                              </span>
                                            )}
                                            {row.astStatus ===
                                              "AWAITING_RESULTS" && (
                                              <span>
                                                {intl.formatMessage({
                                                  id: "microbiology.worklist.ast.awaitingResults",
                                                })}
                                              </span>
                                            )}
                                          </div>
                                        </TableCell>
                                      );
                                    }
                                    if (cell.info.header === "due") {
                                      const detailId =
                                        DUE_ACTION_DETAIL_IDS[row.dueAction];
                                      return (
                                        <TableCell key={cell.id}>
                                          <div className="microbiology-worklist__due-action">
                                            <Tag
                                              type={tagTypeForDueAction(
                                                row.dueAction,
                                              )}
                                            >
                                              {cell.value}
                                            </Tag>
                                            {detailId && (
                                              <span>
                                                {intl.formatMessage({
                                                  id: detailId,
                                                })}
                                              </span>
                                            )}
                                          </div>
                                        </TableCell>
                                      );
                                    }
                                    if (cell.info.header === "priority") {
                                      return (
                                        <TableCell key={cell.id}>
                                          <Tag
                                            type={tagTypeForUrgency(
                                              row.urgency,
                                            )}
                                          >
                                            {cell.value}
                                          </Tag>
                                        </TableCell>
                                      );
                                    }
                                    if (cell.info.header === "lastActivity") {
                                      return (
                                        <TableCell key={cell.id}>
                                          <div className="microbiology-worklist__last-activity">
                                            <span>{cell.value}</span>
                                            {row.lastActivityAt && (
                                              <span>
                                                {intl.formatDate(
                                                  row.lastActivityAt,
                                                  {
                                                    month: "short",
                                                    day: "2-digit",
                                                    hour: "2-digit",
                                                    minute: "2-digit",
                                                  },
                                                )}
                                              </span>
                                            )}
                                          </div>
                                        </TableCell>
                                      );
                                    }
                                    if (cell.info.header === "action") {
                                      const caseActionUrl = (section, action) =>
                                        getMicrobiologyCaseUrl(row.caseId, {
                                          ...filters,
                                          section,
                                          action,
                                          astIsolateId:
                                            section === "ast"
                                              ? row.isolateId
                                              : "",
                                          astRunId:
                                            section === "ast"
                                              ? row.astRunId
                                              : "",
                                        });
                                      return (
                                        <TableCell key={cell.id}>
                                          <OverflowMenu
                                            ariaLabel={intl.formatMessage({
                                              id: "microbiology.worklist.rowActions",
                                            })}
                                            iconDescription={intl.formatMessage(
                                              {
                                                id: "microbiology.worklist.rowActions",
                                              },
                                            )}
                                            size="sm"
                                            flipped
                                          >
                                            <OverflowMenuItem
                                              itemText={intl.formatMessage({
                                                id: "microbiology.worklist.openCase",
                                              })}
                                              onClick={() =>
                                                history.push(caseUrl)
                                              }
                                            />
                                            {isAstGrain && (
                                              <OverflowMenuItem
                                                itemText={intl.formatMessage({
                                                  id: row.astRunId
                                                    ? "microbiology.worklist.editAst"
                                                    : "microbiology.worklist.setupAst",
                                                })}
                                                onClick={() =>
                                                  history.push(
                                                    caseActionUrl("ast", ""),
                                                  )
                                                }
                                              />
                                            )}
                                            {isAstGrain && (
                                              <OverflowMenuItem
                                                itemText={intl.formatMessage({
                                                  id: "microbiology.worklist.viewAudit",
                                                })}
                                                onClick={() =>
                                                  history.push(
                                                    caseActionUrl(
                                                      "timeline",
                                                      "",
                                                    ),
                                                  )
                                                }
                                              />
                                            )}
                                            {isAstGrain && row.astRunId && (
                                              <OverflowMenuItem
                                                itemText={intl.formatMessage({
                                                  id: "microbiology.worklist.newAstAttempt",
                                                })}
                                                disabled={
                                                  row.astStatus !== "REVIEWED"
                                                }
                                                onClick={() =>
                                                  history.push(
                                                    caseActionUrl(
                                                      "ast",
                                                      "new-ast-attempt",
                                                    ),
                                                  )
                                                }
                                              />
                                            )}
                                            {!isAstGrain && (
                                              <OverflowMenuItem
                                                itemText={intl.formatMessage({
                                                  id: "microbiology.worklist.markPositive",
                                                })}
                                                disabled={
                                                  row.stage !== "INCUBATING"
                                                }
                                                onClick={() =>
                                                  history.push(
                                                    caseActionUrl(
                                                      "setup",
                                                      "mark-positive",
                                                    ),
                                                  )
                                                }
                                              />
                                            )}
                                            {!isAstGrain && (
                                              <OverflowMenuItem
                                                itemText={intl.formatMessage({
                                                  id: "microbiology.worklist.markNoGrowth",
                                                })}
                                                disabled={
                                                  row.stage !== "INCUBATING"
                                                }
                                                onClick={() =>
                                                  history.push(
                                                    caseActionUrl(
                                                      "setup",
                                                      "mark-no-growth",
                                                    ),
                                                  )
                                                }
                                              />
                                            )}
                                            {!isAstGrain && (
                                              <OverflowMenuItem
                                                itemText={intl.formatMessage({
                                                  id: "microbiology.worklist.markLost",
                                                })}
                                                isDelete
                                                onClick={() =>
                                                  history.push(
                                                    caseActionUrl(
                                                      "nonconformance",
                                                      "mark-lost",
                                                    ),
                                                  )
                                                }
                                              />
                                            )}
                                          </OverflowMenu>
                                        </TableCell>
                                      );
                                    }
                                    return (
                                      <TableCell key={cell.id}>
                                        {cell.value}
                                      </TableCell>
                                    );
                                  })}
                                </TableRow>
                              );
                            })
                          )}
                        </TableBody>
                      </Table>
                    </div>
                  </div>
                </TableContainer>
              )}
            </DataTable>
          </section>
        )}
        {hasLoaded && (
          <Pagination
            page={worklist.page}
            pageSize={worklist.pageSize}
            pageSizes={MICROBIOLOGY_WORKLIST_PAGE_SIZES}
            totalItems={worklist.total}
            onChange={({ page, pageSize }) =>
              updateFilters({ page, pageSize }, false)
            }
          />
        )}
      </Stack>
    </main>
  );
};

export default MicrobiologyWorklist;

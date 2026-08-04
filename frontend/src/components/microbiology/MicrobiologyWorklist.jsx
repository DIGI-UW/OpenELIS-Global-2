import React, { useEffect, useMemo, useState } from "react";
import { ArrowRight, FilterRemove } from "@carbon/icons-react";
import {
  Button,
  ClickableTile,
  DataTable,
  IconButton,
  InlineNotification,
  Layer,
  Loading,
  Pagination,
  Select,
  SelectItem,
  Stack,
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
  "SETUP",
  "ISOLATE_ID",
  "AST_ENTRY",
  "AST_REVIEW",
  "CASE_REVIEW",
];
const EMPTY_SUMMARY = {
  totalPending: 0,
  incubating: 0,
  growthDetected: 0,
  identification: 0,
  needsAstReview: 0,
  readyForCaseReview: 0,
  openCriticalFollowUps: 0,
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
const dueActionDetailId = (dueAction) =>
  `microbiology.worklist.dueDetail.${String(dueAction || "").toLowerCase()}`;

const MicrobiologyWorklist = ({ service = MicrobiologyService }) => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const filters = parseMicrobiologyWorklistSearch(location.search);
  const [worklist, setWorklist] = useState({
    rows: [],
    summary: EMPTY_SUMMARY,
    total: 0,
    page: filters.page,
    pageSize: filters.pageSize,
  });
  const [loading, setLoading] = useState(true);
  const [hasLoadError, setHasLoadError] = useState(false);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setHasLoadError(false);
    service
      .getWorklistRows(filters)
      .then((response) => {
        if (!active) {
          return;
        }
        setWorklist(normalizePageResponse(response, filters));
        setLoading(false);
      })
      .catch(() => {
        if (!active) {
          return;
        }
        setHasLoadError(true);
        setLoading(false);
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
    filters.workflow ||
    filters.stage ||
    filters.urgency ||
    filters.due ||
    filters.q ||
    filters.sort !== "priority" ||
    filters.page !== 1 ||
    filters.pageSize !== 20,
  );

  const headers = useMemo(
    () => [
      {
        key: "sampleItem",
        header: intl.formatMessage({
          id: "microbiology.worklist.column.sample",
        }),
      },
      {
        key: "workflow",
        header: intl.formatMessage({
          id: "microbiology.worklist.column.workflow",
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
        key: "urgency",
        header: intl.formatMessage({
          id: "microbiology.worklist.column.urgency",
        }),
      },
      {
        key: "context",
        header: intl.formatMessage({
          id: "microbiology.worklist.column.context",
        }),
      },
      {
        key: "action",
        header: intl.formatMessage({
          id: "microbiology.worklist.column.action",
        }),
      },
    ],
    [intl],
  );

  const rowsById = useMemo(
    () => Object.fromEntries(worklist.rows.map((row) => [row.caseId, row])),
    [worklist.rows],
  );
  const tableRows = useMemo(
    () =>
      worklist.rows.map((row) => ({
        id: row.caseId,
        sampleItem: row.sampleItemId,
        workflow: formatMicrobiologyEnum(row.workflowType),
        stage: formatMicrobiologyEnum(row.stage),
        due: formatMicrobiologyEnum(row.dueAction),
        urgency: formatMicrobiologyEnum(row.urgency),
        context: row.siblingWorkflows?.join(", ") || "",
        action: row.caseId,
      })),
    [worklist.rows],
  );
  const summaryTiles = [
    {
      id: "total",
      value: worklist.summary.totalPending,
      labelId: "microbiology.worklist.summary.totalPending",
      changes: { stage: "", due: "" },
      selected: !filters.stage && !filters.due,
    },
    {
      id: "incubating",
      value: worklist.summary.incubating,
      labelId: "microbiology.worklist.summary.incubating",
      changes: { stage: "INCUBATING", due: "" },
      selected: filters.stage === "INCUBATING" && !filters.due,
    },
    {
      id: "growth",
      value: worklist.summary.growthDetected,
      labelId: "microbiology.worklist.summary.growthDetected",
      changes: { stage: "GROWTH_DETECTED", due: "" },
      selected: filters.stage === "GROWTH_DETECTED" && !filters.due,
    },
    {
      id: "identification",
      value: worklist.summary.identification,
      labelId: "microbiology.worklist.summary.identification",
      changes: { stage: "IDENTIFICATION", due: "" },
      selected: filters.stage === "IDENTIFICATION" && !filters.due,
    },
    {
      id: "ast-review",
      value: worklist.summary.needsAstReview,
      labelId: "microbiology.worklist.summary.astReview",
      changes: { stage: "", due: "AST_REVIEW" },
      selected: !filters.stage && filters.due === "AST_REVIEW",
    },
    {
      id: "case-review",
      value: worklist.summary.readyForCaseReview,
      labelId: "microbiology.worklist.summary.caseReview",
      changes: { stage: "", due: "CASE_REVIEW" },
      selected: !filters.stage && filters.due === "CASE_REVIEW",
    },
  ];

  return (
    <main className="microbiology-worklist" data-testid="microbiology-worklist">
      <Stack gap={7}>
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
          </div>
        </section>
        <Layer
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
              onClick={() => history.push(getMicrobiologyWorklistUrl())}
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
        </Layer>
        {loading ? (
          <Loading withOverlay={false} />
        ) : (
          <section
            className="microbiology-worklist__table-section"
            aria-labelledby="microbiology-worklist-table-heading"
          >
            <div className="microbiology-worklist__section-heading">
              <div>
                <h2 id="microbiology-worklist-table-heading">
                  {intl.formatMessage({
                    id: "microbiology.worklist.table.title",
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
                          id: "microbiology.worklist.search",
                        })}
                        onChange={(event) =>
                          updateFilters({ q: event.target.value })
                        }
                      />
                    </TableToolbarContent>
                  </TableToolbar>
                  <div className="microbiology-worklist__table-scroll">
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
                                  id: "microbiology.worklist.empty",
                                })}
                              </TableCell>
                            </TableRow>
                          ) : (
                            rows.map((tableRow) => {
                              const row = rowsById[tableRow.id];
                              const caseUrl = getMicrobiologyCaseUrl(
                                row.caseId,
                                filters,
                              );
                              return (
                                <TableRow
                                  key={tableRow.id}
                                  {...getRowProps({ row: tableRow })}
                                  data-testid={`microbiology-worklist-row-${tableRow.id}`}
                                >
                                  {tableRow.cells.map((cell) => {
                                    if (cell.info.header === "sampleItem") {
                                      return (
                                        <TableCell key={cell.id}>
                                          <Link
                                            className="microbiology-worklist__case-link"
                                            to={caseUrl}
                                          >
                                            {cell.value}
                                          </Link>
                                        </TableCell>
                                      );
                                    }
                                    if (cell.info.header === "stage") {
                                      return (
                                        <TableCell key={cell.id}>
                                          <Tag
                                            type={tagTypeForStage(row.stage)}
                                          >
                                            {cell.value}
                                          </Tag>
                                        </TableCell>
                                      );
                                    }
                                    if (cell.info.header === "due") {
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
                                            <span>
                                              {intl.formatMessage({
                                                id: dueActionDetailId(
                                                  row.dueAction,
                                                ),
                                              })}
                                            </span>
                                          </div>
                                        </TableCell>
                                      );
                                    }
                                    if (cell.info.header === "urgency") {
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
                                    if (cell.info.header === "context") {
                                      return (
                                        <TableCell key={cell.id}>
                                          <div className="microbiology-worklist__context">
                                            {row.hasOpenCriticalCommunication && (
                                              <Tag type="magenta">
                                                {intl.formatMessage({
                                                  id: "microbiology.worklist.critical",
                                                })}
                                              </Tag>
                                            )}
                                            {row.siblingWorkflows?.length >
                                              0 && (
                                              <span data-testid="microbiology-worklist-siblings">
                                                {intl.formatMessage({
                                                  id: "microbiology.worklist.siblings",
                                                })}
                                                {": "}
                                                {row.siblingWorkflows
                                                  .map(formatMicrobiologyEnum)
                                                  .join(", ")}
                                              </span>
                                            )}
                                          </div>
                                        </TableCell>
                                      );
                                    }
                                    if (cell.info.header === "action") {
                                      return (
                                        <TableCell key={cell.id}>
                                          <IconButton
                                            label={intl.formatMessage({
                                              id: "microbiology.worklist.openCase",
                                            })}
                                            kind="ghost"
                                            size="sm"
                                            onClick={() =>
                                              history.push(caseUrl)
                                            }
                                          >
                                            <ArrowRight />
                                          </IconButton>
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
        {!loading && (
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

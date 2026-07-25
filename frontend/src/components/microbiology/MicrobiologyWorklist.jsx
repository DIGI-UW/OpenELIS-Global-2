import React, { useEffect, useState } from "react";
import {
  Button,
  Loading,
  Select,
  SelectItem,
  Stack,
  Tag,
  Tile,
} from "@carbon/react";
import { useHistory, useLocation } from "react-router-dom";
import { useIntl } from "react-intl";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";
import {
  getMicrobiologyCaseUrl,
  getMicrobiologyWorklistUrl,
  parseMicrobiologyWorklistSearch,
} from "./MicrobiologyRoutes";
import MicrobiologyService from "./MicrobiologyService";
import "./MicrobiologyWorklist.css";

const compareRows = (sort) => (left, right) => {
  if (sort === "workflow") {
    return left.workflowType.localeCompare(right.workflowType);
  }
  if (sort === "newest") {
    return new Date(right.createdAt || 0) - new Date(left.createdAt || 0);
  }
  return 0;
};

const MicrobiologyWorklist = ({ service = MicrobiologyService }) => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const filters = parseMicrobiologyWorklistSearch(location.search);

  useEffect(() => {
    service.getWorklistRows().then((worklistRows) => {
      setRows(Array.isArray(worklistRows) ? worklistRows : []);
      setLoading(false);
    });
  }, [service]);

  if (loading) {
    return <Loading withOverlay={false} />;
  }

  const workflowTypes = [
    ...new Set(rows.map((row) => row.workflowType)),
  ].sort();
  const urgencyLevels = [...new Set(rows.map((row) => row.urgency))].sort();
  const dueActions = [...new Set(rows.map((row) => row.dueAction))].sort();
  const visibleRows = rows
    .filter((row) => !filters.workflow || row.workflowType === filters.workflow)
    .filter((row) => !filters.urgency || row.urgency === filters.urgency)
    .filter((row) => !filters.due || row.dueAction === filters.due)
    .sort(compareRows(filters.sort));

  const updateFilters = (changes) => {
    history.push(getMicrobiologyWorklistUrl({ ...filters, ...changes }));
  };

  const hasFilters = Boolean(
    filters.workflow ||
    filters.urgency ||
    filters.due ||
    filters.sort !== "priority",
  );

  return (
    <main className="microbiology-worklist" data-testid="microbiology-worklist">
      <Stack gap={5}>
        <h2>{intl.formatMessage({ id: "microbiology.worklist.title" })}</h2>
        <section
          className="microbiology-worklist__filters"
          aria-label={intl.formatMessage({
            id: "microbiology.worklist.filters",
          })}
        >
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
            {workflowTypes.map((workflowType) => (
              <SelectItem
                key={workflowType}
                value={workflowType}
                text={formatMicrobiologyEnum(workflowType)}
              />
            ))}
          </Select>
          <Select
            id="microbiology-worklist-urgency-filter"
            labelText={intl.formatMessage({
              id: "microbiology.worklist.filter.urgency",
            })}
            value={filters.urgency}
            onChange={(event) => updateFilters({ urgency: event.target.value })}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "microbiology.worklist.filter.allUrgencies",
              })}
            />
            {urgencyLevels.map((urgency) => (
              <SelectItem
                key={urgency}
                value={urgency}
                text={formatMicrobiologyEnum(urgency)}
              />
            ))}
          </Select>
          <Select
            id="microbiology-worklist-due-filter"
            labelText={intl.formatMessage({
              id: "microbiology.worklist.filter.dueAction",
            })}
            value={filters.due}
            onChange={(event) => updateFilters({ due: event.target.value })}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "microbiology.worklist.filter.allActions",
              })}
            />
            {dueActions.map((dueAction) => (
              <SelectItem
                key={dueAction}
                value={dueAction}
                text={formatMicrobiologyEnum(dueAction)}
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
          <Button
            kind="ghost"
            size="sm"
            disabled={!hasFilters}
            onClick={() => history.push(getMicrobiologyWorklistUrl())}
          >
            {intl.formatMessage({ id: "microbiology.worklist.clearFilters" })}
          </Button>
        </section>
        {visibleRows.length === 0 && (
          <p>{intl.formatMessage({ id: "microbiology.worklist.empty" })}</p>
        )}
        {visibleRows.map((row) => (
          <Tile
            key={row.caseId}
            data-testid={`microbiology-worklist-row-${row.caseId}`}
          >
            <Stack gap={3}>
              <div>
                <strong>{row.sampleItemId}</strong>
                <div>{formatMicrobiologyEnum(row.workflowType)}</div>
              </div>
              <div>
                <Tag type={row.urgency === "HIGH" ? "red" : "gray"}>
                  {formatMicrobiologyEnum(row.urgency)}
                </Tag>
                <Tag type={row.needsAstReview ? "purple" : "blue"}>
                  {formatMicrobiologyEnum(row.dueAction)}
                </Tag>
                {row.hasOpenCriticalCommunication && (
                  <Tag type="magenta">
                    {intl.formatMessage({
                      id: "microbiology.worklist.critical",
                    })}
                  </Tag>
                )}
              </div>
              {row.siblingWorkflows.length > 0 && (
                <p data-testid="microbiology-worklist-siblings">
                  {intl.formatMessage({
                    id: "microbiology.worklist.siblings",
                  })}
                  :{" "}
                  {row.siblingWorkflows.map(formatMicrobiologyEnum).join(", ")}
                </p>
              )}
              <Button
                kind="secondary"
                size="sm"
                onClick={() =>
                  history.push(getMicrobiologyCaseUrl(row.caseId, filters))
                }
              >
                {intl.formatMessage({ id: "microbiology.worklist.openCase" })}
              </Button>
            </Stack>
          </Tile>
        ))}
      </Stack>
    </main>
  );
};

export default MicrobiologyWorklist;

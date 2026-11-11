import { format } from "date-fns";
import { IntlShape } from "react-intl";

const SOURCE_KEYS: Record<string, string> = {
  ANALYZER: "run.source.analyzer",
  WORKPLAN: "run.source.workplan",
  RESULT_ENTRY: "run.source.resultEntry",
};

const STATE_KEYS: Record<string, string> = {
  OPEN: "run.state.open",
  HELD: "run.state.held",
  COMPLETE: "run.state.complete",
  SUPERSEDED: "run.state.superseded",
};

export const runSourceKey = (source: string): string =>
  SOURCE_KEYS[source] ?? "run.source.analyzer";

export const runStateKey = (state: string): string =>
  STATE_KEYS[state] ?? "run.state.open";

/** A run's times arrive as ISO instants; the date follows the site date format. */
export const formatRunTime = (
  intl: IntlShape,
  iso: string,
  datePattern: string,
): string => {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return iso;
  }
  return `${format(date, datePattern)} ${intl.formatTime(date)}`;
};

/** Where leaving a run returns to: the worklist as it was before. */
export interface RunLocationState {
  returnTo?: string;
  filtersReplaced?: boolean;
}

/** The worklist filters a run opened from it replaces (FR-E1b). */
export const hadWorklistFilters = (search: string): boolean => {
  const params = new URLSearchParams(search);
  return [
    "testSectionId",
    "accessionNumber",
    "collectionDate",
    "status",
    "patientId",
  ].some((key) => Boolean(params.get(key)));
};

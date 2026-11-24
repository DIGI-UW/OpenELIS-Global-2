import { format } from "date-fns";
import { IntlShape } from "react-intl";
import { labWallClock } from "../../../utils/labClock";

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

/**
 * A run time as the rest of the app shows a stored time (History, sign-offs):
 * the site's date format and a 24-hour time, on the lab's clock whatever the
 * browser's zone.
 */
export const formatRunTime = (
  _intl: IntlShape,
  iso: string,
  datePattern: string,
): string => {
  if (Number.isNaN(new Date(iso).getTime())) {
    return iso;
  }
  return format(labWallClock(iso), `${datePattern} HH:mm`);
};

export interface RunControlDescription {
  level?: string | null;
  lotNumber?: string | null;
  observed?: string | null;
  expected?: string | null;
  verdictSource?: string | null;
  operator?: string | null;
  recordedAt?: string | null;
}

const OUTCOME_LABELS: Record<string, string> = {
  PASS: "label.qc.pass",
  FAIL: "label.qc.fail",
  VALID: "label.results.control.capture.valid",
  INVALID: "label.results.control.capture.invalid",
};

/** The Carbon tag colour of a run, test or control verdict. */
export type RunVerdictTagType = "red" | "purple" | "green" | "gray";

export const runVerdictTagType = (
  verdict?: string | null,
): RunVerdictTagType => {
  switch (verdict) {
    case "FAIL":
    case "HELD":
      return "red";
    case "DECIDED":
      return "purple";
    case "PASS":
      return "green";
    default:
      return "gray";
  }
};

const observedText = (intl: IntlShape, observed?: string | null) =>
  observed && OUTCOME_LABELS[observed]
    ? intl.formatMessage({ id: OUTCOME_LABELS[observed] })
    : observed;

/**
 * One control in words, the same wherever a run's control is shown: level,
 * lot, observed against expected, where the verdict came from, who recorded it
 * and when.
 */
export const describeControl = (
  intl: IntlShape,
  control: RunControlDescription,
  datePattern: string,
): string => {
  const observed = observedText(intl, control.observed);
  return [
    control.level,
    control.lotNumber,
    observed &&
      (control.expected
        ? intl.formatMessage(
            { id: "run.qc.observedAgainst" },
            { observed, expected: control.expected },
          )
        : observed),
    control.verdictSource &&
      intl.formatMessage({ id: `run.qc.source.${control.verdictSource}` }),
    control.operator,
    control.recordedAt && formatRunTime(intl, control.recordedAt, datePattern),
  ]
    .filter(Boolean)
    .join(" · ");
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

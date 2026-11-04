import React, { useEffect, useState } from "react";
import { Button, Tag } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../../utils/Utils";
import { formatRunTime, runSourceKey, runStateKey } from "./runFormat";

export interface RunSummary {
  id: number;
  label: string;
  source: string;
  state: string;
  analyzerName?: string | null;
  openedAt?: string | null;
  dispositioned: number;
  total: number;
}

interface RunList {
  open: number;
  held: number;
  runs: RunSummary[];
}

interface RunsChipProps {
  testSectionId?: string;
  activeRunId?: number;
  onOpenRun: (runId: number) => void;
}

/**
 * OGC-1200 FR-D1: how many runs wait for review and how many are held, for the
 * selected lab unit and only the runs the user may see. Expanding it names the
 * runs; choosing one opens it.
 */
const RunsChip: React.FC<RunsChipProps> = ({
  testSectionId,
  activeRunId,
  onOpenRun,
}) => {
  const intl = useIntl();
  const [list, setList] = useState<RunList | null>(null);
  const [failed, setFailed] = useState(false);
  const [expanded, setExpanded] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    const query = testSectionId
      ? `?testSectionId=${encodeURIComponent(testSectionId)}`
      : "";
    getFromOpenElisServer(
      `/rest/result-runs${query}`,
      (response?: RunList) => {
        setFailed(!response);
        setList(response ?? null);
      },
      controller.signal,
    );
    return () => controller.abort();
  }, [testSectionId]);

  const counts = intl.formatMessage(
    { id: "run.chip.counts" },
    { open: list?.open ?? 0, held: list?.held ?? 0 },
  );

  return (
    <div className="runsChip" data-testid="runs-chip">
      <Tag
        type={expanded || activeRunId ? "blue" : "gray"}
        className="unifiedResultsChip"
        onClick={() => setExpanded((current) => !current)}
        aria-expanded={expanded}
      >
        <FormattedMessage id="run.chip.label" />: {counts}
      </Tag>
      {expanded && (
        <div className="runsChipList" data-testid="runs-chip-list">
          {failed && (
            <p className="runsChipNote">
              <FormattedMessage id="run.chip.loadFailed" />
            </p>
          )}
          {!failed && list && list.runs.length === 0 && (
            <p className="runsChipNote">
              <FormattedMessage id="run.chip.empty" />
            </p>
          )}
          {list?.runs.map((run) => (
            <div key={run.id} className="runsChipRow">
              <Button
                kind="ghost"
                size="sm"
                data-testid={`open-run-${run.id}`}
                disabled={run.id === activeRunId}
                onClick={() => {
                  setExpanded(false);
                  onOpenRun(run.id);
                }}
              >
                {run.label}
              </Button>
              <span>
                {intl.formatMessage({ id: runSourceKey(run.source) })}
                {run.analyzerName ? ` · ${run.analyzerName}` : ""}
                {run.openedAt ? ` · ${formatRunTime(intl, run.openedAt)}` : ""}
                {" · "}
                <FormattedMessage
                  id="run.progress"
                  values={{ done: run.dispositioned, total: run.total }}
                />
              </span>
              <Tag size="sm" type={run.state === "HELD" ? "red" : "cyan"}>
                {intl.formatMessage({ id: runStateKey(run.state) })}
              </Tag>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default RunsChip;

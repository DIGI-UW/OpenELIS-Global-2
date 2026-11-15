import React from "react";
import { Tag } from "@carbon/react";
import { WarningFilled } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { formatRunTime, runSourceKey } from "./runFormat";

export interface WaitingRun {
  runId: number;
  label: string;
  source: string;
  openedAt?: string | null;
  held?: boolean;
}

interface WaitingTagProps {
  run: WaitingRun;
  onOpenRun?: (runId: number) => void;
}

/**
 * OGC-1200 FR-D3: a pending test whose analyzer value waits in an open run.
 * The value is not shown here and the row cannot be entered; the tag opens
 * the run, where the value is reviewed. When a control for the test failed in
 * that run the row is held, and the tag says so in red (FR-D4).
 */
const WaitingTag: React.FC<WaitingTagProps> = ({ run, onOpenRun }) => {
  const intl = useIntl();
  const open = () => {
    if (onOpenRun) {
      onOpenRun(run.runId);
    } else {
      window.location.assign(`/Results?run=${run.runId}`);
    }
  };
  const detail = [
    intl.formatMessage({ id: runSourceKey(run.source) }),
    run.openedAt ? formatRunTime(intl, run.openedAt) : null,
  ]
    .filter(Boolean)
    .join(" · ");
  return (
    <>
      <Tag
        type={run.held ? "red" : "blue"}
        size="sm"
        className="unifiedResultsChip"
        renderIcon={run.held ? WarningFilled : undefined}
        data-testid={`${run.held ? "held" : "waiting"}-run-${run.runId}`}
        onClick={open}
      >
        {intl.formatMessage(
          { id: run.held ? "run.tag.heldQcFailed" : "run.tag.waiting" },
          { runId: run.label },
        )}
      </Tag>
      <span className="runWaitingDetail">{detail}</span>
    </>
  );
};

export default WaitingTag;

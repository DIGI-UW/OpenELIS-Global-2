import React from "react";
import { Tag } from "@carbon/react";
import { useIntl } from "react-intl";
import useSiteDateFormat from "../../../common/useSiteDateFormat";
import { formatRunTime, runSourceKey } from "./runFormat";

export interface WaitingRun {
  runId: number;
  label: string;
  source: string;
  openedAt?: string | null;
}

interface WaitingTagProps {
  run: WaitingRun;
  onOpenRun?: (runId: number) => void;
}

/**
 * OGC-1200 FR-D3: a pending test whose analyzer value waits in an open run.
 * The value is not shown here and the row cannot be entered; the tag opens
 * the run, where the value is reviewed.
 */
const WaitingTag: React.FC<WaitingTagProps> = ({ run, onOpenRun }) => {
  const intl = useIntl();
  const { pattern } = useSiteDateFormat();
  const open = () => {
    if (onOpenRun) {
      onOpenRun(run.runId);
    } else {
      window.location.assign(`/Results?run=${run.runId}`);
    }
  };
  const detail = [
    intl.formatMessage({ id: runSourceKey(run.source) }),
    run.openedAt ? formatRunTime(intl, run.openedAt, pattern) : null,
  ]
    .filter(Boolean)
    .join(" · ");
  return (
    <>
      <Tag
        type="blue"
        size="sm"
        className="unifiedResultsChip"
        data-testid={`waiting-run-${run.runId}`}
        onClick={open}
      >
        {intl.formatMessage({ id: "run.tag.waiting" }, { runId: run.label })}
      </Tag>
      <span className="runWaitingDetail">{detail}</span>
    </>
  );
};

export default WaitingTag;

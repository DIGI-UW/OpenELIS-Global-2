import React, { useState } from "react";
import { Button, DismissibleTag, InlineNotification } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { postToOpenElisServerJsonResponse } from "../../../utils/Utils";
import { requestFailed, serverMessage } from "../../../utils/requestOutcome";

export interface BatchPick {
  testId: string;
  /** The row as the technician knows it: lab number and test. */
  label: string;
}

interface BatchTheseBarProps {
  /** The picked pending rows, by analysis id. */
  picks: Record<string, BatchPick>;
  onRemove: (analysisId: string) => void;
  onClear: () => void;
  onOpenRun: (runId: number) => void;
}

/**
 * OGC-1200 FR-B4 "Batch these": two or more pending rows of one test, picked on
 * the worklist, become a workplan batch released at once as a run, and the run
 * opens with its control and lot ready to enter. Rows of different tests are
 * not batched together; the button says why it is off. Picks outlive a search,
 * so each is listed here and can be removed on its own.
 */
const BatchTheseBar: React.FC<BatchTheseBarProps> = ({
  picks,
  onRemove,
  onClear,
  onOpenRun,
}) => {
  const intl = useIntl();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const analysisIds = Object.keys(picks);
  if (!analysisIds.length) {
    return null;
  }
  const mixed =
    new Set(Object.values(picks).map((pick) => pick.testId)).size > 1;
  const enough = analysisIds.length >= 2;

  const batch = () => {
    setBusy(true);
    setError(null);
    postToOpenElisServerJsonResponse(
      "/rest/batch-workplans/batches/from-worklist",
      JSON.stringify({ analysisIds }),
      (response?: { runId?: number; message?: string; status?: unknown }) => {
        setBusy(false);
        if (requestFailed(response) || !response?.runId) {
          setError(
            serverMessage(response) ||
              intl.formatMessage({ id: "run.batch.failed" }),
          );
          return;
        }
        onClear();
        onOpenRun(response.runId);
      },
    );
  };

  return (
    <div className="runBatchBar" data-testid="batch-these-bar">
      <span data-testid="batch-these-count">
        <FormattedMessage
          id="run.batch.selected"
          values={{ count: analysisIds.length }}
        />
      </span>
      <Button
        size="sm"
        kind="primary"
        disabled={busy || mixed || !enough}
        onClick={batch}
        data-testid="batch-these"
      >
        <FormattedMessage id="run.action.batchThese" />
      </Button>
      <Button size="sm" kind="ghost" onClick={onClear}>
        <FormattedMessage id="run.batch.clear" />
      </Button>
      {(mixed || !enough) && (
        <span className="runsChipNote" data-testid="batch-these-hint">
          <FormattedMessage
            id={mixed ? "run.batch.mixedTests" : "run.batch.needTwo"}
          />
        </span>
      )}
      <div className="runBatchPicks" data-testid="batch-these-picks">
        {analysisIds.map((analysisId) => {
          const remove = intl.formatMessage(
            { id: "run.batch.remove" },
            { row: picks[analysisId].label },
          );
          return (
            <DismissibleTag
              key={analysisId}
              type="blue"
              size="sm"
              text={picks[analysisId].label}
              title={remove}
              dismissTooltipLabel={remove}
              onClose={() => onRemove(analysisId)}
            />
          );
        })}
      </div>
      {error && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={error}
          data-testid="batch-these-error"
        />
      )}
    </div>
  );
};

export default BatchTheseBar;

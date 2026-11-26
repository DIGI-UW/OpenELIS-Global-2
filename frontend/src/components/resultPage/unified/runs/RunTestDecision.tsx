import React, { useState } from "react";
import {
  Button,
  Checkbox,
  InlineNotification,
  Tag,
  TextArea,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { postToOpenElisServerJsonResponse } from "../../../utils/Utils";
import { RequiredMarker, requiredProps } from "../../../common/RequiredMarker";
import type { RunQc, RunQcTestLine } from "./RunQcBlock";
import { missingQcItems } from "./runFormat";

type Action = "RETEST" | "REJECT" | "ACCEPT_DESPITE";

interface RunTestDecisionProps {
  runId: number;
  test: RunQcTestLine;
  onDecided: (qc: RunQc) => void;
}

/**
 * OGC-1200 FR-C5: what is done with a test a failed control holds in a run.
 * Retest and Reject take its values out of review (Reject may file a
 * non-conformity); Accept despite the QC failure needs a reason and always
 * files a non-conformity. Once decided, the line says what was decided.
 */
const RunTestDecision: React.FC<RunTestDecisionProps> = ({
  runId,
  test,
  onDecided,
}) => {
  const intl = useIntl();
  const [action, setAction] = useState<Action | null>(null);
  const [reason, setReason] = useState("");
  const [fileNce, setFileNce] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (test.decision) {
    return (
      <span
        className="runQcDecision"
        data-testid={`run-qc-decision-${test.testId}`}
      >
        {" "}
        <Tag type="purple" size="sm">
          {intl.formatMessage({ id: `run.decision.${test.decision}` })}
        </Tag>
        {test.nceNumber && (
          <span>
            {" "}
            {intl.formatMessage(
              { id: "run.decision.nce" },
              { number: test.nceNumber },
            )}
          </span>
        )}
      </span>
    );
  }
  if (!test.holdCause) {
    return null;
  }
  const missing = test.holdCause === "QC_MISSING";

  const confirm = () => {
    if (action === "ACCEPT_DESPITE" && !reason.trim()) {
      setError(
        intl.formatMessage(
          {
            id: missing
              ? "run.decision.reasonRequiredMissingQc"
              : "run.decision.reasonRequired",
          },
          { items: missingQcItems(intl, test.missing) },
        ),
      );
      return;
    }
    setBusy(true);
    setError(null);
    postToOpenElisServerJsonResponse(
      `/rest/result-runs/${runId}/tests/${test.testId}/decision`,
      JSON.stringify({
        action,
        reason: reason.trim() || null,
        fileNce: action === "ACCEPT_DESPITE" || fileNce,
      }),
      (response?: RunQc & { message?: string }) => {
        setBusy(false);
        if (!response || !Array.isArray(response.tests)) {
          setError(
            response?.message ||
              intl.formatMessage({ id: "run.decision.failed" }),
          );
          return;
        }
        setAction(null);
        onDecided(response);
      },
    );
  };

  return (
    <div className="runQcDecide" data-testid={`run-qc-decide-${test.testId}`}>
      <p className="runQcHeldNote">
        <FormattedMessage id="run.qc.testHeld" />
      </p>
      {!action && (
        <div className="runQcDecideActions">
          <Button kind="tertiary" size="sm" onClick={() => setAction("RETEST")}>
            <FormattedMessage id="run.decision.RETEST" />
          </Button>
          <Button kind="tertiary" size="sm" onClick={() => setAction("REJECT")}>
            <FormattedMessage id="run.decision.REJECT" />
          </Button>
          <Button
            kind="danger--tertiary"
            size="sm"
            onClick={() => setAction("ACCEPT_DESPITE")}
          >
            <FormattedMessage id="run.decision.ACCEPT_DESPITE" />
          </Button>
        </div>
      )}
      {action && (
        <div className="runQcDecideForm">
          <p>
            {intl.formatMessage(
              {
                id:
                  action !== "ACCEPT_DESPITE"
                    ? `run.decision.explain.${action}`
                    : missing
                      ? "run.decision.explainMissingQc.ACCEPT_DESPITE"
                      : "run.decision.explainQcFailed.ACCEPT_DESPITE",
              },
              {
                test: test.testName,
                items: missingQcItems(intl, test.missing),
              },
            )}
          </p>
          {action === "REJECT" && (
            <Checkbox
              id={`run-${runId}-reject-nce-${test.testId}`}
              labelText={intl.formatMessage({ id: "run.decision.fileNce" })}
              checked={fileNce}
              onChange={(_, { checked }) => setFileNce(checked)}
            />
          )}
          {action === "ACCEPT_DESPITE" && (
            <TextArea
              id={`run-${runId}-accept-reason-${test.testId}`}
              labelText={
                <>
                  {intl.formatMessage({ id: "run.decision.reason" })}
                  <RequiredMarker />
                </>
              }
              {...requiredProps()}
              value={reason}
              onChange={(event) => setReason(event.target.value)}
              rows={2}
            />
          )}
          {error && (
            <InlineNotification
              kind="error"
              lowContrast
              hideCloseButton
              title={error}
            />
          )}
          <div className="runQcFormActions">
            <Button
              size="sm"
              kind={action === "ACCEPT_DESPITE" ? "danger" : "primary"}
              disabled={busy}
              onClick={confirm}
              data-testid={`run-qc-decide-confirm-${test.testId}`}
            >
              <FormattedMessage id={`run.decision.${action}`} />
            </Button>
            <Button
              size="sm"
              kind="ghost"
              onClick={() => {
                setAction(null);
                setError(null);
              }}
            >
              <FormattedMessage id="label.button.cancel" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
};

export default RunTestDecision;

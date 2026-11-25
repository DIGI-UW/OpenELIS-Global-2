import React, { useState } from "react";
import { Button, InlineNotification, Link } from "@carbon/react";
import { useIntl } from "react-intl";
import { postToOpenElisServerJsonResponse } from "../../../utils/Utils";
import type { RunLinkItem, RunQc, RunQcTestLine } from "./RunQcBlock";

interface RunRetestLinkProps {
  runId: number;
  test: RunQcTestLine;
  onLinked: (qc: RunQc) => void;
  onOpenRun?: (runId: number) => void;
}

/**
 * OGC-1200 FR-C5 and FR-A3: the runs a test line points to. A later run that
 * covers samples an earlier run set aside for retest suggests "retest of
 * RUN-x" until a user confirms it; the confirmed link reads "Retest of RUN-x"
 * here and "Superseded by RUN-y" on the earlier run.
 */
const RunRetestLink: React.FC<RunRetestLinkProps> = ({
  runId,
  test,
  onLinked,
  onOpenRun,
}) => {
  const intl = useIntl();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const retestOf = test.retestOf ?? [];
  const suggestions = test.retestSuggestions ?? [];
  const supersededBy = test.supersededBy ?? [];
  if (
    retestOf.length === 0 &&
    suggestions.length === 0 &&
    supersededBy.length === 0
  ) {
    return null;
  }

  const runName = (link: RunLinkItem, messageId: string) => {
    const text = intl.formatMessage({ id: messageId }, { run: link.label });
    if (!onOpenRun) {
      return <span>{text}</span>;
    }
    return (
      <Link
        href="#"
        onClick={(event: React.MouseEvent) => {
          event.preventDefault();
          onOpenRun(link.runId);
        }}
      >
        {text}
      </Link>
    );
  };

  const samples = (link: RunLinkItem) =>
    intl.formatMessage(
      { id: "run.link.samples" },
      { count: link.count, total: link.total },
    );

  const confirm = (link: RunLinkItem) => {
    setBusy(true);
    setError(null);
    postToOpenElisServerJsonResponse(
      `/rest/result-runs/${runId}/tests/${test.testId}/retest-of`,
      JSON.stringify({ runId: link.runId }),
      (response?: RunQc & { message?: string }) => {
        setBusy(false);
        if (!response || !Array.isArray(response.tests)) {
          setError(
            response?.message || intl.formatMessage({ id: "run.link.failed" }),
          );
          return;
        }
        onLinked(response);
      },
    );
  };

  return (
    <div className="runQcLinks" data-testid={`run-qc-links-${test.testId}`}>
      {retestOf.map((link) => (
        <p
          key={`retest-of-${link.runId}`}
          data-testid={`run-qc-retest-of-${test.testId}-${link.runId}`}
        >
          {runName(link, "run.link.retestOf")} {samples(link)}
        </p>
      ))}
      {supersededBy.map((link) => (
        <p
          key={`superseded-by-${link.runId}`}
          data-testid={`run-qc-superseded-by-${test.testId}-${link.runId}`}
        >
          {runName(link, "run.link.supersededBy")} {samples(link)}
        </p>
      ))}
      {suggestions.map((link) => (
        <p
          key={`suggested-${link.runId}`}
          data-testid={`run-qc-retest-suggestion-${test.testId}-${link.runId}`}
        >
          {intl.formatMessage(
            { id: "run.link.suggested" },
            { run: link.label, count: link.count, total: link.total },
          )}{" "}
          <Button
            kind="tertiary"
            size="sm"
            disabled={busy}
            onClick={() => confirm(link)}
          >
            {intl.formatMessage(
              { id: "run.link.confirm" },
              { run: link.label },
            )}
          </Button>
        </p>
      ))}
      {error && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={error}
        />
      )}
    </div>
  );
};

export default RunRetestLink;

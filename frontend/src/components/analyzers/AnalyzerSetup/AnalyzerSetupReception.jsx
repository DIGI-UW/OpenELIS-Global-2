import React, { useCallback, useEffect, useState } from "react";
import { Button, InlineNotification } from "@carbon/react";
import { useIntl } from "react-intl";
import { getAnalyzerMapping } from "../../../services/analyzerService";

const AnalyzerSetupReception = ({
  candidate,
  busy,
  onStart,
  onStop,
  blockers = [],
}) => {
  const intl = useIntl();
  const [count, setCount] = useState(null);
  const [loading, setLoading] = useState(false);
  const [failed, setFailed] = useState(false);
  const receiving =
    candidate.status === "VALIDATION" &&
    candidate.connection?.actualRuntimeState === "ACTIVE" &&
    candidate.connection?.receptionPurpose === "SETUP";
  const refresh = useCallback(
    (signal) => {
      setLoading(true);
      getAnalyzerMapping(
        candidate.id,
        (response) => {
          if (signal?.aborted) return;
          const valid = Array.isArray(response?.tests);
          setFailed(!valid);
          setCount(
            valid
              ? response.tests.reduce(
                  (total, row) => total + (row.receivedValues || []).length,
                  0,
                )
              : null,
          );
          setLoading(false);
        },
        signal,
      );
    },
    [candidate.id],
  );
  useEffect(() => {
    const controller = new AbortController();
    if (receiving) refresh(controller.signal);
    return () => controller.abort();
  }, [receiving, refresh]);
  return (
    <section data-testid="analyzer-setup-reception">
      <h4>{intl.formatMessage({ id: "analyzer.setup.reception.title" })}</h4>
      <InlineNotification
        kind="info"
        lowContrast
        hideCloseButton
        title={intl.formatMessage({
          id: receiving
            ? "analyzer.setup.reception.receiving"
            : "analyzer.setup.reception.help",
        })}
      />
      {blockers.map((blocker, index) => (
        <InlineNotification
          key={`${blocker.code}-${index}`}
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage(
            {
              id: Object.prototype.hasOwnProperty.call(
                intl.messages,
                blocker.code,
              )
                ? blocker.code
                : "analyzer.setup.connect.activation.blockerUnknown",
            },
            blocker.args,
          )}
        />
      ))}
      <Button
        type="button"
        kind="tertiary"
        disabled={busy}
        onClick={receiving ? onStop : onStart}
      >
        {intl.formatMessage({
          id: receiving
            ? "analyzer.setup.reception.stop"
            : "analyzer.setup.reception.start",
        })}
      </Button>
      <Button
        type="button"
        kind="ghost"
        disabled={busy || loading}
        onClick={() => refresh()}
      >
        {intl.formatMessage({ id: "analyzer.setup.reception.refresh" })}
      </Button>
      {count !== null && (
        <p role="status">
          {intl.formatMessage(
            { id: "analyzer.setup.reception.count" },
            { count },
          )}
        </p>
      )}
      {failed && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "analyzer.setup.reception.loadError",
          })}
        />
      )}
    </section>
  );
};

export default AnalyzerSetupReception;

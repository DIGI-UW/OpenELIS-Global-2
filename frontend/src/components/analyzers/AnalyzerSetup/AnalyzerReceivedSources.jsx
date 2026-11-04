import React, { useCallback, useEffect, useState } from "react";
import { Button, InlineNotification } from "@carbon/react";
import { useIntl } from "react-intl";
import { getAnalyzerDeliveryIssues } from "../../../services/analyzerService";

/** Only unresolved network identities; already interpreted deliveries keep their binding. */
export const unassignedSources = (rows, protocol) => {
  const sources = new Map();
  rows
    .filter(
      (row) =>
        row.state === "DMQ" &&
        !row.connectionId &&
        row.sender &&
        row.sourceId &&
        Number.isInteger(row.listenerPort) &&
        ["TCP", "MLLP"].includes(row.transport) &&
        row.protocol === protocol &&
        ["UNREGISTERED_SOURCE", "AMBIGUOUS_SOURCE"].includes(row.failureReason),
    )
    .forEach((row) => {
      const key = JSON.stringify([row.sender, row.sourceId, row.listenerPort]);
      if (!sources.has(key)) sources.set(key, row);
    });
  return [...sources.values()];
};

export default function AnalyzerReceivedSources({ protocol, busy, onAdopt }) {
  const intl = useIntl();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(false);
  const [failed, setFailed] = useState(false);
  const refresh = useCallback(
    (signal) => {
      setLoading(true);
      getAnalyzerDeliveryIssues((response) => {
        if (signal?.aborted) return;
        const valid =
          response?.status === "success" && Array.isArray(response.data?.rows);
        setFailed(!valid);
        setRows(valid ? unassignedSources(response.data.rows, protocol) : []);
        setLoading(false);
      }, signal);
    },
    [protocol],
  );
  useEffect(() => {
    const controller = new AbortController();
    refresh(controller.signal);
    return () => controller.abort();
  }, [refresh]);
  return (
    <section data-testid="analyzer-received-sources">
      <h4>{intl.formatMessage({ id: "analyzer.setup.sources.title" })}</h4>
      <p>{intl.formatMessage({ id: "analyzer.setup.sources.help" })}</p>
      <Button
        type="button"
        kind="tertiary"
        disabled={busy || loading}
        onClick={() => refresh()}
      >
        {intl.formatMessage({ id: "analyzer.setup.sources.refresh" })}
      </Button>
      {failed && (
        <InlineNotification
          kind="error"
          hideCloseButton
          title={intl.formatMessage({ id: "analyzer.setup.sources.loadError" })}
        />
      )}
      {!loading && !failed && rows.length === 0 && (
        <p>{intl.formatMessage({ id: "analyzer.setup.sources.empty" })}</p>
      )}
      {rows.map((row) => (
        <div key={row.id} data-testid={`analyzer-received-source-${row.id}`}>
          <p>
            {intl.formatMessage(
              { id: "analyzer.setup.sources.identity" },
              {
                sender: row.sender,
                address: row.sourceId,
                port: String(row.listenerPort),
              },
            )}
          </p>
          <Button
            type="button"
            kind="secondary"
            disabled={busy}
            onClick={() => onAdopt(row)}
          >
            {intl.formatMessage({ id: "analyzer.setup.sources.adopt" })}
          </Button>
        </div>
      ))}
    </section>
  );
}

import React, { useState } from "react";
import { Button, Select, SelectItem, TextInput } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { postToOpenElisServerJsonResponse } from "../../utils/Utils";

/**
 * OGC-1026 (R7, D13) — the Sample status block inside the Storage & sample
 * disposal section: record partial use against the item's remaining quantity,
 * mark it used up (exhausted = remaining 0, not a status), and hand off to the
 * shipped disposal workflow once exhausted. Mutations go through
 * /rest/storage/sample-items/record-usage and /dispose; the parent re-fetches
 * the quantity snapshot after each change.
 */
export interface QuantitySnapshot {
  quantity?: number | string | null;
  remainingQuantity?: number | string | null;
  unitOfMeasure?: string | null;
  disposed?: boolean;
}

interface SampleStatusBlockProps {
  sampleItemId: string;
  snapshot: QuantitySnapshot;
  editable: boolean;
  onChanged: () => void;
}

const DISPOSAL_REASONS = [
  "expired",
  "contaminated",
  "patient_request",
  "testing_complete",
  "other",
];
const DISPOSAL_METHODS = [
  "autoclave",
  "neutralization",
  "incineration",
  "other",
];

const asNumber = (value?: number | string | null): number | null => {
  if (value === null || value === undefined || value === "") {
    return null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
};

const SampleStatusBlock: React.FC<SampleStatusBlockProps> = ({
  sampleItemId,
  snapshot,
  editable,
  onChanged,
}) => {
  const intl = useIntl();
  const [recording, setRecording] = useState(false);
  const [amount, setAmount] = useState("");
  const [disposing, setDisposing] = useState(false);
  const [reason, setReason] = useState("");
  const [method, setMethod] = useState("");
  const [notes, setNotes] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const total = asNumber(snapshot.quantity);
  const remaining = asNumber(snapshot.remainingQuantity);
  const unit = snapshot.unitOfMeasure || "";
  const disposed = Boolean(snapshot.disposed);
  const exhausted = remaining !== null && remaining <= 0;

  const statusKey = disposed
    ? "label.results.sampleStatus.disposed"
    : exhausted
      ? "label.results.sampleStatus.usedUp"
      : "label.results.sampleStatus.available";

  const post = (url: string, body: Record<string, unknown>) => {
    setBusy(true);
    setError("");
    postToOpenElisServerJsonResponse(
      url,
      JSON.stringify(body),
      (response?: { message?: string; status?: number }) => {
        setBusy(false);
        if (response && response.status && response.status >= 400) {
          setError(response.message || "");
          return;
        }
        setRecording(false);
        setDisposing(false);
        setAmount("");
        onChanged();
      },
    );
  };

  const recordUsage = () =>
    post("/rest/storage/sample-items/record-usage", {
      sampleItemId,
      amountUsed: amount,
    });
  const markUsedUp = () =>
    post("/rest/storage/sample-items/record-usage", {
      sampleItemId,
      markUsedUp: true,
    });
  const startDisposal = () =>
    post("/rest/storage/sample-items/dispose", {
      sampleItemId,
      reason,
      method,
      notes,
    });

  return (
    <div className="unifiedSampleStatus" data-testid="sample-status-block">
      <div className="cds--label">
        <FormattedMessage id="label.results.sampleStatus.title" />
      </div>
      <div className="unifiedSampleStatusLine">
        <span>
          {total !== null || remaining !== null ? (
            <FormattedMessage
              id="label.results.sampleStatus.volume"
              values={{
                0: (
                  <strong>
                    {remaining !== null ? remaining : "—"} {unit}
                  </strong>
                ),
                1: total !== null ? `${total} ${unit}` : "—",
                2: (
                  <strong>
                    <FormattedMessage id={statusKey} />
                  </strong>
                ),
              }}
            />
          ) : (
            <FormattedMessage
              id="label.results.sampleStatus.noVolume"
              values={{
                0: (
                  <strong>
                    <FormattedMessage id={statusKey} />
                  </strong>
                ),
              }}
            />
          )}
        </span>
        {editable && !disposed && !exhausted && !recording && (
          <Button
            kind="secondary"
            size="sm"
            disabled={busy || remaining === null}
            onClick={() => setRecording(true)}
            data-testid="record-usage-open"
          >
            <FormattedMessage id="label.results.sampleStatus.recordUse" />
          </Button>
        )}
        {editable && !disposed && !exhausted && (
          <Button
            kind="secondary"
            size="sm"
            disabled={busy}
            onClick={markUsedUp}
            data-testid="mark-used-up"
          >
            <FormattedMessage id="label.results.sampleStatus.markUsedUp" />
          </Button>
        )}
        {editable && !disposed && exhausted && !disposing && (
          <Button
            kind="danger"
            size="sm"
            disabled={busy}
            onClick={() => setDisposing(true)}
            data-testid="start-disposal"
          >
            <FormattedMessage id="label.results.sampleStatus.startDisposal" />
          </Button>
        )}
      </div>
      {recording && (
        <div className="unifiedSampleStatusLine">
          <TextInput
            id={`usage-amount-${sampleItemId}`}
            labelText={intl.formatMessage({
              id: "label.results.sampleStatus.amountUsed",
            })}
            size="sm"
            className="unifiedMiniNumber"
            value={amount}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
              setAmount(e.target.value)
            }
          />
          {unit && <span className="unifiedHistoryFootnote">{unit}</span>}
          <Button
            kind="primary"
            size="sm"
            disabled={busy || !amount}
            onClick={recordUsage}
            data-testid="record-usage-apply"
          >
            <FormattedMessage id="label.results.sampleStatus.record" />
          </Button>
          <Button
            kind="ghost"
            size="sm"
            disabled={busy}
            onClick={() => setRecording(false)}
          >
            <FormattedMessage id="label.button.cancel" />
          </Button>
          <span className="unifiedHistoryFootnote">
            <FormattedMessage id="label.results.sampleStatus.recordHint" />
          </span>
        </div>
      )}
      {exhausted && !disposed && (
        <div className="unifiedSampleStatusNote">
          <FormattedMessage id="label.results.sampleStatus.usedUpNote" />
        </div>
      )}
      {disposing && !disposed && (
        <div className="unifiedSampleStatusLine">
          <Select
            id={`disposal-reason-${sampleItemId}`}
            labelText={intl.formatMessage({
              id: "label.results.sampleStatus.disposalReason",
            })}
            size="sm"
            value={reason}
            onChange={(e: React.ChangeEvent<HTMLSelectElement>) =>
              setReason(e.target.value)
            }
          >
            <SelectItem value="" text="" />
            {DISPOSAL_REASONS.map((id) => (
              <SelectItem
                key={id}
                value={id}
                text={intl.formatMessage({
                  id: `label.results.sampleStatus.reason.${id}`,
                })}
              />
            ))}
          </Select>
          <Select
            id={`disposal-method-${sampleItemId}`}
            labelText={intl.formatMessage({
              id: "label.results.sampleStatus.disposalMethod",
            })}
            size="sm"
            value={method}
            onChange={(e: React.ChangeEvent<HTMLSelectElement>) =>
              setMethod(e.target.value)
            }
          >
            <SelectItem value="" text="" />
            {DISPOSAL_METHODS.map((id) => (
              <SelectItem
                key={id}
                value={id}
                text={intl.formatMessage({
                  id: `label.results.sampleStatus.method.${id}`,
                })}
              />
            ))}
          </Select>
          <TextInput
            id={`disposal-notes-${sampleItemId}`}
            labelText={intl.formatMessage({
              id: "label.results.sampleStatus.disposalNotes",
            })}
            size="sm"
            value={notes}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
              setNotes(e.target.value)
            }
          />
          <Button
            kind="danger"
            size="sm"
            disabled={busy || !reason || !method}
            onClick={startDisposal}
            data-testid="confirm-disposal"
          >
            <FormattedMessage id="label.results.sampleStatus.confirmDisposal" />
          </Button>
          <Button
            kind="ghost"
            size="sm"
            disabled={busy}
            onClick={() => setDisposing(false)}
          >
            <FormattedMessage id="label.button.cancel" />
          </Button>
        </div>
      )}
      {error && <div className="unifiedSampleStatusError">{error}</div>}
      <div className="unifiedHistoryFootnote">
        <FormattedMessage id="label.results.sampleStatus.footnote" />
      </div>
    </div>
  );
};

export default SampleStatusBlock;

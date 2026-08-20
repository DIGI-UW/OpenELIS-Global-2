import React, { useEffect, useMemo, useState } from "react";
import {
  Button,
  Checkbox,
  InlineNotification,
  Select,
  SelectItem,
  TextArea,
  TextInput,
  Tag,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";
import MicrobiologyService from "./MicrobiologyService";

const TARGET_TYPES = ["CASE", "ISOLATE", "SAMPLE_ITEM", "RESULT"];
const METHOD_OPTIONS = ["PHONE", "SMS", "IN_PERSON", "EMAIL"];

const statusTagType = (status) => {
  if (status === "CLOSED") {
    return "green";
  }
  return status === "ACKNOWLEDGED" ? "blue" : "red";
};

const CriticalCommunicationPanel = ({
  caseId,
  sampleItemId,
  isolates = [],
  projectedResultIds = [],
  entryTargetType = "",
  entryTargetId = "",
  service = MicrobiologyService,
  onCaseUpdated,
  onEntryComplete,
}) => {
  const intl = useIntl();
  const [communications, setCommunications] = useState([]);
  const [targetType, setTargetType] = useState("CASE");
  const [targetId, setTargetId] = useState(caseId);
  const [recipient, setRecipient] = useState("");
  const [recipientContact, setRecipientContact] = useState("");
  const [communicationMethod, setCommunicationMethod] = useState("PHONE");
  const [message, setMessage] = useState("");
  const [followUpNeeded, setFollowUpNeeded] = useState(true);
  const [closingId, setClosingId] = useState("");
  const [resolutionNote, setResolutionNote] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const targetLocked = Boolean(entryTargetType && entryTargetId);

  const effectiveTargetType = targetLocked ? entryTargetType : targetType;
  const targetOptions = useMemo(() => {
    if (effectiveTargetType === "ISOLATE") {
      return isolates.map((isolate) => ({
        id: isolate.id,
        label: isolate.isolateLabel,
      }));
    }
    if (effectiveTargetType === "RESULT") {
      return projectedResultIds.map((id) => ({ id, label: id }));
    }
    return [];
  }, [effectiveTargetType, isolates, projectedResultIds]);
  const defaultTargetId =
    effectiveTargetType === "CASE"
      ? caseId
      : effectiveTargetType === "SAMPLE_ITEM"
        ? sampleItemId || ""
        : targetOptions[0]?.id || "";
  const effectiveTargetId = targetLocked
    ? entryTargetId
    : targetId || defaultTargetId;

  const loadCommunications = () => {
    service.getCriticalCommunications(caseId).then((rows) => {
      setCommunications(Array.isArray(rows) ? rows : []);
    });
  };

  useEffect(() => {
    loadCommunications();
  }, [caseId]);

  const refresh = () => {
    loadCommunications();
    if (onCaseUpdated) {
      onCaseUpdated();
    }
  };

  const logCommunication = () => {
    if (!recipient.trim() || !message.trim() || !effectiveTargetId) {
      setError(intl.formatMessage({ id: "microbiology.critical.required" }));
      return;
    }
    setSaving(true);
    setError("");
    service
      .logCriticalCommunication(caseId, {
        targetType: effectiveTargetType,
        targetId: effectiveTargetId,
        recipient,
        recipientContact,
        communicationMethod,
        message,
        followUpNeeded,
      })
      .then(() => {
        setRecipient("");
        setRecipientContact("");
        setMessage("");
        setFollowUpNeeded(true);
        refresh();
        if (onEntryComplete) {
          onEntryComplete();
        }
      })
      .finally(() => setSaving(false));
  };

  const acknowledge = (communicationId) => {
    setSaving(true);
    service
      .acknowledgeCriticalCommunication(communicationId)
      .then(refresh)
      .finally(() => setSaving(false));
  };

  const close = (communicationId) => {
    if (!resolutionNote.trim()) {
      setError(
        intl.formatMessage({ id: "microbiology.critical.resolutionRequired" }),
      );
      return;
    }
    setSaving(true);
    setError("");
    service
      .closeCriticalCommunication(communicationId, { resolutionNote })
      .then(() => {
        setClosingId("");
        setResolutionNote("");
        refresh();
      })
      .finally(() => setSaving(false));
  };

  return (
    <section
      className="microbiology-card"
      data-testid="microbiology-critical-card"
      aria-labelledby="microbiology-critical-heading"
    >
      <div className="microbiology-card__header">
        <div>
          <h3 id="microbiology-critical-heading">
            {intl.formatMessage({ id: "microbiology.critical.title" })}
          </h3>
          <p className="microbiology-card__hint">
            {intl.formatMessage({ id: "microbiology.critical.hint" })}
          </p>
        </div>
        <Tag
          type={
            communications.some(
              (item) => item.acknowledgementStatus !== "CLOSED",
            )
              ? "red"
              : "gray"
          }
        >
          {communications.length}
        </Tag>
      </div>
      <div className="microbiology-card__body">
        {error && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({ id: "microbiology.case.error" })}
            subtitle={error}
            hideCloseButton
          />
        )}

        <div className="microbiology-form-grid">
          <Select
            id="microbiology-critical-target-type"
            labelText={intl.formatMessage({
              id: "microbiology.critical.target",
            })}
            value={effectiveTargetType}
            disabled={targetLocked}
            onChange={(event) => {
              setTargetType(event.target.value);
              setTargetId("");
            }}
          >
            {TARGET_TYPES.map((type) => (
              <SelectItem
                key={type}
                value={type}
                text={formatMicrobiologyEnum(type)}
              />
            ))}
          </Select>
          {targetOptions.length > 0 ? (
            <Select
              id="microbiology-critical-target-id"
              labelText={intl.formatMessage({
                id: "microbiology.critical.targetId",
              })}
              value={effectiveTargetId}
              disabled={targetLocked}
              onChange={(event) => setTargetId(event.target.value)}
            >
              {targetOptions.map((option) => (
                <SelectItem
                  key={option.id}
                  value={option.id}
                  text={option.label}
                />
              ))}
            </Select>
          ) : (
            <TextInput
              id="microbiology-critical-target-id"
              labelText={intl.formatMessage({
                id: "microbiology.critical.targetId",
              })}
              value={effectiveTargetId}
              disabled={
                targetLocked ||
                effectiveTargetType === "CASE" ||
                effectiveTargetType === "SAMPLE_ITEM"
              }
              onChange={(event) => setTargetId(event.target.value)}
            />
          )}
          <TextInput
            id="microbiology-critical-recipient"
            labelText={intl.formatMessage({
              id: "microbiology.critical.recipient",
            })}
            value={recipient}
            onChange={(event) => setRecipient(event.target.value)}
          />
          <TextInput
            id="microbiology-critical-recipient-contact"
            labelText={intl.formatMessage({
              id: "microbiology.critical.recipientContact",
            })}
            value={recipientContact}
            onChange={(event) => setRecipientContact(event.target.value)}
          />
          <Select
            id="microbiology-critical-method"
            labelText={intl.formatMessage({
              id: "microbiology.critical.method",
            })}
            value={communicationMethod}
            onChange={(event) => setCommunicationMethod(event.target.value)}
          >
            {METHOD_OPTIONS.map((method) => (
              <SelectItem
                key={method}
                value={method}
                text={formatMicrobiologyEnum(method)}
              />
            ))}
          </Select>
          <div className="microbiology-form-grid__wide">
            <TextArea
              id="microbiology-critical-message"
              labelText={intl.formatMessage({
                id: "microbiology.critical.message",
              })}
              value={message}
              onChange={(event) => setMessage(event.target.value)}
            />
          </div>
          <Checkbox
            id="microbiology-critical-followup"
            labelText={intl.formatMessage({
              id: "microbiology.critical.followUp",
            })}
            checked={followUpNeeded}
            onChange={(_, state) => setFollowUpNeeded(state.checked)}
          />
          <div>
            <Button onClick={logCommunication} disabled={saving}>
              {intl.formatMessage({ id: "microbiology.critical.log" })}
            </Button>
          </div>
        </div>

        <div className="microbiology-critical-list">
          {communications.length === 0 ? (
            <p>{intl.formatMessage({ id: "microbiology.critical.none" })}</p>
          ) : (
            communications.map((communication) => (
              <div className="microbiology-critical-row" key={communication.id}>
                <div>
                  <p>
                    <strong>{communication.recipient}</strong>:{" "}
                    {communication.message}
                  </p>
                  <p className="microbiology-list__meta">
                    {formatMicrobiologyEnum(communication.targetType)} ·{" "}
                    {formatMicrobiologyEnum(communication.communicationMethod)}
                  </p>
                  <Tag
                    type={statusTagType(communication.acknowledgementStatus)}
                    data-testid="microbiology-critical-status"
                  >
                    {formatMicrobiologyEnum(
                      communication.acknowledgementStatus,
                    )}
                  </Tag>
                  {communication.resolutionNote && (
                    <p>{communication.resolutionNote}</p>
                  )}
                </div>
                <div className="microbiology-critical-row__actions">
                  {communication.acknowledgementStatus === "OPEN" && (
                    <Button
                      kind="secondary"
                      size="sm"
                      onClick={() => acknowledge(communication.id)}
                      disabled={saving}
                    >
                      {intl.formatMessage({
                        id: "microbiology.critical.acknowledge",
                      })}
                    </Button>
                  )}
                  {communication.acknowledgementStatus === "ACKNOWLEDGED" && (
                    <Button
                      kind="secondary"
                      size="sm"
                      onClick={() => setClosingId(communication.id)}
                      disabled={saving}
                    >
                      {intl.formatMessage({
                        id: "microbiology.critical.close",
                      })}
                    </Button>
                  )}
                </div>
                {closingId === communication.id && (
                  <div className="microbiology-form-grid__wide">
                    <TextArea
                      id={`microbiology-critical-resolution-${communication.id}`}
                      labelText={intl.formatMessage({
                        id: "microbiology.critical.resolution",
                      })}
                      value={resolutionNote}
                      onChange={(event) =>
                        setResolutionNote(event.target.value)
                      }
                    />
                    <Button
                      onClick={() => close(communication.id)}
                      disabled={saving || !resolutionNote.trim()}
                    >
                      {intl.formatMessage({
                        id: "microbiology.critical.close",
                      })}
                    </Button>
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      </div>
    </section>
  );
};

export default CriticalCommunicationPanel;

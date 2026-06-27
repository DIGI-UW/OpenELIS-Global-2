import React, { useEffect, useState } from "react";
import {
  Button,
  Checkbox,
  InlineNotification,
  TextArea,
  TextInput,
  Tag,
} from "@carbon/react";
import { useIntl } from "react-intl";
import MicrobiologyService from "./MicrobiologyService";

const CriticalCommunicationPanel = ({
  caseId,
  service = MicrobiologyService,
}) => {
  const intl = useIntl();
  const [communications, setCommunications] = useState([]);
  const [recipient, setRecipient] = useState("");
  const [message, setMessage] = useState("");
  const [followUpNeeded, setFollowUpNeeded] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const loadCommunications = () => {
    service.getCriticalCommunications(caseId).then((rows) => {
      setCommunications(Array.isArray(rows) ? rows : []);
    });
  };

  useEffect(() => {
    loadCommunications();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [caseId]);

  const logCommunication = () => {
    if (!message.trim()) {
      setError(
        intl.formatMessage({ id: "microbiology.critical.messageRequired" }),
      );
      return;
    }
    setSaving(true);
    setError("");
    service
      .logCriticalCommunication(caseId, {
        recipient,
        message,
        followUpNeeded,
      })
      .then(() => {
        setRecipient("");
        setMessage("");
        setFollowUpNeeded(true);
        setSaving(false);
        loadCommunications();
      });
  };

  const acknowledge = (communicationId) => {
    setSaving(true);
    service.acknowledgeCriticalCommunication(communicationId).then(() => {
      setSaving(false);
      loadCommunications();
    });
  };

  return (
    <section
      className="microbiology-card"
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
        <Tag type={communications.length > 0 ? "red" : "gray"}>
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
          <TextInput
            id="microbiology-critical-recipient"
            labelText={intl.formatMessage({
              id: "microbiology.critical.recipient",
            })}
            value={recipient}
            onChange={(event) => setRecipient(event.target.value)}
          />
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
                  <Tag
                    type={
                      communication.acknowledgementStatus === "ACKNOWLEDGED"
                        ? "green"
                        : "red"
                    }
                    data-testid="microbiology-critical-status"
                  >
                    {communication.acknowledgementStatus}
                  </Tag>
                </div>
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
              </div>
            ))
          )}
        </div>
      </div>
    </section>
  );
};

export default CriticalCommunicationPanel;

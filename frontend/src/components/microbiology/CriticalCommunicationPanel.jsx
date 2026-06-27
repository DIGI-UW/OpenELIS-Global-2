import React, { useEffect, useState } from "react";
import {
  Button,
  Checkbox,
  InlineNotification,
  Stack,
  TextArea,
  TextInput,
  Tile,
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
    <section aria-labelledby="microbiology-critical-heading">
      <Stack gap={5}>
        <h3 id="microbiology-critical-heading">
          {intl.formatMessage({ id: "microbiology.critical.title" })}
        </h3>
        {error && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({ id: "microbiology.case.error" })}
            subtitle={error}
            hideCloseButton
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
        <TextArea
          id="microbiology-critical-message"
          labelText={intl.formatMessage({
            id: "microbiology.critical.message",
          })}
          value={message}
          onChange={(event) => setMessage(event.target.value)}
        />
        <Checkbox
          id="microbiology-critical-followup"
          labelText={intl.formatMessage({
            id: "microbiology.critical.followUp",
          })}
          checked={followUpNeeded}
          onChange={(_, state) => setFollowUpNeeded(state.checked)}
        />
        <Button onClick={logCommunication} disabled={saving}>
          {intl.formatMessage({ id: "microbiology.critical.log" })}
        </Button>
        <Stack gap={3}>
          {communications.map((communication) => (
            <Tile key={communication.id}>
              <p>
                <strong>{communication.recipient}</strong>:{" "}
                {communication.message}
              </p>
              <p data-testid="microbiology-critical-status">
                {communication.acknowledgementStatus}
              </p>
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
            </Tile>
          ))}
        </Stack>
      </Stack>
    </section>
  );
};

export default CriticalCommunicationPanel;

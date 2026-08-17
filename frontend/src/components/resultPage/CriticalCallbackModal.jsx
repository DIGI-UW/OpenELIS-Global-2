import React, { useContext, useState } from "react";
import { Modal, TextInput, Select, SelectItem } from "@carbon/react";
import { useIntl } from "react-intl";
import { postToOpenElisServerJsonResponse } from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import { NotificationKinds } from "../common/CustomNotification";

/**
 * OGC-714 — documents a manual critical-result phone callback (TJC
 * NPSG.02.03.01 read-back). The body carries only analysisId + recipient +
 * outcome; caller identity and time are stamped server-side. Repeat
 * submissions for the same analysis are additional attempt rows by design.
 */
const CALLBACK_STATUSES = [
  "CONFIRMED",
  "REACHED_NO_READBACK",
  "UNABLE_TO_REACH",
];

const CriticalCallbackModal = ({ open, resultRow, onClose, onLogged }) => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const [recipientName, setRecipientName] = useState("");
  const [status, setStatus] = useState("CONFIRMED");
  const [submitting, setSubmitting] = useState(false);

  if (!open || !resultRow) return null;

  const reset = () => {
    setRecipientName("");
    setStatus("CONFIRMED");
    setSubmitting(false);
  };

  const handleClose = () => {
    reset();
    onClose();
  };

  const notify = (kind, messageId) => {
    addNotification({
      kind,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({ id: messageId }),
    });
    setNotificationVisible(true);
  };

  const handleSubmit = () => {
    if (!recipientName.trim() || submitting) {
      return;
    }
    setSubmitting(true);
    const body = {
      resultId: String(resultRow.resultId),
      recipientName: recipientName.trim(),
      status: status,
    };
    postToOpenElisServerJsonResponse(
      "/rest/critical-callback",
      JSON.stringify(body),
      (data) => {
        if (data && data.id) {
          notify(NotificationKinds.success, "qa.qi.callback.save.success");
          onLogged?.(resultRow);
          reset();
          onClose();
        } else {
          setSubmitting(false);
          notify(NotificationKinds.error, "qa.qi.callback.save.fail");
        }
      },
    );
  };

  return (
    <Modal
      open={open}
      size="sm"
      data-testid="callback-modal"
      modalHeading={intl.formatMessage({ id: "qa.qi.callback.modal.title" })}
      primaryButtonText={intl.formatMessage({ id: "label.button.save" })}
      secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
      onRequestClose={handleClose}
      onRequestSubmit={handleSubmit}
      primaryButtonDisabled={!recipientName.trim() || submitting}
    >
      <p style={{ marginBottom: "1rem" }}>
        <strong>{resultRow.testName}</strong>
        {": "}
        {resultRow.resultValue}
        {resultRow.unitsOfMeasure ? " " + resultRow.unitsOfMeasure : ""}
        {" — "}
        {resultRow.accessionNumber}
      </p>
      <TextInput
        id="callback-recipient-name"
        data-testid="callback-recipient-name"
        labelText={intl.formatMessage({
          id: "qa.qi.callback.field.recipientName",
        })}
        placeholder={intl.formatMessage({
          id: "qa.qi.callback.field.recipientName.placeholder",
        })}
        maxLength={255}
        value={recipientName}
        onChange={(e) => setRecipientName(e.target.value)}
      />
      <Select
        id="callback-status"
        data-testid="callback-status"
        style={{ marginTop: "1rem" }}
        labelText={intl.formatMessage({ id: "qa.qi.callback.field.status" })}
        value={status}
        onChange={(e) => setStatus(e.target.value)}
      >
        {CALLBACK_STATUSES.map((s) => (
          <SelectItem
            key={s}
            value={s}
            text={intl.formatMessage({ id: "qa.qi.callback.status." + s })}
          />
        ))}
      </Select>
    </Modal>
  );
};

export default CriticalCallbackModal;

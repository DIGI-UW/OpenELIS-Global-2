import React, { useState } from "react";
import { InlineNotification, Modal, TextArea } from "@carbon/react";
import { useIntl } from "react-intl";

const AlertAcknowledgeModal = ({ open, alert, error, onClose, onSubmit }) => {
  const intl = useIntl();
  const [comment, setComment] = useState("");

  if (!open || !alert) return null;

  const resolving = alert.status === "ACKNOWLEDGED";
  const commentRequired = resolving || alert.severity === "CRITICAL";
  const text = resolving
    ? {
        title: "alerts.resolve.title",
        button: "alerts.resolve.button",
        label: "alerts.acknowledge.comment",
        placeholder: "alerts.acknowledge.comment.placeholder",
        required: "alerts.resolve.comment.required",
      }
    : {
        title: "alerts.acknowledge.title",
        button: "alerts.acknowledge.button",
        label: "alerts.acknowledge.note",
        required: "alerts.acknowledge.note.required",
      };

  const handleSubmit = () => {
    if (commentRequired && !comment.trim()) {
      return;
    }
    onSubmit(alert, comment);
    setComment("");
  };

  const handleClose = () => {
    setComment("");
    onClose();
  };

  return (
    <Modal
      open={open}
      modalHeading={intl.formatMessage({ id: text.title })}
      primaryButtonText={intl.formatMessage({ id: text.button })}
      secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
      onRequestClose={handleClose}
      onRequestSubmit={handleSubmit}
      primaryButtonDisabled={commentRequired && !comment.trim()}
    >
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton
          lowContrast
        />
      )}
      <p style={{ marginBottom: "1rem" }}>{alert.message}</p>
      {commentRequired && (
        <p
          style={{
            marginBottom: "0.5rem",
            color: "#da1e28",
            fontWeight: "bold",
          }}
        >
          {intl.formatMessage({ id: text.required })}
        </p>
      )}
      <TextArea
        id="acknowledge-comment"
        labelText={intl.formatMessage({ id: text.label })}
        placeholder={
          text.placeholder
            ? intl.formatMessage({ id: text.placeholder })
            : undefined
        }
        value={comment}
        onChange={(e) => setComment(e.target.value)}
        rows={4}
      />
    </Modal>
  );
};

export default AlertAcknowledgeModal;

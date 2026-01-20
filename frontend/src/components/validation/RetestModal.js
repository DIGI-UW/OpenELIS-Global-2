import React, { useState } from "react";
import { Modal, TextArea, InlineNotification } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

const RetestModal = ({ isOpen, onClose, onConfirm, selectedCount }) => {
  const intl = useIntl();
  const [reason, setReason] = useState("");
  const [showError, setShowError] = useState(false);

  const handleSubmit = () => {
    if (!reason || reason.trim() === "") {
      setShowError(true);
      return;
    }

    onConfirm(reason);
    setReason("");
    setShowError(false);
  };

  const handleClose = () => {
    setReason("");
    setShowError(false);
    onClose();
  };

  return (
    <Modal
      open={isOpen}
      onRequestClose={handleClose}
      onRequestSubmit={handleSubmit}
      modalHeading={intl.formatMessage({ id: "validation.retest.modal.title" })}
      primaryButtonText={intl.formatMessage({
        id: "validation.retest.modal.confirm",
      })}
      secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
      size="md"
    >
      <div style={{ marginBottom: "1rem" }}>
        <p>
          <FormattedMessage
            id="validation.retest.modal.description"
            values={{ count: selectedCount }}
          />
        </p>
      </div>

      {showError && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({ id: "notification.error" })}
          subtitle={intl.formatMessage({
            id: "validation.retest.modal.reason.required",
          })}
          lowContrast
          style={{ marginBottom: "1rem" }}
        />
      )}

      <TextArea
        id="retest-reason"
        labelText={intl.formatMessage({
          id: "validation.retest.modal.reason.label",
        })}
        placeholder={intl.formatMessage({
          id: "validation.retest.modal.reason.placeholder",
        })}
        value={reason}
        onChange={(e) => {
          setReason(e.target.value);
          if (showError && e.target.value.trim() !== "") {
            setShowError(false);
          }
        }}
        rows={4}
        required
        invalid={showError}
        invalidText={intl.formatMessage({
          id: "validation.retest.modal.reason.required",
        })}
      />

      <div style={{ marginTop: "1rem" }}>
        <p style={{ fontSize: "0.875rem", color: "#525252" }}>
          <FormattedMessage id="validation.retest.modal.note" />
        </p>
      </div>
    </Modal>
  );
};

export default RetestModal;

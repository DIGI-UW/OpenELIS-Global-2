import React, { useState } from "react";
import { Modal, TextArea } from "@carbon/react";
import { useIntl } from "react-intl";

const WithdrawModal = ({ open, enrollment, onClose, onConfirm }) => {
  const intl = useIntl();
  const [reason, setReason] = useState("");

  const handleClose = () => {
    setReason("");
    onClose();
  };

  const handleSubmit = () => {
    onConfirm(enrollment?.id, reason);
    setReason("");
  };

  return (
    <Modal
      open={open}
      danger
      modalHeading={intl.formatMessage({ id: "eqa.enrollment.withdraw" })}
      primaryButtonText={intl.formatMessage({
        id: "eqa.enrollment.withdraw",
      })}
      secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
      onRequestClose={handleClose}
      onRequestSubmit={handleSubmit}
    >
      <p style={{ marginBottom: "1rem" }}>
        {intl.formatMessage({ id: "eqa.enrollment.confirmWithdraw" })}
      </p>
      {enrollment && (
        <p style={{ fontWeight: 600, marginBottom: "1rem" }}>
          {enrollment.organizationName} ({enrollment.organizationCode})
        </p>
      )}
      <TextArea
        id="withdraw-reason"
        labelText={intl.formatMessage({
          id: "eqa.enrollment.withdrawReason",
        })}
        value={reason}
        onChange={(e) => setReason(e.target.value)}
        placeholder={intl.formatMessage({
          id: "eqa.enrollment.withdrawReasonPlaceholder",
        })}
      />
    </Modal>
  );
};

export default WithdrawModal;

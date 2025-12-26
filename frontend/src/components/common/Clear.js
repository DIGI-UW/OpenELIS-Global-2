import React from "react";
import { Modal } from "@carbon/react";

const ClearConfirmationModal = ({
  open,
  onConfirm,
  onCancel,
}) => {
  return (
    <Modal
      open={open}
      danger
      modalHeading="Clear patient form?"
      primaryButtonText="Clear form"
      secondaryButtonText="Cancel"
      onRequestClose={onCancel}
      onRequestSubmit={onConfirm}
    >
      <p>
        This will remove all entered patient information. This action cannot be undone.
      </p>
    </Modal>
  );
};

export default ClearConfirmationModal;

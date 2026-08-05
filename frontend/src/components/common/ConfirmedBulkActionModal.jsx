import React from "react";
import { Modal } from "@carbon/react";

const ConfirmedBulkActionModal = ({
  open,
  danger = false,
  title,
  description,
  items = [],
  confirmLabel,
  cancelLabel,
  closeLabel,
  working = false,
  onClose,
  onConfirm,
}) => (
  <Modal
    open={open}
    danger={danger}
    modalHeading={title}
    primaryButtonText={confirmLabel}
    secondaryButtonText={cancelLabel}
    closeButtonLabel={closeLabel}
    primaryButtonDisabled={working}
    onRequestClose={onClose}
    onRequestSubmit={onConfirm}
  >
    <p>{description}</p>
    <ul aria-label={title}>
      {items.map((item) => (
        <li key={item}>{item}</li>
      ))}
    </ul>
  </Modal>
);

export default ConfirmedBulkActionModal;

import React, { useState, useEffect } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  TextArea,
  InlineNotification,
} from "@carbon/react";

/**
 * DismissalModal provides a confirmation dialog for dismissing a
 * delta check alert with a required reason.
 */
const DismissalModal = ({ open, alert, onClose, onDismiss }) => {
  const intl = useIntl();
  const [reason, setReason] = useState("");
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!open) {
      setReason("");
      setError(null);
      setSubmitting(false);
    }
  }, [open]);

  const handleClose = () => {
    if (document.activeElement && document.activeElement.blur) {
      document.activeElement.blur();
    }
    onClose?.();
  };

  const handleSubmit = () => {
    if (!reason.trim()) {
      setError("deltacheck.dismiss.validation.reasonRequired");
      return;
    }
    if (reason.trim().length < 10) {
      setError("deltacheck.dismiss.validation.reasonMinLength");
      return;
    }

    setError(null);
    setSubmitting(true);
    onDismiss?.(alert.id, reason, (_status, isError) => {
      if (isError) setSubmitting(false);
    });
  };

  return (
    <ComposedModal open={open} onClose={handleClose}>
      <ModalHeader
        label={intl.formatMessage({ id: "deltacheck.alert.title" })}
        title={intl.formatMessage({ id: "deltacheck.dismiss.title" })}
      />
      <ModalBody>
        {error && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({ id: error })}
            lowContrast
            hideCloseButton
          />
        )}
        <TextArea
          id="dismiss-reason"
          labelText={intl.formatMessage({ id: "deltacheck.dismiss.reason" })}
          placeholder={intl.formatMessage({
            id: "deltacheck.dismiss.reason.placeholder",
          })}
          value={reason}
          onChange={(e) => {
            setReason(e.target.value);
            setError(null);
          }}
          rows={3}
        />
      </ModalBody>
      <ModalFooter>
        <Button kind="secondary" onClick={handleClose} disabled={submitting}>
          <FormattedMessage id="deltacheck.dismiss.cancel" />
        </Button>
        <Button kind="primary" onClick={handleSubmit} disabled={submitting}>
          <FormattedMessage id="deltacheck.dismiss.confirm" />
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default DismissalModal;

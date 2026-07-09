import { Modal, TextArea } from "@carbon/react";
import { useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { putToOpenElisServer } from "../utils/Utils";

const MarkLostModal = ({ open, referral, onClose, onSuccess }) => {
  const intl = useIntl();
  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [showError, setShowError] = useState(false);

  const close = () => {
    setReason("");
    setShowError(false);
    setSubmitting(false);
    onClose();
  };

  const submit = () => {
    if (reason.trim().length === 0) {
      setShowError(true);
      return;
    }
    setSubmitting(true);
    putToOpenElisServer(
      `/rest/reference-lab-results/referrals/${referral.id}/mark-lost`,
      JSON.stringify({ reason: reason.trim() }),
      (status) => {
        setSubmitting(false);
        if (status === 204 || status === 200) {
          onSuccess(referral);
          close();
        } else {
          setShowError(true);
        }
      },
    );
  };

  return (
    <Modal
      open={open}
      modalHeading={intl.formatMessage({ id: "referral.markLost.modalTitle" })}
      modalLabel={referral?.labNumber}
      primaryButtonText={intl.formatMessage({
        id: "referral.markLost.confirmButton",
      })}
      secondaryButtonText={intl.formatMessage({
        id: "referral.markLost.cancelButton",
      })}
      primaryButtonDisabled={submitting}
      danger
      onRequestClose={close}
      onRequestSubmit={submit}
    >
      <p style={{ marginBottom: "1rem" }}>
        <FormattedMessage id="referral.markLost.description" />
      </p>
      <TextArea
        id="mark-lost-reason"
        labelText={intl.formatMessage({ id: "referral.markLost.reasonLabel" })}
        placeholder={intl.formatMessage({
          id: "referral.markLost.reasonPlaceholder",
        })}
        value={reason}
        onChange={(e) => {
          setReason(e.target.value);
          if (showError && e.target.value.trim().length > 0)
            setShowError(false);
        }}
        invalid={showError}
        invalidText={intl.formatMessage({
          id: "referral.markLost.reasonRequired",
        })}
        rows={4}
        maxCount={500}
      />
    </Modal>
  );
};

export default MarkLostModal;

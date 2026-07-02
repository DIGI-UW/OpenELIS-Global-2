import {
  InlineNotification,
  Modal,
  Select,
  SelectItem,
  TextArea,
} from "@carbon/react";
import { useContext, useState } from "react";
import { useIntl } from "react-intl";
import { NotificationContext } from "../layout/Layout";
import { putToOpenElisServer } from "../utils/Utils";

const REASON_CODES = [
  "insufficientVolume",
  "wrongSampleType",
  "damagedContainer",
  "temperatureDeviation",
  "hemolyzed",
  "clotted",
  "mislabeled",
  "other",
];

const RejectModal = ({ open, referral, onClose, onSuccess }) => {
  const intl = useIntl();
  const { addNotification } = useContext(NotificationContext);
  const [reasonCode, setReasonCode] = useState(REASON_CODES[0]);
  const [reasonText, setReasonText] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [showError, setShowError] = useState(false);

  const close = () => {
    setReasonCode(REASON_CODES[0]);
    setReasonText("");
    setShowError(false);
    setSubmitting(false);
    onClose();
  };

  const submit = () => {
    if (reasonText.trim().length === 0) {
      setShowError(true);
      return;
    }
    setSubmitting(true);
    putToOpenElisServer(
      `/rest/reference-lab-results/referrals/${referral.id}/reject`,
      JSON.stringify({ reasonCode, reasonText: reasonText.trim() }),
      (status) => {
        setSubmitting(false);
        if (status === 204 || status === 200) {
          addNotification({
            kind: "success",
            title: intl.formatMessage({ id: "notification.success" }),
            message: intl.formatMessage({
              id: "referral.notification.rejected",
            }),
          });
          onSuccess(referral);
          close();
        } else {
          addNotification({
            kind: "error",
            title: intl.formatMessage({ id: "notification.error" }),
            message: intl.formatMessage({ id: "referral.reject.error" }),
          });
          setShowError(true);
        }
      },
    );
  };

  return (
    <Modal
      open={open}
      modalHeading={intl.formatMessage({ id: "referral.reject.modalTitle" })}
      modalLabel={referral?.labNumber}
      primaryButtonText={intl.formatMessage({
        id: "referral.reject.confirmButton",
      })}
      secondaryButtonText={intl.formatMessage({
        id: "referral.reject.cancelButton",
      })}
      primaryButtonDisabled={submitting}
      danger
      onRequestClose={close}
      onRequestSubmit={submit}
    >
      <InlineNotification
        kind="warning"
        lowContrast
        hideCloseButton
        title={intl.formatMessage({ id: "referral.reject.warningTitle" })}
        subtitle={intl.formatMessage({ id: "referral.reject.description" })}
        style={{ marginBottom: "1rem" }}
      />
      <Select
        id="reject-reason-code"
        labelText={intl.formatMessage({ id: "referral.reject.reasonLabel" })}
        value={reasonCode}
        onChange={(e) => setReasonCode(e.target.value)}
      >
        {REASON_CODES.map((code) => (
          <SelectItem
            key={code}
            value={code}
            text={intl.formatMessage({ id: `referral.reject.reason.${code}` })}
          />
        ))}
      </Select>
      <TextArea
        id="reject-reason-text"
        labelText={intl.formatMessage({
          id: "referral.reject.reasonTextLabel",
        })}
        placeholder={intl.formatMessage({
          id: "referral.reject.reasonTextPlaceholder",
        })}
        value={reasonText}
        onChange={(e) => {
          setReasonText(e.target.value);
          if (showError && e.target.value.trim().length > 0)
            setShowError(false);
        }}
        invalid={showError}
        invalidText={intl.formatMessage({
          id: "referral.reject.reasonTextRequired",
        })}
        rows={4}
        maxCount={500}
        style={{ marginTop: "1rem" }}
      />
    </Modal>
  );
};

export default RejectModal;

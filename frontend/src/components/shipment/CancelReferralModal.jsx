import { useState, useContext } from "react";
import { Modal, TextArea, InlineNotification } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../layout/Layout";
import { putToOpenElisServerFullResponse } from "../utils/Utils";

const CancelReferralModal = ({ open, onClose, sample, onSuccess }) => {
  const intl = useIntl();
  const { addNotification } = useContext(NotificationContext);

  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = () => {
    if (!reason.trim()) {
      setError(intl.formatMessage({ id: "shipment.error.reasonRequired" }));
      return;
    }

    const referralId = sample?.referralTests?.[0]?.referralId;
    if (!referralId) {
      setError(intl.formatMessage({ id: "shipment.error.noReferralId" }));
      return;
    }

    setSubmitting(true);
    setError(null);

    putToOpenElisServerFullResponse(
      `/rest/unassigned-sample/${referralId}/cancel`,
      JSON.stringify({ reason: reason.trim() }),
      async (response) => {
        try {
          if (response.ok) {
            addNotification({
              kind: "success",
              title: intl.formatMessage({ id: "notification.success" }),
              message: intl.formatMessage({
                id: "shipment.notification.referralCancelled",
              }),
            });

            if (onSuccess) {
              onSuccess();
            }
            onClose();
          } else {
            const errorText = await response.text();
            const errorMessage =
              errorText ||
              intl.formatMessage({ id: "shipment.error.cancelReferral" });
            setError(errorMessage);
            addNotification({
              kind: "error",
              title: intl.formatMessage({ id: "notification.error" }),
              message: errorMessage,
            });
          }
        } catch (error) {
          const errorMessage = intl.formatMessage({
            id: "shipment.error.cancelReferral",
          });
          setError(errorMessage);
          addNotification({
            kind: "error",
            title: intl.formatMessage({ id: "notification.error" }),
            message: errorMessage,
          });
        } finally {
          setSubmitting(false);
        }
      },
    );
  };

  return (
    <Modal
      open={open}
      onRequestClose={onClose}
      onRequestSubmit={handleSubmit}
      modalHeading={intl.formatMessage({
        id: "shipment.modal.cancelReferral.title",
      })}
      primaryButtonText={intl.formatMessage({
        id: "shipment.action.cancelReferral",
      })}
      secondaryButtonText={intl.formatMessage({ id: "label.cancel" })}
      primaryButtonDisabled={submitting || !reason.trim()}
      danger
    >
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({ id: "notification.error" })}
          subtitle={error}
          lowContrast
          hideCloseButton
        />
      )}

      <div style={{ marginBottom: "1rem" }}>
        <p>
          <strong>
            <FormattedMessage id="sample.label.accessionNumber" />:
          </strong>{" "}
          {sample?.accessionNumber}
        </p>
        <p>
          <strong>
            <FormattedMessage id="shipment.label.destination" />:
          </strong>{" "}
          {sample?.destinationFacilityName}
        </p>
        <p>
          <strong>
            <FormattedMessage id="shipment.label.referralTest" />:
          </strong>{" "}
          {sample?.referralTestName}
        </p>
      </div>

      <InlineNotification
        kind="warning"
        title={intl.formatMessage({
          id: "shipment.modal.cancelReferral.warning.title",
        })}
        subtitle={intl.formatMessage({
          id: "shipment.modal.cancelReferral.warning.message",
        })}
        lowContrast
        hideCloseButton
      />

      <TextArea
        id="cancel-reason"
        labelText={intl.formatMessage({
          id: "shipment.modal.cancelReferral.reason",
        })}
        placeholder={intl.formatMessage({
          id: "shipment.modal.cancelReferral.reasonPlaceholder",
        })}
        value={reason}
        onChange={(e) => setReason(e.target.value)}
        disabled={submitting}
        rows={4}
        required
      />
    </Modal>
  );
};

export default CancelReferralModal;

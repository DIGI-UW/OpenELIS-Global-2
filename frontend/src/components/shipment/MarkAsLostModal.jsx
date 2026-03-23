import { useState, useContext } from "react";
import { Modal, TextArea, InlineNotification } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../layout/Layout";
import config from "../../config.json";

const MarkAsLostModal = ({ open, onClose, sample, onSuccess }) => {
  const intl = useIntl();
  const { addNotification } = useContext(NotificationContext);

  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async () => {
    if (!reason.trim()) {
      setError(intl.formatMessage({ id: "shipment.error.reasonRequired" }));
      return;
    }

    const referralId = sample?.referralTests?.[0]?.referralId;
    if (!referralId) {
      setError("No referral ID available for this sample");
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      const response = await fetch(
        `${config.serverBaseUrl}/rest/unassigned-sample/${referralId}/mark-lost`,
        {
          method: "PUT",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          body: JSON.stringify({ reason: reason.trim() }),
        },
      );

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(
          errorText || intl.formatMessage({ id: "shipment.error.markAsLost" }),
        );
      }

      addNotification({
        kind: "success",
        title: intl.formatMessage({ id: "notification.success" }),
        message: intl.formatMessage({
          id: "shipment.notification.sampleMarkedAsLost",
        }),
      });

      if (onSuccess) {
        onSuccess();
      }
      onClose();
    } catch (error) {
      setError(
        error.message ||
          intl.formatMessage({ id: "shipment.error.markAsLost" }),
      );
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message:
          error.message ||
          intl.formatMessage({ id: "shipment.error.markAsLost" }),
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      open={open}
      onRequestClose={onClose}
      onRequestSubmit={handleSubmit}
      modalHeading={intl.formatMessage({
        id: "shipment.modal.markAsLost.title",
      })}
      primaryButtonText={intl.formatMessage({
        id: "shipment.action.markAsLost",
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
          id: "shipment.modal.markAsLost.warning.title",
        })}
        subtitle={intl.formatMessage({
          id: "shipment.modal.markAsLost.warning.message",
        })}
        lowContrast
        hideCloseButton
      />

      <TextArea
        id="lost-reason"
        labelText={intl.formatMessage({
          id: "shipment.modal.markAsLost.reason",
        })}
        placeholder={intl.formatMessage({
          id: "shipment.modal.markAsLost.reasonPlaceholder",
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

export default MarkAsLostModal;

import React, { useState, useContext } from "react";
import { Modal, TextArea, InlineNotification } from "@carbon/react";
import { useIntl } from "react-intl";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import { rejectPool, rejectSampleItem } from "../../api/sampleAcceptanceApi";

/**
 * RejectDialog — the S-09 (OGC-580) Vector reject action. Unlike ResampleDialog
 * it creates NO replacement order: a one-time field catch cannot be re-collected,
 * so reject is terminal. Cascades to the whole pool when a vectorPoolId is given
 * (the pool is the unit of acceptance), or rejects a single non-pooled vector
 * specimen by sampleItemId.
 *
 * Props:
 *  - vectorPoolId: reject the whole pool (preferred); else
 *  - sampleItemId: reject a single (non-pooled vector) specimen
 *  - labNumber
 *  - initialReason: text pre-filled from failed checklist items (editable)
 *  - onClose(): close without rejecting
 *  - onSuccess(): rejection committed
 */
const RejectDialog = ({
  vectorPoolId,
  sampleItemId,
  labNumber,
  initialReason,
  onClose,
  onSuccess,
}) => {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [reason, setReason] = useState(initialReason || "");
  const [submitting, setSubmitting] = useState(false);
  const [reasonInvalid, setReasonInvalid] = useState(false);

  const handleSubmit = () => {
    if (!reason || !reason.trim()) {
      setReasonInvalid(true);
      return;
    }
    setSubmitting(true);
    const action = vectorPoolId
      ? rejectPool(vectorPoolId, reason.trim())
      : rejectSampleItem(sampleItemId, reason.trim());
    action
      .then(() => {
        onSuccess?.();
        onClose?.();
      })
      .catch(() => {
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({
            id: "sampleAcceptance.notify.rejectFailed",
            defaultMessage: "Failed to reject",
          }),
        });
        setNotificationVisible(true);
        setSubmitting(false);
      });
  };

  return (
    <Modal
      open
      danger
      onRequestClose={onClose}
      onRequestSubmit={handleSubmit}
      primaryButtonDisabled={submitting}
      modalHeading={intl.formatMessage(
        {
          id: "sampleAcceptance.reject.title",
          defaultMessage: "Reject — {labNumber}",
        },
        { labNumber: labNumber || "" },
      )}
      primaryButtonText={intl.formatMessage({
        id: "sampleAcceptance.reject.commit",
        defaultMessage: "Commit reject",
      })}
      secondaryButtonText={intl.formatMessage({
        id: "label.button.cancel",
        defaultMessage: "Cancel",
      })}
      size="sm"
    >
      <InlineNotification
        kind="info"
        lowContrast
        hideCloseButton
        title=""
        subtitle={intl.formatMessage({
          id: "sampleAcceptance.reject.info",
          defaultMessage:
            "Reject this catch as unusable. This cannot be re-collected, so no replacement order is created.",
        })}
      />
      <TextArea
        id="reject-reason"
        labelText={intl.formatMessage({
          id: "sampleAcceptance.reject.reason.label",
          defaultMessage: "Reason (pre-filled from failed items — editable)",
        })}
        rows={3}
        value={reason}
        invalid={reasonInvalid}
        invalidText={intl.formatMessage({
          id: "sampleAcceptance.reject.reason.required",
          defaultMessage: "A reason is required",
        })}
        onChange={(e) => {
          setReason(e.target.value);
          if (reasonInvalid && e.target.value.trim()) setReasonInvalid(false);
        }}
        disabled={submitting}
      />
    </Modal>
  );
};

export default RejectDialog;

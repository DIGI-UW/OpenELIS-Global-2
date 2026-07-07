import React, { useState, useContext } from "react";
import { Modal, TextArea, InlineNotification } from "@carbon/react";
import { useIntl } from "react-intl";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import { resampleSample } from "../../api/sampleAcceptanceApi";

/**
 * ResampleDialog — the S-09 (OGC-580) per-specimen Resample action. A
 * deliberately lightweight modal (the rich Report-NCE form stays separate): a
 * reason pre-filled from the failed checklist items, then POST
 * /sample-item/{id}/resample, which atomically records an NCE, rejects just
 * this specimen (the order's accepted specimens proceed), creates a
 * pre-populated draft replacement order, and queues the requester notification.
 *
 * Props:
 *  - sampleItemId, labNumber
 *  - initialReason: text built from the failed items (editable)
 *  - onClose(): close without resampling
 *  - onSuccess(result): result = { newSampleId, newAccessionNumber, nceId, ... }
 */
const ResampleDialog = ({
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
    resampleSample(sampleItemId, reason.trim())
      .then((result) => {
        onSuccess?.(result);
        onClose?.();
      })
      .catch(() => {
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({
            id: "sampleAcceptance.notify.resampleFailed",
            defaultMessage: "Failed to resample",
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
          id: "sampleAcceptance.resample.title",
          defaultMessage: "Resample — {labNumber}",
        },
        { labNumber: labNumber || "" },
      )}
      primaryButtonText={intl.formatMessage({
        id: "sampleAcceptance.resample.commit",
        defaultMessage: "Commit resample",
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
          id: "sampleAcceptance.resample.info",
          defaultMessage:
            "Reject this sample and create a new collection order for re-collection. The requester will be notified.",
        })}
      />
      <TextArea
        id="resample-reason"
        labelText={intl.formatMessage({
          id: "sampleAcceptance.resample.reason.label",
          defaultMessage: "Reason (pre-filled from failed items — editable)",
        })}
        rows={3}
        value={reason}
        invalid={reasonInvalid}
        invalidText={intl.formatMessage({
          id: "sampleAcceptance.resample.reason.required",
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

export default ResampleDialog;

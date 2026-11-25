import React, { useState, useContext, useEffect } from "react";
import {
  Modal,
  TextArea,
  InlineNotification,
  Select,
  SelectItem,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import { rejectPool, rejectSampleItem } from "../../api/sampleAcceptanceApi";
import { getFromOpenElisServer } from "../../../utils/Utils";

/**
 * RejectDialog — the S-09 (OGC-580) Vector reject action. Unlike ResampleDialog
 * it creates NO replacement order: a one-time field catch cannot be re-collected,
 * so reject is terminal. Cascades to the whole pool when a vectorPoolId is given
 * (the pool is the unit of acceptance), or rejects a single non-pooled vector
 * specimen by sampleItemId.
 *
 * An EQA sample (OGC-1488) is rejected for one of the lab's rejection reasons,
 * which is required, with an optional comment: PT material cannot be
 * recollected either.
 *
 * Props:
 *  - vectorPoolId: reject the whole pool (preferred); else
 *  - sampleItemId: reject a single (non-pooled vector) specimen
 *  - labNumber
 *  - initialReason: text pre-filled from failed checklist items (editable)
 *  - eqa: the specimen is an EQA sample; a rejection reason must be chosen
 *  - onClose(): close without rejecting
 *  - onSuccess(): rejection committed
 */
const RejectDialog = ({
  vectorPoolId,
  sampleItemId,
  labNumber,
  initialReason,
  eqa = false,
  onClose,
  onSuccess,
}) => {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [reason, setReason] = useState(initialReason || "");
  const [submitting, setSubmitting] = useState(false);
  const [reasonInvalid, setReasonInvalid] = useState(false);
  const [rejectReasons, setRejectReasons] = useState([]);
  const [rejectReasonId, setRejectReasonId] = useState("");

  useEffect(() => {
    if (!eqa) return undefined;
    let mounted = true;
    getFromOpenElisServer("/rest/displayList/REJECTION_REASONS", (res) => {
      if (mounted) setRejectReasons(Array.isArray(res) ? res : []);
    });
    return () => {
      mounted = false;
    };
  }, [eqa]);

  const handleSubmit = () => {
    if (eqa ? !rejectReasonId : !reason || !reason.trim()) {
      setReasonInvalid(true);
      return;
    }
    setSubmitting(true);
    const action = vectorPoolId
      ? rejectPool(vectorPoolId, reason.trim())
      : rejectSampleItem(
          sampleItemId,
          (reason || "").trim(),
          eqa ? rejectReasonId : undefined,
        );
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
          id: eqa
            ? "sampleAcceptance.reject.info.eqa"
            : "sampleAcceptance.reject.info",
        })}
      />
      {eqa && (
        <Select
          id="reject-reason-code"
          labelText={intl.formatMessage({ id: "sample.reject.reason.label" })}
          aria-required="true"
          value={rejectReasonId}
          invalid={reasonInvalid}
          invalidText={intl.formatMessage({
            id: "sample.reject.reasonRequired",
          })}
          onChange={(e) => {
            setRejectReasonId(e.target.value);
            if (e.target.value) setReasonInvalid(false);
          }}
          disabled={submitting}
        >
          <SelectItem
            text={intl.formatMessage({ id: "sample.reject.reason.choose" })}
            value=""
          />
          {rejectReasons.map((option) => (
            <SelectItem key={option.id} text={option.value} value={option.id} />
          ))}
        </Select>
      )}
      <TextArea
        id="reject-reason"
        labelText={intl.formatMessage({
          id: eqa
            ? "sampleAcceptance.reject.comment.label"
            : "sampleAcceptance.reject.reason.label",
        })}
        rows={3}
        value={reason}
        invalid={!eqa && reasonInvalid}
        invalidText={intl.formatMessage({
          id: "sampleAcceptance.reject.reason.required",
          defaultMessage: "A reason is required",
        })}
        onChange={(e) => {
          setReason(e.target.value);
          if (!eqa && reasonInvalid && e.target.value.trim()) {
            setReasonInvalid(false);
          }
        }}
        disabled={submitting}
      />
    </Modal>
  );
};

export default RejectDialog;

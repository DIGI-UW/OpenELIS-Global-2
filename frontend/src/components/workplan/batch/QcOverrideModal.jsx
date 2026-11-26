import React, { useState } from "react";
import { Modal, TextArea } from "@carbon/react";
import { useIntl } from "react-intl";
import { RequiredMarker, requiredProps } from "../../common/RequiredMarker";
import { formatDateTime, qcSubjects } from "./batchWorkplanUtils";

/**
 * The QC override dialog (OGC-427 FR-NCE-001/005). Its copy names the failure
 * mode. A blocking status (no control lot, a retired lot) has no proceed
 * button at all; a user without qa.qc.override sees it disabled, with the
 * reason in place of the body's last line. An override needs the user's reason
 * (OGC-1588), which is kept with the release and written on the NCE.
 */
export default function QcOverrideModal({ open, qc, ...props }) {
  if (!open || !qc) {
    return null;
  }
  // Mounted only while open, so each opening starts with an empty reason.
  return <OverrideDialog qc={qc} {...props} />;
}

function OverrideDialog({ batch, qc, canOverride, busy, onProceed, onClose }) {
  const intl = useIntl();
  const [reason, setReason] = useState("");
  const [tried, setTried] = useState(false);
  const { test, instrument, lot } = qcSubjects(qc);
  const values = {
    test,
    instrument:
      instrument ||
      intl.formatMessage({ id: "workplan.batch.instrument.bench" }),
    lastPass: lot?.lastPassingAt
      ? formatDateTime(intl, lot.lastPassingAt)
      : intl.formatMessage({ id: "workplan.batch.qc.never" }),
    rules: (lot?.ruleCodes || []).join(", ") || "-",
  };
  const status = qc.status;
  const blocking = qc.blocking;
  const permitted = canOverride && !blocking;
  const reasonMissing = reason.trim() === "";

  return (
    <Modal
      open
      danger
      size="sm"
      modalHeading={intl.formatMessage({
        id: `workplan.batch.override.title.${status}`,
      })}
      modalLabel={batch?.name}
      primaryButtonText={intl.formatMessage({
        id: "workplan.batch.override.proceed",
      })}
      secondaryButtonText={intl.formatMessage({
        id: "workplan.batch.override.goBack",
      })}
      primaryButtonDisabled={!permitted || busy}
      passiveModal={blocking}
      onRequestSubmit={() => {
        setTried(true);
        if (permitted && !reasonMissing) onProceed(reason.trim());
      }}
      onRequestClose={onClose}
      onSecondarySubmit={onClose}
    >
      <p className="batch-workplan__modal-body">
        {intl.formatMessage(
          { id: `workplan.batch.override.body.${status}` },
          values,
        )}
      </p>
      {!blocking && (
        <p className="batch-workplan__modal-body">
          {intl.formatMessage({
            id: canOverride
              ? "workplan.batch.override.traceability"
              : "workplan.batch.override.noPermission",
          })}
        </p>
      )}
      {permitted && (
        <TextArea
          id={`batch-${batch?.id}-override-reason`}
          labelText={
            <>
              {intl.formatMessage({
                id: "workplan.batch.override.reason.label",
              })}
              <RequiredMarker />
            </>
          }
          {...requiredProps()}
          placeholder={intl.formatMessage({
            id: "workplan.batch.override.reason.placeholder",
          })}
          value={reason}
          maxCount={1000}
          enableCounter
          invalid={tried && reasonMissing}
          invalidText={intl.formatMessage({
            id: "workplan.batch.override.reason.required",
          })}
          onChange={(event) => setReason(event.target.value)}
        />
      )}
    </Modal>
  );
}

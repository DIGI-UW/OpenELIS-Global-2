import React from "react";
import { Modal } from "@carbon/react";
import { useIntl } from "react-intl";
import { formatDateTime, qcSubjects } from "./batchWorkplanUtils";

/**
 * The QC override dialog (OGC-427 FR-NCE-001/005). Its copy names the failure
 * mode. A blocking status (no control lot, a retired lot) has no proceed
 * button at all; a user without qa.qc.override sees it disabled, with the
 * reason in place of the body's last line.
 */
export default function QcOverrideModal({
  open,
  batch,
  qc,
  canOverride,
  busy,
  onProceed,
  onClose,
}) {
  const intl = useIntl();
  if (!open || !qc) {
    return null;
  }
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
      onRequestSubmit={() => permitted && onProceed()}
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
    </Modal>
  );
}

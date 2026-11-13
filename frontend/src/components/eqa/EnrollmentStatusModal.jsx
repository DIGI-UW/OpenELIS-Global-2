import React, { useState } from "react";
import {
  DatePicker,
  DatePickerInput,
  Modal,
  Stack,
  TextArea,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { toLocalIsoDate } from "../utils/Utils";
import { calendarOnlyInput } from "./eqaCommon";

/**
 * Moves one of this laboratory's EQA enrolments between Active, Suspended and
 * Withdrawn. Both fields are required: the status the enrolment lands in is only
 * half the record, and the reason and the date it takes effect from are what a
 * later reader needs.
 */
const ACTION_LABEL = {
  Active: "eqa.enrollment.resume",
  Suspended: "eqa.enrollment.suspend",
  Withdrawn: "eqa.enrollment.withdraw",
};

const EnrollmentStatusModal = ({
  enrollment,
  nextStatus,
  onClose,
  onConfirm,
}) => {
  const intl = useIntl();
  const [reason, setReason] = useState("");
  const [effectiveDate, setEffectiveDate] = useState("");

  const actionLabel = intl.formatMessage({
    id: ACTION_LABEL[nextStatus] || "eqa.enrollment.suspend",
  });

  return (
    <Modal
      open
      danger={nextStatus === "Withdrawn"}
      modalHeading={actionLabel}
      primaryButtonText={actionLabel}
      secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
      primaryButtonDisabled={!reason.trim() || !effectiveDate}
      onRequestClose={onClose}
      onRequestSubmit={() => onConfirm(reason, effectiveDate)}
    >
      <Stack gap={5}>
        {enrollment && (
          <p style={{ fontWeight: 600 }}>
            {enrollment.programName} — {enrollment.provider}
          </p>
        )}
        {nextStatus === "Withdrawn" && (
          <p>{intl.formatMessage({ id: "eqa.enrollment.withdrawIsFinal" })}</p>
        )}
        <TextArea
          id="enrollment-status-reason"
          labelText={intl.formatMessage({
            id: "eqa.enrollment.statusReason",
          })}
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          placeholder={intl.formatMessage({
            id: "eqa.enrollment.statusReasonPlaceholder",
          })}
        />
        <DatePicker
          datePickerType="single"
          dateFormat="Y-m-d"
          value={effectiveDate ? [effectiveDate] : []}
          onChange={(dates) =>
            setEffectiveDate(dates.length ? toLocalIsoDate(dates[0]) : "")
          }
        >
          <DatePickerInput
            {...calendarOnlyInput}
            id="enrollment-status-effective-date"
            labelText={intl.formatMessage({
              id: "eqa.enrollment.statusEffectiveDate",
            })}
            placeholder="yyyy-mm-dd"
          />
        </DatePicker>
      </Stack>
    </Modal>
  );
};

export default EnrollmentStatusModal;

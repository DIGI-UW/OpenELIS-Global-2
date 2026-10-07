import React, { useState } from "react";
import { Modal, TextArea } from "@carbon/react";
import { useIntl } from "react-intl";

const CaseCancellationPrompt = ({ cases, onDecision }) => {
  const intl = useIntl();
  const [reason, setReason] = useState("");
  const required = cases.some((entry) => entry.hasResults);
  return (
    <Modal
      open
      danger
      modalHeading={intl.formatMessage({
        id: "microbiology.cancel.confirmTitle",
      })}
      primaryButtonText={intl.formatMessage({
        id: "microbiology.cancel.confirmSave",
      })}
      secondaryButtonText={intl.formatMessage({
        id: "microbiology.cancel.keepEditing",
      })}
      primaryButtonDisabled={required && !reason.trim()}
      onRequestClose={() => onDecision(null)}
      onRequestSubmit={() =>
        onDecision({
          caseIds: cases.map((entry) => entry.caseId),
          reason: reason.trim(),
        })
      }
    >
      <p>
        {intl.formatMessage(
          { id: "microbiology.cancel.explanation" },
          { count: cases.length },
        )}
      </p>
      <ul>
        {cases.map((entry) => (
          <li key={entry.caseId}>{entry.labUnit}</li>
        ))}
      </ul>
      <TextArea
        id="micro-case-cancellation-reason"
        labelText={intl.formatMessage({
          id: required
            ? "microbiology.cancel.reasonRequired"
            : "microbiology.cancel.reasonOptional",
        })}
        value={reason}
        maxLength={4000}
        onChange={(event) => setReason(event.target.value)}
      />
    </Modal>
  );
};
export default CaseCancellationPrompt;

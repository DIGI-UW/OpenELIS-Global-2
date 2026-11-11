import React, { useEffect, useState } from "react";
import { useIntl } from "react-intl";
import { Modal, Select, SelectItem, TextInput } from "@carbon/react";
import { getFromOpenElisServer } from "../utils/Utils";
import { useOrderContext } from "./OrderContext";
import { RequiredMarker, requiredProps } from "../common/RequiredMarker";
import {
  CANCEL_REASON_CATEGORY,
  OTHER_REASON,
  cancelReasonText,
} from "./cancelReasons";

/**
 * OGC-1613 — asked when a save removes a test that already has results: the
 * test is cancelled with the reason given, and the save is sent again with it.
 * Keeping the test leaves the order unsaved.
 */
const CancelTestReasonDialog = () => {
  const intl = useIntl();
  const { cancelReasonAsked, answerCancelReason } = useOrderContext();
  const [reasons, setReasons] = useState([]);
  const [choice, setChoice] = useState("");
  const [other, setOther] = useState("");

  useEffect(() => {
    if (!cancelReasonAsked) {
      return;
    }
    setChoice("");
    setOther("");
    getFromOpenElisServer(
      `/rest/dictionary/categories/${encodeURIComponent(CANCEL_REASON_CATEGORY)}/entries`,
      (entries) => setReasons(Array.isArray(entries) ? entries : []),
    );
  }, [cancelReasonAsked]);

  const reason = cancelReasonText(choice, other);

  if (!cancelReasonAsked) {
    return null;
  }
  return (
    <Modal
      open
      danger
      size="sm"
      data-testid="cancel-test-reason-dialog"
      modalHeading={intl.formatMessage({ id: "order.removeTest.heading" })}
      primaryButtonText={intl.formatMessage({ id: "order.removeTest.confirm" })}
      primaryButtonDisabled={!reason}
      secondaryButtonText={intl.formatMessage({ id: "order.removeTest.keep" })}
      onRequestClose={() => answerCancelReason(null)}
      onRequestSubmit={() => answerCancelReason(reason)}
    >
      <p>{intl.formatMessage({ id: "order.removeTest.body" })}</p>
      <Select
        id="cancel-test-reason"
        labelText={
          <>
            {intl.formatMessage({ id: "order.dashboard.cancel.reason" })}
            <RequiredMarker />
          </>
        }
        {...requiredProps()}
        value={choice}
        onChange={(e) => setChoice(e.target.value)}
      >
        <SelectItem value="" text="" />
        {reasons
          .filter((entry) => entry.label !== "Other")
          .map((entry) => (
            <SelectItem
              key={entry.code}
              value={entry.label}
              text={entry.label}
            />
          ))}
        <SelectItem
          value={OTHER_REASON}
          text={intl.formatMessage({ id: "order.dashboard.cancel.other" })}
        />
      </Select>
      {choice === OTHER_REASON && (
        <TextInput
          id="cancel-test-reason-other"
          labelText={
            <>
              {intl.formatMessage({ id: "order.dashboard.cancel.reasonOther" })}
              <RequiredMarker />
            </>
          }
          {...requiredProps()}
          value={other}
          onChange={(e) => setOther(e.target.value)}
          maxLength={255}
        />
      )}
    </Modal>
  );
};

export default CancelTestReasonDialog;

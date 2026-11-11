import React, { useEffect, useRef, useState } from "react";
import { InlineNotification, Modal, Stack, TextInput } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { postToOpenElisServerJsonResponse } from "../utils/Utils";
import { isDeactivatedRow, objectNaming } from "./pathologyRows";

const DeactivationModal = ({ pending, onClose, onRowDeactivated }) => {
  const intl = useIntl();
  const mounted = useRef(false);
  // Carbon binds its Escape handler once, when the modal opens, so the close
  // it calls must read the request's state from a ref, not a closure.
  const inFlight = useRef(false);
  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [failed, setFailed] = useState(false);
  const [triedBlank, setTriedBlank] = useState(false);

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  const { kind, row } = pending;
  const naming = objectNaming(kind, row);
  const blank = reason.trim() === "";

  // Mid-flight the request will land either way, so closing would only hide
  // its outcome from the person who asked for it.
  const close = () => {
    if (inFlight.current) {
      return;
    }
    onClose();
    // Carbon hands focus back only when a mounted modal closes, and this one
    // unmounts instead.
    setTimeout(() => pending.launcher?.focus());
  };

  const submit = () => {
    if (inFlight.current) {
      return;
    }
    if (blank) {
      setTriedBlank(true);
      return;
    }
    inFlight.current = true;
    setSubmitting(true);
    setFailed(false);
    postToOpenElisServerJsonResponse(
      "/rest/pathology/" + kind + "/" + row.id + "/deactivate",
      JSON.stringify({ reason: reason.trim() }),
      (response) => {
        if (!mounted.current) {
          return;
        }
        if (isDeactivatedRow(response)) {
          onRowDeactivated(pending);
          return;
        }
        inFlight.current = false;
        setSubmitting(false);
        setFailed(true);
      },
    );
  };

  return (
    <Modal
      open
      danger
      size="sm"
      modalHeading={intl.formatMessage(
        { id: naming.headingKey },
        naming.values,
      )}
      primaryButtonText={intl.formatMessage({ id: "common.deactivate" })}
      secondaryButtonText={intl.formatMessage({ id: "common.cancel" })}
      closeButtonLabel={intl.formatMessage({ id: "label.button.close" })}
      primaryButtonDisabled={submitting}
      loadingStatus={submitting ? "active" : "inactive"}
      loadingDescription={intl.formatMessage({
        id: "pathology.label.deactivating",
      })}
      selectorPrimaryFocus="#deactivationReason"
      preventCloseOnClickOutside
      shouldSubmitOnEnter
      onRequestClose={close}
      onRequestSubmit={submit}
    >
      <Stack gap={5}>
        <p>
          <FormattedMessage id="pathology.modal.deactivateBody" />
        </p>
        <TextInput
          id="deactivationReason"
          labelText={intl.formatMessage({ id: "common.reason" })}
          required
          value={reason}
          invalid={triedBlank && blank}
          invalidText={intl.formatMessage({
            id: "pathology.locked.reasonRequired",
          })}
          onChange={(e) => setReason(e.target.value)}
        />
        {failed && (
          <InlineNotification
            kind="error"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "pathology.modal.deactivateFailed",
            })}
          />
        )}
      </Stack>
    </Modal>
  );
};

/**
 * The one confirmation a saved cassette, block or slide needs before it is
 * deactivated, mounted only while one is pending and fresh for each object.
 */
const DeactivateRowDialog = ({ pending, ...props }) =>
  pending ? (
    <DeactivationModal
      key={pending.kind + "-" + pending.row.id}
      pending={pending}
      {...props}
    />
  ) : null;

export default DeactivateRowDialog;

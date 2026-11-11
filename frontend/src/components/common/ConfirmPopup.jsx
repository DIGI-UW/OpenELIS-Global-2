import React from "react";
import {
  Button,
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
} from "@carbon/react";

import { FormattedMessage, useIntl } from "react-intl";

const ConfirmPopup = ({ isOpen, onClose, onConfirm, messageCode }) => {
  const intl = useIntl();
  return (
    <ComposedModal open={isOpen} onClose={onClose}>
      <ModalHeader
        label={intl.formatMessage({
          id: "label.button.confirmAction",
        })}
        title={intl.formatMessage({
          id: "label.button.confirmTitle",
        })}
      />
      <ModalBody>
        <FormattedMessage id={messageCode} />
      </ModalBody>
      <ModalFooter>
        <Button kind="secondary" onClick={onClose}>
          <FormattedMessage id="label.button.cancel" />
        </Button>
        <Button kind="primary" onClick={onConfirm}>
          <FormattedMessage id="label.button.confirm" />
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default ConfirmPopup;

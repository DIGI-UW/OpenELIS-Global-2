import React from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";

/**
 * PrintLabelConfirmationDialog - Confirmation dialog for label printing
 * Shows: "Print label for [Location Name] ([Location Code])?"
 *
 * Props:
 * - open: boolean - Whether dialog is open
 * - locationName: string - Location name
 * - locationCode: string - Location code
 * - onConfirm: function - Callback when user confirms
 * - onCancel: function - Callback when user cancels
 */
const PrintLabelConfirmationDialog = ({
  open,
  locationName,
  locationCode,
  onConfirm,
  onCancel,
}) => {
  const intl = useIntl();

  const displayName = locationName || locationCode || "this location";
  const displayCode = locationCode ? ` (${locationCode})` : "";

  return (
    <ComposedModal
      open={open}
      onClose={onCancel}
      size="sm"
      data-testid="print-label-confirmation-dialog"
    >
      <ModalHeader
        title={intl.formatMessage({
          id: "label.printConfirmation.title",
          defaultMessage: "Print Label",
        })}
      />
      <ModalBody>
        <p>
          {intl.formatMessage(
            {
              id: "label.printConfirmation.message",
              defaultMessage: "Print label for {name}{code}?",
            },
            {
              name: displayName,
              code: displayCode,
            },
          )}
        </p>
      </ModalBody>
      <ModalFooter>
        <Button
          kind="secondary"
          onClick={onCancel}
          data-testid="cancel-print-button"
        >
          <FormattedMessage id="label.button.cancel" defaultMessage="Cancel" />
        </Button>
        <Button
          kind="primary"
          onClick={onConfirm}
          data-testid="confirm-print-button"
        >
          <FormattedMessage id="label.print" defaultMessage="Print" />
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

PrintLabelConfirmationDialog.propTypes = {
  open: PropTypes.bool.isRequired,
  locationName: PropTypes.string,
  locationCode: PropTypes.string,
  onConfirm: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired,
};

PrintLabelConfirmationDialog.defaultProps = {
  locationName: "",
  locationCode: "",
};

export default PrintLabelConfirmationDialog;

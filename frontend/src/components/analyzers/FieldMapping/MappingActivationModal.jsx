/**
 * MappingActivationModal Component
 *
 * Warning modal for confirming activation of mapping changes
 * Task Reference: T164
 *
 * Per FR-010 specification:
 * - Warning variant ComposedModal
 * - Dialog header with title and subtitle
 * - Warning message section with warning icon
 * - Additional warning for active analyzers
 * - Confirmation checkbox (required before activation)
 * - Dialog footer with Cancel and "Activate Changes" button (destructive style)
 */

import React, { useState, useEffect } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  Checkbox,
  InlineNotification,
} from "@carbon/react";
import { WarningAlt } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import "./MappingActivationModal.css";

const MappingActivationModal = ({
  open,
  onClose,
  analyzerName,
  analyzerIsActive = false,
  onConfirm,
}) => {
  const intl = useIntl();
  const [confirmed, setConfirmed] = useState(false);

  // Reset state when modal opens/closes
  useEffect(() => {
    if (!open) {
      setConfirmed(false);
    }
  }, [open]);

  const handleConfirm = () => {
    if (confirmed && onConfirm) {
      onConfirm();
    }
  };

  const handleClose = () => {
    setConfirmed(false);
    if (onClose) {
      onClose();
    }
  };

  return (
    <ComposedModal
      open={open}
      onClose={handleClose}
      size="sm"
      data-testid="mapping-activation-modal"
    >
      <ModalHeader
        title={intl.formatMessage({
          id: "analyzer.fieldMapping.activationModal.title",
          defaultMessage: "Activate Mapping Changes",
        })}
        label={intl.formatMessage(
          {
            id: "analyzer.fieldMapping.activationModal.subtitle",
            defaultMessage: "Confirm activation of mapping changes for analyzer '{name}'",
          },
          { name: analyzerName || "" }
        )}
        data-testid="mapping-activation-modal-header"
      />
      <ModalBody data-testid="mapping-activation-modal-body">
        <div className="mapping-activation-warning-section">
          <div className="warning-icon-container">
            <WarningAlt size={24} className="warning-icon" />
          </div>
          <div className="warning-messages">
            <InlineNotification
              kind="warning"
              title={intl.formatMessage({
                id: "analyzer.fieldMapping.activationModal.warning",
                defaultMessage: `You are about to activate mapping changes for analyzer '${analyzerName || ""}'. These changes will apply to all new messages received after activation. Existing results will not be affected.`,
              })}
              hideCloseButton
              lowContrast
              data-testid="mapping-activation-warning"
            />
            {analyzerIsActive && (
              <InlineNotification
                kind="warning"
                title={intl.formatMessage({
                  id: "analyzer.fieldMapping.activationModal.warningActive",
                  defaultMessage:
                    "This analyzer is currently active. Activating changes may affect incoming results.",
                })}
                hideCloseButton
                lowContrast
                data-testid="mapping-activation-active-warning"
              />
            )}
          </div>
        </div>

        <div className="mapping-activation-confirmation">
          <Checkbox
            id="activation-confirmation-checkbox"
            labelText={intl.formatMessage({
              id: "analyzer.fieldMapping.activationModal.confirmCheckbox",
              defaultMessage:
                "I understand these changes will apply to new messages only",
            })}
            checked={confirmed}
            onChange={(checked) => setConfirmed(checked)}
            data-testid="activation-confirmation-checkbox"
          />
        </div>
      </ModalBody>
      <ModalFooter data-testid="mapping-activation-modal-footer">
        <Button
          kind="secondary"
          onClick={handleClose}
          data-testid="mapping-activation-cancel-button"
        >
          <FormattedMessage
            id="analyzer.fieldMapping.activationModal.cancel"
            defaultMessage="Cancel"
          />
        </Button>
        <Button
          kind="danger"
          onClick={handleConfirm}
          disabled={!confirmed}
          data-testid="activation-confirm-button"
        >
          <FormattedMessage
            id="analyzer.fieldMapping.activationModal.activate"
            defaultMessage="Activate Changes"
          />
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default MappingActivationModal;

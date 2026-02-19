/**
 * MappingActivationModal Component
 *
 * Warning modal for confirming activation of mapping changes
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
  Link,
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
  pendingMessagesCount = 0,
  missingRequired = [],
  concurrentEdit = false,
  onViewPendingMessages,
  onReloadPage,
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
            defaultMessage:
              "Confirm activation of mapping changes for analyzer '{name}'",
          },
          { name: analyzerName || "" },
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
            {pendingMessagesCount > 0 && (
              <div>
                <InlineNotification
                  kind="warning"
                  title={intl.formatMessage(
                    {
                      id: "analyzer.fieldMapping.activationModal.warningPendingMessages",
                      defaultMessage:
                        "This analyzer has {count} pending messages in the error queue. Activating mapping changes may affect how these messages are reprocessed. Consider resolving errors first.",
                    },
                    { count: pendingMessagesCount },
                  )}
                  hideCloseButton
                  lowContrast
                  data-testid="mapping-activation-pending-messages-warning"
                />
                {onViewPendingMessages && (
                  <Link
                    onClick={onViewPendingMessages}
                    data-testid="view-pending-messages-link"
                  >
                    <FormattedMessage
                      id="analyzer.fieldMapping.activationModal.viewPendingMessages"
                      defaultMessage="View Pending Messages"
                    />
                  </Link>
                )}
              </div>
            )}
            {concurrentEdit && (
              <div>
                <InlineNotification
                  kind="error"
                  title={intl.formatMessage({
                    id: "analyzer.fieldMapping.activationModal.errorConcurrentEdit",
                    defaultMessage:
                      "Mapping changes detected. Another user has modified mappings for this analyzer. Please reload the page to see latest changes.",
                  })}
                  hideCloseButton
                  lowContrast
                  data-testid="mapping-activation-concurrent-edit-error"
                />
                {onReloadPage && (
                  <Link onClick={onReloadPage} data-testid="reload-page-link">
                    <FormattedMessage
                      id="analyzer.fieldMapping.activationModal.reloadPage"
                      defaultMessage="Reload Page"
                    />
                  </Link>
                )}
              </div>
            )}
            {missingRequired.length > 0 && (
              <InlineNotification
                kind="error"
                title={intl.formatMessage({
                  id: "analyzer.fieldMapping.activationModal.errorMissingRequired",
                  defaultMessage: "Cannot activate: Required mappings missing",
                })}
                subtitle={intl.formatMessage(
                  {
                    id: "analyzer.fieldMapping.activationModal.errorMissingRequired.detail",
                    defaultMessage: "Missing required fields: {fields}",
                  },
                  { fields: missingRequired.join(", ") },
                )}
                hideCloseButton
                lowContrast
                data-testid="mapping-activation-missing-required-error"
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
            onChange={(_, { checked }) => setConfirmed(checked)}
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
          disabled={!confirmed || missingRequired.length > 0 || concurrentEdit}
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

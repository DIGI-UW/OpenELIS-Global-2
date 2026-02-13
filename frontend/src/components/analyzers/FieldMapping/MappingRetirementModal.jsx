/**
 * MappingRetirementModal Component
 *
 * Confirmation modal for retiring field mappings
 *
 * Per FR-013 specification:
 * - Small size ComposedModal (~400px)
 * - Dialog header with title and subtitle
 * - Confirmation message
 * - Retirement reason field (optional TextArea)
 * - Warning if pending messages exist
 * - Action buttons: Cancel, "Retire Mapping" (destructive)
 */

import React, { useState, useEffect } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  TextArea,
  InlineNotification,
} from "@carbon/react";

import { FormattedMessage, useIntl } from "react-intl";
import "./MappingRetirementModal.css";

const MappingRetirementModal = ({
  open,
  onClose,
  mappingName,
  pendingMessagesCount = 0,
  onConfirm,
}) => {
  const intl = useIntl();
  const [retirementReason, setRetirementReason] = useState("");
  const [characterCount, setCharacterCount] = useState(0);
  const maxReasonLength = 500;

  // Reset state when modal opens/closes
  useEffect(() => {
    if (!open) {
      setRetirementReason("");
      setCharacterCount(0);
    }
  }, [open]);

  const handleReasonChange = (event) => {
    const value = event.target.value;
    if (value.length <= maxReasonLength) {
      setRetirementReason(value);
      setCharacterCount(value.length);
    }
  };

  const handleConfirm = () => {
    if (onConfirm && pendingMessagesCount === 0) {
      onConfirm(retirementReason);
    }
  };

  const handleClose = () => {
    setRetirementReason("");
    setCharacterCount(0);
    if (onClose) {
      onClose();
    }
  };

  const canRetire = pendingMessagesCount === 0;

  return (
    <ComposedModal
      open={open}
      onClose={handleClose}
      size="sm"
      data-testid="mapping-retirement-modal"
      preventCloseOnClickOutside={false}
    >
      <ModalHeader
        title={intl.formatMessage({
          id: "analyzer.fieldMapping.retirementModal.title",
          defaultMessage: "Retire Mapping",
        })}
        label={intl.formatMessage({
          id: "analyzer.fieldMapping.retirementModal.subtitle",
          defaultMessage: "Confirm retirement of field mapping",
        })}
        data-testid="mapping-retirement-modal-header"
      />
      <ModalBody data-testid="mapping-retirement-modal-body">
        <div className="mapping-retirement-content">
          <p className="mapping-retirement-confirmation-message">
            <FormattedMessage
              id="analyzer.fieldMapping.retirementModal.confirmation"
              defaultMessage="Are you sure you want to retire this mapping? Historical messages will still reference it for audit purposes."
            />
          </p>

          {pendingMessagesCount > 0 && (
            <InlineNotification
              kind="error"
              title={intl.formatMessage(
                {
                  id: "analyzer.fieldMapping.retirementModal.errorPendingMessages",
                  defaultMessage:
                    "Cannot retire: {count} pending messages use this mapping",
                },
                { count: pendingMessagesCount },
              )}
              hideCloseButton
              lowContrast
              data-testid="mapping-retirement-pending-messages-error"
            />
          )}

          {canRetire && (
            <div className="mapping-retirement-reason-section">
              <TextArea
                id="retirement-reason"
                labelText={intl.formatMessage({
                  id: "analyzer.fieldMapping.retirementModal.reasonLabel",
                  defaultMessage: "Retirement Reason (Optional)",
                })}
                placeholder={intl.formatMessage({
                  id: "analyzer.fieldMapping.retirementModal.reasonPlaceholder",
                  defaultMessage: "Optional: Reason for retiring this mapping",
                })}
                value={retirementReason}
                onChange={handleReasonChange}
                maxLength={maxReasonLength}
                rows={3}
                data-testid="retirement-reason-textarea"
              />
              <div className="character-count">
                {characterCount} / {maxReasonLength} characters
              </div>
            </div>
          )}
        </div>
      </ModalBody>
      <ModalFooter data-testid="mapping-retirement-modal-footer">
        <Button
          kind="secondary"
          onClick={handleClose}
          data-testid="mapping-retirement-cancel-button"
        >
          <FormattedMessage
            id="analyzer.fieldMapping.retirementModal.cancel"
            defaultMessage="Cancel"
          />
        </Button>
        <Button
          kind="danger"
          onClick={handleConfirm}
          disabled={!canRetire}
          data-testid="mapping-retirement-confirm-button"
        >
          <FormattedMessage
            id="analyzer.fieldMapping.retirementModal.retire"
            defaultMessage="Retire Mapping"
          />
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default MappingRetirementModal;

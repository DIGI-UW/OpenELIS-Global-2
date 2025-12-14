import React, { useState, useEffect } from "react";
import {
  Modal,
  RadioButtonGroup,
  RadioButton,
  TextArea,
  InlineNotification,
  Tag,
  Loading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../../config.json";
import "./NotebookWorkflow.css";

/**
 * FlagSampleModal - Modal for flagging samples with validation status.
 * Implements US7 requirement: flag invalid or inconclusive results for review.
 *
 * @param {Object} props
 * @param {boolean} props.open - Whether the modal is open
 * @param {function} props.onClose - Callback when modal is closed
 * @param {number} props.pageId - The notebook page ID
 * @param {Array} props.selectedSamples - Array of sample IDs to flag
 * @param {function} props.onFlagSuccess - Callback when flagging is successful
 */
function FlagSampleModal({
  open,
  onClose,
  pageId,
  selectedSamples,
  onFlagSuccess,
}) {
  const intl = useIntl();

  // Validation status options
  const VALIDATION_STATUSES = [
    {
      value: "VALID",
      label: intl.formatMessage({
        id: "notebook.validation.status.valid",
        defaultMessage: "Valid",
      }),
      color: "green",
      requiresReason: false,
    },
    {
      value: "INVALID",
      label: intl.formatMessage({
        id: "notebook.validation.status.invalid",
        defaultMessage: "Invalid",
      }),
      color: "red",
      requiresReason: true,
    },
    {
      value: "INCONCLUSIVE",
      label: intl.formatMessage({
        id: "notebook.validation.status.inconclusive",
        defaultMessage: "Inconclusive",
      }),
      color: "yellow",
      requiresReason: true,
    },
  ];

  const [selectedStatus, setSelectedStatus] = useState("");
  const [reason, setReason] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);

  // Reset form when modal opens
  useEffect(() => {
    if (open) {
      setSelectedStatus("");
      setReason("");
      setError(null);
    }
  }, [open]);

  const currentStatusConfig = VALIDATION_STATUSES.find(
    (s) => s.value === selectedStatus,
  );
  const reasonRequired = currentStatusConfig?.requiresReason || false;
  const isValid =
    selectedStatus && (!reasonRequired || (reasonRequired && reason.trim()));

  const handleSubmit = async () => {
    if (!isValid) return;

    setIsSubmitting(true);
    setError(null);

    try {
      const isBulk = selectedSamples.length > 1;
      const endpoint = isBulk
        ? `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageId}/samples/bulk-flag`
        : `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageId}/samples/flag`;

      const body = isBulk
        ? {
            sampleIds: selectedSamples,
            status: selectedStatus,
            reason: reason.trim() || null,
          }
        : {
            sampleId: selectedSamples[0],
            status: selectedStatus,
            reason: reason.trim() || null,
          };

      const response = await fetch(endpoint, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
        credentials: "include",
        body: JSON.stringify(body),
      });

      const data = await response.json();

      if (response.ok && data.success) {
        if (onFlagSuccess) {
          onFlagSuccess({
            samples: selectedSamples,
            status: selectedStatus,
            reason: reason.trim(),
            flaggedCount: isBulk ? data.flaggedCount : 1,
          });
        }
        onClose();
      } else {
        setError(
          data.error ||
            intl.formatMessage({
              id: "notebook.validation.error.failed",
              defaultMessage: "Failed to flag sample(s)",
            }),
        );
      }
    } catch (err) {
      console.error("FlagSampleModal: Error flagging samples:", err);
      setError(
        intl.formatMessage({
          id: "notebook.validation.error.network",
          defaultMessage: "Network error. Please try again.",
        }),
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleClose = () => {
    if (!isSubmitting) {
      onClose();
    }
  };

  return (
    <Modal
      open={open}
      onRequestClose={handleClose}
      modalHeading={intl.formatMessage({
        id: "notebook.validation.modal.title",
        defaultMessage: "Flag Sample Validation Status",
      })}
      primaryButtonText={intl.formatMessage({
        id: "notebook.validation.modal.submit",
        defaultMessage: "Apply Status",
      })}
      secondaryButtonText={intl.formatMessage({
        id: "label.button.cancel",
        defaultMessage: "Cancel",
      })}
      onRequestSubmit={handleSubmit}
      primaryButtonDisabled={!isValid || isSubmitting}
      size="md"
    >
      <div className="flag-sample-modal">
        {/* Selected samples summary */}
        <div className="flag-sample-summary">
          <Tag type="blue">
            <FormattedMessage
              id="notebook.validation.modal.selectedCount"
              defaultMessage="{count} {count, plural, one {sample} other {samples}} selected"
              values={{ count: selectedSamples.length }}
            />
          </Tag>
        </div>

        {/* Instructions */}
        <p className="flag-sample-description">
          <FormattedMessage
            id="notebook.validation.modal.description"
            defaultMessage="Select a validation status for the selected sample(s). A reason is required for Invalid or Inconclusive statuses."
          />
        </p>

        {/* Validation status selector */}
        <div className="flag-sample-status-selector">
          <RadioButtonGroup
            legendText={intl.formatMessage({
              id: "notebook.validation.modal.statusLabel",
              defaultMessage: "Validation Status",
            })}
            name="validation-status"
            valueSelected={selectedStatus}
            onChange={(value) => setSelectedStatus(value)}
            orientation="vertical"
          >
            {VALIDATION_STATUSES.map((status) => (
              <RadioButton
                key={status.value}
                id={`status-${status.value}`}
                value={status.value}
                labelText={
                  <span className={`status-label status-label-${status.color}`}>
                    {status.label}
                    {status.requiresReason && (
                      <span className="reason-required-hint">
                        {" "}
                        (
                        <FormattedMessage
                          id="notebook.validation.reasonRequired"
                          defaultMessage="reason required"
                        />
                        )
                      </span>
                    )}
                  </span>
                }
              />
            ))}
          </RadioButtonGroup>
        </div>

        {/* Reason text field */}
        <div className="flag-sample-reason">
          <TextArea
            id="validation-reason"
            labelText={
              <>
                <FormattedMessage
                  id="notebook.validation.modal.reasonLabel"
                  defaultMessage="Reason"
                />
                {reasonRequired && <span className="required-marker">*</span>}
              </>
            }
            placeholder={intl.formatMessage({
              id: "notebook.validation.modal.reasonPlaceholder",
              defaultMessage: "Enter the reason for this validation status...",
            })}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            rows={3}
            maxCount={500}
            enableCounter
            invalid={reasonRequired && !reason.trim()}
            invalidText={intl.formatMessage({
              id: "notebook.validation.error.reasonRequired",
              defaultMessage: "Reason is required for this status",
            })}
          />
        </div>

        {/* Error notification */}
        {error && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({
              id: "label.error",
              defaultMessage: "Error",
            })}
            subtitle={error}
            hideCloseButton
            lowContrast
          />
        )}

        {/* Loading indicator */}
        {isSubmitting && (
          <Loading
            withOverlay={false}
            description={intl.formatMessage({
              id: "notebook.validation.modal.submitting",
              defaultMessage: "Applying validation status...",
            })}
          />
        )}
      </div>
    </Modal>
  );
}

export default FlagSampleModal;

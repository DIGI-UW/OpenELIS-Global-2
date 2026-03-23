import React, { useState, useMemo } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  TextInput,
  TextArea,
  NumberInput,
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { postToOpenElisServerFullResponse } from "../utils/Utils";

/**
 * CreateAliquotModal - Modal dialog for creating aliquots from parent sample items.
 *
 * Features:
 * - Carbon ComposedModal for consistent UX
 * - Support for creating multiple equal-volume aliquots
 * - Form validation for quantity and number of aliquots
 * - Live preview of quantity per aliquot
 * - API integration with error handling
 * - React Intl for internationalization
 *
 * Props:
 * - open: boolean - controls modal visibility
 * - onClose: () => void - callback when modal closes
 * - parentSample: SampleItemDTO - the parent sample to aliquot from
 * - onSuccess: (response) => void - callback when aliquot is created successfully
 *
 * Related: Feature 001-sample-management, User Story 3
 */
function CreateAliquotModal({ open, onClose, parentSample, onSuccess }) {
  const intl = useIntl();

  // Form state
  const [quantityToTransfer, setQuantityToTransfer] = useState("");
  const [numberOfAliquots, setNumberOfAliquots] = useState(1);
  const [notes, setNotes] = useState("");
  const [error, setError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  /**
   * Calculate quantity per aliquot based on total quantity and number of aliquots.
   */
  const quantityPerAliquot = useMemo(() => {
    const totalQty = parseFloat(quantityToTransfer);
    if (isNaN(totalQty) || totalQty <= 0 || numberOfAliquots < 1) {
      return null;
    }
    return (totalQty / numberOfAliquots).toFixed(3);
  }, [quantityToTransfer, numberOfAliquots]);

  /**
   * Validate form inputs
   */
  const validateForm = () => {
    // Validate quantity
    if (!quantityToTransfer || quantityToTransfer.trim() === "") {
      return intl.formatMessage({
        id: "sample.management.aliquot.error.quantityRequired",
      });
    }

    const quantity = parseFloat(quantityToTransfer);
    if (isNaN(quantity) || quantity <= 0) {
      return intl.formatMessage({
        id: "sample.management.aliquot.error.quantityPositive",
      });
    }

    if (
      parentSample.effectiveRemainingQuantity &&
      quantity > parentSample.effectiveRemainingQuantity
    ) {
      return intl.formatMessage(
        {
          id: "sample.management.aliquot.error.quantityExceedsRemaining",
        },
        {
          requested: quantity,
          remaining: parentSample.effectiveRemainingQuantity,
        },
      );
    }

    // Validate number of aliquots
    if (numberOfAliquots < 1 || numberOfAliquots > 100) {
      return intl.formatMessage({
        id: "sample.management.aliquot.error.numberOfAliquotsInvalid",
      });
    }

    // Validate quantity per aliquot is at least 0.001
    const qtyPerAliquot = quantity / numberOfAliquots;
    if (qtyPerAliquot < 0.001) {
      return intl.formatMessage(
        {
          id: "sample.management.aliquot.error.quantityPerAliquotTooSmall",
        },
        {
          quantityPerAliquot: qtyPerAliquot.toFixed(6),
        },
      );
    }

    return null;
  };

  /**
   * Handle form submission
   */
  const handleSubmit = () => {
    // Validate
    const validationError = validateForm();
    if (validationError) {
      setError(validationError);
      return;
    }

    setIsSubmitting(true);
    setError(null);

    const payload = JSON.stringify({
      parentSampleItemId: parentSample.id,
      quantityToTransfer: parseFloat(quantityToTransfer),
      numberOfAliquots: numberOfAliquots,
      notes: notes || null,
    });

    postToOpenElisServerFullResponse(
      "/rest/sample-management/aliquot",
      payload,
      (response) => {
        setIsSubmitting(false);

        if (response.ok) {
          response.json().then((result) => {
            // Reset form and call success callback
            setQuantityToTransfer("");
            setNumberOfAliquots(1);
            setNotes("");
            setError(null);
            onSuccess(result);
            onClose();
          });
        } else {
          response
            .json()
            .then((errorData) => {
              setError(
                errorData.message ||
                  intl.formatMessage({
                    id: "sample.management.aliquot.error.createFailed",
                  }),
              );
            })
            .catch(() => {
              setError(
                intl.formatMessage({
                  id: "sample.management.aliquot.error.createFailed",
                }),
              );
            });
        }
      },
    );
  };

  /**
   * Handle modal close
   */
  const handleClose = () => {
    if (!isSubmitting) {
      setQuantityToTransfer("");
      setNumberOfAliquots(1);
      setNotes("");
      setError(null);
      onClose();
    }
  };

  return (
    <ComposedModal open={open} onClose={handleClose} size="sm">
      <ModalHeader
        title={intl.formatMessage({
          id: "sample.management.aliquot.modal.title",
        })}
        label={intl.formatMessage(
          {
            id: "sample.management.aliquot.modal.subtitle",
          },
          { externalId: parentSample?.externalId || "" },
        )}
      />

      <ModalBody>
        {error && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({
              id: "sample.management.aliquot.error.title",
            })}
            subtitle={error}
            lowContrast
            hideCloseButton
            style={{ marginBottom: "1rem" }}
          />
        )}

        {/* Parent Sample Info */}
        <div
          style={{
            marginBottom: "1.5rem",
            padding: "1rem",
            backgroundColor: "#f4f4f4",
            borderRadius: "4px",
          }}
        >
          <div style={{ marginBottom: "0.5rem" }}>
            <strong>
              <FormattedMessage id="sample.management.aliquot.modal.parentInfo" />
            </strong>
          </div>
          <div>
            <FormattedMessage id="sample.management.table.header.externalId" />:{" "}
            {parentSample?.externalId}
          </div>
          <div>
            <FormattedMessage id="sample.management.table.header.sampleType" />:{" "}
            {parentSample?.sampleType}
          </div>
          <div>
            <FormattedMessage id="sample.management.table.header.remainingQuantity" />
            : {parentSample?.effectiveRemainingQuantity}{" "}
            {parentSample?.unitOfMeasure}
          </div>
        </div>

        {/* Total Quantity Input */}
        <TextInput
          id="quantity-to-transfer"
          labelText={intl.formatMessage({
            id: "sample.management.aliquot.modal.totalQuantityLabel",
          })}
          placeholder={intl.formatMessage({
            id: "sample.management.aliquot.modal.quantityPlaceholder",
          })}
          value={quantityToTransfer}
          onChange={(e) => {
            setQuantityToTransfer(e.target.value);
            setError(null);
          }}
          disabled={isSubmitting}
          invalid={false}
          type="number"
          step="0.001"
          min="0.001"
          helperText={intl.formatMessage(
            {
              id: "sample.management.aliquot.modal.totalQuantityHelper",
            },
            { unit: parentSample?.unitOfMeasure || "" },
          )}
        />

        {/* Number of Aliquots Input */}
        <NumberInput
          id="number-of-aliquots"
          label={intl.formatMessage({
            id: "sample.management.aliquot.modal.numberOfAliquotsLabel",
          })}
          value={numberOfAliquots}
          onChange={(e, { value }) => {
            setNumberOfAliquots(value);
            setError(null);
          }}
          disabled={isSubmitting}
          min={1}
          max={100}
          step={1}
          helperText={intl.formatMessage({
            id: "sample.management.aliquot.modal.numberOfAliquotsHelper",
          })}
          style={{ marginTop: "1rem" }}
        />

        {/* Quantity Per Aliquot Preview */}
        {quantityPerAliquot && numberOfAliquots > 1 && (
          <div
            style={{
              marginTop: "1rem",
              padding: "0.75rem",
              backgroundColor: "#e0f0ff",
              borderRadius: "4px",
              border: "1px solid #0f62fe",
            }}
          >
            <strong>
              <FormattedMessage id="sample.management.aliquot.modal.preview" />:
            </strong>{" "}
            <FormattedMessage
              id="sample.management.aliquot.modal.previewText"
              values={{
                count: numberOfAliquots,
                quantityEach: quantityPerAliquot,
                unit: parentSample?.unitOfMeasure || "",
              }}
            />
          </div>
        )}

        {/* Notes Input */}
        <TextArea
          id="notes"
          labelText={intl.formatMessage({
            id: "sample.management.aliquot.modal.notesLabel",
          })}
          placeholder={intl.formatMessage({
            id: "sample.management.aliquot.modal.notesPlaceholder",
          })}
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          disabled={isSubmitting}
          rows={3}
          maxLength={1000}
          style={{ marginTop: "1rem" }}
        />
      </ModalBody>

      <ModalFooter>
        <Button kind="secondary" onClick={handleClose} disabled={isSubmitting}>
          <FormattedMessage id="sample.management.aliquot.modal.cancel" />
        </Button>
        <Button kind="primary" onClick={handleSubmit} disabled={isSubmitting}>
          {isSubmitting ? (
            <FormattedMessage id="sample.management.aliquot.modal.creating" />
          ) : numberOfAliquots > 1 ? (
            <FormattedMessage
              id="sample.management.aliquot.modal.createMultiple"
              values={{ count: numberOfAliquots }}
            />
          ) : (
            <FormattedMessage id="sample.management.aliquot.modal.create" />
          )}
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
}

export default CreateAliquotModal;

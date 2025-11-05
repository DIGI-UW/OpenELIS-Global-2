import React, { useState } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  Dropdown,
  TextArea,
  Checkbox,
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { Location } from "@carbon/icons-react";
import "./DisposeSampleModal.css";

/**
 * Modal for disposing a sample
 * Per Figma design: modal title "Dispose Sample" with subtitle, red warning alert at top,
 * sample info section, current location section with pin icon, disposal instructions info box,
 * required Reason and Method dropdowns, optional Notes textarea, confirmation checkbox,
 * Cancel and "Confirm Disposal" button (red/destructive, disabled until checkbox checked)
 *
 * Props:
 * - open: boolean - Whether modal is open
 * - sample: object - { id, sampleId, type, status }
 * - currentLocation: object - { path } or null
 * - onClose: function - Callback when modal closes
 * - onConfirm: function - Callback when disposal is confirmed with { reason, method, notes }
 */
const DisposeSampleModal = ({
  open,
  sample,
  currentLocation,
  onClose,
  onConfirm,
}) => {
  const intl = useIntl();
  const [reason, setReason] = useState("");
  const [method, setMethod] = useState("");
  const [notes, setNotes] = useState("");
  const [confirmed, setConfirmed] = useState(false);

  const disposalReasons = [
    { id: "expired", label: "Expired" },
    { id: "contaminated", label: "Contaminated" },
    { id: "patient_request", label: "Patient Request" },
    { id: "testing_complete", label: "Testing Complete" },
    { id: "other", label: "Other" },
  ];

  const disposalMethods = [
    { id: "autoclave", label: "Biohazard Autoclave" },
    { id: "neutralization", label: "Chemical Neutralization" },
    { id: "incineration", label: "Incineration" },
    { id: "other", label: "Other" },
  ];

  const handleConfirm = () => {
    if (reason && method && confirmed && onConfirm) {
      onConfirm({
        sample,
        reason,
        method,
        notes,
      });
    }
    handleClose();
  };

  const handleClose = () => {
    setReason("");
    setMethod("");
    setNotes("");
    setConfirmed(false);
    onClose();
  };

  const canConfirm = reason && method && confirmed;

  return (
    <ComposedModal
      open={open}
      onClose={handleClose}
      size="lg"
      data-testid="dispose-modal"
    >
      <ModalHeader
        title={intl.formatMessage({
          id: "storage.dispose.sample",
          defaultMessage: "Dispose Sample",
        })}
        subtitle={intl.formatMessage(
          {
            id: "storage.dispose.sample.subtitle",
            defaultMessage: "Permanently dispose of sample {sampleId}",
          },
          { sampleId: sample?.sampleId || "" },
        )}
      />
      <ModalBody>
        {/* Red Warning Alert */}
        <div className="dispose-modal-alert" data-testid="warning-alert">
          <InlineNotification
            kind="error"
            title={intl.formatMessage({
              id: "storage.dispose.warning.title",
              defaultMessage: "This action cannot be undone",
            })}
            subtitle={intl.formatMessage({
              id: "storage.dispose.warning.subtitle",
              defaultMessage:
                "The sample will be marked as disposed and removed from storage.",
            })}
            lowContrast={false}
          />
        </div>

        {/* Sample Information Section */}
        <div
          className="dispose-modal-sample-info"
          data-testid="sample-info-section"
        >
          <div className="info-box">
            <div className="info-row">
              <span className="info-label">
                <FormattedMessage id="sample.id" defaultMessage="Sample ID" />:
              </span>
              <span className="info-value">{sample?.sampleId}</span>
            </div>
            <div className="info-row">
              <span className="info-label">
                <FormattedMessage id="sample.type" defaultMessage="Type" />:
              </span>
              <span className="info-value">{sample?.type}</span>
            </div>
            <div className="info-row">
              <span className="info-label">
                <FormattedMessage id="storage.status" defaultMessage="Status" />
                :
              </span>
              <span className="info-value">{sample?.status}</span>
            </div>
          </div>
        </div>

        {/* Current Location Section */}
        {currentLocation && (
          <div
            className="dispose-modal-current-location"
            data-testid="current-location-section"
          >
            <div className="location-header">
              <Location size={16} />
              <span className="location-label">
                <FormattedMessage
                  id="storage.current.location"
                  defaultMessage="Current Storage Location"
                />
              </span>
            </div>
            <div className="location-box">
              <div className="location-path">{currentLocation.path}</div>
            </div>
            <div className="location-note">
              <FormattedMessage
                id="storage.dispose.location.note"
                defaultMessage="Sample will be removed from this location upon disposal"
              />
            </div>
          </div>
        )}

        {/* Disposal Instructions Info Box */}
        <div
          className="dispose-modal-instructions"
          data-testid="disposal-instructions"
        >
          <InlineNotification
            kind="info"
            title={intl.formatMessage({
              id: "storage.dispose.instructions.title",
              defaultMessage: "Disposal Instructions",
            })}
            subtitle={intl.formatMessage({
              id: "storage.dispose.instructions.subtitle",
              defaultMessage:
                "Please ensure proper disposal procedures are followed according to laboratory safety protocols.",
            })}
            lowContrast={true}
          />
        </div>

        {/* Separator */}
        <div className="dispose-modal-separator" />

        {/* Required Fields */}
        <div className="dispose-modal-fields">
          <div className="form-group">
            <Dropdown
              id="disposal-reason"
              titleText={intl.formatMessage({
                id: "storage.disposal.reason",
                defaultMessage: "Disposal Reason",
              })}
              label={intl.formatMessage({
                id: "storage.disposal.reason",
                defaultMessage: "Disposal Reason",
              })}
              items={disposalReasons}
              itemToString={(item) => (item ? item.label : "")}
              onChange={({ selectedItem }) =>
                setReason(selectedItem ? selectedItem.id : "")
              }
              selectedItem={
                disposalReasons.find((r) => r.id === reason) || null
              }
              placeholder={intl.formatMessage({
                id: "storage.disposal.reason.placeholder",
                defaultMessage: "Select reason...",
              })}
              required
            />
          </div>

          <div className="form-group">
            <Dropdown
              id="disposal-method"
              titleText={intl.formatMessage({
                id: "storage.disposal.method",
                defaultMessage: "Disposal Method",
              })}
              label={intl.formatMessage({
                id: "storage.disposal.method",
                defaultMessage: "Disposal Method",
              })}
              items={disposalMethods}
              itemToString={(item) => (item ? item.label : "")}
              onChange={({ selectedItem }) =>
                setMethod(selectedItem ? selectedItem.id : "")
              }
              selectedItem={
                disposalMethods.find((m) => m.id === method) || null
              }
              placeholder={intl.formatMessage({
                id: "storage.disposal.method.placeholder",
                defaultMessage: "Select method...",
              })}
              required
            />
          </div>

          <div className="form-group">
            <TextArea
              id="disposal-notes"
              labelText={intl.formatMessage({
                id: "storage.disposal.notes",
                defaultMessage: "Additional Notes (optional)",
              })}
              placeholder={intl.formatMessage({
                id: "storage.disposal.notes.placeholder",
                defaultMessage: "Enter any additional notes...",
              })}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={3}
            />
          </div>

          {/* Confirmation Checkbox */}
          <div className="form-group">
            <Checkbox
              id="disposal-confirmation"
              labelText={intl.formatMessage({
                id: "storage.dispose.confirmation",
                defaultMessage:
                  "I confirm that I want to permanently dispose of this sample. This action cannot be undone.",
              })}
              checked={confirmed}
              onChange={(_, { checked }) => setConfirmed(checked)}
            />
          </div>
        </div>
      </ModalBody>
      <ModalFooter>
        <Button kind="secondary" onClick={handleClose}>
          <FormattedMessage id="label.button.cancel" defaultMessage="Cancel" />
        </Button>
        <Button kind="danger" onClick={handleConfirm} disabled={!canConfirm}>
          <FormattedMessage
            id="storage.confirm.disposal"
            defaultMessage="Confirm Disposal"
          />
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default DisposeSampleModal;

import React, { useState, useEffect } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  TextInput,
  TextArea,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import CascadingDropdownMode from "../StorageLocationSelector/CascadingDropdownMode";
import "./ViewStorageModal.css";

/**
 * Modal for viewing and editing storage location assignment
 * Per Figma design: modal title "Storage Location Assignment", sample info section,
 * current location section in gray box, visual separator, full assignment form
 * (barcode scan input, Room/Device/Shelf/Rack/Position selectors, condition notes),
 * Cancel and "Assign Storage Location" buttons
 *
 * Props:
 * - open: boolean - Whether modal is open
 * - sample: object - { id, sampleId, type, status }
 * - currentLocation: object - { path, position } or null
 * - onClose: function - Callback when modal closes
 * - onSave: function - Callback when location is saved/assigned
 */
const ViewStorageModal = ({
  open,
  sample,
  currentLocation,
  onClose,
  onSave,
}) => {
  const intl = useIntl();
  const [selectedLocation, setSelectedLocation] = useState(null);
  const [positionCoordinate, setPositionCoordinate] = useState("");
  const [conditionNotes, setConditionNotes] = useState("");

  // Pre-populate position if current location exists
  useEffect(() => {
    if (currentLocation && currentLocation.position) {
      setPositionCoordinate(currentLocation.position.coordinate || "");
    }
  }, [currentLocation]);

  const handleLocationChange = (location) => {
    setSelectedLocation(location);
    // Update position coordinate if position is selected
    if (location && location.position) {
      setPositionCoordinate(location.position.coordinate || "");
    }
  };

  const handleSave = () => {
    if (selectedLocation && onSave) {
      onSave({
        ...selectedLocation,
        position: {
          ...selectedLocation.position,
          coordinate: positionCoordinate || selectedLocation.position?.coordinate,
        },
        conditionNotes,
      });
    }
    onClose();
  };

  const handleCancel = () => {
    setSelectedLocation(null);
    setPositionCoordinate("");
    setConditionNotes("");
    onClose();
  };

  const canSave = selectedLocation && selectedLocation.room;

  return (
    <ComposedModal open={open} onClose={handleCancel} size="lg" data-testid="view-storage-modal">
      <ModalHeader
        title={intl.formatMessage({
          id: "storage.location.assignment",
          defaultMessage: "Storage Location Assignment",
        })}
      />
      <ModalBody>
        {/* Sample Information Section */}
        {sample && (
          <div className="view-storage-sample-info" data-testid="sample-info-section">
            <div className="info-box">
              <div className="info-row">
                <span className="info-label">
                  <FormattedMessage id="sample.id" defaultMessage="Sample ID" />:
                </span>
                <span className="info-value">{sample.sampleId}</span>
              </div>
              <div className="info-row">
                <span className="info-label">
                  <FormattedMessage id="sample.type" defaultMessage="Type" />:
                </span>
                <span className="info-value">{sample.type}</span>
              </div>
              <div className="info-row">
                <span className="info-label">
                  <FormattedMessage id="storage.status" defaultMessage="Status" />:
                </span>
                <span className="info-value">{sample.status}</span>
              </div>
            </div>
          </div>
        )}

        {/* Current Location Section */}
        {currentLocation && (
          <div className="view-storage-current-location" data-testid="current-location-section">
            <div className="location-box">
              <div className="location-label">
                <FormattedMessage
                  id="storage.current.location"
                  defaultMessage="Current Location"
                />:
              </div>
              <div className="location-path">{currentLocation.path}</div>
            </div>
          </div>
        )}

        {/* Visual Separator */}
        {(sample || currentLocation) && <div className="view-storage-separator" />}

        {/* Full Location Assignment Form */}
        <div className="view-storage-assignment-form" data-testid="assignment-form-section">
          <div className="form-group">
            <label className="form-label">
              <FormattedMessage id="storage.barcode.scan" defaultMessage="Quick Assign (Barcode)" />
            </label>
            <TextInput
              id="barcode-scan"
              placeholder={intl.formatMessage({
                id: "storage.barcode.placeholder",
                defaultMessage: "Scan or enter barcode...",
              })}
              disabled // Barcode scanning deferred to later stage
            />
          </div>

          <CascadingDropdownMode
            onLocationChange={handleLocationChange}
            enableInlineCreation={false}
          />

          <div className="form-group">
            <label className="form-label">
              <FormattedMessage id="storage.position.label" defaultMessage="Position" />{" "}
              <span className="optional-text">
                (<FormattedMessage id="label.optional" defaultMessage="optional" />)
              </span>
            </label>
            <TextInput
              id="position-input"
              labelText=""
              value={positionCoordinate}
              onChange={(e) => setPositionCoordinate(e.target.value)}
              placeholder={intl.formatMessage({
                id: "storage.position.placeholder",
                defaultMessage: "e.g., A5, 1-1, RED-12",
              })}
            />
          </div>

          <div className="form-group">
            <label className="form-label">
              <FormattedMessage
                id="storage.condition.notes"
                defaultMessage="Condition Notes"
              />{" "}
              <span className="optional-text">
                (<FormattedMessage id="label.optional" defaultMessage="optional" />)
              </span>
            </label>
            <TextArea
              id="condition-notes"
              labelText=""
              value={conditionNotes}
              onChange={(e) => setConditionNotes(e.target.value)}
              placeholder={intl.formatMessage({
                id: "storage.condition.notes.placeholder",
                defaultMessage: "Enter any condition notes...",
              })}
              rows={3}
            />
          </div>
        </div>
      </ModalBody>
      <ModalFooter>
        <Button kind="secondary" onClick={handleCancel}>
          <FormattedMessage id="label.button.cancel" defaultMessage="Cancel" />
        </Button>
        <Button kind="primary" onClick={handleSave} disabled={!canSave}>
          <FormattedMessage
            id="storage.assign.location"
            defaultMessage="Assign Storage Location"
          />
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default ViewStorageModal;


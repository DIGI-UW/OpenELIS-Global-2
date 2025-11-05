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
import LocationFilterDropdown from "../StorageDashboard/LocationFilterDropdown";
import "./LocationSelectorModal.css";

/**
 * Expanded modal view for storage location assignment
 * Matches View Storage modal structure: sample info box, current location display, full assignment form
 *
 * Props:
 * - open: boolean - Whether modal is open
 * - sampleInfo: object - { sampleId, type, status }
 * - currentLocation: object - { path, position } or null
 * - onClose: function - Callback when modal closes
 * - onSave: function - Callback when location is saved/assigned
 */
const LocationSelectorModal = ({
  open,
  sampleInfo,
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
    // LocationFilterDropdown returns { id, type, name, ... } format
    // We need to convert it to the expected format with room/device/shelf/rack/position
    if (location && location.id) {
      // If it's a location from the filter dropdown, we need to fetch full hierarchy
      // For now, store it as-is and let the backend handle the assignment
      setSelectedLocation({
        id: location.id,
        type: location.type,
        name: location.name || location.label,
        ...location,
      });
    } else {
      setSelectedLocation(location);
      // Update position coordinate if position is selected
      if (location && location.position) {
        setPositionCoordinate(location.position.coordinate || "");
      }
    }
  };

  const handleSave = () => {
    if (selectedLocation && onSave) {
      onSave({
        ...selectedLocation,
        position: {
          ...selectedLocation.position,
          coordinate:
            positionCoordinate || selectedLocation.position?.coordinate,
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
    <ComposedModal open={open} onClose={handleCancel} size="lg">
      <ModalHeader
        title={intl.formatMessage({
          id: "storage.location.assignment",
          defaultMessage: "Storage Location Assignment",
        })}
      />
      <ModalBody>
        {/* Sample Information Section */}
        {sampleInfo && (
          <div
            className="sample-info-section"
            data-testid="sample-info-section"
          >
            <div className="info-box">
              <div className="info-row">
                <span className="info-label">
                  <FormattedMessage id="sample.id" defaultMessage="Sample ID" />
                  :
                </span>
                <span className="info-value">{sampleInfo.sampleId}</span>
              </div>
              <div className="info-row">
                <span className="info-label">
                  <FormattedMessage id="sample.type" defaultMessage="Type" />:
                </span>
                <span className="info-value">{sampleInfo.type}</span>
              </div>
              <div className="info-row">
                <span className="info-label">
                  <FormattedMessage
                    id="storage.status"
                    defaultMessage="Status"
                  />
                  :
                </span>
                <span className="info-value">{sampleInfo.status}</span>
              </div>
            </div>
          </div>
        )}

        {/* Current Location Section */}
        {currentLocation && (
          <div
            className="current-location-section"
            data-testid="current-location-section"
          >
            <div className="location-box">
              <div className="location-label">
                <FormattedMessage
                  id="storage.current.location"
                  defaultMessage="Current Location"
                />
                :
              </div>
              <div className="location-path">{currentLocation.path}</div>
            </div>
          </div>
        )}

        {/* Visual Separator */}
        {(sampleInfo || currentLocation) && <div className="modal-separator" />}

        {/* Full Location Assignment Form */}
        <div
          className="assignment-form-section"
          data-testid="assignment-form-section"
        >
          <div className="form-group">
            <label className="form-label">
              <FormattedMessage
                id="storage.barcode.scan"
                defaultMessage="Quick Assign (Barcode)"
              />
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

          <div className="form-group">
            <label className="form-label">
              <FormattedMessage
                id="storage.select.location"
                defaultMessage="Select Location"
              />{" "}
              <span className="required-indicator">*</span>
            </label>
            <LocationFilterDropdown
              onLocationChange={handleLocationChange}
              selectedLocation={selectedLocation}
              allowInactive={false}
            />
          </div>

          <div className="form-group">
            <label className="form-label">
              <FormattedMessage
                id="storage.position.label"
                defaultMessage="Position"
              />{" "}
              <span className="optional-text">
                (
                <FormattedMessage
                  id="label.optional"
                  defaultMessage="optional"
                />
                )
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
                (
                <FormattedMessage
                  id="label.optional"
                  defaultMessage="optional"
                />
                )
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

export default LocationSelectorModal;

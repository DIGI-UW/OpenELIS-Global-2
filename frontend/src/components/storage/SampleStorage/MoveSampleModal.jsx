import React, { useState } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  TextArea,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { ArrowDown } from "@carbon/icons-react";
import CascadingDropdownMode from "../StorageLocationSelector/CascadingDropdownMode";
import "./MoveSampleModal.css";

/**
 * Modal for moving a sample to a new storage location
 * Per Figma design: modal title "Move Sample" with subtitle, current location in gray box,
 * downward arrow icon, new location selector in bordered box, "Selected Location" preview box,
 * optional reason textarea, Cancel and "Confirm Move" buttons
 *
 * Props:
 * - open: boolean - Whether modal is open
 * - sample: object - { id, sampleId, type, status }
 * - currentLocation: object - { path, position } or null
 * - onClose: function - Callback when modal closes
 * - onConfirm: function - Callback when move is confirmed with { newLocation, reason }
 */
const MoveSampleModal = ({
  open,
  sample,
  currentLocation,
  onClose,
  onConfirm,
}) => {
  const intl = useIntl();
  const [selectedLocation, setSelectedLocation] = useState(null);
  const [selectedLocationPath, setSelectedLocationPath] = useState("");
  const [reason, setReason] = useState("");

  const handleLocationChange = (location) => {
    setSelectedLocation(location);
    // Build hierarchical path
    if (location && location.position) {
      const path = `${location.room?.name} > ${location.device?.name} > ${location.shelf?.label} > ${location.rack?.label} > Position ${location.position.coordinate}`;
      setSelectedLocationPath(path);
    } else if (location && location.rack) {
      const path = `${location.room?.name} > ${location.device?.name} > ${location.shelf?.label} > ${location.rack?.label}`;
      setSelectedLocationPath(path);
    } else {
      setSelectedLocationPath("");
    }
  };

  const handleConfirm = () => {
    if (selectedLocation && onConfirm) {
      onConfirm({
        sample,
        newLocation: selectedLocation,
        reason,
      });
    }
    handleClose();
  };

  const handleClose = () => {
    setSelectedLocation(null);
    setSelectedLocationPath("");
    setReason("");
    onClose();
  };

  const canConfirm = selectedLocation && selectedLocation.room;

  return (
    <ComposedModal open={open} onClose={handleClose} size="lg" data-testid="move-modal">
      <ModalHeader
        title={intl.formatMessage({
          id: "storage.move.sample",
          defaultMessage: "Move Sample",
        })}
        subtitle={intl.formatMessage(
          {
            id: "storage.move.sample.subtitle",
            defaultMessage: "Move sample {sampleId} to a new storage location",
          },
          { sampleId: sample?.sampleId || "" },
        )}
      />
      <ModalBody>
        {/* Current Location Section */}
        {currentLocation && (
          <div className="move-modal-current-location" data-testid="current-location-section">
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

        {/* Downward Arrow Icon */}
        {currentLocation && (
          <div className="move-modal-arrow">
            <ArrowDown size={24} />
          </div>
        )}

        {/* New Location Selector */}
        <div className="move-modal-new-location" data-testid="new-location-section">
          <div className="location-selector-box">
            <label className="form-label">
              <FormattedMessage
                id="storage.new.location"
                defaultMessage="New Location"
              />{" "}
              <span className="required-indicator">*</span>
            </label>
            <CascadingDropdownMode
              onLocationChange={handleLocationChange}
              enableInlineCreation={false}
            />
          </div>
        </div>

        {/* Selected Location Preview */}
        <div className="move-modal-selected-preview" data-testid="selected-location-preview">
          <div className="preview-box">
            <div className="preview-label">
              <FormattedMessage
                id="storage.selected.location"
                defaultMessage="Selected Location"
              />:
            </div>
            <div className="preview-value">
              {selectedLocationPath || (
                <span className="not-selected">
                  <FormattedMessage
                    id="storage.not.selected"
                    defaultMessage="Not selected"
                  />
                </span>
              )}
            </div>
          </div>
        </div>

        {/* Reason Textarea */}
        <div className="move-modal-reason">
          <TextArea
            id="move-reason"
            labelText={intl.formatMessage({
              id: "storage.move.reason",
              defaultMessage: "Reason for Move",
            })}
            placeholder={intl.formatMessage({
              id: "storage.move.reason.placeholder",
              defaultMessage: "Optional: Enter reason for moving this sample...",
            })}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            rows={3}
          />
        </div>
      </ModalBody>
      <ModalFooter>
        <Button kind="secondary" onClick={handleClose}>
          <FormattedMessage id="label.button.cancel" defaultMessage="Cancel" />
        </Button>
        <Button kind="primary" onClick={handleConfirm} disabled={!canConfirm}>
          <FormattedMessage
            id="storage.confirm.move"
            defaultMessage="Confirm Move"
          />
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default MoveSampleModal;


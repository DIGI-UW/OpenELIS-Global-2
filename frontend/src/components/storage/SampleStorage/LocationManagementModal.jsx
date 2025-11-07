import React, { useState, useRef, useCallback, useEffect } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  TextArea,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { ArrowDown } from "@carbon/icons-react";
import LocationSearchAndCreate from "../StorageLocationSelector/LocationSearchAndCreate";
import "./LocationManagementModal.css";

/**
 * Consolidated modal for managing sample storage location (assignment and movement)
 * Handles both initial assignment (no location) and movement (existing location) workflows
 *
 * Props:
 * - open: boolean - Whether modal is open
 * - sample: object - { id, sampleId, type, status, dateCollected?, patientId?, testOrders? }
 * - currentLocation: object - { path, position } or null
 * - onClose: function - Callback when modal closes
 * - onConfirm: function - Callback when location is confirmed with { sample, newLocation, reason?, conditionNotes? }
 */
const LocationManagementModal = ({
  open,
  sample,
  currentLocation,
  onClose,
  onConfirm,
}) => {
  const intl = useIntl();
  const [selectedLocation, setSelectedLocation] = useState(null);
  const selectedLocationRef = useRef(null);
  const [selectedLocationPath, setSelectedLocationPath] = useState("");
  const [reason, setReason] = useState("");
  const [conditionNotes, setConditionNotes] = useState("");
  const [positionCoordinate, setPositionCoordinate] = useState("");
  const [locationUpdateTrigger, setLocationUpdateTrigger] = useState(0);

  // Determine modal mode: assignment (no location) or movement (location exists)
  const isMovementMode = !!currentLocation;

  // Pre-populate position if current location exists
  useEffect(() => {
    if (currentLocation && currentLocation.position) {
      setPositionCoordinate(currentLocation.position.coordinate || "");
    }
  }, [currentLocation]);

  // Reset form when modal closes
  useEffect(() => {
    if (!open) {
      setSelectedLocation(null);
      selectedLocationRef.current = null;
      setSelectedLocationPath("");
      setReason("");
      setConditionNotes("");
      setPositionCoordinate("");
      setLocationUpdateTrigger(0);
    }
  }, [open]);

  const handleLocationChange = useCallback((location) => {
    if (process.env.NODE_ENV === "development") {
      console.log(
        "[LocationManagementModal] handleLocationChange called with:",
        JSON.stringify(
          {
            location: location
              ? {
                  room: location.room
                    ? { id: location.room.id, name: location.room.name }
                    : null,
                  device: location.device
                    ? { id: location.device.id, name: location.device.name }
                    : null,
                  shelf: location.shelf
                    ? { id: location.shelf.id, label: location.shelf.label }
                    : null,
                  rack: location.rack
                    ? { id: location.rack.id, label: location.rack.label }
                    : null,
                  type: location.type,
                  id: location.id,
                  hierarchicalPath: location.hierarchicalPath,
                  hierarchical_path: location.hierarchical_path,
                }
              : null,
            locationIsTruthy: !!location,
          },
          null,
          2,
        ),
      );
    }

    if (location) {
      selectedLocationRef.current = location;

      let path = "";
      const hierarchicalPath =
        location.hierarchical_path || location.hierarchicalPath;
      if (hierarchicalPath && hierarchicalPath.trim()) {
        path = hierarchicalPath.trim();
      } else {
        const roomName = location.room?.name || location.room?.code || "";
        const deviceName = location.device?.name || location.device?.code || "";
        const shelfLabel = location.shelf?.label || location.shelf?.name || "";
        const rackLabel = location.rack?.label || location.rack?.name || "";
        const positionCoord =
          location.position?.coordinate || location.position || "";

        const pathParts = [];
        if (roomName) pathParts.push(roomName);
        if (deviceName) pathParts.push(deviceName);
        if (shelfLabel) pathParts.push(shelfLabel);
        if (rackLabel) pathParts.push(rackLabel);
        if (positionCoord) pathParts.push(`Position ${positionCoord}`);

        path = pathParts.join(" > ");

        if (!path && location.name) {
          path = location.name;
        }
      }

      setSelectedLocation(location);
      setSelectedLocationPath(path);
      setLocationUpdateTrigger((prev) => prev + 1);
    } else {
      selectedLocationRef.current = null;
      setSelectedLocation(null);
      setSelectedLocationPath("");
      setLocationUpdateTrigger((prev) => prev + 1);
    }
  }, []);

  const handleConfirm = async () => {
    const locationToUse = selectedLocation || selectedLocationRef.current;

    if (!locationToUse) {
      console.error("[LocationManagementModal] handleConfirm: No location selected");
      return;
    }

    if (!onConfirm) {
      console.error("[LocationManagementModal] handleConfirm: onConfirm callback not provided");
      return;
    }

    try {
      console.log("[LocationManagementModal] handleConfirm: Calling onConfirm with:", {
        sample: sample?.id || sample?.sampleId,
        hasNewLocation: !!locationToUse,
        reason: isMovementMode ? reason : undefined,
        positionCoordinate: positionCoordinate || undefined,
      });

      // Ensure onConfirm returns a promise
      const result = onConfirm({
        sample,
        newLocation: locationToUse,
        reason: isMovementMode ? reason : undefined,
        conditionNotes: conditionNotes || undefined,
        positionCoordinate: positionCoordinate || undefined,
      });

      // If onConfirm returns a promise, await it
      if (result && typeof result.then === 'function') {
        await result;
      }

      console.log("[LocationManagementModal] handleConfirm: onConfirm completed successfully, closing modal");
      handleClose();
    } catch (error) {
      console.error("[LocationManagementModal] handleConfirm: Error occurred:", error);
      // Don't close modal on error - let user see the error notification
      // Error is already handled and displayed by the parent component
    }
  };

  const handleClose = () => {
    setSelectedLocation(null);
    selectedLocationRef.current = null;
    setSelectedLocationPath("");
    setReason("");
    setConditionNotes("");
    setPositionCoordinate("");
    setLocationUpdateTrigger(0);
    onClose();
  };

  const selectedLocationForValidation =
    selectedLocationRef.current || selectedLocation;

  // Validation logic for location selection
  const hasRoom = !!(
    selectedLocationForValidation?.room &&
    (selectedLocationForValidation.room.id ||
      selectedLocationForValidation.room.name ||
      selectedLocationForValidation.room)
  );
  const hasDevice = !!(
    selectedLocationForValidation?.device &&
    (selectedLocationForValidation.device.id ||
      selectedLocationForValidation.device.name ||
      selectedLocationForValidation.device)
  );
  const hasShelf = !!(
    selectedLocationForValidation?.shelf &&
    (selectedLocationForValidation.shelf.id ||
      selectedLocationForValidation.shelf.label ||
      selectedLocationForValidation.shelf.name ||
      selectedLocationForValidation.shelf)
  );
  const hasRack = !!(
    selectedLocationForValidation?.rack &&
    (selectedLocationForValidation.rack.id ||
      selectedLocationForValidation.rack.label ||
      selectedLocationForValidation.rack.name ||
      selectedLocationForValidation.rack)
  );

  const hasLocationId = !!selectedLocationForValidation?.id;
  const hasLocationType = !!(
    selectedLocationForValidation?.type &&
    (selectedLocationForValidation.type === "device" ||
      selectedLocationForValidation.type === "shelf" ||
      selectedLocationForValidation.type === "rack")
  );

  let canExtractLocationId = false;
  if (hasRack && selectedLocationForValidation.rack.id) {
    canExtractLocationId = true;
  } else if (hasShelf && selectedLocationForValidation.shelf.id) {
    canExtractLocationId = true;
  } else if (hasDevice && selectedLocationForValidation.device.id) {
    canExtractLocationId = true;
  } else if (hasLocationId && hasLocationType) {
    canExtractLocationId = true;
  }

  const meetsMinimumLevels = (hasRoom && hasDevice) || hasLocationId;
  const canConfirm = meetsMinimumLevels && canExtractLocationId;

  // Determine if Reason for Move should be visible
  // Show only when: location exists AND different location selected
  const showReasonForMove =
    isMovementMode &&
    selectedLocationForValidation &&
    selectedLocationPath &&
    selectedLocationPath !== currentLocation?.path;

  // Dynamic title and button text based on mode
  const modalTitle = isMovementMode
    ? intl.formatMessage({
        id: "storage.move.sample",
        defaultMessage: "Move Sample",
      })
    : intl.formatMessage({
        id: "storage.assign.location",
        defaultMessage: "Assign Storage Location",
      });

  const buttonText = isMovementMode
    ? intl.formatMessage({
        id: "storage.confirm.move",
        defaultMessage: "Confirm Move",
      })
    : intl.formatMessage({
        id: "storage.assign",
        defaultMessage: "Assign",
      });

  return (
    <ComposedModal
      open={open}
      onClose={handleClose}
      size="lg"
      data-testid="location-management-modal"
    >
      <ModalHeader
        title={modalTitle}
        subtitle={
          isMovementMode
            ? intl.formatMessage(
                {
                  id: "storage.move.sample.subtitle",
                  defaultMessage: "Move sample {sampleId} to a new storage location",
                },
                { sampleId: sample?.sampleId || "" },
              )
            : intl.formatMessage(
                {
                  id: "storage.assign.location.subtitle",
                  defaultMessage:
                    "Assign storage location for sample {sampleId}",
                },
                { sampleId: sample?.sampleId || "" },
              )
        }
      />
      <ModalBody>
        {/* Comprehensive Sample Information Section */}
        {sample && (
          <div
            className="location-management-sample-info"
            data-testid="sample-info-section"
          >
            <div className="info-box">
              <div className="info-row">
                <span className="info-label">
                  <FormattedMessage id="sample.id" defaultMessage="Sample ID" />
                  :
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
                  <FormattedMessage
                    id="storage.status"
                    defaultMessage="Status"
                  />
                  :
                </span>
                <span className="info-value">{sample.status}</span>
              </div>
              {sample.dateCollected && (
                <div className="info-row">
                  <span className="info-label">
                    <FormattedMessage
                      id="sample.date.collected"
                      defaultMessage="Date Collected"
                    />
                    :
                  </span>
                  <span className="info-value">{sample.dateCollected}</span>
                </div>
              )}
              {sample.patientId && (
                <div className="info-row">
                  <span className="info-label">
                    <FormattedMessage
                      id="patient.id"
                      defaultMessage="Patient ID"
                    />
                    :
                  </span>
                  <span className="info-value">{sample.patientId}</span>
                </div>
              )}
              {sample.testOrders && sample.testOrders.length > 0 && (
                <div className="info-row">
                  <span className="info-label">
                    <FormattedMessage
                      id="test.orders"
                      defaultMessage="Test Orders"
                    />
                    :
                  </span>
                  <span className="info-value">
                    {Array.isArray(sample.testOrders)
                      ? sample.testOrders.join(", ")
                      : sample.testOrders}
                  </span>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Visual Separator after sample info */}
        {sample && (
          <div className="location-management-separator" />
        )}

        {/* Location Flow Section - Current Location → Arrow → New Location Selector → Selected Location Preview */}
        <div className="location-management-location-flow">
          {/* Current Location Section - Only show if location exists */}
          {currentLocation && (
            <div
              className="location-management-current-location"
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

          {/* Downward Arrow Icon - Only show if current location exists */}
          {currentLocation && (
            <div className="location-management-arrow">
              <ArrowDown size={24} />
            </div>
          )}

          {/* New Location Selector */}
          <div
            className="location-management-new-location"
            data-testid="new-location-section"
          >
            <div className="location-selector-box">
              <label className="form-label">
                <FormattedMessage
                  id="storage.select.location"
                  defaultMessage="Select Location"
                />{" "}
                <span className="required-indicator">*</span>
              </label>
              <div className="location-management-location-selector">
                <LocationSearchAndCreate
                  onLocationChange={handleLocationChange}
                  selectedLocation={selectedLocationForValidation}
                  allowInactive={false}
                  showCreateButton={true}
                />
              </div>
            </div>
          </div>

          {/* Selected Location Preview - Show when location is selected */}
          {selectedLocationPath && (
            <div
              className="location-management-selected-preview"
              data-testid="selected-location-section"
            >
              <div className="location-box">
                <div className="location-label">
                  <FormattedMessage
                    id="storage.selected.location"
                    defaultMessage="Selected Location"
                  />
                  :
                </div>
                <div className="location-path">{selectedLocationPath}</div>
              </div>
            </div>
          )}
        </div>

        {/* Optional Fields Section - Position, Condition Notes, Reason for Move */}
        <div className="location-management-optional-fields">
          {/* Position Input */}
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

          {/* Condition Notes - Always visible */}
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

          {/* Reason for Move - Only show when moving to different location */}
          {showReasonForMove && (
            <div className="form-group">
              <label className="form-label">
                <FormattedMessage
                  id="storage.move.reason"
                  defaultMessage="Reason for Move"
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
                id="move-reason"
                labelText=""
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder={intl.formatMessage({
                  id: "storage.move.reason.placeholder",
                  defaultMessage:
                    "Optional: Enter reason for moving this sample...",
                })}
                rows={3}
              />
            </div>
          )}
        </div>
      </ModalBody>
      <ModalFooter>
        <Button kind="secondary" onClick={handleClose}>
          <FormattedMessage id="label.button.cancel" defaultMessage="Cancel" />
        </Button>
        <Button
          kind="primary"
          onClick={handleConfirm}
          disabled={!canConfirm}
          data-testid={isMovementMode ? "confirm-move-button" : "assign-button"}
        >
          {buttonText}
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default LocationManagementModal;


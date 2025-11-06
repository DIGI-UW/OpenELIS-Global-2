import React, { useState, useRef, useCallback } from "react";
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
import LocationSearchAndCreate from "../StorageLocationSelector/LocationSearchAndCreate";
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
  const selectedLocationRef = useRef(null); // Track location for immediate validation
  const [selectedLocationPath, setSelectedLocationPath] = useState("");
  const [reason, setReason] = useState("");
  const [locationUpdateTrigger, setLocationUpdateTrigger] = useState(0); // Force re-render when location changes

  const handleLocationChange = useCallback((location) => {
    // Debug logging
    if (process.env.NODE_ENV === 'development') {
      console.log('[MoveSampleModal] handleLocationChange called with:', JSON.stringify({
        location: location ? {
          room: location.room ? { id: location.room.id, name: location.room.name } : null,
          device: location.device ? { id: location.device.id, name: location.device.name } : null,
          shelf: location.shelf ? { id: location.shelf.id, label: location.shelf.label } : null,
          rack: location.rack ? { id: location.rack.id, label: location.rack.label } : null,
          type: location.type,
          id: location.id,
          hierarchicalPath: location.hierarchicalPath,
          hierarchical_path: location.hierarchical_path,
        } : null,
        locationIsTruthy: !!location
      }, null, 2));
    }
    
    // CRITICAL: Update ref FIRST synchronously - this is immediately available for validation
    if (location) {
      selectedLocationRef.current = location;
      
      // Build hierarchical path synchronously
      // CRITICAL: Use hierarchical_path/hierarchicalPath from API first (most reliable)
      // API returns hierarchicalPath (camelCase), we normalize to hierarchical_path (snake_case)
      let path = "";
      const hierarchicalPath = location.hierarchical_path || location.hierarchicalPath;
      if (hierarchicalPath && hierarchicalPath.trim()) {
        // API provides full hierarchical path - use it directly
        path = hierarchicalPath.trim();
        if (process.env.NODE_ENV === 'development') {
          console.log('[MoveSampleModal] Using hierarchicalPath from API:', path);
        }
      } else {
        // Build path from location components
        // Extract names/labels - handle both ID-only objects and full objects with names
        const roomName = location.room?.name || location.room?.code || "";
        const deviceName = location.device?.name || location.device?.code || "";
        const shelfLabel = location.shelf?.label || location.shelf?.name || "";
        const rackLabel = location.rack?.label || location.rack?.name || "";
        const positionCoord = location.position?.coordinate || location.position || "";
        
        if (process.env.NODE_ENV === 'development') {
          console.log('[MoveSampleModal] Building path from components:', {
            roomName, deviceName, shelfLabel, rackLabel, positionCoord,
            hasRoom: !!location.room, hasDevice: !!location.device, 
            hasShelf: !!location.shelf, hasRack: !!location.rack, hasPosition: !!location.position
          });
        }
        
        // Build path components, filtering out empty parts
        const pathParts = [];
        if (roomName) pathParts.push(roomName);
        if (deviceName) pathParts.push(deviceName);
        if (shelfLabel) pathParts.push(shelfLabel);
        if (rackLabel) pathParts.push(rackLabel);
        if (positionCoord) pathParts.push(`Position ${positionCoord}`);
        
        path = pathParts.join(" > ");
        
        // If we still don't have a path but have a name, use it
        if (!path && location.name) {
          path = location.name;
        }
        
        if (process.env.NODE_ENV === 'development') {
          console.log('[MoveSampleModal] Built path from components:', path);
        }
      }
      
      // Update state synchronously - trigger re-render to update UI
      setSelectedLocation(location);
      setSelectedLocationPath(path);
      setLocationUpdateTrigger(prev => prev + 1); // Force re-render for immediate validation
      
      if (process.env.NODE_ENV === 'development') {
        console.log('[MoveSampleModal] Synchronously updated ref and state:', {
          refSet: !!selectedLocationRef.current,
          path: path,
          hasHierarchicalPath: !!(location.hierarchical_path || location.hierarchicalPath),
          hierarchicalPath: location.hierarchical_path || location.hierarchicalPath,
          locationType: location.type,
          locationId: location.id,
        });
      }
    } else {
      // Clear everything synchronously
      selectedLocationRef.current = null;
      setSelectedLocation(null);
      setSelectedLocationPath("");
      setLocationUpdateTrigger(prev => prev + 1);
      
      if (process.env.NODE_ENV === 'development') {
        console.warn('[MoveSampleModal] handleLocationChange called with null/undefined location - clearing state');
      }
    }
  }, []); // Empty deps - location is passed as parameter, no external dependencies

  const handleConfirm = async () => {
    const locationToUse = selectedLocation || selectedLocationRef.current;
    
    // Debug logging
    if (process.env.NODE_ENV === 'development') {
      console.log('[MoveSampleModal] handleConfirm called:', JSON.stringify({
        hasSelectedLocation: !!selectedLocation,
        hasRef: !!selectedLocationRef.current,
        locationToUse: locationToUse ? {
          room: locationToUse.room ? { id: locationToUse.room.id, name: locationToUse.room.name } : null,
          device: locationToUse.device ? { id: locationToUse.device.id, name: locationToUse.device.name } : null,
        } : null,
        hasOnConfirm: !!onConfirm
      }, null, 2));
    }
    
    if (locationToUse && onConfirm) {
      try {
        await onConfirm({
          sample,
          newLocation: locationToUse,
          reason,
        });
        // Only close if onConfirm succeeds (no error thrown)
        handleClose();
      } catch (error) {
        // Error notification is handled by parent component
        // Don't close modal on error so user can retry
        console.error("Move confirmation failed:", error);
        throw error; // Re-throw so parent can handle it
      }
    } else {
      if (process.env.NODE_ENV === 'development') {
        console.warn('[MoveSampleModal] handleConfirm: No location to use or onConfirm callback', {
          hasLocation: !!locationToUse,
          hasOnConfirm: !!onConfirm
        });
      }
    }
  };

  const handleClose = () => {
    setSelectedLocation(null);
    selectedLocationRef.current = null; // Clear ref too
    setSelectedLocationPath("");
    setReason("");
    setLocationUpdateTrigger(0); // Reset trigger
    onClose();
  };

  // Require at least 2 levels selected (room + device minimum per FR-033a)
  // CRITICAL: Use ref for immediate validation, fallback to state for reactivity
  // Prioritize ref since it's set synchronously, state is async
  // locationUpdateTrigger forces re-render when location changes, ensuring ref is available
  const selectedLocationForValidation = selectedLocationRef.current || selectedLocation;
  
  // Debug: Log what we're using for validation
  if (process.env.NODE_ENV === 'development' && locationUpdateTrigger > 0) {
    console.log('[MoveSampleModal] Validation using:', {
      refExists: !!selectedLocationRef.current,
      stateExists: !!selectedLocation,
      usingRef: !!selectedLocationRef.current,
      usingState: !selectedLocationRef.current && !!selectedLocation,
      trigger: locationUpdateTrigger,
    });
  }
  // CRITICAL: Check for room/device/shelf/rack/position - handle both search format and cascading format
  // For cascading format: objects have id/name/label properties
  // For search format: parent objects may be set with just IDs
  const hasRoom = !!(selectedLocationForValidation?.room && (selectedLocationForValidation.room.id || selectedLocationForValidation.room.name || selectedLocationForValidation.room));
  const hasDevice = !!(selectedLocationForValidation?.device && (selectedLocationForValidation.device.id || selectedLocationForValidation.device.name || selectedLocationForValidation.device));
  const hasShelf = !!(selectedLocationForValidation?.shelf && (selectedLocationForValidation.shelf.id || selectedLocationForValidation.shelf.label || selectedLocationForValidation.shelf.name || selectedLocationForValidation.shelf));
  const hasRack = !!(selectedLocationForValidation?.rack && (selectedLocationForValidation.rack.id || selectedLocationForValidation.rack.label || selectedLocationForValidation.rack.name || selectedLocationForValidation.rack));
  const hasPosition = !!(selectedLocationForValidation?.position && (selectedLocationForValidation.position.id || selectedLocationForValidation.position.coordinate || selectedLocationForValidation.position));
  
  // For move operation, we need a position ID (not just room+device)
  // The LocationFilterDropdown format provides position/shelf/rack/device IDs directly
  const hasPositionId = selectedLocationForValidation?.id && selectedLocationForValidation?.type === "position";
  const hasShelfId = selectedLocationForValidation?.id && selectedLocationForValidation?.type === "shelf";
  const hasRackId = selectedLocationForValidation?.id && selectedLocationForValidation?.type === "rack";
  const hasDeviceId = selectedLocationForValidation?.id && selectedLocationForValidation?.type === "device";
  const hasPositionFromCascade = selectedLocationForValidation?.position && selectedLocationForValidation.position.id;
  
  // CRITICAL: If location has type="position/shelf/rack/device" and id, it's valid from dropdown
  // Even if room/device aren't set in the converted format, we can still proceed
  // The StorageDashboard will use the ID directly to resolve to a position
  const hasValidLocationFromDropdown = (hasPositionId || hasShelfId || hasRackId || hasDeviceId) && selectedLocationForValidation?.id;
  
  // Validate minimum 2 levels (room + device) AND ensure we have a way to resolve to position
  // Position can be at device level (2 levels), shelf level (3 levels), rack level (4 levels), or position level (5 levels)
  // When only room+device is selected, StorageDashboard will find/create a position at device level
  // OR if we have a location ID directly from dropdown (device/shelf/rack/position), that's also valid
  // CRITICAL: (hasRoom && hasDevice) should be sufficient - this is the 2-level minimum requirement
  const meetsMinimumLevels = (hasRoom && hasDevice) || hasValidLocationFromDropdown || hasShelf || hasRack;
  // Allow room+device (minimum 2 levels) - StorageDashboard will resolve to position ID
  // This should work for both search format (with type/id) and cascading format (with room/device objects)
  const canResolveToPosition = hasPositionId || hasPositionFromCascade || hasShelfId || hasRackId || hasDeviceId || (hasRack && hasPosition) || hasShelf || (hasRoom && hasDevice);
  
  const canConfirm = meetsMinimumLevels && canResolveToPosition;
  
  // Debug logging
  if (process.env.NODE_ENV === 'development') {
    const refValue = selectedLocationRef.current;
    console.log('[MoveSampleModal] Validation check:', JSON.stringify({
      hasRoom,
      hasDevice,
      hasShelf,
      hasRack,
      hasPosition,
      hasPositionId,
      hasShelfId,
      hasRackId,
      hasDeviceId,
      hasPositionFromCascade,
      hasValidLocationFromDropdown,
      meetsMinimumLevels,
      canResolveToPosition,
      canConfirm,
      selectedLocation: selectedLocation ? {
        room: selectedLocation.room ? { id: selectedLocation.room.id, name: selectedLocation.room.name } : null,
        device: selectedLocation.device ? { id: selectedLocation.device.id, name: selectedLocation.device.name } : null,
        type: selectedLocation.type,
        id: selectedLocation.id,
      } : null,
      selectedLocationRef: refValue ? {
        room: refValue.room ? { id: refValue.room.id, name: refValue.room.name } : null,
        device: refValue.device ? { id: refValue.device.id, name: refValue.device.name } : null,
        type: refValue.type,
        id: refValue.id,
      } : null,
      selectedLocationForValidation: selectedLocationForValidation ? {
        room: selectedLocationForValidation.room ? { id: selectedLocationForValidation.room.id, name: selectedLocationForValidation.room.name } : null,
        device: selectedLocationForValidation.device ? { id: selectedLocationForValidation.device.id, name: selectedLocationForValidation.device.name } : null,
        type: selectedLocationForValidation.type,
        id: selectedLocationForValidation.id,
        hierarchical_path: selectedLocationForValidation.hierarchical_path || selectedLocationForValidation.hierarchicalPath,
      } : null,
      refCurrentExists: !!refValue,
      stateExists: !!selectedLocation,
      forValidationExists: !!selectedLocationForValidation,
      roomCheck: selectedLocationForValidation?.room ? {
        hasId: !!selectedLocationForValidation.room.id,
        hasName: !!selectedLocationForValidation.room.name,
        hasObject: !!selectedLocationForValidation.room,
        roomObject: selectedLocationForValidation.room,
      } : null,
      deviceCheck: selectedLocationForValidation?.device ? {
        hasId: !!selectedLocationForValidation.device.id,
        hasName: !!selectedLocationForValidation.device.name,
        hasObject: !!selectedLocationForValidation.device,
        deviceObject: selectedLocationForValidation.device,
      } : null,
    }, null, 2));
  }

  return (
    <ComposedModal
      open={open}
      onClose={handleClose}
      size="lg"
      data-testid="move-modal"
    >
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
          <div
            className="move-modal-current-location"
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

        {/* Downward Arrow Icon */}
        {currentLocation && (
          <div className="move-modal-arrow">
            <ArrowDown size={24} />
          </div>
        )}

        {/* New Location Selector */}
        <div
          className="move-modal-new-location"
          data-testid="new-location-section"
        >
          <div className="location-selector-box">
            <label className="form-label">
              <FormattedMessage
                id="storage.new.location"
                defaultMessage="New Location"
              />{" "}
              <span className="required-indicator">*</span>
            </label>
            <div className="move-modal-new-location-selector">
              <LocationSearchAndCreate
                onLocationChange={handleLocationChange}
                selectedLocation={selectedLocationForValidation}
                allowInactive={false}
                showCreateButton={true}
              />
            </div>
          </div>
        </div>

        {/* Selected Location Preview */}
        {selectedLocationPath && (
          <div
            className="move-modal-selected-preview"
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
              defaultMessage:
                "Optional: Enter reason for moving this sample...",
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
        <Button 
          kind="primary" 
          onClick={handleConfirm} 
          disabled={!canConfirm}
          data-testid="confirm-move-button"
        >
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
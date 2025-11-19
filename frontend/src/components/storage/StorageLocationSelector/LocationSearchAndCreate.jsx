import React, { useState, useEffect, useCallback, useRef } from "react";
import { Button } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { Add } from "@carbon/icons-react";
import { getFromOpenElisServer } from "../../utils/Utils";
import LocationFilterDropdown from "../StorageDashboard/LocationFilterDropdown";
import EnhancedCascadingMode from "./EnhancedCascadingMode";
import "./LocationSearchAndCreate.css";

/**
 * Unified component for location search + inline creation
 * Combines LocationFilterDropdown (search) with EnhancedCascadingMode (create)
 *
 * Props:
 * - onLocationChange: function - Callback when location selected/created
 * - selectedLocation: object - Currently selected location
 * - allowInactive: boolean - Allow inactive locations (default: false)
 * - showCreateButton: boolean - Show "Add Location" button (default: true)
 * - searchPlaceholder: string - Placeholder text for search input (default: "Search for location...")
 * - isActive: boolean - Whether this input method is currently active (for visual feedback)
 */
const LocationSearchAndCreate = ({
  onLocationChange,
  selectedLocation,
  allowInactive = false,
  showCreateButton = true,
  searchPlaceholder,
  isActive = false,
}) => {
  const intl = useIntl();
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [internalSelectedLocation, setInternalSelectedLocation] =
    useState(null);

  // SIMPLIFIED: Only sync with external prop when NOT in create form
  // When in create form, internal state is independent until "Add" is clicked
  // CRITICAL: Don't sync when form just closed - parent might not have updated yet
  const justClosedFormRef = useRef(false);

  useEffect(() => {
    // CRITICAL: When form closes, we need to preserve the location that was just selected
    // If we just closed the form, don't sync - the parent will update selectedLocation
    // and we'll sync on the next render cycle when parent's state has updated
    if (justClosedFormRef.current) {
      justClosedFormRef.current = false;
      // Don't reset internalSelectedLocation - keep it until parent updates
      // This prevents the location from being lost during the async state update
      return;
    }

    // Only sync when we're in search mode (not create form) and parent has a location
    // This allows parent to control the selected location when searching
    // CRITICAL: Don't overwrite internalSelectedLocation if it's valid and parent is null
    // This prevents losing the location during async state updates
    if (!showCreateForm) {
      if (selectedLocation) {
        // Parent has location - sync it
        setInternalSelectedLocation(selectedLocation);
      } else if (
        internalSelectedLocation &&
        internalSelectedLocation.room &&
        internalSelectedLocation.device
      ) {
        // Parent doesn't have location yet (async update), but we have valid internal state
        // Keep it until parent updates - don't reset to null
        // This is the critical fix - preserve state during async updates
      }
      // If both are null, that's fine - user cleared the selection
    }
  }, [selectedLocation, showCreateForm]);

  /**
   * Convert LocationFilterDropdown format to EnhancedCascadingMode format
   * LocationFilterDropdown format: { id, type, name, hierarchicalPath, parentRoomId, parentRoomName, ... }
   * EnhancedCascadingMode format: { room, device, shelf, rack, position, hierarchical_path }
   *
   * API now ALWAYS provides parent IDs and names, so no fallback parsing needed.
   */
  const convertSearchToCascadingFormat = useCallback((searchLocation) => {
    if (!searchLocation || !searchLocation.id) {
      return null;
    }

    // API returns hierarchicalPath (camelCase), preserve it and also set hierarchical_path (snake_case) for consistency
    // CRITICAL: Extract hierarchicalPath BEFORE spread to ensure it's preserved
    const hierarchicalPath =
      searchLocation.hierarchical_path || searchLocation.hierarchicalPath;
    const converted = {
      ...searchLocation, // Spread all other properties first
      id: searchLocation.id,
      type: searchLocation.type,
      // Override with explicit hierarchical_path/hierarchicalPath to ensure they're set (spread might have them, but be explicit)
      hierarchical_path: hierarchicalPath,
      hierarchicalPath: hierarchicalPath,
    };

    // Room selection
    if (searchLocation.type === "room") {
      converted.room = {
        id: searchLocation.id,
        name: searchLocation.name,
        code:
          searchLocation.code ||
          searchLocation.name?.substring(0, 50).toUpperCase(),
        active: searchLocation.active !== false,
      };
    }

    // Device selection - API provides type="device", parentRoomId, and parentRoomName
    if (searchLocation.type === "device") {
      converted.device = {
        id: searchLocation.id,
        name: searchLocation.name,
        code: searchLocation.code,
      };

      if (searchLocation.parentRoomId) {
        converted.room = {
          id: searchLocation.parentRoomId,
          name: searchLocation.parentRoomName,
        };
      }
    }

    // Shelf selection - API provides parentDeviceId, parentDeviceName, parentRoomId, parentRoomName
    if (searchLocation.type === "shelf") {
      converted.shelf = {
        id: searchLocation.id,
        label: searchLocation.label,
      };

      if (searchLocation.parentDeviceId) {
        converted.device = {
          id: searchLocation.parentDeviceId,
          name: searchLocation.parentDeviceName,
        };
      }

      if (searchLocation.parentRoomId) {
        converted.room = {
          id: searchLocation.parentRoomId,
          name: searchLocation.parentRoomName,
        };
      }

      // CRITICAL: Ensure hierarchicalPath is preserved for shelves (API provides full path)
      // Re-assert after setting room/device/shelf to ensure it's not lost
      if (hierarchicalPath) {
        converted.hierarchical_path = hierarchicalPath;
        converted.hierarchicalPath = hierarchicalPath;
      }
    }

    // Rack selection - API provides parentShelfId, parentShelfLabel, parentDeviceId, parentDeviceName, parentRoomId, parentRoomName
    if (searchLocation.type === "rack") {
      converted.rack = {
        id: searchLocation.id,
        label: searchLocation.label,
      };

      if (searchLocation.parentShelfId) {
        converted.shelf = {
          id: searchLocation.parentShelfId,
          label: searchLocation.parentShelfLabel,
        };
      }

      if (searchLocation.parentDeviceId) {
        converted.device = {
          id: searchLocation.parentDeviceId,
          name: searchLocation.parentDeviceName,
        };
      }

      if (searchLocation.parentRoomId) {
        converted.room = {
          id: searchLocation.parentRoomId,
          name: searchLocation.parentRoomName,
        };
      }

      // CRITICAL: Ensure hierarchicalPath is preserved for racks (API provides full path)
      // Re-assert after setting room/device/shelf/rack to ensure it's not lost
      if (hierarchicalPath) {
        converted.hierarchical_path = hierarchicalPath;
        converted.hierarchicalPath = hierarchicalPath;
      }
    }

    // Position selection - API should provide all parent IDs and names
    if (searchLocation.type === "position") {
      converted.position = {
        id: searchLocation.id,
        coordinate: searchLocation.coordinate,
      };

      if (searchLocation.parentRackId) {
        converted.rack = {
          id: searchLocation.parentRackId,
          label: searchLocation.parentRackLabel,
        };
      }

      if (searchLocation.parentShelfId) {
        converted.shelf = {
          id: searchLocation.parentShelfId,
          label: searchLocation.parentShelfLabel,
        };
      }

      if (searchLocation.parentDeviceId) {
        converted.device = {
          id: searchLocation.parentDeviceId,
          name: searchLocation.parentDeviceName,
        };
      }

      if (searchLocation.parentRoomId) {
        converted.room = {
          id: searchLocation.parentRoomId,
          name: searchLocation.parentRoomName,
        };
      }

      // CRITICAL: Ensure hierarchicalPath is preserved for positions (API provides full path)
      // Re-assert after setting all parent components to ensure it's not lost
      if (hierarchicalPath) {
        converted.hierarchical_path = hierarchicalPath;
        converted.hierarchicalPath = hierarchicalPath;
      }
    }

    return converted;
  }, []);

  const handleSearchSelect = (location) => {
    // LocationFilterDropdown format: { id, type, name, hierarchical_path, ... }
    // DEBUG: Log EXACTLY what we receive from parent
    if (process.env.NODE_ENV === "development") {
      console.log(
        "[LocationSearchAndCreate] handleSearchSelect RECEIVED:",
        JSON.stringify(location, null, 2),
      );
    }

    // Convert to EnhancedCascadingMode format for proper initialization
    const convertedLocation = convertSearchToCascadingFormat(location);

    // Debug logging
    if (process.env.NODE_ENV === "development") {
      console.log(
        "[LocationSearchAndCreate] handleSearchSelect called:",
        JSON.stringify(
          {
            originalLocation: location
              ? {
                  id: location.id,
                  type: location.type,
                  deviceType: location.deviceType,
                  name: location.name,
                  hierarchicalPath: location.hierarchicalPath, // API format (camelCase)
                  hierarchical_path: location.hierarchical_path, // Alternative format (snake_case)
                  parentRoomId: location.parentRoomId, // CRITICAL: Parent IDs from API
                  parentDeviceId: location.parentDeviceId,
                  parentShelfId: location.parentShelfId,
                }
              : null,
            convertedLocation: convertedLocation
              ? {
                  hierarchicalPath: convertedLocation.hierarchicalPath, // Preserved from API
                  hierarchical_path: convertedLocation.hierarchical_path, // Normalized
                  room: convertedLocation.room
                    ? {
                        id: convertedLocation.room.id,
                        name: convertedLocation.room.name,
                      }
                    : null,
                  device: convertedLocation.device
                    ? {
                        id: convertedLocation.device.id,
                        name: convertedLocation.device.name,
                      }
                    : null,
                  shelf: convertedLocation.shelf
                    ? {
                        id: convertedLocation.shelf.id,
                        label: convertedLocation.shelf.label,
                      }
                    : null,
                  rack: convertedLocation.rack
                    ? {
                        id: convertedLocation.rack.id,
                        label: convertedLocation.rack.label,
                      }
                    : null,
                  type: convertedLocation.type,
                }
              : null,
          },
          null,
          2,
        ),
      );
    }

    setInternalSelectedLocation(convertedLocation);

    // CRITICAL: Call onLocationChange to notify parent (LocationManagementModal)
    // This ensures the Selected Location preview appears and Confirm button is enabled
    // Add locationId, locationType, and positionCoordinate for flexible assignment architecture
    if (onLocationChange) {
      // Extract locationId and locationType from converted location
      let locationId = null;
      let locationType = null;

      if (convertedLocation?.rack?.id) {
        locationId = convertedLocation.rack.id;
        locationType = "rack";
      } else if (convertedLocation?.shelf?.id) {
        locationId = convertedLocation.shelf.id;
        locationType = "shelf";
      } else if (convertedLocation?.device?.id) {
        locationId = convertedLocation.device.id;
        locationType = "device";
      } else if (convertedLocation?.id && convertedLocation?.type) {
        // Direct format from search - type is already the hierarchy level
        if (
          convertedLocation.type === "rack" ||
          convertedLocation.type === "shelf" ||
          convertedLocation.type === "device"
        ) {
          locationId = convertedLocation.id;
          locationType = convertedLocation.type;
        }
      }

      const locationWithFlexibleFields = {
        ...convertedLocation,
        locationId: locationId,
        locationType: locationType,
        positionCoordinate:
          convertedLocation?.position?.coordinate ||
          convertedLocation?.positionCoordinate ||
          null,
      };

      if (process.env.NODE_ENV === "development") {
        console.log(
          "[LocationSearchAndCreate] Calling onLocationChange with:",
          JSON.stringify(
            {
              hasHierarchicalPath: !!(
                locationWithFlexibleFields?.hierarchicalPath ||
                locationWithFlexibleFields?.hierarchical_path
              ),
              hierarchicalPath: locationWithFlexibleFields?.hierarchicalPath,
              hierarchical_path: locationWithFlexibleFields?.hierarchical_path,
              type: locationWithFlexibleFields?.type,
              locationId: locationWithFlexibleFields?.locationId,
              locationType: locationWithFlexibleFields?.locationType,
              positionCoordinate:
                locationWithFlexibleFields?.positionCoordinate,
              hasRoom: !!locationWithFlexibleFields?.room,
              hasDevice: !!locationWithFlexibleFields?.device,
              hasShelf: !!locationWithFlexibleFields?.shelf,
            },
            null,
            2,
          ),
        );
      }
      onLocationChange(locationWithFlexibleFields);
    }
    setShowCreateForm(false);
  };

  const handleCreateSelect = (location) => {
    // EnhancedCascadingMode format: { room, device, shelf, rack, position }
    // This is called by EnhancedCascadingMode when user clicks through hierarchy

    // If location is null/undefined, don't update (avoid clearing parent state)
    if (!location) {
      return;
    }

    // Update internal state immediately - this triggers button validation
    setInternalSelectedLocation(location);

    // CRITICAL: Build location object with type, hierarchical_path, locationId, locationType, and positionCoordinate
    // for parent (LocationManagementModal) - flexible assignment architecture
    // This ensures parent gets notified immediately and can display full path and enable button
    let locationId = null;
    let locationType = null;

    // Extract locationId and locationType based on lowest selected hierarchy level
    if (location.rack && location.rack.id) {
      locationId = location.rack.id;
      locationType = "rack";
    } else if (location.shelf && location.shelf.id) {
      locationId = location.shelf.id;
      locationType = "shelf";
    } else if (location.device && location.device.id) {
      locationId = location.device.id;
      locationType = "device";
    }

    const locationToPass = {
      ...location,
      locationId: locationId,
      locationType: locationType,
      positionCoordinate:
        location.position?.coordinate || location.positionCoordinate || null,
    };

    // Determine type based on highest level selected
    if (location.position?.id || location.position?.coordinate) {
      locationToPass.type = "position";
      if (location.position.id) {
        locationToPass.id = location.position.id;
      }
    } else if (location.rack?.id) {
      locationToPass.type = "rack";
      locationToPass.id = location.rack.id;
    } else if (location.shelf?.id) {
      locationToPass.type = "shelf";
      locationToPass.id = location.shelf.id;
    } else if (location.device?.id) {
      locationToPass.type = "device";
      locationToPass.id = location.device.id;
    } else if (location.room?.id) {
      locationToPass.type = "room";
      locationToPass.id = location.room.id;
    }

    // Build hierarchical_path from components if not already present
    if (!locationToPass.hierarchical_path && !locationToPass.hierarchicalPath) {
      const pathParts = [];
      const roomName = location.room?.name || location.room?.code || "";
      const deviceName = location.device?.name || location.device?.code || "";
      const shelfLabel = location.shelf?.label || location.shelf?.name || "";
      const rackLabel = location.rack?.label || location.rack?.name || "";
      const positionCoord = location.position?.coordinate || "";

      if (roomName) pathParts.push(roomName);
      if (deviceName) pathParts.push(deviceName);
      if (shelfLabel) pathParts.push(shelfLabel);
      if (rackLabel) pathParts.push(rackLabel);
      if (positionCoord) pathParts.push(`Position ${positionCoord}`);

      if (pathParts.length > 0) {
        locationToPass.hierarchical_path = pathParts.join(" > ");
        locationToPass.hierarchicalPath = pathParts.join(" > ");
      }
    }

    // CRITICAL: Notify parent immediately so it can display path and enable button
    if (onLocationChange) {
      onLocationChange(locationToPass);
    }
  };

  // Debug: Track internalSelectedLocation changes
  useEffect(() => {
    if (process.env.NODE_ENV === "development") {
      console.log(
        "[LocationSearchAndCreate] internalSelectedLocation changed:",
        JSON.stringify(
          {
            room: internalSelectedLocation?.room
              ? {
                  id: internalSelectedLocation.room.id,
                  name: internalSelectedLocation.room.name,
                }
              : null,
            device: internalSelectedLocation?.device
              ? {
                  id: internalSelectedLocation.device.id,
                  name: internalSelectedLocation.device.name,
                }
              : null,
            shelf: internalSelectedLocation?.shelf
              ? {
                  id: internalSelectedLocation.shelf.id,
                  label: internalSelectedLocation.shelf.label,
                }
              : null,
            rack: internalSelectedLocation?.rack
              ? {
                  id: internalSelectedLocation.rack.id,
                  label: internalSelectedLocation.rack.label,
                }
              : null,
          },
          null,
          2,
        ),
      );
    }
  }, [internalSelectedLocation]);

  const handleAddLocation = () => {
    // Validate that we have at least 2 levels selected
    const roomSelected =
      internalSelectedLocation?.room?.id || internalSelectedLocation?.room;
    const deviceSelected =
      internalSelectedLocation?.device?.id || internalSelectedLocation?.device;
    const shelfSelected =
      internalSelectedLocation?.shelf?.id || internalSelectedLocation?.shelf;
    const rackSelected =
      internalSelectedLocation?.rack?.id || internalSelectedLocation?.rack;

    const selectedCount = [
      roomSelected,
      deviceSelected,
      shelfSelected,
      rackSelected,
    ].filter(Boolean).length;

    if (process.env.NODE_ENV === "development") {
      console.log("[LocationSearchAndCreate] handleAddLocation validation:", {
        roomSelected,
        deviceSelected,
        shelfSelected,
        rackSelected,
        selectedCount,
        internalSelectedLocation: {
          room: internalSelectedLocation?.room
            ? {
                id: internalSelectedLocation.room.id,
                name: internalSelectedLocation.room.name,
              }
            : null,
          device: internalSelectedLocation?.device
            ? {
                id: internalSelectedLocation.device.id,
                name: internalSelectedLocation.device.name,
              }
            : null,
          shelf: internalSelectedLocation?.shelf
            ? {
                id: internalSelectedLocation.shelf.id,
                label: internalSelectedLocation.shelf.label,
              }
            : null,
          rack: internalSelectedLocation?.rack
            ? {
                id: internalSelectedLocation.rack.id,
                label: internalSelectedLocation.rack.label,
              }
            : null,
        },
      });
    }

    if (selectedCount >= 2 && internalSelectedLocation) {
      // At least 2 levels selected - valid location
      // CRITICAL: Build hierarchical_path from cascading format to match search format
      // This ensures LocationManagementModal can display the full path and validate consistently
      const locationToPass = {
        ...internalSelectedLocation,
      };

      // Build hierarchical_path from components if not already present
      if (
        !locationToPass.hierarchical_path &&
        !locationToPass.hierarchicalPath
      ) {
        const pathParts = [];
        const roomName = locationToPass.room?.name || "";
        const deviceName = locationToPass.device?.name || "";
        const shelfLabel =
          locationToPass.shelf?.label || locationToPass.shelf?.name || "";
        const rackLabel =
          locationToPass.rack?.label || locationToPass.rack?.name || "";
        const positionCoord = locationToPass.position?.coordinate || "";

        if (roomName) pathParts.push(roomName);
        if (deviceName) pathParts.push(deviceName);
        if (shelfLabel) pathParts.push(shelfLabel);
        if (rackLabel) pathParts.push(rackLabel);
        if (positionCoord) pathParts.push(`Position ${positionCoord}`);

        if (pathParts.length > 0) {
          locationToPass.hierarchical_path = pathParts.join(" > ");
          locationToPass.hierarchicalPath = pathParts.join(" > "); // Also set camelCase for consistency
        }
      }

      if (process.env.NODE_ENV === "development") {
        console.log(
          "[LocationSearchAndCreate] handleAddLocation calling onLocationChange with:",
          JSON.stringify(
            {
              room: locationToPass.room
                ? { id: locationToPass.room.id, name: locationToPass.room.name }
                : null,
              device: locationToPass.device
                ? {
                    id: locationToPass.device.id,
                    name: locationToPass.device.name,
                  }
                : null,
              hierarchical_path: locationToPass.hierarchical_path,
              selectedCount,
            },
            null,
            2,
          ),
        );
      }

      // CRITICAL: Call onLocationChange FIRST (synchronously) - this updates parent's ref immediately
      // Then close form synchronously (like handleSearchSelect does)
      if (onLocationChange) {
        onLocationChange(locationToPass);
      }

      // Close form immediately and synchronously (same as handleSearchSelect)
      setShowCreateForm(false);

      // Keep internalSelectedLocation - it will sync with parent's selectedLocation via useEffect
    }
    // If not valid, don't close (user can continue building location)
  };

  // Check if location is valid (at least 2 levels with actual IDs or objects)
  // A level is considered selected if it has an id OR if it's a newly created object (has name/label)
  const hasRoom = !!(
    internalSelectedLocation?.room &&
    (internalSelectedLocation.room.id || internalSelectedLocation.room.name)
  );
  const hasDevice = !!(
    internalSelectedLocation?.device &&
    (internalSelectedLocation.device.id || internalSelectedLocation.device.name)
  );
  const hasShelf = !!(
    internalSelectedLocation?.shelf &&
    (internalSelectedLocation.shelf.id ||
      internalSelectedLocation.shelf.label ||
      internalSelectedLocation.shelf.name)
  );
  const hasRack = !!(
    internalSelectedLocation?.rack &&
    (internalSelectedLocation.rack.id ||
      internalSelectedLocation.rack.label ||
      internalSelectedLocation.rack.name)
  );

  const selectedCount = [hasRoom, hasDevice, hasShelf, hasRack].filter(
    Boolean,
  ).length;
  const canAddLocation = selectedCount >= 2;

  // Debug logging
  if (process.env.NODE_ENV === "development") {
    console.log(
      "[LocationSearchAndCreate] Validation:",
      JSON.stringify(
        {
          internalSelectedLocation: {
            room: internalSelectedLocation?.room
              ? {
                  id: internalSelectedLocation.room.id,
                  name: internalSelectedLocation.room.name,
                }
              : null,
            device: internalSelectedLocation?.device
              ? {
                  id: internalSelectedLocation.device.id,
                  name: internalSelectedLocation.device.name,
                }
              : null,
            shelf: internalSelectedLocation?.shelf
              ? {
                  id: internalSelectedLocation.shelf.id,
                  label: internalSelectedLocation.shelf.label,
                }
              : null,
            rack: internalSelectedLocation?.rack
              ? {
                  id: internalSelectedLocation.rack.id,
                  label: internalSelectedLocation.rack.label,
                }
              : null,
          },
          hasRoom,
          hasDevice,
          hasShelf,
          hasRack,
          selectedCount,
          canAddLocation,
        },
        null,
        2,
      ),
    );
  }

  return (
    <div
      className={`location-search-and-create ${isActive ? "active-input-method" : ""}`}
      data-testid="location-search-and-create"
    >
      {!showCreateForm ? (
        <div className="location-search-container">
          <div className="location-search-wrapper">
            <LocationFilterDropdown
              onLocationChange={handleSearchSelect}
              selectedLocation={internalSelectedLocation}
              allowInactive={allowInactive}
              showSelectedDisplay={false}
              placeholder={
                searchPlaceholder ||
                intl.formatMessage({
                  id: "storage.search.location.placeholder",
                  defaultMessage: "Search for location...",
                })
              }
            />
          </div>
          {showCreateButton && (
            <Button
              kind="secondary"
              size="md"
              renderIcon={Add}
              onClick={() => setShowCreateForm(true)}
              data-testid="add-location-button"
            >
              <FormattedMessage
                id="storage.add.location"
                defaultMessage="Add Location"
              />
            </Button>
          )}
        </div>
      ) : (
        <div
          className="location-create-container"
          data-testid="location-create-container"
        >
          <EnhancedCascadingMode
            onLocationChange={handleCreateSelect}
            selectedLocation={internalSelectedLocation || null}
          />
          <div className="location-create-actions">
            <Button
              kind="ghost"
              size="sm"
              onClick={() => {
                setShowCreateForm(false);
                setInternalSelectedLocation(null); // Reset state
              }}
              className="cancel-create-button"
            >
              <FormattedMessage
                id="label.button.cancel"
                defaultMessage="Cancel"
              />
            </Button>
            <Button
              kind="primary"
              size="sm"
              onClick={() => {
                if (process.env.NODE_ENV === "development") {
                  console.log("[LocationSearchAndCreate] Add button clicked");
                }
                handleAddLocation();
              }}
              disabled={!canAddLocation}
              className="add-location-create-button"
              data-testid="add-location-create-button"
            >
              <FormattedMessage
                id="storage.add.location.button"
                defaultMessage="Add"
              />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
};

export default LocationSearchAndCreate;

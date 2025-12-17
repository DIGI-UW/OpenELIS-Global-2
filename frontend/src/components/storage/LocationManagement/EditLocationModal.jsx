import React, { useEffect, useRef, useState } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  TextInput,
  TextArea,
  Dropdown,
  Toggle,
  InlineNotification,
  Checkbox,
  SkeletonText,
  SkeletonPlaceholder,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServerV2 } from "../../utils/Utils";
import config from "../../../config.json";
import "./EditLocationModal.css";

/**
 * Modal for editing location entities (Room, Device, Shelf, Rack)
 * Displays editable fields based on entity type, with Code and Parent fields read-only
 *
 * Props:
 * - open: boolean - Whether modal is open
 * - location: object - Location entity data { id, name, code, description, active, ... }
 * - locationType: string - "room" | "device" | "shelf" | "rack"
 * - onClose: function - Callback when modal closes
 * - onSave: function - Callback when save is successful with updated location
 */
const EditLocationModal = ({
  open,
  location,
  locationType,
  onClose,
  onSave,
}) => {
  const intl = useIntl();
  // Initialize formData with default values to ensure controlled components
  const [formData, setFormData] = useState({
    name: "",
    code: "",
    description: "",
    active: false,
    type: "",
    temperatureSetting: "",
    capacityLimit: "",
    label: "",
    rows: "",
    columns: "",
    positionSchemaHint: "",
  });
  const [error, setError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  // Track original code to detect changes and show warning
  const [originalCode, setOriginalCode] = useState("");
  const [codeChangeAcknowledged, setCodeChangeAcknowledged] = useState(false);
  // Track parent change and constraint checking
  const [availableRooms, setAvailableRooms] = useState([]);
  const [availableDevices, setAvailableDevices] = useState([]);
  const [availableShelves, setAvailableShelves] = useState([]);
  const [selectedParentRoomId, setSelectedParentRoomId] = useState(null);
  const [selectedParentDeviceId, setSelectedParentDeviceId] = useState(null);
  const [selectedParentShelfId, setSelectedParentShelfId] = useState(null);
  const [originalParentRoomId, setOriginalParentRoomId] = useState(null);
  const [originalParentDeviceId, setOriginalParentDeviceId] = useState(null);
  const [originalParentShelfId, setOriginalParentShelfId] = useState(null);
  const [parentChangeWarning, setParentChangeWarning] = useState(null);
  const [parentChangeAcknowledged, setParentChangeAcknowledged] =
    useState(false);
  // Synchronous ref to latest form data to prevent race conditions when user toggles
  // and immediately clicks Save before React state updates
  const formDataRef = useRef(null);

  // Normalize active value to boolean for controlled Toggle component
  const normalizeActive = (value) => {
    return value === true || value === "true" || value === 1 || value === "1";
  };

  // Helper function to get correct plural form for API endpoints
  const getPluralType = (type) => {
    const pluralMap = {
      room: "rooms",
      device: "devices",
      shelf: "shelves", // Not "shelfs"
      rack: "racks",
    };
    return pluralMap[type] || `${type}s`;
  };

  // Helper function to get capitalized location type name for titles
  const getLocationTypeName = (type) => {
    const nameMap = {
      room: "Room",
      device: "Device",
      shelf: "Shelf",
      rack: "Rack",
    };
    return nameMap[type] || type;
  };

  // Helper function to initialize form data from location prop
  const initializeFormDataFromLocation = (loc) => {
    if (!loc) return {};
    const normalizedActive = normalizeActive(loc.active);
    return {
      name: loc.name || "",
      code: loc.code || "",
      description: loc.description || "",
      active: Boolean(normalizedActive),
      type: loc.type || "",
      temperatureSetting: loc.temperatureSetting || "",
      capacityLimit: loc.capacityLimit || "",
      label: loc.label || "",
      rows: loc.rows || "",
      columns: loc.columns || "",
      positionSchemaHint: loc.positionSchemaHint || "",
      // Support multiple field names from API for parent data
      parentRoomName:
        loc.parentRoomName || loc.parentRoom?.name || loc.roomName || "",
      parentDeviceName:
        loc.parentDeviceName || loc.parentDevice?.name || loc.deviceName || "",
      parentShelfLabel:
        loc.parentShelfLabel || loc.parentShelf?.label || loc.shelfLabel || "",
    };
  };

  // Initialize form data when modal opens or location changes
  useEffect(() => {
    let isMounted = true;

    if (open && location && location.id && locationType) {
      // Initialize immediately from location prop to avoid undefined values
      const initial = initializeFormDataFromLocation(location);
      formDataRef.current = initial;
      setFormData(initial);
      setOriginalCode(location.code || "");
      setIsLoading(true);
      setError(null);

      // Fetch full location data from API when modal opens
      const endpoint = `/rest/storage/${getPluralType(locationType)}/${location.id}`;
      getFromOpenElisServerV2(endpoint)
        .then((fullLocation) => {
          // Only update state if component is still mounted
          if (!isMounted) return;

          if (fullLocation) {
            const normalizedActive = normalizeActive(fullLocation.active);
            const next = {
              name: fullLocation.name || "",
              code: fullLocation.code || "",
              description: fullLocation.description || "",
              active: Boolean(normalizedActive),
              type: fullLocation.type || "",
              temperatureSetting: fullLocation.temperatureSetting || "",
              capacityLimit: fullLocation.capacityLimit || "",
              label: fullLocation.label || "",
              rows: fullLocation.rows || "",
              columns: fullLocation.columns || "",
              positionSchemaHint: fullLocation.positionSchemaHint || "",
              // Support multiple field names from API for parent data
              parentRoomName:
                fullLocation.parentRoomName ||
                fullLocation.parentRoom?.name ||
                fullLocation.roomName ||
                "",
              parentDeviceName:
                fullLocation.parentDeviceName ||
                fullLocation.parentDevice?.name ||
                fullLocation.deviceName ||
                "",
              parentShelfLabel:
                fullLocation.parentShelfLabel ||
                fullLocation.parentShelf?.label ||
                fullLocation.shelfLabel ||
                "",
            };
            formDataRef.current = next;
            setFormData(next);
            // Store original code from full data
            setOriginalCode(fullLocation.code || "");
            // Store original parent IDs
            if (locationType === "device") {
              const parentRoomId =
                fullLocation.parentRoomId || fullLocation.parentRoom?.id;
              if (parentRoomId) {
                setOriginalParentRoomId(String(parentRoomId));
                setSelectedParentRoomId(String(parentRoomId));
              }
            }
            if (locationType === "shelf") {
              const parentDeviceId =
                fullLocation.parentDeviceId || fullLocation.parentDevice?.id;
              if (parentDeviceId) {
                setOriginalParentDeviceId(String(parentDeviceId));
                setSelectedParentDeviceId(String(parentDeviceId));
              }
            }
            if (locationType === "rack") {
              // Try multiple sources for parentShelfId (API response, nested object, or location prop)
              // parentShelfId is REQUIRED for racks - backend validation will fail without it
              const parentShelfId =
                fullLocation.parentShelfId ||
                fullLocation.parentShelf?.id ||
                location?.parentShelfId ||
                location?.parentShelf?.id;

              if (
                parentShelfId != null &&
                parentShelfId !== undefined &&
                parentShelfId !== ""
              ) {
                setOriginalParentShelfId(String(parentShelfId));
                setSelectedParentShelfId(String(parentShelfId));
              } else {
                // This should never happen - racks must have a parent shelf
                console.error(
                  "EditLocationModal: parentShelfId missing from API response for rack",
                  {
                    rackId: location.id,
                    fullLocation,
                    locationProp: location,
                  },
                );
                setError(
                  "Cannot edit rack: parent shelf information is missing. This may indicate a data integrity issue.",
                );
              }
            }
            setError(null);
            setIsLoading(false);
          } else {
            throw new Error("No data returned from API");
          }
        })
        .catch((err) => {
          // Only update state if component is still mounted
          if (!isMounted) return;

          console.warn("Failed to fetch location data, using prop data:", err);
          setError("Failed to load location data");
          setIsLoading(false);
        });
    } else if (location && !open) {
      // Reset when modal closes
      formDataRef.current = null;
      setFormData({});
      setIsLoading(false);
    } else if (!location) {
      // Initialize with empty values to avoid uncontrolled component warnings
      const empty = {
        name: "",
        code: "",
        description: "",
        active: true,
        type: "",
        temperatureSetting: "",
        capacityLimit: "",
        label: "",
        rows: "",
        columns: "",
        positionSchemaHint: "",
      };
      formDataRef.current = empty;
      setFormData(empty);
      setIsLoading(false);
    }

    // Cleanup function to prevent state updates after unmount
    return () => {
      isMounted = false;
    };
  }, [open, location, locationType]);

  // Load parent options when modal opens
  useEffect(() => {
    if (!open || !location) return;

    let isMounted = true;

    // Load rooms for device parent selection
    if (locationType === "device") {
      const promise = getFromOpenElisServerV2("/rest/storage/rooms");
      if (promise && typeof promise.then === "function") {
        promise
          .then((response) => {
            if (!isMounted) return;
            if (response && Array.isArray(response)) {
              const activeRooms = response.filter(
                (room) => room.active !== false,
              );
              setAvailableRooms(activeRooms);
            }
          })
          .catch((err) => {
            if (!isMounted) return;
            console.error("Failed to load rooms:", err);
          });
      } else {
        // If promise is undefined, log but don't crash
        console.warn(
          "getFromOpenElisServerV2 returned undefined for /rest/storage/rooms",
        );
      }
    }
    // Load devices for shelf parent selection
    if (locationType === "shelf") {
      const promise = getFromOpenElisServerV2("/rest/storage/devices");
      if (promise && typeof promise.then === "function") {
        promise
          .then((response) => {
            if (!isMounted) return;
            if (response && Array.isArray(response)) {
              const activeDevices = response.filter(
                (device) => device.active !== false,
              );
              setAvailableDevices(activeDevices);
            }
          })
          .catch((err) => {
            if (!isMounted) return;
            console.error("Failed to load devices:", err);
          });
      } else {
        console.warn(
          "getFromOpenElisServerV2 returned undefined for /rest/storage/devices",
        );
      }
    }
    // Load shelves for rack parent selection
    if (locationType === "rack") {
      const promise = getFromOpenElisServerV2("/rest/storage/shelves");
      if (promise && typeof promise.then === "function") {
        promise
          .then((response) => {
            if (!isMounted) return;
            if (response && Array.isArray(response)) {
              const activeShelves = response.filter(
                (shelf) => shelf.active !== false,
              );
              setAvailableShelves(activeShelves);
            }
          })
          .catch((err) => {
            if (!isMounted) return;
            console.error("Failed to load shelves:", err);
          });
      } else {
        console.warn(
          "getFromOpenElisServerV2 returned undefined for /rest/storage/shelves",
        );
      }
    }

    return () => {
      isMounted = false;
    };
  }, [open, location, locationType]);

  // Check constraints when parent changes
  const checkParentChangeConstraints = (newParentId) => {
    if (!location || !location.id) return;

    setParentChangeWarning(null);
    setParentChangeAcknowledged(false);

    // Build correct parameter name based on location type
    let paramName = "";
    if (locationType === "device") {
      paramName = "newParentRoomId";
    } else if (locationType === "shelf") {
      paramName = "newParentDeviceId";
    } else if (locationType === "rack") {
      paramName = "newParentShelfId";
    }

    const endpoint = `/rest/storage/${getPluralType(locationType)}/${location.id}/can-move?${paramName}=${newParentId}`;
    getFromOpenElisServerV2(endpoint)
      .then((response) => {
        if (response && response.hasDownstreamSamples) {
          setParentChangeWarning({
            message:
              response.warning ||
              `Moving this ${locationType} will affect ${response.sampleCount} sample(s).`,
            sampleCount: response.sampleCount,
          });
        }
      })
      .catch((err) => {
        console.error("Failed to check constraints:", err);
        // Allow move even if check fails
      });
  };

  // Reset form when modal closes
  useEffect(() => {
    if (!open) {
      setFormData({});
      setError(null);
      setIsSubmitting(false);
      setOriginalCode("");
      setCodeChangeAcknowledged(false);
      setAvailableRooms([]);
      setAvailableDevices([]);
      setAvailableShelves([]);
      setSelectedParentRoomId(null);
      setSelectedParentDeviceId(null);
      setSelectedParentShelfId(null);
      setOriginalParentRoomId(null);
      setOriginalParentDeviceId(null);
      setOriginalParentShelfId(null);
      setParentChangeWarning(null);
      setParentChangeAcknowledged(false);
    }
  }, [open]);

  // Check if code has been changed from original
  const hasCodeChanged = () => {
    return originalCode !== "" && formData.code !== originalCode;
  };

  const handleFieldChange = (field, value) => {
    const normalizedValue = field === "active" ? Boolean(value) : value;
    setFormData((prev) => {
      const updated = { ...prev, [field]: normalizedValue };
      formDataRef.current = updated;
      return updated;
    });
    setError(null);
  };

  // Handle Enter key to submit form
  const handleKeyDown = (event) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      // Check if form is valid before submitting
      const isValid =
        (locationType === "room" && formData.name) ||
        (locationType === "device" && formData.name) ||
        (locationType === "shelf" && formData.label) ||
        (locationType === "rack" && formData.label);
      if (isValid && !isSubmitting && !isSaveDisabled()) {
        handleSave();
      }
    }
  };

  // Check if save should be disabled due to unacknowledged code change
  const isSaveDisabledDueToCodeChange = () => {
    return hasCodeChanged() && !codeChangeAcknowledged;
  };

  // Check if save should be disabled due to unacknowledged parent change
  const isSaveDisabledDueToParentChange = () => {
    if (!parentChangeWarning) return false;
    return !parentChangeAcknowledged;
  };

  // Check if save should be disabled
  const isSaveDisabled = () => {
    return isSaveDisabledDueToCodeChange() || isSaveDisabledDueToParentChange();
  };

  const handleSave = async () => {
    setIsSubmitting(true);
    setError(null);

    try {
      const latest = formDataRef.current || formData;
      // Build endpoint based on location type
      const endpoint = `/rest/storage/${getPluralType(locationType)}/${location.id}`;

      // Build payload with only editable fields
      const payload = {};
      if (locationType === "room") {
        payload.name = latest.name;
        payload.code = latest.code || null;
        payload.description = latest.description || null;
        payload.active = latest.active;
      } else if (locationType === "device") {
        payload.name = latest.name;
        payload.code = latest.code || null;
        payload.type = latest.type;
        payload.temperatureSetting = latest.temperatureSetting || null;
        payload.capacityLimit = latest.capacityLimit
          ? parseInt(latest.capacityLimit, 10)
          : null;
        payload.active = latest.active;
        // Always include parent room ID if selected (backend will handle if unchanged)
        if (selectedParentRoomId) {
          payload.parentRoomId = selectedParentRoomId;
        }
      } else if (locationType === "shelf") {
        payload.label = latest.label;
        payload.code = latest.code || null;
        payload.capacityLimit = latest.capacityLimit
          ? parseInt(latest.capacityLimit, 10)
          : null;
        payload.active = latest.active;
        // Always include parent device ID if selected (backend will handle if unchanged)
        if (selectedParentDeviceId) {
          payload.parentDeviceId = selectedParentDeviceId;
        }
      } else if (locationType === "rack") {
        payload.label = latest.label;
        payload.code = latest.code || null;
        payload.active = latest.active;
        // ALWAYS include parent shelf ID (required by backend @NotBlank validation)
        // Try multiple sources: selectedParentShelfId, originalParentShelfId, or location prop
        const parentShelfIdToUse =
          selectedParentShelfId ||
          originalParentShelfId ||
          location?.parentShelfId ||
          location?.parentShelf?.id;

        if (
          !parentShelfIdToUse ||
          parentShelfIdToUse === "" ||
          parentShelfIdToUse === "null" ||
          parentShelfIdToUse === "undefined"
        ) {
          // This should never happen - racks must have a parent shelf
          // But if it does, we need to fetch it from the existing rack
          throw new Error(
            `Cannot edit rack ${location.id}: parentShelfId is required but not found. ` +
              `This indicates a data integrity issue or the API response is missing parentShelfId.`,
          );
        }

        payload.parentShelfId = String(parentShelfIdToUse);
      }

      // Use fetch directly to get response body for error details
      try {
        const response = await fetch(config.serverBaseUrl + endpoint, {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          credentials: "include",
          body: JSON.stringify(payload),
        });

        setIsSubmitting(false);

        if (response.status >= 200 && response.status < 300) {
          // Success - fetch updated location using authenticated request
          getFromOpenElisServerV2(endpoint)
            .then((data) => {
              if (onSave) {
                onSave(data);
              }
              handleClose();
            })
            .catch((err) => {
              // Even if fetch fails, consider update successful if status is OK
              console.warn(
                "Failed to fetch updated location, using payload:",
                err,
              );
              if (onSave) {
                onSave(payload);
              }
              handleClose();
            });
        } else {
          // Error - extract error details from response body
          let errorMessage = `Failed to update location (status: ${response.status})`;

          // Try to extract error details from response body
          const contentType = response.headers.get("content-type");
          if (contentType && contentType.includes("application/json")) {
            try {
              const errorData = await response.json();
              if (errorData.error) {
                errorMessage = errorData.error;
              } else if (errorData.message) {
                errorMessage = errorData.message;
              } else if (errorData.fieldErrors) {
                // Handle field-specific errors
                const fieldErrors = Object.entries(errorData.fieldErrors)
                  .map(([field, msg]) => `${field}: ${msg}`)
                  .join(", ");
                errorMessage = `Validation errors: ${fieldErrors}`;
              }
            } catch (parseError) {
              console.warn("Failed to parse error response:", parseError);
            }
          }

          setError(errorMessage);
          throw new Error(errorMessage);
        }
      } catch (error) {
        setIsSubmitting(false);
        // If it's already our error message, use it; otherwise use generic
        if (error.message && error.message.startsWith("Failed to update")) {
          // Error already set above
        } else {
          setError(error.message || "Failed to update location");
        }
        throw error;
      }
    } catch (error) {
      setIsSubmitting(false);
      setError(error.message || "Failed to update location");
    }
  };

  const handleClose = () => {
    setFormData({});
    setError(null);
    setIsSubmitting(false);
    onClose();
  };

  // Handle Escape key to close modal (Carbon ComposedModal doesn't handle ESC automatically)
  useEffect(() => {
    const handleEscape = (event) => {
      if (event.key === "Escape" && open) {
        handleClose();
      }
    };

    if (open) {
      document.addEventListener("keydown", handleEscape);
    }

    return () => {
      document.removeEventListener("keydown", handleEscape);
    };
  }, [open, handleClose]);

  const deviceTypes = [
    { id: "freezer", label: "Freezer" },
    { id: "refrigerator", label: "Refrigerator" },
    { id: "cabinet", label: "Cabinet" },
    { id: "other", label: "Other" },
  ];

  // Use larger modal for device/rack (more fields)
  const modalSize =
    locationType === "device" || locationType === "rack" ? "lg" : "md";

  if (!open) {
    return null;
  }

  return (
    <ComposedModal
      open={open}
      onClose={handleClose}
      size={modalSize}
      data-testid="edit-location-modal"
      className="edit-location-modal"
    >
      <ModalHeader
        title={intl.formatMessage(
          {
            id: "storage.edit.location.type",
            defaultMessage: "Edit {type}",
          },
          { type: getLocationTypeName(locationType) },
        )}
      />
      <ModalBody>
        {error && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({
              id: "storage.error",
              defaultMessage: "Error",
            })}
            subtitle={error}
            lowContrast
            onClose={() => setError(null)}
          />
        )}

        {isLoading ? (
          <div className="edit-location-form">
            <SkeletonText heading width="40%" />
            <SkeletonText width="100%" />
            <SkeletonText width="100%" />
            <SkeletonText width="100%" />
            <SkeletonPlaceholder style={{ height: "48px" }} />
            <SkeletonPlaceholder style={{ height: "48px" }} />
          </div>
        ) : (
          <div className="edit-location-form" onKeyDown={handleKeyDown}>
            {/* Room fields */}
            {locationType === "room" && (
              <>
                <TextInput
                  id="room-name"
                  data-testid="edit-location-room-name"
                  labelText={intl.formatMessage({
                    id: "storage.location.name",
                    defaultMessage: "Name",
                  })}
                  value={formData.name || ""}
                  onChange={(e) => handleFieldChange("name", e.target.value)}
                  required
                />
                <TextInput
                  id="room-code"
                  data-testid="edit-location-room-code"
                  labelText={intl.formatMessage({
                    id: "storage.location.code",
                    defaultMessage: "Code",
                  })}
                  value={formData.code || ""}
                  onChange={(e) => {
                    // Auto-uppercase on input and limit to 10 chars
                    const value = e.target.value.toUpperCase().slice(0, 10);
                    handleFieldChange("code", value);
                  }}
                  maxLength={10}
                  helperText={intl.formatMessage({
                    id: "storage.location.code.helper",
                    defaultMessage:
                      "Max 10 characters, alphanumeric with hyphens/underscores",
                  })}
                />
                <TextArea
                  id="room-description"
                  data-testid="edit-location-room-description"
                  labelText={intl.formatMessage({
                    id: "storage.location.description",
                    defaultMessage: "Description",
                  })}
                  value={formData.description || ""}
                  onChange={(e) =>
                    handleFieldChange("description", e.target.value)
                  }
                  rows={3}
                />
                <Toggle
                  id="room-active"
                  data-testid="edit-location-room-active"
                  labelText={intl.formatMessage({
                    id: "storage.location.active",
                    defaultMessage: "Active",
                  })}
                  toggled={!!formData.active}
                  onToggle={(checked) => handleFieldChange("active", checked)}
                />
              </>
            )}

            {/* Device fields */}
            {locationType === "device" && (
              <>
                <TextInput
                  id="device-name"
                  data-testid="edit-location-device-name"
                  labelText={intl.formatMessage({
                    id: "storage.location.name",
                    defaultMessage: "Name",
                  })}
                  value={formData.name || ""}
                  onChange={(e) => handleFieldChange("name", e.target.value)}
                  required
                />
                <TextInput
                  id="device-code"
                  data-testid="edit-location-device-code"
                  labelText={intl.formatMessage({
                    id: "storage.location.code",
                    defaultMessage: "Code",
                  })}
                  value={formData.code || ""}
                  onChange={(e) => {
                    // Auto-uppercase on input and limit to 10 chars
                    const value = e.target.value.toUpperCase().slice(0, 10);
                    handleFieldChange("code", value);
                  }}
                  maxLength={10}
                  helperText={intl.formatMessage({
                    id: "storage.location.code.helper",
                    defaultMessage:
                      "Max 10 characters, alphanumeric with hyphens/underscores",
                  })}
                />
                <Dropdown
                  id="device-parent-room"
                  data-testid="edit-location-device-parent-room"
                  titleText={intl.formatMessage({
                    id: "storage.location.parent.room",
                    defaultMessage: "Parent Room",
                  })}
                  label={intl.formatMessage({
                    id: "storage.location.parent.room",
                    defaultMessage: "Parent Room",
                  })}
                  items={availableRooms}
                  selectedItem={(() => {
                    const found =
                      availableRooms.find(
                        (r) => String(r.id) === selectedParentRoomId,
                      ) || null;
                    return found;
                  })()}
                  onChange={({ selectedItem }) => {
                    if (selectedItem) {
                      const newParentId = String(selectedItem.id);
                      setSelectedParentRoomId(newParentId);
                      // Check constraints if parent changed
                      if (newParentId !== originalParentRoomId) {
                        checkParentChangeConstraints(newParentId);
                      } else {
                        setParentChangeWarning(null);
                        setParentChangeAcknowledged(false);
                      }
                    }
                  }}
                  itemToString={(item) => (item ? item.name : "")}
                />
                {parentChangeWarning && (
                  <InlineNotification
                    kind="warning"
                    title={intl.formatMessage({
                      id: "storage.parent.change.warning",
                      defaultMessage:
                        "Warning: Moving location affects samples",
                    })}
                    subtitle={parentChangeWarning.message}
                    lowContrast
                    hideCloseButton
                  />
                )}
                {parentChangeWarning && (
                  <Checkbox
                    id="acknowledge-parent-change"
                    labelText={intl.formatMessage({
                      id: "storage.parent.change.acknowledge",
                      defaultMessage:
                        "I understand that moving this location will affect the hierarchical path of assigned samples",
                    })}
                    checked={parentChangeAcknowledged}
                    onChange={(checked) => setParentChangeAcknowledged(checked)}
                  />
                )}
                <Dropdown
                  id="device-type"
                  data-testid="edit-location-device-type"
                  titleText={intl.formatMessage({
                    id: "storage.device.type",
                    defaultMessage: "Type",
                  })}
                  label={intl.formatMessage({
                    id: "storage.device.type",
                    defaultMessage: "Type",
                  })}
                  items={deviceTypes}
                  itemToString={(item) => (item ? item.label : "")}
                  onChange={({ selectedItem }) =>
                    handleFieldChange(
                      "type",
                      selectedItem ? selectedItem.id : "",
                    )
                  }
                  selectedItem={
                    deviceTypes.find((t) => t.id === formData.type) || null
                  }
                />
                <TextInput
                  id="device-temperature"
                  data-testid="edit-location-device-temperature"
                  labelText={intl.formatMessage({
                    id: "storage.device.temperature",
                    defaultMessage: "Temperature Setting",
                  })}
                  value={formData.temperatureSetting || ""}
                  onChange={(e) =>
                    handleFieldChange("temperatureSetting", e.target.value)
                  }
                  type="number"
                  invalid={
                    formData.temperatureSetting !== "" &&
                    isNaN(Number(formData.temperatureSetting))
                  }
                  invalidText={intl.formatMessage({
                    id: "storage.device.temperature.invalid",
                    defaultMessage: "Please enter a valid number",
                  })}
                />
                <TextInput
                  id="device-capacity"
                  data-testid="edit-location-device-capacity"
                  labelText={intl.formatMessage({
                    id: "storage.location.capacity",
                    defaultMessage: "Capacity Limit",
                  })}
                  value={formData.capacityLimit || ""}
                  onChange={(e) =>
                    handleFieldChange("capacityLimit", e.target.value)
                  }
                  type="number"
                />
                <Toggle
                  id="device-active"
                  data-testid="edit-location-device-active"
                  labelText={intl.formatMessage({
                    id: "storage.location.active",
                    defaultMessage: "Active",
                  })}
                  toggled={!!formData.active}
                  onToggle={(checked) => handleFieldChange("active", checked)}
                />
              </>
            )}

            {/* Shelf fields */}
            {locationType === "shelf" && (
              <>
                <TextInput
                  id="shelf-label"
                  data-testid="edit-location-shelf-label"
                  labelText={intl.formatMessage({
                    id: "storage.shelf.label",
                    defaultMessage: "Label",
                  })}
                  value={formData.label || ""}
                  onChange={(e) => handleFieldChange("label", e.target.value)}
                  required
                />
                <TextInput
                  id="shelf-code"
                  data-testid="edit-location-shelf-code"
                  labelText={intl.formatMessage({
                    id: "storage.location.code",
                    defaultMessage: "Code",
                  })}
                  value={formData.code || ""}
                  onChange={(e) => {
                    // Auto-uppercase on input and limit to 10 chars
                    const value = e.target.value.toUpperCase().slice(0, 10);
                    handleFieldChange("code", value);
                  }}
                  maxLength={10}
                  helperText={intl.formatMessage({
                    id: "storage.location.code.helper",
                    defaultMessage:
                      "Max 10 characters, alphanumeric with hyphens/underscores",
                  })}
                />
                <Dropdown
                  id="shelf-parent-device"
                  data-testid="edit-location-shelf-parent-device"
                  titleText={intl.formatMessage({
                    id: "storage.location.parent.device",
                    defaultMessage: "Parent Device",
                  })}
                  label={intl.formatMessage({
                    id: "storage.location.parent.device",
                    defaultMessage: "Parent Device",
                  })}
                  items={availableDevices}
                  selectedItem={
                    availableDevices.find(
                      (d) => String(d.id) === selectedParentDeviceId,
                    ) || null
                  }
                  onChange={({ selectedItem }) => {
                    if (selectedItem) {
                      const newParentId = String(selectedItem.id);
                      setSelectedParentDeviceId(newParentId);
                      // Check constraints if parent changed
                      if (newParentId !== originalParentDeviceId) {
                        checkParentChangeConstraints(newParentId);
                      } else {
                        setParentChangeWarning(null);
                        setParentChangeAcknowledged(false);
                      }
                    }
                  }}
                  itemToString={(item) => (item ? item.name : "")}
                />
                {parentChangeWarning && (
                  <InlineNotification
                    kind="warning"
                    title={intl.formatMessage({
                      id: "storage.parent.change.warning",
                      defaultMessage:
                        "Warning: Moving location affects samples",
                    })}
                    subtitle={parentChangeWarning.message}
                    lowContrast
                    hideCloseButton
                  />
                )}
                {parentChangeWarning && (
                  <Checkbox
                    id="acknowledge-parent-change"
                    labelText={intl.formatMessage({
                      id: "storage.parent.change.acknowledge",
                      defaultMessage:
                        "I understand that moving this location will affect the hierarchical path of assigned samples",
                    })}
                    checked={parentChangeAcknowledged}
                    onChange={(checked) => setParentChangeAcknowledged(checked)}
                  />
                )}
                <TextInput
                  id="shelf-capacity"
                  data-testid="edit-location-shelf-capacity"
                  labelText={intl.formatMessage({
                    id: "storage.location.capacity",
                    defaultMessage: "Capacity Limit",
                  })}
                  value={formData.capacityLimit || ""}
                  onChange={(e) =>
                    handleFieldChange("capacityLimit", e.target.value)
                  }
                  type="number"
                />
                <Toggle
                  id="shelf-active"
                  data-testid="edit-location-shelf-active"
                  labelText={intl.formatMessage({
                    id: "storage.location.active",
                    defaultMessage: "Active",
                  })}
                  toggled={!!formData.active}
                  onToggle={(checked) => handleFieldChange("active", checked)}
                />
              </>
            )}

            {/* Rack fields */}
            {locationType === "rack" && (
              <>
                <TextInput
                  id="rack-label"
                  data-testid="edit-location-rack-label"
                  labelText={intl.formatMessage({
                    id: "storage.rack.label",
                    defaultMessage: "Label",
                  })}
                  value={formData.label || ""}
                  onChange={(e) => handleFieldChange("label", e.target.value)}
                  required
                />
                <TextInput
                  id="rack-code"
                  data-testid="edit-location-rack-code"
                  labelText={intl.formatMessage({
                    id: "storage.location.code",
                    defaultMessage: "Code",
                  })}
                  value={formData.code || ""}
                  onChange={(e) => {
                    // Auto-uppercase on input and limit to 10 chars
                    const value = e.target.value.toUpperCase().slice(0, 10);
                    handleFieldChange("code", value);
                  }}
                  maxLength={10}
                  helperText={intl.formatMessage({
                    id: "storage.location.code.helper",
                    defaultMessage:
                      "Max 10 characters, alphanumeric with hyphens/underscores",
                  })}
                />
                <Dropdown
                  id="rack-parent-shelf"
                  data-testid="edit-location-rack-parent-shelf"
                  titleText={intl.formatMessage({
                    id: "storage.location.parent.shelf",
                    defaultMessage: "Parent Shelf",
                  })}
                  label={intl.formatMessage({
                    id: "storage.location.parent.shelf",
                    defaultMessage: "Parent Shelf",
                  })}
                  items={availableShelves}
                  selectedItem={
                    availableShelves.find(
                      (s) => String(s.id) === selectedParentShelfId,
                    ) || null
                  }
                  onChange={({ selectedItem }) => {
                    if (selectedItem) {
                      const newParentId = String(selectedItem.id);
                      setSelectedParentShelfId(newParentId);
                      // Check constraints if parent changed
                      if (newParentId !== originalParentShelfId) {
                        checkParentChangeConstraints(newParentId);
                      } else {
                        setParentChangeWarning(null);
                        setParentChangeAcknowledged(false);
                      }
                    }
                  }}
                  itemToString={(item) => (item ? item.label : "")}
                />
                {parentChangeWarning && (
                  <InlineNotification
                    kind="warning"
                    title={intl.formatMessage({
                      id: "storage.parent.change.warning",
                      defaultMessage:
                        "Warning: Moving location affects samples",
                    })}
                    subtitle={parentChangeWarning.message}
                    lowContrast
                    hideCloseButton
                  />
                )}
                {parentChangeWarning && (
                  <Checkbox
                    id="acknowledge-parent-change"
                    labelText={intl.formatMessage({
                      id: "storage.parent.change.acknowledge",
                      defaultMessage:
                        "I understand that moving this location will affect the hierarchical path of assigned samples",
                    })}
                    checked={parentChangeAcknowledged}
                    onChange={(checked) => setParentChangeAcknowledged(checked)}
                  />
                )}
                <TextInput
                  id="rack-rows"
                  data-testid="edit-location-rack-rows"
                  labelText={intl.formatMessage({
                    id: "storage.rack.rows",
                    defaultMessage: "Rows",
                  })}
                  value={formData.rows || ""}
                  onChange={(e) =>
                    handleFieldChange("rows", parseInt(e.target.value) || 0)
                  }
                  type="number"
                  min="0"
                  required
                />
                <TextInput
                  id="rack-columns"
                  data-testid="edit-location-rack-columns"
                  labelText={intl.formatMessage({
                    id: "storage.rack.columns",
                    defaultMessage: "Columns",
                  })}
                  value={formData.columns || ""}
                  onChange={(e) =>
                    handleFieldChange("columns", parseInt(e.target.value) || 0)
                  }
                  type="number"
                  min="0"
                  required
                />
                <TextInput
                  id="rack-position-schema"
                  data-testid="edit-location-rack-position-schema"
                  labelText={intl.formatMessage({
                    id: "storage.rack.position.schema",
                    defaultMessage: "Position Schema Hint",
                  })}
                  value={formData.positionSchemaHint || ""}
                  onChange={(e) =>
                    handleFieldChange("positionSchemaHint", e.target.value)
                  }
                />
                <Toggle
                  id="rack-active"
                  data-testid="edit-location-rack-active"
                  labelText={intl.formatMessage({
                    id: "storage.location.active",
                    defaultMessage: "Active",
                  })}
                  toggled={!!formData.active}
                  onToggle={(checked) => handleFieldChange("active", checked)}
                />
              </>
            )}

            {/* Inline warning when code has been changed */}
            {hasCodeChanged() && (
              <div style={{ marginTop: "1rem", marginBottom: "1rem" }}>
                <InlineNotification
                  kind="error"
                  title={intl.formatMessage({
                    id: "storage.code.change.warning",
                    defaultMessage: "Warning",
                  })}
                  subtitle={intl.formatMessage({
                    id: "storage.code.change.warning.message",
                    defaultMessage:
                      "Changing the code will invalidate any previously printed labels for this location.",
                  })}
                  lowContrast
                  hideCloseButton
                />
                <Checkbox
                  id="code-change-acknowledge"
                  data-testid="code-change-acknowledge-checkbox"
                  labelText={intl.formatMessage({
                    id: "storage.code.change.acknowledge",
                    defaultMessage:
                      "I understand and want to proceed with the code change",
                  })}
                  checked={codeChangeAcknowledged}
                  onChange={(_, { checked }) =>
                    setCodeChangeAcknowledged(checked)
                  }
                  style={{ marginTop: "0.5rem" }}
                />
              </div>
            )}
          </div>
        )}
      </ModalBody>
      <ModalFooter>
        <Button
          kind="secondary"
          onClick={handleClose}
          disabled={isSubmitting || isLoading}
          data-testid="edit-location-cancel-button"
        >
          <FormattedMessage id="label.button.cancel" defaultMessage="Cancel" />
        </Button>
        <Button
          kind="primary"
          onClick={handleSave}
          disabled={
            isSubmitting ||
            isLoading ||
            isSaveDisabled() ||
            (locationType === "room" && !formData.name) ||
            (locationType === "device" && !formData.name) ||
            (locationType === "shelf" && !formData.label) ||
            (locationType === "rack" && !formData.label)
          }
          data-testid="edit-location-save-button"
        >
          <FormattedMessage
            id="storage.save.changes"
            defaultMessage="Save Changes"
          />
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default EditLocationModal;

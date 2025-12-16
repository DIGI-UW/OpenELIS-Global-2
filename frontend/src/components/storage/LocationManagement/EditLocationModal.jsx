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

  // Reset form when modal closes
  useEffect(() => {
    if (!open) {
      setFormData({});
      setError(null);
      setIsSubmitting(false);
      setOriginalCode("");
      setCodeChangeAcknowledged(false);
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
        (locationType === "rack" &&
          formData.label &&
          formData.rows &&
          formData.columns);
      if (isValid && !isSubmitting && !isSaveDisabledDueToCodeChange()) {
        handleSave();
      }
    }
  };

  // Check if save should be disabled due to unacknowledged code change
  const isSaveDisabledDueToCodeChange = () => {
    return hasCodeChanged() && !codeChangeAcknowledged;
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
      } else if (locationType === "shelf") {
        payload.label = latest.label;
        payload.code = latest.code || null;
        payload.capacityLimit = latest.capacityLimit
          ? parseInt(latest.capacityLimit, 10)
          : null;
        payload.active = latest.active;
      } else if (locationType === "rack") {
        payload.label = latest.label;
        payload.code = latest.code || null;
        payload.rows = latest.rows;
        payload.columns = latest.columns;
        payload.positionSchemaHint = latest.positionSchemaHint || null;
        payload.active = latest.active;
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
  const modalSize = locationType === "device" || locationType === "rack" ? "lg" : "md";

  // Prevent hidden-but-mounted modal DOM from causing duplicate IDs across modals
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
                <TextInput
                  id="device-parent-room"
                  data-testid="edit-location-device-parent-room"
                  labelText={intl.formatMessage({
                    id: "storage.location.parent.room",
                    defaultMessage: "Parent Room",
                  })}
                  value={
                    formData.parentRoomName ||
                    (location && location.parentRoomName) ||
                    (location && location.parentRoom?.name) ||
                    (location && location.roomName) ||
                    ""
                  }
                  disabled
                  readOnly
                />
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
                <TextInput
                  id="shelf-parent-device"
                  data-testid="edit-location-shelf-parent-device"
                  labelText={intl.formatMessage({
                    id: "storage.location.parent.device",
                    defaultMessage: "Parent Device",
                  })}
                  value={
                    formData.parentDeviceName ||
                    (location && location.parentDeviceName) ||
                    (location && location.parentDevice?.name) ||
                    (location && location.deviceName) ||
                    ""
                  }
                  disabled
                  readOnly
                />
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
                <TextInput
                  id="rack-parent-shelf"
                  data-testid="edit-location-rack-parent-shelf"
                  labelText={intl.formatMessage({
                    id: "storage.location.parent.shelf",
                    defaultMessage: "Parent Shelf",
                  })}
                  value={
                    formData.parentShelfLabel ||
                    (location && location.parentShelfLabel) ||
                    (location && location.parentShelf?.label) ||
                    (location && location.shelfLabel) ||
                    ""
                  }
                  disabled
                  readOnly
                />
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
            isSaveDisabledDueToCodeChange() ||
            (locationType === "room" && !formData.name) ||
            (locationType === "device" && !formData.name) ||
            (locationType === "shelf" && !formData.label) ||
            (locationType === "rack" &&
              (!formData.label || !formData.rows || !formData.columns))
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

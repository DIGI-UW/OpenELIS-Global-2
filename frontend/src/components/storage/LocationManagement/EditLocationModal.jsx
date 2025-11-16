import React, { useState, useEffect } from "react";
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
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  putToOpenElisServer,
  getFromOpenElisServerV2,
} from "../../utils/Utils";
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

  // Helper function to normalize active value to boolean
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
    return {
      name: loc.name || "",
      code: loc.code || "",
      description: loc.description || "",
      active: normalizeActive(loc.active),
      type: loc.type || "",
      temperatureSetting: loc.temperatureSetting || "",
      capacityLimit: loc.capacityLimit || "",
      label: loc.label || "",
      rows: loc.rows || "",
      columns: loc.columns || "",
      positionSchemaHint: loc.positionSchemaHint || "",
    };
  };

  // Initialize form data when modal opens or location changes
  // First initialize from location prop (synchronous), then fetch full data from API
  useEffect(() => {
    let isMounted = true;

    if (open && location && location.id && locationType) {
      // Initialize immediately from location prop to avoid undefined values
      setFormData(initializeFormDataFromLocation(location));
      setIsLoading(true);
      setError(null);

      // Fetch full location data from API when modal opens
      const endpoint = `/rest/storage/${getPluralType(locationType)}/${location.id}`;
      getFromOpenElisServerV2(endpoint)
        .then((fullLocation) => {
          // Only update state if component is still mounted
          if (!isMounted) return;

          if (fullLocation) {
            setFormData({
              name: fullLocation.name || "",
              code: fullLocation.code || "",
              description: fullLocation.description || "",
              // Ensure active is properly initialized as boolean
              active: normalizeActive(fullLocation.active),
              type: fullLocation.type || "",
              temperatureSetting: fullLocation.temperatureSetting || "",
              capacityLimit: fullLocation.capacityLimit || "",
              label: fullLocation.label || "",
              rows: fullLocation.rows || "",
              columns: fullLocation.columns || "",
              positionSchemaHint: fullLocation.positionSchemaHint || "",
            });
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
          // Keep the formData that was initialized from location prop
          // (already set above, so no need to set again)
          setError("Failed to load location data");
          setIsLoading(false);
        });
    } else if (location && !open) {
      // Reset when modal closes
      setFormData({});
      setIsLoading(false);
    } else if (!location) {
      // Initialize with empty values to avoid uncontrolled component warnings
      setFormData({
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
      });
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
    }
  }, [open]);

  const handleFieldChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    setError(null);
  };

  const handleSave = async () => {
    setIsSubmitting(true);
    setError(null);

    try {
      // Build endpoint based on location type
      const endpoint = `/rest/storage/${getPluralType(locationType)}/${location.id}`;

      // Build payload with only editable fields
      const payload = {};
      if (locationType === "room") {
        payload.name = formData.name;
        payload.description = formData.description || null;
        payload.active = formData.active;
      } else if (locationType === "device") {
        payload.name = formData.name;
        payload.type = formData.type;
        payload.temperatureSetting = formData.temperatureSetting || null;
        payload.capacityLimit = formData.capacityLimit || null;
        payload.active = formData.active;
      } else if (locationType === "shelf") {
        payload.label = formData.label;
        payload.capacityLimit = formData.capacityLimit || null;
        payload.active = formData.active;
      } else if (locationType === "rack") {
        payload.label = formData.label;
        payload.rows = formData.rows;
        payload.columns = formData.columns;
        payload.positionSchemaHint = formData.positionSchemaHint || null;
        payload.active = formData.active;
      }

      // Use putToOpenElisServer utility
      await new Promise((resolve, reject) => {
        putToOpenElisServer(endpoint, JSON.stringify(payload), (status) => {
          setIsSubmitting(false);
          if (status >= 200 && status < 300) {
            // Success - fetch updated location
            fetch(`${window.location.origin}${endpoint}`)
              .then((res) => res.json())
              .then((data) => {
                if (onSave) {
                  onSave(data);
                }
                handleClose();
                resolve(data);
              })
              .catch((err) => {
                // Even if fetch fails, consider update successful if status is OK
                if (onSave) {
                  onSave(payload);
                }
                handleClose();
                resolve(payload);
              });
          } else {
            // Error
            const errorMessage = `Failed to update location (status: ${status})`;
            setError(errorMessage);
            reject(new Error(errorMessage));
          }
        });
      });
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

  const deviceTypes = [
    { id: "freezer", label: "Freezer" },
    { id: "refrigerator", label: "Refrigerator" },
    { id: "cabinet", label: "Cabinet" },
    { id: "other", label: "Other" },
  ];

  return (
    <ComposedModal
      open={open}
      onClose={handleClose}
      size="md"
      data-testid="edit-location-modal"
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

        <div className="edit-location-form">
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
                disabled
                readOnly
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
                toggled={formData.active === true}
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
                disabled
                readOnly
              />
              <TextInput
                id="device-parent-room"
                data-testid="edit-location-device-parent-room"
                labelText={intl.formatMessage({
                  id: "storage.location.parent.room",
                  defaultMessage: "Parent Room",
                })}
                value={
                  (location && location.parentRoom?.name) ||
                  (location && location.roomName) ||
                  (location && location.parentRoomName) ||
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
                  handleFieldChange("type", selectedItem ? selectedItem.id : "")
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
                toggled={formData.active === true}
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
                id="shelf-parent-device"
                data-testid="edit-location-shelf-parent-device"
                labelText={intl.formatMessage({
                  id: "storage.location.parent.device",
                  defaultMessage: "Parent Device",
                })}
                value={
                  (location && location.parentDevice?.name) ||
                  (location && location.deviceName) ||
                  (location && location.parentDeviceName) ||
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
                toggled={formData.active === true}
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
                id="rack-parent-shelf"
                data-testid="edit-location-rack-parent-shelf"
                labelText={intl.formatMessage({
                  id: "storage.location.parent.shelf",
                  defaultMessage: "Parent Shelf",
                })}
                value={
                  (location && location.parentShelf?.label) ||
                  (location && location.shelfLabel) ||
                  (location && location.parentShelfLabel) ||
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
                toggled={formData.active === true}
                onToggle={(checked) => handleFieldChange("active", checked)}
              />
            </>
          )}
        </div>
      </ModalBody>
      <ModalFooter>
        <Button
          kind="secondary"
          onClick={handleClose}
          disabled={isSubmitting}
          data-testid="edit-location-cancel-button"
        >
          <FormattedMessage id="label.button.cancel" defaultMessage="Cancel" />
        </Button>
        <Button
          kind="primary"
          onClick={handleSave}
          disabled={
            isSubmitting ||
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

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
import { putToOpenElisServer } from "../../utils/Utils";
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
  const [formData, setFormData] = useState({});
  const [error, setError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Initialize form data when location changes
  useEffect(() => {
    if (location) {
      setFormData({
        name: location.name || "",
        code: location.code || "",
        description: location.description || "",
        active: location.active !== undefined ? location.active : true,
        type: location.type || "",
        temperatureSetting: location.temperatureSetting || "",
        capacityLimit: location.capacityLimit || "",
        label: location.label || "",
        rows: location.rows || "",
        columns: location.columns || "",
        positionSchemaHint: location.positionSchemaHint || "",
      });
      setError(null);
    } else {
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
    }
  }, [location]);

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
      const endpoint = `/rest/storage/${locationType}s/${location.id}`;

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
        putToOpenElisServer(
          endpoint,
          JSON.stringify(payload),
          (status) => {
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
          },
        );
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
        title={intl.formatMessage({
          id: "storage.edit.location",
          defaultMessage: "Edit Location",
        })}
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
                toggled={formData.active}
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
                  location.parentRoom?.name ||
                  location.roomName ||
                  location.parentRoomName ||
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
                toggled={formData.active}
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
                  location.parentDevice?.name ||
                  location.deviceName ||
                  location.parentDeviceName ||
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
                toggled={formData.active}
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
                  location.parentShelf?.label ||
                  location.shelfLabel ||
                  location.parentShelfLabel ||
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
                onChange={(e) => handleFieldChange("rows", parseInt(e.target.value) || 0)}
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
                onChange={(e) => handleFieldChange("columns", parseInt(e.target.value) || 0)}
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
                toggled={formData.active}
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
          disabled={isSubmitting || !formData.name}
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


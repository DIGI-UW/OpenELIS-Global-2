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
  NumberInput,
  Stack,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { StorageLocationAPI } from "./InventoryService";

const StorageLocationModal = ({ open, onClose, onSave }) => {
  const intl = useIntl();

  const [formData, setFormData] = useState({
    name: "",
    locationCode: "",
    locationType: "ROOM",
    description: "",
    temperatureMin: -80,
    temperatureMax: -20,
    parentLocation: null,
  });

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  // Reset form when modal opens
  useEffect(() => {
    if (open) {
      setFormData({
        name: "",
        locationCode: "",
        locationType: "ROOM",
        description: "",
        temperatureMin: -80,
        temperatureMax: -20,
        parentLocation: null,
      });
      setError(null);
    }
  }, [open]);

  const locationTypes = [
    { id: "ROOM", text: "Room" },
    { id: "REFRIGERATOR", text: "Refrigerator" },
    { id: "FREEZER", text: "Freezer" },
    { id: "SHELF", text: "Shelf" },
    { id: "DRAWER", text: "Drawer" },
    { id: "CABINET", text: "Cabinet" },
  ];

  const handleFieldChange = (fieldName, fieldValue) => {
    setFormData((prev) => ({ ...prev, [fieldName]: fieldValue }));
    setError(null);
  };

  const validate = () => {
    if (!formData.name?.trim()) {
      setError("Please enter a location name");
      return false;
    }

    if (!formData.locationType) {
      setError("Please select a location type");
      return false;
    }

    if (formData.temperatureMin && formData.temperatureMax) {
      const min = parseFloat(formData.temperatureMin);
      const max = parseFloat(formData.temperatureMax);
      if (!isNaN(min) && !isNaN(max) && min > max) {
        setError(
          "Minimum temperature cannot be greater than maximum temperature",
        );
        return false;
      }
    }

    return true;
  };

  const handleSubmit = async () => {
    if (!validate()) return;

    setSaving(true);
    setError(null);

    try {
      const payload = {
        name: formData.name.trim(),
        locationCode: formData.locationCode?.trim() || null,
        locationType: formData.locationType,
        description: formData.description?.trim() || null,
        temperatureMin: formData.temperatureMin
          ? parseFloat(formData.temperatureMin)
          : null,
        temperatureMax: formData.temperatureMax
          ? parseFloat(formData.temperatureMax)
          : null,
        parentLocation: formData.parentLocation,
        isActive: true, // Boolean, not "Y"
      };

      const newLocation = await StorageLocationAPI.create(payload);
      onSave(newLocation);
    } catch (err) {
      console.error("Error creating storage location:", err);
      setError(err.message || "Error creating storage location");
    } finally {
      setSaving(false);
    }
  };

  return (
    <ComposedModal
      open={open}
      onClose={onClose}
      size="md"
      SelectorPrimaryFocus="#storage-location-name"
      preventCloseOnClickOutside
    >
      <ModalHeader
        title={intl.formatMessage({ id: "storage.location.add.title" })}
      />
      <ModalBody>
        <Stack gap={5}>
          <TextInput
            id="storage-location-name"
            labelText={
              <>
                {intl.formatMessage({ id: "storage.location.name" })}
                <span style={{ color: "#da1e28" }}> *</span>
              </>
            }
            placeholder="e.g., Cold Storage Room 1"
            value={formData.name}
            onChange={(e) => handleFieldChange("name", e.target.value)}
            invalid={error && !formData.name?.trim()}
          />

          <TextInput
            id="storage-location-code"
            labelText={intl.formatMessage({ id: "storage.location.code" })}
            placeholder="e.g., ROOM-001 (optional)"
            value={formData.locationCode}
            onChange={(e) => handleFieldChange("locationCode", e.target.value)}
          />

          <Dropdown
            id="storage-location-type"
            titleText={
              <>
                {intl.formatMessage({ id: "storage.location.type" })}
                <span style={{ color: "#da1e28" }}> *</span>
              </>
            }
            label="Select location type"
            items={locationTypes}
            itemToString={(item) => (item ? item.text : "")}
            selectedItem={locationTypes.find(
              (t) => t.id === formData.locationType,
            )}
            onChange={({ selectedItem }) =>
              handleFieldChange("locationType", selectedItem?.id)
            }
          />

          <div
            style={{
              display: "grid",
              gridTemplateColumns: "1fr 1fr",
              gap: "1rem",
            }}
          >
            <NumberInput
              id="storage-location-temp-min"
              label={intl.formatMessage({ id: "storage.location.tempMin" })}
              value={formData.temperatureMin}
              onChange={(e, { value }) =>
                handleFieldChange("temperatureMin", value)
              }
              min={-200}
              max={200}
              placeholder="-80"
              helperText="°C"
              allowEmpty
            />
            <NumberInput
              id="storage-location-temp-max"
              label={intl.formatMessage({ id: "storage.location.tempMax" })}
              value={formData.temperatureMax}
              onChange={(e, { value }) =>
                handleFieldChange("temperatureMax", value)
              }
              min={-200}
              max={200}
              placeholder="-20"
              helperText="°C"
              allowEmpty
            />
          </div>

          <TextArea
            id="storage-location-description"
            labelText={intl.formatMessage({
              id: "storage.location.description",
            })}
            placeholder="Optional description or notes"
            value={formData.description}
            onChange={(e) => handleFieldChange("description", e.target.value)}
            rows={3}
          />

          {error && (
            <div className="error-message" style={{ color: "#da1e28" }}>
              {error}
            </div>
          )}
        </Stack>
      </ModalBody>
      <ModalFooter>
        <Button kind="secondary" onClick={onClose}>
          {intl.formatMessage({ id: "button.cancel" })}
        </Button>
        <Button kind="primary" onClick={handleSubmit} disabled={saving}>
          {intl.formatMessage({ id: "button.save" })}
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default StorageLocationModal;

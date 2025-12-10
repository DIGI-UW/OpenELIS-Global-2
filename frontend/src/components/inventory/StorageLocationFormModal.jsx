import React, { useState, useEffect } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Form,
  Stack,
  TextInput,
  Select,
  SelectItem,
  NumberInput,
  TextArea,
  Button,
  InlineNotification,
} from "@carbon/react";
import { Temperature } from "@carbon/icons-react";
import { FormattedMessage } from "react-intl";
import { StorageLocationAPI } from "./InventoryService";

const LOCATION_TYPE_OPTIONS = [
  { value: "ROOM", label: "Room", icon: "🏢" },
  { value: "REFRIGERATOR", label: "Refrigerator", icon: "❄️" },
  { value: "FREEZER", label: "Freezer", icon: "🧊" },
  { value: "CABINET", label: "Cabinet", icon: "🗄️" },
  { value: "SHELF", label: "Shelf", icon: "📚" },
  { value: "DRAWER", label: "Drawer", icon: "🗃️" },
];

const HIERARCHY_RULES = {
  ROOM: [],
  REFRIGERATOR: ["ROOM"],
  FREEZER: ["ROOM"],
  CABINET: ["ROOM"],
  SHELF: ["REFRIGERATOR", "FREEZER", "CABINET"],
  DRAWER: ["REFRIGERATOR", "FREEZER", "CABINET"],
};

const INITIAL_FORM_DATA = {
  name: "",
  locationCode: "",
  locationType: "", // Empty - force user to select
  description: "",
  temperatureMin: 2,
  temperatureMax: 8,
  parentLocationId: "", // Add parent selection
};

export default function StorageLocationFormModal({
  isOpen,
  onClose,
  onSubmit,
  mode = "create",
  location = null,
  parentLocation = null,
}) {
  const [formData, setFormData] = useState(INITIAL_FORM_DATA);
  const [error, setError] = useState(null);
  const [availableParents, setAvailableParents] = useState([]);

  useEffect(() => {
    if (isOpen) {
      if (mode === "edit" && location) {
        setFormData({
          name: location.name || "",
          locationCode: location.locationCode || "",
          locationType: location.locationType || "",
          description: location.description || "",
          temperatureMin: location.temperatureMin ?? 2,
          temperatureMax: location.temperatureMax ?? 8,
          parentLocationId: location.parentLocation?.id || "",
        });
        loadAvailableParents(location.locationType, location.id);
      } else if (mode === "create" && parentLocation) {
        let defaultChildType = "SHELF";
        if (parentLocation.locationType === "ROOM") {
          defaultChildType = "REFRIGERATOR";
        }
        setFormData({
          ...INITIAL_FORM_DATA,
          locationType: defaultChildType,
          parentLocationId: parentLocation.id,
        });
        loadAvailableParents(defaultChildType, null);
      } else {
        setFormData(INITIAL_FORM_DATA);
        setAvailableParents([]);
      }
      setError(null);
    }
  }, [isOpen, mode, location, parentLocation]);

  const loadAvailableParents = async (locationType, excludeId = null) => {
    const allowedParentTypes = HIERARCHY_RULES[locationType] || [];

    if (allowedParentTypes.length === 0) {
      setAvailableParents([]);
      return;
    }

    try {
      const allLocations = await StorageLocationAPI.getAll();
      const validParents = allLocations.filter(
        (loc) =>
          allowedParentTypes.includes(loc.locationType) && loc.id !== excludeId,
      );
      setAvailableParents(validParents);
    } catch (err) {
      console.error("Error loading parents:", err);
      setAvailableParents([]);
    }
  };

  const handleChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));

    if (field === "locationType") {
      loadAvailableParents(value, location?.id);
    }
  };

  const handleSubmit = async () => {
    if (!formData.name.trim()) {
      setError("Location name is required");
      return;
    }

    if (!formData.locationType) {
      setError("Location type is required");
      return;
    }

    // Determine parent location
    let parentLocationObj = null;
    if (parentLocation) {
      // If created from tree (Add Child button)
      parentLocationObj = { id: parentLocation.id };
    } else if (formData.parentLocationId) {
      // If parent selected from dropdown
      parentLocationObj = { id: formData.parentLocationId };
    } else if (mode === "edit" && location?.parentLocation) {
      // If editing and already has parent, keep it
      parentLocationObj = location.parentLocation;
    }

    const payload = {
      name: formData.name.trim(),
      locationCode: formData.locationCode?.trim() || null,
      locationType: formData.locationType,
      description: formData.description?.trim() || null,
      temperatureMin:
        formData.temperatureMin !== ""
          ? parseFloat(formData.temperatureMin)
          : null,
      temperatureMax:
        formData.temperatureMax !== ""
          ? parseFloat(formData.temperatureMax)
          : null,
      parentLocation: parentLocationObj,
      isActive: true,
    };

    try {
      if (mode === "edit" && location) {
        await StorageLocationAPI.update(location.id, payload);
      } else {
        await StorageLocationAPI.create(payload);
      }
      onSubmit();
      onClose();
    } catch (err) {
      console.error("Error saving location:", err);
      setError(err.message || "Failed to save location");
    }
  };

  const getLocationIcon = (locationType) => {
    const option = LOCATION_TYPE_OPTIONS.find(
      (opt) => opt.value === locationType,
    );
    return option?.icon || "📍";
  };

  const getLocationLabel = (locationType) => {
    const option = LOCATION_TYPE_OPTIONS.find(
      (opt) => opt.value === locationType,
    );
    return option?.label || locationType;
  };

  return (
    <ComposedModal open={isOpen} onClose={onClose} size="md">
      <ModalHeader
        title={
          mode === "create"
            ? "Create Storage Location"
            : "Edit Storage Location"
        }
        closeModal={onClose}
      />

      <ModalBody>
        {error && (
          <InlineNotification
            kind="error"
            title="Error"
            subtitle={error}
            onCloseButtonClick={() => setError(null)}
            style={{ marginBottom: "1rem" }}
          />
        )}

        {parentLocation && (
          <InlineNotification
            kind="info"
            title="Parent Location"
            subtitle={`Creating child location under: ${parentLocation.name} (${getLocationLabel(parentLocation.locationType)})`}
            hideCloseButton
            lowContrast
            style={{ marginBottom: "1rem" }}
          />
        )}

        <Form>
          <Stack gap={6}>
            <TextInput
              id="name-input"
              labelText="Location Name"
              placeholder="e.g., Main Lab Refrigerator"
              value={formData.name}
              onChange={(e) => handleChange("name", e.target.value)}
              required
            />

            <TextInput
              id="code-input"
              labelText="Location Code"
              placeholder="e.g., LAB-FRIDGE-01"
              value={formData.locationCode}
              onChange={(e) => handleChange("locationCode", e.target.value)}
              helperText="Optional unique identifier"
            />

            <Select
              id="type-select"
              labelText="Location Type"
              value={formData.locationType}
              onChange={(e) => handleChange("locationType", e.target.value)}
              required
            >
              <SelectItem value="" text="Select a location type..." />
              {LOCATION_TYPE_OPTIONS.map((opt) => (
                <SelectItem
                  key={opt.value}
                  value={opt.value}
                  text={`${opt.icon} ${opt.label}`}
                />
              ))}
            </Select>

            {formData.locationType !== "ROOM" &&
              availableParents.length === 0 &&
              !parentLocation && (
                <InlineNotification
                  kind="warning"
                  title="No valid parent locations"
                  subtitle={`You need to create a ${HIERARCHY_RULES[formData.locationType]?.join(" or ")} before you can add this type of location.`}
                  hideCloseButton
                  lowContrast
                />
              )}

            {formData.locationType !== "ROOM" &&
              formData.locationType !== "" &&
              availableParents.length > 0 &&
              !parentLocation && (
                <Select
                  id="parent-select"
                  labelText="Parent Location (Optional)"
                  value={formData.parentLocationId}
                  onChange={(e) =>
                    handleChange("parentLocationId", e.target.value)
                  }
                  helperText={`Select a ${HIERARCHY_RULES[formData.locationType]?.join(" or ")} as parent location`}
                >
                  <SelectItem value="" text="No parent (top-level location)" />
                  {availableParents.map((parent) => (
                    <SelectItem
                      key={parent.id}
                      value={parent.id}
                      text={`${getLocationIcon(parent.locationType)} ${parent.name} (${getLocationLabel(parent.locationType)})`}
                    />
                  ))}
                </Select>
              )}

            <div
              style={{
                padding: "1rem",
                backgroundColor: "#f4f4f4",
                borderRadius: "4px",
              }}
            >
              <h5
                style={{
                  margin: "0 0 1rem 0",
                  fontSize: "0.875rem",
                  fontWeight: 600,
                  display: "flex",
                  alignItems: "center",
                  gap: "0.5rem",
                }}
              >
                <Temperature size={16} />
                Temperature Settings (Optional)
              </h5>
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr",
                  gap: "1rem",
                }}
              >
                <NumberInput
                  id="temp-min"
                  label="Minimum (°C)"
                  value={formData.temperatureMin}
                  onChange={(e, { value }) =>
                    handleChange("temperatureMin", value)
                  }
                  min={-200}
                  max={200}
                  step={1}
                />
                <NumberInput
                  id="temp-max"
                  label="Maximum (°C)"
                  value={formData.temperatureMax}
                  onChange={(e, { value }) =>
                    handleChange("temperatureMax", value)
                  }
                  min={-200}
                  max={200}
                  step={1}
                />
              </div>
            </div>

            <TextArea
              id="desc-input"
              labelText="Description"
              placeholder="Optional description or notes"
              value={formData.description}
              onChange={(e) => handleChange("description", e.target.value)}
              rows={3}
            />
          </Stack>
        </Form>
      </ModalBody>

      <ModalFooter>
        <Button kind="secondary" onClick={onClose}>
          Cancel
        </Button>
        <Button
          kind="primary"
          onClick={handleSubmit}
          disabled={
            !formData.name.trim() ||
            !formData.locationType ||
            (formData.locationType !== "ROOM" &&
              availableParents.length === 0 &&
              !parentLocation &&
              !formData.parentLocationId &&
              !location?.parentLocation)
          }
        >
          {mode === "create" ? "Create" : "Save"}
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
}

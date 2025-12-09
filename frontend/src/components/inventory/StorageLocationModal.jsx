import React, { useState, useEffect } from "react";
import {
  Modal,
  Form,
  Stack,
  TextInput,
  Select,
  SelectItem,
  FormLabel,
  NumberInput,
  TextArea,
} from "@carbon/react";

const LOCATION_TYPE_OPTIONS = [
  { value: "ROOM", label: "Room" },
  { value: "REFRIGERATOR", label: "Refrigerator" },
  { value: "FREEZER", label: "Freezer" },
  { value: "SHELF", label: "Shelf" },
  { value: "DRAWER", label: "Drawer" },
  { value: "CABINET", label: "Cabinet" },
];

const INITIAL_FORM_DATA = {
  name: "",
  locationCode: "",
  locationType: "ROOM",
  description: "",
  temperatureMin: -80,
  temperatureMax: 25,
  parentLocation: null,
};

export default function StorageLocationModal({
  isOpen,
  onClose,
  onSubmit,
  editingLocation = null,
}) {
  const [formData, setFormData] = useState(INITIAL_FORM_DATA);

  useEffect(() => {
    if (editingLocation) {
      setFormData({
        ...INITIAL_FORM_DATA,
        ...editingLocation,
      });
    } else {
      setFormData(INITIAL_FORM_DATA);
    }
  }, [editingLocation, isOpen]);

  const handleFormChange = (field, value) => {
    console.log("handleFormChange called:", field, value);
    setFormData((prev) => {
      const newData = { ...prev, [field]: value };
      console.log("New formData:", newData);
      return newData;
    });
  };

  const handleSubmit = () => {
    const payload = {
      name: formData.name.trim(),
      locationCode: formData.locationCode?.trim() || null,
      locationType: formData.locationType,
      description: formData.description?.trim() || null,
      temperatureMin:
        formData.temperatureMin !== "" && formData.temperatureMin !== null
          ? parseFloat(formData.temperatureMin)
          : null,
      temperatureMax:
        formData.temperatureMax !== "" && formData.temperatureMax !== null
          ? parseFloat(formData.temperatureMax)
          : null,
      parentLocation: formData.parentLocation,
      isActive: true,
    };
    onSubmit(payload);
  };

  const isValid = formData.name && formData.locationType;

  return (
    <Modal
      open={isOpen}
      onRequestClose={onClose}
      onRequestSubmit={handleSubmit}
      modalHeading={
        editingLocation ? "Edit Storage Location" : "Add New Storage Location"
      }
      primaryButtonText={editingLocation ? "Update" : "Create"}
      secondaryButtonText="Cancel"
      primaryButtonDisabled={!isValid}
      size="sm"
    >
      <Form>
        <Stack gap={5}>
          <div>
            <FormLabel
              style={{
                marginBottom: "1rem",
                fontSize: "0.875rem",
                fontWeight: "600",
                color: "#161616",
              }}
            >
              Basic Information
            </FormLabel>
            <Stack gap={5}>
              <TextInput
                id="name"
                labelText="Location Name *"
                placeholder="e.g., Cold Storage Room 1"
                value={formData.name}
                onChange={(e) => handleFormChange("name", e.target.value)}
                required
              />

              <TextInput
                id="locationCode"
                labelText="Location Code"
                placeholder="e.g., ROOM-001 (optional)"
                value={formData.locationCode}
                onChange={(e) =>
                  handleFormChange("locationCode", e.target.value)
                }
              />

              <Select
                id="locationType"
                labelText="Location Type *"
                value={formData.locationType}
                onChange={(e) =>
                  handleFormChange("locationType", e.target.value)
                }
                required
              >
                {LOCATION_TYPE_OPTIONS.map((opt) => (
                  <SelectItem
                    key={opt.value}
                    value={opt.value}
                    text={opt.label}
                  />
                ))}
              </Select>
            </Stack>
          </div>

          <div
            style={{
              borderTop: "1px solid #e0e0e0",
              paddingTop: "1rem",
              marginTop: "0.5rem",
            }}
          >
            <FormLabel
              style={{
                marginBottom: "1rem",
                fontSize: "0.875rem",
                fontWeight: "600",
                color: "#161616",
              }}
            >
              Temperature Settings
            </FormLabel>
            <Stack gap={5}>
              <NumberInput
                id="temperatureMin"
                label="Minimum Temperature (°C)"
                helperText="Minimum safe temperature for this location"
                value={formData.temperatureMin}
                onChange={(e, { value }) =>
                  handleFormChange("temperatureMin", value)
                }
                min={-200}
                max={200}
                step={1}
              />

              <NumberInput
                id="temperatureMax"
                label="Maximum Temperature (°C)"
                helperText="Maximum safe temperature for this location"
                value={formData.temperatureMax}
                onChange={(e, { value }) =>
                  handleFormChange("temperatureMax", value)
                }
                min={-200}
                max={200}
                step={1}
              />
            </Stack>
          </div>

          <div
            style={{
              borderTop: "1px solid #e0e0e0",
              paddingTop: "1rem",
              marginTop: "0.5rem",
            }}
          >
            <FormLabel
              style={{
                marginBottom: "1rem",
                fontSize: "0.875rem",
                fontWeight: "600",
                color: "#161616",
              }}
            >
              Additional Details
            </FormLabel>
            <TextArea
              id="description"
              labelText="Description"
              placeholder="Optional description or notes about this location"
              value={formData.description}
              onChange={(e) => handleFormChange("description", e.target.value)}
              rows={3}
            />
          </div>
        </Stack>
      </Form>
    </Modal>
  );
}

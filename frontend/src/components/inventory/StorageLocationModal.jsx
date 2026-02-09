import { useState } from "react";
import {
  Modal,
  TextInput,
  TextArea,
  Dropdown,
  NumberInput,
  Stack,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { StorageLocationAPI } from "./InventoryService";

const StorageLocationModal = ({ open, onClose, onSave }) => {
  const intl = useIntl();

  const [formData, setFormData] = useState({
    name: "",
    locationCode: "",
    locationType: "ROOM",
    description: "",
    temperatureMin: "",
    temperatureMax: "",
    parentLocation: null,
  });

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const locationTypes = [
    { id: "ROOM", text: "Room" },
    { id: "REFRIGERATOR", text: "Refrigerator" },
    { id: "FREEZER", text: "Freezer" },
    { id: "SHELF", text: "Shelf" },
    { id: "DRAWER", text: "Drawer" },
    { id: "CABINET", text: "Cabinet" },
  ];

  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
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
        isActive: true,
      };

      const newLocation = await StorageLocationAPI.create(payload);

      setFormData({
        name: "",
        locationCode: "",
        locationType: "ROOM",
        description: "",
        temperatureMin: "",
        temperatureMax: "",
        parentLocation: null,
      });

      onSave(newLocation);
    } catch (err) {
      console.error("Error creating storage location:", err);
      setError(
        err.message ||
          "Error creating storage location. Please check the console for details.",
      );
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    setFormData({
      name: "",
      locationCode: "",
      locationType: "ROOM",
      description: "",
      temperatureMin: "",
      temperatureMax: "",
      parentLocation: null,
    });
    setError(null);
    onClose();
  };

  return (
    <Modal
      open={open}
      onRequestClose={handleCancel}
      onRequestSubmit={handleSubmit}
      modalHeading={intl.formatMessage({
        id: "storage.location.add.title",
        defaultMessage: "Add Storage Location",
      })}
      primaryButtonText={intl.formatMessage({
        id: "button.save",
        defaultMessage: "Save",
      })}
      secondaryButtonText={intl.formatMessage({
        id: "button.cancel",
        defaultMessage: "Cancel",
      })}
      primaryButtonDisabled={saving}
      size="md"
    >
      <Stack gap={5}>
        <TextInput
          id="name"
          labelText={
            <>
              {intl.formatMessage({
                id: "storage.location.name",
                defaultMessage: "Location Name",
              })}
              <span style={{ color: "#da1e28" }}> *</span>
            </>
          }
          placeholder="e.g., Cold Storage Room 1"
          value={formData.name}
          onChange={(e) => handleChange("name", e.target.value)}
          invalid={error && !formData.name?.trim()}
        />

        <TextInput
          id="locationCode"
          labelText={intl.formatMessage({
            id: "storage.location.code",
            defaultMessage: "Location Code",
          })}
          placeholder="e.g., ROOM-001 (optional)"
          value={formData.locationCode}
          onChange={(e) => handleChange("locationCode", e.target.value)}
        />

        <Dropdown
          id="locationType"
          titleText={
            <>
              {intl.formatMessage({
                id: "storage.location.type",
                defaultMessage: "Location Type",
              })}
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
            handleChange("locationType", selectedItem?.id)
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
            id="temperatureMin"
            label={intl.formatMessage({
              id: "storage.location.tempMin",
              defaultMessage: "Min Temperature",
            })}
            value={formData.temperatureMin}
            onChange={(e, { value }) => handleChange("temperatureMin", value)}
            min={-200}
            max={200}
            placeholder="-80"
            helperText="°C"
            allowEmpty
          />
          <NumberInput
            id="temperatureMax"
            label={intl.formatMessage({
              id: "storage.location.tempMax",
              defaultMessage: "Max Temperature",
            })}
            value={formData.temperatureMax}
            onChange={(e, { value }) => handleChange("temperatureMax", value)}
            min={-200}
            max={200}
            placeholder="-20"
            helperText="°C"
            allowEmpty
          />
        </div>

        <TextArea
          id="description"
          labelText={intl.formatMessage({
            id: "storage.location.description",
            defaultMessage: "Description",
          })}
          placeholder="Optional description or notes"
          value={formData.description}
          onChange={(e) => handleChange("description", e.target.value)}
          rows={3}
        />

        {error && (
          <div className="error-message" style={{ color: "#da1e28" }}>
            {error}
          </div>
        )}
      </Stack>
    </Modal>
  );
};

export default StorageLocationModal;

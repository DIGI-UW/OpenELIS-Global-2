import React, { useState, useEffect } from "react";
import {
  Modal,
  Form,
  FormGroup,
  TextInput,
  Stack,
  DatePicker,
  DatePickerInput,
  InlineNotification,
  Select,
  SelectItem,
} from "@carbon/react";
import { FormattedMessage } from "react-intl";
import { EquipmentAPI } from "../EquipmentUsageService";

const EquipmentModal = ({ isOpen, onClose, equipment, isNew, onSubmit }) => {
  const [formData, setFormData] = useState({
    name: "",
    serialNumber: "",
    department: "",
    manufacturer: "",
    modelNumber: "",
    purchaseDate: "",
    lastCalibrationDate: "",
    nextCalibrationDue: "",
    isActive: "Y",
  });

  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (equipment && !isNew) {
      setFormData({
        name: equipment.name || "",
        serialNumber: equipment.serialNumber || "",
        department: equipment.department || "",
        manufacturer: equipment.manufacturer || "",
        modelNumber: equipment.modelNumber || "",
        purchaseDate: equipment.purchaseDate
          ? equipment.purchaseDate.split("T")[0]
          : "",
        lastCalibrationDate: equipment.lastCalibrationDate
          ? equipment.lastCalibrationDate.split("T")[0]
          : "",
        nextCalibrationDue: equipment.nextCalibrationDue
          ? equipment.nextCalibrationDue.split("T")[0]
          : "",
        isActive: equipment.isActive || "Y",
      });
    } else {
      setFormData({
        name: "",
        serialNumber: "",
        department: "",
        manufacturer: "",
        modelNumber: "",
        purchaseDate: "",
        lastCalibrationDate: "",
        nextCalibrationDue: "",
        isActive: "Y",
      });
    }
    setError(null);
  }, [equipment, isNew, isOpen]);

  const handleInputChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
    setError(null);
  };

  const handleSubmit = async () => {
    try {
      // Validation
      if (!formData.name.trim()) {
        setError("Equipment name is required");
        return;
      }
      if (!formData.serialNumber.trim()) {
        setError("Serial number is required");
        return;
      }

      setLoading(true);

      const submitData = {
        ...formData,
      };

      if (isNew) {
        await EquipmentAPI.create(submitData);
      } else {
        await EquipmentAPI.update(equipment.id, submitData);
      }

      onSubmit();
    } catch (err) {
      setError(err.message || "Failed to save equipment");
      console.error("Error saving equipment:", err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      open={isOpen}
      modalHeading={isNew ? "Add Equipment" : "Edit Equipment"}
      primaryButtonText="Save"
      secondaryButtonText="Cancel"
      onRequestSubmit={handleSubmit}
      onRequestClose={onClose}
      size="lg"
    >
      {error && (
        <InlineNotification
          title="Error"
          subtitle={error}
          kind="error"
          onClose={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      <Form>
        <FormGroup legendText="Basic Information">
          <Stack gap={4}>
            <TextInput
              id="equipment-name"
              labelText="Equipment Name"
              value={formData.name}
              onChange={(e) => handleInputChange("name", e.target.value)}
              placeholder="e.g., Centrifuge, Analyzer"
              required
            />

            <TextInput
              id="serial-number"
              labelText="Serial Number"
              value={formData.serialNumber}
              onChange={(e) =>
                handleInputChange("serialNumber", e.target.value)
              }
              placeholder="e.g., CENT-2024-001"
              required
            />

            <TextInput
              id="department"
              labelText="Department"
              value={formData.department}
              onChange={(e) => handleInputChange("department", e.target.value)}
              placeholder="e.g., Hematology, Chemistry"
            />
          </Stack>
        </FormGroup>

        <FormGroup legendText="Manufacturer Information">
          <Stack gap={4}>
            <TextInput
              id="manufacturer"
              labelText="Manufacturer"
              value={formData.manufacturer}
              onChange={(e) =>
                handleInputChange("manufacturer", e.target.value)
              }
              placeholder="e.g., Beckman Coulter, Siemens"
            />

            <TextInput
              id="model-number"
              labelText="Model Number"
              value={formData.modelNumber}
              onChange={(e) => handleInputChange("modelNumber", e.target.value)}
              placeholder="e.g., Allegra X-15R"
            />
          </Stack>
        </FormGroup>

        <FormGroup legendText="Dates">
          <Stack gap={4}>
            <DatePicker
              datePickerType="single"
              value={formData.purchaseDate}
              onChange={(dates) => {
                if (dates && dates[0]) {
                  handleInputChange("purchaseDate", dates[0]);
                }
              }}
            >
              <DatePickerInput
                id="purchase-date"
                placeholder="mm/dd/yyyy"
                labelText="Purchase Date"
              />
            </DatePicker>

            <DatePicker
              datePickerType="single"
              value={formData.lastCalibrationDate}
              onChange={(dates) => {
                if (dates && dates[0]) {
                  handleInputChange("lastCalibrationDate", dates[0]);
                }
              }}
            >
              <DatePickerInput
                id="last-calibration-date"
                placeholder="mm/dd/yyyy"
                labelText="Last Calibration Date"
              />
            </DatePicker>

            <DatePicker
              datePickerType="single"
              value={formData.nextCalibrationDue}
              onChange={(dates) => {
                if (dates && dates[0]) {
                  handleInputChange("nextCalibrationDue", dates[0]);
                }
              }}
            >
              <DatePickerInput
                id="next-calibration-due"
                placeholder="mm/dd/yyyy"
                labelText="Next Calibration Due"
              />
            </DatePicker>
          </Stack>
        </FormGroup>

        <FormGroup legendText="Status">
          <Stack gap={4}>
            <Select
              id="is-active"
              labelText="Status"
              value={formData.isActive}
              onChange={(e) => handleInputChange("isActive", e.target.value)}
            >
              <SelectItem value="Y" text="Active" />
              <SelectItem value="N" text="Inactive" />
            </Select>
          </Stack>
        </FormGroup>
      </Form>
    </Modal>
  );
};

export default EquipmentModal;

import React, { useState, useEffect, useContext } from "react";
import {
  Modal,
  Form,
  FormGroup,
  TextInput,
  Stack,
  DatePicker,
  DatePickerInput,
  Select,
  SelectItem,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { EquipmentAPI } from "../EquipmentUsageService";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";

const EquipmentModal = ({ isOpen, onClose, equipment, isNew, onSubmit }) => {
  const intl = useIntl();
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

  const [loading, setLoading] = useState(false);
  const { addNotification, setNotificationVisible } = useContext(NotificationContext);

  // Helper function to safely format date strings for API format (YYYY-MM-DD)
  const formatDateForInput = (dateValue) => {
    if (!dateValue) return "";

    // If it's already a string in YYYY-MM-DD format, return as is
    if (typeof dateValue === "string" && /^\d{4}-\d{2}-\d{2}$/.test(dateValue)) {
      return dateValue;
    }

    // If it's a string with time component, extract date part
    if (typeof dateValue === "string" && dateValue.includes("T")) {
      return dateValue.split("T")[0];
    }

    // If it's a Date object, format it
    if (dateValue instanceof Date) {
      return dateValue.toISOString().split("T")[0];
    }

    // If it's a string that can be parsed as a date
    if (typeof dateValue === "string") {
      try {
        const parsedDate = new Date(dateValue);
        if (!isNaN(parsedDate.getTime())) {
          return parsedDate.toISOString().split("T")[0];
        }
      } catch (e) {
        console.warn("Could not parse date:", dateValue);
      }
    }

    // Fallback to empty string
    return "";
  };

  // Helper function to convert date string to Date object for DatePicker
  const convertToDateObject = (dateString) => {
    if (!dateString) return null;
    const [year, month, day] = dateString.split("-");
    return new Date(year, parseInt(month) - 1, day);
  };

  useEffect(() => {
    if (equipment && !isNew) {
      setFormData({
        name: equipment.name || "",
        serialNumber: equipment.serialNumber || "",
        department: equipment.department || "",
        manufacturer: equipment.manufacturer || "",
        modelNumber: equipment.modelNumber || "",
        purchaseDate: formatDateForInput(equipment.purchaseDate),
        lastCalibrationDate: formatDateForInput(equipment.lastCalibrationDate),
        nextCalibrationDue: formatDateForInput(equipment.nextCalibrationDue),
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
  }, [equipment, isNew, isOpen]);

  const handleInputChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleSubmit = async () => {
    try {
      // Validation
      if (!formData.name.trim()) {
        addNotification({
          title: intl.formatMessage({ id: "notification.title", defaultMessage: "Error" }),
          message: intl.formatMessage({
            id: "equipment.name.required",
            defaultMessage: "Equipment name is required"
          }),
          kind: NotificationKinds.error,
        });
        setNotificationVisible(true);
        return;
      }
      if (!formData.serialNumber.trim()) {
        addNotification({
          title: intl.formatMessage({ id: "notification.title", defaultMessage: "Error" }),
          message: intl.formatMessage({
            id: "equipment.serialNumber.required",
            defaultMessage: "Serial number is required"
          }),
          kind: NotificationKinds.error,
        });
        setNotificationVisible(true);
        return;
      }

      setLoading(true);

      // Format date fields to YYYY-MM-DD for LocalDate fields
      const formatDateForAPI = (dateValue) => {
        if (!dateValue) return null;
        if (dateValue instanceof Date) {
          return dateValue.toISOString().split('T')[0]; // Convert to YYYY-MM-DD
        }
        if (typeof dateValue === 'string' && dateValue.includes('T')) {
          return dateValue.split('T')[0]; // Extract date part from datetime string
        }
        return dateValue;
      };

      const submitData = {
        ...formData,
        purchaseDate: formatDateForAPI(formData.purchaseDate),
        lastCalibrationDate: formatDateForAPI(formData.lastCalibrationDate),
        nextCalibrationDue: formatDateForAPI(formData.nextCalibrationDue),
      };

      if (isNew) {
        await EquipmentAPI.create(submitData);
        addNotification({
          title: intl.formatMessage({ id: "notification.title", defaultMessage: "Success" }),
          message: intl.formatMessage({
            id: "equipment.create.success",
            defaultMessage: "Equipment created successfully"
          }),
          kind: NotificationKinds.success,
        });
      } else {
        await EquipmentAPI.update(equipment.id, submitData);
        addNotification({
          title: intl.formatMessage({ id: "notification.title", defaultMessage: "Success" }),
          message: intl.formatMessage({
            id: "equipment.update.success",
            defaultMessage: "Equipment updated successfully"
          }),
          kind: NotificationKinds.success,
        });
      }

      setNotificationVisible(true);
      onSubmit();
    } catch (err) {
      console.error("Error saving equipment:", err);
      addNotification({
        title: intl.formatMessage({ id: "notification.title", defaultMessage: "Error" }),
        message: err.message || intl.formatMessage({
          id: "equipment.save.error",
          defaultMessage: "Failed to save equipment"
        }),
        kind: NotificationKinds.error,
      });
      setNotificationVisible(true);
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
      <Form>
        <FormGroup legendText="Required Information">
          <Stack gap={4}>
            <TextInput
              id="equipment-name"
              labelText="Equipment Name *"
              value={formData.name}
              onChange={(e) => handleInputChange("name", e.target.value)}
              placeholder="e.g., Centrifuge, Analyzer"
              required
            />

            <TextInput
              id="serial-number"
              labelText="Serial Number *"
              value={formData.serialNumber}
              onChange={(e) =>
                handleInputChange("serialNumber", e.target.value)
              }
              placeholder="e.g., CENT-2024-001"
              required
            />

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

        <FormGroup legendText="Optional Information">
          <Stack gap={4}>
            <TextInput
              id="department"
              labelText="Department"
              value={formData.department}
              onChange={(e) => handleInputChange("department", e.target.value)}
              placeholder="e.g., Hematology, Chemistry"
            />

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

        <FormGroup legendText="Optional Dates">
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
      </Form>
    </Modal>
  );
};

export default EquipmentModal;

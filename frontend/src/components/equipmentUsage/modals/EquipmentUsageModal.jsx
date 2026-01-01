import React, { useState, useEffect, useContext } from "react";
import {
  Modal,
  Form,
  FormGroup,
  Select,
  SelectItem,
  TextInput,
  TextArea,
  DatePickerInput,
  DatePicker,
  TimePicker,
  Stack,
  Loading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import EquipmentUsageService from "../EquipmentUsageService";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";

const EquipmentUsageModal = ({ isOpen, onClose, entry, isNew, onSubmit }) => {
  const intl = useIntl();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [formData, setFormData] = useState({
    equipment: null,
    operatorName: "",
    usageDate: new Date().toISOString().split("T")[0],
    startTime: "",
    endTime: "",
    activitiesDone: "",
    equipmentStatus: "FUNCTIONAL",
  });

  const [equipment, setEquipment] = useState([]);
  const [loading, setLoading] = useState(false);
  const [isMounted, setIsMounted] = useState(true);

  useEffect(() => {
    if (userSessionDetails?.firstName && userSessionDetails?.lastName) {
      setFormData((prev) => ({
        ...prev,
        operatorName: `${userSessionDetails.firstName} ${userSessionDetails.lastName}`,
      }));
    }
    loadEquipment();

    // Cleanup function to prevent memory leaks
    return () => {
      setIsMounted(false);
    };
  }, []);

  // Helper function to safely format date strings
  const formatDateForInput = (dateValue) => {
    if (!dateValue) return "";

    // If it's already a string in YYYY-MM-DD format, return as is
    if (
      typeof dateValue === "string" &&
      /^\d{4}-\d{2}-\d{2}$/.test(dateValue)
    ) {
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

  useEffect(() => {
    if (entry && !isNew) {
      setFormData({
        equipment: entry.equipment,
        operatorName: entry.operatorName || "",
        usageDate: formatDateForInput(entry.loginTime),
        startTime: entry.loginTime?.split("T")[1] || "",
        endTime: entry.logoutTime?.split("T")[1] || "",
        activitiesDone: entry.activitiesDone || "",
        equipmentStatus: entry.equipmentStatus || "FUNCTIONAL",
      });
    } else if (isNew) {
      // Reset form for new entry
      setFormData({
        equipment: null,
        operatorName:
          userSessionDetails?.firstName && userSessionDetails?.lastName
            ? `${userSessionDetails.firstName} ${userSessionDetails.lastName}`
            : "",
        usageDate: new Date().toISOString().split("T")[0],
        startTime: "",
        endTime: "",
        activitiesDone: "",
        equipmentStatus: "FUNCTIONAL",
      });
    }
  }, [entry, isNew, isOpen, userSessionDetails]);

  const loadEquipment = async () => {
    if (!isMounted) return;

    setLoading(true);
    try {
      const data = await EquipmentUsageService.getEquipmentForDropdown();
      if (isMounted) {
        setEquipment(data || []);
      }
    } catch (err) {
      console.error("Error loading equipment:", err);
      if (isMounted) {
        addNotification({
          title: intl.formatMessage({
            id: "notification.title",
            defaultMessage: "Error",
          }),
          message:
            err.message ||
            intl.formatMessage({
              id: "equipment.load.error",
              defaultMessage: "Failed to load equipment list",
            }),
          kind: NotificationKinds.error,
        });
        setNotificationVisible(true);
      }
    } finally {
      if (isMounted) {
        setLoading(false);
      }
    }
  };

  const handleInputChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleEquipmentChange = (equipmentId) => {
    const selected = equipment.find((eq) => eq.id === parseInt(equipmentId));
    setFormData((prev) => ({
      ...prev,
      equipment: selected,
    }));
  };

  const handleSubmit = async () => {
    try {
      // Validation
      if (!formData.equipment) {
        addNotification({
          title: intl.formatMessage({
            id: "notification.title",
            defaultMessage: "Error",
          }),
          message: intl.formatMessage({
            id: "equipment.usage.equipment.required",
            defaultMessage: "Please select an equipment",
          }),
          kind: NotificationKinds.error,
        });
        setNotificationVisible(true);
        return;
      }
      if (!formData.operatorName?.trim()) {
        addNotification({
          title: intl.formatMessage({
            id: "notification.title",
            defaultMessage: "Error",
          }),
          message: intl.formatMessage({
            id: "equipment.usage.operator.required",
            defaultMessage: "Operator name is required",
          }),
          kind: NotificationKinds.error,
        });
        setNotificationVisible(true);
        return;
      }
      if (!formData.usageDate) {
        addNotification({
          title: intl.formatMessage({
            id: "notification.title",
            defaultMessage: "Error",
          }),
          message: intl.formatMessage({
            id: "equipment.usage.date.required",
            defaultMessage: "Usage date is required",
          }),
          kind: NotificationKinds.error,
        });
        setNotificationVisible(true);
        return;
      }

      // Prepare submit data
      const submitData = {
        equipment: {
          id: formData.equipment.id,
        },
        operatorName: formData.operatorName.trim(),
        loginTime: formData.startTime
          ? `${formData.usageDate}T${formData.startTime}`
          : formData.usageDate,
        logoutTime: formData.endTime
          ? `${formData.usageDate}T${formData.endTime}`
          : null,
        activitiesDone: formData.activitiesDone?.trim() || "",
        equipmentStatus: formData.equipmentStatus,
        department: formData.equipment.department || "",
        entryStatus: "DRAFT",
      };

      onSubmit(submitData);
    } catch (err) {
      console.error("Error submitting usage entry:", err);
      addNotification({
        title: intl.formatMessage({
          id: "notification.title",
          defaultMessage: "Error",
        }),
        message:
          err.message ||
          intl.formatMessage({
            id: "equipment.usage.submit.error",
            defaultMessage: "Failed to submit usage entry",
          }),
        kind: NotificationKinds.error,
      });
      setNotificationVisible(true);
    }
  };

  if (loading) {
    return <Loading />;
  }

  return (
    <Modal
      open={isOpen}
      modalHeading={
        isNew ? "New Equipment Usage Entry" : "Edit Equipment Usage Entry"
      }
      primaryButtonText="Save Draft"
      secondaryButtonText="Cancel"
      onRequestSubmit={handleSubmit}
      onRequestClose={onClose}
      size="lg"
    >
      <Form>
        <FormGroup legendText="Equipment & Operator Information">
          <Stack gap={4}>
            <Select
              id="equipment"
              labelText="Equipment Name *"
              placeholder="Select equipment..."
              value={formData.equipment?.id || ""}
              onChange={(e) => handleEquipmentChange(e.target.value)}
              required
            >
              <SelectItem value="" text="Please select equipment..." />
              {equipment.map((eq) => (
                <SelectItem
                  key={eq.id}
                  value={eq.id.toString()}
                  text={eq.name}
                />
              ))}
            </Select>

            <TextInput
              id="operator-name"
              labelText="Operator Name *"
              placeholder="Enter operator's full name"
              value={formData.operatorName}
              onChange={(e) =>
                handleInputChange("operatorName", e.target.value)
              }
              required
            />

            <TextInput
              id="department"
              labelText="Department"
              placeholder="Auto-filled from equipment"
              value={formData.equipment?.department || ""}
              readOnly
              disabled
            />
          </Stack>
        </FormGroup>

        <FormGroup legendText="Date & Time">
          <Stack gap={4}>
            <DatePicker
              datePickerType="single"
              value={formData.usageDate}
              onChange={(dates) => {
                if (dates && dates[0]) {
                  handleInputChange("usageDate", dates[0]);
                }
              }}
            >
              <DatePickerInput
                id="usage-date"
                placeholder="mm/dd/yyyy"
                labelText="Usage Date *"
                required
              />
            </DatePicker>

            <TimePicker
              id="start-time"
              labelText="Start Time"
              placeholder="hh:mm"
              value={formData.startTime}
              onChange={(event) => {
                handleInputChange("startTime", event.target.value);
              }}
            />

            <TimePicker
              id="end-time"
              labelText="End Time"
              placeholder="hh:mm"
              value={formData.endTime}
              onChange={(event) => {
                handleInputChange("endTime", event.target.value);
              }}
            />
          </Stack>
        </FormGroup>

        <FormGroup legendText="Activities & Equipment Status">
          <Stack gap={4}>
            <TextArea
              id="activities"
              labelText="Activities Performed"
              placeholder="Describe activities performed, procedures completed, samples processed, etc."
              value={formData.activitiesDone}
              onChange={(e) =>
                handleInputChange("activitiesDone", e.target.value)
              }
              rows={4}
              maxLength={1000}
            />

            <Select
              id="equipment-status"
              labelText="Equipment Status After Use *"
              value={formData.equipmentStatus}
              onChange={(e) =>
                handleInputChange("equipmentStatus", e.target.value)
              }
              required
            >
              <SelectItem value="FUNCTIONAL" text="Functional" />
              <SelectItem value="UNDER_MAINTENANCE" text="Under Maintenance" />
              <SelectItem value="FAULTY" text="Faulty" />
              <SelectItem
                value="CALIBRATION_REQUIRED"
                text="Calibration Required"
              />
            </Select>
          </Stack>
        </FormGroup>
      </Form>
    </Modal>
  );
};

export default EquipmentUsageModal;

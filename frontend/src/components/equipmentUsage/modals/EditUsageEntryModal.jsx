import React, { useState, useEffect, useContext } from "react";
import {
  Modal,
  Form,
  TextArea,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  TimePicker,
  Stack,
  Grid,
  Column,
} from "@carbon/react";
import { Edit, Time } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";

const EditUsageEntryModal = ({ isOpen, onClose, entry, onSubmit }) => {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } = useContext(NotificationContext);

  const [formData, setFormData] = useState({
    loginTime: "",
    logoutTime: "",
    activitiesDone: "",
    equipmentStatus: "FUNCTIONAL",
  });

  const [loading, setLoading] = useState(false);

  // Helper function to safely format date strings for date picker
  const formatDateForInput = (dateValue) => {
    if (!dateValue) return "";

    if (typeof dateValue === "string" && /^\d{4}-\d{2}-\d{2}$/.test(dateValue)) {
      return dateValue;
    }

    if (typeof dateValue === "string" && dateValue.includes("T")) {
      return dateValue.split("T")[0];
    }

    if (dateValue instanceof Date) {
      return dateValue.toISOString().split("T")[0];
    }

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

    return "";
  };

  // Helper function to extract time from datetime string
  const extractTime = (dateValue) => {
    if (!dateValue) return "";

    if (typeof dateValue === "string" && dateValue.includes("T")) {
      const timePart = dateValue.split("T")[1];
      if (timePart) {
        return timePart.split(".")[0]; // Remove milliseconds if present
      }
    }

    if (dateValue instanceof Date) {
      return dateValue.toTimeString().split(" ")[0];
    }

    return "";
  };

  useEffect(() => {
    if (entry && isOpen) {
      setFormData({
        loginTime: formatDateForInput(entry.loginTime),
        logoutTime: formatDateForInput(entry.logoutTime),
        activitiesDone: entry.activitiesDone || "",
        equipmentStatus: entry.equipmentStatus || "FUNCTIONAL",
      });
    }
  }, [entry, isOpen]);

  const handleInputChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleTimeChange = (field, timeValue) => {
    const dateValue = field === "loginTime" ? formData.loginTime : formData.logoutTime;
    if (dateValue && timeValue) {
      const newDateTime = `${dateValue}T${timeValue}`;
      handleInputChange(field, newDateTime);
    }
  };

  const handleSubmit = async () => {
    try {
      if (!formData.loginTime) {
        addNotification({
          title: intl.formatMessage({ id: "notification.title", defaultMessage: "Error" }),
          message: intl.formatMessage({
            id: "equipment.usage.loginTime.required",
            defaultMessage: "Login time is required"
          }),
          kind: NotificationKinds.error,
        });
        setNotificationVisible(true);
        return;
      }

      setLoading(true);

      const submitData = {
        ...entry,
        loginTime: formData.loginTime,
        logoutTime: formData.logoutTime,
        activitiesDone: formData.activitiesDone,
        equipmentStatus: formData.equipmentStatus,
        entryStatus: "DRAFT", // Keep as draft
      };

      onSubmit(submitData);
    } catch (err) {
      console.error("Error submitting usage entry:", err);
      addNotification({
        title: intl.formatMessage({ id: "notification.title", defaultMessage: "Error" }),
        message: err.message || intl.formatMessage({
          id: "equipment.usage.submit.error",
          defaultMessage: "Failed to submit usage entry"
        }),
        kind: NotificationKinds.error,
      });
      setNotificationVisible(true);
    } finally {
      setLoading(false);
    }
  };

  if (!entry) return null;

  return (
    <Modal
      open={isOpen}
      modalHeading={
        <div style={{ display: "flex", alignItems: "center", gap: "0.75rem" }}>
          <Edit size={24} />
          <span>
            <FormattedMessage
              id="equipment.usage.modal.title.edit"
              defaultMessage="Edit Usage Entry"
            />
          </span>
        </div>
      }
      primaryButtonText={
        <FormattedMessage id="common.button.save" defaultMessage="Save Changes" />
      }
      secondaryButtonText={
        <FormattedMessage id="common.button.cancel" defaultMessage="Cancel" />
      }
      onRequestSubmit={handleSubmit}
      onRequestClose={onClose}
      size="md"
      className="edit-usage-entry-modal"
    >
      {/* Equipment info header */}
      <div
        style={{
          marginBottom: "1.5rem",
          padding: "1rem",
          backgroundColor: "#f0f7ff",
          borderRadius: "6px",
        }}
      >
        <h4 style={{ margin: "0 0 0.5rem 0", fontSize: "1rem", fontWeight: "600" }}>
          <FormattedMessage
            id="equipment.usage.edit.equipment.info"
            defaultMessage="Equipment Information"
          />
        </h4>
        <p style={{ margin: "0", fontSize: "0.875rem", color: "#525252" }}>
          <strong>{entry.equipment?.name || "—"}</strong> •
          <FormattedMessage
            id="equipment.usage.edit.operator"
            defaultMessage="Operator"
          />: {entry.operatorName || "—"}
        </p>
      </div>

      <Form>
        <Grid fullWidth={true}>
          {/* Date and Time Section */}
          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              value={formData.loginTime}
              onChange={(dates) => {
                if (dates && dates[0]) {
                  handleInputChange("loginTime", dates[0]);
                }
              }}
            >
              <DatePickerInput
                id="edit-login-date"
                placeholder="mm/dd/yyyy"
                labelText={
                  <FormattedMessage
                    id="equipment.usage.edit.date"
                    defaultMessage="Usage Date"
                  />
                }
                required
              />
            </DatePicker>
          </Column>

          <Column lg={4} md={2} sm={2}>
            <div>
              <label
                htmlFor="edit-login-time"
                style={{
                  display: "block",
                  marginBottom: "0.5rem",
                  fontSize: "0.875rem",
                  fontWeight: 400,
                  color: "#393939",
                }}
              >
                <Time
                  size={16}
                  style={{
                    marginRight: "0.5rem",
                    verticalAlign: "text-bottom",
                  }}
                />
                <FormattedMessage
                  id="equipment.usage.edit.login.time"
                  defaultMessage="Start Time"
                />
                <span style={{ color: "#da1e28", marginLeft: "0.25rem" }}>*</span>
              </label>
              <TimePicker
                id="edit-login-time"
                labelText=""
                value={extractTime(entry.loginTime) || ""}
                onChange={(e) => {
                  handleTimeChange("loginTime", e.target.value);
                }}
              />
            </div>
          </Column>

          <Column lg={4} md={2} sm={2}>
            <div>
              <label
                htmlFor="edit-logout-time"
                style={{
                  display: "block",
                  marginBottom: "0.5rem",
                  fontSize: "0.875rem",
                  fontWeight: 400,
                  color: "#393939",
                }}
              >
                <Time
                  size={16}
                  style={{
                    marginRight: "0.5rem",
                    verticalAlign: "text-bottom",
                  }}
                />
                <FormattedMessage
                  id="equipment.usage.edit.logout.time"
                  defaultMessage="End Time"
                />
              </label>
              <TimePicker
                id="edit-logout-time"
                labelText=""
                value={extractTime(entry.logoutTime) || ""}
                onChange={(e) => {
                  handleTimeChange("logoutTime", e.target.value);
                }}
              />
            </div>
          </Column>

          {/* Activities Section */}
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="edit-activities"
              labelText={
                <FormattedMessage
                  id="equipment.usage.edit.activities"
                  defaultMessage="Activities Performed"
                />
              }
              placeholder="Describe the activities performed with the equipment..."
              value={formData.activitiesDone}
              onChange={(e) =>
                handleInputChange("activitiesDone", e.target.value)
              }
              rows={3}
              cols={50}
              maxLength={2000}
            />
            <div
              style={{
                fontSize: "0.75rem",
                color: "#6f6f6f",
                marginTop: "0.25rem",
                textAlign: "right",
              }}
            >
              {formData.activitiesDone?.length || 0}/2000 characters
            </div>
          </Column>

          {/* Equipment Status */}
          <Column lg={8} md={4} sm={4}>
            <Select
              id="edit-equipment-status"
              labelText={
                <FormattedMessage
                  id="equipment.usage.edit.status"
                  defaultMessage="Equipment Status After Use"
                />
              }
              value={formData.equipmentStatus}
              onChange={(e) =>
                handleInputChange("equipmentStatus", e.target.value)
              }
              required
            >
              <SelectItem value="FUNCTIONAL" text="Functional" />
              <SelectItem value="UNDER_MAINTENANCE" text="Under Maintenance" />
              <SelectItem value="FAULTY" text="Faulty" />
              <SelectItem value="CALIBRATION_REQUIRED" text="Calibration Required" />
            </Select>
          </Column>
        </Grid>
      </Form>
    </Modal>
  );
};

export default EditUsageEntryModal;
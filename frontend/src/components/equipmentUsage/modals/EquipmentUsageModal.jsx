import React, { useState, useEffect, useContext } from "react";
import {
  Modal,
  Form,
  Select,
  SelectItem,
  TextInput,
  TextArea,
  DatePickerInput,
  DatePicker,
  TimePickerSelect,
  Stack,
  InlineNotification,
  Loading,
  Grid,
  Column,
} from "@carbon/react";
import { FormattedMessage } from "react-intl";
import { Settings, User, Time, Edit } from "@carbon/icons-react";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import EquipmentUsageService from "../EquipmentUsageService";
import "../EquipmentUsage.css";

const EquipmentUsageModal = ({ isOpen, onClose, entry, isNew, onSubmit }) => {
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const [formData, setFormData] = useState({
    equipment: null,
    operatorName: "",
    loginTime: new Date().toISOString().split("T")[0],
    logoutTime: "",
    activitiesDone: "",
    equipmentStatus: "FUNCTIONAL",
    department: "",
  });

  const [equipment, setEquipment] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
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

  useEffect(() => {
    if (entry && !isNew) {
      setFormData({
        equipment: entry.equipment,
        operatorName: entry.operatorName,
        loginTime: entry.loginTime ? entry.loginTime.split("T")[0] : "",
        logoutTime: entry.logoutTime ? entry.logoutTime.split("T")[0] : "",
        activitiesDone: entry.activitiesDone || "",
        equipmentStatus: entry.equipmentStatus || "FUNCTIONAL",
        department: entry.department || "",
      });
    }
  }, [entry, isNew, isOpen]);

  const loadEquipment = async () => {
    if (!isMounted) return;

    setLoading(true);
    try {
      const data = await EquipmentUsageService.getEquipmentForDropdown();
      if (isMounted) {
        setEquipment(data || []);
      }
    } catch (err) {
      if (isMounted) {
        setError(err.message);
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
    setError(null);
  };

  const handleEquipmentChange = (equipmentId) => {
    const selected = equipment.find((eq) => eq.id === parseInt(equipmentId));
    setFormData((prev) => ({
      ...prev,
      equipment: selected,
      department: selected?.department || "",
    }));
  };

  const handleSubmit = async () => {
    try {
      if (!formData.equipment) {
        setError("Please select an equipment");
        return;
      }
      if (!formData.operatorName) {
        setError("Operator name is required");
        return;
      }
      if (!formData.loginTime) {
        setError("Login time is required");
        return;
      }

      const submitData = {
        ...formData,
        equipment: {
          id: formData.equipment.id,
        },
        entryStatus: "DRAFT",
      };

      onSubmit(submitData);
    } catch (err) {
      setError(err.message);
    }
  };

  if (loading) {
    return <Loading />;
  }

  return (
    <Modal
      open={isOpen}
      modalHeading={
        <div style={{ display: "flex", alignItems: "center", gap: "0.75rem" }}>
          <Settings size={24} />
          <span>
            <FormattedMessage
              id={
                isNew
                  ? "equipment.usage.modal.title.new"
                  : "equipment.usage.modal.title.edit"
              }
              defaultMessage={
                isNew
                  ? "New Equipment Usage Entry"
                  : "Edit Equipment Usage Entry"
              }
            />
          </span>
        </div>
      }
      primaryButtonText={
        <FormattedMessage id="common.button.save" defaultMessage="Save Draft" />
      }
      secondaryButtonText={
        <FormattedMessage id="common.button.cancel" defaultMessage="Cancel" />
      }
      onRequestSubmit={handleSubmit}
      onRequestClose={onClose}
      size="lg"
      className="equipment-usage-modal"
    >
      {/* Error notification */}
      {error && (
        <InlineNotification
          title={<FormattedMessage id="common.error" defaultMessage="Error" />}
          subtitle={error}
          kind="error"
          onClose={() => setError(null)}
          style={{ marginBottom: "1.5rem" }}
        />
      )}

      {/* Header information */}
      <div
        style={{
          marginBottom: "1.5rem",
          padding: "1rem",
          backgroundColor: "#f0f7ff",
          borderRadius: "6px",
        }}
      >
        <p style={{ margin: 0, color: "#393939", fontSize: "0.875rem" }}>
          <FormattedMessage
            id="equipment.usage.modal.instruction"
            defaultMessage="Complete the form below to log equipment usage. All required fields are marked with an asterisk (*)"
          />
        </p>
      </div>

      <Form>
        {/* Equipment Section */}
        <div
          className="equipment-section"
          style={{
            marginBottom: "2rem",
            padding: "1.5rem",
            backgroundColor: "#f4f4f4",
            borderRadius: "8px",
          }}
        >
          <h3
            style={{
              marginBottom: "1rem",
              display: "flex",
              alignItems: "center",
              gap: "0.5rem",
              fontSize: "1.125rem",
              fontWeight: "600",
              color: "#393939",
            }}
          >
            <Settings size={20} />
            <FormattedMessage
              id="equipment.usage.section.equipment"
              defaultMessage="Equipment Information"
            />
          </h3>

          <Grid fullWidth={true}>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="equipment"
                labelText={
                  <FormattedMessage
                    id="equipment.label"
                    defaultMessage="Equipment Name"
                  />
                }
                placeholder="Select equipment..."
                value={formData.equipment?.id || ""}
                onChange={(e) => handleEquipmentChange(e.target.value)}
                required
                helperText={
                  <FormattedMessage
                    id="equipment.helper.text"
                    defaultMessage="Choose the equipment you will be using"
                  />
                }
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
            </Column>

            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="serial-number"
                labelText={
                  <FormattedMessage
                    id="equipment.serial.number"
                    defaultMessage="Serial Number"
                  />
                }
                value={formData.equipment?.serialNumber || "—"}
                placeholder="Auto-filled when equipment is selected"
                readOnly
                disabled
                helperText={
                  <FormattedMessage
                    id="equipment.serial.helper"
                    defaultMessage="Automatically populated from equipment database"
                  />
                }
              />
            </Column>
          </Grid>
        </div>

        {/* Usage Details Section */}
        <div
          className="usage-details-section"
          style={{
            marginBottom: "2rem",
            padding: "1.5rem",
            backgroundColor: "#f9f9f9",
            borderRadius: "8px",
          }}
        >
          <h3
            style={{
              marginBottom: "1rem",
              display: "flex",
              alignItems: "center",
              gap: "0.5rem",
              fontSize: "1.125rem",
              fontWeight: "600",
              color: "#393939",
            }}
          >
            <User size={20} />
            <FormattedMessage
              id="equipment.usage.section.details"
              defaultMessage="Usage Details"
            />
          </h3>

          <Grid fullWidth={true}>
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
                  id="login-date"
                  placeholder="mm/dd/yyyy"
                  labelText={
                    <FormattedMessage
                      id="equipment.usage.date"
                      defaultMessage="Usage Date"
                    />
                  }
                  helperText={
                    <FormattedMessage
                      id="equipment.usage.date.helper"
                      defaultMessage="Select the date when equipment was used"
                    />
                  }
                  required
                />
              </DatePicker>
            </Column>

            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="operator-name"
                labelText={
                  <FormattedMessage
                    id="equipment.usage.operator"
                    defaultMessage="Operator Name"
                  />
                }
                placeholder="Enter operator's full name"
                value={formData.operatorName}
                onChange={(e) =>
                  handleInputChange("operatorName", e.target.value)
                }
                helperText={
                  <FormattedMessage
                    id="equipment.usage.operator.helper"
                    defaultMessage="Name of person operating the equipment"
                  />
                }
                required
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="department"
                labelText={
                  <FormattedMessage
                    id="equipment.usage.department"
                    defaultMessage="Department"
                  />
                }
                placeholder="Auto-filled from equipment"
                value={formData.department}
                helperText={
                  <FormattedMessage
                    id="equipment.usage.department.helper"
                    defaultMessage="Department associated with the selected equipment"
                  />
                }
                readOnly
                disabled
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <div>
                <label
                  htmlFor="login-time"
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
                    id="equipment.usage.login.time"
                    defaultMessage="Login Time"
                  />
                  <span style={{ color: "#da1e28", marginLeft: "0.25rem" }}>
                    *
                  </span>
                </label>
                <TimePickerSelect
                  id="login-time"
                  value={formData.loginTime?.split("T")[1] || ""}
                  onChange={(e) => {
                    const newLoginTime = `${formData.loginTime}T${e.target.value}`;
                    handleInputChange("loginTime", newLoginTime);
                  }}
                />
                <div
                  style={{
                    fontSize: "0.75rem",
                    color: "#6f6f6f",
                    marginTop: "0.25rem",
                  }}
                >
                  <FormattedMessage
                    id="equipment.usage.login.time.helper"
                    defaultMessage="Time when equipment usage started"
                  />
                </div>
              </div>
            </Column>

            <Column lg={8} md={4} sm={4}>
              <div>
                <label
                  htmlFor="logout-time"
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
                    id="equipment.usage.logout.time"
                    defaultMessage="Logout Time"
                  />
                </label>
                <TimePickerSelect
                  id="logout-time"
                  value={formData.logoutTime?.split("T")[1] || ""}
                  onChange={(e) => {
                    const newLogoutTime = `${formData.loginTime}T${e.target.value}`;
                    handleInputChange("logoutTime", newLogoutTime);
                  }}
                />
                <div
                  style={{
                    fontSize: "0.75rem",
                    color: "#6f6f6f",
                    marginTop: "0.25rem",
                  }}
                >
                  <FormattedMessage
                    id="equipment.usage.logout.time.helper"
                    defaultMessage="Time when equipment usage ended (optional)"
                  />
                </div>
              </div>
            </Column>
          </Grid>
        </div>

        {/* Activities Section */}
        <div
          className="activities-section"
          style={{
            marginBottom: "2rem",
            padding: "1.5rem",
            backgroundColor: "#f0f7ff",
            borderRadius: "8px",
          }}
        >
          <h3
            style={{
              marginBottom: "1rem",
              display: "flex",
              alignItems: "center",
              gap: "0.5rem",
              fontSize: "1.125rem",
              fontWeight: "600",
              color: "#393939",
            }}
          >
            <Edit size={20} />
            <FormattedMessage
              id="equipment.usage.section.activities"
              defaultMessage="Activities & Status"
            />
          </h3>

          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="activities"
                labelText={
                  <FormattedMessage
                    id="equipment.usage.activities"
                    defaultMessage="Activities Performed"
                  />
                }
                placeholder="Describe the activities performed with the equipment, procedures completed, samples processed, maintenance tasks, etc."
                value={formData.activitiesDone}
                onChange={(e) =>
                  handleInputChange("activitiesDone", e.target.value)
                }
                helperText={
                  <FormattedMessage
                    id="equipment.usage.activities.helper"
                    defaultMessage="Provide detailed information about what was accomplished during equipment usage"
                  />
                }
                rows={4}
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

            <Column lg={8} md={4} sm={4}>
              <Select
                id="equipment-status"
                labelText={
                  <FormattedMessage
                    id="equipment.usage.status.equipment"
                    defaultMessage="Equipment Status After Use"
                  />
                }
                placeholder="Select equipment condition..."
                value={formData.equipmentStatus}
                onChange={(e) =>
                  handleInputChange("equipmentStatus", e.target.value)
                }
                helperText={
                  <FormattedMessage
                    id="equipment.usage.status.equipment.helper"
                    defaultMessage="Current condition of the equipment after usage"
                  />
                }
                required
              >
                <SelectItem value="" text="Select equipment status..." />
                <SelectItem value="FUNCTIONAL" text="Functional" />
                <SelectItem
                  value="UNDER_MAINTENANCE"
                  text="Under Maintenance"
                />
                <SelectItem value="FAULTY" text="Faulty" />
                <SelectItem
                  value="CALIBRATION_REQUIRED"
                  text="Calibration Required"
                />
              </Select>
            </Column>
          </Grid>
        </div>
      </Form>
    </Modal>
  );
};

export default EquipmentUsageModal;

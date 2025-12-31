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
  TimePickerSelect,
  Stack,
  InlineNotification,
  Loading,
} from "@carbon/react";
import { FormattedMessage } from "react-intl";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import EquipmentUsageService from "../EquipmentUsageService";

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

  useEffect(() => {
    if (userSessionDetails?.firstName && userSessionDetails?.lastName) {
      setFormData((prev) => ({
        ...prev,
        operatorName: `${userSessionDetails.firstName} ${userSessionDetails.lastName}`,
      }));
    }
    loadEquipment();
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
    setLoading(true);
    try {
      const data = await EquipmentUsageService.getEquipmentForDropdown();
      setEquipment(data || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
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
        isNew
          ? "New Equipment Usage Entry"
          : "Edit Equipment Usage Entry"
      }
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
        />
      )}

      <Form>
        <FormGroup legendText="Equipment Identification">
          <Stack gap={4}>
            <Select
              id="equipment"
              labelText="Equipment Name"
              value={formData.equipment?.id || ""}
              onChange={(e) => handleEquipmentChange(e.target.value)}
              required
            >
              <SelectItem value="" text="Select Equipment..." />
              {equipment.map((eq) => (
                <SelectItem key={eq.id} value={eq.id.toString()} text={eq.name} />
              ))}
            </Select>

            <TextInput
              id="serial-number"
              labelText="Serial Number"
              value={formData.equipment?.serialNumber || ""}
              readOnly
              disabled
            />
          </Stack>
        </FormGroup>

        <FormGroup legendText="Usage Details">
          <Stack gap={4}>
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
                labelText="Date"
                required
              />
            </DatePicker>

            <TextInput
              id="operator-name"
              labelText="Operator Name"
              value={formData.operatorName}
              onChange={(e) => handleInputChange("operatorName", e.target.value)}
              required
            />

            <TextInput
              id="department"
              labelText="Department"
              value={formData.department}
              readOnly
              disabled
            />

            <TimePickerSelect
              id="login-time"
              labelText="Login Time"
              value={formData.loginTime?.split("T")[1] || ""}
              onChange={(e) => {
                const newLoginTime = `${formData.loginTime}T${e.target.value}`;
                handleInputChange("loginTime", newLoginTime);
              }}
            />

            <TimePickerSelect
              id="logout-time"
              labelText="Logout Time"
              value={formData.logoutTime?.split("T")[1] || ""}
              onChange={(e) => {
                const newLogoutTime = `${formData.loginTime}T${e.target.value}`;
                handleInputChange("logoutTime", newLogoutTime);
              }}
            />
          </Stack>
        </FormGroup>

        <FormGroup legendText="Activities">
          <Stack gap={4}>
            <TextArea
              id="activities"
              labelText="Activities Done"
              placeholder="Describe the activities performed with this equipment..."
              rows={4}
              value={formData.activitiesDone}
              onChange={(e) => handleInputChange("activitiesDone", e.target.value)}
            />
          </Stack>
        </FormGroup>

        <FormGroup legendText="Equipment Status">
          <Stack gap={4}>
            <Select
              id="equipment-status"
              labelText="Equipment Status"
              value={formData.equipmentStatus}
              onChange={(e) => handleInputChange("equipmentStatus", e.target.value)}
            >
              <SelectItem value="FUNCTIONAL" text="Functional" />
              <SelectItem value="UNDER_MAINTENANCE" text="Under Maintenance" />
              <SelectItem value="FAULTY" text="Faulty" />
              <SelectItem value="CALIBRATION_REQUIRED" text="Calibration Required" />
            </Select>
          </Stack>
        </FormGroup>
      </Form>
    </Modal>
  );
};

export default EquipmentUsageModal;

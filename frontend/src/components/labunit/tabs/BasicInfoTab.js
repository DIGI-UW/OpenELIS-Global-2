import React, { useState, useEffect } from "react";
import {
  Grid,
  Column,
  TextInput,
  TextArea,
  NumberInput,
  Toggle,
  Button,
  Modal,
  Tag,
  Select,
  SelectItem,
} from "@carbon/react";
import { AlertTriangle, X } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";

export default function BasicInfoTab({ unit }) {
  const intl = useIntl();
  const [formData, setFormData] = useState({
    name: "",
    code: "",
    description: "",
    displayOrder: 1,
    externalId: "",
    isActive: true,
  });
  const [showDeactivateModal, setShowDeactivateModal] = useState(false);
  const [errors, setErrors] = useState({});

  // Initialize form data when unit is provided
  useEffect(() => {
    if (unit) {
      setFormData({
        name: unit.name || "",
        code: unit.code || "",
        description: unit.description || "",
        displayOrder: unit.displayOrder || 1,
        externalId: unit.externalId || "",
        isActive: unit.isActive !== undefined ? unit.isActive : true,
      });
    }
  }, [unit]);

  const validateForm = () => {
    const newErrors = {};

    if (!formData.name.trim()) {
      newErrors.name = intl.formatMessage({ id: "validation.name.required" });
    }

    if (!formData.code.trim()) {
      newErrors.code = intl.formatMessage({ id: "validation.code.required" });
    }

    if (formData.code.length > 20) {
      newErrors.code = intl.formatMessage({ id: "validation.code.too.long" });
    }

    if (formData.displayOrder < 0) {
      newErrors.displayOrder = intl.formatMessage({
        id: "validation.display.order.positive",
      });
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleInputChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));

    // Clear error for this field when user starts typing
    if (errors[field]) {
      setErrors((prev) => ({
        ...prev,
        [field]: "",
      }));
    }
  };

  const handleStatusToggle = () => {
    if (formData.isActive) {
      // Show deactivation modal if currently active
      setShowDeactivateModal(true);
    } else {
      // Simply activate if currently inactive
      handleInputChange("isActive", true);
    }
  };

  const handleSave = () => {
    if (!validateForm()) {
      return;
    }

    // TODO: Implement save logic
    console.log("Saving lab unit:", formData);
  };

  const handleDeactivateConfirm = () => {
    setFormData((prev) => ({
      ...prev,
      isActive: false,
    }));
    setShowDeactivateModal(false);
  };

  const getStatusBadge = () => {
    return formData.isActive ? (
      <Tag type="green">
        {intl.formatMessage({ id: "labunit.status.active" })}
      </Tag>
    ) : (
      <Tag type="gray">
        {intl.formatMessage({ id: "labunit.status.inactive" })}
      </Tag>
    );
  };

  return (
    <div>
      <Grid fullWidth>
        <Column lg={8} md={4}>
          {/* Basic Information */}
          <div style={{ marginBottom: "2rem" }}>
            <h3
              style={{
                marginBottom: "1.5rem",
                fontSize: "1.25rem",
                fontWeight: "600",
              }}
            >
              {intl.formatMessage({ id: "labunit.basic.info" })}
            </h3>

            <div
              style={{
                display: "grid",
                gap: "1rem",
                gridTemplateColumns: "1fr",
              }}
            >
              <TextInput
                id="lab-unit-name"
                labelText={intl.formatMessage({ id: "labunit.name" })}
                placeholder={intl.formatMessage({
                  id: "labunit.name.placeholder",
                })}
                value={formData.name}
                onChange={(e) => handleInputChange("name", e.target.value)}
                invalid={!!errors.name}
                invalidText={errors.name}
                required
              />

              <TextInput
                id="lab-unit-code"
                labelText={intl.formatMessage({ id: "labunit.code" })}
                placeholder={intl.formatMessage({
                  id: "labunit.code.placeholder",
                })}
                value={formData.code}
                onChange={(e) =>
                  handleInputChange("code", e.target.value.toUpperCase())
                }
                invalid={!!errors.code}
                invalidText={errors.code}
                maxLength={20}
                required
              />

              <TextArea
                id="lab-unit-description"
                labelText={intl.formatMessage({ id: "labunit.description" })}
                placeholder={intl.formatMessage({
                  id: "labunit.description.placeholder",
                })}
                value={formData.description}
                onChange={(e) =>
                  handleInputChange("description", e.target.value)
                }
                rows={3}
              />

              <NumberInput
                id="lab-unit-display-order"
                labelText={intl.formatMessage({ id: "labunit.display.order" })}
                value={formData.displayOrder}
                onChange={(e) =>
                  handleInputChange("displayOrder", e.target.value)
                }
                invalid={!!errors.displayOrder}
                invalidText={errors.displayOrder}
                min={0}
                hideSteppers
              />

              <TextInput
                id="lab-unit-external-id"
                labelText={intl.formatMessage({ id: "labunit.external.id" })}
                placeholder={intl.formatMessage({
                  id: "labunit.external.id.placeholder",
                })}
                value={formData.externalId}
                onChange={(e) =>
                  handleInputChange("externalId", e.target.value)
                }
              />
            </div>
          </div>
        </Column>

        <Column lg={8} md={4}>
          {/* Status Section */}
          <div style={{ marginBottom: "2rem" }}>
            <h3
              style={{
                marginBottom: "1.5rem",
                fontSize: "1.25rem",
                fontWeight: "600",
              }}
            >
              {intl.formatMessage({ id: "labunit.status.title" })}
            </h3>

            <div
              style={{ display: "flex", gap: "2rem", alignItems: "flex-start" }}
            >
              <div style={{ flex: "1" }}>
                <Toggle
                  id="lab-unit-status"
                  labelText=""
                  labelA={intl.formatMessage({ id: "labunit.status.active" })}
                  labelB={intl.formatMessage({ id: "labunit.status.inactive" })}
                  toggled={formData.isActive}
                  onToggle={handleStatusToggle}
                  size="sm"
                />

                <div style={{ marginTop: "1rem" }}>
                  <h4
                    style={{
                      marginBottom: "0.5rem",
                      fontSize: "1rem",
                      fontWeight: "500",
                    }}
                  >
                    {formData.isActive
                      ? intl.formatMessage({ id: "labunit.status.active" })
                      : intl.formatMessage({ id: "labunit.status.inactive" })}
                  </h4>
                  <p style={{ margin: 0, color: "#525252", lineHeight: "1.5" }}>
                    {formData.isActive
                      ? intl.formatMessage({
                          id: "labunit.status.active.description",
                        })
                      : intl.formatMessage({
                          id: "labunit.status.inactive.description",
                        })}
                  </p>

                  {unit && (
                    <div
                      style={{
                        marginTop: "1rem",
                        padding: "1rem",
                        backgroundColor: "#f4f4f4",
                        borderRadius: "4px",
                      }}
                    >
                      <p
                        style={{
                          margin: 0,
                          fontSize: "0.875rem",
                          color: "#525252",
                        }}
                      >
                        <strong>
                          {intl.formatMessage({
                            id: "labunit.current.assignments",
                          })}
                          :
                        </strong>{" "}
                        {unit.tests}{" "}
                        {intl.formatMessage({ id: "labunit.tests" })},{" "}
                        {unit.panels}{" "}
                        {intl.formatMessage({ id: "labunit.panels" })},{" "}
                        {unit.programs}{" "}
                        {intl.formatMessage({ id: "labunit.programs" })},{" "}
                        {unit.projects}{" "}
                        {intl.formatMessage({ id: "labunit.projects" })}
                      </p>
                    </div>
                  )}
                </div>
              </div>

              <div style={{ flex: "1", textAlign: "center" }}>
                <p style={{ marginBottom: "0.5rem", fontWeight: "500" }}>
                  {intl.formatMessage({ id: "labunit.current.status" })}:
                </p>
                {getStatusBadge()}
              </div>
            </div>
          </div>
        </Column>
      </Grid>

      {/* Deactivation Modal */}
      <DeactivateModal
        unit={unit}
        formData={formData}
        isOpen={showDeactivateModal}
        onClose={() => setShowDeactivateModal(false)}
        onConfirm={handleDeactivateConfirm}
      />
    </div>
  );
}

function DeactivateModal({ unit, formData, isOpen, onClose, onConfirm }) {
  const intl = useIntl();
  const [step, setStep] = useState(1);
  const [option, setOption] = useState("keep");
  const [testDestination, setTestDestination] = useState("");
  const [panelDestination, setPanelDestination] = useState("");
  const [programDestination, setProgramDestination] = useState("");
  const [projectDestination, setProjectDestination] = useState("");

  if (!isOpen) return null;

  const handleConfirm = () => {
    onConfirm();
  };

  const renderStep1 = () => (
    <>
      <div
        style={{
          display: "flex",
          alignItems: "flex-start",
          gap: "1rem",
          marginBottom: "1.5rem",
          padding: "1rem",
          backgroundColor: "#fef7f7",
          borderRadius: "4px",
          border: "1px solid #f9c74f",
        }}
      >
        <div style={{ color: "#f9c74f", flexShrink: 0 }}>
          <AlertTriangle size={20} />
        </div>
        <div style={{ flex: 1 }}>
          <h3 style={{ marginBottom: "0.5rem", color: "#da1e28" }}>
            {intl.formatMessage({ id: "labunit.deactivate.title" })}{" "}
            {formData.name}?
          </h3>
          <p style={{ margin: 0, color: "#525252" }}>
            {intl.formatMessage({ id: "labunit.deactivate.warning" })}
          </p>

          <div style={{ marginTop: "1rem" }}>
            <h4
              style={{
                marginBottom: "0.5rem",
                fontSize: "0.875rem",
                fontWeight: "600",
              }}
            >
              {intl.formatMessage({ id: "labunit.deactivate.impact" })}:
            </h4>
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(2, 1fr)",
                gap: "0.5rem",
              }}
            >
              <div
                style={{
                  padding: "0.5rem",
                  backgroundColor: "#ffffff",
                  border: "1px solid #e0e0e0",
                  borderRadius: "4px",
                }}
              >
                <span>
                  {unit?.tests || 0}{" "}
                  {intl.formatMessage({ id: "labunit.tests" })}
                </span>
              </div>
              <div
                style={{
                  padding: "0.5rem",
                  backgroundColor: "#ffffff",
                  border: "1px solid #e0e0e0",
                  borderRadius: "4px",
                }}
              >
                <span>
                  {unit?.panels || 0}{" "}
                  {intl.formatMessage({ id: "labunit.panels" })}
                </span>
              </div>
              <div
                style={{
                  padding: "0.5rem",
                  backgroundColor: "#ffffff",
                  border: "1px solid #e0e0e0",
                  borderRadius: "4px",
                }}
              >
                <span>
                  {unit?.programs || 0}{" "}
                  {intl.formatMessage({ id: "labunit.programs" })}
                </span>
              </div>
              <div
                style={{
                  padding: "0.5rem",
                  backgroundColor: "#ffffff",
                  border: "1px solid #e0e0e0",
                  borderRadius: "4px",
                }}
              >
                <span>
                  {unit?.projects || 0}{" "}
                  {intl.formatMessage({ id: "labunit.projects" })}
                </span>
              </div>
            </div>
          </div>

          <p
            style={{
              marginTop: "1rem",
              fontSize: "0.875rem",
              color: "#525252",
              fontStyle: "italic",
            }}
          >
            {intl.formatMessage({ id: "labunit.deactivate.note" })}
          </p>

          <h4
            style={{
              marginBottom: "0.5rem",
              fontSize: "0.875rem",
              fontWeight: "600",
            }}
          >
            {intl.formatMessage({ id: "labunit.deactivate.options.title" })}:
          </h4>

          <div
            style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}
          >
            <label
              style={{
                display: "flex",
                flexDirection: "column",
                gap: "0.5rem",
                padding: "1rem",
                border: "1px solid #e0e0e0",
                borderRadius: "4px",
                cursor: "pointer",
                backgroundColor: option === "keep" ? "#f0f9ff" : "#ffffff",
              }}
            >
              <div
                style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
              >
                <input
                  type="radio"
                  name="deactivate-option"
                  value="keep"
                  checked={option === "keep"}
                  onChange={(e) => setOption(e.target.value)}
                />
                <strong>
                  {intl.formatMessage({
                    id: "labunit.deactivate.keep.assignments",
                  })}
                </strong>
              </div>
              <p style={{ margin: 0, fontSize: "0.875rem", color: "#525252" }}>
                {intl.formatMessage({
                  id: "labunit.deactivate.keep.description",
                })}
              </p>
            </label>

            <label
              style={{
                display: "flex",
                flexDirection: "column",
                gap: "0.5rem",
                padding: "1rem",
                border: "1px solid #e0e0e0",
                borderRadius: "4px",
                cursor: "pointer",
                backgroundColor:
                  option === "deactivate-all" ? "#f0f9ff" : "#ffffff",
              }}
            >
              <div
                style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
              >
                <input
                  type="radio"
                  name="deactivate-option"
                  value="deactivate-all"
                  checked={option === "deactivate-all"}
                  onChange={(e) => setOption(e.target.value)}
                />
                <strong>
                  {intl.formatMessage({
                    id: "labunit.deactivate.deactivate.all",
                  })}
                </strong>
              </div>
              <p style={{ margin: 0, fontSize: "0.875rem", color: "#525252" }}>
                {intl.formatMessage({
                  id: "labunit.deactivate.deactivate.all.description",
                })}
              </p>
            </label>

            <label
              style={{
                display: "flex",
                flexDirection: "column",
                gap: "0.5rem",
                padding: "1rem",
                border: "1px solid #e0e0e0",
                borderRadius: "4px",
                cursor: "pointer",
                backgroundColor: option === "reassign" ? "#f0f9ff" : "#ffffff",
              }}
            >
              <div
                style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
              >
                <input
                  type="radio"
                  name="deactivate-option"
                  value="reassign"
                  checked={option === "reassign"}
                  onChange={(e) => setOption(e.target.value)}
                />
                <strong>
                  {intl.formatMessage({
                    id: "labunit.deactivate.reassign.first",
                  })}
                </strong>
              </div>
              <p style={{ margin: 0, fontSize: "0.875rem", color: "#525252" }}>
                {intl.formatMessage({
                  id: "labunit.deactivate.reassign.description",
                })}
              </p>
            </label>
          </div>
        </div>
      </div>
    </>
  );

  const renderStep2 = () => (
    <div style={{ marginBottom: "1.5rem" }}>
      <h3
        style={{ marginBottom: "1rem", fontSize: "1.25rem", fontWeight: "600" }}
      >
        {intl.formatMessage({ id: "labunit.reassign.title" })}
      </h3>
      <p style={{ marginBottom: "1.5rem", color: "#525252" }}>
        {intl.formatMessage({ id: "labunit.reassign.description" })}
      </p>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(2, 1fr)",
          gap: "1rem",
        }}
      >
        <div>
          <label
            style={{
              display: "block",
              marginBottom: "0.5rem",
              fontWeight: "500",
            }}
          >
            {intl.formatMessage({ id: "labunit.tests" })}:
          </label>
          <Select
            value={testDestination}
            onChange={(e) => setTestDestination(e.target.value)}
            placeholder={intl.formatMessage({
              id: "labunit.select.destination",
            })}
          >
            <SelectItem value="">
              {intl.formatMessage({ id: "labunit.select.destination" })}
            </SelectItem>
            <SelectItem value="chem">
              {intl.formatMessage({ id: "labunit.clinical.chemistry" })}
            </SelectItem>
            <SelectItem value="hema">
              {intl.formatMessage({ id: "labunit.hematology" })}
            </SelectItem>
            <SelectItem value="micro">
              {intl.formatMessage({ id: "labunit.microbiology" })}
            </SelectItem>
          </Select>
        </div>

        <div>
          <label
            style={{
              display: "block",
              marginBottom: "0.5rem",
              fontWeight: "500",
            }}
          >
            {intl.formatMessage({ id: "labunit.panels" })}:
          </label>
          <Select
            value={panelDestination}
            onChange={(e) => setPanelDestination(e.target.value)}
            placeholder={intl.formatMessage({
              id: "labunit.select.destination",
            })}
          >
            <SelectItem value="">
              {intl.formatMessage({ id: "labunit.select.destination" })}
            </SelectItem>
            <SelectItem value="chem">
              {intl.formatMessage({ id: "labunit.clinical.chemistry" })}
            </SelectItem>
            <SelectItem value="hema">
              {intl.formatMessage({ id: "labunit.hematology" })}
            </SelectItem>
            <SelectItem value="micro">
              {intl.formatMessage({ id: "labunit.microbiology" })}
            </SelectItem>
          </Select>
        </div>

        <div>
          <label
            style={{
              display: "block",
              marginBottom: "0.5rem",
              fontWeight: "500",
            }}
          >
            {intl.formatMessage({ id: "labunit.programs" })}:
          </label>
          <Select
            value={programDestination}
            onChange={(e) => setProgramDestination(e.target.value)}
            placeholder={intl.formatMessage({
              id: "labunit.select.destination",
            })}
          >
            <SelectItem value="">
              {intl.formatMessage({ id: "labunit.select.destination" })}
            </SelectItem>
            <SelectItem value="chem">
              {intl.formatMessage({ id: "labunit.clinical.chemistry" })}
            </SelectItem>
            <SelectItem value="hema">
              {intl.formatMessage({ id: "labunit.hematology" })}
            </SelectItem>
            <SelectItem value="micro">
              {intl.formatMessage({ id: "labunit.microbiology" })}
            </SelectItem>
          </Select>
        </div>

        <div>
          <label
            style={{
              display: "block",
              marginBottom: "0.5rem",
              fontWeight: "500",
            }}
          >
            {intl.formatMessage({ id: "labunit.projects" })}:
          </label>
          <Select
            value={projectDestination}
            onChange={(e) => setProjectDestination(e.target.value)}
            placeholder={intl.formatMessage({
              id: "labunit.select.destination",
            })}
          >
            <SelectItem value="">
              {intl.formatMessage({ id: "labunit.select.destination" })}
            </SelectItem>
            <SelectItem value="chem">
              {intl.formatMessage({ id: "labunit.clinical.chemistry" })}
            </SelectItem>
            <SelectItem value="hema">
              {intl.formatMessage({ id: "labunit.hematology" })}
            </SelectItem>
            <SelectItem value="micro">
              {intl.formatMessage({ id: "labunit.microbiology" })}
            </SelectItem>
          </Select>
        </div>
      </div>
    </div>
  );

  const renderStep3 = () => (
    <div style={{ marginBottom: "1.5rem" }}>
      <div
        style={{
          display: "flex",
          alignItems: "flex-start",
          gap: "1rem",
          marginBottom: "1.5rem",
          padding: "1rem",
          backgroundColor: "#fef2f2",
          borderRadius: "4px",
          border: "1px solid #da1e28",
        }}
      >
        <div style={{ color: "#da1e28", flexShrink: 0 }}>
          <AlertTriangle size={20} />
        </div>
        <div style={{ flex: 1 }}>
          <h3 style={{ marginBottom: "0.5rem", color: "#da1e28" }}>
            {intl.formatMessage({ id: "labunit.deactivate.confirm.bulk" })}
          </h3>

          <div style={{ marginTop: "1rem" }}>
            <div
              style={{
                display: "flex",
                flexDirection: "column",
                gap: "0.5rem",
              }}
            >
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "0.5rem",
                  padding: "0.5rem",
                  backgroundColor: "#ffffff",
                  border: "1px solid #e0e0e0",
                  borderRadius: "4px",
                }}
              >
                <X size={16} />
                <span>
                  {formData.name}{" "}
                  {intl.formatMessage({ id: "labunit.lab.unit" })}
                </span>
              </div>
              {unit?.tests > 0 && (
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "0.5rem",
                    padding: "0.5rem",
                    backgroundColor: "#ffffff",
                    border: "1px solid #e0e0e0",
                    borderRadius: "4px",
                  }}
                >
                  <X size={16} />
                  <span>
                    {unit.tests} {intl.formatMessage({ id: "labunit.tests" })}
                  </span>
                </div>
              )}
              {unit?.panels > 0 && (
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "0.5rem",
                    padding: "0.5rem",
                    backgroundColor: "#ffffff",
                    border: "1px solid #e0e0e0",
                    borderRadius: "4px",
                  }}
                >
                  <X size={16} />
                  <span>
                    {unit.panels} {intl.formatMessage({ id: "labunit.panels" })}
                  </span>
                </div>
              )}
              {unit?.programs > 0 && (
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "0.5rem",
                    padding: "0.5rem",
                    backgroundColor: "#ffffff",
                    border: "1px solid #e0e0e0",
                    borderRadius: "4px",
                  }}
                >
                  <X size={16} />
                  <span>
                    {unit.programs}{" "}
                    {intl.formatMessage({ id: "labunit.programs" })}
                  </span>
                </div>
              )}
              {unit?.projects > 0 && (
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "0.5rem",
                    padding: "0.5rem",
                    backgroundColor: "#ffffff",
                    border: "1px solid #e0e0e0",
                    borderRadius: "4px",
                  }}
                >
                  <X size={16} />
                  <span>
                    {unit.projects}{" "}
                    {intl.formatMessage({ id: "labunit.projects" })}
                  </span>
                </div>
              )}
            </div>
          </div>

          <p
            style={{
              marginTop: "1rem",
              fontSize: "0.875rem",
              color: "#525252",
              fontStyle: "italic",
            }}
          >
            {intl.formatMessage({ id: "labunit.deactivate.reversible.note" })}
          </p>
        </div>
      </div>
    </div>
  );

  const renderContent = () => {
    switch (step) {
      case 1:
        return renderStep1();
      case 2:
        return renderStep2();
      case 3:
        return renderStep3();
      default:
        return null;
    }
  };

  const getStepActions = () => {
    switch (step) {
      case 1:
        return (
          <>
            <Button kind="secondary" onClick={onClose}>
              {intl.formatMessage({ id: "button.cancel" })}
            </Button>
            <Button
              kind="danger"
              onClick={() => {
                if (option === "deactivate-all") {
                  setStep(3);
                } else if (option === "reassign") {
                  setStep(2);
                } else {
                  handleConfirm();
                }
              }}
              disabled={!option}
            >
              {intl.formatMessage({ id: "button.continue" })}
            </Button>
          </>
        );
      case 2:
        return (
          <>
            <Button kind="secondary" onClick={() => setStep(1)}>
              {intl.formatMessage({ id: "button.back" })}
            </Button>
            <Button kind="danger" onClick={handleConfirm}>
              {intl.formatMessage({ id: "button.reassign.deactivate" })}
            </Button>
          </>
        );
      case 3:
        return (
          <>
            <Button kind="secondary" onClick={() => setStep(1)}>
              {intl.formatMessage({ id: "button.back" })}
            </Button>
            <Button kind="danger" onClick={handleConfirm}>
              {intl.formatMessage({ id: "button.deactivate.all" })}
            </Button>
          </>
        );
      default:
        return null;
    }
  };

  return (
    <Modal
      open={isOpen}
      onRequestClose={onClose}
      modalHeading={intl.formatMessage({
        id: "labunit.deactivate.modal.title",
      })}
      passiveModal
      size="lg"
    >
      <div>{renderContent()}</div>

      <div
        style={{
          marginTop: "2rem",
          display: "flex",
          gap: "0.5rem",
          justifyContent: "flex-end",
        }}
      >
        {getStepActions()}
      </div>
    </Modal>
  );
}

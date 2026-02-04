import React, { useState, useEffect } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  TextInput,
  Dropdown,
  InlineNotification,
  FormGroup,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  createAnalyzer,
  updateAnalyzer,
} from "../../../services/analyzerService";
import TestConnectionModal from "../TestConnectionModal/TestConnectionModal";
import "./AnalyzerForm.css";

const AnalyzerForm = ({ analyzer, open, onClose }) => {
  const intl = useIntl();
  const isEditMode = !!analyzer;

  const [formData, setFormData] = useState({
    name: "",
    analyzerType: "",
    ipAddress: "",
    port: "",
    protocolVersion: "ASTM_E1394",
    testUnitIds: [],
    status: "SETUP",
  });

  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [notification, setNotification] = useState(null);
  const [testConnectionModalOpen, setTestConnectionModalOpen] = useState(false);

  // Analyzer type options
  const analyzerTypeOptions = [
    { id: "HEMATOLOGY", text: "Hematology" },
    { id: "CHEMISTRY", text: "Chemistry" },
    { id: "IMMUNOLOGY", text: "Immunology" },
    { id: "MICROBIOLOGY", text: "Microbiology" },
    {
      id: "OTHER",
      text: intl.formatMessage({ id: "analyzer.form.type.other" }),
    },
  ];

  // Unified status options (manual transitions only - ACTIVE, ERROR_PENDING, OFFLINE are automatic)
  const statusOptions = [
    {
      id: "INACTIVE",
      text: intl.formatMessage({ id: "analyzer.status.inactive" }),
    },
    { id: "SETUP", text: intl.formatMessage({ id: "analyzer.status.setup" }) },
    {
      id: "VALIDATION",
      text: intl.formatMessage({ id: "analyzer.status.validation" }),
    },
  ];

  // Initialize form data when analyzer changes
  useEffect(() => {
    if (analyzer) {
      setFormData({
        name: analyzer.name || "",
        analyzerType: analyzer.analyzerType || analyzer.type || "",
        ipAddress: analyzer.ipAddress || "",
        port: analyzer.port ? String(analyzer.port) : "",
        protocolVersion: analyzer.protocolVersion || "ASTM_E1394",
        testUnitIds: analyzer.testUnitIds || [],
        status: analyzer.status || "SETUP",
      });
    } else {
      setFormData({
        name: "",
        analyzerType: "",
        ipAddress: "",
        port: "",
        protocolVersion: "ASTM_E1394",
        testUnitIds: [],
        status: "SETUP",
      });
    }
    setErrors({});
    setNotification(null);
  }, [analyzer, open]);

  // Validate IP address format
  const validateIPAddress = (ip) => {
    const ipRegex = /^(\d{1,3}\.){3}\d{1,3}$/;
    if (!ipRegex.test(ip)) {
      return intl.formatMessage({
        id: "analyzer.form.validation.ipAddress.invalid",
      });
    }
    const parts = ip.split(".");
    for (const part of parts) {
      const num = parseInt(part, 10);
      if (num < 0 || num > 255) {
        return intl.formatMessage({
          id: "analyzer.form.validation.ipAddress.invalid",
        });
      }
    }
    return null;
  };

  // Handle field changes
  const handleFieldChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    // Clear error for this field
    if (errors[field]) {
      setErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }
  };

  // Validate form
  const validateForm = () => {
    const newErrors = {};

    if (!formData.name.trim()) {
      newErrors.name = intl.formatMessage({
        id: "analyzer.form.validation.name.required",
      });
    }

    if (!formData.analyzerType) {
      newErrors.analyzerType = intl.formatMessage({
        id: "analyzer.form.validation.type.required",
      });
    }

    if (formData.ipAddress) {
      const ipError = validateIPAddress(formData.ipAddress);
      if (ipError) {
        newErrors.ipAddress = ipError;
      }
    }

    if (formData.port) {
      const portNum = parseInt(formData.port, 10);
      if (isNaN(portNum) || portNum < 1 || portNum > 65535) {
        newErrors.port = intl.formatMessage({
          id: "analyzer.form.validation.port.invalid",
        });
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Handle form submission
  const handleSubmit = () => {
    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);
    setNotification(null);

    const submitData = {
      ...formData,
      port: formData.port ? parseInt(formData.port, 10) : null,
    };

    const callback = (response, extraParams) => {
      setIsSubmitting(false);
      if (response.error || response.statusCode >= 400) {
        setNotification({
          kind: "error",
          title: intl.formatMessage({ id: "analyzer.form.error.save" }),
          subtitle:
            response.error ||
            response.message ||
            intl.formatMessage({ id: "analyzer.form.error.unknown" }),
        });
      } else {
        setNotification({
          kind: "success",
          title: intl.formatMessage({ id: "analyzer.form.success.save" }),
        });
        // Close modal after short delay
        setTimeout(() => {
          onClose();
        }, 1000);
      }
    };

    if (isEditMode) {
      updateAnalyzer(analyzer.id, submitData, callback);
    } else {
      createAnalyzer(submitData, callback);
    }
  };

  return (
    <>
      <ComposedModal
        open={open}
        onClose={onClose}
        data-testid="analyzer-form"
        className="analyzer-form-modal"
      >
        <ModalHeader
          title={intl.formatMessage({
            id: isEditMode
              ? "analyzer.form.editTitle"
              : "analyzer.form.addTitle",
          })}
          data-testid="analyzer-form-header"
        />
        <ModalBody>
          {notification && (
            <InlineNotification
              kind={notification.kind}
              title={notification.title}
              subtitle={notification.subtitle}
              onClose={() => setNotification(null)}
              data-testid="analyzer-form-notification"
            />
          )}

          <FormGroup legendText="">
            <TextInput
              id="analyzer-name"
              data-testid="analyzer-form-name-input"
              labelText={intl.formatMessage({ id: "analyzer.form.name" })}
              placeholder={intl.formatMessage({
                id: "analyzer.form.name.placeholder",
              })}
              value={formData.name}
              onChange={(e) => handleFieldChange("name", e.target.value)}
              invalid={!!errors.name}
              invalidText={errors.name}
              required
            />

            <Dropdown
              id="analyzer-type"
              data-testid="analyzer-form-type-dropdown"
              titleText={intl.formatMessage({ id: "analyzer.form.type" })}
              label={intl.formatMessage({
                id: "analyzer.form.type.placeholder",
              })}
              items={analyzerTypeOptions}
              selectedItem={
                analyzerTypeOptions.find(
                  (opt) => opt.id === formData.analyzerType,
                ) || null
              }
              itemToString={(item) => (item ? item.text : "")}
              onChange={({ selectedItem }) =>
                handleFieldChange("analyzerType", selectedItem?.id || "")
              }
              invalid={!!errors.analyzerType}
              invalidText={errors.analyzerType}
              required
            />

            <div
              className="connection-fields"
              data-testid="analyzer-form-connection-fields"
            >
              <TextInput
                id="analyzer-ip"
                data-testid="analyzer-form-ip-input"
                labelText={intl.formatMessage({
                  id: "analyzer.form.ipAddress",
                })}
                placeholder={intl.formatMessage({
                  id: "analyzer.form.ipAddress.placeholder",
                })}
                value={formData.ipAddress}
                onChange={(e) => handleFieldChange("ipAddress", e.target.value)}
                invalid={!!errors.ipAddress}
                invalidText={errors.ipAddress}
              />

              <TextInput
                id="analyzer-port"
                data-testid="analyzer-form-port-input"
                labelText={intl.formatMessage({ id: "analyzer.form.port" })}
                placeholder={intl.formatMessage({
                  id: "analyzer.form.port.placeholder",
                })}
                value={formData.port}
                onChange={(e) => handleFieldChange("port", e.target.value)}
                invalid={!!errors.port}
                invalidText={errors.port}
              />

              <Button
                kind="tertiary"
                onClick={() => setTestConnectionModalOpen(true)}
                data-testid="analyzer-form-test-connection-button"
              >
                {intl.formatMessage({ id: "analyzer.form.testConnection" })}
              </Button>
            </div>

            <Dropdown
              id="analyzer-status"
              data-testid="analyzer-form-status-dropdown"
              titleText={intl.formatMessage({
                id: "analyzer.form.status",
              })}
              label={intl.formatMessage({
                id: "analyzer.form.status",
              })}
              items={statusOptions}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={
                statusOptions.find((opt) => opt.id === formData.status) ||
                statusOptions[1] // Default to SETUP
              }
              onChange={({ selectedItem }) => {
                if (selectedItem) {
                  handleFieldChange("status", selectedItem.id);
                }
              }}
              helperText={intl.formatMessage({
                id: "analyzer.form.status.helperText",
              })}
            />
          </FormGroup>
        </ModalBody>
        <ModalFooter>
          <Button
            kind="secondary"
            onClick={onClose}
            data-testid="analyzer-form-cancel-button"
          >
            {intl.formatMessage({ id: "analyzer.form.cancel" })}
          </Button>
          <Button
            kind="primary"
            onClick={handleSubmit}
            disabled={isSubmitting}
            data-testid="analyzer-form-save-button"
          >
            {intl.formatMessage({ id: "analyzer.form.save" })}
          </Button>
        </ModalFooter>
      </ComposedModal>
      <TestConnectionModal
        analyzer={
          formData.ipAddress && formData.port
            ? {
                id: analyzer?.id || "test",
                ipAddress: formData.ipAddress,
                port: parseInt(formData.port, 10),
              }
            : null
        }
        open={testConnectionModalOpen}
        onClose={() => setTestConnectionModalOpen(false)}
      />
    </>
  );
};

export default AnalyzerForm;

import React, { useState, useEffect, useRef } from "react";
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
  getDefaultConfigs,
  getDefaultConfig,
  getAnalyzerTypes,
} from "../../../services/analyzerService";
import TestConnectionModal from "../TestConnectionModal/TestConnectionModal";
import {
  PROTOCOL_VERSIONS,
  PLUGIN_PROTOCOL_DEFAULTS,
  DEFAULT_PROTOCOL_VERSION,
} from "../constants";
import "./AnalyzerForm.css";

const AnalyzerForm = ({ analyzer, open, onClose }) => {
  const intl = useIntl();
  const isEditMode = !!analyzer;

  const [formData, setFormData] = useState({
    name: "",
    analyzerType: "",
    pluginTypeId: "",
    ipAddress: "",
    port: "",
    protocolVersion: DEFAULT_PROTOCOL_VERSION,
    testUnitIds: [],
    status: "SETUP",
    identifierPattern: "",
  });

  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [notification, setNotification] = useState(null);
  const [testConnectionModalOpen, setTestConnectionModalOpen] = useState(false);
  const closeTimeoutRef = useRef(null);

  const [defaultConfigs, setDefaultConfigs] = useState([]);
  const [loadingDefaults, setLoadingDefaults] = useState(false);
  const [selectedDefault, setSelectedDefault] = useState(null);

  const [pluginTypes, setPluginTypes] = useState([]);
  const [loadingPluginTypes, setLoadingPluginTypes] = useState(false);

  const FALLBACK_PLUGIN_TYPES = [
    {
      id: "generic-astm",
      name: "Generic ASTM",
      protocol: "ASTM",
      isGenericPlugin: true,
    },
    {
      id: "generic-hl7",
      name: "Generic HL7",
      protocol: "HL7",
      isGenericPlugin: true,
    },
  ];

  // Analyzer type options (must match DB analyzer_type column values)
  const analyzerTypeOptions = [
    { id: "HEMATOLOGY", text: "Hematology" },
    { id: "CHEMISTRY", text: "Chemistry" },
    { id: "IMMUNOLOGY", text: "Immunology" },
    { id: "MICROBIOLOGY", text: "Microbiology" },
    { id: "MOLECULAR", text: "Molecular" },
    { id: "COAGULATION", text: "Coagulation" },
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

  useEffect(() => {
    if (analyzer) {
      setFormData({
        name: analyzer.name || "",
        analyzerType: analyzer.analyzerType || analyzer.type || "",
        pluginTypeId: analyzer.pluginTypeId || analyzer.analyzerTypeId || "",
        ipAddress: analyzer.ipAddress || "",
        port: analyzer.port ? String(analyzer.port) : "",
        protocolVersion: analyzer.protocolVersion || DEFAULT_PROTOCOL_VERSION,
        testUnitIds: analyzer.testUnitIds || [],
        status: analyzer.status || "SETUP",
        identifierPattern: analyzer.identifierPattern || "",
      });
    } else {
      setFormData({
        name: "",
        analyzerType: "",
        pluginTypeId: "",
        ipAddress: "",
        port: "",
        protocolVersion: DEFAULT_PROTOCOL_VERSION,
        testUnitIds: [],
        status: "SETUP",
        identifierPattern: "",
      });
    }
    setErrors({});
    setNotification(null);
    setSelectedDefault(null);
  }, [analyzer, open]);

  useEffect(() => {
    return () => {
      if (closeTimeoutRef.current) {
        clearTimeout(closeTimeoutRef.current);
        closeTimeoutRef.current = null;
      }
    };
  }, []);

  // Clear close timeout when modal is closed manually (prevents timer firing after user dismisses)
  const handleModalClose = () => {
    if (closeTimeoutRef.current) {
      clearTimeout(closeTimeoutRef.current);
      closeTimeoutRef.current = null;
    }
    onClose();
  };

  useEffect(() => {
    if (open) {
      setLoadingPluginTypes(true);
      getAnalyzerTypes({ active: true }, (data) => {
        setLoadingPluginTypes(false);
        if (Array.isArray(data) && data.length > 0) {
          // In create mode, prefer showing only loaded types.
          // In edit mode, show all types so the current type is always available,
          // even if its plugin JAR is not currently loaded.
          let typesToUse = data;
          if (!analyzer) {
            const loadedTypes = data.filter((t) => t.pluginLoaded !== false);
            typesToUse = loadedTypes.length > 0 ? loadedTypes : data;
          }
          setPluginTypes(typesToUse);
        } else {
          // Fallback to hardcoded list if API returns empty
          setPluginTypes(FALLBACK_PLUGIN_TYPES);
        }
      });
    }
  }, [open]);

  const selectedPluginType = pluginTypes.find(
    (t) => t.id === formData.pluginTypeId,
  );
  const isGenericPlugin = selectedPluginType?.isGenericPlugin === true;

  useEffect(() => {
    if (!isEditMode && open) {
      setLoadingDefaults(true);
      getDefaultConfigs((data) => {
        setLoadingDefaults(false);
        if (Array.isArray(data)) {
          setDefaultConfigs(data);
        } else {
          setDefaultConfigs([]);
        }
      });
    }
  }, [isEditMode, open]);

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

  const handleFieldChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }
  };

  const handleDefaultConfigSelect = (defaultItem) => {
    if (!defaultItem || !defaultItem.id) {
      return;
    }

    setSelectedDefault(defaultItem);

    // Parse protocol and name from id (e.g., "hl7/mindray-bc2000")
    const [protocol, name] = defaultItem.id.split("/");

    getDefaultConfig(protocol, name, (configData) => {
      if (configData && !configData.error) {
        setFormData((prev) => ({
          ...prev,
          name: configData.analyzer_name || prev.name,
          analyzerType: configData.category || prev.analyzerType,
          protocolVersion:
            PLUGIN_PROTOCOL_DEFAULTS[protocol.toUpperCase()] ||
            prev.protocolVersion,
          port: configData.default_port
            ? String(configData.default_port)
            : prev.port,
        }));

        setNotification({
          kind: "info",
          title: intl.formatMessage({ id: "analyzer.form.defaults.loaded" }),
          subtitle: intl.formatMessage(
            { id: "analyzer.form.defaults.loaded.subtitle" },
            { name: configData.analyzer_name },
          ),
        });
      } else {
        setNotification({
          kind: "error",
          title: intl.formatMessage({ id: "analyzer.form.defaults.error" }),
          subtitle:
            configData?.error ||
            intl.formatMessage({ id: "analyzer.form.error.unknown" }),
        });
      }
    });
  };

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
        // Close modal after short delay; timer cleared on unmount or when user closes manually
        if (closeTimeoutRef.current) {
          clearTimeout(closeTimeoutRef.current);
        }
        closeTimeoutRef.current = setTimeout(() => {
          closeTimeoutRef.current = null;
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
        onClose={handleModalClose}
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

            <Dropdown
              id="analyzer-plugin-type"
              data-testid="analyzer-form-plugin-type-dropdown"
              titleText={intl.formatMessage({
                id: "analyzer.form.pluginType",
                defaultMessage: "Plugin Type",
              })}
              label={intl.formatMessage({
                id: "analyzer.form.pluginType.placeholder",
                defaultMessage: "Select plugin type...",
              })}
              items={pluginTypes}
              selectedItem={
                pluginTypes.find((opt) => opt.id === formData.pluginTypeId) ||
                null
              }
              itemToString={(item) =>
                item ? `${item.name} (${item.protocol})` : ""
              }
              onChange={({ selectedItem }) => {
                handleFieldChange("pluginTypeId", selectedItem?.id || "");
                // Auto-set protocol version based on plugin type
                if (selectedItem?.protocol) {
                  handleFieldChange(
                    "protocolVersion",
                    PLUGIN_PROTOCOL_DEFAULTS[selectedItem.protocol] ||
                      formData.protocolVersion,
                  );
                }
              }}
              disabled={loadingPluginTypes}
              helperText={intl.formatMessage({
                id: "analyzer.form.pluginType.helperText",
                defaultMessage:
                  "The analyzer plugin that will handle incoming messages",
              })}
            />

            {!isEditMode && isGenericPlugin && (
              <Dropdown
                id="analyzer-default-config"
                data-testid="analyzer-form-default-config-dropdown"
                titleText={intl.formatMessage({
                  id: "analyzer.form.loadDefaultConfig",
                })}
                label={intl.formatMessage({
                  id: "analyzer.form.loadDefaultConfig.placeholder",
                })}
                items={defaultConfigs}
                selectedItem={selectedDefault}
                itemToString={(item) =>
                  item ? `${item.analyzerName} (${item.protocol})` : ""
                }
                onChange={({ selectedItem }) =>
                  handleDefaultConfigSelect(selectedItem)
                }
                disabled={loadingDefaults}
                helperText={intl.formatMessage({
                  id: "analyzer.form.loadDefaultConfig.helperText",
                })}
              />
            )}

            {isGenericPlugin && (
              <TextInput
                id="analyzer-identifier-pattern"
                data-testid="analyzer-form-identifier-pattern-input"
                labelText={intl.formatMessage({
                  id: "analyzer.form.identifierPattern",
                  defaultMessage: "Identifier Pattern",
                })}
                placeholder={intl.formatMessage({
                  id: "analyzer.form.identifierPattern.placeholder",
                  defaultMessage: "e.g., ^ABX\\^PENTRA.*",
                })}
                value={formData.identifierPattern}
                onChange={(e) =>
                  handleFieldChange("identifierPattern", e.target.value)
                }
                helperText={intl.formatMessage({
                  id: "analyzer.form.identifierPattern.helperText",
                  defaultMessage:
                    "Regex pattern to match incoming message identifiers for routing",
                })}
              />
            )}

            <Dropdown
              id="analyzer-protocol-version"
              data-testid="analyzer-form-protocol-version-dropdown"
              titleText={intl.formatMessage({
                id: "analyzer.form.protocolVersion",
                defaultMessage: "Message Protocol",
              })}
              items={PROTOCOL_VERSIONS}
              selectedItem={
                PROTOCOL_VERSIONS.find(
                  (opt) => opt.value === formData.protocolVersion,
                ) || PROTOCOL_VERSIONS[0]
              }
              itemToString={(item) => (item ? item.label : "")}
              onChange={({ selectedItem }) => {
                if (selectedItem) {
                  handleFieldChange("protocolVersion", selectedItem.value);
                }
              }}
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

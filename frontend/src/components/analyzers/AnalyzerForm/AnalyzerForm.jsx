import React, {
  useState,
  useEffect,
  useRef,
  useMemo,
  useCallback,
} from "react";
import {
  Button,
  ButtonSet,
  TextInput,
  TextArea,
  Dropdown,
  Checkbox,
  InlineNotification,
  FormGroup,
  Loading,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { useHistory, useLocation, useParams } from "react-router-dom";
import {
  createAnalyzer,
  updateAnalyzer,
  getAnalyzer,
  getDefaultConfigs,
  getDefaultConfig,
  getAnalyzerTypes,
} from "../../../services/analyzerService";
import TestConnectionModal from "../TestConnectionModal/TestConnectionModal";
import PageTitle from "../../common/PageTitle/PageTitle";
import {
  PROTOCOL_VERSIONS,
  PLUGIN_PROTOCOL_DEFAULTS,
  DEFAULT_PROTOCOL_VERSION,
  COMMUNICATION_MODES,
  DEFAULT_COMMUNICATION_MODE,
  resolveAnalyzerApiMessage,
} from "../constants";
import "./AnalyzerForm.css";

const AnalyzerForm = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const { id: analyzerId } = useParams();
  const isEditMode = !!analyzerId;
  const [analyzer, setAnalyzer] = useState(null);
  const [loadingAnalyzer, setLoadingAnalyzer] = useState(false);
  const preselectedProfileRef = useRef(null);

  const [formData, setFormData] = useState({
    name: "",
    analyzerType: "",
    pluginTypeId: "",
    ipAddress: "",
    port: "",
    protocolVersion: DEFAULT_PROTOCOL_VERSION,
    communicationMode: DEFAULT_COMMUNICATION_MODE,
    testUnitIds: [],
    status: "SETUP",
    identifierPattern: "",
    // FILE protocol fields
    importDirectory: "",
    fileFormat: "CSV",
    filePattern: "*.csv",
    columnMappings: "{}",
    delimiter: ",",
    hasHeader: true,
    skipRows: 0,
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

  // Unified status options (manual transitions only - ACTIVE, ERROR_PENDING, OFFLINE are automatic).
  // PENDING_REGISTRATION stubs (discovered by the bridge) can only transition to SETUP or
  // INACTIVE per backend rules in AnalyzerServiceImpl.isValidTransition.
  const statusOptions =
    analyzer?.status === "PENDING_REGISTRATION"
      ? [
          {
            id: "SETUP",
            text: intl.formatMessage({ id: "analyzer.status.setup" }),
          },
          {
            id: "INACTIVE",
            text: intl.formatMessage({ id: "analyzer.status.inactive" }),
          },
        ]
      : [
          {
            id: "INACTIVE",
            text: intl.formatMessage({ id: "analyzer.status.inactive" }),
          },
          {
            id: "SETUP",
            text: intl.formatMessage({ id: "analyzer.status.setup" }),
          },
          {
            id: "VALIDATION",
            text: intl.formatMessage({ id: "analyzer.status.validation" }),
          },
        ];

  // Fetch analyzer data when editing (route-param driven)
  useEffect(() => {
    if (isEditMode) {
      setLoadingAnalyzer(true);
      getAnalyzer(analyzerId, (data) => {
        setLoadingAnalyzer(false);
        const a = data?.analyzers?.[0] || data;
        setAnalyzer(a);
        setFormData({
          name: a.name || "",
          analyzerType: a.analyzerType || a.type || "",
          pluginTypeId: a.pluginTypeId || a.analyzerTypeId || "",
          ipAddress: a.ipAddress || "",
          port: a.port ? String(a.port) : "",
          protocolVersion: a.protocolVersion || DEFAULT_PROTOCOL_VERSION,
          communicationMode: a.communicationMode || DEFAULT_COMMUNICATION_MODE,
          testUnitIds: a.testUnitIds || [],
          status: a.status || "SETUP",
          identifierPattern: a.identifierPattern || "",
          importDirectory: a.importDirectory || "",
          fileFormat: a.fileFormat || "CSV",
          filePattern: a.filePattern || "*.csv",
          columnMappings: a.columnMappings
            ? JSON.stringify(a.columnMappings, null, 2)
            : "{}",
          delimiter: a.delimiter || ",",
          hasHeader: a.hasHeader ?? true,
          skipRows: a.skipRows ?? 0,
        });
      });
    }
    setErrors({});
    setNotification(null);
    setSelectedDefault(null);
  }, [analyzerId, isEditMode]);

  useEffect(() => {
    return () => {
      if (closeTimeoutRef.current) {
        clearTimeout(closeTimeoutRef.current);
        closeTimeoutRef.current = null;
      }
    };
  }, []);

  const navigateBack = () => {
    if (closeTimeoutRef.current) {
      clearTimeout(closeTimeoutRef.current);
      closeTimeoutRef.current = null;
    }
    history.push("/analyzers");
  };

  // Load plugin types on mount
  useEffect(() => {
    setLoadingPluginTypes(true);
    getAnalyzerTypes({ active: true }, (data) => {
      setLoadingPluginTypes(false);
      if (Array.isArray(data) && data.length > 0) {
        setPluginTypes(data);
      } else {
        setPluginTypes([]);
      }
    });
  }, []);

  const selectedPluginType = pluginTypes.find(
    (t) => t.id === formData.pluginTypeId,
  );
  const isGenericPlugin = selectedPluginType?.isGenericPlugin === true;
  const isFileProtocol = selectedPluginType?.protocol?.toUpperCase() === "FILE";

  const sortedPluginTypes = useMemo(() => {
    const protocolOrder = { ASTM: 0, HL7: 1, FILE: 2 };
    return [...pluginTypes].sort((a, b) => {
      if (a.isGenericPlugin !== b.isGenericPlugin)
        return b.isGenericPlugin ? 1 : -1;
      if (a.isGenericPlugin && b.isGenericPlugin) {
        return (
          (protocolOrder[a.protocol] ?? 99) - (protocolOrder[b.protocol] ?? 99)
        );
      }
      return a.name.localeCompare(b.name);
    });
  }, [pluginTypes]);

  const communicationModeItems = useMemo(
    () =>
      COMMUNICATION_MODES.map((m) => ({
        ...m,
        label: intl.formatMessage({ id: m.labelId }),
      })),
    [intl],
  );

  const filteredDefaultConfigs = useMemo(() => {
    if (!selectedPluginType?.protocol) return defaultConfigs;
    const proto = selectedPluginType.protocol.toUpperCase();
    return defaultConfigs.filter((c) => c.protocol === proto);
  }, [defaultConfigs, selectedPluginType]);

  // Load default configs once on mount — form is always rendered as a page,
  // so no modal-open gate is needed.
  useEffect(() => {
    setLoadingDefaults(true);
    getDefaultConfigs((data) => {
      setLoadingDefaults(false);
      if (Array.isArray(data)) {
        setDefaultConfigs(data);
      } else {
        setDefaultConfigs([]);
      }
    });
  }, []);

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

  const fileFormatOptions = [
    { id: "CSV", text: "CSV" },
    { id: "TSV", text: "TSV" },
    { id: "EXCEL", text: "Excel (.xls/.xlsx)" },
  ];

  const FILE_FORMAT_PATTERNS = {
    CSV: "*.csv",
    TSV: "*.tsv",
    EXCEL: "*.{xls,xlsx}",
  };

  const handleFileFormatChange = (format) => {
    setFormData((prev) => ({
      ...prev,
      fileFormat: format,
      filePattern: FILE_FORMAT_PATTERNS[format] || prev.filePattern,
      delimiter:
        format === "TSV" ? "\t" : format === "CSV" ? "," : prev.delimiter,
    }));
  };

  const handleDefaultConfigSelect = useCallback(
    (defaultItem) => {
      if (!defaultItem || !defaultItem.id) {
        return;
      }

      setSelectedDefault(defaultItem);

      // Parse protocol and name from id (e.g., "hl7/mindray-bc2000")
      const [protocol, name] = defaultItem.id.split("/");

      getDefaultConfig(protocol, name, (configData) => {
        if (configData && !configData.error) {
          // Set plugin/protocol-level fields only — NOT instance-level (name, port, IP)
          const protocolUpper = protocol.toUpperCase();
          // Auto-resolve pluginTypeId from config protocol
          const matchingPluginType = pluginTypes.find(
            (t) =>
              t.isGenericPlugin && t.protocol?.toUpperCase() === protocolUpper,
          );

          // Base fields from profile
          const baseUpdates = {
            identifierPattern: configData.identifier_pattern || undefined,
            analyzerType:
              configData.category ||
              configData.profileMeta?.category ||
              undefined,
            protocolVersion:
              PLUGIN_PROTOCOL_DEFAULTS[protocolUpper] || undefined,
            communicationMode:
              configData.communication_mode ||
              configData.communication?.mode ||
              undefined,
            pluginTypeId: matchingPluginType?.id || undefined,
          };

          // FILE protocol: auto-fill file import fields from profile
          const fileUpdates = {};
          if (protocolUpper === "FILE") {
            const defaults = configData.configDefaults || {};
            const extensions = configData.supported_extensions || [];
            const format =
              defaults.fileFormat || configData.protocol?.format || "CSV";
            fileUpdates.fileFormat = format;
            fileUpdates.filePattern =
              extensions.length > 0
                ? `*{${extensions.join(",")}}`
                : FILE_FORMAT_PATTERNS[format] || "*.csv";
            fileUpdates.delimiter =
              defaults.delimiter ||
              (format === "CSV" ? "," : format === "TSV" ? "\t" : ",");
            fileUpdates.hasHeader = defaults.hasHeader ?? true;
            fileUpdates.skipRows = defaults.skipRows ?? 0;
            if (configData.column_mapping) {
              fileUpdates.columnMappings = JSON.stringify(
                configData.column_mapping,
                null,
                2,
              );
            }
          }

          setFormData((prev) => ({
            ...prev,
            ...Object.fromEntries(
              Object.entries(baseUpdates).filter(([, v]) => v !== undefined),
            ),
            ...fileUpdates,
          }));

          setNotification({
            kind: "info",
            title: intl.formatMessage({ id: "analyzer.form.defaults.loaded" }),
            subtitle: intl.formatMessage(
              { id: "analyzer.form.defaults.loaded.subtitle" },
              {
                name:
                  configData.analyzer_name ||
                  configData.profileMeta?.displayName,
              },
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
    },
    [intl, pluginTypes],
  );

  useEffect(() => {
    if (isEditMode || loadingDefaults || loadingPluginTypes) {
      return;
    }
    const params = new URLSearchParams(location.search || "");
    const profileId = params.get("profile");
    if (!profileId || preselectedProfileRef.current === profileId) {
      return;
    }
    const defaultItem = defaultConfigs.find((item) => item.id === profileId);
    if (!defaultItem) {
      return;
    }
    preselectedProfileRef.current = profileId;
    handleDefaultConfigSelect(defaultItem);
  }, [
    defaultConfigs,
    handleDefaultConfigSelect,
    isEditMode,
    loadingDefaults,
    loadingPluginTypes,
    location.search,
  ]);

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

    if (isFileProtocol && !formData.importDirectory.trim()) {
      newErrors.importDirectory = intl.formatMessage({
        id: "analyzer.form.validation.importDirectory.required",
        defaultMessage: "Import directory is required for file-based analyzers",
      });
    }

    if (!isFileProtocol && formData.ipAddress) {
      const ipError = validateIPAddress(formData.ipAddress);
      if (ipError) {
        newErrors.ipAddress = ipError;
      }
    }

    if (!isFileProtocol && formData.port) {
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

    // Parse column mappings JSON for FILE protocol
    let columnMappingsObj = {};
    if (isFileProtocol && formData.columnMappings) {
      try {
        columnMappingsObj = JSON.parse(formData.columnMappings);
      } catch {
        setNotification({
          kind: "error",
          title: intl.formatMessage({ id: "analyzer.form.error.save" }),
          subtitle: intl.formatMessage({
            id: "analyzer.form.validation.columnMappings.invalid",
            defaultMessage: "Column mappings must be valid JSON",
          }),
        });
        setIsSubmitting(false);
        return;
      }
    }

    const submitData = {
      ...formData,
      port: formData.port ? parseInt(formData.port, 10) : null,
      defaultConfigId: selectedDefault?.id || null,
      // Clear network/protocol fields for FILE protocol — not applicable
      ...(isFileProtocol && {
        ipAddress: null,
        port: null,
        protocolVersion: null,
        columnMappings: columnMappingsObj,
      }),
      // Clear file import fields for non-FILE protocol — not applicable
      ...(!isFileProtocol && {
        importDirectory: null,
        filePattern: null,
        fileFormat: null,
        columnMappings: null,
        delimiter: null,
        hasHeader: null,
        skipRows: null,
      }),
    };

    const callback = (response, extraParams) => {
      setIsSubmitting(false);
      if (response.error || response.statusCode >= 400) {
        setNotification({
          kind: "error",
          title: intl.formatMessage({ id: "analyzer.form.error.save" }),
          subtitle: resolveAnalyzerApiMessage(
            intl,
            response,
            "analyzer.form.error.unknown",
          ),
        });
      } else {
        setNotification({
          kind: "success",
          title: intl.formatMessage({ id: "analyzer.form.success.save" }),
        });
        // Navigate back after short delay so user sees the success notification.
        if (closeTimeoutRef.current) {
          clearTimeout(closeTimeoutRef.current);
        }
        closeTimeoutRef.current = setTimeout(() => {
          closeTimeoutRef.current = null;
          navigateBack();
        }, 1000);
      }
    };

    if (isEditMode) {
      updateAnalyzer(analyzer.id, submitData, callback);
    } else {
      createAnalyzer(submitData, callback);
    }
  };

  if (loadingAnalyzer) {
    return <Loading withOverlay={false} />;
  }

  return (
    <>
      <div
        data-testid="analyzer-form"
        className="analyzer-form-page pageContent"
      >
        <div data-testid="analyzer-form-header">
          <PageTitle
            breadcrumbs={[
              {
                label: intl.formatMessage({
                  id: "analyzer.page.hierarchy.root",
                }),
                link: "/analyzers",
              },
              {
                label: intl.formatMessage({
                  id: isEditMode
                    ? "analyzer.form.editTitle"
                    : "analyzer.form.addTitle",
                }),
              },
            ]}
          />
        </div>
        <div className="analyzer-form-content">
          {notification && (
            <InlineNotification
              kind={notification.kind}
              title={notification.title}
              subtitle={notification.subtitle}
              onClose={() => setNotification(null)}
              data-testid="analyzer-form-notification"
            />
          )}

          {/* Section 1 — Instance Identity */}
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

          {/* Section 2 — Plugin Configuration */}
          <FormGroup legendText="">
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
              items={sortedPluginTypes}
              selectedItem={
                sortedPluginTypes.find(
                  (opt) => opt.id === formData.pluginTypeId,
                ) || null
              }
              itemToString={(item) =>
                item ? `${item.name} (${item.protocol})` : ""
              }
              onChange={({ selectedItem }) => {
                handleFieldChange("pluginTypeId", selectedItem?.id || "");
                // Reset profile selection when plugin type changes
                setSelectedDefault(null);
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

            {isGenericPlugin && (
              <Dropdown
                id="analyzer-default-config"
                data-testid="analyzer-form-default-config-dropdown"
                titleText={intl.formatMessage({
                  id: "analyzer.form.loadDefaultConfig",
                })}
                label={intl.formatMessage({
                  id: "analyzer.form.loadDefaultConfig.placeholder",
                })}
                items={filteredDefaultConfigs}
                selectedItem={selectedDefault}
                itemToString={(item) =>
                  item
                    ? `${item.analyzerName || item.id?.split("/")[1] || item.id} (${item.protocol})`
                    : ""
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

            {!isFileProtocol && (
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
            )}
          </FormGroup>

          {/* Section 3 — Connection (hidden for FILE protocol) */}
          {!isFileProtocol && (
            <FormGroup legendText="">
              <Dropdown
                id="analyzer-communication-mode"
                data-testid="analyzer-form-communication-mode-dropdown"
                titleText={intl.formatMessage({
                  id: "analyzer.form.communicationMode",
                })}
                items={communicationModeItems}
                selectedItem={
                  communicationModeItems.find(
                    (opt) => opt.value === formData.communicationMode,
                  ) || null
                }
                itemToString={(item) => (item ? item.label : "")}
                onChange={({ selectedItem }) => {
                  if (selectedItem) {
                    handleFieldChange("communicationMode", selectedItem.value);
                  }
                }}
                helperText={intl.formatMessage({
                  id: "analyzer.form.communicationMode.help",
                })}
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
                  onChange={(e) =>
                    handleFieldChange("ipAddress", e.target.value)
                  }
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
            </FormGroup>
          )}

          {/* Section 3b — FILE protocol: import configuration */}
          {isFileProtocol && (
            <FormGroup
              legendText={intl.formatMessage({
                id: "analyzer.form.fileImport.title",
                defaultMessage: "File Import Settings",
              })}
            >
              <Dropdown
                id="analyzer-file-format"
                data-testid="analyzer-form-file-format-dropdown"
                titleText={intl.formatMessage({
                  id: "analyzer.form.fileFormat",
                  defaultMessage: "File Format",
                })}
                items={fileFormatOptions}
                selectedItem={
                  fileFormatOptions.find(
                    (opt) => opt.id === formData.fileFormat,
                  ) || fileFormatOptions[0]
                }
                itemToString={(item) => (item ? item.text : "")}
                onChange={({ selectedItem }) =>
                  handleFileFormatChange(selectedItem?.id || "CSV")
                }
              />

              <TextInput
                id="analyzer-import-directory"
                data-testid="analyzer-form-import-directory-input"
                labelText={intl.formatMessage({
                  id: "analyzer.form.importDirectory",
                  defaultMessage: "Import Directory",
                })}
                placeholder="/data/analyzer-imports/my-analyzer/incoming"
                value={formData.importDirectory}
                onChange={(e) =>
                  handleFieldChange("importDirectory", e.target.value)
                }
                invalid={!!errors.importDirectory}
                invalidText={errors.importDirectory}
                helperText={intl.formatMessage({
                  id: "analyzer.form.importDirectory.helperText",
                  defaultMessage:
                    "Directory the bridge watches for incoming result files",
                })}
              />

              <TextInput
                id="analyzer-file-pattern"
                data-testid="analyzer-form-file-pattern-input"
                labelText={intl.formatMessage({
                  id: "analyzer.form.filePattern",
                  defaultMessage: "File Pattern",
                })}
                placeholder="*.csv"
                value={formData.filePattern}
                onChange={(e) =>
                  handleFieldChange("filePattern", e.target.value)
                }
                helperText={intl.formatMessage({
                  id: "analyzer.form.filePattern.helperText",
                  defaultMessage: "Glob pattern to match result files",
                })}
              />

              <TextArea
                id="analyzer-column-mappings"
                data-testid="analyzer-form-column-mappings-input"
                labelText={intl.formatMessage({
                  id: "analyzer.form.columnMappings",
                  defaultMessage: "Column Mappings (JSON)",
                })}
                placeholder='{"Sample Name": "sampleId", "Target Name": "testCode", "Quantity Mean": "result"}'
                value={formData.columnMappings}
                onChange={(e) =>
                  handleFieldChange("columnMappings", e.target.value)
                }
                rows={4}
                helperText={intl.formatMessage({
                  id: "analyzer.form.columnMappings.helperText",
                  defaultMessage:
                    "Maps file column names to internal field names",
                })}
              />

              {formData.fileFormat !== "EXCEL" && (
                <>
                  <TextInput
                    id="analyzer-delimiter"
                    data-testid="analyzer-form-delimiter-input"
                    labelText={intl.formatMessage({
                      id: "analyzer.form.delimiter",
                      defaultMessage: "Delimiter",
                    })}
                    placeholder=","
                    value={formData.delimiter}
                    onChange={(e) =>
                      handleFieldChange("delimiter", e.target.value)
                    }
                  />

                  <Checkbox
                    id="analyzer-has-header"
                    data-testid="analyzer-form-has-header-checkbox"
                    labelText={intl.formatMessage({
                      id: "analyzer.form.hasHeader",
                      defaultMessage: "File has header row",
                    })}
                    checked={formData.hasHeader}
                    onChange={(_, { checked }) =>
                      handleFieldChange("hasHeader", checked)
                    }
                  />
                </>
              )}
            </FormGroup>
          )}
        </div>
        <ButtonSet className="analyzer-form-actions">
          <Button
            kind="secondary"
            onClick={navigateBack}
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
        </ButtonSet>
      </div>
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

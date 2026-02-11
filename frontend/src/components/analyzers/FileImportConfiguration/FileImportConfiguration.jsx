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
  Checkbox,
  TextArea,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  createConfiguration,
  updateConfiguration,
  getConfigurationByAnalyzerId,
} from "../../../services/fileImportService";
import { getAnalyzers } from "../../../services/analyzerService";
import "./FileImportConfiguration.css";

const FileImportConfiguration = ({ configuration, open, onClose }) => {
  const intl = useIntl();
  const isEditMode = !!configuration;

  const [formData, setFormData] = useState({
    analyzerId: null,
    importDirectory: "",
    filePattern: "*.csv",
    archiveDirectory: "",
    errorDirectory: "",
    columnMappings: "{}",
    delimiter: ",",
    hasHeader: true,
    active: true,
  });

  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [notification, setNotification] = useState(null);
  const [availableAnalyzers, setAvailableAnalyzers] = useState([]);

  // Load analyzers for dropdown
  useEffect(() => {
    if (open) {
      getAnalyzers({}, (data) => {
        const list =
          data && Array.isArray(data.analyzers) ? data.analyzers : [];
        setAvailableAnalyzers(list);
      });
    }
  }, [open]);

  // Initialize form data when configuration changes
  useEffect(() => {
    if (configuration) {
      setFormData({
        analyzerId: configuration.analyzerId,
        importDirectory: configuration.importDirectory || "",
        filePattern: configuration.filePattern || "*.csv",
        archiveDirectory: configuration.archiveDirectory || "",
        errorDirectory: configuration.errorDirectory || "",
        columnMappings: configuration.columnMappings
          ? JSON.stringify(configuration.columnMappings, null, 2)
          : "{}",
        delimiter: configuration.delimiter || ",",
        hasHeader:
          configuration.hasHeader !== undefined
            ? configuration.hasHeader
            : true,
        active:
          configuration.active !== undefined ? configuration.active : true,
      });
    } else {
      setFormData({
        analyzerId: null,
        importDirectory: "",
        filePattern: "*.csv",
        archiveDirectory: "",
        errorDirectory: "",
        columnMappings: "{}",
        delimiter: ",",
        hasHeader: true,
        active: true,
      });
    }
    setErrors({});
    setNotification(null);
  }, [configuration, open]);

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

    if (!formData.analyzerId) {
      newErrors.analyzerId = intl.formatMessage({
        id: "file.import.configuration.validation.analyzerId.required",
      });
    }

    if (!formData.importDirectory.trim()) {
      newErrors.importDirectory = intl.formatMessage({
        id: "file.import.configuration.validation.importDirectory.required",
      });
    }

    // Validate column mappings JSON
    if (formData.columnMappings) {
      try {
        JSON.parse(formData.columnMappings);
      } catch (e) {
        newErrors.columnMappings = intl.formatMessage({
          id: "file.import.configuration.validation.columnMappings.invalid",
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

    // Parse column mappings JSON
    let columnMappingsObj = {};
    try {
      columnMappingsObj = JSON.parse(formData.columnMappings);
    } catch (e) {
      setNotification({
        kind: "error",
        title: intl.formatMessage({
          id: "file.import.configuration.error.save",
        }),
        subtitle: intl.formatMessage({
          id: "file.import.configuration.validation.columnMappings.invalid",
        }),
      });
      setIsSubmitting(false);
      return;
    }

    const submitData = {
      analyzerId: formData.analyzerId,
      importDirectory: formData.importDirectory.trim(),
      filePattern: formData.filePattern || "*.csv",
      archiveDirectory: formData.archiveDirectory.trim() || null,
      errorDirectory: formData.errorDirectory.trim() || null,
      columnMappings: columnMappingsObj,
      delimiter: formData.delimiter || ",",
      hasHeader: formData.hasHeader,
      active: formData.active,
    };

    const callback = (response, extraParams) => {
      setIsSubmitting(false);
      if (response.error || response.statusCode >= 400) {
        setNotification({
          kind: "error",
          title: intl.formatMessage({
            id: "file.import.configuration.error.save",
          }),
          subtitle:
            response.error ||
            response.message ||
            intl.formatMessage({
              id: "file.import.configuration.error.unknown",
            }),
        });
      } else {
        setNotification({
          kind: "success",
          title: intl.formatMessage({
            id: isEditMode
              ? "file.import.configuration.success.updated"
              : "file.import.configuration.success.created",
          }),
        });
        // Close modal after short delay
        setTimeout(() => {
          onClose();
        }, 1000);
      }
    };

    if (isEditMode) {
      updateConfiguration(configuration.id, submitData, callback);
    } else {
      createConfiguration(submitData, callback);
    }
  };

  // Analyzer options for dropdown
  const analyzerOptions = availableAnalyzers.map((analyzer) => ({
    id: analyzer.id,
    text: analyzer.name || `Analyzer ${analyzer.id}`,
  }));

  return (
    <ComposedModal
      open={open}
      onClose={onClose}
      data-testid="file-import-configuration-form"
      className="file-import-configuration-modal"
    >
      <ModalHeader
        title={intl.formatMessage({
          id: isEditMode
            ? "file.import.configuration.title.edit"
            : "file.import.configuration.title.add",
        })}
        data-testid="file-import-configuration-form-header"
      />
      <ModalBody>
        {notification && (
          <InlineNotification
            kind={notification.kind}
            title={notification.title}
            subtitle={notification.subtitle}
            onClose={() => setNotification(null)}
            data-testid="file-import-configuration-form-notification"
          />
        )}

        <FormGroup legendText="">
          <Dropdown
            id="file-import-analyzer"
            data-testid="file-import-configuration-analyzer-dropdown"
            titleText={intl.formatMessage({
              id: "file.import.configuration.analyzer",
            })}
            label={intl.formatMessage({
              id: "file.import.configuration.analyzer.placeholder",
            })}
            items={analyzerOptions}
            selectedItem={
              analyzerOptions.find((opt) => opt.id === formData.analyzerId) ||
              null
            }
            itemToString={(item) => (item ? item.text : "")}
            onChange={({ selectedItem }) =>
              handleFieldChange("analyzerId", selectedItem?.id || null)
            }
            invalid={!!errors.analyzerId}
            invalidText={errors.analyzerId}
            required
            disabled={isEditMode} // Don't allow changing analyzer in edit mode
          />

          <TextInput
            id="file-import-directory"
            data-testid="file-import-configuration-directory-input"
            labelText={intl.formatMessage({
              id: "file.import.configuration.importDirectory",
            })}
            placeholder={intl.formatMessage({
              id: "file.import.configuration.importDirectory.placeholder",
            })}
            value={formData.importDirectory}
            onChange={(e) =>
              handleFieldChange("importDirectory", e.target.value)
            }
            invalid={!!errors.importDirectory}
            invalidText={errors.importDirectory}
            required
            helperText={intl.formatMessage({
              id: "file.import.configuration.importDirectory.helperText",
            })}
          />

          <TextInput
            id="file-import-pattern"
            data-testid="file-import-configuration-pattern-input"
            labelText={intl.formatMessage({
              id: "file.import.configuration.filePattern",
            })}
            placeholder="*.csv"
            value={formData.filePattern}
            onChange={(e) => handleFieldChange("filePattern", e.target.value)}
            helperText={intl.formatMessage({
              id: "file.import.configuration.filePattern.helperText",
            })}
          />

          <TextInput
            id="file-import-archive"
            data-testid="file-import-configuration-archive-input"
            labelText={intl.formatMessage({
              id: "file.import.configuration.archiveDirectory",
            })}
            placeholder={intl.formatMessage({
              id: "file.import.configuration.archiveDirectory.placeholder",
            })}
            value={formData.archiveDirectory}
            onChange={(e) =>
              handleFieldChange("archiveDirectory", e.target.value)
            }
            helperText={intl.formatMessage({
              id: "file.import.configuration.archiveDirectory.helperText",
            })}
          />

          <TextInput
            id="file-import-error"
            data-testid="file-import-configuration-error-input"
            labelText={intl.formatMessage({
              id: "file.import.configuration.errorDirectory",
            })}
            placeholder={intl.formatMessage({
              id: "file.import.configuration.errorDirectory.placeholder",
            })}
            value={formData.errorDirectory}
            onChange={(e) =>
              handleFieldChange("errorDirectory", e.target.value)
            }
            helperText={intl.formatMessage({
              id: "file.import.configuration.errorDirectory.helperText",
            })}
          />

          <TextArea
            id="file-import-column-mappings"
            data-testid="file-import-configuration-column-mappings-input"
            labelText={intl.formatMessage({
              id: "file.import.configuration.columnMappings",
            })}
            placeholder='{"Sample_ID": "sampleId", "Test_Code": "testCode", "Result": "result"}'
            value={formData.columnMappings}
            onChange={(e) =>
              handleFieldChange("columnMappings", e.target.value)
            }
            invalid={!!errors.columnMappings}
            invalidText={errors.columnMappings}
            rows={4}
            helperText={intl.formatMessage({
              id: "file.import.configuration.columnMappings.helperText",
            })}
          />

          <TextInput
            id="file-import-delimiter"
            data-testid="file-import-configuration-delimiter-input"
            labelText={intl.formatMessage({
              id: "file.import.configuration.delimiter",
            })}
            placeholder=","
            value={formData.delimiter}
            onChange={(e) => handleFieldChange("delimiter", e.target.value)}
            helperText={intl.formatMessage({
              id: "file.import.configuration.delimiter.helperText",
            })}
          />

          <Checkbox
            id="file-import-has-header"
            data-testid="file-import-configuration-has-header-checkbox"
            labelText={intl.formatMessage({
              id: "file.import.configuration.hasHeader",
            })}
            checked={formData.hasHeader}
            onChange={(checked) => handleFieldChange("hasHeader", checked)}
          />

          <Checkbox
            id="file-import-active"
            data-testid="file-import-configuration-active-checkbox"
            labelText={intl.formatMessage({
              id: "file.import.configuration.active",
            })}
            checked={formData.active}
            onChange={(checked) => handleFieldChange("active", checked)}
          />
        </FormGroup>
      </ModalBody>
      <ModalFooter>
        <Button
          kind="secondary"
          onClick={onClose}
          data-testid="file-import-configuration-form-cancel-button"
        >
          {intl.formatMessage({ id: "file.import.configuration.cancel" })}
        </Button>
        <Button
          kind="primary"
          onClick={handleSubmit}
          disabled={isSubmitting}
          data-testid="file-import-configuration-form-save-button"
        >
          {intl.formatMessage({ id: "file.import.configuration.save" })}
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default FileImportConfiguration;

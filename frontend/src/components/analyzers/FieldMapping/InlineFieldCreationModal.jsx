/**
 * InlineFieldCreationModal Component
 *
 * Modal for creating new OpenELIS fields inline from the field mapping interface
 *
 * Features:
 * - Form fields: Field Name, Entity Type, LOINC Code, Description, Field Type, Accepted Units
 * - Validation: Field name uniqueness, entity type required, field type compatibility
 * - Confirmation step: "Field will be available for mapping immediately after creation"
 */

import React, { useState } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  TextInput,
  Dropdown,
  Form,
  FormGroup,
  Checkbox,
  InlineNotification,
  MultiSelect,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { createField } from "../../../services/analyzerService";
import "./InlineFieldCreationModal.css";

const InlineFieldCreationModal = ({
  open,
  onClose,
  onFieldCreated,
  fieldType, // Required field type for type compatibility
}) => {
  const intl = useIntl();
  const [formData, setFormData] = useState({
    fieldName: "",
    entityType: "",
    loincCode: "",
    description: "",
    fieldType: fieldType || "NUMERIC",
    acceptedUnits: [],
  });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [notification, setNotification] = useState(null);

  // Entity type options
  const entityTypeOptions = [
    { id: "TEST", text: "Test" },
    { id: "PANEL", text: "Panel" },
    { id: "RESULT", text: "Result" },
    { id: "ORDER", text: "Order" },
    { id: "SAMPLE", text: "Sample" },
    { id: "QC", text: "QC" },
    { id: "METADATA", text: "Metadata" },
    { id: "UNIT", text: "Unit" },
  ];

  // Field type options
  const fieldTypeOptions = [
    { id: "NUMERIC", text: "Numeric" },
    { id: "QUALITATIVE", text: "Qualitative" },
    { id: "TEXT", text: "Text" },
  ];

  // Unit options (mock - should be loaded from API)
  const unitOptions = [
    { id: "mg/dL", text: "mg/dL" },
    { id: "g/dL", text: "g/dL" },
    { id: "mmol/L", text: "mmol/L" },
    { id: "IU/L", text: "IU/L" },
    { id: "U/L", text: "U/L" },
  ];

  // Reset form when modal opens/closes
  React.useEffect(() => {
    if (!open) {
      setFormData({
        fieldName: "",
        entityType: "",
        loincCode: "",
        description: "",
        fieldType: fieldType || "NUMERIC",
        acceptedUnits: [],
      });
      setErrors({});
      setNotification(null);
    }
  }, [open, fieldType]);

  // Validate form
  const validateForm = () => {
    const newErrors = {};

    if (!formData.fieldName || formData.fieldName.trim() === "") {
      newErrors.fieldName = intl.formatMessage({
        id: "analyzer.fieldCreation.validation.fieldName.required",
        defaultMessage: "Field name is required",
      });
    }

    if (!formData.entityType) {
      newErrors.entityType = intl.formatMessage({
        id: "analyzer.fieldCreation.validation.entityType.required",
        defaultMessage: "Entity type is required",
      });
    }

    // Field type compatibility: if fieldType is NUMERIC, acceptedUnits must be provided
    if (
      formData.fieldType === "NUMERIC" &&
      formData.acceptedUnits.length === 0
    ) {
      newErrors.acceptedUnits = intl.formatMessage({
        id: "analyzer.fieldCreation.validation.units.required",
        defaultMessage: "Accepted units are required for numeric fields",
      });
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Handle form field changes
  const handleFieldChange = (fieldName, value) => {
    setFormData((prev) => ({
      ...prev,
      [fieldName]: value,
    }));
    // Clear error for this field
    if (errors[fieldName]) {
      setErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[fieldName];
        return newErrors;
      });
    }
  };

  // Handle form submission
  const handleSubmit = () => {
    if (!validateForm()) {
      return;
    }

    setLoading(true);
    setNotification(null);

    // Prepare request payload
    const payload = {
      fieldName: formData.fieldName.trim(),
      entityType: formData.entityType,
      loincCode: formData.loincCode.trim() || null,
      description: formData.description.trim() || null,
      fieldType: formData.fieldType,
      acceptedUnits:
        formData.fieldType === "NUMERIC" ? formData.acceptedUnits : null,
    };

    createField(
      payload,
      (response, error) => {
        setLoading(false);

        if (error || response?.error) {
          // Handle error
          const errorMessage =
            response?.message ||
            response?.error ||
            intl.formatMessage({
              id: "analyzer.fieldCreation.error.generic",
              defaultMessage: "Failed to create field",
            });

          setNotification({
            kind: "error",
            title: intl.formatMessage({
              id: "analyzer.fieldCreation.error.title",
              defaultMessage: "Error creating field",
            }),
            subtitle: errorMessage,
          });

          // Check for duplicate name error
          if (
            errorMessage.includes("already exists") ||
            errorMessage.includes("duplicate")
          ) {
            setErrors({
              fieldName: intl.formatMessage({
                id: "analyzer.fieldCreation.validation.unique",
                defaultMessage: "A field with this name already exists",
              }),
            });
          }
        } else {
          // Success
          setNotification({
            kind: "success",
            title: intl.formatMessage({
              id: "analyzer.fieldCreation.success.title",
              defaultMessage: "Field created successfully",
            }),
            subtitle:
              response?.message ||
              intl.formatMessage({
                id: "analyzer.fieldCreation.success.message",
                defaultMessage: "Field is now available for mapping",
              }),
          });

          // Call onFieldCreated callback with created field data
          if (onFieldCreated && response?.field) {
            setTimeout(() => {
              onFieldCreated(response.field, response.id);
              onClose();
            }, 1000);
          } else {
            // Close modal after a short delay
            setTimeout(() => {
              onClose();
            }, 1500);
          }
        }
      },
      null,
    );
  };

  // Handle cancel
  const handleCancel = () => {
    onClose();
  };

  return (
    <ComposedModal
      open={open}
      onClose={handleCancel}
      size="md"
      data-testid="inline-field-creation-modal"
    >
      <ModalHeader
        title={intl.formatMessage({
          id: "analyzer.fieldCreation.modal.title",
          defaultMessage: "Create New OpenELIS Field",
        })}
        label={intl.formatMessage({
          id: "analyzer.fieldCreation.modal.subtitle",
          defaultMessage:
            "Create a new field that will be available for mapping immediately",
        })}
        data-testid="inline-field-creation-modal-header"
      />
      <ModalBody data-testid="inline-field-creation-modal-body">
        {notification && (
          <InlineNotification
            kind={notification.kind}
            title={notification.title}
            subtitle={notification.subtitle}
            hideCloseButton={notification.kind === "success"}
            onClose={() => setNotification(null)}
            lowContrast
            data-testid="inline-field-creation-notification"
          />
        )}

        <Form>
          <FormGroup legendText="">
            <TextInput
              id="field-name"
              labelText={intl.formatMessage({
                id: "analyzer.fieldCreation.fieldName.label",
                defaultMessage: "Field Name",
              })}
              placeholder={intl.formatMessage({
                id: "analyzer.fieldCreation.fieldName.placeholder",
                defaultMessage: "Enter field name",
              })}
              value={formData.fieldName}
              onChange={(e) => handleFieldChange("fieldName", e.target.value)}
              invalid={!!errors.fieldName}
              invalidText={errors.fieldName}
              required
              data-testid="field-name-input"
            />

            <Dropdown
              id="entity-type"
              titleText={intl.formatMessage({
                id: "analyzer.fieldCreation.entityType.label",
                defaultMessage: "Entity Type",
              })}
              label={intl.formatMessage({
                id: "analyzer.fieldCreation.entityType.placeholder",
                defaultMessage: "Select entity type",
              })}
              items={entityTypeOptions}
              selectedItem={
                entityTypeOptions.find(
                  (item) => item.id === formData.entityType,
                ) || null
              }
              onChange={({ selectedItem }) => {
                if (selectedItem) {
                  handleFieldChange("entityType", selectedItem.id);
                }
              }}
              invalid={!!errors.entityType}
              invalidText={errors.entityType}
              required
              data-testid="entity-type-dropdown"
            />

            <TextInput
              id="loinc-code"
              labelText={intl.formatMessage({
                id: "analyzer.fieldCreation.loincCode.label",
                defaultMessage: "LOINC Code (Optional)",
              })}
              placeholder={intl.formatMessage({
                id: "analyzer.fieldCreation.loincCode.placeholder",
                defaultMessage: "Enter LOINC code",
              })}
              value={formData.loincCode}
              onChange={(e) => handleFieldChange("loincCode", e.target.value)}
              data-testid="loinc-code-input"
            />

            <TextInput
              id="description"
              labelText={intl.formatMessage({
                id: "analyzer.fieldCreation.description.label",
                defaultMessage: "Description (Optional)",
              })}
              placeholder={intl.formatMessage({
                id: "analyzer.fieldCreation.description.placeholder",
                defaultMessage: "Enter field description",
              })}
              value={formData.description}
              onChange={(e) => handleFieldChange("description", e.target.value)}
              data-testid="description-input"
            />

            <Dropdown
              id="field-type"
              titleText={intl.formatMessage({
                id: "analyzer.fieldCreation.fieldType.label",
                defaultMessage: "Field Type",
              })}
              label={intl.formatMessage({
                id: "analyzer.fieldCreation.fieldType.placeholder",
                defaultMessage: "Select field type",
              })}
              items={fieldTypeOptions}
              selectedItem={
                fieldTypeOptions.find(
                  (item) => item.id === formData.fieldType,
                ) || null
              }
              onChange={({ selectedItem }) => {
                if (selectedItem) {
                  handleFieldChange("fieldType", selectedItem.id);
                  // Clear acceptedUnits if field type is not NUMERIC
                  if (selectedItem.id !== "NUMERIC") {
                    handleFieldChange("acceptedUnits", []);
                  }
                }
              }}
              data-testid="field-type-dropdown"
            />

            {formData.fieldType === "NUMERIC" && (
              <MultiSelect
                id="accepted-units"
                titleText={intl.formatMessage({
                  id: "analyzer.fieldCreation.acceptedUnits.label",
                  defaultMessage: "Accepted Units",
                })}
                label={intl.formatMessage({
                  id: "analyzer.fieldCreation.acceptedUnits.placeholder",
                  defaultMessage: "Select accepted units",
                })}
                items={unitOptions}
                selectedItems={formData.acceptedUnits
                  .map((unit) => unitOptions.find((item) => item.id === unit))
                  .filter(Boolean)}
                onChange={({ selectedItems }) => {
                  handleFieldChange(
                    "acceptedUnits",
                    selectedItems.map((item) => item.id),
                  );
                }}
                invalid={!!errors.acceptedUnits}
                invalidText={errors.acceptedUnits}
                required
                data-testid="accepted-units-multiselect"
              />
            )}

            <div className="field-creation-confirmation">
              <Checkbox
                id="field-creation-confirmation"
                labelText={intl.formatMessage({
                  id: "analyzer.fieldCreation.confirmation",
                  defaultMessage:
                    "I understand this field will be available for mapping immediately after creation",
                })}
                checked={true}
                readOnly
                data-testid="field-creation-confirmation-checkbox"
              />
            </div>
          </FormGroup>
        </Form>
      </ModalBody>
      <ModalFooter data-testid="inline-field-creation-modal-footer">
        <Button
          kind="secondary"
          onClick={handleCancel}
          disabled={loading}
          data-testid="field-creation-cancel-button"
        >
          <FormattedMessage
            id="analyzer.fieldCreation.cancel"
            defaultMessage="Cancel"
          />
        </Button>
        <Button
          kind="primary"
          onClick={handleSubmit}
          disabled={loading}
          data-testid="field-creation-submit-button"
        >
          <FormattedMessage
            id="analyzer.fieldCreation.create"
            defaultMessage="Create Field"
          />
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default InlineFieldCreationModal;

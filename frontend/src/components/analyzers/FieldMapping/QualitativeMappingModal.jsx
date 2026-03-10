/**
 * QualitativeMappingModal Component
 *
 * Modal for configuring qualitative value mappings (many-to-one)
 */

import React, { useState, useEffect } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  Dropdown,
  Checkbox,
  FormGroup,
  InlineNotification,
  List,
  ListItem,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import "./QualitativeMappingModal.css";

const QualitativeMappingModal = ({
  open,
  onClose,
  field,
  qualitativeMappings = [],
  analyzerValues = [],
  onSave,
}) => {
  const intl = useIntl();
  const [mappings, setMappings] = useState([]);
  const [errors, setErrors] = useState({});
  const [notification, setNotification] = useState(null);

  // Mock OpenELIS codes - in production, this would come from API
  const openelisCodes = [
    { id: "POSITIVE", text: "POSITIVE" },
    { id: "NEGATIVE", text: "NEGATIVE" },
    { id: "REACTIVE", text: "REACTIVE" },
    { id: "NON_REACTIVE", text: "NON_REACTIVE" },
    { id: "INDETERMINATE", text: "INDETERMINATE" },
    { id: "INVALID", text: "INVALID" },
    { id: "EQUIVOCAL", text: "EQUIVOCAL" },
  ];

  // Initialize mappings from props
  useEffect(() => {
    if (open) {
      // If analyzerValues provided, create mappings for each value
      if (analyzerValues && analyzerValues.length > 0) {
        const initialMappings = analyzerValues.map((value) => {
          // Check if mapping already exists for this value
          const existing = qualitativeMappings.find(
            (m) => m.analyzerValue === value,
          );
          return {
            analyzerValue: value,
            openelisCode: existing?.openelisCode || "",
            isDefault: existing?.isDefault || false,
          };
        });
        setMappings(initialMappings);
      } else if (qualitativeMappings && qualitativeMappings.length > 0) {
        // Use existing mappings
        setMappings(
          qualitativeMappings.map((m) => ({
            analyzerValue: m.analyzerValue,
            openelisCode: m.openelisCode,
            isDefault: m.isDefault,
          })),
        );
      } else {
        // Default empty mappings
        setMappings([]);
      }
      setErrors({});
      setNotification(null);
    }
  }, [open, qualitativeMappings, analyzerValues]);

  // Validate form
  const validate = () => {
    const newErrors = {};
    let hasError = false;

    mappings.forEach((mapping, index) => {
      if (!mapping.openelisCode || mapping.openelisCode.trim() === "") {
        newErrors[`mapping-${index}`] = intl.formatMessage({
          id: "analyzer.qualitativeMapping.validation.openelisCode.required",
        });
        hasError = true;
      }
    });

    // Check that at least one default is set
    const hasDefault = mappings.some((m) => m.isDefault);
    if (mappings.length > 0 && !hasDefault) {
      setNotification({
        kind: "warning",
        title: intl.formatMessage({
          id: "analyzer.qualitativeMapping.validation.noDefault",
        }),
        subtitle: intl.formatMessage({
          id: "analyzer.qualitativeMapping.validation.noDefault.subtitle",
        }),
      });
    }

    setErrors(newErrors);
    return !hasError;
  };

  // Handle mapping change
  const handleMappingChange = (index, field, value) => {
    setMappings((prev) => {
      const newMappings = [...prev];
      newMappings[index] = {
        ...newMappings[index],
        [field]: value,
      };
      // If setting isDefault to true, unset others
      if (field === "isDefault" && value === true) {
        newMappings.forEach((m, i) => {
          if (i !== index) {
            m.isDefault = false;
          }
        });
      }
      return newMappings;
    });

    // Clear error for this mapping
    if (errors[`mapping-${index}`]) {
      setErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[`mapping-${index}`];
        return newErrors;
      });
    }

    // Clear notification
    if (notification) {
      setNotification(null);
    }
  };

  // Handle save
  const handleSave = () => {
    if (!validate()) {
      setNotification({
        kind: "error",
        title: intl.formatMessage({
          id: "analyzer.qualitativeMapping.validation.error",
        }),
        subtitle: intl.formatMessage({
          id: "analyzer.qualitativeMapping.validation.error.subtitle",
        }),
      });
      return;
    }

    // Transform mappings to API format
    const mappingsData = mappings.map((m) => ({
      analyzerFieldId: field.id,
      analyzerValue: m.analyzerValue,
      openelisCode: m.openelisCode,
      isDefault: m.isDefault,
    }));

    onSave(mappingsData);
  };

  // Handle close
  const handleClose = () => {
    setErrors({});
    setNotification(null);
    onClose();
  };

  return (
    <ComposedModal
      open={open}
      onClose={handleClose}
      size="lg"
      data-testid="qualitative-mapping-modal"
    >
      <ModalHeader>
        <h2>
          <FormattedMessage id="analyzer.qualitativeMapping.title" />
        </h2>
        <p>
          <FormattedMessage
            id="analyzer.qualitativeMapping.subtitle"
            values={{ fieldName: field?.fieldName || "" }}
          />
        </p>
      </ModalHeader>
      <ModalBody>
        {notification && (
          <InlineNotification
            kind={notification.kind}
            title={notification.title}
            subtitle={notification.subtitle}
            onClose={() => setNotification(null)}
            lowContrast
          />
        )}

        <FormGroup
          legendText={intl.formatMessage({
            id: "analyzer.qualitativeMapping.mappings",
          })}
        >
          {mappings.length === 0 ? (
            <p>
              <FormattedMessage id="analyzer.qualitativeMapping.noValues" />
            </p>
          ) : (
            <List ordered={false} nested={false}>
              {mappings.map((mapping, index) => (
                <ListItem key={index} className="qualitative-mapping-item">
                  <div className="qualitative-mapping-row">
                    <div className="qualitative-mapping-analyzer-value">
                      <strong>{mapping.analyzerValue}</strong>
                    </div>
                    <div className="qualitative-mapping-controls">
                      <Dropdown
                        id={`openelis-code-${index}`}
                        titleText=""
                        label={intl.formatMessage({
                          id: "analyzer.qualitativeMapping.openelisCode.placeholder",
                        })}
                        items={openelisCodes}
                        selectedItem={
                          openelisCodes.find(
                            (c) => c.id === mapping.openelisCode,
                          ) || null
                        }
                        onChange={({ selectedItem }) => {
                          handleMappingChange(
                            index,
                            "openelisCode",
                            selectedItem?.id || "",
                          );
                        }}
                        invalid={!!errors[`mapping-${index}`]}
                        invalidText={errors[`mapping-${index}`]}
                        data-testid={`qualitative-mapping-code-${index}`}
                      />
                      <Checkbox
                        id={`is-default-${index}`}
                        labelText={intl.formatMessage({
                          id: "analyzer.qualitativeMapping.isDefault",
                        })}
                        checked={mapping.isDefault}
                        onChange={(checked) => {
                          handleMappingChange(index, "isDefault", checked);
                        }}
                        data-testid={`qualitative-mapping-default-${index}`}
                      />
                    </div>
                  </div>
                </ListItem>
              ))}
            </List>
          )}
        </FormGroup>
      </ModalBody>
      <ModalFooter>
        <Button
          kind="secondary"
          onClick={handleClose}
          data-testid="qualitative-mapping-cancel"
        >
          <FormattedMessage id="analyzer.form.cancel" />
        </Button>
        <Button
          kind="primary"
          onClick={handleSave}
          data-testid="qualitative-mapping-save"
        >
          <FormattedMessage id="analyzer.form.save" />
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default QualitativeMappingModal;

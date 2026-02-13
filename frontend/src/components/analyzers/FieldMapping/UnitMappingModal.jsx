/**
 * UnitMappingModal Component
 *
 * Modal for configuring unit mappings with conversion factors
 */

import React, { useState, useEffect } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  TextInput,
  Dropdown,
  Toggle,
  FormGroup,
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import "./UnitMappingModal.css";

const UnitMappingModal = ({ open, onClose, field, unitMapping, onSave }) => {
  const intl = useIntl();
  const [formData, setFormData] = useState({
    analyzerUnit: "",
    openelisUnit: "",
    conversionFactor: "",
    rejectIfMismatch: false,
  });
  const [errors, setErrors] = useState({});
  const [notification, setNotification] = useState(null);

  // Mock OpenELIS units list - in production, this would come from API
  const openelisUnits = [
    { id: "g/L", text: "g/L" },
    { id: "mg/dL", text: "mg/dL" },
    { id: "mmol/L", text: "mmol/L" },
    { id: "μmol/L", text: "μmol/L" },
    { id: "IU/L", text: "IU/L" },
    { id: "U/L", text: "U/L" },
    { id: "cells/μL", text: "cells/μL" },
    { id: "×10³/μL", text: "×10³/μL" },
  ];

  // Initialize form data
  useEffect(() => {
    if (unitMapping) {
      setFormData({
        analyzerUnit: unitMapping.analyzerUnit || field?.unit || "",
        openelisUnit: unitMapping.openelisUnit || "",
        conversionFactor: unitMapping.conversionFactor
          ? String(unitMapping.conversionFactor)
          : "",
        rejectIfMismatch: unitMapping.rejectIfMismatch || false,
      });
    } else if (field) {
      setFormData({
        analyzerUnit: field.unit || "",
        openelisUnit: "",
        conversionFactor: "",
        rejectIfMismatch: false,
      });
    }
    setErrors({});
    setNotification(null);
  }, [unitMapping, field, open]);

  // Validate form
  const validate = () => {
    const newErrors = {};

    if (!formData.analyzerUnit || formData.analyzerUnit.trim() === "") {
      newErrors.analyzerUnit = intl.formatMessage({
        id: "analyzer.unitMapping.validation.analyzerUnit.required",
      });
    }

    if (!formData.openelisUnit || formData.openelisUnit.trim() === "") {
      newErrors.openelisUnit = intl.formatMessage({
        id: "analyzer.unitMapping.validation.openelisUnit.required",
      });
    }

    // If units differ and rejectIfMismatch is false, conversion factor is required
    if (
      formData.analyzerUnit &&
      formData.openelisUnit &&
      formData.analyzerUnit !== formData.openelisUnit &&
      !formData.rejectIfMismatch
    ) {
      if (
        !formData.conversionFactor ||
        formData.conversionFactor.trim() === ""
      ) {
        newErrors.conversionFactor = intl.formatMessage({
          id: "analyzer.unitMapping.validation.conversionFactor.required",
        });
      } else {
        const factor = parseFloat(formData.conversionFactor);
        if (isNaN(factor) || factor <= 0) {
          newErrors.conversionFactor = intl.formatMessage({
            id: "analyzer.unitMapping.validation.conversionFactor.invalid",
          });
        }
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Handle field changes
  const handleFieldChange = (fieldName, value) => {
    setFormData((prev) => ({ ...prev, [fieldName]: value }));
    // Clear error for this field
    if (errors[fieldName]) {
      setErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[fieldName];
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
          id: "analyzer.unitMapping.validation.error",
        }),
        subtitle: intl.formatMessage({
          id: "analyzer.unitMapping.validation.error.subtitle",
        }),
      });
      return;
    }

    const mappingData = {
      analyzerFieldId: field.id,
      analyzerUnit: formData.analyzerUnit.trim(),
      openelisUnit: formData.openelisUnit,
      conversionFactor:
        formData.conversionFactor && formData.conversionFactor.trim() !== ""
          ? parseFloat(formData.conversionFactor)
          : null,
      rejectIfMismatch: formData.rejectIfMismatch,
    };

    onSave(mappingData);
  };

  // Handle close
  const handleClose = () => {
    setErrors({});
    setNotification(null);
    onClose();
  };

  const unitsDiffer =
    formData.analyzerUnit &&
    formData.openelisUnit &&
    formData.analyzerUnit !== formData.openelisUnit;

  return (
    <ComposedModal
      open={open}
      onClose={handleClose}
      size="md"
      data-testid="unit-mapping-modal"
    >
      <ModalHeader>
        <h2>
          <FormattedMessage id="analyzer.unitMapping.title" />
        </h2>
        <p>
          <FormattedMessage
            id="analyzer.unitMapping.subtitle"
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

        <FormGroup legendText="">
          {/* Source Unit (read-only) */}
          <TextInput
            id="analyzer-unit"
            labelText={intl.formatMessage({
              id: "analyzer.unitMapping.analyzerUnit",
            })}
            value={formData.analyzerUnit}
            readOnly
            helperText={intl.formatMessage({
              id: "analyzer.unitMapping.analyzerUnit.helper",
            })}
            data-testid="unit-mapping-analyzer-unit"
          />

          {/* Target Unit (dropdown) */}
          <Dropdown
            id="openelis-unit"
            titleText={intl.formatMessage({
              id: "analyzer.unitMapping.openelisUnit",
            })}
            label={intl.formatMessage({
              id: "analyzer.unitMapping.openelisUnit.placeholder",
            })}
            items={openelisUnits}
            selectedItem={
              openelisUnits.find((u) => u.id === formData.openelisUnit) || null
            }
            onChange={({ selectedItem }) => {
              handleFieldChange("openelisUnit", selectedItem?.id || "");
            }}
            invalid={!!errors.openelisUnit}
            invalidText={errors.openelisUnit}
            helperText={intl.formatMessage({
              id: "analyzer.unitMapping.openelisUnit.helper",
            })}
            data-testid="unit-mapping-openelis-unit"
          />

          {/* Conversion Factor (conditional) */}
          {unitsDiffer && !formData.rejectIfMismatch && (
            <TextInput
              id="conversion-factor"
              labelText={intl.formatMessage({
                id: "analyzer.unitMapping.conversionFactor",
              })}
              value={formData.conversionFactor}
              onChange={(e) =>
                handleFieldChange("conversionFactor", e.target.value)
              }
              placeholder="0.0555"
              helperText={intl.formatMessage({
                id: "analyzer.unitMapping.conversionFactor.helper",
                values: {
                  from: formData.analyzerUnit,
                  to: formData.openelisUnit,
                },
              })}
              invalid={!!errors.conversionFactor}
              invalidText={errors.conversionFactor}
              type="number"
              step="0.000001"
              data-testid="unit-mapping-conversion-factor"
            />
          )}

          {/* Reject if Mismatch Toggle */}
          <Toggle
            id="reject-if-mismatch"
            labelText={intl.formatMessage({
              id: "analyzer.unitMapping.rejectIfMismatch",
            })}
            toggled={formData.rejectIfMismatch}
            onToggle={(checked) =>
              handleFieldChange("rejectIfMismatch", checked)
            }
            helperText={intl.formatMessage({
              id: "analyzer.unitMapping.rejectIfMismatch.helper",
            })}
            data-testid="unit-mapping-reject-if-mismatch"
          />
        </FormGroup>
      </ModalBody>
      <ModalFooter>
        <Button
          kind="secondary"
          onClick={handleClose}
          data-testid="unit-mapping-cancel"
        >
          <FormattedMessage id="analyzer.form.cancel" />
        </Button>
        <Button
          kind="primary"
          onClick={handleSave}
          data-testid="unit-mapping-save"
        >
          <FormattedMessage id="analyzer.form.save" />
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default UnitMappingModal;

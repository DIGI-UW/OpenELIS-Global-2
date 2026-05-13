import React, { useState, useEffect } from "react";
import {
  Modal,
  TextInput,
  Button,
  Form,
  FormGroup,
  InlineLoading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

const EditSampleTypeModal = ({ isOpen, onClose, onSubmit, isSubmitting, sampleTypeData }) => {
  const intl = useIntl();
  const [formData, setFormData] = useState({
    id: "",
    name: "",
    frenchName: "",
    displayOrder: "",
    whonetCode: "",
    storageDefaults: "",
  });
  const [errors, setErrors] = useState({});

  // Initialize form data when modal opens with sample type data
  useEffect(() => {
    if (isOpen && sampleTypeData) {
      setFormData({
        id: sampleTypeData.id || "",
        name: sampleTypeData.description || "",
        frenchName: sampleTypeData.frenchName || "", // This might need to be fetched separately
        displayOrder: sampleTypeData.sortOrder || "",
        whonetCode: sampleTypeData.whonetCode || "",
        storageDefaults: sampleTypeData.storageDefaults || "",
      });
      setErrors({});
    }
  }, [isOpen, sampleTypeData]);

  const handleInputChange = (field, value) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));

    // Clear error when user starts typing
    if (errors[field]) {
      setErrors(prev => ({
        ...prev,
        [field]: ""
      }));
    }
  };

  const validateForm = () => {
    const newErrors = {};

    if (!formData.name.trim()) {
      newErrors.name = intl.formatMessage({ id: "sample.type.name.required" });
    }

    if (!formData.frenchName.trim()) {
      newErrors.frenchName = intl.formatMessage({ id: "sample.type.french.name.required" });
    }

    if (!formData.displayOrder.trim()) {
      newErrors.displayOrder = intl.formatMessage({ id: "sample.type.display.order.required" });
    } else if (!/^\d+$/.test(formData.displayOrder.trim())) {
      newErrors.displayOrder = intl.formatMessage({ id: "sample.type.display.order.invalid" });
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (validateForm()) {
      onSubmit({
        id: formData.id,
        sampleTypeEnglishName: formData.name.trim(),
        sampleTypeFrenchName: formData.frenchName.trim(),
        displayOrder: parseInt(formData.displayOrder.trim()),
        whonetCode: formData.whonetCode.trim(),
        storageDefaults: formData.storageDefaults.trim(),
      });
    }
  };

  const handleClose = () => {
    // Reset form when closing
    setFormData({
      id: "",
      name: "",
      frenchName: "",
      displayOrder: "",
      whonetCode: "",
      storageDefaults: "",
    });
    setErrors({});
    onClose();
  };

  return (
    <Modal
      open={isOpen}
      onRequestClose={handleClose}
      modalHeading={<FormattedMessage id="sample.type.edit.modal.title" />}
      modalLabel={<FormattedMessage id="sample.type.management" />}
      primaryButtonText={intl.formatMessage({ id: "button.update" })}
      secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
      onRequestSubmit={handleSubmit}
      onSecondarySubmit={handleClose}
      primaryButtonDisabled={isSubmitting}
      size="md"
    >
      <Form onSubmit={handleSubmit}>
        <FormGroup legendText="">
          <TextInput
            id="edit-sample-type-name"
            labelText={<FormattedMessage id="sample.type.name" />}
            placeholder={intl.formatMessage({ id: "sample.type.name.placeholder" })}
            value={formData.name}
            onChange={(e) => handleInputChange("name", e.target.value)}
            invalid={!!errors.name}
            invalidText={errors.name}
            disabled={isSubmitting}
            required
          />
        </FormGroup>

        <FormGroup legendText="">
          <TextInput
            id="edit-sample-type-french-name"
            labelText={<FormattedMessage id="sample.type.french.name" />}
            placeholder={intl.formatMessage({ id: "sample.type.french.name.placeholder" })}
            value={formData.frenchName}
            onChange={(e) => handleInputChange("frenchName", e.target.value)}
            invalid={!!errors.frenchName}
            invalidText={errors.frenchName}
            disabled={isSubmitting}
            required
          />
        </FormGroup>

        <FormGroup legendText="">
          <TextInput
            id="edit-sample-type-display-order"
            labelText={<FormattedMessage id="sample.type.display.order" />}
            placeholder={intl.formatMessage({ id: "sample.type.display.order.placeholder" })}
            value={formData.displayOrder}
            onChange={(e) => handleInputChange("displayOrder", e.target.value)}
            invalid={!!errors.displayOrder}
            invalidText={errors.displayOrder}
            disabled={isSubmitting}
            required
          />
        </FormGroup>

        <FormGroup legendText="">
          <TextInput
            id="edit-sample-type-whonet-code"
            labelText={<FormattedMessage id="sample.type.whonet.code" />}
            placeholder={intl.formatMessage({ id: "sample.type.whonet.code.placeholder" })}
            value={formData.whonetCode}
            onChange={(e) => handleInputChange("whonetCode", e.target.value)}
            disabled={isSubmitting}
          />
        </FormGroup>

        <FormGroup legendText="">
          <TextInput
            id="edit-sample-type-storage-defaults"
            labelText={<FormattedMessage id="sample.type.storage.defaults" />}
            placeholder={intl.formatMessage({ id: "sample.type.storage.defaults.placeholder" })}
            value={formData.storageDefaults}
            onChange={(e) => handleInputChange("storageDefaults", e.target.value)}
            disabled={isSubmitting}
          />
        </FormGroup>

        {isSubmitting && (
          <InlineLoading
            status="active"
            iconDescription="Updating"
            description={<FormattedMessage id="sample.type.updating" />}
          />
        )}
      </Form>
    </Modal>
  );
};

export default EditSampleTypeModal;
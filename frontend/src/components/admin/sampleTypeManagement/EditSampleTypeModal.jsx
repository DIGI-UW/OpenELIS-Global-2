import React, { useState, useEffect } from "react";
import {
  Modal,
  TextInput,
  TextArea,
  Button,
  Form,
  FormGroup,
  InlineLoading,
  Select,
  SelectItem,
  Toggle,
  Tabs,
  Tab,
  TabList,
  TabPanels,
  TabPanel,
  Grid,
  Column,
  Stack,
  Tile,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

// Domain configuration to match the main component
const DOMAIN_OPTIONS = [
  { value: 'CLINICAL', label: 'Clinical' },
  { value: 'ENVIRONMENTAL', label: 'Environmental' },
  { value: 'VECTOR', label: 'Vector' },
];

const DOMAIN_TAG_KIND = {
  CLINICAL: 'green',
  ENVIRONMENTAL: 'purple',
  VECTOR: 'teal',
};

const EditSampleTypeModal = ({ isOpen, onClose, onSubmit, isSubmitting, sampleTypeData }) => {
  const intl = useIntl();
  const [formData, setFormData] = useState({
    id: "",
    name: "",
    description: "",
    domain: "CLINICAL",
    abbreviation: "",
    frenchName: "",
    displayOrder: "",
    whonetCode: "",
    storageDefaults: "",
    active: true,
  });
  const [errors, setErrors] = useState({});

  // Initialize form data when modal opens with sample type data
  useEffect(() => {
    if (isOpen && sampleTypeData) {
      setFormData({
        id: sampleTypeData.id || "",
        name: sampleTypeData.name || sampleTypeData.description || "",
        description: sampleTypeData.description || sampleTypeData.name || "",
        domain: sampleTypeData.domain || "CLINICAL",
        abbreviation: sampleTypeData.abbreviation || "",
        frenchName: sampleTypeData.frenchName || sampleTypeData.name || "", // Use name as fallback
        displayOrder: sampleTypeData.sortOrder || sampleTypeData.testCount || "0",
        whonetCode: sampleTypeData.whonet || sampleTypeData.whonetCode || "",
        storageDefaults: sampleTypeData.storageDefaults || "",
        active: sampleTypeData.active !== undefined ? sampleTypeData.active : true,
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
      newErrors.name = intl.formatMessage({ id: "sample.type.name.required", defaultMessage: "Sample type name is required" });
    }

    if (!formData.description.trim()) {
      newErrors.description = intl.formatMessage({ id: "sample.type.description.required", defaultMessage: "Description is required" });
    }

    if (!formData.frenchName.trim()) {
      newErrors.frenchName = intl.formatMessage({ id: "sample.type.french.name.required", defaultMessage: "French name is required" });
    }

    // Make displayOrder optional since it's not in the main edit form
    if (formData.displayOrder.trim() && !/^\d+$/.test(formData.displayOrder.trim())) {
      newErrors.displayOrder = intl.formatMessage({ id: "sample.type.display.order.invalid", defaultMessage: "Display order must be a number" });
    }

    // Add basic validation for other fields
    if (formData.abbreviation && formData.abbreviation.length > 10) {
      newErrors.abbreviation = intl.formatMessage({ id: "sample.type.abbreviation.invalid", defaultMessage: "Abbreviation must be 10 characters or less" });
    }

    if (formData.whonetCode && formData.whonetCode.length > 5) {
      newErrors.whonetCode = intl.formatMessage({ id: "sample.type.whonet.invalid", defaultMessage: "WHONET code must be 5 characters or less" });
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
        description: formData.description.trim(),
        domain: formData.domain,
        abbreviation: formData.abbreviation.trim(),
        displayOrder: formData.displayOrder.trim() ? parseInt(formData.displayOrder.trim()) : 999,
        whonetCode: formData.whonetCode.trim(),
        storageDefaults: formData.storageDefaults.trim(),
        active: formData.active,
      });
    }
  };

  const handleClose = () => {
    // Reset form when closing
    setFormData({
      id: "",
      name: "",
      description: "",
      domain: "CLINICAL",
      abbreviation: "",
      frenchName: "",
      displayOrder: "",
      whonetCode: "",
      storageDefaults: "",
      active: true,
    });
    setErrors({});
    onClose();
  };

  return (
    <Modal
      open={isOpen}
      onRequestClose={handleClose}
      modalHeading={
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <span>{formData.name || intl.formatMessage({ id: "sample.type.edit.modal.title", defaultMessage: "Edit Sample Type" })}</span>
          {formData.domain && (
            <Tag type={DOMAIN_TAG_KIND[formData.domain]} size="md">
              <FormattedMessage
                id={`label.sampleType.domain.${formData.domain?.toLowerCase()}`}
                defaultMessage={formData.domain}
              />
            </Tag>
          )}
          {formData.active ? (
            <Tag type="green" size="md">
              <FormattedMessage id="label.active" defaultMessage="Active" />
            </Tag>
          ) : (
            <Tag type="gray" size="md">
              <FormattedMessage id="label.inactive" defaultMessage="Inactive" />
            </Tag>
          )}
        </div>
      }
      modalLabel={<FormattedMessage id="sample.type.management" defaultMessage="Sample Type Management" />}
      primaryButtonText={intl.formatMessage({ id: "button.update", defaultMessage: "Update Sample Type" })}
      secondaryButtonText={intl.formatMessage({ id: "button.cancel", defaultMessage: "Cancel" })}
      onRequestSubmit={handleSubmit}
      onSecondarySubmit={handleClose}
      primaryButtonDisabled={isSubmitting || Object.keys(errors).length > 0 || !formData.name.trim() || !formData.description.trim() || !formData.frenchName.trim()}
      size="lg"
      style={{
        zIndex: 9999,
        '--cds-modal-max-height': '90vh',
        '--cds-modal-max-width': '95vw'
      }}
    >
      <div style={{
        minHeight: '500px',
        background: 'var(--cds-layer)',
        marginTop: '-1px' // Compensate for modal border
      }}>
        <Form onSubmit={handleSubmit}>
          {/* 5-Tab Editor matching the Add form structure */}
          <Tabs style={{
            background: 'var(--cds-layer)',
            borderRadius: 'var(--cds-border-radius)'
          }}>
            <TabList>
              <Tab>
                <FormattedMessage id="tab.sampleType.basicInfo" defaultMessage="Basic Info" />
              </Tab>
              <Tab>
                <FormattedMessage id="tab.sampleType.displayOrder" defaultMessage="Display Order" />
              </Tab>
              <Tab>
                <FormattedMessage id="tab.sampleType.tests" defaultMessage="Associated Tests" />
              </Tab>
              <Tab>
                <FormattedMessage id="tab.sampleType.storage" defaultMessage="Storage & Disposal" />
              </Tab>
              <Tab>
                <FormattedMessage id="tab.sampleType.whonet" defaultMessage="WHONET Mapping" />
              </Tab>
            </TabList>
            <TabPanels>
              {/* Basic Info Tab */}
              <TabPanel style={{ padding: 0 }}>
                <div style={{
                  padding: 'var(--cds-spacing-06)',
                  minHeight: '400px'
                }}>
                  <Stack gap={7}>
                    {/* Primary Information Section */}
                    <div style={{
                      background: 'var(--cds-layer-01)',
                      padding: 'var(--cds-spacing-06)',
                      borderRadius: 'var(--cds-border-radius)',
                      border: '1px solid var(--cds-border-subtle-01)'
                    }}>
                      <Grid fullWidth>
                        <Column lg={8} md={6} sm={4}>
                          <Stack gap={5}>
                            <TextInput
                              id="edit-sample-type-name"
                              labelText={
                                <>
                                  <FormattedMessage id="label.sampleType.name" defaultMessage="Sample Type Name" />
                                  <span style={{ color: 'var(--cds-support-error)' }}> *</span>
                                </>
                              }
                              value={formData.name}
                              onChange={(e) => {
                                handleInputChange("name", e.target.value);
                                if (errors.name) {
                                  setErrors(prev => ({ ...prev, name: '' }));
                                }
                              }}
                              invalid={!!errors.name}
                              invalidText={errors.name}
                              helperText={intl.formatMessage({
                                id: 'helper.sampleType.name',
                                defaultMessage: 'Enter a unique name for this sample type (e.g., "Serum", "Whole Blood")'
                              })}
                              disabled={isSubmitting}
                              autoComplete="off"
                              size="md"
                            />

                            <TextArea
                              id="edit-sample-type-description"
                              labelText={
                                <>
                                  <FormattedMessage id="label.sampleType.description" defaultMessage="Description" />
                                  <span style={{ color: 'var(--cds-support-error)' }}> *</span>
                                </>
                              }
                              value={formData.description}
                              onChange={(e) => {
                                handleInputChange("description", e.target.value);
                                if (errors.description) {
                                  setErrors(prev => ({ ...prev, description: '' }));
                                }
                              }}
                              rows={3}
                              invalid={!!errors.description}
                              invalidText={errors.description}
                              helperText={intl.formatMessage({
                                id: 'helper.sampleType.description',
                                defaultMessage: 'Provide a detailed description of this sample type for lab staff reference'
                              })}
                              disabled={isSubmitting}
                            />
                          </Stack>
                        </Column>

                        <Column lg={8} md={6} sm={4}>
                          <Stack gap={5}>
                            <Select
                              id="edit-sample-type-domain"
                              labelText={
                                <>
                                  <FormattedMessage id="label.sampleType.domain" defaultMessage="Sample Domain" />
                                  <span style={{ color: 'var(--cds-support-error)' }}> *</span>
                                </>
                              }
                              value={formData.domain || 'CLINICAL'}
                              onChange={(e) => handleInputChange("domain", e.target.value)}
                              helperText={intl.formatMessage({
                                id: 'label.sampleType.domain.helper',
                                defaultMessage: 'Determines which workflow mode this sample type appears in'
                              })}
                              disabled={isSubmitting}
                              size="md"
                            >
                              {DOMAIN_OPTIONS.map(opt => (
                                <SelectItem
                                  key={opt.value}
                                  value={opt.value}
                                  text={intl.formatMessage({
                                    id: `label.sampleType.domain.${opt.value.toLowerCase()}`,
                                    defaultMessage: opt.label
                                  })}
                                />
                              ))}
                            </Select>

                            <div style={{ paddingTop: 'var(--cds-spacing-03)' }}>
                              <Toggle
                                id="edit-sample-type-active"
                                labelText={intl.formatMessage({
                                  id: 'label.sampleType.active',
                                  defaultMessage: 'Active Status'
                                })}
                                labelA={intl.formatMessage({
                                  id: 'label.inactive',
                                  defaultMessage: 'Inactive'
                                })}
                                labelB={intl.formatMessage({
                                  id: 'label.active',
                                  defaultMessage: 'Active'
                                })}
                                toggled={formData.active}
                                onToggle={(checked) => handleInputChange("active", checked)}
                                disabled={isSubmitting}
                                size="md"
                              />
                            </div>
                          </Stack>
                        </Column>
                      </Grid>
                    </div>

                    {/* Additional Information Section */}
                    <div style={{
                      background: 'var(--cds-layer-01)',
                      padding: 'var(--cds-spacing-06)',
                      borderRadius: 'var(--cds-border-radius)',
                      border: '1px solid var(--cds-border-subtle-01)'
                    }}>
                      <div style={{ marginBottom: 'var(--cds-spacing-05)' }}>
                        <h6 style={{
                          fontSize: '14px',
                          fontWeight: 600,
                          margin: '0',
                          color: 'var(--cds-text-secondary)',
                          textTransform: 'uppercase',
                          letterSpacing: '0.5px'
                        }}>
                          <FormattedMessage id="section.sampleType.additional" defaultMessage="Additional Information" />
                        </h6>
                      </div>

                      <Grid fullWidth>
                        <Column lg={8} md={4} sm={4}>
                          <TextInput
                            id="edit-sample-type-abbreviation"
                            labelText={intl.formatMessage({
                              id: 'label.sampleType.abbreviation',
                              defaultMessage: 'Abbreviation'
                            })}
                            value={formData.abbreviation || ''}
                            onChange={(e) => handleInputChange("abbreviation", e.target.value)}
                            helperText={intl.formatMessage({
                              id: 'helper.sampleType.abbreviation',
                              defaultMessage: 'Optional short code (max 10 characters)'
                            })}
                            maxLength={10}
                            disabled={isSubmitting}
                            size="md"
                            placeholder="e.g., SER, BLD"
                          />
                        </Column>

                        <Column lg={8} md={4} sm={4}>
                          <TextInput
                            id="edit-sample-type-french-name"
                            labelText={
                              <>
                                <FormattedMessage id="sample.type.french.name" defaultMessage="French Name" />
                                <span style={{ color: 'var(--cds-support-error)' }}> *</span>
                              </>
                            }
                            value={formData.frenchName}
                            onChange={(e) => {
                              handleInputChange("frenchName", e.target.value);
                              if (errors.frenchName) {
                                setErrors(prev => ({ ...prev, frenchName: '' }));
                              }
                            }}
                            invalid={!!errors.frenchName}
                            invalidText={errors.frenchName}
                            helperText={intl.formatMessage({
                              id: 'helper.sampleType.frenchName',
                              defaultMessage: 'French translation of the sample type name'
                            })}
                            disabled={isSubmitting}
                            size="md"
                            placeholder="Nom en français"
                          />
                        </Column>
                      </Grid>
                    </div>
                  </Stack>
                </div>
              </TabPanel>

              {/* Display Order Tab */}
              <TabPanel style={{ padding: 0 }}>
                <div style={{
                  padding: 'var(--cds-spacing-06)',
                  minHeight: '400px'
                }}>
                </div>
              </TabPanel>

              {/* Associated Tests Tab */}
              <TabPanel style={{ padding: 0 }}>
                <div style={{
                  padding: 'var(--cds-spacing-06)',
                  minHeight: '400px'
                }}>
                </div>
              </TabPanel>

              {/* Storage & Disposal Tab */}
              <TabPanel style={{ padding: 0 }}>
                <div style={{
                  padding: 'var(--cds-spacing-06)',
                  minHeight: '400px'
                }}>
                </div>
              </TabPanel>

              {/* WHONET Mapping Tab */}
              <TabPanel style={{ padding: 0 }}>
                <div style={{
                  padding: 'var(--cds-spacing-06)',
                  minHeight: '400px'
                }}>
                </div>
              </TabPanel>
            </TabPanels>
          </Tabs>

          {isSubmitting && (
            <div style={{
              marginTop: 'var(--cds-spacing-05)',
              padding: 'var(--cds-spacing-05)',
              background: 'var(--cds-layer-accent)',
              borderRadius: 'var(--cds-border-radius)',
              border: '1px solid var(--cds-border-subtle)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <InlineLoading
                status="active"
                iconDescription="Updating"
                description={<FormattedMessage id="sample.type.updating" defaultMessage="Updating sample type..." />}
              />
            </div>
          )}
        </Form>
      </div>
    </Modal>
  );
};

export default EditSampleTypeModal;
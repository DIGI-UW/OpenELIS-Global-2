import React, { useState, useCallback, useEffect, useContext } from "react";
import {
  Form,
  FormGroup,
  TextInput,
  TextArea,
  Dropdown,
  DatePicker,
  DatePickerInput,
  TimePicker,
  Button,
  InlineNotification,
  Loading,
  Grid,
  Column,
  ContentSwitcher,
  Switch,
  NumberInput,
  Tag,
} from "@carbon/react";
import { Add, Upload, Renew } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";
import {
  postToOpenElisServerJsonResponse,
  getFromOpenElisServer,
} from "../../../utils/Utils";
import UserSessionDetailsContext from "../../../../UserSessionDetailsContext";

/**
 * SampleIntakeForm - Form for registering samples
 * Sub-stage 1b of the Biorepository Intake workflow
 *
 * Aligns with SRS requirements:
 * Required Fields:
 * - Originating laboratory/source
 * - Sample ID (unique, immutable barcode)
 * - Sample type and category
 * - Date/time of receipt
 * - Storage temperature requirement
 * - Receiving personnel ID (auto-populated from logged-in user)
 *
 * Conditional/Optional Fields:
 * - Ethical approval reference (required if human samples)
 * - Project/study association (required if project-linked)
 * - Material transfer agreement (required if external)
 *
 * @param {Object} props
 * @param {Object} props.shipment - The current shipment (optional)
 * @param {Function} props.onSamplesRegistered - Callback when samples are registered
 * @param {Function} props.onBulkImport - Callback to open bulk import modal
 * @param {Function} props.onCancel - Callback to cancel the form
 */
function SampleIntakeForm({
  shipment,
  onSamplesRegistered,
  onBulkImport,
  onCancel,
}) {
  const intl = useIntl();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);

  // Mode: 0 = single entry, 1 = bulk import
  const [mode, setMode] = useState(0);

  // Helper function to get current time in HH:MM format (24-hour)
  const getCurrentTime = () => {
    const now = new Date();
    const hours = now.getHours();
    const minutes = now.getMinutes();
    return `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}`;
  };

  // Form state for single entry - aligned with SRS requirements
  const [formData, setFormData] = useState({
    // Required fields
    originLab: shipment?.senderOrganization || "", // Originating laboratory/source
    barcode: "", // Sample ID (unique barcode)
    sampleTypeId: null, // Sample type and category
    receiptDate: new Date().toISOString().split("T")[0], // Date of receipt (default to today)
    receiptTime: getCurrentTime(), // Time of receipt (default to now in 24-hour format)
    storageTemperature: "AMBIENT", // Storage temperature requirement
    requiredTempMin: null, // Min temp for custom range
    requiredTempMax: null, // Max temp for custom range

    // Conditional/Optional fields
    externalId: "", // External/Donor ID
    projectId: null, // Project/study association
    ethicsApprovalRef: "", // Ethical approval reference
    mtaReference: "", // Material transfer agreement
    biosafetyLevel: "BSL_1", // Biosafety level
    specialHandling: "", // Special handling instructions
    collectionDate: null, // Original collection date (optional)
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);
  const [registeredSamples, setRegisteredSamples] = useState([]);
  const [generatingBarcode, setGeneratingBarcode] = useState(false);

  // Dropdown options
  const [sampleTypes, setSampleTypes] = useState([]);
  const [projects, setProjects] = useState([]);
  const [organizations, setOrganizations] = useState([]);

  // Storage temperature presets
  const storageTemperatures = [
    {
      id: "AMBIENT",
      text: intl.formatMessage({
        id: "biorepository.temp.ambient",
        defaultMessage: "Room Temperature (15-25°C)",
      }),
    },
    {
      id: "REFRIGERATED",
      text: intl.formatMessage({
        id: "biorepository.temp.refrigerated",
        defaultMessage: "Refrigerated (2-8°C)",
      }),
    },
    {
      id: "FROZEN_20",
      text: intl.formatMessage({
        id: "biorepository.temp.frozen20",
        defaultMessage: "Frozen (-20°C)",
      }),
    },
    {
      id: "FROZEN_80",
      text: intl.formatMessage({
        id: "biorepository.temp.frozen80",
        defaultMessage: "Ultra-low (-80°C)",
      }),
    },
    {
      id: "LIQUID_N2",
      text: intl.formatMessage({
        id: "biorepository.temp.liquidN2",
        defaultMessage: "Liquid Nitrogen (-196°C)",
      }),
    },
    {
      id: "CUSTOM",
      text: intl.formatMessage({
        id: "biorepository.temp.custom",
        defaultMessage: "Custom Range",
      }),
    },
  ];

  const biosafetyLevels = [
    { id: "BSL_1", text: "BSL-1" },
    { id: "BSL_2", text: "BSL-2" },
    { id: "BSL_3", text: "BSL-3" },
    { id: "BSL_4", text: "BSL-4" },
  ];

  // Validation state
  const [errors, setErrors] = useState({});

  // Load dropdown options
  useEffect(() => {
    // Load sample types
    getFromOpenElisServer("/rest/displayList/SAMPLE_TYPE_ACTIVE", (data) => {
      if (data) {
        setSampleTypes(data.map((item) => ({ id: item.id, text: item.value })));
      }
    });

    // Load projects
    getFromOpenElisServer("/rest/displayList/PROJECT", (data) => {
      if (data) {
        setProjects(data.map((item) => ({ id: item.id, text: item.value })));
      }
    });

    // Load organizations for origin lab dropdown
    getFromOpenElisServer("/rest/displayList/REFERRING_CLINIC", (data) => {
      if (data) {
        setOrganizations(
          data.map((item) => ({ id: item.id, text: item.value })),
        );
      }
    });
  }, []);

  // Auto-generate barcode on mount
  useEffect(() => {
    generateNewBarcode();
  }, []);

  const generateNewBarcode = useCallback(() => {
    setGeneratingBarcode(true);
    getFromOpenElisServer(
      "/rest/biorepository/sample/generate-barcode",
      (data) => {
        setGeneratingBarcode(false);
        if (data && data.barcode) {
          setFormData((prev) => ({ ...prev, barcode: data.barcode }));
        }
      },
    );
  }, []);

  // Get temperature range based on preset
  const getTemperatureRange = useCallback((preset) => {
    switch (preset) {
      case "AMBIENT":
        return { min: 15, max: 25 };
      case "REFRIGERATED":
        return { min: 2, max: 8 };
      case "FROZEN_20":
        return { min: -25, max: -15 };
      case "FROZEN_80":
        return { min: -86, max: -76 };
      case "LIQUID_N2":
        return { min: -200, max: -190 };
      default:
        return { min: null, max: null };
    }
  }, []);

  const validateForm = useCallback(() => {
    const newErrors = {};

    // Required: Originating laboratory/source
    if (!formData.originLab.trim()) {
      newErrors.originLab = intl.formatMessage({
        id: "biorepository.sample.error.originLab.required",
        defaultMessage: "Originating laboratory/source is required",
      });
    }

    // Required: Sample ID (barcode)
    if (!formData.barcode.trim()) {
      newErrors.barcode = intl.formatMessage({
        id: "biorepository.sample.error.barcode.required",
        defaultMessage: "Sample ID (barcode) is required",
      });
    }

    // Required: Sample type
    if (!formData.sampleTypeId) {
      newErrors.sampleTypeId = intl.formatMessage({
        id: "biorepository.sample.error.sampleType.required",
        defaultMessage: "Sample type is required",
      });
    }

    // Required: Receipt date
    if (!formData.receiptDate) {
      newErrors.receiptDate = intl.formatMessage({
        id: "biorepository.sample.error.receiptDate.required",
        defaultMessage: "Receipt date is required",
      });
    }

    // Required: Storage temperature
    if (!formData.storageTemperature) {
      newErrors.storageTemperature = intl.formatMessage({
        id: "biorepository.sample.error.storageTemp.required",
        defaultMessage: "Storage temperature requirement is required",
      });
    }

    // Custom temperature range validation
    if (formData.storageTemperature === "CUSTOM") {
      if (
        formData.requiredTempMin === null ||
        formData.requiredTempMax === null
      ) {
        newErrors.customTemp = intl.formatMessage({
          id: "biorepository.sample.error.customTemp.required",
          defaultMessage:
            "Both min and max temperatures are required for custom range",
        });
      } else if (formData.requiredTempMin >= formData.requiredTempMax) {
        newErrors.customTemp = intl.formatMessage({
          id: "biorepository.sample.error.customTemp.invalid",
          defaultMessage: "Min temperature must be less than max temperature",
        });
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }, [formData, intl]);

  const handleInputChange = useCallback(
    (field, value) => {
      setFormData((prev) => ({
        ...prev,
        [field]: value,
      }));
      if (errors[field]) {
        setErrors((prev) => {
          const newErrors = { ...prev };
          delete newErrors[field];
          return newErrors;
        });
      }
    },
    [errors],
  );

  const handleSubmit = useCallback(
    (e) => {
      e.preventDefault();

      if (!validateForm()) {
        return;
      }

      setLoading(true);
      setError(null);

      // Calculate temperature range
      let tempRange;
      if (formData.storageTemperature === "CUSTOM") {
        tempRange = {
          min: formData.requiredTempMin,
          max: formData.requiredTempMax,
        };
      } else {
        tempRange = getTemperatureRange(formData.storageTemperature);
      }

      // Combine receipt date and time
      const receiptDateTime =
        formData.receiptDate && formData.receiptTime
          ? new Date(
              `${formData.receiptDate}T${formData.receiptTime}:00`,
            ).toISOString()
          : new Date().toISOString();

      const sampleData = {
        barcode: formData.barcode.trim(),
        externalId: formData.externalId.trim() || formData.barcode.trim(), // Default to barcode if no external ID
        originLab: formData.originLab.trim(),
        sampleTypeId: formData.sampleTypeId,
        receiptDate: receiptDateTime,
        collectionDate: formData.collectionDate,
        requiredTempMin: tempRange.min,
        requiredTempMax: tempRange.max,
        projectId: formData.projectId,
        biosafetyLevel: formData.biosafetyLevel,
        ethicsApprovalRef: formData.ethicsApprovalRef.trim() || null,
        mtaReference: formData.mtaReference.trim() || null,
        specialHandling: formData.specialHandling.trim() || null,
        shipmentId: shipment?.id || null,
      };

      postToOpenElisServerJsonResponse(
        "/rest/biorepository/sample/register",
        JSON.stringify(sampleData),
        (response) => {
          setLoading(false);
          if (response?.error) {
            setError(response.error);
          } else if (response) {
            setSuccess(true);
            setRegisteredSamples((prev) => [...prev, response]);
            if (onSamplesRegistered) {
              onSamplesRegistered([response]);
            }
            // Reset form for next entry but keep shipment-related defaults
            setFormData((prev) => ({
              ...prev,
              barcode: "",
              externalId: "",
              collectionDate: null,
              specialHandling: "",
            }));
            // Generate new barcode for next sample
            generateNewBarcode();
          }
        },
      );
    },
    [
      formData,
      shipment,
      validateForm,
      onSamplesRegistered,
      intl,
      getTemperatureRange,
      generateNewBarcode,
    ],
  );

  return (
    <div className="sample-intake-form">
      {loading && <Loading withOverlay description="Processing..." />}

      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "biorepository.sample.error.title",
            defaultMessage: "Error",
          })}
          subtitle={error}
          lowContrast
          onClose={() => setError(null)}
        />
      )}

      {success && registeredSamples.length > 0 && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({
            id: "biorepository.sample.success.title",
            defaultMessage: "Samples Registered",
          })}
          subtitle={intl.formatMessage(
            {
              id: "biorepository.sample.success.message",
              defaultMessage: "{count} sample(s) registered successfully.",
            },
            { count: registeredSamples.length },
          )}
          lowContrast
          onClose={() => setSuccess(false)}
        />
      )}

      <Grid>
        <Column lg={16} md={8} sm={4}>
          <ContentSwitcher
            onChange={({ index }) => setMode(index)}
            selectedIndex={mode}
            style={{ marginBottom: "1rem" }}
          >
            <Switch name="single">
              <Add size={16} style={{ marginRight: "0.5rem" }} />
              <FormattedMessage
                id="biorepository.sample.mode.single"
                defaultMessage="Single Entry"
              />
            </Switch>
            <Switch name="bulk">
              <Upload size={16} style={{ marginRight: "0.5rem" }} />
              <FormattedMessage
                id="biorepository.sample.mode.bulk"
                defaultMessage="Bulk Import"
              />
            </Switch>
          </ContentSwitcher>
        </Column>

        {/* Display receiving personnel info */}
        <Column lg={16} md={8} sm={4} style={{ marginBottom: "1rem" }}>
          <Tag type="blue">
            <FormattedMessage
              id="biorepository.sample.receivingPersonnel"
              defaultMessage="Receiving Personnel: {name}"
              values={{
                name:
                  userSessionDetails?.firstName +
                    " " +
                    userSessionDetails?.lastName || "Current User",
              }}
            />
          </Tag>
        </Column>
      </Grid>

      {mode === 0 ? (
        <Form onSubmit={handleSubmit}>
          <Grid>
            {/* REQUIRED FIELDS SECTION */}
            <Column lg={16} md={8} sm={4}>
              <h4 style={{ marginBottom: "1rem", marginTop: "0.5rem" }}>
                <FormattedMessage
                  id="biorepository.sample.section.required"
                  defaultMessage="Required Information"
                />
              </h4>
            </Column>

            {/* Originating Laboratory/Source */}
            <Column lg={8} md={4} sm={4}>
              <FormGroup legendText="">
                <TextInput
                  id="originLab"
                  labelText={intl.formatMessage({
                    id: "biorepository.sample.field.originLab",
                    defaultMessage: "Originating Laboratory/Source *",
                  })}
                  placeholder={intl.formatMessage({
                    id: "biorepository.sample.field.originLab.placeholder",
                    defaultMessage: "Enter source laboratory or organization",
                  })}
                  value={formData.originLab}
                  onChange={(e) =>
                    handleInputChange("originLab", e.target.value)
                  }
                  invalid={!!errors.originLab}
                  invalidText={errors.originLab}
                />
              </FormGroup>
            </Column>

            {/* Sample ID (Barcode) */}
            <Column lg={8} md={4} sm={4}>
              <FormGroup legendText="">
                <div
                  style={{
                    display: "flex",
                    alignItems: "flex-end",
                    gap: "0.5rem",
                  }}
                >
                  <TextInput
                    id="barcode"
                    labelText={intl.formatMessage({
                      id: "biorepository.sample.field.barcode",
                      defaultMessage: "Sample ID (Barcode) *",
                    })}
                    placeholder={intl.formatMessage({
                      id: "biorepository.sample.field.barcode.placeholder",
                      defaultMessage: "Auto-generated or enter manually",
                    })}
                    value={formData.barcode}
                    onChange={(e) =>
                      handleInputChange("barcode", e.target.value)
                    }
                    invalid={!!errors.barcode}
                    invalidText={errors.barcode}
                    style={{ flexGrow: 1 }}
                  />
                  <Button
                    kind="ghost"
                    size="md"
                    hasIconOnly
                    renderIcon={Renew}
                    iconDescription={intl.formatMessage({
                      id: "biorepository.sample.button.generateBarcode",
                      defaultMessage: "Generate new barcode",
                    })}
                    onClick={generateNewBarcode}
                    disabled={generatingBarcode}
                  />
                </div>
              </FormGroup>
            </Column>

            {/* Sample Type */}
            <Column lg={8} md={4} sm={4}>
              <FormGroup legendText="">
                <Dropdown
                  id="sampleType"
                  titleText={intl.formatMessage({
                    id: "biorepository.sample.field.sampleType",
                    defaultMessage: "Sample Type *",
                  })}
                  label={intl.formatMessage({
                    id: "biorepository.sample.field.sampleType.placeholder",
                    defaultMessage: "Select sample type",
                  })}
                  items={sampleTypes}
                  itemToString={(item) => (item ? item.text : "")}
                  selectedItem={sampleTypes.find(
                    (t) => t.id === formData.sampleTypeId,
                  )}
                  onChange={({ selectedItem }) =>
                    handleInputChange("sampleTypeId", selectedItem?.id)
                  }
                  invalid={!!errors.sampleTypeId}
                  invalidText={errors.sampleTypeId}
                />
              </FormGroup>
            </Column>

            {/* Receipt Date */}
            <Column lg={4} md={2} sm={2}>
              <FormGroup legendText="">
                <DatePicker
                  datePickerType="single"
                  value={formData.receiptDate}
                  onChange={([date]) => {
                    if (date) {
                      handleInputChange(
                        "receiptDate",
                        date.toISOString().split("T")[0],
                      );
                    }
                  }}
                >
                  <DatePickerInput
                    id="receiptDate"
                    labelText={intl.formatMessage({
                      id: "biorepository.sample.field.receiptDate",
                      defaultMessage: "Receipt Date *",
                    })}
                    placeholder="mm/dd/yyyy"
                    invalid={!!errors.receiptDate}
                    invalidText={errors.receiptDate}
                  />
                </DatePicker>
              </FormGroup>
            </Column>

            {/* Receipt Time */}
            <Column lg={4} md={2} sm={2}>
              <FormGroup legendText="">
                <TextInput
                  id="receiptTime"
                  type="time"
                  labelText={intl.formatMessage({
                    id: "biorepository.sample.field.receiptTime",
                    defaultMessage: "Receipt Time *",
                  })}
                  value={formData.receiptTime}
                  onChange={(e) =>
                    handleInputChange("receiptTime", e.target.value)
                  }
                  invalid={!!errors.receiptTime}
                  invalidText={errors.receiptTime}
                />
              </FormGroup>
            </Column>

            {/* Storage Temperature Requirement */}
            <Column lg={8} md={4} sm={4}>
              <FormGroup legendText="">
                <Dropdown
                  id="storageTemperature"
                  titleText={intl.formatMessage({
                    id: "biorepository.sample.field.storageTemp",
                    defaultMessage: "Storage Temperature Requirement *",
                  })}
                  label={intl.formatMessage({
                    id: "biorepository.sample.field.storageTemp.placeholder",
                    defaultMessage: "Select storage temperature",
                  })}
                  items={storageTemperatures}
                  itemToString={(item) => (item ? item.text : "")}
                  selectedItem={storageTemperatures.find(
                    (t) => t.id === formData.storageTemperature,
                  )}
                  onChange={({ selectedItem }) =>
                    handleInputChange("storageTemperature", selectedItem?.id)
                  }
                  invalid={!!errors.storageTemperature}
                  invalidText={errors.storageTemperature}
                />
              </FormGroup>
            </Column>

            {/* Custom Temperature Range (conditional) */}
            {formData.storageTemperature === "CUSTOM" && (
              <>
                <Column lg={4} md={2} sm={2}>
                  <FormGroup legendText="">
                    <NumberInput
                      id="requiredTempMin"
                      label={intl.formatMessage({
                        id: "biorepository.sample.field.tempMin",
                        defaultMessage: "Min Temp (°C)",
                      })}
                      value={formData.requiredTempMin || ""}
                      onChange={(e, { value }) =>
                        handleInputChange("requiredTempMin", value)
                      }
                      min={-200}
                      max={50}
                      step={1}
                      invalid={!!errors.customTemp}
                    />
                  </FormGroup>
                </Column>
                <Column lg={4} md={2} sm={2}>
                  <FormGroup legendText="">
                    <NumberInput
                      id="requiredTempMax"
                      label={intl.formatMessage({
                        id: "biorepository.sample.field.tempMax",
                        defaultMessage: "Max Temp (°C)",
                      })}
                      value={formData.requiredTempMax || ""}
                      onChange={(e, { value }) =>
                        handleInputChange("requiredTempMax", value)
                      }
                      min={-200}
                      max={50}
                      step={1}
                      invalid={!!errors.customTemp}
                      invalidText={errors.customTemp}
                    />
                  </FormGroup>
                </Column>
              </>
            )}

            {/* CONDITIONAL/OPTIONAL FIELDS SECTION */}
            <Column lg={16} md={8} sm={4}>
              <h4 style={{ marginBottom: "1rem", marginTop: "1.5rem" }}>
                <FormattedMessage
                  id="biorepository.sample.section.optional"
                  defaultMessage="Additional Information"
                />
              </h4>
            </Column>

            {/* External/Donor ID */}
            <Column lg={8} md={4} sm={4}>
              <FormGroup legendText="">
                <TextInput
                  id="externalId"
                  labelText={intl.formatMessage({
                    id: "biorepository.sample.field.externalId",
                    defaultMessage: "External/Donor ID",
                  })}
                  placeholder={intl.formatMessage({
                    id: "biorepository.sample.field.externalId.placeholder",
                    defaultMessage: "Enter external or donor identifier",
                  })}
                  value={formData.externalId}
                  onChange={(e) =>
                    handleInputChange("externalId", e.target.value)
                  }
                />
              </FormGroup>
            </Column>

            {/* Project */}
            <Column lg={8} md={4} sm={4}>
              <FormGroup legendText="">
                <Dropdown
                  id="project"
                  titleText={intl.formatMessage({
                    id: "biorepository.sample.field.project",
                    defaultMessage: "Project/Study Association",
                  })}
                  label={intl.formatMessage({
                    id: "biorepository.sample.field.project.placeholder",
                    defaultMessage: "Select project (if applicable)",
                  })}
                  items={projects}
                  itemToString={(item) => (item ? item.text : "")}
                  selectedItem={projects.find(
                    (p) => p.id === formData.projectId,
                  )}
                  onChange={({ selectedItem }) =>
                    handleInputChange("projectId", selectedItem?.id)
                  }
                />
              </FormGroup>
            </Column>

            {/* Collection Date (original) */}
            <Column lg={8} md={4} sm={4}>
              <FormGroup legendText="">
                <DatePicker
                  datePickerType="single"
                  onChange={([date]) =>
                    handleInputChange("collectionDate", date?.toISOString())
                  }
                >
                  <DatePickerInput
                    id="collectionDate"
                    labelText={intl.formatMessage({
                      id: "biorepository.sample.field.collectionDate",
                      defaultMessage: "Original Collection Date",
                    })}
                    placeholder="mm/dd/yyyy"
                  />
                </DatePicker>
              </FormGroup>
            </Column>

            {/* Biosafety Level */}
            <Column lg={8} md={4} sm={4}>
              <FormGroup legendText="">
                <Dropdown
                  id="biosafetyLevel"
                  titleText={intl.formatMessage({
                    id: "biorepository.sample.field.biosafetyLevel",
                    defaultMessage: "Biosafety Level",
                  })}
                  items={biosafetyLevels}
                  itemToString={(item) => (item ? item.text : "")}
                  selectedItem={biosafetyLevels.find(
                    (l) => l.id === formData.biosafetyLevel,
                  )}
                  onChange={({ selectedItem }) =>
                    handleInputChange("biosafetyLevel", selectedItem?.id)
                  }
                />
              </FormGroup>
            </Column>

            {/* Ethics Approval Reference */}
            <Column lg={8} md={4} sm={4}>
              <FormGroup legendText="">
                <TextInput
                  id="ethicsApprovalRef"
                  labelText={intl.formatMessage({
                    id: "biorepository.sample.field.ethicsApproval",
                    defaultMessage: "Ethics Approval Reference",
                  })}
                  helperText={intl.formatMessage({
                    id: "biorepository.sample.field.ethicsApproval.helper",
                    defaultMessage: "Required for human samples",
                  })}
                  placeholder={intl.formatMessage({
                    id: "biorepository.sample.field.ethicsApproval.placeholder",
                    defaultMessage: "Enter ethics approval reference",
                  })}
                  value={formData.ethicsApprovalRef}
                  onChange={(e) =>
                    handleInputChange("ethicsApprovalRef", e.target.value)
                  }
                />
              </FormGroup>
            </Column>

            {/* MTA Reference */}
            <Column lg={8} md={4} sm={4}>
              <FormGroup legendText="">
                <TextInput
                  id="mtaReference"
                  labelText={intl.formatMessage({
                    id: "biorepository.sample.field.mtaReference",
                    defaultMessage: "MTA Reference",
                  })}
                  helperText={intl.formatMessage({
                    id: "biorepository.sample.field.mtaReference.helper",
                    defaultMessage: "Required for external samples",
                  })}
                  placeholder={intl.formatMessage({
                    id: "biorepository.sample.field.mtaReference.placeholder",
                    defaultMessage:
                      "Enter Material Transfer Agreement reference",
                  })}
                  value={formData.mtaReference}
                  onChange={(e) =>
                    handleInputChange("mtaReference", e.target.value)
                  }
                />
              </FormGroup>
            </Column>

            {/* Special Handling */}
            <Column lg={16} md={8} sm={4}>
              <FormGroup legendText="">
                <TextArea
                  id="specialHandling"
                  labelText={intl.formatMessage({
                    id: "biorepository.sample.field.specialHandling",
                    defaultMessage: "Special Handling Instructions",
                  })}
                  placeholder={intl.formatMessage({
                    id: "biorepository.sample.field.specialHandling.placeholder",
                    defaultMessage: "Enter any special handling requirements",
                  })}
                  value={formData.specialHandling}
                  onChange={(e) =>
                    handleInputChange("specialHandling", e.target.value)
                  }
                  rows={2}
                />
              </FormGroup>
            </Column>

            {/* Form Actions */}
            <Column lg={16} md={8} sm={4}>
              <div
                className="form-actions"
                style={{ marginTop: "1rem", display: "flex", gap: "1rem" }}
              >
                <Button type="submit" disabled={loading}>
                  <FormattedMessage
                    id="biorepository.sample.button.register"
                    defaultMessage="Register Sample"
                  />
                </Button>
                {onCancel && (
                  <Button
                    kind="secondary"
                    onClick={onCancel}
                    disabled={loading}
                  >
                    <FormattedMessage
                      id="biorepository.button.cancel"
                      defaultMessage="Cancel"
                    />
                  </Button>
                )}
              </div>
            </Column>
          </Grid>
        </Form>
      ) : (
        <div style={{ padding: "2rem", textAlign: "center" }}>
          <h5 style={{ marginBottom: "1rem" }}>
            <FormattedMessage
              id="biorepository.sample.bulkImport.title"
              defaultMessage="Bulk Sample Import"
            />
          </h5>
          <p style={{ marginBottom: "1.5rem", color: "#525252" }}>
            <FormattedMessage
              id="biorepository.sample.bulkImport.description"
              defaultMessage="Upload a CSV manifest file to register multiple samples at once."
            />
          </p>
          <Button
            kind="primary"
            renderIcon={Upload}
            onClick={onBulkImport}
            size="lg"
          >
            <FormattedMessage
              id="biorepository.sample.bulkImport.button"
              defaultMessage="Import Manifest"
            />
          </Button>
        </div>
      )}
    </div>
  );
}

SampleIntakeForm.propTypes = {
  shipment: PropTypes.object,
  onSamplesRegistered: PropTypes.func,
  onBulkImport: PropTypes.func,
  onCancel: PropTypes.func,
};

export default SampleIntakeForm;

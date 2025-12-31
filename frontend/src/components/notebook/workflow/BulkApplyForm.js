import React, { useState, useCallback, useMemo } from "react";
import {
  Modal,
  Form,
  FormGroup,
  TextInput,
  DatePicker,
  DatePickerInput,
  Dropdown,
  NumberInput,
  InlineNotification,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { postToOpenElisServer } from "../../utils/Utils";
import "./NotebookWorkflow.css";

/**
 * BulkApplyForm - Modal form for bulk applying values to selected samples.
 * Any field with a non-empty value will be applied automatically.
 * No checkboxes needed - just fill in the values you want to apply.
 *
 * Per FR-034: System MUST provide bulk apply endpoint for common values.
 *
 * @param {Object} props
 * @param {boolean} props.open - Whether the modal is open
 * @param {function} props.onClose - Callback to close the modal
 * @param {number} props.pageId - The notebook page ID
 * @param {Array} props.selectedSampleIds - Array of selected sample IDs
 * @param {function} props.onApplySuccess - Callback when apply succeeds
 * @param {Array} props.fields - Array of field definitions to show in the form
 */
function BulkApplyForm({
  open,
  onClose,
  pageId,
  selectedSampleIds = [],
  onApplySuccess,
  fields = defaultFields,
}) {
  const intl = useIntl();

  // Form state
  const [formValues, setFormValues] = useState({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Handle field value change
  const handleFieldChange = useCallback((fieldId, value) => {
    setFormValues((prev) => ({
      ...prev,
      [fieldId]: value,
    }));
  }, []);

  // Build data object with only non-empty fields
  const buildApplyData = useCallback(() => {
    const data = {};
    Object.entries(formValues).forEach(([fieldId, value]) => {
      // Include field if value is not empty/null/undefined
      if (value !== undefined && value !== null && value !== "") {
        data[fieldId] = value;
      }
    });
    return data;
  }, [formValues]);

  // Count of fields that will be applied (non-empty values)
  const filledFieldCount = useMemo(() => {
    return Object.values(formValues).filter(
      (v) => v !== undefined && v !== null && v !== "",
    ).length;
  }, [formValues]);

  // Handle form submission
  const handleSubmit = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.bulkApply.noSamplesSelected",
          defaultMessage: "No samples selected",
        }),
      );
      return;
    }

    const applyData = buildApplyData();
    if (Object.keys(applyData).length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.bulkApply.noFieldsEntered",
          defaultMessage: "Please enter at least one value to apply",
        }),
      );
      return;
    }

    setLoading(true);
    setError(null);

    const requestBody = {
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      data: applyData,
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageId}/samples/apply`,
      JSON.stringify(requestBody),
      (status, response) => {
        setLoading(false);
        if (status === 200) {
          // Reset form
          setFormValues({});
          if (onApplySuccess) {
            onApplySuccess(response);
          }
          onClose();
        } else {
          setError(
            intl.formatMessage({
              id: "notebook.bulkApply.error",
              defaultMessage: "Failed to apply values. Please try again.",
            }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    pageId,
    buildApplyData,
    onApplySuccess,
    onClose,
    intl,
  ]);

  // Reset form when modal closes
  const handleClose = useCallback(() => {
    setFormValues({});
    setError(null);
    onClose();
  }, [onClose]);

  // Render field based on type - no checkboxes, all fields always enabled
  const renderField = (field) => {
    const value = formValues[field.id];

    switch (field.type) {
      case "text":
        return (
          <FormGroup key={field.id} legendText="">
            <TextInput
              id={field.id}
              labelText={field.label}
              value={value || ""}
              onChange={(e) => handleFieldChange(field.id, e.target.value)}
              placeholder={field.placeholder}
            />
          </FormGroup>
        );

      case "number":
        return (
          <FormGroup key={field.id} legendText="">
            <NumberInput
              id={field.id}
              label={field.label}
              value={value ?? field.initialValue ?? ""}
              onChange={(e, { value: numValue }) =>
                handleFieldChange(field.id, numValue)
              }
              min={field.min ?? 0}
              max={field.max ?? 10000}
              step={field.step ?? 1}
              allowEmpty
            />
          </FormGroup>
        );

      case "date":
        return (
          <FormGroup key={field.id} legendText="">
            <DatePicker
              datePickerType="single"
              onChange={([date]) =>
                handleFieldChange(field.id, date?.toISOString())
              }
            >
              <DatePickerInput
                id={field.id}
                labelText={field.label}
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </FormGroup>
        );

      case "dropdown":
        return (
          <FormGroup key={field.id} legendText="">
            <Dropdown
              id={field.id}
              titleText={field.label}
              label="Select..."
              items={field.options || []}
              itemToString={(item) => item?.text || ""}
              selectedItem={
                field.options?.find((opt) => opt.id === value) || null
              }
              onChange={({ selectedItem }) =>
                handleFieldChange(field.id, selectedItem?.id)
              }
            />
          </FormGroup>
        );

      default:
        return null;
    }
  };

  return (
    <Modal
      open={open}
      modalHeading={intl.formatMessage({
        id: "notebook.bulkApply.title",
        defaultMessage: "Bulk Apply Values",
      })}
      primaryButtonText={intl.formatMessage({
        id: "notebook.bulkApply.apply",
        defaultMessage: "Apply to Selected",
      })}
      secondaryButtonText={intl.formatMessage({
        id: "notebook.bulkApply.cancel",
        defaultMessage: "Cancel",
      })}
      onRequestSubmit={handleSubmit}
      onRequestClose={handleClose}
      primaryButtonDisabled={
        loading || selectedSampleIds.length === 0 || filledFieldCount === 0
      }
      size="lg"
    >
      <div className="bulk-apply-form">
        {/* Selection summary */}
        <div className="bulk-apply-summary">
          <Tag type="blue">
            <FormattedMessage
              id="notebook.bulkApply.samplesSelected"
              defaultMessage="{count} samples selected"
              values={{ count: selectedSampleIds.length }}
            />
          </Tag>
          {filledFieldCount > 0 && (
            <Tag type="green">
              <FormattedMessage
                id="notebook.bulkApply.fieldsToApply"
                defaultMessage="{count} fields will be applied"
                values={{ count: filledFieldCount }}
              />
            </Tag>
          )}
        </div>

        {/* Error display */}
        {error && (
          <InlineNotification
            kind="error"
            title={error}
            hideCloseButton
            lowContrast
          />
        )}

        {/* Instructions */}
        <p className="bulk-apply-instructions">
          <FormattedMessage
            id="notebook.bulkApply.instructions"
            defaultMessage="Enter values in the fields below. Only fields with values will be applied to the selected samples. Empty fields will be left unchanged."
          />
        </p>

        {/* Form fields in a grid layout */}
        <Form className="bulk-apply-fields-grid">
          {fields.map((field) => renderField(field))}
        </Form>
      </div>
    </Modal>
  );
}

// Default fields for immunology workflow (Initial Processing - Page 2)
// Per spec: a) Volume Determination, b) Cell Count & Isolation, c) Log parameters/timestamps
const defaultFields = [
  // Section a: Volume Determination
  {
    id: "initialVolume",
    type: "number",
    label: "Initial Volume (mL)",
    min: 0,
    max: 100,
    step: 0.1,
  },
  {
    id: "finalVolume",
    type: "number",
    label: "Final Volume (mL)",
    min: 0,
    max: 100,
    step: 0.1,
  },
  // Section b: Cell Count & Isolation
  {
    id: "cellCount",
    type: "text",
    label: "Cell Count (cells/mL)",
    placeholder: "e.g., 2.4×10⁶",
  },
  {
    id: "cellViability",
    type: "number",
    label: "Cell Viability (%)",
    min: 0,
    max: 100,
    step: 0.1,
  },
  {
    id: "isolationMethod",
    type: "dropdown",
    label: "Isolation Method",
    options: [
      { id: "ficoll", text: "Ficoll Density Gradient" },
      { id: "centrifugation", text: "Centrifugation" },
      { id: "magnetic", text: "Magnetic Bead Separation" },
      { id: "other", text: "Other" },
    ],
  },
  {
    id: "centrifugeSpeed",
    type: "number",
    label: "Centrifuge Speed (RPM)",
    min: 0,
    max: 20000,
    step: 100,
  },
  {
    id: "centrifugeTime",
    type: "number",
    label: "Centrifuge Time (min)",
    min: 0,
    max: 60,
    step: 1,
  },
  // Section c: Log parameters and timestamps
  {
    id: "processingStartTime",
    type: "date",
    label: "Processing Start Time",
  },
  {
    id: "processingEndTime",
    type: "date",
    label: "Processing End Time",
  },
  {
    id: "technician",
    type: "text",
    label: "Technician",
    placeholder: "Enter technician name",
  },
  {
    id: "notes",
    type: "text",
    label: "Notes",
    placeholder: "Enter processing notes",
  },
];

export default BulkApplyForm;

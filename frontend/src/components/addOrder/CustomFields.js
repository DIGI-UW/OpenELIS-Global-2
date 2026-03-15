import React, { useEffect, useState } from "react";
import {
  Column,
  DatePicker,
  DatePickerInput,
  Grid,
  NumberInput,
  Select,
  SelectItem,
  TextInput,
  Toggle,
} from "@carbon/react";
import { getFromOpenElisServer } from "../utils/Utils";
import { FormattedMessage, useIntl } from "react-intl";

const CustomFields = ({ customFieldValues, setCustomFieldValues }) => {
  const intl = useIntl();
  const [customFields, setCustomFields] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getFromOpenElisServer("/rest/custom-fields", (data) => {
      if (Array.isArray(data)) {
        setCustomFields(data);
      }
      setLoading(false);
    });
  }, []);

  const handleFieldChange = (fieldId, value) => {
    const existingValues = customFieldValues ? [...customFieldValues] : [];
    const existingIndex = existingValues.findIndex(
      (v) => v.customFieldId === fieldId,
    );
    if (existingIndex >= 0) {
      existingValues[existingIndex] = {
        customFieldId: fieldId,
        fieldValue: value,
      };
    } else {
      existingValues.push({ customFieldId: fieldId, fieldValue: value });
    }
    setCustomFieldValues(existingValues);
  };

  const getFieldValue = (fieldId) => {
    if (!customFieldValues) return "";
    const entry = customFieldValues.find((v) => v.customFieldId === fieldId);
    return entry ? entry.fieldValue : "";
  };

  const renderField = (field) => {
    const fieldId = field.id;
    const value = getFieldValue(fieldId);
    const label = field.labelKey
      ? intl.formatMessage(
          { id: field.labelKey, defaultMessage: field.name },
          {},
        )
      : field.name;
    const required = field.isRequired || false;

    switch (field.fieldType) {
      case "STRING":
        return (
          <Column lg={8} md={4} sm={4} key={fieldId}>
            <TextInput
              id={`custom-field-${fieldId}`}
              labelText={
                <>
                  {label}
                  {required && <span className="requiredlabel">*</span>}
                </>
              }
              value={value}
              onChange={(e) => handleFieldChange(fieldId, e.target.value)}
            />
          </Column>
        );

      case "INTEGER":
        return (
          <Column lg={8} md={4} sm={4} key={fieldId}>
            <NumberInput
              id={`custom-field-${fieldId}`}
              label={
                <>
                  {label}
                  {required && <span className="requiredlabel">*</span>}
                </>
              }
              value={value ? parseInt(value) : ""}
              onChange={(e, { value: numValue }) =>
                handleFieldChange(fieldId, String(numValue))
              }
              allowEmpty
              hideSteppers
            />
          </Column>
        );

      case "DECIMAL":
        return (
          <Column lg={8} md={4} sm={4} key={fieldId}>
            <NumberInput
              id={`custom-field-${fieldId}`}
              label={
                <>
                  {label}
                  {required && <span className="requiredlabel">*</span>}
                </>
              }
              value={value ? parseFloat(value) : ""}
              onChange={(e, { value: numValue }) =>
                handleFieldChange(fieldId, String(numValue))
              }
              step={0.01}
              allowEmpty
              hideSteppers
            />
          </Column>
        );

      case "BOOLEAN":
        return (
          <Column lg={8} md={4} sm={4} key={fieldId}>
            <Toggle
              id={`custom-field-${fieldId}`}
              labelText={label}
              toggled={value === "true"}
              onToggle={(checked) =>
                handleFieldChange(fieldId, String(checked))
              }
              labelA={intl.formatMessage({
                id: "customfield.no",
                defaultMessage: "No",
              })}
              labelB={intl.formatMessage({
                id: "customfield.yes",
                defaultMessage: "Yes",
              })}
            />
          </Column>
        );

      case "DATE":
        return (
          <Column lg={8} md={4} sm={4} key={fieldId}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) => {
                if (dates && dates.length > 0) {
                  const date = dates[0];
                  const formatted = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
                  handleFieldChange(fieldId, formatted);
                }
              }}
            >
              <DatePickerInput
                id={`custom-field-${fieldId}`}
                labelText={
                  <>
                    {label}
                    {required && <span className="requiredlabel">*</span>}
                  </>
                }
                placeholder="yyyy-mm-dd"
                value={value}
              />
            </DatePicker>
          </Column>
        );

      case "CHOICE": {
        let options = [];
        try {
          if (field.options) {
            options = JSON.parse(field.options);
          }
        } catch (e) {
          console.error("Failed to parse custom field options:", e);
        }
        return (
          <Column lg={8} md={4} sm={4} key={fieldId}>
            <Select
              id={`custom-field-${fieldId}`}
              labelText={
                <>
                  {label}
                  {required && <span className="requiredlabel">*</span>}
                </>
              }
              value={value}
              onChange={(e) => handleFieldChange(fieldId, e.target.value)}
            >
              <SelectItem value="" text="" />
              {options.map((option, index) => (
                <SelectItem
                  key={index}
                  value={option.value}
                  text={option.display}
                />
              ))}
            </Select>
          </Column>
        );
      }

      default:
        return (
          <Column lg={8} md={4} sm={4} key={fieldId}>
            <TextInput
              id={`custom-field-${fieldId}`}
              labelText={label}
              value={value}
              onChange={(e) => handleFieldChange(fieldId, e.target.value)}
            />
          </Column>
        );
    }
  };

  if (loading) {
    return null;
  }

  if (customFields.length === 0) {
    return null;
  }

  return (
    <div className="orderLegendBody">
      <h3>
        <FormattedMessage
          id="customfield.title"
          defaultMessage="Custom Fields"
        />
      </h3>
      <Grid>
        {customFields.map((field) => renderField(field))}
        <Column lg={16} md={8} sm={3}>
          {" "}
          &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;{" "}
        </Column>
      </Grid>
    </div>
  );
};

export default CustomFields;

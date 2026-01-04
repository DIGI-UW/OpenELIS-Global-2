import React from "react";
import { Select, SelectItem } from "@carbon/react";
import { useIntl } from "react-intl";
import { useField } from "formik";
import PropTypes from "prop-types";

/**
 * FormSelectField - Reusable select dropdown component with Formik integration
 *
 * @param {string} name - Field name (required)
 * @param {string} label - Internationalization key for label
 * @param {Array} options - Array of {value, label} objects
 * @param {boolean} required - Whether field is required
 * @param {boolean} disabled - Whether field is disabled
 * @param {string} defaultOptionLabel - Internationalization key for default option
 */
const FormSelectField = ({
  name,
  label,
  options = [],
  required = false,
  disabled = false,
  defaultOptionLabel = "select.default",
  ...props
}) => {
  const intl = useIntl();
  const [field, meta] = useField(name);

  return (
    <Select
      id={name}
      {...field}
      {...props}
      labelText={label ? intl.formatMessage({ id: label }) : ""}
      invalid={meta.touched && !!meta.error}
      invalidText={
        meta.touched && meta.error ? intl.formatMessage({ id: meta.error }) : ""
      }
      disabled={disabled}
      required={required}
    >
      <SelectItem
        text={intl.formatMessage({ id: defaultOptionLabel })}
        value=""
      />
      {options.map((option) => (
        <SelectItem
          key={option.value}
          text={option.label}
          value={option.value}
        />
      ))}
    </Select>
  );
};

FormSelectField.propTypes = {
  name: PropTypes.string.isRequired,
  label: PropTypes.string,
  options: PropTypes.arrayOf(
    PropTypes.shape({
      value: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
        .isRequired,
      label: PropTypes.string.isRequired,
    }),
  ),
  required: PropTypes.bool,
  disabled: PropTypes.bool,
  defaultOptionLabel: PropTypes.string,
};

export default FormSelectField;

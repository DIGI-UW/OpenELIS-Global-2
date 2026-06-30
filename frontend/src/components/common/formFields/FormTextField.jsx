import React from "react";
import { TextInput } from "@carbon/react";
import { useIntl } from "react-intl";
import { useField } from "formik";
import PropTypes from "prop-types";

/**
 * FormTextField - Reusable text input component with Formik integration
 *
 * @param {string} name - Field name (required)
 * @param {string} label - Internationalization key for label
 * @param {boolean} required - Whether field is required
 * @param {boolean} disabled - Whether field is disabled
 * @param {string} helperText - Internationalization key for helper text
 * @param {string} placeholder - Internationalization key for placeholder
 * @param {string} type - Input type (text, email, password, etc.)
 */
const FormTextField = ({
  name,
  label,
  required = false,
  disabled = false,
  helperText,
  placeholder,
  type = "text",
  ...props
}) => {
  const intl = useIntl();
  const [field, meta] = useField(name);

  return (
    <TextInput
      id={name}
      {...field}
      {...props}
      type={type}
      labelText={label ? intl.formatMessage({ id: label }) : ""}
      helperText={helperText ? intl.formatMessage({ id: helperText }) : ""}
      placeholder={placeholder ? intl.formatMessage({ id: placeholder }) : ""}
      invalid={meta.touched && !!meta.error}
      invalidText={
        meta.touched && meta.error ? intl.formatMessage({ id: meta.error }) : ""
      }
      disabled={disabled}
      required={required}
    />
  );
};

FormTextField.propTypes = {
  name: PropTypes.string.isRequired,
  label: PropTypes.string,
  required: PropTypes.bool,
  disabled: PropTypes.bool,
  helperText: PropTypes.string,
  placeholder: PropTypes.string,
  type: PropTypes.string,
};

export default FormTextField;

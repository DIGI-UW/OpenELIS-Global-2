import React from "react";
import CustomDatePicker from "../CustomDatePicker";
import { useIntl } from "react-intl";
import { useField } from "formik";
import PropTypes from "prop-types";

/**
 * FormDatePickerField - Reusable date picker component with Formik integration
 *
 * @param {string} name - Field name (required)
 * @param {string} label - Internationalization key for label
 * @param {boolean} required - Whether field is required
 * @param {boolean} disallowFutureDate - Prevent selection of future dates
 * @param {boolean} disallowPastDate - Prevent selection of past dates
 * @param {boolean} disabled - Whether field is disabled
 */
const FormDatePickerField = ({
  name,
  label,
  required = false,
  disallowFutureDate = false,
  disallowPastDate = false,
  disabled = false,
  ...props
}) => {
  const intl = useIntl();
  const [field, meta, helpers] = useField(name);

  return (
    <CustomDatePicker
      id={name}
      value={field.value}
      onChange={(date) => helpers.setValue(date)}
      labelText={label ? intl.formatMessage({ id: label }) : ""}
      invalid={meta.touched && !!meta.error}
      invalidText={
        meta.touched && meta.error ? intl.formatMessage({ id: meta.error }) : ""
      }
      disallowFutureDate={disallowFutureDate}
      disallowPastDate={disallowPastDate}
      disabled={disabled}
      {...props}
    />
  );
};

FormDatePickerField.propTypes = {
  name: PropTypes.string.isRequired,
  label: PropTypes.string,
  required: PropTypes.bool,
  disallowFutureDate: PropTypes.bool,
  disallowPastDate: PropTypes.bool,
  disabled: PropTypes.bool,
};

export default FormDatePickerField;

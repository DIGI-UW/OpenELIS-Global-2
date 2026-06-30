import React from "react";
import { Checkbox } from "@carbon/react";
import { useIntl } from "react-intl";
import { useField } from "formik";
import PropTypes from "prop-types";

/**
 * FormCheckboxField - Reusable checkbox component with Formik integration
 *
 * @param {string} name - Field name (required)
 * @param {string} label - Internationalization key for label
 * @param {boolean} disabled - Whether field is disabled
 */
const FormCheckboxField = ({ name, label, disabled = false, ...props }) => {
  const intl = useIntl();
  const [field, meta] = useField({ name, type: "checkbox" });

  return (
    <Checkbox
      id={name}
      {...field}
      {...props}
      labelText={label ? intl.formatMessage({ id: label }) : ""}
      checked={field.value}
      disabled={disabled}
      invalid={meta.touched && !!meta.error}
      invalidText={
        meta.touched && meta.error ? intl.formatMessage({ id: meta.error }) : ""
      }
    />
  );
};

FormCheckboxField.propTypes = {
  name: PropTypes.string.isRequired,
  label: PropTypes.string,
  disabled: PropTypes.bool,
};

export default FormCheckboxField;

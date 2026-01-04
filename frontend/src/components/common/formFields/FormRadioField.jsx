import React from "react";
import { RadioButtonGroup, RadioButton } from "@carbon/react";
import { useIntl } from "react-intl";
import { useField } from "formik";
import PropTypes from "prop-types";

/**
 * FormRadioField - Reusable radio button group component with Formik integration
 *
 * @param {string} name - Field name (required)
 * @param {string} legendText - Internationalization key for legend text
 * @param {Array} options - Array of {value, label} objects
 * @param {boolean} disabled - Whether field is disabled
 */
const FormRadioField = ({
  name,
  legendText,
  options = [],
  disabled = false,
  ...props
}) => {
  const intl = useIntl();
  const [field, meta, helpers] = useField(name);

  return (
    <RadioButtonGroup
      name={name}
      legendText={legendText ? intl.formatMessage({ id: legendText }) : ""}
      valueSelected={field.value}
      onChange={(value) => helpers.setValue(value)}
      invalid={meta.touched && !!meta.error}
      invalidText={
        meta.touched && meta.error ? intl.formatMessage({ id: meta.error }) : ""
      }
      disabled={disabled}
      {...props}
    >
      {options.map((option) => (
        <RadioButton
          key={option.value}
          id={`${name}-${option.value}`}
          labelText={option.label}
          value={option.value}
        />
      ))}
    </RadioButtonGroup>
  );
};

FormRadioField.propTypes = {
  name: PropTypes.string.isRequired,
  legendText: PropTypes.string,
  options: PropTypes.arrayOf(
    PropTypes.shape({
      value: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
        .isRequired,
      label: PropTypes.string.isRequired,
    }),
  ).isRequired,
  disabled: PropTypes.bool,
};

export default FormRadioField;

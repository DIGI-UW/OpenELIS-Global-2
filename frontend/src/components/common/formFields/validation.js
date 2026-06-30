import * as Yup from "yup";

/**
 * Validation Utilities for OpenELIS Form Components
 * Provides reusable Yup validation schemas with internationalized error messages
 */

/**
 * Common validation schemas
 */
export const validationSchemas = {
  /**
   * Required field validation
   * @param {string} fieldName - Field name for error message key
   */
  required: (fieldName) =>
    Yup.string().required(`${fieldName}.validation.required`),

  /**
   * Email validation
   */
  email: () =>
    Yup.string()
      .email("validation.email.invalid")
      .required("validation.email.required"),

  /**
   * Phone number validation (international format)
   */
  phone: () =>
    Yup.string()
      .matches(/^\+?[1-9]\d{1,14}$/, "validation.phone.invalid")
      .required("validation.phone.required"),

  /**
   * Date validation
   */
  date: () => Yup.date().required("validation.date.required"),

  /**
   * Date range validation
   */
  dateRange: (startField, endField) =>
    Yup.date()
      .required("validation.date.required")
      .when(startField, (startDate, schema) => {
        return startDate
          ? schema.min(startDate, `${endField}.validation.after.${startField}`)
          : schema;
      }),

  /**
   * Alphanumeric validation
   */
  alphanumeric: () =>
    Yup.string()
      .matches(/^[a-zA-Z0-9]+$/, "validation.alphanumeric.invalid")
      .required("validation.required"),

  /**
   * Numeric validation
   */
  numeric: () =>
    Yup.number()
      .typeError("validation.numeric.invalid")
      .required("validation.required"),

  /**
   * Positive number validation
   */
  positiveNumber: () =>
    Yup.number()
      .typeError("validation.numeric.invalid")
      .positive("validation.numeric.positive")
      .required("validation.required"),

  /**
   * Integer validation
   */
  integer: () =>
    Yup.number()
      .typeError("validation.numeric.invalid")
      .integer("validation.numeric.integer")
      .required("validation.required"),

  /**
   * URL validation
   */
  url: () =>
    Yup.string()
      .url("validation.url.invalid")
      .required("validation.url.required"),

  /**
   * Minimum length validation
   */
  minLength: (min, fieldName) =>
    Yup.string()
      .min(min, `${fieldName}.validation.minlength`)
      .required(`${fieldName}.validation.required`),

  /**
   * Maximum length validation
   */
  maxLength: (max, fieldName) =>
    Yup.string()
      .max(max, `${fieldName}.validation.maxlength`)
      .required(`${fieldName}.validation.required`),

  /**
   * Pattern validation
   */
  pattern: (regex, errorKey) =>
    Yup.string().matches(regex, errorKey).required("validation.required"),

  /**
   * Optional field (no required constraint)
   */
  optional: () => Yup.string().nullable(),
};

/**
 * Helper function to create a Yup validation schema from field definitions
 *
 * @param {Object} fields - Object where keys are field names and values are validation types
 * @returns {Yup.ObjectSchema} - Yup validation schema
 *
 * @example
 * const schema = createFormSchema({
 *   email: 'email',
 *   phone: 'phone',
 *   sampleId: 'required'
 * });
 */
export const createFormSchema = (fields) => {
  const schema = {};
  Object.keys(fields).forEach((key) => {
    const validationType = fields[key];
    if (typeof validationType === "string") {
      schema[key] = validationSchemas[validationType]
        ? validationSchemas[validationType](key)
        : validationSchemas.required(key);
    } else if (typeof validationType === "function") {
      schema[key] = validationType;
    }
  });
  return Yup.object().shape(schema);
};

/**
 * Date-specific validation helpers
 */
export const dateValidation = {
  /**
   * Validate date is not in the future
   */
  notFuture: () =>
    Yup.date()
      .max(new Date(), "validation.date.notfuture")
      .required("validation.date.required"),

  /**
   * Validate date is not in the past
   */
  notPast: () =>
    Yup.date()
      .min(new Date(), "validation.date.notpast")
      .required("validation.date.required"),

  /**
   * Validate date is within a range
   */
  withinRange: (minDate, maxDate) =>
    Yup.date()
      .min(minDate, "validation.date.min")
      .max(maxDate, "validation.date.max")
      .required("validation.date.required"),
};

/**
 * Custom validation for OpenELIS-specific fields
 */
export const openelisValidation = {
  /**
   * Sample accession number validation
   */
  accessionNumber: () =>
    Yup.string()
      .matches(/^\d{4}-\d{5}$/, "validation.accession.invalid")
      .required("validation.accession.required"),

  /**
   * Patient ID validation
   */
  patientId: () =>
    Yup.string()
      .min(1, "validation.patientid.minlength")
      .required("validation.patientid.required"),

  /**
   * Lab number validation
   */
  labNumber: () =>
    Yup.string()
      .matches(/^[A-Z0-9-]+$/, "validation.labnumber.invalid")
      .required("validation.labnumber.required"),
};

export default {
  validationSchemas,
  createFormSchema,
  dateValidation,
  openelisValidation,
};

package org.openelisglobal.common.validator;

import java.util.regex.Pattern;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.util.validator.CustomDateValidator;
import org.openelisglobal.common.util.validator.CustomDateValidator.DateRelation;
import org.springframework.validation.Errors;

public class ValidationHelper {

    // prevents constructing object all methods are static
    private ValidationHelper() {
    }

    public static final String PATIENT_ID_REGEX = "(?i)^[-a-z0-9/]*$";
    public static final String PHONE_REGEX = "^[-+()0-9a-z./ ]*$";
    public static final String GENDER_REGEX = "^$|^M$|^F$";
    public static final String MESSAGE_KEY_REGEX = "(?i)^$|^[a-z0-9_]+(\\.[a-z0-9_]+)*$";
    public static final String ID_REGEX = "^[0-9]*$";
    public static final String HOUR_REGEX = "^[01]?[0-9]|2[0-3]$";
    public static final String MINUTES_REGEX = "^[0-5]?[0-9]$";
    public static final String YES_NO_REGEX = "^$|^" + IActionConstants.YES + "$|^" + IActionConstants.NO + "$";
    public static final String FLOAT_REGEX = "^[+-]?Infinity$|^([+-]?\\d*\\.?\\d*)$";
    public static final String ALPHA_NUM_REGEX = "(?i)^[a-z0-9]*$";

    private static final String DEFAULT_PREFIX = "Field ";

    /*
     * STRING METHODS
     */

    public static void validateFieldRequired(String value, String name, String displayName, Errors errors) {
        if (org.apache.commons.validator.GenericValidator.isBlankOrNull(value) || value.equals("null")) {
            errors.rejectValue(name, "error.field.required", new Object[] { displayName },
                    DEFAULT_PREFIX + displayName + " is required");
        }
    }

    public static void validateFieldLengthMax(String value, String name, String displayName, Errors errors, int max) {
        int length = 0;
        if (value != null) {
            length = value.length();
        }
        if (length > max) {
            errors.rejectValue(name, "error.field.length.long", new Object[] { displayName, Integer.toString(max) },
                    DEFAULT_PREFIX + displayName + " has too many characters");
        }
    }

    public static void validateFieldLengthMin(String value, String name, String displayName, Errors errors, int min) {
        int length = 0;
        if (value != null) {
            length = value.length();
        }
        if (length < min) {
            errors.rejectValue(name, "error.field.length.short", new Object[] { displayName, Integer.toString(min) },
                    DEFAULT_PREFIX + displayName + " has too few characters");
        }
    }

    public static void validateFieldMatchRegex(String value, String name, String displayName, Errors errors,
            String regex) {
        if (!Pattern.matches(regex, value)) {
            errors.rejectValue(name, "error.field.format.invalid", new Object[] { displayName },
                    DEFAULT_PREFIX + displayName + " is an invalid format");
        }
    }

    public static void validateFieldCharSet(String value, String name, String displayName, Errors errors,
            String charSet) {
        if (!GenericValidator.isBlankOrNull(value)) {
            if (!Pattern.matches("^[" + charSet + "]*$", value)) {
                String charsetForDisplay = charSet.replaceAll("\\\\(?!\\\\)", "");
                errors.rejectValue(name, "error.field.charset.invalid", new Object[] { displayName, charsetForDisplay },
                        DEFAULT_PREFIX + displayName + " has an invalid character. Allowed characters are '"
                                + charsetForDisplay + "'");
            }
        }
    }

    public static void validateDateField(String value, String name, String displayName, Errors errors,
            DateRelation relative) {
        String result = CustomDateValidator.getInstance().validateDate(CustomDateValidator.getInstance().getDate(value),
                relative);
        if (!IActionConstants.VALID.equals(result)) {
            errors.rejectValue(name, "error.field.date.invalid", new Object[] { displayName, result },
                    DEFAULT_PREFIX + displayName + " is not in a valid date format");
        }
    }

    public static void validateTimeField(String value, String name, String displayName, Errors errors) {
        if (!CustomDateValidator.getInstance().validate24HourTime(value)) {
            errors.rejectValue(name, "error.field.time.invalid", new Object[] { displayName, value },
                    DEFAULT_PREFIX + displayName + " is not in a valid time format");
        }
    }

    public static void validateOptionFieldIgnoreCase(String value, String name, String displayName, Errors errors,
            String[] possibleValues) {
        boolean match = false;
        for (String possibleValue : possibleValues) {
            // null safety
            if (possibleValue == null) {
                if (value == null) {
                    match = true;
                }
            } else {
                if (possibleValue.equalsIgnoreCase(value)) {
                    match = true;
                }
            }

            if (match) {
                break;
            }
        }
        if (!match) {
            errors.rejectValue(name, "error.field.option.invalid", new Object[] { displayName },
                    DEFAULT_PREFIX + displayName + " is not a valid option");
        }
    }

    public static void validateFieldLength(String value, String name, String displayName, Errors errors, int min,
            int max) {
        validateFieldLengthMax(value, name, displayName, errors, max);
        validateFieldLengthMin(value, name, displayName, errors, min);
    }

    public static void validateField(String value, String name, String displayName, Errors errors, boolean required,
            int maxLength) {
        if (required) {
            validateFieldRequired(value, name, displayName, errors);
        }
        validateFieldLengthMax(value, name, displayName, errors, maxLength);
    }

    public static void validateField(String value, String name, String displayName, Errors errors, boolean required,
            int maxLength, String regex) {
        validateField(value, name, displayName, errors, required, maxLength);
        validateFieldMatchRegex(value, name, displayName, errors, regex);
    }

    public static void validateFieldAndCharset(String value, String name, String displayName, Errors errors,
            boolean required, int maxLength, String charSetRegex) {
        validateField(value, name, displayName, errors, required, maxLength);
        validateFieldCharSet(value, name, displayName, errors, charSetRegex);
    }

    public static void validateIdField(String value, String name, String displayName, Errors errors, boolean required) {
        validateField(value, name, displayName, errors, required, 10, "[0-9]*");
    }

    public static void validateYNField(String value, String name, String displayName, Errors errors) {
        validateOptionField(value, name, displayName, errors,
                new String[] { IActionConstants.YES, IActionConstants.NO });
    }

    public static void validateDateField(String value, String name, String displayName, Errors errors,
            DateRelation relative, boolean required) {
        if (required) {
            validateFieldRequired(value, name, displayName, errors);
            if (errors.hasErrors()) {
                return;
            }
            validateDateField(value, name, displayName, errors, relative);
        } else {
            if (org.apache.commons.validator.GenericValidator.isBlankOrNull(value)) {
                return;
            }
            validateDateField(value, name, displayName, errors, relative);
        }
    }

    public static void validateTimeField(String value, String name, String displayName, Errors errors,
            boolean required) {
        if (required) {
            validateFieldRequired(value, name, displayName, errors);
        } else {
            if (org.apache.commons.validator.GenericValidator.isBlankOrNull(value)) {
                return;
            }
            validateTimeField(value, name, displayName, errors);
        }
    }

    public static void validateGenderField(String value, String name, String displayName, Errors errors,
            boolean required) {
        if (required) {
            validateOptionField(value, name, displayName, errors, new Object[] { "M", "F" });
        } else {
            validateOptionField(value, name, displayName, errors, new Object[] { "M", "F", "", null });
        }
    }

    // methods for using name = displayName

    public static void validateFieldRequired(String value, String name, Errors errors) {
        validateFieldRequired(value, name, name, errors);
    }

    public static void validateFieldLengthMax(String value, String name, Errors errors, int max) {
        validateFieldLengthMax(value, name, name, errors, max);
    }

    public static void validateFieldLengthMin(String value, String name, Errors errors, int min) {
        validateFieldLengthMin(value, name, name, errors, min);
    }

    public static void validateFieldMatchRegex(String value, String name, Errors errors, String regex) {
        validateFieldMatchRegex(value, name, name, errors, regex);
    }

    public static void validateFieldCharSet(String value, String name, Errors errors, String charSet) {
        validateFieldCharSet(value, name, name, errors, charSet);
    }

    public static void validateFieldLength(String value, String name, Errors errors, int min, int max) {
        validateFieldLength(value, name, name, errors, min, max);
    }

    public static void validateField(String value, String name, Errors errors, boolean required, int maxLength) {
        validateField(value, name, name, errors, required, maxLength);
    }

    public static void validateField(String value, String name, Errors errors, boolean required, int maxLength,
            String regex) {
        validateField(value, name, name, errors, required, maxLength, regex);
    }

    public static void validateFieldAndCharset(String value, String name, Errors errors, boolean required,
            int maxLength, String charSetRegex) {
        validateFieldAndCharset(value, name, name, errors, required, maxLength, charSetRegex);
    }

    public static void validateIdField(String value, String name, Errors errors, boolean required) {
        validateIdField(value, name, name, errors, required);
    }

    public static void validateYNField(String value, String name, Errors errors) {
        validateYNField(value, name, name, errors);
    }

    public static void validateDateField(String value, String name, Errors errors, DateRelation relative) {
        validateDateField(value, name, name, errors, relative);
    }

    public static void validateDateField(String value, String name, Errors errors, DateRelation relative,
            boolean required) {
        validateDateField(value, name, name, errors, relative, required);
    }

    public static void validateTimeField(String value, String name, Errors errors, boolean required) {
        validateTimeField(value, name, name, errors, required);
    }

    public static void validateGenderField(String value, String name, Errors errors) {
        validateGenderField(value, name, name, errors, true);
    }

    public static void validateGenderField(String value, String name, Errors errors, boolean required) {
        validateGenderField(value, name, name, errors, required);
    }

    public static void validateOptionFieldIgnoreCase(String value, String name, Errors errors,
            String[] possibleValues) {
        validateOptionFieldIgnoreCase(value, name, name, errors, possibleValues);
    }

    /*
     * STRING[] METHODS
     */

    public static void validateFieldRequired(String[] value, String name, String displayName, Errors errors) {
        if (value == null || value.length == 0) {
            errors.rejectValue(name, "error.field.required", new Object[] { displayName },
                    DEFAULT_PREFIX + displayName + " is required");
        }
    }

    // methods for using name = displayName

    public static void validateFieldRequired(String[] value, String name, Errors errors) {
        validateFieldRequired(value, name, name, errors);
    }

    /*
     * INTEGER METHODS
     */

    public static void validateFieldMax(Integer value, String name, String displayName, Errors errors, int max) {
        if (value > max) {
            errors.rejectValue(name, "error.field.max", new String[] { displayName }, "Field too large an integer");
        }
    }

    public static void validateFieldMin(Integer value, String name, String displayName, Errors errors, int min) {
        if (value < min) {
            errors.rejectValue(name, "error.field.min", new String[] { displayName }, "Field too small an integer");
        }
    }

    public static void validateFieldMinMax(Integer value, String name, String displayName, Errors errors, int min,
            int max) {
        validateFieldMin(value, name, displayName, errors, min);
        validateFieldMax(value, name, displayName, errors, max);
    }

    // methods for using name = displayName

    public static void validateFieldMax(Integer value, String name, Errors errors, int max) {
        validateFieldMax(value, name, name, errors, max);
    }

    public static void validateFieldMin(Integer value, String name, Errors errors, int min) {
        validateFieldMin(value, name, name, errors, min);
    }

    public static void validateFieldMinMax(Integer value, String name, Errors errors, int min, int max) {
        validateFieldMinMax(value, name, name, errors, min, max);
    }

    /*
     * FLOAT METHODS
     */

    public static void validateFieldMax(Float value, String name, String displayName, Errors errors, float max) {
        if (value > max) {
            errors.rejectValue(name, "error.field.max", new String[] { displayName }, "Field too large a number");
        }
    }

    public static void validateFieldMin(Float value, String name, String displayName, Errors errors, float min) {
        if (value < min) {
            errors.rejectValue(name, "error.field.min", new String[] { displayName }, "Field too small a number");
        }
    }

    public static void validateFieldMinMax(Float value, String name, String displayName, Errors errors, float min,
            float max) {
        validateFieldMin(value, name, displayName, errors, min);
        validateFieldMax(value, name, displayName, errors, max);
    }

    // methods for using name = displayName

    public static void validateFieldMax(Float value, String name, Errors errors, float max) {
        validateFieldMax(value, name, name, errors, max);
    }

    public static void validateFieldMin(Float value, String name, Errors errors, float min) {
        validateFieldMin(value, name, name, errors, min);
    }

    public static void validateFieldMinMax(Float value, String name, Errors errors, float min, float max) {
        validateFieldMinMax(value, name, name, errors, min, max);
    }

    /*
     * BOOLEAN METHODS
     */

    public static void validateTFField(Boolean value, String name, String displayName, Errors errors,
            boolean required) {
        Boolean[] options;
        if (required) {
            options = new Boolean[] { true, false };
        } else {
            options = new Boolean[] { true, false, null };
        }
        validateOptionField(value, name, displayName, errors, options);
    }

    // methods for using name = displayName

    public static void validateTFField(Boolean value, String name, Errors errors, boolean required) {
        validateTFField(value, name, name, errors, required);
    }

    /*
     * OBJECT METHODS
     */

    public static void validateOptionField(Object value, String name, String displayName, Errors errors,
            Object[] possibleValues) {
        boolean match = false;
        for (Object possibleValue : possibleValues) {
            // null safety
            if (possibleValue == null) {
                if (value == null) {
                    match = true;
                }
            } else {
                if (possibleValue.equals(value)) {
                    match = true;
                }
            }

            if (match) {
                break;
            }
        }
        if (!match) {
            errors.rejectValue(name, "error.field.option.invalid", new Object[] { displayName },
                    DEFAULT_PREFIX + displayName + " is not a valid option");
        }
    }

    // methods for using name = displayName

    public static void validateOptionField(Object value, String name, Errors errors, Object[] possibleValues) {
        validateOptionField(value, name, name, errors, possibleValues);
    }
}

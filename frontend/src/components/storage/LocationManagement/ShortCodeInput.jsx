import React, { useState, useEffect } from "react";
import { TextInput, InlineNotification } from "@carbon/react";
import { useIntl } from "react-intl";
import PropTypes from "prop-types";
import { postToOpenElisServer } from "../../utils/Utils";

/**
 * ShortCodeInput - Input field for storage location short codes
 *
 * Features:
 * - Max 10 characters enforced
 * - Auto-uppercase conversion
 * - Format validation (alphanumeric, hyphen, underscore only)
 * - Must start with letter or number
 * - Uniqueness validation via backend API
 * - Inline error messages
 *
 * Props:
 * - value: string - Current short code value
 * - onChange: function - Callback when value changes (receives normalized code)
 * - locationType: string - Location type ('device', 'shelf', 'rack')
 * - locationId: string - Location ID for uniqueness validation
 * - disabled: boolean - Disable input
 */
const ShortCodeInput = ({
  value = "",
  onChange,
  locationType,
  locationId,
  disabled = false,
}) => {
  const intl = useIntl();
  const [inputValue, setInputValue] = useState(value || "");
  const [errorMessage, setErrorMessage] = useState("");
  const [isValidating, setIsValidating] = useState(false);

  // Sync with external value prop
  useEffect(() => {
    setInputValue(value || "");
  }, [value]);

  /**
   * Normalize input: convert to uppercase, trim whitespace
   */
  const normalizeInput = (input) => {
    return input.trim().toUpperCase();
  };

  /**
   * Validate format locally (client-side)
   * Returns { isValid: boolean, error: string }
   */
  const validateFormat = (code) => {
    if (!code || code.length === 0) {
      return { isValid: true, error: "" }; // Empty is valid (clearing short code)
    }

    // Max 10 characters
    if (code.length > 10) {
      return {
        isValid: false,
        error: intl.formatMessage(
          { id: "label.shortCode.error.maxLength" },
          { defaultMessage: "Short code must be 10 characters or less" },
        ),
      };
    }

    // Must start with letter or number
    if (!/^[A-Z0-9]/.test(code)) {
      return {
        isValid: false,
        error: intl.formatMessage(
          { id: "label.shortCode.error.startChar" },
          { defaultMessage: "Short code must start with a letter or number" },
        ),
      };
    }

    // Alphanumeric, hyphen, underscore only
    if (!/^[A-Z0-9_-]+$/.test(code)) {
      return {
        isValid: false,
        error: intl.formatMessage(
          { id: "label.shortCode.error.invalidChars" },
          {
            defaultMessage:
              "Short code can only contain letters, numbers, hyphens, and underscores",
          },
        ),
      };
    }

    return { isValid: true, error: "" };
  };

  /**
   * Validate uniqueness via backend API
   */
  const validateUniqueness = (code, callback) => {
    if (!code || code.length === 0) {
      callback({ isValid: true, error: "" });
      return;
    }

    if (!locationType || !locationId) {
      // Can't validate uniqueness without location context
      callback({ isValid: true, error: "" });
      return;
    }

    setIsValidating(true);
    const url = `/rest/storage/${locationType}/${locationId}/short-code`;
    const formData = { shortCode: code };

    postToOpenElisServer(
      url,
      formData,
      (response) => {
        setIsValidating(false);
        // If response has error, it's a validation error
        if (response.error) {
          callback({ isValid: false, error: response.error });
        } else {
          // Success - short code is valid and unique
          callback({ isValid: true, error: "" });
        }
      },
      (error) => {
        setIsValidating(false);
        // Network error - don't block user, but show warning
        callback({
          isValid: true,
          error: intl.formatMessage(
            { id: "label.shortCode.error.network" },
            {
              defaultMessage:
                "Could not verify uniqueness. Please check manually.",
            },
          ),
        });
      },
    );
  };

  /**
   * Handle input change
   */
  const handleChange = (e) => {
    const rawValue = e.target.value;
    const normalized = normalizeInput(rawValue);

    // Enforce max length
    if (normalized.length > 10) {
      return; // Don't update if exceeds max length
    }

    setInputValue(normalized);
    setErrorMessage(""); // Clear previous errors

    // Validate format
    const formatValidation = validateFormat(normalized);
    if (!formatValidation.isValid) {
      setErrorMessage(formatValidation.error);
      if (onChange) {
        onChange(normalized, false);
      }
      return;
    }

    // Format is valid - update parent immediately
    if (onChange) {
      onChange(normalized, true);
    }

    // Validate uniqueness (debounced - only on blur or Enter)
    // For now, we'll validate on blur to avoid too many API calls
  };

  /**
   * Handle blur - validate uniqueness
   */
  const handleBlur = () => {
    if (inputValue && inputValue.length > 0) {
      validateUniqueness(inputValue, (result) => {
        if (!result.isValid) {
          setErrorMessage(result.error);
          if (onChange) {
            onChange(inputValue, false);
          }
        }
      });
    }
  };

  return (
    <div data-testid="short-code-input-container">
      <TextInput
        id="short-code-input"
        data-testid="short-code-input"
        labelText={intl.formatMessage({
          id: "label.shortCode",
          defaultMessage: "Short Code",
        })}
        value={inputValue}
        onChange={handleChange}
        onBlur={handleBlur}
        disabled={disabled || isValidating}
        invalid={errorMessage.length > 0}
        invalidText={errorMessage}
        maxLength={10}
        placeholder="FRZ01"
        helperText={intl.formatMessage({
          id: "label.shortCode.helper",
          defaultMessage: "Max 10 characters, alphanumeric, hyphen, underscore",
        })}
      />
      {errorMessage && (
        <InlineNotification
          kind="error"
          title={errorMessage}
          lowContrast
          hideCloseButton
          style={{ marginTop: "0.5rem" }}
          data-testid="short-code-error"
        />
      )}
    </div>
  );
};

ShortCodeInput.propTypes = {
  value: PropTypes.string,
  onChange: PropTypes.func.isRequired,
  locationType: PropTypes.oneOf(["device", "shelf", "rack"]),
  locationId: PropTypes.string,
  disabled: PropTypes.bool,
};

ShortCodeInput.defaultProps = {
  value: "",
  locationType: null,
  locationId: null,
  disabled: false,
};

export default ShortCodeInput;

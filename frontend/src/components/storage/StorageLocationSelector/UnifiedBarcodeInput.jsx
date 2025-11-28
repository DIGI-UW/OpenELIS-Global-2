import React, { useState, useEffect, useRef, useCallback } from "react";
import { useIntl } from "react-intl";
import { TextInput, InlineNotification } from "@carbon/react";
import PropTypes from "prop-types";
import BarcodeVisualFeedback from "./BarcodeVisualFeedback";
import useBarcodeDebounce from "./BarcodeDebounceHook";
import { postToOpenElisServerJsonResponse } from "../../utils/Utils";

/**
 * UnifiedBarcodeInput - Barcode-only input field for location barcode scanning
 *
 * Features:
 * - Accepts keyboard input (manual typing)
 * - Accepts rapid character input (barcode scan)
 * - Always validates input as barcode (backend handles format validation)
 * - Enter key triggers validation
 * - Field blur triggers validation
 * - Visual feedback states (ready, success, error)
 * - Auto-clear after successful validation
 *
 * Props:
 * - onScan: Callback when barcode scan detected
 * - onValidationResult: Callback with validation result
 * - onSampleScan: Callback when sample barcode detected
 * - validationState: Current state (ready, success, error)
 * - errorMessage: Error message to display
 */
const UnifiedBarcodeInput = ({
  onScan,
  onValidationResult,
  onSampleScan,
  validationState = "ready",
  errorMessage = "",
}) => {
  const intl = useIntl();
  const [inputValue, setInputValue] = useState("");
  const [lastInputTime, setLastInputTime] = useState(null);
  const [debounceWarning, setDebounceWarning] = useState(null);
  const autoClearTimeoutRef = useRef(null);
  const inputRef = useRef(null);

  /**
   * Debounce warning callback
   */
  const handleDebounceWarning = useCallback((message) => {
    setDebounceWarning(message);
    // Clear warning after 3 seconds
    setTimeout(() => {
      setDebounceWarning(null);
    }, 3000);
  }, []);

  /**
   * Debounce hook for barcode scans
   */
  const { handleScan: debouncedHandleScan } = useBarcodeDebounce(
    (barcode) => {
      console.log(
        "[UnifiedBarcodeInput] debouncedHandleScan callback executed",
        { barcode },
      );
      // Process debounced barcode scan
      if (onScan) {
        console.log("[UnifiedBarcodeInput] Calling onScan callback", {
          barcode,
        });
        onScan(barcode);
      } else {
        console.log("[UnifiedBarcodeInput] onScan callback not provided");
      }
      validateBarcode(barcode);
    },
    500, // 500ms cooldown
    handleDebounceWarning, // Warning callback
  );

  /**
   * Detect rapid character input (barcode scanner)
   * Scanners typically input characters within 50ms
   */
  const isRapidInput = () => {
    if (!lastInputTime) return false;
    const timeSinceLastInput = Date.now() - lastInputTime;
    return timeSinceLastInput < 50;
  };

  /**
   * Call barcode validation API
   * Note: Backend expects POST with JSON body, not GET with query parameter
   */
  const validateBarcode = (barcode) => {
    const url = `/rest/storage/barcode/validate`;
    const payload = JSON.stringify({ barcode: barcode });
    console.log("[UnifiedBarcodeInput] validateBarcode called", {
      barcode,
      url,
      payload,
    });

    postToOpenElisServerJsonResponse(url, payload, (response) => {
      console.log("[UnifiedBarcodeInput] validateBarcode response received", {
        response,
        hasError: !!response.error,
        status: response.status,
      });

      // Handle network/HTTP errors
      if (response.error || (response.status && response.status >= 400)) {
        console.log("[UnifiedBarcodeInput] validateBarcode error response", {
          response,
        });
        if (onValidationResult) {
          onValidationResult({
            success: false,
            error: response.error || {
              message: response.message || "Validation failed",
            },
          });
        }
        return;
      }

      // Check barcode type from response
      const barcodeType = response.barcodeType || "unknown";
      console.log("[UnifiedBarcodeInput] Barcode type detected", {
        barcodeType,
        hasOnValidationResult: !!onValidationResult,
        hasOnSampleScan: !!onSampleScan,
      });

      if (barcodeType === "sample") {
        // Sample barcode detected - call onSampleScan callback
        if (onSampleScan) {
          onSampleScan({
            barcode: barcode,
            type: "sample",
            data: response,
          });
        }
      } else if (barcodeType === "location") {
        // Location barcode - proceed with existing validation result logic
        // Check if validation succeeded or failed
        console.log(
          "[UnifiedBarcodeInput] Location barcode type, calling onValidationResult",
          { valid: response.valid },
        );
        if (onValidationResult) {
          const validationResult = {
            success: response.valid || false,
            data: response,
            // Include progressive validation fields for auto-open behavior
            firstMissingLevel: response.firstMissingLevel || null,
            validComponents: response.validComponents || {},
            hasAdditionalInvalidLevels:
              response.hasAdditionalInvalidLevels || false,
            // Include errorMessage in error object for LocationManagementModal to extract
            error: response.valid
              ? null
              : {
                  errorMessage: response.errorMessage,
                  message: response.errorMessage,
                },
          };
          console.log(
            "[UnifiedBarcodeInput] Calling onValidationResult with",
            validationResult,
          );
          onValidationResult(validationResult);
        } else {
          console.log(
            "[UnifiedBarcodeInput] onValidationResult callback not provided",
          );
        }
      } else {
        // Unknown type - still call validation result for error handling
        console.log(
          "[UnifiedBarcodeInput] Unknown barcode type, calling onValidationResult anyway",
          { valid: response.valid },
        );
        if (onValidationResult) {
          const validationResult = {
            success: response.valid || false,
            data: response,
            // Include progressive validation fields for auto-open behavior
            firstMissingLevel: response.firstMissingLevel || null,
            validComponents: response.validComponents || {},
            hasAdditionalInvalidLevels:
              response.hasAdditionalInvalidLevels || false,
            // Include errorMessage in error object
            error: response.valid
              ? null
              : {
                  errorMessage: response.errorMessage,
                  message: response.errorMessage,
                },
          };
          console.log(
            "[UnifiedBarcodeInput] Calling onValidationResult with (unknown type)",
            validationResult,
          );
          onValidationResult(validationResult);
        } else {
          console.log(
            "[UnifiedBarcodeInput] onValidationResult callback not provided (unknown type)",
          );
        }
      }
    });
    // Note: postToOpenElisServerJsonResponse handles errors in the response object
    // Errors are passed as part of the response with status/error fields
  };

  /**
   * Handle input change
   */
  const handleChange = (event) => {
    const value = event.target.value;
    console.log("[UnifiedBarcodeInput] handleChange called", {
      value,
      inputValue,
    });
    setInputValue(value);
    setLastInputTime(Date.now());
  };

  /**
   * Handle Enter key press
   * Note: Use event.target.value instead of inputValue state to avoid React state timing issues
   * Support both event.key and event.keyCode for Carbon TextInput compatibility
   */
  const handleKeyDown = (event) => {
    const currentValue = event.target.value || "";
    const trimmedValue = currentValue.trim();
    const isEnterKey =
      event.key === "Enter" || event.keyCode === 13 || event.code === "Enter";

    console.log("[UnifiedBarcodeInput] handleKeyDown called", {
      key: event.key,
      keyCode: event.keyCode,
      code: event.code,
      isEnterKey,
      inputValue,
      currentValue,
      trimmedValue,
    });

    if (isEnterKey && trimmedValue !== "") {
      console.log(
        "[UnifiedBarcodeInput] Enter key detected with non-empty input, calling processInput",
      );
      event.preventDefault();
      event.stopPropagation();
      processInput(trimmedValue);
    } else if (isEnterKey) {
      console.log("[UnifiedBarcodeInput] Enter key ignored - empty input");
    } else {
      console.log("[UnifiedBarcodeInput] Enter key ignored", {
        reason: "not Enter key",
        key: event.key,
        keyCode: event.keyCode,
        code: event.code,
      });
    }
  };

  /**
   * Handle field blur
   * Note: Use inputValue state here since blur happens after state has updated
   */
  const handleBlur = (event) => {
    const currentValue = event?.target?.value || inputValue || "";
    const trimmedValue = currentValue.trim();
    console.log("[UnifiedBarcodeInput] handleBlur called", {
      inputValue,
      currentValue,
      trimmedValue,
    });
    if (trimmedValue !== "") {
      console.log(
        "[UnifiedBarcodeInput] Blur with non-empty input, calling processInput",
      );
      processInput(trimmedValue);
    }
  };

  /**
   * Process input (always validate as barcode - backend handles format validation)
   */
  const processInput = (value) => {
    console.log("[UnifiedBarcodeInput] processInput called", { value });
    // Always validate as barcode - backend will return appropriate error for invalid formats
    console.log(
      "[UnifiedBarcodeInput] Calling debouncedHandleScan for validation",
    );
    debouncedHandleScan(value);
  };

  /**
   * Auto-clear input after successful validation
   */
  useEffect(() => {
    if (validationState === "success") {
      // Clear after 2 seconds
      autoClearTimeoutRef.current = setTimeout(() => {
        setInputValue("");
      }, 2000);
    }

    return () => {
      if (autoClearTimeoutRef.current) {
        clearTimeout(autoClearTimeoutRef.current);
      }
    };
  }, [validationState]);

  /**
   * Get placeholder text based on validation state
   */
  const getPlaceholderText = () => {
    switch (validationState) {
      case "success":
        return intl.formatMessage({
          id: "barcode.success",
          defaultMessage: "Location found",
        });
      case "error":
        return intl.formatMessage({
          id: "barcode.error",
          defaultMessage: "Invalid barcode",
        });
      default:
        return intl.formatMessage({
          id: "barcode.scan",
          defaultMessage: "Scan barcode",
        });
    }
  };

  /**
   * Get label text
   */
  const getLabelText = () => {
    return intl.formatMessage({
      id: "barcode.scan",
      defaultMessage: "Scan barcode",
    });
  };

  return (
    <div className="unified-barcode-input" data-testid="unified-barcode-input">
      {debounceWarning && (
        <InlineNotification
          kind="warning"
          title={intl.formatMessage({
            id: "barcode.debounce.warning",
            defaultMessage: "Please wait before scanning another barcode",
          })}
          subtitle={debounceWarning}
          lowContrast
          hideCloseButton
          style={{ marginBottom: "1rem" }}
        />
      )}
      <div style={{ display: "flex", alignItems: "flex-end", gap: "0.5rem" }}>
        <div style={{ flex: 1 }}>
          <TextInput
            ref={inputRef}
            id="barcode-input"
            labelText={getLabelText()}
            placeholder={getPlaceholderText()}
            value={inputValue}
            onChange={handleChange}
            onKeyDown={handleKeyDown}
            onBlur={handleBlur}
            data-barcode-input="true"
          />
        </div>
        <div style={{ marginBottom: "1rem" }}>
          <BarcodeVisualFeedback
            state={validationState}
            errorMessage={errorMessage}
          />
        </div>
      </div>
    </div>
  );
};

UnifiedBarcodeInput.propTypes = {
  onScan: PropTypes.func,
  onValidationResult: PropTypes.func,
  onSampleScan: PropTypes.func,
  validationState: PropTypes.oneOf(["ready", "success", "error"]),
  errorMessage: PropTypes.string,
};

UnifiedBarcodeInput.defaultProps = {
  onScan: () => {},
  onValidationResult: () => {},
  onSampleScan: () => {},
  validationState: "ready",
  errorMessage: "",
};

export default UnifiedBarcodeInput;

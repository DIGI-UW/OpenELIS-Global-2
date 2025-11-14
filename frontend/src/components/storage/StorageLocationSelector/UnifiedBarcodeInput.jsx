import React, { useState, useEffect, useRef, useCallback } from "react";
import { useIntl } from "react-intl";
import { TextInput, InlineNotification } from "@carbon/react";
import PropTypes from "prop-types";
import BarcodeVisualFeedback from "./BarcodeVisualFeedback";
import useBarcodeDebounce from "./BarcodeDebounceHook";
import { getFromOpenElisServer } from "../../utils/Utils";

/**
 * UnifiedBarcodeInput - Unified input field supporting both barcode scan and type-ahead search
 *
 * Features:
 * - Accepts keyboard input (manual typing)
 * - Accepts rapid character input (barcode scan)
 * - Format-based detection (hyphens = barcode, no hyphens = type-ahead)
 * - Enter key triggers validation
 * - Field blur triggers validation
 * - Visual feedback states (ready, success, error)
 * - Auto-clear after successful validation
 *
 * Props:
 * - onScan: Callback when barcode scan detected
 * - onTypeAhead: Callback when type-ahead search triggered
 * - onValidationResult: Callback with validation result
 * - onSampleScan: Callback when sample barcode detected (new prop)
 * - validationState: Current state (ready, success, error)
 * - errorMessage: Error message to display
 */
const UnifiedBarcodeInput = ({
  onScan,
  onTypeAhead,
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
      // Process debounced barcode scan
      if (onScan) {
        onScan(barcode);
      }
      validateBarcode(barcode);
    },
    500, // 500ms cooldown
    handleDebounceWarning, // Warning callback
  );

  /**
   * Detect if input is barcode or type-ahead search
   * Barcode: Contains hyphens (hierarchical format: ROOM-DEVICE-SHELF-RACK-POSITION)
   * Type-ahead: No hyphens (search text like "Freezer" or "Main Lab")
   */
  const isBarcodeFormat = (value) => {
    return value && value.includes("-");
  };

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
   */
  const validateBarcode = (barcode) => {
    const url = `/rest/storage/barcode/validate?barcode=${encodeURIComponent(barcode)}`;

    getFromOpenElisServer(
      url,
      (response) => {
        // Check barcode type from response
        const barcodeType = response.barcodeType || "unknown";

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
          if (onValidationResult) {
            onValidationResult({
              success: response.valid || false,
              data: response,
              // Include errorMessage in error object for LocationManagementModal to extract
              error: response.valid
                ? null
                : {
                    errorMessage: response.errorMessage,
                    message: response.errorMessage,
                  },
            });
          }
        } else {
          // Unknown type - still call validation result for error handling
          if (onValidationResult) {
            onValidationResult({
              success: response.valid || false,
              data: response,
              // Include errorMessage in error object
              error: response.valid
                ? null
                : {
                    errorMessage: response.errorMessage,
                    message: response.errorMessage,
                  },
            });
          }
        }
      },
      (error) => {
        // Error callback
        if (onValidationResult) {
          onValidationResult({
            success: false,
            error: error,
          });
        }
      },
    );
  };

  /**
   * Handle input change
   */
  const handleChange = (event) => {
    const value = event.target.value;
    setInputValue(value);
    setLastInputTime(Date.now());
  };

  /**
   * Handle Enter key press
   */
  const handleKeyDown = (event) => {
    if (event.key === "Enter" && inputValue.trim() !== "") {
      event.preventDefault();
      processInput(inputValue.trim());
    }
  };

  /**
   * Handle field blur
   */
  const handleBlur = () => {
    if (inputValue.trim() !== "") {
      processInput(inputValue.trim());
    }
  };

  /**
   * Process input (barcode scan or type-ahead search)
   */
  const processInput = (value) => {
    if (isBarcodeFormat(value)) {
      // Barcode detected - use debounced handler
      debouncedHandleScan(value);
    } else {
      // Type-ahead search detected
      if (onTypeAhead) {
        onTypeAhead(value);
      }
    }
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
          id: "barcode.scanOrType",
          defaultMessage: "Scan barcode or type location",
        });
    }
  };

  /**
   * Get label text
   */
  const getLabelText = () => {
    return intl.formatMessage({
      id: "barcode.scanOrType",
      defaultMessage: "Scan barcode or type location",
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
            invalid={validationState === "error"}
            invalidText={errorMessage}
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
  onTypeAhead: PropTypes.func,
  onValidationResult: PropTypes.func,
  onSampleScan: PropTypes.func,
  validationState: PropTypes.oneOf(["ready", "success", "error"]),
  errorMessage: PropTypes.string,
};

UnifiedBarcodeInput.defaultProps = {
  onScan: () => {},
  onTypeAhead: () => {},
  onValidationResult: () => {},
  onSampleScan: () => {},
  validationState: "ready",
  errorMessage: "",
};

export default UnifiedBarcodeInput;

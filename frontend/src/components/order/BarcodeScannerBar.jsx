import React, { useState, useEffect, useCallback, useRef } from "react";
import { Search, InlineNotification } from "@carbon/react";
import { useIntl } from "react-intl";
import { useOrderContext } from "./OrderContext";

/**
 * BarcodeScannerBar - Barcode/lab number search bar with inline feedback.
 *
 * Features:
 * - Accepts barcode scan or manual lab number entry
 * - Shows inline success (green) or error (red) feedback when lookup completes
 * - Auto-clears feedback after 3 seconds
 * - Loads order in read-only mode when found
 */

const FEEDBACK_DISPLAY_TIME = 3000; // 3 seconds

const BarcodeScannerBar = ({ onOrderLoaded, className = "" }) => {
  const intl = useIntl();
  const { loadOrder, isLoading } = useOrderContext();

  const [inputValue, setInputValue] = useState("");
  const [feedback, setFeedback] = useState(null); // { type: 'success'|'error', message: string }
  const mountedRef = useRef(true);

  // Clear feedback timer on unmount
  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  // Auto-clear feedback after display time
  useEffect(() => {
    if (!feedback) {
      return undefined;
    }

    const feedbackTimer = setTimeout(() => {
      setFeedback(null);
    }, FEEDBACK_DISPLAY_TIME);
    return () => clearTimeout(feedbackTimer);
  }, [feedback]);

  const handleSearch = useCallback(
    async (barcode) => {
      if (!barcode || barcode.trim() === "") {
        return;
      }

      const trimmedBarcode = barcode.trim();

      try {
        const result = await loadOrder(trimmedBarcode, true);
        if (!mountedRef.current) {
          return;
        }

        setFeedback({
          type: "success",
          message: intl.formatMessage({ id: "barcode.scan.success" }),
        });

        if (onOrderLoaded) {
          onOrderLoaded(result, true);
        }

        // Clear input after successful scan
        setInputValue("");
      } catch {
        if (!mountedRef.current) {
          return;
        }

        setFeedback({
          type: "error",
          message: intl.formatMessage({ id: "barcode.scan.error" }),
        });
      }
    },
    [loadOrder, onOrderLoaded, intl],
  );

  const handleKeyDown = (e) => {
    if (e.key === "Enter") {
      e.preventDefault();
      handleSearch(inputValue);
    }
  };

  const handleChange = (e) => {
    setInputValue(e.target.value);
  };

  return (
    <div className={`barcode-scanner-bar ${className}`}>
      <Search
        id="order-barcode-search"
        labelText={intl.formatMessage({ id: "barcode.scan" })}
        placeholder={intl.formatMessage({ id: "barcode.scan.placeholder" })}
        value={inputValue}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        disabled={isLoading}
        size="lg"
      />
      {feedback && (
        <InlineNotification
          className="barcode-feedback"
          kind={feedback.type === "success" ? "success" : "error"}
          title={feedback.message}
          lowContrast
          hideCloseButton
        />
      )}
    </div>
  );
};

export default BarcodeScannerBar;

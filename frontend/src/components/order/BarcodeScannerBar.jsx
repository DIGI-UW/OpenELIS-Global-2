import React, { useState, useEffect, useCallback, useRef } from "react";
import { Search, InlineNotification } from "@carbon/react";
import { useIntl, FormattedMessage } from "react-intl";
import { useOrderContext } from "./OrderContext";

/**
 * BarcodeScannerBar - Barcode/lab number search bar with inline feedback.
 *
 * Features:
 * - Accepts barcode scan or manual lab number entry
 * - Shows inline success (green) or error (red) feedback within 500ms
 * - Auto-clears feedback after 3 seconds
 * - Loads order in read-only mode when found
 */

const FEEDBACK_DISPLAY_TIME = 3000; // 3 seconds
const MIN_FEEDBACK_DELAY = 500; // 500ms minimum before showing feedback

const BarcodeScannerBar = ({ onOrderLoaded, className = "" }) => {
  const intl = useIntl();
  const { loadOrder, isLoading } = useOrderContext();

  const [inputValue, setInputValue] = useState("");
  const [feedback, setFeedback] = useState(null); // { type: 'success'|'error', message: string }
  const feedbackTimerRef = useRef(null);

  // Clear feedback timer on unmount
  useEffect(() => {
    return () => {
      if (feedbackTimerRef.current) {
        clearTimeout(feedbackTimerRef.current);
      }
    };
  }, []);

  // Auto-clear feedback after display time
  useEffect(() => {
    if (feedback) {
      feedbackTimerRef.current = setTimeout(() => {
        setFeedback(null);
      }, FEEDBACK_DISPLAY_TIME);

      return () => {
        if (feedbackTimerRef.current) {
          clearTimeout(feedbackTimerRef.current);
        }
      };
    }
  }, [feedback]);

  const handleSearch = useCallback(
    async (barcode) => {
      if (!barcode || barcode.trim() === "") {
        return;
      }

      const trimmedBarcode = barcode.trim();
      const startTime = Date.now();

      try {
        const result = await loadOrder(trimmedBarcode, true);

        // Ensure minimum delay before showing feedback (NAV-7: within 500ms)
        const elapsed = Date.now() - startTime;
        if (elapsed < MIN_FEEDBACK_DELAY) {
          await new Promise((resolve) =>
            setTimeout(resolve, MIN_FEEDBACK_DELAY - elapsed),
          );
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
      } catch (error) {
        // Ensure minimum delay before showing feedback
        const elapsed = Date.now() - startTime;
        if (elapsed < MIN_FEEDBACK_DELAY) {
          await new Promise((resolve) =>
            setTimeout(resolve, MIN_FEEDBACK_DELAY - elapsed),
          );
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

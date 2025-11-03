import React, { useState, useEffect, useRef } from "react";
import { TextInput } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

/**
 * Barcode scan mode for storage location selection
 * Detects rapid keyboard input from barcode scanner
 */
const BarcodeScanMode = ({ onLocationScanned }) => {
  const intl = useIntl();
  const [scannedCode, setScannedCode] = useState("");
  const bufferRef = useRef("");
  const timeoutRef = useRef(null);

  useEffect(() => {
    const handleKeyDown = (event) => {
      // Ignore if not focused on barcode input
      if (!event.target.dataset.barcodeInput) {
        return;
      }

      // Ignore modifier keys
      if (event.ctrlKey || event.altKey || event.metaKey) {
        return;
      }

      // Handle Enter key (scan complete)
      if (event.key === "Enter") {
        event.preventDefault();
        if (bufferRef.current.length >= 3) {
          setScannedCode(bufferRef.current);
          if (onLocationScanned) {
            onLocationScanned(bufferRef.current);
          }
        }
        bufferRef.current = "";
        return;
      }

      // Add character to buffer
      if (event.key.length === 1) {
        event.preventDefault();
        bufferRef.current += event.key;

        // Reset timeout
        if (timeoutRef.current) {
          clearTimeout(timeoutRef.current);
        }

        // Set new timeout (50ms - typical scanner speed)
        timeoutRef.current = setTimeout(() => {
          if (bufferRef.current.length >= 3) {
            setScannedCode(bufferRef.current);
            if (onLocationScanned) {
              onLocationScanned(bufferRef.current);
            }
          }
          bufferRef.current = "";
        }, 50);
      }
    };

    window.addEventListener("keydown", handleKeyDown);

    return () => {
      window.removeEventListener("keydown", handleKeyDown);
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
    };
  }, [onLocationScanned]);

  return (
    <div className="barcode-scan-container">
      <TextInput
        id="barcode-input"
        data-barcode-input="true"
        data-testid="barcode-input"
        labelText={intl.formatMessage({ id: "storage.location.label" })}
        placeholder={intl.formatMessage({
          id: "storage.barcode.scan.placeholder",
        })}
        value={scannedCode}
        onChange={(e) => setScannedCode(e.target.value)}
      />
    </div>
  );
};

export default BarcodeScanMode;

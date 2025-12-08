import { useState, useRef, useCallback } from "react";

/**
 * useBarcodeDebounce - Custom React hook for barcode scan debouncing
 *
 * Features:
 * - 500ms cooldown period between scans
 * - Duplicate detection (same barcode within cooldown is ignored)
 * - Different barcode warning (different barcode within cooldown shows warning)
 * - Cooldown timer resets on each successful scan
 * - Handles multiple rapid scans correctly
 *
 * @param {function} onScan - Callback function when barcode scan is processed
 * @param {number} cooldownMs - Cooldown period in milliseconds (default: 500ms)
 * @param {function} onWarning - Optional callback for warning messages
 * @returns {object} - { handleScan, isInCooldown, lastScannedBarcode }
 */
const useBarcodeDebounce = (onScan, cooldownMs = 500, onWarning = null) => {
  const [isInCooldown, setIsInCooldown] = useState(false);
  const [lastScannedBarcode, setLastScannedBarcode] = useState(null);
  const lastScanTimeRef = useRef(null);
  const cooldownTimerRef = useRef(null);

  /**
   * Handle barcode scan with debouncing logic
   */
  const handleScan = useCallback(
    (barcode) => {
      // Validate barcode
      if (!barcode || typeof barcode !== "string" || barcode.trim() === "") {
        return;
      }

      const trimmedBarcode = barcode.trim();
      const currentTime = Date.now();

      // Check if in cooldown period
      if (lastScanTimeRef.current) {
        const timeSinceLastScan = currentTime - lastScanTimeRef.current;

        if (timeSinceLastScan < cooldownMs) {
          // Within cooldown period
          if (trimmedBarcode === lastScannedBarcode) {
            // Duplicate barcode - ignore silently
            return;
          } else {
            // Different barcode - show warning
            if (onWarning) {
              const remainingCooldown = cooldownMs - timeSinceLastScan;
              onWarning(
                `Please wait ${Math.ceil(remainingCooldown / 1000)} second(s) before scanning another barcode.`,
              );
            }
            return;
          }
        }
      }

      // Process the scan
      setLastScannedBarcode(trimmedBarcode);
      lastScanTimeRef.current = currentTime;
      setIsInCooldown(true);

      // Call the callback
      if (onScan) {
        onScan(trimmedBarcode);
      }

      // Clear existing cooldown timer
      if (cooldownTimerRef.current) {
        clearTimeout(cooldownTimerRef.current);
      }

      // Set new cooldown timer
      cooldownTimerRef.current = setTimeout(() => {
        setIsInCooldown(false);
        cooldownTimerRef.current = null;
      }, cooldownMs);
    },
    [onScan, onWarning, cooldownMs, lastScannedBarcode],
  );

  /**
   * Get current cooldown state
   */
  const getCooldownState = useCallback(() => {
    if (!lastScanTimeRef.current) {
      return {
        inCooldown: false,
        remaining: 0,
      };
    }

    const timeSinceLastScan = Date.now() - lastScanTimeRef.current;
    const remaining = Math.max(0, cooldownMs - timeSinceLastScan);

    return {
      inCooldown: remaining > 0,
      remaining,
    };
  }, [cooldownMs]);

  /**
   * Reset cooldown state
   */
  const resetCooldown = useCallback(() => {
    setIsInCooldown(false);
    setLastScannedBarcode(null);
    lastScanTimeRef.current = null;

    if (cooldownTimerRef.current) {
      clearTimeout(cooldownTimerRef.current);
      cooldownTimerRef.current = null;
    }
  }, []);

  return {
    handleScan,
    isInCooldown,
    lastScannedBarcode,
    getCooldownState,
    resetCooldown,
  };
};

export default useBarcodeDebounce;

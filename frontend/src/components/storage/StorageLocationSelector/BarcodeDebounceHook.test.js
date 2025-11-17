import React from "react";
import { render, act } from "@testing-library/react";
import useBarcodeDebounce from "./BarcodeDebounceHook";

// Helper component to test the hook
const HookWrapper = ({ callback, cooldown, warning, children }) => {
  const hookResult = useBarcodeDebounce(callback, cooldown, warning);
  return children(hookResult);
};

// Helper function to render hook
const renderHook = (callback, cooldown = 500, warning = null) => {
  let hookResult = {};
  render(
    <HookWrapper callback={callback} cooldown={cooldown} warning={warning}>
      {(result) => {
        hookResult = result;
        return null;
      }}
    </HookWrapper>,
  );
  return { result: { current: hookResult } };
};

describe("useBarcodeDebounce Hook", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  describe("testDuplicateBarcodeWithin500msIgnored", () => {
    it("should ignore duplicate barcode within 500ms cooldown", () => {
      const mockCallback = jest.fn();
      const { result } = renderHook(mockCallback);

      const barcode = "MAIN-FRZ01";

      // First scan
      act(() => {
        result.current.handleScan(barcode);
      });

      expect(mockCallback).toHaveBeenCalledTimes(1);
      expect(mockCallback).toHaveBeenCalledWith(barcode);

      mockCallback.mockClear();

      // Second scan (same barcode within 500ms) - should be ignored
      act(() => {
        jest.advanceTimersByTime(300); // 300ms later (< 500ms cooldown)
        result.current.handleScan(barcode);
      });

      expect(mockCallback).not.toHaveBeenCalled();
    });

    it("should process duplicate barcode after 500ms cooldown", () => {
      const mockCallback = jest.fn();
      const { result } = renderHook(mockCallback);

      const barcode = "MAIN-FRZ01";

      // First scan
      act(() => {
        result.current.handleScan(barcode);
      });

      expect(mockCallback).toHaveBeenCalledTimes(1);

      mockCallback.mockClear();

      // Second scan (same barcode after 500ms) - should be processed
      act(() => {
        jest.advanceTimersByTime(600); // 600ms later (> 500ms cooldown)
        result.current.handleScan(barcode);
      });

      expect(mockCallback).toHaveBeenCalledTimes(1);
      expect(mockCallback).toHaveBeenCalledWith(barcode);
    });
  });

  describe("testDifferentBarcodeWithin500msShowsWarning", () => {
    it("should show warning when different barcode scanned within cooldown", () => {
      const mockCallback = jest.fn();
      const mockWarningCallback = jest.fn();
      const { result } = renderHook(mockCallback, 500, mockWarningCallback);

      const barcode1 = "MAIN-FRZ01";
      const barcode2 = "MAIN-FRZ02";

      // First scan
      act(() => {
        result.current.handleScan(barcode1);
      });

      expect(mockCallback).toHaveBeenCalledTimes(1);
      expect(mockCallback).toHaveBeenCalledWith(barcode1);

      mockCallback.mockClear();

      // Second scan (different barcode within 500ms) - should trigger warning
      act(() => {
        jest.advanceTimersByTime(300); // 300ms later (< 500ms cooldown)
        result.current.handleScan(barcode2);
      });

      expect(mockWarningCallback).toHaveBeenCalledTimes(1);
      expect(mockWarningCallback).toHaveBeenCalledWith(
        expect.stringContaining("Please wait"),
      );
      expect(mockCallback).not.toHaveBeenCalled();
    });

    it("should process different barcode after cooldown without warning", () => {
      const mockCallback = jest.fn();
      const mockWarningCallback = jest.fn();
      const { result } = renderHook(mockCallback, 500, mockWarningCallback);

      const barcode1 = "MAIN-FRZ01";
      const barcode2 = "MAIN-FRZ02";

      // First scan
      act(() => {
        result.current.handleScan(barcode1);
      });

      expect(mockCallback).toHaveBeenCalledTimes(1);

      mockCallback.mockClear();

      // Second scan (different barcode after 500ms) - should process without warning
      act(() => {
        jest.advanceTimersByTime(600); // 600ms later (> 500ms cooldown)
        result.current.handleScan(barcode2);
      });

      expect(mockWarningCallback).not.toHaveBeenCalled();
      expect(mockCallback).toHaveBeenCalledTimes(1);
      expect(mockCallback).toHaveBeenCalledWith(barcode2);
    });
  });

  describe("testBarcodeAfter500msProcessed", () => {
    it("should process barcode after 500ms cooldown expires", () => {
      const mockCallback = jest.fn();
      const { result } = renderHook(mockCallback);

      const barcode1 = "MAIN-FRZ01";
      const barcode2 = "MAIN-FRZ02";

      // First scan
      act(() => {
        result.current.handleScan(barcode1);
      });

      expect(mockCallback).toHaveBeenCalledWith(barcode1);

      mockCallback.mockClear();

      // Wait for cooldown to expire
      act(() => {
        jest.advanceTimersByTime(500);
      });

      // Second scan (should be processed)
      act(() => {
        result.current.handleScan(barcode2);
      });

      expect(mockCallback).toHaveBeenCalledWith(barcode2);
    });

    it("should process barcodes in sequence with proper cooldown", () => {
      const mockCallback = jest.fn();
      const { result } = renderHook(mockCallback);

      const barcodes = ["MAIN-FRZ01", "MAIN-FRZ02", "MAIN-FRZ03"];

      barcodes.forEach((barcode, index) => {
        act(() => {
          if (index > 0) {
            jest.advanceTimersByTime(500); // Wait for cooldown
          }
          result.current.handleScan(barcode);
        });

        expect(mockCallback).toHaveBeenCalledWith(barcode);
        mockCallback.mockClear();
      });

      expect(mockCallback).toHaveBeenCalledTimes(0); // All cleared after each assertion
    });
  });

  describe("testCooldownTimerResets", () => {
    it("should reset cooldown timer on each scan", () => {
      const mockCallback = jest.fn();
      const { result } = renderHook(mockCallback);

      const barcode1 = "MAIN-FRZ01";
      const barcode2 = "MAIN-FRZ02";

      // First scan
      act(() => {
        result.current.handleScan(barcode1);
      });

      expect(mockCallback).toHaveBeenCalledTimes(1);

      mockCallback.mockClear();

      // Advance time partially
      act(() => {
        jest.advanceTimersByTime(400); // 400ms (< 500ms)
      });

      // Try another scan - should still be in cooldown
      act(() => {
        result.current.handleScan(barcode1);
      });

      expect(mockCallback).not.toHaveBeenCalled();

      // Complete the cooldown
      act(() => {
        jest.advanceTimersByTime(100); // Total 500ms
      });

      // Now scan should work
      act(() => {
        result.current.handleScan(barcode2);
      });

      expect(mockCallback).toHaveBeenCalledTimes(1);
      expect(mockCallback).toHaveBeenCalledWith(barcode2);
    });

    it("should maintain separate cooldown for each unique barcode", () => {
      const mockCallback = jest.fn();
      const { result } = renderHook(mockCallback);

      const barcode1 = "MAIN-FRZ01";

      // First scan
      act(() => {
        result.current.handleScan(barcode1);
      });

      expect(mockCallback).toHaveBeenCalledTimes(1);

      mockCallback.mockClear();

      // Advance time
      act(() => {
        jest.advanceTimersByTime(300);
      });

      // Same barcode - should be ignored
      act(() => {
        result.current.handleScan(barcode1);
      });

      expect(mockCallback).not.toHaveBeenCalled();

      // Complete cooldown
      act(() => {
        jest.advanceTimersByTime(200); // Total 500ms
      });

      // Same barcode after cooldown - should work
      act(() => {
        result.current.handleScan(barcode1);
      });

      expect(mockCallback).toHaveBeenCalledTimes(1);
    });
  });

  describe("testMultipleRapidScansHandled", () => {
    it("should handle multiple rapid scans correctly", () => {
      const mockCallback = jest.fn();
      const mockWarningCallback = jest.fn();
      const { result } = renderHook(mockCallback, 500, mockWarningCallback);

      const barcodes = ["MAIN-FRZ01", "MAIN-FRZ02", "MAIN-FRZ03", "MAIN-FRZ04"];

      // Rapid scans (all within 500ms)
      barcodes.forEach((barcode, index) => {
        act(() => {
          jest.advanceTimersByTime(100); // 100ms between scans
          result.current.handleScan(barcode);
        });
      });

      // Only first scan should be processed
      expect(mockCallback).toHaveBeenCalledTimes(1);
      expect(mockCallback).toHaveBeenCalledWith(barcodes[0]);

      // Others should trigger warnings
      expect(mockWarningCallback).toHaveBeenCalled();
    });

    it("should process scans in batches with cooldown periods", () => {
      const mockCallback = jest.fn();
      const { result } = renderHook(mockCallback);

      const batch1 = ["MAIN-FRZ01", "MAIN-FRZ02"];
      const batch2 = ["MAIN-FRZ03", "MAIN-FRZ04"];

      // First batch (rapid scans)
      batch1.forEach((barcode, index) => {
        act(() => {
          if (index > 0) {
            jest.advanceTimersByTime(100); // Rapid
          }
          result.current.handleScan(barcode);
        });
      });

      expect(mockCallback).toHaveBeenCalledTimes(1); // Only first processed

      mockCallback.mockClear();

      // Wait for cooldown
      act(() => {
        jest.advanceTimersByTime(500);
      });

      // Second batch (rapid scans)
      batch2.forEach((barcode, index) => {
        act(() => {
          if (index > 0) {
            jest.advanceTimersByTime(100); // Rapid
          }
          result.current.handleScan(barcode);
        });
      });

      expect(mockCallback).toHaveBeenCalledTimes(1); // Only first of second batch
      expect(mockCallback).toHaveBeenCalledWith(batch2[0]);
    });

    it("should clear state after extended idle period", () => {
      const mockCallback = jest.fn();
      const { result } = renderHook(mockCallback);

      const barcode = "MAIN-FRZ01";

      // Scan
      act(() => {
        result.current.handleScan(barcode);
      });

      expect(mockCallback).toHaveBeenCalledTimes(1);

      mockCallback.mockClear();

      // Wait extended period (> cooldown)
      act(() => {
        jest.advanceTimersByTime(1000);
      });

      // Same barcode should be processed again (state cleared)
      act(() => {
        result.current.handleScan(barcode);
      });

      expect(mockCallback).toHaveBeenCalledTimes(1);
    });
  });

  describe("edge cases", () => {
    it("should handle empty barcode", () => {
      const mockCallback = jest.fn();
      const { result } = renderHook(mockCallback);

      act(() => {
        result.current.handleScan("");
      });

      expect(mockCallback).not.toHaveBeenCalled();
    });

    it("should handle null barcode", () => {
      const mockCallback = jest.fn();
      const { result } = renderHook(mockCallback);

      act(() => {
        result.current.handleScan(null);
      });

      expect(mockCallback).not.toHaveBeenCalled();
    });

    it("should handle undefined barcode", () => {
      const mockCallback = jest.fn();
      const { result } = renderHook(mockCallback);

      act(() => {
        result.current.handleScan(undefined);
      });

      expect(mockCallback).not.toHaveBeenCalled();
    });

    it("should handle whitespace-only barcode", () => {
      const mockCallback = jest.fn();
      const { result } = renderHook(mockCallback);

      act(() => {
        result.current.handleScan("   ");
      });

      expect(mockCallback).not.toHaveBeenCalled();
    });
  });
});

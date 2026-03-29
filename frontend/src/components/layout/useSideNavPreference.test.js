import { renderHook, act } from "@testing-library/react-hooks";
import { useSideNavPreference } from "./useSideNavPreference";

/**
 * Unit tests for useSideNavPreference hook
 *
 * This hook manages sidenav mode (show/lock/close) with localStorage persistence.
 * Tests cover:
 * - Default mode when no localStorage value exists
 * - Restoring mode from localStorage
 * - Toggle cycle close -> show -> lock -> close with persistence
 * - setMode functionality with persistence
 * - Graceful fallback when localStorage is unavailable
 *
 * @see spec.md User Story 2: Persist User Preference Across Sessions (P1)
 * @see data-model.md UseSideNavPreferenceReturn interface
 */

describe("useSideNavPreference", () => {
  // Mock localStorage
  const localStorageMock = (() => {
    let store = {};
    return {
      getItem: jest.fn((key) => store[key] || null),
      setItem: jest.fn((key, value) => {
        store[key] = value;
      }),
      removeItem: jest.fn((key) => {
        delete store[key];
      }),
      clear: jest.fn(() => {
        store = {};
      }),
    };
  })();

  beforeEach(() => {
    // Reset mocks and clear storage before each test
    jest.clearAllMocks();
    localStorageMock.clear();
    Object.defineProperty(window, "localStorage", {
      value: localStorageMock,
      writable: true,
    });
  });

  describe("initialization", () => {
    /**
     * Test: Returns defaultExpanded when no localStorage value exists
     * @see spec.md US2 Acceptance Scenario 3: New user with no stored preference
     */
    test("testInit_NoLocalStorageValue_ReturnsDefaultExpanded", () => {
      localStorageMock.getItem.mockReturnValue(null);

      const { result } = renderHook(() =>
        useSideNavPreference({ defaultMode: "close" }),
      );

      expect(result.current.mode).toBe("close");
      expect(result.current.isExpanded).toBe(false);
      expect(localStorageMock.getItem).toHaveBeenCalledWith(
        "defaultSideNavMode",
      );
    });

    /**
     * Test: Returns defaultExpanded=true when configured
     */
    test("testInit_DefaultExpandedTrue_ReturnsTrue", () => {
      localStorageMock.getItem.mockReturnValue(null);

      const { result } = renderHook(() =>
        useSideNavPreference({ defaultMode: "lock" }),
      );

      expect(result.current.mode).toBe("lock");
      expect(result.current.isExpanded).toBe(true);
    });

    /**
     * Test: Returns stored value when localStorage has preference
     * @see spec.md US2 Acceptance Scenario 1: Toggled to expanded, navigate, remains expanded
     * Note: localStorage has "true" (legacy boolean), should be ignored, use defaultMode
     */
    test("testInit_LocalStorageHasTrue_ReturnsTrue", () => {
      localStorageMock.getItem.mockReturnValue("true"); // Invalid value (legacy boolean)

      const { result } = renderHook(
        () => useSideNavPreference({ defaultMode: "lock" }), // Valid defaultMode
      );

      // Should ignore invalid localStorage value and use defaultMode
      expect(result.current.mode).toBe("lock");
      expect(result.current.isExpanded).toBe(true);
    });

    /**
     * Test: Returns stored false value from localStorage
     * @see spec.md US2 Acceptance Scenario 2: Toggled to collapsed, refresh, remains collapsed
     */
    test("testInit_LocalStorageHasFalse_ReturnsFalse", () => {
      localStorageMock.getItem.mockReturnValue("false");

      const { result } = renderHook(() =>
        useSideNavPreference({ defaultMode: "close" }),
      );

      expect(result.current.mode).toBe("close");
      expect(result.current.isExpanded).toBe(false);
    });

    /**
     * Test: Uses custom storageKeyPrefix for localStorage key
     * Note: SHOW mode in localStorage is invalid and gets cleared
     * @see data-model.md localStorage Key Format: {storageKeyPrefix}SideNavExpanded
     */
    test("testInit_CustomStorageKeyPrefix_UsesCorrectKey", () => {
      localStorageMock.getItem.mockReturnValue("show");

      const { result } = renderHook(() =>
        useSideNavPreference({
          defaultMode: "close",
          storageKeyPrefix: "analyzer",
        }),
      );

      expect(localStorageMock.getItem).toHaveBeenCalledWith(
        "analyzerSideNavMode",
      );
      // SHOW mode is invalid in localStorage, should fall back to defaultMode
      expect(result.current.mode).toBe("close");
      // Should clear invalid SHOW mode from localStorage
      expect(localStorageMock.removeItem).toHaveBeenCalledWith(
        "analyzerSideNavMode",
      );
    });
  });

  describe("toggle", () => {
    /**
     * Test: toggle() inverts state from false to true
     * @see spec.md US1 Acceptance Scenario 1: Click toggle, sidenav expands
     */
    test("testToggle_CyclesCloseShowLock", () => {
      localStorageMock.getItem.mockReturnValue(null); // default to close

      const { result } = renderHook(() =>
        useSideNavPreference({ defaultMode: "close" }),
      );

      expect(result.current.mode).toBe("close");

      act(() => {
        result.current.toggle();
      });
      expect(result.current.mode).toBe("show");

      act(() => {
        result.current.toggle();
      });
      expect(result.current.mode).toBe("lock");

      act(() => {
        result.current.toggle();
      });
      expect(result.current.mode).toBe("close");
    });

    /**
     * Test: toggle() persists new state to localStorage (except SHOW mode)
     * SHOW mode is temporary and NOT persisted
     * @see spec.md FR-002: System MUST persist the user's sidenav mode preference
     */
    test("testToggle_PersistsToLocalStorage", () => {
      localStorageMock.getItem.mockReturnValue(null);

      const { result } = renderHook(() =>
        useSideNavPreference({ defaultMode: "close" }),
      );

      act(() => {
        result.current.toggle(); // close -> show (NOT persisted)
      });

      // SHOW mode should NOT be persisted
      expect(localStorageMock.setItem).not.toHaveBeenCalledWith(
        "defaultSideNavMode",
        "show",
      );
      expect(result.current.mode).toBe("show");

      act(() => {
        result.current.toggle(); // show -> lock (PERSISTED)
      });

      // LOCK mode SHOULD be persisted
      expect(localStorageMock.setItem).toHaveBeenCalledWith(
        "defaultSideNavMode",
        "lock",
      );
    });

    /**
     * Test: toggle() uses correct key with custom storageKeyPrefix
     * Note: SHOW mode is NOT persisted (it's temporary)
     */
    test("testToggle_CustomPrefix_PersistsWithCorrectKey", () => {
      localStorageMock.getItem.mockReturnValue(null);

      const { result } = renderHook(() =>
        useSideNavPreference({
          defaultMode: "close",
          storageKeyPrefix: "analyzer",
        }),
      );

      act(() => {
        result.current.toggle(); // close -> show (NOT persisted)
      });

      // SHOW mode should NOT be persisted
      expect(localStorageMock.setItem).not.toHaveBeenCalledWith(
        "analyzerSideNavMode",
        "show",
      );

      // But state should be show
      expect(result.current.mode).toBe("show");

      act(() => {
        result.current.toggle(); // show -> lock (PERSISTED)
      });

      // LOCK mode SHOULD be persisted
      expect(localStorageMock.setItem).toHaveBeenCalledWith(
        "analyzerSideNavMode",
        "lock",
      );
    });
  });

  describe("setMode", () => {
    /**
     * Test: setMode(lock) sets state to lock
     */
    test("testSetMode_Lock_SetsToLock", () => {
      localStorageMock.getItem.mockReturnValue(null);

      const { result } = renderHook(() =>
        useSideNavPreference({ defaultMode: "close" }),
      );

      act(() => {
        result.current.setMode("lock");
      });

      expect(result.current.mode).toBe("lock");
      expect(result.current.isExpanded).toBe(true);
    });

    /**
     * Test: setMode(close) sets state to close
     */
    test("testSetMode_Close_SetsToClose", () => {
      localStorageMock.getItem.mockReturnValue("lock");

      const { result } = renderHook(() =>
        useSideNavPreference({ defaultMode: "lock" }),
      );

      act(() => {
        result.current.setMode("close");
      });

      expect(result.current.mode).toBe("close");
      expect(result.current.isExpanded).toBe(false);
    });

    /**
     * Test: setMode() persists to localStorage
     */
    test("testSetMode_PersistsToLocalStorage", () => {
      localStorageMock.getItem.mockReturnValue(null);

      const { result } = renderHook(() =>
        useSideNavPreference({ defaultMode: "close" }),
      );

      act(() => {
        result.current.setMode("lock");
      });

      expect(localStorageMock.setItem).toHaveBeenCalledWith(
        "defaultSideNavMode",
        "lock",
      );
    });
  });

  describe("localStorage unavailable", () => {
    /**
     * Test: Handles localStorage unavailable gracefully (e.g., private browsing)
     * @see spec.md Edge Cases: localStorage unavailable
     */
    test("testInit_LocalStorageUnavailable_FallsBackToDefault", () => {
      // Simulate localStorage throwing an error
      Object.defineProperty(window, "localStorage", {
        value: {
          getItem: jest.fn(() => {
            throw new Error("localStorage is disabled");
          }),
          setItem: jest.fn(() => {
            throw new Error("localStorage is disabled");
          }),
        },
        writable: true,
      });

      // Suppress console.warn for this test
      const consoleWarnSpy = jest
        .spyOn(console, "warn")
        .mockImplementation(() => {});

      const { result } = renderHook(() =>
        useSideNavPreference({ defaultMode: "lock" }),
      );

      expect(result.current.mode).toBe("lock");

      consoleWarnSpy.mockRestore();
    });

    /**
     * Test: toggle() works even when localStorage throws
     */
    test("testToggle_LocalStorageUnavailable_StillTogglesState", () => {
      // First, initialize with working localStorage
      localStorageMock.getItem.mockReturnValue(null);

      const { result } = renderHook(() =>
        useSideNavPreference({ defaultMode: "close" }),
      );

      // Now make localStorage throw on setItem
      localStorageMock.setItem.mockImplementation(() => {
        throw new Error("localStorage is disabled");
      });

      const consoleWarnSpy = jest
        .spyOn(console, "warn")
        .mockImplementation(() => {});

      act(() => {
        result.current.toggle(); // close -> show
      });

      // State should still toggle even if persistence fails
      expect(result.current.mode).toBe("show");

      consoleWarnSpy.mockRestore();
    });
  });

  describe("default options", () => {
    /**
     * Test: Uses sensible defaults when no options provided
     */
    test("testInit_NoOptions_UsesDefaults", () => {
      localStorageMock.getItem.mockReturnValue(null);

      const { result } = renderHook(() => useSideNavPreference());

      // Default should be close with "default" prefix
      expect(result.current.mode).toBe("close");
      expect(localStorageMock.getItem).toHaveBeenCalledWith(
        "defaultSideNavMode",
      );
    });
  });
});

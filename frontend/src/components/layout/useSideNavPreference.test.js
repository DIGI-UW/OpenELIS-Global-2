import { renderHook, act } from "@testing-library/react-hooks";
import { useSideNavPreference } from "./useSideNavPreference";

/**
 * Unit tests for useSideNavPreference hook
 *
 * This hook manages sidenav expanded/collapsed state with localStorage persistence.
 * Tests cover:
 * - Default state when no localStorage value exists
 * - Restoring state from localStorage
 * - Toggle functionality with persistence
 * - setExpanded functionality with persistence
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
        useSideNavPreference({ defaultExpanded: false }),
      );

      expect(result.current.isExpanded).toBe(false);
      expect(localStorageMock.getItem).toHaveBeenCalledWith(
        "defaultSideNavExpanded",
      );
    });

    /**
     * Test: Returns defaultExpanded=true when configured
     */
    test("testInit_DefaultExpandedTrue_ReturnsTrue", () => {
      localStorageMock.getItem.mockReturnValue(null);

      const { result } = renderHook(() =>
        useSideNavPreference({ defaultExpanded: true }),
      );

      expect(result.current.isExpanded).toBe(true);
    });

    /**
     * Test: Returns stored value when localStorage has preference
     * @see spec.md US2 Acceptance Scenario 1: Toggled to expanded, navigate, remains expanded
     */
    test("testInit_LocalStorageHasTrue_ReturnsTrue", () => {
      localStorageMock.getItem.mockReturnValue("true");

      const { result } = renderHook(() =>
        useSideNavPreference({ defaultExpanded: false }),
      );

      expect(result.current.isExpanded).toBe(true);
    });

    /**
     * Test: Returns stored false value from localStorage
     * @see spec.md US2 Acceptance Scenario 2: Toggled to collapsed, refresh, remains collapsed
     */
    test("testInit_LocalStorageHasFalse_ReturnsFalse", () => {
      localStorageMock.getItem.mockReturnValue("false");

      const { result } = renderHook(() =>
        useSideNavPreference({ defaultExpanded: true }),
      );

      expect(result.current.isExpanded).toBe(false);
    });

    /**
     * Test: Uses custom storageKeyPrefix for localStorage key
     * @see data-model.md localStorage Key Format: {storageKeyPrefix}SideNavExpanded
     */
    test("testInit_CustomStorageKeyPrefix_UsesCorrectKey", () => {
      localStorageMock.getItem.mockReturnValue("true");

      const { result } = renderHook(() =>
        useSideNavPreference({
          defaultExpanded: false,
          storageKeyPrefix: "analyzer",
        }),
      );

      expect(localStorageMock.getItem).toHaveBeenCalledWith(
        "analyzerSideNavExpanded",
      );
      expect(result.current.isExpanded).toBe(true);
    });
  });

  describe("toggle", () => {
    /**
     * Test: toggle() inverts state from false to true
     * @see spec.md US1 Acceptance Scenario 1: Click toggle, sidenav expands
     */
    test("testToggle_FromFalse_SetsToTrue", () => {
      localStorageMock.getItem.mockReturnValue(null);

      const { result } = renderHook(() =>
        useSideNavPreference({ defaultExpanded: false }),
      );

      expect(result.current.isExpanded).toBe(false);

      act(() => {
        result.current.toggle();
      });

      expect(result.current.isExpanded).toBe(true);
    });

    /**
     * Test: toggle() inverts state from true to false
     * @see spec.md US1 Acceptance Scenario 2: Click toggle, sidenav collapses
     */
    test("testToggle_FromTrue_SetsToFalse", () => {
      localStorageMock.getItem.mockReturnValue("true");

      const { result } = renderHook(() =>
        useSideNavPreference({ defaultExpanded: false }),
      );

      expect(result.current.isExpanded).toBe(true);

      act(() => {
        result.current.toggle();
      });

      expect(result.current.isExpanded).toBe(false);
    });

    /**
     * Test: toggle() persists new state to localStorage
     * @see spec.md FR-002: System MUST persist the user's sidenav mode preference
     */
    test("testToggle_PersistsToLocalStorage", () => {
      localStorageMock.getItem.mockReturnValue(null);

      const { result } = renderHook(() =>
        useSideNavPreference({ defaultExpanded: false }),
      );

      act(() => {
        result.current.toggle();
      });

      expect(localStorageMock.setItem).toHaveBeenCalledWith(
        "defaultSideNavExpanded",
        "true",
      );
    });

    /**
     * Test: toggle() uses correct key with custom storageKeyPrefix
     */
    test("testToggle_CustomPrefix_PersistsWithCorrectKey", () => {
      localStorageMock.getItem.mockReturnValue(null);

      const { result } = renderHook(() =>
        useSideNavPreference({
          defaultExpanded: false,
          storageKeyPrefix: "analyzer",
        }),
      );

      act(() => {
        result.current.toggle();
      });

      expect(localStorageMock.setItem).toHaveBeenCalledWith(
        "analyzerSideNavExpanded",
        "true",
      );
    });
  });

  describe("setExpanded", () => {
    /**
     * Test: setExpanded(true) sets state to true
     */
    test("testSetExpanded_True_SetsToTrue", () => {
      localStorageMock.getItem.mockReturnValue(null);

      const { result } = renderHook(() =>
        useSideNavPreference({ defaultExpanded: false }),
      );

      act(() => {
        result.current.setExpanded(true);
      });

      expect(result.current.isExpanded).toBe(true);
    });

    /**
     * Test: setExpanded(false) sets state to false
     */
    test("testSetExpanded_False_SetsToFalse", () => {
      localStorageMock.getItem.mockReturnValue("true");

      const { result } = renderHook(() =>
        useSideNavPreference({ defaultExpanded: false }),
      );

      act(() => {
        result.current.setExpanded(false);
      });

      expect(result.current.isExpanded).toBe(false);
    });

    /**
     * Test: setExpanded() persists to localStorage
     */
    test("testSetExpanded_PersistsToLocalStorage", () => {
      localStorageMock.getItem.mockReturnValue(null);

      const { result } = renderHook(() =>
        useSideNavPreference({ defaultExpanded: false }),
      );

      act(() => {
        result.current.setExpanded(true);
      });

      expect(localStorageMock.setItem).toHaveBeenCalledWith(
        "defaultSideNavExpanded",
        "true",
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
        useSideNavPreference({ defaultExpanded: true }),
      );

      expect(result.current.isExpanded).toBe(true);

      consoleWarnSpy.mockRestore();
    });

    /**
     * Test: toggle() works even when localStorage throws
     */
    test("testToggle_LocalStorageUnavailable_StillTogglesState", () => {
      // First, initialize with working localStorage
      localStorageMock.getItem.mockReturnValue(null);

      const { result } = renderHook(() =>
        useSideNavPreference({ defaultExpanded: false }),
      );

      // Now make localStorage throw on setItem
      localStorageMock.setItem.mockImplementation(() => {
        throw new Error("localStorage is disabled");
      });

      const consoleWarnSpy = jest
        .spyOn(console, "warn")
        .mockImplementation(() => {});

      act(() => {
        result.current.toggle();
      });

      // State should still toggle even if persistence fails
      expect(result.current.isExpanded).toBe(true);

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

      // Default should be collapsed (false) with "default" prefix
      expect(result.current.isExpanded).toBe(false);
      expect(localStorageMock.getItem).toHaveBeenCalledWith(
        "defaultSideNavExpanded",
      );
    });
  });
});

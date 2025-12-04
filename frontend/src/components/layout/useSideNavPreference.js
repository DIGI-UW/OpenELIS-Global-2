import { useState, useCallback } from "react";

/**
 * Custom hook for managing sidenav expanded/collapsed state with localStorage persistence.
 *
 * This hook provides:
 * - Initialization from localStorage (with fallback to defaultExpanded)
 * - Toggle function that inverts state and persists
 * - setExpanded function for programmatic control
 * - Graceful handling when localStorage is unavailable (e.g., private browsing)
 *
 * @see spec.md US2: Persist User Preference Across Sessions (P1)
 * @see spec.md FR-002: System MUST persist the user's sidenav mode preference
 * @see data-model.md UseSideNavPreferenceOptions and UseSideNavPreferenceReturn interfaces
 *
 * @param {Object} options - Configuration options
 * @param {boolean} [options.defaultExpanded=false] - Default state when no preference is stored
 * @param {string} [options.storageKeyPrefix='default'] - Prefix for localStorage key
 * @returns {Object} Hook return value
 * @returns {boolean} returns.isExpanded - Current sidenav expansion state
 * @returns {function} returns.toggle - Toggle function (also persists to localStorage)
 * @returns {function} returns.setExpanded - Programmatically set state (also persists)
 *
 * @example
 * // Basic usage
 * const { isExpanded, toggle } = useSideNavPreference();
 *
 * @example
 * // With custom defaults
 * const { isExpanded, toggle, setExpanded } = useSideNavPreference({
 *   defaultExpanded: true,
 *   storageKeyPrefix: 'analyzer'
 * });
 */
export function useSideNavPreference({
  defaultExpanded = false,
  storageKeyPrefix = "default",
} = {}) {
  const storageKey = `${storageKeyPrefix}SideNavExpanded`;

  /**
   * Initialize state from localStorage, falling back to defaultExpanded.
   * Uses a function initializer to avoid reading localStorage on every render.
   */
  const [isExpanded, setIsExpanded] = useState(() => {
    try {
      const saved = localStorage.getItem(storageKey);
      if (saved !== null) {
        return saved === "true";
      }
      return defaultExpanded;
    } catch (e) {
      // localStorage unavailable (e.g., private browsing mode)
      console.warn(
        "localStorage unavailable, using default sidenav preference",
      );
      return defaultExpanded;
    }
  });

  /**
   * Toggle sidenav state and persist to localStorage.
   * @see spec.md US1: Toggle Sidenav Between Modes
   */
  const toggle = useCallback(() => {
    setIsExpanded((prev) => {
      const newValue = !prev;
      try {
        localStorage.setItem(storageKey, String(newValue));
      } catch (e) {
        console.warn("Could not persist sidenav preference to localStorage");
      }
      return newValue;
    });
  }, [storageKey]);

  /**
   * Programmatically set sidenav state and persist to localStorage.
   * Useful for page-level configuration or external control.
   */
  const setExpanded = useCallback(
    (value) => {
      setIsExpanded(value);
      try {
        localStorage.setItem(storageKey, String(value));
      } catch (e) {
        console.warn("Could not persist sidenav preference to localStorage");
      }
    },
    [storageKey],
  );

  return { isExpanded, toggle, setExpanded };
}

export default useSideNavPreference;

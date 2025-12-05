import { useState, useCallback, useEffect } from "react";

const SIDENAV_MODES = {
  SHOW: "show", // overlay, auto-close on outside click
  LOCK: "lock", // pushes content, stays open
  CLOSE: "close", // rail / collapsed
};

const MODE_CYCLE = [
  SIDENAV_MODES.CLOSE,
  SIDENAV_MODES.SHOW,
  SIDENAV_MODES.LOCK,
];

/**
 * Custom hook for managing sidenav mode (show/lock/close) with localStorage persistence.
 *
 * This hook provides:
 * - Initialization from localStorage (with fallback to defaultMode)
 * - toggle() cycles through modes: close -> show -> lock -> close
 * - setMode() for direct control
 * - Graceful handling when localStorage is unavailable (e.g., private browsing)
 *
 * @see spec.md US2: Persist User Preference Across Sessions (P1)
 * @see spec.md FR-002: System MUST persist the user's sidenav mode preference
 * @see data-model.md UseSideNavPreferenceOptions and UseSideNavPreferenceReturn interfaces
 *
 * @param {Object} options - Configuration options
 * @param {("show"|"lock"|"close")} [options.defaultMode="close"] - Default mode when no preference is stored
 * @param {boolean} [options.defaultExpanded] - Deprecated: maps true -> lock, false -> close
 * @param {string} [options.storageKeyPrefix='default'] - Prefix for localStorage key
 * @returns {Object} Hook return value
 * @returns {("show"|"lock"|"close")} returns.mode - Current sidenav mode
 * @returns {boolean} returns.isExpanded - Derived expansion state (show/lock = true, close = false)
 * @returns {function} returns.toggle - Cycle mode (persists)
 * @returns {function} returns.setMode - Programmatically set mode (persists)
 *
 * @example
 * // Basic usage
 * const { mode, toggle } = useSideNavPreference();
 *
 * @example
 * // With custom defaults
 * const { mode, toggle, setMode } = useSideNavPreference({
 *   defaultMode: 'lock',
 *   storageKeyPrefix: 'analyzer'
 * });
 */
export function useSideNavPreference({
  defaultMode,
  defaultExpanded, // deprecated, kept for backward compatibility
  storageKeyPrefix = "default",
} = {}) {
  const storageKey = `${storageKeyPrefix}SideNavMode`;

  const initialMode = () => {
    try {
      const saved = localStorage.getItem(storageKey);
      if (
        saved &&
        [SIDENAV_MODES.SHOW, SIDENAV_MODES.LOCK, SIDENAV_MODES.CLOSE].includes(
          saved,
        )
      ) {
        return saved;
      }
    } catch (e) {
      console.warn("localStorage unavailable, using default sidenav mode");
    }
    if (
      defaultMode &&
      [SIDENAV_MODES.SHOW, SIDENAV_MODES.LOCK, SIDENAV_MODES.CLOSE].includes(
        defaultMode,
      )
    ) {
      return defaultMode;
    }
    if (typeof defaultExpanded === "boolean") {
      return defaultExpanded ? SIDENAV_MODES.LOCK : SIDENAV_MODES.CLOSE;
    }
    return SIDENAV_MODES.CLOSE;
  };

  /**
   * Initialize state from localStorage, falling back to defaultMode/defaultExpanded.
   * Uses a function initializer to avoid reading localStorage on every render.
   */
  const [mode, setModeState] = useState(initialMode);

  /**
   * Reset state when storageKeyPrefix changes (e.g. switching between main and storage layouts)
   */
  useEffect(() => {
    setModeState(initialMode());
  }, [storageKeyPrefix]);

  /**
   * Persist helper
   */
  const persistMode = useCallback(
    (value) => {
      try {
        localStorage.setItem(storageKey, value);
      } catch (e) {
        console.warn("Could not persist sidenav mode to localStorage");
      }
    },
    [storageKey],
  );

  /**
   * Toggle sidenav mode in a 3-step cycle: close -> show -> lock -> close
   */
  const toggle = useCallback(() => {
    setModeState((prev) => {
      const currentIndex = MODE_CYCLE.indexOf(prev);
      const nextMode = MODE_CYCLE[(currentIndex + 1) % MODE_CYCLE.length];
      persistMode(nextMode);
      return nextMode;
    });
  }, [persistMode]);

  /**
   * Programmatically set sidenav mode and persist to localStorage.
   */
  const setMode = useCallback(
    (value) => {
      setModeState(value);
      persistMode(value);
    },
    [persistMode],
  );

  const isExpanded = mode !== SIDENAV_MODES.CLOSE;

  return { mode, isExpanded, toggle, setMode, SIDENAV_MODES };
}

export default useSideNavPreference;

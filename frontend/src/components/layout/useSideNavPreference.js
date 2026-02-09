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

  /**
   * Initialize mode on component mount.
   * SIMPLE: Read localStorage → defaultMode → CLOSE
   */
  const initialMode = () => {
    try {
      const saved = localStorage.getItem(storageKey);

      // Reject SHOW (temporary only) - defensive cleanup
      if (saved === SIDENAV_MODES.SHOW) {
        localStorage.removeItem(storageKey);
      }
      // Accept LOCK or CLOSE
      else if (
        saved &&
        [SIDENAV_MODES.LOCK, SIDENAV_MODES.CLOSE].includes(saved)
      ) {
        return saved;
      }
    } catch (e) {
      // localStorage unavailable (private browsing, etc.)
    }

    // No saved value - use defaultMode
    if (
      defaultMode &&
      [SIDENAV_MODES.LOCK, SIDENAV_MODES.CLOSE].includes(defaultMode)
    ) {
      return defaultMode;
    }

    // Fallback
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
   *
   * CORRECT BEHAVIOR:
   * 1. Read localStorage for the NEW context (each context has its own key)
   * 2. If found and valid (not SHOW), use it (user's preference for this context!)
   * 3. If not found, use defaultMode for this context
   *
   * This ensures:
   * - User preferences persist within each context ✅
   * - Contexts don't pollute each other (separate keys) ✅
   * - SHOW mode doesn't persist (filtered out) ✅
   */
  useEffect(() => {
    const newStorageKey = `${storageKeyPrefix}SideNavMode`;

    try {
      const saved = localStorage.getItem(newStorageKey);

      // Ignore SHOW mode from localStorage (defensive cleanup)
      if (saved === SIDENAV_MODES.SHOW) {
        localStorage.removeItem(newStorageKey);
      }
      // Use valid saved preference for this context
      else if (
        saved &&
        [SIDENAV_MODES.LOCK, SIDENAV_MODES.CLOSE].includes(saved)
      ) {
        setModeState(saved);
        return; // Early return - we found a valid saved preference
      }
      // Clear invalid values
      else if (saved) {
        localStorage.removeItem(newStorageKey);
      }
    } catch (e) {
      // localStorage unavailable
    }

    // No saved preference - use defaultMode
    const effectiveDefault =
      defaultMode &&
      [SIDENAV_MODES.LOCK, SIDENAV_MODES.CLOSE].includes(defaultMode)
        ? defaultMode
        : typeof defaultExpanded === "boolean"
          ? defaultExpanded
            ? SIDENAV_MODES.LOCK
            : SIDENAV_MODES.CLOSE
          : SIDENAV_MODES.CLOSE;

    setModeState(effectiveDefault);
  }, [storageKeyPrefix, defaultMode, defaultExpanded]);

  /**
   * Persist helper - ONLY persist when user explicitly changes from default!
   *
   * RULES:
   * - SHOW mode is temporary - never persist
   * - Only persist if new mode is DIFFERENT from defaultMode
   *   - Dashboard (default CLOSE): Only persist LOCK
   *   - Storage (default LOCK): Only persist CLOSE
   * - If new mode matches defaultMode, clear localStorage (user reset to default)
   */
  const persistMode = useCallback(
    (value) => {
      // SHOW mode is temporary - don't persist it
      if (value === SIDENAV_MODES.SHOW) {
        return;
      }

      // Determine effective default for comparison
      const effectiveDefault =
        defaultMode &&
        [SIDENAV_MODES.LOCK, SIDENAV_MODES.CLOSE].includes(defaultMode)
          ? defaultMode
          : typeof defaultExpanded === "boolean"
            ? defaultExpanded
              ? SIDENAV_MODES.LOCK
              : SIDENAV_MODES.CLOSE
            : SIDENAV_MODES.CLOSE;

      // Only persist if different from default (user explicitly changed preference)
      if (value === effectiveDefault) {
        // User reset to default - clear saved preference
        try {
          localStorage.removeItem(storageKey);
        } catch (e) {
          // Ignore
        }
        return;
      }

      // User changed from default - persist the preference
      try {
        localStorage.setItem(storageKey, value);
      } catch (e) {
        // Ignore
      }
    },
    [storageKey, defaultMode, defaultExpanded],
  );

  /**
   * Toggle sidenav mode in a 3-step cycle: close -> show -> lock -> close
   */
  const toggle = useCallback(() => {
    setModeState((prev) => {
      const currentIndex = MODE_CYCLE.indexOf(prev);
      const nextMode = MODE_CYCLE[(currentIndex + 1) % MODE_CYCLE.length];
      // When cycling into SHOW, do not persist (temporary). Persist otherwise.
      if (nextMode !== SIDENAV_MODES.SHOW) {
        persistMode(nextMode);
      }
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

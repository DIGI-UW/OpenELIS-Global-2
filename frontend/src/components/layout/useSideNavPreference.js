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
  const versionKey = `sidenavPreferenceVersion`;
  const CURRENT_VERSION = "2.0.0"; // Incremented due to context-aware localStorage fix

  /**
   * Migration: Clean up stale localStorage from previous implementations.
   * Run once per version to fix localStorage pollution.
   * 
   * CRITICAL: Previous implementation had bug where SHOW mode auto-close
   * would persist 'close' to storageSideNavMode, overwriting LOCK defaults.
   * This migration ensures clean slate.
   */
  const runMigration = () => {
    try {
      const storedVersion = localStorage.getItem(versionKey);
      
      if (storedVersion !== CURRENT_VERSION) {
        console.log(
          `[useSideNavPreference] Migration: v${storedVersion || 'legacy'} → v${CURRENT_VERSION}`,
        );
        console.log(`[useSideNavPreference] Clearing potentially polluted localStorage keys...`);
        
        // Clear all sidenav preference keys
        // These may have cross-context pollution from previous bugs
        const keysToMigrate = [
          'mainSideNavMode',
          'storageSideNavMode', 
          'defaultSideNavMode',
          // Add any other context prefixes here if added in future
        ];
        
        keysToMigrate.forEach(key => {
          const oldValue = localStorage.getItem(key);
          if (oldValue) {
            console.log(`[useSideNavPreference] Migration: Removing ${key} = ${oldValue}`);
            localStorage.removeItem(key);
          }
        });
        
        // Set new version marker
        localStorage.setItem(versionKey, CURRENT_VERSION);
        console.log(`[useSideNavPreference] Migration complete ✅`);
      }
    } catch (e) {
      console.warn('localStorage migration failed (may be unavailable):', e);
    }
  };

  const initialMode = () => {
    // Run migration on first access (once per version)
    runMigration();
    
    let savedValue = null;
    try {
      savedValue = localStorage.getItem(storageKey);
      console.log(`[useSideNavPreference] Reading from localStorage:`, {
        storageKey,
        savedValue,
        defaultMode,
      });

      // SHOW mode should never be persisted - it's temporary only
      // If we find it in localStorage, treat it as invalid and use default
      if (savedValue === SIDENAV_MODES.SHOW) {
        console.warn(
          `[useSideNavPreference] Found invalid SHOW mode in localStorage - SHOW is temporary only. Clearing and using default.`,
        );
        try {
          localStorage.removeItem(storageKey);
        } catch (e) {
          console.warn("Could not clear invalid SHOW mode from localStorage");
        }
        // Fall through to use defaultMode
      } else if (
        savedValue &&
        [SIDENAV_MODES.LOCK, SIDENAV_MODES.CLOSE].includes(savedValue)
      ) {
        console.log(`[useSideNavPreference] Using saved mode:`, savedValue);
        return savedValue;
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
      console.log(
        `[useSideNavPreference] Using defaultMode (no saved value):`,
        defaultMode,
      );
      return defaultMode;
    }

    if (typeof defaultExpanded === "boolean") {
      const mode = defaultExpanded ? SIDENAV_MODES.LOCK : SIDENAV_MODES.CLOSE;
      console.log(
        `[useSideNavPreference] Using defaultExpanded:`,
        defaultExpanded,
        "=>",
        mode,
      );
      return mode;
    }

    console.log(`[useSideNavPreference] Using fallback CLOSE mode`);
    return SIDENAV_MODES.CLOSE;
  };

  /**
   * Initialize state from localStorage, falling back to defaultMode/defaultExpanded.
   * Uses a function initializer to avoid reading localStorage on every render.
   */
  const [mode, setModeState] = useState(initialMode);

  /**
   * Reset state when storageKeyPrefix changes (e.g. switching between main and storage layouts)
   * CRITICAL: Use defaultMode directly on context switch, NOT localStorage
   * Reason: User's manual toggle in one context should not affect another context's default
   * Example: User closes nav on Dashboard → should not prevent Storage from defaulting to LOCK
   */
  useEffect(() => {
    // On context switch, always apply the new context's defaultMode
    // Do NOT read from localStorage here - that would allow preferences from one context
    // to bleed into another context (e.g., closing nav on Dashboard shouldn't affect Storage's LOCK default)
    const effectiveDefault = defaultMode && 
      [SIDENAV_MODES.SHOW, SIDENAV_MODES.LOCK, SIDENAV_MODES.CLOSE].includes(defaultMode)
      ? defaultMode
      : (typeof defaultExpanded === "boolean" 
          ? (defaultExpanded ? SIDENAV_MODES.LOCK : SIDENAV_MODES.CLOSE)
          : SIDENAV_MODES.CLOSE);
    
    console.log(
      `[useSideNavPreference] storageKeyPrefix changed, applying defaultMode:`,
      {
        storageKeyPrefix,
        defaultMode: effectiveDefault,
        previousMode: mode,
        ignoring_localStorage: true,
      },
    );
    setModeState(effectiveDefault);
  }, [storageKeyPrefix, defaultMode]);

  /**
   * Persist helper - IMPORTANT: SHOW mode is temporary and should NOT be persisted!
   * Only CLOSE and LOCK modes are saved to localStorage.
   * SHOW mode is for temporary overlay that auto-closes on outside click.
   */
  const persistMode = useCallback(
    (value) => {
      // SHOW mode is temporary - don't persist it
      if (value === SIDENAV_MODES.SHOW) {
        console.log(
          `[useSideNavPreference] SHOW mode is temporary - not persisting to localStorage`,
        );
        return;
      }

      try {
        console.log(`[useSideNavPreference] Persisting mode to localStorage:`, {
          storageKey,
          value,
        });
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
      console.log(
        `[useSideNavPreference] toggle() called:`,
        prev,
        "=>",
        nextMode,
      );
      persistMode(nextMode);
      return nextMode;
    });
  }, [persistMode]);

  /**
   * Programmatically set sidenav mode and persist to localStorage.
   */
  const setMode = useCallback(
    (value) => {
      console.log(
        `[useSideNavPreference] setMode() called:`,
        mode,
        "=>",
        value,
      );
      setModeState(value);
      persistMode(value);
    },
    [persistMode, mode],
  );

  const isExpanded = mode !== SIDENAV_MODES.CLOSE;

  return { mode, isExpanded, toggle, setMode, SIDENAV_MODES };
}

export default useSideNavPreference;

/**
 * Layout Components - Public API
 *
 * Re-exports layout components for convenient importing.
 *
 * @example
 * import { TwoModeLayout, useSideNavPreference } from 'components/layout';
 */

// Core layout component with two-mode sidenav
export { default as TwoModeLayout } from "./TwoModeLayout";

// Hook for managing sidenav preference with localStorage persistence
export { useSideNavPreference } from "./useSideNavPreference";

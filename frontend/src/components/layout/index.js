/**
 * Layout Components - Public API
 *
 * Re-exports layout components for convenient importing.
 *
 * @example
 * import { useSideNavPreference, useMenuAutoExpand } from 'components/layout';
 */

// Hook for managing sidenav preference with localStorage persistence
export { useSideNavPreference } from "./useSideNavPreference";

// Hook for auto-expanding menu items based on current route
export { useMenuAutoExpand } from "./useMenuAutoExpand";

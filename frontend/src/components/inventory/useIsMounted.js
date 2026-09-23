import { useCallback, useEffect, useRef } from "react";

/**
 * Whether the component is still on the page.
 *
 * Saving from one of these modals calls onSave(), which unmounts it — and the
 * finally block that clears the busy flag runs after that. Setting state there
 * on a component that is gone is a warning at best and a leak at worst, so
 * every write after an await is guarded by this.
 *
 * Returns a function rather than the ref so callers read `isMounted()` instead
 * of reaching into `.current`, and cannot accidentally read the ref object as
 * a truthy value.
 */
export const useIsMounted = () => {
  const mounted = useRef(true);
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);
  return useCallback(() => mounted.current, []);
};

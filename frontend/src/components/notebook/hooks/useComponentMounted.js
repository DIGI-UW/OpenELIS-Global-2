import { useRef, useEffect, useCallback } from "react";

/**
 * Custom hook for managing component mounted state and safe async operations.
 * Prevents state updates after component unmount.
 *
 * @returns {Object} Object containing:
 *   - componentMounted: ref that is true while component is mounted
 *   - safeSetState: wrapper function that only calls setState if mounted
 *   - runAsync: wrapper for async operations with mounted check
 *
 * @example
 * const { componentMounted, safeSetState, runAsync } = useComponentMounted();
 *
 * // Use safeSetState for direct setState calls
 * safeSetState(setData, newData);
 *
 * // Use runAsync for async operations
 * runAsync(async () => {
 *   const response = await fetchData();
 *   setData(response);
 * });
 *
 * // Or use componentMounted.current in callbacks
 * getFromServer("/api/data", (response) => {
 *   if (componentMounted.current) {
 *     setData(response);
 *   }
 * });
 */
export function useComponentMounted() {
  const componentMounted = useRef(true);

  useEffect(() => {
    componentMounted.current = true;

    return () => {
      componentMounted.current = false;
    };
  }, []);

  /**
   * Safely call a setState function only if component is mounted.
   * @param {Function} setter - The setState function
   * @param {any} value - The value to set
   */
  const safeSetState = useCallback((setter, value) => {
    if (componentMounted.current) {
      setter(value);
    }
  }, []);

  /**
   * Run an async function and only allow state updates if component is still mounted.
   * @param {Function} asyncFn - Async function that receives an isMounted check function
   * @returns {Promise} The result of the async function
   *
   * @example
   * runAsync(async (isMounted) => {
   *   const data = await fetchData();
   *   if (isMounted()) {
   *     setData(data);
   *   }
   * });
   */
  const runAsync = useCallback((asyncFn) => {
    const isMounted = () => componentMounted.current;
    return asyncFn(isMounted);
  }, []);

  return {
    componentMounted,
    safeSetState,
    runAsync,
  };
}

/**
 * Custom hook for async effects with automatic mounted state management.
 * Ensures async operations don't update state after unmount.
 *
 * @param {Function} asyncFn - Async function to run, receives componentMounted ref
 * @param {Array} deps - Dependency array for the effect
 * @returns {Object} Object containing componentMounted ref
 *
 * @example
 * const { componentMounted } = useAsyncEffect(async (mounted) => {
 *   const data = await fetchData();
 *   if (mounted.current) {
 *     setData(data);
 *   }
 * }, [dependency]);
 */
export function useAsyncEffect(asyncFn, deps) {
  const componentMounted = useRef(true);

  useEffect(() => {
    componentMounted.current = true;

    // Run the async function
    asyncFn(componentMounted);

    return () => {
      componentMounted.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return { componentMounted };
}

/**
 * Custom hook for creating a safe callback that checks mounted state.
 * Wraps a callback function with a componentMounted check.
 *
 * @param {Function} callback - The callback function to wrap
 * @param {React.RefObject} componentMounted - The mounted ref from useComponentMounted
 * @returns {Function} Wrapped callback that only executes if mounted
 *
 * @example
 * const { componentMounted } = useComponentMounted();
 *
 * const handleResponse = useSafeCallback((response) => {
 *   setData(response);
 *   setLoading(false);
 * }, componentMounted);
 *
 * getFromServer("/api/data", handleResponse);
 */
export function useSafeCallback(callback, componentMounted) {
  return useCallback(
    (...args) => {
      if (componentMounted.current) {
        return callback(...args);
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [callback],
  );
}

export default useComponentMounted;

import { useCallback, useEffect, useRef } from "react";

const useModalFocusReturn = () => {
  const returnTargetRef = useRef(null);
  const frameRef = useRef(null);

  useEffect(
    () => () => {
      if (frameRef.current !== null) {
        window.cancelAnimationFrame(frameRef.current);
      }
    },
    [],
  );

  const rememberReturnFocus = useCallback((target) => {
    returnTargetRef.current = target instanceof HTMLElement ? target : null;
  }, []);

  const restoreReturnFocus = useCallback(() => {
    const target = returnTargetRef.current;
    returnTargetRef.current = null;
    if (!target) {
      return;
    }
    frameRef.current = window.requestAnimationFrame(() => {
      if (target.isConnected) {
        target.focus();
      }
      frameRef.current = null;
    });
  }, []);

  return { rememberReturnFocus, restoreReturnFocus };
};

export default useModalFocusReturn;

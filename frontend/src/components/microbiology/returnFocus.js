import { useEffect, useState } from "react";

// Carbon's Modal focuses its primary button on open, a no-op while it is disabled.
export const MODAL_FIRST_FIELD = "select, input:not([type=hidden]), textarea";

// Browsers drop focus to <body> when the focused control unmounts or turns disabled.
export const returnFocus = (trigger) =>
  setTimeout(() => {
    if (document.activeElement === document.body && trigger?.isConnected)
      trigger.focus();
  });

export const useReturnFocus = () => {
  const [trigger] = useState(() => document.activeElement);
  useEffect(() => () => returnFocus(trigger), [trigger]);
};

export const ReturnFocus = () => {
  useReturnFocus();
  return null;
};

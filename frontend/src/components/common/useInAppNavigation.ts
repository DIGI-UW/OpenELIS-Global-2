import { useCallback } from "react";
import type React from "react";
import { useHistory } from "react-router-dom";

type ClickLike = Pick<
  React.MouseEvent,
  | "defaultPrevented"
  | "button"
  | "metaKey"
  | "ctrlKey"
  | "shiftKey"
  | "altKey"
  | "preventDefault"
>;

/**
 * Carbon tiles, links and buttons given an `href` render a plain anchor, so a
 * click reloads the whole application. Returns `(href) => onClick` handlers
 * that route inside the app instead, leaving modified and non-primary clicks
 * (new tab, new window) to the browser as with any link.
 */
export const isPlainLeftClick = (event: ClickLike) =>
  !event.defaultPrevented &&
  event.button === 0 &&
  !event.metaKey &&
  !event.ctrlKey &&
  !event.shiftKey &&
  !event.altKey;

export default function useInAppNavigation() {
  const history = useHistory();
  return useCallback(
    (href: string) => (event?: ClickLike) => {
      if (event && !isPlainLeftClick(event)) {
        return;
      }
      event?.preventDefault();
      history.push(href);
    },
    [history],
  );
}

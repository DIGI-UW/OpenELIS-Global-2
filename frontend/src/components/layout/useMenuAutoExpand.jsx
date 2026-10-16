import { useMemo, useRef } from "react";
import { useLocation } from "react-router-dom";

const normalizePath = (path) =>
  path === "/" ? "/Dashboard" : path.replace(/\/$/, "");

const menuKey = (item) => item.menu.elementId || item.menu.id;

/**
 * Derive one active destination and its ancestors from the router location.
 * A location no menu item matches (a redirect target, a page without its own
 * entry) keeps the ancestors of the last matched item expanded, without
 * marking it active, so the side nav does not fold shut under the user.
 */
export function useMenuAutoExpand(initialMenus) {
  const { pathname, search } = useLocation();
  const lastMatchedKey = useRef();
  return useMemo(() => {
    const currentPath = normalizePath(pathname);
    const currentQuery = new URLSearchParams(search);
    let activeItem;
    let bestScore = -1;
    const findDestination = (items, depth = 0) => {
      for (const item of items) {
        if (item.menu.isActive === false) continue;
        const url = item.menu.actionURL;
        if (url?.startsWith("/") && !url.startsWith("//")) {
          const target = new URL(url, "https://openelis.invalid");
          const path = normalizePath(target.pathname);
          const exact = path === currentPath;
          const prefix =
            !item.childMenus?.some((child) => child.menu.isActive !== false) &&
            currentPath.startsWith(path + "/");
          const queryMatches = [...target.searchParams].every(([key, value]) =>
            currentQuery.getAll(key).includes(value),
          );
          if ((exact || prefix) && queryMatches) {
            // Prefer the most specific route, then its query variant, then
            // the deepest item: a group and its first child can share one
            // URL, and only the child can be shown as selected. Extra
            // page/job/review parameters never change the menu destination.
            const score =
              path.length * 1000 + [...target.searchParams].length * 10 + depth;
            if (score > bestScore) {
              activeItem = item;
              bestScore = score;
            }
          }
        }
        findDestination(item.childMenus || [], depth + 1);
      }
    };
    findDestination(initialMenus || []);
    if (activeItem) {
      lastMatchedKey.current = menuKey(activeItem);
    }
    const anchorKey = activeItem ? undefined : lastMatchedKey.current;
    const annotate = (items) =>
      items.map((item) => {
        const childMenus = annotate(item.childMenus || []);
        const activeChild = childMenus.find(
          (child) =>
            child.routeActive ||
            child.activeDescendantId ||
            (anchorKey !== undefined && menuKey(child) === anchorKey),
        );
        return {
          ...item,
          childMenus,
          routeActive: item === activeItem,
          activeDescendantId: activeChild
            ? activeChild.activeDescendantId ||
              activeChild.menu.elementId ||
              activeChild.menu.id
            : undefined,
          expanded: !!item.expanded || !!activeChild,
        };
      });
    return annotate(initialMenus || []);
  }, [initialMenus, pathname, search]);
}
export default useMenuAutoExpand;

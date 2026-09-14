import { useMemo } from "react";
import { useLocation } from "react-router-dom";

const normalizePath = (path) =>
  path === "/" ? "/Dashboard" : path.replace(/\/$/, "");

/** Derive one active destination and its ancestors from the router location. */
export function useMenuAutoExpand(initialMenus) {
  const { pathname, search } = useLocation();
  return useMemo(() => {
    const currentPath = normalizePath(pathname);
    const currentQuery = new URLSearchParams(search);
    let activeItem;
    let bestScore = -1;
    const findDestination = (items) => {
      for (const item of items) {
        const url = item.menu.actionURL;
        if (url?.startsWith("/") && !url.startsWith("//")) {
          const target = new URL(url, "https://openelis.invalid");
          const path = normalizePath(target.pathname);
          const exact = path === currentPath;
          const prefix =
            !item.childMenus?.length && currentPath.startsWith(path + "/");
          const queryMatches = [...target.searchParams].every(([key, value]) =>
            currentQuery.getAll(key).includes(value),
          );
          if ((exact || prefix) && queryMatches) {
            // Prefer the most specific route, then its query variant. Extra
            // page/job/review parameters never change the menu destination.
            const score = path.length * 1000 + [...target.searchParams].length;
            if (score > bestScore) {
              activeItem = item;
              bestScore = score;
            }
          }
        }
        findDestination(item.childMenus || []);
      }
    };
    findDestination(initialMenus || []);
    const annotate = (items) =>
      items.map((item) => {
        const childMenus = annotate(item.childMenus || []);
        const activeChild = childMenus.find(
          (child) => child.routeActive || child.activeDescendantId,
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

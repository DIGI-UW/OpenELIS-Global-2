import type { ReactNode } from "react";

interface PathRouteProps {
  path: string;
  children: ReactNode;
}

const PathRoute = ({ path, children }: PathRouteProps): ReactNode => {
  let fullPath = window.location.href;
  if (window.location.href.includes("?")) {
    // Remove Query Params from the path
    fullPath = window.location.href.split("?")[0];
  }
  return fullPath.endsWith(path) ? children : null;
};

export default PathRoute;

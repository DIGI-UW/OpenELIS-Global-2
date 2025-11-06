const PathRoute = ({ path, children }) => {
  var fullPath = window.location.href;
  if (window.location.href.includes("?")) {
    // Remove Query Params from the path
    fullPath = window.location.href.split("?")[0];
  }
  return fullPath.endsWith(path) ? children : null;
};

export default PathRoute;

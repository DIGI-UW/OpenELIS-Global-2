import { useHistory } from "react-router-dom";

const PathRoute = ({ path, children }) => {
  const history = useHistory();
  let fullPath = history.location.pathname;

  return fullPath.endsWith(path) ? children : null;
};

export default PathRoute;

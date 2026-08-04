import { useCallback, useMemo } from "react";
import { useHistory, useLocation } from "react-router-dom";
import {
  buildReferenceQuery,
  parseReferenceQuery,
  updateReferenceQuery,
} from "./queryState";

export const useReferenceQuery = () => {
  const history = useHistory();
  const location = useLocation();
  const query = useMemo(
    () => parseReferenceQuery(location.search),
    [location.search],
  );
  const setQuery = useCallback(
    (updates, { replace = false } = {}) => {
      const next = updateReferenceQuery(query, updates);
      const destination = {
        pathname: location.pathname,
        search: buildReferenceQuery(next),
      };
      if (replace) history.replace(destination);
      else history.push(destination);
    },
    [history, location.pathname, query],
  );
  return { query, setQuery };
};

import { useCallback, useEffect, useMemo } from "react";
import { useHistory, useLocation } from "react-router-dom";
import {
  buildReferenceQuery,
  parseReferenceQuery,
  updateReferenceQuery,
  validStatusesForSection,
} from "./queryState";

export const useReferenceQuery = (section) => {
  const history = useHistory();
  const location = useLocation();
  const query = useMemo(
    () =>
      parseReferenceQuery(location.search, validStatusesForSection(section)),
    [location.search, section],
  );
  const canonicalSearch = useMemo(() => buildReferenceQuery(query), [query]);

  useEffect(() => {
    if (location.search.replace(/^\?/, "") !== canonicalSearch) {
      history.replace({ pathname: location.pathname, search: canonicalSearch });
    }
  }, [canonicalSearch, history, location.pathname, location.search]);

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

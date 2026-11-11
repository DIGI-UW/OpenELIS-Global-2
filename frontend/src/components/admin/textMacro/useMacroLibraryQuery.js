import { useCallback, useEffect, useMemo } from "react";
import { useHistory, useLocation } from "react-router-dom";
import {
  buildMacroLibraryQuery,
  parseMacroLibraryQuery,
  updateMacroLibraryQuery,
} from "./queryState";

export const useMacroLibraryQuery = () => {
  const history = useHistory();
  const location = useLocation();
  const query = useMemo(
    () => parseMacroLibraryQuery(location.search),
    [location.search],
  );
  const canonicalSearch = useMemo(() => buildMacroLibraryQuery(query), [query]);

  useEffect(() => {
    if (location.search.replace(/^\?/, "") !== canonicalSearch) {
      history.replace({ pathname: location.pathname, search: canonicalSearch });
    }
  }, [canonicalSearch, history, location.pathname, location.search]);

  const setQuery = useCallback(
    (updates, { replace = false } = {}) => {
      const next = updateMacroLibraryQuery(query, updates);
      const destination = {
        pathname: location.pathname,
        search: buildMacroLibraryQuery(next),
      };
      if (replace) history.replace(destination);
      else history.push(destination);
    },
    [history, location.pathname, query],
  );

  return { query, setQuery, pathname: location.pathname };
};

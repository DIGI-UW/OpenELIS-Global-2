import { useCallback } from "react";
import { useHistory, useLocation } from "react-router-dom";

/**
 * Round-trips report filter state through the URL query string (react-router v5),
 * so a filtered report is shareable, bookmarkable, and survives a page reload.
 *
 * `defaults` is an object of filterKey -> default value. A default that is an
 * Array marks a multi-value filter (serialised comma-joined); anything else is a
 * scalar. Reading: absent params fall back to the default; a present scalar comes
 * back as its string, a present array as a split list. Writing (`setUrlFilters`)
 * omits empty/blank values entirely and uses `history.push`, so each Apply is a
 * back-navigable, shareable entry (callers should write only on a deliberate
 * Apply, not on every keystroke).
 */
export function useUrlFilters(defaults) {
  const history = useHistory();
  const location = useLocation();
  const params = new URLSearchParams(location.search);

  const values = {};
  Object.keys(defaults).forEach((key) => {
    if (Array.isArray(defaults[key])) {
      const raw = params.get(key);
      values[key] = raw ? raw.split(",").filter(Boolean) : defaults[key];
    } else {
      values[key] = params.has(key) ? params.get(key) : defaults[key];
    }
  });

  // True when the URL carried at least one of our filter params — callers use
  // this to auto-run the report on load for a shared/bookmarked link.
  const hasParams = Object.keys(defaults).some((key) => params.has(key));

  const setUrlFilters = useCallback(
    (next) => {
      const out = new URLSearchParams();
      Object.keys(next).forEach((key) => {
        const v = next[key];
        if (Array.isArray(v)) {
          if (v.length > 0) out.set(key, v.join(","));
        } else if (v !== undefined && v !== null && String(v).length > 0) {
          out.set(key, String(v));
        }
      });
      history.push({ pathname: location.pathname, search: out.toString() });
    },
    [history, location.pathname],
  );

  return { values, hasParams, setUrlFilters };
}

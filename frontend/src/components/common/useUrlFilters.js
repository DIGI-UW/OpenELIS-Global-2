import { useCallback, useEffect, useRef, useState } from "react";
import { useHistory, useLocation } from "react-router-dom";

/**
 * Round-trips report filter state through the URL query string (react-router v5),
 * so a filtered report is shareable, bookmarkable, and survives a page reload.
 *
 * `defaults` is an object of filterKey -> default value. A default that is an
 * Array marks a multi-value filter (serialised comma-joined, intended for id/enum
 * lists — not free text); anything else is a scalar. Reading: absent params fall
 * back to the default; a present scalar comes back as its string, a present array
 * as a split list. Writing (`setUrlFilters`) omits empty/blank values entirely and
 * uses `history.push`, so each write is a back-navigable, shareable entry (callers
 * should write on a deliberate Apply, or from the same effect that triggers an
 * auto-fetch — not on every keystroke).
 *
 * Everything in a URL is a string, so consumers coerce as needed: `Number(v)` for
 * numeric filters (pagination), `v === "true"` for booleans. Object-shaped filters
 * (e.g. a selected patient) are flattened by the consumer to the scalar id that
 * belongs in the URL; the display object is re-derived on hydration.
 *
 * For the common "run the report once on load when the link already carried
 * filters" behaviour, pair this with {@link useUrlFilterAutoRun} rather than
 * hand-rolling the mount snapshot (see its note).
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

/**
 * Runs `run` exactly once, on mount, if `shouldRun` was true at mount — the
 * "a shared/bookmarked link already carried filters, so render its report on
 * load" behaviour for explicit-Apply reports.
 *
 * Two subtleties this encapsulates (both are easy to get wrong by hand, and
 * getting them wrong re-fetches on every Apply):
 *   - the presence is snapshotted at mount, so a later Apply pushing filters to
 *     the URL — which flips the live `shouldRun` to true — does NOT feed back in
 *     as a second run;
 *   - a ref guards against `run` identity changes re-triggering the effect.
 *
 * Auto-fetch reports (those that already query on every filter change) don't need
 * this — hydrating state from the URL makes their existing effect fire naturally.
 */
export function useUrlFilterAutoRun(shouldRun, run) {
  const [runOnMount] = useState(shouldRun);
  const ran = useRef(false);
  useEffect(() => {
    if (runOnMount && !ran.current) {
      ran.current = true;
      run();
    }
  }, [runOnMount, run]);
}

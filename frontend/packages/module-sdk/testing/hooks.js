/**
 * Doubles for `openelis_core/hooks`: no router, no server. Navigation calls
 * are recorded on `navigations`; the URL filters live in memory.
 */
import { useCallback, useState } from "react";

export const navigations = [];

export const useInAppNavigation = () =>
  useCallback(
    (href) => (event) => {
      if (event && event.preventDefault) event.preventDefault();
      navigations.push(href);
    },
    [],
  );

export const useUrlFilters = (defaults) => {
  const [values, setValues] = useState(defaults);
  const setUrlFilters = useCallback(
    (next) => setValues({ ...defaults, ...next }),
    [defaults],
  );
  return { values, hasParams: false, setUrlFilters };
};

export const useUrlFilterAutoRun = () => {};

export const testLocales = [
  {
    id: "1",
    localeCode: "en",
    displayName: "English",
    active: true,
    fallback: true,
    sortOrder: 1,
  },
];

export const useActiveLocales = () => testLocales;

export const testDomains = [
  { id: "CLINICAL", labelKey: "domain.clinical" },
  { id: "ENVIRONMENTAL", labelKey: "domain.environmental" },
  { id: "VECTOR", labelKey: "domain.vector" },
];

export const useDomains = () => testDomains;

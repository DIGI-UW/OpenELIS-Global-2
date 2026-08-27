import { useEffect, useState } from "react";
import { getFromOpenElisServer } from "../utils/Utils";

/**
 * The single client-side source for the active language list: fetches
 * GET /rest/supportedlocales/active (backed by the multi-language
 * localization mechanism, OGC-1112) so admin screens render one name/label
 * input per configured language instead of hard-coding English/French.
 * Returns an array of { id, localeCode, displayName, active, fallback,
 * sortOrder }, sorted by sortOrder.
 */
export default function useActiveLocales() {
  const [locales, setLocales] = useState([]);
  useEffect(() => {
    getFromOpenElisServer("/rest/supportedlocales/active", (response) => {
      if (Array.isArray(response)) {
        const sorted = [...response].sort(
          (a, b) => (a.sortOrder || 0) - (b.sortOrder || 0),
        );
        setLocales(sorted);
      }
    });
  }, []);
  return locales;
}

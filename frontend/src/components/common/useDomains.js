import { useEffect, useState } from "react";
import { getFromOpenElisServer } from "../utils/Utils";

/**
 * The single client-side source for the catalog domain list: fetches
 * GET /rest/domains (backed by the backend Domain enum) so tests, sample
 * types, and results all render the same options without hard-coding them.
 * Returns an array of { id, labelKey }; labelKey resolves via react-intl.
 */
export default function useDomains() {
  const [domains, setDomains] = useState([]);
  useEffect(() => {
    getFromOpenElisServer("/rest/domains", (response) => {
      if (Array.isArray(response)) {
        setDomains(response);
      }
    });
  }, []);
  return domains;
}

import React, { useEffect, useRef } from "react";
import { TextInput } from "@carbon/react";
import { getFromOpenElisServer } from "../../../utils/Utils";

/**
 * SearchField — flat type-ahead input for the LocationPicker.
 *
 * Hits the existing `/rest/storage/locations/search?q=` endpoint, which
 * returns matches across all hierarchy levels (Room/Device/Shelf/Rack)
 * with a pre-composed `hierarchicalPath` per result. The user clicks a
 * result; we hand it back via `onSelect` and the parent's reducer
 * dispatches `PRELOAD` to populate the cascading selection.
 *
 * Stateless beyond a debounce timer ref. All actual state lives in the
 * useLocationPicker reducer; this component receives `query` + `results`
 * as props and fires callbacks. That separation is the structural fix
 * for the legacy LocationFilterDropdown's prop-sync race (the
 * `// CRITICAL: Don't sync when form just closed` workarounds).
 *
 * Threshold: skip the API for queries < 2 chars (matches the legacy
 * LocationFilterDropdown threshold so the API isn't pummeled by single-
 * character typing). Debounce: 300ms.
 */

const DEBOUNCE_MS = 300;
const MIN_QUERY_LENGTH = 2;

export default function SearchField({
  query,
  results,
  onQueryChange,
  onResultsChange,
  onSelect,
}) {
  const debounceRef = useRef(null);

  useEffect(() => {
    // Reset any pending fetch when the query changes
    if (debounceRef.current) {
      clearTimeout(debounceRef.current);
      debounceRef.current = null;
    }
    if (!query || query.length < MIN_QUERY_LENGTH) {
      return undefined;
    }
    debounceRef.current = setTimeout(() => {
      getFromOpenElisServer(
        `/rest/storage/locations/search?q=${encodeURIComponent(query)}`,
        (response) => {
          onResultsChange(Array.isArray(response) ? response : []);
        },
      );
    }, DEBOUNCE_MS);
    return () => {
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
        debounceRef.current = null;
      }
    };
  }, [query, onResultsChange]);

  return (
    <div className="storage-location-picker-search">
      <TextInput
        id="storage-location-picker-search-input"
        labelText="Search for a storage location"
        placeholder="Type 2+ characters to search…"
        value={query}
        onChange={(e) => onQueryChange(e.target.value)}
      />
      {results.length > 0 && (
        <ul role="listbox" className="storage-location-picker-search-results">
          {results.map((result) => (
            <li
              key={`${result.type || "loc"}-${result.id}`}
              role="option"
              aria-selected={false}
              tabIndex={0}
              onClick={() => onSelect(result)}
              onKeyDown={(e) => {
                if (e.key === "Enter" || e.key === " ") {
                  e.preventDefault();
                  onSelect(result);
                }
              }}
            >
              {result.hierarchicalPath || result.name}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

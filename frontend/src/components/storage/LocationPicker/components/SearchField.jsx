import React, { useEffect, useRef, useState } from "react";
import { TextInput } from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../../utils/Utils";
import { LEVEL_ORDER } from "../useLocationPicker";

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
  selectedSelection = {},
}) {
  const intl = useIntl();
  const debounceRef = useRef(null);
  const requestIdRef = useRef(0);
  const [activeIndex, setActiveIndex] = useState(-1);

  const deepestSelectedLevel = LEVEL_ORDER.reduce((deepest, level) => {
    if (selectedSelection[level]?.id) return level;
    return deepest;
  }, null);
  const selectedResultId = deepestSelectedLevel
    ? String(selectedSelection[deepestSelectedLevel].id)
    : null;

  useEffect(() => {
    if (results.length === 0) {
      setActiveIndex(-1);
      return;
    }
    const selectedIndex = results.findIndex(
      (result) =>
        deepestSelectedLevel &&
        result.type === deepestSelectedLevel &&
        String(result.id) === selectedResultId,
    );
    setActiveIndex(selectedIndex >= 0 ? selectedIndex : 0);
  }, [results, deepestSelectedLevel, selectedResultId]);

  useEffect(() => {
    const requestId = ++requestIdRef.current;
    if (debounceRef.current) {
      clearTimeout(debounceRef.current);
      debounceRef.current = null;
    }
    if (!query || query.length < MIN_QUERY_LENGTH) {
      // Clear stale results only if non-empty. Calling
      // onResultsChange unconditionally would loop: the parent
      // re-renders, passes a new onResultsChange identity, this
      // effect re-fires, and so on.
      if (results.length > 0) onResultsChange([]);
      return undefined;
    }
    debounceRef.current = setTimeout(() => {
      getFromOpenElisServer(
        `/rest/storage/locations/search?q=${encodeURIComponent(query)}`,
        (response) => {
          if (requestId !== requestIdRef.current) {
            return;
          }
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query, onResultsChange]);

  return (
    <div className="storage-location-picker-search">
      <TextInput
        id="storage-location-picker-search-input"
        labelText={intl.formatMessage({
          id: "storage.search.location.label",
          defaultMessage: "Search for a storage location",
        })}
        placeholder={intl.formatMessage({
          id: "storage.search.location.placeholder",
          defaultMessage: "Type 2+ characters to search…",
        })}
        value={query}
        onChange={(e) => onQueryChange(e.target.value)}
        role="combobox"
        aria-controls="storage-location-picker-search-results"
        aria-expanded={results.length > 0}
        aria-activedescendant={
          activeIndex >= 0
            ? `storage-location-search-option-${activeIndex}`
            : undefined
        }
        onKeyDown={(e) => {
          if (results.length === 0) return;
          if (e.key === "ArrowDown") {
            e.preventDefault();
            setActiveIndex((prev) => Math.min(prev + 1, results.length - 1));
            return;
          }
          if (e.key === "ArrowUp") {
            e.preventDefault();
            setActiveIndex((prev) => Math.max(prev - 1, 0));
            return;
          }
          if ((e.key === "Enter" || e.key === " ") && activeIndex >= 0) {
            e.preventDefault();
            const selected = results[activeIndex];
            onSelect(selected);
            onQueryChange(selected.hierarchicalPath || selected.name || "");
            onResultsChange([]);
          }
        }}
      />
      {results.length > 0 && (
        <ul
          id="storage-location-picker-search-results"
          role="listbox"
          className="storage-location-picker-search-results"
        >
          {results.map((result, index) => {
            const optionId = `storage-location-search-option-${index}`;
            const isSelected = Boolean(
              deepestSelectedLevel &&
              result.type === deepestSelectedLevel &&
              String(result.id) === selectedResultId,
            );
            // Canonical ARIA 1.2 combobox pattern: the input is the
            // sole keyboard tab stop; list options are not tab-reachable.
            // Navigation is via arrow keys on the input, selection is
            // surfaced via aria-activedescendant. Mouse users get onClick.
            const parts = (result.hierarchicalPath || result.name || "").split(
              " › ",
            );
            const depth = parts.length - 1;
            return (
              <li
                id={optionId}
                key={`${result.type || "loc"}-${result.id || index}-${index}`}
                role="option"
                aria-selected={isSelected}
                tabIndex={-1}
                className={`storage-search-result depth-${depth}`}
                style={{ paddingLeft: `${0.75 + depth * 1}rem` }}
                onMouseEnter={() => setActiveIndex(index)}
                onClick={() => {
                  onSelect(result);
                  onQueryChange(result.hierarchicalPath || result.name || "");
                  onResultsChange([]);
                }}
              >
                {depth > 0 && (
                  <span className="storage-search-result-ancestors">
                    {parts.slice(0, -1).join(" › ")} ›{" "}
                  </span>
                )}
                <span className="storage-search-result-leaf">
                  {parts[parts.length - 1]}
                </span>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}

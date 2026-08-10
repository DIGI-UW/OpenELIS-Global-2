import React, { useEffect, useRef, useState } from "react";
import { InlineLoading, TextInput } from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../../utils/Utils";
import { LEVEL_ORDER } from "../useLocationPicker";

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
  const [status, setStatus] = useState("idle");
  // The parent passes onResultsChange as an inline arrow, so its identity
  // changes every render. Depending on it re-runs the search effect on every
  // render, and any state update in that effect then loops forever.
  const onResultsChangeRef = useRef(onResultsChange);
  useEffect(() => {
    onResultsChangeRef.current = onResultsChange;
  }, [onResultsChange]);

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
      setStatus("idle");
      onResultsChangeRef.current([]);
      return undefined;
    }
    setStatus("loading");
    debounceRef.current = setTimeout(() => {
      try {
        getFromOpenElisServer(
          `/rest/storage/locations/search?q=${encodeURIComponent(query)}`,
          (response) => {
            if (requestId !== requestIdRef.current) {
              return;
            }
            if (response === null || response === undefined) {
              setStatus("error");
              onResultsChangeRef.current([]);
              return;
            }
            setStatus("loaded");
            onResultsChangeRef.current(Array.isArray(response) ? response : []);
          },
        );
      } catch (e) {
        if (requestId === requestIdRef.current) {
          setStatus("error");
          onResultsChangeRef.current([]);
        }
      }
    }, DEBOUNCE_MS);
    return () => {
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
        debounceRef.current = null;
      }
    };
  }, [query]);

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
          if (e.key === "Enter" && activeIndex >= 0) {
            e.preventDefault();
            onSelect(results[activeIndex]);
          }
        }}
      />
      {query.length > 0 && query.length < MIN_QUERY_LENGTH && (
        <p className="storage-location-picker-search-status">
          {intl.formatMessage({
            id: "storage.search.location.minLength",
            defaultMessage: "Keep typing — at least 2 characters",
          })}
        </p>
      )}
      {status === "loading" && (
        <div className="storage-location-picker-search-status">
          <InlineLoading
            description={intl.formatMessage({
              id: "storage.search.location.searching",
              defaultMessage: "Searching…",
            })}
          />
        </div>
      )}
      {status === "error" && (
        <p
          className="storage-location-picker-search-status storage-location-picker-search-error"
          role="alert"
        >
          {intl.formatMessage({
            id: "storage.search.location.error",
            defaultMessage: "Could not search locations. Please try again.",
          })}
        </p>
      )}
      {status === "loaded" && results.length === 0 && (
        <p className="storage-location-picker-search-status">
          {intl.formatMessage(
            {
              id: "storage.search.location.noResults",
              defaultMessage: 'No storage locations match "{query}"',
            },
            { query },
          )}
        </p>
      )}
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
            return (
              <li
                id={optionId}
                key={`${result.type || "loc"}-${result.id || index}-${index}`}
                role="option"
                aria-selected={isSelected}
                tabIndex={-1}
                onMouseEnter={() => setActiveIndex(index)}
                onClick={() => onSelect(result)}
              >
                {result.hierarchicalPath || result.name}
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}

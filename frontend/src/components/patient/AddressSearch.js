import React, { useState, useEffect, useCallback, useRef } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { TextInput, Layer, Tag } from "@carbon/react";
import { Search, Close, ChevronRight } from "@carbon/icons-react";
import { getFromOpenElisServer } from "../utils/Utils";
import "./AddressSearch.css";

/**
 * AddressSearch component - Provides a search-based address selection similar to OpenMRS.
 * Searches across all hierarchy levels and auto-populates all address fields when a result is selected.
 *
 * Props:
 * - onAddressSelect: function(hierarchyLevels) - Callback when an address is selected
 *   hierarchyLevels is an array of {level, id, name} objects
 * - addressHierarchyLevels: array - The configured hierarchy levels for labeling
 * - placeholder: string - Optional placeholder text
 * - disabled: boolean - Whether the search is disabled
 */
const AddressSearch = ({
  onAddressSelect,
  addressHierarchyLevels = [],
  placeholder,
  disabled = false,
}) => {
  const intl = useIntl();
  const [searchTerm, setSearchTerm] = useState("");
  const [searchResults, setSearchResults] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isOpen, setIsOpen] = useState(false);
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
  const containerRef = useRef(null);
  const inputRef = useRef(null);

  // Debounced search
  const performSearch = useCallback((term) => {
    if (!term || term.trim().length < 2) {
      setSearchResults([]);
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    getFromOpenElisServer(
      `/rest/address-hierarchy/search?query=${encodeURIComponent(term)}&limit=20`,
      (results) => {
        const resultsArray = Array.isArray(results) ? results : [];
        setSearchResults(resultsArray);
        setIsLoading(false);
        setIsOpen(resultsArray.length > 0);
      },
    );
  }, []);

  // Debounce effect
  useEffect(() => {
    if (searchTerm && searchTerm.length >= 2) {
      const timer = setTimeout(() => {
        performSearch(searchTerm);
      }, 300);
      return () => clearTimeout(timer);
    } else {
      setSearchResults([]);
      setIsOpen(false);
    }
  }, [searchTerm, performSearch]);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target)
      ) {
        setIsOpen(false);
        setHighlightedIndex(-1);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleSelect = (result) => {
    if (onAddressSelect && result.hierarchyLevels) {
      onAddressSelect(result.hierarchyLevels);
    }
    setSearchTerm("");
    setSearchResults([]);
    setIsOpen(false);
    setHighlightedIndex(-1);
  };

  const handleKeyDown = (e) => {
    if (!isOpen || searchResults.length === 0) return;

    switch (e.key) {
      case "ArrowDown":
        e.preventDefault();
        setHighlightedIndex((prev) =>
          prev < searchResults.length - 1 ? prev + 1 : 0,
        );
        break;
      case "ArrowUp":
        e.preventDefault();
        setHighlightedIndex((prev) =>
          prev > 0 ? prev - 1 : searchResults.length - 1,
        );
        break;
      case "Enter":
        e.preventDefault();
        if (highlightedIndex >= 0 && highlightedIndex < searchResults.length) {
          handleSelect(searchResults[highlightedIndex]);
        }
        break;
      case "Escape":
        setIsOpen(false);
        setHighlightedIndex(-1);
        break;
      default:
        break;
    }
  };

  const handleClear = () => {
    setSearchTerm("");
    setSearchResults([]);
    setIsOpen(false);
    setHighlightedIndex(-1);
    inputRef.current?.focus();
  };

  const getLevelName = (levelNum) => {
    const level = addressHierarchyLevels.find((l) => l.level === levelNum);
    return level ? level.typeName : `Level ${levelNum}`;
  };

  const defaultPlaceholder = intl.formatMessage({
    id: "address.search.placeholder",
    defaultMessage: "Search for address (type at least 2 characters)...",
  });

  return (
    <div className="address-search-container" ref={containerRef}>
      <div className="address-search-input-wrapper">
        <TextInput
          ref={inputRef}
          id="address-search-input"
          type="text"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          onKeyDown={handleKeyDown}
          onFocus={() => searchResults.length > 0 && setIsOpen(true)}
          placeholder={placeholder || defaultPlaceholder}
          labelText={intl.formatMessage({
            id: "address.search.label",
            defaultMessage: "Quick Address Search",
          })}
          disabled={disabled}
          autoComplete="off"
        />
        <div className="address-search-icons">
          {isLoading ? (
            <div className="address-search-spinner" />
          ) : searchTerm ? (
            <button
              type="button"
              className="address-search-clear"
              onClick={handleClear}
              aria-label="Clear search"
            >
              <Close size={16} />
            </button>
          ) : (
            <Search size={16} className="address-search-icon" />
          )}
        </div>
      </div>

      {isOpen && searchResults.length > 0 && (
        <Layer className="address-search-dropdown">
          <ul
            className="address-search-results"
            role="listbox"
            aria-label="Address search results"
          >
            {searchResults.map((result, index) => (
              <li
                key={result.id}
                role="option"
                aria-selected={highlightedIndex === index}
                className={`address-search-result-item ${highlightedIndex === index ? "highlighted" : ""}`}
                onClick={() => handleSelect(result)}
                onMouseEnter={() => setHighlightedIndex(index)}
              >
                <div className="address-result-path">
                  {result.hierarchyLevels &&
                    result.hierarchyLevels.map((level, levelIndex) => (
                      <span key={level.id} className="address-level-segment">
                        <Tag
                          type="gray"
                          size="sm"
                          className="address-level-tag"
                        >
                          {getLevelName(level.level)}
                        </Tag>
                        <span className="address-level-name">{level.name}</span>
                        {levelIndex < result.hierarchyLevels.length - 1 && (
                          <ChevronRight
                            size={12}
                            className="address-level-separator"
                          />
                        )}
                      </span>
                    ))}
                </div>
              </li>
            ))}
          </ul>
          <div className="address-search-footer">
            <FormattedMessage
              id="address.search.results.count"
              defaultMessage="{count} results found"
              values={{ count: searchResults.length }}
            />
          </div>
        </Layer>
      )}

      {isOpen && !isLoading && searchResults.length === 0 && searchTerm && (
        <Layer className="address-search-dropdown">
          <div className="address-search-no-results">
            <FormattedMessage
              id="address.search.no.results"
              defaultMessage="No addresses found for '{term}'"
              values={{ term: searchTerm }}
            />
          </div>
        </Layer>
      )}
    </div>
  );
};

export default AddressSearch;

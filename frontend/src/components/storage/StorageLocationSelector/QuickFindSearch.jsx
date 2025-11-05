import React, { useState, useEffect, useCallback } from "react";
import { ComboBox } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import "./QuickFindSearch.css";

/**
 * Quick-find search component for rapidly finding existing locations
 * Type-ahead autocomplete matching Room/Device/Shelf/Rack levels, displays full hierarchical paths
 *
 * Props:
 * - onLocationSelect: function - Callback when location is selected
 * - debounceMs: number - Debounce delay in milliseconds (default: 300)
 */
const QuickFindSearch = ({ onLocationSelect, debounceMs = 300 }) => {
  const intl = useIntl();
  const [searchTerm, setSearchTerm] = useState("");
  const [searchResults, setSearchResults] = useState([]);
  const [isLoading, setIsLoading] = useState(false);

  // Debounced search function
  const performSearch = useCallback((term) => {
    if (!term || term.trim().length < 2) {
      setSearchResults([]);
      return;
    }

    setIsLoading(true);
    getFromOpenElisServer(
      `/rest/storage/locations/search?q=${encodeURIComponent(term)}`,
      (results) => {
        const filteredResults = results || [];
        setSearchResults(filteredResults);
        setIsLoading(false);
      },
      () => {
        setSearchResults([]);
        setIsLoading(false);
      },
    );
  }, []);

  // Debounce effect
  useEffect(() => {
    const timer = setTimeout(() => {
      performSearch(searchTerm);
    }, debounceMs);

    return () => clearTimeout(timer);
  }, [searchTerm, debounceMs, performSearch]);

  const handleInputChange = (event) => {
    // Carbon ComboBox onInputChange provides { inputValue } object
    const inputValue =
      event?.inputValue !== undefined
        ? event.inputValue
        : event?.target?.value || "";
    setSearchTerm(inputValue);
  };

  const handleSelectionChange = ({ selectedItem }) => {
    if (selectedItem && onLocationSelect) {
      onLocationSelect(selectedItem);
      // Clear search after selection
      setSearchTerm("");
      setSearchResults([]);
    }
  };

  return (
    <div className="quick-find-search" data-testid="quick-find-search">
      <ComboBox
        id="quick-find-location-search"
        placeholder={intl.formatMessage({
          id: "storage.quick.find.placeholder",
          defaultMessage: "Search for location...",
        })}
        items={searchResults}
        itemToString={(item) =>
          item ? item.hierarchicalPath || item.name || "" : ""
        }
        onChange={handleSelectionChange}
        onInputChange={handleInputChange}
        inputValue={searchTerm}
        loading={isLoading}
        shouldFilterItem={() => false} // Results already filtered by API
      />
    </div>
  );
};

export default QuickFindSearch;

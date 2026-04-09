import React, { useState, useRef, useCallback, useEffect } from "react";
import { ComboBox } from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";

/**
 * Autocomplete/type-ahead mode for storage location selection
 * Uses Carbon ComboBox for searchable selection
 */
const AutocompleteMode = ({ onLocationChange }) => {
  const intl = useIntl();
  const [searchResults, setSearchResults] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const debounceTimer = useRef(null);
  const isMounted = useRef(true);

  useEffect(() => {
    isMounted.current = true;
    return () => {
      isMounted.current = false;
      if (debounceTimer.current) {
        clearTimeout(debounceTimer.current);
      }
    };
  }, []);

  const handleSearch = useCallback((inputValue) => {
    if (debounceTimer.current) {
      clearTimeout(debounceTimer.current);
    }

    if (!inputValue || inputValue.length < 2) {
      setSearchResults([]);
      setIsLoading(false);
      return;
    }

    debounceTimer.current = setTimeout(() => {
      setIsLoading(true);
      getFromOpenElisServer(
        `/rest/storage/locations/search?q=${encodeURIComponent(inputValue)}`,
        (response) => {
          if (!isMounted.current) return;
          setIsLoading(false);
          if (response && Array.isArray(response)) {
            setSearchResults(response);
          } else {
            setSearchResults([]);
          }
        },
      );
    }, 300);
  }, []);

  return (
    <div className="autocomplete-container">
      <ComboBox
        id="location-search"
        titleText={intl.formatMessage({
          id: "storage.location.label",
          defaultMessage: "Storage Location",
        })}
        placeholder={intl.formatMessage({
          id: "storage.searchLocation.placeholder",
          defaultMessage: "Search for location...",
        })}
        items={searchResults}
        itemToString={(item) =>
          item ? item.hierarchicalPath || item.name : ""
        }
        onChange={({ selectedItem }) =>
          onLocationChange && onLocationChange(selectedItem)
        }
        onInputChange={handleSearch}
        disabled={isLoading}
      />
    </div>
  );
};

export default AutocompleteMode;

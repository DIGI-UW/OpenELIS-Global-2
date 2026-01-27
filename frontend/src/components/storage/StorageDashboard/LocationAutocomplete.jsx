import React, { useState, useEffect, useCallback } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import "./LocationAutocomplete.css";

/**
 * LocationAutocomplete component for flat list search results with full hierarchical paths
 * Used in LocationFilterDropdown for autocomplete search mode
 *
 * Props:
 * - onLocationSelect: function - Callback when location is selected
 * - searchTerm: string - Current search term
 * - onSearchTermChange: function - Callback when search term changes
 * - allowInactive: boolean - Allow selection of inactive locations (default: false)
 */
const LocationAutocomplete = ({
  onLocationSelect,
  searchTerm,
  onSearchTermChange,
  allowInactive = false,
}) => {
  const intl = useIntl();
  const [searchResults, setSearchResults] = useState([]);
  const [isLoading, setIsLoading] = useState(false);

  // Perform search when searchTerm changes
  const performSearch = useCallback((term) => {
    if (!term || term.trim().length < 2) {
      setSearchResults([]);
      return;
    }

    setIsLoading(true);
    getFromOpenElisServer(
      `/rest/storage/locations/search?q=${encodeURIComponent(term)}`,
      (results) => {
        // Ensure results is an array - API might return non-array
        const resultsArray = Array.isArray(results) ? results : [];
        // Filter out position-level locations (only Room/Device/Shelf/Rack allowed)
        const filteredResults = resultsArray.filter(
          (location) => location && location.type !== "position",
        );

        setSearchResults(filteredResults);
        setIsLoading(false);
      },
      () => {
        setSearchResults([]);
        setIsLoading(false);
      },
    );
  }, []);

  // Debounce search - trigger when searchTerm prop changes
  useEffect(() => {
    if (searchTerm) {
      const timer = setTimeout(() => {
        performSearch(searchTerm);
      }, 300);

      return () => clearTimeout(timer);
    } else {
      setSearchResults([]);
    }
  }, [searchTerm, performSearch]);

  const handleSelectionChange = ({ selectedItem }) => {
    if (selectedItem && onLocationSelect) {
      onLocationSelect(selectedItem);
    }
  };

  return (
    <div className="location-autocomplete" data-testid="location-autocomplete">
      {isLoading && (
        <div className="loading-indicator">
          <FormattedMessage id="label.loading" defaultMessage="Loading..." />
        </div>
      )}
      {!isLoading && searchResults.length === 0 && searchTerm && (
        <div className="no-results">
          <FormattedMessage
            id="storage.location.no.results"
            defaultMessage="No locations found"
          />
        </div>
      )}
      {searchResults.length > 0 && (
        <ul className="location-autocomplete-results">
          {searchResults.map((location) => {
            const isInactive = location.active === false;
            const canSelect = allowInactive || !isInactive;
            return (
              <li
                key={location.id}
                className={`location-autocomplete-item ${isInactive ? "inactive" : ""} ${!canSelect ? "disabled" : ""}`}
                onClick={() =>
                  canSelect && handleSelectionChange({ selectedItem: location })
                }
                style={{
                  cursor: canSelect ? "pointer" : "not-allowed",
                  opacity: canSelect ? 1 : 0.5,
                }}
              >
                <div className="location-path">
                  {location.hierarchical_path ||
                    location.hierarchicalPath ||
                    location.name ||
                    location.label ||
                    ""}
                </div>
                {location.type && (
                  <div className="location-type">{location.type}</div>
                )}
                {isInactive && <div className="inactive-badge">(Inactive)</div>}
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
};

export default LocationAutocomplete;

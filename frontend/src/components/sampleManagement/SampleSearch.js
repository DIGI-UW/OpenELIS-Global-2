import React, { useState, useCallback, useRef, useEffect } from "react";
import { Search, Loading } from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";

/**
 * SampleSearch - Search component for finding sample items by accession number.
 *
 * Features:
 * - Debounced search input (300ms delay)
 * - Loading state while fetching results
 * - Error handling with callback
 * - React Intl for internationalization
 *
 * Props:
 * - onSearchResults: (response, error) => void - callback with search results or error
 * - includeTests: boolean - whether to load ordered tests with sample items
 *
 * Related: Feature 001-sample-management, User Story 1, Task T033
 */
function SampleSearch({ onSearchResults, includeTests = false }) {
  const intl = useIntl();
  const [searchValue, setSearchValue] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const debounceTimerRef = useRef(null);

  // Cleanup debounce timer on unmount
  useEffect(() => {
    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
    };
  }, []);

  /**
   * Perform the actual search API call.
   */
  const performSearch = useCallback(
    (accessionNumber) => {
      if (!accessionNumber || accessionNumber.trim() === "") {
        // Clear results when search is empty
        onSearchResults(null, null);
        setIsLoading(false);
        return;
      }

      setIsLoading(true);

      const endpoint = `/rest/sample-management/search?accessionNumber=${encodeURIComponent(
        accessionNumber.trim(),
      )}&includeTests=${includeTests}`;

      getFromOpenElisServer(endpoint, (response) => {
        setIsLoading(false);

        if (response) {
          // Successful response
          onSearchResults(response, null);
        } else {
          // Error or no data
          onSearchResults(null, {
            message: intl.formatMessage({
              id: "sample.management.search.error.general",
            }),
          });
        }
      });
    },
    [includeTests, onSearchResults, intl],
  );

  /**
   * Handle search input change with debouncing.
   */
  const handleSearchChange = (event) => {
    const newValue = event.target.value;
    setSearchValue(newValue);

    // Clear previous timer
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
    }

    // Set new timer for debounced search (300ms delay)
    debounceTimerRef.current = setTimeout(() => {
      performSearch(newValue);
    }, 300);
  };

  /**
   * Handle search clear (X button clicked).
   */
  const handleClearSearch = () => {
    setSearchValue("");
    setIsLoading(false);

    // Clear any pending debounced search
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
    }

    // Clear results
    onSearchResults(null, null);
  };

  return (
    <div style={{ position: "relative" }}>
      <Search
        id="sample-search-input"
        labelText={intl.formatMessage({
          id: "sample.management.search.label",
        })}
        placeholder={intl.formatMessage({
          id: "sample.management.search.placeholder",
        })}
        value={searchValue}
        onChange={handleSearchChange}
        onClear={handleClearSearch}
        disabled={isLoading}
        size="lg"
      />
      {isLoading && (
        <div
          style={{
            position: "absolute",
            right: "40px",
            top: "50%",
            transform: "translateY(-50%)",
          }}
        >
          <Loading
            small
            withOverlay={false}
            description={intl.formatMessage({
              id: "sample.management.search.loading",
            })}
          />
        </div>
      )}
    </div>
  );
}

export default SampleSearch;

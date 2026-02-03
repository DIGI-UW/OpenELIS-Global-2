import React, { useState, useRef, useEffect } from "react";
import { ComboBox } from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";

const AutocompleteMode = ({ onLocationChange }) => {
  const intl = useIntl();
  const [searchResults, setSearchResults] = useState([]);
  const debounceTimer = useRef(null);
  const abortControllerRef = useRef(null);

  useEffect(() => {
    return () => {
      if (debounceTimer.current) clearTimeout(debounceTimer.current);
      if (abortControllerRef.current) abortControllerRef.current.abort();
    };
  }, []);

  const handleSearch = (inputValue) => {
    if (debounceTimer.current) clearTimeout(debounceTimer.current);
    if (abortControllerRef.current) abortControllerRef.current.abort();

    if (!inputValue) {
      setSearchResults([]);
      return;
    }

    debounceTimer.current = setTimeout(() => {
      abortControllerRef.current = new AbortController();
      const signal = abortControllerRef.current.signal;

      getFromOpenElisServer(
        `/rest/storage/devices/search?q=${encodeURIComponent(inputValue)}`,
        (data) => {
          if (!signal.aborted) {
            const formattedResults = (data || []).map((item) => ({
              ...item,
              displayPath: item.parentRoomName
                ? `${item.parentRoomName} > ${item.name}`
                : item.name,
            }));
            setSearchResults(formattedResults);
          }
        },
        signal,
        (error) => {
          if (error.name !== "AbortError") {
            console.error("Error searching storage devices:", error);
            setSearchResults([]);
          }
        },
      );
    }, 500);
  };

  return (
    <div className="autocomplete-container">
      <ComboBox
        id="location-search"
        titleText={intl.formatMessage({ id: "storage.location.label" })}
        placeholder={intl.formatMessage({
          id: "storage.location.search.placeholder",
        })}
        items={searchResults}
        itemToString={(item) => (item ? item.displayPath : "")}
        onChange={({ selectedItem }) =>
          onLocationChange && onLocationChange(selectedItem)
        }
        onInputChange={handleSearch}
      />
    </div>
  );
};

export default AutocompleteMode;

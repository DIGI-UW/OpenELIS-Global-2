import React, { useState, useRef, useEffect } from "react";
import { ComboBox } from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";

const DEBOUNCE_MS = 500;

const AutocompleteMode = ({ onLocationChange = () => {} }) => {
  const intl = useIntl();
  const [searchResults, setSearchResults] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const debounceTimer = useRef(null);
  const abortControllerRef = useRef(null);

  useEffect(() => {
    return () => {
      if (debounceTimer.current) clearTimeout(debounceTimer.current);
      if (abortControllerRef.current) abortControllerRef.current.abort();
    };
  }, []);

  const cancelPending = () => {
    if (debounceTimer.current) clearTimeout(debounceTimer.current);
    if (abortControllerRef.current) abortControllerRef.current.abort();
  };

  const handleSearch = (inputValue) => {
    cancelPending();

    if (!inputValue?.trim()) {
      setSearchResults([]);
      setIsLoading(false);
      return;
    }

    setIsLoading(true);

    debounceTimer.current = setTimeout(() => {
      const controller = new AbortController();
      abortControllerRef.current = controller;
      const { signal } = controller;

      getFromOpenElisServer(
        `/rest/storage/devices/search?q=${encodeURIComponent(inputValue.trim())}`,
        (data) => {
          if (signal.aborted) return;

          const formattedResults = (data || []).map((item) => ({
            ...item,
            displayPath: item.parentRoomName
              ? `${item.parentRoomName} > ${item.name}`
              : item.name,
          }));

          setSearchResults(formattedResults);
          setIsLoading(false);
        },
        signal,
      );
    }, DEBOUNCE_MS);
  };

  const handleChange = ({ selectedItem }) => {
    onLocationChange(selectedItem);
  };

  return (
    <div className="autocomplete-container">
      <ComboBox
        id="location-search"
        titleText={intl.formatMessage({
          id: "storage.location.label",
          defaultMessage: "Location",
        })}
        placeholder={intl.formatMessage({
          id: "storage.location.search.placeholder",
          defaultMessage: "Search storage locations",
        })}
        items={searchResults}
        itemToString={(item) => (item ? item.displayPath : "")}
        onChange={handleChange}
        onInputChange={handleSearch}
        loading={isLoading}
      />
    </div>
  );
};

export default AutocompleteMode;

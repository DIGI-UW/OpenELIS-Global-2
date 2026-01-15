import React, { useState, useRef, useEffect } from "react";
import { ComboBox } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

/**
 * Autocomplete/type-ahead mode for storage location selection
 * Uses Carbon ComboBox for searchable selection
 */
const AutocompleteMode = ({ onLocationChange }) => {
  const intl = useIntl();
  const [searchResults, setSearchResults] = useState([]);
  const debounceTimer = useRef(null);

  useEffect(() => {
    return () => {
      if(debounceTimer.current){
        clearTimeout(debounceTimer.current);
      }
    };

  }, []);

  const handleSearch = (inputValue) => {

    if(debounceTimer.current){
      clearTimeout(debounceTimer.current);
    }

    if (!inputValue) {
      setSearchResults([]);
      return;
    }

    debounceTimer.current = setTimeout(() => {
      fetch(`/rest/storage/devices/search?q=${encodeURIComponent(inputValue)}`)
      .then((response) => response.json())
      .then((data) => {
        
        const formattedResults = data.map((item) => ({
          ...item,
          hierarchicalPath: item.hierarchicalPath || item.name,
        }));
        
        setSearchResults(formattedResults);
      })
      
      .catch((error) => {
        setSearchResults([]);
      });

    }, 500);

    
  };

  return (
    <div className="autocomplete-container">
      <ComboBox
        id="location-search"
        titleText={intl.formatMessage({ id: "storage.location.label" })}
        placeholder="Search for location..."
        items={searchResults}
        itemToString={(item) => (item ? item.hierarchicalPath : "")}
        onChange={({ selectedItem }) =>
          onLocationChange && onLocationChange(selectedItem)
        }
        onInputChange={handleSearch}
      />
    </div>
  );
};

export default AutocompleteMode;

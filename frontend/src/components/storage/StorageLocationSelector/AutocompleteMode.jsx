import React, { useState } from "react";
import { ComboBox } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

/**
 * Autocomplete/type-ahead mode for storage location selection
 * Uses Carbon ComboBox for searchable selection
 */
const AutocompleteMode = ({ onLocationChange }) => {
  const intl = useIntl();
  const [searchResults, setSearchResults] = useState([]);

  const handleSearch = (inputValue) => {

    if (!inputValue) {
      setSearchResults([]);
      return;
    }

    fetch(`/rest/storage/devices/search?q=${inputValue}`)
      .then((response) => response.json())
      .then((data) => {
        
        const formattedResults = data.map((item) => ({
          ...item,
          hierarchicalPath: item.hierarchicalPath || item.name,
        }));
        
        setSearchResults(formattedResults);
      })
      
      .catch((error) => {
        console.error("Error fetching storage locations:", error);
        setSearchResults([]);
      });
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

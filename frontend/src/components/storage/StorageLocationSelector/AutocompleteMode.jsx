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
    // TODO: Implement search API call
    // For now, empty implementation
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

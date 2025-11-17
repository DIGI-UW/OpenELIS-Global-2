import React, { useState, useEffect, useRef } from "react";
import { TextInput } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import LocationTreeView from "./LocationTreeView";
import LocationAutocomplete from "./LocationAutocomplete";
import "./LocationFilterDropdown.css";

/**
 * LocationFilterDropdown component - Single location dropdown with tree view and autocomplete
 * Supports Room, Device, Shelf, and Rack levels (Position excluded per FR-065b)
 *
 * Combination mode: tree view for hierarchical browsing and autocomplete for search
 * Shows tree view when not searching, autocomplete when typing
 *
 * Props:
 * - onLocationChange: function - Callback when location is selected, receives { id, type, name, ... }
 * - selectedLocation: object - Currently selected location (optional)
 * - allowInactive: boolean - Allow selection of inactive locations (default: false, true for filter dropdown)
 * - placeholder: string - Custom placeholder text (default: "Filter by locations...")
 */
const LocationFilterDropdown = ({
  onLocationChange,
  selectedLocation,
  allowInactive = false,
  placeholder,
  showSelectedDisplay = true, // Show "Selected:" text by default, can be hidden
}) => {
  const intl = useIntl();
  const [searchTerm, setSearchTerm] = useState("");
  const [showAutocomplete, setShowAutocomplete] = useState(false);
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef(null);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
        setShowAutocomplete(false);
      }
    };

    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isOpen]);

  const handleLocationSelect = (location) => {
    // DEBUG: Log what's being passed to parent - log the FULL object
    if (process.env.NODE_ENV === "development") {
      console.log(
        "[LocationFilterDropdown] handleLocationSelect called with:",
        location,
      );
    }

    if (onLocationChange) {
      onLocationChange(location);
    }
    // Clear search after selection
    setSearchTerm("");
    setShowAutocomplete(false);
    setIsOpen(false);
  };

  const handleSearchTermChange = (event) => {
    const term = event?.target?.value || "";
    setSearchTerm(term);
    // Show autocomplete when user starts typing (2+ characters)
    if (term && term.trim().length >= 2) {
      setShowAutocomplete(true);
      setIsOpen(true);
    } else {
      setShowAutocomplete(false);
    }
  };

  const displayValue = selectedLocation
    ? selectedLocation.name ||
      selectedLocation.label ||
      selectedLocation.hierarchical_path ||
      ""
    : intl.formatMessage({
        id: "storage.filter.select.location",
        defaultMessage: "Select location...",
      });

  return (
    <div
      className="location-filter-dropdown"
      data-testid="location-filter-dropdown"
      ref={dropdownRef}
    >
      {/* Search input - triggers autocomplete when typing */}
      <TextInput
        id="location-filter-search"
        labelText=""
        hideLabel
        placeholder={
          placeholder ||
          intl.formatMessage({
            id: "storage.filter.by.locations.placeholder",
            defaultMessage: "Filter by locations...",
          })
        }
        value={searchTerm}
        onChange={handleSearchTermChange}
        onFocus={() => setIsOpen(true)}
        className="location-filter-search-input"
      />

      {/* Dropdown content - shown when open */}
      {isOpen && (
        <div className="location-filter-dropdown-content">
          {/* Show autocomplete when searching, tree view otherwise */}
          {showAutocomplete ? (
            <div
              className="location-filter-autocomplete-container"
              data-testid="location-autocomplete-container"
            >
              <LocationAutocomplete
                onLocationSelect={handleLocationSelect}
                searchTerm={searchTerm}
                onSearchTermChange={setSearchTerm}
                allowInactive={allowInactive}
              />
            </div>
          ) : (
            <div
              className="location-filter-tree-container"
              data-testid="location-tree-container"
            >
              <LocationTreeView
                onLocationSelect={handleLocationSelect}
                allowInactive={allowInactive}
              />
            </div>
          )}
        </div>
      )}

      {/* Show selected location display when not open - only if showSelectedDisplay prop is true */}
      {selectedLocation && !isOpen && showSelectedDisplay !== false && (
        <div className="selected-location-display">
          <FormattedMessage
            id="storage.filter.selected"
            defaultMessage="Selected:"
          />{" "}
          {selectedLocation.name ||
            selectedLocation.label ||
            selectedLocation.id}
        </div>
      )}
    </div>
  );
};

export default LocationFilterDropdown;

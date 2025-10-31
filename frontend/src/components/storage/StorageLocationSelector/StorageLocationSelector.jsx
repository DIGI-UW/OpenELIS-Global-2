import React, { useState } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import CascadingDropdownMode from './CascadingDropdownMode';
import AutocompleteMode from './AutocompleteMode';
import BarcodeScanMode from './BarcodeScanMode';
import './StorageLocationSelector.css';

/**
 * Main Storage Location Selector Widget
 * Supports three input modes: dropdown, autocomplete, barcode
 * 
 * Props:
 * - mode: 'dropdown' | 'autocomplete' | 'barcode'
 * - onLocationChange: callback when location selected
 * - enableInlineCreation: boolean to show "Add New" buttons
 * - optional: boolean - can be left blank
 */
const StorageLocationSelector = ({ 
  mode = 'dropdown', 
  onLocationChange,
  enableInlineCreation = false,
  optional = true 
}) => {
  const intl = useIntl();
  const [selectedLocation, setSelectedLocation] = useState(null);
  const [hierarchicalPath, setHierarchicalPath] = useState('');

  const handleLocationChange = (location) => {
    setSelectedLocation(location);
    
    // Build hierarchical path
    if (location && location.position) {
      const path = `${location.room?.name} > ${location.device?.name} > ${location.shelf?.label} > ${location.rack?.label} > Position ${location.position.coordinate}`;
      setHierarchicalPath(path);
    }

    if (onLocationChange) {
      onLocationChange(location);
    }
  };

  const handleBarcodeScanned = (barcode) => {
    // TODO: Parse barcode and fetch location
    console.log('Barcode scanned:', barcode);
  };

  return (
    <div className="storage-location-selector" data-testid="storage-location-selector">
      <div className="selector-header">
        <h4>
          <FormattedMessage id="storage.location.label" />
          {optional && <span className="optional-indicator"> (optional)</span>}
        </h4>
      </div>

      <div className="selector-content">
        {mode === 'dropdown' && (
          <CascadingDropdownMode
            onLocationChange={handleLocationChange}
            enableInlineCreation={enableInlineCreation}
          />
        )}

        {mode === 'autocomplete' && (
          <AutocompleteMode onLocationChange={handleLocationChange} />
        )}

        {mode === 'barcode' && (
          <BarcodeScanMode onLocationScanned={handleBarcodeScanned} />
        )}
      </div>

      {hierarchicalPath && (
        <div className="hierarchical-path" data-testid="location-path">
          <span className="path-label">
            <FormattedMessage id="storage.hierarchical.path" />:
          </span>
          <span className="path-value">{hierarchicalPath}</span>
        </div>
      )}
    </div>
  );
};

export default StorageLocationSelector;


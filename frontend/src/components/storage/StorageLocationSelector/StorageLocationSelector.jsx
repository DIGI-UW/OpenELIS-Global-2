import React, { useState, useEffect } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import CascadingDropdownMode from "./CascadingDropdownMode";
import AutocompleteMode from "./AutocompleteMode";
import BarcodeScanMode from "./BarcodeScanMode";
import CompactLocationView from "./CompactLocationView";
import LocationManagementModal from "../SampleStorage/LocationManagementModal";
import "./StorageLocationSelector.css";

/**
 * Main Storage Location Selector Widget
 * Supports two-tier design: compact inline view + expandable modal
 * Also supports legacy mode: direct dropdown/autocomplete/barcode selection
 *
 * Props:
 * - workflow: 'orders' | 'results' - Uses two-tier design when specified
 * - mode: 'dropdown' | 'autocomplete' | 'barcode' - Legacy mode (used when workflow not specified)
 * - onLocationChange: callback when location selected
 * - enableInlineCreation: boolean to show "Add New" buttons
 * - optional: boolean - can be left blank
 * - showQuickFind: boolean - Show quick-find search in compact view (results workflow)
 * - sampleInfo: object - { sampleId, type, status } - For modal display
 * - hierarchicalPath: string - Initial hierarchical path to display (for results workflow)
 */
const StorageLocationSelector = ({
  workflow,
  mode = "dropdown",
  onLocationChange,
  enableInlineCreation = false,
  optional = true,
  showQuickFind = false,
  sampleInfo = null,
  hierarchicalPath: initialHierarchicalPath = "",
}) => {
  const intl = useIntl();
  const [selectedLocation, setSelectedLocation] = useState(null);
  const [hierarchicalPath, setHierarchicalPath] = useState(
    initialHierarchicalPath,
  );
  const [isModalOpen, setIsModalOpen] = useState(false);

  // Update hierarchicalPath when initialHierarchicalPath prop changes
  useEffect(() => {
    if (initialHierarchicalPath) {
      setHierarchicalPath(initialHierarchicalPath);
    }
  }, [initialHierarchicalPath]);

  const buildHierarchicalPath = (location) => {
    if (!location) return "";
    if (location.position) {
      return `${location.room?.name} > ${location.device?.name} > ${location.shelf?.label} > ${location.rack?.label} > Position ${location.position.coordinate}`;
    } else if (location.rack) {
      return `${location.room?.name} > ${location.device?.name} > ${location.shelf?.label} > ${location.rack?.label}`;
    } else if (location.shelf) {
      return `${location.room?.name} > ${location.device?.name} > ${location.shelf?.label}`;
    } else if (location.device) {
      return `${location.room?.name} > ${location.device?.name}`;
    } else if (location.room) {
      return location.room.name;
    }
    return "";
  };

  const handleLocationChange = (location) => {
    setSelectedLocation(location);
    const path = buildHierarchicalPath(location);
    setHierarchicalPath(path);

    if (onLocationChange) {
      onLocationChange(location);
    }
  };

  const handleBarcodeScanned = (barcode) => {
    // TODO: Parse barcode and fetch location
    console.log("Barcode scanned:", barcode);
  };

  const handleExpand = () => {
    setIsModalOpen(true);
  };

  const handleModalSave = (locationData) => {
    // locationData format: { sample, newLocation, reason?, conditionNotes?, positionCoordinate? }
    // Extract newLocation from locationData
    const newLocation = locationData?.newLocation || locationData;
    handleLocationChange(newLocation);
    setIsModalOpen(false);
  };

  const handleModalClose = () => {
    setIsModalOpen(false);
  };

  // Two-tier design: compact view + modal
  if (workflow === "orders" || workflow === "results") {
    const currentLocation = hierarchicalPath
      ? { path: hierarchicalPath, position: selectedLocation?.position }
      : null;

    // Convert sampleInfo format to sample format for LocationManagementModal
    // Support both SampleItem context (sampleItemId, sampleItemExternalId, sampleAccessionNumber)
    // and legacy Sample context (sampleId) for backward compatibility
    const sample = sampleInfo
      ? {
          id: sampleInfo.sampleItemId || sampleInfo.sampleId || sampleInfo.id,
          sampleId:
            sampleInfo.sampleItemId || sampleInfo.sampleId || sampleInfo.id,
          sampleItemId:
            sampleInfo.sampleItemId || sampleInfo.sampleId || sampleInfo.id,
          sampleItemExternalId: sampleInfo.sampleItemExternalId || null,
          sampleAccessionNumber: sampleInfo.sampleAccessionNumber || null,
          type: sampleInfo.type || "",
          status: sampleInfo.status || "Active",
        }
      : null;

    return (
      <div
        className="storage-location-selector"
        data-testid="storage-location-selector"
      >
        <CompactLocationView
          locationPath={hierarchicalPath}
          onExpand={handleExpand}
          showQuickFind={showQuickFind && workflow === "results"}
          onLocationSelect={handleLocationChange}
        />
        <LocationManagementModal
          open={isModalOpen}
          sample={sample}
          currentLocation={currentLocation}
          onClose={handleModalClose}
          onConfirm={handleModalSave}
        />
      </div>
    );
  }

  // Legacy mode: direct selection
  return (
    <div
      className="storage-location-selector"
      data-testid="storage-location-selector"
    >
      <div className="selector-header">
        <h4>
          <FormattedMessage id="storage.location.label" />
          {optional && <span className="optional-indicator"> (optional)</span>}
        </h4>
      </div>

      <div className="selector-content">
        {mode === "dropdown" && (
          <CascadingDropdownMode
            onLocationChange={handleLocationChange}
            enableInlineCreation={enableInlineCreation}
          />
        )}

        {mode === "autocomplete" && (
          <AutocompleteMode onLocationChange={handleLocationChange} />
        )}

        {mode === "barcode" && (
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

import React, { useState, useRef, useCallback, useEffect } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  TextArea,
  TextInput,
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { ArrowDown } from "@carbon/icons-react";
import LocationSearchAndCreate from "../StorageLocationSelector/LocationSearchAndCreate";
import UnifiedBarcodeInput from "../StorageLocationSelector/UnifiedBarcodeInput";
import "./LocationManagementModal.css";

/**
 * Consolidated modal for managing SampleItem storage location (assignment and movement)
 * Handles both initial assignment (no location) and movement (existing location) workflows
 * Storage tracking operates at SampleItem level (physical specimens), not Sample level (orders)
 *
 * Props:
 * - open: boolean - Whether modal is open
 * - sample: object - { id, sampleItemId, sampleItemExternalId?, sampleAccessionNumber?, type, status, dateCollected?, patientId?, testOrders? }
 *   - id/sampleItemId: SampleItem ID (primary identifier)
 *   - sampleItemExternalId: SampleItem external ID (optional, displayed if available)
 *   - sampleAccessionNumber: Parent Sample accession number (secondary identifier)
 *   - type: SampleItem type (e.g., "Blood", "Serum")
 *   - status: SampleItem status
 * - currentLocation: object - { path, position } or null
 * - onClose: function - Callback when modal closes
 * - onConfirm: function - Callback when location is confirmed with { sample, newLocation, reason?, conditionNotes?, positionCoordinate? }
 *   - The sample object should include sampleItemId for API calls
 * - autoSave: boolean - Whether to auto-save when a valid location is selected (default: false)
 *   - When true, automatically calls onConfirm after a 500ms delay when a valid location is selected
 *   - Valid location = minimum device level selected
 *   - Default false: user must click confirm button to submit
 */
const LocationManagementModal = ({
  open,
  sample,
  currentLocation,
  onClose,
  onConfirm,
  autoSave = false, // Disabled by default - user must click confirm button
}) => {
  const intl = useIntl();

  // Map status codes to human-readable labels
  const getStatusLabel = (status) => {
    const statusMap = {
      20: "Sample Entered",
      21: "Entered",
      active: "Active",
      disposed: "Disposed",
    };
    return statusMap[String(status)] || status || "Unknown";
  };

  const [selectedLocation, setSelectedLocation] = useState(null);
  const selectedLocationRef = useRef(null);
  const [selectedLocationPath, setSelectedLocationPath] = useState("");
  const [reason, setReason] = useState("");
  const [conditionNotes, setConditionNotes] = useState("");
  const [positionCoordinate, setPositionCoordinate] = useState("");
  const [locationUpdateTrigger, setLocationUpdateTrigger] = useState(0);
  const [barcodeValidationState, setBarcodeValidationState] = useState("ready");
  const [barcodeErrorMessage, setBarcodeErrorMessage] = useState("");
  const [lastModifiedMethod, setLastModifiedMethod] = useState(null); // null | 'dropdown' | 'barcode'
  const [lastModifiedTimestamp, setLastModifiedTimestamp] = useState(null);
  const [autoOpenCreateForm, setAutoOpenCreateForm] = useState(false);
  const [prefillLocation, setPrefillLocation] = useState(null);
  const [focusField, setFocusField] = useState(null); // 'device' | 'shelf' | 'rack' | 'position'
  const [showAdditionalInvalidWarning, setShowAdditionalInvalidWarning] =
    useState(false);
  const [isAutoSaving, setIsAutoSaving] = useState(false); // Track auto-save in progress
  const [showAutoSaved, setShowAutoSaved] = useState(false); // Show "autosaved" indicator
  const autoSaveTimeoutRef = useRef(null); // Ref to store auto-save timeout

  // Determine modal mode: assignment (no location) or movement (location exists)
  const isMovementMode = !!currentLocation;

  // Track initial values for position and notes to detect changes
  const [initialPositionCoordinate, setInitialPositionCoordinate] =
    useState("");
  const [initialConditionNotes, setInitialConditionNotes] = useState("");

  // Pre-populate position and notes if current location exists
  useEffect(() => {
    if (currentLocation) {
      // Pre-populate position if available, otherwise reset to empty
      const initialPosition = currentLocation.position?.coordinate || "";
      setPositionCoordinate(initialPosition);
      setInitialPositionCoordinate(initialPosition);

      // Pre-populate notes if available, otherwise reset to empty
      const initialNotes = currentLocation.notes || "";
      setConditionNotes(initialNotes);
      setInitialConditionNotes(initialNotes);
    } else {
      // Reset to empty when no current location
      setPositionCoordinate("");
      setInitialPositionCoordinate("");
      setConditionNotes("");
      setInitialConditionNotes("");
    }
  }, [currentLocation]);

  // Reset form when modal closes
  useEffect(() => {
    if (!open) {
      setSelectedLocation(null);
      selectedLocationRef.current = null;
      setSelectedLocationPath("");
      setReason("");
      setConditionNotes("");
      setPositionCoordinate("");
      setLocationUpdateTrigger(0);
      setBarcodeValidationState("ready");
      setBarcodeErrorMessage("");
      setLastModifiedMethod(null);
      setLastModifiedTimestamp(null);
      setAutoOpenCreateForm(false);
      setPrefillLocation(null);
      setFocusField(null);
      setShowAdditionalInvalidWarning(false);
      setIsAutoSaving(false);
      setShowAutoSaved(false);
      // Clear any pending auto-save timeout
      if (autoSaveTimeoutRef.current) {
        clearTimeout(autoSaveTimeoutRef.current);
        autoSaveTimeoutRef.current = null;
      }
    }
  }, [open]);

  // OGC-71: Auto-save effect - triggers handleConfirm when a valid location is selected
  // Uses a debounced approach with 500ms delay to allow user to continue making changes
  useEffect(() => {
    // Only auto-save if enabled and modal is open
    if (!autoSave || !open || isAutoSaving) {
      return;
    }

    // Clear any existing timeout
    if (autoSaveTimeoutRef.current) {
      clearTimeout(autoSaveTimeoutRef.current);
      autoSaveTimeoutRef.current = null;
    }

    // Check if we have a valid location (minimum: device selected)
    const locationToCheck = selectedLocation || selectedLocationRef.current;
    const hasValidLocation =
      locationToCheck &&
      (locationToCheck.device?.id ||
        (locationToCheck.type === "device" && locationToCheck.id) ||
        locationToCheck.shelf?.id ||
        locationToCheck.rack?.id);

    if (hasValidLocation && onConfirm) {
      // Set auto-saving state and trigger save after delay
      autoSaveTimeoutRef.current = setTimeout(async () => {
        setIsAutoSaving(true);
        try {
          const result = onConfirm({
            sample,
            newLocation: locationToCheck,
            reason: isMovementMode ? reason : undefined,
            conditionNotes: conditionNotes || undefined,
            positionCoordinate: positionCoordinate || undefined,
          });
          // If onConfirm returns a promise, await it
          if (result && typeof result.then === "function") {
            await result;
          }
          // Show autosaved indicator (don't close modal - user must click confirm to close)
          setShowAutoSaved(true);
          setIsAutoSaving(false);
          // Hide the indicator after 3 seconds
          setTimeout(() => setShowAutoSaved(false), 3000);
        } catch (error) {
          console.error("[LocationManagementModal] Auto-save error:", error);
          // Don't close modal on error, reset auto-saving state
          setIsAutoSaving(false);
        }
      }, 500); // 500ms delay for debouncing
    }

    // Cleanup function
    return () => {
      if (autoSaveTimeoutRef.current) {
        clearTimeout(autoSaveTimeoutRef.current);
        autoSaveTimeoutRef.current = null;
      }
    };
  }, [
    selectedLocation,
    autoSave,
    open,
    isAutoSaving,
    onConfirm,
    sample,
    isMovementMode,
    reason,
    conditionNotes,
    positionCoordinate,
  ]);

  const handleLocationChange = useCallback(
    (location) => {
      // Implement "last-modified wins" logic: only overwrite if dropdown is newer
      // If barcode was used more recently, don't overwrite
      const timestamp = Date.now();
      if (
        lastModifiedTimestamp === null ||
        timestamp >= lastModifiedTimestamp
      ) {
        // Track last-modified method and timestamp only if we're overwriting
        setLastModifiedMethod("dropdown");
        setLastModifiedTimestamp(timestamp);

        if (location) {
          selectedLocationRef.current = location;

          let path = "";
          const hierarchicalPath =
            location.hierarchical_path || location.hierarchicalPath;
          if (hierarchicalPath && hierarchicalPath.trim()) {
            path = hierarchicalPath.trim();
          } else {
            const roomName = location.room?.name || location.room?.code || "";
            const deviceName =
              location.device?.name || location.device?.code || "";
            const shelfLabel =
              location.shelf?.label || location.shelf?.name || "";
            const rackLabel = location.rack?.label || location.rack?.name || "";
            const positionCoord =
              location.position?.coordinate || location.position || "";

            const pathParts = [];
            if (roomName) pathParts.push(roomName);
            if (deviceName) pathParts.push(deviceName);
            if (shelfLabel) pathParts.push(shelfLabel);
            if (rackLabel) pathParts.push(rackLabel);
            if (positionCoord) pathParts.push(`Position ${positionCoord}`);

            path = pathParts.join(" > ");

            if (!path && location.name) {
              path = location.name;
            }
          }

          setSelectedLocation(location);
          setSelectedLocationPath(path);
          setLocationUpdateTrigger((prev) => prev + 1);

          // Update position coordinate if position is selected
          if (location && location.position) {
            setPositionCoordinate(location.position.coordinate || "");
          }
        } else {
          selectedLocationRef.current = null;
          setSelectedLocation(null);
          setSelectedLocationPath("");
          setLocationUpdateTrigger((prev) => prev + 1);
        }
      }
    },
    [lastModifiedTimestamp],
  );

  const handleConfirm = async () => {
    const locationToUse = selectedLocation || selectedLocationRef.current;

    // Allow submission when:
    // 1. Location is selected (normal case)
    // 2. No location but metadata changed in movement mode (metadata-only update)
    if (
      !locationToUse &&
      !(isMovementMode && (positionCoordinate || conditionNotes))
    ) {
      console.error(
        "[LocationManagementModal] handleConfirm: No location selected and no metadata to update",
      );
      return;
    }

    if (!onConfirm) {
      console.error(
        "[LocationManagementModal] handleConfirm: onConfirm callback not provided",
      );
      return;
    }

    try {
      // Ensure onConfirm returns a promise
      const result = onConfirm({
        sample,
        newLocation: locationToUse || null, // null if only metadata changed
        reason: isMovementMode ? reason : undefined,
        conditionNotes: conditionNotes || undefined,
        positionCoordinate: positionCoordinate || undefined,
      });

      // If onConfirm returns a promise, await it
      if (result && typeof result.then === "function") {
        await result;
      }

      handleClose();
    } catch (error) {
      console.error(
        "[LocationManagementModal] handleConfirm: Error occurred:",
        error,
      );
      // Don't close modal on error - let user see the error notification
      // Error is already handled and displayed by the parent component
    }
  };

  const handleBarcodeScan = (barcode) => {
    // Barcode scan detected - validation will be triggered automatically
  };

  const handleSampleScan = (sampleData) => {
    // Sample barcode detected - load sample details and pre-fill sample context
    // TODO: Implement sample loading logic if needed
  };

  const handleBarcodeValidationResult = (result) => {
    if (result.success && result.data) {
      // Successful barcode validation
      setBarcodeValidationState("success");
      setBarcodeErrorMessage("");

      // Auto-populate location from barcode validation
      // Backend returns location data in validComponents map when valid=true
      const locationData = result.data;
      const validComponents = locationData.validComponents || {};

      // Extract location components from validComponents map (backend structure)
      // or use direct properties if available (for backward compatibility)
      const location = {
        room: locationData.room || validComponents.room || null,
        device: locationData.device || validComponents.device || null,
        shelf: locationData.shelf || validComponents.shelf || null,
        rack: locationData.rack || validComponents.rack || null,
        position: locationData.position || validComponents.position || null,
        hierarchicalPath: locationData.hierarchicalPath || null,
      };

      // Implement "last-modified wins" logic: only overwrite if barcode is newer
      const timestamp = Date.now();
      if (
        lastModifiedTimestamp === null ||
        timestamp >= lastModifiedTimestamp
      ) {
        setLastModifiedMethod("barcode");
        setLastModifiedTimestamp(timestamp);

        setSelectedLocation(location);

        // Update position coordinate if available
        if (locationData.position && locationData.position.coordinate) {
          setPositionCoordinate(locationData.position.coordinate);
        }

        // Update path
        let path = "";
        if (locationData.hierarchicalPath) {
          path = locationData.hierarchicalPath;
        } else {
          const pathParts = [];
          if (locationData.room?.name) pathParts.push(locationData.room.name);
          if (locationData.device?.name)
            pathParts.push(locationData.device.name);
          if (locationData.shelf?.label)
            pathParts.push(locationData.shelf.label);
          if (locationData.rack?.label) pathParts.push(locationData.rack.label);
          if (locationData.position?.coordinate) {
            pathParts.push(`Position ${locationData.position.coordinate}`);
          }
          path = pathParts.join(" > ");
        }
        setSelectedLocationPath(path);
        setLocationUpdateTrigger((prev) => prev + 1);
      }
    } else {
      // Validation failed - check for partial valid hierarchy
      const firstMissingLevel = result.firstMissingLevel || null;
      const validComponents = result.validComponents || {};
      const hasAdditionalInvalid = result.hasAdditionalInvalidLevels || false;

      if (firstMissingLevel && Object.keys(validComponents).length > 0) {
        // Partial valid hierarchy - auto-open create form
        setBarcodeValidationState("error");
        setBarcodeErrorMessage("");

        // Convert validComponents to location format for pre-filling
        // validComponents is a Map<String, Object> where each value is { id, name, code } or { id, label, label }
        // Ensure all required fields are present for proper selection (not creation mode)
        const prefillLoc = {
          room: validComponents.room
            ? {
                id: validComponents.room.id,
                name: validComponents.room.name,
                code: validComponents.room.code,
                active: validComponents.room.active !== false, // Default to true if not specified
              }
            : null,
          device: validComponents.device
            ? {
                id: validComponents.device.id,
                name: validComponents.device.name,
                code: validComponents.device.code,
              }
            : null,
          shelf: validComponents.shelf
            ? {
                id: validComponents.shelf.id,
                label:
                  validComponents.shelf.label || validComponents.shelf.name,
                name: validComponents.shelf.name || validComponents.shelf.label,
              }
            : null,
          rack: validComponents.rack
            ? {
                id: validComponents.rack.id,
                label: validComponents.rack.label || validComponents.rack.name,
                name: validComponents.rack.name || validComponents.rack.label,
              }
            : null,
          position: validComponents.position
            ? {
                id: validComponents.position.id,
                coordinate: validComponents.position.coordinate,
              }
            : null,
        };

        // Set auto-open state
        setAutoOpenCreateForm(true);
        setPrefillLocation(prefillLoc);
        setFocusField(firstMissingLevel);
        setShowAdditionalInvalidWarning(hasAdditionalInvalid);

        // Pre-fill selected location with valid components
        const timestamp = Date.now();
        if (
          lastModifiedTimestamp === null ||
          timestamp >= lastModifiedTimestamp
        ) {
          setLastModifiedMethod("barcode");
          setLastModifiedTimestamp(timestamp);
          setSelectedLocation(prefillLoc);
        }
      } else {
        // Complete validation failure - no valid levels
        setBarcodeValidationState("error");
        setAutoOpenCreateForm(false);
        setPrefillLocation(null);
        setFocusField(null);
        setShowAdditionalInvalidWarning(false);

        const errorMsg =
          result.error?.errorMessage ||
          result.error?.message ||
          result.data?.errorMessage ||
          intl.formatMessage({
            id: "barcode.error",
            defaultMessage: "Invalid barcode",
          });
        setBarcodeErrorMessage(errorMsg);

        // Reset to ready state after 3 seconds
        setTimeout(() => {
          setBarcodeValidationState("ready");
        }, 3000);
      }
    }
  };

  const handleClose = () => {
    setSelectedLocation(null);
    selectedLocationRef.current = null;
    setSelectedLocationPath("");
    setReason("");
    setConditionNotes("");
    setPositionCoordinate("");
    setLocationUpdateTrigger(0);
    setBarcodeValidationState("ready");
    setBarcodeErrorMessage("");
    setLastModifiedMethod(null);
    setLastModifiedTimestamp(null);
    onClose();
  };

  // Handle Escape key to close modal
  useEffect(() => {
    const handleEscape = (event) => {
      if (event.key === "Escape" && open) {
        handleClose();
      }
    };

    if (open) {
      document.addEventListener("keydown", handleEscape);
    }

    return () => {
      document.removeEventListener("keydown", handleEscape);
    };
  }, [open, handleClose]);

  const selectedLocationForValidation =
    selectedLocationRef.current || selectedLocation;

  // Validation logic for location selection
  const hasRoom = !!(
    selectedLocationForValidation?.room &&
    (selectedLocationForValidation.room.id ||
      selectedLocationForValidation.room.name ||
      selectedLocationForValidation.room)
  );
  const hasDevice = !!(
    selectedLocationForValidation?.device &&
    (selectedLocationForValidation.device.id ||
      selectedLocationForValidation.device.name ||
      selectedLocationForValidation.device)
  );
  const hasShelf = !!(
    selectedLocationForValidation?.shelf &&
    (selectedLocationForValidation.shelf.id ||
      selectedLocationForValidation.shelf.label ||
      selectedLocationForValidation.shelf.name ||
      selectedLocationForValidation.shelf)
  );
  const hasRack = !!(
    selectedLocationForValidation?.rack &&
    (selectedLocationForValidation.rack.id ||
      selectedLocationForValidation.rack.label ||
      selectedLocationForValidation.rack.name ||
      selectedLocationForValidation.rack)
  );

  const hasLocationId = !!selectedLocationForValidation?.id;
  const hasLocationType = !!(
    selectedLocationForValidation?.type &&
    (selectedLocationForValidation.type === "device" ||
      selectedLocationForValidation.type === "shelf" ||
      selectedLocationForValidation.type === "rack")
  );

  let canExtractLocationId = false;
  if (hasRack && selectedLocationForValidation.rack.id) {
    canExtractLocationId = true;
  } else if (hasShelf && selectedLocationForValidation.shelf.id) {
    canExtractLocationId = true;
  } else if (hasDevice && selectedLocationForValidation.device.id) {
    canExtractLocationId = true;
  } else if (hasLocationId && hasLocationType) {
    canExtractLocationId = true;
  }

  const meetsMinimumLevels = (hasRoom && hasDevice) || hasLocationId;

  // Check if position or notes have changed from initial values
  const positionChanged = positionCoordinate !== initialPositionCoordinate;
  const notesChanged = conditionNotes !== initialConditionNotes;
  const metadataChanged = positionChanged || notesChanged;

  // Enable button if:
  // 1. Location is selected (original behavior), OR
  // 2. Metadata changed AND we're in movement mode with existing location (can update metadata only)
  const canConfirmLocation =
    (meetsMinimumLevels && canExtractLocationId) ||
    (isMovementMode && metadataChanged);

  // Handle Enter key to submit form (except in textarea)
  const handleKeyDown = (event) => {
    if (event.key === "Enter" && !event.shiftKey) {
      // Don't submit if focus is in textarea
      if (event.target.tagName === "TEXTAREA") {
        return;
      }
      event.preventDefault();
      if (canConfirmLocation) {
        handleConfirm();
      }
    }
  };

  // Determine if Reason for Move should be visible
  // Show only when: location exists AND different location selected
  const showReasonForMove =
    isMovementMode &&
    selectedLocationForValidation &&
    selectedLocationPath &&
    selectedLocationPath !== currentLocation?.path;

  // Dynamic title and button text based on mode
  const modalTitle = isMovementMode
    ? intl.formatMessage({
        id: "storage.move.sample",
        defaultMessage: "Move Sample",
      })
    : intl.formatMessage({
        id: "storage.assign.location",
        defaultMessage: "Assign Storage Location",
      });

  const buttonText = isMovementMode
    ? intl.formatMessage({
        id: "storage.confirm.move",
        defaultMessage: "Confirm Move",
      })
    : intl.formatMessage({
        id: "storage.assign",
        defaultMessage: "Assign",
      });

  return (
    <ComposedModal
      open={open}
      onClose={handleClose}
      size="lg"
      data-testid="location-management-modal"
    >
      <ModalHeader
        title={modalTitle}
        subtitle={
          isMovementMode
            ? intl.formatMessage(
                {
                  id: "storage.move.sample.subtitle",
                  defaultMessage:
                    "Move sample item {sampleItemId} to a new storage location",
                },
                {
                  sampleItemId:
                    sample?.sampleItemExternalId ||
                    sample?.sampleItemId ||
                    sample?.id ||
                    sample?.sampleId ||
                    "",
                },
              )
            : intl.formatMessage(
                {
                  id: "storage.assign.location.subtitle",
                  defaultMessage:
                    "Assign storage location for sample item {sampleItemId}",
                },
                {
                  sampleItemId:
                    sample?.sampleItemExternalId ||
                    sample?.sampleItemId ||
                    sample?.id ||
                    sample?.sampleId ||
                    "",
                },
              )
        }
      />
      <ModalBody onKeyDown={handleKeyDown}>
        {/* Comprehensive Sample Information Section */}
        {sample && (
          <div
            className="location-management-sample-info"
            data-testid="sample-info-section"
          >
            <div className="info-box">
              {/* SampleItem ID/External ID (primary identifier) */}
              <div className="info-row">
                <span className="info-label">
                  <FormattedMessage
                    id="sample.item.id"
                    defaultMessage="Sample Item ID"
                  />
                  :
                </span>
                <span className="info-value">
                  {sample.sampleItemExternalId ||
                    sample.sampleItemId ||
                    sample.id ||
                    sample.sampleId ||
                    "N/A"}
                </span>
              </div>
              {/* Parent Sample accession number (secondary identifier) */}
              {sample.sampleAccessionNumber && (
                <div className="info-row">
                  <span className="info-label">
                    <FormattedMessage
                      id="sample.accession.number"
                      defaultMessage="Sample Accession"
                    />
                    :
                  </span>
                  <span className="info-value">
                    {sample.sampleAccessionNumber}
                  </span>
                </div>
              )}
              <div className="info-row">
                <span className="info-label">
                  <FormattedMessage id="sample.type" defaultMessage="Type" />:
                </span>
                <span className="info-value">{sample.type}</span>
              </div>
              <div className="info-row">
                <span className="info-label">
                  <FormattedMessage
                    id="storage.status"
                    defaultMessage="Status"
                  />
                  :
                </span>
                <span className="info-value">
                  {getStatusLabel(sample.status)}
                </span>
              </div>
              {sample.dateCollected && (
                <div className="info-row">
                  <span className="info-label">
                    <FormattedMessage
                      id="sample.date.collected"
                      defaultMessage="Date Collected"
                    />
                    :
                  </span>
                  <span className="info-value">{sample.dateCollected}</span>
                </div>
              )}
              {sample.patientId && (
                <div className="info-row">
                  <span className="info-label">
                    <FormattedMessage
                      id="patient.id"
                      defaultMessage="Patient ID"
                    />
                    :
                  </span>
                  <span className="info-value">{sample.patientId}</span>
                </div>
              )}
              {sample.testOrders && sample.testOrders.length > 0 && (
                <div className="info-row">
                  <span className="info-label">
                    <FormattedMessage
                      id="test.orders"
                      defaultMessage="Test Orders"
                    />
                    :
                  </span>
                  <span className="info-value">
                    {Array.isArray(sample.testOrders)
                      ? sample.testOrders.join(", ")
                      : sample.testOrders}
                  </span>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Visual Separator after sample info */}
        {sample && <div className="location-management-separator" />}

        {/* Location Flow Section - Current Location → Arrow → New Location Selector → Selected Location Preview */}
        <div className="location-management-location-flow">
          {/* Current Location Section - Only show if location exists */}
          {currentLocation && (
            <div
              className="location-management-current-location"
              data-testid="current-location-section"
            >
              <div className="location-box">
                <div className="location-label">
                  <FormattedMessage
                    id="storage.current.location"
                    defaultMessage="Current Location"
                  />
                  :
                </div>
                <div className="location-path">{currentLocation.path}</div>
              </div>
            </div>
          )}

          {/* Downward Arrow Icon - Only show if current location exists */}
          {currentLocation && (
            <div className="location-management-arrow">
              <ArrowDown size={24} />
            </div>
          )}

          {/* New Location Selector */}
          <div
            className="location-management-new-location"
            data-testid="new-location-section"
          >
            {/* Barcode Input Section */}
            <div
              className={`form-group ${lastModifiedMethod === "barcode" ? "active-input-method" : ""}`}
            >
              <label className="form-label">
                <FormattedMessage
                  id="storage.barcode.scan"
                  defaultMessage="Quick Assign (Barcode)"
                />
              </label>
              <UnifiedBarcodeInput
                onScan={handleBarcodeScan}
                onValidationResult={handleBarcodeValidationResult}
                onSampleScan={handleSampleScan}
                validationState={barcodeValidationState}
                errorMessage={barcodeErrorMessage}
              />
            </div>

            <div className="location-selector-box">
              <label className="form-label">
                <FormattedMessage
                  id="storage.select.location"
                  defaultMessage="Select Location"
                />{" "}
                <span className="required-indicator">*</span>
              </label>
              <div className="location-management-location-selector">
                {showAdditionalInvalidWarning && (
                  <div style={{ marginBottom: "1rem" }}>
                    <InlineNotification
                      kind="warning"
                      title={intl.formatMessage({
                        id: "barcode.additionalInvalidLevels.warning",
                        defaultMessage: "Additional invalid levels detected",
                      })}
                      subtitle={intl.formatMessage({
                        id: "barcode.additionalInvalidLevels.message",
                        defaultMessage:
                          "The barcode contains levels beyond the valid portion. Only valid levels are pre-filled.",
                      })}
                      lowContrast
                      hideCloseButton
                    />
                  </div>
                )}
                <LocationSearchAndCreate
                  onLocationChange={handleLocationChange}
                  selectedLocation={selectedLocationForValidation}
                  allowInactive={false}
                  showCreateButton={true}
                  isActive={lastModifiedMethod === "dropdown"}
                  autoOpenCreateForm={autoOpenCreateForm}
                  prefillLocation={prefillLocation}
                  focusField={focusField}
                  onCreateFormOpened={() => {
                    // Reset auto-open flag after form is opened
                    setAutoOpenCreateForm(false);
                  }}
                />
              </div>
            </div>
          </div>

          {/* Selected Location Preview - Show when location is selected */}
          {selectedLocationPath && (
            <div
              className="location-management-selected-preview"
              data-testid="selected-location-section"
            >
              <div className="location-box">
                <div className="location-label">
                  <FormattedMessage
                    id="storage.selected.location"
                    defaultMessage="Selected Location"
                  />
                  :
                </div>
                <div className="location-path">{selectedLocationPath}</div>
              </div>
              {/* Unobtrusive autosaved indicator */}
              {showAutoSaved && (
                <div
                  className="autosaved-indicator"
                  data-testid="autosaved-indicator"
                >
                  <FormattedMessage
                    id="storage.autosaved"
                    defaultMessage="✓ Autosaved"
                  />
                </div>
              )}
            </div>
          )}
        </div>

        {/* Optional Fields Section - Position, Condition Notes, Reason for Move */}
        <div className="location-management-optional-fields">
          {/* Position Input */}
          <div className="form-group">
            <label className="form-label">
              <FormattedMessage
                id="storage.position.label"
                defaultMessage="Position"
              />{" "}
              <span className="optional-text">
                (
                <FormattedMessage
                  id="label.optional"
                  defaultMessage="optional"
                />
                )
              </span>
            </label>
            <TextInput
              id="position-input"
              labelText=""
              value={positionCoordinate}
              onChange={(e) => setPositionCoordinate(e.target.value)}
              placeholder={intl.formatMessage({
                id: "storage.position.placeholder",
                defaultMessage: "e.g., A5, 1-1, RED-12",
              })}
            />
          </div>

          {/* Condition Notes - Always visible */}
          <div className="form-group">
            <label className="form-label">
              <FormattedMessage
                id="storage.condition.notes"
                defaultMessage="Condition Notes"
              />{" "}
              <span className="optional-text">
                (
                <FormattedMessage
                  id="label.optional"
                  defaultMessage="optional"
                />
                )
              </span>
            </label>
            <TextArea
              id="condition-notes"
              labelText=""
              value={conditionNotes}
              onChange={(e) => setConditionNotes(e.target.value)}
              placeholder={intl.formatMessage({
                id: "storage.condition.notes.placeholder",
                defaultMessage: "Enter any condition notes...",
              })}
              rows={3}
            />
          </div>

          {/* Reason for Move - Only show when moving to different location */}
          {showReasonForMove && (
            <div className="form-group">
              <label className="form-label">
                <FormattedMessage
                  id="storage.move.reason"
                  defaultMessage="Reason for Move"
                />{" "}
                <span className="optional-text">
                  (
                  <FormattedMessage
                    id="label.optional"
                    defaultMessage="optional"
                  />
                  )
                </span>
              </label>
              <TextArea
                id="move-reason"
                labelText=""
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder={intl.formatMessage({
                  id: "storage.move.reason.placeholder",
                  defaultMessage:
                    "Optional: Enter reason for moving this sample...",
                })}
                rows={3}
              />
            </div>
          )}
        </div>
      </ModalBody>
      <ModalFooter>
        <Button kind="secondary" onClick={handleClose}>
          <FormattedMessage id="label.button.cancel" defaultMessage="Cancel" />
        </Button>
        <Button
          kind="primary"
          onClick={handleConfirm}
          disabled={!canConfirmLocation}
          data-testid={isMovementMode ? "confirm-move-button" : "assign-button"}
        >
          {buttonText}
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default LocationManagementModal;

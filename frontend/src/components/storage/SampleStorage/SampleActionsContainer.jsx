import React, { useState } from "react";
import { useIntl } from "react-intl";
import SampleActionsOverflowMenu from "./SampleActionsOverflowMenu";
import LocationManagementModal from "./LocationManagementModal";
import DisposeSampleModal from "./DisposeSampleModal";

/**
 * Container component that manages sample action overflow menu and modals
 * Handles modal state and provides callbacks to open modals
 *
 * Props:
 * - sample: object - Sample data { id, sampleId, type, status, location }
 * - onLocationConfirm: function - Callback when location is confirmed (assignment or movement)
 * - onDisposeConfirm: function - Callback when dispose is confirmed (sample, reason, method, notes)
 * - onNotification: function - Callback to show notifications (optional)
 */
const SampleActionsContainer = ({
  sample,
  onLocationConfirm,
  onDisposeConfirm,
  onNotification,
}) => {
  const intl = useIntl();
  const [locationModalOpen, setLocationModalOpen] = useState(false);
  const [disposeModalOpen, setDisposeModalOpen] = useState(false);

  const handleManageLocation = (sample) => {
    setLocationModalOpen(true);
  };

  const handleDispose = (sample) => {
    setDisposeModalOpen(true);
  };

  const handleLocationConfirm = async (locationData) => {
    // locationData format: { sample, newLocation, reason?, conditionNotes?, positionCoordinate? }
    if (onLocationConfirm) {
      try {
        await onLocationConfirm(locationData);
        // Only close modal if operation succeeds (no error thrown)
        setLocationModalOpen(false);
        // Note: Success notification is handled by parent component after API call succeeds
      } catch (error) {
        // Error notification is handled by parent component
        // Don't close modal on error so user can retry
        console.error("Location confirmation failed:", error);
      }
    }
  };

  const handleDisposeConfirm = (sample, reason, method, notes) => {
    if (onDisposeConfirm) {
      onDisposeConfirm(sample, reason, method, notes);
    }
    setDisposeModalOpen(false);
    if (onNotification) {
      onNotification({
        kind: "success",
        title: intl.formatMessage({
          id: "storage.dispose.success",
          defaultMessage: "Sample disposed successfully",
        }),
      });
    }
  };

  const currentLocation = sample.location
    ? { path: sample.location, position: null }
    : null;

  return (
    <>
      <SampleActionsOverflowMenu
        sample={sample}
        onManageLocation={handleManageLocation}
        onDispose={handleDispose}
      />

      <LocationManagementModal
        open={locationModalOpen}
        sample={sample}
        currentLocation={currentLocation}
        onClose={() => setLocationModalOpen(false)}
        onConfirm={handleLocationConfirm}
      />

      <DisposeSampleModal
        open={disposeModalOpen}
        sample={sample}
        currentLocation={currentLocation}
        onClose={() => setDisposeModalOpen(false)}
        onConfirm={handleDisposeConfirm}
      />
    </>
  );
};

export default SampleActionsContainer;

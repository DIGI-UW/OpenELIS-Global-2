import React, { useState } from "react";
import { useIntl } from "react-intl";
import SampleActionsOverflowMenu from "./SampleActionsOverflowMenu";
import MoveSampleModal from "./MoveSampleModal";
import DisposeSampleModal from "./DisposeSampleModal";
import ViewStorageModal from "./ViewStorageModal";

/**
 * Container component that manages sample action overflow menu and modals
 * Handles modal state and provides callbacks to open modals
 *
 * Props:
 * - sample: object - Sample data { id, sampleId, type, status, location }
 * - onMoveConfirm: function - Callback when move is confirmed (sample, newLocation, reason)
 * - onDisposeConfirm: function - Callback when dispose is confirmed (sample, reason, method, notes)
 * - onViewStorageSave: function - Callback when storage location is saved (sample, location)
 * - onNotification: function - Callback to show notifications (optional)
 */
const SampleActionsContainer = ({
  sample,
  onMoveConfirm,
  onDisposeConfirm,
  onViewStorageSave,
  onNotification,
}) => {
  const intl = useIntl();
  const [moveModalOpen, setMoveModalOpen] = useState(false);
  const [disposeModalOpen, setDisposeModalOpen] = useState(false);
  const [viewStorageModalOpen, setViewStorageModalOpen] = useState(false);

  const handleMove = (sample) => {
    setMoveModalOpen(true);
  };

  const handleDispose = (sample) => {
    setDisposeModalOpen(true);
  };

  const handleViewStorage = (sample) => {
    setViewStorageModalOpen(true);
  };

  const handleMoveConfirm = (sample, newLocation, reason) => {
    if (onMoveConfirm) {
      onMoveConfirm(sample, newLocation, reason);
    }
    setMoveModalOpen(false);
    if (onNotification) {
      onNotification({
        kind: "success",
        title: intl.formatMessage({
          id: "storage.move.success",
          defaultMessage: "Sample moved successfully",
        }),
      });
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

  const handleViewStorageSave = (location) => {
    if (onViewStorageSave) {
      onViewStorageSave(sample, location);
    }
    setViewStorageModalOpen(false);
    if (onNotification) {
      onNotification({
        kind: "success",
        title: intl.formatMessage({
          id: "storage.save.success",
          defaultMessage: "Storage location updated successfully",
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
        onMove={handleMove}
        onDispose={handleDispose}
        onViewStorage={handleViewStorage}
      />

      <MoveSampleModal
        open={moveModalOpen}
        sample={sample}
        currentLocation={currentLocation}
        onClose={() => setMoveModalOpen(false)}
        onConfirm={handleMoveConfirm}
      />

      <DisposeSampleModal
        open={disposeModalOpen}
        sample={sample}
        currentLocation={currentLocation}
        onClose={() => setDisposeModalOpen(false)}
        onConfirm={handleDisposeConfirm}
      />

      <ViewStorageModal
        open={viewStorageModalOpen}
        sample={sample}
        currentLocation={currentLocation}
        onClose={() => setViewStorageModalOpen(false)}
        onSave={handleViewStorageSave}
      />
    </>
  );
};

export default SampleActionsContainer;


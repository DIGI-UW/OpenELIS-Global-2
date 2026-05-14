import React from "react";
import SampleActionsOverflowMenu from "./SampleActionsOverflowMenu";

/**
 * Container component that manages sample action overflow menu
 * Modals are managed at the parent level (StorageDashboard) for better performance
 *
 * Props:
 * - sample: object - Sample data { id, sampleId, type, status, location }
 * - onManageLocation: function - Callback when "Manage Location" is clicked (sample)
 * - onDispose: function - Callback when "Dispose" is clicked (sample)
 * - onViewAudit: function - OGC-649: Callback when "View Audit" is clicked (sample)
 */
const SampleActionsContainer = ({
  sample,
  onManageLocation,
  onDispose,
  onViewAudit,
}) => {
  return (
    <SampleActionsOverflowMenu
      sample={sample}
      onManageLocation={onManageLocation}
      onDispose={onDispose}
      onViewAudit={onViewAudit}
    />
  );
};

export default SampleActionsContainer;

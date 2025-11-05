import React from "react";
import { Button } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import QuickFindSearch from "./QuickFindSearch";
import "./CompactLocationView.css";

/**
 * Compact inline view for storage location selector
 * Displays selected location path (or "Not assigned") with "Expand" or "Edit" button
 *
 * Props:
 * - locationPath: string | null - Hierarchical path to display
 * - onExpand: function - Callback when expand button clicked
 * - showQuickFind: boolean - Whether to show quick-find search input (results workflow)
 * - onLocationSelect: function - Callback when location selected from quick-find (optional)
 */
const CompactLocationView = ({
  locationPath,
  onExpand,
  showQuickFind = false,
  onLocationSelect,
}) => {
  const intl = useIntl();

  const displayText = locationPath || "Not assigned";

  return (
    <div className="compact-location-view" data-testid="compact-location-view">
      <div className="location-path-display">
        <span className="path-label">
          <FormattedMessage id="storage.location.label" />:
        </span>
        <span className="path-value" data-testid="location-path-text">
          {displayText}
        </span>
      </div>
      {showQuickFind && (
        <div
          className="quick-find-container"
          data-testid="quick-find-container"
        >
          <QuickFindSearch onLocationSelect={onLocationSelect} />
        </div>
      )}
      <Button
        kind="ghost"
        size="sm"
        onClick={onExpand}
        data-testid="expand-button"
      >
        Expand
      </Button>
    </div>
  );
};

export default CompactLocationView;

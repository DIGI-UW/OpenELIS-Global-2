import React, { useCallback, useEffect } from "react";
import { OverflowMenu, OverflowMenuItem } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import "./SampleActionsOverflowMenu.css";

/**
 * Overflow menu for sample row actions
 * Displays three menu items: Manage Location, Dispose, View Audit (disabled)
 *
 * Props:
 * - sample: object - Sample data { id, sampleId, type, status }
 * - onManageLocation: function - Callback when Manage Location clicked
 * - onDispose: function - Callback when Dispose clicked
 */
const SampleActionsOverflowMenu = ({ sample, onManageLocation, onDispose }) => {
  const intl = useIntl();

  // Use useCallback to ensure stable function references
  // Carbon OverflowMenuItem onClick receives an event object
  const handleManageLocation = useCallback(
    (event) => {
      // Prevent default behavior and stop propagation
      if (event) {
        event.preventDefault?.();
        event.stopPropagation?.();
      }
      // Execute callback if provided
      if (onManageLocation) {
        onManageLocation(sample);
      }
    },
    [sample, onManageLocation],
  );

  const handleDispose = useCallback(
    (event) => {
      if (event) {
        event.preventDefault?.();
        event.stopPropagation?.();
      }
      if (onDispose) {
        onDispose(sample);
      } else {
        console.warn(
          "SampleActionsOverflowMenu: onDispose callback not provided for sample",
          sample?.sampleId,
        );
      }
    },
    [sample, onDispose],
  );

  return (
    <div className="sample-actions-overflow-menu">
      <OverflowMenu
        ariaLabel={intl.formatMessage({
          id: "storage.sample.actions",
          defaultMessage: "Sample actions",
        })}
        data-testid="sample-actions-overflow-menu"
      >
        <OverflowMenuItem
          itemText={intl.formatMessage({
            id: "storage.manage.location",
            defaultMessage: "Manage Location",
          })}
          onClick={handleManageLocation}
          data-testid="manage-location-menu-item"
        />
        <OverflowMenuItem
          itemText={intl.formatMessage({
            id: "storage.dispose.sample",
            defaultMessage: "Dispose",
          })}
          onClick={handleDispose}
          data-testid="dispose-menu-item"
        />
        <OverflowMenuItem
          itemText={intl.formatMessage({
            id: "storage.view.audit",
            defaultMessage: "View Audit",
          })}
          disabled
          data-testid="view-audit-menu-item"
        />
      </OverflowMenu>
    </div>
  );
};

export default SampleActionsOverflowMenu;

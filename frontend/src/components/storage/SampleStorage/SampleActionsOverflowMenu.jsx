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

  // Debug: Log component props on mount and when they change
  useEffect(() => {
    console.log("SampleActionsOverflowMenu: Component mounted/updated", {
      sampleId: sample?.sampleId,
      hasOnManageLocation: !!onManageLocation,
      hasOnDispose: !!onDispose,
      sample: sample,
    });
  }, [sample, onManageLocation, onDispose]);

  // Use useCallback to ensure stable function references
  // Carbon OverflowMenuItem onClick receives an event object
  const handleManageLocation = useCallback(
    (event) => {
      console.log("SampleActionsOverflowMenu: handleManageLocation called", {
        sample,
        event,
        hasOnManageLocation: !!onManageLocation,
      });
      // Prevent default behavior and stop propagation
      if (event) {
        event.preventDefault?.();
        event.stopPropagation?.();
      }
      // Execute callback if provided
      if (onManageLocation) {
        console.log(
          "SampleActionsOverflowMenu: executing onManageLocation callback",
        );
        try {
          onManageLocation(sample);
        } catch (error) {
          console.error(
            "SampleActionsOverflowMenu: error in onManageLocation callback",
            error,
          );
        }
      } else {
        console.warn(
          "SampleActionsOverflowMenu: onManageLocation callback not provided for sample",
          sample?.sampleId,
        );
      }
    },
    [sample, onManageLocation],
  );

  const handleDispose = useCallback(
    (event) => {
      console.log("SampleActionsOverflowMenu: handleDispose called", {
        sample,
        event,
        hasOnDispose: !!onDispose,
      });
      if (event) {
        event.preventDefault?.();
        event.stopPropagation?.();
      }
      if (onDispose) {
        console.log("SampleActionsOverflowMenu: executing onDispose callback");
        try {
          onDispose(sample);
        } catch (error) {
          console.error(
            "SampleActionsOverflowMenu: error in onDispose callback",
            error,
          );
        }
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

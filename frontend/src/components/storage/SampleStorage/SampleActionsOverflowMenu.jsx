import React, { useCallback, useEffect } from "react";
import { OverflowMenu, OverflowMenuItem } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import "./SampleActionsOverflowMenu.css";

/**
 * Overflow menu for sample row actions
 * Displays four menu items: Move, Dispose, View Audit (disabled), View Storage
 *
 * Props:
 * - sample: object - Sample data { id, sampleId, type, status }
 * - onMove: function - Callback when Move clicked
 * - onDispose: function - Callback when Dispose clicked
 * - onViewStorage: function - Callback when View Storage clicked
 */
const SampleActionsOverflowMenu = ({
  sample,
  onMove,
  onDispose,
  onViewStorage,
}) => {
  const intl = useIntl();

  // Debug: Log component props on mount and when they change
  useEffect(() => {
    console.log("SampleActionsOverflowMenu: Component mounted/updated", {
      sampleId: sample?.sampleId,
      hasOnMove: !!onMove,
      hasOnDispose: !!onDispose,
      hasOnViewStorage: !!onViewStorage,
      sample: sample,
    });
  }, [sample, onMove, onDispose, onViewStorage]);

  // Use useCallback to ensure stable function references
  // Carbon OverflowMenuItem onClick receives an event object
  const handleMove = useCallback(
    (event) => {
      console.log("SampleActionsOverflowMenu: handleMove called", {
        sample,
        event,
        hasOnMove: !!onMove,
      });
      // Prevent default behavior and stop propagation
      if (event) {
        event.preventDefault?.();
        event.stopPropagation?.();
      }
      // Execute callback if provided
      if (onMove) {
        console.log("SampleActionsOverflowMenu: executing onMove callback");
        try {
          onMove(sample);
        } catch (error) {
          console.error(
            "SampleActionsOverflowMenu: error in onMove callback",
            error,
          );
        }
      } else {
        console.warn(
          "SampleActionsOverflowMenu: onMove callback not provided for sample",
          sample?.sampleId,
        );
      }
    },
    [sample, onMove],
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

  const handleViewStorage = useCallback(
    (event) => {
      console.log("SampleActionsOverflowMenu: handleViewStorage called", {
        sample,
        event,
        hasOnViewStorage: !!onViewStorage,
      });
      if (event) {
        event.preventDefault?.();
        event.stopPropagation?.();
      }
      if (onViewStorage) {
        console.log(
          "SampleActionsOverflowMenu: executing onViewStorage callback",
        );
        try {
          onViewStorage(sample);
        } catch (error) {
          console.error(
            "SampleActionsOverflowMenu: error in onViewStorage callback",
            error,
          );
        }
      } else {
        console.warn(
          "SampleActionsOverflowMenu: onViewStorage callback not provided for sample",
          sample?.sampleId,
        );
      }
    },
    [sample, onViewStorage],
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
            id: "storage.move.sample",
            defaultMessage: "Move",
          })}
          onClick={handleMove}
          data-testid="move-menu-item"
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
        <OverflowMenuItem
          itemText={intl.formatMessage({
            id: "storage.view.storage",
            defaultMessage: "View Storage",
          })}
          onClick={handleViewStorage}
          data-testid="view-storage-menu-item"
        />
      </OverflowMenu>
    </div>
  );
};

export default SampleActionsOverflowMenu;

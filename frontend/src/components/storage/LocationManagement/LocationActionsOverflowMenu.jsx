import React, { useCallback } from "react";
import { OverflowMenu, OverflowMenuItem } from "@carbon/react";
import { useIntl } from "react-intl";
import "../SampleStorage/SampleActionsOverflowMenu.css";

/**
 * Overflow menu for location row actions (Rooms, Devices, Shelves, Racks)
 * Displays menu items: Edit, Delete, Label Management (for devices, shelves, racks only)
 *
 * Props:
 * - location: object - Location entity data { id, name, code, type, ... }
 * - onEdit: function - Callback when Edit clicked
 * - onDelete: function - Callback when Delete clicked
 * - onLabelManagement: function - Callback when Label Management clicked
 */
const LocationActionsOverflowMenu = ({
  location,
  onEdit,
  onDelete,
  onLabelManagement,
}) => {
  const intl = useIntl();

  // Use useCallback to ensure stable function references
  const handleEdit = useCallback(
    (event) => {
      if (event) {
        event.preventDefault?.();
        event.stopPropagation?.();
      }
      if (onEdit) {
        onEdit(location);
      }
    },
    [location, onEdit],
  );

  const handleDelete = useCallback(
    (event) => {
      if (event) {
        event.preventDefault?.();
        event.stopPropagation?.();
      }
      if (onDelete) {
        onDelete(location);
      }
    },
    [location, onDelete],
  );

  const handleLabelManagement = useCallback(
    (event) => {
      if (event) {
        event.preventDefault?.();
        event.stopPropagation?.();
      }
      if (onLabelManagement) {
        onLabelManagement(location);
      }
    },
    [location, onLabelManagement],
  );

  return (
    <div className="sample-actions-overflow-menu">
      <OverflowMenu
        ariaLabel={intl.formatMessage({
          id: "storage.location.actions",
          defaultMessage: "Location actions",
        })}
        data-testid="location-actions-overflow-menu"
      >
        <OverflowMenuItem
          itemText={intl.formatMessage({
            id: "storage.edit.location",
            defaultMessage: "Edit",
          })}
          onClick={handleEdit}
          data-testid="edit-location-menu-item"
        />
        <OverflowMenuItem
          itemText={intl.formatMessage({
            id: "storage.delete.location",
            defaultMessage: "Delete",
          })}
          onClick={handleDelete}
          data-testid="delete-location-menu-item"
        />
        {/* Label Management only for devices, shelves, and racks (not rooms) */}
        {location &&
          location.type !== "room" &&
          (location.type === "device" ||
            location.type === "shelf" ||
            location.type === "rack") && (
            <OverflowMenuItem
              itemText={intl.formatMessage({
                id: "label.management.title",
                defaultMessage: "Label Management",
              })}
              onClick={handleLabelManagement}
              data-testid="label-management-menu-item"
            />
          )}
      </OverflowMenu>
    </div>
  );
};

export default LocationActionsOverflowMenu;

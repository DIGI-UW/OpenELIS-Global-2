import React, { useCallback, useContext } from "react";
import { OverflowMenu, OverflowMenuItem } from "@carbon/react";
import { useIntl } from "react-intl";
import { hasRole, Roles } from "../../utils/Utils";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import "../SampleStorage/SampleActionsOverflowMenu.css";

/**
 * Overflow menu for location row actions (Rooms, Devices, Shelves, Racks)
 * Displays menu items: Edit, Delete, Print Label (for devices, shelves, racks only)
 *
 * Props:
 * - location: object - Location entity data { id, name, code, type, ... }
 * - onEdit: function - Callback when Edit clicked
 * - onDelete: function - Callback when Delete clicked
 * - onPrintLabel: function - Callback when Print Label clicked
 */
const LocationActionsOverflowMenu = ({
  location,
  onEdit,
  onDelete,
  onPrintLabel,
}) => {
  const intl = useIntl();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  // Check for both "Global Administrator" and "Admin" roles (database may use either name)
  const isAdmin =
    hasRole(userSessionDetails, Roles.GLOBAL_ADMIN) ||
    hasRole(userSessionDetails, "Admin");

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

  const handlePrintLabel = useCallback(
    (event) => {
      if (event) {
        event.preventDefault?.();
        event.stopPropagation?.();
      }
      if (onPrintLabel) {
        onPrintLabel(location);
      }
    },
    [location, onPrintLabel],
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
          disabled={!isAdmin}
          title={
            !isAdmin
              ? intl.formatMessage({
                  id: "storage.delete.admin.only",
                  defaultMessage:
                    "Only Global Administrators can delete locations",
                })
              : ""
          }
          data-testid="delete-location-menu-item"
        />
        {/* Print Label only for devices, shelves, and racks (not rooms) */}
        {location &&
          location.type !== "room" &&
          (location.type === "device" ||
            location.type === "shelf" ||
            location.type === "rack") && (
            <OverflowMenuItem
              itemText={intl.formatMessage({
                id: "label.printLabel",
                defaultMessage: "Print Label",
              })}
              onClick={handlePrintLabel}
              data-testid="print-label-menu-item"
            />
          )}
      </OverflowMenu>
    </div>
  );
};

export default LocationActionsOverflowMenu;

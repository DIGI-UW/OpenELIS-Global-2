import React from "react";
import { Button } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import LocationActionsOverflowMenu from "../LocationManagement/LocationActionsOverflowMenu";

/**
 * Box/Plate CRUD controls for the Boxes tab.
 *
 * - Keeps existing rack→box→grid assignment workflow intact
 * - Provides:
 *   - Add Box/Plate button (requires rack selection)
 *   - Edit/Delete overflow menu (only when a box is selected)
 */
const BoxCrudControls = ({
  selectedRackId,
  selectedBox,
  onCreate,
  onEdit,
  onDelete,
}) => {
  const boxForMenu = selectedBox ? { ...selectedBox, type: "box" } : null;

  return (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        gap: "0.5rem",
        marginTop: "0.5rem",
      }}
    >
      <Button
        kind="primary"
        onClick={onCreate}
        data-testid="add-box-button"
        disabled={!selectedRackId}
        size="sm"
      >
        <FormattedMessage id="storage.add.box" defaultMessage="Add Box/Plate" />
      </Button>
      {boxForMenu && (
        <LocationActionsOverflowMenu
          location={boxForMenu}
          onEdit={onEdit}
          onDelete={onDelete}
          onPrintLabel={null}
        />
      )}
    </div>
  );
};

export default BoxCrudControls;

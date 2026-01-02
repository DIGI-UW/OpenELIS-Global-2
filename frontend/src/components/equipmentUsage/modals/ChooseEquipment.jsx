import React, { useState, useMemo } from "react";
import {
  Modal,
  TextInput,
  Button,
  ButtonSet,
  Loading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { Search } from "@carbon/icons-react";
import "./ChooseEquipment.css";

/**
 * ChooseEquipmentModal Component
 *
 * Modal for selecting a cartridge (equipment) from available inventory.
 * Features:
 * - Search/filter by equipment name or serial number
 * - Display equipment list with names and serial numbers
 * - Select button to choose equipment
 * - Remove button to deselect
 * - Cancel/Close button
 */
const ChooseEquipmentModal = ({ open, onClose, equipment, onSelectEquipment }) => {
  const intl = useIntl();
  const [searchTerm, setSearchTerm] = useState("");

  // Filter equipment based on search term
  const filteredEquipment = useMemo(() => {
    if (!equipment || !Array.isArray(equipment)) {
      return [];
    }

    if (!searchTerm.trim()) {
      return equipment;
    }

    const lowerSearchTerm = searchTerm.toLowerCase();
    return equipment.filter(
      (item) =>
        (item.name && item.name.toLowerCase().includes(lowerSearchTerm)) ||
        (item.catalogNumber &&
          item.catalogNumber.toLowerCase().includes(lowerSearchTerm)) ||
        (item.manufacturer &&
          item.manufacturer.toLowerCase().includes(lowerSearchTerm))
    );
  }, [equipment, searchTerm]);

  const handleSelectEquipment = (selectedItem) => {
    onSelectEquipment(selectedItem);
  };

  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
  };

  const handleClose = () => {
    setSearchTerm("");
    onClose();
  };

  return (
    <Modal
      open={open}
      onRequestClose={handleClose}
      modalHeading={intl.formatMessage({
        id: "equipment.usage.chooseEquipment",
        defaultMessage: "Choose equipment",
      })}
      primaryButtonText={intl.formatMessage({
        id: "common.close",
        defaultMessage: "Close",
      })}
      onRequestSubmit={handleClose}
      size="md"
    >
      <div className="chooseEquipmentModal">
        {/* Search Field */}
        <div className="searchSection">
          <TextInput
            id="equipment-search"
            type="text"
            placeholder={intl.formatMessage({
              id: "equipment.usage.search.placeholder",
              defaultMessage: "Filter by equipment name or serial",
            })}
            value={searchTerm}
            onChange={handleSearchChange}
            labelText={intl.formatMessage({
              id: "equipment.usage.search.label",
              defaultMessage: "Search",
            })}
          />
        </div>

        {/* Equipment Count */}
        <div className="equipmentCountSection">
          <span className="equipmentCount">
            {filteredEquipment.length}
            {intl.formatMessage({
              id: "equipment.usage.equipmentFound",
              defaultMessage: " equipment found",
            })}
          </span>
        </div>

        {/* Equipment List */}
        <div className="equipmentListSection">
          {filteredEquipment && filteredEquipment.length > 0 ? (
            <ul className="equipmentList">
              {filteredEquipment.map((item) => (
                <li key={item.id} className="equipmentListItem">
                  <div className="equipmentInfo">
                    <div className="equipmentName">{item.name}</div>
                    <div className="equipmentDetails">
                      <span className="equipmentSerial">
                        {item.catalogNumber || "No serial"}
                      </span>
                      {item.manufacturer && (
                        <span className="equipmentManufacturer">
                          {item.manufacturer}
                        </span>
                      )}
                    </div>
                  </div>
                  <div className="equipmentActions">
                    <Button
                      kind="primary"
                      size="sm"
                      onClick={() => handleSelectEquipment(item)}
                    >
                      {intl.formatMessage({
                        id: "equipment.usage.useThisEquipment",
                        defaultMessage: "Use this equipment",
                      })}
                    </Button>
                    <Button kind="ghost" size="sm">
                      {intl.formatMessage({
                        id: "common.remove",
                        defaultMessage: "Remove",
                      })}
                    </Button>
                  </div>
                </li>
              ))}
            </ul>
          ) : (
            <div className="noResultsSection">
              <p>
                {intl.formatMessage({
                  id: "equipment.usage.noEquipment",
                  defaultMessage: "No equipment found",
                })}
              </p>
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="modalFooter">
          <ButtonSet>
            <Button kind="secondary" onClick={handleClose}>
              {intl.formatMessage({
                id: "common.cancel",
                defaultMessage: "Cancel",
              })}
            </Button>
            <Button kind="primary" onClick={handleClose}>
              {intl.formatMessage({
                id: "common.close",
                defaultMessage: "Close",
              })}
            </Button>
          </ButtonSet>
        </div>
      </div>
    </Modal>
  );
};

export default ChooseEquipmentModal;

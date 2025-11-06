import React, { useState, useEffect } from "react";
import { Dropdown } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";

/**
 * Cascading dropdown mode for storage location selection
 * Implements hierarchical selection: Room → Device → Shelf → Rack → Position
 */
const CascadingDropdownMode = ({ onLocationChange, enableInlineCreation }) => {
  const intl = useIntl();

  const [rooms, setRooms] = useState([]);
  const [devices, setDevices] = useState([]);
  const [shelves, setShelves] = useState([]);
  const [racks, setRacks] = useState([]);
  const [positions, setPositions] = useState([]);

  const [selectedRoom, setSelectedRoom] = useState(null);
  const [selectedDevice, setSelectedDevice] = useState(null);
  const [selectedShelf, setSelectedShelf] = useState(null);
  const [selectedRack, setSelectedRack] = useState(null);
  const [selectedPosition, setSelectedPosition] = useState(null);

  // Load rooms on mount
  useEffect(() => {
    getFromOpenElisServer("/rest/storage/rooms", setRooms, () => {});
  }, []);

  // Load devices when room selected
  useEffect(() => {
    if (selectedRoom) {
      getFromOpenElisServer(
        `/rest/storage/devices?roomId=${selectedRoom.id}`,
        setDevices,
        () => {},
      );
      // Reset child selections
      setSelectedDevice(null);
      setSelectedShelf(null);
      setSelectedRack(null);
      setSelectedPosition(null);
      setShelves([]);
      setRacks([]);
      setPositions([]);
    }
  }, [selectedRoom]);

  // Load shelves when device selected
  useEffect(() => {
    if (selectedDevice) {
      getFromOpenElisServer(
        `/rest/storage/shelves?deviceId=${selectedDevice.id}`,
        setShelves,
        () => {},
      );
      setSelectedShelf(null);
      setSelectedRack(null);
      setSelectedPosition(null);
      setRacks([]);
      setPositions([]);
    }
  }, [selectedDevice]);

  // Load racks when shelf selected
  useEffect(() => {
    if (selectedShelf) {
      getFromOpenElisServer(
        `/rest/storage/racks?shelfId=${selectedShelf.id}`,
        setRacks,
        () => {},
      );
      setSelectedRack(null);
      setSelectedPosition(null);
      setPositions([]);
    }
  }, [selectedShelf]);

  // Load positions when rack selected
  useEffect(() => {
    if (selectedRack) {
      getFromOpenElisServer(
        `/rest/storage/positions?rackId=${selectedRack.id}&occupied=false`,
        setPositions,
        () => {},
      );
      setSelectedPosition(null);
    }
  }, [selectedRack]);

  // Notify parent when position selected
  useEffect(() => {
    if (selectedPosition && onLocationChange) {
      onLocationChange({
        room: selectedRoom,
        device: selectedDevice,
        shelf: selectedShelf,
        rack: selectedRack,
        position: selectedPosition,
      });
    }
  }, [selectedPosition]);

  return (
    <div className="cascading-dropdown-container">
      <Dropdown
        id="room-dropdown"
        data-testid="room-dropdown"
        titleText={intl.formatMessage({ id: "storage.room.label" })}
        label={intl.formatMessage({ id: "storage.room.label" })}
        items={rooms || []}
        itemToString={(item) => (item ? item.name : "")}
        onChange={({ selectedItem }) => setSelectedRoom(selectedItem)}
        selectedItem={selectedRoom}
      />

      <Dropdown
        id="device-dropdown"
        data-testid="device-dropdown"
        titleText={intl.formatMessage({ id: "storage.device.label" })}
        label={intl.formatMessage({ id: "storage.device.label" })}
        items={devices || []}
        itemToString={(item) => (item ? item.name : "")}
        onChange={({ selectedItem }) => setSelectedDevice(selectedItem)}
        selectedItem={selectedDevice}
        disabled={!selectedRoom}
      />

      <Dropdown
        id="shelf-dropdown"
        data-testid="shelf-dropdown"
        titleText={intl.formatMessage({ id: "storage.shelf.label" })}
        label={intl.formatMessage({ id: "storage.shelf.label" })}
        items={shelves || []}
        itemToString={(item) => (item ? item.label : "")}
        onChange={({ selectedItem }) => setSelectedShelf(selectedItem)}
        selectedItem={selectedShelf}
        disabled={!selectedDevice}
      />

      <Dropdown
        id="rack-dropdown"
        data-testid="rack-dropdown"
        titleText={intl.formatMessage({ id: "storage.rack.label" })}
        label={intl.formatMessage({ id: "storage.rack.label" })}
        items={racks || []}
        itemToString={(item) => (item ? item.label : "")}
        onChange={({ selectedItem }) => setSelectedRack(selectedItem)}
        selectedItem={selectedRack}
        disabled={!selectedShelf}
      />

      <Dropdown
        id="position-dropdown"
        data-testid="position-dropdown"
        titleText={intl.formatMessage({ id: "storage.position.label" })}
        label={intl.formatMessage({ id: "storage.position.label" })}
        items={positions || []}
        itemToString={(item) => (item ? item.coordinate : "")}
        onChange={({ selectedItem }) => setSelectedPosition(selectedItem)}
        selectedItem={selectedPosition}
        disabled={!selectedRack}
      />

      {enableInlineCreation && (
        <div className="inline-creation-buttons">
          <button type="button">Add New Room</button>
        </div>
      )}
    </div>
  );
};

export default CascadingDropdownMode;

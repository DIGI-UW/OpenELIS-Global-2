import React, { useState, useEffect, useCallback, useRef } from "react";
import { Grid, Column, Dropdown, Loading } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";

/**
 * StorageHierarchySelector - Reusable component for hierarchical storage location selection.
 * Provides cascading dropdowns: Room → Device/Freezer → Shelf → Rack → Box
 *
 * Storage Hierarchy:
 * - Room: Physical room containing storage equipment
 * - Device/Freezer: Refrigerator, Freezer (-20°C, -80°C), LN2 Tank, Incubator
 * - Shelf: Physical shelf within the device
 * - Rack: Storage rack within the shelf
 * - Box: Storage box within the rack
 * - Well: Individual position within the box (handled by BoxLayoutViewer)
 *
 * @param {Object} props
 * @param {function} props.onSelectionChange - Callback when selection changes, receives { room, device, shelf, rack, box }
 * @param {Object} props.initialSelection - Initial selection state (optional)
 * @param {boolean} props.boxRequired - Whether box selection is required (default: true)
 * @param {boolean} props.showPath - Whether to show the hierarchical path (default: true)
 * @param {number} props.entryId - Notebook entry ID for box layout loading (optional)
 * @param {function} props.onBoxLayoutLoaded - Callback when box layout is loaded (optional)
 */
function StorageHierarchySelector({
  onSelectionChange,
  initialSelection,
  boxRequired = true,
  showPath = true,
  entryId,
  onBoxLayoutLoaded,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // Hierarchical storage selection state
  const [rooms, setRooms] = useState([]);
  const [devices, setDevices] = useState([]);
  const [shelves, setShelves] = useState([]);
  const [racks, setRacks] = useState([]);
  const [boxes, setBoxes] = useState([]);

  const [selectedRoom, setSelectedRoom] = useState(null);
  const [selectedDevice, setSelectedDevice] = useState(null);
  const [selectedShelf, setSelectedShelf] = useState(null);
  const [selectedRack, setSelectedRack] = useState(null);
  const [selectedBox, setSelectedBox] = useState(null);

  const [loadingHierarchy, setLoadingHierarchy] = useState(false);

  // Load rooms on mount
  useEffect(() => {
    componentMounted.current = true;
    loadRooms();

    return () => {
      componentMounted.current = false;
    };
  }, []);

  // Notify parent of selection changes
  useEffect(() => {
    if (onSelectionChange) {
      onSelectionChange({
        room: selectedRoom,
        device: selectedDevice,
        shelf: selectedShelf,
        rack: selectedRack,
        box: selectedBox,
      });
    }
  }, [selectedRoom, selectedDevice, selectedShelf, selectedRack, selectedBox]);

  const loadRooms = () => {
    getFromOpenElisServer("/rest/storage/rooms?status=active", (response) => {
      if (componentMounted.current && response && Array.isArray(response)) {
        setRooms(
          response.map((r) => ({
            id: r.id,
            label: r.name,
            ...r,
          })),
        );
      }
    });
  };

  // Load devices when room changes
  const handleRoomChange = ({ selectedItem }) => {
    setSelectedRoom(selectedItem);
    setSelectedDevice(null);
    setSelectedShelf(null);
    setSelectedRack(null);
    setSelectedBox(null);
    setDevices([]);
    setShelves([]);
    setRacks([]);
    setBoxes([]);

    if (selectedItem) {
      setLoadingHierarchy(true);
      getFromOpenElisServer(
        `/rest/storage/devices?roomId=${selectedItem.id}&active=true`,
        (response) => {
          setLoadingHierarchy(false);
          if (componentMounted.current && response && Array.isArray(response)) {
            setDevices(
              response.map((d) => ({
                id: d.id,
                label: d.name,
                ...d,
              })),
            );
          }
        },
      );
    }
  };

  // Load shelves when device changes
  const handleDeviceChange = ({ selectedItem }) => {
    setSelectedDevice(selectedItem);
    setSelectedShelf(null);
    setSelectedRack(null);
    setSelectedBox(null);
    setShelves([]);
    setRacks([]);
    setBoxes([]);

    if (selectedItem) {
      setLoadingHierarchy(true);
      getFromOpenElisServer(
        `/rest/storage/shelves?deviceId=${selectedItem.id}&active=true`,
        (response) => {
          setLoadingHierarchy(false);
          if (componentMounted.current && response && Array.isArray(response)) {
            setShelves(
              response.map((s) => ({
                id: s.id,
                label: s.label || s.name,
                ...s,
              })),
            );
          }
        },
      );
    }
  };

  // Load racks when shelf changes
  const handleShelfChange = ({ selectedItem }) => {
    setSelectedShelf(selectedItem);
    setSelectedRack(null);
    setSelectedBox(null);
    setRacks([]);
    setBoxes([]);

    if (selectedItem) {
      setLoadingHierarchy(true);
      getFromOpenElisServer(
        `/rest/storage/racks?shelfId=${selectedItem.id}&active=true`,
        (response) => {
          setLoadingHierarchy(false);
          if (componentMounted.current && response && Array.isArray(response)) {
            setRacks(
              response.map((r) => ({
                id: r.id,
                label: r.label || r.name,
                ...r,
              })),
            );
          }
        },
      );
    }
  };

  // Load boxes when rack changes
  const handleRackChange = ({ selectedItem }) => {
    setSelectedRack(selectedItem);
    setSelectedBox(null);
    setBoxes([]);

    if (selectedItem) {
      setLoadingHierarchy(true);
      getFromOpenElisServer(
        `/rest/storage/boxes?rackId=${selectedItem.id}&active=true`,
        (response) => {
          setLoadingHierarchy(false);
          if (componentMounted.current && response && Array.isArray(response)) {
            setBoxes(
              response.map((b) => ({
                id: b.id,
                label: b.label || b.name,
                rows: b.rows || 8,
                columns: b.columns || 12,
                ...b,
              })),
            );
          }
        },
      );
    }
  };

  // Load box layout when box changes
  const handleBoxChange = ({ selectedItem }) => {
    setSelectedBox(selectedItem);

    if (selectedItem && entryId && onBoxLayoutLoaded) {
      setLoadingHierarchy(true);
      getFromOpenElisServer(
        `/rest/notebook/${entryId}/box/${selectedItem.id}/layout`,
        (response) => {
          setLoadingHierarchy(false);
          if (componentMounted.current && response) {
            onBoxLayoutLoaded(response.wells || {});
          }
        },
      );
    }
  };

  // Build hierarchical path
  const getHierarchicalPath = () => {
    const parts = [];
    if (selectedRoom) parts.push(selectedRoom.label);
    if (selectedDevice) parts.push(selectedDevice.label);
    if (selectedShelf) parts.push(selectedShelf.label);
    if (selectedRack) parts.push(selectedRack.label);
    if (selectedBox) parts.push(selectedBox.label);
    return parts.join(" > ");
  };

  return (
    <div className="storage-hierarchy-selector">
      <Grid fullWidth narrow>
        <Column lg={8} md={4} sm={4}>
          <Dropdown
            id="room-dropdown"
            titleText={intl.formatMessage({
              id: "notebook.storage.room",
              defaultMessage: "Room",
            })}
            label={intl.formatMessage({
              id: "notebook.storage.selectRoom",
              defaultMessage: "Select room...",
            })}
            items={rooms}
            itemToString={(item) => (item ? item.label : "")}
            selectedItem={selectedRoom}
            onChange={handleRoomChange}
          />
        </Column>
        <Column lg={8} md={4} sm={4}>
          <Dropdown
            id="device-dropdown"
            titleText={intl.formatMessage({
              id: "notebook.storage.device",
              defaultMessage: "Freezer/Device",
            })}
            label={intl.formatMessage({
              id: "notebook.storage.selectDevice",
              defaultMessage: "Select freezer/device...",
            })}
            items={devices}
            itemToString={(item) => (item ? item.label : "")}
            selectedItem={selectedDevice}
            onChange={handleDeviceChange}
            disabled={!selectedRoom}
          />
        </Column>
      </Grid>

      <Grid fullWidth narrow style={{ marginTop: "0.5rem" }}>
        <Column lg={5} md={3} sm={4}>
          <Dropdown
            id="shelf-dropdown"
            titleText={intl.formatMessage({
              id: "notebook.storage.shelf",
              defaultMessage: "Shelf",
            })}
            label={intl.formatMessage({
              id: "notebook.storage.selectShelf",
              defaultMessage: "Select shelf...",
            })}
            items={shelves}
            itemToString={(item) => (item ? item.label : "")}
            selectedItem={selectedShelf}
            onChange={handleShelfChange}
            disabled={!selectedDevice}
          />
        </Column>
        <Column lg={5} md={3} sm={4}>
          <Dropdown
            id="rack-dropdown"
            titleText={intl.formatMessage({
              id: "notebook.storage.rack",
              defaultMessage: "Rack",
            })}
            label={intl.formatMessage({
              id: "notebook.storage.selectRack",
              defaultMessage: "Select rack...",
            })}
            items={racks}
            itemToString={(item) => (item ? item.label : "")}
            selectedItem={selectedRack}
            onChange={handleRackChange}
            disabled={!selectedShelf}
            helperText={intl.formatMessage({
              id: "notebook.storage.rack.optional",
              defaultMessage: "Optional - some labs may not use racks",
            })}
          />
        </Column>
        <Column lg={6} md={2} sm={4}>
          <Dropdown
            id="box-dropdown"
            titleText={intl.formatMessage({
              id: "notebook.storage.box",
              defaultMessage: "Box",
            })}
            label={intl.formatMessage({
              id: "notebook.storage.selectBox",
              defaultMessage: "Select box...",
            })}
            items={boxes}
            itemToString={(item) => (item ? item.label : "")}
            selectedItem={selectedBox}
            onChange={handleBoxChange}
            disabled={!selectedShelf}
            helperText={intl.formatMessage({
              id: "notebook.storage.box.optional",
              defaultMessage: "Optional - can assign to shelf level",
            })}
          />
        </Column>
      </Grid>

      {/* Hierarchical Path Display */}
      {showPath && getHierarchicalPath() && (
        <div
          style={{
            marginTop: "0.5rem",
            fontSize: "0.875rem",
            color: "#525252",
            backgroundColor: "#f4f4f4",
            padding: "0.5rem",
            borderRadius: "4px",
          }}
        >
          <strong>
            <FormattedMessage
              id="notebook.storage.path"
              defaultMessage="Path:"
            />
          </strong>{" "}
          {getHierarchicalPath()}
        </div>
      )}

      {loadingHierarchy && (
        <div style={{ marginTop: "0.5rem" }}>
          <Loading withOverlay={false} small description="Loading..." />
        </div>
      )}
    </div>
  );
}

export default StorageHierarchySelector;
